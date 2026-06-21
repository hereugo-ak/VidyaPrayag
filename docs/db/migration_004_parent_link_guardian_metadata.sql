-- =============================================================================
-- Migration 004 — first-class parent relationship metadata on parent_child_links
--
-- WHY THIS EXISTS
--   The Student Management redesign (RA-SP) makes the Student ↔ Parent
--   relationship a first-class citizen: a student may have MULTIPLE parents but
--   AT MOST ONE primary guardian. StudentAggregationService derives the student
--   profile's parent list (name / relation / primary-guardian / phone) from
--   parent_child_links, and enforces the "single primary guardian per student"
--   invariant on parent-link approval (ParentLinkRouting.kt).
--
--   Two NEW COLUMNS back this and are now mapped in
--   server/.../db/Tables.kt -> ParentChildLinksTable:
--     • relation            VARCHAR(32)  NULL    -- Father | Mother | Guardian | ...
--     • is_primary_guardian BOOLEAN      NOT NULL DEFAULT FALSE
--
--   AUTO_CREATE_TABLES is OFF in production (DatabaseFactory.kt runs NO migration
--   against Postgres — only SQLite dev gets createMissingTablesAndColumns).
--   Without this migration the backend fails on Supabase/Render with:
--       column "is_primary_guardian" of relation "parent_child_links" does not exist
--       column "relation" of relation "parent_child_links" does not exist
--
-- HOW TO RUN
--   Supabase -> SQL Editor -> paste this whole file -> Run.
--   Safe to re-run: every statement is guarded (ADD COLUMN IF NOT EXISTS).
--   It only ADDS columns (one nullable, one with a safe default); it never
--   drops, rewrites, or backfills data. Existing rows keep parsing unchanged
--   (relation = NULL, is_primary_guardian = FALSE).
--
-- RUN ORDER (full provisioning):
--   1. vidyasetu_schema.sql
--   2. migration_001_faculty_and_holiday_list.sql
--   3. migration_002_segmentation_geo_assignments.sql
--   4. migration_003_leave_workflow_and_two_party_messaging.sql
--   5. migration_004_parent_link_guardian_metadata.sql           <-- this file
--
-- Column names/types match server/.../db/Tables.kt EXACTLY.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1) parent_child_links — guardian relationship metadata  (RA-SP)
--    `relation` describes the guardian role; `is_primary_guardian` marks the
--    single primary point-of-contact for the student. The aggregation service
--    enforces AT MOST ONE primary guardian per (school_id, student_code).
-- ---------------------------------------------------------------------------
ALTER TABLE parent_child_links
    ADD COLUMN IF NOT EXISTS relation VARCHAR(32);

ALTER TABLE parent_child_links
    ADD COLUMN IF NOT EXISTS is_primary_guardian BOOLEAN NOT NULL DEFAULT FALSE;

-- Helper index: the single-primary-guardian enforcement + the profile parent
-- list both filter by (school_id, student_code, status). This keeps those
-- school-scoped lookups fast as the link table grows.
CREATE INDEX IF NOT EXISTS ix_pcl_school_student_status
    ON parent_child_links (school_id, student_code, status);
