-- =====================================================================
-- migration_107_fix_school_day_config.sql
-- Run on Supabase if migration_107 was already applied with the bug
-- where 10 of 11 slot rows had config_id = sentinel school_id
-- instead of the actual config row id.
-- Also upgrades idx_sds_config to UNIQUE.
-- =====================================================================

-- 1. Drop the old non-unique index
DROP INDEX IF EXISTS idx_sds_config;

-- 2. Recreate as unique index
CREATE UNIQUE INDEX IF NOT EXISTS idx_sds_config
  ON school_day_slots(config_id, slot_index);

-- 3. Delete the broken seed rows (config_id pointed to sentinel school_id,
--    which is not a valid school_day_config.id — FK violation means they
--    likely never inserted, but clean up just in case the FK was missing)
DELETE FROM school_day_slots
WHERE config_id = '00000000-0000-0000-0000-000000000000';

-- 4. Re-seed the system template config (idempotent)
INSERT INTO school_day_config (id, school_id, name, applicable_days, class_level, is_active)
VALUES ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000000',
        'Default Weekday', '1,2,3,4,5', 'ALL', true)
ON CONFLICT (id) DO NOTHING;

-- 5. Re-seed all 11 slots with the correct config_id
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
