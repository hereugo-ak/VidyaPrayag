package com.littlebridge.enrollplus.feature.admin.domain.repository

import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.feature.admin.domain.model.CreateSchoolClassRequest
import com.littlebridge.enrollplus.feature.admin.domain.model.CreateSchoolSubjectRequest
import com.littlebridge.enrollplus.feature.admin.domain.model.SchoolClassDto
import com.littlebridge.enrollplus.feature.admin.domain.model.SchoolClassListResponse
import com.littlebridge.enrollplus.feature.admin.domain.model.SchoolSubjectDto
import com.littlebridge.enrollplus.feature.admin.domain.model.SchoolSubjectListResponse
import com.littlebridge.enrollplus.feature.admin.domain.model.ChangeRequestListResponse
import com.littlebridge.enrollplus.feature.admin.domain.model.CreateChangeRequestRequest
import com.littlebridge.enrollplus.feature.admin.domain.model.BulkCreatePeriodsRequest
import com.littlebridge.enrollplus.feature.admin.domain.model.BulkCreatePeriodsResponse
import com.littlebridge.enrollplus.feature.admin.domain.model.CreateExceptionRequest
import com.littlebridge.enrollplus.feature.admin.domain.model.CreatePeriodRequest
import com.littlebridge.enrollplus.feature.admin.domain.model.PeriodDetailDto
import com.littlebridge.enrollplus.feature.admin.domain.model.PeriodExceptionDto
import com.littlebridge.enrollplus.feature.admin.domain.model.PeriodExceptionListResponse
import com.littlebridge.enrollplus.feature.admin.domain.model.ReviewRequest
import com.littlebridge.enrollplus.feature.admin.domain.model.TimetableChangeRequestDto
import com.littlebridge.enrollplus.feature.admin.domain.model.TimetableDto
import com.littlebridge.enrollplus.feature.admin.domain.model.UpdatePeriodRequest
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

    suspend fun createPeriod(token: String, req: CreatePeriodRequest): NetworkResult<ApiResponse<PeriodDetailDto>>
    suspend fun bulkCreatePeriods(token: String, req: BulkCreatePeriodsRequest): NetworkResult<ApiResponse<BulkCreatePeriodsResponse>>
    suspend fun updatePeriod(token: String, id: String, req: UpdatePeriodRequest): NetworkResult<ApiResponse<PeriodDetailDto>>
    suspend fun deletePeriod(token: String, id: String): NetworkResult<ApiResponse<Unit>>

    suspend fun listExceptions(token: String, date: String? = null): NetworkResult<ApiResponse<PeriodExceptionListResponse>>
    suspend fun createException(token: String, req: CreateExceptionRequest): NetworkResult<ApiResponse<PeriodExceptionDto>>
    suspend fun deleteException(token: String, id: String): NetworkResult<ApiResponse<Unit>>

    suspend fun listChangeRequests(token: String, status: String? = null): NetworkResult<ApiResponse<ChangeRequestListResponse>>
    suspend fun approveChangeRequest(token: String, id: String, req: ReviewRequest): NetworkResult<ApiResponse<Unit>>
    suspend fun rejectChangeRequest(token: String, id: String, req: ReviewRequest): NetworkResult<ApiResponse<Unit>>
}
