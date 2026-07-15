package com.littlebridge.vidyaprayag.feature.teacher

import com.littlebridge.vidyaprayag.core.fail
import com.littlebridge.vidyaprayag.core.ok
import com.littlebridge.vidyaprayag.core.created
import com.littlebridge.vidyaprayag.core.principalUserId
import com.littlebridge.vidyaprayag.db.DatabaseFactory.dbQuery
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.UUID

// ── Tables ──

object TeacherTestsTable : Table("teacher_tests") {
    val id = uuid("id").clientDefault { UUID.randomUUID() }
    val schoolId = uuid("school_id")
    val teacherId = uuid("teacher_id")
    val title = varchar("title", 255)
    val subject = varchar("subject", 100)
    val className = varchar("class_name", 50)
    val totalMarks = integer("total_marks")
    val testDate = varchar("test_date", 20)
    val createdAt = long("created_at").clientDefault { System.currentTimeMillis() }

    override val primaryKey = PrimaryKey(id)
}

object TeacherHomeworkTable : Table("teacher_homework") {
    val id = uuid("id").clientDefault { UUID.randomUUID() }
    val schoolId = uuid("school_id")
    val teacherId = uuid("teacher_id")
    val title = varchar("title", 255)
    val subject = varchar("subject", 100)
    val className = varchar("class_name", 50)
    val description = text("description").default("")
    val dueDate = varchar("due_date", 20)
    val createdAt = long("created_at").clientDefault { System.currentTimeMillis() }

    override val primaryKey = PrimaryKey(id)
}

// ── DTOs ──

