/*
 * File: TeacherRoutingTasks.kt
 * Module: feature.teacher
 *
 * The teacher TASK surface — attendance, marks, syllabus and homework — originally
 * lived here, split out of TeacherRouting.kt for readability and mounted as child
 * routes of `/api/v1/teacher` by TeacherRouting.teacherRouting() via teacherTaskRoutes().
 *
 * As of T-406 every one of those four surfaces has been REBUILT on its own typed,
 * assignment-scoped plane (DELETE-don't-patch), and the legacy handlers that lived
 * here are GONE:
 *   attendance → TeacherAttendanceRouting.kt   (T-203/T-205)
 *   marks      → TeacherGradebookRouting.kt    (T-303/T-304/T-305)
 *   syllabus   → TeacherSyllabusRouting.kt     (T-402/T-403)
 *   homework   → TeacherHomeworkRouting.kt     (T-405/T-406)
 *
 * What remains here is the (now empty) teacherTaskRoutes() mount point plus the
 * migration ledger below, kept as the documented landing zone for the rebuild story.
 */
package com.littlebridge.enrollplus.feature.teacher

import io.ktor.server.routing.*

// ─────────────────────────────────────────────────────────────────────────────
// DTOs — mirror shared/.../teacher/domain/model/TeacherModels.kt field-for-field.
//
// T-205 (Doc 06 §3, Doc 11): the legacy packed-`grade` attendance DTOs
// (AttendanceEntryDto / TeacherAttendanceData / AttendanceMarkDto /
// SubmitAttendanceRequest) and the legacy `route("/attendance") { get/post }`
// handler that lived here are DELETED — not patched. The typed, assignment-scoped
// attendance plane (TeacherAttendanceRouting.kt, T-203) now OWNS `GET/POST
// /api/v1/teacher/attendance`. (DELETE-don't-patch law.)
// ─────────────────────────────────────────────────────────────────────────────

// T-305 (DELETE-don't-patch): the legacy marks DTOs (MarksEntryDto / TeacherMarksData /
// MarkScoreDto / SubmitMarksRequest) and assessment DTOs (TeacherAssessmentDto /
// TeacherAssessmentsData / CreateAssessmentRequest) that mirrored the deleted `/marks`
// and `/assessments` handlers are GONE. The typed gradebook plane in
// TeacherGradebookRouting.kt defines its own Gb* DTOs (the :server module mirrors shared
// field-for-field).

// T-403 (DELETE-don't-patch): the legacy flat syllabus DTOs (SyllabusUnitDto /
// TeacherSyllabusData / UpdateSyllabusRequest) and the `route("/syllabus")`
// GET/PATCH handler that mirrored the old class+subject contract are GONE. The
// typed, hierarchical, assignment-scoped syllabus plane in TeacherSyllabusRouting.kt
// (T-402/T-403) now OWNS GET/POST/PATCH /api/v1/teacher/syllabus(/units|/progress)
// and defines its own Syl* DTOs (the :server module mirrors shared field-for-field).

// T-406 (DELETE-don't-patch): the legacy homework DTOs (HomeworkDto / TeacherHomeworkData /
// CreateHomeworkRequest) and the `route("/homework") { get/post }` handler that mirrored the
// old free-text-class contract are GONE. The typed, assignment-scoped, lifecycle-aware HOMEWORK
// plane in TeacherHomeworkRouting.kt (T-405) now OWNS the canonical GET/POST
// /api/v1/teacher/homework (list + ASSIGN), GET …/homework/{id}/submissions (roster-joined
// board), POST …/homework/{id}/extend, PATCH …/homework/{id}/submissions/{studentId}, and
// POST …/homework/{id}/close — and defines its own Hw* DTOs (the :server module mirrors shared
// field-for-field). This converges the T-405 staging path /homework-v2 → /homework.

/** Empty as of T-406 — every teacher task surface (attendance / marks / syllabus / homework)
 *  has been rebuilt on its own typed, assignment-scoped plane and the legacy handlers deleted.
 *  Kept (with its call site in TeacherRouting.teacherRouting()) as the documented migration
 *  landing zone; the per-surface REMOVED notes below trace where each one went. */
fun Route.teacherTaskRoutes() {

    // ── Attendance ──────────────────────────────────────────────────────────
    // REMOVED (T-205): the legacy packed-`grade` GET/POST /attendance handler that
    // lived here is deleted. The typed, assignment-scoped plane in
    // TeacherAttendanceRouting.kt now owns GET/POST /api/v1/teacher/attendance.
    // ── Marks & Assessments ───────────────────────────────────────────────────
    // REMOVED (T-305): the legacy force-publishing `route("/marks")` (POST set
    // isPublished=true and notified parents on EVERY save — the B-MK-1 bug) and the
    // free-text `route("/assessments")` (list + create) handlers that lived here are
    // DELETED. The canonical, typed, lifecycle-aware GRADEBOOK plane in
    // TeacherGradebookRouting.kt now owns GET/POST /api/v1/teacher/assessments,
    // GET/PUT …/{id}/marks (SAVE never publishes), POST …/{id}/publish (the ONLY
    // notify path), POST …/{id}/unpublish, and GET …/assessments/history.

    // ── Syllabus ────────────────────────────────────────────────────────────
    // REMOVED (T-403): the legacy class+subject `route("/syllabus") { get/patch }`
    // handler that lived here is DELETED — not patched. It read the flat
    // syllabus_units table, matched class/section by string normalisation, and
    // toggled coverage by a free unit_id. The typed, hierarchical, assignment-scoped
    // plane in TeacherSyllabusRouting.kt now OWNS GET /api/v1/teacher/syllabus
    // (hierarchical load), POST …/syllabus/units (B-SYL-1), PATCH …/syllabus/units/{id},
    // and PATCH …/syllabus/progress (one-tap covered toggle). (DELETE-don't-patch law.)

    // ── Homework ──────────────────────────────────────────────────────────────
    // REMOVED (T-406): the legacy free-text-class `route("/homework") { get/post }` handler
    // that lived here is DELETED — not patched. It listed/created by a packed class string,
    // surfaced no submissions board, no extensions and no attachments, and backed the dead
    // Assign button (F-HW-1). The typed, assignment-scoped, lifecycle-aware plane in
    // TeacherHomeworkRouting.kt (T-405) now OWNS GET/POST /api/v1/teacher/homework and the
    // /{id}/submissions, /{id}/extend, /{id}/close child routes. (DELETE-don't-patch law;
    // converges the T-405 staging path /homework-v2 → /homework.)
}
