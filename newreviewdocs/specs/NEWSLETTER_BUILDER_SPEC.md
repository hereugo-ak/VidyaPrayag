# Newsletter Builder — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-28
> **Prerequisites:** `WHATSAPP_INTEGRATION_SPEC.md`, `EMAIL_INTEGRATION_SPEC.md`
> **Source:** `DIFFERENTIATING_FEATURES.md` §4.3
> **Template:** `_SPEC_TEMPLATE.md` v1 (25 mandatory + 6 optional sections)

---

## 1. Feature Overview

Visual newsletter builder for schools to create and distribute branded newsletters to parents. Drag-and-drop content blocks, AI-assisted content generation, multi-channel distribution (email, WhatsApp, in-app).

### Goals

- Visual newsletter editor with content blocks (text, image, event, announcement, achievement)
- School branding (logo, colors, header/footer)
- AI-assisted content generation ("summarize this month's events")
- Multi-channel distribution: email (HTML), WhatsApp (text + image), in-app (card)
- Newsletter archive (past issues viewable)
- Read tracking (email opens, in-app views)

### Non-goals

- [ ] Parent-created newsletters (admin-only feature)
- [ ] Newsletter subscriptions by external audiences (parents only)
- [ ] Print/PDF export (future enhancement)
- [ ] Multi-school newsletter aggregation
- [ ] Newsletter analytics dashboard with engagement trends (future)

### Dependencies

- `WHATSAPP_INTEGRATION_SPEC.md` — WhatsApp distribution channel
- `EMAIL_INTEGRATION_SPEC.md` — Email distribution channel
- `SCHEDULED_ANNOUNCEMENTS_SPEC.md` — Scheduling pattern reuse
- `SupabaseStorage` — existing file storage for newsletter images
- `NotificationService` — existing notification infrastructure for in-app distribution
- `AnnouncementsTable` — source data for AI-assisted generation
- `CalendarEventsTable` — source data for AI-assisted generation (events)
- `AiService` — AI infrastructure for content generation

### Related Modules

- `server/.../feature/newsletter/` — new newsletter module
- `shared/.../feature/newsletter/` — shared DTOs and API
- `composeApp/.../ui/v2/screens/admin/` — admin newsletter editor UI
- `composeApp/.../ui/v2/screens/parent/` — parent newsletter view UI

---

## 2. Current System Assessment

### Existing Code

- No newsletter system exists in the codebase
- `DIFFERENTIATING_FEATURES.md` §4.3: Newsletter Builder, effort M
- `AnnouncementsTable` — individual announcements, not curated newsletters
- No newsletter tables in `Tables.kt`
- No newsletter-related routes in `Application.kt`

### Existing Database

- `AnnouncementsTable` — announcements for AI-assisted content generation source
- `CalendarEventsTable` — events for AI-assisted content generation source
- `SchoolsTable` — school records with branding info (logo, name, colors)
- `AppUsersTable` — user accounts for parent distribution
- `WhatsappLogsTable` — WhatsApp message logs
- `SupabaseStorage` — file storage for images

### Existing APIs

- Announcement CRUD (source data for AI generation)
- Calendar event CRUD (source data for AI generation)
- File upload via `MediaRouting.kt`
- Auth: OTP login, password login
- Notification delivery via `NotificationService`

### Existing UI

- School portal with admin tabs (`SchoolPortalV2.kt`)
- Parent portal with announcement/feature access
- Web admin dashboard (`website/src/app/admin/`)

### Existing Services

- `NotificationService` — multi-channel notifications (push, in-app, WhatsApp)
- `SupabaseStorage` — file storage with kind-based paths
- `AiService` — AI infrastructure (if available)

### Existing Documentation

- `DIFFERENTIATING_FEATURES.md` §4.3 — Newsletter Builder
- `WHATSAPP_INTEGRATION_SPEC.md` — WhatsApp infrastructure
- `EMAIL_INTEGRATION_SPEC.md` — Email infrastructure
- `SCHEDULED_ANNOUNCEMENTS_SPEC.md` — Scheduling pattern

### Technical Debt

| # | Gap | Details |
|---|---|---|
| TD-1 | No newsletter system | No newsletter tables, no service, no UI |
| TD-2 | No HTML email rendering | Server cannot render HTML email templates |
| TD-3 | Email infrastructure dependency | Requires `EMAIL_INTEGRATION_SPEC.md` to be implemented |
| TD-4 | AI service dependency | Requires `AiService` to be available for AI-assisted generation |

### Gaps

| # | Gap | Impact | Severity |
|---|---|---|---|
| G1 | No newsletter builder | Can't create curated newsletters | **High** |
| G2 | No multi-channel distribution | Can't reach parents via email/WhatsApp/in-app | **High** |
| G3 | No read tracking | Can't measure newsletter engagement | **Medium** |
| G4 | No AI-assisted generation | Admin must manually compose newsletters | **Low** |

---

## 3. Functional Requirements

### FR-001
| Field | Value |
|---|---|
| **Title** | Newsletter Editor with Content Blocks |
| **Description** | Newsletter editor with content blocks: header, text, image, event highlight, announcement summary, achievement, footer. Drag-and-drop block arrangement. |
| **Priority** | Critical |
| **User Roles** | School Admin |
| **Acceptance notes** | Content blocks stored as JSON in `newsletters.content_blocks`. Block types: header, text, image, event, announcement, achievement, footer. |

### FR-002
| Field | Value |
|---|---|
| **Title** | School Branding |
| **Description** | School branding: logo, primary color, school name in header. Applied to email and in-app rendering. |
| **Priority** | High |
| **User Roles** | School Admin |
| **Acceptance notes** | Branding sourced from `SchoolsTable` (logo, name). Primary color configurable per newsletter or from school settings. |

### FR-003
| Field | Value |
|---|---|
| **Title** | AI-Assisted Content Generation |
| **Description** | AI-assisted: "Generate newsletter from this month's announcements/events". Fetches month's data, calls AiService to summarize into content blocks. |
| **Priority** | Medium |
| **User Roles** | School Admin |
| **Acceptance notes** | Requires `AiService` to be available. Falls back to manual composition if AI unavailable. |

### FR-004
| Field | Value |
|---|---|
| **Title** | Preview Before Publish |
| **Description** | Preview before publish (email preview, WhatsApp preview, in-app preview). Admin sees rendered output for each channel. |
| **Priority** | High |
| **User Roles** | School Admin |
| **Acceptance notes** | `GET /api/v1/school/newsletters/{id}/preview` returns rendered HTML (email), text (WhatsApp), and card (in-app). |

### FR-005
| Field | Value |
|---|---|
| **Title** | Multi-Channel Distribution |
| **Description** | Distribute via: email (HTML newsletter), WhatsApp (summary + image), in-app (newsletter card). Admin selects channels at publish time. |
| **Priority** | Critical |
| **User Roles** | School Admin |
| **Acceptance notes** | Email requires `EMAIL_INTEGRATION_SPEC.md`. WhatsApp requires `WHATSAPP_INTEGRATION_SPEC.md`. In-app uses existing `NotificationService`. |

### FR-006
| Field | Value |
|---|---|
| **Title** | Newsletter Archive |
| **Description** | Newsletter archive: parents can view past newsletters. Archived by issue number, sorted descending. |
| **Priority** | High |
| **User Roles** | Parent |
| **Acceptance notes** | `GET /api/v1/parent/newsletters` returns published newsletters. `GET /api/v1/parent/newsletters/{id}` returns full content. |

### FR-007
| Field | Value |
|---|---|
| **Title** | Read Tracking |
| **Description** | Read tracking: email opens (tracking pixel), in-app views (view endpoint). Counts stored on newsletter record. |
| **Priority** | Medium |
| **User Roles** | System |
| **Acceptance notes** | `email_open_count` incremented via tracking pixel. `in_app_view_count` incremented via `POST /api/v1/parent/newsletters/{id}/view`. |

