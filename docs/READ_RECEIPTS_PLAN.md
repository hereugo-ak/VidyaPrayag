# Read Receipts System — Master Plan

> **Feature #157** from `feature_audit.csv` → evolved into a complete cross-portal message read-receipts system.
> **Status:** PLANNING → BUILD READY
> **Last updated:** 2026-07-01

---

## 0. Executive Summary

The audit identified that the messaging system has a `MessageStatusTable` tracking SENT → DELIVERED → READ states, but the state machine is **broken** — messages are inserted as `SENT` and **never transition**. The `POST /threads/{id}/read` endpoint only updates thread-level `isRead` + `unreadCount` but does NOT update individual `MessageStatusTable` rows to `READ`. The Admin UI already has ✓/✓✓ tick marks wired, but they always show single ✓ (SENT) because the status never changes. The Parent UI has no status ticks at all. Teachers have no dedicated messages screen.

This plan completes the read-receipts system: fixes the state machine, adds the missing `DELIVERED` and `READ` transitions, shows read receipts in all three portals, and adds unread badge counts on portal navigation.

### Design Philosophy

We are building an **operating system for schools**. Every feature must:
- Be **role-aware** (Parent, Teacher, School Admin) with clean separation
- Follow **SOLID + MVVM + Clean Architecture** (per `DEVELOPMENT_STANDARDS.md`)
- Be **offline-resilient** (Room cache for reads, outbox for writes — per offline mode initiative)
- Be **accessible to 25-60 year old parents** (large touch targets, clear language, minimal steps)
- Integrate with existing messaging system — **no silos, no duplication**

---

## 1. Deep System Audit

### 1.1 Existing Messaging Infrastructure

| Layer | File | Status |
|---|---|---|
| **DB Tables** | `Tables.kt:820-831` — `MessageStatusTable` (message_id, conversation_id, user_id, status, created_at) | ✅ Exists, but status never transitions from SENT |
| **DB Tables** | `Tables.kt` — `MessageThreadsTable` (owner_user_id, peer_user_id, conversation_id, unread_count, is_read, last_message, etc.) | ✅ Exists, thread-level read works |
| **DB Tables** | `Tables.kt` — `MessagesTable` (thread_id, conversation_id, sender_id, body, seq, created_at, edited_at, deleted_at, reply_to_id) | ✅ Exists |
| **Server Core** | `MessagingCore.kt:373` — `insertMessageStatus()` inserts SENT status for recipient | ✅ Inserts SENT only |
| **Server Core** | `MessagingCore.kt:694` — `loadMessageStatus()` returns status string | ✅ Works, but always returns SENT |
| **Server Core** | `MessagingCore.kt:139` — `sendInConversation()` calls `insertMessageStatus()` | ✅ Creates SENT row on send |
| **Admin Routes** | `MessagesRouting.kt:388` — `POST /threads/{id}/read` | ⚠️ Updates thread only, NOT MessageStatusTable |
| **Admin Routes** | `MessagesRouting.kt:345` — loads `status` via `loadMessageStatus()` for received messages | ✅ Status field in DTO, but always SENT |
| **Admin Routes** | `MessagesRouting.kt:126` — `MessageDto` has `status: String?` field | ✅ DTO has field |
| **Teacher Routes** | `TeacherMessagesRouting.kt:321` — `POST /threads/{id}/read` | ⚠️ Same issue — thread only, not MessageStatusTable |
| **Teacher Routes** | `TeacherMessagesRouting.kt:279` — loads `status` via `loadMessageStatus()` | ✅ Status field in DTO |
| **Teacher Routes** | `TeacherMessagesRouting.kt:83` — `TeacherMessageDto` has `status: String?` | ✅ DTO has field |
| **Parent Routes** | `ParentMessagesRouting.kt:383` — `POST /threads/{id}/read` | ⚠️ Same issue — thread only, not MessageStatusTable |
| **Parent Routes** | `ParentMessagesRouting.kt:341` — loads `status` via `loadMessageStatus()` | ✅ Status field in DTO |
| **Parent Routes** | `ParentMessagesRouting.kt:83` — `ParentMessageDto` has `status: String?` | ✅ DTO has field |
| **Admin VM** | `MessagesViewModel.kt:150` — `markAsRead()` with optimistic update | ✅ Works for thread-level |
| **Admin UI** | `MessagesScreenV2.kt:534-575` — ✓/✓✓ tick marks for READ/DELIVERED/SENT | ✅ UI exists, but always shows ✓ SENT |
| **Parent UI** | `ParentMessagesScreenV2.kt` — conversation view | ❌ No status ticks on own messages |
| **Teacher UI** | No dedicated messages screen (only `ScheduledMessages`) | ❌ No conversation UI at all |
| **Unread Badge** | Portal tab icons | ❌ No unread count badge on Messages tab |

