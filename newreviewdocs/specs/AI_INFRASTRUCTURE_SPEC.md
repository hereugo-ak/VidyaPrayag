# AI Infrastructure — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Audience:** Senior engineer / AI agent implementing the system
> **Last updated:** 2026-06-27
> **Prerequisites:** None
> **Unblocks:** All AI_* specs (AI_EXAM_ANALYSIS, AI_REPORT_CARD, AI_FEE_REMINDER, AI_TIMETABLE, AI_NL_QUERY, OCR_ADMISSION, VIDYASETU_AI_TUTOR, TEACHER_COPILOT, AI_SPOKEN_ENGLISH, AI_QUESTION_BANK)

---

## Table of Contents

1. [Feature Overview](#1-feature-overview)
2. [Current System Assessment](#2-current-system-assessment)
3. [Gap Analysis](#3-gap-analysis)
4. [Functional Requirements](#4-functional-requirements)
5. [Database Design](#5-database-design)
6. [Backend Architecture](#6-backend-architecture)
7. [API Contracts](#7-api-contracts)
8. [Security](#8-security)
9. [Configuration](#9-configuration)
10. [Performance Considerations](#10-performance-considerations)
11. [Cost Management](#11-cost-management)
12. [Monitoring](#12-monitoring)
13. [Migration Strategy](#13-migration-strategy)
14. [Testing Strategy](#14-testing-strategy)
15. [Acceptance Criteria](#14-acceptance-criteria)
16. [Implementation Roadmap](#15-implementation-roadmap)
17. [File-Level Impact Analysis](#16-file-level-impact-analysis)
18. [Risks & Mitigations](#17-risks--mitigations)

---

## 1. Feature Overview

A shared AI/LLM integration layer that provides a unified interface for all AI-powered features across Vidya Prayag. This spec defines the infrastructure for calling LLM providers (OpenAI GPT-4o, Google Gemini), managing API keys, tracking usage/costs, caching responses, and enforcing rate limits.

### Goals

- Single, reusable AI service that all feature modules call
- Support multiple LLM providers with automatic failover
- Per-school usage tracking and cost quotas
- Response caching to reduce API costs
- Structured prompt templates with version control
- Streaming support for long-form generation (report cards, lesson plans)
- Async job queue for batch AI operations (exam analysis for entire class)

---

## 2. Current System Assessment

### 2.1 What Exists

- **No AI/LLM integration** — confirmed by `feature_audit.csv` (AI Report Card: stub 20%, AI Tutoring: missing 0%)
- `DIFFERENTIATING_FEATURES.md` identifies AI as the #1 differentiator but notes "❌ Missing: OpenAI/Gemini API key configuration"
- No AI-related tables in `Tables.kt`
- `AppConfigTable` exists as a KV store — can hold API keys and configuration
- Existing `SupabaseStorage.kt` pattern (REST wrapper) is a good architectural model for an AI service wrapper

### 2.2 Existing Patterns to Reuse

- `NetworkResult<T>` sealed class for error handling
- Koin DI `single` pattern for service registration
- `AppConfigTable` for configuration storage
- Ktor Client for HTTP calls to LLM APIs
- kotlinx.serialization for JSON request/response

---

## 3. Gap Analysis

| # | Gap | Impact |
|---|---|---|
| G1 | No LLM provider integration | All AI features blocked |
| G2 | No API key management | Cannot configure providers without redeploy |
| G3 | No usage tracking | Cannot monitor costs or enforce quotas |
| G4 | No prompt template system | Prompts scattered across features, no versioning |
| G5 | No caching | Repeated identical prompts waste API budget |
| G6 | No streaming | Long-form generation blocks the request thread |
| G7 | No failover | Single provider outage = all AI features down |

---

## 4. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Support OpenAI (GPT-4o, GPT-4o-mini) and Google Gemini (2.0 Flash, 2.0 Pro) as providers |
| FR-2 | Provider selection configurable per feature (e.g., report cards use GPT-4o, fee reminders use Gemini Flash) |
| FR-3 | Automatic failover: if primary provider fails, try secondary |
| FR-4 | API keys stored encrypted in `AppConfigTable`, configurable via admin API |
| FR-5 | Per-school daily/monthly token usage tracking with configurable quotas |
| FR-6 | Response caching by prompt hash (TTL configurable per feature) |
| FR-7 | Streaming responses via SSE for long-form generation |
| FR-8 | Async job queue for batch operations (e.g., generate report cards for all students in a class) |
| FR-9 | Prompt templates stored in DB with version history |
| FR-10 | Rate limiting per school per feature |
| FR-11 | Cost estimation per request (input tokens × rate + output tokens × rate) |

---

## 5. Database Design

### 5.1 New Table: `ai_provider_config`

```sql
CREATE TABLE ai_provider_config (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider        VARCHAR(32) NOT NULL,          -- openai | gemini
    model           VARCHAR(64) NOT NULL,          -- gpt-4o | gpt-4o-mini | gemini-2.0-flash | gemini-2.0-pro
    api_key_encrypted TEXT NOT NULL,               -- AES-256 encrypted
    is_active       BOOLEAN NOT NULL DEFAULT true,
    priority        INTEGER NOT NULL DEFAULT 0,    -- failover order (0 = primary)
    max_tokens_per_request INTEGER NOT NULL DEFAULT 4096,
    temperature     REAL NOT NULL DEFAULT 0.7,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(provider, model)
);
```

### 5.2 New Table: `ai_prompt_templates`

```sql
CREATE TABLE ai_prompt_templates (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    feature         VARCHAR(48) NOT NULL,          -- report_card | exam_analysis | fee_reminder | timetable | nl_query | tutor | copilot
    name            VARCHAR(128) NOT NULL,         -- template identifier
    version         INTEGER NOT NULL DEFAULT 1,
    system_prompt   TEXT NOT NULL,
    user_prompt_template TEXT NOT NULL,            -- with {{variable}} placeholders
    variables       TEXT NOT NULL DEFAULT '[]',    -- JSON array of variable names
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(feature, name, version)
);
```

### 5.3 New Table: `ai_usage_log`

```sql
CREATE TABLE ai_usage_log (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    user_id         UUID,                          -- who triggered the request
    feature         VARCHAR(48) NOT NULL,          -- which feature used AI
    provider        VARCHAR(32) NOT NULL,
    model           VARCHAR(64) NOT NULL,
    prompt_template_id UUID,                       -- FK ai_prompt_templates.id
    input_tokens    INTEGER NOT NULL DEFAULT 0,
    output_tokens   INTEGER NOT NULL DEFAULT 0,
    cost_usd        REAL NOT NULL DEFAULT 0.0,     -- estimated cost
    latency_ms      INTEGER NOT NULL DEFAULT 0,
    status          VARCHAR(16) NOT NULL,          -- success | failed | cached
    error_message   TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_ai_usage_school_date ON ai_usage_log(school_id, created_at DESC);
CREATE INDEX idx_ai_usage_feature ON ai_usage_log(school_id, feature, created_at DESC);
```

### 5.4 New Table: `ai_response_cache`

```sql
CREATE TABLE ai_response_cache (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cache_key       TEXT NOT NULL,                 -- SHA-256(prompt + model + temperature)
    school_id       UUID,                          -- nullable for global cache
    feature         VARCHAR(48) NOT NULL,
    response        TEXT NOT NULL,                 -- cached LLM response
    input_tokens    INTEGER NOT NULL DEFAULT 0,
    output_tokens   INTEGER NOT NULL DEFAULT 0,
    expires_at      TIMESTAMP NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(cache_key)
);

CREATE INDEX idx_ai_cache_expiry ON ai_response_cache(expires_at);
```

### 5.5 New Table: `ai_jobs`

```sql
CREATE TABLE ai_jobs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    feature         VARCHAR(48) NOT NULL,
    status          VARCHAR(16) NOT NULL DEFAULT 'queued',  -- queued | processing | completed | failed
    total_items     INTEGER NOT NULL DEFAULT 0,
    completed_items INTEGER NOT NULL DEFAULT 0,
    result          TEXT,                          -- JSON result or error
    created_by      UUID,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now(),
    completed_at    TIMESTAMP
);

CREATE INDEX idx_ai_jobs_status ON ai_jobs(school_id, status, created_at DESC);
```

### 5.6 Exposed Mappings

```kotlin
object AiProviderConfigTable : UUIDTable("ai_provider_config", "id") {
    val provider           = varchar("provider", 32)
    val model              = varchar("model", 64)
    val apiKeyEncrypted    = text("api_key_encrypted")
    val isActive           = bool("is_active").default(true)
    val priority           = integer("priority").default(0)
    val maxTokensPerRequest = integer("max_tokens_per_request").default(4096)
    val temperature        = float("temperature").default(0.7f)
    val createdAt          = timestamp("created_at")
    val updatedAt          = timestamp("updated_at")
    init { uniqueIndex("ux_ai_provider_model", provider, model) }
}

object AiPromptTemplatesTable : UUIDTable("ai_prompt_templates", "id") {
    val feature            = varchar("feature", 48)
    val name               = varchar("name", 128)
    val version            = integer("version").default(1)
    val systemPrompt       = text("system_prompt")
    val userPromptTemplate = text("user_prompt_template")
    val variables          = text("variables").default("[]")
    val isActive           = bool("is_active").default(true)
    val createdAt          = timestamp("created_at")
    init { uniqueIndex("ux_ai_prompt", feature, name, version) }
}

object AiUsageLogTable : UUIDTable("ai_usage_log", "id") {
    val schoolId           = uuid("school_id")
    val userId             = uuid("user_id").nullable()
    val feature            = varchar("feature", 48)
    val provider           = varchar("provider", 32)
    val model              = varchar("model", 64)
    val promptTemplateId   = uuid("prompt_template_id").nullable()
    val inputTokens        = integer("input_tokens").default(0)
    val outputTokens       = integer("output_tokens").default(0)
    val costUsd            = float("cost_usd").default(0f)
    val latencyMs          = integer("latency_ms").default(0)
    val status             = varchar("status", 16)
    val errorMessage       = text("error_message").nullable()
    val createdAt          = timestamp("created_at")
    init {
        index("idx_ai_usage_school_date", false, schoolId, createdAt)
        index("idx_ai_usage_feature", false, schoolId, feature, createdAt)
    }
}

object AiResponseCacheTable : UUIDTable("ai_response_cache", "id") {
    val cacheKey           = text("cache_key")
    val schoolId           = uuid("school_id").nullable()
    val feature            = varchar("feature", 48)
    val response           = text("response")
    val inputTokens        = integer("input_tokens").default(0)
    val outputTokens       = integer("output_tokens").default(0)
    val expiresAt          = timestamp("expires_at")
    val createdAt          = timestamp("created_at")
    init { uniqueIndex("ux_ai_cache_key", cacheKey) }
}

object AiJobsTable : UUIDTable("ai_jobs", "id") {
    val schoolId           = uuid("school_id")
    val feature            = varchar("feature", 48)
    val status             = varchar("status", 16).default("queued")
    val totalItems         = integer("total_items").default(0)
    val completedItems     = integer("completed_items").default(0)
    val result             = text("result").nullable()
    val createdBy          = uuid("created_by").nullable()
    val createdAt          = timestamp("created_at")
    val updatedAt          = timestamp("updated_at")
    val completedAt        = timestamp("completed_at").nullable()
    init { index("idx_ai_jobs_status", false, schoolId, status, createdAt) }
}
```

---

## 6. Backend Architecture

### 6.1 Component Overview

```
┌──────────────────────────────────────────────────┐
│              Feature Modules                      │
│  (Report Card, Exam Analysis, Fee Reminder, ...)  │
└──────────────────┬───────────────────────────────┘
                   │ AiService.complete(prompt)
                   ▼
┌──────────────────────────────────────────────────┐
│              AiService                            │
│  - Template resolution ({{variable}} → value)     │
│  - Cache lookup                                   │
│  - Provider selection + failover                  │
│  - Usage logging                                  │
│  - Rate limiting                                  │
└──────┬───────────┬───────────────┬───────────────┘
       │           │               │
       ▼           ▼               ▼
┌──────────┐ ┌──────────┐ ┌──────────────┐
│ OpenAI   │ │ Gemini   │ │ Cache Store  │
│ Client   │ │ Client   │ │ (Postgres)   │
└──────────┘ └──────────┘ └──────────────┘
```

### 6.2 AiService

```kotlin
class AiService(
    private val providerConfigs: AiProviderConfigRepository,
    private val promptTemplates: AiPromptTemplateRepository,
    private val cache: AiCacheRepository,
    private val usageLog: AiUsageLogRepository,
    private val encryptionService: EncryptionService,
    private val rateLimiter: AiRateLimiter
) {
    /**
     * Non-streaming completion. Returns full response.
     */
    suspend fun complete(
        schoolId: UUID,
        userId: UUID?,
        feature: String,
        templateName: String,
        variables: Map<String, String>,
        overrideModel: String? = null,
        cacheTtlMinutes: Int = 0
    ): AiResult

    /**
     * Streaming completion. Emits chunks via Flow.
     */
    fun completeStream(
        schoolId: UUID,
        userId: UUID?,
        feature: String,
        templateName: String,
        variables: Map<String, String>,
        overrideModel: String? = null
    ): Flow<AiStreamChunk>

    /**
     * Submit a batch job. Returns job ID.
     */
    suspend fun submitBatch(
        schoolId: UUID,
        userId: UUID,
        feature: String,
        templateName: String,
        items: List<Map<String, String>>
    ): UUID

    /**
     * Get batch job status.
     */
    suspend fun getJobStatus(jobId: UUID): AiJobDto
}
```

### 6.3 Provider Clients

```kotlin
interface LlmProviderClient {
    suspend fun complete(request: LlmRequest): LlmResponse
    fun completeStream(request: LlmRequest): Flow<LlmStreamChunk>
    val providerName: String
}

class OpenAIClient(httpClient: HttpClient, apiKey: String) : LlmProviderClient {
    // POST https://api.openai.com/v1/chat/completions
    // Supports GPT-4o, GPT-4o-mini
    // Streaming via SSE: data: {"choices":[{"delta":{"content":"..."}}]}
}

class GeminiClient(httpClient: HttpClient, apiKey: String) : LlmProviderClient {
    // POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent
    // Streaming via streamGenerateContent
}
```

### 6.4 Failover Logic

```kotlin
suspend fun completeWithFailover(request: LlmRequest): LlmResponse {
    val providers = providerConfigs.getActiveProvidersSortedByPriority()
    for (provider in providers) {
        try {
            val client = providerClientFactory.create(provider)
            return client.complete(request)
        } catch (e: Exception) {
            log.warn("Provider ${provider.providerName} failed: ${e.message}")
            continue
        }
    }
    throw AiException("All providers failed")
}
```

### 6.5 Prompt Template Resolution

```kotlin
fun resolveTemplate(template: AiPromptTemplate, variables: Map<String, String>): String {
    var prompt = template.userPromptTemplate
    for ((key, value) in variables) {
        prompt = prompt.replace("{{${key}}}", value)
    }
    // Validate all variables were replaced
    val missing = Regex("\\{\\{(\\w+)\\}\\}").findAll(prompt).map { it.groupValues[1] }.toList()
    if (missing.isNotEmpty()) {
        throw AiException("Unresolved template variables: $missing")
    }
    return prompt
}
```

### 6.6 Cache Key

```kotlin
fun cacheKey(prompt: String, model: String, temperature: Float): String {
    val input = "$prompt|$model|$temperature"
    return MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        .joinToString("") { "%02x".format(it) }
}
```

### 6.7 Cost Calculation

```kotlin
// Pricing per 1M tokens (update via AppConfigTable)
val PRICING = mapOf(
    "gpt-4o" to Pricing(input = 2.50, output = 10.00),       // USD per 1M tokens
    "gpt-4o-mini" to Pricing(input = 0.15, output = 0.60),
    "gemini-2.0-flash" to Pricing(input = 0.10, output = 0.40),
    "gemini-2.0-pro" to Pricing(input = 1.25, output = 5.00)
)

fun calculateCost(model: String, inputTokens: Int, outputTokens: Int): Float {
    val pricing = PRICING[model] ?: return 0f
    return (inputTokens * pricing.input + outputTokens * pricing.output) / 1_000_000f
}
```

### 6.8 Batch Job Processor

A background coroutine that polls `ai_jobs` for `queued` jobs, processes items sequentially (with concurrency limit of 3), updates `completed_items`, and writes the final result.

---

## 7. API Contracts

### 7.1 AI Completion (Internal — not exposed directly)

Feature modules call `AiService` internally. The results are surfaced through feature-specific endpoints (e.g., `POST /api/v1/school/ai-report-card/generate`).

### 7.2 Provider Configuration

```
GET /api/v1/super/ai/providers
POST /api/v1/super/ai/providers
PATCH /api/v1/super/ai/providers/{id}
DELETE /api/v1/super/ai/providers/{id}
```

**Authorization:** Super Admin only.

### 7.3 Prompt Template Management

```
GET /api/v1/super/ai/prompts?feature={feature}
POST /api/v1/super/ai/prompts
PATCH /api/v1/super/ai/prompts/{id}
```

### 7.4 Usage Analytics

```
GET /api/v1/school/ai/usage?feature={feature}&date_from={YYYY-MM-DD}&date_to={YYYY-MM-DD}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "total_requests": 1250,
    "total_input_tokens": 450000,
    "total_output_tokens": 180000,
    "total_cost_usd": 2.85,
    "by_feature": [
      {"feature": "report_card", "requests": 500, "cost_usd": 2.00},
      {"feature": "exam_analysis", "requests": 750, "cost_usd": 0.85}
    ],
    "daily": [
      {"date": "2026-06-27", "requests": 45, "cost_usd": 0.12}
    ]
  }
}
```

### 7.5 Batch Job Status

```
GET /api/v1/school/ai/jobs/{jobId}
```

### 7.6 Quota Configuration

```
GET /api/v1/school/ai/quota
PATCH /api/v1/school/ai/quota
{
  "daily_token_limit": 500000,
  "monthly_cost_limit_usd": 50.0
}
```

---

## 8. Security

- API keys encrypted at rest using AES-256-GCM (key from environment variable `AI_ENCRYPTION_KEY`)
- API keys never returned in API responses (masked as `sk-****...****`)
- AI endpoints require authentication (JWT) + school scope
- Provider config and prompt template management restricted to Super Admin
- Usage logs are school-scoped — one school cannot see another's usage
- Rate limiting: 30 AI requests per minute per school (configurable)
- Input sanitization: user-provided variables are escaped before template injection
- No PII in prompt templates by default — features must explicitly opt in and log the data access

---

## 9. Configuration

### 9.1 Environment Variables

| Variable | Description | Default |
|---|---|---|
| `AI_ENCRYPTION_KEY` | AES-256 key for API key encryption | Required |
| `AI_DEFAULT_PROVIDER` | Default provider for features without explicit config | `openai` |
| `AI_DEFAULT_MODEL` | Default model | `gpt-4o-mini` |
| `AI_RATE_LIMIT_PER_MIN` | Rate limit per school | `30` |
| `AI_BATCH_CONCURRENCY` | Concurrent items in batch jobs | `3` |
| `AI_CACHE_DEFAULT_TTL_MIN` | Default cache TTL | `60` |

### 9.2 AppConfigTable Keys

| Key | Description |
|---|---|
| `ai_daily_token_limit_{schoolId}` | Per-school daily token quota |
| `ai_monthly_cost_limit_{schoolId}` | Per-school monthly cost cap (USD) |
| `ai_pricing_{model}` | Override pricing for a model |

---

## 10. Performance Considerations

- LLM API calls have 5-30s latency — always async, never block request thread
- Streaming via SSE for responses > 500 tokens
- Cache hit ratio target: 30%+ for report cards (same class, similar comments)
- Batch jobs process with concurrency 3 to avoid rate limits
- Usage logging is async (fire-and-forget)
- Cache cleanup job runs hourly to purge expired entries
- Connection pool for LLM API calls: 10 connections per provider

---

## 11. Cost Management

| Strategy | Implementation |
|---|---|
| Caching | SHA-256 key on prompt+model+temperature; TTL per feature |
| Model selection | Use GPT-4o-mini / Gemini Flash for simple tasks; GPT-4o / Gemini Pro for complex |
| Token budget | Per-school daily token limit; reject with 429 when exceeded |
| Cost alerts | Email/WhatsApp alert when monthly cost > 80% of quota |
| Prompt optimization | System prompts kept concise; few-shot examples minimized |
| Batch efficiency | Batch similar requests (e.g., all students in a class) to reduce per-request overhead |

### Estimated Monthly Cost (10 schools)

| Feature | Requests/month | Model | Est. cost/month |
|---|---|---|---|
| Report Cards (quarterly) | 10K | GPT-4o | ~$50 |
| Exam Analysis | 5K | GPT-4o-mini | ~$2 |
| Fee Reminders | 20K | Gemini Flash | ~$2 |
| Timetable Generation | 100 | GPT-4o | ~$1 |
| NL Query | 5K | GPT-4o-mini | ~$2 |
| **Total** | | | **~$57/month** |

---

## 12. Monitoring

| Metric | Type |
|---|---|
| `ai.requests_total` | Counter (by feature, provider) |
| `ai.request_latency_ms` | Histogram |
| `ai.tokens_input_total` | Counter |
| `ai.tokens_output_total` | Counter |
| `ai.cost_usd_total` | Counter |
| `ai.cache_hit_rate` | Ratio |
| `ai.provider_failures_total` | Counter |
| `ai.batch_jobs_queued` | Gauge |
| `ai.rate_limit_rejections` | Counter |

**Alerts:**
- Provider failure rate > 10% in 5 min → Critical
- Daily cost > 80% of monthly quota → Warning
- Cache hit rate < 10% → Warning (prompts too variable)
- Batch job stuck in `processing` > 30 min → Warning

---

## 13. Migration Strategy

### 13.1 Migration File

`docs/db/migration_031_ai_infrastructure.sql`

Creates all 5 tables (`ai_provider_config`, `ai_prompt_templates`, `ai_usage_log`, `ai_response_cache`, `ai_jobs`) with `CREATE TABLE IF NOT EXISTS`.

### 13.2 Seed Prompt Templates

Seed default templates for each feature:

```sql
INSERT INTO ai_prompt_templates (id, feature, name, version, system_prompt, user_prompt_template, variables, is_active, created_at)
VALUES
  (gen_random_uuid(), 'report_card', 'narrative_v1', 1,
   'You are an experienced teacher writing a report card comment...',
   'Student: {{student_name}}, Class: {{class_name}}, Subjects: {{subjects_with_grades}}, Attendance: {{attendance_pct}}%',
   '["student_name","class_name","subjects_with_grades","attendance_pct"]',
   true, now()),
  -- ... additional seed templates
ON CONFLICT (feature, name, version) DO NOTHING;
```

### 13.3 Rollback

Drop all 5 tables. No business data affected.

---

## 14. Testing Strategy

### 14.1 Unit Tests

- Template resolution — all variables replaced, missing variables detected
- Cache key generation — deterministic for same input
- Cost calculation — correct for each model
- Failover logic — primary fails, secondary succeeds
- Rate limiter — over-limit returns 429

### 14.2 Integration Tests

- Mock LLM provider → complete() returns response
- Cache hit → second call with same prompt returns cached, no provider call
- Usage logging — correct token counts and cost
- Batch job — submit 10 items, all complete, job status updates
- Provider config change — new key takes effect on next request
- Quota enforcement — exceeding daily limit returns 429

### 14.3 Mock Provider

```kotlin
class MockLlmProvider : LlmProviderClient {
    var nextResponse: String = "Mock AI response"
    var shouldFail: Boolean = false

    override suspend fun complete(request: LlmRequest): LlmResponse {
        if (shouldFail) throw RuntimeException("Mock failure")
        return LlmResponse(content = nextResponse, inputTokens = 100, outputTokens = 50)
    }
}
```

---

## 15. Acceptance Criteria

- [ ] `AiService.complete()` returns a response from configured provider
- [ ] Failover works when primary provider is down
- [ ] Cache prevents duplicate API calls for identical prompts
- [ ] Usage is logged with correct token counts and estimated cost
- [ ] Rate limiting rejects requests exceeding per-school limit
- [ ] Batch jobs process items and update progress
- [ ] Prompt templates can be created, versioned, and resolved
- [ ] API keys are encrypted at rest and masked in API responses
- [ ] Usage analytics endpoint returns correct aggregated data
- [ ] Quota configuration is enforced (daily token + monthly cost limits)

---

## 16. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 3 days | DB migration, Exposed tables, encryption service |
| 2 | 3 days | OpenAI client, Gemini client, LlmProviderClient interface |
| 3 | 2 days | AiService (template resolution, cache, failover, usage logging) |
| 4 | 2 days | Rate limiter, quota enforcement |
| 5 | 2 days | Batch job processor |
| 6 | 1 day | Seed prompt templates |
| 7 | 2 days | Admin API (provider config, prompt management, usage analytics) |
| 8 | 3 days | Tests (unit + integration with mock provider) |

---

## 17. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | 5 new table objects |
| `server/.../db/DatabaseFactory.kt` | Modify | Register 5 new tables |
| `server/.../core/crypto/EncryptionService.kt` | New | AES-256-GCM encryption for API keys |
| `server/.../feature/ai/AiService.kt` | New | Core AI service |
| `server/.../feature/ai/LlmProviderClient.kt` | New | Provider interface |
| `server/.../feature/ai/OpenAIClient.kt` | New | OpenAI implementation |
| `server/.../feature/ai/GeminiClient.kt` | New | Gemini implementation |
| `server/.../feature/ai/AiCacheRepository.kt` | New | Cache read/write |
| `server/.../feature/ai/AiUsageLogRepository.kt` | New | Usage tracking |
| `server/.../feature/ai/AiRateLimiter.kt` | New | Per-school rate limiting |
| `server/.../feature/ai/AiBatchProcessor.kt` | New | Batch job worker |
| `server/.../feature/ai/AiRouting.kt` | New | Admin API endpoints |
| `server/.../di/ServerModule.kt` | Modify | Register AI services in Koin |
| `docs/db/migration_031_ai_infrastructure.sql` | New | DDL + seed data |
| `shared/.../feature/ai/AiApi.kt` | New | Client API for usage analytics |
| `composeApp/.../ui/v2/screens/admin/AiUsageScreen.kt` | New | Usage dashboard |

---

## 18. Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| LLM provider outage | Medium | High | Multi-provider failover; cache as fallback |
| API key leakage | Low | Critical | AES-256 encryption at rest; masked in responses; env-var key |
| Cost overrun | Medium | High | Per-school quotas; cost alerts; model selection strategy |
| Prompt injection via user variables | Medium | Medium | Sanitize variables; system prompt isolation; reject control characters |
| LLM latency > 30s | Medium | Medium | Streaming; async batch; timeout at 60s with graceful error |
| Cache stale data | Low | Low | TTL per feature; manual cache invalidation endpoint |
| Token counting inaccuracy | Medium | Low | Use provider's token count in response; estimate only for pre-flight check |
