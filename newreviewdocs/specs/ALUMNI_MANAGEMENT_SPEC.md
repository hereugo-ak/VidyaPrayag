# Alumni Management — Technical Specification

> **Document status:** Implemented (85%) — core alumni CRUD, directory, campaigns, donations, mentorship, career history, deep linking, web admin pages shipped; Phase 2 self-service alumni UI deferred
> **Last updated:** 2026-06-28
> **Prerequisites:** None for core features. FR-5 (event RSVP) requires `EVENT_REGISTRATION_SPEC.md` — `EventRegistrationsTable` does not exist yet.
> **Industry benchmark:** SchoolDeck, CodePex ERP, Vidyalaya, Edumerge, Alumnipad, Hivebrite, 360Alumni, Edulab

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

### K-12 Specific Design Decisions

Based on industry research (SchoolDeck, CodePex):
- **Emotional anchor:** batch identity (Class of 1995), school house, sports teams — not departments/degrees
- **Donation scale:** ₹5,000-₹50,000 for library fund, scholarships, infrastructure — not endowments
- **Mentorship model:** alumni mentor Class 11-12 students on career choices — not internships/first jobs
- **Verification:** year of passing + admission/roll number match against SIS records
- **NEP 2020 §4.38:** "Schools shall establish mechanisms for alumni engagement to support current students through mentorship, career guidance, and community projects"

---

## 2. Current System Assessment

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

---

## 3. Functional Requirements

| ID | Requirement | Notes |
|---|---|---|
| FR-1 | Admin marks graduated students as alumni (graduation year, last class) | Requires `requireSchoolAdmin()`. Auto-creates alumni record from student data. |
| FR-2 | Alumni profile: profession, company, location, contact, LinkedIn, skills, achievements | Rich profile with career history and profile completeness indicator |
| FR-3 | Alumni directory searchable by graduation year, profession, location, company, industry | API: `?year=&profession=&city=&company=&industry=&q=&page=&limit=` |
| FR-4 | Alumni self-registration with SIS verification | Auto-match year of passing + admission/roll number. Mismatches → admin review queue. |
| FR-5 | Alumni events/reunions (reuse `CalendarEventsTable`) | **RSVP BLOCKED** — `EventRegistrationsTable` not implemented. Display events via `CalendarEventsTable` with `ALUMNI` audience. Defer ticketing/RSVP until event registration ships. |
| FR-6 | Structured mentorship: alumni volunteer as mentor, students request mentorship | Two-way flow: alumni opt-in → students browse by industry → request → alumni accept/decline. Session tracking with feedback. |
| FR-7 | Alumni newsletter (reuse announcements with `ALUMNI` audience) | Batch-wise targeting: `ALUMNI` + optional `graduation_year` filter. Requires `ALUMNI` audience type + `NotifyRecipients` resolver. |
| FR-8 | Donation tracking with purpose, amount, payment mode | Individual donation records with receipt generation |
| FR-9 | Donation campaigns with goals, progress meters, batch targeting | Campaign creation: cause, target amount, deadline. Live progress. Target specific batches. |
| FR-10 | 80G-compliant tax receipts | Receipt with trust PAN, 80G registration number, donor details, unique receipt ID. Form 10BD export. Cash > ₹2,000 blocked. |
| FR-11 | Privacy controls — alumni control field-level visibility | Hide phone, mask email, show only LinkedIn, restrict to batch year, or go fully private. DPDP Act 2023 compliant. |
| FR-12 | Bulk import past student rosters | CSV/JSON import matching `SchoolStudentsRouting.kt` pattern. Auto-creates alumni records. |
| FR-13 | Analytics dashboard: engagement, donations, career outcomes, growth | Registration growth, active vs inactive, event participation, donation totals, career distribution |
| FR-14 | Career progression tracking | Employment history with timestamps. Alumni update own career info via self-service. |
| FR-15 | Profile completeness indicator | Derived metric: % of profile fields filled. Prompt alumni to complete profiles. |
| FR-16 | Featured alumni spotlight | Admin can mark alumni as "featured" for directory highlight. |
| FR-17 | WhatsApp outreach to alumni | Reuse existing WhatsApp infrastructure for event invitations, donation drives, newsletter delivery |

---

## 4. Authentication & Authorization Model

### 4.1 New Role: `"alumni"`

The system has no alumni role. Add `"alumni"` to `AppUsersTable.role`.

**Approach (recommended):**
- Add `"alumni"` as a valid role in JWT validation (`JwtConfig.kt:89`)
- Create `AlumniAccess.kt` (mirrors `SchoolAccess.kt`) with `requireAlumniContext()` that resolves alumni record from JWT `sub` → `alumni.user_id`
- Alumni login uses existing OTP/login flow with `role = "alumni"`
- Alumni JWT resolves to alumni profile for self-service endpoints
- Alumni can also self-register (FR-4) — registration creates `app_users` with `role = "alumni"` + `alumni` record linked via `user_id`

**Alternative:** Reuse parent accounts, link via `alumni.student_id` → `students.id` → `children.parent_id` → `app_users.id`. More complex, less clean. Not recommended.

### 4.2 Admin Endpoint Guards

All admin endpoints under `/api/v1/school/alumni` must use:
- `call.requireSchoolContext()` — resolves school ID, enforces tenant isolation (pattern: `AnnouncementRouting.kt:146`)
- `call.requireSchoolAdmin()` — for create/update/donation/mentorship/campaign writes (excludes `school_staff`; pattern: `AnnouncementRouting.kt:183`)

### 4.3 Alumni Self-Service Guards

All self-service endpoints under `/api/v1/alumni` must use:
- `call.requireAlumniContext()` — resolves alumni record from JWT, ensures `is_active = true` and `verification_status = 'approved'`

### 4.4 Alumni Self-Registration & Verification Flow

```
1. Alumnus visits registration screen → enters: name, year of passing, admission/roll number, email, phone
2. System auto-matches against StudentsTable (school_id from registration code + year + student_code/roll_number)
   - If match found → auto-approve → create app_users (role=alumni) + alumni record (student_id linked, user_id linked)
   - If partial match (year only) → create alumni record with verification_status = 'pending' → admin review queue
   - If no match → create alumni record with verification_status = 'pending' → admin review queue
3. Admin reviews pending registrations in AlumniScreen → approve/decline
4. On approval → create app_users entry → send welcome notification (push + WhatsApp)
5. Alumnus can now login and use self-service endpoints
```

**Bulk import path (FR-12):** Admin imports CSV of past students → auto-creates alumni records with `verification_status = 'approved'` (admin-verified by default). No `user_id` until alumnus self-registers and links.

---

## 5. Student Lifecycle Transition

### 5.1 Graduation Flow

Currently `EnrollmentsTable.status` supports `active | transferred | withdrawn`. Add `graduated` status.

```
Admin selects student(s) → marks as graduated (graduation year, last class)
  → EnrollmentsTable.status = 'graduated', end_date = graduation date
  → StudentsTable.isActive = false (no longer in active roster)
  → Auto-create AlumniTable record:
      - school_id = student's school
      - student_id = student's id
      - name = student's full_name
      - graduation_year = admin-specified year
      - last_class = student's class_name
      - verification_status = 'approved' (admin-verified)
      - user_id = null (until alumnus self-registers)
      - is_active = true
```

### 5.2 Enrollment Status Update

**File:** `server/.../db/Tables.kt` — `EnrollmentsTable.status` comment update to include `graduated`

**Migration:** Add `CHECK` constraint or document that `graduated` is a valid status value. No schema change needed (varchar(16) already supports it).

---

## 6. Database Design

> **Migration file:** `docs/db/migration_052_alumni_management.sql`
> **Corrected from `066` → `052`** (latest is `migration_051_parent_pulse.sql`)

### 6.1 DDL (Corrected + Expanded)

