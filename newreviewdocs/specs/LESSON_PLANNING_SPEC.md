# Lesson Planning — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27 (rev 2 — assignment-scoped architecture alignment)
> **Prerequisites:** None
> **Audit ref:** `feature_audit.csv` L127; IMPLEMENTATION_BACKLOG P1-20

---

## 1. Feature Overview

Digital lesson planning for teachers: create, store, and share lesson plans linked to curriculum units and syllabus progress. Enables structured teaching with objectives, activities, resources, and assessment methods.

### Goals

- Teacher creates lesson plans scoped to a `TeacherSubjectAssignment` (assignment_id) — the X-1 ownership pattern used by every other teacher surface (attendance, gradebook, syllabus, homework)
- Link to curriculum units (`CurriculumUnitsTable`) and auto-update syllabus progress (`SyllabusProgressTable`) on completion
- Lesson plan: objectives, activities, resources, duration, assessment method, homework link, attachments
- Admin can review lesson plans (read-only, school-scoped)
- Reusable templates per subject (separate table, not a flag)
- Today tab integration — planned lessons surface in `ResolvedDayDto.periods`
- Calendar view (month-level, what's planned for each day)

### Non-Goals (future phases)

- Co-planning / sharing across teachers teaching the same subject
- Lesson plan approval workflow with revision requests
- Recurring lesson plans ("every Monday for 4 weeks")
- Parent/student notification on syllabus progress update
- Analytics dashboard ("lessons completed vs planned this term")

---

## 2. Current System Assessment

- `feature_audit.csv` L127: Lesson Planning missing (0%)
- `CurriculumUnitsTable` (`Tables.kt:1032-1042`) — hierarchical curriculum tracking: `schoolId`, `classId`, `subjectId`, `parentId` (chapter→topic), `title`, `position`, `isActive`
- `SyllabusProgressTable` (`Tables.kt:1052-1065`) — per-section coverage state: `unitId`, `section`, `assignmentId`, `isCovered`, `coveredOn` (typed date), `coveredBy`, `note`. Unique on `(unitId, section, assignmentId)`
- `HomeworkTable` (`Tables.kt:1083-1103`) — typed scope spine: `assignmentId`, `classId`, `subjectId` (X-1), legacy display columns retained
- `HomeworkAttachmentsTable` (`Tables.kt:1109-1116`) — file/image attachments for homework (pattern to follow for lesson plan attachments)
- `TeacherSubjectAssignmentsTable` — teacher→subject→class+section mapping; the ownership boundary enforced via `requireOwnedAssignment()`
- `TeacherRepository` (`shared/.../teacher/domain/repository/TeacherRepository.kt`) — 79 lines, all methods assignment-scoped; no lesson plan methods exist
- `TeacherModels.kt` (`shared/.../teacher/domain/model/TeacherModels.kt`) — 1132 lines; `ResolvedDayDto`/`ResolvedPeriodDto` carry `assignmentId` per period (Today tab integration point)
- `TeacherRouting.kt` — `teacherRouting()` mounts at `/api/v1/teacher`; sub-surfaces registered via `teacherTaskRoutes()` (currently empty, documented landing zone)
- Existing routing pattern: each surface gets its own `Teacher*Routing.kt` file with `fun Route.teacher*Routing()` extension, DTOs prefixed (e.g. `Syl*`, `Hw*`, `Gb*`)
- Latest DB migration: `migration_024_conversation_seq_and_nullable_body.sql`

---

## 3. Functional Requirements

| ID | Requirement | Priority |
|---|---|---|
| FR-1 | Teacher creates lesson plan scoped to an owned assignment: title, unit (optional), objectives, activities, resources, duration, assessment method | P0 |
| FR-2 | Link to curriculum unit; on completion, auto-upsert `SyllabusProgressTable` (isCovered=true, coveredOn=today, coveredBy=teacher) | P0 |
| FR-3 | Link homework assignment to lesson plan (optional `homework_id`) | P0 |
| FR-4 | Admin can view all lesson plans for their school (read-only, filterable by teacher/class/date) | P0 |
| FR-5 | Save a lesson plan as a template; instantiate a new plan from a template | P0 |
| FR-6 | Lesson plan calendar view: month-level query returning plans grouped by date | P0 |
| FR-7 | Teacher can delete a lesson plan (soft delete: `deleted_at` timestamp) | P0 |
| FR-8 | Teacher can mark a lesson as `skipped` (distinct from `completed` — does NOT update syllabus progress) | P0 |
| FR-9 | GET single lesson plan by id (for edit screen hydration) | P0 |
| FR-10 | List lesson plans with filters: `assignmentId`, `status`, `date_range`, `unit_id` | P0 |
| FR-11 | Attachments: teacher can attach files/links to a lesson plan (URL-based, following `HomeworkAttachmentsTable` pattern) | P1 |
| FR-12 | Today tab integration: `ResolvedPeriodDto` carries a `lesson_plan_id` when a plan exists for that period's date + assignment | P1 |

---

## 4. Database Design

### 4.1 `lesson_plans` table

```sql
CREATE TABLE lesson_plans (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id         UUID NOT NULL,                        -- multi-tenant isolation
    teacher_id        UUID NOT NULL,                        -- FK app_users.id (author, denormalised from TSA)
    assignment_id     UUID NOT NULL,                        -- FK teacher_subject_assignments.id (X-1 scope)
    class_id          UUID NOT NULL,                        -- FK school_classes.id (denormalised from TSA)
    section           VARCHAR(8) NOT NULL DEFAULT 'A',      -- denormalised from TSA (matches SyllabusProgressTable)
    subject_id        UUID,                                 -- FK school_subjects.id (denormalised from TSA)
    subject_name      TEXT NOT NULL,                        -- display column (matches HomeworkTable pattern)
    curriculum_unit_id UUID,                                -- FK curriculum_units.id (optional)
    title             TEXT NOT NULL,
    objectives        TEXT NOT NULL,                        -- JSON array: ["Objective 1", "Objective 2"]
    activities        TEXT,                                 -- JSON: [{"activity": "...", "duration_min": 15}]
    resources         TEXT,                                 -- JSON: ["Textbook pg 45", "Video: ..."]
    assessment_method TEXT,
    duration_minutes  INTEGER NOT NULL DEFAULT 45,
    homework_id       UUID,                                 -- FK homework.id (optional)
    planned_date      DATE,                                 -- when the lesson is scheduled
    completed_at      TIMESTAMP,                            -- null until status=completed
    status            VARCHAR(16) NOT NULL DEFAULT 'planned', -- planned | completed | skipped
    is_template       BOOLEAN NOT NULL DEFAULT false,       -- see 4.2 for template table
    template_source_id UUID,                                -- FK lesson_plans.id (if created from a template)
    deleted_at        TIMESTAMP,                            -- soft delete (null = active)
    created_at        TIMESTAMP NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_lesson_plans_assignment ON lesson_plans(assignment_id, planned_date);
CREATE INDEX idx_lesson_plans_school ON lesson_plans(school_id, assignment_id);
CREATE INDEX idx_lesson_plans_calendar ON lesson_plans(teacher_id, planned_date, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_lesson_plans_unit ON lesson_plans(curriculum_unit_id) WHERE curriculum_unit_id IS NOT NULL;
```

**Key design decisions:**

- **`assignment_id` as primary scope** — aligns with the X-1 ownership pattern. Every read/write is guarded by `requireOwnedAssignment()`. `teacher_id`, `class_id`, `section`, `subject_id` are denormalised from the TSA for query convenience (same pattern as `HomeworkTable`).
- **`section` column** — section-scoped plans match `SyllabusProgressTable.section`. A teacher teaching Section A and B has separate plans.
- **`school_id` in indexes** — multi-tenant safety; every table in the system carries `school_id`.
- **`deleted_at` soft delete** — prevents accidental data loss; all queries filter `WHERE deleted_at IS NULL`.
- **`completed_at`** — typed timestamp for when the lesson was marked complete (drives analytics later).
- **`template_source_id`** — traces instantiation lineage without a separate join table.

### 4.2 `lesson_plan_templates` table

```sql
CREATE TABLE lesson_plan_templates (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id         UUID NOT NULL,
    teacher_id        UUID NOT NULL,                        -- FK app_users.id (author)
    assignment_id     UUID NOT NULL,                        -- FK teacher_subject_assignments.id (scope)
    subject_name      TEXT NOT NULL,
    title             TEXT NOT NULL,
    objectives        TEXT NOT NULL,                        -- JSON array
    activities        TEXT,                                 -- JSON array
    resources         TEXT,                                 -- JSON array
    assessment_method TEXT,
    duration_minutes  INTEGER NOT NULL DEFAULT 45,
    is_shared         BOOLEAN NOT NULL DEFAULT false,       -- shared with other teachers in school
    deleted_at        TIMESTAMP,
    created_at        TIMESTAMP NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_lesson_templates_teacher ON lesson_plan_templates(teacher_id, assignment_id);
CREATE INDEX idx_lesson_templates_school ON lesson_plan_templates(school_id, is_shared);
```

**Why a separate table instead of `is_template` flag:**

- Templates have no `planned_date`, no `status`, no `homework_id`, no `completed_at` — different shape, different lifecycle
- Avoids polluting the `lesson_plans` table with nullable columns that are meaningless for templates
- `is_shared` enables cross-teacher reuse within a school (future co-planning foundation)

### 4.3 `lesson_plan_attachments` table

```sql
CREATE TABLE lesson_plan_attachments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lesson_plan_id  UUID NOT NULL,                          -- FK lesson_plans.id (CASCADE)
    url             TEXT NOT NULL,
    filename        TEXT NOT NULL DEFAULT '',
    mime            TEXT NOT NULL DEFAULT '',
    size_bytes      BIGINT NOT NULL DEFAULT 0,
    uploaded_by     UUID,                                   -- FK app_users.id
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_lesson_attachments_plan ON lesson_plan_attachments(lesson_plan_id);
```

Follows the `HomeworkAttachmentsTable` pattern (`Tables.kt:1109-1116`) exactly.

---

## 5. API Contracts

### 5.1 Teacher endpoints (`TeacherLessonPlanRouting.kt`)

All endpoints require JWT auth + `requireTeacherContext()`. Writes are guarded by `requireOwnedAssignment(assignmentId)`.

```
GET    /api/v1/teacher/lesson-plans?assignmentId={uuid}&status={planned|completed|skipped}&from={YYYY-MM-DD}&to={YYYY-MM-DD}&unitId={uuid}
POST   /api/v1/teacher/lesson-plans                                    -- create
GET    /api/v1/teacher/lesson-plans/{id}                               -- single plan (must own assignment)
PATCH  /api/v1/teacher/lesson-plans/{id}                               -- update fields
DELETE /api/v1/teacher/lesson-plans/{id}                               -- soft delete
POST   /api/v1/teacher/lesson-plans/{id}/complete                      -- marks completed + upserts SyllabusProgressTable
POST   /api/v1/teacher/lesson-plans/{id}/skip                          -- marks skipped (no syllabus update)
GET    /api/v1/teacher/lesson-plans/calendar?assignmentId={uuid}&month={YYYY-MM}
```

### 5.2 Template endpoints

```
GET    /api/v1/teacher/lesson-plan-templates?assignmentId={uuid}       -- list own + shared templates
POST   /api/v1/teacher/lesson-plan-templates                           -- save template
DELETE /api/v1/teacher/lesson-plan-templates/{id}                      -- soft delete template
POST   /api/v1/teacher/lesson-plans/from-template/{templateId}         -- instantiate (assignmentId + plannedDate in body)
```

### 5.3 Admin review endpoint (`SchoolLessonPlanRouting.kt`)

Requires JWT auth + `requireSchoolContext()`. Read-only.

```
GET    /api/v1/school/lesson-plans?teacher_id={uuid}&class_id={uuid}&from={YYYY-MM-DD}&to={YYYY-MM-DD}&subject={text}
```

### 5.4 Request/Response DTOs (server-side `Lp*` prefix, mirroring shared models)

**Create/Update:**
```kotlin
@Serializable
data class LpCreateRequest(
    @SerialName("assignment_id") val assignmentId: String,
    @SerialName("curriculum_unit_id") val curriculumUnitId: String? = null,
    val title: String,
    val objectives: List<String> = emptyList(),
    val activities: List<LpActivityDto> = emptyList(),
    val resources: List<String> = emptyList(),
    @SerialName("assessment_method") val assessmentMethod: String? = null,
    @SerialName("duration_minutes") val durationMinutes: Int = 45,
    @SerialName("homework_id") val homeworkId: String? = null,
    @SerialName("planned_date") val plannedDate: String? = null,  // YYYY-MM-DD
)

@Serializable
data class LpActivityDto(
    val activity: String,
    @SerialName("duration_min") val durationMin: Int = 15,
)

@Serializable
data class LpUpdateRequest(
    @SerialName("curriculum_unit_id") val curriculumUnitId: String? = null,
    val title: String? = null,
    val objectives: List<String>? = null,
    val activities: List<LpActivityDto>? = null,
    val resources: List<String>? = null,
    @SerialName("assessment_method") val assessmentMethod: String? = null,
    @SerialName("duration_minutes") val durationMinutes: Int? = null,
    @SerialName("homework_id") val homeworkId: String? = null,
    @SerialName("planned_date") val plannedDate: String? = null,
)
```

**Response:**
```kotlin
@Serializable
data class LpPlanDto(
    val id: String,
    @SerialName("assignment_id") val assignmentId: String,
    @SerialName("class_id") val classId: String,
    val section: String = "",
    @SerialName("subject_name") val subjectName: String,
    @SerialName("curriculum_unit_id") val curriculumUnitId: String? = null,
    @SerialName("curriculum_unit_title") val curriculumUnitTitle: String? = null,
    val title: String,
    val objectives: List<String> = emptyList(),
    val activities: List<LpActivityDto> = emptyList(),
    val resources: List<String> = emptyList(),
    @SerialName("assessment_method") val assessmentMethod: String? = null,
    @SerialName("duration_minutes") val durationMinutes: Int = 45,
    @SerialName("homework_id") val homeworkId: String? = null,
    @SerialName("planned_date") val plannedDate: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    val status: String = "planned",
    @SerialName("template_source_id") val templateSourceId: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class LpListResponse(
    val success: Boolean = true,
    val data: List<LpPlanDto> = emptyList(),
)

@Serializable
data class LpSingleResponse(
    val success: Boolean = true,
    val data: LpPlanDto,
)

@Serializable
data class LpCalendarResponse(
    val success: Boolean = true,
    val data: LpCalendarDto,
)

@Serializable
data class LpCalendarDto(
    val month: String,                                    // YYYY-MM
    val days: List<LpCalendarDayDto> = emptyList(),
)

@Serializable
data class LpCalendarDayDto(
    val date: String,                                     // YYYY-MM-DD
    val plans: List<LpPlanDto> = emptyList(),
)
```

**Template DTOs:**
```kotlin
@Serializable
data class LpTemplateDto(
    val id: String,
    @SerialName("assignment_id") val assignmentId: String,
    @SerialName("subject_name") val subjectName: String,
    val title: String,
    val objectives: List<String> = emptyList(),
    val activities: List<LpActivityDto> = emptyList(),
    val resources: List<String> = emptyList(),
    @SerialName("assessment_method") val assessmentMethod: String? = null,
    @SerialName("duration_minutes") val durationMinutes: Int = 45,
    @SerialName("is_shared") val isShared: Boolean = false,
)

@Serializable
data class LpTemplateListResponse(
    val success: Boolean = true,
    val data: List<LpTemplateDto> = emptyList(),
)

@Serializable
data class LpInstantiateRequest(
    @SerialName("assignment_id") val assignmentId: String,
    @SerialName("planned_date") val plannedDate: String,  // YYYY-MM-DD
)
```

### 5.5 Authorization Rules

| Endpoint | Guard |
|---|---|
| All teacher endpoints | `requireTeacherContext()` → JWT teacher context |
| Create/Update/Delete/Complete/Skip | `requireOwnedAssignment(ctx, assignmentId)` — 403 if teacher doesn't own the TSA |
| GET single / calendar | Verify `lesson_plans.assignment_id` belongs to a TSA owned by the teacher |
| Admin review | `requireSchoolContext()` → JWT school admin context; auto-scoped to `school_id` |
| Template list | Own templates + `is_shared=true` templates within same `school_id` |
| Template instantiate | `requireOwnedAssignment(ctx, assignmentId)` on the target assignment |

### 5.6 Complete endpoint behaviour (`POST /{id}/complete`)

1. Verify teacher owns the assignment (via `requireOwnedAssignment`)
2. Update `lesson_plans.status = 'completed'`, `completed_at = now()`
3. If `curriculum_unit_id` is set:
   - Upsert `SyllabusProgressTable`: `isCovered = true`, `coveredOn = today`, `coveredBy = ctx.userId`
   - Keyed on `(unitId, section, assignmentId)` — same unique index as the one-tap toggle
   - Section comes from the owned assignment (authoritative, never client-supplied — X-1)
4. Return the updated `LpPlanDto`

### 5.7 Skip endpoint behaviour (`POST /{id}/skip`)

1. Verify teacher owns the assignment
2. Update `lesson_plans.status = 'skipped'`
3. **Does NOT touch `SyllabusProgressTable`** — skipped ≠ covered
4. Return the updated `LpPlanDto`

---

## 6. Shared KMP Layer (DTOs + Repository)

### 6.1 `TeacherModels.kt` additions

Add `LessonPlanDto`, `LessonPlanListResponse`, `LessonPlanSingleResponse`, `LessonCalendarResponse`, `LessonCalendarDto`, `LessonCalendarDayDto`, `LessonActivityDto`, `CreateLessonPlanRequest`, `UpdateLessonPlanRequest`, `LessonTemplateDto`, `LessonTemplateListResponse`, `InstantiateFromTemplateRequest` — mirroring the server `Lp*` DTOs field-for-field (same pattern as `SyllabusLoadResponse` / `HomeworkListResponse` etc.).

### 6.2 `TeacherRepository.kt` additions

```kotlin
// Lesson plans
suspend fun listLessonPlans(
    token: String, assignmentId: String, status: String? = null,
    from: String? = null, to: String? = null, unitId: String? = null,
): NetworkResult<LessonPlanListResponse>

suspend fun getLessonPlan(token: String, planId: String): NetworkResult<LessonPlanSingleResponse>
suspend fun createLessonPlan(token: String, request: CreateLessonPlanRequest): NetworkResult<LessonPlanSingleResponse>
suspend fun updateLessonPlan(token: String, planId: String, request: UpdateLessonPlanRequest): NetworkResult<LessonPlanSingleResponse>
suspend fun deleteLessonPlan(token: String, planId: String): NetworkResult<ApiResponse<Unit>>
suspend fun completeLessonPlan(token: String, planId: String): NetworkResult<LessonPlanSingleResponse>
suspend fun skipLessonPlan(token: String, planId: String): NetworkResult<LessonPlanSingleResponse>
suspend fun getLessonCalendar(token: String, assignmentId: String, month: String): NetworkResult<LessonCalendarResponse>

// Templates
suspend fun listLessonTemplates(token: String, assignmentId: String): NetworkResult<LessonTemplateListResponse>
suspend fun saveLessonTemplate(token: String, request: SaveLessonTemplateRequest): NetworkResult<LessonTemplateDto>
suspend fun deleteLessonTemplate(token: String, templateId: String): NetworkResult<ApiResponse<Unit>>
suspend fun instantiateLessonFromTemplate(token: String, templateId: String, request: InstantiateFromTemplateRequest): NetworkResult<LessonPlanSingleResponse>
```

### 6.3 `TeacherApi.kt` additions

Ktor `@Resource` route definitions for all the above endpoints, following the existing pattern (e.g. `TeacherSyllabusApi`, `TeacherHomeworkApi`).

---

## 7. Today Tab Integration (P1)

### 7.1 `ResolvedPeriodDto` enhancement

Add an optional field to `ResolvedPeriodDto` (`TeacherModels.kt:50-70`):

```kotlin
@SerialName("lesson_plan_id") val lessonPlanId: String? = null,   // non-null when a plan exists for this date+assignment
@SerialName("lesson_plan_status") val lessonPlanStatus: String? = null, // planned | completed | skipped
```

### 7.2 `TeacherDayRouting.kt` change

In the period resolution loop, for each period with an `assignmentId`, query `lesson_plans WHERE assignment_id = ? AND planned_date = ? AND deleted_at IS NULL`. If found, populate `lessonPlanId` + `lessonPlanStatus`. This is a single batched query (collect all assignmentIds for the day, one `IN` query), not N+1.

### 7.3 UI impact

The Today tab period card shows a lesson plan chip: 📋 (planned), ✅ (completed), ⏭️ (skipped). Tapping the chip deep-links to the lesson plan editor.

---

## 8. Acceptance Criteria

- [ ] Teacher creates lesson plans with objectives, activities, resources
- [ ] Lesson plans are scoped to an owned assignment (403 if not owned)
- [ ] Lesson plans link to curriculum units
- [ ] Completing a lesson upserts `SyllabusProgressTable` (isCovered=true, coveredOn=today, coveredBy=teacher)
- [ ] Skipping a lesson does NOT update syllabus progress
- [ ] Homework linked to lesson plan
- [ ] Teacher can soft-delete a lesson plan
- [ ] Teacher can edit a lesson plan (GET single + PATCH)
- [ ] Admin can review lesson plans (school-scoped, filterable)
- [ ] Calendar view shows planned lessons per day for a month
- [ ] Templates can be saved, listed, and instantiated into new plans
- [ ] All queries filter `deleted_at IS NULL`
- [ ] Today tab shows lesson plan chip per period (P1)
- [ ] Attachments can be added to lesson plans (P1)

---

## 9. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 0.5 day | DB migration (`migration_025_lesson_planning.sql`), Exposed tables (`LessonPlansTable`, `LessonPlanTemplatesTable`, `LessonPlanAttachmentsTable`) |
| 2 | 1.5 days | `TeacherLessonPlanRouting.kt` — all teacher endpoints + `SchoolLessonPlanRouting.kt` admin endpoint; `requireOwnedAssignment` guards; syllabus progress integration on complete |
| 3 | 1 day | Shared KMP layer: DTOs in `TeacherModels.kt`, `TeacherRepository` methods, `TeacherApi` route definitions, `TeacherRepositoryImpl` |
| 4 | 2 days | Client UI: `LessonPlanEditorScreen.kt` (create/edit), `LessonCalendarScreen.kt` (month view), `LessonTemplatePickerScreen.kt` (template list + instantiate) |
| 5 | 0.5 day | Today tab integration: `ResolvedPeriodDto` fields + `TeacherDayRouting.kt` batched query + UI chip |
| 6 | 1 day | Tests: server unit tests (create/complete/skip/delete/template), client ViewModel tests |

**Total: ~6.5 days**

---

## 10. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `docs/db/migration_025_lesson_planning.sql` | New | DDL for `lesson_plans`, `lesson_plan_templates`, `lesson_plan_attachments` |
| `server/.../db/Tables.kt` | Add | `LessonPlansTable`, `LessonPlanTemplatesTable`, `LessonPlanAttachmentsTable` objects |
| `server/.../feature/teacher/TeacherLessonPlanRouting.kt` | New | All teacher lesson plan endpoints (list, CRUD, complete, skip, calendar) |
| `server/.../feature/teacher/TeacherLessonTemplateRouting.kt` | New | Template list, save, delete, instantiate endpoints |
| `server/.../feature/school/SchoolLessonPlanRouting.kt` | New | Admin review endpoint (read-only, school-scoped) |
| `server/.../feature/teacher/TeacherRouting.kt` | Edit | Register `teacherLessonPlanRouting()` + `teacherLessonTemplateRouting()` in `teacherRouting()` |
| `server/.../feature/school/SchoolRouting.kt` | Edit | Register `schoolLessonPlanRouting()` in `schoolRouting()` |
| `server/.../feature/teacher/TeacherDayRouting.kt` | Edit | Batched lesson plan lookup per period for Today tab integration |
| `shared/.../teacher/domain/model/TeacherModels.kt` | Add | `LessonPlan*` DTOs, `LessonTemplate*` DTOs, `InstantiateFromTemplateRequest`, `ResolvedPeriodDto` fields |
| `shared/.../teacher/domain/repository/TeacherRepository.kt` | Add | Lesson plan + template repository methods |
| `shared/.../teacher/data/TeacherRepositoryImpl.kt` | Add | Implementations for all new repository methods |
| `shared/.../teacher/data/remote/TeacherApi.kt` | Add | Ktor `@Resource` route definitions for lesson plan endpoints |
| `composeApp/.../ui/v2/screens/teacher/LessonPlanEditorScreen.kt` | New | Create/edit lesson plan UI |
| `composeApp/.../ui/v2/screens/teacher/LessonCalendarScreen.kt` | New | Month calendar view with per-day lesson plans |
| `composeApp/.../ui/v2/screens/teacher/LessonTemplatePickerScreen.kt` | New | Template list + instantiate flow |
| `composeApp/.../ui/v2/screens/teacher/TodayScreen.kt` (or equivalent) | Edit | Add lesson plan chip per period card |
| `composeApp/.../ui/v2/viewmodel/LessonPlanViewModel.kt` | New | MVI state + intents for lesson plan editor |
| `composeApp/.../ui/v2/viewmodel/LessonCalendarViewModel.kt` | New | MVI state for calendar view |
