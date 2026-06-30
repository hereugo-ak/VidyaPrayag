# Forms & Surveys — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-28
> **Prerequisites:** None
> **Source:** `DIFFERENTIATING_FEATURES.md` §4.2
> **Template:** `_SPEC_TEMPLATE.md` v1 (25 mandatory + 6 optional sections)

---

## 1. Feature Overview

Dynamic form builder for schools to create and distribute forms/surveys to parents, teachers, or students. Supports multiple question types, conditional logic, response collection, and analytics.

### Goals

- Admin creates forms with multiple question types (text, multiple choice, rating, date, file upload)
- Conditional logic (show question B only if answer to A is X)
- Distribute to specific audiences (class, role, all school, broadcast group)
- Response collection with submission tracking
- Response analytics (summary, individual responses)
- Anonymous or identified responses (configurable)

### Non-goals

- [ ] Payment collection through forms (future: integrate with payment gateway)
- [ ] Multi-page forms with progress saving (future enhancement)
- [ ] Form templates marketplace (future)
- [ ] A/B testing of form variants
- [ ] Real-time collaborative form editing

### Dependencies

- `NotificationService` — existing notification infrastructure for form publish and reminders
- `NotifyRecipients.kt` — existing recipient resolver for audience targeting
- `SupabaseStorage` — existing file storage for file upload question type
- `BroadcastGroupsTable` — existing broadcast groups for audience targeting
- `WhatsappLogsTable` — existing WhatsApp infrastructure for notifications

### Related Modules

- `server/.../feature/forms/` — new forms & surveys module
- `shared/.../feature/forms/` — shared DTOs and API
- `composeApp/.../ui/v2/screens/admin/` — admin form builder UI
- `composeApp/.../ui/v2/screens/parent/` — parent form fill UI
- `composeApp/.../ui/v2/screens/teacher/` — teacher form fill UI

---

## 2. Current System Assessment

### Existing Code

- `feature_audit.csv` L158: Parent Feedback missing (0%)
- `DIFFERENTIATING_FEATURES.md` §4.2: Forms & Surveys, effort M
- No form/survey system exists in the codebase
- No form-related tables in `Tables.kt`
- No form-related routes in `Application.kt`

### Existing Database

- `AppUsersTable` — user accounts with `role` (parent, teacher, school_admin, etc.)
- `StudentsTable` — student records for class-based audience targeting
- `BroadcastGroupsTable` — broadcast groups for audience targeting
- `NotificationPreferencesTable` — notification preferences per user
- `WhatsappLogsTable` — WhatsApp message logs
- `SupabaseStorage` — file storage infrastructure

### Existing APIs

- Notification CRUD with audience targeting
- Broadcast group management
- File upload via `MediaRouting.kt`
- Auth: OTP login, password login

### Existing UI

- School portal with admin tabs (`SchoolPortalV2.kt`)
- Parent portal with announcement/feature access
- Teacher portal
- Web admin dashboard (`website/src/app/admin/`)

### Existing Services

- `NotificationService` — multi-channel notifications (push, in-app, WhatsApp)
- `NotifyRecipients.kt` — recipient resolution by audience type
- `SupabaseStorage` — file storage with kind-based paths

### Existing Documentation

- `feature_audit.csv` — Parent Feedback at 0%
- `DIFFERENTIATING_FEATURES.md` §4.2 — Forms & Surveys

### Technical Debt

| # | Gap | Details |
|---|---|---|
| TD-1 | No form system | No form tables, no form service, no form UI |
| TD-2 | No survey analytics | No response aggregation or analytics infrastructure |

### Gaps

| # | Gap | Impact | Severity |
|---|---|---|---|
| G1 | No form builder | Can't create or distribute forms | **High** |
| G2 | No response collection | Can't collect structured feedback | **High** |
| G3 | No response analytics | Can't analyze survey results | **Medium** |
| G4 | No conditional logic | Can't create smart forms | **Medium** |

---

## 3. Functional Requirements

### FR-001
| Field | Value |
|---|---|
| **Title** | Form Builder with Multiple Question Types |
| **Description** | Admin creates forms with multiple question types: short_text, long_text, multiple_choice, checkbox, rating_1_5, rating_1_10, date, file_upload. |
| **Priority** | Critical |
| **User Roles** | School Admin |
| **Acceptance notes** | Question types stored in `form_questions.question_type` column. Options for choice questions stored as JSON array. |

### FR-002
| Field | Value |
|---|---|
| **Title** | Conditional Logic |
| **Description** | Show/hide questions based on previous answers. Conditional show stored as JSON: `{"question_id": "...", "answer": "..."}`. |
| **Priority** | High |
| **User Roles** | School Admin |
| **Acceptance notes** | Evaluated client-side during form fill; server validates consistency on submit. |

### FR-003
| Field | Value |
|---|---|
| **Title** | Required vs Optional Fields |
| **Description** | Each question can be marked as required or optional. Required questions must be answered before submission. |
| **Priority** | Critical |
| **User Roles** | School Admin |
| **Acceptance notes** | `form_questions.is_required` boolean. Server validates required fields on submit. |

### FR-004
| Field | Value |
|---|---|
| **Title** | Audience Distribution |
| **Description** | Distribute forms to: class, role (parent/teacher), all school, broadcast group. |
| **Priority** | Critical |
| **User Roles** | School Admin |
| **Acceptance notes** | `forms.audience_type` + `forms.audience_filter` (JSON). Reuses existing audience resolution from `NotifyRecipients.kt`. |

### FR-005
| Field | Value |
|---|---|
| **Title** | Anonymous or Identified Responses |
| **Description** | Anonymous responses (no user ID linked) or identified (configurable per form). |
| **Priority** | High |
| **User Roles** | School Admin |
| **Acceptance notes** | `forms.is_anonymous` boolean. If true, `form_responses.respondent_id` and `respondent_name` are null. |

### FR-006
| Field | Value |
|---|---|
| **Title** | Response Deadline |
| **Description** | Configurable response deadline. Forms auto-close after deadline. |
| **Priority** | Medium |
| **User Roles** | School Admin |
| **Acceptance notes** | `forms.deadline` timestamp. Server rejects submissions after deadline. |

### FR-007
| Field | Value |
|---|---|
| **Title** | Response Analytics |
| **Description** | Response analytics: summary (counts, averages for ratings), individual responses. |
| **Priority** | High |
| **User Roles** | School Admin |
| **Acceptance notes** | Two endpoints: summary analytics + individual response list. |

### FR-008
| Field | Value |
|---|---|
| **Title** | Publication Notification |
| **Description** | Notification sent to audience when form published. |
| **Priority** | High |
| **User Roles** | System |
| **Acceptance notes** | Reuses `NotificationService` + `NotifyRecipients.kt` for audience resolution. |

### FR-009
| Field | Value |
|---|---|
| **Title** | Reminder for Non-Respondents |
| **Description** | Reminder notification for non-respondents. Sent to users who haven't submitted a response. |
| **Priority** | Medium |
| **User Roles** | System |
| **Acceptance notes** | Background job or manual trigger. Resolves audience, excludes respondents, sends reminder. |

---

## 4. User Stories

### School Admin
- [ ] Create a new form with title and description
- [ ] Add questions with various types (text, choice, rating, date, file upload)
- [ ] Set conditional logic (show question B if answer to A is X)
- [ ] Mark questions as required or optional
- [ ] Select target audience (class, role, all school, broadcast group)
- [ ] Configure anonymous or identified responses
- [ ] Set response deadline
- [ ] Publish form (sends notification to audience)
- [ ] View response analytics (summary + individual)
- [ ] Send reminder to non-respondents
- [ ] Close form manually

### Parent
- [ ] View available forms
- [ ] Fill and submit forms
- [ ] See confirmation after submission
- [ ] Cannot submit after deadline

