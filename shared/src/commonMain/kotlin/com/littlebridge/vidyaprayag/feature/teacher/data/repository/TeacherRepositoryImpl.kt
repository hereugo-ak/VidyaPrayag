package com.littlebridge.vidyaprayag.feature.teacher.data.repository

import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.feature.teacher.data.remote.TeacherApi
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.*
import com.littlebridge.vidyaprayag.feature.teacher.domain.repository.TeacherRepository

class TeacherRepositoryImpl(
    private val api: TeacherApi
) : TeacherRepository {

    override suspend fun getHome(token: String): NetworkResult<TeacherHomeResponse> {
        return when (val result = api.getHome(token)) {
            is NetworkResult.Success -> unwrapData(result.data)
            is NetworkResult.Error -> NetworkResult.Error(result.message, result.code)
            is NetworkResult.ConnectionError -> NetworkResult.ConnectionError
        }
    }

    override suspend fun getTimetable(token: String, day: String): NetworkResult<TeacherTimetableResponse> {
        return when (val result = api.getTimetable(token, day)) {
            is NetworkResult.Success -> unwrapData(result.data)
            is NetworkResult.Error -> NetworkResult.Error(result.message, result.code)
            is NetworkResult.ConnectionError -> NetworkResult.ConnectionError
        }
    }

    override suspend fun getClasses(token: String): NetworkResult<TeacherClassesResponse> {
        return when (val result = api.getClasses(token)) {
            is NetworkResult.Success -> unwrapData(result.data)
            is NetworkResult.Error -> NetworkResult.Error(result.message, result.code)
            is NetworkResult.ConnectionError -> NetworkResult.ConnectionError
        }
    }

    override suspend fun getTests(token: String, className: String?, subject: String?): NetworkResult<TeacherTestsResponse> {
        return when (val result = api.getTests(token, className, subject)) {
            is NetworkResult.Success -> unwrapData(result.data)
            is NetworkResult.Error -> NetworkResult.Error(result.message, result.code)
            is NetworkResult.ConnectionError -> NetworkResult.ConnectionError
        }
    }

    override suspend fun createTest(token: String, request: CreateTestRequest): NetworkResult<CreateTestResponse> {
        return when (val result = api.createTest(token, request)) {
            is NetworkResult.Success -> unwrapData(result.data)
            is NetworkResult.Error -> NetworkResult.Error(result.message, result.code)
            is NetworkResult.ConnectionError -> NetworkResult.ConnectionError
        }
    }

    override suspend fun getHomework(token: String, className: String?): NetworkResult<TeacherHomeworkResponse> {
        return when (val result = api.getHomework(token, className)) {
            is NetworkResult.Success -> unwrapData(result.data)
            is NetworkResult.Error -> NetworkResult.Error(result.message, result.code)
            is NetworkResult.ConnectionError -> NetworkResult.ConnectionError
        }
    }

    override suspend fun createHomework(token: String, request: CreateHomeworkRequest): NetworkResult<CreateHomeworkResponse> {
        return when (val result = api.createHomework(token, request)) {
            is NetworkResult.Success -> unwrapData(result.data)
            is NetworkResult.Error -> NetworkResult.Error(result.message, result.code)
            is NetworkResult.ConnectionError -> NetworkResult.ConnectionError
        }
    }

    override suspend fun getCurriculum(token: String, subject: String?): NetworkResult<TeacherCurriculumResponse> {
        return when (val result = api.getCurriculum(token, subject)) {
            is NetworkResult.Success -> unwrapData(result.data)
            is NetworkResult.Error -> NetworkResult.Error(result.message, result.code)
            is NetworkResult.ConnectionError -> NetworkResult.ConnectionError
        }
    }

    override suspend fun getProfile(token: String): NetworkResult<TeacherProfileResponse> {
        return when (val result = api.getProfile(token)) {
            is NetworkResult.Success -> unwrapData(result.data)
            is NetworkResult.Error -> NetworkResult.Error(result.message, result.code)
            is NetworkResult.ConnectionError -> NetworkResult.ConnectionError
        }
    }

    private fun <T> unwrapData(envelope: com.littlebridge.vidyaprayag.core.model.ApiResponse<T>): NetworkResult<T> {
        return when {
            !envelope.success -> NetworkResult.Error(
                envelope.message.ifBlank { "Request failed" }
            )
            envelope.data == null -> NetworkResult.Error("No data in response")
            else -> NetworkResult.Success(envelope.data)
        }
    }
}
