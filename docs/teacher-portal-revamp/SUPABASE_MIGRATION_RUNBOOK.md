# Supabase Migration Runbook — Teacher Portal Rebuild (v1.0.3)

> **Branch:** `backend-by-abuzar_v1.0.3`
> **Audience:** whoever runs SQL in the Supabase SQL Editor for this rebuild.
> **Scope:** the teacher-portal-rebuild migrations, `008` → `017`.

---

## TL;DR — where you are, what's next

You told me you have already run **up to and including
`migration_007_child_link_robustness.sql`** in Supabase.

So the **only files you still need to run** are these, **in this exact order**:

| # | File (in `docs/db/`) | Rebuild task | What it lands |
|---|----------------------|--------------|---------------|
| 1 | `migration_008_enrollments.sql`      | T-001 | `enrollments` — the typed student↔class bridge |
| 2 | `migration_009_tsa_fks.sql`          | T-002 | typed FKs + `is_class_teacher` on `teacher_subject_assignments` |
| 3 | `migration_010_typed_dates.sql`      | T-004 | promotes 6 string date columns → native `date` |
| 4 | `migration_011_periods.sql`          | T-101 | typed/bound/term-scoped `teacher_periods` + `period_exceptions` |
| 5 | `migration_012_holidays_merge.sql`   | T-102 | merges `holiday_list` → `calendar_events(HOLIDAY)` (single source) |
| 6 | `migration_013_teacher_checkins.sql` | T-106a | `teacher_check_ins` (teacher self check-in) |
| 7 | `migration_014_attendance.sql`       | T-201 | typed `attendance_records` (+`leave` state, student/assignment FKs) |
| 8 | `migration_015_assessments.sql`      | T-301 | canonical typed `assessments` + `assessment_marks` model |
| 9 | `migration_016_syllabus.sql`         | T-401 | `curriculum_units` + `syllabus_progress` (template/progress split) |
| 10 | `migration_017_homework.sql`        | T-404 | typed `homework` + attachments + extensions + FK submissions |

**Run them strictly in this numeric order. Do not skip. Do not reorder.**
Each one assumes the previous ones have already landed (e.g. `009` backfills
FKs that point at the `enrollments` table created in `008`; `014` references
both `enrollments` and the assignment FKs from `009`).

---

## How to run each file (the same every time)

1. Open **Supabase → SQL Editor → New query**.
2. Open the migration file from this repo at `docs/db/migration_0XX_*.sql`.
3. **Copy the ENTIRE file** (top comment block included — comments are harmless).
4. Paste into the SQL Editor and press **Run**.
5. Wait for **“Success. No rows returned”** (or a row count). Do **not** start the
   next file until the current one has finished cleanly.
6. Move to the next file in the table above.

> **All ten files are idempotent and guarded** (`IF NOT EXISTS`,
> `ADD COLUMN IF NOT EXISTS`, `INSERT … ON CONFLICT DO NOTHING`, ref-keyed
> backfills). If you’re ever unsure whether a file ran, it is **safe to re-run** —
> it will simply do nothing the second time. They only **add** structure and
> backfill; they do not drop data.

---

## ⚠️ One thing that matters: run SQL **before** deploying the backend

The Ktor backend boots with `AUTO_CREATE_TABLES = OFF` and a
`validateSchema()` gate that **refuses to boot against Postgres if a table the
backend maps is missing.** So the rule is:

> **Apply the SQL in Supabase first, then deploy/restart the backend.**

For this rebuild that means: run all of `008` → `017` in Supabase, *then* point
the `backend-by-abuzar_v1.0.3` server at that database. If you deploy the
backend first, it will log the missing tables and stop — that’s the gate working
as designed, not a bug.

---

## Verify after you finish (optional but recommended)

Run these read-only checks in the SQL Editor after `017`. None of them write
anything; they just confirm the structures exist and the backfills landed.

