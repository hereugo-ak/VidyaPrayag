package com.littlebridge.vidyaprayag.feature.admin.data.repository

import com.littlebridge.vidyaprayag.core.model.ApiResponse
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.feature.admin.data.remote.CalendarApi
import com.littlebridge.vidyaprayag.feature.admin.domain.model.CalendarResponse
import com.littlebridge.vidyaprayag.feature.admin.domain.repository.CalendarRepository

class CalendarRepositoryImpl(
    private val api: CalendarApi
) : CalendarRepository {

    override suspend fun getCalendar(
        token: String,
        date: String?,
        viewType: String
    ): NetworkResult<ApiResponse<CalendarResponse>> = api.getCalendar(token, date, viewType)
}
