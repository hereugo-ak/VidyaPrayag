/*
 * File: LeaveRequestsRouting.kt
 * Module: feature.school
 *
 * Endpoints (all JWT):
 *   GET   /api/v1/school/leave-requests?type=student|teacher&status=Pending|Approved|Rejected
 *   POST  /api/v1/school/leave-requests
 *   PATCH /api/v1/school/leave-requests/{id}/status
 *
 * Spec ref: school_api_spec.artifact.md §Module: Leave Requests
 *
 * Compute rules (live, every request):
 *   approval_rate = 100 * approved / (approved + rejected) over last 30 days
 *                   (0 when denominator is 0).
 *   weekly_count  = count of requests created in the last 7 days.
 *
 * The `requester_role` column doubles as the `type` filter (student | teacher)
 * so the same table backs both UI tabs.  `image_url` is the avatar shown
 * in the list — populated from the request body or auto-derived later.
 *
 * `date_range` is formatted server-side so the UI never has to do date math.
 */
package com.littlebridge.vidyaprayag.feature.school

import com.littlebridge.vidyaprayag.core.created
import com.littlebridge.vidyaprayag.core.fail
import com.littlebridge.vidyaprayag.core.ok
import com.littlebridge.vidyaprayag.core.okMessage
import com.littlebridge.vidyaprayag.core.principalUserId
import com.littlebridge.vidyaprayag.db.AppUsersTable
import com.littlebridge.vidyaprayag.db.DatabaseFactory.dbQuery
import com.littlebridge.vidyaprayag.db.LeaveRequestsTable
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

// ---------------- DTOs ----------------

@Serializable
data class LeaveRequestDto(
    val id: String,
    @SerialName("requester_name") val requesterName: String,
    @SerialName("requester_role") val requesterRole: String,
    @SerialName("date_from") val dateFrom: String,
    @SerialName("date_to") val dateTo: String,
    @SerialName("date_range") val dateRange: String,
    val reason: String,
    @SerialName("image_url") val imageUrl: String? = null,
    val status: String
)

@Serializable
data class LeaveRequestListResponse(
    val type: String,
    @SerialName("approval_rate") val approvalRate: Int,
    @SerialName("weekly_count") val weeklyCount: Int,
    val requests: List<LeaveRequestDto>
)

@Serializable
data class CreateLeaveRequestDto(
    @SerialName("requester_name") val requesterName: String,
    @SerialName("requester_role") val requesterRole: String, // student | teacher
    @SerialName("date_from") val dateFrom: String,           // YYYY-MM-DD
    @SerialName("date_to") val dateTo: String,
    val reason: String,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("requester_id") val requesterId: String? = null
)

@Serializable
data class UpdateLeaveStatusDto(val status: String)

// ---------------- helpers ----------------

private fun resolveSchoolId(uid: UUID): UUID? = AppUsersTable
    .selectAll().where { AppUsersTable.id eq uid }
    .singleOrNull()
    ?.get(AppUsersTable.schoolId)

/** "Dec 12 - Dec 14" / "Dec 12 - Jan 04, 2026" depending on whether years differ. */
private fun formatDateRange(from: String, to: String): String {
    val fmtShort = DateTimeFormatter.ofPattern("MMM dd")
    val fmtLong  = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    return runCatching {
        val a = LocalDate.parse(from)
        val b = LocalDate.parse(to)
        if (a.year == b.year && a.year == LocalDate.now().year)
            "${a.format(fmtShort)} - ${b.format(fmtShort)}"
        else
            "${a.format(fmtLong)} - ${b.format(fmtLong)}"
    }.getOrDefault("$from - $to")
}

private fun org.jetbrains.exposed.sql.ResultRow.toLeaveDto() = LeaveRequestDto(
    id = this[LeaveRequestsTable.id].value.toString(),
    requesterName = this[LeaveRequestsTable.requesterName],
    requesterRole = this[LeaveRequestsTable.requesterRole],
    dateFrom = this[LeaveRequestsTable.dateFrom],
    dateTo = this[LeaveRequestsTable.dateTo],
    dateRange = formatDateRange(
        this[LeaveRequestsTable.dateFrom],
        this[LeaveRequestsTable.dateTo]
    ),
    reason = this[LeaveRequestsTable.reason],
    imageUrl = this[LeaveRequestsTable.imageUrl],
    status = this[LeaveRequestsTable.status]
)