### Teacher
- [ ] View available forms
- [ ] Fill and submit forms
- [ ] See confirmation after submission

### System
- [ ] Send notification when form published
- [ ] Send reminder to non-respondents
- [ ] Auto-close form after deadline
- [ ] Validate required fields on submission
- [ ] Validate conditional logic consistency

---

## 5. Business Rules

### BR-001
**Rule:** One response per user per form (for identified forms).
**Enforcement:** `UNIQUE(form_id, respondent_id)` constraint on `form_responses`. Anonymous forms allow multiple submissions.

### BR-002
**Rule:** Forms must be in `published` status to accept responses.
**Enforcement:** Server checks `forms.status = 'published'` before accepting submissions. Draft forms are not accessible to respondents.

### BR-003
**Rule:** Deadline enforcement.
**Enforcement:** Server checks `forms.deadline` on submission. If `deadline < now()`, reject with error. Form status auto-transitions to `closed` after deadline.

### BR-004
**Rule:** Required field validation.
**Enforcement:** Server validates all `is_required = true` questions have answers in the submission. Missing required answers → 400 error.

### BR-005
**Rule:** Conditional logic is client-side evaluated, server-validated.
**Enforcement:** Client shows/hides questions based on conditional logic. Server validates that hidden questions are not answered and required visible questions are answered.

### BR-006
**Rule:** Only school admin can create, edit, publish, and close forms.
**Enforcement:** `requireSchoolAdmin()` on all admin endpoints.

### BR-007
**Rule:** File upload questions use existing SupabaseStorage.
**Enforcement:** File uploads go through existing `MediaRouting.kt` upload endpoint. Answer stores the file URL.

---

## 6. Database Design

### 6.1 Entity Relationship Summary

Three new tables: `forms` (form metadata + audience + status), `form_questions` (questions with types, options, conditional logic), `form_responses` (submitted responses with answers as JSON).

### 6.2 New Tables

#### `forms` table

```sql
CREATE TABLE forms (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL REFERENCES schools(id),
    title           TEXT NOT NULL,
    description     TEXT,
    created_by      UUID REFERENCES app_users(id),
    audience_type   VARCHAR(32) NOT NULL,          -- ALL_SCHOOL | CLASS | ROLE | BROADCAST_GROUP
    audience_filter TEXT,                          -- JSON: {"class_id": "...", "role": "parent"}
    is_anonymous    BOOLEAN NOT NULL DEFAULT false,
    deadline        TIMESTAMP,
    status          VARCHAR(16) NOT NULL DEFAULT 'draft', -- draft | published | closed
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_forms_school ON forms(school_id, status);
```

#### `form_questions` table

```sql
CREATE TABLE form_questions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    form_id         UUID NOT NULL REFERENCES forms(id) ON DELETE CASCADE,
    question_text   TEXT NOT NULL,
    question_type   VARCHAR(16) NOT NULL,          -- short_text | long_text | multiple_choice | checkbox | rating_1_5 | rating_1_10 | date | file_upload
    options         TEXT,                          -- JSON array for choice questions
    is_required     BOOLEAN NOT NULL DEFAULT false,
    sequence        INTEGER NOT NULL,
    conditional_show TEXT,                         -- JSON: {"question_id": "...", "answer": "..."}
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_form_questions_form ON form_questions(form_id, sequence);
```

#### `form_responses` table

```sql
CREATE TABLE form_responses (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    form_id         UUID NOT NULL REFERENCES forms(id) ON DELETE CASCADE,
    school_id       UUID NOT NULL REFERENCES schools(id),
    respondent_id   UUID,                          -- null if anonymous
    respondent_name TEXT,                          -- null if anonymous
    answers         TEXT NOT NULL,                 -- JSON: [{"question_id": "...", "answer": "..."}]
    submitted_at    TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(form_id, respondent_id)                 -- one response per user (if not anonymous)
);
CREATE INDEX idx_form_responses_form ON form_responses(form_id, submitted_at DESC);
```

### 6.3 Modified Tables

None. All new tables.

### 6.4 Indexes

- `idx_forms_school` — form listing by school + status
- `idx_form_questions_form` — questions ordered by sequence
- `idx_form_responses_form` — responses ordered by submission date

### 6.5 Constraints

- `forms.school_id` — NOT NULL, FK to schools
- `forms.created_by` — nullable, FK to app_users
- `forms.status` — NOT NULL, one of draft/published/closed
- `forms.audience_type` — NOT NULL, one of ALL_SCHOOL/CLASS/ROLE/BROADCAST_GROUP
- `form_questions.form_id` — NOT NULL, FK (CASCADE)
- `form_questions.question_type` — NOT NULL, one of short_text/long_text/multiple_choice/checkbox/rating_1_5/rating_1_10/date/file_upload
- `form_questions.sequence` — NOT NULL, integer
- `form_responses.form_id` — NOT NULL, FK (CASCADE)
- `form_responses.school_id` — NOT NULL, FK to schools
- `form_responses.respondent_id` — nullable (null if anonymous)
- `UNIQUE(form_id, respondent_id)` — one response per user (if not anonymous)

### 6.6 Foreign Keys

- `forms.school_id` → `schools.id`
- `forms.created_by` → `app_users.id` (nullable)
- `form_questions.form_id` → `forms.id` (CASCADE)
- `form_responses.form_id` → `forms.id` (CASCADE)
- `form_responses.school_id` → `schools.id`
- `form_responses.respondent_id` → `app_users.id` (nullable)

### 6.7 Soft Delete Strategy

- Forms: `status = 'closed'` (not deleted). Admin can delete form (hard delete with CASCADE).
- Questions: deleted with form (CASCADE). Individual question deletion supported.
- Responses: not deleted (data integrity). Admin can delete individual responses.

### 6.8 Audit Fields

- `created_at` — all tables
- `updated_at` — forms only
- `submitted_at` — form_responses (submission timestamp)

### 6.9 Migration Notes

Migration: `docs/db/migration_072_forms_surveys.sql`
- Creates 3 new tables with FK constraints and indexes
- No data backfill (new feature)
- No modifications to existing tables

### 6.10 Exposed Mappings

Add 3 new table objects in `server/.../db/Tables.kt`:

- `FormsTable` — form metadata, audience, status
- `FormQuestionsTable` — questions with types, options, conditional logic
- `FormResponsesTable` — submitted responses with answers as JSON

Register in `DatabaseFactory.kt` `allTables` array. Order matters (FK dependencies):
1. `FormsTable`
2. `FormQuestionsTable` (FK to forms)
3. `FormResponsesTable` (FK to forms)

### 6.11 Seed Data

N/A — forms created by admin.

---

## 7. State Machines

### Form Lifecycle State Machine

