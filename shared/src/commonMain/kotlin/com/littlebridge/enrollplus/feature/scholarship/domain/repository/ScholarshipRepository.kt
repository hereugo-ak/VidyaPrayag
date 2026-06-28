package com.littlebridge.enrollplus.feature.scholarship.domain.repository

import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.feature.scholarship.domain.model.*

interface ScholarshipRepository {

    // Admin: Scheme Management
    suspend fun listSchemes(token: String, all: Boolean = false): NetworkResult<ApiResponse<List<ScholarshipScheme>>>
    suspend fun createScheme(token: String, request: CreateSchemeRequest): NetworkResult<ApiResponse<ScholarshipScheme>>
    suspend fun updateScheme(token: String, schemeId: String, request: UpdateSchemeRequest): NetworkResult<ApiResponse<Unit>>
    suspend fun deleteScheme(token: String, schemeId: String): NetworkResult<ApiResponse<Unit>>

    // Admin: Application Review
    suspend fun listApplications(token: String, status: String? = null): NetworkResult<ApiResponse<List<ScholarshipApplication>>>
    suspend fun getApplication(token: String, applicationId: String): NetworkResult<ApiResponse<ScholarshipApplication>>
    suspend fun approveApplication(token: String, applicationId: String, request: ApproveApplicationRequest): NetworkResult<ApiResponse<ScholarshipApplication>>
    suspend fun rejectApplication(token: String, applicationId: String, request: RejectApplicationRequest): NetworkResult<ApiResponse<ScholarshipApplication>>
    suspend fun disburse(token: String, applicationId: String, request: DisburseRequest): NetworkResult<ApiResponse<ScholarshipApplication>>

    // Admin: Renewals
    suspend fun listRenewals(token: String, status: String? = null): NetworkResult<ApiResponse<List<ScholarshipRenewal>>>
    suspend fun approveRenewal(token: String, renewalId: String, request: ApproveRenewalRequest): NetworkResult<ApiResponse<ScholarshipRenewal>>
    suspend fun rejectRenewal(token: String, renewalId: String, request: RejectApplicationRequest): NetworkResult<ApiResponse<ScholarshipRenewal>>

    // Parent
    suspend fun getParentScholarships(token: String): NetworkResult<ApiResponse<ParentScholarshipsData>>
    suspend fun applyScholarship(token: String, request: ApplyScholarshipRequest): NetworkResult<ApiResponse<ScholarshipApplication>>
    suspend fun getParentApplications(token: String): NetworkResult<ApiResponse<List<ScholarshipApplication>>>
    suspend fun applyRenewal(token: String, request: ApplyRenewalRequest): NetworkResult<ApiResponse<ScholarshipRenewal>>
}
