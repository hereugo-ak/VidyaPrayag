# Multi-Branch / School Chain Support — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** None
> **Unblocks:** `MULTI_SCHOOL_MANAGEMENT_SPEC.md`, `SCHOOL_BENCHMARKING_SPEC.md`

---

## 1. Feature Overview

Support for school chains/franchises: a single admin managing multiple school branches under one organization, with shared configuration, cross-branch reporting, and branch-level autonomy.

### Goals

- Super admin creates an organization (school chain) with multiple branches
- Each branch is a `schools` row linked to a parent organization
- Org-level admin can view all branches, aggregate reports, shared settings
- Branch-level admin manages only their branch
- Cross-branch student transfer
- Aggregate analytics across branches

---

## 2. Current System Assessment

- `SchoolsTable` — single school per admin, no parent organization concept
- `feature_audit.csv` L156: "Currently single-school per admin"
- `AppUsersTable` — `schoolId` links user to one school
- All data is school-scoped via `school_id`

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Create organization (chain) with multiple branch schools |
| FR-2 | Org admin role: view all branches, aggregate dashboard, shared config |
| FR-3 | Branch admin: manages own branch only (existing behavior) |
| FR-4 | Cross-branch student transfer (with academic record portability) |
| FR-5 | Aggregate reports: total students, staff, revenue across all branches |
| FR-6 | Shared settings: fee categories, grading scales, templates can be shared from org to branches |
| FR-7 | Branch comparison analytics |

---

## 4. Database Design

### 4.1 New Table: `school_organizations`

```sql
CREATE TABLE school_organizations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            TEXT NOT NULL,                 -- "Delhi Public School Society"
    description     TEXT,
    logo_url        TEXT,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);
```

### 4.2 Modify Existing: `schools`

```sql
ALTER TABLE schools ADD COLUMN organization_id UUID REFERENCES school_organizations(id);
ALTER TABLE schools ADD COLUMN branch_name TEXT;  -- "DPS RK Puram", "DPS Vasant Kunj"
```

### 4.3 Modify Existing: `app_users`

```sql
ALTER TABLE app_users ADD COLUMN organization_id UUID;
ALTER TABLE app_users ADD COLUMN org_admin_role VARCHAR(16); -- org_admin | null
```

### 4.4 New Table: `student_transfers`

```sql
CREATE TABLE student_transfers (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id      UUID NOT NULL,
    from_school_id  UUID NOT NULL,
    to_school_id    UUID NOT NULL,
    transfer_date   DATE NOT NULL,
    reason          TEXT,
    status          VARCHAR(16) NOT NULL DEFAULT 'pending', -- pending | approved | completed | rejected
    approved_by     UUID,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
```

---

## 5. Backend Architecture

### 5.1 OrganizationService

```kotlin
class OrganizationService {
    suspend fun createOrg(name: String, logoUrl: String?): OrgDto
    suspend fun addBranch(orgId: UUID, schoolId: UUID, branchName: String)
    suspend fun getBranches(orgId: UUID): List<SchoolDto>
    suspend fun getAggregateDashboard(orgId: UUID): OrgDashboardDto  // total students, staff, revenue across branches
    suspend fun transferStudent(studentId: UUID, fromSchoolId: UUID, toSchoolId: UUID): UUID
}
```

### 5.2 Auth Modification

JWT claims extended:
- `organization_id` — if user is org admin
- `org_admin_role` — "org_admin" if org-level access
- Existing `school_id` — for branch-level access

### 5.3 Query Scoping

- Branch admin: queries filtered by `school_id` (existing behavior, unchanged)
- Org admin: queries filtered by `organization_id` → all branches' data

---

## 6. API Contracts

```
# Super Admin
POST /api/v1/super/organizations
POST /api/v1/super/organizations/{id}/branches  { school_id, branch_name }

# Org Admin
GET /api/v1/org/dashboard
GET /api/v1/org/branches
GET /api/v1/org/students?branch_id={uuid}
GET /api/v1/org/reports/aggregate
POST /api/v1/org/transfer-student  { student_id, to_school_id }
```

---

## 7. Acceptance Criteria

- [ ] Organization with multiple branches created
- [ ] Org admin sees aggregate dashboard across branches
- [ ] Branch admin sees only own branch (no change)
- [ ] Student transfer between branches works
- [ ] Shared settings propagate to branches
- [ ] Branch comparison analytics available

---

## 8. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 2 days | DB migration, Exposed tables, modify schools + app_users |
| 2 | 3 days | OrganizationService + aggregate queries |
| 3 | 2 days | Student transfer service |
| 4 | 2 days | Auth modification (org admin JWT claims) |
| 5 | 2 days | API endpoints |
| 6 | 3 days | Client UI (org dashboard, branch list, transfer flow) |
| 7 | 2 days | Tests |

---

## 9. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add + Modify | `SchoolOrganizationsTable`, `StudentTransfersTable` + columns on schools, app_users |
| `server/.../feature/org/OrganizationService.kt` | New | Org management |
| `server/.../feature/org/StudentTransferService.kt` | New | Transfer workflow |
| `server/.../core/JwtConfig.kt` | Modify | Add org claims |
| `server/.../feature/auth/AuthService.kt` | Modify | Org admin auth |
| `docs/db/migration_051_multi_branch.sql` | New | DDL |
| `composeApp/.../ui/v2/screens/admin/OrgDashboardScreen.kt` | New | Org admin dashboard |
