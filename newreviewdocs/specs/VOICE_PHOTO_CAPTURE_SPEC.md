# Voice & Photo Capture — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** `AI_INFRASTRUCTURE_SPEC.md`
> **Source:** `DIFFERENTIATING_FEATURES.md` §5.3
> **Template:** `_SPEC_TEMPLATE.md` v1 (25 mandatory + 6 optional sections)

---

## 1. Feature Overview

### What

Voice-to-text input and photo capture with AI processing for teachers and parents. Enables hands-free homework assignment dictation, voice notes, photo-to-text extraction, and visual question answering.

### Why — Product Rationale

Many teachers in India are not comfortable typing on mobile keyboards, especially in regional languages. Voice input and photo capture reduce friction in content creation — dictating homework, sending messages, or photographing worksheets. This is a **medium-priority differentiating feature** (Phase 3, effort M) that improves usability for non-tech-savvy users.

### What Stands Out (Competitive Moat)

From `DIFFERENTIATING_FEATURES.md` §5.3:
> "Voice & Photo Capture — voice-to-text, photo OCR, visual question answering."

No major Indian school ERP offers voice input or photo-to-text for homework creation. The key moat is **multi-language voice recognition** (10 Indian languages) combined with **AI-powered OCR and visual QA**.

### Goals

- Voice input: teacher dictates homework/announcement → AI transcribes to text
- Photo capture: teacher photographs worksheet → AI extracts text for homework description
- Voice notes: parent sends voice message (transcribed automatically)
- Visual question answering: student photographs problem → AI explains (via `VIDYASETU_AI_TUTOR_SPEC.md`)
- Multi-language voice recognition (English, Hindi, regional)

### Non-goals

- [ ] Real-time live transcription (recording → upload → transcribe flow)
- [ ] Voice commands for app navigation (voice is for text input only)
- [ ] Video capture or processing (photos only)
- [ ] Offline transcription (requires AI service)
- [ ] Voice biometrics or speaker identification
- [ ] Auto-translation between languages (transcription in spoken language only)
- [ ] Voice-to-text for student responses (teacher/parent only)

### Dependencies

- `AI_INFRASTRUCTURE_SPEC.md` — LLM with vision capabilities (GPT-4o, Gemini) and speech-to-text
- `HomeworkTable` — homework description (text input, currently manual)
- `MESSAGING_SYSTEM_SPEC.md` — text messages only, adding voice messages
- `VIDYASETU_AI_TUTOR_SPEC.md` — visual QA delegates to AI Tutor
- Supabase Storage — audio and image file storage

### Related Modules

- `server/.../feature/ai/` — AI module (transcription service)
- `shared/.../core/audio/` — platform audio recording
- `composeApp/.../ui/v2/components/` — voice input component
- `composeApp/.../ui/v2/screens/teacher/` — teacher homework/announcement screens
- `composeApp/.../ui/v2/screens/parent/` — parent messaging and AI tutor screens

---

## 2. Current System Assessment

### Existing Code

- No voice/speech recognition in app
- `HomeworkTable` — homework description is text input (manual typing)
- `MESSAGING_SYSTEM_SPEC.md` — text messages only, no voice messages
- `AI_INFRASTRUCTURE_SPEC.md` — LLM with vision capabilities (GPT-4o, Gemini)
- `DIFFERENTIATING_FEATURES.md` §5.3: Voice/Photo Capture, effort M

### Existing Database

- `HomeworkTable` — homework description (text)
- `MessagesTable` (from `MESSAGING_SYSTEM_SPEC.md`) — message body (text), no audio_url field
- No voice or audio storage tables
- Supabase Storage exists for file uploads

### Existing APIs

- Homework API (existing) — text-based homework creation
- Messaging API (existing) — text messages
- AI API (existing) — LLM completion endpoint
- No transcription or OCR endpoints

### Existing UI

- Homework composer (existing) — text input for homework description
- Messaging (existing) — text input for messages
- AI Tutor (existing) — text-based Q&A
- No voice input or photo capture UI

### Existing Services

- `AiService` — LLM completion (text and vision)
- `HomeworkService` — homework CRUD
- `MessagingService` — message CRUD
- No transcription service

### Existing Documentation

- `DIFFERENTIATING_FEATURES.md` §5.3 — Voice/Photo Capture
- `AI_INFRASTRUCTURE_SPEC.md` — AI service with vision
- `MESSAGING_SYSTEM_SPEC.md` — messaging system
- `VIDYASETU_AI_TUTOR_SPEC.md` — AI tutor

### Technical Debt

| # | Gap | Details |
|---|---|---|
| TD-1 | No audio recording | No `AudioRecorder` expect/actual implementation |
| TD-2 | No transcription service | No speech-to-text or OCR service |
| TD-3 | No voice input UI | No `VoiceInputField` composable |
| TD-4 | No voice messages | Messages table has no `audio_url` field |
| TD-5 | No photo capture for OCR | No photo-to-text flow |

### Gaps

| # | Gap | Impact | Severity |
|---|---|---|---|
| G1 | No voice input | Teachers must type manually (friction) | **High** |
| G2 | No photo OCR | Teachers must type worksheet content manually | **High** |
| G3 | No voice messages | Parents can't send voice notes | **Medium** |
| G4 | No visual QA | Students can't photograph problems for AI help | **Medium** |
| G5 | No multi-language voice | Only English typing, no regional language voice | **Medium** |

---

## 3. Functional Requirements

### FR-001
| Field | Value |
|---|---|
| **Title** | Voice Input |
| **Description** | Voice input: press-and-hold microphone → speak → AI transcribes → text field populated. |
| **Priority** | High |
| **User Roles** | Teacher, Parent |
| **Acceptance notes** | Press-and-hold mic button to record. On release, audio uploaded and transcribed. Text populates the field. Always editable before saving. |

### FR-002
| Field | Value |
|---|---|
| **Title** | Photo Capture with OCR |
| **Description** | Photo capture: take photo → AI extracts text (OCR) → text field populated. |
| **Priority** | High |
| **User Roles** | Teacher |
| **Acceptance notes** | Teacher photographs worksheet/textbook page. AI extracts text via vision model. Extracted text populates homework description field. Always editable. |

### FR-003
| Field | Value |
|---|---|
| **Title** | Voice Messages in Chat |
| **Description** | Voice notes in messages: record voice → transcribed → sent as text + audio attachment. |
| **Priority** | Medium |
| **User Roles** | Parent, Teacher |
| **Acceptance notes** | Voice message recorded, uploaded to Supabase Storage, transcribed. Message sent with both `audioUrl` and transcribed `body`. Recipient sees text + play button. |

