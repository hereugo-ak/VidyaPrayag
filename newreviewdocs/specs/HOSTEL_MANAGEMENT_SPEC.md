# Hostel Management — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-28
> **Prerequisites:** None
> **Template:** `_SPEC_TEMPLATE.md` v1 (25 mandatory + 6 optional sections)

---

## 1. Feature Overview

Hostel/boarding management: room allocation, student hostel assignments, attendance, visitor logging, and hostel fee integration.

### Goals

- Admin manages hostel buildings, rooms, and bed capacity
- Assign students to rooms with check-in/check-out dates
- Daily hostel attendance (present/absent/leave)
- Visitor log with approval
- Hostel fee auto-created in `FeeRecordsTable`

### Non-goals

- [ ] Hostel mess/canteen management (future enhancement)
- [ ] Hostel inventory (beds, furniture) — rooms have capacity only
- [ ] Hostel staff payroll (handled by `PAYROLL_MANAGEMENT_SPEC.md`)
- [ ] Parent hostel portal (parents view via existing parent app)
- [ ] Multi-hostel chain management (single school scope)

### Dependencies

- `FeeRecordsTable` — existing fee records for hostel fee auto-creation
- `StudentsTable` — student records for assignment
- `AppUsersTable` — user accounts for warden and visitor approval
- `NotificationService` — existing notification infrastructure
- `SchoolsTable` — school records for multi-tenant isolation

### Related Modules

- `server/.../feature/hostel/` — new hostel module
- `shared/.../feature/hostel/` — shared DTOs and API
- `composeApp/.../ui/v2/screens/admin/` — admin hostel management UI

---

## 2. Current System Assessment

### Existing Code

- `feature_audit.csv` Gap #8: Hostel missing (0%)
- No hostel tables in `Tables.kt`
- No hostel-related routes in `Application.kt`

### Existing Database

- `FeeRecordsTable` — existing fee records (target for hostel fee auto-creation)
- `StudentsTable` — student records for hostel assignment
- `AppUsersTable` — user accounts (warden, staff, parents)
- `SchoolsTable` — school records
- `NotificationPreferencesTable` — notification preferences

### Existing APIs

- Fee management CRUD
- Student management CRUD
- Notification delivery via `NotificationService`
- Auth: OTP login, password login

### Existing UI

- School portal with admin tabs (`SchoolPortalV2.kt`)
- Web admin dashboard (`website/src/app/admin/`)

### Existing Services

- `NotificationService` — multi-channel notifications
- `FeeService` — fee record creation and management

### Existing Documentation

- `feature_audit.csv` — gap analysis showing hostel at 0%

### Technical Debt

| # | Gap | Details |
|---|---|---|
| TD-1 | No hostel tables | No hostel tables in `Tables.kt` |
| TD-2 | No hostel service | No hostel management service |
| TD-3 | No hostel UI | No hostel management screens |

### Gaps

| # | Gap | Impact | Severity |
|---|---|---|---|
| G1 | No hostel management | Can't manage boarding students | **High** |
| G2 | No room allocation | Can't track room assignments | **High** |
| G3 | No hostel attendance | Can't track boarding student attendance | **High** |
| G4 | No visitor logging | Can't track hostel visitors | **Medium** |
| G5 | No hostel fee integration | Hostel fees not auto-created | **Medium** |

---

## 3. Functional Requirements

### FR-001
| Field | Value |
|---|---|
| **Title** | Manage Hostel Buildings, Floors, Rooms, Beds |
| **Description** | Admin manages hostel buildings, floors, rooms, beds. Buildings have warden assignment. Rooms have capacity and occupied count. |
| **Priority** | Critical |
| **User Roles** | School Admin |
| **Acceptance notes** | CRUD for `hostel_buildings` and `hostel_rooms`. Building has warden_id, warden_name, capacity. Room has room_number, floor, capacity, occupied count. |

### FR-002
| Field | Value |
|---|---|
| **Title** | Assign Students to Beds |
| **Description** | Assign students to rooms with check-in/check-out dates. Track active assignments. Update room occupied count. |
| **Priority** | Critical |
| **User Roles** | School Admin |
| **Acceptance notes** | `hostel_assignments` table with student_id, room_id, check_in_date, check_out_date, is_active. Room `occupied` count auto-updated on assignment/check-out. |

### FR-003
| Field | Value |
|---|---|
| **Title** | Daily Hostel Attendance |
| **Description** | Daily hostel attendance (present/absent/leave). Marked by warden or admin. One record per student per date. |
| **Priority** | High |
| **User Roles** | School Admin, Warden |
| **Acceptance notes** | `hostel_attendance` table with student_id, date, status, marked_by. `UNIQUE(school_id, student_id, date)` prevents duplicates. Bulk marking endpoint. |

### FR-004
| Field | Value |
|---|---|
| **Title** | Visitor Log with Approval |
| **Description** | Visitor log with approval workflow. Visitors request entry, warden/admin approves, visitor checks in and out. |
| **Priority** | High |
| **User Roles** | School Admin, Warden |
| **Acceptance notes** | `hostel_visitors` table with visitor_name, visitor_phone, relation, purpose, check_in_time, check_out_time, approved_by, status. Status: pending → approved → checked_in → checked_out. |

### FR-005
| Field | Value |
|---|---|
| **Title** | Hostel Fee Auto-Created |
| **Description** | Hostel fee auto-created in `FeeRecordsTable` when student is assigned to hostel. Fee amount configurable per building or room. |
| **Priority** | Medium |
| **User Roles** | System |
| **Acceptance notes** | On assignment creation, auto-create fee record in `FeeRecordsTable` with hostel fee amount. Integration with existing `FeeService`. |

### FR-006
| Field | Value |
|---|---|
| **Title** | Hostel Warden Dashboard |
| **Description** | Hostel warden dashboard with occupancy stats, attendance summary, pending visitor approvals. |
| **Priority** | Medium |
| **User Roles** | Warden |
| **Acceptance notes** | Dashboard shows building occupancy, today's attendance, pending visitor requests. Warden can mark attendance and approve visitors. |

---

## 4. User Stories

### School Admin
- [ ] Create and manage hostel buildings with warden assignment
- [ ] Create and manage rooms with capacity
- [ ] Assign students to rooms with check-in dates
- [ ] Check out students from rooms
- [ ] View hostel occupancy dashboard
- [ ] View and mark daily hostel attendance
- [ ] Approve/reject visitor requests
- [ ] View visitor log
- [ ] Configure hostel fee amounts

### Warden
- [ ] View assigned building occupancy
- [ ] Mark daily hostel attendance for assigned building
- [ ] Approve/reject visitor requests for assigned building
- [ ] Check in/check out visitors
- [ ] View student hostel assignments

### Parent
- [ ] View child's hostel assignment (room, building, check-in date)
- [ ] View child's hostel attendance
- [ ] Request visitor entry for child

### System
- [ ] Auto-create hostel fee on student assignment
- [ ] Auto-update room occupied count on assignment/check-out
- [ ] Send notification on visitor approval/rejection

---

## 5. Business Rules

### BR-001
**Rule:** Room capacity cannot be exceeded.
**Enforcement:** `hostel_rooms.occupied` checked before assignment. If `occupied >= capacity`, return 400 "Room is at full capacity."

### BR-002
**Rule:** One active assignment per student at a time.
**Enforcement:** Check `hostel_assignments WHERE student_id = ? AND is_active = true` before creating new assignment. Return 409 if exists.

### BR-003
**Rule:** One attendance record per student per date.
**Enforcement:** `UNIQUE(school_id, student_id, date)` constraint on `hostel_attendance` table.

### BR-004
**Rule:** Visitor must be approved before check-in.
**Enforcement:** Visitor status must be `approved` before `check_in_time` can be set. Status flow: pending → approved → checked_in → checked_out.

### BR-005
**Rule:** Only school admin and assigned warden can manage hostel.
**Enforcement:** `requireSchoolAdmin()` for admin endpoints. Warden endpoints check `hostel_buildings.warden_id == current_user_id`.

### BR-006
**Rule:** Hostel fee auto-created on assignment.
**Enforcement:** `HostelService.assignStudent()` calls `FeeService.createFeeRecord()` after successful assignment.

### BR-007
**Rule:** Room and building names are unique per building/school.
**Enforcement:** `UNIQUE(building_id, room_number)` on `hostel_rooms`. Building name unique per school (enforced in service).

---

## 6. Database Design

### 6.1 Entity Relationship Summary

