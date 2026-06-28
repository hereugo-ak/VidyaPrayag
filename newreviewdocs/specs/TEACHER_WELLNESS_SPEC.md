# Teacher Wellness — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** `AI_INFRASTRUCTURE_SPEC.md`
> **Source:** `DIFFERENTIATING_FEATURES.md` §5.2
> **Template:** `_SPEC_TEMPLATE.md` v1 (25 mandatory + 6 optional sections)

---

## 1. Feature Overview

### What

Teacher wellness and burnout prevention: workload tracking, self-assessment, AI-powered workload balance insights, and admin alerts for overburdened teachers.

### Why — Product Rationale

Teacher burnout is a significant problem in Indian schools. High workload, large class sizes, and administrative burdens lead to stress and attrition. A teacher wellness system helps admins identify overburdened teachers and take corrective action — improving retention and teaching quality.

This is a **low-priority differentiating feature** (Phase 3, effort M) that demonstrates care for teacher well-being.

### What Stands Out (Competitive Moat)

From `DIFFERENTIATING_FEATURES.md` §5.2:
> "Teacher Wellness — workload tracking, burnout prevention, AI workload balancing."

No major Indian school ERP offers teacher wellness tracking with AI-powered workload analysis. The key moat is **AI-powered workload balancing** — not just tracking, but actionable recommendations.

### Goals

- Track teacher workload: classes per day, assessments to grade, homework to review
- Weekly self-assessment: stress, workload manageability, job satisfaction
- AI workload analysis: identify teachers with excessive load
- Admin dashboard: teacher workload distribution, burnout risk indicators
- Workload balancing recommendations

### Non-goals

- [ ] Clinical mental health diagnosis (wellness tracking is observational)
- [ ] Direct therapy/counseling through the app
- [ ] Performance evaluation based on wellness data (wellness is not performance)
- [ ] Student-facing teacher wellness data (private to teacher + admin)
- [ ] Parent access to teacher wellness data
- [ ] Real-time workload tracking (weekly snapshots are sufficient)
- [ ] Teacher-to-teacher wellness comparison (privacy)

### Dependencies

- `AI_INFRASTRUCTURE_SPEC.md` — AI service for workload analysis and recommendations
- `TeacherPeriodsTable` — teaching schedule
- `HomeworkTable` / `HomeworkSubmissionsTable` — homework data
- `AssessmentMarksTable` — assessment grading data
- `TeacherCheckInsTable` — attendance check-ins
- `AppUsersTable` — teacher accounts
- `SchoolsTable` — school info

### Related Modules

- `server/.../feature/wellness/` — wellness module (shared with student wellness)
- `server/.../feature/teacher/` — teacher management module
- `composeApp/.../ui/v2/screens/teacher/` — teacher UI
- `composeApp/.../ui/v2/screens/admin/` — admin dashboard UI

---

## 2. Current System Assessment

### Existing Code

- `TeacherPeriodsTable` — teaching schedule
- `HomeworkTable` — homework assigned by teacher
- `AssessmentMarksTable` — marks to grade
- `TeacherCheckInsTable` — attendance check-ins
- No wellness tracking for teachers

### Existing Database

- `TeacherPeriodsTable` — teaching schedule (periods per day, subjects, classes)
- `HomeworkTable` — homework assigned by teacher
- `HomeworkSubmissionsTable` — student homework submissions (status: SUBMITTED, REVIEWED)
- `AssessmentMarksTable` — assessment marks (status: draft, published)
- `TeacherCheckInsTable` — teacher attendance check-ins
- `AppUsersTable` — user accounts (role=teacher)
- `SchoolsTable` — school info
- No teacher wellness survey or workload snapshot tables exist

### Existing APIs

- Teacher schedule API (existing) — teaching periods
- Homework management API (existing) — homework assignments
- Assessment management API (existing) — assessment marks
- No teacher wellness API exists

### Existing UI

- Teacher dashboard (existing) — classes, homework, assessments
- Admin teacher management (existing) — teacher list, assignments
- No teacher wellness survey or workload dashboard UI exists

### Existing Services

- `TeacherService` — teacher management
- `HomeworkService` — homework management
- `AssessmentService` — assessment management
- No teacher wellness service

### Existing Documentation

- `DIFFERENTIATING_FEATURES.md` §5.2 — Teacher Wellness
- `AI_INFRASTRUCTURE_SPEC.md` — AI service

### Technical Debt

| # | Gap | Details |
|---|---|---|
| TD-1 | No wellness survey table | No `teacher_wellness_surveys` table |
| TD-2 | No workload snapshot table | No `teacher_workload_snapshots` table |
| TD-3 | No workload calculation | No service to calculate teacher workload from existing data |
| TD-4 | No burnout risk analysis | No AI-powered burnout risk detection |
| TD-5 | No workload dashboard | No admin dashboard for teacher workload distribution |

### Gaps

| # | Gap | Impact | Severity |
|---|---|---|---|
| G1 | No workload tracking | Can't measure teacher workload | **High** |
| G2 | No self-assessment | Can't capture teacher stress/satisfaction | **High** |
| G3 | No burnout risk detection | Overburdened teachers not identified | **High** |
| G4 | No workload dashboard | Admins lack visibility into workload distribution | **Medium** |
| G5 | No balancing recommendations | No actionable suggestions for workload redistribution | **Medium** |

---

## 3. Functional Requirements

### FR-001
| Field | Value |
|---|---|
| **Title** | Auto-Calculate Workload |
| **Description** | Auto-calculate workload: periods/day, pending grading, pending homework review. |
| **Priority** | High |
| **User Roles** | System |
| **Acceptance notes** | Weekly job calculates workload from TeacherPeriodsTable, pending AssessmentMarksTable (status=draft), pending HomeworkSubmissionsTable (status=SUBMITTED). Stores in teacher_workload_snapshots. |

### FR-002
| Field | Value |
|---|---|
| **Title** | Weekly Self-Assessment |
| **Description** | Weekly self-assessment: stress (1-5), workload manageability (1-5), satisfaction (1-5). |
| **Priority** | High |
| **User Roles** | Teacher |
| **Acceptance notes** | 3 dimensions, each 1-5 scale. Optional text notes. One survey per teacher per week (UNIQUE on teacher_id + survey_date). |

### FR-003
| Field | Value |
|---|---|
| **Title** | AI Workload Analysis |
| **Description** | AI workload analysis: compare actual load vs school average, flag outliers. |
| **Priority** | High |
| **User Roles** | System |
| **Acceptance notes** | Weekly job identifies teachers with workload_score > 1.5x school average. AI generates explanation and risk level. |

### FR-004
| Field | Value |
|---|---|
| **Title** | Admin Dashboard |
| **Description** | Admin dashboard: workload distribution, burnout risk indicators. |
| **Priority** | High |
| **User Roles** | School Admin |
| **Acceptance notes** | Shows all teachers' workload scores, distribution chart, burnout risk indicators (high/medium/low), and trends over time. |

### FR-005
| Field | Value |
|---|---|
| **Title** | Workload Balancing Recommendations |
| **Description** | Workload balancing recommendations (AI-generated). |
| **Priority** | Medium |
| **User Roles** | School Admin |
| **Acceptance notes** | AI analyzes workload distribution and generates recommendations: redistribute periods, reassign grading, adjust assignments. |

### FR-006
| Field | Value |
|---|---|
| **Title** | Privacy Controls |
| **Description** | Privacy: self-assessment visible only to teacher + admin (not other teachers). |
| **Priority** | High |
| **User Roles** | System |
| **Acceptance notes** | Self-assessment data visible only to the teacher themselves and school admin. Not visible to other teachers, parents, or students. Workload snapshots visible to admin only. |

### Non-Functional Requirements

