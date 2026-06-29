/*
 * File: ParentConferencePackService.kt
 * Module: feature.reportcard.ecosystem
 *
 * ECO 2: Parent Conference Pack — assembles a concise meeting-ready summary
 * for parent-teacher conferences (PTM). Pulls the latest published report card
 * draft, attendance summary, PEWS risk status, and teacher notes into a single
 * structured payload that the app can render as a conference-ready card.
 *
 * SOLID: S (single responsibility: conference pack assembly), D (uses dbQuery).
 */
package com.littlebridge.enrollplus.feature.reportcard.ecosystem

import com.littlebridge.enrollplus.db.ChildrenTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.ReportCardDraftsTable
import com.littlebridge.enrollplus.db.StudentsTable
import com.littlebridge.enrollplus.feature.reportcard.core.ReportCardConstants
import com.littlebridge.enrollplus.feature.reportcard.core.ReportCardKillSwitch
import com.littlebridge.enrollplus.feature.reportcard.narrator.ReportDraft
import com.littlebridge.enrollplus.feature.reportcard.rollup.ReportFactBundle
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.selectAll
import java.util.UUID

class ParentConferencePackService {

    @Serializable
    data class ConferencePack(
        val studentName: String,
        val className: String,
        val section: String,
        val term: String,
        val overallPct: Double?,
        val overallGrade: String?,
        val attendancePct: Int?,
        val movementLabel: String,
        val focusAreas: List<String>,
        val strengths: List<String>,
        val improvementAreas: List<String>,
        val parentSummary: String,
        val teacherNote: String,
        val projectionNote: String,
        val pewsRiskLevel: String?,
        val subjectHighlights: List<SubjectHighlight>,
        val conferenceTips: List<String>,
    )

    @Serializable
    data class SubjectHighlight(
        val subject: String,
        val percentage: Double?,
        val grade: String?,
        val movement: String,
    )

    /**
     * Build a conference pack for a parent's child.
     * Uses the latest published report card draft.
     */
    suspend fun buildPack(
        schoolId: UUID,
        parentUserId: UUID,
        childId: UUID,
    ): ConferencePack? {
        ReportCardKillSwitch.require(ReportCardConstants.MODULE_ASSEMBLE)

        // Resolve child → student
        val child = dbQuery {
            ChildrenTable.selectAll().where {
                (ChildrenTable.id eq childId) and
                (ChildrenTable.parentId eq parentUserId) and
                (ChildrenTable.isActive eq true)
            }.singleOrNull()
        } ?: return null

        val studentCode = child[ChildrenTable.studentCode] ?: return null
        val student = dbQuery {
            StudentsTable.selectAll().where {
                (StudentsTable.schoolId eq schoolId) and
                (StudentsTable.studentCode eq studentCode) and
                (StudentsTable.isActive eq true)
            }.singleOrNull()
        } ?: return null

        val studentId = student[StudentsTable.id].value
        val studentName = student[StudentsTable.fullName]

        // Get latest published draft
        val draftRow = dbQuery {
            ReportCardDraftsTable.selectAll().where {
                (ReportCardDraftsTable.schoolId eq schoolId) and
                (ReportCardDraftsTable.studentId eq studentId) and
                (ReportCardDraftsTable.status eq ReportCardConstants.DraftStatus.PUBLISHED)
            }.orderBy(ReportCardDraftsTable.publishedAt, SortOrder.DESC)
                .firstOrNull()
        } ?: return null

        val bundle = ReportFactBundle.fromJson(draftRow[ReportCardDraftsTable.factBundle])
        val draft = ReportDraft.fromJson(draftRow[ReportCardDraftsTable.aiDraft] ?: "{}")

        val subjectHighlights = bundle.subjects.map { sf ->
            SubjectHighlight(
                subject = sf.subject,
                percentage = sf.percentage,
                grade = sf.grade,
                movement = sf.movement,
            )
        }

        // Generate conference tips based on data
        val tips = mutableListOf<String>()
        if (bundle.overallPct != null && bundle.overallPct < 50.0) {
            tips.add("Discuss additional support needs for overall improvement")
        }
        if (bundle.attendancePct != null && bundle.attendancePct < 75) {
            tips.add("Address attendance concerns — below 75% impacts learning continuity")
        }
        val weakSubjects = bundle.subjects.filter { it.percentage != null && it.percentage < 50.0 }
        if (weakSubjects.isNotEmpty()) {
            tips.add("Focus on: ${weakSubjects.joinToString(", ") { it.subject }}")
        }
        val strongSubjects = bundle.subjects.filter { it.percentage != null && it.percentage >= 75.0 }
        if (strongSubjects.isNotEmpty()) {
            tips.add("Encourage continued excellence in: ${strongSubjects.joinToString(", ") { it.subject }}")
        }
        if (bundle.trajectoryLabel == ReportCardConstants.MovementPattern.SLID) {
            tips.add("Discuss factors contributing to recent decline and remediation plan")
        } else if (bundle.trajectoryLabel == ReportCardConstants.MovementPattern.IMPROVED) {
            tips.add("Acknowledge improvement and discuss how to sustain momentum")
        }
        if (tips.isEmpty()) {
            tips.add("Continue regular engagement and monitor progress")
        }

        return ConferencePack(
            studentName = studentName,
            className = draftRow[ReportCardDraftsTable.className],
            section = draftRow[ReportCardDraftsTable.section],
            term = draftRow[ReportCardDraftsTable.term],
            overallPct = bundle.overallPct,
            overallGrade = bundle.overallGrade,
            attendancePct = bundle.attendancePct,
            movementLabel = bundle.trajectoryLabel,
            focusAreas = draft?.focusAreas ?: emptyList(),
            strengths = draft?.strengths ?: emptyList(),
            improvementAreas = draft?.improvementAreas ?: emptyList(),
            parentSummary = draft?.parentSummary ?: "",
            teacherNote = draft?.teacherNote ?: "",
            projectionNote = draft?.projectionNote ?: "",
            pewsRiskLevel = bundle.pewsRiskLevel,
            subjectHighlights = subjectHighlights,
            conferenceTips = tips,
        )
    }
}
