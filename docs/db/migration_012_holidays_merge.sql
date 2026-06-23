-- =============================================================================
-- Migration 012 — single holiday source: merge holiday_list → calendar_events
--
-- WHY THIS EXISTS
--   Teacher Portal Rebuild — Doc 11 task T-102 (Phase 1, schedule spine).
--   Doc 05 §2.3 / §3.3. Closes D-TT-5 (holiday duplication).
--
--   There are TWO sources of holiday truth in the schema:
--     • holiday_list           — the legacy, simple (date,title,type,frequency)
--                                table read by the parent academics + school
--                                holiday screens.
--     • calendar_events(HOLIDAY)— the richer VP-CAL event model (typed dates,
--                                DRAFT/PUBLISHED lifecycle, audience).
--   The resolved-day computation (T-104) must read holidays from EXACTLY ONE
--   place. Doc 05 §2.3 makes `calendar_events(type=HOLIDAY, status=PUBLISHED)`
--   the single source and deprecates holiday_list.
--
-- WHAT THIS DOES
--   For every holiday_list row that does NOT already have a matching PUBLISHED
--   calendar_events(HOLIDAY) for the same (school, date), insert one:
--     • type   = 'HOLIDAY'
--     • status = 'PUBLISHED'      (legacy holidays were always "live")
--     • source = 'MANUAL'         (admin-authored; no announcement origin)
--     • source_ref = 'HL:' || holiday_list.id   (idempotency key — a re-run
--                                  never duplicates because we check this ref)
--     • start_date = end_date = the holiday's date (single-day)
--     • all_day = true, audience = 'ALL_SCHOOL'
--     • title carried over; icon defaults to a calendar/holiday glyph.
--
--   The holiday_list table itself is LEFT IN PLACE (see DEVIATION) so the
--   existing parent/school readers keep compiling+working in this release; this
--   migration only forward-fills the canonical source.
--
-- DEVIATION (flagged per Doc 11 Rule: docs are authority)
--   • Doc 05 §2.3 / Doc 11 T-102 say "deprecate HolidayListTable". This
--     migration does NOT drop the table — it MIGRATES the data into the
--     canonical source and demotes holiday_list to legacy-read-only. Two
--     readers (feature/parent/ParentAcademicsRouting.kt §academics holidays,
--     feature/school/SchoolRouting.kt §holidays) still query holiday_list, so
--     dropping it now would break the build (Rule 4: every commit compiles
--     green). The Exposed mapping (Tables.kt) is annotated @Deprecated. A later
--     phase repoints those two readers at calendar_events and then drops the
--     table.
--   • Filename: Doc 11 names this `migration_014_holidays_merge.sql`. The
--     on-disk chain is contiguous and the prior migration is `011`, so this file
--     is `migration_012_holidays_merge.sql` (same documented deviation pattern
--     as 009/010/011 — the doc number is a label, not the filename).
--
-- HOW TO RUN
--   Supabase -> SQL Editor -> paste this whole file -> Run.
--   Safe to re-run: the INSERT is guarded by a NOT EXISTS on the source_ref
--   idempotency key, so an already-migrated holiday is a no-op.
--
-- RUN ORDER (appended to the existing chain):
--   ...
--   11. docs/db/migration_011_periods.sql
--   12. docs/db/migration_012_holidays_merge.sql   <-- THIS FILE
-- =============================================================================

BEGIN;

-- ---------------------------------------------------------------------------
-- 0. Safety: both tables must exist. If either is missing this is a no-op.
-- ---------------------------------------------------------------------------
DO $$
DECLARE
  v_inserted integer := 0;
BEGIN
  IF to_regclass('public.holiday_list') IS NULL THEN
    RAISE NOTICE 'holiday_list does not exist — nothing to migrate.';
    RETURN;
  END IF;
  IF to_regclass('public.calendar_events') IS NULL THEN
    RAISE NOTICE 'calendar_events does not exist yet — skipping (Exposed will create it; re-run this migration afterwards).';
    RETURN;
  END IF;

  -- -------------------------------------------------------------------------
  -- 1. Forward-fill calendar_events from holiday_list (idempotent via
  --    source_ref = 'HL:<holiday_list.id>'). Only rows not already migrated.
  --
  --    holiday_list.date is varchar(12) "YYYY-MM-DD"; calendar_events.start_date
  --    / end_date are typed `date` (T-004) — cast at the boundary. Bad/blank
  --    dates are skipped (NULLIF + ::date will raise; we guard per-row in a
  --    sub-block so one bad legacy row cannot abort the whole merge).
  -- -------------------------------------------------------------------------
  DECLARE
    r RECORD;
    v_date date;
  BEGIN
    FOR r IN
      SELECT hl.id, hl.school_id, hl.date, hl.title, hl.type
        FROM public.holiday_list hl
       WHERE NOT EXISTS (
               SELECT 1 FROM public.calendar_events ce
                WHERE ce.school_id  = hl.school_id
                  AND ce.source_ref = 'HL:' || hl.id::text
             )
    LOOP
      -- Tolerant date parse: skip rows whose legacy string isn't a real date.
      BEGIN
        v_date := (NULLIF(trim(r.date), ''))::date;
      EXCEPTION WHEN others THEN
        RAISE NOTICE 'holiday_list % has unparseable date "%" — skipped.', r.id, r.date;
        CONTINUE;
      END;
      IF v_date IS NULL THEN
        RAISE NOTICE 'holiday_list % has blank date — skipped.', r.id;
        CONTINUE;
      END IF;

      INSERT INTO public.calendar_events (
        id, school_id, event_code, academic_year_id,
        title, description, type, status, source, source_ref,
        start_date, end_date, all_day, banner_url, icon,
        audience, class_ids, section_ids,
        notify_students, notify_parents, notify_teachers,
        is_milestone, is_active, created_by, updated_by,
        created_at, updated_at
      ) VALUES (
        gen_random_uuid(), r.school_id,
        'CAL_HL' || upper(substr(replace(r.id::text, '-', ''), 1, 8)),
        (SELECT ay.id FROM public.academic_years ay
          WHERE ay.school_id = r.school_id AND ay.is_active = true
          LIMIT 1),
        COALESCE(NULLIF(trim(r.title), ''), 'Holiday'),
        '', 'HOLIDAY', 'PUBLISHED', 'MANUAL', 'HL:' || r.id::text,
        v_date, v_date, true, NULL, 'beach_access',
        'ALL_SCHOOL', NULL, NULL,
        false, false, false,
        false, true, NULL, NULL,
        now(), now()
      );
      v_inserted := v_inserted + 1;
    END LOOP;
  END;

  RAISE NOTICE 'migration_012: % holiday_list row(s) merged into calendar_events(HOLIDAY,PUBLISHED).', v_inserted;
END $$;

COMMIT;

-- =============================================================================
-- POST-RUN REPORT — the canonical published-holiday view after the merge.
-- The resolved-day computation (T-104) reads holidays ONLY from here.
-- =============================================================================
SELECT
  ce.school_id, ce.start_date AS date, ce.title, ce.source_ref
FROM public.calendar_events ce
WHERE ce.type = 'HOLIDAY'
  AND ce.status = 'PUBLISHED'
  AND ce.is_active = true
ORDER BY ce.school_id, ce.start_date;
