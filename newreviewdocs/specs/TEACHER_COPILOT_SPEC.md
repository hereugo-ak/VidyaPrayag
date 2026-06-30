# Teacher Copilot — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** `AI_INFRASTRUCTURE_SPEC.md`
> **Source:** `DIFFERENTIATING_FEATURES.md` §5.1
> **Template:** `_SPEC_TEMPLATE.md` v1 (25 mandatory + 6 optional sections)

---

## 1. Feature Overview

### What

AI-powered assistant for teachers that helps with lesson planning, question paper generation, rubric creation, answer grading assistance, and parent communication drafting. Reduces teacher workload by automating repetitive content creation.

### Why — Product Rationale

Teachers spend significant time on repetitive content creation — lesson plans, question papers, worksheets, rubrics, and parent communications. An AI copilot can generate first drafts in seconds, which teachers review and edit. This saves 30-50% of content creation time, letting teachers focus on teaching.

This is a **differentiating feature** (Priority P1, Phase 3, effort L, "High" value per `DIFFERENTIATING_FEATURES.md`). It directly addresses teacher workload — a key pain point for schools.

### What Stands Out (Competitive Moat)

From `DIFFERENTIATING_FEATURES.md` §5.1:
> "Teacher Copilot — lesson planning, question paper generation, rubric creation, grading assistance, parent communication. Data readiness: Curriculum + marks data exists."

Most school ERPs don't offer AI assistance for teachers. The key moat is **curriculum-integrated** AI — it knows the school's curriculum, marks distribution patterns, and student performance data.

### Goals

- Generate lesson plans from syllabus topics
- Create question papers (MCQ, short answer, long answer) with answer key
- Generate grading rubrics for subjective questions
- AI-assisted grading: suggest marks + feedback for student answers
- Draft parent communication (performance updates, concerns, appreciation)
- Generate worksheets and practice sets
- All outputs editable by teacher before use

### Non-goals

- [ ] Auto-grading without teacher review (AI suggests, teacher approves)
- [ ] Auto-sending parent communications (teacher reviews and sends)
- [ ] Full lesson plan execution (AI generates plan, teacher executes)
- [ ] Question paper auto-publishing (teacher reviews and publishes)
- [ ] Cross-class content sharing (future enhancement)
- [ ] Voice-based interaction (text only initially)
- [ ] Multi-language lesson plans (English + school medium initially)

### Dependencies

- `AI_INFRASTRUCTURE_SPEC.md` — LLM integration with multi-provider failover
- `LESSON_PLANNING_SPEC.md` — lesson plan system (AI Copilot integrates)
- `ONLINE_ASSIGNMENTS_SPEC.md` — assignment questions (AI Copilot generates)
- `AssessmentsTable` + `AssessmentMarksTable` — grading context
- `CurriculumUnitsTable` — curriculum context for lesson plans
- `StudentsTable` — student context for parent communication

### Related Modules

- `server/.../feature/ai/copilot/` — new AI copilot module
- `server/.../feature/ai/` — existing AI infrastructure
- `composeApp/.../ui/v2/screens/teacher/` — copilot UI screens

---

## 2. Current System Assessment

### Existing Code

- `LESSON_PLANNING_SPEC.md` — lesson plan system (AI Copilot integrates)
- `ONLINE_ASSIGNMENTS_SPEC.md` — assignment questions (AI Copilot generates)
- `AssessmentsTable` + `AssessmentMarksTable` — grading context
- `DIFFERENTIATING_FEATURES.md` §5.1: Teacher Copilot, effort L, data readiness: "Curriculum + marks data exists"

### Existing Database

- `AssessmentsTable` — assessment metadata (subject, max marks, type)
- `AssessmentMarksTable` — student marks (for grading context)
- `CurriculumUnitsTable` — curriculum units (for lesson plan generation)
- `StudentsTable` — student info (for parent communication)
- No copilot tables exist

### Existing APIs

- AI infrastructure APIs (from `AI_INFRASTRUCTURE_SPEC.md`) — LLM completion
- Lesson plan APIs (from `LESSON_PLANNING_SPEC.md`)
- Assignment APIs (from `ONLINE_ASSIGNMENTS_SPEC.md`)
- No copilot API endpoints exist

### Existing UI

- No copilot UI exists
- Teacher has lesson plan, assignment, and grading screens (no AI assistance)

### Existing Services

- `AiService` — existing LLM service with multi-provider failover
- No copilot service exists

### Existing Documentation

- `DIFFERENTIATING_FEATURES.md` §5.1 — Teacher Copilot
- `AI_INFRASTRUCTURE_SPEC.md` — AI infrastructure spec
- `LESSON_PLANNING_SPEC.md` — lesson plan spec
- `ONLINE_ASSIGNMENTS_SPEC.md` — assignment spec

### Technical Debt

| # | Gap | Details |
|---|---|---|
| TD-1 | No copilot tables | No `teacher_copilot_usage`, `teacher_copilot_daily` tables |
| TD-2 | No copilot service | No `TeacherCopilotService` for content generation |
| TD-3 | No prompt templates | No system prompts for lesson plan, question paper, rubric, grading, parent comm |
| TD-4 | No rate limiting | No per-teacher daily usage tracking |
| TD-5 | No copilot UI | No dashboard, tool screens, or output editor |
| TD-6 | No usage tracking | No history of AI-generated content |

### Gaps

| # | Gap | Impact | Severity |
|---|---|---|---|
| G1 | No AI content generation | Teachers spend hours on repetitive content creation | **High** |
| G2 | No grading assistance | Subjective grading is time-consuming and inconsistent | **High** |
| G3 | No parent comm drafting | Teachers avoid writing personalized parent updates | **Medium** |
| G4 | No question paper generation | Creating balanced question papers is time-consuming | **High** |
| G5 | No rubric generation | Rubrics are inconsistent across teachers | **Medium** |

---

## 3. Functional Requirements

### FR-001
| Field | Value |
|---|---|
| **Title** | Lesson Plan Generation |
| **Description** | Generate lesson plan from topic: objectives, activities, resources, assessment. |
| **Priority** | High |
| **User Roles** | Teacher |
| **Acceptance notes** | AI generates structured lesson plan with learning objectives, activities, resources, and assessment methods. Teacher reviews and edits before saving. |

### FR-002
| Field | Value |
|---|---|
| **Title** | Question Paper Generation |
| **Description** | Generate question paper: specify subject, topics, marks distribution, difficulty mix, question types. |
| **Priority** | High |
| **User Roles** | Teacher |
| **Acceptance notes** | AI generates questions with answer keys. Supports MCQ, short answer, long answer. Marks distribution and difficulty mix as specified. Teacher reviews and edits. |

### FR-003
| Field | Value |
|---|---|
| **Title** | Rubric Generation |
| **Description** | Generate grading rubric for subjective questions (criteria + marks allocation). |
| **Priority** | Medium |
| **User Roles** | Teacher |
| **Acceptance notes** | AI generates rubric with criteria, marks allocation per criterion, and description for each level. Teacher reviews and edits. |

### FR-004
| Field | Value |
|---|---|
| **Title** | AI-Assisted Grading |
| **Description** | AI-assisted grading: teacher inputs student answer + question → AI suggests marks + feedback. |
| **Priority** | High |
| **User Roles** | Teacher |
| **Acceptance notes** | AI suggests marks (0 to max), provides feedback (correct/missing concepts). Teacher reviews and accepts/adjusts. Suggestion only — teacher has final say. |

### FR-005
| Field | Value |
|---|---|
| **Title** | Parent Communication Drafting |
| **Description** | Draft parent communication: select student + context → AI generates personalized message. |
| **Priority** | Medium |
| **User Roles** | Teacher |
| **Acceptance notes** | AI generates personalized message based on student performance data. Tone: appreciative, concerned, neutral. Teacher reviews and edits before sending. |

