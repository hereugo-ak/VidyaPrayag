-- =====================================================================
-- migration_066_tutor_sessions_jsonb_to_text.sql
--
-- FIX: tutor_sessions.turns and tutor_sessions.grounded_refs were created
-- as JSONB in migration_064, but the Exposed table definition maps them as
-- text(). PostgreSQL won't implicitly cast VARCHAR → JSONB in prepared
-- statements, causing INSERT failures. Since we store JSON strings and
-- don't use PostgreSQL's JSONB querying for these columns, alter them to
-- TEXT so the Exposed text() type matches perfectly.
--
-- Idempotent — safe to re-run.
-- =====================================================================

ALTER TABLE tutor_sessions
    ALTER COLUMN turns TYPE TEXT USING turns::text;

ALTER TABLE tutor_sessions
    ALTER COLUMN grounded_refs TYPE TEXT USING grounded_refs::text;

-- Update defaults to text (not jsonb cast)
ALTER TABLE tutor_sessions
    ALTER COLUMN turns SET DEFAULT '[]';

ALTER TABLE tutor_sessions
    ALTER COLUMN grounded_refs SET DEFAULT '[]';