Five new tables: `hostel_buildings` (buildings with warden), `hostel_rooms` (rooms with capacity), `hostel_assignments` (student-room assignments), `hostel_attendance` (daily attendance), `hostel_visitors` (visitor log with approval).

### 6.2 New Tables

#### `hostel_buildings` table

```sql
CREATE TABLE hostel_buildings (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL REFERENCES schools(id),
    name            TEXT NOT NULL,
    warden_id       UUID,
    warden_name     TEXT,
    capacity        INTEGER NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
```

#### `hostel_rooms` table

```sql
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
```

#### `hostel_assignments` table

```sql
CREATE TABLE hostel_assignments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL REFERENCES schools(id),
    student_id      UUID NOT NULL,
    room_id         UUID NOT NULL REFERENCES hostel_rooms(id),
    check_in_date   DATE NOT NULL,
    check_out_date  DATE,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_hostel_assignments_school ON hostel_assignments(school_id, is_active);
CREATE INDEX idx_hostel_assignments_room ON hostel_assignments(room_id, is_active);
```

#### `hostel_attendance` table

```sql
CREATE TABLE hostel_attendance (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL REFERENCES schools(id),
    student_id      UUID NOT NULL,
    date            DATE NOT NULL,
    status          VARCHAR(16) NOT NULL,          -- present | absent | leave
    marked_by       UUID,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(school_id, student_id, date)
);
CREATE INDEX idx_hostel_attendance_school_date ON hostel_attendance(school_id, date);
```

#### `hostel_visitors` table

```sql
CREATE TABLE hostel_visitors (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL REFERENCES schools(id),
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
CREATE INDEX idx_hostel_visitors_school ON hostel_visitors(school_id, status);
CREATE INDEX idx_hostel_visitors_student ON hostel_visitors(student_id);
```

### 6.3 Modified Tables

None. All new tables. Hostel fee auto-creation inserts into existing `FeeRecordsTable`.

### 6.4 Indexes

- `idx_hostel_assignments_school` — active assignments by school
- `idx_hostel_assignments_room` — active assignments by room (for occupancy)
- `idx_hostel_attendance_school_date` — attendance by school + date
- `idx_hostel_visitors_school` — visitors by school + status
- `idx_hostel_visitors_student` — visitors by student

### 6.5 Constraints

- `hostel_buildings.school_id` — NOT NULL, FK to schools
- `hostel_buildings.name` — NOT NULL, TEXT
- `hostel_rooms.building_id` — NOT NULL, FK to hostel_buildings (CASCADE)
- `hostel_rooms.capacity` — NOT NULL, positive integer
- `hostel_rooms.occupied` — NOT NULL, >= 0, <= capacity
- `UNIQUE(building_id, room_number)` — unique room numbers per building
- `hostel_assignments.school_id` — NOT NULL, FK to schools
- `hostel_assignments.student_id` — NOT NULL
- `hostel_assignments.room_id` — NOT NULL, FK to hostel_rooms
- `hostel_attendance.status` — NOT NULL, one of present/absent/leave
- `UNIQUE(school_id, student_id, date)` — one attendance per student per date
- `hostel_visitors.status` — NOT NULL, one of pending/approved/rejected/checked_in/checked_out

### 6.6 Foreign Keys

- `hostel_buildings.school_id` → `schools.id`
- `hostel_rooms.building_id` → `hostel_buildings.id` (CASCADE)
- `hostel_assignments.school_id` → `schools.id`
- `hostel_assignments.room_id` → `hostel_rooms.id`
- `hostel_attendance.school_id` → `schools.id`
- `hostel_visitors.school_id` → `schools.id`

### 6.7 Soft Delete Strategy

- `hostel_buildings.is_active` — soft delete (deactivate building)
- `hostel_rooms.is_active` — soft delete (deactivate room)
- `hostel_assignments.is_active` — soft delete (set false on check-out)
- `hostel_attendance` — not deleted (historical record)
- `hostel_visitors` — not deleted (audit trail)

### 6.8 Audit Fields

- `created_at` — all 5 tables
- `hostel_attendance.marked_by` — who marked attendance
- `hostel_visitors.approved_by` — who approved visitor

### 6.9 Migration Notes

Migration: `docs/db/migration_047_hostel.sql`
- Creates 5 new tables with FK constraints and indexes
- No data backfill (new feature)
- No modifications to existing tables

### 6.10 Exposed Mappings

Add 5 new table objects in `server/.../db/Tables.kt`:

- `HostelBuildingsTable`
- `HostelRoomsTable`
- `HostelAssignmentsTable`
- `HostelAttendanceTable`
- `HostelVisitorsTable`

Register in `DatabaseFactory.kt` `allTables` array. Order matters (FK dependencies):
1. `HostelBuildingsTable`
2. `HostelRoomsTable` (FK to buildings)
3. `HostelAssignmentsTable` (FK to rooms)
4. `HostelAttendanceTable`
5. `HostelVisitorsTable`

### 6.11 Seed Data

N/A — hostel data created by admin.

---

## 7. State Machines

### Visitor Approval State Machine

```
PENDING ──admin/warden_approves──> APPROVED ──visitor_checks_in──> CHECKED_IN ──visitor_checks_out──> CHECKED_OUT
PENDING ──admin/warden_rejects──> REJECTED
APPROVED ──admin/warden_rejects──> REJECTED (can reject before check-in)
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `pending` | Admin/warden approves | `approved` | Visitor can now check in |
| `pending` | Admin/warden rejects | `rejected` | Visitor cannot check in |
| `approved` | Visitor checks in | `checked_in` | `check_in_time` set to now() |
| `approved` | Admin/warden rejects | `rejected` | Can reject before check-in |
| `checked_in` | Visitor checks out | `checked_out` | `check_out_time` set to now() |

### Student Assignment State Machine

```
NOT_ASSIGNED ──admin_assigns──> ASSIGNED ──admin_checks_out──> CHECKED_OUT
ASSIGNED ──student_leaves──> CHECKED_OUT (with check_out_date)
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `not_assigned` | Admin assigns to room | `assigned` | Room must have available capacity |
| `assigned` | Admin checks out | `checked_out` | `check_out_date` set, `is_active = false`, room occupied decremented |

---

## 8. Backend Architecture

### 8.1 Component Overview

`HostelService` handles building/room CRUD, student assignments, attendance, visitor management, and fee integration. `HostelRouting` exposes admin and warden endpoints.

### 8.2 Design Principles

1. **Hierarchical structure** — buildings → rooms → assignments
2. **Capacity enforcement** — room occupied count always validated
3. **Fee integration** — auto-create fee on assignment via existing `FeeService`
4. **Warden scoping** — warden can only manage assigned building
5. **Multi-tenant isolation** — all queries filtered by `school_id`

### 8.3 Core Types

```kotlin
class HostelService(private val feeService: FeeService) {
    // Buildings
    suspend fun listBuildings(schoolId: UUID): List<HostelBuildingDto>
    suspend fun createBuilding(schoolId: UUID, dto: CreateBuildingDto): HostelBuildingDto
    suspend fun updateBuilding(schoolId: UUID, buildingId: UUID, dto: UpdateBuildingDto): HostelBuildingDto?
    suspend fun deleteBuilding(schoolId: UUID, buildingId: UUID): Boolean

    // Rooms
    suspend fun listRooms(buildingId: UUID): List<HostelRoomDto>
    suspend fun createRoom(buildingId: UUID, dto: CreateRoomDto): HostelRoomDto
    suspend fun updateRoom(buildingId: UUID, roomId: UUID, dto: UpdateRoomDto): HostelRoomDto?
    suspend fun deleteRoom(buildingId: UUID, roomId: UUID): Boolean

    // Assignments
    suspend fun assignStudent(schoolId: UUID, studentId: UUID, roomId: UUID, checkInDate: LocalDate): HostelAssignmentDto
    suspend fun checkOutStudent(schoolId: UUID, assignmentId: UUID, checkOutDate: LocalDate): HostelAssignmentDto?
    suspend fun listAssignments(schoolId: UUID, activeOnly: Boolean): List<HostelAssignmentDto>
    suspend fun getStudentAssignment(schoolId: UUID, studentId: UUID): HostelAssignmentDto?

    // Attendance
    suspend fun markAttendance(schoolId: UUID, date: LocalDate, entries: List<AttendanceEntryDto>, markedBy: UUID): Int
    suspend fun getAttendance(schoolId: UUID, date: LocalDate): List<HostelAttendanceDto>
    suspend fun getAttendanceForStudent(schoolId: UUID, studentId: UUID, fromDate: LocalDate, toDate: LocalDate): List<HostelAttendanceDto>

    // Visitors
    suspend fun requestVisitor(schoolId: UUID, dto: CreateVisitorDto): HostelVisitorDto
    suspend fun approveVisitor(schoolId: UUID, visitorId: UUID, approvedBy: UUID): HostelVisitorDto?
    suspend fun rejectVisitor(schoolId: UUID, visitorId: UUID, approvedBy: UUID): HostelVisitorDto?
    suspend fun checkInVisitor(schoolId: UUID, visitorId: UUID): HostelVisitorDto?
    suspend fun checkOutVisitor(schoolId: UUID, visitorId: UUID): HostelVisitorDto?
    suspend fun listVisitors(schoolId: UUID, status: String?): List<HostelVisitorDto>

    // Dashboard
    suspend fun getWardenDashboard(wardenId: UUID): WardenDashboardDto
    suspend fun getOccupancyStats(schoolId: UUID): OccupancyStatsDto
}
```

