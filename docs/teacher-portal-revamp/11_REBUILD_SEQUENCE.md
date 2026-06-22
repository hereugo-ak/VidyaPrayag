# 11 — REBUILD SEQUENCE (atomic, dependency-ordered)

> **Scope:** The complete, sequenced implementation plan for the teacher-portal rebuild. Every task maps to **one atomic commit** and specifies — with no ambiguity — its preceding schema migration, backend endpoint, and frontend composable/composable. Dependency-ordered so each commit builds on a green state.
> **Source:** Docs 01–10 (all defect IDs and target specs referenced here). Schema source of truth: `db/Tables.kt`; scoping: `core/TeacherAccess.kt`; routes: `feature/teacher/*`; UI: `ui/v2/screens/teacher/*`; tokens: `ui/v2/theme/*`.
> **Law:** Schema → backend → frontend, always. No frontend task may depend on a migration/endpoint that is not already committed earlier in the sequence. `AUTO_CREATE_TABLES` is OFF in production — **every migration is a hand-run Supabase SQL file**, committed as `docs/backend/sql/migration_0NN_*.sql`, and applied before its dependent backend commit is deployed.
> **Branch:** `backend-by-abuzar_v1.0.3`
> **Mode reminder:** This document is the *plan*. No code is written in this session.

---

## 0. How to Read This Document

Each task is an **atomic commit** with:
- **ID** `T-NNN`
- **Commit message** (conventional-commit form to use verbatim)
- **Layer** (Migration / Backend / Frontend / Seed / Shared-DTO)
- **Depends on** (prior task IDs — strict)
- **Touches** (files)
- **Done when** (acceptance, including the manual Supabase apply step for migrations)
- **Closes** (defect IDs from Docs 01–03)

**Phasing:** P0 Foundation (schema+scope) → P1 Schedule/Today → P2 Attendance → P3 Gradebook → P4 Planner → P5 Classes → P6 Profile/Polish. Within a phase, migrations precede their backend, which precedes their frontend.

> **Golden rule of ordering:** a migration is committed *and applied to Supabase* before any backend commit that reads its new columns; a backend commit is merged before any frontend commit that calls its endpoint.

---

## PHASE 0 — FOUNDATION (typed identity + scope)

These resolve the cross-cutting root defects (X-1..X-6) that everything else stands on. Nothing user-visible ships until P1, but **none of the later phases are safe without these**.

### T-001 — Migration: enrollments table (typed class membership)
- **Layer:** Migration · **Depends:** —
- **Touches:** `docs/backend/sql/migration_010_enrollments.sql`; `db/Tables.kt` (add `EnrollmentsTable` Exposed mapping, no auto-create)
- **Details:** create `enrollments` (Doc 09 §1): student_id FK, class_id FK, section, roll_number, status, start_date, end_date. Backfill from existing `students` rows (className/section → class_id via `school_classes` match; flag unmatched for manual review).
- **Done when:** SQL applied in Supabase; Exposed mapping compiles; backfill report shows matched/unmatched counts.
- **Closes:** X-1, X-2, D-STU-1..3 (foundation).

### T-002 — Migration: promote TSA to FK + class-teacher flag
- **Layer:** Migration · **Depends:** T-001
- **Touches:** `migration_011_tsa_fks.sql`; `db/Tables.kt` (`TeacherSubjectAssignmentsTable`)
- **Details:** add/enforce `class_id` FK, `subject_id` FK, `is_class_teacher boolean default false`; keep `className/section/subject` as display-only (no longer source of truth). Backfill FKs via `ClassNaming` one-time, persist resolved IDs.
- **Done when:** applied; FKs populated; `ClassNaming.sameClassSection` demoted to fallback only.
- **Closes:** X-1, B-CLS-3, D-TSA-1..4 (foundation).

