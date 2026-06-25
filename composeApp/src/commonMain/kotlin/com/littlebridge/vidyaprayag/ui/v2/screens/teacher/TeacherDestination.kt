package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

/**
 * TeacherDestination — the single typed deep-link vocabulary for the Teacher Portal
 * (Loop Phase 7 — Cross-Tab Connectivity).
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * WHY A SEALED INTENT, NOT NavController ROUTE STRINGS
 * ─────────────────────────────────────────────────────────────────────────────
 * The loop spec (P7-T1…T4) phrases deep links as "navigate via `NavController` with
 * route arguments". But this app has NO Jetpack `NavController` — `NavGraphV2.kt`
 * drives navigation with a hoisted `route by remember { mutableStateOf(...) }` enum +
 * callback lambdas (e.g. `onParent = { route = ... }`). String routes with bundled
 * args would be a foreign pattern that breaks portal-wide consistency.
 *
 * So Phase 7's deep links are expressed the portal-native way: a single exhaustive
 * `sealed interface` of destinations carrying their typed arguments. Every cross-tab
 * affordance (nudge "Add Now", TodayStrip period, gradebook "Message Parent",
 * notification tap, profile stat) resolves to one of these, and the host's portal
 * shell does a single `when (destination)` to flip its tab/route state. This keeps
 * arguments type-safe (no string parsing), makes the wiring exhaustively checkable by
 * the compiler, and matches the existing callback architecture exactly.
 *
 * The standalone composables raise plain callbacks (`onPlanNow`, `onOpenClass`, …);
 * the host maps each to the matching factory here and applies it. The factories live
 * with the destinations so the intent vocabulary has one home.
 */
sealed interface TeacherDestination {

    /**
     * Gradebook tab, optionally pre-filtered to a class and/or exam.
     * Used by: P7-T1 (nudge "Add Now" — class+exam pre-selected), P6-T3 / P7-T5
     * (assignment row + "Classes Taught" stat → Gradebook).
     */
    data class Gradebook(
        val classId: String? = null,
        val examId: String? = null,
    ) : TeacherDestination

    /**
     * Attendance screen for a specific period.
     * Used by: P7-T2 (TodayStrip period tap when attendance is not yet taken).
     */
    data class Attendance(
        val periodId: String,
    ) : TeacherDestination

    /**
     * Chat tab → the parent thread for a student (or a thread id directly).
     * Used by: P7-T3 (gradebook "Message Parent"), P7-T4 (parent-message notification).
     * Exactly one of `threadId` / `studentId` is normally set; when only `studentId`
     * is known the host's ChatViewModel resolves it to the parent thread.
     */
    data class ChatThread(
        val threadId: String? = null,
        val studentId: String? = null,
    ) : TeacherDestination

    /**
     * A simple read-only notice/announcement detail view (system notices).
     * Used by: P7-T4 (system-notice notification). `noticeId` lets the host's
     * NoticeDetailScreen load the full body.
     */
    data class NoticeDetail(
        val noticeId: String,
        val title: String,
        val body: String,
    ) : TeacherDestination

    /**
     * The student-list screen (stub destination wired with correct navigation).
     * Used by: P7-T5 ("Total Students" profile stat).
     */
    data class StudentList(
        val classId: String? = null,
    ) : TeacherDestination
}