### FR-004
| Field | Value |
|---|---|
| **Title** | Visual Question Answering |
| **Description** | Visual QA: photo of problem → AI explains (delegates to AI Tutor). |
| **Priority** | Medium |
| **User Roles** | Student (via Parent app) |
| **Acceptance notes** | Student/parent photographs problem. Image sent to AI Tutor with question. AI explains the problem using vision capabilities. Delegates to `VIDYASETU_AI_TUTOR_SPEC.md`. |

### FR-005
| Field | Value |
|---|---|
| **Title** | Multi-Language Voice Recognition |
| **Description** | Multi-language: voice recognition in 10 Indian languages. |
| **Priority** | High |
| **User Roles** | Teacher, Parent |
| **Acceptance notes** | Supports: English, Hindi, Bengali, Tamil, Telugu, Marathi, Gujarati, Kannada, Malayalam, Punjabi. Language selected by user or auto-detected. |

### FR-006
| Field | Value |
|---|---|
| **Title** | Editable Transcription |
| **Description** | Editable: transcribed text always editable before saving/sending. |
| **Priority** | High |
| **User Roles** | Teacher, Parent |
| **Acceptance notes** | Transcribed text is a suggestion, not final. User can edit before saving (homework) or sending (message). No auto-save of raw transcription. |

### Non-Functional Requirements

| ID | Requirement |
|---|---|
| NFR-1 | Audio recording: max 5 minutes per recording |
| NFR-2 | Transcription: < 10 seconds for 1-minute audio |
| NFR-3 | OCR: < 5 seconds per image |
| NFR-4 | Audio file size: max 10MB (compressed) |
| NFR-5 | Image file size: max 5MB (compressed) |
| NFR-6 | Supported audio format: AAC/MP3 |
| NFR-7 | Supported image format: JPEG/HEIC |

---

## 4. User Stories

### Teacher
- [ ] Dictate homework assignment via voice → text populated in homework form
- [ ] Photograph worksheet → OCR text populated in homework description
- [ ] Send voice message to parent in chat
- [ ] Dictate announcement via voice → text populated

### Parent
- [ ] Send voice message to teacher in chat
- [ ] Photograph child's problem → AI explains (visual QA)

### Student (via Parent app)
- [ ] Photograph problem → AI explains step-by-step

### System
- [ ] Transcribe audio to text using AI speech-to-text
- [ ] Extract text from images using AI vision (OCR)
- [ ] Store audio files in Supabase Storage
- [ ] Store image files in Supabase Storage
- [ ] Support 10 Indian languages for voice recognition

---

## 5. Business Rules

### BR-001
**Rule:** Transcribed text is always editable.
**Enforcement:** Transcription is a suggestion. User must be able to edit before saving/sending. No auto-save of raw transcription without user review.

### BR-002
**Rule:** Max 5 minutes per audio recording.
**Enforcement:** Client-side enforcement. Recording auto-stops at 5 minutes. Server validates audio duration.

### BR-003
**Rule:** Voice messages include both audio and transcription.
**Enforcement:** Voice message has `audioUrl` (audio file) and `body` (transcribed text). Recipient can read text and play audio.

### BR-004
**Rule:** Multi-language support for voice recognition.
**Enforcement:** 10 Indian languages supported. User selects language or auto-detect. Transcription in spoken language.

### BR-005
**Rule:** Visual QA delegates to AI Tutor.
**Enforcement:** Photo + question sent to AI Tutor service. AI Tutor uses vision capabilities to explain. Response from AI Tutor, not separate service.

### BR-006
**Rule:** Audio and image files stored in Supabase Storage.
**Enforcement:** Files uploaded to Supabase Storage. URL stored in DB. Files accessible via signed URLs.

### BR-007
**Rule:** Transcription and OCR are AI-powered.
**Enforcement:** Uses external AI APIs (OpenAI Whisper for speech-to-text, GPT-4o/Gemini for OCR). No on-device processing.

### BR-008
**Rule:** Photo capture is for text extraction, not storage.
**Enforcement:** Photos are processed for OCR, text extracted. Original image may be stored temporarily but not permanently (unless voice message context).

---

## 6. Database Design

### 6.1 Entity Relationship Summary

No new tables. `MessagesTable` gets a new `audio_url` column. Audio and image files stored in Supabase Storage (not DB).

### 6.2 New Tables

N/A — no new tables. All data stored in existing tables + Supabase Storage.

### 6.3 Modified Tables

#### `MessagesTable` (modification)

Add column:
```sql
ALTER TABLE messages ADD COLUMN audio_url TEXT;
```

- `audio_url` — URL to audio file in Supabase Storage (nullable). When present, `body` contains transcribed text.

### 6.4 Indexes

N/A — no new indexes. Existing message indexes sufficient.

### 6.5 Constraints

- `messages.audio_url` — nullable TEXT. When present, `body` must also be present (transcribed text).
- Audio file: max 10MB, AAC/MP3 format
- Image file: max 5MB, JPEG/HEIC format

### 6.6 Foreign Keys

N/A — no new foreign keys.

### 6.7 Soft Delete Strategy

N/A — no new tables. Voice messages follow existing message soft delete.

### 6.8 Audit Fields

N/A — uses existing message audit fields. Transcription requests logged in AI service logs.

### 6.9 Migration Notes

Migration: `docs/db/migration_084_voice_photo_capture.sql`
- ALTER TABLE: add `audio_url` column to `messages`
- No data migration (new column, nullable)
- No new tables

### 6.10 Exposed Mappings

```kotlin
// Modify existing MessagesTable
object MessagesTable : UUIDTable("messages", "id") {
    // ... existing columns ...
    val audioUrl = text("audio_url").nullable()  // NEW: voice message audio URL
}
```

### 6.11 Seed Data

N/A — no new tables or seed data needed.

---

## 7. State Machines

### Voice Recording State Machine

