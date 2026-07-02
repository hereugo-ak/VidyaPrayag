# Timetable & Class Teacher System — Master Plan

> **Feature:** Complete Timetable/Schedule management + School Day Configuration + Class Teacher allocation + Class Dashboard + Teacher Duty Roster + Room/Lab Management + Teacher Ecosystem
> **Status:** PLANNING → BUILD READY
> **Last updated:** 2026-07-01 (v2 — expanded with full ecosystem)
> **Target:** $100M Series-A grade school operating system

---

## 0. Executive Summary

This plan delivers **seven deeply interconnected subsystems** that form the complete academic operating backbone of any school. A school is not just a set of classes with timetables — it is a living organism where teachers wear many hats: educators, mentors, duty officers, invigilators, counselors, and administrators. This plan reflects that reality.

### The Seven Subsystems

1. **School Day Configuration** — Fully customizable bell schedule: number of periods, duration of each period (variable per period), morning assembly, break/lunch/recess, zero period, different schedules for different weekdays (e.g., Saturday half-day), per-class-level variations (primary vs secondary). This is the foundation — everything else builds on it.
2. **Timetable Management** — Class teachers create/upload weekly timetables for their class using the school day structure. Admin approves. Parents and teachers see it live. PDF upload with manual fallback. Presets to minimize teacher effort. Double periods for labs. Break periods embedded in the grid.
3. **Class Teacher Allocation** — Admin assigns one teacher per class+section as the "Class Teacher" (CT). CT is responsible for attendance, parent queries, behavior, wellness, and admin accountability. Warning on duplicate CT assignment. CT Dashboard consolidates all their responsibilities.
4. **Class Dashboard** — A unified "class view" for admin (all classes) and teachers (their classes) showing today's data: students present, fee status, test reports, timetable with live/current/done markers, student & teacher lists with clickable profiles, and substitute-teacher allocation when a teacher is absent.
5. **Teacher Duty Roster** — Beyond teaching: corridor duty, playground supervision, assembly duty, exam invigilation, library duty, bus duty, event supervision. Admin assigns duties; teachers see them in their daily schedule. Conflict-checked against teaching periods.
6. **Room & Lab Management** — Classroom mapping (which class meets where), science/computer lab booking with conflict detection, shared resource management (AV equipment, projector, sports equipment). Rooms are first-class entities, not free-text fields.
7. **Teacher Ecosystem** — The teacher's complete daily life: free periods (planning/correction time), staff meetings, professional development sessions, workload analysis (teaching hours + duty hours + total workload), and integration with existing features (attendance, marks, homework, syllabus, lesson plans, behavior tracking, wellness, PTM, leave).

### Design Philosophy

We are building an **AI agentic operating system for schools**. Every feature must:
- Be **role-aware** (Parent, Teacher, School Admin) with clean separation
- Follow **SOLID + MVVM + Clean Architecture** (per `DEVELOPMENT_STANDARDS.md`)
- Be **offline-resilient** (Room cache for reads, outbox for writes — per offline mode initiative)
- Be **accessible to 25-60 year old parents** (large touch targets, clear language, minimal steps)
- **Maximize reuse** — existing screens, components, APIs are extended, not duplicated
- Be **conflict-free** — teacher double-booking checks, timetable version control, approval workflow
- Be **scalable** — designed for 1 to 10,000+ schools, each with 5 to 100 classes

---

## 1. Deep System Audit

### 1.1 Existing Timetable Infrastructure

| Layer | File | Status |
|---|---|---|
| **DB** | `Tables.kt:1194-1230` — `TeacherPeriodsTable` | ✅ Exists, well-typed (T-101 migration) |
| **DB** | `Tables.kt:1239-1257` — `PeriodExceptionsTable` | ✅ Exists, handles one-off overrides |
| **DB** | `Tables.kt:302-326` — `TeacherSubjectAssignmentsTable` with `is_class_teacher` | ✅ Exists |
| **DB** | `Tables.kt:269-278` — `SchoolClassesTable` | ✅ Exists |
| **DB** | `Tables.kt:278-289` — `SchoolSubjectsTable` | ✅ Exists |
| **Server** | `SchoolTimetableRouting.kt` — `GET /api/v1/school/timetable` | ✅ Read-only admin endpoint |
| **Server** | `TeacherDayRouting.kt` — `GET /api/v1/teacher/day` + `week` | ✅ Teacher resolved-day view |
| **Server** | `TeacherClassesRouting.kt:621` — `weeklyTimetableInTxn()` | ✅ Teacher class detail weekly view |
| **Server** | `ParentAcademicsRouting.kt:356` — `GET /api/v1/parent/timetable` | ✅ Parent read endpoint |
| **Server** | `TeacherAssignmentRouting.kt` — CRUD for TSA | ✅ Admin assignment management |
| **Web** | `website/src/components/admin/calendar/SchoolCalendar.tsx` | ✅ Admin web calendar |
| **Web** | `website/src/components/admin/calendar/WeekGrid.tsx` | ✅ Web week grid |
| **App (Parent)** | `ParentScheduleCard.kt` — 3-face swipe card | ✅ Parent schedule card |
| **App (Teacher)** | `TeacherClassesScreenV2.kt` — class detail with weekly timetable | ✅ Teacher class detail |
| **App (Admin)** | No timetable screen in app | ❌ Missing admin app timetable view |

### 1.2 Gap Analysis

| Gap | Impact | Priority |
|---|---|---|
| No timetable CREATE/EDIT endpoint | CTs cannot create/edit timetables | 🔴 Critical |
| No approval workflow (DRAFT→PENDING→APPROVED→PUBLISHED) | Admin cannot review before go-live | 🔴 Critical |
| No PDF upload infrastructure | Schools with PDF timetables can't use them | 🟡 High |
| No timetable presets | Teachers build from scratch every time | 🟡 High |
| No class teacher management UI | Admin cannot allocate CTs | 🔴 Critical |
| No class dashboard for admin | Admin lacks unified class view | 🟡 High |
| No substitute teacher allocation UI | Absent teacher's classes can't be reassigned | 🟡 High |
| No teacher availability check endpoint | Substitute allocation may double-book | 🟡 High |
| No parent query routing to CT | Parent queries don't reach the right teacher | 🟡 High |
| No admin app timetable view | Admin on mobile can't view timetables | 🟢 Medium |
| No pre-save conflict detection | Double-booking only caught at DB error | 🟡 High |
| **No school day configuration** — no bell schedule, no customizable period count/duration, no break/lunch/assembly structure | Every school must fit the same rigid period structure | 🔴 Critical |
| **No teacher duty roster** — no corridor/playground/assembly/invigilation duty tracking | Teachers' non-teaching duties invisible to system | 🟡 High |
| **No room/lab management** — `room` is free-text in `TeacherPeriodsTable`, no entity | Room conflicts, lab double-booking undetectable | 🟡 High |
| **No free period tracking** — teacher schedule only shows teaching periods | Can't see planning/correction time, can't detect true availability | 🟡 High |
| **No teacher workload analysis** — `TeacherCardWorkloadDto` has basic counts but no hours-based analysis | Can't flag overworked teachers, can't balance load | 🟢 Medium |
| **No staff meeting / event scheduling for teachers** — calendar events exist but no teacher-specific meeting calendar | Staff meetings invisible in teacher daily view | 🟢 Medium |
| **No CT responsibilities dashboard** — CT has no consolidated view of all their duties | CT must jump between 5+ screens to do their job | 🟡 High |

### 1.3 Reuse Map

| Component | File | Reuse For |
|---|---|---|
| `TeacherPeriodsTable` schema | `Tables.kt:1194` | Timetable CRUD — already has all columns |
| `PeriodExceptionsTable` schema | `Tables.kt:1239` | Substitute teacher, cancelled classes |
| `TeacherSubjectAssignmentsTable.isClassTeacher` | `Tables.kt:317` | CT flag — already exists |
| `SchoolTimetableRouting.kt` GET endpoint | `SchoolTimetableRouting.kt:82` | Admin web read — already works |
| `ParentAcademicsRouting.kt` GET /timetable | `ParentAcademicsRouting.kt:356` | Parent read — already works |
| `TeacherDayRouting.kt` resolved day | `TeacherDayRouting.kt` | Teacher today/week — already works |
| `TeacherClassesScreenV2.kt` class detail | `TeacherClassesScreenV2.kt` | Teacher class dashboard — extend |
| `ParentScheduleCard.kt` 3-face card | `ParentScheduleCard.kt` | Parent timetable display — already works |
| `StudentProfileScreenV2.kt` (admin) | `StudentProfileScreenV2.kt` | Student profile from class dashboard |
| `TeacherStudentProfileScreenV2.kt` | `TeacherStudentProfileScreenV2.kt` | Student profile from teacher class view |
| `SchoolCalendar.tsx` + `WeekGrid.tsx` (web) | `website/src/components/admin/calendar/` | Admin web timetable — extend for edit |
| `CalendarSlotPanel.tsx` (web) | `website/src/components/admin/calendar/` | Period drill-down — extend |
| `TeacherAssignmentRouting.kt` | `TeacherAssignmentRouting.kt` | TSA CRUD — extend for CT allocation |
| `VStateHost`, `VCard`, `VBadge`, `VAvatar`, `VButton`, `VTheme` | `ui/v2/components/` | All UI primitives |
| `NetworkResult` | `core/network/NetworkResult.kt` | API result handling |
| `PreferenceRepository` | `core/prefs/` | Token management |
| Koin DI | `Koin.kt` | VM wiring |

---

## 2. UX Analysis — All Three Portals

### 2.1 Admin (School Admin)

#### 2.1.1 Class Teacher Allocation
- Admin opens People tab → "Class Teachers" section
- List of all classes with their assigned CT
- Tap a class → "Assign Class Teacher" dropdown
- Dropdown shows all teachers in school
- Select teacher → if already CT of another class: warning "John is already CT of Grade 5-A. Assign to Grade 6-B as well?" → [Confirm] [Cancel]
- On confirm: `is_class_teacher = true` for that TSA row
- Notify teacher: "You are now class teacher of Grade 6-B"

#### 2.1.2 Timetable Approval Queue
- Admin gets notification: "Grade 7-A timetable submitted by CT"
- Opens Timetable tab → "Pending Approvals" section
- See draft timetable in week grid view
- Check for conflicts (red highlights on overlapping periods)
- [Approve] [Request Changes] [Reject]
- On approve: status → PUBLISHED, periods written to `TeacherPeriodsTable`
- Notify CT: "Grade 7-A timetable approved"
- Notify parents: "Grade 7-A timetable updated" (push)
- Notify teachers in timetable: "Your schedule has been updated"

#### 2.1.3 Class Dashboard (Admin App + Web)
- Admin opens "Classes" tab → grid of all classes
- Each card: class name, section, CT name, student count, today's attendance %
- Tap a class → Class Dashboard with:
  - KPI tiles: present count, pending fees, upcoming tests
  - Today's timetable with done/now/upcoming markers
  - Student list (tap → profile) and teacher list (tap → profile)
  - Weekly timetable grid [Week] [Day] toggle
- Tap a period with absent teacher → "Allocate Substitute" → dropdown of free teachers

#### 2.1.4 Substitute Teacher Allocation
- Admin sees period where teacher is on leave (or marks teacher absent)
- Tap period → "Teacher absent" banner → "Allocate Substitute"
- Server checks: which teachers are FREE during this weekday+time?
- Dropdown of available teachers (not teaching, not on duty, not on leave)
- Select → creates `PeriodException` (kind=SUBSTITUTION)
- Notify substitute: "You're covering Grade 7-A Maths at 9:00 AM today"

#### 2.1.5 School Day Configuration (NEW)
```
Admin opens Settings → "School Day Structure"
  → Define bell schedule:
    ┌──────────────────────────────────────────────┐
    │ SCHOOL DAY STRUCTURE                          │
    │                                               │
    │ Default Schedule: [Mon-Fri] [Sat] [Sun]       │
    │                                               │
    │ ┌─────┬──────────┬──────────┬───────────────┐│
    │ │ #   │ Start    │ End      │ Type          ││
    │ ├─────┼──────────┼──────────┼───────────────┤│
    │ │ 0   │ 08:00    │ 08:30    │ Assembly      ││
    │ │ 1   │ 08:30    │ 09:15    │ Teaching      ││
    │ │ 2   │ 09:15    │ 10:00    │ Teaching      ││
    │ │ 3   │ 10:00    │ 10:45    │ Teaching      ││
    │ │ -   │ 10:45    │ 11:00    │ Short Break   ││
    │ │ 4   │ 11:00    │ 11:45    │ Teaching      ││
    │ │ 5   │ 11:45    │ 12:30    │ Teaching      ││
    │ │ -   │ 12:30    │ 13:00    │ Lunch Break   ││
    │ │ 6   │ 13:00    │ 13:45    │ Teaching      ││
    │ │ 7   │ 13:45    │ 14:30    │ Teaching      ││
    │ │ 8   │ 14:30    │ 15:15    │ Teaching      ││
    │ └─────┴──────────┴──────────┴───────────────┘│
    │                                               │
    │ [+ Add Period] [+ Add Break]                  │
    │                                               │
    │ Saturday Schedule:                            │
    │   [ ] Same as weekday                         │
    │   [✓] Half-day (4 periods)                    │
    │   [ ] No school                               │
    │                                               │
    │ Per-class-level overrides:                    │
    │   Primary (1-5): 6 periods × 40 min           │
    │   Secondary (6-12): 8 periods × 45 min        │
    │                                               │
    │ [Save]                                        │
    └──────────────────────────────────────────────┘
```

