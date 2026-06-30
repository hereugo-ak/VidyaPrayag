# Facial Recognition Attendance — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** `OFFLINE_MODE_SPEC.md`, `DPDP_COMPLIANCE_SPEC.md`
> **Source:** `COMPETITIVE_GAP_ANALYSIS.md` — emerging trend
> **Template:** `_SPEC_TEMPLATE.md` v1 (25 mandatory + 6 optional sections)

---

## 1. Feature Overview

### What

Automated attendance marking via facial recognition: camera captures student faces at classroom entry, AI matches against enrolled face prints, and marks attendance. Eliminates manual roll call and reduces proxy attendance.

### Why — Product Rationale

Manual attendance is time-consuming and prone to proxy attendance (students marking absent friends present). Facial recognition automates attendance, saving 5-10 minutes per class and eliminating proxy attendance. This is a **medium-priority emerging tech feature** (Phase 4, effort L) that provides competitive differentiation.

### What Stands Out (Competitive Moat)

From `COMPETITIVE_GAP_ANALYSIS.md`: facial recognition as emerging trend.

Few Indian school ERPs offer facial recognition attendance. The key moat is **on-device face vector extraction** (privacy-friendly, works offline) combined with **DPDP-compliant consent and data handling**.

### Goals

- Enroll student face prints (photo during admission/ID card generation)
- Classroom camera or tablet camera captures faces at entry
- AI matches captured faces against enrolled prints
- Auto-mark attendance (present/absent) for matched students
- Privacy: face prints stored as encrypted vectors, not raw images
- Manual override: teacher can correct misidentification
- DPDP compliance: explicit consent for biometric data

### Non-goals

- [ ] Real-time continuous face tracking (capture at entry only, not continuous monitoring)
- [ ] Face recognition for security/access control (attendance only)
- [ ] Facial emotion or behavior analysis (attendance only)
- [ ] Integration with CCTV systems (tablet/dedicated camera only)
- [ ] Face recognition for visitors or staff (students only)
- [ ] Live video streaming to server (images captured and processed locally)
- [ ] Cross-school face matching (each school has own enrollment)

### Dependencies

- `OFFLINE_MODE_SPEC.md` — on-device face vector extraction (offline-capable)
- `DPDP_COMPLIANCE_SPEC.md` — consent flow for biometric data
- `AttendanceRecordsTable` — existing attendance marking
- `StudentsTable` — student photos (`photoUrl`) for enrollment
- `ID_CARD_GENERATION_SPEC.md` — ID card photos can be used for enrollment
- `ClassesTable` — class information for attendance marking

### Related Modules

- `server/.../feature/face/` — new facial recognition module
- `server/.../feature/attendance/` — existing attendance module
- `shared/.../core/ml/` — on-device ML models
- `composeApp/.../ui/v2/screens/admin/` — enrollment UI
- `composeApp/.../ui/v2/screens/teacher/` — classroom capture UI

---

## 2. Current System Assessment

### Existing Code

- `AttendanceRecordsTable` — manual attendance marking (PRESENT/ABSENT/LATE)
- `StudentsTable` — has `photoUrl` for student photos
- `ID_CARD_GENERATION_SPEC.md` — ID card photos can be used for enrollment
- No facial recognition or biometric system
- `COMPETITIVE_GAP_ANALYSIS.md`: facial recognition as emerging trend

### Existing Database

- `AttendanceRecordsTable` — attendance records (student_id, class_id, date, status)
- `StudentsTable` — student info including `photoUrl`
- `ClassesTable` — class information
- `SchoolsTable` — school info
- No face enrollment or face attendance tables

### Existing APIs

- Attendance API (existing) — manual attendance marking
- Student API (existing) — student CRUD
- ID Card API (existing) — ID card generation with photos
- No face recognition API

### Existing UI

- Attendance marking screen (existing) — manual roll call
- Student profile (existing) — shows photo
- ID card generation (existing) — captures/uses student photo
- No face recognition UI

### Existing Services

- `AttendanceService` — manual attendance marking
- `StudentService` — student CRUD
- No face recognition service

### Existing Documentation

- `COMPETITIVE_GAP_ANALYSIS.md` — facial recognition as emerging trend
- `OFFLINE_MODE_SPEC.md` — offline capabilities
- `DPDP_COMPLIANCE_SPEC.md` — data protection compliance
- `ID_CARD_GENERATION_SPEC.md` — ID card generation

### Technical Debt

| # | Gap | Details |
|---|---|---|
| TD-1 | No face enrollment | No `face_enrollments` table |
| TD-2 | No face recognition service | No service for enroll, recognize, match |
| TD-3 | No on-device ML | No TensorFlow Lite face detection model |
| TD-4 | No consent integration | No DPDP consent flow for biometric data |
| TD-5 | No face attendance UI | No enrollment, classroom capture, or teacher review UI |

### Gaps

| # | Gap | Impact | Severity |
|---|---|---|---|
| G1 | No face enrollment | Can't enroll student face prints | **High** |
| G2 | No face recognition | Can't auto-mark attendance | **High** |
| G3 | No consent flow | Non-compliant with DPDP for biometric data | **Critical** |
| G4 | No privacy controls | Raw images and vectors not managed | **High** |
| G5 | No teacher override | Can't correct misidentification | **Medium** |

---

## 3. Functional Requirements

### FR-001
| Field | Value |
|---|---|
| **Title** | Enroll Face Print |
| **Description** | Enroll face print: capture photo → extract face vector (embedding) → store encrypted. |
| **Priority** | High |
| **User Roles** | School Admin |
| **Acceptance notes** | Admin captures/uploads student photo. Face vector extracted (512-dim float array). Vector encrypted (AES-256) and stored. Raw photo deleted after extraction. |

### FR-002
| Field | Value |
|---|---|
| **Title** | Consent Required |
| **Description** | Consent: parent must consent to biometric data usage (via `DPDP_COMPLIANCE_SPEC.md`). |
| **Priority** | Critical |
| **User Roles** | Parent |
| **Acceptance notes** | Parent explicitly consents via DPDP consent flow. Consent recorded with timestamp. Enrollment blocked until consent given. |

### FR-003
| Field | Value |
|---|---|
| **Title** | Classroom Capture |
| **Description** | Classroom capture: tablet camera or dedicated camera captures faces at entry. |
| **Priority** | High |
| **User Roles** | Teacher |
| **Acceptance notes** | Teacher opens face attendance screen. Tablet camera captures faces at classroom entry. Multiple faces captured in single session. Device type recorded (tablet or camera). |

### FR-004
| Field | Value |
|---|---|
| **Title** | AI Matching |
| **Description** | AI matching: compare captured face vectors against enrolled vectors (cosine similarity ≥ 0.85). |
| **Priority** | High |
| **User Roles** | System |
| **Acceptance notes** | Each captured face vector compared against all enrolled vectors for the class. Cosine similarity ≥ 0.85 = match. Best match selected if multiple above threshold. |

### FR-005
| Field | Value |
|---|---|
| **Title** | Auto-Mark Attendance |
| **Description** | Auto-mark attendance for matched students. |
| **Priority** | High |
| **User Roles** | System |
| **Acceptance notes** | Matched students marked PRESENT in `AttendanceRecordsTable`. Unmatched students remain ABSENT (default). Date and class_id recorded. |

### FR-006
| Field | Value |
|---|---|
| **Title** | Unmatched Faces Flagged |
| **Description** | Unmatched faces flagged for teacher review. |
| **Priority** | Medium |
| **User Roles** | Teacher |
| **Acceptance notes** | Captured faces that don't match any enrolled vector are flagged. Teacher sees list of unmatched faces for manual identification. |

### FR-007
| Field | Value |
|---|---|
| **Title** | Teacher Override |
| **Description** | Teacher override: correct misidentification or mark unmatched students. |
| **Priority** | High |
| **User Roles** | Teacher |
| **Acceptance notes** | Teacher can change attendance status for any student. Can mark unmatched faces as specific students. Override logged in audit trail. |

