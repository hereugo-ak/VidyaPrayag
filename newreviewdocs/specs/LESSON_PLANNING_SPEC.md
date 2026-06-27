# Lesson Planning — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** None

---

## 1. Feature Overview

Digital lesson planning for teachers: create, store, and share lesson plans linked to curriculum units and syllabus progress. Enables structured teaching with objectives, activities, resources, and assessment methods.

### Goals

- Teacher creates lesson plans per subject per class
- Link to curriculum units (`CurriculumUnitsTable`) and syllabus progress (`SyllabusProgressTable`)
- Lesson plan: objectives, activities, resources, duration, assessment method, homework link
- Admin can review lesson plans
- Reusable templates per subject

---

## 2. Current System Assessment

- `feature_audit.csv` L126: Lesson Planning missing (0%)
- `CurriculumUnitsTable` (`Tables.kt:870-885`) — curriculum tracking with `unitNumber`, `title`, `description`, `topics`
- `SyllabusProgressTable` — tracks completion percentage per unit
- `HomeworkTable` — homework assignments that can be linked to lessons
- `TeacherSubjectAssignmentsTable` — teacher→subject→class mapping

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Teacher creates lesson plan: title, subject, class, unit (optional), objectives, activities, resources, duration, assessment method |
| FR-2 | Link to curriculum unit and update syllabus progress on completion |
| FR-3 | Link homework assignment to lesson plan |
| FR-4 | Admin can view all lesson plans (read-only review) |
| FR-5 | Save as template for reuse |
| FR-6 | Lesson plan calendar view (what's planned for each day) |

---

## 4. Database Design

```sql
CREATE TABLE lesson_plans (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    teacher_id      UUID NOT NULL,
    class_id        UUID NOT NULL,
    subject_id      UUID,
    subject_name    TEXT NOT NULL,
    curriculum_unit_id UUID,                       -- FK curriculum_units.id
    title           TEXT NOT NULL,
    objectives      TEXT NOT NULL,                 -- newline-separated or JSON array
    activities      TEXT,                          -- JSON: [{"activity": "...", "duration_min": 15}]
    resources       TEXT,                          -- JSON: ["Textbook pg 45", "Video: ..."]
    assessment_method TEXT,
    duration_minutes INTEGER NOT NULL DEFAULT 45,
    homework_id     UUID,                          -- FK homework.id (optional)
    planned_date    DATE,
    status          VARCHAR(16) NOT NULL DEFAULT 'planned', -- planned | completed | skipped
    is_template     BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_lesson_plans_teacher ON lesson_plans(teacher_id, planned_date);
CREATE INDEX idx_lesson_plans_class ON lesson_plans(class_id, subject_name);
```

---

## 5. API Contracts

```
GET/POST /api/v1/teacher/lesson-plans
PATCH /api/v1/teacher/lesson-plans/{id}
POST /api/v1/teacher/lesson-plans/{id}/complete  -- marks completed, updates syllabus progress
GET /api/v1/teacher/lesson-plans/calendar?month={YYYY-MM}
GET /api/v1/school/lesson-plans?teacher_id={uuid}  -- admin review
```

---

## 6. Acceptance Criteria

- [ ] Teacher creates lesson plans with objectives, activities, resources
- [ ] Lesson plans link to curriculum units
- [ | Completing a lesson updates syllabus progress
- [ ] Homework linked to lesson plan
- [ ] Admin can review lesson plans
- [ ] Calendar view shows planned lessons per day
- [ ] Templates can be saved and reused

---

## 7. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration, Exposed table |
| 2 | 2 days | LessonPlanService + syllabus progress integration |
| 3 | 1 day | API endpoints |
| 4 | 3 days | Client UI (plan editor, calendar view, template management) |
| 5 | 1 day | Tests |

---

## 8. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | `LessonPlansTable` |
| `server/.../feature/lesson/LessonPlanService.kt` | New | Core service |
| `docs/db/migration_058_lesson_planning.sql` | New | DDL |
| `composeApp/.../ui/v2/screens/teacher/LessonPlanScreen.kt` | New | Lesson plan editor |
| `composeApp/.../ui/v2/screens/teacher/LessonCalendarScreen.kt` | New | Calendar view |