| ID | Requirement |
|---|---|
| NFR-1 | Workload calculation: < 5 minutes for 100 teachers |
| NFR-2 | Self-assessment submission: < 2 seconds |
| NFR-3 | Admin dashboard load: < 3 seconds |
| NFR-4 | AI analysis: < 30 seconds for entire school |
| NFR-5 | Balancing recommendations: < 10 seconds |

---

## 4. User Stories

### Teacher
- [ ] Complete weekly wellness self-assessment (stress, workload, satisfaction)
- [ ] View my own workload summary (periods, grading pending, homework pending)
- [ ] View my wellness history (past surveys)

### School Admin
- [ ] View teacher workload dashboard (all teachers)
- [ ] See workload distribution chart
- [ ] View burnout risk indicators (which teachers are overburdened)
- [ ] View AI-generated workload balancing recommendations
- [ ] View individual teacher wellness timeline

### System
- [ ] Weekly workload calculation job (Monday 6 AM)
- [ ] Calculate workload score from periods, grading, homework
- [ ] Identify teachers with workload > 1.5x school average
- [ ] Generate AI burnout risk analysis
- [ ] Generate AI workload balancing recommendations

---

## 5. Business Rules

### BR-001
**Rule:** One wellness survey per teacher per week.
**Enforcement:** `UNIQUE(teacher_id, survey_date)` constraint on `teacher_wellness_surveys`. Prevents duplicate surveys.

### BR-002
**Rule:** One workload snapshot per teacher per week.
**Enforcement:** `UNIQUE(teacher_id, week_start_date)` constraint on `teacher_workload_snapshots`. Prevents duplicate snapshots.

### BR-003
**Rule:** Self-assessment is private.
**Enforcement:** Self-assessment visible only to the teacher and school admin. Not visible to other teachers, parents, or students. Workload snapshots visible to admin only.

### BR-004
**Rule:** Burnout risk threshold is 1.5x school average.
**Enforcement:** Teachers with workload_score > 1.5x school average are flagged as burnout risk. AI determines risk level (high/medium/low).

### BR-005
**Rule:** Workload score is a composite metric.
**Enforcement:** Workload score = weighted combination of periods_per_week, pending_grading, pending_homework_review, classes_taught, subjects_taught. Exact formula defined in service.

### BR-006
**Rule:** Wellness data is not performance data.
**Enforcement:** Wellness data (surveys, workload) is NOT used for performance evaluation. It's for well-being support only. Admin sees data for intervention, not evaluation.

### BR-007
**Rule:** Workload snapshots are weekly.
**Enforcement:** Snapshots taken every Monday 6 AM. Represents past week's workload. Not real-time.

### BR-008
**Rule:** AI recommendations are advisory.
**Enforcement:** AI-generated balancing recommendations are suggestions. Admin makes final decision on workload redistribution.

### BR-009
**Rule:** Wellness data is school-scoped.
**Enforcement:** All wellness tables have `school_id`. Queries filtered by school. No cross-school data access.

### BR-010
**Rule:** Teacher can view only their own wellness data.
**Enforcement:** Teacher API returns only the authenticated teacher's surveys and workload. No access to other teachers' data.

---

## 6. Database Design

### 6.1 Entity Relationship Summary

Two new tables: `teacher_wellness_surveys` (weekly self-assessment) and `teacher_workload_snapshots` (weekly calculated workload). Both reference existing `app_users` (teacher) and `schools` tables.

### 6.2 New Tables

#### `teacher_wellness_surveys` table

```sql
CREATE TABLE teacher_wellness_surveys (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    teacher_id      UUID NOT NULL,
    survey_date     DATE NOT NULL,
    stress_level    INTEGER NOT NULL,              -- 1-5
    workload_manageability INTEGER NOT NULL,       -- 1-5
    job_satisfaction INTEGER NOT NULL,             -- 1-5
    notes           TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(teacher_id, survey_date)
);
```

#### `teacher_workload_snapshots` table

```sql
CREATE TABLE teacher_workload_snapshots (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    teacher_id      UUID NOT NULL,
    week_start_date DATE NOT NULL,
    periods_per_week INTEGER NOT NULL,
    pending_grading INTEGER NOT NULL,
    pending_homework_review INTEGER NOT NULL,
    classes_taught  INTEGER NOT NULL,
    subjects_taught INTEGER NOT NULL,
    workload_score  REAL NOT NULL,                 -- calculated composite
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(teacher_id, week_start_date)
);
```

### 6.3 Modified Tables

N/A — no existing tables modified. Workload calculated from existing tables (read-only).

### 6.4 Indexes

- `teacher_wellness_surveys(teacher_id, survey_date)` — UNIQUE, one per week
- `teacher_workload_snapshots(teacher_id, week_start_date)` — UNIQUE, one per week
- `teacher_workload_snapshots(school_id, week_start_date)` — dashboard lookup

### 6.5 Constraints

- `teacher_wellness_surveys.stress_level` — NOT NULL, INTEGER 1-5
- `teacher_wellness_surveys.workload_manageability` — NOT NULL, INTEGER 1-5
- `teacher_wellness_surveys.job_satisfaction` — NOT NULL, INTEGER 1-5
- `teacher_wellness_surveys(teacher_id, survey_date)` — UNIQUE
- `teacher_workload_snapshots.workload_score` — NOT NULL, REAL
- `teacher_workload_snapshots(teacher_id, week_start_date)` — UNIQUE

### 6.6 Foreign Keys

- `teacher_wellness_surveys.school_id` → `schools.id` (implicit)
- `teacher_wellness_surveys.teacher_id` → `app_users.id` (implicit)
- `teacher_workload_snapshots.school_id` → `schools.id` (implicit)
- `teacher_workload_snapshots.teacher_id` → `app_users.id` (implicit)

### 6.7 Soft Delete Strategy

N/A — wellness records are permanent. No soft delete. Surveys and snapshots are historical records.

### 6.8 Audit Fields

- `teacher_wellness_surveys.created_at` — when survey submitted
- `teacher_workload_snapshots.created_at` — when snapshot taken

### 6.9 Migration Notes

Migration: `docs/db/migration_082_teacher_wellness.sql`
- CREATE 2 tables: `teacher_wellness_surveys`, `teacher_workload_snapshots`
- No data migration (new feature)
- Indexes and UNIQUE constraints created in same migration

### 6.10 Exposed Mappings

```kotlin
object TeacherWellnessSurveysTable : UUIDTable("teacher_wellness_surveys", "id") {
    val schoolId             = uuid("school_id")
    val teacherId            = uuid("teacher_id")
    val surveyDate           = date("survey_date")
    val stressLevel          = integer("stress_level")
    val workloadManageability = integer("workload_manageability")
    val jobSatisfaction      = integer("job_satisfaction")
    val notes                = text("notes").nullable()
    val createdAt            = timestamp("created_at")

    init {
        uniqueIndex("idx_teacher_wellness_unique", teacherId, surveyDate)
    }
}

object TeacherWorkloadSnapshotsTable : UUIDTable("teacher_workload_snapshots", "id") {
    val schoolId              = uuid("school_id")
    val teacherId             = uuid("teacher_id")
    val weekStartDate         = date("week_start_date")
    val periodsPerWeek        = integer("periods_per_week")
    val pendingGrading        = integer("pending_grading")
    val pendingHomeworkReview = integer("pending_homework_review")
    val classesTaught         = integer("classes_taught")
    val subjectsTaught        = integer("subjects_taught")
    val workloadScore         = float("workload_score")
    val createdAt             = timestamp("created_at")

    init {
        uniqueIndex("idx_teacher_workload_unique", teacherId, weekStartDate)
        index("idx_teacher_workload_school", schoolId, weekStartDate)
    }
}
```

