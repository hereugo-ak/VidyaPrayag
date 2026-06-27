# Student Wellness Tracking — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** `AI_INFRASTRUCTURE_SPEC.md`, `HEALTH_RECORDS_SPEC.md`
> **Source:** `DIFFERENTIATING_FEATURES.md` §3.1

---

## 1. Feature Overview

Holistic student wellness tracking: mood check-ins, stress indicators, sleep patterns, physical activity, and AI-powered early warning system for mental health concerns. Alerts counselors/teachers when patterns indicate potential issues.

### Goals

- Daily/weekly mood check-in (emoji-based, quick tap)
- Wellness survey: stress level, sleep quality, appetite, social interaction
- Teacher observation logging (behavioral indicators)
- AI early warning: detect declining wellness patterns → alert counselor
- Wellness dashboard for counselors/admin
- Parent-friendly wellness summary (opt-in, non-clinical)
- Privacy-first: sensitive data restricted to authorized staff

---

## 2. Current System Assessment

- `HEALTH_RECORDS_SPEC.md` — physical health records (allergies, immunizations, incidents)
- `HolisticAssessmentsTable` (from `NEP_COMPLIANCE_SPEC.md`) — includes 360-degree assessment
- No wellness/mood tracking exists
- `DIFFERENTIATING_FEATURES.md` §3.1: Student Wellness, effort L, data readiness: "Holistic assessment exists, no wellness data"

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Mood check-in: student taps emoji (happy/okay/sad/anxious/angry) — daily or per-class |
| FR-2 | Weekly wellness survey: 5 questions (stress, sleep, appetite, social, energy) — 1-5 scale |
| FR-3 | Teacher observation: teacher logs behavioral notes (withdrawn, disruptive, engaged, tired) |
| FR-4 | AI early warning: analyze mood + survey + observation trends → flag declining patterns |
| FR-5 | Alert counselor/admin when student flagged (FCM + in-app) |
| FR-6 | Wellness dashboard: aggregated class wellness, individual student timeline |
| FR-7 | Parent wellness summary: weekly non-clinical summary (opt-in) |
| FR-8 | Privacy: wellness data visible only to student's teacher, counselor, admin — not parents by default |
| FR-9 | Intervention tracking: counselor logs actions taken for flagged students |

---

## 4. Database Design

```sql
CREATE TABLE wellness_checkins (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    student_id      UUID NOT NULL,
    mood            VARCHAR(16) NOT NULL,          -- happy | okay | sad | anxious | angry
    checkin_date    DATE NOT NULL,
    notes           TEXT,                          -- optional student note
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(student_id, checkin_date)
);
CREATE INDEX idx_wellness_checkins_student ON wellness_checkins(student_id, checkin_date DESC);

CREATE TABLE wellness_surveys (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    student_id      UUID NOT NULL,
    survey_date     DATE NOT NULL,
    stress_level    INTEGER NOT NULL,              -- 1-5
    sleep_quality   INTEGER NOT NULL,              -- 1-5
    appetite        INTEGER NOT NULL,              -- 1-5
    social_interaction INTEGER NOT NULL,           -- 1-5
    energy_level    INTEGER NOT NULL,              -- 1-5
    overall_score   REAL NOT NULL,                 -- avg of 5 dimensions
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(student_id, survey_date)
);

CREATE TABLE teacher_observations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    student_id      UUID NOT NULL,
    teacher_id      UUID NOT NULL,
    observation_date DATE NOT NULL,
    behavior        VARCHAR(32) NOT NULL,          -- engaged | withdrawn | disruptive | tired | anxious | happy | focused
    notes           TEXT,
    severity        VARCHAR(16) NOT NULL DEFAULT 'normal', -- normal | concern | urgent
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_teacher_obs_student ON teacher_observations(student_id, observation_date DESC);

CREATE TABLE wellness_alerts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    student_id      UUID NOT NULL,
    alert_type      VARCHAR(32) NOT NULL,          -- declining_mood | low_survey_score | teacher_concern | pattern_change
    severity        VARCHAR(16) NOT NULL,          -- low | medium | high
    trigger_data    TEXT NOT NULL,                 -- JSON: what triggered the alert
    ai_analysis     TEXT,                          -- AI-generated explanation
    status          VARCHAR(16) NOT NULL DEFAULT 'open', -- open | acknowledged | intervened | resolved
    acknowledged_by UUID,
    acknowledged_at TIMESTAMP,
    intervention_notes TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_wellness_alerts_school ON wellness_alerts(school_id, status, created_at DESC);
```

---

## 5. Backend Architecture

### 5.1 WellnessService