```
idle ──press_mic──> recording ──release_mic──> uploading ──upload_complete──> transcribing ──transcription_complete──> text_ready
  │                     │                         │                               │
  │                     │                         │                               └──transcription_failed──> error
  │                     │                         └──upload_failed──> error
  │                     └──5_min_limit──> uploading (auto-stop)
  │
  └──text_ready ──user_edits──> text_ready (edited) ──user_saves──> saved
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `idle` | User presses mic | `recording` | Start audio recording |
| `recording` | User releases mic | `uploading` | Stop recording, get audio data |
| `recording` | 5-minute limit reached | `uploading` | Auto-stop recording |
| `uploading` | Upload complete | `transcribing` | Audio URL received |
| `uploading` | Upload failed | `error` | Network or storage error |
| `transcribing` | Transcription complete | `text_ready` | Text returned by AI |
| `transcribing` | Transcription failed | `error` | AI service error |
| `text_ready` | User edits text | `text_ready` | Text modified by user |
| `text_ready` | User saves/sends | `saved` | Text confirmed and saved |
| `error` | User retries | `idle` | Back to idle state |

### Photo Capture State Machine

```
idle ──take_photo──> uploading ──upload_complete──> ocr_processing ──ocr_complete──> text_ready
  │                      │                             │
  │                      │                             └──ocr_failed──> error
  │                      └──upload_failed──> error
  │
  └──text_ready ──user_edits──> text_ready (edited) ──user_saves──> saved
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `idle` | User takes photo | `uploading` | Photo captured |
| `uploading` | Upload complete | `ocr_processing` | Image URL received |
| `uploading` | Upload failed | `error` | Network or storage error |
| `ocr_processing` | OCR complete | `text_ready` | Text extracted by AI |
| `ocr_processing` | OCR failed | `error` | AI service error |
| `text_ready` | User edits text | `text_ready` | Text modified by user |
| `text_ready` | User saves | `saved` | Text confirmed and saved |
| `error` | User retries | `idle` | Back to idle state |

### Voice Message State Machine

```
idle ──record──> recording ──stop──> uploading ──upload_complete──> transcribing ──complete──> sending ──sent──> sent
  │                    │                     │                              │
  │                    │                     │                              └──transcribe_failed──> sending (audio only)
  │                    │                     └──upload_failed──> error
  │                    └──5_min_limit──> uploading
  │
  └──sent (with audio_url + transcribed body)
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `idle` | User records voice | `recording` | Start recording |
| `recording` | User stops | `uploading` | Stop recording |
| `recording` | 5-minute limit | `uploading` | Auto-stop |
| `uploading` | Upload complete | `transcribing` | Audio URL received |
| `uploading` | Upload failed | `error` | Storage error |
| `transcribing` | Transcription complete | `sending` | Text + audio ready |
| `transcribing` | Transcription failed | `sending` | Send audio only (no text) |
| `sending` | Message sent | `sent` | Message with audio_url + body |
| `error` | User retries | `idle` | Back to idle |

---

## 8. Backend Architecture

### 8.1 Component Overview

`TranscriptionService` handles speech-to-text and image OCR using AI services. `AiRouting` exposes internal transcription and OCR endpoints. Audio and image files stored in Supabase Storage. Voice messages integrate with existing messaging system.

### 8.2 Design Principles

1. **AI-powered** — uses external AI APIs for transcription and OCR
2. **Always editable** — transcribed/extracted text is a suggestion, user reviews
3. **Multi-platform** — audio recording via expect/actual (Android, iOS, Web)
4. **Multi-language** — 10 Indian languages for voice recognition
5. **File storage in Supabase** — audio and images in Supabase Storage, URLs in DB
6. **Internal endpoints** — transcription/OCR are internal, called by client during input

### 8.3 Core Types

#### TranscriptionService

```kotlin
class TranscriptionService(private val aiService: AiService) {
    suspend fun transcribeAudio(audioData: ByteArray, language: String): String {
        // 1. Upload audio to Supabase Storage
        // 2. Call AI speech-to-text (OpenAI Whisper API or Google Speech-to-Text)
        // 3. Return transcribed text
    }

    suspend fun extractTextFromImage(imageUrl: String): String {
        // Call AI vision (GPT-4o/Gemini) with OCR prompt
        return aiService.complete(null, null, "ocr", "extract_text_v1",
            mapOf("image_url" to imageUrl))
    }
}
```

#### AudioRecorder (expect/actual)

```kotlin
// expect/actual pattern
expect class AudioRecorder() {
    fun startRecording()
    fun stopRecording(): ByteArray  // returns audio data
    val isRecording: Boolean
}

// Android: MediaRecorder
// iOS: AVAudioRecorder
// Web: MediaRecorder API
```

### 8.4 Repositories

- Uses existing `MessageRepository` — for voice messages (with `audio_url`)
- Uses Supabase Storage client — for audio and image file storage

### 8.5 Mappers

N/A — transcription returns plain text. No complex mapping needed.

### 8.6 Permission Checks

- Transcription endpoint: authenticated user (any role)
- OCR endpoint: authenticated user (any role)
- Voice messages: same permissions as text messages (parent-teacher chat)
- Visual QA: same permissions as AI Tutor

### 8.7 Background Jobs

N/A — transcription and OCR are synchronous (on-demand). No background jobs needed.

### 8.8 Domain Events

- `AudioTranscribed` — emitted when audio is transcribed (logged)
- `ImageTextExtracted` — emitted when image OCR completes (logged)
- `VoiceMessageSent` — emitted when voice message is sent (with audio_url + body)

### 8.9 Caching

N/A — transcription and OCR are on-demand, not cached. Each request is unique.

### 8.10 Transactions

- Transcription: no DB transaction (AI call + storage upload)
- OCR: no DB transaction (AI call)
- Voice message: single transaction (insert message with audio_url + body)

### 8.11 Rate Limiting

- Transcription: 10 requests per minute per user
- OCR: 10 requests per minute per user
- Prevents AI API abuse

### 8.12 Configuration

- `VOICE_PHOTO_CAPTURE_ENABLED` — default `true`; enable/disable feature
- `TRANSCRIPTION_AI_MODEL` — default `whisper-1`; AI model for speech-to-text
- `OCR_AI_MODEL` — default `extract_text_v1`; AI model for OCR
- `MAX_AUDIO_DURATION_SECONDS` — default `300`; 5 minutes
- `MAX_AUDIO_FILE_SIZE_MB` — default `10`
- `MAX_IMAGE_FILE_SIZE_MB` — default `5`
- `SUPPORTED_LANGUAGES` — default `en,hi,bn,ta,te,mr,gu,kn,ml,pa`

### 8.13 Voice Messages (Messaging Integration)

Voice message in messaging:
1. Parent records voice → audio uploaded to Supabase Storage
2. `TranscriptionService.transcribeAudio()` → text
3. Message sent with both `audioUrl` and transcribed `body`
4. Recipient sees text + play button for audio

---

## 9. API Contracts

### 9.1 Internal Endpoints

```
POST /api/v1/ai/transcribe
  Body: { audio_url, language }
  → 200: { text }
  → 400: Invalid audio or language
  → 500: Transcription failed

POST /api/v1/ai/extract-text
  Body: { image_url }
  → 200: { text }
  → 400: Invalid image
  → 500: OCR failed
```

These are internal endpoints called by client during input, not user-facing APIs.

### 9.2 DTO Models

All `@Serializable`, wrapped in `ApiResponse<T>` pattern.

```kotlin
@Serializable data class TranscribeRequest(
    val audioUrl: String,
    val language: String,  // en, hi, bn, ta, te, mr, gu, kn, ml, pa
)

