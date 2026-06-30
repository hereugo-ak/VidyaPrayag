# Voice & Video Calls — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** `MESSAGING_SYSTEM_SPEC.md`, `LIVE_CLASSES_SPEC.md`
> **Source:** `COMPETITIVE_GAP_ANALYSIS.md` — emerging trend
> **Template:** `_SPEC_TEMPLATE.md` v1 (25 mandatory + 6 optional sections)

---

## 1. Feature Overview

### What

In-app voice and video calling between parent and teacher (1-on-1), eliminating the need to share personal phone numbers. Uses WebRTC for peer-to-peer communication with Ktor signaling server.

### Why — Product Rationale

Parents and teachers currently communicate via text messaging, but some conversations are better had voice-to-voice or face-to-face. Sharing personal phone numbers raises privacy concerns. In-app calling provides a safe, controlled communication channel within the school ecosystem. This is a **medium-priority feature** (Phase 4, effort L) that enhances parent-teacher communication.

### What Stands Out (Competitive Moat)

From `COMPETITIVE_GAP_ANALYSIS.md`: voice/video calls as emerging trend.

The key moat is **privacy-first calling** — no personal phone numbers exchanged, all calls logged and tracked within the school system. Combined with **voicemail**, **DND hours**, and **busy indicators**, the system provides professional communication boundaries that phone numbers can't offer. Reuses WebRTC infrastructure from `LIVE_CLASSES_SPEC.md`.

### Goals

- Parent can initiate voice/video call to teacher from messaging screen
- Teacher can initiate voice/video call to parent
- No personal phone numbers exchanged — uses app identity
- Call ringing, acceptance, rejection, and missed call tracking
- Call history with duration
- Call quality adaptation (audio-only fallback on poor network)
- Voicemail: if teacher unavailable, parent can leave a voice message

### Non-goals

- [ ] Group calls / conference calls (1-on-1 only; use live classes for group)
- [ ] Call recording (privacy concern, not stored)
- [ ] Call transcription (future enhancement)
- [ ] Screen sharing during calls (future enhancement)
- [ ] Call scheduling (call initiated in real-time from message thread)
- [ ] Student-to-student calls (parent-teacher only)
- [ ] International call routing (WebRTC P2P, no phone network)
- [ ] Call analytics/quality metrics dashboard (future enhancement)

### Dependencies

- `MESSAGING_SYSTEM_SPEC.md` — thread-based messaging (call initiation context)
- `LIVE_CLASSES_SPEC.md` — WebRTC infrastructure (reusable signaling)
- `MessageThreadsTable` — existing thread context for call initiation
- `NotificationPreferencesTable` — DND hours, calls_enabled
- `AppUsersTable` — user accounts (parent, teacher)
- `StudentsTable` — student info (for parent-teacher relationship)
- Supabase Storage — voicemail audio storage
- FCM — call notifications
- Google WebRTC SDK — WebRTC client (Android, iOS, Web)

### Related Modules

- `server/.../feature/calls/` — new calls module
- `server/.../feature/messaging/` — existing messaging module (thread context)
- `server/.../feature/liveclasses/` — existing live classes module (WebRTC signaling reuse)
- `shared/.../core/webrtc/` — WebRTC client (expect/actual)
- `composeApp/.../ui/v2/screens/calls/` — call UI screens

---

## 2. Current System Assessment

### Existing Code

- `MESSAGING_SYSTEM_SPEC.md` — thread-based messaging between parent and teacher
- `LIVE_CLASSES_SPEC.md` — WebRTC infrastructure for video (reusable signaling)
- `MessageThreadsTable` — existing thread context for call initiation
- No voice/video calling exists
- `COMPETITIVE_GAP_ANALYSIS.md`: voice/video calls as emerging trend

### Existing Database

- `MessageThreadsTable` — message threads (parent-teacher context)
- `NotificationPreferencesTable` — notification preferences (DND, channels)
- `AppUsersTable` — user accounts (parent, teacher)
- `StudentsTable` — student info
- No call logs table
- No DND calls columns in notification preferences

### Existing APIs

- Messaging API (existing) — thread-based messaging
- Live Classes API (existing) — WebRTC signaling (reusable)
- Notification API (existing) — FCM notifications
- No call API

### Existing UI

- Messaging screen (existing) — message threads
- Live classes screen (existing) — WebRTC video (reusable patterns)
- No call UI (incoming call, active call, call history)

### Existing Services

- `MessagingService` — thread-based messaging
- `LiveClassesSignalingService` — WebRTC signaling (reusable patterns)
- `NotificationService` — FCM notifications
- `SmartNotificationService` — notification priority
- No call service

### Existing Documentation

- `COMPETITIVE_GAP_ANALYSIS.md` — voice/video calls as emerging trend
- `MESSAGING_SYSTEM_SPEC.md` — messaging system
- `LIVE_CLASSES_SPEC.md` — live classes (WebRTC)

### Technical Debt

| # | Gap | Details |
|---|---|---|
| TD-1 | No call logs table | No `call_logs` table for tracking calls |
| TD-2 | No call signaling | No WebSocket-based 1-on-1 call signaling (live classes has SFU-based) |
| TD-3 | No WebRTC client | No `WebRtcClient` (expect/actual) for 1-on-1 calls |
| TD-4 | No call UI | No incoming call, active call, call history screens |
| TD-5 | No DND for calls | No DND calls hours in notification preferences |
| TD-6 | No voicemail | No voicemail recording and storage |

### Gaps

| # | Gap | Impact | Severity |
|---|---|---|---|
| G1 | No in-app calling | Parents/teachers exchange phone numbers (privacy risk) | **High** |
| G2 | No voicemail | Missed calls have no message context | **Medium** |
| G3 | No DND for calls | Teachers disturbed outside hours | **Medium** |
| G4 | No call history | No record of communication attempts | **Medium** |
| G5 | No busy indicator | Callers don't know if callee is on another call | **Low** |

---

## 3. Functional Requirements

### FR-001
| Field | Value |
|---|---|
| **Title** | Initiate Call |
| **Description** | Parent initiates voice/video call from message thread with teacher. |
| **Priority** | High |
| **User Roles** | Parent, Teacher |
| **Acceptance notes** | Parent/teacher taps "Voice Call" or "Video Call" in message thread. App creates offer SDP, sends via WebSocket signaling. Server creates call_log (status=ringing), sends FCM to callee. |

### FR-002
| Field | Value |
|---|---|
| **Title** | Incoming Call Notification |
| **Description** | Teacher receives incoming call notification (FCM + in-app ringing). |
| **Priority** | High |
| **User Roles** | Teacher, Parent |
| **Acceptance notes** | Callee receives FCM notification: "Incoming video call from {caller_name}". App shows incoming call screen with accept/reject buttons. In-app ringing sound. |

### FR-003
| Field | Value |
|---|---|
| **Title** | Accept/Reject Call |
| **Description** | Teacher accepts or rejects call. |
| **Priority** | High |
| **User Roles** | Teacher, Parent |
| **Acceptance notes** | Callee taps "Accept" → creates answer SDP → sent via signaling → WebRTC connection established. Callee taps "Reject" → call_log status=rejected → caller notified. |

### FR-004
| Field | Value |
|---|---|
| **Title** | WebRTC Peer-to-Peer |
| **Description** | Call established: WebRTC peer-to-peer audio/video. |
| **Priority** | High |
| **User Roles** | System |
| **Acceptance notes** | Direct P2P connection (no SFU needed for 1-on-1). Audio and video flow directly between peers. STUN/TURN servers used for NAT traversal. |

### FR-005
| Field | Value |
|---|---|
| **Title** | Call Controls |
| **Description** | Call controls: mute, camera toggle, speaker, end call. |
| **Priority** | High |
| **User Roles** | Parent, Teacher |
| **Acceptance notes** | During call: mute/unmute microphone, camera on/off (video calls), speaker on/off, end call. All controls update WebRTC tracks in real-time. |

### FR-006
| Field | Value |
|---|---|
| **Title** | Audio-Only Fallback |
| **Description** | Audio-only fallback: if video fails or network poor, downgrade to audio. |
| **Priority** | Medium |
| **User Roles** | System |
| **Acceptance notes** | If video track fails or network quality drops below threshold, automatically disable video and continue audio-only. UI updates to show audio-only mode. Caller notified. |

### FR-007
| Field | Value |
|---|---|
| **Title** | Missed Call |
| **Description** | Missed call: if teacher doesn't answer in 30 seconds → missed call notification + logged. |
| **Priority** | High |
| **User Roles** | System |
| **Acceptance notes** | 30-second timeout from ringing. If no answer, call_log status=missed. FCM sent to callee: "Missed call from {caller_name}". Caller sees "No answer" with voicemail option. |

