package com.littlebridge.enrollplus.feature.health.domain.repository

import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.feature.health.domain.model.*

interface HealthRepository {
    suspend fun getHealthProfile(token: String, studentId: String): NetworkResult<ApiResponse<HealthProfileDto>>
    suspend fun upsertHealthProfile(token: String, studentId: String, request: UpsertHealthProfileRequest): NetworkResult<ApiResponse<HealthProfileDto>>
    suspend fun getImmunizations(token: String, studentId: String): NetworkResult<ApiResponse<ImmunizationListResponse>>
    suspend fun addImmunization(token: String, request: AddImmunizationRequest): NetworkResult<ApiResponse<ImmunizationDto>>
    suspend fun getIncidents(token: String, studentId: String? = null, dateFrom: String? = null, dateTo: String? = null): NetworkResult<ApiResponse<HealthIncidentListResponse>>
    suspend fun logIncident(token: String, request: LogIncidentRequest): NetworkResult<ApiResponse<HealthIncidentDto>>
    suspend fun markIncidentNotified(token: String, incidentId: String): NetworkResult<ApiResponse<HealthIncidentDto>>
    suspend fun getHealthAlerts(token: String): NetworkResult<ApiResponse<HealthAlertsResponse>>
    suspend fun getChildHealth(token: String, childId: String): NetworkResult<ApiResponse<ParentHealthResponse>>
}
