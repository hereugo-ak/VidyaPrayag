# Student Wellness Tracking — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** `AI_INFRASTRUCTURE_SPEC.md`, `HEALTH_RECORDS_SPEC.md`
> **Source:** `DIFFERENTIATING_FEATURES.md` §3.1
> **Template:** `_SPEC_TEMPLATE.md` v1 (25 mandatory + 6 optional sections)

---

## 1. Feature Overview

### What

Holistic student wellness tracking: mood check-ins, stress indicators, sleep patterns, physical activity, and AI-powered early warning system for mental health concerns. Alerts counselors/teachers when patterns indicate potential issues.

### Why — Product Rationale

Student mental health is a growing concern in Indian schools. Teachers and counselors often lack visibility into student wellness patterns until issues escalate. A wellness tracking system with AI-powered early warning enables proactive intervention — catching declining patterns before they become crises.

This is a **high-priority differentiating feature** (Phase 3, effort L, "High" value per `DIFFERENTIATING_FEATURES.md`). It's unique among Indian school ERPs and addresses a critical need.

### What Stands Out (Competitive Moat)

From `DIFFERENTIATING_FEATURES.md` §3.1:
> "Student Wellness — mood tracking, AI early warning, counselor alerts. Data readiness: Holistic assessment exists, no wellness data."

No major Indian school ERP offers AI-powered wellness tracking with early warning. The key moat is **AI-powered pattern detection** — not just data collection, but proactive alerting.

### Goals

- Daily/weekly mood check-in (emoji-based, quick tap)
- Wellness survey: stress level, sleep quality, appetite, social interaction, energy
- Teacher observation logging (behavioral indicators)
- AI early warning: detect declining wellness patterns → alert counselor
- Wellness dashboard for counselors/admin
- Parent-friendly wellness summary (opt-in, non-clinical)
- Privacy-first: sensitive data restricted to authorized staff
- Intervention tracking: counselor logs actions taken for flagged students

### Non-goals

- [ ] Clinical diagnosis (wellness tracking is observational, not diagnostic)
- [ ] Direct therapy/counseling through the app (referral to external professionals only)
- [ ] Real-time crisis intervention (call emergency services — out of scope)
- [ ] Student-to-student wellness sharing (privacy concerns)
- [ ] Wearable device integration (future enhancement)
- [ ] Parent access to raw wellness data (non-clinical summary only, opt-in)

### Dependencies

- `AI_INFRASTRUCTURE_SPEC.md` — AI service for early warning analysis
- `HEALTH_RECORDS_SPEC.md` — physical health records (allergies, immunizations, incidents)
- `HolisticAssessmentsTable` (from `NEP_COMPLIANCE_SPEC.md`) — 360-degree assessment
- `StudentsTable` — student info
- `UsersTable` — teacher, counselor, admin, parent accounts
- `ClassesTable` — class info for class-level dashboard
- `Notify.kt` — notification dispatch

### Related Modules

- `server/.../feature/wellness/` — new wellness module
- `server/.../feature/health/` — existing health records module
- `composeApp/.../ui/v2/screens/parent/` — mood check-in UI
- `composeApp/.../ui/v2/screens/teacher/` — observation UI
- `composeApp/.../ui/v2/screens/admin/` — counselor dashboard UI

---

## 2. Current System Assessment

### Existing Code

- `HEALTH_RECORDS_SPEC.md` — physical health records (allergies, immunizations, incidents)
- `HolisticAssessmentsTable` (from `NEP_COMPLIANCE_SPEC.md`) — includes 360-degree assessment
- No wellness/mood tracking exists
- `DIFFERENTIATING_FEATURES.md` §3.1: Student Wellness, effort L, data readiness: "Holistic assessment exists, no wellness data"

### Existing Database

- `HolisticAssessmentsTable` — 360-degree assessment data (academic, co-scholastic, behavioral)
- `HealthRecordsTable` — physical health records
- `StudentsTable` — student info
- `UsersTable` — user accounts (teachers, counselors, admins, parents)
- `ClassesTable` — class info
- No wellness check-in, survey, observation, or alert tables exist

### Existing APIs

- Health records API (existing) — physical health management
- Holistic assessment API (existing) — 360-degree assessment
- No wellness tracking API exists

### Existing UI

- Health records screen (existing) — physical health management
- Holistic assessment screen (existing) — 360-degree assessment
- No mood check-in, wellness survey, observation, or dashboard UI exists

### Existing Services

- No wellness service
- No early warning engine
- No intervention tracking

### Existing Documentation

- `DIFFERENTIATING_FEATURES.md` §3.1 — Student Wellness
- `HEALTH_RECORDS_SPEC.md` — physical health records
- `NEP_COMPLIANCE_SPEC.md` — holistic assessments
- `AI_INFRASTRUCTURE_SPEC.md` — AI service

### Technical Debt

| # | Gap | Details |
|---|---|---|
| TD-1 | No wellness tables | No `wellness_checkins`, `wellness_surveys`, `teacher_observations`, `wellness_alerts` tables |
| TD-2 | No mood tracking | No check-in system for student mood |
| TD-3 | No wellness surveys | No structured wellness survey system |
| TD-4 | No teacher observation logging | No behavioral observation recording |
| TD-5 | No early warning engine | No AI-powered pattern detection |
| TD-6 | No alert system | No wellness alert management |
| TD-7 | No intervention tracking | No counselor action logging |
| TD-8 | No wellness dashboard | No aggregated class/school wellness view |

### Gaps

| # | Gap | Impact | Severity |
|---|---|---|---|
| G1 | No mood tracking | Can't monitor student emotional state | **High** |
| G2 | No wellness surveys | Can't assess stress, sleep, social factors | **High** |
| G3 | No teacher observations | Can't capture behavioral indicators | **High** |
| G4 | No early warning | Issues detected too late, after escalation | **Critical** |
| G5 | No alert system | Counselors not notified of concerns | **High** |
| G6 | No intervention tracking | Can't track actions taken for flagged students | **Medium** |
| G7 | No parent summary | Parents lack awareness of child's wellness | **Medium** |

---

## 3. Functional Requirements

### FR-001
| Field | Value |
|---|---|
| **Title** | Mood Check-in |
| **Description** | Mood check-in: student taps emoji (happy/okay/sad/anxious/angry) — daily or per-class. |
| **Priority** | High |
| **User Roles** | Student (via parent app or future student app) |
| **Acceptance notes** | Quick tap interface. 5 mood options. One check-in per day per student (UNIQUE constraint). Optional text note. |

### FR-002
| Field | Value |
|---|---|
| **Title** | Weekly Wellness Survey |
| **Description** | Weekly wellness survey: 5 questions (stress, sleep, appetite, social, energy) — 1-5 scale. |
| **Priority** | High |
| **User Roles** | Student (via parent app or future student app) |
| **Acceptance notes** | 5 dimensions, each 1-5 scale. Overall score = average of 5 dimensions. One survey per week per student (UNIQUE on student_id + survey_date). |

### FR-003
| Field | Value |
|---|---|
| **Title** | Teacher Observation |
| **Description** | Teacher observation: teacher logs behavioral notes (withdrawn, disruptive, engaged, tired, anxious, happy, focused). |
| **Priority** | High |
| **User Roles** | Teacher |
| **Acceptance notes** | Teacher selects behavior type, adds optional notes, sets severity (normal/concern/urgent). Multiple observations per student per day allowed. |

### FR-004
| Field | Value |
|---|---|
| **Title** | AI Early Warning |
| **Description** | AI early warning: analyze mood + survey + observation trends → flag declining patterns. |
| **Priority** | Critical |
| **User Roles** | System |
| **Acceptance notes** | Daily job (6 AM IST). For each student with ≥ 5 check-ins in last 14 days, analyzes trends. AI determines if alert needed, severity, and explanation. |

### FR-005
| Field | Value |
|---|---|
| **Title** | Alert Counselor/Admin |
| **Description** | Alert counselor/admin when student flagged (FCM + in-app). |
| **Priority** | High |
| **User Roles** | System → Counselor, School Admin |
| **Acceptance notes** | FCM + in-app notification to counselor and admin. Alert includes type, severity, AI analysis, and student info. |

### FR-006
| Field | Value |
|---|---|
| **Title** | Wellness Dashboard |
| **Description** | Wellness dashboard: aggregated class wellness, individual student timeline. |
| **Priority** | High |
| **User Roles** | Counselor, School Admin |
| **Acceptance notes** | Class-level: aggregated mood distribution, average survey scores, active alerts. Individual: timeline of check-ins, surveys, observations, alerts. |

### FR-007
| Field | Value |
|---|---|
| **Title** | Parent Wellness Summary |
| **Description** | Parent wellness summary: weekly non-clinical summary (opt-in). |
| **Priority** | Medium |
| **User Roles** | Parent |
| **Acceptance notes** | Opt-in only. Non-clinical language ("Aarav seems to be doing well" or "Aarav might benefit from more rest"). No raw data, no clinical terms, no alert details. |

### FR-008
| Field | Value |
|---|---|
| **Title** | Privacy Controls |
| **Description** | Privacy: wellness data visible only to student's teacher, counselor, admin — not parents by default. |
| **Priority** | Critical |
| **User Roles** | System |
| **Acceptance notes** | Role-based access control. Parents do NOT see raw wellness data by default. Parent summary is opt-in and non-clinical. All access logged in audit log. |

### FR-009
| Field | Value |
|---|---|
| **Title** | Intervention Tracking |
| **Description** | Intervention tracking: counselor logs actions taken for flagged students. |
| **Priority** | Medium |
| **User Roles** | Counselor, School Admin |
| **Acceptance notes** | Counselor acknowledges alert, logs intervention notes. Alert status: open → acknowledged → intervened → resolved. |

### Non-Functional Requirements

