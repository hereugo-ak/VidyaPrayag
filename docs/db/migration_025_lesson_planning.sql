-- Migration 025: Lesson Planning — lesson_plans, lesson_plan_templates, lesson_plan_attachments
--
-- Implements LESSON_PLANNING_SPEC.md (P1-20). Three tables:
--   1. lesson_plans             — teacher lesson plans scoped to a TeacherSubjectAssignment (X-1)
--   2. lesson_plan_templates    — reusable templates (separate table, different shape/lifecycle)
--   3. lesson_plan_attachments  — URL-based file/link attachments (follows homework_attachments pattern)
--
-- Depends on: migration_016_syllabus.sql (curriculum_units, syllabus_progress)
-- Run after:  migration_024_conversation_seq_and_nullable_body.sql

-- ── 1. lesson_plans ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS lesson_plans (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id           UUID NOT NULL,
    teacher_id          UUID NOT NULL,
    assignment_id       UUID NOT NULL,
    class_id            UUID NOT NULL,
    section             VARCHAR(8) NOT NULL DEFAULT 'A',
    subject_id          UUID,
    subject_name        TEXT NOT NULL,
    curriculum_unit_id  UUID,
    title               TEXT NOT NULL,
    objectives          TEXT NOT NULL,
    activities          TEXT,
    resources           TEXT,
    assessment_method   TEXT,
    duration_minutes    INTEGER NOT NULL DEFAULT 45,
    homework_id         UUID,
    planned_date        DATE,
    completed_at        TIMESTAMPTZ,
    status              VARCHAR(16) NOT NULL DEFAULT 'planned',
    is_template         BOOLEAN NOT NULL DEFAULT false,
    template_source_id  UUID,
    deleted_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_lesson_plans_assignment
    ON lesson_plans (assignment_id, planned_date);
CREATE INDEX IF NOT EXISTS idx_lesson_plans_school
    ON lesson_plans (school_id, assignment_id);
CREATE INDEX IF NOT EXISTS idx_lesson_plans_calendar
    ON lesson_plans (teacher_id, planned_date, status)
    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_lesson_plans_unit
    ON lesson_plans (curriculum_unit_id)
    WHERE curriculum_unit_id IS NOT NULL;

-- ── 2. lesson_plan_templates ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS lesson_plan_templates (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id           UUID NOT NULL,
    teacher_id          UUID NOT NULL,
    assignment_id       UUID NOT NULL,
    subject_name        TEXT NOT NULL,
    title               TEXT NOT NULL,
    objectives          TEXT NOT NULL,
    activities          TEXT,
    resources           TEXT,
    assessment_method   TEXT,
    duration_minutes    INTEGER NOT NULL DEFAULT 45,
    is_shared           BOOLEAN NOT NULL DEFAULT false,
    deleted_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_lesson_templates_teacher
    ON lesson_plan_templates (teacher_id, assignment_id);
CREATE INDEX IF NOT EXISTS idx_lesson_templates_school
    ON lesson_plan_templates (school_id, is_shared);

-- ── 3. lesson_plan_attachments ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS lesson_plan_attachments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lesson_plan_id  UUID NOT NULL REFERENCES lesson_plans(id) ON DELETE CASCADE,
    url             TEXT NOT NULL,
    filename        TEXT NOT NULL DEFAULT '',
    mime            TEXT NOT NULL DEFAULT '',
    size_bytes      BIGINT NOT NULL DEFAULT 0,
    uploaded_by     UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_lesson_attachments_plan
    ON lesson_plan_attachments (lesson_plan_id);

-- Verification:
-- SELECT table_name FROM information_schema.tables
--   WHERE table_name IN ('lesson_plans', 'lesson_plan_templates', 'lesson_plan_attachments');
-- Expected: 3 rows
