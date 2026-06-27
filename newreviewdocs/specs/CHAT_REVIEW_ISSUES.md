# VidyaPrayag Chat Feature — Comprehensive Issue Report

> **Generated:** 2026-06-27
> **Reviewers:** Senior Android Dev, Android Architect, Senior QA, Lead Product Manager
> **Scope:** Full messaging stack — backend (`MessagingCore.kt`, `MessagesRouting.kt`, `TeacherMessagesRouting.kt`, `ParentMessagesRouting.kt`), client models (`MessageModels.kt`, `ParentMessageModels.kt`), API layer (`MessagesApi.kt`), ViewModels (`MessagesViewModel.kt`, `ParentMessageViewModel.kt`), UI screens (`MessagesScreenV2.kt`, `ParentMessagesScreenV2.kt`, `TeacherPortalV2.kt`)
> **Reference spec:** `MESSAGING_SYSTEM_SPEC.md`

---

## Table of Contents

1. [Issue Summary](#1-issue-summary)
2. [P0 — Bugs & Data Integrity Issues](#2-p0--bugs--data-integrity-issues)
3. [P1 — UX & Performance Issues](#3-p1--ux--performance-issues)
4. [P2 — Missing Features (Spec'd but not implemented)](#4-p2--missing-features-specd-but-not-implemented)
5. [P3 — Polish & Consistency](#5-p3--polish--consistency)
6. [Fix Priority Matrix](#6-fix-priority-matrix)
7. [Recommended Fix Order](#7-recommended-fix-order)

---

## 1. Issue Summary

| Priority | Count | Category |
|---|---|---|
| **P0** | 7 | Bugs, data corruption, client-server contract mismatch |
| **P1** | 13 | UX degradation, performance, inconsistency |
| **P2** | 10 | Spec'd features with no client-side implementation |
| **P3** | 7 | Minor polish items |
| **Total** | **37** | |

---

## 2. P0 — Bugs & Data Integrity Issues

### P0-1: Race condition in `nextSeqForConversation`

**File:** `server/.../feature/school/MessagingCore.kt:296-302`

**Code:**
```kotlin
private fun nextSeqForConversation(conversationId: UUID): Int {
    val maxSeq = MessagesTable.select(MessagesTable.seq.max())
        .where { MessagesTable.conversationId eq conversationId }
        .firstOrNull()
        ?.let { it[MessagesTable.seq.max()] as? Int } ?: 0
    return maxSeq + 1
}
```

**Problem:** `MAX(seq) + 1` is not atomic. Two concurrent sends in the same conversation can read the same max value and both get the same `seq`, breaking the monotonic ordering guarantee from §7.1 of the spec. Under load (e.g., a teacher broadcast to a class), this is a realistic scenario.

**Impact:** Duplicate sequence numbers → message ordering corruption → delta sync delivers messages in wrong order → client displays messages out of sequence.

**Fix options (in order of preference):**
1. **Postgres sequence per conversation:** Create a `conversation_seq` table with `(conversation_id, next_val)`. Use `INSERT ... ON CONFLICT DO UPDATE SET next_val = next_val + 1 RETURNING next_val` — atomic at the DB level.
2. **Row-level lock:** `SELECT ... FOR UPDATE` on the conversation's thread row before computing max+1. Slower but simpler.
3. **Advisory lock:** `SELECT pg_advisory_xact_lock(hashtext(conversation_id::text))` — locks within the transaction, no extra table.

**Severity:** Critical — silent data corruption under concurrency.

---

### P0-2: `deleteMessage` sets body to `""` instead of `NULL`

**File:** `server/.../feature/school/MessagingCore.kt:623-626`

**Code:**
```kotlin
MessagesTable.update({ MessagesTable.id eq messageId }) {
    it[deletedAt] = now
    it[body] = ""
}
```

**Problem:** The spec (§15.4) says deleted messages should have `body = NULL` (tombstone). Setting `""` means:
- Clients cannot distinguish a deleted message from an intentionally empty one.
- The UI will render a blank bubble instead of "This message was deleted."
- Full-text search (`to_tsvector(coalesce(body, ''))`) will index empty strings, polluting search results.

**Fix:**
```kotlin
MessagesTable.update({ MessagesTable.id eq messageId }) {
    it[deletedAt] = now
    it[body] = null  // or use it[MessagesTable.body] = org.jetbrains.exposed.sql.Op.null
}
```

If the Exposed column is non-nullable, a migration is needed to make `body` nullable, or use a sentinel value like `"<deleted>"` consistently. However, `NULL` is the correct design per the spec.

**Severity:** Critical — breaks tombstone semantics and UI display.

---

### P0-3: `conversationMessagesFor` OR clause can match wrong messages

**File:** `server/.../feature/school/MessagingCore.kt:480-486`

**Code:**
```kotlin
val query = MessagesTable.selectAll().where {
    (MessagesTable.conversationId eq convId) or (MessagesTable.threadId eq threadId)
}
```

**Problem:** The `OR (threadId eq threadId)` is a legacy fallback for messages written before `conversation_id` was added. However:
- `threadId` is the **owner's** thread row ID.
- A message written by the peer has `thread_id` = the **peer's** thread row ID, not the requester's.
- If a legacy message's `thread_id` happens to match a different conversation's thread row, this OR will pull in messages from the wrong conversation.

The correct fallback should be:
```kotlin
(MessagesTable.conversationId eq convId) or
    ((MessagesTable.conversationId isNull) and (MessagesTable.threadId eq threadId))
```

This ensures the legacy fallback only applies when `conversation_id` is genuinely NULL (pre-migration rows), not when it belongs to a different conversation.

**Impact:** Cross-conversation message leakage in rare edge cases with legacy data. Violates tenant isolation and privacy.

**Severity:** Critical — potential data leakage between conversations.

---

### P0-4: `notifyMessageRecipient` deep link hardcoded to `"parent/messages"`

**File:** `server/.../feature/school/MessagingCore.kt:527`

**Code:**
```kotlin
deepLink = "parent/messages",
```

**Problem:** When an admin or teacher receives a message, the push notification deep-links them to the **parent** messages screen. An admin tapping the notification would land on a parent endpoint they can't access (role-gated), resulting in a 403 or a broken screen.

**Fix:** Resolve the recipient's role and construct the deep link accordingly:
```kotlin
val recipient = resolveMessagingUser(recipientId)
val deepLink = when (recipient?.role) {
    "parent" -> "parent/messages"
    "teacher" -> "teacher/messages"
    "admin", "school_admin", "super_admin" -> "school/messages"
    else -> "messages"
}
```

**Severity:** High — broken notification navigation for non-parent users.

---

### P0-5: Client domain models out of sync with server DTOs

**Files:**
- `shared/.../feature/admin/domain/model/MessageModels.kt:79-86` (`Message` class)
- `shared/.../feature/admin/domain/model/MessageModels.kt:92-97` (`ThreadMessagesResponse` class)
- `shared/.../feature/parent/domain/model/ParentMessageModels.kt:48-55` (`ParentMessageDto` class)
- `shared/.../feature/parent/domain/model/ParentMessageModels.kt:58-68` (`ParentThreadMessagesData` class)

**Problem:** The server now returns these Phase 1 fields in message responses:

| Field | Server `MessageDto` | Client `Message` / `ParentMessageDto` |
|---|---|---|
| `seq` | ✅ | ❌ Missing |
| `status` | ✅ | ❌ Missing |
| `edited_at` | ✅ | ❌ Missing |
| `deleted_at` | ✅ | ❌ Missing |
| `reply_to_id` | ✅ | ❌ Missing |
| `attachments` | ✅ | ❌ Missing |
| `has_more` | ✅ (in response) | ❌ Missing |
| `total_count` | ✅ (in response) | ❌ Missing |

Because `Json { ignoreUnknownKeys = true }` is configured, these fields are **silently dropped** during deserialization. This means:
- **No attachment rendering** — images/documents sent in messages are invisible
- **No edit/delete display** — no "edited" label, no "This message was deleted" text
- **No message status** — no sent/delivered/read tick marks
- **No pagination support** — `hasMore`/`totalCount` lost, client can't know if there are older messages

**Fix:** Add all missing fields to the client domain models with `@SerialName` annotations matching the server JSON keys. Use nullable types with defaults for backward compatibility.

**Severity:** Critical — the entire Phase 1 backend work is invisible to the client.

---

### P0-6: `SendMessageRequest` missing Phase 1 fields

**File:** `shared/.../feature/admin/domain/model/MessageModels.kt:55-63`

**Code:**
```kotlin
data class SendMessageRequest(
    @SerialName("thread_id") val threadId: String? = null,
    @SerialName("sender_name") val senderName: String? = null,
    @SerialName("sender_role") val senderRole: String? = null,
    @SerialName("sender_image_url") val senderImageUrl: String? = null,
    @SerialName("icon_name") val iconName: String? = null,
    @SerialName("recipient_user_id") val recipientUserId: String? = null,
    val body: String
)
```

**Missing fields:**
- `client_msg_id` — for idempotency (prevents duplicate messages on network retry)
- `reply_to_id` — for reply-quoting
- `attachments` — for sending media in messages

**Impact:** Without `client_msg_id`, a network timeout followed by a user retry will create **duplicate messages**. The server has idempotency dedup ready, but the client never sends the key.

Same issue exists in `shared/.../feature/parent/domain/model/ParentMessageModels.kt:71-79` (`ParentSendMessageRequest`).

**Fix:** Add the three fields to both request models. Generate `clientMsgId` as `UUID.randomUUID().toString()` on the client before each send.

**Severity:** Critical — duplicate messages on flaky networks (common on mobile).

---

### P0-7: `MessagesApi.kt` missing pagination, edit, delete, attachment endpoints

**File:** `shared/.../feature/admin/data/remote/MessagesApi.kt:54-59`

**Code:**
```kotlin
suspend fun getThreadMessages(
    token: String,
    threadId: String
): NetworkResult<ApiResponse<ThreadMessagesResponse>> = safeApiCall {
    client.get(getUrl("api/v1/school/messages/threads/$threadId/messages"))
}
```

**Missing:**
1. `offset` / `limit` query parameters not passed — server defaults to 50 but client can't request more
2. No `patchMessage(token, messageId, body)` method for `PATCH /messages/{id}`
3. No `deleteMessage(token, messageId, scope)` method for `DELETE /messages/{id}`
4. No `uploadAttachment(token, bytes, fileName, mimeType, conversationId, attachmentType)` method for `POST /messages/attachments`

The server endpoints are implemented and tested, but the client has no way to call them.

**Fix:** Add all four methods to `MessagesApi.kt`. For pagination, add `offset: Int = 0, limit: Int = 50` parameters and append as query params.

**Severity:** Critical — Phase 1 features are unreachable from the client.

---

## 3. P1 — UX & Performance Issues

### P1-1: Admin message list uses `verticalScroll` instead of `LazyColumn`

**File:** `composeApp/.../ui/v2/screens/school/MessagesScreenV2.kt:245-256`

**Code:**
```kotlin
Column(
    Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 12.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
) {
    conversation.messages.forEach { msg -> MessageBubble(msg) }
}
```

**Problem:** `verticalScroll` + `forEach` composes **all** message bubbles eagerly. With 200+ messages, this causes:
- High memory allocation (all composables in tree)
- Jank during scroll (no lazy disposal)
- Potential OOM on low-end Android devices (1-2 GB RAM)

The parent screen correctly uses `LazyColumn` with `items()` — the admin screen should match.

**Fix:** Replace `Column { forEach }` with:
```kotlin
LazyColumn(
    state = listState,
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
) {
    items(conversation.messages, key = { it.id }) { msg -> MessageBubble(msg) }
}
```

Add `rememberLazyListState()` and the auto-scroll `LaunchedEffect` (see P1-9).

**Severity:** High — performance degradation that worsens with conversation length.

---

### P1-2: Admin thread list also uses `verticalScroll` + `forEachIndexed`

**File:** `composeApp/.../ui/v2/screens/school/MessagesScreenV2.kt:148-186`

**Code:**
```kotlin
Column(
    modifier.verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 24.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
) {
    VButton(...)
    VStateHost(...) {
        VCard {
            state.threads.forEachIndexed { i, t ->
                if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(c.border1))
                ThreadRow(thread = t, onClick = { onOpenThread(t.id) })
            }
        }
    }
}
```

**Problem:** Same as P1-1 — all thread rows composed eagerly. With 100+ threads (realistic for an admin managing a large school), this is wasteful.

**Fix:** Replace with `LazyColumn` + `items()`. Move the "New message" button into a `item {}` slot at the top. Move the `VStateHost` wrapper outside the `LazyColumn` or use a `when` block to switch between loading/error/empty/list states.

**Severity:** Medium — degrades with thread count, less severe than P1-1 since thread lists are typically smaller.

---

### P1-3: No optimistic send — UI flicker on slow networks

**Files:**
- `shared/.../feature/admin/presentation/MessagesViewModel.kt:355-359`
- `shared/.../feature/parent/presentation/ParentMessageViewModel.kt:180-184`

**Code (admin):**
```kotlin
is NetworkResult.Success -> {
    _conversation.value = _conversation.value.copy(isSending = false)
    reloadConversation()  // full network round-trip to see your own message
    refresh()
}
```

**Problem:** When the user taps send:
1. `isSending = true` → spinner appears on send button
2. HTTP POST to server (network round-trip: 200ms-3s)
3. On success: `isSending = false` + `reloadConversation()` (another GET request)
4. Only after the GET completes does the message appear in the list

The user stares at a spinner for 400ms-6s with no visible feedback that their message was sent. WhatsApp shows the message **instantly** with a clock icon, then swaps to checkmarks on server confirm.

**Fix (optimistic send pattern):**
```kotlin
fun sendReply(body: String) {
    val threadId = _conversation.value.threadId ?: return
    if (body.isBlank()) return

    // 1. Create a provisional message and add it to the list immediately
    val provisionalMsg = Message(
        id = "provisional-${UUID.randomUUID()}",
        body = body,
        isMine = true,
        createdAt = Instant.now().toString(),
        time = "Now",
        status = "SENDING",  // new field
    )
    _conversation.value = _conversation.value.copy(
        messages = _conversation.value.messages + provisionalMsg,
        isSending = false,  // no spinner — message is visible
    )

    // 2. Send to server
    viewModelScope.launch {
        val token = preferenceRepository.getUserToken().first() ?: return@launch
        val request = SendMessageRequest(
            threadId = threadId,
            body = body,
            clientMsgId = UUID.randomUUID().toString(),  // idempotency
        )
        when (val r = messagesRepository.sendMessage(token, request)) {
            is NetworkResult.Success -> {
                // 3. Replace provisional message with real one (or just reload)
                reloadConversation()
                refresh()
            }
            is NetworkResult.Error -> {
                // 4. Mark provisional message as FAILED
                _conversation.value = _conversation.value.copy(
                    messages = _conversation.value.messages.map {
                        if (it.id == provisionalMsg.id) it.copy(status = "FAILED") else it
                    },
                    error = r.message,
                )
            }
            is NetworkResult.ConnectionError -> { /* same as Error */ }
        }
    }
}
```

**Severity:** High — directly impacts perceived app quality. Users on 2G/3G will think the app is frozen.

---

### P1-4: Polling at 10-second intervals — battery drain

**Files:**
- `shared/.../feature/parent/presentation/ParentMessageViewModel.kt:68` (`POLL_INTERVAL_MS = 10_000L`)
- `shared/.../feature/admin/presentation/MessagesViewModel.kt:93` (`POLL_INTERVAL_MS = 10_000L`)

**Problem:** Both ViewModels poll every 10 seconds while a conversation is open. On mobile:
- Each poll is a full HTTP GET (TLS handshake reuse, JSON parse, DB query)
- 6 requests/minute = 360/hour while a conversation is open
- Significant battery drain (radio wake per request)
- Unnecessary server load (6 requests/min × concurrent users)

**Fix (short-term):** Increase to 30s with adaptive backoff:
```kotlin
private var currentInterval = 30_000L
private val maxInterval = 120_000L

private fun startPolling() {
    stopPolling()
    pollJob = viewModelScope.launch {
        while (isActive) {
            delay(currentInterval)
            if (_state.value.openThreadId != null && !_state.value.sending) {
                val hadNew = reloadConversation()
                if (hadNew) currentInterval = 30_000L  // reset on activity
                else currentInterval = (currentInterval * 2).coerceAtMost(maxInterval)
            }
        }
    }
}
```

**Fix (long-term):** Replace polling with WebSocket (per spec §5) or Server-Sent Events.

**Severity:** Medium — battery impact is cumulative; users will notice over a session.

---

### P1-5: `reloadConversation()` silently swallows errors

**File:** `shared/.../feature/parent/presentation/ParentMessageViewModel.kt:162-165`

**Code:**
```kotlin
is NetworkResult.Error -> {}
is NetworkResult.ConnectionError -> {}
```

**Problem:** During polling, if the network drops, errors are silently ignored. The user sees stale messages with no indication that updates aren't coming. They may believe they're seeing real-time messages when in fact the last update was 5 minutes ago.

**Fix:** Track a `lastSyncAt` timestamp and a `syncError` flag:
```kotlin
is NetworkResult.Error -> {
    _state.update { it.copy(syncError = "Updates paused: ${r.message}") }
}
is NetworkResult.ConnectionError -> {
    _state.update { it.copy(syncError = "Connection lost — showing cached messages") }
}
```

Show a subtle banner: "⚠ Connection lost — showing cached messages" when `syncError != null`.

**Severity:** Medium — misleading UX, user doesn't know data is stale.

---

### P1-6: Double `loadThreads()` call in parent `composeNew`

**File:** `shared/.../feature/parent/presentation/ParentMessageViewModel.kt:287-289`

**Code:**
```kotlin
loadThreads()                                    // call 1
if (newThreadId != null) {
    openThread(newThreadId, recipientName)        // openThread calls loadThreads() internally (call 2)
}
```

**Problem:** `openThread` (line 124) calls `loadThreads()` after successfully loading messages. So the thread list is fetched twice in rapid succession — two identical HTTP GETs within ~100ms.

**Fix:** Remove the explicit `loadThreads()` call before `openThread`. Let `openThread` handle it:
```kotlin
if (newThreadId != null) {
    openThread(newThreadId, recipientName)  // this calls loadThreads() internally
} else {
    loadThreads()  // only call if we're NOT opening a thread
}
```

**Severity:** Low — wasteful but not user-visible. Still, it's an easy fix.

---

### P1-7: No message date grouping (date headers)

**Files:**
- `composeApp/.../ui/v2/screens/parent/ParentMessagesScreenV2.kt:488-490`
- `composeApp/.../ui/v2/screens/school/MessagesScreenV2.kt:253-255`

**Problem:** Messages are rendered as a flat list with no date separators. In a conversation spanning multiple days, the user has no way to tell when the date changed. WhatsApp shows "Today", "Yesterday", "March 15" headers between message groups.

**Fix:** Add a date-header composable that checks if the current message's date differs from the previous message's date:
```kotlin
items(messages, key = { it.id }) { msg ->
    val showDateHeader = // compare msg.createdAt with previous message's createdAt
    if (showDateHeader) {
        DateHeader(date = formatDate(msg.createdAt))
    }
    ParentMessageBubble(msg)
}
```

**Severity:** Low — UX polish, but expected in any modern chat app.

---

### P1-8: No message grouping by sender

**Files:**
- `composeApp/.../ui/v2/screens/parent/ParentMessagesScreenV2.kt:488-490`
- `composeApp/.../ui/v2/screens/school/MessagesScreenV2.kt:253-255`

**Problem:** Consecutive messages from the same sender are rendered as individual bubbles with full padding and rounded corners on all sides. WhatsApp groups consecutive messages:
- Shared top corner (no gap between stacked messages from same sender)
- Only the last message in a group has the "tail" (sharp bottom corner)
- Minimal vertical spacing (2-4dp instead of 6-8dp)

This saves vertical space and visually communicates message flow.

**Fix:** In the `LazyColumn` items lambda, check if the previous message has the same `senderId`. If so, apply tighter spacing and adjust the bubble shape:
```kotlin
itemsIndexed(messages, key = { _, msg -> msg.id }) { index, msg ->
    val isGroupStart = index == 0 || messages[index - 1].senderId != msg.senderId
    val isGroupEnd = index == messages.lastIndex || messages[index + 1].senderId != msg.senderId
    ParentMessageBubble(msg, isGroupStart = isGroupStart, isGroupEnd = isGroupEnd)
}
```

**Severity:** Low — visual polish.

---

### P1-9: No auto-scroll to bottom on admin screen

**File:** `composeApp/.../ui/v2/screens/school/MessagesScreenV2.kt:245-256`

**Problem:** The admin conversation uses `verticalScroll` with no `LaunchedEffect` to scroll to the bottom when new messages arrive. After `reloadConversation()` completes, the user sees the **top** of the conversation, not the latest messages. They have to manually scroll down.

The parent screen correctly has:
```kotlin
LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
        listState.animateScrollToItem(messages.lastIndex)
    }
}
```

**Fix:** When converting to `LazyColumn` (P1-1), add the same `LaunchedEffect` with `rememberLazyListState()`.

**Severity:** High — users will think new messages didn't arrive because they're looking at the top of an old conversation.

---

### P1-10: Inconsistent compose bar UX between admin and parent

**Files:**
- Admin: `composeApp/.../ui/v2/screens/school/MessagesScreenV2.kt:261-291` — uses `VInput` + `VButton("Send")` (form-like layout)
- Parent: `composeApp/.../ui/v2/screens/parent/ParentMessagesScreenV2.kt:602-694` — uses custom WhatsApp-style pill input with embedded circular send button

**Problem:** Two completely different compose bar designs for the same feature. The admin version looks like a form (rectangular input + separate "Send" button), while the parent version looks like WhatsApp (rounded pill input + circular send icon button).

**Fix:** Extract the parent's `ParentComposeBar` into a shared composable `VComposeBar` in the components package. Use it in both admin and parent screens. The WhatsApp-style pill bar is the better UX:
- More familiar to users
- Takes less vertical space
- Send button is always reachable by thumb
- Circular button with icon is more tactile than a text button

**Severity:** Medium — inconsistency is confusing, especially if a user switches between admin and parent roles.

---

### P1-11: Inconsistent thread list layout between admin and parent

**Files:**
- Admin: `composeApp/.../ui/v2/screens/school/MessagesScreenV2.kt:178-184` — `VCard` container with `forEachIndexed` + divider lines, 40dp avatars, `VBadge` for unread
- Parent: `composeApp/.../ui/v2/screens/parent/ParentMessagesScreenV2.kt:206-219` — `LazyColumn` with individual rows, 52dp avatars, custom unread pill badge

**Problem:** Different visual languages for the same data type. Different avatar sizes, different unread badge styles, different container approach (card vs. flat list).

**Fix:** Create a shared `VThreadRow` composable and `VThreadList` composable. Parameterize avatar size and badge style if needed, but default to one consistent design. The parent's flat `LazyColumn` approach is better (more scalable, matches WhatsApp).

**Severity:** Medium — design inconsistency.

---

### P1-12: No ripple/haptic feedback on interactive elements

**Files:** Multiple — every `.clickable()` call in both screens uses `indication = null`:

```kotlin
// ParentMessagesScreenV2.kt:251
.clickable(interactionSource = interaction, indication = null, onClick = onClick)

// ParentMessagesScreenV2.kt:231 (FAB)
.clickable(interactionSource = interaction, indication = null, onClick = onCompose)

// ParentMessagesScreenV2.kt:398 (recipient row)
.clickable(interactionSource = interaction, indication = null, onClick = onClick)

// ParentMessagesScreenV2.kt:674 (send button)
.clickable(interactionSource = sendInteraction, indication = null, enabled = canSend, onClick = onSend)

// MessagesScreenV2.kt:195 (thread row)
.clickable(interactionSource = interaction, indication = null, onClick = onClick)

// MessagesScreenV2.kt:383 (recipient row)
.clickable(interactionSource = interaction, indication = null, onClick = onClick)
```

**Problem:** `indication = null` removes the ripple effect on Android. Tapping a thread row, FAB, recipient, or send button gives **zero visual feedback** that the tap was registered. This feels dead and unresponsive — users will tap multiple times thinking the app didn't respond.

**Fix:** Remove `indication = null` and let the default ripple show. Or explicitly use:
```kotlin
.clickable(
    interactionSource = interaction,
    indication = ripple(bounded = true, color = c.accent),
    onClick = onClick,
)
```

If the design intentionally avoids ripple for aesthetic reasons, add a subtle `animateColorAsState` background change on press instead:
```kotlin
val isPressed by interaction.collectIsPressedAsState()
val bgColor by animateColorAsState(if (isPressed) c.accentTint else Color.Transparent)
```

**Severity:** Medium — feels broken on Android. iOS users won't notice (no ripple convention), but Android users will.

---

### P1-13: No `markAsRead` called from admin UI

**File:** `shared/.../feature/admin/presentation/MessagesViewModel.kt:150-186`

**Problem:** `markAsRead` exists in the ViewModel with optimistic update logic, but is **never called** from `MessagesScreenV2.kt`. The server clears unread as a side effect of `GET /threads/{id}/messages`, but:
1. The optimistic local update (which prevents the unread badge from flickering) is never triggered
2. If the user opens a conversation and the GET fails, the unread badge stays (correct) but if the GET succeeds, the badge clears only after the full response is processed (flicker)

**Fix:** Call `markAsRead(threadId)` immediately when a thread is tapped, before `openConversation`:
```kotlin
fun openConversation(threadId: String) {
    markAsRead(threadId)  // optimistic local update
    // ... existing openConversation logic
}
```

Or call it from the UI:
```kotlin
onOpenThread = { threadId ->
    viewModel.markAsRead(threadId)
    viewModel.openConversation(threadId)
}
```

**Severity:** Low — badge flicker, not a functional bug.

---

## 4. P2 — Missing Features (Spec'd but not implemented)

### P2-1: No teacher messaging screen

**File:** `composeApp/.../ui/v2/screens/teacher/TeacherPortalV2.kt`

**Problem:** `TeacherPortalV2` has 4 tabs (HOME, UPDATE, CLASSES, PROFILE) — none of them is Messages. There is no `TeacherMessagesScreenV2.kt` file. The backend teacher routes exist (`TeacherMessagesRouting.kt` with threads, conversation, send, class broadcast), but there is no client-side screen, ViewModel, API class, or domain model for teacher messaging.

Teachers cannot send or receive messages from the app.

**Fix:** Create:
1. `shared/.../feature/teacher/domain/model/TeacherMessageModels.kt`
2. `shared/.../feature/teacher/data/remote/TeacherMessagesApi.kt`
3. `shared/.../feature/teacher/data/repository/TeacherMessagesRepository.kt` + Impl
4. `shared/.../feature/teacher/presentation/TeacherMessageViewModel.kt`
5. `composeApp/.../ui/v2/screens/teacher/TeacherMessagesScreenV2.kt`
6. Add a Messages tab or overlay to `TeacherPortalV2.kt`
7. Register in Koin DI module

**Severity:** High — entire user role (teachers) cannot use messaging.

---

### P2-2: Teacher backend routes not updated with Phase 1 features

**File:** `server/.../feature/teacher/TeacherMessagesRouting.kt`

**Problem:** The teacher routes still use the old DTO shape:

| Feature | Admin/Parent | Teacher |
|---|---|---|
| Pagination (offset/limit) | ✅ | ❌ |
| `client_msg_id` (idempotency) | ✅ | ❌ |
| `reply_to_id` (reply-quoting) | ✅ | ❌ |
| `attachments` (media in messages) | ✅ | ❌ |
| `PATCH /messages/{id}` (edit) | ✅ | ❌ |
| `DELETE /messages/{id}` (delete) | ✅ | ❌ |
| `POST /attachments` (upload) | ✅ | ❌ |
| `seq` in response | ✅ | ❌ |
| `status` in response | ✅ | ❌ |

The `TeacherSendMessageDto` (line 93-97) has only `threadId`, `recipientUserId`, and `body`. No `clientMsgId`, `replyToId`, or `attachments`.

**Fix:** Mirror the admin `SendMessageDto` and add the same Phase 1 endpoints to the teacher route group. The core engine (`sendInConversation`) already supports all features — only the routing layer needs updating.

**Severity:** High — teacher messaging will be missing all Phase 1 features even after a screen is built.

---

### P2-3: No edit/delete UI

**Files:**
- `composeApp/.../ui/v2/screens/parent/ParentMessagesScreenV2.kt:488-490`
- `composeApp/.../ui/v2/screens/school/MessagesScreenV2.kt:253-255`

**Problem:** No long-press menu on message bubbles. No "Edit" or "Delete" option in any UI. The server endpoints (`PATCH /messages/{id}`, `DELETE /messages/{id}`) are implemented and the core functions (`editMessage`, `deleteMessage`) exist, but the client has no UI to trigger them.

**Fix:**
1. Add a long-press handler to `MessageBubble` that shows a popup menu (`DropdownMenu`)
2. Menu items: "Reply", "Edit" (only for own messages, within 24h), "Delete" (own messages), "Copy"
3. Wire to new ViewModel methods: `editMessage(msgId, newBody)`, `deleteMessage(msgId, scope)`
4. Add the API methods to `MessagesApi.kt` (P0-7)

**Severity:** Medium — expected feature in any chat app.

---

### P2-4: No attachment display or send UI

**Files:**
- `composeApp/.../ui/v2/screens/parent/ParentMessagesScreenV2.kt:554-596` (`ParentMessageBubble`)
- `composeApp/.../ui/v2/screens/school/MessagesScreenV2.kt:400-430` (`MessageBubble`)

**Problem:** Message bubbles only render `msg.body` (text). No image rendering, no document attachment display, no attachment icon in the compose bar. The server stores and returns attachment metadata, but:
1. Client `Message` model doesn't have `attachments` field (P0-5)
2. Even if it did, the bubble composable doesn't render them
3. The compose bar has no attachment picker (paperclip icon)

**Fix:**
1. Fix client models (P0-5)
2. Add attachment rendering in `MessageBubble`: images via Coil, documents as file cards with icon + filename + size
3. Add a paperclip icon button to the compose bar that opens a file picker
4. Upload via `POST /messages/attachments`, get `storage_url`, include in send request

**Severity:** Medium — core messaging feature missing.

---

### P2-5: No message status indicators (ticks)

**Files:** Both `MessageBubble` composables

**Problem:** No single tick (sent), double tick (delivered), or blue double tick (read) on sent messages. The server returns `status` in the `MessageDto`, but:
1. Client model doesn't capture `status` (P0-5)
2. UI doesn't render tick marks

**Fix:**
1. Fix client models (P0-5)
2. Add tick mark icons in the message bubble's timestamp row:
   - `status == "SENT"` → single gray checkmark
   - `status == "DELIVERED"` → double gray checkmarks
   - `status == "READ"` → double blue/accent checkmarks
   - Only show on own messages (`isMine == true`)

**Severity:** Low — UX expectation, not a functional bug.

---

### P2-6: No search UI

**Problem:** No search bar in the thread list screen. The spec (§15.2) describes PostgreSQL FTS with `tsvector` + GIN index, but:
1. The `search_tsv` column and GIN index migration hasn't been run
2. No `GET /messages/search` endpoint exists on any route group
3. No search bar in either UI screen

**Fix:** Implement per the spec — add the FTS migration, search endpoint, and a search bar in the thread list with results showing message preview + peer name + timestamp.

**Severity:** Low — nice-to-have, not critical for initial release.

---

### P2-7: No draft persistence

**Files:**
- `composeApp/.../ui/v2/screens/parent/ParentMessagesScreenV2.kt:451` — `var reply by remember { mutableStateOf("") }`
- `composeApp/.../ui/v2/screens/school/MessagesScreenV2.kt:231` — `var reply by remember { mutableStateOf("") }`

**Problem:** Compose text is stored in a `remember` variable, which is cleared when the composable leaves the composition (screen exit, config change, process death). If a user types a long message and accidentally taps back, the text is lost.

**Fix:** Persist drafts to DataStore or a simple key-value store keyed by `conversation_id`:
```kotlin
// On text change (debounced):
LaunchedEffect(reply) {
    delay(500)  // debounce
    draftStore.saveDraft(conversationId, reply)
}

// On screen entry:
LaunchedEffect(conversationId) {
    reply = draftStore.getDraft(conversationId) ?: ""
}

// On send:
draftStore.clearDraft(conversationId)
```

**Severity:** Low — UX convenience.

---

### P2-8: No typing indicators

**Problem:** No "typing…" indicator shown to the peer. No WebSocket connection to carry ephemeral events. The spec (§7.4) describes WS-only typing events with 5s auto-expiry, but no WS infrastructure exists yet.

**Fix:** Requires WebSocket implementation (spec §5). Not feasible without Phase 2 of the implementation roadmap.

**Severity:** Low — nice-to-have without realtime infrastructure.

---

### P2-9: No presence

**Problem:** No online/offline status on avatars. No "last seen today at 3:45 PM" text. The spec (§7.5) describes Redis-based presence with TTL keys, but no Redis or WS infrastructure exists.

**Fix:** Requires Redis + WebSocket (spec §5, §7.5). Phase 2 of the roadmap.

**Severity:** Low — nice-to-have without realtime infrastructure.

---

### P2-10: No deleted/edited message display

**Problem:** Even if client models are fixed (P0-5), the UI needs to:
1. Show "This message was deleted" (gray italic text) instead of blank when `deletedAt != null`
2. Show "edited" label below timestamp when `editedAt != null`
3. Show the edited body (server already returns the updated body)

**Fix:** In `MessageBubble`:
```kotlin
if (msg.deletedAt != null) {
    Text("This message was deleted", style = VTheme.type.body.colored(c.ink3).copy(fontStyle = FontStyle.Italic))
} else {
    Text(msg.body, style = VTheme.type.body.colored(textColor))
    if (msg.editedAt != null) {
        Text("edited", style = VTheme.type.caption.colored(timeColor).copy(fontSize = 9.sp))
    }
}
```

**Severity:** Low — depends on P0-5 (model sync) first.

---

## 5. P3 — Polish & Consistency

### P3-1: `LaunchedEffect(Unit)` reloads threads on every screen entry

**File:** `composeApp/.../ui/v2/screens/parent/ParentMessagesScreenV2.kt:84`

```kotlin
LaunchedEffect(Unit) { viewModel.loadThreads() }
```

**Problem:** `LaunchedEffect(Unit)` runs every time the composable enters composition. If the user navigates away and back, `loadThreads()` fires again, causing a loading spinner flash even if threads were loaded 2 seconds ago.

**Fix:** Check if threads are already loaded:
```kotlin
LaunchedEffect(Unit) {
    if (state.threads.isEmpty() && !state.loading) {
        viewModel.loadThreads()
    }
}
```

Or better: move the initial load to the ViewModel's `init` block (like `MessagesViewModel` does) and use `LaunchedEffect` only for refresh-on-resume scenarios.

**Severity:** Low — minor UX flash.

---

### P3-2: No keyboard auto-open on compose-new

**Problem:** When opening the compose-new sheet, the keyboard doesn't automatically open for the text input. The user has to tap the text field to start typing.

**Fix:** Use `FocusRequester` and `LaunchedEffect`:
```kotlin
val focusRequester = remember { FocusRequester() }
LaunchedEffect(Unit) {
    delay(300)  // wait for sheet animation
    focusRequester.requestFocus()
}
// Apply to the text field:
OutlinedTextField(modifier = Modifier.focusRequester(focusRequester)...)
```

**Severity:** Low — minor friction.

---

### P3-3: No character count for long messages

**Problem:** Body limit is 4096 characters (server-enforced) but the user gets no feedback until they hit the limit and receive an error response. For long messages, the user might type a 5000-character message and lose it all on send.

**Fix:** Show a character counter when the body exceeds 3800 characters:
```kotlin
if (reply.length > 3800) {
    Text("${reply.length}/4096", style = VTheme.type.caption.colored(
        if (reply.length > 4096) c.danger else c.ink3
    ))
}
```

Disable the send button when `reply.length > 4096`.

**Severity:** Low — edge case.

---

### P3-4: No swipe-to-go-back gesture

**Problem:** Neither screen uses Android's predictive back gesture for swipe-back. The `BackHandler` is wired to a button tap, not a swipe. On Android 14+, users expect to swipe back.

**Fix:** The `BackHandler` composable already handles the back button. For swipe-back, ensure the activity has `enableOnBackInvokedCallback = true` in the manifest and that `BackHandler` is properly registered. Compose Multiplatform's `ui-backhandler` module handles this.

**Severity:** Low — platform convention.

---

### P3-5: No message bubble enter animation

**Problem:** New messages just appear in the list with no animation. WhatsApp slides them in from the bottom with a subtle fade.

**Fix:** Use `AnimatedVisibility` or `animateItemPlacement` (LazyColumn):
```kotlin
LazyColumn(modifier = Modifier.fillMaxSize()) {
    items(messages, key = { it.id }) { msg ->
        // animateItemPlacement() automatically animates position changes
        ParentMessageBubble(msg)
    }
}
```

For new messages at the bottom, the `animateScrollToItem` already provides a scroll animation. Adding `Modifier.animateItemPlacement()` to the item modifier handles insert/move/remove animations.

**Severity:** Low — visual polish.

---

### P3-6: No empty state illustration for compose-new recipients

**File:** `composeApp/.../ui/v2/screens/parent/ParentMessagesScreenV2.kt:339-341`

**Problem:** The compose-new empty state is text-only: "No one to message yet" / "Link your child to a school...". No illustration, no actionable CTA button.

**Fix:** Add an illustration (or icon-based graphic) and a CTA button that links to the child-linking flow:
```kotlin
emptyTitle = "No contacts yet",
emptyBody = "Link your child to a school to message their teachers.",
emptyIcon = VIcons.Chat,
emptyAction = "Link your child",
onEmptyAction = { /* navigate to child linking */ }
```

**Severity:** Low — polish.

---

### P3-7: Admin compose-new doesn't clear body on send

**File:** `composeApp/.../ui/v2/screens/school/MessagesScreenV2.kt:362-364`

```kotlin
if (r != null && body.isNotBlank()) onSend(r.id, body)
```

**Problem:** Unlike the parent screen which does `body = ""` after send, the admin screen doesn't clear the text field. If the send succeeds, the compose sheet closes (so it doesn't matter). But if the send fails, the text remains (which is actually good for retry). However, this is inconsistent with the parent behavior.

**Fix:** This is actually the **better** behavior (preserve text on failure). Make the parent screen match: don't clear `body` until the `onSent` callback fires (which `MessagesViewModel.composeNew` already does via the `onSent` callback in `sendMessage`).

In the parent screen, move the `body = ""` into the success callback path, not before the API call.

**Severity:** Low — inconsistency, but the admin behavior is actually correct.

---

## 6. Fix Priority Matrix

| ID | Priority | Effort | Component | One-line description |
|---|---|---|---|---|
| P0-1 | 🔴 Critical | M | Backend | Seq race condition — use atomic counter |
| P0-2 | 🔴 Critical | S | Backend | Delete sets body="" instead of NULL |
| P0-3 | 🔴 Critical | S | Backend | OR clause can leak cross-conversation messages |
| P0-4 | 🔴 Critical | S | Backend | Deep link hardcoded to "parent/messages" |
| P0-5 | 🔴 Critical | M | Client models | Domain models missing Phase 1 fields |
| P0-6 | 🔴 Critical | S | Client models | SendMessageRequest missing clientMsgId, replyToId, attachments |
| P0-7 | 🔴 Critical | M | Client API | MessagesApi missing pagination, edit, delete, attachment methods |
| P1-1 | 🟠 High | S | Admin UI | Replace verticalScroll with LazyColumn (conversation) |
| P1-2 | 🟠 High | S | Admin UI | Replace verticalScroll with LazyColumn (thread list) |
| P1-3 | 🟠 High | M | Both VMs | No optimistic send — UI flicker |
| P1-4 | 🟠 Medium | S | Both VMs | 10s polling too aggressive — increase to 30s + backoff |
| P1-5 | 🟠 Medium | S | Parent VM | Polling errors silently swallowed |
| P1-6 | 🟠 Low | S | Parent VM | Double loadThreads() in composeNew |
| P1-7 | 🟡 Low | S | Both UIs | No date grouping headers |
| P1-8 | 🟡 Low | S | Both UIs | No message grouping by sender |
| P1-9 | 🟠 High | S | Admin UI | No auto-scroll to bottom |
| P1-10 | 🟠 Medium | M | Both UIs | Inconsistent compose bar UX |
| P1-11 | 🟠 Medium | M | Both UIs | Inconsistent thread list layout |
| P1-12 | 🟠 Medium | S | Both UIs | No ripple feedback on clickable elements |
| P1-13 | 🟡 Low | S | Admin VM | markAsRead never called from UI |
| P2-1 | 🟠 High | L | Teacher | No teacher messaging screen |
| P2-2 | 🟠 High | M | Backend | Teacher routes missing Phase 1 features |
| P2-3 | 🟡 Medium | M | Both UIs | No edit/delete UI |
| P2-4 | 🟡 Medium | M | Both UIs | No attachment display or send UI |
| P2-5 | 🟡 Low | S | Both UIs | No message status ticks |
| P2-6 | 🟡 Low | M | Full stack | No search |
| P2-7 | 🟡 Low | S | Both UIs | No draft persistence |
| P2-8 | 🟡 Low | L | Full stack | No typing indicators (needs WS) |
| P2-9 | 🟡 Low | L | Full stack | No presence (needs WS + Redis) |
| P2-10 | 🟡 Low | S | Both UIs | No deleted/edited message display |
| P3-1 | 🔵 Low | S | Parent UI | LaunchedEffect reloads on every entry |
| P3-2 | 🔵 Low | S | Both UIs | No keyboard auto-open on compose |
| P3-3 | 🔵 Low | S | Both UIs | No character count for long messages |
| P3-4 | 🔵 Low | S | Both UIs | No swipe-to-go-back gesture |
| P3-5 | 🔵 Low | S | Both UIs | No message bubble enter animation |
| P3-6 | 🔵 Low | S | Parent UI | No empty state illustration for compose |
| P3-7 | 🔵 Low | S | Admin UI | Compose body not cleared on send (actually correct behavior) |

**Effort legend:** S = Small (< 1 hour), M = Medium (1-4 hours), L = Large (1+ days)

---

## 7. Recommended Fix Order

### Sprint 1: Critical Bug Fixes (P0)

| Order | ID | Rationale |
|---|---|---|
| 1 | P0-1 | Seq race condition — silent data corruption, fix first |
| 2 | P0-2 | Delete body="" — one-line fix, immediate |
| 3 | P0-3 | OR clause leak — security/data integrity |
| 4 | P0-4 | Deep link hardcode — one-line fix, immediate |
| 5 | P0-5 | Client model sync — unblocks all P2 UI work |
| 6 | P0-6 | Send request fields — unblocks optimistic send (P1-3) |
| 7 | P0-7 | API methods — unblocks edit/delete/attachment UI (P2-3, P2-4) |

### Sprint 2: Performance & UX Foundations (P1)

| Order | ID | Rationale |
|---|---|---|
| 8 | P1-1 | Admin LazyColumn — prevents OOM, unblocks P1-9 |
| 9 | P1-2 | Admin thread list LazyColumn — same |
| 10 | P1-9 | Admin auto-scroll — depends on P1-1 |
| 11 | P1-12 | Ripple feedback — quick win, big perceived quality boost |
| 12 | P1-3 | Optimistic send — biggest UX improvement |
| 13 | P1-4 | Polling interval — quick config change |
| 14 | P1-5 | Polling error surfacing — quick fix |
| 15 | P1-6 | Double loadThreads — one-line fix |
| 16 | P1-13 | markAsRead from UI — one-line fix |

### Sprint 3: Consistency Unification (P1 continued)

| Order | ID | Rationale |
|---|---|---|
| 17 | P1-10 | Unify compose bar — extract shared component |
| 18 | P1-11 | Unify thread list — extract shared component |
| 19 | P1-7 | Date headers — add to shared message list |
| 20 | P1-8 | Message grouping — add to shared message bubble |

### Sprint 4: Missing Features (P2)

| Order | ID | Rationale |
|---|---|---|
| 21 | P2-2 | Teacher backend routes — unblocks P2-1 |
| 22 | P2-1 | Teacher messaging screen — full feature for teacher role |
| 23 | P2-10 | Deleted/edited display — depends on P0-5 |
| 24 | P2-5 | Status ticks — depends on P0-5 |
| 25 | P2-3 | Edit/delete UI — depends on P0-7 |
| 26 | P2-4 | Attachment UI — depends on P0-5, P0-7 |
| 27 | P2-7 | Draft persistence — standalone |

### Sprint 5: Polish (P3 + remaining P2)

| Order | ID | Rationale |
|---|---|---|
| 28 | P3-1 | LaunchedEffect guard |
| 29 | P3-2 | Keyboard auto-open |
| 30 | P3-3 | Character count |
| 31 | P3-5 | Bubble animation |
| 32 | P3-6 | Empty state illustration |
| 33 | P3-7 | Align parent compose-clear with admin (keep text on failure) |
| 34 | P3-4 | Swipe-back gesture |
| 35 | P2-6 | Search (standalone, lower priority) |
| 36 | P2-8 | Typing indicators (requires WebSocket — Phase 2) |
| 37 | P2-9 | Presence (requires WebSocket + Redis — Phase 2) |

---

*End of document.*
