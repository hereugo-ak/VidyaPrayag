-- ──────────────────────────────────────────────────────────────────────────────
-- Migration 106: Per-channel notification preferences (spec §15)
--
-- Adds push_enabled, in_app_enabled, email_enabled, sms_enabled columns
-- to notification_preferences. When NULL, the channel matrix default
-- for the category is used. When set to false, the user has opted out
-- of that specific channel for that category.
--
-- Run manually in Supabase SQL Editor (AUTO_CREATE_TABLES is OFF in prod).
-- ──────────────────────────────────────────────────────────────────────────────

ALTER TABLE notification_preferences
    ADD COLUMN IF NOT EXISTS push_enabled   BOOLEAN DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS in_app_enabled BOOLEAN DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS email_enabled  BOOLEAN DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS sms_enabled    BOOLEAN DEFAULT NULL;