fun Route.leaveRequestsRouting() {
    authenticate("jwt") {
        route("/api/v1/school/leave-requests") {

            // -------- LIST --------
            get {
                val uid = call.principalUserId()?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid token", HttpStatusCode.Unauthorized); return@get }

                val typeParam = call.request.queryParameters["type"]?.lowercase()
                val statusParam = call.request.queryParameters["status"]?.trim()

                val payload = dbQuery {
                    val schoolId = resolveSchoolId(uid)
                        ?: return@dbQuery LeaveRequestListResponse(
                            type = typeParam ?: "student",
                            approvalRate = 0,
                            weeklyCount = 0,
                            requests = emptyList()
                        )

                    // Pull ALL school rows once — cheap for the volumes we expect
                    // and lets us compute both the filtered list and the unfiltered
                    // approval_rate / weekly_count without a second round-trip.
                    val all = LeaveRequestsTable.selectAll()
                        .where { LeaveRequestsTable.schoolId eq schoolId }
                        .orderBy(LeaveRequestsTable.createdAt, SortOrder.DESC)
                        .toList()

                    val filtered = all.filter { row ->
                        (typeParam == null || row[LeaveRequestsTable.requesterRole].equals(typeParam, true)) &&
                            (statusParam == null || row[LeaveRequestsTable.status].equals(statusParam, true))
                    }

                    val now = Instant.now()
                    val thirtyDaysAgo = now.minusSeconds(30L * 24 * 60 * 60)
                    val sevenDaysAgo = now.minusSeconds(7L * 24 * 60 * 60)

                    val window30 = all.filter { it[LeaveRequestsTable.createdAt].isAfter(thirtyDaysAgo) }
                    val approved = window30.count { it[LeaveRequestsTable.status].equals("Approved", true) }
                    val rejected = window30.count { it[LeaveRequestsTable.status].equals("Rejected", true) }
                    val denom = approved + rejected
                    val approvalRate = if (denom == 0) 0 else (approved * 100) / denom

                    val weeklyCount = all.count { it[LeaveRequestsTable.createdAt].isAfter(sevenDaysAgo) }

                    LeaveRequestListResponse(
                        type = typeParam ?: "student",
                        approvalRate = approvalRate,
                        weeklyCount = weeklyCount,
                        requests = filtered.map { it.toLeaveDto() }
                    )
                }

                call.ok(payload, message = "Leave requests fetched successfully")
            }

            // -------- CREATE --------
            post {
                val uid = call.principalUserId()?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid token", HttpStatusCode.Unauthorized); return@post }
                val req = call.receive<CreateLeaveRequestDto>()

                if (req.requesterRole.lowercase() !in setOf("student", "teacher")) {
                    call.fail("requester_role must be student|teacher"); return@post
                }

                val schoolId = dbQuery { resolveSchoolId(uid) }
                    ?: run { call.fail("User not associated with any school", HttpStatusCode.NotFound); return@post }

                val newId = UUID.randomUUID()
                val now = Instant.now()
                dbQuery {
                    LeaveRequestsTable.insert {
                        it[LeaveRequestsTable.id] = newId
                        it[LeaveRequestsTable.schoolId] = schoolId
                        it[requesterId] = req.requesterId?.let { id -> runCatching { UUID.fromString(id) }.getOrNull() }
                        it[requesterName] = req.requesterName
                        it[requesterRole] = req.requesterRole.lowercase()
                        it[dateFrom] = req.dateFrom
                        it[dateTo] = req.dateTo
                        it[reason] = req.reason
                        it[imageUrl] = req.imageUrl
                        it[status] = "Pending"
                        it[createdAt] = now
                        it[updatedAt] = now
                    }
                }
                call.created(
                    LeaveRequestDto(
                        id = newId.toString(),
                        requesterName = req.requesterName,
                        requesterRole = req.requesterRole.lowercase(),
                        dateFrom = req.dateFrom,
                        dateTo = req.dateTo,
                        dateRange = formatDateRange(req.dateFrom, req.dateTo),
                        reason = req.reason,
                        imageUrl = req.imageUrl,
                        status = "Pending"
                    ),
                    message = "Leave request created"
                )
            }

            // -------- UPDATE STATUS --------
            patch("/{id}/status") {
                val uid = call.principalUserId()?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid token", HttpStatusCode.Unauthorized); return@patch }
                val id = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid id"); return@patch }
                val req = call.receive<UpdateLeaveStatusDto>()
                val normalized = when (req.status.trim().lowercase()) {
                    "pending"  -> "Pending"
                    "approved" -> "Approved"
                    "rejected" -> "Rejected"
                    else -> {
                        call.fail("status must be Pending|Approved|Rejected"); return@patch
                    }
                }
                val n = dbQuery {
                    LeaveRequestsTable.update({ LeaveRequestsTable.id eq id }) {
                        it[status] = normalized
                        it[actionedBy] = uid
                        it[actionedAt] = Instant.now()
                        it[updatedAt] = Instant.now()
                    }
                }
                if (n == 0) call.fail("Leave request not found", HttpStatusCode.NotFound)
                else call.okMessage("Leave request status updated")
            }
        }
    }
}
