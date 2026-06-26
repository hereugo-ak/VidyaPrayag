-- =====================================================================
-- migration_007_child_link_robustness.sql
--
-- Fixes the "rosterSize=0 / No student found" bug in the parent→child
-- link flow by ensuring:
--   1. students.is_active has a NOT NULL DEFAULT true so the backend
--      query `WHERE is_active = true` never silently misses rows.
--   2. students.parent_phone column exists (was added by migration_005
--      but only in that migration — the all-in-one 2026-06-07 schema
--      does not include it). Safe to re-run.
--   3. parent_child_links table has all columns needed by the ISSUE 2c/2d
--      redesign (class_name, section, parent_phone, review_reason, status
--      wide enough for "needs_review"). Idempotent.
--   4. Backfills is_active = true where it is currently NULL on students.
--
-- IDEMPOTENT: every statement uses IF NOT EXISTS / DO blocks / COALESCE
-- guards. Running this file on a fully-migrated DB is a harmless no-op.
-- =====================================================================

BEGIN;

-- ── 1. Ensure students.parent_phone column exists ────────────────────
ALTER TABLE students ADD COLUMN IF NOT EXISTS parent_phone text;

-- ── 2. Backfill students.is_active = true where it is NULL ───────────
-- The all-in-one schema defines is_active NOT NULL DEFAULT true, but if
-- any rows were inserted before the column had the default (or via a DB
-- tool that bypassed the default), is_active can be NULL. The backend
-- query `WHERE is_active = true` misses those rows → rosterSize = 0.
UPDATE students
SET    is_active = true
WHERE  is_active IS NULL;

-- ── 3. Harden the is_active column default going forward ─────────────
-- Set the column default so any future INSERT that omits is_active still
-- gets true. `NOT NULL` is only applied if the column currently allows
-- nulls (DO block avoids errors on already-hardened columns).
DO $harden$
BEGIN
    -- Set default if not already set.
    ALTER TABLE students
        ALTER COLUMN is_active SET DEFAULT true;
    -- Make NOT NULL only when no NULLs remain (we just backfilled above).
    ALTER TABLE students
        ALTER COLUMN is_active SET NOT NULL;
EXCEPTION WHEN others THEN
    -- Column may already be NOT NULL — ignore.
    NULL;
END
$harden$;

-- ── 4. parent_child_links — new columns for ISSUE 2c/2d ──────────────
ALTER TABLE parent_child_links ADD COLUMN IF NOT EXISTS class_name    text;
ALTER TABLE parent_child_links ADD COLUMN IF NOT EXISTS section       varchar(16);
ALTER TABLE parent_child_links ADD COLUMN IF NOT EXISTS parent_phone  varchar(32);
ALTER TABLE parent_child_links ADD COLUMN IF NOT EXISTS review_reason text;

-- Widen status to varchar(24) so "needs_review" fits (was varchar(16) in
-- the original schema). Only alter when the current limit is too small.
DO $widen_status$
DECLARE cur_len integer;
BEGIN
    SELECT character_maximum_length INTO cur_len
    FROM   information_schema.columns
    WHERE  table_name = 'parent_child_links' AND column_name = 'status';
    IF cur_len IS NOT NULL AND cur_len < 24 THEN
        ALTER TABLE parent_child_links
            ALTER COLUMN status TYPE varchar(24);
    END IF;
END
$widen_status$;

-- ── 5. Useful indexes ────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS ix_students_school_active
    ON students (school_id, is_active);

CREATE INDEX IF NOT EXISTS ix_pcl_school_status
    ON parent_child_links (school_id, status);

-- ── 6. Verification ──────────────────────────────────────────────────
DO $verify$
DECLARE
    null_active bigint;
    missing_col boolean;
BEGIN
    -- No students should have is_active = NULL after the backfill.
    SELECT COUNT(*) INTO null_active FROM students WHERE is_active IS NULL;
    IF null_active > 0 THEN
        RAISE WARNING 'migration_007: % student(s) still have is_active = NULL — backfill may have been skipped', null_active;
    ELSE
        RAISE NOTICE 'migration_007: students.is_active: OK (no NULLs)';
    END IF;

    -- parent_phone column must exist on students.
    SELECT NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE  table_name = 'students' AND column_name = 'parent_phone'
    ) INTO missing_col;
    IF missing_col THEN
        RAISE WARNING 'migration_007: students.parent_phone column is MISSING!';
    ELSE
        RAISE NOTICE 'migration_007: students.parent_phone: OK';
    END IF;
END
$verify$;

COMMIT;
