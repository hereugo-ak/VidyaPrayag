# Behavior Tracking — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** `STUDENT_WELLNESS_SPEC.md`
> **Source:** `COMPETITIVE_GAP_ANALYSIS.md` — competitor feature
> **Template:** `_SPEC_TEMPLATE.md` v1 (25 mandatory + 6 optional sections)

---

## 1. Feature Overview

### What

Student behavior tracking system: positive/negative behavior incidents, behavior points, pattern analysis, and parent visibility. Focuses on positive reinforcement with structured incident logging.

### Why — Product Rationale

Schools need structured behavior tracking to encourage positive behavior and identify patterns early. Traditional systems rely on informal notes or punishment logs. This system provides structured, data-driven behavior tracking with positive reinforcement focus. This is a **medium-priority feature** (Phase 4, effort M) that provides competitive parity.

### What Stands Out (Competitive Moat)

From `COMPETITIVE_GAP_ANALYSIS.md`: behavior tracking as competitor feature.

The key moat is **AI-powered pattern analysis** that identifies recurring behaviors and trends, combined with **Vidya Passport badge integration** that rewards positive behaviors — turning behavior tracking from punitive to motivational.

### Goals

- Teacher logs behavior incidents (positive: helpful, leadership, improvement; negative: disruptive, late, disrespectful)
- Behavior points system (configurable: +1 to +5 for positive, -1 to -5 for negative)
- Behavior score per student per term
- Pattern analysis: recurring behaviors, trends
- Parent visibility: weekly behavior summary
- Integration with wellness tracking (behavioral indicators)

### Non-goals

- [ ] Automated behavior detection via AI/cameras (manual logging only)
- [ ] Behavior-based disciplinary actions (tracking only, actions are manual)
- [ ] Peer-to-peer behavior reporting (teacher-only logging)
- [ ] Real-time behavior alerts (weekly summary, not real-time)
- [ ] Behavior contracts or improvement plans (future enhancement)
- [ ] Integration with grading/marks (behavior is separate from academics)
- [ ] Public behavior leaderboards (privacy concern, not appropriate)

### Dependencies

- `STUDENT_WELLNESS_SPEC.md` — `TeacherObservationsTable` with `behavior` field
- `VIDYA_PASSPORT_SPEC.md` — positive behaviors trigger badge awards
- `StudentsTable` — student info
- `AppUsersTable` — teacher accounts
- `ClassesTable` — class info
- `SchoolsTable` — school info
- `NotificationsTable` — weekly summary notification

### Related Modules

- `server/.../feature/behavior/` — new behavior tracking module
- `server/.../feature/wellness/` — existing wellness module (integration)
- `server/.../feature/passport/` — existing Vidya Passport module (badge integration)
- `composeApp/.../ui/v2/screens/teacher/` — teacher UI (incident logging)
- `composeApp/.../ui/v2/screens/admin/` — admin UI (behavior dashboard)
- `composeApp/.../ui/v2/screens/parent/` — parent UI (weekly summary)

---

## 2. Current System Assessment

### Existing Code

- `TeacherObservationsTable` (from `STUDENT_WELLNESS_SPEC.md`) — has `behavior` field (engaged/withdrawn/disruptive/etc.)
- No dedicated behavior tracking or points system
- `COMPETITIVE_GAP_ANALYSIS.md`: behavior tracking as competitor feature

### Existing Database

- `TeacherObservationsTable` — has `behavior` field (text, not structured)
- `StudentsTable` — student info
- `AppUsersTable` — teacher accounts
- `ClassesTable` — class info
- `SchoolsTable` — school info
- `NotificationsTable` — notification infrastructure
- No behavior incidents or categories tables

### Existing APIs

- Wellness API (existing) — teacher observations (includes behavior field)
- Student API (existing) — student info
- Notification API (existing) — notification dispatch
- No behavior tracking API

### Existing UI

- Teacher observation screen (existing) — has behavior field (dropdown)
- Student profile (existing) — no behavior history
- Parent app (existing) — no behavior summary
- No behavior tracking UI

### Existing Services

- `WellnessService` — teacher observations (includes behavior)
- `NotificationService` — notification dispatch
- No behavior tracking service

### Existing Documentation

- `COMPETITIVE_GAP_ANALYSIS.md` — behavior tracking as competitor feature
- `STUDENT_WELLNESS_SPEC.md` — wellness tracking with behavior field
- `VIDYA_PASSPORT_SPEC.md` — badge/achievement system

### Technical Debt

| # | Gap | Details |
|---|---|---|
| TD-1 | No behavior incidents table | No `behavior_incidents` table for structured logging |
| TD-2 | No behavior categories | No `behavior_categories` table for configurable categories |
| TD-3 | No points system | No configurable points per category |
| TD-4 | No behavior service | No service for logging, scoring, pattern analysis |
| TD-5 | No behavior UI | No teacher incident log, admin dashboard, or parent summary |

### Gaps

| # | Gap | Impact | Severity |
|---|---|---|---|
| G1 | No structured behavior logging | Behaviors logged as free-text, not analyzable | **High** |
| G2 | No points system | No quantitative behavior scoring | **High** |
| G3 | No pattern analysis | Can't identify recurring behaviors or trends | **Medium** |
| G4 | No parent visibility | Parents unaware of child's behavior patterns | **Medium** |
| G5 | No badge integration | Positive behaviors not rewarded | **Low** |

---

## 3. Functional Requirements

### FR-001
| Field | Value |
|---|---|
| **Title** | Log Behavior Incident |
| **Description** | Teacher logs behavior incident: student, type (positive/negative), category, points, description, date. |
| **Priority** | High |
| **User Roles** | Teacher |
| **Acceptance notes** | Teacher selects student, chooses incident type (positive/negative), selects category, points auto-filled from category default (adjustable), enters description, selects date. Incident saved to `behavior_incidents`. |

### FR-002
| Field | Value |
|---|---|
| **Title** | Behavior Categories |
| **Description** | Behavior categories: helpful, leadership, improvement, participation (positive); disruptive, late, disrespectful, bullying (negative). |
| **Priority** | High |
| **User Roles** | School Admin |
| **Acceptance notes** | Default categories pre-seeded. Admin can add/edit/deactivate categories. Each category has: name, incident_type, default_points, color, is_active. School-specific categories (school_id) or global (null school_id). |

### FR-003
| Field | Value |
|---|---|
| **Title** | Configurable Points |
| **Description** | Points: configurable per category (+1 to +5, -1 to -5). |
| **Priority** | High |
| **User Roles** | School Admin |
| **Acceptance notes** | Each category has default_points. Teacher can adjust points within range (+1 to +5 for positive, -1 to -5 for negative). Points stored per incident. |

### FR-004
| Field | Value |
|---|---|
| **Title** | Behavior Score |
| **Description** | Behavior score: cumulative per student per term. |
| **Priority** | High |
| **User Roles** | System |
| **Acceptance notes** | Score = sum of all incident points for student in term. Positive incidents add points, negative incidents subtract. Score visible to teacher, admin, and parent. |

### FR-005
| Field | Value |
|---|---|
| **Title** | Pattern Analysis |
| **Description** | Pattern analysis: AI identifies recurring behaviors and trends. |
| **Priority** | Medium |
| **User Roles** | System |
| **Acceptance notes** | AI analyzes incident history per student. Identifies: most frequent category, trend (improving/declining), recurring patterns. Uses AI infrastructure from `AI_INFRASTRUCTURE_SPEC.md`. Results shown to teacher and admin. |

### FR-006
| Field | Value |
|---|---|
| **Title** | Parent Weekly Summary |
| **Description** | Parent weekly summary: behavior incidents + score. |
| **Priority** | Medium |
| **User Roles** | Parent |
| **Acceptance notes** | Weekly notification with behavior summary: incidents this week, score change, top category. Sent via FCM + in-app. Uses SmartNotificationService. |

### FR-007
| Field | Value |
|---|---|
| **Title** | Admin Dashboard |
| **Description** | Admin dashboard: class-wise behavior overview. |
| **Priority** | Medium |
| **User Roles** | School Admin |
| **Acceptance notes** | Dashboard shows: class-wise behavior scores, top positive/negative categories, students needing attention (declining trend), overall school behavior trend. |

