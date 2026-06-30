# Live Classes (Virtual Classroom) — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** None
> **Source:** `COMPETITIVE_GAP_ANALYSIS.md` — emerging trend
> **Template:** `_SPEC_TEMPLATE.md` v1 (25 mandatory + 6 optional sections)

---

## 1. Feature Overview

### What

Live virtual classroom integration: teachers can conduct live online classes with video, audio, screen sharing, whiteboard, and recording. Supports attendance tracking, hand raise, chat, and breakout rooms.

### Why — Product Rationale

Schools increasingly need remote learning capabilities for snow days, holidays, student absences, and hybrid learning. Live classes enable teachers to conduct virtual classes with full interactivity. This is a **medium-priority emerging tech feature** (Phase 4, effort L) that provides competitive differentiation.

### What Stands Out (Competitive Moat)

From `COMPETITIVE_GAP_ANALYSIS.md`: live classes as emerging trend.

Few Indian school ERPs offer integrated live classes with attendance tracking, recording, and timetable integration. The key moat is **integrated live classes with attendance tracking and recording** — not just video calls, but a complete virtual classroom tied to the school ERP.

### Goals

- Teacher schedules and starts live class from app
- Students join via link (in-app or browser)
- Video + audio + screen share + digital whiteboard
- Attendance tracking (who joined, duration)
- Class recording stored for later viewing
- Chat within live class
- Hand raise for student questions
- Integration with timetable (from `AI_TIMETABLE_SPEC.md`)

### Non-goals

- [ ] Breakout rooms (future enhancement, not initial scope)
- [ ] Polls and quizzes during live class (future enhancement)
- [ ] Co-teaching / multiple teachers in one class (future enhancement)
- [ ] Virtual background / face filters (not needed for education)
- [ ] Live transcription / captions (future enhancement)
- [ ] Attendance certificate generation (future enhancement)
- [ ] Paid classes / monetization (not for school context)
- [ ] Live streaming to YouTube (not needed)
- [ ] Whiteboard export as PDF (future enhancement)

### Dependencies

- `AI_TIMETABLE_SPEC.md` — timetable integration (schedule from timetable)
- `NotificationsTable` — notification infrastructure (class start notification)
- `StudentsTable` — student info for attendance
- `AppUsersTable` — teacher accounts
- `ClassesTable` — class information
- Supabase Storage — recording storage
- Video SDK provider (Dyte/100ms/VideoSDK) — video conferencing

### Related Modules

- `server/.../feature/liveclass/` — new live class module
- `server/.../feature/notification/` — notification infrastructure
- `composeApp/.../ui/v2/screens/teacher/` — teacher UI (schedule + live class)
- `composeApp/.../ui/v2/screens/parent/` — parent UI (join + recording)

---

## 2. Current System Assessment

### Existing Code

- No video conferencing or live class system
- `COMPETITIVE_GAP_ANALYSIS.md`: live classes as emerging trend
- Ktor server can handle signaling/WebRTC
- Compose Multiplatform supports video rendering

### Existing Database

- `StudentsTable` — student info
- `AppUsersTable` — teacher accounts
- `ClassesTable` — class information
- `SchoolsTable` — school info
- `NotificationsTable` — notification infrastructure
- No live class tables

### Existing APIs

- Notification API (existing) — notification dispatch
- Timetable API (existing, from `AI_TIMETABLE_SPEC.md`) — class schedule
- Student API (existing) — student info
- No live class API

### Existing UI

- Teacher dashboard (existing) — no live class option
- Parent home (existing) — no live class join
- No live class UI

### Existing Services

- `NotificationService` — notification dispatch
- `TimetableService` — timetable management
- No live class service

### Existing Documentation

- `COMPETITIVE_GAP_ANALYSIS.md` — live classes as emerging trend
- `AI_TIMETABLE_SPEC.md` — timetable integration
- `SMART_NOTIFICATIONS_SPEC.md` — notification priority routing

### Technical Debt

| # | Gap | Details |
|---|---|---|
| TD-1 | No live class tables | No `live_classes`, `live_class_attendance`, `live_class_chat` tables |
| TD-2 | No video SDK integration | No video conferencing SDK integrated |
| TD-3 | No live class service | No service for schedule, start, join, end, attendance |
| TD-4 | No recording storage | No recording storage and retrieval |
| TD-5 | No live class UI | No teacher or parent live class UI |

### Gaps

| # | Gap | Impact | Severity |
|---|---|---|---|
| G1 | No live class scheduling | Teachers can't schedule virtual classes | **High** |
| G2 | No video conferencing | No virtual classroom capability | **High** |
| G3 | No attendance tracking | Can't track student participation | **High** |
| G4 | No recording | Absent students can't catch up | **Medium** |
| G5 | No chat/hand raise | Limited interactivity | **Medium** |
| G6 | No notification | Students not informed of class start | **Medium** |

---

## 3. Functional Requirements

### FR-001
| Field | Value |
|---|---|
| **Title** | Schedule Live Class |
| **Description** | Teacher schedules live class (title, subject, class, date, time, duration). |
| **Priority** | High |
| **User Roles** | Teacher |
| **Acceptance notes** | Teacher creates live class with title, subject_name, class_id, scheduled_at, duration_minutes. Status = scheduled. Can be created from timetable slot or standalone. |

### FR-002
| Field | Value |
|---|---|
| **Title** | Start Live Class |
| **Description** | Teacher starts class → generates join link. |
| **Priority** | High |
| **User Roles** | Teacher |
| **Acceptance notes** | Teacher taps "Start Class" → meeting room created (via video SDK) → join token generated. Status = live. started_at recorded. Students notified. |

### FR-003
| Field | Value |
|---|---|
| **Title** | Join Live Class |
| **Description** | Students join via in-app button or browser link. |
| **Priority** | High |
| **User Roles** | Parent (on behalf of student) |
| **Acceptance notes** | Parent sees upcoming live class for child. Taps "Join" → joins via in-app video SDK or browser link. Per-user join token generated. |

### FR-004
| Field | Value |
|---|---|
| **Title** | Video and Audio |
| **Description** | Video + audio for teacher; audio + optional video for students. |
| **Priority** | High |
| **User Roles** | Teacher, Student (via parent app) |
| **Acceptance notes** | Teacher video + audio always on (can mute). Student audio on, video optional (can enable/disable). Bandwidth adaptive. |

### FR-005
| Field | Value |
|---|---|
| **Title** | Screen Sharing |
| **Description** | Screen sharing (teacher). |
| **Priority** | Medium |
| **User Roles** | Teacher |
| **Acceptance notes** | Teacher can share screen (presentation, slides, documents). Students view shared screen. |

### FR-006
| Field | Value |
|---|---|
| **Title** | Digital Whiteboard |
| **Description** | Digital whiteboard (shared drawing surface). |
| **Priority** | Medium |
| **User Roles** | Teacher |
| **Acceptance notes** | Teacher can draw/write on digital whiteboard. Students view whiteboard in real-time. Part of video SDK (Dyte/100ms includes whiteboard). |

### FR-007
| Field | Value |
|---|---|
| **Title** | Chat |
| **Description** | Chat: text messages during class. |
| **Priority** | Medium |
| **User Roles** | Teacher, Student (via parent app) |
| **Acceptance notes** | Text chat during live class. Messages stored in `live_class_chat`. Teacher and students can send messages. |

### FR-008
| Field | Value |
|---|---|
| **Title** | Hand Raise |
| **Description** | Hand raise: student signals to speak. |
| **Priority** | Medium |
| **User Roles** | Student (via parent app) |
| **Acceptance notes** | Student taps "Raise Hand" → teacher sees hand raise indicator. Teacher can acknowledge or call on student. Stored as chat message with `is_hand_raise = true`. |

### FR-009
| Field | Value |
|---|---|
| **Title** | Attendance Tracking |
| **Description** | Attendance: track join time, leave time, duration per student. |
| **Priority** | High |
| **User Roles** | System |
| **Acceptance notes** | Track student join_at, left_at, duration_seconds. Stored in `live_class_attendance`. UNIQUE(live_class_id, student_id). Teacher can view attendance post-class. |

### FR-010
| Field | Value |
|---|---|
| **Title** | Recording |
| **Description** | Recording: class recorded and stored (Supabase Storage). |
| **Priority** | High |
| **User Roles** | System |
| **Acceptance notes** | Class recorded via video SDK. Recording stored in Supabase Storage. recording_url saved to `live_classes` post-class. Available for absent students. |

