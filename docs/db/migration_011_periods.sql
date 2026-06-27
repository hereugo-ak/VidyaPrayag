-- =============================================================================
-- Migration 011 — typed, bound, term-scoped teacher_periods + period_exceptions
--
-- WHY THIS EXISTS
--   Teacher Portal Rebuild — Doc 11 task T-101 (Phase 1, schedule spine).
--   Doc 05 §2.1/§2.2. Closes D-TT-1..4 and lays the foundation for the
--   resolved-day computation (T-104).
--
--   `teacher_periods` was a free-text recurring weekly pattern:
--     • start_time / end_time were varchar(8) "HH:mm"  → string time compares
--       (D-TT-2)
--     • the period was bound to a class only by the free-text className /
--       section / subject columns → drift, no authorization link (D-TT-4, X-1)
--     • no term validity window, no soft-disable, no one-off overrides
--       (substitutions / cancellations / room changes) → "today" cannot be
--       resolved for a SPECIFIC date.
--
-- WHAT THIS DOES
--   teacher_periods:
--     1. start_time / end_time : varchar → `time` (typed; D-TT-2).
--     2. NEW academic_year_id  uuid NULL  fk -> academic_years  (term validity).
--     3. NEW assignment_id     uuid NULL  fk -> teacher_subject_assignments
--        (D-TT-4 — binds a period to the TSA that authorizes it; the linchpin of
--         context inference: current period → assignment_id →
--         requireOwnedAssignment → authorized attendance/marks scope).
--     4. NEW valid_from / valid_to  date NULL  (term-revision window).
--     5. NEW is_active  boolean DEFAULT true  (soft-disable).
--     6. Best-effort backfill of assignment_id from the legacy
--        (school_id, class_name, section, subject, teacher_id) tuple → the
--        matching teacher_subject_assignments row.
--     7. UNIQUE (school_id, teacher_id, weekday, start_time)  (no double-book).
--
--   period_exceptions (NEW):
--     one-off overrides to the recurring pattern for a specific date
--     (CANCELLED | RESCHEDULED | ROOM_CHANGE | SUBSTITUTION | EXTRA).
--
-- DEVIATION (flagged per Doc 11 Rule: docs are authority)
--   • Doc 05 §2.1 says "DROP className/section/subject as source of truth".
--     This migration does NOT drop those columns yet — it keeps them as a
--     demoted display/legacy fallback so the existing timetable readers
--     (Parent/School/Teacher routing) keep working in the same release. A later
--     phase drops them once every reader derives class/subject via assignment_id.
--   • Filename: Doc 11 names this `migration_013_periods.sql`. The real chain in
--     docs/db/ is contiguous and the prior migration is `010`, so this file is
--     `migration_011_periods.sql` (same documented deviation pattern as
--     009/010). The doc number is a label, not the on-disk filename.
--
-- HOW TO RUN
--   Supabase -> SQL Editor -> paste this whole file -> Run.
--   Safe to re-run: every ALTER is guarded by information_schema so an
--   already-migrated column is a no-op; ADD COLUMN uses IF NOT EXISTS; the new
--   table is CREATE TABLE IF NOT EXISTS; the backfill UPDATE is naturally
--   idempotent (only fills NULL assignment_id rows).
--
-- RUN ORDER (appended to the existing chain):
--   ...
--   10. docs/db/migration_010_typed_dates.sql
--   11. docs/db/migration_011_periods.sql      <-- THIS FILE
-- =============================================================================

BEGIN;

-- ---------------------------------------------------------------------------
-- 0. Safety: the table must exist (it is created by the Exposed bootstrap on
--    first server boot). If it does not yet exist, this whole migration is a
--    no-op — the Exposed mapping will create it already-typed.
-- ---------------------------------------------------------------------------
DO $$
BEGIN
  IF to_regclass('public.teacher_periods') IS NULL THEN
    RAISE NOTICE 'teacher_periods does not exist yet — skipping (Exposed will create it typed).';
    RETURN;
  END IF;

  -- -------------------------------------------------------------------------
  -- 1. start_time / end_time : varchar -> time (only if still character type)
  -- -------------------------------------------------------------------------
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'teacher_periods'
      AND column_name = 'start_time' AND data_type IN ('character varying', 'character', 'text')
  ) THEN
    ALTER TABLE public.teacher_periods
      ALTER COLUMN start_time TYPE time USING (NULLIF(trim(start_time), ''))::time;
    RAISE NOTICE 'teacher_periods.start_time -> time';
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'teacher_periods'
      AND column_name = 'end_time' AND data_type IN ('character varying', 'character', 'text')
  ) THEN
    ALTER TABLE public.teacher_periods
      ALTER COLUMN end_time TYPE time USING (NULLIF(trim(end_time), ''))::time;
    RAISE NOTICE 'teacher_periods.end_time -> time';
  END IF;
END $$;

-- ---------------------------------------------------------------------------
-- 2. New columns (idempotent)
-- ---------------------------------------------------------------------------
ALTER TABLE public.teacher_periods
  ADD COLUMN IF NOT EXISTS academic_year_id uuid,
  ADD COLUMN IF NOT EXISTS assignment_id    uuid,
  ADD COLUMN IF NOT EXISTS valid_from       date,
  ADD COLUMN IF NOT EXISTS valid_to         date,
  ADD COLUMN IF NOT EXISTS is_active        boolean NOT NULL DEFAULT true;

