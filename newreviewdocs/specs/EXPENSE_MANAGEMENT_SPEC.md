# Expense Management — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** None
> **Unblocks:** `FEE_EXPENSE_TRANSPARENCY_SPEC.md`

---

## 1. Feature Overview

School expense tracking: record expenses by category, vendor payments, budget vs actual comparison, and expense reports. Provides the data foundation for fee-expense transparency features.

### Goals

- Admin records expenses (amount, category, vendor, date, receipt)
- Budget setting per category per academic year
- Budget vs actual comparison dashboard
- Vendor payment tracking
- Expense reports (monthly, category-wise, vendor-wise)
- Data feeds into fee-expense transparency ("where does my fee go")

---

## 2. Current System Assessment

- `feature_audit.csv` L136: Expense Management missing (0%)
- No expense tables in `Tables.kt`
- `FeeRecordsTable` exists for income side

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Record expense: amount, category, vendor, date, description, receipt image |
| FR-2 | Categories: Salary, Infrastructure, Transport, Utilities, Supplies, Maintenance, Events, Misc |
| FR-3 | Budget per category per academic year |
| FR-4 | Budget vs actual dashboard with progress bars |
| FR-5 | Vendor management: vendor list, payment history |
| FR-6 | Expense reports: monthly, category-wise, vendor-wise |
| FR-7 | Receipt upload to Supabase Storage |
| FR-8 | Aggregate expense data for fee transparency (total expenses by category) |

---

## 4. Database Design

```sql
CREATE TABLE expense_categories (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    name            TEXT NOT NULL,                 -- "Salary", "Transport", "Infrastructure"
    color           VARCHAR(8),                    -- hex color for charts
    is_active       BOOLEAN NOT NULL DEFAULT true,
    UNIQUE(school_id, name)
);

CREATE TABLE expense_vendors (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    name            TEXT NOT NULL,
    phone           VARCHAR(32),
    email           TEXT,
    gst_number      VARCHAR(16),
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE expense_budgets (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    category_id     UUID NOT NULL REFERENCES expense_categories(id),
    academic_year_id UUID NOT NULL,
    budget_amount   DOUBLE PRECISION NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(school_id, category_id, academic_year_id)
);

CREATE TABLE expenses (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    category_id     UUID NOT NULL REFERENCES expense_categories(id),
    vendor_id       UUID REFERENCES expense_vendors(id),
    amount          DOUBLE PRECISION NOT NULL,
    expense_date    DATE NOT NULL,
    description     TEXT,
    receipt_url     TEXT,                          -- Supabase Storage URL
    payment_mode    VARCHAR(16),                   -- cash | cheque | bank_transfer | upi
    reference_number TEXT,                         -- cheque no, transaction id
    recorded_by     UUID,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_expenses_school_date ON expenses(school_id, expense_date DESC);
CREATE INDEX idx_expenses_category ON expenses(school_id, category_id, expense_date DESC);
```

---

## 5. API Contracts

```
GET/POST /api/v1/school/expenses
GET/POST /api/v1/school/expense-categories
GET/POST /api/v1/school/expense-vendors
GET/POST /api/v1/school/expense-budgets
GET /api/v1/school/expenses/dashboard?academic_year_id={uuid}
GET /api/v1/school/expenses/report?from={}&to={}&group_by=category|vendor|month
```

**Dashboard response:**
```json
{
  "total_budget": 5000000,
  "total_expenses": 3200000,
  "budget_utilization": 0.64,
  "by_category": [
    {"category": "Salary", "budget": 3000000, "actual": 2100000, "utilization": 0.70},
    {"category": "Transport", "budget": 500000, "actual": 420000, "utilization": 0.84}
  ],
  "monthly_trend": [
    {"month": "2026-06", "amount": 450000}
  ]
}
```

---

## 6. Acceptance Criteria

- [ ] Expenses recorded with category, vendor, amount, date, receipt
- [ ] Budget set per category per academic year
- [ ] Budget vs actual dashboard with progress bars
- [ ] Vendor management with payment history
- [ ] Expense reports (monthly, category, vendor)
- [ ] Receipt upload works
- [ ] Aggregate data available for fee transparency

---

## 7. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 2 days | DB migration, Exposed tables |
| 2 | 2 days | ExpenseService + budget service |
| 3 | 2 days | API endpoints + reports |
| 4 | 3 days | Client UI (expense entry, dashboard, reports, vendor management) |
| 5 | 1 day | Tests |

---

## 8. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | 4 expense tables |
| `server/.../feature/expense/*.kt` | New | Services + routing |
| `docs/db/migration_052_expense_management.sql` | New | DDL |
| `composeApp/.../ui/v2/screens/admin/ExpenseScreen.kt` | New | Expense management |
| `composeApp/.../ui/v2/screens/admin/ExpenseDashboardScreen.kt` | New | Budget vs actual dashboard |
