-- =============================================================================
-- Migration 002 — broadcast segmentation + geo discovery + teacher assignments
--                 + STUDENT-scope child<->student link + media-upload support
--
-- WHY THIS EXISTS
--   The backend gained new P1/P2 features (SCHOOL_SIDE_STATUS_REPORT §4.4, §5.5,
--   §5.6, §5.7) whose Exposed mappings in
--   server/.../db/Tables.kt now reference columns/tables that the live
--   Supabase schema (docs/db/vidyasetu_schema.sql) does NOT yet have. Without
--   this migration the backend will fail on Render (AUTO_CREATE_TABLES is off in
--   production) with errors like:
--       column "audience_type" of relation "announcements" does not exist
--       column "latitude" of relation "schools" does not exist
--       relation "teacher_subject_assignments" does not exist
--
-- HOW TO RUN
--   Supabase -> SQL Editor -> paste this whole file -> Run.
--   Safe to re-run: every statement is guarded (IF NOT EXISTS / ADD COLUMN IF
--   NOT EXISTS). It only ADDS columns/tables; it never drops or rewrites data.
--
-- RUN ORDER (full provisioning):
--   1. vidyasetu_schema.sql
--   2. migration_001_faculty_and_holiday_list.sql
--   3. migration_002_segmentation_geo_assignments.sql   <-- this file
--
-- Column names/types match server/.../db/Tables.kt EXACTLY.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1) announcements — broadcast audience segmentation  (report §5.6)
--    audience_type  : ALL_SCHOOL | CLASS | SECTION | SUBJECT | STUDENT | CUSTOM
--    audience_filter: JSON scope, e.g. {"class_name":"Grade 5","section":"A"}
--    author_role    : owner role; teacher broadcasts are scoped to their classes
-- ---------------------------------------------------------------------------
ALTER TABLE public.announcements
  ADD COLUMN IF NOT EXISTS audience_type   character varying(16) NOT NULL DEFAULT 'ALL_SCHOOL';
ALTER TABLE public.announcements
  ADD COLUMN IF NOT EXISTS audience_filter text NULL;
ALTER TABLE public.announcements
  ADD COLUMN IF NOT EXISTS author_role     character varying(16) NOT NULL DEFAULT 'school_admin';

-- ---------------------------------------------------------------------------
-- 2) schools — geo coordinates for parent "near me" discovery (report §4.4/§5.7)
-- ---------------------------------------------------------------------------
ALTER TABLE public.schools
  ADD COLUMN IF NOT EXISTS latitude  double precision NULL;
ALTER TABLE public.schools
  ADD COLUMN IF NOT EXISTS longitude double precision NULL;

-- Optional helper index for bounding-box pre-filtering before Haversine sort.
CREATE INDEX IF NOT EXISTS ix_schools_geo ON public.schools (latitude, longitude);

-- ---------------------------------------------------------------------------
-- 3) teacher_subject_assignments — structured teacher<->class<->subject (§5.5)
--    Replaces free-text school_subjects.teacher_assigned.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.teacher_subject_assignments (
  id           uuid NOT NULL,
  school_id    uuid NOT NULL,
  class_id     uuid NULL,
  class_name   text NOT NULL,
  section      character varying(8) NOT NULL DEFAULT 'A',
  subject_id   uuid NULL,
  subject      text NOT NULL,
  teacher_id   uuid NULL,
  teacher_name text NULL,
  is_active    boolean NOT NULL DEFAULT true,
  created_at   timestamp without time zone NOT NULL,
  updated_at   timestamp without time zone NOT NULL,
  CONSTRAINT teacher_subject_assignments_pkey PRIMARY KEY (id),
  CONSTRAINT ux_tsa_unique UNIQUE (school_id, class_name, section, subject, teacher_name)
) TABLESPACE pg_default;

CREATE INDEX IF NOT EXISTS ix_tsa_school    ON public.teacher_subject_assignments (school_id, is_active);
CREATE INDEX IF NOT EXISTS ix_tsa_teacher   ON public.teacher_subject_assignments (school_id, teacher_id);
CREATE INDEX IF NOT EXISTS ix_tsa_class     ON public.teacher_subject_assignments (school_id, class_name);

-- ---------------------------------------------------------------------------
-- 4) children — link a parent's child to the school's canonical student record
--    so STUDENT-scoped broadcasts can target exact students (report §5.6 note).
--    student_code references students.student_code (text, unique).
-- ---------------------------------------------------------------------------
ALTER TABLE public.children
  ADD COLUMN IF NOT EXISTS student_code text NULL;

CREATE INDEX IF NOT EXISTS ix_children_student_code ON public.children (student_code);

-- =============================================================================
-- DONE. After running this, the following should work without errors:
--   POST /api/v1/school/announcements           (with audience_type/filter)
--   POST /api/v1/school/announcements/sync-whatsapp   (segmented recipients)
--   GET/POST/DELETE /api/v1/school/teacher-assignments
--   GET /api/v1/parent/schools/discover?lat=&lng=
-- =============================================================================
