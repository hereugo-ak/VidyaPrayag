-- =============================================================================
-- Migration 008 — enrollments (typed class membership)
--
-- WHY THIS EXISTS
--   Teacher Portal Rebuild — Doc 11 task T-001 (foundation). Today "who is in
--   class 7B" is answered by the in-memory ClassNaming.sameClassSection()
--   heuristic matching free-text students.class_name / .section against
--   teacher_subject_assignments.class_name / .section, and by parsing the
--   packed attendance_records.grade ("<class>-<section>") string. Both are the
--   X-1 / X-2 / D-STU root defects: there is NO typed join from a student to a
--   class. This migration introduces the `enrollments` table — the typed bridge
--   (student_id FK -> students, class_id FK -> school_classes) that makes
--   "enrolled students of 7B" a real query, replacing the heuristic.
--
--   The new Exposed mapping `EnrollmentsTable` in
--   server/.../db/Tables.kt references this table, and it is registered in
--   DatabaseFactory.allTables. AUTO_CREATE_TABLES is OFF in production, and the
--   boot-time validateSchema() gate REFUSES to boot Postgres when a registered
--   table is missing — so this file MUST be applied in Supabase BEFORE the
--   matching backend deploy.
--
-- HOW TO RUN
--   Supabase -> SQL Editor -> paste this whole file -> Run.
--   Safe to re-run: every statement is guarded (IF NOT EXISTS) and the backfill
--   is an idempotent INSERT ... ON CONFLICT DO NOTHING. It only ADDS a table and
--   rows; it never drops or rewrites existing data.
--
-- RUN ORDER (appended to the existing chain):
--   ... 
--   7. docs/db/migration_007_child_link_robustness.sql
--   8. docs/db/migration_008_enrollments.sql               <-- this file
--
-- Column names/types match server/.../db/Tables.kt (EnrollmentsTable) EXACTLY.
--
-- ─────────────────────────────────────────────────────────────────────────────
-- NOTE / PLANNING-DOC CONFLICT (flagged per LAWS — docs are authority):
--   • Doc 11 T-001 names the file `docs/backend/sql/migration_010_enrollments.sql`.
--     The real migration pipeline on this branch lives in `docs/db/` and the
--     latest committed migration is `migration_007`; `docs/backend/sql/` only
--     holds the 01_/02_ supplementary schemas, NOT the ordered chain. To keep
--     this migration inside the actual PROVISION run-order (rather than
--     orphaning it), it is placed at `docs/db/migration_008_enrollments.sql`.
--   • Column shape: Doc 01 §11.2 sketches enrollments as
--     (student_id, class_id, section, from_date, to_date, is_active), while
--     Doc 09 §1 specifies (school_id, student_id, class_id, section,
--     roll_number, status CHECK active/transferred/withdrawn, start_date,
--     end_date). Doc 11 T-001 "Details" explicitly cites "Doc 09 §1", so the
--     Doc 09 §1 shape is authoritative and is what this migration creates.
-- =============================================================================

BEGIN;

-- ─────────────────────────────────────────────────────────────────────────────
-- Helper functions mirroring server-side ClassNaming (idempotent CREATE OR
-- REPLACE — these are also defined by migration_005; re-asserting them here
-- makes this file self-contained and safe to run even if 005 was skipped).
-- ─────────────────────────────────────────────────────────────────────────────

-- Comparable class key: lowercase, strip a leading Grade/Class/Std prefix,
-- roman->arabic (1..12), strip incidental separators/whitespace.
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

-- Canonical section: trim + upper, blank -> 'A'.
CREATE OR REPLACE FUNCTION vp_section_key(raw text)
RETURNS text LANGUAGE plpgsql IMMUTABLE AS $$
DECLARE s text;
BEGIN
    s := upper(btrim(coalesce(raw, '')));
    IF s = '' THEN RETURN 'A'; END IF;
    RETURN s;
END $$;