### FR-008
| Field | Value |
|---|---|
| **Title** | Privacy — Raw Image Deletion |
| **Description** | Privacy: raw images deleted after vector extraction; only encrypted vectors stored. |
| **Priority** | Critical |
| **User Roles** | System |
| **Acceptance notes** | Raw photos deleted immediately after face vector extraction. Only encrypted 512-dim vectors stored. No raw biometric images retained. |

### FR-009
| Field | Value |
|---|---|
| **Title** | Opt-Out |
| **Description** | Opt-out: parent can withdraw consent → face print deleted. |
| **Priority** | Critical |
| **User Roles** | Parent |
| **Acceptance notes** | Parent withdraws consent via DPDP consent flow. Face vector permanently deleted from `face_enrollments`. Student reverts to manual attendance. |

### Non-Functional Requirements

| ID | Requirement |
|---|---|
| NFR-1 | Face vector extraction: < 2 seconds per image (on-device) |
| NFR-2 | Face matching: < 5 seconds for class of 40 students |
| NFR-3 | Face vector size: 512-dim float array (~2KB encrypted) |
| NFR-4 | Cosine similarity threshold: ≥ 0.85 for match |
| NFR-5 | Encryption: AES-256 for face vectors at rest |
| NFR-6 | Raw image deletion: within 1 second of vector extraction |
| NFR-7 | Offline support: on-device extraction works without internet |

---

## 4. User Stories

### School Admin
- [ ] Enroll student face print from admission photo or ID card photo
- [ ] View enrolled students and consent status
- [ ] Delete enrollment when consent withdrawn

### Teacher
- [ ] Open face attendance screen for my class
- [ ] Capture faces at classroom entry via tablet camera
- [ ] View matched students (auto-marked present)
- [ ] View unmatched faces for review
- [ ] Override/correct attendance for misidentified students
- [ ] Mark unmatched faces as specific students

### Parent
- [ ] Consent to biometric data usage for my child
- [ ] Withdraw consent (opt-out) — face print deleted
- [ ] View consent status

### System
- [ ] Extract face vectors from captured photos
- [ ] Match captured vectors against enrolled vectors
- [ ] Auto-mark attendance for matched students
- [ ] Delete raw images after vector extraction
- [ ] Encrypt face vectors at rest (AES-256)
- [ ] Log all face recognition operations for audit

---

## 5. Business Rules

### BR-001
**Rule:** Consent required before enrollment.
**Enforcement:** Enrollment blocked until parent consents via DPDP consent flow. `consent_given = false` blocks enrollment. Consent timestamp recorded.

### BR-002
**Rule:** Face vectors encrypted at rest.
**Enforcement:** AES-256 encryption for `face_vector` column. Encryption key managed by server. Vectors decrypted only during matching.

### BR-003
**Rule:** Raw images deleted after vector extraction.
**Enforcement:** Raw photo deleted within 1 second of vector extraction. No raw biometric images stored. Only encrypted vectors retained.

### BR-004
**Rule:** Cosine similarity ≥ 0.85 for match.
**Enforcement:** Face matching uses cosine similarity. Threshold 0.85 (configurable). Below threshold = unmatched. Best match above threshold selected.

### BR-005
**Rule:** Consent withdrawal deletes face print.
**Enforcement:** When parent withdraws consent, `face_enrollments` row deleted (hard delete, not soft delete). Student reverts to manual attendance.

### BR-006
**Rule:** No external sharing of face data.
**Enforcement:** Face vectors never shared with third parties. On-device extraction preferred. No external API calls for face data.

### BR-007
**Rule:** Face recognition is per-school.
**Enforcement:** Each school has own face enrollments. No cross-school matching. `school_id` scoping on all queries.

### BR-008
**Rule:** Teacher override always available.
**Enforcement:** Teacher can change any student's attendance status after face recognition. Override logged with teacher ID and timestamp.

### BR-009
**Rule:** One face enrollment per student.
**Enforcement:** `face_enrollments.student_id` is UNIQUE. One enrollment per student. Re-enrollment replaces existing (delete + insert).

### BR-010
**Rule:** Face attendance logs retained for audit.
**Enforcement:** `face_attendance_logs` records all capture sessions. Captured/matched/unmatched counts, device type, and timestamp logged. Retained per data retention policy.

---

## 6. Database Design

### 6.1 Entity Relationship Summary

Two new tables: `face_enrollments` (student face vectors) and `face_attendance_logs` (capture session logs). References existing `students`, `schools`, and `classes` tables.

### 6.2 New Tables

#### `face_enrollments` table

```sql
CREATE TABLE face_enrollments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    student_id      UUID NOT NULL UNIQUE,
    face_vector     BYTEA NOT NULL,                -- encrypted face embedding (512-dim float array)
    enrolled_at     TIMESTAMP NOT NULL DEFAULT now(),
    consent_given   BOOLEAN NOT NULL DEFAULT false,
    consent_at      TIMESTAMP,
    is_active       BOOLEAN NOT NULL DEFAULT true
);
```

#### `face_attendance_logs` table

```sql
CREATE TABLE face_attendance_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    class_id        UUID NOT NULL,
    date            DATE NOT NULL,
    captured_count  INTEGER NOT NULL DEFAULT 0,
    matched_count   INTEGER NOT NULL DEFAULT 0,
    unmatched_count INTEGER NOT NULL DEFAULT 0,
    device_type     VARCHAR(16),                   -- tablet | camera
    processed_at    TIMESTAMP NOT NULL DEFAULT now()
);
```

### 6.3 Modified Tables

N/A — no existing tables modified. Attendance marking uses existing `AttendanceRecordsTable`.

### 6.4 Indexes

- `face_enrollments.student_id` — UNIQUE constraint (implicit index)
- `face_enrollments.school_id` — for school-scoped queries
- `face_attendance_logs(school_id, class_id, date)` — for log lookup

### 6.5 Constraints

- `face_enrollments.student_id` — NOT NULL, UNIQUE (one enrollment per student)
- `face_enrollments.face_vector` — NOT NULL, BYTEA (encrypted)
- `face_enrollments.consent_given` — NOT NULL, default false
- `face_enrollments.is_active` — NOT NULL, default true
- `face_attendance_logs.captured_count` — NOT NULL, default 0
- `face_attendance_logs.matched_count` — NOT NULL, default 0
- `face_attendance_logs.unmatched_count` — NOT NULL, default 0
- `face_attendance_logs.device_type` — VARCHAR(16), values: tablet | camera

### 6.6 Foreign Keys

- `face_enrollments.school_id` → `schools.id` (implicit)
- `face_enrollments.student_id` → `students.id` (implicit)
- `face_attendance_logs.school_id` → `schools.id` (implicit)
- `face_attendance_logs.class_id` → `classes.id` (implicit)

### 6.7 Soft Delete Strategy

- `face_enrollments`: NO soft delete. Consent withdrawal = hard delete (permanent deletion of biometric data).
- `face_attendance_logs`: NO soft delete. Retained for audit per data retention policy.

### 6.8 Audit Fields

- `face_enrollments.enrolled_at` — when enrollment occurred
- `face_enrollments.consent_given`, `consent_at` — consent status and timestamp
- `face_enrollments.is_active` — active flag (false if deactivated before deletion)
- `face_attendance_logs.processed_at` — when capture session was processed
- `face_attendance_logs.captured_count`, `matched_count`, `unmatched_count` — session statistics

### 6.9 Migration Notes

Migration: `docs/db/migration_086_facial_recognition.sql`
- CREATE 2 tables: `face_enrollments`, `face_attendance_logs`
- No data migration (new feature)

### 6.10 Exposed Mappings

```kotlin
object FaceEnrollmentsTable : UUIDTable("face_enrollments", "id") {
    val schoolId     = uuid("school_id")
    val studentId    = uuid("student_id").uniqueIndex()
    val faceVector   = binary("face_vector")  // encrypted
    val enrolledAt   = timestamp("enrolled_at")
    val consentGiven = bool("consent_given").default(false)
    val consentAt    = timestamp("consent_at").nullable()
    val isActive     = bool("is_active").default(true)
}

object FaceAttendanceLogsTable : UUIDTable("face_attendance_logs", "id") {
    val schoolId      = uuid("school_id")
    val classId       = uuid("class_id")
    val date          = date("date")
    val capturedCount = integer("captured_count").default(0)
    val matchedCount  = integer("matched_count").default(0)
    val unmatchedCount= integer("unmatched_count").default(0)
    val deviceType    = varchar("device_type", 16).nullable()
    val processedAt   = timestamp("processed_at")

    init {
        index("idx_face_att_logs_school_class_date", schoolId, classId, date)
    }
}
```