### T-003 — Backend: harden TeacherAccess on typed scope
- **Layer:** Backend · **Depends:** T-002
- **Touches:** `core/TeacherAccess.kt`
- **Details:** `teacherAssignmentsFor` filters by `teacher_id` FK first, name fallback only if id null (narrows B-AUTH-1); `requireOwnedAssignment` returns `class_id/subject_id` typed; add `enrollmentsFor(assignmentId)` helper (typed roster query) used by all later phases.
- **Done when:** unit tests: owned assignment resolves by id; roster query returns enrollment-scoped students.
- **Closes:** B-AUTH-1 (partial), enables X-1 fixes downstream.

### T-004 — Migration: typed dates everywhere (attendance/assessment/homework/syllabus/calendar)
- **Layer:** Migration · **Depends:** —
- **Touches:** `docs/db/migration_010_typed_dates.sql` (filename deviation flagged below); `db/Tables.kt` (date columns); + all 14 date consumers (extended touch-list, see note).
- **Details:** convert `attendance_records.date`, `assessments.exam_date`, `homework.due_date`, `syllabus.covered_on`, `calendar_events.start/end_date` from `varchar12` → `date`. Migrate string values (validate format; quarantine bad rows).
- **Done when:** applied; all converted columns are `date`; bad-row report empty or triaged.
- **Closes:** X-3, D-ATT-3, D-ASMT-2, D-HW-2, D-SYL-2.
- **STATUS (executed):** ✅ Tables.kt flipped to `date()`; migration `docs/db/migration_010_typed_dates.sql` created (idempotent, quarantines bad rows into `vp_bad_dates_010`, aborts loudly on bad NOT-NULL values); all 14 consumer files converted at the type boundary (reads → `LocalDate.toString()` for String DTOs/helpers; writes → `LocalDate.parse(...)`; column comparisons → parsed `LocalDate` locals). Registered in `PROVISION.sql` run-order.
  - **Note 1 (filename — docs are authority, deviation flagged):** doc named the file `migration_012_typed_dates.sql`; the real chain in `docs/db/` is contiguous and the prior migration is `009`, so the file is `migration_010_typed_dates.sql` (same deviation pattern already documented on `009`, which the doc called `011`).
  - **Note 2 (extended touch-list — Rule 4):** the doc's "Touches" listed only the migration + `Tables.kt`, but flipping the Exposed column types makes every date consumer fail to compile. To honor Rule 4 (every commit compiles green), T-004 was extended to convert all 14 consumers in the same commit: `feature/calendar/AcademicCalendar{Core,Routing}.kt`, `feature/teacher/TeacherRouting{,Tasks}.kt`, `feature/parent/ParentAcademicsRouting.kt`, and `feature/school/{SchoolAnalytics,SchoolIntelligence,SchoolRecords,SchoolRouting,SchoolStudents,TeacherProvisioning,AdminDashboardOverview,AdminDashboard}Routing.kt` + `StudentAggregationService.kt`.
  - **Out of scope (left as `varchar`, intentionally):** `academic_years.start_date/end_date` and `fee_records.due_date` are NOT in the T-004 column list and remain string-typed.

### T-005 — Seed: realistic teacher dataset (the X-5 fix)
- **Layer:** Seed · **Depends:** T-001, T-002, T-004
- **Touches:** `Seed.kt` / `DemoSeed.kt`
- **Details:** seed for a demo teacher: TSA assignments (incl. 1 class-teacher), enrollments (e.g. 38 students/class), `teacher_periods` for a full week, `curriculum_units` per subject, a couple of `assessments`, sample `homework` + submissions, a few approved leaves. **Without this every screen lands empty.**
- **Done when:** fresh DB + demo login → Today shows periods, Classes shows rosters, Planner shows units.
- **Closes:** X-5, B-FEED-2 (data side), F-SYL-2/F-HOME-3 root cause.

---

## PHASE 1 — SCHEDULE & TODAY (the daily spine)

### T-101 — Migration: typed teacher_periods + assignment binding + exceptions
- **Layer:** Migration · **Depends:** T-002, T-004
- **Touches:** `migration_013_periods.sql`; `db/Tables.kt` (`TeacherPeriodsTable`, new `PeriodExceptionsTable`)
- **Details:** Doc 05 §2.1/2.2 — `start_time/end_time` → `time`; add `assignment_id` FK, `academic_year_id`, `valid_from/to`; create `period_exceptions`. Drop className/section/subject as truth (derive via assignment).
- **Done when:** applied; periods resolve class via assignment.
- **Closes:** D-TT-1..4.

