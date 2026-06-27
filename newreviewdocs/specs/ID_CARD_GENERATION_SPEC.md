# ID Card Generation — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** None

---

## 1. Feature Overview

Generate printable ID cards for students, teachers, and non-teaching staff with school branding, photo, QR code, and emergency contact info.

### Goals

- Admin selects template (student/teacher/staff) and generates ID cards
- Card includes: photo, name, role, class/department, school name + logo, QR code (links to profile), emergency contact, blood group, valid till
- Bulk generation for entire class or all staff
- PDF export (printable, ID-card size: 54mm × 86mm)
- Digital ID card in app (parent/student can show on phone)

---

## 2. Current System Assessment

- `feature_audit.csv` Gap #11: ID Card missing (0%)
- `StudentsTable` has `photoUrl`, `bloodGroup` (if exists)
- `AppUsersTable` has `fullName`, `role`
- `SchoolsTable` has `name`, `logoUrl`
- `NonTeachingStaffTable` has `photoUrl`, `role`, `department`

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Admin selects class/department → bulk generate ID cards |
| FR-2 | Template: front (photo, name, role, class, school, logo) + back (QR, emergency contact, blood group, address, valid till) |
| FR-3 | QR code encodes deep link to profile verification |
| FR-4 | PDF export: 2 cards per A4 page (front + back) |
| FR-5 | Digital ID card in app (parent sees child's, teacher sees own) |
| FR-6 | Configurable: school colors, logo, fields shown |

---

## 4. Database Design

```sql
CREATE TABLE id_card_templates (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    name            TEXT NOT NULL,                 -- "Student Standard", "Staff Standard"
    role_type       VARCHAR(16) NOT NULL,          -- student | teacher | staff
    front_config    TEXT NOT NULL,                 -- JSON: layout, fields, colors
    back_config     TEXT NOT NULL,                 -- JSON: layout, fields, colors
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE id_cards (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    person_id       UUID NOT NULL,                 -- student/teacher/staff id
    person_type     VARCHAR(16) NOT NULL,          -- student | teacher | staff
    person_name     TEXT NOT NULL,
    template_id     UUID NOT NULL REFERENCES id_card_templates(id),
    pdf_url         TEXT,                          -- generated PDF URL
    digital_card_url TEXT,                         -- digital card image URL
    qr_code_data    TEXT NOT NULL,                 -- deep link encoded in QR
    valid_till      DATE,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
```

---

## 5. API Contracts

```
GET/POST /api/v1/school/id-cards/templates
POST /api/v1/school/id-cards/generate
{
  "template_id": "uuid",
  "scope": "class",  // class | all_students | all_staff
  "class_id": "uuid"
}
GET /api/v1/school/id-cards/{id}/pdf
GET /api/v1/parent/id-card/{childId}
GET /api/v1/teacher/id-card
```

---

## 6. Acceptance Criteria

- [ ] ID card template configurable per role
- [ ] Bulk generation for class or all staff
- [ ] PDF export printable (2 cards per A4)
- [ ] QR code links to profile verification
- [ ] Digital ID card viewable in app
- [ ] School branding (logo, colors) applied

---

## 7. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration, Exposed tables |
| 2 | 2 days | ID card renderer (front + back, QR code) |
| 3 | 1 day | PDF generation + Supabase upload |
| 4 | 1 day | API endpoints |
| 5 | 2 days | Client UI (template config, generation, digital card view) |
| 6 | 1 day | Tests |

---

## 8. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | 2 ID card tables |
| `server/.../feature/idcard/IdCardService.kt` | New | Card generation + QR |
| `server/.../feature/idcard/IdCardRouting.kt` | New | API endpoints |
| `docs/db/migration_049_id_card.sql` | New | DDL |
| `composeApp/.../ui/v2/screens/admin/IdCardScreen.kt` | New | Admin generation UI |
| `composeApp/.../ui/v2/screens/parent/DigitalIdCardScreen.kt` | New | Digital card display |
