# Vidya Passport (Student Achievement Wallet) — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** None
> **Source:** `DIFFERENTIATING_FEATURES.md` §6.1
> **Template:** `_SPEC_TEMPLATE.md` v1 (25 mandatory + 6 optional sections)

---

## 1. Feature Overview

### What

A digital student achievement wallet that accumulates badges, certificates, competencies, and milestones throughout a student's academic journey. Portable across schools, shareable with parents, and aligned with NEP 2020's APAAR (Automated Permanent Academic Account Registry) vision.

### Why — Product Rationale

Students accumulate achievements throughout their school years — academic, sports, arts, leadership, behavioral — but these are scattered across report cards, certificates, and teacher memories. A digital passport consolidates all achievements in one place, portable across schools, and shareable with parents.

This is a **differentiating feature** (Priority P2, Phase 3, effort L, "Medium" value per `DIFFERENTIATING_FEATURES.md`). It aligns with NEP 2020's APAAR vision and provides a unique value proposition for parents.

### What Stands Out (Competitive Moat)

From `DIFFERENTIATING_FEATURES.md` §6.1:
> "Vidya Passport — achievement wallet, badges, certificates, competencies, milestones. Data readiness: ParentAchievementsTable exists with badges/competencies."

No major Indian school ERP offers a portable student achievement wallet. The key moat is **NEP-aligned** and **portable** — student can export passport when changing schools.

### Goals

- Accumulate achievements: academic, co-scholastic, extracurricular, behavioral
- Badge system with categories (academic, sports, arts, leadership, community)
- Certificate generation and storage
- Competency mastery tracking (linked to NEP competencies)
- Portable: student can export passport when changing schools
- Parent can view and share child's passport
- Milestone tracking (first 100% attendance, 10 assignments submitted, etc.)

### Non-goals

- [ ] Blockchain-based verification (future enhancement)
- [ ] Cross-school achievement network (future enhancement)
- [ ] Gamified leaderboard across students (privacy concerns)
- [ ] Achievement marketplace (future enhancement)
- [ ] Auto-share with external platforms (future APAAR integration)
- [ ] Student self-service badge requests (teacher/admin awards only)

### Dependencies

- `ParentAchievementsTable` (`Tables.kt:1350-1360`) — partial implementation with `badges`, `competencies`, `eiMetrics`, `missions` (JSON fields)
- `AssessmentMarksTable` — academic performance data
- `CoScholasticRecordsTable` (from `NEP_COMPLIANCE_SPEC.md`) — co-scholastic data
- `StudentsTable` — student info
- `AttendanceTable` — attendance data for auto-award criteria
- `HomeworkTable` + `HomeworkSubmissionsTable` — homework data for milestones

### Related Modules

- `server/.../feature/passport/` — new achievement/passport module
- `composeApp/.../ui/v2/screens/parent/` — passport UI screens
- `server/.../feature/nep/` — NEP compliance module (co-scholastic data)

---

## 2. Current System Assessment

### Existing Code

- `ParentAchievementsTable` (`Tables.kt:1350-1360`) — has `badges`, `competencies`, `eiMetrics`, `missions` (JSON fields) — partial implementation
- `AssessmentMarksTable` — academic performance data
- `CoScholasticRecordsTable` (from `NEP_COMPLIANCE_SPEC.md`) — co-scholastic data
- `DIFFERENTIATING_FEATURES.md` §6.1: Vidya Passport, effort L, data readiness: "ParentAchievementsTable exists with badges/competencies"

### Existing Database

- `ParentAchievementsTable` — has JSON fields for badges, competencies, eiMetrics, missions (partial, unstructured)
- `AssessmentMarksTable` — marks per assessment (for academic achievements)
- `CoScholasticRecordsTable` — co-scholastic records (for arts/sports achievements)
- `AttendanceTable` — attendance data (for attendance badges/milestones)
- `HomeworkTable` + `HomeworkSubmissionsTable` — homework data (for homework milestones)
- `StudentsTable` — student info
- No structured achievement tables exist

### Existing APIs

- Parent achievements API (partial — returns JSON blob from `ParentAchievementsTable`)
- No badge management API
- No certificate generation API
- No passport export API

### Existing UI

- Parent app has partial achievements display (JSON blob rendering)
- No badge collection view
- No certificate view
- No passport timeline

### Existing Services

- No achievement service exists
- No auto-award engine
- No milestone detection
- No certificate generator

### Existing Documentation

- `DIFFERENTIATING_FEATURES.md` §6.1 — Vidya Passport
- `NEP_COMPLIANCE_SPEC.md` — NEP compliance (co-scholastic data, competencies)

### Technical Debt

| # | Gap | Details |
|---|---|---|
| TD-1 | Unstructured achievements | `ParentAchievementsTable` stores badges/competencies as JSON blob — no queryable structure |
| TD-2 | No badge management | No `achievement_badges` table for predefined/custom badges |
| TD-3 | No achievement records | No `student_achievements` table for individual achievement records |
| TD-4 | No milestone tracking | No `student_milestones` table |
| TD-5 | No auto-award engine | No criteria-based automatic badge awarding |
| TD-6 | No certificate generation | No PDF certificate generator |
| TD-7 | No passport export | No PDF/JSON export functionality |
| TD-8 | No share functionality | No shareable achievement image generation |

### Gaps

| # | Gap | Impact | Severity |
|---|---|---|---|
| G1 | Unstructured achievements | Can't query, filter, or sort achievements | **High** |
| G2 | No auto-award | Teachers must manually award all badges — time-consuming | **High** |
| G3 | No milestones | Student milestones go unrecorded | **Medium** |
| G4 | No certificates | No digital certificates for achievements | **Medium** |
| G5 | No portability | Student can't export passport when changing schools | **High** |
| G6 | No share | Parents can't share child's achievements | **Medium** |

---

## 3. Functional Requirements

### FR-001
| Field | Value |
|---|---|
| **Title** | Achievement Types |
| **Description** | Achievement types: academic (marks, rank), co-scholastic (arts, sports), extracurricular (competitions, events), behavioral (attendance, discipline). |
| **Priority** | High |
| **User Roles** | Teacher, School Admin, Parent (view) |
| **Acceptance notes** | All achievement types supported. Categorized for filtering and display. |

### FR-002
| Field | Value |
|---|---|
| **Title** | Badge System |
| **Description** | Badge system: predefined badges (Perfect Attendance, Math Wizard, Sports Star, Reading Champion) + custom badges. |
| **Priority** | High |
| **User Roles** | School Admin (manage badges), Teacher (award badges) |
| **Acceptance notes** | Predefined badges seeded in DB. School admin can create custom badges. Badges have name, description, category, icon, color, criteria. |

### FR-003
| Field | Value |
|---|---|
| **Title** | Auto-Award Badges |
| **Description** | Auto-award badges based on criteria (e.g., 100% attendance for term → Perfect Attendance badge). |
| **Priority** | High |
| **User Roles** | System |
| **Acceptance notes** | Daily job checks criteria for all students. Auto-awards badges when criteria met. Criteria stored as JSON in `achievement_badges.criteria`. |

### FR-004
| Field | Value |
|---|---|
| **Title** | Certificate Generation |
| **Description** | Certificate generation: digital certificate PDF for achievements. |
| **Priority** | Medium |
| **User Roles** | Teacher, School Admin |
| **Acceptance notes** | PDF certificate generated with student name, achievement title, school name, date. Stored in Supabase Storage. URL saved in `evidence_url`. |

### FR-005
| Field | Value |
|---|---|
| **Title** | Competency Mastery |
| **Description** | Competency mastery: link to NEP competencies, show mastery progress. |
| **Priority** | Medium |
| **User Roles** | Parent (view), Teacher (view) |
| **Acceptance notes** | Competencies from NEP_COMPLIANCE_SPEC. Mastery level shown per competency. Linked to assessment and co-scholastic data. |

### FR-006
| Field | Value |
|---|---|
| **Title** | Milestone Tracking |
| **Description** | Milestone tracking: auto-detect and record milestones. |
| **Priority** | Medium |
| **User Roles** | System |
| **Acceptance notes** | Daily job checks milestone conditions. Auto-records milestones. UNIQUE(student_id, milestone_type) prevents duplicates. |

### FR-007
| Field | Value |
|---|---|
| **Title** | Passport Export |
| **Description** | Passport export: PDF or JSON (portable to another school). |
| **Priority** | High |
| **User Roles** | Parent |
| **Acceptance notes** | PDF: formatted passport with all achievements, badges, milestones, competencies. JSON: structured data for import by another school. |

### FR-008
| Field | Value |
|---|---|
| **Title** | Parent View |
| **Description** | Parent view: timeline of achievements, badge collection, competency map. |
| **Priority** | High |
| **User Roles** | Parent |
| **Acceptance notes** | Timeline: chronological list of achievements. Badge collection: grid of earned badges. Competency map: NEP competencies with mastery levels. |

