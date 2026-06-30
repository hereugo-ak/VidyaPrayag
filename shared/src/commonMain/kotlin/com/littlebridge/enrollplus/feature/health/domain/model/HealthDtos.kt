package com.littlebridge.enrollplus.feature.health.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HealthProfileDto(
    val id: String = "",
    @SerialName("student_id") val studentId: String = "",
    @SerialName("blood_group") val bloodGroup: String? = null,
    @SerialName("height_cm") val heightCm: Double? = null,
    @SerialName("weight_kg") val weightKg: Double? = null,
    val allergies: String = "[]",
    @SerialName("chronic_conditions") val chronicConditions: String = "[]",
    val medications: String = "[]",
    @SerialName("emergency_contact_name") val emergencyContactName: String? = null,
    @SerialName("emergency_contact_phone") val emergencyContactPhone: String? = null,
    @SerialName("doctor_name") val doctorName: String? = null,
    @SerialName("doctor_phone") val doctorPhone: String? = null,
)

@Serializable
data class UpsertHealthProfileRequest(
    @SerialName("blood_group") val bloodGroup: String? = null,
    @SerialName("height_cm") val heightCm: Double? = null,
    @SerialName("weight_kg") val weightKg: Double? = null,
    val allergies: String? = null,
    @SerialName("chronic_conditions") val chronicConditions: String? = null,
    val medications: String? = null,
    @SerialName("emergency_contact_name") val emergencyContactName: String? = null,
    @SerialName("emergency_contact_phone") val emergencyContactPhone: String? = null,
    @SerialName("doctor_name") val doctorName: String? = null,
    @SerialName("doctor_phone") val doctorPhone: String? = null,
)

@Serializable
data class ImmunizationDto(
    val id: String = "",
    @SerialName("student_id") val studentId: String = "",
    @SerialName("vaccine_name") val vaccineName: String = "",
    @SerialName("dose_number") val doseNumber: Int = 1,
    @SerialName("date_administered") val dateAdministered: String = "",
    @SerialName("next_due_date") val nextDueDate: String? = null,
    @SerialName("administered_by") val administeredBy: String? = null,
)

@Serializable
data class ImmunizationListResponse(
    val immunizations: List<ImmunizationDto> = emptyList(),
)

@Serializable
data class AddImmunizationRequest(
    @SerialName("student_id") val studentId: String,
    @SerialName("vaccine_name") val vaccineName: String,
    @SerialName("dose_number") val doseNumber: Int = 1,
    @SerialName("date_administered") val dateAdministered: String,
    @SerialName("next_due_date") val nextDueDate: String? = null,
    @SerialName("administered_by") val administeredBy: String? = null,
)

@Serializable
data class HealthIncidentDto(
    val id: String = "",
    @SerialName("student_id") val studentId: String = "",
    val date: String = "",
    val time: String? = null,
    val description: String = "",
    val treatment: String? = null,
    @SerialName("medication_given") val medicationGiven: String? = null,
    @SerialName("parent_notified") val parentNotified: Boolean = false,
    @SerialName("parent_notified_at") val parentNotifiedAt: String? = null,
    @SerialName("attended_by") val attendedBy: String? = null,
    @SerialName("attended_by_name") val attendedByName: String? = null,
    val severity: String = "minor",
)

@Serializable
data class HealthIncidentListResponse(
    val incidents: List<HealthIncidentDto> = emptyList(),
)

@Serializable
data class LogIncidentRequest(
    @SerialName("student_id") val studentId: String,
    val date: String,
    val time: String? = null,
    val description: String,
    val treatment: String? = null,
    @SerialName("medication_given") val medicationGiven: String? = null,
    val severity: String = "minor",
)

@Serializable
data class HealthAlertDto(
    @SerialName("student_id") val studentId: String = "",
    @SerialName("student_name") val studentName: String = "",
    @SerialName("class_name") val className: String = "",
    val section: String = "",
    val allergies: String = "[]",
    @SerialName("chronic_conditions") val chronicConditions: String = "[]",
)

@Serializable
data class HealthAlertsResponse(
    val alerts: List<HealthAlertDto> = emptyList(),
)

@Serializable
data class ParentHealthResponse(
    @SerialName("child_name") val childName: String = "",
    val profile: HealthProfileDto? = null,
    val immunizations: List<ImmunizationDto> = emptyList(),
    val incidents: List<HealthIncidentDto> = emptyList(),
)