### FR-011
| Field | Value |
|---|---|
| **Title** | Pre-class Notification |
| **Description** | Notification: students notified 10 min before class starts. |
| **Priority** | Medium |
| **User Roles** | System |
| **Acceptance notes** | FCM notification 10 min before scheduled_at. "Live class '{title}' starts in 10 minutes." Uses SmartNotificationService with "high" priority. |

### FR-012
| Field | Value |
|---|---|
| **Title** | Post-class Recording Access |
| **Description** | Post-class: recording available for absent students. |
| **Priority** | Medium |
| **User Roles** | Parent (on behalf of student) |
| **Acceptance notes** | Recording URL available in parent app. Students who missed class can view recording. Recording retained per school policy (default 30 days). |

### Non-Functional Requirements

| ID | Requirement |
|---|---|
| NFR-1 | Video latency: < 500ms (real-time) |
| NFR-2 | Audio latency: < 200ms |
| NFR-3 | Max participants: 50 (class size) |
| NFR-4 | Recording storage: Supabase Storage, 30-day retention (configurable) |
| NFR-5 | Notification: 10 min before class (configurable) |
| NFR-6 | Join time: < 5 seconds (from tap to joined) |
| NFR-7 | Chat message delivery: < 1 second |
| NFR-8 | Screen share latency: < 1 second |

---

## 4. User Stories

### Teacher
- [ ] Schedule a live class for my subject and class
- [ ] Start live class and get join link
- [ ] Conduct class with video, audio, screen share, whiteboard
- [ ] See chat messages and hand raises from students
- [ ] End class and view attendance
- [ ] View recording after class

### Student (via Parent App)
- [ ] See upcoming live classes for my class
- [ ] Get notified before class starts
- [ ] Join live class from app
- [ ] Participate in chat and raise hand
- [ ] View recording if I missed the class

### System
- [ ] Create meeting room when teacher starts class
- [ ] Track student join/leave/duration
- [ ] Record class and store in Supabase Storage
- [ ] Send notification 10 min before class
- [ ] Save chat messages
- [ ] Save recording URL post-class

---

## 5. Business Rules

### BR-001
**Rule:** Only teacher can schedule and start live class.
**Enforcement:** Schedule/start endpoints require teacher role + JWT auth. Teacher must be assigned to the class.

### BR-002
**Rule:** Students join via parent app.
**Enforcement:** Join endpoint requires parent role + JWT auth + parent-student relationship verified. Student must be in the class.

### BR-003
**Rule:** One attendance record per student per live class.
**Enforcement:** `UNIQUE(live_class_id, student_id)` constraint. If student leaves and rejoins, update existing record (extend duration).

### BR-004
**Rule:** Recording retained for 30 days (configurable).
**Enforcement:** Recording stored in Supabase Storage with 30-day retention policy. Configurable per school. Auto-deleted after retention period.

### BR-005
**Rule:** Notification 10 minutes before class (configurable).
**Enforcement:** Background job checks for classes starting in 10 minutes. Sends FCM notification to all students in the class. Configurable threshold.

### BR-006
**Rule:** Live class is school-scoped.
**Enforcement:** All live classes have `school_id`. Teachers and students see only own school's classes. No cross-school access.

### BR-007
**Rule:** Teacher can cancel scheduled class.
**Enforcement:** Teacher can cancel before start. Status = cancelled. Students notified of cancellation.

### BR-008
**Rule:** Chat messages retained with class.
**Enforcement:** Chat messages in `live_class_chat` retained as long as live class exists. `ON DELETE CASCADE` from `live_classes`.

### BR-009
**Rule:** Hand raise is a special chat message.
**Enforcement:** Hand raise stored as chat message with `is_hand_raise = true`. Teacher sees hand raise indicator in UI.

### BR-010
**Rule:** Duration tracked in seconds.
**Enforcement:** `duration_seconds` = `left_at - joined_at` in seconds. If student still in class when ended, `left_at` = class `ended_at`.

---

## 6. Database Design

### 6.1 Entity Relationship Summary

Three new tables: `live_classes` (class scheduling and status), `live_class_attendance` (student attendance), `live_class_chat` (chat messages). References existing `students`, `app_users`, `classes`, and `schools` tables.

### 6.2 New Tables

#### `live_classes` table

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
```

#### `live_class_attendance` table

```sql
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
```

#### `live_class_chat` table

```sql
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

### 6.3 Modified Tables

N/A — no existing tables modified. Attendance tracking uses new `live_class_attendance` table (separate from regular `AttendanceRecordsTable`).

### 6.4 Indexes

- `idx_live_classes_school` on `live_classes(school_id, scheduled_at DESC)` — dashboard lookup
- `live_class_attendance(live_class_id, student_id)` — UNIQUE constraint (implicit index)
- `live_class_chat(live_class_id, created_at)` — for chat message ordering (recommended)

### 6.5 Constraints

- `live_classes.title` — NOT NULL
- `live_classes.scheduled_at` — NOT NULL
- `live_classes.duration_minutes` — NOT NULL, default 45
- `live_classes.status` — NOT NULL, default 'scheduled', values: scheduled | live | ended | cancelled
- `live_class_attendance.student_name` — NOT NULL
- `live_class_attendance(live_class_id, student_id)` — UNIQUE
- `live_class_chat.sender_name` — NOT NULL
- `live_class_chat.sender_role` — NOT NULL
- `live_class_chat.message` — NOT NULL
- `live_class_chat.is_hand_raise` — NOT NULL, default false

### 6.6 Foreign Keys

- `live_classes.school_id` → `schools.id` (implicit)
- `live_classes.teacher_id` → `app_users.id` (implicit)
- `live_classes.class_id` → `classes.id` (implicit)
- `live_class_attendance.live_class_id` → `live_classes.id` (explicit, ON DELETE CASCADE)
- `live_class_attendance.student_id` → `students.id` (implicit)
- `live_class_chat.live_class_id` → `live_classes.id` (explicit, ON DELETE CASCADE)

### 6.7 Soft Delete Strategy

N/A — live classes are not soft deleted. Cancelled classes have status = 'cancelled'. Ended classes have status = 'ended'. All retained for audit and recording access.

### 6.8 Audit Fields

- `live_classes.created_at` — when class was scheduled
- `live_classes.started_at` — when class went live
- `live_classes.ended_at` — when class ended
- `live_class_attendance.joined_at` — when student joined
- `live_class_attendance.left_at` — when student left
- `live_class_attendance.duration_seconds` — total time in class
- `live_class_chat.created_at` — when message was sent

### 6.9 Migration Notes

Migration: `docs/db/migration_087_live_classes.sql`
- CREATE 3 tables: `live_classes`, `live_class_attendance`, `live_class_chat`
- CREATE 1 index: `idx_live_classes_school`
- No data migration (new feature)

### 6.10 Exposed Mappings

```kotlin
object LiveClassesTable : UUIDTable("live_classes", "id") {
    val schoolId       = uuid("school_id")
    val teacherId      = uuid("teacher_id")
    val classId        = uuid("class_id")
    val subjectName    = text("subject_name").nullable()
    val title          = text("title")
    val description    = text("description").nullable()
    val scheduledAt    = timestamp("scheduled_at")
    val durationMinutes= integer("duration_minutes").default(45)
    val status         = varchar("status", 16).default("scheduled")
    val meetingId      = text("meeting_id").nullable()
    val recordingUrl   = text("recording_url").nullable()
    val startedAt      = timestamp("started_at").nullable()
    val endedAt        = timestamp("ended_at").nullable()
    val createdAt      = timestamp("created_at")

    init {
        index("idx_live_classes_school", schoolId, scheduledAt)
    }
}

object LiveClassAttendanceTable : UUIDTable("live_class_attendance", "id") {
    val liveClassId    = uuid("live_class_id").references(LiveClassesTable.id, onDelete = ReferenceOption.CASCADE)
    val studentId      = uuid("student_id")
    val studentName    = text("student_name")
    val joinedAt       = timestamp("joined_at").nullable()
    val leftAt         = timestamp("left_at").nullable()
    val durationSeconds= integer("duration_seconds").nullable()
    val createdAt      = timestamp("created_at")

    init {
        uniqueIndex("idx_live_class_att_unique", liveClassId, studentId)
    }
}

object LiveClassChatTable : UUIDTable("live_class_chat", "id") {
    val liveClassId    = uuid("live_class_id").references(LiveClassesTable.id, onDelete = ReferenceOption.CASCADE)
    val senderId       = uuid("sender_id")
    val senderName     = text("sender_name")
    val senderRole     = varchar("sender_role", 32)
    val message        = text("message")
    val isHandRaise    = bool("is_hand_raise").default(false)
    val createdAt      = timestamp("created_at")
}
```