Key customizations:
- **Period count**: 4 to 12 periods per day
- **Period duration**: variable per period (period 1 = 45 min, period 2 = 50 min)
- **Break types**: Assembly, Short Break, Lunch Break, Recess, Prayer
- **Zero period**: optional pre-school period (e.g., 07:30-08:00 for sports/yoga)
- **Day-specific schedules**: Saturday half-day, Wednesday lab-day (double periods)
- **Class-level overrides**: Primary has shorter periods, Secondary has longer
- **Double periods**: labs/practicals occupy 2 consecutive slots
- **Flexible start time**: school can start at 07:30, 08:00, 09:00

#### 2.1.6 Teacher Duty Roster (NEW)
```
Admin opens "Duties" tab → weekly duty roster
  → Assign duties to teachers:
    ┌────────────────────────────────────────────────┐
    │ DUTY ROSTER — Week of July 1                   │
    │                                                │
    │        Mon          Tue          Wed           │
    │ Assembly: John      Mary         Bob           │
    │ Corridor: Alice     John         Mary          │
    │ Playground: Bob     Alice        John          │
    │ Bus Duty: Carol     Carol        Carol         │
    │ Library: —          —           Bob            │
    │                                                │
    │ [Assign Duty] → Select teacher → Select slot   │
    │ → Conflict check: teacher can't be on duty     │
    │   during a teaching period                     │
    └────────────────────────────────────────────────┘
```

Duty types:
- **Corridor duty** — between periods, monitoring hallways
- **Playground supervision** — during break/lunch
- **Assembly duty** — morning assembly management
- **Exam invigilation** — during test/exam periods
- **Bus duty** — arrival/dismissal bus monitoring
- **Library duty** — library supervision
- **Event duty** — annual day, sports day, field trips
- **Recess duty** — break time supervision

#### 2.1.7 Room & Lab Management (NEW)
```
Admin opens "Infrastructure" → "Rooms & Labs"
  → List of rooms: Classroom 101, Lab 1, Lab 2, Computer Lab, Music Room
  → Each room: name, type, capacity, equipment
  → Room allocation: which class uses which room by default
  → Lab booking: teacher books lab for a specific period
    → Conflict check: lab already booked?
    → Shows calendar of lab bookings
```

### 2.2 Teacher (Class Teacher + Subject Teacher)

#### 2.2.1 Class Teacher — Create/Edit Timetable
- CT opens Classes tab → taps their class (where `isClassTeacher=true`)
- "Edit Timetable" button (only visible to CT)
- Timetable Editor: weekly grid with subject dropdown + teacher dropdown per cell
- [Upload PDF] [Use Preset] [Add Period] actions
- [Save as Draft] [Submit for Approval]

#### 2.2.2 PDF Upload Flow
- CT taps "Upload PDF" → file picker
- Upload to server → server attempts parse
- Success → pre-fills grid → CT reviews and adjusts → [Submit]
- Parse error → "Couldn't read the PDF. Please enter manually." → falls back to manual grid

#### 2.2.3 Preset Selection
- CT taps "Use Preset" → server returns presets
- Options: "Standard 8-period day", "Lab-intensive", "Half-day Friday", "Custom from last year"
- Select preset → grid pre-filled with time slots
- CT assigns subjects + teachers to each slot
- [Save as Draft] [Submit for Approval]

#### 2.2.4 Subject Teacher View (Non-CT)
- Opens Classes tab → sees their classes
- Tap a class → existing Class Detail screen (enhanced):
  - Next class card (existing)
  - Attendance snapshot (existing)
  - Weekly timetable (existing, now showing ALL periods for the class)
  - Roster (existing)
  - NEW: "Class Teacher: John D." badge
  - NO "Edit Timetable" button (only CT can edit)

#### 2.2.5 Teacher's Complete Daily View (NEW)
```
Teacher opens Today tab → sees their COMPLETE day:
  ┌──────────────────────────────────────────────────┐
  │ TODAY — Monday, July 1                    08:15   │
  │                                                   │
  │ 08:00-08:30 │ 🏛 Assembly Duty         │ NOW     │
  │ 08:30-09:15 │ 📚 Maths · Grade 7-A     │ UPCOMING│
  │ 09:15-10:00 │ 📚 Maths · Grade 8-B     │         │
  │ 10:00-10:45 │ 📚 Maths · Grade 7-A     │         │
  │ 10:45-11:00 │ ☕ Short Break           │         │
  │ 11:00-11:45 │ 📝 Free Period           │         │
  │ 11:45-12:30 │ 📚 Maths · Grade 9-A     │         │
  │ 12:30-13:00 │ 🍽 Lunch Break           │         │
  │ 13:00-13:45 │ 📝 Free Period           │         │
  │ 13:45-14:30 │ 🧪 Maths Lab · Grade 7-A │         │
  │ 14:30-15:15 │ 🏃 Playground Duty       │         │
  │                                                   │
  │ TODAY'S SUMMARY                                   │
  │ Teaching: 5 periods (3h 45m)                      │
  │ Duties: 2 (Assembly + Playground)                 │
  │ Free: 2 periods (1h 30m)                          │
  │ Breaks: 2 (Short + Lunch)                         │
  │                                                   │
  │ PENDING TASKS                                     │
  │ ⚠ Grade 7-A attendance not marked                 │
  │ ⚠ Grade 8-B homework review pending (3)           │
  │ 📋 Staff meeting at 16:00                         │
  └──────────────────────────────────────────────────┘
```

This is the **teacher's single source of truth** for their day. It merges:
- Teaching periods (from `TeacherPeriodsTable`)
- Duty assignments (from new `TeacherDutiesTable`)
- Breaks (from school day configuration)
- Free periods (calculated: slots with no teaching + no duty)
- Pending tasks (attendance not marked, homework to review)
- Staff meetings / events (from `CalendarEventsTable` filtered for teachers)

#### 2.2.6 Class Teacher Responsibilities Dashboard (NEW)
```
CT opens their class → "My Responsibilities" tab:
  ┌──────────────────────────────────────────────────┐
  │ CLASS TEACHER DASHBOARD — Grade 7-A               │
  │                                                   │
  │ TODAY'S CHECKLIST                                 │
  │ ✅ Attendance marked (32/35 present, 3 absent)    │
  │ ⬜ Verify absent students' reason                 │
  │ ⬜ Check behavior incidents (2 new this week)     │
  │ ⬜ Review homework submissions (3 pending)        │
  │ ⬜ Send weekly progress note to parents           │
  │                                                   │
  │ STUDENT WELLNESS ALERTS                           │
  │ ⚠ Rahul: 3 consecutive absences — follow up      │
  │ ⚠ Priya: Behavior decline this week              │
  │ ℹ Amit: Improving trend — positive note           │
  │                                                   │
  │ PARENT QUERIES (3 unread)                         │
  │ "When is the maths test?" — Mrs. Sharma           │
  │ "Rahul was sick yesterday" — Mr. Khan             │
  │ "PTM slot request" — Mrs. Iyer                    │
  │                                                   │
  │ UPCOMING                                          │
  │ 📋 PTM on July 15 — 12 parents confirmed          │
  │ 📝 Term report cards due July 20                  │
  │ 🎓 Annual day rehearsal — July 18                 │
  │                                                   │
  │ CLASS STATISTICS                                  │
  │ Avg attendance: 91% │ Avg score: 78%              │
  │ Fee pending: 3 students │ Behavior: +12 pts       │
  └──────────────────────────────────────────────────┘
```

This consolidates everything a CT needs to do into one screen:
- **Attendance verification** — not just marking, but following up on absentees
- **Behavior monitoring** — links to behavior tracking system
- **Wellness alerts** — from wellness system (existing spec)
- **Parent queries** — from messaging system, filtered to their class
- **Homework review** — from homework system, showing pending reviews
- **PTM coordination** — from PTM system
- **Report cards** — from report card system
- **Class statistics** — aggregated from attendance, marks, fees, behavior

### 2.3 Parent

#### 2.3.1 View Child's Timetable
- Parent opens Home → Schedule card (existing `ParentScheduleCard`)
- Face 0: Current/next class (existing)
- Swipe → Face 1: Today's full timeline (existing)
- Swipe → Face 2: Weekly timetable (existing)
- All already works — just needs the timetable to be PUBLISHED

#### 2.3.2 Timetable Update Notification
- Parent receives push: "Grade 7-A timetable has been updated"
- Taps notification → opens Academics tab → sees updated weekly timetable

### 2.4 Edge Cases

| Scenario | Handling |
|---|---|
| Two teachers assigned same period for same class | Server conflict check pre-save — reject with error |
| Teacher is CT of two classes | Warning shown, but allowed (some schools require this) |
| Timetable submitted while another is pending | New draft replaces old pending draft; approved stays live |
| PDF corrupted/unreadable | Server returns parse error → UI shows manual fallback |
| Class has no CT assigned | "Edit Timetable" disabled with tooltip |
| Period time overlap within same day | Server validates end_time > start_time and no overlap |
| Holiday on a school day | Calendar event HOLIDAY → resolved-day shows no periods (existing) |
| Teacher on leave during their period | Admin allocates substitute via PeriodException |
| School changes academic year | valid_from/valid_to columns support term revisions (existing) |
| CT transfers to another school | Admin deassigns CT → new CT assigned |
| Timetable approved but needs urgent change | Admin can directly override-publish (bypass approval) |
| Multiple sections of same class | Each section gets its own timetable (section column) |

---

## 3. Database Schema

### 3.1 New Table: `timetable_drafts`

Approval workflow needs a draft table separate from the live `TeacherPeriodsTable`.

```sql
CREATE TABLE timetable_drafts (
    id              UUID PRIMARY KEY,
    school_id       UUID NOT NULL,
    class_id        UUID NOT NULL,
    class_name      TEXT NOT NULL,
    section         VARCHAR(8) DEFAULT 'A',
    academic_year_id UUID,
    created_by      UUID NOT NULL,
    created_by_name TEXT NOT NULL,
    status          VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    reviewed_by     UUID,
    reviewed_at     TIMESTAMP,
    review_note     TEXT DEFAULT '',
    pdf_url         TEXT,
    preset_id       TEXT,
    periods_json    TEXT NOT NULL,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL
);
CREATE INDEX idx_td_school_class ON timetable_drafts(school_id, class_id, section);
CREATE INDEX idx_td_status ON timetable_drafts(school_id, status);
```

**Why JSON for periods_json?** The draft is a working copy — not yet in `TeacherPeriodsTable`. On approval, JSON is parsed and written as individual rows (replacing previous active set for that class+section).

### 3.2 New Table: `timetable_presets`

```sql
CREATE TABLE timetable_presets (
    id              UUID PRIMARY KEY,
    school_id       UUID NOT NULL,
    name            TEXT NOT NULL,
    description     TEXT DEFAULT '',
    period_count    INTEGER NOT NULL,
    days_per_week   INTEGER NOT NULL DEFAULT 6,
    periods_json    TEXT NOT NULL,
    is_system       BOOLEAN DEFAULT false,
    created_at      TIMESTAMP NOT NULL
);
CREATE INDEX idx_tp_school ON timetable_presets(school_id);
```

### 3.3 New Table: `school_day_config`

The foundation — defines the bell schedule for each school. Different schedules per weekday and per class level.

```sql
CREATE TABLE school_day_config (
    id              UUID PRIMARY KEY,
    school_id       UUID NOT NULL,
    name            TEXT NOT NULL,               -- "Default Weekday", "Saturday", "Exam Day"
    applicable_days VARCHAR(20) NOT NULL,        -- "1,2,3,4,5" (Mon-Fri) or "6" (Sat)
    class_level     VARCHAR(20) DEFAULT 'ALL',   -- ALL|PRIMARY|SECONDARY|class_id
    is_active       BOOLEAN DEFAULT true,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL
);
CREATE INDEX idx_sdc_school ON school_day_config(school_id);
```

### 3.4 New Table: `school_day_slots`

The individual time slots within a school day config — periods, breaks, assembly.

