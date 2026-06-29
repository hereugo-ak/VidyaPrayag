package com.littlebridge.enrollplus.feature.idcard.domain.repository

import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.feature.idcard.domain.model.*

interface IdCardRepository {
    suspend fun getTemplates(token: String): NetworkResult<ApiResponse<List<IdCardTemplateDto>>>
    suspend fun createTemplate(token: String, request: CreateTemplateRequest): NetworkResult<ApiResponse<IdCardTemplateDto>>
    suspend fun deactivateTemplate(token: String, templateId: String): NetworkResult<ApiResponse<Unit>>
    suspend fun generateCards(token: String, request: GenerateIdCardRequest): NetworkResult<ApiResponse<GenerateIdCardResponse>>
    suspend fun getCards(token: String): NetworkResult<ApiResponse<List<IdCardDto>>>
    suspend fun getPdfUrl(token: String, cardId: String): NetworkResult<ApiResponse<Map<String, String>>>
    suspend fun deleteCard(token: String, cardId: String): NetworkResult<ApiResponse<Unit>>
    suspend fun getChildIdCard(token: String, childId: String): NetworkResult<ApiResponse<IdCardDto>>
    suspend fun getTeacherIdCard(token: String): NetworkResult<ApiResponse<IdCardDto>>
    suspend fun getStaffIdCard(token: String): NetworkResult<ApiResponse<IdCardDto>>
}