### T-102 — Migration: single holiday source (deprecate HolidayListTable)
- **Layer:** Migration · **Depends:** T-004
- **Touches:** `migration_014_holidays_merge.sql`
- **Details:** migrate `holiday_list` rows into `calendar_events(type=HOLIDAY,status=PUBLISHED)`; deprecate `HolidayListTable`.
- **Done when:** applied; holidays read only from calendar_events.
- **Closes:** D-TT-5.

### T-103 — Shared DTO: ResolvedDay / ResolvedWeek models
- **Layer:** Shared-DTO · **Depends:** —
- **Touches:** `shared/.../feature/teacher/domain/model/TeacherModels.kt`
- **Details:** add `ResolvedDayDto`, `ResolvedPeriodDto`, `CalendarOverlayDto`, `CheckInStatusDto` (Doc 05 §4) with `@SerialName`.
- **Done when:** serialization round-trips in shared tests.

### T-104 — Backend: GET /teacher/day + /teacher/week (resolved schedule)
- **Layer:** Backend · **Depends:** T-101, T-102, T-103, T-003
- **Touches:** new `feature/teacher/TeacherDayRouting.kt`
- **Details:** Doc 05 §4 — merge periods + exceptions + holidays + calendar + per-period `attendanceMarked` (join attendance_records). Authoritative `nowIndex/nextIndex` from server clock.
- **Done when:** returns correct resolved day for seeded teacher incl. a holiday + a cancelled period; `attendanceMarked` accurate.
- **Closes:** B-FEED-1, B-FEED-2, B-HOME-4 (data side), B-HOME-1 (N+1 attendance loop replaced by joined query).

### T-105 — Frontend: Today tab shell + schedule card (3 faces)
- **Layer:** Frontend · **Depends:** T-104
- **Touches:** new `ui/v2/screens/teacher/TodayScreen.kt`, `TeacherScheduleCard.kt`; `TeacherPortalV2.kt` (introduce new tab structure Doc 04 §4)
- **Details:** greeting bar, Now/Next card with pre-scoped CTAs, 3-face swipe (Doc 05 §5, Doc 10 §6.2), VStateHost states.
- **Done when:** Today renders seeded schedule; empty/holiday/unseeded states distinct (Doc 10 §8).
- **Closes:** F-HOME-1..7 (structure), F-SHELL-1.

### T-106 — Migration+Backend+Frontend: teacher self check-in
- **Layer:** Migration→Backend→Frontend (3 commits T-106a/b/c) · **Depends:** T-105
- **Touches:** `migration_015_teacher_checkins.sql` (a); `TeacherDayRouting.kt` POST/GET checkin (b); `expect/actual BiometricAuthenticator` + check-in card (c)
- **Details:** Doc 06 §2 — `teacher_check_ins` table; idempotent endpoint; biometric ladder (biometric→PIN→manual) with always-available fallback.
- **Done when:** check-in flips pill; works with biometric and with manual fallback on a device without biometrics.
- **Closes:** B-ATT-5.

### T-107 — Frontend: real obligations strip on Today
- **Layer:** Frontend (+small backend aggregate) · **Depends:** T-104, (T-204, T-304 for full counts — strip ships incrementally)
- **Touches:** `TodayScreen.kt`; backend `GET /teacher/obligations`
- **Details:** Doc 04 §5.5 — real counts (unmarked classes today, unpublished results, submissions to review). Replaces fabricated tasks.
- **Done when:** counts match seeded reality; "all caught up" only when truly zero.
- **Closes:** B-HOME-4 (UI side), F-SHELL-4 (dock badge fed from this), F-HOME-3.

---

## PHASE 2 — ATTENDANCE

