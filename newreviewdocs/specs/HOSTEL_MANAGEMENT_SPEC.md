# Hostel Management — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** None

---

## 1. Feature Overview

Hostel/boarding management: room allocation, student hostel assignments, attendance, visitor logging, and hostel fee integration.

### Goals

- Admin manages hostel buildings, rooms, and bed capacity
- Assign students to rooms with check-in/check-out dates
- Daily hostel attendance (present/absent/leave)
- Visitor log with approval
- Hostel fee auto-created in `FeeRecordsTable`

---

## 2. Current System Assessment

- `feature_audit.csv` Gap #8: Hostel missing (0%)
- No hostel tables in `Tables.kt`

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Manage hostel buildings, floors, rooms, beds |
| FR-2 | Assign students to beds with check-in/check-out |
| FR-3 | Daily hostel attendance |
| FR-4 | Visitor log with approval workflow |
| FR-5 | Hostel fee auto-created |
| FR-6 | Hostel warden dashboard |

---

## 4. Database Design

```sql
CREATE TABLE hostel_buildings (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    name            TEXT NOT NULL,
    warden_id       UUID,                          -- FK app_users.id
    warden_name     TEXT,
    capacity        INTEGER NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE hostel_rooms (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    building_id     UUID NOT NULL REFERENCES hostel_buildings(id) ON DELETE CASCADE,
    room_number     VARCHAR(16) NOT NULL,
    floor           INTEGER NOT NULL DEFAULT 1,
    capacity        INTEGER NOT NULL DEFAULT 2,
    occupied        INTEGER NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    UNIQUE(building_id, room_number)
);

CREATE TABLE hostel_assignments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    student_id      UUID NOT NULL,
    room_id         UUID NOT NULL REFERENCES hostel_rooms(id),
    check_in_date   DATE NOT NULL,
    check_out_date  DATE,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE hostel_attendance (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    student_id      UUID NOT NULL,
    date            DATE NOT NULL,
    status          VARCHAR(16) NOT NULL,          -- present | absent | leave
    marked_by       UUID,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(school_id, student_id, date)
);

CREATE TABLE hostel_visitors (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    student_id      UUID NOT NULL,
    visitor_name    TEXT NOT NULL,
    visitor_phone   VARCHAR(32),
    relation        TEXT,
    purpose         TEXT,
    check_in_time   TIMESTAMP NOT NULL,
    check_out_time  TIMESTAMP,
    approved_by     UUID,
    status          VARCHAR(16) NOT NULL DEFAULT 'pending', -- pending | approved | rejected | checked_in | checked_out
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
```

---

## 5. API Contracts

```
GET/POST /api/v1/school/hostel/buildings
GET/POST /api/v1/school/hostel/rooms
POST /api/v1/school/hostel/assign  { student_id, room_id }
POST /api/v1/school/hostel/attendance  { date, assignments: [{student_id, status}] }
GET/POST /api/v1/school/hostel/visitors
POST /api/v1/school/hostel/visitors/{id}/approve
```

---

## 6. Acceptance Criteria

- [ ] Buildings, rooms, beds managed
- [ ] Students assigned to rooms
- [ ] Daily hostel attendance tracked
- [ ] Visitor log with approval
- [ ] Hostel fee auto-created
- [ ] Warden dashboard with occupancy and attendance

---

## 7. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 2 days | DB migration, Exposed tables, services |
| 2 | 2 days | API endpoints |
| 3 | 3 days | Client UI (building/room management, assignment, attendance, visitors) |
| 4 | 1 day | Tests |

---

## 8. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | 5 hostel tables |
| `server/.../feature/hostel/*.kt` | New | Services + routing |
| `docs/db/migration_047_hostel.sql` | New | DDL |
| `composeApp/.../ui/v2/screens/admin/HostelScreen.kt` | New | Hostel management |
