# Teacher Copilot — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** `AI_INFRASTRUCTURE_SPEC.md`
> **Source:** `DIFFERENTIATING_FEATURES.md` §5.1

---

## 1. Feature Overview

AI-powered assistant for teachers that helps with lesson planning, question paper generation, rubric creation, answer grading assistance, and parent communication drafting. Reduces teacher workload by automating repetitive content creation.

### Goals

- Generate lesson plans from syllabus topics
- Create question papers (MCQ, short answer, long answer) with answer key
- Generate grading rubrics for subjective questions
- AI-assisted grading: suggest marks + feedback for student answers
- Draft parent communication (performance updates, concerns, appreciation)
- Generate worksheets and practice sets
- All outputs editable by teacher before use

---

## 2. Current System Assessment

- `LESSON_PLANNING_SPEC.md` — lesson plan system (AI Copilot integrates)
- `ONLINE_ASSIGNMENTS_SPEC.md` — assignment questions (AI Copilot generates)
- `AssessmentsTable` + `AssessmentMarksTable` — grading context
- `DIFFERENTIATING_FEATURES.md` §5.1: Teacher Copilot, effort L, data readiness: "Curriculum + marks data exists"

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Generate lesson plan from topic: objectives, activities, resources, assessment |
| FR-2 | Generate question paper: specify subject, topics, marks distribution, difficulty mix, question types |
| FR-3 | Generate grading rubric for subjective questions (criteria + marks allocation) |
| FR-4 | AI-assisted grading: teacher inputs student answer + question → AI suggests marks + feedback |
| FR-5 | Draft parent communication: select student + context → AI generates personalized message |
| FR-6 | Generate worksheet: practice questions with answer key |
| FR-7 | All AI outputs are editable (teacher reviews before saving/sending) |
| FR-8 | Usage tracking (per teacher per day) with rate limiting |

---

## 4. Database Design

```sql
CREATE TABLE teacher_copilot_usage (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    teacher_id      UUID NOT NULL,
    usage_type      VARCHAR(32) NOT NULL,          -- lesson_plan | question_paper | rubric | grading_assist | parent_comm | worksheet
    subject         TEXT,
    input_context   TEXT,                          -- JSON: parameters provided by teacher
    output_content  TEXT NOT NULL,                 -- JSON: AI-generated content
    is_edited       BOOLEAN NOT NULL DEFAULT false,
    model_used      VARCHAR(64),
    tokens_used     INTEGER,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_copilot_usage_teacher ON teacher_copilot_usage(teacher_id, created_at DESC);

CREATE TABLE teacher_copilot_daily (
    teacher_id      UUID NOT NULL,
    date            DATE NOT NULL,
    usage_count     INTEGER NOT NULL DEFAULT 0,
    tokens_used     INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (teacher_id, date)
);
```

---

## 5. Backend Architecture

### 5.1 TeacherCopilotService

```kotlin
class TeacherCopilotService(private val aiService: AiService) {
    suspend fun generateLessonPlan(topic: String, subject: String, grade: String, duration: Int): LessonPlanDto
    suspend fun generateQuestionPaper(request: QuestionPaperRequest): QuestionPaperDto
    suspend fun generateRubric(question: String, maxMarks: Int): RubricDto
    suspend fun assistGrading(question: String, modelAnswer: String, studentAnswer: String, maxMarks: Int): GradingSuggestion
    suspend fun draftParentCommunication(studentId: UUID, context: String, tone: String): String
    suspend fun generateWorksheet(subject: String, topics: List<String>, questionCount: Int, difficulty: String): WorksheetDto
}
```

### 5.2 Prompt Templates

**Question paper generation:**
```
System: You are an expert question paper setter for Indian schools.
Generate a question paper with these specifications:
- Subject: {{subject}}, Grade: {{grade}}
- Total marks: {{total_marks}}, Duration: {{duration}} min
- Question types: {{types}} (MCQ, short_answer, long_answer)
- Topic distribution: {{topics_with_marks}}
- Difficulty mix: {{easy}}% easy, {{medium}}% medium, {{hard}}% hard

Output JSON: [{"question": "...", "type": "...", "marks": ..., "topic": "...", "answer_key": "..."}]
```

**Grading assistance:**
```
System: You are assisting a teacher in grading. Based on the question, model answer, and student's answer:
1. Suggest a mark (0 to {{max_marks}})
2. Provide brief feedback (what's correct, what's missing)
3. Identify key concepts the student got right/wrong

Question: {{question}}
Model Answer: {{model_answer}}
Student Answer: {{student_answer}}
Max Marks: {{max_marks}}

Output JSON: {"suggested_marks": N, "feedback": "...", "correct_concepts": [...], "missing_concepts": [...]}
```

---

## 6. API Contracts

```
POST /api/v1/teacher/copilot/lesson-plan  { topic, subject, grade, duration }
POST /api/v1/teacher/copilot/question-paper  { subject, grade, total_marks, types, topics, difficulty_mix }
POST /api/v1/teacher/copilot/rubric  { question, max_marks }
POST /api/v1/teacher/copilot/grading-assist  { question, model_answer, student_answer, max_marks }
POST /api/v1/teacher/copilot/parent-comm  { student_id, context, tone }
POST /api/v1/teacher/copilot/worksheet  { subject, topics, question_count, difficulty }
GET /api/v1/teacher/copilot/history?page={n}
```

---

## 7. Acceptance Criteria

- [ ] Lesson plans generated with objectives, activities, resources
- [ ] Question papers with correct marks distribution and answer keys
- [ ] Rubrics with criteria and marks allocation
- [ ] Grading assistance suggests marks + feedback
- [ ] Parent communication drafted with appropriate tone
- [ ] Worksheets with practice questions + answer key
- [ ] All outputs editable before use
- [ ] Rate limiting enforced

---

## 8. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration, Exposed tables |
| 2 | 3 days | TeacherCopilotService (all 6 functions) |
| 3 | 2 days | Prompt templates + seed |
| 4 | 1 day | API endpoints + rate limiting |
| 5 | 4 days | Client UI (copilot dashboard, each tool, output editor) |
| 6 | 2 days | Tests |

---

## 9. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | 2 copilot tables |
| `server/.../feature/ai/copilot/TeacherCopilotService.kt` | New | Core service |
| `server/.../feature/ai/copilot/TeacherCopilotRouting.kt` | New | API endpoints |
| `docs/db/migration_077_teacher_copilot.sql` | New | DDL |
| `composeApp/.../ui/v2/screens/teacher/CopilotScreen.kt` | New | Copilot dashboard |
| `composeApp/.../ui/v2/screens/teacher/CopilotLessonPlanScreen.kt` | New | Lesson plan generator |
| `composeApp/.../ui/v2/screens/teacher/CopilotQuestionPaperScreen.kt` | New | Question paper generator |
