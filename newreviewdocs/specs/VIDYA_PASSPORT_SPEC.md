# Vidya Passport (Student Achievement Wallet) — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** None
> **Source:** `DIFFERENTIATING_FEATURES.md` §6.1

---

## 1. Feature Overview

A digital student achievement wallet that accumulates badges, certificates, competencies, and milestones throughout a student's academic journey. Portable across schools, shareable with parents, and aligned with NEP 2020's APAAR (Automated Permanent Academic Account Registry) vision.

### Goals

- Accumulate achievements: academic, co-scholastic, extracurricular, behavioral
- Badge system with categories (academic, sports, arts, leadership, community)
- Certificate generation and storage
- Competency mastery tracking (linked to NEP competencies)
- Portable: student can export passport when changing schools
- Parent can view and share child's passport
- Milestone tracking (first 100% attendance, 10 assignments submitted, etc.)

---

## 2. Current System Assessment

- `ParentAchievementsTable` (`Tables.kt:1350-1360`) — has `badges`, `competencies`, `eiMetrics`, `missions` (JSON fields) — partial implementation
- `AssessmentMarksTable` — academic performance data
- `CoScholasticRecordsTable` (from `NEP_COMPLIANCE_SPEC.md`) — co-scholastic data
- `DIFFERENTIATING_FEATURES.md` §6.1: Vidya Passport, effort L, data readiness: "ParentAchievementsTable exists with badges/competencies"

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Achievement types: academic (marks, rank), co-scholastic (arts, sports), extracurricular (competitions, events), behavioral (attendance, discipline) |
| FR-2 | Badge system: predefined badges (Perfect Attendance, Math Wizard, Sports Star, Reading Champion) + custom badges |
| FR-3 | Auto-award badges based on criteria (e.g., 100% attendance for term → Perfect Attendance badge) |
| FR-4 | Certificate generation: digital certificate PDF for achievements |
| FR-5 | Competency mastery: link to NEP competencies, show mastery progress |
| FR-6 | Milestone tracking: auto-detect and record milestones |
| FR-7 | Passport export: PDF or JSON (portable to another school) |
| FR-8 | Parent view: timeline of achievements, badge collection, competency map |
| FR-9 | Share: parent can share specific achievements via WhatsApp/image |

---

## 4. Database Design

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

### 4.1 Modify Existing: `parent_achievements`

Keep existing `ParentAchievementsTable` for backward compatibility. New `student_achievements` table is the canonical source. Migration job copies existing badges to new table.

---

## 5. Backend Architecture

### 5.1 AchievementService

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

### 5.2 Auto-Award Criteria Examples

| Badge | Criteria |
|---|---|
| Perfect Attendance | attendance_rate = 100% for term |
| Math Wizard | average marks ≥ 90% in Math for term |
| Sports Star | won sports competition or selected for school team |
| Reading Champion | read 20+ library books in academic year |
| Homework Hero | 100% homework submission rate for term |
| Most Improved | highest positive trend from term1 to term2 |

### 5.3 Milestone Detection

Daily job checks:
- First perfect score in any assessment
- 100 cumulative days of present attendance
- 10 consecutive homework submissions on time
- First position in class ranking
- 5 co-scholastic achievements in one term

---

## 6. API Contracts

```
# Admin/Teacher
GET/POST /api/v1/school/achievements/badges
POST /api/v1/school/achievements/award  { student_id, badge_id }
POST /api/v1/school/achievements/award-custom  { student_id, name, description, category }
GET /api/v1/school/achievements/{studentId}

# Parent
GET /api/v1/parent/passport/{childId}
GET /api/v1/parent/passport/{childId}/export?format=pdf|json
POST /api/v1/parent/passport/{childId}/share  { achievement_id }  -- generates shareable image
```

---

## 7. Acceptance Criteria

- [ ] Badges awarded manually by teacher/admin
- [ ] Auto-award badges based on criteria (daily job)
- [ ] Milestones auto-detected and recorded
- [ | Certificate PDF generated for achievements
- [ ] Passport view shows timeline, badges, competencies, milestones
- [ ] Passport export as PDF and JSON
- [ ] Parent can share achievements
- [ ] Competency mastery linked to NEP competencies

---

## 8. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 2 days | DB migration, Exposed tables, seed badges |
| 2 | 3 days | AchievementService (award, auto-award, milestones, certificate) |
| 3 | 2 days | Auto-award criteria engine + daily jobs |
| 4 | 1 day | Passport export (PDF + JSON) |
| 5 | 1 day | API endpoints |
| 6 | 4 days | Client UI (passport timeline, badge collection, certificate view, share) |
| 7 | 2 days | Tests |

---

## 9. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | 3 achievement tables |
| `server/.../feature/passport/AchievementService.kt` | New | Core service |
| `server/.../feature/passport/AutoAwardJob.kt` | New | Daily auto-award job |
| `server/.../feature/passport/CertificateGenerator.kt` | New | Certificate PDF |
| `docs/db/migration_078_vidya_passport.sql` | New | DDL + seed badges |
| `composeApp/.../ui/v2/screens/parent/PassportScreen.kt` | New | Passport timeline |
| `composeApp/.../ui/v2/screens/parent/BadgeCollectionScreen.kt` | New | Badge collection |
