/*
 * File: PewsRepositoryImpl.kt
 * Module: feature.pews.data.repository
 *
 * Thin delegate over PewsApi — same pattern as every other repository impl in
 * the codebase (returns NetworkResult<ApiResponse<T>> straight through).
 */
package com.littlebridge.enrollplus.feature.pews.data.repository

import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.feature.pews.data.remote.PewsApi
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
import com.littlebridge.enrollplus.feature.pews.domain.model.UpdateInterventionRequest
import com.littlebridge.enrollplus.feature.pews.domain.repository.PewsRepository

class PewsRepositoryImpl(
    private val api: PewsApi,
) : PewsRepository {

    override suspend fun getCohort(token: String, minLevel: String?): NetworkResult<ApiResponse<PewsCohortDto>> =
        api.getCohort(token, minLevel)

    override suspend fun getStudent(token: String, studentCode: String): NetworkResult<ApiResponse<PewsStudentDetailDto>> =
        api.getStudent(token, studentCode)

    override suspend fun getInterventions(token: String, status: String?): NetworkResult<ApiResponse<List<PewsInterventionDto>>> =
        api.getInterventions(token, status)

    override suspend fun updateIntervention(token: String, interventionId: String, request: UpdateInterventionRequest): NetworkResult<ApiResponse<PewsInterventionDto>> =
        api.updateIntervention(token, interventionId, request)

    override suspend fun getEffectiveness(token: String): NetworkResult<ApiResponse<PewsEffectivenessDto>> =
        api.getEffectiveness(token)

    override suspend fun getConfig(token: String): NetworkResult<ApiResponse<PewsConfigDto>> =
        api.getConfig(token)

    override suspend fun updateConfig(token: String, config: PewsConfigDto): NetworkResult<ApiResponse<PewsConfigDto>> =
        api.updateConfig(token, config)

    override suspend fun runNow(token: String): NetworkResult<ApiResponse<PewsRunResultDto>> =
        api.runNow(token)

    override suspend fun getJobStatus(token: String, jobId: String): NetworkResult<ApiResponse<PewsJobStatusDto>> =
        api.getJobStatus(token, jobId)

    override suspend fun getTrend(token: String, days: Int): NetworkResult<ApiResponse<PewsEffectivenessTrendDto>> =
        api.getTrend(token, days)

    override suspend fun getTeacherStudents(token: String): NetworkResult<ApiResponse<List<PewsStudentDto>>> =
        api.getTeacherStudents(token)

    override suspend fun getTeacherInterventions(token: String, status: String?): NetworkResult<ApiResponse<List<PewsInterventionDto>>> =
        api.getTeacherInterventions(token, status)

    override suspend fun updateTeacherIntervention(token: String, interventionId: String, request: UpdateInterventionRequest): NetworkResult<ApiResponse<Map<String, Boolean>>> =
        api.updateTeacherIntervention(token, interventionId, request)

    override suspend fun getParentNudge(token: String, childId: String): NetworkResult<ApiResponse<PewsParentNudgeDto>> =
        api.getParentNudge(token, childId)
}