```
DRAFT ──admin_publishes──> PUBLISHED ──admin_closes──> CLOSED
DRAFT ──admin_publishes──> PUBLISHED ──deadline_reached──> CLOSED
DRAFT ──admin_deletes──> DELETED
PUBLISHED ──admin_deletes──> DELETED
CLOSED ──admin_deletes──> DELETED
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `draft` | Admin publishes | `published` | Notification sent to audience |
| `draft` | Admin deletes | `deleted` | Hard delete |
| `published` | Admin closes | `closed` | No more responses accepted |
| `published` | Deadline reached | `closed` | Auto-close via background job or on submission attempt |
| `published` | Admin deletes | `deleted` | Hard delete |
| `closed` | Admin deletes | `deleted` | Hard delete |

### Response Submission State Machine

```
NOT_SUBMITTED ──user_submits──> SUBMITTED
NOT_SUBMITTED ──deadline_reached──> EXPIRED
SUBMITTED ──admin_deletes_response──> DELETED
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `not_submitted` | User submits | `submitted` | Form must be `published` and before deadline |
| `not_submitted` | Deadline reached | `expired` | Can no longer submit |
| `submitted` | Admin deletes response | `deleted` | Admin action |
```

---

## 8. Backend Architecture

### 8.1 Component Overview

`FormService` handles form CRUD, question management, publishing, response collection, and analytics. `FormRouting` exposes admin and respondent endpoints. Reuses `NotifyRecipients.kt` for audience resolution and `NotificationService` for notifications.

### 8.2 Design Principles

1. **Reuse existing infra** — notifications, audience resolution, file storage
2. **JSON for flexibility** — question options, conditional logic, and answers stored as JSON
3. **Server-side validation** — required fields, conditional logic consistency, deadline enforcement
4. **Multi-tenant isolation** — all queries filtered by `school_id`

### 8.3 Core Types

```kotlin
class FormService {
    // Admin — Form CRUD
    suspend fun listForms(schoolId: UUID, status: String?): List<FormDto>
    suspend fun getForm(schoolId: UUID, formId: UUID): FormDto?
    suspend fun createForm(schoolId: UUID, dto: CreateFormDto, createdBy: UUID): FormDto
    suspend fun updateForm(schoolId: UUID, formId: UUID, dto: UpdateFormDto): FormDto?
    suspend fun deleteForm(schoolId: UUID, formId: UUID): Boolean
    suspend fun publishForm(schoolId: UUID, formId: UUID): FormDto?
    suspend fun closeForm(schoolId: UUID, formId: UUID): FormDto?

    // Admin — Questions
    suspend fun addQuestion(schoolId: UUID, formId: UUID, dto: CreateQuestionDto): FormQuestionDto
    suspend fun updateQuestion(schoolId: UUID, formId: UUID, questionId: UUID, dto: UpdateQuestionDto): FormQuestionDto?
    suspend fun deleteQuestion(schoolId: UUID, formId: UUID, questionId: UUID): Boolean
    suspend fun reorderQuestions(schoolId: UUID, formId: UUID, questionIds: List<UUID>): List<FormQuestionDto>

    // Admin — Responses
    suspend fun listResponses(schoolId: UUID, formId: UUID): List<FormResponseDto>
    suspend fun getResponse(schoolId: UUID, responseId: UUID): FormResponseDto?
    suspend fun deleteResponse(schoolId: UUID, responseId: UUID): Boolean
    suspend fun getAnalytics(schoolId: UUID, formId: UUID): FormAnalyticsDto
    suspend fun sendReminder(schoolId: UUID, formId: UUID): Int

    // Respondent
    suspend fun listAvailableForms(userId: UUID, role: String, schoolId: UUID): List<FormDto>
    suspend fun getFormForRespondent(formId: UUID, userId: UUID): FormDto?
    suspend fun submitResponse(formId: UUID, userId: UUID, dto: SubmitResponseDto): FormResponseDto
}
```

### 8.4 Repositories

- `FormRepository` — form CRUD, status management
- `FormQuestionRepository` — question CRUD, ordering
- `FormResponseRepository` — response CRUD, analytics queries

### 8.5 Mappers

- `FormMapper` — maps DB rows to DTOs with question count and response count
- `QuestionMapper` — maps DB rows to DTOs with parsed JSON options and conditional logic
- `ResponseMapper` — maps DB rows to DTOs with parsed JSON answers

### 8.6 Permission Checks

- Admin endpoints: `requireSchoolContext()` + `requireSchoolAdmin()` for all writes
- Respondent endpoints: `requireAuth()` — any authenticated user (parent, teacher)
- Form access: server resolves audience and checks if user is in target audience

### 8.7 Background Jobs

- `FormDeadlineJob` — hourly; checks for published forms with passed deadlines, transitions to `closed`
- `FormReminderJob` — daily; sends reminder notifications for forms with pending non-respondents (if admin opted in)

### 8.8 Domain Events

- `FormCreated` — emitted on form creation
- `FormPublished` — emitted on form publish; triggers notification to audience
- `FormClosed` — emitted on form close (manual or auto)
- `ResponseSubmitted` — emitted on response submission
- `FormReminderSent` — emitted on reminder notification

### 8.9 Caching

- Form definition (questions, options): cached per form until published or edited
- Response analytics: cached for 5 minutes after first request
- No cache for form listing (real-time status needed)

### 8.10 Transactions

- Form creation with questions: insert form + insert questions in transaction
- Form publish: update status + send notification in transaction (notification async)
- Response submission: validate + insert response in transaction

### 8.11 Rate Limiting

- Standard API rate limiting for all endpoints
- No special rate limiting needed

### 8.12 Configuration

- `FORMS_REMINDER_ENABLED` — default `true`; enable/disable automatic reminders
- `FORMS_REMINDER_INTERVAL_HOURS` — default `24`; hours between reminders
- `FORMS_MAX_QUESTIONS` — default `50`; maximum questions per form
- `FORMS_ANALYTICS_CACHE_TTL` — default `300`; analytics cache TTL in seconds

---

## 9. API Contracts

### 9.1 Admin Endpoints

All require `requireSchoolContext()`. Writes require `requireSchoolAdmin()`.

```
# Form CRUD
GET    /api/v1/school/forms?status=&page=&limit=
POST   /api/v1/school/forms
GET    /api/v1/school/forms/{id}
PATCH  /api/v1/school/forms/{id}
DELETE /api/v1/school/forms/{id}
POST   /api/v1/school/forms/{id}/publish
POST   /api/v1/school/forms/{id}/close

# Questions
POST   /api/v1/school/forms/{id}/questions
PATCH  /api/v1/school/forms/{id}/questions/{qId}
DELETE /api/v1/school/forms/{id}/questions/{qId}
PUT    /api/v1/school/forms/{id}/questions/reorder

# Responses
GET    /api/v1/school/forms/{id}/responses
GET    /api/v1/school/forms/{id}/responses/{rId}
DELETE /api/v1/school/forms/{id}/responses/{rId}
GET    /api/v1/school/forms/{id}/analytics
POST   /api/v1/school/forms/{id}/remind
```

### 9.2 Respondent Endpoints

```
# Parent
GET    /api/v1/parent/forms
GET    /api/v1/parent/forms/{id}
POST   /api/v1/parent/forms/{id}/submit

# Teacher
GET    /api/v1/teacher/forms
GET    /api/v1/teacher/forms/{id}
POST   /api/v1/teacher/forms/{id}/submit
```

### 9.3 DTO Models

All `@Serializable`, wrapped in `ApiResponse<T>` pattern.

```kotlin
@Serializable data class FormDto(
    val id: String, val schoolId: String, val title: String,
    val description: String?, val createdBy: String?,
    val audienceType: String, val audienceFilter: String?,
    val isAnonymous: Boolean, val deadline: String?,
    val status: String, val questionCount: Int, val responseCount: Int,
    val createdAt: String, val updatedAt: String,
    val questions: List<FormQuestionDto> = emptyList()
)

@Serializable data class CreateFormDto(
    val title: String, val description: String?,
    val audienceType: String, val audienceFilter: String?,
    val isAnonymous: Boolean = false, val deadline: String?,
    val questions: List<CreateQuestionDto> = emptyList()
)

@Serializable data class UpdateFormDto(
    val title: String? = null, val description: String? = null,
    val audienceType: String? = null, val audienceFilter: String? = null,
    val isAnonymous: Boolean? = null, val deadline: String? = null
)

@Serializable data class FormQuestionDto(
    val id: String, val formId: String,
    val questionText: String, val questionType: String,
    val options: List<String>?, val isRequired: Boolean,
    val sequence: Int, val conditionalShow: ConditionalShow?,
    val createdAt: String
)

@Serializable data class CreateQuestionDto(
    val questionText: String, val questionType: String,
    val options: List<String>? = null, val isRequired: Boolean = false,
    val sequence: Int, val conditionalShow: ConditionalShow? = null
)

