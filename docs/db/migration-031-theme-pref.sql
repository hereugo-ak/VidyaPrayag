-- Migration: Add theme_pref column to app_users
-- Phase 6: Server-side theme preference persistence
--
-- Stores the user's theme mode preference as a string:
--   "system"          — follow OS dark/light
--   "light"           — force light theme
--   "dark"            — force dark theme
--   "custom:<themeId>" — a specific registered theme
--
-- Synced from the client via PUT /api/v1/user/theme-pref
-- Read back in GET /api/v1/user/details → personal_details.theme_pref

ALTER TABLE app_users ADD COLUMN IF NOT EXISTS theme_pref VARCHAR(64);