Register both in `DatabaseFactory.allTables`.

### 6.11 Seed Data

N/A — wellness data is user-generated. No seed data needed.

---

## 7. State Machines

### Workload Snapshot State Machine

```
not_calculated ──weekly_job──> calculated ──next_week──> not_calculated (new week)
  │
  └──calculated ──admin_views──> viewed
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `not_calculated` | Weekly job (Monday 6 AM) | `calculated` | Job runs for all teachers |
| `calculated` | Admin views dashboard | `viewed` | Admin opens dashboard |
| `calculated` | Next week's job | `calculated` | New snapshot for new week |
| `viewed` | Next week's job | `calculated` | New snapshot for new week |

### Burnout Risk State Machine

```
normal ──workload_exceeds_1.5x_avg──> at_risk ──workload_normalizes──> normal
  │                                     │
  │                                     │──workload_exceeds_2x──> high_risk
  │                                     │
  │                                     └──admin_intervention──> monitored
  │
  └──high_risk ──admin_intervention──> monitored ──workload_normalizes──> normal
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `normal` | Workload > 1.5x average | `at_risk` | Weekly job flags |
| `at_risk` | Workload > 2x average | `high_risk` | Weekly job flags |
| `at_risk` | Workload normalizes | `normal` | Workload ≤ 1.5x average |
| `high_risk` | Admin intervention | `monitored` | Admin takes action |
| `at_risk` | Admin intervention | `monitored` | Admin takes action |
| `monitored` | Workload normalizes | `normal` | Workload ≤ 1.5x average for 2+ weeks |
| `monitored` | Workload increases again | `at_risk` | Workload > 1.5x average |

### Survey Submission State Machine

```
not_submitted ──teacher_submits──> submitted
  │
  └──already_submitted_this_week──> submitted (UNIQUE constraint prevents duplicate)
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `not_submitted` | Teacher submits survey | `submitted` | First survey of week |
| `not_submitted` | Already submitted this week | `submitted` | UNIQUE constraint prevents duplicate |
| `submitted` | Next week | `not_submitted` | New week, new survey allowed |

---

## 8. Backend Architecture

### 8.1 Component Overview

`TeacherWellnessService` handles survey submission, workload calculation, dashboard aggregation, burnout risk analysis, and balancing recommendations. `WeeklyWorkloadJob` runs every Monday to calculate workload snapshots. `AiService` (from `AI_INFRASTRUCTURE_SPEC.md`) provides workload analysis and recommendations.

### 8.2 Design Principles

1. **Privacy-first** — self-assessment visible only to teacher + admin
2. **Automated workload tracking** — calculated from existing data, no manual entry
3. **AI-assisted, admin-decided** — AI flags risks and recommends actions, admin decides
4. **Wellness ≠ performance** — wellness data is for support, not evaluation
5. **Weekly cadence** — snapshots and surveys are weekly, not real-time
6. **School-scoped** — all data filtered by school_id

### 8.3 Core Types

#### TeacherWellnessService

```kotlin
class TeacherWellnessService(private val aiService: AiService) {
    suspend fun submitSurvey(teacherId: UUID, survey: SurveyDto)
    suspend fun calculateWorkload(teacherId: UUID, weekStart: LocalDate): WorkloadDto
    suspend fun getWorkloadDashboard(schoolId: UUID): WorkloadDashboardDto  // all teachers
    suspend fun getBurnoutRisk(schoolId: UUID): List<BurnoutRiskDto>
    suspend fun getBalancingRecommendations(schoolId: UUID): List<RecommendationDto>
    suspend fun getMyWorkload(teacherId: UUID): WorkloadDto
    suspend fun getMySurveyHistory(teacherId: UUID, range: TimeRange): List<SurveyDto>
}
```

#### WeeklyWorkloadJob

```kotlin
class WeeklyWorkloadJob(private val wellnessService: TeacherWellnessService) {
    suspend fun execute(): Int {
        // 1. For each teacher:
        //    - Calculate periods from TeacherPeriodsTable
        //    - Count pending grading from AssessmentMarksTable (status=draft)
        //    - Count pending homework from HomeworkSubmissionsTable (status=SUBMITTED)
        //    - Calculate workload_score (composite)
        // 2. Store in teacher_workload_snapshots
        // 3. Identify teachers with workload_score > 1.5x school average
        // 4. Return count of snapshots created
    }
}
```

### 8.4 Repositories

- `TeacherWellnessSurveyRepository` — CRUD for `teacher_wellness_surveys`
- `TeacherWorkloadSnapshotRepository` — CRUD for `teacher_workload_snapshots`

### 8.5 Mappers

- `TeacherWellnessSurveyMapper` — maps `teacher_wellness_surveys` rows to `SurveyDto`
- `TeacherWorkloadSnapshotMapper` — maps `teacher_workload_snapshots` rows to `WorkloadDto`

### 8.6 Permission Checks

- Teacher survey: teacher role, can only submit/view own surveys
- Teacher workload: teacher role, can only view own workload
- Admin dashboard: admin role
- Admin burnout risk: admin role
- Admin recommendations: admin role
- All endpoints: JWT auth via `requireAuth()`

### 8.7 Background Jobs

- **Weekly Workload Job** — every Monday (6 AM IST)
  1. For each teacher: calculate workload from `TeacherPeriodsTable`, pending `AssessmentMarksTable` (status=draft), pending `HomeworkSubmissionsTable` (status=SUBMITTED)
  2. Store in `teacher_workload_snapshots`
  3. Identify teachers with workload_score > 1.5x school average → flag
  4. Return count of snapshots created

- **AI Burnout Analysis Job** — every Monday (7 AM IST, after workload job)
  1. Get all flagged teachers (workload > 1.5x average)
  2. For each flagged teacher:
     - Gather workload trend, survey scores, comparison with peers
     - Call AI to analyze and generate burnout risk assessment
  3. Store risk level and AI explanation
  4. Return count of teachers analyzed

### 8.8 Domain Events

- `TeacherSurveySubmitted` — emitted when teacher submits wellness survey
- `WorkloadSnapshotCreated` — emitted when weekly workload snapshot is created
- `BurnoutRiskDetected` — emitted when teacher flagged as burnout risk
- `BalancingRecommendationsGenerated` — emitted when AI generates recommendations

### 8.9 Caching

- Admin dashboard: cached per school, 10-minute TTL
- Burnout risk list: cached per school, 10-minute TTL
- Teacher's own workload: cached per teacher, 5-minute TTL

### 8.10 Transactions

- Survey submission: single transaction (insert survey)
- Workload snapshot: single transaction (insert snapshot)
- Dashboard read: no transaction (read-only, multiple queries)

### 8.11 Rate Limiting

N/A — wellness features are low-frequency (weekly survey, weekly snapshot).

### 8.12 Configuration

- `TEACHER_WELLNESS_ENABLED` — default `true`; enable/disable feature
- `TEACHER_WELLNESS_BURNOUT_THRESHOLD` — default `1.5`; multiplier for burnout risk flag
- `TEACHER_WELLNESS_HIGH_RISK_THRESHOLD` — default `2.0`; multiplier for high risk
- `TEACHER_WELLNESS_AI_MODEL` — default `teacher_wellness_v1`; AI model identifier

### 8.13 Weekly Workload Job

Every Monday 6 AM:
1. For each teacher: calculate workload from `TeacherPeriodsTable`, pending `AssessmentMarksTable` (status=draft), pending `HomeworkSubmissionsTable` (status=SUBMITTED)
2. Store in `teacher_workload_snapshots`
3. Identify teachers with workload_score > 1.5x school average → flag

### 8.14 AI Prompt (Burnout Analysis)

```
System: You are a teacher wellness analyst. Analyze the following teacher workload data and determine burnout risk.
Consider: workload compared to peers, trend over time, self-assessment scores.