### FR-008
| Field | Value |
|---|---|
| **Title** | Vidya Passport Integration |
| **Description** | Integration with `VIDYA_PASSPORT_SPEC.md` (positive behaviors → badges). |
| **Priority** | Low |
| **User Roles** | System |
| **Acceptance notes** | Positive behavior incidents trigger badge awards in Vidya Passport. E.g., 5 "helpful" incidents → "Helpful Hero" badge. Configurable badge rules. |

### Non-Functional Requirements

| ID | Requirement |
|---|---|
| NFR-1 | Incident logging: < 2 seconds (teacher taps save → confirmed) |
| NFR-2 | Score calculation: < 1 second (sum of points for term) |
| NFR-3 | Pattern analysis: < 10 seconds (AI analysis per student) |
| NFR-4 | Weekly summary: sent every Monday 8 AM (background job) |
| NFR-5 | Dashboard: < 3 seconds to load (class-wise aggregation) |
| NFR-6 | Points range: +1 to +5 (positive), -1 to -5 (negative) |

---

## 4. User Stories

### Teacher
- [ ] Log a positive behavior incident for a student
- [ ] Log a negative behavior incident for a student
- [ ] View a student's behavior history and score for the term
- [ ] See AI pattern analysis for a student's behavior
- [ ] View class-wise behavior overview

### School Admin
- [ ] Configure behavior categories (add, edit, deactivate)
- [ ] Set default points per category
- [ ] View behavior dashboard (class-wise overview)
- [ ] Identify students with declining behavior trends
- [ ] View school-wide behavior statistics

### Parent
- [ ] Receive weekly behavior summary for my child
- [ ] View my child's behavior history and score
- [ ] See behavior trend (improving/declining)

### System
- [ ] Calculate behavior score per student per term
- [ ] Run AI pattern analysis on behavior incidents
- [ ] Send weekly behavior summary to parents
- [ ] Award badges in Vidya Passport for positive behaviors
- [ ] Generate admin dashboard data

---

## 5. Business Rules

### BR-001
**Rule:** Only teachers can log behavior incidents.
**Enforcement:** Incident logging endpoint requires teacher role + JWT auth. Teacher must be assigned to the student's class.

### BR-002
**Rule:** Points range: +1 to +5 (positive), -1 to -5 (negative).
**Enforcement:** Points validated against incident_type. Positive: 1-5. Negative: -5 to -1. Points outside range rejected.

### BR-003
**Rule:** Behavior score is per term.
**Enforcement:** Score calculated per term (term1, term2, term3). Term determined from incident_date. Score = sum of all incident points in term.

### BR-004
**Rule:** Categories are school-specific or global.
**Enforcement:** `behavior_categories.school_id` = null for global defaults. School-specific categories have school_id. School sees both global and own categories.

### BR-005
**Rule:** Categories can be deactivated but not deleted.
**Enforcement:** `is_active = false` deactivates category. Deactivated categories not shown in dropdown. Existing incidents retain category name.

### BR-006
**Rule:** Weekly summary sent every Monday.
**Enforcement:** Background job runs every Monday 8 AM. Sends summary for previous week (Mon-Sun). FCM + in-app notification to parents.

### BR-007
**Rule:** Pattern analysis uses AI infrastructure.
**Enforcement:** AI analysis uses `AI_INFRASTRUCTURE_SPEC.md` LLM. Analyzes incident history per student. Results cached (1-hour TTL).

### BR-008
**Rule:** Positive behaviors trigger badge awards.
**Enforcement:** Configurable rules: N positive incidents of category X → badge Y. Badge award via `VIDYA_PASSPORT_SPEC.md` integration. Checked on each new positive incident.

### BR-009
**Rule:** Behavior incidents are school-scoped.
**Enforcement:** All incidents have `school_id`. Queries filtered by school_id. No cross-school visibility.

### BR-010
**Rule:** Teacher can view own class students' behavior.
**Enforcement:** Teacher sees behavior for students in classes they teach. Admin sees all school students. Parent sees own child only.

---

## 6. Database Design

### 6.1 Entity Relationship Summary

Two new tables: `behavior_incidents` (incident records) and `behavior_categories` (configurable categories). References existing `students`, `app_users`, `classes`, and `schools` tables.

### 6.2 New Tables

#### `behavior_incidents` table

```sql
CREATE TABLE behavior_incidents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    student_id      UUID NOT NULL,
    teacher_id      UUID NOT NULL,
    teacher_name    TEXT NOT NULL,
    incident_type   VARCHAR(16) NOT NULL,          -- positive | negative
    category        VARCHAR(32) NOT NULL,          -- helpful | leadership | disruptive | late | etc.
    points          INTEGER NOT NULL,              -- +1 to +5 or -1 to -5
    description     TEXT,
    incident_date   DATE NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_behavior_student ON behavior_incidents(student_id, incident_date DESC);
CREATE INDEX idx_behavior_school_date ON behavior_incidents(school_id, incident_date DESC);
```

#### `behavior_categories` table

```sql
CREATE TABLE behavior_categories (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID,                          -- null = global default
    name            VARCHAR(32) NOT NULL,
    incident_type   VARCHAR(16) NOT NULL,          -- positive | negative
    default_points  INTEGER NOT NULL,
    color           VARCHAR(8),
    is_active       BOOLEAN NOT NULL DEFAULT true
);
```

### 6.3 Modified Tables

N/A — no existing tables modified. Integration with `TeacherObservationsTable` is read-only (behavior field used as reference).

### 6.4 Indexes

- `idx_behavior_student` on `behavior_incidents(student_id, incident_date DESC)` — student history lookup
- `idx_behavior_school_date` on `behavior_incidents(school_id, incident_date DESC)` — school dashboard lookup
- `behavior_categories(school_id)` — for school-specific category lookup (recommended)

### 6.5 Constraints

- `behavior_incidents.incident_type` — NOT NULL, values: positive | negative
- `behavior_incidents.category` — NOT NULL
- `behavior_incidents.points` — NOT NULL, range: +1 to +5 (positive), -1 to -5 (negative)
- `behavior_incidents.incident_date` — NOT NULL
- `behavior_incidents.teacher_name` — NOT NULL
- `behavior_categories.name` — NOT NULL, max 32 chars
- `behavior_categories.incident_type` — NOT NULL, values: positive | negative
- `behavior_categories.default_points` — NOT NULL
- `behavior_categories.is_active` — NOT NULL, default true

### 6.6 Foreign Keys

- `behavior_incidents.school_id` → `schools.id` (implicit)
- `behavior_incidents.student_id` → `students.id` (implicit)
- `behavior_incidents.teacher_id` → `app_users.id` (implicit)
- `behavior_categories.school_id` → `schools.id` (implicit, nullable for global)

### 6.7 Soft Delete Strategy

- `behavior_incidents`: N/A — incidents are not deleted. They are historical records.
- `behavior_categories`: Soft delete via `is_active = false`. Deactivated categories not shown in dropdown but existing incidents retain category name.

### 6.8 Audit Fields

- `behavior_incidents.created_at` — when incident was logged
- `behavior_incidents.teacher_id`, `teacher_name` — who logged the incident
- `behavior_incidents.incident_date` — when the incident occurred (may differ from created_at)
- `behavior_categories.is_active` — active flag

### 6.9 Migration Notes

Migration: `docs/db/migration_088_behavior_tracking.sql`
- CREATE 2 tables: `behavior_incidents`, `behavior_categories`
- CREATE 2 indexes: `idx_behavior_student`, `idx_behavior_school_date`
- INSERT seed data: default behavior categories (8 categories: 4 positive, 4 negative)

### 6.10 Exposed Mappings

```kotlin
object BehaviorIncidentsTable : UUIDTable("behavior_incidents", "id") {
    val schoolId     = uuid("school_id")
    val studentId    = uuid("student_id")
    val teacherId    = uuid("teacher_id")
    val teacherName  = text("teacher_name")
    val incidentType = varchar("incident_type", 16)
    val category     = varchar("category", 32)
    val points       = integer("points")
    val description  = text("description").nullable()
    val incidentDate = date("incident_date")
    val createdAt    = timestamp("created_at")

    init {
        index("idx_behavior_student", studentId, incidentDate)
        index("idx_behavior_school_date", schoolId, incidentDate)
    }
}

object BehaviorCategoriesTable : UUIDTable("behavior_categories", "id") {
    val schoolId     = uuid("school_id").nullable()  // null = global
    val name         = varchar("name", 32)
    val incidentType = varchar("incident_type", 16)
    val defaultPoints= integer("default_points")
    val color        = varchar("color", 8).nullable()
    val isActive     = bool("is_active").default(true)
}
```

