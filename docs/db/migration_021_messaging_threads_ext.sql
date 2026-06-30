-- =============================================================================
-- Migration 021 — message_threads extensions: is_muted, is_pinned, is_archived,
--                 draft_body
--
-- WHY THIS EXISTS
--   Phase 1 of the messaging system spec (MESSAGING_SYSTEM_SPEC.md §8.1, §21).
--   Adds thread-level preference columns so the client can mute, pin, and
--   archive conversations, plus a server-side draft_body for cross-device draft
--   sync (Phase 4 uses these; the columns are added now so the schema is stable).
--
-- WHAT THIS DOES (all additive / idempotent / non-destructive)
--   1. ALTERs message_threads: ADD is_muted, is_pinned, is_archived, draft_body.
--      All have safe defaults so existing rows are valid immediately.
--   2. CREATEs idx_threads_owner_school_pinned for the thread-list query that
--      sorts pinned threads first.
--
-- WHAT THIS DOES NOT DO
--   • Does NOT drop or alter existing columns.
--   • Does NOT populate draft_body — that is client-driven.
--
-- HOW TO RUN
--   Supabase -> SQL Editor -> paste this whole file -> Run.
--   Safe to re-run: every statement is guarded.
--
-- ROLLBACK:
--   DROP INDEX IF EXISTS idx_threads_owner_school_pinned;
--   ALTER TABLE message_threads DROP COLUMN IF EXISTS draft_body;
--   ALTER TABLE message_threads DROP COLUMN IF EXISTS is_archived;
--   ALTER TABLE message_threads DROP COLUMN IF EXISTS is_pinned;
--   ALTER TABLE message_threads DROP COLUMN IF EXISTS is_muted;
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1) Add preference columns to message_threads
-- ---------------------------------------------------------------------------
ALTER TABLE public.message_threads
  ADD COLUMN IF NOT EXISTS is_muted    boolean NOT NULL DEFAULT false;
ALTER TABLE public.message_threads
  ADD COLUMN IF NOT EXISTS is_pinned   boolean NOT NULL DEFAULT false;
ALTER TABLE public.message_threads
  ADD COLUMN IF NOT EXISTS is_archived boolean NOT NULL DEFAULT false;
ALTER TABLE public.message_threads
  ADD COLUMN IF NOT EXISTS draft_body  text NULL;

-- ---------------------------------------------------------------------------
-- 2) Index for thread-list query (owner + school + pinned-first ordering)
-- ---------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_threads_owner_school_pinned
  ON public.message_threads (owner_user_id, school_id, is_pinned);
