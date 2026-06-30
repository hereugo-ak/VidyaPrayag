# Transparency Dashboard — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** `EXPENSE_MANAGEMENT_SPEC.md`, `FEE_PAYMENT_SPEC.md`
> **Source:** `DIFFERENTIATING_FEATURES.md` §9.2
> **Template:** `_SPEC_TEMPLATE.md` v1 (25 mandatory + 6 optional sections)

---

## 1. Feature Overview

### What

Public-facing transparency dashboard showing school financial health, fee utilization, infrastructure investments, and staff distribution. Builds trust with parents by showing where their fee money goes.

### Why — Product Rationale

Parents in India often lack visibility into how schools utilize fee revenue. A transparency dashboard builds trust by showing fee collection vs expenses, infrastructure investments, and staff distribution. This is a **medium-priority differentiating feature** (Phase 3, effort M) that sets the platform apart from competitors who don't offer financial transparency.

### What Stands Out (Competitive Moat)

From `DIFFERENTIATING_FEATURES.md` §9.2:
> "Transparency Dashboard — fee utilization, expense breakdown, infrastructure investments. Data readiness: Fee data exists, expense data needs EXPENSE_MANAGEMENT_SPEC."

No major Indian school ERP offers a public-facing financial transparency dashboard. The key moat is **voluntary transparency** — schools that choose to show this build stronger parent trust.

### Goals

- Fee collection vs expense breakdown (sankey-style visualization)
- Category-wise expense distribution (salary, infrastructure, transport, etc.)
- Fee utilization per student (avg fee collected / avg expense per student)
- Infrastructure investments over time
- Staff distribution (teaching vs non-teaching, qualification breakdown)
- Admin-configurable: choose what to show publicly
- Updated monthly

### Non-goals

