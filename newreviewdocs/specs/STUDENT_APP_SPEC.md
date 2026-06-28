# Standalone Student App — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** `VIDYASETU_AI_TUTOR_SPEC.md`, `STUDENT_PORTFOLIO_SPEC.md`, `STUDENT_WELLNESS_SPEC.md`
> **Source:** `COMPETITIVE_GAP_ANALYSIS.md` — mobile-first trend
> **Template:** `_SPEC_TEMPLATE.md` v1 (25 mandatory + 6 optional sections)

---

## 1. Feature Overview

### What

A dedicated student-facing app (separate from parent app) that gives students direct access to their schedule, homework, AI tutor, portfolio, wellness check-ins, and class community. Age-appropriate UI with restricted permissions.

### Why — Product Rationale

Students currently rely on parents to view their schedule, homework, marks, and other school information. A dedicated student app empowers students to take ownership of their learning while maintaining parental oversight. This is a **high-priority feature** (Phase 4, effort XL) that aligns with the mobile-first, student-centric trend in education technology.

### What Stands Out (Competitive Moat)

From `COMPETITIVE_GAP_ANALYSIS.md`: mobile-first, student-centric trend.

The key moat is **age-appropriate, restricted-access design** — students get a safe, controlled environment with AI tutor, portfolio, wellness check-ins, and community feed, while parents retain oversight through parental controls. The separate app approach (different Play Store listing) signals a dedicated student experience, not just a parent app with a student mode.

### Goals

- Student login with school-provided credentials or parent-linked OTP
- Student sees: timetable, homework, assignments, marks, attendance, AI tutor, portfolio
- Wellness check-in (mood, survey) — from `STUDENT_WELLNESS_SPEC.md`
- AI Tutor access — from `VIDYASETU_AI_TUTOR_SPEC.md`
- Portfolio management — from `STUDENT_PORTFOLIO_SPEC.md`
- Class community feed (view + post with moderation)
- Restricted: no fee data, no admin functions, no other students' private data
- Parental controls: parent can see what child accesses, set screen time limits

### Non-goals

- [ ] Fee payment or fee visibility (parent-only feature)
- [ ] Admin functions (admin-only, not student-accessible)
- [ ] Other students' private data (marks, attendance, wellness)
- [ ] Direct messaging between students (safety concern)
- [ ] Student-to-teacher chat (use existing communication channels)
- [ ] Customizable app themes (future enhancement)
- [ ] Offline-first architecture (online-first, with limited offline caching)
- [ ] Student app for iOS (Android-first, iOS future)

### Dependencies

- `VIDYASETU_AI_TUTOR_SPEC.md` — AI tutor integration
- `STUDENT_PORTFOLIO_SPEC.md` — portfolio management
- `STUDENT_WELLNESS_SPEC.md` — wellness check-ins
- `AppUsersTable` — user accounts (add `student` role)
- `StudentsTable` — student info
- `HomeworkSubmissionsTable` — homework submission
- `MarksTable` — marks/grades
- `AttendanceTable` — attendance records
- `TimetableTable` — timetable
- `NotificationsTable` — notification infrastructure
- `DARK_MODE_SPEC.md` — dark mode support
- `TABLET_LAYOUT_SPEC.md` — tablet layout

### Related Modules

- `server/.../feature/auth/` — authentication (student auth flow)
- `server/.../feature/student/` — new student module
- `server/.../feature/homework/` — existing homework module
- `server/.../feature/wellness/` — existing wellness module
- `server/.../feature/tutor/` — existing AI tutor module
- `server/.../feature/portfolio/` — existing portfolio module
- `studentApp/` — new KMP app module
- `shared/` — shared KMP module (reused)

---

## 2. Current System Assessment

### Existing Code

- Current app is parent-focused (parents act on behalf of students)
- `AppUsersTable` has `role` field — can add `student` role
- All infrastructure (KMP, Compose Multiplatform, Ktor) is reusable
- `COMPETITIVE_GAP_ANALYSIS.md`: mobile-first, student-centric trend

### Existing Database

- `AppUsersTable` — user accounts with role field (parent, teacher, admin, counselor)
- `StudentsTable` — student info
- `HomeworkSubmissionsTable` — homework submissions
- `MarksTable` — marks/grades
- `AttendanceTable` — attendance records
- `TimetableTable` — timetable
- `NotificationsTable` — notification infrastructure
- No student app sessions table
- No `student` role in `AppUsersTable`
- No parental controls table

### Existing APIs

- Auth API (existing) — parent, teacher, admin login
- Homework API (existing) — homework management
- Marks API (existing) — marks/grades
- Attendance API (existing) — attendance
- AI Tutor API (existing) — AI tutor
- Portfolio API (existing) — portfolio
- Wellness API (existing) — wellness check-ins
- No student-specific API endpoints
- No parental controls API

### Existing UI

- Parent app (existing) — parent-focused
- Teacher dashboard (existing) — teacher-focused
- Admin dashboard (existing) — admin-focused
- No student app or student UI
- Dark mode support (existing, from `DARK_MODE_SPEC.md`)
- Tablet layout (existing, from `TABLET_LAYOUT_SPEC.md`)

### Existing Services

- `AuthService` — authentication
- `HomeworkService` — homework management
- `WellnessService` — wellness check-ins
- `TutorService` — AI tutor
- `PortfolioService` — portfolio management
- `NotificationService` — notification dispatch
- No student-specific service
- No parental controls service

### Existing Documentation

- `COMPETITIVE_GAP_ANALYSIS.md` — mobile-first, student-centric trend
- `VIDYASETU_AI_TUTOR_SPEC.md` — AI tutor
- `STUDENT_PORTFOLIO_SPEC.md` — portfolio
- `STUDENT_WELLNESS_SPEC.md` — wellness
- `DARK_MODE_SPEC.md` — dark mode
- `TABLET_LAYOUT_SPEC.md` — tablet layout

### Technical Debt

| # | Gap | Details |
|---|---|---|
| TD-1 | No student role | `AppUsersTable` has no `student` role |
| TD-2 | No student auth flow | No student login (credentials or OTP) |
| TD-3 | No student API endpoints | No `/api/v1/student/*` routes |
| TD-4 | No student app | No separate student app module |
| TD-5 | No parental controls | No screen time limits or activity tracking |
| TD-6 | No student app sessions | No `student_app_sessions` table |

### Gaps

| # | Gap | Impact | Severity |
|---|---|---|---|
| G1 | No student login | Students can't access app directly | **High** |
| G2 | No student dashboard | Students rely on parents for info | **High** |
| G3 | No parental controls | Parents can't monitor/limit student app usage | **Medium** |
| G4 | No community feed | Students can't interact with class community | **Medium** |
| G5 | No student-specific UI | Current UI is parent-focused, not age-appropriate | **Medium** |

---

## 3. Functional Requirements

### FR-001
| Field | Value |
|---|---|
| **Title** | Student Login |
| **Description** | Student login: school-provided credentials (student ID + password) or parent-linked OTP. |
| **Priority** | High |
| **User Roles** | Student |
| **Acceptance notes** | Two auth options: (A) Admin creates student account with student ID + temp password, student logs in. (B) Parent generates invite, student enters phone → OTP → linked to parent's child record. JWT issued with role=student, linked_student_id. |

### FR-002
| Field | Value |
|---|---|
| **Title** | Student Dashboard |
| **Description** | Student dashboard: timetable, today's homework, upcoming tests, attendance summary. |
| **Priority** | High |
| **User Roles** | Student |
| **Acceptance notes** | Dashboard shows: today's timetable, pending homework, upcoming tests (next 7 days), attendance summary (present/absent/late for current term). Quick links to AI tutor, portfolio, wellness. |

### FR-003
| Field | Value |
|---|---|
| **Title** | Homework View and Submit |
| **Description** | Homework: view assigned homework, submit online assignments. |
| **Priority** | High |
| **User Roles** | Student |
| **Acceptance notes** | Student sees homework assigned to their class. Can view details, submit online assignments (file upload or text). Submission status tracked. Uses existing homework infrastructure. |

### FR-004
| Field | Value |
|---|---|
| **Title** | Marks View |
| **Description** | Marks: view own marks and assessment results. |
| **Priority** | High |
| **User Roles** | Student |
| **Acceptance notes** | Student sees own marks only. No access to other students' marks. Shows: recent marks, term-wise breakdown, subject-wise. Read-only. |

### FR-005
| Field | Value |
|---|---|
| **Title** | AI Tutor Access |
| **Description** | AI Tutor: full access to `VIDYASETU_AI_TUTOR_SPEC.md`. |
| **Priority** | High |
| **User Roles** | Student |
| **Acceptance notes** | Student has full access to AI tutor: ask questions, get explanations, practice problems. Delegates to existing AI tutor endpoints. Student-scoped (linked_student_id from JWT). |

### FR-006
| Field | Value |
|---|---|
| **Title** | Portfolio Management |
| **Description** | Portfolio: manage own portfolio (`STUDENT_PORTFOLIO_SPEC.md`). |
| **Priority** | Medium |
| **User Roles** | Student |
| **Acceptance notes** | Student can upload, edit, view own portfolio. Delegates to existing portfolio endpoints. Student-scoped. Achievement items auto-populated. |

