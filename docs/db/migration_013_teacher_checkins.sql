-- =============================================================================
-- Migration 013 — teacher self check-in (teacher_check_ins)
--
-- WHY THIS EXISTS
--   Teacher Portal Rebuild — Doc 11 task T-106a (Phase 1, attendance spine).
--   Doc 06 §1.3 / §2. Closes B-ATT-5 (teacher self check-in).
--
--   Today a teacher's "presence" is implied only by taking attendance for a
--   period; there is no first-person "I am here today" record. Doc 04 §5.1 puts
--   a check-in band at the top of the Today tab (amber "Tap to check in" → green
--   "Checked in HH:mm") and Doc 06 §2 defines the biometric ladder
--   (biometric → PIN → manual, always-available fallback, never a hard gate).
--   This migration introduces the `teacher_check_ins` table — the authoritative
--   one-row-per-teacher-per-day check-in record, server-stamped
--   (`checked_in_at` is set by the backend with the server clock, NOT the
--   device clock — Doc 06 §2.4 clock-skew edge case).
--
--   The new Exposed mapping `TeacherCheckInsTable` in
--   server/.../db/Tables.kt references this table, and it is registered in
--   DatabaseFactory.allTables. AUTO_CREATE_TABLES is OFF in production, and the
--   boot-time validateSchema() gate REFUSES to boot Postgres when a registered
--   table is missing — so this file MUST be applied in Supabase BEFORE the
--   matching backend deploy.
--
-- WHAT THIS DOES
--   Creates `public.teacher_check_ins` with the Doc 06 §1.3 shape:
--     • id            uuid PK default gen_random_uuid()
--     • school_id     uuid NOT NULL                       (tenant scope)
--     • teacher_id    uuid NOT NULL FK -> app_users(id)   (the checking-in teacher)
--     • date          date NOT NULL                       (the school day, server date)
--     • checked_in_at timestamptz NOT NULL                (authoritative server-stamped time)
--     • method        varchar(16) CHECK IN ('biometric','pin','manual')
--     • device_id     text NULL                           (optional device fingerprint)
--     • UNIQUE (school_id, teacher_id, date)              (idempotency: one check-in per
--                                                          teacher per day per school)
--
--   The UNIQUE constraint is the idempotency key for the POST /teacher/checkin
--   endpoint (T-106b): a second POST for the same (school, teacher, date)
--   returns the existing row instead of duplicating.
--
--   This is pure DDL — it ADDS a table only; it never drops or rewrites
--   existing data. CREATE TABLE IF NOT EXISTS + CREATE INDEX IF NOT EXISTS
--   make it safe to re-run (a no-op once applied).
--
-- DEVIATION (flagged per Doc 11 Rule: docs are authority)
--   • Filename: Doc 11 T-106 names the file `migration_015_teacher_checkins.sql`.
--     The on-disk chain is contiguous and the latest committed migration is
--     `migration_012_holidays_merge.sql`, so this file is
--     `migration_013_teacher_checkins.sql` (same documented deviation pattern
--     as 009/010/011/012 — the doc number is a label, not the filename).
--   • Schema: this matches Doc 06 §1.3 EXACTLY (no added audit columns). The
--     row IS the check-in event — `checked_in_at` is the row's authoritative
--     timestamp, so a separate `created_at` is not required by the spec and is
--     not added (stays faithful to the authority).
--
-- HOW TO RUN
--   Supabase -> SQL Editor -> paste this whole file -> Run.
--   Safe to re-run: every statement is guarded by IF NOT EXISTS.
--
-- RUN ORDER (appended to the existing chain):
--   ...
--   11. docs/db/migration_011_periods.sql
--   12. docs/db/migration_012_holidays_merge.sql
--   13. docs/db/migration_013_teacher_checkins.sql   <-- THIS FILE
--
-- Column names/types match server/.../db/Tables.kt (TeacherCheckInsTable)
-- EXACTLY (Exposed `timestamp()` -> Postgres `timestamptz`, following the same
-- convention as enrollments / calendar_events / academic_years).
-- =============================================================================

BEGIN;

-- ─────────────────────────────────────────────────────────────────────────────
-- teacher_check_ins — one check-in row per teacher per school per day.
-- Doc 06 §1.3 schema. The UNIQUE (school_id, teacher_id, date) is the
-- idempotency key for POST /api/v1/teacher/checkin (T-106b).
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.teacher_check_ins (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id     uuid NOT NULL,
    teacher_id    uuid NOT NULL REFERENCES public.app_users(id) ON DELETE CASCADE,
    date          date NOT NULL,
    checked_in_at timestamptz NOT NULL,
    method        varchar(16) NOT NULL
                     CHECK (method IN ('biometric','pin','manual')),
    device_id     text NULL,
    CONSTRAINT ux_teacher_checkins_unique UNIQUE (school_id, teacher_id, date)
);

-- Lookups: "is teacher X checked in on date Y" and "who checked in at school S
-- on date D" are the two hot paths (GET /teacher/checkin + admin views).
CREATE INDEX IF NOT EXISTS ix_teacher_checkins_teacher
    ON public.teacher_check_ins (teacher_id, date);
CREATE INDEX IF NOT EXISTS ix_teacher_checkins_school
    ON public.teacher_check_ins (school_id, date);

COMMIT;

-- =============================================================================
-- POST-RUN REPORT — confirms the table is present and queryable.
-- A fresh run returns count = 0 (no check-ins yet); after teachers start using
-- the Today-tab check-in card (T-106c) this will show real rows.
-- =============================================================================
SELECT count(*) AS checkins_present FROM public.teacher_check_ins;
