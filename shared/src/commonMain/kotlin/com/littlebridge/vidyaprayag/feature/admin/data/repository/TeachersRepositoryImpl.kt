package com.littlebridge.vidyaprayag.feature.admin.data.repository

import com.littlebridge.vidyaprayag.core.model.ApiResponse
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.feature.admin.data.remote.TeachersApi
import com.littlebridge.vidyaprayag.feature.admin.domain.model.CreateTeacherRequest
import com.littlebridge.vidyaprayag.feature.admin.domain.model.TeacherAccountDto
import com.littlebridge.vidyaprayag.feature.admin.domain.model.TeacherCardListResponse
import com.littlebridge.vidyaprayag.feature.admin.domain.model.TeacherCredentialDto
import com.littlebridge.vidyaprayag.feature.admin.domain.repository.TeachersRepository

class TeachersRepositoryImpl(
    private val api: TeachersApi
) : TeachersRepository {

    override suspend fun getTeachers(
        token: String,
        page: Int,
        pageSize: Int
    ): NetworkResult<ApiResponse<TeacherCardListResponse>> =
        api.getTeachers(token, page, pageSize)

    override suspend fun createTeacher(token: String, request: CreateTeacherRequest): NetworkResult<ApiResponse<TeacherAccountDto>> =
        api.createTeacher(token, request)

    override suspend fun deleteTeacher(token: String, teacherId: String): NetworkResult<ApiResponse<Unit>> =
        api.deleteTeacher(token, teacherId)

    override suspend fun resetTeacherPassword(token: String, teacherId: String): NetworkResult<ApiResponse<TeacherCredentialDto>> =
        api.resetTeacherPassword(token, teacherId)
}