Register in `DatabaseFactory.allTables`.

### 6.11 Seed Data

N/A — live classes are user-created. No seed data needed.

---

## 7. State Machines

### Live Class Lifecycle State Machine

```
scheduled ──teacher_starts──> live ──teacher_ends──> ended ──(terminal)
  │                              │
  │                              └──teacher_cancels──> cancelled (rare, mid-class)
  │
  └──teacher_cancels──> cancelled ──(terminal)
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `scheduled` | Teacher starts class | `live` | Meeting room created, join token generated |
| `scheduled` | Teacher cancels (before start) | `cancelled` | Students notified of cancellation |
| `live` | Teacher ends class | `ended` | Recording saved, attendance finalized |
| `live` | Teacher cancels (mid-class, rare) | `cancelled` | Emergency cancellation |
| `ended` | — | `ended` | Terminal state (recording available) |
| `cancelled` | — | `cancelled` | Terminal state |

### Student Join/Leave State Machine

```
not_joined ──student_joins──> in_class ──student_leaves──> left
  │                               │
  │                               └──class_ends──> left (auto)
  │
  └──left ──student_rejoins──> in_class ──student_leaves──> left
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `not_joined` | Student joins class | `in_class` | joined_at recorded |
| `in_class` | Student leaves | `left` | left_at recorded, duration calculated |
| `in_class` | Class ends | `left` | left_at = class ended_at, duration calculated |
| `left` | Student rejoins | `in_class` | Update existing record, extend duration |
| `left` | — | `left` | Terminal (for this session) |

### Recording State Machine

```
not_recording ──class_starts──> recording ──class_ends──> recorded
  │                                │
  │                                └──recording_fails──> failed
  │
  └──recorded ──retention_expired──> deleted (after 30 days)
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `not_recording` | Class starts (recording enabled) | `recording` | Video SDK starts recording |
| `recording` | Class ends | `recorded` | Recording saved to Supabase Storage |
| `recording` | Recording fails | `failed` | Log error, no recording available |
| `recorded` | Retention period expires (30 days) | `deleted` | Auto-deleted from Supabase Storage |

---

## 8. Backend Architecture

### 8.1 Component Overview

`LiveClassService` handles scheduling, starting, joining, ending, attendance tracking, and recording retrieval. Integrates with external video SDK (Dyte/100ms) for meeting room creation and recording. `LiveClassNotificationJob` sends pre-class notifications.

### 8.2 Design Principles

1. **Third-party SDK first** — use Dyte/100ms for video, migrate to self-hosted if costs grow
2. **Integrated with ERP** — live classes tied to timetable, attendance, and student records
3. **Recording for absentees** — recording available for students who missed class
4. **School-scoped** — all live classes school-specific, no cross-school access
5. **Attendance tracking** — automatic join/leave/duration tracking
6. **Notification-driven** — students notified before class starts

### 8.3 Core Types

#### LiveClassService

```kotlin
class LiveClassService {
    suspend fun schedule(request: ScheduleRequest): LiveClassDto
    suspend fun start(classId: UUID): StartResult  // creates meeting room, returns join token
    suspend fun join(classId: UUID, userId: UUID): JoinToken  // per-user join token
    suspend fun end(classId: UUID): EndResult  // stops recording, marks ended
    suspend fun recordAttendance(classId: UUID, joinEvents: List<JoinEvent>)
    suspend fun getRecording(classId: UUID): String  // recording URL
    suspend fun cancel(classId: UUID): LiveClassDto
    suspend fun getUpcomingClasses(schoolId: UUID, classId: UUID): List<LiveClassDto>
    suspend fun getAttendance(classId: UUID): List<LiveClassAttendanceDto>
    suspend fun getChat(classId: UUID): List<LiveClassChatDto>
}
```

#### Video SDK Integration

```kotlin
interface VideoSdkProvider {
    suspend fun createMeeting(title: String): MeetingResult  // returns meeting_id + join token
    suspend fun generateJoinToken(meetingId: String, userId: String, userName: String, role: String): String
    suspend fun endMeeting(meetingId: String): Boolean
    suspend fun startRecording(meetingId: String): Boolean
    suspend fun stopRecording(meetingId: String): RecordingResult  // returns recording URL
    suspend fun getRecording(meetingId: String): String?
}

// Implementations: DyteProvider, HundredMsProvider, VideoSdkProvider
```

### 8.4 Repositories

- `LiveClassRepository` — CRUD for `live_classes`
- `LiveClassAttendanceRepository` — CRUD for `live_class_attendance`
- `LiveClassChatRepository` — CRUD for `live_class_chat`

### 8.5 Mappers

- `LiveClassMapper` — maps `live_classes` rows to `LiveClassDto`
- `LiveClassAttendanceMapper` — maps `live_class_attendance` rows to DTOs
- `LiveClassChatMapper` — maps `live_class_chat` rows to DTOs

### 8.6 Permission Checks

- Schedule: teacher role + JWT auth + teacher assigned to class
- Start: teacher role + JWT auth + own class
- Join: parent role + JWT auth + parent-student relationship + student in class
- End: teacher role + JWT auth + own class
- Cancel: teacher role + JWT auth + own class
- View attendance: teacher (own class) or school admin
- View recording: parent (own child) or teacher (own class) or school admin
- View chat: teacher (own class) or school admin

### 8.7 Background Jobs

- **Live Class Notification Job** — every 1 minute
  1. `SELECT * FROM live_classes WHERE status = 'scheduled' AND scheduled_at <= now() + 10 minutes AND scheduled_at > now()`
  2. For each: send FCM notification to all students in class
  3. "Live class '{title}' starts in 10 minutes."
  4. Mark notification as sent (prevent duplicate)

### 8.8 Domain Events

- `LiveClassScheduled` — emitted when class is scheduled
- `LiveClassStarted` — emitted when class goes live
- `LiveClassEnded` — emitted when class ends
- `LiveClassCancelled` — emitted when class is cancelled
- `StudentJoinedLiveClass` — emitted when student joins
- `StudentLeftLiveClass` — emitted when student leaves
- `RecordingAvailable` — emitted when recording URL is saved

### 8.9 Caching

- Upcoming classes: cached locally (1-minute TTL)
- Active live class: not cached (real-time status)
- Recording URL: cached locally (1-hour TTL, recording is immutable)

### 8.10 Transactions

- Schedule: single transaction (insert live_class)
- Start: single transaction (update status, set started_at, set meeting_id)
- End: single transaction (update status, set ended_at, set recording_url)
- Join: single transaction (insert/update attendance, set joined_at)
- Leave: single transaction (update attendance, set left_at, duration_seconds)
- Chat: single transaction (insert chat message)

### 8.11 Rate Limiting

- Schedule: 10 per day per teacher
- Start: 1 per minute per teacher
- Join: 5 per minute per parent (multiple children)
- Chat: 30 per minute per user (prevent spam)

### 8.12 Configuration

- `LIVE_CLASSES_ENABLED` — default `false`; enable/disable feature
- `VIDEO_SDK_PROVIDER` — default `dyte`; options: dyte | 100ms | videosdk | webrtc
- `VIDEO_SDK_API_KEY` — API key for video SDK provider
- `VIDEO_SDK_API_SECRET` — API secret for video SDK provider
- `RECORDING_RETENTION_DAYS` — default `30`; recording retention period
- `PRE_CLASS_NOTIFICATION_MINUTES` — default `10`; notification lead time
- `MAX_PARTICIPANTS` — default `50`; max participants per class
- `DEFAULT_DURATION_MINUTES` — default `45`; default class duration

### 8.13 Video Conferencing Approach

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

---

## 9. API Contracts

### 9.1 Teacher Endpoints

```
POST /api/v1/teacher/live-classes
  Body: { title, class_id, subject, scheduled_at, duration }
  → 201: LiveClassDto

POST /api/v1/teacher/live-classes/{id}/start
  → 200: StartResultDto (meeting_id, join_token)

POST /api/v1/teacher/live-classes/{id}/end
  → 200: LiveClassDto (ended)

POST /api/v1/teacher/live-classes/{id}/cancel
  → 200: LiveClassDto (cancelled)

GET /api/v1/teacher/live-classes?status={status}
  → 200: { classes: [LiveClassDto] }

GET /api/v1/teacher/live-classes/{id}/attendance
  → 200: { attendance: [LiveClassAttendanceDto] }