Register in `DatabaseFactory.allTables`.

### 6.11 Seed Data

Default behavior categories (global, school_id = null):

| name | incident_type | default_points | color |
|---|---|---|---|
| helpful | positive | +2 | #4CAF50 |
| leadership | positive | +3 | #2196F3 |
| improvement | positive | +2 | #8BC34A |
| participation | positive | +1 | #00BCD4 |
| disruptive | negative | -2 | #FF5722 |
| late | negative | -1 | #FF9800 |
| disrespectful | negative | -3 | #F44336 |
| bullying | negative | -5 | #E91E63 |

---

## 7. State Machines

### Behavior Incident State Machine

```
not_logged ──teacher_logs──> logged ──(terminal)
  │
  └──logged ──teacher_edits──> edited ──(terminal)
  │
  └──logged/edited ──admin_reviews──> reviewed ──(terminal)
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `not_logged` | Teacher logs incident | `logged` | Incident saved to DB |
| `logged` | Teacher edits incident | `edited` | Update description, points, category |
| `logged`/`edited` | Admin reviews | `reviewed` | Admin views and acknowledges |

Note: Behavior incidents are immutable once logged (except description can be edited by the logging teacher within 24 hours). No deletion.

### Behavior Score State Machine

```
no_incidents ──incident_logged──> scored ──new_incident──> re-scored
  │                                  │
  │                                  └──term_reset──> no_incidents (new term)
  │
  └──scored ──term_reset──> no_incidents (new term, score resets)
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `no_incidents` | Incident logged | `scored` | Score = incident points |
| `scored` | New incident logged | `scored` | Score += incident points |
| `scored` | Term reset (new term starts) | `no_incidents` | Score resets to 0 for new term |

### Category Lifecycle State Machine

```
not_created ──admin_creates──> active ──admin_deactivates──> inactive
  │                                  │
  │                                  └──admin_reactivates──> active
  │
  └──inactive ──admin_reactivates──> active
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `not_created` | Admin creates category | `active` | Category saved with is_active = true |
| `active` | Admin deactivates | `inactive` | is_active = false, not shown in dropdown |
| `inactive` | Admin reactivates | `active` | is_active = true, shown in dropdown |

---

## 8. Backend Architecture

### 8.1 Component Overview

`BehaviorService` handles incident logging, score calculation, pattern analysis, and weekly summaries. Integrates with AI infrastructure for pattern analysis and Vidya Passport for badge awards. `BehaviorSummaryJob` sends weekly parent summaries.

### 8.2 Design Principles

1. **Positive reinforcement focus** — positive categories and points encourage good behavior
2. **Teacher-driven** — only teachers log incidents (not automated)
3. **Configurable** — categories and points configurable per school
4. **AI-enhanced** — pattern analysis identifies trends and recurring behaviors
5. **Parent-transparent** — weekly summaries keep parents informed
6. **School-scoped** — all data school-specific

### 8.3 Core Types

#### BehaviorService

```kotlin
class BehaviorService {
    suspend fun logIncident(request: IncidentRequest): BehaviorIncidentDto
    suspend fun getStudentHistory(studentId: UUID, term: String): List<BehaviorIncidentDto>
    suspend fun getStudentScore(studentId: UUID, term: String): Int
    suspend fun getPatternAnalysis(studentId: UUID): PatternAnalysisDto
    suspend fun getClassOverview(classId: UUID, term: String): ClassBehaviorDto
    suspend fun getDashboard(schoolId: UUID): BehaviorDashboardDto
    suspend fun getCategories(schoolId: UUID): List<BehaviorCategoryDto>
    suspend fun createCategory(request: CategoryRequest): BehaviorCategoryDto
    suspend fun updateCategory(id: UUID, request: CategoryRequest): BehaviorCategoryDto
    suspend fun deactivateCategory(id: UUID): Unit
}
```

#### Pattern Analysis (AI)

```kotlin
data class PatternAnalysisDto(
    val mostFrequentCategory: String,
    val trend: String,           // improving | declining | stable
    val recurringPatterns: List<String>,
    val summary: String,         // AI-generated natural language summary
    val recommendations: List<String>,
)
```

### 8.4 Repositories

- `BehaviorIncidentRepository` — CRUD for `behavior_incidents`
- `BehaviorCategoryRepository` — CRUD for `behavior_categories`

### 8.5 Mappers

- `BehaviorIncidentMapper` — maps `behavior_incidents` rows to DTOs
- `BehaviorCategoryMapper` — maps `behavior_categories` rows to DTOs

### 8.6 Permission Checks

- Log incident: teacher role + JWT auth + teacher assigned to student's class
- View student history: teacher (own class), school admin, or parent (own child)
- View score: teacher (own class), school admin, or parent (own child)
- View pattern analysis: teacher (own class) or school admin
- View dashboard: school admin
- Manage categories: school admin
- View class overview: teacher (own class) or school admin

### 8.7 Background Jobs

- **Behavior Summary Job** — every Monday 8 AM
  1. For each student with incidents in the previous week (Mon-Sun):
  2. Compile weekly summary: incidents, score change, top category
  3. Send FCM + in-app notification to parent
  4. "Weekly behavior summary for {student_name}: {summary}"

### 8.8 Domain Events

- `BehaviorIncidentLogged` — emitted when incident is logged
- `BehaviorScoreUpdated` — emitted when score is recalculated
- `PatternAnalysisCompleted` — emitted when AI analysis completes
- `WeeklyBehaviorSummarySent` — emitted when weekly summary is sent
- `BadgeAwarded` — emitted when positive behaviors trigger badge (via Vidya Passport)

### 8.9 Caching

- Pattern analysis: cached (1-hour TTL, AI analysis is expensive)
- Categories: cached (10-minute TTL, rarely changes)
- Student score: not cached (calculated on demand, fast sum query)
- Dashboard: cached (5-minute TTL)

### 8.10 Transactions

- Log incident: single transaction (insert incident, check badge rules, award badge if applicable)
- Create/update category: single transaction
- Score calculation: read-only (sum query, no transaction needed)

### 8.11 Rate Limiting

- Log incident: 50 per hour per teacher (class of ~40 students)
- Pattern analysis: 10 per hour per teacher (AI is expensive)
- Category management: 20 per hour per admin

### 8.12 Configuration

- `BEHAVIOR_TRACKING_ENABLED` — default `false`; enable/disable feature
- `BEHAVIOR_AI_ANALYSIS_ENABLED` — default `true`; enable/disable AI pattern analysis
- `BEHAVIOR_WEEKLY_SUMMARY_ENABLED` — default `true`; enable/disable weekly parent summary
- `BEHAVIOR_BADGE_INTEGRATION_ENABLED` — default `true`; enable/disable Vidya Passport badge integration
- `BEHAVIOR_POINTS_MIN_POSITIVE` — default `1`; min positive points
- `BEHAVIOR_POINTS_MAX_POSITIVE` — default `5`; max positive points
- `BEHAVIOR_POINTS_MIN_NEGATIVE` — default `-5`; min negative points
- `BEHAVIOR_POINTS_MAX_NEGATIVE` — default `-1`; max negative points

---

## 9. API Contracts

### 9.1 Teacher Endpoints

```
POST /api/v1/teacher/behavior/incident
  Body: { student_id, incident_type, category, points, description, date }
  → 201: BehaviorIncidentDto
  → 400: Invalid points range

GET /api/v1/teacher/behavior/student/{studentId}?term=term1
  → 200: { incidents: [BehaviorIncidentDto], score: Int }

GET /api/v1/teacher/behavior/student/{studentId}/patterns
  → 200: PatternAnalysisDto

GET /api/v1/teacher/behavior/class/{classId}?term=term1
  → 200: ClassBehaviorDto
