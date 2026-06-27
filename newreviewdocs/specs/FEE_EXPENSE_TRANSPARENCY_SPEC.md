# Fee-Expense Transparency — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** `EXPENSE_MANAGEMENT_SPEC.md`, `FEE_PAYMENT_SPEC.md`, `TRANSPARENCY_DASHBOARD_SPEC.md`
> **Source:** `DIFFERENTIATING_FEATURES.md` §9.3

---

## 1. Feature Overview

Parent-facing view that shows exactly how their fee money is utilized: breakdown of fee components, what each component funds, and comparison of their contribution vs per-student cost. More granular than the public transparency dashboard.

### Goals

- Show parent: fee paid by them → where it goes (salary %, infrastructure %, etc.)
- Fee component breakdown: tuition, transport, library, lab, sports — what each funds
- Per-student cost vs fee paid (surplus/deficit indicator)
- Visual: "Your ₹15,000 fee funds: ₹9,300 salary, ₹2,700 infrastructure, ₹1,500 transport, ₹1,500 other"
- Quarterly update with new expense data

---

## 2. Current System Assessment

- `TRANSPARENCY_DASHBOARD_SPEC.md` — school-level transparency (public)
- `FEE_PAYMENT_SPEC.md` — fee structures with components (tuition, transport, etc.)
- `EXPENSE_MANAGEMENT_SPEC.md` — expense categories
- This spec is the parent-facing, personalized view

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Show parent's fee paid (total for academic year) |
| FR-2 | Map fee components to expense categories (tuition → salary+academic, transport → transport expenses, etc.) |
| FR-3 | Visual breakdown: "Your fee funds..." with proportional allocation |
| FR-4 | Per-student cost vs fee paid comparison |
| FR-5 | Quarterly auto-refresh with latest expense data |
| FR-6 | Disclaimer: "Figures are approximate and based on school averages" |

---

## 4. Database Design

No new tables — data aggregated from:
- `FeeRecordsTable` + `PaymentsTable` (from `FEE_PAYMENT_SPEC.md`) — parent's fee paid
- `ExpensesTable` (from `EXPENSE_MANAGEMENT_SPEC.md`) — school expenses by category
- `StudentsTable` — total student count for per-student calculation

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

## 5. Backend Architecture

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

---

## 6. API Contracts

```
GET /api/v1/parent/fee-transparency/{childId}?academic_year_id={uuid}
```

**Response:**
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

---

## 7. Acceptance Criteria

- [ ] Parent sees their fee paid total
- [ ] Fee breakdown by component shown
- [ ] "Your fee funds..." visualization with proportional allocation
- [ ] Per-student cost comparison
- [ ] Quarterly auto-refresh
- [ ] Disclaimer displayed

---

## 8. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 2 days | FeeExpenseTransparencyService (aggregation + mapping) |
| 2 | 1 day | API endpoint |
| 3 | 2 days | Client UI (breakdown visualization, comparison) |
| 4 | 1 day | Tests |

---

## 9. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../feature/transparency/FeeExpenseTransparencyService.kt` | New | Core service |
| `server/.../feature/transparency/TransparencyRouting.kt` | Modify | Add parent endpoint |
| `composeApp/.../ui/v2/screens/parent/FeeTransparencyScreen.kt` | New | Parent fee transparency view |
