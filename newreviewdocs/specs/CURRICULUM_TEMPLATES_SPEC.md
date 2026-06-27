# Curriculum Templates — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** None

---

## 1. Feature Overview

Pre-built curriculum templates aligned with board syllabi (CBSE, ICSE, IB, State) that schools can adopt and customize. Provides structured unit/lesson/topic hierarchy with learning outcomes and competency mapping.

### Goals

- Pre-built curriculum templates per board (CBSE, ICSE, IB, State) per grade per subject
- Admin imports template → auto-creates curriculum units in `CurriculumUnitsTable`
- Customizable after import (add/remove/modify units, topics)
- Learning outcomes per unit (linked to NEP competencies)
- Syllabus progress tracking against template
- Share templates across branches (multi-branch)

---

## 2. Current System Assessment

- `CurriculumUnitsTable` (`Tables.kt:870-885`) — `unitNumber`, `title`, `description`, `topics` (JSON), `estimatedHours`, `subjectId`, `classId`
- `SyllabusProgressTable` (`Tables.kt:890-905`) — tracks completion per unit per class
- `feature_audit.csv` L157: Curriculum Templates missing (0%)
- `NEP_COMPLIANCE_SPEC.md` defines `nep_competencies` table for competency mapping
- No template library — each school builds curriculum from scratch

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Pre-built templates: CBSE/ICSE/IB/State for grades 1-12, major subjects |
| FR-2 | Template structure: units → topics → learning outcomes → estimated hours → competency mapping |
| FR-3 | Admin browses template library, previews, and imports |
| FR-4 | Import auto-creates `CurriculumUnitsTable` rows for the school's class+subject |
| FR-5 | Customizable after import (admin can edit units, add topics, adjust hours) |
| FR-6 | Learning outcomes linked to NEP competencies |
| FR-7 | Template versioning (updated syllabus → new version) |
| FR-8 | Multi-branch: org admin can push template to all branches |

---

## 4. Database Design

```sql
CREATE TABLE curriculum_templates (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    board           VARCHAR(16) NOT NULL,          -- CBSE | ICSE | IB | STATE
    state           VARCHAR(32),                   -- for STATE board
    grade_level     VARCHAR(16) NOT NULL,          -- "Grade 1", "Grade 5", "Grade 10"
    subject_name    TEXT NOT NULL,
    version         VARCHAR(16) NOT NULL DEFAULT '1.0',
    units           TEXT NOT NULL,                 -- JSON: [{"unitNumber": 1, "title": "...", "topics": [...], "learningOutcomes": [...], "estimatedHours": 15, "competencyCodes": ["LANG.1"]}]
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(board, grade_level, subject_name, version)
);

CREATE TABLE curriculum_template_imports (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    template_id     UUID NOT NULL REFERENCES curriculum_templates(id),
    class_id        UUID NOT NULL,
    subject_id      UUID NOT NULL,
    imported_by     UUID,
    imported_at     TIMESTAMP NOT NULL DEFAULT now()
);
```

---

## 5. Backend Architecture

### 5.1 CurriculumTemplateService

```kotlin
class CurriculumTemplateService {
    suspend fun browseTemplates(board: String?, gradeLevel: String?, subject: String?): List<TemplateDto>
    suspend fun getTemplate(templateId: UUID): TemplateDetailDto
    suspend fun importTemplate(schoolId: UUID, templateId: UUID, classId: UUID, subjectId: UUID): ImportResult {
        // 1. Fetch template
        // 2. For each unit in template:
        //    a. Create CurriculumUnitsTable row (school_id, class_id, subject_id, unitNumber, title, description, topics, estimatedHours)
        //    b. Link competency codes to nep_competencies
        // 3. Create SyllabusProgressTable rows (0% for each unit)
        // 4. Record import in curriculum_template_imports
    }
    suspend fun pushToBranches(orgId: UUID, templateId: UUID, classSubjectMap: Map<UUID, UUID>)
}
```

---

## 6. API Contracts

```
# Browse and preview
GET /api/v1/school/curriculum-templates?board={board}&grade={grade}&subject={subject}
GET /api/v1/school/curriculum-templates/{id}

# Import
POST /api/v1/school/curriculum-templates/{id}/import  { class_id, subject_id }

# Manage imports
GET /api/v1/school/curriculum-templates/imports

# Super admin: manage templates
POST /api/v1/super/curriculum-templates
PATCH /api/v1/super/curriculum-templates/{id}
```

---

## 7. Seed Data

Seed templates for CBSE grades 1-10 for major subjects (Mathematics, Science, English, Hindi, Social Science, EVS). Each template includes 8-12 units with topics, learning outcomes, and competency codes.

---

## 8. Acceptance Criteria

- [ ] Template library browsable by board/grade/subject
- [ ] Template preview shows units, topics, learning outcomes
- [ ] Import creates curriculum units for school's class+subject
- [ ] Imported curriculum is customizable
- [ ] Learning outcomes linked to NEP competencies
- [ ] Template versioning supported
- [ ] Multi-branch push works

---

## 9. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration, Exposed tables |
| 2 | 2 days | CurriculumTemplateService (browse, import, customize) |
| 3 | 2 days | Seed CBSE templates (grades 1-10, 6 subjects) |
| 4 | 1 day | API endpoints |
| 5 | 2 days | Client UI (template browser, preview, import flow) |
| 6 | 1 day | Tests |

---

## 10. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | 2 curriculum template tables |
| `server/.../feature/curriculum/CurriculumTemplateService.kt` | New | Core service |
| `docs/db/migration_063_curriculum_templates.sql` | New | DDL + seed data |
| `composeApp/.../ui/v2/screens/admin/CurriculumTemplateBrowserScreen.kt` | New | Template browser |
