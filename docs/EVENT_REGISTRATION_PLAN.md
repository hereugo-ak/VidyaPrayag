# Event Registration System — Master Plan

> **Feature #155** from `feature_audit.csv` → evolved into a full school-wide Event Registration & RSVP platform.
> **Status:** PLANNING → BUILD READY
> **Last updated:** 2026-06-30

---

## 0. Executive Summary

The audit identified that the existing PTM module is **admin-only** — admins can schedule PTM events and track class progress, but **parents cannot book slots** and **teachers are not notified**. This plan expands feature #155 from "parent PTM slot booking" into a **complete Event Registration & RSVP system** that handles any school event type (PTM, sports day, cultural fest, annual day, workshop, parent workshop, health camp, etc.) with structured time-slot booking, capacity management, calendar integration, and cross-portal notifications.

### Design Philosophy

We are building an **operating system for schools**. Every feature must:
- Be **role-aware** (Parent, Teacher, School Admin) with clean separation
- Follow **SOLID + MVVM + Clean Architecture** (per `DEVELOPMENT_STANDARDS.md`)
- Be **offline-resilient** (Room cache for reads, outbox for writes — per offline mode initiative)
- Be **accessible to 25-60 year old parents** (large touch targets, clear language, minimal steps)
- Integrate with existing systems (Calendar, Notifications, Announcements) — **no silos**

---

## 1. Deep System Audit

### 1.1 Existing PTM Infrastructure

| Layer | File | Status |
|---|---|---|
| **DB Tables** | `Tables.kt:715-749` — `PtmEventsTable`, `PtmClassProgressTable` | ✅ Exists, admin-only schema |
| **Server Routes** | `PtmRouting.kt` — `GET/POST /api/v1/school/ptm`, `PATCH metrics`, `PUT class-progress`, `POST complete` | ✅ Admin-only, no parent/teacher endpoints |
| **Shared Models** | `PtmModels.kt` — `PtmActiveEventDto`, `PtmHistoryDto`, `PtmClassProgressDto`, `CreatePtmRequest` | ✅ Admin models only |
| **Shared API** | `PtmApi.kt` — `getPtm()`, `createPtm()` | ✅ Admin API only |
| **Shared Repository** | `PtmRepository.kt` / `PtmRepositoryImpl.kt` | ✅ Admin repo only |
| **Admin ViewModel** | `SchedulePTMViewModel.kt` | ✅ Works |
| **Admin UI** | `SchedulePtmScreenV2.kt` | ✅ Works |
| **Parent UI** | ❌ **MISSING** | Parents have no PTM/event booking screen |
| **Teacher UI** | ❌ **MISSING** | Teachers have no visibility into PTM registrations |
| **Slot Management** | ❌ **MISSING** | `slot` is a free-text string ("09:00 - 13:00"), no structured slots |
| **RSVP/Registration** | ❌ **MISSING** | No registration table, no status tracking |

### 1.2 Existing Calendar Infrastructure

| Component | Location | Relevance |
|---|---|---|
| `CalendarEventsTable` | `Tables.kt:1689-1727` | **Already has** type `PTM`, `SCHOOL_EVENT`, `ACTIVITY` — events live here |
| `AcademicCalendarCore.kt` | `feature/calendar/` | Shared event creation + conflict detection |
| `AcademicCalendarRouting.kt` | `feature/calendar/` | Admin CRUD for calendar events |
| Parent Calendar View | `AcademicCalendarScreenV2.kt` | Read-only view — **no registration action** |
| `NotificationScheduler.kt` | `feature/notifications/` | Sends reminders for published calendar events (T-1 day) |

### 1.3 Existing Notification Infrastructure

| Component | Location | Capability |
|---|---|---|
| `Notify.kt` | `feature/notifications/` | `toUser()`, `toUsers()` — push + in-app notification |
| `NotifyRecipients.kt` | `feature/notifications/` | `parentsOfStudent()`, `parentsInSchool()` — recipient resolution |
| `NotificationScheduler.kt` | `feature/notifications/` | Hourly job: fee reminders + calendar reminders |
| Notification deep links | `DeepLinkTarget` | Supports `ParentTab(tab, overlay)`, `TeacherScreen(screen)` |
| In-app notification inbox | `NotificationsScreenV2` | All 3 portals have notification overlays |

### 1.4 Portal Navigation Integration Points

**Parent Portal** (`ParentPortalV2.kt`):
- `ParentOverlay` enum (line 56) — needs new `Events` / `EventRegistration` overlay
- Home tab has quick-link cards — needs "Upcoming Events" card
- Calendar overlay exists but is read-only — needs "Register" action

**Teacher Portal** (`TeacherPortalV2.kt`):
- `TeacherOverlay` enum (line 30) — needs new `EventRegistrations` / `PTMSlots` overlay
- Today tab shows schedule — needs "PTM bookings today" strip
- Profile tab — needs "My PTM Schedule" link

**Admin Portal** (`SchoolPortalV2.kt`):
- `SchoolOverlay` enum (line 35-74) — already has `SchedulePTM`
- Comms tab — needs "Event Registration" management
- Calendar platform — needs "Enable Registration" toggle on event creation

### 1.5 Architecture Patterns (per `DEVELOPMENT_STANDARDS.md`)

```
shared/commonMain/
  feature/event/
    domain/
      model/EventRegistrationModels.kt       ← @Serializable DTOs
      repository/EventRegistrationRepository.kt  ← interface
    data/
      remote/EventRegistrationApi.kt         ← Ktor client
      repository/EventRegistrationRepositoryImpl.kt
    presentation/
      ParentEventRegistrationViewModel.kt
      TeacherEventRegistrationViewModel.kt
      AdminEventRegistrationViewModel.kt

server/src/main/kotlin/
  feature/event/EventRegistrationRouting.kt   ← Ktor routes
  db/Tables.kt (add EventRegistrationsTable, EventSlotsTable)

composeApp/src/commonMain/kotlin/
  ui/v2/screens/
    parent/ParentEventRegistrationScreenV2.kt
    teacher/TeacherEventRegistrationScreenV2.kt
    school/AdminEventRegistrationScreenV2.kt
```