**Changes from original spec:**
- Added `user_id` column to `alumni` (required for auth + notifications)
- Added FK `REFERENCES` on all `school_id`, `student_id` columns
- Removed denormalized `alumni_name` from `alumni_donations` (JOIN instead)
- Added `updated_at` to `alumni_donations` and `alumni_mentorships`
- Added `ON DELETE CASCADE` to donation/mentorship FKs
- **NEW:** Added `verification_status`, `verified_at`, `verified_by` for SIS verification flow
- **NEW:** Added privacy columns (`show_phone`, `show_email`, `show_linkedin`, `visibility_level`)
- **NEW:** Added `is_featured` for featured alumni spotlight
- **NEW:** Added `skills`, `achievements` for rich profiles
- **NEW:** Added `last_active_at` for engagement tracking
- **NEW:** Added `alumni_donation_campaigns` table for campaign management
- **NEW:** Added `campaign_id` to `alumni_donations` for campaign linking
- **NEW:** Added 80G receipt fields to `alumni_donations`
- **NEW:** Added `alumni_mentorship_requests` table for student-initiated mentorship requests
- **NEW:** Added `alumni_career_history` table for career progression tracking

```sql
CREATE TABLE alumni (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL REFERENCES schools(id),
    student_id      UUID REFERENCES students(id),       -- nullable, if linked to former student
    user_id         UUID REFERENCES app_users(id),      -- nullable, for alumni login + notifications
    name            TEXT NOT NULL,
    graduation_year INTEGER NOT NULL,
    last_class      TEXT,                              -- "Grade 12", "Grade 10"
    current_profession TEXT,
    company         TEXT,
    city            TEXT,
    email           TEXT,
    phone           VARCHAR(32),
    linkedin_url    TEXT,
    photo_url       TEXT,
    skills          TEXT,                              -- comma-separated: "Python, Leadership, Public Speaking"
    achievements    TEXT,                              -- free text
    is_mentor       BOOLEAN NOT NULL DEFAULT false,
    mentor_expertise TEXT,                             -- "Engineering", "Medicine", "Finance"
    is_featured     BOOLEAN NOT NULL DEFAULT false,    -- admin spotlight
    -- Verification (SIS-matched self-registration)
    verification_status VARCHAR(16) NOT NULL DEFAULT 'approved', -- approved | pending | declined
    verified_at     TIMESTAMP,
    verified_by     UUID REFERENCES app_users(id),     -- admin who approved
    -- Privacy controls (DPDP Act 2023)
    show_phone      BOOLEAN NOT NULL DEFAULT false,
    show_email      BOOLEAN NOT NULL DEFAULT false,
    show_linkedin   BOOLEAN NOT NULL DEFAULT true,
    visibility_level VARCHAR(16) NOT NULL DEFAULT 'batch', -- public | batch | private
    -- Engagement
    last_active_at  TIMESTAMP,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_alumni_school_year ON alumni(school_id, graduation_year);
CREATE INDEX idx_alumni_user_id ON alumni(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_alumni_verification ON alumni(school_id, verification_status) WHERE verification_status = 'pending';
CREATE INDEX idx_alumni_featured ON alumni(school_id, is_featured) WHERE is_featured = true;

CREATE TABLE alumni_donation_campaigns (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL REFERENCES schools(id),
    title           TEXT NOT NULL,                     -- "Computer Lab Upgrade"
    description     TEXT,
    cause           TEXT,                              -- "Scholarship Fund", "Library", "Sports Complex"
    target_amount   DOUBLE PRECISION NOT NULL,
    amount_raised   DOUBLE PRECISION NOT NULL DEFAULT 0, -- derived/cache, updated on each donation
    target_batch_year INTEGER,                         -- nullable: target specific batch, null = all
    start_date      DATE NOT NULL,
    end_date        DATE,                              -- nullable: ongoing campaign
    status          VARCHAR(16) NOT NULL DEFAULT 'active', -- active | closed | paused
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_alumni_campaigns_school ON alumni_donation_campaigns(school_id, status);

CREATE TABLE alumni_donations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL REFERENCES schools(id),
    alumni_id       UUID NOT NULL REFERENCES alumni(id) ON DELETE CASCADE,
    campaign_id     UUID REFERENCES alumni_donation_campaigns(id), -- nullable: general donation
    amount          DOUBLE PRECISION NOT NULL,
    purpose         TEXT,                              -- "Scholarship Fund", "Library", "General"
    donation_date   DATE NOT NULL,
    payment_mode    VARCHAR(16),                       -- upi | bank_transfer | cheque | cash | card
    reference_number TEXT,
    -- 80G receipt fields
    receipt_number  TEXT UNIQUE,                       -- auto-generated: SCH-80G-2026-00001
    receipt_issued  BOOLEAN NOT NULL DEFAULT false,
    is_80g_eligible BOOLEAN NOT NULL DEFAULT false,    -- school must have valid 80G certificate
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_alumni_donations_school ON alumni_donations(school_id, donation_date DESC);
CREATE INDEX idx_alumni_donations_alumni ON alumni_donations(alumni_id);
CREATE INDEX idx_alumni_donations_campaign ON alumni_donations(campaign_id) WHERE campaign_id IS NOT NULL;

CREATE TABLE alumni_mentorship_requests (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL REFERENCES schools(id),
    alumni_id       UUID NOT NULL REFERENCES alumni(id) ON DELETE CASCADE,
    student_id      UUID NOT NULL REFERENCES students(id),
    requested_by    UUID NOT NULL REFERENCES app_users(id), -- student's parent or teacher
    expertise_area  TEXT,                              -- "Engineering", "Medicine" — what student wants guidance on
    message         TEXT,                              -- student's request message
    status          VARCHAR(16) NOT NULL DEFAULT 'pending', -- pending | accepted | declined | expired
    responded_at    TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_alumni_mentor_req_school ON alumni_mentorship_requests(school_id, status);
CREATE INDEX idx_alumni_mentor_req_alumni ON alumni_mentorship_requests(alumni_id, status);

CREATE TABLE alumni_mentorships (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL REFERENCES schools(id),
    alumni_id       UUID NOT NULL REFERENCES alumni(id) ON DELETE CASCADE,
    student_id      UUID NOT NULL REFERENCES students(id),
    request_id      UUID REFERENCES alumni_mentorship_requests(id), -- nullable: admin-created has no request
    status          VARCHAR(16) NOT NULL DEFAULT 'active', -- active | ended
    start_date      DATE NOT NULL,
    end_date        DATE,
    notes           TEXT,
    session_count   INTEGER NOT NULL DEFAULT 0,        -- derived: count of sessions logged
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_alumni_mentorships_school ON alumni_mentorships(school_id, status);
CREATE INDEX idx_alumni_mentorships_alumni ON alumni_mentorships(alumni_id);

CREATE TABLE alumni_career_history (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    alumni_id       UUID NOT NULL REFERENCES alumni(id) ON DELETE CASCADE,
    job_title       TEXT NOT NULL,
    company         TEXT NOT NULL,
    industry        TEXT,                              -- "IT", "Healthcare", "Finance", "Civil Services"
    start_date      DATE,
    end_date        DATE,                              -- nullable: current job
    is_current      BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_alumni_career_alumni ON alumni_career_history(alumni_id, is_current DESC);
```

### 6.2 Exposed Table Objects

Add 6 new objects in `server/.../db/Tables.kt` following existing patterns (UUIDTable, school-scoped, indexed):

- `AlumniTable` — mirrors `NonTeachingStaffTable` pattern (school-scoped, `is_active` flag) + verification + privacy columns
- `AlumniDonationCampaignsTable` — school-scoped, campaign management
- `AlumniDonationsTable` — school-scoped, FK to alumni + optional FK to campaign, 80G receipt fields
- `AlumniMentorshipRequestsTable` — school-scoped, student-initiated requests
- `AlumniMentorshipsTable` — school-scoped, FK to alumni + students + optional FK to request
- `AlumniCareerHistoryTable` — FK to alumni, career progression