### T-201 — Migration: typed attendance (FK student, +leave, assignment binding)
- **Layer:** Migration · **Depends:** T-001, T-004
- **Touches:** `migration_016_attendance.sql`; `db/Tables.kt` (`AttendanceRecordsTable`)
- **Details:** Doc 06 §1.2 — `student_id`/`enrollment_id`/`faculty_id`/`assignment_id` FKs; add `leave` to status enum; add `source`, `marked_at`; drop packed `grade`. Migrate existing rows (parse grade → class/section → assignment best-effort; quarantine).
- **Done when:** applied; status enum includes leave; grade column gone.
- **Closes:** D-ATT-1, D-ATT-2, D-ATT-4.

### T-202 — Shared DTO: attendance load/save models (scoped)
- **Layer:** Shared-DTO · **Depends:** —
- **Touches:** `TeacherModels.kt` (`AttendanceLoadDto` with leaveDefaults, alreadyMarked; `AttendanceSaveDto`)

### T-203 — Backend: GET/POST /teacher/attendance (scoped, leave-default, upsert)
- **Layer:** Backend · **Depends:** T-201, T-202, T-003
- **Touches:** `feature/teacher/TeacherRoutingTasks.kt` (attendance handlers)
- **Details:** Doc 06 §3.8 — roster from enrollments; pre-mark approved-leave students (`source=leave_auto`); date default today, back-date window guard; upsert on (school,date,type,student,assignment); `alreadyMarked` + last-marked meta.
- **Done when:** load returns enrollment roster with leave defaults; save upserts; edge cases E1/E3/E9/E10 enforced.
- **Closes:** B-ATT-1..4, B-ATT-6/7.

### T-204 — Backend: leave approval writes attendance (B-LV-2)
- **Layer:** Backend · **Depends:** T-201, T-203
- **Touches:** `feature/teacher/TeacherLeaveRouting.kt`
- **Details:** Doc 06 §3.5 — approving a leave writes/queues `leave` marks for covered dates.
- **Done when:** approving a leave shows those students defaulted to leave on attendance load.
- **Closes:** B-LV-2.

### T-205 — Frontend: attendance screen rebuild (4 pills, enabled date, bulk, scoped)
- **Layer:** Frontend · **Depends:** T-203, T-105
- **Touches:** `ui/v2/screens/teacher/TeacherAttendanceScreenV2.kt`, `presentation/TeacherAttendanceViewModel.kt`
- **Details:** Doc 06 §3 + Doc 10 §6.3 — reached pre-scoped from Today/Classes (no shared picker); P/A/L/Lv pills ≥48dp; enabled date picker (fixes F-ATT-2); leave defaults; bulk "all present"; running counter; result-driven save; wrong-class guard header.
- **Done when:** mark 7B in <60s; back-date works within window; already-marked loads for edit; badge flips on Today.
- **Closes:** F-ATT-1..7.

---

## PHASE 3 — GRADEBOOK (assessment + marks)

### T-301 — Migration: canonical marks model (+type, pass, calendar tie, FK) & deprecate ExamResults
- **Layer:** Migration · **Depends:** T-001, T-002, T-004
- **Touches:** `migration_017_assessments.sql`; `db/Tables.kt`
- **Details:** Doc 07 §1.3 — assessments: assignment_id/class_id/subject_id FKs, type, pass_marks, calendar_event_id, status enum; assessment_marks: student_id FK, is_absent, remark; migrate `ExamResultsTable` → assessment_marks; deprecate ExamResults.
- **Done when:** applied; single marks model; ExamResults read-only/empty.
- **Closes:** X-6, D-ASMT-1..6.

### T-302 — Shared DTO: assessment + marks lifecycle models
- **Layer:** Shared-DTO · **Depends:** — · **Touches:** `TeacherModels.kt` (`AssessmentDto` with status, `MarksLoadDto`, `MarksSaveDto`, `AssessmentHistoryDto`)

### T-303 — Backend: assessment CRUD + marks SAVE (no publish) + explicit PUBLISH
- **Layer:** Backend · **Depends:** T-301, T-302, T-003
- **Touches:** `feature/teacher/TeacherRoutingTasks.kt` (marks/assessments) — **the B-MK-1 fix lives here**
- **Details:** Doc 07 §2 — `PUT marks` saves only (status marks_pending); `POST publish` is the *only* path that publishes + notifies; entry validation (≤max, ≥0) server-side; create scoped via requireOwnedAssignment.
- **Done when:** saving marks does NOT notify parents; publish is separate + confirmed; >max rejected.
- **Closes:** **B-MK-1**, B-MK-2..6.

