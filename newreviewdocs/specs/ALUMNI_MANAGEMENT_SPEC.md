# Alumni Management — Technical Specification

> **Document status:** Implemented (85%) — core alumni CRUD, directory, campaigns, donations, mentorship, career history, deep linking, web admin pages shipped; Phase 2 self-service alumni UI deferred
> **Last updated:** 2026-06-28
> **Prerequisites:** None for core features. FR-5 (event RSVP) requires `EVENT_REGISTRATION_SPEC.md` — `EventRegistrationsTable` does not exist yet.
> **Industry benchmark:** SchoolDeck, CodePex ERP, Vidyalaya, Edumerge, Alumnipad, Hivebrite, 360Alumni, Edulab
> **Template:** `_SPEC_TEMPLATE.md` v1 (25 mandatory + 6 optional sections)

---

## 1. Feature Overview

Alumni directory and engagement platform for K-12 schools: maintain alumni records, track career progression, facilitate mentorship for Class 11-12 students, organize alumni events, enable donation campaigns with 80G-compliant receipts, and keep alumni engaged through targeted newsletters.

### Goals

- Maintain alumni database (graduated students with graduation year, last class, current profession, contact)
- **SIS-verified alumni self-registration** — auto-match against school's student records (year of passing + admission/roll number)
- **Student lifecycle transition** — automatic transition from active student → graduated → alumni
- Alumni directory searchable by graduation year, profession, location, company, industry
- Alumni events and reunions (reuse `CalendarEventsTable`)
- Structured mentorship program: alumni mentor Class 11-12 students on career choices
- Alumni newsletter/announcements with batch-wise targeting
- Donation campaigns with goals, progress tracking, and 80G-compliant receipts
- **Privacy controls** — alumni control field-level visibility (DPDP Act 2023 compliant)
- **Analytics dashboard** — engagement metrics, donation reports, career outcome tracking
- **Bulk import** past student rosters from SIS/CSV
- **Career progression tracking** — employment history over time

### Non-goals

