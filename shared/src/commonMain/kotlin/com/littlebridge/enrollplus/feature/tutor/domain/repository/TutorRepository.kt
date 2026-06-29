package com.littlebridge.enrollplus.feature.tutor.domain.repository

import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.feature.tutor.domain.model.*

interface TutorRepository {
    suspend fun getSubjects(token: String, childId: String): NetworkResult<SubjectsResponse>
    suspend fun askDoubt(token: String, request: DoubtRequest): NetworkResult<DoubtResponse>
    suspend fun getLearnerBundle(token: String, childId: String, subjectId: String): NetworkResult<LearnerBundleResponse>
    suspend fun gradePractice(token: String, request: PracticeGradeRequest): NetworkResult<PracticeGradeResponse>
    suspend fun getPlan(token: String, childId: String, subjectId: String? = null): NetworkResult<PlanResponse>
    suspend fun getTeacherScope(token: String): NetworkResult<TeacherScopeResponse>
    suspend fun getHeatmap(token: String, classId: String, subjectId: String): NetworkResult<HeatmapResponse>
    suspend fun getProgressCard(token: String, childId: String, subjectId: String): NetworkResult<ProgressCardResponse>
    suspend fun getEfficacy(token: String, childId: String, subjectId: String): NetworkResult<EfficacyResponse>
    suspend fun getModuleStatus(token: String): NetworkResult<ModuleStatusResponse>
}
