-- =============================================================================
-- Migration 010 — typed dates everywhere
--                 (attendance / assessment / homework / syllabus / calendar)
--
-- WHY THIS EXISTS
--   Teacher Portal Rebuild — Doc 11 task T-004 (foundation). Closes root defect
--   X-3 (string dates) and the per-feature date defects D-ATT-3, D-ASMT-2,
--   D-HW-2, D-SYL-2.
--
--   These six columns are semantically DATES but are stored as varchar(12)
--   "YYYY-MM-DD" text:
--     • attendance_records.date
--     • assessments.exam_date        (nullable)
--     • homework.due_date
--     • syllabus_units.covered_on     (nullable)
--     • calendar_events.start_date
--     • calendar_events.end_date
--
--   String dates mean every read site must LocalDate.parse(), every range
--   query is a lexical string compare (only correct because the format is
--   zero-padded ISO), ordering is string ordering, and a malformed value
--   silently breaks a screen instead of failing loudly. Promoting them to the
--   native `date` type makes the database the source of truth for date
--   semantics: real ordering, real range predicates, real NULL handling, and a
--   hard guarantee that no non-date text can ever enter these columns again.
--
-- WHAT THIS DOES (per column)
--   1. Quarantine: copy any row whose current text is NOT a valid ISO date
--      into a side table `vp_bad_dates_010` (so nothing is destroyed) and NULL
--      it out first when the column is nullable, or report it when it is NOT
--      nullable (NOT-NULL columns with bad text abort the migration loudly —
--      they must be triaged by hand; see the report at the end).
--   2. Convert: ALTER ... TYPE date USING (trim(col))::date — only runs once
--      the column's live values are all valid (guarded by information_schema so
--      a re-run on an already-`date` column is a no-op).
--
-- HOW TO RUN
--   Supabase -> SQL Editor -> paste this whole file -> Run.
--   Safe to re-run: every ALTER TYPE is guarded by an information_schema check
--   that skips the column if it is already `date`; the quarantine INSERTs use
--   ON CONFLICT DO NOTHING; the bad-date table is CREATE TABLE IF NOT EXISTS.
--
-- RUN ORDER (appended to the existing chain):
--   ...
--   8. docs/db/migration_008_enrollments.sql
--   9. docs/db/migration_009_tsa_fks.sql
--  10. docs/db/migration_010_typed_dates.sql                <-- this file
--
-- Column names/types are kept in lock-step with
-- server/.../db/Tables.kt (date() mappings land in the same commit as T-004).
--
-- ─────────────────────────────────────────────────────────────────────────────
-- NOTE (flagged per LAWS — docs are authority):
--   Doc 11 T-004 names the file `migration_012_typed_dates.sql`. The real
--   migration chain lives in docs/db/ and the previous (T-002) migration is
--   009, so this is placed at migration_010 to keep the sequence contiguous and
--   inside the PROVISION run-order. Same deviation pattern already documented
--   on migration_009 (doc said 011).
-- =============================================================================

BEGIN;

-- ─────────────────────────────────────────────────────────────────────────────
-- 0) Helpers.
--    vp_is_iso_date(text): true when the trimmed text is a valid YYYY-MM-DD
--    date (uses a real cast under a sub-transaction so e.g. '2026-02-31' is
--    rejected, not just regex-matched).
-- ─────────────────────────────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION vp_is_iso_date(raw text)
RETURNS boolean LANGUAGE plpgsql IMMUTABLE AS $$
BEGIN
    IF raw IS NULL THEN
        RETURN false;            -- NULL handled by caller; here we test text
    END IF;
    IF btrim(raw) !~ '^\d{4}-\d{2}-\d{2}$' THEN
        RETURN false;
    END IF;
    -- Real calendar validity (rejects 2026-13-01, 2026-02-31, …).
    PERFORM (btrim(raw))::date;
    RETURN true;
