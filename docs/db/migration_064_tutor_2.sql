-- =============================================================
-- migration_064_tutor_2.sql
-- AI Tutor 2.0 — Agentic Redesign (AI_TUTOR_2.0_AGENTIC_REDESIGN.md §12)
--
-- New tables:
--   1. tutor_sessions       — per-child per-subject agent sessions
--   2. tutor_review_state   — FSRS spaced-repetition spine
--   3. tutor_mastery        — grounded mastery (marks + practice, never model-invented)
--   4. tutor_misconceptions — class-wide intelligence library
--
-- Also adds nullable topic_id FK to assessments for per-topic score derivation.
--
-- Reused (no new table): ai_jobs, ai_response_cache, ai_usage_log,
--   ai_prompt_templates, curriculum_units, syllabus_progress, assessment_marks,
--   homework_submissions, children, parent_child_links, calendar_events, etc.
--
-- Kill-switch flags: seeded into pews_feature_flags (reuses PEWS kill-switch infra).
-- =============================================================

-- 1. Add nullable topic_id FK to assessments (per-topic score derivation)
ALTER TABLE assessments ADD COLUMN IF NOT EXISTS topic_id UUID REFERENCES curriculum_units(id);
COMMENT ON COLUMN assessments.topic_id IS
  'AI Tutor 2.0: optional FK to curriculum_units for per-topic score derivation. Nullable for backward compat.';

-- 2. tutor_sessions — per-child per-subject agent sessions
CREATE TABLE IF NOT EXISTS tutor_sessions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL REFERENCES schools(id),
    child_id        UUID NOT NULL REFERENCES children(id),
    subject_id      UUID NOT NULL REFERENCES school_subjects(id),
    academic_year_id UUID REFERENCES academic_years(id),
    mode            VARCHAR(16) NOT NULL DEFAULT 'DOUBT',
    intent_class    VARCHAR(64),
    turns           JSONB NOT NULL DEFAULT '[]'::jsonb,
    grounded_refs   JSONB NOT NULL DEFAULT '[]'::jsonb,
    provider_used   VARCHAR(64),
    tokens_used     INTEGER NOT NULL DEFAULT 0,
    cache_hit       BOOLEAN NOT NULL DEFAULT false,
    safety_flag     VARCHAR(32),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_tutor_sessions_child_subject ON tutor_sessions(child_id, subject_id);
CREATE INDEX IF NOT EXISTS idx_tutor_sessions_school ON tutor_sessions(school_id);
CREATE INDEX IF NOT EXISTS idx_tutor_sessions_mode ON tutor_sessions(mode);

-- 3. tutor_review_state — FSRS spaced-repetition spine
CREATE TABLE IF NOT EXISTS tutor_review_state (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL REFERENCES schools(id),
    child_id        UUID NOT NULL REFERENCES children(id),
    topic_id        UUID NOT NULL REFERENCES curriculum_units(id),
    stability       DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    difficulty      DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    due_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    reps            INTEGER NOT NULL DEFAULT 0,
    lapses          INTEGER NOT NULL DEFAULT 0,
    last_grade      INTEGER NOT NULL DEFAULT 0,
    last_reviewed_at TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(child_id, topic_id)
);
CREATE INDEX IF NOT EXISTS idx_tutor_review_due ON tutor_review_state(child_id, due_at);

-- 4. tutor_mastery — grounded mastery (marks + practice, never model-invented)
CREATE TABLE IF NOT EXISTS tutor_mastery (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL REFERENCES schools(id),
    child_id        UUID NOT NULL REFERENCES children(id),
    subject_id      UUID NOT NULL REFERENCES school_subjects(id),
    topic_id        UUID NOT NULL REFERENCES curriculum_units(id),
    mastery         DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    source          VARCHAR(16) NOT NULL DEFAULT 'MARKS',
    attempts        INTEGER NOT NULL DEFAULT 0,
    correct         INTEGER NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(child_id, topic_id)
);
CREATE INDEX IF NOT EXISTS idx_tutor_mastery_child_subject ON tutor_mastery(child_id, subject_id);

-- 5. tutor_misconceptions — class-wide intelligence library
CREATE TABLE IF NOT EXISTS tutor_misconceptions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL REFERENCES schools(id),
    class_id        UUID NOT NULL REFERENCES school_classes(id),
    subject_id      UUID NOT NULL REFERENCES school_subjects(id),
    topic_id        UUID NOT NULL REFERENCES curriculum_units(id),
    child_id        UUID NOT NULL REFERENCES children(id),
    misconception_type  VARCHAR(128) NOT NULL,
    evidence        TEXT NOT NULL DEFAULT '',
    resolved        BOOLEAN NOT NULL DEFAULT false,
    surfaced_to_teacher BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_tutor_misconceptions_class_subject ON tutor_misconceptions(class_id, subject_id);
CREATE INDEX IF NOT EXISTS idx_tutor_misconceptions_child ON tutor_misconceptions(child_id);
CREATE INDEX IF NOT EXISTS idx_tutor_misconceptions_topic ON tutor_misconceptions(topic_id);

-- 6. Seed kill-switch rows for AI Tutor modules
INSERT INTO pews_feature_flags (module_name, is_killed, updated_at)
VALUES
    ('tutor_global',           false, now()),
    ('tutor_sense',            false, now()),
    ('tutor_triage',           false, now()),
    ('tutor_agent',            false, now()),
    ('tutor_act',              false, now()),
    ('tutor_learn',            false, now()),
    ('tutor_ingest',           false, now()),
    ('tutor_teacher_heatmap',  false, now()),
    ('tutor_parent_progress',  false, now()),
    ('tutor_admin_efficacy',   false, now()),
    ('tutor_rag',              false, now())
ON CONFLICT (module_name) DO NOTHING;
