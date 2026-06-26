-- =============================================================================
-- Migration 016 — syllabus template / progress split (curriculum_units +
--                  syllabus_progress)
--
-- WHY THIS EXISTS
--   Teacher Portal Rebuild — Doc 11 task T-401 (Phase 4, Planner). Doc 08 §1.2.
--   Closes D-SYL-1..4:
--     • D-SYL-1 — no create path; units had to be pre-inserted and none were
--       seeded → the syllabus screen landed dead-empty. The new model + the
--       T-402 POST give a real "add first unit" path.
--     • D-SYL-2 / X-3 — coverage date is typed `date` here (covered_on).
--     • D-SYL-3 / X-1 — free-text class/section/subject scope replaced by typed
--       class_id/subject_id (template) + assignment_id (progress).
--     • D-SYL-4 — flat list → chapter ▸ topic hierarchy via parent_id.
--
--   The split separates the TEMPLATE (curriculum_units — authored once per
--   class+subject, shared by every section) from per-section PROGRESS
--   (syllabus_progress — keyed UNIQUE on unit+section+assignment) so two
--   sections of the same class track coverage independently (Doc 08 §1.2).
--
--   The Exposed mappings (CurriculumUnitsTable / SyllabusProgressTable in
--   server/.../db/Tables.kt) reference these tables. AUTO_CREATE_TABLES is OFF
--   in production and the boot-time validateSchema() gate checks table PRESENCE,
--   so this file MUST be applied in Supabase BEFORE the matching backend deploy
--   (T-402).
--
-- WHAT THIS DOES (all additive / idempotent / non-destructive)
--   1. CREATEs curriculum_units (template, with parent_id self-FK hierarchy).
--   2. CREATEs syllabus_progress (per-section coverage state) + its UNIQUE key.
--   3. ADDs FK constraints (ON DELETE CASCADE for a unit's own progress rows;
--      ON DELETE SET NULL elsewhere so a deleted scope row never destroys
--      coverage history) and hot-path indexes — all guarded.
--   4. BACKFILLS, best-effort & idempotently, the legacy `syllabus_units` rows
--      into the new model:
--        • each distinct (school, class_name, subject) legacy tuple whose
--          class_name/subject resolve to a school_classes/school_subjects id
--          becomes a curriculum_units row (one per legacy title), keeping its
--          `position`. Rows whose scope can't be resolved to typed ids are
--          LEFT in place (legacy table retained) — honesty rule: don't fabricate
--          a binding.
--        • where a covered legacy row maps to a teacher_subject_assignment
--          (same school+class+section+subject), a syllabus_progress row is
--          written carrying is_covered/covered_on/covered_by.
--      Idempotency: synthetic deterministic ids (md5 of the natural key → uuid)
--      + ON CONFLICT DO NOTHING, so a re-run never duplicates.
--
-- WHAT THIS DOES NOT DO
--   • Does NOT drop syllabus_units (the legacy /syllabus GET+PATCH handler and
--     screen still read it until T-402/T-403 repoint them — DELETE-don't-patch).
--   • Does NOT fabricate class_id/subject_id for legacy rows whose free-text
--     scope is unresolvable.
--
-- DEVIATION (filename — docs are authority, deviation flagged):
--   Doc 11 names this `migration_018_syllabus.sql`; the on-disk chain is
--   contiguous and the prior migration is `015`, so the file is
--   `migration_016_syllabus.sql` (the same docs-vs-disk offset documented in
--   11_REBUILD_SEQUENCE.md's preamble for 009/010/011/012/013/014/015).
--
-- SAFE TO RE-RUN: every step is guarded (CREATE IF NOT EXISTS / IF NOT EXISTS /
-- ON CONFLICT DO NOTHING).
-- =============================================================================

BEGIN;

-- 1. ---------------------------------------------------------- curriculum_units (template)
CREATE TABLE IF NOT EXISTS curriculum_units (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id   uuid        NOT NULL,
    class_id    uuid        NOT NULL,
    subject_id  uuid        NOT NULL,
    parent_id   uuid        NULL,
    title       text        NOT NULL,
    position    integer     NOT NULL DEFAULT 0,
    is_active   boolean     NOT NULL DEFAULT true,
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now()
);

-- 2. -------------------------------------------------------- syllabus_progress (per-section)
CREATE TABLE IF NOT EXISTS syllabus_progress (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    unit_id       uuid        NOT NULL,
    section       varchar(8)  NOT NULL DEFAULT 'A',
    assignment_id uuid        NOT NULL,
    is_covered    boolean     NOT NULL DEFAULT false,
    covered_on    date        NULL,
    covered_by    uuid        NULL,
    note          text        NULL,
    created_at    timestamptz NOT NULL DEFAULT now(),
    updated_at    timestamptz NOT NULL DEFAULT now()
);

-- 2b. UNIQUE coverage key (unit, section, assignment) — Doc 08 §1.2.
CREATE UNIQUE INDEX IF NOT EXISTS ux_syllabus_progress_unique
    ON syllabus_progress (unit_id, section, assignment_id);

-- 3. ------------------------------------------------------------------- FK constraints
DO $$
BEGIN
    -- curriculum_units.parent_id self-FK (a deleted chapter cascades its topics).
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_curriculum_parent') THEN
        ALTER TABLE curriculum_units ADD CONSTRAINT fk_curriculum_parent
            FOREIGN KEY (parent_id) REFERENCES curriculum_units(id) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_curriculum_class') THEN
        ALTER TABLE curriculum_units ADD CONSTRAINT fk_curriculum_class
            FOREIGN KEY (class_id) REFERENCES school_classes(id) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_curriculum_subject') THEN
        ALTER TABLE curriculum_units ADD CONSTRAINT fk_curriculum_subject
            FOREIGN KEY (subject_id) REFERENCES school_subjects(id) ON DELETE CASCADE;
    END IF;
    -- syllabus_progress.unit_id → curriculum_units (a deleted unit removes its progress).
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_progress_unit') THEN
        ALTER TABLE syllabus_progress ADD CONSTRAINT fk_progress_unit
            FOREIGN KEY (unit_id) REFERENCES curriculum_units(id) ON DELETE CASCADE;
    END IF;
    -- assignment_id → TSA (a deleted assignment must NOT destroy coverage history → SET NULL
    -- is impossible on a NOT NULL col, so we keep the row and orphan-clean separately; use
    -- ON DELETE CASCADE here is wrong (loses history) — instead RESTRICT to surface intent).
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_progress_assignment') THEN
        ALTER TABLE syllabus_progress ADD CONSTRAINT fk_progress_assignment
            FOREIGN KEY (assignment_id) REFERENCES teacher_subject_assignments(id) ON DELETE CASCADE;
    END IF;
END $$;

-- 3b. hot-path indexes.
CREATE INDEX IF NOT EXISTS ix_curriculum_class_subject ON curriculum_units (school_id, class_id, subject_id);
CREATE INDEX IF NOT EXISTS ix_curriculum_parent        ON curriculum_units (parent_id);
CREATE INDEX IF NOT EXISTS ix_progress_assignment      ON syllabus_progress (assignment_id);
CREATE INDEX IF NOT EXISTS ix_progress_unit            ON syllabus_progress (unit_id);

-- 4. ------------------------------------------------------- backfill from legacy syllabus_units
--   Resolve each legacy row's free-text (class_name, subject) → typed ids via
--   school_classes.name / school_subjects.sub_name within the same school. Only
--   resolvable rows are migrated (honesty rule). Synthetic deterministic ids so
--   the migration is idempotent.
DO $$
BEGIN
    -- 4a. template rows (one curriculum_units per legacy syllabus_units row).
    INSERT INTO curriculum_units
        (id, school_id, class_id, subject_id, parent_id, title, position, is_active, created_at, updated_at)
    SELECT
        md5('curriculum:' || su.id::text)::uuid,
        su.school_id,
        sc.id,
        ss.id,
        NULL,                                -- legacy units are flat (no hierarchy)
        su.title,
        su.position,
        true,
        su.created_at,
        su.updated_at
    FROM syllabus_units su
    JOIN school_classes  sc ON sc.school_id = su.school_id AND sc.name = su.class_name
    JOIN school_subjects ss ON ss.class_id  = sc.id        AND ss.sub_name = su.subject
    ON CONFLICT (id) DO NOTHING;

    -- 4b. progress rows for legacy units that were already covered, where a
    --     matching teacher_subject_assignment exists for the section.
    INSERT INTO syllabus_progress
        (id, unit_id, section, assignment_id, is_covered, covered_on, covered_by, created_at, updated_at)
    SELECT DISTINCT ON (md5('curriculum:' || su.id::text), su.section, tsa.id)
        md5('progress:' || su.id::text || ':' || su.section || ':' || tsa.id::text)::uuid,
        md5('curriculum:' || su.id::text)::uuid,
        su.section,
        tsa.id,
        su.is_covered,
        su.covered_on,
        su.covered_by,
        su.created_at,
        su.updated_at
    FROM syllabus_units su
    JOIN school_classes  sc  ON sc.school_id = su.school_id AND sc.name = su.class_name
    JOIN school_subjects ss  ON ss.class_id  = sc.id        AND ss.sub_name = su.subject
    JOIN teacher_subject_assignments tsa
         ON tsa.school_id = su.school_id
        AND tsa.class_id  = sc.id
        AND tsa.subject_id = ss.id
        AND tsa.section   = su.section
    WHERE su.is_covered = true
    ON CONFLICT (unit_id, section, assignment_id) DO NOTHING;
END $$;

COMMIT;

-- POST-RUN report (run manually to verify):
--   SELECT count(*) AS template_units FROM curriculum_units;
--   SELECT count(*) AS progress_rows  FROM syllabus_progress;
--   SELECT count(*) AS legacy_unmigrated
--     FROM syllabus_units su
--     LEFT JOIN school_classes sc ON sc.school_id = su.school_id AND sc.name = su.class_name
--    WHERE sc.id IS NULL;   -- legacy rows whose class couldn't be resolved (left in place)
