# Fee-Expense Transparency — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** `EXPENSE_MANAGEMENT_SPEC.md`, `FEE_PAYMENT_SPEC.md`, `TRANSPARENCY_DASHBOARD_SPEC.md`
> **Source:** `DIFFERENTIATING_FEATURES.md` §9.3
> **Template:** `_SPEC_TEMPLATE.md` v1 (25 mandatory + 6 optional sections)

---

## 1. Feature Overview

### What

Parent-facing view that shows exactly how their fee money is utilized: breakdown of fee components, what each component funds, and comparison of their contribution vs per-student cost. More granular than the public transparency dashboard.

### Why — Product Rationale

Parents want to know where their fee money goes. While the public transparency dashboard shows school-level financials, this personalized view shows each parent how *their* specific fee contribution maps to school expenses. This builds trust and justifies fee structures.

This is a **low-priority differentiating feature** (Phase 3, effort S) that extends the transparency dashboard with a parent-specific, personalized view.

### What Stands Out (Competitive Moat)

From `DIFFERENTIATING_FEATURES.md` §9.3:
> "Fee-Expense Transparency — parent-facing view showing how their fee money is utilized."

No major Indian school ERP offers a personalized fee-to-expense mapping for parents. The key moat is **personalized financial transparency** — not just "school spent X" but "your ₹15,000 funds: ₹9,300 salary, ₹2,700 infrastructure..."

### Goals

- Show parent: fee paid by them → where it goes (salary %, infrastructure %, etc.)
- Fee component breakdown: tuition, transport, library, lab, sports — what each funds
- Per-student cost vs fee paid (surplus/deficit indicator)
- Visual: "Your ₹15,000 fee funds: ₹9,300 salary, ₹2,700 infrastructure, ₹1,500 transport, ₹1,500 other"
- Quarterly update with new expense data

### Non-goals

