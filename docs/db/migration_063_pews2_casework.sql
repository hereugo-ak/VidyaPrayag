-- =============================================================================
-- Migration 063 — PEWS 2.0 Casework tables + column expansions
--
-- WHY THIS EXISTS
--   PEWS 2.0 adds managed casework (case files, effectiveness priors, feature
--   flags) and expands existing tables with escalation/SLA/urgency/plan columns.
--   The server's validateSchema() checks for all 3 new tables and will refuse
--   to boot without them.
--
--   Depends on migration_061_pews.sql being applied first.
--
-- HOW TO RUN
--   Supabase -> SQL Editor -> paste this whole file -> Run.
--   Safe to re-run: every CREATE TABLE uses IF NOT EXISTS and ALTER TABLE
--   uses IF NOT EXISTS for columns (via DO block).
--
-- RUN ORDER:
--   ...
--   61. docs/db/migration_061_pews.sql
--   62. docs/db/migration_062_ai_provider_expansion.sql
--   63. docs/db/migration_063_pews2_casework.sql             <-- this file
-- =============================================================================

BEGIN;

-- ── pews_feature_flags ──────────────────────────────────────────────────────
-- Hot-reloadable kill switch. One row per module name; "global" = entire PEWS.
CREATE TABLE IF NOT EXISTS pews_feature_flags (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    module_name         VARCHAR(48) NOT NULL,             -- global|sense|triage|caseworker|act|learn
    is_killed           BOOLEAN     NOT NULL DEFAULT FALSE,
    updated_at          TIMESTAMP   NOT NULL DEFAULT now(),
    updated_by          UUID
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_pews_flags_module
    ON pews_feature_flags (module_name);

-- ── pews_case_files ─────────────────────────────────────────────────────────
-- Structured output from the Caseworker Agent. One row per caseworker run.
CREATE TABLE IF NOT EXISTS pews_case_files (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id           UUID         NOT NULL,
    snapshot_id         UUID,                            -- FK pews_risk_snapshots.id (soft)
    student_code        TEXT         NOT NULL,
    case_file_json      TEXT         NOT NULL,           -- full structured case file (JSON)
    narrative           TEXT,                            -- extracted for querying
    urgency             VARCHAR(8),                      -- low|medium|high
    skip_reason         TEXT,                            -- e.g. "exam week — defer"
    parent_draft_json   TEXT,                            -- vernacular draft (JSON)
    parent_draft_lang   VARCHAR(8),                      -- hi|en|...
    provider_used       VARCHAR(32),
    model_used          VARCHAR(64),
    grounding_passed    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP    NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_pews_case_files
    ON pews_case_files (school_id, student_code);

-- ── pews_effectiveness_priors ───────────────────────────────────────────────
-- The flywheel's policy memory: per-school, per-cause, per-action success rate.
CREATE TABLE IF NOT EXISTS pews_effectiveness_priors (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id           UUID         NOT NULL,
    cause_family        VARCHAR(24)  NOT NULL,           -- attendance|academic|disengagement|wellbeing|financial|external
    action_type         VARCHAR(32)  NOT NULL,           -- parent_call|home_visit|counselling|...
    n_tried             INTEGER      NOT NULL DEFAULT 0,
    n_improved          INTEGER      NOT NULL DEFAULT 0,
    improve_rate        DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    avg_days_to_improve DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    updated_at          TIMESTAMP    NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_pews_priors
    ON pews_effectiveness_priors (school_id, cause_family, action_type);

-- ── ALTER existing tables: add PEWS 2.0 columns ─────────────────────────────
-- Use DO block so each ADD COLUMN is guarded by IF NOT EXISTS (Postgres >= 9.6).

DO $$
BEGIN
    -- pews_risk_snapshots: expanded feature vector columns
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_name = 'pews_risk_snapshots' AND column_name = 'confidence') THEN
        ALTER TABLE pews_risk_snapshots ADD COLUMN confidence DOUBLE PRECISION;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_name = 'pews_risk_snapshots' AND column_name = 'leading_score') THEN
        ALTER TABLE pews_risk_snapshots ADD COLUMN leading_score INTEGER;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_name = 'pews_risk_snapshots' AND column_name = 'cause_family') THEN
        ALTER TABLE pews_risk_snapshots ADD COLUMN cause_family VARCHAR(24);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_name = 'pews_risk_snapshots' AND column_name = 'deltas_json') THEN
        ALTER TABLE pews_risk_snapshots ADD COLUMN deltas_json TEXT;
    END IF;

    -- pews_interventions: managed casework columns
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_name = 'pews_interventions' AND column_name = 'plan_json') THEN
        ALTER TABLE pews_interventions ADD COLUMN plan_json TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_name = 'pews_interventions' AND column_name = 'sla_days') THEN
        ALTER TABLE pews_interventions ADD COLUMN sla_days INTEGER;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_name = 'pews_interventions' AND column_name = 'escalation_level') THEN
        ALTER TABLE pews_interventions ADD COLUMN escalation_level INTEGER NOT NULL DEFAULT 0;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_name = 'pews_interventions' AND column_name = 'follow_up_date') THEN
        ALTER TABLE pews_interventions ADD COLUMN follow_up_date DATE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_name = 'pews_interventions' AND column_name = 'case_file_id') THEN
        ALTER TABLE pews_interventions ADD COLUMN case_file_id UUID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_name = 'pews_interventions' AND column_name = 'urgency') THEN
        ALTER TABLE pews_interventions ADD COLUMN urgency VARCHAR(8);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_name = 'pews_interventions' AND column_name = 'cause_family') THEN
        ALTER TABLE pews_interventions ADD COLUMN cause_family VARCHAR(24);
    END IF;
END $$;

-- Index for cause-family queries on snapshots
CREATE INDEX IF NOT EXISTS idx_pews_snap_cause
    ON pews_risk_snapshots (school_id, cause_family);

-- Verification:
-- SELECT table_name FROM information_schema.tables
--   WHERE table_name IN ('pews_feature_flags','pews_case_files','pews_effectiveness_priors');
-- Expected: 3 rows

COMMIT;
