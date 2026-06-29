// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/admin/AdminEfficacyRouting.kt
package com.littlebridge.enrollplus.feature.tutor.admin

import com.littlebridge.enrollplus.core.fail
import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.requireSchoolContext
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.Serializable

/**
 * Routes for Admin Efficacy (cross-role).
 *
 * Endpoints:
 *   GET /tutor/admin/efficacy — school-wide efficacy report
 *
 * Authorization: school role (school_admin/school_staff/admin) via requireSchoolContext.
 * No individual chat content is exposed (privacy by default).
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §10 (Admin efficacy view)
 */
fun Route.adminEfficacyRouting() {

    get("/tutor/admin/efficacy") {
        val ctx = call.requireSchoolContext() ?: return@get

        val service = AdminEfficacyService()
        val efficacy = service.buildSchoolEfficacy(ctx.schoolId)

        call.ok(
            AdminEfficacyResponse(
                schoolId = efficacy.schoolId.toString(),
                totalSessions = efficacy.totalSessions,
                totalDoubtsResolved = efficacy.totalDoubtsResolved,
                totalChildrenServed = efficacy.totalChildrenServed,
                avgMastery = efficacy.avgMastery,
                totalMisconceptionsLogged = efficacy.totalMisconceptionsLogged,
                totalTokensUsed = efficacy.totalTokensUsed,
                costPerImprovementPoint = efficacy.costPerImprovementPoint,
                weakTopicRecoveryRate = efficacy.weakTopicRecoveryRate,
            ),
            "School efficacy report"
        )
    }
}

@Serializable
data class AdminEfficacyResponse(
    val schoolId: String,
    val totalSessions: Int,
    val totalDoubtsResolved: Int,
    val totalChildrenServed: Int,
    val avgMastery: Double,
    val totalMisconceptionsLogged: Int,
    val totalTokensUsed: Int,
    val costPerImprovementPoint: Double,
    val weakTopicRecoveryRate: Double,
)
