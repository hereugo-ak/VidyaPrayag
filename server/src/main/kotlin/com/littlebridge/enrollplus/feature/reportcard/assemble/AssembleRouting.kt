// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/reportcard/assemble/AssembleRouting.kt
package com.littlebridge.enrollplus.feature.reportcard.assemble

import com.littlebridge.enrollplus.core.fail
import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.okMessage
import com.littlebridge.enrollplus.core.principalUserUuid
import com.littlebridge.enrollplus.core.requireSchoolContext
import com.littlebridge.enrollplus.core.requireTeacherContext
import com.littlebridge.enrollplus.db.AppUsersTable
import com.littlebridge.enrollplus.db.ChildrenTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.StudentsTable
import com.littlebridge.enrollplus.feature.reportcard.core.ReportCardConfig
import com.littlebridge.enrollplus.feature.reportcard.data.ReportCardDraftRepository
import com.littlebridge.enrollplus.feature.reportcard.queue.ReportCardJob
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import java.util.UUID

/**
 * Routes for Tier 3 — Assembly & Publish.
 *
 * Endpoints:
 *   POST   /report-card/generate              — trigger batch generation for a class
 *   GET    /report-card/review-queue           — get drafts awaiting review
 *   GET    /report-card/drafts/{id}            — get a single draft
 *   PUT    /report-card/drafts/{id}/edit       — teacher edits draft narrative
 *   POST   /report-card/drafts/{id}/approve    — teacher approves a draft
 *   POST   /report-card/drafts/{id}/regenerate — teacher regenerates one student's narrative
 *   POST   /report-card/bulk-approve           — teacher bulk-approves drafts
 *   POST   /report-card/publish                — admin publishes approved drafts
 *   GET    /report-card/oversight              — admin: review status across classes
 *   GET    /report-card/published               — parent gets published reports (parent auth)
 *   POST   /report-card/marks-stale             — internal: mark drafts stale on marks change
 *
 * SOLID: S (routes only — no business logic here), D (service injected).
 */