### FR-008
| Field | Value |
|---|---|
| **Title** | Voicemail |
| **Description** | Voicemail: parent can record voice message if teacher unavailable. |
| **Priority** | Medium |
| **User Roles** | Parent, Teacher |
| **Acceptance notes** | After missed call, caller can record voicemail (max 60 seconds). Audio uploaded to Supabase Storage. call_log updated with voicemail_url and voicemail_duration. Callee notified: "Voicemail from {caller_name}". |

### FR-009
| Field | Value |
|---|---|
| **Title** | Call History |
| **Description** | Call history: list of calls (type, duration, status) per thread. |
| **Priority** | Medium |
| **User Roles** | Parent, Teacher |
| **Acceptance notes** | Call history per message thread. Shows: call type (voice/video), status (answered/rejected/missed/failed), duration, timestamp, voicemail indicator. Sorted by most recent. |

### FR-010
| Field | Value |
|---|---|
| **Title** | Busy Indicator |
| **Description** | Busy indicator: if teacher on another call, parent sees "busy". |
| **Priority** | Low |
| **User Roles** | Parent, Teacher |
| **Acceptance notes** | When call initiated, server checks if callee has active call. If busy, caller sees "Callee is on another call" and call_log status=failed (busy). No ringing sent. |

### FR-011
| Field | Value |
|---|---|
| **Title** | Do Not Disturb |
| **Description** | Do Not Disturb: teacher can set DND hours (no call notifications). |
| **Priority** | Medium |
| **User Roles** | Teacher, Parent |
| **Acceptance notes** | Teacher sets DND hours (e.g., 20:00-07:00). During DND, incoming calls go straight to missed call (no FCM ringing). Caller sees "Callee has Do Not Disturb enabled" with voicemail option. |

### Non-Functional Requirements

| ID | Requirement |
|---|---|
| NFR-1 | Call setup: < 3 seconds (offer → answer → connected) |
| NFR-2 | Audio latency: < 200ms (P2P) |
| NFR-3 | Video latency: < 500ms (P2P) |
| NFR-4 | Missed call timeout: 30 seconds |
| NFR-5 | Voicemail: max 60 seconds, < 5MB |
| NFR-6 | Call history: < 2 seconds to load |
| NFR-7 | Concurrent calls per school: ~10-20 (1-on-1, P2P) |
| NFR-8 | Audio-only fallback: < 2 seconds to detect and switch |
| NFR-9 | DND check: < 100ms |
| NFR-10 | Busy check: < 100ms |

---

## 4. User Stories

### Parent
- [ ] Initiate a voice call to my child's teacher
- [ ] Initiate a video call to my child's teacher
- [ ] See call history with my child's teacher
- [ ] Leave a voicemail when teacher is unavailable
- [ ] See missed call notification
- [ ] Use call controls (mute, camera, end call)

### Teacher
- [ ] Receive incoming call notification
- [ ] Accept or reject incoming call
- [ ] Initiate a call to a parent
- [ ] Set DND hours for calls
- [ ] View call history with parents
- [ ] Listen to voicemails from parents
- [ ] Use call controls (mute, camera, end call)

---

## 5. Business Rules

### BR-001
**Rule:** Calls are 1-on-1 only (parent-teacher). No group calls.
**Enforcement:** Call initiation requires a message thread (1-on-1 context). No multi-party call support. Group video uses live classes.

### BR-002
**Rule:** No personal phone numbers exchanged.
**Enforcement:** Calls use app identity (user ID). WebRTC P2P connection. No phone network involved. Caller/callee identified by app name and role.

### BR-003
**Rule:** All calls are logged.
**Enforcement:** Every call creates a `call_logs` record with status, duration, participants. Call history available per message thread. No unlogged calls.

### BR-004
**Rule:** Missed call after 30 seconds.
**Enforcement:** 30-second timeout from ringing status. If no answer, status=missed. FCM to callee. Caller offered voicemail option.

### BR-005
**Rule:** DND hours respected.
**Enforcement:** If callee has DND enabled and current time is within DND window, call goes straight to missed (no FCM ringing). Caller informed. Voicemail available.

### BR-006
**Rule:** Voicemail max 60 seconds.
**Enforcement:** Client-side timer limits recording to 60 seconds. Audio uploaded to Supabase Storage. call_log updated with voicemail_url and duration.

### BR-007
**Rule:** Busy indicator if callee on another call.
**Enforcement:** Server tracks active calls. If callee has active call (status=answered), new call gets status=failed (busy). Caller informed.

### BR-008
**Rule:** Calls are school-scoped.
**Enforcement:** All call_logs have school_id. Parent-teacher relationship verified via message thread (which is school-scoped). No cross-school calls.

### BR-009
**Rule:** Call recording is not supported.
**Enforcement:** No call recording feature. WebRTC streams are real-time only, not stored. Privacy protection.

### BR-010
**Rule:** Audio-only fallback on poor network.
**Enforcement:** WebRTC client monitors connection quality. If video track consistently fails or bitrate drops below threshold, video disabled, audio continues. UI updates to audio-only mode.

---

## 6. Database Design

### 6.1 Entity Relationship Summary

One new table: `call_logs` (call records with status, duration, voicemail). Modified `notification_preferences` table (add DND calls columns). References existing `message_threads`, `app_users` tables.

### 6.2 New Tables

#### `call_logs` table

```sql
CREATE TABLE call_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    thread_id       UUID NOT NULL REFERENCES message_threads(id),
    caller_id       UUID NOT NULL,
    caller_name     TEXT NOT NULL,
    caller_role     VARCHAR(32) NOT NULL,
    callee_id       UUID NOT NULL,
    callee_name     TEXT NOT NULL,
    callee_role     VARCHAR(32) NOT NULL,
    call_type       VARCHAR(16) NOT NULL,          -- voice | video
    status          VARCHAR(16) NOT NULL,          -- ringing | answered | rejected | missed | failed
    started_at      TIMESTAMP,
    ended_at        TIMESTAMP,
    duration_seconds INTEGER,
    voicemail_url   TEXT,                          -- Supabase Storage URL (if voicemail left)
    voicemail_duration_seconds INTEGER,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_call_logs_thread ON call_logs(thread_id, created_at DESC);
CREATE INDEX idx_call_logs_callee ON call_logs(callee_id, status, created_at DESC);
```

### 6.3 Modified Tables

#### `notification_preferences` table (modified)

```sql
ALTER TABLE notification_preferences ADD COLUMN dnd_calls_start VARCHAR(8) DEFAULT '20:00';
ALTER TABLE notification_preferences ADD COLUMN dnd_calls_end VARCHAR(8) DEFAULT '07:00';
ALTER TABLE notification_preferences ADD COLUMN calls_enabled BOOLEAN NOT NULL DEFAULT true;
```

### 6.4 Indexes

- `idx_call_logs_thread` on `call_logs(thread_id, created_at DESC)` — call history per thread
- `idx_call_logs_callee` on `call_logs(callee_id, status, created_at DESC)` — calls by callee and status (busy check, missed calls)

### 6.5 Constraints

- `call_logs.thread_id` — NOT NULL, FK to message_threads
- `call_logs.caller_id` — NOT NULL
- `call_logs.callee_id` — NOT NULL
- `call_logs.call_type` — NOT NULL, values: voice | video
- `call_logs.status` — NOT NULL, values: ringing | answered | rejected | missed | failed
- `call_logs.duration_seconds` — nullable (set when call ends)
- `notification_preferences.calls_enabled` — NOT NULL, default true
- `notification_preferences.dnd_calls_start` — nullable, default '20:00'
- `notification_preferences.dnd_calls_end` — nullable, default '07:00'

### 6.6 Foreign Keys

- `call_logs.thread_id` → `message_threads.id` (explicit FK)
- `call_logs.school_id` → `schools.id` (implicit)
- `call_logs.caller_id` → `app_users.id` (implicit)
- `call_logs.callee_id` → `app_users.id` (implicit)

### 6.7 Soft Delete Strategy

N/A — call logs are not deleted. They are historical records of communication. Retained for audit and history.

### 6.8 Audit Fields

- `call_logs.created_at` — when call was initiated
- `call_logs.started_at` — when call was answered (nullable if not answered)
- `call_logs.ended_at` — when call ended (nullable if not answered)
- `call_logs.duration_seconds` — call duration (nullable if not answered)

### 6.9 Migration Notes

Migration: `docs/db/migration_091_voice_video_calls.sql`
- CREATE `call_logs` table with indexes
- ALTER `notification_preferences`: ADD `dnd_calls_start`, `dnd_calls_end`, `calls_enabled` columns
- No data migration (new feature)

### 6.10 Exposed Mappings

