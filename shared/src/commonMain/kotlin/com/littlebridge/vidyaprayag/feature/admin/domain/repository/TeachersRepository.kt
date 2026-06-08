package com.littlebridge.vidyaprayag.feature.admin.domain.repository

import com.littlebridge.vidyaprayag.core.model.ApiResponse
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.feature.admin.domain.model.CreateTeacherRequest
import com.littlebridge.vidyaprayag.feature.admin.domain.model.TeacherAccountDto
import com.littlebridge.vidyaprayag.feature.admin.domain.model.TeacherListResponse

/** RA-22: school teacher roster (list / add / delete). */
interface TeachersRepository {
    suspend fun getTeachers(token: String): NetworkResult<ApiResponse<TeacherListResponse>>
    suspend fun createTeacher(token: String, request: CreateTeacherRequest): NetworkResult<ApiResponse<TeacherAccountDto>>
    suspend fun deleteTeacher(token: String, teacherId: String): NetworkResult<ApiResponse<Unit>>
}
