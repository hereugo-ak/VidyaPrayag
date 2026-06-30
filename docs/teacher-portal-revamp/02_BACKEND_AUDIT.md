# 02 — BACKEND AUDIT

> **Scope:** Every teacher-facing Ktor flow — what endpoint *should* exist, what *does* exist, what it returns, what it *should* return, what joins are missing, what aggregates are wrong.
> **Source read:** `feature/teacher/TeacherRouting.kt`, `TeacherRoutingTasks.kt`, `TeacherLeaveRouting.kt`, `TeacherMessagesRouting.kt`, `core/TeacherAccess.kt`, `feature/school/SchoolTimetableRouting.kt`, `feature/school/TeacherAssignmentRouting.kt`.
> **Law:** Diagnose before prescribing. Mark every gap precisely. No designing around missing infra — name what must be built.
> **Branch:** `backend-by-abuzar_v1.0.3`

---

## 0. Executive Summary

The teacher backend is a thin, honest CRUD surface that **correctly enforces tenant + assignment scoping** (this is its genuine strength) but is crippled by three structural problems inherited from the schema (Doc 01) and one of its own making:

1. **Scoping leans on a name-string fallback.** `TeacherAccess.teacherAssignmentsFor` matches assignments by `teacher_id == me` **OR** `teacher_name == my full name (case-insensitive)`. The name fallback is a correctness *and* security smell (D-TSA-1/2). It exists only because `teacher_id` is routinely null.
2. **Every roster/scope query is an in-memory string filter, not a SQL join.** `studentsForAssignment`, `avgAttendanceFor`, the assessments/syllabus list filters all `.selectAll()` then `.filter { ClassNaming.sameClassSection(...) }` in Kotlin. This is O(whole-school) per call and cannot use indexes.
3. **Aggregates are recomputed per-request with N+1 queries.** `/home` loops assignments issuing a separate `dbQuery` per class for attendance, plus a per-assessment subquery for pending marks. `/classes` issues 3 `dbQuery` calls *per class*. `/homework` issues a submission-count `dbQuery` *per homework row*.
4. **Whole features have no endpoint at all:** teacher self check-in, per-class student roster, student profile-as-seen-by-teacher, homework submission detail/review, homework due-date extension, scheduled-test surfacing, syllabus unit creation, attendance for a back-date with the correct historical roster, marks "absent" handling.

The backend is **safe but shallow**. The rebuild keeps its scoping discipline, replaces string-filtering with real joins (post-schema-migration), and adds the missing endpoints.

---

## 1. Scoping & auth core — `TeacherAccess.kt`

