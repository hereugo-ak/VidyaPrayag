-- Migration: Timetable management — period exceptions display columns + change requests table
-- Adds denormalised display columns to period_exceptions and creates timetable_change_requests table.

-- ── 1. Add display columns to period_exceptions ──────────────────────────────
ALTER TABLE period_exceptions
    ADD COLUMN IF NOT EXISTS class_name TEXT DEFAULT '',
    ADD COLUMN IF NOT EXISTS section VARCHAR(8) DEFAULT 'A',
    ADD COLUMN IF NOT EXISTS subject TEXT DEFAULT '';

-- ── 2. Create timetable_change_requests table ────────────────────────────────
CREATE TABLE IF NOT EXISTS timetable_change_requests (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    teacher_id      UUID NOT NULL,
    assignment_id   UUID,
    period_id       UUID,
    kind            VARCHAR(16) NOT NULL,          -- NEW_PERIOD | UPDATE_PERIOD | DELETE_PERIOD
    weekday         INTEGER NOT NULL,              -- 1=Mon … 7=Sun
    start_time      TIME,
    end_time        TIME,
    room            TEXT DEFAULT '',
    reason          TEXT DEFAULT '',
    status          VARCHAR(16) DEFAULT 'PENDING', -- PENDING | APPROVED | REJECTED
    admin_note      TEXT DEFAULT '',
    reviewed_by     UUID,
    created_at      TIMESTAMPTZ DEFAULT now(),
    reviewed_at     TIMESTAMPTZ,
    class_name      TEXT DEFAULT '',
    section         VARCHAR(8) DEFAULT 'A',
    subject         TEXT DEFAULT ''
);

CREATE INDEX IF NOT EXISTS idx_tcr_school_status
    ON timetable_change_requests(school_id, status);
CREATE INDEX IF NOT EXISTS idx_tcr_teacher
    ON timetable_change_requests(teacher_id, created_at DESC);
