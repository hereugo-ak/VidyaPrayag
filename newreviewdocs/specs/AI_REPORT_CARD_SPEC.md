# AI Report Card — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** `AI_INFRASTRUCTURE_SPEC.md`, `NEP_COMPLIANCE_SPEC.md`

---

## 1. Feature Overview

AI-powered report card generation that creates narrative comments, predictive insights, and auto-assembled report cards combining academic marks, holistic assessments, and co-scholastic records. Replaces the existing 20% stub with a full AI-driven system.

### Goals

- Generate narrative comments per subject (not generic "good student")
- Predictive insights: likely grade next term, at-risk indicators
- Auto-assemble HPC (Holistic Progress Card) combining all dimensions
- AI-generated parent-friendly summary
- Batch generation for entire class
- Teacher review and edit before publishing

---

## 2. Current System Assessment

- `feature_audit.csv` L23, L107: Report Cards stub at 20%
- `ExamResultsTable` (deprecated) — legacy string-scored model
- `AssessmentsTable` + `AssessmentMarksTable` — typed marks model
- `NEP_COMPLIANCE_SPEC.md` defines `ReportCardsTable`, `ReportCardTemplatesTable`, `HolisticAssessmentsTable`, `CoScholasticRecordsTable`
- `ParentAchievementsTable` — badges, competencies, EI metrics
- No AI narrative generation

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Generate per-subject narrative comment based on marks, trend, and holistic data |
| FR-2 | Generate overall summary paragraph (parent-friendly) |
| FR-3 | Predictive insight: likely next-term performance, at-risk flag |
| FR-4 | Auto-assemble report card from: academic marks + holistic assessments + co-scholastic + AI narratives |
| FR-5 | Teacher can review and edit AI-generated content before publishing |
| FR-6 | Batch generation for entire class (async job) |
| FR-7 | Support board-specific templates (from `NEP_COMPLIANCE_SPEC.md`) |
| FR-8 | Multi-lingual narrative (school's medium of instruction) |

---

## 4. Database Design

Uses tables from `NEP_COMPLIANCE_SPEC.md` (`report_cards`, `report_card_templates`, `holistic_assessments`, `co_scholastic_records`).

### 4.1 New Table: `ai_report_card_narratives`

```sql
CREATE TABLE ai_report_card_narratives (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_card_id  UUID NOT NULL REFERENCES report_cards(id) ON DELETE CASCADE,
    subject_narratives TEXT NOT NULL,              -- JSON: [{"subject": "Math", "comment": "..."}]
    overall_summary TEXT NOT NULL,
    predictive_insight TEXT,                       -- JSON: {"likely_grade": "B", "at_risk": false, "reason": "..."}
    model_used      VARCHAR(64),
    tokens_used     INTEGER,
    is_edited       BOOLEAN NOT NULL DEFAULT false, -- teacher modified AI output
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);
```

---

## 5. Backend Architecture

### 5.1 ReportCardAiService

```kotlin
class ReportCardAiService(private val aiService: AiService) {
    suspend fun generateNarratives(
        schoolId: UUID, studentId: UUID, term: String, reportCardId: UUID
    ): AiNarrativeDto {
        // 1. Gather all data: marks, holistic assessments, co-scholastic, previous term
        // 2. Call LLM for per-subject narratives
        // 3. Call LLM for overall summary
        // 4. Call LLM for predictive insight
        // 5. Store in ai_report_card_narratives
    }

    suspend fun batchGenerate(schoolId: UUID, classId: UUID, term: String): UUID
}
```

### 5.2 Prompt Templates

**Per-subject narrative:**
```
System: You are a teacher writing a report card comment for {{subject}}.
Include: performance level, specific strength/weakness, one improvement suggestion.
Keep to 2-3 sentences. Be specific, not generic.

User: Student: {{name}}, Marks: {{marks}}/{{max}} ({{percentage}}%), Grade: {{grade}},
Previous term: {{prev_percentage}}%, Trend: {{trend}},
Holistic notes: {{holistic_notes}}
```

**Overall summary:**
```
System: Write a 3-4 sentence parent-friendly summary of the student's overall performance this term.
Include: overall assessment, key strength, area to focus on, encouraging closing.

User: Student: {{name}}, Subjects: {{subject_grades}}, Attendance: {{attendance}}%,
Co-scholastic: {{coscholastic_summary}}, Holistic: {{holistic_summary}}
```

**Predictive insight:**
```
System: Based on the student's performance trend, predict likely next-term performance.
Output JSON: {"likely_grade": "A|B|C|D", "at_risk": true|false, "reason": "..."}

User: Current term grades: {{grades}}, Previous term grades: {{prev_grades}},
Attendance trend: {{attendance_trend}}
```

---

## 6. API Contracts

```
POST /api/v1/school/report-cards/{id}/generate-narratives
POST /api/v1/school/report-cards/batch-generate
{
  "class_id": "uuid",
  "term": "term1"
}
PATCH /api/v1/school/report-cards/{id}/narratives
{
  "subject_narratives": [...],  // teacher-edited
  "overall_summary": "..."
}
POST /api/v1/school/report-cards/{id}/publish
```

---

## 7. Acceptance Criteria

- [ ] Per-subject narrative comments generated with specific, non-generic content
- [ ] Overall summary is parent-friendly and encouraging
- [ ] Predictive insight includes likely grade and at-risk flag
- [ ] Teacher can edit narratives before publishing
- [ ] Batch generation works for entire class
- [ ] Board-specific template rendering correct
- [ ] Multi-lingual support (at least Hindi + English)

---

## 8. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration, Exposed table |
| 2 | 3 days | ReportCardAiService (narratives + summary + predictive) |
| 3 | 2 days | Prompt templates + seed |
| 4 | 2 days | Batch generation integration |
| 5 | 2 days | API endpoints (generate, edit, publish) |
| 6 | 3 days | Client UI (narrative editor, preview, publish flow) |
| 7 | 2 days | Tests |

---

## 9. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | `AiReportCardNarrativesTable` |
| `server/.../feature/ai/reportcard/ReportCardAiService.kt` | New | AI narrative generation |
| `server/.../feature/nep/ReportCardService.kt` | Modify | Integrate AI narratives into assembly |
| `docs/db/migration_043_ai_report_card.sql` | New | DDL |
| `composeApp/.../ui/v2/screens/admin/ReportCardNarrativeEditor.kt` | New | Teacher edit UI |