### 8.4 Repositories

- `HostelBuildingRepository` — building CRUD
- `HostelRoomRepository` — room CRUD with capacity management
- `HostelAssignmentRepository` — assignment CRUD with occupancy tracking
- `HostelAttendanceRepository` — attendance CRUD
- `HostelVisitorRepository` — visitor CRUD with status management

### 8.5 Mappers

- `HostelMapper` — maps DB rows to DTOs

### 8.6 Permission Checks

- Admin endpoints: `requireSchoolContext()` + `requireSchoolAdmin()` for building/room management
- Warden endpoints: `requireAuth()` + verify `hostel_buildings.warden_id == current_user_id`
- Attendance: admin or assigned warden
- Visitor approval: admin or assigned warden

### 8.7 Background Jobs

None. All operations are on-demand.

### 8.8 Domain Events

- `StudentAssigned` — emitted on assignment creation; triggers fee creation
- `StudentCheckedOut` — emitted on check-out; room occupied decremented
- `AttendanceMarked` — emitted on attendance marking
- `VisitorApproved` — emitted on visitor approval; notification to parent
- `VisitorRejected` — emitted on visitor rejection; notification to parent
- `VisitorCheckedIn` — emitted on visitor check-in
- `VisitorCheckedOut` — emitted on visitor check-out

### 8.9 Caching

- Building list: cached per school, invalidated on building change
- Room list: cached per building, invalidated on room change
- Occupancy stats: cached for 5 minutes
- No cache for attendance or visitors (real-time)

### 8.10 Transactions

- Assignment: insert assignment + update room occupied count + create fee record in transaction
- Check-out: update assignment + update room occupied count in transaction
- Attendance: bulk insert/update in transaction

### 8.11 Rate Limiting

- Standard API rate limiting for all endpoints
- No special rate limiting needed

### 8.12 Configuration

- `HOSTEL_FEE_DEFAULT_AMOUNT` — default hostel fee amount (overridable per building)
- `HOSTEL_FEE_FREQUENCY` — default `monthly` (monthly fee creation)
- `HOSTEL_VISITOR_AUTO_APPROVE` — default `false` (visitors require approval)
- `HOSTEL_ATTENDANCE_REMINDER_ENABLED` — default `true` (reminder to warden if attendance not marked by 10 AM)

---

## 9. API Contracts

### 9.1 Admin Endpoints

All require `requireSchoolContext()`. Writes require `requireSchoolAdmin()`.

```
# Buildings
GET    /api/v1/school/hostel/buildings
POST   /api/v1/school/hostel/buildings
PATCH  /api/v1/school/hostel/buildings/{id}
DELETE /api/v1/school/hostel/buildings/{id}

# Rooms
GET    /api/v1/school/hostel/buildings/{buildingId}/rooms
POST   /api/v1/school/hostel/buildings/{buildingId}/rooms
PATCH  /api/v1/school/hostel/rooms/{id}
DELETE /api/v1/school/hostel/rooms/{id}

# Assignments
GET    /api/v1/school/hostel/assignments?active_only=true
POST   /api/v1/school/hostel/assign  { student_id, room_id, check_in_date }
POST   /api/v1/school/hostel/assignments/{id}/check-out  { check_out_date }

# Attendance
GET    /api/v1/school/hostel/attendance?date={date}
POST   /api/v1/school/hostel/attendance  { date, entries: [{student_id, status}] }

# Visitors
GET    /api/v1/school/hostel/visitors?status={status}
POST   /api/v1/school/hostel/visitors
POST   /api/v1/school/hostel/visitors/{id}/approve
POST   /api/v1/school/hostel/visitors/{id}/reject
POST   /api/v1/school/hostel/visitors/{id}/check-in
POST   /api/v1/school/hostel/visitors/{id}/check-out

# Dashboard
GET    /api/v1/school/hostel/dashboard
```

### 9.2 Warden Endpoints

```
GET    /api/v1/warden/hostel/dashboard
GET    /api/v1/warden/hostel/attendance?date={date}
POST   /api/v1/warden/hostel/attendance  { date, entries: [...] }
GET    /api/v1/warden/hostel/visitors?status=pending
POST   /api/v1/warden/hostel/visitors/{id}/approve
POST   /api/v1/warden/hostel/visitors/{id}/reject
POST   /api/v1/warden/hostel/visitors/{id}/check-in
POST   /api/v1/warden/hostel/visitors/{id}/check-out
```

### 9.3 Parent Endpoints

```
GET    /api/v1/parent/hostel/assignment  -- child's hostel assignment
GET    /api/v1/parent/hostel/attendance?from={date}&to={date}
POST   /api/v1/parent/hostel/visitors  -- request visitor
```

### 9.4 DTO Models

All `@Serializable`, wrapped in `ApiResponse<T>` pattern.

```kotlin
@Serializable data class HostelBuildingDto(
    val id: String, val schoolId: String, val name: String,
    val wardenId: String?, val wardenName: String?,
    val capacity: Int, val isActive: Boolean, val createdAt: String
)

@Serializable data class CreateBuildingDto(
    val name: String, val wardenId: String?, val wardenName: String?, val capacity: Int
)

@Serializable data class HostelRoomDto(
    val id: String, val buildingId: String, val roomNumber: String,
    val floor: Int, val capacity: Int, val occupied: Int, val isActive: Boolean
)

@Serializable data class CreateRoomDto(
    val roomNumber: String, val floor: Int, val capacity: Int
)

@Serializable data class HostelAssignmentDto(
    val id: String, val schoolId: String, val studentId: String,
    val roomId: String, val roomNumber: String, val buildingName: String,
    val checkInDate: String, val checkOutDate: String?, val isActive: Boolean
)

@Serializable data class HostelAttendanceDto(
    val id: String, val schoolId: String, val studentId: String,
    val date: String, val status: String, val markedBy: String?, val createdAt: String
)

@Serializable data class AttendanceEntryDto(
    val studentId: String, val status: String  // present | absent | leave
)

@Serializable data class HostelVisitorDto(
    val id: String, val schoolId: String, val studentId: String,
    val visitorName: String, val visitorPhone: String?,
    val relation: String?, val purpose: String?,
    val checkInTime: String?, val checkOutTime: String?,
    val approvedBy: String?, val status: String, val createdAt: String
)

@Serializable data class CreateVisitorDto(
    val studentId: String, val visitorName: String,
    val visitorPhone: String?, val relation: String?, val purpose: String?
)

@Serializable data class WardenDashboardDto(
    val buildingName: String, val totalRooms: Int, val totalCapacity: Int,
    val totalOccupied: Int, val todayAttendance: AttendanceSummaryDto,
    val pendingVisitors: Int
)

@Serializable data class AttendanceSummaryDto(
    val present: Int, val absent: Int, val leave: Int, val total: Int
)

@Serializable data class OccupancyStatsDto(
    val totalBuildings: Int, val totalRooms: Int, val totalCapacity: Int,
    val totalOccupied: Int, val occupancyRate: Double
)
```

---

## 10. Frontend Architecture

### 10.1 Screens

| Screen | Platform | Role | Description |
|---|---|---|---|
| `HostelScreen` | Compose | Admin | Hostel management dashboard with buildings, rooms, assignments |
| `HostelBuildingScreen` | Compose | Admin | Building details with rooms and occupancy |
| `HostelAttendanceScreen` | Compose | Admin/Warden | Daily attendance marking |
| `HostelVisitorScreen` | Compose | Admin/Warden | Visitor log with approval controls |
| `WardenDashboardScreen` | Compose | Warden | Warden dashboard with occupancy, attendance, pending visitors |
| Web admin hostel page | Web | Admin | Hostel management dashboard |

