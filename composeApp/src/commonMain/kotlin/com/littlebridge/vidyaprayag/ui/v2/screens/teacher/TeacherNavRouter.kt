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

    /**
     * P7-T3 — resolve a Gradebook "Message Parent" tap to the parent chat thread.
     *
     * From the expanded `StudentMarkDetailSheet`, "Message Parent" deep-links into the
     * Chat tab and opens the [TeacherDestination.ChatThread] for this student. We pass
     * the `studentId` (not a thread id): the host's `ChatViewModel` resolves the
     * student → their linked parent's thread (creating it if it doesn't exist yet), so
     * the Gradebook never needs to know thread identifiers. Returns `null` only when no
     * student id is available, so the button is never a dead end with a bad target.
     */
    fun messageParentDestination(studentId: String?): TeacherDestination? {
        if (studentId.isNullOrBlank()) return null
        return TeacherDestination.ChatThread(studentId = studentId)
    }

    /**
     * P7-T4 — the central notification router: a tap on any [TeacherNotification] row
     * in the NotificationSheet resolves to the destination for its type.
     *
     * Spec mapping:
     *   • Attendance → [TeacherDestination.Attendance] for the alert's period
     *   • Grade      → [TeacherDestination.Gradebook] filtered to the class (mark update)
     *   • Message    → [TeacherDestination.ChatThread] for the parent (thread or student)
     *   • Announcement / Homework / General (system notices) → [TeacherDestination.NoticeDetail]
     *     a simple read-only text view, carrying the notification's own title + message
     *     as the body so the detail screen never shows an empty shell.
     *
     * This is exhaustive over [NotificationType] (compiler-checked), so a new type can
     * never silently fall through to a dead end. When a typed notification is missing
     * its id payload we degrade to the safe tab-level destination (e.g. an Attendance
     * alert with no `periodId` still opens Attendance) rather than dropping the tap.
     */
    fun routeNotification(notification: TeacherNotification): TeacherDestination = when (notification.type) {
        NotificationType.Attendance ->
            TeacherDestination.Attendance(periodId = notification.periodId.orEmpty())
        NotificationType.Grade ->
            TeacherDestination.Gradebook(classId = notification.classId)
        NotificationType.Message ->
            TeacherDestination.ChatThread(
                threadId = notification.parentThreadId,
                studentId = notification.studentId,
            )
        NotificationType.Announcement,
        NotificationType.Homework,
        NotificationType.General ->
            TeacherDestination.NoticeDetail(
                noticeId = notification.id,
                title = notification.title,
                body = notification.message,
            )
    }

    /**
     * P7-T5 — the "Classes Taught" profile stat → the Gradebook tab (all classes).
     * No filter argument: the stat is the teacher's whole teaching load, so it opens
     * the Gradebook at its class picker rather than pre-selecting one class.
     */
    fun classesTaughtDestination(): TeacherDestination = TeacherDestination.Gradebook()

    /**
     * P7-T5 — the "Total Students" profile stat → the student-list screen
     * ([TeacherStudentListScreen]). No `classId` so it lists every student the teacher
     * teaches across classes; the host can later pass a class filter without changing
     * this contract.
     */
    fun totalStudentsDestination(): TeacherDestination = TeacherDestination.StudentList()
}
