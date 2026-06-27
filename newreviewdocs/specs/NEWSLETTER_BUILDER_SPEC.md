# Newsletter Builder — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** `WHATSAPP_INTEGRATION_SPEC.md`, `EMAIL_INTEGRATION_SPEC.md`
> **Source:** `DIFFERENTIATING_FEATURES.md` §4.3

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

---

## 2. Current System Assessment

- No newsletter system exists
- `DIFFERENTIATING_FEATURES.md` §4.3: Newsletter Builder, effort M
- `AnnouncementsTable` — individual announcements, not curated newsletters
- Supabase Storage — for newsletter images
- Email + WhatsApp infrastructure from respective specs

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Newsletter editor with content blocks: header, text, image, event highlight, announcement summary, achievement, footer |
| FR-2 | School branding: logo, primary color, school name in header |
| FR-3 | AI-assisted: "Generate newsletter from this month's announcements/events" |
| FR-4 | Preview before publish (email preview, WhatsApp preview, in-app preview) |
| FR-5 | Distribute via: email (HTML newsletter), WhatsApp (summary + image), in-app (newsletter card) |
| FR-6 | Newsletter archive: parents can view past newsletters |
| FR-7 | Read tracking: email opens, in-app views |
| FR-8 | Schedule for future delivery (reuse `SCHEDULED_ANNOUNCEMENTS_SPEC.md` pattern) |

---

## 4. Database Design

```sql
CREATE TABLE newsletters (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
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
    created_by      UUID,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(school_id, issue_number)
);
```

---

## 5. Backend Architecture

### 5.1 NewsletterService

```kotlin
class NewsletterService(private val aiService: AiService) {
    suspend fun create(schoolId: UUID, title: String): NewsletterDto
    suspend fun updateContent(newsletterId: UUID, blocks: List<ContentBlock>)
    suspend fun aiGenerateFromMonth(schoolId: UUID, month: YearMonth): List<ContentBlock> {
        // 1. Fetch month's announcements, events, achievements
        // 2. Call AiService to summarize into newsletter content blocks
    }
    suspend fun publish(newsletterId: UUID, channels: List<String>)
    suspend fun getArchive(schoolId: UUID): List<NewsletterDto>
    suspend fun trackOpen(newsletterId: UUID)  // email open pixel
    suspend fun trackView(newsletterId: UUID)  // in-app view
}
```

### 5.2 Distribution

- **Email:** Render content blocks as HTML email → send via `EmailService` to all parents
- **WhatsApp:** Generate summary text + header image → send via `WhatsAppService`
- **In-app:** Create notification with deep link to newsletter view

---

## 6. API Contracts

```
# Admin
GET/POST /api/v1/school/newsletters
PATCH /api/v1/school/newsletters/{id}
POST /api/v1/school/newsletters/{id}/ai-generate  { month: "2026-06" }
POST /api/v1/school/newsletters/{id}/publish  { channels: ["email", "whatsapp", "in_app"] }
GET /api/v1/school/newsletters/{id}/preview

# Parent
GET /api/v1/parent/newsletters
GET /api/v1/parent/newsletters/{id}
POST /api/v1/parent/newsletters/{id}/view  -- track view
```

---

## 7. Acceptance Criteria

- [ ] Visual editor with content blocks (text, image, event, announcement, achievement)
- [ ] School branding applied (logo, colors, header)
- [ ] AI generation from month's events works
- [ ] Preview before publish (email, WhatsApp, in-app)
- [ ] Multi-channel distribution works
- [ ] Newsletter archive viewable by parents
- [ ] Read tracking (email opens, in-app views)
- [ ] Scheduling supported

---

## 8. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration, Exposed table |
| 2 | 2 days | NewsletterService (CRUD, AI generation, distribution) |
| 3 | 2 days | HTML email rendering + WhatsApp summary generation |
| 4 | 1 day | API endpoints + tracking |
| 5 | 4 days | Client UI (visual editor, preview, archive, parent view) |
| 6 | 1 day | Tests |

---

## 9. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | `NewslettersTable` |
| `server/.../feature/newsletter/NewsletterService.kt` | New | Core service |
| `server/.../feature/newsletter/NewsletterRenderer.kt` | New | HTML + WhatsApp rendering |
| `docs/db/migration_073_newsletter.sql` | New | DDL |
| `composeApp/.../ui/v2/screens/admin/NewsletterEditorScreen.kt` | New | Visual editor |
| `composeApp/.../ui/v2/screens/parent/NewsletterViewScreen.kt` | New | Parent newsletter view |