GET /api/v1/teacher/live-classes/{id}/chat
  → 200: { messages: [LiveClassChatDto] }
```

### 9.2 Parent Endpoints

```
GET /api/v1/parent/live-classes/{childId}
  → 200: { classes: [LiveClassDto] }

POST /api/v1/parent/live-classes/{id}/join
  → 200: JoinTokenDto

GET /api/v1/parent/live-classes/{id}/recording
  → 200: { recording_url: String }
```

### 9.3 DTO Models

All `@Serializable`, wrapped in `ApiResponse<T>` pattern.

```kotlin
@Serializable data class LiveClassDto(
    val id: String,
    val teacherId: String,
    val teacherName: String,
    val classId: String,
    val className: String,
    val subjectName: String?,
    val title: String,
    val description: String?,
    val scheduledAt: String,
    val durationMinutes: Int,
    val status: String,           // scheduled | live | ended | cancelled
    val recordingUrl: String?,
    val startedAt: String?,
    val endedAt: String?,
    val createdAt: String,
)

@Serializable data class StartResultDto(
    val meetingId: String,
    val joinToken: String,
    val liveClass: LiveClassDto,
)

@Serializable data class JoinTokenDto(
    val joinToken: String,
    val meetingId: String,
)

@Serializable data class LiveClassAttendanceDto(
    val studentId: String,
    val studentName: String,
    val joinedAt: String?,
    val leftAt: String?,
    val durationSeconds: Int?,
)

@Serializable data class LiveClassChatDto(
    val senderId: String,
    val senderName: String,
    val senderRole: String,
    val message: String,
    val isHandRaise: Boolean,
    val createdAt: String,
)

@Serializable data class ScheduleRequest(
    val title: String,
    val classId: String,
    val subjectName: String?,
    val scheduledAt: String,
    val durationMinutes: Int,
    val description: String?,
)
```

---

## 10. Frontend Architecture

### 10.1 Screens

| Screen | Platform | Role | Description |
|---|---|---|---|
| `LiveClassScheduleScreen` | Compose | Teacher | Schedule new live class |
| `LiveClassScreen` | Compose | Teacher | Conduct live class (video, chat, attendance) |
| `LiveClassListScreen` | Compose | Teacher | View upcoming/past live classes |
| `JoinLiveClassScreen` | Compose | Parent | Join live class for child |
| `ClassRecordingScreen` | Compose | Parent | View class recording |
| `LiveClassAttendanceScreen` | Compose | Teacher | View attendance post-class |

### 10.2 Navigation

- Teacher: Dashboard → Live Classes → Schedule → Start → Live Class → End → Attendance
- Parent: Home → Live Classes → Join → Live Class (in-app or browser) → Recording (if missed)

### 10.3 UX Flows

#### Teacher: Schedule and Start Live Class
1. Teacher opens Live Classes → taps "Schedule"
2. Enters title, selects class, subject, date, time, duration
3. Saves → class scheduled, students notified (10 min before)
4. At class time → teacher taps "Start Class"
5. Meeting room created → video SDK UI opens
6. Teacher conducts class (video, audio, screen share, whiteboard, chat)
7. Teacher taps "End Class" → recording saved, attendance finalized

#### Parent: Join Live Class
1. Parent sees upcoming live class notification (10 min before)
2. Opens app → Live Classes section
3. Taps "Join" → video SDK UI opens (in-app or browser)
4. Student participates (audio, optional video, chat, hand raise)
5. Student leaves → attendance recorded

#### Parent: View Recording
1. Parent sees past live class (child was absent)
2. Taps "View Recording"
3. Recording plays in `ClassRecordingScreen`
4. Recording available for 30 days

### 10.4 State Management

```kotlin
data class LiveClassScheduleState(
    val title: String,
    val selectedClass: ClassDto?,
    val subjectName: String,
    val scheduledAt: Instant?,
    val durationMinutes: Int,
    val isScheduling: Boolean,
    val error: String?,
)

data class LiveClassState(
    val liveClass: LiveClassDto?,
    val joinToken: String?,
    val isInClass: Boolean,
    val participants: List<ParticipantDto>,
    val chatMessages: List<LiveClassChatDto>,
    val error: String?,
)

data class JoinLiveClassState(
    val liveClass: LiveClassDto?,
    val joinToken: String?,
    val isJoining: Boolean,
    val error: String?,
)
```

### 10.5 Offline Support

- Live class: NOT available offline (requires real-time video/audio)
- Class list: cached locally (1-minute TTL)
- Recording: requires online (streamed from Supabase Storage)

### 10.6 Loading States

- Scheduling: "Scheduling live class..."
- Starting: "Creating meeting room..."
- Joining: "Joining class..."
- Ending: "Ending class, saving recording..."
- Loading recording: "Loading recording..."

### 10.7 Error Handling (UI)

- Schedule failed: "Failed to schedule class. Please try again."
- Start failed: "Failed to start class. Please check your connection."
- Join failed: "Failed to join class. The class may have ended."
- Recording unavailable: "Recording not available. It may have expired."
- Class cancelled: "This class has been cancelled."
- Class ended: "This class has ended."
- Max participants: "Class is full. Maximum 50 participants."

### 10.8 Component Integration Guidelines

| Rule | Description |
|---|---|
| **R1** | Schedule form: title, class selector, subject, date/time picker, duration |
| **R2** | Class list: upcoming (blue), live (green badge), ended (gray), cancelled (red strikethrough) |
| **R3** | Start button: prominent, green, only when status = scheduled and time is near |
| **R4** | Video SDK UI: pre-built from Dyte/100ms (video grid, controls, chat) |
| **R5** | Hand raise: floating button, visible indicator when raised |
| **R6** | Attendance view: table with student name, join time, leave time, duration |
| **R7** | Recording player: standard video player with seekbar |
| **R8** | Notification: FCM with deep link to join screen |
| **R9** | Cancel button: confirmation dialog "Cancel this class? Students will be notified." |
| **R10** | Chat: scrollable list, input field, hand raise toggle |

### 10.9 Video SDK Integration

Uses third-party video SDK (Dyte/100ms) for:
- Video grid (participant videos)
- Audio controls (mute/unmute)
- Screen sharing
- Digital whiteboard
- Chat (SDK chat or custom chat)
- Recording (SDK-managed)
- Join/leave events

SDK provides pre-built UI components for Compose Multiplatform (or web view fallback).

---

## 11. Shared Module Changes (KMP)

### 11.1 DTOs

All DTOs defined in section 9.3, placed in `shared/.../liveclass/domain/model/LiveClassModels.kt`.

### 11.2 Domain Models

```kotlin
data class LiveClass(
    val id: String,
    val schoolId: String,
    val teacherId: String,
    val classId: String,
    val subjectName: String?,
    val title: String,
    val description: String?,
    val scheduledAt: Instant,
    val durationMinutes: Int,
    val status: LiveClassStatus,
    val meetingId: String?,
    val recordingUrl: String?,
    val startedAt: Instant?,
    val endedAt: Instant?,
    val createdAt: Instant,
)

data class LiveClassAttendance(
    val id: String,
    val liveClassId: String,
    val studentId: String,
    val studentName: String,
    val joinedAt: Instant?,
    val leftAt: Instant?,
    val durationSeconds: Int?,
)

data class LiveClassChat(
    val id: String,
    val liveClassId: String,
    val senderId: String,
    val senderName: String,
    val senderRole: String,
    val message: String,
    val isHandRaise: Boolean,
    val createdAt: Instant,
)

