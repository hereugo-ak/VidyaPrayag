package com.littlebridge.enrollplus.feature.school

import com.littlebridge.enrollplus.core.fail
import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.requireSchoolAdmin
import com.littlebridge.enrollplus.db.AdminSetupStepsTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.FacultyTable
import com.littlebridge.enrollplus.db.SchoolClassesTable
import com.littlebridge.enrollplus.db.SchoolSubjectsTable
import com.littlebridge.enrollplus.db.SchoolsTable
import com.littlebridge.enrollplus.db.StudentsTable
import com.littlebridge.enrollplus.db.TeacherPeriodsTable
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

private const val SETUP_PENDING = "PENDING"
private const val SETUP_IN_PROGRESS = "IN_PROGRESS"
private const val SETUP_COMPLETED = "COMPLETED"
private val SETUP_KEYS = listOf("TEACHERS", "STUDENTS", "SUBJECTS", "CLASSES", "TIMETABLE")
private val SETUP_LABELS = mapOf(
    "TEACHERS" to "Add Teachers",
    "STUDENTS" to "Add Students",
    "SUBJECTS" to "Add Subjects",
    "CLASSES" to "Create Classes",
    "TIMETABLE" to "Create Timetable",
)

@Serializable
data class AdminSetupStepDto(
    val key: String,
    val label: String,
    val status: String,
    val currentCount: Int,
    val targetCount: Int? = null,
)

@Serializable
data class AdminSetupProgressDto(
    val setupComplete: Boolean,
    val completedSteps: Int,
    val totalSteps: Int,
    val steps: List<AdminSetupStepDto>,
)

@Serializable
data class UpdateAdminSetupStepRequest(val status: String)

private data class SetupCounts(
    val teachers: Int,
    val students: Int,
    val subjects: Int,
    val classes: Int,
    val timetable: Int,
    val studentTarget: Int?,
    val classTarget: Int?,
) {
    fun countFor(key: String): Int = when (key) {
        "TEACHERS" -> teachers
        "STUDENTS" -> students
        "SUBJECTS" -> subjects
        "CLASSES" -> classes
        "TIMETABLE" -> timetable
        else -> 0
    }

    fun targetFor(key: String): Int? = when (key) {
        "STUDENTS" -> studentTarget
        "CLASSES" -> classTarget
        else -> null
    }
}

private fun readSetupCounts(schoolId: UUID): SetupCounts {
    val classRows = SchoolClassesTable.selectAll()
        .where { SchoolClassesTable.schoolId eq schoolId }
        .toList()
    val classIds = classRows.map { it[SchoolClassesTable.id].value }
    val subjectCount = if (classIds.isEmpty()) 0 else {
        val predicate = classIds.map { classId -> SchoolSubjectsTable.classId eq classId }
            .reduce { acc, op -> acc or op }
        SchoolSubjectsTable.selectAll().where { predicate }.count().toInt()
    }
    val school = SchoolsTable.selectAll().where { SchoolsTable.id eq schoolId }.singleOrNull()
    return SetupCounts(
        teachers = FacultyTable.selectAll()
            .where { (FacultyTable.schoolId eq schoolId) and (FacultyTable.isActive eq true) }
            .count().toInt(),
        students = StudentsTable.selectAll()
            .where { (StudentsTable.schoolId eq schoolId) and (StudentsTable.isActive eq true) }
            .count().toInt(),
        subjects = subjectCount,
        classes = classRows.size,
        timetable = TeacherPeriodsTable.selectAll()
            .where { (TeacherPeriodsTable.schoolId eq schoolId) and (TeacherPeriodsTable.isActive eq true) }
            .count().toInt(),
        studentTarget = school?.get(SchoolsTable.totalStudents)?.takeIf { it > 0 },
        classTarget = school?.get(SchoolsTable.totalClasses)?.takeIf { it > 0 },
    )
}