```sql
CREATE TABLE school_day_slots (
    id              UUID PRIMARY KEY,
    config_id       UUID NOT NULL,               -- FK school_day_config.id
    school_id       UUID NOT NULL,
    slot_index      INTEGER NOT NULL,            -- 0,1,2,3... (0 = zero period/assembly)
    slot_type       VARCHAR(16) NOT NULL,        -- TEACHING|BREAK|ASSEMBLY|LAB|FREE|ZERO
    label           TEXT DEFAULT '',             -- "Short Break", "Lunch", "Assembly"
    start_time      TIME NOT NULL,
    end_time        TIME NOT NULL,
    is_double       BOOLEAN DEFAULT false,       -- true = this is part of a double period
    double_group    INTEGER DEFAULT 0,           -- groups consecutive double-period slots
    created_at      TIMESTAMP NOT NULL
);
CREATE INDEX idx_sds_config ON school_day_slots(config_id, slot_index);
```

### 3.5 New Table: `teacher_duties`

Non-teaching duties assigned to teachers.

```sql
CREATE TABLE teacher_duties (
    id              UUID PRIMARY KEY,
    school_id       UUID NOT NULL,
    teacher_id      UUID NOT NULL,               -- FK app_users.id
    duty_type       VARCHAR(20) NOT NULL,        -- CORRIDOR|PLAYGROUND|ASSEMBLY|INVIGILATION|BUS|LIBRARY|EVENT|RECESS
    weekday         INTEGER,                     -- 1-7 for recurring, NULL for one-off
    specific_date   DATE,                        -- for one-off duties (e.g., event duty)
    start_time      TIME NOT NULL,
    end_time        TIME NOT NULL,
    location        TEXT DEFAULT '',             -- "Corridor A", "Playground", "Lab 1"
    notes           TEXT DEFAULT '',
    is_active       BOOLEAN DEFAULT true,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL
);
CREATE INDEX idx_td_teacher ON teacher_duties(teacher_id, weekday);
CREATE INDEX idx_td_school_date ON teacher_duties(school_id, specific_date);
```

### 3.6 New Table: `school_rooms`

First-class room entities — replaces free-text `room` column in `TeacherPeriodsTable`.

```sql
CREATE TABLE school_rooms (
    id              UUID PRIMARY KEY,
    school_id       UUID NOT NULL,
    name            TEXT NOT NULL,               -- "Classroom 101", "Science Lab 1"
    room_type       VARCHAR(16) NOT NULL,        -- CLASSROOM|LAB|COMPUTER_LAB|LIBRARY|MUSIC_ROOM|SPORTS|ASSEMBLY_HALL|STAFF_ROOM
    capacity        INTEGER DEFAULT 30,
    floor           VARCHAR(10) DEFAULT '',
    equipment       TEXT DEFAULT '',             -- JSON array: ["projector","whiteboard","AC"]
    is_active       BOOLEAN DEFAULT true,
    created_at      TIMESTAMP NOT NULL
);
CREATE INDEX idx_sr_school ON school_rooms(school_id);
```

### 3.7 New Table: `room_bookings`

Ad-hoc room/lab bookings (beyond default classroom allocation).

```sql
CREATE TABLE room_bookings (
    id              UUID PRIMARY KEY,
    school_id       UUID NOT NULL,
    room_id         UUID NOT NULL,               -- FK school_rooms.id
    booked_by       UUID NOT NULL,               -- FK app_users.id (teacher)
    booking_date    DATE NOT NULL,
    start_time      TIME NOT NULL,
    end_time        TIME NOT NULL,
    purpose         TEXT DEFAULT '',             -- "Science experiment", "Computer practical"
    class_name      TEXT DEFAULT '',
    status          VARCHAR(16) DEFAULT 'CONFIRMED', -- CONFIRMED|CANCELLED
    created_at      TIMESTAMP NOT NULL
);
CREATE INDEX idx_rb_room_date ON room_bookings(room_id, booking_date);
CREATE INDEX idx_rb_school_date ON room_bookings(school_id, booking_date);
```

### 3.8 Existing Tables — No Changes Needed

| Table | Why No Change |
|---|---|
| `TeacherPeriodsTable` | Already has weekday, start/end_time, room, assignment_id, academic_year_id, valid_from/to, is_active. `room` column will be enhanced to reference `school_rooms.id` (nullable for back-compat) |
| `PeriodExceptionsTable` | Already supports SUBSTITUTION with substitute_teacher_id |
| `TeacherSubjectAssignmentsTable` | Already has `is_class_teacher` flag |
| `SchoolClassesTable` | Already has code + name |
| `SchoolSubjectsTable` | Already has class_id + sub_name + sub_code |
| `CalendarEventsTable` | Already supports staff events — reuse for staff meetings |
| `TeacherCardWorkloadDto` | Already has basic workload — extend with hours-based calculation |

### 3.9 Migration Strategy

- **Migration 107**: Create `timetable_drafts` + `timetable_presets` + `school_day_config` + `school_day_slots` + `teacher_duties` + `school_rooms` + `room_bookings` (all additive)
- No destructive changes — all new tables
- Existing `TeacherPeriodsTable` rows remain as the "live published" timetable
- Seed 4 system presets + default school day config (8-period day with breaks)
- Backfill `school_rooms` from existing `TeacherPeriodsTable.room` distinct values (best-effort)

---

## 4. API Design

### 4.1 Timetable Draft Endpoints (Class Teacher)

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/teacher/timetable/drafts` | List drafts by this teacher (all statuses) |
| `GET` | `/api/v1/teacher/timetable/drafts/{classId}` | Get current draft for a class+section |
| `POST` | `/api/v1/teacher/timetable/drafts` | Create/update draft (save periods_json) |
| `POST` | `/api/v1/teacher/timetable/drafts/{id}/submit` | Submit draft for admin approval |
| `DELETE` | `/api/v1/teacher/timetable/drafts/{id}` | Delete draft (only if DRAFT or REJECTED) |
| `POST` | `/api/v1/teacher/timetable/upload-pdf` | Upload PDF for parsing |
| `GET` | `/api/v1/teacher/timetable/presets` | List available presets |
| `GET` | `/api/v1/teacher/timetable/teachers-for-class` | Teachers assigned to class (dropdown) |
| `GET` | `/api/v1/teacher/timetable/subjects-for-class` | Subjects for class (dropdown) |

### 4.2 Timetable Approval Endpoints (Admin)

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/school/timetable/pending` | List pending timetable drafts |
| `GET` | `/api/v1/school/timetable/drafts/{id}` | Get draft detail with periods |
| `POST` | `/api/v1/school/timetable/drafts/{id}/approve` | Approve → publish to TeacherPeriodsTable |
| `POST` | `/api/v1/school/timetable/drafts/{id}/reject` | Reject with note |
| `POST` | `/api/v1/school/timetable/drafts/{id}/override-publish` | Admin emergency publish |
| `GET` | `/api/v1/school/timetable/all` | All classes' timetables with status |

### 4.3 Class Teacher Allocation Endpoints (Admin)

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/school/class-teachers` | List all class+section with their CT |
| `POST` | `/api/v1/school/class-teachers/assign` | Assign CT (with duplicate warning) |
| `POST` | `/api/v1/school/class-teachers/unassign` | Remove CT from class+section |
| `GET` | `/api/v1/school/class-teachers/check-duplicate` | Check if teacher is already CT elsewhere |

### 4.4 Class Dashboard Endpoints (Admin + Teacher)

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/school/classes/overview` | All classes: CT, student count, attendance %, fees, tests |
| `GET` | `/api/v1/school/classes/{classId}/dashboard` | Full class dashboard |
| `GET` | `/api/v1/teacher/classes/{assignmentId}/dashboard` | Teacher's class dashboard (scoped) |

### 4.5 Substitute Teacher Endpoints (Admin)

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/school/teachers/available` | Teachers free during weekday+time slot (not teaching, not on duty, not on leave) |
| `POST` | `/api/v1/school/timetable/allocate-substitute` | Create PeriodException (SUBSTITUTION) |

### 4.6 School Day Configuration Endpoints (Admin)

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/school/day-config` | List all school day configs |
| `GET` | `/api/v1/school/day-config/{id}` | Get config with all slots |
| `POST` | `/api/v1/school/day-config` | Create config (with slots) |
| `PUT` | `/api/v1/school/day-config/{id}` | Update config + slots |
| `DELETE` | `/api/v1/school/day-config/{id}` | Deactivate config |
| `GET` | `/api/v1/school/day-config/for-class` | Get applicable config for a class+weekday |

### 4.7 Teacher Duty Roster Endpoints (Admin)

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/school/duties` | List all duties (filter by weekday, teacher, type) |
| `POST` | `/api/v1/school/duties` | Assign duty to teacher (conflict check) |
| `PUT` | `/api/v1/school/duties/{id}` | Update duty |
| `DELETE` | `/api/v1/school/duties/{id}` | Remove duty |
| `GET` | `/api/v1/teacher/duties` | Teacher's own duties (today + weekly) |

### 4.8 Room & Lab Management Endpoints (Admin + Teacher)

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/school/rooms` | List all rooms |
| `POST` | `/api/v1/school/rooms` | Create room |
| `PUT` | `/api/v1/school/rooms/{id}` | Update room |
| `DELETE` | `/api/v1/school/rooms/{id}` | Deactivate room |
| `GET` | `/api/v1/school/rooms/available` | Available rooms for a date+time (for booking) |
| `POST` | `/api/v1/school/rooms/book` | Book a room (conflict check) |
| `DELETE` | `/api/v1/school/rooms/bookings/{id}` | Cancel booking |
| `GET` | `/api/v1/teacher/rooms/bookings` | Teacher's own room bookings |

### 4.9 Teacher Complete Day Endpoints (Teacher)

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/teacher/complete-day` | Merged view: teaching periods + duties + breaks + free periods + pending tasks |
| `GET` | `/api/v1/teacher/workload` | Workload analysis: teaching hours, duty hours, free time, total workload |

### 4.10 CT Responsibilities Dashboard Endpoint (Teacher)

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/teacher/class-teacher/dashboard` | CT dashboard: checklist, wellness alerts, parent queries, upcoming, stats |

### 4.11 Key DTOs

