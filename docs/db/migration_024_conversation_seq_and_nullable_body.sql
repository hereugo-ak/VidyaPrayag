-- Migration 024: Atomic conversation seq counter + nullable message body
--
-- P0-1: Add conversation_seq table for atomic seq generation (prevents
--       duplicate seq values under concurrent inserts).
-- P0-2: Alter messages.body to nullable for soft-delete tombstones
--       (deleted messages set body = NULL instead of empty string).
--
-- Depends on: migration_020_messaging_seq.sql (adds seq column to messages)
-- Run after:  migration_022_message_status.sql, migration_023_message_attachments.sql

-- ── P0-1: Atomic per-conversation sequence counter ──────────────────────────
CREATE TABLE IF NOT EXISTS conversation_seq (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL UNIQUE,
    next_val        INTEGER NOT NULL DEFAULT 1,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Index for fast lookups by conversation_id (covered by UNIQUE, but explicit).
CREATE INDEX IF NOT EXISTS idx_conversation_seq_conv
    ON conversation_seq (conversation_id);

-- ── P0-2: Make messages.body nullable for soft-delete tombstones ────────────
-- This allows deleteMessage to set body = NULL instead of "" (empty string),
-- so the client can distinguish "deleted message" from "empty body message".
ALTER TABLE messages ALTER COLUMN body DROP NOT NULL;

-- Verification:
-- SELECT column_name, is_nullable FROM information_schema.columns
--   WHERE table_name = 'messages' AND column_name = 'body';
-- Expected: body | YES
