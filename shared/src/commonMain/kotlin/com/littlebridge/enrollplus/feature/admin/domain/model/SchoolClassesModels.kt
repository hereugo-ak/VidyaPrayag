package com.littlebridge.enrollplus.feature.admin.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SchoolClassDto(
    val id: String,
    val code: String,
    val name: String,
    val sections: List<String> = emptyList(),
    @SerialName("subject_count") val subjectCount: Int = 0,
)

@Serializable
data class SchoolClassListResponse(
    val classes: List<SchoolClassDto>,
)

@Serializable
data class CreateSchoolClassRequest(
    val code: String,
    val name: String,
    val sections: List<String> = listOf("A"),
)

@Serializable
data class UpdateSchoolClassRequest(
    val code: String,
    val name: String,
    val sections: List<String> = listOf("A"),
)

@Serializable
data class SchoolSubjectDto(
    val id: String,
    @SerialName("class_id") val classId: String,
    val name: String,
    val code: String,
)

@Serializable
data class SchoolSubjectListResponse(
    val subjects: List<SchoolSubjectDto>,
)

@Serializable
data class CreateSchoolSubjectRequest(
    val name: String,
    val code: String,
)

@Serializable
data class UpdateSchoolSubjectRequest(
    val name: String,
    val code: String,
)

// ── Timetable (read-only admin view) ──────────────────────────────────────────

@Serializable
data class TimetablePeriodDto(
    val id: String,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    @SerialName("class_name") val className: String,
    val section: String,
    val subject: String,
    val room: String,
    @SerialName("teacher_id") val teacherId: String,
    @SerialName("teacher_name") val teacherName: String,
)

@Serializable
data class TimetableWeekdayDto(
    val weekday: Int,
    val periods: List<TimetablePeriodDto>,
)

@Serializable
data class TimetableDto(
    val weekdays: List<TimetableWeekdayDto>,
    val classes: List<String>,
)

// ── Period CRUD (admin) ───────────────────────────────────────────────────────

@Serializable
data class PeriodDetailDto(
    val id: String,
    @SerialName("teacher_id") val teacherId: String,
    @SerialName("assignment_id") val assignmentId: String? = null,
    val weekday: Int,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    @SerialName("class_name") val className: String,
    val section: String,
    val subject: String,
    val room: String,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("valid_from") val validFrom: String? = null,
    @SerialName("valid_to") val validTo: String? = null,
)

@Serializable
data class CreatePeriodRequest(
    @SerialName("assignment_id") val assignmentId: String,
    val weekday: Int,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    val room: String = "",
    @SerialName("valid_from") val validFrom: String? = null,
    @SerialName("valid_to") val validTo: String? = null,
)

@Serializable
data class UpdatePeriodRequest(
    val weekday: Int? = null,
    @SerialName("start_time") val startTime: String? = null,
    @SerialName("end_time") val endTime: String? = null,
    val room: String? = null,
    @SerialName("is_active") val isActive: Boolean? = null,
    @SerialName("valid_from") val validFrom: String? = null,
    @SerialName("valid_to") val validTo: String? = null,
)

// ── Period Exceptions ─────────────────────────────────────────────────────────

@Serializable
data class PeriodExceptionDto(
    val id: String,
    @SerialName("period_id") val periodId: String? = null,
    val date: String,
    val kind: String,
    @SerialName("new_start") val newStart: String? = null,
    @SerialName("new_end") val newEnd: String? = null,
    @SerialName("new_room") val newRoom: String? = null,
    @SerialName("substitute_teacher_id") val substituteTeacherId: String? = null,
    @SerialName("substitute_teacher_name") val substituteTeacherName: String? = null,
    @SerialName("assignment_id") val assignmentId: String? = null,
    val note: String = "",
    @SerialName("class_name") val className: String = "",
    val section: String = "",
    val subject: String = "",
)

@Serializable
data class CreateExceptionRequest(
    @SerialName("period_id") val periodId: String? = null,
    val date: String,
    val kind: String,
    @SerialName("new_start") val newStart: String? = null,
    @SerialName("new_end") val newEnd: String? = null,
    @SerialName("new_room") val newRoom: String? = null,
    @SerialName("substitute_teacher_id") val substituteTeacherId: String? = null,
    @SerialName("assignment_id") val assignmentId: String? = null,
    val note: String = "",
)

@Serializable
data class PeriodExceptionListResponse(
    val exceptions: List<PeriodExceptionDto>,
)

// ── Timetable Change Requests (teacher → admin approval) ──────────────────────

@Serializable
data class TimetableChangeRequestDto(
    val id: String,
    @SerialName("teacher_id") val teacherId: String,
    @SerialName("teacher_name") val teacherName: String = "",
    @SerialName("assignment_id") val assignmentId: String? = null,
    @SerialName("period_id") val periodId: String? = null,
    val kind: String,
    val weekday: Int,
    @SerialName("start_time") val startTime: String? = null,
    @SerialName("end_time") val endTime: String? = null,
    val room: String = "",
    val reason: String = "",
    val status: String,
    @SerialName("admin_note") val adminNote: String = "",
    @SerialName("reviewed_by") val reviewedBy: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("reviewed_at") val reviewedAt: String? = null,
    @SerialName("class_name") val className: String = "",
    val section: String = "",
    val subject: String = "",
)

@Serializable
data class CreateChangeRequestRequest(
    @SerialName("assignment_id") val assignmentId: String? = null,
    @SerialName("period_id") val periodId: String? = null,
    val kind: String,
    val weekday: Int,
    @SerialName("start_time") val startTime: String? = null,
    @SerialName("end_time") val endTime: String? = null,
    val room: String = "",
    val reason: String = "",
)

@Serializable
data class ReviewRequest(
    @SerialName("admin_note") val adminNote: String = "",
)

@Serializable
data class ChangeRequestListResponse(
    val requests: List<TimetableChangeRequestDto>,
)
