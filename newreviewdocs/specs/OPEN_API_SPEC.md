# Open API & Third-Party Integrations — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** `TWO_FACTOR_AUTH_SPEC.md`, `AUDIT_LOG_SPEC.md`
> **Source:** `COMPETITIVE_GAP_ANALYSIS.md` — integration ecosystem
> **Template:** `_SPEC_TEMPLATE.md` v1 (25 mandatory + 6 optional sections)

---

## 1. Feature Overview

### What

Public REST API for third-party integrations: enables other EdTech platforms, government portals (UDISE+), and custom school workflows to integrate with Vidya Prayag. Includes API key management, rate limiting, webhooks, and developer documentation.

### Why — Product Rationale

Schools use multiple systems — government portals (UDISE+), custom reporting tools, third-party EdTech platforms. Without an open API, data must be manually exported/imported, which is error-prone and time-consuming. An open API enables automated, secure integrations that reduce manual work and make Vidya Prayag a platform. This is a **medium-priority feature** (Phase 4, effort L) that builds an integration ecosystem.

### What Stands Out (Competitive Moat)

From `COMPETITIVE_GAP_ANALYSIS.md`: open API as integration ecosystem trend.

The key moat is **scoped, secure API access** — API keys with granular permissions (entity-level read/write), rate limiting per key, HMAC-signed webhooks, and sandbox mode for testing. This makes Vidya Prayag a platform, not just an app — other systems can integrate without manual data export/import.

### Goals

- API key management: admin generates API keys with scoped permissions
- REST API endpoints for core entities (students, attendance, marks, fees, classes)
- Rate limiting per API key (1000 req/hour default, configurable)
- Webhooks: notify third-party systems on events (student enrolled, fee paid, marks published)
- API documentation (OpenAPI/Swagger)
- Sandbox environment for testing
- Audit logging for all API calls

### Non-goals

- [ ] GraphQL API (REST only for simplicity)
- [ ] Real-time API (webhooks for async notifications, REST for sync queries)
- [ ] OAuth2/OIDC for third-party apps (API key auth only)
- [ ] Bulk data export API (use pagination for large datasets)
- [ ] API marketplace or public listing (school-admin generates keys)
- [ ] WebSocket streaming API (REST + webhooks only)
- [ ] Student/parent API access (admin-only key generation)
- [ ] Cross-school API access (school-scoped keys only)

### Dependencies

- `TWO_FACTOR_AUTH_SPEC.md` — security baseline (2FA for admin key generation)
- `AUDIT_LOG_SPEC.md` — audit logging infrastructure (reusable)
- `StudentsTable` — student data
- `AttendanceTable` — attendance data
- `MarksTable` — marks data
- `FeesTable` — fee data
- `ClassesTable` — class data
- `SchoolsTable` — school data
- `AppUsersTable` — admin accounts (key generation)
- Supabase / PostgreSQL — database
- Ktor — server framework

### Related Modules

- `server/.../feature/openapi/` — new open API module
- `server/.../feature/auth/` — existing auth (JWT for admin endpoints)
- `server/.../feature/audit/` — existing audit logging
- `server/.../db/` — database tables
- `composeApp/.../ui/v2/screens/admin/` — admin UI for API management

---

## 2. Current System Assessment

### Existing Code

- All existing endpoints are JWT-authenticated (user-specific)
- No API key authentication or public API
- `AUDIT_LOG_SPEC.md` — audit logging infrastructure (reusable)
- `TWO_FACTOR_AUTH_SPEC.md` — security baseline
- `COMPETITIVE_GAP_ANALYSIS.md`: open API as integration ecosystem trend

### Existing Database

- `StudentsTable` — student data
- `AttendanceTable` — attendance data
- `MarksTable` — marks data
- `FeesTable` — fee data
- `ClassesTable` — class data
- `SchoolsTable` — school data
- `AppUsersTable` — user accounts
- `AuditLogsTable` — audit logs (reusable)
- No API keys table
- No webhook subscriptions table
- No webhook deliveries table

### Existing APIs

- All existing endpoints: JWT-authenticated (user-specific)
- No API key authentication
- No public API namespace (`/api/v1/open/*`)
- No webhook management endpoints
- No API key management endpoints

### Existing UI

- Admin dashboard (existing) — admin functions
- No API key management UI
- No webhook configuration UI
- No delivery log viewer

### Existing Services

- `AuthService` — JWT authentication
- `AuditService` — audit logging (reusable)
- No API key service
- No webhook service
- No rate limiter

### Existing Documentation

- `COMPETITIVE_GAP_ANALYSIS.md` — open API as integration ecosystem trend
- `TWO_FACTOR_AUTH_SPEC.md` — security baseline
- `AUDIT_LOG_SPEC.md` — audit logging

### Technical Debt

| # | Gap | Details |
|---|---|---|
| TD-1 | No API key auth | No API key authentication middleware |
| TD-2 | No public API | No `/api/v1/open/*` namespace |
| TD-3 | No rate limiter | No per-key rate limiting |
| TD-4 | No webhooks | No webhook subscription or delivery |
| TD-5 | No API docs | No OpenAPI/Swagger documentation |
| TD-6 | No sandbox | No sandbox mode for testing |

### Gaps

| # | Gap | Impact | Severity |
|---|---|---|---|
| G1 | No public API | Third-party integrations impossible | **High** |
| G2 | No webhooks | Third-party systems can't receive real-time updates | **Medium** |
| G3 | No API docs | Developers can't integrate without documentation | **Medium** |
| G4 | No rate limiting | API abuse risk | **Medium** |
| G5 | No sandbox | Developers can't test without real data | **Low** |

---

## 3. Functional Requirements

### FR-001
| Field | Value |
|---|---|
| **Title** | API Key Generation |
| **Description** | Admin generates API key with: name, scopes (entities accessible), rate limit, expiry. |
| **Priority** | High |
| **User Roles** | School Admin |
| **Acceptance notes** | Admin specifies: name (e.g., "UDISE+ Integration"), scopes (JSON array: ["students:read", "attendance:read"]), rate_limit_per_hour (default 1000), expires_at (optional). Key generated with `vp_` prefix + 32 random chars. Key shown once (hashed at rest). 2FA required for key generation. |

### FR-002
| Field | Value |
|---|---|
| **Title** | API Key Authentication |
| **Description** | API key authentication: `Authorization: Bearer api_key` header. |
| **Priority** | High |
| **User Roles** | System |
| **Acceptance notes** | Middleware intercepts requests with `Bearer vp_*` header. Validates key (bcrypt hash match), checks active status, checks expiry. Key record attached to request attributes. School_id from key used for data scoping. |

### FR-003
| Field | Value |
|---|---|
| **Title** | Scoped Permissions |
| **Description** | Scoped permissions: read-only by default, write per scope. |
| **Priority** | High |
| **User Roles** | System |
| **Acceptance notes** | Scopes format: `entity:action` (e.g., `students:read`, `fees:read`, `attendance:write`). Read-only by default. Write scopes must be explicitly granted. Middleware checks scope before each endpoint. |

### FR-004
| Field | Value |
|---|---|
| **Title** | Rate Limiting |
| **Description** | Rate limiting: per API key (configurable, default 1000 req/hour). |
| **Priority** | High |
| **User Roles** | System |
| **Acceptance notes** | Sliding window rate limiter per API key. Default 1000 requests/hour. Configurable per key. Returns 429 Too Many Requests with `X-RateLimit-Remaining` and `X-RateLimit-Reset` headers. |

### FR-005
| Field | Value |
|---|---|
| **Title** | Webhook Registration |
| **Description** | Webhooks: register URL → receive POST on events (student.enrolled, fee.paid, marks.published, attendance.marked). |
| **Priority** | Medium |
| **User Roles** | School Admin |
| **Acceptance notes** | Admin registers webhook URL + events list. HMAC secret generated for signing payloads. On event, server sends HTTP POST with JSON payload + `X-Webhook-Signature` header (HMAC-SHA256). |

### FR-006
| Field | Value |
|---|---|
| **Title** | Webhook Retry |
| **Description** | Webhook retry: 3 retries with exponential backoff on failure. |
| **Priority** | Medium |
| **User Roles** | System |
| **Acceptance notes** | On delivery failure (non-2xx response or timeout), retry with exponential backoff: 1min, 5min, 15min. Max 3 attempts. After 3 failures, status=failed. Background job processes retries every 5 minutes. |

### FR-007
| Field | Value |
|---|---|
| **Title** | OpenAPI/Swagger Documentation |
| **Description** | OpenAPI/Swagger documentation auto-generated. |
| **Priority** | Medium |
| **User Roles** | Developer |
| **Acceptance notes** | Auto-generated OpenAPI spec at `/api/v1/open/swagger.json`. Swagger UI at `/api/v1/open/docs`. Includes all public endpoints, request/response schemas, authentication info. |

### FR-008
| Field | Value |
|---|---|
| **Title** | Sandbox Mode |
| **Description** | Sandbox: test API keys for development. |
| **Priority** | Low |
| **User Roles** | School Admin, Developer |
| **Acceptance notes** | Admin generates sandbox key (is_sandbox=true). Sandbox key returns mock data (no real database queries). No real mutations. For developer testing only. |

### FR-009
| Field | Value |
|---|---|
| **Title** | Audit Logging |
| **Description** | All API calls logged in audit log. |
| **Priority** | High |
| **User Roles** | System |
| **Acceptance notes** | Every API call logged in `audit_logs`: API key ID, endpoint, method, response status, timestamp. Uses existing `AUDIT_LOG_SPEC.md` infrastructure. |

### FR-010
| Field | Value |
|---|---|
| **Title** | API Key Revocation |
| **Description** | API key revocation. |
| **Priority** | High |
| **User Roles** | School Admin |
| **Acceptance notes** | Admin revokes API key. Key status set to inactive (is_active=false). Revoked_at and revoked_by recorded. Subsequent requests with revoked key return 401. 2FA required for revocation. |

### Non-Functional Requirements

