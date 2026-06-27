# AI Timetable Auto-Generator — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** `AI_INFRASTRUCTURE_SPEC.md`

---

## 1. Feature Overview

AI-powered timetable generation that creates optimized class schedules considering teacher availability, subject priorities, room constraints, and no-conflict guarantees. Uses constraint satisfaction + LLM for natural language preferences.

### Goals

- Generate conflict-free timetable for all classes in a school
- Respect teacher availability (no double-booking)
- Optimize subject distribution (no 2 consecutive Math periods)
- Support constraints: teacher preferences, room availability, lunch breaks
- LLM interprets natural language constraints ("Don't schedule PE after lunch")
- Manual override after generation

---

## 2. Current System Assessment

- `TeacherPeriodsTable` + `PeriodExceptionsTable` — existing teacher schedule tracking
- `SchoolClassesTable` + `SchoolSubjectsTable` + `TeacherSubjectAssignmentsTable` — class/subject/teacher mapping
- No timetable generation exists
- `feature_audit.csv` L125: Exam Timetable missing; no weekly timetable either

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Generate weekly timetable for all classes given: subjects, teacher assignments, periods per day, working days |
| FR-2 | No teacher double-booked in same period |
| FR-3 | Subject distribution: max 1 period per subject per day (configurable) |
| FR-4 | Support natural language constraints via LLM parsing |
| FR-5 | Manual override: admin can drag-and-drop swap periods |
| FR-6 | Regenerate single class without affecting others |
| FR-7 | Export timetable as PDF and image |

---

## 4. Database Design

### 4.1 New Table: `timetables`

```sql
CREATE TABLE timetables (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    class_id        UUID NOT NULL,
    academic_year_id UUID NOT NULL,
    status          VARCHAR(16) NOT NULL DEFAULT 'draft', -- draft | published
    generated_by    VARCHAR(16) NOT NULL DEFAULT 'ai',    -- ai | manual
    constraints_text TEXT,                                -- natural language constraints
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(school_id, class_id, academic_year_id)
);
```

### 4.2 New Table: `timetable_slots`

```sql
CREATE TABLE timetable_slots (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    timetable_id    UUID NOT NULL REFERENCES timetables(id) ON DELETE CASCADE,
    day_of_week     INTEGER NOT NULL,            -- 1=Mon, 7=Sun
    period_number   INTEGER NOT NULL,
    subject_id      UUID,                        -- FK school_subjects.id
    teacher_id      UUID,                        -- FK app_users.id
    room            TEXT,
    start_time      VARCHAR(8),                  -- "09:00"
    end_time        VARCHAR(8),                  -- "09:45"
    UNIQUE(timetable_id, day_of_week, period_number)
);
CREATE INDEX idx_tt_slots_teacher ON timetable_slots(teacher_id, day_of_week, period_number);
```

---

## 5. Backend Architecture

### 5.1 TimetableGeneratorService

```kotlin
class TimetableGeneratorService(private val aiService: AiService) {
    suspend fun generate(
        schoolId: UUID, classId: UUID, academicYearId: UUID, constraints: String?
    ): TimetableDto {
        // 1. Fetch: subjects for class, teacher assignments, periods config
        // 2. If natural language constraints, use LLM to parse into structured rules
        // 3. Run constraint satisfaction algorithm (backtracking + heuristics)
        // 4. Verify no conflicts (teacher double-booking)
        // 5. Store timetable + slots
        // 6. Return generated timetable
    }
}
```

### 5.2 Constraint Satisfaction

Core algorithm (not LLM — LLM only parses natural language):
- Backtracking with forward-checking
- Variables: (day, period) → (subject, teacher)
- Constraints: no teacher conflict, subject distribution, room availability
- Heuristics: most-constrained-first (subjects with fewest available teachers scheduled first)

---

## 6. API Contracts

```
POST /api/v1/school/timetable/generate
{
  "class_id": "uuid",
  "constraints": "Don't schedule Mathematics in the last period. PE should be after lunch."
}
```

```
GET /api/v1/school/timetable/{classId}
PATCH /api/v1/school/timetable/slots/{slotId}
POST /api/v1/school/timetable/{id}/publish
GET /api/v1/school/timetable/{id}/pdf
```

---

## 7. Acceptance Criteria

- [ ] Generated timetable has zero teacher conflicts
- [ ] Subject distribution rules respected
- [ ] Natural language constraints parsed and applied
- [ ] Manual override (swap periods) works
- [ ] Export as PDF
- [ ] Regeneration for single class doesn't affect others

---

## 8. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration, Exposed tables |
| 2 | 3 days | Constraint satisfaction algorithm |
| 3 | 1 day | LLM constraint parsing |
| 4 | 2 days | API endpoints + manual override |
| 5 | 2 days | Client UI (timetable grid, drag-swap, PDF export) |
| 6 | 1 day | Tests |

---

## 9. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | `TimetablesTable`, `TimetableSlotsTable` |
| `server/.../feature/timetable/TimetableGeneratorService.kt` | New | Core generator |
| `server/.../feature/timetable/ConstraintParser.kt` | New | LLM constraint parsing |
| `server/.../feature/timetable/TimetableRouting.kt` | New | API endpoints |
| `docs/db/migration_041_ai_timetable.sql` | New | DDL |
| `composeApp/.../ui/v2/screens/admin/TimetableScreen.kt` | New | Timetable grid UI |