```kotlin
object CallLogsTable : UUIDTable("call_logs", "id") {
    val schoolId                  = uuid("school_id")
    val threadId                  = uuid("thread_id").references(MessageThreadsTable.id)
    val callerId                  = uuid("caller_id")
    val callerName                = text("caller_name")
    val callerRole                = varchar("caller_role", 32)
    val calleeId                  = uuid("callee_id")
    val calleeName                = text("callee_name")
    val calleeRole                = varchar("callee_role", 32)
    val callType                  = varchar("call_type", 16)    // voice | video
    val status                    = varchar("status", 16)       // ringing | answered | rejected | missed | failed
    val startedAt                 = timestamp("started_at").nullable()
    val endedAt                   = timestamp("ended_at").nullable()
    val durationSeconds           = integer("duration_seconds").nullable()
    val voicemailUrl              = text("voicemail_url").nullable()
    val voicemailDurationSeconds  = integer("voicemail_duration_seconds").nullable()
    val createdAt                 = timestamp("created_at")

    init {
        index("idx_call_logs_thread", threadId, createdAt)
        index("idx_call_logs_callee", calleeId, status, createdAt)
    }
}

// Modified: NotificationPreferencesTable
object NotificationPreferencesTable : UUIDTable("notification_preferences", "id") {
    // ... existing columns ...
    val dndCallsStart   = varchar("dnd_calls_start", 8).default("20:00")
    val dndCallsEnd     = varchar("dnd_calls_end", 8).default("07:00")
    val callsEnabled    = bool("calls_enabled").default(true)
}
```

Register `CallLogsTable` in `DatabaseFactory.allTables`.

### 6.11 Seed Data

N/A — call logs are user-generated. No seed data needed.

---

## 7. State Machines

### Call Lifecycle State Machine

```
initiated ──callee_rings──> ringing ──callee_accepts──> answered ──either_ends──> ended
  │                           │
  │                           ├──callee_rejects──> rejected (terminal)
  │                           │
  │                           ├──30s_timeout──> missed (terminal)
  │                           │
  │                           └──caller_cancels──> cancelled (terminal)
  │
  └──callee_busy──> failed (terminal)
  │
  └──callee_dnd──> failed (terminal, DND)
  │
  └──missed ──caller_leaves_voicemail──> voicemail_left (terminal)
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `initiated` | Server checks callee status | `ringing` | Callee available (not busy, not DND) |
| `initiated` | Callee is busy | `failed` | Callee has active call (status=answered) |
| `initiated` | Callee has DND enabled | `failed` | Current time within DND window |
| `ringing` | Callee accepts | `answered` | Answer SDP sent, WebRTC connected |
| `ringing` | Callee rejects | `rejected` | Callee taps reject |
| `ringing` | 30s timeout | `missed` | No answer in 30 seconds |
| `ringing` | Caller cancels | `cancelled` | Caller hangs up before answer |
| `answered` | Either party ends call | `ended` | Hangup signal, duration calculated |
| `answered` | Network failure | `ended` | Connection lost, call terminated |
| `missed` | Caller leaves voicemail | `voicemail_left` | Voicemail recorded and uploaded |
| `missed` | Caller doesn't leave voicemail | `missed` (terminal) | No voicemail |

### DND State Machine

```
dnd_off ──time_enters_dnd_window──> dnd_on ──time_exits_dnd_window──> dnd_off
  │                                      │
  └──dnd_on ──teacher_disables_dnd──> dnd_off
  │
  └──dnd_off ──teacher_enables_dnd──> dnd_on
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `dnd_off` | Current time enters DND window | `dnd_on` | time >= dnd_calls_start OR time < dnd_calls_end |
| `dnd_on` | Current time exits DND window | `dnd_off` | time >= dnd_calls_end AND time < dnd_calls_start |
| `dnd_off` | Teacher manually enables DND | `dnd_on` | Teacher toggles DND on |
| `dnd_on` | Teacher manually disables DND | `dnd_off` | Teacher toggles DND off |

### Voicemail State Machine

```
no_voicemail ──caller_records──> recording ──caller_stops──> uploaded
  │                                │
  │                                └──caller_cancels──> no_voicemail
  │
  └──uploaded ──callee_listens──> listened
  │
  └──uploaded (terminal, retained for history)
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `no_voicemail` | Caller starts recording | `recording` | After missed call |
| `recording` | Caller stops (or 60s limit) | `uploaded` | Audio uploaded to Supabase Storage |
| `recording` | Caller cancels | `no_voicemail` | Recording discarded |
| `uploaded` | Callee listens | `listened` | Callee plays voicemail |
| `uploaded` | (retained) | `uploaded` | Voicemail retained for history |

---

## 8. Backend Architecture

### 8.1 Component Overview

`CallSignalingService` handles WebSocket-based WebRTC signaling (SDP offer/answer, ICE candidates). `CallService` manages call lifecycle (initiate, accept, reject, end, missed, voicemail). Reuses signaling patterns from `LIVE_CLASSES_SPEC.md` but simplified for 1-on-1 P2P (no SFU). `CallRouting` defines REST + WebSocket endpoints.

### 8.2 Design Principles

1. **Privacy-first** — no personal phone numbers, all calls via app identity
2. **P2P direct** — WebRTC peer-to-peer, no SFU for 1-on-1 (lower latency, lower cost)
3. **Logged** — all calls recorded in call_logs for history and audit
4. **Respectful** — DND hours, busy indicators, voicemail for missed calls
5. **Resilient** — audio-only fallback on poor network
6. **School-scoped** — all calls within school context

### 8.3 Core Types

#### CallSignalingService

```kotlin
// Reuse signaling infrastructure from LIVE_CLASSES_SPEC.md
// But for 1-on-1 calls (simpler — no SFU needed, direct peer-to-peer)

class CallSignalingService {
    // WebSocket-based signaling
    suspend fun onOffer(callerId: UUID, calleeId: UUID, sdp: String)
    suspend fun onAnswer(calleeId: UUID, callerId: UUID, sdp: String)
    suspend fun onIceCandidate(userId: UUID, peerId: UUID, candidate: IceCandidate)
    suspend fun onHangup(userId: UUID, peerId: UUID)
}
```

#### CallService

```kotlin
class CallService {
    suspend fun initiateCall(
        callerId: UUID, calleeId: UUID, threadId: UUID, callType: String
    ): CallDto {
        // 1. Check callee's DND / calls_enabled
        // 2. Check if callee is busy (active call)
        // 3. Create call_log (status=ringing)
        // 4. Send FCM to callee: incoming call notification
        // 5. Start 30-second timeout for missed call
    }

    suspend fun acceptCall(callId: UUID, calleeId: UUID): CallDto
    suspend fun rejectCall(callId: UUID, calleeId: UUID): CallDto
    suspend fun endCall(callId: UUID, userId: UUID): CallDto
    suspend fun missedCallTimeout(callId: UUID)  // auto-triggered after 30s
    suspend fun leaveVoicemail(callId: UUID, audioUrl: String, duration: Int): CallDto
    suspend fun getCallHistory(threadId: UUID): List<CallDto>
}
```

### 8.4 Repositories

- `CallLogRepository` — CRUD for `call_logs`
- `ActiveCallRegistry` — in-memory tracking of active calls (for busy check)

### 8.5 Mappers

- `CallLogMapper` — maps `call_logs` rows to DTOs

### 8.6 Permission Checks

- Initiate call: parent or teacher with active message thread
- Accept/reject/end: callee or caller (own call only)
- Voicemail: caller of missed call only
- Call history: participants of the message thread only
- DND settings: own notification preferences only

### 8.7 Background Jobs

- **Missed Call Timeout** — per-call, 30-second timer
  1. Call initiated → status=ringing → 30s timer started
  2. If no answer in 30s → status=missed → FCM to callee → caller offered voicemail
  3. Timer cancelled if call answered or rejected before 30s

- **Active Call Cleanup** — every 1 minute
  1. Check for calls with status=answered but no heartbeat in 60 seconds
  2. Mark as ended (network failure) → calculate duration
  3. Return count of cleaned-up calls

### 8.8 Domain Events

- `CallInitiated` — emitted when call is initiated
- `CallAnswered` — emitted when callee accepts
- `CallRejected` — emitted when callee rejects
- `CallEnded` — emitted when call ends (either party)
- `CallMissed` — emitted when 30s timeout reached
- `VoicemailLeft` — emitted when voicemail is uploaded
- `DndEnabled` — emitted when DND is toggled on
- `DndDisabled` — emitted when DND is toggled off

### 8.9 Caching

- Active calls: in-memory registry (real-time, no caching)
- Call history: cached locally (5-minute TTL)
- DND preferences: cached locally (10-minute TTL)
- Voicemail URLs: not cached (signed URLs, time-limited)

### 8.10 Transactions

- Initiate call: single transaction (check DND, check busy, insert call_log)
- Accept call: single transaction (update status, set started_at)
- End call: single transaction (update status, set ended_at, calculate duration)
- Voicemail: single transaction (update call_log with voicemail_url)

### 8.11 Rate Limiting

- Initiate call: 10 per hour per user (prevent spam)
- Voicemail: 5 per hour per user
- WebSocket signaling: 100 messages per minute per connection

### 8.12 Configuration

- `CALLS_ENABLED` — default `false`; enable/disable calling feature
- `CALLS_MISSED_TIMEOUT_SECONDS` — default `30`; timeout for missed call
- `CALLS_VOICEMAIL_MAX_SECONDS` — default `60`; max voicemail duration
- `CALLS_VOICEMAIL_MAX_SIZE_MB` — default `5`; max voicemail file size
- `CALLS_STUN_SERVER` — default `stun:stun.l.google.com:19302`; STUN server
- `CALLS_TURN_SERVER` — default `""`; TURN server (if needed for NAT)
- `CALLS_TURN_USERNAME` — default `""`; TURN credentials
- `CALLS_TURN_PASSWORD` — default `""`; TURN credentials
- `CALLS_AUDIO_ONLY_FALLBACK_ENABLED` — default `true`; enable audio-only fallback

---

## 9. API Contracts

### 9.1 REST Endpoints

```
POST /api/v1/calls/initiate
  Body: { thread_id, callee_id, call_type }
  → 201: CallDto
  → 403: DND enabled / calls disabled / busy

