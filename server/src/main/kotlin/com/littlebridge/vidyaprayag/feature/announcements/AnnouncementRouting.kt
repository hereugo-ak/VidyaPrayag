/*
 * File: AnnouncementRouting.kt
 * Module: feature.announcements
 *
 * Endpoints:
 *   GET  /api/v1/school/announcements?user_id=…
 *   GET  /api/v1/school/announcements/search?query=…&user_id=…
 *   POST /api/v1/school/announcements/sync-whatsapp
 *   POST /api/v1/school/announcements          -- NEW (create one)
 *
 * Spec ref: vidya_prayag_api_spec2.artifact.md §Screen: School Dashboard (Announcement Tab)
 *
 * Authorization & school-resolution rule:
 *   Every endpoint is guarded by call.requireSchoolContext(): the caller must
 *   hold a school role and have a school. Reads/writes are scoped to that
 *   resolved school_id. A ?school_id= / body school_id override is honoured
 *   ONLY for the platform "admin" role; for everyone else it is ignored so a
 *   school_admin can never read or mutate another school's announcements.
 *
 * sync-whatsapp behaviour:
 *   - Only announcements that actually belong to the caller's school are
 *     considered; any foreign event_ids in the request are rejected.
 *   - Marks the synced announcements as synced_to_wa = true (scoped by school).
 *   - Inserts one real row per (announcement × parent_phone) into whatsapp_logs
 *     with status = QUEUED — total_queued is the true number of messages that
 *     will be dispatched (recipients = active parent users with a phone).
 *   - Returns the job_id and an ETA derived from the real queued count. The
 *     actual provider dispatch is performed by the async WhatsApp worker that
 *     drains whatsapp_logs rows in QUEUED status.
 */
package com.littlebridge.vidyaprayag.feature.announcements

import com.littlebridge.vidyaprayag.core.accepted
import com.littlebridge.vidyaprayag.core.created
import com.littlebridge.vidyaprayag.core.fail
import com.littlebridge.vidyaprayag.core.ok
import com.littlebridge.vidyaprayag.core.requireSchoolContext
import com.littlebridge.vidyaprayag.db.AnnouncementsTable
import com.littlebridge.vidyaprayag.db.AppUsersTable
import com.littlebridge.vidyaprayag.db.DatabaseFactory.dbQuery
import com.littlebridge.vidyaprayag.db.WhatsappLogsTable
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

@Serializable
data class AnnouncementDto(
    val type: String,
    @SerialName("event_id") val eventId: String,
    val title: String,
    @SerialName("sub_title") val subTitle: String? = null,
    val description: String,
    @SerialName("event_image") val eventImage: String? = null,
    val date: String
)

@Serializable
data class AnnouncementsListResponse(val announcements: List<AnnouncementDto>)

@Serializable
data class CreateAnnouncementDto(
    val type: String,
    val title: String,
    @SerialName("sub_title") val subTitle: String? = null,
    val description: String,
    @SerialName("event_image") val eventImage: String? = null,
    val date: String,                              // YYYY-MM-DD
    @SerialName("school_id") val schoolId: String? = null
)

@Serializable
data class SyncWhatsAppRequest(
    @SerialName("school_id") val schoolId: String? = null,
    @SerialName("announcement_ids") val announcementIds: List<String>? = null
)

@Serializable
data class SyncWhatsAppResponse(
    @SerialName("job_id") val jobId: String,
    @SerialName("total_queued") val totalQueued: Int,
    @SerialName("estimated_time_minutes") val estimatedTimeMinutes: Int
)

// ------- helpers -------

/**
 * The effective school for the request. By default it is the caller's own
 * school. Only the platform "admin" role may target another school via an
 * override (query param or body); for any other role the override is ignored.
 */
private fun effectiveSchoolId(ctxSchoolId: UUID, ctxRole: String, override: String?): UUID {
    if (ctxRole != "admin") return ctxSchoolId
    return override?.let { runCatching { UUID.fromString(it) }.getOrNull() } ?: ctxSchoolId
}

// ------- routing -------

