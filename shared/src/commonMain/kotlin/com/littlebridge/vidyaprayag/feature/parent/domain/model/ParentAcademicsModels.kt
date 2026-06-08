package com.littlebridge.vidyaprayag.feature.parent.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─────────────────────────────────────────────────────────────────────────────
// RA-43 + RA-56: child-scoped parent academic reads. Mirror the server DTOs in
// server/.../feature/parent/ParentAcademicsRouting.kt. Every read is for ONE
// child (resolved + ownership-checked server-side), so a multi-child parent sees
// per-child data instead of silently the first child only.
// ─────────────────────────────────────────────────────────────────────────────

// ── Attendance ──
@Serializable
data class ParentAttendanceResponse(
    val success: Boolean,
    val data: ParentAttendanceData,
)

@Serializable
data class ParentAttendanceData(
    @SerialName("child_name") val childName: String,
    @SerialName("present_days") val presentDays: Int = 0,
    @SerialName("absent_days") val absentDays: Int = 0,
    @SerialName("late_days") val lateDays: Int = 0,
    @SerialName("total_days") val totalDays: Int = 0,
    @SerialName("attendance_rate") val attendanceRate: Int = 0,
    val records: List<ParentAttendanceDayDto> = emptyList(),
)

@Serializable
data class ParentAttendanceDayDto(
    val date: String,
    val status: String, // present | absent | late
)

// ── Marks ──
@Serializable
data class ParentMarksResponse(
    val success: Boolean,
    val data: ParentMarksData,
)

@Serializable
data class ParentMarksData(
    @SerialName("child_name") val childName: String,
    val results: List<ParentMarkDto> = emptyList(),
)

@Serializable
data class ParentMarkDto(
    @SerialName("exam_name") val examName: String,
    val subject: String,
    val marks: Double? = null,
    @SerialName("max_marks") val maxMarks: Int,
    @SerialName("exam_date") val examDate: String? = null,
)

// ── Syllabus ──
@Serializable
data class ParentSyllabusResponse(
    val success: Boolean,
    val data: ParentSyllabusData,
)

@Serializable
data class ParentSyllabusData(
    @SerialName("child_name") val childName: String,
    @SerialName("class_name") val className: String,
    val subjects: List<ParentSyllabusSubjectDto> = emptyList(),
)

@Serializable
data class ParentSyllabusSubjectDto(
    val subject: String,
    val progress: Int = 0,
    val units: List<ParentSyllabusUnitDto> = emptyList(),
)

@Serializable
data class ParentSyllabusUnitDto(
    val title: String,
    @SerialName("is_covered") val isCovered: Boolean,
    @SerialName("covered_on") val coveredOn: String? = null,
)