POST /api/v1/calls/{id}/accept
  → 200: CallDto

POST /api/v1/calls/{id}/reject
  → 200: CallDto

POST /api/v1/calls/{id}/end
  → 200: CallDto

POST /api/v1/calls/{id}/voicemail
  Body: { audio_url, duration_seconds }
  → 200: CallDto

GET /api/v1/calls/history?thread_id={uuid}
  → 200: { calls: [CallDto] }
```

### 9.2 WebSocket Signaling

```
WS /api/v1/calls/signaling  -- bidirectional for SDP/ICE exchange

Messages (JSON):
  { type: "offer", sdp: "..." }
  { type: "answer", sdp: "..." }
  { type: "ice", candidate: { ... } }
  { type: "hangup" }
  { type: "busy" }
  { type: "ringing" }
```

### 9.3 DTO Models

All `@Serializable`, wrapped in `ApiResponse<T>` pattern.

```kotlin
@Serializable data class CallDto(
    val id: String,
    val threadId: String,
    val callerId: String,
    val callerName: String,
    val callerRole: String,
    val calleeId: String,
    val calleeName: String,
    val calleeRole: String,
    val callType: String,        // voice | video
    val status: String,          // ringing | answered | rejected | missed | failed | ended
    val startedAt: String?,
    val endedAt: String?,
    val durationSeconds: Int?,
    val voicemailUrl: String?,
    val voicemailDurationSeconds: Int?,
    val createdAt: String,
)

@Serializable data class InitiateCallRequest(
    val threadId: String,
    val calleeId: String,
    val callType: String,        // voice | video
)

@Serializable data class VoicemailRequest(
    val audioUrl: String,
    val durationSeconds: Int,
)

@Serializable data class IceCandidate(
    val sdp: String,
    val sdpMLineIndex: Int,
    val sdpMid: String,
)
```

---

## 10. Frontend Architecture

### 10.1 Screens

| Screen | Platform | Role | Description |
|---|---|---|---|
| `IncomingCallScreen` | Compose | Parent, Teacher | Incoming call UI (accept/reject) |
| `ActiveCallScreen` | Compose | Parent, Teacher | Active call with controls |
| `CallHistoryScreen` | Compose | Parent, Teacher | Call history per thread |
| `VoicemailPlayerScreen` | Compose | Parent, Teacher | Play voicemail audio |
| `DndSettingsScreen` | Compose | Teacher | DND hours configuration |

### 10.2 Navigation

- From message thread → tap call button → call initiated
- Incoming call: full-screen overlay (above all navigation)
- Active call: full-screen with controls
- Call history: within message thread detail
- DND settings: within notification preferences

### 10.3 UX Flows

#### Parent: Initiate Video Call
1. Parent opens message thread with teacher
2. Taps "Video Call" button
3. App creates offer SDP → sends via WebSocket
4. Waiting screen: "Calling {teacher_name}..."
5. Teacher answers → call connected → active call screen
6. Call controls available (mute, camera, end)
7. Tap "End Call" → call ended → duration logged

#### Teacher: Receive Incoming Call
1. Teacher receives FCM: "Incoming video call from {parent_name}"
2. Full-screen incoming call UI (caller name, photo, accept/reject)
3. Teacher taps "Accept" → answer SDP → call connected
4. OR teacher taps "Reject" → caller notified "Call rejected"

#### Parent: Leave Voicemail
1. After missed call (30s timeout), parent sees "No answer"
2. Taps "Leave Voicemail"
3. Recording UI: record button, timer (max 60s)
4. Stop recording → audio uploaded → voicemail sent
5. Teacher notified: "Voicemail from {parent_name}"

### 10.4 State Management

```kotlin
data class CallState(
    val activeCall: CallDto?,
    val isInitiating: Boolean,
    val isIncoming: Boolean,
    val callStatus: String,      // idle | ringing | answered | ended
    val isMuted: Boolean,
    val isCameraOn: Boolean,
    val isSpeakerOn: Boolean,
    val callDuration: Duration,
    val error: String?,
)

data class CallHistoryState(
    val calls: List<CallDto>,
    val isLoading: Boolean,
    val error: String?,
)

data class VoicemailState(
    val isRecording: Boolean,
    val recordingDuration: Duration,
    val voicemailUrl: String?,
    val error: String?,
)
```

### 10.5 Offline Support

- Calls: require online (WebRTC P2P, WebSocket signaling)
- Call history: cached locally (5-minute TTL)
- Voicemail: requires online (upload to Supabase Storage)
- DND settings: cached locally (10-minute TTL)

### 10.6 Loading States

- Initiating call: "Calling {name}..."
- Incoming call: ringing sound + vibration
- Ending call: "Ending call..."
- Uploading voicemail: "Uploading voicemail..."
- Loading history: "Loading call history..."

### 10.7 Error Handling (UI)

- Call failed (busy): "{name} is on another call."
- Call failed (DND): "{name} has Do Not Disturb enabled. You can leave a voicemail."
- Call failed (network): "Call failed. Please check your network."
- WebRTC error: "Connection error. Call ended."
- Voicemail upload failed: "Failed to upload voicemail. Please try again."
- Camera permission denied: "Camera permission required for video calls."
- Microphone permission denied: "Microphone permission required for calls."

### 10.8 Component Integration Guidelines

| Rule | Description |
|---|---|
| **R1** | Incoming call: full-screen overlay, caller name + photo, accept (green) / reject (red) |
| **R2** | Active call: video fills screen (video calls), controls overlay (semi-transparent) |
| **R3** | Call controls: mute, camera toggle (video), speaker, end call (red, prominent) |
| **R4** | Call duration: displayed in real-time (mm:ss format) |
| **R5** | Audio-only fallback: video area replaced with avatar + "Audio only" indicator |
| **R6** | Call history: list with call type icon, status, duration, timestamp |
| **R7** | Voicemail indicator: play button in call history for calls with voicemail |
| **R8** | DND: toggle in notification preferences, time range picker |
| **R9** | Call buttons: voice (phone icon) and video (video icon) in message thread header |
| **R10** | Ringing: device vibration + ringtone (respects device settings) |

---

## 11. Shared Module Changes (KMP)

### 11.1 DTOs

All DTOs defined in section 9.3, placed in `shared/.../calls/domain/model/CallModels.kt`.

### 11.2 Domain Models

```kotlin
data class CallLog(
    val id: String,
    val schoolId: String,
    val threadId: String,
    val callerId: String,
    val callerName: String,
    val callerRole: String,
    val calleeId: String,
    val calleeName: String,
    val calleeRole: String,
    val callType: CallType,
    val status: CallStatus,
    val startedAt: Instant?,
    val endedAt: Instant?,
    val durationSeconds: Int?,
    val voicemailUrl: String?,
    val voicemailDurationSeconds: Int?,
    val createdAt: Instant,
)