```

### 9.2 Admin Endpoints

```
GET /api/v1/school/behavior/dashboard
  → 200: BehaviorDashboardDto

GET /api/v1/school/behavior/categories
  → 200: { categories: [BehaviorCategoryDto] }

POST /api/v1/school/behavior/categories
  Body: { name, incident_type, default_points, color }
  → 201: BehaviorCategoryDto

PUT /api/v1/school/behavior/categories/{id}
  Body: { name, default_points, color, is_active }
  → 200: BehaviorCategoryDto
```

### 9.3 Parent Endpoints

```
GET /api/v1/parent/behavior/{childId}?term=term1
  → 200: { incidents: [BehaviorIncidentDto], score: Int, trend: String }
```

### 9.4 DTO Models

All `@Serializable`, wrapped in `ApiResponse<T>` pattern.

```kotlin
@Serializable data class BehaviorIncidentDto(
    val id: String,
    val studentId: String,
    val studentName: String,
    val teacherId: String,
    val teacherName: String,
    val incidentType: String,    // positive | negative
    val category: String,
    val points: Int,
    val description: String?,
    val incidentDate: String,
    val createdAt: String,
)

@Serializable data class BehaviorCategoryDto(
    val id: String,
    val name: String,
    val incidentType: String,
    val defaultPoints: Int,
    val color: String?,
    val isActive: Boolean,
)

@Serializable data class PatternAnalysisDto(
    val mostFrequentCategory: String,
    val trend: String,
    val recurringPatterns: List<String>,
    val summary: String,
    val recommendations: List<String>,
)

@Serializable data class ClassBehaviorDto(
    val classId: String,
    val className: String,
    val totalScore: Int,
    val studentCount: Int,
    val topPositiveCategory: String,
    val topNegativeCategory: String,
    val studentsNeedingAttention: List<String>,
)

@Serializable data class BehaviorDashboardDto(
    val schoolId: String,
    val totalIncidents: Int,
    val positiveCount: Int,
    val negativeCount: Int,
    val classOverviews: List<ClassBehaviorDto>,
    val schoolTrend: String,
)

@Serializable data class IncidentRequest(
    val studentId: String,
    val incidentType: String,
    val category: String,
    val points: Int,
    val description: String?,
    val date: String,
)

@Serializable data class CategoryRequest(
    val name: String,
    val incidentType: String,
    val defaultPoints: Int,
    val color: String?,
)
```

---

## 10. Frontend Architecture

### 10.1 Screens

| Screen | Platform | Role | Description |
|---|---|---|---|
| `BehaviorLogScreen` | Compose | Teacher | Log behavior incidents, view student history |
| `BehaviorDashboardScreen` | Compose | Admin | School-wide behavior dashboard |
| `BehaviorCategoryScreen` | Compose | Admin | Manage behavior categories |
| `BehaviorSummaryScreen` | Compose | Parent | View child's behavior summary and history |

### 10.2 Navigation

- Teacher: Student → Behavior → Log Incident / View History / Pattern Analysis
- Admin: Dashboard → Behavior → Overview / Categories
- Parent: Home → Behavior → Summary / History

### 10.3 UX Flows

#### Teacher: Log Behavior Incident
1. Teacher opens student profile → taps "Log Behavior"
2. Selects incident type: Positive (green) or Negative (red)
3. Selects category from dropdown (filtered by incident type)
4. Points auto-filled from category default (adjustable within range)
5. Enters description (optional)
6. Selects date (defaults to today)
7. Taps "Save" → incident logged, score updated
8. If positive → badge check (may award badge)

#### Admin: View Dashboard
1. Admin opens Behavior Dashboard
2. Sees school-wide stats: total incidents, positive/negative ratio, trend
3. Class-wise overview: scores, top categories, students needing attention
4. Taps class for detailed view
5. Can manage categories from dashboard

#### Parent: View Behavior Summary
1. Parent receives weekly summary notification (Monday 8 AM)
2. Opens Behavior section in app
3. Sees: current term score, trend (improving/declining), recent incidents
4. Can view full history by term

### 10.4 State Management

```kotlin
data class BehaviorLogState(
    val selectedStudent: StudentDto?,
    val incidentType: String?,        // positive | negative
    val selectedCategory: BehaviorCategoryDto?,
    val points: Int,
    val description: String,
    val selectedDate: LocalDate,
    val isSaving: Boolean,
    val error: String?,
)

data class BehaviorDashboardState(
    val dashboard: BehaviorDashboardDto?,
    val isLoading: Boolean,
    val error: String?,
)

data class BehaviorSummaryState(
    val childId: String,
    val score: Int,
    val trend: String,
    val incidents: List<BehaviorIncidentDto>,
    val isLoading: Boolean,
    val error: String?,
)
```

### 10.5 Offline Support

- Incident logging: available offline (queued, synced when online)
- Student history: cached locally (5-minute TTL)
- Dashboard: requires online (aggregation query)
- Pattern analysis: requires online (AI call)

### 10.6 Loading States

- Logging: "Saving behavior incident..."
- Loading history: "Loading behavior history..."
- Pattern analysis: "Analyzing behavior patterns..." (may take 10 seconds)
- Dashboard: "Loading behavior dashboard..."

### 10.7 Error Handling (UI)

- Invalid points: "Points must be between +1 and +5 for positive incidents."
- Category not found: "This category is no longer active. Please select another."
- No incidents: "No behavior incidents recorded for this term."
- Pattern analysis failed: "Unable to analyze patterns. Please try again later."
- Permission denied: "You can only log behavior for students in your class."

### 10.8 Component Integration Guidelines

| Rule | Description |
|---|---|
| **R1** | Incident type selector: green for positive, red for negative |
| **R2** | Category dropdown: filtered by incident type, shows color dot |
| **R3** | Points input: auto-filled, adjustable within range, validated |
| **R4** | History list: chronological, color-coded (green/red), with points |
| **R5** | Score display: prominent, color-coded (green if positive, red if negative) |
| **R6** | Trend indicator: arrow up (improving), down (declining), flat (stable) |
| **R7** | Dashboard: bar charts for class-wise comparison |
| **R8** | Students needing attention: highlighted in amber on dashboard |
| **R9** | Category management: add/edit/deactivate with color picker |
| **R10** | Weekly summary: card with score, trend, top category, recent incidents |

---

## 11. Shared Module Changes (KMP)

### 11.1 DTOs

All DTOs defined in section 9.4, placed in `shared/.../behavior/domain/model/BehaviorModels.kt`.

### 11.2 Domain Models

```kotlin
data class BehaviorIncident(
    val id: String,
    val schoolId: String,
    val studentId: String,
    val teacherId: String,
    val teacherName: String,
    val incidentType: IncidentType,
    val category: String,
    val points: Int,
    val description: String?,
    val incidentDate: LocalDate,
    val createdAt: Instant,
)

data class BehaviorCategory(
    val id: String,
    val schoolId: String?,
    val name: String,
    val incidentType: IncidentType,
    val defaultPoints: Int,
    val color: String?,
    val isActive: Boolean,
)

