-- =============================================================================
-- Migration 015 — canonical typed assessment + marks model (assessments revised)
--
-- WHY THIS EXISTS
--   Teacher Portal Rebuild — Doc 11 task T-301 (Phase 3, Gradebook). Doc 07 §1.3.
--   Closes X-6 (two parallel marks models) and D-ASMT-1..6:
--     • D-ASMT-1 / X-6 — duplicate marks models (`assessments`+`assessment_marks`
--       vs string-scored `exam_results`) → pick ONE canonical model
--       (assessments) and MIRROR the legacy numeric exam_results rows into it.
--     • D-ASMT-2 / X-3 — `exam_date` already typed `date` (migration_010); kept.
--     • D-ASMT-3 / X-4 — `assessment_marks.student_id` is free text
--       (student_code); add a typed `student_ref uuid` FK -> students, join the
--       name for display instead of trusting the denormalised `student_name`.
--     • D-ASMT-4 — no assessment type, no pass mark → add `type`, `pass_marks`.
--     • D-ASMT-5 — no calendar link → add `calendar_event_id`.
--     • D-ASMT-6 / X-1 — free-text class/section/subject scope → add provable
--       `assignment_id`/`class_id`/`subject_id` bindings.
--   Also adds the publish-discipline state column `status`
--   (draft|scheduled|marks_pending|published|archived) that the T-303 SAVE/PUBLISH
--   split (the B-MK-1 fix) drives; kept in sync with the legacy `is_published`
--   gate so ParentAcademicsRouting (which filters on is_published=true) keeps
--   working until parent reads move onto status='published'.
--
--   The Exposed mappings (AssessmentsTable / AssessmentMarksTable in
--   server/.../db/Tables.kt) reference these columns. AUTO_CREATE_TABLES is OFF
--   in production and the boot-time validateSchema() gate only checks table
--   PRESENCE (not columns); the typed T-303 read/write handlers REQUIRE these
--   columns, so this file MUST be applied in Supabase BEFORE the matching
--   backend deploy.
--
-- WHAT THIS DOES (all additive / idempotent / non-destructive)
--   1. ADDS the typed assessment columns (academic_year_id, assignment_id,
--      class_id, subject_id, type, pass_marks, calendar_event_id, status,
--      created_by) — all nullable/defaulted (IF NOT EXISTS via DO blocks).
--   2. ADDS the typed mark columns (student_ref, is_absent, remark, entered_at).
--   3. BACKFILLS, best-effort & idempotently:
--        • assessment_marks.student_ref  from students.student_code = student_id
--          (same school, via the parent assessment's school_id);
--        • assessment_marks.entered_at   ← updated_at (last edit time);
--        • assessments.status            ← 'published' where is_published, else
--          'draft' (the migration's only inference; T-303 owns the full machine);
--        • assessments.type              ← 'scheduled' (default; honest — the
--          legacy model has no type so we do NOT guess 'exam' for teacher rows);
--        • assessments.class_id/subject_id are LEFT NULL on backfill — the legacy
--          free-text class_name/subject are ambiguous to resolve to ids safely
--          (honesty rule: don't fabricate a binding); new writes (T-303) set them.
--   4. ADDS FK constraints (ON DELETE SET NULL — a deleted scope row must never
--      cascade-delete graded marks) and hot-path indexes.
--   5. ADDS CHECK constraints: type ∈ (scheduled,surprise,assignment,project,exam);
--      status ∈ (draft,scheduled,marks_pending,published,archived);
--      pass_marks BETWEEN 0 AND max_marks; marks >= 0. (guarded; drop+recreate so
--      re-runs are safe).
--   6. MIRRORS legacy numeric `exam_results` rows into the canonical model so the
--      teacher Gradebook history (T-304/T-306) is complete WITHOUT touching the
--      admin-side exam_results table (still read by 5 admin routers). Only rows
--      whose `score` parses as a number are mirrored; "A+"/"Pending" string rows
--      are skipped (no honest numeric denominator). Idempotent via a synthetic
--      stable id and ON CONFLICT DO NOTHING.
--
-- WHAT THIS DOES NOT DO
--   • Does NOT drop exam_results (out-of-scope admin readers — see Tables.kt note).
--   • Does NOT drop legacy assessment_marks.student_id / student_name (the legacy
--     teacher /marks handler + ParentAcademicsRouting still select them until
--     T-303 / the parent rebuild repoint them).
--   • Does NOT guess class_id/subject_id/assignment_id for legacy rows.
--
-- SAFE TO RE-RUN: every step is guarded (IF NOT EXISTS / ON CONFLICT / drop+add).
-- =============================================================================

BEGIN;

-- 1. ---------------------------------------------------------------- assessments: typed columns
ALTER TABLE assessments ADD COLUMN IF NOT EXISTS academic_year_id  uuid;
ALTER TABLE assessments ADD COLUMN IF NOT EXISTS assignment_id     uuid;
ALTER TABLE assessments ADD COLUMN IF NOT EXISTS class_id          uuid;
ALTER TABLE assessments ADD COLUMN IF NOT EXISTS subject_id        uuid;
ALTER TABLE assessments ADD COLUMN IF NOT EXISTS type              text   NOT NULL DEFAULT 'scheduled';
ALTER TABLE assessments ADD COLUMN IF NOT EXISTS pass_marks        integer;
ALTER TABLE assessments ADD COLUMN IF NOT EXISTS calendar_event_id uuid;
ALTER TABLE assessments ADD COLUMN IF NOT EXISTS status            text   NOT NULL DEFAULT 'draft';
ALTER TABLE assessments ADD COLUMN IF NOT EXISTS created_by        uuid;

-- 2. ------------------------------------------------------- assessment_marks: typed columns
ALTER TABLE assessment_marks ADD COLUMN IF NOT EXISTS student_ref uuid;
ALTER TABLE assessment_marks ADD COLUMN IF NOT EXISTS is_absent   boolean NOT NULL DEFAULT false;
ALTER TABLE assessment_marks ADD COLUMN IF NOT EXISTS remark      text;
ALTER TABLE assessment_marks ADD COLUMN IF NOT EXISTS entered_at  timestamptz;

-- 3. ------------------------------------------------------------------------ backfills
-- 3a. status from the legacy publish gate (idempotent: only touch rows still 'draft').
UPDATE assessments
   SET status = 'published'
 WHERE is_published = true
   AND status = 'draft';

-- 3b. typed student FK from the legacy student_code, scoped by the parent
--     assessment's school (so codes can't collide across tenants).
UPDATE assessment_marks am
   SET student_ref = s.id
  FROM assessments a, students s
 WHERE am.assessment_id = a.id
   AND s.school_id = a.school_id
   AND s.student_code = am.student_id
   AND am.student_ref IS NULL;

-- 3c. entered_at audit ← last edit time, best-effort.
UPDATE assessment_marks
   SET entered_at = updated_at
 WHERE entered_at IS NULL;

-- 4. ---------------------------------------------------------------------- FK constraints
-- ON DELETE SET NULL everywhere: a deleted class/subject/assignment/year/event/
-- author must NEVER cascade-delete graded marks (data-loss guard).
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_assess_academic_year') THEN
        ALTER TABLE assessments ADD CONSTRAINT fk_assess_academic_year
            FOREIGN KEY (academic_year_id) REFERENCES academic_years(id) ON DELETE SET NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_assess_assignment') THEN
        ALTER TABLE assessments ADD CONSTRAINT fk_assess_assignment
            FOREIGN KEY (assignment_id) REFERENCES teacher_subject_assignments(id) ON DELETE SET NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_assess_class') THEN
        ALTER TABLE assessments ADD CONSTRAINT fk_assess_class
            FOREIGN KEY (class_id) REFERENCES school_classes(id) ON DELETE SET NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_assess_subject') THEN
        ALTER TABLE assessments ADD CONSTRAINT fk_assess_subject
            FOREIGN KEY (subject_id) REFERENCES school_subjects(id) ON DELETE SET NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_assess_calendar_event') THEN
        ALTER TABLE assessments ADD CONSTRAINT fk_assess_calendar_event
            FOREIGN KEY (calendar_event_id) REFERENCES calendar_events(id) ON DELETE SET NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_assess_marks_student_ref') THEN
        ALTER TABLE assessment_marks ADD CONSTRAINT fk_assess_marks_student_ref
            FOREIGN KEY (student_ref) REFERENCES students(id) ON DELETE SET NULL;
    END IF;
END $$;

-- 5. ----------------------------------------------------------------- CHECK constraints
DO $$
BEGIN
    -- type domain (D-ASMT-4)
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_assess_type') THEN
        ALTER TABLE assessments DROP CONSTRAINT ck_assess_type;
    END IF;
    ALTER TABLE assessments ADD CONSTRAINT ck_assess_type
        CHECK (type IN ('scheduled','surprise','assignment','project','exam'));

    -- status domain (Doc 07 §2)
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_assess_status') THEN
        ALTER TABLE assessments DROP CONSTRAINT ck_assess_status;
    END IF;
    ALTER TABLE assessments ADD CONSTRAINT ck_assess_status
        CHECK (status IN ('draft','scheduled','marks_pending','published','archived'));

    -- pass_marks bound (D-ASMT-4): 0 <= pass_marks <= max_marks (NULL allowed)
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_assess_pass_marks') THEN
        ALTER TABLE assessments DROP CONSTRAINT ck_assess_pass_marks;
    END IF;
    ALTER TABLE assessments ADD CONSTRAINT ck_assess_pass_marks
        CHECK (pass_marks IS NULL OR (pass_marks >= 0 AND pass_marks <= max_marks));

    -- marks non-negative (per-row max enforced in app + at PUBLISH time)
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_assess_marks_nonneg') THEN
        ALTER TABLE assessment_marks DROP CONSTRAINT ck_assess_marks_nonneg;
    END IF;
    ALTER TABLE assessment_marks ADD CONSTRAINT ck_assess_marks_nonneg
        CHECK (marks IS NULL OR marks >= 0);
END $$;

-- 6. ------------------------------------------------------------------- hot-path indexes
CREATE INDEX IF NOT EXISTS ix_assess_assignment ON assessments (assignment_id);
CREATE INDEX IF NOT EXISTS ix_assess_class      ON assessments (class_id);
CREATE INDEX IF NOT EXISTS ix_assess_subject    ON assessments (subject_id);
CREATE INDEX IF NOT EXISTS ix_assess_status     ON assessments (school_id, status);
CREATE INDEX IF NOT EXISTS ix_assess_calendar   ON assessments (calendar_event_id);
CREATE INDEX IF NOT EXISTS ix_assess_marks_student_ref ON assessment_marks (student_ref);

-- 7. --------------------------------------------------- mirror legacy numeric exam_results
--   Bring numeric-scored exam_results into the canonical model so the teacher
--   Gradebook timeline (T-304/T-306) is complete. exam_results itself is NOT
--   modified (admin readers untouched). Only numeric scores are mirrored.
--
--   Idempotency: the mirrored assessment id is derived deterministically from
--   the exam_results natural key via md5 → uuid, so a re-run hits ON CONFLICT.
--   We create one assessment per (school, test, class, subject) and one mark
--   per student under it.
DO $$
BEGIN
    -- 7a. mirror assessments (one per distinct exam_results test/class/subject)
    INSERT INTO assessments
        (id, school_id, teacher_id, class_name, section, subject, name,
         max_marks, exam_date, is_active, is_published, published_at,
         created_at, updated_at, type, status)
    SELECT
        md5('exam_results:' || er.school_id::text || ':' || er.test || ':'
            || er.class_name || ':' || er.subject)::uuid,
        er.school_id,
        NULL,                                   -- legacy admin rows have no teacher author
        er.class_name,
        'A',                                    -- exam_results has no section → default
        er.subject,
        er.test,
        100,                                    -- legacy scores are out of an implied 100 (% model)
        NULL,
        true,
        true,                                   -- admin exam_results are already "published"
        min(er.created_at),
        min(er.created_at),
        max(er.updated_at),
        'exam',                                 -- mirrored admin rows ARE exams
        'published'
    FROM exam_results er
    WHERE er.score ~ '^[0-9]+(\.[0-9]+)?$'      -- numeric scores only
    GROUP BY er.school_id, er.test, er.class_name, er.subject
    ON CONFLICT (id) DO NOTHING;

    -- 7b. mirror the per-student marks under those assessments
    INSERT INTO assessment_marks
        (id, assessment_id, student_id, student_name, marks, entered_by,
         created_at, updated_at, student_ref, is_absent, entered_at)
    SELECT
        md5('exam_marks:' || er.school_id::text || ':' || er.test || ':'
            || er.class_name || ':' || er.subject || ':' || er.student_id)::uuid,
        md5('exam_results:' || er.school_id::text || ':' || er.test || ':'
            || er.class_name || ':' || er.subject)::uuid,
        er.student_id,
        er.student_name,
        er.score::numeric,
        NULL,
        er.created_at,
        er.updated_at,
        s.id,                                   -- typed student FK when resolvable
        false,
        er.updated_at
    FROM exam_results er
    LEFT JOIN students s
           ON s.school_id = er.school_id AND s.student_code = er.student_id
    WHERE er.score ~ '^[0-9]+(\.[0-9]+)?$'
    ON CONFLICT (assessment_id, student_id) DO NOTHING;
END $$;

COMMIT;

-- POST-RUN report (run manually to verify):
--   SELECT status, count(*) FROM assessments GROUP BY status;
--   SELECT count(*) AS marks_with_typed_fk FROM assessment_marks WHERE student_ref IS NOT NULL;
--   SELECT count(*) AS mirrored_exams FROM assessments WHERE type = 'exam';