@Serializable data class TranscribeResponse(
    val text: String,
)

@Serializable data class ExtractTextRequest(
    val imageUrl: String,
)

@Serializable data class ExtractTextResponse(
    val text: String,
)
```

### 9.3 Voice Message (Messaging Integration)

Existing messaging API extended:
```
POST /api/v1/messages/send
  Body: { recipient_id, body, audio_url? }
  → 201: MessageDto (with audio_url if provided)
```

```kotlin
// Extended MessageDto
@Serializable data class MessageDto(
    // ... existing fields ...
    val audioUrl: String?,  // NEW: voice message audio URL
)
```

---

## 10. Frontend Architecture

### 10.1 Screens

| Screen | Platform | Role | Description |
|---|---|---|---|
| `VoiceInputField` (component) | Compose | Teacher, Parent | Reusable voice input field with mic button |
| `HomeworkComposerScreen` (modified) | Compose | Teacher | Add voice/photo input for homework description |
| `MessagingScreen` (modified) | Compose | Parent, Teacher | Add voice message recording |
| `AiTutorScreen` (modified) | Compose | Parent | Add photo capture for visual QA |

### 10.2 Navigation

- No new navigation routes. Voice/photo input is integrated into existing screens.

### 10.3 UX Flows

#### Teacher: Voice Input for Homework
1. Teacher opens homework composer
2. Press-and-hold mic button → speak homework
3. Release mic → "Transcribing..." loading
4. Transcribed text appears in description field
5. Teacher reviews and edits text
6. Save homework

#### Teacher: Photo OCR for Homework
1. Teacher opens homework composer
2. Tap photo button → camera opens
3. Photograph worksheet/textbook
4. "Extracting text..." loading
5. Extracted text appears in description field
6. Teacher reviews and edits text
7. Save homework

#### Parent: Voice Message
1. Parent opens chat with teacher
2. Press-and-hold mic button → speak message
3. Release mic → "Sending..." loading
4. Message sent with audio + transcribed text
5. Teacher sees text + play button

#### Parent/Student: Visual QA
1. Parent opens AI Tutor
2. Tap photo button → camera opens
3. Photograph problem/question
4. AI Tutor receives image + optional text question
5. AI explains the problem

### 10.4 State Management

```kotlin
data class VoiceInputState(
    val isRecording: Boolean,
    val isTranscribing: Boolean,
    val transcribedText: String,
    val error: String?,
)

data class PhotoCaptureState(
    val isProcessing: Boolean,
    val extractedText: String,
    val error: String?,
)

data class VoiceMessageState(
    val isRecording: Boolean,
    val isUploading: Boolean,
    val isTranscribing: Boolean,
    val error: String?,
)
```

### 10.5 Offline Support

- Voice recording: available offline (recording is local)
- Transcription: NOT available offline (requires AI service)
- OCR: NOT available offline (requires AI service)
- Voice messages: NOT available offline (requires upload + transcription)

### 10.6 Loading States

- Transcribing: "Transcribing audio..."
- OCR: "Extracting text from image..."
- Uploading: "Uploading audio..."
- Sending voice message: "Sending voice message..."

### 10.7 Error Handling (UI)

- Transcription failed: "Unable to transcribe audio. Please try again or type manually."
- OCR failed: "Unable to extract text from image. Please try again or type manually."
- Upload failed: "Upload failed. Please check your connection."
- Mic permission denied: "Microphone access required. Please enable in settings."
- Camera permission denied: "Camera access required. Please enable in settings."
- Recording too long: "Recording limit is 5 minutes."

### 10.8 Component Integration Guidelines

| Rule | Description |
|---|---|
| **R1** | Voice input: press-and-hold mic button, release to stop |
| **R2** | Recording indicator: pulsing red dot + timer when recording |
| **R3** | Transcribed text: populated in text field, highlighted for review |
| **R4** | Photo capture: camera intent, crop/adjust optional |
| **R5** | OCR text: populated in text field, highlighted for review |
| **R6** | Voice message: mic button in chat, waveform animation when recording |
| **R7** | Voice message playback: play button next to text in chat |
| **R8** | Language selector: dropdown for voice recognition language |
| **R9** | Editable: all transcribed/extracted text is editable before saving |
| **R10** | Error states: show error message with "Try again" or "Type manually" options |

### 10.9 Voice Input Component

```kotlin
@Composable
fun VoiceInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isRecording by remember { mutableStateOf(false) }
    Row {
        VTextField(value = value, onValueChange = onValueChange, modifier = modifier)
        VIconButton(onClick = { isRecording = !isRecording }) {
            VIcon(if (isRecording) VIcons.Stop else VIcons.Mic)
        }
    }
    if (isRecording) {
        // Platform-specific audio recording
        // On stop: send audio to server for transcription
    }
}
```

### 10.10 Platform Audio Recording

```kotlin
// expect/actual pattern
expect class AudioRecorder() {
    fun startRecording()
    fun stopRecording(): ByteArray  // returns audio data
    val isRecording: Boolean
}

// Android: MediaRecorder
// iOS: AVAudioRecorder
// Web: MediaRecorder API
```

---

## 11. Shared Module Changes (KMP)

### 11.1 DTOs

All DTOs defined in section 9.2, placed in `shared/.../ai/domain/model/TranscriptionModels.kt`.

### 11.2 Domain Models

```kotlin
data class TranscriptionResult(
    val text: String,
    val language: String,
    val durationSeconds: Int,
)

data class OcrResult(
    val text: String,
    val imageUrl: String,
)

data class VoiceMessage(
    val audioUrl: String,
    val transcribedText: String?,
    val durationSeconds: Int,
)

