package com.littlebridge.enrollplus.feature.branding.domain.repository

import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.feature.branding.domain.model.*

interface BrandingRepository {
    suspend fun getBranding(token: String): NetworkResult<ApiResponse<SchoolBranding>>
    suspend fun updateBranding(token: String, request: UpdateBrandingRequest): NetworkResult<ApiResponse<SchoolBranding>>
    suspend fun resetBranding(token: String): NetworkResult<ApiResponse<SchoolBranding>>
    suspend fun checkSubdomain(token: String, subdomain: String): NetworkResult<ApiResponse<SubdomainCheckResponse>>
    suspend fun updateSubdomain(token: String, request: SubdomainRequest): NetworkResult<ApiResponse<SubdomainResponse>>
    suspend fun removeSubdomain(token: String): NetworkResult<ApiResponse<RemoveSubdomainResponse>>
    suspend fun getPublicBranding(schoolId: String): NetworkResult<ApiResponse<SchoolBranding>>
    suspend fun resolveSubdomain(subdomain: String): NetworkResult<ApiResponse<SubdomainResolution>>
}