```kotlin
@Serializable
data class TimetableDraftDto(
    val id: String,
    @SerialName("class_id") val classId: String,
    @SerialName("class_name") val className: String,
    val section: String,
    val status: String,              // DRAFT|PENDING|APPROVED|REJECTED|PUBLISHED
    @SerialName("created_by") val createdBy: String,
    @SerialName("created_by_name") val createdByName: String,
    @SerialName("reviewed_by") val reviewedBy: String? = null,
    @SerialName("review_note") val reviewNote: String = "",
    @SerialName("pdf_url") val pdfUrl: String? = null,
    val periods: List<DraftPeriodDto>,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class DraftPeriodDto(
    val weekday: Int,                // 1=Mon..7=Sun
    @SerialName("start_time") val startTime: String,  // "HH:mm"
    @SerialName("end_time") val endTime: String,
    val subject: String,
    @SerialName("teacher_id") val teacherId: String?,
    @SerialName("teacher_name") val teacherName: String,
    val room: String,
    @SerialName("assignment_id") val assignmentId: String? = null,
    val conflict: Boolean = false,
    @SerialName("conflict_reason") val conflictReason: String? = null,
)

@Serializable
data class CreateDraftRequest(
    @SerialName("class_id") val classId: String,
    val section: String,
    val periods: List<DraftPeriodDto>,
    @SerialName("pdf_url") val pdfUrl: String? = null,
    @SerialName("preset_id") val presetId: String? = null,
)

@Serializable
data class PdfUploadResponse(
    val success: Boolean,
    val periods: List<DraftPeriodDto>? = null,
    val error: String? = null,
)

@Serializable
data class ClassTeacherDto(
    @SerialName("class_id") val classId: String,
    @SerialName("class_name") val className: String,
    val section: String,
    @SerialName("teacher_id") val teacherId: String?,
    @SerialName("teacher_name") val teacherName: String?,
    @SerialName("is_class_teacher") val isClassTeacher: Boolean,
    @SerialName("duplicate_warning") val duplicateWarning: String? = null,
)

@Serializable
data class AssignClassTeacherRequest(
    @SerialName("class_id") val classId: String,
    val section: String,
    @SerialName("teacher_id") val teacherId: String,
    @SerialName("force") val force: Boolean = false,
)

@Serializable
data class ClassDashboardDto(
    @SerialName("class_id") val classId: String,
    @SerialName("class_name") val className: String,
    val section: String,
    @SerialName("class_teacher_name") val classTeacherName: String?,
    @SerialName("student_count") val studentCount: Int,
    @SerialName("present_today") val presentToday: Int,
    @SerialName("absent_today") val absentToday: Int,
    @SerialName("pending_fees_count") val pendingFeesCount: Int,
    @SerialName("pending_fees_amount") val pendingFeesAmount: Double,
    @SerialName("upcoming_tests") val upcomingTests: List<ClassTestDto>,
    @SerialName("today_timetable") val todayTimetable: List<DashboardPeriodDto>,
    @SerialName("weekly_timetable") val weeklyTimetable: List<TimetableWeekdayDto>,
    val students: List<ClassStudentSummaryDto>,
    val teachers: List<ClassTeacherSummaryDto>,
)

@Serializable
data class DashboardPeriodDto(
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    val subject: String,
    @SerialName("teacher_name") val teacherName: String,
    @SerialName("teacher_id") val teacherId: String,
    val room: String,
    val status: String,              // done|now|upcoming|cancelled|substituted
    @SerialName("substitute_name") val substituteName: String? = null,
    @SerialName("attendance_marked") val attendanceMarked: Boolean = false,
)

@Serializable
data class ClassStudentSummaryDto(
    val id: String,
    val name: String,
    @SerialName("roll_number") val rollNumber: Int?,
    @SerialName("attendance_rate") val attendanceRate: Double,
    @SerialName("present_today") val presentToday: Boolean?,
    @SerialName("fee_status") val feeStatus: String,    // paid|pending|overdue
)

@Serializable
data class ClassTeacherSummaryDto(
    val id: String,
    val name: String,
    val subject: String,
    @SerialName("is_class_teacher") val isClassTeacher: Boolean,
    @SerialName("present_today") val presentToday: Boolean?,
)

@Serializable
data class AvailableTeacherDto(
    val id: String,
    val name: String,
    val subjects: List<String>,
    @SerialName("is_free") val isFree: Boolean,
)

@Serializable
data class AllocateSubstituteRequest(
    @SerialName("period_id") val periodId: String,
    val date: String,
    @SerialName("substitute_teacher_id") val substituteTeacherId: String,
    val note: String = "",
)

// ── School Day Configuration ──
@Serializable
data class SchoolDayConfigDto(
    val id: String,
    val name: String,
    @SerialName("applicable_days") val applicableDays: String,  // "1,2,3,4,5"
    @SerialName("class_level") val classLevel: String,          // ALL|PRIMARY|SECONDARY
    val slots: List<SchoolDaySlotDto>,
    @SerialName("is_active") val isActive: Boolean,
)

@Serializable
data class SchoolDaySlotDto(
    @SerialName("slot_index") val slotIndex: Int,
    @SerialName("slot_type") val slotType: String,   // TEACHING|BREAK|ASSEMBLY|LAB|FREE|ZERO
    val label: String,
    @SerialName("start_time") val startTime: String,  // "HH:mm"
    @SerialName("end_time") val endTime: String,
    @SerialName("is_double") val isDouble: Boolean = false,
    @SerialName("double_group") val doubleGroup: Int = 0,
)

@Serializable
data class CreateSchoolDayConfigRequest(
    val name: String,
    @SerialName("applicable_days") val applicableDays: String,
    @SerialName("class_level") val classLevel: String,
    val slots: List<SchoolDaySlotDto>,
)

// ── Teacher Duty ──
@Serializable
data class TeacherDutyDto(
    val id: String,
    @SerialName("teacher_id") val teacherId: String,
    @SerialName("teacher_name") val teacherName: String,
    @SerialName("duty_type") val dutyType: String,    // CORRIDOR|PLAYGROUND|ASSEMBLY|...
    val weekday: Int?,                                 // null for one-off
    @SerialName("specific_date") val specificDate: String?,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    val location: String,
    val notes: String,
    @SerialName("conflict") val conflict: Boolean = false,
    @SerialName("conflict_reason") val conflictReason: String? = null,
)

@Serializable
data class AssignDutyRequest(
    @SerialName("teacher_id") val teacherId: String,
    @SerialName("duty_type") val dutyType: String,
    val weekday: Int?,
    @SerialName("specific_date") val specificDate: String?,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    val location: String = "",
    val notes: String = "",
)

// ── Room Management ──
@Serializable
data class SchoolRoomDto(
    val id: String,
    val name: String,
    @SerialName("room_type") val roomType: String,    // CLASSROOM|LAB|COMPUTER_LAB|...
    val capacity: Int,
    val floor: String,
    val equipment: List<String>,
    @SerialName("is_active") val isActive: Boolean,
)

@Serializable
data class RoomBookingDto(
    val id: String,
    @SerialName("room_id") val roomId: String,
    @SerialName("room_name") val roomName: String,
    @SerialName("booked_by") val bookedBy: String,
    @SerialName("booked_by_name") val bookedByName: String,
    @SerialName("booking_date") val bookingDate: String,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    val purpose: String,
    @SerialName("class_name") val className: String,
    val status: String,
)

@Serializable
data class BookRoomRequest(
    @SerialName("room_id") val roomId: String,
    @SerialName("booking_date") val bookingDate: String,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    val purpose: String,
    @SerialName("class_name") val className: String = "",
)

// ── Teacher Complete Day ──
@Serializable
data class TeacherCompleteDayDto(
    val date: String,
    val slots: List<CompleteDaySlotDto>,
    val summary: TeacherDaySummaryDto,
    @SerialName("pending_tasks") val pendingTasks: List<PendingTaskDto>,
)

@Serializable
data class CompleteDaySlotDto(
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    val type: String,             // TEACHING|DUTY|BREAK|FREE|ASSEMBLY|LAB|MEETING
    val label: String,            // "Maths · Grade 7-A" or "Corridor Duty" or "Lunch Break"
    @SerialName("class_name") val className: String?,
    val subject: String?,
    val room: String?,
    val status: String,           // done|now|upcoming
    @SerialName("substitute_name") val substituteName: String? = null,
    @SerialName("duty_type") val dutyType: String? = null,
    @SerialName("duty_location") val dutyLocation: String? = null,
)

@Serializable
data class TeacherDaySummaryDto(
    @SerialName("teaching_periods") val teachingPeriods: Int,
    @SerialName("teaching_minutes") val teachingMinutes: Int,
    @SerialName("duty_count") val dutyCount: Int,
    @SerialName("duty_minutes") val dutyMinutes: Int,
    @SerialName("free_periods") val freePeriods: Int,
    @SerialName("free_minutes") val freeMinutes: Int,
    @SerialName("break_count") val breakCount: Int,
    @SerialName("break_minutes") val breakMinutes: Int,
    @SerialName("total_workload_minutes") val totalWorkloadMinutes: Int,
)

@Serializable
data class PendingTaskDto(
    val type: String,             // ATTENDANCE|HOMEWORK_REVIEW|BEHAVIOR|WELLNESS|MESSAGE|MEETING
    val label: String,
    val priority: String,         // HIGH|MEDIUM|LOW
    @SerialName("class_name") val className: String?,
    @SerialName("student_name") val studentName: String?,
    @SerialName("due_date") val dueDate: String?,
)

// ── CT Dashboard ──
@Serializable
data class CtDashboardDto(
    val checklist: List<CtChecklistItemDto>,
    @SerialName("wellness_alerts") val wellnessAlerts: List<CtWellnessAlertDto>,
    @SerialName("parent_queries") val parentQueries: List<CtParentQueryDto>,
    val upcoming: List<CtUpcomingItemDto>,
    val stats: CtClassStatsDto,
)

@Serializable
data class CtChecklistItemDto(
    val id: String,
    val label: String,
    val done: Boolean,
    val priority: String,
)

@Serializable
data class CtWellnessAlertDto(
    @SerialName("student_id") val studentId: String,
    @SerialName("student_name") val studentName: String,
    val alert: String,
    val severity: String,        // URGENT|CONCERN|INFO
)

@Serializable
data class CtParentQueryDto(
    val id: String,
    @SerialName("parent_name") val parentName: String,
    @SerialName("student_name") val studentName: String,
    val message: String,
    @SerialName("received_at") val receivedAt: String,
    val unread: Boolean,
)

@Serializable
data class CtUpcomingItemDto(
    val type: String,            // PTM|REPORT_CARD|EVENT|TEST
    val label: String,
    val date: String,
    val details: String,
)

@Serializable
data class CtClassStatsDto(
    @SerialName("avg_attendance") val avgAttendance: Double,
    @SerialName("avg_score") val avgScore: Double,
    @SerialName("fee_pending_count") val feePendingCount: Int,
    @SerialName("behavior_score") val behaviorScore: Int,
)
```

---

## 5. End-to-End Architecture Flow

### 5.1 Timetable Creation → Approval → Publication

```
CT opens class in app → taps "Edit Timetable"
  ↓
GET /teacher/timetable/drafts/{classId} → existing draft or null
  ↓
CT builds timetable:
  A: Upload PDF → POST /timetable/upload-pdf → parse → pre-fill
  B: Use Preset → GET /timetable/presets → select → pre-fill slots
  C: Manual entry → add periods one by one
  ↓
Each period: subject dropdown (TSA) + teacher dropdown (TSA) + room + time
  ↓
Server validates on save:
  - No teacher double-booking (same weekday + overlapping time)
  - No class double-booking (same class+section+weekday+time)
  - end_time > start_time
  ↓
POST /timetable/drafts → saves as DRAFT (periods_json)
  ↓
CT taps "Submit for Approval" → POST /drafts/{id}/submit → status=PENDING
  ↓
Admin gets notification → GET /school/timetable/pending
  ↓
Admin reviews draft in week grid → conflict highlights
  ↓
[Approve] → POST /drafts/{id}/approve
  → Server parses periods_json
  → Deactivates old TeacherPeriodsTable rows for this class+section (is_active=false)
  → Inserts new TeacherPeriodsTable rows from draft
  → Draft status = PUBLISHED
  → Notify CT: "approved"
  → Notify parents: "timetable updated"
  → Notify teachers in timetable: "schedule updated"
  ↓
[Reject] → POST /drafts/{id}/reject with note
  → Draft status = REJECTED
  → Notify CT: "rejected: {note}"
```

### 5.2 Class Teacher Allocation Flow

```
Admin opens People tab → "Class Teachers" section
  ↓
GET /school/class-teachers → list of all classes with CT status
  ↓
Admin taps unassigned class → "Assign Class Teacher"
  → Dropdown of all active teachers in school
  ↓
Select teacher → GET /class-teachers/check-duplicate?teacher_id=X
  → If teacher is already CT elsewhere:
    ⚠️ "John is already CT of Grade 5-A. Continue?"
    → Admin confirms → POST /class-teachers/assign with force=true
  → If not duplicate:
    → POST /class-teachers/assign
  ↓
Server: UPDATE teacher_subject_assignments SET is_class_teacher=true
  WHERE teacher_id=X AND class_id=Y AND section=Z
  ↓
Notify teacher: "You are now class teacher of Grade 6-B"
```

### 5.3 Substitute Teacher Allocation Flow

```
Admin opens Class Dashboard → sees period with absent teacher
  ↓
Tap period → "Allocate Substitute"
  ↓
GET /school/teachers/available?weekday=1&start_time=09:00&end_time=09:45
  → Server queries: teachers NOT in TeacherPeriodsTable for this weekday+time
  → Returns list of AvailableTeacherDto
  ↓
Admin selects teacher → POST /timetable/allocate-substitute
  → Server creates PeriodException (kind=SUBSTITUTION, substitute_teacher_id=X)
  → Server double-checks: substitute has no conflicting period
  ↓
Notify substitute: "You're covering Grade 7-A Maths at 9:00 AM today"
  → Resolved-day endpoint now shows substitute for this period
```

### 5.4 Class Dashboard Data Flow

```
Admin/Teacher opens Class Dashboard
  ↓
GET /school/classes/{classId}/dashboard
  ↓
Server assembles in one transaction:
  1. Class info (SchoolClassesTable)
  2. CT name (TSA where is_class_teacher=true)
  3. Student count + list (EnrollmentsTable + StudentsTable)
  4. Today's attendance (AttendanceRecordsTable for today)
  5. Fee summary (FeesTable for students in this class)
  6. Upcoming tests (AssessmentsTable where class_id=X and date >= today)
  7. Today's timetable (TeacherPeriodsTable for this class+section+today's weekday)
     + PeriodExceptions for today
     + Status computation (done/now/upcoming based on current time)
  8. Weekly timetable (TeacherPeriodsTable for this class+section, all weekdays)
  9. Teacher list (TSA for this class+section, joined with AppUsersTable)
  ↓
Returns ClassDashboardDto
```

---

## 6. Implementation Phases

### Phase 0: School Day Configuration (Foundation — MUST BE FIRST)

**Goal:** Bell schedule, period structure, breaks, day-specific schedules. Everything else depends on this.

**Files to create/modify:**

| File | Action | Description |
|---|---|---|
| `docs/db/migration_107_school_day_config.sql` | CREATE | `school_day_config` + `school_day_slots` tables + seed default config |
| `server/.../db/Tables.kt` | EDIT | Add `SchoolDayConfigTable` and `SchoolDaySlotsTable` objects |
| `server/.../feature/school/SchoolDayConfigRouting.kt` | CREATE | CRUD for configs + slots, for-class endpoint |
| `shared/.../feature/admin/domain/model/SchoolDayConfigModels.kt` | CREATE | DTOs |
| `shared/.../feature/admin/data/remote/SchoolDayConfigApi.kt` | CREATE | API client |
| `shared/.../feature/admin/domain/repository/SchoolDayConfigRepository.kt` | CREATE | Repo interface |
| `shared/.../feature/admin/data/repository/SchoolDayConfigRepositoryImpl.kt` | CREATE | Repo impl |
| `shared/.../feature/admin/presentation/SchoolDayConfigViewModel.kt` | CREATE | VM |
| `composeApp/.../screens/school/SchoolDayConfigScreenV2.kt` | CREATE | Bell schedule editor UI |
| `composeApp/.../screens/school/SchoolSettingsScreenV2.kt` | EDIT | Add "School Day Structure" link |
| `website/src/components/admin/SchoolDayConfig.tsx` | CREATE | Web bell schedule editor |
| `website/src/lib/admin/hooks.ts` | EDIT | Add useSchoolDayConfig hook |
| `website/src/lib/admin/client.ts` | EDIT | Add API calls |
| `website/src/lib/admin/types.ts` | EDIT | Add types |