### 10.2 Navigation

- Admin portal → Hostel → `HostelScreen`
- Admin portal → Hostel → {building} → `HostelBuildingScreen`
- Admin portal → Hostel → Attendance → `HostelAttendanceScreen`
- Admin portal → Hostel → Visitors → `HostelVisitorScreen`
- Warden portal → Dashboard → `WardenDashboardScreen`

### 10.3 UX Flows

#### Admin: Create Building and Assign Students

1. Admin opens Hostel → clicks "New Building"
2. Enters building name, warden, capacity
3. Creates rooms with room numbers, floors, capacities
4. Selects students to assign to rooms
5. Sets check-in dates
6. System auto-creates hostel fees

#### Warden: Mark Attendance and Approve Visitors

1. Warden opens dashboard → sees today's pending tasks
2. Marks attendance for all assigned students (present/absent/leave)
3. Reviews pending visitor requests → approves or rejects
4. Checks in approved visitors → checks out when leaving

### 10.4 State Management

```kotlin
data class HostelState(
    val buildings: List<HostelBuildingDto>,
    val currentBuilding: HostelBuildingDto?,
    val rooms: List<HostelRoomDto>,
    val assignments: List<HostelAssignmentDto>,
    val attendance: List<HostelAttendanceDto>,
    val visitors: List<HostelVisitorDto>,
    val dashboard: WardenDashboardDto?,
    val isLoading: Boolean,
    val error: String?,
)
```

### 10.5 Offline Support

- Building and room data cached locally
- Attendance can be marked offline, synced when online

### 10.6 Loading States

- Loading buildings: "Loading hostel buildings..."
- Marking attendance: "Saving attendance..."
- Approving visitor: "Approving visitor..."

### 10.7 Error Handling (UI)

- Room full: "Room is at full capacity. Select another room."
- Student already assigned: "Student is already assigned to a room."
- Visitor not found: "Visitor request not found."
- Warden not assigned: "You are not assigned as warden for this building."

### 10.8 Component Integration Guidelines

| Rule | Description |
|---|---|
| **R1** | Building list with warden name and occupancy stats |
| **R2** | Room grid showing capacity vs occupied |
| **R3** | Student assignment form with room selector |
| **R4** | Bulk attendance marking with present/absent/leave buttons |
| **R5** | Visitor list with status badges and action buttons |
| **R6** | Warden dashboard with occupancy, attendance, pending visitors |
| **R7** | Check-in/check-out date pickers |
| **R8** | Occupancy progress bars per room and building |

---

## 11. Shared Module Changes (KMP)

### 11.1 DTOs

All DTOs defined in section 9.4, placed in `shared/.../feature/hostel/data/remote/`.

### 11.2 Domain Models

```kotlin
data class HostelBuilding(
    val id: UUID, val schoolId: UUID, val name: String,
    val wardenId: UUID?, val wardenName: String?,
    val capacity: Int, val isActive: Boolean,
)

data class HostelRoom(
    val id: UUID, val buildingId: UUID, val roomNumber: String,
    val floor: Int, val capacity: Int, val occupied: Int, val isActive: Boolean,
)

data class HostelAssignment(
    val id: UUID, val schoolId: UUID, val studentId: UUID,
    val roomId: UUID, val checkInDate: LocalDate,
    val checkOutDate: LocalDate?, val isActive: Boolean,
)

data class HostelAttendance(
    val id: UUID, val schoolId: UUID, val studentId: UUID,
    val date: LocalDate, val status: AttendanceStatus, val markedBy: UUID?,
)

enum class AttendanceStatus { PRESENT, ABSENT, LEAVE }

data class HostelVisitor(
    val id: UUID, val schoolId: UUID, val studentId: UUID,
    val visitorName: String, val visitorPhone: String?,
    val relation: String?, val purpose: String?,
    val checkInTime: Instant?, val checkOutTime: Instant?,
    val approvedBy: UUID?, val status: VisitorStatus,
)

enum class VisitorStatus { PENDING, APPROVED, REJECTED, CHECKED_IN, CHECKED_OUT }
```

### 11.3 Repository Interfaces

```kotlin
interface HostelRepository {
    suspend fun listBuildings(): NetworkResult<List<HostelBuildingDto>>
    suspend fun createBuilding(dto: CreateBuildingDto): NetworkResult<HostelBuildingDto>
    suspend fun listRooms(buildingId: String): NetworkResult<List<HostelRoomDto>>
    suspend fun createRoom(buildingId: String, dto: CreateRoomDto): NetworkResult<HostelRoomDto>
    suspend fun assignStudent(dto: AssignDto): NetworkResult<HostelAssignmentDto>
    suspend fun checkOutStudent(assignmentId: String, checkOutDate: String): NetworkResult<HostelAssignmentDto>
    suspend fun listAssignments(activeOnly: Boolean): NetworkResult<List<HostelAssignmentDto>>
    suspend fun markAttendance(dto: MarkAttendanceDto): NetworkResult<Int>
    suspend fun getAttendance(date: String): NetworkResult<List<HostelAttendanceDto>>
    suspend fun listVisitors(status: String?): NetworkResult<List<HostelVisitorDto>>
    suspend fun approveVisitor(visitorId: String): NetworkResult<HostelVisitorDto>
    suspend fun rejectVisitor(visitorId: String): NetworkResult<HostelVisitorDto>
    suspend fun checkInVisitor(visitorId: String): NetworkResult<HostelVisitorDto>
    suspend fun checkOutVisitor(visitorId: String): NetworkResult<HostelVisitorDto>
    suspend fun getDashboard(): NetworkResult<WardenDashboardDto>
}
```

### 11.4 UseCases

- `ListBuildingsUseCase`, `CreateBuildingUseCase`, `ListRoomsUseCase`, `CreateRoomUseCase`
- `AssignStudentUseCase`, `CheckOutStudentUseCase`, `ListAssignmentsUseCase`
- `MarkAttendanceUseCase`, `GetAttendanceUseCase`
- `ListVisitorsUseCase`, `ApproveVisitorUseCase`, `RejectVisitorUseCase`
- `CheckInVisitorUseCase`, `CheckOutVisitorUseCase`
- `GetDashboardUseCase`

### 11.5 Validation

- Building name: not empty, max 100 characters
- Room number: not empty, max 16 characters
- Room capacity: positive integer, max 20
- Check-in date: not in the past (for new assignments)
- Check-out date: must be after check-in date
- Visitor name: not empty, max 100 characters
- Visitor phone: valid phone format (if provided)

### 11.6 Serialization

Standard Kotlinx serialization.

### 11.7 Network APIs

Ktor `@Resource` route definitions:
- `SchoolHostelApi` — admin endpoints
- `WardenHostelApi` — warden endpoints
- `ParentHostelApi` — parent endpoints

### 11.8 Database Models (Local Cache)

- Building and room data cached locally
- Attendance can be cached for offline marking

---

## 12. Permissions Matrix

| Action | Super Admin | School Admin | Warden | Teacher | Parent |
|---|---|---|---|---|---|
| Manage buildings | ✅ | ✅ | ❌ | ❌ | ❌ |
| Manage rooms | ✅ | ✅ | ❌ | ❌ | ❌ |
| Assign/check-out students | ✅ | ✅ | ❌ | ❌ | ❌ |
| Mark attendance | ✅ | ✅ | ✅ (own building) | ❌ | ❌ |
| View attendance | ✅ | ✅ | ✅ (own building) | ❌ | ✅ (own child) |
| Approve/reject visitors | ✅ | ✅ | ✅ (own building) | ❌ | ❌ |
| Check in/out visitors | ✅ | ✅ | ✅ (own building) | ❌ | ❌ |
| Request visitor | N/A | N/A | N/A | N/A | ✅ |
| View hostel dashboard | ✅ | ✅ | ✅ (own building) | ❌ | ❌ |
| View child assignment | N/A | N/A | N/A | N/A | ✅ |

---

## 13. Notifications

### Hostel-Specific Notification Triggers

