package com.littlebridge.enrollplus.feature.idcard.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class IdCardTemplateDto(
    val id: String,
    val schoolId: String,
    val name: String,
    val roleType: String,
    val frontConfig: String,
    val backConfig: String,
    val isActive: Boolean,
    val createdAt: String,
)

@Serializable
data class CreateTemplateRequest(
    val name: String,
    val roleType: String,
    val frontConfig: String,
    val backConfig: String,
)

@Serializable
data class GenerateIdCardRequest(
    val templateId: String,
    val scope: String,
    val classId: String? = null,
)

@Serializable
data class GenerateIdCardResponse(
    val cardIds: List<String>,
    val pdfUrl: String? = null,
    val count: Int,
)

@Serializable
data class IdCardDto(
    val id: String,
    val schoolId: String? = null,
    val personId: String,
    val personType: String,
    val personName: String,
    val pdfUrl: String? = null,
    val digitalCardUrl: String? = null,
    val qrCodeData: String,
    val validTill: String? = null,
    val status: String = "ready",
)
