-- =====================================================================
-- migration_102_id_card.sql
-- ID Card Generation (ID_CARD_GENERATION_SPEC.md)
-- Creates: id_card_templates, id_cards
-- =====================================================================

CREATE TABLE IF NOT EXISTS id_card_templates (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    name            TEXT NOT NULL,
    role_type       VARCHAR(16) NOT NULL,          -- student | teacher | staff
    front_config    TEXT NOT NULL,                 -- JSON: layout, fields, colors
    back_config     TEXT NOT NULL,                 -- JSON: layout, fields, colors
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS id_cards (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id        UUID NOT NULL,
    person_id        UUID NOT NULL,
    person_type      VARCHAR(16) NOT NULL,          -- student | teacher | staff
    person_name      TEXT NOT NULL,
    template_id      UUID NOT NULL REFERENCES id_card_templates(id),
    pdf_url          TEXT,
    digital_card_url TEXT,
    qr_code_data     TEXT NOT NULL,
    valid_till       DATE,
    status           VARCHAR(16) NOT NULL DEFAULT 'ready',  -- requested | generated | ready | failed
    created_at       TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_id_card_templates_school ON id_card_templates(school_id, role_type, is_active);
CREATE INDEX IF NOT EXISTS idx_id_cards_person ON id_cards(person_id, person_type);
CREATE INDEX IF NOT EXISTS idx_id_cards_school ON id_cards(school_id, created_at DESC);