### FR-008
| Field | Value |
|---|---|
| **Title** | Scheduled Delivery |
| **Description** | Schedule for future delivery (reuse `SCHEDULED_ANNOUNCEMENTS_SPEC.md` pattern). Newsletter status transitions to `scheduled` then auto-publishes at `scheduled_at`. |
| **Priority** | Medium |
| **User Roles** | School Admin |
| **Acceptance notes** | `newsletters.scheduled_at` timestamp. Background job checks for scheduled newsletters past their time and publishes. |

---

## 4. User Stories

### School Admin
- [ ] Create a new newsletter with title and issue number
- [ ] Add and arrange content blocks (text, image, event, announcement, achievement)
- [ ] Apply school branding (logo, colors, header)
- [ ] Generate newsletter content using AI from month's events/announcements
- [ ] Preview newsletter in email, WhatsApp, and in-app formats
- [ ] Publish newsletter to selected channels (email, WhatsApp, in-app)
- [ ] Schedule newsletter for future delivery
- [ ] View newsletter archive
- [ ] View read tracking metrics (email opens, in-app views)

### Parent
- [ ] View newsletter archive (past issues)
- [ ] Open and read individual newsletters
- [ ] Receive newsletter via email, WhatsApp, or in-app notification
- [ ] View newsletter in-app with full content

### System
- [ ] Render content blocks as HTML email
- [ ] Generate WhatsApp summary text + header image
- [ ] Create in-app notification with deep link
- [ ] Track email opens via tracking pixel
- [ ] Track in-app views via view endpoint
- [ ] Auto-publish scheduled newsletters

---

## 5. Business Rules

### BR-001
**Rule:** Issue numbers are unique per school.
**Enforcement:** `UNIQUE(school_id, issue_number)` constraint on `newsletters` table.

### BR-002
**Rule:** Only school admin can create, edit, publish, and schedule newsletters.
**Enforcement:** `requireSchoolAdmin()` on all admin endpoints.

### BR-003
**Rule:** Newsletters must be in `published` status to be viewable by parents.
**Enforcement:** Parent endpoints filter `status = 'published'`. Draft and scheduled newsletters are admin-only.

### BR-004
**Rule:** Scheduled newsletters auto-publish at `scheduled_at`.
**Enforcement:** Background job checks `status = 'scheduled' AND scheduled_at <= now()`, transitions to `published` and triggers distribution.

### BR-005
**Rule:** AI generation is optional and falls back to manual.
**Enforcement:** If `AiService` is unavailable or returns error, admin composes manually. AI generation is a helper, not a requirement.

### BR-006
**Rule:** Content blocks are stored as JSON.
**Enforcement:** `newsletters.content_blocks` is TEXT column storing JSON array. Each block has `type` and type-specific fields.

### BR-007
**Rule:** Email distribution requires email infrastructure.
**Enforcement:** If `EMAIL_INTEGRATION_SPEC.md` is not implemented, email channel is disabled. Admin sees "Email channel not available" in UI.

---

## 6. Database Design

### 6.1 Entity Relationship Summary

One new table: `newsletters` (metadata, content blocks as JSON, status, tracking counts, scheduling).

### 6.2 New Tables

#### `newsletters` table

```sql
CREATE TABLE newsletters (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL REFERENCES schools(id),
    title           TEXT NOT NULL,                 -- "June 2026 Newsletter"
    issue_number    INTEGER NOT NULL,
    content_blocks  TEXT NOT NULL,                 -- JSON: [{"type": "text", "content": "..."}, {"type": "image", "url": "..."}, {"type": "event", "event_id": "..."}]
    header_image_url TEXT,                         -- optional custom header
    status          VARCHAR(16) NOT NULL DEFAULT 'draft', -- draft | scheduled | published
    scheduled_at    TIMESTAMP,
    published_at    TIMESTAMP,
    email_sent_count INTEGER NOT NULL DEFAULT 0,
    email_open_count INTEGER NOT NULL DEFAULT 0,
    in_app_view_count INTEGER NOT NULL DEFAULT 0,
    created_by      UUID REFERENCES app_users(id),
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(school_id, issue_number)
);
CREATE INDEX idx_newsletters_school ON newsletters(school_id, status, published_at DESC);
```

### 6.3 Modified Tables

None. All new tables.

### 6.4 Indexes

- `idx_newsletters_school` — archive listing by school + status + date

### 6.5 Constraints

- `newsletters.school_id` — NOT NULL, FK to schools
- `newsletters.title` — NOT NULL, TEXT
- `newsletters.issue_number` — NOT NULL, INTEGER
- `newsletters.content_blocks` — NOT NULL, TEXT (JSON array)
- `newsletters.status` — NOT NULL, one of draft/scheduled/published
- `newsletters.created_by` — nullable, FK to app_users
- `UNIQUE(school_id, issue_number)` — one issue number per school

### 6.6 Foreign Keys

- `newsletters.school_id` → `schools.id`
- `newsletters.created_by` → `app_users.id` (nullable)

### 6.7 Soft Delete Strategy

- Newsletters: not deleted (archive). Admin can unpublish (status → draft).
- No hard delete for published newsletters (audit trail).

### 6.8 Audit Fields

- `created_at` — newsletters
- `updated_at` — newsletters
- `published_at` — newsletters (publish timestamp)
- `scheduled_at` — newsletters (scheduled delivery time)

### 6.9 Migration Notes

Migration: `docs/db/migration_073_newsletter.sql`
- Creates 1 new table with FK constraints and indexes
- No data backfill (new feature)
- No modifications to existing tables

### 6.10 Exposed Mappings

Add 1 new table object in `server/.../db/Tables.kt`:

- `NewslettersTable` — newsletter metadata, content blocks, status, tracking

Register in `DatabaseFactory.kt` `allTables` array.

### 6.11 Seed Data

N/A — newsletters created by admin.

---

## 7. State Machines

### Newsletter Lifecycle State Machine

```
DRAFT ──admin_schedules──> SCHEDULED ──scheduled_at_reached──> PUBLISHED
DRAFT ──admin_publishes──> PUBLISHED
SCHEDULED ──admin_cancels_schedule──> DRAFT
PUBLISHED ──admin_unpublishes──> DRAFT
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `draft` | Admin publishes | `published` | Distribution triggered immediately |
| `draft` | Admin schedules | `scheduled` | `scheduled_at` must be in the future |
| `scheduled` | `scheduled_at` reached | `published` | Auto-publish via background job; distribution triggered |
| `scheduled` | Admin cancels schedule | `draft` | `scheduled_at` cleared |
| `published` | Admin unpublishes | `draft` | No longer visible to parents; tracking counts preserved |

---

## 8. Backend Architecture

### 8.1 Component Overview

`NewsletterService` handles newsletter CRUD, content block management, AI generation, publishing, scheduling, and tracking. `NewsletterRenderer` handles HTML email rendering and WhatsApp summary generation. `NewsletterRouting` exposes admin and parent endpoints.

### 8.2 Design Principles

1. **JSON content blocks** — flexible content storage supporting multiple block types
2. **Multi-channel rendering** — same content blocks rendered differently for email, WhatsApp, in-app
3. **Reuse existing infra** — notifications, file storage, scheduling pattern
4. **AI-assisted, not AI-dependent** — AI generation is a helper; manual composition always available
5. **Multi-tenant isolation** — all queries filtered by `school_id`

### 8.3 Core Types

```kotlin
class NewsletterService(private val aiService: AiService?) {
    // Admin — CRUD
    suspend fun listNewsletters(schoolId: UUID, status: String?): List<NewsletterDto>
    suspend fun getNewsletter(schoolId: UUID, newsletterId: UUID): NewsletterDto?
    suspend fun createNewsletter(schoolId: UUID, title: String, issueNumber: Int, createdBy: UUID): NewsletterDto
    suspend fun updateNewsletter(schoolId: UUID, newsletterId: UUID, dto: UpdateNewsletterDto): NewsletterDto?
    suspend fun updateContent(schoolId: UUID, newsletterId: UUID, blocks: List<ContentBlock>): NewsletterDto?
    suspend fun deleteNewsletter(schoolId: UUID, newsletterId: UUID): Boolean