### FR-009
| Field | Value |
|---|---|
| **Title** | Share Achievements |
| **Description** | Share: parent can share specific achievements via WhatsApp/image. |
| **Priority** | Medium |
| **User Roles** | Parent |
| **Acceptance notes** | Generates shareable image with achievement details (badge icon, title, student name, date). Shared via WhatsApp intent or saved to gallery. |

### Non-Functional Requirements

| ID | Requirement |
|---|---|
| NFR-1 | Passport view loads in < 2 seconds |
| NFR-2 | Certificate PDF generation: < 5 seconds |
| NFR-3 | Passport export PDF: < 10 seconds |
| NFR-4 | Auto-award job: < 5 minutes for 1,000 students |
| NFR-5 | Milestone detection job: < 5 minutes for 1,000 students |
| NFR-6 | Share image generation: < 3 seconds |
| NFR-7 | Achievement history paginated (50 per page) |

---

## 4. User Stories

### Parent
- [ ] View my child's achievement passport (timeline, badges, competencies, milestones)
- [ ] View my child's badge collection
- [ ] Export my child's passport as PDF
- [ ] Export my child's passport as JSON (for school transfer)
- [ ] Share my child's achievement via WhatsApp
- [ ] View my child's competency mastery map

### Teacher
- [ ] Award a predefined badge to a student
- [ ] Award a custom badge to a student
- [ ] View student achievements
- [ ] Generate certificate for an achievement

### School Admin
- [ ] Manage predefined badges (create, edit, deactivate)
- [ ] Create custom badges for school
- [ ] View achievement statistics across school
- [ ] Configure auto-award criteria

### System
- [ ] Auto-award badges based on criteria (daily job)
- [ ] Auto-detect and record milestones (daily job)
- [ ] Migrate existing `ParentAchievementsTable` data to new structured tables

---

## 5. Business Rules

### BR-001
**Rule:** `student_achievements` is canonical source.
**Enforcement:** New `student_achievements` table is the canonical source for all achievements. Existing `ParentAchievementsTable` kept for backward compatibility. Migration job copies existing data to new table.

### BR-002
**Rule:** Auto-award is idempotent.
**Enforcement:** Before awarding a badge, check if student already has it. If yes, skip. Prevents duplicate badges on job retries.

### BR-003
**Rule:** Milestones are unique per student per type.
**Enforcement:** `UNIQUE(student_id, milestone_type)` constraint on `student_milestones`. Prevents duplicate milestones.

### BR-004
**Rule:** Badges can be global or school-specific.
**Enforcement:** `achievement_badges.school_id` — null = global badge (available to all schools), non-null = school-specific badge. Global badges seeded by system.

### BR-005
**Rule:** Parent can only view own child's passport.
**Enforcement:** Parent endpoints require `ChildAccessResolver` (parent-child link). No cross-parent access.

### BR-006
**Rule:** Passport export is available to parents only.
**Enforcement:** Export endpoints require parent role + child access. Teachers and admins can view but not export (they have school-level access, not parent-level export).

### BR-007
**Rule:** Certificates are generated on-demand.
**Enforcement:** Certificate PDF generated when teacher/admin requests. Not auto-generated for all achievements. Stored in Supabase Storage with URL in `evidence_url`.

### BR-008
**Rule:** Competency mastery linked to NEP competencies.
**Enforcement:** Competency data from `NEP_COMPLIANCE_SPEC.md`. Mastery level derived from assessment marks and co-scholastic records. Not a separate competency table — uses existing NEP data.

---

## 6. Database Design

### 6.1 Entity Relationship Summary

Three new tables: `achievement_badges` (badge definitions), `student_achievements` (individual achievement records), `student_milestones` (milestone records). Existing `ParentAchievementsTable` kept for backward compatibility.

### 6.2 New Tables

#### `achievement_badges` table

```sql
CREATE TABLE achievement_badges (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID,                          -- null = global badge
    name            TEXT NOT NULL,
    description     TEXT NOT NULL,
    category        VARCHAR(32) NOT NULL,          -- academic | sports | arts | leadership | community | behavior
    icon_url        TEXT,                          -- badge icon image
    color           VARCHAR(8),                    -- hex color
    criteria        TEXT,                          -- JSON: {"type": "attendance", "threshold": 100, "period": "term"}
    is_auto_award   BOOLEAN NOT NULL DEFAULT false,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
```

#### `student_achievements` table

```sql
CREATE TABLE student_achievements (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    student_id      UUID NOT NULL,
    achievement_type VARCHAR(32) NOT NULL,         -- badge | certificate | milestone | competency
    badge_id        UUID REFERENCES achievement_badges(id),
    title           TEXT NOT NULL,
    description     TEXT,
    category        VARCHAR(32) NOT NULL,
    evidence_url    TEXT,                          -- certificate PDF, photo
    awarded_by      UUID,
    awarded_by_name TEXT,
    awarded_at      TIMESTAMP NOT NULL DEFAULT now(),
    is_auto_generated BOOLEAN NOT NULL DEFAULT false
);
CREATE INDEX idx_student_achievements_student ON student_achievements(student_id, awarded_at DESC);
CREATE INDEX idx_student_achievements_category ON student_achievements(student_id, category);
```

#### `student_milestones` table

```sql
CREATE TABLE student_milestones (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    student_id      UUID NOT NULL,
    milestone_type  VARCHAR(48) NOT NULL,          -- first_perfect_score | 100_attendance_days | 10_homework_streak | etc.
    milestone_value TEXT NOT NULL,                 -- description
    achieved_at     TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(student_id, milestone_type)
);
```

### 6.3 Modified Tables

#### `parent_achievements` (existing)

Keep existing `ParentAchievementsTable` for backward compatibility. New `student_achievements` table is the canonical source. Migration job copies existing badges to new table.

### 6.4 Indexes

- `student_achievements(student_id, awarded_at DESC)` — achievement history lookup
- `student_achievements(student_id, category)` — category-filtered lookup
- `student_milestones(student_id, milestone_type)` — UNIQUE, milestone lookup

### 6.5 Constraints

- `achievement_badges.school_id` — nullable (null = global badge)
- `achievement_badges.name` — NOT NULL
- `achievement_badges.category` — NOT NULL, VARCHAR(32)
- `student_achievements.school_id` — NOT NULL
- `student_achievements.student_id` — NOT NULL
- `student_achievements.achievement_type` — NOT NULL, VARCHAR(32)
- `student_achievements.title` — NOT NULL
- `student_achievements.category` — NOT NULL
- `student_milestones(student_id, milestone_type)` — UNIQUE

### 6.6 Foreign Keys

- `student_achievements.badge_id` → `achievement_badges.id`
- `student_achievements.school_id` → `schools.id` (implicit)
- `student_achievements.student_id` → `students.id` (implicit)
- `student_milestones.school_id` → `schools.id` (implicit)
- `student_milestones.student_id` → `students.id` (implicit)

### 6.7 Soft Delete Strategy

N/A — achievements are permanent records. No soft delete. Badges can be deactivated (`is_active = false`) but not deleted.

### 6.8 Audit Fields

- `achievement_badges.created_at` — when badge created
- `student_achievements.awarded_at` — when achievement awarded
- `student_achievements.awarded_by` — who awarded (UUID)
- `student_achievements.awarded_by_name` — name of awarder (denormalized)
- `student_achievements.is_auto_generated` — whether auto-awarded by system
- `student_milestones.achieved_at` — when milestone achieved

### 6.9 Migration Notes

Migration: `docs/db/migration_078_vidya_passport.sql`
- CREATE 3 tables: `achievement_badges`, `student_achievements`, `student_milestones`
- Seed predefined global badges (Perfect Attendance, Math Wizard, Sports Star, Reading Champion, Homework Hero, Most Improved)
- Data migration job: copy existing `ParentAchievementsTable` JSON badges to `student_achievements` table
- Indexes created in same migration

### 6.10 Exposed Mappings

```kotlin
object AchievementBadgesTable : UUIDTable("achievement_badges", "id") {
    val schoolId     = uuid("school_id").nullable()  // null = global
    val name         = text("name")
    val description  = text("description")
    val category     = varchar("category", 32)
    val iconUrl      = text("icon_url").nullable()
    val color        = varchar("color", 8).nullable()
    val criteria     = text("criteria").nullable()  // JSON
    val isAutoAward  = bool("is_auto_award").default(false)
    val isActive     = bool("is_active").default(true)
    val createdAt    = timestamp("created_at")
}

object StudentAchievementsTable : UUIDTable("student_achievements", "id") {
    val schoolId         = uuid("school_id")
    val studentId        = uuid("student_id")
    val achievementType  = varchar("achievement_type", 32)
    val badgeId          = uuid("badge_id").nullable()
    val title            = text("title")
    val description      = text("description").nullable()
    val category         = varchar("category", 32)
    val evidenceUrl      = text("evidence_url").nullable()
    val awardedBy        = uuid("awarded_by").nullable()
    val awardedByName    = text("awarded_by_name").nullable()
    val awardedAt        = timestamp("awarded_at")
    val isAutoGenerated  = bool("is_auto_generated").default(false)
}

object StudentMilestonesTable : UUIDTable("student_milestones", "id") {
    val schoolId      = uuid("school_id")
    val studentId     = uuid("student_id")
    val milestoneType = varchar("milestone_type", 48)
    val milestoneValue = text("milestone_value")
    val achievedAt    = timestamp("achieved_at")

    init {
        uniqueIndex("idx_student_milestones_unique", studentId, milestoneType)
    }
}
```