### T-304 — Backend: GET /assessments/history (server aggregates)
- **Layer:** Backend · **Depends:** T-301 · **Touches:** new history handler
- **Details:** Doc 07 §6 — class average over time, distribution, compare two; computed server-side.
- **Closes:** B-MK-7.

### T-305 — Frontend: Gradebook tab (list, create, marks grid validated, publish)
- **Layer:** Frontend · **Depends:** T-303, T-105
- **Touches:** new `ui/v2/screens/teacher/GradebookScreen.kt`; rework `TeacherMarksScreenV2.kt`; remove `TeacherExamPicker` from shared selector
- **Details:** Doc 07 §3/§5 + Doc 10 §6.4 — scoped create; dense DM Mono grid; inline validation; sticky header counts; autosave draft; **Save vs Publish** distinct (publish confirm names parent count); large-class ergonomics.
- **Done when:** create→enter→save(draft)→publish flow; typo 105/100 blocked; reopening restores entries.
- **Closes:** F-MK-1..7, F-SHELL-3 (exam picker removed).

### T-306 — Frontend: assessment history & comparison
- **Layer:** Frontend · **Depends:** T-304, T-305 · **Touches:** `GradebookScreen.kt`
- **Details:** Doc 07 §6 — timeline, distribution, compare; empty state.

---

## PHASE 4 — PLANNER (syllabus + homework)

### T-401 — Migration: curriculum_units + syllabus_progress (template/progress split)
- **Layer:** Migration · **Depends:** T-002, T-004 · **Touches:** `migration_018_syllabus.sql`; `db/Tables.kt`
- **Details:** Doc 08 §1.2 — split template from per-section progress; hierarchy via parent_id; assignment-scoped progress; migrate existing `syllabus_units`.
- **Closes:** D-SYL-1..4.

### T-402 — Backend: syllabus CRUD + one-tap toggle (POST create fixes B-SYL-1)
- **Layer:** Backend · **Depends:** T-401, T-003 · **Touches:** `feature/teacher/TeacherRoutingTasks.kt` (syllabus)
- **Details:** Doc 08 §3 — add POST units (create), PATCH reorder/rename, PATCH progress (one-tap toggle, typed covered_on).
- **Closes:** B-SYL-1..3.

### T-403 — Frontend: Planner → Syllabus (one-tap, progress bar, add-first-unit)
- **Layer:** Frontend · **Depends:** T-402, T-105 · **Touches:** new `PlannerScreen.kt`; rework `TeacherSyllabusScreenV2.kt`
- **Details:** Doc 08 §2 + Doc 10 — single-tap toggle (no form), progress bar, edit-mode for add/reorder, honest empty state.
- **Closes:** F-SYL-1..4.

### T-404 — Migration: homework typed (attachments, extensions, FK submissions)
- **Layer:** Migration · **Depends:** T-001, T-004 · **Touches:** `migration_019_homework.sql`; `db/Tables.kt`
- **Details:** Doc 08 §5.3 — homework: assignment/class/subject FKs, due_time, allow_late; new `homework_attachments`, `homework_extensions`; submissions: student_id FK, not_submitted/late states.
- **Closes:** D-HW-1..5.

### T-405 — Backend: homework assign + submissions board (roster-join) + extend + attachments
- **Layer:** Backend · **Depends:** T-404, T-003 · **Touches:** `feature/teacher/TeacherRoutingTasks.kt` (homework)
- **Details:** Doc 08 §6/§7/§8 — POST assign (+attachment); GET submissions roster-joined (not-submitted appear); POST extend (class/student); no-submit-past-due enforced on student path.
- **Closes:** B-HW-1..6.

