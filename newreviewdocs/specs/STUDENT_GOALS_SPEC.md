# Student Goals & OKR — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** None
> **Source:** `DIFFERENTIATING_FEATURES.md` §6.2

---

## 1. Feature Overview

Goal-setting framework for students: teacher and parent collaboratively set academic and personal goals per term, track progress, and celebrate achievements. Combines OKR-style goal tracking with age-appropriate UI.

### Goals

- Teacher/parent sets goals per student per term (academic, behavioral, extracurricular)
- Goals are SMART (specific, measurable, achievable, relevant, time-bound)
- Progress tracking with check-ins
- AI-suggested goals based on performance data
- Goal completion celebration (badges, parent notification)
- Term-over-term goal history

---

## 2. Current System Assessment

- No goal-setting system exists
- `AssessmentMarksTable` — performance data for AI goal suggestions
- `HolisticAssessmentsTable` (from `NEP_COMPLIANCE_SPEC.md`) — holistic data
- `DIFFERENTIATING_FEATURES.md` §6.2: Student Goals, effort M

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Create goals: title, description, category (academic, behavioral, extracurricular), target metric, deadline |
| FR-2 | Goal categories: academic (marks target), behavioral (attendance, discipline), extracurricular (sports, arts), personal (reading, habits) |
| FR-3 | Progress check-ins: update progress value (0-100%) with notes |
| FR-4 | AI-suggested goals based on performance gaps (from `AI_EXAM_ANALYSIS_SPEC.md`) |
| FR-5 | Goal completion: auto-detect when target met → celebration badge + parent notification |
| FR-6 | Term history: view past term goals and completion rate |
| FR-7 | Collaborative: teacher creates, parent can view and add goals |

---

## 4. Database Design

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

---

## 5. Backend Architecture

### 5.1 StudentGoalService

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
}
```

### 5.2 AI Goal Suggestion Prompt

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

## 6. API Contracts

```
# Teacher
GET/POST /api/v1/teacher/goals/{studentId}
POST /api/v1/teacher/goals/{id}/checkin  { progress_value, notes }
GET /api/v1/teacher/goals/{studentId}/ai-suggestions

# Parent
GET /api/v1/parent/goals/{childId}
POST /api/v1/parent/goals/{childId}  { title, category, target_metric, deadline }
POST /api/v1/parent/goals/{id}/checkin  { progress_value, notes }
```

---

## 7. Acceptance Criteria

- [ ] Teacher/parent creates SMART goals per student per term
- [ ] Progress check-ins update goal progress
- [ ] AI suggests goals based on performance data
- [ ] Goal completion auto-detected → celebration + notification
- [ ] Term history shows past goals and completion rate
- [ ] Both teacher and parent can create and check in goals

---

## 8. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration, Exposed tables |
| 2 | 2 days | StudentGoalService (CRUD, checkin, completion) |
| 3 | 1 day | AI goal suggestion |
| 4 | 1 day | API endpoints |
| 5 | 3 days | Client UI (goal list, creation, progress tracker, celebration) |
| 6 | 1 day | Tests |

---

## 9. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | 2 goal tables |
| `server/.../feature/goals/StudentGoalService.kt` | New | Core service |
| `docs/db/migration_083_student_goals.sql` | New | DDL |
| `composeApp/.../ui/v2/screens/parent/StudentGoalsScreen.kt` | New | Parent goal view |
| `composeApp/.../ui/v2/screens/teacher/StudentGoalsScreen.kt` | New | Teacher goal management |