-- end_time > start_time (one-time, guarded so a re-run doesn't duplicate it)
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'ck_periods_time_order'
  ) THEN
    ALTER TABLE public.teacher_periods
      ADD CONSTRAINT ck_periods_time_order CHECK (end_time > start_time);
  END IF;
EXCEPTION WHEN others THEN
  -- If legacy data violates the order we don't want to hard-fail the whole
  -- migration; the constraint can be added by hand after triage.
  RAISE NOTICE 'ck_periods_time_order not added (existing rows may violate it): %', SQLERRM;
END $$;

-- weekday range 1..7 (ISO) — guarded
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'ck_periods_weekday_range'
  ) THEN
    ALTER TABLE public.teacher_periods
      ADD CONSTRAINT ck_periods_weekday_range CHECK (weekday BETWEEN 1 AND 7);
  END IF;
EXCEPTION WHEN others THEN
  RAISE NOTICE 'ck_periods_weekday_range not added: %', SQLERRM;
END $$;

-- ---------------------------------------------------------------------------
-- 3. FKs (guarded — only if the referenced tables exist)
-- ---------------------------------------------------------------------------
DO $$
BEGIN
  IF to_regclass('public.academic_years') IS NOT NULL
     AND NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_periods_year') THEN
    ALTER TABLE public.teacher_periods
      ADD CONSTRAINT fk_periods_year FOREIGN KEY (academic_year_id)
      REFERENCES public.academic_years(id) ON DELETE SET NULL;
  END IF;

  IF to_regclass('public.teacher_subject_assignments') IS NOT NULL
     AND NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_periods_assignment') THEN
    ALTER TABLE public.teacher_periods
      ADD CONSTRAINT fk_periods_assignment FOREIGN KEY (assignment_id)
      REFERENCES public.teacher_subject_assignments(id) ON DELETE SET NULL;
  END IF;
END $$;

-- ---------------------------------------------------------------------------
-- 4. Best-effort backfill of assignment_id from the legacy display tuple.
--    Only touches rows where assignment_id IS NULL, so it is idempotent.
-- ---------------------------------------------------------------------------
DO $$
BEGIN
  IF to_regclass('public.teacher_subject_assignments') IS NOT NULL THEN
    UPDATE public.teacher_periods p
       SET assignment_id = tsa.id
      FROM public.teacher_subject_assignments tsa
     WHERE p.assignment_id IS NULL
       AND tsa.school_id   = p.school_id
       AND tsa.teacher_id  = p.teacher_id
       AND tsa.class_name  = p.class_name
       AND tsa.section     = p.section
       AND tsa.subject     = p.subject
       AND tsa.is_active   = true;
    RAISE NOTICE 'teacher_periods.assignment_id backfilled from TSA where matchable.';
  END IF;
END $$;

-- ---------------------------------------------------------------------------
-- 5. No-double-book unique index (idempotent)
-- ---------------------------------------------------------------------------
CREATE UNIQUE INDEX IF NOT EXISTS ux_periods_no_double_book
  ON public.teacher_periods (school_id, teacher_id, weekday, start_time);

-- ---------------------------------------------------------------------------
-- 6. period_exceptions (NEW) — Doc 05 §2.2
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.period_exceptions (
  id                     uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  school_id              uuid NOT NULL,
  period_id              uuid,                       -- NULL for EXTRA periods
  date                   date NOT NULL,
  kind                   varchar(16) NOT NULL,       -- CANCELLED|RESCHEDULED|ROOM_CHANGE|SUBSTITUTION|EXTRA
  new_start              time,
  new_end                time,
  new_room               text,
  substitute_teacher_id  uuid,
  assignment_id          uuid,
  note                   text NOT NULL DEFAULT '',
  created_at             timestamptz NOT NULL DEFAULT now(),
  updated_at             timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_period_exceptions_unique
  ON public.period_exceptions (school_id, period_id, date, kind);

DO $$
BEGIN
  IF to_regclass('public.teacher_periods') IS NOT NULL
     AND NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_pexc_period') THEN
    ALTER TABLE public.period_exceptions
      ADD CONSTRAINT fk_pexc_period FOREIGN KEY (period_id)
      REFERENCES public.teacher_periods(id) ON DELETE CASCADE;
  END IF;

  IF to_regclass('public.teacher_subject_assignments') IS NOT NULL
     AND NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_pexc_assignment') THEN
    ALTER TABLE public.period_exceptions
      ADD CONSTRAINT fk_pexc_assignment FOREIGN KEY (assignment_id)
      REFERENCES public.teacher_subject_assignments(id) ON DELETE SET NULL;
  END IF;
END $$;

COMMIT;

-- =============================================================================
-- POST-RUN REPORT — periods that could NOT be bound to an assignment.
-- These keep working off the legacy display columns; bind them by creating the
-- matching teacher_subject_assignments row (or fixing the className/section/
-- subject typo) and re-running the section-4 backfill.
-- =============================================================================
SELECT
  p.id, p.teacher_id, p.weekday, p.start_time, p.end_time,
  p.class_name, p.section, p.subject
FROM public.teacher_periods p
WHERE p.assignment_id IS NULL
ORDER BY p.teacher_id, p.weekday, p.start_time;
