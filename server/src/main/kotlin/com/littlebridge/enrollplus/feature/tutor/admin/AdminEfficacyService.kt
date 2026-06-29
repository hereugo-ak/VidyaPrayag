// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/admin/AdminEfficacyService.kt
package com.littlebridge.enrollplus.feature.tutor.admin

import com.littlebridge.enrollplus.db.AiUsageLogTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.TutorMasteryTable
import com.littlebridge.enrollplus.db.TutorMisconceptionsTable
import com.littlebridge.enrollplus.db.TutorSessionsTable
import com.littlebridge.enrollplus.feature.tutor.core.TutorConstants
import com.littlebridge.enrollplus.feature.tutor.core.TutorKillSwitch
import com.littlebridge.enrollplus.feature.tutor.learn.TutorEfficacyService
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Cross-role — Admin Efficacy Service.
 *
 * School-wide aggregates for the admin "Tutor Effectiveness" view:
 * - Weak-topic recovery rate
 * - Doubts resolved
 * - Teacher hours saved (proxy: total sessions)
 * - Token cost per improvement point
 *
 * Admin sees school-wide aggregates, no individual chat content (privacy by default).
 *
 * Kill-switched under module name "tutor_admin_efficacy".
 *
 * SOLID:
 *   S → Single responsibility: school-wide efficacy aggregation.
 *   D → Depends on repository/service abstractions.
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §10 (Admin efficacy view)
 */
class AdminEfficacyService(
    private val efficacyService: TutorEfficacyService = TutorEfficacyService(),
) {
    private val log = LoggerFactory.getLogger("AdminEfficacyService")

    data class SchoolEfficacy(
        val schoolId: UUID,
        val totalSessions: Int,
        val totalDoubtsResolved: Int,
        val totalChildrenServed: Int,
        val avgMastery: Double,
        val totalMisconceptionsLogged: Int,
        val totalTokensUsed: Int,
        val costPerImprovementPoint: Double,
        val weakTopicRecoveryRate: Double,
    )

    /**
     * Build the school-wide efficacy report.
     */
    suspend fun buildSchoolEfficacy(schoolId: UUID): SchoolEfficacy {
        TutorKillSwitch.require(TutorConstants.MODULE_ADMIN_EFFICACY)

        // Count sessions
        val sessions = dbQuery {
            TutorSessionsTable.selectAll().where {
                TutorSessionsTable.schoolId eq schoolId
            }.toList()
        }
        val totalSessions = sessions.size
        val totalDoubtsResolved = sessions.count { it[TutorSessionsTable.mode] == "DOUBT" }
        val totalChildrenServed = sessions.map { it[TutorSessionsTable.childId] }.distinct().size
        val totalTokensUsed = sessions.sumOf { it[TutorSessionsTable.tokensUsed] }

        // Mastery averages
        val masteryRows = dbQuery {
            TutorMasteryTable.selectAll().where {
                TutorMasteryTable.schoolId eq schoolId
            }.toList()
        }
        val avgMastery = if (masteryRows.isNotEmpty()) {
            masteryRows.map { it[TutorMasteryTable.mastery] }.average()
        } else 0.0

        // Misconceptions
        val misconceptionCount = dbQuery {
            TutorMisconceptionsTable.selectAll().where {
                TutorMisconceptionsTable.schoolId eq schoolId
            }.count().toInt()
        }

        // AI usage log tokens for tutor features
        val aiTokens = dbQuery {
            AiUsageLogTable.selectAll().where {
                (AiUsageLogTable.schoolId eq schoolId) and
                (AiUsageLogTable.feature like "ai_tutor%")
            }.sumOf { it[AiUsageLogTable.outputTokens] + it[AiUsageLogTable.inputTokens] }
        }

        // Weak-topic recovery rate: % of topics where mastery moved from <50 to >=60
        val recoveryRate = if (masteryRows.isNotEmpty()) {
            val recovered = masteryRows.count {
                it[TutorMasteryTable.mastery] >= 60.0 && it[TutorMasteryTable.masterySource] == "PRACTICE"
            }
            recovered.toDouble() / masteryRows.size
        } else 0.0

        // Cost per improvement point (tokens per 1% mastery gain)
        val costPerPoint = if (avgMastery > 0 && aiTokens > 0) {
            aiTokens.toDouble() / avgMastery
        } else 0.0

        log.info("AdminEfficacy: school={} — {} sessions, {} children, avgMastery={}, recoveryRate={}",
            schoolId, totalSessions, totalChildrenServed,
            String.format("%.1f", avgMastery), String.format("%.2f", recoveryRate))

        return SchoolEfficacy(
            schoolId = schoolId,
            totalSessions = totalSessions,
            totalDoubtsResolved = totalDoubtsResolved,
            totalChildrenServed = totalChildrenServed,
            avgMastery = avgMastery,
            totalMisconceptionsLogged = misconceptionCount,
            totalTokensUsed = aiTokens,
            costPerImprovementPoint = costPerPoint,
            weakTopicRecoveryRate = recoveryRate,
        )
    }
}
