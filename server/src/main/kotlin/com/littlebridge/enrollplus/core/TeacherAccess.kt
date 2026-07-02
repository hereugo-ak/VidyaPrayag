/*
 * File: TeacherAccess.kt
 * Module: core
 *
 * Purpose:
 *   Single source of truth for TEACHER-side authorization & scoping, mirroring
 *   SchoolAccess.kt. Master rebuild doc gap G1 mandates: "every teacher endpoint
 *   filters by teacher_id ∈ JWT and the requested class_id must belong to that
 *   teacher's assignments; reject otherwise with 403".
 *
 *   This module centralizes:
 *     - principal parsing (UUID) reusing core/SecurityModule
 *     - school + role resolution straight from app_users (not the JWT claim, so a
 *       role change takes effect on the next request and a forged claim can't
 *       widen access)
 *     - a guard that requires an authenticated TEACHER with a school
 *     - an assignment-scope check so a teacher can only read/write the classes
 *       they are actually assigned to (teacher_subject_assignments)
 *
 * The `assignmentId` exchanged with the client (TeacherClassSummaryDto.assignmentId) is the
 * teacher_subject_assignments row id. Every scoped endpoint resolves that id
 * back to its assignment, asserts it belongs to the caller's school AND is
 * assigned to the caller (by teacher_id OR teacher_name fallback), and only then
 * trusts its className / section / subject.
 *
 * Usage in a route handler:
 *   val ctx = call.requireTeacherContext() ?: return@get          // 401/403/404 itself
 *   val asg = call.requireOwnedAssignment(ctx, classId) ?: return@get  // 400/403/404 itself
 *   // asg.className, asg.section, asg.subject are now trusted
 *   val roster = enrollmentsFor(asg)                              // typed enrollment roster
 *
 * T-003 (Teacher Portal Rebuild): ownership is now id-FIRST. After T-002
 * backfilled teacher_subject_assignments.teacher_id, an assignment that HAS a
 * teacher_id is owned ONLY by that exact user — the free-text teacher_name
 * match is a fallback used ONLY when teacher_id is null (legacy/unmigrated
 * rows). This narrows B-AUTH-1: a teacher can no longer reach another
 * teacher's id-owned assignment just because their display names collide.
 */
package com.littlebridge.enrollplus.core

import com.littlebridge.enrollplus.db.AppUsersTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.EnrollmentsTable
import com.littlebridge.enrollplus.db.StudentsTable
import com.littlebridge.enrollplus.db.TeacherSubjectAssignmentsTable
import io.ktor.http.*
import io.ktor.server.application.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import java.util.UUID

/** Roles permitted to access the teacher surface. */
val TEACHER_ROLES = setOf("teacher", "school_admin", "admin")

/** Resolved, trusted context for a teacher-side request. */
data class TeacherContext(
    val userId: UUID,
    val schoolId: UUID,
    val role: String,
    val fullName: String,
)

/**
 * A teacher's class assignment, resolved + ownership-checked. The className /
 * section / subject here are trusted (came from a row already proven to belong
 * to the caller), so handlers can safely scope further queries by them.
 */
data class OwnedAssignment(
    val assignmentId: UUID,
    val schoolId: UUID,
    // T-003: the typed scope. classId / subjectId are FK-backed (T-002) and are
    // the AUTHORITATIVE identity of the class+subject; className / section /
    // subject are the display strings carried for convenience. Later phases
    // (attendance, marks, roster) query by classId, never by the free-text name.
    val classId: UUID?,
    val subjectId: UUID?,
    val className: String,
    val section: String,
    val subject: String,
    val teacherId: UUID?,
    val teacherName: String?,
    val isClassTeacher: Boolean,
)

/** A single enrolled student in an owned class (typed enrollment roster row). */
data class EnrolledStudent(
    val studentId: UUID,
    val studentCode: String,
    val fullName: String,
    val rollNumber: Int?,
    val section: String,
    val enrollmentId: UUID,
)

/**
 * The canonical guard for every teacher endpoint. Responds with the appropriate
 * error envelope and returns null when the caller is not authorized:
 *   401 – no/invalid token
 *   403 – authenticated but not a teacher role
 *   404 – teacher role but no school yet
 *
 * On success returns a fully-trusted [TeacherContext].
 */