```kotlin
class WellnessService(private val aiService: AiService) {
    suspend fun checkin(studentId: UUID, mood: String, notes: String?)
    suspend fun submitSurvey(studentId: UUID, survey: WellnessSurveyDto)
    suspend fun logObservation(teacherId: UUID, studentId: UUID, observation: ObservationDto)
    suspend fun getStudentTimeline(studentId: UUID, range: TimeRange): WellnessTimelineDto
    suspend fun getClassDashboard(classId: UUID): ClassWellnessDto  // aggregated
    suspend fun runEarlyWarning(): Int  // daily AI job
    suspend fun acknowledgeAlert(alertId: UUID, userId: UUID)
    suspend fun logIntervention(alertId: UUID, notes: String)
}
```

### 5.2 AI Early Warning Job

Daily 6 AM IST:
1. For each student with ≥ 5 check-ins in last 14 days:
   - Analyze mood trend (declining? consistently negative?)
   - Check survey scores (overall < 2.5? declining trend?)
   - Check teacher observations (concern/urgent in last 7 days?)
2. If concerning pattern detected:
   - Call AI to analyze and generate explanation
   - Create `wellness_alert` with severity
   - Notify counselor/admin via FCM

### 5.3 AI Prompt

```
System: You are a student wellness analyst. Analyze the following wellness data and determine if an alert is needed.
Consider: trend direction, consistency, severity of negative indicators.
If alert needed, specify severity (low/medium/high) and provide a brief explanation.

Data:
- Mood trend (last 14 days): {{mood_sequence}}
- Survey scores: {{survey_data}}
- Teacher observations: {{observations}}
- Previous alerts: {{previous_alerts}}

Output JSON: {"alert_needed": true/false, "severity": "...", "explanation": "..."}
```

---

## 6. API Contracts

```
# Student (via parent app or future student app)
POST /api/v1/parent/wellness/checkin  { student_id, mood, notes }
POST /api/v1/parent/wellness/survey  { student_id, ... }

# Teacher
POST /api/v1/teacher/wellness/observation  { student_id, behavior, notes, severity }
GET /api/v1/teacher/wellness/class/{classId}

# Counselor/Admin
GET /api/v1/school/wellness/alerts?status=open
POST /api/v1/school/wellness/alerts/{id}/acknowledge
POST /api/v1/school/wellness/alerts/{id}/intervene  { notes }
GET /api/v1/school/wellness/dashboard
GET /api/v1/school/wellness/student/{studentId}/timeline

# Parent (opt-in)
GET /api/v1/parent/wellness/summary/{childId}
```

---

## 7. Privacy & Security

- Wellness data (check-ins, surveys, observations) visible only to:
  - The student themselves
  - Student's class teacher
  - School counselor
  - School admin
- Parents do NOT see raw wellness data by default
- Parent wellness summary is non-clinical ("Aarav seems to be doing well this week" or "Aarav might benefit from more rest")
- All access logged in audit log
- Wellness alerts are confidential

---

## 8. Acceptance Criteria

- [ ] Student can do daily mood check-in
- [ ] Weekly wellness survey submitted
- [ ] Teacher can log behavioral observations
- [ ] AI early warning detects declining patterns
- [ ] Alerts sent to counselor/admin
- [ ] Wellness dashboard shows class and individual data
- [ ] Intervention tracking works
- [ ] Parent summary (opt-in, non-clinical)
- [ ] Privacy: data restricted to authorized staff

---

## 9. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 2 days | DB migration, Exposed tables |
| 2 | 2 days | WellnessService (checkin, survey, observation) |
| 3 | 3 days | AI early warning engine + daily job |
| 4 | 1 day | Alert management + intervention tracking |
| 5 | 1 day | API endpoints |
| 6 | 4 days | Client UI (mood check-in, survey, teacher observation, counselor dashboard, parent summary) |
| 7 | 2 days | Tests |

---

## 10. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | 4 wellness tables |
| `server/.../feature/wellness/WellnessService.kt` | New | Core service |
| `server/.../feature/wellness/EarlyWarningJob.kt` | New | AI early warning job |
| `server/.../feature/wellness/WellnessRouting.kt` | New | API endpoints |
| `docs/db/migration_080_student_wellness.sql` | New | DDL |
| `composeApp/.../ui/v2/screens/parent/MoodCheckInScreen.kt` | New | Mood check-in |
| `composeApp/.../ui/v2/screens/teacher/WellnessObservationScreen.kt` | New | Teacher observation |
| `composeApp/.../ui/v2/screens/admin/WellnessDashboardScreen.kt` | New | Counselor dashboard |
