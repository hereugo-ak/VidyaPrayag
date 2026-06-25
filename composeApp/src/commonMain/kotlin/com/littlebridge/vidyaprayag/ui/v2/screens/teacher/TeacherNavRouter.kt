package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import com.littlebridge.vidyaprayag.feature.teacher.presentation.ResolvedPeriodUi

/**
 * TeacherNavRouter — the Teacher Portal's cross-tab deep-link resolver
 * (Loop Phase 7 — Cross-Tab Connectivity).
 *
 * Pure functions (no Compose, no side effects) that translate a user gesture on one
 * tab into a typed [TeacherDestination] the host applies on another. Keeping them
 * pure means every deep link is unit-reasoned and exhaustively `when`-checked by the
 * compiler — "no isolated dead ends" (the Phase 7 goal) becomes a compile-time
 * guarantee rather than a manual audit.
 *
 * Each task adds the function it needs:
 *   • P7-T1  [nudgeDestination]        Home Smart-Nudge "Add Now" → its target tab
 *   • P7-T2  [periodDestination]       TodayStrip period → Attendance (when untaken)
 *   • P7-T3  [messageParentDestination] Gradebook student → parent ChatThread
 *   • P7-T4  [routeNotification]       NotificationSheet tap → typed destination
 *   • P7-T5  [classesTaughtDestination] / [totalStudentsDestination]  profile stats
 */
object TeacherNavRouter {

    /**
     * P7-T1 — resolve a Home Smart-Nudge's primary action to its destination.
     *
     * The headline case from the spec: a "Marks not entered" nudge's **"Add Now"**
     * jumps to the Gradebook **pre-selected** to that nudge's class + exam. The other
     * nudges route to their natural home so no nudge is a dead end:
     *   • MarksNotEntered   → Gradebook(classId, examId)   ("Add Now")
     *   • AttendanceNotTaken→ Attendance(periodId)         ("Take Now")
     *   • ParentUnread      → ChatThread(threadId/studentId) ("Reply")
     *   • HomeworkUngraded  → Gradebook(classId)           ("Grade")
     *
     * Ids are nullable on the nudge models (host populates them); when an id is
     * missing the destination still opens the right tab, just unfiltered.
     */
    fun nudgeDestination(nudge: TeacherNudge): TeacherDestination = when (nudge) {
        is TeacherNudge.MarksNotEntered ->
            TeacherDestination.Gradebook(classId = nudge.classId, examId = nudge.examId)
        is TeacherNudge.AttendanceNotTaken ->
            // periodId may be absent for a legacy nudge → fall back to an empty marker
            // the host treats as "open today's attendance picker".
            TeacherDestination.Attendance(periodId = nudge.periodId.orEmpty())
        is TeacherNudge.ParentUnread ->
            TeacherDestination.ChatThread(threadId = nudge.threadId, studentId = nudge.studentId)
        is TeacherNudge.HomeworkUngraded ->
            TeacherDestination.Gradebook(classId = nudge.classId)
    }

    /**
     * P7-T2 — resolve a TodayStrip period tap to an Attendance destination.
     *
     * The spec: tapping a period on the Home strip whose attendance is **not yet
     * taken** offers "Take Attendance" → the [TeacherDestination.Attendance] screen for
     * *that specific period* (`periodId` as the typed arg). When attendance is already
     * marked (or the period was cancelled, or it has no stable `periodId`) there is no
     * attendance action to take, so this returns `null` and the host leaves the period
     * inert — deliberately NOT a dead-end button.
     *
     * Returning a nullable destination (rather than always navigating) is what keeps
     * the "no isolated dead ends" goal honest: the affordance only exists when the
     * action is real.
     */
    fun periodDestination(period: ResolvedPeriodUi): TeacherDestination? {
        val id = period.periodId
        if (period.attendanceMarked || period.isCancelled || id.isNullOrBlank()) return null
        return TeacherDestination.Attendance(periodId = id)
    }
}