fun Route.assembleRouting(service: ReportAssemblyService) {

    // ── Teacher: trigger batch generation ──────────────────────────────
    post("/report-card/generate") {
        val ctx = call.requireTeacherContext() ?: return@post
        val body = call.receive<GenerateRequest>()
        val lang = body.language ?: service.resolveLanguagePref(ctx.userId)
        val result = service.generateForClass(
            schoolId = ctx.schoolId,
            className = body.className,
            section = body.section,
            term = body.term,
            academicYearId = body.academicYearId?.let { runCatching { UUID.fromString(it) }.getOrNull() },
            language = lang,
            createdBy = ctx.userId,
        )
        call.ok(result, "Batch generation complete")
    }

    // ── Teacher/Admin: check async job status ──────────────────────────
    get("/report-card/jobs/{jobId}") {
        val jobId = call.parameters["jobId"]?.let {
            runCatching { UUID.fromString(it) }.getOrNull()
        } ?: return@get call.fail("Invalid job ID", HttpStatusCode.BadRequest)
        val status = ReportCardJob.status(jobId)
            ?: return@get call.fail("Job not found", HttpStatusCode.NotFound)
        call.ok(status, "Job status: ${status.status}")
    }

    // ── Teacher: get review queue ──────────────────────────────────────
    get("/report-card/review-queue") {
        val ctx = call.requireTeacherContext() ?: return@get
        val className = call.request.queryParameters["className"]
            ?: return@get call.fail("className parameter required", HttpStatusCode.BadRequest)
        val section = call.request.queryParameters["section"] ?: "A"
        val term = call.request.queryParameters["term"]
            ?: return@get call.fail("term parameter required", HttpStatusCode.BadRequest)
        val academicYearId = call.request.queryParameters["academicYearId"]?.let {
            runCatching { UUID.fromString(it) }.getOrNull()
        }

        val drafts = service.reviewQueue(ctx.schoolId, className, section, term, academicYearId)
        call.ok(drafts.map { it.toDto() }, "Review queue (${drafts.size} drafts)")
    }

    // ── Teacher/Admin: get single draft ────────────────────────────────
    get("/report-card/drafts/{id}") {
        val ctx = call.requireTeacherContext() ?: return@get
        val draftId = call.parameters["id"]?.let {
            runCatching { UUID.fromString(it) }.getOrNull()
        } ?: return@get call.fail("Invalid draft ID", HttpStatusCode.BadRequest)

        val draft = service.getDraft(draftId)
            ?: return@get call.fail("Draft not found", HttpStatusCode.NotFound)
        call.ok(draft.toDto(), "Draft retrieved")
    }

    // ── Teacher: edit draft ────────────────────────────────────────────
    put("/report-card/drafts/{id}/edit") {
        val ctx = call.requireTeacherContext() ?: return@put
        val draftId = call.parameters["id"]?.let {
            runCatching { UUID.fromString(it) }.getOrNull()
        } ?: return@put call.fail("Invalid draft ID", HttpStatusCode.BadRequest)

        val body = call.receive<EditDraftRequest>()
        val success = service.editDraft(draftId, body.draftJson, ctx.userId)
        if (success) call.okMessage("Draft edited successfully")
        else call.fail("Failed to edit draft — may not exist", HttpStatusCode.NotFound)
    }

    // ── Teacher: approve draft ─────────────────────────────────────────
    post("/report-card/drafts/{id}/approve") {
        val ctx = call.requireTeacherContext() ?: return@post
        val draftId = call.parameters["id"]?.let {
            runCatching { UUID.fromString(it) }.getOrNull()
        } ?: return@post call.fail("Invalid draft ID", HttpStatusCode.BadRequest)

        val success = service.approveDraft(draftId, ctx.userId)
        if (success) call.okMessage("Draft approved")
        else call.fail("Cannot approve — draft may not be in draft/flagged status", HttpStatusCode.BadRequest)
    }

    // ── Teacher: regenerate one student's narrative ───────────────────
    post("/report-card/drafts/{id}/regenerate") {
        val ctx = call.requireTeacherContext() ?: return@post
        val draftId = call.parameters["id"]?.let {
            runCatching { UUID.fromString(it) }.getOrNull()
        } ?: return@post call.fail("Invalid draft ID", HttpStatusCode.BadRequest)

        val result = service.regenerateForStudent(ctx.schoolId, draftId, ctx.userId)
        if (result.success) call.ok(result, "Draft regenerated successfully")
        else call.fail(result.error ?: "Regeneration failed", HttpStatusCode.BadRequest)
    }

    // ── Teacher: bulk approve ──────────────────────────────────────────
    post("/report-card/bulk-approve") {
        val ctx = call.requireTeacherContext() ?: return@post
        val body = call.receive<BulkApproveRequest>()
        val count = service.bulkApprove(
            body.draftIds.mapNotNull { runCatching { UUID.fromString(it) }.getOrNull() },
            ctx.userId,
        )
        call.ok(BulkApproveResult(count), "$count drafts approved")
    }

    // ── Admin: publish for class ───────────────────────────────────────
    post("/report-card/publish") {
        val ctx = call.requireSchoolContext() ?: return@post
        val body = call.receive<PublishRequest>()
        val count = service.publishForClass(
            schoolId = ctx.schoolId,
            className = body.className,
            section = body.section,
            term = body.term,
            academicYearId = body.academicYearId?.let { runCatching { UUID.fromString(it) }.getOrNull() },
            publishedBy = ctx.userId,
        )
        call.ok(PublishResult(count), "$count reports published")
    }

    // ── Admin: oversight — review status across all classes ───────────
    get("/report-card/oversight") {
        val ctx = call.requireSchoolContext() ?: return@get
        val term = call.request.queryParameters["term"]
            ?: ReportCardConfig.currentTerm
        val academicYearId = call.request.queryParameters["academicYearId"]?.let {
            runCatching { UUID.fromString(it) }.getOrNull()
        }
        if (term == null) {
            call.ok(
                ReportAssemblyService.OversightSummary(ctx.schoolId, emptyList()),
                "No current term configured"
            )
            return@get
        }
        val summary = service.getOversightSummary(ctx.schoolId, term, academicYearId)
        call.ok(summary, "Oversight summary (${summary.classes.size} classes)")
    }

    // ── Internal: mark drafts stale on marks change ───────────────────
    post("/report-card/marks-stale") {
        val ctx = call.requireTeacherContext() ?: return@post
        val body = call.receive<MarksStaleRequest>()
        service.markDraftsStaleOnMarksChange(ctx.schoolId, body.className, body.section, body.term)
        call.okMessage("Drafts marked stale")
    }

    // ── Admin: term window config ──────────────────────────────────────
    get("/report-card/term-config") {
        val ctx = call.requireSchoolContext() ?: return@get
        call.ok(
            TermConfigDto(
                currentTerm = ReportCardConfig.currentTerm,
                termWindowDays = ReportCardConfig.termWindowDays,
                enabled = ReportCardConfig.enabled,
                batchConcurrency = ReportCardConfig.batchConcurrency,
                fallbackOnAiFail = ReportCardConfig.fallbackOnAiFail,
            ),
            "Term configuration"
        )
    }

    // ── Admin: update term config ─────────────────────────────────────
    put("/report-card/term-config") {
        val ctx = call.requireSchoolContext() ?: return@put
        val body = call.receive<UpdateTermConfigRequest>()
        ReportCardConfig.updateConfig(
            currentTerm = body.currentTerm,
            termWindowDays = body.termWindowDays,
            enabled = body.enabled,
            fallbackOnAiFail = body.fallbackOnAiFail,
        )
        call.ok(
            TermConfigDto(
                currentTerm = ReportCardConfig.currentTerm,
                termWindowDays = ReportCardConfig.termWindowDays,
                enabled = ReportCardConfig.enabled,
                batchConcurrency = ReportCardConfig.batchConcurrency,
                fallbackOnAiFail = ReportCardConfig.fallbackOnAiFail,
            ),
            "Term configuration updated"
        )
    }

    // ── Parent: get published reports for their child ─────────────────
    get("/report-card/published") {
        val uid = call.principalUserUuid() ?: run {
            call.fail("Invalid token", HttpStatusCode.Unauthorized); return@get
        }
        val childId = call.request.queryParameters["childId"]?.let {
            runCatching { UUID.fromString(it) }.getOrNull()
        } ?: return@get call.fail("childId parameter required", HttpStatusCode.BadRequest)
        val academicYearId = call.request.queryParameters["academicYearId"]?.let {
            runCatching { UUID.fromString(it) }.getOrNull()
        }

        // Resolve the schoolId from the parent's child record
        val schoolId = dbQuery {
            ChildrenTable.selectAll().where {
                (ChildrenTable.id eq childId) and
                (ChildrenTable.parentId eq uid) and
                (ChildrenTable.isActive eq true)
            }.firstOrNull()?.get(ChildrenTable.schoolId)
        } ?: return@get call.fail("Child not found or not linked to your account", HttpStatusCode.NotFound)

        val reports = service.getPublishedForParent(schoolId, uid, childId, academicYearId)
        call.ok(reports, "Published reports (${reports.size})")
    }
}

