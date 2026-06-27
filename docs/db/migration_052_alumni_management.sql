-- =============================================================================
-- Migration 052 — Alumni Management
--                (alumni directory, mentorship, donations, career tracking)
--
-- WHY THIS EXISTS
--   ALUMNI_MANAGEMENT_SPEC.md — K-12 alumni engagement platform: maintain
--   alumni records, track career progression, facilitate mentorship for
--   Class 11-12 students, organize alumni events, enable donation campaigns
--   with 80G-compliant receipts, and keep alumni engaged through targeted
--   newsletters.
--
--   This creates 7 tables + adds columns to `schools`:
--     alumni                    — core alumni records (graduated students)
--     alumni_donation_campaigns — donation campaigns with goals + progress
--     alumni_donations          — individual donation records + 80G receipts
--     alumni_mentorship_requests — student-initiated mentorship requests
--     alumni_mentorships        — active mentorship relationships
--     alumni_career_history     — career progression over time
--     alumni_mentorship_settings — per-school mentorship config (admin-configurable)
--
--   Plus ALTER TABLE schools:
--     school_code               — for alumni self-registration lookup
--     pan_number                — 80G receipt generation
--     g80_registration_number   — 80G receipt generation
--     g80_validity_date         — 80G receipt generation
--     g80_certificate_url       — 80G certificate document
--
--   The Exposed mappings (AlumniTable, AlumniDonationCampaignsTable, etc. in
--   server/.../db/Tables.kt) reference these tables. AUTO_CREATE_TABLES is
--   OFF in production and validateSchema() gates boot on table presence, so
--   this file MUST be applied in Supabase BEFORE the matching backend deploy.
--
-- HOW TO RUN
--   Supabase -> SQL Editor -> paste this whole file -> Run.
--   Safe to re-run: every CREATE TABLE uses IF NOT EXISTS.
--
-- RUN ORDER (appended to the existing chain):
--   ...
--   51. docs/db/migration_051_parent_pulse.sql
--   52. docs/db/migration_052_alumni_management.sql       <-- this file
--
-- Column names/types are kept in lock-step with
-- server/.../db/Tables.kt (the Exposed mappings land in the same commit).
-- =============================================================================

BEGIN;

-- ── schools additions ────────────────────────────────────────────────────────
ALTER TABLE schools ADD COLUMN IF NOT EXISTS school_code VARCHAR(20);
ALTER TABLE schools ADD COLUMN IF NOT EXISTS pan_number VARCHAR(20);
ALTER TABLE schools ADD COLUMN IF NOT EXISTS g80_registration_number VARCHAR(50);
ALTER TABLE schools ADD COLUMN IF NOT EXISTS g80_validity_date DATE;
ALTER TABLE schools ADD COLUMN IF NOT EXISTS g80_certificate_url TEXT;

CREATE UNIQUE INDEX IF NOT EXISTS idx_schools_code
    ON schools (school_code) WHERE school_code IS NOT NULL;

-- Backfill school_code for existing schools
UPDATE schools
SET school_code = UPPER(LEFT(slug, 4)) || '-' || SUBSTRING(id::text, 1, 4)
WHERE school_code IS NULL;

