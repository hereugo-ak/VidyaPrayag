-- =============================================================================
-- Migration 004 — parent_achievements
--
-- WHY THIS EXISTS
--   The rebuilt Parents Portal **Profile tab** ships a swipe-down
--   "Missions & Achievements" sheet (badges, NEP-aligned academic
--   competencies, emotional-intelligence metrics, play & discovery missions).
--
--   That sheet is served by GET /api/v1/parent/track-progress, which ALREADY
--   works without this table — it falls back to the app_config CMS templates
--   plus locally-derived achievements. This migration is the OPTIONAL upgrade
--   path for schools/operators who want to store REAL, per-child earned
--   achievements instead of CMS-wide templates.
--
-- HOW TO RUN
--   Supabase -> SQL Editor -> paste this whole file -> Run.
--   100% SAFE TO RE-RUN: every statement is guarded with IF NOT EXISTS, so
--   running it against a database that already has the table / columns /
--   indexes is a harmless no-op and NEVER raises an error.
--
-- The column names / types below match server/.../db/Tables.kt
-- (ParentAchievementsTable) EXACTLY so the Exposed mapping lines up with the
-- real Postgres schema.
--
-- `kind` discriminates the row so one table powers every section of the sheet:
--   BADGE       -> a collectible achievement badge       (icon + colours + earned/locked)
--   COMPETENCY  -> a NEP-aligned academic competency bar (title + 0..1 progress)
--   EI_METRIC   -> an emotional-intelligence metric       (title + 0..1 value)
--   MISSION     -> a play & discovery milestone           (MET | IN_PROGRESS | LOCKED)
-- =============================================================================

CREATE TABLE IF NOT EXISTS public.parent_achievements (
  id          uuid NOT NULL,
  child_id    uuid NOT NULL,                 -- FK children.id — whose achievement
  school_id   uuid NULL,                     -- FK schools.id (optional tenant scope)
  kind        varchar(16) NOT NULL,          -- BADGE | COMPETENCY | EI_METRIC | MISSION
  title       text NOT NULL,
  description text NULL,
  icon        varchar(64) NULL,              -- Material symbol name (e.g. "verified")
  colors      text NOT NULL DEFAULT '[]',    -- JSON array of hex stops, e.g. ["#B6C7EB","#006C49"]
  progress    double precision NULL,         -- 0..1 for COMPETENCY / EI_METRIC; NULL otherwise
  status      varchar(16) NOT NULL DEFAULT 'LOCKED', -- EARNED | LOCKED | MET | IN_PROGRESS
  is_locked   boolean NOT NULL DEFAULT true,
  sort_order  integer NOT NULL DEFAULT 0,
  created_at  timestamp without time zone NOT NULL DEFAULT now(),
  updated_at  timestamp without time zone NOT NULL DEFAULT now(),
  CONSTRAINT parent_achievements_pkey PRIMARY KEY (id)
) TABLESPACE pg_default;

-- Re-assert every column so the migration also "heals" a partially-created table
-- (e.g. an earlier draft). Each is a no-op when the column already exists.
ALTER TABLE public.parent_achievements ADD COLUMN IF NOT EXISTS school_id   uuid NULL;
ALTER TABLE public.parent_achievements ADD COLUMN IF NOT EXISTS description text NULL;
ALTER TABLE public.parent_achievements ADD COLUMN IF NOT EXISTS icon        varchar(64) NULL;
ALTER TABLE public.parent_achievements ADD COLUMN IF NOT EXISTS colors      text NOT NULL DEFAULT '[]';
ALTER TABLE public.parent_achievements ADD COLUMN IF NOT EXISTS progress    double precision NULL;
ALTER TABLE public.parent_achievements ADD COLUMN IF NOT EXISTS status      varchar(16) NOT NULL DEFAULT 'LOCKED';
ALTER TABLE public.parent_achievements ADD COLUMN IF NOT EXISTS is_locked   boolean NOT NULL DEFAULT true;
ALTER TABLE public.parent_achievements ADD COLUMN IF NOT EXISTS sort_order  integer NOT NULL DEFAULT 0;
ALTER TABLE public.parent_achievements ADD COLUMN IF NOT EXISTS created_at  timestamp without time zone NOT NULL DEFAULT now();
ALTER TABLE public.parent_achievements ADD COLUMN IF NOT EXISTS updated_at  timestamp without time zone NOT NULL DEFAULT now();

-- Indexes (read path: "all achievements for a child", optionally filtered by kind).
CREATE INDEX IF NOT EXISTS ix_parent_achievements_child
  ON public.parent_achievements (child_id);
CREATE INDEX IF NOT EXISTS ix_parent_achievements_child_kind
  ON public.parent_achievements (child_id, kind);

-- OPTIONAL seed (commented out by default — uncomment to give a child a starter set).
-- Replace <CHILD_UUID> with a real children.id.
--
-- INSERT INTO public.parent_achievements (id, child_id, kind, title, description, icon, colors, progress, status, is_locked, sort_order)
-- VALUES
--   (gen_random_uuid(), '<CHILD_UUID>', 'BADGE',      'Social Star',  NULL,                       'workspace_premium', '["#B6C7EB","#006C49"]', NULL, 'EARNED',      false, 0),
--   (gen_random_uuid(), '<CHILD_UUID>', 'COMPETENCY', 'Literacy',     NULL,                       'translate',         '[]',                    0.85, 'EARNED',      false, 0),
--   (gen_random_uuid(), '<CHILD_UUID>', 'COMPETENCY', 'Numeracy',     NULL,                       'calculate',         '[]',                    0.78, 'EARNED',      false, 1),
--   (gen_random_uuid(), '<CHILD_UUID>', 'EI_METRIC',  'Empathy',      NULL,                       NULL,                '[]',                    0.80, 'EARNED',      false, 0),
--   (gen_random_uuid(), '<CHILD_UUID>', 'MISSION',    'Agility',      'Gross motor met',          NULL,                '[]',                    NULL, 'MET',         false, 0),
--   (gen_random_uuid(), '<CHILD_UUID>', 'MISSION',    'Curiosity',    'Exploration in progress',  NULL,                '[]',                    NULL, 'IN_PROGRESS', true,  1)
-- ON CONFLICT (id) DO NOTHING;