enum class CallType { VOICE, VIDEO }
enum class CallStatus { RINGING, ANSWERED, REJECTED, MISSED, FAILED, ENDED }
```

### 11.3 Repository Interfaces

```kotlin
interface CallRepository {
    suspend fun initiateCall(token: String, request: InitiateCallRequest): NetworkResult<CallDto>
    suspend fun acceptCall(token: String, callId: String): NetworkResult<CallDto>
    suspend fun rejectCall(token: String, callId: String): NetworkResult<CallDto>
    suspend fun endCall(token: String, callId: String): NetworkResult<CallDto>
    suspend fun leaveVoicemail(token: String, callId: String, request: VoicemailRequest): NetworkResult<CallDto>
    suspend fun getCallHistory(token: String, threadId: String): NetworkResult<List<CallDto>>
}
```

### 11.4 UseCases

- `InitiateCallUseCase`
- `AcceptCallUseCase`
- `RejectCallUseCase`
- `EndCallUseCase`
- `LeaveVoicemailUseCase`
- `GetCallHistoryUseCase`
- `ConnectSignalingUseCase`

### 11.5 Validation

- `thread_id`: non-empty, valid UUID
- `callee_id`: non-empty, valid UUID
- `call_type`: one of voice, video
- `audio_url`: valid URL (for voicemail)
- `duration_seconds`: positive integer, max 60 (voicemail)

### 11.6 Serialization

Standard Kotlinx serialization. Enums serialized as lowercase strings. Timestamps as ISO strings.

### 11.7 Network APIs

Ktor `@Resource` route definitions in `CallApi.kt`:
- POST `/api/v1/calls/initiate`
- POST `/api/v1/calls/{id}/accept`
- POST `/api/v1/calls/{id}/reject`
- POST `/api/v1/calls/{id}/end`
- POST `/api/v1/calls/{id}/voicemail`
- GET `/api/v1/calls/history?thread_id={uuid}`
- WS `/api/v1/calls/signaling`

### 11.8 Database Models (Local Cache)

- Call history: cached locally (5-minute TTL)
- DND preferences: cached locally (10-minute TTL)
- Active call: in-memory only (not cached)

---

## 12. Permissions Matrix

| Action | Super Admin | School Admin | Counselor | Teacher | Parent | Student |
|---|---|---|---|---|---|---|
| Initiate call | N/A | N/A | N/A | ✅ (own threads) | ✅ (own threads) | ❌ |
| Receive call | N/A | N/A | N/A | ✅ (own threads) | ✅ (own threads) | ❌ |
| Accept/reject call | N/A | N/A | N/A | ✅ (own call) | ✅ (own call) | ❌ |
| End call | N/A | N/A | N/A | ✅ (own call) | ✅ (own call) | ❌ |
| Leave voicemail | N/A | N/A | N/A | ✅ (own missed call) | ✅ (own missed call) | ❌ |
| View call history | N/A | ✅ (own school) | N/A | ✅ (own threads) | ✅ (own threads) | ❌ |
| Set DND hours | N/A | N/A | N/A | ✅ (own) | ✅ (own) | ❌ |
| Enable/disable calls | N/A | N/A | N/A | ✅ (own) | ✅ (own) | ❌ |

---

## 13. Notifications

### Call Notifications

| Trigger | Recipient | Channel | Template |
|---|---|---|---|
| Incoming call | Callee | FCM (high priority) + in-app ringing | "Incoming {call_type} call from {caller_name}" |
| Missed call | Callee | FCM (normal) + in-app | "Missed {call_type} call from {caller_name}" |
| Voicemail received | Callee | FCM (normal) + in-app | "Voicemail from {caller_name} ({duration}s)" |
| Call rejected | Caller | In-app | "{callee_name} rejected your call." |
| Call ended | Both | In-app | "Call ended. Duration: {duration}" |

### Notification Integration

Uses `SmartNotificationService` with "high" priority for incoming calls (needs to ring immediately). Uses "normal" priority for missed call and voicemail notifications. FCM data payload includes call_id, caller_name, call_type for in-app call screen.

---

## 14. Background Jobs

### Missed Call Timeout

| Property | Value |
|---|---|
| **Name** | Per-call timer (not scheduled job) |
| **Trigger** | Call initiated (status=ringing) |
| **Duration** | 30 seconds |
| **Action** | If no answer: status=missed, FCM to callee, caller offered voicemail |

### Active Call Cleanup

| Property | Value |
|---|---|
| **Name** | `ActiveCallCleanupJob` |
| **Schedule** | Every 1 minute |
| **Duration** | < 5 seconds |

#### Job Flow

1. Check for calls with status=answered but no heartbeat in 60 seconds
2. Mark as ended (network failure) → calculate duration from started_at to now
3. Return count of cleaned-up calls

---

## 15. Integrations

### Internal Integrations

| System | Integration Point | Direction | Protocol | Error Handling |
|---|---|---|---|---|
| `MESSAGING_SYSTEM_SPEC.md` | Message thread context | Read | Direct DB | Required for call initiation |
| `LIVE_CLASSES_SPEC.md` | WebRTC signaling patterns | Reuse | Code reuse | N/A (pattern reuse) |
| `NotificationPreferencesTable` | DND hours, calls_enabled | Read | Direct DB | Required for DND check |
| `MessageThreadsTable` | Thread context | Read | Direct DB | Required |
| `AppUsersTable` | User info | Read | Direct DB | Required |
| `Notify.kt` | Notification dispatch | Call | Direct call | Log error, continue |
| `SmartNotificationService` | Notification priority | Call | Direct call | Log error, continue |

### External Integrations

| System | Integration Point | Direction | Protocol | Error Handling |
|---|---|---|---|---|
| Google WebRTC SDK | WebRTC client | Use | SDK (Android/iOS/Web) | Log error, call fails |
| STUN server | NAT traversal | Call | UDP | Fallback to TURN |
| TURN server | NAT traversal (relay) | Call | UDP/TCP | Call fails if TURN unavailable |
| Supabase Storage | Voicemail storage | Call | HTTP API | Log error, voicemail upload fails |
| FCM | Call notifications | Call | HTTP API | Log error, notification not sent |

### Integration Patterns

- **WebRTC signaling:** WebSocket-based, reuses patterns from `LIVE_CLASSES_SPEC.md` but simplified for 1-on-1 P2P (no SFU)
- **Call notification:** FCM with high priority for incoming calls, data payload for in-app call screen
- **Voicemail storage:** Audio uploaded to Supabase Storage, signed URL stored in call_logs
- **DND check:** Before call initiation, check callee's notification_preferences for DND hours

---

## 16. Security

### Authentication

- All call endpoints: JWT auth (parent or teacher role)
- WebSocket signaling: JWT auth on connection
- Call initiation: requires active message thread between caller and callee

### Authorization

- Initiate call: parent or teacher with active message thread
- Accept/reject/end: only call participants (caller or callee)
- Voicemail: only caller of missed call
- Call history: only thread participants
- DND settings: own notification preferences only

### Data Protection

- No personal phone numbers exchanged (app identity only)
- Call logs contain app user IDs, not phone numbers
- Voicemail audio stored in Supabase Storage with signed URLs
- Call history visible only to thread participants
- No call recording (privacy protection)

### Input Validation

- `thread_id`: non-empty, valid UUID, caller must be participant
- `callee_id`: non-empty, valid UUID, must be thread participant
- `call_type`: one of voice, video
- `audio_url`: valid URL (for voicemail)
- `duration_seconds`: positive integer, max 60

### Rate Limiting

- Initiate call: 10 per hour per user
- Voicemail: 5 per hour per user
- WebSocket signaling: 100 messages per minute per connection

### Audit Logging

- Call initiated: caller ID, callee ID, thread ID, call type, timestamp
- Call answered: call ID, callee ID, timestamp
- Call rejected: call ID, callee ID, timestamp
- Call ended: call ID, user ID, duration, timestamp
- Call missed: call ID, timestamp
- Voicemail left: call ID, caller ID, duration, timestamp
- DND toggled: user ID, enabled/disabled, timestamp

### PII Handling

- Caller/callee names visible in call logs (necessary for identification)
- No phone numbers stored or exchanged
- Voicemail audio may contain voice (PII) — stored in Supabase Storage, signed URLs
- Call history visible only to thread participants
- No call content stored (no recording)

### Multi-tenant Isolation

- All call_logs have school_id — school-scoped
- Call initiation requires message thread (school-scoped)
- No cross-school calls
- WebSocket connection scoped to user's school

---

## 17. Performance & Scalability

### Expected Scale

- Concurrent 1-on-1 calls: ~10-20 per school (peak hours)
- Call duration: ~3-10 minutes average
- Voicemail: ~1-5 per day per school
- Call history: ~10-50 records per thread
- WebSocket connections: ~100-200 concurrent (for signaling)

### Query Optimization

- Call history: `idx_call_logs_thread(thread_id, created_at DESC)` — sorted by most recent
- Busy check: `idx_call_logs_callee(callee_id, status, created_at DESC)` — check for active call
- DND check: direct lookup on notification_preferences (by user ID)

### Indexing Strategy

- `call_logs(thread_id, created_at DESC)` — call history per thread
- `call_logs(callee_id, status, created_at DESC)` — busy check, missed calls by callee

### Caching Strategy

- Active calls: in-memory registry (real-time, no caching)
- Call history: cached locally (5-minute TTL)
- DND preferences: cached locally (10-minute TTL)

### Pagination

- Call history: 20 per page (infinite scroll)

### Connection Pooling

Uses existing HikariCP connection pool. WebSocket connections managed separately (Ktor WebSocket sessions).

### Async Processing

- Call initiation: synchronous (caller waits for ringing confirmation)
- Signaling: asynchronous (WebSocket, real-time)
- Missed call timeout: async (per-call timer)
- Voicemail upload: synchronous (caller waits for confirmation)
- Notifications: async (existing infrastructure)

### Scalability Concerns

- WebRTC P2P: no server bandwidth for media (P2P direct). Only signaling on server.
- WebSocket connections: ~200 concurrent. Ktor handles this well.
- Voicemail storage: ~5/day × ~2MB = ~10MB/day. Supabase Storage handles this.
- DB storage: ~50 call_logs/day. Negligible.
- STUN/TURN: STUN is free (Google). TURN may incur cost if many users behind NAT.

---

## 18. Edge Cases

| # | Scenario | Expected Behavior |
|---|---|---|
| EC-1 | Callee on another call | Return 403. "Callee is on another call." Call_log status=failed. |
| EC-2 | Callee has DND enabled | Return 403. "Callee has Do Not Disturb enabled." Voicemail offered. |
| EC-3 | Callee has calls disabled | Return 403. "Callee has calls disabled." |
| EC-4 | Caller initiates call but loses network | Call fails. WebSocket disconnects. Call_log status=failed. |
| EC-5 | Callee accepts but WebRTC fails | Call status=answered but connection fails. Cleanup job marks as ended. |
| EC-6 | Voicemail exceeds 60 seconds | Client-side timer stops recording at 60s. Uploads what's recorded. |
| EC-7 | Voicemail upload fails | Caller informed. Can retry. Call_log has no voicemail_url. |
| EC-8 | Both parties try to end call simultaneously | First end call processed. Second returns current state (ended). |
| EC-9 | Caller cancels before callee answers | Call_log status=cancelled (or missed). Callee notified "Call cancelled." |
| EC-10 | WebSocket disconnects during call | WebRTC may continue (P2P). If call ends, cleanup job handles it. |
| EC-11 | Camera permission denied (video call) | Fallback to voice call. Inform user. |
| EC-12 | Microphone permission denied | Call cannot proceed. Inform user to grant permission. |

---

## 19. Error Handling

### Error Response Format

Standard `ApiResponse` error format.

### Error Codes

| Code | HTTP Status | Description | User Message |
|---|---|---|---|
| `CALLEE_BUSY` | 403 | Callee on another call | "{name} is on another call." |
| `CALLEE_DND` | 403 | Callee has DND enabled | "{name} has Do Not Disturb enabled. You can leave a voicemail." |
| `CALLEE_CALLS_DISABLED` | 403 | Callee has calls disabled | "{name} has calls disabled." |
| `CALL_NOT_FOUND` | 404 | Call not found | "Call not found." |
| `NOT_CALL_PARTICIPANT` | 403 | User not participant in call | "You are not a participant in this call." |
| `THREAD_NOT_FOUND` | 404 | Message thread not found | "Message thread not found." |
| `NOT_THREAD_PARTICIPANT` | 403 | User not participant in thread | "You are not a participant in this thread." |
| `VOICEMAIL_UPLOAD_FAILED` | 500 | Voicemail upload failed | "Failed to upload voicemail. Please try again." |
| `CALL_ALREADY_ENDED` | 409 | Call already ended | "This call has already ended." |
| `SIGNALING_ERROR` | 500 | WebSocket signaling error | "Connection error. Please try again." |

### Error Handling Strategy

- **Busy/DND:** Return 403. Caller informed, voicemail offered.
- **WebRTC failure:** Call terminated. Both parties informed.
- **Voicemail upload failure:** Caller can retry. Call_log updated if successful.
- **WebSocket disconnect:** Cleanup job handles orphaned calls.
- **Permission denied:** UI prompts user to grant camera/microphone permission.

### Retry Strategy

- Call initiation: caller can retry (busy/DND may change)
- Voicemail upload: caller can retry
- WebSocket: auto-reconnect (if call still active)

### Fallback Behavior

- Video fails → audio-only fallback (video track disabled, audio continues)
- STUN fails → TURN server used (relay)
- TURN fails → call fails (NAT traversal not possible)
- FCM unavailable → in-app notification still works (if app is open)
- Supabase Storage unavailable → voicemail upload fails, caller informed

---

## 20. Analytics & Reporting

### Analytics Dashboard Data

| Metric | Source | Derivation |
|---|---|---|
| Total calls | `call_logs` | Count per school |
| Calls by type | `call_logs` | Count per call_type (voice/video) |
| Calls by status | `call_logs` | Count per status (answered/missed/rejected/failed) |
| Answer rate | `call_logs` | answered / total |
| Average call duration | `call_logs` | avg(duration_seconds) where status=ended |
| Voicemails left | `call_logs` | Count where voicemail_url IS NOT NULL |
| DND enabled users | `notification_preferences` | Count where calls_enabled=false OR DND active |
| Peak concurrent calls | Active call registry | Max concurrent calls per day |

### Export Capabilities

- Call activity report (CSV) — user, call count, total duration, answer rate
- Voicemail report (CSV) — callee, voicemail count, total duration

### Report Types

| Report | Format | Frequency | Recipient |
|---|---|---|---|
| Call activity | JSON (API) | Monthly | Product Team |
| Call quality | JSON (API) | Monthly | DevOps Team |
| DND adoption | JSON (API) | Monthly | Product Team |

---

## 21. Testing Strategy

### Unit Tests

- `CallService.initiateCall()` — DND check, busy check, call_log creation
- `CallService.acceptCall()` — status update, started_at set
- `CallService.endCall()` — duration calculation, status update
- `CallService.missedCallTimeout()` — 30s timeout, status=missed
- `CallService.leaveVoicemail()` — voicemail URL update
- `CallSignalingService` — offer/answer/ICE/hangup routing
- DND time window check (boundary cases: midnight crossover)

### Integration Tests

- Full call flow: initiate → ringing → accept → answered → end → duration logged
- Missed call flow: initiate → ringing → 30s timeout → missed → voicemail
- Busy flow: callee on call → new call → failed (busy)
- DND flow: DND enabled → call → failed (DND) → voicemail
- WebSocket signaling: offer → answer → ICE exchange → hangup
- Permission checks: non-participant blocked from call
- Multi-tenant: cross-school call blocked

### E2E Tests

- Parent initiates video call → teacher accepts → call connected → ends → history shows call
- Parent initiates call → teacher doesn't answer → missed call → parent leaves voicemail → teacher listens
- Teacher sets DND → parent calls → DND message → parent leaves voicemail
- Audio-only fallback: video call → network degrades → audio-only mode

### Performance Tests

- Call setup: < 3 seconds (offer → answer → connected)
- WebSocket: 200 concurrent connections
- Call history: < 2 seconds for 50 records
- Missed call timeout: exactly 30 seconds

### Test Data

- 2 test users (parent + teacher) with message thread
- Test call logs (various statuses)
- Test voicemail audio file
- Mock WebRTC client
- Mock FCM service
- Test STUN/TURN servers

### Test Environment

- Test database with call_logs table
- Mock WebRTC client (no real P2P)
- Mock FCM service
- Test WebSocket server
- Test Supabase Storage bucket (for voicemail)
- Test JWT tokens for parent and teacher

---

## 22. Acceptance Criteria

- [ ] Parent initiates voice/video call from message thread
- [ ] Teacher receives incoming call notification
- [ ] Teacher accepts/rejects call
- [ ] WebRTC audio/video works peer-to-peer
- [ ] Call controls: mute, camera toggle, end call
- [ ] Audio-only fallback on poor network
- [ ] Missed call logged after 30s timeout
- [ ] Voicemail: parent leaves voice message if teacher unavailable
- [ ] Call history with duration
- [ ] DND hours respected
- [ ] Busy indicator when teacher on another call
- [ ] No personal phone numbers exchanged
- [ ] School-scoped (no cross-school calls)
- [ ] Call logs retained for history

---

## 23. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration `migration_091_voice_video_calls.sql`, Exposed table, register in `DatabaseFactory` |
| 2 | 3 days | WebRTC signaling server (WebSocket) — `CallSignalingService` |
| 3 | 3 days | WebRTC client (expect/actual: Android + iOS + Web) — `WebRtcClient` |
| 4 | 2 days | `CallService` (initiate, accept, reject, end, voicemail, missed call timeout) |
| 5 | 2 days | FCM call notifications + missed call timeout + active call cleanup job |
| 6 | 4 days | Client UI: `IncomingCallScreen`, `ActiveCallScreen`, `CallHistoryScreen`, `VoicemailPlayerScreen`, `DndSettingsScreen` |
| 7 | 2 days | Tests: unit, integration, E2E |

### Pre-Implementation Checklist

- [ ] Verify `MESSAGING_SYSTEM_SPEC.md` is implemented (message threads)
- [ ] Verify `LIVE_CLASSES_SPEC.md` WebRTC signaling patterns are available
- [ ] Verify STUN/TURN servers are configured
- [ ] Verify Supabase Storage is available (for voicemail)
- [ ] Verify FCM is available (for call notifications)
- [ ] Verify Google WebRTC SDK is integrated (Android, iOS, Web)
- [ ] Verify `NotificationPreferencesTable` can be modified

---

## 24. File-Level Impact Analysis

### Server

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add + Modify | `CallLogsTable` + columns on `NotificationPreferencesTable` |
| `server/.../db/DatabaseFactory.kt` | Modify | Register new table in `allTables` |
| `server/.../feature/calls/CallSignalingService.kt` | **New** | WebSocket signaling |
| `server/.../feature/calls/CallService.kt` | **New** | Call lifecycle management |
| `server/.../feature/calls/CallRouting.kt` | **New** | REST + WS endpoints |
| `server/.../feature/calls/ActiveCallRegistry.kt` | **New** | In-memory active call tracking |
| `docs/db/migration_091_voice_video_calls.sql` | **New** | DDL |

### Shared (KMP)

| File | Change Type | Description |
|---|---|---|
| `shared/.../calls/domain/model/CallModels.kt` | **New** | DTOs, domain models, enums |
| `shared/.../calls/domain/repository/CallRepository.kt` | **New** | Repository interface |
| `shared/.../calls/data/remote/CallApi.kt` | **New** | HTTP + WS API definitions |
| `shared/.../core/webrtc/WebRtcClient.kt` | **New** (expect/actual) | WebRTC client (Android, iOS, Web) |

### Client (Compose)

| File | Change Type | Description |
|---|---|---|
| `composeApp/.../ui/v2/screens/calls/IncomingCallScreen.kt` | **New** | Incoming call UI |
| `composeApp/.../ui/v2/screens/calls/ActiveCallScreen.kt` | **New** | Active call UI with controls |
| `composeApp/.../ui/v2/screens/calls/CallHistoryScreen.kt` | **New** | Call history per thread |
| `composeApp/.../ui/v2/screens/calls/VoicemailPlayerScreen.kt` | **New** | Voicemail playback |
| `composeApp/.../ui/v2/screens/calls/DndSettingsScreen.kt` | **New** | DND hours configuration |
| `composeApp/.../ui/v2/screens/parent/MessagesScreenV2.kt` | Modify | Add call buttons (voice + video) |
| `composeApp/.../ui/v2/screens/teacher/MessagesScreenV2.kt` | Modify | Add call buttons (voice + video) |

---

## 25. Future Enhancements

| # | Enhancement | Priority | Effort | Notes |
|---|---|---|---|---|
| F-1 | Call recording | Low | M | Record calls (with consent, privacy concern) |
| F-2 | Call transcription | Low | L | AI-powered transcription of calls |
| F-3 | Screen sharing | Medium | M | Share screen during video calls |
| F-4 | Call scheduling | Medium | S | Schedule calls for later (calendar integration) |
| F-5 | Group calls (3+ participants) | Medium | L | Multi-party WebRTC (requires SFU) |
| F-6 | Call quality metrics | Low | S | Real-time quality dashboard (bitrate, latency, packet loss) |
| F-7 | Call reactions | Low | S | Emoji reactions during calls |
| F-8 | Virtual backgrounds | Low | M | Blur or replace background in video calls |
| F-9 | Call forwarding | Low | M | Forward call to another teacher |
| F-10 | Integration with phone dialer | Low | XL | Bridge to phone network (VoIP gateway) |

---

## Appendix A: Sequence Diagrams

### A.1 Call Initiation and Answer

```
Parent (app)        Server              DB              Teacher (app)
  │                    │                  │                │
  │  POST /calls/      │                  │                │
  │  initiate          │                  │                │
  │  {thread_id,       │                  │                │
  │   callee_id,       │                  │                │
  │   call_type: video}│                  │                │
  │  ───────────────>  │                  │                │
  │                    │──check DND──────>│                │
  │                    │←──dnd off────────│                │
  │                    │──check busy──────>│                │
  │                    │←──not busy───────│                │
  │                    │──insert call_log>│                │
  │                    │  (status=ringing)│                │
  │                    │←──ok─────────────│                │
  │                    │                  │                │
  │                    │──FCM: incoming call────────────>│
  │                    │                  │   (ringing)    │
  │  ←──201: CallDto──│                  │                │
  │  (status=ringing)  │                  │                │
  │                    │                  │                │
  │  WS: offer SDP     │                  │                │
  │  ───────────────>  │                  │                │
  │                    │──relay offer─────────────────────>│
  │                    │                  │                │
  │                    │←──answer SDP─────────────────────│
  │                    │──relay answer──>│                │
  │  ←──WS: answer────│                  │                │
  │                    │                  │                │
  │  POST /calls/      │                  │                │
  │  {id}/accept       │                  │                │
  │  (from teacher)    │                  │                │
  │                    │──update status──>│                │
  │                    │  (answered,      │                │
  │                    │   started_at)    │                │
  │                    │←──ok─────────────│                │
  │                    │                  │                │
  │  WebRTC P2P established                 │              │
  │  ══════════════════════════════════════════════════════│
  │  (audio/video flows directly P2P)       │              │
  │                    │                  │                │
  │  POST /calls/      │                  │                │
  │  {id}/end          │                  │                │
  │  ───────────────>  │──update status──>│                │
  │                    │  (ended,          │                │
  │                    │   ended_at,       │                │
  │                    │   duration)       │                │
  │                    │←──ok─────────────│                │
  │  ←──200: CallDto──│                  │                │
  │  (status=ended,    │                  │                │
  │   duration=180s)   │                  │                │
  │                    │                  │                │