Register all 3 in `DatabaseFactory.allTables`.

### 6.11 Seed Data

Predefined global badges (school_id = null):

| Badge Name | Category | Criteria | Auto-Award |
|---|---|---|---|
| Perfect Attendance | behavior | `{"type": "attendance", "threshold": 100, "period": "term"}` | ✅ |
| Math Wizard | academic | `{"type": "subject_average", "threshold": 90, "subject": "Math", "period": "term"}` | ✅ |
| Sports Star | sports | `{"type": "co_scholastic", "category": "sports"}` | ❌ |
| Reading Champion | academic | `{"type": "library_books", "threshold": 20, "period": "year"}` | ✅ |
| Homework Hero | behavior | `{"type": "homework_submission", "threshold": 100, "period": "term"}` | ✅ |
| Most Improved | academic | `{"type": "improvement_trend", "period": "term"}` | ✅ |

---

## 7. State Machines

### Badge Award State Machine

```
not_awarded ──criteria_met──> auto_awarded ──recorded──> active
  │                              │
  │──teacher_awards──>           │
  ──> manually_awarded           │──already_awarded──> skipped
  ──> recorded ──> active        │
  │                              │
  └──criteria_not_met──>         └──criteria_not_met──>
  not_awarded                    not_awarded
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `not_awarded` | Auto-award criteria met | `auto_awarded` | Student doesn't already have badge |
| `not_awarded` | Teacher awards badge | `manually_awarded` | Teacher has permission |
| `not_awarded` | Criteria not met | `not_awarded` | Daily job, no action |
| `auto_awarded` | Record saved | `active` | Insert into `student_achievements` |
| `manually_awarded` | Record saved | `active` | Insert into `student_achievements` |
| `auto_awarded` | Already has badge | `skipped` | Idempotent check — skip |
| `active` | Badge deactivated | `inactive` | `is_active = false` on badge definition |

### Milestone Detection State Machine

```
not_achieved ──condition_met──> detected ──recorded──> achieved
  │                               │
  │                               │──already_achieved──> skipped
  │
  └──condition_not_met──> not_achieved
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `not_achieved` | Milestone condition met | `detected` | Daily job detects condition |
| `not_achieved` | Condition not met | `not_achieved` | Daily job, no action |
| `detected` | Record saved | `achieved` | Insert into `student_milestones` |
| `detected` | Already achieved | `skipped` | UNIQUE constraint — skip |
| `achieved` | N/A | `achieved` | Permanent — no state change |

### Certificate Generation State Machine

```
not_generated ──teacher_requests──> generating ──pdf_created──> generated ──stored──> available
  │                                  │
  │                                  │──generation_error──> error
  │
  └──already_generated──> available
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `not_generated` | Teacher requests certificate | `generating` | Achievement exists |
| `generating` | PDF created successfully | `generated` | PDF generation succeeds |
| `generating` | PDF generation fails | `error` | Error in PDF generation |
| `generated` | PDF stored in Supabase | `available` | URL saved in `evidence_url` |
| `error` | Teacher retries | `generating` | New attempt |
| `available` | Teacher requests again | `available` | Return existing URL (idempotent) |

### Passport Export State Machine

```
not_exported ──parent_requests──> exporting ──export_success──> exported
  │                                │
  │                                │──export_error──> error
  │
  └──already_exported──> exported (cached)
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `not_exported` | Parent requests export | `exporting` | Parent has child access |
| `exporting` | Export succeeds | `exported` | PDF or JSON generated |
| `exporting` | Export fails | `error` | Generation error |
| `error` | Parent retries | `exporting` | New attempt |
| `exported` | Parent downloads | `exported` | File served |

---

## 8. Backend Architecture

### 8.1 Component Overview

`AchievementService` handles badge awarding, certificate generation, passport retrieval, and export. `AutoAwardJob` runs daily to check criteria and auto-award badges. `MilestoneDetectionJob` runs daily to detect and record milestones. `CertificateGenerator` produces PDF certificates. `PassportExporter` generates PDF and JSON exports.

### 8.2 Design Principles

1. **Structured achievements** — queryable tables, not JSON blobs
2. **Auto-award first** — system auto-detects and awards where possible, manual for rest
3. **Idempotent operations** — auto-award and milestone detection skip existing records
4. **Portable** — passport export in PDF (human-readable) and JSON (machine-readable)
5. **NEP-aligned** — competency mastery linked to NEP 2020 competencies
6. **Backward compatible** — existing `ParentAchievementsTable` kept, data migrated

### 8.3 Core Types

#### AchievementService

```kotlin
class AchievementService {
    suspend fun awardBadge(studentId: UUID, badgeId: UUID, awardedBy: UUID): AchievementDto
    suspend fun awardCustomBadge(studentId: UUID, name: String, description: String, category: String, awardedBy: UUID): AchievementDto
    suspend fun generateCertificate(achievementId: UUID): String  // returns PDF URL
    suspend fun getPassport(studentId: UUID): PassportDto  // all achievements, badges, milestones, competencies
    suspend fun exportPassport(studentId: UUID, format: String): ByteArray  // PDF or JSON
    suspend fun checkAndAutoAward(): Int  // daily job: check criteria, auto-award badges
    suspend fun checkMilestones(): Int  // daily job: check milestone conditions
}
```

#### AutoAwardJob

```kotlin
class AutoAwardJob(private val achievementService: AchievementService) {
    suspend fun execute(): Int {
        // 1. Get all active auto-award badges
        // 2. For each badge, get criteria
        // 3. For each student, check if criteria met
        // 4. If met and not already awarded, award badge
        // 5. Return count of badges awarded
    }
}
```

### 8.4 Repositories

- `BadgeRepository` — CRUD for `achievement_badges`
- `AchievementRepository` — CRUD for `student_achievements`
- `MilestoneRepository` — CRUD for `student_milestones`

### 8.5 Mappers

- `BadgeMapper` — maps `achievement_badges` rows to `BadgeDto`
- `AchievementMapper` — maps `student_achievements` rows to `AchievementDto`
- `MilestoneMapper` — maps `student_milestones` rows to `MilestoneDto`

### 8.6 Permission Checks

- Parent endpoints: parent role + `ChildAccessResolver` (parent-child link)
- Teacher endpoints: teacher role + class assignment verification
- Admin endpoints: school admin role
- All endpoints: JWT auth via `requireAuth()`

### 8.7 Background Jobs

- **Auto-Award Job** — daily (2 AM IST)
  1. Get all active auto-award badges
  2. For each badge, evaluate criteria for all students
  3. Award badges to students who meet criteria (idempotent — skip if already awarded)
  4. Return count of badges awarded

- **Milestone Detection Job** — daily (2:30 AM IST)
  1. Check milestone conditions for all students
  2. Record new milestones (idempotent — UNIQUE constraint prevents duplicates)
  3. Return count of milestones recorded

### 8.8 Domain Events

- `BadgeAwarded` — emitted when badge awarded (manual or auto)
- `CustomBadgeAwarded` — emitted when custom badge created and awarded
- `CertificateGenerated` — emitted when certificate PDF generated
- `MilestoneAchieved` — emitted when milestone detected and recorded
- `PassportExported` — emitted when passport exported (PDF or JSON)
- `AchievementShared` — emitted when parent shares achievement

### 8.9 Caching

- Badge definitions: cached per school, 1-hour TTL (rarely change)
- Student passport: cached per student, 5-minute TTL
- Competency mastery: cached per student, 10-minute TTL (from NEP module)

### 8.10 Transactions

- Badge award: single transaction (insert achievement record)
- Auto-award: batch transaction (insert multiple achievements)
- Milestone recording: single transaction per milestone (with UNIQUE constraint)
- Certificate generation: no DB transaction (PDF generation + storage upload)
- Passport export: no DB transaction (read-only + file generation)

### 8.11 Rate Limiting

N/A — no rate limiting for achievement features. Auto-award and milestone jobs run once daily.

### 8.12 Configuration

- `PASSPORT_ENABLED` — default `true`; enable/disable feature
- `PASSPORT_AUTO_AWARD_ENABLED` — default `true`; enable/disable auto-award job
- `PASSPORT_MILESTONE_DETECTION_ENABLED` — default `true`; enable/disable milestone job
- `PASSPORT_CERTIFICATE_TEMPLATE` — default `default`; certificate PDF template
- `PASSPORT_EXPORT_PAGE_SIZE` — default `50`; achievements per page in export

