/*
 * File: AlumniAccess.kt
 * Module: core
 *
 * Purpose:
 *   Authorization & scoping for alumni self-service endpoints. Mirrors the
 *   pattern established by SchoolAccess.kt but resolves the alumni record
 *   from JWT sub → alumni.user_id (NOT app_users.school_id, which is null
 *   for alumni — see spec §20.6 C8).
 *
 *   This module centralizes:
 *     - principal parsing (UUID from JWT sub)
 *     - alumni record resolution from alumni table
 *     - a guard that requires an authenticated, approved, active alumni
 *
 * Usage in a route handler:
 *   val ctx = call.requireAlumniContext() ?: return@get   // responds 401/403 itself
 *   // ctx.alumniId, ctx.schoolId, ctx.userId are now trusted
 */
package com.littlebridge.enrollplus.core

import com.littlebridge.enrollplus.db.AlumniTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import io.ktor.http.*
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import java.util.UUID

/** Resolved, trusted context for an alumni self-service request. */
data class AlumniContext(
    val alumniId: UUID,
    val schoolId: UUID,
    val userId: UUID
)

/**
 * The canonical guard for every alumni self-service endpoint. Responds with
 * the appropriate error envelope and returns null when the caller is not
 * authorized:
 *   401 – no/invalid token
 *   403 – authenticated but no alumni record linked to this user
 *   403 – alumni record exists but verification_status != 'approved'
 *   403 – alumni record is deactivated (is_active = false)
 *
 * On success returns a fully-trusted [AlumniContext].
 *
 * IMPORTANT: schoolId is resolved from AlumniTable.school_id, NOT from
 * app_users.school_id (which is null for alumni — see spec §20.6 C8).
 */
suspend fun ApplicationCall.requireAlumniContext(): AlumniContext? {
    val uid = principalUserUuid() ?: run {
        fail("Invalid token", HttpStatusCode.Unauthorized, "UNAUTHORIZED")
        return null
    }

    val alumniRow = dbQuery {
        AlumniTable.selectAll().where { AlumniTable.userId eq uid }.singleOrNull()
    } ?: run {
        fail("No alumni profile linked to this account", HttpStatusCode.Forbidden, "NO_ALUMNI_PROFILE")
        return null
    }

    if (!alumniRow[AlumniTable.isActive]) {
        fail("This alumni account has been deactivated", HttpStatusCode.Forbidden, "ACCOUNT_DEACTIVATED")
        return null
    }

    if (alumniRow[AlumniTable.verificationStatus] != "approved") {
        fail("Your alumni registration is pending verification", HttpStatusCode.Forbidden, "PENDING_VERIFICATION")
        return null
    }

    val alumniId = alumniRow[AlumniTable.id].value
    val schoolId = alumniRow[AlumniTable.schoolId]

    return AlumniContext(alumniId, schoolId, uid)
}
