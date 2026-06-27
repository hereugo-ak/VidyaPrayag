# Open API & Third-Party Integrations — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** `TWO_FACTOR_AUTH_SPEC.md`, `AUDIT_LOG_SPEC.md`
> **Source:** `COMPETITIVE_GAP_ANALYSIS.md` — integration ecosystem

---

## 1. Feature Overview

Public REST API for third-party integrations: enables other EdTech platforms, government portals (UDISE+), and custom school workflows to integrate with Vidya Prayag. Includes API key management, rate limiting, webhooks, and developer documentation.

### Goals

- API key management: admin generates API keys with scoped permissions
- REST API endpoints for core entities (students, attendance, marks, fees, classes)
- Rate limiting per API key (1000 req/hour default, configurable)
- Webhooks: notify third-party systems on events (student enrolled, fee paid, marks published)
- API documentation (OpenAPI/Swagger)
- Sandbox environment for testing
- Audit logging for all API calls

---

## 2. Current System Assessment

- All existing endpoints are JWT-authenticated (user-specific)
- No API key authentication or public API
- `AUDIT_LOG_SPEC.md` — audit logging infrastructure (reusable)
- `TWO_FACTOR_AUTH_SPEC.md` — security baseline
- `COMPETITIVE_GAP_ANALYSIS.md`: open API as integration ecosystem trend

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Admin generates API key with: name, scopes (entities accessible), rate limit, expiry |
| FR-2 | API key authentication: `Authorization: Bearer api_key` header |
| FR-3 | Scoped permissions: read-only by default, write per scope |
| FR-4 | Rate limiting: per API key (configurable, default 1000 req/hour) |
| FR-5 | Webhooks: register URL → receive POST on events (student.enrolled, fee.paid, marks.published, attendance.marked) |
| FR-6 | Webhook retry: 3 retries with exponential backoff on failure |
| FR-7 | OpenAPI/Swagger documentation auto-generated |
| FR-8 | Sandbox: test API keys for development |
| FR-9 | All API calls logged in audit log |
| FR-10 | API key revocation |

---

## 4. Database Design

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

---

## 5. Backend Architecture

### 5.1 ApiKeyAuthMiddleware

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

### 5.2 OpenApiEndpoints

```kotlin
// /api/v1/open/ namespace for all public API endpoints
route("/api/v1/open") {
    // Students
    get("/students") { /* list with pagination */ }
    get("/students/{id}") { /* get by ID */ }

    // Attendance
    get("/attendance") { /* query by class, date range */ }

    // Marks
    get("/marks") { /* query by class, term, subject */ }

    // Fees
    get("/fees") { /* query by status, date range */ }

    // Classes
    get("/classes") { /* list classes */ }

    // All scoped to school_id from API key
    // All read-only unless scope includes write
}
```

### 5.3 WebhookService

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
}
```

### 5.4 Event Triggers

Events that trigger webhooks:
- `student.enrolled` — new enrollment created
- `student.withdrawn` — enrollment deactivated
- `attendance.marked` — attendance submitted for a class
- `marks.published` — assessment marks published
- `fee.paid` — fee payment received
- `fee.overdue` — fee becomes overdue
- `announcement.published` — announcement sent

### 5.5 Swagger/OpenAPI

```kotlin
// Using ktor-swagger plugin or custom OpenAPI generation
install(SwaggerSupport) {
    info {
        title = "Vidya Prayag Open API"
        version = "1.0"
        description = "REST API for third-party integrations"
    }
    // Auto-generates /api/v1/open/swagger.json
    // Served at /api/v1/open/docs (Swagger UI)
}
```

---

## 6. API Contracts

### 6.1 API Key Management

```
POST /api/v1/school/api-keys  { name, scopes, rate_limit_per_hour, expires_at }
GET /api/v1/school/api-keys
DELETE /api/v1/school/api-keys/{id}  -- revoke
```

### 6.2 Open API Endpoints

```
GET /api/v1/open/students?page=1&limit=50&class_id={uuid}
GET /api/v1/open/students/{id}
GET /api/v1/open/attendance?class_id={uuid}&from={YYYY-MM-DD}&to={YYYY-MM-DD}
GET /api/v1/open/marks?class_id={uuid}&term=term1&subject_id={uuid}
GET /api/v1/open/fees?status=OVERDUE&from={}&to={}
GET /api/v1/open/classes
```

### 6.3 Webhook Management

```
POST /api/v1/school/webhooks  { url, events: [...] }
GET /api/v1/school/webhooks
DELETE /api/v1/school/webhooks/{id}
GET /api/v1/school/webhooks/{id}/deliveries?status=failed
```

---

## 7. Security

- API keys are bcrypt-hashed at rest (never stored in plaintext)
- Key format: `vp_` prefix + 32 random alphanumeric chars
- All API calls logged in `audit_logs` (from `AUDIT_LOG_SPEC.md`)
- Rate limiting per key (sliding window)
- Webhook payloads HMAC-signed with subscription secret
- HTTPS only
- Sandbox keys have `is_sandbox=true` → return mock data, no real mutations

---

## 8. Acceptance Criteria

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

---

## 9. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 2 days | DB migration, Exposed tables |
| 2 | 2 days | ApiKeyAuthMiddleware + rate limiter |
| 3 | 3 days | Open API endpoints (students, attendance, marks, fees, classes) |
| 4 | 2 days | WebhookService (trigger, deliver, retry) |
| 5 | 2 days | Swagger/OpenAPI documentation |
| 6 | 2 days | API key management + webhook management endpoints |
| 7 | 2 days | Client UI (API key management, webhook config, delivery logs) |
| 8 | 2 days | Tests |

---

## 10. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | 3 API/webhook tables |
| `server/.../feature/openapi/ApiKeyService.kt` | New | API key management |
| `server/.../feature/openapi/ApiKeyAuthMiddleware.kt` | New | Auth middleware |
| `server/.../feature/openapi/OpenApiRouting.kt` | New | Public API endpoints |
| `server/.../feature/openapi/WebhookService.kt` | New | Webhook delivery + retry |
| `server/.../feature/openapi/SwaggerConfig.kt` | New | OpenAPI docs |
| `docs/db/migration_091_open_api.sql` | New | DDL |
| `composeApp/.../ui/v2/screens/admin/ApiManagementScreen.kt` | New | API key + webhook UI |