Register in `DatabaseFactory.allTables`.

### 6.11 Seed Data

N/A — face enrollments are user-initiated. No seed data needed.

---

## 7. State Machines

### Face Enrollment State Machine

```
not_enrolled ──consent_given──> enrolling ──vector_extracted──> enrolled
  │                                │
  │                                └──extraction_failed──> not_enrolled
  │
  └──enrolled ──consent_withdrawn──> deleting ──deleted──> not_enrolled
  │
  └──enrolled ──re_enroll──> enrolling (replace) ──vector_extracted──> enrolled
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `not_enrolled` | Parent consents + admin enrolls | `enrolling` | Consent verified, photo available |
| `enrolling` | Face vector extracted successfully | `enrolled` | Vector encrypted and stored |
| `enrolling` | Extraction failed | `not_enrolled` | Error, no vector stored |
| `enrolled` | Parent withdraws consent | `deleting` | Consent withdrawal initiated |
| `deleting` | Face vector deleted | `not_enrolled` | Hard delete complete |
| `enrolled` | Admin re-enrolls (new photo) | `enrolling` | Replace existing enrollment |

### Face Attendance Capture State Machine

```
idle ──teacher_opens──> capturing ──teacher_stops──> processing ──matching_complete──> results_ready
  │                        │                            │                                │
  │                        │                            └──matching_failed──> error      │
  │                        └──cancel──> idle                                              │
  │                                                                                       │
  └──results_ready ──teacher_reviews──> reviewing ──teacher_confirms──> attendance_marked
  │                                              │
  │                                              └──teacher_overrides──> reviewing (modified)
  │
  └──attendance_marked ──(terminal)
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `idle` | Teacher opens face attendance | `capturing` | Select class and date |
| `capturing` | Teacher stops capture | `processing` | Images captured |
| `capturing` | Teacher cancels | `idle` | No images processed |
| `processing` | Matching complete | `results_ready` | Matched + unmatched lists ready |
| `processing` | Matching failed | `error` | AI/model error |
| `results_ready` | Teacher reviews results | `reviewing` | View matched/unmatched |
| `reviewing` | Teacher overrides | `reviewing` | Correct misidentification |
| `reviewing` | Teacher confirms | `attendance_marked` | Attendance saved |
| `error` | Teacher retries | `capturing` | Back to capture |
| `attendance_marked` | — | `attendance_marked` | Terminal state |

### Consent State Machine

```
no_consent ──parent_consents──> consented ──parent_withdraws──> no_consent
  │
  └──consented ──enrollment_possible──> enrolled (face print stored)
  │
  └──enrolled ──parent_withdraws──> deleting ──deleted──> no_consent
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `no_consent` | Parent consents | `consented` | Consent recorded with timestamp |
| `consented` | Admin enrolls | `enrolled` | Face vector stored |
| `enrolled` | Parent withdraws consent | `deleting` | Consent withdrawal initiated |
| `deleting` | Face vector deleted | `no_consent` | Hard delete complete |
| `consented` | Parent withdraws (before enrollment) | `no_consent` | Consent revoked |

---

## 8. Backend Architecture

### 8.1 Component Overview

`FaceRecognitionService` handles enrollment, recognition/matching, and deletion. On-device `FaceDetector` (expect/actual) extracts face vectors from images. Server-side matching compares vectors using cosine similarity. Consent managed via DPDP compliance module.

### 8.2 Design Principles

1. **Privacy-first** — on-device extraction, encrypted storage, raw image deletion
2. **Consent-gated** — no enrollment without explicit parental consent
3. **Offline-capable** — on-device face vector extraction works without internet
4. **Teacher override** — AI is assistive, not authoritative; teacher can always correct
5. **DPDP-compliant** — biometric data handled per DPDP regulations
6. **School-scoped** — face enrollments per school, no cross-school matching

### 8.3 Core Types

#### FaceRecognitionService

```kotlin
class FaceRecognitionService {
    suspend fun enroll(studentId: UUID, photoUrl: String, consent: Boolean): EnrollmentResult {
        // 1. Verify consent
        // 2. Download photo
        // 3. Extract face vector (via face recognition API or on-device model)
        // 4. Encrypt and store face_vector
        // 5. Delete raw photo
    }

    suspend fun recognize(capturedImages: List<String>, classId: UUID, date: LocalDate): RecognitionResult {
        // 1. For each captured image:
        //    a. Extract face vector
        //    b. Compare against all enrolled vectors for class
        //    c. If similarity ≥ 0.85 → match found
        // 2. Mark attendance for matched students
        // 3. Return matched + unmatched lists
    }

    suspend fun deleteEnrollment(studentId: UUID)  // consent withdrawal
}
```

#### FaceDetector (expect/actual)

```kotlin
// expect/actual pattern for on-device face detection
expect class FaceDetector() {
    fun detectFaces(image: ByteArray): List<FaceDetection>
    fun extractVector(faceImage: ByteArray): FloatArray  // 512-dim embedding
}

// Android: TensorFlow Lite face recognition model
// iOS: Core ML face recognition model
// Web: TensorFlow.js face recognition model
```

### 8.4 Repositories

- `FaceEnrollmentRepository` — CRUD for `face_enrollments`
- `FaceAttendanceLogRepository` — CRUD for `face_attendance_logs`

### 8.5 Mappers

- `FaceEnrollmentMapper` — maps `face_enrollments` rows to DTOs (face_vector not exposed in DTOs)
- `FaceAttendanceLogMapper` — maps `face_attendance_logs` rows to DTOs

### 8.6 Permission Checks

- Enroll: school admin role + JWT auth + consent verified
- Recognize: teacher role + JWT auth + class assignment verified
- Delete enrollment: school admin role OR parent (for own child) + JWT auth
- Consent: parent role + JWT auth + parent-student relationship verified
- View logs: school admin role + JWT auth

### 8.7 Background Jobs

N/A — face recognition is on-demand (triggered by teacher during classroom capture). No background jobs needed.

### 8.8 Domain Events

- `FaceEnrolled` — emitted when student face print is enrolled
- `FaceRecognized` — emitted when face matching completes (with counts)
- `FaceEnrollmentDeleted` — emitted when enrollment is deleted (consent withdrawal)
- `ConsentGiven` — emitted when parent consents to biometric data
- `ConsentWithdrawn` — emitted when parent withdraws consent

### 8.9 Caching

N/A — face vectors are fetched from DB during matching. No caching (security concern with caching biometric data).

### 8.10 Transactions

- Enroll: single transaction (insert enrollment, delete raw photo)
- Recognize: single transaction (insert attendance records, insert log)
- Delete enrollment: single transaction (hard delete enrollment)

### 8.11 Rate Limiting

- Enroll: 10 per minute per admin (bulk enrollment during admission)
- Recognize: 5 per minute per teacher (one per class session)
- Consent: 1 per minute per parent (prevent spam)

### 8.12 Configuration

- `FACIAL_RECOGNITION_ENABLED` — default `false`; enable/disable feature (off by default, opt-in)
- `FACE_MATCH_THRESHOLD` — default `0.85`; cosine similarity threshold for match
- `FACE_VECTOR_DIMENSIONS` — default `512`; embedding dimensions
- `FACE_VECTOR_ENCRYPTION_KEY` — AES-256 encryption key for face vectors
- `FACE_EXTRACTION_MODE` — default `on_device`; options: on_device | server_api | hybrid
- `RAW_IMAGE_DELETE_DELAY_MS` — default `1000`; delay before raw image deletion

### 8.13 Face Vector Extraction

Options:
- **Server-side API:** AWS Rekognition, Google Face API, or Azure Face API
- **On-device:** TensorFlow Lite face recognition model (offline, privacy-friendly)
- **Hybrid:** On-device extraction + server-side matching

Recommended: on-device extraction (privacy) + server-side matching (performance).

---

## 9. API Contracts

### 9.1 Admin Endpoints

```
POST /api/v1/school/face/enroll
  Body: { student_id, photo_url }
  → 201: EnrollmentDto
  → 403: Consent not given

