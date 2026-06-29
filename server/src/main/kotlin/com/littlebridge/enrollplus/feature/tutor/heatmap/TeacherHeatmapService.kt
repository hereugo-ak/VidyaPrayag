// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/heatmap/TeacherHeatmapService.kt
package com.littlebridge.enrollplus.feature.tutor.heatmap

import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.TutorMisconceptionsTable
import com.littlebridge.enrollplus.feature.tutor.core.TutorConstants
import com.littlebridge.enrollplus.feature.tutor.core.TutorKillSwitch
import com.littlebridge.enrollplus.feature.tutor.data.TutorMisconceptionRepository
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Cross-role — Teacher Heatmap Service.
 *
 * Aggregates misconceptions and mastery data into a class-wide heatmap
 * for the teacher. Shows which topics are tripping up the most kids,
 * *before the test* — the single most valuable thing the system can
 * tell a teacher.
 *
 * Teacher scoping: only classes/subjects in TeacherSubjectAssignmentsTable.
 *
 * Kill-switched under module name "tutor_heatmap".
 *
 * SOLID:
 *   S → Single responsibility: aggregate class-wide misconception data.
 *   D → Depends on repository abstractions.
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §8.2, §10 (Teacher heatmap)
 */
class TeacherHeatmapService(
    private val misconceptionRepo: TutorMisconceptionRepository = TutorMisconceptionRepository(),
) {
    private val log = LoggerFactory.getLogger("TeacherHeatmapService")

    data class HeatmapCell(
        val topicId: UUID,
        val misconceptionType: String,
        val affectedChildren: Int,
        val evidence: List<String>,
        val severity: String,          // high | medium | low
    )

    data class ClassHeatmap(
        val classId: UUID,
        val subjectId: UUID,
        val cells: List<HeatmapCell>,
        val totalChildren: Int,
        val totalMisconceptions: Int,
    )

    /**
     * Build the misconception heatmap for a class+subject.
     * Aggregates by (topicId, misconceptionType) and counts affected children.
     */
    suspend fun buildHeatmap(
        schoolId: UUID,
        classId: UUID,
        subjectId: UUID,
    ): ClassHeatmap {
        TutorKillSwitch.require(TutorConstants.MODULE_TEACHER_HEATMAP)

        val misconceptions = misconceptionRepo.findByClassAndSubject(classId, subjectId)

        // Group by (topicId, misconceptionType)
        val grouped = misconceptions.groupBy { "${it.topicId}|${it.misconceptionType}" }

        val cells = grouped.map { (_, rows) ->
            val first = rows.first()
            val affectedChildren = rows.map { it.childId }.distinct().size
            HeatmapCell(
                topicId = first.topicId,
                misconceptionType = first.misconceptionType,
                affectedChildren = affectedChildren,
                evidence = rows.take(3).map { it.evidence }.filter { it.isNotEmpty() },
                severity = when {
                    affectedChildren >= 10 -> "high"
                    affectedChildren >= 5 -> "medium"
                    else -> "low"
                },
            )
        }.sortedByDescending { it.affectedChildren }

        log.info("TeacherHeatmap: built heatmap for class={} subject={} — {} cells, {} total misconceptions",
            classId, subjectId, cells.size, misconceptions.size)

        return ClassHeatmap(
            classId = classId,
            subjectId = subjectId,
            cells = cells,
            totalChildren = misconceptions.map { it.childId }.distinct().size,
            totalMisconceptions = misconceptions.size,
        )
    }

    /**
     * Get the teacher's assigned classes+subjects for scoping.
     * Only classes/subjects the teacher is assigned to should be visible.
     */
    suspend fun getTeacherScope(
        schoolId: UUID,
        teacherId: UUID,
    ): List<TeacherScopeItem> = dbQuery {
        com.littlebridge.enrollplus.db.TeacherSubjectAssignmentsTable.selectAll().where {
            (com.littlebridge.enrollplus.db.TeacherSubjectAssignmentsTable.schoolId eq schoolId) and
            (com.littlebridge.enrollplus.db.TeacherSubjectAssignmentsTable.teacherId eq teacherId) and
            (com.littlebridge.enrollplus.db.TeacherSubjectAssignmentsTable.isActive eq true)
        }.mapNotNull { row ->
            val classId = row[com.littlebridge.enrollplus.db.TeacherSubjectAssignmentsTable.classId]
            val subjectId = row[com.littlebridge.enrollplus.db.TeacherSubjectAssignmentsTable.subjectId]
            if (classId != null && subjectId != null) {
                TeacherScopeItem(
                    classId = classId,
                    className = row[com.littlebridge.enrollplus.db.TeacherSubjectAssignmentsTable.className],
                    subjectId = subjectId,
                    subjectName = row[com.littlebridge.enrollplus.db.TeacherSubjectAssignmentsTable.subject],
                )
            } else null
        }
    }

    data class TeacherScopeItem(
        val classId: UUID,
        val className: String,
        val subjectId: UUID,
        val subjectName: String,
    )
}
