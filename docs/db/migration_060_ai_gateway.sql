-- =============================================================================
-- Migration 060 — AI Gateway
--                (shared multi-provider OpenAI-compatible LLM gateway)
--
-- WHY THIS EXISTS
--   AI_FEATURES_PLAN.md §4 / AI_INFRASTRUCTURE_SPEC.md §6 — the single choke
--   point every AI feature calls. Provider strategy: five free-tier
--   OpenAI-compatible providers (Cerebras / Groq / SambaNova / Mistral /
--   OpenRouter). Keys are seeded from AI_<PROVIDER>_API_KEY env vars by the
--   KeyVault on boot and stored AES-256-GCM encrypted in ai_provider_config.
--
--   Creates six tables:
--     ai_provider_config   — provider/model registry + encrypted key + base_url
--     ai_prompt_templates  — versioned prompts + pii_allowed_providers allow-list
--     ai_usage_log         — per-school observability + quota (append-only)
--     ai_response_cache    — L1 exact-match cache (SHA-256), school-scoped, TTL
--     ai_jobs              — batch queue for class/school-wide runs
--     ai_provider_health   — per-(provider,model) circuit-breaker + rolling stats
--
--   The Exposed mappings (Ai*Table in server/.../db/Tables.kt) reference these.
--   AUTO_CREATE_TABLES is OFF in production and validateSchema() gates boot on
--   table presence, so this file MUST be applied in Supabase BEFORE the
--   matching backend deploy.
--
-- HOW TO RUN
--   Supabase -> SQL Editor -> paste this whole file -> Run.
--   Safe to re-run: every CREATE TABLE uses IF NOT EXISTS.
--
-- RUN ORDER (appended to the existing chain):
--   ...
--   53. docs/db/migration_053_transport_tracking.sql
--   60. docs/db/migration_060_ai_gateway.sql            <-- this file
--   61. docs/db/migration_061_pews.sql
--
-- Column names/types are kept in lock-step with server/.../db/Tables.kt
-- (the Exposed mappings land in the same commit).
-- =============================================================================

BEGIN;

-- ── ai_provider_config ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ai_provider_config (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider                 VARCHAR(32)  NOT NULL,   -- cerebras|groq|sambanova|mistral|openrouter
    model                    VARCHAR(96)  NOT NULL,   -- pinned model id
    api_key_encrypted        TEXT         NOT NULL DEFAULT '', -- AES-256-GCM (empty = unconfigured)
    base_url                 TEXT         NOT NULL,   -- OpenAI-compatible /v1 base
    is_active                BOOLEAN      NOT NULL DEFAULT TRUE,
    priority                 INTEGER      NOT NULL DEFAULT 0,    -- failover order within a lane
    tier                     VARCHAR(16)  NOT NULL DEFAULT 'fast', -- fast|reason|batch|stt
    no_training              BOOLEAN      NOT NULL DEFAULT TRUE, -- false = training opt-in (PII-restricted)
    max_tokens_per_request   INTEGER      NOT NULL DEFAULT 2048,
    temperature              DOUBLE PRECISION NOT NULL DEFAULT 0.4,
    created_at               TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at               TIMESTAMP    NOT NULL DEFAULT now(),
    UNIQUE (provider, model)
);

-- ── ai_prompt_templates ──────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ai_prompt_templates (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    feature                  VARCHAR(48)  NOT NULL,
    name                     VARCHAR(128) NOT NULL,
    version                  INTEGER      NOT NULL DEFAULT 1,
    system_prompt            TEXT         NOT NULL,
    user_prompt_template     TEXT         NOT NULL,
    variables                TEXT         NOT NULL DEFAULT '[]',
    pii_allowed_providers    TEXT         NOT NULL DEFAULT '', -- CSV; empty = all no-training
    guardrail_config         TEXT         NOT NULL DEFAULT '{}',
    traffic_weight           INTEGER      NOT NULL DEFAULT 100,
    is_active                BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at               TIMESTAMP    NOT NULL DEFAULT now(),
    UNIQUE (feature, name, version)
);

-- ── ai_usage_log ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ai_usage_log (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id                UUID,                       -- null for platform/system calls
    user_id                  UUID,
    feature                  VARCHAR(48)  NOT NULL,
    provider                 VARCHAR(32)  NOT NULL,
    model                    VARCHAR(96)  NOT NULL,
    provider_used            VARCHAR(32),                -- actual (may differ on failover)
    model_used               VARCHAR(96),
    input_tokens             INTEGER      NOT NULL DEFAULT 0,
    output_tokens            INTEGER      NOT NULL DEFAULT 0,
    cost_usd                 DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    latency_ms               INTEGER      NOT NULL DEFAULT 0,
    status                   VARCHAR(16)  NOT NULL,      -- success|failed|cached|guardrail_blocked
    routing_decision         VARCHAR(32)  NOT NULL DEFAULT 'direct',
    error_message            TEXT,
    created_at               TIMESTAMP    NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_ai_usage_school_date ON ai_usage_log (school_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ai_usage_feature     ON ai_usage_log (school_id, feature, created_at DESC);

-- ── ai_response_cache ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ai_response_cache (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cache_key                TEXT         NOT NULL,      -- SHA-256(prompt+model+temp)
    school_id                UUID,
    feature                  VARCHAR(48)  NOT NULL,
    response                 TEXT         NOT NULL,
    input_tokens             INTEGER      NOT NULL DEFAULT 0,
    output_tokens            INTEGER      NOT NULL DEFAULT 0,
    provider_used            VARCHAR(32),
    model_used               VARCHAR(96),
    expires_at               TIMESTAMP    NOT NULL,
    created_at               TIMESTAMP    NOT NULL DEFAULT now(),
    UNIQUE (cache_key)
);
CREATE INDEX IF NOT EXISTS idx_ai_cache_expiry ON ai_response_cache (expires_at);

-- ── ai_jobs ──────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ai_jobs (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id                UUID         NOT NULL,
    feature                  VARCHAR(48)  NOT NULL,
    status                   VARCHAR(16)  NOT NULL DEFAULT 'queued', -- queued|processing|completed|failed
    total_items              INTEGER      NOT NULL DEFAULT 0,
    completed_items          INTEGER      NOT NULL DEFAULT 0,
    result                   TEXT,
    created_by               UUID,
    created_at               TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at               TIMESTAMP    NOT NULL DEFAULT now(),
    completed_at             TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ai_jobs_status ON ai_jobs (school_id, status, created_at DESC);

-- ── ai_provider_health ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ai_provider_health (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider                 VARCHAR(32)  NOT NULL,
    model                    VARCHAR(96)  NOT NULL,
    circuit_state            VARCHAR(16)  NOT NULL DEFAULT 'closed', -- closed|open|half_open
    total_requests           INTEGER      NOT NULL DEFAULT 0,
    total_failures           INTEGER      NOT NULL DEFAULT 0,
    consecutive_failures     INTEGER      NOT NULL DEFAULT 0,
    avg_latency_ms           INTEGER      NOT NULL DEFAULT 0,
    rate_limit_hits          INTEGER      NOT NULL DEFAULT 0,
    last_failure_at          TIMESTAMP,
    circuit_opened_at        TIMESTAMP,
    last_updated             TIMESTAMP    NOT NULL DEFAULT now(),
    UNIQUE (provider, model)
);

-- Verification:
-- SELECT table_name FROM information_schema.tables
--   WHERE table_name IN ('ai_provider_config','ai_prompt_templates','ai_usage_log',
--                        'ai_response_cache','ai_jobs','ai_provider_health');
-- Expected: 6 rows

COMMIT;