**Why first:** The timetable editor needs to know how many periods exist, their durations, and where breaks fall. Without this, the editor is just a blank grid. The school day config provides the skeleton that the timetable fills with subjects.

### Phase 1: Database + Server Foundation (Backend)

**Goal:** New tables, migration, draft CRUD, approval workflow, CT allocation, class dashboard, substitute allocation, teacher duties, room management.

**Files to create/modify:**

| File | Action | Description |
|---|---|---|
| `docs/db/migration_108_timetable_ecosystem.sql` | CREATE | `timetable_drafts` + `timetable_presets` + `teacher_duties` + `school_rooms` + `room_bookings` tables + seed presets |
| `server/.../db/Tables.kt` | EDIT | Add `TimetableDraftsTable`, `TimetablePresetsTable`, `TeacherDutiesTable`, `SchoolRoomsTable`, `RoomBookingsTable` objects |
| `server/.../feature/teacher/TeacherTimetableDraftRouting.kt` | CREATE | Draft CRUD, submit, PDF upload, presets, dropdowns |
| `server/.../feature/school/SchoolTimetableApprovalRouting.kt` | CREATE | Pending list, approve, reject, override-publish |
| `server/.../feature/school/ClassTeacherRouting.kt` | CREATE | List, assign, unassign, check-duplicate |
| `server/.../feature/school/ClassDashboardRouting.kt` | CREATE | Class overview + full dashboard endpoint |
| `server/.../feature/school/SubstituteTeacherRouting.kt` | CREATE | Available teachers + allocate substitute |
| `server/.../feature/teacher/TeacherClassDashboardRouting.kt` | CREATE | Teacher-scoped class dashboard |
| `server/.../feature/school/TeacherDutyRouting.kt` | CREATE | Duty CRUD, conflict check against teaching periods |
| `server/.../feature/school/RoomManagementRouting.kt` | CREATE | Room CRUD, room booking with conflict detection |
| `server/.../feature/teacher/TeacherCompleteDayRouting.kt` | CREATE | Merged daily view: teaching + duties + breaks + free + pending tasks |
| `server/.../feature/teacher/TeacherWorkloadRouting.kt` | CREATE | Workload analysis: teaching hours + duty hours + total |
| `server/.../feature/teacher/CtDashboardRouting.kt` | CREATE | CT responsibilities dashboard |
| `server/.../db/Tables.kt` | EDIT | Register new tables in auto-create list |
| `server/.../Application.kt` or routing setup | EDIT | Wire new routing modules |

**Server-side validation rules:**
- Draft create/update: validate periods_json — no teacher double-booking, no class overlap, end > start
- Draft submit: only DRAFT or REJECTED can be submitted → PENDING
- Draft approve: only PENDING can be approved → parse JSON → write TeacherPeriodsTable → PUBLISHED
- Draft reject: only PENDING can be rejected → REJECTED with note
- Draft delete: only DRAFT or REJECTED can be deleted
- CT assign: check duplicate, require force=true to override
- Substitute: check substitute has no conflicting period at that weekday+time
- Duty assign: check teacher has no teaching period or other duty at that time
- Room booking: check room is not already booked at that date+time
- School day config: validate slot times (end > start), no overlapping slots within same config
- Timetable draft: validate periods fit within school day config slots (teaching periods align with TEACHING slots)

**PDF parsing strategy:**
- Accept multipart file upload
- Use Apache PDFBox or Tika (JVM) to extract text
- Parse for time patterns (e.g., "09:00-09:45", "9:00 AM - 9:45 AM") and subject names
- Return parsed periods or error if unparseable
- Store PDF in Supabase Storage for reference

### Phase 2: Shared Layer (KMP Models + API + Repository)

**Goal:** Domain models, API interfaces, repository implementations for all new endpoints.

**Files to create/modify:**

| File | Action | Description |
|---|---|---|
| `shared/.../feature/teacher/domain/model/TimetableDraftModels.kt` | CREATE | All draft-related DTOs |
| `shared/.../feature/teacher/data/remote/TimetableDraftApi.kt` | CREATE | Ktor API client for draft endpoints |
| `shared/.../feature/teacher/domain/repository/TimetableDraftRepository.kt` | CREATE | Repository interface |
| `shared/.../feature/teacher/data/repository/TimetableDraftRepositoryImpl.kt` | CREATE | Repository impl |
| `shared/.../feature/admin/domain/model/ClassTeacherModels.kt` | CREATE | CT DTOs |
| `shared/.../feature/admin/data/remote/ClassTeacherApi.kt` | CREATE | CT API client |
| `shared/.../feature/admin/domain/repository/ClassTeacherRepository.kt` | CREATE | CT repo interface |
| `shared/.../feature/admin/data/repository/ClassTeacherRepositoryImpl.kt` | CREATE | CT repo impl |
| `shared/.../feature/admin/domain/model/ClassDashboardModels.kt` | CREATE | Dashboard DTOs |
| `shared/.../feature/admin/data/remote/ClassDashboardApi.kt` | CREATE | Dashboard API client |
| `shared/.../feature/admin/domain/repository/ClassDashboardRepository.kt` | CREATE | Dashboard repo interface |
| `shared/.../feature/admin/data/repository/ClassDashboardRepositoryImpl.kt` | CREATE | Dashboard repo impl |
| `shared/.../feature/admin/domain/model/SubstituteTeacherModels.kt` | CREATE | Substitute DTOs |
| `shared/.../feature/admin/data/remote/SubstituteTeacherApi.kt` | CREATE | Substitute API client |
| `shared/.../feature/admin/domain/model/TeacherDutyModels.kt` | CREATE | Duty DTOs |
| `shared/.../feature/admin/data/remote/TeacherDutyApi.kt` | CREATE | Duty API client |
| `shared/.../feature/admin/domain/repository/TeacherDutyRepository.kt` | CREATE | Duty repo interface |
| `shared/.../feature/admin/data/repository/TeacherDutyRepositoryImpl.kt` | CREATE | Duty repo impl |
| `shared/.../feature/admin/domain/model/RoomManagementModels.kt` | CREATE | Room + booking DTOs |
| `shared/.../feature/admin/data/remote/RoomManagementApi.kt` | CREATE | Room API client |
| `shared/.../feature/admin/domain/repository/RoomManagementRepository.kt` | CREATE | Room repo interface |
| `shared/.../feature/admin/data/repository/RoomManagementRepositoryImpl.kt` | CREATE | Room repo impl |
| `shared/.../feature/teacher/domain/model/TeacherCompleteDayModels.kt` | CREATE | Complete day DTOs |
| `shared/.../feature/teacher/data/remote/TeacherCompleteDayApi.kt` | CREATE | Complete day API client |
| `shared/.../feature/teacher/domain/repository/TeacherCompleteDayRepository.kt` | CREATE | Complete day repo interface |
| `shared/.../feature/teacher/data/repository/TeacherCompleteDayRepositoryImpl.kt` | CREATE | Complete day repo impl |
| `shared/.../feature/teacher/domain/model/CtDashboardModels.kt` | CREATE | CT dashboard DTOs |
| `shared/.../feature/teacher/data/remote/CtDashboardApi.kt` | CREATE | CT dashboard API client |
| `shared/.../feature/teacher/domain/repository/CtDashboardRepository.kt` | CREATE | CT dashboard repo interface |
| `shared/.../feature/teacher/data/repository/CtDashboardRepositoryImpl.kt` | CREATE | CT dashboard repo impl |
| `shared/.../di/Koin.kt` | EDIT | Register all new VMs, repos, APIs |

### Phase 3: App UI — Teacher Timetable Editor

**Goal:** Class teacher can create/edit/submit timetable from the app.

**Files to create/modify:**

| File | Action | Description |
|---|---|---|
| `composeApp/.../screens/teacher/TimetableEditorScreenV2.kt` | CREATE | Weekly grid editor with subject/teacher dropdowns |
| `composeApp/.../screens/teacher/TimetablePdfUploadSheet.kt` | CREATE | PDF upload bottom sheet |
| `composeApp/.../screens/teacher/TimetablePresetSheet.kt` | CREATE | Preset selection bottom sheet |
| `shared/.../feature/teacher/presentation/TimetableEditorViewModel.kt` | CREATE | VM for editor: load draft, save, submit, PDF upload, presets |
| `composeApp/.../screens/teacher/TeacherClassesScreenV2.kt` | EDIT | Add "Edit Timetable" button when isClassTeacher=true |
| `composeApp/.../screens/teacher/TeacherPortalV2.kt` | EDIT | Add TimetableEditor overlay |

**Editor UX:**
- Grid: 6 columns (Mon-Sat) × N rows (periods from school day config)
- Break slots shown as non-editable grey rows (from school day config)
- Each teaching cell: tap to edit → subject dropdown → teacher dropdown (filtered) → room dropdown (from school_rooms) → time (pre-filled from config)
- Double periods: visually merged cell spanning 2 slots
- Conflict cells highlighted red with reason
- Bottom bar: [Upload PDF] [Use Preset] [Add Period]
- Top bar: [Save Draft] [Submit for Approval]
- Draft auto-saves on changes (debounced)
- Break rows show label ("Short Break", "Lunch") from school day config
- Assembly/Zero period rows shown but not editable by CT

### Phase 3.5: App UI — Teacher Complete Day + CT Dashboard

**Goal:** Teacher sees their complete day (teaching + duties + breaks + free + pending tasks). CT sees responsibilities dashboard.

**Files to create/modify:**

| File | Action | Description |
|---|---|---|
| `shared/.../feature/teacher/presentation/TeacherCompleteDayViewModel.kt` | CREATE | VM for complete day view |
| `shared/.../feature/teacher/presentation/CtDashboardViewModel.kt` | CREATE | VM for CT responsibilities dashboard |
| `composeApp/.../screens/teacher/TeacherCompleteDayScreenV2.kt` | CREATE | Complete day timeline with teaching, duties, breaks, free periods |
| `composeApp/.../screens/teacher/CtDashboardScreenV2.kt` | CREATE | CT responsibilities dashboard |
| `composeApp/.../screens/teacher/TeacherHomeScreenV2.kt` | EDIT | Replace/enhance today's periods strip with complete day view |
| `composeApp/.../screens/teacher/TeacherClassesScreenV2.kt` | EDIT | Add "My Responsibilities" tab for CT |
| `composeApp/.../screens/teacher/TeacherPortalV2.kt` | EDIT | Add CT Dashboard overlay |

### Phase 4: App UI — Admin Approval + Class Dashboard + Duty Roster + Room Management

**Goal:** Admin can approve timetables, view class dashboard, allocate CTs and substitutes.

**Files to create/modify:**

| File | Action | Description |
|---|---|---|
| `composeApp/.../screens/school/TimetableApprovalScreenV2.kt` | CREATE | Pending approvals list + draft review grid |
| `composeApp/.../screens/school/ClassDashboardScreenV2.kt` | CREATE | Full class dashboard with KPIs, timetable, student/teacher lists |
| `composeApp/.../screens/school/ClassTeacherManagementScreenV2.kt` | CREATE | CT allocation list + assign/unassign |
| `composeApp/.../screens/school/SubstituteAllocationSheet.kt` | CREATE | Substitute teacher dropdown sheet |
| `composeApp/.../screens/school/DutyRosterScreenV2.kt` | CREATE | Weekly duty roster grid with assign/remove |
| `composeApp/.../screens/school/RoomManagementScreenV2.kt` | CREATE | Room list + create/edit + lab booking calendar |
| `shared/.../feature/admin/presentation/TimetableApprovalViewModel.kt` | CREATE | VM for approval queue |
| `shared/.../feature/admin/presentation/ClassDashboardViewModel.kt` | CREATE | VM for class dashboard |
| `shared/.../feature/admin/presentation/ClassTeacherViewModel.kt` | CREATE | VM for CT management |
| `shared/.../feature/admin/presentation/DutyRosterViewModel.kt` | CREATE | VM for duty roster |
| `shared/.../feature/admin/presentation/RoomManagementViewModel.kt` | CREATE | VM for room management |
| `composeApp/.../screens/school/SchoolPortalV2.kt` | EDIT | Add overlays for approval, class dashboard, CT management, duty roster, room management |
| `composeApp/.../screens/school/PeopleTab.kt` or equivalent | EDIT | Add "Class Teachers" section linking to CT management |
| `composeApp/.../screens/school/SchoolSettingsScreenV2.kt` | EDIT | Add "Rooms & Labs" link |