### FR-006
| Field | Value |
|---|---|
| **Title** | Worksheet Generation |
| **Description** | Generate worksheet: practice questions with answer key. |
| **Priority** | Medium |
| **User Roles** | Teacher |
| **Acceptance notes** | AI generates practice questions with answer key. Teacher reviews and edits. Can be assigned as homework or printed. |

### FR-007
| Field | Value |
|---|---|
| **Title** | Editable Outputs |
| **Description** | All AI outputs are editable (teacher reviews before saving/sending). |
| **Priority** | Critical |
| **User Roles** | Teacher |
| **Acceptance notes** | All AI-generated content shown in editable text area. Teacher can modify, add, remove content. Changes tracked (`is_edited = true`). |

### FR-008
| Field | Value |
|---|---|
| **Title** | Usage Tracking & Rate Limiting |
| **Description** | Usage tracking (per teacher per day) with rate limiting. |
| **Priority** | High |
| **User Roles** | System |
| **Acceptance notes** | `teacher_copilot_daily` tracks daily usage. 100 requests per teacher per day (configurable). Returns 429 when exceeded. |

### Non-Functional Requirements

| ID | Requirement |
|---|---|
| NFR-1 | Lesson plan generation: < 10 seconds |
| NFR-2 | Question paper generation: < 15 seconds |
| NFR-3 | Grading assistance: < 5 seconds |
| NFR-4 | Parent communication: < 5 seconds |
| NFR-5 | Worksheet generation: < 10 seconds |
| NFR-6 | All outputs in JSON format for structured rendering |
| NFR-7 | Usage history paginated (20 records per page) |
| NFR-8 | Rate limit enforced server-side |

---

## 4. User Stories

### Teacher
- [ ] Generate a lesson plan for any topic in my subject
- [ ] Generate a question paper with specified marks distribution and difficulty
- [ ] Generate a rubric for subjective questions
- [ ] Get AI assistance for grading subjective answers
- [ ] Draft a personalized parent communication
- [ ] Generate a worksheet with practice questions
- [ ] Edit all AI-generated content before saving/sending
- [ ] View my copilot usage history

### School Admin
- [ ] View copilot usage across teachers (aggregated)
- [ ] Configure rate limits for copilot usage

### System
- [ ] Enforce rate limiting (100 requests/teacher/day)
- [ ] Track all AI-generated content for audit
- [ ] Track token usage for cost monitoring
- [ ] Support multi-provider failover for LLM calls

---

## 5. Business Rules

### BR-001
**Rule:** AI outputs are suggestions, not final.
**Enforcement:** All AI-generated content shown in editable text area. Teacher must review and can modify before saving/sending. `is_edited` flag tracks if teacher modified output.

### BR-002
**Rule:** Rate limit: 100 requests per teacher per day.
**Enforcement:** `teacher_copilot_daily` table tracks daily count. Server checks before processing. Returns 429 when exceeded. Configurable via `TEACHER_COPILOT_DAILY_LIMIT`.

### BR-003
**Rule:** Grading assistance is advisory only.
**Enforcement:** AI suggests marks and feedback. Teacher can accept, adjust, or reject. AI suggestion is not binding. Teacher has final say on marks.

### BR-004
**Rule:** Parent communication is not auto-sent.
**Enforcement:** AI drafts message. Teacher reviews, edits, and manually sends via existing communication channels. AI does not send messages directly.

### BR-005
**Rule:** Question papers are not auto-published.
**Enforcement:** AI generates question paper. Teacher reviews, edits, and manually publishes via existing assessment creation flow.

### BR-006
**Rule:** All usage logged for audit.
**Enforcement:** `teacher_copilot_usage` table stores all AI-generated content with input context, output content, model used, and tokens used. Retained per school's data retention policy.

### BR-007
**Rule:** Copilot is teacher-only.
**Enforcement:** All copilot endpoints require teacher role. Parents and students cannot access copilot features.

### BR-008
**Rule:** AI uses curriculum context.
**Enforcement:** Lesson plans and question papers use curriculum units from `CurriculumUnitsTable`. AI knows the school's curriculum, grade level, and subject syllabus.

---

## 6. Database Design

### 6.1 Entity Relationship Summary

Two new tables: `teacher_copilot_usage` (AI-generated content history) and `teacher_copilot_daily` (rate limiting).

### 6.2 New Tables

#### `teacher_copilot_usage` table

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
```

#### `teacher_copilot_daily` table

```sql
CREATE TABLE teacher_copilot_daily (
    teacher_id      UUID NOT NULL,
    date            DATE NOT NULL,
    usage_count     INTEGER NOT NULL DEFAULT 0,
    tokens_used     INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (teacher_id, date)
);
```

### 6.3 Modified Tables

N/A — no existing tables modified.

### 6.4 Indexes

- `teacher_copilot_usage(teacher_id, created_at DESC)` — usage history lookup
- `teacher_copilot_daily(teacher_id, date)` — PRIMARY KEY, for rate limiting

### 6.5 Constraints

- `teacher_copilot_usage.school_id` — NOT NULL
- `teacher_copilot_usage.teacher_id` — NOT NULL
- `teacher_copilot_usage.usage_type` — NOT NULL, VARCHAR(32)
- `teacher_copilot_usage.output_content` — NOT NULL (TEXT, JSON)
- `teacher_copilot_daily(teacher_id, date)` — PRIMARY KEY (composite)

### 6.6 Foreign Keys

- `teacher_copilot_usage.school_id` → `schools.id` (implicit)
- `teacher_copilot_usage.teacher_id` → `users.id` (implicit, teacher role)
- `teacher_copilot_daily.teacher_id` → `users.id` (implicit, teacher role)

### 6.7 Soft Delete Strategy

N/A — usage records retained for audit. No soft delete. Hard delete only per data retention policy.

### 6.8 Audit Fields

- `teacher_copilot_usage.created_at` — when content generated
- `teacher_copilot_usage.is_edited` — whether teacher modified the output
- `teacher_copilot_usage.model_used` — which LLM model was used
- `teacher_copilot_usage.tokens_used` — token consumption

### 6.9 Migration Notes

Migration: `docs/db/migration_077_teacher_copilot.sql`
- CREATE 2 tables: `teacher_copilot_usage`, `teacher_copilot_daily`
- No data migration (new feature)
- Indexes created in same migration

### 6.10 Exposed Mappings

```kotlin
object TeacherCopilotUsageTable : UUIDTable("teacher_copilot_usage", "id") {
    val schoolId      = uuid("school_id")
    val teacherId     = uuid("teacher_id")
    val usageType     = varchar("usage_type", 32)
    val subject       = text("subject").nullable()
    val inputContext  = text("input_context").nullable()  // JSON
    val outputContent = text("output_content")             // JSON
    val isEdited      = bool("is_edited").default(false)
    val modelUsed     = varchar("model_used", 64).nullable()
    val tokensUsed    = integer("tokens_used").nullable()
    val createdAt     = timestamp("created_at")
}

object TeacherCopilotDailyTable : Table("teacher_copilot_daily") {
    val teacherId   = uuid("teacher_id")
    val date        = date("date")
    val usageCount  = integer("usage_count").default(0)
    val tokensUsed  = integer("tokens_used").default(0)

