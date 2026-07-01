-- Migration 106: Add read_at column to message_status table
-- Read Receipts Phase 1 (READ_RECEIPTS_PLAN.md §6.1)
-- Adds a nullable timestamp to track when a message was marked as READ.

ALTER TABLE message_status
    ADD COLUMN IF NOT EXISTS read_at TIMESTAMPTZ;
