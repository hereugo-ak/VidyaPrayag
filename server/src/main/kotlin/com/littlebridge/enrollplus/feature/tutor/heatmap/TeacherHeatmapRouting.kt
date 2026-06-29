// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/heatmap/TeacherHeatmapRouting.kt
package com.littlebridge.enrollplus.feature.tutor.heatmap

import com.littlebridge.enrollplus.core.fail
import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.requireSchoolContext
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Routes for Teacher Heatmap (cross-role).
 *
 * Endpoints:
 *   GET /tutor/heatmap/scope            — get teacher's assigned classes+subjects
 *   GET /tutor/heatmap/{classId}/{subjectId} — get the misconception heatmap
 *
 * Authorization: school role (school_admin/school_staff/admin) via requireSchoolContext.
 * Teacher scoping: only classes/subjects in TeacherSubjectAssignmentsTable.
 *
 * SOLID: S (routes only — no business logic), D (service injected).
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §10 (Teacher heatmap)
 */
fun Route.heatmapRouting() {

    get("/tutor/heatmap/scope") {
        val ctx = call.requireSchoolContext() ?: return@get

        val service = TeacherHeatmapService()
        val scope = service.getTeacherScope(ctx.schoolId, ctx.userId)

        call.ok(
            scope.map { s ->
                TeacherScopeResponse(
                    classId = s.classId.toString(),
                    className = s.className,
                    subjectId = s.subjectId.toString(),
                    subjectName = s.subjectName,
                )
            },
            "Teacher scope (${scope.size} assignments)"
        )
    }

    get("/tutor/heatmap/{classId}/{subjectId}") {
        val ctx = call.requireSchoolContext() ?: return@get
        val classIdStr = call.parameters["classId"]
            ?: return@get call.fail("classId required", HttpStatusCode.BadRequest, "BAD_CLASS_ID")
        val subjectIdStr = call.parameters["subjectId"]
            ?: return@get call.fail("subjectId required", HttpStatusCode.BadRequest, "BAD_SUBJECT_ID")
        val classId = runCatching { UUID.fromString(classIdStr) }.getOrNull()
            ?: return@get call.fail("Invalid classId", HttpStatusCode.BadRequest, "BAD_CLASS_ID")
        val subjectId = runCatching { UUID.fromString(subjectIdStr) }.getOrNull()
            ?: return@get call.fail("Invalid subjectId", HttpStatusCode.BadRequest, "BAD_SUBJECT_ID")

        val service = TeacherHeatmapService()
        val heatmap = service.buildHeatmap(ctx.schoolId, classId, subjectId)

        call.ok(
            HeatmapResponse(
                classId = heatmap.classId.toString(),
                subjectId = heatmap.subjectId.toString(),
                totalChildren = heatmap.totalChildren,
                totalMisconceptions = heatmap.totalMisconceptions,
                cells = heatmap.cells.map { c ->
                    HeatmapCellResponse(
                        topicId = c.topicId.toString(),
                        misconceptionType = c.misconceptionType,
                        affectedChildren = c.affectedChildren,
                        evidence = c.evidence,
                        severity = c.severity,
                    )
                }
            ),
            "Heatmap (${heatmap.cells.size} cells)"
        )
    }
}

@Serializable
data class TeacherScopeResponse(
    val classId: String,
    val className: String,
    val subjectId: String,
    val subjectName: String,
)

@Serializable
data class HeatmapResponse(
    val classId: String,
    val subjectId: String,
    val totalChildren: Int,
    val totalMisconceptions: Int,
    val cells: List<HeatmapCellResponse>,
)

@Serializable
data class HeatmapCellResponse(
    val topicId: String,
    val misconceptionType: String,
    val affectedChildren: Int,
    val evidence: List<String>,
    val severity: String,
)
