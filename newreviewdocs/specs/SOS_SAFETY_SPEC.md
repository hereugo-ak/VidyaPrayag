# SOS Safety Alert — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** `WHATSAPP_INTEGRATION_SPEC.md`, `TRANSPORT_TRACKING_SPEC.md`
> **Source:** `DIFFERENTIATING_FEATURES.md` §6.3
> **Template:** `_SPEC_TEMPLATE.md` v1 (25 mandatory + 6 optional sections)

---

## 1. Feature Overview

### What

Emergency SOS system for students during school hours and transport: one-tap SOS button that alerts school admin, class teacher, and designated guardians with location, student info, and timestamp. Integrates with transport tracking for bus emergencies.

### Why — Product Rationale

Student safety is the top priority for parents and schools. An SOS system provides immediate emergency communication with location context, enabling rapid response. This is a **medium-priority differentiating feature** (Phase 3, effort M) that builds trust and addresses a critical safety need.

### What Stands Out (Competitive Moat)

From `DIFFERENTIATING_FEATURES.md` §6.3:
> "SOS Safety — one-tap emergency alert with location, multi-channel delivery, escalation."

No major Indian school ERP offers an integrated SOS system with multi-channel delivery (FCM + WhatsApp + SMS), automatic escalation, and transport integration. The key moat is **multi-channel emergency alerting with automatic escalation**.

### Goals

- One-tap SOS from parent app (on behalf of child) or future student app
- Immediate alerts to: school admin, class teacher, transport staff (if on bus)
- Alert includes: student name, class, current location (if available), timestamp, last known bus location
- Multi-channel: FCM (instant) + WhatsApp + SMS
- Admin SOS dashboard with active alerts
- Alert escalation: if not acknowledged in 5 min → escalate to super admin + police helpline (configurable)
- Resolution tracking: admin marks alert as resolved with notes

### Non-goals