    override val primaryKey = PrimaryKey(teacherId, date)
}
```

Register both in `DatabaseFactory.allTables`.

### 6.11 Seed Data

N/A — copilot data created by usage. No seed data needed.

---

## 7. State Machines

### Content Generation State Machine

```
idle ──teacher_requests──> generating ──ai_success──> draft ──teacher_edits──> edited ──teacher_saves──> saved
  │                            │                       │
  │                            │                       │──teacher_discards──> idle
  │                            │
  │                            │──ai_error──> error
  │                            │
  │                            │──rate_limit──> blocked
  │
  └──rate_limit_exceeded──> blocked
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `idle` | Teacher requests content | `generating` | Rate limit not exceeded |
| `idle` | Rate limit exceeded | `blocked` | Daily limit reached |
| `generating` | AI returns content | `draft` | Successful LLM call |
| `generating` | AI error | `error` | LLM call fails |
| `generating` | Rate limit exceeded | `blocked` | Daily limit reached during generation |
| `draft` | Teacher edits content | `edited` | Teacher modifies output |
| `draft` | Teacher saves without edits | `saved` | Teacher accepts as-is |
| `draft` | Teacher discards | `idle` | Teacher rejects output |
| `edited` | Teacher saves | `saved` | Teacher saves edited content |
| `edited` | Teacher discards | `idle` | Teacher rejects output |
| `error` | Teacher retries | `generating` | Rate limit not exceeded |
| `blocked` | Next day | `idle` | Daily reset |

### Grading Assistance State Machine

```
idle ──teacher_inputs──> analyzing ──ai_suggests──> suggestion ──teacher_accepts──> applied
  │                          │                       │
  │                          │                       │──teacher_adjusts──> adjusted ──teacher_confirms──> applied
  │                          │                       │
  │                          │                       │──teacher_rejects──> idle
  │                          │
  │                          │──ai_error──> error
  │
  └──rate_limit──> blocked
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `idle` | Teacher inputs answer | `analyzing` | Rate limit not exceeded |
| `analyzing` | AI returns suggestion | `suggestion` | Successful LLM call |
| `analyzing` | AI error | `error` | LLM call fails |
| `suggestion` | Teacher accepts | `applied` | Teacher agrees with suggested marks |
| `suggestion` | Teacher adjusts marks | `adjusted` | Teacher modifies suggested marks |
| `suggestion` | Teacher rejects | `idle` | Teacher ignores suggestion |
| `adjusted` | Teacher confirms | `applied` | Teacher confirms adjusted marks |
| `error` | Teacher retries | `analyzing` | Rate limit not exceeded |
| `blocked` | Next day | `idle` | Daily reset |

### Daily Usage State Machine

```
new_day (0 requests) ──teacher_requests──> counting ──more_requests──> counting
  │                                         │
  └──midnight_reset──>                      │──limit_reached (100)──> blocked
  new_day                                  blocked
                                           │
                                           └──midnight_reset──> new_day
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `new_day` | Teacher requests | `counting` | `usage_count = 1` |
| `counting` | Teacher requests | `counting` | `usage_count < 100` |
| `counting` | `usage_count = 100` | `blocked` | Daily limit reached |
| `blocked` | Teacher requests | `blocked` | Return 429 |
| `blocked` | Midnight (new day) | `new_day` | Daily reset |
| `counting` | Midnight (new day) | `new_day` | Daily reset |

---

## 8. Backend Architecture

### 8.1 Component Overview

`TeacherCopilotService` handles all 6 content generation functions (lesson plan, question paper, rubric, grading assist, parent comm, worksheet). Uses existing `AiService` from `AI_INFRASTRUCTURE_SPEC.md` for LLM calls with multi-provider failover. All outputs stored in `teacher_copilot_usage` for audit.

### 8.2 Design Principles

1. **AI suggests, teacher decides** — all outputs are editable, teacher has final say
2. **Curriculum-integrated** — AI uses school's curriculum, grade, and subject context
3. **Rate-limited** — 100 requests/day per teacher, enforced server-side
4. **Audit-tracked** — all AI-generated content logged with input/output/tokens
5. **Multi-provider failover** — uses existing AI infrastructure for reliability
6. **Structured output** — AI returns JSON for structured rendering in UI

### 8.3 Core Types

#### TeacherCopilotService

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

### 8.4 Repositories

- `CopilotUsageRepository` — CRUD for `teacher_copilot_usage`
- `CopilotDailyRepository` — CRUD for `teacher_copilot_daily` (with upsert for daily count)

### 8.5 Mappers

- `CopilotUsageMapper` — maps `teacher_copilot_usage` rows to `CopilotUsageDto`

### 8.6 Permission Checks

- All endpoints: teacher role + JWT auth via `requireAuth()`
- Teacher can only view own usage history
- School admin can view aggregated usage across teachers

### 8.7 Background Jobs

N/A — no background jobs. Copilot is on-demand content generation.

### 8.8 Domain Events

- `CopilotContentGenerated` — emitted when AI generates content (includes type, tokens)
- `CopilotContentEdited` — emitted when teacher edits AI output
- `CopilotContentSaved` — emitted when teacher saves AI output
- `CopilotRateLimitExceeded` — emitted when teacher hits daily limit

### 8.9 Caching

- No caching — each request is unique (different inputs → different outputs)
- Usage history: not cached (paginated DB query)

### 8.10 Transactions

- Content generation: single transaction (insert usage record, update daily count)
- Content save: single transaction (update usage record with `is_edited` flag)

### 8.11 Rate Limiting

- 100 requests per day per teacher (configurable via `TEACHER_COPILOT_DAILY_LIMIT`)
- Enforced server-side via `teacher_copilot_daily` table
- Returns 429 with message "Daily limit reached. Try again tomorrow."

### 8.12 Configuration

- `TEACHER_COPILOT_ENABLED` — default `true`; enable/disable feature
- `TEACHER_COPILOT_DAILY_LIMIT` — default `100`; max requests per teacher per day
- `TEACHER_COPILOT_MAX_TOKENS_PER_REQUEST` — default `4000`; max tokens per AI response
- `TEACHER_COPILOT_MODEL` — default `copilot_v1`; AI model identifier
- `TEACHER_COPILOT_HISTORY_PAGE_SIZE` — default `20`; records per page

### 8.13 Prompt Templates

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

**Lesson plan generation:**
```
System: You are an expert lesson planner for Indian schools.
Generate a lesson plan for:
- Subject: {{subject}}, Grade: {{grade}}
- Topic: {{topic}}
- Duration: {{duration}} minutes

Include: learning objectives, teaching activities, resources needed, assessment methods.
Output JSON: {"objectives": [...], "activities": [...], "resources": [...], "assessment": "..."}
```

**Parent communication:**
```
System: You are a teacher writing to a parent about their child.
- Student: {{student_name}}
- Context: {{context}} (e.g., "performance declining in Math", "excellent participation in Science")
- Tone: {{tone}} (appreciative, concerned, neutral)

Write a personalized message to the parent. Keep it concise (100-150 words).
Output: plain text message.
```

---

## 9. API Contracts

### 9.1 Teacher Endpoints

```
POST /api/v1/teacher/copilot/lesson-plan
  Body: { topic: "Fractions", subject: "Math", grade: "6", duration: 45 }
  → 200: { lesson_plan: LessonPlanDto, tokens_used: 1200 }
  → 429: Daily limit reached

POST /api/v1/teacher/copilot/question-paper
  Body: { subject: "Math", grade: "6", total_marks: 100, duration: 180, types: ["mcq", "short_answer", "long_answer"], topics: [{ topic: "Fractions", marks: 20 }, { topic: "Decimals", marks: 30 }], difficulty_mix: { easy: 30, medium: 50, hard: 20 } }
  → 200: { question_paper: QuestionPaperDto, tokens_used: 3500 }

POST /api/v1/teacher/copilot/rubric
  Body: { question: "Explain the water cycle", max_marks: 10 }
  → 200: { rubric: RubricDto, tokens_used: 800 }

POST /api/v1/teacher/copilot/grading-assist
  Body: { question: "Explain photosynthesis", model_answer: "...", student_answer: "...", max_marks: 10 }
  → 200: { suggestion: GradingSuggestion, tokens_used: 600 }

POST /api/v1/teacher/copilot/parent-comm
  Body: { student_id: "uuid", context: "performance declining in Math", tone: "concerned" }
  → 200: { message: "Dear Parent, I wanted to discuss...", tokens_used: 400 }

POST /api/v1/teacher/copilot/worksheet
  Body: { subject: "Math", topics: ["Fractions", "Decimals"], question_count: 10, difficulty: "medium" }
  → 200: { worksheet: WorksheetDto, tokens_used: 2000 }

GET /api/v1/teacher/copilot/history?page={n}
  → 200: { records: [CopilotUsageDto], total: Int, page: Int, pageSize: Int }
```

