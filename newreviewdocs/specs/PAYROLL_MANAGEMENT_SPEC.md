# Payroll Management — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** None

---

## 1. Feature Overview

Staff payroll management: salary structure, payslip generation, attendance-linked calculation, deductions (PF, ESI, TDS), and payroll reports.

### Goals

- Admin defines salary structure per staff (basic, HRA, allowances, deductions)
- Monthly payroll calculation based on attendance
- Payslip PDF generation
- PF, ESI, TDS deduction support
- Payroll register and bank statement export

---

## 2. Current System Assessment

- `feature_audit.csv` L137: Payroll missing (0%)
- `NonTeachingStaffTable` exists for non-teaching staff
- `FacultyTable` + `AppUsersTable` for teachers
- `AttendanceRecordsTable` for student attendance (not staff)
- `TeacherCheckInsTable` — teacher check-in records (can be used for staff attendance)

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Define salary structure: basic, HRA, conveyance, special allowance, PF, ESI, TDS, professional tax |
| FR-2 | Monthly payroll calculation: gross = basic + allowances; net = gross - deductions |
| FR-3 | Attendance-linked: deductions for unpaid leave |
| FR-4 | Generate payslip PDF per employee per month |
| FR-5 | Payroll register: all employees, monthly summary |
| FR-6 | Bank statement export (CSV with account details for bank transfer) |
| FR-7 | Salary revision history |

---

## 4. Database Design

```sql
CREATE TABLE payroll_structures (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    employee_id     UUID NOT NULL,                 -- FK app_users.id or non_teaching_staff.id
    employee_type   VARCHAR(16) NOT NULL,          -- teacher | non_teaching
    employee_name   TEXT NOT NULL,
    basic_salary    DOUBLE PRECISION NOT NULL,
    hra             DOUBLE PRECISION NOT NULL DEFAULT 0,
    conveyance      DOUBLE PRECISION NOT NULL DEFAULT 0,
    special_allowance DOUBLE PRECISION NOT NULL DEFAULT 0,
    pf_percentage   REAL NOT NULL DEFAULT 0,       -- 12% = 0.12
    esi_percentage  REAL NOT NULL DEFAULT 0,       -- 0.75% = 0.0075
    tds_monthly     DOUBLE PRECISION NOT NULL DEFAULT 0,
    professional_tax DOUBLE PRECISION NOT NULL DEFAULT 0,
    bank_account    VARCHAR(32),
    bank_ifsc       VARCHAR(16),
    is_active       BOOLEAN NOT NULL DEFAULT true,
    effective_from  DATE NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE payroll_runs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    month           INTEGER NOT NULL,              -- 1-12
    year            INTEGER NOT NULL,
    status          VARCHAR(16) NOT NULL DEFAULT 'draft', -- draft | processed | paid
    processed_by    UUID,
    processed_at    TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(school_id, month, year)
);

CREATE TABLE payroll_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payroll_run_id  UUID NOT NULL REFERENCES payroll_runs(id) ON DELETE CASCADE,
    employee_id     UUID NOT NULL,
    employee_name   TEXT NOT NULL,
    basic           DOUBLE PRECISION NOT NULL,
    hra             DOUBLE PRECISION NOT NULL,
    conveyance      DOUBLE PRECISION NOT NULL,
    special_allowance DOUBLE PRECISION NOT NULL,
    gross_salary    DOUBLE PRECISION NOT NULL,
    pf_deduction    DOUBLE PRECISION NOT NULL,
    esi_deduction   DOUBLE PRECISION NOT NULL,
    tds_deduction   DOUBLE PRECISION NOT NULL,
    professional_tax DOUBLE PRECISION NOT NULL,
    unpaid_leave_deduction DOUBLE PRECISION NOT NULL DEFAULT 0,
    net_salary      DOUBLE PRECISION NOT NULL,
    working_days     INTEGER NOT NULL,
    present_days     INTEGER NOT NULL,
    payslip_url      TEXT,
    created_at       TIMESTAMP NOT NULL DEFAULT now()
);
```

---

## 5. API Contracts

```
GET/POST /api/v1/school/payroll/structures
POST /api/v1/school/payroll/run  { month, year }
GET /api/v1/school/payroll/run/{id}
POST /api/v1/school/payroll/run/{id}/process
POST /api/v1/school/payroll/run/{id}/mark-paid
GET /api/v1/school/payroll/run/{id}/payslip/{employeeId}
GET /api/v1/school/payroll/run/{id}/bank-statement
```

---

## 6. Acceptance Criteria

- [ ] Salary structure defined per employee
- [ ] Monthly payroll calculated with all components
- [ ] Attendance-linked deductions
- [ ] Payslip PDF generated
- [ ] Payroll register available
- [ ] Bank statement CSV export

---

## 7. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 2 days | DB migration, Exposed tables |
| 2 | 3 days | PayrollService (calculation, deductions) |
| 3 | 2 days | Payslip PDF generation |
| 4 | 2 days | API endpoints |
| 5 | 3 days | Client UI (salary structure, payroll run, payslip view, register) |
| 6 | 1 day | Tests |

---

## 8. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | 3 payroll tables |
| `server/.../feature/payroll/*.kt` | New | Services + routing |
| `docs/db/migration_048_payroll.sql` | New | DDL |
| `composeApp/.../ui/v2/screens/admin/PayrollScreen.kt` | New | Payroll management |
