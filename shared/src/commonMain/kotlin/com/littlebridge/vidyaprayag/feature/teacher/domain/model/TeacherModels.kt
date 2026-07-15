package com.littlebridge.vidyaprayag.feature.teacher.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Home ──

@Serializable
data class TeacherHomeResponse(
    @SerialName("teacher_name") val teacherName: String = "",
    @SerialName("teacher_subject") val teacherSubject: String = "",
    @SerialName("teacher_image_url") val teacherImageUrl: String? = null,
    @SerialName("class_count") val classCount: Int = 0,
    @SerialName("student_count") val studentCount: Int = 0,
    @SerialName("today_classes") val todayClasses: List<TeacherTimetableEntry> = emptyList(),
    @SerialName("pending_homework") val pendingHomework: Int = 0,
    @SerialName("unread_messages") val unreadMessages: Int = 0
)

// ── Timetable ──

@Serializable
data class TeacherTimetableEntry(
    val id: String,
    val day: String,
    val period: Int,
    @SerialName("class_name") val className: String,
    val subject: String,
    val room: String,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String
)

@Serializable
data class TeacherTimetableResponse(
    val day: String,
    val entries: List<TeacherTimetableEntry>
)

// ── Classes ──

@Serializable
data class TeacherClass(
    val id: String,
    @SerialName("class_name") val className: String,
    val subject: String,
    @SerialName("student_count") val studentCount: Int,
    @SerialName("next_period") val nextPeriod: String? = null
)

@Serializable
data class TeacherClassesResponse(
    val classes: List<TeacherClass>
)

// ── Marks / Tests ──

@Serializable
data class TeacherTest(
    val id: String,
    val title: String,
    val subject: String,
    @SerialName("class_name") val className: String,
    @SerialName("total_marks") val totalMarks: Int,
    @SerialName("test_date") val testDate: String,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class TeacherTestsResponse(
    val tests: List<TeacherTest>
)

@Serializable
data class CreateTestRequest(
    val title: String,
    val subject: String,
    @SerialName("class_name") val className: String,
    @SerialName("total_marks") val totalMarks: Int,
    @SerialName("test_date") val testDate: String
)

@Serializable
data class CreateTestResponse(
    @SerialName("test_id") val testId: String
)

// ── Homework ──

@Serializable
data class TeacherHomework(
    val id: String,
    val title: String,
    val subject: String,
    @SerialName("class_name") val className: String,
    val description: String,
    @SerialName("due_date") val dueDate: String,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class TeacherHomeworkResponse(
    val homework: List<TeacherHomework>
)

@Serializable
data class CreateHomeworkRequest(
    val title: String,
    val subject: String,
    @SerialName("class_name") val className: String,
    val description: String,
    @SerialName("due_date") val dueDate: String
)

@Serializable
data class CreateHomeworkResponse(
    @SerialName("homework_id") val homeworkId: String
)

// ── Curriculum ──

@Serializable
data class CurriculumUnit(
    val id: String,
    val title: String,
    val subject: String,
    @SerialName("unit_number") val unitNumber: Int,
    val lessons: List<CurriculumLesson> = emptyList(),
    @SerialName("is_completed") val isCompleted: Boolean = false
)

@Serializable
data class CurriculumLesson(
    val id: String,
    val title: String,
    @SerialName("is_completed") val isCompleted: Boolean = false
)

@Serializable
data class TeacherCurriculumResponse(
    val units: List<CurriculumUnit>
)

// ── Profile ──

@Serializable
data class TeacherProfileResponse(
    @SerialName("teacher_name") val teacherName: String,
    @SerialName("teacher_subject") val teacherSubject: String,
    @SerialName("teacher_image_url") val teacherImageUrl: String? = null,
    val email: String = "",
    val phone: String = "",
    @SerialName("joining_date") val joiningDate: String = "",
    val salary: String = "",
    @SerialName("classes_taught") val classesTaught: Int = 0,
    @SerialName("total_students") val totalStudents: Int = 0
)
