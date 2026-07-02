package com.littlebridge.enrollplus.feature.admin.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SchoolDaySlotDto(
    @SerialName("slot_index") val slotIndex: Int,
    @SerialName("slot_type") val slotType: String,
    val label: String,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    @SerialName("is_double") val isDouble: Boolean = false,
    @SerialName("double_group") val doubleGroup: Int = 0,
)

@Serializable
data class SchoolDayConfigDto(
    val id: String,
    val name: String,
    @SerialName("applicable_days") val applicableDays: String,
    @SerialName("class_level") val classLevel: String,
    val slots: List<SchoolDaySlotDto>,
    @SerialName("is_active") val isActive: Boolean,
)

@Serializable
data class SchoolDayConfigListResponse(
    val configs: List<SchoolDayConfigDto>,
)

@Serializable
data class CreateSchoolDayConfigRequest(
    val name: String,
    @SerialName("applicable_days") val applicableDays: String,
    @SerialName("class_level") val classLevel: String,
    val slots: List<SchoolDaySlotDto>,
)

@Serializable
data class UpdateSchoolDayConfigRequest(
    val name: String,
    @SerialName("applicable_days") val applicableDays: String,
    @SerialName("class_level") val classLevel: String,
    val slots: List<SchoolDaySlotDto>,
    @SerialName("is_active") val isActive: Boolean = true,
)
