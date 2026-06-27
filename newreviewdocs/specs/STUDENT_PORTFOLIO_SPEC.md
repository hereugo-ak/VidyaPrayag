# Student Digital Portfolio — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** `VIDYA_PASSPORT_SPEC.md`
> **Source:** `COMPETITIVE_GAP_ANALYSIS.md` — emerging trend

---

## 1. Feature Overview

Digital portfolio for students showcasing their academic work, projects, art, certifications, and achievements over their academic journey. A curated, reflective collection that grows with the student and can be shared for admissions or scholarships.

### Goals

- Student/teacher uploads work samples (essays, projects, art, presentations, certificates)
- Categorized by type: academic, creative, sports, community, certification
- Reflection notes per item (what I learned, what I'd improve)
- Timeline view: portfolio items across years
- Teacher endorsement: teacher can endorse/comment on portfolio items
- Shareable: generate portfolio PDF or shareable link for admissions
- Integration with `VIDYA_PASSPORT_SPEC.md` (badges + achievements auto-added)

---

## 2. Current System Assessment

- `VIDYA_PASSPORT_SPEC.md` — badges, certificates, milestones
- `SchoolMediaTable` — media storage infrastructure
- `HomeworkSubmissionsTable` — student work (can be promoted to portfolio)
- No portfolio system exists
- `COMPETITIVE_GAP_ANALYSIS.md`: student portfolio as emerging trend

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Upload portfolio item: title, type, description, file (image/PDF/doc), date, subject |
| FR-2 | Categories: academic, creative (art/music), sports, community, certification, project |
| FR-3 | Reflection note: student writes what they learned and would improve |
| FR-4 | Teacher endorsement: teacher can endorse items with comment |
| FR-5 | Timeline view: items chronologically across academic years |
| FR-6 | Auto-populate: badges, certificates, achievements from `VIDYA_PASSPORT_SPEC.md` |
| FR-7 | Export: generate portfolio PDF for admissions/scholarships |
| FR-8 | Shareable link: time-limited public link for external viewing |
| FR-9 | Promote homework: teacher can promote a homework submission to portfolio |

---

## 4. Database Design

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

---

## 5. API Contracts

```
# Parent (on behalf of student)
GET/POST /api/v1/parent/portfolio/{childId}
PATCH /api/v1/parent/portfolio/items/{id}
DELETE /api/v1/parent/portfolio/items/{id}
POST /api/v1/parent/portfolio/items/{id}/share  { expires_in_days: 30 }
GET /api/v1/parent/portfolio/{childId}/export

# Teacher
POST /api/v1/teacher/portfolio/items/{id}/endorse  { comment }
POST /api/v1/teacher/portfolio/promote-homework  { submission_id, title, category }

# Public (shareable link)
GET /api/v1/public/portfolio/{shareToken}
```

---

## 6. Acceptance Criteria

- [ ] Portfolio items uploaded with file, category, reflection
- [ ] Timeline view shows items across academic years
- [ ] Teacher can endorse items with comment
- [ ] Auto-populated with achievements from Vidya Passport
- [ ] Export as PDF for admissions
- [ ] Shareable link with expiry
- [ ] Teacher can promote homework to portfolio

---

## 7. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration, Exposed table |
| 2 | 2 days | PortfolioService (CRUD, endorsement, share, export) |
| 3 | 1 day | Achievement auto-population integration |
| 4 | 1 day | API endpoints |
| 5 | 4 days | Client UI (portfolio timeline, upload, reflection, teacher endorsement, PDF export) |
| 6 | 1 day | Tests |

---

## 8. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | `PortfolioItemsTable` |
| `server/.../feature/portfolio/PortfolioService.kt` | New | Core service |
| `docs/db/migration_088_student_portfolio.sql` | New | DDL |
| `composeApp/.../ui/v2/screens/parent/PortfolioScreen.kt` | New | Portfolio timeline |
| `composeApp/.../ui/v2/screens/teacher/PortfolioReviewScreen.kt` | New | Teacher endorsement |
