# Forms & Surveys — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** None
> **Source:** `DIFFERENTIATING_FEATURES.md` §4.2

---

## 1. Feature Overview

Dynamic form builder for schools to create and distribute forms/surveys to parents, teachers, or students. Supports multiple question types, conditional logic, response collection, and analytics.

### Goals

- Admin creates forms with multiple question types (text, multiple choice, rating, date, file upload)
- Conditional logic (show question B only if answer to A is X)
- Distribute to specific audiences (class, role, all school)
- Response collection with submission tracking
- Response analytics (summary, individual responses)
- Anonymous or identified responses (configurable)

---

## 2. Current System Assessment

- No form/survey system exists
- `feature_audit.csv` L158: Parent Feedback missing (0%)
- `DIFFERENTIATING_FEATURES.md` §4.2: Forms & Surveys, effort M

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Form builder: add questions (short_text, long_text, multiple_choice, checkbox, rating_1_5, rating_1_10, date, file_upload) |
| FR-2 | Conditional logic: show/hide questions based on previous answers |
| FR-3 | Required vs optional fields |
| FR-4 | Distribute to: class, role (parent/teacher), all school, broadcast group |
| FR-5 | Anonymous responses (no user ID linked) or identified (configurable) |
| FR-6 | Response deadline |
| FR-7 | Response analytics: summary (counts, averages for ratings), individual responses |
| FR-8 | Notification sent to audience when form published |
| FR-9 | Reminder notification for non-respondents |

---

## 4. Database Design

```sql
CREATE TABLE forms (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    title           TEXT NOT NULL,
    description     TEXT,
    created_by      UUID,
    audience_type   VARCHAR(32) NOT NULL,          -- ALL_SCHOOL | CLASS | ROLE | BROADCAST_GROUP
    audience_filter TEXT,                          -- JSON: {"class_id": "...", "role": "parent"}
    is_anonymous    BOOLEAN NOT NULL DEFAULT false,
    deadline        TIMESTAMP,
    status          VARCHAR(16) NOT NULL DEFAULT 'draft', -- draft | published | closed
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE form_questions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    form_id         UUID NOT NULL REFERENCES forms(id) ON DELETE CASCADE,
    question_text   TEXT NOT NULL,
    question_type   VARCHAR(16) NOT NULL,          -- short_text | long_text | multiple_choice | checkbox | rating_1_5 | rating_1_10 | date | file_upload
    options         TEXT,                          -- JSON array for choice questions
    is_required     BOOLEAN NOT NULL DEFAULT false,
    sequence        INTEGER NOT NULL,
    conditional_show TEXT,                         -- JSON: {"question_id": "...", "answer": "..."} — show this question only if condition met
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_form_questions_form ON form_questions(form_id, sequence);

CREATE TABLE form_responses (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    form_id         UUID NOT NULL REFERENCES forms(id) ON DELETE CASCADE,
    school_id       UUID NOT NULL,
    respondent_id   UUID,                          -- null if anonymous
    respondent_name TEXT,                          -- null if anonymous
    answers         TEXT NOT NULL,                 -- JSON: [{"question_id": "...", "answer": "..."}]
    submitted_at    TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(form_id, respondent_id)                 -- one response per user (if not anonymous)
);
CREATE INDEX idx_form_responses_form ON form_responses(form_id, submitted_at DESC);
```

---

## 5. API Contracts

```
# Admin
GET/POST /api/v1/school/forms
PATCH /api/v1/school/forms/{id}
POST /api/v1/school/forms/{id}/publish
POST /api/v1/school/forms/{id}/close
GET /api/v1/school/forms/{id}/responses
GET /api/v1/school/forms/{id}/analytics

# Parent/Teacher
GET /api/v1/parent/forms
GET /api/v1/parent/forms/{id}
POST /api/v1/parent/forms/{id}/submit  { answers: [...] }
GET /api/v1/teacher/forms
POST /api/v1/teacher/forms/{id}/submit
```

---

## 6. Acceptance Criteria

- [ ] Admin creates forms with multiple question types
- [ ] Conditional logic works (show/hide questions)
- [ ] Forms distributed to specified audience
- [ ] Notification sent on publish
- [ ] Responses collected and tracked
- [ ] Response analytics available (summary + individual)
- [ ] Anonymous mode hides respondent identity
- [ ] Deadline enforcement
- [ ] Reminder for non-respondents

---

## 7. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 2 days | DB migration, Exposed tables |
| 2 | 3 days | FormService (CRUD, conditional logic, distribution) |
| 3 | 2 days | Response analytics |
| 4 | 1 day | API endpoints + notification integration |
| 5 | 4 days | Client UI (form builder, form fill, response dashboard) |
| 6 | 2 days | Tests |

---

## 8. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | 3 form tables |
| `server/.../feature/forms/FormService.kt` | New | Core service |
| `server/.../feature/forms/FormRouting.kt` | New | API endpoints |
| `docs/db/migration_072_forms_surveys.sql` | New | DDL |
| `composeApp/.../ui/v2/screens/admin/FormBuilderScreen.kt` | New | Form builder UI |
| `composeApp/.../ui/v2/screens/parent/FormFillScreen.kt` | New | Form submission |
| `composeApp/.../ui/v2/screens/admin/FormResponsesScreen.kt` | New | Response analytics |