suspend fun ApplicationCall.requireTeacherContext(): TeacherContext? {
    val uid = principalUserUuid() ?: run {
        fail("Invalid token", HttpStatusCode.Unauthorized, "UNAUTHORIZED")
        return null
    }
    val row = dbQuery {
        AppUsersTable.selectAll().where { AppUsersTable.id eq uid }.singleOrNull()
    } ?: run {
        fail("User not found", HttpStatusCode.Unauthorized, "UNAUTHORIZED")
        return null
    }
    // RA-34: a deactivated account must pass no guard.
    if (!row[AppUsersTable.isActive]) {
        fail("This account has been deactivated. Contact your administrator.", HttpStatusCode.Forbidden, "ACCOUNT_DEACTIVATED")
        return null
    }
    val role = row[AppUsersTable.role]
    if (role !in TEACHER_ROLES) {
        fail("You do not have access to teacher resources", HttpStatusCode.Forbidden, "FORBIDDEN")
        return null
    }
    val schoolId = row[AppUsersTable.schoolId] ?: run {
        fail("Your account is not linked to a school yet", HttpStatusCode.NotFound, "NO_SCHOOL")
        return null
    }
    return TeacherContext(
        userId = uid,
        schoolId = schoolId,
        role = role,
        fullName = row[AppUsersTable.fullName],
    )
}

/**
 * Pure, DB-free ownership decision — the heart of the T-003 id-FIRST rule
 * (narrows B-AUTH-1). Extracted so it can be unit-tested without a database
 * (see TeacherAccessTest):
 *   • school_admin / admin own every assignment in their school.
 *   • If the assignment has a teacher_id FK → owned ONLY when it equals the
 *     caller's userId. The name is NOT consulted, so a display-name collision
 *     can no longer grant access to an assignment id-bound to a different user.
 *   • If teacher_id is null (legacy/unmigrated row) → fall back to a
 *     case-insensitive teacher_name match.
 */
fun ownsAssignment(
    callerUserId: UUID,
    callerRole: String,
    callerFullName: String,
    rowTeacherId: UUID?,
    rowTeacherName: String?,
): Boolean {
    if (callerRole == "school_admin" || callerRole == "admin") return true
    return if (rowTeacherId != null) {
        rowTeacherId == callerUserId
    } else {
        rowTeacherName?.equals(callerFullName, ignoreCase = true) == true
    }
}

/** Row-level ownership check delegating to the pure [ownsAssignment]. */
private fun TeacherContext.owns(r: org.jetbrains.exposed.sql.ResultRow): Boolean =
    ownsAssignment(
        callerUserId = userId,
        callerRole = role,
        callerFullName = fullName,
        rowTeacherId = r[TeacherSubjectAssignmentsTable.teacherId],
        rowTeacherName = r[TeacherSubjectAssignmentsTable.teacherName],
    )

/**
 * All assignment rows owned by [ctx] (same school AND owned per the id-first
 * [owns] rule). school_admin / admin see every active assignment in their
 * school so they can stand in for a teacher.
 */
suspend fun teacherAssignmentsFor(ctx: TeacherContext): List<OwnedAssignment> = dbQuery {
    TeacherSubjectAssignmentsTable.selectAll().where {
        (TeacherSubjectAssignmentsTable.schoolId eq ctx.schoolId) and
            (TeacherSubjectAssignmentsTable.isActive eq true)
    }.filter { ctx.owns(it) }
        .map { it.toOwnedAssignment() }
}

/**
 * Resolve a client-supplied class_id (assignment id) to a trusted
 * [OwnedAssignment], asserting ownership. Responds + returns null on:
 *   400 – missing/malformed id
 *   404 – not found in caller's school
 *   403 – exists but not assigned to caller
 */
suspend fun ApplicationCall.requireOwnedAssignment(
    ctx: TeacherContext,
    classId: String?,
): OwnedAssignment? {
    val id = classId?.let { runCatching { UUID.fromString(it) }.getOrNull() } ?: run {
        fail("A valid class_id is required", HttpStatusCode.BadRequest, "BAD_CLASS_ID")
        return null
    }
    val row = dbQuery {
        TeacherSubjectAssignmentsTable.selectAll().where {
            (TeacherSubjectAssignmentsTable.id eq id) and
                (TeacherSubjectAssignmentsTable.schoolId eq ctx.schoolId) and
                (TeacherSubjectAssignmentsTable.isActive eq true)
        }.singleOrNull()
    } ?: run {
        fail("Class not found in your school", HttpStatusCode.NotFound, "CLASS_NOT_FOUND")
        return null
    }

    if (!ctx.owns(row)) {
        fail("You are not assigned to this class", HttpStatusCode.Forbidden, "NOT_ASSIGNED")
        return null
    }
    return row.toOwnedAssignment()
}

/**
 * Typed enrollment roster for an owned assignment (T-003). Returns the active
 * students enrolled in the assignment's class_id + section, via the
 * `enrollments` table (T-001) — NOT the ClassNaming free-text heuristic and NOT
 * the packed attendance grade string. Used by every later phase (attendance,
 * marks, classes roster).
 *
 * Scope: enrollments.class_id == assignment.classId AND enrollments.section ==
 * assignment.section AND status == 'active', joined to students for identity.
 * When classId is null (unmigrated TSA row), resolves it from SchoolClassesTable
 * by class name, then falls back to StudentsTable + ClassNaming if no enrollments.
 */