- [ ] Real-time financial data (monthly aggregation is sufficient)
- [ ] Detailed transaction-level expense reporting (aggregated categories only)
- [ ] Profit/loss statement (not a financial accounting tool)
- [ ] Comparison with other schools (benchmarking is separate feature)
- [ ] Parent payment history (that's in fee payment module)
- [ ] Budget planning/forecasting (future enhancement)
- [ ] Audit-grade financial reporting (aggregated, transparency-focused only)

### Dependencies

- `EXPENSE_MANAGEMENT_SPEC.md` — expense data by category
- `FEE_PAYMENT_SPEC.md` — fee collection data
- `SchoolsTable` — school info, board, medium
- `NonTeachingStaffTable` + `AppUsersTable` (role=teacher) — staff data
- `AcademicYearsTable` — academic year context

### Related Modules

- `server/.../feature/transparency/` — new transparency module
- `server/.../feature/expense/` — existing expense management module
- `server/.../feature/fees/` — existing fee payment module
- `composeApp/.../ui/v2/screens/parent/` — parent dashboard view
- `composeApp/.../ui/v2/screens/admin/` — admin config

---

## 2. Current System Assessment

### Existing Code

- `EXPENSE_MANAGEMENT_SPEC.md` — expense data by category
- `FEE_PAYMENT_SPEC.md` — fee collection data
- `SchoolsTable` — school info, board, medium
- `NonTeachingStaffTable` + `AppUsersTable` (role=teacher) — staff data
- `DIFFERENTIATING_FEATURES.md` §9.2: Transparency Dashboard, effort M, data readiness: "Fee data exists, expense data needs EXPENSE_MANAGEMENT_SPEC"

### Existing Database

- `SchoolsTable` — school info (name, board, medium, etc.)
- `FeeRecordsTable` / `PaymentsTable` — fee collection data
- `ExpensesTable` — expense data by category (from `EXPENSE_MANAGEMENT_SPEC.md`)
- `AppUsersTable` — user accounts (teachers)
- `NonTeachingStaffTable` — non-teaching staff info
- `AcademicYearsTable` — academic year context
- No transparency dashboard config table exists

### Existing APIs

- Fee payment API (existing) — fee collection data
- Expense management API (existing) — expense data by category
- Staff management API (existing) — staff data
- No transparency dashboard API exists

### Existing UI

- Fee management screens (existing)
- Expense management screens (existing)
- Staff management screens (existing)
- No transparency dashboard UI exists

### Existing Services

- `FeeService` — fee collection calculations
- `ExpenseService` — expense aggregation by category
- No transparency service

### Existing Documentation

- `DIFFERENTIATING_FEATURES.md` §9.2 — Transparency Dashboard
- `EXPENSE_MANAGEMENT_SPEC.md` — expense management
- `FEE_PAYMENT_SPEC.md` — fee payment

### Technical Debt

| # | Gap | Details |
|---|---|---|
| TD-1 | No transparency config | No `transparency_dashboard_config` table |
| TD-2 | No aggregation service | No service to aggregate fee + expense data for dashboard |
| TD-3 | No public dashboard | No public-facing dashboard view |
| TD-4 | No admin config UI | No admin controls for section visibility |

### Gaps

| # | Gap | Impact | Severity |
|---|---|---|---|
| G1 | No transparency config | Can't control what's visible publicly | **High** |
| G2 | No aggregation service | Can't calculate dashboard metrics | **High** |
| G3 | No public dashboard | Parents can't see financial transparency | **High** |
| G4 | No admin config UI | Admins can't control visibility | **Medium** |
| G5 | No per-student cost calculation | Can't show cost efficiency | **Medium** |
| G6 | No infrastructure investment view | Can't show major investments | **Medium** |
| G7 | No staff distribution view | Can't show staffing transparency | **Medium** |

---

## 3. Functional Requirements

### FR-001
| Field | Value |
|---|---|
| **Title** | Fee Collection Summary |
| **Description** | Fee collection summary: total collected, pending, waivers/scholarships. |
| **Priority** | High |
| **User Roles** | Public, Parent |
| **Acceptance notes** | Aggregated from fee_records + payments. Shows total_collected, total_pending, scholarships_waived for the academic year. |

### FR-002
| Field | Value |
|---|---|
| **Title** | Expense Breakdown by Category |
| **Description** | Expense breakdown by category (pie chart). |
| **Priority** | High |
| **User Roles** | Public, Parent |
| **Acceptance notes** | Aggregated from expenses table. Categories: Salary, Infrastructure, Transport, Utilities, Other. Shows amount and percentage per category. |

### FR-003
| Field | Value |
|---|---|
| **Title** | Fee vs Expense Comparison |
| **Description** | Fee vs expense: income vs expenditure (bar chart, monthly). |
| **Priority** | High |
| **User Roles** | Public, Parent |
| **Acceptance notes** | Monthly bar chart comparing fee collection vs expenses. Shows surplus/deficit per month. |

### FR-004
| Field | Value |
|---|---|
| **Title** | Per-Student Cost |
| **Description** | Per-student cost: total expense / total students. |
| **Priority** | Medium |
| **User Roles** | Public, Parent |
| **Acceptance notes** | Calculated as total_annual_expense / total_students. Shows average cost per student. |

### FR-005
| Field | Value |
|---|---|
| **Title** | Infrastructure Investments |
| **Description** | Infrastructure investments: major expenses > ₹50,000 with description. |
| **Priority** | Medium |
| **User Roles** | Public, Parent |
| **Acceptance notes** | Filtered from expenses table where amount > 50,000 and category = Infrastructure. Shows description and amount. |

### FR-006
| Field | Value |
|---|---|
| **Title** | Staff Distribution |
| **Description** | Staff distribution: teaching/non-teaching ratio, qualification breakdown. |
| **Priority** | Medium |
| **User Roles** | Public, Parent |
| **Acceptance notes** | Teaching count from AppUsersTable (role=teacher). Non-teaching count from NonTeachingStaffTable. Ratio calculated. Qualification breakdown if available. |

### FR-007
| Field | Value |
|---|---|
| **Title** | Admin Controls |
| **Description** | Admin controls: toggle visibility of each section. |
| **Priority** | High |
| **User Roles** | School Admin |
| **Acceptance notes** | Admin can toggle: show_fee_collection, show_expense_breakdown, show_fee_vs_expense, show_per_student_cost, show_infrastructure, show_staff_distribution. Also toggle is_public (public vs parent-only). |

### FR-008
| Field | Value |
|---|---|
| **Title** | Public/Parent Access |
| **Description** | Public access: no login required (or parent login). |
| **Priority** | High |
| **User Roles** | Public, Parent |
| **Acceptance notes** | If is_public=true, no login required. If is_public=false, parent login required. Admin/counselor/teacher can also view. |

### FR-009
| Field | Value |
|---|---|
| **Title** | Monthly Auto-Update |
| **Description** | Monthly auto-update from expense + fee data. |
| **Priority** | Medium |
| **User Roles** | System |
| **Acceptance notes** | Dashboard data aggregated on-demand from live data. No separate snapshot needed — always reflects current state. Monthly job can pre-compute and cache for performance. |

### Non-Functional Requirements

| ID | Requirement |
|---|---|
| NFR-1 | Dashboard load: < 3 seconds |
| NFR-2 | Config update: < 1 second |
| NFR-3 | Aggregation query: < 5 seconds (for large schools) |
| NFR-4 | Dashboard accessible 24/7 (public endpoint) |
| NFR-5 | Data freshness: current academic year, updated on-demand |

---

## 4. User Stories

### Public User
- [ ] View school transparency dashboard (if is_public=true)
- [ ] See fee collection summary
- [ ] See expense breakdown by category
- [ ] See fee vs expense comparison
- [ ] See per-student cost
- [ ] See infrastructure investments
- [ ] See staff distribution

### Parent
- [ ] View child's school transparency dashboard
- [ ] See all visible sections (same as public, plus parent-only if configured)

### School Admin
- [ ] View transparency dashboard
- [ ] Configure section visibility (toggle each section)
- [ ] Toggle public vs parent-only access
- [ ] Preview dashboard before making public

### System
- [ ] Aggregate fee collection data from fee_records + payments
- [ ] Aggregate expense data by category
- [ ] Calculate per-student cost
- [ ] Fetch infrastructure investments (> ₹50,000)
- [ ] Calculate staff distribution
- [ ] Respect admin config (hide/show sections)

---

## 5. Business Rules

### BR-001
**Rule:** Dashboard respects admin config.
**Enforcement:** Each section has a boolean toggle in `transparency_dashboard_config`. If toggle is false, section is omitted from response. Admin controls visibility.

### BR-002
**Rule:** Public access requires is_public=true.
**Enforcement:** If `is_public=false`, dashboard requires parent/admin/teacher JWT auth. If `is_public=true`, no auth required for GET endpoint.

### BR-003
**Rule:** One config per school.
**Enforcement:** `UNIQUE(school_id)` constraint on `transparency_dashboard_config`. One config row per school.

### BR-004
**Rule:** Data is read-only for public/parent.
**Enforcement:** Only admin can modify config. Public/parent can only GET dashboard data. No write access.

### BR-005
**Rule:** Dashboard data is aggregated, not transaction-level.
**Enforcement:** Dashboard shows aggregated totals and category breakdowns. Individual transactions are not exposed. Protects privacy while providing transparency.

### BR-006
**Rule:** Infrastructure investments threshold is ₹50,000.
**Enforcement:** Only expenses with amount > 50,000 and category = Infrastructure are shown in infrastructure investments section.

### BR-007
**Rule:** Per-student cost uses total enrolled students.
**Enforcement:** Per-student cost = total_annual_expense / total_enrolled_students. Uses current academic year enrollment count.

### BR-008
**Rule:** Dashboard data is school-scoped.
**Enforcement:** All data filtered by school_id. No cross-school data. Multi-branch schools see per-branch data (if MULTI_BRANCH enabled).

### BR-009
**Rule:** Dashboard shows current academic year by default.
**Enforcement:** Dashboard defaults to current academic year. Admin/parent can specify academic_year_id query parameter for historical data.

### BR-010
**Rule:** Staff distribution is headcount-based.
**Enforcement:** Teaching count = count of AppUsersTable where role=teacher and is_active=true. Non-teaching count = count of NonTeachingStaffTable where is_active=true. Ratio = total_students / total_teaching_staff.

---

## 6. Database Design

### 6.1 Entity Relationship Summary

One new table: `transparency_dashboard_config` (admin configuration for dashboard visibility). All dashboard data is aggregated from existing tables (fee_records, payments, expenses, app_users, non_teaching_staff).

### 6.2 New Tables

#### `transparency_dashboard_config` table

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
    is_public BOOLEAN NOT NULL DEFAULT false,
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);
```

### 6.3 Modified Tables

N/A — no existing tables modified. Dashboard reads from existing tables.

### 6.4 Indexes

- `transparency_dashboard_config(school_id)` — UNIQUE, one config per school

### 6.5 Constraints

- `transparency_dashboard_config.school_id` — NOT NULL, UNIQUE
- All `show_*` columns — NOT NULL, BOOLEAN, default `true`
- `is_public` — NOT NULL, BOOLEAN, default `false`

### 6.6 Foreign Keys

- `transparency_dashboard_config.school_id` → `schools.id` (implicit)

### 6.7 Soft Delete Strategy

N/A — config is a single row per school. No soft delete. Config is updated in place.

### 6.8 Audit Fields

- `transparency_dashboard_config.updated_at` — last config update timestamp

### 6.9 Migration Notes

Migration: `docs/db/migration_081_transparency_dashboard.sql`
- CREATE 1 table: `transparency_dashboard_config`
- No data migration (new feature)
- Default config created when school first accesses dashboard (lazy initialization)

### 6.10 Exposed Mappings

```kotlin
object TransparencyDashboardConfigTable : UUIDTable("transparency_dashboard_config", "id") {
    val schoolId              = uuid("school_id").uniqueIndex()
    val showFeeCollection     = bool("show_fee_collection").default(true)
    val showExpenseBreakdown  = bool("show_expense_breakdown").default(true)
    val showFeeVsExpense      = bool("show_fee_vs_expense").default(true)
    val showPerStudentCost    = bool("show_per_student_cost").default(true)
    val showInfrastructure    = bool("show_infrastructure").default(true)
    val showStaffDistribution = bool("show_staff_distribution").default(true)
    val isPublic              = bool("is_public").default(false)
    val updatedAt             = timestamp("updated_at")
}
```

Register in `DatabaseFactory.allTables`.

### 6.11 Seed Data

N/A — config created lazily when school first accesses dashboard. Defaults: all sections visible, is_public=false.

---

## 7. State Machines

### Dashboard Config State Machine

```
not_configured ──admin_first_access──> configured (all visible, not public)
  │
  └──configured ──admin_toggles_section──> configured (updated visibility)
                 ──admin_toggles_public──> configured (public/parent-only)
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `not_configured` | Admin first access | `configured` | Default config created (all visible, not public) |
| `configured` | Admin toggles section visibility | `configured` | Section show/hide updated |
| `configured` | Admin toggles is_public | `configured` | Public access enabled/disabled |
| `configured` | Admin updates config | `configured` | updated_at refreshed |