### 8.13 Auto-Award Criteria Examples

| Badge | Criteria |
|---|---|
| Perfect Attendance | attendance_rate = 100% for term |
| Math Wizard | average marks ≥ 90% in Math for term |
| Sports Star | won sports competition or selected for school team |
| Reading Champion | read 20+ library books in academic year |
| Homework Hero | 100% homework submission rate for term |
| Most Improved | highest positive trend from term1 to term2 |

### 8.14 Milestone Detection

Daily job checks:
- First perfect score in any assessment
- 100 cumulative days of present attendance
- 10 consecutive homework submissions on time
- First position in class ranking
- 5 co-scholastic achievements in one term

---

## 9. API Contracts

### 9.1 Admin/Teacher Endpoints

```
GET /api/v1/school/achievements/badges
  → 200: { badges: [BadgeDto] }

POST /api/v1/school/achievements/badges
  Body: { name, description, category, icon_url, color, criteria, is_auto_award }
  → 201: BadgeDto

POST /api/v1/school/achievements/award
  Body: { student_id: "uuid", badge_id: "uuid" }
  → 201: AchievementDto

POST /api/v1/school/achievements/award-custom
  Body: { student_id: "uuid", name: "Best Speaker", description: "...", category: "leadership" }
  → 201: AchievementDto

GET /api/v1/school/achievements/{studentId}
  → 200: { achievements: [AchievementDto], milestones: [MilestoneDto] }

POST /api/v1/school/achievements/{achievementId}/certificate
  → 200: { certificate_url: "https://supabase.url/cert.pdf" }
```

### 9.2 Parent Endpoints

```
GET /api/v1/parent/passport/{childId}
  → 200: PassportDto {
    achievements: [AchievementDto],
    badges: [BadgeDto],
    milestones: [MilestoneDto],
    competencies: [CompetencyDto]
  }

GET /api/v1/parent/passport/{childId}/export?format=pdf|json
  → 200: binary (PDF) or JSON

POST /api/v1/parent/passport/{childId}/share
  Body: { achievement_id: "uuid" }
  → 200: { share_image_url: "https://supabase.url/share.webp" }
```

### 9.3 DTO Models

All `@Serializable`, wrapped in `ApiResponse<T>` pattern.

```kotlin
@Serializable data class BadgeDto(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val iconUrl: String?,
    val color: String?,
    val isAutoAward: Boolean,
)

@Serializable data class AchievementDto(
    val id: String,
    val achievementType: String,   // badge | certificate | milestone | competency
    val title: String,
    val description: String?,
    val category: String,
    val evidenceUrl: String?,
    val awardedByName: String?,
    val awardedAt: String,
    val isAutoGenerated: Boolean,
)

@Serializable data class MilestoneDto(
    val id: String,
    val milestoneType: String,
    val milestoneValue: String,
    val achievedAt: String,
)

@Serializable data class CompetencyDto(
    val competency: String,
    val masteryLevel: Double,      // 0.0 to 1.0
    val source: String,            // "assessment" | "co_scholastic"
)

@Serializable data class PassportDto(
    val achievements: List<AchievementDto>,
    val badges: List<BadgeDto>,
    val milestones: List<MilestoneDto>,
    val competencies: List<CompetencyDto>,
)

enum class AchievementCategory { ACADEMIC, SPORTS, ARTS, LEADERSHIP, COMMUNITY, BEHAVIOR }
enum class AchievementType { BADGE, CERTIFICATE, MILESTONE, COMPETENCY }
```

---

## 10. Frontend Architecture

### 10.1 Screens

| Screen | Platform | Role | Description |
|---|---|---|---|
| `PassportScreen` | Compose | Parent | Passport timeline (chronological achievements) |
| `BadgeCollectionScreen` | Compose | Parent | Badge collection grid |
| `CompetencyMapScreen` | Compose | Parent | NEP competency mastery map |
| `CertificateViewScreen` | Compose | Parent | Certificate PDF viewer |
| `BadgeManagementScreen` | Compose | Admin | Badge management (create, edit, deactivate) |
| `AwardBadgeScreen` | Compose | Teacher | Award badge to student |

### 10.2 Navigation

- Parent: Home tab → Passport → Timeline / Badges / Competencies
- Parent: Passport → Achievement Detail → Share
- Parent: Passport → Export (PDF/JSON)
- Teacher: Student Profile → Award Badge
- Admin: Settings → Badge Management

### 10.3 UX Flows

#### Parent: View Passport
1. Parent opens Passport
2. Sees timeline of achievements (chronological)
3. Tabs: Timeline | Badges | Competencies | Milestones
4. Tap achievement → detail view with certificate (if available)
5. Tap share → generates shareable image → WhatsApp intent

#### Parent: Export Passport
1. Parent opens Passport → Export
2. Selects format: PDF or JSON
3. System generates export
4. Parent downloads or shares

#### Teacher: Award Badge
1. Teacher opens student profile
2. Taps "Award Badge"
3. Selects from predefined badges or creates custom
4. If custom: enters name, description, category
5. Confirms award
6. Achievement recorded

### 10.4 State Management

```kotlin
data class PassportState(
    val achievements: List<AchievementDto>,
    val badges: List<BadgeDto>,
    val milestones: List<MilestoneDto>,
    val competencies: List<CompetencyDto>,
    val selectedTab: PassportTab,
    val isLoading: Boolean,
    val error: String?,
)

data class BadgeCollectionState(
    val badges: List<BadgeDto>,
    val earnedBadgeIds: Set<String>,
    val isLoading: Boolean,
)

data class AwardBadgeState(
    val selectedStudentId: String?,
    val selectedBadgeId: String?,
    val customBadgeName: String,
    val customBadgeDescription: String,
    val customBadgeCategory: String,
    val isAwarding: Boolean,
)

enum class PassportTab { TIMELINE, BADGES, COMPETENCIES, MILESTONES }
```

### 10.5 Offline Support

- Passport data: cached locally (last fetched)
- Badge definitions: cached locally (rarely change)
- Certificate PDFs: not cached (downloaded on-demand)
- Export: not available offline (requires server generation)

### 10.6 Loading States

- Passport: "Loading passport..."
- Badge collection: "Loading badges..."
- Certificate: "Generating certificate..."
- Export: "Exporting passport..."
- Share: "Creating shareable image..."

### 10.7 Error Handling (UI)

- No achievements: "No achievements yet. Keep up the good work!"
- Certificate generation failed: "Could not generate certificate. Please try again."
- Export failed: "Could not export passport. Please try again."
- Network error: "Connection issue. Please check your internet."

### 10.8 Component Integration Guidelines

| Rule | Description |
|---|---|
| **R1** | Timeline: chronological list with achievement icon, title, date, category color |
| **R2** | Badge collection: grid of badge icons (earned = full color, not earned = grayscale) |
| **R3** | Competency map: list of NEP competencies with progress bars (mastery level) |
| **R4** | Milestones: list with milestone icon, title, date achieved |
| **R5** | Achievement detail: full view with description, certificate (if available), share button |
| **R6** | Share: generates image with badge icon, title, student name, date → WhatsApp intent |
| **R7** | Export: dropdown for PDF/JSON format, download button |
| **R8** | Badge management (admin): list of badges with create/edit/deactivate actions |
| **R9** | Award badge (teacher): badge picker (predefined) or custom badge form |
| **R10** | Certificate view: PDF rendered in viewer with download button |

---

## 11. Shared Module Changes (KMP)

### 11.1 DTOs

All DTOs defined in section 9.3, placed in `shared/.../passport/domain/model/PassportModels.kt`.

### 11.2 Domain Models

```kotlin
data class Passport(
    val achievements: List<Achievement>,
    val badges: List<Badge>,
    val milestones: List<Milestone>,
    val competencies: List<Competency>,
)

data class Achievement(
    val id: String,
    val type: AchievementType,
    val title: String,
    val description: String?,
    val category: AchievementCategory,
    val evidenceUrl: String?,
    val awardedByName: String?,
    val awardedAt: Instant,
    val isAutoGenerated: Boolean,
)

data class Badge(
    val id: String,
    val name: String,
    val description: String,
    val category: AchievementCategory,
    val iconUrl: String?,
    val color: String?,
    val isAutoAward: Boolean,
)

data class Milestone(
    val id: String,
    val type: String,
    val value: String,
    val achievedAt: Instant,
)
```

### 11.3 Repository Interfaces

```kotlin
interface PassportRepository {
    suspend fun getPassport(token: String, childId: String): NetworkResult<PassportDto>
    suspend fun exportPassport(token: String, childId: String, format: String): NetworkResult<ByteArray>
    suspend fun shareAchievement(token: String, childId: String, achievementId: String): NetworkResult<String>
}

interface AchievementManagementRepository {
    suspend fun getBadges(token: String): NetworkResult<List<BadgeDto>>
    suspend fun createBadge(token: String, request: CreateBadgeRequest): NetworkResult<BadgeDto>
    suspend fun awardBadge(token: String, request: AwardBadgeRequest): NetworkResult<AchievementDto>
    suspend fun awardCustomBadge(token: String, request: AwardCustomBadgeRequest): NetworkResult<AchievementDto>
    suspend fun generateCertificate(token: String, achievementId: String): NetworkResult<String>
    suspend fun getStudentAchievements(token: String, studentId: String): NetworkResult<StudentAchievementsDto>
}
```

