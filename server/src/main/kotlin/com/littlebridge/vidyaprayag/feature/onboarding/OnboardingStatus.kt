/*
 * File: OnboardingStatus.kt
 * Module: feature.onboarding
 *
 * Purpose:
 *   THE single source of truth for "how far along is this school's onboarding".
 *   Before this, three places computed completion three different ways:
 *     - UserDetailsRouting : real school fields + school_classes count + onboarded_at  (correct)
 *     - SchoolDashboard    : "a draft row exists for this step"                          (too loose)
 *     - Onboarding submit  : "is_final_submission && step==REVIEW"                       (only the end)
 *
 *   Every caller now derives status from the SAME persisted facts:
 *     Step 1 BASIC     -> schools row exists with a name + a contact email/phone
 *     Step 2 BRANDING  -> schools.logo_url present
 *     Step 3 ACADEMIC  -> >= 1 row in school_classes for this school
 *     Step 4 REVIEW    -> schools.onboarded_at IS NOT NULL
 *
 *   "Draft presence" is explicitly NOT used for completion — drafts are only for
 *   restoring half-typed form state.
 */
package com.littlebridge.vidyaprayag.feature.onboarding

import com.littlebridge.vidyaprayag.db.AppUsersTable
import com.littlebridge.vidyaprayag.db.DatabaseFactory.dbQuery
import com.littlebridge.vidyaprayag.db.SchoolClassesTable
import com.littlebridge.vidyaprayag.db.SchoolsTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import java.util.UUID

/** Per-step completion booleans derived strictly from persisted school data. */
data class OnboardingStatus(
    val schoolExists: Boolean,
    val basicsDone: Boolean,
    val brandingDone: Boolean,
    val academicDone: Boolean,
    val finalDone: Boolean
) {
    /** Number of completed steps (0..4). */
    val completedSteps: Int =
        listOf(basicsDone, brandingDone, academicDone, finalDone).count { it }

    val totalSteps: Int = 4

    val progress: Double = completedSteps.toDouble() / totalSteps.toDouble()

    /** Coarse overall status used by the home/dashboard surfaces. */
    val overallStatus: String = when {
        finalDone -> "COMPLETED"
        basicsDone || brandingDone || academicDone -> "IN_PROGRESS"
        else -> "NOT_STARTED"
    }

    /** True step-type given an index 1..4, used by the dashboard step list. */
    fun isStepDone(stepId: Int): Boolean = when (stepId) {
        1 -> basicsDone
        2 -> brandingDone
        3 -> academicDone
        4 -> finalDone
        else -> false
    }
}

/** Empty status for a user that hasn't created a school yet. */
private val EMPTY_STATUS = OnboardingStatus(
    schoolExists = false,
    basicsDone = false,
    brandingDone = false,
    academicDone = false,
    finalDone = false
)

/**
 * Computes [OnboardingStatus] for the school owned by [userId]. Runs in its own
 * dbQuery; pass an already-open transaction-free UUID.
 */
suspend fun computeOnboardingStatus(userId: UUID): OnboardingStatus = dbQuery {
    val schoolId = AppUsersTable.selectAll()
        .where { AppUsersTable.id eq userId }
        .singleOrNull()
        ?.get(AppUsersTable.schoolId)
        ?: return@dbQuery EMPTY_STATUS

    val school = SchoolsTable.selectAll()
        .where { SchoolsTable.id eq schoolId }
        .singleOrNull()
        ?: return@dbQuery EMPTY_STATUS

    val basicsDone = school[SchoolsTable.name].isNotBlank() &&
        (school[SchoolsTable.contactEmail] != null || school[SchoolsTable.contactPhone] != null)
    val brandingDone = school[SchoolsTable.logoUrl]?.isNotBlank() == true
    val academicDone = SchoolClassesTable.selectAll()
        .where { SchoolClassesTable.schoolId eq schoolId }
        .count() > 0L
    val finalDone = school[SchoolsTable.onboardedAt] != null

    OnboardingStatus(
        schoolExists = true,
        basicsDone = basicsDone,
        brandingDone = brandingDone,
        academicDone = academicDone,
        finalDone = finalDone
    )
}