### 9.2 DTO Models

All `@Serializable`, wrapped in `ApiResponse<T>` pattern.

```kotlin
@Serializable data class LessonPlanDto(
    val objectives: List<String>,
    val activities: List<String>,
    val resources: List<String>,
    val assessment: String,
)

@Serializable data class QuestionPaperRequest(
    val subject: String,
    val grade: String,
    val totalMarks: Int,
    val duration: Int,
    val types: List<String>,
    val topics: List<TopicMarks>,
    val difficultyMix: DifficultyMix,
)

@Serializable data class TopicMarks(
    val topic: String,
    val marks: Int,
)

@Serializable data class DifficultyMix(
    val easy: Int,    // percentage
    val medium: Int,  // percentage
    val hard: Int,    // percentage
)

@Serializable data class QuestionPaperDto(
    val questions: List<QuestionItem>,
)

@Serializable data class QuestionItem(
    val question: String,
    val type: String,        // "mcq" | "short_answer" | "long_answer"
    val marks: Int,
    val topic: String,
    val answerKey: String,
    val options: List<String>?,  // for MCQ
)

@Serializable data class RubricDto(
    val criteria: List<RubricCriterion>,
)

@Serializable data class RubricCriterion(
    val criterion: String,
    val maxMarks: Int,
    val levels: List<RubricLevel>,
)

@Serializable data class RubricLevel(
    val level: String,     // "excellent" | "good" | "satisfactory" | "needs_improvement"
    val marks: Int,
    val description: String,
)

@Serializable data class GradingSuggestion(
    val suggestedMarks: Int,
    val feedback: String,
    val correctConcepts: List<String>,
    val missingConcepts: List<String>,
)

@Serializable data class WorksheetDto(
    val questions: List<WorksheetQuestion>,
)

@Serializable data class WorksheetQuestion(
    val question: String,
    val answer: String,
    val difficulty: String,
    val topic: String,
)

@Serializable data class CopilotUsageDto(
    val id: String,
    val usageType: String,
    val subject: String?,
    val isEdited: Boolean,
    val createdAt: String,
    val tokensUsed: Int?,
)

enum class CopilotUsageType { LESSON_PLAN, QUESTION_PAPER, RUBRIC, GRADING_ASSIST, PARENT_COMM, WORKSHEET }
enum class CommunicationTone { APPRECIATIVE, CONCERNED, NEUTRAL }
```

---

## 10. Frontend Architecture

### 10.1 Screens

| Screen | Platform | Role | Description |
|---|---|---|---|
| `CopilotScreen` | Compose | Teacher | Copilot dashboard with tool selection |
| `CopilotLessonPlanScreen` | Compose | Teacher | Lesson plan generator with editable output |
| `CopilotQuestionPaperScreen` | Compose | Teacher | Question paper generator with editable output |
| `CopilotRubricScreen` | Compose | Teacher | Rubric generator with editable output |
| `CopilotGradingAssistScreen` | Compose | Teacher | Grading assistance with suggestion display |
| `CopilotParentCommScreen` | Compose | Teacher | Parent communication drafter with editable output |
| `CopilotWorksheetScreen` | Compose | Teacher | Worksheet generator with editable output |
| `CopilotHistoryScreen` | Compose | Teacher | Usage history (paginated) |

### 10.2 Navigation

- Teacher: Admin tab → Copilot → Dashboard → [Tool Screen]
- Teacher: Copilot → History → Usage list

### 10.3 UX Flows

#### Teacher: Generate Question Paper
1. Teacher opens Copilot → Question Paper
2. Selects subject, grade, total marks, duration
3. Adds topics with marks distribution
4. Selects question types (MCQ, short answer, long answer)
5. Sets difficulty mix (easy/medium/hard percentages)
6. Clicks "Generate"
7. AI generates question paper with answer keys
8. Teacher reviews each question in editable view
9. Teacher edits/adds/removes questions
10. Teacher saves (or exports to assessment creation flow)

#### Teacher: AI-Assisted Grading
1. Teacher opens Copilot → Grading Assist
2. Inputs question, model answer, student answer, max marks
3. Clicks "Get Suggestion"
4. AI suggests marks + feedback + correct/missing concepts
5. Teacher reviews suggestion
6. Teacher accepts, adjusts, or rejects
7. If accepted/adjusted, marks applied to student's assessment

#### Teacher: Draft Parent Communication
1. Teacher opens Copilot → Parent Communication
2. Selects student from class
3. Selects context (performance declining, excellent participation, etc.)
4. Selects tone (appreciative, concerned, neutral)
5. Clicks "Draft"
6. AI generates personalized message
7. Teacher reviews and edits message
8. Teacher copies message or sends via existing communication channel

### 10.4 State Management

```kotlin
data class CopilotState(
    val selectedTool: CopilotUsageType?,
    val isGenerating: Boolean,
    val generatedContent: String?,
    val isEdited: Boolean,
    val error: String?,
    val dailyUsageUsed: Int,
    val dailyUsageLimit: Int,
)

data class QuestionPaperState(
    val subject: String,
    val grade: String,
    val totalMarks: Int,
    val duration: Int,
    val selectedTypes: Set<String>,
    val topics: List<TopicMarks>,
    val difficultyMix: DifficultyMix,
    val generatedPaper: QuestionPaperDto?,
    val isGenerating: Boolean,
)

data class GradingAssistState(
    val question: String,
    val modelAnswer: String,
    val studentAnswer: String,
    val maxMarks: Int,
    val suggestion: GradingSuggestion?,
    val adjustedMarks: Int?,
    val isAnalyzing: Boolean,
)

data class CopilotHistoryState(
    val records: List<CopilotUsageDto>,
    val currentPage: Int,
    val totalPages: Int,
    val isLoading: Boolean,
)
```

### 10.5 Offline Support

- Copilot features: not available offline (requires AI)
- Usage history: cached locally (last fetched)
- Generated content: not cached (unique per request)

### 10.6 Loading States

- Lesson plan: "Generating lesson plan..."
- Question paper: "Generating question paper..."
- Grading assist: "Analyzing answer..."
- Parent comm: "Drafting message..."
- Worksheet: "Generating worksheet..."

### 10.7 Error Handling (UI)

- Rate limit: "Daily limit of 100 requests reached. Try again tomorrow."
- AI error: "Copilot is having trouble right now. Please try again."
- Network error: "Connection issue. Please check your internet."
- Invalid input: "Please fill all required fields."

### 10.8 Component Integration Guidelines

| Rule | Description |
|---|---|
| **R1** | Copilot dashboard shows 6 tool cards (lesson plan, question paper, rubric, grading, parent comm, worksheet) |
| **R2** | Each tool screen has input form + generate button + editable output area |
| **R3** | Output rendered as structured UI (not raw JSON) — questions as cards, rubric as table, lesson plan as sections |
| **R4** | Edit mode: tap any field to edit, changes tracked |
| **R5** | "Save" button stores content; "Discard" returns to dashboard |
| **R6** | Daily usage counter shown ("23/100 requests used today") |
| **R7** | Grading assist shows suggestion with accept/adjust/reject buttons |
| **R8** | Parent comm shows editable text area with character count |
| **R9** | Question paper can be exported to assessment creation flow |
| **R10** | History screen shows paginated list with type, subject, date, edited status |

---

## 11. Shared Module Changes (KMP)

### 11.1 DTOs

