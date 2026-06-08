-- =============================================================================
-- VidyaPrayag — TEACHER VERTICAL SCHEMA (additive, idempotent)
-- Version: 1.0  |  Engine: PostgreSQL (Supabase)
--
-- WHAT THIS FILE DOES
-- -------------------
-- Adds the tables the TEACHER portal needs (master rebuild doc Step 7 / §9 /
-- gap G1) so the `api/v1/teacher/*` routes have somewhere to read/write. These
-- are consumed by the KMP client's `shared/feature/teacher` data layer.
--
--   1.  assessments            — a teacher-authored test/exam for one
--                                class+section+subject, with a max-marks
--                                denominator (the numeric, normalized
--                                counterpart to the string-scored
--                                `exam_results` used by the school-admin
--                                Results screen).
--   2.  assessment_marks       — per-student score for an assessment.
--   3.  syllabus_units         — chapter/topic coverage per class+subject.
--   4.  homework               — assignments authored by a teacher.
--   5.  homework_submissions   — per-student submission rows (for the
--                                submitted/total ratio on homework cards).
--   6.  teacher_periods        — optional weekly timetable (teacher Home
--                                "today's periods"). Empty-safe: no rows = no
--                                periods rendered (never fabricated).
--
-- Attendance is NOT re-modelled here — the teacher portal writes student
-- attendance into the existing `attendance_records` table (type='student',
-- marked_by = teacher's app_users.id).
--
-- ALL `CREATE TABLE` statements use `IF NOT EXISTS` so this file is safe to
-- re-run. Run it in Supabase Dashboard → SQL Editor → New Query → paste → Run.
--
-- ⚠️  PREREQUISITES (RA-63): the canonical path bundles this fragment into
--     scripts/schema-all-in-one-2026-06-07.sql — run THAT (see
--     docs/db/PROVISION.sql / scripts/README-RUN-ORDER.md), not the legacy
--     root "VIDYASETU v2.1" schema (archived to docs/_archive/...ABANDONED.sql).
--     This file depends on `schools`, `app_users`, `students`, and
--     `teacher_subject_assignments` already existing.
--
-- SCOPING MODEL
-- -------------
-- Every teacher endpoint resolves the caller's school from app_users.school_id
-- and constrains class+subject access to the caller's rows in
-- `teacher_subject_assignments` (matched on teacher_id = app_users.id, with a
-- teacher_name fallback for legacy free-text rows). The denormalised
-- class_name / section / subject columns below mirror that table so reads are
-- single-table and fast.
-- =============================================================================


-- =============================================================================
-- SECTION 0 — EXTENSIONS (already enabled by base schema, kept for safety)
-- =============================================================================
CREATE EXTENSION IF NOT EXISTS "pgcrypto";


-- =============================================================================
-- SECTION 1 — assessments  (teacher test/exam definitions)
-- =============================================================================
CREATE TABLE IF NOT EXISTS assessments (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id   UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    teacher_id  UUID REFERENCES app_users(id) ON DELETE SET NULL,
    class_name  TEXT NOT NULL,
    section     VARCHAR(8) NOT NULL DEFAULT 'A',
    subject     TEXT NOT NULL,
    name        TEXT NOT NULL,
    max_marks   INTEGER NOT NULL DEFAULT 100,
    exam_date   VARCHAR(12),                       -- YYYY-MM-DD
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_assessments_school_class_subject
    ON assessments (school_id, class_name, section, subject);


-- =============================================================================
-- SECTION 2 — assessment_marks  (per-student scores)
--
-- marks is NULL until a score is entered. The route clamps it to
-- [0, assessments.max_marks]. One (assessment, student) pair is unique so a
-- re-submit updates in place rather than duplicating.
-- =============================================================================
CREATE TABLE IF NOT EXISTS assessment_marks (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessment_id UUID NOT NULL REFERENCES assessments(id) ON DELETE CASCADE,
    student_id    TEXT NOT NULL,                   -- students.student_code
    student_name  TEXT NOT NULL,
    marks         DOUBLE PRECISION,                -- NULL = not entered yet
    entered_by    UUID REFERENCES app_users(id) ON DELETE SET NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ux_assessment_marks_unique UNIQUE (assessment_id, student_id)
);


-- =============================================================================
-- SECTION 3 — syllabus_units  (chapter/topic coverage)
-- =============================================================================
CREATE TABLE IF NOT EXISTS syllabus_units (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id   UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    class_name  TEXT NOT NULL,
    section     VARCHAR(8) NOT NULL DEFAULT 'A',
    subject     TEXT NOT NULL,
    title       TEXT NOT NULL,
    position    INTEGER NOT NULL DEFAULT 0,
    is_covered  BOOLEAN NOT NULL DEFAULT FALSE,
    covered_on  VARCHAR(12),                       -- YYYY-MM-DD
    covered_by  UUID REFERENCES app_users(id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_syllabus_units_scope
    ON syllabus_units (school_id, class_name, section, subject, position);


-- =============================================================================
-- SECTION 4 — homework  (teacher assignments)
-- =============================================================================
CREATE TABLE IF NOT EXISTS homework (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id   UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    teacher_id  UUID REFERENCES app_users(id) ON DELETE SET NULL,
    class_name  TEXT NOT NULL,
    section     VARCHAR(8) NOT NULL DEFAULT 'A',
    subject     TEXT NOT NULL,
    title       TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    due_date    VARCHAR(12) NOT NULL,              -- YYYY-MM-DD
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_homework_school_class_subject
    ON homework (school_id, class_name, section, subject);


-- =============================================================================
-- SECTION 5 — homework_submissions  (per-student submission rows)
-- =============================================================================
CREATE TABLE IF NOT EXISTS homework_submissions (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    homework_id  UUID NOT NULL REFERENCES homework(id) ON DELETE CASCADE,
    student_id   TEXT NOT NULL,                    -- students.student_code
    status       VARCHAR(16) NOT NULL DEFAULT 'submitted',  -- submitted | graded | late
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ux_homework_submissions_unique UNIQUE (homework_id, student_id)
);


-- =============================================================================
-- SECTION 6 — teacher_periods  (optional weekly timetable)
--
-- weekday is 1..7 (Mon..Sun) to match java.time.DayOfWeek.value. Empty-safe:
-- with no rows the teacher Home renders an honest empty "today's periods".
-- =============================================================================
CREATE TABLE IF NOT EXISTS teacher_periods (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id   UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    teacher_id  UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    weekday     INTEGER NOT NULL,                  -- 1=Mon … 7=Sun
    start_time  VARCHAR(8) NOT NULL,               -- "HH:mm"
    end_time    VARCHAR(8) NOT NULL,               -- "HH:mm"
    class_name  TEXT NOT NULL,
    section     VARCHAR(8) NOT NULL DEFAULT 'A',
    subject     TEXT NOT NULL,
    room        TEXT NOT NULL DEFAULT '',
    position    INTEGER NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_teacher_periods_teacher_weekday
    ON teacher_periods (teacher_id, weekday, position);


-- =============================================================================
-- SECTION 99 — BACK-FILL: tables added to the Ktor backend after
-- `01_supplementary_schema.sql` was last edited (the SQL doc had drifted
-- behind db/Tables.kt). These are created by SchemaUtils on SQLite dev and by
-- AUTO_CREATE_TABLES=true on first Postgres boot, but are spelled out here so a
-- locked-down production DB (no auto-create) can be migrated by hand. All
-- IF NOT EXISTS, so harmless if they already exist.
--   • teacher_subject_assignments   (structured teacher⇄class⇄subject graph)
--   • children, fee_records         (parent ecosystem)
--   • leave_requests, ptm_events, ptm_class_progress,
--     message_threads, messages, exam_results   (school ecosystem)
-- The authoritative column list is db/Tables.kt; run AUTO_CREATE_TABLES=true
-- once on a staging DB and diff if you need the exact generated DDL.
-- =============================================================================
-- (Intentionally left as a documented pointer rather than duplicated DDL, to
--  avoid two diverging sources of truth. db/Tables.kt + SchemaUtils is the
--  generator; this file owns only the teacher-vertical tables above.)


-- =============================================================================
-- DONE — teacher-vertical schema v1.0 loaded.
--
-- CHANGELOG
--   v1.0  (2026-06-06) — assessments, assessment_marks, syllabus_units,
--                        homework, homework_submissions, teacher_periods.
-- =============================================================================
