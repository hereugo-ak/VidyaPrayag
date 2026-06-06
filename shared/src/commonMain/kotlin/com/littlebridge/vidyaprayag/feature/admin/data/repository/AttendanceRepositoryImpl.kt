package com.littlebridge.vidyaprayag.feature.admin.data.repository

import com.littlebridge.vidyaprayag.core.model.ApiResponse
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.feature.admin.data.remote.AttendanceApi
import com.littlebridge.vidyaprayag.feature.admin.domain.model.AttendanceResponse
import com.littlebridge.vidyaprayag.feature.admin.domain.repository.AttendanceRepository

class AttendanceRepositoryImpl(
    private val api: AttendanceApi
) : AttendanceRepository {

    override suspend fun getDailyAttendance(
        token: String,
        type: String,
        grade: String?,
        date: String?
    ): NetworkResult<ApiResponse<AttendanceResponse>> = api.getDailyAttendance(token, type, grade, date)
}
