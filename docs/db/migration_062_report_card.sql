-- FILE: docs/db/migration_062_report_card.sql
-- AI Report Card 2.0 — Agentic Redesign (AI_REPORT_CARD_2.0_AGENTIC_REDESIGN.md)
--
-- 5 new tables:
--   1. report_card_drafts           — per-student AI-generated draft (Tier 2→3)
--   2. report_focus_effectiveness   — Tier 4 flywheel priors
--   3. holistic_assessments         — NEP 360-degree assessment (graceful when empty)
--   4. co_scholastic_records        — NEP arts/sports/life-skills (graceful when empty)
--   5. report_card_templates        — board-specific report card layout templates
--
-- Must run in Supabase SQL Editor before deploy. AUTO_CREATE_TABLES is OFF in prod.
-- =====================================================================

-- 1. report_card_drafts
--    One row per (school, student, class, term, academic_year).
--    Status state machine: draft → flagged_for_review → approved → published → archived
CREATE TABLE IF NOT EXISTS report_card_drafts (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id           UUID NOT NULL,
    student_id          UUID NOT NULL,              -- FK students.id
    class_id            UUID,                       -- FK school_classes.id (nullable for legacy)
    class_name          TEXT NOT NULL,              -- denormalized for display
    section             VARCHAR(8) DEFAULT 'A',
    term                VARCHAR(16) NOT NULL,       -- e.g. "Term 1", "Term 2"
    academic_year_id    UUID,                       -- FK academic_years.id
    fact_bundle         TEXT NOT NULL,              -- JSON: Tier-0 deterministic fact bundle
    fact_hash           VARCHAR(64) NOT NULL,       -- SHA-256 of fact_bundle (cache key component)
    ai_draft            TEXT,                       -- JSON: Tier-2 structured Report Draft
    class_context       TEXT,                       -- Tier-1 class-context paragraph (cohort-dedup)
    ai_provider_used    VARCHAR(32),
    ai_model_used       VARCHAR(96),
    tokens_used         INTEGER DEFAULT 0,
    template_version    INTEGER DEFAULT 1,
    language            VARCHAR(8) DEFAULT 'hi',    -- narrative language
    status              VARCHAR(24) DEFAULT 'draft',-- draft|flagged_for_review|approved|published|archived
    grounding_flags     TEXT,                       -- JSON: array of fields that failed grounding
    edited_by           UUID,                       -- FK app_users.id (teacher who edited)
    edited_at           TIMESTAMP,                  -- last teacher edit timestamp
    approved_by         UUID,                       -- FK app_users.id (teacher who approved)
    approved_at         TIMESTAMP,
    published_by        UUID,                       -- FK app_users.id (admin who published)
    published_at        TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_rcd_school_class_term ON report_card_drafts(school_id, class_id, term, academic_year_id);
CREATE INDEX IF NOT EXISTS idx_rcd_status ON report_card_drafts(school_id, status);
CREATE INDEX IF NOT EXISTS idx_rcd_student ON report_card_drafts(student_id, academic_year_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_rcd_student_term_year ON report_card_drafts(school_id, student_id, term, academic_year_id);

-- 2. report_focus_effectiveness
--    Tier 4 flywheel: tracks how effective focus-area recommendations were.
--    One row per (school, focus_area, term, academic_year).
CREATE TABLE IF NOT EXISTS report_focus_effectiveness (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id           UUID NOT NULL,
    focus_area          VARCHAR(64) NOT NULL,       -- e.g. "attendance", "homework_completion", "classroom_participation"
    term                VARCHAR(16) NOT NULL,
    academic_year_id    UUID,
    students_targeted   INTEGER DEFAULT 0,
    students_improved   INTEGER DEFAULT 0,
    avg_delta           DOUBLE PRECISION DEFAULT 0, -- average improvement delta
    effectiveness_score DOUBLE PRECISION DEFAULT 0, -- 0..1 (students_improved / students_targeted)
    sample_size         INTEGER DEFAULT 0,
    confidence          VARCHAR(16) DEFAULT 'low',  -- low|medium|high
    metadata            TEXT,                       -- JSON: extra context
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_rfe_school_focus_term ON report_focus_effectiveness(school_id, focus_area, term, academic_year_id);

-- 3. holistic_assessments
--    NEP 2020 360-degree assessment: self, peer, teacher, parent ratings.
--    Graceful when empty — Tier-0 treats absence as "no holistic data".
CREATE TABLE IF NOT EXISTS holistic_assessments (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id           UUID NOT NULL,
    student_id          UUID NOT NULL,              -- FK students.id
    class_id            UUID,
    term                VARCHAR(16) NOT NULL,
    academic_year_id    UUID,
    assessor_type       VARCHAR(16) NOT NULL,       -- self|peer|teacher|parent
    assessor_id         UUID,                       -- FK app_users.id (null for self/peer anonymous)
    -- 360-degree dimensions (each 1..5 scale)
    critical_thinking   INTEGER,
    creativity          INTEGER,
    communication      INTEGER,
    collaboration       INTEGER,
    self_awareness      INTEGER,
    social_emotional    INTEGER,
    -- Optional narrative
    remarks             TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_ha_student_term ON holistic_assessments(school_id, student_id, term, academic_year_id);

-- 4. co_scholastic_records
--    NEP co-scholastic tracking: arts, sports, life skills, values.
--    Graceful when empty — Tier-0 treats absence as "no co-scholastic data".
CREATE TABLE IF NOT EXISTS co_scholastic_records (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id           UUID NOT NULL,
    student_id          UUID NOT NULL,              -- FK students.id
    class_id            UUID,
    term                VARCHAR(16) NOT NULL,
    academic_year_id    UUID,
    category            VARCHAR(32) NOT NULL,       -- arts|sports|life_skills|values|work_education
    activity_name       TEXT NOT NULL,              -- e.g. "Drawing", "Football", "Teamwork"
    grade               VARCHAR(4),                 -- A|B|C|D or 5-point scale
    descriptor          TEXT,                       -- "Excellent", "Good", etc.
    teacher_remarks     TEXT,
    recorded_by         UUID,                       -- FK app_users.id
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_csr_student_term ON co_scholastic_records(school_id, student_id, term, academic_year_id);

-- 5. report_card_templates
--    Board-specific report card layout templates (CBSE, ICSE, IB, State).
--    Used by Tier-0 to determine grading scale and by Tier-3 for layout.
CREATE TABLE IF NOT EXISTS report_card_templates (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id           UUID,                       -- null = global template, non-null = school-specific
    board               VARCHAR(32) NOT NULL,       -- CBSE|ICSE|IB|STATE|NEP_HPC
    grade_range         VARCHAR(16) NOT NULL,       -- e.g. "1-5", "6-8", "9-10", "11-12"
    template_name       TEXT NOT NULL,
    version             INTEGER DEFAULT 1,
    -- Grading scale as JSON: [{"min_pct": 90, "grade": "A1", "descriptor": "Outstanding"}, ...]
    grading_scale       TEXT NOT NULL,
    -- Layout sections as JSON: [{"section": "academic", "required": true}, ...]
    layout              TEXT,
    -- Whether to include holistic, co-scholastic, attendance sections
    includes_holistic   BOOLEAN DEFAULT true,
    includes_co_scholastic BOOLEAN DEFAULT true,
    includes_attendance BOOLEAN DEFAULT true,
    includes_ai_narrative BOOLEAN DEFAULT true,
    is_active           BOOLEAN DEFAULT true,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_rct_board_grade ON report_card_templates(board, grade_range, is_active);

-- Seed default kill-switch rows for report card modules
INSERT INTO pews_feature_flags (module_name, is_killed, updated_at)
VALUES
    ('reportcard', false, now()),
    ('reportcard_rollup', false, now()),
    ('reportcard_triage', false, now()),
    ('reportcard_narrator', false, now()),
    ('reportcard_assemble', false, now()),
    ('reportcard_learn', false, now())
ON CONFLICT (module_name) DO NOTHING;

-- Seed a default CBSE 9-point grading scale template
INSERT INTO report_card_templates (school_id, board, grade_range, template_name, grading_scale, layout, is_active)
VALUES (
    NULL,
    'CBSE',
    '6-10',
    'CBSE 9-Point Grading (Secondary)',
    '[{"min_pct":90,"grade":"A1","descriptor":"Outstanding"},{"min_pct":80,"grade":"A2","descriptor":"Excellent"},{"min_pct":71,"grade":"B1","descriptor":"Very Good"},{"min_pct":62,"grade":"B2","descriptor":"Good"},{"min_pct":53,"grade":"C1","descriptor":"Fair"},{"min_pct":45,"grade":"C2","descriptor":"Satisfactory"},{"min_pct":33,"grade":"D","descriptor":"Pass"},{"min_pct":0,"grade":"E","descriptor":"Needs Improvement"}]',
    '[{"section":"academic","required":true},{"section":"holistic","required":true},{"section":"co_scholastic","required":true},{"section":"attendance","required":true},{"section":"ai_narrative","required":true}]',
    true
) ON CONFLICT DO NOTHING;

-- Seed a default ICSE percentage-based template
INSERT INTO report_card_templates (school_id, board, grade_range, template_name, grading_scale, layout, is_active)
VALUES (
    NULL,
    'ICSE',
    '6-10',
    'ICSE Percentage-Based (Secondary)',
    '[{"min_pct":90,"grade":"A+","descriptor":"Excellent"},{"min_pct":75,"grade":"A","descriptor":"Very Good"},{"min_pct":60,"grade":"B","descriptor":"Good"},{"min_pct":45,"grade":"C","descriptor":"Satisfactory"},{"min_pct":33,"grade":"D","descriptor":"Pass"},{"min_pct":0,"grade":"F","descriptor":"Fail"}]',
    '[{"section":"academic","required":true},{"section":"attendance","required":true},{"section":"ai_narrative","required":true}]',
    true
) ON CONFLICT DO NOTHING;

-- Seed a default NEP HPC template
INSERT INTO report_card_templates (school_id, board, grade_range, template_name, grading_scale, layout, is_active)
VALUES (
    NULL,
    'NEP_HPC',
    '1-12',
    'NEP Holistic Progress Card',
    '[{"min_pct":90,"grade":"A","descriptor":"Advanced"},{"min_pct":75,"grade":"B","descriptor":"Proficient"},{"min_pct":55,"grade":"C","descriptor":"Developing"},{"min_pct":35,"grade":"D","descriptor":"Beginner"},{"min_pct":0,"grade":"E","descriptor":"Needs Support"}]',
    '[{"section":"academic","required":true},{"section":"holistic","required":true},{"section":"co_scholastic","required":true},{"section":"attendance","required":true},{"section":"ai_narrative","required":true}]',
    true
) ON CONFLICT DO NOTHING;