```sql
-- 008: typed enrollments exist
SELECT count(*) AS enrollments FROM enrollments;

-- 009: TSA now has typed FKs + class-teacher flag
SELECT count(*) AS tsa_rows,
       count(class_id)   AS with_class_id,
       count(subject_id) AS with_subject_id,
       count(*) FILTER (WHERE is_class_teacher) AS class_teachers
  FROM teacher_subject_assignments;

-- 010: these six columns must report type 'date'
SELECT table_name, column_name, data_type
  FROM information_schema.columns
 WHERE (table_name, column_name) IN (
        ('attendance_records','date'),
        ('assessments','exam_date'),
        ('homework','due_date'),
        ('syllabus_units','covered_on'),
        ('calendar_events','start_date'),
        ('calendar_events','end_date'))
 ORDER BY table_name, column_name;

-- 011: periods now time-typed and bindable to an assignment
SELECT count(*) AS periods,
       count(assignment_id)     AS bound_to_assignment,
       count(academic_year_id)  AS term_scoped
  FROM teacher_periods;
SELECT count(*) AS period_exceptions FROM period_exceptions;

-- 012: holidays now live in calendar_events (single source)
SELECT count(*) AS published_holidays
  FROM calendar_events
 WHERE type = 'HOLIDAY' AND status = 'PUBLISHED';

-- 013: teacher self check-in table
SELECT count(*) AS teacher_check_ins FROM teacher_check_ins;

-- 014: attendance is typed + supports 'leave'
SELECT count(*) AS attendance_rows,
       count(student_id)    AS typed_student,
       count(assignment_id) AS scoped_to_assignment,
       count(*) FILTER (WHERE status = 'leave') AS leave_marks
  FROM attendance_records;

-- 015: canonical typed assessments/marks
SELECT count(*) AS assessments,
       count(*) FILTER (WHERE status = 'published') AS published,
       count(pass_marks) AS with_pass_mark
  FROM assessments;
SELECT count(*) AS marks, count(student_ref) AS typed_student
  FROM assessment_marks;

-- 016: syllabus template/progress split
SELECT count(*) AS curriculum_units FROM curriculum_units;
SELECT count(*) AS syllabus_progress FROM syllabus_progress;

-- 017: homework scope + attachments + extensions + typed submissions
SELECT count(*) AS homework_rows,
       count(*) FILTER (WHERE assignment_id IS NOT NULL) AS scoped,
       count(*) FILTER (WHERE assignment_id IS NULL)     AS unresolved
  FROM homework;
SELECT count(*) AS submissions, count(student_uuid) AS with_typed_student
  FROM homework_submissions;
SELECT count(*) AS attachments FROM homework_attachments;
SELECT count(*) AS extensions  FROM homework_extensions;
```

**What “good” looks like:** no errors, all tables resolve, the `010` query
returns `date` for all six rows, and the `unresolved` homework count is small
(rows the heuristic couldn’t map are expected to be a minority; new homework
created via the rebuilt Planner is always typed-scoped).

---

## If something errors mid-run

- **“relation … does not exist”** → you skipped a file or ran out of order. Stop,
  go back, and run the lower-numbered file first. (e.g. `014` failing on
  `enrollments` means `008` hasn’t run.)
- **“column … already exists” / “already exists, skipping”** → harmless; the file
  is idempotent and is no-op’ing a part that already landed. Let it finish.
- **A backfill UPDATE touches 0 rows** → also fine; it means there was nothing
  legacy to migrate (e.g. a fresh DB with no old string-scoped homework).

After a clean run of all ten, deploy the `backend-by-abuzar_v1.0.3` server and
the schema-validation gate should report all mapped tables present and boot.

---

## Quick reference — the full provisioning chain (fresh DB only)

If you are ever provisioning a **brand-new** database from scratch (not your
case today — you’re already at `007`), the complete order is:

```
vidyasetu_schema.sql
migration_001_faculty_and_holiday_list.sql
migration_002_segmentation_geo_assignments.sql
migration_003_leave_workflow_and_two_party_messaging.sql
migration_004_parent_link_guardian_metadata.sql
migration_004_parent_achievements.sql
migration_005_class_normalization_and_student_code_standard.sql
migration_006_parent_link_review_fields.sql
migration_007_child_link_robustness.sql
../backend/sql/02_teacher_schema.sql          # legacy teacher tables
migration_008_enrollments.sql                 # ← teacher-portal rebuild starts here
migration_009_tsa_fks.sql
migration_010_typed_dates.sql
migration_011_periods.sql
migration_012_holidays_merge.sql
migration_013_teacher_checkins.sql
migration_014_attendance.sql
migration_015_assessments.sql
migration_016_syllabus.sql
migration_017_homework.sql
```

For your current task, you only need everything from `008` down. ✅
