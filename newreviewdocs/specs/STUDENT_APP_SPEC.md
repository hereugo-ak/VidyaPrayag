# Standalone Student App — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** `VIDYASETU_AI_TUTOR_SPEC.md`, `STUDENT_PORTFOLIO_SPEC.md`, `STUDENT_WELLNESS_SPEC.md`
> **Source:** `COMPETITIVE_GAP_ANALYSIS.md` — mobile-first trend

---

## 1. Feature Overview

A dedicated student-facing app (separate from parent app) that gives students direct access to their schedule, homework, AI tutor, portfolio, wellness check-ins, and class community. Age-appropriate UI with restricted permissions.

### Goals

- Student login with school-provided credentials or parent-linked OTP
- Student sees: timetable, homework, assignments, marks, attendance, AI tutor, portfolio
- Wellness check-in (mood, survey) — from `STUDENT_WELLNESS_SPEC.md`
- AI Tutor access — from `VIDYASETU_AI_TUTOR_SPEC.md`
- Portfolio management — from `STUDENT_PORTFOLIO_SPEC.md`
- Class community feed (view + post with moderation)
- Restricted: no fee data, no admin functions, no other students' private data
- Parental controls: parent can see what child accesses, set screen time limits

---

## 2. Current System Assessment

- Current app is parent-focused (parents act on behalf of students)
- `AppUsersTable` has `role` field — can add `student` role
- All infrastructure (KMP, Compose Multiplatform, Ktor) is reusable
- `COMPETITIVE_GAP_ANALYSIS.md`: mobile-first, student-centric trend

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Student login: school-provided credentials (student ID + password) or parent-linked OTP |
| FR-2 | Student dashboard: timetable, today's homework, upcoming tests, attendance summary |
| FR-3 | Homework: view assigned homework, submit online assignments |
| FR-4 | Marks: view own marks and assessment results |
| FR-5 | AI Tutor: full access to `VIDYASETU_AI_TUTOR_SPEC.md` |
| FR-6 | Portfolio: manage own portfolio (`STUDENT_PORTFOLIO_SPEC.md`) |
| FR-7 | Wellness: daily mood check-in, weekly survey (`STUDENT_WELLNESS_SPEC.md`) |
| FR-8 | Community feed: view and post (with teacher moderation) |
| FR-9 | Restricted: no fee data, no admin functions, no other students' private data |
| FR-10 | Parental controls: parent can view child's app activity, set usage limits |
| FR-11 | Notifications: homework assigned, test scheduled, marks published, AI tutor reminders |

---

## 4. Database Design

### 4.1 Modify Existing: `app_users`

```sql
-- Add 'student' to role enum/varchar
-- Student accounts linked to students table
ALTER TABLE app_users ADD COLUMN linked_student_id UUID;  -- FK students.id
ALTER TABLE app_users ADD COLUMN parent_linked_id UUID;   -- FK app_users.id (parent who authorized)
```

### 4.2 New Table: `student_app_sessions`

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

---

## 5. Backend Architecture

### 5.1 Student Auth Flow

```
Option A: School-provided credentials
1. Admin creates student app account (student ID + temp password)
2. Student logs in with student ID + password
3. JWT issued with role=student, linked_student_id

Option B: Parent-linked OTP
1. Parent generates student app invite from parent app
2. Student enters phone number → OTP sent
3. Student enters OTP → linked to parent's child record
4. JWT issued with role=student, linked_student_id, parent_linked_id
```

### 5.2 Student API Middleware

```kotlin
fun Application.studentRoutes() {
    authenticate("student-jwt") {
        route("/api/v1/student") {
            // All endpoints scoped to linked_student_id from JWT
            // No access to fee, admin, or other students' data
        }
    }
}
```

### 5.3 Parental Controls

```kotlin
class ParentalControlsService {
    suspend fun getAppActivity(parentId: UUID, childId: UUID): AppActivityDto
    suspend fun setScreenTimeLimit(parentId: UUID, childId: UUID, dailyLimitMinutes: Int)
    suspend fun checkScreenTime(studentId: UUID): Boolean  // returns false if limit exceeded
}
```

---

## 6. API Contracts

```
# Student
POST /api/v1/auth/student/login  { student_id, password }
POST /api/v1/auth/student/otp-login  { phone, otp }

GET /api/v1/student/dashboard
GET /api/v1/student/timetable
GET /api/v1/student/homework
POST /api/v1/student/homework/{id}/submit
GET /api/v1/student/marks
GET /api/v1/student/attendance
POST /api/v1/student/wellness/checkin  { mood }
GET /api/v1/student/ai-tutor/...  (delegates to AI Tutor endpoints)
GET /api/v1/student/portfolio/...  (delegates to Portfolio endpoints)
GET /api/v1/student/community-feed/{classId}

# Parent (controls)
GET /api/v1/parent/student-app/activity/{childId}
POST /api/v1/parent/student-app/limits/{childId}  { daily_limit_minutes }
```

---

## 7. Frontend Architecture

- Separate Compose Multiplatform app target or separate navigation graph within same app
- Age-appropriate UI: larger touch targets, simpler navigation, gamified elements
- Dark mode support (from `DARK_MODE_SPEC.md`)
- Tablet layout (from `TABLET_LAYOUT_SPEC.md`)

### Recommended: Separate App

Build as a separate KMP app (`studentApp/` module) sharing `shared/` module:
- Different app icon, name ("Vidya Prayag Student")
- Different entry point and navigation
- Same backend, different JWT role
- Separate Play Store/App Store listing

---

## 8. Acceptance Criteria

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

---

## 9. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 2 days | Student auth flow + JWT role |
| 2 | 2 days | Student API middleware + endpoints |
| 3 | 3 days | Student dashboard + homework + marks views |
| 4 | 2 days | Wellness + community feed integration |
| 5 | 2 days | Parental controls |
| 6 | 5 days | Student app UI (dashboard, homework, marks, tutor, portfolio, wellness) |
| 7 | 2 days | Tests |

---

## 10. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Modify + Add | Columns on app_users + `StudentAppSessionsTable` |
| `server/.../feature/auth/AuthService.kt` | Modify | Student auth flow |
| `server/.../feature/student/StudentRouting.kt` | New | Student API endpoints |
| `server/.../feature/student/ParentalControlsService.kt` | New | Parental controls |
| `docs/db/migration_089_student_app.sql` | New | DDL |
| `studentApp/` | New | Separate KMP app module (or navigation graph) |
| `composeApp/.../ui/v2/screens/student/*.kt` | New | Student screens |
