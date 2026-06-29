// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/reportcard/assemble/ReportAssemblyService.kt
package com.littlebridge.enrollplus.feature.reportcard.assemble

import com.littlebridge.enrollplus.db.AiResponseCacheTable
import com.littlebridge.enrollplus.db.AppUsersTable
import com.littlebridge.enrollplus.db.ChildrenTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.ReportCardDraftsTable
import com.littlebridge.enrollplus.db.StudentsTable
import com.littlebridge.enrollplus.feature.notifications.Notify
import com.littlebridge.enrollplus.feature.notifications.NotifyRecipients
import com.littlebridge.enrollplus.feature.pews.core.AuditLogger
import com.littlebridge.enrollplus.feature.reportcard.core.ReportCardConstants
import com.littlebridge.enrollplus.feature.reportcard.core.ReportCardConfig
import com.littlebridge.enrollplus.feature.reportcard.core.ReportCardKillSwitch
import com.littlebridge.enrollplus.feature.reportcard.data.ReportCardDraftRepository
import com.littlebridge.enrollplus.feature.reportcard.narrator.NarratorService
import com.littlebridge.enrollplus.feature.reportcard.narrator.ReportDraft
import com.littlebridge.enrollplus.feature.reportcard.rollup.ReportFactBundle
import com.littlebridge.enrollplus.feature.reportcard.rollup.ReportFactBundle.Companion.hash
import com.littlebridge.enrollplus.feature.reportcard.rollup.ReportFactBundle.Companion.toJson
import com.littlebridge.enrollplus.feature.reportcard.rollup.ReportRollupService
import com.littlebridge.enrollplus.feature.reportcard.triage.ReportTriageService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Tier 3 — Assembly & Publish.
 *
 * Orchestrates the full pipeline: Rollup → Triage → Narrator → Draft storage.
 * Manages the draft→review→publish state machine. Runs as an async batch job
 * to avoid blocking calls and handle large volumes.
 *
 * State machine:
 *   draft → flagged_for_review → approved → published → archived
 *
 * Never auto-published. Teacher review and admin publish are mandatory.
 *
 * SOLID:
 *   S → Single responsibility: orchestration + state machine.
 *   D → All tier services injected.
 *
 * Kill switch: [KillSwitchGuard.require] at entry with "reportcard_assemble".
 */