enum class IncidentType { POSITIVE, NEGATIVE }
enum class BehaviorTrend { IMPROVING, DECLINING, STABLE }
```

### 11.3 Repository Interfaces

```kotlin
interface BehaviorRepository {
    suspend fun logIncident(token: String, request: IncidentRequest): NetworkResult<BehaviorIncidentDto>
    suspend fun getStudentHistory(token: String, studentId: String, term: String): NetworkResult<List<BehaviorIncidentDto>>
    suspend fun getStudentScore(token: String, studentId: String, term: String): NetworkResult<Int>
    suspend fun getPatternAnalysis(token: String, studentId: String): NetworkResult<PatternAnalysisDto>
    suspend fun getClassOverview(token: String, classId: String, term: String): NetworkResult<ClassBehaviorDto>
    suspend fun getDashboard(token: String): NetworkResult<BehaviorDashboardDto>
    suspend fun getCategories(token: String): NetworkResult<List<BehaviorCategoryDto>>
    suspend fun createCategory(token: String, request: CategoryRequest): NetworkResult<BehaviorCategoryDto>
    suspend fun updateCategory(token: String, id: String, request: CategoryRequest): NetworkResult<BehaviorCategoryDto>
    suspend fun getParentBehavior(token: String, childId: String, term: String): NetworkResult<BehaviorSummaryDto>
}
```

### 11.4 UseCases

- `LogBehaviorIncidentUseCase`
- `GetStudentBehaviorHistoryUseCase`
- `GetStudentBehaviorScoreUseCase`
- `GetPatternAnalysisUseCase`
- `GetClassBehaviorOverviewUseCase`
- `GetBehaviorDashboardUseCase`
- `GetBehaviorCategoriesUseCase`
- `ManageBehaviorCategoryUseCase`
- `GetParentBehaviorSummaryUseCase`

### 11.5 Validation

- `student_id`: valid UUID
- `incident_type`: "positive" or "negative"
- `category`: non-empty, must exist in `behavior_categories`
- `points`: integer, range +1 to +5 (positive) or -5 to -1 (negative)
- `description`: max 500 characters
- `date`: valid date, not in future
- `name` (category): non-empty, max 32 characters
- `default_points`: integer, range +1 to +5 (positive) or -5 to -1 (negative)

### 11.6 Serialization

Standard Kotlinx serialization. Enums serialized as lowercase strings. Dates as ISO strings.

### 11.7 Network APIs

Ktor `@Resource` route definitions in `BehaviorApi.kt`:
- POST `/api/v1/teacher/behavior/incident`
- GET `/api/v1/teacher/behavior/student/{studentId}`
- GET `/api/v1/teacher/behavior/student/{studentId}/patterns`
- GET `/api/v1/teacher/behavior/class/{classId}`
- GET `/api/v1/school/behavior/dashboard`
- GET `/api/v1/school/behavior/categories`
- POST `/api/v1/school/behavior/categories`
- PUT `/api/v1/school/behavior/categories/{id}`
- GET `/api/v1/parent/behavior/{childId}`

### 11.8 Database Models (Local Cache)

- Categories: cached locally (10-minute TTL)
- Student history: cached locally (5-minute TTL)
- Pattern analysis: cached locally (1-hour TTL)
- Dashboard: not cached (requires online)

---

## 12. Permissions Matrix

| Action | Super Admin | School Admin | Counselor | Teacher | Parent |
|---|---|---|---|---|---|
| Log behavior incident | N/A | ✅ (own school) | ❌ | ✅ (own class) | ❌ |
| View student history | ✅ (all) | ✅ (own school) | ✅ (assigned) | ✅ (own class) | ✅ (own child) |
| View student score | ✅ (all) | ✅ (own school) | ✅ (assigned) | ✅ (own class) | ✅ (own child) |
| View pattern analysis | ✅ (all) | ✅ (own school) | ✅ (assigned) | ✅ (own class) | ❌ |
| View class overview | ✅ (all) | ✅ (own school) | ❌ | ✅ (own class) | ❌ |
| View dashboard | ✅ (all) | ✅ (own school) | ❌ | ❌ | ❌ |
| Manage categories | ✅ (all) | ✅ (own school) | ❌ | ❌ | ❌ |
| View weekly summary | N/A | N/A | N/A | ❌ | ✅ (own child) |

---

## 13. Notifications

### Behavior Notifications

| Trigger | Recipient | Channel | Template |
|---|---|---|---|
| Weekly summary (Monday 8 AM) | Parent | FCM (normal) + in-app | "Weekly behavior summary for {student_name}: Score {score}, {trend}. {top_category} this week." |
| Badge awarded (positive behavior) | Parent | FCM (normal) + in-app | "{student_name} earned the '{badge_name}' badge for positive behavior!" |
| Student needs attention (declining trend) | Teacher + Admin | In-app | "{student_name} shows declining behavior trend. Consider intervention." |

### Notification Integration

Uses `SmartNotificationService` with "normal" priority for weekly summaries and badge awards. Uses "high" priority for declining trend alerts to teachers.

---

## 14. Background Jobs

### Behavior Summary Job

| Property | Value |
|---|---|
| **Name** | `BehaviorSummaryJob` |
| **Schedule** | Every Monday 8 AM |
| **Duration** | < 30 seconds |
| **Retry** | None (next week's job handles it) |

#### Job Flow

1. For each student with incidents in the previous week (Mon-Sun):
2. Compile weekly summary: incidents, score change, top category
3. Send FCM + in-app notification to parent
4. "Weekly behavior summary for {student_name}: {summary}"
5. Return count of summaries sent

### Badge Check (Inline)

Badge check runs inline on each positive incident (not a background job):
1. Check badge rules for student's positive incident count per category
2. If threshold met → award badge via `VIDYA_PASSPORT_SPEC.md`
3. Send badge notification to parent

---

## 15. Integrations

### Internal Integrations

| System | Integration Point | Direction | Protocol | Error Handling |
|---|---|---|---|---|
| `STUDENT_WELLNESS_SPEC.md` | Teacher observations | Read | Direct DB | Optional reference |
| `VIDYA_PASSPORT_SPEC.md` | Badge awards | Call | Direct call | Log error, badge not awarded |
| `AI_INFRASTRUCTURE_SPEC.md` | Pattern analysis | Call | LLM API | Return "analysis unavailable" |
| `StudentsTable` | Student info | Read | Direct DB | Required |
| `ClassesTable` | Class info | Read | Direct DB | Required |
| `AppUsersTable` | Teacher info | Read | Direct DB | Required |
| `Notify.kt` | Notification dispatch | Call | Direct call | Log error, continue |
| `SmartNotificationService` | Notification priority | Call | Direct call | Log error, continue |

### External Integrations

| System | Integration Point | Direction | Protocol | Error Handling |
|---|---|---|---|---|
| AI/LLM provider | Pattern analysis | Call | HTTP API | Return "analysis unavailable" |
| FCM | Push notifications | Call | HTTP API | Log error, continue |

### Integration Patterns

- **AI pattern analysis:** `aiService.analyze(incidents)` → LLM processes incident history → returns patterns, trends, recommendations
- **Badge award:** On positive incident → check badge rules → `passportService.awardBadge(studentId, badgeId)` if threshold met
- **Weekly summary:** `BehaviorSummaryJob` → compile summary → `Notify.kt` dispatch → FCM + in-app

---

## 16. Security

### Authentication

- Teacher endpoints: teacher role + JWT auth via `requireAuth()`
- Admin endpoints: school admin role + JWT auth
- Parent endpoints: parent role + JWT auth + parent-student relationship verified

### Authorization

- Log incident: teacher (own class) or school admin
- View history/score: teacher (own class), school admin, counselor (assigned), parent (own child)
- Pattern analysis: teacher (own class) or school admin
- Dashboard: school admin only
- Categories: school admin only
- Weekly summary: parent (own child) only

### Data Protection

- Behavior data is school-scoped
- Parent sees only own child's behavior
- Teacher sees only own class students' behavior
- Pattern analysis results not shared with parents (teacher/admin only)

### Input Validation

- `student_id`: valid UUID
- `incident_type`: "positive" or "negative"
- `category`: non-empty, must exist and be active
- `points`: integer, range validated against incident_type
- `description`: max 500 characters
- `date`: valid date, not in future
- `name` (category): non-empty, max 32 characters

### Rate Limiting

- Log incident: 50 per hour per teacher
- Pattern analysis: 10 per hour per teacher (AI is expensive)
- Category management: 20 per hour per admin

### Audit Logging

- Incident logged: teacher ID, student ID, category, points, timestamp
- Incident edited: teacher ID, incident ID, changes, timestamp
- Category created/updated/deactivated: admin ID, category ID, changes, timestamp
- Weekly summary sent: student ID, parent ID, timestamp
- Badge awarded: student ID, badge ID, trigger incident ID, timestamp

### PII Handling

- Student name and teacher name in incidents (necessary for identification)
- Behavior descriptions may contain sensitive info about student conduct
- Pattern analysis results may contain sensitive behavioral assessments
- Parent sees own child's data only
- No external sharing of behavior data

### Multi-tenant Isolation

- All incidents have `school_id` — school-scoped
- Categories have `school_id` (nullable for global defaults)
- Queries filtered by `school_id`
- No cross-school behavior visibility

---

## 17. Performance & Scalability

### Expected Scale

- Incidents: ~10-20 per day per school (1-2 per teacher per day)
- Students: ~200-500 per school
- Categories: ~8-16 per school (default + custom)
- Score calculation: sum query, < 100ms
- Pattern analysis: AI call, < 10 seconds

### Query Optimization

- Student history: `idx_behavior_student(student_id, incident_date DESC)` — filtered by student, sorted by date
- Dashboard: `idx_behavior_school_date(school_id, incident_date DESC)` — filtered by school, sorted by date
- Score: `SELECT SUM(points) FROM behavior_incidents WHERE student_id = ? AND incident_date BETWEEN ? AND ?` — fast sum

### Indexing Strategy

- `behavior_incidents(student_id, incident_date DESC)` — student history
- `behavior_incidents(school_id, incident_date DESC)` — school dashboard
- `behavior_categories(school_id)` — category lookup (recommended)

### Caching Strategy

- Pattern analysis: cached (1-hour TTL, AI is expensive)
- Categories: cached (10-minute TTL)
- Dashboard: cached (5-minute TTL)
- Student score: not cached (fast sum query)

### Pagination

- Student history: 20 per page
- Dashboard class list: all in single response (max ~20 classes)

### Connection Pooling

Uses existing HikariCP connection pool. No additional pooling needed.

### Async Processing

- Incident logging: synchronous (teacher waits for confirmation)
- Pattern analysis: synchronous (teacher waits, < 10 seconds)
- Weekly summary: async (background job)
- Badge check: synchronous (inline, fast)

### Scalability Concerns

- DB storage: ~20 incidents/day × ~1KB = ~20KB/day. Negligible.
- AI calls: ~10 per hour max. LLM handles this easily.
- Weekly summary job: ~500 students × 1 notification = ~500 FCM calls. < 30 seconds.
- Dashboard query: aggregation over ~500 incidents. < 3 seconds with index.

---

## 18. Edge Cases

| # | Scenario | Expected Behavior |
|---|---|---|
| EC-1 | Student with no incidents | Score = 0. History empty. "No incidents recorded." |
| EC-2 | All positive incidents | High positive score. Trend: improving. Badge awards likely. |
| EC-3 | All negative incidents | Negative score. Trend: declining. Alert to teacher/admin. |
| EC-4 | Category deactivated after incidents logged | Existing incidents retain category name. New incidents can't use deactivated category. |
| EC-5 | Teacher logs incident for student not in their class | Return 403. "You can only log behavior for students in your class." |
| EC-6 | Points outside allowed range | Return 400. "Points must be between +1 and +5 for positive incidents." |
| EC-7 | AI pattern analysis fails | Return "Analysis unavailable. Please try again later." |
| EC-8 | Parent views behavior for child with no incidents | Score = 0. "No behavior incidents recorded for this term." |
| EC-9 | Term reset (new term) | Score resets to 0. Previous term incidents still viewable by selecting term. |
| EC-10 | Duplicate incident (same student, category, date) | Allowed (teacher may log multiple incidents of same type on same day). |
| EC-11 | Badge rule threshold met multiple times | Badge awarded once. Subsequent triggers ignored (idempotent). |
| EC-12 | Weekly summary with no incidents in week | Summary not sent (no data to report). |

---

## 19. Error Handling

### Error Response Format

Standard `ApiResponse` error format.

### Error Codes

| Code | HTTP Status | Description | User Message |
|---|---|---|---|
| `INVALID_POINTS_RANGE` | 400 | Points outside allowed range | "Points must be between +1 and +5 for positive, -1 to -5 for negative." |
| `CATEGORY_NOT_FOUND` | 404 | Category not found or inactive | "This category is no longer active. Please select another." |
| `CATEGORY_ALREADY_EXISTS` | 409 | Category name already exists | "A category with this name already exists." |
| `STUDENT_NOT_IN_CLASS` | 403 | Teacher not assigned to student's class | "You can only log behavior for students in your class." |
| `PATTERN_ANALYSIS_FAILED` | 500 | AI analysis failed | "Unable to analyze patterns. Please try again later." |
| `NOT_AUTHORIZED` | 403 | Role not authorized | "You are not authorized to perform this action." |
| `NOT_PARENT_OF_STUDENT` | 403 | Parent not linked to student | "You can only view behavior for your own children." |
| `DATE_IN_FUTURE` | 400 | Incident date is in the future | "Incident date cannot be in the future." |

### Error Handling Strategy

- **Invalid points:** Return 400. Teacher adjusts points.
- **Category inactive:** Return 404. Teacher selects active category.
- **AI failure:** Return 500. Teacher can retry later.
- **Permission denied:** Return 403. User informed of scope.
- **Badge award failure:** Log error, continue. Badge not awarded but incident saved.

### Retry Strategy

- Incident logging: teacher retries (usually no retry needed)
- Pattern analysis: teacher retries (AI may succeed on retry)
- Weekly summary: no retry (next week's job handles it)

### Fallback Behavior

- AI unavailable: pattern analysis returns "unavailable." Teacher uses manual review.
- Badge integration unavailable: incident saved, badge not awarded. Log error.
- Notification fails: incident saved, parent may miss summary.

---

## 20. Analytics & Reporting

### Analytics Dashboard Data

| Metric | Source | Derivation |
|---|---|---|
| Total incidents | `behavior_incidents` | Count per school per term |
| Positive/negative ratio | `behavior_incidents` | positive_count / total_count |
| Average score per student | `behavior_incidents` | avg(score) per student per term |
| Top positive category | `behavior_incidents` | Most frequent positive category |
| Top negative category | `behavior_incidents` | Most frequent negative category |
| Students with declining trend | Pattern analysis | Count of students with declining trend |
| Class-wise comparison | `behavior_incidents` | avg(score) per class |
| Badge awards | Badge integration | Count of behavior-triggered badges |

### Export Capabilities

- Behavior incident report (CSV) — student, category, points, date, teacher
- Behavior score report (CSV) — student, term, score, trend
- Class behavior overview (CSV) — class, avg score, top categories

### Report Types

| Report | Format | Frequency | Recipient |
|---|---|---|---|
| Behavior overview | JSON (API) | Monthly | School Admin |
| Pattern analysis summary | JSON (API) | Monthly | School Admin |
| Badge awards report | JSON (API) | Monthly | Product Team |

---

## 21. Testing Strategy

### Unit Tests

- `BehaviorService.logIncident()` — validation, insert, badge check
- `BehaviorService.getStudentScore()` — sum calculation, term filtering
- `BehaviorService.getPatternAnalysis()` — AI call, caching
- `BehaviorService.getDashboard()` — aggregation, class-wise
- Points validation: positive/negative range checks
- Category management: create, update, deactivate

### Integration Tests

- Full flow: log incident → verify score → verify history
- Badge integration: log positive incidents → verify badge awarded at threshold
- Weekly summary: log incidents → run job → verify notification sent
- Category management: create → log incident with category → deactivate → verify not in dropdown
- Permission checks: teacher-only logging, parent-only own child, admin-only dashboard
- Multi-tenant: verify school-scoped queries

### E2E Tests

- Teacher logs positive incident → parent sees in weekly summary → badge awarded
- Teacher logs negative incident → admin sees declining trend on dashboard
- Admin creates custom category → teacher uses it → parent sees in summary

### Performance Tests

- Score calculation: < 100ms for 500 incidents
- Dashboard: < 3 seconds for school-wide aggregation
- Pattern analysis: < 10 seconds per student
- Weekly summary job: < 30 seconds for 500 students

### Test Data

- 10 test students with varying behavior histories
- 1 test teacher with assigned class
- 1 test admin
- 1 test parent with child
- Default categories pre-seeded
- Mock AI service for pattern analysis
- Test badge rules

### Test Environment

- Test database with behavior tables
- Mock AI/LLM service (returns controlled pattern analysis)
- Mock Vidya Passport service (returns controlled badge awards)
- Test JWT tokens for all roles
- Test FCM tokens

---

## 22. Acceptance Criteria

- [ ] Teacher logs positive and negative behavior incidents
- [ ] Points system configurable per category
- [ ] Behavior score calculated per student per term
- [ ] AI pattern analysis identifies recurring behaviors
- [ ] Parent weekly summary shows behavior incidents + score
- [ ] Admin dashboard shows class-wise behavior overview
- [ ] Positive behaviors trigger badge awards in Vidya Passport
- [ ] Categories configurable (add, edit, deactivate)
- [ ] Trend indicator shows improving/declining/stable
- [ ] School-scoped (no cross-school access)
- [ ] Points range enforced (+1 to +5, -1 to -5)

---

## 23. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration `migration_088_behavior_tracking.sql`, Exposed tables, seed categories, register in `DatabaseFactory` |
| 2 | 2 days | `BehaviorService` (log, score, pattern analysis, dashboard, categories) |
| 3 | 1 day | API endpoints (teacher, admin, parent) |
| 4 | 3 days | Client UI: `BehaviorLogScreen`, `BehaviorDashboardScreen`, `BehaviorCategoryScreen`, `BehaviorSummaryScreen` |
| 5 | 1 day | Tests: unit, integration, E2E |

### Pre-Implementation Checklist

- [ ] Verify `STUDENT_WELLNESS_SPEC.md` is implemented (TeacherObservationsTable)
- [ ] Verify `AI_INFRASTRUCTURE_SPEC.md` is available for pattern analysis
- [ ] Verify `VIDYA_PASSPORT_SPEC.md` is implemented for badge integration
- [ ] Verify `StudentsTable`, `ClassesTable`, `AppUsersTable` are available
- [ ] Verify notification infrastructure (FCM, SmartNotificationService)
- [ ] Verify term system (term1, term2, term3) is defined

---

## 24. File-Level Impact Analysis

### Server

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | `BehaviorIncidentsTable`, `BehaviorCategoriesTable` |
| `server/.../db/DatabaseFactory.kt` | Modify | Register new tables in `allTables` |
| `server/.../feature/behavior/BehaviorService.kt` | **New** | Core service |
| `server/.../feature/behavior/BehaviorRouting.kt` | **New** | API endpoints |
| `server/.../feature/behavior/BehaviorSummaryJob.kt` | **New** | Weekly summary job |
| `docs/db/migration_088_behavior_tracking.sql` | **New** | DDL + seed data |

### Shared (KMP)

| File | Change Type | Description |
|---|---|---|
| `shared/.../behavior/domain/model/BehaviorModels.kt` | **New** | DTOs, domain models, enums |
| `shared/.../behavior/domain/repository/BehaviorRepository.kt` | **New** | Repository interface |
| `shared/.../behavior/data/remote/BehaviorApi.kt` | **New** | HTTP API definitions |

### Client (Compose)

| File | Change Type | Description |
|---|---|---|
| `composeApp/.../ui/v2/screens/teacher/BehaviorLogScreen.kt` | **New** | Incident logging + student history |
| `composeApp/.../ui/v2/screens/admin/BehaviorDashboardScreen.kt` | **New** | Admin dashboard |
| `composeApp/.../ui/v2/screens/admin/BehaviorCategoryScreen.kt` | **New** | Category management |
| `composeApp/.../ui/v2/screens/parent/BehaviorSummaryScreen.kt` | **New** | Parent weekly summary |

---

## 25. Future Enhancements

| # | Enhancement | Priority | Effort | Notes |
|---|---|---|---|---|
| F-1 | Behavior contracts | Medium | M | Structured improvement plans with goals |
| F-2 | Peer recognition | Low | M | Students award positive points to peers |
| F-3 | Behavior-based interventions | Medium | L | Automated intervention suggestions |
| F-4 | Behavior trends visualization | Medium | S | Charts and graphs for behavior over time |
| F-5 | Multi-term comparison | Low | S | Compare behavior across terms |
| F-6 | Behavior export for parents | Low | S | PDF report for parent-teacher meetings |
| F-7 | Counselor integration | Medium | M | Counselor can view and add notes to behavior |
| F-8 | Automated alerts | Medium | S | Real-time alert for severe negative behavior |
| F-9 | Behavior gamification | Low | M | Class-wide behavior challenges and rewards |
| F-10 | External behavior frameworks | Low | M | Integration with frameworks like PBIS |

---

## Appendix A: Sequence Diagrams

### A.1 Log Behavior Incident

```
Teacher (app)       Server              DB              AI/Passport
  │                    │                  │                │
  │  POST /behavior/   │                  │                │
  │  incident          │                  │                │
  │  {student_id,      │                  │                │
  │   type, category,  │                  │                │
  │   points, desc}    │                  │                │
  │  ───────────────>  │                  │                │
  │                    │──validate points─│                │
  │                    │  and category    │                │
  │                    │──insert incident>│                │
  │                    │←──success───────│                │
  │                    │                  │                │
  │                    │  if positive:    │                │
  │                    │──check badge────│─────────────────>│
  │                    │  rules           │                │
  │                    │←──badge awarded?────────────────│
  │                    │                  │                │
  │  ←──201: IncidentDto│                 │                │
  │  (+ badge info if   │                  │                │
  │     awarded)        │                  │                │
  │                    │                  │                │
