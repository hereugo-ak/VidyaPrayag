# 01 — SCHEMA AUDIT

> **Scope:** Every data entity that touches a teacher's daily workflow.
> **Source of truth read:** `server/src/main/kotlin/com/littlebridge/vidyaprayag/db/Tables.kt` (the Exposed model the running Ktor backend actually binds to), cross-checked against `docs/backend/sql/02_teacher_schema.sql`, `docs/db/migration_002_segmentation_geo_assignments.sql`, `docs/db/vidyasetu_schema.sql`.
> **Law:** Problems first, root cause before solution. Nothing here is presumed correct.
> **Branch:** `backend-by-abuzar_v1.0.3`

---

## 0. Executive Summary — the schema in one breath

The teacher vertical sits on top of a schema that was **grown additively, never designed as a graph**. The defining structural decision — and the source of the majority of teacher bugs — is that **the teacher's operational tables are denormalized to free-text `class_name` / `section` / `subject` strings instead of foreign keys**. There is exactly one structured relationship table, `teacher_subject_assignments` (TSA), and even it carries denormalized text columns and *nullable* FKs that are routinely null in practice.

Consequences that cascade into every screen:

1. **No referential integrity between a teacher's assignment and the data they create.** An `assessments` row, a `homework` row, a `syllabus_units` row, and a `teacher_periods` row are all linked to a class only by *string equality on `class_name` + `section` + `subject`*. Nothing in the database guarantees those strings match the canonical `school_classes` / `school_subjects` rows, or each other.
2. **Class-identity drift is a data-integrity problem, not just a display problem.** "Grade 4" vs "4" vs "Class IV", "A" vs "a" vs "". The backend papers over this with a runtime `ClassNaming.sameClassSection()` matcher (see Doc 02), but the *database* stores the drift permanently.
3. **The student roster is a read-only mirror (`students`) keyed by `student_code` (TEXT), not by UUID FK.** Every marks/attendance/homework-submission row references a student by this text code with **no FK constraint**, so a typo or a re-coded student silently orphans academic history.
4. **Several teacher features have NO backing schema at all** — most critically **teacher self check-in (biometric or otherwise)**, **homework attachments**, **homework due-date extension / late tracking**, **scheduled-vs-surprise assessment typing**, and **per-class roster detail surfaced to the teacher**.
5. **No seed data exists** for `teacher_periods`, `syllabus_units`, or `assessments` (verified against `Seed.kt` / `DemoSeed.kt`). A freshly provisioned teacher therefore opens an app where the schedule, syllabus, and marks planes are *structurally* empty on day one — indistinguishable from "broken."

The teacher vertical does not need a thousand small patches. It needs a **canonical class-assignment graph** with real foreign keys, and a small number of net-new tables for the features that have no home today.

---

## 1. Entity inventory — every table that touches a teacher

