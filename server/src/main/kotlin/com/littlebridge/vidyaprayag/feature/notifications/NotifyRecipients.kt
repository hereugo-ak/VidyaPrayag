/*
 * File: NotifyRecipients.kt
 * Module: feature.notifications
 *
 * RA-41: recipient resolvers for the notification spine. Triggers (attendance,
 * marks, homework, announcements, …) know WHAT happened in WHICH school but need
 * to turn that into the set of parent / teacher app_users.id to notify. These
 * helpers do that, ALWAYS scoped to a single school_id (multi-tenant isolation).
 *
 * Parent linkage: children.parent_id (app_users.id) joined by either
 *   - children.student_code  (exact student → that student's parents), or
 *   - children.current_grade (class → all parents whose child is in that class).
 */
package com.littlebridge.vidyaprayag.feature.notifications

import com.littlebridge.vidyaprayag.db.AppUsersTable
import com.littlebridge.vidyaprayag.db.ChildrenTable
import com.littlebridge.vidyaprayag.db.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import java.util.UUID

object NotifyRecipients {

    /** Parent app_users.id for a specific student_code within [schoolId]. */
    suspend fun parentsOfStudent(schoolId: UUID, studentCode: String): List<UUID> = dbQuery {
        ChildrenTable.selectAll().where {
            (ChildrenTable.schoolId eq schoolId) and
                (ChildrenTable.studentCode eq studentCode) and
                (ChildrenTable.isActive eq true)
        }.map { it[ChildrenTable.parentId] }.distinct()
    }

    /**
     * Parent app_users.id for every child in [className] within [schoolId].
     * Used for class-wide events (homework, class announcement). children only
     * carries current_grade (no section), so this targets the whole grade.
     */
    suspend fun parentsOfClass(schoolId: UUID, className: String): List<UUID> = dbQuery {
        ChildrenTable.selectAll().where {
            (ChildrenTable.schoolId eq schoolId) and
                (ChildrenTable.currentGrade eq className) and
                (ChildrenTable.isActive eq true)
        }.map { it[ChildrenTable.parentId] }.distinct()
    }

    /** Every active parent app_users row with a child enrolled in [schoolId]. */
    suspend fun parentsInSchool(schoolId: UUID): List<UUID> = dbQuery {
        ChildrenTable.selectAll().where {
            (ChildrenTable.schoolId eq schoolId) and (ChildrenTable.isActive eq true)
        }.map { it[ChildrenTable.parentId] }.distinct()
    }

    /** Every active teacher app_users.id in [schoolId]. */
    suspend fun teachersInSchool(schoolId: UUID): List<UUID> = dbQuery {
        AppUsersTable.selectAll().where {
            (AppUsersTable.schoolId eq schoolId) and
                (AppUsersTable.role eq "teacher") and
                (AppUsersTable.isActive eq true)
        }.map { it[AppUsersTable.id].value }.distinct()
    }

    /** Every active school-admin app_users.id in [schoolId] (school_admin | admin). */
    suspend fun adminsInSchool(schoolId: UUID): List<UUID> = dbQuery {
        AppUsersTable.selectAll().where {
            (AppUsersTable.schoolId eq schoolId) and
                ((AppUsersTable.role eq "school_admin") or (AppUsersTable.role eq "admin")) and
                (AppUsersTable.isActive eq true)
        }.map { it[AppUsersTable.id].value }.distinct()
    }
}