Data:
- Teacher workload score: {{workload_score}}
- School average workload: {{school_avg}}
- Workload trend (last 4 weeks): {{workload_trend}}
- Self-assessment scores: {{survey_data}}
- Peer comparison: {{peer_comparison}}

Output JSON: {"risk_level": "low|medium|high", "explanation": "...", "recommendations": ["..."]}
```

---

## 9. API Contracts

### 9.1 Teacher Endpoints

```
POST /api/v1/teacher/wellness/survey
  Body: { stress_level: 3, workload_manageability: 4, job_satisfaction: 4, notes: "optional" }
  → 201: { success: true }

GET /api/v1/teacher/wellness/my-workload
  → 200: WorkloadDto

GET /api/v1/teacher/wellness/my-surveys?weeks=12
  → 200: { surveys: [SurveyDto] }
```

### 9.2 Admin Endpoints

```
GET /api/v1/school/wellness/teacher-dashboard
  → 200: WorkloadDashboardDto

GET /api/v1/school/wellness/burnout-risk
  → 200: { teachers: [BurnoutRiskDto] }

GET /api/v1/school/wellness/balancing-recommendations
  → 200: { recommendations: [RecommendationDto] }
```

### 9.3 DTO Models

All `@Serializable`, wrapped in `ApiResponse<T>` pattern.

```kotlin
@Serializable data class SurveyDto(
    val id: String,
    val surveyDate: String,
    val stressLevel: Int,           // 1-5
    val workloadManageability: Int, // 1-5
    val jobSatisfaction: Int,       // 1-5
    val notes: String?,
)

@Serializable data class WorkloadDto(
    val id: String,
    val weekStartDate: String,
    val periodsPerWeek: Int,
    val pendingGrading: Int,
    val pendingHomeworkReview: Int,
    val classesTaught: Int,
    val subjectsTaught: Int,
    val workloadScore: Float,
)

@Serializable data class WorkloadDashboardDto(
    val schoolAverageScore: Float,
    val teachers: List<TeacherWorkloadDto>,
    val distribution: Map<String, Int>,  // range → count
)

@Serializable data class TeacherWorkloadDto(
    val teacherId: String,
    val teacherName: String,
    val workloadScore: Float,
    val periodsPerWeek: Int,
    val pendingGrading: Int,
    val pendingHomeworkReview: Int,
    val riskLevel: String,  // low | medium | high
)

@Serializable data class BurnoutRiskDto(
    val teacherId: String,
    val teacherName: String,
    val workloadScore: Float,
    val riskLevel: String,     // low | medium | high
    val aiExplanation: String,
    val recommendations: List<String>,
)

@Serializable data class RecommendationDto(
    val teacherId: String,
    val teacherName: String,
    val recommendation: String,
    val priority: String,  // high | medium | low
)

enum class RiskLevel { LOW, MEDIUM, HIGH }
```

---

## 10. Frontend Architecture

### 10.1 Screens

| Screen | Platform | Role | Description |
|---|---|---|---|
| `WellnessSurveyScreen` | Compose | Teacher | Weekly self-assessment (stress, workload, satisfaction) |
| `MyWorkloadScreen` | Compose | Teacher | View own workload summary |
| `TeacherWellnessScreen` | Compose | School Admin | Admin dashboard with workload distribution, burnout risk |

### 10.2 Navigation

- Teacher: Home → Wellness → Survey / My Workload
- Admin: Dashboard → Teacher Wellness → Workload Dashboard / Burnout Risk / Recommendations

### 10.3 UX Flows

#### Teacher: Submit Weekly Survey
1. Teacher opens Wellness → Weekly Survey
2. 3 questions with 1-5 slider/radio: stress, workload manageability, job satisfaction
3. Optional: add notes
4. Submit → confirmation

#### Admin: View Teacher Wellness Dashboard
1. Admin opens Dashboard → Teacher Wellness
2. See workload distribution chart (all teachers)
3. See school average workload score
4. See burnout risk list (flagged teachers)
5. Tap teacher → see detailed workload, survey history, AI analysis
6. View balancing recommendations
7. Take action (redistribute workload, etc.)

### 10.4 State Management

```kotlin
data class WellnessSurveyState(
    val stressLevel: Int,
    val workloadManageability: Int,
    val jobSatisfaction: Int,
    val notes: String,
    val isSubmitting: Boolean,
    val error: String?,
)

data class MyWorkloadState(
    val workload: WorkloadDto?,
    val isLoading: Boolean,
    val error: String?,
)

data class TeacherWellnessDashboardState(
    val dashboard: WorkloadDashboardDto?,
    val burnoutRisks: List<BurnoutRiskDto>,
    val recommendations: List<RecommendationDto>,
    val isLoading: Boolean,
    val error: String?,
)
```

### 10.5 Offline Support

- Survey: available offline (queued, synced when online)
- My workload: cached locally (last fetched)
- Dashboard: cached locally (last fetched, 10-minute TTL)

### 10.6 Loading States

- Survey: "Submitting..."
- My workload: "Loading workload..."
- Dashboard: "Loading teacher wellness data..."

### 10.7 Error Handling (UI)

- Already submitted this week: "You've already submitted a survey this week."
- No workload data: "No workload data available yet. Data is calculated every Monday."
- No burnout risk: "No burnout risks detected. All teachers within normal workload range."
- Network error: "Connection issue. Please check your internet."

### 10.8 Component Integration Guidelines

| Rule | Description |
|---|---|
| **R1** | Survey: 3 questions with 1-5 slider/radio, progress indicator |
| **R2** | My workload: stat cards (periods, pending grading, pending homework, score) |
| **R3** | Dashboard: bar chart of workload scores per teacher |
| **R4** | Burnout risk: list sorted by risk level (high → medium → low), color-coded badges |
| **R5** | Recommendations: list with teacher name, recommendation text, priority badge |
| **R6** | Privacy notice: "Your self-assessment is private and visible only to you and school admin." |
| **R7** | Workload score: displayed as number with color indicator (green/amber/red) |
| **R8** | Distribution chart: histogram of workload scores across teachers |

---

## 11. Shared Module Changes (KMP)

### 11.1 DTOs

All DTOs defined in section 9.3, placed in `shared/.../wellness/domain/model/TeacherWellnessModels.kt`.

### 11.2 Domain Models

```kotlin
data class TeacherSurvey(
    val id: String,
    val surveyDate: LocalDate,
    val stressLevel: Int,
    val workloadManageability: Int,
    val jobSatisfaction: Int,
    val notes: String?,
)

data class TeacherWorkload(
    val id: String,
    val weekStartDate: LocalDate,
    val periodsPerWeek: Int,
    val pendingGrading: Int,
    val pendingHomeworkReview: Int,
    val classesTaught: Int,
    val subjectsTaught: Int,
    val workloadScore: Float,
)

data class WorkloadDashboard(
    val schoolAverageScore: Float,
    val teachers: List<TeacherWorkloadSummary>,
    val distribution: Map<String, Int>,
)

data class TeacherWorkloadSummary(
    val teacherId: String,
    val teacherName: String,
    val workloadScore: Float,
    val periodsPerWeek: Int,
    val pendingGrading: Int,
    val pendingHomeworkReview: Int,
    val riskLevel: RiskLevel,
)

data class BurnoutRisk(
    val teacherId: String,
    val teacherName: String,
    val workloadScore: Float,
    val riskLevel: RiskLevel,
    val aiExplanation: String,
    val recommendations: List<String>,
)

