-- =============================================================================
-- Migration 001 — faculty + holiday_list
--
-- WHY THIS EXISTS
--   SCHOOL_SIDE_STATUS_REPORT §5.1 found that the Ktor backend's Exposed
--   mappings (server/.../db/Tables.kt) reference two tables that the uploaded
--   operational schema (docs/db/vidyasetu_schema.sql) does NOT create:
--       * faculty       -> FacultyTable        (teacher attendance + analytics)
--       * holiday_list  -> HolidayListTable     (/holidays + calendar summary)
--   Without these tables the following school features fail on production
--   Supabase with "relation does not exist" or always return empty:
--       * GET /api/v1/school/attendance/daily?type=faculty
--       * GET /api/v1/school/holidays
--       * GET /api/v1/school/calendar   (public/school holiday counts)
--       * Teacher performance analytics
--
-- HOW TO RUN
--   Supabase -> SQL Editor -> paste this whole file -> Run.
--   Safe to re-run: every statement is guarded with IF NOT EXISTS.
--
-- The column names/types below match server/.../db/Tables.kt EXACTLY so the
-- Exposed mappings line up with the real Postgres schema.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- faculty  ->  FacultyTable
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.faculty (
  id          uuid NOT NULL,
  school_id   uuid NOT NULL,
  external_id text NOT NULL,
  user_id     uuid NULL,
  name        text NOT NULL,
  profile_pic text NULL,
  department  text NULL,
  is_active   boolean NOT NULL DEFAULT true,
  created_at  timestamp without time zone NOT NULL,
  CONSTRAINT faculty_pkey PRIMARY KEY (id),
  CONSTRAINT faculty_external_id_unique UNIQUE (external_id)
) TABLESPACE pg_default;

CREATE INDEX IF NOT EXISTS ix_faculty_school_id ON public.faculty (school_id);
CREATE INDEX IF NOT EXISTS ix_faculty_active    ON public.faculty (school_id, is_active);

-- ---------------------------------------------------------------------------
-- holiday_list  ->  HolidayListTable
--   type      = 'Public' | 'School'
--   frequency = 'weekly' | 'monthly' | 'yearly'
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.holiday_list (
  id         uuid NOT NULL,
  school_id  uuid NOT NULL,
  date       character varying(12) NOT NULL,
  title      text NOT NULL,
  type       character varying(16) NOT NULL,
  frequency  character varying(16) NOT NULL,
  created_at timestamp without time zone NOT NULL,
  CONSTRAINT holiday_list_pkey PRIMARY KEY (id)
) TABLESPACE pg_default;

CREATE INDEX IF NOT EXISTS ix_holiday_list_school_id ON public.holiday_list (school_id);
CREATE INDEX IF NOT EXISTS ix_holiday_list_lookup    ON public.holiday_list (school_id, frequency);

-- =============================================================================
-- DONE. After running this, re-test:
--   GET /api/v1/school/holidays?filter_type=yearly
--   GET /api/v1/school/attendance/daily?type=faculty
--   GET /api/v1/school/calendar
-- All three should return data instead of an error/empty set.
-- =============================================================================
