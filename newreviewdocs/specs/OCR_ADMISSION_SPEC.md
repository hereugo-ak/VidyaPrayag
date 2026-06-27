# OCR Admission Form Scanner — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** `AI_INFRASTRUCTURE_SPEC.md`

---

## 1. Feature Overview

OCR-based admission form scanning that extracts student data from photographed paper forms and pre-fills the digital admission enquiry. Uses LLM vision capabilities (GPT-4o vision / Gemini) to read handwritten and printed forms.

### Goals

- Admin/parent photographs admission form → OCR extracts structured data
- Pre-fills `AdmissionEnquiriesTable` with extracted data
- Confidence score per field — low confidence fields flagged for manual review
- Support multiple form layouts (school-specific templates)
- Reduce manual data entry during admission season

---

## 2. Current System Assessment

- `AdmissionEnquiriesTable` (`Tables.kt:330-344`) — stores admission enquiries with `studentName`, `parentName`, `phone`, `className`, `section`, `status`
- `SchoolMediaTable` — existing media storage infrastructure
- No OCR or image processing exists

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Upload/capture photo of admission form |
| FR-2 | LLM vision extracts: student name, parent name, phone, date of birth, class, address, previous school |
| FR-3 | Confidence score per extracted field (0-1) |
| FR-4 | Low-confidence fields (< 0.7) highlighted for manual review |
| FR-5 | Admin reviews and edits extracted data before saving |
| FR-6 | Save as admission enquiry in `AdmissionEnquiriesTable` |
| FR-7 | Support English and Hindi forms |

---

## 4. Database Design

### 4.1 New Table: `ocr_admission_scans`

```sql
CREATE TABLE ocr_admission_scans (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    image_url       TEXT NOT NULL,                 -- Supabase Storage URL
    extracted_data  TEXT NOT NULL,                 -- JSON: [{"field": "student_name", "value": "...", "confidence": 0.95}]
    status          VARCHAR(16) NOT NULL DEFAULT 'pending', -- pending | reviewed | saved
    admission_enquiry_id UUID,                     -- FK admission_enquiries.id (once saved)
    scanned_by      UUID,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
```

---

## 5. Backend Architecture

### 5.1 OcrAdmissionService

```kotlin
class OcrAdmissionService(private val aiService: AiService) {
    suspend fun scan(schoolId: UUID, imageUrl: String): OcrScanDto {
        // 1. Call LLM vision with image + extraction prompt
        val result = aiService.complete(schoolId, null, "ocr_admission", "extract_form_v1",
            mapOf("image_url" to imageUrl))
        // 2. Parse structured response (field, value, confidence)
        // 3. Store in ocr_admission_scans
        // 4. Return for admin review
    }

    suspend fun saveToEnquiry(scanId: UUID, editedData: Map<String, String>): UUID {
        // 1. Read scan
        // 2. Apply admin edits
        // 3. Create AdmissionEnquiriesTable row
        // 4. Update scan status = 'saved'
    }
}
```

### 5.2 Vision Prompt

```
System: You are an OCR system extracting data from school admission forms.
Extract the following fields from the image. For each field, provide the value and a confidence score (0-1).
Output JSON: [{"field": "student_name", "value": "...", "confidence": 0.95}, ...]

Fields: student_name, parent_name, phone, date_of_birth, class_applied, address, previous_school, gender

User: [image attached]
```

---

## 6. API Contracts

```
POST /api/v1/school/admission/ocr-scan
{
  "image_url": "https://supabase.co/..."
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "scan_id": "uuid",
    "extracted_data": [
      {"field": "student_name", "value": "Aarav Sharma", "confidence": 0.95},
      {"field": "parent_name", "value": "Rajesh Sharma", "confidence": 0.92},
      {"field": "phone", "value": "+919876543210", "confidence": 0.88},
      {"field": "date_of_birth", "value": "2015-06-15", "confidence": 0.65}
    ],
    "low_confidence_fields": ["date_of_birth"]
  }
}
```

```
POST /api/v1/school/admission/ocr-scan/{scanId}/save
{
  "student_name": "Aarav Sharma",
  "parent_name": "Rajesh Sharma",
  "phone": "+919876543210",
  "date_of_birth": "2015-06-15",
  "class_applied": "Grade 1"
}
```

---

## 7. Acceptance Criteria

- [ ] Photo of admission form → structured data extracted
- [ ] Confidence score per field
- [ ] Low-confidence fields flagged
- [ ] Admin can edit before saving
- [ ] Saved as admission enquiry
- [ ] Works with printed and handwritten forms (English + Hindi)

---

## 8. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration, Exposed table |
| 2 | 2 days | OcrAdmissionService + vision prompt |
| 3 | 2 days | API endpoints |
| 4 | 2 days | Client UI (camera capture, review screen, edit fields) |
| 5 | 1 day | Tests |

---

## 9. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | `OcrAdmissionScansTable` |
| `server/.../feature/ai/ocr/OcrAdmissionService.kt` | New | Core service |
| `server/.../feature/ai/ocr/OcrRouting.kt` | New | API endpoints |
| `docs/db/migration_044_ocr_admission.sql` | New | DDL |
| `composeApp/.../ui/v2/screens/admin/OcrScanScreen.kt` | New | Scan + review UI |