| ID | Requirement |
|---|---|
| NFR-1 | Mood check-in: < 2 seconds (quick tap) |
| NFR-2 | Wellness survey: < 30 seconds (5 questions) |
| NFR-3 | Early warning job: < 15 minutes for 1,000 students |
| NFR-4 | Wellness dashboard: < 3 seconds |
| NFR-5 | Student timeline: < 2 seconds |
| NFR-6 | Alert notification: < 10 seconds from alert creation |
| NFR-7 | AI analysis: < 10 seconds per student |

---

## 4. User Stories

### Student (via parent app or future student app)
- [ ] Do daily mood check-in (tap emoji)
- [ ] Complete weekly wellness survey
- [ ] Add optional note to mood check-in

### Teacher
- [ ] Log behavioral observation for a student
- [ ] Set observation severity (normal/concern/urgent)
- [ ] View class wellness overview

### Counselor
- [ ] View wellness alerts (open, acknowledged, intervened, resolved)
- [ ] Acknowledge an alert
- [ ] Log intervention notes for an alert
- [ ] Resolve an alert
- [ ] View wellness dashboard (class-level aggregated data)
- [ ] View individual student wellness timeline

### School Admin
- [ ] View school-wide wellness dashboard
- [ ] View wellness alerts
- [ ] Configure wellness settings (opt-in parent summary, survey frequency)

### Parent (opt-in)
- [ ] View child's weekly wellness summary (non-clinical)
- [ ] Opt in/out of wellness summary

### System
- [ ] Run daily AI early warning job
- [ ] Create alerts when concerning patterns detected
- [ ] Send FCM + in-app notifications to counselor/admin
- [ ] Generate weekly parent wellness summaries (opt-in)

---

## 5. Business Rules

### BR-001
**Rule:** One mood check-in per student per day.
**Enforcement:** `UNIQUE(student_id, checkin_date)` constraint on `wellness_checkins`. Prevents duplicate check-ins.

### BR-002
**Rule:** One wellness survey per student per survey date.
**Enforcement:** `UNIQUE(student_id, survey_date)` constraint on `wellness_surveys`. Prevents duplicate surveys.

### BR-003
**Rule:** Parents do not see raw wellness data by default.
**Enforcement:** Parent API returns only non-clinical summary (opt-in). No raw check-ins, surveys, observations, or alerts exposed to parents. Parent summary uses non-clinical language.

### BR-004
**Rule:** Early warning requires minimum data.
**Enforcement:** AI early warning job only analyzes students with ≥ 5 check-ins in last 14 days. Students with insufficient data are skipped.

### BR-005
**Rule:** Alert status transitions are controlled.
**Enforcement:** `open → acknowledged → intervened → resolved`. Only counselor/admin can change status. Acknowledged by tracked. Intervention notes required for `intervened` status.

### BR-006
**Rule:** Teacher observations are not editable.
**Enforcement:** Observations are point-in-time records. Once submitted, they cannot be edited or deleted. Ensures audit trail integrity.

### BR-007
**Rule:** Wellness data is school-scoped.
**Enforcement:** All wellness tables have `school_id`. Queries filtered by school. No cross-school wellness data access.

### BR-008
**Rule:** AI analysis is advisory, not diagnostic.
**Enforcement:** AI-generated alerts and explanations are advisory. Counselors make final determination. AI does not diagnose mental health conditions.

### BR-009
**Rule:** Parent wellness summary is non-clinical.
**Enforcement:** Summary uses phrases like "doing well," "might benefit from more rest," "seems engaged." No clinical terms (depression, anxiety, disorder). No alert details or severity levels.

### BR-010
**Rule:** All wellness data access is audited.
**Enforcement:** Every access to wellness data (view timeline, view dashboard, view alerts) logged in audit log with user ID, student ID, and timestamp.

---

## 6. Database Design

### 6.1 Entity Relationship Summary

Four new tables: `wellness_checkins` (daily mood), `wellness_surveys` (weekly survey), `teacher_observations` (behavioral notes), `wellness_alerts` (AI-generated alerts with intervention tracking).

### 6.2 New Tables

#### `wellness_checkins` table

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
```

#### `wellness_surveys` table

```sql
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
```

#### `teacher_observations` table

```sql
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
```

#### `wellness_alerts` table

```sql
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

### 6.3 Modified Tables

N/A — no existing tables modified.

### 6.4 Indexes

- `wellness_checkins(student_id, checkin_date DESC)` — timeline lookup
- `wellness_checkins(student_id, checkin_date)` — UNIQUE, one per day
- `wellness_surveys(student_id, survey_date)` — UNIQUE, one per date
- `teacher_observations(student_id, observation_date DESC)` — timeline lookup
- `wellness_alerts(school_id, status, created_at DESC)` — alert dashboard lookup

### 6.5 Constraints

- `wellness_checkins.mood` — NOT NULL, VARCHAR(16), one of: happy, okay, sad, anxious, angry
- `wellness_checkins(student_id, checkin_date)` — UNIQUE
- `wellness_surveys` — all 5 dimensions NOT NULL, INTEGER 1-5
- `wellness_surveys.overall_score` — NOT NULL, REAL (avg of 5 dimensions)
- `wellness_surveys(student_id, survey_date)` — UNIQUE
- `teacher_observations.behavior` — NOT NULL, VARCHAR(32)
- `teacher_observations.severity` — NOT NULL, VARCHAR(16), one of: normal, concern, urgent
- `wellness_alerts.alert_type` — NOT NULL, VARCHAR(32)
- `wellness_alerts.severity` — NOT NULL, VARCHAR(16), one of: low, medium, high
- `wellness_alerts.status` — NOT NULL, VARCHAR(16), one of: open, acknowledged, intervened, resolved

### 6.6 Foreign Keys

- `wellness_checkins.school_id` → `schools.id` (implicit)
- `wellness_checkins.student_id` → `students.id` (implicit)
- `wellness_surveys.school_id` → `schools.id` (implicit)
- `wellness_surveys.student_id` → `students.id` (implicit)
- `teacher_observations.school_id` → `schools.id` (implicit)
- `teacher_observations.student_id` → `students.id` (implicit)
- `teacher_observations.teacher_id` → `users.id` (implicit)
- `wellness_alerts.school_id` → `schools.id` (implicit)
- `wellness_alerts.student_id` → `students.id` (implicit)
- `wellness_alerts.acknowledged_by` → `users.id` (implicit)

### 6.7 Soft Delete Strategy

N/A — wellness records are permanent. No soft delete. Teacher observations are not editable (audit trail). Alerts transition through status (open → resolved) but are not deleted.

### 6.8 Audit Fields

- `wellness_checkins.created_at` — when check-in submitted
- `wellness_surveys.created_at` — when survey submitted
- `teacher_observations.created_at` — when observation logged
- `teacher_observations.teacher_id` — who logged (UUID)
- `wellness_alerts.created_at` — when alert created
- `wellness_alerts.acknowledged_by` — who acknowledged (UUID)
- `wellness_alerts.acknowledged_at` — when acknowledged
- `wellness_alerts.intervention_notes` — counselor's intervention notes

### 6.9 Migration Notes

Migration: `docs/db/migration_080_student_wellness.sql`
- CREATE 4 tables: `wellness_checkins`, `wellness_surveys`, `teacher_observations`, `wellness_alerts`
- No data migration (new feature)
- Indexes and UNIQUE constraints created in same migration

### 6.10 Exposed Mappings

```kotlin
object WellnessCheckinsTable : UUIDTable("wellness_checkins", "id") {
    val schoolId    = uuid("school_id")
    val studentId   = uuid("student_id")
    val mood        = varchar("mood", 16)
    val checkinDate = date("checkin_date")
    val notes       = text("notes").nullable()
    val createdAt   = timestamp("created_at")

    init {
        uniqueIndex("idx_wellness_checkins_unique", studentId, checkinDate)
    }
}

object WellnessSurveysTable : UUIDTable("wellness_surveys", "id") {
    val schoolId         = uuid("school_id")
    val studentId        = uuid("student_id")
    val surveyDate       = date("survey_date")
    val stressLevel      = integer("stress_level")
    val sleepQuality     = integer("sleep_quality")
    val appetite         = integer("appetite")
    val socialInteraction = integer("social_interaction")
    val energyLevel      = integer("energy_level")
    val overallScore     = float("overall_score")
    val createdAt        = timestamp("created_at")

    init {
        uniqueIndex("idx_wellness_surveys_unique", studentId, surveyDate)
    }
}

object TeacherObservationsTable : UUIDTable("teacher_observations", "id") {
    val schoolId        = uuid("school_id")
    val studentId       = uuid("student_id")
    val teacherId       = uuid("teacher_id")
    val observationDate = date("observation_date")
    val behavior        = varchar("behavior", 32)
    val notes           = text("notes").nullable()
    val severity        = varchar("severity", 16).default("normal")
    val createdAt       = timestamp("created_at")
}

object WellnessAlertsTable : UUIDTable("wellness_alerts", "id") {
    val schoolId          = uuid("school_id")
    val studentId         = uuid("student_id")
    val alertType         = varchar("alert_type", 32)
    val severity          = varchar("severity", 16)
    val triggerData       = text("trigger_data")  // JSON
    val aiAnalysis        = text("ai_analysis").nullable()
    val status            = varchar("status", 16).default("open")
    val acknowledgedBy    = uuid("acknowledged_by").nullable()
    val acknowledgedAt    = timestamp("acknowledged_at").nullable()
    val interventionNotes = text("intervention_notes").nullable()
    val createdAt         = timestamp("created_at")
}
```

Register all 4 in `DatabaseFactory.allTables`.

### 6.11 Seed Data

N/A — wellness data is user-generated. No seed data needed.

---

## 7. State Machines

### Wellness Alert State Machine

