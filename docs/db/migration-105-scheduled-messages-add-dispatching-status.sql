-- =============================================================================
-- Migration 105 — Add DISPATCHING status to scheduled_messages check constraint
--
-- WHY THIS EXISTS
--   The MessageDispatchScheduler uses an atomic claim pattern: it updates
--   status from SCHEDULED → DISPATCHING before dispatching, then DISPATCHED
--   on success. The original migration-104 CHECK constraint did not include
--   'DISPATCHING', causing every dispatch attempt to fail with:
--     ERROR: new row for relation "scheduled_messages" violates check
--     constraint "scheduled_messages_status_check"
--
--   This migration drops the old constraint and adds a new one that includes
--   'DISPATCHING'.
--
-- HOW TO RUN
--   Supabase -> SQL Editor -> paste this whole file -> Run.
--   Safe to re-run: uses IF EXISTS on DROP and IF NOT EXISTS equivalent logic.
--
-- RUN ORDER (appended to the existing chain):
--   ...
--   104. docs/db/migration-104-scheduled-messages.sql
--   105. docs/db/migration-105-scheduled-messages-add-dispatching-status.sql  <-- THIS FILE
-- =============================================================================

BEGIN;

-- Drop the old constraint that doesn't include DISPATCHING
ALTER TABLE public.scheduled_messages
    DROP CONSTRAINT IF EXISTS scheduled_messages_status_check;

-- Add the new constraint with DISPATCHING included
ALTER TABLE public.scheduled_messages
    ADD CONSTRAINT scheduled_messages_status_check
    CHECK (status IN ('DRAFT','SCHEDULED','DISPATCHING','DISPATCHED','FAILED','CANCELLED'));

COMMIT;

-- =============================================================================
-- POST-RUN REPORT — verify the constraint includes DISPATCHING
-- =============================================================================
SELECT con.conname, pg_get_constraintdef(con.oid) AS definition
FROM pg_constraint con
JOIN pg_class rel ON rel.oid = con.conrelid
WHERE rel.relname = 'scheduled_messages'
  AND con.conname = 'scheduled_messages_status_check';