```

### A.2 Pattern Analysis

```
Teacher (app)       Server              DB              AI/LLM
  │                    │                  │                │
  │  GET /behavior/    │                  │                │
  │  student/{id}/     │                  │                │
  │  patterns          │                  │                │
  │  ───────────────>  │                  │                │
  │                    │──check cache─────│                │
  │                    │←──miss──────────│                │
  │                    │                  │                │
  │                    │──get incidents──>│                │
  │                    │←──incident list──│                │
  │                    │                  │                │
  │                    │──AI analyze─────────────────────>│
  │                    │  (incidents)     │                │
  │                    │←──patterns + trends──────────────│
  │                    │                  │                │
  │                    │──cache result───│                │
  │  ←──200: Analysis──│                  │                │
  │  (most frequent,   │                  │                │
  │   trend, patterns, │                  │                │
  │   recommendations) │                  │                │
  │                    │                  │                │
```

### A.3 Weekly Summary Job

```
Server (job)        DB                  Notify.kt        FCM
  │                    │                    │              │
  │  (Monday 8 AM)     │                    │              │
  │ ──get students──>  │                    │              │
  │  with incidents    │                    │              │
  │  in past week      │                    │              │
  │←──student list────│                    │              │
  │                    │                    │              │
  │  for each student: │                    │              │
  │ ──get week's────>  │                    │              │
  │  incidents         │                    │              │
  │←──incidents───────│                    │              │
  │                    │                    │              │
  │  compile summary   │                    │              │
  │  (score, trend,    │                    │              │
  │   top category)    │                    │              │
  │                    │                    │              │
  │──send notification─────────────────────>│              │
  │                    │  (FCM + in-app)    │              │
  │                    │─────────────────────────────────>│
  │                    │                    │              │
  │  return count      │                    │              │
  │                    │                    │              │
