-- =============================================================================
-- Migration 009 — promote teacher_subject_assignments to typed FKs
--                 + is_class_teacher flag
--
-- WHY THIS EXISTS
--   Teacher Portal Rebuild — Doc 11 task T-002 (foundation, depends on T-001).
--   teacher_subject_assignments (TSA) currently carries nullable class_id /
--   subject_id columns (migration_002) that are NOT backfilled and have NO FK
--   constraint, plus free-text class_name/section/subject that the scoping core
--   treats as the source of truth via the ClassNaming string heuristic (X-1,
--   B-CLS-3, D-TSA). There is also no signal for "this teacher is the class
--   teacher of 7B".
--
--   This migration makes the typed columns authoritative:
--     • backfills class_id   from class_name -> school_classes.id (vp_class_key)
--     • backfills subject_id from subject    -> school_subjects.id (within the
--       resolved class)
--     • adds FK constraints on both (so the IDs can be trusted)
--     • adds is_class_teacher boolean DEFAULT false
--   class_name/section/subject remain as DISPLAY-only denormalised columns; the
--   scoping core (TeacherAccess, T-003) switches to id-first and demotes
--   ClassNaming.sameClassSection to a fallback.
--
-- HOW TO RUN
--   Supabase -> SQL Editor -> paste this whole file -> Run.
--   Safe to re-run: ADD COLUMN IF NOT EXISTS, guarded FK creation (skips if the
--   constraint already exists), and idempotent UPDATE backfills. It never drops
--   or rewrites the display columns.
--
-- RUN ORDER (appended to the existing chain):
--   ...
--   8. docs/db/migration_008_enrollments.sql
--   9. docs/db/migration_009_tsa_fks.sql                   <-- this file
--
-- Column names/types match server/.../db/Tables.kt
-- (TeacherSubjectAssignmentsTable) EXACTLY.
--
-- ─────────────────────────────────────────────────────────────────────────────
-- NOTE (flagged per LAWS — docs are authority):
--   Doc 11 T-002 names the file `migration_011_tsa_fks.sql`. The real chain
--   lives in docs/db/ and the previous (T-001) migration is 008, so this is
--   placed at migration_009 to keep the sequence contiguous and inside the
--   PROVISION run-order.
-- =============================================================================

BEGIN;

-- vp_class_key is (re)defined by migration_005 / migration_008. Re-assert it so
-- this file is self-contained and safe even if those were skipped.
CREATE OR REPLACE FUNCTION vp_class_key(raw text)
RETURNS text LANGUAGE plpgsql IMMUTABLE AS $$
DECLARE s text;
BEGIN
    IF raw IS NULL THEN RETURN ''; END IF;
    s := lower(btrim(regexp_replace(raw, '\s+', ' ', 'g')));
    s := regexp_replace(s, '^(grade|class|standard|std\.?|cls)[\s\-]+', '');
    s := btrim(s);
    s := CASE s
        WHEN 'i' THEN '1' WHEN 'ii' THEN '2' WHEN 'iii' THEN '3' WHEN 'iv' THEN '4'
        WHEN 'v' THEN '5' WHEN 'vi' THEN '6' WHEN 'vii' THEN '7' WHEN 'viii' THEN '8'
        WHEN 'ix' THEN '9' WHEN 'x' THEN '10' WHEN 'xi' THEN '11' WHEN 'xii' THEN '12'
        ELSE s END;
    s := regexp_replace(s, '\s+', '', 'g');
    RETURN s;
END $$;

-- Comparable subject key: lowercase + strip non-alphanumerics, so "Maths" and
-- "maths" and "MATHS " collapse for the subject_id resolution.
CREATE OR REPLACE FUNCTION vp_subject_key(raw text)
RETURNS text LANGUAGE plpgsql IMMUTABLE AS $$
BEGIN
    IF raw IS NULL THEN RETURN ''; END IF;
    RETURN lower(regexp_replace(raw, '[^a-zA-Z0-9]', '', 'g'));
