/*
 * File: PtmRouting.kt
 * Module: feature.school
 *
 * Endpoints (all JWT):
 *   GET  /api/v1/school/ptm
 *   POST /api/v1/school/ptm
 *
 * Spec ref: school_api_spec.artifact.md §Module: PTM
 *
 * `GET /api/v1/school/ptm` returns three buckets:
 *   active_event   — next upcoming PTM (date >= today); falls back to the
 *                    most-recent past row if no future row exists.  Null if
 *                    the school has never scheduled a PTM.
 *   history        — past PTMs with turnout / total_met (capped at 10).
 *   class_progress — per-class met/total rollup for the active event,
 *                    computed against `ptm_class_progress`.
 *
 * `POST /api/v1/school/ptm` creates a new PTM with sane defaults (counters
 * start at zero) and returns it as the new active_event payload.
 */
package com.littlebridge.vidyaprayag.feature.school

import com.littlebridge.vidyaprayag.core.created
import com.littlebridge.vidyaprayag.core.fail
import com.littlebridge.vidyaprayag.core.ok
import com.littlebridge.vidyaprayag.core.principalUserId
import com.littlebridge.vidyaprayag.db.AppUsersTable
import com.littlebridge.vidyaprayag.db.DatabaseFactory.dbQuery
import com.littlebridge.vidyaprayag.db.PtmClassProgressTable
import com.littlebridge.vidyaprayag.db.PtmEventsTable
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

// ---------------- DTOs ----------------

@Serializable
data class PtmActiveEventDto(
    val id: String,
    val title: String,
    val date: String,
    val slot: String,
    @SerialName("expected_parents") val expectedParents: Int,
    @SerialName("checked_in_parents") val checkedInParents: Int,
    @SerialName("invites_delivered") val invitesDelivered: Int,
    @SerialName("read_receipts") val readReceipts: Int
)

@Serializable
data class PtmHistoryDto(
    val id: String,
    val date: String,
    val title: String,
    val turnout: Int,
    @SerialName("total_met") val totalMet: Int
)

@Serializable
data class PtmClassProgressDto(
    val id: String,
    @SerialName("class_name") val className: String,
    @SerialName("teacher_name") val teacherName: String,
    @SerialName("met_count") val metCount: Int,
    @SerialName("total_count") val totalCount: Int,
    val progress: Double
)

@Serializable
data class PtmResponse(
    @SerialName("active_event") val activeEvent: PtmActiveEventDto?,
    val history: List<PtmHistoryDto>,
    @SerialName("class_progress") val classProgress: List<PtmClassProgressDto>
)

@Serializable
data class CreatePtmDto(
    val title: String,
    val date: String,                                        // YYYY-MM-DD
    val slot: String,                                        // e.g. "09:00 - 13:00"
    @SerialName("expected_parents") val expectedParents: Int = 0
)

// ---------------- helpers ----------------

private fun resolveSchoolId(uid: UUID): UUID? = AppUsersTable
    .selectAll().where { AppUsersTable.id eq uid }
    .singleOrNull()
    ?.get(AppUsersTable.schoolId)

private fun org.jetbrains.exposed.sql.ResultRow.toActiveDto() = PtmActiveEventDto(
    id = this[PtmEventsTable.id].value.toString(),
    title = this[PtmEventsTable.title],
    date = this[PtmEventsTable.date],
    slot = this[PtmEventsTable.slot],
    expectedParents = this[PtmEventsTable.expectedParents],
    checkedInParents = this[PtmEventsTable.checkedInParents],
    invitesDelivered = this[PtmEventsTable.invitesDelivered],
    readReceipts = this[PtmEventsTable.readReceipts]
)

private fun org.jetbrains.exposed.sql.ResultRow.toHistoryDto() = PtmHistoryDto(
    id = this[PtmEventsTable.id].value.toString(),
    date = this[PtmEventsTable.date],
    title = this[PtmEventsTable.title],
    turnout = this[PtmEventsTable.turnout],
    totalMet = this[PtmEventsTable.totalMet]
)

