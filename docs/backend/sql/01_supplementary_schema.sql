-- =============================================================================
-- VidyaPrayag — SUPPLEMENTARY SCHEMA (additive, idempotent)
-- Version: 1.0  |  Engine: PostgreSQL (Supabase)
--
-- ⚠️  RA-63 NOTE: This file is a HISTORICAL fragment. The canonical, current
--     provisioning source is scripts/schema-all-in-one-2026-06-07.sql (built
--     from docs/db/vidyasetu_schema.sql + migration_001/002/003 + patches).
--     See docs/db/PROVISION.sql / scripts/README-RUN-ORDER.md. Do NOT use the
--     legacy root schema (archived to docs/_archive/...ABANDONED.sql).
--
-- WHAT THIS FILE DOES
-- -------------------
-- This file adds the tables that the Ktor backend needs on top of the base
-- operational tables (schools, students, attendance, fees, etc.):
--
--   1.  app_users               — our own user record (decoupled from
--                                  auth.users so phone-OTP-only signup works
--                                  without an email + Supabase Auth session)
--   2.  auth_otps               — industrial-grade OTP storage with TTL,
--                                  attempt counter, rate-limit, IP/device log
--   3.  user_sessions           — refresh-token store (rotating)
--   4.  cms_landing_content     — KV store for the landing page (Screen 1)
--   5.  app_config              — KV store for /config/app-status (Splash)
--   6.  school_onboarding_drafts — per-user step drafts
--   7.  school_classes          — Grade 5, Grade 10, … (with sections csv)
--   8.  school_subjects         — subjects per class
--   9.  announcements           — Holidays / PTM / Events / Special / Remainder
--  10.  whatsapp_logs           — outbound sync log
--  11.  admission_enquiries     — leads in the school CRM
--  12.  school_philosophy       — core_mission / learning_model / language
--  13.  school_media            — gallery (images + video tour urls)
--  14.  storage_metrics         — per-school storage usage
--  15.  academic_calendar       — calendar events
--  16.  holiday_list            — holiday list per frequency
--  17.  faculty                 — lookup for attendance lists
--  18.  attendance_records      — daily attendance (student | faculty)
--
-- ALL `CREATE TABLE` statements use `IF NOT EXISTS` so you can re-run this
-- file safely after any change. Run it in:
--   Supabase Dashboard → SQL Editor → New Query → paste → Run
--
-- ⚠️  PREREQUISITE (RA-63): the canonical path bundles this fragment into
--                  scripts/schema-all-in-one-2026-06-07.sql — run THAT, not
--                  the legacy root schema. This file only adds extra tables.
--
-- AUTOMATIC OTP CLEANUP
-- ---------------------
-- We attach a cleanup helper + a row-level trigger so that expired OTPs are
-- physically deleted on insert/update.  In addition, a `pg_cron` job (one
-- line, optional) deletes expired OTPs every minute — see SECTION 99.
-- =============================================================================


-- =============================================================================
-- SECTION 0 — EXTENSIONS (already enabled by base schema, kept here for safety)
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";


-- =============================================================================
-- SECTION 1 — app_users
--
-- Decoupled from auth.users so we can support pure phone+OTP onboarding
-- without first creating a Supabase Auth account (which always needs an
-- email).  When a user later links an email, we copy auth.users.id into
-- `linked_auth_user_id` to keep them addressable from Supabase Auth too.
-- =============================================================================