### 1.2 The Broken State Machine

```
Current flow:
  Sender sends message → insertMessageStatus(status=SENT) → DONE
  Recipient opens thread → POST /threads/{id}/read → updates MessageThreadsTable only
  MessageStatusTable row stays SENT forever ❌

Expected flow:
  Sender sends message → insertMessageStatus(status=SENT)
  Recipient's device receives message → status transitions to DELIVERED
  Recipient opens thread → POST /threads/{id}/read →
    → MessageThreadsTable: isRead=true, unreadCount=0
    → MessageStatusTable: ALL messages in conversation → status=READ ✅
  Sender sees ✓✓ Read on their messages
```

### 1.3 Root Cause

The `POST /threads/{id}/read` endpoint in all three routing files only runs:
```kotlin
MessageThreadsTable.update({
    (MessageThreadsTable.id eq id) and (MessageThreadsTable.ownerUserId eq uid)
}) {
    it[unreadCount] = 0
    it[isRead] = true
    it[updatedAt] = Instant.now()
}
```

It never touches `MessageStatusTable`. The fix is to also bulk-update all `MessageStatusTable` rows for messages in that conversation (where `userId = uid`) to `READ`.

---

## 2. UX Analysis — Read Receipts for All Portals

### 2.1 Core UX Principles

| Principle | Implementation |
|---|---|
| **Familiar pattern** | WhatsApp-style: ✓ Sent, ✓✓ Delivered, ✓✓ Read (blue) |
| **Large touch targets** | Status ticks 14dp, but inside 48dp message bubble tap zone |
| **Clear language** | contentDescription: "Sent", "Delivered", "Read" for screen readers |
| **Timestamp on read** | (Future) "Read at 2:30 PM" on long-press |
| **Unread badge** | Red circle with count on Messages tab icon in all portals |
| **Accessibility** | Status ticks have contentDescription for TalkBack/VoiceOver |
| **Low-end device** | No extra network calls — status is embedded in existing message DTO |

### 2.2 User Journey — Parent Sees Read Receipt

```
1. Parent sends message to teacher
2. Parent sees ✓ (Sent) next to message
3. Teacher's device syncs → status becomes DELIVERED → parent sees ✓✓ (Delivered)
4. Teacher opens conversation → status becomes READ → parent sees ✓✓ (Read, blue)
5. Parent knows teacher has seen the message
```

### 2.3 User Journey — Teacher Sees Read Receipt

```
1. Teacher sends message to parent
2. Teacher sees ✓ (Sent)
3. Parent opens app → status becomes DELIVERED → teacher sees ✓✓
4. Parent opens conversation → status becomes READ → teacher sees ✓✓ (Read)
```

### 2.4 Edge Cases