```
open ──counselor_acknowledges──> acknowledged ──intervention_logged──> intervened ──resolved──> resolved
  │                                │
  │                                │──more_info_needed──> acknowledged (stays)
  │
  └──auto_escalated──> open (severity increased)
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `open` | Counselor acknowledges | `acknowledged` | Counselor/admin role |
| `open` | Auto-escalation (new data) | `open` | Severity increased, new trigger data appended |
| `acknowledged` | Intervention logged | `intervened` | Intervention notes required |
| `acknowledged` | More info needed | `acknowledged` | Stays in acknowledged, notes updated |
| `intervened` | Issue resolved | `resolved` | Counselor/admin marks resolved |
| `intervened` | New concerning pattern | `intervened` | New alert created (separate alert) |
| `resolved` | New concerning pattern | `open` | New alert created (separate alert) |

### Mood Check-in State Machine

```
not_checked_in ──student_taps_emoji──> checked_in
  │
  └──already_checked_in──> checked_in (update not allowed, UNIQUE constraint)
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `not_checked_in` | Student taps emoji | `checked_in` | First check-in of day |
| `not_checked_in` | Already checked in today | `checked_in` | UNIQUE constraint prevents duplicate |
| `checked_in` | Next day | `not_checked_in` | New day, new check-in allowed |

### Early Warning Job State Machine

```
idle ──daily_trigger──> running ──all_students_processed──> completed
  │                        │
  │                        │──error──> error
  │                        │
  │                        └──partial_failure──> completed (with warnings)
  │
  └──completed──> idle (next day)
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `idle` | Daily trigger (6 AM IST) | `running` | Job starts |
| `running` | All students processed | `completed` | All students with ≥ 5 check-ins analyzed |
| `running` | Error for individual student | `running` | Log error, continue with next student |
| `running` | Fatal error | `error` | Job fails completely |
| `completed` | Next day's trigger | `running` | New job starts |
| `error` | Next day's trigger | `running` | Retry |

### Parent Summary State Machine

```
not_opted_in ──parent_opts_in──> opted_in ──weekly_job──> summary_generated ──parent_views──> viewed
  │                              │
  │                              │──parent_opts_out──> not_opted_in
  │
  └──opted_out──> not_opted_in
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `not_opted_in` | Parent opts in | `opted_in` | Parent consent recorded |
| `opted_in` | Weekly summary job | `summary_generated` | Job generates non-clinical summary |
| `opted_in` | Parent opts out | `not_opted_in` | Parent withdraws consent |
| `summary_generated` | Parent views | `viewed` | Parent opens summary |
| `viewed` | Next week's job | `summary_generated` | New weekly summary |

---

## 8. Backend Architecture

### 8.1 Component Overview

`WellnessService` handles check-ins, surveys, observations, timeline, dashboard, and alert management. `EarlyWarningJob` runs daily to analyze wellness patterns using AI. `AiService` (from `AI_INFRASTRUCTURE_SPEC.md`) provides LLM-based pattern analysis. `Notify.kt` dispatches alert notifications.

### 8.2 Design Principles

1. **Privacy-first** — sensitive data restricted to authorized staff, parents get non-clinical summary only
2. **AI-assisted, human-decided** — AI flags patterns, counselor makes final determination
3. **Proactive, not reactive** — early warning catches declining patterns before crisis
4. **Non-clinical for parents** — summary uses everyday language, no clinical terms
5. **Audit trail** — all wellness data access logged
6. **Observations are immutable** — teacher observations cannot be edited (point-in-time records)

### 8.3 Core Types

#### WellnessService

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
    suspend fun resolveAlert(alertId: UUID)
    suspend fun getParentSummary(childId: UUID): ParentWellnessSummaryDto
}
```

#### EarlyWarningJob

```kotlin
class EarlyWarningJob(private val wellnessService: WellnessService) {
    suspend fun execute(): Int {
        // 1. Get all students with ≥ 5 check-ins in last 14 days
        // 2. For each student, gather mood trend, survey scores, observations
        // 3. Call AI to analyze patterns
        // 4. If alert needed, create wellness_alert with severity + AI analysis
        // 5. Notify counselor/admin via FCM + in-app
        // 6. Return count of alerts created
    }
}
```

### 8.4 Repositories

- `WellnessCheckinRepository` — CRUD for `wellness_checkins`
- `WellnessSurveyRepository` — CRUD for `wellness_surveys`
- `TeacherObservationRepository` — CRUD for `teacher_observations`
- `WellnessAlertRepository` — CRUD for `wellness_alerts`

### 8.5 Mappers

- `WellnessCheckinMapper` — maps `wellness_checkins` rows to `CheckinDto`
- `WellnessSurveyMapper` — maps `wellness_surveys` rows to `SurveyDto`
- `ObservationMapper` — maps `teacher_observations` rows to `ObservationDto`
- `WellnessAlertMapper` — maps `wellness_alerts` rows to `AlertDto`

### 8.6 Permission Checks

- Student check-in/survey: parent role + `ChildAccessResolver` (parent submits on behalf of child)
- Teacher observation: teacher role + class assignment verification
- Counselor/Admin alert access: counselor or admin role
- Counselor/Admin dashboard: counselor or admin role
- Parent summary: parent role + `ChildAccessResolver` + opt-in verification
- All endpoints: JWT auth via `requireAuth()`

### 8.7 Background Jobs

- **Early Warning Job** — daily (6 AM IST)
  1. Get all students with ≥ 5 check-ins in last 14 days
  2. For each student:
     - Analyze mood trend (declining? consistently negative?)
     - Check survey scores (overall < 2.5? declining trend?)
     - Check teacher observations (concern/urgent in last 7 days?)
  3. If concerning pattern detected:
     - Call AI to analyze and generate explanation
     - Create `wellness_alert` with severity
     - Notify counselor/admin via FCM + in-app
  4. Return count of alerts created

- **Parent Summary Job** — weekly (Sunday 8 AM IST)
  1. Get all parents who opted in to wellness summary
  2. For each parent's child:
     - Analyze week's wellness data (mood, survey, observations)
     - Generate non-clinical summary
     - Send via FCM + in-app
  3. Return count of summaries sent

### 8.8 Domain Events

- `MoodCheckinSubmitted` — emitted when student submits mood check-in
- `WellnessSurveySubmitted` — emitted when student submits wellness survey
- `TeacherObservationLogged` — emitted when teacher logs observation
- `WellnessAlertCreated` — emitted when AI early warning creates alert
- `AlertAcknowledged` — emitted when counselor acknowledges alert
- `InterventionLogged` — emitted when counselor logs intervention
- `AlertResolved` — emitted when alert marked resolved
- `ParentSummaryGenerated` — emitted when weekly parent summary generated

### 8.9 Caching

- Class wellness dashboard: cached per class, 5-minute TTL
- Student timeline: cached per student, 5-minute TTL
- Alert list: cached per school, 1-minute TTL (frequently updated)

### 8.10 Transactions

- Check-in: single transaction (insert check-in)
- Survey: single transaction (insert survey)
- Observation: single transaction (insert observation)
- Alert creation: single transaction (insert alert)
- Alert status change: single transaction (update status, acknowledged_by, acknowledged_at)
- Intervention logging: single transaction (update status, intervention_notes)

### 8.11 Rate Limiting

N/A — wellness features are not high-frequency. Check-in is once per day, survey once per week.

### 8.12 Configuration

- `WELLNESS_ENABLED` — default `true`; enable/disable feature
- `WELLNESS_EARLY_WARNING_ENABLED` — default `true`; enable/disable AI early warning
- `WELLNESS_PARENT_SUMMARY_ENABLED` — default `true`; enable/disable parent summary
- `WELLNESS_MIN_CHECKINS_FOR_WARNING` — default `5`; minimum check-ins for AI analysis
- `WELLNESS_ANALYSIS_WINDOW_DAYS` — default `14`; analysis window
- `WELLNESS_AI_MODEL` — default `wellness_v1`; AI model identifier

### 8.13 AI Early Warning Job

Daily 6 AM IST:
1. For each student with ≥ 5 check-ins in last 14 days:
   - Analyze mood trend (declining? consistently negative?)
   - Check survey scores (overall < 2.5? declining trend?)
   - Check teacher observations (concern/urgent in last 7 days?)
2. If concerning pattern detected:
   - Call AI to analyze and generate explanation
   - Create `wellness_alert` with severity
   - Notify counselor/admin via FCM

### 8.14 AI Prompt

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

## 9. API Contracts

### 9.1 Student Endpoints (via parent app or future student app)

```
POST /api/v1/parent/wellness/checkin
  Body: { student_id: "uuid", mood: "happy", notes: "optional text" }
  → 201: { success: true }

POST /api/v1/parent/wellness/survey
  Body: { student_id: "uuid", stress_level: 3, sleep_quality: 4, appetite: 3, social_interaction: 4, energy_level: 3 }
  → 201: { success: true, overall_score: 3.4 }
```

### 9.2 Teacher Endpoints

```
POST /api/v1/teacher/wellness/observation
  Body: { student_id: "uuid", behavior: "withdrawn", notes: "not participating", severity: "concern" }
  → 201: { success: true }

GET /api/v1/teacher/wellness/class/{classId}
  → 200: ClassWellnessDto
```

### 9.3 Counselor/Admin Endpoints

```
GET /api/v1/school/wellness/alerts?status=open
  → 200: { alerts: [AlertDto] }

POST /api/v1/school/wellness/alerts/{id}/acknowledge
  → 200: { success: true }

POST /api/v1/school/wellness/alerts/{id}/intervene
  Body: { notes: "Spoke with student, referred to counselor" }
  → 200: { success: true }

POST /api/v1/school/wellness/alerts/{id}/resolve
  → 200: { success: true }

GET /api/v1/school/wellness/dashboard
  → 200: SchoolWellnessDashboardDto

GET /api/v1/school/wellness/student/{studentId}/timeline
  → 200: WellnessTimelineDto
