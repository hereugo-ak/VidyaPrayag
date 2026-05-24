/*
 * File: ParentDashboardRouting.kt
 * Module: feature.parent
 *
 * Endpoint: GET /api/v1/parent/dashboard   (JWT)
 *
 * Spec ref: parent_api_spec.artifact.md §Module: Core Dashboard & Progress
 *                                       §Screen: Parent Dashboard (Home)
 *
 * The primary "handshake" API. Drives the home screen:
 *   - greeting           : time-of-day + parent first-name (from JWT.name claim)
 *   - child_summary      : first active child of the parent
 *   - alerts             : OVERDUE fees → CRITICAL alert; INFO fallback from CMS
 *   - featured_schools   : top 5 active schools from the SchoolsTable
 *   - curation_logic     : pulled from app_config (CMS string)
 *
 * Falls back gracefully when the parent has no child yet (returns null
 * child_summary but still responds 200 so the UI can show an empty-state).
 */
package com.littlebridge.vidyaprayag.feature.parent

import com.littlebridge.vidyaprayag.core.fail
import com.littlebridge.vidyaprayag.core.ok
import com.littlebridge.vidyaprayag.core.principalName
import com.littlebridge.vidyaprayag.core.principalUserId
import com.littlebridge.vidyaprayag.db.AppConfigTable
import com.littlebridge.vidyaprayag.db.AppUsersTable
import com.littlebridge.vidyaprayag.db.ChildrenTable
import com.littlebridge.vidyaprayag.db.DatabaseFactory.dbQuery
import com.littlebridge.vidyaprayag.db.FeeRecordsTable
import com.littlebridge.vidyaprayag.db.SchoolsTable
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import java.time.LocalTime
import java.util.UUID

@Serializable
data class ChildSummary(
    val id: String,
    val name: String,
    @SerialName("overall_progress") val overallProgress: Double,
    @SerialName("current_level") val currentLevel: Int,
    @SerialName("attendance_status") val attendanceStatus: String,
    @SerialName("profile_pic") val profilePic: String? = null
)

@Serializable
data class DashboardAlert(
    val id: String,
    val title: String,
    val value: String,
    val type: String  // CRITICAL | INFO | WARNING
)

@Serializable
data class FeaturedSchool(
    val id: String,
    val name: String,
    val rating: Double,
    val location: String,
    val image: String? = null
)

@Serializable
data class DashboardResponse(
    val greeting: String,
    @SerialName("child_summary") val childSummary: ChildSummary? = null,
    val alerts: List<DashboardAlert>,
    @SerialName("featured_schools") val featuredSchools: List<FeaturedSchool>,
    @SerialName("curation_logic") val curationLogic: String
)

private fun timeOfDayGreeting(): String {
    val hour = LocalTime.now().hour
    return when {
        hour < 12 -> "Good Morning"
        hour < 17 -> "Good Afternoon"
        else      -> "Good Evening"
    }
}

private fun firstName(full: String?): String = full?.trim()?.split(" ")?.firstOrNull().orEmpty()

private fun formatMoney(amount: Double, currency: String): String {
    val symbol = when (currency.uppercase()) {
        "USD" -> "$"
        "INR" -> "₹"
        "EUR" -> "€"
        "GBP" -> "£"
        else  -> "$"
    }
    val rounded = amount.toLong()
    val withCommas = "%,d".format(rounded)
    return "$symbol$withCommas"
}

fun Route.parentDashboardRouting() {
    authenticate("jwt") {
        route("/api/v1/parent") {
            get("/dashboard") {
                val uid = call.principalUserId()?.let { runCatching { UUID.fromString(it) }.getOrNull() } ?: run {
                    call.fail("Invalid token", HttpStatusCode.Unauthorized); return@get
                }
                val nameClaim = call.principalName()

                val payload = dbQuery {
                    val user = AppUsersTable.selectAll()
                        .where { AppUsersTable.id eq uid }
                        .singleOrNull()
                    val displayFirstName = firstName(nameClaim ?: user?.get(AppUsersTable.fullName))
                    val greeting = if (displayFirstName.isBlank()) timeOfDayGreeting()
                                   else "${timeOfDayGreeting()}, $displayFirstName"

                    // ----- child summary (first active child) -----
                    val childRow = ChildrenTable.selectAll()
                        .where { (ChildrenTable.parentId eq uid) and (ChildrenTable.isActive eq true) }
                        .orderBy(ChildrenTable.createdAt, SortOrder.ASC)
                        .firstOrNull()

                    val childSummary = childRow?.let {
                        ChildSummary(
                            id = it[ChildrenTable.id].value.toString(),
                            name = it[ChildrenTable.childName],
                            overallProgress = it[ChildrenTable.overallProgress],
                            currentLevel = it[ChildrenTable.currentLevel],
                            attendanceStatus = it[ChildrenTable.attendanceStatus],
                            profilePic = it[ChildrenTable.profilePic]
                        )
                    }

                    // ----- alerts: overdue fees -----
                    val alerts = mutableListOf<DashboardAlert>()
                    val overdueRows = FeeRecordsTable.selectAll()
                        .where { (FeeRecordsTable.parentId eq uid) and (FeeRecordsTable.status eq "OVERDUE") }
                        .toList()
                    if (overdueRows.isNotEmpty()) {
                        val totalOverdue = overdueRows.sumOf { it[FeeRecordsTable.amount] }
                        val currency = overdueRows.first()[FeeRecordsTable.currency]
                        alerts += DashboardAlert(
                            id = "fees_overdue",
                            title = "Fees Due",
                            value = formatMoney(totalOverdue, currency),
                            type = "CRITICAL"
                        )
                    }

                    // INFO alerts can be seeded in app_config under "parent_dashboard_info_alerts"
                    // (JSON array of {id,title,value,type}). Safe noop when absent.
                    val infoAlertsRaw = AppConfigTable.selectAll()
                        .where { AppConfigTable.key eq "parent_dashboard_info_alerts" }
                        .singleOrNull()
                        ?.get(AppConfigTable.value)
                    if (!infoAlertsRaw.isNullOrBlank()) {
                        runCatching {
                            val parsed = kotlinx.serialization.json.Json.decodeFromString(
                                kotlinx.serialization.builtins.ListSerializer(DashboardAlert.serializer()),
                                infoAlertsRaw
                            )
                            alerts.addAll(parsed)
                        }
                    }

                    // ----- featured schools -----
                    val schools = SchoolsTable.selectAll()
                        .where { SchoolsTable.isActive eq true }
                        .limit(5)
                        .map {
                            FeaturedSchool(
                                id = it[SchoolsTable.id].value.toString(),
                                name = it[SchoolsTable.name],
                                rating = 4.5, // operational rating column is in :supplementary_schema; default for now
                                location = it[SchoolsTable.city],
                                image = it[SchoolsTable.logoUrl]
                            )
                        }

                    // ----- curation logic CMS string -----
                    val curationLogic = AppConfigTable.selectAll()
                        .where { AppConfigTable.key eq "parent_dashboard_curation_logic" }
                        .singleOrNull()
                        ?.get(AppConfigTable.value)
                        ?.trim('"')
                        ?: "Curation aligned with NEP 2020 developmental milestones."

                    DashboardResponse(
                        greeting = greeting,
                        childSummary = childSummary,
                        alerts = alerts,
                        featuredSchools = schools,
                        curationLogic = curationLogic
                    )
                }

                call.ok(payload, message = "Dashboard fetched successfully")
            }
        }
    }
}
