package com.littlebridge.vidyaprayag.feature.admin.data.repository

import com.littlebridge.vidyaprayag.core.model.ApiResponse
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.feature.admin.data.remote.ResultsApi
import com.littlebridge.vidyaprayag.feature.admin.domain.model.PublishResultsRequest
import com.littlebridge.vidyaprayag.feature.admin.domain.model.PublishResultsResponse
import com.littlebridge.vidyaprayag.feature.admin.domain.model.ResultsResponse
import com.littlebridge.vidyaprayag.feature.admin.domain.repository.ResultsRepository

class ResultsRepositoryImpl(private val api: ResultsApi) : ResultsRepository {
    override suspend fun getResults(token: String, test: String?, className: String?, subject: String?): NetworkResult<ApiResponse<ResultsResponse>> =
        api.getResults(token, test, className, subject)

    override suspend fun publishResults(token: String, request: PublishResultsRequest): NetworkResult<ApiResponse<PublishResultsResponse>> =
        api.publishResults(token, request)
}
