-- =============================================================================
-- Migration 023 — message_attachments table (media in messages)
--
-- WHY THIS EXISTS
--   Phase 1 of the messaging system spec (MESSAGING_SYSTEM_SPEC.md §8.2, §12,
--   §21). Closes gap G8 (no media in messages). Reuses the proven Supabase
--   Storage pipeline (SupabaseStorage.kt) with a new "MESSAGE" kind.
--
--   The flow (§12.1):
--     1. Client picks file → POST multipart to /messages/attachments
--     2. Server uploads to Supabase Storage: {schoolId}/message/{uuid}.{ext}
--     3. Returns {storage_url, file_name, mime_type, size_bytes, ...}
--     4. Client includes attachment metadata in send message request
--     5. Server INSERTs into message_attachments
--
-- WHAT THIS DOES
--   1. CREATEs message_attachments table with:
--      - message_id FK to messages(id) ON DELETE CASCADE
--      - conversation_id, sender_id, school_id for tenant isolation + queries
--      - file_name, mime_type, size_bytes, storage_url (Supabase public URL)
--      - thumbnail_url (optional, for images/video)
--      - attachment_type (IMAGE | VIDEO | DOCUMENT | AUDIO)
--      - width, height (for images/video), duration_ms (for audio/video)
--   2. CREATEs idx_attachments_message for per-message attachment lookup.
--
-- HOW TO RUN
--   Supabase -> SQL Editor -> paste this whole file -> Run.
--   Safe to re-run: CREATE TABLE IF NOT EXISTS + CREATE INDEX IF NOT EXISTS.
--
-- ROLLBACK:
--   DROP TABLE IF EXISTS public.message_attachments CASCADE;
-- =============================================================================

CREATE TABLE IF NOT EXISTS public.message_attachments (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id      uuid NOT NULL REFERENCES public.messages(id) ON DELETE CASCADE,
    conversation_id uuid NOT NULL,
    sender_id       uuid NOT NULL,
    school_id       uuid NOT NULL,
    file_name       text NOT NULL,
    mime_type       text NOT NULL,
    size_bytes      bigint NOT NULL,
    storage_url     text NOT NULL,
    thumbnail_url   text NULL,
    attachment_type varchar(16) NOT NULL DEFAULT 'IMAGE',
    width           integer NULL,
    height          integer NULL,
    duration_ms     integer NULL,
    created_at      timestamp NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_attachments_message
  ON public.message_attachments (message_id);