data class BalancingRecommendation(
    val teacherId: String,
    val teacherName: String,
    val recommendation: String,
    val priority: String,
)
```

### 11.3 Repository Interfaces

```kotlin
interface TeacherWellnessRepository {
    suspend fun submitSurvey(token: String, request: SurveyRequest): NetworkResult<Unit>
    suspend fun getMyWorkload(token: String): NetworkResult<WorkloadDto>
    suspend fun getMySurveys(token: String, weeks: Int): NetworkResult<List<SurveyDto>>
}

interface TeacherWellnessManagementRepository {
    suspend fun getDashboard(token: String): NetworkResult<WorkloadDashboardDto>
    suspend fun getBurnoutRisk(token: String): NetworkResult<List<BurnoutRiskDto>>
    suspend fun getRecommendations(token: String): NetworkResult<List<RecommendationDto>>
}
```

### 11.4 UseCases

- `SubmitTeacherSurveyUseCase`
- `GetMyWorkloadUseCase`
- `GetMySurveyHistoryUseCase`
- `GetTeacherWellnessDashboardUseCase`
- `GetBurnoutRiskUseCase`
- `GetBalancingRecommendationsUseCase`

### 11.5 Validation

- `stress_level`, `workload_manageability`, `job_satisfaction`: integer 1-5
- `notes`: max 500 characters (optional)
- `weeks`: integer 1-52 (for survey history)

### 11.6 Serialization

Standard Kotlinx serialization. Enums serialized as lowercase strings.

### 11.7 Network APIs

Ktor `@Resource` route definitions in `TeacherWellnessApi.kt`:
- POST `/api/v1/teacher/wellness/survey`
- GET `/api/v1/teacher/wellness/my-workload`
- GET `/api/v1/teacher/wellness/my-surveys?weeks={weeks}`
- GET `/api/v1/school/wellness/teacher-dashboard`
- GET `/api/v1/school/wellness/burnout-risk`
- GET `/api/v1/school/wellness/balancing-recommendations`

### 11.8 Database Models (Local Cache)

- My workload cached in local DB (last fetched, 5-minute TTL)
- Dashboard cached in local DB (last fetched, 10-minute TTL)
- Offline surveys queued in local DB (synced when online)

---

## 12. Permissions Matrix

| Action | Super Admin | School Admin | Counselor | Teacher | Parent |
|---|---|---|---|---|---|
| Submit wellness survey | N/A | N/A | N/A | ✅ (own only) | ❌ |
| View own workload | N/A | N/A | N/A | ✅ | ❌ |
| View own survey history | N/A | N/A | N/A | ✅ | ❌ |
| View teacher wellness dashboard | ✅ | ✅ | ❌ | ❌ | ❌ |
| View burnout risk | ✅ | ✅ | ❌ | ❌ | ❌ |
| View balancing recommendations | ✅ | ✅ | ❌ | ❌ | ❌ |
| View other teachers' surveys | ✅ | ✅ | ❌ | ❌ | ❌ |
| View other teachers' workload | ✅ | ✅ | ❌ | ❌ | ❌ |

---

## 13. Notifications

### Teacher Wellness Notifications

| Trigger | Recipient | Channel | Template |
|---|---|---|---|
| Burnout risk detected (high) | Admin | FCM + in-app | "HIGH RISK: Teacher {name} shows high burnout risk. Workload score: {score}. Please review." |
| Burnout risk detected (medium) | Admin | in-app | "Teacher {name} flagged for burnout risk. Workload score: {score}." |
| Weekly survey reminder | Teacher | FCM + in-app | "Reminder: Please complete your weekly wellness survey." |
| Workload snapshot available | Teacher | in-app | "Your weekly workload summary is available." |
| Balancing recommendations ready | Admin | in-app | "AI workload balancing recommendations are ready for review." |

### Notification Integration

Uses existing `Notify.kt` dispatch. Notifications created with category "teacher_wellness".

---

## 14. Background Jobs

### Weekly Workload Job

| Property | Value |
|---|---|
| **Name** | `WeeklyWorkloadJob` |
| **Schedule** | Weekly (Monday 6 AM IST) |
| **Duration** | < 5 minutes for 100 teachers |
| **Retry** | None (next week) |

#### Job Flow

1. For each teacher:
   - Calculate periods from `TeacherPeriodsTable`
   - Count pending grading from `AssessmentMarksTable` (status=draft)
   - Count pending homework from `HomeworkSubmissionsTable` (status=SUBMITTED)
   - Calculate workload_score (composite)
2. Store in `teacher_workload_snapshots`
3. Identify teachers with workload_score > 1.5x school average → flag
4. Return count of snapshots created

### AI Burnout Analysis Job

| Property | Value |
|---|---|
| **Name** | `BurnoutAnalysisJob` |
| **Schedule** | Weekly (Monday 7 AM IST, after workload job) |
| **Duration** | < 30 seconds for 100 teachers |
| **Retry** | None (next week) |

#### Job Flow

1. Get all flagged teachers (workload > 1.5x average)
2. For each flagged teacher:
   - Gather workload trend, survey scores, peer comparison
   - Call AI to analyze and generate burnout risk assessment
3. Store risk level and AI explanation
4. Return count of teachers analyzed

---

## 15. Integrations

### Internal Integrations

| System | Integration Point | Direction | Protocol | Error Handling |
|---|---|---|---|---|
| `AiService` (`AI_INFRASTRUCTURE_SPEC.md`) | Burnout analysis | Call | Direct call | Log error, skip teacher |
| `AiService` (`AI_INFRASTRUCTURE_SPEC.md`) | Balancing recommendations | Call | Direct call | Log error, return empty list |
| `TeacherPeriodsTable` | Teaching schedule | Read | Direct DB | Required |
| `AssessmentMarksTable` | Pending grading | Read | Direct DB | Count = 0 if none |
| `HomeworkSubmissionsTable` | Pending homework review | Read | Direct DB | Count = 0 if none |
| `AppUsersTable` | Teacher info | Read | Direct DB | Required |
| `Notify.kt` | Notifications | Call | Direct call | Log on failure |

### External Integrations

N/A — no external integrations. All data is internal. AI uses existing `AiService` (internal LLM).

### Integration Patterns

- **Workload calculation:** Direct DB queries on `TeacherPeriodsTable`, `AssessmentMarksTable`, `HomeworkSubmissionsTable`
- **AI analysis:** `aiService.complete(null, null, "teacher_wellness", "teacher_wellness_v1", context)`
- **Notifications:** `Notify.kt` called with category "teacher_wellness"

---

## 16. Security

### Authentication

- All teacher wellness APIs: JWT auth via `requireAuth()`
- Teacher endpoints: teacher role, can only access own data
- Admin endpoints: admin role

### Authorization

- Self-assessment visible only to teacher + admin
- Workload snapshots visible to admin only
- Teacher can view only own surveys and workload
- No parent or student access to teacher wellness data
- No cross-school data access

### Data Protection

- Wellness data is sensitive (teacher mental health indicators)
- Self-assessment data encrypted at rest
- AI analysis does not send teacher names to LLM (uses anonymized IDs)
- Workload data is derived from existing system data (not PII)

### Input Validation

- `stress_level`, `workload_manageability`, `job_satisfaction`: integer 1-5
- `notes`: max 500 characters (optional)
- `weeks`: integer 1-52

### Rate Limiting

N/A — wellness features are low-frequency (weekly).

### Audit Logging

- Survey submitted: teacher ID, stress/workload/satisfaction levels, date
- Workload snapshot created: teacher ID, workload score, week
- Dashboard viewed: admin ID, school ID, timestamp
- Burnout risk viewed: admin ID, school ID, timestamp
- Recommendations viewed: admin ID, school ID, timestamp

### PII Handling

- Teacher name used in admin notifications (necessary for action)
- Teacher name NOT sent to LLM (uses anonymized context)
- No PII shared with external services
- All wellness data encrypted at rest

### Multi-tenant Isolation

- All wellness tables have `school_id` — school-scoped
- All queries filtered by `school_id`
- No cross-school wellness data access

---

## 17. Performance & Scalability

### Expected Scale

- 50-100 teachers per school
- 1 survey per teacher per week = 50-100 surveys/week
- 1 workload snapshot per teacher per week = 50-100 snapshots/week
- Weekly job: 100 teachers × workload calculation = < 5 minutes

### Query Optimization

- Survey history: `idx_teacher_wellness_unique(teacher_id, survey_date)` — paginated
- Workload snapshots: `idx_teacher_workload_unique(teacher_id, week_start_date)` — UNIQUE
- Dashboard: `idx_teacher_workload_school(school_id, week_start_date)` — filtered

### Indexing Strategy

- `teacher_wellness_surveys(teacher_id, survey_date)` — UNIQUE
- `teacher_workload_snapshots(teacher_id, week_start_date)` — UNIQUE
- `teacher_workload_snapshots(school_id, week_start_date)` — dashboard lookup

### Caching Strategy

- Admin dashboard: cached per school, 10-minute TTL
- Burnout risk list: cached per school, 10-minute TTL
- Teacher's own workload: cached per teacher, 5-minute TTL

### Pagination

- Survey history: 20 surveys per page
- Dashboard: all teachers in single response (max ~100)

### Connection Pooling

Uses existing HikariCP connection pool. No additional pooling needed.

### Async Processing

- Weekly workload job: background job (async)
- AI burnout analysis: background job (async, after workload job)
- Notifications: async (fire-and-forget via `Notify.kt`)

### Scalability Concerns

- Weekly job: 100 teachers × DB queries. Very manageable.
- AI analysis: only for flagged teachers (typically < 20% of staff). ~20 AI calls per week.
- DB storage: 100 snapshots/week × 52 weeks = 5,200 records/year × ~0.2KB = ~1MB/year. Negligible.

---

## 18. Edge Cases

| # | Scenario | Expected Behavior |
|---|---|---|
| EC-1 | Teacher submits survey twice same week | UNIQUE constraint prevents duplicate. Return 409. |
| EC-2 | Teacher has no periods assigned | Workload score = 0. periods_per_week = 0. |
| EC-3 | Teacher has no pending grading | pending_grading = 0. Workload score lower. |
| EC-4 | Teacher has no pending homework | pending_homework_review = 0. Workload score lower. |
| EC-5 | All teachers have same workload | No burnout risks flagged. School average = all teachers. |
| EC-6 | Only 1 teacher in school | Can't compare to peers. Workload score calculated but no relative comparison. |
| EC-7 | Teacher newly joined (no history) | First snapshot created. No trend data. AI analysis skipped (insufficient data). |
| EC-8 | AI analysis fails for teacher | Log error. Skip teacher. No risk level assigned. Continue with next. |
| EC-9 | Teacher has no survey data | AI analysis uses workload only. Survey section empty in prompt. |
| EC-10 | School has no teachers | Dashboard shows empty state. No workload data. |
| EC-11 | Workload job fails | Log error. No snapshots created. Next week's job handles it. |
| EC-12 | Teacher views another teacher's workload | Return 403. Teachers can only view own data. |

---

## 19. Error Handling

### Error Response Format

Standard `ApiResponse` error format.

### Error Codes

| Code | HTTP Status | Description | User Message |
|---|---|---|---|
| `ALREADY_SURVEYED` | 409 | Already submitted survey this week | "You've already submitted a survey this week." |
| `NOT_AUTHORIZED` | 403 | Teacher trying to access another teacher's data | "You can only view your own wellness data." |
| `NO_WORKLOAD_DATA` | 404 | No workload snapshot found | "No workload data available yet. Data is calculated every Monday." |
| `INVALID_SURVEY_VALUE` | 400 | Survey value not 1-5 | "Survey values must be between 1 and 5." |
| `AI_ANALYSIS_FAILED` | 500 | AI analysis failed | (Internal — logged, teacher skipped in job) |

### Error Handling Strategy

- **Survey submission:** Return 409 on duplicate. Return 400 on invalid input.
- **Workload access:** Return 403 if teacher tries to access another's data. Return 404 if no snapshot.
- **Dashboard:** Return empty state if no data (not an error).
- **AI analysis:** Log error per teacher. Continue with next. No 500 — it's a batch job.

### Retry Strategy

- Survey submission: client retries on network error (3 attempts)
- Weekly workload job: no retry (next week's job handles it)
- AI analysis job: no retry (next week's job handles it)

### Fallback Behavior

- No workload data: empty dashboard
- AI analysis fails: no risk level for that teacher
- No survey data: AI uses workload only
- No teachers: empty dashboard

---

## 20. Analytics & Reporting

### Analytics Dashboard Data

| Metric | Source | Derivation |
|---|---|---|
| Survey participation rate | `teacher_wellness_surveys` | Teachers with survey this week / total teachers |
| Average stress level | `teacher_wellness_surveys` | Avg stress_level for current week |
| Average workload score | `teacher_workload_snapshots` | Avg workload_score for current week |
| Burnout risk count | AI analysis | Count by risk level (high/medium/low) |
| Workload distribution | `teacher_workload_snapshots` | Histogram of workload scores |
| Teachers above average | `teacher_workload_snapshots` | Count where score > school average |
| Survey trend | `teacher_wellness_surveys` | Avg scores over time (4-week trend) |

### Export Capabilities

- Workload report (CSV) — teacher, workload score, periods, pending grading, pending homework
- Burnout risk report (CSV) — teacher, risk level, AI explanation, recommendations
- Survey summary (CSV) — teacher, stress, workload, satisfaction, date

### Report Types

| Report | Format | Frequency | Recipient |
|---|---|---|---|
| Workload overview | JSON (API) | Weekly | Admin |
| Burnout risk report | JSON (API) | Weekly | Admin |
| Survey participation | JSON (API) | Weekly | Product Team |
| AI accuracy | JSON (API) | Monthly | Dev Team |

---

## 21. Testing Strategy

### Unit Tests

- `TeacherWellnessService.submitSurvey()` — validation, UNIQUE constraint
- `TeacherWellnessService.calculateWorkload()` — workload score calculation
- `TeacherWellnessService.getWorkloadDashboard()` — aggregation, distribution
- `TeacherWellnessService.getBurnoutRisk()` — risk level determination
- `TeacherWellnessService.getBalancingRecommendations()` — AI response parsing
- Workload score formula — weighted combination verification
- AI prompt building — context assembly, anonymization

### Integration Tests

- Full survey flow: submit survey → verify in DB → verify in history
- Full workload flow: set up periods/grading/homework → run job → verify snapshot
- Dashboard flow: create snapshots → get dashboard → verify aggregation
- Burnout risk flow: set up high workload → run job → verify risk flagged
- Privacy: teacher tries to access another teacher's data → verify 403
- AI analysis: set up flagged teacher → run AI job → verify risk level and explanation

### E2E Tests

- Teacher submits weekly survey → admin sees in dashboard
- Weekly job runs → admin sees workload distribution
- Teacher flagged as burnout risk → admin receives notification
- Admin views balancing recommendations

### Performance Tests

- Weekly workload job: < 5 minutes for 100 teachers
- AI burnout analysis: < 30 seconds for 20 flagged teachers
- Dashboard load: < 3 seconds
- Survey submission: < 2 seconds

### Test Data

- 20 teachers with varying workloads (high, normal, low)
- 4 weeks of workload snapshots
- 4 weeks of survey data
- Teachers with no survey data (edge case)
- Teachers with no periods (edge case)
- Mock LLM (returns controlled risk levels and recommendations)

### Test Environment

- Test database with teacher wellness tables
- Pre-seeded teacher, period, assessment, homework data
- Mock LLM (returns controlled responses)
- Test JWT tokens for teacher and admin roles

---

## 22. Acceptance Criteria

- [ ] Workload auto-calculated weekly
- [ ] Teacher self-assessment submitted weekly
- [ ] Admin dashboard shows workload distribution
- [ ] Burnout risk indicators flag overburdened teachers
- [ ] AI balancing recommendations generated
- [ ] Privacy: self-assessment not visible to other teachers
- [ ] Teachers can only view own wellness data
- [ ] One survey per teacher per week enforced
- [ ] One workload snapshot per teacher per week created
- [ ] Admin receives notification for high-risk teachers
- [ ] Weekly survey reminder sent to teachers

---

## 23. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration `migration_082_teacher_wellness.sql`, Exposed tables, register in `DatabaseFactory` |
| 2 | 2 days | Workload calculation + weekly job (`WeeklyWorkloadJob`) |
| 3 | 2 days | AI analysis + recommendations (`BurnoutAnalysisJob`, AI prompts) |
| 4 | 1 day | API endpoints |
| 5 | 2 days | Client UI: `WellnessSurveyScreen` (teacher), `MyWorkloadScreen` (teacher), `TeacherWellnessScreen` (admin) |
| 6 | 1 day | Tests: unit, integration, E2E |

### Pre-Implementation Checklist

- [ ] Verify `AI_INFRASTRUCTURE_SPEC.md` is implemented and `AiService` available
- [ ] Verify `TeacherPeriodsTable` exists and has data
- [ ] Verify `AssessmentMarksTable` has status field (draft/published)
- [ ] Verify `HomeworkSubmissionsTable` has status field (SUBMITTED/REVIEWED)
- [ ] Verify `Notify.kt` supports "teacher_wellness" category
- [ ] Verify admin role exists in user system

---

## 24. File-Level Impact Analysis

### Server

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | `TeacherWellnessSurveysTable`, `TeacherWorkloadSnapshotsTable` |
| `server/.../db/DatabaseFactory.kt` | Modify | Register 2 teacher wellness tables in `allTables` |
| `server/.../feature/wellness/TeacherWellnessService.kt` | **New** | Core service (survey, workload, dashboard, burnout risk, recommendations) |
| `server/.../feature/wellness/WeeklyWorkloadJob.kt` | **New** | Weekly workload calculation job |
| `server/.../feature/wellness/BurnoutAnalysisJob.kt` | **New** | Weekly AI burnout analysis job |
| `server/.../feature/wellness/TeacherWellnessRouting.kt` | **New** | API endpoints |
| `docs/db/migration_082_teacher_wellness.sql` | **New** | DDL |

### Shared (KMP)

| File | Change Type | Description |
|---|---|---|
| `shared/.../wellness/domain/model/TeacherWellnessModels.kt` | **New** | DTOs, domain models, enums |
| `shared/.../wellness/domain/repository/TeacherWellnessRepository.kt` | **New** | Repository interfaces |
| `shared/.../wellness/data/remote/TeacherWellnessApi.kt` | **New** | HTTP API definitions |

### Client (Compose)

| File | Change Type | Description |
|---|---|---|
| `composeApp/.../ui/v2/screens/teacher/WellnessSurveyScreen.kt` | **New** | Self-assessment survey |
| `composeApp/.../ui/v2/screens/teacher/MyWorkloadScreen.kt` | **New** | Teacher's own workload view |
| `composeApp/.../ui/v2/screens/admin/TeacherWellnessScreen.kt` | **New** | Admin dashboard |

---

## 25. Future Enhancements

| # | Enhancement | Priority | Effort | Notes |
|---|---|---|---|---|
| F-1 | Workload auto-balancing | Medium | L | AI suggests period reassignment, admin approves |
| F-2 | Teacher wellness resources | Low | S | Curated wellness resources for teachers |
| F-3 | Peer support groups | Low | M | Anonymous teacher support groups |
| F-4 | Wellness trend alerts | Medium | S | Alert admin if teacher's wellness declining over time |
| F-5 | Teacher feedback on recommendations | Low | S | Teacher can rate usefulness of AI recommendations |
| F-6 | Integration with leave management | Medium | M | Correlate wellness with leave taken |
| F-7 | Multi-teacher comparison (anonymized) | Low | S | Show where teacher stands relative to peers (anonymized) |
| F-8 | Wellness gamification (positive reinforcement) | Low | M | Rewards for consistent survey participation |
| F-9 | Custom wellness dimensions | Low | S | Admin can add custom survey dimensions |
| F-10 | Real-time workload tracking | Low | L | Move from weekly snapshots to real-time workload |

---

## Appendix A: Sequence Diagrams

### A.1 Weekly Workload Job Flow

```
WeeklyWorkloadJob    Server          DB
  │                    │               │
  │  execute()         │               │
  │  ───────────────>  │               │
  │                    │──get all teachers──>│
  │                    │←──teacher list──────│
  │                    │               │
  │                    │──for each teacher:  │
  │                    │  count periods──────>│
  │                    │  count pending grading>│
  │                    │  count pending homework>│
  │                    │  calculate score     │
  │                    │  store snapshot──────>│
  │                    │←──success────────────│
  │                    │               │
  │                    │  identify outliers   │
  │                    │  (score > 1.5x avg) │
  │                    │               │
  │  ←──count created──│               │
  │                    │               │