CREATE TABLE IF NOT EXISTS app_users (
  id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  linked_auth_user_id   UUID UNIQUE REFERENCES auth.users(id) ON DELETE SET NULL,
  school_id             UUID REFERENCES schools(id) ON DELETE SET NULL,

  role                  user_role NOT NULL DEFAULT 'parent',

  full_name             TEXT NOT NULL,
  phone                 TEXT UNIQUE,                -- E.164: +919876543210
  email                 TEXT UNIQUE,
  password_hash         TEXT,                       -- BCrypt for email login
  profile_pic_url       TEXT,
  language_pref         lang_pref DEFAULT 'hi',

  is_phone_verified     BOOLEAN DEFAULT FALSE,
  is_email_verified     BOOLEAN DEFAULT FALSE,
  profile_completed     BOOLEAN DEFAULT FALSE,

  is_active             BOOLEAN DEFAULT TRUE,
  last_login_at         TIMESTAMPTZ,
  created_at            TIMESTAMPTZ DEFAULT NOW(),
  updated_at            TIMESTAMPTZ DEFAULT NOW(),

  -- A user must have at least ONE identifier:
  CONSTRAINT chk_app_users_has_identifier CHECK (
    phone IS NOT NULL OR email IS NOT NULL
  )
);

CREATE INDEX IF NOT EXISTS idx_app_users_phone  ON app_users (phone);
CREATE INDEX IF NOT EXISTS idx_app_users_email  ON app_users (email);
CREATE INDEX IF NOT EXISTS idx_app_users_role   ON app_users (role);
CREATE INDEX IF NOT EXISTS idx_app_users_school ON app_users (school_id);

DROP TRIGGER IF EXISTS trg_app_users_updated_at ON app_users;
CREATE TRIGGER trg_app_users_updated_at
  BEFORE UPDATE ON app_users
  FOR EACH ROW EXECUTE FUNCTION handle_updated_at();


-- =============================================================================
-- SECTION 2 — auth_otps   (industrial-grade)
--
-- DESIGN GOALS
--  a) ABSOLUTE 10-minute lifetime — `expires_at = sent_at + 10 minutes`.
--     The frontend MUST reject if `now() > expires_at`, AND the backend
--     enforces this on `verify` too.
--  b) Resend within 10 min OVERWRITES the same row (UPSERT on
--     (identifier, purpose)).  The OTP code rotates, `expires_at` resets,
--     `attempt_count` resets, `resend_count` increments.
--  c) Rate limit: max 5 resends per identifier per hour
--     (`resend_count` + `first_sent_at`).
--  d) Brute-force lock: max 5 wrong verify attempts before the OTP is
--     auto-marked `is_locked = TRUE` and a fresh OTP must be requested.
--  e) Defence in depth: we store `code_hash` (sha-256), NOT plain code.
--     Even a DB leak doesn't reveal active OTPs.
--  f) Audit: ip_address, user_agent, device_id are logged for fraud review.
--  g) Hard delete after expiry (no soft-delete) so PII surface stays tiny.
-- =============================================================================

CREATE TABLE IF NOT EXISTS auth_otps (
  id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  -- Identifier the OTP was sent to (phone in E.164 form, or email).
  identifier          TEXT NOT NULL,
  identifier_type     TEXT NOT NULL CHECK (identifier_type IN ('phone', 'email')),

  -- Why was this OTP sent?
  purpose             TEXT NOT NULL DEFAULT 'login'
                        CHECK (purpose IN ('login', 'signup', 'reset_password',
                                           'verify_phone', 'verify_email')),

  -- SHA-256 hex of (code || pepper).  Never store the plain code.
  code_hash           TEXT NOT NULL,
  -- First 6 chars of bcrypt id (rotated per OTP) so old hashes stay invalid
  -- after a code-rotation even if the row is reused. Optional, but cheap.
  code_salt           TEXT NOT NULL,

  -- Timestamps & lifetime
  sent_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  first_sent_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),  -- never reset by resend
  expires_at          TIMESTAMPTZ NOT NULL DEFAULT NOW() + INTERVAL '10 minutes',

  -- Counters
  resend_count        SMALLINT NOT NULL DEFAULT 0,         -- # of resends in window
  attempt_count       SMALLINT NOT NULL DEFAULT 0,         -- # of wrong verifies
  max_attempts        SMALLINT NOT NULL DEFAULT 5,
  max_resends         SMALLINT NOT NULL DEFAULT 5,
  resend_window_secs  INTEGER  NOT NULL DEFAULT 3600,      -- 1 hour

  -- Outcome flags
  is_verified         BOOLEAN  NOT NULL DEFAULT FALSE,
  is_locked           BOOLEAN  NOT NULL DEFAULT FALSE,     -- too many failed attempts
  verified_at         TIMESTAMPTZ,

  -- Audit
  ip_address          TEXT,
  user_agent          TEXT,
  device_id           TEXT,
  delivery_channel    TEXT,                                 -- 'sms' | 'whatsapp' | 'email' | 'mock'
  delivery_provider   TEXT,                                 -- 'twilio' | 'msg91' | 'gupshup' | …
  provider_message_id TEXT,

  created_at          TIMESTAMPTZ DEFAULT NOW(),
  updated_at          TIMESTAMPTZ DEFAULT NOW(),

  -- ONE active OTP per (identifier, purpose).
  -- This is the magic that makes "resend overwrites" work via UPSERT.
  CONSTRAINT ux_auth_otps_identifier_purpose UNIQUE (identifier, purpose)
);

