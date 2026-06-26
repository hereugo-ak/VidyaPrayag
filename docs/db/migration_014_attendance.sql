-- =============================================================================
-- Migration 014 — typed student attendance (attendance_records revised)
--
-- WHY THIS EXISTS
--   Teacher Portal Rebuild — Doc 11 task T-201 (Phase 2, Attendance). Doc 06 §1.2.
--   Closes D-ATT-1 (no `leave` state), D-ATT-2 (no typed student FK), D-ATT-4 /
--   X-1 (class/section parsed from a packed `grade` string instead of a provable
--   assignment binding).
--
--   The legacy `attendance_records` identifies a student by a free
--   `person_id` (the student_code) + a packed `grade` "<class>-<section>"
--   string, supports only present|absent|late, and has no link to the
--   authorizing teacher_subject_assignment. This migration moves it to the
--   Doc 06 §1.2 TYPED shape:
--     • student_id    uuid NULL  FK -> students            (typed WHO, D-ATT-2)
--     • enrollment_id uuid NULL  FK -> enrollments         (exact class membership, X-1)
--     • faculty_id    uuid NULL  FK -> app_users           (when type='faculty')
--     • assignment_id uuid NULL  FK -> teacher_subject_assignments (the period/class)
--     • status        +'leave'                              (D-ATT-1)
--     • source        text DEFAULT 'manual'                 (manual|leave_auto|bulk|biometric)
--     • marked_at     timestamptz DEFAULT now()
--   and replaces the UNIQUE key with
--     (school_id, date, type, student_id, assignment_id)    -- one mark per
--                                                              student per class
--                                                              per day (Doc 06 §1.2).
--
--   The new Exposed mapping (AttendanceRecordsTable in server/.../db/Tables.kt)
--   references these columns. AUTO_CREATE_TABLES is OFF in production; the
--   boot-time validateSchema() gate only checks table PRESENCE (not columns),
--   but the typed read/write handlers (T-203) require these columns, so this
--   file MUST be applied in Supabase BEFORE the matching backend deploy.
--
-- WHAT THIS DOES
--   1. ADDS the typed columns (student_id, enrollment_id, faculty_id,
--      assignment_id, source, marked_at) — all nullable/defaulted, so the ADD is
--      non-destructive and re-runnable (IF NOT EXISTS via DO blocks).
--   2. RELAXES person_id to NULL (typed rows don't need it; it is retained only
--      to QUARANTINE legacy rows that can't be resolved — never deleted).
--   3. BACKFILLS, best-effort and idempotently:
--        • student_id    from students.student_code = person_id (same school);
--        • enrollment_id from the student's active enrollment whose class+section
--          matches the packed `grade` (when uniquely resolvable);
--        • assignment_id is LEFT NULL on backfill — the legacy `grade` does not
--          name a subject, so the authorizing TSA is ambiguous (multiple subject
--          teachers per class). Per Doc 06 §1.2 "best-effort; quarantine", we do
--          NOT guess (honesty rule); new writes (T-203) always set it.
--      Rows whose student_id can't be resolved keep their person_id and are thus
--      QUARANTINED (still queryable, never lost).
--   4. ADDS the typed UNIQUE constraint and the hot-path indexes. The legacy
--      ux_att_records_unique (school,date,type,person_id) is KEPT for now (it is
--      harmless for the retained legacy/faculty rows and avoids invalidating the
--      old writer before T-203 replaces it); the new key is additive.
--   5. ADDS a CHECK constraint enforcing status IN (present,absent,late,leave)
--      and type IN (student,faculty) (guarded; dropped+recreated so re-runs are
--      safe).
--
-- DEVIATIONS (flagged per Doc 11 "docs are authority")
--   • Filename: Doc 11 T-201 names it `migration_016_attendance.sql`. The on-disk
--     chain is contiguous (…012, 013_teacher_checkins), so this is
--     `migration_014_attendance.sql` — the same documented docs-number-vs-disk
--     offset pattern as 009/010/011/012/013.
--   • `grade` is NOT physically dropped yet. Doc 06 §1.2 says "grade string is
--     dropped". Dropping it now would break readers that still consume it and
--     are OUT of this phase's scope — the T-104 resolver's attendanceMarked join,
--     admin dashboards, parent academics, the legacy teacher attendance writer.
--     Those migrate to the typed columns in T-203/T-205; the physical
--     `ALTER TABLE … DROP COLUMN grade` is deferred to a follow-up migration once
--     every reader is off it. The Exposed mapping marks `grade` DEPRECATED to
--     reflect this. (Keeping a now-unused-by-new-code column is strictly safer
--     than an early drop that breaks shipped endpoints.)
--   • assignment_id is left NULL on historical rows (see step 3) rather than
--     fabricated — preserves the honesty rule; it becomes authoritative for all
--     NEW marks via T-203.
--
-- HOW TO RUN
--   Supabase -> SQL Editor -> paste this whole file -> Run. Safe to re-run:
--   every step is guarded (IF NOT EXISTS / DROP-then-ADD for constraints).
--
-- RUN ORDER (appended to the existing chain):
--   ...
--   13. docs/db/migration_013_teacher_checkins.sql
--   14. docs/db/migration_014_attendance.sql            <-- THIS FILE
--
-- Column names/types match server/.../db/Tables.kt (AttendanceRecordsTable)
-- EXACTLY (Exposed timestamp() -> timestamptz, uuid() -> uuid, date() -> date).
-- =============================================================================

BEGIN;

-- ─────────────────────────────────────────────────────────────────────────────
-- 1. Typed columns (Doc 06 §1.2). All nullable/defaulted → non-destructive ADD.
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE public.attendance_records
    ADD COLUMN IF NOT EXISTS student_id    uuid NULL,
    ADD COLUMN IF NOT EXISTS enrollment_id uuid NULL,
    ADD COLUMN IF NOT EXISTS faculty_id    uuid NULL,
    ADD COLUMN IF NOT EXISTS assignment_id uuid NULL,
    ADD COLUMN IF NOT EXISTS source        text NOT NULL DEFAULT 'manual',
    ADD COLUMN IF NOT EXISTS marked_at     timestamptz NULL;

-- 2. Relax person_id to NULL (typed rows don't need it; retained for quarantine).
ALTER TABLE public.attendance_records
    ALTER COLUMN person_id DROP NOT NULL;

-- FK references (guarded — added only if not already present). ON DELETE SET NULL
-- so deleting a student/enrollment/assignment never deletes the attendance audit
-- trail; the row degrades to quarantined rather than vanishing.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_att_student') THEN
        ALTER TABLE public.attendance_records
            ADD CONSTRAINT fk_att_student
            FOREIGN KEY (student_id) REFERENCES public.students(id) ON DELETE SET NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_att_enrollment') THEN
        ALTER TABLE public.attendance_records
            ADD CONSTRAINT fk_att_enrollment
            FOREIGN KEY (enrollment_id) REFERENCES public.enrollments(id) ON DELETE SET NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_att_faculty') THEN
        ALTER TABLE public.attendance_records
            ADD CONSTRAINT fk_att_faculty
            FOREIGN KEY (faculty_id) REFERENCES public.app_users(id) ON DELETE SET NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_att_assignment') THEN
        ALTER TABLE public.attendance_records
            ADD CONSTRAINT fk_att_assignment
            FOREIGN KEY (assignment_id) REFERENCES public.teacher_subject_assignments(id) ON DELETE SET NULL;
    END IF;
END $$;

-- ─────────────────────────────────────────────────────────────────────────────
-- 3. Best-effort, idempotent backfill of the typed identity for STUDENT rows.
--    Only touches rows that haven't been resolved yet (student_id IS NULL) so a
--    re-run is a no-op once applied.
-- ─────────────────────────────────────────────────────────────────────────────

-- 3a. student_id ← students.student_code = person_id (same school).
UPDATE public.attendance_records a
SET student_id = s.id
FROM public.students s
WHERE a.type = 'student'
  AND a.student_id IS NULL
  AND a.person_id IS NOT NULL
  AND s.school_id = a.school_id
  AND s.student_code = a.person_id;

-- 3b. enrollment_id ← the student's enrollment whose class+section matches the
--     packed `grade` "<class>-<section>". Only set when EXACTLY ONE enrollment
--     matches (avoid guessing across transfers); ambiguous rows stay NULL.
--     The packed grade was built from the TSA's denormalised class_name
--     (TeacherRoutingTasks: "${asg.className}-${asg.section}"). The class name
--     convention can be either school_classes.name OR .code, so we match BOTH to
--     be robust to either school's seeding (still single-match guarded).
UPDATE public.attendance_records a
SET enrollment_id = e.id
FROM public.enrollments e
JOIN public.school_classes c ON c.id = e.class_id
WHERE a.type = 'student'
  AND a.student_id IS NOT NULL
  AND a.enrollment_id IS NULL
  AND a.grade IS NOT NULL
  AND e.student_id = a.student_id
  AND e.school_id = a.school_id
  AND (
        (c.name || '-' || e.section) = a.grade OR
        (c.code || '-' || e.section) = a.grade
      )
  AND (
        SELECT count(*) FROM public.enrollments e2
        JOIN public.school_classes c2 ON c2.id = e2.class_id
        WHERE e2.student_id = a.student_id
          AND e2.school_id = a.school_id
          AND (
                (c2.name || '-' || e2.section) = a.grade OR
                (c2.code || '-' || e2.section) = a.grade
              )
      ) = 1;

-- 3c. marked_at backfill — historical rows get their created_at as a best-effort
--     "when it was marked" so the column is non-null for legacy data too.
UPDATE public.attendance_records
SET marked_at = created_at
WHERE marked_at IS NULL;

-- NOTE: assignment_id is intentionally NOT backfilled (see header DEVIATION 3) —
-- the legacy grade names no subject, so the authorizing TSA is ambiguous. New
-- marks set it authoritatively (T-203). Rows where student_id is still NULL are
-- QUARANTINED (their person_id is retained) — never deleted.

-- ─────────────────────────────────────────────────────────────────────────────
-- 4. Typed UNIQUE + hot-path indexes (additive; legacy UNIQUE kept for now).
--    Postgres treats NULLs as distinct, so this only enforces uniqueness for
--    fully-typed student rows (the only writers after T-203) — exactly the
--    "one mark per student per class per day" rule (Doc 06 §1.2).
-- ─────────────────────────────────────────────────────────────────────────────
CREATE UNIQUE INDEX IF NOT EXISTS ux_att_records_typed_unique
    ON public.attendance_records (school_id, date, type, student_id, assignment_id);

CREATE INDEX IF NOT EXISTS ix_att_student
    ON public.attendance_records (school_id, student_id, date);
CREATE INDEX IF NOT EXISTS ix_att_assignment
    ON public.attendance_records (school_id, assignment_id, date);

-- ─────────────────────────────────────────────────────────────────────────────
-- 5. CHECK constraints for the typed state space (drop-then-add → re-runnable).
--    status now includes 'leave' (D-ATT-1). We normalise any legacy upper/mixed
--    case to lowercase FIRST so the CHECK can't reject existing rows.
-- ─────────────────────────────────────────────────────────────────────────────
UPDATE public.attendance_records SET status = lower(status) WHERE status <> lower(status);
UPDATE public.attendance_records SET type   = lower(type)   WHERE type   <> lower(type);

ALTER TABLE public.attendance_records
    DROP CONSTRAINT IF EXISTS chk_att_status,
    DROP CONSTRAINT IF EXISTS chk_att_type;
ALTER TABLE public.attendance_records
    ADD CONSTRAINT chk_att_status CHECK (status IN ('present','absent','late','leave')),
    ADD CONSTRAINT chk_att_type   CHECK (type   IN ('student','faculty'));

COMMIT;

-- =============================================================================
-- POST-RUN REPORT — quantify the migration's reach + any quarantined rows.
--   • total            : all attendance rows.
--   • resolved_students: student rows now carrying a typed student_id.
--   • quarantined      : student rows that could NOT be resolved (person_id kept).
--   • with_enrollment  : student rows scoped to an exact enrollment.
-- A healthy seeded DB shows quarantined = 0; any >0 are safe to inspect by
-- person_id and re-link manually (their data is intact, never dropped).
-- =============================================================================
SELECT
    count(*)                                                              AS total,
    count(*) FILTER (WHERE type = 'student' AND student_id IS NOT NULL)   AS resolved_students,
    count(*) FILTER (WHERE type = 'student' AND student_id IS NULL)       AS quarantined,
    count(*) FILTER (WHERE type = 'student' AND enrollment_id IS NOT NULL) AS with_enrollment
FROM public.attendance_records;