DELETE /api/v1/school/face/enroll/{studentId}
  → 204: No content (enrollment deleted)

POST /api/v1/school/face/recognize
  Body: { class_id, date, captured_images: [...] }
  → 200: RecognitionResultDto
  → 400: No enrolled students in class

GET /api/v1/school/face/attendance-logs?class_id={uuid}&date={YYYY-MM-DD}
  → 200: { logs: [FaceAttendanceLogDto] }
```

### 9.2 Parent Endpoints (Consent)

```
POST /api/v1/parent/face/consent
  Body: { child_id, consent: true }
  → 200: ConsentDto

DELETE /api/v1/parent/face/consent/{childId}
  → 204: No content (consent withdrawn, enrollment deleted if exists)
```

### 9.3 DTO Models

All `@Serializable`, wrapped in `ApiResponse<T>` pattern.

```kotlin
@Serializable data class EnrollmentDto(
    val studentId: String,
    val enrolledAt: String,
    val consentGiven: Boolean,
    val consentAt: String?,
    val isActive: Boolean,
    // NOTE: face_vector NEVER exposed in DTO
)

@Serializable data class RecognitionResultDto(
    val matchedStudents: List<MatchedStudentDto>,
    val unmatchedCount: Int,
    val logId: String,
)

@Serializable data class MatchedStudentDto(
    val studentId: String,
    val studentName: String,
    val similarity: Double,
    val attendanceStatus: String,  // PRESENT
)

@Serializable data class FaceAttendanceLogDto(
    val id: String,
    val classId: String,
    val date: String,
    val capturedCount: Int,
    val matchedCount: Int,
    val unmatchedCount: Int,
    val deviceType: String?,
    val processedAt: String,
)

@Serializable data class ConsentDto(
    val childId: String,
    val consent: Boolean,
    val consentAt: String,
)

@Serializable data class EnrollRequest(val studentId: String, val photoUrl: String)
@Serializable data class RecognizeRequest(val classId: String, val date: String, val capturedImages: List<String>)
@Serializable data class ConsentRequest(val childId: String, val consent: Boolean)
```

---

## 10. Frontend Architecture

### 10.1 Screens

| Screen | Platform | Role | Description |
|---|---|---|---|
| `FaceEnrollmentScreen` | Compose | Admin | Enroll student face prints, view consent status |
| `FaceAttendanceScreen` | Compose | Teacher | Classroom face capture, view results, override |
| `FaceConsentScreen` | Compose | Parent | Consent/withdraw consent for biometric data |
| `FaceAttendanceLogScreen` | Compose | Admin | View face attendance logs |

### 10.2 Navigation

- Admin: Students → Face Enrollment → Enroll/View/Delete
- Teacher: Attendance → Face Attendance → Capture → Results → Override → Confirm
- Parent: Settings → Face Recognition Consent → Give/Withdraw

### 10.3 UX Flows

#### Admin: Enroll Student Face
1. Admin opens Face Enrollment screen
2. Selects student (or searches)
3. Verifies consent status (green = consented, red = no consent)
4. If consented: uploads/captures photo
5. "Extracting face vector..." loading
6. Enrollment complete → "Face print enrolled for {student_name}"
7. Raw photo deleted (not stored)

#### Teacher: Face Attendance
1. Teacher opens Face Attendance screen
2. Selects class and date
3. "Capture faces at classroom entry" — tablet camera opens
4. Students walk past camera → faces captured
5. Teacher taps "Stop & Process"
6. "Matching faces..." loading
7. Results: matched students (auto-marked PRESENT) + unmatched faces
8. Teacher reviews, overrides if needed
9. Teacher confirms → attendance saved

#### Parent: Consent
1. Parent opens Settings → Face Recognition
2. Sees explanation: "Facial recognition attendance uses biometric data..."
3. Toggles consent: "I consent to facial recognition attendance for {child_name}"
4. Consent recorded
5. To withdraw: toggles off → "Face print will be permanently deleted. Continue?"
6. Confirms → face print deleted

### 10.4 State Management

```kotlin
data class FaceEnrollmentState(
    val selectedStudent: StudentDto?,
    val consentStatus: ConsentStatus?,
    val isEnrolling: Boolean,
    val error: String?,
)

data class FaceAttendanceState(
    val selectedClass: ClassDto?,
    val selectedDate: LocalDate,
    val isCapturing: Boolean,
    val isProcessing: Boolean,
    val recognitionResult: RecognitionResultDto?,
    val error: String?,
)

data class FaceConsentState(
    val childId: String,
    val consentGiven: Boolean,
    val isUpdating: Boolean,
    val error: String?,
)
```

### 10.5 Offline Support

- Face vector extraction: available offline (on-device TFLite model)
- Face matching: available offline (vectors fetched from local cache or server)
- Enrollment: requires server (to store encrypted vector)
- Consent: requires server (to record consent)

### 10.6 Loading States

- Enrolling: "Extracting face vector..."
- Capturing: "Capturing faces..." (live camera preview)
- Processing: "Matching faces..."
- Deleting: "Deleting face print..."
- Consent: "Updating consent..."

### 10.7 Error Handling (UI)

- No consent: "Parent consent required. Please ask parent to consent via app."
- Extraction failed: "Unable to extract face from photo. Ensure clear, front-facing photo."
- No enrolled students: "No students enrolled in this class. Please enroll first."
- No faces detected: "No faces detected in capture. Ensure students face the camera."
- Matching failed: "Face matching failed. Please use manual attendance."
- Consent withdrawal: "Face print permanently deleted. Student will use manual attendance."

### 10.8 Component Integration Guidelines

| Rule | Description |
|---|---|
| **R1** | Enrollment: clear, front-facing photo with good lighting |
| **R2** | Consent indicator: green badge (consented), red badge (no consent) |
| **R3** | Classroom capture: live camera preview with face detection overlay |
| **R4** | Results: green for matched, amber for unmatched |
| **R5** | Override: tap student to change status (PRESENT/ABSENT/LATE) |
| **R6** | Unmatched faces: show face thumbnail with "Who is this?" selector |
| **R7** | Confirm button: disabled until teacher reviews all unmatched |
| **R8** | Privacy notice: "Raw images are deleted. Only encrypted vectors stored." |
| **R9** | Consent screen: clear explanation of biometric data usage |
| **R10** | Withdraw consent: confirmation dialog with permanent deletion warning |

### 10.9 Face Detector (On-Device)

```kotlin
// expect/actual pattern for on-device face detection
expect class FaceDetector() {
    fun detectFaces(image: ByteArray): List<FaceDetection>
    fun extractVector(faceImage: ByteArray): FloatArray  // 512-dim embedding
}

// Android: TensorFlow Lite face recognition model
// iOS: Core ML face recognition model
// Web: TensorFlow.js face recognition model
```

---

## 11. Shared Module Changes (KMP)

### 11.1 DTOs

All DTOs defined in section 9.3, placed in `shared/.../face/domain/model/FaceRecognitionModels.kt`.

### 11.2 Domain Models

```kotlin
data class FaceEnrollment(
    val id: String,
    val schoolId: String,
    val studentId: String,
    val enrolledAt: Instant,
    val consentGiven: Boolean,
    val consentAt: Instant?,
    val isActive: Boolean,
)

data class FaceAttendanceLog(
    val id: String,
    val schoolId: String,
    val classId: String,
    val date: LocalDate,
    val capturedCount: Int,
    val matchedCount: Int,
    val unmatchedCount: Int,
    val deviceType: String?,
    val processedAt: Instant,
)

data class RecognitionResult(
    val matchedStudents: List<MatchedStudent>,
    val unmatchedCount: Int,
    val logId: String,
)

