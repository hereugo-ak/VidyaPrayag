# AI Exam Analysis — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** `AI_INFRASTRUCTURE_SPEC.md`

---

## 1. Feature Overview

AI-powered analysis of exam results that identifies weak subjects, performance trends, class-level insights, and generates personalized feedback for students and parents. Uses the shared `AiService` to call LLM providers with assessment data.

### Goals

- Per-student AI analysis: weak subjects, improvement areas, trend (improving/declining)
- Per-class AI analysis: subject-wise performance distribution, common weak areas
- Personalized narrative feedback for report cards
- Predictive insight: "at risk" students flagged based on trend
- Parent-friendly summary with actionable recommendations

---

## 2. Current System Assessment

- `AssessmentsTable` + `AssessmentMarksTable` — typed marks with `maxMarks`, `marks`, `isAbsent`, `remark`, `status`
- `feature_audit.csv` L107: Report Cards stub at 20%
- No AI analysis exists (`feature_audit.csv` L158: AI Tutoring missing)
- `DIFFERENTIATING_FEATURES.md` §1.4: AI Exam Analysis, effort M, data readiness: marks data exists

### Data Available

- Per student: all assessment marks across subjects and terms
- Per class: aggregated marks for all students
- Historical: multiple terms/years for trend analysis

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Generate per-student AI analysis: strengths, weaknesses, trend, recommendations |
| FR-2 | Generate per-class AI analysis: subject distribution, common weak areas, top performers |
| FR-3 | Identify "at risk" students (declining trend + below-average marks) |
| FR-4 | Generate personalized feedback paragraph for report cards |
| FR-5 | Parent-friendly summary with actionable recommendations |
| FR-6 | Support batch analysis for entire class (async job via `AiJobsTable`) |
| FR-7 | Analysis cached per (student, term) — re-generated only when new marks added |

---

## 4. Database Design

### 4.1 New Table: `ai_exam_analyses`

```sql
CREATE TABLE ai_exam_analyses (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    student_id      UUID NOT NULL,
    academic_year_id UUID NOT NULL,
    term            VARCHAR(16) NOT NULL,
    analysis_type   VARCHAR(16) NOT NULL,          -- student | class
    class_id        UUID,                          -- for class-level analysis
    input_summary   TEXT NOT NULL,                 -- JSON: aggregated marks data sent to LLM
    analysis_result TEXT NOT NULL,                 -- JSON: structured AI response
    narrative       TEXT,                          -- plain-text feedback paragraph
    model_used      VARCHAR(64),
    tokens_used     INTEGER,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(school_id, student_id, academic_year_id, term, analysis_type)
);
CREATE INDEX idx_ai_exam_school_term ON ai_exam_analyses(school_id, term, created_at DESC);
```

### 4.2 Exposed Mapping

```kotlin
object AiExamAnalysesTable : UUIDTable("ai_exam_analyses", "id") {
    val schoolId       = uuid("school_id")
    val studentId      = uuid("student_id")
    val academicYearId = uuid("academic_year_id")
    val term           = varchar("term", 16)
    val analysisType   = varchar("analysis_type", 16)
    val classId        = uuid("class_id").nullable()
    val inputSummary   = text("input_summary")
    val analysisResult = text("analysis_result")
    val narrative      = text("narrative").nullable()
    val modelUsed      = varchar("model_used", 64).nullable()
    val tokensUsed     = integer("tokens_used").nullable()
    val createdAt      = timestamp("created_at")
    init {
        uniqueIndex("ux_ai_exam_unique", schoolId, studentId, academicYearId, term, analysisType)
        index("idx_ai_exam_school_term", false, schoolId, term, createdAt)
    }
}
```

---

## 5. Backend Architecture

### 5.1 ExamAnalysisService