```

### 9.4 Parent Endpoints (opt-in)

```
GET /api/v1/parent/wellness/summary/{childId}
  → 200: ParentWellnessSummaryDto
  → 403: Not opted in

POST /api/v1/parent/wellness/summary/opt-in
  Body: { child_id: "uuid" }
  → 200: { success: true }

POST /api/v1/parent/wellness/summary/opt-out
  Body: { child_id: "uuid" }
  → 200: { success: true }
```

### 9.5 DTO Models

All `@Serializable`, wrapped in `ApiResponse<T>` pattern.

```kotlin
@Serializable data class CheckinDto(
    val id: String,
    val mood: String,        // happy | okay | sad | anxious | angry
    val checkinDate: String,
    val notes: String?,
)

@Serializable data class WellnessSurveyDto(
    val id: String,
    val surveyDate: String,
    val stressLevel: Int,
    val sleepQuality: Int,
    val appetite: Int,
    val socialInteraction: Int,
    val energyLevel: Int,
    val overallScore: Float,
)

@Serializable data class ObservationDto(
    val id: String,
    val studentId: String,
    val teacherId: String,
    val observationDate: String,
    val behavior: String,    // engaged | withdrawn | disruptive | tired | anxious | happy | focused
    val notes: String?,
    val severity: String,    // normal | concern | urgent
)

@Serializable data class AlertDto(
    val id: String,
    val studentId: String,
    val alertType: String,   // declining_mood | low_survey_score | teacher_concern | pattern_change
    val severity: String,    // low | medium | high
    val aiAnalysis: String?,
    val status: String,      // open | acknowledged | intervened | resolved
    val acknowledgedByName: String?,
    val interventionNotes: String?,
    val createdAt: String,
)

@Serializable data class WellnessTimelineDto(
    val checkins: List<CheckinDto>,
    val surveys: List<WellnessSurveyDto>,
    val observations: List<ObservationDto>,
    val alerts: List<AlertDto>,
)

@Serializable data class ClassWellnessDto(
    val classId: String,
    val className: String,
    val moodDistribution: Map<String, Int>,  // mood → count
    val avgSurveyScore: Float,
    val activeAlerts: Int,
    val studentCount: Int,
)

@Serializable data class ParentWellnessSummaryDto(
    val childId: String,
    val weekOf: String,
    val summary: String,     // non-clinical text
    val moodTrend: String,   // "stable" | "improving" | "slightly_down" — non-clinical
)

enum class Mood { HAPPY, OKAY, SAD, ANXIOUS, ANGRY }
enum class Behavior { ENGAGED, WITHDRAWN, DISRUPTIVE, TIRED, ANXIOUS, HAPPY, FOCUSED }
enum class ObservationSeverity { NORMAL, CONCERN, URGENT }
enum class AlertType { DECLINING_MOOD, LOW_SURVEY_SCORE, TEACHER_CONCERN, PATTERN_CHANGE }
enum class AlertSeverity { LOW, MEDIUM, HIGH }
enum class AlertStatus { OPEN, ACKNOWLEDGED, INTERVENED, RESOLVED }
```

---

## 10. Frontend Architecture

### 10.1 Screens

| Screen | Platform | Role | Description |
|---|---|---|---|
| `MoodCheckInScreen` | Compose | Parent (on behalf of student) | Emoji-based mood check-in |
| `WellnessSurveyScreen` | Compose | Parent (on behalf of student) | Weekly 5-question survey |
| `WellnessObservationScreen` | Compose | Teacher | Log behavioral observation |
| `WellnessDashboardScreen` | Compose | Counselor, Admin | School/class wellness dashboard |
| `WellnessAlertsScreen` | Compose | Counselor, Admin | Alert list with status management |
| `WellnessTimelineScreen` | Compose | Counselor, Admin | Individual student wellness timeline |
| `ParentWellnessSummaryScreen` | Compose | Parent | Weekly non-clinical wellness summary |

### 10.2 Navigation

- Parent: Home → Wellness → Mood Check-in / Survey / Summary
- Teacher: Student Profile → Log Observation / Class Wellness
- Counselor: Dashboard → Alerts / Dashboard / Student Timeline
- Admin: Dashboard → Wellness Dashboard / Alerts

### 10.3 UX Flows

#### Student: Mood Check-in (via parent app)
1. Parent opens Wellness → Mood Check-in
2. 5 emoji buttons: 😊 🙂 😐 😟 😠
3. Student taps emoji
4. Optional: add text note
5. Submit → confirmation

#### Teacher: Log Observation
1. Teacher opens student profile → Log Observation
2. Select behavior: engaged, withdrawn, disruptive, tired, anxious, happy, focused
3. Set severity: normal, concern, urgent
4. Optional: add notes
5. Submit → confirmation

#### Counselor: View Alerts
1. Counselor opens Wellness → Alerts
2. Sees list of open alerts (sorted by severity, date)
3. Tap alert → see AI analysis, trigger data, student timeline
4. Acknowledge → status changes to "acknowledged"
5. Log intervention → enter notes → status changes to "intervened"
6. Resolve → status changes to "resolved"

### 10.4 State Management

```kotlin
data class MoodCheckInState(
    val selectedMood: Mood?,
    val notes: String,
    val isSubmitting: Boolean,
    val error: String?,
)

data class WellnessSurveyState(
    val stressLevel: Int,
    val sleepQuality: Int,
    val appetite: Int,
    val socialInteraction: Int,
    val energyLevel: Int,
    val isSubmitting: Boolean,
    val error: String?,
)

data class WellnessAlertsState(
    val alerts: List<AlertDto>,
    val filterStatus: AlertStatus,
    val isLoading: Boolean,
    val error: String?,
)

data class WellnessDashboardState(
    val classWellness: List<ClassWellnessDto>,
    val activeAlerts: Int,
    val totalStudents: Int,
    val isLoading: Boolean,
)

data class WellnessTimelineState(
    val checkins: List<CheckinDto>,
    val surveys: List<WellnessSurveyDto>,
    val observations: List<ObservationDto>,
    val alerts: List<AlertDto>,
    val isLoading: Boolean,
)
```

### 10.5 Offline Support

- Mood check-in: available offline (queued, synced when online)
- Wellness survey: available offline (queued, synced when online)
- Dashboard: cached locally (last fetched)
- Alerts: cached locally (last fetched)
- Timeline: cached locally (last fetched)

### 10.6 Loading States

- Check-in: "Submitting..."
- Survey: "Submitting..."
- Dashboard: "Loading wellness data..."
- Alerts: "Loading alerts..."
- Timeline: "Loading timeline..."

### 10.7 Error Handling (UI)

- Already checked in today: "You've already checked in today."
- Not opted in (parent summary): "Opt in to see your child's wellness summary."
- No data: "No wellness data available yet."
- Network error: "Connection issue. Please check your internet."

### 10.8 Component Integration Guidelines

| Rule | Description |
|---|---|
| **R1** | Mood check-in: 5 large emoji buttons, single tap to select, submit button |
| **R2** | Survey: 5 questions with 1-5 slider/radio, progress indicator |
| **R3** | Observation: dropdown for behavior, dropdown for severity, text field for notes |
| **R4** | Dashboard: mood distribution chart, avg survey score, active alerts count |
| **R5** | Alerts: list sorted by severity (high → medium → low), color-coded severity badges |
| **R6** | Timeline: chronological list of check-ins, surveys, observations, alerts |
| **R7** | Parent summary: non-clinical text, no raw data, no clinical terms |
| **R8** | Alert detail: AI analysis, trigger data, student timeline link, action buttons |
| **R9** | Intervention form: text area for notes, submit button |
| **R10** | Privacy notice: "Wellness data is confidential and visible only to authorized staff." |

---

## 11. Shared Module Changes (KMP)

### 11.1 DTOs

All DTOs defined in section 9.5, placed in `shared/.../wellness/domain/model/WellnessModels.kt`.

### 11.2 Domain Models

```kotlin
data class WellnessTimeline(
    val checkins: List<Checkin>,
    val surveys: List<WellnessSurvey>,
    val observations: List<Observation>,
    val alerts: List<WellnessAlert>,
)

data class Checkin(
    val id: String,
    val mood: Mood,
    val checkinDate: LocalDate,
    val notes: String?,
)

data class WellnessSurvey(
    val id: String,
    val surveyDate: LocalDate,
    val stressLevel: Int,
    val sleepQuality: Int,
    val appetite: Int,
    val socialInteraction: Int,
    val energyLevel: Int,
    val overallScore: Float,
)

data class Observation(
    val id: String,
    val studentId: String,
    val teacherId: String,
    val observationDate: LocalDate,
    val behavior: Behavior,
    val notes: String?,
    val severity: ObservationSeverity,
)

data class WellnessAlert(
    val id: String,
    val studentId: String,
    val alertType: AlertType,
    val severity: AlertSeverity,
    val aiAnalysis: String?,
    val status: AlertStatus,
    val acknowledgedByName: String?,
    val interventionNotes: String?,
    val createdAt: Instant,
)
```

### 11.3 Repository Interfaces

```kotlin
interface WellnessRepository {
    suspend fun checkin(token: String, studentId: String, mood: String, notes: String?): NetworkResult<Unit>
    suspend fun submitSurvey(token: String, request: SurveyRequest): NetworkResult<Float>
    suspend fun getParentSummary(token: String, childId: String): NetworkResult<ParentWellnessSummaryDto>
    suspend fun optInSummary(token: String, childId: String): NetworkResult<Unit>
    suspend fun optOutSummary(token: String, childId: String): NetworkResult<Unit>
}

