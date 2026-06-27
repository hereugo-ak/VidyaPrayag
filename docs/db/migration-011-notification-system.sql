-- Migration 011: Notification System Enhancements
-- Adds fee reminder throttling, calendar reminder tracking, notification
-- preferences, idempotency, indexes, and soft-archive support.

-- 1. Fee reminder throttling: track last reminder timestamp
ALTER TABLE fee_records
    ADD COLUMN IF NOT EXISTS last_reminded_at TIMESTAMPTZ;

-- 2. Calendar event reminder tracking: mark when a reminder has been sent
ALTER TABLE calendar_events
    ADD COLUMN IF NOT EXISTS reminder_sent BOOLEAN DEFAULT false;

-- 3. Notification preferences: per-user, per-category enable/sound
CREATE TABLE IF NOT EXISTS notification_preferences (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    category    VARCHAR(32) NOT NULL,
    enabled     BOOLEAN NOT NULL DEFAULT true,
    sound       VARCHAR(64),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ux_notif_prefs_user_category UNIQUE (user_id, category)
);

-- 4. Notifications table: idempotency key + soft-archive
ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(128);
ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS archived_at TIMESTAMPTZ;

-- 5. Indexes for efficient notification queries
CREATE INDEX IF NOT EXISTS ix_notifications_user_unread
    ON notifications (user_id, is_read);
CREATE INDEX IF NOT EXISTS ix_notifications_user_category
    ON notifications (user_id, category);
CREATE INDEX IF NOT EXISTS ix_notifications_idempotency
    ON notifications (idempotency_key)
    WHERE idempotency_key IS NOT NULL;
