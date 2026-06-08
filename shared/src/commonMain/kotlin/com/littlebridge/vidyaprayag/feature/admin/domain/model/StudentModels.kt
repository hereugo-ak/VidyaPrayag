/*
 * File: StudentModels.kt
 * Module: feature.admin.domain.model
 *
 * RA-45: DTOs for the admin student roster + student profile + teacher profile
 * detail. Mirror server: feature.school.SchoolStudentsRouting.kt
 *   GET    /api/v1/school/students
 *   POST   /api/v1/school/students
 *   DELETE /api/v1/school/students/{id}
 *   GET    /api/v1/school/students/{id}
 *   GET    /api/v1/school/teachers/{id}
 *
 * @SerialName mirrors the server so the same JSON decodes on both sides.
 */
package com.littlebridge.vidyaprayag.feature.admin.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StudentDto(
    val id: String,
    @SerialName("student_code") val studentCode: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("class_name") val className: String,
    val section: String,
    @SerialName("roll_number") val rollNumber: String,
    @SerialName("profile_photo_url") val profilePhotoUrl: String? = null
)

@Serializable
data class StudentListResponse(val students: List<StudentDto>)

@Serializable
data class CreateStudentRequest(
    @SerialName("full_name") val fullName: String,
    @SerialName("class_name") val className: String,
    val section: String? = null,
    @SerialName("roll_number") val rollNumber: String,
    @SerialName("student_code") val studentCode: String? = null
)

@Serializable
data class AttendanceDayDto(val date: String, val status: String)

@Serializable
data class StudentMarkDto(
    val subject: String,
    @SerialName("assessment") val assessmentName: String,
    val marks: Double? = null,
    @SerialName("max_marks") val maxMarks: Int,
    @SerialName("exam_date") val examDate: String? = null
)

@Serializable
data class StudentLeaveDto(
    @SerialName("date_from") val dateFrom: String,
    @SerialName("date_to") val dateTo: String,
    val reason: String,
    val status: String
)

@Serializable
data class StudentFeeDto(
    val title: String,
    val amount: Double,
    val currency: String,
    val status: String,
    @SerialName("due_date") val dueDate: String? = null
)

@Serializable
data class StudentProfileDto(
    val student: StudentDto,
    @SerialName("present_days") val presentDays: Int,
    @SerialName("absent_days") val absentDays: Int,
    @SerialName("late_days") val lateDays: Int,
    @SerialName("attendance_rate") val attendanceRate: Int,
    @SerialName("recent_attendance") val recentAttendance: List<AttendanceDayDto>,
    val marks: List<StudentMarkDto>,
    val leave: List<StudentLeaveDto>,
    val fees: List<StudentFeeDto>
)

@Serializable
data class TeacherAssignmentDto(
    @SerialName("class_name") val className: String,
    val section: String,
    val subject: String
)

@Serializable
data class TeacherProfileDto(
    val id: String,
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val role: String,
    val assignments: List<TeacherAssignmentDto>,
    @SerialName("class_count") val classCount: Int,
    @SerialName("subject_count") val subjectCount: Int
)