interface WellnessManagementRepository {
    suspend fun logObservation(token: String, request: ObservationRequest): NetworkResult<Unit>
    suspend fun getClassWellness(token: String, classId: String): NetworkResult<ClassWellnessDto>
    suspend fun getAlerts(token: String, status: String): NetworkResult<List<AlertDto>>
    suspend fun acknowledgeAlert(token: String, alertId: String): NetworkResult<Unit>
    suspend fun logIntervention(token: String, alertId: String, notes: String): NetworkResult<Unit>
    suspend fun resolveAlert(token: String, alertId: String): NetworkResult<Unit>
    suspend fun getDashboard(token: String): NetworkResult<SchoolWellnessDashboardDto>
    suspend fun getTimeline(token: String, studentId: String): NetworkResult<WellnessTimelineDto>
}
```

### 11.4 UseCases

- `CheckinUseCase`
- `SubmitSurveyUseCase`
- `LogObservationUseCase`
- `GetAlertsUseCase`
- `AcknowledgeAlertUseCase`
- `LogInterventionUseCase`
- `ResolveAlertUseCase`
- `GetDashboardUseCase`
- `GetTimelineUseCase`
- `GetParentSummaryUseCase`
- `OptInSummaryUseCase`
- `OptOutSummaryUseCase`

### 11.5 Validation

- `mood`: one of happy, okay, sad, anxious, angry
- `stress_level`, `sleep_quality`, `appetite`, `social_interaction`, `energy_level`: integer 1-5
- `behavior`: one of engaged, withdrawn, disruptive, tired, anxious, happy, focused
- `severity`: one of normal, concern, urgent
- `student_id`: valid UUID
- `notes`: max 500 characters (optional)

### 11.6 Serialization

Standard Kotlinx serialization. Enums serialized as lowercase strings.

### 11.7 Network APIs

Ktor `@Resource` route definitions in `WellnessApi.kt`:
- POST `/api/v1/parent/wellness/checkin`
- POST `/api/v1/parent/wellness/survey`
- GET `/api/v1/parent/wellness/summary/{childId}`
- POST `/api/v1/parent/wellness/summary/opt-in`
- POST `/api/v1/parent/wellness/summary/opt-out`
- POST `/api/v1/teacher/wellness/observation`
- GET `/api/v1/teacher/wellness/class/{classId}`
- GET `/api/v1/school/wellness/alerts?status={status}`
- POST `/api/v1/school/wellness/alerts/{id}/acknowledge`
- POST `/api/v1/school/wellness/alerts/{id}/intervene`
- POST `/api/v1/school/wellness/alerts/{id}/resolve`
- GET `/api/v1/school/wellness/dashboard`
- GET `/api/v1/school/wellness/student/{studentId}/timeline`

### 11.8 Database Models (Local Cache)

- Dashboard data cached in local DB (last fetched, 5-minute TTL)
- Alert list cached in local DB (last fetched, 1-minute TTL)
- Timeline cached in local DB (last fetched, 5-minute TTL)
- Offline check-ins queued in local DB (synced when online)
- Offline surveys queued in local DB (synced when online)

---

## 12. Permissions Matrix

| Action | Super Admin | School Admin | Counselor | Teacher | Parent |
|---|---|---|---|---|---|
| Submit mood check-in | N/A | N/A | N/A | N/A | ✅ (on behalf of child) |
| Submit wellness survey | N/A | N/A | N/A | N/A | ✅ (on behalf of child) |
| Log teacher observation | ✅ | ✅ | N/A | ✅ | ❌ |
| View class wellness | ✅ | ✅ | ✅ | ✅ (own class) | ❌ |
| View wellness alerts | ✅ | ✅ | ✅ | ❌ | ❌ |
| Acknowledge alert | ✅ | ✅ | ✅ | ❌ | ❌ |
| Log intervention | ✅ | ✅ | ✅ | ❌ | ❌ |
| Resolve alert | ✅ | ✅ | ✅ | ❌ | ❌ |
| View wellness dashboard | ✅ | ✅ | ✅ | ❌ | ❌ |
| View student timeline | ✅ | ✅ | ✅ | ✅ (own students) | ❌ |
| View parent summary | N/A | N/A | N/A | N/A | ✅ (opt-in, own child) |
| Opt in/out summary | N/A | N/A | N/A | N/A | ✅ |

---

## 13. Notifications

### Wellness Notifications

| Trigger | Recipient | Channel | Template |
|---|---|---|---|
| Wellness alert created (high severity) | Counselor + Admin | FCM + in-app + WhatsApp | "URGENT: Wellness alert for {student_name}. Severity: High. Please review immediately." |
| Wellness alert created (medium severity) | Counselor + Admin | FCM + in-app | "Wellness alert for {student_name}. Severity: Medium. Please review when available." |
| Wellness alert created (low severity) | Counselor | in-app | "Wellness alert for {student_name}. Severity: Low." |
| Parent weekly summary (opt-in) | Parent | FCM + in-app | "Weekly wellness summary for {child_name} is available." |
| Alert acknowledged | Admin | in-app | "Counselor {name} acknowledged alert for {student_name}." |
| Alert resolved | Admin | in-app | "Alert for {student_name} resolved by {name}." |

### Notification Integration

Uses existing `Notify.kt` dispatch. Notifications created with category "wellness" and sent via appropriate channels based on severity.

---

## 14. Background Jobs

### Early Warning Job

| Property | Value |
|---|---|
| **Name** | `EarlyWarningJob` |
| **Schedule** | Daily (6 AM IST) |
| **Duration** | < 15 minutes for 1,000 students |
| **Retry** | None (next day) |

#### Job Flow

1. Get all students with ≥ 5 check-ins in last 14 days
2. For each student:
   - Gather mood trend (last 14 days)
   - Gather survey scores (last 3 surveys)
   - Gather teacher observations (last 7 days)
   - Gather previous alerts
3. Call AI to analyze patterns
4. If alert needed:
   - Create `wellness_alert` with type, severity, AI analysis
   - Send notification to counselor/admin
5. Return count of alerts created

### Parent Summary Job

| Property | Value |
|---|---|
| **Name** | `ParentSummaryJob` |
| **Schedule** | Weekly (Sunday 8 AM IST) |
| **Duration** | < 10 minutes for 1,000 students |
| **Retry** | None (next week) |

#### Job Flow

1. Get all parents who opted in to wellness summary
2. For each parent's child:
   - Analyze week's wellness data (mood, survey, observations)
   - Generate non-clinical summary using AI
   - Send via FCM + in-app
3. Return count of summaries sent

---

## 15. Integrations

### Internal Integrations

| System | Integration Point | Direction | Protocol | Error Handling |
|---|---|---|---|---|
| `AiService` (`AI_INFRASTRUCTURE_SPEC.md`) | AI early warning analysis | Call | Direct call | Log error, skip student |
| `AiService` (`AI_INFRASTRUCTURE_SPEC.md`) | Parent summary generation | Call | Direct call | Log error, skip parent |
| `Notify.kt` | Alert notifications | Call | Direct call | Log on failure |
| `StudentsTable` | Student info | Read | Direct DB | Required |
| `UsersTable` | Teacher/counselor/admin info | Read | Direct DB | Required |
| `ClassesTable` | Class info | Read | Direct DB | Required |
| `HolisticAssessmentsTable` | Holistic assessment data | Read | Direct DB | Optional (enriches analysis) |
| `HealthRecordsTable` | Physical health data | Read | Direct DB | Optional (enriches analysis) |

### External Integrations

N/A — no external integrations. All data is internal. AI uses existing `AiService` (internal LLM).

### Integration Patterns

- **AI early warning:** `aiService.complete(null, null, "wellness", "wellness_v1", context)` — uses existing AI infrastructure
- **Parent summary:** `aiService.complete(null, null, "wellness_summary", "wellness_summary_v1", context)` — non-clinical summary
- **Notifications:** `Notify.kt` called with category "wellness"
- **Holistic assessment:** Read from `HolisticAssessmentsTable` to enrich AI analysis context
- **Health records:** Read from `HealthRecordsTable` to check for physical health factors

---

## 16. Security

### Authentication

- All wellness APIs: JWT auth via `requireAuth()`
- Student endpoints: parent role + `ChildAccessResolver`
- Teacher endpoints: teacher role + class assignment
- Counselor/Admin endpoints: counselor or admin role
- Parent summary: parent role + `ChildAccessResolver` + opt-in verification

### Authorization

- Wellness data visible only to: student's teacher, counselor, school admin
- Parents do NOT see raw wellness data by default
- Parent summary is opt-in and non-clinical
- No cross-school wellness data access
- Teachers can only observe students in their assigned classes

### Data Protection

- **Wellness data is highly sensitive** — mental health indicators
- All wellness data encrypted at rest (database-level encryption)
- All wellness data access logged in audit log
- AI analysis does not send student names to LLM (uses anonymized IDs)
- Parent summary uses non-clinical language only
- Wellness alerts are confidential — not shared with parents or students

### Input Validation

- `mood`: one of happy, okay, sad, anxious, angry
- Survey dimensions: integer 1-5
- `behavior`: one of engaged, withdrawn, disruptive, tired, anxious, happy, focused
- `severity`: one of normal, concern, urgent
- `notes`: max 500 characters (optional)
- `student_id`: valid UUID

### Rate Limiting

N/A — wellness features are low-frequency (daily check-in, weekly survey).

### Audit Logging

- Mood check-in submitted: student ID, mood, date
- Survey submitted: student ID, overall score, date
- Observation logged: teacher ID, student ID, behavior, severity
- Alert created: student ID, alert type, severity, AI analysis
- Alert viewed: user ID, student ID, alert ID
- Alert acknowledged: user ID, alert ID, timestamp
- Intervention logged: user ID, alert ID, notes
- Alert resolved: user ID, alert ID, timestamp
- Timeline viewed: user ID, student ID
- Dashboard viewed: user ID, school ID
- Parent summary viewed: parent ID, child ID
- Parent opt-in/opt-out: parent ID, child ID, action

### PII Handling

- Student name used in alert notifications (necessary for counselor action)
- Student name NOT sent to LLM (uses anonymized context)
- Parent summary uses child's first name only (non-clinical)
- No PII shared with external services
- All wellness data encrypted at rest

### Multi-tenant Isolation

- All wellness tables have `school_id` — school-scoped
- All queries filtered by `school_id`
- No cross-school wellness data access
- Counselor/admin can only view their school's wellness data

---

## 17. Performance & Scalability

### Expected Scale

- 1,000 students per school
- 1 check-in per student per day = 1,000 check-ins/day
- 1 survey per student per week = ~143 surveys/day
- 5-10 observations per teacher per day
- Early warning job: 1,000 students × AI analysis = 1,000 AI calls per day (only for students with ≥ 5 check-ins)

### Query Optimization

- Check-in timeline: `idx_wellness_checkins_student(student_id, checkin_date DESC)` — paginated
- Survey lookup: `idx_wellness_surveys_unique(student_id, survey_date)` — UNIQUE
- Observation timeline: `idx_teacher_obs_student(student_id, observation_date DESC)` — paginated
- Alert dashboard: `idx_wellness_alerts_school(school_id, status, created_at DESC)` — filtered

### Indexing Strategy

- `wellness_checkins(student_id, checkin_date DESC)` — timeline lookup
- `wellness_checkins(student_id, checkin_date)` — UNIQUE, one per day
- `wellness_surveys(student_id, survey_date)` — UNIQUE, one per date
- `teacher_observations(student_id, observation_date DESC)` — timeline lookup
- `wellness_alerts(school_id, status, created_at DESC)` — alert dashboard

### Caching Strategy

- Class wellness dashboard: cached per class, 5-minute TTL
- Student timeline: cached per student, 5-minute TTL
- Alert list: cached per school, 1-minute TTL

### Pagination

- Timeline: 50 items per page (check-ins, surveys, observations, alerts)
- Alerts: 20 per page

### Connection Pooling

Uses existing HikariCP connection pool. No additional pooling needed.

### Async Processing

- Early warning job: background job (async)
- Parent summary job: background job (async)
- AI analysis: synchronous within job (with timeout)
- Notifications: async (fire-and-forget via `Notify.kt`)

### Scalability Concerns

- Early warning job: 1,000 AI calls per day. Each call < 10 seconds. Total: ~2.8 hours sequential. Can be parallelized to < 15 minutes.
- DB storage: 1,000 check-ins/day × 365 days = 365K records/year × ~0.5KB = ~180MB/year. Manageable.
- AI cost: 1,000 calls/day × ~500 tokens = 500K tokens/day. Manageable with existing AI infrastructure.

---

## 18. Edge Cases

| # | Scenario | Expected Behavior |
|---|---|---|
| EC-1 | Student checks in twice same day | UNIQUE constraint prevents duplicate. Return 409 "Already checked in today." |
| EC-2 | Student submits survey twice same date | UNIQUE constraint prevents duplicate. Return 409. |
| EC-3 | Student has < 5 check-ins in 14 days | Skip in early warning job. Not enough data for analysis. |
| EC-4 | AI analysis fails for student | Log error. Skip student. Continue with next. No alert created. |
| EC-5 | No counselor assigned to school | Alert sent to admin only. Log warning. |
| EC-6 | Parent not opted in to summary | Return 403 "Not opted in." |
| EC-7 | Teacher observes student not in their class | Return 403 "Not your student." |
| EC-8 | Alert already exists for same student same day | Create new alert (different trigger data). Multiple alerts per student per day allowed. |
| EC-9 | Counselor acknowledges already-acknowledged alert | Return 409 "Already acknowledged." |
| EC-10 | Student has no survey data | AI analysis uses mood + observations only. Survey section empty in prompt. |
| EC-11 | Student has no observations | AI analysis uses mood + surveys only. Observation section empty in prompt. |
| EC-12 | All check-ins are "happy" for 14 days | AI likely returns alert_needed: false. No alert created. |
| EC-13 | All check-ins are "sad" for 14 days | AI likely returns alert_needed: true, severity: high. Alert created. |
| EC-14 | Parent opts out after summary generated | Existing summary remains accessible. No new summaries generated. |
| EC-15 | School has no wellness data at all | Dashboard shows "No wellness data available." Empty state. |

---

## 19. Error Handling

### Error Response Format

Standard `ApiResponse` error format.

### Error Codes

| Code | HTTP Status | Description | User Message |
|---|---|---|---|
| `ALREADY_CHECKED_IN` | 409 | Already checked in today | "You've already checked in today." |
| `ALREADY_SURVEYED` | 409 | Already submitted survey for this date | "You've already submitted a survey for this date." |
| `NOT_OPTED_IN` | 403 | Parent has not opted in to wellness summary | "Opt in to see your child's wellness summary." |
| `NOT_YOUR_STUDENT` | 403 | Teacher not assigned to student's class | "You do not have access to this student." |
| `ALERT_NOT_FOUND` | 404 | Alert ID not found | "Alert not found." |
| `ALREADY_ACKNOWLEDGED` | 409 | Alert already acknowledged | "This alert has already been acknowledged." |
| `ALREADY_RESOLVED` | 409 | Alert already resolved | "This alert has already been resolved." |
| `INVALID_MOOD` | 400 | Mood value not valid | "Invalid mood. Use: happy, okay, sad, anxious, angry." |
| `INVALID_BEHAVIOR` | 400 | Behavior value not valid | "Invalid behavior." |
| `INVALID_SEVERITY` | 400 | Severity value not valid | "Invalid severity. Use: normal, concern, urgent." |
| `SURVEY_VALUE_OUT_OF_RANGE` | 400 | Survey value not 1-5 | "Survey values must be between 1 and 5." |
| `AI_ANALYSIS_FAILED` | 500 | AI analysis failed | (Internal — logged, student skipped in job) |

### Error Handling Strategy

- **Check-in/survey:** Return 409 on duplicate. Return 400 on invalid input.
- **Observation:** Return 403 if not teacher's student. Return 400 on invalid input.
- **Early warning job:** Log error per student. Continue with next. No 500 — it's a batch job.
- **Alert management:** Return 404 if not found. Return 409 on invalid state transition.
- **Parent summary:** Return 403 if not opted in.

### Retry Strategy

- Check-in/survey: client retries on network error (3 attempts)
- Early warning job: no retry (next day's job handles it)
- Parent summary job: no retry (next week's job handles it)
- AI analysis: no retry within job (skip student on failure)

### Fallback Behavior

- No wellness data: empty timeline/dashboard
- AI analysis fails: no alert created for that student
- No counselor: alert sent to admin only
- Parent not opted in: 403 with opt-in prompt

---

## 20. Analytics & Reporting

### Analytics Dashboard Data

| Metric | Source | Derivation |
|---|---|---|
| Check-in participation rate | `wellness_checkins` | Students with ≥ 1 check-in this week / total students |
| Survey participation rate | `wellness_surveys` | Students with survey this week / total students |
| Average mood distribution | `wellness_checkins` | Count by mood for current week |
| Average survey score | `wellness_surveys` | Avg overall_score for current week |
| Active alerts | `wellness_alerts` | Count where status = open |
| Alerts by severity | `wellness_alerts` | Count by severity (low/medium/high) |
| Alert resolution time | `wellness_alerts` | Avg time from created_at to resolved |
| Intervention rate | `wellness_alerts` | Count intervened / count total |
| Parent opt-in rate | Audit logs | Count opted in / count total parents |

### Export Capabilities

- Wellness summary export (CSV) — student, mood distribution, avg score, alert count
- Alert log export (CSV) — student, type, severity, status, dates, intervention notes

### Report Types

| Report | Format | Frequency | Recipient |
|---|---|---|---|
| Wellness overview | JSON (API) | On-demand | Counselor, Admin |
| Alert log | JSON (API) | On-demand | Counselor, Admin |
| Participation rates | JSON (API) | Weekly | Product Team |
| AI accuracy | JSON (API) | Monthly | Dev Team |

---

## 21. Testing Strategy

### Unit Tests

- `WellnessService.checkin()` — mood validation, UNIQUE constraint
- `WellnessService.submitSurvey()` — survey validation, overall score calculation
- `WellnessService.logObservation()` — behavior/severity validation, teacher authorization
- `WellnessService.acknowledgeAlert()` — status transition, authorization
- `WellnessService.logIntervention()` — notes required, status transition
- `WellnessService.resolveAlert()` — status transition, authorization
- `WellnessService.getParentSummary()` — opt-in verification, non-clinical language
- AI prompt building — context assembly, anonymization
- AI response parsing — alert_needed, severity, explanation parsing

### Integration Tests

- Full check-in flow: submit check-in → verify in DB → verify in timeline
- Full survey flow: submit survey → verify score calculation → verify in timeline
- Full observation flow: teacher logs → verify in DB → verify in timeline
- Full alert flow: set up declining data → run job → verify alert created → verify notification
- Alert lifecycle: create → acknowledge → intervene → resolve → verify status transitions
- Parent summary: opt in → run job → verify summary generated → verify non-clinical language
- Privacy: parent tries to access raw data → verify 403
- Privacy: teacher tries to access alerts → verify 403

### E2E Tests

- Parent submits mood check-in → counselor sees in timeline
- Teacher logs observation → counselor sees in timeline
- AI detects declining pattern → counselor receives alert → acknowledges → logs intervention → resolves
- Parent opts in → receives weekly summary

### Performance Tests

- Early warning job: < 15 minutes for 1,000 students
- Parent summary job: < 10 minutes for 1,000 students
- Dashboard: < 3 seconds
- Timeline: < 2 seconds
- AI analysis: < 10 seconds per student

### Test Data

- 20 students with varying mood patterns (happy, declining, stable)
- 3 weeks of check-in data
- 2 weeks of survey data
- Teacher observations (normal, concern, urgent)
- Mock LLM (returns controlled alert_needed, severity, explanation)
- Students with < 5 check-ins (edge case)
- Parents opted in and opted out

### Test Environment

- Test database with wellness tables
- Mock LLM (returns controlled responses)
- Test JWT tokens for parent, teacher, counselor, admin roles
- Pre-seeded student and class data

---

## 22. Acceptance Criteria

- [ ] Student can do daily mood check-in
- [ ] Weekly wellness survey submitted
- [ ] Teacher can log behavioral observations
- [ ] AI early warning detects declining patterns
- [ ] Alerts sent to counselor/admin
- [ ] Wellness dashboard shows class and individual data
- [ ] Intervention tracking works (acknowledge → intervene → resolve)
- [ ] Parent summary (opt-in, non-clinical)
- [ ] Privacy: data restricted to authorized staff
- [ ] Parents cannot see raw wellness data by default
- [ ] All wellness data access audited
- [ ] Teacher observations are immutable (cannot edit/delete)
- [ ] One check-in per student per day enforced
- [ ] One survey per student per survey date enforced

---

## 23. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 2 days | DB migration `migration_080_student_wellness.sql`, Exposed tables, register in `DatabaseFactory` |
| 2 | 2 days | `WellnessService` (checkin, survey, observation, timeline, dashboard) |
| 3 | 3 days | AI early warning engine + daily job (`EarlyWarningJob`) |
| 4 | 1 day | Alert management + intervention tracking |
| 5 | 1 day | API endpoints |
| 6 | 4 days | Client UI: `MoodCheckInScreen`, `WellnessSurveyScreen`, `WellnessObservationScreen`, `WellnessDashboardScreen`, `WellnessAlertsScreen`, `WellnessTimelineScreen`, `ParentWellnessSummaryScreen` |
| 7 | 2 days | Tests: unit, integration, E2E |

### Pre-Implementation Checklist

- [ ] Verify `AI_INFRASTRUCTURE_SPEC.md` is implemented and `AiService` available
- [ ] Verify `HEALTH_RECORDS_SPEC.md` is implemented
- [ ] Verify `HolisticAssessmentsTable` exists (from `NEP_COMPLIANCE_SPEC.md`)
- [ ] Verify `Notify.kt` supports "wellness" category
- [ ] Verify counselor role exists in user system
- [ ] Verify parent-child link (`ChildAccessResolver`) works
- [ ] Verify FCM notifications work for counselor/admin
- [ ] Verify AI prompt template for wellness analysis

---

## 24. File-Level Impact Analysis

### Server

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | `WellnessCheckinsTable`, `WellnessSurveysTable`, `TeacherObservationsTable`, `WellnessAlertsTable` |
| `server/.../db/DatabaseFactory.kt` | Modify | Register 4 wellness tables in `allTables` |
| `server/.../feature/wellness/WellnessService.kt` | **New** | Core service (checkin, survey, observation, timeline, dashboard, alerts) |
| `server/.../feature/wellness/EarlyWarningJob.kt` | **New** | Daily AI early warning job |
| `server/.../feature/wellness/ParentSummaryJob.kt` | **New** | Weekly parent summary job |
| `server/.../feature/wellness/WellnessRouting.kt` | **New** | API endpoints |
| `docs/db/migration_080_student_wellness.sql` | **New** | DDL |

### Shared (KMP)

| File | Change Type | Description |
|---|---|---|
| `shared/.../wellness/domain/model/WellnessModels.kt` | **New** | DTOs, domain models, enums |
| `shared/.../wellness/domain/repository/WellnessRepository.kt` | **New** | Repository interfaces |
| `shared/.../wellness/data/remote/WellnessApi.kt` | **New** | HTTP API definitions |

### Client (Compose)

| File | Change Type | Description |
|---|---|---|
| `composeApp/.../ui/v2/screens/parent/MoodCheckInScreen.kt` | **New** | Mood check-in (emoji) |
| `composeApp/.../ui/v2/screens/parent/WellnessSurveyScreen.kt` | **New** | Weekly wellness survey |
| `composeApp/.../ui/v2/screens/parent/ParentWellnessSummaryScreen.kt` | **New** | Parent wellness summary |
| `composeApp/.../ui/v2/screens/teacher/WellnessObservationScreen.kt` | **New** | Teacher observation |
| `composeApp/.../ui/v2/screens/admin/WellnessDashboardScreen.kt` | **New** | Counselor dashboard |
| `composeApp/.../ui/v2/screens/admin/WellnessAlertsScreen.kt` | **New** | Alert management |
| `composeApp/.../ui/v2/screens/admin/WellnessTimelineScreen.kt` | **New** | Student timeline |

---

## 25. Future Enhancements

| # | Enhancement | Priority | Effort | Notes |
|---|---|---|---|---|
| F-1 | Wearable device integration | Low | L | Sync sleep, activity data from wearables |
| F-2 | Student self-assessment | Medium | M | Students complete self-assessment in student app |
| F-3 | Peer wellness support | Low | L | Anonymous peer support groups |
| F-4 | Wellness resources library | Medium | S | Curated mental health resources |
| F-5 | Counselor notes (private) | Medium | S | Private notes separate from intervention notes |
| F-6 | Wellness trends over terms | Low | S | Term-over-term wellness comparison |
| F-7 | Parent wellness education | Low | M | Educational content for parents |
| F-8 | Crisis escalation protocol | High | M | Automated escalation for high-severity alerts |
| F-9 | Multi-language wellness surveys | Medium | M | Surveys in regional languages |
| F-10 | Wellness gamification (positive reinforcement) | Low | M | Rewards for consistent check-ins |

---

## Appendix A: Sequence Diagrams

### A.1 Early Warning Flow

```
EarlyWarningJob    Server          AiService        Notify.kt
  │                  │                │                │
  │  execute()       │                │                │
  │  ──────────────> │                │                │
  │                  │──get students with ≥5 checkins─>│ (DB)
  │                  │←──student list──────────────────│
  │                  │                │                │
  │                  │──for each student:              │
  │                  │  gather mood, survey, obs       │
  │                  │  build AI prompt                │
  │                  │──analyze────────>│              │
  │                  │←──{alert_needed, severity,      │
  │                  │    explanation}──│              │
  │                  │                │                │
  │                  │──if alert_needed:               │
  │                  │  create wellness_alert          │
  │                  │──send notification──────────────>│
  │                  │←──success──────────────────────│
  │                  │                │                │
  │  ←──count of alerts created       │                │
  │                  │                │                │
