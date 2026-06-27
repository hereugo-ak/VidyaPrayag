# Library Management — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** None

---

## 1. Feature Overview

School library management: book catalog, issue/return tracking, reservations, fines, and student reading history.

### Goals

- Admin/librarian manages book catalog (title, author, ISBN, copies, category)
- Issue/return books to students/teachers with due dates
- Reservations for unavailable books
- Fine calculation for overdue returns
- Student reading history
- Search by title, author, ISBN, category

---

## 2. Current System Assessment

- `feature_audit.csv` Gap #7: Library missing (0%)
- No library tables in `Tables.kt`
- `StudentsTable` exists for student lookup

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Book catalog CRUD with ISBN, title, author, publisher, category, copies, shelf location |
| FR-2 | Issue book to student/teacher with due date (default 14 days) |
| FR-3 | Return book, calculate fine if overdue (₹1/day configurable) |
| FR-4 | Reserve unavailable book — notified when available |
| FR-5 | Search by title, author, ISBN, category |
| FR-6 | Student reading history |
| FR-7 | Library dashboard: total books, issued, available, overdue |

---

## 4. Database Design

```sql
CREATE TABLE library_books (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    isbn            VARCHAR(20),
    title           TEXT NOT NULL,
    author          TEXT,
    publisher       TEXT,
    category        VARCHAR(48),
    total_copies    INTEGER NOT NULL DEFAULT 1,
    available_copies INTEGER NOT NULL DEFAULT 1,
    shelf_location  VARCHAR(32),
    cover_url       TEXT,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE library_issues (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    book_id         UUID NOT NULL REFERENCES library_books(id),
    borrower_id     UUID NOT NULL,                 -- FK app_users.id or students.id
    borrower_type   VARCHAR(16) NOT NULL,          -- student | teacher
    borrower_name   TEXT NOT NULL,
    issue_date      DATE NOT NULL,
    due_date        DATE NOT NULL,
    return_date     DATE,
    fine_amount     DOUBLE PRECISION NOT NULL DEFAULT 0,
    status          VARCHAR(16) NOT NULL DEFAULT 'issued', -- issued | returned | lost
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_library_issues_borrower ON library_issues(borrower_id, status);

CREATE TABLE library_reservations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    book_id         UUID NOT NULL REFERENCES library_books(id),
    reserved_by     UUID NOT NULL,
    reserved_by_name TEXT NOT NULL,
    status          VARCHAR(16) NOT NULL DEFAULT 'pending', -- pending | fulfilled | cancelled
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    fulfilled_at    TIMESTAMP
);
```

---

## 5. API Contracts

```
# Admin/Librarian
GET/POST /api/v1/school/library/books
PATCH/DELETE /api/v1/school/library/books/{id}
POST /api/v1/school/library/issue  { book_id, borrower_id, borrower_type }
POST /api/v1/school/library/return  { issue_id }
GET /api/v1/school/library/dashboard
GET /api/v1/school/library/issues?status=issued&overdue=true

# Parent/Student
GET /api/v1/parent/library/search?q={query}
GET /api/v1/parent/library/issued-books/{childId}
POST /api/v1/parent/library/reserve  { book_id }
```

---

## 6. Acceptance Criteria

- [ ] Book catalog CRUD works
- [ ] Issue/return with due date and fine calculation
- [ ] Search by title, author, ISBN, category
- [ ] Reservation for unavailable books
- [ ] Student reading history
- [ ] Library dashboard with counts

---

## 7. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 2 days | DB migration, Exposed tables, services |
| 2 | 2 days | API endpoints |
| 3 | 3 days | Client UI (catalog, issue/return, search, dashboard) |
| 4 | 1 day | Tests |

---

## 8. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | 3 library tables |
| `server/.../feature/library/*.kt` | New | Services + routing |
| `docs/db/migration_046_library.sql` | New | DDL |
| `composeApp/.../ui/v2/screens/admin/LibraryScreen.kt` | New | Library management |
| `composeApp/.../ui/v2/screens/parent/LibrarySearchScreen.kt` | New | Book search + issued books |