| ID | Requirement |
|---|---|
| NFR-1 | API key validation: < 50ms (bcrypt hash comparison) |
| NFR-2 | Rate limit check: < 10ms (in-memory sliding window) |
| NFR-3 | API endpoint response: < 500ms (excluding DB query time) |
| NFR-4 | Webhook delivery: < 5 seconds (HTTP POST timeout) |
| NFR-5 | Webhook retry: exponential backoff (1min, 5min, 15min) |
| NFR-6 | Swagger UI load: < 2 seconds |
| NFR-7 | Audit log write: < 50ms (async) |
| NFR-8 | Concurrent API requests: ~100 per school (rate-limited) |
| NFR-9 | Pagination: 50 records per page default, max 200 |
| NFR-10 | API key generation: < 1 second (bcrypt hash) |

---

## 4. User Stories

### School Admin
- [ ] Generate API key with specific scopes and rate limit
- [ ] View list of active API keys
- [ ] Revoke an API key
- [ ] Register webhook URL for event notifications
- [ ] View webhook delivery logs (success, failures, retries)
- [ ] Generate sandbox API key for testing
- [ ] View API documentation (Swagger UI)

### Third-Party Developer
- [ ] Authenticate with API key (Bearer token)
- [ ] Query students with pagination and filtering
- [ ] Query attendance by class and date range
- [ ] Query marks by class, term, and subject
- [ ] Query fees by status and date range
- [ ] List classes
- [ ] Receive webhook notifications for registered events
- [ ] Verify webhook signature (HMAC)
- [ ] Test integration with sandbox key
- [ ] Read API documentation (OpenAPI/Swagger)

---

## 5. Business Rules

### BR-001
**Rule:** API keys are school-scoped.
**Enforcement:** Every API key has school_id. All queries scoped to key's school_id. No cross-school data access. Key generated by school admin only.

### BR-002
**Rule:** API keys are bcrypt-hashed at rest.
**Enforcement:** Key shown once at generation (plaintext). Stored as bcrypt hash. key_prefix (first 8 chars) stored for identification. Validation: bcrypt compare.

### BR-003
**Rule:** Read-only by default, write per scope.
**Enforcement:** Scopes format: `entity:action`. Default scopes: read-only. Write scopes must be explicitly granted. Middleware checks scope before each endpoint.

### BR-004
**Rule:** Rate limiting per key (sliding window).
**Enforcement:** Sliding window rate limiter per API key. Default 1000 req/hour. Configurable per key. Returns 429 with rate limit headers. In-memory tracking (not DB).

### BR-005
**Rule:** Webhook payloads are HMAC-signed.
**Enforcement:** Each webhook subscription has a secret. Payload signed with HMAC-SHA256. Signature in `X-Webhook-Signature` header. Receiver verifies signature.

### BR-006
**Rule:** Webhook retries: 3 attempts with exponential backoff.
**Enforcement:** On failure (non-2xx or timeout): retry at 1min, 5min, 15min. Max 3 attempts. After 3 failures: status=failed. Background job processes retries every 5 minutes.

### BR-007
**Rule:** All API calls are audit-logged.
**Enforcement:** Every API call logged in `audit_logs`: key ID, endpoint, method, status, timestamp. Uses existing `AUDIT_LOG_SPEC.md` infrastructure. Async logging.

### BR-008
**Rule:** 2FA required for API key generation and revocation.
**Enforcement:** Admin must complete 2FA challenge before generating or revoking API keys. Uses `TWO_FACTOR_AUTH_SPEC.md` infrastructure.

### BR-009
**Rule:** Sandbox keys return mock data.
**Enforcement:** Sandbox key (is_sandbox=true) returns mock data. No real database queries. No real mutations. For developer testing only.

### BR-010
**Rule:** API key format: `vp_` prefix + 32 random alphanumeric chars.
**Enforcement:** Key format: `vp_` + 32 chars [a-zA-Z0-9]. key_prefix (first 8 chars including `vp_`) stored for identification. Full key shown once at generation.

---

## 6. Database Design

### 6.1 Entity Relationship Summary

Three new tables: `api_keys` (API key management), `webhook_subscriptions` (webhook registration), `webhook_deliveries` (webhook delivery tracking). References existing `schools`, `app_users` tables.

### 6.2 New Tables

#### `api_keys` table

```sql
CREATE TABLE api_keys (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    name            TEXT NOT NULL,                 -- "UDISE+ Integration", "Custom Portal"
    key_hash        TEXT NOT NULL,                 -- bcrypt hash of API key
    key_prefix      VARCHAR(8) NOT NULL,           -- first 8 chars for identification (vp_xxxx...)
    scopes          TEXT NOT NULL DEFAULT '[]',    -- JSON: ["students:read", "attendance:read", "fees:read"]
    rate_limit_per_hour INTEGER NOT NULL DEFAULT 1000,
    is_sandbox      BOOLEAN NOT NULL DEFAULT false,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    expires_at      TIMESTAMP,
    last_used_at    TIMESTAMP,
    created_by      UUID,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    revoked_at      TIMESTAMP,
    revoked_by      UUID
);
CREATE INDEX idx_api_keys_school ON api_keys(school_id, is_active);
```

#### `webhook_subscriptions` table

```sql
CREATE TABLE webhook_subscriptions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    api_key_id      UUID NOT NULL REFERENCES api_keys(id),
    url             TEXT NOT NULL,                 -- webhook endpoint URL
    events          TEXT NOT NULL DEFAULT '[]',    -- JSON: ["student.enrolled", "fee.paid"]
    secret          TEXT NOT NULL,                 -- HMAC signing secret
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
```

#### `webhook_deliveries` table

```sql
CREATE TABLE webhook_deliveries (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    webhook_id      UUID NOT NULL REFERENCES webhook_subscriptions(id) ON DELETE CASCADE,
    event_type      VARCHAR(48) NOT NULL,
    payload         TEXT NOT NULL,                 -- JSON payload sent
    response_status INTEGER,                       -- HTTP status code
    response_body   TEXT,
    attempt         INTEGER NOT NULL DEFAULT 1,
    status          VARCHAR(16) NOT NULL DEFAULT 'pending', -- pending | delivered | failed | retrying
    delivered_at    TIMESTAMP,
    next_retry_at   TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_webhook_deliveries_pending ON webhook_deliveries(status, next_retry_at);
```

### 6.3 Modified Tables

N/A — no existing tables modified. All new tables.

### 6.4 Indexes

- `idx_api_keys_school` on `api_keys(school_id, is_active)` — active keys per school
- `idx_webhook_deliveries_pending` on `webhook_deliveries(status, next_retry_at)` — pending retries

### 6.5 Constraints

- `api_keys.school_id` — NOT NULL
- `api_keys.key_hash` — NOT NULL
- `api_keys.key_prefix` — NOT NULL
- `api_keys.scopes` — NOT NULL, default '[]'
- `api_keys.rate_limit_per_hour` — NOT NULL, default 1000
- `api_keys.is_sandbox` — NOT NULL, default false
- `api_keys.is_active` — NOT NULL, default true
- `webhook_subscriptions.school_id` — NOT NULL
- `webhook_subscriptions.api_key_id` — NOT NULL, FK to api_keys
- `webhook_subscriptions.url` — NOT NULL
- `webhook_subscriptions.events` — NOT NULL, default '[]'
- `webhook_subscriptions.secret` — NOT NULL
- `webhook_deliveries.webhook_id` — NOT NULL, FK to webhook_subscriptions (CASCADE)
- `webhook_deliveries.event_type` — NOT NULL
- `webhook_deliveries.payload` — NOT NULL
- `webhook_deliveries.attempt` — NOT NULL, default 1
- `webhook_deliveries.status` — NOT NULL, default 'pending'

### 6.6 Foreign Keys

- `webhook_subscriptions.api_key_id` → `api_keys.id` (explicit FK)
- `webhook_deliveries.webhook_id` → `webhook_subscriptions.id` (explicit FK, CASCADE)
- `api_keys.school_id` → `schools.id` (implicit)
- `api_keys.created_by` → `app_users.id` (implicit)
- `api_keys.revoked_by` → `app_users.id` (implicit)
- `webhook_subscriptions.school_id` → `schools.id` (implicit)

### 6.7 Soft Delete Strategy

N/A — API keys are revoked (is_active=false, revoked_at, revoked_by), not deleted. Webhook subscriptions can be deactivated (is_active=false). Webhook deliveries are historical records, not deleted.

### 6.8 Audit Fields

- `api_keys.created_at` — when key was generated
- `api_keys.created_by` — who generated the key
- `api_keys.last_used_at` — last API call with this key
- `api_keys.expires_at` — when key expires (optional)
- `api_keys.revoked_at` — when key was revoked
- `api_keys.revoked_by` — who revoked the key
- `webhook_subscriptions.created_at` — when webhook was registered
- `webhook_deliveries.created_at` — when delivery was created
- `webhook_deliveries.delivered_at` — when delivery succeeded
- `webhook_deliveries.next_retry_at` — when to retry (if failed)

### 6.9 Migration Notes

Migration: `docs/db/migration_092_open_api.sql`
- CREATE `api_keys` table with index
- CREATE `webhook_subscriptions` table
- CREATE `webhook_deliveries` table with index
- No data migration (new feature)

### 6.10 Exposed Mappings

```kotlin
object ApiKeysTable : UUIDTable("api_keys", "id") {
    val schoolId          = uuid("school_id")
    val name              = text("name")
    val keyHash           = text("key_hash")
    val keyPrefix         = varchar("key_prefix", 8)
    val scopes            = text("scopes").default("[]")
    val rateLimitPerHour  = integer("rate_limit_per_hour").default(1000)
    val isSandbox         = bool("is_sandbox").default(false)
    val isActive          = bool("is_active").default(true)
    val expiresAt         = timestamp("expires_at").nullable()
    val lastUsedAt        = timestamp("last_used_at").nullable()
    val createdBy         = uuid("created_by").nullable()
    val createdAt         = timestamp("created_at")
    val revokedAt         = timestamp("revoked_at").nullable()
    val revokedBy         = uuid("revoked_by").nullable()

    init {
        index("idx_api_keys_school", schoolId, isActive)
    }
}

object WebhookSubscriptionsTable : UUIDTable("webhook_subscriptions", "id") {
    val schoolId   = uuid("school_id")
    val apiKeyId   = uuid("api_key_id").references(ApiKeysTable.id)
    val url        = text("url")
    val events     = text("events").default("[]")
    val secret     = text("secret")
    val isActive   = bool("is_active").default(true)
    val createdAt  = timestamp("created_at")
}

object WebhookDeliveriesTable : UUIDTable("webhook_deliveries", "id") {
    val webhookId      = uuid("webhook_id").references(WebhookSubscriptionsTable.id, onDelete = ReferenceOption.CASCADE)
    val eventType      = varchar("event_type", 48)
    val payload        = text("payload")
    val responseStatus = integer("response_status").nullable()
    val responseBody   = text("response_body").nullable()
    val attempt        = integer("attempt").default(1)
    val status         = varchar("status", 16).default("pending")
    val deliveredAt    = timestamp("delivered_at").nullable()
    val nextRetryAt    = timestamp("next_retry_at").nullable()
    val createdAt      = timestamp("created_at")

    init {
        index("idx_webhook_deliveries_pending", status, nextRetryAt)
    }
}
```