### Dashboard Visibility State Machine

```
is_public=false ──admin_enables_public──> is_public=true
  │                                        │
  │                                        └──admin_disables_public──> is_public=false
  │
  └──parent_access──> dashboard_visible (parent auth required)
  └──public_access──> 403 (auth required)
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `is_public=false` | Admin enables public | `is_public=true` | Admin role |
| `is_public=true` | Admin disables public | `is_public=false` | Admin role |
| `is_public=false` | Public user accesses | `403` | Auth required |
| `is_public=false` | Parent accesses | `dashboard_visible` | Parent JWT auth |
| `is_public=true` | Public user accesses | `dashboard_visible` | No auth required |
| `is_public=true` | Parent accesses | `dashboard_visible` | No auth required |

### Section Visibility State Machine

```
section_hidden ──admin_toggles_on──> section_visible
  │                                   │
  │                                   └──admin_toggles_off──> section_hidden
  │
  └──dashboard_request──> section_omitted (not in response)
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `section_hidden` | Admin toggles on | `section_visible` | Admin role |
| `section_visible` | Admin toggles off | `section_hidden` | Admin role |
| `section_hidden` | Dashboard request | `section_omitted` | Section not in response |
| `section_visible` | Dashboard request | `section_included` | Section in response |

---

## 8. Backend Architecture

### 8.1 Component Overview

`TransparencyService` aggregates fee collection, expense, and staff data to produce the dashboard DTO. It reads config from `transparency_dashboard_config` to determine which sections to include. `TransparencyRouting` exposes public/parent GET endpoint and admin PATCH endpoint.

### 8.2 Design Principles

1. **Aggregated, not transaction-level** — dashboard shows totals and category breakdowns, not individual transactions
2. **Admin-controlled visibility** — each section can be toggled on/off by admin
3. **Public or parent-only** — admin decides if dashboard is publicly accessible or parent-only
4. **On-demand aggregation** — data computed from live tables, cached for performance
5. **Read-only for public/parent** — only admin can modify config
6. **School-scoped** — all data filtered by school_id

### 8.3 Core Types

#### TransparencyService

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
    suspend fun getConfig(schoolId: UUID): TransparencyConfigDto
}
```

### 8.4 Repositories

- `TransparencyConfigRepository` — CRUD for `transparency_dashboard_config`
- Uses existing `FeeRepository`, `ExpenseRepository` for data aggregation

### 8.5 Mappers

- `TransparencyConfigMapper` — maps `transparency_dashboard_config` rows to `TransparencyConfigDto`

### 8.6 Permission Checks

- Public GET: no auth if `is_public=true`; parent JWT if `is_public=false`
- Admin GET config: admin role
- Admin PATCH config: admin role
- All other roles (teacher, counselor): can view dashboard if `is_public=true` or with JWT

### 8.7 Background Jobs

- **Dashboard Pre-Compute Job** — monthly (1st of month, 2 AM IST)
  1. For each school with transparency dashboard enabled
  2. Pre-compute dashboard data and cache
  3. Store in cache with 30-day TTL
  4. Reduces on-demand aggregation load

### 8.8 Domain Events

- `TransparencyConfigUpdated` — emitted when admin updates config
- `TransparencyDashboardViewed` — emitted when dashboard is viewed (analytics)

### 8.9 Caching

- Dashboard data: cached per school + academic_year, 1-hour TTL (on-demand) or 30-day TTL (pre-computed)
- Config: cached per school, 5-minute TTL

### 8.10 Transactions

- Config update: single transaction (upsert config row)
- Dashboard read: no transaction (read-only, multiple queries)

### 8.11 Rate Limiting

- Public endpoint: 100 requests per minute per IP (prevent abuse)
- Parent endpoint: standard JWT-based limits

### 8.12 Configuration

- `TRANSPARENCY_DASHBOARD_ENABLED` — default `true`; enable/disable feature globally
- `TRANSPARENCY_CACHE_TTL_HOURS` — default `1`; cache TTL for on-demand aggregation
- `TRANSPARENCY_PRECOMPUTE_ENABLED` — default `true`; enable monthly pre-compute job
- `TRANSPARENCY_INFRA_THRESHOLD` — default `50000`; threshold for infrastructure investments

---

## 9. API Contracts

### 9.1 Public/Parent Endpoint

```
GET /api/v1/transparency/{schoolId}?academic_year_id={uuid}
  → 200: TransparencyDashboardDto
  → 403: Dashboard not public (if is_public=false and no auth)
  → 404: School not found
```

### 9.2 Admin Config Endpoints

```
GET /api/v1/school/transparency/config
  → 200: TransparencyConfigDto

