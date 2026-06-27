# DPDP Act Compliance — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Audience:** Senior engineer / AI agent implementing the system
> **Last updated:** 2026-06-27
> **Prerequisites:** `AUDIT_LOG_SPEC.md`
> **Unblocks:** Data export/erasure workflows across all features

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
10. [Validation Rules](#10-validation-rules)
11. [Error Handling](#11-error-handling)
12. [Edge Cases](#12-edge-cases)
13. [Background Processing](#13-background-processing)
14. [Monitoring](#14-monitoring)
15. [Feature Flags](#15-feature-flags)
16. [Migration Strategy](#16-migration-strategy)
17. [Testing Strategy](#17-testing-strategy)
18. [Acceptance Criteria](#18-acceptance-criteria)
19. [Implementation Roadmap](#19-implementation-roadmap)
20. [File-Level Impact Analysis](#20-file-level-impact-analysis)
21. [Risks & Mitigations](#21-risks--mitigations)

---

## 1. Feature Overview

Implementation of India's Digital Personal Data Protection (DPDP) Act 2023 compliance features: consent management, data subject rights (access, correction, erasure), data breach notification, privacy policy tracking, and data processing records.

### Goals

- Record and manage consent for data processing per parent/student
- Enable data subject access requests (DSAR) — export all personal data
- Enable data correction requests
- Enable data erasure requests (right to be forgotten)
- Maintain data processing records for regulatory audit
- Data breach notification workflow
- Privacy policy versioning and acceptance tracking

---

## 2. Current System Assessment

### 2.1 What Exists

- **`ParentChildLinksTable`** — tracks parent→child link approval (consent-like, but not DPDP consent)
- **`AppUsersTable`** — has `isPhoneVerified`, `isEmailVerified` (verification, not consent)
- **`NotificationPreferencesTable`** — per-category opt-out (partial consent management)
- **`AuditLogsTable`** (from `AUDIT_LOG_SPEC.md`) — action logging (reusable for DPDP audit)
- No consent table, no DSAR workflow, no data processing records
- No privacy policy tracking

### 2.2 Data Categories in Vidya Prayag

| Category | Tables | DPDP Sensitivity |
|---|---|---|
| Identity | `app_users` (name, phone, email) | Personal |
| Student records | `students`, `children`, `enrollments` | Personal + Sensitive (child data) |
| Academic | `attendance_records`, `assessments`, `assessment_marks`, `homework` | Sensitive |
| Financial | `fee_records`, `payments` (with fee payment spec) | Sensitive |
| Communication | `messages`, `message_threads`, `whatsapp_messages` | Personal |
| Health | `health_records` (future) | Sensitive (special category) |
| Device | `device_tokens`, `user_sessions` | Personal |

---

## 3. Gap Analysis

| # | Gap | DPDP Section | Impact |
|---|---|---|---|
| G1 | No consent management | §4-§6 | Non-compliant processing |
| G2 | No DSAR (data export) | §11 | Cannot fulfill data access requests |
| G3 | No erasure workflow | §11(b) | Cannot fulfill right to erasure |
| G4 | No data processing records | §8(5) | Cannot demonstrate compliance |
| G5 | No privacy policy versioning | §5 | Cannot prove consent was informed |
| G6 | No breach notification | §8(6) | Non-compliance with 72h notification rule |
| G7 | No data retention policies | §8(5) | Indefinite retention non-compliant |

---

## 4. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Record consent per data category per user (explicit, timestamped, withdrawable) |
| FR-2 | Privacy policy versioning — each consent linked to policy version |
| FR-3 | Parent can view all data categories and consent status |
| FR-4 | Parent can withdraw consent per category (triggers data processing stop) |
| FR-5 | Data Subject Access Request (DSAR): export all personal data as structured JSON/CSV |
| FR-6 | Data correction request: parent submits correction, admin approves |
| FR-7 | Data erasure request: parent requests deletion, admin processes with retention checks |
| FR-8 | Data processing records: log purpose, legal basis, data categories, recipients |
| FR-9 | Data breach notification workflow: detect, log, notify DPA within 72 hours |
| FR-10 | Data retention policies per category with automatic purge |
| FR-11 | Consent re-confirmation when policy changes |

---

## 5. User Roles & Permissions

| Action | Parent | School Admin | Super Admin (DPO) |
|---|---|---|---|
| View own consent status | ✅ | N/A | ✅ |
| Grant/withdraw consent | ✅ | N/A | N/A |
| Submit DSAR | ✅ | ✅ | ✅ |
| Submit correction request | ✅ | ✅ | ✅ |
| Submit erasure request | ✅ | ✅ | ✅ |
| Process DSAR/correction/erasure | ❌ | ✅ | ✅ |
| View data processing records | ❌ | ✅ | ✅ |
| Manage privacy policy | ❌ | ❌ | ✅ |
| Manage breach notification | ❌ | ❌ | ✅ |

---

## 6. Database Design

### 6.1 New Table: `privacy_policies`

```sql
CREATE TABLE privacy_policies (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version         VARCHAR(16) NOT NULL,          -- "1.0", "1.1", "2.0"
    title           TEXT NOT NULL,
    content         TEXT NOT NULL,                 -- full policy text (markdown)
    effective_date  DATE NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
```

### 6.2 New Table: `data_consents`

```sql
CREATE TABLE data_consents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,                 -- FK app_users.id
    school_id       UUID NOT NULL,
    data_category   VARCHAR(32) NOT NULL,          -- identity | academic | financial | communication | health | device | marketing
    consent_status  VARCHAR(16) NOT NULL,          -- granted | withdrawn | pending
    privacy_policy_id UUID NOT NULL REFERENCES privacy_policies(id),
    granted_at      TIMESTAMP,
    withdrawn_at    TIMESTAMP,
    ip_address      TEXT,
    user_agent      TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(user_id, school_id, data_category)
);
CREATE INDEX idx_consents_user ON data_consents(user_id, consent_status);
```

### 6.3 New Table: `data_subject_requests`

```sql
CREATE TABLE data_subject_requests (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,
    school_id       UUID NOT NULL,
    request_type    VARCHAR(16) NOT NULL,          -- access | correction | erasure
    status          VARCHAR(16) NOT NULL DEFAULT 'submitted', -- submitted | under_review | completed | rejected
    description     TEXT,                          -- user's description of request
    correction_data TEXT,                          -- JSON: {"field": "new_value"} for correction requests
    export_url      TEXT,                          -- Supabase Storage URL for DSAR export
    processed_by    UUID,                          -- admin who processed
    processed_at    TIMESTAMP,
    rejection_reason TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_dsr_school_status ON data_subject_requests(school_id, status, created_at DESC);
```

### 6.4 New Table: `data_processing_records`

```sql
CREATE TABLE data_processing_records (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    purpose         VARCHAR(64) NOT NULL,          -- admission | attendance_tracking | fee_collection | communication | exam_assessment | transport | health_monitoring
    legal_basis     VARCHAR(32) NOT NULL,          -- consent | legitimate_interest | legal_obligation | vital_interest
    data_categories TEXT NOT NULL,                 -- JSON array: ["identity", "academic"]
    data_subjects   VARCHAR(32) NOT NULL,          -- students | parents | staff | all
    recipients      TEXT,                          -- JSON array of third-party recipients: [{"name":"Razorpay", "purpose":"payment"}]
    retention_period_days INTEGER NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);
```

### 6.5 New Table: `data_breach_incidents`

```sql
CREATE TABLE data_breach_incidents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID,
    title           TEXT NOT NULL,
    description     TEXT NOT NULL,
    severity        VARCHAR(16) NOT NULL,          -- low | medium | high | critical
    affected_categories TEXT,                      -- JSON array of data categories
    affected_count  INTEGER,
    detected_at     TIMESTAMP NOT NULL,
    reported_at     TIMESTAMP,                     -- when reported to DPA
    dpa_notified    BOOLEAN NOT NULL DEFAULT false,
    subjects_notified BOOLEAN NOT NULL DEFAULT false,
    status          VARCHAR(16) NOT NULL DEFAULT 'open', -- open | contained | resolved | reported
    resolution      TEXT,
    created_by      UUID,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);
```

### 6.6 Exposed Mappings

```kotlin
object PrivacyPoliciesTable : UUIDTable("privacy_policies", "id") {
    val version        = varchar("version", 16)
    val title          = text("title")
    val content        = text("content")
    val effectiveDate  = date("effective_date")
    val isActive       = bool("is_active").default(true)
    val createdAt      = timestamp("created_at")
}

object DataConsentsTable : UUIDTable("data_consents", "id") {
    val userId         = uuid("user_id")
    val schoolId       = uuid("school_id")
    val dataCategory   = varchar("data_category", 32)
    val consentStatus  = varchar("consent_status", 16)
    val privacyPolicyId = uuid("privacy_policy_id")
    val grantedAt      = timestamp("granted_at").nullable()
    val withdrawnAt    = timestamp("withdrawn_at").nullable()
    val ipAddress      = text("ip_address").nullable()
    val userAgent      = text("user_agent").nullable()
    val createdAt      = timestamp("created_at")
    val updatedAt      = timestamp("updated_at")
    init {
        uniqueIndex("ux_consents_user_school_cat", userId, schoolId, dataCategory)
        index("idx_consents_user", false, userId, consentStatus)
    }
}

object DataSubjectRequestsTable : UUIDTable("data_subject_requests", "id") {
    val userId         = uuid("user_id")
    val schoolId       = uuid("school_id")
    val requestType    = varchar("request_type", 16)
    val status         = varchar("status", 16).default("submitted")
    val description    = text("description").nullable()
    val correctionData = text("correction_data").nullable()
    val exportUrl      = text("export_url").nullable()
    val processedBy    = uuid("processed_by").nullable()
    val processedAt    = timestamp("processed_at").nullable()
    val rejectionReason = text("rejection_reason").nullable()
    val createdAt      = timestamp("created_at")
    val updatedAt      = timestamp("updated_at")
    init { index("idx_dsr_school_status", false, schoolId, status, createdAt) }
}

object DataProcessingRecordsTable : UUIDTable("data_processing_records", "id") {
    val schoolId       = uuid("school_id")
    val purpose        = varchar("purpose", 64)
    val legalBasis     = varchar("legal_basis", 32)
    val dataCategories = text("data_categories")
    val dataSubjects   = varchar("data_subjects", 32)
    val recipients     = text("recipients").nullable()
    val retentionPeriodDays = integer("retention_period_days")
    val isActive       = bool("is_active").default(true)
    val createdAt      = timestamp("created_at")
    val updatedAt      = timestamp("updated_at")
}

object DataBreachIncidentsTable : UUIDTable("data_breach_incidents", "id") {
    val schoolId       = uuid("school_id").nullable()
    val title          = text("title")
    val description    = text("description")
    val severity       = varchar("severity", 16)
    val affectedCategories = text("affected_categories").nullable()
    val affectedCount  = integer("affected_count").nullable()
    val detectedAt     = timestamp("detected_at")
    val reportedAt     = timestamp("reported_at").nullable()
    val dpaNotified    = bool("dpa_notified").default(false)
    val subjectsNotified = bool("subjects_notified").default(false)
    val status         = varchar("status", 16).default("open")
    val resolution     = text("resolution").nullable()
    val createdBy      = uuid("created_by").nullable()
    val createdAt      = timestamp("created_at")
    val updatedAt      = timestamp("updated_at")
}
```

---

## 7. Backend Architecture

### 7.1 ConsentService

```kotlin
class ConsentService {
    suspend fun grantConsent(userId: UUID, schoolId: UUID, category: String, request: ApplicationCall)
    suspend fun withdrawConsent(userId: UUID, schoolId: UUID, category: String)
    suspend fun getConsents(userId: UUID, schoolId: UUID): List<ConsentDto>
    suspend fun checkConsent(userId: UUID, schoolId: UUID, category: String): Boolean
    suspend fun reconfirmOnPolicyChange(schoolId: UUID, newPolicyId: UUID): Int  // returns affected users
}
```

**Consent withdrawal triggers:**
- `communication` withdrawn → disable all notifications for user
- `marketing` withdrawn → disable announcement notifications
- `device` withdrawn → delete all device tokens for user
- `academic` withdrawn → restrict parent from viewing child's academic data (requires admin review)

### 7.2 DataSubjectRequestService

```kotlin
class DataSubjectRequestService {
    suspend fun submitAccessRequest(userId: UUID, schoolId: UUID): UUID
    suspend fun submitCorrectionRequest(userId: UUID, schoolId: UUID, corrections: Map<String, String>): UUID
    suspend fun submitErasureRequest(userId: UUID, schoolId: UUID, reason: String): UUID
    suspend fun processAccessRequest(requestId: UUID, adminId: UUID): String  // returns export URL
    suspend fun processCorrectionRequest(requestId: UUID, adminId: UUID, approved: Boolean, reason: String?)
    suspend fun processErasureRequest(requestId: UUID, adminId: UUID, approved: Boolean, reason: String?)
    suspend fun getRequests(schoolId: UUID, status: String?): PaginatedResult<DsrDto>
}
```

### 7.3 DataExportService

Generates a complete personal data export for DSAR:

```kotlin
class DataExportService {
    suspend fun exportUserData(userId: UUID, schoolId: UUID): String {
        // 1. Collect data from all tables where user_id or parent_id = userId
        //    - app_users (own record)
        //    - children (parent's children)
        //    - students (linked via student_code)
        //    - attendance_records (via student_id)
        //    - assessments + assessment_marks (via student_id)
        //    - homework + submissions (via student_id)
        //    - fee_records (via parent_id)
        //    - payments (via parent_id)
        //    - messages + message_threads (via owner_user_id or sender_id)
        //    - notifications (via user_id)
        //    - device_tokens (via user_id)
        //    - user_sessions (via user_id)
        //    - parent_child_links (via parent_id)
        //    - leave_requests (via requester_id or parent_id)
        //    - audit_logs (via actor_id)
        // 2. Serialize as structured JSON
        // 3. Upload to Supabase Storage: {schoolId}/dsar/{userId}_{timestamp}.json
        // 4. Return URL
    }
}
```

### 7.4 DataErasureService

```kotlin
class DataErasureService {
    suspend fun eraseUserData(userId: UUID, schoolId: UUID, retentionOverrides: Map<String, Int>) {
        // 1. Check legal retention requirements:
        //    - Financial records: retain 7 years (Income Tax Act)
        //    - Academic records: retain per school policy (default 5 years)
        //    - Attendance: retain 3 years
        //    - Communication: delete immediately
        //    - Device/session: delete immediately
        // 2. For deletable categories: DELETE or anonymize (replace PII with "ERASED")
        // 3. For retained categories: anonymize PII fields but keep aggregate data
        // 4. Log erasure in audit_logs
        // 5. Revoke all active sessions
        // 6. Delete device tokens
        // 7. Deactivate app_users account
    }
}
```

### 7.5 ConsentCheckInterceptor

A Ktor plugin that checks consent before processing data:

```kotlin
install(ConsentCheck) {
    // Map route patterns to required data categories
    routeConsentMap = mapOf(
        "/api/v1/parent/academics/**" to "academic",
        "/api/v1/parent/fees/**" to "financial",
        "/api/v1/parent/messages/**" to "communication"
    )
    // If consent withdrawn → 403 with message to re-grant consent
}
```

---

## 8. API Contracts

### 8.1 Consent Management

```
GET /api/v1/parent/privacy/consents
POST /api/v1/parent/privacy/consents
{
  "data_category": "academic",
  "consent_status": "granted"
}
DELETE /api/v1/parent/privacy/consents/{category}
```

### 8.2 Privacy Policy

```
GET /api/v1/privacy-policy
```

Returns the current active privacy policy.

```
POST /api/v1/super/privacy/policies
PATCH /api/v1/super/privacy/policies/{id}
```

Super Admin manages policy versions.

### 8.3 Data Subject Requests

```
POST /api/v1/parent/privacy/dsr
{
  "request_type": "access",  // access | correction | erasure
  "description": "I want a copy of all my data",
  "correction_data": null    // for correction: {"full_name": "Correct Name"}
}
```

```
GET /api/v1/parent/privacy/dsr
GET /api/v1/school/privacy/dsr?status={status}
POST /api/v1/school/privacy/dsr/{id}/process
{
  "approved": true,
  "rejection_reason": null
}
```

### 8.4 Data Processing Records

```
GET /api/v1/school/privacy/processing-records
POST /api/v1/super/privacy/processing-records
```

### 8.5 Breach Management

```
POST /api/v1/super/privacy/breach
GET /api/v1/super/privacy/breach
PATCH /api/v1/super/privacy/breach/{id}
POST /api/v1/super/privacy/breach/{id}/notify-dpa
POST /api/v1/super/privacy/breach/{id}/notify-subjects
```

---

## 9. Security

- Consent records are immutable (INSERT only, no UPDATE — withdrawal creates new row with `withdrawn_at`)
- DSAR export files are private (signed URLs, 7-day expiry)
- Erasure is irreversible — requires admin approval + confirmation
- Breach incidents are super-admin only
- All consent/DSAR operations logged in `audit_logs`
- Privacy policy content is public (no auth required to view)
- Data processing records are admin-only

---

## 10. Validation Rules

| Field | Rule |
|---|---|
| data_category | One of: identity, academic, financial, communication, health, device, marketing |
| consent_status | One of: granted, withdrawn, pending |
| request_type | One of: access, correction, erasure |
| correction_data | JSON object with field→value pairs, max 10 fields |
| retention_period_days | > 0, ≤ 3650 (10 years max) |
| severity | One of: low, medium, high, critical |

---

## 11. Error Handling

| Code | HTTP | Message |
|---|---|---|
| `CONSENT_REQUIRED` | 403 | Consent for {category} has been withdrawn. Please re-grant consent. |
| `DSR_ALREADY_PENDING` | 409 | You already have a pending {type} request |
| `DSR_NOT_FOUND` | 404 | Request not found |
| `ERASURE_BLOCKED_RETENTION` | 403 | Data cannot be erased due to legal retention requirement ({years} years) |
| `POLICY_NOT_FOUND` | 404 | No active privacy policy found |

---

## 12. Edge Cases

- **Parent erases account but child remains enrolled:** Student data retained (school's legitimate interest); parent's PII anonymized
- **Consent withdrawn for academic:** Parent loses access to child's academic data but child's records remain in school's system
- **DSAR for data across multiple schools:** Parent must submit per school (data is school-scoped)
- **Erasure with pending fee dues:** Erasure blocked until dues cleared (legitimate interest for fee collection)
- **Policy change after consent granted:** All affected users must re-confirm consent on next login
- **Breach detected by automated monitoring:** Create incident, alert super admin immediately

---

## 13. Background Processing

| Job | Schedule | Description |
|---|---|---|
| DSAR export generation | On submit (async) | Generate data export JSON, upload to storage |
| Retention purge | Monthly | Delete data past retention period per category |
| Consent re-confirmation check | On policy publish | Flag users who need re-confirmation |
| Breach notification timer | On breach creation | Alert if 72h window approaching without DPA notification |

---

## 14. Monitoring

| Metric | Type |
|---|---|
| `dpdp.consents_granted_total` | Counter |
| `dpdp.consents_withdrawn_total` | Counter |
| `dpdp.dsr_submitted_total` | Counter (by type) |
| `dpdp.dsr_processing_time_hours` | Histogram |
| `dpdp.erasure_completed_total` | Counter |
| `dpdp.breach_incidents_open` | Gauge |

**Alerts:**
- DSAR not processed within 30 days → Warning (DPDP requires prompt response)
- Breach not reported to DPA within 72h → Critical
- Consent withdrawal rate > 5% in a week → Warning

---

## 15. Feature Flags

| Flag | Default | Description |
|---|---|---|
| `DPDP_COMPLIANCE_ENABLED` | false | Enable consent management + DSAR |
| `DPDP_CONSENT_CHECK_ENABLED` | false | Enable consent check interceptor |
| `DPDP_AUTO_RETENTION_PURGE` | false | Enable automatic data purge |

---

## 16. Migration Strategy

### 16.1 Migration File

`docs/db/migration_034_dpdp_compliance.sql`

Creates `privacy_policies`, `data_consents`, `data_subject_requests`, `data_processing_records`, `data_breach_incidents` tables.

### 16.2 Seed Data

- Insert initial privacy policy v1.0
- Insert default data processing records for each purpose
- Backfill consent records for all existing users (status='granted', implicit consent from existing usage)

### 16.3 Rollback

Drop all 5 tables. No business data affected.

---

## 17. Testing Strategy

### 17.1 Unit Tests

- Consent grant/withdraw — status transitions correct
- Consent check — returns false for withdrawn categories
- Data export — all relevant tables included in export JSON
- Erasure — correct tables deleted vs anonymized based on retention
- Retention calculation — correct per category

### 17.2 Integration Tests

- Grant consent → access data → withdraw consent → access blocked
- Submit DSAR → admin processes → export URL available
- Submit erasure → admin approves → user data anonymized, sessions revoked
- Erasure with pending fees → blocked with error
- Policy change → users flagged for re-confirmation
- Breach creation → 72h timer alert

---

## 18. Acceptance Criteria

- [ ] Parent can view and manage consent per data category
- [ ] Consent withdrawal blocks access to corresponding data
- [ ] Parent can submit DSAR (access, correction, erasure)
- [ ] Admin can process DSAR requests
- [ ] DSAR export includes all personal data across all tables
- [ ] Erasure anonymizes PII while respecting legal retention
- [ ] Privacy policy is versioned and viewable
- [ ] Policy change triggers consent re-confirmation
- [ ] Data processing records are maintained
- [ ] Breach notification workflow with 72h timer
- [ ] All consent/DSAR actions logged in audit log

---

## 19. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 2 days | DB migration, Exposed tables, seed data |
| 2 | 3 days | ConsentService + ConsentCheck interceptor |
| 3 | 3 days | DataExportService (collect from all tables) |
| 4 | 2 days | DataErasureService (with retention logic) |
| 5 | 2 days | DSR workflow (submit, process, approve/reject) |
| 6 | 2 days | Privacy policy management + versioning |
| 7 | 1 day | Data processing records CRUD |
| 8 | 2 days | Breach incident management + 72h timer |
| 9 | 3 days | Client UI (consent management, DSAR submission, admin DSAR queue) |
| 10 | 2 days | Tests (unit + integration) |

---

## 20. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | 5 new table objects |
| `server/.../db/DatabaseFactory.kt` | Modify | Register new tables |
| `server/.../feature/privacy/ConsentService.kt` | New | Consent management |
| `server/.../feature/privacy/ConsentCheckPlugin.kt` | New | Ktor consent check interceptor |
| `server/.../feature/privacy/DataExportService.kt` | New | DSAR data export |
| `server/.../feature/privacy/DataErasureService.kt` | New | DSAR erasure |
| `server/.../feature/privacy/DataSubjectRequestService.kt` | New | DSR workflow |
| `server/.../feature/privacy/PrivacyRouting.kt` | New | All privacy API endpoints |
| `server/.../feature/privacy/BreachService.kt` | New | Breach management |
| `server/.../Application.kt` | Modify | Install ConsentCheck plugin, register routes |
| `docs/db/migration_034_dpdp_compliance.sql` | New | DDL + seed data |
| `shared/.../feature/privacy/PrivacyApi.kt` | New | Client API |
| `composeApp/.../ui/v2/screens/parent/ConsentManagementScreen.kt` | New | Consent UI |
| `composeApp/.../ui/v2/screens/parent/DataRequestScreen.kt` | New | DSAR submission |
| `composeApp/.../ui/v2/screens/admin/DataRequestQueueScreen.kt` | New | Admin DSR processing |

---

## 21. Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Erasure deletes data required for legal compliance | Medium | Critical | Retention overrides per category; admin approval required |
| DSAR export takes too long for large datasets | Medium | Medium | Async generation; notify when ready |
| Consent check blocks critical functionality | Low | High | Exclude auth and consent management routes from check |
| Breach notification missed | Low | Critical | Automated 72h timer with escalating alerts |
| Backfilling consent for existing users is inaccurate | Medium | Low | Mark as 'granted' with note 'implicit from existing usage'; allow withdrawal |