| Table | Role in teacher workflow | Key shape | Integrity health |
|---|---|---|---|
| `app_users` | The teacher's identity + login (`role='teacher'`), `school_id` scope, `must_change_password` first-login gate | UUID PK | **OK** |
| `schools` | Tenant; `name` shown in greeting/profile | UUID PK | OK |
| `school_classes` | Canonical class list (`code`, `name`, `sections` JSON) | UUID PK | **Under-used** — almost nothing FKs to it |
| `school_subjects` | Canonical subjects, **per class** (`class_id`, `sub_name`) | UUID PK | **Under-used** + legacy free-text `teacher_assigned` |
| `teacher_subject_assignments` (TSA) | **The one assignment graph**: who teaches what subject to which class+section | UUID PK | **Weak** — nullable FKs, dominant uniqueness on `teacher_name` text |
| `students` | Read-only roster mirror; the population a teacher marks/grades | UUID PK, `student_code` UNIQUE TEXT | **Weak** — referenced by text code, no FK inbound |
| `attendance_records` | Student attendance the teacher marks (`type='student'`) AND faculty attendance (`type='faculty'`) | UUID PK | **Weak** — `person_id` TEXT, `grade` TEXT free-string |
| `assessments` | Teacher-authored test/exam definition | UUID PK | **Weak** — class/subject are text, no FK |
| `assessment_marks` | Per-student score for an assessment | UUID PK | **Weak** — `student_id` TEXT, no FK to students |
| `syllabus_units` | Chapter/topic coverage per class+subject | UUID PK | **Weak** — class/subject text, no FK; no curriculum source |
| `homework` | Teacher assignment for a class+subject | UUID PK | **Weak** — class/subject text; **no attachment, no extension** |
| `homework_submissions` | Per-student submission | UUID PK | **Weak** — `student_id` TEXT; **status enum incomplete**; no due-date awareness |
| `teacher_periods` | Weekly timetable (the workflow backbone) | UUID PK | **Weak** — class/subject text; **no FK to TSA**; no period-number; no academic-year link |
| `leave_requests` | Student leave routed to the class teacher | UUID PK | **Partial** — routing columns exist but population is fragile |
| `announcements` | Teacher broadcasts (segmented by `audience_type`) | UUID PK | OK for broadcast; not teacher-scoped storage |
| `message_threads` / `messages` | Teacher ↔ parent messaging | UUID PK | OK (RA-51 model) |
| `notifications` | Cross-user notification spine the teacher writes into | UUID PK | OK |
| `calendar_events` / `academic_years` | School academic calendar (VP-CAL) the teacher *reads* | UUID PK | OK but **not joined to teacher data** |
| `exam_results` | School-admin string-scored results (parallel to `assessments`) | UUID PK | **Duplication risk** — two marks models coexist |
| **(missing)** `teacher_check_ins` | Teacher's own daily check-in / biometric attendance | — | **DOES NOT EXIST** |
| **(missing)** `homework_attachments` | Files attached to homework / submissions | — | **DOES NOT EXIST** |
| **(missing)** `syllabus_curriculum` | The master curriculum units are seeded from | — | **DOES NOT EXIST** |

---

## 2. The assignment graph — `teacher_subject_assignments` (TSA)

This is the spine of teacher scoping. Everything ("which classes can I touch") flows from it.

### 2.1 What exists (`Tables.kt` lines 282–300, migration_002 lines 57–76)

```
teacher_subject_assignments
  id           uuid PK
  school_id    uuid   NOT NULL        -- (no FK declared in migration_002)
  class_id     uuid   NULL            -- FK school_classes.id  «OPTIONAL pre-migration»
  class_name   text   NOT NULL        -- denormalised display
  section      varchar(8) DEFAULT 'A'
  subject_id   uuid   NULL            -- FK school_subjects.id «OPTIONAL»
  subject      text   NOT NULL        -- denormalised display
  teacher_id   uuid   NULL            -- FK faculty.id / app_users.id «AMBIGUOUS»
  teacher_name text   NULL            -- display fallback
  is_active    boolean DEFAULT true
  UNIQUE (school_id, class_name, section, subject, teacher_name)   -- ux_tsa_unique
```

### 2.2 Root-cause defects

- **D-TSA-1 — Uniqueness keyed on `teacher_name` (a display string), not `teacher_id`.** `ux_tsa_unique = (school_id, class_name, section, subject, teacher_name)`. Two consequences:
  - A teacher whose name is edited creates a *new* assignment identity → their old assessments/homework/syllabus (matched by class/subject text) appear to belong to "someone else," and `teacherAssignmentsFor` (which falls back to name match, Doc 02) silently changes what they own.
  - Two teachers with the same display name in one school **collide** and cannot both be assigned the same class+subject.
  - **Correct:** uniqueness must be `(school_id, class_id, section, subject_id, teacher_id)` once FKs are populated.