enum class SupportedLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    HINDI("hi", "Hindi"),
    BENGALI("bn", "Bengali"),
    TAMIL("ta", "Tamil"),
    TELUGU("te", "Telugu"),
    MARATHI("mr", "Marathi"),
    GUJARATI("gu", "Gujarati"),
    KANNADA("kn", "Kannada"),
    MALAYALAM("ml", "Malayalam"),
    PUNJABI("pa", "Punjabi"),
}
```

### 11.3 Repository Interfaces

```kotlin
interface TranscriptionRepository {
    suspend fun transcribeAudio(token: String, audioUrl: String, language: String): NetworkResult<TranscriptionResult>
    suspend fun extractTextFromImage(token: String, imageUrl: String): NetworkResult<OcrResult>
}
```

### 11.4 UseCases

- `TranscribeAudioUseCase`
- `ExtractTextFromImageUseCase`
- `SendVoiceMessageUseCase`
- `VisualQAUseCase` (delegates to AI Tutor)

### 11.5 Validation

- `audio_url`: valid URL, audio file in Supabase Storage
- `language`: one of supported language codes
- `image_url`: valid URL, image file in Supabase Storage
- Audio duration: max 300 seconds (5 minutes)
- Audio file size: max 10MB
- Image file size: max 5MB

### 11.6 Serialization

Standard Kotlinx serialization. Language codes as lowercase strings.

### 11.7 Network APIs

Ktor `@Resource` route definitions in `TranscriptionApi.kt`:
- POST `/api/v1/ai/transcribe`
- POST `/api/v1/ai/extract-text`

### 11.8 Database Models (Local Cache)

N/A — transcription and OCR results are not cached locally. They are used immediately in the UI.

---

## 12. Permissions Matrix

| Action | Super Admin | School Admin | Counselor | Teacher | Parent |
|---|---|---|---|---|---|
| Use voice input | ✅ | ✅ | ✅ | ✅ | ✅ |
| Use photo OCR | ✅ | ✅ | ✅ | ✅ | ✅ |
| Send voice messages | ✅ | ✅ | ✅ | ✅ | ✅ |
| Use visual QA | N/A | N/A | N/A | N/A | ✅ |
| Configure supported languages | ✅ | ❌ | ❌ | ❌ | ❌ |

---

## 13. Notifications

### Voice & Photo Capture Notifications

N/A — this feature does not generate notifications. Voice messages are delivered via existing messaging notification system.

### Notification Integration

Voice messages use existing `Notify.kt` dispatch with category "messaging" (same as text messages).

---

## 14. Background Jobs

N/A — no background jobs. Transcription and OCR are synchronous (on-demand). Voice messages are sent immediately.

---

## 15. Integrations

### Internal Integrations

| System | Integration Point | Direction | Protocol | Error Handling |
|---|---|---|---|---|
| `AiService` (`AI_INFRASTRUCTURE_SPEC.md`) | Speech-to-text | Call | Direct call | Return error, user types manually |
| `AiService` (`AI_INFRASTRUCTURE_SPEC.md`) | Image OCR | Call | Direct call | Return error, user types manually |
| `VIDYASETU_AI_TUTOR_SPEC.md` | Visual QA | Call | Direct call | Return error, user types question |
| `MESSAGING_SYSTEM_SPEC.md` | Voice messages | Modify | DB + Storage | Log error, send text only |
| Supabase Storage | Audio/image storage | Call | HTTP API | Return error, retry |
| `HomeworkService` | Homework description | Modify | DB | N/A (text field) |

### External Integrations

| System | Integration Point | Direction | Protocol | Error Handling |
|---|---|---|---|---|
| OpenAI Whisper API | Speech-to-text | Call | HTTP API | Return error, user types manually |
| GPT-4o / Gemini | Image OCR | Call | HTTP API | Return error, user types manually |

### Integration Patterns

- **Speech-to-text:** Upload audio to Supabase Storage → call AI with audio URL → get text
- **OCR:** Upload image to Supabase Storage → call AI vision with image URL → get text
- **Voice messages:** Upload audio → transcribe → insert message with `audio_url` + `body`
- **Visual QA:** Upload image → delegate to AI Tutor with image + question

---

## 16. Security

### Authentication

- Transcription/OCR endpoints: JWT auth via `requireAuth()`
- Any authenticated user can use voice input and photo OCR
- Voice messages: same auth as text messages

### Authorization

- All authenticated users can use transcription and OCR
- Voice messages: same authorization as text messages (parent-teacher chat)
- Visual QA: same authorization as AI Tutor

### Data Protection

- Audio files stored in Supabase Storage (private bucket, signed URLs)
- Image files stored in Supabase Storage (private bucket, signed URLs)
- Transcription text is user-generated content (not PII)
- No PII sent to external AI APIs (audio is voice, images are worksheets/problems)

### Input Validation

- `audio_url`: valid URL, file exists in Supabase Storage
- `language`: one of supported language codes
- `image_url`: valid URL, file exists in Supabase Storage
- Audio duration: max 300 seconds
- File sizes: audio max 10MB, image max 5MB

### Rate Limiting

- Transcription: 10 requests per minute per user
- OCR: 10 requests per minute per user
- Prevents AI API abuse and cost overrun

### Audit Logging

- Transcription requested: user ID, language, audio duration, timestamp
- OCR requested: user ID, image URL, timestamp
- Voice message sent: sender ID, recipient ID, audio URL, timestamp

### PII Handling

- Audio recordings may contain voice (biometric-ish data) — stored temporarily in Supabase Storage
- Images may contain student work — stored temporarily for OCR
- No PII sent to external AI APIs beyond the audio/image content itself
- Audio files for voice messages stored permanently (part of message history)

### Multi-tenant Isolation

- Audio and image files stored in school-scoped Supabase Storage buckets
- Transcription/OCR endpoints filter by authenticated user's school
- No cross-school file access

---

## 17. Performance & Scalability

### Expected Scale

- 100-500 transcription requests per day per school
- 50-200 OCR requests per day per school
- 50-200 voice messages per day per school
- Each request: upload + AI call (5-10 seconds)

### Query Optimization

N/A — no DB queries for transcription/OCR. File uploads to Supabase Storage.

### Indexing Strategy

N/A — no new indexes. Existing message indexes sufficient.

### Caching Strategy

N/A — transcription and OCR are on-demand, not cached. Each request is unique.

### Pagination

N/A — single request, single response.

### Connection Pooling

Uses existing HikariCP connection pool for DB. Supabase Storage uses HTTP client.

### Async Processing

- Transcription: synchronous (client waits for response)
- OCR: synchronous (client waits for response)
- Voice message upload: synchronous (client waits for upload + transcription)
- Notifications: async (fire-and-forget via `Notify.kt`)

### Scalability Concerns

- AI API calls: 500 transcriptions/day × 5 seconds = ~42 minutes of AI time. Manageable.
- Supabase Storage: 500 audio files/day × 1MB avg = 500MB/day. Need storage management.
- Cost: AI API calls have per-request cost. Rate limiting prevents abuse.
- File cleanup: OCR images can be deleted after extraction. Voice message audio retained.

---

## 18. Edge Cases

| # | Scenario | Expected Behavior |
|---|---|---|
| EC-1 | Audio recording is silence | AI returns empty string. UI shows "No speech detected. Please try again." |
| EC-2 | Audio in unsupported language | AI returns garbage text or error. UI shows "Unable to transcribe. Try different language." |
| EC-3 | Image has no text | AI returns empty string. UI shows "No text found in image." |
| EC-4 | Image is blurry/unreadable | AI returns partial or incorrect text. User can edit. |
| EC-5 | Audio file too large (> 10MB) | Client compresses before upload. If still too large, reject. |
| EC-6 | Image file too large (> 5MB) | Client compresses before upload. If still too large, reject. |
| EC-7 | AI service unavailable | Return error. User can type manually. |
| EC-8 | Recording exceeds 5 minutes | Auto-stop at 5 minutes. Upload and transcribe what was recorded. |
| EC-9 | Mic permission denied | UI shows "Microphone access required. Please enable in settings." |
| EC-10 | Camera permission denied | UI shows "Camera access required. Please enable in settings." |
| EC-11 | Network interrupted during upload | Return error. User can retry or type manually. |
| EC-12 | Voice message transcription fails | Send audio only (no text). Recipient can play audio. |

---

## 19. Error Handling

### Error Response Format

Standard `ApiResponse` error format.

### Error Codes

| Code | HTTP Status | Description | User Message |
|---|---|---|---|
| `TRANSCRIPTION_FAILED` | 500 | AI transcription failed | "Unable to transcribe audio. Please try again or type manually." |
| `OCR_FAILED` | 500 | AI OCR failed | "Unable to extract text from image. Please try again or type manually." |
| `INVALID_AUDIO_FORMAT` | 400 | Audio format not supported | "Audio format not supported. Please use AAC or MP3." |
| `INVALID_IMAGE_FORMAT` | 400 | Image format not supported | "Image format not supported. Please use JPEG or HEIC." |
| `FILE_TOO_LARGE` | 400 | File exceeds size limit | "File too large. Audio max 10MB, image max 5MB." |
| `UNSUPPORTED_LANGUAGE` | 400 | Language not supported | "Language not supported. Please select from available languages." |
| `RECORDING_TOO_LONG` | 400 | Audio exceeds 5 minutes | "Recording limit is 5 minutes." |

### Error Handling Strategy

- **Transcription/OCR failure:** Return error. User can retry or type manually.
- **Upload failure:** Return error. User can retry.
- **Voice message transcription failure:** Send audio only (no text body). Recipient plays audio.
- **AI service unavailable:** Return error. User types manually.

### Retry Strategy

- Transcription: client retries once on AI failure, then shows error
- OCR: client retries once on AI failure, then shows error
- Upload: client retries 3 times on network error
- Voice message: if transcription fails, send audio only (no retry on transcription)

### Fallback Behavior

- Transcription fails: user types manually
- OCR fails: user types manually
- Voice message transcription fails: send audio only
- AI service unavailable: user types manually
- Mic/camera permission denied: show settings prompt

---

## 20. Analytics & Reporting

### Analytics Dashboard Data

| Metric | Source | Derivation |
|---|---|---|
| Transcription usage | Audit logs | Count per user per day |
| OCR usage | Audit logs | Count per user per day |
| Voice messages sent | `messages` (audio_url not null) | Count per day |
| Language distribution | Audit logs | Count by language |
| Average audio duration | Audit logs | Avg duration per transcription |
| Transcription success rate | Audit logs | Successful / total requests |
| OCR success rate | Audit logs | Successful / total requests |

### Export Capabilities

- Voice input usage report (CSV) — user, date, language, duration
- OCR usage report (CSV) — user, date, image URL
- Voice message report (CSV) — sender, recipient, date, duration

### Report Types

| Report | Format | Frequency | Recipient |
|---|---|---|---|
| Voice/Photo usage | JSON (API) | Monthly | Product Team |
| AI API costs | JSON (API) | Monthly | Dev Team |
| Language distribution | JSON (API) | Monthly | Product Team |

---

## 21. Testing Strategy

### Unit Tests

- `TranscriptionService.transcribeAudio()` — AI call, response parsing
- `TranscriptionService.extractTextFromImage()` — AI call, response parsing
- `AudioRecorder` — platform-specific recording (mocked)
- Language validation — supported/unsupported codes
- File size validation — audio/image limits

### Integration Tests

- Full transcription flow: upload audio → transcribe → verify text
- Full OCR flow: upload image → extract text → verify text
- Voice message flow: record → upload → transcribe → send → verify message
- Visual QA flow: upload image → delegate to AI Tutor → verify response
- Transcription failure: AI fails → verify error, user can type manually
- Voice message transcription failure: AI fails → verify audio-only message sent

### E2E Tests

- Teacher dictates homework → text populated → save homework
- Teacher photographs worksheet → text populated → save homework
- Parent sends voice message → teacher receives text + audio
- Parent photographs problem → AI explains

### Performance Tests

- Transcription: < 10 seconds for 1-minute audio
- OCR: < 5 seconds per image
- Upload: < 5 seconds for 5MB file
- Voice message send: < 15 seconds total (upload + transcribe + send)

### Test Data

- Sample audio files in multiple languages (English, Hindi, Tamil)
- Sample images with text (worksheets, textbook pages)
- Sample images without text (blank, photos)
- Mock AI service (returns controlled transcription/OCR text)

### Test Environment

- Mock AI service (returns controlled responses)
- Test Supabase Storage bucket
- Test audio recorder (mocked ByteArray output)
- Test JWT tokens for all roles

---

## 22. Acceptance Criteria

- [ ] Voice input: speak → transcribed text populates field
- [ ] Photo capture: photo → OCR text populates field
- [ ] Voice messages in chat: audio + transcription
- [ ] Visual QA: photo of problem → AI explanation
- [ ] Multi-language voice recognition (at least English + Hindi)
- [ ] Transcribed text always editable
- [ ] Audio recording max 5 minutes enforced
- [ ] File size limits enforced (audio 10MB, image 5MB)
- [ ] Transcription failure: user can type manually
- [ ] Voice message: audio + text sent together
- [ ] Visual QA delegates to AI Tutor

---

## 23. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 2 days | `AudioRecorder` expect/actual (Android + iOS + Web) |
| 2 | 2 days | `TranscriptionService` (speech-to-text + image OCR) |
| 3 | 2 days | `VoiceInputField` composable + integration into homework/announcement forms |
| 4 | 2 days | Voice messages in messaging (DB migration + UI) |
| 5 | 2 days | Visual QA integration with AI Tutor |
| 6 | 2 days | Tests: unit, integration, E2E |

### Pre-Implementation Checklist

- [ ] Verify `AI_INFRASTRUCTURE_SPEC.md` is implemented and `AiService` available
- [ ] Verify AI service supports speech-to-text (Whisper or equivalent)
- [ ] Verify AI service supports vision (GPT-4o or Gemini)
- [ ] Verify Supabase Storage is configured for audio/image uploads
- [ ] Verify `MESSAGING_SYSTEM_SPEC.md` is implemented
- [ ] Verify `VIDYASETU_AI_TUTOR_SPEC.md` is implemented
- [ ] Verify camera and microphone permissions in app manifest

---

## 24. File-Level Impact Analysis

### Server

| File | Change Type | Description |
|---|---|---|
| `server/.../feature/ai/TranscriptionService.kt` | **New** | Speech-to-text + OCR service |
| `server/.../feature/ai/AiRouting.kt` | Modify | Add transcribe + extract-text endpoints |
| `server/.../db/Tables.kt` | Modify | Add `audio_url` to `MessagesTable` |
| `docs/db/migration_084_voice_photo_capture.sql` | **New** | ALTER TABLE for messages.audio_url |

### Shared (KMP)

| File | Change Type | Description |
|---|---|---|
| `shared/.../core/audio/AudioRecorder.kt` | **New** + expect/actual | Platform audio recording |
| `shared/.../ai/domain/model/TranscriptionModels.kt` | **New** | DTOs, domain models, enums |
| `shared/.../ai/domain/repository/TranscriptionRepository.kt` | **New** | Repository interface |
| `shared/.../ai/data/remote/TranscriptionApi.kt` | **New** | HTTP API definitions |

### Client (Compose)

| File | Change Type | Description |
|---|---|---|
| `composeApp/.../ui/v2/components/VoiceInputField.kt` | **New** | Voice input composable |
| `composeApp/.../ui/v2/screens/teacher/HomeworkComposerScreen.kt` | Modify | Add voice/photo input |
| `composeApp/.../ui/v2/screens/parent/AiTutorScreen.kt` | Modify | Add photo capture for visual QA |
| `composeApp/.../ui/v2/screens/messaging/ChatScreen.kt` | Modify | Add voice message recording |

### Platform-Specific (expect/actual)

| File | Platform | Description |
|---|---|---|
| `androidMain/.../core/audio/AudioRecorder.kt` | Android | MediaRecorder implementation |
| `iosMain/.../core/audio/AudioRecorder.kt` | iOS | AVAudioRecorder implementation |
| `jsMain/.../core/audio/AudioRecorder.kt` | Web | MediaRecorder API implementation |

---

## 25. Future Enhancements

| # | Enhancement | Priority | Effort | Notes |
|---|---|---|---|---|
| F-1 | Real-time live transcription | Medium | L | Stream audio, show text as user speaks |
| F-2 | Voice commands for app navigation | Low | M | "Open homework", "Send message to..." |
| F-3 | Auto-translation between languages | Medium | M | Transcribe in Hindi, translate to English |
| F-4 | Voice-to-text for student responses | Low | M | Allow students to dictate answers |
| F-5 | Audio waveform visualization | Low | S | Show waveform during recording |
| F-6 | Voice message transcription editing | Low | S | Allow editing transcription before sending |
| F-7 | Batch photo OCR | Low | M | Photograph multiple pages, extract all text |
| F-8 | Voice template messages | Low | S | Pre-recorded voice templates for common messages |
| F-9 | Offline transcription (on-device) | Low | L | Use on-device ML model for basic transcription |
| F-10 | Voice-based search | Low | M | Search homework/messages by voice |

---

## Appendix A: Sequence Diagrams

### A.1 Voice Input Flow

```
User (app)          Server              AI Service        Supabase Storage
  │                    │                    │                   │
  │  press mic         │                    │                   │
  │  start recording   │                    │                   │
  │  ...               │                    │                   │
  │  release mic       │                    │                   │
  │  stop recording    │                    │                   │
  │  ──upload audio───────────────────────────────────────────>│
  │  ←──audio url─────────────────────────────────────────────│
  │                    │                    │                   │
  │  POST /transcribe  │                    │                   │
  │  {audio_url, lang} │                    │                   │
  │  ───────────────>  │                    │                   │
  │                    │──call AI──────────>│                   │
  │                    │←──transcribed text─│                   │
  │  ←──{text}─────────│                    │                   │
  │                    │                    │                   │
  │  text in field     │                    │                   │
  │  user edits        │                    │                   │
  │  user saves        │                    │                   │
  │                    │                    │                   │