### FR-007
| Field | Value |
|---|---|
| **Title** | Wellness Check-in |
| **Description** | Wellness: daily mood check-in, weekly survey (`STUDENT_WELLNESS_SPEC.md`). |
| **Priority** | Medium |
| **User Roles** | Student |
| **Acceptance notes** | Daily mood check-in (emoji-based: happy, okay, sad, angry). Weekly wellness survey. Delegates to existing wellness endpoints. Student-scoped. |

### FR-008
| Field | Value |
|---|---|
| **Title** | Community Feed |
| **Description** | Community feed: view and post (with teacher moderation). |
| **Priority** | Medium |
| **User Roles** | Student |
| **Acceptance notes** | Class community feed: students can view posts and create posts (text, image). All posts moderated by teacher (approved/rejected before visible). No direct messaging. School-appropriate content only. |

### FR-009
| Field | Value |
|---|---|
| **Title** | Restricted Access |
| **Description** | Restricted: no fee data, no admin functions, no other students' private data. |
| **Priority** | High |
| **User Roles** | Student |
| **Acceptance notes** | Student JWT has role=student. Middleware blocks access to fee endpoints, admin endpoints, and other students' data. All queries scoped to linked_student_id. |

### FR-010
| Field | Value |
|---|---|
| **Title** | Parental Controls |
| **Description** | Parental controls: parent can view child's app activity, set usage limits. |
| **Priority** | Medium |
| **User Roles** | Parent |
| **Acceptance notes** | Parent sees: child's app activity (features used, time spent), screen time limit setting. Parent can set daily screen time limit (minutes). Student app checks limit and shows warning when exceeded. |

### FR-011
| Field | Value |
|---|---|
| **Title** | Notifications |
| **Description** | Notifications: homework assigned, test scheduled, marks published, AI tutor reminders. |
| **Priority** | Medium |
| **User Roles** | Student |
| **Acceptance notes** | Student receives FCM notifications: homework assigned, test scheduled, marks published, AI tutor reminders. Uses existing notification infrastructure. Student-specific notification preferences. |

### Non-Functional Requirements

| ID | Requirement |
|---|---|
| NFR-1 | Login: < 2 seconds (credentials or OTP) |
| NFR-2 | Dashboard load: < 2 seconds |
| NFR-3 | Homework submit: < 3 seconds (file upload) |
| NFR-4 | Community feed: < 2 seconds to load |
| NFR-5 | Screen time tracking: real-time (updated every 60 seconds) |
| NFR-6 | Age-appropriate UI: larger touch targets (min 48dp), simpler navigation |
| NFR-7 | Parental controls: activity sync every 5 minutes |
| NFR-8 | App size: < 20MB (shared module reuse) |

---

## 4. User Stories

### Student
- [ ] Log in with my school credentials
- [ ] Log in with parent-linked OTP
- [ ] View my dashboard (timetable, homework, tests, attendance)
- [ ] View and submit homework
- [ ] View my marks
- [ ] Access AI tutor
- [ ] Manage my portfolio
- [ ] Do daily wellness check-in
- [ ] View and post in class community feed
- [ ] Receive notifications for homework, tests, marks

### Parent
- [ ] View my child's app activity
- [ ] Set daily screen time limit for my child
- [ ] Generate student app invite for my child

### School Admin
- [ ] Create student app accounts (student ID + temp password)
- [ ] View student app adoption metrics

### Teacher
- [ ] Moderate class community feed (approve/reject posts)

---

## 5. Business Rules

### BR-001
**Rule:** Student login requires either school-provided credentials or parent-linked OTP.
**Enforcement:** Two auth flows: (A) credentials (student ID + password, created by admin), (B) OTP (parent generates invite, student enters phone → OTP → linked). JWT issued with role=student.

### BR-002
**Rule:** Student can only see own data.
**Enforcement:** All queries scoped to `linked_student_id` from JWT. No access to other students' marks, attendance, wellness, or private data. Middleware enforces scoping.

### BR-003
**Rule:** Student has no access to fee data or admin functions.
**Enforcement:** Student JWT role=student. Middleware blocks `/api/v1/fee/*`, `/api/v1/admin/*`, `/api/v1/school/*` endpoints. Returns 403.

### BR-004
**Rule:** Community feed posts require teacher moderation.
**Enforcement:** Student creates post → post status=pending → teacher reviews → approved (visible) or rejected (hidden). No posts visible without teacher approval.

### BR-005
**Rule:** Parent can set daily screen time limit.
**Enforcement:** Parent sets `daily_limit_minutes` via parental controls. Student app tracks screen time (cumulative per day). When limit exceeded, app shows warning and optionally blocks further usage (configurable: warn-only or block).

### BR-006
**Rule:** Student app is a separate app from parent app.
**Enforcement:** Separate KMP app module (`studentApp/`), different app icon, name ("Vidya Prayag Student"), separate Play Store listing. Same backend, different JWT role.

### BR-007
**Rule:** Student accounts are linked to parent accounts (for parental controls).
**Enforcement:** `app_users.parent_linked_id` links student account to parent account. Parent can view child's activity. If no parent link (Option A credentials), parental controls optional.

### BR-008
**Rule:** AI Tutor, Portfolio, and Wellness delegate to existing endpoints.
**Enforcement:** Student app calls existing endpoints with student JWT. Existing services handle scoping based on `linked_student_id`. No duplication of logic.

### BR-009
**Rule:** Notifications are student-specific.
**Enforcement:** Student receives notifications relevant to them: homework, tests, marks, AI tutor reminders. No parent notifications (fee, admin). Uses existing notification infrastructure with student-specific preferences.

### BR-010
**Rule:** Screen time is tracked per day per student.
**Enforcement:** `student_app_sessions` table tracks cumulative screen time per day. `UNIQUE(student_id, date)` ensures one record per day. Screen time updated every 60 seconds while app is active.

---

## 6. Database Design

### 6.1 Entity Relationship Summary

Modify existing `app_users` table (add `linked_student_id` and `parent_linked_id` columns). New `student_app_sessions` table for screen time tracking. References existing `students`, `app_users` tables.

### 6.2 New Tables

#### `student_app_sessions` table

```sql
CREATE TABLE student_app_sessions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id      UUID NOT NULL,
    device_id       TEXT,
    app_version     VARCHAR(16),
    last_active_at  TIMESTAMP NOT NULL DEFAULT now(),
    screen_time_seconds INTEGER NOT NULL DEFAULT 0,  -- daily cumulative
    date            DATE NOT NULL DEFAULT CURRENT_DATE,
    UNIQUE(student_id, date)
);
```

### 6.3 Modified Tables

#### `app_users` table (modified)

```sql
-- Add 'student' to role enum/varchar
-- Student accounts linked to students table
ALTER TABLE app_users ADD COLUMN linked_student_id UUID;  -- FK students.id
ALTER TABLE app_users ADD COLUMN parent_linked_id UUID;   -- FK app_users.id (parent who authorized)
```

### 6.4 Indexes

- `student_app_sessions(student_id, date)` — UNIQUE constraint, ensures one record per day
- `app_users(linked_student_id)` — for student account lookup (recommended)
- `app_users(parent_linked_id)` — for parent-child relationship lookup (recommended)

### 6.5 Constraints

- `student_app_sessions.student_id` — NOT NULL
- `student_app_sessions.screen_time_seconds` — NOT NULL, default 0
- `student_app_sessions.date` — NOT NULL, default CURRENT_DATE
- `student_app_sessions.UNIQUE(student_id, date)` — one record per student per day
- `app_users.linked_student_id` — nullable (only for student role)
- `app_users.parent_linked_id` — nullable (only for parent-linked OTP auth)

### 6.6 Foreign Keys

- `student_app_sessions.student_id` → `students.id` (implicit)
- `app_users.linked_student_id` → `students.id` (implicit, nullable)
- `app_users.parent_linked_id` → `app_users.id` (implicit, nullable, self-reference)

### 6.7 Soft Delete Strategy

N/A — student app sessions are not deleted. They are historical records for screen time tracking. Student accounts can be deactivated (set inactive) but not deleted.

### 6.8 Audit Fields

- `student_app_sessions.last_active_at` — when student was last active
- `student_app_sessions.screen_time_seconds` — cumulative screen time for the day
- `student_app_sessions.date` — which day the session tracks
- `student_app_sessions.device_id` — which device the student used
- `student_app_sessions.app_version` — app version used

### 6.9 Migration Notes

Migration: `docs/db/migration_090_student_app.sql`
- ALTER `app_users`: ADD `linked_student_id`, `parent_linked_id` columns
- CREATE `student_app_sessions` table with UNIQUE constraint
- No data migration (new feature)

### 6.10 Exposed Mappings

```kotlin
// Modified: AppUsersTable
object AppUsersTable : UUIDTable("app_users", "id") {
    // ... existing columns ...
    val linkedStudentId = uuid("linked_student_id").nullable()  // for student role
    val parentLinkedId  = uuid("parent_linked_id").nullable()   // parent who authorized
}

// New: StudentAppSessionsTable
object StudentAppSessionsTable : UUIDTable("student_app_sessions", "id") {
    val studentId         = uuid("student_id")
    val deviceId          = text("device_id").nullable()
    val appVersion        = varchar("app_version", 16).nullable()
    val lastActiveAt      = timestamp("last_active_at")
    val screenTimeSeconds = integer("screen_time_seconds").default(0)
    val date              = date("date")

    init {
        uniqueIndex("uq_student_date", studentId, date)
    }
}
```

