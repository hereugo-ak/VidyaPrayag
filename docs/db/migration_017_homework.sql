-- =============================================================================
-- Migration 017 — homework typed scope + attachments + extensions + FK submissions
--                  (homework revised, homework_attachments, homework_extensions,
--                   homework_submissions revised)
--
-- WHY THIS EXISTS
--   Teacher Portal Rebuild — Doc 11 task T-404 (Phase 4, Planner). Doc 08 §5.3.
--   Closes D-HW-1..5 (and unblocks B-HW-1..6 / F-HW-1..4 in T-405/T-406):
--     • D-HW-1 / X-1 — free-text class/section/subject scope replaced by typed
--       assignment_id + class_id + subject_id (the scope spine the backend
--       enforces via requireOwnedAssignment).
--     • D-HW-2 / X-3 — due_date is already typed `date` (T-010/T-004); this adds
--       an optional typed due_time so "is it past due?" is real date+time math.
--     • D-HW-3 / B-HW-5 — NEW homework_attachments table (file/image per HW).
--     • D-HW-4 — allow_late policy column → the no-submit-past-due rule.
--     • D-HW-5 — NEW homework_extensions table (whole-class OR per-student
--       teacher override of the cutoff).
--     • B-HW-6 — homework_submissions gains a TYPED student_uuid FK to students,
--       plus the lifecycle columns (status incl. 'not_submitted', grade,
--       reviewed_by/at) the submissions board (T-405) reads/writes.
--
--   The Exposed mappings (HomeworkTable revised + HomeworkAttachmentsTable +
--   HomeworkExtensionsTable + HomeworkSubmissionsTable revised in
--   server/.../db/Tables.kt, landed alongside T-404) reference these tables.
--   AUTO_CREATE_TABLES is OFF in production and the boot-time validateSchema()
--   gate checks table/column PRESENCE, so this file MUST be applied in Supabase
--   BEFORE the matching backend deploy (T-405). Schema-before-features law.
--
-- WHAT THIS DOES (all additive / idempotent / non-destructive)
--   1. ALTERs homework: ADD assignment_id, class_id, subject_id (typed scope),
--      due_time, allow_late — all nullable/defaulted so existing rows stay valid.
--   2. CREATEs homework_attachments + its FK + index.
--   3. ALTERs homework_submissions: ADD student_uuid (typed FK), grade,
--      reviewed_by, reviewed_at; widens status varchar(16)->text and replaces the
--      old status check with one that includes 'not_submitted'. submitted_at is
--      made NULLable (a 'not_submitted' row has no submission time).
--   4. CREATEs homework_extensions + its FKs + index.
--   5. ADDs FK constraints (homework scope → TSA/class/subject; submissions →
--      homework/students; extensions → homework/students) — all guarded.
--   6. BACKFILLS, best-effort & idempotently, the typed scope columns on existing
--      `homework` rows: resolve each row's free-text (class_name, subject) →
--      class_id/subject_id via school_classes.name / school_subjects.sub_name, and
--      assignment_id via the matching teacher_subject_assignment
--      (same school+class+section+subject[+teacher]). Rows whose scope can't be
--      resolved are LEFT with NULL typed ids (honesty rule — don't fabricate a
--      binding); the legacy display columns remain the fallback.
--      Also backfills homework_submissions.student_uuid from the legacy
--      student_id (students.student_code) where it resolves.
--
-- WHAT THIS DOES NOT DO
--   • Does NOT drop the legacy display columns on homework (class_name/section/
--     subject) or the legacy text student_id on homework_submissions — they keep
--     the existing /homework GET+POST handler/screen compiling green until the
--     T-405/T-406 typed plane replaces them (DELETE-don't-patch). A later phase
--     drops them once every reader derives scope via the FKs.
--   • Does NOT fabricate typed ids for rows whose free-text scope is unresolvable.
--   • Does NOT pre-create 'not_submitted' submission rows — the board computes the
--     not-submitted set by LEFT JOIN against the roster at read time (T-405).
--
-- DEVIATION (filename — docs are authority, deviation flagged):
--   Doc 11 names this `migration_019_homework.sql`; the on-disk chain is
--   contiguous and the prior migration is `016`, so the file is
--   `migration_017_homework.sql` (the same docs-vs-disk offset documented in
--   11_REBUILD_SEQUENCE.md's preamble and in migration_016's header).
--
-- SAFE TO RE-RUN: every step is guarded (ADD COLUMN IF NOT EXISTS /
-- CREATE TABLE IF NOT EXISTS / IF NOT EXISTS constraint guards / UPDATE ... WHERE
-- col IS NULL / ON CONFLICT DO NOTHING).
-- =============================================================================

BEGIN;

-- 1. ------------------------------------------------------------- homework (revised)
--    Typed scope spine + due_time + allow_late. Nullable/defaulted so existing
--    rows remain valid; backfill (step 6) fills the typed ids where resolvable.
ALTER TABLE homework ADD COLUMN IF NOT EXISTS assignment_id uuid    NULL;
ALTER TABLE homework ADD COLUMN IF NOT EXISTS class_id      uuid    NULL;
ALTER TABLE homework ADD COLUMN IF NOT EXISTS subject_id    uuid    NULL;
ALTER TABLE homework ADD COLUMN IF NOT EXISTS due_time      time    NULL;
ALTER TABLE homework ADD COLUMN IF NOT EXISTS allow_late    boolean NOT NULL DEFAULT false;

-- 2. ------------------------------------------------------- homework_attachments (NEW)
CREATE TABLE IF NOT EXISTS homework_attachments (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    homework_id  uuid        NOT NULL,
    url          text        NOT NULL,
    filename     text        NOT NULL DEFAULT '',
    mime         text        NOT NULL DEFAULT '',
    size_bytes   bigint      NOT NULL DEFAULT 0,
    uploaded_by  uuid        NULL,
    created_at   timestamptz NOT NULL DEFAULT now()
);

-- 3. ----------------------------------------------- homework_submissions (revised)
--    Typed student FK + lifecycle columns. submitted_at becomes NULLable (a
--    'not_submitted'/late-pending row has no submission time). status widened to
--    text with a check that includes 'not_submitted'.
ALTER TABLE homework_submissions ADD COLUMN IF NOT EXISTS student_uuid uuid        NULL;
ALTER TABLE homework_submissions ADD COLUMN IF NOT EXISTS grade        text        NULL;
ALTER TABLE homework_submissions ADD COLUMN IF NOT EXISTS reviewed_by  uuid        NULL;
ALTER TABLE homework_submissions ADD COLUMN IF NOT EXISTS reviewed_at  timestamptz NULL;

DO $$
BEGIN
    -- widen status varchar(16) -> text (idempotent: only if not already text).
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'homework_submissions' AND column_name = 'status'
          AND data_type <> 'text'
    ) THEN
        ALTER TABLE homework_submissions ALTER COLUMN status TYPE text;
    END IF;

    -- submitted_at -> NULLable (idempotent).
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'homework_submissions' AND column_name = 'submitted_at'
          AND is_nullable = 'NO'
    ) THEN
        ALTER TABLE homework_submissions ALTER COLUMN submitted_at DROP NOT NULL;
    END IF;

    -- replace the status check to include 'not_submitted' (drop old if present).
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_homework_sub_status') THEN
        ALTER TABLE homework_submissions DROP CONSTRAINT ck_homework_sub_status;
    END IF;
    ALTER TABLE homework_submissions ADD CONSTRAINT ck_homework_sub_status
        CHECK (status IN ('submitted','late','graded','not_submitted'));