| Scenario | Handling |
|---|---|
| Recipient has multiple devices | First device to open → READ, all devices reflect via next sync |
| Message is edited | Status ticks remain — edit only updates body + editedAt |
| Message is deleted | Status ticks hidden (isDeleted check already in UI) |
| Self/system thread (no peer) | No status ticks — `status` is null for own messages |
| Offline read | Queue mark-read in outbox, sync when online. Sender sees READ after sync |
| Bulk mark-read (open thread with 50 unread) | Single UPDATE statement: all SENT/DELIVERED → READ in one transaction |
| Recipient reads via push notification (doesn't open thread) | Not counted as READ — only opening the thread counts |

---

## 3. Database Schema

### 3.1 No New Tables Needed

`MessageStatusTable` already exists with the right schema:
```kotlin
object MessageStatusTable : UUIDTable("message_status", "id") {
    val messageId      = uuid("message_id")
    val conversationId = uuid("conversation_id")
    val userId         = uuid("user_id")           // recipient
    val status         = varchar("status", 16)     // SENT | DELIVERED | READ
    val createdAt      = timestamp("created_at")
    init {
        uniqueIndex("ux_message_status_msg_user", messageId, userId)
        index("idx_msg_status_conv_user", isUnique = false, conversationId, userId)
    }
}
```

### 3.2 Optional Enhancement: `readAt` Timestamp

Add `read_at` column to track when the message was read (for future "Read at 2:30 PM" UI):

```sql
ALTER TABLE message_status ADD COLUMN read_at TIMESTAMP NULL;
```

This is **additive** and **nullable** — no breaking changes.

### 3.3 Migration Strategy

- **Migration N → N+1**: Add `read_at` column to `message_status` table (nullable)
- Use Exposed `SchemaUtils.create()` existing pattern — Exposed auto-adds missing columns on some DBs
- No destructive changes — all additive
- Existing `SENT` rows remain `SENT` until recipient opens thread (lazy fix on next read)

---

## 4. API Design

### 4.1 Modified Endpoints (No New Routes)

| Method | Path | Change |
|---|---|---|
| `POST` | `/api/v1/school/messages/threads/{id}/read` | **Also** update `MessageStatusTable` → `READ` for all messages in conversation |
| `POST` | `/api/v1/teacher/messages/threads/{id}/read` | Same fix |
| `POST` | `/api/v1/parent/messages/threads/{id}/read` | Same fix |
| `GET` | `/api/v1/school/messages/threads/{id}/messages` | Already returns `status` — no change needed |
| `GET` | `/api/v1/teacher/messages/threads/{id}/messages` | Already returns `status` — no change needed |
| `GET` | `/api/v1/parent/messages/threads/{id}/messages` | Already returns `status` — no change needed |

### 4.2 New Endpoint: Unread Count

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/{role}/messages/unread-count` | Returns total unread message count for badge display |

```kotlin
@Serializable
data class UnreadCountDto(
    val unreadCount: Int
)
```

### 4.3 New Endpoint: Mark Delivered (Future — Optional)

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/{role}/messages/threads/{id}/delivered` | Transition SENT → DELIVERED when recipient's device receives message |

> **Note:** DELIVERED transition is optional for Phase 1. The critical fix is SENT → READ. DELIVERED can be added later without breaking anything (Open/Closed — just add a new endpoint + transition).

---

## 5. End-to-End Architecture Flow

### 5.1 Sender Sends Message → Recipient Reads

```
Sender (Admin) → POST /api/v1/school/messages
  ↓
Server: sendInConversation() → insertMessage + insertMessageStatus(status=SENT)
  ↓
Server: Notify.toUser(recipientId, "New message from {sender}")
  ↓
Recipient (Parent) → opens Messages tab
  ↓
ParentMessageViewModel.loadThreads() → GET /api/v1/parent/messages/threads
  ↓
Server: returns threads with unreadCount > 0 for new message
  ↓
Parent sees unread badge on thread
  ↓
Parent taps thread → openThread() → GET /api/v1/parent/messages/threads/{id}/messages
  ↓
Server: returns messages with status=SENT for recipient's messages
  ↓
ParentMessageViewModel automatically calls markAsRead()
  ↓
POST /api/v1/parent/messages/threads/{id}/read
  ↓
Server: NEW FIX — atomic transaction:
  1. MessageThreadsTable: isRead=true, unreadCount=0
  2. MessageStatusTable: UPDATE SET status='READ', read_at=now
     WHERE conversation_id={convId} AND user_id={uid} AND status IN ('SENT','DELIVERED')
  ↓
Server: return success
  ↓
Next time sender loads conversation: GET /api/v1/school/messages/threads/{id}/messages
  ↓
Server: loadMessageStatus() returns 'READ' for those messages
  ↓
Sender sees ✓✓ Read (blue ticks) on their messages
```

### 5.2 Unread Badge Count Flow

```
Portal loads → GET /api/v1/{role}/messages/unread-count
  ↓
Server: SELECT SUM(unreadCount) FROM message_threads WHERE owner_user_id={uid} AND unreadCount > 0
  ↓
Returns: { "unreadCount": 5 }
  ↓
UI: shows red badge with "5" on Messages tab icon
  ↓
User opens Messages → badge clears (threads are marked read)
```

---

## 6. Implementation Phases

### Phase 1 — Server: Fix Read State Machine (3 steps)

| Step | File | Description |
|---|---|---|
| 1.1 | `MessagingCore.kt` | Add `markConversationRead(userId, conversationId)` function: bulk-update `MessageStatusTable` → `READ` + `read_at = now` for all messages in conversation where `userId = recipient` and `status != READ` |
| 1.2 | `MessagesRouting.kt` | In `POST /threads/{id}/read`: after updating `MessageThreadsTable`, also call `markConversationRead()`. Need to resolve `conversationId` from thread row first. |
| 1.3 | `TeacherMessagesRouting.kt` | Same fix as 1.2 for teacher endpoint |
| 1.4 | `ParentMessagesRouting.kt` | Same fix as 1.2 for parent endpoint |

### Phase 2 — Server: Unread Count Endpoint (2 steps)

| Step | File | Description |
|---|---|---|
| 2.1 | `MessagingCore.kt` | Add `getUnreadCount(userId): Int` — `SUM(unreadCount) WHERE ownerUserId = userId AND unreadCount > 0` |
| 2.2 | `MessagesRouting.kt`, `TeacherMessagesRouting.kt`, `ParentMessagesRouting.kt` | Add `GET /messages/unread-count` endpoint to each portal |

### Phase 3 — Server: Delivered Transition (Optional, 2 steps)

| Step | File | Description |
|---|---|---|
| 3.1 | `MessagingCore.kt` | Add `markConversationDelivered(userId, conversationId)` — bulk-update `SENT → DELIVERED` |
| 3.2 | All routing files | Add `POST /threads/{id}/delivered` endpoint. Called by recipient's device when messages are received (not yet opened). |

> Phase 3 is **optional** — can be deferred. The critical fix is Phase 1 (SENT → READ).

### Phase 4 — Shared Layer (4 steps)

| Step | File | Description |
|---|---|---|
| 4.1 | `MessageModels.kt` (or existing message models) | Add `UnreadCountDto` if not already present |
| 4.2 | `MessagesApi.kt` (or existing API) | Add `getUnreadCount(token): NetworkResult<UnreadCountDto>` |
| 4.3 | `MessagesRepository.kt` / `MessagesRepositoryImpl.kt` | Add `getUnreadCount()` method |
| 4.4 | `Koin.kt` | Wire any new classes (likely none — existing repo/api extended) |

### Phase 5 — Admin UI (3 steps)

| Step | File | Description |
|---|---|---|
| 5.1 | `MessagesScreenV2.kt` | Status ticks already exist (✓/✓✓). Verify they work now that server returns READ. May need to refresh conversation after markAsRead to pick up status changes. |
| 5.2 | `MessagesViewModel.kt` | After `markAsRead()` success, re-fetch conversation messages to update status ticks. Add `unreadCount` StateFlow for badge. |
| 5.3 | `SchoolPortalV2.kt` | Show unread badge count on Messages tab icon. Poll or fetch on portal load. |

### Phase 6 — Parent UI (3 steps)

| Step | File | Description |
|---|---|---|
| 6.1 | `ParentMessagesScreenV2.kt` | Add ✓/✓✓ status ticks on own messages (mirror admin `MessagesScreenV2.kt:534-575` pattern). contentDescription for accessibility. |
| 6.2 | `ParentMessageViewModel.kt` | After `markAsRead()` success, re-fetch conversation messages. Add `unreadCount` StateFlow. |
| 6.3 | `ParentPortalV2.kt` | Show unread badge count on Messages/Conversations tab icon. |

### Phase 7 — Teacher UI (4 steps)

| Step | File | Description |
|---|---|---|
| 7.1 | `TeacherMessagesScreenV2.kt` | **NEW FILE** — conversation UI mirroring admin pattern. Thread list + conversation detail + status ticks. |
| 7.2 | `TeacherMessageViewModel.kt` | **NEW FILE** — Teacher VM for messages (load threads, open conversation, mark read, send reply). |
| 7.3 | `Koin.kt` | Register teacher message VM. |
| 7.4 | `TeacherPortalV2.kt` | Add `Messages` overlay to `TeacherOverlay` enum. Add tab/icon for messages. Show unread badge. |

---

## 7. SOLID Compliance Checklist

| Principle | How Applied |
|---|---|
| **S** — Single Responsibility | `markConversationRead()` only updates MessageStatusTable. `getUnreadCount()` only queries counts. VMs only manage state. |
| **O** — Open/Closed | DELIVERED transition added by new endpoint, not by editing existing read logic. New teacher messages screen extends UI without modifying admin/parent screens. |
| **L** — Liskov Substitution | Repository impl fully substitutes interface. Any future Room-backed impl too. |
| **I** — Interface Segregation | VMs call only methods they need. Unread count is a separate method from thread loading. |
| **D** — Dependency Inversion | VMs depend on repository interface via Koin. API depends on injected HttpClient. |

## 8. MVVM Compliance Checklist

| Layer | Rule | Compliance |
|---|---|---|
| **Model** | `@Serializable` data classes, no logic | ✅ `UnreadCountDto`, existing message DTOs |
| **Repository** | Interface + Impl, coordinates remote + local | ✅ Extended existing repository |
| **ViewModel** | Exposes `StateFlow` only, no Compose imports | ✅ `*MessageViewModel` with unreadCount StateFlow |
| **View** | Stateless composables, receives state + callbacks | ✅ `*MessagesScreenV2.kt` |
| **DI** | All wired in `Koin.kt` | ✅ New VMs registered as factory |

---

## 9. Security & Validation

| Concern | Mitigation |
|---|---|
| User marks someone else's thread as read | Server: verify `ownerUserId == authenticated user` (existing check) |
| User marks messages they didn't receive as read | Server: `MessageStatusTable.userId == authenticated user` in WHERE clause |
| Unread count leaks other users' data | Server: `WHERE ownerUserId = authenticated user` |
| Delivered spoofing | (Future) Device-level attestation — out of scope for now |
| Bulk update performance | Single UPDATE with WHERE clause — no row-by-row iteration |

---

## 10. Testing Strategy

### 10.1 Server Tests

```kotlin
// ReadReceiptsTest.kt (or extend existing messaging tests)
- "POST /threads/{id}/read updates MessageStatusTable to READ"
- "POST /threads/{id}/read only updates messages for authenticated user"
- "POST /threads/{id}/read does not update already-READ messages"
- "GET /messages/unread-count returns correct total"
- "GET /messages/unread-count returns 0 when all read"
- "Sender sees READ status after recipient marks read"
- "Self/system thread has no status ticks (status is null for own messages)"
- "Mark read is idempotent — calling twice does not error"
```

### 10.2 Shared Tests

```kotlin
// Extend existing MessagesRepositoryTest.kt
- "Repository delegates getUnreadCount to API"
- "Repository handles NetworkResult.Error for unread count"
```

### 10.3 Build Verification

```bash
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot
./gradlew :shared:compileKotlinJvm :shared:jvmTest
./gradlew :shared:compileDevDebugKotlinAndroid :composeApp:compileDevDebugKotlinAndroid
```

---

## 11. Loop Review

### Review Round 1 — Initial Audit

| Check | Finding | Status |
|---|---|---|
| Is MessageStatusTable used? | Yes — inserted as SENT on send, loaded on conversation open | ✅ Exists |
| Does status transition? | No — SENT forever, never transitions to DELIVERED or READ | ❌ Broken |
| Does POST /threads/{id}/read fix this? | No — only updates thread-level isRead, not per-message status | ❌ Root cause |
| Do DTOs have status field? | Yes — all three portals (Admin, Teacher, Parent) have `status: String?` | ✅ Ready |
| Does Admin UI show ticks? | Yes — ✓/✓✓ for SENT/DELIVERED/READ | ✅ UI ready |
| Does Parent UI show ticks? | No — no status display in ParentMessagesScreenV2 | ❌ Missing |
| Does Teacher have messages UI? | No — only ScheduledMessages | ❌ Missing |
| Is there an unread count endpoint? | No — no badge count API | ❌ Missing |
| Is the fix minimal? | Yes — one new function + update 3 endpoints + add status ticks to parent UI | ✅ Minimal |

### Review Round 2 — Enhancement Suggestions

| # | Suggestion | Priority | Status |
|---|---|---|---|
| 1 | **read_at timestamp**: Track when message was read | Medium | ✅ Added to schema (§3.2) |
| 2 | **Unread badge count**: Show count on Messages tab | High | ✅ Added to API (§4.2) + Phase 2 |
| 3 | **DELIVERED transition**: SENT → DELIVERED when received | Low | ✅ Added as Phase 3 (optional) |
| 4 | **Teacher messages screen**: Full conversation UI | High | ✅ Added as Phase 7 |
| 5 | **Refresh after mark-read**: Re-fetch conversation to update ticks | High | ✅ Added to Phase 5.2, 6.2 |
| 6 | **"Read at HH:mm" on long-press**: Timestamp display | Low | Future — read_at column enables this |

### Review Round 3 — Future Enhancement Suggestions

| # | Suggestion | Priority | Phase |
|---|---|---|---|
| 1 | **Typing indicators**: Show "typing..." when other party is composing | Medium | Future |
| 2 | **Read receipts for group chats**: When class broadcast is read by N parents | Medium | Future |
| 3 | **Read receipt settings**: Parent can disable read receipts (privacy) | Low | Future |
| 4 | **Per-message read timestamp in UI**: "Read at 2:30 PM" below ticks | Low | Future (enabled by read_at) |
| 5 | **Push notification on read**: "Your message was read by {name}" | Low | Future |

### Review Round 4 — Integration Audit

| Integration Point | Status | Notes |
|---|---|---|
| `MessageStatusTable` | ✅ Reused | No new table — fix existing state machine |
| `MessageThreadsTable` | ✅ Reused | Thread-level isRead continues to work |
| `MessagingCore.kt` | ✅ Extended | New `markConversationRead()` function |
| `Notify.kt` | ✅ Integrated | Existing notification on send — no change needed |
| `Koin.kt` | ✅ Wired | New teacher VM registered |
| `MessagesScreenV2.kt` | ✅ Enhanced | Status ticks already exist — will now show correct state |
| `ParentMessagesScreenV2.kt` | ✅ Enhanced | Add status ticks (mirror admin pattern) |
| `TeacherPortalV2.kt` | ✅ Extended | New Messages overlay |
| `ParentPortalV2.kt` | ✅ Extended | Unread badge on Messages tab |
| `SchoolPortalV2.kt` | ✅ Extended | Unread badge on Messages tab |

---

## 12. God-Mode Agent Prompt

> Paste the block below as the system/first message for an AI agent (GLM 5.2, Devin, etc.) to execute this plan through a strict Plan→Build→Test→Review→Iterate loop.

```text
See the God-Mode prompt in the chat response — it is provided separately for easy copy-paste.
```

---

## 13. Build & Audit Log

> This section is updated as implementation progresses. Each phase completion appends a summary.

### Phase 1 — Server: Fix Read State Machine
- **Status:** NOT STARTED
- **Started:** —
- **Completed:** —
- **Audit:** —

### Phase 2 — Server: Unread Count Endpoint
- **Status:** NOT STARTED
- **Started:** —
- **Completed:** —
- **Audit:** —

### Phase 3 — Server: Delivered Transition (Optional)
- **Status:** NOT STARTED
- **Started:** —
- **Completed:** —
- **Audit:** —

### Phase 4 — Shared Layer
- **Status:** NOT STARTED
- **Started:** —
- **Completed:** —
- **Audit:** —

### Phase 5 — Admin UI
- **Status:** NOT STARTED
- **Started:** —
- **Completed:** —
- **Audit:** —

### Phase 6 — Parent UI
- **Status:** NOT STARTED
- **Started:** —
- **Completed:** —
- **Audit:** —

### Phase 7 — Teacher UI
- **Status:** NOT STARTED
- **Started:** —
- **Completed:** —
- **Audit:** —

---

## 14. References

- `feature_audit.csv` line 157 — Read Receipts feature entry
- `server/.../db/Tables.kt:820-831` — `MessageStatusTable` definition
- `server/.../feature/school/MessagingCore.kt:373` — `insertMessageStatus()` (SENT)
- `server/.../feature/school/MessagingCore.kt:694` — `loadMessageStatus()`
- `server/.../feature/school/MessagesRouting.kt:388` — Admin `POST /threads/{id}/read`
- `server/.../feature/teacher/TeacherMessagesRouting.kt:321` — Teacher `POST /threads/{id}/read`
- `server/.../feature/user/ParentMessagesRouting.kt:383` — Parent `POST /threads/{id}/read`
- `composeApp/.../ui/v2/screens/school/MessagesScreenV2.kt:534-575` — Admin status ticks UI
- `composeApp/.../ui/v2/screens/parent/ParentMessagesScreenV2.kt` — Parent messages (no ticks)
- `DEVELOPMENT_STANDARDS.md` — Architecture conventions
- `MESSAGING_SYSTEM_SPEC.md` — Original messaging spec