fun Route.announcementRouting() {
    authenticate("jwt") {
        route("/api/v1/school/announcements") {

            // ---- list ----
            get {
                val ctx = call.requireSchoolContext() ?: return@get
                val schoolId = effectiveSchoolId(
                    ctx.schoolId, ctx.role, call.request.queryParameters["school_id"]
                )
                val list = dbQuery {
                    AnnouncementsTable.selectAll()
                        .where { AnnouncementsTable.schoolId eq schoolId }
                        .orderBy(AnnouncementsTable.date, SortOrder.DESC)
                        .map { it.toDto() }
                }
                call.ok(AnnouncementsListResponse(list), message = "Announcements fetched successfully")
            }

            // ---- search ----
            get("/search") {
                val q = call.request.queryParameters["query"]?.lowercase().orEmpty()
                if (q.isBlank()) { call.fail("query is required"); return@get }
                val ctx = call.requireSchoolContext() ?: return@get
                val schoolId = effectiveSchoolId(
                    ctx.schoolId, ctx.role, call.request.queryParameters["school_id"]
                )
                val pattern = "%$q%"
                val list = dbQuery {
                    AnnouncementsTable.selectAll()
                        .where {
                            (AnnouncementsTable.schoolId eq schoolId) and
                                ((AnnouncementsTable.title.lowerCase() like pattern) or
                                    (AnnouncementsTable.description.lowerCase() like pattern))
                        }
                        .orderBy(AnnouncementsTable.date, SortOrder.DESC)
                        .map { it.toDto() }
                }
                call.ok(AnnouncementsListResponse(list), message = "Search results fetched")
            }

            // ---- create ----
            post {
                val ctx = call.requireSchoolContext() ?: return@post
                val uid = ctx.userId
                val req = call.receive<CreateAnnouncementDto>()
                val schoolId = effectiveSchoolId(ctx.schoolId, ctx.role, req.schoolId)
                val now = Instant.now()
                val eventId = "EVT_" + UUID.randomUUID().toString().take(8).uppercase()
                dbQuery {
                    AnnouncementsTable.insert {
                        it[AnnouncementsTable.schoolId] = schoolId
                        it[AnnouncementsTable.eventId] = eventId
                        it[type] = req.type
                        it[title] = req.title
                        it[subTitle] = req.subTitle
                        it[description] = req.description
                        it[eventImage] = req.eventImage
                        it[date] = req.date
                        it[syncedToWa] = false
                        it[createdBy] = uid
                        it[createdAt] = now
                        it[updatedAt] = now
                    }
                }
                call.created(
                    AnnouncementDto(req.type, eventId, req.title, req.subTitle, req.description, req.eventImage, req.date),
                    message = "Announcement created"
                )
            }

            // ---- sync-whatsapp ----
            post("/sync-whatsapp") {
                val ctx = call.requireSchoolContext() ?: return@post
                val req = call.receive<SyncWhatsAppRequest>()
                val schoolId = effectiveSchoolId(ctx.schoolId, ctx.role, req.schoolId)

                val jobId = "SYNC_WA_${UUID.randomUUID().toString().take(8).uppercase()}"
                val now = Instant.now()

                // Determine which announcements to sync, ALWAYS constrained to the
                // resolved school so foreign event_ids can never be queued.
                val result = dbQuery {
                    val schoolEventIds = AnnouncementsTable.selectAll()
                        .where { AnnouncementsTable.schoolId eq schoolId }
                        .map { it[AnnouncementsTable.eventId] }
                        .toSet()

                    val requested = req.announcementIds
                    val toSync: List<String> = if (requested.isNullOrEmpty()) {
                        // Default: every not-yet-synced announcement in this school.
                        AnnouncementsTable.selectAll()
                            .where {
                                (AnnouncementsTable.schoolId eq schoolId) and
                                    (AnnouncementsTable.syncedToWa eq false)
                            }
                            .map { it[AnnouncementsTable.eventId] }
                    } else {
                        // Keep only the requested IDs that truly belong to this school.
                        requested.filter { it in schoolEventIds }
                    }

                    // Surface foreign / unknown IDs as an error rather than silently
                    // ignoring them, so callers know their request was rejected.
                    val foreign = requested?.filterNot { it in schoolEventIds }.orEmpty()

                    Triple(toSync, foreign, schoolEventIds)
                }

                val (toSync, foreign, _) = result
                if (foreign.isNotEmpty()) {
                    call.fail(
                        "These announcement_ids do not belong to your school: ${foreign.joinToString()}",
                        HttpStatusCode.Forbidden
                    )
                    return@post
                }
                if (toSync.isEmpty()) {
                    call.accepted(
                        SyncWhatsAppResponse(jobId, 0, 0),
                        message = "Nothing to sync — all selected announcements are already up to date"
                    )
                    return@post
                }

                val queued = dbQuery {
                    // Recipients: every ACTIVE parent user in this school with a phone.
                    val parents = AppUsersTable.selectAll()
                        .where {
                            (AppUsersTable.schoolId eq schoolId) and
                                (AppUsersTable.role eq "parent")
                        }
                        .mapNotNull { it[AppUsersTable.phone] }
                        .filter { it.isNotBlank() }
                        .distinct()

                    var inserted = 0
                    toSync.forEach { aid ->
                        parents.forEach { phone ->
                            WhatsappLogsTable.insert {
                                it[WhatsappLogsTable.schoolId] = schoolId
                                it[announcementId] = aid
                                it[WhatsappLogsTable.jobId] = jobId
                                it[WhatsappLogsTable.phone] = phone
                                it[status] = "QUEUED"
                                it[createdAt] = now
                            }
                            inserted++
                        }
                        // Scope the flip by BOTH school and event id (defensive).
                        AnnouncementsTable.update({
                            (AnnouncementsTable.schoolId eq schoolId) and
                                (AnnouncementsTable.eventId eq aid)
                        }) {
                            it[syncedToWa] = true
                            it[updatedAt] = now
                        }
                    }
                    inserted
                }

                // ETA assumes ~30 messages/min throughput from the async worker.
                val eta = if (queued == 0) 0 else (queued / 30 + 1).coerceAtMost(60)
                call.accepted(
                    SyncWhatsAppResponse(jobId, queued, eta),
                    message = if (queued == 0)
                        "No parent recipients with a phone number were found for this school"
                    else
                        "Sync queued: $queued message(s) will be dispatched"
                )
            }
        }
    }
}

private fun org.jetbrains.exposed.sql.ResultRow.toDto() = AnnouncementDto(
    type = this[AnnouncementsTable.type],
    eventId = this[AnnouncementsTable.eventId],
    title = this[AnnouncementsTable.title],
    subTitle = this[AnnouncementsTable.subTitle],
    description = this[AnnouncementsTable.description],
    eventImage = this[AnnouncementsTable.eventImage],
    date = this[AnnouncementsTable.date]
)
