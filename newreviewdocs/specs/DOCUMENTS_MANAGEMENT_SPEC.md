# Documents Management — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** None

---

## 1. Feature Overview

Centralized document management for schools: store, organize, and share documents (circulars, notices, certificates, policies, forms) with role-based access, versioning, and expiry tracking.

### Goals

- Admin uploads documents categorized by type (circular, policy, form, certificate template, notice)
- Documents organized in folders/categories
- Role-based access: some docs public to parents, some admin-only
- Document versioning (replace with new version, keep history)
- Expiry tracking for time-sensitive documents
- Parent/teacher can view documents they have access to
- Download tracking (who downloaded what)

---

## 2. Current System Assessment

- `SchoolMediaTable` (`Tables.kt:355-370`) — generic media storage with `mediaType`, `url`, `uploadedBy`
- `feature_audit.csv` L141: Document Management missing (0%)
- Supabase Storage integration exists for file uploads
- No document categorization, access control, or versioning

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Admin uploads document with: title, category, description, file, access level |
| FR-2 | Categories: circular, policy, form, certificate, notice, other |
| FR-3 | Access levels: admin_only | teacher | parent | public |
| FR-4 | Document versioning: upload new version, keep version history |
| FR-5 | Expiry date for time-sensitive documents (auto-archive after expiry) |
| FR-6 | Parent/teacher views documents based on access level |
| FR-7 | Download tracking: log who downloaded and when |
| FR-8 | Search by title, category, date range |

---

## 4. Database Design

```sql
CREATE TABLE document_categories (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    name            VARCHAR(48) NOT NULL,          -- "Circulars", "Policies", "Forms"
    icon            VARCHAR(32),
    sort_order      INTEGER NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    UNIQUE(school_id, name)
);

CREATE TABLE documents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    category_id     UUID REFERENCES document_categories(id),
    title           TEXT NOT NULL,
    description     TEXT,
    access_level    VARCHAR(16) NOT NULL DEFAULT 'admin_only', -- admin_only | teacher | parent | public
    current_version_id UUID,                       -- FK document_versions.id
    expiry_date     DATE,
    status          VARCHAR(16) NOT NULL DEFAULT 'active', -- active | archived
    uploaded_by     UUID,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_documents_school_access ON documents(school_id, access_level, status);

CREATE TABLE document_versions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id     UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    version_number  INTEGER NOT NULL,
    file_url        TEXT NOT NULL,                 -- Supabase Storage URL
    file_name       TEXT NOT NULL,
    file_size_bytes BIGINT,
    mime_type       VARCHAR(64),
    uploaded_by     UUID,
    uploaded_at     TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(document_id, version_number)
);

CREATE TABLE document_downloads (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id     UUID NOT NULL REFERENCES documents(id),
    user_id         UUID NOT NULL,
    user_name       TEXT NOT NULL,
    user_role       VARCHAR(32) NOT NULL,
    downloaded_at   TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_doc_downloads_document ON document_downloads(document_id, downloaded_at DESC);
```

---

## 5. API Contracts

```
# Admin
GET/POST /api/v1/school/documents
PATCH /api/v1/school/documents/{id}
DELETE /api/v1/school/documents/{id}
POST /api/v1/school/documents/{id}/new-version  { file_url, file_name }
GET /api/v1/school/documents/{id}/downloads
GET/POST /api/v1/school/document-categories

# Parent/Teacher
GET /api/v1/parent/documents?category={name}
GET /api/v1/teacher/documents?category={name}
GET /api/v1/parent/documents/{id}/download
GET /api/v1/teacher/documents/{id}/download
```

---

## 6. Acceptance Criteria

- [ ] Admin uploads documents with category and access level
- [ ] Document versioning preserves history
- [ ] Access levels enforced (parent can't see admin_only docs)
- [ ] Expiry auto-archives documents
- [ ] Download tracking logs user, role, timestamp
- [ ] Search by title, category, date range
- [ ] Parent/teacher can view and download accessible documents

---

## 7. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration, Exposed tables |
| 2 | 2 days | DocumentService (CRUD, versioning, access control) |
| 3 | 1 day | Download tracking + expiry job |
| 4 | 1 day | API endpoints |
| 5 | 2 days | Client UI (document list, upload, version history, parent/teacher view) |
| 6 | 1 day | Tests |

---

## 8. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | 4 document tables |
| `server/.../feature/documents/DocumentService.kt` | New | Core service |
| `server/.../feature/documents/DocumentRouting.kt` | New | API endpoints |
| `docs/db/migration_061_documents_management.sql` | New | DDL |
| `composeApp/.../ui/v2/screens/admin/DocumentsScreen.kt` | New | Admin document management |
| `composeApp/.../ui/v2/screens/parent/DocumentsScreen.kt` | New | Parent document view |