- [ ] Paid event ticketing for alumni events
- [ ] Alumni job board (K-12 scope, not university)
- [ ] Alumni social feed (future: Class Community Feed spec)
- [ ] Alumni ID card generation
- [ ] Alumni membership tiers (K-12 schools don't charge membership)
- [ ] AI-powered career recommendations
- [ ] Connection graph / networking visualization

### Dependencies

- `CalendarEventsTable` — existing school calendar (modified with `ALUMNI` audience)
- `NotificationService` — existing notification infrastructure (modified with alumni resolver)
- `WhatsappLogsTable` — existing WhatsApp infrastructure (reused for alumni outreach)
- `SupabaseStorage` — existing file storage (reused for alumni photos)
- `EventRegistrationsTable` — **does not exist yet** (FR-5 RSVP blocked until event registration ships)

### Related Modules

- `server/.../feature/alumni/` — new alumni management module
- `server/.../feature/announcements/` — modified for `ALUMNI` audience type
- `server/.../feature/notifications/` — modified for alumni recipient resolver
- `shared/.../feature/alumni/` — shared alumni DTOs and API
- `composeApp/.../ui/v2/screens/school/` — admin UI
- `composeApp/.../ui/v2/screens/alumni/` — alumni self-service UI (Phase 2)
- `website/src/app/admin/alumni/` — web admin dashboard

### K-12 Specific Design Decisions

Based on industry research (SchoolDeck, CodePex):
- **Emotional anchor:** batch identity (Class of 1995), school house, sports teams — not departments/degrees
- **Donation scale:** ₹5,000-₹50,000 for library fund, scholarships, infrastructure — not endowments
- **Mentorship model:** alumni mentor Class 11-12 students on career choices — not internships/first jobs
- **Verification:** year of passing + admission/roll number match against SIS records
- **NEP 2020 §4.38:** "Schools shall establish mechanisms for alumni engagement to support current students through mentorship, career guidance, and community projects"

### Industry Benchmarking

Based on research of SchoolDeck, CodePex ERP, Vidyalaya, Edumerge, Alumnipad, Hivebrite, 360Alumni, Edulab, and AlumniReady:

| Feature | Industry Standard | Our Spec | Status |
|---|---|---|---|
| Alumni directory with search | All platforms | FR-3 — search by year, profession, city, company, industry | Covered |
| SIS-verified registration | SchoolDeck, CodePex | FR-4 — auto-match year + admission number, admin review queue | Covered |
| Student lifecycle transition | Edumerge, CodePex | Section 5 — enrollment status `graduated`, auto-create alumni record | Covered |
| Rich alumni profiles | All platforms | FR-2 — profession, company, skills, achievements, LinkedIn, career history | Covered |
| Profile completeness | Edulab, AlumniReady | FR-15 — derived 0-100% metric | Covered |
| Donation tracking | All platforms | FR-8 — individual donations with purpose, amount, payment mode | Covered |
| Donation campaigns | Hivebrite, Alumnipad, SchoolDeck | FR-9 — campaigns with goals, progress, batch targeting | Covered |
| 80G tax receipts | SchoolDeck (India-specific) | FR-10 — receipt with PAN, 80G number, Form 10BD export | Covered |
| Mentorship program | All platforms | FR-6 — two-way flow: alumni opt-in, student request, accept/decline | Covered |
| Events & reunions | All platforms | FR-5 — reuse `CalendarEventsTable` (RSVP deferred) | Partial (depends on event registration) |
| Newsletter/announcements | All platforms | FR-7 — reuse announcements with `ALUMNI` audience + batch targeting | Covered |
| Privacy controls | SchoolDeck, Hivebrite | FR-11 — field-level visibility, DPDP Act 2023 compliant | Covered |
| Bulk import | Edulab, AlumniReady, Edumerge | FR-12 — CSV/JSON import matching existing student import pattern | Covered |
| Analytics dashboard | All platforms | FR-13 — engagement, donations, career outcomes, growth | Covered |
| Career tracking | Edulab, vmedulife | FR-14 — career history table with timestamps | Covered |
| Featured alumni | Edulab | FR-16 — admin spotlight in directory | Covered |
| WhatsApp integration | Alumnipad, CodePex | FR-17 — reuse existing WhatsApp infra for outreach | Covered |
| Job board | Edulab, AlumniReady, 360Alumni | — | Not in scope (K-12, not university) |
| Social feed | Edulab, AlumniReady, Hivebrite | — | Not in scope (future: Class Community Feed spec) |
| Alumni ID card | Edulab | — | Not in scope |
| Connection graph | Edulab | — | Not in scope (future) |
| AI career recommendations | Edulab | — | Not in scope (future: AI infrastructure) |
| Membership tiers | AlumniReady | — | Not in scope (K-12 schools don't charge membership) |

---

## 2. Current System Assessment

### Existing Code

- `feature_audit.csv` L140: Alumni Management missing (0%)
- `StudentsTable` + `EnrollmentsTable` — current students; no alumni concept
- `EnrollmentsTable.status` supports `active | transferred | withdrawn` — **no `graduated` status**
- `StudentsTable.isActive` — boolean soft-delete; no graduation concept
- No alumni tables in `Tables.kt`
- **No `"alumni"` role exists** in `AppUsersTable.role` — current roles: `parent`, `teacher`, `school_admin`, `school_staff`, `admin`
- **`EventRegistrationsTable` does not exist** — only defined in unimplemented `EVENT_REGISTRATION_SPEC.md`
- **`CalendarEventsTable` exists** (`Tables.kt:1576`) with `audience` field supporting `ALL_SCHOOL | GRADES | CLASSES | SECTIONS | TEACHERS | PARENTS | STUDENTS` — no `ALUMNI` audience
- **Announcement audience types** (`AnnouncementRouting.kt:109`): `ALL_SCHOOL | CLASS | SECTION | SUBJECT | STUDENT | CUSTOM` — no `ALUMNI` type
- **`NotifyRecipients.kt`** resolves parents/teachers only — no alumni recipient resolver
- **WhatsApp infrastructure exists** — `WhatsappLogsTable`, WhatsApp messaging in announcements; can be reused for alumni outreach
- **Bulk import pattern exists** — `SchoolStudentsRouting.kt` has CSV/JSON bulk import for students; same pattern applies to alumni
- **Latest migration:** `migration_051_parent_pulse.sql` — next is `052`

### Existing Database

- `StudentsTable` — student records with `studentCode` (globally unique), `rollNumber`, `isActive`
- `EnrollmentsTable` — enrollment records with `status` (active/transferred/withdrawn), `endDate` (nullable)
- `AppUsersTable` — user accounts with `role`, `schoolId` (nullable), `phone`, `email`, `passwordHash`
- `CalendarEventsTable` — school calendar events with `audience` field
- `NotificationPreferencesTable` — notification preferences per user
- `WhatsappLogsTable` — WhatsApp message logs
- `SchoolsTable` — school records with `slug`, `id` (UUID), no `school_code`

### Existing APIs

- School calendar CRUD (admin)
- Announcement CRUD with audience targeting
- Student bulk import (CSV/JSON)
- Auth: OTP login, password login, signup (parent-only via RA-53)
- File upload via `MediaRouting.kt` (school-scoped, `requireSchoolContext()`)

### Existing UI

- School portal with People tab (`SchoolPortalV2.kt`)
- Student roster with bulk actions (`SchoolStudentsRouting.kt`)
- Calendar view
- Announcement management
- Web admin dashboard (`website/src/app/admin/`)

### Existing Services

- `NotificationService` — multi-channel notifications (push, in-app, WhatsApp)
- `NotifyRecipients.kt` — recipient resolution by audience type
- `SupabaseStorage` — file storage with kind-based paths
- `CalendarService` — calendar event management

### Existing Documentation

- `feature_audit.csv` — alumni management at 0%
- `IMPLEMENTATION_BACKLOG` — P1-28 entry
- `EVENT_REGISTRATION_SPEC.md` — defines `EventRegistrationsTable` (not yet implemented)

### Technical Debt

| # | Gap | Details |
|---|---|---|
| TD-1 | No alumni concept | No alumni tables, no alumni role, no graduation status |
| TD-2 | No `graduated` enrollment status | `EnrollmentsTable.status` missing `graduated` value |
| TD-3 | No alumni role in auth | `AppUsersTable.role` has no `"alumni"` value |
| TD-4 | No alumni audience type | Announcements and calendar events can't target alumni |
| TD-5 | No alumni recipient resolver | `NotifyRecipients.kt` can't resolve alumni recipients |
| TD-6 | No `school_code` on SchoolsTable | Self-registration can't look up school by code |
| TD-7 | No 80G certificate storage | `SchoolsTable` missing PAN, 80G registration fields |
| TD-8 | No PDF generation library | Server can't generate 80G receipt PDFs |

### Gaps

| # | Gap | Impact | Severity |
|---|---|---|---|
| G1 | No alumni database | Can't track or engage alumni | **High** |
| G2 | No graduation flow | Students can't transition to alumni | **High** |
| G3 | No alumni self-registration | Alumni can't self-serve | **Medium** |
| G4 | No donation tracking | Can't manage alumni donations | **Medium** |
| G5 | No mentorship program | Can't facilitate alumni-student mentorship | **Medium** |
| G6 | No privacy controls | DPDP Act 2023 non-compliant | **High** |
| G7 | No analytics | Can't measure engagement | **Low** |

---

## 3. Functional Requirements

### FR-001
| Field | Value |
|---|---|
| **Title** | Mark Graduated Students as Alumni |
| **Description** | Admin marks graduated students as alumni (graduation year, last class). Auto-creates alumni record from student data. |
| **Priority** | Critical |
| **User Roles** | School Admin |
| **Acceptance notes** | Requires `requireSchoolAdmin()`. Auto-creates alumni record from student data. Bulk graduation supported. |

### FR-002
| Field | Value |
|---|---|
| **Title** | Alumni Profile |
| **Description** | Alumni profile: profession, company, location, contact, LinkedIn, skills, achievements. Rich profile with career history and profile completeness indicator. |
| **Priority** | Critical |
| **User Roles** | School Admin, Alumni (self-service) |
| **Acceptance notes** | Profile completeness derived metric (0-100%). |

### FR-003
| Field | Value |
|---|---|
| **Title** | Alumni Directory Search |
| **Description** | Alumni directory searchable by graduation year, profession, location, company, industry. |
| **Priority** | Critical |
| **User Roles** | School Admin, Alumni (privacy-filtered) |
| **Acceptance notes** | API: `?year=&profession=&city=&company=&industry=&q=&page=&limit=` |

### FR-004
| Field | Value |
|---|---|
| **Title** | Alumni Self-Registration with SIS Verification |
| **Description** | Alumni self-registration with SIS verification. Auto-match year of passing + admission/roll number. Mismatches → admin review queue. |
| **Priority** | High |
| **User Roles** | Alumni (public), School Admin (verification) |
| **Acceptance notes** | Auto-approve on SIS match; pending queue for mismatches. |

### FR-005
| Field | Value |
|---|---|
| **Title** | Alumni Events/Reunions |
| **Description** | Alumni events/reunions (reuse `CalendarEventsTable`). |
| **Priority** | Medium |
| **User Roles** | School Admin |
| **Acceptance notes** | **RSVP BLOCKED** — `EventRegistrationsTable` not implemented. Display events via `CalendarEventsTable` with `ALUMNI` audience. Defer ticketing/RSVP until event registration ships. |

### FR-006
| Field | Value |
|---|---|
| **Title** | Structured Mentorship Program |
| **Description** | Alumni volunteer as mentor, students request mentorship. Two-way flow: alumni opt-in → students browse by industry → request → alumni accept/decline. Session tracking with feedback. |
| **Priority** | High |
| **User Roles** | Alumni, Student/Parent, School Admin |
| **Acceptance notes** | Admin-configurable scope (eligible classes, max mentees). |

### FR-007
| Field | Value |
|---|---|
| **Title** | Alumni Newsletter |
| **Description** | Alumni newsletter (reuse announcements with `ALUMNI` audience). Batch-wise targeting: `ALUMNI` + optional `graduation_year` filter. |
| **Priority** | High |
| **User Roles** | School Admin |
| **Acceptance notes** | Requires `ALUMNI` audience type + `NotifyRecipients` resolver. |

### FR-008
| Field | Value |
|---|---|
| **Title** | Donation Tracking |
| **Description** | Donation tracking with purpose, amount, payment mode. Individual donation records with receipt generation. |
| **Priority** | High |
| **User Roles** | School Admin |
| **Acceptance notes** | Manual entry only for Phase 1. Payment gateway in Phase 2+. |

### FR-009
| Field | Value |
|---|---|
| **Title** | Donation Campaigns |
| **Description** | Donation campaigns with goals, progress meters, batch targeting. Campaign creation: cause, target amount, deadline. Live progress. Target specific batches. |
| **Priority** | High |
| **User Roles** | School Admin |
| **Acceptance notes** | `alumni_donation_campaigns` table with `amount_raised` cache. |

### FR-010
| Field | Value |
|---|---|
| **Title** | 80G-Compliant Tax Receipts |
| **Description** | Receipt with trust PAN, 80G registration number, donor details, unique receipt ID. Form 10BD export. Cash > ₹2,000 blocked. |
| **Priority** | High |
| **User Roles** | School Admin |
| **Acceptance notes** | School must have valid 80G certificate. PDF generation via OpenPDF. |

### FR-011
| Field | Value |
|---|---|
| **Title** | Privacy Controls |
| **Description** | Alumni control field-level visibility. Hide phone, mask email, show only LinkedIn, restrict to batch year, or go fully private. DPDP Act 2023 compliant. |
| **Priority** | Critical |
| **User Roles** | Alumni (self-service) |
| **Acceptance notes** | `show_phone`, `show_email`, `show_linkedin`, `visibility_level` (public/batch/private). |

### FR-012
| Field | Value |
|---|---|
| **Title** | Bulk Import |
| **Description** | Bulk import past student rosters. CSV/JSON import matching `SchoolStudentsRouting.kt` pattern. Auto-creates alumni records. |
| **Priority** | Medium |
| **User Roles** | School Admin |
| **Acceptance notes** | All imported records get `verification_status = 'approved'`, `user_id = null`. |

### FR-013
| Field | Value |
|---|---|
| **Title** | Analytics Dashboard |
| **Description** | Analytics dashboard: engagement, donations, career outcomes, growth. Registration growth, active vs inactive, event participation, donation totals, career distribution. |
| **Priority** | Medium |
| **User Roles** | School Admin |
| **Acceptance notes** | Four analytics endpoints: overview, engagement, donations, career. |

### FR-014
| Field | Value |
|---|---|
| **Title** | Career Progression Tracking |
| **Description** | Employment history with timestamps. Alumni update own career info via self-service. |
| **Priority** | Medium |
| **User Roles** | Alumni (self-service), School Admin |
| **Acceptance notes** | `alumni_career_history` table with `is_current` flag. |

### FR-015
| Field | Value |
|---|---|
| **Title** | Profile Completeness Indicator |
| **Description** | Derived metric: % of profile fields filled. Prompt alumni to complete profiles. |
| **Priority** | Low |
| **User Roles** | Alumni, School Admin |
| **Acceptance notes** | Computed on read. Formula: (filled_fields / total_fields) * 100. |

### FR-016
| Field | Value |
|---|---|
| **Title** | Featured Alumni Spotlight |
| **Description** | Admin can mark alumni as "featured" for directory highlight. |
| **Priority** | Low |
| **User Roles** | School Admin |
| **Acceptance notes** | `is_featured` column + toggle endpoint. |

### FR-017
| Field | Value |
|---|---|
| **Title** | WhatsApp Outreach |
| **Description** | Reuse existing WhatsApp infrastructure for event invitations, donation drives, newsletter delivery. |
| **Priority** | Medium |
| **User Roles** | System |
| **Acceptance notes** | Reuse `WhatsappLogsTable` and announcement WhatsApp sync. |

---

## 4. User Stories

### School Admin
- [ ] Mark graduated students as alumni (bulk graduation)
- [ ] Create/edit/deactivate alumni records
- [ ] Bulk import past student rosters as alumni
- [ ] Review and approve/decline pending self-registrations
- [ ] Search alumni directory by year, profession, city, company, industry
- [ ] Create and manage donation campaigns
- [ ] Log donations and generate 80G receipts
- [ ] Export Form 10BD for annual tax filing
- [ ] Create and manage mentorships
- [ ] View analytics dashboard (engagement, donations, career)
- [ ] Mark alumni as featured
- [ ] Send alumni newsletters with batch-wise targeting

### Alumni (Self-Service — Phase 2)
- [ ] Self-register with SIS verification
- [ ] View and edit own profile
- [ ] Manage privacy settings (field-level visibility)
- [ ] Add career history entries
- [ ] Volunteer as mentor
- [ ] View and respond to mentorship requests
- [ ] Search alumni directory (privacy-filtered)
- [ ] View own donation history
- [ ] View active donation campaigns

### Student/Parent
- [ ] Browse alumni mentors by industry
- [ ] Request mentorship from alumni
- [ ] Receive notification when mentorship request is accepted/declined

### System
- [ ] Auto-create alumni record on graduation
- [ ] Auto-approve SIS-matched self-registrations
- [ ] Send notifications to alumni (push, WhatsApp, email)
- [ ] Generate 80G receipt PDFs
- [ ] Calculate profile completeness
- [ ] Track engagement metrics (last_active_at)

---

## 5. Business Rules

### BR-001
**Rule:** Graduated students transition to alumni automatically.
**Enforcement:** `EnrollmentsTable.status = 'graduated'`, `endDate = graduation_date`, `StudentsTable.isActive = false`, auto-create `AlumniTable` record.

### BR-002
**Rule:** SIS verification required for self-registration.
**Enforcement:** Match `year_of_passing + admission_number` against `StudentsTable`. Match → auto-approve. No match → pending review queue.

### BR-003
**Rule:** One alumni record per person per school.
**Enforcement:** `AlumniTable` unique on `(school_id, student_id)` where `student_id IS NOT NULL`. Self-registration checks for existing record.

### BR-004
**Rule:** Cash donations above ₹2,000 are not 80G-eligible.
**Enforcement:** If `payment_mode = 'cash' AND amount > 2000`, set `is_80g_eligible = false`.

### BR-005
**Rule:** 80G receipts require valid school 80G certificate.
**Enforcement:** Check `schools.g80_registration_number IS NOT NULL AND g80_validity_date >= donation_date` before generating receipt.

### BR-006
**Rule:** Privacy controls apply to alumni-to-alumni directory only.
**Enforcement:** Admin endpoints see full data. Directory endpoint applies `visibility_level` and field-level masking.

### BR-007
**Rule:** Mentorship scope is admin-configurable.
**Enforcement:** `MentorshipSettings` per school: `eligibleClassIds`, `maxMenteesPerAlumni` (default 5), `requestApprovalRequired` (default true).

### BR-008
**Rule:** Alumni login uses existing OTP/password auth.
**Enforcement:** Alumni `app_users` rows have `role = 'alumni'`, `phone` (for OTP), `email + passwordHash` (for password login). `AlumniAccess.kt` resolves alumni context from JWT.

### BR-009
**Rule:** `school_code` auto-generated and unique.
**Enforcement:** Format: `UPPER(LEFT(slug,4)) + '-' + random 4 chars` (e.g., `DPS-AB12`). Unique index on `schools.school_code`.

### BR-010
**Rule:** Alumni use the same Compose app with role-based redirect.
**Enforcement:** `NavGraphV2.kt` checks `role` claim from JWT; redirects to alumni dashboard if `role = "alumni"`.

---

## 6. Database Design

### 6.1 Entity Relationship Summary

Six new tables: `alumni` (master records), `alumni_donation_campaigns` (campaign management), `alumni_donations` (individual donations with 80G fields), `alumni_mentorship_requests` (student-initiated requests), `alumni_mentorships` (active mentorship relationships), `alumni_career_history` (employment timeline). Plus modifications to `schools` (80G fields, school_code), `EnrollmentsTable` (graduated status), `CalendarEventsTable` (ALUMNI audience).

### 6.2 New Tables

#### `alumni` table

```sql
CREATE TABLE alumni (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL REFERENCES schools(id),
    student_id      UUID REFERENCES students(id),
    user_id         UUID REFERENCES app_users(id),
    name            TEXT NOT NULL,
    graduation_year INTEGER NOT NULL,
    last_class      TEXT,
    current_profession TEXT,
    company         TEXT,
    city            TEXT,
    email           TEXT,
    phone           VARCHAR(32),
    linkedin_url    TEXT,
    photo_url       TEXT,
    skills          TEXT,
    achievements    TEXT,
    is_mentor       BOOLEAN NOT NULL DEFAULT false,
    mentor_expertise TEXT,
    is_featured     BOOLEAN NOT NULL DEFAULT false,
    verification_status VARCHAR(16) NOT NULL DEFAULT 'approved',
    verified_at     TIMESTAMP,
    verified_by     UUID REFERENCES app_users(id),
    show_phone      BOOLEAN NOT NULL DEFAULT false,
    show_email      BOOLEAN NOT NULL DEFAULT false,
    show_linkedin   BOOLEAN NOT NULL DEFAULT true,
    visibility_level VARCHAR(16) NOT NULL DEFAULT 'batch',
    last_active_at  TIMESTAMP,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_alumni_school_year ON alumni(school_id, graduation_year);
CREATE INDEX idx_alumni_user_id ON alumni(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_alumni_verification ON alumni(school_id, verification_status) WHERE verification_status = 'pending';
CREATE INDEX idx_alumni_featured ON alumni(school_id, is_featured) WHERE is_featured = true;
```

#### `alumni_donation_campaigns` table

```sql
CREATE TABLE alumni_donation_campaigns (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL REFERENCES schools(id),
    title           TEXT NOT NULL,
    description     TEXT,
    cause           TEXT,
    target_amount   DOUBLE PRECISION NOT NULL,
    amount_raised   DOUBLE PRECISION NOT NULL DEFAULT 0,
    target_batch_year INTEGER,
    start_date      DATE NOT NULL,
    end_date        DATE,
    status          VARCHAR(16) NOT NULL DEFAULT 'active',
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_alumni_campaigns_school ON alumni_donation_campaigns(school_id, status);
```

#### `alumni_donations` table

```sql
CREATE TABLE alumni_donations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL REFERENCES schools(id),
    alumni_id       UUID NOT NULL REFERENCES alumni(id) ON DELETE CASCADE,
    campaign_id     UUID REFERENCES alumni_donation_campaigns(id),
    amount          DOUBLE PRECISION NOT NULL,
    purpose         TEXT,
    donation_date   DATE NOT NULL,
    payment_mode    VARCHAR(16),
    reference_number TEXT,
    receipt_number  TEXT UNIQUE,
    receipt_issued  BOOLEAN NOT NULL DEFAULT false,
    is_80g_eligible BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_alumni_donations_school ON alumni_donations(school_id, donation_date DESC);
CREATE INDEX idx_alumni_donations_alumni ON alumni_donations(alumni_id);
CREATE INDEX idx_alumni_donations_campaign ON alumni_donations(campaign_id) WHERE campaign_id IS NOT NULL;
```

#### `alumni_mentorship_requests` table

```sql
CREATE TABLE alumni_mentorship_requests (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL REFERENCES schools(id),
    alumni_id       UUID NOT NULL REFERENCES alumni(id) ON DELETE CASCADE,
    student_id      UUID NOT NULL REFERENCES students(id),
    requested_by    UUID NOT NULL REFERENCES app_users(id),
    expertise_area  TEXT,
    message         TEXT,
    status          VARCHAR(16) NOT NULL DEFAULT 'pending',
    responded_at    TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_alumni_mentor_req_school ON alumni_mentorship_requests(school_id, status);
CREATE INDEX idx_alumni_mentor_req_alumni ON alumni_mentorship_requests(alumni_id, status);
```

#### `alumni_mentorships` table

```sql
CREATE TABLE alumni_mentorships (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL REFERENCES schools(id),
    alumni_id       UUID NOT NULL REFERENCES alumni(id) ON DELETE CASCADE,
    student_id      UUID NOT NULL REFERENCES students(id),
    request_id      UUID REFERENCES alumni_mentorship_requests(id),
    status          VARCHAR(16) NOT NULL DEFAULT 'active',
    start_date      DATE NOT NULL,
    end_date        DATE,
    notes           TEXT,
    session_count   INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_alumni_mentorships_school ON alumni_mentorships(school_id, status);
CREATE INDEX idx_alumni_mentorships_alumni ON alumni_mentorships(alumni_id);
```

#### `alumni_career_history` table

```sql
CREATE TABLE alumni_career_history (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    alumni_id       UUID NOT NULL REFERENCES alumni(id) ON DELETE CASCADE,
    job_title       TEXT NOT NULL,
    company         TEXT NOT NULL,
    industry        TEXT,
    start_date      DATE,
    end_date        DATE,
    is_current      BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_alumni_career_alumni ON alumni_career_history(alumni_id, is_current DESC);
```

### 6.3 Modified Tables

#### `schools` table modifications

```sql
ALTER TABLE schools ADD COLUMN pan_number VARCHAR(20);
ALTER TABLE schools ADD COLUMN g80_registration_number VARCHAR(50);
ALTER TABLE schools ADD COLUMN g80_validity_date DATE;
ALTER TABLE schools ADD COLUMN g80_certificate_url TEXT;
ALTER TABLE schools ADD COLUMN school_code VARCHAR(20);
CREATE UNIQUE INDEX idx_schools_code ON schools(school_code) WHERE school_code IS NOT NULL;
```

#### `EnrollmentsTable` — add `graduated` status

No schema change needed (varchar(16) already supports it). Document that `graduated` is a valid status value.

#### `CalendarEventsTable` — add `ALUMNI` audience

No schema change needed. Document that `ALUMNI` is a valid audience value.

### 6.4 Indexes

All indexes included in DDL above. Key indexes:
- `idx_alumni_school_year` — directory search by school + graduation year
- `idx_alumni_user_id` — JWT resolution for self-service
- `idx_alumni_verification` — pending verification queue
- `idx_alumni_featured` — featured alumni lookup
- `idx_alumni_donations_school` — donation reports by date
- `idx_alumni_career_alumni` — career history with current job first

### 6.5 Constraints

- `alumni.school_id` — NOT NULL, FK to schools
- `alumni.student_id` — nullable, FK to students
- `alumni.user_id` — nullable, FK to app_users
- `alumni.graduation_year` — NOT NULL, integer
- `alumni.verification_status` — NOT NULL, one of approved/pending/declined
- `alumni.visibility_level` — NOT NULL, one of public/batch/private
- `alumni_donations.alumni_id` — NOT NULL, FK (CASCADE)
- `alumni_donations.campaign_id` — nullable, FK
- `alumni_donations.receipt_number` — UNIQUE
- `alumni_mentorship_requests.alumni_id` — NOT NULL, FK (CASCADE)
- `alumni_mentorship_requests.student_id` — NOT NULL, FK
- `alumni_mentorships.alumni_id` — NOT NULL, FK (CASCADE)
- `alumni_mentorships.student_id` — NOT NULL, FK
- `alumni_career_history.alumni_id` — NOT NULL, FK (CASCADE)

### 6.6 Foreign Keys

- `alumni.school_id` → `schools.id`
- `alumni.student_id` → `students.id` (nullable)
- `alumni.user_id` → `app_users.id` (nullable)
- `alumni.verified_by` → `app_users.id` (nullable)
- `alumni_donation_campaigns.school_id` → `schools.id`
- `alumni_donations.school_id` → `schools.id`
- `alumni_donations.alumni_id` → `alumni.id` (CASCADE)
- `alumni_donations.campaign_id` → `alumni_donation_campaigns.id` (nullable)
- `alumni_mentorship_requests.school_id` → `schools.id`
- `alumni_mentorship_requests.alumni_id` → `alumni.id` (CASCADE)
- `alumni_mentorship_requests.student_id` → `students.id`
- `alumni_mentorship_requests.requested_by` → `app_users.id`
- `alumni_mentorships.school_id` → `schools.id`
- `alumni_mentorships.alumni_id` → `alumni.id` (CASCADE)
- `alumni_mentorships.student_id` → `students.id`
- `alumni_mentorships.request_id` → `alumni_mentorship_requests.id` (nullable)
- `alumni_career_history.alumni_id` → `alumni.id` (CASCADE)

### 6.7 Soft Delete Strategy

- Alumni: `is_active = false` (soft delete). Hard delete on DPDP erasure request.
- Donations: not deleted (financial records). `receipt_issued` tracks receipt status.
- Campaigns: `status = 'closed'` (not deleted).
- Mentorships: `status = 'ended'` (not deleted).
- Career history: hard delete (alumni can remove entries).

### 6.8 Audit Fields

- `created_at` — all tables
- `updated_at` — alumni, donations, campaigns, mentorships, mentorship_requests
- `verified_at` — alumni (verification timestamp)
- `last_active_at` — alumni (engagement tracking)
- `responded_at` — mentorship_requests (alumni response timestamp)

### 6.9 Migration Notes

Migration: `docs/db/migration_052_alumni_management.sql`
- Creates 6 new tables with FK constraints and indexes
- Alters `schools` with 5 new columns (80G + school_code)
- Backfills `school_code` for existing schools
- Adds notification preferences defaults for alumni users
- No data backfill for alumni records (new feature)

### 6.10 Exposed Mappings

Add 6 new table objects in `server/.../db/Tables.kt`:

- `AlumniTable` — mirrors `NonTeachingStaffTable` pattern + verification + privacy columns
- `AlumniDonationCampaignsTable` — school-scoped, campaign management
- `AlumniDonationsTable` — school-scoped, FK to alumni + optional FK to campaign, 80G receipt fields
- `AlumniMentorshipRequestsTable` — school-scoped, student-initiated requests
- `AlumniMentorshipsTable` — school-scoped, FK to alumni + students + optional FK to request
- `AlumniCareerHistoryTable` — FK to alumni, career progression

Register in `DatabaseFactory.kt` `allTables` array after `ParentPulsesTable` (line ~208). Order matters (FK dependencies):
1. `AlumniTable`
2. `AlumniDonationCampaignsTable`
3. `AlumniDonationsTable` (FK to alumni + campaigns)
4. `AlumniMentorshipRequestsTable` (FK to alumni + students)
5. `AlumniMentorshipsTable` (FK to alumni + students + requests)
6. `AlumniCareerHistoryTable` (FK to alumni)

### 6.11 Seed Data

N/A — alumni records created via graduation, self-registration, or bulk import.

### 6.12 Profile Completeness (Derived)

Not stored — computed on read. Formula:
```
completeness = (filled_fields / total_fields) * 100
fields = [name, current_profession, company, city, email, phone, linkedin_url, photo_url, skills, achievements]
```
Returned in `AlumniDto.profileCompleteness` as integer 0-100.

---

## 7. State Machines

### Student Lifecycle Transition

```
ACTIVE_STUDENT ──admin_graduates──> GRADUATED ──auto_create──> ALUMNI
ACTIVE_STUDENT ──admin_transfers──> TRANSFERRED (no alumni record)
ACTIVE_STUDENT ──admin_withdraws──> WITHDRAWN (no alumni record)
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `active` | Admin marks graduated | `graduated` | Admin specifies graduation_year |
| `active` | Admin transfers | `transferred` | No alumni record created |
| `active` | Admin withdraws | `withdrawn` | No alumni record created |
| `graduated` | Auto-create alumni record | `alumni` | `AlumniTable` row created with `verification_status='approved'` |

### Alumni Verification State Machine

```
UNREGISTERED ──self_registers_with_SIS_match──> APPROVED ──account_created──> ACTIVE
UNREGISTERED ──self_registers_no_match──> PENDING ──admin_approves──> APPROVED ──account_created──> ACTIVE
UNREGISTERED ──self_registers_no_match──> PENDING ──admin_declines──> DECLINED
UNREGISTERED ──admin_bulk_import──> APPROVED (user_id=null until self-registration links)
UNREGISTERED ──admin_graduates_student──> APPROVED (user_id=null until self-registration links)
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `unregistered` | Self-register with SIS match | `approved` | Year + admission_number match |
| `unregistered` | Self-register without match | `pending` | Admin review queue |
| `unregistered` | Admin bulk import | `approved` | Admin-verified by default |
| `unregistered` | Admin graduates student | `approved` | Admin-verified |
| `pending` | Admin approves | `approved` | Admin action; account created |
| `pending` | Admin declines | `declined` | Admin action |
| `approved` | Alumni self-registers and links | `active` | `user_id` set; account created |

### Mentorship Request State Machine

```
PENDING ──alumni_accepts──> ACCEPTED ──mentorship_created──> ACTIVE_MENTORSHIP
PENDING ──alumni_declines──> DECLINED
PENDING ──admin_cancels──> EXPIRED
PENDING ──timeout──> EXPIRED
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `pending` | Alumni accepts | `accepted` | Mentorship record created |
| `pending` | Alumni declines | `declined` | Student notified |
| `pending` | Admin cancels | `expired` | Admin override |
| `accepted` | Mentorship created | `active_mentorship` | `AlumniMentorshipsTable` row |
| `active_mentorship` | Admin ends | `ended` | `end_date` set |

### Donation Campaign State Machine

```
ACTIVE ──admin_closes──> CLOSED
ACTIVE ──admin_pauses──> PAUSED ──admin_resumes──> ACTIVE
ACTIVE ──end_date_reached──> CLOSED
```

---

## 8. Backend Architecture

### 8.1 Component Overview

`AlumniService` handles all alumni CRUD, graduation, verification, campaigns, donations, 80G receipts, mentorship, and analytics. `AlumniReceiptService` handles PDF receipt generation and Form 10BD export. `AlumniRouting` exposes admin, self-service, and public endpoints. `AlumniAccess.kt` provides auth guard for self-service endpoints.

### 8.2 Design Principles

1. **SIS-verified registration** — auto-match against student records; admin review for mismatches
2. **Privacy-first** — DPDP Act 2023 compliant field-level visibility controls
3. **Reuse existing infra** — notifications, WhatsApp, calendar, file storage
4. **80G compliance** — receipt generation with PAN, registration number, Form 10BD export
5. **Multi-tenant isolation** — all queries filtered by `school_id`

### 8.3 Core Types

```kotlin
class AlumniService {
    // Admin — Alumni CRUD
    suspend fun listAlumni(schoolId: UUID, filters: AlumniSearchFilters): PaginatedResult<AlumniDto>
    suspend fun getAlumni(schoolId: UUID, alumniId: UUID): AlumniDto?
    suspend fun createAlumni(schoolId: UUID, dto: CreateAlumniDto, verifiedBy: UUID): AlumniDto
    suspend fun updateAlumni(schoolId: UUID, alumniId: UUID, dto: UpdateAlumniDto): AlumniDto?
    suspend fun deactivateAlumni(schoolId: UUID, alumniId: UUID): Boolean
    suspend fun graduateStudents(schoolId: UUID, dto: GraduateStudentsDto, adminId: UUID): List<AlumniDto>
    suspend fun bulkImport(schoolId: UUID, rows: List<CreateAlumniDto>): BulkImportResult

    // Admin — Verification
    suspend fun listPendingVerifications(schoolId: UUID): List<AlumniDto>
    suspend fun verifyAlumni(schoolId: UUID, alumniId: UUID, action: String, adminId: UUID): AlumniDto?

    // Admin — Featured
    suspend fun toggleFeatured(schoolId: UUID, alumniId: UUID): AlumniDto?

    // Admin — Campaigns
    suspend fun listCampaigns(schoolId: UUID): List<AlumniDonationCampaignDto>
    suspend fun createCampaign(schoolId: UUID, dto: CreateCampaignDto): AlumniDonationCampaignDto
    suspend fun getCampaign(schoolId: UUID, campaignId: UUID): AlumniDonationCampaignDto?
    suspend fun updateCampaign(schoolId: UUID, campaignId: UUID, status: String): AlumniDonationCampaignDto?

    // Admin — Donations
    suspend fun listDonations(schoolId: UUID, campaignId: UUID?, alumniId: UUID?): List<AlumniDonationDto>
    suspend fun createDonation(schoolId: UUID, dto: CreateDonationDto): AlumniDonationDto
    suspend fun generateReceipt(schoolId: UUID, donationId: UUID): ByteArray
    suspend fun exportForm10BD(schoolId: UUID, year: Int): ByteArray

    // Admin — Mentorship
    suspend fun listMentorships(schoolId: UUID): List<AlumniMentorshipDto>
    suspend fun createMentorship(schoolId: UUID, dto: CreateMentorshipDto): AlumniMentorshipDto
    suspend fun endMentorship(schoolId: UUID, mentorshipId: UUID): Boolean
    suspend fun listMentorshipRequests(schoolId: UUID): List<AlumniMentorshipRequestDto>

    // Admin — Analytics
    suspend fun getAnalyticsOverview(schoolId: UUID): AlumniAnalyticsDto
    suspend fun getEngagementMetrics(schoolId: UUID): EngagementMetrics
    suspend fun getDonationAnalytics(schoolId: UUID): DonationAnalytics
    suspend fun getCareerAnalytics(schoolId: UUID): CareerAnalytics

    // Alumni self-service
    suspend fun getAlumniProfile(userId: UUID): AlumniDto?
    suspend fun updateAlumniProfile(userId: UUID, dto: UpdateAlumniDto): AlumniDto?
    suspend fun updatePrivacy(userId: UUID, dto: AlumniPrivacyDto): AlumniDto?
    suspend fun volunteerAsMentor(userId: UUID, expertise: String): AlumniDto?
    suspend fun getMentorshipRequests(userId: UUID): List<AlumniMentorshipRequestDto>
    suspend fun respondToMentorshipRequest(userId: UUID, requestId: UUID, action: String): AlumniMentorshipRequestDto?
    suspend fun getOwnMentorships(userId: UUID): List<AlumniMentorshipDto>
    suspend fun getCareerHistory(userId: UUID): List<CareerHistoryDto>
    suspend fun addCareerHistory(userId: UUID, dto: CreateCareerHistoryDto): CareerHistoryDto
    suspend fun updateCareerHistory(userId: UUID, entryId: UUID, dto: CreateCareerHistoryDto): CareerHistoryDto?
    suspend fun getOwnDonations(userId: UUID): List<AlumniDonationDto>
    suspend fun searchDirectory(userId: UUID, filters: AlumniSearchFilters): PaginatedResult<AlumniDto>

    // Public — Self-registration
    suspend fun registerAlumni(dto: AlumniRegisterDto): RegistrationResult
}
```

### 8.4 Repositories

- `AlumniRepository` — alumni CRUD, verification, directory search
- `AlumniDonationRepository` — donations, campaigns
- `AlumniMentorshipRepository` — mentorships, requests
- `AlumniCareerHistoryRepository` — career history

### 8.5 Mappers

- `AlumniMapper` — maps DB rows to DTOs with profile completeness calculation
- `DonationMapper` — maps DB rows to DTOs with campaign title JOIN
- `MentorshipMapper` — maps DB rows to DTOs with name JOINs

### 8.6 Permission Checks

- Admin endpoints: `requireSchoolContext()` + `requireSchoolAdmin()` for writes
- Alumni self-service: `requireAlumniContext()` — resolves alumni from JWT `sub` → `alumni.user_id`
- Public registration: no auth (rate-limited)
- `AlumniAccess.kt` returns `AlumniContext(alumniId, schoolId, userId)` — schoolId from `AlumniTable.school_id`, not `app_users.schoolId`

### 8.7 Background Jobs

- `ProfileCompletionReminderJob` — monthly; sends push to alumni with < 50% profile completeness
- `CampaignProgressJob` — weekly; sends campaign progress to donors and target batch

### 8.8 Domain Events

- `AlumniCreated` — emitted on create/graduate/import
- `AlumniVerified` — emitted on admin approval
- `AlumniRegistered` — emitted on self-registration
- `DonationLogged` — emitted on donation creation
- `CampaignCreated` — emitted on campaign creation
- `MentorshipRequested` — emitted on student request
- `MentorshipAccepted` — emitted on alumni acceptance
- `MentorshipDeclined` — emitted on alumni decline

### 8.9 Caching

- Alumni directory: no cache (privacy filtering requires real-time)
- Campaign `amount_raised`: cached, updated on each donation
- Analytics: cached for 5 minutes (expensive aggregate queries)

### 8.10 Transactions

- Graduation: update enrollment + create alumni in transaction
- Self-registration: SIS match + create alumni + create app_users in transaction
- Donation: insert donation + update campaign `amount_raised` in transaction
- Mentorship acceptance: update request status + create mentorship in transaction

### 8.11 Rate Limiting

- Self-registration: 3 attempts per IP per hour (Ktor `RateLimit`)
- Standard API rate limiting for all other endpoints

### 8.12 Configuration

- `ALUMNI_SELF_REG_RATE_LIMIT` — registration attempts per IP per hour (default: `3`)
- `ALUMNI_PROFILE_REMINDER_THRESHOLD` — completeness % below which reminder sent (default: `50`)
- `ALUMNI_MAX_MENTEES_DEFAULT` — max mentees per alumni (default: `5`)
- `ALUMNI_DIRECTORY_MAX_PAGE_SIZE` — max results per directory search (default: `50`)

---

## 9. API Contracts

### 9.1 Admin Endpoints — Alumni Management

All require `requireSchoolContext()`. Writes require `requireSchoolAdmin()`.

```
# Alumni CRUD + lifecycle
GET    /api/v1/school/alumni?year=&profession=&city=&company=&industry=&q=&page=1&limit=20
POST   /api/v1/school/alumni
GET    /api/v1/school/alumni/{id}
PATCH  /api/v1/school/alumni/{id}
PATCH  /api/v1/school/alumni/{id}/deactivate
POST   /api/v1/school/alumni/graduate
POST   /api/v1/school/alumni/import

# Verification queue
GET    /api/v1/school/alumni/pending
PATCH  /api/v1/school/alumni/{id}/verify

# Featured alumni
PATCH  /api/v1/school/alumni/{id}/feature
```

### 9.2 Admin Endpoints — Donations & Campaigns

```
# Campaigns
GET    /api/v1/school/alumni/campaigns
POST   /api/v1/school/alumni/campaigns
GET    /api/v1/school/alumni/campaigns/{id}
PATCH  /api/v1/school/alumni/campaigns/{id}

# Donations
GET    /api/v1/school/alumni/donations?campaign_id=&alumni_id=&page=&limit=
GET    /api/v1/school/alumni/{id}/donations
POST   /api/v1/school/alumni/donations
GET    /api/v1/school/alumni/donations/{id}/receipt
GET    /api/v1/school/alumni/donations/80g/form10bd
```

### 9.3 Admin Endpoints — Mentorship

```
GET    /api/v1/school/alumni/mentorships
POST   /api/v1/school/alumni/mentorships
PATCH  /api/v1/school/alumni/mentorships/{id}
GET    /api/v1/school/alumni/mentorship-requests
PATCH  /api/v1/school/alumni/mentorship-requests/{id}
GET    /api/v1/school/alumni/mentorship/settings
PUT    /api/v1/school/alumni/mentorship/settings
```

### 9.4 Admin Endpoints — Analytics

```
GET    /api/v1/school/alumni/analytics/overview
GET    /api/v1/school/alumni/analytics/engagement
GET    /api/v1/school/alumni/analytics/donations
GET    /api/v1/school/alumni/analytics/career
```

### 9.5 Alumni Self-Service Endpoints

All require `requireAlumniContext()`.

```
# Profile
GET    /api/v1/alumni/profile
PATCH  /api/v1/alumni/profile
PATCH  /api/v1/alumni/privacy
POST   /api/v1/alumni/photo

# Mentorship
POST   /api/v1/alumni/mentor-volunteer
GET    /api/v1/alumni/mentorship-requests
PATCH  /api/v1/alumni/mentorship-requests/{id}
GET    /api/v1/alumni/mentorships

# Career history
GET    /api/v1/alumni/career-history
POST   /api/v1/alumni/career-history
PATCH  /api/v1/alumni/career-history/{id}

# Donations
GET    /api/v1/alumni/donations
GET    /api/v1/alumni/campaigns

# Directory (privacy-filtered)
GET    /api/v1/alumni/directory?year=&profession=&city=&q=
```

### 9.6 Alumni Self-Registration (Public, no auth)

```
POST   /api/v1/alumni/register
GET    /api/v1/alumni/schools/search?q=...
```

### 9.7 DTO Models

All `@Serializable`, wrapped in `ApiResponse<T>` pattern.

```kotlin
@Serializable data class AlumniDto(
    val id: String, val schoolId: String, val studentId: String?,
    val userId: String?, val name: String, val graduationYear: Int,
    val lastClass: String?, val currentProfession: String?,
    val company: String?, val city: String?, val email: String?,
    val phone: String?, val linkedinUrl: String?, val photoUrl: String?,
    val skills: String?, val achievements: String?,
    val isMentor: Boolean, val mentorExpertise: String?,
    val isFeatured: Boolean,
    val verificationStatus: String, val verifiedAt: String?,
    val showPhone: Boolean, val showEmail: Boolean, val showLinkedin: Boolean,
    val visibilityLevel: String,
    val profileCompleteness: Int,
    val lastActiveAt: String?,
    val isActive: Boolean, val createdAt: String, val updatedAt: String,
    val careerHistory: List<CareerHistoryDto> = emptyList()
)

@Serializable data class CreateAlumniDto(
    val studentId: String?, val name: String, val graduationYear: Int,
    val lastClass: String?, val currentProfession: String?,
    val company: String?, val city: String?, val email: String?,
    val phone: String?, val linkedinUrl: String?, val photoUrl: String?,
    val skills: String?, val achievements: String?
)

@Serializable data class UpdateAlumniDto(
    val name: String? = null, val currentProfession: String? = null,
    val company: String? = null, val city: String? = null,
    val email: String? = null, val phone: String? = null,
    val linkedinUrl: String? = null, val photoUrl: String? = null,
    val skills: String? = null, val achievements: String? = null,
    val isMentor: Boolean? = null, val mentorExpertise: String? = null,
    val isFeatured: Boolean? = null, val isActive: Boolean? = null
)

@Serializable data class AlumniPrivacyDto(
    val showPhone: Boolean, val showEmail: Boolean,
    val showLinkedin: Boolean, val visibilityLevel: String
)

@Serializable data class AlumniRegisterDto(
    val schoolCode: String?, val schoolName: String?,
    val name: String, val yearOfPassing: Int,
    val admissionNumber: String, val email: String, val phone: String
)

@Serializable data class GraduateStudentsDto(
    val studentIds: List<String>, val graduationYear: Int
)

@Serializable data class AlumniDonationCampaignDto(
    val id: String, val schoolId: String, val title: String,
    val description: String?, val cause: String?,
    val targetAmount: Double, val amountRaised: Double,
    val targetBatchYear: Int?, val startDate: String,
    val endDate: String?, val status: String, val isActive: Boolean,
    val donorCount: Int, val createdAt: String, val updatedAt: String
)

@Serializable data class CreateCampaignDto(
    val title: String, val description: String?, val cause: String?,
    val targetAmount: Double, val targetBatchYear: Int? = null,
    val startDate: String, val endDate: String? = null
)

@Serializable data class AlumniDonationDto(
    val id: String, val schoolId: String, val alumniId: String,
    val alumniName: String, val campaignId: String?, val campaignTitle: String?,
    val amount: Double, val purpose: String?,
    val donationDate: String, val paymentMode: String?,
    val referenceNumber: String?,
    val receiptNumber: String?, val receiptIssued: Boolean, val is80gEligible: Boolean,
    val createdAt: String
)

@Serializable data class CreateDonationDto(
    val alumniId: String, val campaignId: String? = null,
    val amount: Double, val purpose: String?,
    val donationDate: String, val paymentMode: String?,
    val referenceNumber: String?
)

@Serializable data class AlumniMentorshipRequestDto(
    val id: String, val schoolId: String,
    val alumniId: String, val alumniName: String,
    val studentId: String, val studentName: String,
    val requestedBy: String, val requestedByName: String,
    val expertiseArea: String?, val message: String?,
    val status: String, val respondedAt: String?, val createdAt: String
)

@Serializable data class CreateMentorshipRequestDto(
    val alumniId: String, val expertiseArea: String?, val message: String?
)

@Serializable data class AlumniMentorshipDto(
    val id: String, val schoolId: String, val alumniId: String,
    val alumniName: String, val studentId: String, val studentName: String,
    val requestId: String?, val status: String,
    val startDate: String, val endDate: String?,
    val notes: String?, val sessionCount: Int,
    val createdAt: String
)

@Serializable data class CreateMentorshipDto(
    val alumniId: String, val studentId: String,
    val startDate: String, val notes: String?
)

@Serializable data class CareerHistoryDto(
    val id: String, val alumniId: String,
    val jobTitle: String, val company: String, val industry: String?,
    val startDate: String?, val endDate: String?, val isCurrent: Boolean,
    val createdAt: String
)

@Serializable data class CreateCareerHistoryDto(
    val jobTitle: String, val company: String, val industry: String?,
    val startDate: String?, val endDate: String?, val isCurrent: Boolean = false
)

@Serializable data class AlumniAnalyticsDto(
    val totalAlumni: Int, val activeAlumni: Int, val pendingVerifications: Int,
    val byGraduationYear: Map<String, Int>,
    val byProfession: Map<String, Int>,
    val byCity: Map<String, Int>,
    val totalDonations: Double, val donationCount: Int,
    val activeCampaigns: Int, val activeMentorships: Int,
    val mentorshipRequestsPending: Int,
    val engagementRate: Double
)
```

---

## 10. Frontend Architecture

### 10.1 Screens

| Screen | Platform | Role | Description |
|---|---|---|---|
| `AlumniScreen` | Compose | Admin | Main alumni management with 5 tabs (directory, verification, donations, mentorship, analytics) |
| `AlumniDetailScreen` | Compose | Admin | Single alumni profile with career timeline, donations, mentorships |
| `AlumniCampaignScreen` | Compose | Admin | Campaign detail with donor list + progress |
| `AlumniRegistrationScreen` | Compose | Alumni | Self-registration with SIS verification |
| `AlumniProfileScreen` | Compose | Alumni | Self-service profile + privacy + career history |
| `AlumniDirectoryScreen` | Compose | Alumni | Privacy-filtered directory search |
| `AlumniMentorshipScreen` | Compose | Alumni | Mentorship requests + active mentorships |
| `AlumniDonationScreen` | Compose | Alumni | Own donations + active campaigns |
| `AlumniDashboardV2` | Compose | Alumni | Alumni portal with bottom nav tabs |
| Web admin alumni pages | Web | Admin | Directory, detail, campaigns, analytics pages |

### 10.2 Navigation

- Admin portal → People → Alumni → `AlumniScreen`
- Admin portal → People → Alumni → {alumni} → `AlumniDetailScreen`
- Admin portal → People → Alumni → Campaigns → {campaign} → `AlumniCampaignScreen`
- Alumni portal → Profile → `AlumniProfileScreen`
- Alumni portal → Directory → `AlumniDirectoryScreen`
- Alumni portal → Mentorship → `AlumniMentorshipScreen`
- Alumni portal → Donations → `AlumniDonationScreen`
- Web admin → /admin/alumni → directory page
- Web admin → /admin/alumni/[id] → detail page
- Web admin → /admin/alumni/campaigns → campaigns page
- Web admin → /admin/alumni/analytics → analytics page

### 10.3 UX Flows

#### Admin: Bulk Graduation

1. Admin opens student roster
2. Selects students to graduate
3. Clicks "Mark as Alumni" bulk action
4. Enters graduation year
5. Confirms
6. Students transition to graduated; alumni records auto-created
7. Admin navigates to Alumni → Directory to verify

#### Alumni: Self-Registration

1. Alumnus opens app → selects "Alumni Registration"
2. Searches school by name or code
3. Enters name, year of passing, admission number, email, phone
4. System auto-matches SIS records
5. If matched → auto-approved → account created → login
6. If not matched → "Your registration is pending review. School admin will approve shortly."
7. Admin reviews in verification queue → approves → account created → welcome notification

#### Admin: Log Donation

1. Admin opens Alumni → Donations tab
2. Clicks "Log Donation"
3. Selects alumni (auto-filled from directory)
4. Enters amount, payment method, date, campaign (optional)
5. System checks 80G eligibility
6. If eligible → receipt auto-generated (PDF)
7. Admin can download receipt

### 10.4 State Management

```kotlin
data class AlumniState(
    val alumni: List<AlumniDto>,
    val currentAlumni: AlumniDto?,
    val campaigns: List<AlumniDonationCampaignDto>,
    val donations: List<AlumniDonationDto>,
    val mentorships: List<AlumniMentorshipDto>,
    val mentorshipRequests: List<AlumniMentorshipRequestDto>,
    val pendingVerifications: List<AlumniDto>,
    val analytics: AlumniAnalyticsDto?,
    val careerHistory: List<CareerHistoryDto>,
    val filter: AlumniSearchFilters,
    val isLoading: Boolean,
    val error: String?,
)
```

### 10.5 Offline Support

- Alumni directory cached locally (last search results)
- Own profile cached for offline viewing
- Career history cached locally

### 10.6 Loading States

- Loading directory: "Loading alumni directory..."
- Graduating students: "Graduating students..."
- Logging donation: "Logging donation..."
- Generating receipt: "Generating receipt..."

### 10.7 Error Handling (UI)

- SIS match failed: "Could not verify against school records. Your registration is pending admin review."
- Already registered: "You're already registered as an alumni of this school."
- 80G not eligible: "This donation is not 80G-eligible (cash > ₹2,000 or school 80G certificate not valid)."
- Private profile: "This alumni profile is private."
- No alumni found: "No alumni found for selected filters."

### 10.8 Component Integration Guidelines

| Rule | Description |
|---|---|
| **R1** | Alumni directory with search filters (year, profession, city, company, industry) |
| **R2** | Profile completeness progress bar (0-100%) |
| **R3** | Featured alumni badge in directory |
| **R4** | Verification queue with approve/decline buttons |
| **R5** | Campaign progress bar: amount_raised / target_amount |
| **R6** | Donation form with payment method dropdown |
| **R7** | 80G receipt download button (PDF) |
| **R8** | Mentorship request card with accept/decline |
| **R9** | Career timeline with current job highlighted |
| **R10** | Privacy settings toggles (phone, email, LinkedIn) + visibility level selector |
| **R11** | Analytics dashboard with charts (by year, profession, city, industry) |
| **R12** | School search autocomplete in registration form |
| **R13** | "Mark as Alumni" bulk action in student roster |
| **R14** | Alumni portal with bottom nav (Profile, Directory, Mentorship, Donations) |

---

## 11. Shared Module Changes (KMP)

### 11.1 DTOs

All DTOs defined in section 9.7, placed in `shared/.../feature/alumni/data/remote/`.

### 11.2 Domain Models

```kotlin
data class Alumni(
    val id: UUID, val schoolId: UUID, val studentId: UUID?,
    val userId: UUID?, val name: String, val graduationYear: Int,
    val currentProfession: String?, val company: String?, val city: String?,
    val isMentor: Boolean, val isFeatured: Boolean,
    val verificationStatus: VerificationStatus,
    val visibilityLevel: VisibilityLevel,
    val profileCompleteness: Int,
    val careerHistory: List<CareerHistory>,
)

enum class VerificationStatus { APPROVED, PENDING, DECLINED }
enum class VisibilityLevel { PUBLIC, BATCH, PRIVATE }

data class AlumniDonation(
    val id: UUID, val alumniId: UUID, val campaignId: UUID?,
    val amount: Double, val donationDate: LocalDate,
    val paymentMode: String?, val receiptNumber: String?,
    val is80gEligible: Boolean,
)

data class AlumniDonationCampaign(
    val id: UUID, val title: String, val cause: String?,
    val targetAmount: Double, val amountRaised: Double,
    val targetBatchYear: Int?, val status: CampaignStatus,
)

enum class CampaignStatus { ACTIVE, CLOSED, PAUSED }

data class AlumniMentorship(
    val id: UUID, val alumniId: UUID, val studentId: UUID,
    val status: MentorshipStatus, val startDate: LocalDate,
    val endDate: LocalDate?, val sessionCount: Int,
)

enum class MentorshipStatus { ACTIVE, ENDED }

data class AlumniMentorshipRequest(
    val id: UUID, val alumniId: UUID, val studentId: UUID,
    val expertiseArea: String?, val message: String?,
    val status: MentorshipRequestStatus,
)

enum class MentorshipRequestStatus { PENDING, ACCEPTED, DECLINED, EXPIRED }

data class CareerHistory(
    val id: UUID, val alumniId: UUID,
    val jobTitle: String, val company: String, val industry: String?,
    val startDate: LocalDate?, val endDate: LocalDate?, val isCurrent: Boolean,
)

data class AlumniAnalytics(
    val totalAlumni: Int, val activeAlumni: Int,
    val byGraduationYear: Map<Int, Int>,
    val byProfession: Map<String, Int>,
    val totalDonations: Double, val activeCampaigns: Int,
    val activeMentorships: Int, val engagementRate: Double,
)
```

### 11.3 Repository Interfaces

```kotlin
interface AlumniRepository {
    suspend fun listAlumni(filters: AlumniSearchFilters): NetworkResult<List<AlumniDto>>
    suspend fun getAlumni(id: String): NetworkResult<AlumniDto>
    suspend fun graduateStudents(studentIds: List<String>, graduationYear: Int): NetworkResult<List<AlumniDto>>
    suspend fun bulkImport(rows: List<CreateAlumniDto>): NetworkResult<BulkImportResult>
    suspend fun listPendingVerifications(): NetworkResult<List<AlumniDto>>
    suspend fun verifyAlumni(id: String, action: String): NetworkResult<AlumniDto>
    suspend fun toggleFeatured(id: String): NetworkResult<AlumniDto>
    suspend fun getProfile(): NetworkResult<AlumniDto>
    suspend fun updateProfile(dto: UpdateAlumniDto): NetworkResult<AlumniDto>
    suspend fun updatePrivacy(dto: AlumniPrivacyDto): NetworkResult<AlumniDto>
    suspend fun searchDirectory(filters: AlumniSearchFilters): NetworkResult<List<AlumniDto>>
    suspend fun registerAlumni(dto: AlumniRegisterDto): NetworkResult<RegistrationResult>
}
```

### 11.4 UseCases

- `ListAlumniUseCase`, `GetAlumniUseCase`, `GraduateStudentsUseCase`
- `BulkImportAlumniUseCase`, `VerifyAlumniUseCase`, `ToggleFeaturedUseCase`
- `GetAlumniProfileUseCase`, `UpdateAlumniProfileUseCase`, `UpdatePrivacyUseCase`
- `SearchDirectoryUseCase`, `RegisterAlumniUseCase`
- `ListCampaignsUseCase`, `CreateCampaignUseCase`, `GetCampaignUseCase`
- `ListDonationsUseCase`, `CreateDonationUseCase`, `DownloadReceiptUseCase`
- `ListMentorshipsUseCase`, `CreateMentorshipUseCase`, `EndMentorshipUseCase`
- `ListMentorshipRequestsUseCase`, `RespondToMentorshipRequestUseCase`
- `GetCareerHistoryUseCase`, `AddCareerHistoryUseCase`, `UpdateCareerHistoryUseCase`
- `GetAlumniAnalyticsUseCase`

### 11.5 Validation

- Name: not empty, max 200 characters
- Graduation year: integer, 1950-2100
- Email: valid format
- Phone: valid format
- Donation amount: positive double
- Campaign target amount: positive double
- Visibility level: one of public/batch/private

### 11.6 Serialization

Standard Kotlinx serialization.

### 11.7 Network APIs

Ktor `@Resource` route definitions:
- `SchoolAlumniApi` — admin endpoints
- `AlumniSelfServiceApi` — alumni self-service endpoints
- `AlumniPublicApi` — public registration + school search

### 11.8 Database Models (Local Cache)

- Alumni directory search results cached locally
- Own profile cached for offline viewing
- Career history cached locally

---

## 12. Permissions Matrix

| Action | Super Admin | School Admin | Teacher | Parent | Alumni |
|---|---|---|---|---|---|
| View alumni directory | ✅ | ✅ | ❌ | ❌ | ✅ (privacy-filtered) |
| Create/edit alumni | ✅ | ✅ | ❌ | ❌ | ❌ |
| Graduate students | ✅ | ✅ | ❌ | ❌ | ❌ |
| Bulk import alumni | ✅ | ✅ | ❌ | ❌ | ❌ |
| Verify registrations | ✅ | ✅ | ❌ | ❌ | ❌ |
| Toggle featured | ✅ | ✅ | ❌ | ❌ | ❌ |
| Manage campaigns | ✅ | ✅ | ❌ | ❌ | ❌ |
| Log donations | ✅ | ✅ | ❌ | ❌ | ❌ |
| Generate 80G receipts | ✅ | ✅ | ❌ | ❌ | ❌ |
| Export Form 10BD | ✅ | ✅ | ❌ | ❌ | ❌ |
| Create mentorships | ✅ | ✅ | ❌ | ❌ | ❌ |
| End mentorships | ✅ | ✅ | ❌ | ❌ | ❌ |
| View analytics | ✅ | ✅ | ❌ | ❌ | ❌ |
| Self-register | N/A | N/A | N/A | N/A | ✅ (public) |
| View own profile | N/A | N/A | N/A | N/A | ✅ |
| Edit own profile | N/A | N/A | N/A | N/A | ✅ |
| Manage privacy | N/A | N/A | N/A | N/A | ✅ |
| Volunteer as mentor | N/A | N/A | N/A | N/A | ✅ |
| Respond to mentorship requests | N/A | N/A | N/A | N/A | ✅ |
| Add career history | N/A | N/A | N/A | N/A | ✅ |
| View own donations | N/A | N/A | N/A | N/A | ✅ |
| Request mentorship | N/A | N/A | N/A | ✅ | N/A |

---

## 13. Notifications

### Alumni-Specific Notification Triggers

| Type | Trigger | Recipient | Channel | Message |
|---|---|---|---|---|
| Verification Approved | Admin approves pending registration | Alumni | Push + WhatsApp | "Welcome! Your alumni registration for {school_name} has been approved." |
| Mentorship Request Received | Student requests mentorship | Alumni | Push + WhatsApp | "{student_name} has requested mentorship for {expertise_area}. Tap to respond." |
| Mentorship Request Accepted | Alumni accepts request | Student's parent | Push | "{alumni_name} has accepted your mentorship request." |
| Mentorship Request Declined | Alumni declines request | Student's parent | Push | "{alumni_name} has declined your mentorship request." |
| Donation Received | Admin logs donation | Alumni | Push + Email | "Thank you for your donation of ₹{amount} to {school_name}. Receipt: {receipt_number}" |
| Campaign Launched | New campaign targeting their batch | Alumni in batch | Push + WhatsApp | "New campaign: {campaign_title}. Target: ₹{target_amount}. Donate now!" |
| Profile Completion Reminder | Monthly, if completeness < 50% | Incomplete alumni | Push | "Your alumni profile is {completeness}% complete. Update now to help students connect with you." |
| Alumni Newsletter | Admin sends announcement with ALUMNI audience | Alumni | Push + WhatsApp | "{announcement_title}" |

### Notification System Integration

- Add `"ALUMNI"` to `VALID_AUDIENCE_TYPES` in `AnnouncementRouting.kt`
- Add `alumniInSchool(schoolId, graduationYear)` to `NotifyRecipients.kt`
- Add `ALUMNI` case to recipient resolution
- `Notify.kt` works with any `app_users.id` — alumni need `user_id` linked to `app_users`
- Verify `NotificationPreferencesTable` has default entries for alumni users
- Reuse WhatsApp infrastructure for alumni outreach

---

## 14. Background Jobs

### Profile Completion Reminder Job

| Field | Value |
|---|---|
| **Name** | `ProfileCompletionReminderJob` |
| **Trigger** | Monthly |
| **Frequency** | Monthly (1st of month) |
| **Description** | Sends push notification to alumni with profile completeness < 50% |
| **Timeout** | 120 seconds |
| **Retry** | None |
| **On failure** | Logged; retried next month |

### Campaign Progress Job

| Field | Value |
|---|---|
| **Name** | `CampaignProgressJob` |
| **Trigger** | Weekly |
| **Frequency** | Weekly (Monday) |
| **Description** | Sends campaign progress update to donors and target batch alumni |
| **Timeout** | 120 seconds |
| **Retry** | None |
| **On failure** | Logged; retried next week |

### Engagement Tracking (Event-Driven)

| Field | Value |
|---|---|
| **Name** | `AlumniEngagementTracker` |
| **Trigger** | Alumni self-service API call |
| **Frequency** | On-demand (synchronous) |
| **Description** | Updates `alumni.last_active_at` on each self-service request |
| **Timeout** | 1 second |
| **Retry** | None |
| **On failure** | Logged; non-blocking |

---

## 15. Integrations

### Internal Integrations

| System | Integration Point | Direction | Protocol | Error Handling |
|---|---|---|---|---|
| `CalendarEventsTable` | Alumni events with `ALUMNI` audience | Write | Direct DB | N/A |
| `AnnouncementRouting.kt` | `ALUMNI` audience type for newsletters | Write | Direct code | N/A |
| `NotifyRecipients.kt` | Alumni recipient resolver | Write | Direct code | Fallback: skip if no `user_id` |
| `Notify.kt` | Push + WhatsApp delivery to alumni | Write | Direct call | Logged; non-blocking |
| `WhatsappLogsTable` | WhatsApp message logging | Write | Direct DB | Logged |
| `SupabaseStorage` | Alumni photo upload | Write | Direct call | Return error to client |
| `AuthRouting.kt` | Alumni login (OTP + password) | Read | Direct code | Standard auth errors |
| `JwtConfig.kt` | Alumni role in JWT | Write | Direct code | N/A |
| `EnrollmentsTable` | Graduation status update | Write | Direct DB | Transaction rollback |
| `StudentsTable` | SIS verification match | Read | Direct DB | Return pending status |
| `SchoolsTable` | School code + 80G certificate | Read | Direct DB | Return error if not found |
| `NotificationPreferencesTable` | Default preferences for alumni | Write | Direct DB | Logged; non-blocking |

### External Integrations

| System | Purpose | Direction | Protocol | Authentication | Error Handling |
|---|---|---|---|---|---|
| Meta WhatsApp Business API | Alumni outreach (event invites, campaigns) | Outbound | HTTP API | Bearer token | Retry 3x; log to `WhatsappLogsTable` |
| OpenPDF (library) | 80G receipt PDF generation | Local | Library call | N/A | Return error to admin |
| Razorpay/Cashfree (future) | Online donation payments | Bidirectional | HTTP API + Webhook | API key + webhook secret | Phase 2+ scope |

### Integration Patterns

- **WhatsApp:** Reuse existing `WhatsappLogsTable` and announcement WhatsApp sync. New templates required: `alumni_mentorship_request`, `alumni_campaign_launch`, `alumni_verification_approved`, `alumni_donation_receipt`. Submit for Meta approval early (24-48hr turnaround).
- **PDF Generation:** Add `com.github.librepdf:openpdf:2.0.3` to `server/build.gradle.kts`. Alternative: Apache PDFBox (`org.apache.pdfbox:pdfbox:3.0.3`). OpenPDF is sufficient for structured receipt templates.
- **File Upload:** Create `POST /api/v1/alumni/photo` with `requireAlumniContext()`. Reuse `SupabaseStorage.upload()` with `kind = "PROFILE"`. Path: `{schoolId}/profile/{uuid}.{ext}`. Max 5MB.

---

## 16. Security

### Authentication

- Alumni login via existing OTP (phone) or password (email) flow
- JWT contains `role = "alumni"` claim
- `AlumniAccess.kt` resolves alumni context from JWT `sub` → `alumni.user_id`
- `requireAlumniContext()` returns `AlumniContext(alumniId, schoolId, userId)`
- School ID resolved from `AlumniTable.school_id`, NOT from `app_users.schoolId` (which is null for alumni)

### Authorization

- Admin endpoints: `requireSchoolContext()` + `requireSchoolAdmin()` for writes
- Alumni self-service: `requireAlumniContext()` — ensures `is_active = true` and `verification_status = 'approved'`
- Public registration: no auth (rate-limited, honeypot, school code validation)
- Alumni intentionally excluded from `SCHOOL_ROLES` set — they use separate `requireAlumniContext()`

### Data Protection

- **DPDP Act 2023 compliance:** Field-level privacy controls (`show_phone`, `show_email`, `show_linkedin`, `visibility_level`)
- **Right to access:** `GET /api/v1/alumni/profile` returns all stored data
- **Right to correct:** `PATCH /api/v1/alumni/profile` for profile updates
- **Right to erasure:** `PATCH /api/v1/alumni/profile` with `isActive = false` (soft delete). Admin can hard-delete on request.
- **Data export:** `GET /api/v1/alumni/profile` returns all stored data for the alumnus

### Input Validation

- Name: not empty, max 200 characters
- Graduation year: integer, 1950-2100
- Email: valid format (RFC 5322)
- Phone: valid format (E.164 or local)
- Donation amount: positive double
- Campaign target amount: positive double
- Visibility level: one of public/batch/private
- Payment mode: one of upi/bank_transfer/cheque/cash/card
- School code: exact match against `schools.school_code`

### Rate Limiting

- Self-registration: 3 attempts per IP per hour (Ktor `RateLimit`)
- Standard API rate limiting for all other endpoints

### Audit Logging

- Alumni creation (admin create, graduation, bulk import, self-registration)
- Verification actions (approve/decline)
- Donation logging
- Receipt generation
- Mentorship creation/termination
- Profile updates (self-service)
- Privacy setting changes

### PII Handling

- Alumni PII: name, email, phone, LinkedIn, photo, profession, company, city
- Privacy controls mask PII in directory based on alumni preferences
- Admin endpoints see full data; alumni-to-alumni directory is privacy-filtered
- 80G receipts contain donor PII (name, address, PAN if applicable) — stored as PDF, access restricted to admin

### Multi-tenant Isolation

- All queries filtered by `school_id`
- `requireAlumniContext()` resolves `schoolId` from `AlumniTable.school_id`
- Self-service methods accept `schoolId` parameter and filter queries
- Never trust client-provided `school_id` in self-service requests
- Integration test: alumni from school A cannot access data from school B

### Public Endpoint Security

- `POST /api/v1/alumni/register` — first public account-creation endpoint
- Mitigations: rate limiting, school code validation, honeypot field, email/phone OTP verification, admin approval for mismatches
- No auto-account-creation without SIS match or admin review
- Dedicated endpoint (not `/api/v1/auth/signup` which is locked to parent role via RA-53)

---

## 17. Performance & Scalability

### Expected Scale

- Small school: 500-2,000 alumni
- Medium school: 2,000-10,000 alumni
- Large school: 10,000-50,000 alumni
- Multi-school: 100,000+ alumni across tenants

### Query Optimization

- **Directory search:** Composite index `idx_alumni_school_year` on `(school_id, graduation_year)`. Two-stage query: SQL filters visibility, Kotlin masks fields. Pagination max 50.
- **Privacy-filtered directory:** SQL: `visibility_level = 'public' OR (visibility_level = 'batch' AND graduation_year = :requester_year)`. Excludes `private` entirely. Post-query field masking in Kotlin.
- **Analytics:** Cached for 5 minutes. Aggregate queries on indexed columns.
- **Campaign progress:** `amount_raised` cached, updated on each donation (no recompute).
- **Profile completeness:** Computed on read (not stored). 10 field check per alumni.

### Indexing Strategy

- `idx_alumni_school_year` — directory search
- `idx_alumni_user_id` — JWT resolution (partial index, `WHERE user_id IS NOT NULL`)
- `idx_alumni_verification` — pending queue (partial index, `WHERE verification_status = 'pending'`)
- `idx_alumni_featured` — featured lookup (partial index, `WHERE is_featured = true`)
- `idx_alumni_donations_school` — donation reports by date
- `idx_alumni_career_alumni` — career history with current job first
- `idx_schools_code` — school code lookup (unique, partial index)

### Caching Strategy

- Analytics: 5-minute TTL cache
- Campaign `amount_raised`: cached, updated on donation
- School code lookup: cached in memory (small dataset)
- No cache for directory (privacy filtering requires real-time)

### Pagination

- Directory: max 50 per page
- Donations: max 100 per page
- Mentorships: max 50 per page
- Analytics: no pagination (aggregate)

### Connection Pooling

- Uses existing HikariCP connection pool
- No additional pooling needed for alumni queries

### Async Processing

- 80G receipt PDF generation: synchronous (small file, fast)
- Form 10BD export: synchronous (CSV, fast)
- Bulk import: synchronous (admin waits for result)
- Notification delivery: async (existing `Notify.kt` pattern)

### Scalability Concerns

- Privacy-filtered directory query complexity: O(n) per result for field masking. Mitigated by pagination (max 50).
- Analytics aggregate queries: cached for 5 minutes to reduce DB load.
- Bulk import: process in batches of 100 to avoid long transactions.

---

## 18. Edge Cases

| # | Scenario | Expected Behavior |
|---|---|---|
| EC-1 | Alumnus self-registers but student record has different name | Auto-approve if year + admission_number match (name may differ due to marriage/name change). Admin can edit later. |
| EC-2 | Two alumni with same name and graduation year | Directory shows both; disambiguated by profession/company/city. No uniqueness on name. |
| EC-3 | Alumni deactivated then re-activated | `is_active = true` restored. `user_id` preserved if linked. No data loss. |
| EC-4 | Donation logged for deactivated alumni | Allowed (financial record). Admin can log donations for any alumni regardless of `is_active`. |
| EC-5 | Campaign target reached | `amount_raised >= target_amount`. Campaign remains active until admin closes or `end_date` reached. No auto-close. |
| EC-6 | Campaign end_date passed but still active | `CampaignProgressJob` flags for admin review. Admin manually closes. |
| EC-7 | Alumni tries to view own profile but `verification_status = 'pending'` | `requireAlumniContext()` rejects — returns 403 "Your registration is pending approval." |
| EC-8 | Student requests mentorship from alumni who is no longer a mentor | Check `is_mentor = true` at request time. If false, return 400 "This alumni is not available as a mentor." |
| EC-9 | Alumni exceeds max mentees | Check `maxMenteesPerAlumni` in `MentorshipSettings`. Return 400 "This alumni has reached the maximum number of mentees." |
| EC-10 | Cash donation > ₹2,000 | `is_80g_eligible = false`. Receipt not generated. Admin informed in UI. |
| EC-11 | School 80G certificate expired | Check `g80_validity_date >= donation_date`. If expired, `is_80g_eligible = false`. |
| EC-12 | Duplicate self-registration (same student_id) | Check existing `alumni` record with `student_id`. If exists, return 409 "You're already registered as an alumni of this school." |
| EC-13 | Bulk import with missing required fields | Skip row, collect in `BulkImportResult.errors`. Required: `name`, `graduation_year`. |
| EC-14 | Alumni photo upload > 5MB | Return 413 "File too large. Maximum 5MB for profile photos." |
| EC-15 | Graduation year in future | Validate `graduation_year <= current_year + 1`. Return 400 if invalid. |
| EC-16 | Alumni searches directory but own visibility is `private` | Can still search and see others (based on their settings). Own profile excluded from others' results. |
| EC-17 | School code not found during registration | Return 404 "School not found. Please check the school code or search by name." |
| EC-18 | Alumni with `user_id = null` (bulk imported, not self-registered) | Cannot login. Admin can see in directory. When alumnus self-registers, links to existing record via `student_id` match. |

---

## 19. Error Handling

### Error Response Format

Standard `ApiResponse` error format:
```json
{
  "success": false,
  "error": {
    "code": "ALUMNI_NOT_FOUND",
    "message": "Alumni not found",
    "details": {}
  }
}
```

### Error Codes

| Code | HTTP Status | Description | User Message |
|---|---|---|---|
| `ALUMNI_NOT_FOUND` | 404 | Alumni record not found | "Alumni not found." |
| `ALUMNI_ALREADY_REGISTERED` | 409 | Duplicate registration | "You're already registered as an alumni of this school." |
| `ALUMNI_PENDING_VERIFICATION` | 403 | Self-service accessed before approval | "Your registration is pending approval." |
| `ALUMNI_NOT_MENTOR` | 400 | Mentorship requested from non-mentor | "This alumni is not available as a mentor." |
| `ALUMNI_MAX_MENTEES` | 400 | Max mentees exceeded | "This alumni has reached the maximum number of mentees." |
| `CAMPAIGN_NOT_FOUND` | 404 | Campaign not found | "Campaign not found." |
| `CAMPAIGN_CLOSED` | 400 | Donation to closed campaign | "This campaign is no longer accepting donations." |
| `DONATION_NOT_FOUND` | 404 | Donation not found | "Donation not found." |
| `RECEIPT_NOT_ELIGIBLE` | 400 | 80G receipt requested for ineligible donation | "This donation is not 80G-eligible." |
| `SCHOOL_NOT_FOUND` | 404 | School code/name not found | "School not found. Please check the school code or search by name." |
| `SIS_MATCH_FAILED` | 200 | Self-registration SIS match failed (not an error — returns pending status) | "Your registration is pending admin review." |
| `MENTORSHIP_NOT_FOUND` | 404 | Mentorship not found | "Mentorship not found." |
| `MENTORSHIP_REQUEST_NOT_FOUND` | 404 | Request not found | "Mentorship request not found." |
| `FILE_TOO_LARGE` | 413 | Photo upload > 5MB | "File too large. Maximum 5MB for profile photos." |
| `INVALID_GRADUATION_YEAR` | 400 | Year out of range | "Please enter a valid graduation year." |
| `RATE_LIMIT_EXCEEDED` | 429 | Registration rate limit | "Too many registration attempts. Please try again later." |

### Error Handling Strategy

- **Validation errors:** Return 400 with field-specific message
- **Auth errors:** Return 401/403 with clear message
- **Not found:** Return 404
- **Conflicts:** Return 409
- **Rate limit:** Return 429 with `Retry-After` header
- **Server errors:** Return 500 with generic message; log full error
- **SIS match failure:** Return 200 with `status: pending_review` (not an error)

### Retry Strategy

- Client retries: 3 attempts with exponential backoff for 5xx errors
- No retry for 4xx errors (client errors)
- WhatsApp delivery: 3 retries with 5-second intervals (existing pattern)

### Fallback Behavior

- WhatsApp not available: Fall back to push notification only
- PDF generation fails: Return error to admin; donation still logged
- Analytics cache miss: Compute on-demand (slower but correct)
- Notification preferences missing: Create default preferences on-the-fly

---

## 20. Analytics & Reporting

### Analytics Dashboard Data

| Metric | Source | Derivation |
|---|---|---|
| Total alumni | `AlumniTable` count where `is_active = true` | Direct count |
| Active alumni (90 days) | `AlumniTable.last_active_at` | Count where `last_active_at > now() - 90 days` |
| Pending verifications | `AlumniTable.verification_status = 'pending'` | Direct count |
| By graduation year | `AlumniTable.graduation_year` | Group + count |
| By profession | `AlumniTable.current_profession` | Group + count |
| By city | `AlumniTable.city` | Group + count |
| Total donations | `AlumniDonationsTable.amount` | Sum |
| Active campaigns | `AlumniDonationCampaignsTable.status = 'active'` | Count + sum raised |
| Active mentorships | `AlumniMentorshipsTable.status = 'active'` | Count |
| Pending mentorship requests | `AlumniMentorshipRequestsTable.status = 'pending'` | Count |
| Engagement rate | Active / Total * 100 | Derived |
| Career distribution | `AlumniCareerHistoryTable.industry` | Group + count where `is_current = true` |

### Export Capabilities

- Alumni directory export (CSV) — filtered by year, profession, city
- Donation report export (CSV) — for accounting, Form 10BD filing
- Campaign donor list export (CSV)
- Engagement report export (CSV) — for NAAC/NIRF documentation if applicable

### Report Types

| Report | Format | Frequency | Recipient |
|---|---|---|---|
| Alumni directory | CSV | On-demand | School Admin |
| Donation report | CSV | On-demand | School Admin |
| Form 10BD | CSV | Annual | School Admin (tax filing) |
| Campaign donor list | CSV | On-demand | School Admin |
| Engagement report | CSV | On-demand | School Admin |
| Analytics overview | JSON (API) | On-demand | School Admin |

---

## 21. Testing Strategy

### Unit Tests

- `AlumniService` — all methods (CRUD, graduation, verification, campaigns, donations, mentorship, analytics)
- `AlumniReceiptService` — 80G receipt generation, Form 10BD export
- `AlumniAccess` — context resolution, auth guards
- `AlumniMapper` — profile completeness calculation
- Privacy filtering logic — field masking, visibility level filtering
- SIS verification matching logic
- 80G eligibility checks (cash > ₹2,000, certificate validity)

### Integration Tests

- Graduation flow: mark students graduated → verify alumni records created → verify enrollment status
- Self-registration: SIS match → auto-approve → account created → login works
- Self-registration: no match → pending → admin approves → account created
- Donation flow: log donation → campaign amount_raised updated → receipt generated
- Mentorship flow: student requests → alumni accepts → mentorship created → admin ends
- Privacy filtering: alumni A (public) visible to alumni B; alumni C (private) not visible
- Multi-tenant: alumni from school A cannot access school B data
- Bulk import: CSV with valid + invalid rows → correct success/error counts

### E2E Tests

- Admin graduates students → alumni appears in directory
- Alumni self-registers → admin approves → alumni logs in → views profile
- Admin creates campaign → logs donation → receipt generated → alumni views own donations
- Student requests mentorship → alumni accepts → both notified

### Performance Tests

- Directory search with 10,000 alumni: < 500ms response time
- Analytics overview with 10,000 alumni: < 1s response time (cached)
- Bulk import 1,000 rows: < 10 seconds

### Test Data

- 100 sample alumni across 5 graduation years
- 10 donation campaigns (active, closed, paused)
- 50 donations across campaigns
- 20 mentorships (active, ended)
- 30 mentorship requests (pending, accepted, declined)
- 5 schools with different 80G certificate statuses

### Test Environment

- Test database with schema migration applied
- Mock WhatsApp API for notification tests
- Mock SupabaseStorage for file upload tests
- Test JWT tokens for admin, alumni (approved), alumni (pending) roles

---

## 22. Acceptance Criteria

### Core (Phase 1)

- [ ] Admin can mark graduated students as alumni (bulk graduation)
- [ ] Alumni directory searchable by year, profession, location, company, industry
- [ ] Admin can create/edit/deactivate alumni records
- [ ] Admin can bulk import past student rosters as alumni
- [ ] Donation tracking with purpose, amount, payment mode
- [ ] 80G-compliant receipt generation (when school has 80G certificate)
- [ ] Form 10BD export for annual tax filing
- [ ] Donation campaigns with goals, progress meters, batch targeting
- [ ] Mentorship: admin can create mentorships, alumni can volunteer
- [ ] Alumni newsletter via announcements with `ALUMNI` audience type
- [ ] Batch-wise targeting for announcements (`ALUMNI` + `graduation_year`)
- [ ] All admin endpoints enforce `requireSchoolContext()` + `requireSchoolAdmin()`
- [ ] `alumni.user_id` linked to `app_users.id` for notification delivery
- [ ] Analytics dashboard with engagement, donation, career metrics
- [ ] Featured alumni spotlight
- [ ] Web admin pages (directory, detail, campaigns, analytics)
- [ ] Compose app admin screens (5 tabs, detail, campaign)

### Self-Service (Phase 2)

- [ ] Alumni self-registration with SIS verification (auto-approve + pending queue)
- [ ] Alumni can view/edit own profile
- [ ] Alumni can manage privacy settings (field-level visibility)
- [ ] Alumni can add career history entries
- [ ] Alumni can volunteer as mentor
- [ ] Students/parents can request mentorship from alumni
- [ ] Alumni can accept/decline mentorship requests
- [ ] Alumni directory search (privacy-filtered)
- [ ] Alumni can view own donation history
- [ ] All self-service endpoints enforce `requireAlumniContext()`
- [ ] Profile completeness indicator displayed
- [ ] WhatsApp notifications for mentorship requests, campaign launches, verification approval
- [ ] Alumni photo upload via dedicated endpoint
- [ ] Alumni portal with bottom nav (Profile, Directory, Mentorship, Donations)

### Events (Phase 3 — depends on `EVENT_REGISTRATION_SPEC.md`)

- [ ] Alumni events with RSVP and ticketing
- [ ] Reunion management with attendee dashboard

---

## 23. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration (`052`), 6 Exposed table objects, `DatabaseFactory` registration, `schools` table alterations |
| 2 | 1 day | `AlumniAccess.kt` (auth guard), update `JwtConfig.kt` (alumni role), `EnrollmentsTable` status update, `roleNormalised()` update |
| 3 | 3 days | `AlumniService.kt` — CRUD, graduation, bulk import, verification, campaigns, donations, 80G receipts, mentorship requests, analytics |
| 4 | 1 day | `AlumniRouting.kt` with all endpoints + DTOs, mount in `Application.kt` |
| 5 | 1 day | Notification integration: `ALUMNI` audience type, `NotifyRecipients` resolver, `CalendarEventsTable` audience, WhatsApp integration, notification preferences defaults |
| 6 | 2 days | Client: `AlumniApi.kt`, repository, domain models, Koin registration |
| 7 | 3 days | Client admin UI: `AlumniScreen.kt` (5 tabs), `AlumniDetailScreen.kt`, `AlumniCampaignScreen.kt`, wire into `SchoolPortalV2.kt`, `NavGraphV2.kt` |
| 8 | 2 days | Web admin: nav entry + 4 pages (directory, detail, campaigns, analytics) |
| 9 | 2 days | Tests (server unit + integration, client unit) |
| 10 | Deferred | Alumni self-service UI (Phase 2): registration, profile, directory, mentorship, donations |
| 11 | Deferred | Alumni events with RSVP (Phase 3 — depends on `EVENT_REGISTRATION_SPEC.md`) |

### Pre-Implementation Checklist

- [ ] Verify `NotificationPreferencesTable` schema — exact column names and defaults for migration backfill
- [ ] Audit all enrollment status queries — `grep -r "status" server/.../feature/school/ --include="*.kt"` — verify none use `status != 'withdrawn'`
- [ ] Check if Ktor `RateLimit` plugin is already installed — if not, add to `Application.kt`
- [ ] Check if `gradle/libs.versions.toml` has a PDF library — if not, add OpenPDF dependency
- [x] `StudentsTable` has `studentCode` (text, uniqueIndex) AND `rollNumber` (text) — Confirmed at `Tables.kt:533-537`
- [x] `JwtConfig.issueToken()` accepts arbitrary role strings — Confirmed at `JwtConfig.kt:89-98`
- [x] `SupabaseStorage.upload()` accepts `kind = "PROFILE"` — Confirmed in `MediaRouting.kt:76`
- [x] `EnrollmentsTable.endDate` exists — Confirmed at `Tables.kt:578`
- [x] `StudentsTable` has no `graduation_year` column — Confirmed. Graduation year captured on `alumni` table.
- [x] `StudentsTable.studentCode` global unique index — Keep existing. No schema change.

### Recommended Bottleneck Resolution Order

| Order | Bottleneck | Why First | Phase |
|---|---|---|---|
| 1 | B2 — Auth role + `AlumniAccess.kt` | Unblocks all self-service endpoints | Phase 1 |
| 2 | B3 — Notification preferences defaults | Unblocks push notification delivery | Phase 1 |
| 3 | B7 — 80G school settings field | Unblocks receipt generation; migration must ship with `052` | Phase 1 |
| 4 | B4 — PDF library dependency | Unblocks receipt PDFs; add to `build.gradle.kts` early | Phase 1 |
| 5 | B9 — Enrollment status audit | Must complete before any lifecycle code | Phase 1 |
| 6 | B8 — Privacy directory query design | Most complex query; design and test early | Phase 2 |
| 7 | B10 — Public endpoint security | Must be secure before exposing registration | Phase 2 |
| 8 | B6 — WhatsApp templates | Submit for approval early (external dependency, 24-48hr) | Phase 1-2 |
| 9 | B14 — Multi-tenant scoping audit | Security-critical; verify during Phase 2 self-service | Phase 2 |
| 10 | B12 — FCM token registration | Verify during Phase 2 when alumni client ships | Phase 2 |
| 11 | B13 — Photo upload role access | Verify during Phase 2 | Phase 2 |
| 12 | B11 — Bulk import field mapping | Build during Phase 1 but can ship after core | Phase 1 |
| 13 | B5 — Payment gateway | Future scope; do not block alumni feature | Phase 2+ (separate spec) |
| 14 | B1 — EventRegistrationsTable | External dependency on event registration spec | Phase 3 |

---

## 24. File-Level Impact Analysis

### Required — Phase 1 (25 files)

| # | File | Change Type | Description |
|---|---|---|---|
| 1 | `docs/db/migration_052_alumni_management.sql` | New | DDL: 6 tables with FK constraints, `user_id`, privacy, verification, 80G, campaigns + `schools` alterations |
| 2 | `server/.../db/Tables.kt` | Modify | Add 6 table objects + update `EnrollmentsTable.status` comment + `CalendarEventsTable.audience` comment |
| 3 | `server/.../db/DatabaseFactory.kt` | Modify | Register 6 tables in `allTables` array (ordered by FK dependencies) |
| 4 | `server/.../feature/alumni/AlumniRouting.kt` | New | Route definitions + all DTOs + `alumniRouting()` function |
| 5 | `server/.../feature/alumni/AlumniService.kt` | New | Core business logic (CRUD, graduation, import, verification, campaigns, donations, 80G, mentorship, analytics) |
| 6 | `server/.../feature/alumni/AlumniReceiptService.kt` | New | 80G receipt PDF generation + Form 10BD CSV export |
| 7 | `server/.../core/AlumniAccess.kt` | New | Auth guard for alumni self-service (`requireAlumniContext()`) |
| 8 | `server/.../core/JwtConfig.kt` | Modify | Add `"alumni"` to valid JWT role claims |
| 9 | `server/.../Application.kt` | Modify | Import + mount `alumniRouting()` |
| 10 | `server/.../feature/announcements/AnnouncementRouting.kt` | Modify | Add `"ALUMNI"` to `VALID_AUDIENCE_TYPES` + batch-year filter support |
| 11 | `server/.../feature/notifications/NotifyRecipients.kt` | Modify | Add `alumniInSchool()` resolver + `ALUMNI` audience case |
| 12 | `shared/.../feature/alumni/data/remote/AlumniApi.kt` | New | Ktor client for all alumni endpoints |
| 13 | `shared/.../feature/alumni/data/repository/AlumniRepository.kt` | New | Repository interface |
| 14 | `shared/.../feature/alumni/data/repository/AlumniRepositoryImpl.kt` | New | Repository implementation |
| 15 | `shared/.../feature/alumni/domain/model/Alumni.kt` | New | Domain models (Alumni, Donation, Campaign, Mentorship, Request, CareerHistory, Analytics) |
| 16 | `shared/.../di/Koin.kt` | Modify | Register `AlumniApi` + `AlumniRepository` |
| 17 | `composeApp/.../ui/v2/screens/school/AlumniScreen.kt` | New | Admin alumni management (5 tabs: directory, verification, donations, mentorship, analytics) |
| 18 | `composeApp/.../ui/v2/screens/school/AlumniDetailScreen.kt` | New | Single alumni profile with career timeline, donations, mentorships |
| 19 | `composeApp/.../ui/v2/screens/school/AlumniCampaignScreen.kt` | New | Campaign detail with donor list + progress |
| 20 | `composeApp/.../ui/v2/screens/school/SchoolPortalV2.kt` | Modify | Add `SchoolOverlay.Alumni` + navigation entry under People tab |
| 21 | `composeApp/.../ui/v2/screens/school/SchoolStudentsRouting.kt` | Modify | Add "Mark as Alumni" bulk action in student roster |
| 22 | `composeApp/.../ui/v2/navigation/NavGraphV2.kt` | Modify | Add alumni deep link support (`alumni`, `alumni/{id}`) |
| 23 | `website/src/lib/admin/nav.ts` | Modify | Add alumni nav item |
| 24 | `website/src/app/admin/alumni/page.tsx` | New | Web admin alumni directory page |
| 25 | `website/src/app/admin/alumni/[id]/page.tsx` | New | Web admin alumni detail page |

### Optional — Phase 2 (8 files)

| # | File | Change Type | Description |
|---|---|---|---|
| 26 | `composeApp/.../ui/v2/screens/alumni/AlumniRegistrationScreen.kt` | New | Self-registration with SIS verification |
| 27 | `composeApp/.../ui/v2/screens/alumni/AlumniProfileScreen.kt` | New | Self-service profile + privacy + career history |
| 28 | `composeApp/.../ui/v2/screens/alumni/AlumniDirectoryScreen.kt` | New | Privacy-filtered directory search |
| 29 | `composeApp/.../ui/v2/screens/alumni/AlumniMentorshipScreen.kt` | New | Mentorship requests + active mentorships |
| 30 | `composeApp/.../ui/v2/screens/alumni/AlumniDonationScreen.kt` | New | Own donations + active campaigns |
| 31 | `website/src/app/admin/alumni/campaigns/page.tsx` | New | Web admin campaign management |
| 32 | `website/src/app/admin/alumni/analytics/page.tsx` | New | Web admin analytics dashboard |
| 33 | `server/.../feature/alumni/AlumniAnalyticsService.kt` | New | Dedicated analytics aggregation (if service gets too large) |

---

## 25. Future Enhancements

| # | Enhancement | Priority | Effort | Notes |
|---|---|---|---|---|
| F-1 | Online donation payments (Razorpay/Cashfree) | Medium | L | Phase 2+; separate spec `ALUMNI_PAYMENT_INTEGRATION_SPEC.md` |
| F-2 | Alumni events with RSVP and ticketing | Medium | M | Depends on `EVENT_REGISTRATION_SPEC.md` |
| F-3 | Reunion management with attendee dashboard | Low | M | Phase 3 |
| F-4 | Alumni ID card generation | Low | S | Not in current scope |
| F-5 | Connection graph / networking visualization | Low | L | Future |
| F-6 | AI-powered career recommendations | Low | L | Future: AI infrastructure |
| F-7 | Class community feed | Low | L | Future: separate spec |
| F-8 | Alumni job board | Low | M | Not in K-12 scope |
| F-9 | Mentorship session tracking with feedback | Medium | S | Enhance mentorship with session logs |
| F-10 | Alumni giving day / fundraising event | Low | M | Time-bound campaign variant |
| F-11 | Alumni mentorship analytics | Low | S | Track mentorship outcomes |
| F-12 | Batch-wise reunion planning tools | Low | M | Group messaging, venue coordination |
| F-13 | Alumni testimonial collection | Low | S | For school marketing |
| F-14 | Alumni-student career talk scheduling | Medium | M | Calendar integration for talks |

---

## Appendix A: Sequence Diagrams

### A.1 Admin Bulk Graduation

```
Admin         AlumniService         EnrollmentsTable      AlumniTable
  │                │                      │                    │
  │──graduateStudents(studentIds, year)──→│                    │
  │                │──update status='graduated'──────────────→│
  │                │──set endDate=graduation_date────────────→│
  │                │──insert alumni records─────────────────→│
  │                │←──created alumni list───────────────────│
  │←──List<AlumniDto>│                     │                    │
  │                │                      │                    │
```

### A.2 Alumni Self-Registration with SIS Verification

```
Alumnus        AlumniService         StudentsTable      AppUsersTable      AlumniTable
  │                │                      │                  │                  │
  │──register(dto)─→│                      │                  │                  │
  │                │──match(year, admission_number)─────────→│                  │
  │                │←──match result──────────────────────────│                  │
  │                │                      │                  │                  │
  │                │  [if matched]        │                  │                  │
  │                │──create app_user (role='alumni')────────→│                  │
  │                │──create alumni (verification='approved', user_id=...)──────→│
  │                │──send welcome notification              │                  │
  │←──RegistrationResult(status='approved')│                 │                  │
  │                │                      │                  │                  │
  │                │  [if not matched]    │                  │                  │
  │                │──create alumni (verification='pending', user_id=null)──────→│
  │←──RegistrationResult(status='pending_review')│           │                  │
  │                │                      │                  │                  │
  │                │  [admin approves]    │                  │                  │
  │                │──create app_user─────→│                  │                  │
  │                │──update alumni (verification='approved', user_id=...)──────→│
  │                │──send approval notification              │                  │
```

### A.3 Donation Logging with 80G Receipt

```
Admin          AlumniService         AlumniDonationsTable   CampaignsTable     AlumniReceiptService
  │                │                      │                    │                    │
  │──createDonation(dto)─→│               │                    │                    │
  │                │──check 80G eligibility (cash>2000? cert valid?)              │
  │                │──insert donation────→│                    │                    │
  │                │──update amount_raised───────────────────→│                    │
  │                │──generate receipt PDF──────────────────────────────────────→│
  │                │←──PDF bytes─────────────────────────────────────────────────│
  │                │──update receipt_number, receipt_issued──→│                    │
  │                │──send donation notification to alumni    │                    │
  │←──AlumniDonationDto│                    │                    │                    │
  │                │                      │                    │                    │
```

### A.4 Mentorship Request Flow

```
Parent         AlumniService         MentorshipRequestsTable   NotifyService      Alumni (via push)
  │                │                      │                      │                    │
  │──requestMentorship(alumniId, msg)──→│                      │                    │
  │                │──check is_mentor=true│                     │                    │
  │                │──check max_mentees   │                     │                    │
  │                │──insert request─────→│                     │                    │
  │                │──send notification──────────────────────→│                    │
  │                │                      │                      │──push────────────→│
  │←──requestDto──│                      │                      │                    │
  │                │                      │                      │                    │
  │                │  [alumni accepts]    │                      │                    │
  │                │──update request status='accepted'────────→│                    │
  │                │──create mentorship record                 │                    │
  │                │──notify parent (accepted)────────────────→│                    │
  │                │                      │                      │                    │
```

### A.5 Privacy-Filtered Directory Search

```
Alumnus        AlumniService         AlumniTable          (privacy filter)
  │                │                      │                    │
  │──searchDirectory(filters)──→│         │                    │
  │                │──resolve requester's graduation_year     │
  │                │──SQL: WHERE visibility='public'          │
  │                │     OR (visibility='batch' AND year=:requester_year)│
  │                │     AND is_active=true AND verification='approved'│
  │                │──execute query────────→│                    │
  │                │←──raw rows────────────│                    │
  │                │──apply field masking (show_phone, show_email, show_linkedin)│
  │                │──compute profile_completeness per row     │
  │←──PaginatedResult<AlumniDto>│           │                    │
  │                │                      │                    │
```

---

## Appendix B: Domain Model / ER Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              schools                                     │
│  id (PK)  school_code  name  pan_number  g80_registration_number        │
│  g80_validity_date  g80_certificate_url                                 │
└──────────────────────────┬──────────────────────────────────────────────┘
                           │
           ┌───────────────┼───────────────────────────┐
           │               │                           │
           ▼               ▼                           ▼
┌─────────────────┐ ┌──────────────────┐ ┌──────────────────────────────┐
│     alumni       │ │ alumni_donation_  │ │ alumni_donations             │
│                  │ │ campaigns         │ │                              │
│ id (PK)         │ │ id (PK)           │ │ id (PK)                      │
│ school_id (FK)  │ │ school_id (FK)    │ │ school_id (FK)               │
│ student_id (FK) │ │ title             │ │ alumni_id (FK→alumni)        │
│ user_id (FK)    │ │ target_amount     │ │ campaign_id (FK→campaigns)   │
│ name            │ │ amount_raised     │ │ amount                       │
│ graduation_year │ │ target_batch_year │ │ purpose                      │
│ profession      │ │ start_date        │ │ donation_date                │
│ company, city   │ │ end_date          │ │ payment_mode                 │
│ is_mentor       │ │ status            │ │ receipt_number (UNIQUE)      │
│ is_featured     │ └──────────────────┘ │ receipt_issued                │
│ verification    │                      │ is_80g_eligible              │
│ show_phone      │                      └──────────────────────────────┘
│ show_email      │
│ show_linkedin   │      ┌──────────────────────────────┐
│ visibility_level│      │ alumni_mentorship_requests    │
│ last_active_at  │      │ id (PK)                       │
│ is_active       │      │ school_id (FK)                │
└────────┬────────┘      │ alumni_id (FK→alumni)         │
         │               │ student_id (FK→students)      │
         │               │ requested_by (FK→app_users)   │
         │               │ expertise_area                │
         │               │ message                       │
         │               │ status                        │
         │               └──────────┬───────────────────┘
         │                          │
         │               ┌──────────▼───────────────────┐
         │               │ alumni_mentorships            │
         │               │ id (PK)                       │
         │               │ school_id (FK)                │
         │               │ alumni_id (FK→alumni)         │
         │               │ student_id (FK→students)      │
         │               │ request_id (FK→requests)      │
         │               │ status                        │
         │               │ start_date, end_date          │
         │               │ session_count                 │
         │               └──────────────────────────────┘
         │
         │               ┌──────────────────────────────┐
         └──────────────→│ alumni_career_history         │
                         │ id (PK)                       │
                         │ alumni_id (FK→alumni)         │
                         │ job_title, company            │
                         │ industry                      │
                         │ start_date, end_date          │
                         │ is_current                    │
                         └──────────────────────────────┘
```

---

## Appendix C: Event Flow

### Domain Events

| Event | Emitter | Consumers | Payload | Side Effects |
|---|---|---|---|---|
| `AlumniCreated` | `AlumniService.graduateStudents()` | NotificationService | `alumniId, schoolId, name, graduationYear` | None (admin-created, no welcome notification) |
| `AlumniCreated` | `AlumniService.createAlumni()` | NotificationService | `alumniId, schoolId, name` | None |
| `AlumniCreated` | `AlumniService.bulkImport()` | NotificationService | `alumniId, schoolId, name` | None (no user_id yet) |
| `AlumniRegistered` | `AlumniService.registerAlumni()` | NotificationService | `alumniId, schoolId, name, status` | If approved: send welcome notification |
| `AlumniVerified` | `AlumniService.verifyAlumni()` | NotificationService | `alumniId, schoolId, action` | If approved: create app_user, send welcome notification |
| `DonationLogged` | `AlumniService.createDonation()` | NotificationService, CampaignService | `donationId, alumniId, schoolId, amount, campaignId` | Send donation receipt notification; update campaign amount_raised |
| `CampaignCreated` | `AlumniService.createCampaign()` | NotificationService | `campaignId, schoolId, title, targetBatchYear` | If targetBatchYear: notify alumni in that batch |
| `MentorshipRequested` | `AlumniService.createMentorshipRequest()` | NotificationService | `requestId, alumniId, studentId, schoolId` | Send push + WhatsApp to alumni |
| `MentorshipAccepted` | `AlumniService.respondToMentorshipRequest()` | NotificationService | `requestId, alumniId, studentId, schoolId` | Create mentorship record; notify student's parent |
| `MentorshipDeclined` | `AlumniService.respondToMentorshipRequest()` | NotificationService | `requestId, alumniId, studentId, schoolId` | Notify student's parent |

### Event Delivery Guarantees

- Events are emitted synchronously within the same transaction
- Notification delivery is async (fire-and-forget with logging)
- Failed notifications are logged in `WhatsappLogsTable` and notification logs
- No event bus / message queue — direct function calls

---

## Appendix D: Configuration

### Environment Variables

| Variable | Default | Description |
|---|---|---|
| `ALUMNI_SELF_REG_RATE_LIMIT` | `3` | Registration attempts per IP per hour |
| `ALUMNI_PROFILE_REMINDER_THRESHOLD` | `50` | Completeness % below which reminder sent |
| `ALUMNI_MAX_MENTEES_DEFAULT` | `5` | Max mentees per alumni |
| `ALUMNI_DIRECTORY_MAX_PAGE_SIZE` | `50` | Max results per directory search |
| `ALUMNI_ANALYTICS_CACHE_TTL` | `300` | Analytics cache TTL in seconds (5 min) |

### Feature Flags

| Flag | Default | Description |
|---|---|---|
| `ALUMNI_SELF_REGISTRATION_ENABLED` | `true` | Enable/disable public self-registration endpoint |
| `ALUMNI_MENTORSHIP_ENABLED` | `true` | Enable/disable mentorship feature |
| `ALUMNI_DONATIONS_ENABLED` | `true` | Enable/disable donation tracking |
| `ALUMNI_80G_RECEIPTS_ENABLED` | `true` | Enable/disable 80G receipt generation |
| `ALUMNI_WHATSAPP_OUTREACH_ENABLED` | `true` | Enable/disable WhatsApp notifications for alumni |

### School-Level Settings

| Setting | Stored In | Default | Description |
|---|---|---|---|
| 80G certificate | `schools.g80_registration_number` | NULL | Required for 80G receipts |
| 80G validity date | `schools.g80_validity_date` | NULL | Expiry check for 80G eligibility |
| PAN number | `schools.pan_number` | NULL | Required on 80G receipts |
| School code | `schools.school_code` | auto-generated | Used for self-registration lookup |
| Mentorship eligible classes | `MentorshipSettings.eligibleClassIds` | [11, 12] | Classes eligible for mentorship |
| Max mentees per alumni | `MentorshipSettings.maxMenteesPerAlumni` | 5 | Per-alumni limit |
| Request approval required | `MentorshipSettings.requestApprovalRequired` | true | Admin approval before mentorship starts |

---

## Appendix E: Migration & Rollback

### Migration: `migration_052_alumni_management.sql`

```sql
-- Migration 052: Alumni Management
-- Creates 6 new tables, alters schools table

BEGIN;

-- 1. Alter schools table
ALTER TABLE schools ADD COLUMN IF NOT EXISTS pan_number VARCHAR(20);
ALTER TABLE schools ADD COLUMN IF NOT EXISTS g80_registration_number VARCHAR(50);
ALTER TABLE schools ADD COLUMN IF NOT EXISTS g80_validity_date DATE;
ALTER TABLE schools ADD COLUMN IF NOT EXISTS g80_certificate_url TEXT;
ALTER TABLE schools ADD COLUMN IF NOT EXISTS school_code VARCHAR(20);
CREATE UNIQUE INDEX IF NOT EXISTS idx_schools_code ON schools(school_code) WHERE school_code IS NOT NULL;

-- 2. Create alumni table
CREATE TABLE IF NOT EXISTS alumni (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL REFERENCES schools(id),
    student_id      UUID REFERENCES students(id),
    user_id         UUID REFERENCES app_users(id),
    name            TEXT NOT NULL,
    graduation_year INTEGER NOT NULL,
    last_class      TEXT,
    current_profession TEXT,
    company         TEXT,
    city            TEXT,
    email           TEXT,
    phone           VARCHAR(32),
    linkedin_url    TEXT,
    photo_url       TEXT,
    skills          TEXT,
    achievements    TEXT,
    is_mentor       BOOLEAN NOT NULL DEFAULT false,
    mentor_expertise TEXT,
    is_featured     BOOLEAN NOT NULL DEFAULT false,
    verification_status VARCHAR(16) NOT NULL DEFAULT 'approved',
    verified_at     TIMESTAMP,
    verified_by     UUID REFERENCES app_users(id),
    show_phone      BOOLEAN NOT NULL DEFAULT false,
    show_email      BOOLEAN NOT NULL DEFAULT false,
    show_linkedin   BOOLEAN NOT NULL DEFAULT true,
    visibility_level VARCHAR(16) NOT NULL DEFAULT 'batch',
    last_active_at  TIMESTAMP,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_alumni_school_year ON alumni(school_id, graduation_year);
CREATE INDEX IF NOT EXISTS idx_alumni_user_id ON alumni(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_alumni_verification ON alumni(school_id, verification_status) WHERE verification_status = 'pending';
CREATE INDEX IF NOT EXISTS idx_alumni_featured ON alumni(school_id, is_featured) WHERE is_featured = true;

-- 3. Create alumni_donation_campaigns table
CREATE TABLE IF NOT EXISTS alumni_donation_campaigns (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL REFERENCES schools(id),
    title           TEXT NOT NULL,
    description     TEXT,
    cause           TEXT,
    target_amount   DOUBLE PRECISION NOT NULL,
    amount_raised   DOUBLE PRECISION NOT NULL DEFAULT 0,
    target_batch_year INTEGER,
    start_date      DATE NOT NULL,
    end_date        DATE,
    status          VARCHAR(16) NOT NULL DEFAULT 'active',
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_alumni_campaigns_school ON alumni_donation_campaigns(school_id, status);

-- 4. Create alumni_donations table
CREATE TABLE IF NOT EXISTS alumni_donations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL REFERENCES schools(id),
    alumni_id       UUID NOT NULL REFERENCES alumni(id) ON DELETE CASCADE,
    campaign_id     UUID REFERENCES alumni_donation_campaigns(id),
    amount          DOUBLE PRECISION NOT NULL,
    purpose         TEXT,
    donation_date   DATE NOT NULL,
    payment_mode    VARCHAR(16),
    reference_number TEXT,
    receipt_number  TEXT UNIQUE,
    receipt_issued  BOOLEAN NOT NULL DEFAULT false,
    is_80g_eligible BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_alumni_donations_school ON alumni_donations(school_id, donation_date DESC);
CREATE INDEX IF NOT EXISTS idx_alumni_donations_alumni ON alumni_donations(alumni_id);
CREATE INDEX IF NOT EXISTS idx_alumni_donations_campaign ON alumni_donations(campaign_id) WHERE campaign_id IS NOT NULL;

-- 5. Create alumni_mentorship_requests table
CREATE TABLE IF NOT EXISTS alumni_mentorship_requests (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL REFERENCES schools(id),
    alumni_id       UUID NOT NULL REFERENCES alumni(id) ON DELETE CASCADE,
    student_id      UUID NOT NULL REFERENCES students(id),
    requested_by    UUID NOT NULL REFERENCES app_users(id),
    expertise_area  TEXT,
    message         TEXT,
    status          VARCHAR(16) NOT NULL DEFAULT 'pending',
    responded_at    TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_alumni_mentor_req_school ON alumni_mentorship_requests(school_id, status);
CREATE INDEX IF NOT EXISTS idx_alumni_mentor_req_alumni ON alumni_mentorship_requests(alumni_id, status);

-- 6. Create alumni_mentorships table
CREATE TABLE IF NOT EXISTS alumni_mentorships (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL REFERENCES schools(id),
    alumni_id       UUID NOT NULL REFERENCES alumni(id) ON DELETE CASCADE,
    student_id      UUID NOT NULL REFERENCES students(id),
    request_id      UUID REFERENCES alumni_mentorship_requests(id),
    status          VARCHAR(16) NOT NULL DEFAULT 'active',
    start_date      DATE NOT NULL,
    end_date        DATE,
    notes           TEXT,
    session_count   INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_alumni_mentorships_school ON alumni_mentorships(school_id, status);
CREATE INDEX IF NOT EXISTS idx_alumni_mentorships_alumni ON alumni_mentorships(alumni_id);

-- 7. Create alumni_career_history table
CREATE TABLE IF NOT EXISTS alumni_career_history (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    alumni_id       UUID NOT NULL REFERENCES alumni(id) ON DELETE CASCADE,
    job_title       TEXT NOT NULL,
    company         TEXT NOT NULL,
    industry        TEXT,
    start_date      DATE,
    end_date        DATE,
    is_current      BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_alumni_career_alumni ON alumni_career_history(alumni_id, is_current DESC);

-- 8. Backfill school_code for existing schools
UPDATE schools SET school_code = UPPER(LEFT(slug, 4)) || '-' || SUBSTRING(MD5(RANDOM()::TEXT), 1, 4) WHERE school_code IS NULL;

COMMIT;
```

### Rollback: `migration_052_rollback.sql`

```sql
BEGIN;
DROP TABLE IF EXISTS alumni_career_history;
DROP TABLE IF EXISTS alumni_mentorships;
DROP TABLE IF EXISTS alumni_mentorship_requests;
DROP TABLE IF EXISTS alumni_donations;
DROP TABLE IF EXISTS alumni_donation_campaigns;
DROP TABLE IF EXISTS alumni;
DROP INDEX IF EXISTS idx_schools_code;
ALTER TABLE schools DROP COLUMN IF EXISTS school_code;
ALTER TABLE schools DROP COLUMN IF EXISTS g80_certificate_url;
ALTER TABLE schools DROP COLUMN IF EXISTS g80_validity_date;
ALTER TABLE schools DROP COLUMN IF EXISTS g80_registration_number;
ALTER TABLE schools DROP COLUMN IF EXISTS pan_number;
COMMIT;
```

### Migration Validation

- Verify all 6 tables created with correct columns
- Verify all FK constraints in place
- Verify all indexes created
- Verify `school_code` backfilled for all existing schools
- Verify `school_code` unique index works (no duplicates)
- Run `SELECT count(*) FROM alumni` — should be 0 (new feature)

---

## Appendix F: Observability

### Structured Logging

| Log Level | Event | Context Fields |
|---|---|---|
| INFO | Alumni created (graduation) | `alumniId, schoolId, studentId, graduationYear, adminId` |
| INFO | Alumni created (bulk import) | `alumniId, schoolId, rowNumber, totalRows` |
| INFO | Alumni self-registered | `alumniId, schoolId, sisMatched, status` |
| INFO | Alumni verified | `alumniId, schoolId, action, adminId` |
| INFO | Donation logged | `donationId, schoolId, alumniId, amount, is80gEligible` |
| INFO | Receipt generated | `donationId, receiptNumber, schoolId` |
| INFO | Campaign created | `campaignId, schoolId, title, targetAmount` |
| INFO | Mentorship request created | `requestId, schoolId, alumniId, studentId` |
| INFO | Mentorship request responded | `requestId, schoolId, action, alumniId` |
| WARN | SIS match failed | `schoolId, name, yearOfPassing, admissionNumber` |
| WARN | 80G receipt not eligible | `donationId, reason (cash_limit / cert_expired)` |
| WARN | Rate limit exceeded | `ip, endpoint, attempts` |
| ERROR | PDF generation failed | `donationId, error` |
| ERROR | Notification delivery failed | `alumniId, channel, error` |

### Metrics

| Metric | Type | Labels | Description |
|---|---|---|---|
| `alumni_total` | Gauge | `school_id` | Total alumni per school |
| `alumni_active_90d` | Gauge | `school_id` | Active alumni (90 days) |
| `alumni_pending_verifications` | Gauge | `school_id` | Pending verification count |
| `alumni_registrations_total` | Counter | `school_id, status` | Registration attempts by outcome |
| `alumni_donations_total` | Counter | `school_id, payment_mode` | Donations logged |
| `alumni_donation_amount_total` | Counter | `school_id` | Total donation amount |
| `alumni_campaigns_active` | Gauge | `school_id` | Active campaigns |
| `alumni_mentorships_active` | Gauge | `school_id` | Active mentorships |
| `alumni_directory_search_duration` | Histogram | `school_id` | Directory search latency |
| `alumni_analytics_duration` | Histogram | `school_id` | Analytics query latency |
| `alumni_receipt_generation_duration` | Histogram | `school_id` | PDF generation latency |

### Health Checks

| Check | Endpoint | Description |
|---|---|---|
| Alumni tables exist | `/health/alumni` | Verify 6 alumni tables are accessible |
| School code index | `/health/alumni` | Verify `idx_schools_code` exists |
| 80G certificate check | `/health/alumni` | Count schools with valid 80G certificates |

### Alerts

| Alert | Condition | Severity | Notification |
|---|---|---|---|
| High pending verifications | `alumni_pending_verifications > 50` for 7 days | Warning | Email to school admin |
| Receipt generation failures | `alumni_receipt_generation_duration` error rate > 5% | Critical | PagerDuty / email |
| Self-registration rate limit | `alumni_registrations_total{status="rate_limited"}` > 100/hour | Warning | Email to dev team |
| Campaign target exceeded | `amount_raised > target_amount * 1.5` | Info | Log only (no action needed) |
| Mentorship requests stale | `alumni_mentorship_requests{status="pending"}` age > 30 days | Warning | Email to school admin |

### Dashboards

| Dashboard | Panels | Audience |
|---|---|---|
| Alumni Overview | Total alumni, active %, pending verifications, registrations trend | School Admin |
| Donation Analytics | Total donations, campaign progress, 80G eligibility rate, receipt generation rate | School Admin |
| Mentorship Dashboard | Active mentorships, pending requests, response rate, average sessions | School Admin |
| Engagement Metrics | Profile completeness distribution, last active distribution, directory search volume | School Admin |
| System Health | API latency, error rate, receipt generation latency, notification delivery rate | Dev Team |