enum class LiveClassStatus { SCHEDULED, LIVE, ENDED, CANCELLED }
```

### 11.3 Repository Interfaces

```kotlin
interface LiveClassRepository {
    suspend fun schedule(token: String, request: ScheduleRequest): NetworkResult<LiveClassDto>
    suspend fun start(token: String, classId: String): NetworkResult<StartResultDto>
    suspend fun join(token: String, classId: String): NetworkResult<JoinTokenDto>
    suspend fun end(token: String, classId: String): NetworkResult<LiveClassDto>
    suspend fun cancel(token: String, classId: String): NetworkResult<LiveClassDto>
    suspend fun getTeacherClasses(token: String, status: String?): NetworkResult<List<LiveClassDto>>
    suspend fun getParentClasses(token: String, childId: String): NetworkResult<List<LiveClassDto>>
    suspend fun getAttendance(token: String, classId: String): NetworkResult<List<LiveClassAttendanceDto>>
    suspend fun getChat(token: String, classId: String): NetworkResult<List<LiveClassChatDto>>
    suspend fun getRecording(token: String, classId: String): NetworkResult<String>
}
```

### 11.4 UseCases

- `ScheduleLiveClassUseCase`
- `StartLiveClassUseCase`
- `JoinLiveClassUseCase`
- `EndLiveClassUseCase`
- `CancelLiveClassUseCase`
- `GetTeacherLiveClassesUseCase`
- `GetParentLiveClassesUseCase`
- `GetLiveClassAttendanceUseCase`
- `GetLiveClassChatUseCase`
- `GetRecordingUseCase`

### 11.5 Validation

- `title`: non-empty, max 200 characters
- `class_id`: valid UUID
- `scheduled_at`: valid timestamp, not in past
- `duration_minutes`: 15-180 minutes
- `message` (chat): non-empty, max 500 characters

### 11.6 Serialization

Standard Kotlinx serialization. Enums serialized as lowercase strings. Timestamps as ISO strings.

### 11.7 Network APIs

Ktor `@Resource` route definitions in `LiveClassApi.kt`:
- POST `/api/v1/teacher/live-classes`
- POST `/api/v1/teacher/live-classes/{id}/start`
- POST `/api/v1/teacher/live-classes/{id}/end`
- POST `/api/v1/teacher/live-classes/{id}/cancel`
- GET `/api/v1/teacher/live-classes`
- GET `/api/v1/teacher/live-classes/{id}/attendance`
- GET `/api/v1/teacher/live-classes/{id}/chat`
- GET `/api/v1/parent/live-classes/{childId}`
- POST `/api/v1/parent/live-classes/{id}/join`
- GET `/api/v1/parent/live-classes/{id}/recording`

### 11.8 Database Models (Local Cache)

- Upcoming classes: cached locally (1-minute TTL)
- Recording URL: cached locally (1-hour TTL)
- Active live class: not cached (real-time)

---

## 12. Permissions Matrix

| Action | Super Admin | School Admin | Counselor | Teacher | Parent |
|---|---|---|---|---|---|
| Schedule live class | N/A | ✅ (own school) | ❌ | ✅ (own class) | ❌ |
| Start live class | N/A | ✅ (own school) | ❌ | ✅ (own class) | ❌ |
| Join live class | N/A | N/A | ❌ | ❌ | ✅ (own child) |
| End live class | N/A | ✅ (own school) | ❌ | ✅ (own class) | ❌ |
| Cancel live class | N/A | ✅ (own school) | ❌ | ✅ (own class) | ❌ |
| View attendance | ✅ (all) | ✅ (own school) | ❌ | ✅ (own class) | ❌ |
| View chat | ✅ (all) | ✅ (own school) | ❌ | ✅ (own class) | ❌ |
| View recording | ✅ (all) | ✅ (own school) | ❌ | ✅ (own class) | ✅ (own child) |
| Configure settings | ✅ | ✅ | ❌ | ❌ | ❌ |

---

## 13. Notifications

### Live Class Notifications

| Trigger | Recipient | Channel | Template |
|---|---|---|---|
| Class scheduled (10 min before) | Parent (student) | FCM (high) | "Live class '{title}' starts in 10 minutes. Tap to join." |
| Class started | Parent (student) | FCM (high) | "Live class '{title}' has started. Join now!" |
| Class cancelled | Parent (student) | FCM (normal) | "Live class '{title}' has been cancelled." |
| Recording available | Parent (student, if absent) | FCM (normal) | "Recording for '{title}' is now available." |

### Notification Integration

Uses `SmartNotificationService` with "high" priority for pre-class and class-started notifications. Uses `Notify.kt` dispatch.

---

## 14. Background Jobs

### Live Class Notification Job

| Property | Value |
|---|---|
| **Name** | `LiveClassNotificationJob` |
| **Schedule** | Every 1 minute |
| **Duration** | < 5 seconds |
| **Retry** | None (next minute's job handles it) |

#### Job Flow

1. `SELECT * FROM live_classes WHERE status = 'scheduled' AND scheduled_at <= now() + 10 minutes AND scheduled_at > now()`
2. For each class:
   - Get all students in the class
   - Send FCM notification to each student's parent
   - "Live class '{title}' starts in 10 minutes."
   - Mark notification as sent (prevent duplicate)
3. Return count of notifications sent

### Recording Cleanup Job

| Property | Value |
|---|---|
| **Name** | `RecordingCleanupJob` |
| **Schedule** | Daily at 2 AM |
| **Duration** | < 30 seconds |

#### Job Flow

1. `SELECT * FROM live_classes WHERE status = 'ended' AND ended_at < now() - 30 days AND recording_url IS NOT NULL`
2. For each: delete recording from Supabase Storage, set recording_url = null
3. Return count of recordings deleted

---

## 15. Integrations

### Internal Integrations

| System | Integration Point | Direction | Protocol | Error Handling |
|---|---|---|---|---|
| `AI_TIMETABLE_SPEC.md` | Timetable slots | Read | Direct DB | Optional (standalone or from timetable) |
| `NotificationsTable` | Notifications | Call | Direct call | Log error, continue |
| `StudentsTable` | Student info | Read | Direct DB | Required |
| `ClassesTable` | Class info | Read | Direct DB | Required |
| `AppUsersTable` | Teacher info | Read | Direct DB | Required |
| `Notify.kt` | Notification dispatch | Call | Direct call | Log error, continue |
| Supabase Storage | Recording storage | Call | HTTP API | Log error, recording unavailable |

### External Integrations

| System | Integration Point | Direction | Protocol | Error Handling |
|---|---|---|---|---|
| Dyte / 100ms / VideoSDK | Video conferencing | Call | HTTP API + SDK | Fallback to browser join |
| FCM | Push notifications | Call | HTTP API | Log error, continue |

### Integration Patterns

- **Video SDK:** `videoSdkProvider.createMeeting()` → meeting_id + join token → `videoSdkProvider.generateJoinToken()` per user
- **Recording:** `videoSdkProvider.startRecording()` on class start → `videoSdkProvider.stopRecording()` on class end → recording URL saved
- **Notification:** `SmartNotificationService.processNotification()` with priority="high" → FCM to parents
- **Timetable:** Optional integration — class can be created from timetable slot or standalone

---

## 16. Security

### Authentication

- Teacher endpoints: teacher role + JWT auth via `requireAuth()`
- Parent endpoints: parent role + JWT auth + parent-student relationship verified

### Authorization

- Schedule/start/end/cancel: teacher (own class) or school admin
- Join: parent (own child, student in class)
- View attendance/chat: teacher (own class) or school admin
- View recording: parent (own child), teacher (own class), or school admin

### Data Protection

- Live class data (chat, attendance) is school-scoped
- Recording stored in Supabase Storage with school-specific bucket
- Recording access controlled (only class participants and admin)
- Video SDK tokens are per-user, time-limited

### Input Validation

- `title`: non-empty, max 200 characters
- `class_id`: valid UUID
- `scheduled_at`: valid timestamp, not in past
- `duration_minutes`: 15-180 minutes
- `message` (chat): non-empty, max 500 characters
- `description`: max 1000 characters

### Rate Limiting

- Schedule: 10 per day per teacher
- Start: 1 per minute per teacher
- Join: 5 per minute per parent
- Chat: 30 per minute per user

### Audit Logging

- Class scheduled: teacher ID, class ID, timestamp, scheduled_at
- Class started: teacher ID, class ID, started_at, meeting_id
- Class ended: teacher ID, class ID, ended_at, recording_url
- Class cancelled: teacher ID, class ID, timestamp, reason
- Student joined: student ID, class ID, joined_at
- Student left: student ID, class ID, left_at, duration
- Chat message: sender ID, class ID, timestamp, message (truncated)

### PII Handling

- Student names in attendance and chat (necessary for identification)
- Teacher name in class info (necessary for identification)
- Recording may contain student video/audio (retained 30 days, then deleted)
- Chat messages retained with class (deleted on class deletion via CASCADE)

### Multi-tenant Isolation

- All live classes have `school_id` — school-scoped
- Teacher and parent queries filtered by `school_id`
- Recording in school-specific Supabase Storage bucket
- No cross-school live class access

---

## 17. Performance & Scalability

### Expected Scale

- Live classes: 5-20 per day per school (one per subject/class)
- Participants: 30-50 per class
- Duration: 45-60 minutes per class
- Chat messages: ~100-500 per class
- Recordings: 5-20 per day, ~100-500MB each

### Query Optimization

- Teacher classes: `idx_live_classes_school(school_id, scheduled_at DESC)` — filtered by school and sorted by date
- Parent classes: filtered by class_id (student's class) and scheduled_at
- Attendance: filtered by live_class_id (UNIQUE index)
- Chat: filtered by live_class_id, ordered by created_at

### Indexing Strategy

- `live_classes(school_id, scheduled_at DESC)` — dashboard and notification lookup
- `live_class_attendance(live_class_id, student_id)` — UNIQUE index
- `live_class_chat(live_class_id, created_at)` — chat ordering (recommended)

### Caching Strategy

- Upcoming classes: cached locally (1-minute TTL)
- Recording URL: cached locally (1-hour TTL, immutable)
- Active live class: not cached (real-time status)

### Pagination

- Class list: 20 per page
- Chat messages: 50 per page (infinite scroll)
- Attendance: all in single response (max 50)

### Connection Pooling

Uses existing HikariCP connection pool. No additional pooling needed.

### Async Processing

- Notification job: background job (async)
- Recording cleanup: background job (async, daily)
- Video SDK calls: async (HTTP API)
- Chat: real-time via WebSocket or polling

### Scalability Concerns

- Video conferencing: handled by video SDK provider (Dyte/100ms scales to 100+ participants)
- Recording storage: ~500MB × 20 classes/day = ~10GB/day. Supabase Storage handles this.
- Chat: ~500 messages × 20 classes = ~10,000 messages/day. Negligible DB load.
- Notification job: 1-minute interval, few classes. Negligible load.
- DB storage: ~20 classes/day × ~5KB = ~100KB/day. Negligible.

---

## 18. Edge Cases

| # | Scenario | Expected Behavior |
|---|---|---|
| EC-1 | Teacher starts class but no students join | Class continues. Attendance shows 0 students. Recording still saved. |
| EC-2 | Student joins after class started | joined_at = actual join time. Duration = ended_at - joined_at. |
| EC-3 | Student leaves and rejoins | Update existing attendance record. Extend duration. |
| EC-4 | Student still in class when teacher ends | left_at = class ended_at. Duration = ended_at - joined_at. |
| EC-5 | Video SDK fails to create meeting | Return error. Teacher can retry. |
| EC-6 | Recording fails | Class continues without recording. recording_url = null. Log error. |
| EC-7 | Teacher cancels class after starting | Status = cancelled. Students notified. No recording saved. |
| EC-8 | Max participants reached (50) | New join attempts rejected. "Class is full." |
| EC-9 | Network error during class | Video SDK handles reconnection. If fails, student leaves. |
| EC-10 | Recording expired (30 days) | recording_url = null. "Recording no longer available." |
| EC-11 | Teacher schedules class for past time | Return 400. "Cannot schedule class in the past." |
| EC-12 | Parent tries to join class for different class student | Return 403. |

---

## 19. Error Handling

### Error Response Format

Standard `ApiResponse` error format.

### Error Codes

| Code | HTTP Status | Description | User Message |
|---|---|---|---|
| `CLASS_NOT_FOUND` | 404 | Live class not found | "Live class not found." |
| `CLASS_NOT_SCHEDULED` | 409 | Class is not in scheduled state | "This class cannot be started." |
| `CLASS_ALREADY_LIVE` | 409 | Class is already live | "This class is already in progress." |
| `CLASS_ALREADY_ENDED` | 409 | Class has already ended | "This class has already ended." |
| `MEETING_CREATION_FAILED` | 500 | Video SDK failed to create meeting | "Failed to create meeting room. Please try again." |
| `RECORDING_FAILED` | 500 | Recording failed | "Recording unavailable. Class continued without recording." |
| `MAX_PARTICIPANTS_REACHED` | 403 | Class is full | "Class is full. Maximum 50 participants." |
| `NOT_AUTHORIZED` | 403 | Role not authorized | "You are not authorized to perform this action." |
| `NOT_TEACHER_OF_CLASS` | 403 | Teacher not assigned to class | "You can only manage your own classes." |
| `NOT_PARENT_OF_STUDENT` | 403 | Parent not linked to student | "You can only join classes for your own children." |
| `RECORDING_EXPIRED` | 410 | Recording no longer available | "Recording has expired." |

### Error Handling Strategy

- **Meeting creation failure:** Return 500. Teacher can retry start.
- **Recording failure:** Class continues. recording_url = null. Log error.
- **Join failure:** Return error. Parent can retry.
- **Class full:** Return 403. Parent informed.
- **Class ended/cancelled:** Return 409. Parent informed.

### Retry Strategy

- Start: teacher retries (meeting creation may succeed on retry)
- Join: parent retries (network issue may resolve)
- Chat: no retry (messages are best-effort)

### Fallback Behavior

- Video SDK unavailable: fallback to browser join (if supported by SDK)
- Recording unavailable: class continues, no recording
- Chat fails: class continues without chat
- Notification fails: class continues, parent may miss notification

---

## 20. Analytics & Reporting

### Analytics Dashboard Data

| Metric | Source | Derivation |
|---|---|---|
| Classes conducted | `live_classes` (status=ended) | Count per school per month |
| Average duration | `live_classes` | avg(ended_at - started_at) |
| Student participation | `live_class_attendance` | Count of students joined per class |
| Average attendance duration | `live_class_attendance` | avg(duration_seconds) |
| Recording views | API logs | Count of recording access |
| Chat activity | `live_class_chat` | Messages per class |
| Cancellation rate | `live_classes` (status=cancelled) | cancelled / total scheduled |

### Export Capabilities

- Live class report (CSV) — class, teacher, date, duration, student count, recording status
- Attendance report (CSV) — student, class, join time, leave time, duration
- Chat export (CSV) — sender, message, timestamp (for audit)

### Report Types

| Report | Format | Frequency | Recipient |
|---|---|---|---|
| Live class usage | JSON (API) | Monthly | Product Team |
| Attendance summary | JSON (API) | Monthly | School Admin |
| Recording storage | JSON (API) | Monthly | DevOps Team |

---

## 21. Testing Strategy

### Unit Tests

- `LiveClassService.schedule()` — validation, insert, status
- `LiveClassService.start()` — meeting creation, status update
- `LiveClassService.join()` — token generation, attendance insert
- `LiveClassService.end()` — recording stop, status update, attendance finalize
- `LiveClassService.cancel()` — status update, notification
- State transitions: scheduled → live → ended, scheduled → cancelled
- Duration calculation: joined_at, left_at, duration_seconds

### Integration Tests

- Full flow: schedule → start → join → chat → end → recording available
- Attendance: join → leave → rejoin → verify duration
- Cancellation: schedule → cancel → verify students notified
- Recording: start → end → verify recording URL saved
- Permission checks: teacher-only schedule/start, parent-only join
- Multi-tenant: verify school-scoped queries

### E2E Tests

- Teacher schedules class → parent notified → parent joins → teacher ends → recording available
- Teacher starts class → student joins → hand raise → chat → student leaves → attendance recorded
- Teacher cancels class → parent notified of cancellation

### Performance Tests

- 50 concurrent participants in a class (video SDK handles)
- 500 chat messages per class (DB insert performance)
- 20 classes per day (notification job performance)
- Recording upload to Supabase Storage (< 30 seconds for 500MB)

### Test Data

- 5 test classes (scheduled, live, ended, cancelled)
- 10 test students with parent accounts
- 1 test teacher
- Mock video SDK provider (returns controlled meeting IDs and tokens)
- Test chat messages

### Test Environment

- Test database with live class tables
- Mock video SDK provider
- Test Supabase Storage bucket
- Test FCM tokens
- Test JWT tokens for all roles

---

## 22. Acceptance Criteria

- [ ] Teacher schedules and starts live class
- [ ] Students join via in-app or browser
- [ ] Video, audio, screen share, whiteboard work
- [ ] Chat and hand raise during class
- [ ] Attendance tracked (join/leave/duration)
- [ ] Class recording stored and viewable
- [ ] Notification sent before class starts
- [ ] Recording available for absent students
- [ ] Class can be cancelled with student notification
- [ ] Max 50 participants enforced
- [ ] Recording auto-deleted after 30 days
- [ ] School-scoped (no cross-school access)

---

## 23. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 2 days | DB migration `migration_087_live_classes.sql`, Exposed tables, register in `DatabaseFactory` |
| 2 | 3 days | Video SDK integration (Dyte/100ms) — `VideoSdkProvider` interface + implementation |
| 3 | 2 days | `LiveClassService` (schedule, start, join, end, cancel, attendance, recording) |
| 4 | 2 days | Attendance tracking + chat (join/leave events, chat messages) |
| 5 | 2 days | Recording storage + retrieval (Supabase Storage integration) |
| 6 | 1 day | Notification integration (`LiveClassNotificationJob` + FCM) |
| 7 | 5 days | Client UI: `LiveClassScheduleScreen`, `LiveClassScreen`, `JoinLiveClassScreen`, `ClassRecordingScreen`, `LiveClassAttendanceScreen` |
| 8 | 2 days | Tests: unit, integration, E2E |

### Pre-Implementation Checklist

- [ ] Verify video SDK provider account (Dyte/100ms) is set up
- [ ] Verify Supabase Storage bucket for recordings is configured
- [ ] Verify FCM notification infrastructure is available
- [ ] Verify `AI_TIMETABLE_SPEC.md` timetable tables exist (optional integration)
- [ ] Verify `StudentsTable`, `ClassesTable`, `AppUsersTable` are available
- [ ] Verify camera/microphone permissions in app manifest

---

## 24. File-Level Impact Analysis

### Server

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | `LiveClassesTable`, `LiveClassAttendanceTable`, `LiveClassChatTable` |
| `server/.../db/DatabaseFactory.kt` | Modify | Register new tables in `allTables` |
| `server/.../feature/liveclass/LiveClassService.kt` | **New** | Core service |
| `server/.../feature/liveclass/LiveClassRouting.kt` | **New** | API endpoints |
| `server/.../feature/liveclass/VideoSdkProvider.kt` | **New** | Video SDK interface + Dyte/100ms implementation |
| `server/.../feature/liveclass/LiveClassNotificationJob.kt` | **New** | Pre-class notification job |
| `server/.../feature/liveclass/RecordingCleanupJob.kt` | **New** | Recording retention cleanup job |
| `docs/db/migration_087_live_classes.sql` | **New** | DDL |

### Shared (KMP)

| File | Change Type | Description |
|---|---|---|
| `shared/.../liveclass/domain/model/LiveClassModels.kt` | **New** | DTOs, domain models, enums |
| `shared/.../liveclass/domain/repository/LiveClassRepository.kt` | **New** | Repository interface |
| `shared/.../liveclass/data/remote/LiveClassApi.kt` | **New** | HTTP API definitions |

### Client (Compose)

| File | Change Type | Description |
|---|---|---|
| `composeApp/.../ui/v2/screens/teacher/LiveClassScheduleScreen.kt` | **New** | Schedule UI |
| `composeApp/.../ui/v2/screens/teacher/LiveClassScreen.kt` | **New** | Teacher live class (video SDK UI) |
| `composeApp/.../ui/v2/screens/teacher/LiveClassListScreen.kt` | **New** | Class list |
| `composeApp/.../ui/v2/screens/teacher/LiveClassAttendanceScreen.kt` | **New** | Attendance view |
| `composeApp/.../ui/v2/screens/parent/JoinLiveClassScreen.kt` | **New** | Student join |
| `composeApp/.../ui/v2/screens/parent/ClassRecordingScreen.kt` | **New** | Recording viewer |

---

## 25. Future Enhancements

| # | Enhancement | Priority | Effort | Notes |
|---|---|---|---|---|
| F-1 | Breakout rooms | Medium | L | Split students into small groups |
| F-2 | Polls and quizzes | Medium | M | Interactive polls during class |
| F-3 | Co-teaching | Medium | M | Multiple teachers in one class |
| F-4 | Live transcription | Low | M | Real-time captions |
| F-5 | Whiteboard export as PDF | Low | S | Export whiteboard content |
| F-6 | Attendance certificate | Low | S | Generate certificate for attendees |
| F-7 | WebRTC self-hosted | Medium | XL | Migrate from SDK to self-hosted SFU |
| F-8 | Virtual background | Low | S | Background blur/replacement |
| F-9 | Class analytics dashboard | Medium | M | Detailed engagement analytics |
| F-10 | Recording chapters | Low | S | Auto-generate chapters from chat/whiteboard events |

---

## Appendix A: Sequence Diagrams

### A.1 Schedule and Start Live Class

```
Teacher (app)       Server              DB              Video SDK
  │                    │                  │                │
  │  POST /live-classes│                  │                │
  │  {title, class_id, │                  │                │
  │   scheduled_at}    │                  │                │
  │  ───────────────>  │                  │                │
  │                    │──insert class──>│                │
  │                    │←──success───────│                │
  │  ←──201: LiveClassDto│                │                │
  │                    │                  │                │
  │  (10 min before)   │                  │                │
  │  Notification job  │                  │                │
  │  sends FCM to      │                  │                │
  │  parents           │                  │                │
  │                    │                  │                │
  │  POST /{id}/start  │                  │                │
  │  ───────────────>  │                  │                │
  │                    │──create meeting─────────────────>│
  │                    │←──meeting_id + token────────────│
  │                    │──update status──>│                │
  │                    │  status=live     │                │
  │                    │←──success───────│                │
  │  ←──200: StartResult│                 │                │
  │  (meeting_id,       │                  │                │
  │   join_token)       │                  │                │
  │                    │                  │                │
  │  Video SDK UI opens│                  │                │
  │  Class conducted   │                  │                │
  │                    │                  │                │