### 6.3 DatabaseFactory Registration

Register in `server/.../db/DatabaseFactory.kt` `allTables` array after `ParentPulsesTable` (line ~208).
Order matters (FK dependencies):
1. `AlumniTable`
2. `AlumniDonationCampaignsTable`
3. `AlumniDonationsTable` (FK to alumni + campaigns)
4. `AlumniMentorshipRequestsTable` (FK to alumni + students)
5. `AlumniMentorshipsTable` (FK to alumni + students + requests)
6. `AlumniCareerHistoryTable` (FK to alumni)

In production (`AUTO_CREATE_TABLES=false`), `validateSchema()` will refuse to boot if migration hasn't been applied.

### 6.4 Profile Completeness (Derived)

Not stored — computed on read. Formula:
```
completeness = (filled_fields / total_fields) * 100
fields = [name, current_profession, company, city, email, phone, linkedin_url, photo_url, skills, achievements]
```
Returned in `AlumniDto.profileCompleteness` as integer 0-100.

---

## 7. API Contracts

### 7.1 Admin Endpoints — Alumni Management

All require `requireSchoolContext()`. Writes require `requireSchoolAdmin()`.

```
# Alumni CRUD + lifecycle
GET    /api/v1/school/alumni?year=&profession=&city=&company=&industry=&q=&page=1&limit=20  -- directory search
POST   /api/v1/school/alumni                    -- create alumni (admin)
GET    /api/v1/school/alumni/{id}               -- single alumni profile (with career history)
PATCH  /api/v1/school/alumni/{id}               -- update alumni
PATCH  /api/v1/school/alumni/{id}/deactivate    -- soft-delete (is_active=false)
POST   /api/v1/school/alumni/graduate            -- bulk graduate students → alumni { student_ids, graduation_year }
POST   /api/v1/school/alumni/import              -- bulk import CSV/JSON

# Verification queue
GET    /api/v1/school/alumni/pending             -- pending self-registrations
PATCH  /api/v1/school/alumni/{id}/verify         -- approve/decline { action: approve|decline }

# Featured alumni
PATCH  /api/v1/school/alumni/{id}/feature        -- toggle is_featured
```

### 7.2 Admin Endpoints — Donations & Campaigns

```
# Campaigns
GET    /api/v1/school/alumni/campaigns                    -- list campaigns
POST   /api/v1/school/alumni/campaigns                    -- create campaign
GET    /api/v1/school/alumni/campaigns/{id}               -- campaign detail + progress
PATCH  /api/v1/school/alumni/campaigns/{id}               -- update campaign (close, pause)

# Donations
GET    /api/v1/school/alumni/donations?campaign_id=&alumni_id=&page=&limit=  -- all donations
GET    /api/v1/school/alumni/{id}/donations               -- per-alumni donation history
POST   /api/v1/school/alumni/donations                    -- log donation (auto-generates receipt if 80G eligible)
GET    /api/v1/school/alumni/donations/{id}/receipt       -- download 80G receipt (PDF)
GET    /api/v1/school/alumni/donations/80g/form10bd       -- export Form 10BD (CSV, annual filing)
```

### 7.3 Admin Endpoints — Mentorship

```
# Mentorships (admin-managed)
GET    /api/v1/school/alumni/mentorships                   -- all mentorships
POST   /api/v1/school/alumni/mentorships                   -- create mentorship { alumni_id, student_id }
PATCH  /api/v1/school/alumni/mentorships/{id}              -- end mentorship (status=ended, end_date)

# Mentorship requests (student-initiated, admin oversight)
GET    /api/v1/school/alumni/mentorship-requests           -- all requests
PATCH  /api/v1/school/alumni/mentorship-requests/{id}      -- admin override (cancel, force-accept)
```

### 7.4 Admin Endpoints — Analytics

```
GET    /api/v1/school/alumni/analytics/overview            -- counts: total, active, by year, by profession
GET    /api/v1/school/alumni/analytics/engagement          -- engagement metrics (last_active, event attendance, mentorship participation)
GET    /api/v1/school/alumni/analytics/donations           -- donation totals, campaign progress, top donors
GET    /api/v1/school/alumni/analytics/career              -- career distribution by industry, company, city
```

### 7.5 Alumni Self-Service Endpoints

All require `requireAlumniContext()` (resolves alumni from JWT `sub` → `alumni.user_id`).

```
# Profile
GET    /api/v1/alumni/profile                              -- own profile (full, including privacy settings)
PATCH  /api/v1/alumni/profile                              -- update own profile (profession, company, city, etc.)
PATCH  /api/v1/alumni/privacy                              -- update privacy settings { show_phone, show_email, show_linkedin, visibility_level }

# Mentorship
POST   /api/v1/alumni/mentor-volunteer                     -- volunteer as mentor { expertise }
GET    /api/v1/alumni/mentorship-requests                   -- incoming mentorship requests from students
PATCH  /api/v1/alumni/mentorship-requests/{id}             -- accept/decline request { action: accept|decline }
GET    /api/v1/alumni/mentorships                           -- own active mentorships

# Career history
GET    /api/v1/alumni/career-history                        -- own career history
POST   /api/v1/alumni/career-history                        -- add career entry
PATCH  /api/v1/alumni/career-history/{id}                  -- update career entry

# Donations
GET    /api/v1/alumni/donations                             -- own donation history
GET    /api/v1/alumni/campaigns                             -- active campaigns (for viewing)

# Directory (privacy-filtered)
GET    /api/v1/alumni/directory?year=&profession=&city=&q= -- search other alumni (privacy-filtered based on each alumnus's settings)
```

### 7.6 Alumni Self-Registration (Public, no auth)

```
POST   /api/v1/alumni/register                             -- self-register { school_code, name, year_of_passing, admission_number, email, phone }
                                                            -- returns: { status: auto_approved | pending_review, alumni_id }
```

### 7.7 DTO Models

All `@Serializable`, wrapped in `ApiResponse<T>` pattern matching existing code.

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
    val profileCompleteness: Int,  -- derived 0-100
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
    val showLinkedin: Boolean, val visibilityLevel: String  // public | batch | private
)

@Serializable data class AlumniRegisterDto(
    val schoolCode: String, val name: String,
    val yearOfPassing: Int, val admissionNumber: String,
    val email: String, val phone: String
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
    val donorCount: Int,  -- derived
    val createdAt: String, val updatedAt: String
)

@Serializable data class CreateCampaignDto(
    val title: String, val description: String?, val cause: String?,
    val targetAmount: Double, val targetBatchYear: Int? = null,
    val startDate: String, val endDate: String? = null
)

