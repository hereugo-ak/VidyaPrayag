# NEP 2020 Compliance — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Audience:** Senior engineer / AI agent implementing the system
> **Last updated:** 2026-06-27
> **Prerequisites:** None
> **Unblocks:** `AI_REPORT_CARD_SPEC.md`, `CURRICULUM_TEMPLATES_SPEC.md`, `APAAR_ID_SPEC.md`

---

## Table of Contents

1. [Feature Overview](#1-feature-overview)
2. [Current System Assessment](#2-current-system-assessment)
3. [Gap Analysis](#3-gap-analysis)
4. [Functional Requirements](#4-functional-requirements)
5. [User Roles & Permissions](#5-user-roles--permissions)
6. [Database Design](#6-database-design)
7. [Backend Architecture](#7-backend-architecture)
8. [API Contracts](#8-api-contracts)
9. [Validation Rules](#9-validation-rules)
10. [Migration Strategy](#10-migration-strategy)
11. [Testing Strategy](#11-testing-strategy)
12. [Acceptance Criteria](#12-acceptance-criteria)
13. [Implementation Roadmap](#13-implementation-roadmap)
14. [File-Level Impact Analysis](#14-file-level-impact-analysis)
15. [Risks & Mitigations](#15-risks--mitigations)
16. [Future Extensibility](#16-future-extensibility)

---

## 1. Feature Overview

Implementation of NEP 2020 (National Education Policy) compliance features: Holistic Progress Card (HPC), UDISE+ reporting, board-specific report card templates, competency-based grading scales, and 360-degree assessment framework.

### Goals

- Support NEP 2020's Holistic Progress Card with 360-degree assessment (self, peer, teacher, parent)
- Board-specific report card templates (CBSE, ICSE, IB, State Boards)
- Competency-based grading scales (replacing marks-only with letter grades + descriptors)
- UDISE+ annual data submission report generation
- Multi-lingual report card support
- Co-scholastic assessment tracking (arts, sports, life skills)

---

## 2. Current System Assessment

### 2.1 What Exists

- **`AssessmentsTable`** + **`AssessmentMarksTable`** — typed marks model with `maxMarks`, `marks` (Double), `isAbsent`, `remark`, `status` (draft→published)
- **`ExamResultsTable`** (deprecated) — legacy string-scored marks model
- **`SchoolsTable`** has `board` field (CBSE/ICSE/IB/State) and `gradingSystem` field
- **`AcademicYearsTable`** — academic year management
- **`CurriculumUnitsTable`** + **`SyllabusProgressTable`** — curriculum tracking
- **`ParentAchievementsTable`** — badges, competencies, EI metrics, missions (partially aligns with HPC)
- Report card generation is a **stub** (feature_audit.csv L23: 20%, "Report Cards" stub)

### 2.2 What's Missing

- No HPC (Holistic Progress Card) data model
- No 360-degree assessment (self/peer/teacher/parent evaluation)
- No board-specific report card templates
- No competency-based grading scale configuration
- No UDISE+ report generation
- No co-scholastic assessment tracking
- No cognitive/non-cognitive domain tracking

---

## 3. Gap Analysis

| # | Gap | NEP Reference | Impact |
|---|---|---|---|
| G1 | No HPC data model | §4.34 | Non-compliant report cards |
| G2 | No 360-degree assessment | §4.34 | Missing holistic evaluation |
| G3 | No board templates | §4.28 | Cannot generate board-compliant cards |
| G4 | No competency grading | §4.33 | Marks-only, not competency-based |
| G5 | No UDISE+ reporting | MoE mandate | Non-compliance with annual data submission |
| G6 | No co-scholastic tracking | §4.27 | Missing arts/sports/life skills |
| G7 | No cognitive domain tracking | §4.30 | Missing NEP learning domains |

---

## 4. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Admin can configure grading scale per school (letter grades with descriptors + percentage ranges) |
| FR-2 | Support NEP 360-degree assessment: self-assessment, peer-assessment, teacher-assessment, parent-assessment |
| FR-3 | Track co-scholastic domains: arts, sports, life skills, values, attitudes |
| FR-4 | Track cognitive domains: language, numeracy, science, social awareness |
| FR-5 | Generate HPC report card per student per term with all assessment dimensions |
| FR-6 | Board-specific template rendering (CBSE, ICSE, IB, State Board formats) |
| FR-7 | UDISE+ annual data report generation (enrollment, infrastructure, teacher, outcome metrics) |
| FR-8 | Multi-lingual report card (in school's medium of instruction) |
| FR-9 | Competency mapping: link assessments to NEP-defined competencies |
| FR-10 | Term/semester-based progress tracking with trend analysis |

---

## 5. User Roles & Permissions

| Action | Parent | Teacher | School Admin | Super Admin |
|---|---|---|---|---|
| View child's HPC | ✅ | N/A | ✅ | ✅ |
| Enter teacher assessment | ❌ | ✅ | ✅ | ✅ |
| Enter self-assessment (student) | ❌ | ❌ | ❌ | ❌ |
| Enter parent assessment | ✅ | N/A | N/A | N/A |
| Configure grading scale | ❌ | ❌ | ✅ | ✅ |
| Generate report cards | ❌ | ❌ | ✅ | ✅ |
| Generate UDISE+ report | ❌ | ❌ | ✅ | ✅ |
| Configure board template | ❌ | ❌ | ❌ | ✅ |

---

## 6. Database Design

### 6.1 New Table: `grading_scales`

```sql
CREATE TABLE grading_scales (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    name            TEXT NOT NULL,                 -- "CBSE Secondary", "ICSE Primary"
    board           VARCHAR(16) NOT NULL,          -- CBSE | ICSE | IB | STATE
    grade_levels    TEXT NOT NULL,                 -- JSON: [{"grade":"A1","min":91,"max":100,"descriptor":"Outstanding"}, ...]
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);
```

### 6.2 New Table: `nep_competencies`

```sql
CREATE TABLE nep_competencies (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code            VARCHAR(32) NOT NULL UNIQUE,   -- "LANG.1", "NUM.3", "ARTS.2"
    domain          VARCHAR(32) NOT NULL,          -- cognitive | co_scholastic
    sub_domain      VARCHAR(32) NOT NULL,          -- language | numeracy | science | arts | sports | life_skills | values
    name            TEXT NOT NULL,
    description     TEXT,
    grade_level     VARCHAR(16),                   -- "Primary", "Upper Primary", "Secondary"
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
```

### 6.3 New Table: `holistic_assessments`

```sql
CREATE TABLE holistic_assessments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    student_id      UUID NOT NULL,                 -- FK students.id
    academic_year_id UUID NOT NULL REFERENCES academic_years(id),
    term            VARCHAR(16) NOT NULL,          -- term1 | term2 | term3 | annual
    assessment_type VARCHAR(16) NOT NULL,          -- self | peer | teacher | parent
    assessor_id     UUID,                          -- FK app_users.id (teacher/parent who assessed)
    competency_id   UUID REFERENCES nep_competencies(id),
    rating          VARCHAR(8) NOT NULL,           -- A | B | C | D | E (or numeric scale)
    evidence        TEXT,                          -- qualitative observation
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(school_id, student_id, academic_year_id, term, assessment_type, competency_id)
);
CREATE INDEX idx_holistic_student_term ON holistic_assessments(student_id, term);
```

### 6.4 New Table: `co_scholastic_records`

```sql
CREATE TABLE co_scholastic_records (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    student_id      UUID NOT NULL,
    academic_year_id UUID NOT NULL REFERENCES academic_years(id),
    term            VARCHAR(16) NOT NULL,
    domain          VARCHAR(32) NOT NULL,          -- arts | sports | life_skills | values | attitudes
    sub_domain      TEXT,                          -- "Music", "Football", "Leadership", "Honesty"
    grade           VARCHAR(8),                    -- A | B | C | D
    teacher_remark  TEXT,
    evidence_url    TEXT,                          -- photo/video/document URL
    assessed_by     UUID,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_cosch_student ON co_scholastic_records(student_id, academic_year_id, term);
```

### 6.5 New Table: `report_card_templates`

```sql
CREATE TABLE report_card_templates (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    board           VARCHAR(16) NOT NULL,          -- CBSE | ICSE | IB | STATE
    name            TEXT NOT NULL,                 -- "CBSE HPC Secondary 2026"
    template_config TEXT NOT NULL,                 -- JSON: sections, fields, layout, grading_scale_id
    language        VARCHAR(8) NOT NULL DEFAULT 'en',
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);
```

### 6.6 New Table: `report_cards`

```sql
CREATE TABLE report_cards (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    student_id      UUID NOT NULL,
    academic_year_id UUID NOT NULL REFERENCES academic_years(id),
    term            VARCHAR(16) NOT NULL,
    template_id     UUID REFERENCES report_card_templates(id),
    grading_scale_id UUID REFERENCES grading_scales(id),
    status          VARCHAR(16) NOT NULL DEFAULT 'draft', -- draft | generated | published | archived
    pdf_url         TEXT,                          -- Supabase Storage URL
    generated_by    UUID,
    published_at    TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(school_id, student_id, academic_year_id, term)
);
CREATE INDEX idx_report_cards_school_term ON report_cards(school_id, academic_year_id, term);
```

### 6.7 New Table: `udise_reports`

```sql
CREATE TABLE udise_reports (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    academic_year_id UUID NOT NULL REFERENCES academic_years(id),
    report_data     TEXT NOT NULL,                 -- JSON: all UDISE+ fields
    status          VARCHAR(16) NOT NULL DEFAULT 'draft', -- draft | submitted | archived
    submitted_at    TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(school_id, academic_year_id)
);
```

### 6.8 Modify Existing: `assessments`

```sql
ALTER TABLE assessments ADD COLUMN competency_id UUID;
ALTER TABLE assessments ADD COLUMN grading_scale_id UUID;
```

### 6.9 Exposed Mappings

```kotlin
object GradingScalesTable : UUIDTable("grading_scales", "id") {
    val schoolId    = uuid("school_id")
    val name        = text("name")
    val board       = varchar("board", 16)
    val gradeLevels = text("grade_levels")          // JSON
    val isActive    = bool("is_active").default(true)
    val createdAt   = timestamp("created_at")
    val updatedAt   = timestamp("updated_at")
}

object NepCompetenciesTable : UUIDTable("nep_competencies", "id") {
    val code        = varchar("code", 32).uniqueIndex()
    val domain      = varchar("domain", 32)
    val subDomain   = varchar("sub_domain", 32)
    val name        = text("name")
    val description = text("description").nullable()
    val gradeLevel  = varchar("grade_level", 16).nullable()
    val isActive    = bool("is_active").default(true)
    val createdAt   = timestamp("created_at")
}

object HolisticAssessmentsTable : UUIDTable("holistic_assessments", "id") {
    val schoolId       = uuid("school_id")
    val studentId      = uuid("student_id")
    val academicYearId = uuid("academic_year_id")
    val term           = varchar("term", 16)
    val assessmentType = varchar("assessment_type", 16)
    val assessorId     = uuid("assessor_id").nullable()
    val competencyId   = uuid("competency_id").nullable()
    val rating         = varchar("rating", 8)
    val evidence       = text("evidence").nullable()
    val createdAt      = timestamp("created_at")
    val updatedAt      = timestamp("updated_at")
    init {
        uniqueIndex("ux_holistic_unique", schoolId, studentId, academicYearId, term, assessmentType, competencyId)
        index("idx_holistic_student_term", false, studentId, term)
    }
}

object CoScholasticRecordsTable : UUIDTable("co_scholastic_records", "id") {
    val schoolId       = uuid("school_id")
    val studentId      = uuid("student_id")
    val academicYearId = uuid("academic_year_id")
    val term           = varchar("term", 16)
    val domain         = varchar("domain", 32)
    val subDomain      = text("sub_domain").nullable()
    val grade          = varchar("grade", 8).nullable()
    val teacherRemark  = text("teacher_remark").nullable()
    val evidenceUrl    = text("evidence_url").nullable()
    val assessedBy     = uuid("assessed_by").nullable()
    val createdAt      = timestamp("created_at")
    val updatedAt      = timestamp("updated_at")
    init { index("idx_cosch_student", false, studentId, academicYearId, term) }
}

object ReportCardTemplatesTable : UUIDTable("report_card_templates", "id") {
    val board          = varchar("board", 16)
    val name           = text("name")
    val templateConfig = text("template_config")    // JSON
    val language       = varchar("language", 8).default("en")
    val isActive       = bool("is_active").default(true)
    val createdAt      = timestamp("created_at")
    val updatedAt      = timestamp("updated_at")
}

object ReportCardsTable : UUIDTable("report_cards", "id") {
    val schoolId       = uuid("school_id")
    val studentId      = uuid("student_id")
    val academicYearId = uuid("academic_year_id")
    val term           = varchar("term", 16)
    val templateId     = uuid("template_id").nullable()
    val gradingScaleId = uuid("grading_scale_id").nullable()
    val status         = varchar("status", 16).default("draft")
    val pdfUrl         = text("pdf_url").nullable()
    val generatedBy    = uuid("generated_by").nullable()
    val publishedAt    = timestamp("published_at").nullable()
    val createdAt      = timestamp("created_at")
    val updatedAt      = timestamp("updated_at")
    init {
        uniqueIndex("ux_report_cards_unique", schoolId, studentId, academicYearId, term)
        index("idx_report_cards_school_term", false, schoolId, academicYearId, term)
    }
}

object UdiseReportsTable : UUIDTable("udise_reports", "id") {
    val schoolId       = uuid("school_id")
    val academicYearId = uuid("academic_year_id")
    val reportData     = text("report_data")        // JSON
    val status         = varchar("status", 16).default("draft")
    val submittedAt    = timestamp("submitted_at").nullable()
    val createdAt      = timestamp("created_at")
    val updatedAt      = timestamp("updated_at")
    init { uniqueIndex("ux_udise_unique", schoolId, academicYearId) }
}
```

---

## 7. Backend Architecture

### 7.1 Services

```kotlin
class GradingScaleService {
    suspend fun create(schoolId: UUID, request: CreateGradingScaleRequest): GradingScaleDto
    suspend fun getForSchool(schoolId: UUID): List<GradingScaleDto>
    suspend fun convertMarksToGrade(gradingScaleId: UUID, percentage: Double): GradeLevel
}

class HolisticAssessmentService {
    suspend fun enterAssessment(request: HolisticAssessmentRequest): HolisticAssessmentDto
    suspend fun getStudentAssessments(studentId: UUID, term: String): List<HolisticAssessmentDto>
    suspend fun get360View(studentId: UUID, term: String): HolisticProgress360Dto
}

class CoScholasticService {
    suspend fun enterRecord(request: CoScholasticRequest): CoScholasticDto
    suspend fun getStudentRecords(studentId: UUID, term: String): List<CoScholasticDto>
}

class ReportCardService {
    suspend fun generate(schoolId: UUID, studentId: UUID, term: String, templateId: UUID): ReportCardDto
    suspend fun publish(reportCardId: UUID): ReportCardDto
    suspend fun getForStudent(studentId: UUID): List<ReportCardDto>
    suspend fun bulkGenerate(schoolId: UUID, classId: UUID, term: String): UUID  // batch job
}

class UdiseReportService {
    suspend fun generateDraft(schoolId: UUID, academicYearId: UUID): UdiseReportDto
    suspend fun submit(reportId: UUID): UdiseReportDto
    suspend fun getForSchool(schoolId: UUID): List<UdiseReportDto>
}
```

### 7.2 Report Card Generation Flow

```
1. Admin selects class + term → POST /report-cards/generate
2. For each student:
   a. Fetch academic marks (AssessmentsTable + AssessmentMarksTable)
   b. Fetch holistic assessments (360-degree)
   c. Fetch co-scholastic records
   d. Apply grading scale conversion
   e. Render template (board-specific layout)
   f. Generate PDF → upload to Supabase Storage
   g. Create report_cards row (status='generated')
3. Admin reviews → publishes (status='published')
4. Parent notification sent with deep link to report card
```

### 7.3 UDISE+ Report Generation

Aggregates data from existing tables:
- **Enrollment:** `students` + `enrollments` (total, by gender, by class, by category)
- **Infrastructure:** `schools` fields (type, board, medium, gender)
- **Teachers:** `faculty` + `app_users` (count, by qualification)
- **Outcomes:** `assessments` + `attendance_records` (pass rate, attendance rate)
- **Facilities:** from `schools` extended fields (if available)

Output: JSON matching UDISE+ DCF (Data Capture Format) schema.

---

## 8. API Contracts

### 8.1 Grading Scale

```
GET /api/v1/school/grading-scales
POST /api/v1/school/grading-scales
PATCH /api/v1/school/grading-scales/{id}
```

### 8.2 Holistic Assessment

```
POST /api/v1/teacher/holistic-assessment
{
  "student_id": "uuid",
  "academic_year_id": "uuid",
  "term": "term1",
  "assessment_type": "teacher",
  "ratings": [
    {"competency_id": "uuid", "rating": "A", "evidence": "Excellent language skills"},
    {"competency_id": "uuid", "rating": "B", "evidence": "Good numeracy"}
  ]
}
```

```
GET /api/v1/parent/holistic-progress/{childId}?term=term1
```

**Response (360-degree view):**
```json
{
  "success": true,
  "data": {
    "student_name": "Aarav Sharma",
    "term": "term1",
    "cognitive": [
      {
        "competency": "Language & Literacy",
        "self": "A",
        "peer": "A",
        "teacher": "A",
        "parent": "A",
        "average": "A"
      }
    ],
    "co_scholastic": [
      {"domain": "Arts", "sub_domain": "Music", "grade": "A", "remark": "Participated in annual day"}
    ],
    "academic_summary": [
      {"subject": "Mathematics", "marks": 85, "grade": "A2", "max_marks": 100}
    ]
  }
}
```

### 8.3 Report Card Generation

```
POST /api/v1/school/report-cards/generate
{
  "class_id": "uuid",
  "term": "term1",
  "template_id": "uuid",
  "grading_scale_id": "uuid"
}
```

Returns a batch job ID. Admin can check status:

```
GET /api/v1/school/report-cards/generate/{jobId}/status
```

### 8.4 Report Card Access

```
GET /api/v1/parent/report-cards/{childId}
GET /api/v1/school/report-cards/{studentId}
GET /api/v1/school/report-cards/{id}/pdf
```

### 8.5 UDISE+ Report

```
GET /api/v1/school/udise-report?academic_year_id={uuid}
POST /api/v1/school/udise-report/generate?academic_year_id={uuid}
POST /api/v1/school/udise-report/{id}/submit
```

---

## 9. Validation Rules

| Field | Rule |
|---|---|
| grade_levels | JSON array, each item has grade, min, max, descriptor; min < max; ranges non-overlapping |
| rating | One of: A, B, C, D, E |
| term | One of: term1, term2, term3, annual |
| assessment_type | One of: self, peer, teacher, parent |
| domain (co-scholastic) | One of: arts, sports, life_skills, values, attitudes |
| board | One of: CBSE, ICSE, IB, STATE |

---

## 10. Migration Strategy

### 10.1 Migration File

`docs/db/migration_035_nep_compliance.sql`

Creates 7 new tables, adds columns to `assessments`, seeds NEP competencies.

### 10.2 Seed Data

Seed `nep_competencies` with NEP 2020 defined competencies:
- Cognitive: Language & Literacy (LANG.1-5), Numeracy & Math (NUM.1-5), Science (SCI.1-3), Social Awareness (SOC.1-3)
- Co-scholastic: Arts (ARTS.1-3), Sports (SPT.1-3), Life Skills (LS.1-5), Values (VAL.1-5)

Seed default grading scales for CBSE, ICSE, IB.

Seed default report card templates per board.

### 10.3 Rollback

Drop 7 new tables, remove added columns from `assessments`.

---

## 11. Testing Strategy

### 11.1 Unit Tests

- Grading scale conversion — marks → grade mapping correct
- 360-degree aggregation — average of self/peer/teacher/parent ratings
- UDISE+ data aggregation — correct counts from existing tables
- Report card uniqueness — duplicate generation returns existing

### 11.2 Integration Tests

- Enter holistic assessment → retrieve 360 view
- Generate report card → PDF created → publish → parent notified
- Bulk generate for class → all students have report cards
- UDISE+ report → data matches school's actual enrollment/teacher counts

---

## 12. Acceptance Criteria

- [ ] Admin can configure grading scales per board
- [ ] Teacher can enter 360-degree assessments per student
- [ ] Parent can enter parent-assessment for child
- [ ] HPC report card can be generated with all dimensions (academic + holistic + co-scholastic)
- [ ] Board-specific templates render correctly (CBSE, ICSE, IB, State)
- [ ] Report card PDF is generated and downloadable
- [ ] Bulk generation works for entire class
- [ ] UDISE+ report can be generated with data from existing tables
- [ ] NEP competencies are seeded and linkable to assessments

---

## 13. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 2 days | DB migration, Exposed tables, seed competencies + grading scales |
| 2 | 3 days | GradingScaleService + HolisticAssessmentService |
| 3 | 2 days | CoScholasticService |
| 4 | 3 days | ReportCardService (generation + PDF + bulk) |
| 5 | 2 days | Report card template system (board-specific rendering) |
| 6 | 3 days | UdiseReportService (data aggregation) |
| 7 | 3 days | Client UI (teacher assessment entry, admin generation, parent HPC view) |
| 8 | 2 days | Tests (unit + integration) |

---

## 14. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add + Modify | 7 new tables + columns on AssessmentsTable |
| `server/.../db/DatabaseFactory.kt` | Modify | Register new tables |
| `server/.../feature/nep/GradingScaleService.kt` | New | Grading scale management |
| `server/.../feature/nep/HolisticAssessmentService.kt` | New | 360-degree assessment |
| `server/.../feature/nep/CoScholasticService.kt` | New | Co-scholastic tracking |
| `server/.../feature/nep/ReportCardService.kt` | New | Report card generation |
| `server/.../feature/nep/ReportCardRenderer.kt` | New | Board-specific PDF rendering |
| `server/.../feature/nep/UdiseReportService.kt` | New | UDISE+ report generation |
| `server/.../feature/nep/NepRouting.kt` | New | All NEP API endpoints |
| `docs/db/migration_035_nep_compliance.sql` | New | DDL + seed data |
| `shared/.../feature/nep/NepApi.kt` | New | Client API |
| `composeApp/.../ui/v2/screens/teacher/HolisticAssessmentScreen.kt` | New | Teacher assessment entry |
| `composeApp/.../ui/v2/screens/parent/HolisticProgressScreen.kt` | New | Parent HPC view |
| `composeApp/.../ui/v2/screens/admin/ReportCardGenerationScreen.kt` | New | Admin generation UI |
| `composeApp/.../ui/v2/screens/admin/UdiseReportScreen.kt` | New | UDISE+ report UI |

---

## 15. Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| NEP competency definitions change | Medium | Medium | Competencies in DB (not hardcoded); admin can update |
| Board template format changes | Low | Medium | Template config in JSON; update without code change |
| PDF rendering library compatibility | Low | Medium | Use HTML-to-PDF approach (flexible, well-supported) |
| UDISE+ schema changes annually | Medium | Medium | Report data in JSON; schema version tracked |
| Bulk generation for large schools | Low | Medium | Batch job with progress tracking |

---

## 16. Future Extensibility

- **APAAR ID integration** — link report cards to APAAR (Automated Permanent Academic Account Registry) wallet
- **DIKSHA content alignment** — link curriculum units to DIKSHA resources
- **NIPUN Bharat** — foundational literacy and numeracy tracking for primary grades
- **Multi-board comparison** — analytics comparing performance across board formats
- **Parent-teacher conference integration** — HPC data feeds into PTM discussion points
