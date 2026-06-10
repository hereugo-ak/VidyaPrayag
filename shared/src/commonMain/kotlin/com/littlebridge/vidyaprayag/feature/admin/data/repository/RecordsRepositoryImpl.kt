package com.littlebridge.vidyaprayag.feature.admin.data.repository

import com.littlebridge.vidyaprayag.core.model.ApiResponse
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.feature.admin.data.remote.RecordsApi
import com.littlebridge.vidyaprayag.feature.admin.domain.model.AttendanceSummaryDto
import com.littlebridge.vidyaprayag.feature.admin.domain.model.FeeLedgerDto
import com.littlebridge.vidyaprayag.feature.admin.domain.model.MarksSummaryDto
import com.littlebridge.vidyaprayag.feature.admin.domain.repository.RecordsRepository

class RecordsRepositoryImpl(
    private val api: RecordsApi
) : RecordsRepository {

    override suspend fun getAttendanceSummary(token: String): NetworkResult<ApiResponse<AttendanceSummaryDto>> =
        api.getAttendanceSummary(token)

    override suspend fun getMarksSummary(token: String): NetworkResult<ApiResponse<MarksSummaryDto>> =
        api.getMarksSummary(token)

    override suspend fun getFeeLedger(token: String): NetworkResult<ApiResponse<FeeLedgerDto>> =
        api.getFeeLedger(token)
}