All DTOs defined in section 9.2, placed in `shared/.../ai/copilot/domain/model/CopilotModels.kt`.

### 11.2 Domain Models

```kotlin
data class CopilotUsage(
    val id: String,
    val teacherId: String,
    val usageType: CopilotUsageType,
    val subject: String?,
    val isEdited: Boolean,
    val createdAt: Instant,
    val tokensUsed: Int?,
)

data class LessonPlan(
    val objectives: List<String>,
    val activities: List<String>,
    val resources: List<String>,
    val assessment: String,
)

data class QuestionPaper(
    val questions: List<QuestionItem>,
)

data class GradingSuggestion(
    val suggestedMarks: Int,
    val feedback: String,
    val correctConcepts: List<String>,
    val missingConcepts: List<String>,
)
```

### 11.3 Repository Interfaces

```kotlin
interface TeacherCopilotRepository {
    suspend fun generateLessonPlan(token: String, request: LessonPlanRequest): NetworkResult<LessonPlanResponse>
    suspend fun generateQuestionPaper(token: String, request: QuestionPaperRequest): NetworkResult<QuestionPaperResponse>
    suspend fun generateRubric(token: String, request: RubricRequest): NetworkResult<RubricResponse>
    suspend fun assistGrading(token: String, request: GradingRequest): NetworkResult<GradingResponse>
    suspend fun draftParentComm(token: String, request: ParentCommRequest): NetworkResult<ParentCommResponse>
    suspend fun generateWorksheet(token: String, request: WorksheetRequest): NetworkResult<WorksheetResponse>
    suspend fun getHistory(token: String, page: Int): NetworkResult<PaginatedResult<CopilotUsageDto>>
}
```

### 11.4 UseCases

- `GenerateLessonPlanUseCase`
- `GenerateQuestionPaperUseCase`
- `GenerateRubricUseCase`
- `AssistGradingUseCase`
- `DraftParentCommunicationUseCase`
- `GenerateWorksheetUseCase`
- `GetCopilotHistoryUseCase`

### 11.5 Validation

- `subject`: non-empty, max 100 characters
- `grade`: non-empty
- `topic`: non-empty, max 200 characters
- `total_marks`: > 0
- `duration`: > 0
- `max_marks`: > 0
- `question_count`: > 0, max 50
- `difficulty`: one of `easy`, `medium`, `hard`
- `tone`: one of `appreciative`, `concerned`, `neutral`
- `page`: ≥ 1

### 11.6 Serialization

Standard Kotlinx serialization. Enums serialized as lowercase strings.

### 11.7 Network APIs

Ktor `@Resource` route definitions in `TeacherCopilotApi.kt`:
- POST `/api/v1/teacher/copilot/lesson-plan`
- POST `/api/v1/teacher/copilot/question-paper`
- POST `/api/v1/teacher/copilot/rubric`
- POST `/api/v1/teacher/copilot/grading-assist`
- POST `/api/v1/teacher/copilot/parent-comm`
- POST `/api/v1/teacher/copilot/worksheet`
- GET `/api/v1/teacher/copilot/history`

### 11.8 Database Models (Local Cache)

- Usage history cached in local DB (last fetched, paginated)
- No content caching (unique per request)

---

## 12. Permissions Matrix

| Action | Super Admin | School Admin | Teacher | Parent |
|---|---|---|---|---|
| Generate lesson plan | N/A | N/A | ✅ | ❌ |
| Generate question paper | N/A | N/A | ✅ | ❌ |
| Generate rubric | N/A | N/A | ✅ | ❌ |
| Use grading assist | N/A | N/A | ✅ | ❌ |
| Draft parent communication | N/A | N/A | ✅ | ❌ |
| Generate worksheet | N/A | N/A | ✅ | ❌ |
| View own usage history | N/A | N/A | ✅ | ❌ |
| View all teachers' usage | ✅ | ✅ | ❌ | ❌ |
| Configure rate limit | ✅ | ✅ | ❌ | ❌ |

---

## 13. Notifications

N/A — copilot is on-demand. No notifications triggered by copilot usage.

---

## 14. Background Jobs

N/A — no background jobs. Copilot is on-demand content generation.

---

## 15. Integrations

### Internal Integrations

| System | Integration Point | Direction | Protocol | Error Handling |
|---|---|---|---|---|
| `AiService` (`AI_INFRASTRUCTURE_SPEC.md`) | LLM completion | Call | Direct call | Multi-provider failover |
| `CurriculumUnitsTable` | Curriculum context | Read | Direct DB | Return empty if no curriculum |
| `AssessmentsTable` | Assessment context | Read | Direct DB | Optional context |
| `AssessmentMarksTable` | Student marks | Read | Direct DB | Optional context for grading |
| `StudentsTable` | Student info for parent comm | Read | Direct DB | Return error if not found |
| `LESSON_PLANNING_SPEC.md` | Lesson plan save | Write | Direct call | Log on failure |
| `ONLINE_ASSIGNMENTS_SPEC.md` | Question paper export | Write | Direct call | Log on failure |

### External Integrations

| System | Purpose | Direction | Protocol | Authentication | Error Handling |
|---|---|---|---|---|---|
| LLM Provider (via `AiService`) | AI content generation | Outbound | HTTP API | API key (existing) | Multi-provider failover |

### Integration Patterns

- **AiService:** `aiService.complete(null, null, "copilot", "copilot_v1", context)` — uses existing AI infrastructure
- **Lesson plan save:** Generated lesson plan can be saved to lesson plan system via `LessonPlanService`
- **Question paper export:** Generated question paper can be exported to assessment creation flow
- **Grading integration:** Grading suggestion can be applied to `AssessmentMarksTable` (teacher confirms)
- **Parent comm:** Drafted message copied to clipboard or sent via existing communication channel

---

## 16. Security

### Authentication

- All copilot APIs: JWT auth via `requireAuth()`, teacher role

### Authorization

- Teacher can only access own copilot usage and history
- School admin can view aggregated usage across teachers
- No parent or student access to copilot

### Data Protection

- AI-generated content: stored in DB, accessible to teacher (own) and school admin (aggregated)
- Student answers (for grading assist): same sensitivity as marks data
- Parent communication drafts: contain student performance info — teacher-only access
- No PII sent to LLM beyond what's necessary (student name for parent comm, answer text for grading)

### Input Validation

- `subject`: non-empty, max 100 characters
- `grade`: non-empty
- `topic`: non-empty, max 200 characters
- `total_marks`: > 0, max 1000
- `duration`: > 0, max 300 (minutes)
- `max_marks`: > 0, max 100
- `question_count`: > 0, max 50
- `difficulty`: one of `easy`, `medium`, `hard`
- `tone`: one of `appreciative`, `concerned`, `neutral`
- `student_answer`: max 5000 characters
- `model_answer`: max 5000 characters

### Rate Limiting

- 100 requests per day per teacher (server-side enforced)
- Returns 429 when exceeded

### Audit Logging

- Content generated: teacher ID, usage type, subject, tokens used, model
- Content edited: teacher ID, usage ID, edit timestamp
- Content saved: teacher ID, usage ID, save timestamp
- Rate limit exceeded: teacher ID, daily count

### PII Handling

- Student name sent to LLM for parent communication (necessary for personalization)
- Student answers sent to LLM for grading assistance (necessary for grading)
- No other PII sent to LLM
- All content stored in DB for audit

### Multi-tenant Isolation

- `teacher_copilot_usage.school_id` — school-scoped
- `teacher_copilot_daily.teacher_id` — teacher-scoped
- All queries filtered by `school_id` and `teacher_id`
- No cross-school copilot access

---

## 17. Performance & Scalability

### Expected Scale

- 1 teacher generates 10-50 content pieces per day
- 50 teachers per school → 500-2,500 requests per day per school
- 10 schools → 5,000-25,000 requests per day
- Each request: 1 LLM call (~2,000-4,000 tokens)