EXCEPTION WHEN others THEN
    RETURN false;
END $$;

-- Quarantine table — preserves every row we cannot convert, with provenance.
CREATE TABLE IF NOT EXISTS public.vp_bad_dates_010 (
    table_name  text        NOT NULL,
    column_name text        NOT NULL,
    row_id      text        NOT NULL,
    bad_value   text,
    noted_at    timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (table_name, column_name, row_id)
);

-- ─────────────────────────────────────────────────────────────────────────────
-- 1) NULLABLE columns: quarantine + NULL-out bad text, then convert.
--    assessments.exam_date, syllabus_units.covered_on.
-- ─────────────────────────────────────────────────────────────────────────────

-- assessments.exam_date ------------------------------------------------------
INSERT INTO public.vp_bad_dates_010 (table_name, column_name, row_id, bad_value)
SELECT 'assessments', 'exam_date', id::text, exam_date
FROM public.assessments
WHERE exam_date IS NOT NULL AND NOT vp_is_iso_date(exam_date)
ON CONFLICT DO NOTHING;

UPDATE public.assessments
SET exam_date = NULL
WHERE exam_date IS NOT NULL AND NOT vp_is_iso_date(exam_date);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema='public' AND table_name='assessments'
          AND column_name='exam_date' AND data_type <> 'date'
    ) THEN
        ALTER TABLE public.assessments
            ALTER COLUMN exam_date TYPE date
            USING (NULLIF(btrim(exam_date), ''))::date;
    END IF;
END $$;

-- syllabus_units.covered_on --------------------------------------------------
INSERT INTO public.vp_bad_dates_010 (table_name, column_name, row_id, bad_value)
SELECT 'syllabus_units', 'covered_on', id::text, covered_on
FROM public.syllabus_units
WHERE covered_on IS NOT NULL AND NOT vp_is_iso_date(covered_on)
ON CONFLICT DO NOTHING;

UPDATE public.syllabus_units
SET covered_on = NULL
WHERE covered_on IS NOT NULL AND NOT vp_is_iso_date(covered_on);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema='public' AND table_name='syllabus_units'
          AND column_name='covered_on' AND data_type <> 'date'
    ) THEN
        ALTER TABLE public.syllabus_units
            ALTER COLUMN covered_on TYPE date
            USING (NULLIF(btrim(covered_on), ''))::date;
    END IF;
END $$;

-- ─────────────────────────────────────────────────────────────────────────────
-- 2) NOT-NULL columns: quarantine bad rows for review, then convert ONLY if no
--    invalid live values remain. If invalid text is present in a NOT-NULL
--    column we RAISE EXCEPTION (abort the whole migration) rather than silently
--    coerce — the data must be triaged by a human (see vp_bad_dates_010).
--    attendance_records.date, homework.due_date,
--    calendar_events.start_date, calendar_events.end_date.
-- ─────────────────────────────────────────────────────────────────────────────

-- attendance_records.date ----------------------------------------------------
INSERT INTO public.vp_bad_dates_010 (table_name, column_name, row_id, bad_value)
SELECT 'attendance_records', 'date', id::text, date
FROM public.attendance_records
WHERE NOT vp_is_iso_date(date)
ON CONFLICT DO NOTHING;

DO $$
DECLARE v_bad bigint;
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema='public' AND table_name='attendance_records'
          AND column_name='date' AND data_type <> 'date'
    ) THEN
        SELECT count(*) INTO v_bad FROM public.attendance_records
        WHERE NOT vp_is_iso_date(date);
        IF v_bad > 0 THEN
            RAISE EXCEPTION
              'migration_010: % attendance_records.date value(s) are not ISO dates. They have been logged in vp_bad_dates_010 — fix/delete them, then re-run.', v_bad;
        END IF;
        ALTER TABLE public.attendance_records
            ALTER COLUMN date TYPE date USING (btrim(date))::date;
    END IF;
END $$;