### Phase 5: Web — Admin Timetable Management + Class Dashboard

**Goal:** Extend web admin dashboard with timetable editing, approval queue, and class dashboard.

**Files to create/modify:**

| File | Action | Description |
|---|---|---|
| `website/src/components/admin/timetable/TimetableEditor.tsx` | CREATE | Web timetable editor (mirrors app) |
| `website/src/components/admin/timetable/TimetableApprovalQueue.tsx` | CREATE | Pending approvals panel |
| `website/src/components/admin/timetable/ClassTeacherManager.tsx` | CREATE | CT allocation panel |
| `website/src/components/admin/timetable/DutyRosterManager.tsx` | CREATE | Web duty roster grid |
| `website/src/components/admin/timetable/RoomManager.tsx` | CREATE | Web room management + lab booking calendar |
| `website/src/components/admin/ClassDashboard.tsx` | CREATE | Web class dashboard |
| `website/src/components/admin/SchoolDayConfig.tsx` | CREATE | Web bell schedule editor (from Phase 0) |
| `website/src/components/admin/calendar/SchoolCalendar.tsx` | EDIT | Add edit mode when admin clicks a class |
| `website/src/lib/admin/hooks.ts` | EDIT | Add useTimetableDrafts, usePendingApprovals, useClassTeachers, useClassDashboard, useSchoolDayConfig, useDutyRoster, useRooms hooks |
| `website/src/lib/admin/client.ts` | EDIT | Add API calls for new endpoints |
| `website/src/lib/admin/types.ts` | EDIT | Add new TypeScript types for all new DTOs |
| `website/src/components/admin/DashboardWorkspace.tsx` | EDIT | Add timetable approval tab, duty roster tab, rooms tab |

### Phase 6: Notifications + Parent Integration

**Goal:** Push notifications on approval, CT assignment, timetable updates. Parent query routing to CT.

**Files to create/modify:**

| File | Action | Description |
|---|---|---|
| `server/.../feature/school/SchoolTimetableApprovalRouting.kt` | EDIT | Send notifications on approve/reject |
| `server/.../feature/school/ClassTeacherRouting.kt` | EDIT | Send notification on CT assignment |
| `server/.../feature/user/ParentMessagesRouting.kt` | EDIT | Default route parent queries to CT (sort CT first, pre-select CT as recipient) |
| `server/.../feature/teacher/TeacherTimetableDraftRouting.kt` | EDIT | Notify teachers in timetable when published |
| `shared/.../feature/parent/presentation/ParentDashboardViewModel.kt` | EDIT | Refresh timetable on notification |

### Phase 7: Offline Support + Testing

**Goal:** Cache timetable reads, outbox for draft saves, comprehensive tests.

**Files to create/modify:**

| File | Action | Description |
|---|---|---|
| `shared/.../data/local/TimetableCacheDao.kt` | CREATE | Room DAO for timetable cache |
| `shared/.../data/local/AppDatabase.kt` | EDIT | Add TimetableCacheEntity, bump version |
| `shared/.../data/local/Migration_4_5.kt` | CREATE | Room migration for timetable cache |
| `shared/.../feature/teacher/data/repository/TimetableDraftRepositoryImpl.kt` | EDIT | Cache-then-network for reads, outbox for draft saves |
| `server/src/test/kotlin/.../TimetableDraftTest.kt` | CREATE | Server tests: CRUD, validation, approval flow |
| `server/src/test/kotlin/.../ClassTeacherTest.kt` | CREATE | Server tests: assign, duplicate check, unassign |
| `server/src/test/kotlin/.../ClassDashboardTest.kt` | CREATE | Server tests: dashboard assembly, data correctness |
| `server/src/test/kotlin/.../SubstituteTeacherTest.kt` | CREATE | Server tests: availability check, allocation, conflict |
| `shared/src/commonTest/kotlin/.../TimetableDraftRepositoryTest.kt` | CREATE | Repository tests |

---

## 7. Server Architecture Details

### 7.1 TimetableDraftRouting — Key Functions

```kotlin
// Pseudo-code for the core approval publish logic
suspend fun approveDraft(draftId: UUID, adminCtx: SchoolContext) = dbQuery {
    val draft = TimetableDraftsTable.selectAll()
        .where { TimetableDraftsTable.id eq draftId }
        .singleOrNull() ?: throw NotFoundException("Draft not found")

    require(draft[TimetableDraftsTable.status] == "PENDING") {
        "Only pending drafts can be approved"
    }

    val periods = Json.decodeFromString<List<DraftPeriodJson>>(draft[TimetableDraftsTable.periodsJson])

    // Validate all periods (final safety check)
    periods.forEach { p ->
        validateNoConflict(draft[TimetableDraftsTable.schoolId], p)
    }

    // Deactivate old periods for this class+section
    TeacherPeriodsTable.update({
        (TeacherPeriodsTable.schoolId eq draft[TimetableDraftsTable.schoolId]) and
        (TeacherPeriodsTable.className eq draft[TimetableDraftsTable.className]) and
        (TeacherPeriodsTable.section eq draft[TimetableDraftsTable.section]) and
        (TeacherPeriodsTable.isActive eq true)
    }) {
        it[isActive] = false
    }

    // Insert new periods
    periods.forEach { p ->
        TeacherPeriodsTable.insert {
            it[id] = UUID.randomUUID()
            it[schoolId] = draft[TimetableDraftsTable.schoolId]
            it[teacherId] = UUID.fromString(p.teacherId)
            it[weekday] = p.weekday
            it[startTime] = LocalTime.parse(p.startTime)
            it[endTime] = LocalTime.parse(p.endTime)
            it[className] = draft[TimetableDraftsTable.className]
            it[section] = draft[TimetableDraftsTable.section]
            it[subject] = p.subject
            it[room] = p.room
            it[assignmentId] = p.assignmentId?.let { UUID.fromString(it) }
            it[academicYearId] = draft[TimetableDraftsTable.academicYearId]
            it[isActive] = true
            it[createdAt] = Instant.now()
        }
    }

    // Update draft status
    TimetableDraftsTable.update({ TimetableDraftsTable.id eq draftId }) {
        it[status] = "PUBLISHED"
        it[reviewedBy] = adminCtx.userId
        it[reviewedAt] = Instant.now()
    }
}
```

### 7.2 Conflict Detection Logic

```kotlin
fun validateNoConflict(schoolId: UUID, period: DraftPeriodJson) {
    // Check teacher double-booking
    val teacherConflict = TeacherPeriodsTable.selectAll().where {
        (TeacherPeriodsTable.schoolId eq schoolId) and
        (TeacherPeriodsTable.teacherId eq UUID.fromString(period.teacherId)) and
        (TeacherPeriodsTable.weekday eq period.weekday) and
        (TeacherPeriodsTable.isActive eq true) and
        (TeacherPeriodsTable.startTime lessEq LocalTime.parse(period.endTime)) and
        (TeacherPeriodsTable.endTime greaterEq LocalTime.parse(period.startTime))
    }.toList()

    require(teacherConflict.isEmpty()) {
        "Teacher ${period.teacherName} is already teaching ${teacherConflict.first()[TeacherPeriodsTable.className]} at this time"
    }
}
```

### 7.3 Available Teachers Query

```kotlin
fun availableTeachers(schoolId: UUID, weekday: Int, startTime: LocalTime, endTime: LocalTime): List<AvailableTeacherDto> {
    // All active teachers in school
    val allTeachers = AppUsersTable.selectAll().where {
        (AppUsersTable.schoolId eq schoolId) and
        (AppUsersTable.role eq "teacher") and
        (AppUsersTable.isActive eq true)
    }.toList()

    // Teachers who have a period at this time
    val busyTeacherIds = TeacherPeriodsTable.selectAll().where {
        (TeacherPeriodsTable.schoolId eq schoolId) and
        (TeacherPeriodsTable.weekday eq weekday) and
        (TeacherPeriodsTable.isActive eq true) and
        (TeacherPeriodsTable.startTime lessEq endTime) and
        (TeacherPeriodsTable.endTime greaterEq startTime)
    }.map { it[TeacherPeriodsTable.teacherId] }.toSet()

    return allTeachers
        .filter { it[AppUsersTable.id].value !in busyTeacherIds }
        .map { row ->
            AvailableTeacherDto(
                id = row[AppUsersTable.id].value.toString(),
                name = row[AppUsersTable.fullName],
                subjects = getTeacherSubjects(schoolId, row[AppUsersTable.id].value),
                isFree = true,
            )
        }
}
```

### 7.4 Class Dashboard Assembly

```kotlin
fun classDashboard(schoolId: UUID, classId: UUID, section: String, today: LocalDate): ClassDashboardDto {
    val classInfo = SchoolClassesTable.selectAll().where { SchoolClassesTable.id eq classId }.single()
    val className = classInfo[SchoolClassesTable.name]

    // CT
    val ct = TeacherSubjectAssignmentsTable.selectAll().where {
        (TeacherSubjectAssignmentsTable.schoolId eq schoolId) and
        (TeacherSubjectAssignmentsTable.classId eq classId) and
        (TeacherSubjectAssignmentsTable.section eq section) and
        (TeacherSubjectAssignmentsTable.isClassTeacher eq true) and
        (TeacherSubjectAssignmentsTable.isActive eq true)
    }.singleOrNull()
    val ctName = ct?.let { AppUsersTable.selectAll().where { AppUsersTable.id eq it[TeacherSubjectAssignmentsTable.teacherId] }.single()[AppUsersTable.fullName] }

    // Students
    val enrollments = EnrollmentsTable.selectAll().where {
        (EnrollmentsTable.classId eq classId) and
        (EnrollmentsTable.section eq section) and
        (EnrollmentsTable.status eq "active")
    }.toList()
    val studentIds = enrollments.map { it[EnrollmentsTable.studentId] }

    // Today's attendance
    val todayAttendance = AttendanceRecordsTable.selectAll().where {
        (AttendanceRecordsTable.schoolId eq schoolId) and
        (AttendanceRecordsTable.date eq today) and
        // Filter to students in this class
    }.toList()
    val presentToday = todayAttendance.count { it[AttendanceRecordsTable.status] == "present" }
    val absentToday = todayAttendance.count { it[AttendanceRecordsTable.status] == "absent" }

    // Today's timetable with status
    val weekday = today.dayOfWeek.value
    val todayPeriods = TeacherPeriodsTable.selectAll().where {
        (TeacherPeriodsTable.schoolId eq schoolId) and
        (TeacherPeriodsTable.className eq className) and
        (TeacherPeriodsTable.section eq section) and
        (TeacherPeriodsTable.weekday eq weekday) and
        (TeacherPeriodsTable.isActive eq true)
    }.sortedBy { it[TeacherPeriodsTable.startTime] }.map { period ->
        val now = LocalTime.now()
        val start = period[TeacherPeriodsTable.startTime]
        val end = period[TeacherPeriodsTable.endTime]
        val status = when {
            now > end -> "done"
            now >= start && now < end -> "now"
            else -> "upcoming"
        }
        // Check for period exceptions (substitutions, cancellations)
        DashboardPeriodDto(
            startTime = start.format(HHMM),
            endTime = end.format(HHMM),
            subject = period[TeacherPeriodsTable.subject],
            teacherName = getTeacherName(period[TeacherPeriodsTable.teacherId]),
            teacherId = period[TeacherPeriodsTable.teacherId].toString(),
            room = period[TeacherPeriodsTable.room],
            status = status,
            attendanceMarked = checkAttendanceMarked(schoolId, classId, today, period),
        )
    }

    // Assemble full DTO...
    return ClassDashboardDto(...)
}
```

---

## 8. Security & Validation

| Concern | Mitigation |
|---|---|
| **Only CT can create/edit drafts** | Server checks `is_class_teacher=true` for the requesting teacher on that class+section |
| **Only admin can approve/reject** | Server checks `role == "school_admin"` in SchoolContext |
| **Teacher can only edit their own class's timetable** | Server validates `teacherId` matches CT of the class+section in the draft |
| **Draft cannot be modified after submission** | Server rejects POST to draft if status != DRAFT and != REJECTED |
| **PDF upload size limit** | 10MB max, server-side validation |
| **PDF content validation** | Server validates file type (application/pdf), not just extension |
| **Period time validation** | end_time > start_time, no overlap within same class+weekday |
| **Teacher double-booking** | Pre-save conflict check + DB unique index |
| **Substitute double-booking** | Server checks substitute has no period at that time before creating exception |
| **XSS in review_note** | Server sanitizes all text inputs |
| **SQL injection** | Exposed ORM parameterized queries (existing pattern) |
| **Authorization on class dashboard** | Admin: school-scoped. Teacher: must have active TSA for that class |

---

## 9. Testing Strategy

### 9.1 Server Tests