| Type | Trigger | Recipient | Channel | Message |
|---|---|---|---|---|
| Student Assigned | Admin assigns student to hostel | Parent | Push + WhatsApp | "{student_name} has been assigned to {building_name}, Room {room_number}." |
| Student Checked Out | Admin checks out student | Parent | Push | "{student_name} has been checked out from hostel." |
| Visitor Approved | Warden/admin approves visitor | Parent | Push | "Visitor {visitor_name} approved for {student_name}." |
| Visitor Rejected | Warden/admin rejects visitor | Parent | Push | "Visitor {visitor_name} request rejected." |
| Visitor Checked In | Visitor checks in | Parent | Push | "Visitor {visitor_name} has checked in to meet {student_name}." |
| Visitor Checked Out | Visitor checks out | Parent | Push | "Visitor {visitor_name} has checked out." |
| Attendance Reminder | 10 AM and attendance not marked | Warden | Push | "Hostel attendance not marked for today. Please mark now." |
| Absent Notification | Student marked absent | Parent | Push + WhatsApp | "{student_name} was marked absent in hostel today." |

### Notification System Integration

- Reuse `NotificationService` for push + WhatsApp notifications
- Reuse `NotifyRecipients.kt` for parent audience resolution (by student)
- New WhatsApp templates: `hostel_assignment`, `visitor_approved`, `visitor_rejected`, `hostel_absent`

---

## 14. Background Jobs

### Attendance Reminder Job

| Field | Value |
|---|---|
| **Name** | `HostelAttendanceReminderJob` |
| **Trigger** | Daily |
| **Frequency** | Daily at 10 AM IST |
| **Description** | Checks if hostel attendance has been marked for today. If not, sends reminder to warden. |
| **Timeout** | 30 seconds |
| **Retry** | None |
| **On failure** | Logged; retried next day |

### Hostel Fee Creation (Event-Driven)

| Field | Value |
|---|---|
| **Name** | `HostelFeeCreation` |
| **Trigger** | Student assigned to hostel |
| **Frequency** | On-demand (event-driven) |
| **Description** | Auto-creates hostel fee record in `FeeRecordsTable` when student is assigned |
| **Timeout** | 10 seconds |
| **Retry** | 3 attempts with 5-second intervals |
| **On failure** | Logged; admin notified. Assignment remains but fee not created. |

---

## 15. Integrations

### Internal Integrations

| System | Integration Point | Direction | Protocol | Error Handling |
|---|---|---|---|---|
| `FeeRecordsTable` | Hostel fee auto-creation on assignment | Write | Direct DB (via `FeeService`) | Logged; assignment remains, fee retried |
| `FeeService` | Fee record creation | Outbound | Direct call | Retry 3x; log on failure |
| `StudentsTable` | Student validation for assignment | Read | Direct DB | Return error if student not found |
| `AppUsersTable` | Warden validation | Read | Direct DB | Return error if warden not found |
| `NotificationService` | Assignment, visitor, attendance notifications | Outbound | Direct call | Logged; non-blocking |
| `NotifyRecipients.kt` | Parent audience resolution | Read | Direct code | Fallback: skip if no recipients |
| `WhatsappLogsTable` | WhatsApp message logging | Write | Direct DB | Logged |

### External Integrations

| System | Purpose | Direction | Protocol | Authentication | Error Handling |
|---|---|---|---|---|---|
| Meta WhatsApp Business API | Hostel notifications (assignment, absent, visitor) | Outbound | HTTP API | Bearer token | Retry 3x; log to `WhatsappLogsTable` |

### Integration Patterns

- **Fee integration:** On `StudentAssigned` event, call `FeeService.createFeeRecord()` with hostel fee amount. Transactional with assignment creation.
- **Notifications:** Reuse existing `NotificationService` + `NotifyRecipients.kt`. Parent resolved via student relationship.
- **WhatsApp:** Reuse existing WhatsApp infrastructure. New templates: `hostel_assignment`, `visitor_approved`, `visitor_rejected`, `hostel_absent`.

---

## 16. Security

### Authentication

- Admin endpoints: standard JWT auth via `requireSchoolContext()` + `requireSchoolAdmin()`
- Warden endpoints: standard JWT auth via `requireAuth()` + warden assignment verification
- Parent endpoints: standard JWT auth via `requireAuth()`

### Authorization

- Only school admin can manage buildings, rooms, and assignments
- Warden can mark attendance and manage visitors only for assigned building
- Parents can view child's assignment and attendance, request visitors
- Server validates warden assignment on all warden endpoints

### Data Protection

- Hostel data is school-scoped — no cross-school access
- Student assignments contain student_id — standard PII protection
- Visitor data contains visitor name + phone — PII, school-scoped
- Attendance data contains student_id + status — standard PII

### Input Validation

- Building name: not empty, max 100 characters
- Room number: not empty, max 16 characters
- Room capacity: positive integer, max 20
- Floor: positive integer, max 20
- Check-in date: valid date, not in past for new assignments
- Check-out date: must be after check-in date
- Visitor name: not empty, max 100 characters
- Visitor phone: valid phone format (if provided)
- Attendance status: one of present, absent, leave

### Rate Limiting

- Standard API rate limiting for all endpoints
- No special rate limiting needed

### Audit Logging

- Building creation, update, deletion
- Room creation, update, deletion
- Student assignment, check-out
- Attendance marking (bulk)
- Visitor request, approval, rejection, check-in, check-out
- Fee auto-creation

### PII Handling

- Student IDs linked to `StudentsTable` — standard PII
- Visitor name and phone — PII, school-scoped
- Warden ID linked to `AppUsersTable` — standard PII
- No additional PII beyond standard school management

### Multi-tenant Isolation

- All queries filtered by `school_id`
- `hostel_buildings.school_id` — NOT NULL, FK to schools
- `hostel_assignments.school_id` — NOT NULL, FK to schools
- `hostel_attendance.school_id` — NOT NULL, FK to schools
- `hostel_visitors.school_id` — NOT NULL, FK to schools
- Server validates school context on all admin endpoints
- Warden endpoints resolve school from JWT context

---

## 17. Performance & Scalability

### Expected Scale

- Small school: 1-3 buildings, 10-50 rooms, 50-200 students
- Medium school: 3-10 buildings, 50-200 rooms, 200-1,000 students
- Large school: 10-20 buildings, 200-500 rooms, 1,000-3,000 students

### Query Optimization

- **Building listing:** PK lookup by school_id. Small dataset.
- **Room listing:** FK lookup by building_id. Small dataset.
- **Assignment listing:** `idx_hostel_assignments_school` on `(school_id, is_active)`. Paginated.
- **Attendance by date:** `idx_hostel_attendance_school_date` on `(school_id, date)`. Single query.
- **Visitors by status:** `idx_hostel_visitors_school` on `(school_id, status)`. Paginated.

### Indexing Strategy

- `idx_hostel_assignments_school` — active assignments by school
- `idx_hostel_assignments_room` — active assignments by room (occupancy check)
- `idx_hostel_attendance_school_date` — attendance by school + date
- `idx_hostel_visitors_school` — visitors by school + status
- `idx_hostel_visitors_student` — visitors by student
- `UNIQUE(building_id, room_number)` — unique room numbers
- `UNIQUE(school_id, student_id, date)` — one attendance per student per date

### Caching Strategy

- Building list: cached per school, invalidated on building change
- Room list: cached per building, invalidated on room change
- Occupancy stats: 5-minute TTL cache
- No cache for attendance or visitors (real-time)

### Pagination

- Assignment listing: max 100 per page
- Visitor listing: max 50 per page
- Attendance: no pagination (single date query)

### Connection Pooling

- Uses existing HikariCP connection pool
- No additional pooling needed

### Async Processing

- Notification delivery: async (existing `NotificationService` pattern)
- Fee creation: synchronous (within assignment transaction)
- Attendance marking: synchronous (bulk insert)

### Scalability Concerns

- Bulk attendance for 1,000+ students: single transaction, <2 seconds
- Occupancy count updates: atomic increment/decrement, no race condition risk with proper transaction isolation
- Visitor log growth: historical data, consider archival after 1 year

---

## 18. Edge Cases

