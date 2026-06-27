# Facial Recognition Attendance — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** `OFFLINE_MODE_SPEC.md`, `DPDP_COMPLIANCE_SPEC.md`
> **Source:** `COMPETITIVE_GAP_ANALYSIS.md` — emerging trend

---

## 1. Feature Overview

Automated attendance marking via facial recognition: camera captures student faces at classroom entry, AI matches against enrolled face prints, and marks attendance. Eliminates manual roll call and reduces proxy attendance.

### Goals

- Enroll student face prints (photo during admission/ID card generation)
- Classroom camera or tablet camera captures faces at entry
- AI matches captured faces against enrolled prints
- Auto-mark attendance (present/absent) for matched students
- Privacy: face prints stored as encrypted vectors, not raw images
- Manual override: teacher can correct misidentification
- DPDP compliance: explicit consent for biometric data

---

## 2. Current System Assessment

- `AttendanceRecordsTable` — manual attendance marking (PRESENT/ABSENT/LATE)
- `StudentsTable` — has `photoUrl` for student photos
- `ID_CARD_GENERATION_SPEC.md` — ID card photos can be used for enrollment
- No facial recognition or biometric system
- `COMPETITIVE_GAP_ANALYSIS.md`: facial recognition as emerging trend

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Enroll face print: capture photo → extract face vector (embedding) → store encrypted |
| FR-2 | Consent: parent must consent to biometric data usage (via `DPDP_COMPLIANCE_SPEC.md`) |
| FR-3 | Classroom capture: tablet camera or dedicated camera captures faces at entry |
| FR-4 | AI matching: compare captured face vectors against enrolled vectors (cosine similarity ≥ 0.85) |
| FR-5 | Auto-mark attendance for matched students |
| FR-6 | Unmatched faces flagged for teacher review |
| FR-7 | Teacher override: correct misidentification or mark unmatched students |
| FR-8 | Privacy: raw images deleted after vector extraction; only encrypted vectors stored |
| FR-9 | Opt-out: parent can withdraw consent → face print deleted |

---

## 4. Database Design

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

---

## 5. Backend Architecture

### 5.1 FaceRecognitionService

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

### 5.2 Face Vector Extraction

Options:
- **Server-side API:** AWS Rekognition, Google Face API, or Azure Face API
- **On-device:** TensorFlow Lite face recognition model (offline, privacy-friendly)
- **Hybrid:** On-device extraction + server-side matching

Recommended: on-device extraction (privacy) + server-side matching (performance).

---

## 6. API Contracts

```
# Admin
POST /api/v1/school/face/enroll  { student_id, photo_url }
DELETE /api/v1/school/face/enroll/{studentId}  -- consent withdrawal
POST /api/v1/school/face/recognize  { class_id, date, captured_images: [...] }
GET /api/v1/school/face/attendance-logs?class_id={uuid}&date={YYYY-MM-DD}

# Parent (consent)
POST /api/v1/parent/face/consent  { child_id, consent: true }
DELETE /api/v1/parent/face/consent/{childId}  -- withdraw consent
```

---

## 7. Privacy & DPDP Compliance

- **Consent required:** Parent must explicitly consent via `DPDP_COMPLIANCE_SPEC.md` consent flow
- **Data category:** Biometric (special category under DPDP)
- **Raw images:** Deleted immediately after vector extraction
- **Face vectors:** Encrypted (AES-256) at rest
- **Opt-out:** Parent can withdraw consent → face vector permanently deleted
- **No external sharing:** Face data never shared with third parties
- **Audit log:** All face recognition operations logged

---

## 8. Acceptance Criteria

- [ ] Student face prints enrolled with parental consent
- [ ] Classroom capture recognizes enrolled students
- [ ] Auto-marks attendance for matched students
- [ ] Unmatched faces flagged for teacher review
- [ ] Teacher can override/correct attendance
- [ ] Raw images deleted after vector extraction
- [ ] Consent withdrawal deletes face print
- [ ] DPDP compliance: consent, encryption, audit

---

## 9. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 2 days | DB migration, Exposed tables |
| 2 | 3 days | Face vector extraction (on-device TFLite model) |
| 3 | 3 days | FaceRecognitionService (enroll, recognize, match) |
| 4 | 2 days | Consent integration (DPDP) |
| 5 | 2 days | API endpoints |
| 6 | 4 days | Client UI (enrollment, classroom capture, teacher review, consent) |
| 7 | 2 days | Tests |

---

## 10. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | 2 face recognition tables |
| `server/.../feature/face/FaceRecognitionService.kt` | New | Core service |
| `docs/db/migration_085_facial_recognition.sql` | New | DDL |
| `shared/.../core/ml/FaceDetector.kt` | New + expect/actual | On-device face detection |
| `composeApp/.../ui/v2/screens/admin/FaceEnrollmentScreen.kt` | New | Enrollment UI |
| `composeApp/.../ui/v2/screens/teacher/FaceAttendanceScreen.kt` | New | Classroom capture |
