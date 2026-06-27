# Event Registration — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** None

---

## 1. Feature Overview

Event registration and RSVP system for school events (Annual Day, Sports Day, PTM, workshops). Parents can register/RSPV for events, admin tracks attendance, and generates event reports.

### Goals

- Admin creates event with registration settings (capacity, deadline, RSVP required)
- Parent registers/RSPVs for events
- Admin views registration list, check-in at event
- Waitlist when capacity reached
- Event reminders sent to registered parents
- Post-event attendance report

---

## 2. Current System Assessment

- `CalendarEventsTable` — stores events but no registration/RSVP
- `feature_audit.csv` L159: Event Registration missing (0%)
- `NotificationsTable` — can send event reminders

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Admin creates event with: title, date, time, venue, description, capacity, registration_deadline, rsvp_required |
| FR-2 | Parent views upcoming events and registers/RSPVs (yes/no/maybe) |
| FR-3 | Capacity enforcement: waitlist when full |
| FR-4 | Admin views registration list (name, RSVP status, check-in status) |
| FR-5 | Event reminder sent 24h before event to registered parents |
| FR-6 | Admin checks in attendees at event (QR code or manual) |
| FR-7 | Post-event report: registered vs attended, no-show rate |

---

## 4. Database Design

```sql
CREATE TABLE event_registrations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    event_id        UUID NOT NULL REFERENCES calendar_events(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL,
    user_name       TEXT NOT NULL,
    user_role       VARCHAR(32) NOT NULL,
    student_id      UUID,                          -- if parent registering for event related to child
    student_name    TEXT,
    rsvp_status     VARCHAR(16) NOT NULL,          -- yes | no | maybe
    is_waitlisted   BOOLEAN NOT NULL DEFAULT false,
    checked_in      BOOLEAN NOT NULL DEFAULT false,
    checked_in_at   TIMESTAMP,
    registered_at   TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(event_id, user_id)
);
CREATE INDEX idx_event_registrations_event ON event_registrations(event_id, rsvp_status);
```

Modify `CalendarEventsTable`:
```sql
ALTER TABLE calendar_events ADD COLUMN requires_registration BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE calendar_events ADD COLUMN max_capacity INTEGER;
ALTER TABLE calendar_events ADD COLUMN registration_deadline TIMESTAMP;
ALTER TABLE calendar_events ADD COLUMN venue TEXT;
```

---

## 5. API Contracts

```
# Admin
GET /api/v1/school/events/{id}/registrations
POST /api/v1/school/events/{id}/checkin  { user_id }
GET /api/v1/school/events/{id}/report

# Parent
GET /api/v1/parent/events
POST /api/v1/parent/events/{id}/register  { rsvp_status, student_id }
PATCH /api/v1/parent/events/{id}/register  { rsvp_status }  -- update RSVP
```

---

## 6. Acceptance Criteria

- [ ] Admin creates event with registration settings
- [ ] Parent registers/RSPVs for events
- [ ] Waitlist activates when capacity reached
- [ ] Admin views registration list
- [ ] Event reminder sent 24h before
- [ ] Check-in works (manual or QR)
- [ ] Post-event report generated

---

## 7. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration, Exposed table |
| 2 | 2 days | EventRegistrationService (register, waitlist, check-in) |
| 3 | 1 day | Reminder notification integration |
| 4 | 1 day | API endpoints |
| 5 | 2 days | Client UI (event list, registration, admin check-in, report) |
| 6 | 1 day | Tests |

---

## 8. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add + Modify | `EventRegistrationsTable` + columns on calendar_events |
| `server/.../feature/events/EventRegistrationService.kt` | New | Core service |
| `docs/db/migration_064_event_registration.sql` | New | DDL |
| `composeApp/.../ui/v2/screens/parent/EventRegistrationScreen.kt` | New | Parent event view |
| `composeApp/.../ui/v2/screens/admin/EventCheckInScreen.kt` | New | Admin check-in |
