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
 * School-resolution rule:
 *   Every authenticated user has app_users.school_id set after onboarding.
 *   We use that to scope announcements.  super_admins must pass ?school_id=
 *   explicitly to disambiguate.
 *
 * sync-whatsapp behaviour:
 *   - Marks chosen announcements as synced_to_wa = true.
 *   - Inserts one row per (announcement × parent_phone) into whatsapp_logs
 *     with status = QUEUED.
 *   - Returns a fake job_id (UUID) and ETA.
 *   - NO real WhatsApp call is made — drop in your provider client here.
 */
package com.littlebridge.vidyaprayag.feature.announcements

import com.littlebridge.vidyaprayag.core.accepted
import com.littlebridge.vidyaprayag.core.created
import com.littlebridge.vidyaprayag.core.fail
import com.littlebridge.vidyaprayag.core.ok
import com.littlebridge.vidyaprayag.core.principalUserId
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

private suspend fun resolveSchoolIdForUser(uid: UUID): UUID? = dbQuery {
    AppUsersTable.selectAll()
        .where { AppUsersTable.id eq uid }
        .singleOrNull()
        ?.get(AppUsersTable.schoolId)
}

private fun queryParamSchool(call: io.ktor.server.application.ApplicationCall): UUID? =
    call.request.queryParameters["school_id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }

// ------- routing -------

fun Route.announcementRouting() {
    authenticate("jwt") {
        route("/api/v1/school/announcements") {

            // ---- list ----
            get {
                val uid = call.principalUserId()?.let { UUID.fromString(it) } ?: run {
                    call.fail("Invalid token", HttpStatusCode.Unauthorized); return@get
                }
                val schoolId = queryParamSchool(call) ?: resolveSchoolIdForUser(uid) ?: run {
                    call.fail("User not associated with any school. Pass ?school_id=UUID for super_admin.",
                        HttpStatusCode.NotFound); return@get
                }
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
                val uid = call.principalUserId()?.let { UUID.fromString(it) } ?: run {
                    call.fail("Invalid token", HttpStatusCode.Unauthorized); return@get
                }
                val schoolId = queryParamSchool(call) ?: resolveSchoolIdForUser(uid) ?: run {
                    call.fail("User not associated with any school", HttpStatusCode.NotFound); return@get
                }
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
                val uid = call.principalUserId()?.let { UUID.fromString(it) } ?: run {
                    call.fail("Invalid token", HttpStatusCode.Unauthorized); return@post
                }
                val req = call.receive<CreateAnnouncementDto>()
                val schoolId = req.schoolId?.let { UUID.fromString(it) }
                    ?: resolveSchoolIdForUser(uid) ?: run {
                        call.fail("school_id required"); return@post
                    }
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
                val uid = call.principalUserId()?.let { UUID.fromString(it) } ?: run {
                    call.fail("Invalid token", HttpStatusCode.Unauthorized); return@post
                }
                val req = call.receive<SyncWhatsAppRequest>()
                val schoolId = req.schoolId?.let { UUID.fromString(it) }
                    ?: resolveSchoolIdForUser(uid) ?: run {
                        call.fail("school_id missing and could not be inferred from token"); return@post
                    }

                val jobId = "SYNC_WA_${UUID.randomUUID().toString().take(8).uppercase()}"
                val now = Instant.now()

                val queued = dbQuery {
                    val toSync = if (req.announcementIds.isNullOrEmpty()) {
                        AnnouncementsTable.selectAll()
                            .where {
                                (AnnouncementsTable.schoolId eq schoolId) and
                                    (AnnouncementsTable.syncedToWa eq false)
                            }
                            .map { it[AnnouncementsTable.eventId] }
                    } else req.announcementIds

                    if (toSync.isEmpty()) return@dbQuery 0

                    // Recipients: every PARENT user in this school with a phone.
                    val parents = AppUsersTable.selectAll()
                        .where {
                            (AppUsersTable.schoolId eq schoolId) and
                                (AppUsersTable.role eq "parent")
                        }
                        .mapNotNull { it[AppUsersTable.phone] }
                        .filter { it.isNotBlank() }

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
                        AnnouncementsTable.update({ AnnouncementsTable.eventId eq aid }) {
                            it[syncedToWa] = true
                            it[updatedAt] = now
                        }
                    }
                    if (inserted == 0) toSync.size else inserted
                }

                val eta = (queued / 30 + 1).coerceAtMost(60)
                call.accepted(
                    SyncWhatsAppResponse(jobId, queued, eta),
                    message = "Sync process initiated successfully"
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
