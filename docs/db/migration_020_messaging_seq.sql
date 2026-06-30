-- =============================================================================
-- Migration 020 — messaging seq, client_msg_id, edited_at, deleted_at, reply_to_id
--
-- WHY THIS EXISTS
--   Phase 1 of the messaging system spec (MESSAGING_SYSTEM_SPEC.md §21).
--   Closes gaps G6 (pagination), G7 (edit/delete), G12 (idempotency), G17
--   (reply/threading), and the ordering guarantee (§7.1: server-assigned
--   monotonic seq per conversation).
--
--   The Exposed mappings in server/.../db/Tables.kt reference these columns.
--   AUTO_CREATE_TABLES is OFF in production and validateSchema() gates boot on
--   table/column presence, so this file MUST be applied in Supabase BEFORE the
--   matching backend deploy. Schema-before-features law.
--
-- WHAT THIS DOES (all additive / idempotent / non-destructive)
--   1. ALTERs messages: ADD seq, client_msg_id, edited_at, deleted_at,
--      reply_to_id — all nullable so existing rows stay valid.
--   2. CREATEs idx_messages_conv_seq for delta-sync and pagination queries.
--   3. CREATEs ux_messages_client_msg_id (partial unique) for idempotency.
--   4. BACKFILLS seq via ROW_NUMBER() OVER (PARTITION BY conversation_id
--      ORDER BY created_at) so every existing message gets a monotonic per-
--      conversation sequence. Idempotent: only updates rows WHERE seq IS NULL.
--
-- WHAT THIS DOES NOT DO
--   • Does NOT drop or alter existing columns.
--   • Does NOT make seq NOT NULL yet — legacy rows without conversation_id
--     get seq = NULL and the client falls back to created_at ordering (§19.4).
--
-- HOW TO RUN
--   Supabase -> SQL Editor -> paste this whole file -> Run.
--   Safe to re-run: every statement is guarded.
--
-- VERIFY AFTER RUN
--   SELECT count(*) FROM messages WHERE seq IS NULL;
--   -- Should be 0 (or only rows with NULL conversation_id, which are legacy
--   --   system threads that predate the conversation model).
--
-- ROLLBACK:
--   DROP INDEX IF EXISTS idx_messages_conv_seq;
--   DROP INDEX IF EXISTS ux_messages_client_msg_id;
--   ALTER TABLE messages DROP COLUMN IF EXISTS reply_to_id;
--   ALTER TABLE messages DROP COLUMN IF EXISTS deleted_at;
--   ALTER TABLE messages DROP COLUMN IF EXISTS edited_at;
--   ALTER TABLE messages DROP COLUMN IF EXISTS client_msg_id;
--   ALTER TABLE messages DROP COLUMN IF EXISTS seq;
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1) Add new columns to messages
-- ---------------------------------------------------------------------------
ALTER TABLE public.messages
  ADD COLUMN IF NOT EXISTS seq           integer NULL;
ALTER TABLE public.messages
  ADD COLUMN IF NOT EXISTS client_msg_id uuid NULL;
ALTER TABLE public.messages
  ADD COLUMN IF NOT EXISTS edited_at     timestamp NULL;
ALTER TABLE public.messages
  ADD COLUMN IF NOT EXISTS deleted_at    timestamp NULL;
ALTER TABLE public.messages
  ADD COLUMN IF NOT EXISTS reply_to_id   uuid NULL REFERENCES public.messages(id);

-- ---------------------------------------------------------------------------
-- 2) Indexes
-- ---------------------------------------------------------------------------
-- Delta sync + pagination: WHERE conversation_id=? AND seq>? ORDER BY seq
CREATE INDEX IF NOT EXISTS idx_messages_conv_seq
  ON public.messages (conversation_id, seq);

-- Idempotency: only enforce uniqueness when client_msg_id is present
CREATE UNIQUE INDEX IF NOT EXISTS ux_messages_client_msg_id
  ON public.messages (client_msg_id)
  WHERE client_msg_id IS NOT NULL;

-- ---------------------------------------------------------------------------
-- 3) Backfill seq for existing messages
--    Assign monotonic per-conversation sequence based on created_at order.
--    Only touches rows where seq IS NULL (idempotent on re-run).
-- ---------------------------------------------------------------------------
UPDATE public.messages m
SET seq = sub.rn
FROM (
  SELECT id,
         ROW_NUMBER() OVER (
           PARTITION BY conversation_id
           ORDER BY created_at
         ) AS rn
  FROM public.messages
  WHERE conversation_id IS NOT NULL
    AND seq IS NULL
) sub
WHERE m.id = sub.id;
