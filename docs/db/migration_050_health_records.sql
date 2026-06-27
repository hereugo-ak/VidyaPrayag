-- =============================================================================
-- Migration 050 — Student Health Records
--                (health profiles, immunizations, health incidents)
--
-- WHY THIS EXISTS
--   HEALTH_RECORDS_SPEC.md (P1-12) — Student Health & Immunization Records.
--   feature_audit.csv L120: Health Records missing (0%).
--   No health tables existed in the schema or Tables.kt before this migration.
--
--   This creates three tables:
--     1. student_health_profiles   — 1:1 per-student health profile
--     2. student_immunizations     — immunization/vaccine tracking
--     3. student_health_incidents  — health incident log
--
--   The Exposed mappings (StudentHealthProfilesTable, StudentImmunizationsTable,
--   StudentHealthIncidentsTable in server/.../db/Tables.kt) reference these
--   tables. AUTO_CREATE_TABLES is OFF in production and validateSchema() gates
--   boot on table presence, so this file MUST be applied in Supabase BEFORE
--   the matching backend deploy.
--
-- HOW TO RUN
--   Supabase -> SQL Editor -> paste this whole file -> Run.
--   Safe to re-run: every CREATE TABLE uses IF NOT EXISTS.
--
-- RUN ORDER (appended to the existing chain):
--   ...
--   25. docs/db/migration_025_lesson_planning.sql
--   50. docs/db/migration_050_health_records.sql          <-- this file
--
-- Column names/types are kept in lock-step with
-- server/.../db/Tables.kt (the Exposed mappings land in the same commit).
-- =============================================================================

BEGIN;

-- ── 1. student_health_profiles ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS student_health_profiles (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id                UUID NOT NULL,
    student_id               UUID NOT NULL UNIQUE,
    blood_group              VARCHAR(8),
    height_cm                REAL,
    weight_kg                REAL,
    allergies                TEXT NOT NULL DEFAULT '[]',
    chronic_conditions       TEXT NOT NULL DEFAULT '[]',
    medications              TEXT NOT NULL DEFAULT '[]',
    emergency_contact_name   TEXT,
    emergency_contact_phone  VARCHAR(32),
    doctor_name              TEXT,
    doctor_phone             VARCHAR(32),
    updated_at               TIMESTAMP NOT NULL DEFAULT now(),
    created_at               TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_health_profiles_school
    ON student_health_profiles (school_id);

-- ── 2. student_immunizations ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS student_immunizations (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id        UUID NOT NULL,
    vaccine_name      TEXT NOT NULL,
    dose_number       INTEGER NOT NULL DEFAULT 1,
    date_administered DATE NOT NULL,
    next_due_date     DATE,
    administered_by   TEXT,
    created_at        TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_immunizations_student
    ON student_immunizations (student_id);

-- ── 3. student_health_incidents ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS student_health_incidents (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id           UUID NOT NULL,
    student_id          UUID NOT NULL,
    date                DATE NOT NULL,
    time                VARCHAR(8),
    description         TEXT NOT NULL,
    treatment           TEXT,
    medication_given    TEXT,
    parent_notified     BOOLEAN NOT NULL DEFAULT false,
    parent_notified_at  TIMESTAMP,
    attended_by         UUID,
    attended_by_name    TEXT,
    severity            VARCHAR(16) NOT NULL DEFAULT 'minor',
    created_at          TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_health_incidents_student
    ON student_health_incidents (student_id, date DESC);

-- Verification:
-- SELECT table_name FROM information_schema.tables
--   WHERE table_name IN ('student_health_profiles', 'student_immunizations', 'student_health_incidents');
-- Expected: 3 rows

COMMIT;
