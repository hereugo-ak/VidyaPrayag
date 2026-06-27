package com.littlebridge.enrollplus

import com.littlebridge.enrollplus.core.ownsAssignment
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Regression guard for the T-003 (Teacher Portal Rebuild) id-FIRST ownership
 * rule that narrows B-AUTH-1.
 *
 * BEFORE: teacherAssignmentsFor / requireOwnedAssignment treated an assignment
 * as owned if EITHER teacher_id == caller OR teacher_name matched the caller's
 * display name. So Teacher A could reach Teacher B's id-bound assignment merely
 * by sharing a display name ("Priya Sharma") — a real authorization hole.
 *
 * AFTER (this rule): once an assignment carries a teacher_id FK (backfilled by
 * T-002), ownership is decided ONLY by that id; the name is never consulted.
 * The name match survives ONLY as a fallback for legacy rows whose teacher_id
 * is still null.
 *
 * These pin the pure decision function ownsAssignment(), the single source of
 * truth shared by every scoped teacher endpoint.
 */
class TeacherAccessTest {

    private val teacherA = UUID.fromString("00000000-0000-0000-0000-0000000000a1")
    private val teacherB = UUID.fromString("00000000-0000-0000-0000-0000000000b2")

    // ── id-bound assignment is owned ONLY by the matching id ─────────────────
    @Test
    fun idBoundAssignment_ownedByMatchingId() {
        assertTrue(
            ownsAssignment(
                callerUserId = teacherA,
                callerRole = "teacher",
                callerFullName = "Priya Sharma",
                rowTeacherId = teacherA,
                rowTeacherName = "Priya Sharma",
            ),
        )
    }

    // ── B-AUTH-1 fix: a NAME COLLISION must NOT grant access to an id-bound row
    @Test
    fun idBoundAssignment_nameCollisionDenied() {
        // Teacher A shares the display name of Teacher B's id-owned assignment.
        // Under the old OR-rule this returned true; it MUST now be false.
        assertFalse(
            ownsAssignment(
                callerUserId = teacherA,
                callerRole = "teacher",
                callerFullName = "Priya Sharma",
                rowTeacherId = teacherB,            // id-bound to a DIFFERENT user
                rowTeacherName = "Priya Sharma",    // … but the names collide
            ),
            "A display-name collision must not grant access to an id-bound assignment",
        )
    }

    // ── Legacy (teacher_id NULL) row still resolves by the name fallback ─────
    @Test
    fun legacyRow_resolvesByNameFallback() {
        assertTrue(
            ownsAssignment(
                callerUserId = teacherA,
                callerRole = "teacher",
                callerFullName = "Priya Sharma",
                rowTeacherId = null,                // unmigrated row
                rowTeacherName = "priya sharma",    // case-insensitive match
            ),
        )
        assertFalse(
            ownsAssignment(
                callerUserId = teacherA,
                callerRole = "teacher",
                callerFullName = "Priya Sharma",
                rowTeacherId = null,
                rowTeacherName = "Someone Else",
            ),
        )
    }

    // ── A legacy row with no id AND no name matches nobody (teacher role) ─────
    @Test
    fun legacyRow_noIdNoName_deniedForTeacher() {
        assertFalse(
            ownsAssignment(
                callerUserId = teacherA,
                callerRole = "teacher",
                callerFullName = "Priya Sharma",
                rowTeacherId = null,
                rowTeacherName = null,
            ),
        )
    }

    // ── school_admin / admin stand in for any teacher in their school ────────
    @Test
    fun privilegedRoles_ownEverything() {
        for (role in listOf("school_admin", "admin")) {
            assertTrue(
                ownsAssignment(
                    callerUserId = teacherA,
                    callerRole = role,
                    callerFullName = "Whoever",
                    rowTeacherId = teacherB,        // someone else's id
                    rowTeacherName = "Someone Else",
                ),
                "$role must be able to stand in for any teacher in their school",
            )
        }
    }
}