**DI wiring**: All new classes registered in `Koin.kt` — APIs as `single`, repositories as `single<Interface>`, ViewModels as `factory`.

---

## 2. UX Analysis — Parent Persona (25-60 years old)

### 2.1 Core UX Principles

| Principle | Implementation |
|---|---|
| **Large touch targets** | Min 48dp tap zones, 56dp primary CTA buttons |
| **Clear language** | "Register for Event" not "RSVP", "Cancel Registration" not "Revoke RSVP" |
| **Minimal steps** | Event list → tap event → tap slot → confirm. **3 taps max.** |
| **Visual confirmation** | Green checkmark + "You're registered!" animation on success |
| **Calendar integration** | Show event date in parent's calendar with "Registered" badge |
| **Reminder** | Push notification T-1 day + T-1 hour before event |
| **Cancel with reason** | Simple dropdown: "Can't make it" / "Schedule conflict" / "Other" |
| **Multi-child aware** | If parent has multiple children, show which child the event is for |
| **Offline** | Registrations work offline, sync when online (outbox pattern) |
| **Accessibility** | WCAG 2.1 AA: high contrast (4.5:1 ratio), 16sp min body text, 20sp headings, screen reader labels on all interactive elements, haptic feedback on confirm actions, focus indicators visible, reduce-motion respected |
| **Language simplicity** | All labels at 6th-grade reading level. No jargon. "Register" not "RSVP", "Cancel" not "Revoke" |
| **Error recovery** | Every error has a plain-language explanation + a suggested next action ("This slot is full. Try another time below.") |

### 2.2 User Journey — Parent Registers for PTM

```
1. Parent opens app → Home tab
2. "Upcoming Events" card shows next PTM with "Register" button
3. Tap "Register" → Event detail screen
4. See: Event title, date, location, description
5. See: Available time slots (e.g., 09:00-09:15, 09:15-09:30, ...)
6. Tap a slot → "Confirm Registration" button
7. Tap confirm → Green checkmark + "Registered for 09:00-09:15 slot"
8. Push notification sent to: class teacher + admin
9. Calendar updated with "Registered" badge
10. T-1 day reminder: "PTM tomorrow at 09:00"
```

### 2.3 User Journey — Parent Registers for School Event (non-PTM)

```
1. Parent opens Calendar → sees "Annual Day" event
2. Tap event → Event detail with "Register" button (if registration enabled)
3. Tap "Register" → Choose number of attendees (1-4 family members)
4. Tap "Confirm" → Green checkmark + "Registered: 3 attendees"
5. Push notification: "You're registered for Annual Day"
6. Calendar shows "Registered" badge
```

### 2.4 Edge Cases Considered

| Scenario | Handling |
|---|---|
| Parent has multiple children, event is class-specific | Show child selector, register per-child |
| Slot is full while parent is viewing | Optimistic lock: server rejects → show "Slot full, pick another" |
| Parent tries to register after deadline | Disable register button, show "Registration closed" |
| Parent cancels registration | Allow cancel until T-1 day, notify teacher + admin |
| Parent is offline | Queue in outbox, sync when online, show "Pending" state |
| Teacher hasn't been assigned to class yet | Admin sees "No class teacher" warning when creating PTM slots |
| Event is cancelled by admin | Auto-cancel all registrations, push notification to all registered parents |
| Parent already registered for same event | Show "Already registered" with cancel/change slot option |
| Event has no slots (open event) | Skip slot selection, just "Register" / "Cancel" |
| Parent has conflicting event at same time | Show warning: "You have another event at this time: [Event Name]. Register anyway?" |
| Parent changes slot (reschedule) | Allow until deadline. Old slot freed, new slot booked atomically. Notify teacher of change. |
| Parent has low-end Android phone | Minimal images, lazy-load event banners, text-first design, <2MB data per session |
| Parent has poor internet | Timeout after 10s, show cached data with "Refreshing..." indicator, never block UI |
| Parent is visually impaired | TalkBack/VoiceOver compatible: all buttons have contentDescription, slot list is navigable |
| Parent speaks regional language | (Future) Event title/description in parent's preferred language — store multi-lang fields |

---

## 3. Database Schema

### 3.1 New Tables

#### `event_slots` — Structured time slots for an event

```sql
CREATE TABLE event_slots (
    id              UUID PRIMARY KEY,
    event_id        UUID NOT NULL,                    -- FK calendar_events.id
    start_time      VARCHAR(8) NOT NULL,              -- HH:mm (24h)
    end_time        VARCHAR(8) NOT NULL,              -- HH:mm (24h)
    capacity        INTEGER NOT NULL DEFAULT 1,       -- max registrations per slot
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL,
    UNIQUE(event_id, start_time)
);
```

#### `event_registrations` — Parent registrations for events

```sql
CREATE TABLE event_registrations (
    id              UUID PRIMARY KEY,
    event_id        UUID NOT NULL,                    -- FK calendar_events.id
    slot_id         UUID NULL,                        -- FK event_slots.id (NULL for open events)
    parent_user_id  UUID NOT NULL,                    -- FK app_users.id
    student_id      UUID NULL,                        -- FK students.id (for class-specific events)
    school_id       UUID NOT NULL,                    -- FK schools.id (tenant scope)
    attendee_count  INTEGER NOT NULL DEFAULT 1,       -- number of family members attending
    status          VARCHAR(16) NOT NULL DEFAULT 'REGISTERED',  -- REGISTERED | CANCELLED | WAITLISTED | CHECKED_IN
    cancel_reason   TEXT NULL,
    registered_at   TIMESTAMP NOT NULL,
    cancelled_at    TIMESTAMP NULL,
    updated_at      TIMESTAMP NOT NULL,
    UNIQUE(event_id, parent_user_id, student_id)      -- one registration per parent per event per child
);
```

### 3.2 Existing Table Changes