    // Admin — AI
    suspend fun aiGenerateFromMonth(schoolId: UUID, month: YearMonth): List<ContentBlock> {
        // 1. Fetch month's announcements, events, achievements
        // 2. Call AiService to summarize into newsletter content blocks
    }

    // Admin — Publish
    suspend fun previewNewsletter(schoolId: UUID, newsletterId: UUID): NewsletterPreviewDto
    suspend fun publishNewsletter(schoolId: UUID, newsletterId: UUID, channels: List<String>): NewsletterDto
    suspend fun scheduleNewsletter(schoolId: UUID, newsletterId: UUID, scheduledAt: Instant, channels: List<String>): NewsletterDto
    suspend fun cancelSchedule(schoolId: UUID, newsletterId: UUID): NewsletterDto?
    suspend fun unpublish(schoolId: UUID, newsletterId: UUID): NewsletterDto?

    // Parent
    suspend fun getArchive(schoolId: UUID): List<NewsletterDto>
    suspend fun getNewsletterForParent(newsletterId: UUID): NewsletterDto?
    suspend fun trackView(newsletterId: UUID): Unit
    suspend fun trackOpen(newsletterId: UUID): Unit  // email open pixel
}
```

### 8.4 Repositories

- `NewsletterRepository` — newsletter CRUD, status management, archive queries

### 8.5 Mappers

- `NewsletterMapper` — maps DB rows to DTOs with parsed JSON content blocks

### 8.6 Permission Checks

- Admin endpoints: `requireSchoolContext()` + `requireSchoolAdmin()` for all writes
- Parent endpoints: `requireAuth()` — any authenticated parent
- Tracking endpoints: no auth required for email open pixel (pixel URL includes newsletter ID)

### 8.7 Background Jobs

- `NewsletterScheduleJob` — hourly; checks for scheduled newsletters past their `scheduled_at`, transitions to `published` and triggers distribution

### 8.8 Domain Events

- `NewsletterCreated` — emitted on creation
- `NewsletterPublished` — emitted on publish; triggers distribution
- `NewsletterScheduled` — emitted on schedule
- `NewsletterViewed` — emitted on in-app view
- `NewsletterOpened` — emitted on email open

### 8.9 Caching

- Newsletter content: cached per newsletter until updated
- Archive listing: cached for 5 minutes
- No cache for tracking counts (real-time)

### 8.10 Transactions

- Newsletter creation: insert newsletter in transaction
- Publish: update status + trigger distribution (distribution async)
- Schedule: update status + set `scheduled_at` in transaction

### 8.11 Rate Limiting

- Standard API rate limiting for all endpoints
- No special rate limiting needed

### 8.12 Configuration

- `NEWSLETTER_AI_ENABLED` — default `false`; enable AI-assisted generation
- `NEWSLETTER_ARCHIVE_CACHE_TTL` — default `300`; archive cache TTL in seconds
- `NEWSLETTER_MAX_BLOCKS` — default `30`; maximum content blocks per newsletter
- `NEWSLETTER_EMAIL_ENABLED` — default `true`; enable email distribution channel
- `NEWSLETTER_WHATSAPP_ENABLED` — default `true`; enable WhatsApp distribution channel

---

## 9. API Contracts

### 9.1 Admin Endpoints

All require `requireSchoolContext()`. Writes require `requireSchoolAdmin()`.

```
# Newsletter CRUD
GET    /api/v1/school/newsletters?status=&page=&limit=
POST   /api/v1/school/newsletters
GET    /api/v1/school/newsletters/{id}
PATCH  /api/v1/school/newsletters/{id}
DELETE /api/v1/school/newsletters/{id}

# Content
PATCH  /api/v1/school/newsletters/{id}/content  { blocks: [...] }

# AI
POST   /api/v1/school/newsletters/{id}/ai-generate  { month: "2026-06" }

# Publish / Schedule
POST   /api/v1/school/newsletters/{id}/preview
POST   /api/v1/school/newsletters/{id}/publish  { channels: ["email", "whatsapp", "in_app"] }
POST   /api/v1/school/newsletters/{id}/schedule  { scheduled_at: "...", channels: [...] }
POST   /api/v1/school/newsletters/{id}/cancel-schedule
POST   /api/v1/school/newsletters/{id}/unpublish
```

### 9.2 Parent Endpoints

```
GET    /api/v1/parent/newsletters
GET    /api/v1/parent/newsletters/{id}
POST   /api/v1/parent/newsletters/{id}/view
```

### 9.3 Public Tracking Endpoint

```
GET    /api/v1/newsletters/{id}/open.png  -- email open tracking pixel (no auth)
```

### 9.4 DTO Models

All `@Serializable`, wrapped in `ApiResponse<T>` pattern.

```kotlin
@Serializable data class NewsletterDto(
    val id: String, val schoolId: String, val title: String,
    val issueNumber: Int, val contentBlocks: List<ContentBlockDto>,
    val headerImageUrl: String?, val status: String,
    val scheduledAt: String?, val publishedAt: String?,
    val emailSentCount: Int, val emailOpenCount: Int, val inAppViewCount: Int,
    val createdBy: String?, val createdAt: String, val updatedAt: String
)

@Serializable data class CreateNewsletterDto(
    val title: String, val issueNumber: Int
)

@Serializable data class UpdateNewsletterDto(
    val title: String? = null, val headerImageUrl: String? = null
)