```

### A.2 Missed Call and Voicemail

```
Parent (app)        Server              DB              Teacher (app)
  │                    │                  │                │
  │  (call ringing     │                  │                │
  │   for 30s...)      │                  │                │
  │                    │                  │                │
  │                    │──30s timeout─────│                │
  │                    │──update status──>│                │
  │                    │  (missed)        │                │
  │                    │←──ok─────────────│                │
  │                    │──FCM: missed call───────────────>│
  │  ←──WS: missed────│                  │                │
  │                    │                  │                │
  │  (parent sees      │                  │                │
  │   "No answer" +    │                  │                │
  │   "Leave Voicemail")│                 │                │
  │                    │                  │                │
  │  (records audio,   │                  │                │
  │   uploads to       │                  │                │
  │   Supabase Storage)│                  │                │
  │                    │                  │                │
  │  POST /calls/      │                  │                │
  │  {id}/voicemail    │                  │                │
  │  {audio_url,       │                  │                │
  │   duration: 30}    │                  │                │
  │  ───────────────>  │──update call_log>│                │
  │                    │  (voicemail_url, │                │
  │                    │   voicemail_dur) │                │
  │                    │←──ok─────────────│                │
  │                    │──FCM: voicemail─────────────────>│
  │  ←──200: CallDto──│                  │                │
  │                    │                  │                │
