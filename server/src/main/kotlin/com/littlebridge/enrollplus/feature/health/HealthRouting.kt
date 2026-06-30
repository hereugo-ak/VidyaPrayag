/*
 * File: HealthRouting.kt
 * Module: feature.health
 *
 * Student Health Records — server surface (HEALTH_RECORDS_SPEC.md — P1-12).
 *
 * Three role-based surfaces, all JWT-authenticated:
 *
 *   Admin/Nurse (school-scoped, requireSchoolContext):
 *     GET    /api/v1/school/health/profiles/{studentId}          get or create-empty health profile
 *     POST   /api/v1/school/health/profiles/{studentId}          upsert health profile
 *     GET    /api/v1/school/health/immunizations/{studentId}     list immunizations
 *     POST   /api/v1/school/health/immunizations                 add immunization record
 *     GET    /api/v1/school/health/incidents                     list incidents (filter: student_id, date_from, date_to)
 *     POST   /api/v1/school/health/incidents                     log a health incident
 *     PATCH  /api/v1/school/health/incidents/{id}/notify         mark parent notified
 *
 *   Teacher (requireTeacherContext):
 *     GET    /api/v1/teacher/health/alerts                       allergy + condition alerts for assigned classes
 *
 *   Parent (principalUserId ownership gate):
 *     GET    /api/v1/parent/health/{childId}                     child's full health record (profile + immunizations + incidents)
 *
 * Every school-side read/write is constrained to ctx.schoolId (IDOR-safe).
 * Parent reads verify child ownership via children.parent_id = jwt.sub.
 */
package com.littlebridge.enrollplus.feature.health

import com.littlebridge.enrollplus.core.created
import com.littlebridge.enrollplus.core.fail
import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.requireSchoolContext
import com.littlebridge.enrollplus.core.requireTeacherContext
import com.littlebridge.enrollplus.core.teacherAssignmentsFor
import com.littlebridge.enrollplus.core.principalUserId
import com.littlebridge.enrollplus.db.ChildrenTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.EnrollmentsTable
import com.littlebridge.enrollplus.db.StudentHealthIncidentsTable
import com.littlebridge.enrollplus.db.StudentHealthProfilesTable
import com.littlebridge.enrollplus.db.StudentImmunizationsTable
import com.littlebridge.enrollplus.db.StudentsTable
import com.littlebridge.enrollplus.db.TeacherSubjectAssignmentsTable
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

// ════════════════════════════ DTOs ════════════════════════════

@Serializable
data class HealthProfileDto(
    val id: String,
    @SerialName("student_id") val studentId: String,
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
    val id: String,
    @SerialName("student_id") val studentId: String,
    @SerialName("vaccine_name") val vaccineName: String,
    @SerialName("dose_number") val doseNumber: Int = 1,
    @SerialName("date_administered") val dateAdministered: String,
    @SerialName("next_due_date") val nextDueDate: String? = null,
    @SerialName("administered_by") val administeredBy: String? = null,
)

@Serializable
data class ImmunizationListResponse(val immunizations: List<ImmunizationDto>)

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
    val id: String,
    @SerialName("student_id") val studentId: String,
    val date: String,
    val time: String? = null,
    val description: String,
    val treatment: String? = null,
    @SerialName("medication_given") val medicationGiven: String? = null,
    @SerialName("parent_notified") val parentNotified: Boolean = false,
    @SerialName("parent_notified_at") val parentNotifiedAt: String? = null,
    @SerialName("attended_by") val attendedBy: String? = null,
    @SerialName("attended_by_name") val attendedByName: String? = null,
    val severity: String = "minor",
)

@Serializable
data class HealthIncidentListResponse(val incidents: List<HealthIncidentDto>)

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
    @SerialName("student_id") val studentId: String,
    @SerialName("student_name") val studentName: String,
    @SerialName("class_name") val className: String,
    val section: String,
    val allergies: String,
    @SerialName("chronic_conditions") val chronicConditions: String,
)

@Serializable
data class HealthAlertsResponse(val alerts: List<HealthAlertDto>)