-- homework.due_date ----------------------------------------------------------
INSERT INTO public.vp_bad_dates_010 (table_name, column_name, row_id, bad_value)
SELECT 'homework', 'due_date', id::text, due_date
FROM public.homework
WHERE NOT vp_is_iso_date(due_date)
ON CONFLICT DO NOTHING;

DO $$
DECLARE v_bad bigint;
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema='public' AND table_name='homework'
          AND column_name='due_date' AND data_type <> 'date'
    ) THEN
        SELECT count(*) INTO v_bad FROM public.homework
        WHERE NOT vp_is_iso_date(due_date);
        IF v_bad > 0 THEN
            RAISE EXCEPTION
              'migration_010: % homework.due_date value(s) are not ISO dates. Logged in vp_bad_dates_010 — fix/delete them, then re-run.', v_bad;
        END IF;
        ALTER TABLE public.homework
            ALTER COLUMN due_date TYPE date USING (btrim(due_date))::date;
    END IF;
END $$;

-- calendar_events.start_date / end_date --------------------------------------
INSERT INTO public.vp_bad_dates_010 (table_name, column_name, row_id, bad_value)
SELECT 'calendar_events', 'start_date', id::text, start_date
FROM public.calendar_events
WHERE NOT vp_is_iso_date(start_date)
ON CONFLICT DO NOTHING;

INSERT INTO public.vp_bad_dates_010 (table_name, column_name, row_id, bad_value)
SELECT 'calendar_events', 'end_date', id::text, end_date
FROM public.calendar_events
WHERE NOT vp_is_iso_date(end_date)
ON CONFLICT DO NOTHING;

DO $$
DECLARE v_bad_start bigint; v_bad_end bigint;
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema='public' AND table_name='calendar_events'
          AND column_name='start_date' AND data_type <> 'date'
    ) THEN
        SELECT count(*) INTO v_bad_start FROM public.calendar_events
        WHERE NOT vp_is_iso_date(start_date);
        IF v_bad_start > 0 THEN
            RAISE EXCEPTION
              'migration_010: % calendar_events.start_date value(s) are not ISO dates. Logged in vp_bad_dates_010 — fix/delete them, then re-run.', v_bad_start;
        END IF;
        ALTER TABLE public.calendar_events
            ALTER COLUMN start_date TYPE date USING (btrim(start_date))::date;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema='public' AND table_name='calendar_events'
          AND column_name='end_date' AND data_type <> 'date'
    ) THEN
        SELECT count(*) INTO v_bad_end FROM public.calendar_events
        WHERE NOT vp_is_iso_date(end_date);
        IF v_bad_end > 0 THEN
            RAISE EXCEPTION
              'migration_010: % calendar_events.end_date value(s) are not ISO dates. Logged in vp_bad_dates_010 — fix/delete them, then re-run.', v_bad_end;
        END IF;
        ALTER TABLE public.calendar_events
            ALTER COLUMN end_date TYPE date USING (btrim(end_date))::date;
    END IF;
END $$;

-- ─────────────────────────────────────────────────────────────────────────────
-- 3) Bad-row report (T-004 "Done when": bad-row report empty or triaged).
-- ─────────────────────────────────────────────────────────────────────────────
DO $$
DECLARE v_quarantined bigint;
BEGIN
    SELECT count(*) INTO v_quarantined FROM public.vp_bad_dates_010;
    IF v_quarantined = 0 THEN
        RAISE NOTICE 'migration_010: all six date columns converted to `date`; quarantine table empty (clean).';
    ELSE
        RAISE NOTICE 'migration_010: % row(s) quarantined in vp_bad_dates_010 (nullable columns were NULLed; review with the SELECT below).', v_quarantined;
    END IF;
END $$;

-- Review quarantined values (run on demand):
--   SELECT * FROM public.vp_bad_dates_010 ORDER BY table_name, column_name;

COMMIT;