private fun org.jetbrains.exposed.sql.ResultRow.toProgressDto(): PtmClassProgressDto {
    val met = this[PtmClassProgressTable.metCount]
    val total = this[PtmClassProgressTable.totalCount]
    return PtmClassProgressDto(
        id = this[PtmClassProgressTable.id].value.toString(),
        className = this[PtmClassProgressTable.className],
        teacherName = this[PtmClassProgressTable.teacherName],
        metCount = met,
        totalCount = total,
        progress = if (total == 0) 0.0 else met.toDouble() / total.toDouble()
    )
}

fun Route.ptmRouting() {
    authenticate("jwt") {
        route("/api/v1/school/ptm") {

            // -------- GET --------
            get {
                val uid = call.principalUserId()?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid token", HttpStatusCode.Unauthorized); return@get }

                val payload = dbQuery {
                    val schoolId = resolveSchoolId(uid)
                        ?: return@dbQuery PtmResponse(null, emptyList(), emptyList())

                    val today = LocalDate.now().toString()

                    // Active event = next upcoming (date >= today, soonest first)
                    val upcoming = PtmEventsTable.selectAll()
                        .where { (PtmEventsTable.schoolId eq schoolId) and (PtmEventsTable.date greaterEq today) }
                        .orderBy(PtmEventsTable.date, SortOrder.ASC)
                        .limit(1)
                        .firstOrNull()
                    val past = if (upcoming == null) {
                        PtmEventsTable.selectAll()
                            .where { (PtmEventsTable.schoolId eq schoolId) and (PtmEventsTable.date less today) }
                            .orderBy(PtmEventsTable.date, SortOrder.DESC)
                            .limit(1)
                            .firstOrNull()
                    } else null
                    val active = upcoming ?: past

                    val historyRows = PtmEventsTable.selectAll()
                        .where { (PtmEventsTable.schoolId eq schoolId) and (PtmEventsTable.date less today) }
                        .orderBy(PtmEventsTable.date, SortOrder.DESC)
                        .limit(10)
                        .toList()

                    val classProgress = active?.let { ev ->
                        PtmClassProgressTable.selectAll()
                            .where { PtmClassProgressTable.ptmEventId eq ev[PtmEventsTable.id].value }
                            .orderBy(PtmClassProgressTable.className, SortOrder.ASC)
                            .map { it.toProgressDto() }
                    } ?: emptyList()

                    PtmResponse(
                        activeEvent = active?.toActiveDto(),
                        history = historyRows.map { it.toHistoryDto() },
                        classProgress = classProgress
                    )
                }
                call.ok(payload, message = "PTM data fetched successfully")
            }

            // -------- CREATE --------
            post {
                val uid = call.principalUserId()?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid token", HttpStatusCode.Unauthorized); return@post }
                val req = call.receive<CreatePtmDto>()
                if (req.title.isBlank() || req.date.isBlank() || req.slot.isBlank()) {
                    call.fail("title, date, slot are required"); return@post
                }
                // Basic date sanity (YYYY-MM-DD) — non-fatal, just informational
                runCatching { LocalDate.parse(req.date) }.onFailure {
                    call.fail("date must be YYYY-MM-DD"); return@post
                }

                val schoolId = dbQuery { resolveSchoolId(uid) }
                    ?: run { call.fail("User not associated with any school", HttpStatusCode.NotFound); return@post }

                val newId = UUID.randomUUID()
                val now = Instant.now()
                dbQuery {
                    PtmEventsTable.insert {
                        it[PtmEventsTable.id] = newId
                        it[PtmEventsTable.schoolId] = schoolId
                        it[title] = req.title
                        it[date] = req.date
                        it[slot] = req.slot
                        it[expectedParents] = req.expectedParents
                        it[checkedInParents] = 0
                        it[invitesDelivered] = 0
                        it[readReceipts] = 0
                        it[turnout] = 0
                        it[totalMet] = 0
                        it[createdBy] = uid
                        it[createdAt] = now
                        it[updatedAt] = now
                    }
                }
                call.created(
                    PtmActiveEventDto(
                        id = newId.toString(),
                        title = req.title,
                        date = req.date,
                        slot = req.slot,
                        expectedParents = req.expectedParents,
                        checkedInParents = 0,
                        invitesDelivered = 0,
                        readReceipts = 0
                    ),
                    message = "PTM scheduled"
                )
            }
        }
    }
}