```

### A.2 Alert Management Flow

```
Counselor (app)    Server              DB
  │                  │                   │
  │  GET /alerts     │                   │
  │  ?status=open    │                   │
  │  ──────────────> │                   │
  │                  │──get open alerts──>│
  │                  │←──alert list──────│
  │  ←──200: alerts  │                   │
  │                  │                   │
  │  POST /acknowledge│                   │
  │  ──────────────> │                   │
  │                  │──update status────>│
  │                  │  acknowledged_by  │
  │                  │  acknowledged_at  │
  │                  │←──success─────────│
  │  ←──200: success │                   │
  │                  │                   │
  │  POST /intervene │                   │
  │  {notes}         │                   │
  │  ──────────────> │                   │
  │                  │──update status────>│
  │                  │  intervention_notes│
  │                  │←──success─────────│
  │  ←──200: success │                   │
  │                  │                   │
```

---

## Appendix B: Domain Model / ER Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                    wellness_checkins (new)                            │
│  id (PK)                                                              │
│  school_id, student_id                                                │
│  mood (happy|okay|sad|anxious|angry)                                  │
│  checkin_date, notes                                                  │
│  created_at                                                           │
│  UNIQUE: (student_id, checkin_date)                                   │
│  INDEX: (student_id, checkin_date DESC)                               │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│                    wellness_surveys (new)                             │
│  id (PK)                                                              │
│  school_id, student_id                                                │
│  survey_date                                                          │
│  stress_level, sleep_quality, appetite, social_interaction,           │
│  energy_level (all 1-5)                                               │
│  overall_score (REAL, avg of 5)                                       │
│  created_at                                                           │
│  UNIQUE: (student_id, survey_date)                                    │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│                  teacher_observations (new)                           │
│  id (PK)                                                              │
│  school_id, student_id, teacher_id                                    │
│  observation_date                                                     │
│  behavior (engaged|withdrawn|disruptive|tired|anxious|happy|focused)  │
│  notes, severity (normal|concern|urgent)                              │
│  created_at                                                           │
│  INDEX: (student_id, observation_date DESC)                           │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│                    wellness_alerts (new)                              │
│  id (PK)                                                              │
│  school_id, student_id                                                │
│  alert_type (declining_mood|low_survey_score|teacher_concern|         │
│              pattern_change)                                          │
│  severity (low|medium|high)                                           │
│  trigger_data (JSON), ai_analysis                                     │
│  status (open|acknowledged|intervened|resolved)                       │
│  acknowledged_by, acknowledged_at                                     │
│  intervention_notes                                                   │
│  created_at                                                           │
│  INDEX: (school_id, status, created_at DESC)                          │
└──────────────────────────────────────────────────────────────────────┘

Existing tables used (read-only):
┌────────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│ holistic_assessments│  │ health_records   │  │ students         │
│ (existing)          │  │ (existing)       │  │ (existing)       │
└────────────────────┘  └──────────────────┘  └──────────────────┘
┌──────────────────┐  ┌──────────────────┐
│ users            │  │ classes          │
│ (existing)       │  │ (existing)       │
└──────────────────┘  └──────────────────┘
```