Register all three tables in `DatabaseFactory.allTables`.

### 6.11 Seed Data

N/A — API keys and webhooks are admin-generated. No seed data needed.

---

## 7. State Machines

### API Key Lifecycle State Machine

```
generated ──admin_activates──> active ──admin_revokes──> revoked (terminal)
  │                              │
  │                              ├──expires──> expired (terminal)
  │                              │
  │                              └──api_call──> active (last_used_at updated)
  │
  └──generated (sandbox) ──admin_activates──> active (sandbox) ──admin_revokes──> revoked
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `generated` | Admin generates key | `active` | Key created, is_active=true |
| `active` | Admin revokes key | `revoked` | is_active=false, revoked_at set |
| `active` | Key expires | `expired` | expires_at < now |
| `active` | API call made | `active` | last_used_at updated |
| `revoked` | (terminal) | `revoked` | Key cannot be reactivated |
| `expired` | (terminal) | `expired` | Key expired, admin must generate new |

### Webhook Delivery State Machine

```
pending ──http_post_sent──> delivered (terminal, success)
  │
  ├──http_post_failed──> retrying ──retry_succeeded──> delivered (terminal)
  │                          │
  │                          ├──retry_failed──> retrying (attempt++)
  │                          │
  │                          └──max_retries_reached──> failed (terminal)
  │
  └──pending ──timeout──> retrying
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `pending` | HTTP POST sent, 2xx response | `delivered` | delivered_at set |
| `pending` | HTTP POST sent, non-2xx response | `retrying` | next_retry_at set (1min) |
| `pending` | HTTP POST timeout | `retrying` | next_retry_at set (1min) |
| `retrying` | Retry sent, 2xx response | `delivered` | delivered_at set |
| `retrying` | Retry sent, non-2xx response | `retrying` | attempt++, next_retry_at set (5min, 15min) |
| `retrying` | Max retries reached (3) | `failed` | status=failed, no more retries |
| `delivered` | (terminal) | `delivered` | Delivery successful |
| `failed` | (terminal) | `failed` | All retries exhausted |

### Webhook Subscription State Machine

