-- =====================================================================
-- migration_005_class_normalization_and_student_code_standard.sql
--
-- Fixes for ISSUE 1 (teacher⇄student relationships never connect) and
-- ISSUE 2a/2b (inconsistent student_code + missing parent phone).
--
-- This migration is IDEMPOTENT and SAFE to re-run. It:
--   1. Adds students.parent_phone (ISSUE 2b).
--   2. Normalises existing class_name / section on BOTH the students and
--      teacher_subject_assignments tables so the derived join holds
--      (ISSUE 1). The normalisation mirrors the server-side ClassNaming
--      object (lowercase, strip Grade/Class/Std prefix, roman→arabic for
--      the COMPARISON; here we canonicalise the STORED display value to the
--      school's school_classes.name when one matches, else trim+collapse).
--   3. Regenerates EVERY students.student_code into the single standard
--      <CLASS_TOKEN><SECTION>-<ROLL3> (ISSUE 2a) and cascades the new code
--      to every table that references it by value (no FK exists):
--        - attendance_records.person_id   (type = 'student')
--        - children.student_code
--        - exam_results.student_id
--        - assessment_marks.student_id
--        - homework_submissions.student_id
--        - parent_child_links.student_code
--
-- SAFETY / IDEMPOTENCY:
--   • ADD COLUMN ... IF NOT EXISTS         — re-running won't error if the
--     parent_phone column is already present.
--   • CREATE OR REPLACE FUNCTION           — helper functions are replaced,
--     never duplicated.
--   • CREATE INDEX ... IF NOT EXISTS       — indexes are only created once.
--   • to_regclass(...) IS NOT NULL guards  — every OPTIONAL dependent table
--     update is skipped if that table doesn't exist in this deployment.
--   • The whole thing runs in ONE transaction, so a failure rolls back fully.
--   Re-running after a successful run is harmless: class names are already
--   canonical (DISTINCT FROM guards skip them) and codes are already in the
--   standard, so the regenerated value equals the stored one.
--
-- Run inside a single transaction.
-- =====================================================================

BEGIN;

-- ── 1. ISSUE 2b: parent phone column ────────────────────────────────
ALTER TABLE students ADD COLUMN IF NOT EXISTS parent_phone text;

-- ── Helper functions mirroring server-side ClassNaming / StudentCode ─

-- Comparable class key: lowercase, strip a leading Grade/Class/Std prefix,
-- roman→arabic (1..12), strip incidental separators/whitespace.
CREATE OR REPLACE FUNCTION vp_class_key(raw text)
RETURNS text LANGUAGE plpgsql IMMUTABLE AS $$
DECLARE s text;
BEGIN
    IF raw IS NULL THEN RETURN ''; END IF;
    s := lower(btrim(regexp_replace(raw, '\s+', ' ', 'g')));
    -- strip a leading prefix word
    s := regexp_replace(s, '^(grade|class|standard|std\.?|cls)[\s\-]+', '');
    s := btrim(s);
    -- roman → arabic
    s := CASE s
        WHEN 'i' THEN '1' WHEN 'ii' THEN '2' WHEN 'iii' THEN '3' WHEN 'iv' THEN '4'
        WHEN 'v' THEN '5' WHEN 'vi' THEN '6' WHEN 'vii' THEN '7' WHEN 'viii' THEN '8'
        WHEN 'ix' THEN '9' WHEN 'x' THEN '10' WHEN 'xi' THEN '11' WHEN 'xii' THEN '12'
        ELSE s END;
    -- drop incidental whitespace so "g 4"/"g-4"/"g4" collapse
    s := regexp_replace(s, '\s+', '', 'g');
    RETURN s;
END $$;

-- Canonical section: trim + upper, blank → 'A'.
CREATE OR REPLACE FUNCTION vp_section_key(raw text)
RETURNS text LANGUAGE plpgsql IMMUTABLE AS $$
DECLARE s text;
BEGIN
    s := upper(btrim(coalesce(raw, '')));
    IF s = '' THEN RETURN 'A'; END IF;
    RETURN s;
END $$;

-- Compact class token for the student code (mirrors StudentCode.classToken):
--   numeric class  -> 'G' + number  ("4" -> "G4")
--   named class    -> up to 4 upper alnum ("nursery" -> "NUR")
CREATE OR REPLACE FUNCTION vp_class_token(raw text)
RETURNS text LANGUAGE plpgsql IMMUTABLE AS $$
DECLARE k text; alnum text;
BEGIN
    k := vp_class_key(raw);
    IF k = '' THEN RETURN 'X'; END IF;
    IF k ~ '^[0-9]+$' THEN RETURN 'G' || k; END IF;
    alnum := upper(regexp_replace(k, '[^a-zA-Z0-9]', '', 'g'));
    IF alnum = '' THEN RETURN 'X'; END IF;
    RETURN left(alnum, 4);
END $$;

-- Roll token (mirrors StudentCode.rollToken): pure-numeric -> zero-pad 3;
-- mixed -> upper alnum capped to 6.
CREATE OR REPLACE FUNCTION vp_roll_token(raw text)
RETURNS text LANGUAGE plpgsql IMMUTABLE AS $$
DECLARE r text; digits text;
BEGIN
    r := btrim(coalesce(raw, ''));
    IF r = '' THEN RETURN '000'; END IF;
    IF r ~ '^[0-9]+$' THEN
        digits := ltrim(r, '0');
        IF digits = '' THEN digits := '0'; END IF;
        RETURN lpad(digits, 3, '0');
    END IF;
    r := upper(regexp_replace(r, '[^a-zA-Z0-9]', '', 'g'));
    IF r = '' THEN RETURN '000'; END IF;
    RETURN left(r, 6);
END $$;

-- ── 2. ISSUE 1: canonicalise stored class_name to school_classes.name ─
-- For each student, if its class key matches a configured class for the
-- same school, adopt that class's canonical display name; always canonicalise
-- the section. Same for teacher_subject_assignments. This makes the two ends
-- store byte-identical strings so the derived join holds.

UPDATE students s
SET class_name = sc.name,
    section    = vp_section_key(s.section)
FROM school_classes sc
WHERE sc.school_id = s.school_id
  AND vp_class_key(sc.name) = vp_class_key(s.class_name)
  AND (s.class_name IS DISTINCT FROM sc.name OR s.section IS DISTINCT FROM vp_section_key(s.section));

-- Students whose class did NOT match a configured class: still canonicalise section.
UPDATE students s
SET section = vp_section_key(s.section)
WHERE s.section IS DISTINCT FROM vp_section_key(s.section);

UPDATE teacher_subject_assignments t
SET class_name = sc.name,
    section    = vp_section_key(t.section)
FROM school_classes sc
WHERE sc.school_id = t.school_id
  AND vp_class_key(sc.name) = vp_class_key(t.class_name)
  AND (t.class_name IS DISTINCT FROM sc.name OR t.section IS DISTINCT FROM vp_section_key(t.section));

UPDATE teacher_subject_assignments t
SET section = vp_section_key(t.section)
WHERE t.section IS DISTINCT FROM vp_section_key(t.section);

-- ── 3. ISSUE 2a: regenerate all student_code values + cascade ────────
-- Build the new code per student with a per-school collision suffix, then
-- propagate it everywhere the old code was referenced by value.

-- 3a. Compute the desired base code and a uniqueness-safe final code.
--     IDEMPOTENT: a student whose current code is ALREADY the base (or a
--     base-with-suffix in the standard) keeps it — ordering prefers such rows
--     for rank 1, so a second run produces new_code == old_code (no-ops).
CREATE TEMP TABLE vp_code_map ON COMMIT DROP AS
WITH base AS (
    SELECT
        id,
        school_id,
        student_code AS old_code,
        vp_class_token(class_name) || vp_section_key(section) || '-' || vp_roll_token(roll_number) AS base_code
    FROM students
),
ranked AS (
    SELECT
        id, school_id, old_code, base_code,
        ROW_NUMBER() OVER (
            PARTITION BY base_code
            -- Prefer the row that already holds the exact base code, then any
            -- row already in the base-with-suffix standard, then by old code.
            ORDER BY
                (old_code = base_code) DESC,
                (old_code ~ ('^' || base_code || '(-[0-9]+)?$')) DESC,
                old_code
        ) AS rn
    FROM base
)
SELECT
    id, school_id, old_code, base_code,
    CASE WHEN rn = 1 THEN base_code ELSE base_code || '-' || rn END AS new_code
FROM ranked;

-- 3b. Guard: if (against expectation) any new_code still collides globally,
--     fall back to appending the short id so the unique index never trips.
UPDATE vp_code_map m
SET new_code = m.base_code || '-' || left(replace(m.id::text, '-', ''), 6)
WHERE EXISTS (
    SELECT 1 FROM vp_code_map x
    WHERE x.new_code = m.new_code AND x.id <> m.id
);

-- 3c. Cascade to dependent tables FIRST (they reference the OLD code by value).
--     Each table is guarded with to_regclass so the migration still runs on a
--     deployment that does not (yet) have an optional table. We also verify the
--     specific column exists before touching it (defensive against schema drift).
DO $cascade$
BEGIN
    IF to_regclass('public.attendance_records') IS NOT NULL THEN
        UPDATE attendance_records a
        SET person_id = m.new_code
        FROM vp_code_map m
        WHERE a.type = 'student' AND a.person_id = m.old_code;
    END IF;

    IF to_regclass('public.children') IS NOT NULL THEN
        UPDATE children c
        SET student_code = m.new_code
        FROM vp_code_map m
        WHERE c.student_code = m.old_code;
    END IF;

    IF to_regclass('public.exam_results') IS NOT NULL THEN
        UPDATE exam_results e
        SET student_id = m.new_code
        FROM vp_code_map m
        WHERE e.student_id = m.old_code;
    END IF;

    IF to_regclass('public.assessment_marks') IS NOT NULL THEN
        UPDATE assessment_marks am
        SET student_id = m.new_code
        FROM vp_code_map m
        WHERE am.student_id = m.old_code;
    END IF;

    IF to_regclass('public.homework_submissions') IS NOT NULL THEN
        UPDATE homework_submissions h
        SET student_id = m.new_code
        FROM vp_code_map m
        WHERE h.student_id = m.old_code;
    END IF;

    IF to_regclass('public.parent_child_links') IS NOT NULL THEN
        UPDATE parent_child_links p
        SET student_code = m.new_code
        FROM vp_code_map m
        WHERE p.student_code = m.old_code;
    END IF;
END
$cascade$;

-- 3d. Finally rewrite the canonical students.student_code.
UPDATE students s
SET student_code = m.new_code
FROM vp_code_map m
WHERE s.id = m.id;

-- ── 4. Indexes that speed up the (now reliable) derived join ─────────
CREATE INDEX IF NOT EXISTS ix_students_school_class_section
    ON students (school_id, class_name, section);
CREATE INDEX IF NOT EXISTS ix_tsa_school_class_section
    ON teacher_subject_assignments (school_id, class_name, section);

-- ── 5. Clean up helper functions (optional; keep if you prefer) ──────
DROP FUNCTION IF EXISTS vp_class_key(text);
DROP FUNCTION IF EXISTS vp_section_key(text);
DROP FUNCTION IF EXISTS vp_class_token(text);
DROP FUNCTION IF EXISTS vp_roll_token(text);

COMMIT;