PATCH /api/v1/school/transparency/config
  Body: {
    show_fee_collection: true,
    show_expense_breakdown: true,
    show_fee_vs_expense: true,
    show_per_student_cost: true,
    show_infrastructure: false,
    show_staff_distribution: true,
    is_public: true
  }
  → 200: TransparencyConfigDto (updated)
```

### 9.3 Response Body

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
    "fee_vs_expense": [
      {"month": "Apr", "collected": 450000, "spent": 380000},
      {"month": "May", "collected": 520000, "spent": 410000},
      {"month": "Jun", "collected": 480000, "spent": 395000}
    ],
    "per_student_cost": 45000,
    "infrastructure_investments": [
      {"description": "New science lab equipment", "amount": 150000, "date": "2026-04-15"},
      {"description": "Library renovation", "amount": 200000, "date": "2026-05-20"},
      {"description": "Smart boards for 10 classrooms", "amount": 350000, "date": "2026-06-10"}
    ],
    "staff_distribution": {
      "teaching": 25,
      "non_teaching": 8,
      "ratio": "1:18"
    }
  }
}
```

### 9.4 DTO Models

All `@Serializable`, wrapped in `ApiResponse<T>` pattern.

```kotlin
@Serializable data class TransparencyDashboardDto(
    val academicYear: String,
    val feeCollection: FeeCollectionSummaryDto?,
    val expenseBreakdown: List<ExpenseCategoryDto>?,
    val feeVsExpense: List<MonthlyComparisonDto>?,
    val perStudentCost: Float?,
    val infrastructureInvestments: List<InfrastructureInvestmentDto>?,
    val staffDistribution: StaffDistributionDto?,
)

@Serializable data class FeeCollectionSummaryDto(
    val totalCollected: Long,
    val totalPending: Long,
    val scholarshipsWaived: Long,
)

@Serializable data class ExpenseCategoryDto(
    val category: String,
    val amount: Long,
    val percentage: Float,
)

@Serializable data class MonthlyComparisonDto(
    val month: String,
    val collected: Long,
    val spent: Long,
)

@Serializable data class InfrastructureInvestmentDto(
    val description: String,
    val amount: Long,
    val date: String,
)

@Serializable data class StaffDistributionDto(
    val teaching: Int,
    val nonTeaching: Int,
    val ratio: String,
)

@Serializable data class TransparencyConfigDto(
    val showFeeCollection: Boolean,
    val showExpenseBreakdown: Boolean,
    val showFeeVsExpense: Boolean,
    val showPerStudentCost: Boolean,
    val showInfrastructure: Boolean,
    val showStaffDistribution: Boolean,
    val isPublic: Boolean,
    val updatedAt: String,
)

@Serializable data class UpdateConfigRequest(
    val showFeeCollection: Boolean? = null,
    val showExpenseBreakdown: Boolean? = null,
    val showFeeVsExpense: Boolean? = null,
    val showPerStudentCost: Boolean? = null,
    val showInfrastructure: Boolean? = null,
    val showStaffDistribution: Boolean? = null,
    val isPublic: Boolean? = null,
)
```

---

## 10. Frontend Architecture

### 10.1 Screens

| Screen | Platform | Role | Description |
|---|---|---|---|
| `TransparencyScreen` | Compose | Public, Parent | Dashboard with charts and data |
| `TransparencyConfigScreen` | Compose | School Admin | Config UI for section visibility |

### 10.2 Navigation

- Public: Direct URL `/transparency/{schoolId}` (no login if is_public=true)
- Parent: Home → Transparency Dashboard
- Admin: Settings → Transparency Config

### 10.3 UX Flows

#### Public/Parent: View Dashboard
1. Navigate to transparency dashboard
2. See fee collection summary (if visible)
3. See expense breakdown pie chart (if visible)
4. See fee vs expense bar chart (if visible)
5. See per-student cost (if visible)
6. See infrastructure investments list (if visible)
7. See staff distribution (if visible)

#### Admin: Configure Dashboard
1. Admin opens Settings → Transparency Config
2. See toggles for each section
3. Toggle sections on/off
4. Toggle is_public (public vs parent-only)
5. Save → config updated
6. Preview dashboard

### 10.4 State Management

```kotlin
data class TransparencyDashboardState(
    val dashboard: TransparencyDashboardDto?,
    val isLoading: Boolean,
    val error: String?,
)

data class TransparencyConfigState(
    val config: TransparencyConfigDto?,
    val isLoading: Boolean,
    val isSaving: Boolean,
    val error: String?,
)
```

### 10.5 Offline Support

- Dashboard: cached locally (last fetched, 1-hour TTL)
- Config: cached locally (last fetched, 5-minute TTL)

### 10.6 Loading States

- Dashboard: "Loading transparency data..."
- Config: "Loading config..." / "Saving..."

### 10.7 Error Handling (UI)

- Dashboard not public: "This dashboard is available to parents only. Please log in."
- No data: "No transparency data available for this academic year."
- Network error: "Connection issue. Please check your internet."

### 10.8 Component Integration Guidelines

| Rule | Description |
|---|---|
| **R1** | Fee collection: 3 stat cards (collected, pending, waived) |
| **R2** | Expense breakdown: pie chart with category labels and percentages |
| **R3** | Fee vs expense: grouped bar chart (monthly, collected vs spent) |
| **R4** | Per-student cost: large stat card with ₹ amount |
| **R5** | Infrastructure investments: list of items with description, amount, date |
| **R6** | Staff distribution: donut chart (teaching vs non-teaching) + ratio text |
| **R7** | Config: toggle switches for each section + is_public toggle |
| **R8** | Hidden sections: completely omitted from dashboard (not greyed out) |
| **R9** | Currency: Indian Rupee (₹) formatting with lakhs/crores |
| **R10** | Charts: use Compose Charts library (or similar) |

---

## 11. Shared Module Changes (KMP)

### 11.1 DTOs

All DTOs defined in section 9.4, placed in `shared/.../transparency/domain/model/TransparencyModels.kt`.

### 11.2 Domain Models