---

## Appendix C: Event Flow

### Domain Events

| Event | Emitter | Consumers | Payload | Side Effects |
|---|---|---|---|---|
| `MoodCheckinSubmitted` | `WellnessService.checkin()` | None (logged) | `studentId, mood, checkinDate` | Check-in recorded |
| `WellnessSurveySubmitted` | `WellnessService.submitSurvey()` | None (logged) | `studentId, overallScore, surveyDate` | Survey recorded |
| `TeacherObservationLogged` | `WellnessService.logObservation()` | None (logged) | `teacherId, studentId, behavior, severity` | Observation recorded |
| `WellnessAlertCreated` | `EarlyWarningJob` | `Notify.kt` | `studentId, alertType, severity, aiAnalysis` | Notification to counselor/admin |
| `AlertAcknowledged` | `WellnessService.acknowledgeAlert()` | `Notify.kt` | `alertId, userId, studentId` | Notification to admin |
| `InterventionLogged` | `WellnessService.logIntervention()` | None (logged) | `alertId, userId, notes` | Alert status updated |
| `AlertResolved` | `WellnessService.resolveAlert()` | `Notify.kt` | `alertId, userId, studentId` | Notification to admin |
| `ParentSummaryGenerated` | `ParentSummaryJob` | `Notify.kt` | `childId, parentId, summary` | Notification to parent |

