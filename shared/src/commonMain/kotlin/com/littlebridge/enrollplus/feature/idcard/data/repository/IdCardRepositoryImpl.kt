package com.littlebridge.enrollplus.feature.idcard.data.repository

import com.littlebridge.enrollplus.feature.idcard.data.remote.IdCardApi
import com.littlebridge.enrollplus.feature.idcard.domain.model.*
import com.littlebridge.enrollplus.feature.idcard.domain.repository.IdCardRepository

class IdCardRepositoryImpl(
    private val api: IdCardApi,
) : IdCardRepository {

    override suspend fun getTemplates(token: String) = api.getTemplates(token)
    override suspend fun createTemplate(token: String, request: CreateTemplateRequest) = api.createTemplate(token, request)
    override suspend fun deactivateTemplate(token: String, templateId: String) = api.deactivateTemplate(token, templateId)
    override suspend fun generateCards(token: String, request: GenerateIdCardRequest) = api.generateCards(token, request)
    override suspend fun getCards(token: String) = api.getCards(token)
    override suspend fun getPdfUrl(token: String, cardId: String) = api.getPdfUrl(token, cardId)
    override suspend fun deleteCard(token: String, cardId: String) = api.deleteCard(token, cardId)
    override suspend fun getChildIdCard(token: String, childId: String) = api.getChildIdCard(token, childId)
    override suspend fun getTeacherIdCard(token: String) = api.getTeacherIdCard(token)
    override suspend fun getStaffIdCard(token: String) = api.getStaffIdCard(token)
}
