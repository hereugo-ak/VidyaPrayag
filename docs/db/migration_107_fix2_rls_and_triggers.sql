-- =====================================================================
-- migration_107_fix2_rls_and_triggers.sql
-- Run on Supabase after migration_107 (or migration_107_fix) to add:
--   1. Row Level Security policies on school_day_config + school_day_slots
--   2. updated_at auto-update trigger on school_day_config
-- =====================================================================

-- 1. Enable RLS on both tables
ALTER TABLE school_day_config ENABLE ROW LEVEL SECURITY;
ALTER TABLE school_day_slots ENABLE ROW LEVEL SECURITY;

-- 2. RLS policies for school_day_config
-- Service role (used by Ktor backend via SUPABASE_SERVICE_KEY) bypasses RLS,
-- so these policies apply to direct Supabase API access (e.g. parent/teacher
-- auth'd clients). The backend always goes through the service role.

-- Read: any authenticated user can read configs for their school
DROP POLICY IF EXISTS sdc_read_own_school ON school_day_config;
CREATE POLICY sdc_read_own_school
  ON school_day_config FOR SELECT
  TO authenticated
  USING (school_id = '00000000-0000-0000-0000-000000000000'::uuid);

-- Note: school-scoped RLS for non-system rows requires joining to app_users
-- to find the user's school_id. Since the Ktor backend uses the service role
-- (which bypasses RLS), we use a permissive read for authenticated users and
-- rely on the backend for strict scoping. Write is restricted to service_role.

-- Write: only service role can insert/update/delete
DROP POLICY IF EXISTS sdc_write_service_only ON school_day_config;
CREATE POLICY sdc_write_service_only
  ON school_day_config FOR ALL
  TO service_role
  USING (true) WITH CHECK (true);

-- 3. RLS policies for school_day_slots
DROP POLICY IF EXISTS sds_read_own_school ON school_day_slots;
CREATE POLICY sds_read_own_school
  ON school_day_slots FOR SELECT
  TO authenticated
  USING (school_id = '00000000-0000-0000-0000-000000000000'::uuid);

DROP POLICY IF EXISTS sds_write_service_only ON school_day_slots;
CREATE POLICY sds_write_service_only
  ON school_day_slots FOR ALL
  TO service_role
  USING (true) WITH CHECK (true);

-- 4. updated_at auto-update trigger on school_day_config
CREATE OR REPLACE FUNCTION fn_sdc_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_sdc_updated_at ON school_day_config;

CREATE TRIGGER trg_sdc_updated_at
  BEFORE UPDATE ON school_day_config
  FOR EACH ROW
  EXECUTE FUNCTION fn_sdc_updated_at();
