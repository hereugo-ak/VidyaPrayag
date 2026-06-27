# Alumni Management — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** None

---

## 1. Feature Overview

Alumni directory and engagement platform: maintain alumni records, track career progression, facilitate mentorship, organize alumni events, and enable alumni donations/contributions.

### Goals

- Maintain alumni database (graduated students with graduation year, current profession, contact)
- Alumni directory searchable by graduation year, profession, location
- Alumni events and reunions
- Mentorship program: alumni mentor current students
- Alumni newsletter/announcements
- Donation/contribution tracking

---

## 2. Current System Assessment

- `feature_audit.csv` L140: Alumni Management missing (0%)
- `StudentsTable` + `EnrollmentsTable` — current students; no alumni concept
- No alumni tables in `Tables.kt`

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Admin marks graduated students as alumni (graduation year, last class) |
| FR-2 | Alumni profile: current profession, company, location, contact info, LinkedIn |
| FR-3 | Alumni directory searchable by graduation year, profession, location |
| FR-4 | Alumni events/reunions (reuse `CalendarEventsTable` + `EventRegistrationsTable`) |
| FR-5 | Mentorship: alumni volunteers as mentor, matched with current students |
| FR-6 | Alumni newsletter (reuse announcements with alumni audience) |
| FR-7 | Donation tracking: alumni contributions with purpose and amount |

---

## 4. Database Design

```sql
CREATE TABLE alumni (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    student_id      UUID,                          -- FK students.id (if linked)
    name            TEXT NOT NULL,
    graduation_year INTEGER NOT NULL,
    last_class      TEXT,                          -- "Grade 12", "Grade 10"
    current_profession TEXT,
    company         TEXT,
    city            TEXT,
    email           TEXT,
    phone           VARCHAR(32),
    linkedin_url    TEXT,
    photo_url       TEXT,
    is_mentor       BOOLEAN NOT NULL DEFAULT false,
    mentor_expertise TEXT,                         -- "Engineering", "Medicine", "Finance"
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_alumni_school_year ON alumni(school_id, graduation_year);

CREATE TABLE alumni_donations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    alumni_id       UUID NOT NULL REFERENCES alumni(id),
    alumni_name     TEXT NOT NULL,
    amount          DOUBLE PRECISION NOT NULL,
    purpose         TEXT,                          -- "Scholarship Fund", "Library", "General"
    donation_date   DATE NOT NULL,
    payment_mode    VARCHAR(16),                   -- upi | bank_transfer | cheque
    reference_number TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_alumni_donations_school ON alumni_donations(school_id, donation_date DESC);

CREATE TABLE alumni_mentorships (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    alumni_id       UUID NOT NULL REFERENCES alumni(id),
    student_id      UUID NOT NULL,
    status          VARCHAR(16) NOT NULL DEFAULT 'active', -- active | ended
    start_date      DATE NOT NULL,
    end_date        DATE,
    notes           TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
```

---

## 5. API Contracts

```
# Admin
GET/POST /api/v1/school/alumni
PATCH /api/v1/school/alumni/{id}
GET /api/v1/school/alumni/donations
POST /api/v1/school/alumni/donations
GET /api/v1/school/alumni/mentorships
POST /api/v1/school/alumni/mentorships  { alumni_id, student_id }

# Alumni (self-service, if they have login)
GET /api/v1/alumni/profile
PATCH /api/v1/alumni/profile
POST /api/v1/alumni/mentor-volunteer  { expertise }
```

---

## 6. Acceptance Criteria

- [ ] Admin can mark graduated students as alumni
- [ ] Alumni directory searchable by year, profession, location
- [ ] Alumni events/reunions using existing event system
- [ ] Mentorship program: alumni mentor students
- [ ] Donation tracking with purpose and amount
- [ ] Alumni newsletter via announcements

---

## 7. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration, Exposed tables |
| 2 | 2 days | AlumniService (CRUD, search, mentorship, donations) |
| 3 | 1 day | API endpoints |
| 4 | 2 days | Client UI (directory, profile, donations, mentorship management) |
| 5 | 1 day | Tests |

---

## 8. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | 3 alumni tables |
| `server/.../feature/alumni/AlumniService.kt` | New | Core service |
| `docs/db/migration_066_alumni_management.sql` | New | DDL |
| `composeApp/.../ui/v2/screens/admin/AlumniScreen.kt` | New | Alumni management |