-- ── alumni ────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS alumni (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id            UUID NOT NULL REFERENCES schools(id),
    student_id           UUID REFERENCES students(id),       -- nullable, if linked to former student
    user_id              UUID REFERENCES app_users(id),      -- nullable, for alumni login + notifications
    name                 TEXT NOT NULL,
    graduation_year      INTEGER NOT NULL,
    last_class           TEXT,                               -- "Grade 12", "Grade 10"
    current_profession   TEXT,
    company              TEXT,
    city                 TEXT,
    email                TEXT,
    phone                VARCHAR(32),
    linkedin_url         TEXT,
    photo_url            TEXT,
    skills               TEXT,                               -- comma-separated
    achievements         TEXT,                               -- free text
    is_mentor            BOOLEAN NOT NULL DEFAULT false,
    mentor_expertise     TEXT,                               -- "Engineering", "Medicine", "Finance"
    is_featured          BOOLEAN NOT NULL DEFAULT false,     -- admin spotlight
    -- Verification (SIS-matched self-registration)
    verification_status  VARCHAR(16) NOT NULL DEFAULT 'approved', -- approved | pending | declined
    verified_at          TIMESTAMP,
    verified_by          UUID REFERENCES app_users(id),      -- admin who approved
    -- Privacy controls (DPDP Act 2023)
    show_phone           BOOLEAN NOT NULL DEFAULT false,
    show_email           BOOLEAN NOT NULL DEFAULT false,
    show_linkedin        BOOLEAN NOT NULL DEFAULT true,
    visibility_level     VARCHAR(16) NOT NULL DEFAULT 'batch', -- public | batch | private
    -- Engagement
    last_active_at       TIMESTAMP,
    is_active            BOOLEAN NOT NULL DEFAULT true,
    created_at           TIMESTAMP NOT NULL DEFAULT now(),
    updated_at           TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_alumni_school_year
    ON alumni (school_id, graduation_year);
CREATE INDEX IF NOT EXISTS idx_alumni_user_id
    ON alumni (user_id) WHERE user_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_alumni_verification
    ON alumni (school_id, verification_status) WHERE verification_status = 'pending';
CREATE INDEX IF NOT EXISTS idx_alumni_featured
    ON alumni (school_id, is_featured) WHERE is_featured = true;

-- ── alumni_donation_campaigns ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS alumni_donation_campaigns (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id            UUID NOT NULL REFERENCES schools(id),
    title                TEXT NOT NULL,                      -- "Computer Lab Upgrade"
    description          TEXT,
    cause                TEXT,                               -- "Scholarship Fund", "Library", "Sports Complex"
    target_amount        DOUBLE PRECISION NOT NULL,
    amount_raised        DOUBLE PRECISION NOT NULL DEFAULT 0, -- derived/cache, updated on each donation
    target_batch_year    INTEGER,                            -- nullable: target specific batch, null = all
    start_date           DATE NOT NULL,
    end_date             DATE,                               -- nullable: ongoing campaign
    status               VARCHAR(16) NOT NULL DEFAULT 'active', -- active | closed | paused
    is_active            BOOLEAN NOT NULL DEFAULT true,
    created_at           TIMESTAMP NOT NULL DEFAULT now(),
    updated_at           TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_alumni_campaigns_school
    ON alumni_donation_campaigns (school_id, status);

-- ── alumni_donations ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS alumni_donations (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id            UUID NOT NULL REFERENCES schools(id),
    alumni_id            UUID NOT NULL REFERENCES alumni(id) ON DELETE CASCADE,
    campaign_id          UUID REFERENCES alumni_donation_campaigns(id), -- nullable: general donation
    amount               DOUBLE PRECISION NOT NULL,
    purpose              TEXT,                               -- "Scholarship Fund", "Library", "General"
    donation_date        DATE NOT NULL,
    payment_mode         VARCHAR(16),                        -- upi | bank_transfer | cheque | cash | card
    reference_number     TEXT,
    -- 80G receipt fields
    receipt_number       TEXT UNIQUE,                        -- auto-generated: SCH-80G-2026-00001
    receipt_issued       BOOLEAN NOT NULL DEFAULT false,
    is_80g_eligible      BOOLEAN NOT NULL DEFAULT false,     -- school must have valid 80G certificate
    created_at           TIMESTAMP NOT NULL DEFAULT now(),
    updated_at           TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_alumni_donations_school
    ON alumni_donations (school_id, donation_date DESC);
CREATE INDEX IF NOT EXISTS idx_alumni_donations_alumni
    ON alumni_donations (alumni_id);
CREATE INDEX IF NOT EXISTS idx_alumni_donations_campaign
    ON alumni_donations (campaign_id) WHERE campaign_id IS NOT NULL;

-- ── alumni_mentorship_requests ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS alumni_mentorship_requests (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id            UUID NOT NULL REFERENCES schools(id),
    alumni_id            UUID NOT NULL REFERENCES alumni(id) ON DELETE CASCADE,
    student_id           UUID NOT NULL REFERENCES students(id),
    requested_by         UUID NOT NULL REFERENCES app_users(id), -- student's parent or teacher
    expertise_area       TEXT,                               -- "Engineering", "Medicine"
    message              TEXT,                               -- student's request message
    status               VARCHAR(16) NOT NULL DEFAULT 'pending', -- pending | accepted | declined | expired
    responded_at         TIMESTAMP,
    created_at           TIMESTAMP NOT NULL DEFAULT now(),
    updated_at           TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_alumni_mentor_req_school
    ON alumni_mentorship_requests (school_id, status);
CREATE INDEX IF NOT EXISTS idx_alumni_mentor_req_alumni
    ON alumni_mentorship_requests (alumni_id, status);

-- ── alumni_mentorships ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS alumni_mentorships (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id            UUID NOT NULL REFERENCES schools(id),
    alumni_id            UUID NOT NULL REFERENCES alumni(id) ON DELETE CASCADE,
    student_id           UUID NOT NULL REFERENCES students(id),
    request_id           UUID REFERENCES alumni_mentorship_requests(id), -- nullable: admin-created
    status               VARCHAR(16) NOT NULL DEFAULT 'active', -- active | ended
    start_date           DATE NOT NULL,
    end_date             DATE,                               -- nullable: ongoing
    notes                TEXT,
    session_count        INTEGER NOT NULL DEFAULT 0,         -- derived: count of sessions logged
    created_at           TIMESTAMP NOT NULL DEFAULT now(),
    updated_at           TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_alumni_mentorships_school
    ON alumni_mentorships (school_id, status);
CREATE INDEX IF NOT EXISTS idx_alumni_mentorships_alumni
    ON alumni_mentorships (alumni_id);

-- ── alumni_career_history ─────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS alumni_career_history (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    alumni_id            UUID NOT NULL REFERENCES alumni(id) ON DELETE CASCADE,
    job_title            TEXT NOT NULL,
    company              TEXT NOT NULL,
    industry             TEXT,                               -- "IT", "Healthcare", "Finance", "Civil Services"
    start_date           DATE,
    end_date             DATE,                               -- nullable: current job
    is_current           BOOLEAN NOT NULL DEFAULT false,
    created_at           TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_alumni_career_alumni
    ON alumni_career_history (alumni_id, is_current DESC);

-- ── alumni_mentorship_settings ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS alumni_mentorship_settings (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id                 UUID NOT NULL REFERENCES schools(id),
    enabled                   BOOLEAN NOT NULL DEFAULT true,
    eligible_class_ids        TEXT,                           -- JSON array of UUIDs
    max_mentees_per_alumni    INTEGER NOT NULL DEFAULT 5,
    request_approval_required BOOLEAN NOT NULL DEFAULT true,
    created_at                TIMESTAMP NOT NULL DEFAULT now(),
    updated_at                TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (school_id)
);

-- Verification:
-- SELECT table_name FROM information_schema.tables
--   WHERE table_name IN ('alumni', 'alumni_donation_campaigns', 'alumni_donations',
--       'alumni_mentorship_requests', 'alumni_mentorships', 'alumni_career_history',
--       'alumni_mentorship_settings')
--   ORDER BY table_name;
-- Expected: 7 rows

COMMIT;

-- ───────────────────────────────────────────────────────────────────────────
-- B3: NotificationPreferencesTable defaults for alumni users
-- Notify.kt silently skips push delivery if no preference rows exist.
-- Insert default-enabled rows for any alumni app_users that don't have them.
-- Categories match those used by Notify.toUsers() — announcement is the
-- primary channel for alumni newsletters/batch targeting.
-- ───────────────────────────────────────────────────────────────────────────
BEGIN;

INSERT INTO notification_preferences (id, user_id, category, enabled, created_at, updated_at)
SELECT
    gen_random_uuid(),
    au.id,
    'announcement',
    true,
    now(),
    now()
FROM app_users au
LEFT JOIN notification_preferences np
    ON np.user_id = au.id AND np.category = 'announcement'
WHERE au.role = 'alumni' AND np.user_id IS NULL;

COMMIT;