**`CalendarEventsTable`** — add columns:
```sql
ALTER TABLE calendar_events ADD COLUMN registration_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE calendar_events ADD COLUMN registration_deadline VARCHAR(12) NULL;  -- YYYY-MM-DD
ALTER TABLE calendar_events ADD COLUMN max_attendees INTEGER NULL;              -- overall cap (null = unlimited)
ALTER TABLE calendar_events ADD COLUMN venue TEXT NULL;                         -- location text
```

### 3.3 Migration Strategy

- **Migration N → N+1**: Add `event_slots` + `event_registrations` tables + alter `calendar_events`
- Use Exposed `SchemaUtils.create()` in `DatabaseFactory.init()` (existing pattern)
- No destructive changes — all additive
- PTM events in `calendar_events` with `type = 'PTM'` automatically gain registration capability

---

## 4. API Design

### 4.1 Parent Endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/parent/events` | List events with registration enabled (parent's school + child's class) |
| `GET` | `/api/v1/parent/events/{eventId}` | Event detail with slots + parent's registration status |
| `POST` | `/api/v1/parent/events/{eventId}/register` | Register for event (with optional slot_id + attendee_count) |
| `DELETE` | `/api/v1/parent/events/{eventId}/register` | Cancel registration (with optional cancel_reason) |
| `GET` | `/api/v1/parent/events/registrations` | List parent's own registrations (all statuses) |
| `PATCH` | `/api/v1/parent/events/{eventId}/reschedule` | Change slot for existing registration (within deadline) |

### 4.2 Teacher Endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/teacher/events/ptm` | List PTM events for teacher's classes + registration counts |
| `GET` | `/api/v1/teacher/events/ptm/{eventId}` | PTM event detail with slot-wise parent bookings |
| `GET` | `/api/v1/teacher/events/ptm/{eventId}/slots` | Slot list with booked/total counts |
| `PATCH` | `/api/v1/teacher/events/ptm/{eventId}/checkin/{registrationId}` | Check-in a parent at PTM |

### 4.3 Admin Endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/school/events/registrations` | List all registrations (filterable by event, status, class) |
| `GET` | `/api/v1/school/events/{eventId}/registrations` | Event-wise registration list with attendee details |
| `POST` | `/api/v1/school/events/{eventId}/slots` | Create time slots for an event |
| `POST` | `/api/v1/school/events/{eventId}/slots/auto-generate` | Auto-generate slots from time range + duration |
| `PUT` | `/api/v1/school/events/{eventId}/slots/{slotId}` | Update slot (capacity, time) |
| `DELETE` | `/api/v1/school/events/{eventId}/slots/{slotId}` | Delete slot (fails if registrations exist) |
| `PATCH` | `/api/v1/school/events/{eventId}/registration-status` | Enable/disable registration, set deadline |
| `POST` | `/api/v1/school/events/{eventId}/cancel` | Cancel event → auto-cancel all registrations + notify |
| `GET` | `/api/v1/school/events/{eventId}/registrations/export` | Export registrations as CSV |

### 4.4 Request/Response DTOs

```kotlin
// ── Parent ──
@Serializable
data class ParentEventDto(
    val id: String,
    val title: String,
    val description: String,
    val type: String,                         // PTM, SCHOOL_EVENT, ACTIVITY, etc.
    val startDate: String,
    val endDate: String,
    val allDay: Boolean,
    val venue: String? = null,
    val registrationEnabled: Boolean,
    val registrationDeadline: String? = null,
    val hasSlots: Boolean,                    // true if event_slots exist
    val maxAttendees: Int? = null,
    val audience: String,                     // ALL_SCHOOL, CLASSES, etc.
    val classIds: List<String> = emptyList(),
    val myRegistrationStatus: String? = null, // REGISTERED, CANCELLED, null
    val mySlotId: String? = null,
    val myAttendeeCount: Int? = null,
    val bannerUrl: String? = null,              // event banner image (from calendar_events)
    val icon: String? = null,                   // event icon (from calendar_events)
    val conflictingEventId: String? = null,     // if parent has another event at same time
    val conflictingEventTitle: String? = null
)

@Serializable
data class EventSlotDto(
    val id: String,
    val startTime: String,                    // HH:mm
    val endTime: String,                      // HH:mm
    val capacity: Int,
    val bookedCount: Int,
    val isFull: Boolean,
    val myRegistration: Boolean               // is this slot booked by me
)

@Serializable
data class RegisterRequest(
    val slotId: String? = null,               // null for open events
    val studentId: String? = null,            // for class-specific events
    val attendeeCount: Int = 1
)

@Serializable
data class CancelRegistrationRequest(
    val reason: String? = null
)

@Serializable
data class RescheduleRequest(
    val newSlotId: String                       // must be a valid, non-full slot for same event
)

@Serializable
data class RegistrationDto(
    val id: String,
    val eventId: String,
    val eventTitle: String,
    val eventDate: String,
    val slotStartTime: String? = null,
    val slotEndTime: String? = null,
    val attendeeCount: Int,
    val status: String,
    val registeredAt: String,
    val cancelledAt: String? = null
)

// ── Teacher ──
@Serializable
data class TeacherPtmEventDto(
    val id: String,
    val title: String,
    val date: String,
    val className: String,
    val totalRegistrations: Int,
    val checkedIn: Int,
    val slots: List<TeacherSlotDto>
)

@Serializable
data class TeacherSlotDto(
    val id: String,
    val startTime: String,
    val endTime: String,
    val capacity: Int,
    val bookedCount: Int,
    val bookings: List<SlotBookingDto>
)

@Serializable
data class SlotBookingDto(
    val registrationId: String,
    val parentName: String,
    val studentName: String,
    val attendeeCount: Int,
    val status: String,                       // REGISTERED, CHECKED_IN
    val registeredAt: String
)

// ── Admin ──
@Serializable
data class CreateSlotRequest(
    val startTime: String,                    // HH:mm
    val endTime: String,                      // HH:mm
    val capacity: Int = 1
)

@Serializable
data class AutoGenerateSlotsRequest(
    val rangeStart: String,                   // HH:mm (e.g. "09:00")
    val rangeEnd: String,                     // HH:mm (e.g. "13:00")
    val slotDurationMinutes: Int,             // e.g. 15 → 09:00-09:15, 09:15-09:30, ...
    val capacity: Int = 1,                    // parents per slot (1 for PTM, more for workshops)
    val breakAfterSlots: Int = 0,             // insert a break every N slots (e.g. every 4 = 5-min break)
    val breakDurationMinutes: Int = 5
)

@Serializable
data class AdminRegistrationDto(
    val id: String,
    val eventTitle: String,
    val eventDate: String,
    val parentName: String,
    val parentMobile: String,
    val studentName: String?,
    val slotTime: String?,                    // "09:00-09:15"
    val attendeeCount: Int,
    val status: String,
    val registeredAt: String
)

@Serializable
data class UpdateRegistrationConfigRequest(
    val registrationEnabled: Boolean? = null,
    val registrationDeadline: String? = null,
    val maxAttendees: Int? = null,
    val venue: String? = null
)
```