| # | Scenario | Expected Behavior |
|---|---|---|
| EC-1 | Admin assigns student to full room | Return 400 "Room is at full capacity." |
| EC-2 | Admin assigns student already assigned | Return 409 "Student is already assigned to a room. Check out first." |
| EC-3 | Admin checks out student not assigned | Return 400 "Student is not currently assigned." |
| EC-4 | Warden marks attendance for non-assigned building | Return 403 "You are not the warden for this building." |
| EC-5 | Attendance already marked for student on date | Upsert: update existing record. `UNIQUE` constraint handles conflict. |
| EC-6 | Visitor check-in without approval | Return 400 "Visitor must be approved before check-in." |
| EC-7 | Visitor check-out without check-in | Return 400 "Visitor has not checked in yet." |
| EC-8 | Admin deletes building with active rooms | CASCADE delete rooms. Active assignments become orphaned — block deletion if active assignments exist. Return 400 "Building has active student assignments. Check out students first." |
| EC-9 | Admin deletes room with active assignment | Return 400 "Room has active assignments. Check out students first." |
| EC-10 | Fee creation fails on assignment | Log error. Assignment remains. Admin notified. Fee can be manually created. |
| EC-11 | Warden not found for building | Building warden_id is null. Warden endpoints return 403. Admin must assign warden. |
| EC-12 | Parent requests visitor for non-hostel student | Return 400 "Student is not assigned to hostel." |
| EC-13 | Check-out date before check-in date | Return 400 "Check-out date must be after check-in date." |
| EC-14 | Two visitors check in simultaneously for same student | Allowed. Both visitors tracked separately. |
| EC-15 | Room capacity set to 0 | Return 400 "Room capacity must be at least 1." |
| EC-16 | Building capacity doesn't match sum of room capacities | Warning logged. Building capacity is independent field (can be total or planned). |
| EC-17 | Student marked 'leave' in attendance | Allowed. Leave is a valid status. Parent notified. |
| EC-18 | Visitor status is 'rejected' but tries to check in | Return 400 "Visitor request was rejected." |

---

## 19. Error Handling

### Error Response Format

Standard `ApiResponse` error format:
```json
{
  "success": false,
  "error": {
    "code": "ROOM_FULL",
    "message": "Room is at full capacity",
    "details": {}
  }
}
```

### Error Codes

| Code | HTTP Status | Description | User Message |
|---|---|---|---|
| `BUILDING_NOT_FOUND` | 404 | Building not found | "Building not found." |
| `ROOM_NOT_FOUND` | 404 | Room not found | "Room not found." |
| `ASSIGNMENT_NOT_FOUND` | 404 | Assignment not found | "Assignment not found." |
| `VISITOR_NOT_FOUND` | 404 | Visitor not found | "Visitor request not found." |
| `ROOM_FULL` | 400 | Room at capacity | "Room is at full capacity." |
| `STUDENT_ALREADY_ASSIGNED` | 409 | Student already in hostel | "Student is already assigned to a room." |
| `BUILDING_HAS_ASSIGNMENTS` | 400 | Building has active assignments | "Building has active student assignments." |
| `ROOM_HAS_ASSIGNMENTS` | 400 | Room has active assignments | "Room has active assignments." |
| `VISITOR_NOT_APPROVED` | 400 | Visitor not approved | "Visitor must be approved before check-in." |
| `VISITOR_NOT_CHECKED_IN` | 400 | Visitor not checked in | "Visitor has not checked in yet." |
| `VISITOR_REJECTED` | 400 | Visitor was rejected | "Visitor request was rejected." |
| `NOT_WARDEN` | 403 | User is not warden | "You are not the warden for this building." |
| `INVALID_CHECKOUT_DATE` | 400 | Check-out before check-in | "Check-out date must be after check-in date." |
| `STUDENT_NOT_IN_HOSTEL` | 400 | Student not in hostel | "Student is not assigned to hostel." |
| `FEE_CREATION_FAILED` | 500 | Hostel fee creation failed | "Hostel fee could not be created. Please create manually." |

### Error Handling Strategy

- **Validation errors:** Return 400 with field-specific message
- **Auth errors:** Return 401/403 with clear message
- **Not found:** Return 404
- **Conflicts:** Return 409
- **Server errors:** Return 500 with generic message; log full error

### Retry Strategy

- Client retries: 3 attempts with exponential backoff for 5xx errors
- No retry for 4xx errors (client errors)
- Fee creation: 3 retries with 5-second intervals
- WhatsApp delivery: 3 retries with 5-second intervals (existing pattern)

### Fallback Behavior

- Fee creation fails: Assignment remains valid; admin can manually create fee
- WhatsApp unavailable: Send push notification only
- Warden unavailable: Admin can perform warden actions (admin has all permissions)

---

## 20. Analytics & Reporting

### Analytics Dashboard Data

| Metric | Source | Derivation |
|---|---|---|
| Total buildings | `hostel_buildings` count by school | Direct count |
| Total rooms | `hostel_rooms` count by building | Direct count |
| Total capacity | Sum of `hostel_rooms.capacity` | Aggregate |
| Total occupied | Sum of `hostel_rooms.occupied` | Aggregate |
| Occupancy rate | `occupied / capacity * 100` | Derived percentage |
| Today's attendance | `hostel_attendance` for today | Group by status, count |
| Pending visitors | `hostel_visitors` where `status='pending'` | Direct count |
| Active assignments | `hostel_assignments` where `is_active=true` | Direct count |

### Export Capabilities

- Assignment list export (CSV) — student, room, building, check-in, check-out
- Attendance report export (CSV) — student, date, status, marked_by
- Visitor log export (CSV) — visitor, student, check-in, check-out, status

### Report Types

| Report | Format | Frequency | Recipient |
|---|---|---|---|
| Occupancy report | CSV | On-demand | School Admin |
| Attendance report | CSV | On-demand | School Admin, Warden |
| Visitor log | CSV | On-demand | School Admin |
| Monthly hostel summary | JSON (API) | On-demand | School Admin |

---

## 21. Testing Strategy

### Unit Tests

- `HostelService` — all methods (buildings, rooms, assignments, attendance, visitors, dashboard)
- Room capacity enforcement
- Occupancy count updates (increment on assign, decrement on check-out)
- Visitor state machine transitions
- Fee auto-creation on assignment
- Warden permission checks

### Integration Tests

- Building → room → assignment → fee creation flow
- Check-out: assignment deactivated, room occupied decremented
- Attendance: bulk mark → verify all records created
- Visitor: request → approve → check-in → check-out flow
- Visitor: request → reject flow
- Multi-tenant: school A hostel data not accessible to school B
- Warden scoping: warden can only access assigned building

### E2E Tests

- Admin creates building → creates rooms → assigns students → fee created → parent notified
- Warden marks attendance → parent of absent student notified
- Parent requests visitor → warden approves → visitor checks in → checks out

### Performance Tests

- Bulk attendance for 500 students: < 2 seconds
- Assignment listing with 1,000 records: < 500ms
- Visitor listing with 500 records: < 300ms

### Test Data

- 3 sample buildings with different wardens
- 20 sample rooms with varying capacities
- 50 sample assignments (active and checked-out)
- 100 sample attendance records
- 20 sample visitors (all statuses)

### Test Environment

- Test database with schema migration applied
- Mock `FeeService` for fee creation tests
- Mock `NotificationService` for notification tests
- Mock WhatsApp API for notification tests
- Test JWT tokens for admin, warden, parent roles

---

## 22. Acceptance Criteria

- [ ] Buildings, rooms, beds managed
- [ ] Students assigned to rooms with check-in/check-out
- [ ] Room capacity enforced (no over-assignment)
- [ ] Room occupied count auto-updated
- [ ] Daily hostel attendance tracked (present/absent/leave)
- [ ] Bulk attendance marking supported
- [ ] Visitor log with approval workflow
- [ ] Visitor state machine (pending → approved → checked_in → checked_out)
- [ ] Hostel fee auto-created on assignment
- [ ] Warden dashboard with occupancy and attendance
- [ ] Warden scoped to assigned building only
- [ ] All admin endpoints enforce `requireSchoolContext()` + `requireSchoolAdmin()`
- [ ] Compose app hostel management UI
- [ ] Parent can view child's hostel assignment and attendance
- [ ] Parent can request visitor entry
- [ ] Notifications sent for assignment, visitor, and absent attendance

---

## 23. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 2 days | DB migration (`047`), 5 Exposed table objects, `DatabaseFactory` registration |
| 2 | 2 days | `HostelService.kt` — buildings, rooms, assignments, attendance, visitors, fee integration |
| 3 | 2 days | `HostelRouting.kt` with all endpoints + DTOs, mount in `Application.kt`, notification integration |
| 4 | 3 days | Client UI: `HostelScreen.kt`, `HostelAttendanceScreen.kt`, `HostelVisitorScreen.kt`, `WardenDashboardScreen.kt`, wire into navigation |
| 5 | 1 day | Tests (server unit + integration, client unit) |

### Pre-Implementation Checklist

- [ ] Verify `FeeRecordsTable` schema for fee creation
- [ ] Verify `FeeService.createFeeRecord()` API signature
- [ ] Verify `StudentsTable` schema for student validation
- [ ] Verify `NotificationService` supports student-based recipient resolution
- [ ] Check if warden role exists in auth system or needs to be added