### Query Optimization

- Usage history: `idx_copilot_usage_teacher(teacher_id, created_at DESC)` — paginated
- Daily usage: `teacher_copilot_daily(teacher_id, date)` — PRIMARY KEY, O(1) lookup

### Indexing Strategy

- `teacher_copilot_usage(teacher_id, created_at DESC)` — usage history
- `teacher_copilot_daily(teacher_id, date)` — PRIMARY KEY, rate limiting

### Caching Strategy

- No caching — each request is unique
- Usage history: not cached (paginated DB query)

### Pagination

- Usage history: 20 records per page

### Connection Pooling

Uses existing HikariCP connection pool. No additional pooling needed.

### Async Processing

- AI calls: synchronous (with timeout)
- Usage tracking: synchronous (within transaction)
- Content save: synchronous

### Scalability Concerns

- LLM calls: 25,000/day → ~0.3 QPS average, ~3 QPS peak. Manageable.
- Token usage: 25,000 × 3,000 tokens = 75M tokens/day. Monitor costs.
- DB storage: 25,000 records/day × ~3KB = ~75MB/day. Manageable.

---

## 18. Edge Cases

| # | Scenario | Expected Behavior |
|---|---|---|
| EC-1 | AI generates invalid question paper format | Retry with stricter prompt. If still fails, return error. |
| EC-2 | AI suggests incorrect marks for grading | Teacher adjusts. AI suggestion is advisory only. |
| EC-3 | Rate limit exceeded | Return 429. Teacher can try next day. |
| EC-4 | LLM provider unavailable | Multi-provider failover. If all fail, return 503. |
| EC-5 | No curriculum data for subject | AI generates with reduced context. May be less accurate. |
| EC-6 | Teacher generates question paper with impossible marks distribution | Validate input: total topic marks must equal total marks. Return 400 if mismatch. |
| EC-7 | Teacher edits AI output extensively | `is_edited = true`. Edited content saved. |
| EC-8 | Teacher discards AI output | Content not saved. Usage record still logged (for analytics). |
| EC-9 | Parent communication for student with no marks data | AI generates generic message. Teacher adds specifics. |
| EC-10 | Worksheet generation with 50 questions | May take longer (15-20 seconds). Show progress indicator. |
| EC-11 | Concurrent requests from same teacher | Allow (different content). Rate limit checks total daily count. |
| EC-12 | AI generates inappropriate content | Log incident. Notify school admin. Teacher can discard. |
| EC-13 | Lesson plan for non-curriculum topic | AI generates plan. May not align with syllabus. Teacher reviews. |
| EC-14 | Grading assist for objective question (MCQ) | Return message "Grading assist is for subjective questions only." |
| EC-15 | Daily usage row doesn't exist for today | Create new row with count = 1. |
| EC-16 | Teacher saves content without editing | `is_edited = false`. Content saved as-is. |
| EC-17 | Question paper with single topic | Valid. All marks allocated to one topic. |
| EC-18 | Difficulty mix doesn't sum to 100 | Validate input. Return 400 "Difficulty mix must sum to 100." |

---

## 19. Error Handling

### Error Response Format

Standard `ApiResponse` error format.

### Error Codes

| Code | HTTP Status | Description | User Message |
|---|---|---|---|
| `RATE_LIMIT_EXCEEDED` | 429 | Daily request limit reached | "Daily limit of {limit} requests reached. Try again tomorrow." |
| `AI_SERVICE_UNAVAILABLE` | 503 | All LLM providers unavailable | "Copilot is unavailable right now. Please try again." |
| `INVALID_INPUT` | 400 | Input validation failed | "Invalid input: {details}" |
| `MARKS_MISMATCH` | 400 | Topic marks don't equal total marks | "Topic marks must sum to total marks." |
| `DIFFICULTY_MISMATCH` | 400 | Difficulty mix doesn't sum to 100 | "Difficulty mix must sum to 100." |
| `STUDENT_NOT_FOUND` | 404 | Student ID not found | "Student not found." |
| `SUBJECT_REQUIRED` | 400 | Subject not provided | "Subject is required." |

### Error Handling Strategy

- **AI errors:** Multi-provider failover. If all fail, return 503. No charge to daily limit.
- **Rate limit:** Return 429. No charge for rejected request.
- **Validation errors:** Return 400 with specific message.
- **AI invalid output:** Retry with stricter prompt. If still fails, return 503.

### Retry Strategy

- AI service: multi-provider failover (existing infrastructure)
- Client: retry on 503 (3 attempts with 5-second intervals)
- Invalid JSON: retry with stricter prompt (1 retry)

### Fallback Behavior

- AI unavailable: "Copilot is unavailable. Please try again later."
- No curriculum data: AI generates with reduced context
- LLM timeout: return 503, no charge to daily limit

---

## 20. Analytics & Reporting

### Analytics Dashboard Data

| Metric | Source | Derivation |
|---|---|---|
| Total copilot requests per day | `teacher_copilot_usage` | Count by date |
| Requests by type | `teacher_copilot_usage` | Count by `usage_type` |
| Tokens used per day | `teacher_copilot_daily` | Sum of `tokens_used` |
| Average tokens per request | `teacher_copilot_usage` | Avg `tokens_used` |
| Edit rate | `teacher_copilot_usage` | % where `is_edited = true` |
| Most used tool | `teacher_copilot_usage` | Count by `usage_type`, sort desc |
| Active teachers per day | `teacher_copilot_daily` | Count distinct `teacher_id` |
| Rate limit hits per day | Audit logs | Count of 429 responses |

### Export Capabilities

- Usage export (CSV) — teacher, type, subject, date, tokens, edited

### Report Types

| Report | Format | Frequency | Recipient |
|---|---|---|---|
| Copilot usage | JSON (API) | On-demand | School Admin |
| Token usage | JSON (API) | Weekly | Dev Team |
| Cost analysis | JSON (API) | Monthly | Dev Team |
| Edit rate analysis | JSON (API) | Monthly | Product Team |

---

## 21. Testing Strategy

### Unit Tests

- `TeacherCopilotService.generateLessonPlan()` — prompt building, JSON parsing, output validation
- `TeacherCopilotService.generateQuestionPaper()` — marks distribution validation, JSON parsing
- `TeacherCopilotService.generateRubric()` — rubric structure validation
- `TeacherCopilotService.assistGrading()` — suggestion parsing, marks range validation
- `TeacherCopilotService.draftParentCommunication()` — tone validation, message generation
- `TeacherCopilotService.generateWorksheet()` — question count validation, JSON parsing
- Rate limiting — daily count check, 429 response, daily reset
- Input validation — marks mismatch, difficulty mismatch, empty fields

### Integration Tests

- Full lesson plan flow: generate → edit → save → verify stored
- Full question paper flow: generate → edit → export to assessment
- Full grading assist flow: input → suggestion → accept → verify marks applied
- Rate limit: 100 requests → 101st returns 429
- Multi-provider failover: primary LLM fails → secondary used

### E2E Tests

- Teacher generates question paper → edits → saves → exports to assessment
- Teacher uses grading assist → adjusts marks → applies to student
- Teacher drafts parent communication → edits → copies/sends

### Performance Tests

- Lesson plan generation: < 10 seconds
- Question paper generation: < 15 seconds
- Grading assistance: < 5 seconds
- Parent communication: < 5 seconds
- Worksheet generation: < 10 seconds

### Test Data

- 3 subjects with curriculum units
- 10 sample students with marks
- Mock LLM (returns structured JSON for each tool type)
- Sample question papers, rubrics, lesson plans

### Test Environment

- Test database with copilot tables
- Mock LLM provider (returns controlled responses)
- Test JWT tokens for teacher and admin roles

---

## 22. Acceptance Criteria