- [ ] School-level financial dashboard (that's `TRANSPARENCY_DASHBOARD_SPEC.md`)
- [ ] Expense management or fee collection (existing modules)
- [ ] Budget planning or forecasting
- [ ] Comparison with other parents' contributions (privacy)
- [ ] Real-time expense tracking (quarterly refresh is sufficient)
- [ ] Profit/loss statement for the school
- [ ] Transaction-level expense detail (aggregated categories only)

### Dependencies

- `TRANSPARENCY_DASHBOARD_SPEC.md` — school-level transparency (public)
- `FEE_PAYMENT_SPEC.md` — fee structures with components (tuition, transport, etc.)
- `EXPENSE_MANAGEMENT_SPEC.md` — expense categories
- `StudentsTable` — total student count for per-student calculation

### Related Modules

- `server/.../feature/transparency/` — transparency module (shared with dashboard)
- `server/.../feature/fees/` — fee payment module
- `server/.../feature/expense/` — expense management module
- `composeApp/.../ui/v2/screens/parent/` — parent UI

---

## 2. Current System Assessment

### Existing Code

- `TRANSPARENCY_DASHBOARD_SPEC.md` — school-level transparency (public)
- `FEE_PAYMENT_SPEC.md` — fee structures with components (tuition, transport, etc.)
- `EXPENSE_MANAGEMENT_SPEC.md` — expense categories
- This spec is the parent-facing, personalized view

### Existing Database

- `FeeRecordsTable` + `PaymentsTable` (from `FEE_PAYMENT_SPEC.md`) — parent's fee paid
- `ExpensesTable` (from `EXPENSE_MANAGEMENT_SPEC.md`) — school expenses by category
- `StudentsTable` — total student count for per-student calculation
- `FeeComponentsTable` — fee component definitions (tuition, transport, library, lab, sports)
- No new tables needed — all data aggregated from existing tables

### Existing APIs

- Fee payment API (existing) — parent's fee payment history
- Expense management API (existing) — expense data by category
- Transparency dashboard API (existing) — school-level transparency data
- No parent fee-expense transparency API exists

### Existing UI

- Fee payment screens (existing) — fee payment history
- Transparency dashboard (existing) — school-level dashboard
- No parent fee-expense transparency UI exists

### Existing Services

- `FeeService` — fee collection and component data
- `ExpenseService` — expense aggregation by category
- `TransparencyService` — school-level transparency dashboard
- No fee-expense transparency service

### Existing Documentation

- `DIFFERENTIATING_FEATURES.md` §9.3 — Fee-Expense Transparency
- `TRANSPARENCY_DASHBOARD_SPEC.md` — public transparency dashboard
- `FEE_PAYMENT_SPEC.md` — fee payment
- `EXPENSE_MANAGEMENT_SPEC.md` — expense management

### Technical Debt

| # | Gap | Details |
|---|---|---|
| TD-1 | No fee-to-expense mapping | No service to map fee components to expense categories |
| TD-2 | No parent personalized view | No parent-facing API or UI for personalized fee transparency |
| TD-3 | No proportional allocation | No logic to calculate how parent's fee maps to expenses |

### Gaps

| # | Gap | Impact | Severity |
|---|---|---|---|
| G1 | No fee-to-expense mapping | Can't show how fee components fund expenses | **High** |
| G2 | No parent personalized view | Parents can't see their own contribution mapping | **High** |
| G3 | No proportional allocation | Can't calculate "your fee funds..." breakdown | **Medium** |
| G4 | No per-student cost comparison | Can't show surplus/deficit indicator | **Medium** |
| G5 | No quarterly refresh | Data may be stale | **Low** |

---

## 3. Functional Requirements

### FR-001
| Field | Value |
|---|---|
| **Title** | Show Parent's Fee Paid |
| **Description** | Show parent's fee paid (total for academic year). |
| **Priority** | High |
| **User Roles** | Parent |
| **Acceptance notes** | Aggregated from FeeRecordsTable + PaymentsTable for the parent's child for the academic year. Shows total_fee_paid. |

### FR-002
| Field | Value |
|---|---|
| **Title** | Map Fee Components to Expense Categories |
| **Description** | Map fee components to expense categories (tuition → salary+academic, transport → transport expenses, etc.). |
| **Priority** | High |
| **User Roles** | System |
| **Acceptance notes** | Predefined mapping table. Tuition → Salary, Academic supplies, Utilities. Transport → Transport. Library → Library books, Library supplies. Lab → Lab equipment, Lab supplies. Sports → Sports equipment, Sports events. Infrastructure → Infrastructure, Maintenance. |

### FR-003
| Field | Value |
|---|---|
| **Title** | Visual Breakdown — "Your Fee Funds..." |
| **Description** | Visual breakdown: "Your fee funds..." with proportional allocation. |
| **Priority** | High |
| **User Roles** | Parent |
| **Acceptance notes** | Shows how parent's fee is proportionally allocated to expense categories. E.g., "Your ₹15,000 fee funds: ₹9,300 salary, ₹2,700 infrastructure, ₹1,500 transport, ₹1,500 other." |

### FR-004
| Field | Value |
|---|---|
| **Title** | Per-Student Cost Comparison |
| **Description** | Per-student cost vs fee paid comparison. |
| **Priority** | Medium |
| **User Roles** | Parent |
| **Acceptance notes** | Per-student cost = total expenses / total students. Shows parent's fee as percentage of per-student cost. "Your fee covers 35.7% of per-student cost." |

### FR-005
| Field | Value |
|---|---|
| **Title** | Quarterly Auto-Refresh |
| **Description** | Quarterly auto-refresh with latest expense data. |
| **Priority** | Low |
| **User Roles** | System |
| **Acceptance notes** | Data aggregated on-demand from live tables. Quarterly job can pre-compute and cache for performance. Always reflects current expense data. |

### FR-006
| Field | Value |
|---|---|
| **Title** | Disclaimer Display |
| **Description** | Disclaimer: "Figures are approximate and based on school averages." |
| **Priority** | Medium |
| **User Roles** | Parent |
| **Acceptance notes** | Disclaimer always displayed. Figures are proportional estimates, not exact accounting. Based on school-wide average allocation. |

### Non-Functional Requirements

| ID | Requirement |
|---|---|
| NFR-1 | API response: < 3 seconds |
| NFR-2 | Fee-to-expense mapping: < 1 second |
| NFR-3 | Per-student cost calculation: < 1 second |
| NFR-4 | Data freshness: current academic year, updated on-demand |
| NFR-5 | Disclaimer always visible in UI |

---

## 4. User Stories

### Parent
- [ ] View my fee paid total for the academic year
- [ ] See fee breakdown by component (tuition, transport, library, etc.)
- [ ] See "Your fee funds..." visualization with proportional allocation
- [ ] See per-student cost and my contribution percentage
- [ ] See disclaimer about approximate figures

### System
- [ ] Aggregate parent's fee paid from FeeRecordsTable + PaymentsTable
- [ ] Get fee breakdown by component
- [ ] Get school's total expenses by category
- [ ] Map fee components to expense categories
- [ ] Calculate proportional allocation
- [ ] Calculate per-student cost (total expenses / total students)
- [ ] Compare fee paid vs per-student cost

---

## 5. Business Rules

### BR-001
**Rule:** Fee-to-expense mapping is predefined.
**Enforcement:** Fixed mapping table (see section 6). Each fee component maps to one or more expense categories. Mapping is not configurable by admin (standardized).

### BR-002
**Rule:** Proportional allocation based on school-wide expense ratios.
**Enforcement:** Parent's fee is allocated to expense categories based on the school's overall expense distribution. E.g., if 62% of school expenses are salary, then 62% of parent's fee is shown as funding salary.

### BR-003
**Rule:** Per-student cost = total expenses / total students.
**Enforcement:** Total annual expenses divided by total enrolled students. Shows average cost per student. Parent's fee compared as percentage.

### BR-004
**Rule:** Figures are approximate.
**Enforcement:** All figures are proportional estimates based on school averages. Not exact accounting. Disclaimer always displayed.

### BR-005
**Rule:** Data is school-scoped and academic-year-scoped.
**Enforcement:** All data filtered by school_id and academic_year_id. No cross-school or cross-year data.

### BR-006
**Rule:** Only parent can view their own fee transparency.
**Enforcement:** Parent API returns only the authenticated parent's children's data. No access to other parents' fee data.

### BR-007
**Rule:** Fee component breakdown from fee structure.
**Enforcement:** Fee breakdown comes from the fee structure assigned to the student (tuition, transport, library, lab, sports, infrastructure components).

### BR-008
**Rule:** Quarterly refresh for cached data.
**Enforcement:** On-demand aggregation always available. Quarterly pre-compute job caches data for performance. Cache invalidated on new expense or fee data.

---

## 6. Database Design

### 6.1 Entity Relationship Summary

No new tables — all data aggregated from existing tables:
- `FeeRecordsTable` + `PaymentsTable` (from `FEE_PAYMENT_SPEC.md`) — parent's fee paid
- `ExpensesTable` (from `EXPENSE_MANAGEMENT_SPEC.md`) — school expenses by category
- `StudentsTable` — total student count for per-student calculation

### 6.2 New Tables

N/A — no new tables. All data is aggregated from existing tables.

### 6.3 Modified Tables

N/A — no existing tables modified.

### 6.4 Indexes

N/A — uses existing indexes on fee_records, payments, expenses tables.

### 6.5 Constraints

N/A — no new constraints.

### 6.6 Foreign Keys

N/A — no new foreign keys.

### 6.7 Soft Delete Strategy

N/A — no new tables. Data is read-only aggregation.

### 6.8 Audit Fields

N/A — no new tables. Views are logged via audit logging.

### 6.9 Migration Notes

No DB migration needed. No new tables. All data from existing tables.

### 6.10 Exposed Mappings

N/A — no new Exposed table mappings.

### 6.11 Seed Data

N/A — no new tables. Fee-to-expense mapping is hardcoded in service.

### Fee Component → Expense Category Mapping

| Fee Component | Expense Categories |
|---|---|
| Tuition | Salary, Academic supplies, Utilities |
| Transport | Transport |
| Library | Library books, Library supplies |
| Lab | Lab equipment, Lab supplies |
| Sports | Sports equipment, Sports events |
| Infrastructure | Infrastructure, Maintenance |

---

## 7. State Machines

### Fee Transparency Data State Machine

```
not_cached ──parent_requests──> aggregating ──complete──> cached
  │                                                        │
  │                                                        └──quarterly_job──> cached (refreshed)
  │                                                        └──new_expense_data──> not_cached (invalidated)
  │
  └──cached ──parent_requests──> cached (served from cache)
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `not_cached` | Parent requests view | `aggregating` | On-demand calculation |
| `aggregating` | Calculation complete | `cached` | Data stored in cache |
| `cached` | Parent requests view | `cached` | Served from cache (within TTL) |
| `cached` | Quarterly pre-compute job | `cached` | Cache refreshed with latest data |
| `cached` | New expense/fee data | `not_cached` | Cache invalidated |
| `cached` | Cache TTL expired | `not_cached` | Cache expired, re-aggregate on next request |

### Parent View State Machine

```
not_viewed ──parent_opens──> viewing ──parent_closes──> viewed
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `not_viewed` | Parent opens fee transparency | `viewing` | Parent navigates to screen |
| `viewing` | Parent closes screen | `viewed` | Parent navigates away |
| `viewed` | Parent reopens | `viewing` | Parent returns to screen |

---

## 8. Backend Architecture

### 8.1 Component Overview

`FeeExpenseTransparencyService` aggregates parent's fee paid, fee breakdown by component, school expenses by category, maps fee components to expense categories, calculates proportional allocation, and per-student cost. `TransparencyRouting` exposes the parent endpoint.

### 8.2 Design Principles

1. **Personalized** — shows parent's own fee contribution, not school-level data
2. **Proportional allocation** — fee mapped to expenses based on school-wide ratios
3. **Approximate, not exact** — figures are estimates based on averages (disclaimer required)
4. **No new tables** — all data aggregated from existing fee, expense, student tables
5. **On-demand + cached** — aggregated on request, cached for performance
6. **Parent-only access** — only parent can view their own children's data

### 8.3 Core Types

#### FeeExpenseTransparencyService

```kotlin
class FeeExpenseTransparencyService {
    suspend fun getParentView(parentId: UUID, studentId: UUID, academicYearId: UUID): FeeExpenseDto {
        // 1. Get total fee paid by parent for this student this year
        // 2. Get fee breakdown by component (tuition, transport, etc.)
        // 3. Get school's total expenses by category
        // 4. Map fee components to expense categories
        // 5. Calculate proportional allocation
        // 6. Calculate per-student cost (total expenses / total students)
        // 7. Compare fee paid vs per-student cost
    }
}
```

### 8.4 Repositories

- Uses existing `FeeRepository` — fee payment data
- Uses existing `ExpenseRepository` — expense data by category
- Uses existing `StudentRepository` — student count

### 8.5 Mappers

- `FeeExpenseMapper` — maps aggregated data to `FeeExpenseDto`

### 8.6 Permission Checks

- Parent endpoint: parent role + JWT auth
- Parent can only view their own children's data (verified via parent-student relationship)
- No admin, teacher, or public access to parent-specific fee transparency

### 8.7 Background Jobs

- **Quarterly Pre-Compute Job** — quarterly (1st of Jan/Apr/Jul/Oct, 3 AM IST)
  1. For each school with transparency enabled:
  2. For each parent with fee payments:
  3. Pre-compute fee-expense transparency data
  4. Cache result (3-month TTL)
  5. Return count of pre-computed views

### 8.8 Domain Events

- `FeeTransparencyViewed` — emitted when parent views fee transparency (analytics)
- `FeeTransparencyPreComputed` — emitted by quarterly job (logged)

### 8.9 Caching

- Parent view: cached per parent + student + academic_year, 1-hour TTL (on-demand) or 3-month TTL (pre-computed)
- Cache invalidated on new fee payment or expense data

### 8.10 Transactions

- Read-only: no transactions needed. Multiple read queries for aggregation.

### 8.11 Rate Limiting

N/A — parent-only endpoint, standard JWT-based limits.

### 8.12 Configuration

- `FEE_EXPENSE_TRANSPARENCY_ENABLED` — default `true`; enable/disable feature
- `FEE_EXPENSE_TRANSPARENCY_CACHE_TTL_HOURS` — default `1`; cache TTL for on-demand
- `FEE_EXPENSE_TRANSPARENCY_PRECOMPUTE_ENABLED` — default `true`; enable quarterly pre-compute

### 8.13 Fee Component → Expense Category Mapping

| Fee Component | Expense Categories |
|---|---|
| Tuition | Salary, Academic supplies, Utilities |
| Transport | Transport |
| Library | Library books, Library supplies |
| Lab | Lab equipment, Lab supplies |
| Sports | Sports equipment, Sports events |
| Infrastructure | Infrastructure, Maintenance |

---

## 9. API Contracts

### 9.1 Parent Endpoint

```
GET /api/v1/parent/fee-transparency/{childId}?academic_year_id={uuid}
  → 200: FeeExpenseDto
  → 403: Not parent of this child
  → 404: Child or academic year not found
```

### 9.2 Response Body

```json
{
  "success": true,
  "data": {
    "total_fee_paid": 15000,
    "fee_breakdown": [
      {"component": "Tuition", "amount": 12000},
      {"component": "Transport", "amount": 2000},
      {"component": "Library", "amount": 1000}
    ],
    "your_fee_funds": [
      {"category": "Teacher Salaries", "amount": 7440, "percentage": 49.6},
      {"category": "Infrastructure", "amount": 2700, "percentage": 18},
      {"category": "Transport", "amount": 2000, "percentage": 13.3},
      {"category": "Academic Supplies", "amount": 1860, "percentage": 12.4},
      {"category": "Utilities", "amount": 1000, "percentage": 6.7}
    ],
    "per_student_cost": 42000,
    "your_contribution_vs_cost": "Your fee covers 35.7% of per-student cost. Remaining funded by other sources.",
    "disclaimer": "Figures are approximate and based on school averages."
  }
}
```

### 9.3 DTO Models

All `@Serializable`, wrapped in `ApiResponse<T>` pattern.

```kotlin
@Serializable data class FeeExpenseDto(
    val totalFeePaid: Long,
    val feeBreakdown: List<FeeComponentDto>,
    val yourFeeFunds: List<FeeFundsDto>,
    val perStudentCost: Long,
    val yourContributionVsCost: String,
    val disclaimer: String,
)

@Serializable data class FeeComponentDto(
    val component: String,
    val amount: Long,
)

@Serializable data class FeeFundsDto(
    val category: String,
    val amount: Long,
    val percentage: Float,
)
```

---

## 10. Frontend Architecture

### 10.1 Screens

| Screen | Platform | Role | Description |
|---|---|---|---|
| `FeeTransparencyScreen` | Compose | Parent | Parent fee transparency view with breakdown visualization |

### 10.2 Navigation

- Parent: Home → Fee Transparency → Select Child → View

### 10.3 UX Flows

#### Parent: View Fee Transparency
1. Parent opens Fee Transparency
2. Select child (if multiple)
3. See total fee paid for academic year
4. See fee breakdown by component (tuition, transport, etc.)
5. See "Your fee funds..." visualization (pie/donut chart with proportional allocation)
6. See per-student cost and contribution percentage
7. See disclaimer: "Figures are approximate and based on school averages."

### 10.4 State Management

```kotlin
data class FeeTransparencyState(
    val transparency: FeeExpenseDto?,
    val isLoading: Boolean,
    val error: String?,
    val selectedChildId: String?,
)
```

### 10.5 Offline Support

- Fee transparency data cached locally (last fetched, 1-hour TTL)

### 10.6 Loading States

- "Loading fee transparency data..."

### 10.7 Error Handling (UI)

- No fee data: "No fee payment data found for this academic year."
- No expense data: "Expense data not yet available. Please check back later."
- Network error: "Connection issue. Please check your internet."
- Not parent of child: "You don't have access to this child's data."

### 10.8 Component Integration Guidelines

| Rule | Description |
|---|---|
| **R1** | Total fee paid: large stat card with ₹ amount |
| **R2** | Fee breakdown: list of components with amounts |
| **R3** | "Your fee funds...": donut/pie chart with category labels, amounts, percentages |
| **R4** | Per-student cost: stat card with comparison bar (fee paid vs cost) |
| **R5** | Contribution text: "Your fee covers X% of per-student cost." |
| **R6** | Disclaimer: always visible at bottom, italic/grey text |
| **R7** | Currency: Indian Rupee (₹) formatting with lakhs/crores |
| **R8** | Charts: use Compose Charts library (or similar) |
| **R9** | Color coding: green for fee paid, blue for allocation, amber for cost |

---

## 11. Shared Module Changes (KMP)

### 11.1 DTOs

All DTOs defined in section 9.3, placed in `shared/.../transparency/domain/model/FeeExpenseModels.kt`.

### 11.2 Domain Models

```kotlin
data class FeeExpense(
    val totalFeePaid: Long,
    val feeBreakdown: List<FeeComponent>,
    val yourFeeFunds: List<FeeFunds>,
    val perStudentCost: Long,
    val yourContributionVsCost: String,
    val disclaimer: String,
)

data class FeeComponent(
    val component: String,
    val amount: Long,
)

data class FeeFunds(
    val category: String,
    val amount: Long,
    val percentage: Float,
)
```

### 11.3 Repository Interfaces

```kotlin
interface FeeExpenseTransparencyRepository {
    suspend fun getParentView(token: String, childId: String, academicYearId: String?): NetworkResult<FeeExpenseDto>
}
```

### 11.4 UseCases

- `GetFeeTransparencyUseCase`

### 11.5 Validation

- `childId`: valid UUID
- `academic_year_id`: valid UUID (optional, defaults to current)

### 11.6 Serialization

Standard Kotlinx serialization. Monetary values as Long (paise internally, displayed as rupees).

### 11.7 Network APIs

Ktor `@Resource` route definitions in `FeeExpenseTransparencyApi.kt`:
- GET `/api/v1/parent/fee-transparency/{childId}?academic_year_id={academicYearId}`

### 11.8 Database Models (Local Cache)

- Fee transparency data cached in local DB (last fetched, 1-hour TTL)

---

## 12. Permissions Matrix

| Action | Super Admin | School Admin | Counselor | Teacher | Parent |
|---|---|---|---|---|---|
| View own child's fee transparency | N/A | N/A | N/A | N/A | ✅ (own children only) |
| View other parents' fee transparency | ✅ | ❌ | ❌ | ❌ | ❌ |
| Configure fee-expense mapping | ✅ | ❌ | ❌ | ❌ | ❌ |

---

## 13. Notifications

### Fee-Expense Transparency Notifications

| Trigger | Recipient | Channel | Template |
|---|---|---|---|
| Quarterly transparency update | Parent | in-app | "Fee transparency data has been updated for this quarter." |

### Notification Integration

Uses existing `Notify.kt` dispatch. Notifications created with category "fee_transparency".

---

## 14. Background Jobs

### Quarterly Pre-Compute Job

| Property | Value |
|---|---|
| **Name** | `FeeTransparencyPreComputeJob` |
| **Schedule** | Quarterly (1st of Jan/Apr/Jul/Oct, 3 AM IST) |
| **Duration** | < 10 minutes for 500 parents |
| **Retry** | None (next quarter) |

#### Job Flow

1. For each school with transparency enabled:
2. For each parent with fee payments in current academic year:
3. Pre-compute fee-expense transparency data
4. Cache result (3-month TTL)
5. Return count of pre-computed views

---

## 15. Integrations

### Internal Integrations

| System | Integration Point | Direction | Protocol | Error Handling |
|---|---|---|---|---|
| `FeeService` / `FeeRepository` | Fee payment data | Read | Direct DB | Return 0 if no data |
| `ExpenseService` / `ExpenseRepository` | Expense data by category | Read | Direct DB | Return 0 if no data |
| `StudentRepository` | Student count | Read | Direct DB | Return 0 if no students |
| `TransparencyService` | Transparency config | Read | Direct DB | Default config if not found |
| `Notify.kt` | Notifications | Call | Direct call | Log on failure |

### External Integrations

N/A — no external integrations. All data is internal.

### Integration Patterns

- **Fee paid:** `feeRepository.getTotalPaidForStudent(studentId, academicYearId)` — aggregated sum
- **Fee breakdown:** `feeRepository.getFeeBreakdownForStudent(studentId, academicYearId)` — by component
- **Expenses:** `expenseRepository.getExpensesByCategory(schoolId, academicYearId)` — grouped by category
- **Student count:** `studentRepository.countEnrolled(schoolId, academicYearId)`
- **Notifications:** `Notify.kt` called with category "fee_transparency"

---

## 16. Security

### Authentication

- Parent endpoint: parent role + JWT auth via `requireAuth()`
- Parent can only view their own children's data (verified via parent-student relationship)

### Authorization

- Only parent can view their own children's fee transparency
- No access to other parents' data
- No public access (this is personalized, unlike the public dashboard)

### Data Protection

- Shows parent's own fee data (not other parents')
- No PII in aggregated expense data (school-wide totals)
- Per-student cost is an average (no individual student cost)