```kotlin
data class TransparencyDashboard(
    val academicYear: String,
    val feeCollection: FeeCollectionSummary?,
    val expenseBreakdown: List<ExpenseCategory>?,
    val feeVsExpense: List<MonthlyComparison>?,
    val perStudentCost: Float?,
    val infrastructureInvestments: List<InfrastructureInvestment>?,
    val staffDistribution: StaffDistribution?,
)

data class FeeCollectionSummary(
    val totalCollected: Long,
    val totalPending: Long,
    val scholarshipsWaived: Long,
)

data class ExpenseCategory(
    val category: String,
    val amount: Long,
    val percentage: Float,
)

data class MonthlyComparison(
    val month: String,
    val collected: Long,
    val spent: Long,
)

data class InfrastructureInvestment(
    val description: String,
    val amount: Long,
    val date: LocalDate,
)

data class StaffDistribution(
    val teaching: Int,
    val nonTeaching: Int,
    val ratio: String,
)

data class TransparencyConfig(
    val showFeeCollection: Boolean,
    val showExpenseBreakdown: Boolean,
    val showFeeVsExpense: Boolean,
    val showPerStudentCost: Boolean,
    val showInfrastructure: Boolean,
    val showStaffDistribution: Boolean,
    val isPublic: Boolean,
    val updatedAt: Instant,
)
```

### 11.3 Repository Interfaces

```kotlin
interface TransparencyRepository {
    suspend fun getDashboard(token: String?, schoolId: String, academicYearId: String?): NetworkResult<TransparencyDashboardDto>
    suspend fun getConfig(token: String): NetworkResult<TransparencyConfigDto>
    suspend fun updateConfig(token: String, request: UpdateConfigRequest): NetworkResult<TransparencyConfigDto>
}
```

### 11.4 UseCases

- `GetDashboardUseCase`
- `GetConfigUseCase`
- `UpdateConfigUseCase`

### 11.5 Validation

- `school_id`: valid UUID
- `academic_year_id`: valid UUID (optional, defaults to current)
- Config booleans: true/false (optional in update, null = no change)

### 11.6 Serialization

Standard Kotlinx serialization. Monetary values as Long (paise/cents internally, displayed as rupees).

### 11.7 Network APIs

Ktor `@Resource` route definitions in `TransparencyApi.kt`:
- GET `/api/v1/transparency/{schoolId}?academic_year_id={academicYearId}`
- GET `/api/v1/school/transparency/config`
- PATCH `/api/v1/school/transparency/config`

### 11.8 Database Models (Local Cache)

- Dashboard data cached in local DB (last fetched, 1-hour TTL)
- Config cached in local DB (last fetched, 5-minute TTL)

---

## 12. Permissions Matrix

| Action | Super Admin | School Admin | Counselor | Teacher | Parent | Public (Anonymous) |
|---|---|---|---|---|---|---|
| View dashboard (is_public=true) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| View dashboard (is_public=false) | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| View config | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| Update config | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| Toggle section visibility | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| Toggle is_public | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |

---

## 13. Notifications

### Transparency Notifications

| Trigger | Recipient | Channel | Template |
|---|---|---|---|
| Dashboard made public | All parents | FCM + in-app | "School transparency dashboard is now publicly available." |
| Dashboard config updated | Admin | in-app | "Transparency dashboard config updated." |
| Monthly dashboard updated | Parents (if opted in) | in-app | "Monthly transparency dashboard has been updated." |

### Notification Integration

Uses existing `Notify.kt` dispatch. Notifications created with category "transparency".

---

## 14. Background Jobs

### Dashboard Pre-Compute Job

| Property | Value |
|---|---|
| **Name** | `TransparencyPreComputeJob` |
| **Schedule** | Monthly (1st of month, 2 AM IST) |
| **Duration** | < 5 minutes for 100 schools |
| **Retry** | None (next month) |

#### Job Flow

1. Get all schools with transparency dashboard enabled
2. For each school:
   - Aggregate fee collection data
   - Aggregate expense data by category
   - Calculate per-student cost
   - Fetch infrastructure investments
   - Calculate staff distribution
   - Cache result (30-day TTL)
3. Return count of dashboards pre-computed

---

## 15. Integrations

### Internal Integrations

| System | Integration Point | Direction | Protocol | Error Handling |
|---|---|---|---|---|
| `FeeService` / `FeeRepository` | Fee collection data | Read | Direct DB | Return 0 if no data |
| `ExpenseService` / `ExpenseRepository` | Expense data by category | Read | Direct DB | Return 0 if no data |
| `AppUsersTable` | Teaching staff count | Read | Direct DB | Return 0 if no data |
| `NonTeachingStaffTable` | Non-teaching staff count | Read | Direct DB | Return 0 if no data |
| `SchoolsTable` | School info | Read | Direct DB | Required |
| `AcademicYearsTable` | Academic year context | Read | Direct DB | Required |
| `Notify.kt` | Notifications | Call | Direct call | Log on failure |

### External Integrations

N/A — no external integrations. All data is internal.

### Integration Patterns

- **Fee collection:** `feeRepository.getTotalCollected(schoolId, academicYearId)` — aggregated sum
- **Expenses:** `expenseRepository.getExpensesByCategory(schoolId, academicYearId)` — grouped by category
- **Staff count:** `appUsersRepository.countByRole(schoolId, Role.TEACHER)` + `nonTeachingStaffRepository.count(schoolId)`
- **Notifications:** `Notify.kt` called with category "transparency"

---

## 16. Security

### Authentication

- Public GET: no auth if `is_public=true`; parent JWT if `is_public=false`
- Admin GET/PATCH config: admin role + JWT auth
- All other roles: JWT auth (if is_public=false)

### Authorization

- Only admin can modify config
- Public/parent can only view dashboard
- Config is admin-only

### Data Protection

- Dashboard shows aggregated data only (no individual transactions)
- No PII in dashboard data (no student names, no parent info)
- Staff distribution shows headcount only (no individual staff info)
- Infrastructure investments show description and amount only

### Input Validation

- `school_id`: valid UUID
- `academic_year_id`: valid UUID (optional)
- Config booleans: true/false

### Rate Limiting

- Public endpoint: 100 requests per minute per IP
- Parent endpoint: standard JWT-based limits

### Audit Logging

- Dashboard viewed: user ID (if auth), school ID, timestamp
- Config updated: admin ID, school ID, changes, timestamp
- Public access: IP address, school ID, timestamp

### PII Handling

- No PII in dashboard data
- No student names, parent names, or staff names
- Only aggregated financial and staffing data

### Multi-tenant Isolation

- All data filtered by school_id
- No cross-school data
- Config is per-school

---

## 17. Performance & Scalability

### Expected Scale

- 100-500 schools per deployment
- 500-2,000 students per school
- Dashboard accessed by parents (monthly) + public (occasional)
- Aggregation queries on fee_records + expenses tables

### Query Optimization

