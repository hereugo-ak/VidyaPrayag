/*
 * File: ReportCardRepository.kt
 * Module: feature.reportcard.domain.repository
 */
package com.littlebridge.enrollplus.feature.reportcard.domain.repository

import com.littlebridge.enrollplus.feature.reportcard.data.remote.ReportCardApi
import com.littlebridge.enrollplus.feature.reportcard.domain.model.ReportCardModels

class ReportCardRepository(
    private val api: ReportCardApi,
) {
    suspend fun generateBatch(token: String, request: ReportCardModels.GenerateRequest) =
        api.generateBatch(token, request)

    suspend fun getJobStatus(token: String, jobId: String) =
        api.getJobStatus(token, jobId)

    suspend fun getReviewQueue(token: String, className: String, section: String, term: String, academicYearId: String? = null) =
        api.getReviewQueue(token, className, section, term, academicYearId)

    suspend fun getDraft(token: String, draftId: String) =
        api.getDraft(token, draftId)

    suspend fun editDraft(token: String, draftId: String, editedDraft: String) =
        api.editDraft(token, draftId, ReportCardModels.EditDraftRequest(editedDraft))

    suspend fun approveDraft(token: String, draftId: String) =
        api.approveDraft(token, draftId)

    suspend fun regenerateDraft(token: String, draftId: String) =
        api.regenerateDraft(token, draftId)

    suspend fun bulkApprove(token: String, draftIds: List<String>) =
        api.bulkApprove(token, draftIds)

    suspend fun publishClass(token: String, request: ReportCardModels.PublishRequest) =
        api.publishClass(token, request)

    suspend fun getOversight(token: String, term: String, academicYearId: String? = null) =
        api.getOversight(token, term, academicYearId)

    suspend fun getTermConfig(token: String) =
        api.getTermConfig(token)

    suspend fun runFlywheel(token: String, currentTerm: String, previousTerm: String, academicYearId: String? = null) =
        api.runFlywheel(token, currentTerm, previousTerm, academicYearId)

    suspend fun getEffectiveness(token: String) =
        api.getEffectiveness(token)

    suspend fun getProjectionAccuracy(token: String, currentTerm: String, previousTerm: String, academicYearId: String? = null) =
        api.getProjectionAccuracy(token, currentTerm, previousTerm, academicYearId)

    suspend fun getPatterns(token: String, term: String, academicYearId: String? = null) =
        api.getPatterns(token, term, academicYearId)

    suspend fun getPublishedReports(token: String, childId: String, academicYearId: String? = null) =
        api.getPublishedReports(token, childId, academicYearId)

    suspend fun getConferencePack(token: String, childId: String) =
        api.getConferencePack(token, childId)
}
