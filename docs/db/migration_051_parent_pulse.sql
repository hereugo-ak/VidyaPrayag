-- =============================================================================
-- Migration 051 — Parent Pulse
--                (weekly AI-generated summary card for parents)
--
-- WHY THIS EXISTS
--   PARENT_PULSE_SPEC.md — AI-generated weekly summary card for parents that
--   distills all school activity for their child into a single, glanceable
--   "pulse": attendance, marks, homework, announcements, messages, and AI
--   insights. Delivered as a notification + in-app card every Sunday 6 PM IST.
--
--   This creates one table:
--     parent_pulses — one row per (parent, student, week)
--
--   The Exposed mapping (ParentPulsesTable in server/.../db/Tables.kt)
--   references this table. AUTO_CREATE_TABLES is OFF in production and
--   validateSchema() gates boot on table presence, so this file MUST be
--   applied in Supabase BEFORE the matching backend deploy.
--
-- HOW TO RUN
--   Supabase -> SQL Editor -> paste this whole file -> Run.
--   Safe to re-run: every CREATE TABLE uses IF NOT EXISTS.
--
-- RUN ORDER (appended to the existing chain):
--   ...
--   50. docs/db/migration_050_health_records.sql
--   51. docs/db/migration_051_parent_pulse.sql          <-- this file
--
-- Column names/types are kept in lock-step with
-- server/.../db/Tables.kt (the Exposed mappings land in the same commit).
-- =============================================================================

BEGIN;

-- ── parent_pulses ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS parent_pulses (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id                UUID NOT NULL,
    parent_id                UUID NOT NULL,
    student_id               UUID NOT NULL,
    student_name             TEXT NOT NULL,
    week_start_date          DATE NOT NULL,
    week_end_date            DATE NOT NULL,
    attendance_percentage    DOUBLE PRECISION,
    attendance_trend         VARCHAR(8),               -- up | down | stable
    marks_summary            TEXT,                     -- JSON: [{"subject":"Math","test":"Unit Test","marks":85,"max":100}]
    homework_pending         INTEGER NOT NULL DEFAULT 0,
    homework_completed       INTEGER NOT NULL DEFAULT 0,
    announcements_count      INTEGER NOT NULL DEFAULT 0,
    unread_messages          INTEGER NOT NULL DEFAULT 0,
    upcoming_events          TEXT,                     -- JSON: [{"title":"PTM","date":"2026-07-01"}]
    ai_narrative             TEXT NOT NULL,            -- AI-generated or fallback narrative
    actionable_items         TEXT,                     -- JSON: ["Submit Math homework","Read PTM invitation"]
    model_used               VARCHAR(64),
    tokens_used              INTEGER,
    created_at               TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (parent_id, student_id, week_start_date)
);

CREATE INDEX IF NOT EXISTS idx_parent_pulses_parent
    ON parent_pulses (parent_id, week_start_date DESC);

-- Verification:
-- SELECT table_name FROM information_schema.tables
--   WHERE table_name = 'parent_pulses';
-- Expected: 1 row

COMMIT;
