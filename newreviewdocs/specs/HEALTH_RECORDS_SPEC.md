# Student Health Records — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** None

---

## 1. Feature Overview

Student health and immunization record management: medical history, allergies, medications, immunization tracking, emergency contacts, and health incident logging.

### Goals

- Admin/nurse maintains student health profiles
- Track immunizations (vaccine, date, next due)
- Allergy and medical condition alerts (visible to teachers)
- Health incident logging (what happened, treatment, parent notified)
- Emergency contact and blood group
- Parent can view child's health records

---

## 2. Current System Assessment

- `feature_audit.csv` L120: Health Records missing (0%)
- `StudentsTable` may have `bloodGroup` field (needs verification)
- No health tables in `Tables.kt`

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Health profile per student: blood group, height, weight, allergies, chronic conditions, medications |
| FR-2 | Immunization tracking: vaccine name, date administered, next due date, booster schedule |
| FR-3 | Health incident log: date, description, treatment, medication given, parent notified (Y/N), staff who attended |
| FR-4 | Allergy alerts visible to teachers on student profile |
| FR-5 | Parent can view child's health records |
| FR-6 | Emergency contact + doctor information |
| FR-7 | Health checkup scheduling and results |

---

## 4. Database Design

```sql
CREATE TABLE student_health_profiles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    student_id      UUID NOT NULL UNIQUE,
    blood_group     VARCHAR(8),
    height_cm       REAL,
    weight_kg       REAL,
    allergies       TEXT,                          -- JSON array: ["Peanuts", "Penicillin"]
    chronic_conditions TEXT,                       -- JSON: [{"condition": "Asthma", "notes": "..."}]
    medications     TEXT,                          -- JSON: [{"name": "Inhaler", "dose": "...", "frequency": "..."}]
    emergency_contact_name TEXT,
    emergency_contact_phone VARCHAR(32),
    doctor_name     TEXT,
    doctor_phone    VARCHAR(32),
    updated_at      TIMESTAMP NOT NULL DEFAULT now(),
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE student_immunizations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id      UUID NOT NULL,
    vaccine_name    TEXT NOT NULL,                 -- "BCG", "DPT", "MMR", "COVID-19"
    dose_number    INTEGER NOT NULL DEFAULT 1,
    date_administered DATE NOT NULL,
    next_due_date   DATE,
    administered_by TEXT,                          -- doctor/clinic name
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_immunizations_student ON student_immunizations(student_id);

CREATE TABLE student_health_incidents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    student_id      UUID NOT NULL,
    date            DATE NOT NULL,
    time            VARCHAR(8),
    description     TEXT NOT NULL,                 -- "Fell during recess, scraped knee"
    treatment       TEXT,                          -- "Cleaned with antiseptic, bandage applied"
    medication_given TEXT,
    parent_notified BOOLEAN NOT NULL DEFAULT false,
    parent_notified_at TIMESTAMP,
    attended_by     UUID,                          -- staff who handled
    attended_by_name TEXT,
    severity        VARCHAR(16) NOT NULL DEFAULT 'minor', -- minor | moderate | major
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_health_incidents_student ON student_health_incidents(student_id, date DESC);
```

---

## 5. API Contracts

```
# Admin/Nurse
GET/POST /api/v1/school/health/profiles/{studentId}
POST /api/v1/school/health/immunizations
POST /api/v1/school/health/incidents
GET /api/v1/school/health/incidents?student_id={uuid}&date_from={}&date_to={}

# Teacher
GET /api/v1/teacher/health/alerts  -- allergy + condition alerts for assigned classes

# Parent
GET /api/v1/parent/health/{childId}
```

---

## 6. Acceptance Criteria

- [x] Health profile maintained per student
- [x] Immunization tracking with next-due alerts
- [x] Health incidents logged with treatment details
- [x] Allergy alerts visible to teachers
- [x] Parent can view child's health records
- [x] Emergency contact information stored

---

## 7. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 2 days | DB migration, Exposed tables, services |
| 2 | 2 days | API endpoints |
| 3 | 3 days | Client UI (health profile, immunization, incident log, teacher alerts) |
| 4 | 1 day | Tests |

---

## 8. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | 3 health tables |
| `server/.../feature/health/*.kt` | New | Services + routing |
| `docs/db/migration_050_health_records.sql` | New | DDL |
| `composeApp/.../ui/v2/screens/admin/HealthRecordsScreen.kt` | New | Health management |
| `composeApp/.../ui/v2/screens/parent/HealthScreen.kt` | New | Parent health view |