data class MatchedStudent(
    val studentId: String,
    val studentName: String,
    val similarity: Double,
)

data class FaceDetection(
    val boundingBox: Rect,
    val confidence: Double,
)

enum class DeviceType { TABLET, CAMERA }
```

### 11.3 Repository Interfaces

```kotlin
interface FaceRecognitionRepository {
    suspend fun enroll(token: String, studentId: String, photoUrl: String): NetworkResult<EnrollmentDto>
    suspend fun deleteEnrollment(token: String, studentId: String): NetworkResult<Unit>
    suspend fun recognize(token: String, classId: String, date: String, images: List<String>): NetworkResult<RecognitionResultDto>
    suspend fun getAttendanceLogs(token: String, classId: String, date: String): NetworkResult<List<FaceAttendanceLogDto>>
    suspend fun giveConsent(token: String, childId: String, consent: Boolean): NetworkResult<ConsentDto>
    suspend fun withdrawConsent(token: String, childId: String): NetworkResult<Unit>
}
```

### 11.4 UseCases

- `EnrollFaceUseCase`
- `RecognizeFacesUseCase`
- `DeleteEnrollmentUseCase`
- `GiveConsentUseCase`
- `WithdrawConsentUseCase`
- `GetAttendanceLogsUseCase`

### 11.5 Validation

- `student_id`: valid UUID
- `photo_url`: valid URL, image file
- `class_id`: valid UUID
- `date`: valid date, not in future
- `captured_images`: non-empty list of image URLs/data
- `consent`: boolean

### 11.6 Serialization

Standard Kotlinx serialization. Dates as ISO strings. Face vectors never serialized in DTOs.

### 11.7 Network APIs

Ktor `@Resource` route definitions in `FaceRecognitionApi.kt`:
- POST `/api/v1/school/face/enroll`
- DELETE `/api/v1/school/face/enroll/{studentId}`
- POST `/api/v1/school/face/recognize`
- GET `/api/v1/school/face/attendance-logs`
- POST `/api/v1/parent/face/consent`
- DELETE `/api/v1/parent/face/consent/{childId}`

### 11.8 Database Models (Local Cache)

- Face enrollment status cached locally (consent status, enrollment active)
- Face vectors: NOT cached locally (security concern, fetched from server)
- Attendance logs: cached locally (5-minute TTL)

---

## 12. Permissions Matrix

| Action | Super Admin | School Admin | Counselor | Teacher | Parent |
|---|---|---|---|---|---|
| Enroll face print | ✅ (all) | ✅ (own school) | ❌ | ❌ | ❌ |
| Delete enrollment | ✅ (all) | ✅ (own school) | ❌ | ❌ | ✅ (own child, via consent withdrawal) |
| Recognize (capture) | N/A | ✅ (own school) | ❌ | ✅ (own class) | ❌ |
| Override attendance | N/A | ✅ (own school) | ❌ | ✅ (own class) | ❌ |
| View attendance logs | ✅ (all) | ✅ (own school) | ❌ | ✅ (own class) | ❌ |
| Give/withdraw consent | N/A | N/A | N/A | N/A | ✅ (own child) |
| Configure threshold | ✅ | ✅ | ❌ | ❌ | ❌ |

---

## 13. Notifications

### Face Recognition Notifications

N/A — face recognition does not generate notifications. Attendance marking uses existing attendance notification system.

### Notification Integration

Attendance marked via face recognition triggers existing attendance notifications (if configured) via `Notify.kt` with category "attendance".

---

## 14. Background Jobs

N/A — no background jobs. Face recognition is on-demand (triggered by teacher during classroom capture). Enrollment is on-demand (triggered by admin).

---

## 15. Integrations

### Internal Integrations

| System | Integration Point | Direction | Protocol | Error Handling |
|---|---|---|---|---|
| `AttendanceService` | Mark attendance | Call | Direct call | Log error, teacher can mark manually |
| `DPDP_COMPLIANCE_SPEC.md` | Consent flow | Call | Direct call | Block enrollment if no consent |
| `StudentsTable` | Student info, photoUrl | Read | Direct DB | Required |
| `ID_CARD_GENERATION_SPEC.md` | ID card photos | Read | Direct DB | Optional photo source |
| `ClassesTable` | Class info | Read | Direct DB | Required |
| `Notify.kt` | Attendance notifications | Call | Direct call | Log error, continue |

### External Integrations

| System | Integration Point | Direction | Protocol | Error Handling |
|---|---|---|---|---|
| TensorFlow Lite (on-device) | Face vector extraction | Call | Local ML model | Fallback to manual attendance |
| AWS Rekognition (optional) | Server-side face API | Call | HTTP API | Fallback to on-device or manual |
| Google Face API (optional) | Server-side face API | Call | HTTP API | Fallback to on-device or manual |

### Integration Patterns

- **On-device extraction (recommended):** TFLite model extracts 512-dim vector → server matches against enrolled vectors
- **Server-side extraction (optional):** Image uploaded → server API extracts vector → matches → deletes image
- **Hybrid:** On-device extraction + server-side matching (recommended approach)
- **Consent:** DPDP consent flow → consent recorded → enrollment unblocked
- **Attendance:** Face recognition results → `AttendanceService.markAttendance()` → existing attendance flow

---

## 16. Security

### Authentication

- Admin endpoints: school admin role + JWT auth via `requireAuth()`
- Teacher endpoints: teacher role + JWT auth
- Parent endpoints: parent role + JWT auth + parent-student relationship verified

### Authorization

- Enroll: school admin only
- Recognize: teacher (own class) or school admin
- Delete enrollment: school admin or parent (own child, via consent withdrawal)
- Consent: parent (own child) only
- View logs: school admin or teacher (own class)

### Data Protection

- Face vectors: encrypted at rest (AES-256)
- Raw images: deleted immediately after vector extraction (within 1 second)
- Face vectors never exposed in API responses or DTOs
- Face vectors never cached locally on client
- Encryption key managed by server (not accessible to client)

### Input Validation

- `student_id`: valid UUID
- `photo_url`: valid URL, image file (JPEG/PNG/HEIC)
- `class_id`: valid UUID
- `date`: valid date, not in future
- `captured_images`: non-empty list, max 50 images per session
- Image size: max 5MB per image

### Rate Limiting

- Enroll: 10 per minute per admin
- Recognize: 5 per minute per teacher
- Consent: 1 per minute per parent

### Audit Logging

- Enrollment: admin ID, student ID, timestamp, consent status
- Recognition: teacher ID, class ID, date, captured/matched/unmatched counts, device type
- Deletion: admin/parent ID, student ID, timestamp, reason (consent withdrawal or admin action)
- Consent: parent ID, child ID, timestamp, consent given/withdrawn

### PII Handling

- Face vectors are biometric data (special category under DPDP)
- Raw images deleted immediately — no biometric images stored
- Face vectors encrypted at rest — only decrypted during matching
- Face vectors never shared externally
- Consent required before any biometric data processing
- Consent withdrawal = permanent deletion (hard delete)

### Multi-tenant Isolation

- All face enrollments have `school_id` — school-scoped
- Face matching queries filtered by `school_id` and `class_id`
- No cross-school face matching
- Encryption key per school (optional, for enhanced isolation)

---

## 17. Performance & Scalability

### Expected Scale

- Enrollment: ~200-500 students per school (one-time, during admission)
- Recognition: 1-5 sessions per day per class, ~30-40 students per class
- Face vectors: ~2KB each (encrypted), ~200-500 per school = ~1MB total
- Matching: 30-40 captured faces × 30-40 enrolled vectors = ~1600 comparisons per session

### Query Optimization

- Face enrollments: fetched by `school_id` + `class_id` (via student-class relationship)
- Attendance logs: indexed on `(school_id, class_id, date)`
- Matching: in-memory cosine similarity comparison (vectors loaded from DB)

### Indexing Strategy

- `face_enrollments.student_id` — UNIQUE index
- `face_enrollments.school_id` — for school-scoped queries
- `face_attendance_logs(school_id, class_id, date)` — log lookup

### Caching Strategy

N/A — face vectors NOT cached (security concern). Fetched from DB during matching. Attendance logs cached locally (5-minute TTL).

### Pagination

- Attendance logs: 20 per page
- Enrollment list: 50 per page

### Connection Pooling

Uses existing HikariCP connection pool. No additional pooling needed.

### Async Processing

- Enrollment: synchronous (admin waits for confirmation)
- Recognition: synchronous (teacher waits for results)
- Consent: synchronous (parent waits for confirmation)
- Notifications: async (fire-and-forget via `Notify.kt`)

### Scalability Concerns

- Face vector storage: ~2KB × 500 students = ~1MB per school. Negligible.
- Matching performance: ~1600 cosine similarity comparisons per session. < 5 seconds. Manageable.
- On-device model: TFLite model ~5-10MB. Acceptable for mobile.
- Server load: matching is CPU-bound but fast (in-memory float array operations).

---

## 18. Edge Cases

| # | Scenario | Expected Behavior |
|---|---|---|
| EC-1 | Student without consent | Enrollment blocked. Show "Parent consent required." |
| EC-2 | Blurry or dark photo | Face extraction fails. Show "Unable to extract face. Use clear, front-facing photo." |
| EC-3 | Multiple faces in one photo | Extract largest/most prominent face. Log warning. |
| EC-4 | No faces detected in capture | Return empty result. Show "No faces detected. Ensure students face camera." |
| EC-5 | Student not enrolled | Unmatched. Teacher can manually identify. |
| EC-6 | Similar faces (twins) | Both may match above threshold. Teacher override resolves. |
| EC-7 | Student wearing mask/face covering | Face extraction may fail. Unmatched. Teacher manual marking. |
| EC-8 | Consent withdrawn after enrollment | Face vector permanently deleted. Student reverts to manual attendance. |
| EC-9 | Re-enrollment with new photo | Delete old enrollment, create new with updated vector. |
| EC-10 | Camera/tablet unavailable | Fallback to manual attendance. |
| EC-11 | On-device model not loaded | Fallback to server-side extraction (if configured) or manual attendance. |
| EC-12 | All captured faces unmatched | All students remain ABSENT. Teacher reviews and marks manually. |

---

## 19. Error Handling

### Error Response Format

Standard `ApiResponse` error format.

### Error Codes

| Code | HTTP Status | Description | User Message |
|---|---|---|---|
| `CONSENT_REQUIRED` | 403 | Parent has not consented | "Parent consent required for facial recognition." |
| `FACE_EXTRACTION_FAILED` | 422 | Unable to extract face vector | "Unable to extract face from photo. Use clear, front-facing photo." |
| `NO_FACES_DETECTED` | 422 | No faces in captured image | "No faces detected in capture." |
| `NO_ENROLLED_STUDENTS` | 400 | No enrolled students in class | "No students enrolled in this class. Please enroll first." |
| `ENROLLMENT_NOT_FOUND` | 404 | Enrollment not found for student | "Face enrollment not found for this student." |
| `STUDENT_NOT_FOUND` | 404 | Student ID not found | "Student not found." |
| `NOT_AUTHORIZED` | 403 | Role not authorized | "You are not authorized to perform this action." |
| `NOT_PARENT_OF_STUDENT` | 403 | Parent not linked to student | "You can only manage consent for your own children." |

### Error Handling Strategy

- **Extraction failure:** Return error. Admin/teacher can retry with better photo.
- **Matching failure:** Return error. Teacher falls back to manual attendance.
- **Consent not given:** Return 403. Admin informed to ask parent for consent.
- **Enrollment not found:** Return 404. Student not enrolled in face recognition.
- **Model not loaded:** Fallback to server-side or manual attendance.

### Retry Strategy

- Enrollment: admin retries with different photo
- Recognition: teacher retries capture or falls back to manual
- Consent: parent retries (usually no retry needed)

### Fallback Behavior

- Face extraction fails: use different photo or manual enrollment
- Face matching fails: manual attendance
- On-device model unavailable: server-side extraction (if configured) or manual attendance
- No enrolled students: manual attendance
- All faces unmatched: teacher marks manually

---

## 20. Analytics & Reporting

### Analytics Dashboard Data

| Metric | Source | Derivation |
|---|---|---|
| Enrollment rate | `face_enrollments` | Enrolled / total students per school |
| Consent rate | `face_enrollments` (consent_given) | Consented / total students |
| Face attendance usage | `face_attendance_logs` | Sessions per day per class |
| Match rate | `face_attendance_logs` | matched_count / captured_count |
| Unmatched rate | `face_attendance_logs` | unmatched_count / captured_count |
| Override rate | Audit logs | Teacher overrides / total matched |
| Device distribution | `face_attendance_logs` | Count by device_type |

### Export Capabilities

- Face enrollment report (CSV) — student, consent status, enrollment date
- Face attendance usage report (CSV) — class, date, captured/matched/unmatched counts
- Consent report (CSV) — student, consent status, consent date

### Report Types

| Report | Format | Frequency | Recipient |
|---|---|---|---|
| Face recognition usage | JSON (API) | Monthly | Product Team |
| Consent compliance | JSON (API) | Monthly | Compliance Team |
| Match accuracy | JSON (API) | Monthly | Product Team |

---

## 21. Testing Strategy

### Unit Tests

- `FaceRecognitionService.enroll()` — consent verification, vector extraction, encryption, storage
- `FaceRecognitionService.recognize()` — vector extraction, cosine similarity matching, attendance marking
- `FaceRecognitionService.deleteEnrollment()` — hard delete, consent withdrawal
- Cosine similarity calculation — threshold edge cases (0.84, 0.85, 0.86)
- Encryption/decryption — AES-256 round-trip
- Consent state transitions

### Integration Tests

- Full enrollment flow: consent → enroll → verify storage → verify raw image deleted
- Full recognition flow: capture → extract → match → mark attendance → verify records
- Consent withdrawal: enroll → withdraw consent → verify enrollment deleted
- Teacher override: recognize → override → verify attendance updated
- Permission checks: admin-only enroll, teacher-only recognize, parent-only consent
- Multi-tenant: verify school-scoped matching (no cross-school)

### E2E Tests

- Admin enrolls student → teacher captures faces → attendance auto-marked → teacher confirms
- Parent consents → admin enrolls → parent withdraws → enrollment deleted
- Teacher captures → unmatched faces → teacher manually identifies → attendance marked

### Performance Tests

- Face vector extraction: < 2 seconds per image (on-device)
- Face matching: < 5 seconds for class of 40 students
- Enrollment: < 5 seconds total (extract + encrypt + store)
- Raw image deletion: within 1 second of extraction

### Test Data

- 10 student photos (various lighting, angles, expressions)
- 5 twin/similar-looking student pairs (edge case testing)
- Photos with multiple faces (edge case)
- Photos with no faces (edge case)
- Mock face detector (returns controlled vectors)
- Pre-seeded consent records

### Test Environment

- Test database with face enrollment tables
- Mock TFLite model (returns controlled 512-dim vectors)
- Mock DPDP consent service
- Test JWT tokens for all roles
- Test encryption key

---

## 22. Acceptance Criteria

- [ ] Student face prints enrolled with parental consent
- [ ] Classroom capture recognizes enrolled students
- [ ] Auto-marks attendance for matched students
- [ ] Unmatched faces flagged for teacher review
- [ ] Teacher can override/correct attendance
- [ ] Raw images deleted after vector extraction
- [ ] Consent withdrawal deletes face print
- [ ] DPDP compliance: consent, encryption, audit
- [ ] Face vectors encrypted at rest (AES-256)
- [ ] Face vectors never exposed in API responses
- [ ] On-device extraction works offline
- [ ] Cosine similarity threshold ≥ 0.85 for match

---

## 23. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 2 days | DB migration `migration_086_facial_recognition.sql`, Exposed tables, register in `DatabaseFactory` |
| 2 | 3 days | Face vector extraction (on-device TFLite model, expect/actual for Android/iOS/Web) |
| 3 | 3 days | `FaceRecognitionService` (enroll, recognize, match, delete) + AES-256 encryption |
| 4 | 2 days | Consent integration (DPDP compliance flow) |
| 5 | 2 days | API endpoints (admin enroll/recognize + parent consent) |
| 6 | 4 days | Client UI: `FaceEnrollmentScreen`, `FaceAttendanceScreen`, `FaceConsentScreen`, `FaceAttendanceLogScreen` |
| 7 | 2 days | Tests: unit, integration, E2E |

### Pre-Implementation Checklist

- [ ] Verify `DPDP_COMPLIANCE_SPEC.md` is implemented and consent flow available
- [ ] Verify `OFFLINE_MODE_SPEC.md` is implemented
- [ ] Verify TFLite face recognition model is available for Android/iOS
- [ ] Verify AES-256 encryption utilities are available
- [ ] Verify `AttendanceRecordsTable` and `AttendanceService` are available
- [ ] Verify `StudentsTable` has `photoUrl` field
- [ ] Verify camera permissions in app manifest

---

## 24. File-Level Impact Analysis

### Server

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | `FaceEnrollmentsTable`, `FaceAttendanceLogsTable` |
| `server/.../db/DatabaseFactory.kt` | Modify | Register new tables in `allTables` |
| `server/.../feature/face/FaceRecognitionService.kt` | **New** | Core service (enroll, recognize, match, delete) |
| `server/.../feature/face/FaceRouting.kt` | **New** | API endpoints |
| `server/.../feature/face/FaceEncryption.kt` | **New** | AES-256 encryption utilities |
| `docs/db/migration_086_facial_recognition.sql` | **New** | DDL |

### Shared (KMP)

| File | Change Type | Description |
|---|---|---|
| `shared/.../core/ml/FaceDetector.kt` | **New** + expect/actual | On-device face detection and vector extraction |
| `shared/.../face/domain/model/FaceRecognitionModels.kt` | **New** | DTOs, domain models, enums |
| `shared/.../face/domain/repository/FaceRecognitionRepository.kt` | **New** | Repository interface |
| `shared/.../face/data/remote/FaceRecognitionApi.kt` | **New** | HTTP API definitions |

### Client (Compose)

| File | Change Type | Description |
|---|---|---|
| `composeApp/.../ui/v2/screens/admin/FaceEnrollmentScreen.kt` | **New** | Enrollment UI |
| `composeApp/.../ui/v2/screens/teacher/FaceAttendanceScreen.kt` | **New** | Classroom capture + results + override |
| `composeApp/.../ui/v2/screens/parent/FaceConsentScreen.kt` | **New** | Consent/withdraw consent |
| `composeApp/.../ui/v2/screens/admin/FaceAttendanceLogScreen.kt` | **New** | View attendance logs |

### Platform-Specific (expect/actual)

| File | Platform | Description |
|---|---|---|
| `androidMain/.../core/ml/FaceDetector.kt` | Android | TensorFlow Lite implementation |
| `iosMain/.../core/ml/FaceDetector.kt` | iOS | Core ML implementation |
| `jsMain/.../core/ml/FaceDetector.kt` | Web | TensorFlow.js implementation |

---

## 25. Future Enhancements

| # | Enhancement | Priority | Effort | Notes |
|---|---|---|---|---|
| F-1 | CCTV integration | Medium | L | Use existing CCTV cameras for face capture |
| F-2 | Staff face attendance | Medium | M | Extend to staff/teacher attendance |
| F-3 | Visitor face recognition | Low | M | Recognize and log visitors |
| F-4 | Face liveness detection | High | M | Prevent photo spoofing (anti-spoofing) |
| F-5 | Multi-angle enrollment | Medium | S | Enroll from multiple angles for better accuracy |
| F-6 | Automatic re-enrollment | Low | S | Re-enroll when student appearance changes significantly |
| F-7 | Face recognition analytics | Low | M | Accuracy trends, match rate over time |
| F-8 | Batch enrollment from ID cards | Medium | S | Enroll all students from ID card photos |
| F-9 | Real-time face tracking | Low | L | Continuous face tracking during class (not just entry) |
| F-10 | Cross-class matching | Low | M | Match against all school enrollments (not just class) |

---

## Appendix A: Sequence Diagrams

### A.1 Face Enrollment Flow

```
Admin (app)         Server              DB              On-Device ML
  │                    │                  │                │
  │  POST /face/enroll │                  │                │
  │  {student_id,      │                  │                │
  │   photo_url}       │                  │                │
  │  ───────────────>  │                  │                │
  │                    │──verify consent─>│                │
  │                    │←──consent ok─────│                │
  │                    │                  │                │
  │                    │──extract vector─────────────────>│
  │                    │←──512-dim vector────────────────│
  │                    │                  │                │
  │                    │──encrypt vector──│                │
  │                    │──insert enrollment>│              │
  │                    │←──success────────│                │
  │                    │                  │                │
  │                    │──delete raw photo│                │
  │                    │                  │                │
  │  ←──201: EnrollDto│                  │                │
  │                    │                  │                │
  │  "Face print       │                  │                │
  │   enrolled"        │                  │                │
  │                    │                  │                │
