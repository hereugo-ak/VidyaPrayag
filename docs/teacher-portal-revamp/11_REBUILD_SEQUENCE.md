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

## PHASE 0 — FOUNDATION (typed identity + scope) — ✅ COMPLETED

These resolve the cross-cutting root defects (X-1..X-6) that everything else stands on. Nothing user-visible ships until P1, but **none of the later phases are safe without these**.

> **STATUS:** ✅ **Phase 0 complete.** All foundation tasks executed and pushed to `backend-by-abuzar_v1.0.3`:
> T-001 (`enrollments` table, commit `02ac955`), T-002 (TSA typed FKs + `is_class_teacher`, commit `a7f26cd`),
> T-003 (TeacherAccess id-first scope, commit `1242d34`), T-004 (typed dates everywhere, commit `364146a` + run guide `3a723c1`),
> T-005 (realistic teacher demo dataset, this commit). The typed-identity + scope spine (X-1..X-3, X-5) is now in place;
> Phase 1 (schedule & today) can build on it safely.

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
- **STATUS (executed):** ✅ `DemoSeed.kt` extended with the full teacher operational dataset, all idempotent (deterministic anchor ids / natural-key guards). Seeds in FK-dependency order: 2 demo classes (`school_classes`) + 4 per-class subjects (`school_subjects`) so TSA can carry typed `class_id`/`subject_id`; 3 `teacher_subject_assignments` for the demo teacher incl. **1 class-teacher** (Grade 4/A Math) + Grade 4/A Science + Grade 5/A Math; a **38-student roster** for Grade 4/A (`students` + typed `enrollments`, student #1 = the existing demo child `DEMO-S001`); a full **Mon–Fri `teacher_periods`** timetable (3 periods/day, weekday 1..5); **9 `syllabus_units`** across the subjects (some pre-covered with `covered_on`); **2 published `assessments`** (Unit Test I, Math + Science) with deterministic marks for the whole roster; **2 `homework`** rows (due in +2 days) with ~60% deterministic submissions; **2 approved `leave_requests`**.
  - **Note (DB-side only — Rule 4):** T-005 is pure seed data; it touches only `DemoSeed.kt` and adds no new types, so no consumer files change. The demo school previously had **zero** classes/subjects, so the seed creates them first (TSA FKs require them) rather than assuming an operator-provisioned school. `leave_requests.date_from/date_to` are still `varchar(12)` (intentionally out of T-004 scope), so leave dates are seeded as `"YYYY-MM-DD"` strings; all other dates use typed `LocalDate` (T-004).

---

## PHASE 1 — SCHEDULE & TODAY (the daily spine)

### T-101 — Migration: typed teacher_periods + assignment binding + exceptions
- **Layer:** Migration · **Depends:** T-002, T-004
- **Touches:** `migration_013_periods.sql`; `db/Tables.kt` (`TeacherPeriodsTable`, new `PeriodExceptionsTable`)
- **Details:** Doc 05 §2.1/2.2 — `start_time/end_time` → `time`; add `assignment_id` FK, `academic_year_id`, `valid_from/to`; create `period_exceptions`. Drop className/section/subject as truth (derive via assignment).
- **Done when:** applied; periods resolve class via assignment.
- **Closes:** D-TT-1..4.
- **STATUS (executed):** ✅ `migration_011_periods.sql` created (idempotent; `information_schema`-guarded `start_time/end_time` varchar→`time`; adds `academic_year_id`/`assignment_id`/`valid_from`/`valid_to`/`is_active`; FKs + CHECKs guarded; best-effort `assignment_id` backfill from the legacy display tuple; `period_exceptions` table; post-run report of un-bound periods). `Tables.kt`: `TeacherPeriodsTable.startTime/endTime` flipped to `time()` (LocalTime), new columns added, no-double-book unique index; new `PeriodExceptionsTable` registered in `DatabaseFactory.kt`. `DemoSeed.kt` (T-005) periods now write typed `LocalTime` + bind each slot to its TSA (`assignment_id`). Registered in `PROVISION.sql` (step 11).
  - **Note 1 (filename — docs are authority, deviation flagged):** doc named it `migration_013_periods.sql`; the on-disk chain is contiguous and the prior migration is `010`, so the file is `migration_011_periods.sql` (same documented deviation pattern as 009/010).
  - **Note 2 (className/section/subject NOT dropped yet — Rule 4):** Doc 05 §2.1 says drop them as source of truth; they are instead **demoted to display/legacy fallback** (kept) so the existing timetable readers (`feature/parent/ParentAcademicsRouting.kt`, `feature/school/SchoolTimetableRouting.kt`, `feature/teacher/TeacherRouting.kt`) keep compiling green in the same release. `assignment_id` is now the authoritative binding; a later phase drops the legacy columns once every reader derives class/subject via the FK.
  - **Note 3 (extended touch-list — Rule 4):** flipping `start_time/end_time` to `time` changes every period reader's value type, so T-101 also converts the three timetable readers above to format `LocalTime` → `"HH:mm"` at the DTO boundary (wire contract unchanged). `feature/user/ParentMessagesRouting.kt` reads only `class_name`/`teacher_id`, so it is unaffected.

### T-102 — Migration: single holiday source (deprecate HolidayListTable)
- **Layer:** Migration · **Depends:** T-004
- **Touches:** `migration_014_holidays_merge.sql`
- **Details:** migrate `holiday_list` rows into `calendar_events(type=HOLIDAY,status=PUBLISHED)`; deprecate `HolidayListTable`.
- **Done when:** applied; holidays read only from calendar_events.
- **Closes:** D-TT-5.
- **STATUS (executed):** ✅ `migration_012_holidays_merge.sql` created (idempotent; per-row tolerant date cast varchar→`date`; forward-fills every `holiday_list` row into `calendar_events(type=HOLIDAY,status=PUBLISHED,source=MANUAL)` guarded by `source_ref='HL:<id>'` so a re-run never duplicates; post-run report of the canonical published-holiday view). `Tables.kt`: `HolidayListTable` annotated `@Deprecated` (legacy read-only). Registered in `PROVISION.sql` (step 12). The resolved-day computation (T-104) reads holidays ONLY from `calendar_events`.
  - **Note 1 (filename — docs are authority, deviation flagged):** doc named it `migration_014_holidays_merge.sql`; the on-disk chain is contiguous and the prior migration is `011`, so the file is `migration_012_holidays_merge.sql` (same documented deviation pattern as 009/010/011).
  - **Note 2 (HolidayListTable NOT dropped yet — Rule 4):** the doc says "deprecate HolidayListTable". The data is migrated and the mapping `@Deprecated`, but the table + mapping are KEPT (not dropped) so the two surviving legacy readers — `feature/parent/ParentAcademicsRouting.kt` and `feature/school/SchoolRouting.kt` — keep compiling green in this release. A later phase repoints both readers at `calendar_events` and then drops the table (same demotion pattern as T-101's legacy period columns).

### T-103 — Shared DTO: ResolvedDay / ResolvedWeek models
- **Layer:** Shared-DTO · **Depends:** —
- **Touches:** `shared/.../feature/teacher/domain/model/TeacherModels.kt`
- **Details:** add `ResolvedDayDto`, `ResolvedPeriodDto`, `CalendarOverlayDto`, `CheckInStatusDto` (Doc 05 §4) with `@SerialName`.
- **Done when:** serialization round-trips in shared tests.
- **STATUS (executed):** ✅ Added to `TeacherModels.kt`: `ResolvedDayResponse`/`ResolvedDayDto`, `ResolvedPeriodDto` (with status, `attendance_marked`, substitute + overlap flags), `CalendarOverlayDto` (with `assessment_id` deep-link), `ResolvedWeekResponse`/`ResolvedWeekDto`, `CheckInStatusResponse`/`CheckInStatusDto` + `TeacherCheckInRequest` (T-106 shared side), and `TeacherObligationsResponse`/`TeacherObligationsDto`/`ObligationItemDto` (T-107 shared side, with an honest `isAllCaughtUp` helper). All snake_case `@SerialName` matching the wire contract. Round-trip test `shared/.../commonTest/.../TeacherTodayModelsTest.kt` asserts encode/decode equality + snake_case field names + the all-caught-up honesty rule.
  - **Note (scope — Rule 4):** the doc's "Touches" listed only the four core resolved-day DTOs; the check-in (T-106) and obligations (T-107) shared models are added here too since they share the Today wire surface and keep the later frontend commits from re-touching this file. The matching server DTOs land with their endpoints (T-104/106/107).

### T-104 — Backend: GET /teacher/day + /teacher/week (resolved schedule)
- **Layer:** Backend · **Depends:** T-101, T-102, T-103, T-003
- **Touches:** new `feature/teacher/TeacherDayRouting.kt`
- **Details:** Doc 05 §4 — merge periods + exceptions + holidays + calendar + per-period `attendanceMarked` (join attendance_records). Authoritative `nowIndex/nextIndex` from server clock.
- **Done when:** returns correct resolved day for seeded teacher incl. a holiday + a cancelled period; `attendanceMarked` accurate.
- **Closes:** B-FEED-1, B-FEED-2, B-HOME-4 (data side), B-HOME-1 (N+1 attendance loop replaced by joined query).
- **STATUS (executed):** ✅ New file `server/.../feature/teacher/TeacherDayRouting.kt` with `GET /api/v1/teacher/day?date=` and `GET /api/v1/teacher/week[?date=]`, registered in `Application.kt` (after `teacherRouting()`). A single `resolveDayInTxn(...)` resolver (reused by both endpoints) merges, for a specific date: published HOLIDAY/overlay events from `calendar_events` (T-102's single source) → recurring `teacher_periods` (weekday + `valid_from`/`valid_to` term window + `is_active`) → `period_exceptions` (CANCELLED struck-through, RESCHEDULED/ROOM_CHANGE applied, SUBSTITUTION names the sub & flags `is_substitute_for_me`, plus EXTRA / substitute-insert rows that belong to me) → per-period `attendance_marked` via **one batched** `attendance_records` query over the day's distinct `"<class>-<section>"` grade strings (B-HOME-1 N+1 killed) → overlap flagging (Doc 05 §6) → authoritative `now_index`/`next_index` from the server clock (CANCELLED periods skipped). Holiday short-circuits to `is_holiday=true` + overlay + no periods (Doc 06). `/week` resolves Mon–Sat inside ONE transaction sharing the assignment-scope cache. Display scope is assignment-first (TSA className/section/subject), falling back to the period's demoted display columns only when unbound (Doc 05 §2.1). Server DTOs mirror the T-103 shared models field-for-field. The per-request assignment-scope cache is a **local** `HashMap` threaded as a parameter (never a shared field) so concurrent requests can't race.
  - **Note 1 (deviation — assessment deep-link):** Doc 05 §3.3 / Doc 07 §4.1 envisage an EXAM calendar event deep-linking to the teacher's matching assessment via `CalendarOverlayDto.assessment_id`. The current schema has **no link column** between `calendar_events(EXAM)` and `assessments` (`assessments.examId` in the API is the assessment's own UUID, not a calendar `event_code`; there is no `exam_id`/`event_code` column on `assessments`). Rather than fabricate a fragile name/date heuristic (violates the honesty rule), `assessment_id` is returned `null` here and the real binding is deferred to the Gradebook phase (P3), where the assessment↔exam link is introduced. The contract field is preserved.
  - **Note 2 (filename/registration):** route mounted as `teacherDayRouting()` immediately after `teacherRouting()` so it shares the `/api/v1/teacher` prefix; no change to existing teacher routes (DELETE-don't-patch applies to screens, not to additive new endpoints).

### T-105 — Frontend: Today tab shell + schedule card (3 faces)
- **Layer:** Frontend · **Depends:** T-104
- **Touches:** new `ui/v2/screens/teacher/TodayScreen.kt`, `TeacherScheduleCard.kt`; `TeacherPortalV2.kt` (introduce new tab structure Doc 04 §4)
- **Details:** greeting bar, Now/Next card with pre-scoped CTAs, 3-face swipe (Doc 05 §5, Doc 10 §6.2), VStateHost states.
- **Done when:** Today renders seeded schedule; empty/holiday/unseeded states distinct (Doc 10 §8).
- **Closes:** F-HOME-1..7 (structure), F-SHELL-1.
- **STATUS (executed):** ✅ Shared (T-105a): `TeacherApi.getDay/getWeek` (+ `TeacherRepository` interface + `TeacherRepositoryImpl` overrides) over the T-104 endpoints, returning the T-103 `ResolvedDayResponse`/`ResolvedWeekResponse`. ViewModel (T-105b): new `TeacherTodayViewModel` (state = loading/error/`teacherName`/`day`/`week`/`weekStart`; `isUnseeded` computed) with `ResolvedDayUi`/`ResolvedPeriodUi`/`CalendarOverlayUi` UI models + `.toUi()` mappers (`now_index`/`next_index` mapped `?: -1`); registered as a Koin `factory`. **It does NO client clock math to rank periods** — the server's `now_index`/`next_index` are authoritative (Doc 05 §6); the device clock only drives the live "now" pulse and a `refreshIfStale` day-rollover refetch. UI (T-105c): new `TeacherScheduleCard.kt` modelled face-for-face on `ParentScheduleCard` (component-scoped `face` state, ±36px swipe, `AnimatedContent` slide+fade) — Face 0 NowNext (verdict hero "Teaching now"/"Covering now"/"Up next"/"Day complete" from `nowIndex`/`nextIndex`, `AttendanceBadge`, **pre-scoped CTAs** [Mark attendance][Syllabus][Homework] shown ONLY when the focused period carries a non-null `assignmentId` and isn't cancelled, day-progress, calendar chips, holiday/unseeded plates), Face 1 timeline (done/now/cancelled/upcoming nodes, struck-through cancelled, substitution/room/note meta, overlap chip), Face 2 weekly Mon–Sat grid (holiday badged, cancelled struck-through); new `TodayScreen.kt` (greeting bar + `VStateHost` → card; holiday/unseeded handled INSIDE the card as content states so the host "empty" leg only fires when the day genuinely fails to resolve). Shell (T-105d): `TeacherPortalV2.kt` rebuilt to the **5-tab IA** (Today · Classes · Gradebook · Planner · Profile); attendance is now an ACTION (the Update write-plane Attendance/Marks/Syllabus/Homework moved off the bottom nav into a full-screen overlay, opened pre-seeded from a Today CTA's pre-authorized assignment).
  - **Note 1 (deviation, SEQUENCE design):** Gradebook (P3 / T-301..306) and Planner (P4 / T-401..406) tabs are honest "coming in this phase" `StagedTab` placeholders — wired now so the 5-tab IA is visible/stable, with their real screens landing in their own phases (avoids a late nav reshuffle). Documented per the constitution's flag-deviations rule.
  - **Note 2:** the greeting name is sourced best-effort from `PreferenceRepository.getUserName()` (surfaced as `TeacherTodayState.teacherName`); blank falls back to "Teacher".

### T-106 — Migration+Backend+Frontend: teacher self check-in
- **Layer:** Migration→Backend→Frontend (3 commits T-106a/b/c) · **Depends:** T-105
- **Touches:** `migration_015_teacher_checkins.sql` (a); `TeacherDayRouting.kt` POST/GET checkin (b); `expect/actual BiometricAuthenticator` + check-in card (c)
- **Details:** Doc 06 §2 — `teacher_check_ins` table; idempotent endpoint; biometric ladder (biometric→PIN→manual) with always-available fallback.
- **Done when:** check-in flips pill; works with biometric and with manual fallback on a device without biometrics.
- **Closes:** B-ATT-5.
- **STATUS (executed):** ✅ T-106a — `teacher_check_ins` table + Exposed `TeacherCheckInsTable` (UNIQUE on school+teacher+date for idempotency). **Filename deviation:** the doc names it `migration_015_teacher_checkins.sql`; the on-disk chain is contiguous so it landed as `migration_013_*` (the docs-vs-disk offset pattern noted in this file's preamble). T-106b — `POST/GET /api/v1/teacher/checkin` in `TeacherDayRouting.kt`: idempotent per (school,teacher,date) — a repeat POST returns the existing row, never duplicates; `checked_in_at` is SERVER-stamped (`Instant.now()`, device clock never trusted, Doc 06 §2.4); `method` records the rung (biometric|pin|manual). T-106c — `expect/actual BiometricAuthenticator` placed in `composeApp` (it's a Compose-UI/Activity concern, not a `:shared` transport one) with all four actuals: Android (real AndroidX `BiometricPrompt` + DEVICE_CREDENTIAL), iOS (`LAContext` `canEvaluatePolicy`/`evaluatePolicy`), jvm + web (capability=None, fail-fast). Shared API/repo wiring + `TeacherCheckInViewModel` (optimistic flip, reconciles to the server timestamp, idempotent guard) + Koin factory; `TeacherCheckInCard` (amber↔green two faces via `AnimatedContent`, runs the ladder: capability None → manual; Cancelled/Failed → manual; "Confirm manually instead" always visible), inserted in the Today greeting band.
  - **Note (deviation — Activity type):** AndroidX `BiometricPrompt` requires a `FragmentActivity`, but the app's `MainActivity` is a `ComponentActivity`. Rather than refactor `MainActivity` (risking existing flows), the Android actual reports `capability=None` when no `FragmentActivity` is found (`tailrec` through `ContextWrapper`); the always-available **manual confirm** is the working rung. Wiring `BiometricPrompt` end-to-end is left for a focused Activity-type change. Flagged per the constitution.

### T-107 — Frontend: real obligations strip on Today
- **Layer:** Frontend (+small backend aggregate) · **Depends:** T-104, (T-204, T-304 for full counts — strip ships incrementally)
- **Touches:** `TodayScreen.kt`; backend `GET /teacher/obligations`
- **Details:** Doc 04 §5.5 — real counts (unmarked classes today, unpublished results, submissions to review). Replaces fabricated tasks.
- **Done when:** counts match seeded reality; "all caught up" only when truly zero.
- **Closes:** B-HOME-4 (UI side), F-SHELL-4 (dock badge fed from this), F-HOME-3.
- **STATUS (executed):** ✅ Backend `GET /api/v1/teacher/obligations` added to `TeacherDayRouting.kt` (additive inside the already-registered `teacherDayRouting()` — no new `Application.kt` line). REAL counts, all scoped to the teacher's allocation at the QUERY level (the constitution's third scoping level): **unmarked_classes** — today's non-cancelled, attendance-bearing periods whose `attendance_marked` is false, computed by **reusing `resolveDayInTxn`** so the count can never drift from the schedule card; **unpublished_results** — my active assessments with `is_published=false`; **submissions_to_review** — `homework_submissions` still in the `submitted` (ungraded) state on homework I authored; **pending_leave_decisions** — student leave requests routed to me (direct `teacher_id` OR one of my class/section pairs, mirroring `TeacherLeaveRouting`'s exact scope), status Pending. Privileged roles (school_admin/admin) see the whole school, consistent with the other teacher endpoints. Each item carries a pre-scoped deep-link (`assignment_id`/`ref_id`). Server DTOs (`TeacherObligationsDto`/`ObligationItemDto`) mirror the T-103 shared models field-for-field. Shared: `TeacherApi.getObligations` + repo interface/impl. ViewModel: new `TeacherObligationsViewModel` (separate from the schedule VM so the strip refreshes independently) exposing per-category counts, the deep-linkable items, `isAllCaughtUp` (TRUE only when the load **succeeded** AND every count is genuinely zero — a failed/in-flight load is never "caught up"), and `totalOutstanding` (feeds the F-SHELL-4 dock badge); Koin factory. UI: new `TeacherObligationsStrip` on Today between the check-in band and the schedule card — REAL rows only (kills B-HOME-4's fabricated tasks), calm green "All caught up" ONLY when truly zero, **hides** on read-failure (never fabricates done-ness), each row deep-links pre-scoped via `onOpenObligation` → `TeacherPortalV2.openObligation` (attendance → Update plane pre-seeded by assignment id; leave → Leave inbox; marks → Gradebook tab; homework → Planner tab).
  - **Note (deviation — incremental counts, SEQUENCE design):** Doc 11 explicitly states this strip "ships incrementally" because full marks/attendance semantics depend on T-204/T-304 (not yet built). The counts implemented are exactly those the **current** schema can answer honestly; `marks`/`homework` rows currently deep-link to the honest **staged** Gradebook/Planner tabs (their real publish/review screens land in P3/P4), at which point those taps will resolve to the real flows with no contract change. Flagged per the constitution.

---

## PHASE 2 — ATTENDANCE

### T-201 — Migration: typed attendance (FK student, +leave, assignment binding)
- **Layer:** Migration · **Depends:** T-001, T-004
- **Touches:** `migration_016_attendance.sql`; `db/Tables.kt` (`AttendanceRecordsTable`)
- **Details:** Doc 06 §1.2 — `student_id`/`enrollment_id`/`faculty_id`/`assignment_id` FKs; add `leave` to status enum; add `source`, `marked_at`; drop packed `grade`. Migrate existing rows (parse grade → class/section → assignment best-effort; quarantine).
- **Done when:** applied; status enum includes leave; grade column gone.
- **Closes:** D-ATT-1, D-ATT-2, D-ATT-4.
- **STATUS (executed):** ✅ `AttendanceRecordsTable` rewritten to the typed shape: added `student_id`/`enrollment_id`/`faculty_id`/`assignment_id` (uuid nullable FKs), `source` (varchar16 default `manual`), `marked_at` (timestamp nullable); new typed `uniqueIndex(school,date,type,student,assignment)`. Migration `docs/db/migration_014_attendance.sql` created — non-destructive & idempotent: ADD typed columns + FKs (ON DELETE SET NULL), relax `person_id` to NULL, backfill `student_id` (by student_code), backfill `enrollment_id` (single-match on the packed grade tuple), backfill `marked_at←created_at`, normalize status/type lowercase, ADD typed UNIQUE + hot-path indexes, ADD CHECK constraints (status∈present/absent/late/leave; type∈student/faculty), POST-RUN report.
- **DEVIATIONS (flagged):** (1) filename is `migration_014_attendance.sql` not `_016_` — the on-disk chain is contiguous (the docs-vs-disk offset noted in this file's preamble). (2) `grade` is **NOT** physically dropped — it's kept `@Deprecated` nullable because many current readers (admin dashboards, parent academics, the legacy teacher writer, the T-104 resolver) still reference it; dropping it now would break the build before T-205 retires those readers. `person_id` likewise kept nullable for the same reason. (3) `assignment_id` is **NOT** backfilled (the legacy grade string is ambiguous → honesty rule: don't fabricate a binding).

### T-202 — Shared DTO: attendance load/save models (scoped)
- **Layer:** Shared-DTO · **Depends:** —
- **Touches:** `TeacherModels.kt` (`AttendanceLoadDto` with leaveDefaults, alreadyMarked; `AttendanceSaveDto`)
- **STATUS (executed):** ✅ Added typed attendance DTOs to `TeacherModels.kt` (after the legacy `AttendanceMarkDto`, which is retained until T-205): `AttendanceLoadResponse`/`AttendanceLoadDto` (scope, className/section/subject, students, `alreadyMarked`, last-marked audit, `leaveDefaults`, holiday/cancelled flags, `backDateWindowDays`), `AttendanceStudentDto` (studentId/name/rollNo/status/source/enrollmentId), `AttendanceSaveRequest`/`AttendanceSaveMarkDto`, `AttendanceSaveResponse`/`AttendanceSaveResultDto`. All snake_case `@SerialName`. Round-trip + server-envelope (ignore `message`) + save tests added to `commonTest/.../TeacherTodayModelsTest.kt`.

### T-203 — Backend: GET/POST /teacher/attendance (scoped, leave-default, upsert)
- **Layer:** Backend · **Depends:** T-201, T-202, T-003
- **Touches:** `feature/teacher/TeacherRoutingTasks.kt` (attendance handlers)
- **Details:** Doc 06 §3.8 — roster from enrollments; pre-mark approved-leave students (`source=leave_auto`); date default today, back-date window guard; upsert on (school,date,type,student,assignment); `alreadyMarked` + last-marked meta.
- **Done when:** load returns enrollment roster with leave defaults; save upserts; edge cases E1/E3/E9/E10 enforced.
- **Closes:** B-ATT-1..4, B-ATT-6/7.
- **STATUS (executed):** ✅ New file `server/.../feature/teacher/TeacherAttendanceRouting.kt` (NOT a patch of the legacy `TeacherRoutingTasks.kt` handlers — those stay until T-205 deletes them, per the DELETE-don't-patch law) registered in `Application.kt` after `teacherDayRouting()`. `GET /attendance-typed?assignmentId=&date=` — roster from `enrollmentsFor` (E4/E5-honored active enrollments), published-HOLIDAY flag covering the date (E1), existing marks merged for EDIT with `alreadyMarked` + last-marked-by/at audit (E3), approved-leave students pre-defaulted to `leave`/`leave_auto` (§3.5), advertises `back_date_window_days`. `POST /attendance-typed` — future-date block + back-date window guard (E9, `ATT_BACK_DATE_WINDOW_DAYS=7`), upsert on (school,date,type=student,student_id,assignment_id) stamping `marked_by=me`, `marked_at=now`, `source=manual`, validating each mark against the typed roster + valid state space (present/absent/late/leave); **no publish side effects** (contrast B-MK-1). Server DTOs mirror shared `TeacherModels.kt` field-for-field. Scope enforced at all three constitution levels (SQL roster / API payload / pre-scoped UI in T-205). **Bug fixed in flight:** `leave_requests.child_id` is a FK to `children.id` (parent-side row), NOT `students.id`; the approved-leave resolver now maps `child_id → children.student_code → students.id` so the leave set can actually intersect the enrollment roster.
- **DEVIATION (flagged):** route path is `/attendance-typed`, not `/attendance`, because `teacherRouting()` already binds `GET/POST /attendance` and Ktor forbids two handlers on the same method+path. T-205 converges to `/attendance` and deletes the legacy handler.

### T-204 — Backend: leave approval writes attendance (B-LV-2)
- **Layer:** Backend · **Depends:** T-201, T-203
- **Touches:** `feature/teacher/TeacherLeaveRouting.kt`
- **Details:** Doc 06 §3.5 — approving a leave writes/queues `leave` marks for covered dates.
- **Done when:** approving a leave shows those students defaulted to leave on attendance load.
- **Closes:** B-LV-2.
- **STATUS (executed):** ✅ `TeacherLeaveRouting.kt` PATCH `/{id}` approve path now calls `writeLeaveMarksOnApproval(...)` **inside the same `dbQuery` transaction** that flips the request to Approved (atomic). It resolves `child_id → children.student_code → students.id` (same identity chain as T-203 — `leave_requests.child_id` is a FK to `children.id`, NOT `students.id`), expands `dateFrom..dateTo`, and upserts one **day-level** row per covered date keyed on `(school, date, type=student, student_id, assignment_id IS NULL)` with `status=leave`, `source=leave_auto`, `marked_by=actioner`. Manual upsert (Postgres treats NULL `assignment_id` as distinct, so the typed unique index can't dedupe) → idempotent across re-approvals; never clobbers an existing non-`leave_auto` mark (a leave-approved student who actually attended keeps the teacher's manual mark); resolution misses return 0 silently so the approval still succeeds; rejections write nothing. This closes B-LV-2 from the WRITE direction (T-203 already closed the READ direction via `approvedLeaveStudentIds`).
- **DEVIATION (flagged):** day-level write uses `assignment_id = NULL` (a leave is a whole-day fact, not one teacher's period; per-assignment fan-out would collide with T-203 per-period marks); `LEAVE_WRITE_MAX_DAYS=62` caps one approval's row fan-out for write-amplification safety (spec names no cap).

### T-205 — Frontend: attendance screen rebuild (4 pills, enabled date, bulk, scoped)
- **Layer:** Frontend · **Depends:** T-203, T-105
- **Touches:** `ui/v2/screens/teacher/TeacherAttendanceScreenV2.kt`, `presentation/TeacherAttendanceViewModel.kt`
- **Details:** Doc 06 §3 + Doc 10 §6.3 — reached pre-scoped from Today/Classes (no shared picker); P/A/L/Lv pills ≥48dp; enabled date picker (fixes F-ATT-2); leave defaults; bulk "all present"; running counter; result-driven save; wrong-class guard header.
- **Done when:** mark 7B in <60s; back-date works within window; already-marked loads for edit; badge flips on Today.
- **Closes:** F-ATT-1..7.
- **STATUS (executed):** ✅ The attendance plane was rebuilt from scratch (DELETE-don't-patch) across all three layers, converging the API contract on the canonical `/attendance` path.
  - **Backend converge + delete legacy:** the legacy packed-`grade` `route("/attendance") { get/post }` handler in `TeacherRoutingTasks.kt` — plus its now-dead DTOs (`AttendanceEntryDto`/`TeacherAttendanceData`/`AttendanceMarkDto`/`SubmitAttendanceRequest`) and `VALID_ATTENDANCE` — were **deleted**. The typed, assignment-scoped plane in `TeacherAttendanceRouting.kt` (T-203) renamed `/attendance-typed → /attendance` (no more method+path collision since the legacy handler is gone) and now OWNS `GET/POST /api/v1/teacher/attendance`. The RA-41 parent absent/late alert was **preserved** (moved into the typed POST, resolving `student_id → student_code` from the enrollment roster for `NotifyRecipients.parentsOfStudent`). `Application.kt` registration comment updated.
  - **Shared:** `TeacherApi.getAttendance/submitAttendance` (class_id+grade) replaced by `loadAttendance(assignmentId, date?)` / `saveAttendance(AttendanceSaveRequest)` over the typed envelopes (`AttendanceLoadResponse`/`AttendanceSaveResponse`, T-202); same on `TeacherRepository`/`Impl`. The five legacy teacher attendance DTOs in `TeacherModels.kt` deleted (admin's same-named `AttendanceEntryDto` is a SEPARATE package — untouched).
  - **ViewModel:** `TeacherAttendanceViewModel` rewritten clean — assignment-scoped `load`, enabled/correctable `changeDate` (F-ATT-2), 4-state `setStatus` (present·absent·late·leave, D-ATT-1), `markAllPresent` that preserves manual + approved-leave exceptions (§3.6), live counters (present/absent/late/leave/unmarked), load-for-EDIT audit (`alreadyMarked`/`lastMarkedBy`/`lastMarkedAt`, E3), holiday/back-date-window flags (E1/E9), and result-driven `save` (`isSaving`/`saveSuccess`/`saveError`) with NO publish side effect (§3.7).
  - **Screen:** `TeacherAttendanceScreenV2` rebuilt face-for-face on the parents design system — `VCard`/`VStateHost`/`VButton`/`VDatePicker`, semantic tokens (success/danger/warning/accentSoft), DM-Mono counters. Wrong-class **guard header** (E15: class · subject · date, instant via `scopeHint`, server-confirmed); 4 P/A/L/Lv pills ≥48dp, color **and** letter encoded with `contentDescription` (Doc 10 §11); approved-leave students badged; holiday warn banner (E1); virtualized `LazyColumn` roster (Doc 10 §7); sticky counter + result-driven Save footer; honest empty/loading/error.
  - **Shell wiring:** `TeacherPortalV2` threads the pre-authorized `assignmentId` + a pre-known `scopeHint` to the screen and **removes the shared `TeacherClassPicker` for the Attendance leaf** (F-ATT-1 — attendance is reached pre-scoped, never re-picked). The obligations/Today CTAs already carry the assignment id.
  - **DEVIATIONS (flagged):** (1) Per-period **back-date window** stays server-enforced (`ATT_BACK_DATE_WINDOW_DAYS=7`); the date picker is intentionally left fully enabled (F-ATT-2 is about the *disabled* field) — a save outside the window returns a clear `BACK_DATE_BLOCKED` message rather than disabling earlier dates client-side (avoids a second source of truth). (2) Offline/queued save (E11) is **not** implemented this commit — the save is online result-driven; the optimistic-queue path is deferred to the T-603 state-host/offline pass (no schema/contract change needed later). (3) Marks/Syllabus leaves still front the shared `TeacherClassPicker` until their own rebuilds (P3/P4); only the Attendance leaf is converged here.

---

## PHASE 3 — GRADEBOOK (assessment + marks)

### T-301 — Migration: canonical marks model (+type, pass, calendar tie, FK) & deprecate ExamResults
- **Layer:** Migration · **Depends:** T-001, T-002, T-004
- **Touches:** `migration_017_assessments.sql`; `db/Tables.kt`
- **Details:** Doc 07 §1.3 — assessments: assignment_id/class_id/subject_id FKs, type, pass_marks, calendar_event_id, status enum; assessment_marks: student_id FK, is_absent, remark; migrate `ExamResultsTable` → assessment_marks; deprecate ExamResults.
- **Done when:** applied; single marks model; ExamResults read-only/empty.
- **Closes:** X-6, D-ASMT-1..6.
- **STATUS (executed):** ✅ `AssessmentsTable` extended to the Doc 07 §1.3 canonical, typed, scope-bound shape — added `academic_year_id`, `assignment_id` (X-1/D-ASMT-6 provable scope), `class_id`, `subject_id` (all nullable FKs), `type` (default `scheduled`, D-ASMT-4), `pass_marks` (D-ASMT-4), `calendar_event_id` (D-ASMT-5), `status` (the publish-discipline machine: draft→scheduled→marks_pending→published→archived, drives the T-303 B-MK-1 fix), `created_by`. `AssessmentMarksTable` extended — added `student_ref` (uuid FK→students.id, the typed WHO, D-ASMT-3), `is_absent` (AB≠0), `remark`, `entered_at`. Legacy columns kept (`AssessmentMarksTable.studentId`/`studentName` marked `@Deprecated`; `is_published`/`publishedAt` kept in sync with `status='published'`) so out-of-scope readers don't break. Migration `docs/db/migration_015_assessments.sql` created — non-destructive & idempotent: ADD all typed columns (IF NOT EXISTS), backfill `status` from `is_published`, backfill `student_ref` from `student_code` (school-scoped), backfill `entered_at←updated_at`, ADD FKs (ON DELETE SET NULL so a deleted scope row never cascade-deletes graded marks), ADD CHECK constraints (type domain; status domain; `0≤pass_marks≤max_marks`; `marks≥0`), ADD hot-path indexes, and **MIRROR** numeric-scored `exam_results` rows into `assessments`/`assessment_marks` (`type='exam'`, idempotent via md5→uuid synthetic ids + ON CONFLICT DO NOTHING) so the teacher Gradebook history (T-304/T-306) is complete. `ExamResultsTable` annotated `@Deprecated` (single-source-of-truth note).
- **DEVIATIONS (flagged):** (1) filename is `migration_015_assessments.sql` not `_017_` — the on-disk chain is contiguous (the docs-vs-disk offset noted in this file's preamble; last on-disk was `_014_`). (2) `ExamResultsTable` is **NOT** dropped or emptied — 5 school-admin routers (`ResultsRouting`, `SchoolAnalyticsRouting`, `SchoolStudentsRouting`, `AdminDashboard*Routing`, 78 refs) still read it and are OUT OF SCOPE for the teacher rebuild; per the non-destructive-schema rule the migration MIRRORS its numeric rows into the canonical model (so teacher reads are complete) and leaves the legacy table read-only for admins. "Single marks model / ExamResults read-only" is honoured for the teacher portal; physical drop awaits a future admin-portal pass. (3) Legacy `assessment_marks.student_id`/`student_name` are **NOT** dropped — the legacy teacher `/marks` handler (deleted in T-303) and `ParentAcademicsRouting` still select them; they retire when those readers do (same T-201 retention pattern). (4) `class_id`/`subject_id`/`assignment_id` are **NOT** backfilled for legacy rows — the free-text class/subject can't be resolved to ids unambiguously (honesty rule: don't fabricate a binding); new writes (T-303) always set them. (5) Mirrored exam rows default `max_marks=100` (legacy `exam_results.score` is a percentage-style model with no explicit denominator) — flagged as an approximation, not a fabricated per-test maximum.

### T-302 — Shared DTO: assessment + marks lifecycle models
- **Layer:** Shared-DTO · **Depends:** — · **Touches:** `TeacherModels.kt` (`AssessmentDto` with status, `MarksLoadDto`, `MarksSaveDto`, `AssessmentHistoryDto`)
- **STATUS (executed):** ✅ Added the canonical gradebook contract to `TeacherModels.kt`: `AssessmentStatus`/`AssessmentType` string constants (lenient, unknown→draft); `AssessmentDto` (scope-bound `assignment_id`/`class_id`/`subject_id`, joined display `class_name`/`section`/`subject`, `type`, `max_marks`, `pass_marks`, typed `exam_date`, `calendar_event_id`, lifecycle `status` + `published_at`, `entered_count`/`roster_count`, honest `isPublished`/`isDraft`/`hasPassLine` helpers); `AssessmentListResponse`/`AssessmentListData`; `CreateAssessmentRequestV2` (assignment-scoped, optional calendar tie); `MarksLoadResponse`/`MarksLoadDto`/`MarkEntryDto` (roster restores entered values + `is_absent` AB-distinct-from-0, §5.3); `MarksSaveRequest`/`MarkSaveEntryDto`/`MarksSaveResponse`/`MarksSaveResultDto` (**SAVE payload carries NO publish concept — the structural B-MK-1 fix**; result echoes the still-`marks_pending` status); `PublishResponse`/`PublishResultDto` (the only paths that notify, with `parents_notified` count); `AssessmentHistoryResponse`/`AssessmentHistoryDto`/`AssessmentTrendPointDto`/`MarkBucketDto` (server-aggregated timeline + distribution, honest `hasData`). All snake_case `@SerialName` mirroring the server wire surface. Wired the seven T-303/T-304 endpoints into `TeacherApi` (`listAssessments`/`createAssessmentV2`/`getAssessmentMarks`/`saveAssessmentMarks`/`publishAssessment`/`unpublishAssessment`/`getAssessmentHistory`) + `TeacherRepository`/`Impl`. Round-trip + B-MK-1-structural-absence + honest-empty tests added to `TeacherTodayModelsTest.kt`.
  - **Note (scope — Rule 4):** the doc's "Touches" listed only `TeacherModels.kt`; the API/repo wiring is added here too (same pattern as T-103/T-202) so the T-305 frontend commit doesn't re-touch the transport layer. The legacy `TeacherMarksResponse`/`SubmitMarksRequest`/`TeacherAssessmentDto` + their API/repo methods are RETAINED (DELETE-don't-patch: they stay valid until T-303 deletes the legacy `/marks` handler and T-305 reworks the screen in the same commits that remove them). New create method is `createAssessmentV2` to avoid colliding with the legacy `createAssessment` until T-305 retires it.

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