CREATE INDEX IF NOT EXISTS idx_auth_otps_expires_at ON auth_otps (expires_at);
CREATE INDEX IF NOT EXISTS idx_auth_otps_identifier ON auth_otps (identifier);
CREATE INDEX IF NOT EXISTS idx_auth_otps_locked     ON auth_otps (is_locked)
  WHERE is_locked = TRUE;

DROP TRIGGER IF EXISTS trg_auth_otps_updated_at ON auth_otps;
CREATE TRIGGER trg_auth_otps_updated_at
  BEFORE UPDATE ON auth_otps
  FOR EACH ROW EXECUTE FUNCTION handle_updated_at();


-- ---- Helper: physically delete every expired OTP row.  Called by:
--      (a) backend on every send/verify (in-line)
--      (b) pg_cron once a minute (SECTION 99 below)
CREATE OR REPLACE FUNCTION purge_expired_otps()
RETURNS INTEGER
LANGUAGE plpgsql
AS $$
DECLARE
  v_deleted INTEGER;
BEGIN
  DELETE FROM auth_otps
   WHERE expires_at < NOW()
      OR (is_verified = TRUE AND verified_at < NOW() - INTERVAL '5 minutes');
  GET DIAGNOSTICS v_deleted = ROW_COUNT;
  RETURN v_deleted;
END;
$$;


-- =============================================================================
-- SECTION 2b — otp_delivery_attempts  (full forensic trail)
--
-- WHY THIS EXISTS
-- ---------------
-- `auth_otps` only records the WINNING provider for one OTP.  But when an
-- OTP fails to arrive, support / ops need to know exactly which providers
-- were tried, in what order, what each one returned, and how slow they
-- were.  Without this table the only forensic source is stdout logs,
-- which Render free tier truncates after a few hours.
--
-- HOW IT'S USED
-- -------------
-- The Ktor backend (OtpService.persistDeliveryAttempts) writes ONE row
-- per provider attempt during a single /send-otp call:
--
--   identifier=+919876XXXX, purpose=login →
--     row 0:  fast2sms     sms        sent       latency=512ms  msg_id=abc
--   (chain stopped — first Sent wins)
--
-- Or on a multi-failure:
--
--   row 0:  whatsapp_cloud whatsapp   skipped    "not configured"
--   row 1:  fast2sms       sms        failed     http=401  "invalid key"
--   row 2:  msg91          sms        failed     http=500
--   row 3:  twilio         sms        sent       latency=812ms
--
-- ADMIN ACCESS
--   GET /api/v1/admin/otp/attempts?identifier=...  (gated by OTP_ADMIN_TOKEN)
--
-- RETENTION
--   Rows are kept 7 days then purged by `purge_old_delivery_attempts()`
--   below.  Run it from pg_cron OR let the Ktor backend call it
--   opportunistically on every Nth send.  Default keeps the table small
--   on Render free Postgres (500 MB limit).
-- =============================================================================