@Serializable
data class ParentHealthResponse(
    @SerialName("child_name") val childName: String,
    val profile: HealthProfileDto? = null,
    val immunizations: List<ImmunizationDto> = emptyList(),
    val incidents: List<HealthIncidentDto> = emptyList(),
)

// ════════════════════════════ Row mappers ════════════════════════════

private fun ResultRow.toProfileDto(): HealthProfileDto = HealthProfileDto(
    id = this[StudentHealthProfilesTable.id].value.toString(),
    studentId = this[StudentHealthProfilesTable.studentId].toString(),
    bloodGroup = this[StudentHealthProfilesTable.bloodGroup],
    heightCm = this[StudentHealthProfilesTable.heightCm],
    weightKg = this[StudentHealthProfilesTable.weightKg],
    allergies = this[StudentHealthProfilesTable.allergies],
    chronicConditions = this[StudentHealthProfilesTable.chronicConditions],
    medications = this[StudentHealthProfilesTable.medications],
    emergencyContactName = this[StudentHealthProfilesTable.emergencyContactName],
    emergencyContactPhone = this[StudentHealthProfilesTable.emergencyContactPhone],
    doctorName = this[StudentHealthProfilesTable.doctorName],
    doctorPhone = this[StudentHealthProfilesTable.doctorPhone],
)

private fun ResultRow.toImmunizationDto(): ImmunizationDto = ImmunizationDto(
    id = this[StudentImmunizationsTable.id].value.toString(),
    studentId = this[StudentImmunizationsTable.studentId].toString(),
    vaccineName = this[StudentImmunizationsTable.vaccineName],
    doseNumber = this[StudentImmunizationsTable.doseNumber],
    dateAdministered = this[StudentImmunizationsTable.dateAdministered].toString(),
    nextDueDate = this[StudentImmunizationsTable.nextDueDate]?.toString(),
    administeredBy = this[StudentImmunizationsTable.administeredBy],
)

private fun ResultRow.toIncidentDto(): HealthIncidentDto = HealthIncidentDto(
    id = this[StudentHealthIncidentsTable.id].value.toString(),
    studentId = this[StudentHealthIncidentsTable.studentId].toString(),
    date = this[StudentHealthIncidentsTable.date].toString(),
    time = this[StudentHealthIncidentsTable.time],
    description = this[StudentHealthIncidentsTable.description],
    treatment = this[StudentHealthIncidentsTable.treatment],
    medicationGiven = this[StudentHealthIncidentsTable.medicationGiven],
    parentNotified = this[StudentHealthIncidentsTable.parentNotified],
    parentNotifiedAt = this[StudentHealthIncidentsTable.parentNotifiedAt]?.toString(),
    attendedBy = this[StudentHealthIncidentsTable.attendedBy]?.toString(),
    attendedByName = this[StudentHealthIncidentsTable.attendedByName],
    severity = this[StudentHealthIncidentsTable.severity],
)

// ════════════════════════════ Helpers ════════════════════════════

private val VALID_SEVERITIES = setOf("minor", "moderate", "major")

private fun parseDate(s: String): LocalDate? =
    runCatching { LocalDate.parse(s) }.getOrNull()

private fun verifyStudentInSchool(schoolId: UUID, studentId: UUID): Boolean =
    StudentsTable.selectAll().where {
        (StudentsTable.id eq studentId) and (StudentsTable.schoolId eq schoolId)
    }.any()

// ════════════════════════════ Routing ════════════════════════════

