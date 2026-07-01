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