- [ ] Lesson plans generated with objectives, activities, resources
- [ ] Question papers with correct marks distribution and answer keys
- [ ] Rubrics with criteria and marks allocation
- [ ] Grading assistance suggests marks + feedback
- [ ] Parent communication drafted with appropriate tone
- [ ] Worksheets with practice questions + answer key
- [ ] All outputs editable before use
- [ ] Rate limiting enforced (100 requests/day)
- [ ] Usage history available (paginated)
- [ ] Daily usage counter visible to teacher
- [ ] Multi-provider failover works
- [ ] Content save tracks edit status

---

## 23. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration `migration_077_teacher_copilot.sql`, Exposed tables, register in `DatabaseFactory` |
| 2 | 3 days | `TeacherCopilotService` (all 6 functions) |
| 3 | 2 days | Prompt templates + seed |
| 4 | 1 day | API endpoints + rate limiting |
| 5 | 4 days | Client UI: `CopilotScreen` (dashboard), `CopilotLessonPlanScreen`, `CopilotQuestionPaperScreen`, `CopilotRubricScreen`, `CopilotGradingAssistScreen`, `CopilotParentCommScreen`, `CopilotWorksheetScreen`, `CopilotHistoryScreen` |
| 6 | 2 days | Tests: unit, integration, E2E |

### Pre-Implementation Checklist

- [ ] Verify `AI_INFRASTRUCTURE_SPEC.md` is implemented and `AiService` available
- [ ] Verify `LESSON_PLANNING_SPEC.md` is implemented
- [ ] Verify `ONLINE_ASSIGNMENTS_SPEC.md` is implemented
- [ ] Verify `CurriculumUnitsTable` has test data
- [ ] Verify LLM provider supports JSON output
- [ ] Verify existing communication channels for parent comm

---

## 24. File-Level Impact Analysis

### Server

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | `TeacherCopilotUsageTable`, `TeacherCopilotDailyTable` |
| `server/.../db/DatabaseFactory.kt` | Modify | Register 2 copilot tables in `allTables` |
| `server/.../feature/ai/copilot/TeacherCopilotService.kt` | **New** | Core service (6 content generation functions) |
| `server/.../feature/ai/copilot/TeacherCopilotRouting.kt` | **New** | API endpoints |
| `docs/db/migration_077_teacher_copilot.sql` | **New** | DDL: 2 copilot tables |

### Shared (KMP)

| File | Change Type | Description |
|---|---|---|
| `shared/.../ai/copilot/domain/model/CopilotModels.kt` | **New** | DTOs, domain models, enums |
| `shared/.../ai/copilot/domain/repository/TeacherCopilotRepository.kt` | **New** | Repository interface |
| `shared/.../ai/copilot/data/remote/TeacherCopilotApi.kt` | **New** | HTTP API definitions |

### Client (Compose)

| File | Change Type | Description |
|---|---|---|
| `composeApp/.../ui/v2/screens/teacher/CopilotScreen.kt` | **New** | Copilot dashboard (tool selection) |
| `composeApp/.../ui/v2/screens/teacher/CopilotLessonPlanScreen.kt` | **New** | Lesson plan generator |
| `composeApp/.../ui/v2/screens/teacher/CopilotQuestionPaperScreen.kt` | **New** | Question paper generator |
| `composeApp/.../ui/v2/screens/teacher/CopilotRubricScreen.kt` | **New** | Rubric generator |
| `composeApp/.../ui/v2/screens/teacher/CopilotGradingAssistScreen.kt` | **New** | Grading assistance |
| `composeApp/.../ui/v2/screens/teacher/CopilotParentCommScreen.kt` | **New** | Parent communication drafter |
| `composeApp/.../ui/v2/screens/teacher/CopilotWorksheetScreen.kt` | **New** | Worksheet generator |
| `composeApp/.../ui/v2/screens/teacher/CopilotHistoryScreen.kt` | **New** | Usage history |

---

## 25. Future Enhancements

| # | Enhancement | Priority | Effort | Notes |
|---|---|---|---|---|
| F-1 | Multi-language content generation | Medium | M | Generate content in school's medium of instruction |
| F-2 | Question bank from generated papers | Medium | M | Save generated questions to reusable bank |
| F-3 | Collaborative lesson planning | Low | L | Multiple teachers co-edit lesson plans |
| F-4 | AI-powered curriculum mapping | Medium | M | Map generated content to curriculum standards |
| F-5 | Bulk grading assistance | Medium | M | Grade multiple student answers at once |
| F-6 | Template library | Low | S | Save and reuse prompt templates |
| F-7 | Voice-based input | Low | L | Voice input for grading assistance |
| F-8 | Content quality scoring | Low | M | AI scores its own output quality |
| F-9 | Cross-class content sharing | Low | L | Share generated content across teachers |
| F-10 | AI-powered report card comments | Medium | S | Generate report card comments from marks data |

---

## Appendix A: Sequence Diagrams

### A.1 Question Paper Generation Flow

```
Teacher (app)       Server              AiService          LLM
  │                  │                    │                 │
  │  POST /copilot/  │                    │                 │
  │  question-paper  │                    │                 │
  │  {subject, grade,│                    │                 │
  │   marks, topics, │                    │                 │
  │   difficulty}    │                    │                 │
  │  ──────────────> │                    │                 │
  │                  │──check rate limit──>│                 │
  │                  │──validate input────││                 │
  │                  │  (marks match)     ││                 │
  │                  │──build prompt─────→│                 │
  │                  │                    │──LLM call──────→│
  │                  │                    │←──JSON questions│
  │                  │←──question paper───│                 │
  │                  │──save usage────────→│                 │
  │                  │──update daily──────→│                 │
  │  ←──200: question paper + tokens      │                 │
  │                  │                    │                 │
  │  ──render editable questions──        │                 │
  │                  │                    │                 │
  │  ──teacher edits──                    │                 │
  │  ──teacher saves──                    │                 │
  │  ──────────────> │                    │                 │
  │                  │──update is_edited──→│                 │
  │  ←──200: saved    │                    │                 │
  │                  │                    │                 │
```

### A.2 Grading Assistance Flow

```
Teacher (app)       Server              AiService          LLM
  │                  │                    │                 │
  │  POST /copilot/  │                    │                 │
  │  grading-assist  │                    │                 │
  │  {question,      │                    │                 │
  │   model_answer,  │                    │                 │
  │   student_answer,│                    │                 │
  │   max_marks}     │                    │                 │
  │  ──────────────> │                    │                 │
  │                  │──check rate limit──>│                 │
  │                  │──build prompt─────→│                 │
  │                  │                    │──LLM call──────→│
  │                  │                    │←──JSON suggestion│
  │                  │←──suggestion───────│                 │
  │                  │──save usage────────→│                 │
  │  ←──200: suggestion (marks, feedback) │                 │
  │                  │                    │                 │
  │  ──teacher reviews──                   │                 │
  │  ──teacher accepts/adjusts──           │                 │
  │  ──marks applied to student──          │                 │
  │                  │                    │                 │
```

---

## Appendix B: Domain Model / ER Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                   teacher_copilot_usage (new)                         │
│  id (PK)                                                              │
│  school_id, teacher_id                                                │
│  usage_type (lesson_plan|question_paper|rubric|grading_assist|        │
│              parent_comm|worksheet)                                   │
│  subject, input_context (JSON), output_content (JSON)                 │
│  is_edited, model_used, tokens_used                                   │
│  created_at                                                           │
│  INDEX: (teacher_id, created_at DESC)                                 │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│                    teacher_copilot_daily (new)                        │
│  teacher_id (PK part 1)                                               │
│  date (PK part 2)                                                     │
│  usage_count, tokens_used                                             │
│  PRIMARY KEY: (teacher_id, date)                                      │
└──────────────────────────────────────────────────────────────────────┘