```

---

## Appendix B: Domain Model / ER Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                      call_logs (new)                                  │
│  id (PK)                                                              │
│  school_id, thread_id (FK message_threads)                            │
│  caller_id, caller_name, caller_role                                  │
│  callee_id, callee_name, callee_role                                  │
│  call_type (voice | video)                                            │
│  status (ringing | answered | rejected | missed | failed | ended)    │
│  started_at, ended_at, duration_seconds                               │
│  voicemail_url, voicemail_duration_seconds                            │
│  created_at                                                           │
│  INDEX: (thread_id, created_at DESC)                                  │
│  INDEX: (callee_id, status, created_at DESC)                          │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│              notification_preferences (modified)                      │
│  ... existing columns ...                                             │
│  dnd_calls_start (NEW, default '20:00')                               │
│  dnd_calls_end (NEW, default '07:00')                                 │
│  calls_enabled (NEW, default true)                                    │
└──────────────────────────────────────────────────────────────────────┘

Existing tables used:
┌──────────────────┐  ┌──────────────────┐
│ message_threads  │  │ app_users        │
│ (existing)       │  │ (existing)       │
│ thread context   │  │ user accounts    │
└──────────────────┘  └──────────────────┘
```

---

## Appendix C: Event Flow

### Domain Events

| Event | Emitter | Consumers | Payload | Side Effects |
|---|---|---|---|---|
| `CallInitiated` | `CallService.initiateCall()` | `NotificationService` | `callId, callerId, calleeId, callType` | FCM to callee |
| `CallAnswered` | `CallService.acceptCall()` | None (logged) | `callId, calleeId` | Audit log |
| `CallRejected` | `CallService.rejectCall()` | `NotificationService` (caller) | `callId, calleeId` | In-app to caller |
| `CallEnded` | `CallService.endCall()` | None (logged) | `callId, userId, duration` | Audit log |
| `CallMissed` | `CallService.missedCallTimeout()` | `NotificationService` (callee) | `callId, callerId` | FCM to callee |
| `VoicemailLeft` | `CallService.leaveVoicemail()` | `NotificationService` (callee) | `callId, callerId, duration` | FCM to callee |
| `DndEnabled` | Notification preferences update | None (logged) | `userId, dndStart, dndEnd` | Audit log |
| `DndDisabled` | Notification preferences update | None (logged) | `userId` | Audit log |

