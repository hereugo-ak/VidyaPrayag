-- ============================================================================
-- schema-patch-school-onboarding.sql
--
-- Idempotent patch that brings the `schools` table up to the full set of
-- columns the school-onboarding wizard collects, and adds an explicit
-- `onboarding_status` lifecycle column alongside the existing `onboarded_at`
-- timestamp.
--
-- Design notes
-- ------------
--   * Every statement is `IF NOT EXISTS` so this is safe to run any number of
--     times against an already-provisioned database (Supabase → SQL Editor).
--   * `onboarded_at` already exists in the base schema (vidyasetu_schema.sql)
--     and remains the canonical "completed" signal that the server stamps on
--     the final REVIEW submit / POST /onboarding/complete. `onboarding_status`
--     is a denormalised, human-readable mirror ('pending' → 'active') the
--     server keeps in lock-step so dashboards/queries can filter on a single
--     column without a NULL-check on a timestamp.
--   * The richer identity/academic fields below were previously only persisted
--     in `school_onboarding_drafts` (key/value). Promoting them to first-class,
--     nullable columns lets the dashboard read them directly without replaying
--     drafts. All nullable → no backfill required for legacy rows.
--
-- Run order: after docs/db/vidyasetu_schema.sql (and the existing migrations).
-- ============================================================================

-- Explicit lifecycle status. 'pending' until the admin completes onboarding,
-- then 'active'. NOT NULL with a default so every existing row reads 'pending'
-- (or is immediately corrected by the server on the next status read/complete).
ALTER TABLE public.schools
    ADD COLUMN IF NOT EXISTS onboarding_status text NOT NULL DEFAULT 'pending';

-- Step 1 — School identity (extra fields beyond name/board/medium).
ALTER TABLE public.schools
    ADD COLUMN IF NOT EXISTS school_type text NULL;          -- Government | Private Aided | Private Unaided | Central
ALTER TABLE public.schools
    ADD COLUMN IF NOT EXISTS affiliation_number text NULL;   -- e.g. UP/CBSE/2021/4421
ALTER TABLE public.schools
    ADD COLUMN IF NOT EXISTS year_established integer NULL;

-- Step 3 — Contact (principal_name/phone/email already exist in base schema).
ALTER TABLE public.schools
    ADD COLUMN IF NOT EXISTS website text NULL;

-- Step 4 — Academics.
ALTER TABLE public.schools
    ADD COLUMN IF NOT EXISTS total_students integer NULL;
ALTER TABLE public.schools
    ADD COLUMN IF NOT EXISTS total_classes integer NULL;
ALTER TABLE public.schools
    ADD COLUMN IF NOT EXISTS academic_year_start_month text NULL;  -- e.g. 'April'
ALTER TABLE public.schools
    ADD COLUMN IF NOT EXISTS grading_system text NULL;             -- Percentage | CGPA | Grades

-- Backfill: any school that already has onboarded_at stamped is 'active'.
UPDATE public.schools
    SET onboarding_status = 'active'
    WHERE onboarded_at IS NOT NULL AND onboarding_status <> 'active';

-- Helpful index for "list pending onboardings" ops queries (idempotent).
CREATE INDEX IF NOT EXISTS ix_schools_onboarding_status
    ON public.schools (onboarding_status);
