-- =====================================================================
-- migration_006_parent_link_review_fields.sql
--
-- ISSUE 2c/2d — parent->child link redesign + matching buckets.
--
-- Adds the columns the redesigned link step and the match classifier need on
-- parent_child_links, and widens the status column so it can hold the new
-- "needs_review" value alongside pending | approved | rejected.
--
-- SAFETY / IDEMPOTENCY:
--   * ADD COLUMN ... IF NOT EXISTS for every new column (safe to re-run).
--   * The status widen uses ALTER TYPE only when the current length is too
--     small, so re-running is a no-op. Guarded with a DO block + catalog check
--     so it never errors if the column is already wide enough (or already text).
--   * Runs in one transaction.
-- =====================================================================

BEGIN;

-- New claim/review columns (ISSUE 2c/2d).
ALTER TABLE parent_child_links ADD COLUMN IF NOT EXISTS class_name    text;
ALTER TABLE parent_child_links ADD COLUMN IF NOT EXISTS section       varchar(16);
ALTER TABLE parent_child_links ADD COLUMN IF NOT EXISTS parent_phone  varchar(32);
ALTER TABLE parent_child_links ADD COLUMN IF NOT EXISTS review_reason text;

-- Widen status varchar(16) -> varchar(24) so "needs_review" fits. Only alter
-- when the column is currently a length-limited varchar narrower than 24
-- (idempotent: re-running, or a text column, is a no-op).
DO $widen$
DECLARE cur_len integer;
BEGIN
    SELECT character_maximum_length INTO cur_len
    FROM information_schema.columns
    WHERE table_name = 'parent_child_links' AND column_name = 'status';

    IF cur_len IS NOT NULL AND cur_len < 24 THEN
        ALTER TABLE parent_child_links ALTER COLUMN status TYPE varchar(24);
    END IF;
END
$widen$;

-- Helpful index for the admin queue's per-status filtering.
CREATE INDEX IF NOT EXISTS ix_pcl_school_status
    ON parent_child_links (school_id, status);

COMMIT;
