/*
 * File: AcademicYearRepository.kt
 * Module: feature.admin.domain.repository
 */
package com.littlebridge.vidyaprayag.feature.admin.domain.repository

import com.littlebridge.vidyaprayag.core.model.ApiResponse
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.feature.admin.domain.model.AcademicYearDto
import com.littlebridge.vidyaprayag.feature.admin.domain.model.AcademicYearsListResponse
import com.littlebridge.vidyaprayag.feature.admin.domain.model.CreateAcademicYearRequest
import com.littlebridge.vidyaprayag.feature.admin.domain.model.UpdateAcademicYearRequest

interface AcademicYearRepository {
    suspend fun getYears(token: String): NetworkResult<ApiResponse<AcademicYearsListResponse>>
    suspend fun createYear(token: String, request: CreateAcademicYearRequest): NetworkResult<ApiResponse<AcademicYearDto>>
    suspend fun updateYear(token: String, yearId: String, request: UpdateAcademicYearRequest): NetworkResult<ApiResponse<AcademicYearDto>>
}