- Fee collection: SUM on payments table, indexed by school_id + academic_year_id
- Expense breakdown: SUM GROUP BY category, indexed by school_id + academic_year_id
- Staff count: COUNT queries, indexed by school_id + role
- Infrastructure: SELECT WHERE amount > 50000 AND category = 'Infrastructure', indexed by school_id

### Indexing Strategy

- Existing indexes on fee_records, payments, expenses tables (school_id, academic_year_id)
- `transparency_dashboard_config(school_id)` — UNIQUE

### Caching Strategy

- Dashboard data: cached per school + academic_year, 1-hour TTL (on-demand) or 30-day TTL (pre-computed)
- Config: cached per school, 5-minute TTL

### Pagination

N/A — dashboard is a single response with all visible sections.

### Connection Pooling

Uses existing HikariCP connection pool. No additional pooling needed.

### Async Processing

- Pre-compute job: background job (async)
- On-demand aggregation: synchronous (with cache)
- Notifications: async (fire-and-forget via `Notify.kt`)

### Scalability Concerns

- Aggregation queries: SUM on large tables. Pre-compute job reduces on-demand load.
- Public endpoint: rate-limited to prevent abuse.
- Cache: 30-day TTL for pre-computed, 1-hour for on-demand. Reduces DB load.

---

## 18. Edge Cases

| # | Scenario | Expected Behavior |
|---|---|---|
| EC-1 | School has no fee data | fee_collection shows 0 for all fields |
| EC-2 | School has no expense data | expense_breakdown is empty array |
| EC-3 | School has no students | per_student_cost is 0 (division by zero handled) |
| EC-4 | School has no infrastructure investments | infrastructure_investments is empty array |
| EC-5 | School has no staff | staff_distribution shows 0 teaching, 0 non-teaching, ratio "N/A" |
| EC-6 | Config not yet created | Create default config (all visible, not public), return dashboard |
| EC-7 | is_public=false and no auth | Return 403 "Dashboard available to parents only." |
| EC-8 | All sections hidden by admin | Dashboard returns empty data object with academic_year only |
| EC-9 | Academic year not found | Return 404 "Academic year not found." |
| EC-10 | School not found | Return 404 "School not found." |
| EC-11 | Expenses exceed fee collection | Show deficit in fee_vs_expense (negative surplus) |
| EC-12 | Public access rate limit exceeded | Return 429 "Too many requests." |

---

## 19. Error Handling

### Error Response Format

Standard `ApiResponse` error format.

### Error Codes

| Code | HTTP Status | Description | User Message |
|---|---|---|---|
| `DASHBOARD_NOT_PUBLIC` | 403 | Dashboard requires auth | "This dashboard is available to parents only. Please log in." |
| `SCHOOL_NOT_FOUND` | 404 | School ID not found | "School not found." |
| `ACADEMIC_YEAR_NOT_FOUND` | 404 | Academic year not found | "Academic year not found." |
| `CONFIG_NOT_FOUND` | 404 | Config not yet created | (Internal — auto-create default config) |
| `RATE_LIMIT_EXCEEDED` | 429 | Public rate limit exceeded | "Too many requests. Please try again later." |
| `INVALID_CONFIG` | 400 | Invalid config update | "Invalid configuration values." |

### Error Handling Strategy

- **Public access:** Return 403 if not public and no auth. Return 429 if rate limited.
- **Config update:** Return 400 on invalid values. Return 403 if not admin.
- **Data aggregation:** Return 0/empty for missing data (not an error).
- **Pre-compute job:** Log error per school. Continue with next.

### Retry Strategy