- **D-TSA-2 — `teacher_id` is nullable AND ambiguous (faculty.id *or* app_users.id).** The comment says "FK faculty.id / app_users.id." The teacher portal authenticates as `app_users.id`. The admin provisioning flow may write `faculty.id`. There is no constraint forcing one. When `teacher_id` is null, scoping degrades to **name-string matching** (`TeacherAccess.teacherAssignmentsFor`), which is the single most dangerous line in the teacher backend.
  - **Correct:** `teacher_id` must be NOT NULL and FK to `app_users.id` exactly. Provisioning must materialise the `app_users` row first and write its id.

- **D-TSA-3 — `class_id` / `subject_id` nullable and unenforced.** They are described as "optional pre-migration" but never backfilled. As long as they are null, the database cannot answer "which subjects exist for this class" via join — the app re-derives it from text. The canonical `school_classes` / `school_subjects` tables are therefore **decorative**.
  - **Correct:** backfill + make NOT NULL with real FKs; drop the denormalized `class_name`/`subject` (or keep them as a *generated/trigger-maintained* mirror, never the source of truth).

- **D-TSA-4 — No FK to `schools`.** `school_id` has no declared FK in migration_002 (the teacher-schema FKs in `02_teacher_schema.sql` *do* cascade to schools, but TSA's own migration does not). A deleted school can orphan assignments.

### 2.3 What's missing

- **No "class teacher" flag.** `TeacherClassDto.is_class_teacher` is hardcoded `false` (Doc 02/03). There is no `is_class_teacher` / `is_primary` column on TSA, so the concept of a *form teacher* (who owns the homeroom, sees the full roster, receives student leave) cannot be expressed. **This is required** for the Classes tab (Doc 09) and leave routing (Doc 06/02).
- **No period/timetable link.** TSA and `teacher_periods` are independent islands; a period for a class+subject the teacher is *not* assigned to is structurally possible.

---

## 3. The student roster — `students` and the text-code coupling

### 3.1 What exists (`Tables.kt` 446–460)

```
students
  id            uuid PK
  school_id     uuid NOT NULL
  student_code  text UNIQUE        -- the join key EVERYTHING uses
  full_name     text
  class_name    text
  section       text DEFAULT 'A'
  roll_number   text
  parent_phone  text NULL
  profile_photo_url text NULL
  is_active     boolean DEFAULT true
```

### 3.2 Root-cause defects

- **D-STU-1 — `students` is described as a "read-only mirror — operational writes happen elsewhere."** But there is no documented "elsewhere" in this repo's teacher/admin flow that authoritatively owns student lifecycle (transfers, promotions, mid-term joins). The teacher portal's roster is whatever happens to be in this mirror. **Mid-term transfer, promotion, and section change have no modeled event** — `is_active=false` is the only lever, and it erases the student from *all* historical rosters, not just forward-dated ones.
- **D-STU-2 — Every academic child-record references `student_code` (TEXT) with no FK.** `assessment_marks.student_id`, `homework_submissions.student_id`, `attendance_records.person_id`, `exam_results.student_id`, `children.student_code` all store the text code. There is **no foreign key** from any of them to `students.student_code`. A re-coded or deleted student leaves orphan marks/attendance that still render (the join is by string).
- **D-STU-3 — `(class_name, section)` here must match `(class_name, section)` on TSA / assessments / homework by string** — the same drift problem as D-TSA-1, now on the population side. The backend's `studentsForAssignment` (Doc 02) filters the *entire* school roster in-memory with `ClassNaming.sameClassSection` because it cannot trust the strings to match.

### 3.3 What's missing

- **No enrollment record with date ranges.** To answer "who was in 6-A on 12 March" (needed for back-dated attendance, Doc 06) you need an `enrollments(student_id, class_id, section, from_date, to_date)` table. Today the roster is a *current snapshot* only — back-dated attendance for a transferred student is impossible to do correctly.
- **No `student_id` UUID FK column** on the child tables (see D-STU-2).

---

## 4. Attendance — `attendance_records`

### 4.1 What exists (`Tables.kt` 429–441)

```
attendance_records
  id         uuid PK
  school_id  uuid NOT NULL
  date       varchar(12)        -- 'YYYY-MM-DD' (string, not DATE)
  type       varchar(16)        -- 'student' | 'faculty'
  person_id  text               -- student_code OR faculty id
  grade      text NULL          -- "<class_name>-<section>" composite string
  status     varchar(16)        -- 'present'|'absent'|'late'  (NO 'leave')
  marked_by  uuid NULL          -- app_users.id (the teacher)
  UNIQUE (school_id, date, type, person_id)   -- ux_att_records_unique
```

### 4.2 Root-cause defects

- **D-ATT-1 — `grade` is a composite free-string `"<class>-<section>"`.** The backend builds it as `"${className}-${section}"` then *parses it back* by splitting on the last `-` (see `avgAttendanceFor`, Doc 02). Any class label containing a hyphen, or any case/format drift, corrupts the round-trip. There is **no FK to a class**.
- **D-ATT-2 — `status` enum has no `leave` value.** The brief (Doc 06) requires students on *approved leave* to default to a `leave` status sourced from `leave_requests`. The column allows only `present|absent|late` (enforced at the route as `VALID_ATTENDANCE`). **There is no join, column, or value that connects an approved `leave_request` to that day's attendance row.** This is a hard schema gap.
- **D-ATT-3 — The unique key is `(school_id, date, type, person_id)` — it deliberately omits `grade`.** A student belongs to one school-wide attendance row per day regardless of class. For a *subject-teacher* model where the same student is "present in period 2, absent in period 5," this schema **cannot represent period-level attendance** — it is a single daily homeroom mark. The product must decide: daily homeroom attendance (current) vs per-period. The schema only supports the former, but the teacher portal is built around *subject* assignments, creating a conceptual mismatch (Doc 06 resolves this).
- **D-ATT-4 — `date` stored as `varchar(12)` not `DATE`.** String date columns appear across the whole teacher schema (`exam_date`, `due_date`, `covered_on`, `date_from/to`). This blocks date-range queries, ordering correctness across formats, and DB-level "today" defaults; it forces every comparison into string-lexical territory (which only works because of the rigid `YYYY-MM-DD` convention — one malformed write breaks ordering).

### 4.3 What's missing

- **Teacher self check-in has NO table.** `type='faculty'` rows *could* hold it, but **no teacher endpoint ever writes a faculty row** (verified — Doc 02). There is no `checked_in_at` timestamp, no device/biometric audit, no geo/IP. The brief (Doc 06) requires a biometric check-in on first app open; **none of the supporting schema exists.**

---

## 5. Assessments & marks — `assessments` + `assessment_marks`

### 5.1 What exists (`Tables.kt` 693–727; `02_teacher_schema.sql` 60–96)

```
assessments
  id, school_id (FK schools CASCADE), teacher_id (FK app_users SET NULL),
  class_name text, section varchar(8), subject text,
  name text, max_marks int DEFAULT 100,
  exam_date varchar(12) NULL,
  is_active bool, is_published bool, published_at ts
  INDEX (school_id, class_name, section, subject)

assessment_marks
  id, assessment_id (FK assessments CASCADE),
  student_id text, student_name text,    -- denormalised name
  marks double NULL, entered_by uuid (FK app_users SET NULL)
  UNIQUE (assessment_id, student_id)
```

### 5.2 Root-cause defects

- **D-ASMT-1 — class/subject are text, not FKs (same drift family).** An assessment "belongs to" a class only by string. The list endpoint must re-filter with `ClassNaming.sameClassSection` (Doc 02).
- **D-ASMT-2 — No assessment *type*.** The brief (Doc 07) distinguishes **scheduled tests** (tied to the academic calendar, surfacing automatically when their date arrives) from **surprise/class tests** (ad-hoc). The schema has only `name` + nullable `exam_date`. There is no `type`, no `is_scheduled`, **no link to `calendar_events`** — so "scheduled tests surface automatically" is unbuildable today.
- **D-ASMT-3 — `is_published` is auto-set as a side-effect of entering marks** (route forces `is_published=true` on any marks submit, Doc 02). There is no *draft → review → publish* lifecycle column set; publish is implicit and irreversible from the teacher UI. A mistakenly-entered mark is published to parents instantly.
- **D-ASMT-4 — No grading rules.** `max_marks` exists, but there is **no `pass_marks`, no weightage, no grade-band model.** The brief (Doc 07) requires "minimum passing" validation at entry — there is nowhere to store the threshold.
- **D-ASMT-5 — `assessment_marks` has no `is_absent` / `is_exempt`.** A student who missed a test can only be represented as `marks = null` (= "not entered yet") — indistinguishable from a teacher who simply hasn't typed the score. Absent-from-exam vs not-yet-graded collapse into one state.
- **D-ASMT-6 — Two parallel marks models.** `exam_results` (school-admin, string `score`) and `assessments`/`assessment_marks` (teacher, numeric) both exist and are **not reconciled**. The same test can live in both with different shapes. Downstream (parent academics) must guess which to trust.

### 5.3 What's missing

- **No `assessment_type` enum, no `calendar_event_id` FK, no `pass_marks`, no `is_absent` flag on marks, no marks audit/history.**

---

## 6. Syllabus — `syllabus_units`

### 6.1 What exists (`Tables.kt` 734–746)

```
syllabus_units
  id, school_id (FK schools CASCADE),
  class_name text, section varchar(8), subject text,
  title text, position int,
  is_covered bool, covered_on varchar(12), covered_by uuid (FK app_users SET NULL)
  INDEX (school_id, class_name, section, subject, position)
```

### 6.2 Root-cause defects

- **D-SYL-1 — Units are per `(class, section)` text tuple, not per *curriculum*.** Every section re-creates its own unit rows. There is **no master curriculum** (a CBSE/ICSE/state-board chapter list) that sections instantiate from. So a school with 6-A, 6-B, 6-C must triplicate "Chapter 1: Integers." Nothing keeps them consistent.
- **D-SYL-2 — No "who seeds the units" path.** There is no admin/teacher endpoint to *create* units and **no seed** (verified). `syllabus_units` is empty on a fresh install → the Syllabus plane is permanently empty (Doc 03). The toggle works only if units somehow already exist.
- **D-SYL-3 — Boolean `is_covered` is the entire state model.** No "in progress," no partial coverage, no planned-date vs covered-date, no period link. The brief (Doc 08) wants "mark covered in seconds" — the model supports that, but offers no richer planning the schema could otherwise enable.
- **D-SYL-4 — Section drift again.** Coverage in 6-A vs "6"-vs-"Grade 6" stored separately; progress can fracture across label variants.

---

## 7. Homework — `homework` + `homework_submissions`

### 7.1 What exists (`Tables.kt` 754–781)

```
homework
  id, school_id (FK schools CASCADE), teacher_id (FK app_users SET NULL),
  class_name text, section varchar(8), subject text,
  title text, description text DEFAULT '', due_date varchar(12) NOT NULL,
  is_active bool
  INDEX (school_id, class_name, section, subject)

homework_submissions
  id, homework_id (FK homework CASCADE),
  student_id text, status varchar(16) DEFAULT 'submitted',  -- submitted|graded|late
  submitted_at ts
  UNIQUE (homework_id, student_id)
```

### 7.2 Root-cause defects

- **D-HW-1 — No attachment model.** The brief (Doc 08) requires "assignment description/attachment" and "open and review submissions." Neither `homework` nor `homework_submissions` has a file/url column. There is a generic `school_media` table but nothing links it to homework. **Attachments are unbuildable today.**
- **D-HW-2 — No due-date extension support.** The brief mandates "students cannot submit past the due date, but the teacher can extend the due date." `homework` has a single `due_date` and no `extended_due_date` / `original_due_date` / extension audit. Extending = silently overwriting `due_date`, losing the original and any record of who/when.
- **D-HW-3 — `submissions.status` enum is incomplete and not enforced against the due date.** Values are `submitted|graded|late`, but the *server never computes `late`* — submissions are not compared to `homework.due_date` (Doc 02). There is also no `not_submitted` row (absence of a row = not submitted), so "who hasn't submitted" must be derived by anti-joining the roster, and no `grade`/`feedback` column exists despite the `graded` status.
- **D-HW-4 — No per-submission content** (text answer, marks, teacher feedback). "Review submissions" (Doc 08) has nothing to review — only a status flag and timestamp.
- **D-HW-5 — `total_count` is computed live from `students` by class string** (Doc 02), inheriting all the section-drift fragility. The denominator of "submitted / total" is only as correct as the string match.

---

## 8. The timetable — `teacher_periods`

### 8.1 What exists (`Tables.kt` 789–801; `02_teacher_schema.sql` 162–178)

```
teacher_periods
  id, school_id (FK schools CASCADE), teacher_id (FK app_users CASCADE),
  weekday int (1=Mon..7=Sun),
  start_time varchar(8) "HH:mm", end_time varchar(8) "HH:mm",
  class_name text, section varchar(8), subject text,
  room text DEFAULT '', position int
  INDEX (teacher_id, weekday, position)
```

### 8.2 Root-cause defects

- **D-TT-1 — Periods are NOT linked to the assignment graph (TSA).** A period stores its own `class_name`/`section`/`subject` text, with **no `assignment_id` FK**. So a teacher's timetable can list a class+subject they are not actually assigned to teach, and the two sources of "what do I teach" (periods vs TSA) can disagree. Doc 05 treats this as the central schedule defect.
- **D-TT-2 — No period number / ordering beyond `position`.** No "Period 3" label, no school-wide bell schedule, no break/lunch rows. `position` is a raw int with no semantics.
- **D-TT-3 — Recurring-only, no date exceptions.** The model is a pure weekly recurrence keyed by `weekday`. There is **no link to `calendar_events`/`holiday_list`**, so the timetable cannot natively know a Monday is a holiday or that a period is cancelled for an exam (Doc 05). The client must layer that itself.
- **D-TT-4 — No academic-year link.** `calendar_events` and `academic_years` exist, but `teacher_periods` has no `academic_year_id`. A new year cannot cleanly supersede last year's timetable.
- **D-TT-5 — No "class teacher" vs "subject period" distinction**, and no link to a *room/resource* table (room is free text).

---

## 9. Leave routing — `leave_requests` (teacher-facing slice)

### 9.1 What exists (`Tables.kt` 536–561)

Relevant columns: `requester_role` (`student|teacher`), `class_id` (FK school_classes, nullable), `class_name`, `section`, `teacher_id` (FK app_users, the routed-to teacher, nullable), `child_id`, `parent_id`, `status` (`Pending|Approved|Rejected`), `actioned_by/at`.

### 9.2 Root-cause defects

- **D-LV-1 — Routing depends on nullable `teacher_id` OR `(class_name, section)` text match.** A student leave is shown to a teacher if `teacher_id == me` **or** the `(class_name, section)` is in the teacher's owned pairs (Doc 02). With no "class teacher" concept (D-TSA-defects), *every subject teacher of that class* sees and can decide the leave — there is no single responsible owner. Two teachers can approve/reject the same request (last write wins, no concurrency guard).
- **D-LV-2 — Approved leave does not flow into attendance.** D-ATT-2: there is no mechanism (column, trigger, or join) that turns an approved leave into a `leave`-status attendance row. The brief explicitly requires this.

---

## 10. Cross-cutting structural problems

- **X-1 — Free-text class identity is the master defect.** `class_name` + `section` strings appear as the join key on TSA, students, assessments, syllabus_units, homework, teacher_periods, attendance(`grade`), exam_results, leave_requests. The canonical `school_classes`/`school_subjects` UUIDs are barely referenced. **This is the single change that fixes the most bugs.**
- **X-2 — Student identity by TEXT code with no FKs.** Second-worst. Orphans every academic record on re-code/delete.
- **X-3 — String dates everywhere.** `varchar(12)` instead of `DATE`. Blocks ranges, ordering guarantees, DB defaults.
- **X-4 — Denormalized display columns treated as source of truth** (`teacher_name`, `student_name`, `class_name`, `subject`). When the underlying entity is renamed, the copies drift.
- **X-5 — No seed data for the teacher operational tables** → fresh teacher = empty everything (Doc 03 persona impact).
- **X-6 — Two marks models** (`assessments` vs `exam_results`) and **two calendar models** (`academic_calendar` legacy vs `calendar_events` VP-CAL) coexist unreconciled.

---

## 11. Correct target schema (what it SHOULD look like)

> These are the structural targets the rebuild sequence (Doc 11) will migrate toward. Each is additive-then-cutover where possible to protect production (AUTO_CREATE_TABLES is OFF in prod).

### 11.1 Promote the assignment graph to real FKs
```
teacher_subject_assignments
  teacher_id   uuid NOT NULL  REFERENCES app_users(id) ON DELETE CASCADE   -- was nullable/ambiguous
  class_id     uuid NOT NULL  REFERENCES school_classes(id) ON DELETE CASCADE
  subject_id   uuid NOT NULL  REFERENCES school_subjects(id) ON DELETE CASCADE
  section      varchar(8) NOT NULL
  is_class_teacher boolean NOT NULL DEFAULT false      -- NEW: the form-teacher flag
  academic_year_id uuid REFERENCES academic_years(id)  -- NEW
  UNIQUE (class_id, section, subject_id, teacher_id)    -- replaces teacher_name uniqueness
  -- class_name/subject become trigger-maintained mirrors, never authoritative
```

### 11.2 Give students a stable UUID join + enrollment history
```
-- add to every child table: student_uuid uuid REFERENCES students(id)
-- backfill from student_code, then make NOT NULL, then keep student_code as a display mirror
enrollments (NEW)
  id, student_id uuid FK students, class_id uuid FK school_classes,
  section varchar(8), from_date date, to_date date NULL, is_active bool
  -- answers "who was in this class on date X" for back-dated attendance
```

### 11.3 Attendance: typed status, real class FK, real DATE, period option
```
attendance_records
  date         date NOT NULL                       -- was varchar
  class_id     uuid REFERENCES school_classes(id)  -- replaces 'grade' string
  section      varchar(8)
  status       varchar(16)  CHECK in ('present','absent','late','leave','holiday')  -- + 'leave'
  source       varchar(16) DEFAULT 'teacher'       -- teacher|auto_leave|import
  -- (optional, if per-period chosen) period_id uuid REFERENCES teacher_periods(id)
```
```
teacher_check_ins (NEW — the missing self check-in table)
  id, school_id FK, teacher_id FK app_users, date date,
  checked_in_at timestamptz, method varchar(16) ('biometric'|'device'|'manual'),
  device_id text, platform varchar(16), latitude double, longitude double,
  UNIQUE (school_id, teacher_id, date)
```

### 11.4 Assessments: type, calendar link, pass marks, absent flag, lifecycle
```
assessments
  class_id uuid FK, subject_id uuid FK,             -- real FKs
  type varchar(16) ('scheduled'|'class_test'|'surprise') NOT NULL
  calendar_event_id uuid REFERENCES calendar_events(id) NULL  -- scheduled tests
  max_marks int, pass_marks int NULL,
  status varchar(16) ('draft'|'scheduled'|'in_progress'|'published') -- explicit lifecycle
assessment_marks
  student_id uuid FK students,
  marks double NULL, is_absent boolean DEFAULT false, is_exempt boolean DEFAULT false
```

### 11.5 Syllabus: master curriculum + per-section instantiation
```
curriculum_units (NEW)
  id, school_id FK, class_id FK, subject_id FK, title, position
  -- the canonical chapter list, authored once per class+subject
syllabus_units
  curriculum_unit_id uuid REFERENCES curriculum_units(id)  -- instance points at master
  section varchar(8), is_covered bool, covered_on date, covered_by uuid FK
```

### 11.6 Homework: attachments, extension audit, real submission content
```
homework
  class_id FK, subject_id FK,
  due_date date NOT NULL, original_due_date date NULL, extended_by uuid FK, extended_at ts
homework_attachments (NEW)
  id, homework_id FK, url, kind, size_bytes, uploaded_by FK
homework_submissions
  student_id uuid FK students,
  status varchar(16) ('not_submitted'|'submitted'|'late'|'graded'),
  content text NULL, grade text NULL, feedback text NULL,
  submitted_at ts, graded_by uuid FK, graded_at ts
homework_submission_files (NEW)  -- student-attached files
```

### 11.7 Timetable: bind to the assignment graph + the calendar
```
teacher_periods
  assignment_id uuid REFERENCES teacher_subject_assignments(id) ON DELETE CASCADE  -- NEW, replaces text class/subject
  academic_year_id uuid FK
  period_no int NULL, kind varchar(12) DEFAULT 'class'  -- class|break|assembly
  start_time time, end_time time          -- real TIME
```

### 11.8 Leave: single responsible owner
```
leave_requests
  routed_to_teacher_id uuid FK app_users  -- the CLASS TEACHER (from TSA.is_class_teacher), not "any subject teacher"
  -- decision locked once status != Pending (guarded in route)
```

---

## 12. Defect register (quick index for Doc 11 sequencing)

| ID | Severity | Table | Defect | Fix class |
|---|---|---|---|---|
| D-TSA-1 | **Critical** | TSA | Uniqueness on `teacher_name` text | Migration |
| D-TSA-2 | **Critical** | TSA | `teacher_id` nullable + ambiguous | Migration + provisioning |
| D-TSA-3 | High | TSA | `class_id`/`subject_id` null, FKs unused | Migration + backfill |
| D-TSA-4 | Med | TSA | No FK to schools | Migration |
| (new) is_class_teacher | **Critical** | TSA | No form-teacher concept | Migration |
| D-STU-1 | High | students | No enrollment/transfer model | New table |
| D-STU-2 | **Critical** | many | Child records ref `student_code` text, no FK | Migration + backfill |
| D-ATT-1 | High | attendance | `grade` composite string, no class FK | Migration |
| D-ATT-2 | **Critical** | attendance | No `leave` status / no leave→attendance link | Migration + logic |
| D-ATT-3 | High | attendance | Cannot represent per-period attendance | Product decision (Doc 06) |
| D-ATT-4 | High | (all) | String dates not DATE | Migration |
| (new) check_ins | **Critical** | — | Teacher self check-in has no table | New table |
| D-ASMT-2 | **Critical** | assessments | No type / no calendar link | Migration |
| D-ASMT-3 | High | assessments | Implicit irreversible publish | Logic + column |
| D-ASMT-4 | High | assessments | No pass_marks | Migration |
| D-ASMT-5 | High | assessment_marks | absent vs not-graded collapse | Migration |
| D-ASMT-6 | Med | exam_results | Duplicate marks model | Reconciliation |
| D-SYL-1 | High | syllabus | No master curriculum | New table |
| D-SYL-2 | **Critical** | syllabus | No way to create units / no seed | New endpoint + seed |
| D-HW-1 | **Critical** | homework | No attachments | New table |
| D-HW-2 | **Critical** | homework | No due-date extension audit | Migration |
| D-HW-3 | High | submissions | `late` never computed; status enum gaps | Logic + column |
| D-HW-4 | High | submissions | No submission content/feedback | Migration |
| D-TT-1 | **Critical** | periods | Not linked to TSA | Migration |
| D-TT-3 | High | periods | No calendar/holiday exceptions | Logic + link |
| D-LV-1 | High | leave | No single owner; double-decide | Migration + guard |
| X-1 | **Critical** | global | Free-text class identity | Migration (the big one) |
| X-5 | High | global | No teacher seed data | Seed |

---

*End of 01_SCHEMA_AUDIT.md*