@Serializable data class UpdateQuestionDto(
    val questionText: String? = null, val questionType: String? = null,
    val options: List<String>? = null, val isRequired: Boolean? = null,
    val conditionalShow: ConditionalShow? = null
)

@Serializable data class ConditionalShow(
    val questionId: String, val answer: String
)

@Serializable data class FormResponseDto(
    val id: String, val formId: String,
    val respondentId: String?, val respondentName: String?,
    val answers: List<AnswerDto>,
    val submittedAt: String
)

@Serializable data class AnswerDto(
    val questionId: String, val answer: String
)

@Serializable data class SubmitResponseDto(
    val answers: List<AnswerDto>
)

@Serializable data class FormAnalyticsDto(
    val formId: String, val title: String,
    val totalResponses: Int, val questionAnalytics: List<QuestionAnalyticsDto>
)

@Serializable data class QuestionAnalyticsDto(
    val questionId: String, val questionText: String, val questionType: String,
    val responseCount: Int,
    val choiceDistribution: Map<String, Int>?,
    val ratingAverage: Double?,
    val textResponses: List<String>?
)
```

---

## 10. Frontend Architecture

### 10.1 Screens

| Screen | Platform | Role | Description |
|---|---|---|---|
| `FormBuilderScreen` | Compose | Admin | Drag-and-drop form builder with question types, options, conditional logic |
| `FormListScreen` | Compose | Admin | List of all forms with status, response count |
| `FormResponsesScreen` | Compose | Admin | Response analytics dashboard with summary + individual responses |
| `FormFillScreen` | Compose | Parent/Teacher | Form filling with conditional logic, validation, submission |
| `FormListParentScreen` | Compose | Parent | Available forms for parent |
| `FormListTeacherScreen` | Compose | Teacher | Available forms for teacher |
| Web admin forms page | Web | Admin | Form management dashboard |

### 10.2 Navigation

- Admin portal → Forms → `FormListScreen`
- Admin portal → Forms → New → `FormBuilderScreen`
- Admin portal → Forms → {form} → `FormResponsesScreen`
- Parent portal → Forms → `FormListParentScreen`
- Parent portal → Forms → {form} → `FormFillScreen`
- Teacher portal → Forms → `FormListTeacherScreen`
- Teacher portal → Forms → {form} → `FormFillScreen`
- Web admin → /admin/forms → form list page

### 10.3 UX Flows

#### Admin: Create and Publish Form

1. Admin opens Forms → clicks "New Form"
2. Enters title, description
3. Selects audience (class, role, all school, broadcast group)
4. Configures anonymous/identified
5. Sets deadline (optional)
6. Adds questions: select type, enter text, add options (for choice), set required, set conditional logic
7. Reorders questions via drag-and-drop
8. Saves as draft
9. Reviews form
10. Clicks "Publish" → notification sent to audience

#### Parent: Fill Form

1. Parent opens app → sees form notification or navigates to Forms
2. Opens form
3. Fills questions (conditional questions appear/disappear based on answers)
4. Uploads file if file_upload question
5. Submits
6. Sees confirmation

### 10.4 State Management

```kotlin
data class FormState(
    val forms: List<FormDto>,
    val currentForm: FormDto?,
    val responses: List<FormResponseDto>,
    val analytics: FormAnalyticsDto?,
    val isLoading: Boolean,
    val error: String?,
)
```

### 10.5 Offline Support

- Form definitions cached locally for offline filling
- Responses queued and submitted when online

### 10.6 Loading States

- Loading forms: "Loading forms..."
- Submitting response: "Submitting response..."
- Loading analytics: "Loading response analytics..."

### 10.7 Error Handling (UI)

- Form closed: "This form is no longer accepting responses."
- Deadline passed: "The deadline for this form has passed."
- Already submitted: "You have already submitted a response to this form."
- Required field missing: "Please answer all required questions."
- Form not found: "Form not found."

### 10.8 Component Integration Guidelines

| Rule | Description |
|---|---|
| **R1** | Drag-and-drop question reordering in form builder |
| **R2** | Question type selector with 8 types |
| **R3** | Options editor for choice questions (add/remove options) |
| **R4** | Conditional logic builder (select question + answer to trigger show) |
| **R5** | Required toggle per question |
| **R6** | Audience selector (class, role, all school, broadcast group) |
| **R7** | Anonymous toggle |
| **R8** | Deadline picker |
| **R9** | Form status badge (draft, published, closed) |
| **R10** | Response count badge |
| **R11** | Analytics charts: bar for choices, gauge for ratings, list for text |
| **R12** | Conditional question show/hide in form fill |
| **R13** | File upload component for file_upload questions |
| **R14** | Submit confirmation dialog |

---

## 11. Shared Module Changes (KMP)

### 11.1 DTOs

All DTOs defined in section 9.3, placed in `shared/.../feature/forms/data/remote/`.

### 11.2 Domain Models

```kotlin
data class Form(
    val id: UUID, val schoolId: UUID, val title: String,
    val description: String?, val createdBy: UUID?,
    val audienceType: AudienceType, val audienceFilter: String?,
    val isAnonymous: Boolean, val deadline: Instant?,
    val status: FormStatus,
    val questions: List<FormQuestion>,
)

enum class AudienceType { ALL_SCHOOL, CLASS, ROLE, BROADCAST_GROUP }
enum class FormStatus { DRAFT, PUBLISHED, CLOSED }

data class FormQuestion(
    val id: UUID, val formId: UUID,
    val questionText: String, val questionType: QuestionType,
    val options: List<String>?, val isRequired: Boolean,
    val sequence: Int, val conditionalShow: ConditionalShow?,
)

enum class QuestionType { SHORT_TEXT, LONG_TEXT, MULTIPLE_CHOICE, CHECKBOX, RATING_1_5, RATING_1_10, DATE, FILE_UPLOAD }

data class ConditionalShow(
    val questionId: UUID, val answer: String,
)

data class FormResponse(
    val id: UUID, val formId: UUID,
    val respondentId: UUID?, val respondentName: String?,
    val answers: List<Answer>,
    val submittedAt: Instant,
)

data class Answer(
    val questionId: UUID, val answer: String,
)

data class FormAnalytics(
    val formId: UUID, val title: String,
    val totalResponses: Int,
    val questionAnalytics: List<QuestionAnalytics>,
)