```

### A.2 Admin Dashboard Flow

```
Admin (app)         Server              DB
  │                   │                   │
  │  GET /dashboard   │                   │
  │  ──────────────>  │                   │
  │                   │──get snapshots────>│
  │                   │←──snapshot list────│
  │                   │                   │
  │                   │──calculate avg─────>│
  │                   │←──avg score────────│
  │                   │                   │
  │                   │──get burnout risks>│
  │                   │←──risk list────────│
  │                   │                   │
  │                   │  build DTO        │
  │  ←──200: Dashboard│                   │
  │                   │                   │
```

---

## Appendix B: Domain Model / ER Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                  teacher_wellness_surveys (new)                       │
│  id (PK)                                                              │
│  school_id, teacher_id                                                │
│  survey_date                                                          │
│  stress_level, workload_manageability, job_satisfaction (all 1-5)     │
│  notes                                                                │
│  created_at                                                           │
│  UNIQUE: (teacher_id, survey_date)                                    │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│                teacher_workload_snapshots (new)                       │
│  id (PK)                                                              │
│  school_id, teacher_id                                                │
│  week_start_date                                                      │
│  periods_per_week, pending_grading, pending_homework_review,          │
│  classes_taught, subjects_taught                                      │
│  workload_score (REAL, composite)                                     │
│  created_at                                                           │
│  UNIQUE: (teacher_id, week_start_date)                                │
│  INDEX: (school_id, week_start_date)                                  │
└──────────────────────────────────────────────────────────────────────┘

Existing tables used (read-only):
┌────────────────────┐  ┌──────────────────┐  ┌────────────────────────┐
│ teacher_periods    │  │ assessment_marks │  │ homework_submissions   │
│ (existing)         │  │ (existing,       │  │ (existing,             │
│                    │  │  status=draft)   │  │  status=SUBMITTED)     │
└────────────────────┘  └──────────────────┘  └────────────────────────┘
┌──────────────────┐  ┌──────────────────┐
│ app_users        │  │ schools          │
│ (existing,       │  │ (existing)       │
│  role=teacher)   │  │                  │
└──────────────────┘  └──────────────────┘
```