### 11.4 UseCases

- `GetPassportUseCase`
- `ExportPassportUseCase`
- `ShareAchievementUseCase`
- `GetBadgesUseCase`
- `CreateBadgeUseCase`
- `AwardBadgeUseCase`
- `AwardCustomBadgeUseCase`
- `GenerateCertificateUseCase`

### 11.5 Validation

- `student_id`: valid UUID, linked to parent (for parent endpoints)
- `badge_id`: valid UUID, exists in `achievement_badges`
- `name`: non-empty, max 100 characters (for custom badges)
- `description`: max 500 characters
- `category`: one of `academic`, `sports`, `arts`, `leadership`, `community`, `behavior`
- `format`: one of `pdf`, `json` (for export)
- `achievement_id`: valid UUID, exists in `student_achievements`

### 11.6 Serialization

Standard Kotlinx serialization. Enums serialized as lowercase strings.

### 11.7 Network APIs

Ktor `@Resource` route definitions in `PassportApi.kt`:
- GET `/api/v1/parent/passport/{childId}`
- GET `/api/v1/parent/passport/{childId}/export?format={pdf|json}`
- POST `/api/v1/parent/passport/{childId}/share`
- GET `/api/v1/school/achievements/badges`
- POST `/api/v1/school/achievements/badges`
- POST `/api/v1/school/achievements/award`
- POST `/api/v1/school/achievements/award-custom`
- GET `/api/v1/school/achievements/{studentId}`
- POST `/api/v1/school/achievements/{achievementId}/certificate`

### 11.8 Database Models (Local Cache)

- Passport data cached in local DB (last fetched, 5-minute TTL)
- Badge definitions cached in local DB (1-hour TTL)
- Certificate URLs cached in DataStore

---

## 12. Permissions Matrix

| Action | Super Admin | School Admin | Teacher | Parent |
|---|---|---|---|---|
| View own child's passport | N/A | N/A | N/A | ✅ |
| Export passport | N/A | N/A | N/A | ✅ |
| Share achievement | N/A | N/A | N/A | ✅ |
| View student achievements | ✅ | ✅ | ✅ | N/A |
| Award predefined badge | ✅ | ✅ | ✅ | ❌ |
| Award custom badge | ✅ | ✅ | ✅ | ❌ |
| Generate certificate | ✅ | ✅ | ✅ | ❌ |
| Manage badges (create/edit) | ✅ | ✅ | ❌ | ❌ |
| Deactivate badge | ✅ | ✅ | ❌ | ❌ |
| Configure auto-award criteria | ✅ | ✅ | ❌ | ❌ |

---

## 13. Notifications

### Achievement Notifications

| Trigger | Recipient | Channel | Template |
|---|---|---|---|
| Badge awarded (auto) | Parent | FCM + in-app | "Congratulations! {child} earned the {badge_name} badge!" |
| Badge awarded (manual) | Parent | FCM + in-app | "{teacher} awarded {child} the {badge_name} badge!" |
| Milestone achieved | Parent | FCM + in-app | "Milestone: {child} {milestone_value}!" |
| Certificate generated | Parent | FCM + in-app | "A certificate has been generated for {child}'s {achievement_title}." |

### Notification Integration

Uses existing `Notify.kt` dispatch. Notifications created with category "passport" and sent via FCM + in-app.

---

## 14. Background Jobs

### Auto-Award Job

| Property | Value |
|---|---|
| **Name** | `AutoAwardJob` |
| **Schedule** | Daily (2 AM IST) |
| **Duration** | < 5 minutes for 1,000 students |
| **Retry** | None (next day) |

#### Job Flow

1. Get all active auto-award badges
2. For each badge, get criteria (JSON)
3. For each student in school, evaluate criteria
4. If criteria met and student doesn't already have badge, award badge
5. Send notification to parent
6. Return count of badges awarded

### Milestone Detection Job

| Property | Value |
|---|---|
| **Name** | `MilestoneDetectionJob` |
| **Schedule** | Daily (2:30 AM IST) |
| **Duration** | < 5 minutes for 1,000 students |
| **Retry** | None (next day) |

#### Job Flow

1. Check milestone conditions for all students
2. Conditions: first perfect score, 100 attendance days, 10 homework streak, first in class, 5 co-scholastic in term
3. Record new milestones (UNIQUE constraint prevents duplicates)
4. Send notification to parent
5. Return count of milestones recorded

### Data Migration Job (one-time)

| Property | Value |
|---|---|
| **Name** | `PassportDataMigrationJob` |
| **Schedule** | One-time (post-deployment) |
| **Duration** | < 30 minutes |

#### Job Flow

1. Read existing `ParentAchievementsTable` records
2. Parse JSON badges/competencies
3. Insert into `student_achievements` table
4. Mark migration as complete
5. Return count of records migrated

---

## 15. Integrations

### Internal Integrations

| System | Integration Point | Direction | Protocol | Error Handling |
|---|---|---|---|---|
| `AssessmentMarksTable` | Academic performance | Read | Direct DB | Skip if no data |
| `AttendanceTable` | Attendance data | Read | Direct DB | Skip if no data |
| `HomeworkTable` + `HomeworkSubmissionsTable` | Homework data | Read | Direct DB | Skip if no data |
| `CoScholasticRecordsTable` | Co-scholastic data | Read | Direct DB | Skip if no data |
| `StudentsTable` | Student info | Read | Direct DB | Return error if not found |
| `ParentAchievementsTable` | Legacy data | Read (migration) | Direct DB | Log on failure |
| `Notify.kt` | Notifications | Call | Direct call | Log on failure |
| Supabase Storage | Certificate PDFs, share images | Upload/Read | HTTP API | Log on failure |

### External Integrations

N/A — no external integrations. All data is internal.

### Integration Patterns

- **Auto-award criteria:** Direct DB queries to `AssessmentMarksTable`, `AttendanceTable`, `HomeworkTable` to evaluate criteria
- **Competency mastery:** Read from `CoScholasticRecordsTable` and `AssessmentMarksTable` (via NEP module)
- **Certificate storage:** PDF uploaded to Supabase Storage, URL saved in `evidence_url`
- **Share image:** Generated image uploaded to Supabase Storage, URL returned to client
- **Notifications:** `Notify.kt` called with category "passport"

---

## 16. Security

### Authentication

- All passport APIs: JWT auth via `requireAuth()`
- Parent endpoints: parent role + `ChildAccessResolver`
- Teacher/Admin endpoints: respective role verification

### Authorization

- Parent can only view own child's passport
- Teacher can award badges to students in assigned classes
- School admin can manage badges and view all student achievements
- No cross-school achievement access

### Data Protection

- Achievement data: same sensitivity as marks data
- Certificates: contain student name, school name — stored in Supabase Storage (private bucket)
- Share images: stored in Supabase Storage (public bucket, expiring URLs)
- Passport export: available to parent only

### Input Validation

- `student_id`: valid UUID, linked to parent (for parent endpoints)
- `badge_id`: valid UUID, exists and active
- `name`: non-empty, max 100 characters (custom badges)
- `description`: max 500 characters
- `category`: one of allowed values
- `format`: one of `pdf`, `json`
- `achievement_id`: valid UUID, exists

### Rate Limiting

N/A — no rate limiting for achievement features.

### Audit Logging

- Badge awarded: awarder ID, student ID, badge ID, auto/manual
- Custom badge awarded: awarder ID, student ID, badge name, category
- Certificate generated: requester ID, achievement ID, PDF URL
- Passport exported: parent ID, student ID, format
- Achievement shared: parent ID, student ID, achievement ID
- Badge created/deactivated: admin ID, badge ID, action

### PII Handling

- Student name included in certificates and share images (necessary)
- No PII sent to external services
- Certificates in private Supabase bucket (authenticated access)
- Share images with expiring URLs (24-hour expiry)

### Multi-tenant Isolation

- `achievement_badges.school_id` — null = global, non-null = school-specific
- `student_achievements.school_id` — school-scoped
- `student_milestones.school_id` — school-scoped
- All queries filtered by `school_id`
- No cross-school achievement access

---

## 17. Performance & Scalability

### Expected Scale

- 1,000 students per school
- 10-50 achievements per student per year
- 10,000-50,000 achievement records per school per year
- Auto-award job: 1,000 students × 6 badges = 6,000 criteria checks per school per day

### Query Optimization

