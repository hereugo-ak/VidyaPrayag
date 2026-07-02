-- =====================================================================
-- migration_107_school_day_config.sql
-- Timetable & Class Teacher Ecosystem — Phase 0, item 0.1
-- Creates: school_day_config, school_day_slots
-- Seeds: default 8-period weekday system template
-- =====================================================================

CREATE TABLE IF NOT EXISTS school_day_config (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    name            TEXT NOT NULL,
    applicable_days VARCHAR(20) NOT NULL,
    class_level     VARCHAR(20) NOT NULL DEFAULT 'ALL',
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_sdc_school ON school_day_config(school_id);

CREATE TABLE IF NOT EXISTS school_day_slots (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    config_id       UUID NOT NULL REFERENCES school_day_config(id) ON DELETE CASCADE,
    school_id       UUID NOT NULL,
    slot_index      INTEGER NOT NULL,
    slot_type       VARCHAR(16) NOT NULL,
    label           TEXT NOT NULL DEFAULT '',
    start_time      TIME NOT NULL,
    end_time        TIME NOT NULL,
    is_double       BOOLEAN NOT NULL DEFAULT false,
    double_group    INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_sds_config ON school_day_slots(config_id, slot_index);

-- System template: default 8-period weekday config (Mon–Fri, ALL class levels)
-- school_id = 00000000-0000-0000-0000-000000000000 is a sentinel; no real school owns it.
-- The for-class endpoint falls back to this template when a school has no custom config.
INSERT INTO school_day_config (id, school_id, name, applicable_days, class_level, is_active)
VALUES ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000000',
        'Default Weekday', '1,2,3,4,5', 'ALL', true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO school_day_slots (config_id, school_id, slot_index, slot_type, label, start_time, end_time, is_double, double_group) VALUES
('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000000', 0,  'ASSEMBLY', 'Assembly',     '08:30', '08:45', false, 0),
('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000000', 1,  'TEACHING', 'Period 1',     '08:45', '09:30', false, 0),
('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000000', 2,  'TEACHING', 'Period 2',     '09:30', '10:15', false, 0),
('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000000', 3,  'BREAK',    'Short Break',  '10:15', '10:30', false, 0),
('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000000', 4,  'TEACHING', 'Period 3',     '10:30', '11:15', false, 0),
('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000000', 5,  'TEACHING', 'Period 4',     '11:15', '12:00', false, 0),
('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000000', 6,  'BREAK',    'Lunch',        '12:00', '12:45', false, 0),
('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000000', 7,  'TEACHING', 'Period 5',     '12:45', '13:30', false, 0),
('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000000', 8,  'TEACHING', 'Period 6',     '13:30', '14:15', false, 0),
('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000000', 9,  'TEACHING', 'Period 7',     '14:15', '15:00', false, 0),
('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000000', 10, 'TEACHING', 'Period 8',     '15:00', '15:45', false, 0)
ON CONFLICT DO NOTHING;

-- ── RLS policies (H-2) ─────────────────────────────────────────────────
ALTER TABLE school_day_config ENABLE ROW LEVEL SECURITY;
ALTER TABLE school_day_slots ENABLE ROW LEVEL SECURITY;

-- Service role bypasses RLS (used by Ktor backend). Authenticated users can
-- read the system template; school-scoped reads go through the backend.
DROP POLICY IF EXISTS sdc_read_own_school ON school_day_config;
CREATE POLICY sdc_read_own_school
  ON school_day_config FOR SELECT
  TO authenticated
  USING (school_id = '00000000-0000-0000-0000-000000000000'::uuid);

DROP POLICY IF EXISTS sdc_write_service_only ON school_day_config;
CREATE POLICY sdc_write_service_only
  ON school_day_config FOR ALL
  TO service_role
  USING (true) WITH CHECK (true);

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

-- ── updated_at auto-update trigger (H-3) ───────────────────────────────
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