### T-406 — Frontend: Planner → Homework (assign sheet, board, extensions) — fixes dead button
- **Layer:** Frontend · **Depends:** T-405, T-403 · **Touches:** rework `TeacherHomeworkScreenV2.kt`
- **Details:** Doc 08 — wire the dead `onAssign` to a real assign sheet (title/due-date/attachment); submissions board with status columns; grant extension.
- **Closes:** **F-HW-1** (dead button), F-HW-2..4.

---

## PHASE 5 — CLASSES

### T-501 — Backend: GET /teacher/classes (single aggregated query, real is_class_teacher, atRisk)
- **Layer:** Backend · **Depends:** T-001, T-002, T-201, T-301 · **Touches:** `feature/teacher/TeacherRouting.kt`
- **Details:** Doc 09 §2 — replace 3× N+1 loop with one aggregated query set; real class-teacher flag; student count from enrollments; next period; today-marked; atRiskCount.
- **Closes:** B-CLS-1, B-CLS-2, B-CLS-3.

### T-502 — Backend: GET /teacher/classes/{id} composite (roster+signals+summaries)
- **Layer:** Backend · **Depends:** T-501 · **Touches:** `TeacherRouting.kt`
- **Details:** Doc 09 §3 — composite endpoint: roster (attendance rate, latest mark, flags), weekly timetable, next period, attendance summary, assessment schedule, active homework. Flags computed (Doc 09 §5).
- **Closes:** B-CLS-4/5.

### T-503 — Backend: GET /teacher/students/{id} (scoped profile)
- **Layer:** Backend · **Depends:** T-502 · **Touches:** new `TeacherStudentRouting.kt`
- **Details:** Doc 09 §4 — scoped student profile (attendance, performance, flags, gated parent contact); 403 if teacher doesn't teach student.
- **Closes:** B-PROF-1, B-PROF-2.

### T-504 — Frontend: Classes list + class detail (real roster, replaces VComingSoon)
- **Layer:** Frontend · **Depends:** T-502, T-105 · **Touches:** `ui/v2/screens/teacher/TeacherClassesScreenV2.kt`
- **Details:** Doc 09 §2/§3 + Doc 10 §6.5 — class list with badges; class detail sections; **real roster replacing `VComingSoon`**; long-list virtualization.
- **Closes:** **F-CLS-5** (VComingSoon), F-CLS-1..4/6.

### T-505 — Frontend: student profile drill-down
- **Layer:** Frontend · **Depends:** T-503, T-504 · **Touches:** new `TeacherStudentProfileScreen.kt`
- **Details:** Doc 09 §4 — header, attendance, performance trajectory, flags, gated contact.
- **Closes:** F-PROF-3.

---

## PHASE 6 — PROFILE, NAV CHROME, POLISH

### T-601 — Frontend: teacher dock (5 tabs) + real Today badge, header class-context chip
- **Layer:** Frontend · **Depends:** T-105, T-107 · **Touches:** `TeacherPortalV2.kt`, new `TeacherDock.kt`
- **Details:** Doc 04 §4/§6 + Doc 10 §5 — ParentDock-physics dock; badge fed by obligations (not hardcoded 1); header class-context chip; eliminate the shared Update picker entirely.
- **Closes:** F-SHELL-2, F-SHELL-4.

### T-602 — Backend+Frontend: Profile (identity, full week schedule, leave apply, settings)
- **Layer:** Backend→Frontend (T-602a/b) · **Depends:** T-104, T-601 · **Touches:** `feature/teacher/TeacherRouting.kt` (profile PATCH), `TeacherProfileScreenV2.kt`
- **Details:** Doc 04 §5.14 — wire inert setting rows (password change via `mustChangePassword`, theme Light/Warm/Night, notifications, logout); full weekly schedule view; leave apply sheet (date-from/to, reason, image) + status list.
- **Closes:** F-PROF-1, F-PROF-2, B-PROF (profile PATCH gap).

### T-603 — Frontend: state-host audit pass (loading/empty/error everywhere)
- **Layer:** Frontend · **Depends:** all prior frontend · **Touches:** all teacher screens
- **Details:** Doc 10 §8 — verify every surface has skeleton/empty/error via VStateHost; honest empties; retry on error; no fabricated content anywhere.
- **Closes:** F-DATA-1..4.