fun Route.healthRouting() {
    authenticate("jwt") {

        // ── Admin/Nurse: /api/v1/school/health ──────────────────────
        route("/api/v1/school/health") {

            // ---- health profile: get or empty ----
            get("/profiles/{studentId}") {
                val ctx = call.requireSchoolContext() ?: return@get
                val studentId = call.parameters["studentId"]
                    ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid studentId"); return@get }

                val belongs = dbQuery { verifyStudentInSchool(ctx.schoolId, studentId) }
                if (!belongs) {
                    call.fail("Student not found in your school", HttpStatusCode.NotFound, "STUDENT_NOT_FOUND")
                    return@get
                }

                val dto = dbQuery {
                    StudentHealthProfilesTable.selectAll()
                        .where { StudentHealthProfilesTable.studentId eq studentId }
                        .firstOrNull()?.toProfileDto()
                }
                if (dto == null) {
                    call.ok(HealthProfileDto(id = "", studentId = studentId.toString()), message = "No health profile yet")
                } else {
                    call.ok(dto, message = "Health profile fetched")
                }
            }

            // ---- health profile: upsert ----
            post("/profiles/{studentId}") {
                val ctx = call.requireSchoolContext() ?: return@post
                val studentId = call.parameters["studentId"]
                    ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid studentId"); return@post }

                val belongs = dbQuery { verifyStudentInSchool(ctx.schoolId, studentId) }
                if (!belongs) {
                    call.fail("Student not found in your school", HttpStatusCode.NotFound, "STUDENT_NOT_FOUND")
                    return@post
                }

                val req = runCatching { call.receive<UpsertHealthProfileRequest>() }.getOrNull()
                    ?: run { call.fail("Invalid body"); return@post }

                val now = Instant.now()
                val dto = dbQuery {
                    val existing = StudentHealthProfilesTable.selectAll()
                        .where { StudentHealthProfilesTable.studentId eq studentId }
                        .firstOrNull()

                    if (existing == null) {
                        StudentHealthProfilesTable.insert {
                            it[StudentHealthProfilesTable.schoolId] = ctx.schoolId
                            it[StudentHealthProfilesTable.studentId] = studentId
                            it[bloodGroup] = req.bloodGroup?.trim()
                            it[heightCm] = req.heightCm
                            it[weightKg] = req.weightKg
                            it[allergies] = req.allergies ?: "[]"
                            it[chronicConditions] = req.chronicConditions ?: "[]"
                            it[medications] = req.medications ?: "[]"
                            it[emergencyContactName] = req.emergencyContactName?.trim()
                            it[emergencyContactPhone] = req.emergencyContactPhone?.trim()
                            it[doctorName] = req.doctorName?.trim()
                            it[doctorPhone] = req.doctorPhone?.trim()
                            it[createdAt] = now
                            it[updatedAt] = now
                        }
                        StudentHealthProfilesTable.selectAll()
                            .where { StudentHealthProfilesTable.studentId eq studentId }
                            .first().toProfileDto()
                    } else {
                        StudentHealthProfilesTable.update({
                            StudentHealthProfilesTable.studentId eq studentId
                        }) {
                            req.bloodGroup?.let { v -> it[bloodGroup] = v.trim() }
                            req.heightCm?.let { v -> it[heightCm] = v }
                            req.weightKg?.let { v -> it[weightKg] = v }
                            req.allergies?.let { v -> it[allergies] = v }
                            req.chronicConditions?.let { v -> it[chronicConditions] = v }
                            req.medications?.let { v -> it[medications] = v }
                            req.emergencyContactName?.let { v -> it[emergencyContactName] = v.trim() }
                            req.emergencyContactPhone?.let { v -> it[emergencyContactPhone] = v.trim() }
                            req.doctorName?.let { v -> it[doctorName] = v.trim() }
                            req.doctorPhone?.let { v -> it[doctorPhone] = v.trim() }
                            it[updatedAt] = now
                        }
                        StudentHealthProfilesTable.selectAll()
                            .where { StudentHealthProfilesTable.studentId eq studentId }
                            .first().toProfileDto()
                    }
                }
                call.ok(dto, message = "Health profile saved")
            }

            // ---- immunizations: list for a student ----
            get("/immunizations/{studentId}") {
                val ctx = call.requireSchoolContext() ?: return@get
                val studentId = call.parameters["studentId"]
                    ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid studentId"); return@get }

                val belongs = dbQuery { verifyStudentInSchool(ctx.schoolId, studentId) }
                if (!belongs) {
                    call.fail("Student not found in your school", HttpStatusCode.NotFound, "STUDENT_NOT_FOUND")
                    return@get
                }

                val rows = dbQuery {
                    StudentImmunizationsTable.selectAll()
                        .where { StudentImmunizationsTable.studentId eq studentId }
                        .orderBy(StudentImmunizationsTable.dateAdministered to SortOrder.DESC)
                        .map { it.toImmunizationDto() }
                }
                call.ok(ImmunizationListResponse(rows), message = "Immunizations fetched")
            }

            // ---- immunizations: add a record ----
            post("/immunizations") {
                val ctx = call.requireSchoolContext() ?: return@post
                val req = runCatching { call.receive<AddImmunizationRequest>() }.getOrNull()
                    ?: run { call.fail("Invalid body"); return@post }

                if (req.vaccineName.isBlank()) {
                    call.fail("vaccine_name is required")
                    return@post
                }

                val studentId = runCatching { UUID.fromString(req.studentId) }.getOrNull()
                    ?: run { call.fail("Invalid student_id"); return@post }

                val belongs = dbQuery { verifyStudentInSchool(ctx.schoolId, studentId) }
                if (!belongs) {
                    call.fail("Student not found in your school", HttpStatusCode.NotFound, "STUDENT_NOT_FOUND")
                    return@post
                }

                val administered = parseDate(req.dateAdministered)
                    ?: run { call.fail("Invalid date_administered (expected YYYY-MM-DD)"); return@post }
                val nextDue = req.nextDueDate?.let { parseDate(it) }

                val dto = dbQuery {
                    val newId = StudentImmunizationsTable.insert {
                        it[StudentImmunizationsTable.studentId] = studentId
                        it[vaccineName] = req.vaccineName.trim()
                        it[doseNumber] = req.doseNumber
                        it[dateAdministered] = administered
                        it[nextDueDate] = nextDue
                        it[administeredBy] = req.administeredBy?.trim()
                        it[createdAt] = Instant.now()
                    } get StudentImmunizationsTable.id
                    StudentImmunizationsTable.selectAll()
                        .where { StudentImmunizationsTable.id eq newId }
                        .first().toImmunizationDto()
                }
                call.created(dto, message = "Immunization record added")
            }

            // ---- incidents: list with optional filters ----
            get("/incidents") {
                val ctx = call.requireSchoolContext() ?: return@get
                val studentIdParam = call.request.queryParameters["student_id"]
                    ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                val dateFrom = call.request.queryParameters["date_from"]?.let { parseDate(it) }
                val dateTo = call.request.queryParameters["date_to"]?.let { parseDate(it) }

                val rows = dbQuery {
                    StudentHealthIncidentsTable.selectAll()
                        .where { StudentHealthIncidentsTable.schoolId eq ctx.schoolId }
                        .orderBy(StudentHealthIncidentsTable.date to SortOrder.DESC)
                        .map { it.toIncidentDto() }
                }.filter { dto ->
                    (studentIdParam == null || dto.studentId == studentIdParam.toString()) &&
                        run {
                            val d = parseDate(dto.date) ?: return@run true
                            (dateFrom == null || !d.isBefore(dateFrom)) &&
                                (dateTo == null || !d.isAfter(dateTo))
                        }
                }
                call.ok(HealthIncidentListResponse(rows), message = "Health incidents fetched")
            }

            // ---- incidents: log a new incident ----
            post("/incidents") {
                val ctx = call.requireSchoolContext() ?: return@post
                val req = runCatching { call.receive<LogIncidentRequest>() }.getOrNull()
                    ?: run { call.fail("Invalid body"); return@post }

                if (req.description.isBlank()) {
                    call.fail("description is required")
                    return@post
                }
                if (req.severity !in VALID_SEVERITIES) {
                    call.fail("severity must be one of: minor, moderate, major")
                    return@post
                }

                val studentId = runCatching { UUID.fromString(req.studentId) }.getOrNull()
                    ?: run { call.fail("Invalid student_id"); return@post }

                val belongs = dbQuery { verifyStudentInSchool(ctx.schoolId, studentId) }
                if (!belongs) {
                    call.fail("Student not found in your school", HttpStatusCode.NotFound, "STUDENT_NOT_FOUND")
                    return@post
                }

                val incidentDate = parseDate(req.date)
                    ?: run { call.fail("Invalid date (expected YYYY-MM-DD)"); return@post }

                val dto = dbQuery {
                    val newId = StudentHealthIncidentsTable.insert {
                        it[StudentHealthIncidentsTable.schoolId] = ctx.schoolId
                        it[StudentHealthIncidentsTable.studentId] = studentId
                        it[date] = incidentDate
                        it[time] = req.time?.trim()
                        it[description] = req.description.trim()
                        it[treatment] = req.treatment?.trim()
                        it[medicationGiven] = req.medicationGiven?.trim()
                        it[parentNotified] = false
                        it[attendedBy] = ctx.userId
                        it[attendedByName] = null
                        it[severity] = req.severity
                        it[createdAt] = Instant.now()
                    } get StudentHealthIncidentsTable.id
                    StudentHealthIncidentsTable.selectAll()
                        .where { StudentHealthIncidentsTable.id eq newId }
                        .first().toIncidentDto()
                }
                call.created(dto, message = "Health incident logged")
            }

            // ---- incidents: mark parent notified ----
            patch("/incidents/{id}/notify") {
                val ctx = call.requireSchoolContext() ?: return@patch
                val incidentId = call.parameters["id"]
                    ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid incident id"); return@patch }

                val now = Instant.now()
                val dto = dbQuery {
                    val existing = StudentHealthIncidentsTable.selectAll()
                        .where {
                            (StudentHealthIncidentsTable.id eq incidentId) and
                                (StudentHealthIncidentsTable.schoolId eq ctx.schoolId)
                        }
                        .firstOrNull() ?: return@dbQuery null

                    StudentHealthIncidentsTable.update({
                        (StudentHealthIncidentsTable.id eq incidentId) and
                            (StudentHealthIncidentsTable.schoolId eq ctx.schoolId)
                    }) {
                        it[parentNotified] = true
                        it[parentNotifiedAt] = now
                    }
                    StudentHealthIncidentsTable.selectAll()
                        .where { StudentHealthIncidentsTable.id eq incidentId }
                        .first().toIncidentDto()
                }
                if (dto == null) {
                    call.fail("Health incident not found in your school", HttpStatusCode.NotFound, "INCIDENT_NOT_FOUND")
                    return@patch
                }
                call.ok(dto, message = "Parent notified")
            }
        }

        // ── Teacher: /api/v1/teacher/health/alerts ──────────────────
        route("/api/v1/teacher/health") {

            // ---- allergy + condition alerts for assigned classes ----
            get("/alerts") {
                val ctx = call.requireTeacherContext() ?: return@get
                val assignments = teacherAssignmentsFor(ctx)
                if (assignments.isEmpty()) {
                    call.ok(HealthAlertsResponse(emptyList()), message = "No class assignments")
                    return@get
                }

                // Collect distinct (classId, section) pairs from assignments
                val classSections = assignments
                    .filter { it.classId != null }
                    .map { it.classId!! to it.section }
                    .distinct()

                // Find student ids enrolled in those classes
                val studentIds = if (classSections.isNotEmpty()) dbQuery {
                    // Build OR chain: (classId=? AND section=?) OR (classId=? AND section=?) ...
                    val orCondition = classSections.drop(1).fold(
                        (EnrollmentsTable.classId eq classSections[0].first) and
                            (EnrollmentsTable.section eq classSections[0].second)
                    ) { acc, (cls, sec) ->
                        acc or ((EnrollmentsTable.classId eq cls) and (EnrollmentsTable.section eq sec))
                    }
                    EnrollmentsTable.selectAll()
                        .where {
                            (EnrollmentsTable.schoolId eq ctx.schoolId) and
                                (EnrollmentsTable.status eq "active") and
                                orCondition
                        }
                        .map { it[EnrollmentsTable.studentId] }
                        .distinct()
                } else emptyList()

                if (studentIds.isEmpty()) {
                    call.ok(HealthAlertsResponse(emptyList()), message = "No students in assigned classes")
                    return@get
                }

                // Fetch health profiles with non-empty allergies or chronic conditions
                val alerts = dbQuery {
                    StudentHealthProfilesTable.selectAll()
                        .where {
                            (StudentHealthProfilesTable.schoolId eq ctx.schoolId) and
                                (StudentHealthProfilesTable.studentId inList studentIds)
                        }
                        .filter { row ->
                            val allergies = row[StudentHealthProfilesTable.allergies]
                            val conditions = row[StudentHealthProfilesTable.chronicConditions]
                            allergies != "[]" && allergies.isNotBlank() ||
                                conditions != "[]" && conditions.isNotBlank()
                        }
                        .map { row ->
                            val studentId = row[StudentHealthProfilesTable.studentId]
                            val student = StudentsTable.selectAll()
                                .where { StudentsTable.id eq studentId }
                                .singleOrNull()
                            HealthAlertDto(
                                studentId = studentId.toString(),
                                studentName = student?.get(StudentsTable.fullName) ?: "Unknown",
                                className = student?.get(StudentsTable.className) ?: "",
                                section = student?.get(StudentsTable.section) ?: "",
                                allergies = row[StudentHealthProfilesTable.allergies],
                                chronicConditions = row[StudentHealthProfilesTable.chronicConditions],
                            )
                        }
                }
                call.ok(HealthAlertsResponse(alerts), message = "Health alerts fetched")
            }
        }

        // ── Parent: /api/v1/parent/health/{childId} ─────────────────
        route("/api/v1/parent/health") {

            // ---- full health record for a child ----
            get("/{childId}") {
                val uid = call.principalUserId()
                    ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid token", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@get }
                val childId = call.parameters["childId"]
                    ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("A valid child id is required", HttpStatusCode.BadRequest, "BAD_CHILD_ID"); return@get }

                // Verify child ownership
                val child = dbQuery {
                    ChildrenTable.selectAll()
                        .where {
                            (ChildrenTable.id eq childId) and
                                (ChildrenTable.parentId eq uid) and
                                (ChildrenTable.isActive eq true)
                        }
                        .singleOrNull()
                } ?: run {
                    call.fail("Child not found", HttpStatusCode.NotFound, "CHILD_NOT_FOUND")
                    return@get
                }

                val childName = child[ChildrenTable.childName]
                val studentCode = child[ChildrenTable.studentCode]

                // Resolve student UUID from student_code if linked
                val studentId = if (studentCode != null) dbQuery {
                    StudentsTable.selectAll()
                        .where { StudentsTable.studentCode eq studentCode }
                        .singleOrNull()?.get(StudentsTable.id)?.value
                } else null

                if (studentId == null) {
                    call.ok(
                        ParentHealthResponse(childName = childName),
                        message = "No health records linked yet",
                    )
                    return@get
                }

                val sid = studentId

                val profile = dbQuery {
                    StudentHealthProfilesTable.selectAll()
                        .where { StudentHealthProfilesTable.studentId eq sid }
                        .firstOrNull()?.toProfileDto()
                }

                val immunizations = dbQuery {
                    StudentImmunizationsTable.selectAll()
                        .where { StudentImmunizationsTable.studentId eq sid }
                        .orderBy(StudentImmunizationsTable.dateAdministered to SortOrder.DESC)
                        .map { it.toImmunizationDto() }
                }

                val incidents = dbQuery {
                    StudentHealthIncidentsTable.selectAll()
                        .where { StudentHealthIncidentsTable.studentId eq sid }
                        .orderBy(StudentHealthIncidentsTable.date to SortOrder.DESC)
                        .map { it.toIncidentDto() }
                }

                call.ok(
                    ParentHealthResponse(
                        childName = childName,
                        profile = profile,
                        immunizations = immunizations,
                        incidents = incidents,
                    ),
                    message = "Health records fetched",
                )
            }
        }
    }
}