CREATE TABLE IF NOT EXISTS otp_delivery_attempts (
  id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  -- Soft-link to auth_otps. Nullable because the parent row may already
  -- have been purged by the time we look at this audit row, and we still
  -- want forensic value out of orphan rows.
  otp_id              UUID,

  identifier          TEXT NOT NULL,                  -- snapshot for forensics
  purpose             TEXT NOT NULL DEFAULT 'login',

  attempt_index       INTEGER NOT NULL DEFAULT 0,     -- 0 = first provider tried
  provider_name       TEXT NOT NULL,                  -- 'fast2sms' | 'msg91' | ...
  channel             TEXT NOT NULL                   -- 'sms' | 'whatsapp' | 'email' | 'voice' | 'console'
                      CHECK (channel IN ('sms','whatsapp','email','voice','console')),
  status              TEXT NOT NULL                   -- 'sent' | 'failed' | 'skipped'
                      CHECK (status  IN ('sent','failed','skipped')),

  provider_message_id TEXT,                            -- vendor-side id (Twilio SID, MSG91 request_id, ...)
  http_status         INTEGER,                         -- upstream HTTP code
  latency_ms          INTEGER NOT NULL DEFAULT 0,
  reason              TEXT,                            -- failure / skip reason
  raw_response        TEXT,                            -- 240-char trimmed body

  created_at          TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_oda_identifier_created
  ON otp_delivery_attempts (identifier, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_oda_otp_id
  ON otp_delivery_attempts (otp_id) WHERE otp_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_oda_provider
  ON otp_delivery_attempts (provider_name, status, created_at DESC);

-- ---- Retention helper. Keep 7 days of audit data; tune via the WHERE.
CREATE OR REPLACE FUNCTION purge_old_delivery_attempts()
RETURNS INTEGER
LANGUAGE plpgsql
AS $$
DECLARE
  v_deleted INTEGER;
BEGIN
  DELETE FROM otp_delivery_attempts
   WHERE created_at < NOW() - INTERVAL '7 days';
  GET DIAGNOSTICS v_deleted = ROW_COUNT;
  RETURN v_deleted;
END;
$$;


-- =============================================================================
-- SECTION 3 — user_sessions  (rotating refresh tokens)
-- =============================================================================

CREATE TABLE IF NOT EXISTS user_sessions (
  id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id             UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,

  refresh_token_hash  TEXT NOT NULL UNIQUE,
  device_id           TEXT,
  platform            TEXT,
  ip_address          TEXT,
  user_agent          TEXT,

  issued_at           TIMESTAMPTZ DEFAULT NOW(),
  expires_at          TIMESTAMPTZ NOT NULL,
  revoked_at          TIMESTAMPTZ,

  last_used_at        TIMESTAMPTZ,

  created_at          TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_user_sessions_user    ON user_sessions (user_id);
CREATE INDEX IF NOT EXISTS idx_user_sessions_expires ON user_sessions (expires_at);


-- =============================================================================
-- SECTION 4 — KV stores (landing + app config)
-- =============================================================================

CREATE TABLE IF NOT EXISTS cms_landing_content (
  key        TEXT PRIMARY KEY,
  value      JSONB NOT NULL,
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS app_config (
  key        TEXT PRIMARY KEY,
  value      JSONB NOT NULL,
  updated_at TIMESTAMPTZ DEFAULT NOW()
);


-- =============================================================================
-- SECTION 5 — Onboarding drafts
-- =============================================================================

CREATE TABLE IF NOT EXISTS school_onboarding_drafts (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id     UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
  step_type   TEXT NOT NULL CHECK (step_type IN ('BASIC','BRANDING','ACADEMIC','REVIEW')),
  key         TEXT NOT NULL,
  value       TEXT NOT NULL,
  updated_at  TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE (user_id, step_type, key)
);

CREATE INDEX IF NOT EXISTS idx_ob_drafts_user ON school_onboarding_drafts (user_id, step_type);


-- =============================================================================
-- SECTION 6 — Classes + Subjects (onboarding ACADEMIC step + class-details)
-- =============================================================================

CREATE TABLE IF NOT EXISTS school_classes (
  id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  school_id  UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
  code       TEXT NOT NULL,                          -- e.g. C10
  name       TEXT NOT NULL,                          -- e.g. Grade 10
  sections   JSONB NOT NULL DEFAULT '[]',            -- ["A","B"]
  created_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE (school_id, code)
);

CREATE TABLE IF NOT EXISTS school_subjects (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  class_id          UUID NOT NULL REFERENCES school_classes(id) ON DELETE CASCADE,
  sub_name          TEXT NOT NULL,
  sub_code          TEXT NOT NULL,
  teacher_assigned  TEXT,
  created_at        TIMESTAMPTZ DEFAULT NOW()
);


-- =============================================================================
-- SECTION 7 — Announcements + WhatsApp logs
-- =============================================================================

CREATE TABLE IF NOT EXISTS announcements (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  school_id    UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
  event_id     TEXT UNIQUE NOT NULL,
  type         TEXT NOT NULL CHECK (type IN ('Holidays','PTM','Events','Special','Remainder')),
  title        TEXT NOT NULL,
  sub_title    TEXT,
  description  TEXT NOT NULL,
  event_image  TEXT,
  date         DATE NOT NULL,
  synced_to_wa BOOLEAN DEFAULT FALSE,
  created_by   UUID REFERENCES app_users(id),
  created_at   TIMESTAMPTZ DEFAULT NOW(),
  updated_at   TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ann_school_date ON announcements (school_id, date DESC);
CREATE INDEX IF NOT EXISTS idx_ann_unsynced   ON announcements (school_id, synced_to_wa)
  WHERE synced_to_wa = FALSE;
CREATE INDEX IF NOT EXISTS idx_ann_fts ON announcements
  USING gin(to_tsvector('english', coalesce(title,'') || ' ' || coalesce(description,'')));

DROP TRIGGER IF EXISTS trg_announcements_updated_at ON announcements;
CREATE TRIGGER trg_announcements_updated_at
  BEFORE UPDATE ON announcements
  FOR EACH ROW EXECUTE FUNCTION handle_updated_at();


CREATE TABLE IF NOT EXISTS whatsapp_logs (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  school_id         UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
  announcement_id   TEXT NOT NULL,
  job_id            TEXT NOT NULL,
  phone             TEXT NOT NULL,
  status            TEXT NOT NULL DEFAULT 'QUEUED'
                    CHECK (status IN ('QUEUED','SENT','FAILED','DELIVERED')),
  provider_message_id TEXT,
  error_message     TEXT,
  created_at        TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_wa_school_job ON whatsapp_logs (school_id, job_id);


-- =============================================================================
-- SECTION 8 — Admission enquiries
-- =============================================================================

CREATE TABLE IF NOT EXISTS admission_enquiries (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  school_id     UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,

  student_name  TEXT NOT NULL,
  parent_name   TEXT NOT NULL,
  parent_phone  TEXT,
  parent_email  TEXT,
  class_name    TEXT NOT NULL,
  date          DATE NOT NULL DEFAULT CURRENT_DATE,
  status        TEXT NOT NULL DEFAULT 'new'
                CHECK (status IN ('new','followup','converted','rejected')),
  profile_pic   TEXT,
  source        TEXT,                                 -- 'web','walk_in','phone','referral'
  notes         TEXT,
  assigned_to   UUID REFERENCES app_users(id),
  converted_at  TIMESTAMPTZ,

  created_at    TIMESTAMPTZ DEFAULT NOW(),
  updated_at    TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ae_school_status ON admission_enquiries (school_id, status);
CREATE INDEX IF NOT EXISTS idx_ae_school_date   ON admission_enquiries (school_id, date DESC);

DROP TRIGGER IF EXISTS trg_ae_updated_at ON admission_enquiries;
CREATE TRIGGER trg_ae_updated_at
  BEFORE UPDATE ON admission_enquiries
  FOR EACH ROW EXECUTE FUNCTION handle_updated_at();


-- =============================================================================
-- SECTION 9 — School profile (philosophy + media + storage)
-- =============================================================================

CREATE TABLE IF NOT EXISTS school_philosophy (
  school_id        UUID PRIMARY KEY REFERENCES schools(id) ON DELETE CASCADE,
  core_mission     TEXT,
  learning_model   TEXT,
  primary_language TEXT,
  public_profile   BOOLEAN DEFAULT TRUE,
  updated_at       TIMESTAMPTZ DEFAULT NOW()
);

DROP TRIGGER IF EXISTS trg_school_philosophy_updated_at ON school_philosophy;
CREATE TRIGGER trg_school_philosophy_updated_at
  BEFORE UPDATE ON school_philosophy
  FOR EACH ROW EXECUTE FUNCTION handle_updated_at();


CREATE TABLE IF NOT EXISTS school_media (
  id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  school_id  UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
  kind       TEXT NOT NULL CHECK (kind IN ('IMAGE','VIDEO')),
  url        TEXT NOT NULL,
  position   INTEGER DEFAULT 0,
  size_bytes BIGINT DEFAULT 0,
  uploaded_by UUID REFERENCES app_users(id),
  created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sm_school_kind ON school_media (school_id, kind);


CREATE TABLE IF NOT EXISTS storage_metrics (
  school_id      UUID PRIMARY KEY REFERENCES schools(id) ON DELETE CASCADE,
  total_storage  TEXT NOT NULL DEFAULT '10 GB',
  storage_used   TEXT NOT NULL DEFAULT '0 B',
  bytes_used     BIGINT NOT NULL DEFAULT 0,
  updated_at     TIMESTAMPTZ DEFAULT NOW()
);

DROP TRIGGER IF EXISTS trg_storage_metrics_updated_at ON storage_metrics;
CREATE TRIGGER trg_storage_metrics_updated_at
  BEFORE UPDATE ON storage_metrics
  FOR EACH ROW EXECUTE FUNCTION handle_updated_at();


-- =============================================================================
-- SECTION 10 — Academic calendar + Holidays + Faculty + Attendance
-- =============================================================================

CREATE TABLE IF NOT EXISTS academic_calendar (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  school_id         UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
  event_id          TEXT UNIQUE NOT NULL,
  date              DATE NOT NULL,
  day               TEXT NOT NULL,
  event_title       TEXT NOT NULL,
  event_description TEXT,
  standard          TEXT,
  is_holiday        BOOLEAN DEFAULT FALSE,
  created_at        TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ac_school_date ON academic_calendar (school_id, date);


CREATE TABLE IF NOT EXISTS holiday_list (
  id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  school_id  UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
  date       DATE NOT NULL,
  title      TEXT NOT NULL,
  type       TEXT NOT NULL CHECK (type IN ('Public','School')),
  frequency  TEXT NOT NULL CHECK (frequency IN ('weekly','monthly','yearly')),
  created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_hl_school_freq ON holiday_list (school_id, frequency);


CREATE TABLE IF NOT EXISTS faculty (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  school_id   UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
  external_id TEXT UNIQUE NOT NULL,                 -- FAC_001
  user_id     UUID REFERENCES app_users(id) ON DELETE SET NULL,
  name        TEXT NOT NULL,
  profile_pic TEXT,
  department  TEXT,
  is_active   BOOLEAN DEFAULT TRUE,
  created_at  TIMESTAMPTZ DEFAULT NOW()
);


-- A lightweight attendance table that backs the
-- "Get Daily Attendance" API (spec2).  Distinct from the operational
-- `daily_progress` table — this one is generic enough for both student
-- and faculty rows.
CREATE TABLE IF NOT EXISTS attendance_records (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  school_id   UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
  date        DATE NOT NULL,
  type        TEXT NOT NULL CHECK (type IN ('student','faculty')),
  person_id   TEXT NOT NULL,                        -- ST_501 / FAC_001
  grade       TEXT,                                 -- null for faculty
  status      TEXT NOT NULL CHECK (status IN ('present','absent','late','half_day','on_leave')),
  marked_by   UUID REFERENCES app_users(id),
  created_at  TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE (school_id, date, type, person_id)
);

CREATE INDEX IF NOT EXISTS idx_att_school_date ON attendance_records (school_id, date);


-- =============================================================================
-- SECTION 11 — Row Level Security (additive policies)
--
-- We keep these tables BEHIND service_role only for the Ktor backend.
-- The mobile app NEVER hits Postgres directly — it goes through Ktor.
-- So we turn RLS on but only allow service_role (the backend's connection)
-- to read/write.  No anon access.
-- =============================================================================

ALTER TABLE app_users               ENABLE ROW LEVEL SECURITY;
ALTER TABLE auth_otps               ENABLE ROW LEVEL SECURITY;
ALTER TABLE otp_delivery_attempts   ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_sessions           ENABLE ROW LEVEL SECURITY;
ALTER TABLE cms_landing_content     ENABLE ROW LEVEL SECURITY;
ALTER TABLE app_config              ENABLE ROW LEVEL SECURITY;
ALTER TABLE school_onboarding_drafts ENABLE ROW LEVEL SECURITY;
ALTER TABLE school_classes          ENABLE ROW LEVEL SECURITY;
ALTER TABLE school_subjects         ENABLE ROW LEVEL SECURITY;
ALTER TABLE announcements           ENABLE ROW LEVEL SECURITY;
ALTER TABLE whatsapp_logs           ENABLE ROW LEVEL SECURITY;
ALTER TABLE admission_enquiries     ENABLE ROW LEVEL SECURITY;
ALTER TABLE school_philosophy       ENABLE ROW LEVEL SECURITY;
ALTER TABLE school_media            ENABLE ROW LEVEL SECURITY;
ALTER TABLE storage_metrics         ENABLE ROW LEVEL SECURITY;
ALTER TABLE academic_calendar       ENABLE ROW LEVEL SECURITY;
ALTER TABLE holiday_list            ENABLE ROW LEVEL SECURITY;
ALTER TABLE faculty                 ENABLE ROW LEVEL SECURITY;
ALTER TABLE attendance_records      ENABLE ROW LEVEL SECURITY;

-- Landing + app_config are READ-public (they're literally CMS strings).
DROP POLICY IF EXISTS "anon reads landing" ON cms_landing_content;
CREATE POLICY "anon reads landing" ON cms_landing_content FOR SELECT USING (TRUE);

DROP POLICY IF EXISTS "anon reads app_config" ON app_config;
CREATE POLICY "anon reads app_config" ON app_config FOR SELECT USING (TRUE);


-- =============================================================================
-- SECTION 99 — OPTIONAL: pg_cron job to purge expired OTPs every minute
--
-- Run this AFTER you enable the pg_cron extension in Supabase:
--   Dashboard → Database → Extensions → enable "pg_cron"
--
-- This is the belt-and-braces cleanup so OTPs are physically gone even if
-- the backend forgets to call purge_expired_otps() in-line.
-- =============================================================================

-- (Uncomment after enabling pg_cron in Supabase Dashboard.)
-- SELECT cron.schedule(
--   'vp_purge_expired_otps',
--   '* * * * *',
--   $$ SELECT purge_expired_otps(); $$
-- );

-- Audit retention — keep otp_delivery_attempts at 7 days max.
-- SELECT cron.schedule(
--   'vp_purge_old_delivery_attempts',
--   '17 3 * * *',                              -- 03:17 UTC daily
--   $$ SELECT purge_old_delivery_attempts(); $$
-- );


-- =============================================================================
-- DONE — supplementary schema v1.1 loaded.
--
-- CHANGELOG
--   v1.1  (2026-05-24) — adds otp_delivery_attempts (audit table) +
--                       purge_old_delivery_attempts() helper for ops use.
--   v1.0  (initial)    — app_users, auth_otps, user_sessions, CMS, …
-- =============================================================================
