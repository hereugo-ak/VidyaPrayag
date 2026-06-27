# Scholarship Workflow — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** None (extends existing scholarship tables)

---

## 1. Feature Overview

Complete scholarship management lifecycle: scholarship schemes, student applications, approval workflow, disbursement tracking, and renewal management.

### Goals

- Admin creates scholarship schemes (merit, need-based, category-based)
- Students/parents apply for scholarships with required documents
- Admin reviews applications, approves/rejects with remarks
- Approved scholarships linked to fee records (waiver/discount)
- Renewal tracking for multi-year scholarships

---

## 2. Current System Assessment

- `ScholarshipsTable` (`Tables.kt:1233-1250`) — exists with `name`, `description`, `amount`, `eligibilityCriteria`, `startDate`, `endDate`, `isActive`
- `ScholarshipApplicationsTable` (`Tables.kt:1255-1270`) — exists with `studentId`, `scholarshipId`, `status` (PENDING/APPROVED/REJECTED), `appliedAt`, `reviewedAt`, `reviewedBy`, `remarks`
- `feature_audit.csv` L138: Scholarship Applications partially implemented (40%)
- Missing: document upload, disbursement tracking, renewal, fee integration

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Admin creates scholarship schemes with eligibility criteria, amount, type (full/partial waiver, fixed amount) |
| FR-2 | Parent applies on behalf of student with documents (income proof, caste cert, mark sheet) |
| FR-3 | Admin reviews applications, approves/rejects with remarks |
| FR-4 | Approved scholarship auto-applies to fee records (waiver or discount) |
| FR-5 | Disbursement tracking: amount, date, reference number |
| FR-6 | Renewal: multi-year scholarships require annual renewal application |
| FR-7 | Notification to parent on approval/rejection |

---

## 4. Database Design

### 4.1 Modify Existing: `scholarships`

```sql
ALTER TABLE scholarships ADD COLUMN scholarship_type VARCHAR(16) NOT NULL DEFAULT 'fixed'; -- fixed | full_waiver | partial_waiver
ALTER TABLE scholarships ADD COLUMN waiver_percentage REAL;  -- for partial_waiver
ALTER TABLE scholarships ADD COLUMN is_renewable BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE scholarships ADD COLUMN renewal_period_months INTEGER; -- 12 for annual
```

### 4.2 Modify Existing: `scholarship_applications`

```sql
ALTER TABLE scholarship_applications ADD COLUMN document_urls TEXT; -- JSON array of Supabase URLs
ALTER TABLE scholarship_applications ADD COLUMN academic_year_id UUID;
ALTER TABLE scholarship_applications ADD COLUMN disbursement_amount DOUBLE PRECISION;
ALTER TABLE scholarship_applications ADD COLUMN disbursement_date TIMESTAMP;
ALTER TABLE scholarship_applications ADD COLUMN disbursement_reference TEXT;
ALTER TABLE scholarship_applications ADD COLUMN parent_application_text TEXT;
```

### 4.3 New Table: `scholarship_renewals`

```sql
CREATE TABLE scholarship_renewals (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    original_application_id UUID NOT NULL REFERENCES scholarship_applications(id),
    student_id      UUID NOT NULL,
    scholarship_id  UUID NOT NULL REFERENCES scholarships(id),
    academic_year_id UUID NOT NULL,
    status          VARCHAR(16) NOT NULL DEFAULT 'pending',
    applied_at      TIMESTAMP NOT NULL DEFAULT now(),
    reviewed_at     TIMESTAMP,
    reviewed_by     UUID,
    remarks         TEXT
);
```

---

## 5. API Contracts

```
# Admin
GET/POST /api/v1/school/scholarships
GET /api/v1/school/scholarship-applications?status={status}
POST /api/v1/school/scholarship-applications/{id}/approve  { disbursement_amount, remarks }
POST /api/v1/school/scholarship-applications/{id}/reject  { remarks }
POST /api/v1/school/scholarship-applications/{id}/disburse  { amount, reference }

# Parent
GET /api/v1/parent/scholarships  -- available scholarships
POST /api/v1/parent/scholarships/apply  { scholarship_id, child_id, documents, application_text }
GET /api/v1/parent/scholarships/applications  -- child's applications
```

---

## 6. Acceptance Criteria

- [ ] Admin creates scholarship schemes with type and eligibility
- [ ] Parent applies with document upload
- [ ] Admin reviews and approves/rejects
- [ ] Approved scholarship auto-applies fee waiver/discount
- [ ] Disbursement tracked with amount and reference
- [ ] Renewable scholarships support annual renewal
- [ ] Parent notified on approval/rejection

---

## 7. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration, modify existing tables |
| 2 | 2 days | ScholarshipService (application workflow, approval) |
| 3 | 1 day | Fee integration (auto-apply waiver) |
| 4 | 1 day | Renewal service |
| 5 | 2 days | API endpoints |
| 6 | 2 days | Client UI (scheme list, application form, admin review queue) |
| 7 | 1 day | Tests |

---

## 8. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Modify + Add | Columns on scholarships/applications + `ScholarshipRenewalsTable` |
| `server/.../feature/scholarship/ScholarshipService.kt` | New | Core service |
| `docs/db/migration_060_scholarship_workflow.sql` | New | DDL |
| `composeApp/.../ui/v2/screens/admin/ScholarshipReviewScreen.kt` | New | Admin review |
| `composeApp/.../ui/v2/screens/parent/ScholarshipApplyScreen.kt` | New | Parent application |