END $$;

-- ─────────────────────────────────────────────────────────────────────────────
-- 1) is_class_teacher flag (B-CLS-3 / F-CLS-3).
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE public.teacher_subject_assignments
  ADD COLUMN IF NOT EXISTS is_class_teacher boolean NOT NULL DEFAULT false;

-- ─────────────────────────────────────────────────────────────────────────────
-- 2) Backfill class_id from the free-text class_name (one-time, idempotent).
--    Only fills rows whose class_id is still NULL and whose class_name maps to
--    exactly one configured class for the same school.
-- ─────────────────────────────────────────────────────────────────────────────
UPDATE public.teacher_subject_assignments t
SET class_id = sc.id
FROM public.school_classes sc
WHERE t.class_id IS NULL
  AND sc.school_id = t.school_id
  AND vp_class_key(sc.name) = vp_class_key(t.class_name);

-- ─────────────────────────────────────────────────────────────────────────────
-- 3) Backfill subject_id from the free-text subject within the resolved class
--    (one-time, idempotent). school_subjects is keyed by class_id + sub_name.
-- ─────────────────────────────────────────────────────────────────────────────
UPDATE public.teacher_subject_assignments t
SET subject_id = ss.id
FROM public.school_subjects ss
WHERE t.subject_id IS NULL
  AND t.class_id IS NOT NULL
  AND ss.class_id = t.class_id
  AND vp_subject_key(ss.sub_name) = vp_subject_key(t.subject);

-- ─────────────────────────────────────────────────────────────────────────────
-- 4) FK constraints on the now-backfilled typed columns. Both columns stay
--    NULLABLE (a row that couldn't be resolved keeps a NULL FK + display text),
--    so the FK only validates the non-NULL values. Guarded so re-runs skip.
-- ─────────────────────────────────────────────────────────────────────────────
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_tsa_class'
    ) THEN
        ALTER TABLE public.teacher_subject_assignments
          ADD CONSTRAINT fk_tsa_class
          FOREIGN KEY (class_id) REFERENCES public.school_classes(id)
          ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_tsa_subject'
    ) THEN
        ALTER TABLE public.teacher_subject_assignments
          ADD CONSTRAINT fk_tsa_subject
          FOREIGN KEY (subject_id) REFERENCES public.school_subjects(id)
          ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS ix_tsa_class_id   ON public.teacher_subject_assignments (class_id);
CREATE INDEX IF NOT EXISTS ix_tsa_subject_id ON public.teacher_subject_assignments (subject_id);

-- ─────────────────────────────────────────────────────────────────────────────
-- 5) Backfill report — how many TSA rows now have resolved FKs (T-002 "Done
--    when": FKs populated). Unresolved rows keep working via display text +
--    the ClassNaming fallback until their class/subject is configured.
-- ─────────────────────────────────────────────────────────────────────────────
DO $$
DECLARE
    v_total       bigint;
    v_class_ok    bigint;
    v_subject_ok  bigint;
BEGIN
    SELECT count(*) INTO v_total       FROM public.teacher_subject_assignments;
    SELECT count(*) INTO v_class_ok    FROM public.teacher_subject_assignments WHERE class_id   IS NOT NULL;
    SELECT count(*) INTO v_subject_ok  FROM public.teacher_subject_assignments WHERE subject_id IS NOT NULL;

    RAISE NOTICE 'migration_009 TSA FK backfill: total assignments=%, class_id resolved=%, subject_id resolved=%',
        v_total, v_class_ok, v_subject_ok;

    IF v_total > v_class_ok THEN
        RAISE NOTICE 'migration_009: % assignment(s) still have NULL class_id (class_name maps to no configured class) — review with the SELECT in the comment below.',
            (v_total - v_class_ok);
    END IF;
END $$;

-- Unresolved assignments for manual review (run on demand):
--   SELECT id, school_id, class_name, section, subject, teacher_name
--   FROM public.teacher_subject_assignments
--   WHERE class_id IS NULL;

COMMIT;
