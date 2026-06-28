# Student Digital Portfolio — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** `VIDYA_PASSPORT_SPEC.md`
> **Source:** `COMPETITIVE_GAP_ANALYSIS.md` — emerging trend
> **Template:** `_SPEC_TEMPLATE.md` v1 (25 mandatory + 6 optional sections)

---

## 1. Feature Overview

### What

Digital portfolio for students showcasing their academic work, projects, art, certifications, and achievements over their academic journey. A curated, reflective collection that grows with the student and can be shared for admissions or scholarships.

### Why — Product Rationale

Students need a way to showcase their work beyond grades. A digital portfolio provides a holistic view of a student's academic journey — academic work, creative projects, sports achievements, community service, and certifications. This is a **medium-priority feature** (Phase 4, effort L) that provides competitive differentiation and supports student admissions.

### What Stands Out (Competitive Moat)

From `COMPETITIVE_GAP_ANALYSIS.md`: student portfolio as emerging trend.

The key moat is **integration with Vidya Passport** — badges, certificates, and achievements auto-populate the portfolio, creating a comprehensive digital record without manual effort. Combined with **teacher endorsements** and **reflection notes**, the portfolio becomes a rich, verified record of student growth.

### Goals

- Student/teacher uploads work samples (essays, projects, art, presentations, certificates)
- Categorized by type: academic, creative, sports, community, certification
- Reflection notes per item (what I learned, what I'd improve)
- Timeline view: portfolio items across years
- Teacher endorsement: teacher can endorse/comment on portfolio items
- Shareable: generate portfolio PDF or shareable link for admissions
- Integration with `VIDYA_PASSPORT_SPEC.md` (badges + achievements auto-added)

### Non-goals

- [ ] Peer review/comments on portfolio items (teacher endorsement only)
- [ ] Portfolio templates (free-form portfolio)
- [ ] Video portfolio items (future enhancement, storage cost)
- [ ] Public portfolio search/discovery (shareable link only, not indexed)
- [ ] Portfolio analytics (views, engagement) (future enhancement)
- [ ] Multi-student portfolio comparison (individual portfolios only)
- [ ] Portfolio migration between schools (future enhancement)
- [ ] AI-powered portfolio curation (future enhancement)

### Dependencies

- `VIDYA_PASSPORT_SPEC.md` — badges, certificates, achievements (auto-population)
- `STUDENT_APP_SPEC.md` — student app (student uploads)
- `SchoolMediaTable` — media storage infrastructure
- `HomeworkSubmissionsTable` — student work (can be promoted to portfolio)
- `StudentsTable` — student info
- `AppUsersTable` — teacher and parent accounts
- `AcademicYearsTable` — academic year info
- Supabase Storage — file storage

### Related Modules

- `server/.../feature/portfolio/` — new portfolio module
- `server/.../feature/passport/` — existing Vidya Passport module (auto-population)
- `server/.../feature/homework/` — existing homework module (promotion)
- `composeApp/.../ui/v2/screens/parent/` — parent UI (portfolio timeline, upload)
- `composeApp/.../ui/v2/screens/teacher/` — teacher UI (endorsement, promote homework)

---

## 2. Current System Assessment

### Existing Code

- `VIDYA_PASSPORT_SPEC.md` — badges, certificates, milestones
- `SchoolMediaTable` — media storage infrastructure
- `HomeworkSubmissionsTable` — student work (can be promoted to portfolio)
- No portfolio system exists
- `COMPETITIVE_GAP_ANALYSIS.md`: student portfolio as emerging trend

### Existing Database

- `SchoolMediaTable` — media storage (images, files)
- `HomeworkSubmissionsTable` — homework submissions (student work)
- `StudentsTable` — student info
- `AppUsersTable` — teacher and parent accounts
- `AcademicYearsTable` — academic year info
- Vidya Passport tables — badges, certificates, achievements
- No portfolio items table

### Existing APIs

- Media API (existing) — file upload to Supabase Storage
- Homework API (existing) — homework submissions
- Vidya Passport API (existing) — badges, certificates
- Student API (existing) — student info
- No portfolio API

### Existing UI

- Parent app (existing) — no portfolio section
- Teacher dashboard (existing) — no portfolio review
- Student profile (existing) — no portfolio tab
- No portfolio UI

### Existing Services

- `MediaService` — file upload and storage
- `HomeworkService` — homework management
- `PassportService` — Vidya Passport (badges, certificates)
- No portfolio service

### Existing Documentation

- `COMPETITIVE_GAP_ANALYSIS.md` — student portfolio as emerging trend
- `VIDYA_PASSPORT_SPEC.md` — badge/achievement system
- `STUDENT_APP_SPEC.md` — student app (student uploads)

### Technical Debt

| # | Gap | Details |
|---|---|---|
| TD-1 | No portfolio items table | No `portfolio_items` table for storing portfolio entries |
| TD-2 | No portfolio service | No service for CRUD, endorsement, share, export |
| TD-3 | No portfolio UI | No parent portfolio timeline, teacher endorsement screen |
| TD-4 | No PDF export | No portfolio PDF generation |
| TD-5 | No shareable links | No time-limited public links for external viewing |

### Gaps

| # | Gap | Impact | Severity |
|---|---|---|---|
| G1 | No portfolio storage | Students can't showcase work | **High** |
| G2 | No reflection notes | Students can't reflect on learning | **Medium** |
| G3 | No teacher endorsement | Items lack verification | **Medium** |
| G4 | No auto-population | Manual entry for all achievements | **Medium** |
| G5 | No export/share | Can't share for admissions | **High** |
| G6 | No homework promotion | Good homework can't become portfolio item | **Low** |

---

## 3. Functional Requirements

### FR-001
| Field | Value |
|---|---|
| **Title** | Upload Portfolio Item |
| **Description** | Upload portfolio item: title, type, description, file (image/PDF/doc), date, subject. |
| **Priority** | High |
| **User Roles** | Parent (on behalf of student), Student (via student app) |
| **Acceptance notes** | Parent/student uploads file to Supabase Storage, creates portfolio item with title, category, description, file_url, file_type, subject, academic_year. Item saved to `portfolio_items`. |

### FR-002
| Field | Value |
|---|---|
| **Title** | Portfolio Categories |
| **Description** | Categories: academic, creative (art/music), sports, community, certification, project. |
| **Priority** | High |
| **User Roles** | System |
| **Acceptance notes** | Fixed categories: academic, creative, sports, community, certification, project. Each item must have one category. Categories used for filtering and timeline grouping. |

### FR-003
| Field | Value |
|---|---|
| **Title** | Reflection Note |
| **Description** | Reflection note: student writes what they learned and would improve. |
| **Priority** | Medium |
| **User Roles** | Parent (on behalf of student), Student |
| **Acceptance notes** | Free-text reflection note per portfolio item. "What I learned" and "What I'd improve" fields. Optional but encouraged. Stored in `reflection` column. |

### FR-004
| Field | Value |
|---|---|
| **Title** | Teacher Endorsement |
| **Description** | Teacher endorsement: teacher can endorse items with comment. |
| **Priority** | Medium |
| **User Roles** | Teacher |
| **Acceptance notes** | Teacher views student portfolio items, can endorse with comment. Sets `teacher_endorsed = true`, `teacher_endorsement = comment`, `endorsed_by = teacher_id`, `endorsed_at = now()`. Endorsement visible in portfolio and PDF export. |

### FR-005
| Field | Value |
|---|---|
| **Title** | Timeline View |
| **Description** | Timeline view: items chronologically across academic years. |
| **Priority** | High |
| **User Roles** | Parent, Teacher |
| **Acceptance notes** | Portfolio items displayed in timeline, grouped by academic year. Chronological order (newest first). Filter by category. Visual timeline with year separators. |

### FR-006
| Field | Value |
|---|---|
| **Title** | Auto-populate Achievements |
| **Description** | Auto-populate: badges, certificates, achievements from `VIDYA_PASSPORT_SPEC.md`. |
| **Priority** | Medium |
| **User Roles** | System |
| **Acceptance notes** | When student earns badge/certificate/achievement in Vidya Passport, auto-create portfolio item with category="certification" or appropriate category, item_type="achievement". Linked to Passport record. Cannot be deleted by parent/student. |

### FR-007
| Field | Value |
|---|---|
| **Title** | Export Portfolio PDF |
| **Description** | Export: generate portfolio PDF for admissions/scholarships. |
| **Priority** | High |
| **User Roles** | Parent |
| **Acceptance notes** | Generate PDF with: student info, portfolio items (title, category, description, reflection, endorsement), academic year grouping. PDF stored temporarily in Supabase Storage. Download link provided. |

### FR-008
| Field | Value |
|---|---|
| **Title** | Shareable Link |
| **Description** | Shareable link: time-limited public link for external viewing. |
| **Priority** | Medium |
| **User Roles** | Parent |
| **Acceptance notes** | Parent generates shareable link for portfolio. `share_token` generated, `share_expires_at` set (default 30 days). Public endpoint `/api/v1/public/portfolio/{shareToken}` shows portfolio without auth. Link can be revoked. |

### FR-009
| Field | Value |
|---|---|
| **Title** | Promote Homework to Portfolio |
| **Description** | Promote homework: teacher can promote a homework submission to portfolio. |
| **Priority** | Low |
| **User Roles** | Teacher |
| **Acceptance notes** | Teacher selects homework submission, provides title and category, promotes to portfolio. Creates portfolio item with file_url from submission. Original submission retained. |

### Non-Functional Requirements

| ID | Requirement |
|---|---|
| NFR-1 | Upload: < 5 seconds (file upload to Supabase Storage + DB insert) |
| NFR-2 | Timeline view: < 2 seconds (load all items for student) |
| NFR-3 | PDF export: < 10 seconds (generate PDF with all items) |
| NFR-4 | Shareable link: < 1 second (generate token) |
| NFR-5 | File size: max 10MB per file (images, PDFs, docs) |
| NFR-6 | File types: pdf, jpg, png, doc, docx, ppt, pptx |
| NFR-7 | Shareable link: default 30 days expiry (configurable 1-90 days) |
| NFR-8 | Auto-population: < 5 seconds (badge/achievement → portfolio item) |

---

## 4. User Stories

### Parent (on behalf of student)
- [ ] Upload my child's work sample to portfolio
- [ ] Add a reflection note to a portfolio item
- [ ] View my child's portfolio timeline
- [ ] Export my child's portfolio as PDF
- [ ] Generate a shareable link for admissions
- [ ] Revoke a shareable link
- [ ] Delete a portfolio item (non-achievement items only)

### Teacher
- [ ] View a student's portfolio
- [ ] Endorse a portfolio item with a comment
- [ ] Promote a homework submission to portfolio
- [ ] View class-wise portfolio overview

### Student (via student app)
- [ ] Upload my work to portfolio
- [ ] Write a reflection note
- [ ] View my portfolio timeline
- [ ] See my achievements auto-populated

### System
- [ ] Auto-populate portfolio with Vidya Passport achievements
- [ ] Generate PDF export
- [ ] Generate shareable links with expiry
- [ ] Clean up expired shareable links

---

## 5. Business Rules

### BR-001
**Rule:** Parent manages portfolio for younger students; student manages own portfolio via student app.
**Enforcement:** Parent endpoints require parent role + JWT auth + parent-student relationship. Student endpoints (via student app) require student auth. Both can upload, edit, delete (non-achievement items).

### BR-002
**Rule:** Achievement items (auto-populated) cannot be deleted.
**Enforcement:** Items with `item_type = 'achievement'` are system-generated. Delete endpoint checks item_type and rejects deletion for achievement items.

### BR-003
**Rule:** Teacher endorsement is permanent (cannot be removed).
**Enforcement:** Once `teacher_endorsed = true`, endorsement cannot be removed. Teacher can update endorsement comment but not remove endorsement.

### BR-004
**Rule:** Shareable links have expiry (default 30 days, max 90 days).
**Enforcement:** `share_expires_at` set on link creation. Public endpoint checks expiry. Expired links return 410 Gone. Parent can revoke (set share_token = null).

### BR-005
**Rule:** File size limit 10MB, allowed types only.
**Enforcement:** Upload validates file size and type. Rejected if > 10MB or type not in allowed list (pdf, jpg, png, doc, docx, ppt, pptx).

### BR-006
**Rule:** Portfolio is school-scoped.
**Enforcement:** All items have `school_id`. Queries filtered by school_id. No cross-school portfolio visibility (except via shareable link).

### BR-007
**Rule:** Only one shareable link per portfolio item.
**Enforcement:** `share_token` is per-item. Generating new link replaces old token. One active link per item.

### BR-008
**Rule:** PDF export includes all portfolio items.
**Enforcement:** PDF includes all items for student, grouped by academic year, with endorsements. Generated on demand, not stored permanently.

### BR-009
**Rule:** Teacher can only endorse items for students in their class.
**Enforcement:** Endorse endpoint checks teacher-student class relationship. Teacher must be assigned to student's class.

### BR-010
**Rule:** Reflection notes are optional but encouraged.
**Enforcement:** `reflection` column is nullable. UI encourages reflection but doesn't require it. Items without reflection are still valid.

---

## 6. Database Design

### 6.1 Entity Relationship Summary

One new table: `portfolio_items` (portfolio entries with file, category, reflection, endorsement, share). References existing `students`, `app_users`, `academic_years`, and `schools` tables. Integrates with Vidya Passport tables for auto-population.

### 6.2 New Tables

#### `portfolio_items` table

```sql
CREATE TABLE portfolio_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    student_id      UUID NOT NULL,
    title           TEXT NOT NULL,
    description     TEXT,
    category        VARCHAR(32) NOT NULL,          -- academic | creative | sports | community | certification | project
    item_type       VARCHAR(16) NOT NULL,          -- file | image | link | achievement
    file_url        TEXT,                          -- Supabase Storage URL
    file_type       VARCHAR(16),                   -- pdf | image | doc | video
    subject         TEXT,
    academic_year_id UUID,
    reflection      TEXT,                          -- student's reflection note
    teacher_endorsed BOOLEAN NOT NULL DEFAULT false,
    teacher_endorsement TEXT,                      -- teacher's endorsement comment
    endorsed_by     UUID,
    endorsed_at     TIMESTAMP,
    is_public       BOOLEAN NOT NULL DEFAULT false,
    share_token     TEXT,                          -- for shareable link
    share_expires_at TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_portfolio_student ON portfolio_items(student_id, created_at DESC);
CREATE INDEX idx_portfolio_category ON portfolio_items(student_id, category);
```

### 6.3 Modified Tables

N/A — no existing tables modified. Integration with Vidya Passport tables is read-only (achievements read and auto-populated as portfolio items).

### 6.4 Indexes

- `idx_portfolio_student` on `portfolio_items(student_id, created_at DESC)` — timeline view
- `idx_portfolio_category` on `portfolio_items(student_id, category)` — category filter
- `portfolio_items(share_token)` — for public link lookup (recommended, unique when not null)

### 6.5 Constraints

- `portfolio_items.title` — NOT NULL
- `portfolio_items.category` — NOT NULL, values: academic | creative | sports | community | certification | project
- `portfolio_items.item_type` — NOT NULL, values: file | image | link | achievement
- `portfolio_items.teacher_endorsed` — NOT NULL, default false
- `portfolio_items.is_public` — NOT NULL, default false
- `portfolio_items.created_at` — NOT NULL, default now()
- `portfolio_items.updated_at` — NOT NULL, default now()

### 6.6 Foreign Keys

- `portfolio_items.school_id` → `schools.id` (implicit)
- `portfolio_items.student_id` → `students.id` (implicit)
- `portfolio_items.academic_year_id` → `academic_years.id` (implicit, nullable)
- `portfolio_items.endorsed_by` → `app_users.id` (implicit, nullable)

### 6.7 Soft Delete Strategy

N/A — portfolio items are hard-deleted by parent/student (except achievement items which cannot be deleted). No soft delete needed.

### 6.8 Audit Fields

- `portfolio_items.created_at` — when item was created
- `portfolio_items.updated_at` — when item was last updated
- `portfolio_items.endorsed_by` — which teacher endorsed
- `portfolio_items.endorsed_at` — when endorsement was given
- `portfolio_items.share_expires_at` — when shareable link expires

### 6.9 Migration Notes

Migration: `docs/db/migration_089_student_portfolio.sql`
- CREATE 1 table: `portfolio_items`
- CREATE 2 indexes: `idx_portfolio_student`, `idx_portfolio_category`
- No data migration (new feature)

### 6.10 Exposed Mappings

```kotlin
object PortfolioItemsTable : UUIDTable("portfolio_items", "id") {
    val schoolId          = uuid("school_id")
    val studentId         = uuid("student_id")
    val title             = text("title")
    val description       = text("description").nullable()
    val category          = varchar("category", 32)
    val itemType          = varchar("item_type", 16)
    val fileUrl           = text("file_url").nullable()
    val fileType          = varchar("file_type", 16).nullable()
    val subject           = text("subject").nullable()
    val academicYearId    = uuid("academic_year_id").nullable()
    val reflection        = text("reflection").nullable()
    val teacherEndorsed   = bool("teacher_endorsed").default(false)
    val teacherEndorsement= text("teacher_endorsement").nullable()
    val endorsedBy        = uuid("endorsed_by").nullable()
    val endorsedAt        = timestamp("endorsed_at").nullable()
    val isPublic          = bool("is_public").default(false)
    val shareToken        = text("share_token").nullable()
    val shareExpiresAt    = timestamp("share_expires_at").nullable()
    val createdAt         = timestamp("created_at")
    val updatedAt         = timestamp("updated_at")

    init {
        index("idx_portfolio_student", studentId, createdAt)
        index("idx_portfolio_category", studentId, category)
    }
}
```

Register in `DatabaseFactory.allTables`.

### 6.11 Seed Data

N/A — portfolio items are user-created. No seed data needed.

---

## 7. State Machines

### Portfolio Item Lifecycle State Machine

```
not_created ──upload──> created ──teacher_endorses──> endorsed ──(terminal)
  │                        │
  │                        ├──edit──> updated
  │                        │
  │                        ├──share──> shared ──link_expires──> shared_expired
  │                        │              │
  │                        │              └──revoke──> shared_revoked
  │                        │
  │                        └──delete──> deleted (terminal, except achievements)
  │
  └──created (achievement) ──(cannot delete)──> endorsed/unendorsed (terminal)
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `not_created` | Parent/student uploads | `created` | Item saved to DB |
| `created` | Teacher endorses | `endorsed` | teacher_endorsed = true |
| `created`/`endorsed` | Parent/student edits | `updated` | Update title, description, reflection |
| `created`/`endorsed` | Parent generates share link | `shared` | share_token set, share_expires_at set |
| `shared` | Link expires | `shared_expired` | share_expires_at < now() |
| `shared` | Parent revokes link | `shared_revoked` | share_token = null |
| `created`/`endorsed` | Parent/student deletes | `deleted` | item_type != 'achievement' |
| `created` (achievement) | Delete attempted | `created` | Rejected — achievements cannot be deleted |

### Shareable Link State Machine

```
no_link ──parent_generates──> active ──expires──> expired
  │                              │
  │                              └──parent_revokes──> revoked
  │
  └──expired/revoked ──parent_regenerates──> active
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `no_link` | Parent generates share link | `active` | share_token generated, expiry set |
| `active` | Expiry time reached | `expired` | share_expires_at < now() |
| `active` | Parent revokes | `revoked` | share_token = null |
| `expired`/`revoked` | Parent regenerates | `active` | New share_token, new expiry |

### Endorsement State Machine

```
not_endorsed ──teacher_endorses──> endorsed ──(terminal, cannot un-endorse)
  │
  └──endorsed ──teacher_updates_comment──> endorsed (comment updated)
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `not_endorsed` | Teacher endorses with comment | `endorsed` | teacher_endorsed = true, endorsement saved |
| `endorsed` | Teacher updates comment | `endorsed` | teacher_endorsement updated, endorsed_at unchanged |
| `endorsed` | Un-endorse attempted | `endorsed` | Rejected — endorsement is permanent |

---

## 8. Backend Architecture

### 8.1 Component Overview

`PortfolioService` handles CRUD, endorsement, share, export, and auto-population. Integrates with Vidya Passport for achievement auto-population, homework service for promotion, and Supabase Storage for file storage. `ShareLinkCleanupJob` cleans up expired shareable links.

### 8.2 Design Principles

1. **Student-centric** — portfolio belongs to the student, managed by parent/student
2. **Auto-populated** — achievements from Vidya Passport auto-added
3. **Teacher-verified** — teacher endorsements add credibility
4. **Shareable** — time-limited public links for external viewing
5. **Exportable** — PDF export for admissions/scholarships
6. **School-scoped** — all data school-specific

### 8.3 Core Types

#### PortfolioService

```kotlin
class PortfolioService {
    suspend fun uploadItem(request: UploadRequest): PortfolioItemDto
    suspend fun updateItem(id: UUID, request: UpdateRequest): PortfolioItemDto
    suspend fun deleteItem(id: UUID): Unit
    suspend fun getItem(id: UUID): PortfolioItemDto
    suspend fun getStudentPortfolio(studentId: UUID, category: String?): List<PortfolioItemDto>
    suspend fun endorseItem(id: UUID, teacherId: UUID, comment: String): PortfolioItemDto
    suspend fun promoteHomework(submissionId: UUID, title: String, category: String): PortfolioItemDto
    suspend fun shareItem(id: UUID, expiresInDays: Int): ShareLinkDto
    suspend fun revokeShare(id: UUID): Unit
    suspend fun getPublicPortfolio(shareToken: String): PublicPortfolioDto
    suspend fun exportPdf(studentId: UUID): String  // returns PDF download URL
    suspend fun autoPopulateAchievement(studentId: UUID, achievement: AchievementDto): PortfolioItemDto
}
```

#### PDF Export

```kotlin
class PortfolioPdfGenerator {
    suspend fun generate(student: StudentDto, items: List<PortfolioItemDto>): String  // returns Supabase Storage URL
}
```

### 8.4 Repositories

- `PortfolioItemRepository` — CRUD for `portfolio_items`

### 8.5 Mappers

- `PortfolioItemMapper` — maps `portfolio_items` rows to DTOs

### 8.6 Permission Checks

- Upload: parent (own child) or student (own portfolio via student app)
- Update/delete: parent (own child) or student (own portfolio), not for achievement items
- View portfolio: parent (own child), teacher (own class), student (own)
- Endorse: teacher (own class student)
- Promote homework: teacher (own class student)
- Share/revoke: parent (own child)
- Public portfolio: no auth (share token based)
- Export PDF: parent (own child)

### 8.7 Background Jobs

- **Share Link Cleanup Job** — daily at 3 AM
  1. `SELECT * FROM portfolio_items WHERE share_expires_at < now() AND share_token IS NOT NULL`
  2. For each: set `share_token = null`, `share_expires_at = null`, `is_public = false`
  3. Return count of expired links cleaned

### 8.8 Domain Events

- `PortfolioItemUploaded` — emitted when item is uploaded
- `PortfolioItemEndorsed` — emitted when teacher endorses
- `PortfolioItemShared` — emitted when shareable link generated
- `PortfolioItemRevoked` — emitted when shareable link revoked
- `PortfolioPdfExported` — emitted when PDF is generated
- `AchievementAutoPopulated` — emitted when achievement is auto-added to portfolio
- `HomeworkPromoted` — emitted when homework is promoted to portfolio

### 8.9 Caching

- Student portfolio: cached locally (5-minute TTL)
- Public portfolio (share link): cached (1-minute TTL, share may expire)
- PDF export: not cached (generated on demand)

### 8.10 Transactions

- Upload: single transaction (insert portfolio item)
- Endorse: single transaction (update item with endorsement)
- Share: single transaction (update item with share token)
- Delete: single transaction (delete item, check achievement flag)
- Auto-populate: single transaction (insert achievement portfolio item)

### 8.11 Rate Limiting

- Upload: 20 per hour per parent (multiple children)
- Endorse: 50 per hour per teacher
- Share: 10 per hour per parent
- PDF export: 5 per hour per parent (PDF generation is expensive)
- Public portfolio: 100 per hour per share token

### 8.12 Configuration

- `PORTFOLIO_ENABLED` — default `false`; enable/disable feature
- `PORTFOLIO_MAX_FILE_SIZE_MB` — default `10`; max file size
- `PORTFOLIO_ALLOWED_FILE_TYPES` — default `pdf,jpg,png,doc,docx,ppt,pptx`
- `PORTFOLIO_SHARE_DEFAULT_DAYS` — default `30`; default share link expiry
- `PORTFOLIO_SHARE_MAX_DAYS` — default `90`; max share link expiry
- `PORTFOLIO_AUTO_POPULATE_ENABLED` — default `true`; auto-populate from Vidya Passport
- `PORTFOLIO_PDF_EXPORT_ENABLED` — default `true`; enable/disable PDF export

---

## 9. API Contracts

### 9.1 Parent Endpoints

```
GET /api/v1/parent/portfolio/{childId}
  → 200: { items: [PortfolioItemDto] }

POST /api/v1/parent/portfolio/{childId}
  Body: { title, category, item_type, description, file_url, file_type, subject, reflection }
  → 201: PortfolioItemDto

PATCH /api/v1/parent/portfolio/items/{id}
  Body: { title?, description?, reflection?, subject? }
  → 200: PortfolioItemDto

DELETE /api/v1/parent/portfolio/items/{id}
  → 204: No Content
  → 403: Cannot delete achievement items

POST /api/v1/parent/portfolio/items/{id}/share
  Body: { expires_in_days: 30 }
  → 200: { share_url: String, expires_at: String }

DELETE /api/v1/parent/portfolio/items/{id}/share
  → 204: No Content (revoked)

GET /api/v1/parent/portfolio/{childId}/export
  → 200: { pdf_url: String }
```

### 9.2 Teacher Endpoints

```
POST /api/v1/teacher/portfolio/items/{id}/endorse
  Body: { comment: String }
  → 200: PortfolioItemDto

POST /api/v1/teacher/portfolio/promote-homework
  Body: { submission_id, title, category }
  → 201: PortfolioItemDto

GET /api/v1/teacher/portfolio/student/{studentId}
  → 200: { items: [PortfolioItemDto] }
```

### 9.3 Public Endpoint

```
GET /api/v1/public/portfolio/{shareToken}
  → 200: PublicPortfolioDto (no auth required)
  → 410: Link expired
```

### 9.4 DTO Models

All `@Serializable`, wrapped in `ApiResponse<T>` pattern.

```kotlin
@Serializable data class PortfolioItemDto(
    val id: String,
    val studentId: String,
    val studentName: String,
    val title: String,
    val description: String?,
    val category: String,        // academic | creative | sports | community | certification | project
    val itemType: String,        // file | image | link | achievement
    val fileUrl: String?,
    val fileType: String?,
    val subject: String?,
    val academicYearId: String?,
    val academicYearName: String?,
    val reflection: String?,
    val teacherEndorsed: Boolean,
    val teacherEndorsement: String?,
    val endorsedByName: String?,
    val endorsedAt: String?,
    val isPublic: Boolean,
    val shareUrl: String?,
    val shareExpiresAt: String?,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable data class ShareLinkDto(
    val shareUrl: String,
    val expiresAt: String,
)

@Serializable data class PublicPortfolioDto(
    val studentName: String,
    val schoolName: String,
    val className: String?,
    val items: List<PortfolioItemDto>,
)

@Serializable data class UploadRequest(
    val title: String,
    val category: String,
    val itemType: String,
    val description: String?,
    val fileUrl: String?,
    val fileType: String?,
    val subject: String?,
    val reflection: String?,
)

@Serializable data class UpdateRequest(
    val title: String?,
    val description: String?,
    val reflection: String?,
    val subject: String?,
)
```

---

## 10. Frontend Architecture

### 10.1 Screens

| Screen | Platform | Role | Description |
|---|---|---|---|
| `PortfolioScreen` | Compose | Parent | Portfolio timeline, upload, manage items |
| `PortfolioUploadScreen` | Compose | Parent | Upload new portfolio item |
| `PortfolioDetailScreen` | Compose | Parent | View/edit item detail, reflection, share |
| `PortfolioReviewScreen` | Compose | Teacher | View student portfolio, endorse items |
| `PublicPortfolioScreen` | Compose/Web | Public | View shared portfolio (no auth) |

### 10.2 Navigation

- Parent: Home → Portfolio → Timeline → Upload / Detail / Share / Export
- Teacher: Student → Portfolio → Review → Endorse
- Public: Shareable link → Public Portfolio

### 10.3 UX Flows

#### Parent: Upload Portfolio Item
1. Parent opens Portfolio → taps "Add Item"
2. Enters title, selects category, item type
3. Uploads file (image/PDF/doc) or enters link
4. Enters description and reflection note
5. Selects subject (optional)
6. Taps "Save" → item created, appears in timeline

#### Parent: Share Portfolio Item
1. Parent opens item detail → taps "Share"
2. Selects expiry (default 30 days, max 90)
3. Shareable link generated → copy or share
4. Link can be revoked from item detail

#### Parent: Export PDF
1. Parent opens Portfolio → taps "Export PDF"
2. PDF generated with all items, grouped by academic year
3. Download link provided → parent downloads or shares

#### Teacher: Endorse Item
1. Teacher opens student portfolio
2. Views items in timeline
3. Taps "Endorse" on an item
4. Enters endorsement comment
5. Endorsement saved, visible in portfolio and PDF

### 10.4 State Management

```kotlin
data class PortfolioState(
    val items: List<PortfolioItemDto>,
    val filteredItems: List<PortfolioItemDto>,
    val selectedCategory: String?,
    val isLoading: Boolean,
    val error: String?,
)

data class PortfolioUploadState(
    val title: String,
    val selectedCategory: String,
    val selectedItemType: String,
    val fileUrl: String?,
    val description: String,
    val reflection: String,
    val subject: String,
    val isUploading: Boolean,
    val error: String?,
)

data class PortfolioDetailState(
    val item: PortfolioItemDto?,
    val isSharing: Boolean,
    val shareUrl: String?,
    val isExporting: Boolean,
    val error: String?,
)
```

### 10.5 Offline Support

- Portfolio timeline: cached locally (5-minute TTL)
- Upload: available offline (queued, synced when online)
- PDF export: requires online (server-side generation)
- Share: requires online (server generates token)

### 10.6 Loading States

- Uploading: "Uploading portfolio item..."
- Loading timeline: "Loading portfolio..."
- Endorsing: "Saving endorsement..."
- Sharing: "Generating shareable link..."
- Exporting PDF: "Generating portfolio PDF..." (may take 10 seconds)

### 10.7 Error Handling (UI)

- Upload failed: "Failed to upload item. Please try again."
- File too large: "File exceeds 10MB limit."
- Invalid file type: "This file type is not supported."
- Cannot delete achievement: "Achievement items cannot be deleted."
- Share link expired: "This share link has expired."
- PDF export failed: "Failed to generate PDF. Please try again."
- Permission denied: "You can only manage your own child's portfolio."

### 10.8 Component Integration Guidelines

| Rule | Description |
|---|---|
| **R1** | Timeline: chronological, grouped by academic year, newest first |
| **R2** | Category filter: chips (All, Academic, Creative, Sports, Community, Certification, Project) |
| **R3** | Item card: title, category badge, thumbnail (if image), endorsement badge |
| **R4** | Upload form: title, category dropdown, file picker, description, reflection |
| **R5** | Reflection: two fields — "What I learned" and "What I'd improve" |
| **R6** | Endorsement badge: green checkmark with teacher name, visible on card |
| **R7** | Share button: generates link, copy button, expiry indicator |
| **R8** | Export button: prominent, generates PDF download |
| **R9** | Achievement items: special badge, cannot delete, auto-populated label |
| **R10** | Public portfolio: clean, read-only view, no edit/share buttons |

---

## 11. Shared Module Changes (KMP)

### 11.1 DTOs

All DTOs defined in section 9.4, placed in `shared/.../portfolio/domain/model/PortfolioModels.kt`.

### 11.2 Domain Models

```kotlin
data class PortfolioItem(
    val id: String,
    val schoolId: String,
    val studentId: String,
    val title: String,
    val description: String?,
    val category: PortfolioCategory,
    val itemType: PortfolioItemType,
    val fileUrl: String?,
    val fileType: String?,
    val subject: String?,
    val academicYearId: String?,
    val reflection: String?,
    val teacherEndorsed: Boolean,
    val teacherEndorsement: String?,
    val endorsedBy: String?,
    val endorsedAt: Instant?,
    val isPublic: Boolean,
    val shareToken: String?,
    val shareExpiresAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

enum class PortfolioCategory { ACADEMIC, CREATIVE, SPORTS, COMMUNITY, CERTIFICATION, PROJECT }
enum class PortfolioItemType { FILE, IMAGE, LINK, ACHIEVEMENT }
```

### 11.3 Repository Interfaces

```kotlin
interface PortfolioRepository {
    suspend fun getPortfolio(token: String, childId: String, category: String?): NetworkResult<List<PortfolioItemDto>>
    suspend fun uploadItem(token: String, childId: String, request: UploadRequest): NetworkResult<PortfolioItemDto>
    suspend fun updateItem(token: String, id: String, request: UpdateRequest): NetworkResult<PortfolioItemDto>
    suspend fun deleteItem(token: String, id: String): NetworkResult<Unit>
    suspend fun endorseItem(token: String, id: String, comment: String): NetworkResult<PortfolioItemDto>
    suspend fun promoteHomework(token: String, submissionId: String, title: String, category: String): NetworkResult<PortfolioItemDto>
    suspend fun shareItem(token: String, id: String, expiresInDays: Int): NetworkResult<ShareLinkDto>
    suspend fun revokeShare(token: String, id: String): NetworkResult<Unit>
    suspend fun exportPdf(token: String, childId: String): NetworkResult<String>
    suspend fun getPublicPortfolio(shareToken: String): NetworkResult<PublicPortfolioDto>
}
```

### 11.4 UseCases

- `GetPortfolioUseCase`
- `UploadPortfolioItemUseCase`
- `UpdatePortfolioItemUseCase`
- `DeletePortfolioItemUseCase`
- `EndorsePortfolioItemUseCase`
- `PromoteHomeworkUseCase`
- `SharePortfolioItemUseCase`
- `RevokeShareUseCase`
- `ExportPortfolioPdfUseCase`
- `GetPublicPortfolioUseCase`

### 11.5 Validation

- `title`: non-empty, max 200 characters
- `category`: one of academic, creative, sports, community, certification, project
- `item_type`: one of file, image, link, achievement
- `file_url`: required if item_type is file or image
- `file_type`: pdf, jpg, png, doc, docx, ppt, pptx
- `description`: max 2000 characters
- `reflection`: max 2000 characters
- `subject`: max 100 characters
- `expires_in_days`: 1-90

### 11.6 Serialization

Standard Kotlinx serialization. Enums serialized as lowercase strings. Timestamps as ISO strings.

### 11.7 Network APIs

Ktor `@Resource` route definitions in `PortfolioApi.kt`:
- GET `/api/v1/parent/portfolio/{childId}`
- POST `/api/v1/parent/portfolio/{childId}`
- PATCH `/api/v1/parent/portfolio/items/{id}`
- DELETE `/api/v1/parent/portfolio/items/{id}`
- POST `/api/v1/parent/portfolio/items/{id}/share`
- DELETE `/api/v1/parent/portfolio/items/{id}/share`
- GET `/api/v1/parent/portfolio/{childId}/export`
- POST `/api/v1/teacher/portfolio/items/{id}/endorse`
- POST `/api/v1/teacher/portfolio/promote-homework`
- GET `/api/v1/teacher/portfolio/student/{studentId}`
- GET `/api/v1/public/portfolio/{shareToken}`

### 11.8 Database Models (Local Cache)

- Portfolio items: cached locally (5-minute TTL)
- Public portfolio: cached locally (1-minute TTL)
- PDF URL: cached locally (1-hour TTL, PDF is immutable once generated)

---

## 12. Permissions Matrix

| Action | Super Admin | School Admin | Counselor | Teacher | Parent | Student | Public |
|---|---|---|---|---|---|---|---|
| Upload item | N/A | ✅ (own school) | ❌ | ❌ | ✅ (own child) | ✅ (own) | ❌ |
| Update item | N/A | ✅ (own school) | ❌ | ❌ | ✅ (own child) | ✅ (own) | ❌ |
| Delete item | N/A | ✅ (own school) | ❌ | ❌ | ✅ (own child, non-achievement) | ✅ (own, non-achievement) | ❌ |
| View portfolio | ✅ (all) | ✅ (own school) | ✅ (assigned) | ✅ (own class) | ✅ (own child) | ✅ (own) | ❌ |
| Endorse item | N/A | ✅ (own school) | ❌ | ✅ (own class) | ❌ | ❌ | ❌ |
| Promote homework | N/A | ✅ (own school) | ❌ | ✅ (own class) | ❌ | ❌ | ❌ |
| Share item | N/A | N/A | ❌ | ❌ | ✅ (own child) | ✅ (own) | ❌ |
| Revoke share | N/A | N/A | ❌ | ❌ | ✅ (own child) | ✅ (own) | ❌ |
| Export PDF | N/A | ✅ (own school) | ❌ | ❌ | ✅ (own child) | ✅ (own) | ❌ |
| View public portfolio | N/A | N/A | N/A | N/A | N/A | N/A | ✅ (valid share token) |

---

## 13. Notifications

### Portfolio Notifications

| Trigger | Recipient | Channel | Template |
|---|---|---|---|
| Item endorsed by teacher | Parent | FCM (normal) + in-app | "Teacher {teacher_name} endorsed '{item_title}' in your child's portfolio." |
| Achievement auto-populated | Parent | In-app | "New achievement '{achievement_name}' added to portfolio." |
| Homework promoted to portfolio | Parent | In-app | "Teacher promoted '{homework_title}' to portfolio." |
| Share link expiring (3 days before) | Parent | FCM (low) | "Shareable link for '{item_title}' expires in 3 days." |

### Notification Integration

Uses `SmartNotificationService` with "normal" priority for endorsements and auto-population. Uses "low" priority for share link expiry warnings.

---

## 14. Background Jobs

### Share Link Cleanup Job

| Property | Value |
|---|---|
| **Name** | `ShareLinkCleanupJob` |
| **Schedule** | Daily at 3 AM |
| **Duration** | < 10 seconds |
| **Retry** | None |

#### Job Flow

1. `SELECT * FROM portfolio_items WHERE share_expires_at < now() AND share_token IS NOT NULL`
2. For each: set `share_token = null`, `share_expires_at = null`, `is_public = false`
3. Return count of expired links cleaned

### Share Link Expiry Warning Job

| Property | Value |
|---|---|
| **Name** | `ShareLinkExpiryWarningJob` |
| **Schedule** | Daily at 8 AM |
| **Duration** | < 10 seconds |

#### Job Flow

1. `SELECT * FROM portfolio_items WHERE share_expires_at BETWEEN now() AND now() + 3 days AND share_token IS NOT NULL`
2. For each: send FCM notification to parent "Share link expires in 3 days"
3. Return count of warnings sent

---

## 15. Integrations

### Internal Integrations

| System | Integration Point | Direction | Protocol | Error Handling |
|---|---|---|---|---|
| `VIDYA_PASSPORT_SPEC.md` | Achievements auto-population | Listen (event) | Domain event | Log error, item not auto-populated |
| `HomeworkSubmissionsTable` | Homework promotion | Read | Direct DB | Required for promotion |
| `SchoolMediaTable` | File storage | Call | Supabase Storage | Log error, upload fails |
| `StudentsTable` | Student info | Read | Direct DB | Required |
| `AcademicYearsTable` | Academic year | Read | Direct DB | Required for grouping |
| `Notify.kt` | Notification dispatch | Call | Direct call | Log error, continue |

### External Integrations

| System | Integration Point | Direction | Protocol | Error Handling |
|---|---|---|---|---|
| Supabase Storage | File storage + PDF | Call | HTTP API | Log error, upload/export fails |
| FCM | Push notifications | Call | HTTP API | Log error, continue |

### Integration Patterns

- **Achievement auto-population:** Listen for `BadgeAwarded`/`CertificateEarned` events → `portfolioService.autoPopulateAchievement()` → insert portfolio item
- **Homework promotion:** Teacher selects submission → `portfolioService.promoteHomework()` → copy file_url from submission → create portfolio item
- **PDF export:** `PortfolioPdfGenerator.generate()` → create PDF → upload to Supabase Storage → return download URL
- **Shareable link:** Generate UUID token → set share_token + share_expires_at → public endpoint serves portfolio

---

## 16. Security

### Authentication

- Parent endpoints: parent role + JWT auth + parent-student relationship
- Teacher endpoints: teacher role + JWT auth + teacher-class relationship
- Student endpoints (via student app): student auth
- Public endpoint: no auth (share token based)

### Authorization

- Upload/update/delete: parent (own child) or student (own), not for achievements
- Endorse: teacher (own class student)
- Promote homework: teacher (own class student)
- Share/revoke: parent (own child) or student (own)
- Export PDF: parent (own child) or student (own)
- Public portfolio: valid share token required

### Data Protection

- Portfolio data is school-scoped
- Parent sees only own child's portfolio
- Teacher sees only own class students' portfolios
- Public portfolio shows only items with active share token
- File URLs are Supabase Storage signed URLs (time-limited access)

### Input Validation

- `title`: non-empty, max 200 characters
- `category`: must be one of the 6 valid categories
- `item_type`: must be one of file, image, link, achievement
- `file_url`: required for file/image types
- `file_type`: must be in allowed list
- `description`: max 2000 characters
- `reflection`: max 2000 characters
- `expires_in_days`: 1-90

### Rate Limiting

- Upload: 20 per hour per parent
- Endorse: 50 per hour per teacher
- Share: 10 per hour per parent
- PDF export: 5 per hour per parent
- Public portfolio: 100 per hour per share token

### Audit Logging

- Item uploaded: user ID, student ID, title, category, timestamp
- Item updated: user ID, item ID, changes, timestamp
- Item deleted: user ID, item ID, timestamp
- Item endorsed: teacher ID, item ID, comment, timestamp
- Share link generated: user ID, item ID, expiry, timestamp
- Share link revoked: user ID, item ID, timestamp
- PDF exported: user ID, student ID, timestamp
- Achievement auto-populated: student ID, achievement ID, timestamp
- Homework promoted: teacher ID, submission ID, item ID, timestamp

### PII Handling

- Student name in portfolio items (necessary for identification)
- Reflection notes may contain personal student thoughts
- Teacher endorsements may contain personal assessments
- Public portfolio shows student name, school, class — visible to anyone with link
- PDF export contains student info — parent controls sharing

### Multi-tenant Isolation

- All portfolio items have `school_id` — school-scoped
- Queries filtered by school_id
- No cross-school portfolio visibility (except via shareable link)
- Shareable link is item-specific, not school-specific

---

## 17. Performance & Scalability

### Expected Scale

- Portfolio items: ~20-50 per student over academic journey
- Students: ~200-500 per school
- File uploads: ~5-10 per day per school
- PDF exports: ~1-5 per day per school
- Shareable links: ~1-5 active per student

### Query Optimization

- Student portfolio: `idx_portfolio_student(student_id, created_at DESC)` — filtered by student, sorted by date
- Category filter: `idx_portfolio_category(student_id, category)` — filtered by student and category
- Share token lookup: `portfolio_items(share_token)` — for public endpoint (recommended index)

### Indexing Strategy

- `portfolio_items(student_id, created_at DESC)` — timeline view
- `portfolio_items(student_id, category)` — category filter
- `portfolio_items(share_token)` — public link lookup (recommended, unique when not null)

### Caching Strategy

- Student portfolio: cached locally (5-minute TTL)
- Public portfolio: cached (1-minute TTL, share may expire)
- PDF URL: cached (1-hour TTL, PDF is immutable)

### Pagination

- Portfolio timeline: 20 per page (infinite scroll)
- Public portfolio: all items in single response (max ~50 items)

### Connection Pooling

Uses existing HikariCP connection pool. No additional pooling needed.

### Async Processing

- Upload: synchronous (parent waits for confirmation)
- Endorse: synchronous (teacher waits)
- PDF export: synchronous (parent waits, < 10 seconds)
- Share link cleanup: async (background job)
- Auto-population: async (event-driven)

### Scalability Concerns

- File storage: ~10 uploads/day × ~5MB = ~50MB/day. Supabase Storage handles this.
- DB storage: ~10 items/day × ~2KB = ~20KB/day. Negligible.
- PDF generation: ~5/day, server-side. < 10 seconds each. Negligible load.
- Share link cleanup: daily job, few items. Negligible.

---

## 18. Edge Cases

| # | Scenario | Expected Behavior |
|---|---|---|
| EC-1 | Parent tries to delete achievement item | Return 403. "Achievement items cannot be deleted." |
| EC-2 | Teacher tries to un-endorse | Return 403. "Endorsement cannot be removed." |
| EC-3 | Share link expired | Public endpoint returns 410. "Link expired." |
| EC-4 | Share link revoked | Public endpoint returns 404. "Link not found." |
| EC-5 | File upload exceeds 10MB | Return 400. "File exceeds 10MB limit." |
| EC-6 | Invalid file type | Return 400. "This file type is not supported." |
| EC-7 | PDF export with no items | Generate PDF with student info only, "No portfolio items." |
| EC-8 | Auto-population for achievement already in portfolio | Check for existing item with same achievement reference. Skip if exists (idempotent). |
| EC-9 | Promote homework that doesn't exist | Return 404. "Homework submission not found." |
| EC-10 | Parent accesses another parent's child portfolio | Return 403. "You can only view your own child's portfolio." |
| EC-11 | Public access without share token | Return 404. "Invalid link." |
| EC-12 | Student graduates/leaves school | Portfolio retained. Parent can still access and export. |

---

## 19. Error Handling

### Error Response Format

Standard `ApiResponse` error format.

### Error Codes

| Code | HTTP Status | Description | User Message |
|---|---|---|---|
| `ITEM_NOT_FOUND` | 404 | Portfolio item not found | "Portfolio item not found." |
| `CANNOT_DELETE_ACHIEVEMENT` | 403 | Achievement items cannot be deleted | "Achievement items cannot be deleted." |
| `ENDORSEMENT_PERMANENT` | 403 | Cannot remove endorsement | "Endorsement cannot be removed." |
| `SHARE_LINK_EXPIRED` | 410 | Share link has expired | "This share link has expired." |
| `SHARE_LINK_INVALID` | 404 | Share link not found | "Invalid share link." |
| `FILE_TOO_LARGE` | 400 | File exceeds size limit | "File exceeds 10MB limit." |
| `INVALID_FILE_TYPE` | 400 | File type not supported | "This file type is not supported." |
| `NOT_AUTHORIZED` | 403 | Role not authorized | "You are not authorized to perform this action." |
| `NOT_PARENT_OF_STUDENT` | 403 | Parent not linked to student | "You can only manage your own child's portfolio." |
| `NOT_TEACHER_OF_CLASS` | 403 | Teacher not assigned to student's class | "You can only endorse items for students in your class." |
| `PDF_GENERATION_FAILED` | 500 | PDF generation failed | "Failed to generate PDF. Please try again." |
| `SUBMISSION_NOT_FOUND` | 404 | Homework submission not found | "Homework submission not found." |

### Error Handling Strategy

- **File upload failure:** Return error. Parent retries.
- **PDF generation failure:** Return 500. Parent retries.
- **Share link expired:** Return 410. Parent generates new link.
- **Achievement deletion:** Return 403. Inform parent.
- **Endorsement removal:** Return 403. Inform teacher.

### Retry Strategy

- Upload: parent retries (network issue may resolve)
- PDF export: parent retries (generation may succeed)
- Share: parent retries (token generation is fast)

### Fallback Behavior

- Supabase Storage unavailable: upload fails, parent informed
- PDF generator unavailable: export fails, parent informed
- Vidya Passport integration unavailable: auto-population skipped, manual upload still works

---

## 20. Analytics & Reporting

### Analytics Dashboard Data

| Metric | Source | Derivation |
|---|---|---|
| Total portfolio items | `portfolio_items` | Count per school |
| Items by category | `portfolio_items` | Count per category |
| Endorsed items | `portfolio_items` | Count where teacher_endorsed = true |
| Endorsement rate | `portfolio_items` | endorsed / total |
| Shareable links active | `portfolio_items` | Count where share_token IS NOT NULL AND share_expires_at > now() |
| PDF exports | API logs | Count of export requests |
| Auto-populated items | `portfolio_items` | Count where item_type = 'achievement' |
| Homework promotions | `portfolio_items` | Count of promoted items |

### Export Capabilities

- Portfolio PDF (per student) — for admissions/scholarships
- Portfolio summary report (CSV) — student, item count, category breakdown, endorsement rate

### Report Types

| Report | Format | Frequency | Recipient |
|---|---|---|---|
| Portfolio usage | JSON (API) | Monthly | Product Team |
| Endorsement activity | JSON (API) | Monthly | School Admin |
| Export activity | JSON (API) | Monthly | Product Team |

---

## 21. Testing Strategy

### Unit Tests

- `PortfolioService.uploadItem()` — validation, insert, file URL
- `PortfolioService.endorseItem()` — endorsement, permanence check
- `PortfolioService.deleteItem()` — achievement deletion prevention
- `PortfolioService.shareItem()` — token generation, expiry
- `PortfolioService.autoPopulateAchievement()` — idempotent check, insert
- `PortfolioService.promoteHomework()` — submission lookup, item creation
- State transitions: created → endorsed, shared → expired

### Integration Tests

- Full flow: upload → endorse → share → public access → export PDF
- Auto-population: badge awarded → portfolio item created → idempotent on re-trigger
- Homework promotion: submission exists → promote → verify item created
- Share link: generate → access → expire → cleanup job
- Permission checks: parent-only upload, teacher-only endorse, public-only via token
- Multi-tenant: verify school-scoped queries

### E2E Tests

- Parent uploads item → teacher endorses → parent exports PDF → PDF contains item + endorsement
- Parent generates share link → external viewer accesses → sees portfolio
- Achievement earned → auto-populated → parent sees in timeline
- Teacher promotes homework → parent sees in portfolio

### Performance Tests

- Timeline load: < 2 seconds for 50 items
- PDF export: < 10 seconds for 50 items
- File upload: < 5 seconds for 10MB file
- Share link generation: < 1 second

### Test Data

- 5 test students with varying portfolio histories
- 1 test teacher with assigned class
- 1 test parent with child
- Test files (image, PDF, doc)
- Mock Vidya Passport events
- Test homework submissions

### Test Environment

- Test database with portfolio table
- Test Supabase Storage bucket
- Mock Vidya Passport service
- Mock homework service
- Test JWT tokens for all roles
- Test share tokens

---

## 22. Acceptance Criteria

- [ ] Portfolio items uploaded with file, category, reflection
- [ ] Timeline view shows items across academic years
- [ ] Teacher can endorse items with comment
- [ ] Auto-populated with achievements from Vidya Passport
- [ ] Export as PDF for admissions
- [ ] Shareable link with expiry
- [ ] Teacher can promote homework to portfolio
- [ ] Achievement items cannot be deleted
- [ ] Endorsement is permanent (cannot be removed)
- [ ] Share link can be revoked by parent
- [ ] School-scoped (no cross-school access)
- [ ] File size and type validation enforced

---

## 23. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration `migration_089_student_portfolio.sql`, Exposed table, register in `DatabaseFactory` |
| 2 | 2 days | `PortfolioService` (CRUD, endorsement, share, export, auto-populate, promote homework) |
| 3 | 1 day | Achievement auto-population integration (event listener for Vidya Passport events) |
| 4 | 1 day | API endpoints (parent, teacher, public) |
| 5 | 4 days | Client UI: `PortfolioScreen`, `PortfolioUploadScreen`, `PortfolioDetailScreen`, `PortfolioReviewScreen`, `PublicPortfolioScreen` |
| 6 | 1 day | Tests: unit, integration, E2E |

### Pre-Implementation Checklist

- [ ] Verify `VIDYA_PASSPORT_SPEC.md` is implemented (for auto-population)
- [ ] Verify `SchoolMediaTable` and Supabase Storage are available
- [ ] Verify `HomeworkSubmissionsTable` is available (for promotion)
- [ ] Verify `AcademicYearsTable` is available
- [ ] Verify PDF generation library is available
- [ ] Verify notification infrastructure (FCM, SmartNotificationService)

---

## 24. File-Level Impact Analysis

### Server

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | `PortfolioItemsTable` |
| `server/.../db/DatabaseFactory.kt` | Modify | Register new table in `allTables` |
| `server/.../feature/portfolio/PortfolioService.kt` | **New** | Core service |
| `server/.../feature/portfolio/PortfolioRouting.kt` | **New** | API endpoints |
| `server/.../feature/portfolio/PortfolioPdfGenerator.kt` | **New** | PDF export |
| `server/.../feature/portfolio/ShareLinkCleanupJob.kt` | **New** | Cleanup job |
| `docs/db/migration_089_student_portfolio.sql` | **New** | DDL |

### Shared (KMP)

| File | Change Type | Description |
|---|---|---|
| `shared/.../portfolio/domain/model/PortfolioModels.kt` | **New** | DTOs, domain models, enums |
| `shared/.../portfolio/domain/repository/PortfolioRepository.kt` | **New** | Repository interface |
| `shared/.../portfolio/data/remote/PortfolioApi.kt` | **New** | HTTP API definitions |

### Client (Compose)

| File | Change Type | Description |
|---|---|---|
| `composeApp/.../ui/v2/screens/parent/PortfolioScreen.kt` | **New** | Portfolio timeline |
| `composeApp/.../ui/v2/screens/parent/PortfolioUploadScreen.kt` | **New** | Upload form |
| `composeApp/.../ui/v2/screens/parent/PortfolioDetailScreen.kt` | **New** | Item detail, share, export |
| `composeApp/.../ui/v2/screens/teacher/PortfolioReviewScreen.kt` | **New** | Teacher endorsement |
| `composeApp/.../ui/v2/screens/public/PublicPortfolioScreen.kt` | **New** | Public portfolio view |

---

## 25. Future Enhancements

| # | Enhancement | Priority | Effort | Notes |
|---|---|---|---|---|
| F-1 | Video portfolio items | Medium | M | Support video uploads (storage cost consideration) |
| F-2 | Portfolio analytics | Low | S | Views, engagement metrics for shared links |
| F-3 | AI-powered curation | Medium | L | AI suggests best items to showcase |
| F-4 | Portfolio templates | Low | M | Pre-defined portfolio structures by field/interest |
| F-5 | Peer review | Low | M | Students comment on each other's portfolio items |
| F-6 | Portfolio migration | Medium | M | Transfer portfolio when student changes schools |
| F-7 | Multi-language portfolio | Low | S | Portfolio items in multiple languages |
| F-8 | Portfolio search | Low | S | Search within portfolio by keyword |
| F-9 | Batch export | Low | S | Export multiple students' portfolios at once |
| F-10 | Portfolio versioning | Low | M | Track changes to portfolio items over time |

---

## Appendix A: Sequence Diagrams

### A.1 Upload Portfolio Item

```
Parent (app)        Server              DB              Supabase
  │                    │                  │                │
  │  Upload file       │                  │                │
  │  ───────────────>  │                  │                │
  │                    │──store file─────────────────────>│
  │                    │←──file_url──────────────────────│
  │                    │                  │                │
  │  POST /portfolio   │                  │                │
  │  {title, category, │                  │                │
  │   file_url, ...}   │                  │                │
  │  ───────────────>  │                  │                │
  │                    │──validate────────│                │
  │                    │──insert item────>│                │
  │                    │←──success───────│                │
  │  ←──201: ItemDto──│                  │                │
  │                    │                  │                │
```

### A.2 Teacher Endorsement

```
Teacher (app)       Server              DB
  │                    │                  │
  │  POST /items/{id}/ │                  │
  │  endorse           │                  │
  │  {comment}         │                  │
  │  ───────────────>  │                  │
  │                    │──verify teacher──│
  │                    │  -class relation │
  │                    │←──ok─────────────│
  │                    │                  │
  │                    │──update item────>│
  │                    │  endorsed=true   │
  │                    │  endorsement=... │
  │                    │  endorsed_by=... │
  │                    │  endorsed_at=now()│
  │                    │←──success───────│
  │  ←──200: ItemDto──│                  │
  │                    │                  │
  │  (notification     │                  │
  │   sent to parent)  │                  │
  │                    │                  │
```

### A.3 Share and Public Access

```
Parent (app)        Server              DB              External Viewer
  │                    │                  │                │
  │  POST /items/{id}/ │                  │                │
  │  share             │                  │                │
  │  {expires_in: 30}  │                  │                │
  │  ───────────────>  │                  │                │
  │                    │──generate token──│                │
  │                    │──update item────>│                │
  │                    │  share_token=... │                │
  │                    │  share_expires=..│                │
  │                    │←──success───────│                │
  │  ←──200: ShareUrl──│                  │                │
  │                    │                  │                │
  │  (parent shares    │                  │                │
  │   link externally) │                  │                │
  │                    │                  │                │
  │                    │      GET /public/portfolio/{token}│
  │                    │      <───────────────────────────│
  │                    │──lookup token───>│                │
  │                    │←──item found────│                │
  │                    │──check expiry────│                │
  │                    │  (not expired)   │                │
  │                    │      ──200: Portfolio──────────>│
  │                    │                  │                │
```

---

## Appendix B: Domain Model / ER Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                    portfolio_items (new)                              │
│  id (PK)                                                              │
│  school_id, student_id                                                │
│  title, description, category, item_type                              │
│  file_url, file_type, subject                                         │
│  academic_year_id (nullable)                                          │
│  reflection (student's note)                                          │
│  teacher_endorsed, teacher_endorsement                                │
│  endorsed_by, endorsed_at                                             │
│  is_public, share_token, share_expires_at                             │
│  created_at, updated_at                                               │
│  INDEX: (student_id, created_at DESC)                                 │
│  INDEX: (student_id, category)                                        │
└──────────────────────────────────────────────────────────────────────┘

Existing tables used:
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│ students         │  │ app_users        │  │ academic_years   │
│ (existing)       │  │ (existing)       │  │ (existing)       │
│ student info     │  │ teacher/parent   │  │ year info        │
└──────────────────┘  └──────────────────┘  └──────────────────┘
┌──────────────────┐  ┌──────────────────┐
│ school_media     │  │ homework_         │
│ (existing)       │  │ submissions       │
│ media storage    │  │ (existing)        │
└──────────────────┘  └──────────────────┘
┌──────────────────┐
│ passport tables  │
│ (existing)       │
│ badges, certs    │
└──────────────────┘
```

---

## Appendix C: Event Flow

### Domain Events

| Event | Emitter | Consumers | Payload | Side Effects |
|---|---|---|---|---|
| `PortfolioItemUploaded` | `PortfolioService.uploadItem()` | None (logged) | `studentId, itemId, category` | Audit log |
| `PortfolioItemEndorsed` | `PortfolioService.endorseItem()` | `NotificationService` | `studentId, itemId, teacherId, comment` | FCM to parent |
| `PortfolioItemShared` | `PortfolioService.shareItem()` | None (logged) | `studentId, itemId, shareToken, expiresAt` | Audit log |
| `PortfolioItemRevoked` | `PortfolioService.revokeShare()` | None (logged) | `studentId, itemId` | Audit log |
| `PortfolioPdfExported` | `PortfolioService.exportPdf()` | None (logged) | `studentId, pdfUrl` | Audit log |
| `AchievementAutoPopulated` | `PortfolioService.autoPopulateAchievement()` | `NotificationService` | `studentId, achievementId, itemId` | In-app to parent |
| `HomeworkPromoted` | `PortfolioService.promoteHomework()` | `NotificationService` | `studentId, submissionId, itemId` | In-app to parent |

### Event Delivery Guarantees

- Item uploaded: fire-and-forget logging
- Endorsed: synchronous, notification dispatch async
- Shared/revoked: fire-and-forget logging
- PDF exported: fire-and-forget logging
- Achievement auto-populated: event-driven (triggered by Passport event), notification async
- Homework promoted: synchronous, notification dispatch async

---

## Appendix D: Configuration

### Environment Variables

| Variable | Default | Description |
|---|---|---|
| `PORTFOLIO_ENABLED` | `false` | Enable/disable feature |
| `PORTFOLIO_MAX_FILE_SIZE_MB` | `10` | Max file size in MB |
| `PORTFOLIO_ALLOWED_FILE_TYPES` | `pdf,jpg,png,doc,docx,ppt,pptx` | Allowed file types |
| `PORTFOLIO_SHARE_DEFAULT_DAYS` | `30` | Default share link expiry |
| `PORTFOLIO_SHARE_MAX_DAYS` | `90` | Max share link expiry |
| `PORTFOLIO_AUTO_POPULATE_ENABLED` | `true` | Auto-populate from Vidya Passport |
| `PORTFOLIO_PDF_EXPORT_ENABLED` | `true` | Enable/disable PDF export |

### Feature Flags

| Flag | Default | Description |
|---|---|---|
| `PORTFOLIO_ENABLED` | `false` | Enable/disable portfolio |
| `PORTFOLIO_AUTO_POPULATE_ENABLED` | `true` | Auto-populate achievements |
| `PORTFOLIO_PDF_EXPORT_ENABLED` | `true` | PDF export |
| `PORTFOLIO_SHARE_ENABLED` | `true` | Shareable links |

### School-Level Settings

| Setting | Default | Description |
|---|---|---|
| `portfolio_enabled` | `false` | Per-school enable/disable |
| `portfolio_auto_populate_enabled` | `true` | Per-school auto-population |
| `portfolio_pdf_export_enabled` | `true` | Per-school PDF export |
| `portfolio_share_enabled` | `true` | Per-school shareable links |

---

## Appendix E: Migration & Rollback

### Migration: `migration_089_student_portfolio.sql`

```sql
-- Migration 089: Student Digital Portfolio
-- Creates portfolio_items table

BEGIN;

CREATE TABLE IF NOT EXISTS portfolio_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    student_id      UUID NOT NULL,
    title           TEXT NOT NULL,
    description     TEXT,
    category        VARCHAR(32) NOT NULL,
    item_type       VARCHAR(16) NOT NULL,
    file_url        TEXT,
    file_type       VARCHAR(16),
    subject         TEXT,
    academic_year_id UUID,
    reflection      TEXT,
    teacher_endorsed BOOLEAN NOT NULL DEFAULT false,
    teacher_endorsement TEXT,
    endorsed_by     UUID,
    endorsed_at     TIMESTAMP,
    is_public       BOOLEAN NOT NULL DEFAULT false,
    share_token     TEXT,
    share_expires_at TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_portfolio_student
    ON portfolio_items (student_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_portfolio_category
    ON portfolio_items (student_id, category);

COMMIT;
```

### Rollback: `migration_089_rollback.sql`

```sql
BEGIN;
DROP TABLE IF EXISTS portfolio_items;
COMMIT;
```

### Migration Validation

- Verify `portfolio_items` table created with indexes
- Run `SELECT count(*) FROM portfolio_items` — should be 0 (new feature)

---

## Appendix F: Observability

### Structured Logging

| Log Level | Event | Context Fields |
|---|---|---|
| INFO | Item uploaded | `studentId, itemId, category, itemType` |
| INFO | Item endorsed | `itemId, teacherId, endorsedAt` |
| INFO | Share link generated | `itemId, shareToken, expiresAt` |
| INFO | Share link revoked | `itemId` |
| INFO | Share link expired (cleanup) | `itemId` |
| INFO | PDF exported | `studentId, pdfUrl` |
| INFO | Achievement auto-populated | `studentId, achievementId, itemId` |
| INFO | Homework promoted | `studentId, submissionId, itemId` |
| WARN | File upload failed | `studentId, error` |
| WARN | PDF generation failed | `studentId, error` |
| WARN | Auto-population: item already exists | `studentId, achievementId` |
| ERROR | Supabase Storage error | `studentId, error` |

### Metrics

| Metric | Type | Labels | Description |
|---|---|---|---|
| `portfolio_items_total` | Counter | `school_id, category` | Total items uploaded |
| `portfolio_items_endorsed` | Counter | `school_id` | Total endorsements |
| `portfolio_share_links_active` | Gauge | `school_id` | Active shareable links |
| `portfolio_pdf_exports` | Counter | `school_id` | PDF exports generated |
| `portfolio_auto_populated` | Counter | `school_id` | Auto-populated achievements |
| `portfolio_homework_promoted` | Counter | `school_id` | Homework promotions |
| `portfolio_public_views` | Counter | `school_id` | Public portfolio views |
| `portfolio_upload_duration` | Histogram | `school_id` | Upload duration |
| `portfolio_pdf_duration` | Histogram | `school_id` | PDF generation duration |

### Health Checks

| Check | Endpoint | Description |
|---|---|---|
| Portfolio service | `/health/portfolio` | Verify service and DB accessible |
| Supabase Storage | `/health/storage` | Verify storage accessible |

### Alerts

| Alert | Condition | Severity | Notification |
|---|---|---|---|
| Supabase Storage unavailable | Health check failed | Critical | Email + SMS to dev team |
| PDF generation failures | > 5 failures in 1 hour | Warning | Email to dev team |
| High upload failures | > 20% failure rate | Warning | Email to dev team |
| Share link cleanup failed | Job error | Warning | Email to dev team |

### Dashboards

| Dashboard | Panels | Audience |
|---|---|---|
| Portfolio Overview | Total items, by category, endorsement rate | Product Team |
| Sharing Activity | Active links, public views, expiry warnings | Product Team |
| Export Activity | PDF exports, generation duration | DevOps Team |
| Auto-population | Achievements populated, success rate | Product Team |

### Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| File storage cost growth | Medium | Medium | File size limit (10MB). Monitor storage. |
| Inappropriate content upload | Medium | High | Teacher review. Endorsement adds verification. |
| Share link leaked | Low | Medium | Time-limited (30 days default). Parent can revoke. |
| PDF generation failure | Low | Low | Retry. Fallback: parent screenshots. |
| Auto-population failure | Low | Low | Manual upload still works. Log error. |
| Privacy breach via share link | Low | High | Share token is unguessable UUID. Time-limited. |