data class QuestionAnalytics(
    val questionId: UUID, val questionText: String, val questionType: QuestionType,
    val responseCount: Int,
    val choiceDistribution: Map<String, Int>?,
    val ratingAverage: Double?,
    val textResponses: List<String>?,
)
```

### 11.3 Repository Interfaces

```kotlin
interface FormRepository {
    suspend fun listForms(status: String?): NetworkResult<List<FormDto>>
    suspend fun getForm(id: String): NetworkResult<FormDto>
    suspend fun createForm(dto: CreateFormDto): NetworkResult<FormDto>
    suspend fun updateForm(id: String, dto: UpdateFormDto): NetworkResult<FormDto>
    suspend fun deleteForm(id: String): NetworkResult<Boolean>
    suspend fun publishForm(id: String): NetworkResult<FormDto>
    suspend fun closeForm(id: String): NetworkResult<FormDto>
    suspend fun listResponses(formId: String): NetworkResult<List<FormResponseDto>>
    suspend fun getAnalytics(formId: String): NetworkResult<FormAnalyticsDto>
    suspend fun sendReminder(formId: String): NetworkResult<Int>
    suspend fun listAvailableForms(): NetworkResult<List<FormDto>>
    suspend fun submitResponse(formId: String, dto: SubmitResponseDto): NetworkResult<FormResponseDto>
}
```

### 11.4 UseCases

- `ListFormsUseCase`, `GetFormUseCase`, `CreateFormUseCase`, `UpdateFormUseCase`
- `DeleteFormUseCase`, `PublishFormUseCase`, `CloseFormUseCase`
- `ListResponsesUseCase`, `GetAnalyticsUseCase`, `SendReminderUseCase`
- `ListAvailableFormsUseCase`, `SubmitResponseUseCase`

### 11.5 Validation

- Title: not empty, max 200 characters
- Question text: not empty, max 500 characters
- Options: at least 2 options for choice questions
- Sequence: positive integer
- Answers: required questions must have non-empty answers

### 11.6 Serialization

Standard Kotlinx serialization with JSON for options, conditional_show, and answers.

### 11.7 Network APIs

Ktor `@Resource` route definitions:
- `SchoolFormsApi` — admin endpoints
- `ParentFormsApi` — parent endpoints
- `TeacherFormsApi` — teacher endpoints

### 11.8 Database Models (Local Cache)

- Form definitions cached locally for offline filling
- Pending responses queued locally

---

## 12. Permissions Matrix

| Action | Super Admin | School Admin | Teacher | Parent |
|---|---|---|---|---|
| Create/edit/delete forms | ✅ | ✅ | ❌ | ❌ |
| Publish/close forms | ✅ | ✅ | ❌ | ❌ |
| Add/edit/delete questions | ✅ | ✅ | ❌ | ❌ |
| View responses | ✅ | ✅ | ❌ | ❌ |
| View analytics | ✅ | ✅ | ❌ | ❌ |
| Send reminders | ✅ | ✅ | ❌ | ❌ |
| View available forms | N/A | N/A | ✅ | ✅ |
| Submit response | N/A | N/A | ✅ | ✅ |

---

## 13. Notifications

### Form-Specific Notification Triggers

| Type | Trigger | Recipient | Channel | Message |
|---|---|---|---|---|
| Form Published | Admin publishes form | Target audience | Push + WhatsApp | "New form: {form_title}. Please submit your response by {deadline}." |
| Form Reminder | Admin triggers or daily job | Non-respondents | Push | "Reminder: {form_title} is awaiting your response. Deadline: {deadline}." |
| Form Closed | Admin closes form or deadline reached | Target audience | Push | "{form_title} is now closed. Thank you to all who responded." |
| Response Confirmation | User submits response | Respondent | Push | "Your response to {form_title} has been submitted. Thank you!" |

### Notification System Integration

- Reuse `NotificationService` for multi-channel delivery
- Reuse `NotifyRecipients.kt` for audience resolution (ALL_SCHOOL, CLASS, ROLE, BROADCAST_GROUP)
- Form publish notification: resolve audience → send push + WhatsApp
- Reminder: resolve audience → exclude respondents → send push
- No new audience types needed (reuses existing types)

---

## 14. Background Jobs

### Form Deadline Job

| Field | Value |
|---|---|
| **Name** | `FormDeadlineJob` |
| **Trigger** | Hourly |
| **Frequency** | Every hour |
| **Description** | Checks for published forms with passed deadlines, transitions status to `closed` |
| **Timeout** | 60 seconds |
| **Retry** | None |
| **On failure** | Logged; retried next hour |

### Form Reminder Job

| Field | Value |
|---|---|
| **Name** | `FormReminderJob` |
| **Trigger** | Daily |
| **Frequency** | Daily (9 AM) |
| **Description** | Sends reminder notifications for published forms with pending non-respondents |
| **Timeout** | 120 seconds |
| **Retry** | None |
| **On failure** | Logged; retried next day |

---

## 15. Integrations

### Internal Integrations

| System | Integration Point | Direction | Protocol | Error Handling |
|---|---|---|---|---|
| `NotificationService` | Form publish, reminder, close notifications | Outbound | Direct call | Logged; non-blocking |
| `NotifyRecipients.kt` | Audience resolution for notifications | Read | Direct code | Fallback: skip if no recipients |
| `SupabaseStorage` | File upload for file_upload question type | Write | Direct call | Return error to client |
| `MediaRouting.kt` | File upload endpoint | Write | HTTP | Standard error response |
| `WhatsappLogsTable` | WhatsApp message logging | Write | Direct DB | Logged |
| `BroadcastGroupsTable` | Broadcast group audience resolution | Read | Direct DB | Return empty if not found |
| `StudentsTable` | Class-based audience resolution | Read | Direct DB | Return empty if not found |
| `AppUsersTable` | Role-based audience resolution | Read | Direct DB | Return empty if not found |

### External Integrations

| System | Purpose | Direction | Protocol | Authentication | Error Handling |
|---|---|---|---|---|---|
| Meta WhatsApp Business API | Form publish/reminder notifications | Outbound | HTTP API | Bearer token | Retry 3x; log to `WhatsappLogsTable` |

### Integration Patterns

- **Notifications:** Reuse existing `NotificationService` + `NotifyRecipients.kt`. No new audience types needed — forms use existing ALL_SCHOOL, CLASS, ROLE, BROADCAST_GROUP types.
- **File Upload:** File upload questions use existing `MediaRouting.kt` upload endpoint. Answer stores the returned file URL. Max file size: 10MB (existing limit).
- **WhatsApp:** Reuse existing WhatsApp infrastructure. New templates: `form_published`, `form_reminder`. Submit for Meta approval early.

---

## 16. Security

### Authentication

- Admin endpoints: standard JWT auth via `requireSchoolContext()` + `requireSchoolAdmin()`
- Respondent endpoints: standard JWT auth via `requireAuth()`
- No public endpoints

### Authorization

- Only school admin can create, edit, publish, close, and delete forms
- Only school admin can view responses and analytics
- Any authenticated parent/teacher can view available forms and submit responses
- Server validates that respondent is in the form's target audience before accepting submission

### Data Protection

- Anonymous responses: `respondent_id` and `respondent_name` are null — no PII linked
- Identified responses: `respondent_id` linked to `app_users.id` — standard PII protection
- File uploads: stored in SupabaseStorage with school-scoped paths
- Form data is school-scoped — no cross-school access

### Input Validation

- Title: not empty, max 200 characters
- Description: max 2000 characters
- Question text: not empty, max 500 characters
- Options: at least 2 options for choice questions, max 20 options
- Sequence: positive integer
- Answers: required questions must have non-empty answers
- File upload: max 10MB (existing limit)
- Deadline: must be in the future when set

### Rate Limiting

- Standard API rate limiting for all endpoints
- No special rate limiting needed

### Audit Logging

- Form creation, update, deletion
- Form publish, close
- Question add, update, delete, reorder
- Response submission
- Response deletion
- Reminder sent

### PII Handling

- Anonymous forms: no PII collected (respondent_id = null)
- Identified forms: respondent_id and respondent_name stored — standard PII
- File uploads may contain PII — stored in school-scoped SupabaseStorage
- Admin can delete individual responses (right to erasure)

### Multi-tenant Isolation

- All queries filtered by `school_id`
- `forms.school_id` — NOT NULL, FK to schools
- `form_responses.school_id` — NOT NULL, FK to schools
- Server validates school context on all admin endpoints
- Respondent endpoints resolve school from JWT context

---

## 17. Performance & Scalability

### Expected Scale

- Small school: 10-50 forms, 100-500 responses per form
- Medium school: 50-200 forms, 500-2,000 responses per form
- Large school: 200-1,000 forms, 2,000-10,000 responses per form

### Query Optimization

- **Form listing:** `idx_forms_school` on `(school_id, status)`. Paginated.
- **Question retrieval:** `idx_form_questions_form` on `(form_id, sequence)`. Single query per form.
- **Response listing:** `idx_form_responses_form` on `(form_id, submitted_at DESC)`. Paginated.
- **Analytics:** Aggregate query per question. Cached for 5 minutes.
- **Available forms:** Query by audience resolution (existing `NotifyRecipients` pattern).

### Indexing Strategy

- `idx_forms_school` — form listing by school + status
- `idx_form_questions_form` — questions ordered by sequence
- `idx_form_responses_form` — responses ordered by submission date
- `UNIQUE(form_id, respondent_id)` — prevents duplicate submissions (identified forms)

### Caching Strategy

- Form definition (questions, options): cached per form until published or edited
- Response analytics: 5-minute TTL cache
- No cache for form listing (real-time status needed)

### Pagination

- Form listing: max 50 per page
- Response listing: max 100 per page
- Analytics: no pagination (aggregate per form)

### Connection Pooling

- Uses existing HikariCP connection pool
- No additional pooling needed

### Async Processing

- Notification delivery: async (existing `Notify.kt` pattern)
- Analytics computation: synchronous (small dataset, cached)
- Deadline check: hourly background job

### Scalability Concerns

- Analytics on forms with 10,000+ responses: cached for 5 minutes to reduce DB load
- JSON parsing of answers: O(n) per response where n = number of questions. Mitigated by pagination.
- Conditional logic evaluation: client-side (no server load)

---

## 18. Edge Cases

| # | Scenario | Expected Behavior |
|---|---|---|
| EC-1 | User submits response to draft form | Return 400 "This form is not yet published." |
| EC-2 | User submits response to closed form | Return 400 "This form is no longer accepting responses." |
| EC-3 | User submits response after deadline | Return 400 "The deadline for this form has passed." |
| EC-4 | User submits second response to identified form | Return 409 "You have already submitted a response to this form." |
| EC-5 | User submits second response to anonymous form | Allowed (no unique constraint on anonymous). |
| EC-6 | Required question not answered | Return 400 "Please answer all required questions: {question_text}" |
| EC-7 | Answer to hidden question (conditional logic) submitted | Server strips hidden question answers; only visible question answers stored. |
| EC-8 | Admin edits published form | Allowed for description, deadline. Question changes blocked for published forms. Return 400 "Cannot edit questions of a published form. Close and create a new form." |
| EC-9 | Admin deletes form with responses | Allowed (CASCADE delete). Confirm dialog in UI. |
| EC-10 | Form with no questions published | Return 400 "Cannot publish a form with no questions." |
| EC-11 | File upload question with no file submitted | If required → 400. If optional → skip. |
| EC-12 | Choice question with invalid option | Return 400 "Invalid option selected for {question_text}." |
| EC-13 | Rating question with out-of-range value | Return 400 "Rating must be between 1 and {max}." |
| EC-14 | Deadline set in the past | Return 400 "Deadline must be in the future." |
| EC-15 | Reminder sent for form with no non-respondents | Return 200 with count=0. No notifications sent. |
| EC-16 | User not in target audience tries to access form | Return 403 "You are not in the target audience for this form." |
| EC-17 | Anonymous form with respondent_id in submission | Server ignores respondent_id; stores as null. |
| EC-18 | Form with deadline exactly at current time | Server checks `deadline > now()` (strict). If equal, form is closed. |

---

## 19. Error Handling

### Error Response Format

Standard `ApiResponse` error format:
```json
{
  "success": false,
  "error": {
    "code": "FORM_NOT_FOUND",
    "message": "Form not found",
    "details": {}
  }
}
```

### Error Codes

| Code | HTTP Status | Description | User Message |
|---|---|---|---|
| `FORM_NOT_FOUND` | 404 | Form not found | "Form not found." |
| `FORM_NOT_PUBLISHED` | 400 | Form is in draft status | "This form is not yet published." |
| `FORM_CLOSED` | 400 | Form is closed | "This form is no longer accepting responses." |
| `FORM_DEADLINE_PASSED` | 400 | Deadline has passed | "The deadline for this form has passed." |
| `ALREADY_SUBMITTED` | 409 | Duplicate submission (identified) | "You have already submitted a response to this form." |
| `REQUIRED_FIELD_MISSING` | 400 | Required question not answered | "Please answer all required questions." |
| `INVALID_OPTION` | 400 | Invalid choice option | "Invalid option selected." |
| `INVALID_RATING` | 400 | Rating out of range | "Rating must be between 1 and {max}." |
| `NOT_IN_AUDIENCE` | 403 | User not in target audience | "You are not in the target audience for this form." |
| `CANNOT_EDIT_PUBLISHED` | 400 | Editing questions of published form | "Cannot edit questions of a published form." |
| `NO_QUESTIONS` | 400 | Publishing form with no questions | "Cannot publish a form with no questions." |
| `INVALID_DEADLINE` | 400 | Deadline in the past | "Deadline must be in the future." |

### Error Handling Strategy

- **Validation errors:** Return 400 with field-specific message
- **Auth errors:** Return 401/403 with clear message
- **Not found:** Return 404
- **Conflicts:** Return 409
- **Server errors:** Return 500 with generic message; log full error

### Retry Strategy

- Client retries: 3 attempts with exponential backoff for 5xx errors
- No retry for 4xx errors (client errors)
- WhatsApp delivery: 3 retries with 5-second intervals (existing pattern)

### Fallback Behavior

- WhatsApp not available: Fall back to push notification only
- Analytics cache miss: Compute on-demand (slower but correct)
- File upload fails: Return error to user; response not submitted

---

## 20. Analytics & Reporting

### Analytics Dashboard Data

| Metric | Source | Derivation |
|---|---|---|
| Total responses | `form_responses` count by form | Direct count |
| Response rate | Total responses / Target audience size | Derived |
| Per-question choice distribution | `form_responses.answers` JSON | Parse JSON, group by answer, count |
| Per-question rating average | `form_responses.answers` JSON | Parse JSON, extract numeric, average |
| Per-question text responses | `form_responses.answers` JSON | Parse JSON, list text answers |
| Submission timeline | `form_responses.submitted_at` | Group by day/hour, count |

### Export Capabilities

- Response export (CSV) — all responses with answers as columns
- Summary export (CSV) — choice distribution + rating averages per question

### Report Types

| Report | Format | Frequency | Recipient |
|---|---|---|---|
| Response summary | CSV | On-demand | School Admin |
| Individual responses | CSV | On-demand | School Admin |
| Analytics overview | JSON (API) | On-demand | School Admin |

---

## 21. Testing Strategy

### Unit Tests

- `FormService` — all methods (CRUD, publish, close, questions, responses, analytics, reminder)
- Conditional logic evaluation
- Required field validation
- Deadline enforcement
- Anonymous vs identified response handling
- Analytics computation (choice distribution, rating average)

### Integration Tests

- Form creation with questions → publish → notification sent
- Response submission → validation → storage → confirmation
- Deadline auto-close: create form with past deadline → run job → verify closed
- Reminder: publish form → some users submit → run reminder → verify non-respondents notified
- Conditional logic: fill form with conditional questions → verify hidden questions not stored
- Anonymous form: submit → verify respondent_id is null
- Identified form: submit twice → verify 409 on second submission
- Multi-tenant: form from school A not accessible to school B users

### E2E Tests

- Admin creates form → publishes → parent fills → admin views analytics
- Admin creates form with conditional logic → parent fills → verify conditional show/hide
- Admin creates anonymous form → multiple parents fill → admin views analytics (no names)

### Performance Tests

- Analytics on form with 10,000 responses: < 2s response time (cached)
- Form listing with 500 forms: < 500ms response time
- Response submission: < 200ms response time

### Test Data

- 20 sample forms (draft, published, closed)
- 100 sample responses across forms
- 8 question types represented
- Forms with conditional logic
- Anonymous and identified forms

### Test Environment

- Test database with schema migration applied
- Mock WhatsApp API for notification tests
- Mock SupabaseStorage for file upload tests
- Test JWT tokens for admin, parent, teacher roles

---

## 22. Acceptance Criteria

- [ ] Admin creates forms with multiple question types (short_text, long_text, multiple_choice, checkbox, rating_1_5, rating_1_10, date, file_upload)
- [ ] Conditional logic works (show/hide questions based on previous answers)
- [ ] Required vs optional fields enforced
- [ ] Forms distributed to specified audience (class, role, all school, broadcast group)
- [ ] Notification sent on publish
- [ ] Responses collected and tracked
- [ ] Response analytics available (summary + individual)
- [ ] Anonymous mode hides respondent identity
- [ ] Deadline enforcement (manual close + auto-close)
- [ ] Reminder for non-respondents
- [ ] One response per user for identified forms
- [ ] Multiple responses allowed for anonymous forms
- [ ] File upload questions work with SupabaseStorage
- [ ] Form status lifecycle (draft → published → closed)
- [ ] All admin endpoints enforce `requireSchoolContext()` + `requireSchoolAdmin()`
- [ ] Respondent endpoints validate audience membership
- [ ] Compose app form builder UI
- [ ] Compose app form fill UI
- [ ] Compose app response analytics UI

---

## 23. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 2 days | DB migration (`072`), 3 Exposed table objects, `DatabaseFactory` registration |
| 2 | 3 days | `FormService.kt` — CRUD, question management, conditional logic, distribution, response collection |
| 3 | 2 days | Response analytics (choice distribution, rating averages, text responses) |
| 4 | 1 day | `FormRouting.kt` with all endpoints + DTOs, mount in `Application.kt`, notification integration |
| 5 | 4 days | Client UI: `FormBuilderScreen.kt` (drag-and-drop), `FormFillScreen.kt` (conditional logic), `FormResponsesScreen.kt` (analytics), wire into navigation |
| 6 | 2 days | Tests (server unit + integration, client unit) |

### Pre-Implementation Checklist

- [ ] Verify `NotifyRecipients.kt` supports BROADCAST_GROUP audience type
- [ ] Verify `MediaRouting.kt` file upload works for non-admin roles (parent, teacher)
- [ ] Check if drag-and-drop library is available in Compose (reorderable library or custom)
- [ ] Verify `BroadcastGroupsTable` schema for audience resolution

---

## 24. File-Level Impact Analysis

### Required (7 files)

| # | File | Change Type | Description |
|---|---|---|---|
| 1 | `docs/db/migration_072_forms_surveys.sql` | New | DDL: 3 tables with FK constraints and indexes |
| 2 | `server/.../db/Tables.kt` | Modify | Add 3 table objects (FormsTable, FormQuestionsTable, FormResponsesTable) |
| 3 | `server/.../db/DatabaseFactory.kt` | Modify | Register 3 tables in `allTables` array |
| 4 | `server/.../feature/forms/FormService.kt` | New | Core service (CRUD, questions, responses, analytics, reminders) |
| 5 | `server/.../feature/forms/FormRouting.kt` | New | API endpoints + DTOs + `formRouting()` function |
| 6 | `server/.../Application.kt` | Modify | Import + mount `formRouting()` |
| 7 | `shared/.../feature/forms/` | New | Shared DTOs, domain models, repository, API client |

### Client UI (3 files)

| # | File | Change Type | Description |
|---|---|---|---|
| 8 | `composeApp/.../ui/v2/screens/admin/FormBuilderScreen.kt` | New | Drag-and-drop form builder with question types, options, conditional logic |
| 9 | `composeApp/.../ui/v2/screens/parent/FormFillScreen.kt` | New | Form filling with conditional logic, validation, submission |
| 10 | `composeApp/.../ui/v2/screens/admin/FormResponsesScreen.kt` | New | Response analytics dashboard |

### Optional (2 files)

| # | File | Change Type | Description |
|---|---|---|---|
| 11 | `website/src/app/admin/forms/page.tsx` | New | Web admin form management page |
| 12 | `composeApp/.../ui/v2/screens/teacher/FormFillScreen.kt` | New | Teacher form fill (shared with parent or separate) |

---

## 25. Future Enhancements

| # | Enhancement | Priority | Effort | Notes |
|---|---|---|---|---|
| F-1 | Multi-page forms with progress saving | Medium | M | Future enhancement |
| F-2 | Form templates marketplace | Low | M | Pre-built templates for common surveys |
| F-3 | Payment collection through forms | Medium | L | Integrate with payment gateway |
| F-4 | A/B testing of form variants | Low | L | Future |
| F-5 | Real-time collaborative form editing | Low | L | Future |
| F-6 | Export to Google Sheets / Excel | Medium | S | Direct export integration |
| F-7 | Form scheduling (publish at future date) | Low | S | Auto-publish at scheduled time |
| F-8 | Response branching (different questions based on answers) | Medium | M | Enhanced conditional logic |
| F-9 | Form duplication / clone | Low | S | Copy existing form as starting point |
| F-10 | Multi-language forms | Low | M | i18n for form content |

---

## Appendix A: Sequence Diagrams

### A.1 Admin Creates and Publishes Form

```
Admin          FormService         FormsTable       FormQuestionsTable    NotificationService
  │                │                   │                    │                    │
  │──createForm(dto)─→│                │                    │                    │
  │                │──insert form─────→│                    │                    │
  │                │──insert questions────────────────────→│                    │
  │                │←──form + questions────────────────────│                    │
  │←──FormDto──────│                   │                    │                    │
  │                │                   │                    │                    │
  │──publishForm(id)─→│                │                    │                    │
  │                │──update status='published'───────────→│                    │
  │                │──resolve audience via NotifyRecipients│                    │
  │                │──send notification────────────────────────────────────────→│
  │←──FormDto──────│                   │                    │                    │
  │                │                   │                    │                    │
