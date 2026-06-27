# Bulk Import/Export — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Audience:** Senior engineer / AI agent implementing the system
> **Last updated:** 2026-06-27
> **Prerequisites:** None
> **Unblocks:** Data migration for new schools, `MULTI_BRANCH_SPEC.md`

---

## Table of Contents

1. [Feature Overview](#1-feature-overview)
2. [Current System Assessment](#2-current-system-assessment)
3. [Gap Analysis](#3-gap-analysis)
4. [Functional Requirements](#4-functional-requirements)
5. [User Roles & Permissions](#5-user-roles--permissions)
6. [UX Flow](#6-ux-flow)
7. [Database Design](#7-database-design)
8. [Backend Architecture](#8-backend-architecture)
9. [Frontend Architecture](#9-frontend-architecture)
10. [API Contracts](#10-api-contracts)
11. [Validation Rules](#11-validation-rules)
12. [Error Handling](#12-error-handling)
13. [Edge Cases](#13-edge-cases)
14. [Background Processing](#14-background-processing)
15. [Monitoring](#15-monitoring)
16. [Feature Flags](#16-feature-flags)
17. [Migration Strategy](#17-migration-strategy)
18. [Testing Strategy](#18-testing-strategy)
19. [Acceptance Criteria](#19-acceptance-criteria)
20. [Implementation Roadmap](#20-implementation-roadmap)
21. [File-Level Impact Analysis](#21-file-level-impact-analysis)
22. [Risks & Mitigations](#22-risks--mitigations)

---

## 1. Feature Overview

CSV/Excel import and export for students, teachers, marks, fee structures, and other bulk data operations. Enables schools to migrate from spreadsheets/legacy systems and export data for external use.

### Goals

- Import students from CSV/Excel with validation, dedup, and error reporting
- Import teachers and subject assignments
- Import assessment marks (bulk entry from spreadsheet)
- Import fee structures
- Export any data category to CSV/Excel
- Template downloads with correct column headers
- Row-level error reporting with line numbers
- Dry-run mode (validate without committing)
- Progress tracking for large imports

---

## 2. Current System Assessment

### 2.1 What Exists

- **Onboarding wizard** has inline bulk entry for classes/subjects (`feature_audit.csv` L159: "Onboarding wizard has bulk entry, no CSV import/export")
- `SchoolClassesTable`, `SchoolSubjectsTable`, `StudentsTable`, `TeacherSubjectAssignmentsTable` — all support bulk creation via existing routes
- `SupabaseStorage.kt` — can store uploaded CSV files
- `AssessmentsTable` + `AssessmentMarksTable` — marks entry exists but one-at-a-time
- No CSV parsing library, no import/export endpoints, no template generation

### 2.2 What's Missing

- No file upload for CSV/Excel
- No CSV parsing/validation
- No template downloads
- No export endpoints
- No import progress tracking
- No error reporting with row-level detail

---

## 3. Gap Analysis

| # | Gap | Impact |
|---|---|---|
| G1 | No CSV import for students | Schools must enter students one-by-one |
| G2 | No CSV import for teachers | Same for teacher onboarding |
| G3 | No bulk marks import | Teachers enter marks one student at a time |
| G4 | No export | Data lock-in; cannot use in other systems |
| G5 | No template downloads | Users don't know required format |
| G6 | No error reporting | Silent failures, data corruption |

---

## 4. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Import students from CSV (name, roll_number, class, section, parent_phone, gender, DOB) |
| FR-2 | Import teachers from CSV (name, email, phone, subjects, classes) |
| FR-3 | Import marks from CSV (student_code, subject, assessment_name, marks, max_marks) |
| FR-4 | Import fee structures from CSV (class, category, amount, due_date, installments) |
| FR-5 | Export students, teachers, marks, attendance, fees to CSV |
| FR-6 | Download import templates (CSV with correct headers + sample row) |
| FR-7 | Dry-run mode: validate all rows, report errors, don't commit |
| FR-8 | Row-level error reporting (line number, field, error message) |
| FR-9 | Progress tracking for imports > 100 rows |
| FR-10 | Duplicate detection (by student_code, phone, email) |
| FR-11 | Update existing records if match found (configurable: upsert vs skip) |
| FR-12 | Support both CSV and Excel (.xlsx) formats |

---

## 5. User Roles & Permissions

| Action | School Admin | Teacher | Parent |
|---|---|---|---|
| Import students | ✅ | ❌ | ❌ |
| Import teachers | ✅ | ❌ | ❌ |
| Import marks | ✅ | ✅ (own class) | ❌ |
| Import fee structures | ✅ | ❌ | ❌ |
| Export students | ✅ | ❌ | ❌ |
| Export marks | ✅ | ✅ (own class) | ❌ |
| Export attendance | ✅ | ✅ (own class) | ❌ |
| Download templates | ✅ | ✅ | ❌ |

---

## 6. UX Flow

### 6.1 Import Flow

```
Admin Dashboard → Data Management → Import
  → Select data type (Students/Teachers/Marks/Fees)
  → Download template (optional)
  → Upload CSV/Excel file
  → Select options (dry-run, upsert mode)
  → Submit → Validation runs
  → Show results: X valid, Y errors
  → Review errors (downloadable error report)
  → Confirm import (if dry-run) → Data committed
  → Summary: X created, Y updated, Z skipped
```

### 6.2 Export Flow

```
Admin Dashboard → Data Management → Export
  → Select data type
  → Select filters (class, date range, etc.)
  → Click Export → CSV/Excel downloaded
```

---

## 7. Database Design

### 7.1 New Table: `import_jobs`

```sql
CREATE TABLE import_jobs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    user_id         UUID NOT NULL,                 -- who initiated the import
    import_type     VARCHAR(32) NOT NULL,          -- students | teachers | marks | fees
    file_url        TEXT NOT NULL,                 -- Supabase Storage URL of uploaded file
    file_name       TEXT NOT NULL,
    status          VARCHAR(16) NOT NULL DEFAULT 'uploaded', -- uploaded | validating | validated | importing | completed | failed
    total_rows      INTEGER NOT NULL DEFAULT 0,
    valid_rows      INTEGER NOT NULL DEFAULT 0,
    error_rows      INTEGER NOT NULL DEFAULT 0,
    created_rows    INTEGER NOT NULL DEFAULT 0,
    updated_rows    INTEGER NOT NULL DEFAULT 0,
    skipped_rows    INTEGER NOT NULL DEFAULT 0,
    error_report_url TEXT,                         -- CSV with row-level errors
    is_dry_run      BOOLEAN NOT NULL DEFAULT false,
    upsert_mode     VARCHAR(16) NOT NULL DEFAULT 'skip', -- skip | update | upsert
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now(),
    completed_at    TIMESTAMP
);
CREATE INDEX idx_import_jobs_school ON import_jobs(school_id, created_at DESC);
```

### 7.2 Exposed Mapping

```kotlin
object ImportJobsTable : UUIDTable("import_jobs", "id") {
    val schoolId       = uuid("school_id")
    val userId         = uuid("user_id")
    val importType     = varchar("import_type", 32)
    val fileUrl        = text("file_url")
    val fileName       = text("file_name")
    val status         = varchar("status", 16).default("uploaded")
    val totalRows      = integer("total_rows").default(0)
    val validRows      = integer("valid_rows").default(0)
    val errorRows      = integer("error_rows").default(0)
    val createdRows    = integer("created_rows").default(0)
    val updatedRows    = integer("updated_rows").default(0)
    val skippedRows    = integer("skipped_rows").default(0)
    val errorReportUrl = text("error_report_url").nullable()
    val isDryRun       = bool("is_dry_run").default(false)
    val upsertMode     = varchar("upsert_mode", 16).default("skip")
    val createdAt      = timestamp("created_at")
    val updatedAt      = timestamp("updated_at")
    val completedAt    = timestamp("completed_at").nullable()
    init { index("idx_import_jobs_school", false, schoolId, createdAt) }
}
```

---

## 8. Backend Architecture

### 8.1 Component Overview

```
┌─────────────────────────────────────────────────┐
│                 Client (KMP)                     │
│  ImportScreen → Upload file → Poll status        │
└──────────────────┬──────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────┐
│              Backend (Ktor)                      │
│                                                  │
│  ImportRouting                                   │
│    ├── POST /import/upload → store file, create  │
│    │   import_job                                │
│    ├── POST /import/{id}/validate → dry-run      │
│    ├── POST /import/{id}/confirm → commit        │
│    ├── GET /import/{id}/status → progress        │
│    ├── GET /import/{id}/errors → error report    │
│    ├── GET /import/template/{type} → download    │
│    └── GET /export/{type} → CSV download         │
│                                                  │
│  ImportProcessor (background)                    │
│    ├── CsvParser → parse rows                    │
│    ├── Validator → validate each row             │
│    ├── ImporterService → commit to DB            │
│    └── ErrorReportGenerator → CSV error report   │
│                                                  │
│  ExportService                                   │
│    └── Query data → format CSV → stream response │
└──────────────────────────────────────────────────┘
```

### 8.2 ImportProcessor

```kotlin
class ImportProcessor(
    private val csvParser: CsvParser,
    private val validators: Map<String, ImportValidator>,
    private val importers: Map<String, ImportImporter>,
    private val storage: SupabaseStorage
) {
    suspend fun process(jobId: UUID) {
        val job = jobRepository.get(jobId)
        job.status = "validating"

        // 1. Download file from Supabase Storage
        val fileContent = storage.download(job.fileUrl)

        // 2. Parse CSV/Excel
        val rows = csvParser.parse(fileContent, job.importType)
        job.totalRows = rows.size

        // 3. Validate each row
        val validator = validators[job.importType]!!
        val errors = mutableListOf<RowError>()
        val validRows = mutableListOf<ParsedRow>()

        for ((index, row) in rows.withIndex()) {
            val result = validator.validate(row, job.schoolId)
            if (result.isValid) validRows.add(row)
            else errors.add(RowError(lineNumber = index + 2, field = result.field, message = result.message))
        }

        job.validRows = validRows.size
        job.errorRows = errors.size

        // 4. Generate error report if errors exist
        if (errors.isNotEmpty()) {
            job.errorReportUrl = generateErrorReport(errors, job)
        }

        // 5. If dry-run, stop here
        if (job.isDryRun) {
            job.status = "validated"
            return
        }

        // 6. Import valid rows
        job.status = "importing"
        val importer = importers[job.importType]!!
        val importResult = importer.import(validRows, job.schoolId, job.upsertMode)

        job.createdRows = importResult.created
        job.updatedRows = importResult.updated
        job.skippedRows = importResult.skipped
        job.status = "completed"
        job.completedAt = now()
    }
}
```

### 8.3 Validators

Each import type has a dedicated validator:

```kotlin
class StudentImportValidator : ImportValidator {
    override suspend fun validate(row: ParsedRow, schoolId: UUID): ValidationResult {
        // Required: full_name, class_name, section
        // Optional: roll_number, parent_phone, gender, date_of_birth
        // Validate:
        //   - full_name not blank, ≤ 100 chars
        //   - class_name exists in school_classes for school
        //   - section is valid for the class
        //   - parent_phone is valid E.164 if provided
        //   - gender is MALE/FEMALE/OTHER if provided
        //   - date_of_birth is valid date if provided
        //   - student_code auto-generated if not provided
        //   - duplicate check: (class_name, section, roll_number) or parent_phone
    }
}

class MarksImportValidator : ImportValidator {
    override suspend fun validate(row: ParsedRow, schoolId: UUID): ValidationResult {
        // Required: student_code, assessment_name, subject, marks
        // Validate:
        //   - student_code exists in students table
        //   - assessment_name exists in assessments for class+subject
        //   - marks is numeric, 0 <= marks <= max_marks
        //   - subject exists in school_subjects
    }
}
```

### 8.4 Importers

```kotlin
class StudentImporter : ImportImporter {
    override suspend fun import(rows: List<ParsedRow>, schoolId: UUID, upsertMode: String): ImportResult {
        var created = 0; var updated = 0; var skipped = 0
        for (row in rows) {
            val existing = findExisting(row, schoolId)
            if (existing != null) {
                when (upsertMode) {
                    "skip" -> skipped++
                    "update", "upsert" -> { updateStudent(existing, row); updated++ }
                }
            } else {
                createStudent(row, schoolId)
                created++
            }
        }
        return ImportResult(created, updated, skipped)
    }
}
```

### 8.5 ExportService

```kotlin
class ExportService {
    suspend fun exportStudents(schoolId: UUID, classId: UUID?): String  // CSV string
    suspend fun exportTeachers(schoolId: UUID): String
    suspend fun exportMarks(schoolId: UUID, classId: UUID, assessmentId: UUID): String
    suspend fun exportAttendance(schoolId: UUID, classId: UUID, dateFrom: LocalDate, dateTo: LocalDate): String
    suspend fun exportFees(schoolId: UUID, status: String?): String
}
```

Export streams CSV directly in HTTP response (`Content-Type: text/csv`, `Content-Disposition: attachment`).

### 8.6 Template Generator

```kotlin
fun generateTemplate(type: ImportType): String {
    return when (type) {
        ImportType.STUDENTS -> "full_name,roll_number,class_name,section,parent_phone,gender,date_of_birth\n" +
            "John Doe,101,Grade 5,A,+919876543210,MALE,2015-06-15"
        ImportType.TEACHERS -> "full_name,email,phone,subjects,classes\n" +
            "Jane Smith,jane@school.com,+919876543211,Mathematics|Science,Grade 5|Grade 6"
        ImportType.MARKS -> "student_code,assessment_name,subject,marks,max_marks\n" +
            "S001,Unit Test 1,Mathematics,85,100"
        ImportType.FEES -> "class_name,category,amount,due_date,installment_count\n" +
            "Grade 5,Tuition,15000,2026-07-15,3"
    }
}
```

---

## 9. Frontend Architecture

### 9.1 Client API

```kotlin
class ImportApi(httpClient: HttpClient) {
    suspend fun uploadFile(type: String, file: ByteArray, fileName: String, isDryRun: Boolean, upsertMode: String): NetworkResult<ImportJobDto>
    suspend fun validateJob(jobId: UUID): NetworkResult<ImportJobDto>
    suspend fun confirmJob(jobId: UUID): NetworkResult<ImportJobDto>
    suspend fun getJobStatus(jobId: UUID): NetworkResult<ImportJobDto>
    suspend fun downloadErrorReport(jobId: UUID): NetworkResult<ByteArray>
    suspend fun downloadTemplate(type: String): NetworkResult<ByteArray>
    suspend fun exportData(type: String, filters: Map<String, String>): NetworkResult<ByteArray>
}
```

### 9.2 File Picker

- **Android:** `ActivityResultContracts.GetContent()` with MIME types `text/csv`, `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- **iOS:** `UIDocumentPickerViewController` with same UTIs
- File size limit: 10MB

---

## 10. API Contracts

### 10.1 Upload Import File

```
POST /api/v1/school/import/upload
Content-Type: multipart/form-data

file: <csv/xlsx file>
import_type: students
is_dry_run: true
upsert_mode: skip
```

**Response 201:**
```json
{
  "success": true,
  "data": {
    "job_id": "uuid",
    "status": "uploaded",
    "file_name": "students.csv"
  }
}
```

### 10.2 Validate (Dry Run)

```
POST /api/v1/school/import/{jobId}/validate
```

Triggers validation. Returns job status with counts.

### 10.3 Confirm Import

```
POST /api/v1/school/import/{jobId}/confirm
```

Commits valid rows to database.

### 10.4 Job Status

```
GET /api/v1/school/import/{jobId}/status
```

**Response:**
```json
{
  "success": true,
  "data": {
    "job_id": "uuid",
    "status": "completed",
    "total_rows": 500,
    "valid_rows": 480,
    "error_rows": 20,
    "created_rows": 450,
    "updated_rows": 30,
    "skipped_rows": 0,
    "error_report_url": "https://supabase.co/...",
    "completed_at": "2026-06-27T10:35:00Z"
  }
}
```

### 10.5 Download Template

```
GET /api/v1/school/import/template/{type}
```

Returns CSV file download.

### 10.6 Export

```
GET /api/v1/school/export/{type}?class_id={uuid}&date_from={YYYY-MM-DD}&date_to={YYYY-MM-DD}
```

Returns CSV file download.

---

## 11. Validation Rules

### 11.1 Students Import

| Field | Required | Rule |
|---|---|---|
| full_name | Yes | Non-blank, ≤ 100 chars |
| roll_number | No | If provided, unique within class+section |
| class_name | Yes | Must exist in `school_classes` for school |
| section | Yes | Must be valid section for the class |
| parent_phone | No | E.164 format if provided |
| gender | No | MALE, FEMALE, or OTHER |
| date_of_birth | No | YYYY-MM-DD, not in future |

### 11.2 Teachers Import

| Field | Required | Rule |
|---|---|---|
| full_name | Yes | Non-blank, ≤ 100 chars |
| email | No | Valid email format if provided |
| phone | No | E.164 format if provided |
| subjects | No | Pipe-separated list |
| classes | No | Pipe-separated list |

### 11.3 Marks Import

| Field | Required | Rule |
|---|---|---|
| student_code | Yes | Must exist in `students` table |
| assessment_name | Yes | Must exist in `assessments` for student's class+subject |
| subject | Yes | Must exist in `school_subjects` |
| marks | Yes | Numeric, 0 ≤ marks ≤ max_marks |
| max_marks | No | Default 100 if not provided |

---

## 12. Error Handling

| Code | HTTP | Message |
|---|---|---|
| `IMPORT_FILE_TOO_LARGE` | 413 | File exceeds 10MB limit |
| `IMPORT_FILE_INVALID_FORMAT` | 415 | Only CSV and XLSX supported |
| `IMPORT_JOB_NOT_FOUND` | 404 | Import job not found |
| `IMPORT_ALREADY_COMPLETED` | 409 | Job already completed, cannot re-validate |
| `IMPORT_NO_VALID_ROWS` | 400 | All rows have validation errors |

---

## 13. Edge Cases

- **Empty CSV (headers only):** Total rows = 0, status = completed, created = 0
- **Missing required columns:** Validation fails at parse stage, error before row validation
- **Extra columns:** Ignored (only mapped columns processed)
- **Unicode names (Hindi, Tamil, etc.):** UTF-8 encoding required; validate file encoding
- **Very large file (10K+ rows):** Process in chunks of 500; update progress every chunk
- **Duplicate rows within same file:** Second occurrence flagged as error (duplicate within file)
- **Class doesn't exist:** Row error with suggestion to create class first
- **Excel with multiple sheets:** Only first sheet processed; warn if multiple sheets detected

---

## 14. Background Processing

Import processing runs as a background coroutine:
1. Job created with `status=uploaded`
2. Background worker picks up `status=uploaded` jobs
3. Processes in chunks of 500 rows
4. Updates `status` and counts incrementally
5. Generates error report CSV if errors exist
6. Sets `status=completed` or `status=failed`

**Concurrency:** Max 2 import jobs processing simultaneously per server.

---

## 15. Monitoring

| Metric | Type |
|---|---|
| `import.jobs_total` | Counter (by type, status) |
| `import.rows_processed_total` | Counter |
| `import.rows_errored_total` | Counter |
| `import.processing_time_ms` | Histogram |
| `export.requests_total` | Counter (by type) |

**Alerts:**
- Import failure rate > 20% → Warning
- Import processing > 10 min → Warning

---

## 16. Feature Flags

| Flag | Default | Description |
|---|---|---|
| `BULK_IMPORT_ENABLED` | false | Enable file import |
| `BULK_EXPORT_ENABLED` | true | Enable data export (lower risk) |

---

## 17. Migration Strategy

### 17.1 Migration File

`docs/db/migration_036_bulk_import_export.sql`

Creates `import_jobs` table.

### 17.2 Dependencies

Add CSV parsing library to `server/build.gradle.kts`:
- `com.opencsv:opencsv:5.9` (CSV parsing)
- `org.apache.poi:poi-ooxml:5.3.0` (Excel parsing, optional)

### 17.3 Rollback

Drop `import_jobs` table. Remove library dependencies.

---

## 18. Testing Strategy

### 18.1 Unit Tests

- CSV parsing — correct column mapping, UTF-8 handling
- Student validation — all field rules, edge cases
- Marks validation — student_code exists, marks within range
- Duplicate detection — within file and against DB
- Template generation — correct headers and sample row
- Error report generation — correct format with line numbers

### 18.2 Integration Tests

- Upload students CSV → validate → confirm → students created in DB
- Dry-run mode → no data committed
- Upsert mode → existing student updated, new student created
- Error report → downloadable CSV with correct error details
- Export students → CSV matches DB data
- Export with filters → only matching rows exported
- Large file (1000 rows) → completes within 60s
- Excel file import → same as CSV

---

## 19. Acceptance Criteria

- [ ] Admin can upload CSV/Excel file for student/teacher/marks/fee import
- [ ] Template downloads available for each import type
- [ ] Dry-run validation reports errors without committing
- [ ] Row-level errors include line number, field, and message
- [ ] Error report is downloadable as CSV
- [ ] Confirm import creates/updates/skips records correctly
- [ ] Upsert mode updates existing records
- [ ] Export generates CSV for students, teachers, marks, attendance, fees
- [ ] Export respects filters (class, date range, status)
- [ ] Import progress is trackable via status endpoint
- [ ] Large files (1000+ rows) process within 60 seconds

---

## 20. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration, Exposed table, CSV parser integration |
| 2 | 3 days | Validators (student, teacher, marks, fees) |
| 3 | 3 days | Importers (student, teacher, marks, fees) |
| 4 | 2 days | ImportProcessor (background job, chunking, progress) |
| 5 | 2 days | Error report generator |
| 6 | 2 days | ExportService (all data types) |
| 7 | 2 days | Template generator |
| 8 | 2 days | API endpoints (upload, validate, confirm, status, export, template) |
| 9 | 3 days | Client UI (import wizard, export buttons, file picker) |
| 10 | 2 days | Tests (unit + integration) |

---

## 21. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | `ImportJobsTable` |
| `server/.../db/DatabaseFactory.kt` | Modify | Register new table |
| `server/.../feature/import/CsvParser.kt` | New | CSV/Excel parsing |
| `server/.../feature/import/ImportValidator.kt` | New | Validator interface + implementations |
| `server/.../feature/import/ImportImporter.kt` | New | Importer interface + implementations |
| `server/.../feature/import/ImportProcessor.kt` | New | Background processing |
| `server/.../feature/import/ErrorReportGenerator.kt` | New | Error CSV generation |
| `server/.../feature/import/ExportService.kt` | New | Data export |
| `server/.../feature/import/TemplateGenerator.kt` | New | Template downloads |
| `server/.../feature/import/ImportRouting.kt` | New | API endpoints |
| `server/build.gradle.kts` | Modify | Add opencsv dependency |
| `docs/db/migration_036_bulk_import_export.sql` | New | DDL |
| `shared/.../feature/import/ImportApi.kt` | New | Client API |
| `composeApp/.../ui/v2/screens/admin/ImportScreen.kt` | New | Import wizard UI |
| `composeApp/.../ui/v2/screens/admin/ExportScreen.kt` | New | Export UI |

---

## 22. Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| CSV encoding issues (non-UTF-8) | Medium | Medium | Detect encoding; convert to UTF-8; reject if unconvertable |
| Large file OOM | Low | High | Stream parsing; chunk processing; 10MB file limit |
| Malicious file upload | Low | Medium | Validate MIME type; scan for macros; server-side parsing only |
| Data corruption from bad import | Medium | High | Dry-run mode; transaction per chunk; rollback on failure |
| Excel library bloat | Low | Low | Make Excel support optional (CSV first) |
