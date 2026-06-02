package com.littlebridge.vidyaprayag.feature.admin.domain.repository

import com.littlebridge.vidyaprayag.core.model.ApiResponse
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.feature.admin.domain.model.CalendarResponse

interface CalendarRepository {
    suspend fun getCalendar(token: String, date: String? = null, viewType: String = "month"): NetworkResult<ApiResponse<CalendarResponse>>
}