```

### A.2 Photo OCR Flow

```
User (app)          Server              AI Service        Supabase Storage
  │                    │                    │                   │
  │  take photo        │                    │                   │
  │  ──upload image────────────────────────────────────────────>│
  │  ←──image url──────────────────────────────────────────────│
  │                    │                    │                   │
  │  POST /extract-text│                    │                   │
  │  {image_url}       │                    │                   │
  │  ───────────────>  │                    │                   │
  │                    │──call AI vision──>│                   │
  │                    │←──extracted text──│                   │
  │  ←──{text}─────────│                    │                   │
  │                    │                    │                   │
  │  text in field     │                    │                   │
  │  user edits        │                    │                   │
  │  user saves        │                    │                   │
  │                    │                    │                   │
```

### A.3 Voice Message Flow

```
Parent (app)        Server              AI Service        Supabase Storage
  │                    │                    │                   │
  │  record voice      │                    │                   │
  │  ──upload audio───────────────────────────────────────────>│
  │  ←──audio url─────────────────────────────────────────────│
  │                    │                    │                   │
  │  POST /transcribe  │                    │                   │
  │  ───────────────>  │──call AI──────────>│                   │
  │  ←──{text}─────────│←──transcribed text─│                   │
  │                    │                    │                   │
  │  POST /messages/send                  │                    │
  │  {body: text, audio_url}              │                    │
  │  ───────────────>  │                    │                   │
  │  ←──201: Message───│                    │                   │
  │                    │                    │                   │
  │  (teacher receives notification +     │                    │
  │   sees text + play button)             │                    │
  │                    │                    │                   │
