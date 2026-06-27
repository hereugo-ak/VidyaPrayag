# Live Classes (Virtual Classroom) — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** None
> **Source:** `COMPETITIVE_GAP_ANALYSIS.md` — emerging trend

---

## 1. Feature Overview

Live virtual classroom integration: teachers can conduct live online classes with video, audio, screen sharing, whiteboard, and recording. Supports attendance tracking, hand raise, chat, and breakout rooms.

### Goals

- Teacher schedules and starts live class from app
- Students join via link (in-app or browser)
- Video + audio + screen share + digital whiteboard
- Attendance tracking (who joined, duration)
- Class recording stored for later viewing
- Chat within live class
- Hand raise for student questions
- Integration with timetable (from `AI_TIMETABLE_SPEC.md`)

---

## 2. Current System Assessment

- No video conferencing or live class system
- `COMPETITIVE_GAP_ANALYSIS.md`: live classes as emerging trend
- Ktor server can handle signaling/WebRTC
- Compose Multiplatform supports video rendering

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Teacher schedules live class (title, subject, class, date, time, duration) |
| FR-2 | Teacher starts class → generates join link |
| FR-3 | Students join via in-app button or browser link |
| FR-4 | Video + audio for teacher; audio + optional video for students |
| FR-5 | Screen sharing (teacher) |
| FR-6 | Digital whiteboard (shared drawing surface) |
| FR-7 | Chat: text messages during class |
| FR-8 | Hand raise: student signals to speak |
| FR-9 | Attendance: track join time, leave time, duration per student |
| FR-10 | Recording: class recorded and stored (Supabase Storage) |
| FR-11 | Notification: students notified 10 min before class starts |
| FR-12 | Post-class: recording available for absent students |

---

## 4. Database Design

```sql
CREATE TABLE live_classes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    teacher_id      UUID NOT NULL,
    class_id        UUID NOT NULL,
    subject_name    TEXT,
    title           TEXT NOT NULL,
    description     TEXT,
    scheduled_at    TIMESTAMP NOT NULL,
    duration_minutes INTEGER NOT NULL DEFAULT 45,
    status          VARCHAR(16) NOT NULL DEFAULT 'scheduled', -- scheduled | live | ended | cancelled
    meeting_id      TEXT,                          -- external meeting ID (WebRTC room)
    recording_url   TEXT,                          -- Supabase Storage URL (post-class)
    started_at      TIMESTAMP,
    ended_at        TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_live_classes_school ON live_classes(school_id, scheduled_at DESC);

CREATE TABLE live_class_attendance (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    live_class_id   UUID NOT NULL REFERENCES live_classes(id) ON DELETE CASCADE,
    student_id      UUID NOT NULL,
    student_name    TEXT NOT NULL,
    joined_at       TIMESTAMP,
    left_at         TIMESTAMP,
    duration_seconds INTEGER,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(live_class_id, student_id)
);

CREATE TABLE live_class_chat (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    live_class_id   UUID NOT NULL REFERENCES live_classes(id) ON DELETE CASCADE,
    sender_id       UUID NOT NULL,
    sender_name     TEXT NOT NULL,
    sender_role     VARCHAR(32) NOT NULL,
    message         TEXT NOT NULL,
    is_hand_raise   BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
```

---

## 5. Backend Architecture

### 5.1 Video Conferencing Approach

**Option A: WebRTC (self-hosted)**
- Ktor server as signaling server
- WebRTC peer-to-peer or SFU (Selective Forwarding Unit) for multi-party
- SFU: mediasoup or Jitsi Videobridge
- Pros: no per-minute cost, full control
- Cons: infrastructure complexity, scaling challenges

**Option B: Third-party SDK (recommended for initial)**
- Dyte, VideoSDK, or 100ms (Indian-friendly providers)
- Pre-built UI, recording, chat, whiteboard
- Pros: fast integration, managed infrastructure
- Cons: per-participant-minute cost

**Recommended:** Start with Option B (Dyte/100ms), migrate to Option A if costs grow.

### 5.2 LiveClassService

```kotlin
class LiveClassService {
    suspend fun schedule(request: ScheduleRequest): LiveClassDto
    suspend fun start(classId: UUID): StartResult  // creates meeting room, returns join token
    suspend fun join(classId: UUID, userId: UUID): JoinToken  // per-user join token
    suspend fun end(classId: UUID): EndResult  // stops recording, marks ended
    suspend fun recordAttendance(classId: UUID, joinEvents: List<JoinEvent>)
    suspend fun getRecording(classId: UUID): String  // recording URL
}
```

---

## 6. API Contracts

```
# Teacher
POST /api/v1/teacher/live-classes  { title, class_id, subject, scheduled_at, duration }
POST /api/v1/teacher/live-classes/{id}/start
POST /api/v1/teacher/live-classes/{id}/end
GET /api/v1/teacher/live-classes?status={status}

# Student (via parent app)
GET /api/v1/parent/live-classes/{childId}
POST /api/v1/parent/live-classes/{id}/join
GET /api/v1/parent/live-classes/{id}/recording
```

---

## 7. Acceptance Criteria

- [ ] Teacher schedules and starts live class
- [ ] Students join via in-app or browser
- [ ] Video, audio, screen share, whiteboard work
- [ ] Chat and hand raise during class
- [ ] Attendance tracked (join/leave/duration)
- [ ] Class recording stored and viewable
- [ ] Notification sent before class starts
- [ ] Recording available for absent students

---

## 8. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 2 days | DB migration, Exposed tables |
| 2 | 3 days | Video SDK integration (Dyte/100ms) |
| 3 | 2 days | LiveClassService (schedule, start, join, end) |
| 4 | 2 days | Attendance tracking + chat |
| 5 | 2 days | Recording storage + retrieval |
| 6 | 1 day | Notification integration |
| 7 | 5 days | Client UI (teacher: schedule + live class; parent: join + recording) |
| 8 | 2 days | Tests |

---

## 9. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | 3 live class tables |
| `server/.../feature/liveclass/LiveClassService.kt` | New | Core service |
| `server/.../feature/liveclass/LiveClassRouting.kt` | New | API endpoints |
| `docs/db/migration_086_live_classes.sql` | New | DDL |
| `composeApp/.../ui/v2/screens/teacher/LiveClassScreen.kt` | New | Teacher live class |
| `composeApp/.../ui/v2/screens/parent/JoinLiveClassScreen.kt` | New | Student join |
| `composeApp/.../ui/v2/screens/parent/ClassRecordingScreen.kt` | New | Recording viewer |