```

### A.2 Parent Fills and Submits Form

```
Parent          FormService         FormsTable       FormResponsesTable
  │                │                   │                    │
  │──getForm(id)──→│                   │                    │
  │                │──check audience membership            │                    │
  │                │──load form + questions──────────────→│                    │
  │←──FormDto──────│                   │                    │
  │                │                   │                    │
  │──submitResponse(answers)─→│        │                    │
  │                │──check status='published'             │                    │
  │                │──check deadline > now()               │                    │
  │                │──validate required fields             │                    │
  │                │──validate conditional logic           │                    │
  │                │──check unique(form_id, respondent_id) │                    │
  │                │──insert response──────────────────────────────────────────→│
  │                │──send confirmation notification       │                    │
  │←──FormResponseDto│                 │                    │                    │
  │                │                   │                    │
```

### A.3 Admin Views Analytics

```
Admin          FormService         FormResponsesTable    (analytics cache)
  │                │                   │                    │
  │──getAnalytics(id)─→│               │                    │
  │                │──check cache──────│──────────────────→│
  │                │  [cache hit]      │                    │
  │                │←──cached analytics│                    │
  │                │  [cache miss]     │                    │
  │                │──load all responses──────────────────→│
  │                │←──responses──────│                    │
  │                │──parse answers JSON per response      │
  │                │──compute choice distribution          │
  │                │──compute rating averages              │
  │                │──collect text responses               │
  │                │──cache result (5 min TTL)             │
  │←──FormAnalyticsDto│                │                    │
  │                │                   │                    │