```

### A.2 Face Recognition Flow

```
Teacher (app)       Server              DB              On-Device ML
  │                    │                  │                │
  │  POST /face/       │                  │                │
  │  recognize         │                  │                │
  │  {class_id, date,  │                  │                │
  │   captured_images} │                  │                │
  │  ───────────────>  │                  │                │
  │                    │                  │                │
  │                    │──get enrolled──>│                │
  │                    │  vectors for    │                │
  │                    │  class          │                │
  │                    │←──vector list───│                │
  │                    │                  │                │
  │                    │──for each image: │                │
  │                    │  extract vector─────────────────>│
  │                    │←──512-dim vector────────────────│
  │                    │                  │                │
  │                    │  compare cosine  │                │
  │                    │  similarity      │                │
  │                    │  ≥ 0.85 = match  │                │
  │                    │                  │                │
  │                    │──mark attendance>│                │
  │                    │  (matched=PRESENT)│               │
  │                    │←──success────────│                │
  │                    │                  │                │
  │                    │──insert log──────>│               │
  │                    │←──success────────│                │
  │                    │                  │                │
  │  ←──200: Result───│                  │                │
  │  (matched +        │                  │                │
  │   unmatched)       │                  │                │
  │                    │                  │                │
  │  teacher reviews   │                  │                │
  │  teacher overrides │                  │                │
  │  teacher confirms  │                  │                │
  │                    │                  │                │
