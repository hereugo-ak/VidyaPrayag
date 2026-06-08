-- =============================================================================
-- Migration 003 — cross-role leave workflow routing + two-party messaging engine
--
-- WHY THIS EXISTS
--   The backend gained two features whose Exposed mappings in
--   server/.../db/Tables.kt now reference columns the live Supabase schema does
--   NOT yet have:
--     • RA-44 — parent files leave for a child → routed to the class teacher →
--               teacher decides / admin overrides → parent notified. Needs
--               class/teacher/child/parent routing context on leave_requests.
--     • RA-51 — real parent↔teacher (and admin↔parent) conversations. The old
--               single-owner thread model can't represent a shared history, so
--               MessagingCore.kt now creates ONE thread row per participant, all
--               sharing a conversation_id. Needs conversation_id/peer_user_id.
--   AUTO_CREATE_TABLES is OFF in production (DatabaseFactory.kt runs NO migration
--   against Postgres — only SQLite gets createMissingTablesAndColumns). Without
--   this migration the backend fails on Supabase/Render with errors like:
--       column "class_id" of relation "leave_requests" does not exist
--       column "conversation_id" of relation "message_threads" does not exist
--
-- HOW TO RUN
--   Supabase -> SQL Editor -> paste this whole file -> Run.
--   Safe to re-run: every statement is guarded (ADD COLUMN IF NOT EXISTS /
--   CREATE INDEX IF NOT EXISTS). It only ADDS nullable columns + helper indexes;
--   it never drops, rewrites, or backfills data.
--
-- RUN ORDER (full provisioning):
--   1. vidyasetu_schema.sql
--   2. migration_001_faculty_and_holiday_list.sql
--   3. migration_002_segmentation_geo_assignments.sql
--   4. migration_003_leave_workflow_and_two_party_messaging.sql   <-- this file
--
-- Column names/types match server/.../db/Tables.kt EXACTLY.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1) leave_requests — class/teacher/child/parent routing context  (RA-44)
--    A parent applies on behalf of a child; the request is routed to the
--    child's class teacher (resolved at apply time from
--    teacher_subject_assignments) and can be decided by that teacher or
--    overridden by an admin. All nullable so legacy student/teacher-leave rows
--    still load.
--      class_id    : FK school_classes.id (the class; name is canonical)
--      class_name  : denormalized class label the teacher owns
--      section     : denormalized section label
--      teacher_id  : FK app_users.id of the routed-to teacher
--      child_id    : FK children.id (the student the leave is for)
--      parent_id   : FK app_users.id of the applying parent (notify target)
-- ---------------------------------------------------------------------------
ALTER TABLE public.leave_requests
  ADD COLUMN IF NOT EXISTS class_id   uuid NULL;
ALTER TABLE public.leave_requests
  ADD COLUMN IF NOT EXISTS class_name character varying(64) NULL;
ALTER TABLE public.leave_requests
  ADD COLUMN IF NOT EXISTS section    character varying(16) NULL;
ALTER TABLE public.leave_requests
  ADD COLUMN IF NOT EXISTS teacher_id uuid NULL;
ALTER TABLE public.leave_requests
  ADD COLUMN IF NOT EXISTS child_id   uuid NULL;
ALTER TABLE public.leave_requests
  ADD COLUMN IF NOT EXISTS parent_id  uuid NULL;

-- A teacher pulling their pending leave queue filters by (school, teacher, status).
CREATE INDEX IF NOT EXISTS ix_leave_requests_teacher
  ON public.leave_requests (school_id, teacher_id, status);

-- ---------------------------------------------------------------------------
-- 2) message_threads / messages — two-party conversation engine  (RA-51)
--    Both participants get one thread row each, sharing the same conversation_id.
--    peer_user_id identifies the other party; messages are keyed by
--    conversation_id so both sides see one history. All nullable so legacy
--    single-owner rows keep working (conversationKey() falls back to the
--    thread's own id when conversation_id IS NULL).
-- ---------------------------------------------------------------------------
ALTER TABLE public.message_threads
  ADD COLUMN IF NOT EXISTS conversation_id uuid NULL;
ALTER TABLE public.message_threads
  ADD COLUMN IF NOT EXISTS peer_user_id    uuid NULL;
ALTER TABLE public.messages
  ADD COLUMN IF NOT EXISTS conversation_id uuid NULL;

CREATE INDEX IF NOT EXISTS ix_message_threads_conversation
  ON public.message_threads (school_id, conversation_id);
CREATE INDEX IF NOT EXISTS ix_message_threads_owner_peer
  ON public.message_threads (owner_user_id, peer_user_id);
CREATE INDEX IF NOT EXISTS ix_messages_conversation
  ON public.messages (conversation_id, created_at);

-- =============================================================================
-- DONE. After running this, the following should work without errors:
--   POST /api/v1/parent/leave                  (parent files leave for a child)
--   GET/POST /api/v1/teacher/leave-requests     (teacher decides)
--   GET/POST /api/v1/teacher/messages/*         (two-party + class broadcast)
--   GET/POST /api/v1/parent/messages/*          (two-party reply path)
--   GET/POST /api/v1/school/messages/*          (admin two-party path)
-- =============================================================================

-- VERIFICATION — all 9 added columns must report 'present'
SELECT 'leave_requests.class_id'        AS object, CASE WHEN EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='leave_requests'  AND column_name='class_id')        THEN 'present' ELSE 'MISSING' END AS state
UNION ALL SELECT 'leave_requests.class_name',  CASE WHEN EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='leave_requests'  AND column_name='class_name')  THEN 'present' ELSE 'MISSING' END
UNION ALL SELECT 'leave_requests.section',     CASE WHEN EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='leave_requests'  AND column_name='section')     THEN 'present' ELSE 'MISSING' END
UNION ALL SELECT 'leave_requests.teacher_id',  CASE WHEN EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='leave_requests'  AND column_name='teacher_id')  THEN 'present' ELSE 'MISSING' END
UNION ALL SELECT 'leave_requests.child_id',    CASE WHEN EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='leave_requests'  AND column_name='child_id')    THEN 'present' ELSE 'MISSING' END
UNION ALL SELECT 'leave_requests.parent_id',   CASE WHEN EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='leave_requests'  AND column_name='parent_id')   THEN 'present' ELSE 'MISSING' END
UNION ALL SELECT 'message_threads.conversation_id', CASE WHEN EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='message_threads' AND column_name='conversation_id') THEN 'present' ELSE 'MISSING' END
UNION ALL SELECT 'message_threads.peer_user_id',    CASE WHEN EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='message_threads' AND column_name='peer_user_id')    THEN 'present' ELSE 'MISSING' END
UNION ALL SELECT 'messages.conversation_id',        CASE WHEN EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='messages'        AND column_name='conversation_id') THEN 'present' ELSE 'MISSING' END;

-- END    docs/db/migration_003_leave_workflow_and_two_party_messaging.sql
