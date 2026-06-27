# Online Assignments & Submissions — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** None (extends existing homework system)

---

## 1. Feature Overview

Extend the existing homework system to support online assignment submission: students submit answers/files digitally, teachers review and grade online, with plagiarism check integration and auto-grading for objective questions.

### Goals

- Teacher creates online assignment (objective questions, subjective questions, file upload)
- Student submits answers online (text, file upload, or both)
- Auto-grading for objective questions (MCQ, true/false, fill-in-the-blank)
- Teacher reviews subjective answers and assigns marks
- Plagiarism check for text submissions (optional, via API)
- Submission status tracking (submitted, late, graded, returned)
- Resubmission support (configurable)

---

## 2. Current System Assessment

- `HomeworkTable` (`Tables.kt:920-945`) — homework with `title`, `description`, `dueDate`, `subjectId`, `classId`
- `HomeworkSubmissionsTable` (`Tables.kt:960-975`) — has `submittedAt`, `content`, `status` (SUBMITTED/GRADED/RETURNED), `teacherFeedback`, `marks`
- `HomeworkAttachmentsTable` — file attachments for homework
- `HomeworkExtensionsTable` — extension requests
- Existing system supports text submissions but no structured questions or auto-grading

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Teacher creates assignment with structured questions: MCQ, true/false, fill-in-the-blank, short answer, long answer, file upload |
| FR-2 | Student submits answers online per question |
| FR-3 | Auto-grading for objective questions (MCQ, true/false, fill-in) |
| FR-4 | Teacher reviews subjective answers, assigns marks, provides feedback |
| FR-5 | Submission status: not_started | in_progress | submitted | late | graded | returned |
| FR-6 | Resubmission allowed (configurable per assignment) |
| FR-7 | Late submission penalty (configurable: % deduction per day) |
| FR-8 | Plagiarism check (optional, via external API) |
| FR-9 | Assignment results feed into `AssessmentMarksTable` |

---

## 4. Database Design

### 4.1 New Table: `assignment_questions`

```sql
CREATE TABLE assignment_questions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    homework_id     UUID NOT NULL REFERENCES homework(id) ON DELETE CASCADE,
    question_type   VARCHAR(16) NOT NULL,          -- mcq | true_false | fill_blank | short_answer | long_answer | file_upload
    question_text   TEXT NOT NULL,
    options         TEXT,                          -- JSON array for MCQ: ["Option A", "Option B", ...]
    correct_answer  TEXT,                          -- for auto-grading: "B" or "true" or "Paris"
    max_marks       INTEGER NOT NULL DEFAULT 1,
    sequence        INTEGER NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_assignment_questions_hw ON assignment_questions(homework_id, sequence);
```

### 4.2 New Table: `assignment_submissions`

```sql
CREATE TABLE assignment_submissions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    homework_id     UUID NOT NULL REFERENCES homework(id) ON DELETE CASCADE,
    student_id      UUID NOT NULL,
    school_id       UUID NOT NULL,
    answers         TEXT NOT NULL,                 -- JSON: [{"question_id": "uuid", "answer": "...", "file_url": "..."}]
    auto_graded_marks INTEGER,                     -- marks from auto-graded questions
    teacher_graded_marks INTEGER,                  -- marks from teacher review
    total_marks     INTEGER,                       -- auto + teacher
    status          VARCHAR(16) NOT NULL DEFAULT 'in_progress', -- in_progress | submitted | late | graded | returned
    submitted_at    TIMESTAMP,
    graded_at       TIMESTAMP,
    graded_by       UUID,
    teacher_feedback TEXT,
    plagiarism_score REAL,                         -- 0-1 (if checked)
    is_resubmission BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(homework_id, student_id)
);
CREATE INDEX idx_assignment_submissions_hw ON assignment_submissions(homework_id, status);
```

### 4.3 Modify Existing: `homework`

```sql
ALTER TABLE homework ADD COLUMN is_online_assignment BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE homework ADD COLUMN allow_resubmission BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE homework ADD COLUMN late_penalty_percent REAL NOT NULL DEFAULT 0;
ALTER TABLE homework ADD COLUMN total_marks INTEGER;
```

---

## 5. Backend Architecture

### 5.1 AssignmentService

```kotlin
class AssignmentService {
    suspend fun createQuestions(homeworkId: UUID, questions: List<QuestionDto>)
    suspend fun submitAnswers(homeworkId: UUID, studentId: UUID, answers: List<AnswerDto>): SubmissionDto
    suspend fun autoGrade(submissionId: UUID): Int  // returns auto-graded marks
    suspend fun teacherGrade(submissionId: UUID, grades: Map<UUID, Int>, feedback: String)
    suspend fun returnSubmission(submissionId: UUID, feedback: String)
    suspend fun checkPlagiarism(submissionId: UUID): Float  // returns score 0-1
}
```

### 5.2 Auto-Grading Logic

```kotlin
fun autoGrade(answers: List<AnswerDto>, questions: List<AssignmentQuestion>): Int {
    var marks = 0
    for (question in questions) {
        if (question.questionType in listOf("mcq", "true_false", "fill_blank")) {
            val answer = answers.find { it.questionId == question.id }
            if (answer?.answer?.trim()?.equals(question.correctAnswer?.trim(), ignoreCase = true) == true) {
                marks += question.maxMarks
            }
        }
    }
    return marks
}
```

---

## 6. API Contracts

```
# Teacher
POST /api/v1/teacher/assignments/{homeworkId}/questions
GET /api/v1/teacher/assignments/{homeworkId}/submissions
POST /api/v1/teacher/assignments/submissions/{id}/grade
POST /api/v1/teacher/assignments/submissions/{id}/return

# Student (via parent app for now, or student app in future)
GET /api/v1/parent/assignments/{homeworkId}
POST /api/v1/parent/assignments/{homeworkId}/submit
```

---

## 7. Acceptance Criteria

- [ ] Teacher creates assignments with structured questions (MCQ, true/false, fill-in, short/long answer, file upload)
- [ ] Student submits answers online
- [ ] Auto-grading works for objective questions
- [ ] Teacher reviews and grades subjective answers
- [ ] Submission status tracked correctly
- [ ] Late submission penalty applied
- [ ] Resubmission supported when enabled
- [ ] Results can feed into assessment marks

---

## 8. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration, Exposed tables |
| 2 | 2 days | AssignmentService (questions, submissions, auto-grading) |
| 3 | 2 days | Teacher grading + return workflow |
| 4 | 1 day | Plagiarism check integration (optional) |
| 5 | 2 days | API endpoints |
| 6 | 3 days | Client UI (question editor, student answer view, grading interface) |
| 7 | 2 days | Tests |

---

## 9. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add + Modify | 2 new tables + columns on homework |
| `server/.../feature/assignment/AssignmentService.kt` | New | Core service |
| `server/.../feature/assignment/AssignmentRouting.kt` | New | API endpoints |
| `docs/db/migration_059_online_assignments.sql` | New | DDL |
| `composeApp/.../ui/v2/screens/teacher/AssignmentEditorScreen.kt` | New | Question editor |
| `composeApp/.../ui/v2/screens/teacher/AssignmentGradingScreen.kt` | New | Grading interface |
| `composeApp/.../ui/v2/screens/parent/AssignmentSubmitScreen.kt` | New | Student submission UI |
