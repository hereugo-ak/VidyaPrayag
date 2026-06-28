# Student Goals & OKR — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** None
> **Source:** `DIFFERENTIATING_FEATURES.md` §6.2
> **Template:** `_SPEC_TEMPLATE.md` v1 (25 mandatory + 6 optional sections)

---

## 1. Feature Overview

### What

Goal-setting framework for students: teacher and parent collaboratively set academic and personal goals per term, track progress, and celebrate achievements. Combines OKR-style goal tracking with age-appropriate UI.

### Why — Product Rationale

Goal-setting teaches students self-direction and accountability. By involving both teachers and parents in setting SMART goals, the platform fosters a collaborative growth environment. This is a **low-priority differentiating feature** (Phase 3, effort M) that adds value beyond basic grade tracking.

### What Stands Out (Competitive Moat)

From `DIFFERENTIATING_FEATURES.md` §6.2:
> "Student Goals — OKR-style goal tracking, AI-suggested goals, celebration badges."

No major Indian school ERP offers structured goal-setting with AI suggestions and celebration. The key moat is **AI-powered SMART goal suggestions** based on actual performance data.

### Goals

- Teacher/parent sets goals per student per term (academic, behavioral, extracurricular)
- Goals are SMART (specific, measurable, achievable, relevant, time-bound)
- Progress tracking with check-ins
- AI-suggested goals based on performance data
- Goal completion celebration (badges, parent notification)
- Term-over-term goal history

### Non-goals

