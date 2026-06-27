# Calendar Sync (Google/Outlook) — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** None

---

## 1. Feature Overview

Sync school calendar events (PTM, exams, holidays, events) to parents' and teachers' personal Google Calendar or Outlook Calendar via ICS feed and OAuth-based sync.

### Goals

- Generate per-user ICS feed URL (subscribe in any calendar app)
- OAuth-based Google Calendar sync (push events to user's calendar)
- Events synced: PTM, exams, holidays, school events, assignment due dates
- Two modes: subscribe (read-only ICS) or connected (OAuth push)
- Auto-refresh: ICS feed updates when events change

---

## 2. Current System Assessment

- `CalendarEventsTable` (`Tables.kt:1370-1395`) — school calendar with `title`, `description`, `date`, `endDate`, `type`, `audienceType`, `classIds`
- `AcademicCalendarTable` — holiday/event calendar
- `feature_audit.csv` L143: Calendar Sync missing (0%)
- No ICS generation, no OAuth integration

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Generate per-user ICS feed URL (authenticated, unique token) |
| FR-2 | ICS feed includes events relevant to user (based on role + class) |
| FR-3 | Google Calendar OAuth: connect → push events to user's primary calendar |
| FR-4 | Events synced: PTM, exams, holidays, school events, assignment due dates |
| FR-5 | ICS feed auto-updates when events are added/modified/deleted |
| FR-6 | User can disconnect calendar sync |
| FR-7 | Sync frequency: ICS (client polls), OAuth (push on event change + daily refresh) |

---

## 4. Database Design

```sql
CREATE TABLE calendar_sync_tokens (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL UNIQUE,
    school_id       UUID NOT NULL,
    ics_token       TEXT NOT NULL UNIQUE,          -- random token for ICS URL auth
    google_refresh_token TEXT,                     -- encrypted OAuth refresh token
    google_calendar_id TEXT,                       -- user's calendar ID
    sync_provider   VARCHAR(16) NOT NULL DEFAULT 'ics', -- ics | google
    is_active       BOOLEAN NOT NULL DEFAULT true,
    last_synced_at  TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
```

---

## 5. Backend Architecture

### 5.1 IcsFeedService

```kotlin
class IcsFeedService {
    suspend fun generateIcs(userToken: String): String {
        // 1. Resolve user from token
        // 2. Fetch relevant events (CalendarEventsTable + AcademicCalendarTable + Homework due dates)
        // 3. Filter by audience (role + class)
        // 4. Generate ICS format (VCALENDAR with VEVENT entries)
        // 5. Return as text/calendar
    }
}
```

### 5.2 GoogleCalendarSyncService

```kotlin
class GoogleCalendarSyncService {
    suspend fun connect(userId: UUID, authCode: String): CalendarSyncToken
    suspend fun pushEvents(userId: UUID)  // push all relevant events to Google Calendar
    suspend fun disconnect(userId: UUID)
}
```

### 5.3 Event Change Trigger

When `CalendarEventsTable` is modified:
1. Find all users with active Google Calendar sync for that school
2. For each user, check if event is relevant (audience filter)
3. Push create/update/delete to Google Calendar API

---

## 6. API Contracts

```
# ICS Feed (no auth header, token in URL)
GET /api/v1/calendar/ics/{token}

# Google OAuth
GET /api/v1/parent/calendar/google/auth-url  -- returns OAuth URL
POST /api/v1/parent/calendar/google/callback  { auth_code }  -- exchange for tokens
DELETE /api/v1/parent/calendar/sync  -- disconnect

# Status
GET /api/v1/parent/calendar/sync-status
```

---

## 7. Acceptance Criteria

- [ ] ICS feed URL generated per user with unique token
- [ ] ICS feed contains relevant events (filtered by role + class)
- [ ] ICS feed updates when events change
- [ ] Google Calendar OAuth connection works
- [ ] Events pushed to Google Calendar on creation/modification
- [ ] User can disconnect sync
- [ ] Assignment due dates included in feed

---

## 8. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration, Exposed table |
| 2 | 2 days | IcsFeedService (ICS generation, event filtering) |
| 3 | 3 days | GoogleCalendarSyncService (OAuth, push, event change trigger) |
| 4 | 1 day | API endpoints |
| 5 | 2 days | Client UI (sync settings, connect/disconnect, ICS URL copy) |
| 6 | 1 day | Tests |

---

## 9. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | `CalendarSyncTokensTable` |
| `server/.../feature/calendar/IcsFeedService.kt` | New | ICS generation |
| `server/.../feature/calendar/GoogleCalendarSyncService.kt` | New | OAuth sync |
| `server/.../feature/calendar/CalendarSyncRouting.kt` | New | API endpoints |
| `docs/db/migration_062_calendar_sync.sql` | New | DDL |
| `composeApp/.../ui/v2/screens/parent/CalendarSyncSettingsScreen.kt` | New | Sync settings UI |