```

### A.3 Consent Withdrawal Flow

```
Parent (app)        Server              DB
  │                    │                  │
  │  DELETE /face/     │                  │
  │  consent/{childId} │                  │
  │  ───────────────>  │                  │
  │                    │──verify parent──>│
  │                    │  -child relation │
  │                    │←──ok─────────────│
  │                    │                  │
  │                    │──hard delete────>│
  │                    │  enrollment      │
  │                    │←──deleted────────│
  │                    │                  │
  │  ←──204: No Content│                  │
  │                    │                  │
  │  "Face print       │                  │                │
  │   permanently      │                  │                │
  │   deleted"         │                  │                │
  │                    │                  │                │
```

---

## Appendix B: Domain Model / ER Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                    face_enrollments (new)                             │
│  id (PK)                                                              │
│  school_id, student_id (UNIQUE)                                       │
│  face_vector (BYTEA, encrypted AES-256)                               │
│  enrolled_at, consent_given, consent_at                               │
│  is_active                                                            │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│                  face_attendance_logs (new)                           │
│  id (PK)                                                              │
│  school_id, class_id, date                                            │
│  captured_count, matched_count, unmatched_count                       │
│  device_type (tablet|camera)                                          │
│  processed_at                                                         │
│  INDEX: (school_id, class_id, date)                                   │
└──────────────────────────────────────────────────────────────────────┘

Existing tables used:
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│ students         │  │ classes          │  │ schools          │
│ (existing)       │  │ (existing)       │  │ (existing)       │
│ photoUrl, name   │  │ class info       │  │ school info      │
└──────────────────┘  └──────────────────┘  └──────────────────┘
┌──────────────────┐
│ attendance_records│
│ (existing)        │
│ student, class,   │
│ date, status      │
└──────────────────┘
```