```

---

## Appendix B: Domain Model / ER Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                    behavior_incidents (new)                           │
│  id (PK)                                                              │
│  school_id, student_id, teacher_id, teacher_name                      │
│  incident_type (positive|negative)                                    │
│  category, points (+1 to +5 or -1 to -5)                              │
│  description, incident_date                                           │
│  created_at                                                           │
│  INDEX: (student_id, incident_date DESC)                              │
│  INDEX: (school_id, incident_date DESC)                               │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│                  behavior_categories (new)                            │
│  id (PK)                                                              │
│  school_id (nullable, null = global)                                  │
│  name, incident_type (positive|negative)                              │
│  default_points, color                                                │
│  is_active                                                            │
└──────────────────────────────────────────────────────────────────────┘

Existing tables used:
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│ students         │  │ app_users        │  │ classes          │
│ (existing)       │  │ (existing)       │  │ (existing)       │
│ student info     │  │ teacher accounts │  │ class info       │
└──────────────────┘  └──────────────────┘  └──────────────────┘
┌──────────────────┐
│ teacher_observations│
│ (existing, wellness)│
│ behavior field     │
└──────────────────┘
```

---

## Appendix C: Event Flow

### Domain Events

| Event | Emitter | Consumers | Payload | Side Effects |
|---|---|---|---|---|
| `BehaviorIncidentLogged` | `BehaviorService.logIncident()` | Badge service | `studentId, incidentType, category, points` | Badge check |
| `BehaviorScoreUpdated` | Score calculation | None (logged) | `studentId, term, newScore` | Audit log |
| `PatternAnalysisCompleted` | `BehaviorService.getPatternAnalysis()` | None (logged) | `studentId, trend, patterns` | Cache result |
| `WeeklyBehaviorSummarySent` | `BehaviorSummaryJob` | None (logged) | `studentId, parentId, summary` | Audit log |
| `BadgeAwarded` | Badge integration | `NotificationService` | `studentId, badgeId, triggerIncidentId` | FCM to parent |