Register `StudentAppSessionsTable` in `DatabaseFactory.allTables`.

### 6.11 Seed Data

N/A — student accounts and sessions are user-created. No seed data needed.

---

## 7. State Machines

### Student Auth State Machine

```
not_authenticated ──credentials_login──> authenticated ──logout──> not_authenticated
  │                                       │
  │──otp_sent──> otp_pending ──otp_verified──> authenticated
  │                  │
  │                  └──otp_expired──> not_authenticated
  │
  └──authenticated ──session_expired──> not_authenticated
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `not_authenticated` | Student enters credentials | `authenticated` | Valid student ID + password |
| `not_authenticated` | Parent generates invite, student enters phone | `otp_pending` | OTP sent to phone |
| `otp_pending` | Student enters correct OTP | `authenticated` | OTP verified, linked to parent's child |
| `otp_pending` | OTP expires (5 min) | `not_authenticated` | OTP expired, student must restart |
| `authenticated` | Student logs out | `not_authenticated` | JWT invalidated |
| `authenticated` | JWT expires | `not_authenticated` | Session expired, re-login required |

### Screen Time State Machine

```
under_limit ──app_active──> tracking ──limit_reached──> limit_exceeded
  │              │                              │
  │              └──app_background──> under_limit │
  │                                              │
  └──limit_exceeded ──new_day──> under_limit     │
  │                                              │
  └──limit_exceeded ──parent_increases_limit──> under_limit
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `under_limit` | App becomes active | `tracking` | Screen time tracking starts |
| `tracking` | App goes to background | `under_limit` | Screen time paused |
| `tracking` | Cumulative time reaches limit | `limit_exceeded` | Screen time >= daily_limit_minutes |
| `limit_exceeded` | New day (midnight) | `under_limit` | Screen time resets to 0 |
| `limit_exceeded` | Parent increases limit | `under_limit` | New limit > current screen time |

### Community Feed Post State Machine

```
draft ──student_submits──> pending ──teacher_approves──> approved ──(visible)
  │                          │
  │                          └──teacher_rejects──> rejected ──(hidden)
  │
  └──approved ──student_deletes──> deleted (terminal)
  │
  └──approved ──teacher_removes──> removed (terminal)
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `draft` | Student submits post | `pending` | Post saved with status=pending |
| `pending` | Teacher approves | `approved` | Post visible to class |
| `pending` | Teacher rejects | `rejected` | Post hidden, student notified |
| `approved` | Student deletes own post | `deleted` | Post removed |
| `approved` | Teacher removes | `removed` | Post removed by teacher |

---

## 8. Backend Architecture

### 8.1 Component Overview

`StudentAuthService` handles student login (credentials and OTP). `StudentRouting` defines student-specific API endpoints. `ParentalControlsService` handles screen time tracking and limits. Existing services (HomeworkService, WellnessService, TutorService, PortfolioService) are reused with student JWT scoping. `CommunityFeedService` handles class community feed with moderation.

### 8.2 Design Principles

1. **Student-safe** — restricted access, no fee/admin/other students' data
2. **Age-appropriate** — larger touch targets, simpler navigation, gamified elements
3. **Parental oversight** — parent can monitor and limit student app usage
4. **Reuse existing** — delegate to existing services (homework, wellness, tutor, portfolio)
5. **Separate app** — dedicated student app, not parent app with student mode
6. **School-scoped** — all data school-specific

### 8.3 Core Types

#### StudentAuthService

```kotlin
class StudentAuthService {
    suspend fun loginWithCredentials(studentId: String, password: String): AuthResult
    suspend fun loginWithOtp(phone: String, otp: String): AuthResult
    suspend fun sendOtp(phone: String): Unit
    suspend fun logout(token: String): Unit
}
```

#### ParentalControlsService

```kotlin
class ParentalControlsService {
    suspend fun getAppActivity(parentId: UUID, childId: UUID): AppActivityDto
    suspend fun setScreenTimeLimit(parentId: UUID, childId: UUID, dailyLimitMinutes: Int): Unit
    suspend fun checkScreenTime(studentId: UUID): Boolean  // returns false if limit exceeded
    suspend fun updateScreenTime(studentId: UUID, additionalSeconds: Int): Unit
}
```

#### CommunityFeedService

```kotlin
class CommunityFeedService {
    suspend fun createPost(studentId: UUID, classId: UUID, content: String, imageUrl: String?): PostDto
    suspend fun getFeed(classId: UUID): List<PostDto>
    suspend fun moderatePost(postId: UUID, action: String, teacherId: UUID): PostDto  // approve | reject
    suspend fun deletePost(postId: UUID, studentId: UUID): Unit
}
```

### 8.4 Repositories

- `StudentAppSessionRepository` — CRUD for `student_app_sessions`
- `CommunityFeedRepository` — CRUD for community feed posts (new table or reuse existing)

### 8.5 Mappers

- `StudentAppSessionMapper` — maps `student_app_sessions` rows to DTOs
- `PostMapper` — maps community feed posts to DTOs

### 8.6 Permission Checks

- All student endpoints: student role + JWT auth + scoped to `linked_student_id`
- Parental controls: parent role + JWT auth + parent-child relationship verified
- Community feed moderation: teacher role + JWT auth + teacher-class relationship
- No access to: fee endpoints, admin endpoints, other students' data

### 8.7 Background Jobs

- **Screen Time Reset Job** — daily at midnight
  1. New day starts → screen time resets to 0 for all students
  2. New `student_app_sessions` record created for each active student
  3. Return count of sessions reset

- **Activity Sync Job** — every 5 minutes
  1. Sync student app activity to parent (if parent_linked_id exists)
  2. Update `last_active_at` and `screen_time_seconds`
  3. Return count of sessions synced

### 8.8 Domain Events

- `StudentLoggedIn` — emitted when student logs in
- `StudentLoggedOut` — emitted when student logs out
- `ScreenTimeLimitReached` — emitted when student reaches daily screen time limit
- `CommunityPostCreated` — emitted when student creates a post
- `CommunityPostApproved` — emitted when teacher approves a post
- `CommunityPostRejected` — emitted when teacher rejects a post
- `ScreenTimeLimitSet` — emitted when parent sets/updates screen time limit

### 8.9 Caching

- Student dashboard: cached locally (2-minute TTL)
- Timetable: cached locally (10-minute TTL, rarely changes)
- Homework list: cached locally (2-minute TTL)
- Community feed: cached locally (1-minute TTL)
- Screen time: not cached (real-time tracking)

### 8.10 Transactions

- Student login: single transaction (verify credentials, issue JWT, create/update session)
- Community post: single transaction (insert post with status=pending)
- Post moderation: single transaction (update post status)
- Screen time update: single transaction (upsert session record)

### 8.11 Rate Limiting

- Login: 5 per hour per device (prevent brute force)
- OTP send: 3 per hour per phone number
- Community post: 10 per hour per student
- Homework submit: 20 per hour per student
- AI tutor: delegated to existing AI tutor rate limits

### 8.12 Configuration

- `STUDENT_APP_ENABLED` — default `false`; enable/disable student app feature
- `STUDENT_APP_OTP_EXPIRY_MINUTES` — default `5`; OTP expiry time
- `STUDENT_APP_SCREEN_TIME_TRACKING_ENABLED` — default `true`; enable screen time tracking
- `STUDENT_APP_SCREEN_TIME_DEFAULT_LIMIT_MINUTES` — default `120`; default daily limit (2 hours)
- `STUDENT_APP_SCREEN_TIME_MODE` — default `warn`; `warn` or `block` when limit exceeded
- `STUDENT_APP_COMMUNITY_FEED_ENABLED` — default `true`; enable community feed
- `STUDENT_APP_COMMUNITY_FEED_MODERATION_REQUIRED` — default `true`; require teacher approval

---

## 9. API Contracts

### 9.1 Student Auth Endpoints

```
POST /api/v1/auth/student/login
  Body: { student_id, password }
  → 200: { token, student: StudentDto }
  → 401: Invalid credentials

POST /api/v1/auth/student/otp-send
  Body: { phone }
  → 200: { otp_sent: true }

POST /api/v1/auth/student/otp-login
  Body: { phone, otp }
  → 200: { token, student: StudentDto }
  → 401: Invalid OTP
  → 410: OTP expired
```

### 9.2 Student Endpoints

```
GET /api/v1/student/dashboard
  → 200: { timetable: [...], homework: [...], tests: [...], attendance: {...} }

GET /api/v1/student/timetable
  → 200: { timetable: [...] }

GET /api/v1/student/homework
  → 200: { homework: [...] }

POST /api/v1/student/homework/{id}/submit
  Body: { file_url?, text? }
  → 201: SubmissionDto

GET /api/v1/student/marks
  → 200: { marks: [...] }

GET /api/v1/student/attendance
  → 200: { attendance: {...} }

POST /api/v1/student/wellness/checkin
  Body: { mood }
  → 201: CheckInDto

GET /api/v1/student/ai-tutor/...
  (delegates to AI Tutor endpoints with student JWT)

GET /api/v1/student/portfolio/...
  (delegates to Portfolio endpoints with student JWT)

