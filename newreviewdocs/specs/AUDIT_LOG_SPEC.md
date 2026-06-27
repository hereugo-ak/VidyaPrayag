# Audit Log — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Audience:** Senior engineer / AI agent implementing the system
> **Last updated:** 2026-06-27
> **Prerequisites:** None
> **Unblocks:** `DPDP_COMPLIANCE_SPEC.md`, all admin-action features

---

## Table of Contents

1. [Feature Overview](#1-feature-overview)
2. [Current System Assessment](#2-current-system-assessment)
3. [Gap Analysis](#3-gap-analysis)
4. [Functional Requirements](#4-functional-requirements)
5. [User Roles & Permissions](#5-user-roles--permissions)
6. [Database Design](#6-database-design)
7. [Backend Architecture](#7-backend-architecture)
8. [API Contracts](#8-api-contracts)
9. [Security](#9-security)
10. [Performance Considerations](#10-performance-considerations)
11. [Monitoring](#11-monitoring)
12. [Migration Strategy](#12-migration-strategy)
13. [Testing Strategy](#13-testing-strategy)
14. [Acceptance Criteria](#14-acceptance-criteria)
15. [Implementation Roadmap](#15-implementation-roadmap)
16. [File-Level Impact Analysis](#16-file-level-impact-analysis)
17. [Risks & Mitigations](#17-risks--mitigations)

---

## 1. Feature Overview

A centralized audit logging system that records all administrative actions, data access events, and configuration changes across the Vidya Prayag platform. This is a foundational infrastructure component required for DPDP Act compliance, security forensics, and operational accountability.

### Goals

- Record every write operation performed by school admins, teachers, and system processes
- Provide queryable, filterable audit trail with role-based access
- Support data export for compliance audits
- Enable tamper-evident logging (append-only, no UPDATE/DELETE)
- Support retention policies (configurable per school)

---

## 2. Current System Assessment

### 2.1 What Exists

- **No audit log table** — `Tables.kt` has no audit-related table
- `NotificationsTable` records notification events but not user actions
- `OtpDeliveryAttemptsTable` is the closest existing pattern — a full audit trail of OTP delivery attempts with provider, status, latency, and raw response
- `UserSessionsTable` tracks session issuance/revocation but not actions within sessions
- Some routes log to server console via `call.application.log` but this is ephemeral

### 2.2 What's Missing

- No persistent action log
- No query interface for audit data
- No retention management
- No correlation between actions and affected entities

---

## 3. Gap Analysis

| # | Gap | Impact |
|---|---|---|
| G1 | No audit trail for admin actions | Cannot investigate data changes, security incidents |
| G2 | No DPDP compliance evidence | Legal liability |
| G3 | No data access logging | Cannot prove who accessed student/parent data |
| G4 | No configuration change tracking | Silent regressions untraceable |
| G5 | No export for regulatory audits | Manual evidence gathering |

---

## 4. Functional Requirements

### 4.1 Core Requirements

| ID | Requirement |
|---|---|
| FR-1 | Log every CREATE/UPDATE/DELETE operation on school-scoped data |
| FR-2 | Capture actor (user_id, role), action type, entity type, entity ID, before/after diff |
| FR-3 | Capture request metadata: IP, user agent, device ID, timestamp |
| FR-4 | Provide paginated, filterable query API (by actor, entity, action, date range) |
| FR-5 | Export audit log as CSV/JSON for compliance |
| FR-6 | Enforce append-only — no UPDATE or DELETE on audit rows |
| FR-7 | Configurable retention period per school (default 365 days) |
| FR-8 | Automatic purge of records older than retention period (scheduled job) |

### 4.2 Action Types

```
CREATE | UPDATE | DELETE | LOGIN | LOGOUT | LOGIN_FAILED | EXPORT | CONFIG_CHANGE |
PERMISSION_CHANGE | BULK_IMPORT | BULK_DELETE | DATA_ACCESS
```

### 4.3 Entity Types

```
student | teacher | parent | class | subject | announcement | attendance_record |
assessment | homework | fee_record | leave_request | calendar_event | school_config |
message | scholarship | enrollment | onboarding | child_link | non_teaching_staff
```

---

## 5. User Roles & Permissions

| Action | Super Admin | School Admin | Teacher | Parent |
|---|---|---|---|---|
| View own school's audit log | ✅ | ✅ | ❌ | ❌ |
| Export audit log | ✅ | ✅ | ❌ | ❌ |
| View all schools' audit log | ✅ | ❌ | ❌ | ❌ |
| View own action history | ✅ | ✅ | ✅ | ✅ |

---

## 6. Database Design

### 6.1 New Table: `audit_logs`

```sql
CREATE TABLE audit_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    actor_id        UUID,                          -- FK app_users.id (null for system actions)
    actor_role      VARCHAR(32) NOT NULL,          -- school_admin | teacher | parent | system
    actor_name      TEXT,                          -- denormalized for display
    action          VARCHAR(32) NOT NULL,          -- CREATE | UPDATE | DELETE | ...
    entity_type     VARCHAR(48) NOT NULL,          -- student | teacher | ...
    entity_id       TEXT,                          -- UUID or natural key of affected entity
    entity_name     TEXT,                          -- human-readable entity label
    changes         TEXT,                          -- JSON diff: {"field": {"old": x, "new": y}}
    ip_address      TEXT,
    user_agent      TEXT,
    device_id       TEXT,
    request_path    TEXT,                          -- API endpoint that triggered the action
    request_method  VARCHAR(8),                    -- GET | POST | PATCH | DELETE
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_school_created ON audit_logs(school_id, created_at DESC);
CREATE INDEX idx_audit_actor ON audit_logs(actor_id, created_at DESC);
CREATE INDEX idx_audit_entity ON audit_logs(entity_type, entity_id, created_at DESC);
CREATE INDEX idx_audit_action ON audit_logs(school_id, action, created_at DESC);
```

### 6.2 Exposed Mapping

```kotlin
object AuditLogsTable : UUIDTable("audit_logs", "id") {
    val schoolId      = uuid("school_id")
    val actorId       = uuid("actor_id").nullable()
    val actorRole     = varchar("actor_role", 32)
    val actorName     = text("actor_name").nullable()
    val action        = varchar("action", 32)
    val entityType    = varchar("entity_type", 48)
    val entityId      = text("entity_id")
    val entityName    = text("entity_name").nullable()
    val changes       = text("changes").nullable()   // JSON diff
    val ipAddress     = text("ip_address").nullable()
    val userAgent     = text("user_agent").nullable()
    val deviceId      = text("device_id").nullable()
    val requestPath   = text("request_path").nullable()
    val requestMethod = varchar("request_method", 8).nullable()
    val createdAt     = timestamp("created_at")

    init {
        index("idx_audit_school_created", false, schoolId, createdAt)
        index("idx_audit_actor", false, actorId, createdAt)
        index("idx_audit_entity", false, entityType, entityId, createdAt)
        index("idx_audit_action", false, schoolId, action, createdAt)
    }
}
```

### 6.3 Retention Configuration

Add to `AppConfigTable`:
```
key: audit_retention_days
value: 365
```

---

## 7. Backend Architecture

### 7.1 AuditService

```kotlin
class AuditService {
    suspend fun log(
        schoolId: UUID,
        actorId: UUID?,
        actorRole: String,
        actorName: String?,
        action: String,
        entityType: String,
        entityId: String,
        entityName: String? = null,
        changes: String? = null,        // JSON diff
        request: ApplicationCall? = null
    )

    suspend fun query(
        schoolId: UUID,
        actorId: UUID? = null,
        action: String? = null,
        entityType: String? = null,
        entityId: String? = null,
        dateFrom: LocalDate? = null,
        dateTo: LocalDate? = null,
        offset: Int = 0,
        limit: Int = 50
    ): PaginatedResult<AuditLogDto>

    suspend fun export(
        schoolId: UUID,
        dateFrom: LocalDate,
        dateTo: LocalDate,
        format: ExportFormat
    ): ByteArray

    suspend fun purgeExpired(schoolId: UUID, retentionDays: Int): Int
}
```

### 7.2 AuditInterceptor (Ktor Plugin)

A custom Ktor plugin that intercepts all mutating requests (POST/PATCH/DELETE) to `/api/v1/school/*` and `/api/v1/teacher/*` routes and automatically logs them.

```kotlin
// Install in Application.kt
install(AuditLogging) {
    enabled = true
    excludedPaths = setOf("/api/v1/auth", "/api/v1/health")
    captureChanges = true
}
```

**Implementation approach:** The plugin wraps each route handler. Before the handler runs, it captures the "before" state (for UPDATE/DELETE). After the handler runs, it captures the "after" state and writes the audit log entry.

For routes that don't easily support before/after capture (e.g., bulk operations), the handler itself calls `AuditService.log()` directly.

### 7.3 Manual Audit Calls

For complex operations where automatic interception is insufficient:

```kotlin
auditService.log(
    schoolId = schoolId,
    actorId = userId,
    actorRole = "school_admin",
    actorName = userName,
    action = "CREATE",
    entityType = "student",
    entityId = student.id.toString(),
    entityName = student.fullName,
    changes = Json.encodeToString(student),
    request = call
)
```

### 7.4 Scheduled Purge Job

A daily cron job (using Ktor's scheduled tasks or a simple timer) that:
1. Reads `audit_retention_days` from `AppConfigTable`
2. `DELETE FROM audit_logs WHERE school_id = ? AND created_at < now() - INTERVAL '? days'`
3. Logs the purge count

---

## 8. API Contracts

### 8.1 Query Audit Log

```
GET /api/v1/school/audit-log?actor_id={uuid}&action={type}&entity_type={type}&entity_id={id}&date_from={YYYY-MM-DD}&date_to={YYYY-MM-DD}&offset=0&limit=50
```

**Response 200:**
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": "uuid",
        "actor_id": "uuid",
        "actor_name": "Rajesh Kumar",
        "actor_role": "school_admin",
        "action": "UPDATE",
        "entity_type": "student",
        "entity_id": "uuid",
        "entity_name": "Aarav Sharma",
        "changes": {"class_name": {"old": "Grade 5", "new": "Grade 6"}},
        "ip_address": "192.168.1.1",
        "request_path": "/api/v1/school/students/uuid",
        "request_method": "PATCH",
        "created_at": "2026-06-27T10:30:00Z"
      }
    ],
    "total_count": 1250,
    "has_more": true
  }
}
```

### 8.2 Export Audit Log

```
GET /api/v1/school/audit-log/export?date_from={YYYY-MM-DD}&date_to={YYYY-MM-DD}&format=csv|json
```

**Response 200:** File download (`Content-Type: text/csv` or `application/json`)

### 8.3 View Own Action History

```
GET /api/v1/{role}/audit-log/my-actions?offset=0&limit=50
```

Available to all roles. Returns only actions by the authenticated user.

### 8.4 Configure Retention

```
PATCH /api/v1/school/audit-log/retention
{
  "retention_days": 730
}
```

**Authorization:** Super Admin only.

---

## 9. Security

- Audit log is **append-only** — no UPDATE or DELETE except by the purge job
- School-scoped — queries filter by `school_id` from JWT
- Super admin can view all schools; school admin limited to own school
- Export requires explicit admin role
- IP and user agent captured for forensic analysis
- Audit log writes are **best-effort** — a failed audit write must NOT block the business operation (log error, continue)

---

## 10. Performance Considerations

- Audit writes are **async** (fire-and-forget via a queue or coroutine) to avoid adding latency to API responses
- `changes` JSON is capped at 10KB to prevent oversized payloads
- Indexes on `(school_id, created_at)` and `(actor_id, created_at)` cover the primary query patterns
- For high-volume schools (>10K actions/day), consider partitioning by month
- Export is streamed (not loaded into memory) for large date ranges
- Purge job runs during off-peak hours (2 AM IST)

---

## 11. Monitoring

| Metric | Type |
|---|---|
| `audit.writes_total` | Counter |
| `audit.write_failures_total` | Counter |
| `audit.query_latency_ms` | Histogram |
| `audit.export_duration_ms` | Histogram |
| `audit.purge_rows_deleted` | Gauge |

**Alerts:**
- Audit write failure rate > 1% → Warning
- Purge job not run in 24h → Warning

---

## 12. Migration Strategy

### 12.1 Migration File

`docs/db/migration_030_audit_logs.sql`

```sql
CREATE TABLE IF NOT EXISTS audit_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    actor_id        UUID,
    actor_role      VARCHAR(32) NOT NULL,
    actor_name      TEXT,
    action          VARCHAR(32) NOT NULL,
    entity_type     VARCHAR(48) NOT NULL,
    entity_id       TEXT,
    entity_name     TEXT,
    changes         TEXT,
    ip_address      TEXT,
    user_agent      TEXT,
    device_id       TEXT,
    request_path    TEXT,
    request_method  VARCHAR(8),
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_audit_school_created ON audit_logs(school_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_actor ON audit_logs(actor_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_entity ON audit_logs(entity_type, entity_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_action ON audit_logs(school_id, action, created_at DESC);

-- Default retention
INSERT INTO app_config (key, value, updated_at)
VALUES ('audit_retention_days', '365', now())
ON CONFLICT (key) DO NOTHING;

-- ROLLBACK:
-- DROP TABLE IF EXISTS audit_logs;
-- DELETE FROM app_config WHERE key = 'audit_retention_days';
```

### 12.2 Rollback

Drop the table and remove the config key. No data loss to business operations.

---

## 13. Testing Strategy

### 13.1 Unit Tests

- `AuditService.log()` — all fields captured correctly
- `AuditService.query()` — filtering by actor, action, entity, date range
- `AuditService.export()` — CSV and JSON format correctness
- `AuditService.purgeExpired()` — correct retention calculation
- Append-only enforcement — attempting UPDATE throws

### 13.2 Integration Tests

- Create student → audit log entry exists with action=CREATE
- Update student → audit log entry has before/after diff
- Delete student → audit log entry with action=DELETE
- Cross-school query → returns only own school's entries
- Teacher querying school audit log → 403
- Export → valid CSV with correct headers and row count
- Purge job → records older than retention are deleted

---

## 14. Acceptance Criteria

- [ ] Every POST/PATCH/DELETE to `/api/v1/school/*` generates an audit log entry
- [ ] Audit log entries include actor, action, entity, changes, IP, timestamp
- [ ] School admin can query and filter audit log
- [ ] School admin can export audit log as CSV
- [ ] Teacher/parent can view their own action history
- [ ] Retention period is configurable
- [ ] Purge job runs daily and removes expired records
- [ ] Audit writes do not add > 5ms to API response time (async)
- [ ] Cross-school access is prevented

---

## 15. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 2 days | DB migration, Exposed table, AuditService |
| 2 | 3 days | Ktor AuditLogging plugin, integrate into existing school/teacher routes |
| 3 | 2 days | Query API + export endpoint |
| 4 | 1 day | Purge job + retention config |
| 5 | 2 days | Client UI (audit log screen with filters + export button) |
| 6 | 2 days | Tests (unit + integration) |

---

## 16. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | `AuditLogsTable` object |
| `server/.../db/DatabaseFactory.kt` | Modify | Register `AuditLogsTable` in `allTables` |
| `server/.../feature/audit/AuditService.kt` | New | Core audit service |
| `server/.../feature/audit/AuditRouting.kt` | New | Query/export endpoints |
| `server/.../feature/audit/AuditLoggingPlugin.kt` | New | Ktor plugin for automatic logging |
| `server/.../Application.kt` | Modify | Install `AuditLogging` plugin |
| `server/.../feature/school/*.kt` | Modify | Add manual audit calls where auto-intercept is insufficient |
| `docs/db/migration_030_audit_logs.sql` | New | DDL migration |
| `shared/.../feature/audit/AuditApi.kt` | New | Client API |
| `shared/.../feature/audit/AuditRepository.kt` | New | Client repository |
| `composeApp/.../ui/v2/screens/admin/AuditLogScreen.kt` | New | Admin audit log screen |
| `composeApp/.../ui/v2/screens/admin/AuditLogViewModel.kt` | New | ViewModel |

---

## 17. Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Audit writes slow down API responses | Medium | Medium | Async writes via coroutine; fire-and-forget |
| Audit table grows unbounded | High | Medium | Daily purge job; configurable retention |
| Before/after diff capture fails | Medium | Low | Best-effort; log error, continue with null changes |
| Manual audit calls forgotten in new routes | Medium | Medium | Code review checklist; plugin handles most cases automatically |
| Export of large date range OOMs | Low | High | Stream export; chunk by day if > 10K rows |
