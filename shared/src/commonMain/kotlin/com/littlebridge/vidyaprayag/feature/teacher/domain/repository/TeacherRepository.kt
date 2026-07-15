package com.littlebridge.vidyaprayag.feature.teacher.domain.repository

import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.*

interface TeacherRepository {
    suspend fun getHome(token: String): NetworkResult<TeacherHomeResponse>
    suspend fun getTimetable(token: String, day: String): NetworkResult<TeacherTimetableResponse>
    suspend fun getClasses(token: String): NetworkResult<TeacherClassesResponse>
    suspend fun getTests(token: String, className: String? = null, subject: String? = null): NetworkResult<TeacherTestsResponse>
    suspend fun createTest(token: String, request: CreateTestRequest): NetworkResult<CreateTestResponse>
    suspend fun getHomework(token: String, className: String? = null): NetworkResult<TeacherHomeworkResponse>
    suspend fun createHomework(token: String, request: CreateHomeworkRequest): NetworkResult<CreateHomeworkResponse>
    suspend fun getCurriculum(token: String, subject: String? = null): NetworkResult<TeacherCurriculumResponse>
    suspend fun getProfile(token: String): NetworkResult<TeacherProfileResponse>
}