class ReportAssemblyService(
    private val rollupService: ReportRollupService = ReportRollupService(),
    private val triageService: ReportTriageService = ReportTriageService(),
    private val narratorService: NarratorService = NarratorService(triageService),
    private val draftRepo: ReportCardDraftRepository = ReportCardDraftRepository(),
) {
    private val log = LoggerFactory.getLogger("ReportAssemblyService")
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    data class BatchResult(
        val jobId: UUID,
        val totalStudents: Int,
        val completed: Int,
        val failed: Int,
        val grounded: Int,
        val flagged: Int,
        val fallbackUsed: Int,
        val errors: List<String> = emptyList(),
    )

    @Serializable
    data class RegenerateResult(
        val draftId: UUID,
        val studentId: UUID,
        val success: Boolean,
        val grounded: Boolean,
        val fallbackUsed: Boolean,
        val error: String? = null,
    )

    @Serializable
    data class ClassOversightRow(
        val className: String,
        val section: String,
        val term: String,
        val totalDrafts: Int,
        val draftCount: Int,
        val flaggedCount: Int,
        val approvedCount: Int,
        val publishedCount: Int,
    )

    @Serializable
    data class OversightSummary(
        val schoolId: UUID,
        val classes: List<ClassOversightRow>,
    )

    @Serializable
    data class ParentReportDto(
        val id: String,
        val term: String,
        val className: String,
        val section: String,
        val aiDraft: String?,
        val classContext: String?,
        val language: String,
        val publishedAt: String?,
        val groundingFlags: String?,
    )

    /**
     * Generate report card drafts for an entire class.
     *
     * Runs the full pipeline: Rollup → Triage → Narrator → Store.
     * Uses bounded concurrency to avoid overwhelming AI providers.
     *
     * @param schoolId       School UUID
     * @param className      Class name (e.g. "Class 8")
     * @param section        Section (e.g. "A")
     * @param term           Term label (e.g. "Term 1")
     * @param academicYearId Academic year UUID
     * @param language       Narrative language
     * @param createdBy      User ID of the teacher/admin who triggered the batch
     */
    suspend fun generateForClass(
        schoolId: UUID,
        className: String,
        section: String,
        term: String,
        academicYearId: UUID?,
        language: String = "hi",
        createdBy: UUID,
    ): BatchResult {
        ReportCardKillSwitch.require(ReportCardConstants.MODULE_ASSEMBLE)
        log.info("Assembly: generating for class {}-{}, school={}, term={}", className, section, schoolId, term)

        // 1) Get all students in this class
        val students = dbQuery {
            StudentsTable.selectAll().where {
                (StudentsTable.schoolId eq schoolId) and
                (StudentsTable.className eq className) and
                (StudentsTable.section eq section) and
                (StudentsTable.isActive eq true)
            }.map { row ->
                StudentInfo(
                    id = row[StudentsTable.id].value,
                    code = row[StudentsTable.studentCode],
                    name = row[StudentsTable.fullName],
                )
            }
        }

        if (students.isEmpty()) {
            log.warn("Assembly: no students found for class {}-{}", className, section)
            return BatchResult(UUID.randomUUID(), 0, 0, 0, 0, 0, 0, listOf("No students in class"))
        }

        // 2) Build fact bundles for all students (Tier 0)
        val bundles = students.map { student ->
            rollupService.buildBundle(schoolId, student.id, term, academicYearId, language)
        }

        // 3) Run triage on the batch (Tier 1)
        val triageResult = triageService.triage(schoolId, bundles)

        // 4) Narrate each student with bounded concurrency (Tier 2)
        val semaphore = Semaphore(ReportCardConfig.batchConcurrency)
        val results = coroutineScope {
            students.zip(bundles).map { (student, bundle) ->
                async {
                    semaphore.withPermit {
                        runCatching {
                            val factBundleJson = toJson(bundle)
                            val factHash = hash(bundle)

                            // Check cache first — skip AI call if fact hash matches
                            val cachedResponse = lookupCachedNarration(schoolId, factHash)
                            val narration = if (cachedResponse != null) {
                                log.info("Assembly: cache hit for student {} (hash={})", student.code, factHash)
                                val cachedDraft = ReportDraft.fromJson(cachedResponse)
                                NarratorService.NarrationResult(
                                    draft = cachedDraft ?: ReportDraft(
                                        studentName = student.name,
                                        className = className,
                                        section = section,
                                        term = term,
                                    ),
                                    draftJson = cachedResponse,
                                    groundingPassed = true,
                                    groundingFlags = emptyList(),
                                    providerUsed = "cache",
                                    modelUsed = null,
                                    tokensUsed = 0,
                                    usedFallback = false,
                                )
                            } else {
                                narratorService.narrate(
                                    schoolId = schoolId,
                                    bundle = bundle,
                                    classContext = triageResult.overallClassContext,
                                    language = language,
                                )
                            }

                            val existingDraft = draftRepo.findByStudentAndTerm(schoolId, student.id, term, academicYearId)

                            val draftId = if (existingDraft != null) {
                                // Update existing draft
                                draftRepo.updateAiDraft(
                                    id = existingDraft.id,
                                    aiDraft = narration.draftJson,
                                    classContext = triageResult.overallClassContext,
                                    providerUsed = narration.providerUsed,
                                    modelUsed = narration.modelUsed,
                                    tokensUsed = narration.tokensUsed,
                                    groundingFlags = if (narration.groundingFlags.isNotEmpty())
                                        json.encodeToString(narration.groundingFlags) else null,
                                    status = if (narration.groundingPassed)
                                        ReportCardConstants.DraftStatus.DRAFT
                                    else
                                        ReportCardConstants.DraftStatus.FLAGGED,
                                )
                                existingDraft.id
                            } else {
                                draftRepo.insert(
                                    schoolId = schoolId,
                                    studentId = student.id,
                                    classId = null,
                                    className = className,
                                    section = section,
                                    term = term,
                                    academicYearId = academicYearId,
                                    factBundle = factBundleJson,
                                    factHash = factHash,
                                    language = language,
                                )
                            }.also { newId ->
                                if (existingDraft == null) {
                                    draftRepo.updateAiDraft(
                                        id = newId,
                                        aiDraft = narration.draftJson,
                                        classContext = triageResult.overallClassContext,
                                        providerUsed = narration.providerUsed,
                                        modelUsed = narration.modelUsed,
                                        tokensUsed = narration.tokensUsed,
                                        groundingFlags = if (narration.groundingFlags.isNotEmpty())
                                            json.encodeToString(narration.groundingFlags) else null,
                                        status = if (narration.groundingPassed)
                                            ReportCardConstants.DraftStatus.DRAFT
                                        else
                                            ReportCardConstants.DraftStatus.FLAGGED,
                                    )
                                }
                            }
                            narration
                        }.getOrElse { e ->
                            log.error("Assembly: narration failed for student {}: {}", student.code, e.message)
                            null
                        }
                    }
                }
            }.awaitAll()
        }

        // 5) Tally results
        var completed = 0
        var failed = 0
        var grounded = 0
        var flagged = 0
        var fallbackUsed = 0
        val errors = mutableListOf<String>()

        for (result in results) {
            if (result == null) {
                failed++
                errors.add("Narration failed")
            } else {
                completed++
                if (result.groundingPassed) grounded++ else flagged++
                if (result.usedFallback) fallbackUsed++
            }
        }

        // 6) Audit log
        AuditLogger.log(
            schoolId = schoolId,
            actorUserId = createdBy,
            module = ReportCardConstants.MODULE_ASSEMBLE,
            action = "batch_generate",
            entityType = "class",
            entityId = "$className-$section",
            details = mapOf(
                "term" to term,
                "students" to students.size,
                "completed" to completed,
                "flagged" to flagged,
                "fallback" to fallbackUsed,
            ),
        )

        // 7) Notify teachers of the class that drafts are ready for review
        if (completed > 0) {
            runCatching {
                val teacherIds = NotifyRecipients.teachersInSchool(schoolId)
                if (teacherIds.isNotEmpty()) {
                    Notify.toUsers(
                        userIds = teacherIds,
                        category = "report_card",
                        title = "Report Card Drafts Ready",
                        body = "$completed draft(s) for $className-$section ($term) are ready for review." +
                            if (flagged > 0) " $flagged flagged for grounding review." else "",
                        schoolId = schoolId,
                        actorId = createdBy,
                        deepLink = "/teacher/report-review?className=$className&section=$section&term=$term",
                        refType = "report_card_batch",
                        refId = "$className-$section-$term",
                    )
                }
            }.onFailure { log.warn("Failed to notify teachers: {}", it.message) }
        }

        // 8) Notify teachers about grounding-flagged drafts specifically
        if (flagged > 0) {
            runCatching {
                val flaggedDrafts = results.mapNotNull { r ->
                    if (r != null && !r.groundingPassed) r else null
                }
                val teacherIds = NotifyRecipients.teachersInSchool(schoolId)
                if (teacherIds.isNotEmpty() && flaggedDrafts.isNotEmpty()) {
                    Notify.toUsers(
                        userIds = teacherIds,
                        category = "report_card_flagged",
                        title = "$flagged Draft(s) Flagged for Review",
                        body = "Some narratives for $className-$section ($term) failed grounding checks and need teacher review before approval.",
                        schoolId = schoolId,
                        actorId = createdBy,
                        deepLink = "/teacher/report-review?className=$className&section=$section&term=$term&filter=flagged",
                        refType = "report_card_flagged",
                        refId = "$className-$section-$term",
                    )
                }
            }.onFailure { log.warn("Failed to notify about flagged drafts: {}", it.message) }
        }

        // 8b) PEWS feedback loop — notify admins about students with engagement/attendance
        // focus areas so PEWS can prioritise interventions. This closes the loop:
        // PEWS risk data feeds into report card fact bundles → report card identifies
        // focus areas → this notification alerts admins to act via PEWS.
        if (completed > 0) {
            runCatching {
                // results and students are zipped 1:1 in step 4
                val atRiskCount = students.zip(results).count { (student, result) ->
                    if (result == null) return@count false
                    val draft = draftRepo.findByStudentAndTerm(schoolId, student.id, term, academicYearId)
                    val bundle = draft?.factBundle?.let { ReportFactBundle.fromJson(it) }
                    val focusAreas = bundle?.projection?.focusAreas ?: emptyList()
                    val hasPewsFocus = focusAreas.any { it in listOf("engagement", "attendance") }
                    val pewsRisk = bundle?.pewsRiskScore
                    hasPewsFocus || (pewsRisk != null && pewsRisk >= 60)
                }
                if (atRiskCount > 0) {
                    val adminIds = NotifyRecipients.adminsInSchool(schoolId)
                    if (adminIds.isNotEmpty()) {
                        Notify.toUsers(
                            userIds = adminIds,
                            category = "report_card_pews",
                            title = "$atRiskCount Student(s) Need PEWS Follow-up",
                            body = "Report card generation for $className-$section ($term) identified " +
                                "$atRiskCount student(s) with engagement/attendance focus areas. " +
                                "Review in PEWS for targeted interventions.",
                            schoolId = schoolId,
                            actorId = createdBy,
                            deepLink = "/school/pews",
                            refType = "report_card_pews_feedback",
                            refId = "$className-$section-$term",
                        )
                    }
                }
            }.onFailure { log.warn("Failed to send PEWS feedback notification: {}", it.message) }
        }

        log.info("Assembly: complete — {} students, {} completed, {} flagged, {} fallback",
            students.size, completed, flagged, fallbackUsed)

        return BatchResult(
            jobId = UUID.randomUUID(),
            totalStudents = students.size,
            completed = completed,
            failed = failed,
            grounded = grounded,
            flagged = flagged,
            fallbackUsed = fallbackUsed,
            errors = errors,
        )
    }

    /**
     * Teacher edits a draft's narrative content.
     */
    suspend fun editDraft(draftId: UUID, editedDraftJson: String, editedBy: UUID): Boolean {
        ReportCardKillSwitch.require(ReportCardConstants.MODULE_ASSEMBLE)
        val success = draftRepo.updateEditedDraft(draftId, editedDraftJson, editedBy)
        if (success) {
            AuditLogger.log(
                schoolId = draftRepo.findById(draftId)?.schoolId ?: UUID.randomUUID(),
                actorUserId = editedBy,
                module = ReportCardConstants.MODULE_ASSEMBLE,
                action = "draft_edit",
                entityType = "draft",
                entityId = draftId.toString(),
            )
        }
        return success
    }

    /**
     * Teacher approves a single draft.
     */
    suspend fun approveDraft(draftId: UUID, approvedBy: UUID): Boolean {
        ReportCardKillSwitch.require(ReportCardConstants.MODULE_ASSEMBLE)
        val success = draftRepo.approve(draftId, approvedBy)
        if (success) {
            AuditLogger.log(
                schoolId = draftRepo.findById(draftId)?.schoolId ?: UUID.randomUUID(),
                actorUserId = approvedBy,
                module = ReportCardConstants.MODULE_ASSEMBLE,
                action = "draft_approve",
                entityType = "draft",
                entityId = draftId.toString(),
            )
        }
        return success
    }

    /**
     * Teacher bulk-approves multiple drafts.
     */
    suspend fun bulkApprove(draftIds: List<UUID>, approvedBy: UUID): Int {
        ReportCardKillSwitch.require(ReportCardConstants.MODULE_ASSEMBLE)
        val count = draftRepo.bulkApprove(draftIds, approvedBy)
        AuditLogger.log(
            schoolId = draftRepo.findById(draftIds.firstOrNull() ?: UUID.randomUUID())?.schoolId ?: UUID.randomUUID(),
            actorUserId = approvedBy,
            module = ReportCardConstants.MODULE_ASSEMBLE,
            action = "bulk_approve",
            entityType = "draft",
            entityId = count.toString(),
            details = mapOf("count" to count, "ids" to draftIds.take(5)),
        )
        return count
    }

    /**
     * Admin publishes all approved drafts for a class.
     * Sends notifications to parents.
     */
    suspend fun publishForClass(
        schoolId: UUID,
        className: String,
        section: String,
        term: String,
        academicYearId: UUID?,
        publishedBy: UUID,
    ): Int {
        ReportCardKillSwitch.require(ReportCardConstants.MODULE_ASSEMBLE)

        // Find the classId from students
        val classId = dbQuery {
            StudentsTable.selectAll().where {
                (StudentsTable.schoolId eq schoolId) and
                (StudentsTable.className eq className) and
                (StudentsTable.section eq section)
            }.firstOrNull()?.get(StudentsTable.id)?.value
        }

        val count = draftRepo.publishByClass(schoolId, classId, term, academicYearId, publishedBy)

        if (count > 0) {
            AuditLogger.log(
                schoolId = schoolId,
                actorUserId = publishedBy,
                module = ReportCardConstants.MODULE_ASSEMBLE,
                action = "publish_class",
                entityType = "class",
                entityId = "$className-$section",
                details = mapOf("term" to term, "count" to count),
            )

            // Notify parents
            runCatching {
                val publishedDrafts = draftRepo.findByStatus(schoolId, "published", classId)
                for (draft in publishedDrafts) {
                    // Best-effort notification — never fails the publish
                    runCatching {
                        // Look up the student's code for parent notification
                        val studentCode = dbQuery {
                            StudentsTable.selectAll().where {
                                StudentsTable.id eq draft.studentId
                            }.firstOrNull()?.get(StudentsTable.studentCode)
                        } ?: return@runCatching

                        val parentUserIds = NotifyRecipients.parentsOfStudent(schoolId, studentCode)
                        if (parentUserIds.isNotEmpty()) {
                            Notify.toUsers(
                                userIds = parentUserIds,
                                category = "report_card",
                                title = "Report Card Published",
                                body = "Your child's report card for $term is now available.",
                                schoolId = schoolId,
                                actorId = publishedBy,
                                deepLink = "/report-card/${draft.id}",
                                refType = "report_card_draft",
                                refId = draft.id.toString(),
                            )
                        }
                    }.onFailure { log.warn("Notify failed for draft {}: {}", draft.id, it.message) }
                }
            }
        }

        log.info("Assembly: published {} drafts for class {}-{}", count, className, section)
        return count
    }

    /**
     * Get the review queue for a teacher (all drafts in draft/flagged status
     * for their class).
     */
    suspend fun reviewQueue(
        schoolId: UUID,
        className: String,
        section: String,
        term: String,
        academicYearId: UUID?,
    ): List<ReportCardDraftRepository.DraftRow> {
        ReportCardKillSwitch.require(ReportCardConstants.MODULE_ASSEMBLE)
        val allDrafts = draftRepo.findByClassAndTerm(schoolId, null, term, academicYearId)
        return allDrafts.filter {
            it.className.equals(className, ignoreCase = true) &&
            it.section.equals(section, ignoreCase = true) &&
            (it.status == ReportCardConstants.DraftStatus.DRAFT ||
             it.status == ReportCardConstants.DraftStatus.FLAGGED)
        }
    }

    /**
     * Get a single draft by ID.
     */
    suspend fun getDraft(draftId: UUID): ReportCardDraftRepository.DraftRow? {
        ReportCardKillSwitch.require(ReportCardConstants.MODULE_ASSEMBLE)
        return draftRepo.findById(draftId)
    }

    /**
     * Get all published reports for a student (parent view).
     */
    suspend fun getPublishedForStudent(
        schoolId: UUID,
        studentId: UUID,
        academicYearId: UUID?,
    ): List<ReportCardDraftRepository.DraftRow> {
        ReportCardKillSwitch.require(ReportCardConstants.MODULE_ASSEMBLE)
        return draftRepo.findPublishedForStudent(schoolId, studentId, academicYearId)
    }

    /**
     * Regenerate the AI narrative for a single student's draft.
     * Rebuilds the fact bundle, re-runs triage context, and re-narrates.
     * The draft must be in draft/flagged_for_review status.
     */
    suspend fun regenerateForStudent(
        schoolId: UUID,
        draftId: UUID,
        regeneratedBy: UUID,
    ): RegenerateResult {
        ReportCardKillSwitch.require(ReportCardConstants.MODULE_ASSEMBLE)
        log.info("Assembly: regenerating draft {}", draftId)

        val existing = draftRepo.findById(draftId)
            ?: return RegenerateResult(draftId, UUID.randomUUID(), false, false, false, "Draft not found")

        if (existing.status !in listOf(ReportCardConstants.DraftStatus.DRAFT, ReportCardConstants.DraftStatus.FLAGGED)) {
            return RegenerateResult(draftId, existing.studentId, false, false, false,
                "Draft is in ${existing.status} status — can only regenerate draft or flagged drafts")
        }

        return runCatching {
            val bundle = rollupService.buildBundle(
                schoolId, existing.studentId, existing.term, existing.academicYearId, existing.language,
            )
            val triageResult = triageService.triage(schoolId, listOf(bundle))
            val narration = narratorService.narrate(
                schoolId = schoolId,
                bundle = bundle,
                classContext = triageResult.overallClassContext,
                language = existing.language,
            )

            draftRepo.updateAiDraft(
                id = draftId,
                aiDraft = narration.draftJson,
                classContext = triageResult.overallClassContext,
                providerUsed = narration.providerUsed,
                modelUsed = narration.modelUsed,
                tokensUsed = narration.tokensUsed,
                groundingFlags = if (narration.groundingFlags.isNotEmpty())
                    json.encodeToString(narration.groundingFlags) else null,
                status = if (narration.groundingPassed)
                    ReportCardConstants.DraftStatus.DRAFT
                else
                    ReportCardConstants.DraftStatus.FLAGGED,
            )

            AuditLogger.log(
                schoolId = schoolId,
                actorUserId = regeneratedBy,
                module = ReportCardConstants.MODULE_ASSEMBLE,
                action = "regenerate_student",
                entityType = "draft",
                entityId = draftId.toString(),
                details = mapOf(
                    "studentId" to existing.studentId.toString(),
                    "grounded" to narration.groundingPassed,
                    "fallback" to narration.usedFallback,
                ),
            )

            RegenerateResult(draftId, existing.studentId, true, narration.groundingPassed, narration.usedFallback)
        }.getOrElse { e ->
            log.error("Assembly: regeneration failed for draft {}: {}", draftId, e.message)
            RegenerateResult(draftId, existing.studentId, false, false, false, e.message)
        }
    }

    /**
     * Admin oversight: review status across all classes for a term.
     */
    suspend fun getOversightSummary(
        schoolId: UUID,
        term: String,
        academicYearId: UUID?,
    ): OversightSummary {
        ReportCardKillSwitch.require(ReportCardConstants.MODULE_ASSEMBLE)
        val allDrafts = draftRepo.findByClassAndTerm(schoolId, null, term, academicYearId)
        val byClass = allDrafts.groupBy { "${it.className}-${it.section}" }
        val rows = byClass.map { (key, drafts) ->
            val first = drafts.first()
            ClassOversightRow(
                className = first.className,
                section = first.section,
                term = term,
                totalDrafts = drafts.size,
                draftCount = drafts.count { it.status == ReportCardConstants.DraftStatus.DRAFT },
                flaggedCount = drafts.count { it.status == ReportCardConstants.DraftStatus.FLAGGED },
                approvedCount = drafts.count { it.status == ReportCardConstants.DraftStatus.APPROVED },
                publishedCount = drafts.count { it.status == ReportCardConstants.DraftStatus.PUBLISHED },
            )
        }.sortedBy { it.className + "-" + it.section }
        return OversightSummary(schoolId, rows)
    }

    /**
     * Get published reports for a parent's child.
     * Resolves the student from the parent's children table using studentCode.
     */
    suspend fun getPublishedForParent(
        schoolId: UUID,
        parentUserId: UUID,
        childId: UUID,
        academicYearId: UUID?,
    ): List<ParentReportDto> {
        ReportCardKillSwitch.require(ReportCardConstants.MODULE_ASSEMBLE)
        val child = dbQuery {
            ChildrenTable.selectAll().where {
                (ChildrenTable.id eq childId) and
                (ChildrenTable.parentId eq parentUserId) and
                (ChildrenTable.isActive eq true)
            }.singleOrNull()
        } ?: return emptyList()

        val studentCode = child[ChildrenTable.studentCode] ?: return emptyList()
        val student = dbQuery {
            StudentsTable.selectAll().where {
                (StudentsTable.schoolId eq schoolId) and
                (StudentsTable.studentCode eq studentCode) and
                (StudentsTable.isActive eq true)
            }.singleOrNull()
        } ?: return emptyList()

        val drafts = draftRepo.findPublishedForStudent(schoolId, student[StudentsTable.id].value, academicYearId)
        return drafts.map { row ->
            ParentReportDto(
                id = row.id.toString(),
                term = row.term,
                className = row.className,
                section = row.section,
                aiDraft = row.aiDraft,
                classContext = row.classContext,
                language = row.language,
                publishedAt = row.publishedAt?.toString(),
                groundingFlags = row.groundingFlags,
            )
        }
    }

    /**
     * Mark drafts as stale when marks are published for a class.
     * Called from the marks-publish path to invalidate existing drafts.
     */
    suspend fun markDraftsStaleOnMarksChange(
        schoolId: UUID,
        className: String,
        section: String,
        term: String,
    ) {
        ReportCardKillSwitch.require(ReportCardConstants.MODULE_ASSEMBLE)
        val now = Instant.now()
        dbQuery {
            ReportCardDraftsTable.update({
                (ReportCardDraftsTable.schoolId eq schoolId) and
                (ReportCardDraftsTable.className eq className) and
                (ReportCardDraftsTable.section eq section) and
                (ReportCardDraftsTable.term eq term) and
                (ReportCardDraftsTable.status inList listOf(
                    ReportCardConstants.DraftStatus.DRAFT,
                    ReportCardConstants.DraftStatus.FLAGGED,
                ))
            }) {
                it[ReportCardDraftsTable.status] = ReportCardConstants.DraftStatus.DRAFT
                it[ReportCardDraftsTable.updatedAt] = now
            }
        }
        log.info("Assembly: marked drafts stale for {}-{} ({})", className, section, term)
    }

    /**
     * Resolve the language preference for a user from app_users.language_pref.
     */
    suspend fun resolveLanguagePref(userId: UUID): String = dbQuery {
        AppUsersTable.selectAll().where { AppUsersTable.id eq userId }
            .singleOrNull()?.get(AppUsersTable.languagePref) ?: "hi"
    }

    /**
     * Look up a cached AI response by fact hash to skip re-narration
     * when the underlying data hasn't changed.
     */
    suspend fun lookupCachedNarration(
        schoolId: UUID,
        factHash: String,
    ): String? = dbQuery {
        AiResponseCacheTable.selectAll().where {
            (AiResponseCacheTable.schoolId eq schoolId) and
            (AiResponseCacheTable.feature eq ReportCardConstants.AI_FEATURE_TAG) and
            (AiResponseCacheTable.cacheKey eq factHash) and
            (AiResponseCacheTable.expiresAt greater Instant.now())
        }.singleOrNull()?.get(AiResponseCacheTable.response)
    }

    private data class StudentInfo(
        val id: UUID,
        val code: String,
        val name: String,
    )
}
