# Behavior Tracking — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** `STUDENT_WELLNESS_SPEC.md`
> **Source:** `COMPETITIVE_GAP_ANALYSIS.md` — competitor feature

---

## 1. Feature Overview

Student behavior tracking system: positive/negative behavior incidents, behavior points, pattern analysis, and parent visibility. Focuses on positive reinforcement with structured incident logging.

### Goals

- Teacher logs behavior incidents (positive: helpful, leadership, improvement; negative: disruptive, late, disrespectful)
- Behavior points system (configurable: +1 to +5 for positive, -1 to -5 for negative)
- Behavior score per student per term
- Pattern analysis: recurring behaviors, trends
- Parent visibility: weekly behavior summary
- Integration with wellness tracking (behavioral indicators)

---

## 2. Current System Assessment

- `TeacherObservationsTable` (from `STUDENT_WELLNESS_SPEC.md`) — has `behavior` field (engaged/withdrawn/disruptive/etc.)
- No dedicated behavior tracking or points system
- `COMPETITIVE_GAP_ANALYSIS.md`: behavior tracking as competitor feature

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Teacher logs behavior incident: student, type (positive/negative), category, points, description, date |
| FR-2 | Behavior categories: helpful, leadership, improvement, participation (positive); disruptive, late, disrespectful, bullying (negative) |
| FR-3 | Points: configurable per category (+1 to +5, -1 to -5) |
| FR-4 | Behavior score: cumulative per student per term |
| FR-5 | Pattern analysis: AI identifies recurring behaviors and trends |
| FR-6 | Parent weekly summary: behavior incidents + score |
| FR-7 | Admin dashboard: class-wise behavior overview |
| FR-8 | Integration with `VIDYA_PASSPORT_SPEC.md` (positive behaviors → badges) |

---

## 4. Database Design

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

---

## 5. API Contracts

```
# Teacher
POST /api/v1/teacher/behavior/incident  { student_id, incident_type, category, points, description, date }
GET /api/v1/teacher/behavior/student/{studentId}?term=term1

# Admin
GET /api/v1/school/behavior/dashboard?class_id={uuid}
GET/POST /api/v1/school/behavior/categories

# Parent
GET /api/v1/parent/behavior/{childId}?term=term1
```

---

## 6. Acceptance Criteria

- [ ] Teacher logs positive and negative behavior incidents
- [ ] Points system configurable per category
- [ ] Behavior score calculated per student per term
- [ ] AI pattern analysis identifies recurring behaviors
- [ ] Parent weekly summary shows behavior incidents + score
- [ ] Admin dashboard shows class-wise behavior overview
- [ ] Positive behaviors trigger badge awards in Vidya Passport

---

## 7. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration, Exposed tables, seed categories |
| 2 | 2 days | BehaviorService (log, score, pattern analysis) |
| 3 | 1 day | API endpoints |
| 4 | 3 days | Client UI (teacher incident log, admin dashboard, parent summary) |
| 5 | 1 day | Tests |

---

## 8. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | 2 behavior tables |
| `server/.../feature/behavior/BehaviorService.kt` | New | Core service |
| `docs/db/migration_087_behavior_tracking.sql` | New | DDL |
| `composeApp/.../ui/v2/screens/teacher/BehaviorLogScreen.kt` | New | Incident logging |
| `composeApp/.../ui/v2/screens/admin/BehaviorDashboardScreen.kt` | New | Admin dashboard |