---

## 5. End-to-End Architecture Flow

### 5.1 Admin Creates Event with Registration

```
Admin → Academic Calendar Platform → Create Event (type=PTM, registrationEnabled=true)
  ↓
Server: AcademicCalendarCore.createCalendarEvent() → inserts into calendar_events
  ↓
Server: Admin creates slots via POST /api/v1/school/events/{eventId}/slots
  ↓
Server: Inserts into event_slots
  ↓
Server: Notify.toUsers() → parents in target audience (push: "New event: PTM on July 15")
  ↓
Server: Notify.toUsers() → class teachers (push: "PTM scheduled for your class")
  ↓
Calendar event visible to parents in calendar + events list
```

### 5.2 Parent Registers for Event

```
Parent → Home tab → "Upcoming Events" card → tap → Event detail
  ↓
ParentEventRegistrationViewModel.loadEventDetail(eventId)
  ↓
EventRegistrationApi.getEventDetail(token, eventId) → GET /api/v1/parent/events/{eventId}
  ↓
Server: query calendar_events + event_slots + event_registrations (parent's status)
  ↓
Parent sees: event info + available slots + their registration status
  ↓
Parent taps slot → taps "Confirm Registration"
  ↓
EventRegistrationApi.register(token, eventId, RegisterRequest(slotId, studentId))
  ↓
Server: POST /api/v1/parent/events/{eventId}/register
  ↓
Server: validate (deadline, capacity, duplicate check) → insert event_registrations
  ↓
Server: Notify.toUser(classTeacherId, "PTM: {parent} booked slot 09:00-09:15")
  ↓
Server: Return RegistrationDto (status=REGISTERED)
  ↓
ViewModel updates state → UI shows green checkmark + "Registered!"
  ↓
If offline: queue in outbox → sync engine retries when online
```

### 5.3 Teacher Views PTM Bookings

```
Teacher → Today tab → "PTM Today" strip (if any PTM scheduled today)
  ↓
TeacherEventRegistrationViewModel.loadPtmEvents()
  ↓
EventRegistrationApi.getTeacherPtmEvents(token) → GET /api/v1/teacher/events/ptm
  ↓
Server: query calendar_events (type=PTM, teacher's classes) + count registrations
  ↓
Teacher sees: PTM list with registration counts
  ↓
Teacher taps PTM → detail screen with slot-wise bookings
  ↓
Teacher sees: each slot with parent name, student name, status
  ↓
On PTM day: teacher taps "Check-in" next to each parent
  ↓
EventRegistrationApi.checkinParent(token, eventId, registrationId)
  ↓
Server: PATCH → update status to CHECKED_IN
```

### 5.4 Admin Manages Registrations

```
Admin → Comms tab → "Event Registration" overlay
  ↓
AdminEventRegistrationViewModel.loadRegistrations()
  ↓
EventRegistrationApi.getAdminRegistrations(token) → GET /api/v1/school/events/registrations
  ↓
Admin sees: all registrations with filters (event, status, class)
  ↓
Admin can: export CSV, cancel event (auto-cancel all + notify), view analytics
```

### 5.5 Notification Flow

```
Event Created → Notify parents (audience-filtered) + teachers
Parent Registers → Notify class teacher + admin
Parent Cancels → Notify class teacher
Event Cancelled by Admin → Notify all registered parents + teachers
T-1 Day Reminder → NotificationScheduler → Notify registered parents
T-1 Hour Reminder → (future) Notify registered parents
Post-Event → (future) Notify parents who didn't attend with "Missed PTM" follow-up
```

### 5.6 Parent Reschedule Flow

```
Parent → Event detail → already registered → taps "Change Slot"
  ↓
UI shows available slots (excluding current slot, highlighting non-full ones)
  ↓
Parent taps new slot → taps "Confirm Change"
  ↓
EventRegistrationApi.reschedule(token, eventId, RescheduleRequest(newSlotId))
  ↓
Server: PATCH /api/v1/parent/events/{eventId}/reschedule
  ↓
Server: validate (deadline, new slot capacity, same event) → atomic transaction:
         1. Free old slot (decrement bookedCount)
         2. Book new slot (increment bookedCount, check capacity)
         3. Update event_registrations.slot_id
  ↓
Server: Notify.toUser(classTeacherId, "PTM: {parent} changed to slot 09:30-09:45")
  ↓
Server: Return updated RegistrationDto
  ↓
UI shows: "Slot changed to 09:30-09:45" + green checkmark
```

### 5.7 Offline Support (Future Phase)

```
Parent registers offline:
  → Write to Room outbox (operation=EVENT_REGISTER, payload={eventId, slotId, studentId, clientRequestId=UUID})
  → UI shows "Pending" state with spinner
  → SyncEngine drains outbox when online
  → Server: idempotency check on clientRequestId → no duplicate registration
  → Server processes → if slot full, dead-letter + show error notification with "Pick another slot" deep link

Parent views events offline:
  → Room cache: EventEntity, EventSlotEntity, RegistrationEntity
  → Cache-then-network: show cached, refresh from API
  → Storage cap: 50 events, LRU by date
```

