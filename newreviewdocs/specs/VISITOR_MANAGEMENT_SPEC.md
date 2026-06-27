# Visitor Management — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** None

---

## 1. Feature Overview

Digital visitor management for schools: visitor check-in/check-out, purpose tracking, host approval, visitor pass generation, and visitor log reports.

### Goals

- Front desk checks in visitors (name, phone, purpose, host person, photo)
- Host (teacher/admin) receives approval notification
- Visitor pass generated (QR code or printed badge)
- Check-out tracking with duration
- Visitor log with search and filters
- Blacklist management (blocked visitors)

---

## 2. Current System Assessment

- `feature_audit.csv` L140: Visitor Management missing (0%)
- No visitor tables in `Tables.kt`

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Check-in: visitor name, phone, email, purpose, host person, photo, organization |
| FR-2 | Host receives notification for approval (FCM + in-app) |
| FR-3 | Visitor pass: QR code + name + host + purpose + check-in time |
| FR-4 | Check-out: scan QR or manual, records duration |
| FR-5 | Visitor log: searchable by date, name, purpose, host |
| FR-6 | Blacklist: blocked visitors flagged at check-in |
| FR-7 | Daily/weekly/monthly visitor report |

---

## 4. Database Design

```sql
CREATE TABLE visitors (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    name            TEXT NOT NULL,
    phone           VARCHAR(32),
    email           TEXT,
    organization    TEXT,
    photo_url       TEXT,
    is_blacklisted  BOOLEAN NOT NULL DEFAULT false,
    blacklist_reason TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE visitor_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    visitor_id      UUID NOT NULL REFERENCES visitors(id),
    visitor_name    TEXT NOT NULL,
    visitor_phone   VARCHAR(32),
    purpose         TEXT NOT NULL,                 -- "Parent Meeting", "Delivery", "Inspection"
    host_id         UUID,                          -- FK app_users.id (teacher/admin being visited)
    host_name       TEXT NOT NULL,
    check_in_time   TIMESTAMP NOT NULL,
    check_out_time  TIMESTAMP,
    duration_minutes INTEGER,                      -- calculated on check-out
    status          VARCHAR(16) NOT NULL DEFAULT 'checked_in', -- checked_in | checked_out
    pass_qr_data    TEXT NOT NULL,                 -- QR code content for visitor pass
    approved        BOOLEAN NOT NULL DEFAULT false,
    approved_at     TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_visitor_logs_school_date ON visitor_logs(school_id, check_in_time DESC);
CREATE INDEX idx_visitor_logs_host ON visitor_logs(host_id, status);
```

---

## 5. API Contracts

```
# Admin/Front Desk
POST /api/v1/school/visitors/check-in  { name, phone, purpose, host_id, photo_url }
POST /api/v1/school/visitors/check-out  { visitor_log_id }
GET /api/v1/school/visitors/logs?date={YYYY-MM-DD}&host_id={uuid}
GET /api/v1/school/visitors/report?from={}&to={}

# Host approval
POST /api/v1/teacher/visitors/{id}/approve
POST /api/v1/teacher/visitors/{id}/reject

# Blacklist
POST /api/v1/school/visitors/{id}/blacklist  { reason }
DELETE /api/v1/school/visitors/{id}/blacklist
```

---

## 6. Acceptance Criteria

- [ ] Visitor check-in with photo, purpose, host
- [ ] Host receives approval notification
- [ ] Visitor pass with QR code generated
- [ ] Check-out records duration
- [ ] Visitor log searchable
- [ ] Blacklist prevents check-in
- [ ] Reports available (daily/weekly/monthly)

---

## 7. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration, Exposed tables |
| 2 | 2 days | VisitorService (check-in/out, approval, blacklist) |
| 3 | 1 day | QR code generation for visitor pass |
| 4 | 1 day | API endpoints |
| 5 | 2 days | Client UI (check-in form, visitor pass, log, reports) |
| 6 | 1 day | Tests |

---

## 8. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | 2 visitor tables |
| `server/.../feature/visitor/VisitorService.kt` | New | Core service |
| `docs/db/migration_065_visitor_management.sql` | New | DDL |
| `composeApp/.../ui/v2/screens/admin/VisitorManagementScreen.kt` | New | Visitor management UI |
