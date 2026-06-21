package com.littlebridge.vidyaprayag.feature.admin.domain.repository

import com.littlebridge.vidyaprayag.core.model.ApiResponse
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.feature.admin.domain.model.AttendanceResponse

interface AttendanceRepository {
    suspend fun getDailyAttendance(
        token: String,
        type: String = "student",
        grade: String? = null,
        date: String? = null
    ): NetworkResult<ApiResponse<AttendanceResponse>>
}
