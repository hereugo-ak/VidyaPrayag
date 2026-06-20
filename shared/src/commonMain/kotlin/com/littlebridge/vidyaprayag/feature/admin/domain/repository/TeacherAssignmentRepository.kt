package com.littlebridge.vidyaprayag.feature.admin.domain.repository

import com.littlebridge.vidyaprayag.core.model.ApiResponse
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.feature.admin.domain.model.AssignTeacherClassesRequest
import com.littlebridge.vidyaprayag.feature.admin.domain.model.AssignmentOptionsDto
import com.littlebridge.vidyaprayag.feature.admin.domain.model.BulkAssignResponseDto
import com.littlebridge.vidyaprayag.feature.admin.domain.model.TeacherAssignmentOverviewDto

/** RA-TAM: Teacher Assignment Management reads & writes (single source of truth). */
interface TeacherAssignmentRepository {
    suspend fun getOverview(token: String, teacherId: String): NetworkResult<ApiResponse<TeacherAssignmentOverviewDto>>
    suspend fun getOptions(token: String): NetworkResult<ApiResponse<AssignmentOptionsDto>>
    suspend fun bulkAssign(token: String, teacherId: String, request: AssignTeacherClassesRequest): NetworkResult<ApiResponse<BulkAssignResponseDto>>
    suspend fun removeAssignment(token: String, teacherId: String, assignmentId: String): NetworkResult<ApiResponse<Unit>>
}