GET /api/v1/student/community-feed/{classId}
  → 200: { posts: [...] }

POST /api/v1/student/community-feed/{classId}
  Body: { content, image_url? }
  → 201: PostDto (status=pending)

DELETE /api/v1/student/community-feed/posts/{id}
  → 204: No Content

POST /api/v1/student/screen-time/heartbeat
  Body: { active_seconds }
  → 200: { screen_time_seconds, limit_reached: Boolean }
```

### 9.3 Parent (Controls) Endpoints

```
GET /api/v1/parent/student-app/activity/{childId}
  → 200: { last_active, screen_time_today, features_used: [...] }

POST /api/v1/parent/student-app/limits/{childId}
  Body: { daily_limit_minutes }
  → 200: { daily_limit_minutes }

POST /api/v1/parent/student-app/invite/{childId}
  Body: { phone }
  → 200: { invite_sent: true }
```

### 9.4 Teacher (Moderation) Endpoints

```
GET /api/v1/teacher/community-feed/{classId}/pending
  → 200: { posts: [...] }

POST /api/v1/teacher/community-feed/posts/{id}/moderate
  Body: { action: "approve" | "reject" }
  → 200: PostDto
```

### 9.5 DTO Models

All `@Serializable`, wrapped in `ApiResponse<T>` pattern.

```kotlin
@Serializable data class StudentDto(
    val id: String,
    val name: String,
    val classId: String,
    val className: String,
    val schoolId: String,
    val schoolName: String,
    val photoUrl: String?,
)

@Serializable data class StudentDashboardDto(
    val timetable: List<TimetableEntryDto>,
    val homework: List<HomeworkDto>,
    val tests: List<TestDto>,
    val attendance: AttendanceSummaryDto,
)

@Serializable data class PostDto(
    val id: String,
    val studentId: String,
    val studentName: String,
    val classId: String,
    val content: String,
    val imageUrl: String?,
    val status: String,        // pending | approved | rejected | deleted | removed
    val createdAt: String,
    val moderatedAt: String?,
    val moderatedByName: String?,
)

@Serializable data class AppActivityDto(
    val lastActiveAt: String,
    val screenTimeTodaySeconds: Int,
    val screenTimeLimitMinutes: Int?,
    val featuresUsed: List<String>,    // ["ai_tutor", "homework", "portfolio", ...]
    val dailyHistory: List<DayActivityDto>,
)

@Serializable data class DayActivityDto(
    val date: String,
    val screenTimeSeconds: Int,
    val featuresUsed: List<String>,
)

@Serializable data class ScreenTimeHeartbeatDto(
    val screenTimeSeconds: Int,
    val limitReached: Boolean,
    val limitMinutes: Int?,
)
```

---

## 10. Frontend Architecture

### 10.1 Screens

| Screen | Platform | Role | Description |
|---|---|---|---|
| `StudentLoginScreen` | Compose | Student | Login (credentials or OTP) |
| `StudentDashboardScreen` | Compose | Student | Dashboard (timetable, homework, tests, attendance) |
| `StudentHomeworkScreen` | Compose | Student | Homework list + submit |
| `StudentMarksScreen` | Compose | Student | Marks view |
| `StudentAttendanceScreen` | Compose | Student | Attendance view |
| `StudentTutorScreen` | Compose | Student | AI tutor (delegates to existing tutor UI) |
| `StudentPortfolioScreen` | Compose | Student | Portfolio (delegates to existing portfolio UI) |
| `StudentWellnessScreen` | Compose | Student | Wellness check-in (delegates to existing wellness UI) |
| `StudentCommunityFeedScreen` | Compose | Student | Community feed (view + post) |
| `ParentalControlsScreen` | Compose | Parent | View child activity, set limits |
| `TeacherModerationScreen` | Compose | Teacher | Moderate community feed posts |

### 10.2 Navigation

- Student: Login → Dashboard → Homework / Marks / Attendance / Tutor / Portfolio / Wellness / Community
- Parent: Home → Student App → Activity / Limits
- Teacher: Dashboard → Community Feed → Moderation

### 10.3 UX Flows

#### Student: Login with Credentials
1. Student opens "Vidya Prayag Student" app
2. Enters student ID + password
3. JWT issued → dashboard loads
4. If first login → prompt to change password

#### Student: Login with OTP
1. Parent generates invite from parent app
2. Student opens app → enters phone number
3. OTP sent → student enters OTP
4. Linked to parent's child record → JWT issued → dashboard loads

#### Student: Submit Homework
1. Student opens Homework → sees assigned homework
2. Taps homework item → sees details
3. Submits online (file upload or text)
4. Submission status updated → confirmation

#### Parent: Set Screen Time Limit
1. Parent opens Parental Controls → selects child
2. Views current activity (screen time, features used)
3. Sets daily limit (slider: 30-300 minutes)
4. Limit saved → student app will enforce

### 10.4 State Management

```kotlin
data class StudentDashboardState(
    val dashboard: StudentDashboardDto?,
    val isLoading: Boolean,
    val error: String?,
)

data class StudentHomeworkState(
    val homework: List<HomeworkDto>,
    val selectedHomework: HomeworkDto?,
    val isSubmitting: Boolean,
    val error: String?,
)

data class CommunityFeedState(
    val posts: List<PostDto>,
    val isPosting: Boolean,
    val error: String?,
)

data class ParentalControlsState(
    val activity: AppActivityDto?,
    val dailyLimitMinutes: Int,
    val isSaving: Boolean,
    val error: String?,
)
```

### 10.5 Offline Support

- Dashboard: cached locally (2-minute TTL)
- Timetable: cached locally (10-minute TTL)
- Homework: cached locally (2-minute TTL)
- Community feed: cached locally (1-minute TTL)
- AI tutor: requires online (AI call)
- Portfolio: cached locally (5-minute TTL, delegated)
- Wellness: requires online (submit check-in)

### 10.6 Loading States

- Login: "Logging in..."
- Dashboard: "Loading dashboard..."
- Homework: "Loading homework..."
- Submitting: "Submitting homework..."
- Community feed: "Loading feed..."
- Posting: "Posting... (awaiting moderation)"

### 10.7 Error Handling (UI)

- Login failed: "Invalid student ID or password."
- OTP expired: "OTP has expired. Please request a new one."
- Screen time limit reached: "You've reached your daily screen time limit. Take a break!"
- Post rejected: "Your post was not approved by your teacher."
- No homework: "No homework assigned."
- No marks: "No marks published yet."
- Permission denied: "You don't have access to this feature."

### 10.8 Component Integration Guidelines

| Rule | Description |
|---|---|
| **R1** | Age-appropriate: larger touch targets (min 48dp), simpler navigation |
| **R2** | Gamified elements: badges, streaks, progress bars |
| **R3** | Bottom navigation: Dashboard, Homework, Tutor, Portfolio, More |
| **R4** | Dashboard cards: timetable, homework, tests, attendance (tap to expand) |
| **R5** | Community feed: card-based, student name + photo, content, image |
| **R6** | Post status: "Pending approval" label for unapproved posts |
| **R7** | Screen time: subtle indicator in header (remaining time) |
| **R8** | Dark mode: full support (from `DARK_MODE_SPEC.md`) |
| **R9** | Tablet layout: adaptive (from `TABLET_LAYOUT_SPEC.md`) |
| **R10** | Separate app icon and name ("Vidya Prayag Student") |

---

## 11. Shared Module Changes (KMP)

### 11.1 DTOs

All DTOs defined in section 9.5, placed in `shared/.../student/domain/model/StudentAppModels.kt`.

### 11.2 Domain Models

```kotlin
data class StudentAppSession(
    val id: String,
    val studentId: String,
    val deviceId: String?,
    val appVersion: String?,
    val lastActiveAt: Instant,
    val screenTimeSeconds: Int,
    val date: LocalDate,
)

data class CommunityPost(
    val id: String,
    val studentId: String,
    val studentName: String,
    val classId: String,
    val content: String,
    val imageUrl: String?,
    val status: PostStatus,
    val createdAt: Instant,
    val moderatedAt: Instant?,
    val moderatedByName: String?,
)

enum class PostStatus { PENDING, APPROVED, REJECTED, DELETED, REMOVED }
```

### 11.3 Repository Interfaces

```kotlin
interface StudentAuthRepository {
    suspend fun loginWithCredentials(studentId: String, password: String): NetworkResult<AuthResult>
    suspend fun sendOtp(phone: String): NetworkResult<Unit>
    suspend fun loginWithOtp(phone: String, otp: String): NetworkResult<AuthResult>
    suspend fun logout(token: String): NetworkResult<Unit>
}

interface StudentAppRepository {
    suspend fun getDashboard(token: String): NetworkResult<StudentDashboardDto>
    suspend fun getTimetable(token: String): NetworkResult<List<TimetableEntryDto>>
    suspend fun getHomework(token: String): NetworkResult<List<HomeworkDto>>
    suspend fun submitHomework(token: String, id: String, fileUrl: String?, text: String?): NetworkResult<SubmissionDto>
    suspend fun getMarks(token: String): NetworkResult<List<MarksDto>>
    suspend fun getAttendance(token: String): NetworkResult<AttendanceSummaryDto>
    suspend fun getCommunityFeed(token: String, classId: String): NetworkResult<List<PostDto>>
    suspend fun createPost(token: String, classId: String, content: String, imageUrl: String?): NetworkResult<PostDto>
    suspend fun deletePost(token: String, postId: String): NetworkResult<Unit>
    suspend fun sendHeartbeat(token: String, activeSeconds: Int): NetworkResult<ScreenTimeHeartbeatDto>
}

