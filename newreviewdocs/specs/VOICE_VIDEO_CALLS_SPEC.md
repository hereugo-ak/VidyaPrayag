# Voice & Video Calls — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** `MESSAGING_SYSTEM_SPEC.md`, `LIVE_CLASSES_SPEC.md`
> **Source:** `COMPETITIVE_GAP_ANALYSIS.md` — emerging trend

---

## 1. Feature Overview

In-app voice and video calling between parent and teacher (1-on-1), eliminating the need to share personal phone numbers. Uses WebRTC for peer-to-peer communication with Ktor signaling server.

### Goals

- Parent can initiate voice/video call to teacher from messaging screen
- Teacher can initiate voice/video call to parent
- No personal phone numbers exchanged — uses app identity
- Call ringing, acceptance, rejection, and missed call tracking
- Call history with duration
- Call quality adaptation (audio-only fallback on poor network)
- Voicemail: if teacher unavailable, parent can leave a voice message

---

## 2. Current System Assessment

- `MESSAGING_SYSTEM_SPEC.md` — thread-based messaging between parent and teacher
- `LIVE_CLASSES_SPEC.md` — WebRTC infrastructure for video (reusable signaling)
- `MessageThreadsTable` — existing thread context for call initiation
- No voice/video calling exists
- `COMPETITIVE_GAP_ANALYSIS.md`: voice/video calls as emerging trend

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Parent initiates voice/video call from message thread with teacher |
| FR-2 | Teacher receives incoming call notification (FCM + in-app ringing) |
| FR-3 | Teacher accepts or rejects call |
| FR-4 | Call established: WebRTC peer-to-peer audio/video |
| FR-5 | Call controls: mute, camera toggle, speaker, end call |
| FR-6 | Audio-only fallback: if video fails or network poor, downgrade to audio |
| FR-7 | Missed call: if teacher doesn't answer in 30 seconds → missed call notification + logged |
| FR-8 | Voicemail: parent can record voice message if teacher unavailable |
| FR-9 | Call history: list of calls (type, duration, status) per thread |
| FR-10 | Busy indicator: if teacher on another call, parent sees "busy" |
| FR-11 | Do Not Disturb: teacher can set DND hours (no call notifications) |

---

## 4. Database Design

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

### 4.1 Modify Existing: `notification_preferences`

```sql
ALTER TABLE notification_preferences ADD COLUMN dnd_calls_start VARCHAR(8) DEFAULT '20:00';
ALTER TABLE notification_preferences ADD COLUMN dnd_calls_end VARCHAR(8) DEFAULT '07:00';
ALTER TABLE notification_preferences ADD COLUMN calls_enabled BOOLEAN NOT NULL DEFAULT true;
```

---

## 5. Backend Architecture

### 5.1 WebRTC Signaling (Ktor)

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

### 5.2 CallService

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

### 5.3 Call Flow

```
1. Parent taps "Video Call" in message thread
2. App creates offer SDP → sends via WebSocket signaling
3. Server sends FCM to teacher: "Incoming video call from {parent_name}"
4. Teacher app shows incoming call screen (ringing)
5. Teacher accepts → creates answer SDP → sent via signaling
6. WebRTC connection established → audio/video flows P2P
7. Either party taps "End Call" → hangup signal → call_log updated
8. If teacher doesn't answer in 30s → missed call → parent can leave voicemail
```

---

## 6. API Contracts

```
# REST endpoints
POST /api/v1/calls/initiate  { thread_id, callee_id, call_type }
POST /api/v1/calls/{id}/accept
POST /api/v1/calls/{id}/reject
POST /api/v1/calls/{id}/end
POST /api/v1/calls/{id}/voicemail  { audio_url, duration_seconds }
GET /api/v1/calls/history?thread_id={uuid}

# WebSocket signaling
WS /api/v1/calls/signaling  -- bidirectional for SDP/ICE exchange
```

---

## 7. Frontend Architecture

### 7.1 Call UI

```kotlin
@Composable
fun IncomingCallScreen(call: CallDto, onAccept: () -> Unit, onReject: () -> Unit)
@Composable
fun ActiveCallScreen(
    callType: String,  // voice | video
    calleeName: String,
    calleeAvatar: String?,
    callDuration: Duration,
    onMute: () -> Unit,
    onCameraToggle: () -> Unit,
    onEndCall: () -> Unit
)
```

### 7.2 WebRTC Client

```kotlin
// expect/actual pattern for WebRTC
expect class WebRtcClient() {
    fun createOffer(): String  // SDP
    fun createAnswer(remoteSdp: String): String  // SDP
    fun setRemoteDescription(sdp: String)
    fun addIceCandidate(candidate: IceCandidate)
    fun toggleMute()
    fun toggleCamera()
    fun hangup()
    val localVideoTrack: VideoTrack?
    val remoteVideoTrack: VideoTrack?
    val remoteAudioTrack: AudioTrack?
}

// Android: Google WebRTC SDK
// iOS: Google WebRTC framework
// Web: browser WebRTC API
```

---

## 8. Acceptance Criteria

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

---

## 9. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration, Exposed table |
| 2 | 3 days | WebRTC signaling server (WebSocket) |
| 3 | 3 days | WebRTC client (expect/actual: Android + iOS + Web) |
| 4 | 2 days | CallService (initiate, accept, reject, end, voicemail) |
| 5 | 2 days | FCM call notifications + missed call timeout |
| 6 | 4 days | Client UI (incoming call, active call, call history, voicemail) |
| 7 | 2 days | Tests |

---

## 10. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add + Modify | `CallLogsTable` + columns on notification_preferences |
| `server/.../feature/calls/CallSignalingService.kt` | New | WebSocket signaling |
| `server/.../feature/calls/CallService.kt` | New | Call lifecycle |
| `server/.../feature/calls/CallRouting.kt` | New | REST + WS endpoints |
| `docs/db/migration_090_voice_video_calls.sql` | New | DDL |
| `shared/.../core/webrtc/WebRtcClient.kt` | New + expect/actual | WebRTC client |
| `composeApp/.../ui/v2/screens/calls/IncomingCallScreen.kt` | New | Incoming call UI |
| `composeApp/.../ui/v2/screens/calls/ActiveCallScreen.kt` | New | Active call UI |
| `composeApp/.../ui/v2/screens/parent/MessagesScreenV2.kt` | Modify | Add call buttons |