// ── DTOs ────────────────────────────────────────────────────────────────

@Serializable
data class GenerateRequest(
    val className: String,
    val section: String = "A",
    val term: String,
    val academicYearId: String? = null,
    val language: String? = null,
)

@Serializable
data class EditDraftRequest(
    val draftJson: String,
)

@Serializable
data class BulkApproveRequest(
    val draftIds: List<String>,
)

@Serializable
data class BulkApproveResult(val approved: Int)

@Serializable
data class PublishRequest(
    val className: String,
    val section: String = "A",
    val term: String,
    val academicYearId: String? = null,
)

@Serializable
data class PublishResult(val published: Int)

@Serializable
data class MarksStaleRequest(
    val className: String,
    val section: String,
    val term: String,
)

@Serializable
data class TermConfigDto(
    val currentTerm: String?,
    val termWindowDays: Int,
    val enabled: Boolean,
    val batchConcurrency: Int,
    val fallbackOnAiFail: Boolean,
)

@Serializable
data class UpdateTermConfigRequest(
    val currentTerm: String? = null,
    val termWindowDays: Int? = null,
    val enabled: Boolean? = null,
    val fallbackOnAiFail: Boolean? = null,
)

@Serializable
data class DraftDto(
    val id: String,
    val studentId: String,
    val className: String,
    val section: String,
    val term: String,
    val academicYearId: String?,
    val aiDraft: String?,
    val classContext: String?,
    val status: String,
    val aiProviderUsed: String?,
    val tokensUsed: Int,
    val language: String,
    val groundingFlags: String?,
    val createdAt: String,
    val updatedAt: String,
)

fun ReportCardDraftRepository.DraftRow.toDto() = DraftDto(
    id = id.toString(),
    studentId = studentId.toString(),
    className = className,
    section = section,
    term = term,
    academicYearId = academicYearId?.toString(),
    aiDraft = aiDraft,
    classContext = classContext,
    status = status,
    aiProviderUsed = aiProviderUsed,
    tokensUsed = tokensUsed,
    language = language,
    groundingFlags = groundingFlags,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)
