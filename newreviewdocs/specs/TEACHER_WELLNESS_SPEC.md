# Teacher Wellness — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** `AI_INFRASTRUCTURE_SPEC.md`
> **Source:** `DIFFERENTIATING_FEATURES.md` §5.2

---

## 1. Feature Overview

Teacher wellness and burnout prevention: workload tracking, self-assessment, AI-powered workload balance insights, and admin alerts for overburdened teachers.

### Goals

- Track teacher workload: classes per day, assessments to grade, homework to review
- Weekly self-assessment: stress, workload manageability, job satisfaction
- AI workload analysis: identify teachers with excessive load
- Admin dashboard: teacher workload distribution, burnout risk indicators
- Workload balancing recommendations

---

## 2. Current System Assessment

- `TeacherPeriodsTable` — teaching schedule
- `HomeworkTable` — homework assigned by teacher
- `AssessmentMarksTable` — marks to grade
- `TeacherCheckInsTable` — attendance check-ins
- No wellness tracking for teachers

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Auto-calculate workload: periods/day, pending grading, pending homework review |
| FR-2 | Weekly self-assessment: stress (1-5), workload manageability (1-5), satisfaction (1-5) |
| FR-3 | AI workload analysis: compare actual load vs school average, flag outliers |
| FR-4 | Admin dashboard: workload distribution, burnout risk indicators |
| FR-5 | Workload balancing recommendations (AI-generated) |
| FR-6 | Privacy: self-assessment visible only to teacher + admin (not other teachers) |

---

## 4. Database Design

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

---

## 5. Backend Architecture

### 5.1 TeacherWellnessService

```kotlin
class TeacherWellnessService(private val aiService: AiService) {
    suspend fun submitSurvey(teacherId: UUID, survey: SurveyDto)
    suspend fun calculateWorkload(teacherId: UUID, weekStart: LocalDate): WorkloadDto
    suspend fun getWorkloadDashboard(schoolId: UUID): WorkloadDashboardDto  // all teachers
    suspend fun getBurnoutRisk(schoolId: UUID): List<BurnoutRiskDto>
    suspend fun getBalancingRecommendations(schoolId: UUID): List<RecommendationDto>
}
```

### 5.2 Weekly Workload Job

Every Monday 6 AM:
1. For each teacher: calculate workload from `TeacherPeriodsTable`, pending `AssessmentMarksTable` (status=draft), pending `HomeworkSubmissionsTable` (status=SUBMITTED)
2. Store in `teacher_workload_snapshots`
3. Identify teachers with workload_score > 1.5x school average → flag

---

## 6. API Contracts

```
# Teacher
POST /api/v1/teacher/wellness/survey
GET /api/v1/teacher/wellness/my-workload

# Admin
GET /api/v1/school/wellness/teacher-dashboard
GET /api/v1/school/wellness/burnout-risk
GET /api/v1/school/wellness/balancing-recommendations
```

---

## 7. Acceptance Criteria

- [ ] Workload auto-calculated weekly
- [ ] Teacher self-assessment submitted weekly
- [ ] Admin dashboard shows workload distribution
- [ ] Burnout risk indicators flag overburdened teachers
- [ ] AI balancing recommendations generated
- [ ] Privacy: self-assessment not visible to other teachers

---

## 8. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration, Exposed tables |
| 2 | 2 days | Workload calculation + weekly job |
| 3 | 2 days | AI analysis + recommendations |
| 4 | 1 day | API endpoints |
| 5 | 2 days | Client UI (teacher survey, admin dashboard) |
| 6 | 1 day | Tests |

---

## 9. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | 2 teacher wellness tables |
| `server/.../feature/wellness/TeacherWellnessService.kt` | New | Core service |
| `docs/db/migration_082_teacher_wellness.sql` | New | DDL |
| `composeApp/.../ui/v2/screens/teacher/WellnessSurveyScreen.kt` | New | Self-assessment |
| `composeApp/.../ui/v2/screens/admin/TeacherWellnessScreen.kt` | New | Admin dashboard |