```

---

## Appendix B: Domain Model / ER Diagram

```
No new tables. Modified existing table:

┌──────────────────────────────────────────────────────────────────────┐
│                      messages (modified)                              │
│  id (PK)                                                              │
│  ... existing columns ...                                             │
│  audio_url (TEXT, nullable)  ← NEW: voice message audio URL          │
│  body (TEXT)                 ← transcribed text for voice messages   │
└──────────────────────────────────────────────────────────────────────┘

Supabase Storage:
┌──────────────────┐  ┌──────────────────┐
│ audio files      │  │ image files      │
│ (AAC/MP3)        │  │ (JPEG/HEIC)      │
│ max 10MB         │  │ max 5MB          │
│ 5 min duration   │  │                  │
└──────────────────┘  └──────────────────┘

Existing tables used:
┌──────────────────┐  ┌──────────────────┐
│ homework         │  │ messages         │
│ (existing)       │  │ (modified)       │
│ description      │  │ + audio_url      │
│ (text field)     │  │                  │
└──────────────────┘  └──────────────────┘
```

---

## Appendix C: Event Flow

### Domain Events

| Event | Emitter | Consumers | Payload | Side Effects |
|---|---|---|---|---|
| `AudioTranscribed` | `TranscriptionService.transcribeAudio()` | None (logged) | `userId, language, durationSeconds, textLength` | Analytics tracking |
| `ImageTextExtracted` | `TranscriptionService.extractTextFromImage()` | None (logged) | `userId, imageUrl, textLength` | Analytics tracking |
| `VoiceMessageSent` | `MessagingService.send()` (with audio_url) | `Notify.kt` | `senderId, recipientId, audioUrl, hasTranscription` | Notification to recipient |

### Event Delivery Guarantees

- Audio transcribed event: emitted synchronously (fire-and-forget logging)
- Image text extracted event: emitted synchronously (fire-and-forget logging)
- Voice message sent event: emitted synchronously, notification async

---

## Appendix D: Configuration

### Environment Variables

| Variable | Default | Description |
|---|---|---|
| `VOICE_PHOTO_CAPTURE_ENABLED` | `true` | Enable/disable feature |
| `TRANSCRIPTION_AI_MODEL` | `whisper-1` | AI model for speech-to-text |
| `OCR_AI_MODEL` | `extract_text_v1` | AI model for OCR |
| `MAX_AUDIO_DURATION_SECONDS` | `300` | Max audio recording duration (5 min) |
| `MAX_AUDIO_FILE_SIZE_MB` | `10` | Max audio file size |
| `MAX_IMAGE_FILE_SIZE_MB` | `5` | Max image file size |
| `SUPPORTED_LANGUAGES` | `en,hi,bn,ta,te,mr,gu,kn,ml,pa` | Supported voice recognition languages |
| `TRANSCRIPTION_RATE_LIMIT` | `10` | Requests per minute per user |
| `OCR_RATE_LIMIT` | `10` | Requests per minute per user |

### Feature Flags

| Flag | Default | Description |
|---|---|---|
| `VOICE_INPUT_ENABLED` | `true` | Enable/disable voice input |
| `PHOTO_OCR_ENABLED` | `true` | Enable/disable photo OCR |
| `VOICE_MESSAGES_ENABLED` | `true` | Enable/disable voice messages |
| `VISUAL_QA_ENABLED` | `true` | Enable/disable visual QA |

### School-Level Settings

| Setting | Default | Description |
|---|---|---|
| `voice_photo_capture_enabled` | `true` | Per-school enable/disable |
| `supported_languages` | all | Which languages are available |

---

## Appendix E: Migration & Rollback

### Migration: `migration_084_voice_photo_capture.sql`

```sql
-- Migration 084: Voice & Photo Capture
-- Adds audio_url column to messages table for voice messages

