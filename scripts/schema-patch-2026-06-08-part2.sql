-- =============================================================================
-- schema-patch-2026-06-08-part2.sql
-- VidyaPrayag — Schema Gap Patch (PostgreSQL / Supabase)  — Part 2 audit
-- -----------------------------------------------------------------------------
-- WHEN TO RUN
--   Apply AFTER scripts/schema-patch-2026-06-07.sql on any Postgres/Supabase DB
--   provisioned via docs/db/PROVISION.sql. Adds the tables + columns introduced
--   by the audit-2026-06-08-part2 remediation so the server's validateSchema()
--   boot gate passes in strict (AUTO_CREATE_TABLES=false) mode.
--
-- IDEMPOTENT
--   Every statement uses IF NOT EXISTS / IF EXISTS / DO blocks. Re-running is a
--   no-op and destroys no data.
--
-- WHAT'S IN HERE  (each mirrors server/.../db/Tables.kt exactly)
--   PATCH-101  app_users.must_change_password           (RA-54)
--   PATCH-102  assessments.is_published + published_at   (RA-43 marks workflow)
--   PATCH-103  leave_requests requester/class/teacher cols (RA-44)
--   PATCH-104  CREATE TABLE notifications                (RA-41/42/46/50)
--   PATCH-105  CREATE TABLE device_tokens               (RA-41 push)
--   PATCH-106  CREATE TABLE parent_child_links          (RA-48)
--   VERIFY     Final SELECTs confirming each object exists.
-- =============================================================================

BEGIN;

-- -----------------------------------------------------------------------------
-- PATCH-101 — app_users.must_change_password (RA-54)
-- Provisioned teachers are created with this = true; POST /auth/change-password
-- clears it (and flips profile_completed=true). The teacher first-login gate
-- now has a real server signal instead of being client-local only.
-- -----------------------------------------------------------------------------
ALTER TABLE app_users
    ADD COLUMN IF NOT EXISTS must_change_password boolean NOT NULL DEFAULT false;

COMMENT ON COLUMN app_users.must_change_password IS
    'RA-54: forced first-login password change pending (provisioned teachers). Cleared by POST /auth/change-password.';

-- -----------------------------------------------------------------------------
-- PATCH-102 — assessments.is_published + published_at (RA-43)
-- Marks are only visible to a parent once the teacher publishes. Parent marks
-- reads filter on is_published = true.
-- -----------------------------------------------------------------------------
ALTER TABLE assessments
    ADD COLUMN IF NOT EXISTS is_published boolean NOT NULL DEFAULT false;
ALTER TABLE assessments
    ADD COLUMN IF NOT EXISTS published_at timestamp without time zone NULL;

COMMENT ON COLUMN assessments.is_published IS 'RA-43: marks become parent-visible only when published.';

-- -----------------------------------------------------------------------------
-- PATCH-103 — leave_requests cross-role columns (RA-44)
-- Adds the addressing columns the parent→teacher→admin leave workflow needs.
-- -----------------------------------------------------------------------------
ALTER TABLE leave_requests ADD COLUMN IF NOT EXISTS class_id    uuid        NULL;
ALTER TABLE leave_requests ADD COLUMN IF NOT EXISTS class_name  varchar(64) NULL;
ALTER TABLE leave_requests ADD COLUMN IF NOT EXISTS section     varchar(16) NULL;
ALTER TABLE leave_requests ADD COLUMN IF NOT EXISTS teacher_id  uuid        NULL;
ALTER TABLE leave_requests ADD COLUMN IF NOT EXISTS child_id    uuid        NULL;
ALTER TABLE leave_requests ADD COLUMN IF NOT EXISTS parent_id   uuid        NULL;

CREATE INDEX IF NOT EXISTS ix_leave_requests_teacher ON leave_requests (school_id, teacher_id, status);
CREATE INDEX IF NOT EXISTS ix_leave_requests_parent  ON leave_requests (parent_id);

