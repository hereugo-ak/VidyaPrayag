# Voice & Photo Capture — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** `AI_INFRASTRUCTURE_SPEC.md`
> **Source:** `DIFFERENTIATING_FEATURES.md` §5.3

---

## 1. Feature Overview

Voice-to-text input and photo capture with AI processing for teachers and parents. Enables hands-free homework assignment dictation, voice notes, photo-to-text extraction, and visual question answering.

### Goals

- Voice input: teacher dictates homework/announcement → AI transcribes to text
- Photo capture: teacher photographs worksheet → AI extracts text for homework description
- Voice notes: parent sends voice message (transcribed automatically)
- Visual question answering: student photographs problem → AI explains (via `VIDYASETU_AI_TUTOR_SPEC.md`)
- Multi-language voice recognition (English, Hindi, regional)

---

## 2. Current System Assessment

- No voice/speech recognition in app
- `HomeworkTable` — homework description is text input (manual typing)
- `MESSAGING_SYSTEM_SPEC.md` — text messages only, no voice messages
- `AI_INFRASTRUCTURE_SPEC.md` — LLM with vision capabilities (GPT-4o, Gemini)
- `DIFFERENTIATING_FEATURES.md` §5.3: Voice/Photo Capture, effort M

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Voice input: press-and-hold microphone → speak → AI transcribes → text field populated |
| FR-2 | Photo capture: take photo → AI extracts text (OCR) → text field populated |
| FR-3 | Voice notes in messages: record voice → transcribed → sent as text + audio attachment |
| FR-4 | Visual QA: photo of problem → AI explains (delegates to AI Tutor) |
| FR-5 | Multi-language: voice recognition in 10 Indian languages |
| FR-6 | Editable: transcribed text always editable before saving/sending |

---

## 4. Frontend Architecture

### 4.1 Voice Input Component

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

### 4.2 Platform Audio Recording

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

## 5. Backend Architecture

### 5.1 TranscriptionService

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

### 5.2 Voice Messages (Messaging Integration)

Voice message in messaging:
1. Parent records voice → audio uploaded to Supabase Storage
2. `TranscriptionService.transcribeAudio()` → text
3. Message sent with both `audioUrl` and transcribed `body`
4. Recipient sees text + play button for audio

---

## 6. API Contracts

```
POST /api/v1/ai/transcribe  { audio_url, language }  → { text }
POST /api/v1/ai/extract-text  { image_url }  → { text }
```

These are internal endpoints called by client during input, not user-facing APIs.

---

## 7. Acceptance Criteria

- [ ] Voice input: speak → transcribed text populates field
- [ ] Photo capture: photo → OCR text populates field
- [ ] Voice messages in chat: audio + transcription
- [ ] Visual QA: photo of problem → AI explanation
- [ ] Multi-language voice recognition (at least English + Hindi)
- [ ] Transcribed text always editable

---

## 8. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 2 days | AudioRecorder expect/actual (Android + iOS + Web) |
| 2 | 2 days | TranscriptionService (speech-to-text + image OCR) |
| 3 | 2 days | VoiceInputField composable + integration into homework/announcement forms |
| 4 | 2 days | Voice messages in messaging |
| 5 | 2 days | Visual QA integration with AI Tutor |
| 6 | 2 days | Tests |

---

## 9. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `shared/.../core/audio/AudioRecorder.kt` | New + expect/actual | Platform audio recording |
| `server/.../feature/ai/TranscriptionService.kt` | New | Speech-to-text + OCR |
| `server/.../feature/ai/AiRouting.kt` | Modify | Add transcribe + extract-text endpoints |
| `composeApp/.../ui/v2/components/VoiceInputField.kt` | New | Voice input composable |
| `composeApp/.../ui/v2/screens/teacher/HomeworkComposerScreen.kt` | Modify | Add voice/photo input |
| `composeApp/.../ui/v2/screens/parent/AiTutorScreen.kt` | Modify | Add photo capture for visual QA |