- Achievement history: `idx_student_achievements_student(student_id, awarded_at DESC)` — paginated
- Category filter: `idx_student_achievements_category(student_id, category)` — indexed
- Milestone lookup: `idx_student_milestones_unique(student_id, milestone_type)` — UNIQUE

### Indexing Strategy

- `student_achievements(student_id, awarded_at DESC)` — achievement history
- `student_achievements(student_id, category)` — category filter
- `student_milestones(student_id, milestone_type)` — UNIQUE, milestone lookup

### Caching Strategy

- Badge definitions: cached per school, 1-hour TTL
- Student passport: cached per student, 5-minute TTL
- Competency mastery: cached per student, 10-minute TTL

### Pagination

- Achievement history: 50 per page

### Connection Pooling

Uses existing HikariCP connection pool. No additional pooling needed.

### Async Processing

- Auto-award job: background job (async)
- Milestone detection: background job (async)
- Certificate generation: synchronous (with timeout)
- Passport export: synchronous (with timeout)
- Notifications: async (fire-and-forget via `Notify.kt`)

### Scalability Concerns

- Auto-award job: 6,000 criteria checks per school per day. Negligible.
- DB storage: 50,000 records/year × ~1KB = ~50MB/year. Negligible.
- Certificate PDFs: stored in Supabase Storage. ~100KB per certificate. Manageable.
- Passport export: on-demand. No scheduled load.

---

## 18. Edge Cases

| # | Scenario | Expected Behavior |
|---|---|---|
| EC-1 | Student already has badge when auto-award runs | Skip (idempotent). No duplicate. |
| EC-2 | Milestone already achieved when job runs | Skip (UNIQUE constraint). No duplicate. |
| EC-3 | Badge criteria can't be evaluated (missing data) | Skip student. Log warning. Continue with next. |
| EC-4 | Certificate generation fails | Return error. Teacher can retry. Achievement still recorded. |
| EC-5 | Passport export for student with no achievements | Return empty passport (valid PDF/JSON with no entries). |
| EC-6 | Parent exports passport for child no longer linked | Return 403 "Access denied." |
| EC-7 | Teacher awards badge to student not in their class | Return 403 "Not your student." |
| EC-8 | Custom badge with same name as predefined badge | Allowed. Custom badge is school-specific. |
| EC-9 | Badge deactivated after students earned it | Existing achievements remain. Badge no longer available for new awards. |
| EC-10 | Share image generation fails | Return error. Parent can retry. |
| EC-11 | Student transfers to another school | Parent exports passport as JSON. New school imports (future feature). |
| EC-12 | No co-scholastic data for competency map | Competency map shows academic competencies only. |
| EC-13 | Auto-award job takes too long | Log warning. Continue. Next day's job picks up where left off. |
| EC-14 | Concurrent badge awards for same student | Allow (different badges). UNIQUE not on student+badge, but idempotent check prevents duplicates. |
| EC-15 | Passport export PDF too large | Paginate or limit to last 200 achievements. |

---

## 19. Error Handling

### Error Response Format

Standard `ApiResponse` error format.

### Error Codes

| Code | HTTP Status | Description | User Message |
|---|---|---|---|
| `BADGE_NOT_FOUND` | 404 | Badge ID not found | "Badge not found." |
| `BADGE_INACTIVE` | 400 | Badge has been deactivated | "This badge is no longer available." |
| `ACHIEVEMENT_NOT_FOUND` | 404 | Achievement ID not found | "Achievement not found." |
| `CHILD_NOT_LINKED` | 403 | Parent doesn't have access to child | "You do not have access to this child." |
| `NOT_YOUR_STUDENT` | 403 | Teacher not assigned to student's class | "You do not have access to this student." |
| `CERTIFICATE_GENERATION_FAILED` | 500 | PDF generation failed | "Could not generate certificate. Please try again." |
| `EXPORT_FAILED` | 500 | Passport export failed | "Could not export passport. Please try again." |
| `SHARE_FAILED` | 500 | Share image generation failed | "Could not create shareable image. Please try again." |
| `INVALID_CATEGORY` | 400 | Category not valid | "Invalid category." |
| `INVALID_FORMAT` | 400 | Export format not valid | "Invalid format. Use pdf or json." |

### Error Handling Strategy

- **Certificate generation:** Return 500 on failure. Achievement still recorded. Teacher can retry.
- **Export:** Return 500 on failure. Parent can retry.
- **Auto-award:** Log error for individual student. Continue with next student. No 500 — it's a background job.
- **Milestone detection:** Same as auto-award — log and continue.

### Retry Strategy