### Event Delivery Guarantees

- Incident logged: synchronous, badge check inline
- Score updated: fire-and-forget logging
- Pattern analysis: fire-and-forget logging, result cached
- Weekly summary: async (background job), fire-and-forget
- Badge awarded: synchronous, notification dispatch async

---

## Appendix D: Configuration

### Environment Variables

| Variable | Default | Description |
|---|---|---|
| `BEHAVIOR_TRACKING_ENABLED` | `false` | Enable/disable feature |
| `BEHAVIOR_AI_ANALYSIS_ENABLED` | `true` | Enable/disable AI pattern analysis |
| `BEHAVIOR_WEEKLY_SUMMARY_ENABLED` | `true` | Enable/disable weekly parent summary |
| `BEHAVIOR_BADGE_INTEGRATION_ENABLED` | `true` | Enable/disable Vidya Passport badge integration |
| `BEHAVIOR_POINTS_MIN_POSITIVE` | `1` | Min positive points |
| `BEHAVIOR_POINTS_MAX_POSITIVE` | `5` | Max positive points |
| `BEHAVIOR_POINTS_MIN_NEGATIVE` | `-5` | Min negative points |
| `BEHAVIOR_POINTS_MAX_NEGATIVE` | `-1` | Max negative points |

### Feature Flags

| Flag | Default | Description |
|---|---|---|
| `BEHAVIOR_TRACKING_ENABLED` | `false` | Enable/disable behavior tracking |
| `BEHAVIOR_AI_ANALYSIS_ENABLED` | `true` | Enable/disable AI pattern analysis |
| `BEHAVIOR_WEEKLY_SUMMARY_ENABLED` | `true` | Enable/disable weekly summaries |
| `BEHAVIOR_BADGE_INTEGRATION_ENABLED` | `true` | Enable/disable badge integration |

### School-Level Settings

| Setting | Default | Description |
|---|---|---|
| `behavior_tracking_enabled` | `false` | Per-school enable/disable |
| `behavior_ai_analysis_enabled` | `true` | Per-school AI analysis |
| `behavior_weekly_summary_enabled` | `true` | Per-school weekly summaries |
| `behavior_badge_integration_enabled` | `true` | Per-school badge integration |

---

## Appendix E: Migration & Rollback

### Migration: `migration_088_behavior_tracking.sql`

```sql
-- Migration 088: Behavior Tracking
-- Creates behavior_incidents and behavior_categories tables
-- Seeds default behavior categories

BEGIN;

CREATE TABLE IF NOT EXISTS behavior_incidents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    student_id      UUID NOT NULL,
    teacher_id      UUID NOT NULL,
    teacher_name    TEXT NOT NULL,
    incident_type   VARCHAR(16) NOT NULL,
    category        VARCHAR(32) NOT NULL,
    points          INTEGER NOT NULL,
    description     TEXT,
    incident_date   DATE NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_behavior_student
    ON behavior_incidents (student_id, incident_date DESC);

CREATE INDEX IF NOT EXISTS idx_behavior_school_date
    ON behavior_incidents (school_id, incident_date DESC);

CREATE TABLE IF NOT EXISTS behavior_categories (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID,
    name            VARCHAR(32) NOT NULL,
    incident_type   VARCHAR(16) NOT NULL,
    default_points  INTEGER NOT NULL,
    color           VARCHAR(8),
    is_active       BOOLEAN NOT NULL DEFAULT true
);

-- Seed default categories (global, school_id = null)
INSERT INTO behavior_categories (id, school_id, name, incident_type, default_points, color, is_active) VALUES
    (gen_random_uuid(), NULL, 'helpful', 'positive', 2, '#4CAF50', true),
    (gen_random_uuid(), NULL, 'leadership', 'positive', 3, '#2196F3', true),
    (gen_random_uuid(), NULL, 'improvement', 'positive', 2, '#8BC34A', true),
    (gen_random_uuid(), NULL, 'participation', 'positive', 1, '#00BCD4', true),
    (gen_random_uuid(), NULL, 'disruptive', 'negative', -2, '#FF5722', true),
    (gen_random_uuid(), NULL, 'late', 'negative', -1, '#FF9800', true),
    (gen_random_uuid(), NULL, 'disrespectful', 'negative', -3, '#F44336', true),
    (gen_random_uuid(), NULL, 'bullying', 'negative', -5, '#E91E63', true);

COMMIT;
```

### Rollback: `migration_088_rollback.sql`

```sql
BEGIN;
DROP TABLE IF EXISTS behavior_categories;
DROP TABLE IF EXISTS behavior_incidents;
COMMIT;
```

### Migration Validation

- Verify `behavior_incidents` table created with indexes
- Verify `behavior_categories` table created
- Verify 8 default categories seeded: `SELECT count(*) FROM behavior_categories WHERE school_id IS NULL` — should be 8
- Run `SELECT count(*) FROM behavior_incidents` — should be 0 (new feature)

---

## Appendix F: Observability

### Structured Logging

| Log Level | Event | Context Fields |
|---|---|---|
| INFO | Incident logged | `studentId, teacherId, incidentType, category, points` |
| INFO | Category created | `categoryId, name, incidentType, defaultPoints` |
| INFO | Category deactivated | `categoryId, name` |
| INFO | Weekly summary sent | `studentId, parentId, score, trend` |
| INFO | Badge awarded | `studentId, badgeId, triggerIncidentId` |
| WARN | Pattern analysis failed | `studentId, error` |
| WARN | Badge award failed | `studentId, badgeId, error` |
| WARN | Weekly summary: no incidents | `studentId` |
| ERROR | AI service error | `studentId, error` |

### Metrics

| Metric | Type | Labels | Description |
|---|---|---|---|
| `behavior_incidents_total` | Counter | `school_id, incident_type` | Total incidents logged |
| `behavior_incidents_by_category` | Counter | `school_id, category` | Incidents per category |
| `behavior_score_avg` | Gauge | `school_id, term` | Average student score |
| `behavior_pattern_analysis` | Counter | `school_id` | Pattern analyses run |
| `behavior_weekly_summaries_sent` | Counter | `school_id` | Weekly summaries sent |
| `behavior_badges_awarded` | Counter | `school_id, badge_id` | Behavior-triggered badges |
| `behavior_dashboard_load_duration` | Histogram | `school_id` | Dashboard load time |

### Health Checks

| Check | Endpoint | Description |
|---|---|---|
| Behavior service | `/health/behavior` | Verify service and DB accessible |
| AI analysis | `/health/behavior-ai` | Verify AI service reachable |

### Alerts

| Alert | Condition | Severity | Notification |
|---|---|---|---|
| AI analysis unavailable | Health check failed | Warning | Email to dev team |
| High negative incident rate | > 30% negative in 1 week | Warning | Email to admin |
| Weekly summary job failed | Job error | Warning | Email to dev team |
| Badge integration down | Health check failed | Warning | Email to dev team |

### Dashboards

| Dashboard | Panels | Audience |
|---|---|---|
| Behavior Overview | Total incidents, positive/negative ratio, trend | Product Team |
| School Behavior | Class-wise scores, top categories, students needing attention | School Admin |
| AI Analysis | Analyses run, success rate, duration | DevOps Team |
| Badge Integration | Badges awarded, by category, by badge type | Product Team |

### Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| AI bias in pattern analysis | Medium | Medium | Human review. AI is assistive, not authoritative. |
| Negative labeling of students | Medium | High | Focus on positive reinforcement. Parent sees summary, not raw analysis. |
| Teacher bias in logging | Medium | Medium | Admin oversight. Pattern analysis identifies outliers. |
| Privacy breach | Low | High | School-scoped. Parent sees own child only. No external sharing. |
| Badge inflation | Low | Low | Configurable thresholds. Admin can adjust rules. |
| AI service unavailable | Medium | Low | Fallback: manual review. Pattern analysis is optional. |
