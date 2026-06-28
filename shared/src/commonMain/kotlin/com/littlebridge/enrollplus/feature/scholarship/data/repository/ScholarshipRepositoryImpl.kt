package com.littlebridge.enrollplus.feature.scholarship.data.repository

import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.feature.scholarship.data.remote.ScholarshipApi
import com.littlebridge.enrollplus.feature.scholarship.domain.model.*
import com.littlebridge.enrollplus.feature.scholarship.domain.repository.ScholarshipRepository

class ScholarshipRepositoryImpl(
    private val api: ScholarshipApi,
) : ScholarshipRepository {

    override suspend fun listSchemes(token: String, all: Boolean) = api.listSchemes(token, all)
    override suspend fun createScheme(token: String, request: CreateSchemeRequest) = api.createScheme(token, request)
    override suspend fun updateScheme(token: String, schemeId: String, request: UpdateSchemeRequest) = api.updateScheme(token, schemeId, request)
    override suspend fun deleteScheme(token: String, schemeId: String) = api.deleteScheme(token, schemeId)

    override suspend fun listApplications(token: String, status: String?) = api.listApplications(token, status)
    override suspend fun getApplication(token: String, applicationId: String) = api.getApplication(token, applicationId)
    override suspend fun approveApplication(token: String, applicationId: String, request: ApproveApplicationRequest) = api.approveApplication(token, applicationId, request)
    override suspend fun rejectApplication(token: String, applicationId: String, request: RejectApplicationRequest) = api.rejectApplication(token, applicationId, request)
    override suspend fun disburse(token: String, applicationId: String, request: DisburseRequest) = api.disburse(token, applicationId, request)

    override suspend fun listRenewals(token: String, status: String?) = api.listRenewals(token, status)
    override suspend fun approveRenewal(token: String, renewalId: String, request: ApproveRenewalRequest) = api.approveRenewal(token, renewalId, request)
    override suspend fun rejectRenewal(token: String, renewalId: String, request: RejectApplicationRequest) = api.rejectRenewal(token, renewalId, request)

    override suspend fun getParentScholarships(token: String) = api.getParentScholarships(token)
    override suspend fun applyScholarship(token: String, request: ApplyScholarshipRequest) = api.applyScholarship(token, request)
    override suspend fun getParentApplications(token: String) = api.getParentApplications(token)
    override suspend fun applyRenewal(token: String, request: ApplyRenewalRequest) = api.applyRenewal(token, request)
}
