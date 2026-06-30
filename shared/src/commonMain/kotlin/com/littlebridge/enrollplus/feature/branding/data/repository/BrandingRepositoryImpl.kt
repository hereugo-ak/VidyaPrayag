package com.littlebridge.enrollplus.feature.branding.data.repository

import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.feature.branding.data.remote.BrandingApi
import com.littlebridge.enrollplus.feature.branding.domain.model.*
import com.littlebridge.enrollplus.feature.branding.domain.repository.BrandingRepository

class BrandingRepositoryImpl(
    private val api: BrandingApi,
) : BrandingRepository {

    override suspend fun getBranding(token: String) = api.getBranding(token)
    override suspend fun updateBranding(token: String, request: UpdateBrandingRequest) = api.updateBranding(token, request)
    override suspend fun resetBranding(token: String) = api.resetBranding(token)
    override suspend fun checkSubdomain(token: String, subdomain: String) = api.checkSubdomain(token, subdomain)
    override suspend fun updateSubdomain(token: String, request: SubdomainRequest) = api.updateSubdomain(token, request)
    override suspend fun removeSubdomain(token: String) = api.removeSubdomain(token)
    override suspend fun getPublicBranding(schoolId: String) = api.getPublicBranding(schoolId)
    override suspend fun resolveSubdomain(subdomain: String) = api.resolveSubdomain(subdomain)
}
