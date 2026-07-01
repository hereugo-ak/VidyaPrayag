package com.littlebridge.enrollplus.feature.admin.data.repository

import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.feature.admin.data.remote.SchoolClassesApi
import com.littlebridge.enrollplus.feature.admin.domain.model.CreateSchoolClassRequest
import com.littlebridge.enrollplus.feature.admin.domain.model.CreateSchoolSubjectRequest
import com.littlebridge.enrollplus.feature.admin.domain.model.SchoolClassDto
import com.littlebridge.enrollplus.feature.admin.domain.model.SchoolClassListResponse
import com.littlebridge.enrollplus.feature.admin.domain.model.SchoolSubjectDto
import com.littlebridge.enrollplus.feature.admin.domain.model.SchoolSubjectListResponse
import com.littlebridge.enrollplus.feature.admin.domain.model.TimetableDto
import com.littlebridge.enrollplus.feature.admin.domain.model.UpdateSchoolClassRequest
import com.littlebridge.enrollplus.feature.admin.domain.model.UpdateSchoolSubjectRequest
import com.littlebridge.enrollplus.feature.admin.domain.repository.SchoolClassesRepository

class SchoolClassesRepositoryImpl(
    private val api: SchoolClassesApi,
) : SchoolClassesRepository {

    override suspend fun listClasses(token: String) = api.listClasses(token)
    override suspend fun createClass(token: String, req: CreateSchoolClassRequest) = api.createClass(token, req)
    override suspend fun updateClass(token: String, id: String, req: UpdateSchoolClassRequest) = api.updateClass(token, id, req)
    override suspend fun deleteClass(token: String, id: String) = api.deleteClass(token, id)

    override suspend fun listSubjects(token: String, classId: String) = api.listSubjects(token, classId)
    override suspend fun createSubject(token: String, classId: String, req: CreateSchoolSubjectRequest) = api.createSubject(token, classId, req)
    override suspend fun updateSubject(token: String, id: String, req: UpdateSchoolSubjectRequest) = api.updateSubject(token, id, req)
    override suspend fun deleteSubject(token: String, id: String) = api.deleteSubject(token, id)

    override suspend fun getTimetable(token: String, classFilter: String?) = api.getTimetable(token, classFilter)
}
