-- Migration 101: School Branding Kit
-- Creates school_branding table for per-school branding customization
-- Spec: SCHOOL_BRANDING_KIT_SPEC.md

BEGIN;

CREATE TABLE IF NOT EXISTS school_branding (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL UNIQUE,
    logo_url        TEXT,
    logo_dark_url   TEXT,
    favicon_url     TEXT,
    app_icon_url    TEXT,
    splash_screen_url TEXT,
    primary_color   VARCHAR(8) DEFAULT '#2563EB',
    secondary_color VARCHAR(8) DEFAULT '#1E40AF',
    accent_color    VARCHAR(8) DEFAULT '#3B82F6',
    custom_subdomain TEXT,
    login_background_url TEXT,
    is_customized   BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_school_branding_subdomain
    ON school_branding (custom_subdomain)
    WHERE custom_subdomain IS NOT NULL;

COMMIT;