---

## 24. File-Level Impact Analysis

### Required (6 files)

| # | File | Change Type | Description |
|---|---|---|---|
| 1 | `docs/db/migration_047_hostel.sql` | New | DDL: 5 tables with FK constraints and indexes |
| 2 | `server/.../db/Tables.kt` | Modify | Add 5 table objects (HostelBuildingsTable, HostelRoomsTable, HostelAssignmentsTable, HostelAttendanceTable, HostelVisitorsTable) |
| 3 | `server/.../db/DatabaseFactory.kt` | Modify | Register 5 tables in `allTables` array |
| 4 | `server/.../feature/hostel/HostelService.kt` | New | Core service (buildings, rooms, assignments, attendance, visitors, fee integration) |
| 5 | `server/.../feature/hostel/HostelRouting.kt` | New | API endpoints + DTOs + `hostelRouting()` function |
| 6 | `server/.../Application.kt` | Modify | Import + mount `hostelRouting()` |

### Additional Required (1 file)

| # | File | Change Type | Description |
|---|---|---|---|
| 7 | `shared/.../feature/hostel/` | New | Shared DTOs, domain models, repository, API client |

### Client UI (4 files)

| # | File | Change Type | Description |
|---|---|---|---|
| 8 | `composeApp/.../ui/v2/screens/admin/HostelScreen.kt` | New | Hostel management dashboard |
| 9 | `composeApp/.../ui/v2/screens/admin/HostelAttendanceScreen.kt` | New | Daily attendance marking |
| 10 | `composeApp/.../ui/v2/screens/admin/HostelVisitorScreen.kt` | New | Visitor log with approval |
| 11 | `composeApp/.../ui/v2/screens/warden/WardenDashboardScreen.kt` | New | Warden dashboard |

### Optional (1 file)

| # | File | Change Type | Description |
|---|---|---|---|
| 12 | `website/src/app/admin/hostel/page.tsx` | New | Web admin hostel management page |

---

## 25. Future Enhancements

| # | Enhancement | Priority | Effort | Notes |
|---|---|---|---|---|
| F-1 | Hostel mess/canteen management | Medium | L | Meal planning, mess attendance, menu |
| F-2 | Hostel inventory (beds, furniture) | Low | M | Track room assets and condition |
| F-3 | Hostel leave management | Medium | S | Student leave requests with approval |
| F-4 | Hostel fee waiver/scholarship | Low | S | Integration with scholarship workflow |
| F-5 | Hostel room transfer | Medium | S | Move student between rooms |
| F-6 | Hostel inspection reports | Low | M | Periodic room inspection and maintenance |
| F-7 | Hostel gate pass | Medium | S | Digital gate pass for student outings |
| F-8 | Hostel laundry management | Low | M | Laundry tracking and scheduling |
| F-9 | Hostel medical records | Low | M | Integration with health records |
| F-10 | Hostel analytics dashboard | Low | M | Occupancy trends, attendance patterns |

---

## Appendix A: Sequence Diagrams

### A.1 Admin Assigns Student to Room

```
Admin       HostelService    HostelRoomsTable    HostelAssignmentsTable    FeeService
  │                │                │                    │                    │
  │──assign(studentId,roomId)──→│   │                    │                    │
  │                │──check room capacity──│              │                    │
  │                │←──room (occupied<capacity)──│        │                    │
  │                │──check existing assignment──│────────│                    │
  │                │←──no active assignment──│             │                    │
  │                │──insert assignment──────────────────→│                    │
  │                │──increment room occupied──│          │                    │
  │                │──create fee record──────────────────────────────────────→│
  │                │←──fee created────────────────────────────────────────────│
  │←──AssignmentDto│                │                    │                    │
  │                │                │                    │                    │
```

### A.2 Warden Marks Attendance

```
Warden       HostelService    HostelAttendanceTable    NotificationService
  │                │                    │                      │
  │──markAttendance(date,entries)──→│    │                      │
  │                │──verify warden for building              │
  │                │──bulk upsert attendance──│                │
  │                │←──count inserted/updated──│              │
  │                │  [for each absent student]│              │
  │                │──send absent notification──────────────────→│
  │←──count────────│                    │                      │
  │                │                    │                      │
```

### A.3 Visitor Approval Flow

```
Parent       HostelService    HostelVisitorsTable    Warden       NotificationService
  │                │                │                    │             │
  │──requestVisitor──→│              │                    │             │
  │                │──insert visitor (status=pending)──→│              │
  │←──VisitorDto──│                │                    │             │
  │                │                │                    │             │
  │                │  [warden sees pending request]     │             │
  │                │←──approve(visitorId)────────────────│             │
  │                │──update status=approved──────────→│              │
  │                │──notify parent──────────────────────────────────→│
  │←──notification──│                │                    │             │
  │                │                │                    │             │
  │                │  [visitor arrives]                   │             │
  │                │←──checkIn(visitorId)────────────────│             │
  │                │──update status=checked_in, check_in_time=now()──→│
  │                │──notify parent──────────────────────────────────→│
  │                │                │                    │             │
  │                │  [visitor leaves]                    │             │
  │                │←──checkOut(visitorId)───────────────│             │
  │                │──update status=checked_out, check_out_time=now()─→│
  │                │──notify parent──────────────────────────────────→│
  │                │                │                    │             │
```

### A.4 Admin Checks Out Student

```
Admin       HostelService    HostelAssignmentsTable    HostelRoomsTable
  │                │                    │                      │
  │──checkOut(assignmentId,date)──→│    │                      │
  │                │──update is_active=false, check_out_date──│
  │                │←──updated assignment──│                   │
  │                │──decrement room occupied─────────────────────────→│
  │←──AssignmentDto│                    │                      │
  │                │                    │                      │
```

---

## Appendix B: Domain Model / ER Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                          schools                                       │
│  id (PK)  name  ...                                                   │
└──────────────────────────┬───────────────────────────────────────────┘
                           │
           ┌───────────────┼───────────────┐
           │               │               │
           ▼               ▼               ▼
┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐
│ hostel_buildings  │ │ hostel_attendance │ │  hostel_visitors  │
│ id (PK)           │ │ id (PK)           │ │  id (PK)          │
│ school_id (FK)    │ │ school_id (FK)    │ │  school_id (FK)   │
│ name, warden_id   │ │ student_id        │ │  student_id       │
│ capacity          │ │ date, status      │ │  visitor_name     │
│ is_active         │ │ marked_by         │ │  status, times    │
└────────┬─────────┘ │ UNIQUE(school,     │ └──────────────────┘
         │           │   student, date)   │
         │ FK        └──────────────────┘
         ▼
┌──────────────────┐
│  hostel_rooms     │
│ id (PK)           │
│ building_id (FK)  │
│ room_number       │
│ floor, capacity   │
│ occupied          │
│ UNIQUE(building,  │
│   room_number)    │
└────────┬─────────┘
         │ FK
         ▼