---

## Appendix C: Event Flow

### Domain Events

| Event | Emitter | Consumers | Payload | Side Effects |
|---|---|---|---|---|
| `TeacherSurveySubmitted` | `TeacherWellnessService.submitSurvey()` | None (logged) | `teacherId, stressLevel, workloadManageability, jobSatisfaction` | Survey recorded |
| `WorkloadSnapshotCreated` | `WeeklyWorkloadJob` | None (logged) | `teacherId, workloadScore, weekStartDate` | Snapshot stored |
| `BurnoutRiskDetected` | `BurnoutAnalysisJob` | `Notify.kt` | `teacherId, riskLevel, aiExplanation` | Notification to admin |
| `BalancingRecommendationsGenerated` | `TeacherWellnessService.getBalancingRecommendations()` | None (logged) | `schoolId, recommendationCount` | Recommendations available |

### Event Delivery Guarantees

- Survey submitted event: emitted synchronously
- Workload snapshot event: emitted within job execution
- Burnout risk event: emitted within job execution, notification async
- Recommendations event: emitted on-demand (when admin requests)

---

## Appendix D: Configuration

### Environment Variables

| Variable | Default | Description |
|---|---|---|
| `TEACHER_WELLNESS_ENABLED` | `true` | Enable/disable teacher wellness feature |
| `TEACHER_WELLNESS_BURNOUT_THRESHOLD` | `1.5` | Multiplier for burnout risk flag |
| `TEACHER_WELLNESS_HIGH_RISK_THRESHOLD` | `2.0` | Multiplier for high risk flag |
| `TEACHER_WELLNESS_AI_MODEL` | `teacher_wellness_v1` | AI model for burnout analysis |
| `TEACHER_WELLNESS_SURVEY_REMINDER_ENABLED` | `true` | Enable weekly survey reminder |