### Event Delivery Guarantees

- Events emitted synchronously within service methods
- Notification dispatch is async (fire-and-forget via `Notify.kt`)
- Early warning and parent summary events emitted within job execution

---

## Appendix D: Configuration

### Environment Variables

| Variable | Default | Description |
|---|---|---|
| `WELLNESS_ENABLED` | `true` | Enable/disable wellness feature |
| `WELLNESS_EARLY_WARNING_ENABLED` | `true` | Enable/disable AI early warning |
| `WELLNESS_PARENT_SUMMARY_ENABLED` | `true` | Enable/disable parent summary |
| `WELLNESS_MIN_CHECKINS_FOR_WARNING` | `5` | Minimum check-ins for AI analysis |
| `WELLNESS_ANALYSIS_WINDOW_DAYS` | `14` | Analysis window in days |
| `WELLNESS_AI_MODEL` | `wellness_v1` | AI model for early warning |
| `WELLNESS_SUMMARY_AI_MODEL` | `wellness_summary_v1` | AI model for parent summary |

### Feature Flags

| Flag | Default | Description |
|---|---|---|
| `WELLNESS_ENABLED` | `true` | Enable/disable wellness |
| `WELLNESS_EARLY_WARNING_ENABLED` | `true` | Enable/disable early warning |
| `WELLNESS_PARENT_SUMMARY_ENABLED` | `true` | Enable/disable parent summary |
| `WELLNESS_TEACHER_OBSERVATION_ENABLED` | `true` | Enable/disable teacher observations |

### School-Level Settings

| Setting | Default | Description |
|---|---|---|
| `wellness_enabled` | `true` | Per-school enable/disable |
| `wellness_parent_summary_enabled` | `true` | Per-school parent summary toggle |
| `wellness_survey_frequency` | `weekly` | Survey frequency |

---

## Appendix E: Migration & Rollback

### Migration: `migration_080_student_wellness.sql`

```sql
-- Migration 080: Student Wellness Tracking
-- Creates wellness_checkins, wellness_surveys, teacher_observations, wellness_alerts tables

BEGIN;

CREATE TABLE IF NOT EXISTS wellness_checkins (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    student_id      UUID NOT NULL,
    mood            VARCHAR(16) NOT NULL,
    checkin_date    DATE NOT NULL,
    notes           TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(student_id, checkin_date)
);

CREATE INDEX IF NOT EXISTS idx_wellness_checkins_student
    ON wellness_checkins (student_id, checkin_date DESC);

CREATE TABLE IF NOT EXISTS wellness_surveys (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    student_id      UUID NOT NULL,
    survey_date     DATE NOT NULL,
    stress_level    INTEGER NOT NULL,
    sleep_quality   INTEGER NOT NULL,
    appetite        INTEGER NOT NULL,
    social_interaction INTEGER NOT NULL,
    energy_level    INTEGER NOT NULL,
    overall_score   REAL NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(student_id, survey_date)
);

CREATE TABLE IF NOT EXISTS teacher_observations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    student_id      UUID NOT NULL,
    teacher_id      UUID NOT NULL,
    observation_date DATE NOT NULL,
    behavior        VARCHAR(32) NOT NULL,
    notes           TEXT,
    severity        VARCHAR(16) NOT NULL DEFAULT 'normal',
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_teacher_obs_student
    ON teacher_observations (student_id, observation_date DESC);

CREATE TABLE IF NOT EXISTS wellness_alerts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    student_id      UUID NOT NULL,
    alert_type      VARCHAR(32) NOT NULL,
    severity        VARCHAR(16) NOT NULL,
    trigger_data    TEXT NOT NULL,
    ai_analysis     TEXT,
    status          VARCHAR(16) NOT NULL DEFAULT 'open',
    acknowledged_by UUID,
    acknowledged_at TIMESTAMP,
    intervention_notes TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_wellness_alerts_school
    ON wellness_alerts (school_id, status, created_at DESC);

COMMIT;
```

### Rollback: `migration_080_rollback.sql`

```sql
BEGIN;
DROP TABLE IF EXISTS wellness_alerts;
DROP TABLE IF EXISTS teacher_observations;
DROP TABLE IF EXISTS wellness_surveys;
DROP TABLE IF EXISTS wellness_checkins;
COMMIT;
```

### Migration Validation

- Verify 4 tables created with correct columns
- Verify `wellness_checkins(student_id, checkin_date)` UNIQUE constraint
- Verify `wellness_surveys(student_id, survey_date)` UNIQUE constraint
- Verify indexes created
- Run `SELECT count(*) FROM wellness_checkins` — should be 0 (new feature)

---

## Appendix F: Observability

### Structured Logging

| Log Level | Event | Context Fields |
|---|---|---|
| INFO | Mood check-in submitted | `studentId, mood, checkinDate` |
| INFO | Wellness survey submitted | `studentId, overallScore, surveyDate` |
| INFO | Teacher observation logged | `teacherId, studentId, behavior, severity` |
| INFO | Wellness alert created | `studentId, alertType, severity` |
| INFO | Alert acknowledged | `alertId, userId, studentId` |
| INFO | Intervention logged | `alertId, userId` |
| INFO | Alert resolved | `alertId, userId, studentId` |
| INFO | Parent summary generated | `childId, parentId` |
| INFO | Parent opted in | `parentId, childId` |
| INFO | Parent opted out | `parentId, childId` |
| INFO | Early warning job completed | `schoolCount, alertsCreated, duration` |
| INFO | Parent summary job completed | `summaryCount, duration` |
| WARN | Student has < 5 check-ins, skipping | `studentId, checkinCount` |
| WARN | AI analysis failed for student | `studentId, error` |
| WARN | No counselor assigned | `schoolId` |
| WARN | Parent summary generation failed | `childId, parentId, error` |
| ERROR | Early warning job failed | `error` |
| ERROR | Parent summary job failed | `error` |

### Metrics

| Metric | Type | Labels | Description |
|---|---|---|---|
| `wellness_checkins_total` | Counter | `school_id, mood` | Total mood check-ins |
| `wellness_surveys_total` | Counter | `school_id` | Total wellness surveys |
| `wellness_observations_total` | Counter | `school_id, behavior, severity` | Total teacher observations |
| `wellness_alerts_created` | Counter | `school_id, alert_type, severity` | Total alerts created |
| `wellness_alerts_resolved` | Counter | `school_id` | Total alerts resolved |
| `wellness_parent_summaries` | Counter | `school_id` | Total parent summaries generated |
| `wellness_parent_opt_ins` | Counter | `school_id` | Total parent opt-ins |
| `wellness_early_warning_duration` | Histogram | `school_id` | Early warning job duration |
| `wellness_ai_analysis_duration` | Histogram | — | AI analysis latency per student |
| `wellness_active_alerts` | Gauge | `school_id, severity` | Currently open alerts |

### Health Checks

| Check | Endpoint | Description |
|---|---|---|
| Wellness service | `/health/wellness` | Verify service and DB accessible |
| Early warning job | `/health/early-warning` | Verify last job run and status |

### Alerts

| Alert | Condition | Severity | Notification |
|---|---|---|---|
| Early warning job failed | Job error rate > 10% | Warning | Email to dev team |
| Early warning job slow | Duration > 30 minutes | Info | Email to dev team |
| AI analysis failure rate | > 20% | Warning | Email to dev team |
| High-severity alert unresolved > 24h | Alert age > 24h | Critical | Email + SMS to counselor and admin |
| Parent summary failure rate | > 10% | Info | Email to dev team |

### Dashboards

| Dashboard | Panels | Audience |
|---|---|---|
| Wellness Overview | Check-ins/day, surveys/week, active alerts, participation rate | Product Team |
| Alert Management | Alerts by severity, resolution time, intervention rate | Counselor, Admin |
| Job Performance | Early warning duration, AI analysis duration, error rate | Dev Team |
| Parent Engagement | Opt-in rate, summary views, opt-out rate | Product Team |

### Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Privacy breach (wellness data exposed) | Low | Critical | Role-based access. Audit logging. Encryption at rest. |
| AI false positive (alert when not needed) | Medium | Medium | Counselor reviews. AI is advisory. Can dismiss alert. |
| AI false negative (miss declining pattern) | Medium | High | Daily job. Multiple data sources. Counselor can manually review. |
| Parent sees clinical data | Low | High | Non-clinical summary only. Opt-in required. Raw data not exposed. |
| Student gaming check-ins | Medium | Low | Teacher observations cross-validate. AI considers multiple sources. |
| Counselor overwhelmed with alerts | Medium | Medium | Severity-based filtering. Batch notifications. Alert fatigue monitoring. |
| AI sends student name to LLM | Low | High | Anonymized IDs in AI prompt. No PII in LLM context. |
| Wellness data used for discrimination | Low | High | Access restricted. Audit trail. Data not shared with external parties. |