```

### A.2 Student Join and Attendance

```
Parent (app)        Server              DB              Video SDK
  │                    │                  │                │
  │  POST /{id}/join   │                  │                │
  │  ───────────────>  │                  │                │
  │                    │──verify parent──>│                │
  │                    │  -child relation │                │
  │                    │←──ok─────────────│                │
  │                    │                  │                │
  │                    │──generate token─────────────────>│
  │                    │←──join_token────────────────────│
  │                    │                  │                │
  │                    │──insert attendance>│              │
  │                    │  joined_at=now() │                │
  │                    │←──success───────│                │
  │  ←──200: JoinToken │                  │                │
  │                    │                  │                │
  │  Student joins via │                  │                │
  │  video SDK         │                  │                │
  │                    │                  │                │
  │  (student leaves)  │                  │                │
  │  WebSocket/poll    │                  │                │
  │  ───────────────>  │                  │                │
  │                    │──update attendance>│              │
  │                    │  left_at=now()   │                │
  │                    │  duration=calc   │                │
  │                    │←──success───────│                │
  │                    │                  │                │
```

### A.3 End Class and Recording

```
Teacher (app)       Server              DB              Video SDK    Supabase
  │                    │                  │                │           │
  │  POST /{id}/end    │                  │                │           │
  │  ───────────────>  │                  │                │           │
  │                    │──stop recording─────────────────>│           │
  │                    │←──recording_url────────────────│             │
  │                    │                  │                │           │
  │                    │──upload recording───────────────────────────>│
  │                    │←──storage_url────────────────────────────────│
  │                    │                  │                │           │
  │                    │──update class───>│                │           │
  │                    │  status=ended    │                │           │
  │                    │  ended_at=now()  │                │           │
  │                    │  recording_url   │                │           │
  │                    │←──success───────│                │           │
  │                    │                  │                │           │
  │                    │──finalize att──>│                │           │
  │                    │  (set left_at   │                │           │
  │                    │   for still-in) │                │           │
  │                    │←──success───────│                │           │
  │  ←──200: LiveClassDto│                │                │           │
  │                    │                  │                │           │