- [ ] Student self-set goals without adult involvement (requires teacher/parent)
- [ ] School-level OKRs (this is student-level only)
- [ ] Teacher performance goals (that's `TEACHER_WELLNESS_SPEC.md`)
- [ ] Gamification platform (badges are simple, not a full gamification system)
- [ ] Real-time goal tracking (check-in based, not continuous)
- [ ] Goal-based grading (goals are motivational, not evaluative)
- [ ] Cross-student goal comparison (goals are individual)

### Dependencies

- `AssessmentMarksTable` — performance data for AI goal suggestions
- `HolisticAssessmentsTable` (from `NEP_COMPLIANCE_SPEC.md`) — holistic data
- `AI_EXAM_ANALYSIS_SPEC.md` — exam analysis for AI goal suggestions
- `AiService` (from `AI_INFRASTRUCTURE_SPEC.md`) — AI for goal suggestions
- `StudentsTable` — student info
- `AcademicYearsTable` — academic year and term context

### Related Modules

- `server/.../feature/goals/` — new goals module
- `server/.../feature/assessment/` — assessment data for AI suggestions
- `server/.../feature/ai/` — AI service
- `composeApp/.../ui/v2/screens/parent/` — parent UI
- `composeApp/.../ui/v2/screens/teacher/` — teacher UI

---

## 2. Current System Assessment

### Existing Code

- No goal-setting system exists
- `AssessmentMarksTable` — performance data for AI goal suggestions
- `HolisticAssessmentsTable` (from `NEP_COMPLIANCE_SPEC.md`) — holistic data
- `DIFFERENTIATING_FEATURES.md` §6.2: Student Goals, effort M

### Existing Database

- `AssessmentMarksTable` — exam marks for performance analysis
- `HolisticAssessmentsTable` — holistic assessment data
- `StudentsTable` — student info
- `AcademicYearsTable` — academic year and term info
- `AppUsersTable` — teacher and parent accounts
- No goal or goal check-in tables exist

### Existing APIs

- Assessment API (existing) — exam marks
- Holistic assessment API (existing) — holistic data
- No goal-setting API exists

### Existing UI

- Assessment screens (existing) — exam results
- Holistic assessment screens (existing) — holistic data
- No goal-setting UI exists

### Existing Services

- `AssessmentService` — exam marks
- `AiService` — AI infrastructure
- No goal service

### Existing Documentation

- `DIFFERENTIATING_FEATURES.md` §6.2 — Student Goals
- `AI_EXAM_ANALYSIS_SPEC.md` — exam analysis
- `AI_INFRASTRUCTURE_SPEC.md` — AI service

### Technical Debt

| # | Gap | Details |
|---|---|---|
| TD-1 | No goal tables | No `student_goals` or `student_goal_checkins` tables |
| TD-2 | No goal service | No service for CRUD, check-in, completion detection |
| TD-3 | No AI goal suggestion | No AI-powered SMART goal generation |
| TD-4 | No goal UI | No parent or teacher goal management screens |

### Gaps

| # | Gap | Impact | Severity |
|---|---|---|---|
| G1 | No goal-setting system | Can't set or track student goals | **High** |
| G2 | No progress tracking | Can't track goal progress via check-ins | **High** |
| G3 | No AI suggestions | Can't leverage performance data for goal suggestions | **Medium** |
| G4 | No completion celebration | No positive reinforcement for goal completion | **Medium** |
| G5 | No term history | Can't view past goals and completion rates | **Medium** |

---

## 3. Functional Requirements

### FR-001
| Field | Value |
|---|---|
| **Title** | Create Goals |
| **Description** | Create goals: title, description, category (academic, behavioral, extracurricular), target metric, deadline. |
| **Priority** | High |
| **User Roles** | Teacher, Parent |
| **Acceptance notes** | Goal has: title, description, category, target_metric (text), target_value (numeric, optional), deadline. Created by teacher or parent. |

### FR-002
| Field | Value |
|---|---|
| **Title** | Goal Categories |
| **Description** | Goal categories: academic (marks target), behavioral (attendance, discipline), extracurricular (sports, arts), personal (reading, habits). |
| **Priority** | High |
| **User Roles** | Teacher, Parent |
| **Acceptance notes** | 4 categories: academic, behavioral, extracurricular, personal. Each goal has exactly one category. |

### FR-003
| Field | Value |
|---|---|
| **Title** | Progress Check-ins |
| **Description** | Progress check-ins: update progress value (0-100%) with notes. |
| **Priority** | High |
| **User Roles** | Teacher, Parent |
| **Acceptance notes** | Check-in updates current_value and progress_percentage (0-100). Optional notes. Multiple check-ins over time create progress history. |

### FR-004
| Field | Value |
|---|---|
| **Title** | AI-Suggested Goals |
| **Description** | AI-suggested goals based on performance gaps (from `AI_EXAM_ANALYSIS_SPEC.md`). |
| **Priority** | Medium |
| **User Roles** | Teacher |
| **Acceptance notes** | AI fetches latest exam analysis (weak subjects, recommendations), calls AI to generate 3 SMART goals. Teacher can accept/modify/dismiss suggestions. |

### FR-005
| Field | Value |
|---|---|
| **Title** | Goal Completion |
| **Description** | Goal completion: auto-detect when target met → celebration badge + parent notification. |
| **Priority** | Medium |
| **User Roles** | System |
| **Acceptance notes** | Daily job checks if current_value >= target_value (or progress = 100%). On completion: set status=completed, set completed_at, emit celebration event, notify parent. |

### FR-006
| Field | Value |
|---|---|
| **Title** | Term History |
| **Description** | Term history: view past term goals and completion rate. |
| **Priority** | Medium |
| **User Roles** | Teacher, Parent |
| **Acceptance notes** | View goals from past terms. Shows completion rate (completed / total goals per term). Historical view. |

### FR-007
| Field | Value |
|---|---|
| **Title** | Collaborative Goal Setting |
| **Description** | Collaborative: teacher creates, parent can view and add goals. |
| **Priority** | High |
| **User Roles** | Teacher, Parent |
| **Acceptance notes** | Both teacher and parent can create goals and check in progress. Both can view all goals for a student. created_by tracks who created the goal. |

### Non-Functional Requirements

| ID | Requirement |
|---|---|
| NFR-1 | Goal creation: < 2 seconds |
| NFR-2 | Check-in: < 2 seconds |
| NFR-3 | AI suggestion: < 10 seconds |
| NFR-4 | Goal list load: < 3 seconds |
| NFR-5 | Completion check job: < 5 minutes for 1,000 goals |

---

## 4. User Stories

### Teacher
- [ ] Create SMART goals for students (academic, behavioral, extracurricular, personal)
- [ ] Check in on goal progress
- [ ] View AI-suggested goals based on performance data
- [ ] View student goal history by term
- [ ] See goal completion celebrations

### Parent
- [ ] View child's goals for current term
- [ ] Create goals for child
- [ ] Check in on child's goal progress
- [ ] View child's goal history by term
- [ ] Receive notification when child completes a goal

### System
- [ ] Daily job: check if goals have met their targets
- [ ] Auto-complete goals when target met
- [ ] Send celebration notification to parent on goal completion
- [ ] Generate AI goal suggestions from exam analysis data

---

## 5. Business Rules

### BR-001
**Rule:** Goals are per student per term.
**Enforcement:** Goals have `student_id`, `academic_year_id`, and `term`. Multiple goals per student per term allowed. No duplicate titles per student per term (recommended, not enforced).

### BR-002
**Rule:** Goals are SMART.
**Enforcement:** Goal has title (specific), target_metric and target_value (measurable), deadline (time-bound). AI suggestions follow SMART format. Manual goals should follow SMART but not enforced.

### BR-003
**Rule:** Both teacher and parent can create and check in goals.
**Enforcement:** `created_by` and `checked_in_by` track who performed the action. Both roles have write access. Both can view all goals for a student.

### BR-004
**Rule:** Goal completion is auto-detected.
**Enforcement:** Daily job checks if `current_value >= target_value` (for numeric targets) or `progress_percentage >= 100`. On completion: status → completed, completed_at set, notification sent.

### BR-005
**Rule:** Progress percentage is 0-100.
**Enforcement:** `progress_percentage` is REAL, 0-100. Calculated as `(current_value / target_value) * 100` for numeric targets, or manually set for non-numeric targets.

### BR-006
**Rule:** Goal status transitions: active → completed | missed | cancelled.
**Enforcement:** Goals start as `active`. Auto-complete when target met (`completed`). Auto-flag as `missed` when deadline passed and target not met. Can be `cancelled` by creator.

### BR-007
**Rule:** AI suggestions are advisory.
**Enforcement:** AI-generated goals are suggestions. Teacher must explicitly accept/modify to create actual goal. AI does not auto-create goals.

### BR-008
**Rule:** Goals are student-scoped and school-scoped.
**Enforcement:** All goals have `student_id` and `school_id`. No cross-student or cross-school data access.

### BR-009
**Rule:** Check-ins create progress history.
**Enforcement:** Each check-in creates a row in `student_goal_checkins`. History of progress over time. Cannot delete check-ins (audit trail).

### BR-010
**Rule:** Deadline determines missed status.
**Enforcement:** If deadline < today and status = active and target not met, daily job sets status = missed. Exception: if progress > 80%, keep as active (close to completion).

---

## 6. Database Design

### 6.1 Entity Relationship Summary

Two new tables: `student_goals` (goal definitions) and `student_goal_checkins` (progress check-ins). Goals reference existing `students`, `academic_years`, and `app_users` tables.

### 6.2 New Tables

#### `student_goals` table

```sql
CREATE TABLE student_goals (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    student_id      UUID NOT NULL,
    academic_year_id UUID NOT NULL,
    term            VARCHAR(16) NOT NULL,
    title           TEXT NOT NULL,
    description     TEXT,
    category        VARCHAR(32) NOT NULL,          -- academic | behavioral | extracurricular | personal
    target_metric   TEXT,                          -- "85% in Math", "100% attendance", "Read 20 books"
    target_value    REAL,                          -- numeric target if measurable
    current_value   REAL NOT NULL DEFAULT 0,
    progress_percentage REAL NOT NULL DEFAULT 0,   -- 0-100
    status          VARCHAR(16) NOT NULL DEFAULT 'active', -- active | completed | missed | cancelled
    deadline        DATE NOT NULL,
    created_by      UUID NOT NULL,
    created_by_name TEXT NOT NULL,
    completed_at    TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_student_goals_student ON student_goals(student_id, status, created_at DESC);
```

#### `student_goal_checkins` table

```sql
CREATE TABLE student_goal_checkins (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    goal_id         UUID NOT NULL REFERENCES student_goals(id) ON DELETE CASCADE,
    progress_value  REAL NOT NULL,
    progress_percentage REAL NOT NULL,
    notes           TEXT,
    checked_in_by   UUID NOT NULL,
    checked_in_by_name TEXT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
```

### 6.3 Modified Tables

N/A — no existing tables modified.

### 6.4 Indexes

- `idx_student_goals_student` on `student_goals(student_id, status, created_at DESC)` — goal list lookup
- `student_goal_checkins(goal_id)` — FK index for cascade delete

### 6.5 Constraints

- `student_goals.title` — NOT NULL
- `student_goals.category` — NOT NULL, VARCHAR(32), values: academic | behavioral | extracurricular | personal
- `student_goals.status` — NOT NULL, VARCHAR(16), default 'active', values: active | completed | missed | cancelled
- `student_goals.deadline` — NOT NULL, DATE
- `student_goals.progress_percentage` — NOT NULL, REAL, 0-100
- `student_goal_checkins.progress_percentage` — NOT NULL, REAL, 0-100

### 6.6 Foreign Keys

- `student_goals.school_id` → `schools.id` (implicit)
- `student_goals.student_id` → `students.id` (implicit)
- `student_goals.academic_year_id` → `academic_years.id` (implicit)
- `student_goals.created_by` → `app_users.id` (implicit)
- `student_goal_checkins.goal_id` → `student_goals.id` ON DELETE CASCADE

### 6.7 Soft Delete Strategy

N/A — goals use status field (active/completed/missed/cancelled) instead of soft delete. Cancelled goals remain in DB for history.

### 6.8 Audit Fields

- `student_goals.created_by`, `created_by_name` — who created the goal
- `student_goals.created_at`, `updated_at` — timestamps
- `student_goals.completed_at` — when goal was completed
- `student_goal_checkins.checked_in_by`, `checked_in_by_name` — who checked in
- `student_goal_checkins.created_at` — when check-in happened

### 6.9 Migration Notes

Migration: `docs/db/migration_083_student_goals.sql`
- CREATE 2 tables: `student_goals`, `student_goal_checkins`
- CREATE 1 index: `idx_student_goals_student`
- FK with ON DELETE CASCADE on `student_goal_checkins.goal_id`
- No data migration (new feature)

### 6.10 Exposed Mappings

```kotlin
object StudentGoalsTable : UUIDTable("student_goals", "id") {
    val schoolId           = uuid("school_id")
    val studentId          = uuid("student_id")
    val academicYearId     = uuid("academic_year_id")
    val term               = varchar("term", 16)
    val title              = text("title")
    val description        = text("description").nullable()
    val category           = varchar("category", 32)
    val targetMetric       = text("target_metric").nullable()
    val targetValue        = float("target_value").nullable()
    val currentValue       = float("current_value").default(0f)
    val progressPercentage = float("progress_percentage").default(0f)
    val status             = varchar("status", 16).default("active")
    val deadline           = date("deadline")
    val createdBy          = uuid("created_by")
    val createdByName      = text("created_by_name")
    val completedAt        = timestamp("completed_at").nullable()
    val createdAt          = timestamp("created_at")
    val updatedAt          = timestamp("updated_at")

    init {
        index("idx_student_goals_student", studentId, status, createdAt)
    }
}

object StudentGoalCheckinsTable : UUIDTable("student_goal_checkins", "id") {
    val goalId             = uuid("goal_id").references(StudentGoalsTable, onDelete = ReferenceOption.CASCADE)
    val progressValue      = float("progress_value")
    val progressPercentage = float("progress_percentage")
    val notes              = text("notes").nullable()
    val checkedInBy        = uuid("checked_in_by")
    val checkedInByName    = text("checked_in_by_name")
    val createdAt          = timestamp("created_at")
}
```

Register both in `DatabaseFactory.allTables`.

### 6.11 Seed Data

N/A — goals are user-created. No seed data needed.

---

## 7. State Machines

### Goal Lifecycle State Machine

```
active ──target_met──> completed ──(terminal)
  │
  ├──deadline_passed──> missed ──(terminal)
  │
  ├──creator_cancels──> cancelled ──(terminal)
  │
  └──progress_checkin──> active (updated progress)
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `active` | Target met (current >= target) | `completed` | Daily job or check-in detects completion |
| `active` | Deadline passed, target not met | `missed` | Daily job, deadline < today, progress < 80% |
| `active` | Deadline passed, progress > 80% | `active` | Close to completion, keep active |
| `active` | Creator cancels | `cancelled` | Teacher or parent cancels |
| `active` | Progress check-in | `active` | Progress updated, check-in recorded |
| `completed` | — | `completed` | Terminal state |
| `missed` | — | `missed` | Terminal state (can be reactivated manually) |
| `cancelled` | — | `cancelled` | Terminal state |

### Check-in State Machine

```
no_checkins ──first_checkin──> has_progress
  │
  └──has_progress ──subsequent_checkin──> has_progress (updated)
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `no_checkins` | First check-in | `has_progress` | Initial progress recorded |
| `has_progress` | Subsequent check-in | `has_progress` | Progress updated, history maintained |
| `has_progress` | Goal completed | `completed` | Target met, no more check-ins needed |

### AI Suggestion State Machine

```
not_generated ──teacher_requests──> generating ──ai_responds──> suggested
  │                                                    │
  │                                                    └──teacher_accepts──> created (goal)
  │                                                    └──teacher_modifies──> created (modified goal)
  │                                                    └──teacher_dismisses──> dismissed
  │
  └──suggested ──teacher_requests_again──> generating (new suggestions)
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `not_generated` | Teacher requests suggestions | `generating` | AI call initiated |
| `generating` | AI responds | `suggested` | 3 SMART goals returned |
| `generating` | AI fails | `not_generated` | Error, can retry |
| `suggested` | Teacher accepts | `created` | Goal created from suggestion |
| `suggested` | Teacher modifies and accepts | `created` | Modified goal created |
| `suggested` | Teacher dismisses | `dismissed` | Suggestions discarded |
| `dismissed` | Teacher requests again | `generating` | New AI call |

---

## 8. Backend Architecture

### 8.1 Component Overview

`StudentGoalService` handles goal CRUD, check-ins, completion detection, AI suggestions, and term history. `GoalCompletionJob` runs daily to check if targets are met. `AiService` (from `AI_INFRASTRUCTURE_SPEC.md`) provides SMART goal suggestions based on performance data.

### 8.2 Design Principles

1. **Collaborative** — both teacher and parent can create and check in goals
2. **SMART goals** — structured goal format with measurable targets
3. **AI-assisted** — AI suggests goals from performance data, but human creates
4. **Positive reinforcement** — celebration on completion, not penalty for missing
5. **Historical** — term-over-term history for growth tracking
6. **Student-scoped** — all goals are per-student, no cross-student comparison

### 8.3 Core Types

#### StudentGoalService

```kotlin
class StudentGoalService(private val aiService: AiService) {
    suspend fun createGoal(request: CreateGoalRequest): GoalDto
    suspend fun checkin(goalId: UUID, progress: Double, notes: String, userId: UUID): GoalDto
    suspend fun getGoals(studentId: UUID, term: String?): List<GoalDto>
    suspend fun getGoalHistory(studentId: UUID): List<TermGoalsDto>
    suspend fun aiSuggestGoals(studentId: UUID): List<GoalSuggestionDto> {
        // 1. Fetch latest exam analysis (weak subjects, recommendations)
        // 2. Call AI to generate SMART goals
    }
    suspend fun checkCompletion(): Int  // daily job: check if targets met
    suspend fun cancelGoal(goalId: UUID, userId: UUID): GoalDto
}
```

#### GoalCompletionJob

```kotlin
class GoalCompletionJob(private val goalService: StudentGoalService) {
    suspend fun execute(): Int {
        // 1. Get all active goals
        // 2. For each goal:
        //    - If current_value >= target_value → mark completed, notify parent
        //    - If deadline < today and progress < 80% → mark missed
        // 3. Return count of goals processed
    }
}
```

### 8.4 Repositories

- `StudentGoalRepository` — CRUD for `student_goals`
- `StudentGoalCheckinRepository` — CRUD for `student_goal_checkins`

### 8.5 Mappers

- `StudentGoalMapper` — maps `student_goals` rows to `GoalDto`
- `StudentGoalCheckinMapper` — maps `student_goal_checkins` rows to `CheckinDto`

### 8.6 Permission Checks

- Teacher endpoints: teacher role, can access any student in their school
- Parent endpoints: parent role, can only access own children
- Both can create goals and check in progress
- Both can view all goals for a student
- All endpoints: JWT auth via `requireAuth()`

### 8.7 Background Jobs

- **Goal Completion Job** — daily (every day, 6 AM IST)
  1. Get all active goals
  2. For each goal:
     - If `current_value >= target_value` (or progress = 100%) → mark completed, set completed_at, notify parent
     - If `deadline < today` and `progress_percentage < 80` → mark missed
  3. Return count of goals processed

### 8.8 Domain Events

- `GoalCreated` — emitted when goal is created
- `GoalCheckinRecorded` — emitted when progress check-in is recorded
- `GoalCompleted` — emitted when goal is auto-completed (celebration)
- `GoalMissed` — emitted when goal deadline passes without completion
- `GoalCancelled` — emitted when goal is cancelled
- `AIGoalsSuggested` — emitted when AI generates goal suggestions

### 8.9 Caching

- Goal list: cached per student + term, 5-minute TTL
- AI suggestions: cached per student, 1-hour TTL (avoid repeated AI calls)

### 8.10 Transactions

- Goal creation: single transaction (insert goal)
- Check-in: single transaction (insert check-in, update goal progress)
- Completion: single transaction (update goal status, insert notification)

### 8.11 Rate Limiting

N/A — goal features are low-frequency.

### 8.12 Configuration

- `STUDENT_GOALS_ENABLED` — default `true`; enable/disable feature
- `STUDENT_GOALS_AI_MODEL` — default `student_goals_v1`; AI model for suggestions
- `STUDENT_GOALS_MISS_THRESHOLD` — default `80`; progress % below which missed status is set after deadline

### 8.13 AI Goal Suggestion Prompt

```
System: Based on the student's performance data, suggest 3 SMART goals for next term.
Each goal should be: Specific, Measurable, Achievable, Relevant, Time-bound.
Categories: academic, behavioral, extracurricular, personal.

Student: {{name}}, Grade: {{grade}}
Weak subjects: {{weak_subjects}}
Attendance: {{attendance}}%
Recommendations from exam analysis: {{recommendations}}

Output JSON: [{"title": "...", "category": "...", "target_metric": "...", "target_value": N, "description": "..."}]
```

---

## 9. API Contracts

### 9.1 Teacher Endpoints

```
GET /api/v1/teacher/goals/{studentId}?term={term}
  → 200: { goals: [GoalDto] }

POST /api/v1/teacher/goals/{studentId}
  Body: { title, description, category, target_metric, target_value, deadline, term }
  → 201: GoalDto

POST /api/v1/teacher/goals/{id}/checkin
  Body: { progress_value, notes }
  → 200: GoalDto (updated)

GET /api/v1/teacher/goals/{studentId}/ai-suggestions
  → 200: { suggestions: [GoalSuggestionDto] }

GET /api/v1/teacher/goals/{studentId}/history
  → 200: { terms: [TermGoalsDto] }

POST /api/v1/teacher/goals/{id}/cancel
  → 200: GoalDto (cancelled)
```

### 9.2 Parent Endpoints

```
GET /api/v1/parent/goals/{childId}?term={term}
  → 200: { goals: [GoalDto] }

POST /api/v1/parent/goals/{childId}
  Body: { title, description, category, target_metric, target_value, deadline, term }
  → 201: GoalDto

POST /api/v1/parent/goals/{id}/checkin
  Body: { progress_value, notes }
  → 200: GoalDto (updated)

GET /api/v1/parent/goals/{childId}/history
  → 200: { terms: [TermGoalsDto] }
```

### 9.3 DTO Models

All `@Serializable`, wrapped in `ApiResponse<T>` pattern.

```kotlin
@Serializable data class GoalDto(
    val id: String,
    val studentId: String,
    val term: String,
    val title: String,
    val description: String?,
    val category: String,          // academic | behavioral | extracurricular | personal
    val targetMetric: String?,
    val targetValue: Float?,
    val currentValue: Float,
    val progressPercentage: Float, // 0-100
    val status: String,            // active | completed | missed | cancelled
    val deadline: String,
    val createdByName: String,
    val completedAt: String?,
    val createdAt: String,
    val checkins: List<CheckinDto>?,
)

@Serializable data class CheckinDto(
    val id: String,
    val progressValue: Float,
    val progressPercentage: Float,
    val notes: String?,
    val checkedInByName: String,
    val createdAt: String,
)

@Serializable data class GoalSuggestionDto(
    val title: String,
    val category: String,
    val targetMetric: String,
    val targetValue: Float?,
    val description: String,
)

@Serializable data class TermGoalsDto(
    val academicYearId: String,
    val term: String,
    val totalGoals: Int,
    val completedGoals: Int,
    val missedGoals: Int,
    val completionRate: Float,     // 0-100
    val goals: List<GoalDto>,
)

@Serializable data class CreateGoalRequest(
    val title: String,
    val description: String?,
    val category: String,
    val targetMetric: String?,
    val targetValue: Float?,
    val deadline: String,
    val term: String,
)

enum class GoalCategory { ACADEMIC, BEHAVIORAL, EXTRACURRICULAR, PERSONAL }
enum class GoalStatus { ACTIVE, COMPLETED, MISSED, CANCELLED }
```

---

## 10. Frontend Architecture

### 10.1 Screens

| Screen | Platform | Role | Description |
|---|---|---|---|
| `StudentGoalsScreen` (teacher) | Compose | Teacher | Goal management: create, check-in, view, AI suggestions |
| `StudentGoalsScreen` (parent) | Compose | Parent | Goal view: create, check-in, view, history |
| `GoalCreationDialog` | Compose | Teacher, Parent | SMART goal creation form |
| `GoalCelebrationDialog` | Compose | Teacher, Parent | Celebration on goal completion |

### 10.2 Navigation

- Teacher: Student Profile → Goals → Create / Check-in / AI Suggestions
- Parent: Child Profile → Goals → Create / Check-in / History

### 10.3 UX Flows

#### Teacher: Create Goal
1. Teacher opens student profile → Goals
2. Tap "Create Goal"
3. Fill: title, category, target metric, deadline, term
4. Optional: description, target value
5. Save → goal created

#### Teacher: AI Suggestions
1. Teacher opens student profile → Goals → AI Suggestions
2. AI generates 3 SMART goals based on performance
3. Teacher reviews suggestions
4. Accept / modify / dismiss each suggestion
5. Accepted suggestions become goals

#### Parent: Check-in Progress
1. Parent opens child profile → Goals
2. Select goal → Check-in
3. Update progress value
4. Add notes (optional)
5. Save → progress updated

#### Goal Completion Celebration
1. Daily job detects goal completion
2. Parent receives notification
3. Parent/teacher opens goal → celebration dialog
4. Badge shown, congratulations message

### 10.4 State Management

```kotlin
data class StudentGoalsState(
    val goals: List<GoalDto>,
    val isLoading: Boolean,
    val error: String?,
    val selectedTerm: String?,
)

data class GoalCreationState(
    val title: String,
    val description: String,
    val category: GoalCategory,
    val targetMetric: String,
    val targetValue: String,
    val deadline: LocalDate,
    val term: String,
    val isSaving: Boolean,
    val error: String?,
)

data class AISuggestionsState(
    val suggestions: List<GoalSuggestionDto>,
    val isLoading: Boolean,
    val error: String?,
)
```

### 10.5 Offline Support

- Goal list: cached locally (last fetched, 5-minute TTL)
- Check-ins: available offline (queued, synced when online)
- AI suggestions: not available offline (requires AI call)

### 10.6 Loading States

- Goal list: "Loading goals..."
- AI suggestions: "Generating AI suggestions..."
- Check-in: "Saving progress..."
- Goal creation: "Creating goal..."

### 10.7 Error Handling (UI)

- No goals: "No goals set for this term. Create one to get started."
- AI suggestions failed: "Unable to generate suggestions. Please try again."
- Goal already completed: "This goal has already been completed!"
- Network error: "Connection issue. Please check your internet."

### 10.8 Component Integration Guidelines

| Rule | Description |
|---|---|
| **R1** | Goal list: grouped by status (active, completed, missed), sorted by deadline |
| **R2** | Goal card: title, category badge, progress bar (0-100%), deadline, status badge |
| **R3** | Progress bar: color-coded (green > 80%, amber 50-80%, red < 50%) |
| **R4** | Category badge: color per category (blue=academic, green=behavioral, orange=extracurricular, purple=personal) |
| **R5** | Check-in: slider or input for progress value, notes textarea |
| **R6** | AI suggestions: cards with accept/modify/dismiss buttons |
| **R7** | Celebration: confetti animation, badge icon, congratulations text |
| **R8** | Term history: accordion per term, completion rate shown as percentage |
| **R9** | Goal creation: form with category dropdown, date picker for deadline |
| **R10** | Created by: show name of who created the goal (teacher/parent) |

---

## 11. Shared Module Changes (KMP)

### 11.1 DTOs

All DTOs defined in section 9.3, placed in `shared/.../goals/domain/model/GoalModels.kt`.

### 11.2 Domain Models

```kotlin
data class StudentGoal(
    val id: String,
    val studentId: String,
    val academicYearId: String,
    val term: String,
    val title: String,
    val description: String?,
    val category: GoalCategory,
    val targetMetric: String?,
    val targetValue: Float?,
    val currentValue: Float,
    val progressPercentage: Float,
    val status: GoalStatus,
    val deadline: LocalDate,
    val createdByName: String,
    val completedAt: Instant?,
    val createdAt: Instant,
)

data class GoalCheckin(
    val id: String,
    val goalId: String,
    val progressValue: Float,
    val progressPercentage: Float,
    val notes: String?,
    val checkedInByName: String,
    val createdAt: Instant,
)

data class GoalSuggestion(
    val title: String,
    val category: GoalCategory,
    val targetMetric: String,
    val targetValue: Float?,
    val description: String,
)

data class TermGoals(
    val academicYearId: String,
    val term: String,
    val totalGoals: Int,
    val completedGoals: Int,
    val missedGoals: Int,
    val completionRate: Float,
    val goals: List<StudentGoal>,
)
```

### 11.3 Repository Interfaces

```kotlin
interface StudentGoalRepository {
    suspend fun getGoals(token: String, studentId: String, term: String?): NetworkResult<List<GoalDto>>
    suspend fun createGoal(token: String, studentId: String, request: CreateGoalRequest): NetworkResult<GoalDto>
    suspend fun checkin(token: String, goalId: String, progressValue: Float, notes: String?): NetworkResult<GoalDto>
    suspend fun cancelGoal(token: String, goalId: String): NetworkResult<GoalDto>
    suspend fun getHistory(token: String, studentId: String): NetworkResult<List<TermGoalsDto>>
}

interface StudentGoalAIRepository {
    suspend fun getAISuggestions(token: String, studentId: String): NetworkResult<List<GoalSuggestionDto>>
}
```

### 11.4 UseCases

- `GetStudentGoalsUseCase`
- `CreateStudentGoalUseCase`
- `CheckinGoalUseCase`
- `CancelGoalUseCase`
- `GetGoalHistoryUseCase`
- `GetAIGoalSuggestionsUseCase`

### 11.5 Validation

- `title`: non-empty, max 200 characters
- `category`: one of academic, behavioral, extracurricular, personal
- `target_value`: positive number (if provided)
- `deadline`: date in the future
- `term`: non-empty string
- `progress_value`: non-negative number

### 11.6 Serialization

Standard Kotlinx serialization. Enums serialized as lowercase strings. Dates as ISO strings.

### 11.7 Network APIs

Ktor `@Resource` route definitions in `StudentGoalApi.kt`:
- GET `/api/v1/teacher/goals/{studentId}?term={term}`
- POST `/api/v1/teacher/goals/{studentId}`
- POST `/api/v1/teacher/goals/{id}/checkin`
- GET `/api/v1/teacher/goals/{studentId}/ai-suggestions`
- GET `/api/v1/teacher/goals/{studentId}/history`
- POST `/api/v1/teacher/goals/{id}/cancel`
- GET `/api/v1/parent/goals/{childId}?term={term}`
- POST `/api/v1/parent/goals/{childId}`
- POST `/api/v1/parent/goals/{id}/checkin`
- GET `/api/v1/parent/goals/{childId}/history`

### 11.8 Database Models (Local Cache)

- Goal list cached in local DB (last fetched, 5-minute TTL)
- Offline check-ins queued in local DB (synced when online)

---

## 12. Permissions Matrix

| Action | Super Admin | School Admin | Counselor | Teacher | Parent |
|---|---|---|---|---|---|
| View student goals | ✅ | ✅ | ✅ | ✅ (own students) | ✅ (own children) |
| Create goals | ✅ | ✅ | ❌ | ✅ | ✅ |
| Check-in progress | ✅ | ✅ | ❌ | ✅ | ✅ |
| Cancel goals | ✅ | ✅ | ❌ | ✅ (own created) | ✅ (own created) |
| View AI suggestions | ✅ | ✅ | ❌ | ✅ | ❌ |
| View goal history | ✅ | ✅ | ✅ | ✅ | ✅ |

---

## 13. Notifications

### Goal Notifications

| Trigger | Recipient | Channel | Template |
|---|---|---|---|
| Goal completed | Parent | FCM + in-app | "Great news! {child_name} completed their goal: '{title}'. Celebration time!" |
| Goal completed | Teacher | in-app | "Student {name} completed goal: '{title}'." |
| Goal missed | Parent | in-app | "Goal '{title}' deadline has passed. Let's set a new goal for next term." |
| Goal missed | Teacher | in-app | "Student {name} missed goal: '{title}'. Consider adjusting targets." |
| New goal created | Parent | in-app | "A new goal has been set for {child_name}: '{title}'." |
| Check-in recorded | Parent | in-app | "Progress update on goal '{title}': {progress}%." |

### Notification Integration

Uses existing `Notify.kt` dispatch. Notifications created with category "student_goals".

---

## 14. Background Jobs

### Goal Completion Job

| Property | Value |
|---|---|
| **Name** | `GoalCompletionJob` |
| **Schedule** | Daily (every day, 6 AM IST) |
| **Duration** | < 5 minutes for 1,000 active goals |
| **Retry** | None (next day) |

#### Job Flow

1. Get all active goals
2. For each goal:
   - If `current_value >= target_value` (or progress = 100%):
     - Set status = completed, set completed_at = now
     - Emit `GoalCompleted` event
     - Notify parent (FCM + in-app) and teacher (in-app)
   - If `deadline < today` and `progress_percentage < 80`:
     - Set status = missed
     - Emit `GoalMissed` event
     - Notify parent and teacher (in-app)
3. Return count of goals processed

---

## 15. Integrations

### Internal Integrations

| System | Integration Point | Direction | Protocol | Error Handling |
|---|---|---|---|---|
| `AiService` (`AI_INFRASTRUCTURE_SPEC.md`) | Goal suggestions | Call | Direct call | Log error, return empty list |
| `AssessmentMarksTable` | Performance data for AI | Read | Direct DB | Return empty if no data |
| `HolisticAssessmentsTable` | Holistic data for AI | Read | Direct DB | Return empty if no data |
| `AI_EXAM_ANALYSIS_SPEC.md` | Exam analysis recommendations | Read | Direct DB | Return empty if no data |
| `StudentsTable` | Student info | Read | Direct DB | Required |
| `AcademicYearsTable` | Term info | Read | Direct DB | Required |
| `Notify.kt` | Notifications | Call | Direct call | Log on failure |

### External Integrations

N/A — no external integrations. AI uses existing `AiService` (internal LLM).

### Integration Patterns

- **AI suggestions:** `aiService.complete(null, null, "student_goals", "student_goals_v1", context)` with exam analysis data
- **Notifications:** `Notify.kt` called with category "student_goals"
- **Performance data:** Direct DB queries on `AssessmentMarksTable`, `HolisticAssessmentsTable`

---

## 16. Security

### Authentication

- All goal APIs: JWT auth via `requireAuth()`
- Teacher endpoints: teacher role, can access students in their school
- Parent endpoints: parent role, can only access own children

### Authorization

- Teacher can create/check-in/cancel goals for any student in their school
- Parent can create/check-in/cancel goals for own children only
- Both can view all goals for a student
- No student self-access (requires teacher or parent)

### Data Protection

- Goal data is student-specific (not PII itself, but relates to student performance)
- AI suggestions do not send student names to LLM (uses anonymized context)
- Goal data visible to teacher, parent, and authorized school staff

### Input Validation

- `title`: non-empty, max 200 characters
- `category`: one of academic, behavioral, extracurricular, personal
- `target_value`: positive number (if provided)
- `deadline`: date in the future
- `progress_value`: non-negative number

### Rate Limiting

N/A — goal features are low-frequency.

### Audit Logging

- Goal created: creator ID, student ID, goal details, timestamp
- Check-in recorded: checker ID, goal ID, progress, timestamp
- Goal completed: goal ID, timestamp (by system)
- Goal cancelled: canceller ID, goal ID, timestamp
- AI suggestions requested: teacher ID, student ID, timestamp

### PII Handling

- Student name used in notifications (necessary for parent communication)
- Student name NOT sent to LLM (uses anonymized context)
- Goal data stored in DB, accessible only to authorized roles

### Multi-tenant Isolation

- All goal tables have `school_id` — school-scoped
- All queries filtered by `school_id`
- No cross-school goal data access

---

## 17. Performance & Scalability

### Expected Scale

- 500-2,000 students per school
- 3-5 goals per student per term = 1,500-10,000 goals per term
- 2-3 check-ins per goal per term = 3,000-30,000 check-ins per term
- Daily completion job: ~1,000 active goals

### Query Optimization

- Goal list: `idx_student_goals_student(student_id, status, created_at DESC)` — filtered by student and status
- Check-in history: `student_goal_checkins(goal_id)` — FK index
- Completion job: query active goals where deadline <= today or current >= target

### Indexing Strategy

- `student_goals(student_id, status, created_at DESC)` — goal list lookup
- `student_goal_checkins(goal_id)` — FK index for cascade delete and history lookup

### Caching Strategy

- Goal list: cached per student + term, 5-minute TTL
- AI suggestions: cached per student, 1-hour TTL (avoid repeated AI calls)

### Pagination

- Goal list: all goals for a student in single response (max ~20 per term)
- Check-in history: 20 check-ins per page
- Term history: all terms in single response

### Connection Pooling

Uses existing HikariCP connection pool. No additional pooling needed.

### Async Processing

- Completion job: background job (async)
- AI suggestions: synchronous (with cache)
- Notifications: async (fire-and-forget via `Notify.kt`)

### Scalability Concerns

- Daily completion job: 1,000 goals × simple check. Very manageable.
- AI suggestions: only when teacher requests. Cached for 1 hour.
- DB storage: 10,000 goals/term × 2 terms/year = 20,000/year × ~0.3KB = ~6MB/year. Negligible.

---

## 18. Edge Cases

| # | Scenario | Expected Behavior |
|---|---|---|
| EC-1 | Goal has no target_value (non-measurable) | Progress set manually via check-ins. Completion when progress = 100%. |
| EC-2 | Goal target_value = 0 | Completion when current_value >= 0 (immediately). Edge case, validate on creation. |
| EC-3 | Multiple check-ins same day | All recorded. Latest check-in determines current progress. |
| EC-4 | Check-in progress > target | Progress capped at 100%. Goal auto-completed. |
| EC-5 | Check-in progress < previous | Progress can decrease (setback). Recorded in history. |
| EC-6 | Goal deadline in past at creation | Validate on creation. Reject if deadline < today. |
| EC-7 | AI suggestions with no exam data | AI uses basic template goals. No personalization. |
| EC-8 | AI call fails | Return error. Teacher can retry or create manually. |
| EC-9 | Parent creates goal for child not theirs | Return 403. |
| EC-10 | Goal cancelled after completion | Not allowed. Completed goals are terminal. |
| EC-11 | Goal cancelled after missed | Not allowed. Missed goals are terminal. |
| EC-12 | Two parents create same goal for child | Both goals created. No deduplication (different perspectives). |

---

## 19. Error Handling

### Error Response Format

Standard `ApiResponse` error format.

### Error Codes

| Code | HTTP Status | Description | User Message |
|---|---|---|---|
| `GOAL_NOT_FOUND` | 404 | Goal ID not found | "Goal not found." |
| `NOT_AUTHORIZED` | 403 | Parent accessing other's child | "You can only view your own children's goals." |
| `INVALID_CATEGORY` | 400 | Category not in allowed values | "Invalid goal category." |
| `INVALID_DEADLINE` | 400 | Deadline in the past | "Deadline must be in the future." |
| `GOAL_ALREADY_COMPLETED` | 409 | Trying to check in completed goal | "This goal has already been completed." |
| `GOAL_ALREADY_CANCELLED` | 409 | Trying to check in cancelled goal | "This goal has been cancelled." |
| `AI_SUGGESTION_FAILED` | 500 | AI call failed | "Unable to generate suggestions. Please try again." |

### Error Handling Strategy

- **Goal creation:** Return 400 on invalid input. Return 403 if not authorized.
- **Check-in:** Return 409 if goal is completed/cancelled. Return 404 if goal not found.
- **AI suggestions:** Return 500 on AI failure. Teacher can retry.
- **Completion job:** Log error per goal. Continue with next.

### Retry Strategy

- Goal creation: client retries on network error (3 attempts)
- Check-in: client retries on network error (3 attempts)
- AI suggestions: client retries on AI failure (1 retry, then show error)
- Completion job: no retry (next day's job handles it)

### Fallback Behavior

- No goals: empty list, UI shows "Create goal" prompt
- No exam data for AI: AI returns generic template goals
- AI failure: return error, teacher creates manually
- No check-ins: goal shows 0% progress

---

## 20. Analytics & Reporting

### Analytics Dashboard Data

| Metric | Source | Derivation |
|---|---|---|
| Total goals created | `student_goals` | Count per school per term |
| Completion rate | `student_goals` | Completed / total goals per term |
| Miss rate | `student_goals` | Missed / total goals per term |
| Category distribution | `student_goals` | Count by category (academic, behavioral, etc.) |
| AI suggestion acceptance rate | Audit logs | Accepted / suggested per teacher |
| Average goals per student | `student_goals` | Total goals / total students |
| Parent vs teacher created | `student_goals` | Count by created_by role |

### Export Capabilities

- Goal report (CSV) — student, goal title, category, status, progress, term
- Completion report (CSV) — student, completion rate per term
- AI suggestion report (CSV) — student, suggestions, accepted/dismissed

### Report Types

| Report | Format | Frequency | Recipient |
|---|---|---|---|
| Goal overview | JSON (API) | On-demand | Teacher, Admin |
| Completion report | JSON (API) | Per term | Admin |
| AI suggestion analytics | JSON (API) | Monthly | Product Team |

---

## 21. Testing Strategy

### Unit Tests

- `StudentGoalService.createGoal()` — validation, category check, deadline check
- `StudentGoalService.checkin()` — progress update, percentage calculation, completion detection
- `StudentGoalService.getGoals()` — filtering by student, term, status
- `StudentGoalService.getGoalHistory()` — term aggregation, completion rate
- `StudentGoalService.aiSuggestGoals()` — AI prompt building, response parsing
- `GoalCompletionJob.execute()` — completion detection, missed detection
- Progress percentage calculation — edge cases (0 target, progress > target)

### Integration Tests

- Full goal lifecycle: create → check-in → complete → verify status and notification
- Full goal lifecycle: create → no check-ins → deadline passes → missed
- AI suggestions: set up exam data → request suggestions → verify SMART format
- Parent creates goal → teacher sees it
- Teacher creates goal → parent sees it
- Cancel goal → verify status, no more check-ins allowed
- Term history: create goals in multiple terms → verify history aggregation

### E2E Tests

- Teacher creates goal → parent receives notification → parent checks in → goal completes → celebration
- Teacher requests AI suggestions → accepts suggestion → goal created
- Goal deadline passes → missed status → parent notified

### Performance Tests

- Goal list load: < 3 seconds
- AI suggestions: < 10 seconds
- Completion job: < 5 minutes for 1,000 goals
- Check-in: < 2 seconds

### Test Data

- 10 students with varying goals (active, completed, missed, cancelled)
- 3 terms of goal history
- Goals in all 4 categories
- Check-in history (multiple per goal)
- Exam analysis data for AI suggestions
- Mock LLM (returns controlled SMART goals)

### Test Environment

- Test database with goal tables
- Pre-seeded student, assessment, academic year data
- Mock LLM (returns controlled suggestions)
- Test JWT tokens for teacher and parent roles

---

## 22. Acceptance Criteria

- [ ] Teacher/parent creates SMART goals per student per term
- [ ] Progress check-ins update goal progress
- [ ] AI suggests goals based on performance data
- [ ] Goal completion auto-detected → celebration + notification
- [ ] Term history shows past goals and completion rate
- [ ] Both teacher and parent can create and check in goals
- [ ] Goal categories: academic, behavioral, extracurricular, personal
- [ ] Goal status: active, completed, missed, cancelled
- [ ] Daily completion job runs and processes goals
- [ ] Parent receives notification on goal completion
- [ ] AI suggestions follow SMART format
- [ ] Progress percentage displayed (0-100%)

---

## 23. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration `migration_083_student_goals.sql`, Exposed tables, register in `DatabaseFactory` |
| 2 | 2 days | `StudentGoalService` (CRUD, check-in, completion, history) |
| 3 | 1 day | AI goal suggestion (prompt, response parsing, caching) |
| 4 | 1 day | API endpoints (teacher + parent) |
| 5 | 3 days | Client UI: `StudentGoalsScreen` (teacher + parent), `GoalCreationDialog`, `GoalCelebrationDialog` |
| 6 | 1 day | Tests: unit, integration, E2E |

### Pre-Implementation Checklist

- [ ] Verify `AI_INFRASTRUCTURE_SPEC.md` is implemented and `AiService` available
- [ ] Verify `AssessmentMarksTable` exists with exam data
- [ ] Verify `AcademicYearsTable` has term information
- [ ] Verify `Notify.kt` supports "student_goals" category
- [ ] Verify parent-student relationship exists in DB
- [ ] Verify teacher-student relationship exists in DB

---

## 24. File-Level Impact Analysis

### Server

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | `StudentGoalsTable`, `StudentGoalCheckinsTable` |
| `server/.../db/DatabaseFactory.kt` | Modify | Register 2 goal tables in `allTables` |
| `server/.../feature/goals/StudentGoalService.kt` | **New** | Core service (CRUD, check-in, completion, AI, history) |
| `server/.../feature/goals/GoalCompletionJob.kt` | **New** | Daily completion check job |
| `server/.../feature/goals/StudentGoalRouting.kt` | **New** | API endpoints (teacher + parent) |
| `docs/db/migration_083_student_goals.sql` | **New** | DDL |

### Shared (KMP)

| File | Change Type | Description |
|---|---|---|
| `shared/.../goals/domain/model/GoalModels.kt` | **New** | DTOs, domain models, enums |
| `shared/.../goals/domain/repository/StudentGoalRepository.kt` | **New** | Repository interfaces |
| `shared/.../goals/data/remote/StudentGoalApi.kt` | **New** | HTTP API definitions |

### Client (Compose)

| File | Change Type | Description |
|---|---|---|
| `composeApp/.../ui/v2/screens/parent/StudentGoalsScreen.kt` | **New** | Parent goal view |
| `composeApp/.../ui/v2/screens/teacher/StudentGoalsScreen.kt` | **New** | Teacher goal management |
| `composeApp/.../ui/v2/screens/goals/GoalCreationDialog.kt` | **New** | Goal creation form |
| `composeApp/.../ui/v2/screens/goals/GoalCelebrationDialog.kt` | **New** | Celebration on completion |

---

## 25. Future Enhancements

| # | Enhancement | Priority | Effort | Notes |
|---|---|---|---|---|
| F-1 | Student self-set goals | Medium | M | Allow students to propose goals (with teacher/parent approval) |
| F-2 | Goal templates | Medium | S | Pre-defined goal templates by grade/category |
| F-3 | Goal dependencies | Low | M | Goals that depend on other goals' completion |
| F-4 | Goal sharing with student | Medium | S | Show goals to student (age-appropriate UI) |
| F-5 | Goal analytics dashboard | Low | M | Admin view of goal-setting patterns across school |
| F-6 | Recurring goals | Low | S | Goals that auto-recreate each term |
| F-7 | Goal comments | Low | S | Teacher/parent comments on goal progress |
| F-8 | Goal badges and rewards | Medium | M | Digital badges for goal completion, visible in student profile |
| F-9 | Peer goal inspiration (anonymized) | Low | S | Show anonymized example goals for inspiration |
| F-10 | Integration with Vidya Passport | Low | S | Goal achievements reflected in student passport |

---

## Appendix A: Sequence Diagrams

### A.1 Goal Creation and Check-in Flow

```
Teacher/Parent       Server                      DB
  │                    │                           │
  │  POST /goals       │                           │
  │  {title, cat, ...} │                           │
  │  ───────────────>  │                           │
  │                    │──validate input            │
  │                    │──insert goal──────────────>│
  │                    │←──goal created─────────────│
  │  ←──201: GoalDto───│                           │
  │                    │                           │
  │  POST /checkin     │                           │
  │  {progress, notes} │                           │
  │  ───────────────>  │                           │
  │                    │──insert checkin───────────>│
  │                    │──update goal progress─────>│
  │                    │←──success─────────────────│
  │  ←──200: GoalDto───│                           │
  │                    │                           │
```

### A.2 Goal Completion Job Flow

```
GoalCompletionJob    Server          DB              Notify
  │                    │               │               │
  │  execute()         │               │               │
  │  ───────────────>  │               │               │
  │                    │──get active goals────────────>│
  │                    │←──goal list──────────────────│
  │                    │               │               │
  │                    │──for each goal:               │
  │                    │  check if target met          │
  │                    │ ──update status──────────────>│
  │                    │←──success────────────────────│
  │                    │               │               │
  │                    │──notify parent──────────────────>│
  │                    │──notify teacher──────────────────>│
  │                    │               │               │
  │  ←──count processed│               │               │
  │                    │               │               │
```

### A.3 AI Goal Suggestion Flow

```
Teacher (app)      Server              AI Service
  │                  │                    │
  │  GET /ai-suggest │                    │
  │  ─────────────>  │                    │
  │                  │──get exam data──>  │ (DB)
  │                  │←──exam analysis───│
  │                  │                    │
  │                  │──call AI──────────>│
  │                  │  (SMART prompt)    │
  │                  │←──3 goals (JSON)──│
  │                  │                    │
  │  ←──200: suggest─│                    │
  │                  │                    │
```

---

## Appendix B: Domain Model / ER Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                      student_goals (new)                              │
│  id (PK)                                                              │
│  school_id, student_id, academic_year_id                              │
│  term, title, description                                             │
│  category (academic|behavioral|extracurricular|personal)              │
│  target_metric, target_value, current_value                           │
│  progress_percentage (0-100)                                          │
│  status (active|completed|missed|cancelled)                           │
│  deadline, created_by, created_by_name                                │
│  completed_at, created_at, updated_at                                 │
│  INDEX: (student_id, status, created_at DESC)                         │
└──────────────────────────────────────────────────────────────────────┘
         │
         │ 1:N
         ↓
┌──────────────────────────────────────────────────────────────────────┐
│                   student_goal_checkins (new)                         │
│  id (PK)                                                              │
│  goal_id (FK → student_goals.id, ON DELETE CASCADE)                   │
│  progress_value, progress_percentage                                  │
│  notes                                                                │
│  checked_in_by, checked_in_by_name                                    │
│  created_at                                                           │
└──────────────────────────────────────────────────────────────────────┘

Existing tables used:
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│ students         │  │ academic_years   │  │ assessment_marks │
│ (existing)       │  │ (existing)       │  │ (existing)       │
└──────────────────┘  └──────────────────┘  └──────────────────┘
┌──────────────────┐  ┌────────────────────────┐
│ app_users        │  │ holistic_assessments   │
│ (existing)       │  │ (existing)             │
└──────────────────┘  └────────────────────────┘
```

---

## Appendix C: Event Flow

### Domain Events

| Event | Emitter | Consumers | Payload | Side Effects |
|---|---|---|---|---|
| `GoalCreated` | `StudentGoalService.createGoal()` | `Notify.kt` | `goalId, studentId, title, createdByName` | Notify parent if teacher created |
| `GoalCheckinRecorded` | `StudentGoalService.checkin()` | `Notify.kt` | `goalId, studentId, progressPercentage, checkedInByName` | Notify parent of progress update |
| `GoalCompleted` | `GoalCompletionJob` | `Notify.kt` | `goalId, studentId, title` | Celebration notification to parent + teacher |
| `GoalMissed` | `GoalCompletionJob` | `Notify.kt` | `goalId, studentId, title` | Notification to parent + teacher |
| `GoalCancelled` | `StudentGoalService.cancelGoal()` | None (logged) | `goalId, cancelledBy` | Goal status updated |
| `AIGoalsSuggested` | `StudentGoalService.aiSuggestGoals()` | None (logged) | `studentId, suggestionCount` | Suggestions available |

### Event Delivery Guarantees

- Goal created event: emitted synchronously, notification async
- Check-in event: emitted synchronously, notification async
- Completion event: emitted within job execution, notification async
- Missed event: emitted within job execution, notification async
- AI suggestions event: emitted synchronously (fire-and-forget logging)

---

## Appendix D: Configuration

### Environment Variables

| Variable | Default | Description |
|---|---|---|
| `STUDENT_GOALS_ENABLED` | `true` | Enable/disable student goals feature |
| `STUDENT_GOALS_AI_MODEL` | `student_goals_v1` | AI model for goal suggestions |
| `STUDENT_GOALS_MISS_THRESHOLD` | `80` | Progress % below which missed status is set after deadline |
| `STUDENT_GOALS_AI_CACHE_TTL_HOURS` | `1` | Cache TTL for AI suggestions |

### Feature Flags

| Flag | Default | Description |
|---|---|---|
| `STUDENT_GOALS_ENABLED` | `true` | Enable/disable student goals |
| `STUDENT_GOALS_AI_ENABLED` | `true` | Enable/disable AI goal suggestions |

### School-Level Settings

| Setting | Default | Description |
|---|---|---|
| `student_goals_enabled` | `true` | Per-school enable/disable |
| `goal_categories_enabled` | all | Which categories are available |

---

## Appendix E: Migration & Rollback

### Migration: `migration_083_student_goals.sql`

```sql
-- Migration 083: Student Goals
-- Creates student_goals and student_goal_checkins tables

BEGIN;

CREATE TABLE IF NOT EXISTS student_goals (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    student_id      UUID NOT NULL,
    academic_year_id UUID NOT NULL,
    term            VARCHAR(16) NOT NULL,
    title           TEXT NOT NULL,
    description     TEXT,
    category        VARCHAR(32) NOT NULL,
    target_metric   TEXT,
    target_value    REAL,
    current_value   REAL NOT NULL DEFAULT 0,
    progress_percentage REAL NOT NULL DEFAULT 0,
    status          VARCHAR(16) NOT NULL DEFAULT 'active',
    deadline        DATE NOT NULL,
    created_by      UUID NOT NULL,
    created_by_name TEXT NOT NULL,
    completed_at    TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_student_goals_student
    ON student_goals (student_id, status, created_at DESC);

CREATE TABLE IF NOT EXISTS student_goal_checkins (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    goal_id         UUID NOT NULL REFERENCES student_goals(id) ON DELETE CASCADE,
    progress_value  REAL NOT NULL,
    progress_percentage REAL NOT NULL,
    notes           TEXT,
    checked_in_by   UUID NOT NULL,
    checked_in_by_name TEXT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

COMMIT;
```

### Rollback: `migration_083_rollback.sql`

```sql
BEGIN;
DROP TABLE IF EXISTS student_goal_checkins;
DROP TABLE IF EXISTS student_goals;
COMMIT;
```

### Migration Validation

- Verify 2 tables created with correct columns
- Verify `idx_student_goals_student` index created
- Verify FK on `student_goal_checkins.goal_id` with ON DELETE CASCADE
- Run `SELECT count(*) FROM student_goals` — should be 0 (new feature)

---

## Appendix F: Observability

### Structured Logging

| Log Level | Event | Context Fields |
|---|---|---|
| INFO | Goal created | `goalId, studentId, title, category, createdByName` |
| INFO | Check-in recorded | `goalId, studentId, progressPercentage, checkedInByName` |
| INFO | Goal completed | `goalId, studentId, title` |
| INFO | Goal missed | `goalId, studentId, title, deadline` |
| INFO | Goal cancelled | `goalId, cancelledBy` |
| INFO | AI suggestions generated | `studentId, suggestionCount` |
| INFO | Completion job completed | `processedCount, completedCount, missedCount, duration` |
| WARN | AI suggestions failed | `studentId, error` |
| WARN | Goal has no target_value | `goalId` |
| WARN | Check-in progress decreased | `goalId, previousProgress, newProgress` |
| ERROR | Completion job failed | `error` |
| ERROR | AI call failed | `studentId, error` |

### Metrics

| Metric | Type | Labels | Description |
|---|---|---|---|
| `student_goals_created` | Counter | `school_id, category` | Total goals created |
| `student_goals_completed` | Counter | `school_id, category` | Total goals completed |
| `student_goals_missed` | Counter | `school_id` | Total goals missed |
| `student_goal_checkins` | Counter | `school_id` | Total check-ins recorded |
| `student_goal_completion_rate` | Gauge | `school_id` | Completion rate per term |
| `student_goal_ai_suggestions` | Counter | `school_id` | Total AI suggestion requests |
| `student_goal_ai_acceptance_rate` | Gauge | `school_id` | AI suggestion acceptance rate |
| `student_goal_completion_job_duration` | Histogram | `school_id` | Completion job duration |

### Health Checks

| Check | Endpoint | Description |
|---|---|---|
| Student goals service | `/health/student-goals` | Verify service and DB accessible |
| Completion job | `/health/goal-completion` | Verify last job run and status |

### Alerts

| Alert | Condition | Severity | Notification |
|---|---|---|---|
| Completion job failed | Job error rate > 10% | Warning | Email to dev team |
| AI suggestion failure rate | > 20% | Warning | Email to dev team |
| Low goal creation | < 1 goal per student per term | Info | Email to product team |
| High miss rate | > 50% goals missed | Warning | Email to admin (targets too ambitious?) |

### Dashboards

| Dashboard | Panels | Audience |
|---|---|---|
| Goal Overview | Goals created, completion rate, miss rate, category distribution | Product Team |
| AI Suggestions | Request count, acceptance rate, failure rate | Product Team |
| Job Performance | Completion job duration, error rate | Dev Team |
| Goal Engagement | Goals per student, check-in frequency, parent vs teacher created | Product Team |

### Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Goals used for grading/evaluation | Medium | High | Clear policy. Goals are motivational, not evaluative. |
| AI suggests unrealistic goals | Medium | Medium | Teacher reviews and modifies. SMART format. |
| Parents disagree with teacher goals | Low | Medium | Both can create goals. Collaborative approach. |
| Goal completion not detected | Low | Low | Daily job. Progress check-ins trigger completion check. |
| Too many goals overwhelm student | Medium | Low | Recommended 3-5 goals per term. UI guidance. |
| AI sends student name to LLM | Low | High | Anonymized context in AI prompt. No PII in LLM. |
| Goals create pressure on student | Medium | Medium | Positive reinforcement. Celebration, not penalty. |