```
registered ──admin_activates──> active ──admin_deactivates──> inactive
  │                                │
  │                                └──active ──admin_deactivates──> inactive
  │
  └──inactive ──admin_reactivates──> active
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `registered` | Webhook created | `active` | is_active=true |
| `active` | Admin deactivates | `inactive` | is_active=false |
| `inactive` | Admin reactivates | `active` | is_active=true |

---

## 8. Backend Architecture

### 8.1 Component Overview

`ApiKeyService` manages API key generation, validation, and revocation. `ApiKeyAuthMiddleware` intercepts requests with API key auth, validates, checks rate limit. `OpenApiRouting` defines public API endpoints (`/api/v1/open/*`). `WebhookService` handles webhook triggering, delivery, and retries. `RateLimiter` provides in-memory sliding window rate limiting. `SwaggerConfig` auto-generates OpenAPI documentation.

### 8.2 Design Principles

1. **Secure by default** — read-only scopes, bcrypt-hashed keys, 2FA for key management
2. **School-scoped** — all data scoped to API key's school_id
3. **Rate-limited** — per-key rate limiting prevents abuse
4. **Observable** — all API calls audit-logged
5. **Developer-friendly** — Swagger docs, sandbox mode, HMAC-signed webhooks
6. **Resilient** — webhook retries with exponential backoff

### 8.3 Core Types

#### ApiKeyAuthMiddleware

```kotlin
fun Application.apiKeyAuth() {
    intercept(ApplicationCallPipeline.Features) {
        val authHeader = call.request.headers["Authorization"]
        if (authHeader?.startsWith("Bearer vp_") == true) {
            val apiKey = authHeader.removePrefix("Bearer ")
            val keyRecord = apiKeyService.validate(apiKey)
            if (keyRecord != null) {
                call.attributes.put(ApiKeyKey, keyRecord)
                // Check rate limit
                if (!rateLimiter.check(keyRecord.id, keyRecord.rateLimitPerHour)) {
                    call.respond(HttpStatusCode.TooManyRequests)
                    return@intercept
                }
            } else {
                call.respond(HttpStatusCode.Unauthorized)
                return@intercept
            }
        }
    }
}
```

#### ApiKeyService

```kotlin
class ApiKeyService {
    suspend fun generateKey(schoolId: UUID, name: String, scopes: List<String>, rateLimitPerHour: Int, expiresAt: Instant?, isSandbox: Boolean, createdBy: UUID): ApiKeyDto
    suspend fun validate(apiKey: String): ApiKeyRecord?  // bcrypt compare
    suspend fun revoke(keyId: UUID, revokedBy: UUID): Unit
    suspend fun listKeys(schoolId: UUID): List<ApiKeyDto>
    suspend fun updateLastUsed(keyId: UUID): Unit
}
```

#### WebhookService

```kotlin
class WebhookService {
    suspend fun triggerEvent(schoolId: UUID, eventType: String, payload: JsonObject) {
        // 1. Find all active webhook subscriptions for school + event
        // 2. For each: create webhook_delivery (status=pending)
        // 3. Send HTTP POST with HMAC-signed payload
        // 4. On success: status=delivered
        // 5. On failure: schedule retry (exponential backoff, max 3)
    }

    suspend fun processRetries()  // background job every 5 min
    suspend fun registerWebhook(schoolId: UUID, apiKeyId: UUID, url: String, events: List<String>): WebhookDto
    suspend fun listWebhooks(schoolId: UUID): List<WebhookDto>
    suspend fun deleteWebhook(webhookId: UUID): Unit
    suspend fun getDeliveries(webhookId: UUID, status: String?): List<WebhookDeliveryDto>
}
```

#### RateLimiter

```kotlin
class RateLimiter {
    fun check(keyId: UUID, limitPerHour: Int): Boolean  // sliding window
    fun getRemaining(keyId: UUID, limitPerHour: Int): Int
    fun getResetTime(keyId: UUID): Instant
}
```

### 8.4 Repositories

- `ApiKeyRepository` — CRUD for `api_keys`
- `WebhookSubscriptionRepository` — CRUD for `webhook_subscriptions`
- `WebhookDeliveryRepository` — CRUD for `webhook_deliveries`

### 8.5 Mappers

- `ApiKeyMapper` — maps `api_keys` rows to DTOs (never includes key_hash)
- `WebhookMapper` — maps `webhook_subscriptions` rows to DTOs (never includes secret)
- `WebhookDeliveryMapper` — maps `webhook_deliveries` rows to DTOs

### 8.6 Permission Checks

- API key generation: school admin + 2FA
- API key revocation: school admin + 2FA
- API key validation: middleware (bcrypt compare, active check, expiry check)
- Scope check: middleware (entity:action scope required for endpoint)
- Webhook management: school admin
- Open API endpoints: valid API key + required scope
- All data scoped to API key's school_id

### 8.7 Background Jobs

- **Webhook Retry Job** — every 5 minutes
  1. Find deliveries with status=retrying and next_retry_at <= now
  2. For each: send HTTP POST with HMAC-signed payload
  3. On success: status=delivered, delivered_at set
  4. On failure: attempt++, next_retry_at set (exponential backoff)
  5. If attempt >= 3: status=failed
  6. Return count of deliveries processed

- **API Key Expiry Job** — every 1 hour
  1. Find active API keys with expires_at < now
  2. Set is_active=false (auto-expire)
  3. Return count of keys expired

- **Webhook Delivery Cleanup** — daily
  1. Delete webhook_deliveries older than 30 days with status=delivered
  2. Return count of deliveries cleaned up

### 8.8 Domain Events

- `ApiKeyGenerated` — emitted when API key is generated
- `ApiKeyRevoked` — emitted when API key is revoked
- `ApiKeyExpired` — emitted when API key auto-expires
- `WebhookRegistered` — emitted when webhook is registered
- `WebhookDelivered` — emitted when webhook delivery succeeds
- `WebhookFailed` — emitted when webhook delivery fails (all retries exhausted)
- `ApiCallLogged` — emitted when API call is audit-logged

### 8.9 Caching

- API key validation: cached for 5 minutes (bcrypt is expensive)
- Rate limiter: in-memory (real-time, no caching)
- Webhook subscriptions: cached for 5 minutes
- Swagger spec: cached (generated once, updated on deploy)

### 8.10 Transactions

- Generate API key: single transaction (insert api_key)
- Revoke API key: single transaction (update is_active, revoked_at, revoked_by)
- Register webhook: single transaction (insert webhook_subscription)
- Trigger webhook: single transaction (insert webhook_delivery) + async HTTP POST
- Webhook retry: single transaction (update delivery status, next_retry_at)

### 8.11 Rate Limiting

- Sliding window rate limiter per API key
- Default 1000 requests/hour
- Configurable per key (rate_limit_per_hour)
- In-memory tracking (ConcurrentHashMap with timestamp deque)
- Returns 429 with headers: `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`

### 8.12 Configuration

- `OPEN_API_ENABLED` — default `false`; enable/disable open API
- `OPEN_API_RATE_LIMIT_DEFAULT` — default `1000`; default rate limit per hour
- `OPEN_API_RATE_LIMIT_BURST` — default `50`; max burst (requests per second)
- `OPEN_API_WEBHOOK_TIMEOUT_SECONDS` — default `10`; HTTP POST timeout
- `OPEN_API_WEBHOOK_MAX_RETRIES` — default `3`; max retry attempts
- `OPEN_API_WEBHOOK_RETRY_DELAYS` — default `1,5,15`; retry delays in minutes
- `OPEN_API_SANDBOX_ENABLED` — default `true`; enable sandbox mode
- `OPEN_API_SWAGGER_ENABLED` — default `true`; enable Swagger UI
- `OPEN_API_KEY_EXPIRY_DAYS` — default `365`; default key expiry (if not specified)
- `OPEN_API_DELIVERY_RETENTION_DAYS` — default `30`; days to keep delivered webhook deliveries

---

## 9. API Contracts

### 9.1 API Key Management (Admin, JWT-authenticated)

```
POST /api/v1/school/api-keys
  Body: { name, scopes: [...], rate_limit_per_hour, expires_at?, is_sandbox? }
  → 201: { id, name, key_prefix, scopes, rate_limit_per_hour, is_sandbox, is_active, expires_at, api_key: "vp_..." }
  → 403: 2FA required

GET /api/v1/school/api-keys
  → 200: { keys: [ApiKeyDto] }

DELETE /api/v1/school/api-keys/{id}
  → 200: { revoked: true }
  → 403: 2FA required
```

### 9.2 Open API Endpoints (API key-authenticated)

```
GET /api/v1/open/students?page=1&limit=50&class_id={uuid}
  → 200: { students: [...], pagination: { page, limit, total, total_pages } }
  → 401: Invalid API key
  → 403: Scope not authorized
  → 429: Rate limit exceeded

GET /api/v1/open/students/{id}
  → 200: StudentDto
  → 404: Not found

GET /api/v1/open/attendance?class_id={uuid}&from={YYYY-MM-DD}&to={YYYY-MM-DD}
  → 200: { attendance: [...] }

GET /api/v1/open/marks?class_id={uuid}&term=term1&subject_id={uuid}
  → 200: { marks: [...] }

GET /api/v1/open/fees?status=OVERDUE&from={}&to={}
  → 200: { fees: [...] }

GET /api/v1/open/classes
  → 200: { classes: [...] }
```

### 9.3 Webhook Management (Admin, JWT-authenticated)

```
POST /api/v1/school/webhooks
  Body: { url, events: [...] }
  → 201: WebhookDto (includes secret for HMAC verification)

GET /api/v1/school/webhooks
  → 200: { webhooks: [WebhookDto] }

DELETE /api/v1/school/webhooks/{id}
  → 204: No Content

GET /api/v1/school/webhooks/{id}/deliveries?status=failed
  → 200: { deliveries: [WebhookDeliveryDto] }
```

### 9.4 Documentation Endpoints (Public)

```
GET /api/v1/open/swagger.json
  → 200: OpenAPI JSON spec

GET /api/v1/open/docs
  → 200: Swagger UI HTML
```

### 9.5 Webhook Payload (Sent to registered URL)

```
POST {registered_url}
  Headers:
    Content-Type: application/json
    X-Webhook-Signature: {HMAC-SHA256 signature}
    X-Webhook-Event: {event_type}
    X-Webhook-Delivery: {delivery_id}
  Body: {
    event: "student.enrolled",
    timestamp: "2026-06-28T10:00:00Z",
    school_id: "...",
    data: { ... event-specific payload ... }
  }
  → 2xx: Delivery successful
  → non-2xx: Delivery failed, will retry
```

### 9.6 DTO Models

All `@Serializable`, wrapped in `ApiResponse<T>` pattern.

```kotlin
@Serializable data class ApiKeyDto(
    val id: String,
    val name: String,
    val keyPrefix: String,         // "vp_abc1..."
    val scopes: List<String>,      // ["students:read", "attendance:read"]
    val rateLimitPerHour: Int,
    val isSandbox: Boolean,
    val isActive: Boolean,
    val expiresAt: String?,
    val lastUsedAt: String?,
    val createdAt: String,
    val revokedAt: String?,
    // api_key only included in generate response (never in list)
)

@Serializable data class GenerateApiKeyResponse(
    val apiKey: String,            // full key, shown once
    val key: ApiKeyDto,
)

@Serializable data class WebhookDto(
    val id: String,
    val url: String,
    val events: List<String>,
    val isActive: Boolean,
    val createdAt: String,
    // secret only included in register response
)

@Serializable data class RegisterWebhookResponse(
    val secret: String,            // HMAC secret, shown once
    val webhook: WebhookDto,
)

@Serializable data class WebhookDeliveryDto(
    val id: String,
    val webhookId: String,
    val eventType: String,
    val payload: String,           // JSON string
    val responseStatus: Int?,
    val responseBody: String?,
    val attempt: Int,
    val status: String,            // pending | delivered | failed | retrying
    val deliveredAt: String?,
    val nextRetryAt: String?,
    val createdAt: String,
)

@Serializable data class OpenApiStudentDto(
    val id: String,
    val name: String,
    val classId: String,
    val className: String,
    val rollNumber: String?,
    val admissionNumber: String?,
    val dateOfAdmission: String?,
)

@Serializable data class PaginationDto(
    val page: Int,
    val limit: Int,
    val total: Int,
    val totalPages: Int,
)
```

---

## 10. Frontend Architecture

### 10.1 Screens

| Screen | Platform | Role | Description |
|---|---|---|---|
| `ApiManagementScreen` | Compose | School Admin | API key management (generate, list, revoke) |
| `WebhookManagementScreen` | Compose | School Admin | Webhook configuration (register, list, delete) |
| `WebhookDeliveryLogScreen` | Compose | School Admin | Webhook delivery logs (success, failures, retries) |
| `ApiDocumentationScreen` | Compose (WebView) | School Admin | Swagger UI (embedded) |

### 10.2 Navigation

- Admin dashboard → API Management → API Keys / Webhooks / Delivery Logs / Documentation

### 10.3 UX Flows

#### Admin: Generate API Key
1. Admin opens API Management → taps "Generate API Key"
2. 2FA challenge (enter OTP)
3. Form: name, scopes (checkboxes), rate limit, expiry, sandbox toggle
4. Submit → key generated → full key shown once (copy button)
5. Admin copies key → key stored securely by third-party developer

#### Admin: Register Webhook
1. Admin opens Webhook Management → taps "Register Webhook"
2. Form: URL, events (checkboxes: student.enrolled, fee.paid, etc.)
3. Submit → webhook registered → secret shown once (copy button)
4. Admin shares secret with third-party for HMAC verification

#### Admin: View Delivery Logs
1. Admin opens Webhook Management → selects webhook → taps "Delivery Logs"
2. List of deliveries: event, status, response code, attempts, timestamp
3. Filter by status (all, delivered, failed, retrying)
4. Tap delivery → view payload, response body

### 10.4 State Management

```kotlin
data class ApiManagementState(
    val apiKeys: List<ApiKeyDto>,
    val isLoading: Boolean,
    val generatedKey: String?,      // shown once after generation
    val isGenerating: Boolean,
    val error: String?,
)

data class WebhookManagementState(
    val webhooks: List<WebhookDto>,
    val isLoading: Boolean,
    val generatedSecret: String?,   // shown once after registration
    val isRegistering: Boolean,
    val error: String?,
)

data class WebhookDeliveryState(
    val deliveries: List<WebhookDeliveryDto>,
    val filterStatus: String?,
    val isLoading: Boolean,
    val error: String?,
)
```

### 10.5 Offline Support

- API key list: cached locally (5-minute TTL)
- Webhook list: cached locally (5-minute TTL)
- Delivery logs: requires online (real-time)
- Swagger docs: requires online (served from server)

### 10.6 Loading States

- Generating API key: "Generating API key... (2FA required)"
- Revoking API key: "Revoking API key..."
- Registering webhook: "Registering webhook..."
- Loading delivery logs: "Loading delivery logs..."

### 10.7 Error Handling (UI)

- 2FA failed: "2FA verification failed. Please try again."
- API key generation failed: "Failed to generate API key."
- API key revoked: "API key revoked successfully."
- Webhook registration failed: "Failed to register webhook."
- Invalid webhook URL: "Please enter a valid URL (https://...)."
- No API keys: "No API keys generated yet."
- No webhooks: "No webhooks registered yet."
- No deliveries: "No webhook deliveries yet."

### 10.8 Component Integration Guidelines

| Rule | Description |
|---|---|
| **R1** | API key shown once: full key in a copyable text field with "Copy" button |
| **R2** | Key prefix displayed in list (e.g., "vp_abc1...") for identification |
| **R3** | Scopes: checkboxes for entity:action pairs (students:read, students:write, etc.) |
| **R4** | Rate limit: number input (default 1000, min 100, max 10000) |
| **R5** | Expiry: date picker (optional, default 365 days) |
| **R6** | Sandbox: toggle switch (default off) |
| **R7** | Webhook URL: text input with URL validation (https:// required) |
| **R8** | Webhook events: checkboxes (student.enrolled, fee.paid, marks.published, etc.) |
| **R9** | Webhook secret: shown once in a copyable text field |
| **R10** | Delivery logs: table with event, status badge, response code, attempts, timestamp |
| **R11** | Status badges: green (delivered), red (failed), yellow (retrying), gray (pending) |
| **R12** | 2FA modal: OTP input before key generation/revocation |
| **R13** | Swagger UI: embedded WebView at `/api/v1/open/docs` |

---

## 11. Shared Module Changes (KMP)

### 11.1 DTOs

All DTOs defined in section 9.6, placed in `shared/.../openapi/domain/model/OpenApiModels.kt`.

### 11.2 Domain Models

```kotlin
data class ApiKey(
    val id: String,
    val schoolId: String,
    val name: String,
    val keyPrefix: String,
    val scopes: List<String>,
    val rateLimitPerHour: Int,
    val isSandbox: Boolean,
    val isActive: Boolean,
    val expiresAt: Instant?,
    val lastUsedAt: Instant?,
    val createdAt: Instant,
    val revokedAt: Instant?,
)

data class WebhookSubscription(
    val id: String,
    val schoolId: String,
    val apiKeyId: String,
    val url: String,
    val events: List<String>,
    val isActive: Boolean,
    val createdAt: Instant,
)

data class WebhookDelivery(
    val id: String,
    val webhookId: String,
    val eventType: String,
    val payload: String,
    val responseStatus: Int?,
    val responseBody: String?,
    val attempt: Int,
    val status: DeliveryStatus,
    val deliveredAt: Instant?,
    val nextRetryAt: Instant?,
    val createdAt: Instant,
)

enum class DeliveryStatus { PENDING, DELIVERED, FAILED, RETRYING }
```

### 11.3 Repository Interfaces

```kotlin
interface ApiKeyManagementRepository {
    suspend fun generateKey(token: String, request: GenerateApiKeyRequest): NetworkResult<GenerateApiKeyResponse>
    suspend fun listKeys(token: String): NetworkResult<List<ApiKeyDto>>
    suspend fun revokeKey(token: String, keyId: String): NetworkResult<Unit>
}

interface WebhookManagementRepository {
    suspend fun registerWebhook(token: String, request: RegisterWebhookRequest): NetworkResult<RegisterWebhookResponse>
    suspend fun listWebhooks(token: String): NetworkResult<List<WebhookDto>>
    suspend fun deleteWebhook(token: String, webhookId: String): NetworkResult<Unit>
    suspend fun getDeliveries(token: String, webhookId: String, status: String?): NetworkResult<List<WebhookDeliveryDto>>
}
```

### 11.4 UseCases

- `GenerateApiKeyUseCase`
- `ListApiKeysUseCase`
- `RevokeApiKeyUseCase`
- `RegisterWebhookUseCase`
- `ListWebhooksUseCase`
- `DeleteWebhookUseCase`
- `GetWebhookDeliveriesUseCase`

### 11.5 Validation

- `name`: non-empty, max 100 characters
- `scopes`: valid entity:action pairs (students:read, students:write, attendance:read, etc.)
- `rate_limit_per_hour`: 100-10000
- `expires_at`: future date (optional)
- `url`: valid HTTPS URL
- `events`: non-empty list of valid event types

### 11.6 Serialization

Standard Kotlinx serialization. Enums serialized as lowercase strings. Timestamps as ISO strings. Scopes and events as JSON arrays.

### 11.7 Network APIs

Ktor `@Resource` route definitions in `ApiKeyManagementApi.kt`, `WebhookManagementApi.kt`:
- POST `/api/v1/school/api-keys`
- GET `/api/v1/school/api-keys`
- DELETE `/api/v1/school/api-keys/{id}`
- POST `/api/v1/school/webhooks`
- GET `/api/v1/school/webhooks`
- DELETE `/api/v1/school/webhooks/{id}`
- GET `/api/v1/school/webhooks/{id}/deliveries`

### 11.8 Database Models (Local Cache)

- API key list: cached locally (5-minute TTL)
- Webhook list: cached locally (5-minute TTL)
- Delivery logs: not cached (real-time)

---

## 12. Permissions Matrix

| Action | Super Admin | School Admin | Counselor | Teacher | Parent | Student |
|---|---|---|---|---|---|---|
| Generate API key | N/A | ✅ (own school, 2FA) | ❌ | ❌ | ❌ | ❌ |
| List API keys | N/A | ✅ (own school) | ❌ | ❌ | ❌ | ❌ |
| Revoke API key | N/A | ✅ (own school, 2FA) | ❌ | ❌ | ❌ | ❌ |
| Register webhook | N/A | ✅ (own school) | ❌ | ❌ | ❌ | ❌ |
| List webhooks | N/A | ✅ (own school) | ❌ | ❌ | ❌ | ❌ |
| Delete webhook | N/A | ✅ (own school) | ❌ | ❌ | ❌ | ❌ |
| View delivery logs | N/A | ✅ (own school) | ❌ | ❌ | ❌ | ❌ |
| View API docs (Swagger) | ✅ (all) | ✅ (own school) | ❌ | ❌ | ❌ | ❌ |
| Use Open API (API key) | N/A | N/A | N/A | N/A | N/A | N/A (third-party) |

---

## 13. Notifications

### Admin Notifications

| Trigger | Recipient | Channel | Template |
|---|---|---|---|
| API key generated | School Admin | In-app | "API key '{name}' generated. Key: {key_prefix}..." |
| API key revoked | School Admin | In-app | "API key '{name}' ({key_prefix}...) has been revoked." |
| API key expired | School Admin | In-app + email | "API key '{name}' ({key_prefix}...) has expired." |
| Webhook delivery failed (all retries) | School Admin | In-app + email | "Webhook delivery failed for {event_type} to {url}. All 3 retry attempts failed." |
| Rate limit exceeded (warning at 90%) | School Admin | In-app | "API key '{name}' has used 90% of its rate limit." |

### Notification Integration

Uses `SmartNotificationService` with "normal" priority for key management, "high" priority for webhook delivery failures (action needed).

---

## 14. Background Jobs

### Webhook Retry Job

| Property | Value |
|---|---|
| **Name** | `WebhookRetryJob` |
| **Schedule** | Every 5 minutes |
| **Duration** | < 30 seconds |

#### Job Flow

1. Find deliveries with status=retrying and next_retry_at <= now
2. For each: send HTTP POST with HMAC-signed payload
3. On success (2xx): status=delivered, delivered_at set
4. On failure: attempt++, next_retry_at set (exponential backoff: 1min, 5min, 15min)
5. If attempt >= 3: status=failed, notify admin
6. Return count of deliveries processed

### API Key Expiry Job

| Property | Value |
|---|---|
| **Name** | `ApiKeyExpiryJob` |
| **Schedule** | Every 1 hour |
| **Duration** | < 5 seconds |

#### Job Flow

1. Find active API keys with expires_at < now
2. Set is_active=false (auto-expire)
3. Notify admin: "API key expired"
4. Return count of keys expired

### Webhook Delivery Cleanup Job

| Property | Value |
|---|---|
| **Name** | `WebhookDeliveryCleanupJob` |
| **Schedule** | Daily at 3 AM |
| **Duration** | < 10 seconds |

#### Job Flow

1. Delete webhook_deliveries older than 30 days with status=delivered
2. Return count of deliveries cleaned up

---

## 15. Integrations

### Internal Integrations

| System | Integration Point | Direction | Protocol | Error Handling |
|---|---|---|---|---|
| `AUDIT_LOG_SPEC.md` | Audit logging | Call | Direct call | Log error, continue |
| `TWO_FACTOR_AUTH_SPEC.md` | 2FA for key management | Call | Direct call | Required (block if 2FA fails) |
| `StudentsTable` | Student data | Read | Direct DB | Required |
| `AttendanceTable` | Attendance data | Read | Direct DB | Required |
| `MarksTable` | Marks data | Read | Direct DB | Required |
| `FeesTable` | Fee data | Read | Direct DB | Required |
| `ClassesTable` | Class data | Read | Direct DB | Required |
| `SchoolsTable` | School data | Read | Direct DB | Required |
| `AppUsersTable` | Admin accounts | Read | Direct DB | Required |
| `Notify.kt` | Notification dispatch | Call | Direct call | Log error, continue |
| `SmartNotificationService` | Notification priority | Call | Direct call | Log error, continue |

### External Integrations

| System | Integration Point | Direction | Protocol | Error Handling |
|---|---|---|---|---|
| Third-party webhook URLs | Webhook delivery | Call | HTTP POST | Retry on failure (3 attempts) |
| Swagger/kotlin-swagger | API documentation | Use | Library | Log error, docs unavailable |

### Integration Patterns

- **Audit logging:** Every API call logged via `AuditService.log()` (async). Uses existing `AUDIT_LOG_SPEC.md` infrastructure.
- **2FA:** Admin must complete 2FA challenge before generating/revoking API keys. Uses `TWO_FACTOR_AUTH_SPEC.md` infrastructure.
- **Webhook delivery:** HTTP POST with HMAC-SHA256 signature. Receiver verifies signature. Retries on failure.
- **Data scoping:** All queries scoped to API key's school_id. No cross-school access.

---

## 16. Security

### Authentication

- API key auth: `Authorization: Bearer vp_*` header
- API key validation: bcrypt hash comparison
- Admin endpoints: JWT auth (existing) + 2FA for key generation/revocation
- Swagger UI: public (no auth needed to view docs)

### Authorization

- API key scopes: entity:action (e.g., students:read, fees:read)
- Read-only by default, write per scope
- Middleware checks scope before each endpoint
- School-scoped: all data scoped to API key's school_id
- Admin endpoints: school admin role + 2FA

### Data Protection

- API keys bcrypt-hashed at rest (never stored in plaintext)
- Full API key shown once at generation (never retrievable)
- Webhook secrets shown once at registration (never retrievable)
- Webhook payloads HMAC-signed with subscription secret
- No PII in Swagger docs (schema only, no real data)
- Sandbox keys return mock data (no real data exposure)

### Input Validation

- `name`: non-empty, max 100 characters
- `scopes`: valid entity:action pairs
- `rate_limit_per_hour`: 100-10000
- `expires_at`: future date (optional)
- `url`: valid HTTPS URL
- `events`: non-empty list of valid event types
- Pagination: page >= 1, limit 1-200

### Rate Limiting

- Sliding window rate limiter per API key
- Default 1000 requests/hour
- Configurable per key (100-10000)
- Returns 429 with `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset` headers
- In-memory tracking (not DB)

### Audit Logging

- Every API call logged: API key ID, endpoint, method, response status, timestamp
- Uses existing `AUDIT_LOG_SPEC.md` infrastructure
- Async logging (non-blocking)
- Logs retained per audit policy

### PII Handling

- API keys: no PII (key_hash, key_prefix, name, scopes)
- Open API endpoints: return student data (name, class) — PII
- Webhook payloads: may contain PII (student names, fee amounts)
- Webhook delivery: HTTPS required for webhook URLs
- Swagger docs: schema only, no real data

### Multi-tenant Isolation

- All API keys school-scoped (school_id)
- All queries filtered by school_id from API key
- No cross-school data access
- Webhook subscriptions school-scoped
- Admin can only manage own school's keys and webhooks

---

## 17. Performance & Scalability

### Expected Scale

- API keys per school: ~5-20 (few integrations per school)
- API requests: ~100-1000 per hour per key (rate-limited)
- Webhook subscriptions per school: ~3-10
- Webhook deliveries per day: ~10-100 per school
- Concurrent API requests: ~100 per school (rate-limited)

### Query Optimization

- API key validation: cached for 5 minutes (bcrypt is expensive)
- Rate limiter: in-memory (O(1) check)
- Open API endpoints: use existing indexes on students, attendance, marks, fees
- Webhook deliveries: indexed by (status, next_retry_at) for retry job
- Pagination: LIMIT/OFFSET (or cursor-based for large datasets)

### Indexing Strategy

- `api_keys(school_id, is_active)` — active keys per school
- `webhook_deliveries(status, next_retry_at)` — pending retries
- Existing indexes on students, attendance, marks, fees, classes (reused)

### Caching Strategy

- API key validation: cached for 5 minutes (bcrypt is expensive)
- Rate limiter: in-memory (real-time, no caching)
- Webhook subscriptions: cached for 5 minutes
- Swagger spec: cached (generated once, updated on deploy)

### Pagination

- Default: 50 records per page
- Maximum: 200 records per page
- Response includes: page, limit, total, total_pages

### Connection Pooling

Uses existing HikariCP connection pool. No additional pooling needed.

### Async Processing

- API key validation: synchronous (cached after first validation)
- Rate limit check: synchronous (in-memory)
- Open API endpoints: synchronous (DB query)
- Webhook delivery: async (HTTP POST in background)
- Webhook retry: async (background job)
- Audit logging: async (non-blocking)

### Scalability Concerns

- Rate limiter: in-memory, per-instance. For multi-instance deployment, use Redis-based rate limiter.
- Webhook delivery: HTTP POST in background. For high volume, use message queue.
- API key cache: per-instance. For multi-instance, use Redis-based cache.
- DB queries: use existing indexes. Pagination for large datasets.

---

## 18. Edge Cases

| # | Scenario | Expected Behavior |
|---|---|---|
| EC-1 | Invalid API key | Return 401. "Invalid API key." |
| EC-2 | Revoked API key | Return 401. "API key has been revoked." |
| EC-3 | Expired API key | Return 401. "API key has expired." |
| EC-4 | Scope not authorized | Return 403. "Your API key does not have permission to access this resource." |
| EC-5 | Rate limit exceeded | Return 429. "Rate limit exceeded. Try again in {reset_seconds} seconds." |
| EC-6 | Sandbox key on write endpoint | Return 403. "Sandbox keys cannot perform write operations." |
| EC-7 | Webhook URL unreachable | Delivery failed. Retry scheduled. After 3 retries: status=failed. |
| EC-8 | Webhook returns non-2xx | Delivery failed. Retry scheduled. After 3 retries: status=failed. |
| EC-9 | Webhook URL timeout (>10s) | Delivery failed (timeout). Retry scheduled. |
| EC-10 | API key with no scopes | Return 403 for all endpoints. "No scopes authorized." |
| EC-11 | Pagination beyond total_pages | Return 200 with empty array. |
| EC-12 | 2FA not completed for key generation | Return 403. "2FA verification required." |
| EC-13 | Non-admin tries to generate key | Return 403. "Only school admins can manage API keys." |
| EC-14 | Webhook secret mismatch (receiver) | Receiver should verify HMAC. If mismatch, receiver should reject. |

---

## 19. Error Handling

### Error Response Format

Standard JSON error format:

```json
{
  "error": {
    "code": "ERROR_CODE",
    "message": "User-friendly message",
    "details": {}
  }
}
```

### Error Codes

| Code | HTTP Status | Description | User Message |
|---|---|---|---|
| `INVALID_API_KEY` | 401 | Invalid API key | "Invalid API key." |
| `API_KEY_REVOKED` | 401 | API key revoked | "API key has been revoked." |
| `API_KEY_EXPIRED` | 401 | API key expired | "API key has expired." |
| `SCOPE_NOT_AUTHORIZED` | 403 | Scope not authorized | "Your API key does not have permission to access this resource." |
| `RATE_LIMIT_EXCEEDED` | 429 | Rate limit exceeded | "Rate limit exceeded. Try again in {reset_seconds} seconds." |
| `SANDBOX_WRITE_BLOCKED` | 403 | Sandbox key write attempt | "Sandbox keys cannot perform write operations." |
| `RESOURCE_NOT_FOUND` | 404 | Resource not found | "Resource not found." |
| `2FA_REQUIRED` | 403 | 2FA not completed | "2FA verification required." |
| `NOT_SCHOOL_ADMIN` | 403 | Non-admin access | "Only school admins can manage API keys." |
| `INVALID_WEBHOOK_URL` | 400 | Invalid webhook URL | "Please provide a valid HTTPS URL." |
| `WEBHOOK_DELIVERY_FAILED` | 200 (to caller) | Webhook delivery failed | (Logged, admin notified) |

### Error Handling Strategy

- **Invalid/expired/revoked key:** Return 401. Caller must use valid key.
- **Scope not authorized:** Return 403. Caller must request additional scope.
- **Rate limit exceeded:** Return 429 with rate limit headers. Caller must wait.
- **Webhook delivery failure:** Retry (3 attempts). After 3 failures: admin notified.
- **2FA required:** Return 403. Admin must complete 2FA challenge.

### Retry Strategy

- API calls: caller responsible for retry (rate limit may apply)
- Webhook delivery: server retries (3 attempts, exponential backoff)
- Webhook retry job: processes pending retries every 5 minutes

### Fallback Behavior

- API key cache miss: fall back to DB query (bcrypt validation)
- Rate limiter unavailable: allow request (fail-open for availability)
- Webhook URL unreachable: retry (3 attempts), then admin notified
- Swagger generation fails: docs unavailable (non-critical)
- Audit logging fails: log error, continue (non-blocking)

---

## 20. Analytics & Reporting

### Analytics Dashboard Data

| Metric | Source | Derivation |
|---|---|---|
| API key count | `api_keys` | Count per school (active) |
| API calls per day | `audit_logs` | Count of API calls per key per day |
| API calls by endpoint | `audit_logs` | Count per endpoint |
| Rate limit hits | `audit_logs` | Count of 429 responses per key |
| Webhook deliveries | `webhook_deliveries` | Count per webhook, success rate |
| Webhook delivery failures | `webhook_deliveries` | Count where status=failed |
| Average API response time | `audit_logs` | avg(response_time_ms) per endpoint |
| Top API consumers | `audit_logs` | Top keys by call count |

### Export Capabilities

- API usage report (CSV) — key, endpoint, call count, date range
- Webhook delivery report (CSV) — webhook, event, status, attempts, date

### Report Types

| Report | Format | Frequency | Recipient |
|---|---|---|---|
| API usage | JSON (API) | Monthly | Product Team |
| Webhook health | JSON (API) | Monthly | DevOps Team |
| Integration adoption | JSON (API) | Monthly | Product Team |

---

## 21. Testing Strategy

### Unit Tests

- `ApiKeyService.generateKey()` — key generation, bcrypt hashing, scope validation
- `ApiKeyService.validate()` — bcrypt comparison, active check, expiry check
- `ApiKeyService.revoke()` — status update, revoked_at/by set
- `RateLimiter.check()` — sliding window, boundary cases, concurrent access
- `WebhookService.triggerEvent()` — subscription lookup, delivery creation
- `WebhookService.processRetries()` — retry logic, exponential backoff, max attempts
- Scope checking: valid/invalid scopes, read vs write
- HMAC signing: correct signature generation

### Integration Tests

- Full API key flow: generate → use → revoke → 401
- Full API call flow: authenticate → query → audit log written
- Rate limiting: 1001st request → 429
- Scope enforcement: read-only key on write endpoint → 403
- Webhook flow: register → trigger event → delivery created → HTTP POST sent
- Webhook retry: failure → retry → success (or failure after 3 attempts)
- Sandbox mode: sandbox key → mock data returned
- 2FA enforcement: key generation without 2FA → 403
- Multi-tenant: cross-school query → empty result (scoped)

### E2E Tests

- Admin generates API key → third-party queries students → data returned
- Admin registers webhook → event triggered → webhook delivered → receiver verifies HMAC
- Admin revokes API key → subsequent API calls → 401
- API key expires → auto-expire job → 401
- Webhook delivery fails 3 times → admin notified

### Performance Tests

- API key validation: < 50ms (cached) / < 200ms (uncached, bcrypt)
- Rate limit check: < 10ms (in-memory)
- Open API endpoint: < 500ms (excluding DB time)
- Webhook delivery: < 5 seconds (HTTP POST timeout)
- 100 concurrent API requests: all handled (rate-limited)

### Test Data

- 5 test API keys with various scopes and rate limits
- 2 test webhook subscriptions (one active, one inactive)
- Test webhook delivery records (pending, delivered, failed, retrying)
- Mock webhook receiver (HTTP server for delivery testing)
- Test sandbox key
- Test 2FA challenge

### Test Environment

- Test database with api_keys, webhook_subscriptions, webhook_deliveries tables
- Mock webhook receiver (HTTP server)
- Test JWT tokens for admin
- Test API keys (real bcrypt hashing)
- Test HMAC verification

---

## 22. Acceptance Criteria

- [ ] Admin generates API keys with scoped permissions
- [ ] API key authentication works
- [ ] Rate limiting enforced per key
- [ ] Open API endpoints return correct data (scoped to school)
- [ ] Webhooks fire on registered events
- [ ] Webhook retries on failure (3 attempts, exponential backoff)
- [ ] OpenAPI/Swagger documentation available
- [ ] All API calls audit-logged
- [ ] API key revocation works
- [ ] Sandbox mode returns mock data
- [ ] 2FA required for key generation and revocation
- [ ] Webhook payloads HMAC-signed
- [ ] Pagination works on all list endpoints
- [ ] School-scoped (no cross-school data access)

---

## 23. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 2 days | DB migration `migration_092_open_api.sql`, Exposed tables, register in `DatabaseFactory` |
| 2 | 2 days | `ApiKeyAuthMiddleware` + `RateLimiter` (sliding window) |
| 3 | 3 days | Open API endpoints (students, attendance, marks, fees, classes) with scope checking |
| 4 | 2 days | `WebhookService` (trigger, deliver, retry with exponential backoff) |
| 5 | 2 days | Swagger/OpenAPI documentation (`SwaggerConfig`) |
| 6 | 2 days | API key management + webhook management endpoints (admin, 2FA) |
| 7 | 2 days | Client UI: `ApiManagementScreen`, `WebhookManagementScreen`, `WebhookDeliveryLogScreen` |
| 8 | 2 days | Tests: unit, integration, E2E |

### Pre-Implementation Checklist

- [ ] Verify `AUDIT_LOG_SPEC.md` is implemented (audit logging)
- [ ] Verify `TWO_FACTOR_AUTH_SPEC.md` is implemented (2FA for admin)
- [ ] Verify existing tables (students, attendance, marks, fees, classes) have indexes
- [ ] Verify Ktor supports interceptors for API key middleware
- [ ] Verify Swagger/kotlin-swagger library is available
- [ ] Verify HTTP client for webhook delivery (Ktor HttpClient)
- [ ] Set up test webhook receiver for integration testing

---

## 24. File-Level Impact Analysis

### Server

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | `ApiKeysTable`, `WebhookSubscriptionsTable`, `WebhookDeliveriesTable` |
| `server/.../db/DatabaseFactory.kt` | Modify | Register 3 new tables in `allTables` |
| `server/.../feature/openapi/ApiKeyService.kt` | **New** | API key management (generate, validate, revoke) |
| `server/.../feature/openapi/ApiKeyAuthMiddleware.kt` | **New** | Auth middleware (Bearer vp_ validation, rate limit) |
| `server/.../feature/openapi/RateLimiter.kt` | **New** | Sliding window rate limiter (in-memory) |
| `server/.../feature/openapi/OpenApiRouting.kt` | **New** | Public API endpoints (`/api/v1/open/*`) |
| `server/.../feature/openapi/WebhookService.kt` | **New** | Webhook delivery + retry |
| `server/.../feature/openapi/WebhookRouting.kt` | **New** | Webhook management endpoints (admin) |
| `server/.../feature/openapi/ApiKeyRouting.kt` | **New** | API key management endpoints (admin) |
| `server/.../feature/openapi/SwaggerConfig.kt` | **New** | OpenAPI/Swagger documentation |
| `docs/db/migration_092_open_api.sql` | **New** | DDL (3 tables + indexes) |

### Shared (KMP)

| File | Change Type | Description |
|---|---|---|
| `shared/.../openapi/domain/model/OpenApiModels.kt` | **New** | DTOs, domain models, enums |
| `shared/.../openapi/domain/repository/ApiKeyManagementRepository.kt` | **New** | API key repository interface |
| `shared/.../openapi/domain/repository/WebhookManagementRepository.kt` | **New** | Webhook repository interface |
| `shared/.../openapi/data/remote/ApiKeyManagementApi.kt` | **New** | HTTP API definitions |
| `shared/.../openapi/data/remote/WebhookManagementApi.kt` | **New** | HTTP API definitions |

### Client (Compose)

| File | Change Type | Description |
|---|---|---|
| `composeApp/.../ui/v2/screens/admin/ApiManagementScreen.kt` | **New** | API key management UI |
| `composeApp/.../ui/v2/screens/admin/WebhookManagementScreen.kt` | **New** | Webhook configuration UI |
| `composeApp/.../ui/v2/screens/admin/WebhookDeliveryLogScreen.kt` | **New** | Delivery log viewer |
| `composeApp/.../ui/v2/screens/admin/ApiDocumentationScreen.kt` | **New** | Swagger UI (embedded WebView) |

---

## 25. Future Enhancements

| # | Enhancement | Priority | Effort | Notes |
|---|---|---|---|---|
| F-1 | GraphQL API | Low | L | Alternative query language for flexible queries |
| F-2 | OAuth2/OIDC | Medium | L | OAuth2 for third-party app authorization (instead of API keys) |
| F-3 | API marketplace | Low | XL | Public listing of available integrations |
| F-4 | Bulk export API | Medium | M | Bulk data export (CSV/JSON) for large datasets |
| F-5 | WebSocket streaming | Low | L | Real-time data streaming via WebSocket |
| F-6 | API versioning | Medium | S | Versioned API (`/api/v2/open/*`) with backward compatibility |
| F-7 | Request signing | Low | M | Request body signing (in addition to Bearer auth) |
| F-8 | IP allowlisting | Medium | S | Restrict API key to specific IP addresses |
| F-9 | API analytics dashboard | Medium | M | Real-time API usage dashboard for admins |
| F-10 | Webhook event filtering | Low | S | Filter webhook events by entity (e.g., only specific classes) |

---

## Appendix A: Sequence Diagrams

### A.1 API Key Generation

```
Admin (app)        Server              DB
  │                    │                  │
  │  POST /school/     │                  │
  │  api-keys          │                  │
  │  {name, scopes,    │                  │
  │   rate_limit,      │                  │
  │   expires_at}      │                  │
  │  + 2FA OTP         │                  │
  │  ───────────────>  │                  │
  │                    │──verify 2FA─────>│
  │                    │←──ok─────────────│
  │                    │                  │
  │                    │──generate key    │
  │                    │  (vp_ + 32 chars)│
  │                    │──bcrypt hash─────│
  │                    │──insert api_key>│
  │                    │←──ok─────────────│
  │  ←──201: {         │                  │
  │    api_key: "vp_...",                  │
  │    key: ApiKeyDto  │                  │
  │  }                 │                  │
  │                    │                  │
  │  (key shown once,  │                  │
  │   admin copies)    │                  │
  │                    │                  │
```

### A.2 API Call with API Key

```
Third-party         Server              DB
  │                    │                  │
  │  GET /api/v1/open/ │                  │
  │  students?page=1   │                  │
  │  Authorization:    │                  │
  │  Bearer vp_abc1... │                  │
  │  ───────────────>  │                  │
  │                    │──validate key    │
  │                    │  (bcrypt compare)│
  │                    │  (cached 5min)   │
  │                    │←──valid──────────│
  │                    │                  │
  │                    │──check rate limit│
  │                    │  (in-memory)     │
  │                    │←──ok─────────────│
  │                    │                  │
  │                    │──check scope     │
  │                    │  (students:read) │
  │                    │←──authorized─────│
  │                    │                  │
  │                    │──query students─>│
  │                    │  (school-scoped) │
  │                    │←──students───────│
  │                    │                  │
  │                    │──audit log──────>│
  │                    │  (async)         │
  │                    │                  │
  │  ←──200: {         │                  │
  │    students: [...],│                  │
  │    pagination: {...}│                 │
  │  }                 │                  │
  │                    │                  │
```

### A.3 Webhook Delivery and Retry

```
Server              DB              Third-party URL
  │                    │                  │
  │  (event triggered: │                  │
  │   student.enrolled)│                  │
  │                    │                  │
  │──find webhooks────>│                  │
  │  (school + event)  │                  │
  │←──subscriptions────│                  │
  │                    │                  │
  │──insert delivery──>│                  │
  │  (status=pending)  │                  │
  │←──ok───────────────│                  │
  │                    │                  │
  │──HTTP POST─────────────────────────>│
  │  payload + HMAC    │                  │
  │  signature         │                  │
  │←──200 OK─────────────────────────────│
  │                    │                  │
  │──update delivery──>│                  │
  │  (status=delivered)│                  │
  │←──ok───────────────│                  │
  │                    │                  │

  (If delivery fails:)
  │──HTTP POST─────────────────────────>│
  │←──500 Error──────────────────────────│
  │                    │                  │
  │──update delivery──>│                  │
  │  (status=retrying, │                  │
  │   next_retry_at=   │                  │
  │   now+1min)        │                  │
  │←──ok───────────────│                  │
  │                    │                  │
  (Retry job, 5 min later:)
  │──find retries─────>│                  │
  │  (status=retrying, │                  │
  │   next_retry_at<=now)│                │
  │←──deliveries───────│                  │
  │──HTTP POST─────────────────────────>│
  │←──200 OK─────────────────────────────│
  │──update delivery──>│                  │
  │  (status=delivered)│                  │
  │←──ok───────────────│                  │
  │                    │                  │
```

---

## Appendix B: Domain Model / ER Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                      api_keys (new)                                   │
│  id (PK)                                                              │
│  school_id, name                                                      │
│  key_hash (bcrypt), key_prefix (vp_xxxx...)                           │
│  scopes (JSON: ["students:read", ...])                                │
│  rate_limit_per_hour (default 1000)                                   │
│  is_sandbox, is_active                                                │
│  expires_at, last_used_at                                             │
│  created_by, created_at                                               │
│  revoked_at, revoked_by                                               │
│  INDEX: (school_id, is_active)                                        │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│                  webhook_subscriptions (new)                          │
│  id (PK)                                                              │
│  school_id, api_key_id (FK api_keys)                                  │
│  url, events (JSON: ["student.enrolled", ...])                        │
│  secret (HMAC signing)                                                │
│  is_active, created_at                                                │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│                  webhook_deliveries (new)                             │
│  id (PK)                                                              │
│  webhook_id (FK webhook_subscriptions, CASCADE)                       │
│  event_type, payload (JSON)                                           │
│  response_status, response_body                                       │
│  attempt (default 1), status (pending|delivered|failed|retrying)     │
│  delivered_at, next_retry_at                                          │
│  created_at                                                           │
│  INDEX: (status, next_retry_at)                                       │
└──────────────────────────────────────────────────────────────────────┘

Existing tables used:
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│ students         │  │ attendance       │  │ marks            │
│ (existing)       │  │ (existing)       │  │ (existing)       │
│ student data     │  │ attendance data  │  │ marks data       │
└──────────────────┘  └──────────────────┘  └──────────────────┘
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│ fees             │  │ classes          │  │ audit_logs       │
│ (existing)       │  │ (existing)       │  │ (existing)       │
│ fee data         │  │ class data       │  │ audit logs       │
└──────────────────┘  └──────────────────┘  └──────────────────┘
```

---

## Appendix C: Event Flow

### Domain Events

| Event | Emitter | Consumers | Payload | Side Effects |
|---|---|---|---|---|
| `ApiKeyGenerated` | `ApiKeyService.generateKey()` | `NotificationService` (admin) | `keyId, name, scopes, schoolId` | In-app to admin |
| `ApiKeyRevoked` | `ApiKeyService.revoke()` | `NotificationService` (admin) | `keyId, name, schoolId` | In-app to admin |
| `ApiKeyExpired` | `ApiKeyExpiryJob` | `NotificationService` (admin) | `keyId, name, schoolId` | In-app + email to admin |
| `WebhookRegistered` | `WebhookService.registerWebhook()` | None (logged) | `webhookId, url, events, schoolId` | Audit log |
| `WebhookDelivered` | `WebhookService` (on success) | None (logged) | `deliveryId, webhookId, eventType` | Audit log |
| `WebhookFailed` | `WebhookService` (after 3 retries) | `NotificationService` (admin) | `deliveryId, webhookId, eventType, url` | In-app + email to admin |
| `ApiCallLogged` | `ApiKeyAuthMiddleware` (async) | None (logged) | `keyId, endpoint, method, status` | Audit log |

### Event Triggers (Webhook Events)

| Event | Trigger | Payload |
|---|---|---|
| `student.enrolled` | New enrollment created | `{ student: StudentDto, classId, className }` |
| `student.withdrawn` | Enrollment deactivated | `{ student: StudentDto, withdrawnAt }` |
| `attendance.marked` | Attendance submitted for class | `{ classId, date, totalStudents, present, absent }` |
| `marks.published` | Assessment marks published | `{ classId, subjectId, assessmentName, studentCount }` |
| `fee.paid` | Fee payment received | `{ studentId, amount, paymentDate, receiptNumber }` |
| `fee.overdue` | Fee becomes overdue | `{ studentId, amount, dueDate, daysOverdue }` |
| `announcement.published` | Announcement sent | `{ announcementId, title, audience, createdAt }` |

### Event Delivery Guarantees

- API key generation/revocation: synchronous, notification async
- Webhook trigger: synchronous (delivery creation), async (HTTP POST)
- Webhook retry: async (background job)
- Audit logging: async (non-blocking)

---

## Appendix D: Configuration

### Environment Variables

| Variable | Default | Description |
|---|---|---|
| `OPEN_API_ENABLED` | `false` | Enable/disable open API |
| `OPEN_API_RATE_LIMIT_DEFAULT` | `1000` | Default rate limit per hour |
| `OPEN_API_RATE_LIMIT_BURST` | `50` | Max burst (requests per second) |
| `OPEN_API_WEBHOOK_TIMEOUT_SECONDS` | `10` | HTTP POST timeout |
| `OPEN_API_WEBHOOK_MAX_RETRIES` | `3` | Max retry attempts |
| `OPEN_API_WEBHOOK_RETRY_DELAYS` | `1,5,15` | Retry delays in minutes |
| `OPEN_API_SANDBOX_ENABLED` | `true` | Enable sandbox mode |
| `OPEN_API_SWAGGER_ENABLED` | `true` | Enable Swagger UI |
| `OPEN_API_KEY_EXPIRY_DAYS` | `365` | Default key expiry (if not specified) |
| `OPEN_API_DELIVERY_RETENTION_DAYS` | `30` | Days to keep delivered webhook deliveries |

### Feature Flags

| Flag | Default | Description |
|---|---|---|
| `OPEN_API_ENABLED` | `false` | Enable/disable open API |
| `OPEN_API_SWAGGER_ENABLED` | `true` | Swagger UI |
| `OPEN_API_SANDBOX_ENABLED` | `true` | Sandbox mode |
| `OPEN_API_WEBHOOKS_ENABLED` | `true` | Webhook delivery |

### School-Level Settings

| Setting | Default | Description |
|---|---|---|
| `open_api_enabled` | `false` | Per-school enable/disable |
| `open_api_webhooks_enabled` | `true` | Per-school webhooks |
| `open_api_sandbox_enabled` | `true` | Per-school sandbox |

---

## Appendix E: Migration & Rollback

### Migration: `migration_092_open_api.sql`

```sql
-- Migration 092: Open API & Third-Party Integrations
-- Creates api_keys, webhook_subscriptions, webhook_deliveries tables

BEGIN;

-- Create api_keys table
CREATE TABLE IF NOT EXISTS api_keys (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    name            TEXT NOT NULL,
    key_hash        TEXT NOT NULL,
    key_prefix      VARCHAR(8) NOT NULL,
    scopes          TEXT NOT NULL DEFAULT '[]',
    rate_limit_per_hour INTEGER NOT NULL DEFAULT 1000,
    is_sandbox      BOOLEAN NOT NULL DEFAULT false,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    expires_at      TIMESTAMP,
    last_used_at    TIMESTAMP,
    created_by      UUID,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    revoked_at      TIMESTAMP,
    revoked_by      UUID
);

CREATE INDEX IF NOT EXISTS idx_api_keys_school
    ON api_keys (school_id, is_active);

-- Create webhook_subscriptions table
CREATE TABLE IF NOT EXISTS webhook_subscriptions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    api_key_id      UUID NOT NULL REFERENCES api_keys(id),
    url             TEXT NOT NULL,
    events          TEXT NOT NULL DEFAULT '[]',
    secret          TEXT NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

-- Create webhook_deliveries table
CREATE TABLE IF NOT EXISTS webhook_deliveries (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    webhook_id      UUID NOT NULL REFERENCES webhook_subscriptions(id) ON DELETE CASCADE,
    event_type      VARCHAR(48) NOT NULL,
    payload         TEXT NOT NULL,
    response_status INTEGER,
    response_body   TEXT,
    attempt         INTEGER NOT NULL DEFAULT 1,
    status          VARCHAR(16) NOT NULL DEFAULT 'pending',
    delivered_at    TIMESTAMP,
    next_retry_at   TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_webhook_deliveries_pending
    ON webhook_deliveries (status, next_retry_at);

COMMIT;
```

### Rollback: `migration_092_rollback.sql`

```sql
BEGIN;
DROP TABLE IF EXISTS webhook_deliveries;
DROP TABLE IF EXISTS webhook_subscriptions;
DROP TABLE IF EXISTS api_keys;
COMMIT;
```

### Migration Validation

- Verify `api_keys` table created with index
- Verify `webhook_subscriptions` table created with FK to `api_keys`
- Verify `webhook_deliveries` table created with FK to `webhook_subscriptions` (CASCADE)
- Verify `webhook_deliveries` has index on (status, next_retry_at)
- Run `SELECT count(*) FROM api_keys` — should be 0 (new feature)

---

## Appendix F: Observability

### Structured Logging

| Log Level | Event | Context Fields |
|---|---|---|
| INFO | API key generated | `keyId, name, schoolId, scopes, createdBy` |
| INFO | API key revoked | `keyId, name, schoolId, revokedBy` |
| INFO | API key expired | `keyId, name, schoolId` |
| INFO | Webhook registered | `webhookId, url, events, schoolId` |
| INFO | Webhook delivered | `deliveryId, webhookId, eventType, responseStatus` |
| INFO | API call | `keyId, endpoint, method, responseStatus, durationMs` |
| WARN | Rate limit exceeded | `keyId, endpoint, remaining` |
| WARN | Webhook delivery failed | `deliveryId, webhookId, responseStatus, attempt` |
| WARN | Webhook retry scheduled | `deliveryId, nextRetryAt, attempt` |
| WARN | Scope not authorized | `keyId, endpoint, requiredScope` |
| ERROR | Webhook delivery failed (all retries) | `deliveryId, webhookId, url` |
| ERROR | API key validation error | `keyPrefix, error` |
| ERROR | Swagger generation failed | `error` |

### Metrics

| Metric | Type | Labels | Description |
|---|---|---|---|
| `open_api_requests_total` | Counter | `school_id, endpoint, status` | Total API requests |
| `open_api_rate_limit_hits` | Counter | `school_id, key_id` | Rate limit 429 responses |
| `open_api_key_count` | Gauge | `school_id` | Active API keys per school |
| `open_api_webhook_deliveries` | Counter | `school_id, event_type, status` | Webhook deliveries by status |
| `open_api_webhook_retry_count` | Counter | `school_id, attempt` | Webhook retry attempts |
| `open_api_response_duration` | Histogram | `school_id, endpoint` | API response time |
| `open_api_active_keys` | Gauge | `school_id` | Active API keys |
| `open_api_sandbox_keys` | Gauge | `school_id` | Sandbox API keys |

### Health Checks

| Check | Endpoint | Description |
|---|---|---|
| Open API service | `/health/open-api` | Verify service and DB accessible |
| Rate limiter | `/health/rate-limiter` | Verify rate limiter operational |
| Swagger | `/health/swagger` | Verify Swagger UI accessible |

### Alerts

| Alert | Condition | Severity | Notification |
|---|---|---|---|
| High rate limit hits | > 100 429s in 1 hour per key | Warning | Email to school admin |
| Webhook delivery failures | > 10 failed deliveries in 1 hour | Warning | Email to school admin |
| API key validation errors | > 50 errors in 1 hour | Warning | Email to dev team |
| Swagger UI unavailable | Health check failed | Warning | Email to dev team |
| Rate limiter unavailable | Health check failed | Critical | Email + SMS to dev team |

### Dashboards

| Dashboard | Panels | Audience |
|---|---|---|
| API Overview | Request count, response time, error rate | DevOps Team |
| API Keys | Active keys, by school, by scope | Product Team |
| Webhooks | Delivery rate, retry count, failures | DevOps Team |
| Rate Limiting | 429 rate, by key, by school | DevOps Team |

### Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| API key leakage | Medium | High | Bcrypt-hashed at rest. Shown once. 2FA for generation. Admin can revoke. |
| Rate limit bypass | Low | Medium | Sliding window per key. In-memory (fast). Burst limit. |
| Webhook URL spoofing | Low | Medium | HMAC signature verification. HTTPS required. |
| Data exfiltration | Medium | High | Scoped permissions. Read-only by default. Audit logging. School-scoped. |
| Sandbox data exposure | Low | Low | Sandbox returns mock data. No real data. |
| API abuse | Medium | Medium | Rate limiting. Scope restrictions. Audit logging. Admin monitoring. |
| Webhook delivery backlog | Low | Medium | Retry job every 5 min. Max 3 retries. Admin notified on failures. |