END $$;

-- 4. -------------------------------------------------------- homework_extensions (NEW)
--    Teacher override of the cutoff. student_id NULL = whole-class extension.
CREATE TABLE IF NOT EXISTS homework_extensions (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    homework_id   uuid        NOT NULL,
    student_id    uuid        NULL,           -- NULL = extension for the whole class
    new_due_date  date        NOT NULL,
    new_due_time  time        NULL,
    granted_by    uuid        NULL,
    reason        text        NULL,
    created_at    timestamptz NOT NULL DEFAULT now()
);

-- 5. ------------------------------------------------------------------- FK constraints
DO $$
BEGIN
    -- homework typed scope → TSA / class / subject. ON DELETE SET NULL so deleting
    -- a scope row never destroys homework history (the typed col just goes null and
    -- the legacy display column remains the fallback).
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_homework_assignment') THEN
        ALTER TABLE homework ADD CONSTRAINT fk_homework_assignment
            FOREIGN KEY (assignment_id) REFERENCES teacher_subject_assignments(id) ON DELETE SET NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_homework_class') THEN
        ALTER TABLE homework ADD CONSTRAINT fk_homework_class
            FOREIGN KEY (class_id) REFERENCES school_classes(id) ON DELETE SET NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_homework_subject') THEN
        ALTER TABLE homework ADD CONSTRAINT fk_homework_subject
            FOREIGN KEY (subject_id) REFERENCES school_subjects(id) ON DELETE SET NULL;
    END IF;

    -- attachments → homework (a deleted homework removes its attachments).
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_hw_attach_homework') THEN
        ALTER TABLE homework_attachments ADD CONSTRAINT fk_hw_attach_homework
            FOREIGN KEY (homework_id) REFERENCES homework(id) ON DELETE CASCADE;
    END IF;

    -- submissions → homework (cascade) + students (typed FK; SET NULL keeps the
    -- submission row if a student record is removed).
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_hw_sub_homework') THEN
        ALTER TABLE homework_submissions ADD CONSTRAINT fk_hw_sub_homework
            FOREIGN KEY (homework_id) REFERENCES homework(id) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_hw_sub_student') THEN
        ALTER TABLE homework_submissions ADD CONSTRAINT fk_hw_sub_student
            FOREIGN KEY (student_uuid) REFERENCES students(id) ON DELETE SET NULL;
    END IF;

    -- extensions → homework (cascade) + students (SET NULL).
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_hw_ext_homework') THEN
        ALTER TABLE homework_extensions ADD CONSTRAINT fk_hw_ext_homework
            FOREIGN KEY (homework_id) REFERENCES homework(id) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_hw_ext_student') THEN
        ALTER TABLE homework_extensions ADD CONSTRAINT fk_hw_ext_student
            FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE SET NULL;
    END IF;