| Test | Description |
|---|---|
| `TimetableDraftCrudTest` | Create, read, update, delete draft — happy path |
| `TimetableDraftValidationTest` | Conflict detection, time validation, status transitions |
| `TimetableApprovalTest` | Approve → publishes to TeacherPeriodsTable, deactivates old, notifies |
| `TimetableApprovalRejectTest` | Reject with note, CT can re-edit |
| `ClassTeacherAssignTest` | Assign CT, duplicate warning, force override |
| `ClassTeacherUnassignTest` | Remove CT, is_class_teacher=false |
| `ClassDashboardTest` | Full dashboard assembly — attendance, fees, tests, timetable, roster |
| `SubstituteTeacherTest` | Available teachers query, allocate substitute, conflict check |
| `TimetablePdfUploadTest` | Upload valid PDF → parse. Upload invalid → error response |
| `PresetTest` | List presets, apply preset → pre-fill time slots |
| `SchoolDayConfigTest` | CRUD config + slots, validate no overlapping slots, for-class resolution |
| `TeacherDutyTest` | Assign duty, conflict check against teaching periods, one-off vs recurring |
| `RoomManagementTest` | Room CRUD, booking conflict detection, available rooms query |
| `TeacherCompleteDayTest` | Merged view: teaching + duties + breaks + free periods + pending tasks |
| `CtDashboardTest` | CT dashboard assembly: checklist, wellness alerts, parent queries, stats |
| `TeacherWorkloadTest` | Workload calculation: teaching hours + duty hours + total |

### 9.2 Shared/Repository Tests

| Test | Description |
|---|---|
| `TimetableDraftRepositoryTest` | API calls map to correct endpoints, NetworkResult handling |
| `ClassTeacherRepositoryTest` | Assign/unassign API calls, duplicate check |
| `ClassDashboardRepositoryTest` | Dashboard fetch, error handling |

### 9.3 Build Verification

```bash
# JVM compile + tests
./gradlew :shared:compileKotlinJvm :shared:jvmTest

# Android compile
./gradlew :shared:compileDevDebugKotlinAndroid :composeApp:compileDevDebugKotlinAndroid

# Server tests
./gradlew :server:test
```

---

## 10. SOLID + MVVM Compliance Checklist

| Principle | How This Feature Complies |
|---|---|
| **S**ingle Responsibility | Each routing file handles one domain (drafts, approvals, CT, dashboard, substitute) |
| **O**pen/Closed | New tables and endpoints are additive — existing code unchanged |
| **L**iskov Substitution | Repository impls match interfaces exactly |
| **I**nterface Segregation | Separate repository interfaces per feature (draft, CT, dashboard, substitute) |
| **D**ependency Inversion | VMs depend on repository interfaces, not impls. Koin wires impls. |
| **MVVM** | View (Composable) → ViewModel (StateFlow) → Repository → API → Server |
| **Clean Architecture** | Domain models in shared/commonMain, data layer in data/, presentation in presentation/ |

---

## 11. Notification Triggers

| Event | Recipient | Channel | Message |
|---|---|---|---|
| Draft submitted | Admin | Push + in-app | "Grade 7-A timetable submitted by {CT name}" |
| Draft approved | CT | Push + in-app | "Grade 7-A timetable approved" |
| Draft rejected | CT | Push + in-app | "Grade 7-A timetable rejected: {note}" |
| Timetable published | Parents of class | Push | "Grade 7-A timetable has been updated" |
| Timetable published | Teachers in timetable | Push + in-app | "Your schedule has been updated for Grade 7-A" |
| CT assigned | Teacher | Push + in-app | "You are now class teacher of {class}" |
| CT unassigned | Teacher | Push + in-app | "You are no longer class teacher of {class}" |
| Substitute allocated | Substitute teacher | Push + in-app | "You're covering {class} {subject} at {time} on {date}" |
| Duty assigned | Teacher | Push + in-app | "You have {duty_type} duty on {day} at {time}" |
| Duty conflict detected | Admin | In-app | "{teacher} has a duty conflict with teaching period at {time}" |
| Room booking confirmed | Teacher | In-app | "Room {room} booked for {date} at {time}" |
| Room booking conflict | Teacher | In-app | "Room {room} is already booked at that time" |
| School day config changed | All teachers | Push | "School day structure has been updated. Please review your timetable." |
| CT dashboard: wellness alert | CT | Push + in-app | "Wellness alert: {student} — {alert details}" |
| CT dashboard: parent query | CT | Push + in-app | "New parent query from {parent} regarding {student}" |
| CT dashboard: consecutive absence | CT | Push | "{student} has been absent for {N} consecutive days" |

---

## 12. Future Enhancements (Non-Breaking)

| Enhancement | Description |
|---|---|
| **AI timetable optimization** | Suggest optimal timetable based on teacher availability, subject priorities, room constraints, duty load balancing |
| **Recurring timetable generation** | Auto-generate next term's timetable from current term with one click |
| ~~**Room allocation**~~ | ✅ Now included in this plan (Phase 1+4+5) |
| ~~**Break/lunch periods**~~ | ✅ Now included in school day config (Phase 0) |
| **Timetable templates per board** | CBSE/ICSE/State board specific presets |
| ~~**Teacher workload analysis**~~ | ✅ Now included in teacher complete day + workload endpoint (Phase 1+3.5) |
| **Parent-teacher meeting slot** | Auto-block PTM slots in timetable |
| **Timetable version history** | Track all published versions with diff view |
| **Bulk class promotion** | Promote all students + copy timetable to next class at year end |
| **AI duty rotation** | Auto-rotate duties fairly among teachers based on workload |
| **Teacher availability marketplace** | Teachers mark preferred free slots for substitute assignments |
| **Cross-school substitute pool** | Share substitutes across nearby schools in same trust |

---

## 13. God-Mode Agent Prompt

> Paste the prompt below into an AI agent (Claude/GLM/GPT) to execute this plan end-to-end.
> The agent follows a strict loop: BUILD → TEST → REVIEW → FIX → COMMIT per phase.

---