@Serializable
data class TeacherTimetableEntryDto(
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
data class TeacherTimetableResponseDto(
    val day: String,
    val entries: List<TeacherTimetableEntryDto>
)

@Serializable
data class TeacherClassDto(
    val id: String,
    @SerialName("class_name") val className: String,
    val subject: String,
    @SerialName("student_count") val studentCount: Int,
    @SerialName("next_period") val nextPeriod: String? = null
)

@Serializable
data class TeacherClassesResponseDto(
    val classes: List<TeacherClassDto>
)

@Serializable
data class TeacherTestDto(
    val id: String,
    val title: String,
    val subject: String,
    @SerialName("class_name") val className: String,
    @SerialName("total_marks") val totalMarks: Int,
    @SerialName("test_date") val testDate: String,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class TeacherTestsResponseDto(
    val tests: List<TeacherTestDto>
)

@Serializable
data class CreateTestRequestDto(
    val title: String,
    val subject: String,
    @SerialName("class_name") val className: String,
    @SerialName("total_marks") val totalMarks: Int,
    @SerialName("test_date") val testDate: String
)

@Serializable
data class CreateTestResponseDto(
    @SerialName("test_id") val testId: String
)

@Serializable
data class TeacherHomeworkDto(
    val id: String,
    val title: String,
    val subject: String,
    @SerialName("class_name") val className: String,
    val description: String,
    @SerialName("due_date") val dueDate: String,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class TeacherHomeworkResponseDto(
    val homework: List<TeacherHomeworkDto>
)

@Serializable
data class CreateHomeworkRequestDto(
    val title: String,
    val subject: String,
    @SerialName("class_name") val className: String,
    val description: String = "",
    @SerialName("due_date") val dueDate: String
)

@Serializable
data class CreateHomeworkResponseDto(
    @SerialName("homework_id") val homeworkId: String
)

@Serializable
data class CurriculumLessonDto(
    val id: String,
    val title: String,
    @SerialName("is_completed") val isCompleted: Boolean = false
)

@Serializable
data class CurriculumUnitDto(
    val id: String,
    val title: String,
    val subject: String,
    @SerialName("unit_number") val unitNumber: Int,
    val lessons: List<CurriculumLessonDto> = emptyList(),
    @SerialName("is_completed") val isCompleted: Boolean = false
)

@Serializable
data class TeacherCurriculumResponseDto(
    val units: List<CurriculumUnitDto>
)

@Serializable
data class TeacherHomeResponseDto(
    @SerialName("teacher_name") val teacherName: String = "",
    @SerialName("teacher_subject") val teacherSubject: String = "",
    @SerialName("teacher_image_url") val teacherImageUrl: String? = null,
    @SerialName("class_count") val classCount: Int = 0,
    @SerialName("student_count") val studentCount: Int = 0,
    @SerialName("today_classes") val todayClasses: List<TeacherTimetableEntryDto> = emptyList(),
    @SerialName("pending_homework") val pendingHomework: Int = 0,
    @SerialName("unread_messages") val unreadMessages: Int = 0
)

@Serializable
data class TeacherProfileResponseDto(
    @SerialName("teacher_name") val teacherName: String = "",
    @SerialName("teacher_subject") val teacherSubject: String = "",
    @SerialName("teacher_image_url") val teacherImageUrl: String? = null,
    val email: String = "",
    val phone: String = "",
    @SerialName("joining_date") val joiningDate: String = "",
    val salary: String = "",
    @SerialName("classes_taught") val classesTaught: Int = 0,
    @SerialName("total_students") val totalStudents: Int = 0
)

// ── Routing ──

fun Route.teacherRouting() {
    authenticate("jwt") {
        route("/api/v1/teacher") {

            get("/home") {
                val uid = call.principalUserId()?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid token", HttpStatusCode.Unauthorized); return@get }
                call.ok(TeacherHomeResponseDto())
            }

            get("/timetable") {
                val day = call.request.queryParameters["day"] ?: "Mon"
                call.ok(TeacherTimetableResponseDto(day = day, entries = emptyList()))
            }

            get("/classes") {
                call.ok(TeacherClassesResponseDto(classes = emptyList()))
            }

            get("/tests") {
                val className = call.request.queryParameters["class"]
                val subject = call.request.queryParameters["subject"]
                call.ok(TeacherTestsResponseDto(tests = emptyList()))
            }

            post("/tests") {
                val req = call.receive<CreateTestRequestDto>()
                if (req.title.isBlank() || req.subject.isBlank() || req.className.isBlank()) {
                    call.fail("title, subject, and class are required"); return@post
                }
                val uid = call.principalUserId()?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid token", HttpStatusCode.Unauthorized); return@post }
                val testId = UUID.randomUUID()
                dbQuery {
                    TeacherTestsTable.insert {
                        it[id] = testId
                        it[TeacherTestsTable.schoolId] = uid
                        it[teacherId] = uid
                        it[title] = req.title
                        it[subject] = req.subject
                        it[className] = req.className
                        it[totalMarks] = req.totalMarks
                        it[testDate] = req.testDate
                    }
                }
                call.created(CreateTestResponseDto(testId.toString()))
            }

            get("/homework") {
                val className = call.request.queryParameters["class"]
                call.ok(TeacherHomeworkResponseDto(homework = emptyList()))
            }

            post("/homework") {
                val req = call.receive<CreateHomeworkRequestDto>()
                if (req.title.trim().length < 3) {
                    call.fail("Title must be at least 3 characters"); return@post
                }
                if (req.subject.isBlank() || req.className.isBlank()) {
                    call.fail("subject and class are required"); return@post
                }
                val uid = call.principalUserId()?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid token", HttpStatusCode.Unauthorized); return@post }
                val hwId = UUID.randomUUID()
                dbQuery {
                    TeacherHomeworkTable.insert {
                        it[id] = hwId
                        it[TeacherHomeworkTable.schoolId] = uid
                        it[teacherId] = uid
                        it[title] = req.title.trim()
                        it[subject] = req.subject
                        it[className] = req.className
                        it[description] = req.description
                        it[dueDate] = req.dueDate
                    }
                }
                call.created(CreateHomeworkResponseDto(hwId.toString()))
            }

            get("/curriculum") {
                val subject = call.request.queryParameters["subject"]
                call.ok(TeacherCurriculumResponseDto(units = emptyList()))
            }

            get("/profile") {
                call.ok(TeacherProfileResponseDto())
            }
        }
    }
}