```

---

## Appendix B: Domain Model / ER Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                    live_classes (new)                                 │
│  id (PK)                                                              │
│  school_id, teacher_id, class_id                                      │
│  subject_name, title, description                                     │
│  scheduled_at, duration_minutes                                       │
│  status (scheduled|live|ended|cancelled)                              │
│  meeting_id, recording_url                                            │
│  started_at, ended_at, created_at                                     │
│  INDEX: (school_id, scheduled_at DESC)                                │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│                  live_class_attendance (new)                          │
│  id (PK)                                                              │
│  live_class_id (FK → live_classes, CASCADE)                           │
│  student_id, student_name                                             │
│  joined_at, left_at, duration_seconds                                 │
│  UNIQUE: (live_class_id, student_id)                                  │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│                    live_class_chat (new)                              │
│  id (PK)                                                              │
│  live_class_id (FK → live_classes, CASCADE)                           │
│  sender_id, sender_name, sender_role                                  │
│  message, is_hand_raise                                               │
│  created_at                                                           │
└──────────────────────────────────────────────────────────────────────┘

Existing tables used:
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│ students         │  │ classes          │  │ app_users        │
│ (existing)       │  │ (existing)       │  │ (existing)       │
│ student info     │  │ class info       │  │ teacher accounts │
└──────────────────┘  └──────────────────┘  └──────────────────┘
```

