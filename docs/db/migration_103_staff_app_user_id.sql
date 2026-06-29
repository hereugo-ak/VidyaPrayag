-- migration_103_staff_app_user_id.sql
-- OPT-06: Add app_user_id column to non_teaching_staff for direct FK lookup.
-- Backfill via email match with app_users where possible.

ALTER TABLE non_teaching_staff
ADD COLUMN IF NOT EXISTS app_user_id UUID;

CREATE INDEX IF NOT EXISTS idx_non_teaching_staff_app_user_id
ON non_teaching_staff(app_user_id)
WHERE app_user_id IS NOT NULL;

-- Backfill: match existing staff to app_users by email
UPDATE non_teaching_staff ns
SET app_user_id = au.id
FROM app_users au
WHERE ns.email IS NOT NULL
  AND ns.email = au.email
  AND ns.app_user_id IS NULL;
