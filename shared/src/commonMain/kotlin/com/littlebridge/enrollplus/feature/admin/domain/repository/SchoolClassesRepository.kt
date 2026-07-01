package com.littlebridge.enrollplus.feature.admin.domain.repository

import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.feature.admin.domain.model.CreateSchoolClassRequest
import com.littlebridge.enrollplus.feature.admin.domain.model.CreateSchoolSubjectRequest
import com.littlebridge.enrollplus.feature.admin.domain.model.SchoolClassDto
import com.littlebridge.enrollplus.feature.admin.domain.model.SchoolClassListResponse
import com.littlebridge.enrollplus.feature.admin.domain.model.SchoolSubjectDto
import com.littlebridge.enrollplus.feature.admin.domain.model.SchoolSubjectListResponse
import com.littlebridge.enrollplus.feature.admin.domain.model.TimetableDto
import com.littlebridge.enrollplus.feature.admin.domain.model.UpdateSchoolClassRequest
import com.littlebridge.enrollplus.feature.admin.domain.model.UpdateSchoolSubjectRequest

interface SchoolClassesRepository {
    suspend fun listClasses(token: String): NetworkResult<ApiResponse<SchoolClassListResponse>>
    suspend fun createClass(token: String, req: CreateSchoolClassRequest): NetworkResult<ApiResponse<SchoolClassDto>>
    suspend fun updateClass(token: String, id: String, req: UpdateSchoolClassRequest): NetworkResult<ApiResponse<SchoolClassDto>>
    suspend fun deleteClass(token: String, id: String): NetworkResult<ApiResponse<Unit>>

    suspend fun listSubjects(token: String, classId: String): NetworkResult<ApiResponse<SchoolSubjectListResponse>>
    suspend fun createSubject(token: String, classId: String, req: CreateSchoolSubjectRequest): NetworkResult<ApiResponse<SchoolSubjectDto>>
    suspend fun updateSubject(token: String, id: String, req: UpdateSchoolSubjectRequest): NetworkResult<ApiResponse<SchoolSubjectDto>>
    suspend fun deleteSubject(token: String, id: String): NetworkResult<ApiResponse<Unit>>

    suspend fun getTimetable(token: String, classFilter: String? = null): NetworkResult<ApiResponse<TimetableDto>>
}