END $$;

-- 5b. hot-path indexes.
CREATE INDEX IF NOT EXISTS ix_homework_assignment   ON homework (assignment_id);
CREATE INDEX IF NOT EXISTS ix_homework_class_subject ON homework (school_id, class_id, subject_id);
CREATE INDEX IF NOT EXISTS ix_hw_attach_homework    ON homework_attachments (homework_id);
CREATE INDEX IF NOT EXISTS ix_hw_sub_homework       ON homework_submissions (homework_id);
CREATE INDEX IF NOT EXISTS ix_hw_sub_student        ON homework_submissions (student_uuid);
CREATE INDEX IF NOT EXISTS ix_hw_ext_homework       ON homework_extensions (homework_id);

-- 6. ------------------------------------------------ backfill typed scope (best-effort)
DO $$
BEGIN
    -- 6a. homework typed class/subject ids from the free-text display columns.
    UPDATE homework h
       SET class_id = sc.id,
           subject_id = ss.id
      FROM school_classes sc
      JOIN school_subjects ss ON ss.class_id = sc.id
     WHERE sc.school_id = h.school_id
       AND sc.name      = h.class_name
       AND ss.sub_name  = h.subject
       AND (h.class_id IS NULL OR h.subject_id IS NULL);

    -- 6b. homework assignment_id from the matching TSA (same school+class+section+
    --     subject; prefer the authoring teacher when present).
    UPDATE homework h
       SET assignment_id = tsa.id
      FROM teacher_subject_assignments tsa
     WHERE tsa.school_id = h.school_id
       AND tsa.class_id  = h.class_id
       AND tsa.subject_id = h.subject_id
       AND tsa.section   = h.section
       AND (h.teacher_id IS NULL OR tsa.teacher_id = h.teacher_id OR tsa.teacher_id IS NULL)
       AND h.assignment_id IS NULL
       AND h.class_id IS NOT NULL
       AND h.subject_id IS NOT NULL;

    -- 6c. homework_submissions.student_uuid from the legacy text student_id
    --     (students.student_code), scoped to the homework's school.
    UPDATE homework_submissions hs
       SET student_uuid = s.id
      FROM students s
      JOIN homework h ON h.id = hs.homework_id
     WHERE s.school_id    = h.school_id
       AND s.student_code = hs.student_id
       AND hs.student_uuid IS NULL;
END $$;

COMMIT;

-- POST-RUN report (run manually to verify):
--   SELECT count(*) AS homework_rows,
--          count(*) FILTER (WHERE assignment_id IS NOT NULL) AS scoped,
--          count(*) FILTER (WHERE assignment_id IS NULL)     AS unresolved
--     FROM homework;
--   SELECT count(*) AS submissions, count(student_uuid) AS with_typed_student
--     FROM homework_submissions;
--   SELECT count(*) AS attachments FROM homework_attachments;
--   SELECT count(*) AS extensions  FROM homework_extensions;
