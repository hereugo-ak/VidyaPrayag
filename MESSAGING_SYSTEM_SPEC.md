# VidyaPrayag Messaging System — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Audience:** Senior engineer / AI agent implementing the system
> **Last updated:** 2026-06-12

---

## Table of Contents

1. [Current System Assessment](#1-current-system-assessment)
2. [Gap Analysis](#2-gap-analysis)
3. [Target Architecture](#3-target-architecture)
4. [Data Flow & Message Lifecycle](#4-data-flow--message-lifecycle)
5. [Realtime Architecture](#5-realtime-architecture)
6. [Synchronization & Offline-First Model](#6-synchronization--offline-first-model)
7. [Delivery, Ordering & Read Status Guarantees](#7-delivery-ordering--read-status-guarantees)
8. [Database Schema Evolution](#8-database-schema-evolution)
9. [API Contracts](#9-api-contracts)
10. [Security Model](#10-security-model)
11. [Permission Matrix](#11-permission-matrix)
12. [Media Pipeline](#12-media-pipeline)
13. [Push Notifications](#13-push-notifications)
14. [Offline Behavior, Caching & Conflict Resolution](#14-offline-behavior-caching--conflict-resolution)
15. [Pagination, Search & Advanced Features](#15-pagination-search--advanced-features)
16. [Infrastructure & Scalability](#16-infrastructure--scalability)
17. [Monitoring, Logging & Analytics](#17-monitoring-logging--analytics)
18. [Testing Strategy](#18-testing-strategy)
19. [Deployment & Rollout](#19-deployment--rollout)
20. [Migration Plan](#20-migration-plan)
21. [Implementation Roadmap](#21-implementation-roadmap)
22. [Risk Register & Trade-offs](#22-risk-register--trade-offs)
23. [Event & Error Catalogue](#23-event--error-catalogue)
24. [Glossary](#24-glossary)

---

## 1. Current System Assessment

### 1.1 Architecture Overview

The VidyaPrayag platform is a Kotlin Multiplatform (KMP) school management system:

| Layer | Technology | Entry Point |
|---|---|---|
| **Backend** | Kotlin + Ktor 3.4.3 (Netty) | `server/.../Application.kt` |
| **Shared client** | KMP (Android + iOS + JVM + JS + wasmJs) | `shared/.../di/Koin.kt` |
| **Compose UI** | Compose Multiplatform 1.10.3 | `composeApp/.../ui/v2/` |

**Backend stack:** Ktor 3.4.3 (Netty), Exposed ORM 0.50.0 → PostgreSQL (Supabase) / SQLite (dev), HikariCP (pool 5), JWT HS256, Firebase Admin SDK 9.4.3 for FCM, Supabase Storage REST, kotlinx.serialization JSON.

**Client stack:** Ktor Client 3.4.3 (OkHttp/Darwin/CIO), Koin 4.0.0 DI, Compose Multiplatform, DataStore preferences, Coil 3.4.0.

### 1.2 Existing Messaging Implementation

#### 1.2.1 Conversation Model

Two-row-per-conversation model. Each two-party conversation creates two rows in `message_threads` — one owned by each participant — sharing the same `conversation_id`. Messages stored in `messages` table keyed by `conversation_id`.

**Key files:**
- `server/.../feature/school/MessagingCore.kt` — `sendInConversation`, `conversationMessagesFor`, `notifyMessageRecipient`
- `server/.../feature/school/MessagesRouting.kt` — admin routes
- `server/.../feature/teacher/TeacherMessagesRouting.kt` — teacher routes
- `server/.../feature/user/ParentMessagesRouting.kt` — parent routes

#### 1.2.2 Existing Database Schema

```sql
CREATE TABLE message_threads (
    id              UUID PRIMARY KEY,
    school_id       UUID NOT NULL,
    owner_user_id   UUID NOT NULL,
    conversation_id UUID NOT NULL,
    peer_user_id    UUID,
    sender_name     TEXT NOT NULL,
    sender_role     TEXT NOT NULL,
    sender_image_url TEXT,
    icon_name       TEXT,
    last_message    TEXT NOT NULL,
    last_message_at TIMESTAMP NOT NULL,
    unread_count    INT NOT NULL DEFAULT 0,
    is_read         BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL
);

CREATE TABLE messages (
    id              UUID PRIMARY KEY,
    conversation_id UUID NOT NULL,
    thread_id       UUID NOT NULL,
    sender_id       UUID,
    body            TEXT NOT NULL,
    created_at      TIMESTAMP NOT NULL
);
```

#### 1.2.3 Existing API Endpoints

| Role | Base path | Endpoints |
|---|---|---|
| Admin/School Admin | `/api/v1/school/messages` | `GET /threads`, `GET /threads/{id}/messages`, `POST /threads/{id}/read`, `POST` (send) |
| Teacher | `/api/v1/teacher/messages` | Same + `POST /class` (broadcast) |
| Parent | `/api/v1/parent/messages` | Same + `GET /recipients` |

**Send flow (`sendInConversation`):**
1. `thread_id` provided → append to existing conversation
2. `recipient_user_id` provided (no thread) → create new two-party conversation
3. Neither → self/system thread
4. After write → best-effort push via `notifyMessageRecipient`

#### 1.2.4 Existing Notification Infrastructure

- `NotificationsTable` — per-recipient rows (category, title, body, deep_link, actor_id, is_read)
- `DeviceTokensTable` — multi-device FCM token registry
- `Notify.kt` — writes notification rows + dispatches FCM
- Routes: `GET /notifications`, `GET /notifications/summary`, `POST /notifications/{id}/read`, `POST /notifications/read-all`, `POST /api/device-tokens`

#### 1.2.5 Existing Authentication

- **Parents:** OTP login → JWT access (15 min) + refresh (30 days, rotated)
- **Staff:** Email + password → same JWT pair
- JWT claims: `sub` (UUID), `role`, `name`
- Every request validates `is_active=true` (instant kill-switch)
- Client: Koin `single { HttpClient }` with `installTokenAuth` — auto 401 → refresh → retry
- `SessionManager` clears bearer cache on logout

#### 1.2.6 Existing Media Infrastructure

- `MediaRouting.kt` — `POST /api/v1/school/media/upload` (multipart)
- `SupabaseStorage.kt` — REST wrapper (upload, delete, public URL)
- Path: `{schoolId}/{kind}/{uuid}.{ext}` (multi-tenant safe)
- Max 25 MB, validated content types
- **Not wired to messaging** — school gallery/logo/profile only

#### 1.2.7 Existing Networking & DI

- Koin `commonModule` — single `HttpClient` with `ContentNegotiation`, `HttpTimeout` (60s), `Auth` (bearer + auto-refresh)
- Each feature has `XxxApi` class wrapping shared `HttpClient`
- Interface → Impl repository pattern, all Koin `single`s
- `NetworkResult<T>` sealed class (Success / Error / ConnectionError) with `safeApiCall`
- **No client local DB** — all data fetched fresh per screen load

#### 1.2.8 Existing UI

- `MessagesScreenV2.kt` (admin) — inbox + conversation + compose
- `ParentMessagesScreenV2.kt` (parent) — same + recipients picker
- `MessagesViewModel.kt` / `ParentMessageViewModel.kt` — state machine: idle/loading/loaded/error/openThread/composeOpen
- State-driven navigation (no NavHost), `VStateHost` for loading/error/content
- No teacher messaging screen in `ui/v2/screens/`

### 1.3 What Works

- Two-party conversation model supports parent ↔ teacher ↔ admin
- Multi-tenant isolation via `school_id`
- Role-gated routes prevent cross-role access
- Push notification foundation (FCM + device tokens) operational
- Media upload pipeline (Supabase Storage) proven and reusable
- JWT auth with auto-refresh is robust

---

## 2. Gap Analysis

### 2.1 Critical Gaps (P0)

| # | Gap | Current State |
|---|---|---|
| G1 | No realtime delivery (no WS/SSE/polling) | Pure request-response REST |
| G2 | No client-side persistence | DataStore stores only auth tokens |
| G3 | No per-message status tracking | Only thread-level unread_count |
| G4 | No typing indicators | Not implemented |
| G5 | No presence | Not implemented |
| G6 | No message pagination | `selectAll()` loads all at once |
| G7 | No message editing/deletion | Not implemented |
| G8 | No media in messages | Media pipeline is gallery-only |
| G9 | No message search | Not implemented |
| G10 | No draft persistence | In-memory only |
| G11 | No retry/queue for failed sends | NetworkResult.Error shown, no retry |
| G12 | No idempotency on send | No client-generated message ID |

### 2.2 Important Gaps (P1)

| # | Gap |
|---|---|
| G13 | No message reactions |
| G14 | No message forwarding |
| G15 | No link previews |
| G16 | No group conversations (broadcast is fan-out 1:1) |
| G17 | No message threading/replies |
| G18 | No mute/block |
| G19 | No spam prevention beyond JWT auth |
| G20 | No multi-device sync |
| G21 | No message encryption at rest |
| G22 | No backpressure on notification fan-out |

### 2.3 Nice-to-Have Gaps (P2)

G23: Voice messages. G24: Stickers/GIFs. G25: Read receipts opt-out. G26: Message scheduling. G27: Conversation pinning/archiving. G28: Export conversation.

---

## 3. Target Architecture

### 3.1 High-Level Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                      CLIENT (KMP)                            │
│  Compose UI ←→ ViewModel ←→ Repository ←→ Local DB (SQLDelight)
│                    ↕                        ↕                │
│              Sync Engine (outbox, delta sync, conflict)      │
│                    ↕                        ↕                │
│         Ktor HttpClient (REST)    WebSocket Client (realtime) │
└──────────────────┬──────────────────────────┬───────────────┘
                   │ HTTP/HTTPS              │ WSS
┌──────────────────┼──────────────────────────┼───────────────┐
│           BACKEND (Ktor)                     │               │
│  ┌──────────┐  ┌──────────┐  ┌────────────┐ │               │
│  │ REST     │  │ WebSocket│  │ Notification│ │               │
│  │ Routes   │  │ Server   │  │ Service(FCM)│ │               │
│  └────┬─────┘  └────┬─────┘  └──────┬─────┘ │               │
│       └──────┬──────────────┬──────┘        │               │
│         Service Layer                       │               │
│  Messaging / Presence / Media / Search      │               │
│       └──────┬──────────────┬──────────────┘               │
│     PostgreSQL  │  Redis (presence/pubsub)  │  Supabase     │
│     (Supabase)  │                           │  Storage      │
└──────────────────────────────────────────────────────────────┘
```

### 3.2 Component Inventory

| Component | Status | Location |
|---|---|---|
| REST message routes (3 roles) | **Existing** | `server/.../feature/{school,teacher,user}/` |
| `sendInConversation` engine | **Existing, modify** | `MessagingCore.kt` |
| `Notify.kt` + notification tables | **Existing** | `server/.../feature/notifications/` |
| `SupabaseStorage.kt` | **Existing, extend** | `server/.../feature/media/` |
| JWT auth | **Existing** | `server/.../core/JwtConfig.kt` |
| Koin DI | **Existing, extend** | `shared/.../di/Koin.kt` |
| MessagesScreenV2 / ParentMessagesScreenV2 | **Existing, modify** | `composeApp/.../ui/v2/screens/` |
| WebSocket server | **New** | `server/.../feature/messaging/ws/` |
| Presence service | **New** | `server/.../feature/messaging/presence/` |
| Redis connection | **New** | `server/.../infrastructure/` |
| Client local DB (SQLDelight) | **New** | `shared/.../core/local/` |
| Client sync engine + outbox | **New** | `shared/.../core/sync/` |
| Client WebSocket client | **New** | `shared/.../core/network/` |
| Message search service | **New** | `server/.../feature/messaging/search/` |
| New DB tables (status, attachments, reactions) | **New** | `server/.../db/Tables.kt` |
| Teacher messages screen | **New** | `composeApp/.../ui/v2/screens/teacher/` |
| Rate limiter | **New** | `server/.../core/ratelimit/` |

### 3.3 Architectural Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Realtime transport | WebSocket (Ktor `WebSockets`) | Bidirectional, low latency, native Ktor + JWT |
| Presence | Redis (pub/sub + TTL keys) | Low-latency, multi-instance fan-out |
| Client local DB | SQLDelight | Mature KMP-native, type-safe, all targets |
| Sync model | Delta sync with server cursor | Minimal payload, deterministic ordering |
| Conflict resolution | LWW + client UUID idempotency | Simple, deterministic |
| Message ordering | Server-assigned monotonic seq per conversation | Global order per conversation |
| Media in messages | Reuse Supabase Storage with `MESSAGE` kind | Proven pipeline, no new infra |
| Search | PostgreSQL FTS (tsvector + GIN) | No additional infra needed |
| Rate limiting | Token bucket per user (in-memory + Redis) | Simple, effective |

---

## 4. Data Flow & Message Lifecycle

### 4.1 Message Send Sequence

```
Client → POST /messages/send {client_msg_id, thread_id, body, attachments[]}
  → Server validates + rate limits + idempotency check
  → INSERT message (assign seq) + INSERT message_status (SENT)
  → Publish event to Redis pubsub "msg:{conversation_id}"
  → Return 201 {message_id, seq, server_timestamp}
  → Server WS delivers msg_new to recipient if online
  → If recipient offline: FCM push via notifyMessageRecipient
Client → Update local outbox (status=SENT), update local DB
```

### 4.2 Message State Machine

```
DRAFT → SENDING → SENT → DELIVERED → READ
                 ↘ FAILED (auto-retry → SENDING)
```

| Stage | Trigger | Server Action | WS Event |
|---|---|---|---|
| DRAFT | User types | None | None |
| SENDING | User taps send | None | None |
| SENT | Server returns 201 | INSERT msg + status | `msg_new` to recipient |
| DELIVERED | Recipient ACKs via WS | UPDATE status | `msg_status` to sender |
| READ | Recipient opens conversation | UPDATE status + thread | `msg_status` + `thread_read` |
| FAILED | HTTP error/timeout | None | None |
| EDITED | User edits (24h window) | UPDATE body, edited_at | `msg_edit` to recipient |
| DELETED | User deletes | UPDATE deleted_at, body=NULL | `msg_delete` to recipient |

---

## 5. Realtime Architecture

### 5.1 WebSocket Server

Single endpoint: `WS /ws/v1/messaging?token=<jwt>`

**Connection lifecycle:**
1. Client sends WS upgrade with JWT query param
2. Server validates JWT + `is_active=true`
3. Register in `ConnectionRegistry` (userId → Set<WebSocketSession>)
4. Set Redis presence key (TTL 90s, refreshed by heartbeat)
5. Client sends `hello` with last seq per conversation
6. Server delivers missed events (delta sync)
7. Heartbeat: client ping every 30s, server pong + refresh Redis TTL
8. On disconnect: unregister + delete presence key + persist `last_seen_at`

**Multi-instance fan-out:** Redis pub/sub. Sender's instance publishes to `msg:{conversation_id}` channel. Recipient's instance subscribes and delivers via WS.

### 5.2 Event Protocol

**Client → Server:**

| Type | Payload | Purpose |
|---|---|---|
| `hello` | `{device_id, last_seqs: {conv_id: int}}` | Handshake + delta sync |
| `ping` | `{}` | Heartbeat |
| `typing` | `{conversation_id, is_typing}` | Typing indicator |
| `ack` | `{message_id, status: "DELIVERED"}` | Acknowledge receipt |
| `read` | `{conversation_id}` | Mark read |
| `presence_query` | `{user_ids: [uuid]}` | Query presence |

**Server → Client:**

| Type | Payload | Purpose |
|---|---|---|
| `pong` | `{}` | Heartbeat response |
| `msg_new` | `{message, conversation_id}` | New message |
| `msg_status` | `{message_id, conversation_id, status, user_id}` | Status update |
| `msg_edit` | `{message_id, conversation_id, body, edited_at}` | Edited message |
| `msg_delete` | `{message_id, conversation_id}` | Deleted message |
| `typing` | `{conversation_id, user_id, is_typing}` | Typing from peer |
| `presence` | `{user_id, status, last_seen_at}` | Presence update |
| `thread_update` | `{thread}` | Thread metadata changed |
| `error` | `{code, message}` | Error frame |

### 5.3 Reconnection Strategy

```
DISCONNECTED → CONNECTING → CONNECTED
                    ↓ failure    ↓ disconnect
               RECONNECTING   RECONNECTING (backoff: 1s,2s,4s,8s,16s,30s cap, ±20% jitter)
                    ↓ auth 401
               REFRESH_TOKEN → refresh ok → CONNECTING
                             → refresh fail → DISCONNECTED (logout)

On reconnect: send hello with cursors → delta sync → flush outbox
```

---

## 6. Synchronization & Offline-First Model

### 6.1 Client Local DB (SQLDelight)

Tables: `thread_local`, `message_local`, `outbox_local`, `draft_local`.

Key queries per table:
- **thread_local:** `SELECT ... ORDER BY last_message_at DESC`, `INSERT OR REPLACE`, `UPDATE unread_count=0`
- **message_local:** `SELECT ... ORDER BY COALESCE(seq,0), created_at ASC LIMIT :limit OFFSET :offset`, `INSERT OR REPLACE`, `UPDATE status`, `UPDATE deleted_at, body=NULL`
- **outbox_local:** `SELECT ... WHERE status='SENDING' OR (status='FAILED' AND next_retry_at <= :now)`, retry with backoff
- **draft_local:** `INSERT OR REPLACE`, `DELETE` on send

### 6.2 Sync Engine

```kotlin
class SyncEngine(localDb, messagingApi, webSocketClient, outboxManager) {
    suspend fun initialSync(): SyncResult       // on app launch
    suspend fun deltaSync(cursors: Map<String, Long>): SyncResult  // on WS reconnect
    suspend fun loadConversation(convId, offset, limit): List<MessageLocal>
    suspend fun backgroundSync(): SyncResult    // WorkManager periodic
    suspend fun clearAll()                      // on logout
}
```

### 6.3 Delta Sync Protocol

1. Client maintains `sync_cursor` per conversation in `thread_local`
2. On WS reconnect, sends `hello` with `{conv_id: cursor}` for each conversation
3. Server: `SELECT * FROM messages WHERE conversation_id=? AND seq>? ORDER BY seq ASC`
4. Server streams events: `msg_new`, `msg_status`, `msg_edit`, `msg_delete`
5. Client applies events, advancing `sync_cursor`

### 6.4 Outbox Manager

Retry policy: attempt 1 immediate, 2: +2s, 3: +5s, 4: +15s, 5: +60s, 6+: +300s (cap), max 20 attempts.

### 6.5 Conflict Resolution

| Conflict | Resolution |
|---|---|
| Duplicate send (same client_msg_id) | Server 409; client treats as success |
| Edit + delete concurrent | Delete wins (tombstone) |
| Status regression (READ→DELIVERED) | Server ignores |
| Offline edit of server-deleted message | Server 410; client marks deleted |

---

## 7. Delivery, Ordering & Read Status Guarantees

### 7.1 Ordering

- Per-conversation monotonic `seq = MAX(seq)+1` in single transaction
- Display ordered by `seq`; outbox messages shown at end with "sending" indicator
- Reply-quoting via `reply_to_id` (validated same conversation)

### 7.2 Delivery Guarantees

| Guarantee | Mechanism |
|---|---|
| At-least-once send | Outbox + client_msg_id idempotency |
| At-most-once WS delivery | Fire-and-forget; delta sync on reconnect covers offline |
| Order preservation | Server seq; delta sync replays in order |

### 7.3 Read Status

New `message_status` table tracks per-message per-user status (SENT/DELIVERED/READ).

Flow: recipient opens conversation → server updates all messages to READ → publishes `msg_status` to sender → sender sees blue ticks.

### 7.4 Typing Indicators

Ephemeral, WS-only, never persisted. 5s server-side auto-expiry if no `is_typing: false`.

### 7.5 Presence

Redis TTL keys (90s). `last_seen_at` persisted to `app_users` on disconnect. Client subscribes to presence for peers in thread list.

---

## 8. Database Schema Evolution

### 8.1 Modified Tables

**`messages` — add columns:**
```sql
ALTER TABLE messages ADD COLUMN seq INTEGER;
ALTER TABLE messages ADD COLUMN client_msg_id UUID;
ALTER TABLE messages ADD COLUMN edited_at TIMESTAMP;
ALTER TABLE messages ADD COLUMN deleted_at TIMESTAMP;
ALTER TABLE messages ADD COLUMN reply_to_id UUID REFERENCES messages(id);
CREATE INDEX idx_messages_conv_seq ON messages(conversation_id, seq);
CREATE UNIQUE INDEX ux_messages_client_msg_id ON messages(client_msg_id) WHERE client_msg_id IS NOT NULL;
```

Backfill: `ROW_NUMBER() OVER (PARTITION BY conversation_id ORDER BY created_at)` → UPDATE seq.

**`message_threads` — add columns:**
```sql
ALTER TABLE message_threads ADD COLUMN is_muted BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE message_threads ADD COLUMN is_pinned BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE message_threads ADD COLUMN is_archived BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE message_threads ADD COLUMN draft_body TEXT;
```

**`app_users` — add column:**
```sql
ALTER TABLE app_users ADD COLUMN last_seen_at TIMESTAMP;
```

### 8.2 New Tables

**`message_status`:**
```sql
CREATE TABLE message_status (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    conversation_id UUID NOT NULL,
    user_id UUID NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('SENT','DELIVERED','READ')),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(message_id, user_id)
);
CREATE INDEX idx_msg_status_conv_user ON message_status(conversation_id, user_id);
```

**`message_attachments`:**
```sql
CREATE TABLE message_attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    conversation_id UUID NOT NULL,
    sender_id UUID NOT NULL,
    school_id UUID NOT NULL,
    file_name TEXT NOT NULL,
    mime_type TEXT NOT NULL,
    size_bytes BIGINT NOT NULL,
    storage_url TEXT NOT NULL,
    thumbnail_url TEXT,
    attachment_type VARCHAR(16) NOT NULL DEFAULT 'IMAGE',
    width INTEGER, height INTEGER, duration_ms INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_attachments_message ON message_attachments(message_id);
```

**`message_reactions`:**
```sql
CREATE TABLE message_reactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    conversation_id UUID NOT NULL,
    user_id UUID NOT NULL,
    emoji VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(message_id, user_id, emoji)
);
CREATE INDEX idx_reactions_message ON message_reactions(message_id);
```

### 8.3 Full-Text Search

```sql
ALTER TABLE messages ADD COLUMN search_tsv tsvector
  GENERATED ALWAYS AS (to_tsvector('english', coalesce(body, ''))) STORED;
CREATE INDEX idx_messages_tsv ON messages USING GIN(search_tsv);
```

### 8.4 Index Summary

| Index | Query |
|---|---|
| `idx_messages_conv_seq` | Delta sync: `WHERE conversation_id=? AND seq>? ORDER BY seq` |
| `ux_messages_client_msg_id` | Idempotency on send |
| `idx_threads_owner_school_pinned` | Thread list query |
| `idx_msg_status_conv_user` | Read status per conversation |
| `idx_messages_tsv` (GIN) | Full-text search |

---

## 9. API Contracts

### 9.1 Enhanced Send Message

**Request (all three role paths):**
```json
{
  "client_msg_id": "uuid",
  "thread_id": "uuid-or-null",
  "recipient_user_id": "uuid-or-null",
  "body": "Hello",
  "reply_to_id": "uuid-or-null",
  "attachments": [{"file_name":"photo.jpg","mime_type":"image/jpeg","size_bytes":102400,"storage_url":"https://...","attachment_type":"IMAGE"}]
}
```

**Response 201:** `{success, data: {message_id, thread_id, seq, server_timestamp}}`
**Response 409 (duplicate):** `{success:false, data: {message_id, thread_id, seq}}`

### 9.2 Paginated Thread Messages

`GET /api/v1/{role}/messages/threads/{id}/messages?offset=0&limit=50`

Response includes `has_more`, `total_count`. Each message includes `seq`, `status`, `edited_at`, `deleted_at`, `attachments[]`, `reactions[]`.

### 9.3 Delta Sync (New)

`GET /api/v1/{role}/messages/sync?cursors={conv_id:seq}` → returns events array (msg_new, msg_status, msg_edit, msg_delete) + threads_updated.

### 9.4 New Endpoints

| Method | Path | Purpose |
|---|---|---|
| `PATCH` | `/api/v1/{role}/messages/{id}` | Edit message (24h window) |
| `DELETE` | `/api/v1/{role}/messages/{id}?scope=everyone\|me` | Delete message |
| `POST` | `/api/v1/{role}/messages/{id}/reactions` | Add reaction |
| `DELETE` | `/api/v1/{role}/messages/{id}/reactions/{emoji}` | Remove reaction |
| `GET` | `/api/v1/{role}/messages/search?q=...&limit=20` | Search messages |
| `PATCH` | `/api/v1/{role}/messages/threads/{id}` | Update thread prefs (mute/pin/archive) |
| `POST` | `/api/v1/{role}/messages/attachments` | Upload attachment (multipart) |
| `POST` | `/api/v1/{role}/messages/{id}/report` | Report spam |
| `POST` | `/api/v1/{role}/messages/block/{user_id}` | Block user |

### 9.5 Rate Limiting

| Endpoint | Limit | Scope |
|---|---|---|
| Send message | 30/min | Per user |
| Broadcast | 5/hour | Per teacher |
| Edit | 30/min | Per user |
| Upload attachment | 20/min | Per user |
| WS reconnect | 10/min | Per user |
| Search | 10/min | Per user |

Returns HTTP 429 with `Retry-After` header.

---

## 10. Security Model

### 10.1 Authentication (Existing, Reused)

JWT bearer (HS256, 15-min access, 30-day rotated refresh). WS authenticates via `?token=<jwt>` on upgrade. Every request validates `is_active=true`.

### 10.2 Authorization

1. JWT validation + active check
2. Role gating (school_admin/admin for school routes, teacher for teacher routes, parent for parent routes)
3. Thread ownership (`owner_user_id = jwt.sub`)
4. School isolation (`school_id` matches user's school)
5. Send authorization: parent → teachers/admins in child's school; teacher → parents/admins in school; admin → anyone in school

### 10.3 Tenant Isolation

Every row carries `school_id`. Storage path: `{schoolId}/message/{uuid}.{ext}`. WS events only delivered to same-school connections.

### 10.4 Input Validation

| Field | Rule |
|---|---|
| body | Non-blank, max 4096 chars, HTML stripped |
| client_msg_id | Valid UUID, unique per user |
| recipient_user_id | Active user in same school |
| attachments[].storage_url | Supabase URL for user's school bucket |
| attachments[].size_bytes | ≤ 25 MB |
| emoji | Unicode emoji allowlist |

### 10.5 Spam Prevention

Rate limiting (token bucket), content flagging for first messages, report/block mechanism.

### 10.6 Encryption

In transit: TLS 1.2+ (HTTPS/WSS). At rest: Supabase disk encryption. Message body: plaintext (E2EE out of scope). Attachments: public URLs (P1: signed URLs).

### 10.7 Data Retention

Messages: indefinite until deleted. Deleted messages: 30 days then hard delete. Notifications: 90 days.

---

## 11. Permission Matrix

| Action | Parent | Teacher | School Admin | Super Admin |
|---|---|---|---|---|
| View own threads | ✅ | ✅ | ✅ | ✅ |
| Send to teacher | ✅ | N/A | ✅ | ✅ |
| Send to admin | ✅ | ✅ | N/A | ✅ |
| Send to parent | N/A | ✅ | ✅ | ✅ |
| Broadcast to class | ❌ | ✅ (own class) | ✅ (any) | ✅ |
| Edit own message (24h) | ✅ | ✅ | ✅ | ✅ |
| Delete own message | ✅ | ✅ | ✅ | ✅ |
| Delete others' message | ❌ | ❌ | ✅ (school) | ✅ |
| Mark read / mute / pin / archive | ✅ (own) | ✅ (own) | ✅ (own) | ✅ (own) |
| Search | ✅ (own) | ✅ (own) | ✅ (own) | ✅ (own) |
| Reactions / report / block | ✅ | ✅ | ✅ | ✅ |
| View presence | ✅ (peers) | ✅ (peers) | ✅ (school) | ✅ (all) |
| Upload attachment | ✅ | ✅ | ✅ | ✅ |

---

## 12. Media Pipeline

### 12.1 Upload Flow

1. Client picks file → POST multipart to `/api/v1/{role}/messages/attachments`
2. Server validates JWT + role + school + size + MIME
3. Server uploads to Supabase Storage: `{schoolId}/message/{uuid}.{ext}`
4. Returns `{storage_url, thumbnail_url, file_name, mime_type, size_bytes, width, height}`
5. Client includes attachment metadata in send message request
6. Server INSERTs into `message_attachments`

### 12.2 Allowed Types

| Type | MIME | Max |
|---|---|---|
| IMAGE | jpeg/png/webp/gif/heic | 10 MB |
| VIDEO | mp4/mov/webm | 25 MB |
| DOCUMENT | pdf/doc/docx/xls/xlsx/ppt/pptx/txt | 25 MB |
| AUDIO | mpeg/mp4/aac/ogg | 10 MB |

### 12.3 Download

Images via Coil. Documents via Ktor HttpClient download. Videos via platform player streaming from URL.

### 12.4 Storage Cleanup

Hard-delete after 30-day retention: delete storage objects via `SupabaseStorage.delete()`, then delete DB rows.

---

## 13. Push Notifications

### 13.1 Enhanced `notifyMessageRecipient`

- Writes notification row with `deepLink = "messages/{threadId}"`
- If recipient online (WS connected): skip FCM (in-app delivery suffices)
- If thread muted: skip FCM, still write notification row
- If blocked: skip entirely

### 13.2 Notification Suppression

| Condition | Action |
|---|---|
| Recipient online | Skip FCM |
| Thread muted | Skip FCM, keep notification row |
| Sender = recipient | Skip |
| Blocked | Skip |
| Broadcast, no child in class | Skip |

### 13.3 Battery Optimization

WS heartbeat: 30s foreground, 60s background. App killed: FCM only. Android: WorkManager 15-min periodic sync. iOS: background refresh + APNs.

---

## 14. Offline Behavior, Caching & Conflict Resolution

### 14.1 Offline-First Principles

1. Read from local DB first (show stale data + "syncing" indicator)
2. Write to local DB + outbox first (instant UI with "sending" status)
3. Sync engine reconciles via delta sync + outbox flush
4. Network unavailable: read cached conversations, compose queued messages

### 14.2 Cache Strategy

| Data | Location | TTL | Eviction |
|---|---|---|---|
| Thread list | SQLDelight | Until logout | LRU if > 100 MB |
| Messages | SQLDelight | Until logout | Evict convs > 500 msgs (keep 100) |
| Recipients | SQLDelight | 24 hours | Refresh on compose |
| Presence | In-memory | 60s | Clear on screen exit |
| Drafts | SQLDelight | Until sent/deleted | — |
| Attachments | Coil cache | 7 days | LRU |

### 14.3 Conflict Resolution

| Scenario | Resolution |
|---|---|
| Duplicate send | Server dedupes via client_msg_id; 409 = success |
| Edit offline + peer deletes | Delete wins; client receives msg_delete |
| Read offline + new message | Server: unread_count = new count; delta sync updates |
| Outbox permanent failure | Mark FAILED; show retry button |
| Schema mismatch | SQLDelight migration; if fails, clear + re-sync |

---

## 15. Pagination, Search & Advanced Features

### 15.1 Pagination

Threads: no pagination initially (< 100 per user). Messages: offset-based, 50 per page, infinite scroll upward. Delta sync: no pagination (batch if > 500 events).

### 15.2 Search

PostgreSQL FTS via `tsvector` + GIN index. `plainto_tsquery(:term)`. Top 50 results across user's threads, ordered by `created_at DESC`. Tapping result opens conversation scrolled to message.

### 15.3 Editing

24h window. `UPDATE messages SET body=:new, edited_at=now()`. WS `msg_edit` to recipient. UI shows "edited" label.

### 15.4 Deletion

Scope=everyone: tombstone (`deleted_at=now(), body=NULL`). WS `msg_delete`. Scope=me: client-side only. Attachment cleanup after 30-day retention.

### 15.5 Reactions

Allowlist: 👍 ❤️ 😂 😮 😢 👏. Unique per user per message per emoji. Long-press → picker. Shown below bubble with count.

### 15.6 Reply/Threading

Reply-quoting via `reply_to_id`. UI shows quoted original above reply. Not nested threads.

### 15.7 Forwarding

Long-press → Forward → pick recipient → new message with same body. References original storage URLs for attachments. Original sender info NOT included.

### 15.8 Link Previews

Server detects URLs, fetches OpenGraph metadata (5s timeout). Cached in `message_link_previews` table. WS `msg_link_preview` sent async after `msg_new`.

### 15.9 Drafts

Client-only in `draft_local`. Saved on keystroke (debounced 500ms). Deleted on send or clear.

---

## 16. Infrastructure & Scalability

### 16.1 Current vs Target

| Component | Current | Target |
|---|---|---|
| App server | Render free (512 MB) | Render standard (2 GB) |
| Database | Supabase free (500 MB) | Supabase Pro (8 GB) |
| Redis | None | Upstash/Render Redis (256 MB) |
| Storage | Supabase free (1 GB) | Supabase Pro (100 GB) |

### 16.2 Capacity (10 schools, 10K users)

~2000 concurrent WS, ~50K messages/day, ~20 GB DB/year, ~100 GB media/year.

### 16.3 Scaling

- **App server:** Horizontal behind load balancer. WS sticky sessions. Redis pub/sub fan-out.
- **DB:** Supabase PgBouncer pooling. Partition messages by conversation_id hash if > 10M rows.
- **Redis:** Serverless (Upstash). Single instance up to 10K connections.
- **WS:** ~500-1000 concurrent per instance. Add instances as needed.

### 16.4 New Dependencies

| Dependency | Version | Purpose |
|---|---|---|
| `ktor-server-websockets` | 3.4.3 | WS server |
| `ktor-client-websockets` | 3.4.3 | WS client (KMP) |
| Redis client (Lettuce) | latest | Presence + pub/sub |
| SQLDelight | 2.0.0+ | Client local DB |

---

## 17. Monitoring, Logging & Analytics

### 17.1 Metrics

| Metric | Type |
|---|---|
| `ws.active_connections` | Gauge |
| `messages.sent_total` / `messages.failed_total` | Counter |
| `messages.delivery_latency_ms` | Histogram |
| `outbox.queue_size` | Gauge |
| `redis.pubsub_latency_ms` | Histogram |
| `fcm.push_success_rate` | Ratio |

### 17.2 Alerts

| Alert | Condition | Severity |
|---|---|---|
| WS connection spike | > 2x baseline | Warning |
| Send failure rate | > 5% in 5 min | Critical |
| Redis lost | Ping fails | Critical |
| DB pool exhausted | > 90% | Critical |
| FCM failure rate | > 10% in 15 min | Warning |
| Storage quota | > 80% | Warning |

---

## 18. Testing Strategy

### 18.1 Unit Tests

- `sendInConversation` edge cases, idempotency
- WebSocket event parsing, connection registry, presence
- Rate limiter token bucket
- SyncEngine delta sync, outbox flush, conflict resolution
- SQLDelight CRUD, migration, cache eviction

### 18.2 Integration Tests

- Send + receive via WS (two TestHost clients)
- Offline send + reconnect (outbox flush)
- Delta sync with stale cursor
- Edit + delete + tombstone
- Attachment upload + download
- Rate limiting (31st message → 429)
- Multi-tenant isolation (cross-school → 404)
- Cross-role send (parent → parent → 403)

### 18.3 Load Tests

1000 concurrent WS, 100 msg/sec send rate, 10K message delta sync, 100 concurrent uploads.

---

## 19. Deployment & Rollout

### 19.1 Feature Flags

| Flag | Default |
|---|---|
| `WS_ENABLED` | false |
| `MESSAGING_OFFLINE_ENABLED` | false |
| `MESSAGING_SEARCH_ENABLED` | false |
| `MESSAGING_REACTIONS_ENABLED` | false |
| `MESSAGING_TYPING_ENABLED` | false |
| `MESSAGING_PRESENCE_ENABLED` | false |

### 19.2 Rollout Phases

| Phase | Duration | Scope | Features |
|---|---|---|---|
| 1 | 2 weeks | Dev team | REST enhancements (pagination, edit, delete, attachments) |
| 2 | 2 weeks | 1 pilot school | + WebSocket (realtime, typing, presence) |
| 3 | 2 weeks | 5 schools | + Offline-first (local DB, sync, outbox) |
| 4 | 1 week | All schools | + Search, reactions, link previews |

### 19.3 Rollback

| Issue | Action |
|---|---|
| WS crashes | `WS_ENABLED=false` → client falls back to REST polling |
| Redis unavailable | Presence degrades; messaging continues via REST + delta sync |
| Local DB corruption | Clear on next launch; re-sync |
| Migration breaks | Rollback SQL (provided with each migration) |

### 19.4 Version Compatibility

- New server + old client: server handles missing `client_msg_id` (generates one)
- Old server + new client: client detects 404 on WS upgrade → falls back to REST polling
- Mixed: `seq` NULL for old messages; client falls back to `created_at` ordering

---

## 20. Migration Plan

### 20.1 Migration Files

| File | Description |
|---|---|
| `docs/db/migration_020_messaging_seq.sql` | Add seq, client_msg_id, edited_at, deleted_at, reply_to_id to messages; backfill seq |
| `docs/db/migration_021_messaging_threads_ext.sql` | Add is_muted, is_pinned, is_archived, draft_body to message_threads |
| `docs/db/migration_022_message_status.sql` | Create message_status table |
| `docs/db/migration_023_message_attachments.sql` | Create message_attachments table |
| `docs/db/migration_024_message_reactions.sql` | Create message_reactions table |
| `docs/db/migration_025_app_users_last_seen.sql` | Add last_seen_at to app_users |
| `docs/db/migration_026_messages_fts.sql` | Add search_tsv + GIN index |

### 20.2 Execution Order

Run in order 020 → 026 in Supabase SQL Editor. Each migration includes a rollback section. Verify with `SELECT count(*) FROM messages WHERE seq IS NULL` after 020.

### 20.3 Rollback SQL

Each migration file includes:
```sql
-- ROLLBACK:
-- ALTER TABLE messages DROP COLUMN IF EXISTS seq;
-- ...
```

---

## 21. Implementation Roadmap

### Phase 1: REST Enhancements (Weeks 1-3)

1. **DB migrations 020-023** (seq, client_msg_id, status, attachments)
2. **Modify `sendInConversation`** — add idempotency, seq assignment, attachment support
3. **Add pagination** to `conversationMessagesFor` — offset/limit
4. **Add edit/delete endpoints** — `PATCH/DELETE /messages/{id}`
5. **Add attachment upload endpoint** — `POST /messages/attachments` (reuse SupabaseStorage)
6. **Modify client ViewModels + Screens** — pagination, edit/delete UI, attachment picker
7. **Add teacher messages screen** — `TeacherMessagesScreenV2.kt`
8. **Tests:** Unit + integration for all above

### Phase 2: Realtime (Weeks 4-6)

1. **Provision Redis** (Upstash)
2. **Install `ktor-server-websockets`** plugin
3. **Implement `ConnectionRegistry`** + WebSocket server
4. **Implement Redis pub/sub** for multi-instance fan-out
5. **Implement presence service** (Redis TTL keys)
6. **Implement typing indicators** (WS-only, ephemeral)
7. **Client WebSocket client** — connect, hello, heartbeat, reconnection
8. **Modify `notifyMessageRecipient`** — skip FCM if online
9. **Feature flag:** `WS_ENABLED`
10. **Tests:** WS integration tests, presence tests

### Phase 3: Offline-First (Weeks 7-10)

1. **Add SQLDelight** to shared module (all KMP targets)
2. **Implement local DB schema** — thread_local, message_local, outbox_local, draft_local
3. **Implement `SyncEngine`** — initialSync, deltaSync, backgroundSync
4. **Implement `OutboxManager`** — enqueue, flush, retry with backoff
5. **Modify ViewModels** — read from local DB first, write to outbox
6. **Modify Screens** — offline indicators, "sending" status, retry button
7. **WorkManager** (Android) / background task (iOS) for periodic sync
8. **Feature flag:** `MESSAGING_OFFLINE_ENABLED`
9. **Tests:** Sync engine, outbox, conflict resolution, cache eviction

### Phase 4: Advanced Features (Weeks 11-13)

1. **DB migrations 024-026** (reactions, FTS)
2. **Message search** — `GET /messages/search` + UI search bar
3. **Reactions** — add/remove endpoints + UI
4. **Link previews** — URL detection + OpenGraph fetch + UI
5. **Thread preferences** — mute/pin/archive
6. **Draft persistence** — client-only
7. **Rate limiting** — token bucket
8. **Report/block** — endpoints + UI
9. **Feature flags:** `MESSAGING_SEARCH_ENABLED`, `MESSAGING_REACTIONS_ENABLED`
10. **Tests:** Search, reactions, rate limiting, block

### Phase 5: Polish & Scale (Weeks 14-15)

1. **Monitoring** — metrics, alerts, dashboards
2. **Load testing** — 1000 WS connections, 100 msg/sec
3. **Storage quotas** — per-school limits
4. **Signed URLs** for private attachments (P1)
5. **Multi-instance testing** — Redis pub/sub fan-out verification
6. **Documentation** — API docs update, user guide
7. **Performance optimization** — query tuning, index verification

---

## 22. Risk Register & Trade-offs

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| WebSocket connections exhaust server RAM | Medium | High | Monitor `ws.active_connections`, scale horizontally, set max connections per instance |
| Redis single point of failure | Low | High | Upstash has HA. Fallback: presence degrades, messaging continues via REST |
| SQLDelight schema migration fails on client | Low | Medium | Clear local DB + re-sync. Version migrations carefully |
| Message volume exceeds Supabase free tier | Medium | Medium | Monitor storage, upgrade to Pro, set quotas |
| Client clock skew causes ordering issues | Low | Low | Server-authoritative seq and timestamps |
| FCM delivery delays | Medium | Low | WS as primary delivery; FCM is fallback only |
| Attachment storage abuse | Medium | Medium | Per-school quotas, rate limiting, file type validation |

### Trade-offs

| Decision | Trade-off |
|---|---|
| WebSocket vs SSE | WS is bidirectional (typing, acks) but more complex than SSE |
| SQLDelight vs Room KMP | SQLDelight is more mature for KMP but less ergonomic than Room |
| PostgreSQL FTS vs Elasticsearch | FTS is simpler (no new infra) but less powerful for fuzzy search |
| LWW vs CRDT for conflict resolution | LWW is simpler but can lose concurrent edits; CRDTs are complex |
| No E2EE | School ecosystem doesn't require E2EE; adds significant complexity |
| Redis for presence | Adds infra dependency; alternative is DB polling (higher latency) |

---

## 23. Event & Error Catalogue

### 23.1 WebSocket Event Catalogue

| Code | Event | Direction | Payload |
|---|---|---|---|
| WS-001 | `hello` | C→S | `{device_id, last_seqs}` |
| WS-002 | `ping` | C→S | `{}` |
| WS-003 | `pong` | S→C | `{}` |
| WS-004 | `typing` | C→S | `{conversation_id, is_typing}` |
| WS-005 | `typing` | S→C | `{conversation_id, user_id, is_typing}` |
| WS-006 | `ack` | C→S | `{message_id, status:"DELIVERED"}` |
| WS-007 | `read` | C→S | `{conversation_id}` |
| WS-008 | `msg_new` | S→C | `{message, conversation_id}` |
| WS-009 | `msg_status` | S→C | `{message_id, conversation_id, status, user_id}` |
| WS-010 | `msg_edit` | S→C | `{message_id, conversation_id, body, edited_at}` |
| WS-011 | `msg_delete` | S→C | `{message_id, conversation_id}` |
| WS-012 | `presence` | S→C | `{user_id, status, last_seen_at}` |
| WS-013 | `thread_update` | S→C | `{thread}` |
| WS-014 | `presence_query` | C→S | `{user_ids}` |
| WS-015 | `error` | S→C | `{code, message}` |

### 23.2 Error Catalogue

| Code | HTTP | Message | Cause |
|---|---|---|---|
| `MSG_DUPLICATE` | 409 | Message already sent | Duplicate client_msg_id |
| `MSG_NOT_FOUND` | 404 | Message not found | Invalid message_id |
| `MSG_EDIT_WINDOW_EXPIRED` | 403 | Edit window expired | > 24h since send |
| `MSG_NOT_OWNER` | 403 | You can only edit/delete your own messages | sender_id mismatch |
| `THREAD_NOT_FOUND` | 404 | Thread not found | Invalid thread_id or not owner |
| `RECIPIENT_INVALID` | 400 | Recipient not found or not in your school | Invalid recipient_user_id |
| `RATE_LIMITED` | 429 | Too many messages. Retry after N seconds | Rate limit exceeded |
| `ATTACHMENT_TOO_LARGE` | 413 | File too large (max 25 MB) | Size > 25 MB |
| `ATTACHMENT_TYPE_UNSUPPORTED` | 415 | Unsupported file type | MIME not in allowlist |
| `WS_AUTH_FAILED` | 401 | WebSocket authentication failed | Invalid/expired JWT |
| `WS_RATE_LIMITED` | 429 | Too many reconnections | > 10 reconnects/min |
| `BLOCKED_BY_RECIPIENT` | 403 | You have been blocked by this user | Block entry exists |
| `BODY_TOO_LONG` | 400 | Message body exceeds 4096 characters | body.length > 4096 |

---

## 24. Glossary

| Term | Definition |
|---|---|
| **Conversation** | A two-party message exchange. Represented by two `message_threads` rows sharing a `conversation_id`. |
| **Thread** | A single participant's view of a conversation. Owned by `owner_user_id`. |
| **Seq** | Server-assigned monotonic sequence number per conversation. Guarantees ordering. |
| **Client_msg_id** | Client-generated UUID for idempotency. Server dedupes on this. |
| **Outbox** | Client-side queue of messages pending send. Survives network failures. |
| **Delta sync** | Fetching only events since last known cursor (seq) per conversation. |
| **Tombstone** | Soft-delete marker. Message body set to NULL, `deleted_at` timestamp set. |
| **Presence** | Real-time online/offline status. Managed via Redis TTL keys. |
| **ConnectionRegistry** | Server-side map of userId → active WebSocket sessions. |
| **LWW** | Last-Writer-Wins. Conflict resolution strategy using server-authoritative timestamps. |
