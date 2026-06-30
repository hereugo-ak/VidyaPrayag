-- =============================================================================
-- Migration 104 — scheduled_messages (Message Scheduling feature)
--
-- WHY THIS EXISTS
--   MESSAGE_SCHEDULING_PLAN.md §4 — introduces a unified scheduling table that
--   allows school admins and teachers to schedule announcements, admin
--   broadcasts, and teacher class broadcasts for future delivery.
--
--   The MessageDispatchScheduler (a polling-based dispatcher following the
--   existing NotificationScheduler pattern) reads rows with status='SCHEDULED'
--   and scheduled_at <= now(), dispatches them via the existing Notify.toUsers /
--   sendInConversation / createCalendarEvent primitives, and marks them
--   DISPATCHED or FAILED.
--
--   Status state machine: DRAFT → SCHEDULED → DISPATCHED → FAILED | CANCELLED
--
-- WHAT THIS DOES
--   Creates `public.scheduled_messages` with the §4.1 shape:
--     • id                   uuid PK default gen_random_uuid()
--     • school_id            uuid NOT NULL                    (tenant scope)
--     • message_type         varchar(24) NOT NULL             (ANNOUNCEMENT|ADMIN_BROADCAST|TEACHER_BROADCAST)
--     • status               varchar(16) NOT NULL DEFAULT 'SCHEDULED' (DRAFT|SCHEDULED|DISPATCHED|FAILED|CANCELLED)
--     • scheduled_at         timestamptz NOT NULL             (when to dispatch, UTC)
--     • dispatched_at        timestamptz NULL                 (actual dispatch time)
--     • payload              text NOT NULL                    (JSON: full original request body)
--     • created_by           uuid NOT NULL                    (author user id)
--     • author_role          varchar(16) NOT NULL             (school_admin|teacher)
--     • author_name          varchar(128) NULL
--     • audience_type        varchar(16) DEFAULT 'ALL_SCHOOL'
--     • audience_label       varchar(256) NULL
--     • title                varchar(256) NULL
--     • body_preview         varchar(256) NULL
--     • add_to_calendar      boolean DEFAULT false
--     • calendar_event_code  varchar(20) NULL
--     • retry_count          integer DEFAULT 0
--     • max_retries          integer DEFAULT 3
--     • last_error           text NULL
--     • client_msg_id        uuid NULL                       (idempotency key)
--     • created_at           timestamptz NOT NULL DEFAULT now()
--     • updated_at           timestamptz NOT NULL DEFAULT now()
--
--   Indexes:
--     • (school_id, status)              — list query hot path
--     • (scheduled_at) WHERE SCHEDULED   — dispatcher poll query
--     • (created_by)                     — "my scheduled messages" query
--     • (client_msg_id) WHERE NOT NULL   — idempotency lookup (§17 item 21)
--
--   This is pure DDL — it ADDS a table only; it never drops or rewrites
--   existing data. CREATE TABLE IF NOT EXISTS + CREATE INDEX IF NOT EXISTS
--   make it safe to re-run (a no-op once applied).
--
-- DEVIATION (flagged per plan §4.4)
--   • Filename: Plan §4.4 originally specified `migration-011-scheduled-messages.sql`
--     but migration 011 is already used by migration-011-notification-system.sql
--     and migration_011_periods.sql. The latest on-disk migration is 103, so this
--     file is `migration-104-scheduled-messages.sql` to keep the sequence
--     contiguous and collision-free.
--
-- HOW TO RUN
--   Supabase -> SQL Editor -> paste this whole file -> Run.
--   Safe to re-run: every statement is guarded by IF NOT EXISTS.
--
-- RUN ORDER (appended to the existing chain):
--   ...
--   103. docs/db/migration_103_staff_app_user_id.sql
--   104. docs/db/migration-104-scheduled-messages.sql   <-- THIS FILE
--
-- Column names/types match server/.../db/Tables.kt (ScheduledMessagesTable)
-- EXACTLY (Exposed `timestamp()` -> Postgres `timestamptz`).
-- =============================================================================

BEGIN;

-- ─────────────────────────────────────────────────────────────────────────────
-- scheduled_messages — unified scheduling table for all schedulable message types.
-- §4.1 schema. The dispatcher polls WHERE status='SCHEDULED' AND scheduled_at <= now().
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.scheduled_messages (
    id                   uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id            uuid NOT NULL,
    message_type         varchar(24) NOT NULL
                             CHECK (message_type IN ('ANNOUNCEMENT','ADMIN_BROADCAST','TEACHER_BROADCAST')),
    status               varchar(16) NOT NULL DEFAULT 'SCHEDULED'
                             CHECK (status IN ('DRAFT','SCHEDULED','DISPATCHED','FAILED','CANCELLED')),
    scheduled_at         timestamptz NOT NULL,
    dispatched_at        timestamptz NULL,
    payload              text NOT NULL,
    created_by           uuid NOT NULL,
    author_role          varchar(16) NOT NULL,
    author_name          varchar(128) NULL,
    audience_type        varchar(16) NOT NULL DEFAULT 'ALL_SCHOOL',
    audience_label       varchar(256) NULL,
    title                varchar(256) NULL,
    body_preview         varchar(256) NULL,
    add_to_calendar      boolean NOT NULL DEFAULT false,
    calendar_event_code  varchar(20) NULL,
    retry_count          integer NOT NULL DEFAULT 0,
    max_retries          integer NOT NULL DEFAULT 3,
    last_error           text NULL,
    client_msg_id        uuid NULL,
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now()
);

-- List query: GET /scheduled-messages?status=SCHEDULED filtered by school.
CREATE INDEX IF NOT EXISTS idx_scheduled_messages_school_status
    ON public.scheduled_messages (school_id, status);

-- Dispatcher poll: SELECT ... WHERE status='SCHEDULED' AND scheduled_at <= now().
CREATE INDEX IF NOT EXISTS idx_scheduled_messages_scheduled_at
    ON public.scheduled_messages (scheduled_at) WHERE status = 'SCHEDULED';

-- "My scheduled messages" query: filtered by created_by.
CREATE INDEX IF NOT EXISTS idx_scheduled_messages_created_by
    ON public.scheduled_messages (created_by);

-- Idempotency lookup by client_msg_id (§17 item 21).
CREATE INDEX IF NOT EXISTS idx_scheduled_messages_client_msg_id
    ON public.scheduled_messages (client_msg_id) WHERE client_msg_id IS NOT NULL;

COMMIT;

-- =============================================================================
-- POST-RUN REPORT — confirms the table is present and queryable.
-- =============================================================================
SELECT count(*) AS scheduled_messages_present FROM public.scheduled_messages;