---

## 6. Implementation Phases

### Phase 1 — Server Foundation (Backend)

| Step | File | Description |
|---|---|---|
| 1.1 | `Tables.kt` | Add `EventSlotsTable`, `EventRegistrationsTable` + alter `CalendarEventsTable` |
| 1.2 | `DatabaseFactory.kt` | Register new tables in `SchemaUtils.create()` |
| 1.3 | `EventRegistrationRouting.kt` | Parent endpoints (5 routes) |
| 1.4 | `EventRegistrationRouting.kt` | Teacher endpoints (4 routes) |
| 1.5 | `EventRegistrationRouting.kt` | Admin endpoints (8 routes) |
| 1.6 | `Application.kt` | Mount `eventRegistrationRouting()` |
| 1.7 | `NotificationScheduler.kt` | Add event registration reminders (T-1 day for registered parents) |
| 1.8 | `PtmRouting.kt` | Bridge: when PTM created via legacy endpoint, auto-create calendar event (type=PTM) + enable registration. Legacy `ptm_events` table kept for back-compat — new registrations go through `event_registrations` |
| 1.9 | `EventRegistrationRouting.kt` | Auto-slot generation endpoint (POST /slots/auto-generate) |

### Phase 2 — Shared Layer (KMP)

| Step | File | Description |
|---|---|---|
| 2.1 | `EventRegistrationModels.kt` | All DTOs (parent, teacher, admin) |
| 2.2 | `EventRegistrationApi.kt` | Ktor client — all endpoints |
| 2.3 | `EventRegistrationRepository.kt` | Interface |
| 2.4 | `EventRegistrationRepositoryImpl.kt` | Implementation |
| 2.5 | `Koin.kt` | Register API (single), repository (single<Interface>) |

### Phase 3 — Admin UI

| Step | File | Description |
|---|---|---|
| 3.1 | `AdminEventRegistrationScreenV2.kt` | Registration management list + filters |
| 3.2 | `AdminEventRegistrationViewModel.kt` | Admin VM |
| 3.3 | `Koin.kt` | Register admin VM |
| 3.4 | `SchoolPortalV2.kt` | Add `EventRegistration` overlay + Comms tab link |
| 3.5 | `CreateEventScreenV2.kt` | Add "Enable Registration" toggle + slot creation step (manual + auto-generate) |
| 3.6 | `SchedulePtmScreenV2.kt` | Add "Create Slots" button → slot creation dialog with auto-generate option (duration picker, time range, capacity) |
| 3.7 | `AdminEventRegistrationScreenV2.kt` | Add analytics strip: registration rate, slot fill %, checked-in count |

### Phase 4 — Parent UI

| Step | File | Description |
|---|---|---|
| 4.1 | `ParentEventRegistrationScreenV2.kt` | Event list + detail + slot picker + register/cancel/reschedule. Banner image lazy-loaded. Conflict warning if overlapping events. |
| 4.2 | `ParentEventRegistrationViewModel.kt` | Parent VM |
| 4.3 | `Koin.kt` | Register parent VM |
| 4.4 | `ParentPortalV2.kt` | Add `Events` overlay + Home tab "Upcoming Events" card |
| 4.5 | `ParentHomeScreenV2.kt` | Add "Upcoming Events" quick-link card |
| 4.6 | `AcademicCalendarScreenV2.kt` | Add "Register" button on events with registration enabled + "Registered" badge |
| 4.7 | `ParentEventRegistrationScreenV2.kt` | Reschedule flow: "Change Slot" button for registered events (within deadline) |

### Phase 5 — Teacher UI

| Step | File | Description |
|---|---|---|
| 5.1 | `TeacherEventRegistrationScreenV2.kt` | PTM event list + slot-wise bookings + check-in |
| 5.2 | `TeacherEventRegistrationViewModel.kt` | Teacher VM |
| 5.3 | `Koin.kt` | Register teacher VM |
| 5.4 | `TeacherPortalV2.kt` | Add `EventRegistrations` overlay |
| 5.5 | Teacher Today tab | Add "PTM Today" strip (if PTM scheduled today) |

### Phase 6 — Notifications & Calendar Integration

| Step | File | Description |
|---|---|---|
| 6.1 | `EventRegistrationRouting.kt` | Trigger `Notify.toUser()` on register/cancel/reschedule/cancel-event |
| 6.2 | `NotificationScheduler.kt` | Add T-1 day reminder for registered parents (separate from general calendar reminder) |
| 6.3 | `DeepLinkTarget` | Add `ParentTab("home", "events")` deep link for event notifications |
| 6.4 | `AcademicCalendarScreenV2.kt` | Show "Registered" badge on calendar events |
| 6.5 | `EventRegistrationRouting.kt` | Idempotency: accept `X-Client-Request-Id` header on register endpoint, deduplicate |
| 6.6 | `EventRegistrationRouting.kt` | SMS fallback: if parent has no FCM token, send SMS via OTP gateway for critical event notifications |

### Phase 7 — Offline Support (Future)

| Step | Description |
|---|---|
| 7.1 | Room entities: `EventEntity`, `EventSlotEntity`, `RegistrationEntity` |
| 7.2 | Room DAOs + data sources (cache-then-network) |
| 7.3 | Outbox handler for `EVENT_REGISTER` + `EVENT_CANCEL` operations |
| 7.4 | Storage caps: 50 events, LRU by date |
| 7.5 | Migration: AppDatabase v5 |

### Phase 8 — Class Teacher Notification (Future)

| Step | Description |
|---|---|
| 8.1 | When `class_teacher` field is introduced in `teacher_assignments` table, auto-resolve class teacher for PTM events |
| 8.2 | Notify class teacher when: PTM created, parent registers, parent cancels |
| 8.3 | Teacher UI: "My Classes' PTM" filter |

---

## 7. SOLID Compliance Checklist