@Serializable data class ContentBlockDto(
    val type: String,  // header | text | image | event | announcement | achievement | footer
    val content: String? = null,
    val imageUrl: String? = null,
    val eventId: String? = null,
    val announcementId: String? = null,
    val achievementText: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable data class UpdateContentDto(
    val blocks: List<ContentBlockDto>
)

@Serializable data class AiGenerateDto(
    val month: String  // "2026-06"
)

@Serializable data class PublishDto(
    val channels: List<String>  // ["email", "whatsapp", "in_app"]
)

@Serializable data class ScheduleDto(
    val scheduledAt: String,
    val channels: List<String>
)

@Serializable data class NewsletterPreviewDto(
    val emailHtml: String,
    val whatsappText: String,
    val whatsappImageUrl: String?,
    val inAppCard: InAppCardDto
)

@Serializable data class InAppCardDto(
    val title: String, val summary: String,
    val imageUrl: String?, val deepLink: String
)
```

---

## 10. Frontend Architecture

### 10.1 Screens

| Screen | Platform | Role | Description |
|---|---|---|---|
| `NewsletterEditorScreen` | Compose | Admin | Visual editor with drag-and-drop content blocks, branding, preview |
| `NewsletterListScreen` | Compose | Admin | List of newsletters with status, tracking metrics |
| `NewsletterArchiveScreen` | Compose | Parent | Archive of past newsletters, sorted by issue |
| `NewsletterViewScreen` | Compose | Parent | Full newsletter view with rendered content blocks |
| Web admin newsletter page | Web | Admin | Newsletter management dashboard |

### 10.2 Navigation

- Admin portal → Newsletters → `NewsletterListScreen`
- Admin portal → Newsletters → New → `NewsletterEditorScreen`
- Admin portal → Newsletters → {newsletter} → `NewsletterEditorScreen`
- Parent portal → Newsletters → `NewsletterArchiveScreen`
- Parent portal → Newsletters → {newsletter} → `NewsletterViewScreen`
- Web admin → /admin/newsletters → newsletter list page

### 10.3 UX Flows

#### Admin: Create and Publish Newsletter

1. Admin opens Newsletters → clicks "New Newsletter"
2. Enters title and issue number
3. Adds content blocks: text, image, event highlight, announcement summary, achievement
4. Optionally clicks "AI Generate" → selects month → AI generates content blocks from month's data
5. Arranges blocks via drag-and-drop
6. Clicks "Preview" → sees email, WhatsApp, and in-app previews
7. Selects distribution channels (email, WhatsApp, in-app)
8. Clicks "Publish" or "Schedule"
9. If publish → distribution triggered immediately
10. If schedule → select date/time → auto-publishes at scheduled time

#### Parent: View Newsletter

1. Parent receives notification (email, WhatsApp, or in-app)
2. Opens newsletter from in-app notification or archive
3. Views rendered newsletter with school branding
4. View tracked via `POST /api/v1/parent/newsletters/{id}/view`

### 10.4 State Management

```kotlin
data class NewsletterState(
    val newsletters: List<NewsletterDto>,
    val currentNewsletter: NewsletterDto?,
    val preview: NewsletterPreviewDto?,
    val isLoading: Boolean,
    val error: String?,
)
```

### 10.5 Offline Support

- Newsletter archive cached locally for offline viewing
- Individual newsletter content cached after first view

### 10.6 Loading States

- Loading archive: "Loading newsletters..."
- Generating AI content: "Generating newsletter content..."
- Publishing: "Publishing newsletter..."
- Rendering preview: "Generating preview..."

### 10.7 Error Handling (UI)

- AI generation failed: "AI generation unavailable. Please compose manually."
- Email channel unavailable: "Email distribution is not available. Please use WhatsApp or in-app."
- Newsletter not found: "Newsletter not found."
- Newsletter not published: "This newsletter is not yet published."

### 10.8 Component Integration Guidelines

| Rule | Description |
|---|---|
| **R1** | Drag-and-drop content block arrangement |
| **R2** | Block type selector (text, image, event, announcement, achievement) |
| **R3** | School branding header (logo, school name, primary color) |
| **R4** | AI generate button with month picker |
| **R5** | Preview tabs (email, WhatsApp, in-app) |
| **R6** | Channel selector (email, WhatsApp, in-app checkboxes) |
| **R7** | Schedule date/time picker |
| **R8** | Newsletter status badge (draft, scheduled, published) |
| **R9** | Tracking metrics display (email opens, in-app views) |
| **R10** | Newsletter archive list sorted by issue number |
| **R11** | Rendered newsletter view with branding |
| **R12** | Image upload for content blocks |

---

## 11. Shared Module Changes (KMP)

### 11.1 DTOs

All DTOs defined in section 9.4, placed in `shared/.../feature/newsletter/data/remote/`.

### 11.2 Domain Models

```kotlin
data class Newsletter(
    val id: UUID, val schoolId: UUID, val title: String,
    val issueNumber: Int, val contentBlocks: List<ContentBlock>,
    val headerImageUrl: String?, val status: NewsletterStatus,
    val scheduledAt: Instant?, val publishedAt: Instant?,
    val emailSentCount: Int, val emailOpenCount: Int, val inAppViewCount: Int,
)

enum class NewsletterStatus { DRAFT, SCHEDULED, PUBLISHED }

sealed class ContentBlock {
    data class Header(val title: String, val subtitle: String?) : ContentBlock()
    data class Text(val content: String) : ContentBlock()
    data class Image(val url: String, val caption: String?) : ContentBlock()
    data class EventHighlight(val eventId: UUID, val title: String, val date: Instant) : ContentBlock()
    data class AnnouncementSummary(val announcementId: UUID, val title: String, val summary: String) : ContentBlock()
    data class Achievement(val text: String, val studentName: String?) : ContentBlock()
    data class Footer(val content: String) : ContentBlock()
}
```

### 11.3 Repository Interfaces

```kotlin
interface NewsletterRepository {
    suspend fun listNewsletters(status: String?): NetworkResult<List<NewsletterDto>>
    suspend fun getNewsletter(id: String): NetworkResult<NewsletterDto>
    suspend fun createNewsletter(dto: CreateNewsletterDto): NetworkResult<NewsletterDto>
    suspend fun updateNewsletter(id: String, dto: UpdateNewsletterDto): NetworkResult<NewsletterDto>
    suspend fun updateContent(id: String, blocks: List<ContentBlockDto>): NetworkResult<NewsletterDto>
    suspend fun deleteNewsletter(id: String): NetworkResult<Boolean>
    suspend fun aiGenerate(month: String): NetworkResult<List<ContentBlockDto>>
    suspend fun preview(id: String): NetworkResult<NewsletterPreviewDto>
    suspend fun publish(id: String, channels: List<String>): NetworkResult<NewsletterDto>
    suspend fun schedule(id: String, scheduledAt: String, channels: List<String>): NetworkResult<NewsletterDto>
    suspend fun cancelSchedule(id: String): NetworkResult<NewsletterDto>
    suspend fun getArchive(): NetworkResult<List<NewsletterDto>>
    suspend fun getNewsletterForParent(id: String): NetworkResult<NewsletterDto>
    suspend fun trackView(id: String): NetworkResult<Unit>
}
```

### 11.4 UseCases

- `ListNewslettersUseCase`, `GetNewsletterUseCase`, `CreateNewsletterUseCase`
- `UpdateNewsletterUseCase`, `UpdateContentUseCase`, `DeleteNewsletterUseCase`
- `AiGenerateUseCase`, `PreviewNewsletterUseCase`
- `PublishNewsletterUseCase`, `ScheduleNewsletterUseCase`, `CancelScheduleUseCase`
- `GetArchiveUseCase`, `GetNewsletterForParentUseCase`, `TrackViewUseCase`

### 11.5 Validation

- Title: not empty, max 200 characters
- Issue number: positive integer
- Content blocks: at least 1 block, max 30 blocks
- Scheduled at: must be in the future when scheduling
- Channels: at least 1 channel selected

### 11.6 Serialization

Standard Kotlinx serialization with JSON for content blocks.

### 11.7 Network APIs

Ktor `@Resource` route definitions:
- `SchoolNewslettersApi` — admin endpoints
- `ParentNewslettersApi` — parent endpoints

### 11.8 Database Models (Local Cache)

- Newsletter archive cached locally for offline viewing
- Individual newsletter content cached after first view

---

## 12. Permissions Matrix

| Action | Super Admin | School Admin | Teacher | Parent |
|---|---|---|---|---|
| Create/edit/delete newsletters | ✅ | ✅ | ❌ | ❌ |
| AI generate content | ✅ | ✅ | ❌ | ❌ |
| Preview newsletters | ✅ | ✅ | ❌ | ❌ |
| Publish/schedule newsletters | ✅ | ✅ | ❌ | ❌ |
| View tracking metrics | ✅ | ✅ | ❌ | ❌ |
| View newsletter archive | N/A | N/A | ❌ | ✅ |
| View individual newsletter | N/A | N/A | ❌ | ✅ |
| Track view | N/A | N/A | N/A | ✅ |

---

## 13. Notifications

### Newsletter-Specific Notification Triggers

| Type | Trigger | Recipient | Channel | Message |
|---|---|---|---|---|
| Newsletter Published (in-app) | Admin publishes newsletter | All parents | Push | "New newsletter: {newsletter_title}. Tap to read." |
| Newsletter Published (email) | Admin publishes newsletter | All parents | Email | HTML newsletter with school branding |
| Newsletter Published (WhatsApp) | Admin publishes newsletter | All parents | WhatsApp | Summary text + header image |
| Newsletter Scheduled | Admin schedules newsletter | None (admin confirmation) | N/A | N/A |
| Newsletter Auto-Published | Scheduled newsletter auto-publishes | All parents | Push + Email + WhatsApp | Same as published |

### Notification System Integration

- Reuse `NotificationService` for in-app push notifications
- Email distribution: render HTML from content blocks → send via `EmailService`
- WhatsApp distribution: generate summary text + header image → send via `WhatsAppService`
- In-app: create notification with deep link to `NewsletterViewScreen`
- Reuse `NotifyRecipients.kt` for parent audience resolution

---

## 14. Background Jobs

### Newsletter Schedule Job

| Field | Value |
|---|---|
| **Name** | `NewsletterScheduleJob` |
| **Trigger** | Hourly |
| **Frequency** | Every hour |
| **Description** | Checks for scheduled newsletters past their `scheduled_at`, transitions to `published` and triggers distribution |
| **Timeout** | 120 seconds |
| **Retry** | None |
| **On failure** | Logged; retried next hour |

### Distribution (Event-Driven)

| Field | Value |
|---|---|
| **Name** | `NewsletterDistribution` |
| **Trigger** | Newsletter published (manual or auto) |
| **Frequency** | On-demand (async) |
| **Description** | Renders content blocks for selected channels and distributes to parents |
| **Timeout** | 300 seconds (large parent list) |
| **Retry** | 3 attempts with 30-second intervals |
| **On failure** | Logged; admin notified of partial failure |

---

## 15. Integrations

### Internal Integrations

| System | Integration Point | Direction | Protocol | Error Handling |
|---|---|---|---|---|
| `NotificationService` | In-app push notification on publish | Outbound | Direct call | Logged; non-blocking |
| `NotifyRecipients.kt` | Parent audience resolution | Read | Direct code | Fallback: skip if no recipients |
| `SupabaseStorage` | Newsletter images (header, content blocks) | Write | Direct call | Return error to client |
| `MediaRouting.kt` | Image upload endpoint | Write | HTTP | Standard error response |
| `AnnouncementsTable` | Source data for AI generation | Read | Direct DB | Return empty if no announcements |
| `CalendarEventsTable` | Source data for AI generation (events) | Read | Direct DB | Return empty if no events |
| `SchoolsTable` | School branding (logo, name, colors) | Read | Direct DB | Use defaults if not found |
| `WhatsappLogsTable` | WhatsApp message logging | Write | Direct DB | Logged |
| `AiService` | AI-assisted content generation | Outbound | Direct call | Fallback: manual composition |

### External Integrations

| System | Purpose | Direction | Protocol | Authentication | Error Handling |
|---|---|---|---|---|---|
| Meta WhatsApp Business API | WhatsApp newsletter distribution | Outbound | HTTP API | Bearer token | Retry 3x; log to `WhatsappLogsTable` |
| Email Service (SMTP/API) | Email newsletter distribution | Outbound | SMTP/HTTP | SMTP credentials / API key | Retry 3x; log error |
| AI Provider (OpenAI/etc.) | AI-assisted content generation | Outbound | HTTP API | API key | Return error; fallback to manual |

### Integration Patterns

- **Email:** Render content blocks as HTML email with school branding → send via `EmailService` to all parents. Email includes tracking pixel for open tracking.
- **WhatsApp:** Generate summary text (max 1024 chars) + header image → send via `WhatsAppService`. New template: `newsletter_published`.
- **In-app:** Create notification with deep link to `NewsletterViewScreen`. Notification includes newsletter title and summary.
- **AI:** Fetch month's announcements + events → send to `AiService` with prompt to summarize into newsletter content blocks → return structured blocks.

---

## 16. Security

### Authentication

- Admin endpoints: standard JWT auth via `requireSchoolContext()` + `requireSchoolAdmin()`
- Parent endpoints: standard JWT auth via `requireAuth()`
- Tracking pixel endpoint: no auth (public URL with newsletter ID embedded)

### Authorization

- Only school admin can create, edit, publish, schedule, and delete newsletters
- Any authenticated parent can view published newsletters and archive
- Tracking pixel: no auth required (image endpoint)

### Data Protection

- Newsletter content: school-scoped, no PII (unless admin includes student names in achievement blocks)
- Email tracking pixel: uses newsletter ID only, no user identification
- In-app view tracking: uses user ID from JWT (standard)
- Images: stored in SupabaseStorage with school-scoped paths

### Input Validation

- Title: not empty, max 200 characters
- Issue number: positive integer
- Content blocks: at least 1 block, max 30 blocks
- Text block content: max 5000 characters
- Image URL: valid URL format
- Scheduled at: must be in the future when scheduling
- Channels: at least 1 channel selected, valid channel names

### Rate Limiting

- Standard API rate limiting for all endpoints
- AI generation: additional rate limit (max 5 requests per hour per school)

### Audit Logging

- Newsletter creation, update, deletion
- Newsletter publish, schedule, cancel schedule, unpublish
- AI generation requests
- Distribution results (sent counts, failures)

### PII Handling

- Newsletter content may include student names in achievement blocks — admin responsibility
- Email tracking: no PII (newsletter ID only)
- In-app tracking: user ID linked to view (standard PII)
- No additional PII collected beyond standard auth

### Multi-tenant Isolation

- All queries filtered by `school_id`
- `newsletters.school_id` — NOT NULL, FK to schools
- Server validates school context on all admin endpoints
- Parent endpoints resolve school from JWT context

---

## 17. Performance & Scalability

### Expected Scale

- Small school: 10-50 newsletters/year, 100-500 parents
- Medium school: 50-100 newsletters/year, 500-2,000 parents
- Large school: 100-200 newsletters/year, 2,000-10,000 parents

### Query Optimization

- **Archive listing:** `idx_newsletters_school` on `(school_id, status, published_at DESC)`. Paginated.
- **Admin listing:** Same index, filtered by status.
- **Individual newsletter:** PK lookup by ID.
- **Scheduled newsletters:** Query `status = 'scheduled' AND scheduled_at <= now()`. Hourly job.

### Indexing Strategy

- `idx_newsletters_school` — archive listing by school + status + date
- `UNIQUE(school_id, issue_number)` — prevents duplicate issue numbers

### Caching Strategy

- Newsletter content: cached per newsletter until updated
- Archive listing: 5-minute TTL cache
- No cache for tracking counts (real-time)

### Pagination

- Newsletter listing: max 50 per page
- Archive: max 20 per page (fewer items, richer content)

### Connection Pooling

- Uses existing HikariCP connection pool
- No additional pooling needed

### Async Processing

- Distribution: async (render + send for each channel)
- AI generation: synchronous (admin waits for result)
- Tracking: synchronous (increment counter)

### Scalability Concerns

- Email distribution to 10,000+ parents: batch in chunks of 100 with 1-second delay
- WhatsApp distribution to 10,000+ parents: batch in chunks of 50 with rate limiting
- HTML rendering: O(n) where n = number of content blocks. Fast for <30 blocks.
- AI generation: depends on AI provider latency (5-30 seconds typical)

---

## 18. Edge Cases

| # | Scenario | Expected Behavior |
|---|---|---|
| EC-1 | Admin publishes newsletter with no content blocks | Return 400 "Cannot publish a newsletter with no content." |
| EC-2 | Admin schedules newsletter with past `scheduled_at` | Return 400 "Scheduled time must be in the future." |
| EC-3 | Admin creates newsletter with duplicate issue number | Return 409 "Issue number {n} already exists for this school." |
| EC-4 | AI service unavailable | Return 503 "AI generation unavailable. Please compose manually." |
| EC-5 | Email service unavailable | Skip email channel; distribute via other channels. Log warning. |
| EC-6 | WhatsApp service unavailable | Skip WhatsApp channel; distribute via other channels. Log warning. |
| EC-7 | Parent views unpublished newsletter | Return 404 "Newsletter not found." (don't reveal existence) |
| EC-8 | Parent views newsletter from different school | Return 404 "Newsletter not found." |
| EC-9 | Tracking pixel requested for non-existent newsletter | Return 1x1 transparent pixel (no error). |
| EC-10 | Admin unpublishes already-unpublished newsletter | Return 400 "Newsletter is not published." |
| EC-11 | Admin cancels schedule for non-scheduled newsletter | Return 400 "Newsletter is not scheduled." |
| EC-12 | Scheduled newsletter auto-publishes but distribution fails | Log error; admin notified. Newsletter status is `published` but distribution incomplete. |
| EC-13 | Content block references non-existent event/announcement | Server skips block during rendering; logs warning. |
| EC-14 | Image URL in content block is broken | Render with alt text or placeholder; don't fail entire newsletter. |
| EC-15 | Admin selects no channels for publish | Return 400 "At least one distribution channel must be selected." |
| EC-16 | Newsletter with 30+ content blocks | Return 400 "Maximum 30 content blocks allowed." |
| EC-17 | AI generates empty content (no announcements/events for month) | Return 200 with empty blocks list. Admin composes manually. |
| EC-18 | Email tracking pixel blocked by email client | Open not tracked. Acceptable behavior. |

---

## 19. Error Handling

### Error Response Format

Standard `ApiResponse` error format:
```json
{
  "success": false,
  "error": {
    "code": "NEWSLETTER_NOT_FOUND",
    "message": "Newsletter not found",
    "details": {}
  }
}
```

### Error Codes

| Code | HTTP Status | Description | User Message |
|---|---|---|---|
| `NEWSLETTER_NOT_FOUND` | 404 | Newsletter not found | "Newsletter not found." |
| `NEWSLETTER_NOT_PUBLISHED` | 400 | Newsletter is in draft/scheduled status | "This newsletter is not yet published." |
| `DUPLICATE_ISSUE_NUMBER` | 409 | Issue number already exists | "Issue number {n} already exists for this school." |
| `NO_CONTENT_BLOCKS` | 400 | Publishing with no content blocks | "Cannot publish a newsletter with no content." |
| `INVALID_SCHEDULE_TIME` | 400 | Scheduled time in the past | "Scheduled time must be in the future." |
| `NO_CHANNELS_SELECTED` | 400 | No distribution channels selected | "At least one distribution channel must be selected." |
| `MAX_BLOCKS_EXCEEDED` | 400 | More than 30 content blocks | "Maximum 30 content blocks allowed." |
| `AI_UNAVAILABLE` | 503 | AI service unavailable | "AI generation unavailable. Please compose manually." |
| `EMAIL_CHANNEL_UNAVAILABLE` | 400 | Email infrastructure not configured | "Email distribution is not available." |
| `NOT_PUBLISHED` | 400 | Unpublishing non-published newsletter | "Newsletter is not published." |
| `NOT_SCHEDULED` | 400 | Cancelling schedule for non-scheduled newsletter | "Newsletter is not scheduled." |

### Error Handling Strategy

- **Validation errors:** Return 400 with field-specific message
- **Auth errors:** Return 401/403 with clear message
- **Not found:** Return 404
- **Conflicts:** Return 409
- **AI errors:** Return 503 with fallback suggestion
- **Server errors:** Return 500 with generic message; log full error

### Retry Strategy

- Client retries: 3 attempts with exponential backoff for 5xx errors
- No retry for 4xx errors (client errors)
- Email delivery: 3 retries with 60-second intervals (existing pattern)
- WhatsApp delivery: 3 retries with 5-second intervals (existing pattern)
- AI generation: 1 retry with 10-second delay

### Fallback Behavior

- AI unavailable: Admin composes manually
- Email unavailable: Distribute via WhatsApp + in-app
- WhatsApp unavailable: Distribute via email + in-app
- Both email + WhatsApp unavailable: Distribute via in-app only
- Distribution partial failure: Log which channels failed; admin notified

---

## 20. Analytics & Reporting

### Analytics Dashboard Data

| Metric | Source | Derivation |
|---|---|---|
| Email sent count | `newsletters.email_sent_count` | Direct count |
| Email open count | `newsletters.email_open_count` | Tracking pixel increments |
| Email open rate | `email_open_count / email_sent_count` | Derived percentage |
| In-app view count | `newsletters.in_app_view_count` | View endpoint increments |
| Newsletter archive count | `newsletters` where `status='published'` | Direct count |
| Publication frequency | `newsletters.published_at` | Count per month |

### Export Capabilities

- Newsletter list export (CSV) — title, issue, status, dates, tracking counts
- Individual newsletter content export (JSON) — full content blocks

### Report Types

| Report | Format | Frequency | Recipient |
|---|---|---|---|
| Newsletter tracking summary | CSV | On-demand | School Admin |
| Publication history | CSV | On-demand | School Admin |
| Engagement overview | JSON (API) | On-demand | School Admin |

---

## 21. Testing Strategy

### Unit Tests

- `NewsletterService` — all methods (CRUD, AI generation, publish, schedule, tracking)
- `NewsletterRenderer` — HTML rendering, WhatsApp summary generation, in-app card generation
- Content block parsing and validation
- State machine transitions (draft → scheduled → published, unpublish, cancel schedule)
- Tracking count increments (email open, in-app view)

### Integration Tests

- Newsletter creation → add content → publish → distribution triggered
- Newsletter scheduling → auto-publish via background job → distribution
- AI generation: mock AiService → verify content blocks generated from month's data
- Email tracking pixel: request pixel → verify `email_open_count` incremented
- In-app view: parent views newsletter → verify `in_app_view_count` incremented
- Multi-channel distribution: publish with all channels → verify all channels triggered
- Multi-tenant: newsletter from school A not accessible to school B parents

### E2E Tests

- Admin creates newsletter → AI generates content → previews → publishes → parent views
- Admin schedules newsletter → background job publishes → parent receives notification
- Parent opens newsletter archive → views past newsletter → view tracked

### Performance Tests

- HTML rendering with 30 content blocks: < 500ms
- Distribution to 1,000 parents (email): < 60s
- Archive listing with 200 newsletters: < 500ms

### Test Data

- 10 sample newsletters (draft, scheduled, published)
- Various content block types represented
- Newsletters with tracking counts (email opens, in-app views)
- Scheduled newsletters with past and future `scheduled_at`

### Test Environment

- Test database with schema migration applied
- Mock AI service for AI generation tests
- Mock email service for distribution tests
- Mock WhatsApp API for notification tests
- Test JWT tokens for admin and parent roles

---

## 22. Acceptance Criteria

- [ ] Visual editor with content blocks (text, image, event, announcement, achievement)
- [ ] School branding applied (logo, colors, header)
- [ ] AI generation from month's events works (when AI service available)
- [ ] Preview before publish (email, WhatsApp, in-app)
- [ ] Multi-channel distribution works (email, WhatsApp, in-app)
- [ ] Newsletter archive viewable by parents
- [ ] Read tracking (email opens via pixel, in-app views)
- [ ] Scheduling supported (auto-publish at scheduled time)
- [ ] Issue numbers unique per school
- [ ] All admin endpoints enforce `requireSchoolContext()` + `requireSchoolAdmin()`
- [ ] Compose app newsletter editor UI
- [ ] Compose app newsletter view UI for parents
- [ ] Tracking pixel endpoint works without auth
- [ ] Fallback when AI service unavailable (manual composition)
- [ ] Fallback when email/WhatsApp unavailable (other channels still work)

---

## 23. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration (`073`), `NewslettersTable` Exposed object, `DatabaseFactory` registration |
| 2 | 2 days | `NewsletterService.kt` — CRUD, content blocks, AI generation, publish, schedule, tracking |
| 3 | 2 days | `NewsletterRenderer.kt` — HTML email rendering, WhatsApp summary generation, in-app card |
| 4 | 1 day | `NewsletterRouting.kt` with all endpoints + DTOs, mount in `Application.kt`, tracking pixel endpoint |
| 5 | 4 days | Client UI: `NewsletterEditorScreen.kt` (drag-and-drop, branding, preview), `NewsletterViewScreen.kt` (parent view), wire into navigation |
| 6 | 1 day | Tests (server unit + integration, client unit) |

### Pre-Implementation Checklist

- [ ] Verify `EMAIL_INTEGRATION_SPEC.md` is implemented (for email channel)
- [ ] Verify `WHATSAPP_INTEGRATION_SPEC.md` is implemented (for WhatsApp channel)
- [ ] Verify `AiService` availability (for AI-assisted generation)
- [ ] Check if drag-and-drop library is available in Compose
- [ ] Verify `SchoolsTable` has logo and branding fields
- [ ] Verify `AnnouncementsTable` and `CalendarEventsTable` schemas for AI source data

---

## 24. File-Level Impact Analysis

### Required (6 files)

| # | File | Change Type | Description |
|---|---|---|---|
| 1 | `docs/db/migration_073_newsletter.sql` | New | DDL: 1 table with FK constraints and indexes |
| 2 | `server/.../db/Tables.kt` | Modify | Add `NewslettersTable` object |
| 3 | `server/.../db/DatabaseFactory.kt` | Modify | Register `NewslettersTable` in `allTables` array |
| 4 | `server/.../feature/newsletter/NewsletterService.kt` | New | Core service (CRUD, AI, publish, schedule, tracking) |
| 5 | `server/.../feature/newsletter/NewsletterRenderer.kt` | New | HTML + WhatsApp + in-app rendering |
| 6 | `server/.../feature/newsletter/NewsletterRouting.kt` | New | API endpoints + DTOs + `newsletterRouting()` function |

### Additional Required (2 files)

| # | File | Change Type | Description |
|---|---|---|---|
| 7 | `server/.../Application.kt` | Modify | Import + mount `newsletterRouting()` |
| 8 | `shared/.../feature/newsletter/` | New | Shared DTOs, domain models, repository, API client |

### Client UI (2 files)

| # | File | Change Type | Description |
|---|---|---|---|
| 9 | `composeApp/.../ui/v2/screens/admin/NewsletterEditorScreen.kt` | New | Visual editor with drag-and-drop, branding, preview |
| 10 | `composeApp/.../ui/v2/screens/parent/NewsletterViewScreen.kt` | New | Parent newsletter view with rendered content |

### Optional (2 files)

| # | File | Change Type | Description |
|---|---|---|---|
| 11 | `website/src/app/admin/newsletters/page.tsx` | New | Web admin newsletter management page |
| 12 | `composeApp/.../ui/v2/screens/admin/NewsletterListScreen.kt` | New | Admin newsletter list with tracking metrics |

---

## 25. Future Enhancements

| # | Enhancement | Priority | Effort | Notes |
|---|---|---|---|---|
| F-1 | Print/PDF export | Medium | S | Generate PDF version of newsletter |
| F-2 | Newsletter analytics dashboard | Medium | M | Engagement trends, open rate over time |
| F-3 | Newsletter templates | Low | M | Pre-built templates for common formats |
| F-4 | Parent feedback on newsletters | Low | S | Like/comment on newsletter issues |
| F-5 | Multi-language newsletters | Low | M | i18n for newsletter content |
| F-6 | Newsletter subscription preferences | Medium | S | Parents choose preferred channel |
| F-7 | Newsletter duplication / clone | Low | S | Copy existing newsletter as starting point |
| F-8 | Rich text editor for text blocks | Medium | M | WYSIWYG editor instead of plain text |
| F-9 | Video content blocks | Low | M | Embedded video in newsletters |
| F-10 | Newsletter scheduling calendar view | Low | S | Calendar UI for scheduling newsletters |

---

## Appendix A: Sequence Diagrams

### A.1 Admin Creates and Publishes Newsletter

```
Admin       NewsletterService    NewslettersTable    NewsletterRenderer    NotificationService
  │                │                   │                    │                    │
  │──create(title,issue)─→│            │                    │                    │
  │                │──insert newsletter─→│                    │                    │
  │←──NewsletterDto│                   │                    │                    │
  │                │                   │                    │                    │
  │──updateContent(blocks)─→│          │                    │                    │
  │                │──update content_blocks─→│               │                    │
  │←──NewsletterDto│                   │                    │                    │
  │                │                   │                    │                    │
  │──preview()────→│                   │                    │                    │
  │                │──load newsletter──│                    │                    │
  │                │──render HTML──────│──────────────────→│                    │
  │                │──render WhatsApp──│──────────────────→│                    │
  │                │──render in-app────│──────────────────→│                    │
  │←──PreviewDto──│                   │                    │                    │
  │                │                   │                    │                    │
  │──publish(channels)─→│              │                    │                    │
  │                │──update status='published'───────────→│                    │
  │                │──render for each channel──────────────→│                    │
  │                │──distribute (email/WhatsApp/in-app)──────────────────────→│
  │←──NewsletterDto│                   │                    │                    │
  │                │                   │                    │                    │
```

### A.2 AI-Assisted Content Generation

```
Admin       NewsletterService    AnnouncementsTable   CalendarEventsTable   AiService
  │                │                   │                    │                  │
  │──aiGenerate(month)─→│              │                    │                  │
  │                │──fetch announcements for month───────→│                  │
  │                │←──announcements──│                    │                  │
  │                │──fetch events for month─────────────────────────────────→│
  │                │←──events──────────────────────────────│                  │
  │                │──send to AiService with prompt──────────────────────────→│
  │                │←──generated content blocks───────────────────────────────│
  │←──List<ContentBlock>│              │                    │                  │
  │                │                   │                    │                  │
  │  [if AI unavailable]               │                    │                  │
  │←──503 error───│                   │                    │                  │
  │                │                   │                    │                  │
```

### A.3 Scheduled Newsletter Auto-Publish

```
NewsletterScheduleJob    NewsletterService    NewslettersTable    NotificationService
  │                          │                     │                    │
  │──checkScheduled()────────→│                     │                    │
  │                          │──SELECT status='scheduled' AND scheduled_at<=now()─→│
  │                          │←──due newsletters───│                    │
  │                          │  [for each]         │                    │
  │                          │──update status='published'──────────────→│                    │
  │                          │──render + distribute──────────────────────────────────────→│
  │←──count published────────│                     │                    │
  │                          │                     │                    │
```

### A.4 Email Open Tracking

```
Email Client    Server (Tracking Pixel)    NewslettersTable
  │                    │                        │
  │──GET /open.png?id──→│                        │
  │                    │──increment email_open_count─→│
  │←──1x1 pixel────│                        │
  │                    │                        │
```

---

## Appendix B: Domain Model / ER Diagram

```
┌──────────────────────────────────────────────────────────────────────────┐
│                              schools                                      │
│  id (PK)  name  logo_url  primary_color                                  │
└──────────────────────────┬───────────────────────────────────────────────┘
                           │
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                           newsletters                                    │
│  id (PK)                                                                  │
│  school_id (FK→schools)                                                   │
│  title, issue_number (UNIQUE per school)                                  │
│  content_blocks (JSON array)                                              │
│  header_image_url                                                         │
│  status (draft|scheduled|published)                                       │
│  scheduled_at, published_at                                               │
│  email_sent_count, email_open_count, in_app_view_count                   │
│  created_by (FK→app_users)                                                │
│  created_at, updated_at                                                   │
└──────────────────────────────────────────────────────────────────────────┘

Content Block JSON Structure:
┌─────────────────────────────────────────────────────────┐
│  { "type": "header",   "content": "June 2026" }         │
│  { "type": "text",     "content": "Dear parents..." }   │
│  { "type": "image",    "url": "...", "caption": "..." } │
│  { "type": "event",    "event_id": "..." }              │
│  { "type": "announcement", "announcement_id": "..." }   │
│  { "type": "achievement", "text": "...", "student": "..." } │
│  { "type": "footer",   "content": "..." }               │
└─────────────────────────────────────────────────────────┘
```

---

## Appendix C: Event Flow

### Domain Events

| Event | Emitter | Consumers | Payload | Side Effects |
|---|---|---|---|---|
| `NewsletterCreated` | `NewsletterService.createNewsletter()` | None | `newsletterId, schoolId, title, issueNumber, createdBy` | None |
| `NewsletterPublished` | `NewsletterService.publishNewsletter()` | NotificationService, NewsletterRenderer | `newsletterId, schoolId, title, channels` | Render + distribute via selected channels |
| `NewsletterScheduled` | `NewsletterService.scheduleNewsletter()` | None | `newsletterId, schoolId, scheduledAt, channels` | None (wait for scheduled time) |
| `NewsletterAutoPublished` | `NewsletterScheduleJob` | NotificationService, NewsletterRenderer | `newsletterId, schoolId, title, channels` | Same as `NewsletterPublished` |
| `NewsletterViewed` | `NewsletterService.trackView()` | None | `newsletterId, schoolId, userId` | Increment `in_app_view_count` |
| `NewsletterOpened` | `NewsletterService.trackOpen()` | None | `newsletterId` | Increment `email_open_count` |

### Event Delivery Guarantees

- Events are emitted synchronously within the same transaction
- Distribution is async (fire-and-forget with logging)
- Failed distributions are logged; admin notified
- No event bus / message queue — direct function calls

---

## Appendix D: Configuration

### Environment Variables

| Variable | Default | Description |
|---|---|---|
| `NEWSLETTER_AI_ENABLED` | `false` | Enable AI-assisted generation |
| `NEWSLETTER_ARCHIVE_CACHE_TTL` | `300` | Archive cache TTL in seconds (5 min) |
| `NEWSLETTER_MAX_BLOCKS` | `30` | Maximum content blocks per newsletter |
| `NEWSLETTER_EMAIL_ENABLED` | `true` | Enable email distribution channel |
| `NEWSLETTER_WHATSAPP_ENABLED` | `true` | Enable WhatsApp distribution channel |
| `NEWSLETTER_AI_RATE_LIMIT` | `5` | Max AI generation requests per hour per school |
| `NEWSLETTER_EMAIL_BATCH_SIZE` | `100` | Email batch size for large distributions |
| `NEWSLETTER_WHATSAPP_BATCH_SIZE` | `50` | WhatsApp batch size for large distributions |

### Feature Flags

| Flag | Default | Description |
|---|---|---|
| `NEWSLETTER_ENABLED` | `true` | Enable/disable newsletter feature |
| `NEWSLETTER_AI_ENABLED` | `false` | Enable/disable AI-assisted generation |
| `NEWSLETTER_SCHEDULING_ENABLED` | `true` | Enable/disable scheduling |
| `NEWSLETTER_TRACKING_ENABLED` | `true` | Enable/disable read tracking |

### School-Level Settings

N/A — newsletters are managed entirely by school admin. School branding (logo, name, colors) sourced from `SchoolsTable`.

---

## Appendix E: Migration & Rollback

### Migration: `migration_073_newsletter.sql`

```sql
-- Migration 073: Newsletter Builder
-- Creates 1 new table

BEGIN;

CREATE TABLE IF NOT EXISTS newsletters (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL REFERENCES schools(id),
    title           TEXT NOT NULL,
    issue_number    INTEGER NOT NULL,
    content_blocks  TEXT NOT NULL,
    header_image_url TEXT,
    status          VARCHAR(16) NOT NULL DEFAULT 'draft',
    scheduled_at    TIMESTAMP,
    published_at    TIMESTAMP,
    email_sent_count INTEGER NOT NULL DEFAULT 0,
    email_open_count INTEGER NOT NULL DEFAULT 0,
    in_app_view_count INTEGER NOT NULL DEFAULT 0,
    created_by      UUID REFERENCES app_users(id),
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(school_id, issue_number)
);
CREATE INDEX IF NOT EXISTS idx_newsletters_school ON newsletters(school_id, status, published_at DESC);

COMMIT;
```

### Rollback: `migration_073_rollback.sql`

```sql
BEGIN;
DROP TABLE IF EXISTS newsletters;
COMMIT;
```

### Migration Validation

- Verify `newsletters` table created with correct columns
- Verify FK constraints in place
- Verify `UNIQUE(school_id, issue_number)` constraint works
- Verify index `idx_newsletters_school` created
- Run `SELECT count(*) FROM newsletters` — should be 0 (new feature)

---

## Appendix F: Observability

### Structured Logging

| Log Level | Event | Context Fields |
|---|---|---|
| INFO | Newsletter created | `newsletterId, schoolId, title, issueNumber, createdBy` |
| INFO | Newsletter published | `newsletterId, schoolId, title, channels` |
| INFO | Newsletter scheduled | `newsletterId, schoolId, scheduledAt, channels` |
| INFO | Newsletter auto-published | `newsletterId, schoolId, channels` |
| INFO | Newsletter viewed (in-app) | `newsletterId, schoolId, userId` |
| INFO | Newsletter opened (email) | `newsletterId` |
| INFO | Distribution completed | `newsletterId, channel, sentCount, failedCount` |
| WARN | AI generation failed | `schoolId, month, error` |
| WARN | Email channel unavailable | `newsletterId, schoolId` |
| WARN | WhatsApp channel unavailable | `newsletterId, schoolId` |
| WARN | Distribution partial failure | `newsletterId, channel, failedCount` |
| ERROR | Distribution failed completely | `newsletterId, channel, error` |

### Metrics

| Metric | Type | Labels | Description |
|---|---|---|---|
| `newsletters_total` | Gauge | `school_id, status` | Total newsletters by status |
| `newsletters_published_total` | Counter | `school_id` | Newsletters published |
| `newsletter_email_sent_total` | Counter | `school_id` | Email newsletters sent |
| `newsletter_email_opened_total` | Counter | `newsletter_id` | Email opens tracked |
| `newsletter_in_app_viewed_total` | Counter | `newsletter_id` | In-app views tracked |
| `newsletter_distribution_duration` | Histogram | `channel` | Distribution latency per channel |
| `newsletter_ai_generation_duration` | Histogram | `school_id` | AI generation latency |
| `newsletter_render_duration` | Histogram | `channel` | Rendering latency per channel |

### Health Checks

| Check | Endpoint | Description |
|---|---|---|
| Newsletters table exists | `/health/newsletters` | Verify `newsletters` table is accessible |

### Alerts

| Alert | Condition | Severity | Notification |
|---|---|---|---|
| Distribution failure rate high | `newsletter_distribution_duration` error rate > 10% | Critical | PagerDuty / email |
| AI generation consistently failing | AI error rate > 50% over 1 hour | Warning | Email to dev team |
| Email open rate very low | `newsletter_email_opened_total / newsletter_email_sent_total` < 10% | Info | Log only (content quality) |
| Scheduled newsletter not published | `status='scheduled' AND scheduled_at < now() - 1 hour` | Warning | Email to admin |

### Dashboards

| Dashboard | Panels | Audience |
|---|---|---|
| Newsletter Overview | Total newsletters, published count, scheduled count, publication trend | School Admin |
| Engagement Metrics | Email open rate, in-app view count, tracking over time | School Admin |
| Distribution Health | Sent counts, failure rates, latency per channel | Dev Team |
| AI Usage | AI generation requests, success rate, latency | Dev Team |