```

### A.4 Deadline Auto-Close

```
FormDeadlineJob    FormService       FormsTable
  │                    │                 │
  │──checkDeadlines()──→│                 │
  │                    │──SELECT WHERE status='published' AND deadline < now()─→│
  │                    │←──expired forms──│
  │                    │──UPDATE status='closed' for each─────────────────────→│
  │                    │──send "form closed" notification to audience          │
  │←──count closed─────│                 │
  │                    │                 │
```

---

## Appendix B: Domain Model / ER Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                              schools                                  │
│  id (PK)                                                              │
└──────────────────────────┬───────────────────────────────────────────┘
                           │
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────────┐
│                              forms                                    │
│  id (PK)                                                              │
│  school_id (FK→schools)                                               │
│  title, description                                                   │
│  created_by (FK→app_users)                                            │
│  audience_type (ALL_SCHOOL|CLASS|ROLE|BROADCAST_GROUP)               │
│  audience_filter (JSON)                                               │
│  is_anonymous, deadline                                               │
│  status (draft|published|closed)                                      │
│  created_at, updated_at                                               │
└──────────┬───────────────────────────────────┬───────────────────────┘
           │                                   │
           │ (CASCADE)                         │ (CASCADE)
           ▼                                   ▼
┌──────────────────────────────┐    ┌──────────────────────────────────────┐
│      form_questions           │    │       form_responses                 │
│  id (PK)                      │    │  id (PK)                             │
│  form_id (FK→forms, CASCADE)  │    │  form_id (FK→forms, CASCADE)         │
│  question_text                │    │  school_id (FK→schools)              │
│  question_type                │    │  respondent_id (FK→app_users, null)  │
│  options (JSON array)         │    │  respondent_name (null if anonymous) │
│  is_required                  │    │  answers (JSON array)                │
│  sequence                     │    │  submitted_at                        │
│  conditional_show (JSON)      │    │  UNIQUE(form_id, respondent_id)      │
│  created_at                   │    │                                      │
└──────────────────────────────┘    └──────────────────────────────────────┘
```