BEGIN;

ALTER TABLE messages ADD COLUMN IF NOT EXISTS audio_url TEXT;

COMMIT;
```

### Rollback: `migration_084_rollback.sql`

```sql
BEGIN;
ALTER TABLE messages DROP COLUMN IF EXISTS audio_url;
COMMIT;
```

### Migration Validation

- Verify `audio_url` column added to `messages` table
- Verify column is nullable TEXT
- Run `SELECT count(*) FROM messages WHERE audio_url IS NOT NULL` — should be 0 (new feature)

---

## Appendix F: Observability

### Structured Logging

| Log Level | Event | Context Fields |
|---|---|---|
| INFO | Audio transcribed | `userId, language, durationSeconds, textLength` |
| INFO | Image text extracted | `userId, textLength` |
| INFO | Voice message sent | `senderId, recipientId, hasTranscription` |
| WARN | Transcription returned empty | `userId, language, durationSeconds` |
| WARN | OCR returned empty | `userId` |
| WARN | Unsupported language requested | `userId, language` |
| ERROR | Transcription failed | `userId, error` |
| ERROR | OCR failed | `userId, error` |
| ERROR | Upload failed | `userId, fileType, error` |

### Metrics

| Metric | Type | Labels | Description |
|---|---|---|---|
| `voice_transcriptions_total` | Counter | `school_id, language` | Total transcription requests |
| `voice_transcription_success` | Counter | `school_id` | Successful transcriptions |
| `voice_transcription_duration` | Histogram | `school_id` | Transcription processing time |
| `ocr_requests_total` | Counter | `school_id` | Total OCR requests |
| `ocr_success_total` | Counter | `school_id` | Successful OCR requests |
| `ocr_duration` | Histogram | `school_id` | OCR processing time |
| `voice_messages_sent` | Counter | `school_id` | Total voice messages sent |
| `audio_upload_duration` | Histogram | `school_id` | Audio upload time |
| `avg_audio_duration` | Gauge | `school_id` | Average audio recording duration |

### Health Checks

| Check | Endpoint | Description |
|---|---|---|
| Transcription service | `/health/transcription` | Verify service and AI API accessible |
| Supabase Storage | `/health/storage` | Verify storage accessible |

### Alerts

| Alert | Condition | Severity | Notification |
|---|---|---|---|
| Transcription failure rate | > 20% | Warning | Email to dev team |
| OCR failure rate | > 20% | Warning | Email to dev team |
| AI API cost spike | > 2x daily average | Warning | Email to dev team |
| Upload failure rate | > 10% | Warning | Email to dev team |
| Storage usage high | > 80% capacity | Warning | Email to dev team |

### Dashboards

| Dashboard | Panels | Audience |
|---|---|---|
| Voice/Photo Usage | Transcriptions/day, OCR/day, voice messages/day, language distribution | Product Team |
| AI Performance | Transcription duration, OCR duration, success rates | Dev Team |
| Storage | Audio storage used, image storage used, growth trend | Dev Team |
| Cost Monitoring | AI API calls/day, estimated cost, cost trend | Dev Team |

### Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| AI API cost overrun | Medium | Medium | Rate limiting. Per-user limits. Monitor cost dashboard. |
| AI service unavailable | Low | Medium | Fallback to manual typing. Error message. |
| Poor transcription quality | Medium | Low | User can edit text. Multi-language models improving. |
| Poor OCR quality | Medium | Low | User can edit text. Vision models improving. |
| Privacy concern with audio | Low | Medium | Audio stored in private bucket. Signed URLs. Temporary for OCR. |
| Mic/camera permission denied | Medium | Low | Clear UI prompt. Fallback to manual input. |
| Large file uploads fail | Low | Low | Client-side compression. Size limits. Retry. |
| Unsupported language | Low | Low | Language selector. Clear error message. |
