# Transparency Dashboard — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** `EXPENSE_MANAGEMENT_SPEC.md`, `FEE_PAYMENT_SPEC.md`
> **Source:** `DIFFERENTIATING_FEATURES.md` §9.2

---

## 1. Feature Overview

Public-facing transparency dashboard showing school financial health, fee utilization, infrastructure investments, and staff distribution. Builds trust with parents by showing where their fee money goes.

### Goals

- Fee collection vs expense breakdown (sankey-style visualization)
- Category-wise expense distribution (salary, infrastructure, transport, etc.)
- Fee utilization per student (avg fee collected / avg expense per student)
- Infrastructure investments over time
- Staff distribution (teaching vs non-teaching, qualification breakdown)
- Admin-configurable: choose what to show publicly
- Updated monthly

---

## 2. Current System Assessment

- `EXPENSE_MANAGEMENT_SPEC.md` — expense data by category
- `FEE_PAYMENT_SPEC.md` — fee collection data
- `SchoolsTable` — school info, board, medium
- `NonTeachingStaffTable` + `AppUsersTable` (role=teacher) — staff data
- `DIFFERENTIATING_FEATURES.md` §9.2: Transparency Dashboard, effort M, data readiness: "Fee data exists, expense data needs EXPENSE_MANAGEMENT_SPEC"

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Fee collection summary: total collected, pending, waivers/scholarships |
| FR-2 | Expense breakdown by category (pie chart) |
| FR-3 | Fee vs expense: income vs expenditure (bar chart, monthly) |
| FR-4 | Per-student cost: total expense / total students |
| FR-5 | Infrastructure investments: major expenses > ₹50,000 with description |
| FR-6 | Staff distribution: teaching/non-teaching ratio, qualification breakdown |
| FR-7 | Admin controls: toggle visibility of each section |
| FR-8 | Public access: no login required (or parent login) |
| FR-9 | Monthly auto-update from expense + fee data |

---

## 4. Database Design

```sql
CREATE TABLE transparency_dashboard_config (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL UNIQUE,
    show_fee_collection BOOLEAN NOT NULL DEFAULT true,
    show_expense_breakdown BOOLEAN NOT NULL DEFAULT true,
    show_fee_vs_expense BOOLEAN NOT NULL DEFAULT true,
    show_per_student_cost BOOLEAN NOT NULL DEFAULT true,
    show_infrastructure BOOLEAN NOT NULL DEFAULT true,
    show_staff_distribution BOOLEAN NOT NULL DEFAULT true,
    is_public BOOLEAN NOT NULL DEFAULT false,     -- if false, only parents can see
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);
```

---

## 5. Backend Architecture

### 5.1 TransparencyService

```kotlin
class TransparencyService {
    suspend fun getDashboard(schoolId: UUID, academicYearId: UUID): TransparencyDashboardDto {
        // 1. Check config (what's visible)
        // 2. Aggregate fee collection from fee_records + payments
        // 3. Aggregate expenses from expenses table (by category)
        // 4. Calculate per-student cost
        // 5. Fetch infrastructure investments (expenses > 50000)
        // 6. Staff distribution from app_users + non_teaching_staff
        // 7. Return DTO
    }

    suspend fun updateConfig(schoolId: UUID, config: UpdateConfigRequest)
}
```

---

## 6. API Contracts

```
# Public/Parent
GET /api/v1/transparency/{schoolId}?academic_year_id={uuid}

# Admin
GET/PATCH /api/v1/school/transparency/config
```

**Response:**
```json
{
  "success": true,
  "data": {
    "academic_year": "2026-27",
    "fee_collection": {
      "total_collected": 4500000,
      "total_pending": 500000,
      "scholarships_waived": 300000
    },
    "expense_breakdown": [
      {"category": "Salary", "amount": 2800000, "percentage": 62},
      {"category": "Infrastructure", "amount": 800000, "percentage": 18},
      {"category": "Transport", "amount": 450000, "percentage": 10},
      {"category": "Utilities", "amount": 250000, "percentage": 6},
      {"category": "Other", "amount": 200000, "percentage": 4}
    ],
    "per_student_cost": 45000,
    "staff_distribution": {
      "teaching": 25,
      "non_teaching": 8,
      "ratio": "1:18"
    }
  }
}
```

---

## 7. Acceptance Criteria

- [ ] Fee collection summary displayed
- [ ] Expense breakdown by category (pie chart)
- [ ] Fee vs expense comparison (monthly bar chart)
- [ ] Per-student cost calculated
- [ ] Infrastructure investments listed
- [ ] Staff distribution shown
- [ ] Admin can toggle section visibility
- [ ] Dashboard accessible (public or parent-only)
- [ ] Monthly auto-update

---

## 8. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration, Exposed table |
| 2 | 2 days | TransparencyService (aggregation) |
| 3 | 1 day | API endpoints + config |
| 4 | 3 days | Client UI (dashboard with charts, admin config) |
| 5 | 1 day | Tests |

---

## 9. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | `TransparencyDashboardConfigTable` |
| `server/.../feature/transparency/TransparencyService.kt` | New | Core service |
| `docs/db/migration_081_transparency_dashboard.sql` | New | DDL |
| `composeApp/.../ui/v2/screens/parent/TransparencyScreen.kt` | New | Parent dashboard view |
| `composeApp/.../ui/v2/screens/admin/TransparencyConfigScreen.kt` | New | Admin config |