```kotlin
class ExamAnalysisService(
    private val aiService: AiService,
    private val marksRepository: AssessmentMarksRepository
) {
    suspend fun analyzeStudent(
        schoolId: UUID, studentId: UUID, academicYearId: UUID, term: String
    ): AiExamAnalysisDto {
        // 1. Fetch all marks for student in term
        val marks = marksRepository.getByStudentAndTerm(studentId, term)
        // 2. Build input summary (subject, marks, maxMarks, percentage, grade, rank)
        val inputSummary = buildStudentInputSummary(marks)
        // 3. Check cache (ai_exam_analyses)
        // 4. Call AiService with "exam_analysis" template
        val result = aiService.complete(
            schoolId, null, "exam_analysis", "student_analysis_v1",
            mapOf(
                "student_name" to studentName,
                "class_name" to className,
                "marks_data" to inputSummary,
                "term" to term
            )
        )
        // 5. Parse structured response
        // 6. Store in ai_exam_analyses
        // 7. Return DTO
    }

    suspend fun analyzeClass(
        schoolId: UUID, classId: UUID, academicYearId: UUID, term: String
    ): AiExamAnalysisDto

    suspend fun batchAnalyzeClass(
        schoolId: UUID, classId: UUID, term: String
    ): UUID  // returns AI job ID
}
```

### 5.2 Prompt Template

**System prompt:**
```
You are an experienced education analyst. Analyze the student's exam performance and provide:
1. Strengths: subjects where student excels (≥75%)
2. Weaknesses: subjects needing improvement (<50%)
3. Trend: compare with previous term if available (improving/declining/stable)
4. At-risk: flag if declining trend AND below 50% in 2+ subjects
5. Recommendations: 3 actionable suggestions for improvement
6. Narrative: 2-3 sentence feedback paragraph for report card
```

**User prompt template:**
```
Student: {{student_name}}, Class: {{class_name}}, Term: {{term}}
Marks: {{marks_data}}
Previous term marks: {{previous_marks}}
```

### 5.3 Structured Response

```json
{
  "strengths": ["Mathematics (87%)", "Science (82%)"],
  "weaknesses": ["Hindi (42%)", "Social Studies (48%)"],
  "trend": "improving",
  "at_risk": false,
  "recommendations": [
    "Focus on Hindi vocabulary building with daily 15-min practice",
    "Use map-based learning for Social Studies",
    "Continue strong performance in Math and Science"
  ],
  "narrative": "Aarav has shown commendable improvement this term, particularly in Mathematics and Science. With focused effort in Hindi and Social Studies, he can achieve balanced excellence across all subjects."
}
```

---

## 6. API Contracts

### 6.1 Student Analysis

```
GET /api/v1/parent/ai-exam-analysis/{childId}?term=term1
POST /api/v1/school/ai-exam-analysis/student/{studentId}?term=term1
```

### 6.2 Class Analysis

```
POST /api/v1/school/ai-exam-analysis/class/{classId}?term=term1
```

### 6.3 Batch Analysis

```
POST /api/v1/school/ai-exam-analysis/batch
{
  "class_id": "uuid",
  "term": "term1"
}
```

Returns AI job ID. Status polled via `GET /api/v1/school/ai/jobs/{jobId}`.

---

## 7. Testing Strategy

- Unit: input summary builder, response parser, cache check
- Integration: mock LLM → analysis stored → retrieve → correct structure
- Batch: 30 students → all analyses generated → job completed

---

## 8. Acceptance Criteria

- [ ] Per-student analysis generates strengths, weaknesses, trend, recommendations
- [ ] Per-class analysis generates subject distribution and common weak areas
- [ ] At-risk students flagged correctly
- [ ] Narrative feedback suitable for report card inclusion
- [ ] Batch analysis processes entire class
- [ ] Results cached per (student, term)

---

## 9. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration, Exposed table |
| 2 | 2 days | ExamAnalysisService (student + class) |
| 3 | 1 day | Prompt template + seed |
| 4 | 1 day | Batch analysis integration |
| 5 | 2 days | API endpoints |
| 6 | 2 days | Client UI (parent analysis view, admin class analysis) |
| 7 | 1 day | Tests |

---

## 10. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | `AiExamAnalysesTable` |
| `server/.../feature/ai/exam/ExamAnalysisService.kt` | New | Core service |
| `server/.../feature/ai/exam/ExamAnalysisRouting.kt` | New | API endpoints |
| `docs/db/migration_039_ai_exam_analysis.sql` | New | DDL |
| `shared/.../feature/ai/ExamAnalysisApi.kt` | New | Client API |
| `composeApp/.../ui/v2/screens/parent/AiExamAnalysisScreen.kt` | New | Parent view |
| `composeApp/.../ui/v2/screens/admin/ClassAnalysisScreen.kt` | New | Admin class view |
