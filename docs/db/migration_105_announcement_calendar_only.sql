-- migration_105_announcement_calendar_only.sql
-- Unified Event & Announcement System: adds is_calendar_only column to announcements.
-- When true, the announcement is hidden from the parent feed but still syncs to the
-- academic calendar. Admins see it with a "Calendar Only" badge.

ALTER TABLE announcements ADD COLUMN IF NOT EXISTS is_calendar_only BOOLEAN DEFAULT FALSE;