---

## Appendix C: Event Flow

### Domain Events

| Event | Emitter | Consumers | Payload | Side Effects |
|---|---|---|---|---|
| `FormCreated` | `FormService.createForm()` | None | `formId, schoolId, title, createdBy` | None |
| `FormPublished` | `FormService.publishForm()` | NotificationService | `formId, schoolId, title, audienceType, audienceFilter, deadline` | Send notification to target audience |
| `FormClosed` | `FormService.closeForm()` or `FormDeadlineJob` | NotificationService | `formId, schoolId, title, reason (manual/auto)` | Send "form closed" notification to audience |
| `ResponseSubmitted` | `FormService.submitResponse()` | NotificationService | `responseId, formId, schoolId, respondentId (null if anonymous)` | Send confirmation to respondent |
| `FormReminderSent` | `FormService.sendReminder()` or `FormReminderJob` | None | `formId, schoolId, recipientCount` | None (notifications already sent) |

### Event Delivery Guarantees

- Events are emitted synchronously within the same transaction
- Notification delivery is async (fire-and-forget with logging)
- Failed notifications are logged in notification logs
- No event bus / message queue — direct function calls

---

## Appendix D: Configuration

### Environment Variables

| Variable | Default | Description |
|---|---|---|
| `FORMS_REMINDER_ENABLED` | `true` | Enable/disable automatic reminders |
| `FORMS_REMINDER_INTERVAL_HOURS` | `24` | Hours between reminders |
| `FORMS_MAX_QUESTIONS` | `50` | Maximum questions per form |
| `FORMS_ANALYTICS_CACHE_TTL` | `300` | Analytics cache TTL in seconds (5 min) |
| `FORMS_MAX_FILE_SIZE_MB` | `10` | Max file size for file_upload questions |

### Feature Flags

| Flag | Default | Description |
|---|---|---|
| `FORMS_ENABLED` | `true` | Enable/disable forms feature |
| `FORMS_FILE_UPLOAD_ENABLED` | `true` | Enable/disable file upload question type |
| `FORMS_ANONYMOUS_ENABLED` | `true` | Enable/disable anonymous form mode |
| `FORMS_CONDITIONAL_LOGIC_ENABLED` | `true` | Enable/disable conditional logic |

### School-Level Settings

N/A — forms are managed entirely by school admin. No school-level configuration needed beyond feature flags.

---

## Appendix E: Migration & Rollback

### Migration: `migration_072_forms_surveys.sql`

```sql
-- Migration 072: Forms & Surveys
-- Creates 3 new tables

BEGIN;

-- 1. Create forms table
CREATE TABLE IF NOT EXISTS forms (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL REFERENCES schools(id),
    title           TEXT NOT NULL,
    description     TEXT,
    created_by      UUID REFERENCES app_users(id),
    audience_type   VARCHAR(32) NOT NULL,
    audience_filter TEXT,
    is_anonymous    BOOLEAN NOT NULL DEFAULT false,
    deadline        TIMESTAMP,
    status          VARCHAR(16) NOT NULL DEFAULT 'draft',
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_forms_school ON forms(school_id, status);

-- 2. Create form_questions table
CREATE TABLE IF NOT EXISTS form_questions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    form_id         UUID NOT NULL REFERENCES forms(id) ON DELETE CASCADE,
    question_text   TEXT NOT NULL,
    question_type   VARCHAR(16) NOT NULL,
    options         TEXT,
    is_required     BOOLEAN NOT NULL DEFAULT false,
    sequence        INTEGER NOT NULL,
    conditional_show TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_form_questions_form ON form_questions(form_id, sequence);

-- 3. Create form_responses table
CREATE TABLE IF NOT EXISTS form_responses (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    form_id         UUID NOT NULL REFERENCES forms(id) ON DELETE CASCADE,
    school_id       UUID NOT NULL REFERENCES schools(id),
    respondent_id   UUID,
    respondent_name TEXT,
    answers         TEXT NOT NULL,
    submitted_at    TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(form_id, respondent_id)
);
CREATE INDEX IF NOT EXISTS idx_form_responses_form ON form_responses(form_id, submitted_at DESC);

COMMIT;
```

### Rollback: `migration_072_rollback.sql`

```sql
BEGIN;
DROP TABLE IF EXISTS form_responses;
DROP TABLE IF EXISTS form_questions;
DROP TABLE IF EXISTS forms;
COMMIT;
```

### Migration Validation

- Verify all 3 tables created with correct columns
- Verify all FK constraints in place
- Verify all indexes created
- Verify `UNIQUE(form_id, respondent_id)` constraint works
- Run `SELECT count(*) FROM forms` — should be 0 (new feature)

---

## Appendix F: Observability

### Structured Logging

| Log Level | Event | Context Fields |
|---|---|---|
| INFO | Form created | `formId, schoolId, title, createdBy` |
| INFO | Form published | `formId, schoolId, title, audienceType` |
| INFO | Form closed | `formId, schoolId, reason (manual/auto)` |
| INFO | Response submitted | `responseId, formId, schoolId, isAnonymous` |
| INFO | Reminder sent | `formId, schoolId, recipientCount` |
| WARN | Form deadline passed | `formId, schoolId, deadline` |
| WARN | Duplicate submission attempt | `formId, schoolId, respondentId` |
| WARN | Required field missing | `formId, schoolId, questionId` |
| ERROR | Notification delivery failed | `formId, channel, error` |

### Metrics

| Metric | Type | Labels | Description |
|---|---|---|---|
| `forms_total` | Gauge | `school_id, status` | Total forms by status |
| `forms_published_total` | Counter | `school_id` | Forms published |
| `form_responses_total` | Counter | `form_id` | Responses submitted |
| `form_response_duration` | Histogram | `form_id` | Response submission latency |
| `form_analytics_duration` | Histogram | `form_id` | Analytics query latency |
| `form_reminders_sent_total` | Counter | `school_id` | Reminders sent |

### Health Checks

| Check | Endpoint | Description |
|---|---|---|
| Forms tables exist | `/health/forms` | Verify 3 form tables are accessible |

### Alerts

| Alert | Condition | Severity | Notification |
|---|---|---|---|
| High form error rate | `form_response_duration` error rate > 5% | Warning | Email to dev team |
| Analytics slow | `form_analytics_duration` p95 > 5s | Warning | Email to dev team |
| Notification delivery failures | Form notification failure rate > 10% | Critical | PagerDuty / email |

### Dashboards

| Dashboard | Panels | Audience |
|---|---|---|
| Forms Overview | Total forms, published count, response rate, submission trend | School Admin |
| Response Analytics | Choice distribution, rating averages, text responses, timeline | School Admin |
| System Health | API latency, error rate, notification delivery rate | Dev Team |