- Certificate generation: client retries (3 attempts)
- Export: client retries (3 attempts)
- Auto-award job: no retry (next day's job handles it)
- Milestone detection: no retry (next day's job handles it)

### Fallback Behavior

- No achievements: empty passport (valid response)
- No co-scholastic data: competency map shows academic only
- Certificate generation fails: achievement recorded without certificate. Teacher can generate later.
- Badge criteria can't be evaluated: skip student, log warning

---

## 20. Analytics & Reporting

### Analytics Dashboard Data

| Metric | Source | Derivation |
|---|---|---|
| Total achievements per month | `student_achievements` | Count by month |
| Achievements by category | `student_achievements` | Count by category |
| Auto-awarded vs manual | `student_achievements` | Count by `is_auto_generated` |
| Most earned badges | `student_achievements` | Count by badge_id, sort desc |
| Milestones achieved per month | `student_milestones` | Count by month |
| Certificates generated | `student_achievements` | Count where `evidence_url IS NOT NULL` |
| Passport exports | Audit logs | Count of export requests |
| Share count | Audit logs | Count of share requests |

### Export Capabilities

- Achievement export (CSV) — student, type, title, category, date, auto/manual
- Badge statistics (CSV) — badge name, count awarded, auto/manual

### Report Types

| Report | Format | Frequency | Recipient |
|---|---|---|---|
| Achievement summary | JSON (API) | On-demand | School Admin |
| Badge statistics | JSON (API) | On-demand | School Admin |
| Passport adoption | JSON (API) | Monthly | Product Team |

---

## 21. Testing Strategy

### Unit Tests

- `AchievementService.awardBadge()` — badge award, validation
- `AchievementService.awardCustomBadge()` — custom badge creation and award
- `AchievementService.generateCertificate()` — PDF generation, storage upload
- `AchievementService.getPassport()` — passport assembly, data from multiple sources
- `AchievementService.exportPassport()` — PDF and JSON export
- `AutoAwardJob.execute()` — criteria evaluation, idempotent awarding
- `MilestoneDetectionJob.execute()` — milestone detection, UNIQUE constraint
- Criteria evaluation — attendance, marks, homework, co-scholastic criteria

### Integration Tests

- Full award flow: award badge → verify in student achievements → verify passport
- Auto-award flow: set up test data → run job → verify badges awarded
- Milestone flow: set up test data → run job → verify milestones recorded
- Certificate flow: award achievement → generate certificate → verify PDF URL
- Export flow: create achievements → export PDF → verify content
- Export flow: create achievements → export JSON → verify structure
- Share flow: create achievement → share → verify image URL
- Idempotency: run auto-award twice → verify no duplicates

### E2E Tests

- Parent views child's passport → sees timeline, badges, competencies, milestones
- Parent exports passport as PDF → downloads file
- Teacher awards badge to student → parent sees badge in passport
- Admin creates custom badge → teacher awards it → parent sees it

### Performance Tests

- Auto-award job: < 5 minutes for 1,000 students
- Milestone detection: < 5 minutes for 1,000 students
- Passport view: < 2 seconds
- Certificate generation: < 5 seconds
- Passport export PDF: < 10 seconds

### Test Data

- 10 students with varying achievements
- 6 predefined badges with criteria
- 5 milestone types
- Sample assessment marks, attendance, homework data
- Mock Supabase Storage (for certificates and share images)

### Test Environment

- Test database with achievement tables
- Mock Supabase Storage
- Test JWT tokens for parent, teacher, and admin roles
- Pre-seeded badge definitions

---

## 22. Acceptance Criteria

- [ ] Badges awarded manually by teacher/admin
- [ ] Auto-award badges based on criteria (daily job)
- [ ] Milestones auto-detected and recorded
- [ ] Certificate PDF generated for achievements
- [ ] Passport view shows timeline, badges, competencies, milestones
- [ ] Passport export as PDF and JSON
- [ ] Parent can share achievements
- [ ] Competency mastery linked to NEP competencies
- [ ] Idempotent auto-award (no duplicate badges)
- [ ] Idempotent milestone detection (no duplicate milestones)
- [ ] Existing `ParentAchievementsTable` data migrated to new tables
- [ ] Badge management (create, edit, deactivate) for admin

---

## 23. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 2 days | DB migration `migration_078_vidya_passport.sql`, Exposed tables, seed badges, register in `DatabaseFactory` |
| 2 | 3 days | `AchievementService` (award, auto-award, milestones, certificate, export) |
| 3 | 2 days | `AutoAwardJob` + `MilestoneDetectionJob` (criteria engine, daily jobs) |
| 4 | 1 day | `PassportExporter` (PDF + JSON export) |
| 5 | 1 day | API endpoints |
| 6 | 4 days | Client UI: `PassportScreen` (timeline), `BadgeCollectionScreen`, `CompetencyMapScreen`, `CertificateViewScreen`, `BadgeManagementScreen`, `AwardBadgeScreen` |
| 7 | 2 days | Tests: unit, integration, E2E |

### Pre-Implementation Checklist

- [ ] Verify `ParentAchievementsTable` structure and existing data
- [ ] Verify `AssessmentMarksTable` has test data
- [ ] Verify `AttendanceTable` has test data
- [ ] Verify `HomeworkTable` + `HomeworkSubmissionsTable` have test data
- [ ] Verify `CoScholasticRecordsTable` (from NEP_COMPLIANCE_SPEC) has test data
- [ ] Verify Supabase Storage for certificate and share image uploads
- [ ] Verify `Notify.kt` supports "passport" category
- [ ] Verify PDF generation library available

---

## 24. File-Level Impact Analysis

### Server

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | `AchievementBadgesTable`, `StudentAchievementsTable`, `StudentMilestonesTable` |
| `server/.../db/DatabaseFactory.kt` | Modify | Register 3 achievement tables in `allTables` |
| `server/.../feature/passport/AchievementService.kt` | **New** | Core service (award, auto-award, milestones, certificate, export) |
| `server/.../feature/passport/AutoAwardJob.kt` | **New** | Daily auto-award job |
| `server/.../feature/passport/MilestoneDetectionJob.kt` | **New** | Daily milestone detection job |
| `server/.../feature/passport/CertificateGenerator.kt` | **New** | Certificate PDF generator |
| `server/.../feature/passport/PassportExporter.kt` | **New** | Passport PDF/JSON exporter |
| `server/.../feature/passport/PassportRouting.kt` | **New** | API endpoints |
| `docs/db/migration_078_vidya_passport.sql` | **New** | DDL + seed badges |

### Shared (KMP)

| File | Change Type | Description |
|---|---|---|
| `shared/.../passport/domain/model/PassportModels.kt` | **New** | DTOs, domain models, enums |
| `shared/.../passport/domain/repository/PassportRepository.kt` | **New** | Repository interfaces |
| `shared/.../passport/data/remote/PassportApi.kt` | **New** | HTTP API definitions |

### Client (Compose)

| File | Change Type | Description |
|---|---|---|
| `composeApp/.../ui/v2/screens/parent/PassportScreen.kt` | **New** | Passport timeline |
| `composeApp/.../ui/v2/screens/parent/BadgeCollectionScreen.kt` | **New** | Badge collection grid |
| `composeApp/.../ui/v2/screens/parent/CompetencyMapScreen.kt` | **New** | NEP competency mastery map |
| `composeApp/.../ui/v2/screens/parent/CertificateViewScreen.kt` | **New** | Certificate PDF viewer |
| `composeApp/.../ui/v2/screens/teacher/AwardBadgeScreen.kt` | **New** | Award badge to student |
| `composeApp/.../ui/v2/screens/admin/BadgeManagementScreen.kt` | **New** | Badge management |

---

## 25. Future Enhancements

| # | Enhancement | Priority | Effort | Notes |
|---|---|---|---|---|
| F-1 | Blockchain-based verification | Low | L | Tamper-proof achievement verification |
| F-2 | Cross-school achievement network | Medium | L | Shared achievement network across schools |
| F-3 | APAAR integration | High | M | Direct integration with NEP 2020 APAAR registry |
| F-4 | Achievement marketplace | Low | L | Redeem achievements for rewards |
| F-5 | Student self-service badge requests | Low | S | Students can request badges from teachers |
| F-6 | Gamified leaderboard (opt-in) | Low | M | Optional leaderboard for competitive schools |
| F-7 | Achievement analytics for teachers | Medium | S | Which badges are most/least awarded |
| F-8 | Custom certificate templates | Medium | M | School-specific certificate designs |
| F-9 | Achievement import from other schools | Medium | M | Import passport JSON from previous school |
| F-10 | Achievement-based recommendations | Low | M | AI recommends activities based on achievement gaps |

---

## Appendix A: Sequence Diagrams

### A.1 Auto-Award Badge Flow

```
AutoAwardJob       Server              DB
  │                  │                  │
  │  execute()       │                  │
  │  ──────────────> │                  │
  │                  │──get active badges──────────────>│
  │                  │←──badge list with criteria──────│
  │                  │                  │
  │                  │──for each badge──│              │
  │                  │  for each student│              │
  │                  │──evaluate criteria─────────────>│
  │                  │  (attendance, marks, etc.)      │
  │                  │←──criteria met?────────────────│
  │                  │                  │
  │                  │──check if already awarded──────>│
  │                  │←──not awarded──────────────────│
  │                  │                  │
  │                  │──award badge──────────────────>│
  │                  │──send notification─────────────│
  │                  │←──success──────────────────────│
  │                  │                  │
  │  ←──count of badges awarded         │
  │                  │                  │
```

### A.2 Passport Export Flow

```
Parent (app)       Server              Supabase Storage
  │                  │                    │
  │  GET /passport/  │                    │
  │  {childId}/      │                    │
  │  export?format=  │                    │
  │  pdf             │                    │
  │  ──────────────> │                    │
  │                  │──verify child access
  │                  │──get achievements──│
  │                  │──get badges────────│
  │                  │──get milestones────│
  │                  │──get competencies──│
  │                  │──generate PDF──────│
  │                  │──upload to storage─→│
  │                  │←──storage URL──────│
  │  ←──200: PDF binary                   │
  │                  │                    │
  │  ──download/save──│                    │
  │                  │                    │
```

---

## Appendix B: Domain Model / ER Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                    achievement_badges (new)                           │
│  id (PK)                                                              │
│  school_id (nullable — null = global)                                 │
│  name, description, category                                          │
│  icon_url, color                                                      │
│  criteria (JSON: {type, threshold, period, ...})                      │
│  is_auto_award, is_active                                             │
│  created_at                                                           │
└──────────────────────────┬───────────────────────────────────────────┘
                           │
                           │  badge_id (FK)
                           ▼
┌──────────────────────────────────────────────────────────────────────┐
│                   student_achievements (new)                          │
│  id (PK)                                                              │
│  school_id, student_id                                                │
│  achievement_type (badge|certificate|milestone|competency)            │
│  badge_id (FK → achievement_badges)                                   │
│  title, description, category                                         │
│  evidence_url (certificate PDF URL)                                   │
│  awarded_by, awarded_by_name                                          │
│  awarded_at, is_auto_generated                                        │
│  INDEX: (student_id, awarded_at DESC)                                 │
│  INDEX: (student_id, category)                                        │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│                    student_milestones (new)                           │
│  id (PK)                                                              │
│  school_id, student_id                                                │
│  milestone_type, milestone_value                                      │
│  achieved_at                                                          │
│  UNIQUE: (student_id, milestone_type)                                 │
└──────────────────────────────────────────────────────────────────────┘

Existing tables used (read-only):
┌────────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│ parent_achievements │  │ assessment_marks │  │ co_scholastic    │
│ (existing, legacy)  │  │ (existing)       │  │ (existing)       │
└────────────────────┘  └──────────────────┘  └──────────────────┘
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│ attendance        │  │ homework +       │  │ students         │
│ (existing)        │  │ submissions      │  │ (existing)       │
└──────────────────┘  └──────────────────┘  └──────────────────┘
```

---

## Appendix C: Event Flow

### Domain Events

| Event | Emitter | Consumers | Payload | Side Effects |
|---|---|---|---|---|
| `BadgeAwarded` | `AchievementService.awardBadge()` / `AutoAwardJob` | `Notify.kt` | `studentId, badgeId, badgeName, isAuto` | Notification to parent |
| `CustomBadgeAwarded` | `AchievementService.awardCustomBadge()` | `Notify.kt` | `studentId, badgeName, category, awardedBy` | Notification to parent |
| `CertificateGenerated` | `AchievementService.generateCertificate()` | `Notify.kt` | `achievementId, studentId, certificateUrl` | Notification to parent |
| `MilestoneAchieved` | `MilestoneDetectionJob` | `Notify.kt` | `studentId, milestoneType, milestoneValue` | Notification to parent |
| `PassportExported` | `AchievementService.exportPassport()` | None (logged) | `studentId, format, parentId` | None |
| `AchievementShared` | `AchievementService.shareAchievement()` | None (logged) | `studentId, achievementId, parentId` | None |

### Event Delivery Guarantees

- Events emitted synchronously within service methods
- Notification dispatch is async (fire-and-forget via `Notify.kt`)
- Auto-award and milestone events emitted within job execution

---

## Appendix D: Configuration

### Environment Variables

| Variable | Default | Description |
|---|---|---|
| `PASSPORT_ENABLED` | `true` | Enable/disable passport feature |
| `PASSPORT_AUTO_AWARD_ENABLED` | `true` | Enable/disable auto-award job |
| `PASSPORT_MILESTONE_DETECTION_ENABLED` | `true` | Enable/disable milestone detection job |
| `PASSPORT_CERTIFICATE_TEMPLATE` | `default` | Certificate PDF template name |
| `PASSPORT_EXPORT_PAGE_SIZE` | `50` | Achievements per page in export |
| `PASSPORT_SHARE_IMAGE_EXPIRY_HOURS` | `24` | Share image URL expiry |

### Feature Flags

| Flag | Default | Description |
|---|---|---|
| `PASSPORT_ENABLED` | `true` | Enable/disable passport |
| `PASSPORT_AUTO_AWARD_ENABLED` | `true` | Enable/disable auto-award |
| `PASSPORT_MILESTONE_ENABLED` | `true` | Enable/disable milestone detection |
| `PASSPORT_CERTIFICATE_ENABLED` | `true` | Enable/disable certificate generation |
| `PASSPORT_EXPORT_ENABLED` | `true` | Enable/disable passport export |
| `PASSPORT_SHARE_ENABLED` | `true` | Enable/disable achievement sharing |

### School-Level Settings

| Setting | Default | Description |
|---|---|---|
| `passport_enabled` | `true` | Per-school enable/disable |
| `passport_auto_award_enabled` | `true` | Per-school auto-award toggle |
| `passport_custom_badges_enabled` | `true` | Allow school to create custom badges |

---

## Appendix E: Migration & Rollback

### Migration: `migration_078_vidya_passport.sql`

```sql
-- Migration 078: Vidya Passport
-- Creates achievement_badges, student_achievements, student_milestones tables
-- Seeds predefined global badges

BEGIN;

CREATE TABLE IF NOT EXISTS achievement_badges (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID,
    name            TEXT NOT NULL,
    description     TEXT NOT NULL,
    category        VARCHAR(32) NOT NULL,
    icon_url        TEXT,
    color           VARCHAR(8),
    criteria        TEXT,
    is_auto_award   BOOLEAN NOT NULL DEFAULT false,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS student_achievements (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    student_id      UUID NOT NULL,
    achievement_type VARCHAR(32) NOT NULL,
    badge_id        UUID REFERENCES achievement_badges(id),
    title           TEXT NOT NULL,
    description     TEXT,
    category        VARCHAR(32) NOT NULL,
    evidence_url    TEXT,
    awarded_by      UUID,
    awarded_by_name TEXT,
    awarded_at      TIMESTAMP NOT NULL DEFAULT now(),
    is_auto_generated BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_student_achievements_student
    ON student_achievements (student_id, awarded_at DESC);
CREATE INDEX IF NOT EXISTS idx_student_achievements_category
    ON student_achievements (student_id, category);

CREATE TABLE IF NOT EXISTS student_milestones (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    student_id      UUID NOT NULL,
    milestone_type  VARCHAR(48) NOT NULL,
    milestone_value TEXT NOT NULL,
    achieved_at     TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(student_id, milestone_type)
);

-- Seed predefined global badges (school_id = NULL)
INSERT INTO achievement_badges (school_id, name, description, category, criteria, is_auto_award, is_active)
VALUES
    (NULL, 'Perfect Attendance', '100% attendance for the term', 'behavior', '{"type":"attendance","threshold":100,"period":"term"}', true, true),
    (NULL, 'Math Wizard', 'Average ≥ 90% in Math for the term', 'academic', '{"type":"subject_average","threshold":90,"subject":"Math","period":"term"}', true, true),
    (NULL, 'Sports Star', 'Excellence in sports', 'sports', '{"type":"co_scholastic","category":"sports"}', false, true),
    (NULL, 'Reading Champion', 'Read 20+ books in the academic year', 'academic', '{"type":"library_books","threshold":20,"period":"year"}', true, true),
    (NULL, 'Homework Hero', '100% homework submission for the term', 'behavior', '{"type":"homework_submission","threshold":100,"period":"term"}', true, true),
    (NULL, 'Most Improved', 'Highest improvement from term 1 to term 2', 'academic', '{"type":"improvement_trend","period":"term"}', true, true)
ON CONFLICT DO NOTHING;

COMMIT;
```

### Rollback: `migration_078_rollback.sql`

```sql
BEGIN;
DROP TABLE IF EXISTS student_milestones;
DROP TABLE IF EXISTS student_achievements;
DROP TABLE IF EXISTS achievement_badges;
COMMIT;
```

### Migration Validation

- Verify 3 tables created with correct columns
- Verify `student_milestones(student_id, milestone_type)` UNIQUE constraint
- Verify `student_achievements.badge_id` foreign key to `achievement_badges`
- Verify 6 predefined global badges seeded (school_id = NULL)
- Verify indexes created
- Run `SELECT count(*) FROM achievement_badges WHERE school_id IS NULL` — should be 6

### Data Migration Job

One-time job to migrate existing `ParentAchievementsTable` data:
1. Read all records from `parent_achievements`
2. Parse JSON `badges` field
3. For each badge, insert into `student_achievements`
4. Log count of records migrated
5. Do NOT delete `parent_achievements` (backward compatibility)

---

## Appendix F: Observability

### Structured Logging

| Log Level | Event | Context Fields |
|---|---|---|
| INFO | Badge awarded (manual) | `studentId, badgeId, badgeName, awardedBy, category` |
| INFO | Badge awarded (auto) | `studentId, badgeId, badgeName, autoAward, category` |
| INFO | Custom badge awarded | `studentId, badgeName, category, awardedBy` |
| INFO | Certificate generated | `achievementId, studentId, certificateUrl` |
| INFO | Milestone achieved | `studentId, milestoneType, milestoneValue` |
| INFO | Passport exported | `studentId, format, parentId` |
| INFO | Achievement shared | `studentId, achievementId, parentId` |
| INFO | Badge created | `badgeId, name, category, schoolId, createdBy` |
| INFO | Badge deactivated | `badgeId, name, deactivatedBy` |
| WARN | Auto-award criteria evaluation failed | `studentId, badgeId, error` |
| WARN | Milestone detection failed | `studentId, milestoneType, error` |
| WARN | Certificate generation failed | `achievementId, studentId, error` |
| ERROR | Passport export failed | `studentId, format, error` |
| ERROR | Share image generation failed | `achievementId, studentId, error` |

### Metrics

| Metric | Type | Labels | Description |
|---|---|---|---|
| `passport_badges_awarded_total` | Counter | `school_id, category, auto` | Total badges awarded |
| `passport_milestones_total` | Counter | `school_id, milestone_type` | Total milestones achieved |
| `passport_certificates_generated` | Counter | `school_id` | Total certificates generated |
| `passport_exports_total` | Counter | `school_id, format` | Total passport exports |
| `passport_shares_total` | Counter | `school_id` | Total achievement shares |
| `passport_auto_award_duration` | Histogram | `school_id` | Auto-award job duration |
| `passport_milestone_detection_duration` | Histogram | `school_id` | Milestone detection job duration |
| `passport_view_duration` | Histogram | `school_id` | Passport view API latency |
| `passport_active_students` | Gauge | `school_id` | Students with at least 1 achievement |

### Health Checks

| Check | Endpoint | Description |
|---|---|---|
| Passport service | `/health/passport` | Verify service and DB accessible |
| Auto-award job | `/health/auto-award` | Verify last job run and status |

### Alerts

| Alert | Condition | Severity | Notification |
|---|---|---|---|
| Auto-award job failed | Job error rate > 10% | Warning | Email to dev team |
| Auto-award job slow | Duration > 10 minutes | Info | Email to dev team |
| Certificate generation failure rate | > 5% | Warning | Email to dev team |
| No badges awarded in 24h | Count = 0 | Info | Email to product team (check criteria) |

### Dashboards

| Dashboard | Panels | Audience |
|---|---|---|
| Passport Usage | Badges/day, milestones/day, exports/day, shares/day | Product Team |
| Auto-Award Performance | Job duration, badges awarded, error rate | Dev Team |
| Badge Statistics | Most earned, least earned, by category | School Admin |
| Certificate Generation | Count/day, failure rate | Dev Team |

### Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Auto-award awards wrong badges | Low | Medium | Criteria validation. Idempotent. Teacher can remove. |
| Milestone detection misses milestones | Medium | Low | Daily job. Missed milestones detected next day. |
| Certificate generation fails | Medium | Low | Achievement still recorded. Teacher can retry. |
| Passport export too large | Low | Low | Limit to last 200 achievements. Paginate. |
| Legacy data migration incomplete | Medium | Medium | Migration job logs count. Verify post-migration. |
| Badge inflation (too many badges) | Medium | Low | School admin controls badge creation. Auto-award criteria configurable. |
| Share image PII exposure | Low | Medium | Expiring URLs (24h). Private bucket for certificates. |
| DB storage growth | Low | Low | ~50MB/year. Negligible. |