### Event Delivery Guarantees

- Call initiated: synchronous, FCM async
- Call answered/rejected: synchronous, notification async
- Call ended: synchronous, audit log async
- Call missed: async (timeout-triggered), FCM async
- Voicemail: synchronous, FCM async
- DND toggle: synchronous, audit log async

---

## Appendix D: Configuration

### Environment Variables

| Variable | Default | Description |
|---|---|---|
| `CALLS_ENABLED` | `false` | Enable/disable calling feature |
| `CALLS_MISSED_TIMEOUT_SECONDS` | `30` | Timeout for missed call |
| `CALLS_VOICEMAIL_MAX_SECONDS` | `60` | Max voicemail duration |
| `CALLS_VOICEMAIL_MAX_SIZE_MB` | `5` | Max voicemail file size |
| `CALLS_STUN_SERVER` | `stun:stun.l.google.com:19302` | STUN server |
| `CALLS_TURN_SERVER` | `""` | TURN server (if needed) |
| `CALLS_TURN_USERNAME` | `""` | TURN credentials |
| `CALLS_TURN_PASSWORD` | `""` | TURN credentials |
| `CALLS_AUDIO_ONLY_FALLBACK_ENABLED` | `true` | Enable audio-only fallback |

### Feature Flags

| Flag | Default | Description |
|---|---|---|
| `CALLS_ENABLED` | `false` | Enable/disable calling |
| `CALLS_VIDEO_ENABLED` | `true` | Enable video calls |
| `CALLS_VOICEMAIL_ENABLED` | `true` | Enable voicemail |
| `CALLS_AUDIO_ONLY_FALLBACK_ENABLED` | `true` | Audio-only fallback |

### School-Level Settings

| Setting | Default | Description |
|---|---|---|
| `calls_enabled` | `false` | Per-school enable/disable |
| `calls_video_enabled` | `true` | Per-school video calls |
| `calls_voicemail_enabled` | `true` | Per-school voicemail |

---

## Appendix E: Migration & Rollback

### Migration: `migration_091_voice_video_calls.sql`

```sql
-- Migration 091: Voice & Video Calls
-- Creates call_logs table and modifies notification_preferences

BEGIN;

-- Create call_logs table
CREATE TABLE IF NOT EXISTS call_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    thread_id       UUID NOT NULL REFERENCES message_threads(id),
    caller_id       UUID NOT NULL,
    caller_name     TEXT NOT NULL,
    caller_role     VARCHAR(32) NOT NULL,
    callee_id       UUID NOT NULL,
    callee_name     TEXT NOT NULL,
    callee_role     VARCHAR(32) NOT NULL,
    call_type       VARCHAR(16) NOT NULL,
    status          VARCHAR(16) NOT NULL,
    started_at      TIMESTAMP,
    ended_at        TIMESTAMP,
    duration_seconds INTEGER,
    voicemail_url   TEXT,
    voicemail_duration_seconds INTEGER,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_call_logs_thread
    ON call_logs (thread_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_call_logs_callee
    ON call_logs (callee_id, status, created_at DESC);

-- Add DND columns to notification_preferences
ALTER TABLE notification_preferences
    ADD COLUMN IF NOT EXISTS dnd_calls_start VARCHAR(8) DEFAULT '20:00';
ALTER TABLE notification_preferences
    ADD COLUMN IF NOT EXISTS dnd_calls_end VARCHAR(8) DEFAULT '07:00';
ALTER TABLE notification_preferences
    ADD COLUMN IF NOT EXISTS calls_enabled BOOLEAN NOT NULL DEFAULT true;

COMMIT;
```

### Rollback: `migration_091_rollback.sql`

```sql
BEGIN;
ALTER TABLE notification_preferences DROP COLUMN IF EXISTS calls_enabled;
ALTER TABLE notification_preferences DROP COLUMN IF EXISTS dnd_calls_end;
ALTER TABLE notification_preferences DROP COLUMN IF EXISTS dnd_calls_start;
DROP TABLE IF EXISTS call_logs;
COMMIT;
```

### Migration Validation

- Verify `call_logs` table created with indexes and FK
- Verify `notification_preferences` has DND columns
- Run `SELECT count(*) FROM call_logs` — should be 0 (new feature)
- Verify FK to `message_threads` is valid

---

## Appendix F: Observability

### Structured Logging

| Log Level | Event | Context Fields |
|---|---|---|
| INFO | Call initiated | `callId, callerId, calleeId, callType, threadId` |
| INFO | Call answered | `callId, calleeId, startedAt` |
| INFO | Call rejected | `callId, calleeId` |
| INFO | Call ended | `callId, userId, durationSeconds` |
| INFO | Call missed | `callId, callerId, calleeId` |
| INFO | Voicemail left | `callId, callerId, durationSeconds` |
| INFO | DND toggled | `userId, enabled, dndStart, dndEnd` |
| WARN | Call failed (busy) | `callId, calleeId` |
| WARN | Call failed (DND) | `callId, calleeId` |
| WARN | Call failed (network) | `callId, error` |
| WARN | Audio-only fallback | `callId, reason` |
| WARN | Voicemail upload failed | `callId, error` |
| ERROR | WebSocket signaling error | `userId, error` |
| ERROR | WebRTC connection failed | `callId, error` |

### Metrics

| Metric | Type | Labels | Description |
|---|---|---|---|
| `calls_total` | Counter | `school_id, call_type, status` | Total calls by type and status |
| `calls_duration_avg` | Histogram | `school_id, call_type` | Average call duration |
| `calls_concurrent` | Gauge | `school_id` | Concurrent active calls |
| `calls_voicemail_total` | Counter | `school_id` | Voicemails left |
| `calls_missed_total` | Counter | `school_id` | Missed calls |
| `calls_dnd_blocked` | Counter | `school_id` | Calls blocked by DND |
| `calls_busy_blocked` | Counter | `school_id` | Calls blocked (busy) |
| `calls_websocket_connections` | Gauge | `school_id` | Active WebSocket connections |
| `calls_setup_duration` | Histogram | `school_id` | Call setup time (offer → connected) |

### Health Checks

| Check | Endpoint | Description |
|---|---|---|
| Call service | `/health/calls` | Verify service and DB accessible |
| WebSocket | `/health/calls/ws` | Verify WebSocket server running |
| STUN/TURN | `/health/calls/stun` | Verify STUN/TURN reachable |

### Alerts

| Alert | Condition | Severity | Notification |
|---|---|---|---|
| WebSocket server down | Health check failed | Critical | Email + SMS to dev team |
| STUN/TURN unreachable | Health check failed | Critical | Email + SMS to dev team |
| High call failure rate | > 20% failures in 1 hour | Warning | Email to dev team |
| Voicemail upload failures | > 5 failures in 1 hour | Warning | Email to dev team |
| Active call cleanup backlog | > 10 orphaned calls | Warning | Email to dev team |

### Dashboards

| Dashboard | Panels | Audience |
|---|---|---|
| Call Overview | Total calls, answer rate, avg duration | Product Team |
| Call Quality | Setup time, audio-only fallback rate, failures | DevOps Team |
| Voicemail | Voicemails left, upload success rate | Product Team |
| DND | DND adoption, calls blocked by DND | Product Team |

### Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| WebRTC P2P fails (NAT) | Medium | High | STUN + TURN fallback. TURN relay for difficult NATs. |
| Voicemail storage cost | Low | Low | Max 60s, max 5MB. ~10MB/day. Negligible. |
| Inappropriate call behavior | Low | Medium | All calls logged. DND available. School admin can disable calls. |
| WebSocket scalability | Low | Medium | Ktor handles ~1000 concurrent WS. Monitor. |
| Privacy breach (call content) | Low | High | No call recording. P2P direct. No server-side media. |
| FCM delivery delay | Medium | Medium | High priority FCM for incoming calls. In-app ringing as backup. |