Existing tables used (read-only):
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│ assessments       │  │ assessment_marks │  │ curriculum_units │
│ (existing)        │  │ (existing)       │  │ (existing)       │
└──────────────────┘  └──────────────────┘  └──────────────────┘
┌──────────────────┐
│ students          │
│ (existing)        │
└──────────────────┘
```

---

## Appendix C: Event Flow

### Domain Events

| Event | Emitter | Consumers | Payload | Side Effects |
|---|---|---|---|---|
| `CopilotContentGenerated` | `TeacherCopilotService.*()` | None (logged) | `teacherId, usageType, subject, tokensUsed, model` | Usage record saved, daily count updated |
| `CopilotContentEdited` | Content save endpoint | None (logged) | `usageId, teacherId, editTimestamp` | `is_edited = true` |
| `CopilotContentSaved` | Content save endpoint | None (logged) | `usageId, teacherId, usageType` | Content finalized |
| `CopilotRateLimitExceeded` | `TeacherCopilotService.*()` | None (logged) | `teacherId, dailyCount` | 429 returned |

### Event Delivery Guarantees

- Events emitted synchronously within service methods
- All events logged for audit
- No external consumers — events are internal audit trail

---

## Appendix D: Configuration

### Environment Variables

| Variable | Default | Description |
|---|---|---|
| `TEACHER_COPILOT_ENABLED` | `true` | Enable/disable copilot feature |
| `TEACHER_COPILOT_DAILY_LIMIT` | `100` | Max requests per teacher per day |
| `TEACHER_COPILOT_MAX_TOKENS_PER_REQUEST` | `4000` | Max tokens per AI response |
| `TEACHER_COPILOT_MODEL` | `copilot_v1` | AI model identifier |
| `TEACHER_COPILOT_HISTORY_PAGE_SIZE` | `20` | Records per page in history |

### Feature Flags

| Flag | Default | Description |
|---|---|---|
| `TEACHER_COPILOT_ENABLED` | `true` | Enable/disable copilot |
| `TEACHER_COPILOT_LESSON_PLAN_ENABLED` | `true` | Enable/disable lesson plan generation |
| `TEACHER_COPILOT_QUESTION_PAPER_ENABLED` | `true` | Enable/disable question paper generation |
| `TEACHER_COPILOT_RUBRIC_ENABLED` | `true` | Enable/disable rubric generation |
| `TEACHER_COPILOT_GRADING_ASSIST_ENABLED` | `true` | Enable/disable grading assistance |
| `TEACHER_COPILOT_PARENT_COMM_ENABLED` | `true` | Enable/disable parent communication drafting |
| `TEACHER_COPILOT_WORKSHEET_ENABLED` | `true` | Enable/disable worksheet generation |

### School-Level Settings

| Setting | Default | Description |
|---|---|---|
| `copilot_daily_limit` | `100` | Per-school override for daily request limit |
| `copilot_enabled` | `true` | Per-school enable/disable |

---

## Appendix E: Migration & Rollback

### Migration: `migration_077_teacher_copilot.sql`

```sql
-- Migration 077: Teacher Copilot
-- Creates teacher_copilot_usage, teacher_copilot_daily tables

BEGIN;

CREATE TABLE IF NOT EXISTS teacher_copilot_usage (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    teacher_id      UUID NOT NULL,
    usage_type      VARCHAR(32) NOT NULL,
    subject         TEXT,
    input_context   TEXT,
    output_content  TEXT NOT NULL,
    is_edited       BOOLEAN NOT NULL DEFAULT false,
    model_used      VARCHAR(64),
    tokens_used     INTEGER,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_copilot_usage_teacher
    ON teacher_copilot_usage (teacher_id, created_at DESC);

CREATE TABLE IF NOT EXISTS teacher_copilot_daily (
    teacher_id      UUID NOT NULL,
    date            DATE NOT NULL,
    usage_count     INTEGER NOT NULL DEFAULT 0,
    tokens_used     INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (teacher_id, date)
);

COMMIT;
```

### Rollback: `migration_077_rollback.sql`

```sql
BEGIN;
DROP TABLE IF EXISTS teacher_copilot_daily;
DROP TABLE IF EXISTS teacher_copilot_usage;
COMMIT;
```

### Migration Validation

- Verify 2 tables created with correct columns
- Verify `teacher_copilot_daily(teacher_id, date)` PRIMARY KEY
- Verify index on `teacher_copilot_usage(teacher_id, created_at DESC)`
- Run `SELECT count(*) FROM teacher_copilot_usage` — should be 0 (new feature)

---

## Appendix F: Observability

### Structured Logging

| Log Level | Event | Context Fields |
|---|---|---|
| INFO | Content generated | `teacherId, schoolId, usageType, subject, tokensUsed, model` |
| INFO | Content edited | `usageId, teacherId, editTimestamp` |
| INFO | Content saved | `usageId, teacherId, usageType` |
| INFO | History viewed | `teacherId, page` |
| WARN | Rate limit exceeded | `teacherId, dailyCount, limit` |
| WARN | LLM provider failover | `primaryProvider, secondaryProvider, reason` |
| WARN | AI generated invalid JSON | `teacherId, usageType, rawResponse` |
| ERROR | AI service unavailable | `teacherId, usageType, error` |
| ERROR | Content save failed | `usageId, teacherId, error` |

### Metrics

| Metric | Type | Labels | Description |
|---|---|---|---|
| `copilot_requests_total` | Counter | `school_id, usage_type` | Total requests by type |
| `copilot_tokens_used` | Counter | `model` | Total tokens consumed |
| `copilot_response_duration` | Histogram | `usage_type` | AI response latency |
| `copilot_rate_limit_hits` | Counter | `school_id` | Rate limit 429 count |
| `copilot_edit_rate` | Gauge | `school_id, usage_type` | % of outputs edited by teachers |
| `copilot_active_teachers` | Gauge | `school_id` | Unique teachers using copilot per day |
| `copilot_daily_requests` | Histogram | `school_id` | Requests per teacher per day |

### Health Checks

| Check | Endpoint | Description |
|---|---|---|
| Copilot service | `/health/copilot` | Verify service and DB accessible |
| LLM connectivity | `/health/ai` | Verify LLM providers reachable (existing) |

### Alerts

| Alert | Condition | Severity | Notification |
|---|---|---|---|
| Copilot error rate high | Error rate > 5% | Warning | Email to dev team |
| LLM failover rate high | Failover > 20% | Warning | Email to dev team |
| Token usage spike | Daily tokens > 150% of average | Warning | Email to dev team (cost) |
| Rate limit hits increasing | 429 rate > 15% | Info | Email to product team (consider raising limit) |

### Dashboards

| Dashboard | Panels | Audience |
|---|---|---|
| Copilot Usage | Requests/day, by type, active teachers, tokens/day | Product Team |
| Copilot Performance | Response duration, error rate, failover rate | Dev Team |
| Edit Rate | % of outputs edited, by type | Product Team |
| Cost Analysis | Tokens/day, cost/day, cost per teacher | Dev Team |

### Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| AI generates incorrect content | Medium | Medium | Teacher reviews all outputs. Editable. |
| AI generates inappropriate content | Low | High | Safety guidelines. Teacher can discard. Log incident. |
| LLM unavailable | Low | High | Multi-provider failover. Graceful 503. |
| Token cost explosion | Medium | Medium | Rate limiting (100/day). Token monitoring. Alerts. |
| Teacher over-reliance on AI | Medium | Medium | All outputs editable. AI is advisory only. |
| Grading inconsistency | Low | Medium | AI suggests, teacher decides. Consistent rubrics help. |
| Slow AI responses | Medium | Medium | 10-15 second targets. Timeout at 30 seconds. |
| DB storage growth | Low | Low | ~75MB/day. Manageable. Archive old records. |
| Privacy/PII concerns | Low | Medium | Minimal PII to LLM. All content stored for audit. |
