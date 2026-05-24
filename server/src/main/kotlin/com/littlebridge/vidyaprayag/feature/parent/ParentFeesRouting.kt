/*
 * File: ParentFeesRouting.kt
 * Module: feature.parent
 *
 * Endpoint: GET /api/v1/parent/fees   (JWT)
 *
 * Spec ref: parent_api_spec.artifact.md §Module: School Management §Screen: Fees
 *
 * Aggregates the parent's fee_records into the stats block expected by the
 * UI and appends fee-related announcements from app_config["parent_fees_announcements"]
 * (so ops can edit deadlines without redeploying).
 *
 * Aggregation rules:
 *   total_collected = SUM(amount WHERE status = 'PAID')
 *   outstanding     = SUM(amount WHERE status IN ('DUE','OVERDUE'))
 *   overdue_count   = COUNT(* WHERE status = 'OVERDUE')
 *   progress        = total_collected / (total_collected + outstanding)
 *                     (returns 0.0 when no rows exist; coerced to [0,1])
 */
package com.littlebridge.vidyaprayag.feature.parent

import com.littlebridge.vidyaprayag.core.fail
import com.littlebridge.vidyaprayag.core.ok
import com.littlebridge.vidyaprayag.core.principalUserId
import com.littlebridge.vidyaprayag.db.AppConfigTable
import com.littlebridge.vidyaprayag.db.DatabaseFactory.dbQuery
import com.littlebridge.vidyaprayag.db.FeeRecordsTable
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.selectAll
import java.util.UUID

@Serializable
data class FeesStats(
    @SerialName("total_collected") val totalCollected: String,
    val progress: Double,
    val outstanding: String,
    @SerialName("overdue_count") val overdueCount: Int
)

@Serializable
data class FeesAnnouncement(
    val id: String,
    val title: String,
    val time: String,
    val desc: String,
    val type: String
)

@Serializable
data class ParentFeesResponse(
    val stats: FeesStats,
    val announcements: List<FeesAnnouncement>
)

private val lenientJson = Json { ignoreUnknownKeys = true; isLenient = true }

private fun money(amount: Double, currency: String): String {
    val symbol = when (currency.uppercase()) {
        "USD" -> "$"
        "INR" -> "₹"
        "EUR" -> "€"
        "GBP" -> "£"
        else  -> "$"
    }
    return "$symbol${"%,d".format(amount.toLong())}"
}

fun Route.parentFeesRouting() {
    authenticate("jwt") {
        route("/api/v1/parent") {
            get("/fees") {
                val uid = call.principalUserId()?.let { runCatching { UUID.fromString(it) }.getOrNull() } ?: run {
                    call.fail("Invalid token", HttpStatusCode.Unauthorized); return@get
                }

                val response = dbQuery {
                    val rows = FeeRecordsTable.selectAll()
                        .where { FeeRecordsTable.parentId eq uid }
                        .toList()

                    val currency = rows.firstOrNull()?.get(FeeRecordsTable.currency) ?: "USD"
                    val collected = rows.filter { it[FeeRecordsTable.status] == "PAID" }
                        .sumOf { it[FeeRecordsTable.amount] }
                    val outstanding = rows.filter { it[FeeRecordsTable.status] in setOf("DUE", "OVERDUE") }
                        .sumOf { it[FeeRecordsTable.amount] }
                    val overdueCount = rows.count { it[FeeRecordsTable.status] == "OVERDUE" }

                    val total = collected + outstanding
                    val progress = if (total <= 0.0) 0.0
                                   else (collected / total).coerceIn(0.0, 1.0)

                    val stats = FeesStats(
                        totalCollected = money(collected, currency),
                        progress = progress,
                        outstanding = money(outstanding, currency),
                        overdueCount = overdueCount
                    )

                    // ----- announcements (CMS) -----
                    val annRaw = AppConfigTable.selectAll()
                        .where { AppConfigTable.key eq "parent_fees_announcements" }
                        .singleOrNull()
                        ?.get(AppConfigTable.value)
                    val announcements: List<FeesAnnouncement> = annRaw?.let {
                        runCatching {
                            lenientJson.decodeFromString(ListSerializer(FeesAnnouncement.serializer()), it)
                        }.getOrNull()
                    } ?: listOf(
                        FeesAnnouncement(
                            id = "f1",
                            title = "Deadline",
                            time = "2h ago",
                            desc = "Submit Q3 fees.",
                            type = "Payment"
                        )
                    )

                    ParentFeesResponse(stats = stats, announcements = announcements)
                }

                call.ok(response, message = "Fee status fetched successfully")
            }
        }
    }
}
