-- =====================================================================
-- Migration 060: Scholarship Workflow (SCHOLARSHIP_WORKFLOW_SPEC.md)
--
-- Extends the existing scholarships + scholarship_applications tables with
-- full workflow columns (types, eligibility, renewals, disbursement, documents,
-- fee integration) and creates the new scholarship_renewals table.
--
-- All new columns are nullable or have defaults so existing rows and seed
-- data continue to work without backfill.
--
-- Prerequisites: base schema (vidyasetu_schema.sql), academic_years table
-- (migration_051_parent_pulse.sql or later).
-- =====================================================================

BEGIN;

-- ── 1. Extend scholarships table ──────────────────────────────────────
ALTER TABLE scholarships ADD COLUMN IF NOT EXISTS school_id UUID;
ALTER TABLE scholarships ADD COLUMN IF NOT EXISTS scholarship_type VARCHAR(16) NOT NULL DEFAULT 'fixed';
ALTER TABLE scholarships ADD COLUMN IF NOT EXISTS waiver_percentage REAL;
ALTER TABLE scholarships ADD COLUMN IF NOT EXISTS numeric_amount DOUBLE PRECISION;
ALTER TABLE scholarships ADD COLUMN IF NOT EXISTS eligibility_criteria TEXT NOT NULL DEFAULT '';
ALTER TABLE scholarships ADD COLUMN IF NOT EXISTS start_date VARCHAR(12);
ALTER TABLE scholarships ADD COLUMN IF NOT EXISTS end_date VARCHAR(12);
ALTER TABLE scholarships ADD COLUMN IF NOT EXISTS is_renewable BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE scholarships ADD COLUMN IF NOT EXISTS renewal_period_months INTEGER;

CREATE INDEX IF NOT EXISTS idx_scholarships_school ON scholarships(school_id, is_active);

-- ── 2. Extend scholarship_applications table ──────────────────────────
ALTER TABLE scholarship_applications ADD COLUMN IF NOT EXISTS scholarship_id UUID;
ALTER TABLE scholarship_applications ADD COLUMN IF NOT EXISTS student_id UUID;
ALTER TABLE scholarship_applications ADD COLUMN IF NOT EXISTS academic_year_id UUID;
ALTER TABLE scholarship_applications ADD COLUMN IF NOT EXISTS document_urls TEXT;
ALTER TABLE scholarship_applications ADD COLUMN IF NOT EXISTS parent_application_text TEXT;
ALTER TABLE scholarship_applications ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP;
ALTER TABLE scholarship_applications ADD COLUMN IF NOT EXISTS reviewed_by UUID;
ALTER TABLE scholarship_applications ADD COLUMN IF NOT EXISTS remarks TEXT;
ALTER TABLE scholarship_applications ADD COLUMN IF NOT EXISTS disbursement_amount DOUBLE PRECISION;
ALTER TABLE scholarship_applications ADD COLUMN IF NOT EXISTS disbursement_date TIMESTAMP;
ALTER TABLE scholarship_applications ADD COLUMN IF NOT EXISTS disbursement_reference TEXT;

CREATE INDEX IF NOT EXISTS idx_scholarship_apps_status ON scholarship_applications(status, academic_year_id);
CREATE INDEX IF NOT EXISTS idx_scholarship_apps_parent ON scholarship_applications(parent_id);
CREATE INDEX IF NOT EXISTS idx_scholarship_apps_scholarship ON scholarship_applications(scholarship_id);

-- ── 3. Create scholarship_renewals table ──────────────────────────────
CREATE TABLE IF NOT EXISTS scholarship_renewals (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    original_application_id  UUID NOT NULL,
    student_id               UUID NOT NULL,
    scholarship_id           UUID NOT NULL,
    school_id                UUID NOT NULL,
    academic_year_id         UUID NOT NULL,
    status                   VARCHAR(16) NOT NULL DEFAULT 'pending',
    document_urls            TEXT,
    applied_at               TIMESTAMP NOT NULL DEFAULT now(),
    reviewed_at              TIMESTAMP,
    reviewed_by              UUID,
    remarks                  TEXT
);

CREATE INDEX IF NOT EXISTS idx_scholarship_renewals_original ON scholarship_renewals(original_application_id);
CREATE INDEX IF NOT EXISTS idx_scholarship_renewals_student ON scholarship_renewals(student_id, academic_year_id);
CREATE INDEX IF NOT EXISTS idx_scholarship_renewals_school ON scholarship_renewals(school_id, status);

-- ── 4. Extend fee_records table with scholarship waiver fields ────────
ALTER TABLE fee_records ADD COLUMN IF NOT EXISTS scholarship_id UUID;
ALTER TABLE fee_records ADD COLUMN IF NOT EXISTS scholarship_type VARCHAR(16);
ALTER TABLE fee_records ADD COLUMN IF NOT EXISTS scholarship_amount DOUBLE PRECISION;
ALTER TABLE fee_records ADD COLUMN IF NOT EXISTS original_amount DOUBLE PRECISION;

COMMIT;

-- ── ROLLBACK ──────────────────────────────────────────────────────────
-- BEGIN;
-- DROP TABLE IF EXISTS scholarship_renewals;
-- ALTER TABLE fee_records DROP COLUMN IF EXISTS original_amount;
-- ALTER TABLE fee_records DROP COLUMN IF EXISTS scholarship_amount;
-- ALTER TABLE fee_records DROP COLUMN IF EXISTS scholarship_type;
-- ALTER TABLE fee_records DROP COLUMN IF EXISTS scholarship_id;
-- ALTER TABLE scholarship_applications DROP COLUMN IF EXISTS disbursement_reference;
-- ALTER TABLE scholarship_applications DROP COLUMN IF EXISTS disbursement_date;
-- ALTER TABLE scholarship_applications DROP COLUMN IF EXISTS disbursement_amount;
-- ALTER TABLE scholarship_applications DROP COLUMN IF EXISTS remarks;
-- ALTER TABLE scholarship_applications DROP COLUMN IF EXISTS reviewed_by;
-- ALTER TABLE scholarship_applications DROP COLUMN IF EXISTS reviewed_at;
-- ALTER TABLE scholarship_applications DROP COLUMN IF EXISTS parent_application_text;
-- ALTER TABLE scholarship_applications DROP COLUMN IF EXISTS document_urls;
-- ALTER TABLE scholarship_applications DROP COLUMN IF EXISTS academic_year_id;
-- ALTER TABLE scholarship_applications DROP COLUMN IF EXISTS student_id;
-- ALTER TABLE scholarship_applications DROP COLUMN IF EXISTS scholarship_id;
-- ALTER TABLE scholarships DROP COLUMN IF EXISTS renewal_period_months;
-- ALTER TABLE scholarships DROP COLUMN IF EXISTS is_renewable;
-- ALTER TABLE scholarships DROP COLUMN IF EXISTS end_date;
-- ALTER TABLE scholarships DROP COLUMN IF EXISTS start_date;
-- ALTER TABLE scholarships DROP COLUMN IF EXISTS eligibility_criteria;
-- ALTER TABLE scholarships DROP COLUMN IF EXISTS numeric_amount;
-- ALTER TABLE scholarships DROP COLUMN IF EXISTS waiver_percentage;
-- ALTER TABLE scholarships DROP COLUMN IF EXISTS scholarship_type;
-- ALTER TABLE scholarships DROP COLUMN IF EXISTS school_id;
-- COMMIT;