```
You are the Timetable & Class Teacher System Executor. You are implementing a production-grade feature for a Kotlin Multiplatform school management app (VidyaPrayag/EnrollPlus) targeting a $100M Series A.

## YOUR MISSION
Implement the complete Timetable & Class Teacher System as defined in docs/TIMETABLE_CLASS_TEACHER_PLAN.md. You will work phase by phase, building server → shared → app → web, testing at each step.

## CODEBASE ARCHITECTURE (MEMORIZE THIS)
- **Server:** Ktor + Exposed ORM + PostgreSQL (Supabase). Routing files in `server/src/main/kotlin/com/littlebridge/enrollplus/feature/`. Tables in `server/.../db/Tables.kt`. Pattern: each feature has a `*Routing.kt` file with route handlers + DTOs.
- **Shared (KMP):** `shared/src/commonMain/kotlin/com/littlebridge/enrollplus/feature/`. Structure: `domain/model/` (DTOs), `data/remote/` (Ktor API), `domain/repository/` (interfaces), `data/repository/` (impls), `presentation/` (ViewModels). DI via Koin in `shared/.../di/Koin.kt`.
- **App (Compose):** `composeApp/src/commonMain/kotlin/com/littlebridge/enrollplus/ui/v2/screens/`. Portals: `school/` (admin), `teacher/`, `parent/`. Each portal has a `*PortalV2.kt` with tab + overlay routing. UI primitives: `VCard`, `VButton`, `VBadge`, `VAvatar`, `VStateHost`, `VTheme`, `VIcons` in `ui/v2/components/`.
- **Web (Next.js):** `website/src/components/admin/`. SWR hooks in `lib/admin/hooks.ts`. API client in `lib/admin/client.ts`. Types in `lib/admin/types.ts`.
- **DB Migrations:** `docs/db/migration_NNN_*.sql` (sequential numbering).
- **Build:** `./gradlew :shared:compileKotlinJvm :shared:jvmTest` (JVM), `./gradlew :shared:compileDevDebugKotlinAndroid :composeApp:compileDevDebugKotlinAndroid` (Android). Set `JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot`.

## EXISTING INFRASTRUCTURE (REUSE, DON'T DUPLICATE)
- `TeacherPeriodsTable` (Tables.kt:1194) — live timetable periods. Has: schoolId, teacherId, weekday, startTime, endTime, room, className, section, subject, assignmentId, academicYearId, validFrom, validTo, isActive.
- `PeriodExceptionsTable` (Tables.kt:1239) — one-off overrides. Has: periodId, date, kind (CANCELLED|RESCHEDULED|ROOM_CHANGE|SUBSTITUTION|EXTRA), substituteTeacherId.
- `TeacherSubjectAssignmentsTable` (Tables.kt:302) — teacher↔class↔subject mapping. Has: isClassTeacher boolean.
- `SchoolClassesTable` (Tables.kt:269), `SchoolSubjectsTable` (Tables.kt:278), `EnrollmentsTable` (Tables.kt:577).
- `SchoolTimetableRouting.kt` — GET /api/v1/school/timetable (admin read, all classes).
- `ParentAcademicsRouting.kt:356` — GET /api/v1/parent/timetable (parent read, child's class).
- `TeacherDayRouting.kt` — GET /api/v1/teacher/day + week (resolved day with exceptions).
- `TeacherClassesRouting.kt` — GET /api/v1/teacher/classes + /classes/{assignmentId} (class detail with weekly timetable).
- `TeacherAssignmentRouting.kt` — CRUD for TSA (admin manages teacher assignments).
- `ParentScheduleCard.kt` — 3-face swipe card (current → today → weekly) — already works for parents.
- `TeacherClassesScreenV2.kt` — class list → class detail → student profile — extend for timetable edit.
- `SchoolCalendar.tsx` + `WeekGrid.tsx` — admin web calendar — extend for edit mode.
- `StudentProfileScreenV2.kt` (admin) + `TeacherStudentProfileScreenV2.kt` (teacher) — reuse for student profiles from class dashboard.
- `ParentMessagesRouting.kt` — already flags `isClassTeacher` for recipient sorting. Extend to default-route queries to CT.
- `BEHAVIOR_TRACKING_SPEC.md` — behavior incidents system (spec ready, integrate with CT dashboard).
- `STUDENT_WELLNESS_SPEC.md` — wellness tracking system (spec ready, integrate with CT dashboard).
- `CalendarEventsTable` — academic calendar events. Reuse for staff meetings.
- `TeacherCardWorkloadDto` — basic workload counts. Extend with hours-based calculation.

## EXECUTION RULES
1. **One phase at a time.** Complete Phase 0 first (school day config), then Phase 1 (server), then Phase 2 (shared), etc.
2. **BUILD → TEST → FIX loop.** After each file change:
   - Run `./gradlew :shared:compileKotlinJvm` to verify compilation.
   - Run `./gradlew :shared:jvmTest` for shared tests.
   - Run `./gradlew :server:test` for server tests.
   - Fix ALL errors before moving to the next file.
3. **Follow existing patterns EXACTLY.** Look at how `PtmRouting.kt` or `TeacherAssignmentRouting.kt` is structured — mirror that for new routing files. Look at how `PtmModels.kt` or `TeacherModels.kt` structures DTOs — mirror that. Look at how `PtmRepositoryImpl.kt` works — mirror that.
4. **No comments unless asked.** No documentation files unless asked. No helper scripts.
5. **Minimal edits.** Prefer editing existing files over creating new ones when possible. Use `edit` and `multi_edit` tools.
6. **SerialName on all DTO fields.** Every DTO must use `@SerialName("snake_case")` for wire format consistency.
7. **Koin registration.** Every new ViewModel, Repository, API must be registered in `Koin.kt`.
8. **Migration numbering.** Check `docs/db/` for the highest migration number and use the next one. Phase 0 uses migration_107, Phase 1 uses migration_108.
9. **Test before claiming done.** Run the build commands. Paste the output. If it fails, fix it.

## PHASE EXECUTION ORDER

### Phase 0: School Day Configuration (MUST BE FIRST)
1. Create `docs/db/migration_107_school_day_config.sql` — school_day_config + school_day_slots tables + seed default 8-period config with assembly, breaks, lunch
2. Edit `server/.../db/Tables.kt` — add `SchoolDayConfigTable` and `SchoolDaySlotsTable` objects
3. Create `server/.../feature/school/SchoolDayConfigRouting.kt` — CRUD configs + slots, for-class endpoint
4. Create shared layer: models, API, repo interface, repo impl, VM
5. Create app UI: `SchoolDayConfigScreenV2.kt` — bell schedule editor with add/remove periods and breaks
6. Create web UI: `SchoolDayConfig.tsx` — web bell schedule editor
7. BUILD + TEST

### Phase 1: Database + Server Foundation
1. Create `docs/db/migration_108_timetable_ecosystem.sql` — timetable_drafts + timetable_presets + teacher_duties + school_rooms + room_bookings tables + seed 4 system presets
2. Edit `server/.../db/Tables.kt` — add `TimetableDraftsTable` and `TimetablePresetsTable` objects (follow existing UUIDTable pattern)
3. Create `server/.../feature/teacher/TeacherTimetableDraftRouting.kt`:
   - GET /api/v1/teacher/timetable/drafts (list this teacher's drafts)
   - GET /api/v1/teacher/timetable/drafts/{classId} (get draft for class+section)
   - POST /api/v1/teacher/timetable/drafts (create/update draft — validate CT status, conflict check)
   - POST /api/v1/teacher/timetable/drafts/{id}/submit (DRAFT→PENDING)
   - DELETE /api/v1/teacher/timetable/drafts/{id} (only DRAFT/REJECTED)
   - POST /api/v1/teacher/timetable/upload-pdf (multipart, parse, return periods or error)
   - GET /api/v1/teacher/timetable/presets (list presets)
   - GET /api/v1/teacher/timetable/teachers-for-class?class_id=X&section=Y (dropdown data)
   - GET /api/v1/teacher/timetable/subjects-for-class?class_id=X (dropdown data)
4. Create `server/.../feature/school/SchoolTimetableApprovalRouting.kt`:
   - GET /api/v1/school/timetable/pending (list PENDING drafts)
   - GET /api/v1/school/timetable/drafts/{id} (draft detail)
   - POST /api/v1/school/timetable/drafts/{id}/approve (publish to TeacherPeriodsTable)
   - POST /api/v1/school/timetable/drafts/{id}/reject (with note)
   - POST /api/v1/school/timetable/drafts/{id}/override-publish (admin bypass)
   - GET /api/v1/school/timetable/all (all classes with status)
5. Create `server/.../feature/school/ClassTeacherRouting.kt`:
   - GET /api/v1/school/class-teachers (list all classes with CT)
   - POST /api/v1/school/class-teachers/assign (with duplicate check + force)
   - POST /api/v1/school/class-teachers/unassign
   - GET /api/v1/school/class-teachers/check-duplicate?teacher_id=X
6. Create `server/.../feature/school/ClassDashboardRouting.kt`:
   - GET /api/v1/school/classes/overview (all classes summary)
   - GET /api/v1/school/classes/{classId}/dashboard (full dashboard)
7. Create `server/.../feature/school/SubstituteTeacherRouting.kt`:
   - GET /api/v1/school/teachers/available?weekday=X&start_time=HH:mm&end_time=HH:mm
   - POST /api/v1/school/timetable/allocate-substitute
8. Create `server/.../feature/teacher/TeacherClassDashboardRouting.kt`:
   - GET /api/v1/teacher/classes/{assignmentId}/dashboard (teacher-scoped)
9. Create `server/.../feature/school/TeacherDutyRouting.kt`:
   - GET /api/v1/school/duties (list all, filter by weekday/teacher/type)
   - POST /api/v1/school/duties (assign duty — conflict check against teaching periods)
   - PUT /api/v1/school/duties/{id} (update duty)
   - DELETE /api/v1/school/duties/{id} (remove duty)
   - GET /api/v1/teacher/duties (teacher's own duties)
10. Create `server/.../feature/school/RoomManagementRouting.kt`:
    - GET /api/v1/school/rooms (list all rooms)
    - POST /api/v1/school/rooms (create room)
    - PUT /api/v1/school/rooms/{id} (update room)
    - DELETE /api/v1/school/rooms/{id} (deactivate)
    - GET /api/v1/school/rooms/available?date=X&start_time=HH:mm&end_time=HH:mm (available rooms)
    - POST /api/v1/school/rooms/book (book room — conflict check)
    - DELETE /api/v1/school/rooms/bookings/{id} (cancel booking)
11. Create `server/.../feature/teacher/TeacherCompleteDayRouting.kt`:
    - GET /api/v1/teacher/complete-day (merged: teaching + duties + breaks + free + pending tasks)
    - GET /api/v1/teacher/workload (workload analysis: teaching hrs + duty hrs + total)
12. Create `server/.../feature/teacher/CtDashboardRouting.kt`:
    - GET /api/v1/teacher/class-teacher/dashboard (CT responsibilities dashboard)
13. Wire all new routing files into the server's route setup
14. Create server tests for each new routing file
15. BUILD + TEST: `./gradlew :server:compileKotlin :server:test`

### Phase 2: Shared Layer (KMP)
1. Create `shared/.../feature/teacher/domain/model/TimetableDraftModels.kt` — all draft DTOs (mirror server DTOs exactly, @SerialName)
2. Create `shared/.../feature/teacher/data/remote/TimetableDraftApi.kt` — Ktor client (follow PtmApi.kt pattern)
3. Create `shared/.../feature/teacher/domain/repository/TimetableDraftRepository.kt` — interface
4. Create `shared/.../feature/teacher/data/repository/TimetableDraftRepositoryImpl.kt` — impl (follow PtmRepositoryImpl.kt)
5. Create admin-side models, APIs, repos for: ClassTeacher, ClassDashboard, SubstituteTeacher, TeacherDuty, RoomManagement
6. Create teacher-side models, APIs, repos for: TeacherCompleteDay, CtDashboard
7. Edit `shared/.../di/Koin.kt` — register all new VMs, repos, APIs
8. BUILD: `./gradlew :shared:compileKotlinJvm :shared:jvmTest`

### Phase 3: App UI — Teacher Timetable Editor
1. Create `shared/.../feature/teacher/presentation/TimetableEditorViewModel.kt` — StateFlow-based VM
2. Create `composeApp/.../screens/teacher/TimetableEditorScreenV2.kt` — weekly grid editor
3. Create `composeApp/.../screens/teacher/TimetablePdfUploadSheet.kt` — PDF upload bottom sheet
4. Create `composeApp/.../screens/teacher/TimetablePresetSheet.kt` — preset selection
5. Edit `composeApp/.../screens/teacher/TeacherClassesScreenV2.kt` — add "Edit Timetable" button when isClassTeacher=true
6. Edit `composeApp/.../screens/teacher/TeacherPortalV2.kt` — add TimetableEditor overlay
7. BUILD: `./gradlew :composeApp:compileDevDebugKotlinAndroid`

### Phase 3.5: App UI — Teacher Complete Day + CT Dashboard
1. Create `shared/.../feature/teacher/presentation/TeacherCompleteDayViewModel.kt`
2. Create `shared/.../feature/teacher/presentation/CtDashboardViewModel.kt`
3. Create `composeApp/.../screens/teacher/TeacherCompleteDayScreenV2.kt` — merged daily timeline
4. Create `composeApp/.../screens/teacher/CtDashboardScreenV2.kt` — CT responsibilities dashboard
5. Edit `TeacherHomeScreenV2.kt` — enhance today's strip with complete day view
6. Edit `TeacherClassesScreenV2.kt` — add "My Responsibilities" tab for CT
7. Edit `TeacherPortalV2.kt` — add CT Dashboard overlay
8. BUILD: `./gradlew :composeApp:compileDevDebugKotlinAndroid`

### Phase 4: App UI — Admin Approval + Class Dashboard + Duty Roster + Room Management
1. Create VMs: TimetableApprovalViewModel, ClassDashboardViewModel, ClassTeacherViewModel, DutyRosterViewModel, RoomManagementViewModel
2. Create screens: TimetableApprovalScreenV2, ClassDashboardScreenV2, ClassTeacherManagementScreenV2, SubstituteAllocationSheet, DutyRosterScreenV2, RoomManagementScreenV2
3. Edit SchoolPortalV2.kt — add overlays for all new screens
4. Edit PeopleTab — add "Class Teachers" section
5. Edit SchoolSettingsScreenV2.kt — add "Rooms & Labs" link
6. BUILD: `./gradlew :composeApp:compileDevDebugKotlinAndroid`

### Phase 5: Web — Admin Timetable Management + Duty Roster + Room Management
1. Edit `website/src/lib/admin/types.ts` — add TypeScript types for all new DTOs
2. Edit `website/src/lib/admin/client.ts` — add API calls for all new endpoints
3. Edit `website/src/lib/admin/hooks.ts` — add SWR hooks (useSchoolDayConfig, useTimetableDrafts, usePendingApprovals, useClassTeachers, useClassDashboard, useDutyRoster, useRooms)
4. Create `website/src/components/admin/SchoolDayConfig.tsx` — bell schedule editor
5. Create `website/src/components/admin/timetable/TimetableEditor.tsx`
6. Create `website/src/components/admin/timetable/TimetableApprovalQueue.tsx`
7. Create `website/src/components/admin/timetable/ClassTeacherManager.tsx`
8. Create `website/src/components/admin/timetable/DutyRosterManager.tsx`
9. Create `website/src/components/admin/timetable/RoomManager.tsx`
10. Create `website/src/components/admin/ClassDashboard.tsx`
11. Edit `DashboardWorkspace.tsx` — wire all new components into tabs
12. Edit `SchoolCalendar.tsx` — add edit mode for admin

### Phase 6: Notifications + Parent Integration
1. Edit server routing files to send notifications (follow existing Notify pattern)
2. Edit `ParentMessagesRouting.kt` — default-route queries to CT
3. Edit parent VM to refresh timetable on notification

### Phase 7: Offline Support + Testing
1. Create Room DAO + entity for timetable cache
2. Bump AppDatabase version + create migration
3. Edit repository impls for cache-then-network
4. Create all server + shared tests
5. Final BUILD + TEST all modules

## ANTI-PATTERNS (DO NOT DO THESE)
- ❌ Do NOT create new UI components when VCard/VButton/VBadge/VStateHost exist
- ❌ Do NOT create new table columns on existing tables — use new tables or JSON
- ❌ Do NOT skip @SerialName on DTO fields
- ❌ Do NOT hardcode teacher names, class names, or school IDs
- ❌ Do NOT create duplicate read endpoints — extend existing ones
- ❌ Do NOT add comments or documentation to code files
- ❌ Do NOT skip Koin registration for new dependencies
- ❌ Do NOT skip server-side validation (conflict checks, status transitions, duty conflicts, room booking conflicts)
- ❌ Do NOT break existing endpoints — all changes are additive
- ❌ Do NOT create .md files other than the migration SQL
- ❌ Do NOT skip the school day config (Phase 0) — everything depends on it
- ❌ Do NOT create separate calendars for duties vs teaching — merge into one complete day view

## START
Begin with Phase 0, step 1. Read the existing `Tables.kt` to understand the exact pattern for defining tables. Read `TeacherAssignmentRouting.kt` to understand the routing pattern. Then create the migration SQL file for school day configuration.
```

---

## 14. Summary

This plan delivers a complete, production-grade **Timetable & Class Teacher Ecosystem** that goes beyond timetables to cover the full spectrum of a teacher's daily life in a school:

### Core Timetable System
- **Reuses 80%+ of existing infrastructure** — `TeacherPeriodsTable`, `PeriodExceptionsTable`, `TSA.isClassTeacher`, parent schedule card, teacher class detail, admin web calendar, student profiles
- **Ensures conflict-free operation** — pre-save validation, DB unique indexes, teacher availability checks
- **Provides a complete approval workflow** — DRAFT → PENDING → APPROVED → PUBLISHED with admin review
- **Supports PDF upload with graceful fallback** — parse → pre-fill → manual edit if parse fails
- **Minimizes teacher effort** — presets, dropdowns filtered by TSA, auto-fill from PDF

### School Day Configuration (NEW)
- **Fully customizable bell schedule** — period count, duration (variable per period), breaks, lunch, assembly, zero period
- **Day-specific schedules** — Saturday half-day, Wednesday lab-day, exam-day schedules
- **Class-level overrides** — Primary vs Secondary different period structures
- **Double periods** — for labs and practicals

### Teacher Duty Roster (NEW)
- **8 duty types** — corridor, playground, assembly, invigilation, bus, library, event, recess
- **Conflict-checked** — duties can't overlap with teaching periods
- **Recurring + one-off** — weekly recurring duties + special event duties
- **Visible in teacher's daily view** — duties appear alongside teaching periods

### Room & Lab Management (NEW)
- **Rooms as first-class entities** — no more free-text room names
- **Room types** — classroom, lab, computer lab, library, music room, sports, assembly hall
- **Lab booking with conflict detection** — no double-booking labs
- **Equipment tracking** — projector, whiteboard, AC per room

### Teacher Ecosystem (NEW)
- **Complete daily view** — teaching + duties + breaks + free periods + pending tasks in one timeline
- **Free period tracking** — planning/correction time visible and calculated
- **Workload analysis** — teaching hours + duty hours + total workload per teacher
- **CT responsibilities dashboard** — attendance verification, behavior monitoring, wellness alerts, parent queries, homework review, PTM coordination, report cards, class statistics — all in one screen
- **Integration with existing systems** — behavior tracking, wellness, attendance, fees, homework, PTM, messaging

### Cross-Cutting
- **7 new tables** — `school_day_config`, `school_day_slots`, `timetable_drafts`, `timetable_presets`, `teacher_duties`, `school_rooms`, `room_bookings`
- **Covers all 3 portals** — admin (web + app), teacher (app), parent (app) with role-appropriate views
- **Routes parent queries to class teachers** — extending existing messaging infrastructure
- **Follows all architectural standards** — SOLID, MVVM, Clean Architecture, Koin DI, offline-first
- **8 implementation phases** — Phase 0 (school day config) through Phase 7 (offline + testing)
