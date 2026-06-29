/*
 * File: PewsRepository.kt
 * Module: feature.pews.domain.repository
 *
 * Repository contract for the Predictive Early Warning System across all three
 * roles (school admin, teacher, parent). Delegates to PewsApi.
 */
package com.littlebridge.enrollplus.feature.pews.domain.repository

import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.feature.pews.domain.model.PewsCohortDto
import com.littlebridge.enrollplus.feature.pews.domain.model.PewsConfigDto
import com.littlebridge.enrollplus.feature.pews.domain.model.PewsEffectivenessDto
import com.littlebridge.enrollplus.feature.pews.domain.model.PewsEffectivenessTrendDto
import com.littlebridge.enrollplus.feature.pews.domain.model.PewsInterventionDto
import com.littlebridge.enrollplus.feature.pews.domain.model.PewsJobStatusDto
import com.littlebridge.enrollplus.feature.pews.domain.model.PewsParentNudgeDto
import com.littlebridge.enrollplus.feature.pews.domain.model.PewsRunResultDto
import com.littlebridge.enrollplus.feature.pews.domain.model.PewsStudentDetailDto
import com.littlebridge.enrollplus.feature.pews.domain.model.PewsStudentDto
import com.littlebridge.enrollplus.feature.pews.domain.model.ParentDraftDto
import com.littlebridge.enrollplus.feature.pews.domain.model.UpdateInterventionRequest

interface PewsRepository {
    // school admin
    suspend fun getCohort(token: String, minLevel: String? = null): NetworkResult<ApiResponse<PewsCohortDto>>
    suspend fun getStudent(token: String, studentCode: String): NetworkResult<ApiResponse<PewsStudentDetailDto>>
    suspend fun getInterventions(token: String, status: String? = null): NetworkResult<ApiResponse<List<PewsInterventionDto>>>
    suspend fun updateIntervention(token: String, interventionId: String, request: UpdateInterventionRequest): NetworkResult<ApiResponse<PewsInterventionDto>>
    suspend fun getEffectiveness(token: String): NetworkResult<ApiResponse<PewsEffectivenessDto>>
    suspend fun getConfig(token: String): NetworkResult<ApiResponse<PewsConfigDto>>
    suspend fun updateConfig(token: String, config: PewsConfigDto): NetworkResult<ApiResponse<PewsConfigDto>>
    suspend fun runNow(token: String): NetworkResult<ApiResponse<PewsRunResultDto>>
    suspend fun getJobStatus(token: String, jobId: String): NetworkResult<ApiResponse<PewsJobStatusDto>>
    suspend fun getTrend(token: String, days: Int = 30): NetworkResult<ApiResponse<PewsEffectivenessTrendDto>>

    // teacher
    suspend fun getTeacherStudents(token: String): NetworkResult<ApiResponse<List<PewsStudentDto>>>
    suspend fun getTeacherInterventions(token: String, status: String? = null): NetworkResult<ApiResponse<List<PewsInterventionDto>>>
    suspend fun updateTeacherIntervention(token: String, interventionId: String, request: UpdateInterventionRequest): NetworkResult<ApiResponse<Map<String, Boolean>>>
    suspend fun generateParentDraft(token: String, interventionId: String, lang: String = "hi"): NetworkResult<ApiResponse<ParentDraftDto>>

    // parent
    suspend fun getParentNudge(token: String, childId: String): NetworkResult<ApiResponse<PewsParentNudgeDto>>
}