### Input Validation

- `childId`: valid UUID
- `academic_year_id`: valid UUID (optional)

### Rate Limiting

N/A — parent-only endpoint, standard JWT-based limits.

### Audit Logging

- Fee transparency viewed: parent ID, student ID, academic year, timestamp

### PII Handling

- Parent's own fee data is shown to them (not PII exposure)
- No other parents' data accessible
- No student names in aggregated expense data

### Multi-tenant Isolation

- All data filtered by school_id and academic_year_id
- No cross-school data access

---

## 17. Performance & Scalability

### Expected Scale

- 500-2,000 parents per school
- 1-3 children per parent
- On-demand aggregation: fee + expense queries
- Quarterly pre-compute: 500 parents × aggregation

### Query Optimization

- Fee paid: SUM on payments table, indexed by student_id + academic_year_id
- Fee breakdown: GROUP BY component, indexed by student_id
- Expenses: SUM GROUP BY category, indexed by school_id + academic_year_id
- Student count: COUNT, indexed by school_id + academic_year_id

### Indexing Strategy

- Existing indexes on fee_records, payments, expenses tables (school_id, academic_year_id, student_id)

### Caching Strategy

- Parent view: cached per parent + student + academic_year, 1-hour TTL (on-demand) or 3-month TTL (pre-computed)
- Cache invalidated on new fee payment or expense data