---

## Appendix C: Event Flow

### Domain Events

| Event | Emitter | Consumers | Payload | Side Effects |
|---|---|---|---|---|
| `LiveClassScheduled` | `LiveClassService.schedule()` | None (logged) | `classId, teacherId, scheduledAt` | Audit log |
| `LiveClassStarted` | `LiveClassService.start()` | `NotificationService` | `classId, meetingId, startedAt` | FCM to parents |
| `LiveClassEnded` | `LiveClassService.end()` | None (logged) | `classId, endedAt, recordingUrl` | Audit log |
| `LiveClassCancelled` | `LiveClassService.cancel()` | `NotificationService` | `classId, reason` | FCM to parents |
| `StudentJoinedLiveClass` | `LiveClassService.join()` | None (logged) | `classId, studentId, joinedAt` | Audit log |
| `StudentLeftLiveClass` | Attendance tracking | None (logged) | `classId, studentId, leftAt, duration` | Audit log |
| `RecordingAvailable` | `LiveClassService.end()` | `NotificationService` | `classId, recordingUrl` | FCM to absent students |

### Event Delivery Guarantees

- Scheduled event: fire-and-forget logging
- Started event: synchronous, notification dispatch async
- Ended event: fire-and-forget logging
- Cancelled event: synchronous, notification dispatch async
- Join/leave events: fire-and-forget logging
- Recording available: fire-and-forget, notification dispatch async

---

## Appendix D: Configuration

### Environment Variables

| Variable | Default | Description |
|---|---|---|
| `LIVE_CLASSES_ENABLED` | `false` | Enable/disable feature |
| `VIDEO_SDK_PROVIDER` | `dyte` | Video SDK: dyte, 100ms, videosdk, webrtc |
| `VIDEO_SDK_API_KEY` | — | API key for video SDK |
| `VIDEO_SDK_API_SECRET` | — | API secret for video SDK |
| `RECORDING_RETENTION_DAYS` | `30` | Recording retention period |
| `PRE_CLASS_NOTIFICATION_MINUTES` | `10` | Notification lead time |
| `MAX_PARTICIPANTS` | `50` | Max participants per class |
| `DEFAULT_DURATION_MINUTES` | `45` | Default class duration |

### Feature Flags

| Flag | Default | Description |
|---|---|---|
| `LIVE_CLASSES_ENABLED` | `false` | Enable/disable live classes |
| `RECORDING_ENABLED` | `true` | Enable/disable recording |
| `CHAT_ENABLED` | `true` | Enable/disable chat |
| `HAND_RAISE_ENABLED` | `true` | Enable/disable hand raise |

### School-Level Settings

| Setting | Default | Description |
|---|---|---|
| `live_classes_enabled` | `false` | Per-school enable/disable |
| `recording_enabled` | `true` | Per-school recording |
| `recording_retention_days` | `30` | Per-school retention |
| `max_participants` | `50` | Per-school max |
| `pre_class_notification_minutes` | `10` | Per-school notification lead time |

---

## Appendix E: Migration & Rollback

### Migration: `migration_087_live_classes.sql`

```sql
-- Migration 087: Live Classes (Virtual Classroom)
-- Creates live_classes, live_class_attendance, live_class_chat tables

BEGIN;

CREATE TABLE IF NOT EXISTS live_classes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    teacher_id      UUID NOT NULL,
    class_id        UUID NOT NULL,
    subject_name    TEXT,
    title           TEXT NOT NULL,
    description     TEXT,
    scheduled_at    TIMESTAMP NOT NULL,
    duration_minutes INTEGER NOT NULL DEFAULT 45,
    status          VARCHAR(16) NOT NULL DEFAULT 'scheduled',
    meeting_id      TEXT,
    recording_url   TEXT,
    started_at      TIMESTAMP,
    ended_at        TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_live_classes_school
    ON live_classes (school_id, scheduled_at DESC);

CREATE TABLE IF NOT EXISTS live_class_attendance (
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

CREATE TABLE IF NOT EXISTS live_class_chat (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    live_class_id   UUID NOT NULL REFERENCES live_classes(id) ON DELETE CASCADE,
    sender_id       UUID NOT NULL,
    sender_name     TEXT NOT NULL,
    sender_role     VARCHAR(32) NOT NULL,
    message         TEXT NOT NULL,
    is_hand_raise   BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

COMMIT;
```

### Rollback: `migration_087_rollback.sql`

```sql
BEGIN;
DROP TABLE IF EXISTS live_class_chat;
DROP TABLE IF EXISTS live_class_attendance;
DROP TABLE IF EXISTS live_classes;
COMMIT;
```

### Migration Validation

- Verify `live_classes` table created with index
- Verify `live_class_attendance` table created with UNIQUE constraint and CASCADE
- Verify `live_class_chat` table created with CASCADE
- Run `SELECT count(*) FROM live_classes` — should be 0 (new feature)
- Run `SELECT count(*) FROM live_class_attendance` — should be 0
- Run `SELECT count(*) FROM live_class_chat` — should be 0

---

## Appendix F: Observability

### Structured Logging

| Log Level | Event | Context Fields |
|---|---|---|
| INFO | Class scheduled | `classId, teacherId, scheduledAt, title` |
| INFO | Class started | `classId, meetingId, startedAt` |
| INFO | Class ended | `classId, endedAt, recordingUrl, participantCount` |
| INFO | Class cancelled | `classId, teacherId, reason` |
| INFO | Student joined | `classId, studentId, joinedAt` |
| INFO | Student left | `classId, studentId, leftAt, durationSeconds` |
| WARN | Meeting creation failed | `classId, error` |
| WARN | Recording failed | `classId, error` |
| WARN | Max participants reached | `classId, participantCount` |
| WARN | Notification job: no classes found | — |
| ERROR | Video SDK error | `classId, meetingId, error` |
| ERROR | Supabase Storage upload failed | `classId, recordingUrl, error` |

### Metrics

| Metric | Type | Labels | Description |
|---|---|---|---|
| `live_classes_scheduled_total` | Counter | `school_id` | Total classes scheduled |
| `live_classes_started_total` | Counter | `school_id` | Total classes started |
| `live_classes_ended_total` | Counter | `school_id` | Total classes ended |
| `live_classes_cancelled_total` | Counter | `school_id` | Total classes cancelled |
| `live_class_participants` | Histogram | `school_id` | Participants per class |
| `live_class_duration_minutes` | Histogram | `school_id` | Class duration |
| `live_class_attendance_duration` | Histogram | `school_id` | Student attendance duration |
| `live_class_chat_messages` | Counter | `school_id` | Total chat messages |
| `live_class_recordings` | Gauge | `school_id` | Active recordings in storage |
| `live_class_recording_size_mb` | Histogram | `school_id` | Recording file size |
| `live_class_notification_sent` | Counter | `school_id` | Pre-class notifications sent |

### Health Checks

| Check | Endpoint | Description |
|---|---|---|
| Live class service | `/health/live-classes` | Verify service and DB accessible |
| Video SDK connectivity | `/health/video-sdk` | Verify video SDK provider reachable |

### Alerts

| Alert | Condition | Severity | Notification |
|---|---|---|---|
| Video SDK unavailable | Health check failed | Critical | Email + SMS to dev team |
| Recording storage full | Supabase Storage > 80% | Warning | Email to DevOps |
| Notification job failed | Job error | Warning | Email to dev team |
| High cancellation rate | > 20% cancellations in 1 week | Warning | Email to admin |
| Low participation rate | < 30% students joining | Warning | Email to admin |

### Dashboards

| Dashboard | Panels | Audience |
|---|---|---|
| Live Class Overview | Classes scheduled, started, ended, cancelled | Product Team |
| Participation | Participants per class, attendance duration, join rate | Product Team |
| Recording Storage | Active recordings, storage used, retention status | DevOps Team |
| Notification Performance | Notifications sent, delivery rate | Product Team |

### Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Video SDK outage | Low | Critical | Fallback to browser join. Health check. |
| Recording storage full | Medium | Medium | Monitoring. Auto-cleanup after retention. |
| High SDK cost | Medium | Medium | Monitor usage. Migrate to WebRTC if costs grow. |
| Poor video quality | Medium | Medium | Video SDK handles adaptive bitrate. |
| Student can't join | Medium | Low | Retry. Fallback to browser. |
| Notification not sent | Low | Medium | Job runs every minute. Retry in next run. |
| Chat spam | Low | Low | Rate limiting (30/min). Teacher can mute. |