### 1.1 What exists
- `requireTeacherContext()` — resolves role from `app_users` (not the JWT claim — **good**, a forged claim can't widen access), rejects deactivated accounts, requires a `school_id`. Returns `TeacherContext(userId, schoolId, role, fullName)`.
- `TEACHER_ROLES = {teacher, school_admin, admin}` — admins are treated as super-teachers who see *every* active assignment in the school.
- `teacherAssignmentsFor(ctx)` — the scope resolver.
- `requireOwnedAssignment(ctx, classId)` — resolves a client `class_id` (= a TSA row id) to a trusted `OwnedAssignment`, asserting ownership (400/404/403).

### 1.2 Defects

- **B-AUTH-1 (Critical) — name-string ownership fallback.**
  ```kotlin
  val byId = r[TSA.teacherId] == ctx.userId
  val byName = r[TSA.teacherName]?.equals(ctx.fullName, ignoreCase = true) == true
  byId || byName
  ```
  Root cause: D-TSA-2 (nullable `teacher_id`). Effect: a teacher named "Sunita Sharma" owns *every* assignment whose `teacher_name` text is "sunita sharma" — including another physical person's. And renaming a teacher silently re-shuffles ownership. **Fix is in the schema (make `teacher_id` NOT NULL + backfill), then delete the `byName` branch.**

- **B-AUTH-2 (High) — admins see everything with no audit of "acting as."** `privileged = role in {school_admin, admin}` short-circuits all ownership checks. Fine for standing in, but writes (attendance/marks) are then attributed to the admin's `marked_by`/`entered_by` with no "on behalf of" trail.

- **B-AUTH-3 (Med) — `requireOwnedAssignment` resolves only ONE assignment (one subject).** A teacher who teaches 6-A for two subjects has two TSA rows = two `class_id`s. The portal's class picker therefore shows "6-A · Maths" and "6-A · Science" as *separate cards*. Attendance, which is class-level not subject-level (D-ATT-3), is then keyed to whichever subject-assignment the teacher happened to pick — conceptually wrong.

---

## 2. `GET /api/v1/teacher/home`

### 2.1 Should return
Today's date-aware glance: teacher + school name, today's periods (real timetable), counts that are *actionable* (pending attendance, pending marks, homework needing attention), and task cards that deep-link to the exact unfinished action. Should reflect the teacher's *self check-in* state (have they checked in today?).

### 2.2 Currently returns (`TeacherRouting.kt` 199–308)
`TeacherHomeData { teacher_name, school_name, classes_today, pending_attendance, pending_marks, homework_due, today_periods[], tasks[] }`.

### 2.3 Defects
- **B-HOME-1 (Critical) — N+1 attendance loop.** For each assignment, a separate `dbQuery` checks if any attendance row exists for that `grade` today. A teacher with 8 assignments = 8 round-trips just for the pending count.
- **B-HOME-2 (High) — `pending_attendance` counts by SUBJECT-assignment, double-counting class-level attendance.** Because attendance is class-level (one daily row per student) but assignments are per-subject, a teacher of 6-A Maths + 6-A Science shows "2 pending attendance" for what is one homeroom mark. The grade key `"${className}-${section}"` is shared, so it's checked twice.
- **B-HOME-3 (High) — `homework_due` counts homework due *today or later*** (`dueDate >= today`), labelled "Homework due." That is "homework still open," not "homework needing my attention." The actionable number a teacher wants is "homework whose due date passed and that I haven't reviewed/graded" — which the backend cannot compute (no late/graded logic, D-HW-3).
- **B-HOME-4 (High) — `tasks[]` is fabricated uniformly as "Mark attendance" for every assignment**, `is_done=false` always. It never reflects marks-pending or syllabus-pending, never flips to done, and (per B-HOME-2) duplicates per subject. The Home screen's "Today's tasks" is therefore noise, not a worklist.
- **B-HOME-5 (Critical) — No check-in awareness.** Home cannot tell the teacher "you haven't checked in today" because there is no check-in endpoint or table (Doc 01 §4.3). The brief's first-open biometric prompt has no server state to read or write.
- **B-HOME-6 (Med) — `classes_today` falls back to "distinct assignments" when no periods exist.** So on a school with no timetable (the common case — no seed), "classes today" = total assignments regardless of weekday. Misleading.
- **B-HOME-7 (Med) — periods are filtered by `teacher_id == ctx.userId` only**, never cross-checked against TSA, so a stray period row for an unassigned class would still render (D-TT-1).

---

## 3. `GET /api/v1/teacher/classes`

### 3.1 Should return
Each class the teacher owns, with a trustworthy student count, current syllabus %, and a real attendance rate — cheap enough to load instantly.

### 3.2 Currently returns (`TeacherRouting.kt` 311–330)
List of `TeacherClassDto { id, class_name (="<cls>-<sec>"), subject, student_count, is_class_teacher=false, syllabus_progress, avg_attendance }`.

### 3.3 Defects
- **B-CLS-1 (Critical) — 3× N+1 per class.** Each class triggers `studentCountFor` + `syllabusProgressFor` + `avgAttendanceFor`, each its own `dbQuery`, each a full-school `selectAll().filter{}`. 8 classes = 24 full-roster scans per screen load.
- **B-CLS-2 (High) — `is_class_teacher` hardcoded `false`.** No schema concept (D-TSA). The Classes tab can never distinguish the homeroom you own from a subject you visit.
- **B-CLS-3 (High) — `avgAttendanceFor` parses the `grade` composite string** by splitting on the last `-`. Fragile (D-ATT-1) and computed over *all history* (no date window), so "attendance" is a lifetime average, not "this week/month" the Classes tab implies.
- **B-CLS-4 (Med) — one card per (class, subject)**, so the "My Classes" grid shows 6-A three times if you teach it three subjects. There is no class-first grouping.
- **B-CLS-5 (Med) — `syllabusProgressFor` matches subject by exact `eq` but class by `ClassNaming`** — inconsistent matching within one function family.

---

## 4. `GET /api/v1/teacher/profile`

### 4.1 Currently returns (`TeacherRouting.kt` 333–364)
`TeacherProfileData { id, name, username, school_name, subjects[], classes[], photo_url, email, phone }`, deriving subjects/classes from assignments.

### 4.2 Defects
- **B-PROF-1 (Low) — `username` = email ?: phone ?: ""** — fine, but no employee/staff id is surfaced even though `faculty.external_id` exists.
- **B-PROF-2 (Med) — Profile is read-only.** There is no `PATCH /teacher/profile` (photo, phone, notification prefs). The frontend rows "Notification preferences" / "Change password" are inert (Doc 03). Change-password exists globally (`/auth/change-password`) but isn't wired here.

---

## 5. Attendance — `GET/POST /api/v1/teacher/attendance`

### 5.1 Should support
Load a class roster for a date (default today, back-date allowed within policy) with each student pre-filled to their correct default — **present**, except students with an **approved leave** for that date who default to **leave**. Submit upserts. Block/curate re-marking. Handle holidays, cancelled classes, mid-term transfers.

### 5.2 Currently does (`TeacherRoutingTasks.kt` 196–305)
- **GET** `?class_id=&date=`: resolves owned assignment, builds `grade="<cls>-<sec>"`, reads roster via `studentsForAssignment`, overlays any existing `attendance_records` for that date+grade, defaults unmarked to `"present"`.
- **POST**: validates each status ∈ `{present,absent,late}`, upserts one row per entry keyed `(school, date, type=student, person_id)`, sets `grade`/`marked_by`. Notifies parents of absent/late students.

### 5.3 Defects
- **B-ATT-1 (Critical) — Approved leave is never reflected.** No read of `leave_requests` anywhere in the attendance path. A student the teacher *approved* leave for yesterday still defaults to "present." The brief's leave-default requirement is unimplemented; the schema can't even store `leave` status (D-ATT-2).
- **B-ATT-2 (Critical) — No re-mark guard / no "already marked" signal.** GET happily returns existing marks as the defaults; POST silently overwrites them. There's no flag telling the UI "this class+date is already submitted," no lock, no audit of edits. The brief's "what if the class was already marked" has no handled state.
- **B-ATT-3 (High) — Back-dated roster is wrong for transfers.** GET uses the *current* `students` mirror (D-STU-1). Marking 12-March attendance uses today's roster, not March's. A student who left in February is missing; one who joined in April appears.
- **B-ATT-4 (High) — No holiday / cancelled-class awareness.** Nothing checks `holiday_list` / `calendar_events`. A teacher can mark attendance on a declared holiday; the system has no opinion.
- **B-ATT-5 (High) — Subject vs class-level ambiguity.** POST keys on `(school, date, student)` ignoring class entirely — so the *last* class a student is marked in for the day wins the single daily row, and `grade` is overwritten to whichever subject-assignment submitted last (D-ATT-3, B-AUTH-3).
- **B-ATT-6 (Med) — Parent notification fires on every submit, including edits/corrections** → a correction from absent→present still sends nothing, but absent re-sent on every resubmit can spam.
- **B-ATT-7 (Med) — `date` defaulting is client-trusted string;** no validation it isn't a future date, malformed, or outside the academic year.

---

## 6. Marks & assessments — `/marks` + `/assessments`

### 6.1 Should support
Create assessments scoped to the teacher's class+subject (✓), distinguish scheduled vs ad-hoc, surface scheduled tests when their date arrives, enter marks only for created assessments, validate against max + pass marks at entry, support absent-from-exam, publish deliberately, compare over time.

### 6.2 Currently does (`TeacherRoutingTasks.kt` 307–531)
- **GET /assessments** `?class_id=`: lists active assessments for the owned class+subject (subject exact, class via ClassNaming).
- **POST /assessments**: creates `{name, max_marks?=100, exam_date?}` scoped to the owned assignment. `is_published=false`.
- **GET /marks** `?class_id=&exam_id=`: roster with existing scores for the assessment.
- **POST /marks**: upserts scores clamped to `[0, max_marks]`, **then force-sets `is_published=true`** and notifies all class parents.

### 6.3 Defects
- **B-MK-1 (Critical) — Entering any mark auto-publishes to parents irreversibly.** `POST /marks` sets `is_published=true, published_at=now` unconditionally and fires "Results published" to every parent of the class. A teacher saving a partial/draft roster (or a typo) publishes instantly. There is no draft save, no review step, no unpublish. (D-ASMT-3.)
- **B-MK-2 (Critical) — No pass-marks / entry-time validation beyond clamping.** Marks are silently clamped to `[0,max]`. The brief wants validation (out-of-total, minimum passing) *at entry*; the only rule is clamp, applied at *submit*, with no feedback on which entries were clamped.
- **B-MK-3 (High) — Absent-from-exam unrepresentable.** `marks=null` = "not entered." A genuinely absent student cannot be recorded as absent (D-ASMT-5); they look ungraded forever, inflating "pending marks."
- **B-MK-4 (High) — No scheduled-test surfacing.** No `type`, no `calendar_event_id` (D-ASMT-2). Assessments never appear automatically when their date arrives; the teacher must create one manually each time (the exam picker's inline create is the only path — Doc 03).
- **B-MK-5 (High) — `/marks` GET does not verify the assessment belongs to the picked class.** It checks `assessment.school_id == ctx.schoolId` only — not that the assessment's class/section matches the `class_id` the teacher selected. A valid exam_id from a *different* class in the same school loads that other class's roster header but this class's students (roster comes from `asg`). Cross-wiring risk.
- **B-MK-6 (Med) — No "compare over time."** No endpoint returns a student's or class's assessment history/trend. The brief's "comparing performance over time" is absent.
- **B-MK-7 (Med) — `/home` pending_marks is N+1** (per-assessment subquery) and counts an assessment as pending if *any* student lacks a score — including absent students (B-MK-3), so it never reaches zero for a class with an absentee.

---

## 7. Syllabus — `GET /syllabus` + `PATCH /syllabus`

### 7.1 Currently does (`TeacherRoutingTasks.kt` 533–614)
- **GET** `?class_id=&subject=`: lists units for the owned class+subject ordered by `position`, with `overall_progress`.
- **PATCH** `{unit_id, is_covered}`: toggles coverage, sets `covered_on=today`/`covered_by`, re-asserts ownership by matching the unit's class/section/subject against the teacher's assignments.

### 7.2 Defects
- **B-SYL-1 (Critical) — No create/seed path.** There is **no `POST /syllabus` to author units**, and no seed (D-SYL-2). On every real install `syllabus_units` is empty, so GET returns `[]`, the screen is permanently empty, and PATCH has nothing to toggle. The feature is inert end-to-end until someone inserts rows by hand in SQL.
- **B-SYL-2 (High) — PATCH ownership check uses exact `==` on class/section/subject** (not `ClassNaming`), while GET uses `ClassNaming`. So a unit GET can return (matched loosely) but its PATCH can 403 (matched strictly) for the same teacher when labels drift. Inconsistent matcher within the same feature.
- **B-SYL-3 (Med) — No notification / parent visibility toggle**, no "in progress" state (D-SYL-3).

---

## 8. Homework — `GET /homework` + `POST /homework`

### 8.1 Should support
List the teacher's homework with accurate submitted/total, create with description + **attachment** + due date, a **status board** (who submitted / hasn't / late), open & review submissions, **extend due date**, and a hard "no submit past due" rule on the parent side.

### 8.2 Currently does (`TeacherRoutingTasks.kt` 616–711)
- **GET**: active homework authored by the teacher (admins see all), each with `submitted_count` (live count of submission rows) and `total_count` (`studentCountFor`).
- **POST** `{class_id, title, description?, due_date}`: creates, notifies class parents.

### 8.3 Defects
- **B-HW-1 (Critical) — No attachment support.** POST accepts only text (D-HW-1). The frontend has no attach UI either (Doc 03). "Assignment description/attachment" half-built.
- **B-HW-2 (Critical) — No status board endpoint.** GET returns only `submitted_count`/`total_count` *numbers*. There is no `GET /homework/{id}/submissions` returning who submitted, who didn't, who was late, with submission content. The brief's "homework status board" + "open and review submissions" has no backend.
- **B-HW-3 (Critical) — No due-date extension endpoint.** No `PATCH /homework/{id}` to change `due_date`; no extension audit (D-HW-2). The brief mandates extension from the management view.
- **B-HW-4 (Critical) — "No submit past due" is unenforced and lives nowhere.** There is no student/parent submission endpoint in the teacher module at all, and `homework_submissions` rows are created by an unseen path; `status='late'` is never computed against `due_date` (D-HW-3). The rule the brief demands has no enforcement point.
- **B-HW-5 (High) — `submitted_count`/`total_count` N+1 + string-match denominator.** Per homework row: one submission-count `dbQuery` + one full-roster `studentCountFor`. And `total` rides section-drift (D-HW-5).
- **B-HW-6 (Med) — No edit/delete/close** of a homework (only soft `is_active`, never toggled by any route).

---

## 9. Leave — `GET /teacher/leave-requests` + `PATCH /{id}`

### 9.1 Currently does (`TeacherLeaveRouting.kt`)
- **GET** `?status=`: student leave requests in the school routed to the teacher — direct (`teacher_id==me`) or by owned `(class_name,section)` pair; admins see all. Returns `pending_count` + list.
- **PATCH** `{status: Approved|Rejected}`: re-verifies routing, updates, notifies the applicant parent.

### 9.2 Defects
- **B-LV-1 (High) — Every subject teacher of a class is a valid decider** (owned-pair match), not a single class teacher (D-LV-1). No "already decided" concurrency guard beyond last-write-wins; two teachers can flip the decision.
- **B-LV-2 (Critical) — Approving leave does nothing to attendance.** No write of a `leave` attendance row for the leave dates (D-ATT-2). The approval is cosmetic w.r.t. the attendance register.
- **B-LV-3 (Med) — No teacher *own* leave application** from the teacher module (the teacher is only a *decider* of student leave here). A teacher applying for their own leave uses the school table but there's no teacher-side endpoint.

---

## 10. Messaging — `POST /teacher/messages/class` (+ thread routes)

### 10.1 Currently does (`TeacherMessagesRouting.kt`)
- Broadcast to every parent of an owned class (`TeacherClassBroadcastRequest`), plus 1:1 thread reads/sends mirroring the parent side (RA-51).

### 10.2 Defects
- **B-MSG-1 (Med) — Broadcast recipients resolved by class name string** (`NotifyRecipients.parentsOfClass`), inheriting drift; section is optional and often ignored.
- **B-MSG-2 (Low) — No per-student message** from a class context (teacher → one parent about one student) surfaced as a first-class flow.

---

## 11. Admin-side feeders the teacher portal depends on

These are not teacher endpoints but **populate** the teacher's data; their gaps are why the teacher portal is often empty.

- **`TeacherAssignmentRouting.kt`** (`/api/v1/school/teacher-assignments`) — creates TSA rows. **Defect B-FEED-1:** it can write rows with `teacher_id=null` (name only), directly causing B-AUTH-1. It is the upstream of D-TSA-2.
- **`SchoolTimetableRouting.kt`** (`GET /api/v1/school/timetable`) — reads `teacher_periods` school-wide. **Defect B-FEED-2:** there is a *read* for the whole school but the **write path for `teacher_periods` is unclear/absent** in the teacher+school modules surveyed — meaning timetables are entered by some other tool or not at all (hence empty Home periods).
- **No syllabus-unit authoring endpoint anywhere** (B-SYL-1) — the curriculum has no creator.
- **No student-roster authoring** beyond the read-only mirror (D-STU-1).

---

## 12. Cross-cutting backend problems

- **C-1 — In-memory filtering instead of SQL joins.** `ClassNaming.sameClassSection` is applied in Kotlin after `selectAll()`. Correct *given* the schema (Doc 01 X-1), but O(school) and index-blind. Fixing X-1 lets these become indexed joins.
- **C-2 — N+1 everywhere** (`/home`, `/classes`, `/homework`). Each screen load fans out into per-class/per-row queries.
- **C-3 — Denormalized writes** (`student_name` into marks, `grade` string into attendance) bake drift into history.
- **C-4 — Implicit side effects** (marks submit → publish + notify). Writes do more than they say.
- **C-5 — No pagination** on any list (rosters, homework, leave). A 60-student class × many assessments is fine now but unbounded.
- **C-6 — String-date comparisons** (`dueDate >= today`) work only by `YYYY-MM-DD` convention; no validation.
- **C-7 — Inconsistent matchers** within feature families (GET ClassNaming vs PATCH exact in syllabus; subject exact vs class loose in classes).

---

## 13. Endpoint gap register (drives Doc 11)

| Endpoint | State | Severity | Defect |
|---|---|---|---|
| `POST /teacher/check-in` | **MISSING** | Critical | No self check-in / biometric (Doc 01 §4.3) |
| `GET /teacher/check-in/today` | **MISSING** | Critical | Home can't read check-in state |
| `GET /teacher/classes/{id}/students` | **MISSING** | Critical | No per-class roster (Doc 09) |
| `GET /teacher/students/{code}` | **MISSING** | High | No student profile-as-teacher (Doc 09) |
| `GET /teacher/homework/{id}/submissions` | **MISSING** | Critical | No status board (Doc 08) |
| `PATCH /teacher/homework/{id}` (extend/edit) | **MISSING** | Critical | No due-date extension (Doc 08) |
| `POST /teacher/syllabus` (create units) | **MISSING** | Critical | Units uncreatable → empty (Doc 08) |
| homework attachments (POST/GET) | **MISSING** | Critical | No file model (Doc 08) |
| scheduled-test surfacing in `/assessments` | **MISSING** | High | No type/calendar link (Doc 07) |
| marks "absent" + draft/publish split | **MISSING** | Critical | Auto-publish, no absent (Doc 07) |
| leave→attendance materialisation | **MISSING** | Critical | Approved leave ignored (Doc 06) |
| `GET /teacher/home` check-in + real tasks | Exists, wrong | High | Fabricated tasks, no check-in (Doc 03) |
| `GET /teacher/classes` class-grouped + class-teacher | Exists, wrong | High | Per-subject dup, hardcoded flag |
| `PATCH /teacher/profile` | **MISSING** | Med | Profile read-only |
| `GET /teacher/assessments/history` | **MISSING** | Med | No trend/compare (Doc 07) |
| attendance "already marked" + holiday guard | Exists, wrong | High | No lock/holiday awareness (Doc 06) |
| `POST /teacher/leave` (own leave) | **MISSING** | Med | Teacher can't apply own leave here |

---

*End of 02_BACKEND_AUDIT.md*