┌──────────────────┐
│hostel_assignments │
│ id (PK)           │
│ school_id (FK)    │
│ student_id        │
│ room_id (FK)      │
│ check_in_date     │
│ check_out_date    │
│ is_active         │
└──────────────────┘
```

---

## Appendix C: Event Flow

### Domain Events

| Event | Emitter | Consumers | Payload | Side Effects |
|---|---|---|---|---|
| `StudentAssigned` | `HostelService.assignStudent()` | FeeService, NotificationService | `assignmentId, schoolId, studentId, roomId, buildingName, roomNumber` | Fee record created; parent notified |
| `StudentCheckedOut` | `HostelService.checkOutStudent()` | NotificationService | `assignmentId, schoolId, studentId, checkOutDate` | Room occupied decremented; parent notified |
| `AttendanceMarked` | `HostelService.markAttendance()` | NotificationService | `schoolId, date, count, absentStudentIds` | Absent students' parents notified |
| `VisitorRequested` | `HostelService.requestVisitor()` | None | `visitorId, schoolId, studentId, visitorName` | None (warden sees pending) |
| `VisitorApproved` | `HostelService.approveVisitor()` | NotificationService | `visitorId, schoolId, studentId, visitorName, approvedBy` | Parent notified |
| `VisitorRejected` | `HostelService.rejectVisitor()` | NotificationService | `visitorId, schoolId, studentId, visitorName, rejectedBy` | Parent notified |
| `VisitorCheckedIn` | `HostelService.checkInVisitor()` | NotificationService | `visitorId, schoolId, studentId, visitorName, checkInTime` | Parent notified |
| `VisitorCheckedOut` | `HostelService.checkOutVisitor()` | NotificationService | `visitorId, schoolId, studentId, visitorName, checkOutTime` | Parent notified |

### Event Delivery Guarantees

- Events are emitted synchronously within the same transaction (for DB operations)
- Notifications are async (fire-and-forget with logging)
- Fee creation is synchronous within assignment transaction
- Failed notifications are logged; not retried

---

## Appendix D: Configuration

### Environment Variables

| Variable | Default | Description |
|---|---|---|
| `HOSTEL_FEE_DEFAULT_AMOUNT` | `5000` | Default hostel fee amount in INR |
| `HOSTEL_FEE_FREQUENCY` | `monthly` | Fee creation frequency (monthly, quarterly, yearly) |
| `HOSTEL_VISITOR_AUTO_APPROVE` | `false` | Auto-approve visitor requests (skip warden approval) |
| `HOSTEL_ATTENDANCE_REMINDER_ENABLED` | `true` | Send attendance reminder to warden at 10 AM |
| `HOSTEL_ATTENDANCE_REMINDER_CRON` | `0 0 10 * * ?` | Cron for attendance reminder (10 AM IST) |
| `HOSTEL_OCCUPANCY_CACHE_TTL` | `300` | Occupancy stats cache TTL in seconds (5 min) |

### Feature Flags

| Flag | Default | Description |
|---|---|---|
| `HOSTEL_ENABLED` | `true` | Enable/disable hostel feature |
| `HOSTEL_FEE_AUTO_CREATE` | `true` | Enable/disable auto fee creation on assignment |
| `HOSTEL_VISITOR_LOGGING` | `true` | Enable/disable visitor logging |
| `HOSTEL_ATTENDANCE_REMINDER` | `true` | Enable/disable attendance reminder |

### School-Level Settings

N/A — hostel configuration is per-building (warden, capacity). No school-level settings beyond standard.

---

## Appendix E: Migration & Rollback

### Migration: `migration_047_hostel.sql`

```sql
-- Migration 047: Hostel Management
-- Creates 5 new tables

BEGIN;

-- 1. hostel_buildings
CREATE TABLE IF NOT EXISTS hostel_buildings (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL REFERENCES schools(id),
    name            TEXT NOT NULL,
    warden_id       UUID,
    warden_name     TEXT,
    capacity        INTEGER NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

-- 2. hostel_rooms
CREATE TABLE IF NOT EXISTS hostel_rooms (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    building_id     UUID NOT NULL REFERENCES hostel_buildings(id) ON DELETE CASCADE,
    room_number     VARCHAR(16) NOT NULL,
    floor           INTEGER NOT NULL DEFAULT 1,
    capacity        INTEGER NOT NULL DEFAULT 2,
    occupied        INTEGER NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    UNIQUE(building_id, room_number)
);

-- 3. hostel_assignments
CREATE TABLE IF NOT EXISTS hostel_assignments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL REFERENCES schools(id),
    student_id      UUID NOT NULL,
    room_id         UUID NOT NULL REFERENCES hostel_rooms(id),
    check_in_date   DATE NOT NULL,
    check_out_date  DATE,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_hostel_assignments_school ON hostel_assignments(school_id, is_active);
CREATE INDEX IF NOT EXISTS idx_hostel_assignments_room ON hostel_assignments(room_id, is_active);

-- 4. hostel_attendance
CREATE TABLE IF NOT EXISTS hostel_attendance (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL REFERENCES schools(id),
    student_id      UUID NOT NULL,
    date            DATE NOT NULL,
    status          VARCHAR(16) NOT NULL,
    marked_by       UUID,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(school_id, student_id, date)
);
CREATE INDEX IF NOT EXISTS idx_hostel_attendance_school_date ON hostel_attendance(school_id, date);

-- 5. hostel_visitors
CREATE TABLE IF NOT EXISTS hostel_visitors (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL REFERENCES schools(id),
    student_id      UUID NOT NULL,
    visitor_name    TEXT NOT NULL,
    visitor_phone   VARCHAR(32),
    relation        TEXT,
    purpose         TEXT,
    check_in_time   TIMESTAMP NOT NULL,
    check_out_time  TIMESTAMP,
    approved_by     UUID,
    status          VARCHAR(16) NOT NULL DEFAULT 'pending',
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_hostel_visitors_school ON hostel_visitors(school_id, status);
CREATE INDEX IF NOT EXISTS idx_hostel_visitors_student ON hostel_visitors(student_id);

COMMIT;
```

### Rollback: `migration_047_rollback.sql`

```sql
BEGIN;
DROP TABLE IF EXISTS hostel_visitors;
DROP TABLE IF EXISTS hostel_attendance;
DROP TABLE IF EXISTS hostel_assignments;
DROP TABLE IF EXISTS hostel_rooms;
DROP TABLE IF EXISTS hostel_buildings;
COMMIT;
```

### Migration Validation

- Verify all 5 tables created with correct columns
- Verify FK constraints in place
- Verify `UNIQUE(building_id, room_number)` constraint works
- Verify `UNIQUE(school_id, student_id, date)` constraint works
- Verify all indexes created
- Run `SELECT count(*) FROM hostel_buildings` — should be 0 (new feature)

---

## Appendix F: Observability

### Structured Logging

| Log Level | Event | Context Fields |
|---|---|---|
| INFO | Building created | `schoolId, buildingId, name, wardenId, capacity` |
| INFO | Building updated | `schoolId, buildingId, fields, oldValues, newValues` |
| INFO | Room created | `buildingId, roomId, roomNumber, floor, capacity` |
| INFO | Student assigned | `schoolId, assignmentId, studentId, roomId, checkInDate` |
| INFO | Student checked out | `schoolId, assignmentId, studentId, checkOutDate` |
| INFO | Attendance marked | `schoolId, date, count, markedBy` |
| INFO | Visitor requested | `schoolId, visitorId, studentId, visitorName` |
| INFO | Visitor approved | `schoolId, visitorId, approvedBy` |
| INFO | Visitor rejected | `schoolId, visitorId, rejectedBy` |
| INFO | Visitor checked in | `schoolId, visitorId, checkInTime` |
| INFO | Visitor checked out | `schoolId, visitorId, checkOutTime` |
| WARN | Room at capacity | `roomId, roomNumber, occupied, capacity` |
| WARN | Fee creation failed | `schoolId, assignmentId, error` |
| WARN | Warden not assigned | `buildingId, buildingName` |
| ERROR | Assignment failed | `schoolId, studentId, roomId, error` |
| ERROR | Attendance marking failed | `schoolId, date, error` |

### Metrics

| Metric | Type | Labels | Description |
|---|---|---|---|
| `hostel_buildings_total` | Gauge | `school_id` | Total buildings per school |
| `hostel_rooms_total` | Gauge | `school_id, building_id` | Total rooms per building |
| `hostel_occupancy_rate` | Gauge | `school_id, building_id` | Occupancy percentage |
| `hostel_assignments_active` | Gauge | `school_id` | Active assignments |
| `hostel_attendance_marked_total` | Counter | `school_id, status` | Attendance records by status |
| `hostel_visitors_total` | Counter | `school_id, status` | Visitors by status |
| `hostel_fee_creation_duration` | Histogram | — | Fee creation latency |
| `hostel_attendance_marking_duration` | Histogram | — | Bulk attendance marking latency |

### Health Checks

| Check | Endpoint | Description |
|---|---|---|
| Hostel tables exist | `/health/hostel` | Verify all 5 hostel tables are accessible |

### Alerts

| Alert | Condition | Severity | Notification |
|---|---|---|---|
| Fee creation failure rate high | `hostel_fee_creation_duration` error rate > 5% | Warning | Email to dev team |
| Attendance not marked by 10 AM | No `hostel_attendance_marked_total` for today by 10:30 AM | Warning | Push to warden |
| Occupancy rate > 95% | `hostel_occupancy_rate` > 95% | Info | Log only (capacity planning) |
| Visitor approval pending > 24h | `hostel_visitors_total` pending > 24 hours | Info | Push to warden |

### Dashboards

| Dashboard | Panels | Audience |
|---|---|---|
| Hostel Overview | Buildings, rooms, occupancy, active assignments | School Admin |
| Warden Dashboard | Occupancy, today's attendance, pending visitors | Warden |
| Attendance Trends | Daily attendance rates, absent patterns | School Admin |
| Visitor Analytics | Visitor frequency, approval times, peak hours | School Admin |