interface ParentalControlsRepository {
    suspend fun getAppActivity(token: String, childId: String): NetworkResult<AppActivityDto>
    suspend fun setScreenTimeLimit(token: String, childId: String, dailyLimitMinutes: Int): NetworkResult<Unit>
    suspend fun sendInvite(token: String, childId: String, phone: String): NetworkResult<Unit>
}
```

### 11.4 UseCases

- `StudentLoginUseCase`
- `StudentOtpLoginUseCase`
- `GetStudentDashboardUseCase`
- `GetStudentHomeworkUseCase`
- `SubmitStudentHomeworkUseCase`
- `GetStudentMarksUseCase`
- `GetStudentAttendanceUseCase`
- `GetCommunityFeedUseCase`
- `CreateCommunityPostUseCase`
- `DeleteCommunityPostUseCase`
- `SendScreenTimeHeartbeatUseCase`
- `GetAppActivityUseCase`
- `SetScreenTimeLimitUseCase`

### 11.5 Validation

- `student_id`: non-empty
- `password`: non-empty, min 6 characters
- `phone`: valid phone number format
- `otp`: 6 digits
- `content` (post): non-empty, max 500 characters
- `image_url`: valid URL (optional)
- `daily_limit_minutes`: 30-300
- `active_seconds`: positive integer

### 11.6 Serialization

Standard Kotlinx serialization. Enums serialized as lowercase strings. Timestamps as ISO strings.

### 11.7 Network APIs

Ktor `@Resource` route definitions in `StudentAuthApi.kt`, `StudentAppApi.kt`, `ParentalControlsApi.kt`:
- POST `/api/v1/auth/student/login`
- POST `/api/v1/auth/student/otp-send`
- POST `/api/v1/auth/student/otp-login`
- GET `/api/v1/student/dashboard`
- GET `/api/v1/student/timetable`
- GET `/api/v1/student/homework`
- POST `/api/v1/student/homework/{id}/submit`
- GET `/api/v1/student/marks`
- GET `/api/v1/student/attendance`
- GET `/api/v1/student/community-feed/{classId}`
- POST `/api/v1/student/community-feed/{classId}`
- DELETE `/api/v1/student/community-feed/posts/{id}`
- POST `/api/v1/student/screen-time/heartbeat`
- GET `/api/v1/parent/student-app/activity/{childId}`
- POST `/api/v1/parent/student-app/limits/{childId}`
- POST `/api/v1/parent/student-app/invite/{childId}`

### 11.8 Database Models (Local Cache)

- Dashboard: cached locally (2-minute TTL)
- Timetable: cached locally (10-minute TTL)
- Homework: cached locally (2-minute TTL)
- Community feed: cached locally (1-minute TTL)
- Marks: cached locally (5-minute TTL)
- Attendance: cached locally (5-minute TTL)

---

## 12. Permissions Matrix

| Action | Super Admin | School Admin | Counselor | Teacher | Parent | Student |
|---|---|---|---|---|---|---|
| Student login | N/A | N/A | N/A | N/A | N/A | ✅ (own account) |
| View dashboard | N/A | N/A | N/A | N/A | N/A | ✅ (own) |
| View homework | N/A | ✅ (own school) | N/A | ✅ (own class) | ✅ (own child) | ✅ (own) |
| Submit homework | N/A | N/A | N/A | N/A | N/A | ✅ (own) |
| View marks | N/A | ✅ (own school) | ✅ (assigned) | ✅ (own class) | ✅ (own child) | ✅ (own) |
| View attendance | N/A | ✅ (own school) | ✅ (assigned) | ✅ (own class) | ✅ (own child) | ✅ (own) |
| Access AI tutor | N/A | N/A | N/A | N/A | N/A | ✅ (own) |
| Manage portfolio | N/A | N/A | N/A | N/A | ✅ (own child) | ✅ (own) |
| Wellness check-in | N/A | N/A | N/A | N/A | N/A | ✅ (own) |
| View community feed | N/A | ✅ (own school) | N/A | ✅ (own class) | N/A | ✅ (own class) |
| Create community post | N/A | N/A | N/A | N/A | N/A | ✅ (own class) |
| Moderate community feed | N/A | ✅ (own school) | N/A | ✅ (own class) | N/A | ❌ |
| View fee data | ✅ (all) | ✅ (own school) | ❌ | ❌ | ✅ (own child) | ❌ |
| View app activity | N/A | N/A | N/A | N/A | ✅ (own child) | ❌ |
| Set screen time limit | N/A | N/A | N/A | N/A | ✅ (own child) | ❌ |
| Generate student invite | N/A | N/A | N/A | N/A | ✅ (own child) | ❌ |
| Create student account | N/A | ✅ (own school) | N/A | N/A | N/A | ❌ |

---

## 13. Notifications

### Student Notifications

| Trigger | Recipient | Channel | Template |
|---|---|---|---|
| Homework assigned | Student | FCM + in-app | "New homework: {homework_title} for {subject}. Due: {due_date}." |
| Test scheduled | Student | FCM + in-app | "Test scheduled: {test_name} for {subject} on {date}." |
| Marks published | Student | FCM + in-app | "Marks published for {subject}: {score}." |
| AI tutor reminder | Student | FCM (low) | "Ready for a study session? Your AI tutor is waiting!" |
| Community post approved | Student | In-app | "Your post was approved and is now visible to your class." |
| Community post rejected | Student | In-app | "Your post was not approved. Please review community guidelines." |
| Screen time warning (80%) | Student | In-app | "You've used 80% of your daily screen time." |
| Screen time limit reached | Student | FCM + in-app | "You've reached your daily screen time limit. Take a break!" |

### Notification Integration

Uses `SmartNotificationService` with "normal" priority for homework, tests, marks. Uses "low" priority for AI tutor reminders. Uses "high" priority for screen time limit reached.

---

## 14. Background Jobs

### Screen Time Reset Job

| Property | Value |
|---|---|
| **Name** | `ScreenTimeResetJob` |
| **Schedule** | Daily at midnight |
| **Duration** | < 5 seconds |

#### Job Flow

1. New day starts → screen time resets to 0 for all students
2. New `student_app_sessions` record created for each active student (on next heartbeat)
3. Return count of sessions reset

### Activity Sync Job

| Property | Value |
|---|---|
| **Name** | `ActivitySyncJob` |
| **Schedule** | Every 5 minutes |
| **Duration** | < 10 seconds |

#### Job Flow

1. Sync student app activity to parent (if parent_linked_id exists)
2. Update `last_active_at` and `screen_time_seconds`
3. Return count of sessions synced

---

## 15. Integrations

### Internal Integrations

| System | Integration Point | Direction | Protocol | Error Handling |
|---|---|---|---|---|
| `VIDYASETU_AI_TUTOR_SPEC.md` | AI tutor | Delegate | API call | Delegate error |
| `STUDENT_PORTFOLIO_SPEC.md` | Portfolio | Delegate | API call | Delegate error |
| `STUDENT_WELLNESS_SPEC.md` | Wellness | Delegate | API call | Delegate error |
| `HomeworkService` | Homework | Delegate | API call | Delegate error |
| `AuthService` | Auth | Extend | Direct call | Required |
| `Notify.kt` | Notification dispatch | Call | Direct call | Log error, continue |
| `SmartNotificationService` | Notification priority | Call | Direct call | Log error, continue |
| `StudentsTable` | Student info | Read | Direct DB | Required |
| `AppUsersTable` | User accounts | Read/Write | Direct DB | Required |
| `TimetableTable` | Timetable | Read | Direct DB | Required |
| `MarksTable` | Marks | Read | Direct DB | Required |
| `AttendanceTable` | Attendance | Read | Direct DB | Required |

### External Integrations

| System | Integration Point | Direction | Protocol | Error Handling |
|---|---|---|---|---|
| FCM | Push notifications | Call | HTTP API | Log error, continue |
| OTP provider | OTP delivery | Call | HTTP API | Log error, OTP not sent |

### Integration Patterns

- **AI Tutor/Portfolio/Wellness delegation:** Student app calls existing endpoints with student JWT. Existing services handle scoping based on `linked_student_id`. No duplication.
- **Auth extension:** `AuthService` extended with student login methods. JWT includes `role=student`, `linked_student_id`, `parent_linked_id` (if applicable).
- **Screen time tracking:** Student app sends heartbeat every 60 seconds. `ParentalControlsService.updateScreenTime()` upserts session record.

---

## 16. Security

### Authentication

- Student login: credentials (student ID + password) or OTP (phone + OTP)
- JWT issued with role=student, linked_student_id, parent_linked_id (if applicable)
- JWT expiry: 24 hours (student app, longer session for convenience)
- OTP expiry: 5 minutes

### Authorization

- All student endpoints: student role + JWT auth + scoped to linked_student_id
- No access to: `/api/v1/fee/*`, `/api/v1/admin/*`, `/api/v1/school/*`
- Parental controls: parent role + parent-child relationship
- Community feed moderation: teacher role + teacher-class relationship
- Middleware enforces scoping based on JWT claims

### Data Protection

- Student can only see own data (marks, attendance, wellness, portfolio)
- No access to other students' private data
- Community feed: only approved posts visible to class
- Parental controls: parent sees own child's activity only
- Screen time data: parent sees own child only

### Input Validation

- `student_id`: non-empty
- `password`: min 6 characters
- `phone`: valid phone number format
- `otp`: 6 digits
- `content` (post): non-empty, max 500 characters
- `daily_limit_minutes`: 30-300
- `active_seconds`: positive integer

### Rate Limiting

- Login: 5 per hour per device (prevent brute force)
- OTP send: 3 per hour per phone number
- Community post: 10 per hour per student
- Homework submit: 20 per hour per student

### Audit Logging

- Student login: student ID, auth method (credentials/OTP), timestamp
- Student logout: student ID, timestamp
- Homework submitted: student ID, homework ID, timestamp
- Community post created: student ID, post ID, timestamp
- Post moderated: teacher ID, post ID, action, timestamp
- Screen time limit set: parent ID, child ID, limit, timestamp
- Screen time limit reached: student ID, timestamp

### PII Handling

- Student name, photo, class info visible in community feed (class-scoped)
- Student marks, attendance, wellness: own data only
- Screen time data: parent sees own child only
- No external sharing of student data
- Student phone number (for OTP): stored temporarily, not shared

### Multi-tenant Isolation

- All student data scoped by school_id (via student → class → school)
- Student JWT includes school_id
- Queries filtered by school_id
- No cross-school student visibility

---

## 17. Performance & Scalability

### Expected Scale

- Students: ~200-500 per school
- Concurrent student app users: ~100-200 per school (during school hours)
- Dashboard load: < 2 seconds
- Heartbeat: every 60 seconds per active student
- Community feed: ~10-20 posts per day per class

### Query Optimization

- Dashboard: parallel queries (timetable, homework, tests, attendance) — each < 500ms
- Community feed: indexed by class_id, status (approved only for students)
- Screen time: UNIQUE(student_id, date) — fast upsert
- Marks: indexed by student_id — fast lookup

### Indexing Strategy

- `student_app_sessions(student_id, date)` — UNIQUE, fast upsert
- `app_users(linked_student_id)` — student account lookup (recommended)
- `app_users(parent_linked_id)` — parent-child relationship (recommended)
- Community feed posts: `(class_id, status, created_at DESC)` — feed lookup (recommended)

### Caching Strategy

- Dashboard: cached locally (2-minute TTL)
- Timetable: cached locally (10-minute TTL)
- Homework: cached locally (2-minute TTL)
- Community feed: cached locally (1-minute TTL)
- Screen time: not cached (real-time tracking)

### Pagination

- Homework: 20 per page
- Community feed: 20 per page (infinite scroll)
- Marks: all in single response (max ~50 per term)

### Connection Pooling

Uses existing HikariCP connection pool. No additional pooling needed.

### Async Processing

- Login: synchronous (student waits)
- Dashboard: synchronous (parallel queries)
- Homework submit: synchronous (student waits)
- Screen time heartbeat: asynchronous (fire-and-forget)
- Activity sync: async (background job)
- Notifications: async (existing infrastructure)

### Scalability Concerns

- Heartbeats: ~200 students × 1 per minute = ~200 requests/minute. Negligible.
- Dashboard: parallel queries, each < 500ms. Total < 2 seconds.
- Community feed: ~20 posts/day. Negligible.
- DB storage: sessions ~500 records/day. Negligible.

---

## 18. Edge Cases

| # | Scenario | Expected Behavior |
|---|---|---|
| EC-1 | Student tries to access fee endpoint | Return 403. "Students don't have access to this feature." |
| EC-2 | Student tries to view another student's marks | Return 403. "You can only view your own data." |
| EC-3 | Student tries to access admin endpoint | Return 403. "Students don't have access to this feature." |
| EC-4 | OTP expired | Return 410. "OTP has expired. Please request a new one." |
| EC-5 | Screen time limit reached (warn mode) | Show warning. Student can continue. |
| EC-6 | Screen time limit reached (block mode) | Show blocking screen. Student cannot continue until next day. |
| EC-7 | Community post with no content | Return 400. "Post content cannot be empty." |
| EC-8 | Student deletes post already removed by teacher | Return 404. "Post not found." |
| EC-9 | Parent sets limit lower than current screen time | Limit saved. Student sees warning immediately. |
| EC-10 | Student logs in on multiple devices | Allowed. Screen time tracked per student (not per device). |
| EC-11 | Student account deactivated by admin | JWT invalidated. Student cannot log in. |
| EC-12 | Parent not linked (Option A credentials) | Parental controls optional. No parent activity sync. |

---

## 19. Error Handling

### Error Response Format

Standard `ApiResponse` error format.

### Error Codes

| Code | HTTP Status | Description | User Message |
|---|---|---|---|
| `INVALID_CREDENTIALS` | 401 | Invalid student ID or password | "Invalid student ID or password." |
| `OTP_EXPIRED` | 410 | OTP has expired | "OTP has expired. Please request a new one." |
| `INVALID_OTP` | 401 | Invalid OTP | "Invalid OTP. Please try again." |
| `STUDENT_NOT_FOUND` | 404 | Student not found | "Student not found." |
| `NOT_AUTHORIZED` | 403 | Role not authorized | "Students don't have access to this feature." |
| `SCREEN_TIME_LIMIT_REACHED` | 403 | Screen time limit reached | "You've reached your daily screen time limit." |
| `POST_NOT_FOUND` | 404 | Community post not found | "Post not found." |
| `POST_CONTENT_EMPTY` | 400 | Post content is empty | "Post content cannot be empty." |
| `POST_ALREADY_MODERATED` | 409 | Post already moderated | "This post has already been moderated." |
| `NOT_PARENT_OF_STUDENT` | 403 | Parent not linked to student | "You can only view your own child's activity." |

### Error Handling Strategy

- **Login failure:** Return 401. Student retries.
- **OTP expired:** Return 410. Student requests new OTP.
- **Screen time limit:** Return 403 (block mode) or show warning (warn mode).
- **Permission denied:** Return 403. Student informed of restriction.
- **Post moderation conflict:** Return 409. Teacher informed.

### Retry Strategy

- Login: student retries (credentials may be mistyped)
- OTP: student requests new OTP (5-minute expiry)
- Heartbeat: no retry (next heartbeat in 60 seconds)

### Fallback Behavior

- OTP provider unavailable: login via credentials (Option A) still works
- FCM unavailable: in-app notifications still work
- AI tutor unavailable: student informed, can retry
- Community feed unavailable: student sees cached feed

---

## 20. Analytics & Reporting

### Analytics Dashboard Data

| Metric | Source | Derivation |
|---|---|---|
| Student app adoption | `app_users` | Count of student role accounts |
| Daily active students | `student_app_sessions` | Count of unique students per day |
| Average screen time | `student_app_sessions` | avg(screen_time_seconds) per student per day |
| Feature usage | API logs | Count of requests per feature (tutor, homework, portfolio, etc.) |
| Community feed activity | Community posts | Count of posts, approval rate |
| Parental controls usage | Parent settings | Count of parents with limits set |

### Export Capabilities

- Student app adoption report (CSV) — school, student count, adoption rate
- Screen time report (CSV) — student, daily screen time, limit
- Community feed report (CSV) — class, post count, approval rate

### Report Types

| Report | Format | Frequency | Recipient |
|---|---|---|---|
| Student app adoption | JSON (API) | Monthly | Product Team |
| Feature usage | JSON (API) | Monthly | Product Team |
| Screen time analytics | JSON (API) | Monthly | Product Team |

---

## 21. Testing Strategy

### Unit Tests

- `StudentAuthService.loginWithCredentials()` — credential validation, JWT issuance
- `StudentAuthService.loginWithOtp()` — OTP verification, parent linking
- `ParentalControlsService.setScreenTimeLimit()` — limit setting, validation
- `ParentalControlsService.checkScreenTime()` — limit check, edge cases
- `CommunityFeedService.createPost()` — post creation, status=pending
- `CommunityFeedService.moderatePost()` — approve/reject, status update
- Screen time tracking: heartbeat, cumulative tracking, daily reset

### Integration Tests

- Full auth flow: credentials login → JWT → dashboard access
- Full OTP flow: parent invite → OTP send → OTP verify → JWT → dashboard
- Homework: view → submit → verify status
- Community feed: create post → teacher moderates → student sees approved
- Parental controls: set limit → student hits limit → warning/block
- Permission checks: student blocked from fee, admin, other students' data
- Multi-tenant: verify school-scoped queries

### E2E Tests

- Student logs in → views dashboard → submits homework → sees marks
- Student creates community post → teacher approves → post visible to class
- Parent sets screen time limit → student reaches limit → warning shown
- Student accesses AI tutor → asks question → gets response

### Performance Tests

- Dashboard load: < 2 seconds (parallel queries)
- Heartbeat: < 100ms (upsert)
- Community feed: < 2 seconds for 50 posts
- Login: < 2 seconds

### Test Data

- 10 test students with varying data (marks, homework, attendance)
- 1 test teacher with assigned class
- 1 test parent with child
- Test community feed posts (pending, approved, rejected)
- Test screen time sessions
- Mock AI tutor service
- Mock OTP provider

### Test Environment

- Test database with student app tables
- Mock OTP provider
- Mock AI tutor service
- Test JWT tokens for student, parent, teacher roles
- Test FCM tokens

---

## 22. Acceptance Criteria

- [ ] Student can log in (credentials or parent-linked OTP)
- [ ] Dashboard shows timetable, homework, marks, attendance
- [ ] Homework submission works
- [ ] AI Tutor accessible
- [ ] Portfolio management works
- [ ] Wellness check-in works
- [ ] Community feed viewable (with moderation)
- [ ] No access to fee data or admin functions
- [ ] Parental controls: activity view + screen time limits
- [ ] Notifications received
- [ ] Screen time tracking works (heartbeat, daily reset)
- [ ] Community feed posts require teacher approval
- [ ] Student can only see own data (no other students' private data)
- [ ] Age-appropriate UI (larger touch targets, simpler navigation)

---

## 23. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 2 days | Student auth flow + JWT role, modify `app_users`, `StudentAuthService` |
| 2 | 2 days | Student API middleware + endpoints (`StudentRouting`) |
| 3 | 3 days | Student dashboard + homework + marks views |
| 4 | 2 days | Wellness + community feed integration |
| 5 | 2 days | Parental controls (`ParentalControlsService`, screen time tracking) |
| 6 | 5 days | Student app UI (dashboard, homework, marks, tutor, portfolio, wellness, community) |
| 7 | 2 days | Tests: unit, integration, E2E |

### Pre-Implementation Checklist

- [ ] Verify `VIDYASETU_AI_TUTOR_SPEC.md` is implemented
- [ ] Verify `STUDENT_PORTFOLIO_SPEC.md` is implemented
- [ ] Verify `STUDENT_WELLNESS_SPEC.md` is implemented
- [ ] Verify `AppUsersTable` can be modified (add columns)
- [ ] Verify OTP provider is available
- [ ] Verify notification infrastructure (FCM, SmartNotificationService)
- [ ] Verify `DARK_MODE_SPEC.md` and `TABLET_LAYOUT_SPEC.md` are implemented
- [ ] Set up separate `studentApp/` KMP module

---

## 24. File-Level Impact Analysis

### Server

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Modify + Add | Columns on `AppUsersTable` + `StudentAppSessionsTable` |
| `server/.../db/DatabaseFactory.kt` | Modify | Register new table in `allTables` |
| `server/.../feature/auth/AuthService.kt` | Modify | Student auth flow (credentials + OTP) |
| `server/.../feature/auth/AuthRouting.kt` | Modify | Student login endpoints |
| `server/.../feature/student/StudentRouting.kt` | **New** | Student API endpoints |
| `server/.../feature/student/StudentMiddleware.kt` | **New** | Student JWT middleware (scoping, restrictions) |
| `server/.../feature/student/ParentalControlsService.kt` | **New** | Parental controls service |
| `server/.../feature/student/CommunityFeedService.kt` | **New** | Community feed with moderation |
| `docs/db/migration_090_student_app.sql` | **New** | DDL (ALTER + CREATE) |

### Shared (KMP)

| File | Change Type | Description |
|---|---|---|
| `shared/.../student/domain/model/StudentAppModels.kt` | **New** | DTOs, domain models, enums |
| `shared/.../student/domain/repository/StudentAuthRepository.kt` | **New** | Auth repository interface |
| `shared/.../student/domain/repository/StudentAppRepository.kt` | **New** | App repository interface |
| `shared/.../student/domain/repository/ParentalControlsRepository.kt` | **New** | Parental controls repository |
| `shared/.../student/data/remote/StudentAuthApi.kt` | **New** | Auth HTTP API definitions |
| `shared/.../student/data/remote/StudentAppApi.kt` | **New** | App HTTP API definitions |
| `shared/.../student/data/remote/ParentalControlsApi.kt` | **New** | Parental controls HTTP API |

### Client (Student App)

| File | Change Type | Description |
|---|---|---|
| `studentApp/` | **New** | Separate KMP app module |
| `studentApp/build.gradle.kts` | **New** | Gradle config for student app |
| `studentApp/src/.../StudentApp.kt` | **New** | App entry point |
| `studentApp/src/.../ui/StudentNavigation.kt` | **New** | Navigation graph |
| `studentApp/src/.../ui/screens/StudentLoginScreen.kt` | **New** | Login screen |
| `studentApp/src/.../ui/screens/StudentDashboardScreen.kt` | **New** | Dashboard |
| `studentApp/src/.../ui/screens/StudentHomeworkScreen.kt` | **New** | Homework list + submit |
| `studentApp/src/.../ui/screens/StudentMarksScreen.kt` | **New** | Marks view |
| `studentApp/src/.../ui/screens/StudentCommunityFeedScreen.kt` | **New** | Community feed |
| `studentApp/src/.../ui/screens/StudentWellnessScreen.kt` | **New** | Wellness check-in |

### Client (Parent App — additions)

| File | Change Type | Description |
|---|---|---|
| `composeApp/.../ui/v2/screens/parent/ParentalControlsScreen.kt` | **New** | Parental controls UI |

### Client (Teacher App — additions)

| File | Change Type | Description |
|---|---|---|
| `composeApp/.../ui/v2/screens/teacher/CommunityModerationScreen.kt` | **New** | Community feed moderation |

---

## 25. Future Enhancements

| # | Enhancement | Priority | Effort | Notes |
|---|---|---|---|---|
| F-1 | iOS student app | High | XL | Port to iOS (currently Android-first) |
| F-2 | Customizable themes | Low | S | Student can choose app theme/color |
| F-3 | Gamification | Medium | M | Points, badges, leaderboards for engagement |
| F-4 | Offline-first | Medium | L | Full offline support with sync |
| F-5 | Student-to-student collaboration | Low | L | Group study, shared notes (with moderation) |
| F-6 | Push notification preferences | Medium | S | Student can choose which notifications to receive |
| F-7 | App usage analytics for students | Low | S | Students see own usage stats |
| F-8 | Multi-language support | Medium | M | Student app in multiple languages |
| F-9 | Voice commands | Low | M | Voice navigation for younger students |
| F-10 | Parent-teacher chat about student | Medium | M | Direct communication about student's progress |

---

## Appendix A: Sequence Diagrams

### A.1 Student Login (Credentials)

```
Student (app)       Server              DB
  │                    │                  │
  │  POST /auth/       │                  │
  │  student/login     │                  │
  │  {student_id,      │                  │
  │   password}        │                  │
  │  ───────────────>  │                  │
  │                    │──verify creds──>│
  │                    │←──ok─────────────│
  │                    │                  │
  │                    │──create JWT──────│
  │                    │  (role=student)  │
  │                    │──upsert session─>│
  │                    │←──ok─────────────│
  │  ←──200: Token─────│                  │
  │  + StudentDto      │                  │
  │                    │                  │
```

### A.2 Student Login (OTP)

```
Parent (app)        Server              DB              OTP Provider
  │                    │                  │                │
  │  POST /parent/     │                  │                │
  │  student-app/      │                  │                │
  │  invite/{childId}  │                  │                │
  │  {phone}           │                  │                │
  │  ───────────────>  │                  │                │
  │                    │──store phone────>│                │
  │  ←──200: Invite────│                  │                │
  │                    │                  │                │

Student (app)       Server              DB              OTP Provider
  │                    │                  │                │
  │  POST /auth/       │                  │                │
  │  student/otp-send  │                  │                │
  │  {phone}           │                  │                │
  │  ───────────────>  │                  │                │
  │                    │──send OTP──────────────────────>│
  │                    │←──sent──────────────────────────│
  │  ←──200: OTP sent──│                  │                │
  │                    │                  │                │
  │  POST /auth/       │                  │                │
  │  student/otp-login │                  │                │
  │  {phone, otp}      │                  │                │
  │  ───────────────>  │                  │                │
  │                    │──verify OTP──────│                │
  │                    │←──ok─────────────│                │
  │                    │──link to child──>│                │
  │                    │──create JWT──────│                │
  │                    │  (role=student,  │                │
  │                    │   parent_linked) │                │
  │  ←──200: Token─────│                  │                │
  │  + StudentDto      │                  │                │
  │                    │                  │                │
```

### A.3 Screen Time Heartbeat

```
Student (app)       Server              DB
  │                    │                  │
  │  POST /student/    │                  │
  │  screen-time/      │                  │ (every 60 sec)
  │  heartbeat         │                  │
  │  {active_seconds: 60}│                │
  │  ───────────────>  │                  │
  │                    │──upsert session>│                │
  │                    │  screen_time += 60│               │
  │                    │←──ok─────────────│                │
  │                    │                  │                │
  │                    │──check limit─────│                │
  │                    │  (vs parent set) │                │
  │                    │←──limit status──│                │
  │  ←──200: {         │                  │
  │    screen_time,    │                  │
  │    limit_reached}  │                  │
  │                    │                  │
  │  (if limit reached:│                  │
  │   show warning or  │                  │
  │   block screen)    │                  │
  │                    │                  │
```

---

## Appendix B: Domain Model / ER Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                    app_users (modified)                               │
│  id (PK)                                                              │
│  ... existing columns ...                                             │
│  linked_student_id (NEW, nullable, FK students.id)                    │
│  parent_linked_id (NEW, nullable, FK app_users.id)                    │
│  role: parent | teacher | admin | counselor | student (NEW)           │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│                  student_app_sessions (new)                           │
│  id (PK)                                                              │
│  student_id, device_id, app_version                                   │
│  last_active_at, screen_time_seconds                                  │
│  date                                                                 │
│  UNIQUE(student_id, date)                                             │
└──────────────────────────────────────────────────────────────────────┘

Existing tables used:
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│ students         │  │ homework_        │  │ marks            │
│ (existing)       │  │ submissions      │  │ (existing)       │
│ student info     │  │ (existing)       │  │ marks/grades     │
└──────────────────┘  └──────────────────┘  └──────────────────┘
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│ attendance       │  │ timetable        │  │ notifications    │
│ (existing)       │  │ (existing)       │  │ (existing)       │
│ attendance       │  │ timetable        │  │ notifications    │
└──────────────────┘  └──────────────────┘  └──────────────────┘
```

---

## Appendix C: Event Flow

### Domain Events

| Event | Emitter | Consumers | Payload | Side Effects |
|---|---|---|---|---|
| `StudentLoggedIn` | `StudentAuthService` | None (logged) | `studentId, authMethod, timestamp` | Audit log |
| `StudentLoggedOut` | `StudentAuthService` | None (logged) | `studentId, timestamp` | Audit log |
| `ScreenTimeLimitReached` | `ParentalControlsService` | `NotificationService` | `studentId, limitMinutes` | FCM to student |
| `ScreenTimeLimitSet` | `ParentalControlsService` | None (logged) | `childId, parentId, limitMinutes` | Audit log |
| `CommunityPostCreated` | `CommunityFeedService` | `NotificationService` (teacher) | `studentId, postId, classId` | In-app to teacher (moderation queue) |
| `CommunityPostApproved` | `CommunityFeedService` | `NotificationService` (student) | `postId, teacherId` | In-app to student |
| `CommunityPostRejected` | `CommunityFeedService` | `NotificationService` (student) | `postId, teacherId, reason` | In-app to student |
| `HomeworkSubmitted` | Student homework endpoint | `NotificationService` (teacher) | `studentId, homeworkId` | In-app to teacher |

### Event Delivery Guarantees

- Login/logout: fire-and-forget logging
- Screen time limit: synchronous check, notification async
- Community post: synchronous creation, notification async
- Post moderation: synchronous update, notification async
- Homework submission: synchronous, notification async

---

## Appendix D: Configuration

### Environment Variables

| Variable | Default | Description |
|---|---|---|
| `STUDENT_APP_ENABLED` | `false` | Enable/disable student app |
| `STUDENT_APP_OTP_EXPIRY_MINUTES` | `5` | OTP expiry time |
| `STUDENT_APP_SCREEN_TIME_TRACKING_ENABLED` | `true` | Enable screen time tracking |
| `STUDENT_APP_SCREEN_TIME_DEFAULT_LIMIT_MINUTES` | `120` | Default daily limit (2 hours) |
| `STUDENT_APP_SCREEN_TIME_MODE` | `warn` | `warn` or `block` when limit exceeded |
| `STUDENT_APP_COMMUNITY_FEED_ENABLED` | `true` | Enable community feed |
| `STUDENT_APP_COMMUNITY_FEED_MODERATION_REQUIRED` | `true` | Require teacher approval for posts |
| `STUDENT_APP_JWT_EXPIRY_HOURS` | `24` | JWT expiry for student tokens |

### Feature Flags

| Flag | Default | Description |
|---|---|---|
| `STUDENT_APP_ENABLED` | `false` | Enable/disable student app |
| `STUDENT_APP_SCREEN_TIME_TRACKING_ENABLED` | `true` | Screen time tracking |
| `STUDENT_APP_COMMUNITY_FEED_ENABLED` | `true` | Community feed |
| `STUDENT_APP_AI_TUTOR_ENABLED` | `true` | AI tutor access |
| `STUDENT_APP_PORTFOLIO_ENABLED` | `true` | Portfolio access |
| `STUDENT_APP_WELLNESS_ENABLED` | `true` | Wellness check-in |

### School-Level Settings

| Setting | Default | Description |
|---|---|---|
| `student_app_enabled` | `false` | Per-school enable/disable |
| `student_app_screen_time_tracking_enabled` | `true` | Per-school screen time |
| `student_app_community_feed_enabled` | `true` | Per-school community feed |
| `student_app_screen_time_default_limit` | `120` | Per-school default limit |

---

## Appendix E: Migration & Rollback

### Migration: `migration_090_student_app.sql`

```sql
-- Migration 090: Standalone Student App
-- Modifies app_users table and creates student_app_sessions table

BEGIN;

-- Add columns to app_users for student accounts
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS linked_student_id UUID;
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS parent_linked_id UUID;

-- Create student_app_sessions table
CREATE TABLE IF NOT EXISTS student_app_sessions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id      UUID NOT NULL,
    device_id       TEXT,
    app_version     VARCHAR(16),
    last_active_at  TIMESTAMP NOT NULL DEFAULT now(),
    screen_time_seconds INTEGER NOT NULL DEFAULT 0,
    date            DATE NOT NULL DEFAULT CURRENT_DATE,
    UNIQUE(student_id, date)
);

COMMIT;
```

### Rollback: `migration_090_rollback.sql`

```sql
BEGIN;
DROP TABLE IF EXISTS student_app_sessions;
ALTER TABLE app_users DROP COLUMN IF EXISTS parent_linked_id;
ALTER TABLE app_users DROP COLUMN IF EXISTS linked_student_id;
COMMIT;
```

### Migration Validation

- Verify `app_users` has `linked_student_id` and `parent_linked_id` columns
- Verify `student_app_sessions` table created with UNIQUE constraint
- Run `SELECT count(*) FROM student_app_sessions` — should be 0 (new feature)
- Verify `app_users` role field accepts 'student' value

---

## Appendix F: Observability

### Structured Logging

| Log Level | Event | Context Fields |
|---|---|---|
| INFO | Student logged in | `studentId, authMethod (credentials/otp), deviceId` |
| INFO | Student logged out | `studentId` |
| INFO | Homework submitted | `studentId, homeworkId` |
| INFO | Community post created | `studentId, postId, classId` |
| INFO | Post approved | `postId, teacherId` |
| INFO | Post rejected | `postId, teacherId, reason` |
| INFO | Screen time limit set | `childId, parentId, limitMinutes` |
| WARN | Screen time limit reached | `studentId, limitMinutes, screenTimeSeconds` |
| WARN | Login failed | `studentId, reason (invalid_credentials/invalid_otp)` |
| WARN | OTP expired | `phone` |
| WARN | Permission denied | `studentId, endpoint` |
| ERROR | OTP provider error | `phone, error` |

### Metrics

| Metric | Type | Labels | Description |
|---|---|---|---|
| `student_app_logins_total` | Counter | `school_id, auth_method` | Student logins |
| `student_app_active_users` | Gauge | `school_id` | Daily active students |
| `student_app_screen_time_avg` | Gauge | `school_id` | Average screen time per student |
| `student_app_screen_time_limit_reached` | Counter | `school_id` | Students hitting limit |
| `student_app_homework_submissions` | Counter | `school_id` | Homework submissions via student app |
| `student_app_community_posts` | Counter | `school_id, status` | Community posts by status |
| `student_app_feature_usage` | Counter | `school_id, feature` | Feature usage (tutor, portfolio, etc.) |
| `student_app_parental_controls_set` | Counter | `school_id` | Parents setting screen time limits |

### Health Checks

| Check | Endpoint | Description |
|---|---|---|
| Student app service | `/health/student-app` | Verify service and DB accessible |
| OTP provider | `/health/otp-provider` | Verify OTP provider reachable |

### Alerts

| Alert | Condition | Severity | Notification |
|---|---|---|---|
| OTP provider unavailable | Health check failed | Critical | Email + SMS to dev team |
| High login failure rate | > 20% failures in 1 hour | Warning | Email to dev team |
| Community feed moderation backlog | > 50 pending posts | Warning | Email to school admin |
| Screen time tracking failure | Heartbeat errors > 10% | Warning | Email to dev team |

### Dashboards

| Dashboard | Panels | Audience |
|---|---|---|
| Student App Overview | Adoption, DAU, feature usage | Product Team |
| Screen Time | Average, limit reached, by school | Product Team |
| Community Feed | Posts, approval rate, moderation queue | Product Team |
| Auth | Login success rate, by method | DevOps Team |

### Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Inappropriate content in community feed | Medium | High | Teacher moderation required. All posts pending until approved. |
| Student data exposure | Low | High | Strict scoping. Student sees own data only. Middleware enforces. |
| Screen time bypass | Medium | Low | App tracks foreground time. Can't bypass within app. |
| OTP fraud | Low | Medium | Rate limit OTP sends. 3 per hour per phone. |
| Unauthorized access (fee/admin) | Low | High | Middleware blocks. JWT role=student. Defense in depth. |
| Student account takeover | Low | High | Password change on first login. OTP for parent-linked. |
