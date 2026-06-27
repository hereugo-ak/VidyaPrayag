# Exam Timetable — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** None

---

## 1. Feature Overview

Exam schedule creation and management: define exam periods, assign subjects to date/time slots per class, generate exam timetables for students and teachers, and publish notifications.

### Goals

- Admin creates exam period (e.g., "Term 1 Exams", dates, description)
- Assign subjects to specific date/time/room per class
- Generate per-student and per-teacher exam timetable
- Publish exam timetable → notification to parents and teachers
- Export as PDF

---

## 2. Current System Assessment

- `feature_audit.csv` L125: Exam Timetable missing (0%)
- `AssessmentsTable` has `assessmentName`, `type` (UNIT_TEST, MID_TERM, FINAL, QUIZ), `date` — but no structured timetable
- `SchoolClassesTable` + `SchoolSubjectsTable` exist for class/subject mapping

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Admin creates exam period with name, start/end date, term |
| FR-2 | Assign exam slots: class, subject, date, start_time, end_time, room, max_marks |
| FR-3 | Auto-generate per-student timetable (all exams for their class) |
| FR-4 | Auto-generate per-teacher timetable (all exams they invigilate) |
| FR-5 | Publish → notification to parents + teachers |
| FR-6 | Export exam timetable as PDF |
| FR-7 | No two exams for same class on same date/time (conflict check) |

---

## 4. Database Design

```sql
CREATE TABLE exam_periods (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    name            TEXT NOT NULL,                 -- "Term 1 Final Exams"
    term            VARCHAR(16) NOT NULL,          -- term1 | term2 | term3 | annual
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    status          VARCHAR(16) NOT NULL DEFAULT 'draft', -- draft | published
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE exam_slots (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_period_id  UUID NOT NULL REFERENCES exam_periods(id) ON DELETE CASCADE,
    school_id       UUID NOT NULL,
    class_id        UUID NOT NULL,
    subject_id      UUID,
    subject_name    TEXT NOT NULL,
    exam_date       DATE NOT NULL,
    start_time      VARCHAR(8) NOT NULL,           -- "09:00"
    end_time        VARCHAR(8) NOT NULL,           -- "12:00"
    room            TEXT,
    max_marks       INTEGER NOT NULL DEFAULT 100,
    invigilator_id  UUID,                          -- FK app_users.id (teacher)
    invigilator_name TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_exam_slots_class_date ON exam_slots(class_id, exam_date);
CREATE INDEX idx_exam_slots_period ON exam_slots(exam_period_id);
```

---

## 5. API Contracts

```
GET/POST /api/v1/school/exam-periods
POST /api/v1/school/exam-periods/{id}/slots
GET /api/v1/school/exam-periods/{id}/timetable?class_id={uuid}
POST /api/v1/school/exam-periods/{id}/publish
GET /api/v1/school/exam-periods/{id}/pdf

GET /api/v1/parent/exam-timetable/{childId}
GET /api/v1/teacher/exam-timetable
```

---

## 6. Acceptance Criteria

- [ ] Admin creates exam period and assigns exam slots
- [ ] Conflict check prevents same class double-booked
- [ ] Per-student and per-teacher timetables generated
- [ ] Publish sends notifications
- [ ] PDF export works

---

## 7. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration, Exposed tables |
| 2 | 2 days | ExamTimetableService + conflict checker |
| 3 | 1 day | API endpoints + PDF generation |
| 4 | 2 days | Client UI (exam period creation, slot assignment, timetable view) |
| 5 | 1 day | Tests |

---

## 8. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | 2 exam timetable tables |
| `server/.../feature/exam/ExamTimetableService.kt` | New | Core service |
| `docs/db/migration_057_exam_timetable.sql` | New | DDL |
| `composeApp/.../ui/v2/screens/admin/ExamTimetableScreen.kt` | New | Admin UI |
| `composeApp/.../ui/v2/screens/parent/ExamTimetableScreen.kt` | New | Parent view |
