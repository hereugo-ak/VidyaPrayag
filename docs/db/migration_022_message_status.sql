-- =============================================================================
-- Migration 022 — message_status table (per-message per-user delivery status)
--
-- WHY THIS EXISTS
--   Phase 1 of the messaging system spec (MESSAGING_SYSTEM_SPEC.md §8.2, §21).
--   Closes gap G3 (no per-message status tracking). The existing thread-level
--   unread_count is insufficient for blue-tick / delivered / read receipts.
--
--   This table tracks per-message, per-user status: SENT → DELIVERED → READ.
--   The status state machine is defined in §4.2 of the spec.
--
-- WHAT THIS DOES
--   1. CREATEs message_status table with:
--      - message_id FK to messages(id) ON DELETE CASCADE
--      - conversation_id for efficient per-conversation queries
--      - user_id (the recipient whose status this row tracks)
--      - status CHECK constraint (SENT | DELIVERED | READ)
--      - UNIQUE(message_id, user_id) to prevent duplicate status rows
--   2. CREATEs idx_msg_status_conv_user for read-status queries.
--
-- HOW TO RUN
--   Supabase -> SQL Editor -> paste this whole file -> Run.
--   Safe to re-run: CREATE TABLE IF NOT EXISTS + CREATE INDEX IF NOT EXISTS.
--
-- ROLLBACK:
--   DROP TABLE IF EXISTS public.message_status CASCADE;
-- =============================================================================

CREATE TABLE IF NOT EXISTS public.message_status (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id      uuid NOT NULL REFERENCES public.messages(id) ON DELETE CASCADE,
    conversation_id uuid NOT NULL,
    user_id         uuid NOT NULL,
    status          varchar(16) NOT NULL CHECK (status IN ('SENT','DELIVERED','READ')),
    created_at      timestamp NOT NULL DEFAULT now(),
    UNIQUE(message_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_msg_status_conv_user
  ON public.message_status (conversation_id, user_id);