### Pagination

N/A — single response with all transparency data.

### Connection Pooling

Uses existing HikariCP connection pool. No additional pooling needed.

### Async Processing

- Quarterly pre-compute job: background job (async)
- On-demand aggregation: synchronous (with cache)
- Notifications: async (fire-and-forget via `Notify.kt`)

### Scalability Concerns

- Aggregation queries: SUM on existing tables. Pre-compute job reduces on-demand load.
- Cache: 3-month TTL for pre-computed, 1-hour for on-demand. Reduces DB load.
- Minimal DB storage (no new tables, cached results only).

---

## 18. Edge Cases

| # | Scenario | Expected Behavior |
|---|---|---|
| EC-1 | Parent has no fee payments | total_fee_paid = 0, fee_breakdown empty, your_fee_funds empty |
| EC-2 | School has no expense data | your_fee_funds empty, per_student_cost = 0, disclaimer shown |
| EC-3 | School has no students | per_student_cost = 0 (division by zero handled), contribution text: "N/A" |
| EC-4 | Fee paid > per-student cost | Contribution > 100%, text: "Your fee covers X% (exceeds per-student cost)." |
| EC-5 | Parent has multiple children | Parent selects child, view shows that child's fee data |
| EC-6 | Parent not linked to child | Return 403 "You don't have access to this child's data." |
| EC-7 | Academic year not found | Return 404 "Academic year not found." |
| EC-8 | Fee component not in mapping table | Component shown in fee_breakdown but not mapped to expenses |
| EC-9 | All expenses in one category | your_fee_funds shows single category at 100% |
| EC-10 | Fee paid is 0 but expenses exist | total_fee_paid = 0, your_fee_funds empty, per_student_cost shown |