suspend fun enrollmentsFor(assignment: OwnedAssignment): List<EnrolledStudent> {
    val resolvedClassId = assignment.classId ?: resolveClassIdByNameInTxn(assignment) ?: return fallbackRosterByClassNamingInTxn(assignment)
    return dbQuery {
        // Two-step lookup (the established single-table pattern in this codebase —
        // there are no Exposed table-joins elsewhere, and EnrollmentsTable.studentId
        // is a plain uuid column with no .references(), so an inferred join isn't
        // available). 1) active enrollments for class+section; 2) batch-load the
        // students for identity, then stitch in memory.
        val enrollments = EnrollmentsTable.selectAll().where {
            (EnrollmentsTable.classId eq resolvedClassId) and
                (EnrollmentsTable.section eq assignment.section) and
                (EnrollmentsTable.status eq "active")
        }.toList()
        if (enrollments.isEmpty()) return@dbQuery fallbackRosterByClassNamingInTxn(assignment)

        val studentIds = enrollments.map { it[EnrollmentsTable.studentId] }.distinct()
        val studentsById = StudentsTable.selectAll().where {
            // Exposed 0.50: the id column is Column<EntityID<UUID>>, so inList needs
            // EntityID values, not raw UUIDs.
            StudentsTable.id inList studentIds.map { EntityID(it, StudentsTable) }
        }.associateBy { it[StudentsTable.id].value }

        enrollments.mapNotNull { e ->
            val sid = e[EnrollmentsTable.studentId]
            val s = studentsById[sid] ?: return@mapNotNull null  // orphaned enrollment → skip
            EnrolledStudent(
                studentId = sid,
                studentCode = s[StudentsTable.studentCode],
                fullName = s[StudentsTable.fullName],
                rollNumber = e[EnrollmentsTable.rollNumber],
                section = e[EnrollmentsTable.section],
                enrollmentId = e[EnrollmentsTable.id].value,
            )
        }.sortedWith(compareBy({ it.rollNumber ?: Int.MAX_VALUE }, { it.fullName }))
    }
}

/** Look up classId from SchoolClassesTable by school + class name (case-insensitive). */
private suspend fun resolveClassIdByNameInTxn(a: OwnedAssignment): java.util.UUID? = dbQuery {
    com.littlebridge.enrollplus.db.SchoolClassesTable.selectAll().where {
        com.littlebridge.enrollplus.db.SchoolClassesTable.schoolId eq a.schoolId
    }.firstOrNull {
        ClassNaming.classKey(it[com.littlebridge.enrollplus.db.SchoolClassesTable.name]) ==
            ClassNaming.classKey(a.className)
    }?.get(com.littlebridge.enrollplus.db.SchoolClassesTable.id)?.value
}

/** Fallback: match students by ClassNaming on className + section (no enrollments needed). */
private suspend fun fallbackRosterByClassNamingInTxn(a: OwnedAssignment): List<EnrolledStudent> = dbQuery {
    StudentsTable.selectAll().where {
        (StudentsTable.schoolId eq a.schoolId) and (StudentsTable.isActive eq true)
    }.filter {
        ClassNaming.sameClassSection(
            it[StudentsTable.className], it[StudentsTable.section], a.className, a.section
        )
    }.map { s ->
        EnrolledStudent(
            studentId = s[StudentsTable.id].value,
            studentCode = s[StudentsTable.studentCode],
            fullName = s[StudentsTable.fullName],
            rollNumber = s[StudentsTable.rollNumber]?.toIntOrNull(),
            section = a.section,
            enrollmentId = s[StudentsTable.id].value,
        )
    }.sortedWith(compareBy({ it.rollNumber ?: Int.MAX_VALUE }, { it.fullName }))
}

private fun org.jetbrains.exposed.sql.ResultRow.toOwnedAssignment() = OwnedAssignment(
    assignmentId = this[TeacherSubjectAssignmentsTable.id].value,
    schoolId = this[TeacherSubjectAssignmentsTable.schoolId],
    classId = this[TeacherSubjectAssignmentsTable.classId],
    subjectId = this[TeacherSubjectAssignmentsTable.subjectId],
    className = this[TeacherSubjectAssignmentsTable.className],
    section = this[TeacherSubjectAssignmentsTable.section],
    subject = this[TeacherSubjectAssignmentsTable.subject],
    teacherId = this[TeacherSubjectAssignmentsTable.teacherId],
    teacherName = this[TeacherSubjectAssignmentsTable.teacherName],
    isClassTeacher = this[TeacherSubjectAssignmentsTable.isClassTeacher],
)
