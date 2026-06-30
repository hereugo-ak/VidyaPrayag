-- =============================================================================
-- Migration 061 — PEWS (Predictive Early Warning System)
--
-- WHY THIS EXISTS
--   AI_FEATURES_PLAN.md Part A §A.3 — PEWS is the first AI feature. It upgrades
--   the already-shipped rule-based early-warning list (SchoolIntelligenceRouting
--   early_warning block) into a Sense -> Reason -> Act -> Learn agent.
--
--   The INPUT signals (attendance/marks/leave) come from EXISTING tables
--   (attendance_records, assessment_marks, assessments, leave_requests,
--   students) — NO schema change there. This migration only adds the agent's
--   MEMORY:
--     pews_risk_snapshots  — one auditable row per (school, student, run_date);
--                            deterministic numbers + nullable AI narrative/cause/
--                            recommendation (null = not yet reasoned). Honesty
--                            LAW 6: every AI field traces to a real snapshot.
--     pews_interventions   — the Act + Learn loop (owner, action, status, outcome)
--     pews_config          — per-school tuning (PK = school_id)
--
--   The Exposed mappings (Pews*Table in server/.../db/Tables.kt) reference these.
--   AUTO_CREATE_TABLES is OFF in production and validateSchema() gates boot, so
--   this file MUST be applied in Supabase BEFORE the matching backend deploy.
--   Depends on migration_060 (AI gateway) being applied first.
--
-- HOW TO RUN
--   Supabase -> SQL Editor -> paste this whole file -> Run.
--   Safe to re-run: every CREATE TABLE uses IF NOT EXISTS.
--
-- RUN ORDER:
--   ...
--   60. docs/db/migration_060_ai_gateway.sql
--   61. docs/db/migration_061_pews.sql                  <-- this file
-- =============================================================================

BEGIN;

-- ── pews_risk_snapshots ──────────────────────────────────────────────────────
-- Snapshot of each computed risk run (auditable, reproducible). signal_hash is
-- the SHA-256 of the deterministic signal bundle and is the cache key for the
-- AI narrative (identical signals never re-call the LLM).
CREATE TABLE IF NOT EXISTS pews_risk_snapshots (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id           UUID         NOT NULL,
    student_code        TEXT         NOT NULL,
    run_date            DATE         NOT NULL,
    risk_score          INTEGER      NOT NULL,           -- 0..100 composite, deterministic
    risk_level          VARCHAR(8)   NOT NULL,           -- watch | medium | high
    attendance_pct      INTEGER,
    marks_pct           INTEGER,
    leave_count         INTEGER      NOT NULL DEFAULT 0,
    attendance_slope    DOUBLE PRECISION,                -- negative = sliding
    marks_slope         DOUBLE PRECISION,
    signals_json        TEXT         NOT NULL,           -- deterministic reasons array (JSON)
    signal_hash         VARCHAR(64)  NOT NULL,           -- SHA-256 of the signal bundle
    ai_narrative        TEXT,                            -- LLM explanation (null = not yet reasoned)
    ai_cause            TEXT,                            -- LLM likely-cause hypothesis
    ai_recommendation   TEXT,                            -- LLM recommended intervention
    ai_provider_used    VARCHAR(32),                     -- observability
    created_at          TIMESTAMP    NOT NULL DEFAULT now(),
    UNIQUE (school_id, student_code, run_date)
);
CREATE INDEX IF NOT EXISTS idx_pews_snap
    ON pews_risk_snapshots (school_id, run_date DESC, risk_level);

-- ── pews_interventions ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS pews_interventions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id           UUID         NOT NULL,
    student_code        TEXT         NOT NULL,
    snapshot_id         UUID,                            -- FK pews_risk_snapshots.id (soft)
    owner_user_id       UUID         NOT NULL,           -- assigned teacher/admin
    action_type         VARCHAR(32)  NOT NULL,           -- parent_call|home_visit|counselling|remedial_class|parent_message|observe
    status              VARCHAR(16)  NOT NULL DEFAULT 'open', -- open|in_progress|done|dismissed
    notes               TEXT,
    opened_at           TIMESTAMP    NOT NULL DEFAULT now(),
    resolved_at         TIMESTAMP,
    outcome             VARCHAR(16),                     -- improved|unchanged|worsened (Learn stage)
    created_at          TIMESTAMP    NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_pews_intv
    ON pews_interventions (school_id, status, owner_user_id);

-- ── pews_config ──────────────────────────────────────────────────────────────
-- One row per school; the id column IS the school_id (we reuse the UUIDTable
-- mapping for uniformity but treat id as the school identity).
CREATE TABLE IF NOT EXISTS pews_config (
    id                       UUID PRIMARY KEY,           -- = school_id
    use_relative_thresholds  BOOLEAN  NOT NULL DEFAULT TRUE,
    attendance_floor_pct     INTEGER  NOT NULL DEFAULT 75,   -- fallback absolute floor
    marks_floor_pct          INTEGER  NOT NULL DEFAULT 40,
    leave_floor_count        INTEGER  NOT NULL DEFAULT 3,
    run_frequency            VARCHAR(8) NOT NULL DEFAULT 'daily', -- daily|weekly
    ai_narrative_enabled     BOOLEAN  NOT NULL DEFAULT TRUE,
    parent_share_enabled     BOOLEAN  NOT NULL DEFAULT FALSE, -- gates parent-facing nudges
    updated_at               TIMESTAMP NOT NULL DEFAULT now()
);

-- Verification:
-- SELECT table_name FROM information_schema.tables
--   WHERE table_name IN ('pews_risk_snapshots','pews_interventions','pews_config');
-- Expected: 3 rows

COMMIT;
