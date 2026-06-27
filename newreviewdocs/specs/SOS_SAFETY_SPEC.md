# SOS Safety Alert — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** `WHATSAPP_INTEGRATION_SPEC.md`, `TRANSPORT_TRACKING_SPEC.md`
> **Source:** `DIFFERENTIATING_FEATURES.md` §6.3

---

## 1. Feature Overview

Emergency SOS system for students during school hours and transport: one-tap SOS button that alerts school admin, class teacher, and designated guardians with location, student info, and timestamp. Integrates with transport tracking for bus emergencies.

### Goals

- One-tap SOS from parent app (on behalf of child) or future student app
- Immediate alerts to: school admin, class teacher, transport staff (if on bus)
- Alert includes: student name, class, current location (if available), timestamp, last known bus location
- Multi-channel: FCM (instant) + WhatsApp + SMS
- Admin SOS dashboard with active alerts
- Alert escalation: if not acknowledged in 5 min → escalate to super admin + police helpline (configurable)
- Resolution tracking: admin marks alert as resolved with notes

---

## 2. Current System Assessment

- `TRANSPORT_TRACKING_SPEC.md` — bus GPS tracking (location context for transport SOS)
- `NotificationsTable` — notification infrastructure
- `WHATSAPP_INTEGRATION_SPEC.md` — WhatsApp for instant alerts
- No SOS/emergency system exists
- `DIFFERENTIATING_FEATURES.md` §6.3: SOS Safety, effort M

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | SOS button in parent app (prominent, accessible from any screen via shake gesture or floating button) |
| FR-2 | On trigger: alert school admin + class teacher + transport staff (if student assigned to bus) |
| FR-3 | Alert payload: student name, class, photo, parent contact, last known bus location, timestamp |
| FR-4 | Multi-channel delivery: FCM push (critical priority) + WhatsApp + SMS |
| FR-5 | Admin SOS dashboard: active alerts with student info + acknowledge button |
| FR-6 | Escalation: if not acknowledged in 5 min → escalate to super admin + configurable emergency contact |
| FR-7 | Resolution: admin marks resolved with notes + action taken |
| FR-8 | Parent receives confirmation when alert acknowledged |
| FR-9 | SOS history log for audit |

---

## 4. Database Design

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

---

## 5. Backend Architecture

### 5.1 SosService

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
}
```

### 5.2 Escalation Timer

Background job every 1 minute:
1. `SELECT * FROM sos_alerts WHERE status = 'active' AND created_at < now() - 5 minutes`
2. For each: call `escalate(alertId)`

---

## 6. API Contracts

```
# Parent
POST /api/v1/parent/sos  { student_id, location_lat, location_lng }

# Admin/Teacher
GET /api/v1/school/sos/active
POST /api/v1/school/sos/{id}/acknowledge
POST /api/v1/school/sos/{id}/resolve  { resolution_notes }
GET /api/v1/school/sos/history
```

---

## 7. Frontend Architecture

### 7.1 SOS Button

- Floating action button (FAB) on parent home screen — red, prominent
- Long-press (2 seconds) to trigger (prevents accidental activation)
- Confirmation dialog: "Send SOS alert for {child_name}?"
- On confirm: immediate trigger + show "Alert sent. School has been notified."

### 7.2 Admin SOS Dashboard

- Red banner at top of admin dashboard when active SOS exists
- Full-screen SOS alert view with student photo, info, location map
- Acknowledge button (large, prominent)
- Resolution form

---

## 8. Acceptance Criteria

- [ ] Parent can trigger SOS with one tap (long-press confirmation)
- [ ] Alert sent to admin + teacher via FCM + WhatsApp + SMS
- [ ] Alert includes student info + location + bus location (if applicable)
- [ ] Admin SOS dashboard shows active alerts
- [ ] Admin can acknowledge alert → parent notified
- [ ] Escalation triggers after 5 min if not acknowledged
- [ ] Resolution with notes tracked
- [ ] SOS history available for audit

---

## 9. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration, Exposed table |
| 2 | 2 days | SosService (trigger, acknowledge, escalate, resolve) |
| 3 | 1 day | Escalation timer job |
| 4 | 1 day | Multi-channel notification integration |
| 5 | 1 day | API endpoints |
| 6 | 3 days | Client UI (SOS button, admin dashboard, resolution form) |
| 7 | 1 day | Tests |

---

## 10. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | `SosAlertsTable` |
| `server/.../feature/sos/SosService.kt` | New | Core service |
| `server/.../feature/sos/SosEscalationJob.kt` | New | Escalation timer |
| `server/.../feature/sos/SosRouting.kt` | New | API endpoints |
| `docs/db/migration_084_sos_safety.sql` | New | DDL |
| `composeApp/.../ui/v2/screens/parent/ParentHomeScreen.kt` | Modify | Add SOS FAB |
| `composeApp/.../ui/v2/screens/admin/SosDashboardScreen.kt` | New | Admin SOS dashboard |