@Serializable data class AlumniDonationDto(
    val id: String, val schoolId: String, val alumniId: String,
    val alumniName: String,  -- resolved via JOIN
    val campaignId: String?, val campaignTitle: String?,  -- resolved via JOIN
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
    val engagementRate: Double  -- % of alumni active in last 90 days
)
```

---

## 8. Notification System Integration

### 8.1 Add `ALUMNI` Audience Type

**File:** `server/.../feature/announcements/AnnouncementRouting.kt:109`

Add `"ALUMNI"` to `VALID_AUDIENCE_TYPES` set.

**Batch-wise targeting:** When audience = `ALUMNI`, support optional `graduation_year` filter in announcement body to target specific batches (e.g., "Class of 1998's collective ask").

### 8.2 Add `ALUMNI` to CalendarEventsTable Audience

**File:** `server/.../db/Tables.kt:1599`

`CalendarEventsTable.audience` currently supports `ALL_SCHOOL | GRADES | CLASSES | SECTIONS | TEACHERS | PARENTS | STUDENTS`. Add `ALUMNI` as a valid value for alumni-specific events (reunions, networking).

### 8.3 Alumni Recipient Resolver

**File:** `server/.../feature/notifications/NotifyRecipients.kt`

Add `alumniInSchool(schoolId, graduationYear: Int? = null)` function — queries `AlumniTable` where `is_active = true AND user_id IS NOT NULL`, optionally filtered by `graduation_year`.

Add `ALUMNI` case to recipient resolution (alongside existing `ALL_SCHOOL`, `CLASS`, etc. cases).

### 8.4 Notification Pipeline

`Notify.kt` already works with any `app_users.id` — it queries `NotificationPreferencesTable` and `DeviceTokensTable` by user ID. As long as alumni have `user_id` linked to `app_users`, the existing FCM pipeline works.

**Verify:** `NotificationPreferencesTable` has default entries for new alumni users, or add a migration default.

### 8.5 WhatsApp Integration for Alumni

Reuse existing WhatsApp infrastructure (`WhatsappLogsTable`, announcement WhatsApp sync) for:
- Event invitations to alumni
- Donation campaign appeals with batch targeting
- Newsletter delivery (announcement with `ALUMNI` audience → WhatsApp + push)
- Mentorship request notifications to alumni

### 8.6 Alumni-Specific Notification Triggers

| Trigger | Recipient | Channel |
|---|---|---|
| Mentorship request received | Alumni | Push + WhatsApp |
| Mentorship request accepted/declined | Student's parent | Push |
| Donation received (receipt generated) | Alumni | Push + Email (PDF receipt) |
| Campaign launched targeting their batch | Alumni in batch | Push + WhatsApp |
| Verification approved | Alumni (pending) | Push + WhatsApp |
| Profile completion reminder (monthly) | Incomplete profiles | Push |

---

## 9. Privacy & DPDP Compliance

### 9.1 Privacy Model

Each alumnus controls their visibility through `AlumniPrivacyDto`:

| Setting | Default | Description |
|---|---|---|
| `show_phone` | `false` | Phone visible to other alumni in directory |
| `show_email` | `false` | Email visible to other alumni in directory |
| `show_linkedin` | `true` | LinkedIn URL visible in directory |
| `visibility_level` | `"batch"` | `public` = all alumni see; `batch` = only same graduation year; `private` = only admin sees |

### 9.2 Directory Query Privacy Filtering

When alumni search the directory (`GET /api/v1/alumni/directory`), results are filtered:
- `public` alumni: visible to all (with field-level privacy applied)
- `batch` alumni: visible only to alumni with same `graduation_year`
- `private` alumni: excluded from directory entirely
- `show_phone = false` → phone field returns `null` in DTO
- `show_email = false` → email field returns `null` in DTO
- `show_linkedin = false` → linkedin_url field returns `null` in DTO

### 9.3 Admin Access

Admin endpoints (`/api/v1/school/alumni/*`) always see full data regardless of privacy settings. Privacy only affects alumni-to-alumni directory visibility.

### 9.4 DPDP Act 2023 Rights

- **Right to access:** Alumni can view own full profile (`GET /api/v1/alumni/profile`)
- **Right to correct:** Alumni can update own profile (`PATCH /api/v1/alumni/profile`)
- **Right to erasure:** Alumni can deactivate own profile (`PATCH /api/v1/alumni/profile` with `isActive = false`). Admin can hard-delete on request.
- **Data export:** `GET /api/v1/alumni/profile` returns all stored data for the alumnus

---

## 10. Service Layer

**New file:** `server/.../feature/alumni/AlumniService.kt`

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
    suspend fun generateReceipt(schoolId: UUID, donationId: UUID): ByteArray  // PDF
    suspend fun exportForm10BD(schoolId: UUID, year: Int): ByteArray  // CSV

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
    suspend fun registerAlumni(dto: AlumniRegisterDto): RegistrationResult  // auto-approve or pending
}
```

---

## 11. Routing

**New file:** `server/.../feature/alumni/AlumniRouting.kt`

Defines `fun Route.alumniRouting()` with all endpoints from section 7.

**Mount in:** `server/.../Application.kt` — add `alumniRouting()` call inside `routing { ... }` block + import.

**Route structure:**
- `/api/v1/school/alumni/*` — admin endpoints (JWT + `requireSchoolContext` / `requireSchoolAdmin`)
- `/api/v1/alumni/*` — alumni self-service endpoints (JWT + `requireAlumniContext`)
- `/api/v1/alumni/register` — public endpoint (no auth)

---

## 12. Client-Side (Compose App)

### 12.1 Shared Module — API Client

New files following existing patterns (`TeacherApi.kt`, `AuthApi.kt`):

- `shared/.../feature/alumni/data/remote/AlumniApi.kt` — Ktor client with `safeApiCall` wrappers for all endpoints
- `shared/.../feature/alumni/data/repository/AlumniRepository.kt` — interface
- `shared/.../feature/alumni/data/repository/AlumniRepositoryImpl.kt` — implementation
- `shared/.../feature/alumni/domain/model/Alumni.kt` — domain models (`Alumni`, `AlumniDonation`, `AlumniDonationCampaign`, `AlumniMentorship`, `AlumniMentorshipRequest`, `CareerHistory`, `AlumniAnalytics`)

### 12.2 Koin DI Registration

**File:** `shared/.../di/Koin.kt` (around line 104)

```kotlin
single { AlumniApi(get(), AppConfig.schoolBaseUrl) }
single<AlumniRepository> { AlumniRepositoryImpl(get()) }
```

### 12.3 Admin UI — School Portal

- New screen: `composeApp/.../ui/v2/screens/school/AlumniScreen.kt` — main alumni management with tabs:
  - **Directory tab** — searchable alumni list with filters (year, profession, city, industry). Profile completeness indicator. Featured alumni badge.
  - **Verification tab** — pending self-registrations queue with approve/decline
  - **Donations tab** — donation list + campaign management with progress bars
  - **Mentorship tab** — mentorship list + student requests overview
  - **Analytics tab** — engagement metrics, donation totals, career distribution charts
- New screen: `composeApp/.../ui/v2/screens/school/AlumniDetailScreen.kt` — single alumni profile with career history timeline, donation history, mentorship history
- New screen: `composeApp/.../ui/v2/screens/school/AlumniCampaignScreen.kt` — campaign detail with donor list + progress
- Add `SchoolOverlay.Alumni` to sealed class in `SchoolPortalV2.kt` — alumni as sub-section under **People** tab
- Add deep link support in `NavGraphV2.kt` — add `"alumni"` and `"alumni/{id}"` to school/admin deep link parser
- **Graduation action** — in student roster, add "Mark as Alumni" bulk action that calls `POST /api/v1/school/alumni/graduate`

### 12.4 Alumni Self-Service UI (Phase 2 — deferred)

- `composeApp/.../ui/v2/screens/alumni/AlumniProfileScreen.kt` — own profile + edit + privacy settings + career history management
- `composeApp/.../ui/v2/screens/alumni/AlumniDirectoryScreen.kt` — privacy-filtered directory of other alumni
- `composeApp/.../ui/v2/screens/alumni/AlumniMentorshipScreen.kt` — incoming mentorship requests + active mentorships
- `composeApp/.../ui/v2/screens/alumni/AlumniDonationScreen.kt` — own donation history + active campaigns
- `composeApp/.../ui/v2/screens/alumni/AlumniRegistrationScreen.kt` — self-registration with SIS verification
- Add alumni role to `NavGraphV2.kt` navigation parsing

---

## 13. Web Admin Dashboard

- Add nav entry in `website/src/lib/admin/nav.ts`: `{ href: "/admin/alumni", label: "Alumni", icon: GraduationCap }`
- New page: `website/src/app/admin/alumni/page.tsx` — alumni directory table with search, filters, export
- New page: `website/src/app/admin/alumni/[id]/page.tsx` — alumni detail with career timeline, donations, mentorships
- New page: `website/src/app/admin/alumni/campaigns/page.tsx` — campaign management dashboard
- New page: `website/src/app/admin/alumni/analytics/page.tsx` — analytics dashboard with charts

---

## 14. Analytics & Reporting

### 14.1 Analytics Dashboard Data

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

### 14.2 Export Capabilities

- Alumni directory export (CSV) — filtered by year, profession, city
- Donation report export (CSV) — for accounting, Form 10BD filing
- Campaign donor list export (CSV)
- Engagement report export (CSV) — for NAAC/NIRF documentation if applicable

---

## 15. Acceptance Criteria

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

### Events (Phase 3 — depends on `EVENT_REGISTRATION_SPEC.md`)
- [ ] Alumni events with RSVP and ticketing
- [ ] Reunion management with attendee dashboard

---

## 16. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration (`052`), 6 Exposed table objects, `DatabaseFactory` registration |
| 2 | 1 day | `AlumniAccess.kt` (auth guard), update `JwtConfig.kt` (alumni role), `EnrollmentsTable` status update |
| 3 | 3 days | `AlumniService.kt` — CRUD, graduation, bulk import, verification, campaigns, donations, 80G receipts, mentorship requests, analytics |
| 4 | 1 day | `AlumniRouting.kt` with all endpoints + DTOs, mount in `Application.kt` |
| 5 | 1 day | Notification integration: `ALUMNI` audience type, `NotifyRecipients` resolver, `CalendarEventsTable` audience, WhatsApp integration |
| 6 | 2 days | Client: `AlumniApi.kt`, repository, domain models, Koin registration |
| 7 | 3 days | Client admin UI: `AlumniScreen.kt` (5 tabs), `AlumniDetailScreen.kt`, `AlumniCampaignScreen.kt`, wire into `SchoolPortalV2.kt`, `NavGraphV2.kt` |
| 8 | 2 days | Web admin: nav entry + 4 pages (directory, detail, campaigns, analytics) |
| 9 | 2 days | Tests (server unit + integration, client unit) |
| 10 | Deferred | Alumni self-service UI (Phase 2): registration, profile, directory, mentorship, donations |
| 11 | Deferred | Alumni events with RSVP (Phase 3 — depends on `EVENT_REGISTRATION_SPEC.md`) |

---

## 17. File-Level Impact Analysis

### Required — Phase 1 (25 files)

| # | File | Change Type | Description |
|---|---|---|---|
| 1 | `docs/db/migration_052_alumni_management.sql` | New | DDL: 6 tables with FK constraints, `user_id`, privacy, verification, 80G, campaigns |
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

## 18. Industry Benchmarking

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

## 19. Review Challenges & Resolutions Summary

| # | Challenge | Resolution | Priority |
|---|---|---|---|
| 1 | Migration number wrong (066) | Fixed to `052` | P0 |
| 2 | Missing FK constraints on all tables | Added `REFERENCES` on all `school_id`, `student_id`, `alumni_id` | P0 |
| 3 | Missing `user_id` column on `alumni` | Added `user_id UUID REFERENCES app_users(id)` — required for auth + notifications | P0 |
| 4 | No `"alumni"` role in auth system | Add role + `AlumniAccess.kt` + update `JwtConfig.kt` | P0 |
| 5 | `EventRegistrationsTable` doesn't exist (FR-5) | Defer RSVP; use `CalendarEventsTable` display only; mark as prerequisite | P0 |
| 6 | No `ALUMNI` audience type in announcements | Add to `VALID_AUDIENCE_TYPES` + resolver in `NotifyRecipients.kt` | P1 |
| 7 | Missing endpoints (single fetch, deactivate, end mentorship, per-alumni donations) | Added to API contract | P1 |
| 8 | Missing DTO/serialization models | Added full DTO definitions (16 DTOs) | P1 |
| 9 | Missing search query parameters | Added `?year=&profession=&city=&company=&industry=&q=&page=&limit=` | P1 |
| 10 | Denormalized `alumni_name` in donations | Removed — use JOIN at query time | P3 |
| 11 | Missing `updated_at` on donations + mentorships | Added to all tables | P3 |
| 12 | No auth model documented | Added section 4 with self-registration flow | P1 |
| 13 | No service layer interface defined | Added section 10 with full method signatures (30+ methods) | P1 |
| 14 | No routing function specified | Added section 11 with file + mount location | P1 |
| 15 | No client API service defined | Added section 12 with all files + Koin registration | P2 |
| 16 | No admin UI navigation entry | Added `SchoolOverlay.Alumni` under People tab + deep link | P2 |
| 17 | No web admin entry | Added nav item + 4 pages | P2 |
| 18 | File impact analysis incomplete (4 files) | Expanded to 25 required + 8 optional | P3 |
| 19 | Prerequisites incorrectly say "None" | Updated to note FR-5 dependency on `EVENT_REGISTRATION_SPEC.md` | P0 |
| 20 | No notification pipeline verification | Noted that `Notify.kt` works with any `app_users.id`; verify `NotificationPreferencesTable` defaults | P2 |
| 21 | **NEW:** No SIS verification flow | Added FR-4 + `verification_status` column + admin review queue + self-registration endpoint | P1 |
| 22 | **NEW:** No student lifecycle transition | Added section 5 — `graduated` enrollment status + auto-create alumni record | P1 |
| 23 | **NEW:** No privacy controls (DPDP Act) | Added FR-11 + privacy columns + directory filtering logic + section 9 | P1 |
| 24 | **NEW:** No donation campaigns | Added `alumni_donation_campaigns` table + FR-9 + campaign endpoints | P1 |
| 25 | **NEW:** No 80G receipt compliance | Added 80G fields to donations + FR-10 + receipt service + Form 10BD export | P1 |
| 26 | **NEW:** No mentorship request flow (student-side) | Added `alumni_mentorship_requests` table + FR-6 + request endpoints | P1 |
| 27 | **NEW:** No bulk import | Added FR-12 + import endpoint matching `SchoolStudentsRouting.kt` pattern | P2 |
| 28 | **NEW:** No analytics dashboard | Added FR-13 + analytics endpoints + section 14 | P2 |
| 29 | **NEW:** No career history tracking | Added `alumni_career_history` table + FR-14 + career endpoints | P2 |
| 30 | **NEW:** No batch-wise targeting for announcements | Added `graduation_year` filter to `ALUMNI` audience type | P1 |
| 31 | **NEW:** No profile completeness indicator | Added FR-15 — derived metric in `AlumniDto` | P3 |
| 32 | **NEW:** No featured alumni | Added FR-16 + `is_featured` column + toggle endpoint | P3 |
| 33 | **NEW:** No WhatsApp integration for alumni | Added FR-17 — reuse existing WhatsApp infra | P2 |
| 34 | **NEW:** No `ALUMNI` in CalendarEventsTable audience | Add `ALUMNI` to valid audience values | P1 |
| 35 | **NEW:** No graduation bulk action in student roster | Added "Mark as Alumni" action in `SchoolStudentsRouting.kt` | P2 |

---

## 20. Bottlenecks, Risks & Mitigations

### 20.1 Hard Blockers

| # | Bottleneck | Impact | Mitigation / Solution | Priority |
|---|---|---|---|---|
| B1 | `EventRegistrationsTable` doesn't exist | FR-5 alumni events with RSVP/ticketing fully blocked | Defer RSVP to Phase 3. Use `CalendarEventsTable` with `ALUMNI` audience for display-only events until `EVENT_REGISTRATION_SPEC.md` ships. Track dependency in implementation roadmap. | P0 |
| B2 | No `"alumni"` role in auth pipeline | All self-service endpoints blocked — alumni can't login or access own profile | **Phase 1 prerequisite.** Add `"alumni"` to `JwtConfig.kt` valid roles, create `AlumniAccess.kt` with `requireAlumniContext()`, update `SecurityModule.kt` to validate alumni JWT claims. Must be done before any self-service endpoint work. | P0 |
| B3 | `NotificationPreferencesTable` no default entries for alumni | `Notify.kt` silently skips push delivery for alumni users — notifications never delivered | **Migration addition:** Add default `NotificationPreferencesTable` rows for any `app_users` with `role = 'alumni'` that don't have entries. Alternatively, add logic in `AlumniService.registerAlumni()` and `verifyAlumni()` to create default preference rows when `app_users` record is created. Pattern: check existing `NotificationPreferencesTable` seeding logic in `DatabaseFactory.kt` or migration scripts. | P0 |

### 20.2 Infrastructure Dependencies

| # | Bottleneck | Impact | Mitigation / Solution | Priority |
|---|---|---|---|---|
| B4 | No PDF generation library on server | `AlumniReceiptService.kt` can't generate 80G receipt PDFs | **Add Gradle dependency:** `com.github.librepdf:openpdf:2.0.3` (lightweight, MIT license, Kotlin-friendly). Add to `server/build.gradle.kts` dependencies block. Alternative: Apache PDFBox (`org.apache.pdfbox:pdfbox:3.0.3`) — heavier but more feature-rich. OpenPDF is sufficient for structured receipt templates. If neither is acceptable, generate receipts as HTML → server-side render via existing Ktor HTML DSL, or use a template engine (Freemarker/Thymeleaf) → return as downloadable HTML with print-to-PDF on client. | P1 |
| B5 | No payment gateway for online donations | Donations can only be logged manually (admin enters offline payments). No online UPI/card payment flow for alumni self-service. | **Phase 1:** Manual entry only — admin logs donations with `payment_mode` and `reference_number`. This is sufficient for cheque/bank transfer/cash. **Phase 2 (future):** Integrate Razorpay or Cashfree for UPI/card payments. Add `alumni_donations.payment_gateway_ref` column, webhook endpoint for payment confirmation, and auto-receipt generation on verified payment. Scope this as a separate spec (`ALUMNI_PAYMENT_INTEGRATION_SPEC.md`) — do not block alumni feature on this. | P2 |
| B6 | WhatsApp template approval for alumni notifications | New WhatsApp message templates (mentorship request, campaign launch, verification approved, donation receipt) must be approved by Meta before use. External dependency with 24-48hr turnaround. | **Action:** Submit WhatsApp template approvals early in Phase 1 (before notification integration work begins). Templates needed: `alumni_mentorship_request`, `alumni_campaign_launch`, `alumni_verification_approved`, `alumni_donation_receipt`. Use existing WhatsApp template submission process from announcement feature. **Fallback:** If templates not yet approved, deliver via push notification only until WhatsApp templates are live. | P1 |
| B7 | No 80G certificate storage on `SchoolsTable` | `is_80g_eligible` flag on donations has no school-level source of truth. Can't generate valid 80G receipts without school's PAN, 80G registration number, and validity period. | **Migration addition:** Add columns to `schools` table: `pan_number VARCHAR(20)`, `g80_registration_number VARCHAR(50)`, `g80_validity_date DATE`, `g80_certificate_url TEXT`. Admin updates these in school settings. `AlumniReceiptService.generateReceipt()` checks `schools.g80_registration_number IS NOT NULL AND g80_validity_date >= donation_date` before generating. If not valid, block `is_80g_eligible = true` and return error. Add school settings UI fields in web admin `school/settings` page. | P1 |

### 20.3 Complexity Risks

| # | Bottleneck | Impact | Mitigation / Solution | Priority |
|---|---|---|---|---|
| B8 | Privacy-filtered directory query is complex | `GET /api/v1/alumni/directory` requires per-row visibility filtering based on requester's `graduation_year` vs each result's `visibility_level`, plus field-level masking. Can't be a simple `SELECT *`. Risk of performance issues with large alumni tables. | **Approach:** Two-stage query: (1) SQL filters `visibility_level = 'public' OR (visibility_level = 'batch' AND graduation_year = :requester_year)` — excludes `private` entirely. (2) Post-query field masking in Kotlin: for each result, if `show_phone = false` set `phone = null`, if `show_email = false` set `email = null`, if `show_linkedin = false` set `linkedin_url = null`. **Performance:** Add composite index `idx_alumni_directory ON alumni(school_id, visibility_level, graduation_year) WHERE is_active = true AND verification_status = 'approved'`. Pagination limit max 50 to prevent large result sets. | P1 |
| B9 | Adding `graduated` to `EnrollmentsTable.status` may break existing queries | Any code filtering enrollment status (active student counts, roster displays, attendance, report cards) may not account for `graduated` status. Could cause graduated students to appear in active rosters or skew metrics. | **Pre-implementation audit:** `grep -r "status" server/.../feature/school/ --include="*.kt"` to find all enrollment status queries. Key files to audit: `SchoolStudentsRouting.kt`, `StudentAggregationService.kt`, attendance routing, report card routing, parent dashboard queries. **Fix:** All queries that filter active students must explicitly use `status = 'active'` (not `status != 'withdrawn'`). Add integration test: graduate a student → verify they disappear from active roster, attendance, and report cards. | P1 |
| B10 | Public self-registration endpoint (`POST /api/v1/alumni/register`) — first public account-creation endpoint | No existing pattern for no-auth account creation. Risk of spam registrations, brute-force school code guessing, abuse. | **Mitigations:** (1) Rate limiting: Use Ktor `RateLimit` plugin — limit to 3 registration attempts per IP per hour. (2) School code validation: `school_code` must match `schools.registration_code` — not easily guessable (existing pattern uses unique codes). (3) Honeypot field: Add hidden `company_name` field in form — if filled, reject as bot. (4) Email/phone verification: Require OTP verification before creating `app_users` record. Reuse existing OTP flow from `AuthApi.kt`. (5) All pending registrations require admin approval — no auto-account-creation without SIS match or admin review. | P1 |
| B11 | Bulk import field mapping differs from student import | Alumni CSV has different fields than `SchoolStudentsRouting.kt` student import (`graduation_year`, `last_class`, `current_profession` etc.). Can't reuse student import logic directly. | **Solution:** Create `AlumniImportService.kt` with its own CSV parser and validation. Required columns: `name`, `graduation_year`, `last_class`. Optional: `email`, `phone`, `current_profession`, `company`, `city`, `linkedin_url`. Validation: skip rows with missing `name` or `graduation_year`, collect errors in `BulkImportResult.errors` list. All imported records get `verification_status = 'approved'`, `user_id = null`. Follow same response pattern as student import: `{ successCount, errorCount, errors: List<ImportError> }`. | P2 |

### 20.4 Cross-Cutting Concerns

| # | Bottleneck | Impact | Mitigation / Solution | Priority |
|---|---|---|---|---|
| B12 | FCM token registration for alumni role | Client-side device token registration may be role-aware. If registration code checks `role == 'parent'` or `role == 'teacher'`, alumni tokens won't register and push notifications won't deliver. | **Audit:** Check `DeviceTokensTable` registration code in shared client module. Search for role checks in token registration flow. **Fix:** Ensure token registration accepts any authenticated user regardless of role. `DeviceTokensTable` is user-scoped (`user_id` FK) — no schema change needed. If role-specific logic exists, add `'alumni'` to the allowed roles list. | P1 |
| B13 | Alumni photo upload — file upload routes may restrict by role | `photo_url` field needs an upload endpoint. If existing file upload routes (e.g., `/api/v1/upload`) check for `parent` or `teacher` roles only, alumni can't upload photos. | **Audit:** Search for file upload routes in `Application.kt` and any `UploadRouting.kt` or similar. Check role guards on upload endpoints. **Fix:** Add `'alumni'` to allowed roles for upload endpoints, or create a dedicated `/api/v1/alumni/photo` upload endpoint with `requireAlumniContext()`. Reuse existing file storage mechanism (S3, local, etc.). | P2 |
| B14 | Multi-tenant scoping for self-service endpoints | Admin endpoints use `requireSchoolContext()` which extracts `school_id` from JWT. Alumni self-service endpoints resolve school from `alumni.school_id` (via `user_id` lookup), not directly from JWT. Risk of school isolation breach if queries don't filter by resolved `school_id`. | **Solution:** `requireAlumniContext()` in `AlumniAccess.kt` must return both `alumniId` and `schoolId`. All self-service service methods must accept `schoolId` parameter and filter queries with `AlumniTable.school_id eq schoolId`. Never trust client-provided `school_id` in self-service requests. Add integration test: alumni from school A cannot access data from school B. | P1 |

### 20.5 Recommended Bottleneck Resolution Order

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

### 20.6 Codebase Cross-Reference Findings (Pre-Implementation Audit)

These are critical gaps discovered by verifying the spec against actual codebase files. Each must be resolved before or during implementation.

| # | Finding | File Verified | Impact | Resolution | Priority |
|---|---|---|---|---|---|
| C1 | **No `school_code` on `SchoolsTable`** — `AlumniRegisterDto.schoolCode` has no matching column. Schools are identified by `slug` or `id` (UUID). | `Tables.kt:187-243` | Self-registration endpoint can't look up school by code — flow is broken. | **Option A (recommended):** Add `school_code VARCHAR(20)` column to `SchoolsTable` in migration `052`. Admin generates/sets a unique code in school settings. Self-registration validates against this. **Option B:** Use `SchoolsTable.slug` instead of a code — but slugs are long and not user-friendly for alumni to type. **Option C:** Use school `id` (UUID) — too long for manual entry. **Decision:** Add `school_code` column with unique index. Admin can set/regenerate it. Default: auto-generate from school name + random 4 chars (e.g., `DPS-AB12`). | P0 |
| C2 | **`/signup` hard-locked to `parent` role (RA-53)** — `AuthRouting.kt:401-419` explicitly rejects any role other than `parent` on public signup. | `AuthRouting.kt:401-419` | Alumni cannot self-register via existing `/api/v1/auth/signup`. The spec's `POST /api/v1/alumni/register` is a separate endpoint, but the conflict with RA-53 must be documented. | **No conflict** — alumni registration uses its own dedicated endpoint `POST /api/v1/alumni/register` (not `/api/v1/auth/signup`). The RA-53 lock on `/signup` is actually a security benefit — it prevents privilege escalation via the generic signup. The alumni endpoint creates `app_users` with `role = "alumni"` server-side after SIS verification or admin approval, never from anonymous client input. **Action:** Document in `AlumniRouting.kt` that the register endpoint is the ONLY sanctioned path for alumni account creation, mirroring the RA-53 pattern. | P0 |
| C3 | **`roleNormalised()` doesn't map `"alumni"`** — `AuthRouting.kt:236-244` maps `admin/school_admin → school_admin`, `teacher → teacher`, `super_admin → super_admin`, else → `parent`. No `"alumni"` case. | `AuthRouting.kt:236-244` | If alumni login sends `role = "alumni"` in the login request, `roleNormalised()` will fall through to `parent`. However, login reads role from DB (`row[AppUsersTable.role]`), not from the request — so this is a non-issue for login. But `roleNormalised()` is used in signup, which alumni don't use. | **No change needed for login** — `/login` reads role from DB row, not from `roleNormalised()`. **Action:** Add `"alumni" → "alumni"` to `roleNormalised()` for completeness and future-proofing (e.g., if check-user is called with role=alumni). | P1 |
| C4 | **`SecurityModule.kt` validates `is_active` but not role** — JWT validation (`SecurityModule.kt:36-51`) only checks `sub` is valid and `is_active = true`. It does NOT validate the role claim against an allowlist. | `SecurityModule.kt:36-51` | Any role in JWT will be accepted as long as the user is active. This is fine — role-based access is enforced by `SchoolAccess.kt`, `TeacherAccess.kt`, and the new `AlumniAccess.kt`, not by the JWT validator. | **No change needed** — `SecurityModule.kt` is role-agnostic by design. `AlumniAccess.kt` will enforce role checks at the route level, same as `SchoolAccess.kt` does for school roles. | P3 |
| C5 | **`SCHOOL_ROLES` set doesn't include `"alumni"`** — `SchoolAccess.kt:37` defines `SCHOOL_ROLES = setOf("school_admin", "school_staff", "admin")`. | `SchoolAccess.kt:37` | Alumni calling admin endpoints will be rejected with 403 — which is correct behavior. Alumni should NOT access school admin endpoints. | **No change needed** — alumni use `requireAlumniContext()`, not `requireSchoolContext()`. The separation is correct. Document that alumni are intentionally excluded from `SCHOOL_ROLES`. | P3 |
| C6 | **`EnrollmentsTable` has `endDate` column** — `Tables.kt:578` confirms `val endDate = date("end_date").nullable()` exists. | `Tables.kt:570-586` | No issue — the spec's graduation flow (`end_date = graduation date`) is supported. | **No change needed** — `endDate` already exists and is nullable. Graduation flow sets `status = 'graduated'` and `endDate = graduation_date`. | P3 |
| C7 | **File upload uses `requireSchoolContext()`** — `MediaRouting.kt:89` calls `requireSchoolContext()` which checks `SCHOOL_ROLES`. Alumni are rejected. | `MediaRouting.kt:89` | Alumni can't upload profile photos via existing `/api/v1/school/media/upload`. | **Resolution:** Create `POST /api/v1/alumni/photo` in `AlumniRouting.kt` with `requireAlumniContext()`. Reuse `SupabaseStorage.upload()` directly (same infrastructure, different auth guard). Path convention: `{schoolId}/profile/{uuid}.{ext}` — school ID resolved from alumni record. Add `"PROFILE"` to valid kinds (already exists in `SupabaseStorage`). Max 5MB (profile photos only). Return `{ photoUrl: String }`. | P1 |
| C8 | **`AppUsersTable.schoolId` is nullable** — Alumni `app_users` rows will have `schoolId = null` (they're not school staff). School association is via `alumni.school_id`. | `Tables.kt:57` | `requireSchoolContext()` reads `schoolId` from `app_users` — will return null for alumni, causing 403. This is correct (alumni shouldn't access school endpoints), but `AlumniAccess.kt` must resolve school from `alumni.school_id`, not from `app_users.schoolId`. | **Resolution:** `AlumniAccess.kt`'s `requireAlumniContext()` must: (1) get `userId` from JWT `sub`, (2) query `AlumniTable` where `user_id = userId AND is_active = true AND verification_status = 'approved'`, (3) return `AlumniContext(alumniId, schoolId, userId)` where `schoolId` comes from `AlumniTable.school_id`. Never read `app_users.schoolId` for alumni context. | P0 |

### 20.7 Updated Migration Requirements Summary

Based on the cross-reference audit, migration `052` must include:

```sql
-- 1. Alumni tables (6 tables — see Section 6.1 DDL)
-- 2. SchoolsTable additions for 80G (B7)
ALTER TABLE schools ADD COLUMN pan_number VARCHAR(20);
ALTER TABLE schools ADD COLUMN g80_registration_number VARCHAR(50);
ALTER TABLE schools ADD COLUMN g80_validity_date DATE;
ALTER TABLE schools ADD COLUMN g80_certificate_url TEXT;

-- 3. School code for alumni self-registration (C1)
ALTER TABLE schools ADD COLUMN school_code VARCHAR(20);
CREATE UNIQUE INDEX idx_schools_code ON schools(school_code) WHERE school_code IS NOT NULL;
-- Backfill: generate codes for existing schools
UPDATE schools SET school_code = UPPER(LEFT(slug, 4)) || '-' || SUBSTRING(id::text, 1, 4) WHERE school_code IS NULL;

-- 4. NotificationPreferencesTable defaults for alumni (B3)
INSERT INTO notification_preferences (user_id, ...)
SELECT au.id, ...
FROM app_users au
LEFT JOIN notification_preferences np ON np.user_id = au.id
WHERE au.role = 'alumni' AND np.user_id IS NULL;
-- (Exact columns depend on NotificationPreferencesTable schema — verify before writing migration)
```

### 20.8 Pre-Implementation Checklist

Before writing any code, verify these items:

- [ ] **Verify `NotificationPreferencesTable` schema** — Read `Tables.kt` for exact column names and defaults. The migration backfill (B3) depends on this.
- [x] **`StudentsTable` has `studentCode` (text, uniqueIndex) AND `rollNumber` (text)** — Confirmed at `Tables.kt:533-537`. Self-registration auto-match (FR-4) should match against `StudentsTable.studentCode` (admission number) within the school. `EnrollmentsTable.rollNumber` (integer, nullable) is a secondary match field. **Note:** `StudentsTable.studentCode` is globally unique (`uniqueIndex()`), not per-school — verify this doesn't cause issues if multiple schools use the same admission number format.
- [ ] **Audit all enrollment status queries** — Run `grep -r "status" server/.../feature/school/ --include="*.kt"` and verify none use `status != 'withdrawn'` (which would include graduated students in active rosters). All must use `status = 'active'`.
- [x] **`JwtConfig.issueToken()` accepts arbitrary role strings** — Confirmed at `JwtConfig.kt:89-98`. It passes `role` directly to `.withClaim("role", role)` with no validation. `"alumni"` will work. No change needed.
- [ ] **Check if Ktor `RateLimit` plugin is already installed** — If not, B10's rate limiting solution needs the plugin added to `Application.kt`.
- [x] **`SupabaseStorage.upload()` accepts `kind = "PROFILE"`** — Confirmed in `MediaRouting.kt:76` (`VALID_KINDS` includes `"PROFILE"`). ✓
- [ ] **Check if `gradle/libs.versions.toml` has a PDF library** — If not, add OpenPDF dependency (B4).
- [x] **`EnrollmentsTable.endDate` exists** — Confirmed at `Tables.kt:578` (`val endDate = date("end_date").nullable()`). Graduation flow can set this. ✓
- [x] **`StudentsTable` has no `graduation_year` or `passing_year` column** — Confirmed. Graduation year is captured on the `alumni` table, not on `students`. The graduation flow must accept `graduation_year` as an admin input parameter, not derive it from the student record.
- [x] **`StudentsTable.studentCode` global unique index** — Keep existing `uniqueIndex()` untouched. No schema change. Alumni SIS matching queries with `WHERE school_id = ? AND student_code = ?`. The global uniqueness is stricter than needed but won't break anything — existing data is already compliant. If two schools somehow have the same code, the second school's import would have already failed at insert time, so the data is clean.

### 20.9 Design Decisions Resolved (User-Confirmed)

These decisions were confirmed by the product owner on 2026-06-27 and are now locked for implementation.

| # | Decision | Details | Impact on Spec |
|---|---|---|---|
| D1 | **School code: auto-generated + portal-visible** | `school_code` is auto-generated on school creation (format: `UPPER(LEFT(slug,4)) + '-' + random 4 chars`, e.g., `DPS-AB12`). Visible to admin in school settings/portal. Alumni can search by **school name OR school code** during self-registration — the endpoint accepts either field and does a case-insensitive `ILIKE` on `schools.name` or exact match on `schools.school_code`. | Update `POST /api/v1/alumni/register` to accept `{ schoolName?: String, schoolCode?: String, ... }` — at least one required. Add `GET /api/v1/alumni/schools/search?q=...` public endpoint returning `{ schoolId, name, schoolCode, city }` for autocomplete. |
| D2 | **Alumni login: both OTP and password** | Alumni can log in via **OTP (phone)** or **password (email)**, same as the existing dual-mode auth. The `/api/v1/auth/check-user` endpoint already returns whether the account uses phone or email. Alumni `app_users` rows store both `phone` (for OTP) and `email` + `passwordHash` (for password login). During registration, alumni provide both phone and email. | No new auth endpoints needed — existing `/send-otp`, `/verify-otp`, `/login` all work. `AlumniAccess.kt` validates the JWT after login. Document that alumni registration requires both phone and email. |
| D3 | **Mentorship: admin-configurable scope** | Admin can configure which classes/grades are eligible for mentorship requests. Default: Class 11-12 only. Admin settings: `MentorshipSettings` stored per school (either in `SchoolsTable` as JSON column or a new `alumni_mentorship_settings` table). Fields: `enabled: Boolean`, `eligibleClassIds: List<UUID>`, `maxMenteesPerAlumni: Int` (default 5), `requestApprovalRequired: Boolean` (default true). | Add `GET/PUT /api/v1/school/alumni/mentorship/settings` admin endpoints. Add `alumni_mentorship_settings` table to migration `052`. Update mentorship request flow to check `eligibleClassIds` before allowing student to request. |
| D4 | **Donations: manual logging only for Phase 1, payment gateway in later phase** | Phase 1 supports **admin manual entry** of donations (cheque, bank transfer, cash, UPI manual). Payment gateway integration (Razorpay/Cashfree) is scoped for a later phase. The `alumni_donations` table already has `payment_method`, `transaction_ref`, and `gateway_txn_id` (nullable) columns — these support manual entry now and gateway integration later. Receipt generation (80G) works for both manual and gateway-logged donations. | No change to DDL. Mark payment gateway as "Phase 2+" in roadmap. Phase 1 admin UI has a "Log Donation" form with fields: amount, donor (auto-filled from alumni record), payment method (dropdown: cheque/bank transfer/cash/UPI), transaction ref, date, campaign (optional). |
| D5 | **`StudentsTable.studentCode` unique index: keep as-is** | The existing global `uniqueIndex()` on `students.student_code` is kept unchanged. No schema modification. Alumni SIS matching uses `WHERE school_id = ? AND student_code = ?` — the global uniqueness is stricter but non-breaking. Existing data is already compliant. | No migration change. Document in `AlumniService.kt` that SIS match query includes `schoolId` filter for correctness, even though the global index would prevent duplicates anyway. |
| D6 | **Alumni app: same Compose app, role-based redirect** | Alumni use the **same Android/iOS Compose app**. After login, `NavGraphV2.kt` checks `role` claim from JWT and redirects to alumni dashboard if `role = "alumni"`. No separate app flavor or build variant. Alumni screens are new composables in `ui/v2/screens/alumni/`. | Add `"alumni"` role branch in `NavGraphV2.kt` deep link parser. Add alumni screens: `AlumniDashboardV2.kt`, `AlumniDirectoryScreen.kt`, `AlumniEventsScreen.kt`, `AlumniMentorshipScreen.kt`, `AlumniDonationsScreen.kt`, `AlumniProfileScreen.kt`. Add alumni bottom nav tabs in `AlumniPortalV2.kt` (similar to `SchoolPortalV2.kt` pattern). |
| D7 | **Admin access: both web admin and Compose app** | Alumni management is accessible from **both** the web admin dashboard (`website/src/app/admin/alumni/`) and the Compose app school admin screens (`ui/v2/screens/school/alumni/`). Both ship in Phase 1. Web admin is for desktop management (bulk import, analytics, campaign creation). Compose app is for on-the-go approval (verification queue, quick actions). | Add alumni section to `website/src/lib/admin/nav.ts`. Add web pages: `admin/alumni/page.tsx` (directory), `admin/alumni/verify/page.tsx` (review queue), `admin/alumni/campaigns/page.tsx`, `admin/alumni/donations/page.tsx`, `admin/alumni/analytics/page.tsx`. Add Compose screens: `SchoolAlumniScreen.kt`, `SchoolAlumniVerifyScreen.kt`, `SchoolAlumniCampaignsScreen.kt`. Both use the same `/api/v1/school/alumni/*` endpoints. |