- [ ] Two-way audio/video communication (SOS is alert-only, not a call)
- [ ] Real-time location tracking of student (uses last known bus location or parent-provided)
- [ ] Automated emergency services dispatch (notifies contacts, doesn't call 112)
- [ ] SOS from teacher app (teacher can acknowledge/resolve, not trigger)
- [ ] Geo-fencing alerts (that's transport tracking)
- [ ] Anonymous SOS reporting (SOS is identified, not anonymous)
- [ ] Medical emergency integration (no integration with hospitals/ambulance)
- [ ] Panic button wearable integration (app-based only)

### Dependencies

- `TRANSPORT_TRACKING_SPEC.md` — bus GPS tracking (location context for transport SOS)
- `NotificationsTable` — notification infrastructure
- `WHATSAPP_INTEGRATION_SPEC.md` — WhatsApp for instant alerts
- `StudentsTable` — student info (name, class, photo, parent contact)
- `AppUsersTable` — admin and teacher accounts for notification delivery
- `SmartNotificationService` — priority-based notification routing

### Related Modules

- `server/.../feature/sos/` — new SOS module
- `server/.../feature/transport/` — transport tracking (bus location)
- `server/.../feature/notification/` — notification infrastructure
- `composeApp/.../ui/v2/screens/parent/` — parent UI (SOS button)
- `composeApp/.../ui/v2/screens/admin/` — admin UI (SOS dashboard)

---

## 2. Current System Assessment

### Existing Code

- `TRANSPORT_TRACKING_SPEC.md` — bus GPS tracking (location context for transport SOS)
- `NotificationsTable` — notification infrastructure
- `WHATSAPP_INTEGRATION_SPEC.md` — WhatsApp for instant alerts
- No SOS/emergency system exists
- `DIFFERENTIATING_FEATURES.md` §6.3: SOS Safety, effort M

### Existing Database

- `NotificationsTable` — notification records
- `StudentsTable` — student info (name, class, photo, parent contact)
- `AppUsersTable` — admin and teacher accounts
- `BusRoutesTable` / `BusTrackingTable` (from `TRANSPORT_TRACKING_SPEC.md`) — bus location
- `SchoolsTable` — school info, emergency contacts
- No SOS alerts table exists

### Existing APIs

- Notification API (existing) — notification dispatch
- WhatsApp API (existing) — WhatsApp message sending
- Transport API (existing) — bus location
- No SOS API exists

### Existing UI

- Parent home screen (existing) — no SOS button
- Admin dashboard (existing) — no SOS dashboard
- No SOS UI exists

### Existing Services

- `SmartNotificationService` — priority-based notification routing
- `WhatsAppService` — WhatsApp message sending
- `TransportService` — bus tracking
- No SOS service

### Existing Documentation

- `DIFFERENTIATING_FEATURES.md` §6.3 — SOS Safety
- `TRANSPORT_TRACKING_SPEC.md` — transport tracking
- `WHATSAPP_INTEGRATION_SPEC.md` — WhatsApp integration
- `SMART_NOTIFICATIONS_SPEC.md` — notification priority routing

### Technical Debt

| # | Gap | Details |
|---|---|---|
| TD-1 | No SOS alerts table | No `sos_alerts` table |
| TD-2 | No SOS service | No service for trigger, acknowledge, escalate, resolve |
| TD-3 | No escalation job | No background job for auto-escalation |
| TD-4 | No SOS UI | No parent SOS button or admin dashboard |

### Gaps

| # | Gap | Impact | Severity |
|---|---|---|---|
| G1 | No SOS trigger | Parents can't send emergency alerts | **Critical** |
| G2 | No multi-channel delivery | Alerts may be missed | **High** |
| G3 | No escalation | Unacknowledged alerts not escalated | **High** |
| G4 | No admin dashboard | Admin can't see/manage active alerts | **High** |
| G5 | No resolution tracking | No audit trail of SOS resolution | **Medium** |
| G6 | No transport integration | Bus location not included in SOS | **Medium** |

---

## 3. Functional Requirements

### FR-001
| Field | Value |
|---|---|
| **Title** | SOS Button |
| **Description** | SOS button in parent app (prominent, accessible from any screen via shake gesture or floating button). |
| **Priority** | Critical |
| **User Roles** | Parent |
| **Acceptance notes** | Floating action button (FAB) on parent home screen — red, prominent. Long-press (2 seconds) to trigger (prevents accidental activation). Confirmation dialog: "Send SOS alert for {child_name}?" |

### FR-002
| Field | Value |
|---|---|
| **Title** | Alert Recipients |
| **Description** | On trigger: alert school admin + class teacher + transport staff (if student assigned to bus). |
| **Priority** | Critical |
| **User Roles** | System |
| **Acceptance notes** | School admin (all admins with SOS access), class teacher (student's class teacher), transport staff (if student assigned to bus route). All receive simultaneous alerts. |

### FR-003
| Field | Value |
|---|---|
| **Title** | Alert Payload |
| **Description** | Alert payload: student name, class, photo, parent contact, last known bus location, timestamp. |
| **Priority** | Critical |
| **User Roles** | System |
| **Acceptance notes** | Payload includes: student_name, class_name, photo_url, parent_phone, location_lat/lng (if provided), bus_id, bus_location_lat/lng (if on bus), triggered_by_name, created_at. |

### FR-004
| Field | Value |
|---|---|
| **Title** | Multi-Channel Delivery |
| **Description** | Multi-channel delivery: FCM push (critical priority) + WhatsApp + SMS. |
| **Priority** | Critical |
| **User Roles** | System |
| **Acceptance notes** | FCM push notification (critical priority, immediate delivery). WhatsApp message to admin + teacher. SMS to admin (if phone available). Uses SmartNotificationService for priority routing. |

### FR-005
| Field | Value |
|---|---|
| **Title** | Admin SOS Dashboard |
| **Description** | Admin SOS dashboard: active alerts with student info + acknowledge button. |
| **Priority** | High |
| **User Roles** | School Admin |
| **Acceptance notes** | Red banner at top of admin dashboard when active SOS exists. Full-screen SOS alert view with student photo, info, location map. Acknowledge button (large, prominent). |

### FR-006
| Field | Value |
|---|---|
| **Title** | Escalation |
| **Description** | Escalation: if not acknowledged in 5 min → escalate to super admin + configurable emergency contact. |
| **Priority** | High |
| **User Roles** | System |
| **Acceptance notes** | Background job every 1 minute checks for active alerts older than 5 minutes. Escalates to super admin + configurable emergency contact (police helpline, if configured). |

### FR-007
| Field | Value |
|---|---|
| **Title** | Resolution |
| **Description** | Resolution: admin marks resolved with notes + action taken. |
| **Priority** | High |
| **User Roles** | School Admin |
| **Acceptance notes** | Admin marks alert as resolved with resolution_notes (text). Status → resolved. Parent notified of resolution. |

### FR-008
| Field | Value |
|---|---|
| **Title** | Parent Confirmation |
| **Description** | Parent receives confirmation when alert acknowledged. |
| **Priority** | High |
| **User Roles** | Parent |
| **Acceptance notes** | When admin acknowledges: parent receives FCM notification "SOS acknowledged by {admin_name}. Help is on the way." |

### FR-009
| Field | Value |
|---|---|
| **Title** | SOS History Log |
| **Description** | SOS history log for audit. |
| **Priority** | Medium |
| **User Roles** | School Admin, Super Admin |
| **Acceptance notes** | All SOS alerts retained in DB. History view shows all alerts with status, timestamps, resolution notes. Available for audit. |

### Non-Functional Requirements

| ID | Requirement |
|---|---|
| NFR-1 | SOS trigger: < 2 seconds (alert sent) |
| NFR-2 | FCM delivery: < 5 seconds |
| NFR-3 | WhatsApp delivery: < 10 seconds |
| NFR-4 | SMS delivery: < 15 seconds |
| NFR-5 | Escalation check: every 1 minute |
| NFR-6 | Escalation threshold: 5 minutes (configurable) |
| NFR-7 | SOS history: retained for 7 years (audit) |

---

## 4. User Stories

### Parent
- [ ] Trigger SOS for my child with one tap (long-press confirmation)
- [ ] Receive confirmation when school acknowledges SOS
- [ ] Receive notification when SOS is resolved
- [ ] View my SOS history

### School Admin
- [ ] See active SOS alerts on dashboard (red banner)
- [ ] View full SOS alert details (student info, location, photo)
- [ ] Acknowledge SOS alert → parent notified
- [ ] Resolve SOS alert with resolution notes
- [ ] View SOS history for audit

### Teacher
- [ ] Receive SOS alert for my class student (FCM + WhatsApp)
- [ ] View SOS alert details
- [ ] Acknowledge SOS alert

### System
- [ ] Send multi-channel alerts (FCM + WhatsApp + SMS)
- [ ] Escalate unacknowledged alerts after 5 minutes
- [ ] Notify parent on acknowledgment
- [ ] Notify parent on resolution
- [ ] Log all SOS events for audit

---

## 5. Business Rules

### BR-001
**Rule:** SOS requires long-press (2 seconds) to trigger.
**Enforcement:** Prevents accidental activation. Client-side enforcement with confirmation dialog. User must confirm "Send SOS alert for {child_name}?".

### BR-002
**Rule:** SOS alerts are critical priority.
**Enforcement:** Uses SmartNotificationService with "critical" priority. FCM + WhatsApp + SMS (all channels). Immediate delivery, no batching.

### BR-003
**Rule:** Escalation after 5 minutes if not acknowledged.
**Enforcement:** Background job checks every 1 minute. If status = active AND created_at < now() - 5 minutes → escalate. Escalation threshold configurable per school.

### BR-004
**Rule:** Only admin can resolve SOS.
**Enforcement:** Resolve endpoint requires admin role. Teacher can acknowledge but not resolve. Super admin can resolve any school's SOS.

### BR-005
**Rule:** SOS alerts are school-scoped.
**Enforcement:** All alerts have school_id. Admin sees own school's alerts. Super admin sees all. No cross-school access.

### BR-006
**Rule:** SOS history retained for 7 years.
**Enforcement:** No auto-deletion of SOS alerts. Retained for audit and compliance. Data retention policy: 7 years.

### BR-007
**Rule:** Transport integration when student on bus.
**Enforcement:** If student assigned to bus route AND bus has current location → include bus_id and bus_location in alert payload. If not on bus, bus fields are null.

### BR-008
**Rule:** Parent can trigger SOS for own children only.
**Enforcement:** Parent endpoint verifies parent-student relationship. No triggering SOS for other students.

### BR-009
**Rule:** Escalation contacts are configurable per school.
**Enforcement:** School configures emergency contact (phone number) for escalation. If not configured, escalation only to super admin. Police helpline is optional, configurable.

### BR-010
**Rule:** Multiple SOS for same student allowed.
**Enforcement:** No deduplication. If parent triggers SOS again, new alert created. Each alert tracked independently. Historical alerts remain.

---

## 6. Database Design

### 6.1 Entity Relationship Summary

One new table: `sos_alerts`. References existing `students`, `app_users`, `schools`, and `buses` tables.

### 6.2 New Tables

#### `sos_alerts` table

```sql
CREATE TABLE sos_alerts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    student_id      UUID NOT NULL,
    student_name    TEXT NOT NULL,
    class_name      TEXT,
    triggered_by    UUID NOT NULL,                 -- parent/user who triggered
    triggered_by_name TEXT NOT NULL,
    trigger_source  VARCHAR(16) NOT NULL,          -- parent_app | student_app | bus | teacher
    location_lat    DOUBLE PRECISION,
    location_lng    DOUBLE PRECISION,
    bus_id          UUID,                          -- if on bus at time of SOS
    bus_location_lat DOUBLE PRECISION,
    bus_location_lng DOUBLE PRECISION,
    status          VARCHAR(16) NOT NULL DEFAULT 'active', -- active | acknowledged | escalated | resolved
    acknowledged_by UUID,
    acknowledged_at TIMESTAMP,
    escalated_at    TIMESTAMP,
    resolved_by     UUID,
    resolved_at     TIMESTAMP,
    resolution_notes TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_sos_alerts_school_status ON sos_alerts(school_id, status, created_at DESC);
```

### 6.3 Modified Tables

N/A — no existing tables modified.

### 6.4 Indexes

- `idx_sos_alerts_school_status` on `sos_alerts(school_id, status, created_at DESC)` — dashboard lookup
- Implicit index on `student_id` for history lookup

### 6.5 Constraints

- `sos_alerts.student_name` — NOT NULL
- `sos_alerts.triggered_by_name` — NOT NULL
- `sos_alerts.trigger_source` — NOT NULL, VARCHAR(16), values: parent_app | student_app | bus | teacher
- `sos_alerts.status` — NOT NULL, VARCHAR(16), default 'active', values: active | acknowledged | escalated | resolved
- `sos_alerts.resolution_notes` — nullable TEXT (only when resolved)

### 6.6 Foreign Keys

- `sos_alerts.school_id` → `schools.id` (implicit)
- `sos_alerts.student_id` → `students.id` (implicit)
- `sos_alerts.triggered_by` → `app_users.id` (implicit)
- `sos_alerts.bus_id` → `buses.id` (implicit, nullable)
- `sos_alerts.acknowledged_by` → `app_users.id` (implicit, nullable)
- `sos_alerts.resolved_by` → `app_users.id` (implicit, nullable)

### 6.7 Soft Delete Strategy

N/A — SOS alerts are never deleted. Retained for 7-year audit period. No soft delete needed.

### 6.8 Audit Fields

- `sos_alerts.triggered_by`, `triggered_by_name` — who triggered the SOS
- `sos_alerts.acknowledged_by`, `acknowledged_at` — who acknowledged and when
- `sos_alerts.escalated_at` — when escalation occurred
- `sos_alerts.resolved_by`, `resolved_at` — who resolved and when
- `sos_alerts.resolution_notes` — resolution details
- `sos_alerts.created_at` — when SOS was triggered

### 6.9 Migration Notes

Migration: `docs/db/migration_085_sos_safety.sql`
- CREATE 1 table: `sos_alerts`
- CREATE 1 index: `idx_sos_alerts_school_status`
- No data migration (new feature)

### 6.10 Exposed Mappings

```kotlin
object SosAlertsTable : UUIDTable("sos_alerts", "id") {
    val schoolId          = uuid("school_id")
    val studentId         = uuid("student_id")
    val studentName       = text("student_name")
    val className         = text("class_name").nullable()
    val triggeredBy       = uuid("triggered_by")
    val triggeredByName   = text("triggered_by_name")
    val triggerSource     = varchar("trigger_source", 16)
    val locationLat       = double("location_lat").nullable()
    val locationLng       = double("location_lng").nullable()
    val busId             = uuid("bus_id").nullable()
    val busLocationLat    = double("bus_location_lat").nullable()
    val busLocationLng    = double("bus_location_lng").nullable()
    val status            = varchar("status", 16).default("active")
    val acknowledgedBy    = uuid("acknowledged_by").nullable()
    val acknowledgedAt    = timestamp("acknowledged_at").nullable()
    val escalatedAt       = timestamp("escalated_at").nullable()
    val resolvedBy        = uuid("resolved_by").nullable()
    val resolvedAt        = timestamp("resolved_at").nullable()
    val resolutionNotes   = text("resolution_notes").nullable()
    val createdAt         = timestamp("created_at")

    init {
        index("idx_sos_alerts_school_status", schoolId, status, createdAt)
    }
}
```

Register in `DatabaseFactory.allTables`.

### 6.11 Seed Data

N/A — SOS alerts are user-triggered. No seed data needed.

---

## 7. State Machines

### SOS Alert Lifecycle State Machine

```
active ──admin_acknowledges──> acknowledged ──admin_resolves──> resolved ──(terminal)
  │
  ├──5_min_no_ack──> escalated ──admin_acknowledges──> acknowledged ──admin_resolves──> resolved
  │                    │
  │                    └──admin_resolves──> resolved (direct from escalated)
  │
  └──(terminal states: resolved)
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `active` | Admin acknowledges | `acknowledged` | Admin taps acknowledge button |
| `active` | 5 min elapsed, no acknowledgment | `escalated` | Escalation job detects timeout |
| `acknowledged` | Admin resolves with notes | `resolved` | Admin enters resolution notes |
| `escalated` | Admin acknowledges | `acknowledged` | Admin taps acknowledge (after escalation) |
| `escalated` | Admin resolves with notes | `resolved` | Admin resolves directly (after escalation) |
| `resolved` | — | `resolved` | Terminal state |

### SOS Trigger State Machine

```
idle ──parent_long_press──> confirming ──parent_confirms──> triggering ──alert_sent──> alert_active
  │                              │                              │
  │                              └──parent_cancels──> idle       └──trigger_failed──> error
  │
  └──alert_active ──admin_acknowledges──> parent_notified──> idle (SOS resolved)
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `idle` | Parent long-presses SOS button | `confirming` | Show confirmation dialog |
| `confirming` | Parent confirms | `triggering` | Start SOS trigger |
| `confirming` | Parent cancels | `idle` | Dismiss dialog |
| `triggering` | Alert sent successfully | `alert_active` | Alert created, notifications sent |
| `triggering` | Trigger failed | `error` | Network or server error |
| `error` | Parent retries | `confirming` | Back to confirmation |
| `alert_active` | Admin acknowledges | `parent_notified` | Parent notified of acknowledgment |
| `parent_notified` | Admin resolves | `idle` | SOS resolved, parent notified |

### Escalation State Machine

```
not_escalated ──5_min_timeout──> escalating ──escalation_sent──> escalated
  │                                                     │
  │                                                     └──admin_acknowledges──> acknowledged
  │
  └──not_escalated ──admin_acknowledges_before_timeout──> acknowledged (no escalation needed)
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `not_escalated` | 5 min timeout, no acknowledgment | `escalating` | Escalation job triggers |
| `not_escalated` | Admin acknowledges before timeout | `acknowledged` | No escalation needed |
| `escalating` | Escalation notifications sent | `escalated` | Super admin + emergency contact notified |
| `escalated` | Admin acknowledges | `acknowledged` | Admin responds after escalation |

---

## 8. Backend Architecture

### 8.1 Component Overview

`SosService` handles SOS trigger, acknowledge, escalate, and resolve operations. `SosEscalationJob` runs every 1 minute to check for unacknowledged alerts. `SosRouting` exposes API endpoints. Uses `SmartNotificationService` for multi-channel delivery.

### 8.2 Design Principles

1. **Speed is critical** — SOS trigger and notification delivery must be fast (< 2 seconds)
2. **Multi-channel redundancy** — FCM + WhatsApp + SMS to ensure alert is received
3. **Automatic escalation** — no human intervention needed for escalation
4. **Audit trail** — all SOS events logged, retained 7 years
5. **School-scoped** — alerts and dashboard are school-specific
6. **Transport integration** — bus location included when student on bus

### 8.3 Core Types

#### SosService

```kotlin
class SosService {
    suspend fun trigger(request: SosTriggerRequest): SosAlertDto {
        // 1. Create sos_alerts row (status=active)
        // 2. Fetch student info (name, class, photo, parent contact)
        // 3. If student assigned to bus → fetch current bus location
        // 4. Send critical notifications:
        //    - FCM push (critical priority) to school admin + class teacher
        //    - WhatsApp to admin + teacher
        //    - SMS to admin (if phone available)
        // 5. Schedule escalation timer (5 min)
        // 6. Return alert DTO
    }

    suspend fun acknowledge(alertId: UUID, userId: UUID): SosAlertDto {
        // 1. Update status=acknowledged
        // 2. Notify parent: "SOS acknowledged by {admin_name}"
    }

    suspend fun escalate(alertId: UUID) {
        // 1. Update status=escalated
        // 2. Notify super admin
        // 3. If configured: notify emergency contact / police helpline
    }

    suspend fun resolve(alertId: UUID, userId: UUID, notes: String): SosAlertDto
    suspend fun getActiveAlerts(schoolId: UUID): List<SosAlertDto>
    suspend fun getHistory(schoolId: UUID, page: Int): List<SosAlertDto>
}
```

#### SosEscalationJob

```kotlin
class SosEscalationJob(private val sosService: SosService) {
    suspend fun execute(): Int {
        // 1. SELECT * FROM sos_alerts WHERE status = 'active' AND created_at < now() - 5 minutes
        // 2. For each: call sosService.escalate(alertId)
        // 3. Return count of escalated alerts
    }
}
```

### 8.4 Repositories

- `SosAlertRepository` — CRUD for `sos_alerts`

### 8.5 Mappers

- `SosAlertMapper` — maps `sos_alerts` rows to `SosAlertDto`

### 8.6 Permission Checks

- Parent trigger: parent role + JWT auth + parent-student relationship verified
- Admin acknowledge/resolve: school admin role + JWT auth
- Teacher acknowledge: teacher role + JWT auth (student's class teacher)
- Super admin: super admin role (all schools)
- Active alerts: admin/teacher role for own school
- History: admin role for own school, super admin for all

### 8.7 Background Jobs

- **SOS Escalation Job** — every 1 minute
  1. `SELECT * FROM sos_alerts WHERE status = 'active' AND created_at < now() - 5 minutes`
  2. For each: call `sosService.escalate(alertId)`
  3. Return count of escalated alerts

### 8.8 Domain Events

- `SosTriggered` — emitted when SOS is triggered
- `SosAcknowledged` — emitted when admin acknowledges
- `SosEscalated` — emitted when alert is escalated
- `SosResolved` — emitted when alert is resolved

### 8.9 Caching

N/A — SOS alerts are real-time, not cached. Dashboard fetches from DB directly.

### 8.10 Transactions

- Trigger: single transaction (insert alert, send notifications)
- Acknowledge: single transaction (update status, notify parent)
- Escalate: single transaction (update status, notify super admin + emergency contact)
- Resolve: single transaction (update status, notify parent)

### 8.11 Rate Limiting

- SOS trigger: 1 per minute per parent (prevents spam). Additional triggers within 1 minute are allowed but logged.

### 8.12 Configuration

- `SOS_SAFETY_ENABLED` — default `true`; enable/disable feature
- `SOS_ESCALATION_THRESHOLD_MINUTES` — default `5`; minutes before escalation
- `SOS_ESCALATION_CHECK_INTERVAL_SECONDS` — default `60`; job interval
- `SOS_EMERGENCY_CONTACT_PHONE` — configurable per school; phone for escalation
- `SOS_POLICE_HELPLINE_ENABLED` — default `false`; enable police helpline escalation
- `SOS_HISTORY_RETENTION_YEARS` — default `7`; data retention period

### 8.13 Escalation Timer

Background job every 1 minute:
1. `SELECT * FROM sos_alerts WHERE status = 'active' AND created_at < now() - 5 minutes`
2. For each: call `escalate(alertId)`

---

## 9. API Contracts

### 9.1 Parent Endpoints

```
POST /api/v1/parent/sos
  Body: { student_id, location_lat?, location_lng? }
  → 201: SosAlertDto
  → 403: Not parent of this student

GET /api/v1/parent/sos/history
  → 200: { alerts: [SosAlertDto] }
```

### 9.2 Admin/Teacher Endpoints

```
GET /api/v1/school/sos/active
  → 200: { alerts: [SosAlertDto] }

POST /api/v1/school/sos/{id}/acknowledge
  → 200: SosAlertDto (acknowledged)

POST /api/v1/school/sos/{id}/resolve
  Body: { resolution_notes }
  → 200: SosAlertDto (resolved)

GET /api/v1/school/sos/history?page={page}
  → 200: { alerts: [SosAlertDto], total: Int }
```

### 9.3 DTO Models

All `@Serializable`, wrapped in `ApiResponse<T>` pattern.

```kotlin
@Serializable data class SosAlertDto(
    val id: String,
    val studentId: String,
    val studentName: String,
    val className: String?,
    val triggeredByName: String,
    val triggerSource: String,       // parent_app | student_app | bus | teacher
    val locationLat: Double?,
    val locationLng: Double?,
    val busId: String?,
    val busLocationLat: Double?,
    val busLocationLng: Double?,
    val status: String,              // active | acknowledged | escalated | resolved
    val acknowledgedByName: String?,
    val acknowledgedAt: String?,
    val escalatedAt: String?,
    val resolvedByName: String?,
    val resolvedAt: String?,
    val resolutionNotes: String?,
    val createdAt: String,
)

@Serializable data class SosTriggerRequest(
    val studentId: String,
    val locationLat: Double?,
    val locationLng: Double?,
)

@Serializable data class SosResolveRequest(
    val resolutionNotes: String,
)

enum class SosTriggerSource { PARENT_APP, STUDENT_APP, BUS, TEACHER }
enum class SosStatus { ACTIVE, ACKNOWLEDGED, ESCALATED, RESOLVED }
```

---

## 10. Frontend Architecture

### 10.1 Screens

| Screen | Platform | Role | Description |
|---|---|---|---|
| `ParentHomeScreen` (modified) | Compose | Parent | Add SOS FAB (red, prominent) |
| `SosConfirmationDialog` | Compose | Parent | Confirmation dialog before SOS trigger |
| `SosDashboardScreen` | Compose | Admin | Active SOS alerts dashboard |
| `SosAlertDetailScreen` | Compose | Admin, Teacher | Full SOS alert view with student info, location, acknowledge/resolve |
| `SosHistoryScreen` | Compose | Admin | SOS history log |

### 10.2 Navigation

- Parent: Home → SOS FAB → Confirmation → Alert sent
- Admin: Dashboard → SOS Banner → SOS Dashboard → Alert Detail → Acknowledge/Resolve
- Teacher: Notification → SOS Alert Detail → Acknowledge

### 10.3 UX Flows

#### Parent: Trigger SOS
1. Parent long-presses red SOS FAB (2 seconds)
2. Confirmation dialog: "Send SOS alert for {child_name}?"
3. Parent confirms → "Sending SOS..." loading
4. Alert sent → "Alert sent. School has been notified."
5. Parent waits for acknowledgment notification

#### Admin: View and Acknowledge SOS
1. Admin sees red banner on dashboard: "ACTIVE SOS: {student_name}"
2. Taps banner → SOS Dashboard
3. Sees active alert(s) with student photo, info, location
4. Taps alert → Full detail view
5. Taps "Acknowledge" (large button)
6. Parent notified: "SOS acknowledged by {admin_name}"

#### Admin: Resolve SOS
1. Admin views acknowledged alert
2. Taps "Resolve"
3. Enters resolution notes
4. Submits → alert resolved
5. Parent notified: "SOS resolved. {resolution_notes}"

### 10.4 State Management

```kotlin
data class SosTriggerState(
    val isLongPressing: Boolean,
    val showConfirmation: Boolean,
    val isTriggering: Boolean,
    val error: String?,
)

data class SosDashboardState(
    val activeAlerts: List<SosAlertDto>,
    val isLoading: Boolean,
    val error: String?,
)

data class SosAlertDetailState(
    val alert: SosAlertDto?,
    val isAcknowledging: Boolean,
    val isResolving: Boolean,
    val resolutionNotes: String,
    val error: String?,
)
```

### 10.5 Offline Support

- SOS trigger: NOT available offline (requires server to create alert and send notifications)
- SOS dashboard: requires online (real-time alerts)
- SOS history: cached locally (last fetched)

### 10.6 Loading States

- Triggering SOS: "Sending SOS alert..."
- Acknowledging: "Acknowledging alert..."
- Resolving: "Resolving alert..."
- Loading dashboard: "Loading active alerts..."

### 10.7 Error Handling (UI)

- Trigger failed: "Failed to send SOS. Please try again or call school directly."
- Not parent of student: "You can only send SOS for your own children."
- Alert not found: "SOS alert not found."
- Already acknowledged: "This alert has already been acknowledged."
- Already resolved: "This alert has already been resolved."
- Network error: "Connection issue. Please check your internet."

### 10.8 Component Integration Guidelines

| Rule | Description |
|---|---|
| **R1** | SOS FAB: red, prominent, bottom-right of parent home screen |
| **R2** | Long-press: 2 seconds to activate (visual progress indicator) |
| **R3** | Confirmation dialog: clear "Send SOS alert for {child_name}?" with Yes/No |
| **R4** | Admin dashboard: red banner at top when active SOS exists |
| **R5** | Alert detail: student photo, name, class, location map, timestamp |
| **R6** | Acknowledge button: large, green, prominent |
| **R7** | Resolve form: textarea for resolution notes, submit button |
| **R8** | Escalation indicator: amber badge when alert is escalated |
| **R9** | Status badges: red=active, blue=acknowledged, amber=escalated, green=resolved |
| **R10** | Location map: show student location + bus location (if available) |

### 10.9 SOS Button

- Floating action button (FAB) on parent home screen — red, prominent
- Long-press (2 seconds) to trigger (prevents accidental activation)
- Confirmation dialog: "Send SOS alert for {child_name}?"
- On confirm: immediate trigger + show "Alert sent. School has been notified."

### 10.10 Admin SOS Dashboard

- Red banner at top of admin dashboard when active SOS exists
- Full-screen SOS alert view with student photo, info, location map
- Acknowledge button (large, prominent)
- Resolution form

---

## 11. Shared Module Changes (KMP)

### 11.1 DTOs

All DTOs defined in section 9.3, placed in `shared/.../sos/domain/model/SosModels.kt`.

### 11.2 Domain Models

```kotlin
data class SosAlert(
    val id: String,
    val schoolId: String,
    val studentId: String,
    val studentName: String,
    val className: String?,
    val triggeredBy: String,
    val triggeredByName: String,
    val triggerSource: SosTriggerSource,
    val locationLat: Double?,
    val locationLng: Double?,
    val busId: String?,
    val busLocationLat: Double?,
    val busLocationLng: Double?,
    val status: SosStatus,
    val acknowledgedBy: String?,
    val acknowledgedAt: Instant?,
    val escalatedAt: Instant?,
    val resolvedBy: String?,
    val resolvedAt: Instant?,
    val resolutionNotes: String?,
    val createdAt: Instant,
)
```

### 11.3 Repository Interfaces

```kotlin
interface SosRepository {
    suspend fun trigger(token: String, studentId: String, lat: Double?, lng: Double?): NetworkResult<SosAlertDto>
    suspend fun getActiveAlerts(token: String): NetworkResult<List<SosAlertDto>>
    suspend fun acknowledge(token: String, alertId: String): NetworkResult<SosAlertDto>
    suspend fun resolve(token: String, alertId: String, notes: String): NetworkResult<SosAlertDto>
    suspend fun getHistory(token: String, page: Int): NetworkResult<List<SosAlertDto>>
    suspend fun getParentHistory(token: String): NetworkResult<List<SosAlertDto>>
}
```

### 11.4 UseCases

- `TriggerSosUseCase`
- `GetActiveSosAlertsUseCase`
- `AcknowledgeSosUseCase`
- `ResolveSosUseCase`
- `GetSosHistoryUseCase`

### 11.5 Validation

- `student_id`: valid UUID
- `location_lat` / `location_lng`: valid coordinates (optional)
- `resolution_notes`: non-empty, max 1000 characters

### 11.6 Serialization

Standard Kotlinx serialization. Enums serialized as lowercase strings. Timestamps as ISO strings.

### 11.7 Network APIs

Ktor `@Resource` route definitions in `SosApi.kt`:
- POST `/api/v1/parent/sos`
- GET `/api/v1/parent/sos/history`
- GET `/api/v1/school/sos/active`
- POST `/api/v1/school/sos/{id}/acknowledge`
- POST `/api/v1/school/sos/{id}/resolve`
- GET `/api/v1/school/sos/history?page={page}`

### 11.8 Database Models (Local Cache)

- SOS history cached locally (last fetched, 5-minute TTL)
- Active alerts: not cached (real-time fetch)

---

## 12. Permissions Matrix

| Action | Super Admin | School Admin | Counselor | Teacher | Parent |
|---|---|---|---|---|---|
| Trigger SOS | N/A | N/A | N/A | N/A | ✅ (own children) |
| View active SOS | ✅ (all) | ✅ (own school) | ❌ | ✅ (own class) | ❌ |
| Acknowledge SOS | ✅ (all) | ✅ (own school) | ❌ | ✅ (own class) | ❌ |
| Resolve SOS | ✅ (all) | ✅ (own school) | ❌ | ❌ | ❌ |
| View SOS history | ✅ (all) | ✅ (own school) | ❌ | ❌ | ✅ (own children) |
| Configure escalation | ✅ | ✅ | ❌ | ❌ | ❌ |

---

## 13. Notifications

### SOS Notifications

| Trigger | Recipient | Channel | Template |
|---|---|---|---|
| SOS triggered | School Admin | FCM (critical) + WhatsApp + SMS | "🚨 SOS ALERT: {student_name}, Class {class}. Triggered by {parent_name}. View now." |
| SOS triggered | Class Teacher | FCM (critical) + WhatsApp | "🚨 SOS ALERT: {student_name} in your class. Triggered by parent. View now." |
| SOS triggered | Transport Staff | FCM (critical) | "🚨 SOS ALERT: {student_name} on bus {bus_id}. Check immediately." |
| SOS acknowledged | Parent | FCM + in-app | "SOS acknowledged by {admin_name}. Help is on the way." |
| SOS escalated | Super Admin | FCM (critical) + WhatsApp + SMS | "🚨 SOS ESCALATION: {student_name} at {school_name}. Alert not acknowledged in 5 min." |
| SOS escalated | Emergency Contact | SMS | "SOS ESCALATION: Student {student_name} at {school_name}. Alert not acknowledged." |
| SOS resolved | Parent | FCM + in-app | "SOS resolved. {resolution_notes}" |

### Notification Integration

Uses `SmartNotificationService` with "critical" priority for SOS alerts. Critical priority = immediate delivery, all channels (FCM + WhatsApp + SMS), no batching. Uses `Notify.kt` dispatch.

---

## 14. Background Jobs

### SOS Escalation Job

| Property | Value |
|---|---|
| **Name** | `SosEscalationJob` |
| **Schedule** | Every 1 minute |
| **Duration** | < 10 seconds (few active alerts expected) |
| **Retry** | None (next minute's job handles it) |

#### Job Flow

1. `SELECT * FROM sos_alerts WHERE status = 'active' AND created_at < now() - 5 minutes`
2. For each alert:
   - Update status = escalated, set escalated_at = now
   - Emit `SosEscalated` event
   - Notify super admin (FCM critical + WhatsApp + SMS)
   - If emergency contact configured: notify via SMS
   - If police helpline enabled: notify via SMS
3. Return count of escalated alerts

---

## 15. Integrations

### Internal Integrations

| System | Integration Point | Direction | Protocol | Error Handling |
|---|---|---|---|---|
| `SmartNotificationService` | Multi-channel notifications | Call | Direct call | Log error, continue |
| `WhatsAppService` (`WHATSAPP_INTEGRATION_SPEC.md`) | WhatsApp alerts | Call | Direct call | Log error, continue with FCM+SMS |
| `TransportService` (`TRANSPORT_TRACKING_SPEC.md`) | Bus location | Read | Direct DB | Bus fields null if no data |
| `StudentsTable` | Student info | Read | Direct DB | Required |
| `AppUsersTable` | Admin/teacher accounts | Read | Direct DB | Required |
| `Notify.kt` | Notification dispatch | Call | Direct call | Log error, continue |

### External Integrations

| System | Integration Point | Direction | Protocol | Error Handling |
|---|---|---|---|---|
| FCM (Firebase Cloud Messaging) | Push notifications | Call | HTTP API | Log error, continue with WhatsApp+SMS |
| WhatsApp Business API | WhatsApp messages | Call | HTTP API | Log error, continue with FCM+SMS |
| SMS Gateway | SMS alerts | Call | HTTP API | Log error, continue with FCM+WhatsApp |

### Integration Patterns

- **Multi-channel delivery:** `SmartNotificationService.processNotification()` with priority="critical" → sends FCM + WhatsApp + SMS simultaneously
- **Bus location:** `transportService.getBusLocation(busId)` — returns current bus GPS coordinates
- **Student info:** `studentRepository.getById(studentId)` — returns name, class, photo, parent contact
- **Admin accounts:** `appUserRepository.getSchoolAdmins(schoolId)` — returns all admin accounts for notification

---

## 16. Security

### Authentication

- Parent trigger: parent role + JWT auth via `requireAuth()`
- Admin/teacher endpoints: admin/teacher role + JWT auth
- Super admin: super admin role for cross-school access

### Authorization

- Parent can trigger SOS for own children only (parent-student relationship verified)
- Admin can view/acknowledge/resolve SOS for own school
- Teacher can view/acknowledge SOS for own class students
- Super admin can view/acknowledge/resolve SOS for all schools

### Data Protection

- SOS alerts contain student name, class, photo (sensitive data)
- Location data (student + bus) is sensitive
- Access restricted to authorized roles
- SOS history retained 7 years (audit requirement)

### Input Validation

- `student_id`: valid UUID
- `location_lat` / `location_lng`: valid coordinates (optional)
- `resolution_notes`: non-empty, max 1000 characters

### Rate Limiting

- SOS trigger: 1 per minute per parent (prevents spam, but allows re-trigger in genuine emergencies)

### Audit Logging

- SOS triggered: triggered_by, student_id, school_id, timestamp, trigger_source, location
- SOS acknowledged: acknowledged_by, alert_id, timestamp
- SOS escalated: alert_id, escalated_at, contacts notified
- SOS resolved: resolved_by, alert_id, resolution_notes, timestamp

### PII Handling

- Student name, class, photo in SOS alerts (necessary for emergency response)
- Parent contact in alert payload (necessary for admin to contact parent)
- Location data (student + bus) in alerts (necessary for emergency response)
- All PII access logged, restricted to authorized roles

### Multi-tenant Isolation

- All SOS alerts have `school_id` — school-scoped
- Admin queries filtered by `school_id`
- Super admin can access all schools
- No cross-school SOS data access (except super admin)

---

## 17. Performance & Scalability

### Expected Scale

- SOS triggers: rare (emergency only), maybe 1-10 per month per school
- Active alerts at any time: 0-2 per school
- Escalation job: checks every 1 minute, very few active alerts
- Dashboard: real-time fetch, few alerts

### Query Optimization

- Active alerts: `idx_sos_alerts_school_status(school_id, status, created_at DESC)` — filtered by school and status
- History: paginated, filtered by school_id, ordered by created_at DESC
- Escalation job: `WHERE status = 'active' AND created_at < now() - 5 minutes` — uses index

### Indexing Strategy

- `sos_alerts(school_id, status, created_at DESC)` — dashboard and escalation lookup
- Implicit index on `student_id` for parent history

### Caching Strategy

N/A — SOS alerts are real-time, not cached. Dashboard fetches from DB directly.

### Pagination

- SOS history: 20 alerts per page
- Active alerts: all active alerts in single response (max ~5)

### Connection Pooling

Uses existing HikariCP connection pool. No additional pooling needed.

### Async Processing

- Escalation job: background job (async)
- Notifications: async via `SmartNotificationService` (fire-and-forget)
- Trigger: synchronous (client waits for confirmation)

### Scalability Concerns

- Very low volume (emergencies only). No scalability concerns.
- Escalation job: 1-minute interval, few alerts. Negligible load.
- Notification burst: when SOS triggered, ~5-10 notifications sent simultaneously. Manageable.
- DB storage: ~10 alerts/month/school × ~1KB = ~120KB/year/school. Negligible.

---

## 18. Edge Cases

| # | Scenario | Expected Behavior |
|---|---|---|
| EC-1 | Parent triggers SOS with no location | Location fields null. Alert still sent with student info. |
| EC-2 | Student not assigned to bus | Bus fields null. Transport staff not notified. |
| EC-3 | No admin online (no FCM token) | FCM fails, WhatsApp + SMS still delivered. Escalation triggers after 5 min. |
| EC-4 | Admin acknowledges after escalation | Status → acknowledged (from escalated). Parent notified. |
| EC-5 | Multiple SOS for same student | Each alert created independently. All tracked separately. |
| EC-6 | SOS triggered during school holidays | Alert still sent. Admin may not be active → escalation after 5 min. |
| EC-7 | Parent triggers SOS for child not theirs | Return 403. |
| EC-8 | Admin tries to resolve without notes | Return 400. Resolution notes required. |
| EC-9 | Teacher tries to resolve | Return 403. Only admin can resolve. |
| EC-10 | SOS triggered but WhatsApp/SMS gateway down | FCM still delivered. Log error. Escalation still works. |
| EC-11 | All notification channels fail | Alert created in DB. Escalation job will escalate after 5 min. Super admin notified. |
| EC-12 | Network error during trigger | Client retries. If still failing, show "Call school directly" message with phone number. |

---

## 19. Error Handling

### Error Response Format

Standard `ApiResponse` error format.

### Error Codes

| Code | HTTP Status | Description | User Message |
|---|---|---|---|
| `NOT_PARENT_OF_STUDENT` | 403 | Parent not linked to student | "You can only send SOS for your own children." |
| `STUDENT_NOT_FOUND` | 404 | Student ID not found | "Student not found." |
| `ALERT_NOT_FOUND` | 404 | Alert ID not found | "SOS alert not found." |
| `ALERT_ALREADY_ACKNOWLEDGED` | 409 | Alert already acknowledged | "This alert has already been acknowledged." |
| `ALERT_ALREADY_RESOLVED` | 409 | Alert already resolved | "This alert has already been resolved." |
| `RESOLUTION_NOTES_REQUIRED` | 400 | Empty resolution notes | "Resolution notes are required." |
| `NOT_AUTHORIZED` | 403 | Teacher trying to resolve | "Only admin can resolve SOS alerts." |

### Error Handling Strategy

- **Trigger failure:** Return error. Client shows "Call school directly" with phone number.
- **Notification failure:** Log error. Continue with other channels. Alert still created in DB.
- **Escalation failure:** Log error. Next minute's job retries.
- **Acknowledge/resolve failure:** Return error. Admin can retry.

### Retry Strategy

- SOS trigger: client retries 3 times on network error. If still failing, show "Call school" message.
- Notifications: SmartNotificationService retries FCM once. WhatsApp/SMS no retry (log error).
- Escalation job: no retry (next minute's job handles it).

### Fallback Behavior

- All notification channels fail: alert in DB, escalation job will escalate
- Trigger fails: show "Call school directly" with school phone number
- WhatsApp down: FCM + SMS still work
- SMS gateway down: FCM + WhatsApp still work
- FCM down: WhatsApp + SMS still work

---

## 20. Analytics & Reporting

### Analytics Dashboard Data

| Metric | Source | Derivation |
|---|---|---|
| Total SOS triggered | `sos_alerts` | Count per school per month |
| Avg acknowledgment time | `sos_alerts` | Avg(acknowledged_at - created_at) |
| Escalation rate | `sos_alerts` | Escalated / total alerts |
| Resolution rate | `sos_alerts` | Resolved / total alerts |
| Avg resolution time | `sos_alerts` | Avg(resolved_at - created_at) |
| Trigger source distribution | `sos_alerts` | Count by trigger_source |
| Active alerts (current) | `sos_alerts` | Count where status = active |

### Export Capabilities

- SOS audit report (CSV) — all alerts with full details, timestamps, resolution notes
- SOS summary report (CSV) — monthly summary by school

### Report Types

| Report | Format | Frequency | Recipient |
|---|---|---|---|
| SOS audit report | CSV | On-demand | Super Admin, Auditors |
| SOS summary | JSON (API) | Monthly | School Admin |
| Escalation analysis | JSON (API) | Monthly | Product Team |

---

## 21. Testing Strategy

### Unit Tests

- `SosService.trigger()` — alert creation, student info fetch, bus location fetch, notification dispatch
- `SosService.acknowledge()` — status update, parent notification
- `SosService.escalate()` — status update, super admin notification, emergency contact notification
- `SosService.resolve()` — status update, parent notification, resolution notes
- `SosEscalationJob.execute()` — timeout detection, escalation
- Status transition validation — active → acknowledged → resolved, active → escalated → acknowledged → resolved

### Integration Tests

- Full SOS lifecycle: trigger → acknowledge → resolve → verify status and notifications
- Escalation: trigger → wait 5 min → verify escalation → acknowledge → resolve
- Multi-channel delivery: trigger → verify FCM + WhatsApp + SMS sent
- Transport integration: trigger for student on bus → verify bus location in alert
- Parent notification on acknowledgment → verify parent receives notification
- Parent notification on resolution → verify parent receives notification
- Permission checks: parent can only trigger for own children, teacher can acknowledge but not resolve

### E2E Tests

- Parent triggers SOS → admin sees alert on dashboard → admin acknowledges → parent notified
- Parent triggers SOS → no acknowledgment → escalation after 5 min → super admin notified
- Admin resolves SOS → parent notified with resolution notes

### Performance Tests

- SOS trigger: < 2 seconds
- FCM delivery: < 5 seconds
- Escalation job: < 10 seconds
- Dashboard load: < 3 seconds

### Test Data

- 5 students with varying SOS scenarios (on bus, not on bus, with location, without location)
- 3 admin accounts (for notification delivery)
- 2 teacher accounts (class teachers)
- 1 super admin account
- Mock FCM, WhatsApp, SMS services
- Configured emergency contact and police helpline

### Test Environment

- Test database with SOS alerts table
- Pre-seeded student, admin, teacher, transport data
- Mock notification services (FCM, WhatsApp, SMS)
- Test JWT tokens for all roles
- Configurable escalation threshold (set to 1 minute for testing)

---

## 22. Acceptance Criteria

- [ ] Parent can trigger SOS with one tap (long-press confirmation)
- [ ] Alert sent to admin + teacher via FCM + WhatsApp + SMS
- [ ] Alert includes student info + location + bus location (if applicable)
- [ ] Admin SOS dashboard shows active alerts
- [ ] Admin can acknowledge alert → parent notified
- [ ] Escalation triggers after 5 min if not acknowledged
- [ ] Resolution with notes tracked
- [ ] SOS history available for audit
- [ ] Parent receives confirmation when alert acknowledged
- [ ] Parent receives notification when alert resolved
- [ ] Teacher can acknowledge but not resolve
- [ ] Super admin can view all schools' SOS alerts

---

## 23. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration `migration_085_sos_safety.sql`, Exposed table, register in `DatabaseFactory` |
| 2 | 2 days | `SosService` (trigger, acknowledge, escalate, resolve) |
| 3 | 1 day | `SosEscalationJob` (1-minute interval, 5-min threshold) |
| 4 | 1 day | Multi-channel notification integration (FCM + WhatsApp + SMS) |
| 5 | 1 day | API endpoints (parent + admin/teacher) |
| 6 | 3 days | Client UI: SOS FAB, `SosDashboardScreen`, `SosAlertDetailScreen`, `SosHistoryScreen` |
| 7 | 1 day | Tests: unit, integration, E2E |

### Pre-Implementation Checklist

- [ ] Verify `WHATSAPP_INTEGRATION_SPEC.md` is implemented and WhatsApp service available
- [ ] Verify `TRANSPORT_TRACKING_SPEC.md` is implemented and bus location available
- [ ] Verify `SmartNotificationService` supports "critical" priority
- [ ] Verify FCM, WhatsApp, SMS services are configured
- [ ] Verify admin and teacher accounts exist with FCM tokens
- [ ] Verify parent-student relationship exists in DB
- [ ] Verify school emergency contact configuration is available

---

## 24. File-Level Impact Analysis

### Server

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | `SosAlertsTable` |
| `server/.../db/DatabaseFactory.kt` | Modify | Register `SosAlertsTable` in `allTables` |
| `server/.../feature/sos/SosService.kt` | **New** | Core service (trigger, acknowledge, escalate, resolve) |
| `server/.../feature/sos/SosEscalationJob.kt` | **New** | Escalation timer job (1-min interval) |
| `server/.../feature/sos/SosRouting.kt` | **New** | API endpoints (parent + admin/teacher) |
| `docs/db/migration_085_sos_safety.sql` | **New** | DDL |

### Shared (KMP)

| File | Change Type | Description |
|---|---|---|
| `shared/.../sos/domain/model/SosModels.kt` | **New** | DTOs, domain models, enums |
| `shared/.../sos/domain/repository/SosRepository.kt` | **New** | Repository interface |
| `shared/.../sos/data/remote/SosApi.kt` | **New** | HTTP API definitions |

### Client (Compose)

| File | Change Type | Description |
|---|---|---|
| `composeApp/.../ui/v2/screens/parent/ParentHomeScreen.kt` | Modify | Add SOS FAB (red, prominent) |
| `composeApp/.../ui/v2/screens/admin/SosDashboardScreen.kt` | **New** | Admin SOS dashboard |
| `composeApp/.../ui/v2/screens/sos/SosAlertDetailScreen.kt` | **New** | Full alert detail view |
| `composeApp/.../ui/v2/screens/sos/SosHistoryScreen.kt` | **New** | SOS history log |
| `composeApp/.../ui/v2/components/SosFab.kt` | **New** | SOS floating action button component |

---

## 25. Future Enhancements

| # | Enhancement | Priority | Effort | Notes |
|---|---|---|---|---|
| F-1 | Student app SOS | Medium | M | SOS from student app (when student app is built) |
| F-2 | Two-way audio call | Medium | L | Voice communication between parent and admin during SOS |
| F-3 | Live location tracking | Medium | M | Real-time student location during SOS (if GPS available) |
| F-4 | Wearable device integration | Low | L | Integration with smartwatches for SOS trigger |
| F-5 | Automated emergency services | Low | L | Auto-call 112 / ambulance with location |
| F-6 | SOS drill mode | Low | S | Practice mode for schools to test SOS system |
| F-7 | Multi-language SOS alerts | Low | S | Alerts in regional languages for non-English admin |
| F-8 | SOS analytics dashboard | Low | M | Trends, response times, escalation patterns |
| F-9 | Geo-fenced SOS zones | Low | M | Auto-trigger SOS when student enters danger zone |
| F-10 | Integration with CCTV | Low | L | Auto-pull CCTV feed near SOS location |

---

## Appendix A: Sequence Diagrams

### A.1 SOS Trigger Flow

```
Parent (app)         Server                    DB              Notify
  │                    │                        │                │
  │  POST /parent/sos  │                        │                │
  │  {student_id, loc} │                        │                │
  │  ───────────────>  │                        │                │
  │                    │──verify parent-child──>│                │
  │                    │←──relationship ok──────│                │
  │                    │                        │                │
  │                    │──get student info──────>│                │
  │                    │←──name, class, photo───│                │
  │                    │                        │                │
  │                    │──get bus location──────>│ (if on bus)   │
  │                    │←──bus lat/lng──────────│                │
  │                    │                        │                │
  │                    │──insert sos_alert──────>│                │
  │                    │←──alert created────────│                │
  │                    │                        │                │
  │                    │──send FCM (critical)──────────────────>│
  │                    │──send WhatsApp────────────────────────>│
  │                    │──send SMS─────────────────────────────>│
  │                    │                        │                │
  │  ←──201: SosDto───│                        │                │
  │                    │                        │                │
  │  "Alert sent.      │                        │                │
  │   School notified."│                        │                │
  │                    │                        │                │
```

### A.2 SOS Escalation Flow

```
SosEscalationJob     Server              DB              Notify
  │                    │                  │                │
  │  execute()         │                  │                │
  │  ───────────────>  │                  │                │
  │                    │──SELECT active   │                │
  │                    │  alerts > 5 min──>│                │
  │                    │←──alert list─────│                │
  │                    │                  │                │
  │                    │──for each alert:  │                │
  │                    │  update status    │                │
  │                    │  = escalated──────>│                │
  │                    │←──success────────│                │
  │                    │                  │                │
  │                    │──notify super admin──────────────>│
  │                    │  (FCM+WhatsApp+SMS)               │
  │                    │──notify emergency contact────────>│
  │                    │  (SMS)            │                │
  │                    │                  │                │
  │  ←──count──────────│                  │                │
  │                    │                  │                │
```

### A.3 SOS Acknowledge and Resolve Flow

```
Admin (app)          Server              DB              Notify
  │                    │                  │                │
  │  POST /acknowledge │                  │                │
  │  ───────────────>  │                  │                │
  │                    │──update status──>│                │
  │                    │  = acknowledged  │                │
  │                    │←──success────────│                │
  │                    │                  │                │
  │                    │──notify parent───────────────────>│
  │                    │  "Acknowledged   │                │
  │                    │   by {admin}"    │                │
  │                    │                  │                │
  │  ←──200: SosDto───│                  │                │
  │                    │                  │                │
  │  POST /resolve     │                  │                │
  │  {notes}           │                  │                │
  │  ───────────────>  │                  │                │
  │                    │──update status──>│                │
  │                    │  = resolved      │                │
  │                    │←──success────────│                │
  │                    │                  │                │
  │                    │──notify parent───────────────────>│
  │                    │  "Resolved.      │                │
  │                    │   {notes}"       │                │
  │                    │                  │                │
  │  ←──200: SosDto───│                  │                │
  │                    │                  │                │
```

---

## Appendix B: Domain Model / ER Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                      sos_alerts (new)                                 │
│  id (PK)                                                              │
│  school_id, student_id                                                │
│  student_name, class_name                                             │
│  triggered_by, triggered_by_name                                      │
│  trigger_source (parent_app|student_app|bus|teacher)                  │
│  location_lat, location_lng                                           │
│  bus_id, bus_location_lat, bus_location_lng                           │
│  status (active|acknowledged|escalated|resolved)                      │
│  acknowledged_by, acknowledged_at                                     │
│  escalated_at                                                         │
│  resolved_by, resolved_at, resolution_notes                           │
│  created_at                                                           │
│  INDEX: (school_id, status, created_at DESC)                          │
└──────────────────────────────────────────────────────────────────────┘

Existing tables used:
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│ students         │  │ app_users        │  │ schools          │
│ (existing)       │  │ (existing)       │  │ (existing)       │
│ name, class,     │  │ admin, teacher,  │  │ emergency        │
│ photo, parent    │  │ super admin      │  │ contact config   │
└──────────────────┘  └──────────────────┘  └──────────────────┘
┌──────────────────┐  ┌──────────────────┐
│ buses            │  │ notifications    │
│ (existing)       │  │ (existing)       │
│ GPS location     │  │ notification     │
└──────────────────┘  └──────────────────┘
```

---

## Appendix C: Event Flow

### Domain Events

| Event | Emitter | Consumers | Payload | Side Effects |
|---|---|---|---|---|
| `SosTriggered` | `SosService.trigger()` | `SmartNotificationService` | `alertId, studentId, schoolId, triggeredByName` | Multi-channel notification to admin + teacher |
| `SosAcknowledged` | `SosService.acknowledge()` | `SmartNotificationService` | `alertId, acknowledgedByName` | Notify parent of acknowledgment |
| `SosEscalated` | `SosService.escalate()` | `SmartNotificationService` | `alertId, schoolId` | Notify super admin + emergency contact |
| `SosResolved` | `SosService.resolve()` | `SmartNotificationService` | `alertId, resolvedByName, resolutionNotes` | Notify parent of resolution |

### Event Delivery Guarantees

- SOS triggered event: emitted synchronously, notifications async (critical priority)
- SOS acknowledged event: emitted synchronously, notification async
- SOS escalated event: emitted within job execution, notifications async (critical priority)
- SOS resolved event: emitted synchronously, notification async

---

## Appendix D: Configuration

### Environment Variables

| Variable | Default | Description |
|---|---|---|
| `SOS_SAFETY_ENABLED` | `true` | Enable/disable SOS feature |
| `SOS_ESCALATION_THRESHOLD_MINUTES` | `5` | Minutes before auto-escalation |
| `SOS_ESCALATION_CHECK_INTERVAL_SECONDS` | `60` | Escalation job interval |
| `SOS_HISTORY_RETENTION_YEARS` | `7` | Data retention period for audit |
| `SOS_POLICE_HELPLINE_ENABLED` | `false` | Enable police helpline escalation |
| `SOS_POLICE_HELPLINE_NUMBER` | `112` | Police helpline number (if enabled) |

### Feature Flags

| Flag | Default | Description |
|---|---|---|
| `SOS_SAFETY_ENABLED` | `true` | Enable/disable SOS safety feature |

### School-Level Settings

| Setting | Default | Description |
|---|---|---|
| `sos_enabled` | `true` | Per-school enable/disable |
| `sos_escalation_threshold_minutes` | `5` | Per-school escalation threshold |
| `sos_emergency_contact_phone` | — | Emergency contact phone for escalation |
| `sos_police_helpline_enabled` | `false` | Per-school police helpline escalation |

---

## Appendix E: Migration & Rollback

### Migration: `migration_085_sos_safety.sql`

```sql
-- Migration 085: SOS Safety Alert
-- Creates sos_alerts table

BEGIN;

CREATE TABLE IF NOT EXISTS sos_alerts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    student_id      UUID NOT NULL,
    student_name    TEXT NOT NULL,
    class_name      TEXT,
    triggered_by    UUID NOT NULL,
    triggered_by_name TEXT NOT NULL,
    trigger_source  VARCHAR(16) NOT NULL,
    location_lat    DOUBLE PRECISION,
    location_lng    DOUBLE PRECISION,
    bus_id          UUID,
    bus_location_lat DOUBLE PRECISION,
    bus_location_lng DOUBLE PRECISION,
    status          VARCHAR(16) NOT NULL DEFAULT 'active',
    acknowledged_by UUID,
    acknowledged_at TIMESTAMP,
    escalated_at    TIMESTAMP,
    resolved_by     UUID,
    resolved_at     TIMESTAMP,
    resolution_notes TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_sos_alerts_school_status
    ON sos_alerts (school_id, status, created_at DESC);

COMMIT;
```

### Rollback: `migration_085_rollback.sql`

```sql
BEGIN;
DROP TABLE IF EXISTS sos_alerts;
COMMIT;
```

### Migration Validation

- Verify `sos_alerts` table created with correct columns
- Verify `idx_sos_alerts_school_status` index created
- Run `SELECT count(*) FROM sos_alerts` — should be 0 (new feature)

---

## Appendix F: Observability

### Structured Logging

| Log Level | Event | Context Fields |
|---|---|---|
| INFO | SOS triggered | `alertId, studentId, schoolId, triggeredByName, triggerSource` |
| INFO | SOS acknowledged | `alertId, acknowledgedByName, responseTimeSeconds` |
| WARN | SOS escalated | `alertId, schoolId, timeToEscalation` |
| INFO | SOS resolved | `alertId, resolvedByName, timeToResolution` |
| WARN | SOS notification failed (channel) | `alertId, channel, error` |
| ERROR | SOS trigger failed | `studentId, parentId, error` |
| ERROR | Escalation job failed | `error` |
| ERROR | All notification channels failed | `alertId` |

### Metrics

| Metric | Type | Labels | Description |
|---|---|---|---|
| `sos_triggered_total` | Counter | `school_id, trigger_source` | Total SOS alerts triggered |
| `sos_acknowledged_total` | Counter | `school_id` | Total SOS alerts acknowledged |
| `sos_escalated_total` | Counter | `school_id` | Total SOS alerts escalated |
| `sos_resolved_total` | Counter | `school_id` | Total SOS alerts resolved |
| `sos_active_count` | Gauge | `school_id` | Current active SOS alerts |
| `sos_response_time` | Histogram | `school_id` | Time from trigger to acknowledgment |
| `sos_resolution_time` | Histogram | `school_id` | Time from trigger to resolution |
| `sos_escalation_rate` | Gauge | `school_id` | Escalation rate (escalated / total) |

### Health Checks

| Check | Endpoint | Description |
|---|---|---|
| SOS service | `/health/sos` | Verify service and DB accessible |
| Escalation job | `/health/sos-escalation` | Verify last job run and status |

### Alerts

| Alert | Condition | Severity | Notification |
|---|---|---|---|
| Escalation job failed | Job error rate > 10% | Critical | Email + SMS to dev team |
| SOS notification all channels failed | All channels failed for an alert | Critical | Email to dev team + super admin |
| High escalation rate | > 50% alerts escalated | Warning | Email to admin (slow response?) |
| SOS service down | Health check failed | Critical | Email + SMS to dev team |

### Dashboards

| Dashboard | Panels | Audience |
|---|---|---|
| SOS Overview | Active count, triggered/month, escalation rate, resolution rate | Product Team |
| Response Performance | Avg response time, avg resolution time, escalation rate | School Admin |
| Job Performance | Escalation job duration, error rate | Dev Team |
| Notification Delivery | FCM/WhatsApp/SMS success rates per channel | Dev Team |

### Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| SOS triggered in non-emergency (false alarm) | Medium | Low | Long-press prevents accidental. Confirmation dialog. Admin can resolve quickly. |
| All notification channels fail | Low | Critical | Alert in DB. Escalation job will escalate. Fallback to phone call. |
| Admin doesn't see alert (no FCM token) | Medium | High | Multi-channel (WhatsApp + SMS). Escalation after 5 min. |
| Escalation job fails | Low | High | Runs every minute. Next run retries. Alert in DB. |
| Parent triggers SOS by mistake | Medium | Low | Long-press + confirmation. Admin resolves quickly. |
| SOS system unavailable | Low | Critical | Health check. Immediate alert to dev team. Fallback: school phone number shown. |
| Location data inaccurate | Medium | Medium | Bus location from GPS. Parent location from phone GPS. Best effort. |
| Privacy concern with location data | Low | Medium | Location only in SOS alerts. Restricted access. 7-year retention for audit. |