### T-604 — Accessibility & cross-portal consistency audit
- **Layer:** Frontend · **Depends:** T-603 · **Touches:** all teacher screens
- **Details:** Doc 10 §11/§12 checklist — 48dp targets, ≥16sp primary, font-scale to 200%, color-independent status, content descriptions, reduce-motion, biometric fallback. Confirm reuse of VCard/dock/header/schedule card.
- **Closes:** accessibility tensions logged in Doc 03.

---

## 12. Dependency Graph (summary)

```
P0:  T-001 ─┬─ T-002 ── T-003
            └─ T-004 ── T-005 (seed; needs 001,002,004)
P1:  (002,004)→T-101 ; (004)→T-102 ; T-103 ; (101,102,103,003)→T-104 →T-105 →{T-106,T-107}
P2:  (001,004)→T-201 ; T-202 ; (201,202,003)→T-203 →T-204 ; (203,105)→T-205
P3:  (001,002,004)→T-301 ; T-302 ; (301,302,003)→T-303 →T-304 ; (303,105)→T-305 →T-306
P4:  (002,004)→T-401 →T-402 →(105)T-403 ; (001,004)→T-404 →T-405 →(403)T-406
P5:  (001,002,201,301)→T-501 →T-502 →T-503 ; (502,105)→T-504 →T-505
P6:  (105,107)→T-601 →T-602 →T-603 →T-604
```

**Critical path:** T-001 → T-002 → T-003 → T-104 → T-105 (Today live) then phases P2–P5 largely parallelizable behind P0+T-105, converging at P6 polish.

---

## 13. Per-Commit Discipline (rules for every T-NNN)

1. **One concern per commit**; message in conventional form (e.g. `feat(teacher-attendance): scoped load with leave defaults [T-203]`).
2. **Migrations:** committed as `docs/backend/sql/migration_0NN_*.sql` **and** applied in Supabase SQL Editor (AUTO_CREATE off) before the dependent backend commit deploys. Commit notes the applied timestamp.
3. **Backend before frontend:** never merge a frontend commit whose endpoint isn't already on the branch.
4. **Each commit leaves the branch green** (compiles; existing flows unbroken — old screens kept until their replacement lands, then removed in the same commit).
5. **Defect traceability:** every commit body lists the `Closes:` defect IDs from Docs 01–03.
6. **Seed (T-005) is non-negotiable early** — without it the new surfaces are untestable/empty (X-5).

---

## 14. Closure Matrix (defect → task)

| Defect | Closed by |
|--------|-----------|
| X-1 (free-text class) | T-001, T-002, T-101 |
| X-2 (student code text) | T-001 |
| X-3 (string dates) | T-004 |
| X-4 (denormalized display as truth) | T-001, T-301 (joins for display) |
| X-5 (no teacher seed) | T-005 |
| X-6 (duplicate marks/calendar models) | T-301 |
| D-ATT-1/2/4 | T-201 ; D-ATT-3 → T-004 |
| D-ASMT-* | T-301 (dates via T-004) |
| D-SYL-* | T-401 (dates via T-004) |
| D-HW-* | T-404 (dates via T-004) |
| D-TT-1..4 | T-101 ; D-TT-5 → T-102 |
| B-MK-1 (force publish) | **T-303** |
| B-LV-2 (approval no attendance) | T-204 |
| B-FEED-1/2 (no calendar/empty) | T-104, T-005 |
| B-HOME-4 (fake tasks) | T-104 + T-107 |
| B-CLS-1/2/3 | T-501 |
| B-PROF-1/2 | T-503 |
| B-SYL-1 (no create) | T-402 |
| B-HW-1/3 (assign/board) | T-405 |
| F-HW-1 (dead button) | T-406 |
| F-CLS-5 (VComingSoon) | T-504 |
| F-SHELL-2/3 (shared picker) | T-601 (+T-305 exam picker) |
| F-SHELL-4 (hardcoded badge) | T-107 + T-601 |
| F-ATT-1/2 (buried/disabled date) | T-205 |

---

*End of 11_REBUILD_SEQUENCE.md*