private fun upsertSetupStatus(userId: UUID, schoolId: UUID, key: String, status: String) {
    val now = Instant.now()
    val existing = AdminSetupStepsTable.selectAll().where {
        (AdminSetupStepsTable.userId eq userId) and (AdminSetupStepsTable.stepKey eq key)
    }.singleOrNull()
    if (existing == null) {
        AdminSetupStepsTable.insert {
            it[AdminSetupStepsTable.userId] = userId
            it[AdminSetupStepsTable.schoolId] = schoolId
            it[stepKey] = key
            it[AdminSetupStepsTable.status] = status
            it[startedAt] = now
            it[completedAt] = now.takeIf { status == SETUP_COMPLETED }
            it[updatedAt] = now
        }
    } else if (existing[AdminSetupStepsTable.status] != SETUP_COMPLETED) {
        AdminSetupStepsTable.update({ AdminSetupStepsTable.id eq existing[AdminSetupStepsTable.id].value }) {
            it[AdminSetupStepsTable.status] = status
            if (status == SETUP_COMPLETED) it[completedAt] = now
            it[updatedAt] = now
        }
    }
}

private fun setupProgress(userId: UUID, schoolId: UUID): AdminSetupProgressDto {
    val counts = readSetupCounts(schoolId)
    val persisted = AdminSetupStepsTable.selectAll().where {
        (AdminSetupStepsTable.userId eq userId) and (AdminSetupStepsTable.schoolId eq schoolId)
    }.associate { it[AdminSetupStepsTable.stepKey] to it[AdminSetupStepsTable.status] }

    // Real feature data promotes a step permanently. Deleting the last record later
    // does not resurrect a checklist that the admin already finished.
    SETUP_KEYS.forEach { key ->
        if (counts.countFor(key) > 0 && persisted[key] != SETUP_COMPLETED) {
            upsertSetupStatus(userId, schoolId, key, SETUP_COMPLETED)
        }
    }

    val finalStatuses = AdminSetupStepsTable.selectAll().where {
        (AdminSetupStepsTable.userId eq userId) and (AdminSetupStepsTable.schoolId eq schoolId)
    }.associate { it[AdminSetupStepsTable.stepKey] to it[AdminSetupStepsTable.status] }
    val steps = SETUP_KEYS.map { key ->
        AdminSetupStepDto(
            key = key,
            label = SETUP_LABELS.getValue(key),
            status = finalStatuses[key] ?: SETUP_PENDING,
            currentCount = counts.countFor(key),
            targetCount = counts.targetFor(key),
        )
    }
    val completed = steps.count { it.status == SETUP_COMPLETED }
    return AdminSetupProgressDto(
        setupComplete = completed == SETUP_KEYS.size,
        completedSteps = completed,
        totalSteps = SETUP_KEYS.size,
        steps = steps,
    )
}

/** Backend-owned setup checklist state for the currently authenticated school admin. */
fun Route.adminSetupProgressRouting() {
    authenticate("jwt") {
        route("/api/admin/setup") {
            get {
                val ctx = call.requireSchoolAdmin() ?: return@get
                val payload = dbQuery { setupProgress(ctx.userId, ctx.schoolId) }
                call.ok(payload, message = "Setup progress fetched")
            }

            put("/{stepKey}") {
                val ctx = call.requireSchoolAdmin() ?: return@put
                val key = call.parameters["stepKey"]?.uppercase()
                if (key !in SETUP_KEYS) {
                    call.fail("Unknown setup step", HttpStatusCode.BadRequest)
                    return@put
                }
                val request = call.receive<UpdateAdminSetupStepRequest>()
                val status = request.status.uppercase()
                if (status !in setOf(SETUP_IN_PROGRESS, SETUP_COMPLETED)) {
                    call.fail("status must be IN_PROGRESS or COMPLETED", HttpStatusCode.BadRequest)
                    return@put
                }

                val payload = dbQuery {
                    val count = readSetupCounts(ctx.schoolId).countFor(key!!)
                    if (status == SETUP_COMPLETED && count == 0) return@dbQuery null
                    upsertSetupStatus(ctx.userId, ctx.schoolId, key, status)
                    setupProgress(ctx.userId, ctx.schoolId)
                }
                if (payload == null) {
                    call.fail(
                        "Complete the linked setup action before marking this step completed",
                        HttpStatusCode.Conflict,
                        "SETUP_STEP_NOT_READY",
                    )
                    return@put
                }
                call.ok(payload, message = "Setup step updated")
            }
        }
    }
}