-- -----------------------------------------------------------------------------
-- PATCH-104 — notifications table (RA-41/42/46/50)
-- The cross-user notification spine. Recipient-scoped (user_id) AND
-- school-scoped (school_id). notify() writes here; GET /notifications reads by
-- jwt.sub; /summary returns the unread count for the bells.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notifications (
    id          uuid                        PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id   uuid                        NULL,
    user_id     uuid                        NOT NULL,
    category    varchar(32)                 NOT NULL DEFAULT 'general',
    title       text                        NOT NULL,
    body        text                        NOT NULL DEFAULT '',
    deep_link   text                        NULL,
    actor_id    uuid                        NULL,
    ref_type    varchar(32)                 NULL,
    ref_id      text                        NULL,
    is_read     boolean                     NOT NULL DEFAULT false,
    created_at  timestamp without time zone NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    read_at     timestamp without time zone NULL
);

COMMENT ON TABLE notifications IS 'RA-41/42/46/50: cross-user notification spine. Recipient = user_id (jwt.sub on read), school-scoped by school_id.';

CREATE INDEX IF NOT EXISTS ix_notifications_user_unread ON notifications (user_id, is_read, created_at);
CREATE INDEX IF NOT EXISTS ix_notifications_school      ON notifications (school_id);

-- -----------------------------------------------------------------------------
-- PATCH-105 — device_tokens table (RA-41 push, Phase E)
-- FCM/APNs token registry, per user + device. Push dispatch is Phase E; the
-- table exists now so the registration endpoint has somewhere to write.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS device_tokens (
    id          uuid                        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     uuid                        NOT NULL,
    token       text                        NOT NULL,
    platform    varchar(16)                 NOT NULL DEFAULT 'android',
    is_active   boolean                     NOT NULL DEFAULT true,
    created_at  timestamp without time zone NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    updated_at  timestamp without time zone NOT NULL DEFAULT (now() AT TIME ZONE 'UTC')
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_device_tokens_token ON device_tokens (token);
CREATE INDEX IF NOT EXISTS ix_device_tokens_user ON device_tokens (user_id, is_active);

-- -----------------------------------------------------------------------------
-- PATCH-106 — parent_child_links table (RA-48)
-- Approval workflow for parent→child linking. POST /parent/link-child creates a
-- pending row; admin GET /school/link-requests + approve/reject resolves it.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS parent_child_links (
    id            uuid                        PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_id     uuid                        NOT NULL,
    school_id     uuid                        NULL,
    student_code  varchar(64)                 NULL,
    roll_number   varchar(64)                 NULL,
    child_name    text                        NULL,
    child_id      uuid                        NULL,
    status        varchar(16)                 NOT NULL DEFAULT 'pending',
    requested_at  timestamp without time zone NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    actioned_by   uuid                        NULL,
    actioned_at   timestamp without time zone NULL
);

COMMENT ON TABLE parent_child_links IS 'RA-48: parent→child link approval. status: pending | approved | rejected.';

CREATE INDEX IF NOT EXISTS ix_parent_child_links_school_status ON parent_child_links (school_id, status);
CREATE INDEX IF NOT EXISTS ix_parent_child_links_parent        ON parent_child_links (parent_id);

COMMIT;

-- =============================================================================
-- VERIFICATION — every object below must report 'exists'/'present'.
-- =============================================================================
SELECT 'app_users.must_change_password' AS object,
       CASE WHEN EXISTS (SELECT 1 FROM information_schema.columns
                         WHERE table_name='app_users' AND column_name='must_change_password')
            THEN 'present' ELSE 'MISSING' END AS state
UNION ALL
SELECT 'assessments.is_published',
       CASE WHEN EXISTS (SELECT 1 FROM information_schema.columns
                         WHERE table_name='assessments' AND column_name='is_published')
            THEN 'present' ELSE 'MISSING' END
UNION ALL
SELECT 'leave_requests.teacher_id',
       CASE WHEN EXISTS (SELECT 1 FROM information_schema.columns
                         WHERE table_name='leave_requests' AND column_name='teacher_id')
            THEN 'present' ELSE 'MISSING' END
UNION ALL
SELECT 'notifications',
       CASE WHEN to_regclass('public.notifications') IS NOT NULL THEN 'exists' ELSE 'MISSING' END
UNION ALL
SELECT 'device_tokens',
       CASE WHEN to_regclass('public.device_tokens') IS NOT NULL THEN 'exists' ELSE 'MISSING' END
UNION ALL
SELECT 'parent_child_links',
       CASE WHEN to_regclass('public.parent_child_links') IS NOT NULL THEN 'exists' ELSE 'MISSING' END;

-- =============================================================================
-- END schema-patch-2026-06-08-part2.sql
-- =============================================================================
