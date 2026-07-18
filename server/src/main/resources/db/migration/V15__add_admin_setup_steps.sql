-- Persistent, per-admin progress for the five-step Home setup checklist.
CREATE TABLE IF NOT EXISTS admin_setup_steps (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL,
    school_id    UUID NOT NULL,
    step_key     VARCHAR(32) NOT NULL,
    status       VARCHAR(16) NOT NULL DEFAULT 'IN_PROGRESS',
    started_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_admin_setup_status CHECK (status IN ('IN_PROGRESS', 'COMPLETED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_admin_setup_user_step
    ON admin_setup_steps (user_id, step_key);
CREATE INDEX IF NOT EXISTS idx_admin_setup_school
    ON admin_setup_steps (school_id);
