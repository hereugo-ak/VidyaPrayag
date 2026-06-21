package com.littlebridge.vidyaprayag.feature.admin.domain.repository

import com.littlebridge.vidyaprayag.core.model.ApiResponse
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.feature.admin.domain.model.AttendanceSummaryDto
import com.littlebridge.vidyaprayag.feature.admin.domain.model.FeeLedgerDto
import com.littlebridge.vidyaprayag.feature.admin.domain.model.MarksSummaryDto

/** RA-52: admin Records rollups (attendance / marks / fees) — school-scoped reads. */
interface RecordsRepository {
    suspend fun getAttendanceSummary(token: String): NetworkResult<ApiResponse<AttendanceSummaryDto>>
    suspend fun getMarksSummary(token: String): NetworkResult<ApiResponse<MarksSummaryDto>>
    suspend fun getFeeLedger(token: String): NetworkResult<ApiResponse<FeeLedgerDto>>
}