| Principle | How Applied |
|---|---|
| **S** — Single Responsibility | `EventRegistrationApi` only does HTTP. `EventRegistrationRepository` only does data coordination. `ParentEventRegistrationViewModel` only manages parent UI state. |
| **O** — Open/Closed | New event types are added by extending `EventType` constants — no code changes to registration logic. New notification triggers added via `Notify` without modifying existing channels. |
| **L** — Liskov Substitution | `EventRegistrationRepositoryImpl` fully substitutes `EventRegistrationRepository` interface. Any future Room-backed impl also substitutes. |
| **I** — Interface Segregation | Separate repository interfaces could be: `ParentEventRepository`, `TeacherEventRepository`, `AdminEventRepository` — but we keep one interface with clear method groups since they share the same domain. VMs only call methods they need. |
| **D** — Dependency Inversion | VMs depend on `EventRegistrationRepository` (interface), resolved via Koin. API depends on `HttpClient` (injected). Server routes depend on `DatabaseFactory.dbQuery` (abstracted). |

## 8. MVVM Compliance Checklist

| Layer | Rule | Compliance |
|---|---|---|
| **Model** | `@Serializable` data classes, no logic | ✅ DTOs in `EventRegistrationModels.kt` |
| **Repository** | Interface + Impl, coordinates remote + local | ✅ `EventRegistrationRepository` + Impl |
| **ViewModel** | Exposes `StateFlow` only, no Compose imports | ✅ `*EventRegistrationViewModel` |
| **View** | Stateless composables, receives state + callbacks | ✅ `*EventRegistrationScreenV2.kt` |
| **DI** | All wired in `Koin.kt` | ✅ API single, repo single, VM factory |

---

## 9. Security & Validation

| Concern | Mitigation |
|---|---|
| Parent registers for event not in their school | Server: verify `parent.school_id == event.school_id` |
| Parent registers for event not for their child's class | Server: check `event.audience` + `class_ids` against parent's children |
| Duplicate registration | DB unique constraint: `(event_id, parent_user_id, student_id)` |
| Slot overbooking | Server: atomic check `bookedCount < capacity` inside transaction |
| Registration after deadline | Server: compare `LocalDate.now()` with `registration_deadline` |
| Teacher checks in wrong parent | Server: verify teacher teaches the student's class |
| Admin cancels event | Server: bulk update all registrations to CANCELLED + notify |
| CSV export injection | Server: sanitize names, use proper CSV escaping |

---

## 10. Testing Strategy

### 10.1 Server Tests

```kotlin
// EventRegistrationRoutingTest.kt
- "Parent can register for open event"
- "Parent can register for slotted event"
- "Parent cannot register twice for same event"
- "Parent cannot register for full slot"
- "Parent cannot register after deadline"
- "Parent can cancel registration"
- "Teacher can view PTM bookings for their classes"
- "Teacher cannot view PTM bookings for other classes"
- "Teacher can check-in parent"
- "Admin can create slots"
- "Admin can cancel event → all registrations cancelled"
- "Admin can export registrations CSV"
```

### 10.2 Shared Tests

```kotlin
// EventRegistrationRepositoryTest.kt
- "Repository delegates to API for register"
- "Repository delegates to API for cancel"
- "Repository handles NetworkResult.Error"
- "Repository handles NetworkResult.ConnectionError"
```

### 10.3 Build Verification

```bash
# JVM
./gradlew :shared:compileKotlinJvm :shared:jvmTest

# Android
./gradlew :shared:compileDevDebugKotlinAndroid :composeApp:compileDevDebugKotlinAndroid

# Set JAVA_HOME to JDK 21
```

---

## 11. Loop Review — Self-Audit

### Review Round 1

| Check | Finding | Action |
|---|---|---|
| Are we limited to PTM? | No — design supports any `CalendarEventsTable` type | ✅ Good |
| Is calendar integration clean? | Yes — events live in `calendar_events`, registrations are a separate concern | ✅ Good |
| Are notifications wired? | Yes — `Notify.toUser()` on register/cancel/cancel-event | ✅ Good |
| Is offline support planned? | Yes — Phase 7, follows existing outbox pattern | ✅ Good |
| Is class teacher notification future-proof? | Yes — Phase 8, deferred until `class_teacher` field exists | ✅ Good |
| Are we creating a separate event table? | No — reusing `calendar_events` with `registration_enabled` flag | ✅ Good, avoids duplication |
| Is the parent UX truly 3-tap? | Event list → tap event → tap slot → confirm = 3 taps | ✅ Good |
| Is the slot model flexible? | `capacity` defaults to 1 (PTM) but can be higher (workshop) | ✅ Good |
| What about waitlisting? | Added `WAITLISTED` status — auto-promote on cancel (future) | ✅ Noted |
| Is CSV export safe? | Yes — server-side with proper escaping | ✅ Good |

### Review Round 2 — Enhancement Suggestions (implemented in this revision)

| # | Suggestion | Priority | Status |
|---|---|---|---|
| 1 | **Reschedule flow**: Parent can change slot without cancelling | High | ✅ Added to API (§4.1), DTOs (§4.4), flow (§5.6), Phase 4.7 |
| 2 | **Auto-slot generation**: Admin defines duration + range → auto-generate | High | ✅ Added to API (§4.3), DTOs (§4.4), Phase 1.9, 3.5, 3.6 |
| 3 | **Conflict detection**: Warn parent about overlapping events | High | ✅ Added to edge cases (§2.4), ParentEventDto (§4.4) |
| 4 | **Idempotency**: Prevent duplicate registrations from offline sync | High | ✅ Added to offline flow (§5.7), Phase 6.5 |
| 5 | **SMS fallback**: Notify parents without smartphones | Medium | ✅ Added to Phase 6.6 |
| 6 | **Enhanced accessibility**: WCAG 2.1 AA compliance | High | ✅ Enhanced §2.1 |
| 7 | **Low-end device support**: Minimal data, text-first | High | ✅ Added to edge cases (§2.4) |
| 8 | **Error recovery**: Plain-language errors + next actions | High | ✅ Added to §2.1 |
| 9 | **Admin analytics strip**: Registration rate, fill % | Medium | ✅ Added to Phase 3.7 |
| 10 | **PTM bridge strategy**: Explicit back-compat plan | High | ✅ Added to Phase 1.8 |

