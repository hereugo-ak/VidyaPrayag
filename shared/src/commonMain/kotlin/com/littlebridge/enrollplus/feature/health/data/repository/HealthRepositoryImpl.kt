package com.littlebridge.enrollplus.feature.health.data.repository

import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.feature.health.data.remote.HealthApi
import com.littlebridge.enrollplus.feature.health.domain.model.*
import com.littlebridge.enrollplus.feature.health.domain.repository.HealthRepository

class HealthRepositoryImpl(
    private val api: HealthApi,
) : HealthRepository {

    override suspend fun getHealthProfile(token: String, studentId: String): NetworkResult<ApiResponse<HealthProfileDto>> =
        api.getHealthProfile(token, studentId)

    override suspend fun upsertHealthProfile(token: String, studentId: String, request: UpsertHealthProfileRequest): NetworkResult<ApiResponse<HealthProfileDto>> =
        api.upsertHealthProfile(token, studentId, request)

    override suspend fun getImmunizations(token: String, studentId: String): NetworkResult<ApiResponse<ImmunizationListResponse>> =
        api.getImmunizations(token, studentId)

    override suspend fun addImmunization(token: String, request: AddImmunizationRequest): NetworkResult<ApiResponse<ImmunizationDto>> =
        api.addImmunization(token, request)

    override suspend fun getIncidents(token: String, studentId: String?, dateFrom: String?, dateTo: String?): NetworkResult<ApiResponse<HealthIncidentListResponse>> =
        api.getIncidents(token, studentId, dateFrom, dateTo)

    override suspend fun logIncident(token: String, request: LogIncidentRequest): NetworkResult<ApiResponse<HealthIncidentDto>> =
        api.logIncident(token, request)

    override suspend fun markIncidentNotified(token: String, incidentId: String): NetworkResult<ApiResponse<HealthIncidentDto>> =
        api.markIncidentNotified(token, incidentId)

    override suspend fun getHealthAlerts(token: String): NetworkResult<ApiResponse<HealthAlertsResponse>> =
        api.getHealthAlerts(token)

    override suspend fun getChildHealth(token: String, childId: String): NetworkResult<ApiResponse<ParentHealthResponse>> =
        api.getChildHealth(token, childId)
}
