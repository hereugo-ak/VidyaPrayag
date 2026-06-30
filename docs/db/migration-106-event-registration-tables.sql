-- =====================================================================
-- Migration 106: Event Registration System tables
-- Creates: event_slots, event_registrations
-- Alters:  calendar_events (adds registration_enabled, registration_deadline,
--           max_attendees, venue)
-- Run this in Supabase SQL editor before starting the server.
-- =====================================================================

-- 1. Additive columns on calendar_events (safe — all nullable/defaulted)
ALTER TABLE calendar_events
    ADD COLUMN IF NOT EXISTS registration_enabled  boolean DEFAULT false,
    ADD COLUMN IF NOT EXISTS registration_deadline varchar(12),
    ADD COLUMN IF NOT EXISTS max_attendees         integer,
    ADD COLUMN IF NOT EXISTS venue                 text;

-- 2. event_slots — structured time slots for events with registration
CREATE TABLE IF NOT EXISTS event_slots (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id    uuid NOT NULL,
    start_time  varchar(8)  NOT NULL,
    end_time    varchar(8)  NOT NULL,
    capacity    integer     NOT NULL DEFAULT 1,
    is_active   boolean     NOT NULL DEFAULT true,
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ux_event_slots_event_start UNIQUE (event_id, start_time)
);

-- 3. event_registrations — parent registrations for events
CREATE TABLE IF NOT EXISTS event_registrations (
    id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id          uuid NOT NULL,
    slot_id           uuid,
    parent_user_id    uuid NOT NULL,
    student_id        uuid,
    school_id         uuid NOT NULL,
    attendee_count    integer     NOT NULL DEFAULT 1,
    status            varchar(16) NOT NULL DEFAULT 'REGISTERED',
    cancel_reason     text,
    registered_at     timestamptz NOT NULL DEFAULT now(),
    cancelled_at      timestamptz,
    updated_at        timestamptz NOT NULL DEFAULT now(),
    client_request_id varchar(64),
    CONSTRAINT ux_event_registrations_unique UNIQUE (event_id, parent_user_id, student_id)
);

-- Indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_event_slots_event_id ON event_slots(event_id);
CREATE INDEX IF NOT EXISTS idx_event_registrations_event_id ON event_registrations(event_id);
CREATE INDEX IF NOT EXISTS idx_event_registrations_parent ON event_registrations(parent_user_id);
CREATE INDEX IF NOT EXISTS idx_event_registrations_slot ON event_registrations(slot_id);
CREATE INDEX IF NOT EXISTS idx_event_registrations_school ON event_registrations(school_id);
CREATE INDEX IF NOT EXISTS idx_event_registrations_status ON event_registrations(status);