### Review Round 3 — Future Enhancement Suggestions (not yet implemented)

| # | Suggestion | Priority | Phase |
|---|---|---|---|
| 1 | **QR code check-in**: Teacher scans parent's QR code instead of manual check-in | Medium | Future |
| 2 | **Reminder cascade**: T-3 days (email), T-1 day (push), T-1 hour (push) | Medium | Future |
| 3 | **Post-event feedback**: After event, parent gets "How was it?" 1-5 star rating | Low | Future |
| 4 | **Analytics dashboard**: Registration rate, no-show rate, per-class turnout | Medium | Future |
| 5 | **Multi-language event descriptions**: Store title/description in multiple languages | Low | Future |
| 6 | **Recurring events**: "PTM every 2nd Saturday" → auto-create events + slots | Medium | Future |
| 7 | **Calendar sync (iCal)**: Export registered events to Google/Outlook calendar | Low | Future (feature #152) |
| 8 | **Capacity waitlist auto-promote**: When parent cancels, first waitlisted parent auto-promoted + notified | Medium | Future |
| 9 | **WhatsApp integration**: Send event reminders via WhatsApp Business API | Medium | Future |
| 10 | **Post-event follow-up**: Notify parents who didn't attend with "Missed PTM" + reschedule link | Low | Future |

### Review Round 4 — Integration Audit

| Integration Point | Status | Notes |
|---|---|---|
| `CalendarEventsTable` | ✅ Reused | No new event table — `registration_enabled` flag added |
| `PtmEventsTable` | ⚠️ Bridge | Phase 1.8 bridges PTM → calendar_events. Legacy table kept for back-compat |
| `Notify.kt` | ✅ Integrated | All register/cancel/cancel-event triggers use `Notify.toUser()` / `toUsers()` |
| `NotificationScheduler.kt` | ✅ Extended | T-1 day reminder for registered parents added |
| `DeepLinkTarget` | ✅ Extended | New `events` overlay deep link |
| `Koin.kt` | ✅ Wired | All new classes registered |
| `ParentPortalV2.kt` | ✅ Extended | New `Events` overlay |
| `TeacherPortalV2.kt` | ✅ Extended | New `EventRegistrations` overlay |
| `SchoolPortalV2.kt` | ✅ Extended | New `EventRegistration` overlay |
| `AcademicCalendarScreenV2.kt` | ✅ Enhanced | "Register" button on events with registration enabled |
| `CreateEventScreenV2.kt` | ✅ Enhanced | "Enable Registration" toggle + slot creation |
| Offline (Room/Outbox) | 📋 Planned | Phase 7 — follows existing SyncEngine pattern |
| Class Teacher (future) | 📋 Planned | Phase 8 — deferred until `class_teacher` field exists |

---

## 12. God-Mode Agent Prompt

> Paste the block below as the system/first message for an AI agent (GLM, GPT, Claude, etc.) to execute this plan through a strict Plan→Build→Test→Review→Iterate loop.

```text
ROLE
You are a principal Kotlin Multiplatform engineer working in the Vidya Prayag repo
(KMP + Compose MP + Ktor server, Koin DI, Room, MVVM, Clean Architecture). You execute
the Event Registration system defined in EVENT_REGISTRATION_PLAN.md. You write
production-grade, minimal, no-junk code that already-passing builds would accept.

GROUND TRUTH (read before acting; never contradict)
- EVENT_REGISTRATION_PLAN.md ......... the authoritative plan, phases, contracts, DTOs.
- DEVELOPMENT_STANDARDS.md ........... the mandatory file/DI/layering conventions.
- shared/.../core/network/NetworkResult.kt + safeApiCall .... reuse; ConnectionError == offline.
- shared/.../di/Koin.kt + platformModule.* ........ all wiring goes here, nowhere else.
- server/.../db/Tables.kt ............ Exposed ORM table definitions (add new tables here).
- server/.../db/DatabaseFactory.kt ... SchemaUtils.create() registration (add new tables here).
- server/.../Application.kt .......... route mounting (add eventRegistrationRouting() here).
- server/.../feature/notifications/Notify.kt .... Notify.toUser() / Notify.toUsers() for push.
- server/.../feature/calendar/AcademicCalendarCore.kt ... reuse createCalendarEvent() for event creation.
- composeApp/.../ui/v2/screens/parent/ParentPortalV2.kt ... ParentOverlay enum (add Events).
- composeApp/.../ui/v2/screens/teacher/TeacherPortalV2.kt .. TeacherOverlay enum (add EventRegistrations).
- composeApp/.../ui/v2/screens/school/SchoolPortalV2.kt .... SchoolOverlay enum (add EventRegistration).
- Room lives in roomMain (android/jvm/ios). WEB (js/wasmJs) HAS NO ROOM → must no-op.
- Build commands: ./gradlew :shared:compileKotlinJvm :shared:jvmTest
  ./gradlew :shared:compileDevDebugKotlinAndroid :composeApp:compileDevDebugKotlinAndroid
  Set JAVA_HOME to JDK 21: C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot

NON-NEGOTIABLE PRINCIPLES
- SOLID: one responsibility per class; extend by adding handlers, never by editing core logic
  (Open/Closed); depend on interfaces resolved via Koin (DIP).
- MVVM: ViewModel exposes StateFlow only; Composables stay stateless; no Compose imports in VMs.
- Clean Architecture: Domain (models + interfaces) → Data (API + impl) → Presentation (VMs) → UI.
- Reuse: events live in calendar_events (NOT a new table). Registrations are a separate table.
- No-failure: every function is total. No uncaught exceptions. Every write is idempotent
  (X-Client-Request-Id header on register endpoint → deduplicate offline retries).
- Reschedule: parent can change slot via PATCH /reschedule — atomic transaction (free old,
  book new, update registration). Notify teacher of change.
- Auto-slot generation: POST /slots/auto-generate takes rangeStart, rangeEnd, slotDurationMinutes,
  capacity, optional breaks. Server generates slot rows.
- SMS fallback: if parent has no FCM device token, send critical event notifications via SMS
  using the existing OTP gateway (feature/auth/OtpAdminRouting.kt).
- NO junk code: no placeholder TODOs, no dead classes, no speculative abstraction beyond
  the plan. If it isn't wired into Koin and used, don't write it.
- Parent UX: 3-tap registration max. Large touch targets (48dp min). Clear language.
  WCAG 2.1 AA: high contrast, screen reader labels, 16sp min body text. Low-end device:
  text-first, lazy-load images, <2MB data per session.

THE LOOP (you MUST follow this graph for every work item)
1. PLAN  — restate the single next item from EVENT_REGISTRATION_PLAN.md §6 you will do.
           List the exact files you will add/change and why. Stop and confirm scope.
2. BUILD — implement only that item. Follow existing patterns verbatim (look at PtmRouting.kt,
           PtmApi.kt, PtmRepository.kt, SchedulePTMViewModel.kt for the pattern). Web target gets
           an in-memory/no-op implementation so it still compiles.
3. TEST  — write/extend server tests (EventRegistrationRoutingTest.kt) and shared tests
           (EventRegistrationRepositoryTest.kt). Then run:
             ./gradlew :shared:compileKotlinJvm :shared:jvmTest
             ./gradlew :shared:compileDevDebugKotlinAndroid :composeApp:compileDevDebugKotlinAndroid
4. REVIEW — self-audit against: (a) SOLID/MVVM checklist (§7, §8), (b) security checklist (§9),
           (c) DEVELOPMENT_STANDARDS, (d) "is there any junk / duplication / unused code?",
           (e) does it integrate with existing Calendar + Notification systems cleanly?
           Output findings explicitly.
5. ITERATE — if REVIEW finds ANY gap or any build/test fails: go back to BUILD with the
           findings as the new spec. Only when the item is "ideal" (compiles on all
           targets, tests green, zero junk) do you COMMIT and move to the next §6 item.
           Repeat until the phase is complete.

OUTPUT FORMAT (every turn)
- ## NODE: <PLAN|BUILD|TEST|REVIEW|ITERATE>
- What you did / are about to do (concise).
- Code in full file blocks with correct package paths (no diffs-with-ellipsis for new files).
- For TEST: the exact gradle commands + expected/observed result.
- For REVIEW: a checked list (SOLID, MVVM, security, junk-scan, integration) with PASS/FAIL.
- End with: NEXT ACTION = <stay in loop / advance to item X / phase complete>.

HARD STOPS (ask, don't guess)
- If a server change is needed and the endpoint contract is unknown, stop and request the route spec.
- If a conflict policy for registration data is ambiguous, default to server-authoritative + re-queue
  and flag it for human review. Never silently overwrite.
- Never run a destructive migration on existing tables. All changes must be additive.
- If the calendar_events table needs new columns, use ALTER TABLE ADD COLUMN (nullable with defaults).

EXECUTION ORDER
Phase 1 (Server) → Phase 2 (Shared) → Phase 3 (Admin UI) → Phase 4 (Parent UI) →
Phase 5 (Teacher UI) → Phase 6 (Notifications & Calendar Integration)
(Phases 7-8 are future — do NOT implement unless explicitly asked.)

DEFINITION OF DONE (per item)
[] compiles: jvm + android   [] jvmTest green   [] follows DEVELOPMENT_STANDARDS
[] Koin-wired & used   [] zero junk/dead/duplicate code
[] integrates with existing Calendar + Notification systems   [] committed.

Begin at NODE: PLAN with Phase 1, item 1.1 from EVENT_REGISTRATION_PLAN.md §6.
```

---

## 13. Build & Audit Log

> This section is updated as implementation progresses. Each phase completion appends a summary.

### Phase 1 — Server Foundation
- **Status:** NOT STARTED
- **Started:** —
- **Completed:** —
- **Audit:** —

### Phase 2 — Shared Layer
- **Status:** NOT STARTED
- **Started:** —
- **Completed:** —
- **Audit:** —

### Phase 3 — Admin UI
- **Status:** NOT STARTED
- **Started:** —
- **Completed:** —
- **Audit:** —

### Phase 4 — Parent UI
- **Status:** NOT STARTED
- **Started:** —
- **Completed:** —
- **Audit:** —

### Phase 5 — Teacher UI
- **Status:** NOT STARTED
- **Started:** —
- **Completed:** —
- **Audit:** —

### Phase 6 — Notifications & Calendar Integration
- **Status:** NOT STARTED
- **Started:** —
- **Completed:** —
- **Audit:** —

---

## 14. References

| Document | Path |
|---|---|
| Development Standards | `DEVELOPMENT_STANDARDS.md` |
| Feature Audit | `feature_audit.csv` (row 155) |
| GLM God-Mode Prompt (reference) | `docs/GLM_GODMODE_PROMPT.md` |
| Calendar Spec | `docs/dashboard-revamp/CALENDAR_SPEC.md` |
| Notification System Spec | `NOTIFICATION_SYSTEM_SPEC.md` |
| Offline Mode Plan | (completed — see memory) |
| PtmRouting.kt | `server/.../feature/school/PtmRouting.kt` |
| AcademicCalendarCore.kt | `server/.../feature/calendar/AcademicCalendarCore.kt` |
| Notify.kt | `server/.../feature/notifications/Notify.kt` |
| ParentPortalV2.kt | `composeApp/.../ui/v2/screens/parent/ParentPortalV2.kt` |
| TeacherPortalV2.kt | `composeApp/.../ui/v2/screens/teacher/TeacherPortalV2.kt` |
| SchoolPortalV2.kt | `composeApp/.../ui/v2/screens/school/SchoolPortalV2.kt` |
| Koin.kt | `shared/.../di/Koin.kt` |
| Tables.kt | `server/.../db/Tables.kt` |