---

## 19. Error Handling

### Error Response Format

Standard `ApiResponse` error format.

### Error Codes

| Code | HTTP Status | Description | User Message |
|---|---|---|---|
| `NOT_PARENT_OF_CHILD` | 403 | Parent not linked to child | "You don't have access to this child's data." |
| `CHILD_NOT_FOUND` | 404 | Child ID not found | "Child not found." |
| `ACADEMIC_YEAR_NOT_FOUND` | 404 | Academic year not found | "Academic year not found." |
| `NO_FEE_DATA` | 200 | No fee payments found | (Empty data in response, not an error) |
| `NO_EXPENSE_DATA` | 200 | No expense data found | (Empty your_fee_funds in response, not an error) |

### Error Handling Strategy

- **Parent access:** Return 403 if parent not linked to child.
- **Missing data:** Return 200 with empty fields (not an error). Show appropriate UI message.
- **Division by zero:** Handle gracefully — per_student_cost = 0, contribution = "N/A".
- **Pre-compute job:** Log error per parent. Continue with next.

### Retry Strategy

- Fee transparency fetch: client retries on network error (3 attempts)
- Pre-compute job: no retry (next quarter's job handles it)

### Fallback Behavior

- No fee data: total_fee_paid = 0, empty breakdown
- No expense data: empty your_fee_funds, per_student_cost = 0
- No students: per_student_cost = 0, contribution = "N/A"

---

## 20. Analytics & Reporting

### Analytics Dashboard Data

| Metric | Source | Derivation |
|---|---|---|
| Fee transparency views | Audit logs | Count of views per school per month |
| Unique parents viewing | Audit logs | Distinct parent IDs viewing per month |
| Average fee paid | Fee data | Avg total_fee_paid across parents |
| Average per-student cost | Expense data | Avg per_student_cost across schools |
| Average contribution % | Calculated | Avg (fee_paid / per_student_cost * 100) |

### Export Capabilities

- Fee transparency export (CSV) — parent, fee paid, breakdown, allocation, per-student cost

### Report Types

| Report | Format | Frequency | Recipient |
|---|---|---|---|
| Fee transparency views | JSON (API) | Monthly | Product Team |
| Parent engagement | JSON (API) | Monthly | Product Team |

---

## 21. Testing Strategy

### Unit Tests

- `FeeExpenseTransparencyService.getParentView()` — aggregation, mapping, proportional allocation
- Fee-to-expense mapping — verify all components map correctly
- Proportional allocation — verify percentage calculations
- Per-student cost — verify division, handle zero students
- Contribution percentage — verify calculation

### Integration Tests

- Full flow: set up fee payments + expenses → get parent view → verify response
- No fee data: verify empty fields, 200 response
- No expense data: verify empty your_fee_funds, 200 response
- Parent not linked to child: verify 403
- Multiple children: verify parent can view each child's data
- Pre-compute job: run job → verify cache populated

### E2E Tests

- Parent views fee transparency → sees breakdown and allocation
- Parent switches between children → sees different data
- Quarterly update → data refreshed

### Performance Tests

- API response: < 3 seconds
- Aggregation query: < 5 seconds (large school with 2,000 students)
- Pre-compute job: < 10 minutes for 500 parents

### Test Data

- 5 parents with varying fee payments (full, partial, none)
- Fee structures with multiple components (tuition, transport, library, lab, sports)
- Expenses by category (Salary, Infrastructure, Transport, Utilities, etc.)
- Students with and without fee payments
- Parent-child relationships (1 child, multiple children)

### Test Environment

- Test database with fee, expense, student tables
- Pre-seeded fee structures and expense data
- Test JWT tokens for parent role
- Mock parent-child relationships

---

## 22. Acceptance Criteria

- [ ] Parent sees their fee paid total
- [ ] Fee breakdown by component shown
- [ ] "Your fee funds..." visualization with proportional allocation
- [ ] Per-student cost comparison
- [ ] Quarterly auto-refresh
- [ ] Disclaimer displayed
- [ ] Parent can only view own children's data
- [ ] Empty data handled gracefully (no errors)

---

## 23. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 2 days | `FeeExpenseTransparencyService` (aggregation + mapping + proportional allocation) |
| 2 | 1 day | API endpoint (add to `TransparencyRouting`) |
| 3 | 2 days | Client UI: `FeeTransparencyScreen` (breakdown visualization, comparison, disclaimer) |
| 4 | 1 day | Tests: unit, integration, E2E |

### Pre-Implementation Checklist

- [ ] Verify `EXPENSE_MANAGEMENT_SPEC.md` is implemented and expense data available
- [ ] Verify `FEE_PAYMENT_SPEC.md` is implemented and fee data available
- [ ] Verify `TRANSPARENCY_DASHBOARD_SPEC.md` is implemented
- [ ] Verify fee components exist in fee structure (tuition, transport, library, lab, sports)
- [ ] Verify parent-student relationship exists in DB
- [ ] Verify chart library available for Compose UI

---

## 24. File-Level Impact Analysis

### Server

| File | Change Type | Description |
|---|---|---|
| `server/.../feature/transparency/FeeExpenseTransparencyService.kt` | **New** | Core service (aggregation, mapping, proportional allocation) |
| `server/.../feature/transparency/TransparencyRouting.kt` | Modify | Add parent fee-transparency endpoint |
| `server/.../feature/transparency/FeeTransparencyPreComputeJob.kt` | **New** | Quarterly pre-compute job |

### Shared (KMP)

| File | Change Type | Description |
|---|---|---|
| `shared/.../transparency/domain/model/FeeExpenseModels.kt` | **New** | DTOs, domain models |
| `shared/.../transparency/domain/repository/FeeExpenseTransparencyRepository.kt` | **New** | Repository interface |
| `shared/.../transparency/data/remote/FeeExpenseTransparencyApi.kt` | **New** | HTTP API definitions |

### Client (Compose)

| File | Change Type | Description |
|---|---|---|
| `composeApp/.../ui/v2/screens/parent/FeeTransparencyScreen.kt` | **New** | Parent fee transparency view |

---

## 25. Future Enhancements

| # | Enhancement | Priority | Effort | Notes |
|---|---|---|---|---|
| F-1 | Component-level expense mapping | Medium | M | Show exactly what each fee component funds (not just proportional) |
| F-2 | Historical year comparison | Low | S | Compare current year vs previous year fee transparency |
| F-3 | Fee utilization trend over time | Low | M | Show how fee utilization has changed over quarters |
| F-4 | Downloadable fee transparency report (PDF) | Medium | S | Parent can download personalized report |
| F-5 | Multi-child aggregated view | Low | S | Show combined fee transparency for all children |
| F-6 | Real-time fee transparency | Low | M | Update on new fee payment immediately |
| F-7 | Fee component customization | Low | M | Admin can customize fee-to-expense mapping |
| F-8 | Surplus/deficit visualization | Low | S | Visual indicator for fee vs cost surplus/deficit |
| F-9 | Expense sub-category breakdown | Low | S | Drill down from category to sub-categories |
| F-10 | Comparison with school average fee | Low | S | Show parent's fee vs school average fee |

---

## Appendix A: Sequence Diagrams

### A.1 Parent Fee Transparency View Flow

```
Parent (app)         Server                          DB
  │                    │                               │
  │  GET /fee-trans    │                               │
  │  /{childId}        │                               │
  │  ?academic_year=.. │                               │
  │  ───────────────>  │                               │
  │                    │──verify parent-child link────>│
  │                    │←──relationship confirmed──────│
  │                    │                               │
  │                    │──get fee paid for student────>│
  │                    │←──total paid + breakdown──────│
  │                    │                               │
  │                    │──get expenses by category────>│
  │                    │←──category amounts────────────│
  │                    │                               │
  │                    │──get student count────────────>│
  │                    │←──total enrolled──────────────│
  │                    │                               │
  │                    │  map fee → expenses           │
  │                    │  calculate proportional alloc │
  │                    │  calculate per-student cost   │
  │                    │  build DTO                    │
  │                    │                               │
  │  ←──200: FeeDto────│                               │
  │                    │                               │
```

---

## Appendix B: Domain Model / ER Diagram

```
No new tables. All data aggregated from existing tables:

┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│ fee_records      │  │ payments         │  │ expenses         │
│ (existing)       │  │ (existing)       │  │ (existing)       │
│                  │  │                  │  │                  │
│ components:      │  │ total paid       │  │ categories:      │
│ tuition,         │  │                  │  │ salary, infra,   │
│ transport,       │  │                  │  │ transport,       │
│ library, lab,    │  │                  │  │ utilities,       │
│ sports, infra    │  │                  │  │ academic, etc.   │
└──────────────────┘  └──────────────────┘  └──────────────────┘
┌──────────────────┐
│ students         │
│ (existing)       │
│ count for        │
│ per-student cost │
└──────────────────┘

Fee Component → Expense Category Mapping (in service):
  Tuition      → Salary, Academic supplies, Utilities
  Transport    → Transport
  Library      → Library books, Library supplies
  Lab          → Lab equipment, Lab supplies
  Sports       → Sports equipment, Sports events
  Infrastructure → Infrastructure, Maintenance
```

---

## Appendix C: Event Flow

### Domain Events

| Event | Emitter | Consumers | Payload | Side Effects |
|---|---|---|---|---|
| `FeeTransparencyViewed` | `FeeExpenseTransparencyService.getParentView()` | None (logged) | `parentId, studentId, academicYearId` | Analytics tracking |
| `FeeTransparencyPreComputed` | `FeeTransparencyPreComputeJob` | None (logged) | `schoolId, parentCount` | Cache populated |

### Event Delivery Guarantees

- Fee transparency viewed event: emitted synchronously (fire-and-forget logging)
- Pre-computed event: emitted within job execution

---

## Appendix D: Configuration

### Environment Variables

| Variable | Default | Description |
|---|---|---|
| `FEE_EXPENSE_TRANSPARENCY_ENABLED` | `true` | Enable/disable feature |
| `FEE_EXPENSE_TRANSPARENCY_CACHE_TTL_HOURS` | `1` | Cache TTL for on-demand aggregation |
| `FEE_EXPENSE_TRANSPARENCY_PRECOMPUTE_ENABLED` | `true` | Enable quarterly pre-compute job |

### Feature Flags

| Flag | Default | Description |
|---|---|---|
| `FEE_EXPENSE_TRANSPARENCY_ENABLED` | `true` | Enable/disable fee-expense transparency |

### School-Level Settings

N/A — uses transparency dashboard config (`is_public` setting). Parent fee transparency always available to parents (not public).

---

## Appendix E: Migration & Rollback

### Migration

No DB migration needed. No new tables. All data aggregated from existing tables.

### Rollback

N/A — no DB changes to roll back. Feature can be disabled via feature flag.

### Migration Validation

N/A — no migration to validate.

---

## Appendix F: Observability

### Structured Logging

| Log Level | Event | Context Fields |
|---|---|---|
| INFO | Fee transparency viewed | `parentId, studentId, academicYearId` |
| INFO | Pre-compute job completed | `schoolId, parentCount, duration` |
| WARN | No fee data for parent | `parentId, studentId, academicYearId` |
| WARN | No expense data for school | `schoolId, academicYearId` |
| WARN | No students for school | `schoolId, academicYearId` |
| ERROR | Pre-compute job failed | `schoolId, error` |
| ERROR | Aggregation query failed | `parentId, studentId, error` |

### Metrics

| Metric | Type | Labels | Description |
|---|---|---|---|
| `fee_transparency_views` | Counter | `school_id` | Total fee transparency views |
| `fee_transparency_unique_parents` | Gauge | `school_id` | Unique parents viewing per month |
| `fee_transparency_precompute_duration` | Histogram | `school_id` | Pre-compute job duration |
| `fee_transparency_avg_fee_paid` | Gauge | `school_id` | Average fee paid by parents |
| `fee_transparency_avg_contribution_pct` | Gauge | `school_id` | Average contribution percentage |

### Health Checks

| Check | Endpoint | Description |
|---|---|---|
| Fee transparency service | `/health/fee-transparency` | Verify service and DB accessible |

### Alerts

| Alert | Condition | Severity | Notification |
|---|---|---|---|
| Pre-compute job failed | Job error rate > 10% | Warning | Email to dev team |
| Aggregation slow | Duration > 10 seconds | Warning | Email to dev team |
| Low parent engagement | < 10% parents viewing per quarter | Info | Email to product team |

### Dashboards

| Dashboard | Panels | Audience |
|---|---|---|
| Fee Transparency Overview | Views/month, unique parents, avg fee paid, avg contribution | Product Team |
| Job Performance | Pre-compute duration, error rate | Dev Team |
| Parent Engagement | Views by school, contribution distribution | Product Team |

### Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Parents confused by approximate figures | Medium | Low | Disclaimer always displayed. Clear UI messaging. |
| Incorrect fee-to-expense mapping | Low | Medium | Predefined mapping. Unit tests for mapping. |
| Proportional allocation misleading | Medium | Low | Disclaimer. Based on school averages, not exact. |
| Parent sees other parent's data | Low | High | Parent-child relationship verified. Role-based access. |
| Stale cached data | Medium | Low | 1-hour TTL for on-demand. Cache invalidated on new data. |
| Division by zero (no students) | Low | Low | Handle gracefully — per_student_cost = 0. |
