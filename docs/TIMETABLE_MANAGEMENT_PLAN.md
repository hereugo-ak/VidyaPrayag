# Timetable Management — Full Feature Plan

## Problem
`teacher_periods` table has zero POST/PUT/DELETE endpoints. Nobody can create
or edit timetables through the app. `PeriodExceptionsTable` has no API either.

## Solution: Three-layer timetable management

### Layer 1: Admin CRUD for periods (direct, no approval)
Admin assigns teachers to weekly recurring slots.

**Endpoints** (extend `SchoolTimetableRouting.kt`):
- `POST   /api/v1/school/timetable/periods` — create a period
- `PUT    /api/v1/school/timetable/periods/{id}` — update (time, room, active)
- `DELETE /api/v1/school/timetable/periods/{id}` — soft-deactivate

**Body**: `assignmentId` (FK → teacher_subject_assignments), weekday, startTime,
endTime, room, validFrom?, validTo?

**Validation**:
- Assignment must exist and belong to the school
- No double-booking (ux_periods_no_double_book index)
- Weekday 1-7, time format HH:mm

### Layer 2: Period exceptions (admin or teacher)
One-off overrides for a specific date.

**Endpoints** (new `PeriodExceptionRouting.kt`):
- `GET    /api/v1/school/timetable/exceptions?date=YYYY-MM-DD` — list
- `POST   /api/v1/school/timetable/exceptions` — create
- `DELETE /api/v1/school/timetable/exceptions/{id}` — delete

**Kinds**: CANCELLED, RESCHEDULED, ROOM_CHANGE, SUBSTITUTION, EXTRA

**Teacher endpoints** (same, scoped to teacher's own periods):
- `POST   /api/v1/teacher/timetable/exceptions` — teacher creates exception
- `GET    /api/v1/teacher/timetable/exceptions?date=` — teacher views own

### Layer 3: Timetable change requests (teacher → admin approval)
Teacher proposes a new/changed period; admin approves/rejects.

**New table**: `timetable_change_requests`
- id, schoolId, teacherId, assignmentId, weekday, startTime, endTime, room
- kind: NEW_PERIOD | UPDATE_PERIOD | DELETE_PERIOD
- status: PENDING | APPROVED | REJECTED
- adminNote, createdAt, reviewedAt, reviewedBy

**Endpoints**:
- `POST   /api/v1/teacher/timetable-requests` — teacher submits
- `GET    /api/v1/teacher/timetable-requests` — teacher views own
- `GET    /api/v1/school/timetable-requests` — admin lists pending
- `POST   /api/v1/school/timetable-requests/{id}/approve` — admin approves → creates/updates period
- `POST   /api/v1/school/timetable-requests/{id}/reject` — admin rejects

### Layer 4: Notifications
On approval/rejection, notify the requesting teacher.
On period creation/update, notify affected teachers and parents of the class.

Uses existing `Notify.toUsers()` + `NotifyRecipients.parentsOfClass()`.

## Build Order
1. Backend: Admin period CRUD
2. Backend: Period exceptions (admin + teacher)
3. Backend: Change requests + approval workflow
4. Backend: Notifications
5. Shared: DTOs + API + Repository + ViewModel
6. UI: Timetable tab CRUD, exception UI, approval screen
7. Build + test + commit