- Dashboard fetch: client retries on network error (3 attempts)
- Config update: client retries on network error (3 attempts)
- Pre-compute job: no retry (next month's job handles it)

### Fallback Behavior

- No fee data: show 0 for all fee collection fields
- No expense data: empty expense_breakdown array
- No students: per_student_cost = 0
- No staff: staff_distribution with 0 counts
- No infrastructure: empty infrastructure_investments array

---

## 20. Analytics & Reporting

### Analytics Dashboard Data

| Metric | Source | Derivation |
|---|---|---|
| Dashboard views | Audit logs | Count of dashboard views per school per month |
| Public vs parent views | Audit logs | Count by access type |
| Config change frequency | Audit logs | Count of config updates per school |
| Schools with public dashboard | `transparency_dashboard_config` | Count where is_public=true |
| Schools with dashboard enabled | `transparency_dashboard_config` | Count where any show_* = true |
| Most viewed sections | Audit logs | Section view counts (if tracked) |

### Export Capabilities

- Dashboard data export (CSV) — fee collection, expense breakdown, per-student cost
- Config export (JSON) — current config for audit purposes

### Report Types

| Report | Format | Frequency | Recipient |
|---|---|---|---|
| Transparency overview | JSON (API) | On-demand | Admin |
| Dashboard views | JSON (API) | Monthly | Product Team |
| Config audit | JSON (API) | On-demand | Admin |

---

## 21. Testing Strategy

### Unit Tests

- `TransparencyService.getDashboard()` — config check, section visibility
- `TransparencyService.updateConfig()` — config validation, upsert
- Fee collection aggregation — sum calculation
- Expense breakdown aggregation — category grouping, percentage calculation
- Per-student cost calculation — division by zero handling
- Infrastructure investments filtering — amount > 50000, category = Infrastructure
- Staff distribution — teaching/non-teaching count, ratio calculation

### Integration Tests

- Full dashboard flow: create config → aggregate data → verify response
- Config update: update config → verify sections hidden/shown
- Public access: is_public=true → no auth → 200
- Parent access: is_public=false → parent JWT → 200
- Public access denied: is_public=false → no auth → 403
- Empty data: no fee/expense data → 0/empty arrays
- Pre-compute job: run job → verify cache populated

### E2E Tests

- Admin configures dashboard → parent views dashboard
- Admin toggles section off → section disappears from dashboard
- Admin enables public access → public user views dashboard
- Monthly pre-compute → dashboard loads from cache

### Performance Tests

- Dashboard load: < 3 seconds
- Aggregation query: < 5 seconds (large school with 2,000 students)
- Pre-compute job: < 5 minutes for 100 schools
- Public rate limit: 100 requests/minute enforced

### Test Data

- 5 schools with varying data (full data, partial data, no data)
- Fee records + payments for current academic year
- Expenses by category (Salary, Infrastructure, Transport, Utilities, Other)
- Infrastructure investments (> ₹50,000)
- Staff data (teachers + non-teaching)
- Config rows (public, parent-only, all sections, some hidden)

### Test Environment

- Test database with fee, expense, staff tables
- Pre-seeded school and academic year data
- Test JWT tokens for admin, parent roles
- Mock public access (no auth)

---

## 22. Acceptance Criteria

- [ ] Fee collection summary displayed
- [ ] Expense breakdown by category (pie chart)
- [ ] Fee vs expense comparison (monthly bar chart)
- [ ] Per-student cost calculated
- [ ] Infrastructure investments listed
- [ ] Staff distribution shown
- [ ] Admin can toggle section visibility
- [ ] Dashboard accessible (public or parent-only)
- [ ] Monthly auto-update (pre-compute job)
- [ ] Hidden sections omitted from response
- [ ] Public access requires is_public=true
- [ ] Rate limiting on public endpoint

---

## 23. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration `migration_081_transparency_dashboard.sql`, Exposed table, register in `DatabaseFactory` |
| 2 | 2 days | `TransparencyService` (aggregation: fee, expense, staff, infrastructure) |
| 3 | 1 day | API endpoints + config (GET/PATCH config, GET dashboard) |
| 4 | 3 days | Client UI: `TransparencyScreen` (dashboard with charts), `TransparencyConfigScreen` (admin config) |
| 5 | 1 day | Tests: unit, integration, E2E |

### Pre-Implementation Checklist

- [ ] Verify `EXPENSE_MANAGEMENT_SPEC.md` is implemented and expense data available
- [ ] Verify `FEE_PAYMENT_SPEC.md` is implemented and fee data available
- [ ] Verify `NonTeachingStaffTable` exists and has data
- [ ] Verify `AppUsersTable` has teacher role data
- [ ] Verify `AcademicYearsTable` exists
- [ ] Verify chart library available for Compose UI

---

## 24. File-Level Impact Analysis

### Server

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | `TransparencyDashboardConfigTable` |
| `server/.../db/DatabaseFactory.kt` | Modify | Register `TransparencyDashboardConfigTable` in `allTables` |
| `server/.../feature/transparency/TransparencyService.kt` | **New** | Core service (aggregation, config) |
| `server/.../feature/transparency/TransparencyRouting.kt` | **New** | API endpoints |
| `server/.../feature/transparency/TransparencyPreComputeJob.kt` | **New** | Monthly pre-compute job |
| `docs/db/migration_081_transparency_dashboard.sql` | **New** | DDL |

### Shared (KMP)

| File | Change Type | Description |
|---|---|---|
| `shared/.../transparency/domain/model/TransparencyModels.kt` | **New** | DTOs, domain models |
| `shared/.../transparency/domain/repository/TransparencyRepository.kt` | **New** | Repository interface |
| `shared/.../transparency/data/remote/TransparencyApi.kt` | **New** | HTTP API definitions |

### Client (Compose)

| File | Change Type | Description |
|---|---|---|
| `composeApp/.../ui/v2/screens/parent/TransparencyScreen.kt` | **New** | Parent/public dashboard view |
| `composeApp/.../ui/v2/screens/admin/TransparencyConfigScreen.kt` | **New** | Admin config |

---

## 25. Future Enhancements

| # | Enhancement | Priority | Effort | Notes |
|---|---|---|---|---|
| F-1 | Budget planning & forecasting | Medium | L | Admin can set budgets, dashboard shows budget vs actual |
| F-2 | Historical year comparison | Medium | S | Compare current year vs previous year |
| F-3 | Sankey diagram for fee flow | Low | M | Visual flow: fees → categories → utilization |
| F-4 | Parent-specific dashboard view | Low | S | Show parent's own fee payments vs average |
| F-5 | Expense trend over time | Medium | S | Line chart of monthly expenses by category |
| F-6 | Downloadable reports (PDF) | Medium | M | Admin can download transparency report |
| F-7 | Multi-branch aggregated view | Low | M | Aggregate across branches (if MULTI_BRANCH) |
| F-8 | Third-party audit integration | Low | L | Integrate with external audit tools |
| F-9 | Expense sub-category breakdown | Low | S | Drill down from category to sub-categories |
| F-10 | Real-time dashboard updates | Low | M | WebSocket-based live updates |

---

## Appendix A: Sequence Diagrams

### A.1 Dashboard View Flow

```
Client (Public/Parent)    Server (TransparencyService)    DB
  │                           │                              │
  │  GET /transparency/{id}   │                              │
  │  ?academic_year_id=...    │                              │
  │  ──────────────────────>  │                              │
  │                           │──check config────────────────>│
  │                           │←──config row─────────────────│
  │                           │                              │
  │                           │  if is_public=false:         │
  │                           │    check JWT auth            │
  │                           │                              │
  │                           │──aggregate fee collection────>│
  │                           │←──total_collected, pending───│
  │                           │                              │
  │                           │──aggregate expenses by cat───>│
  │                           │←──category amounts───────────│
  │                           │                              │
  │                           │──calculate per-student cost──>│
  │                           │←──student count──────────────│
  │                           │                              │
  │                           │──fetch infrastructure────────>│
  │                           │←──investments > 50K──────────│
  │                           │                              │
  │                           │──count staff─────────────────>│
  │                           │←──teaching, non-teaching─────│
  │                           │                              │
  │                           │  build DTO (respect config)  │
  │  ←──200: DashboardDto─────│                              │
  │                           │                              │
```

### A.2 Config Update Flow

```
Admin (app)          Server                    DB
  │                    │                         │
  │  PATCH /config     │                         │
  │  {show_...: true}  │                         │
  │  ───────────────>  │                         │
  │                    │──verify admin role──────│ (JWT)
  │                    │──upsert config──────────>│
  │                    │←──success───────────────│
  │  ←──200: ConfigDto─│                         │
  │                    │                         │
```

---

## Appendix B: Domain Model / ER Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│              transparency_dashboard_config (new)                      │
│  id (PK)                                                              │
│  school_id (UNIQUE)                                                   │
│  show_fee_collection, show_expense_breakdown,                         │
│  show_fee_vs_expense, show_per_student_cost,                          │
│  show_infrastructure, show_staff_distribution (all BOOLEAN)           │
│  is_public (BOOLEAN, default false)                                   │
│  updated_at                                                           │
└──────────────────────────────────────────────────────────────────────┘

Existing tables used (read-only aggregation):
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│ fee_records      │  │ payments         │  │ expenses         │
│ (existing)       │  │ (existing)       │  │ (existing)       │
└──────────────────┘  └──────────────────┘  └──────────────────┘
┌──────────────────┐  ┌────────────────────┐  ┌──────────────────┐
│ app_users        │  │ non_teaching_staff │  │ schools          │
│ (existing,       │  │ (existing)         │  │ (existing)       │
│  role=teacher)   │  │                    │  │                  │
└──────────────────┘  └────────────────────┘  └──────────────────┘
┌──────────────────┐
│ academic_years   │
│ (existing)       │
└──────────────────┘
```

---

## Appendix C: Event Flow

### Domain Events

| Event | Emitter | Consumers | Payload | Side Effects |
|---|---|---|---|---|
| `TransparencyConfigUpdated` | `TransparencyService.updateConfig()` | `Notify.kt` | `schoolId, changes, updatedBy` | Notification to parents if is_public changed |
| `TransparencyDashboardViewed` | `TransparencyService.getDashboard()` | None (logged) | `schoolId, academicYearId, viewerType` | Analytics tracking |
| `TransparencyPreComputed` | `TransparencyPreComputeJob` | None (logged) | `schoolId, academicYearId` | Cache populated |

### Event Delivery Guarantees

- Config updated event: emitted synchronously, notification async
- Dashboard viewed event: emitted synchronously (fire-and-forget logging)
- Pre-computed event: emitted within job execution

---

## Appendix D: Configuration

### Environment Variables

| Variable | Default | Description |
|---|---|---|
| `TRANSPARENCY_DASHBOARD_ENABLED` | `true` | Enable/disable feature globally |
| `TRANSPARENCY_CACHE_TTL_HOURS` | `1` | Cache TTL for on-demand aggregation |
| `TRANSPARENCY_PRECOMPUTE_ENABLED` | `true` | Enable monthly pre-compute job |
| `TRANSPARENCY_INFRA_THRESHOLD` | `50000` | Threshold for infrastructure investments |
| `TRANSPARENCY_PUBLIC_RATE_LIMIT` | `100` | Requests per minute per IP for public endpoint |

### Feature Flags

| Flag | Default | Description |
|---|---|---|
| `TRANSPARENCY_DASHBOARD_ENABLED` | `true` | Enable/disable transparency dashboard |

### School-Level Settings

| Setting | Default | Description |
|---|---|---|
| `show_fee_collection` | `true` | Show fee collection section |
| `show_expense_breakdown` | `true` | Show expense breakdown section |
| `show_fee_vs_expense` | `true` | Show fee vs expense section |
| `show_per_student_cost` | `true` | Show per-student cost section |
| `show_infrastructure` | `true` | Show infrastructure investments section |
| `show_staff_distribution` | `true` | Show staff distribution section |
| `is_public` | `false` | Dashboard publicly accessible (vs parent-only) |

---

## Appendix E: Migration & Rollback

### Migration: `migration_081_transparency_dashboard.sql`

```sql
-- Migration 081: Transparency Dashboard
-- Creates transparency_dashboard_config table

BEGIN;

CREATE TABLE IF NOT EXISTS transparency_dashboard_config (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL UNIQUE,
    show_fee_collection BOOLEAN NOT NULL DEFAULT true,
    show_expense_breakdown BOOLEAN NOT NULL DEFAULT true,
    show_fee_vs_expense BOOLEAN NOT NULL DEFAULT true,
    show_per_student_cost BOOLEAN NOT NULL DEFAULT true,
    show_infrastructure BOOLEAN NOT NULL DEFAULT true,
    show_staff_distribution BOOLEAN NOT NULL DEFAULT true,
    is_public BOOLEAN NOT NULL DEFAULT false,
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

COMMIT;
```

### Rollback: `migration_081_rollback.sql`

```sql
BEGIN;
DROP TABLE IF EXISTS transparency_dashboard_config;
COMMIT;
```

### Migration Validation

- Verify table created with correct columns
- Verify `school_id` UNIQUE constraint
- Verify all defaults (show_* = true, is_public = false)
- Run `SELECT count(*) FROM transparency_dashboard_config` — should be 0 (lazy init)

---

## Appendix F: Observability

### Structured Logging

| Log Level | Event | Context Fields |
|---|---|---|
| INFO | Dashboard viewed | `schoolId, academicYearId, viewerType (public/parent/admin)` |
| INFO | Config updated | `schoolId, adminId, changes` |
| INFO | Config created (lazy init) | `schoolId` |
| INFO | Pre-compute job completed | `schoolCount, duration` |
| WARN | No fee data for school | `schoolId, academicYearId` |
| WARN | No expense data for school | `schoolId, academicYearId` |
| WARN | No staff data for school | `schoolId` |
| WARN | Public rate limit exceeded | `ip, schoolId` |
| ERROR | Pre-compute job failed | `schoolId, error` |
| ERROR | Aggregation query failed | `schoolId, error` |

### Metrics

| Metric | Type | Labels | Description |
|---|---|---|---|
| `transparency_dashboard_views` | Counter | `school_id, viewer_type` | Total dashboard views |
| `transparency_config_updates` | Counter | `school_id` | Total config updates |
| `transparency_public_dashboards` | Gauge | — | Count of schools with is_public=true |
| `transparency_precompute_duration` | Histogram | — | Pre-compute job duration |
| `transparency_aggregation_duration` | Histogram | `school_id` | On-demand aggregation duration |
| `transparency_rate_limit_hits` | Counter | `school_id` | Public rate limit exceeded count |

### Health Checks

| Check | Endpoint | Description |
|---|---|---|
| Transparency service | `/health/transparency` | Verify service and DB accessible |

### Alerts

| Alert | Condition | Severity | Notification |
|---|---|---|---|
| Pre-compute job failed | Job error rate > 10% | Warning | Email to dev team |
| Aggregation slow | Duration > 10 seconds | Warning | Email to dev team |
| Public rate limit high | Rate limit hits > 1000/hour | Info | Email to dev team (possible abuse) |

### Dashboards

| Dashboard | Panels | Audience |
|---|---|---|
| Transparency Overview | Views/month, public dashboards, config changes | Product Team |
| Job Performance | Pre-compute duration, aggregation duration, error rate | Dev Team |
| Public Access | Views by school, rate limit hits, viewer types | Product Team |

### Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Sensitive financial data exposed publicly | Low | High | Admin controls visibility. Aggregated data only. No transaction-level data. |
| Rate limit bypass (DDoS on public endpoint) | Medium | Medium | Rate limiting per IP. CDN caching. |
| Incorrect aggregation (wrong totals) | Low | High | Unit tests for aggregation. Cross-verify with fee/expense reports. |
| Config accidentally set to public | Low | Medium | Config update requires admin role. Audit log. |
| Stale cached data | Medium | Low | 1-hour TTL for on-demand, 30-day for pre-computed. Cache invalidation on config update. |
| Division by zero (no students) | Low | Low | Handle gracefully — return 0 for per_student_cost. |
