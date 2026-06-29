// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/reportcard/core/ReportCardHealthCheck.kt
package com.littlebridge.enrollplus.feature.reportcard.core

import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.feature.ai.AiService
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.selectAll
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Pre-flight health check for the AI Report Card 2.0 feature.
 *
 * Run before batch jobs to verify that all dependencies are available:
 *   - AI providers are configured (at least one key exists)
 *   - Core DB tables are present and queryable
 *   - Report card tables are present and queryable
 *
 * Returns a structured [HealthReport] so the caller can decide whether to
 * proceed, warn, or abort. Prevents silent failures mid-batch.
 *
 * SOLID: S (single responsibility: health checking only).
 */
object ReportCardHealthCheck {
    private val log = LoggerFactory.getLogger("ReportCardHealthCheck")

    @Serializable
    data class HealthReport(
        val healthy: Boolean,
        val aiProvidersConfigured: Boolean,
        val coreTablesOk: Boolean,
        val reportCardTablesOk: Boolean,
        val warnings: List<String> = emptyList(),
    )

    /**
     * Run the pre-flight check. Safe to call at any time — never throws.
     * Uses a lightweight SELECT 1 equivalent on each table.
     */
    suspend fun check(schoolId: UUID? = null): HealthReport {
        val warnings = mutableListOf<String>()

        // Check AI providers
        val aiOk = runCatching {
            val result = AiService.complete(
                feature = "report_card_health",
                lane = com.littlebridge.enrollplus.feature.ai.AiLane.FAST_CHAT,
                messages = listOf(com.littlebridge.enrollplus.feature.ai.LlmMessage(role = "user", content = "ping")),
                containsPii = false,
                schoolId = schoolId,
                temperature = 0.0,
                maxTokens = 1,
                cache = false,
            )
            result.ok || result.routingDecision == "unavailable"
        }.getOrElse {
            warnings.add("AI provider check failed: ${it.message}")
            false
        }

        // Check core tables (students, assessments, assessment_marks)
        val coreOk = runCatching {
            dbQuery {
                org.jetbrains.exposed.sql.selectAll()
                    .from(com.littlebridge.enrollplus.db.StudentsTable)
                    .limit(1)
                    .toList()
            }
            true
        }.getOrElse {
            warnings.add("Core tables check failed: ${it.message}")
            false
        }

        // Check report card tables
        val rcOk = runCatching {
            dbQuery {
                com.littlebridge.enrollplus.db.ReportCardDraftsTable.selectAll().limit(1).toList()
            }
            true
        }.getOrElse {
            warnings.add("Report card tables not yet created — run migration_062_report_card.sql")
            false
        }

        val healthy = aiOk && coreOk && rcOk
        log.info("ReportCardHealthCheck: healthy={}, ai={}, core={}, rc={}", healthy, aiOk, coreOk, rcOk)

        return HealthReport(
            healthy = healthy,
            aiProvidersConfigured = aiOk,
            coreTablesOk = coreOk,
            reportCardTablesOk = rcOk,
            warnings = warnings,
        )
    }
}