---

## Appendix C: Event Flow

### Domain Events

| Event | Emitter | Consumers | Payload | Side Effects |
|---|---|---|---|---|
| `FaceEnrolled` | `FaceRecognitionService.enroll()` | None (logged) | `studentId, schoolId, enrolledAt` | Audit log |
| `FaceRecognized` | `FaceRecognitionService.recognize()` | `AttendanceService` | `classId, date, matchedCount, unmatchedCount` | Attendance marked |
| `FaceEnrollmentDeleted` | `FaceRecognitionService.deleteEnrollment()` | None (logged) | `studentId, reason` | Audit log |
| `ConsentGiven` | Consent flow | `FaceRecognitionService` | `childId, parentId, consentAt` | Enrollment unblocked |
| `ConsentWithdrawn` | Consent flow | `FaceRecognitionService` | `childId, parentId` | Enrollment deleted |

### Event Delivery Guarantees

- Face enrolled event: emitted synchronously (fire-and-forget logging)
- Face recognized event: emitted synchronously, attendance marking synchronous
- Face enrollment deleted event: emitted synchronously (fire-and-forget logging)
- Consent events: emitted synchronously, enrollment operations synchronous

---

## Appendix D: Configuration

### Environment Variables

| Variable | Default | Description |
|---|---|---|
| `FACIAL_RECOGNITION_ENABLED` | `false` | Enable/disable feature (off by default, opt-in) |
| `FACE_MATCH_THRESHOLD` | `0.85` | Cosine similarity threshold for match |
| `FACE_VECTOR_DIMENSIONS` | `512` | Embedding dimensions |
| `FACE_VECTOR_ENCRYPTION_KEY` | — | AES-256 encryption key for face vectors |
| `FACE_EXTRACTION_MODE` | `on_device` | Extraction mode: on_device, server_api, hybrid |
| `RAW_IMAGE_DELETE_DELAY_MS` | `1000` | Delay before raw image deletion |
| `MAX_CAPTURED_IMAGES_PER_SESSION` | `50` | Max images per recognition session |

### Feature Flags

| Flag | Default | Description |
|---|---|---|
| `FACIAL_RECOGNITION_ENABLED` | `false` | Enable/disable facial recognition feature |
| `FACE_EXTRACTION_ON_DEVICE` | `true` | Use on-device extraction (vs server API) |
| `FACE_LIVENESS_CHECK` | `false` | Enable liveness detection (anti-spoofing) |

### School-Level Settings

| Setting | Default | Description |
|---|---|---|
| `facial_recognition_enabled` | `false` | Per-school enable/disable |
| `face_match_threshold` | `0.85` | Per-school match threshold |
| `face_extraction_mode` | `on_device` | Per-school extraction mode |

---

## Appendix E: Migration & Rollback

### Migration: `migration_086_facial_recognition.sql`

```sql
-- Migration 086: Facial Recognition Attendance
-- Creates face_enrollments and face_attendance_logs tables

BEGIN;

CREATE TABLE IF NOT EXISTS face_enrollments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    student_id      UUID NOT NULL UNIQUE,
    face_vector     BYTEA NOT NULL,
    enrolled_at     TIMESTAMP NOT NULL DEFAULT now(),
    consent_given   BOOLEAN NOT NULL DEFAULT false,
    consent_at      TIMESTAMP,
    is_active       BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE IF NOT EXISTS face_attendance_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    class_id        UUID NOT NULL,
    date            DATE NOT NULL,
    captured_count  INTEGER NOT NULL DEFAULT 0,
    matched_count   INTEGER NOT NULL DEFAULT 0,
    unmatched_count INTEGER NOT NULL DEFAULT 0,
    device_type     VARCHAR(16),
    processed_at    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_face_att_logs_school_class_date
    ON face_attendance_logs (school_id, class_id, date);

COMMIT;
```

### Rollback: `migration_086_rollback.sql`

```sql
BEGIN;
DROP TABLE IF EXISTS face_attendance_logs;
DROP TABLE IF EXISTS face_enrollments;
COMMIT;
```

### Migration Validation

- Verify `face_enrollments` table created with UNIQUE constraint on `student_id`
- Verify `face_attendance_logs` table created with index
- Run `SELECT count(*) FROM face_enrollments` — should be 0 (new feature)
- Run `SELECT count(*) FROM face_attendance_logs` — should be 0 (new feature)

---

## Appendix F: Observability

### Structured Logging

| Log Level | Event | Context Fields |
|---|---|---|
| INFO | Face enrolled | `studentId, schoolId, enrolledAt` |
| INFO | Face recognized | `classId, date, capturedCount, matchedCount, unmatchedCount` |
| INFO | Face enrollment deleted | `studentId, reason` |
| INFO | Consent given | `childId, parentId, consentAt` |
| INFO | Consent withdrawn | `childId, parentId` |
| WARN | Face extraction failed | `studentId, error` |
| WARN | No faces detected | `classId, date` |
| WARN | Low match rate | `classId, date, matchRate` |
| WARN | High override rate | `classId, date, overrideRate` |
| ERROR | Encryption/decryption failed | `studentId, error` |
| ERROR | Recognition service error | `classId, date, error` |

### Metrics

| Metric | Type | Labels | Description |
|---|---|---|---|
| `face_enrollments_total` | Counter | `school_id` | Total face enrollments |
| `face_enrollments_active` | Gauge | `school_id` | Current active enrollments |
| `face_recognition_sessions` | Counter | `school_id, device_type` | Total recognition sessions |
| `face_match_rate` | Histogram | `school_id` | Match rate per session |
| `face_unmatched_rate` | Histogram | `school_id` | Unmatched rate per session |
| `face_override_rate` | Histogram | `school_id` | Teacher override rate |
| `face_extraction_duration` | Histogram | `school_id` | Vector extraction time |
| `face_matching_duration` | Histogram | `school_id` | Matching time per session |
| `consent_given_total` | Counter | `school_id` | Total consents given |
| `consent_withdrawn_total` | Counter | `school_id` | Total consents withdrawn |

### Health Checks

| Check | Endpoint | Description |
|---|---|---|
| Face recognition service | `/health/face-recognition` | Verify service and DB accessible |
| Encryption service | `/health/face-encryption` | Verify encryption key available |

### Alerts

| Alert | Condition | Severity | Notification |
|---|---|---|---|
| Low match rate | < 70% over 1 week | Warning | Email to admin (enrollment quality?) |
| High override rate | > 20% over 1 week | Warning | Email to admin (matching accuracy?) |
| Encryption service down | Health check failed | Critical | Email + SMS to dev team |
| Consent withdrawal spike | > 10% withdrawals in 1 week | Warning | Email to admin (privacy concern?) |
| Face recognition service down | Health check failed | Critical | Email to dev team |

### Dashboards

| Dashboard | Panels | Audience |
|---|---|---|
| Face Recognition Overview | Enrollments, active enrollments, consent rate | Product Team |
| Recognition Performance | Match rate, unmatched rate, override rate, extraction time | Product Team |
| Consent Tracking | Consents given, withdrawn, active consent rate | Compliance Team |
| Usage | Sessions per day, device distribution, class coverage | Product Team |

### Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Privacy breach (face vectors leaked) | Low | Critical | AES-256 encryption. Vectors never in DTOs. No caching. |
| Misidentification (wrong student marked) | Medium | Medium | Teacher override. Cosine threshold 0.85. Audit trail. |
| Consent not obtained | Low | Critical | DPDP consent flow. Enrollment blocked without consent. |
| On-device model accuracy | Medium | Medium | Use high-quality TFLite model. Multi-angle enrollment (future). |
| Spoofing (photo of student) | Medium | High | Liveness detection (future). Teacher oversight. |
| Low enrollment rate | Medium | Low | Admin can bulk enroll from ID card photos. |
| DPDP non-compliance | Low | Critical | Consent flow, encryption, audit, opt-out, raw image deletion. |
| Model not available on device | Low | Medium | Fallback to server-side or manual attendance. |
