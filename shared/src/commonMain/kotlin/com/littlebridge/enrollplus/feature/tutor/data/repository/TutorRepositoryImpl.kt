package com.littlebridge.enrollplus.feature.tutor.data.repository

import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.feature.tutor.data.remote.TutorApi
import com.littlebridge.enrollplus.feature.tutor.domain.model.*
import com.littlebridge.enrollplus.feature.tutor.domain.repository.TutorRepository

class TutorRepositoryImpl(
    private val api: TutorApi,
) : TutorRepository {
    override suspend fun getSubjects(token: String, childId: String): NetworkResult<SubjectsResponse> =
        api.getSubjects(token, childId)

    override suspend fun askDoubt(token: String, request: DoubtRequest): NetworkResult<DoubtResponse> =
        api.askDoubt(token, request)

    override suspend fun getLearnerBundle(token: String, childId: String, subjectId: String): NetworkResult<LearnerBundleResponse> =
        api.getLearnerBundle(token, childId, subjectId)

    override suspend fun gradePractice(token: String, request: PracticeGradeRequest): NetworkResult<PracticeGradeResponse> =
        api.gradePractice(token, request)

    override suspend fun getPlan(token: String, childId: String, subjectId: String?): NetworkResult<PlanResponse> =
        api.getPlan(token, childId, subjectId)

    override suspend fun getTeacherScope(token: String): NetworkResult<TeacherScopeResponse> =
        api.getTeacherScope(token)

    override suspend fun getHeatmap(token: String, classId: String, subjectId: String): NetworkResult<HeatmapResponse> =
        api.getHeatmap(token, classId, subjectId)

    override suspend fun getProgressCard(token: String, childId: String, subjectId: String): NetworkResult<ProgressCardResponse> =
        api.getProgressCard(token, childId, subjectId)

    override suspend fun getEfficacy(token: String, childId: String, subjectId: String): NetworkResult<EfficacyResponse> =
        api.getEfficacy(token, childId, subjectId)

    override suspend fun getModuleStatus(token: String): NetworkResult<ModuleStatusResponse> =
        api.getModuleStatus(token)
}