### Feature Flags

| Flag | Default | Description |
|---|---|---|
| `TEACHER_WELLNESS_ENABLED` | `true` | Enable/disable teacher wellness |
| `TEACHER_WELLNESS_AI_ENABLED` | `true` | Enable/disable AI analysis |

### School-Level Settings

| Setting | Default | Description |
|---|---|---|
| `teacher_wellness_enabled` | `true` | Per-school enable/disable |
| `burnout_threshold` | `1.5` | Per-school burnout threshold |

---

## Appendix E: Migration & Rollback

### Migration: `migration_082_teacher_wellness.sql`

```sql
-- Migration 082: Teacher Wellness
-- Creates teacher_wellness_surveys and teacher_workload_snapshots tables

BEGIN;

CREATE TABLE IF NOT EXISTS teacher_wellness_surveys (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    teacher_id      UUID NOT NULL,
    survey_date     DATE NOT NULL,
    stress_level    INTEGER NOT NULL,
    workload_manageability INTEGER NOT NULL,
    job_satisfaction INTEGER NOT NULL,
    notes           TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(teacher_id, survey_date)
);

CREATE TABLE IF NOT EXISTS teacher_workload_snapshots (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    teacher_id      UUID NOT NULL,
    week_start_date DATE NOT NULL,
    periods_per_week INTEGER NOT NULL,
    pending_grading INTEGER NOT NULL,
    pending_homework_review INTEGER NOT NULL,
    classes_taught  INTEGER NOT NULL,
    subjects_taught INTEGER NOT NULL,
    workload_score  REAL NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(teacher_id, week_start_date)
);

CREATE INDEX IF NOT EXISTS idx_teacher_workload_school
    ON teacher_workload_snapshots (school_id, week_start_date);

COMMIT;
```

### Rollback: `migration_082_rollback.sql`

```sql
BEGIN;
DROP TABLE IF EXISTS teacher_workload_snapshots;
DROP TABLE IF EXISTS teacher_wellness_surveys;
COMMIT;
```

### Migration Validation

- Verify 2 tables created with correct columns
- Verify `teacher_wellness_surveys(teacher_id, survey_date)` UNIQUE constraint
- Verify `teacher_workload_snapshots(teacher_id, week_start_date)` UNIQUE constraint
- Verify index on `teacher_workload_snapshots(school_id, week_start_date)`
- Run `SELECT count(*) FROM teacher_wellness_surveys` — should be 0 (new feature)

---

## Appendix F: Observability

### Structured Logging

| Log Level | Event | Context Fields |
|---|---|---|
| INFO | Survey submitted | `teacherId, stressLevel, workloadManageability, jobSatisfaction` |
| INFO | Workload snapshot created | `teacherId, workloadScore, weekStartDate` |
| INFO | Burnout risk detected | `teacherId, riskLevel, workloadScore` |
| INFO | Recommendations generated | `schoolId, recommendationCount` |
| INFO | Weekly workload job completed | `teacherCount, flaggedCount, duration` |
| INFO | Burnout analysis job completed | `flaggedCount, analyzedCount, duration` |
| INFO | Dashboard viewed | `adminId, schoolId` |
| WARN | Teacher has no periods | `teacherId` |
| WARN | AI analysis failed for teacher | `teacherId, error` |
| WARN | No survey data for teacher | `teacherId` |
| ERROR | Weekly workload job failed | `error` |
| ERROR | Burnout analysis job failed | `error` |

### Metrics

| Metric | Type | Labels | Description |
|---|---|---|---|
| `teacher_wellness_surveys_total` | Counter | `school_id` | Total surveys submitted |
| `teacher_workload_snapshots_total` | Counter | `school_id` | Total snapshots created |
| `teacher_burnout_risks_detected` | Counter | `school_id, risk_level` | Total burnout risks detected |
| `teacher_workload_job_duration` | Histogram | `school_id` | Weekly workload job duration |
| `teacher_burnout_analysis_duration` | Histogram | `school_id` | Burnout analysis job duration |
| `teacher_avg_workload_score` | Gauge | `school_id` | Average teacher workload score |
| `teacher_survey_participation_rate` | Gauge | `school_id` | Survey participation rate |

### Health Checks

| Check | Endpoint | Description |
|---|---|---|
| Teacher wellness service | `/health/teacher-wellness` | Verify service and DB accessible |
| Weekly workload job | `/health/weekly-workload` | Verify last job run and status |

### Alerts

| Alert | Condition | Severity | Notification |
|---|---|---|---|
| Weekly workload job failed | Job error rate > 10% | Warning | Email to dev team |
| Burnout analysis job failed | Job error rate > 10% | Warning | Email to dev team |
| High burnout risk count | > 20% of teachers | Warning | Email to admin |
| Survey participation low | < 30% for 2+ weeks | Info | Email to admin |
| AI analysis failure rate | > 20% | Warning | Email to dev team |

### Dashboards

| Dashboard | Panels | Audience |
|---|---|---|
| Teacher Wellness Overview | Survey participation, avg stress, avg workload, burnout risks | Product Team |
| Workload Distribution | Workload scores by teacher, distribution histogram | Admin |
| Job Performance | Workload job duration, AI analysis duration, error rate | Dev Team |
| Burnout Risk Monitoring | Risk count by level, trend over time, flagged teachers | Admin |

### Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Wellness data used for performance evaluation | Medium | High | Clear policy. Wellness ≠ performance. Access audit. |
| Privacy breach (survey data exposed) | Low | High | Role-based access. Teacher can only see own data. Encryption. |
| AI false positive (flag non-burnt teacher) | Medium | Low | Admin reviews. AI is advisory. Can dismiss. |
| AI false negative (miss burnt teacher) | Medium | Medium | Weekly job. Multiple data sources. Admin can manually review. |
| Teacher gaming survey | Low | Low | Workload data cross-validates. AI considers both. |
| Admin overwhelmed with alerts | Low | Low | Only high-risk triggers FCM. Medium = in-app only. |
| AI sends teacher name to LLM | Low | High | Anonymized IDs in AI prompt. No PII in LLM context. |