-- ─────────────────────────────────────────────────────────────────────────────
-- 1) enrollments — typed class membership (Doc 09 §1)
--    The typed bridge: a student is enrolled in a class+section for a period.
--    UNIQUE (student_id, class_id, section, start_date) makes re-runs idempotent
--    and allows historical/transfer rows (different start_date) to coexist.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.enrollments (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id    uuid NOT NULL,
    student_id   uuid NOT NULL REFERENCES public.students(id) ON DELETE CASCADE,
    class_id     uuid NOT NULL REFERENCES public.school_classes(id) ON DELETE CASCADE,
    section      varchar(8) NOT NULL DEFAULT 'A',
    roll_number  integer NULL,
    status       varchar(16) NOT NULL DEFAULT 'active'
                   CHECK (status IN ('active','transferred','withdrawn')),
    start_date   date NOT NULL DEFAULT CURRENT_DATE,
    end_date     date NULL,
    created_at   timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ux_enrollments_unique UNIQUE (student_id, class_id, section, start_date)
);

CREATE INDEX IF NOT EXISTS ix_enrollments_school  ON public.enrollments (school_id);
CREATE INDEX IF NOT EXISTS ix_enrollments_class   ON public.enrollments (class_id);
CREATE INDEX IF NOT EXISTS ix_enrollments_student ON public.enrollments (student_id);

-- ─────────────────────────────────────────────────────────────────────────────
-- 2) Backfill from existing students rows.
--    For every active student, resolve its free-text class_name to the school's
--    configured school_classes row via the canonical class key, then create an
--    'active' enrollment. roll_number is parsed from the numeric portion of the
--    free-text students.roll_number (NULL when non-numeric). Students whose
--    class cannot be matched to a configured class are NOT enrolled here and are
--    surfaced by the unmatched report below (manual review).
--    ON CONFLICT DO NOTHING -> re-running never duplicates.
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO public.enrollments
    (school_id, student_id, class_id, section, roll_number, status, start_date)
SELECT
    s.school_id,
    s.id,
    sc.id,
    vp_section_key(s.section),
    NULLIF(regexp_replace(coalesce(s.roll_number, ''), '\D', '', 'g'), '')::integer,
    'active',
    CURRENT_DATE
FROM public.students s
JOIN public.school_classes sc
  ON sc.school_id = s.school_id
 AND vp_class_key(sc.name) = vp_class_key(s.class_name)
WHERE coalesce(s.is_active, true) = true
ON CONFLICT (student_id, class_id, section, start_date) DO NOTHING;

-- ─────────────────────────────────────────────────────────────────────────────
-- 3) Backfill report — matched vs unmatched counts (the T-001 "Done when").
--    Read these NOTICE lines in the Supabase SQL Editor output. Unmatched
--    students have a class_name that does not map to any configured
--    school_classes row for their school and need manual review (create the
--    missing class, or fix the student's class_name, then re-run this file).
-- ─────────────────────────────────────────────────────────────────────────────
DO $$
DECLARE
    v_total     bigint;
    v_matched   bigint;
    v_unmatched bigint;
BEGIN
    SELECT count(*) INTO v_total
    FROM public.students s
    WHERE coalesce(s.is_active, true) = true;

    SELECT count(DISTINCT s.id) INTO v_matched
    FROM public.students s
    JOIN public.school_classes sc
      ON sc.school_id = s.school_id
     AND vp_class_key(sc.name) = vp_class_key(s.class_name)
    WHERE coalesce(s.is_active, true) = true;

    v_unmatched := v_total - v_matched;

    RAISE NOTICE 'migration_008 enrollments backfill: total active students=%, matched(class resolved)=%, UNMATCHED(manual review)=%',
        v_total, v_matched, v_unmatched;

    IF v_unmatched > 0 THEN
        RAISE NOTICE 'migration_008: % student(s) have a class_name that maps to NO configured school_classes row — list them with the SELECT in the comment below.', v_unmatched;
    END IF;
END $$;

-- Unmatched students for manual review (run on demand):
--   SELECT s.id, s.school_id, s.student_code, s.full_name, s.class_name, s.section
--   FROM public.students s
--   WHERE coalesce(s.is_active, true) = true
--     AND NOT EXISTS (
--       SELECT 1 FROM public.school_classes sc
--       WHERE sc.school_id = s.school_id
--         AND vp_class_key(sc.name) = vp_class_key(s.class_name)
--     );

COMMIT;
