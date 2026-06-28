# Smart Notifications — Technical Specification

> **Document status:** Partial (65%) — preferences filtering, rate limiting, scheduler, push bridge implemented; AI priority, batching, quiet hours, daily digest TODO
> **Last updated:** 2026-06-28
> **Prerequisites:** `AI_INFRASTRUCTURE_SPEC.md`, `WHATSAPP_INTEGRATION_SPEC.md`
> **Source:** `DIFFERENTIATING_FEATURES.md` §4.1
> **Template:** `_SPEC_TEMPLATE.md` v1 (25 mandatory + 6 optional sections)

---

## 1. Feature Overview

### What

AI-powered notification prioritization, batching, and smart delivery. Instead of sending every notification immediately, the system groups, prioritizes, and times notifications to reduce notification fatigue while ensuring critical alerts are never missed.

### Why — Product Rationale

School parents receive dozens of notifications daily — attendance, homework, marks, fees, announcements, events. Without prioritization, parents either mute all notifications (missing critical alerts) or suffer notification fatigue. Smart Notifications uses AI to classify urgency, batches non-critical notifications, respects quiet hours, and delivers a daily digest — ensuring parents see what matters when it matters.

This is a **differentiating feature** (Priority P1, Phase 2, effort M, "High" value per `DIFFERENTIATING_FEATURES.md`). It improves parent engagement by reducing notification fatigue while ensuring critical alerts are never missed.

### What Stands Out (Competitive Moat)

From `DIFFERENTIATING_FEATURES.md` §4.1:
> "Smart Notifications — AI-powered notification prioritization, batching, and smart delivery."

No major school ERP uses AI for notification prioritization. Most send all notifications immediately with no batching, quiet hours, or digest.

### Goals

- Priority levels: critical (immediate), high (within 5 min), normal (batched), low (digest)
- Smart batching: non-critical notifications batched and sent together (max 3 per hour)
- AI priority assignment: LLM categorizes notification importance based on content + context
- Quiet hours: no non-critical notifications 9 PM - 7 AM (configurable)
- Smart channel selection: critical → WhatsApp + FCM, normal → FCM only, low → in-app only
- Notification grouping: related notifications grouped (e.g., 3 fee reminders → 1 grouped notification)

### Non-goals

- [ ] User-to-user messaging (existing messaging system)
- [ ] Notification content generation (AI only classifies, doesn't write content)
- [ ] Cross-school notification aggregation
- [ ] Notification analytics dashboard for parents
- [ ] Custom notification channels beyond WhatsApp, FCM, in-app

### Dependencies

- `AI_INFRASTRUCTURE_SPEC.md` — AI service for priority classification
- `WHATSAPP_INTEGRATION_SPEC.md` — WhatsApp channel for critical notifications
- `NotificationsTable` — existing notifications table (modified)
- `NotificationPreferencesTable` — existing preferences table (modified)
- `Notify.kt` — existing notification dispatch (modified)
- `NotificationScheduler.kt` — existing scheduling infrastructure
- `NotificationService.kt` — existing FCM push dispatch
- `DeviceTokenRepository.kt` — existing FCM device token management

### Related Modules

- `server/.../feature/notifications/` — notification system (modified)
- `server/.../feature/notification/service/` — FCM push service
- `composeApp/.../ui/v2/screens/parent/` — notification preferences UI

---

## 2. Current System Assessment

### Existing Code

- `NotificationsTable` (`Tables.kt`) — per-recipient notifications with `category`, `isRead`
- `NotificationPreferencesTable` (`Tables.kt`) — per-category enable/disable + sound
- `Notify.kt` — now includes **preferences filtering** (drops recipients who disabled a category), **rate limiting** (max 50/user/day, max 10/category/hour), and **push bridge** (fire-and-forget FCM dispatch via `NotificationService`)
- `NotificationPreferencesRouting.kt` (`server/.../feature/notifications/`) — API for reading/updating per-category notification preferences
- `NotificationScheduler.kt` (`server/.../feature/notifications/`) — scheduling infrastructure for delayed/batched notifications
- `NotificationService.kt` (`server/.../feature/notification/service/`) — FCM push dispatch with `SendNotificationRequest`
- `DeviceTokenRepository.kt` — manages FCM device tokens
- `NotificationPermissionLauncher` — cross-platform permission request flow (Android, iOS, JVM, web)

### Existing Database

- `NotificationsTable` — per-recipient notifications with category, isRead, createdAt
- `NotificationPreferencesTable` — per-user per-category enable/disable + sound
- No priority, batch_id, scheduled_for, or quiet hours columns

### Existing APIs

- `NotificationPreferencesRouting.kt` — GET/PATCH preferences
- `Notify.kt` — internal dispatch API (not HTTP)

### Existing UI

- `NotificationPreferencesScreen.kt` — per-category enable/disable toggles

### Existing Services

- `Notify.kt` — notification dispatch with preferences filtering and rate limiting
- `NotificationService.kt` — FCM push dispatch
- `NotificationScheduler.kt` — scheduling infrastructure

### Existing Documentation

- `DIFFERENTIATING_FEATURES.md` §4.1 — Smart Notifications
- `AI_INFRASTRUCTURE_SPEC.md` — AI service (prerequisite)
- `WHATSAPP_INTEGRATION_SPEC.md` — WhatsApp integration (prerequisite)

### Technical Debt

| # | Gap | Details |
|---|---|---|
| TD-1 | No priority levels | All notifications treated equally — no `priority` column |
| TD-2 | No AI classification | No LLM-based priority assignment |
| TD-3 | No batching | All notifications sent immediately — no `batch_id` or `scheduled_for` |
| TD-4 | No quiet hours | No `quiet_hours_start` / `quiet_hours_end` columns |
| TD-5 | No daily digest | No digest job or `daily_digest_enabled` flag |
| TD-6 | No smart channel selection | All notifications go through same channels |
| TD-7 | No notification grouping | No grouping of same-category notifications |

### Gaps

| # | Gap | Impact | Severity |
|---|---|---|---|
| G1 | No priority levels | Critical alerts buried in noise | **High** |
| G2 | No AI classification | Manual priority assignment not scalable | **High** |
| G3 | No batching | Notification fatigue — too many pushes | **High** |
| G4 | No quiet hours | Notifications at night disturb parents | **Medium** |
| G5 | No daily digest | Low-priority notifications pile up unread | **Medium** |
| G6 | No smart channel selection | All notifications use same channel regardless of urgency | **Medium** |
| G7 | No notification grouping | Multiple similar notifications sent separately | **Low** |

### What Is Not Yet Implemented

AI priority assignment, smart batching (grouping related notifications), quiet hours, daily digest, smart channel selection (WhatsApp + FCM routing), notification grouping.

---

## 3. Functional Requirements

### FR-001
| Field | Value |
|---|---|
| **Title** | AI Priority Assignment |
| **Description** | AI assigns priority (critical/high/normal/low) based on notification content + user context. |
| **Priority** | Critical |
| **User Roles** | System |
| **Acceptance notes** | LLM classifies notification using category, title, body. User can override per category. |

### FR-002
| Field | Value |
|---|---|
| **Title** | Critical Notification Delivery |
| **Description** | Critical notifications sent immediately via WhatsApp + FCM + in-app. |
| **Priority** | Critical |
| **User Roles** | System |
| **Acceptance notes** | No batching, no quiet hours bypass. All three channels used. |

### FR-003
| Field | Value |
|---|---|
| **Title** | High Priority Delivery |
| **Description** | High priority sent within 5 minutes via FCM + in-app. |
| **Priority** | High |
| **User Roles** | System |
| **Acceptance notes** | 5-minute delay allows for grouping if multiple high-priority notifications arrive together. |

### FR-004
| Field | Value |
|---|---|
| **Title** | Normal Notification Batching |
| **Description** | Normal notifications batched (max 3 per hour) via FCM. |
| **Priority** | High |
| **User Roles** | System |
| **Acceptance notes** | Batch processor runs every 30 min. Groups ≥3 pending normal notifications into one batch with AI summary. |

### FR-005
| Field | Value |
|---|---|
| **Title** | Low Priority In-App Only |
| **Description** | Low priority: in-app only, included in daily digest. |
| **Priority** | Medium |
| **User Roles** | System |
| **Acceptance notes** | No push notification. Visible in app. Included in daily digest at 8 AM IST. |

### FR-006
| Field | Value |
|---|---|
| **Title** | Quiet Hours |
| **Description** | Quiet hours: 9 PM - 7 AM, non-critical notifications queued (configurable per user). |
| **Priority** | High |
| **User Roles** | Parent |
| **Acceptance notes** | Configurable per user. At quiet hours end, queued notifications flushed (batched). |

### FR-007
| Field | Value |
|---|---|
| **Title** | Notification Grouping |
| **Description** | Notification grouping: same-category notifications within 30 min grouped. |
| **Priority** | Medium |
| **User Roles** | System |
| **Acceptance notes** | Same-category notifications within 30 min share `batch_id`. AI generates summary text. |

### FR-008
| Field | Value |
|---|---|
| **Title** | Custom Priority Override |
| **Description** | User can override: set custom priority for categories. |
| **Priority** | Medium |
| **User Roles** | Parent |
| **Acceptance notes** | `notification_preferences.custom_priority` per category. Overrides AI classification. |

### FR-009
| Field | Value |
|---|---|
| **Title** | Daily Digest |
| **Description** | Daily digest: all low-priority notifications sent as one summary at 8 AM. |
| **Priority** | Medium |
| **User Roles** | System |
| **Acceptance notes** | Runs at 8 AM IST. Sends one FCM + in-app notification with AI-generated summary of all low-priority notifications from previous day. |

### Non-Functional Requirements

| ID | Requirement |
|---|---|
| NFR-1 | AI priority classification completes in < 2 seconds per notification |
| NFR-2 | Batch processor handles 10,000 pending notifications in < 5 minutes |
| NFR-3 | Daily digest job completes in < 10 minutes for all users |
| NFR-4 | Critical notifications delivered within 30 seconds of creation |
| NFR-5 | Quiet hours configuration stored per user, not per device |
| NFR-6 | Existing rate limiting (50/user/day, 10/category/hour) preserved |

---

## 4. User Stories

### Parent
- [ ] Receive critical notifications immediately via WhatsApp + push
- [ ] Receive batched normal notifications (max 3/hour) instead of individual pushes
- [ ] Not receive non-critical notifications during quiet hours
- [ ] Get a daily digest of low-priority notifications at 8 AM
- [ ] Override AI priority for specific categories (e.g., always "high" for fees)
- [ ] Configure quiet hours start/end times
- [ ] Enable/disable daily digest

### System
- [ ] AI classifies notification priority based on content + context
- [ ] Batch normal notifications every 30 minutes
- [ ] Flush queued notifications at quiet hours end
- [ ] Generate daily digest at 8 AM IST
- [ ] Group same-category notifications within 30 min
- [ ] Route critical notifications to WhatsApp + FCM + in-app
- [ ] Route normal notifications to FCM only
- [ ] Route low-priority notifications to in-app only

---

## 5. Business Rules

### BR-001
**Rule:** Critical notifications bypass quiet hours and batching.
**Enforcement:** `priority = 'critical'` → `sendImmediately()` with all channels. No quiet hours check.

### BR-002
**Rule:** Max 3 batched notifications per hour per user.
**Enforcement:** Batch processor checks count of batches in last hour. If ≥ 3, defer to next hour.

### BR-003
**Rule:** User's custom priority overrides AI classification.
**Enforcement:** `notification_preferences.custom_priority` checked before AI. If set, use that priority.

### BR-004
**Rule:** Quiet hours default 9 PM - 7 AM.
**Enforcement:** `notification_preferences.quiet_hours_start` default '21:00', `quiet_hours_end` default '07:00'. Configurable per user.

### BR-005
**Rule:** Same-category notifications within 30 min are grouped.
**Enforcement:** Batch processor groups by `(user_id, category)` where `created_at` within 30 min window. Shared `batch_id`.

### BR-006
**Rule:** Daily digest sent at 8 AM IST.
**Enforcement:** `DailyDigestJob` scheduled at 8 AM IST. Collects all low-priority notifications from previous 24 hours. AI generates summary. Sent as one FCM + in-app.

### BR-007
**Rule:** Existing rate limiting preserved.
**Enforcement:** `Notify.kt` rate limiting (50/user/day, 10/category/hour) remains. Smart notifications layer on top, not replacing.

### BR-008
**Rule:** Low-priority notifications never sent as push.
**Enforcement:** `priority = 'low'` → `storeInAppOnly()`. No FCM dispatch. Only in-app + daily digest.

---

## 6. Database Design

### 6.1 Entity Relationship Summary

Modified `notifications` table (add priority, batch_id, scheduled_for, ai_priority_assigned), modified `notification_preferences` table (add quiet hours, custom priority, daily digest), new `notification_batches` table (batch metadata + AI summary).

### 6.2 New Tables

#### `notification_batches` table

```sql
CREATE TABLE notification_batches (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,
    school_id       UUID NOT NULL,
    notification_ids TEXT NOT NULL,                -- JSON array of notification UUIDs
    summary_text    TEXT NOT NULL,                 -- AI-generated batch summary
    sent_at         TIMESTAMP NOT NULL DEFAULT now(),
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
```

### 6.3 Modified Tables

#### `notifications` — add four columns

```sql
ALTER TABLE notifications ADD COLUMN priority VARCHAR(16) NOT NULL DEFAULT 'normal';
-- critical | high | normal | low
ALTER TABLE notifications ADD COLUMN batch_id UUID;  -- grouped notifications share batch_id
ALTER TABLE notifications ADD COLUMN scheduled_for TIMESTAMP;  -- when to send (for batched/digest)
ALTER TABLE notifications ADD COLUMN ai_priority_assigned BOOLEAN NOT NULL DEFAULT false;
```

#### `notification_preferences` — add four columns

```sql
ALTER TABLE notification_preferences ADD COLUMN quiet_hours_start VARCHAR(8) DEFAULT '21:00';
ALTER TABLE notification_preferences ADD COLUMN quiet_hours_end VARCHAR(8) DEFAULT '07:00';
ALTER TABLE notification_preferences ADD COLUMN custom_priority VARCHAR(16); -- override AI priority
ALTER TABLE notification_preferences ADD COLUMN daily_digest_enabled BOOLEAN NOT NULL DEFAULT true;
```

### 6.4 Indexes

- `notifications(user_id, priority, scheduled_for)` — for batch processor queries
- `notifications(batch_id)` — for grouping
- `notification_batches(user_id, sent_at)` — for rate limiting checks

### 6.5 Constraints

- `notifications.priority` — NOT NULL, default 'normal', VARCHAR(16)
- `notifications.ai_priority_assigned` — NOT NULL, default false
- `notification_preferences.quiet_hours_start` — VARCHAR(8), default '21:00'
- `notification_preferences.quiet_hours_end` — VARCHAR(8), default '07:00'
- `notification_preferences.daily_digest_enabled` — NOT NULL, default true
- `notification_batches.user_id` — NOT NULL
- `notification_batches.school_id` — NOT NULL
- `notification_batches.notification_ids` — NOT NULL (JSON array)
- `notification_batches.summary_text` — NOT NULL

### 6.6 Foreign Keys

- `notifications.batch_id` → `notification_batches.id` (implicit)
- `notification_batches.user_id` → `app_users.id` (implicit)
- `notification_batches.school_id` → `schools.id` (implicit)

### 6.7 Soft Delete Strategy

N/A — notifications are not soft-deleted. Read status tracked via `isRead`.

### 6.8 Audit Fields

- `notifications`: `createdAt` (existing), `scheduled_for` (new)
- `notification_batches`: `created_at`, `sent_at`

### 6.9 Migration Notes

Migration: `docs/db/migration_070_smart_notifications.sql`
- ALTER `notifications` — add `priority`, `batch_id`, `scheduled_for`, `ai_priority_assigned`
- ALTER `notification_preferences` — add `quiet_hours_start`, `quiet_hours_end`, `custom_priority`, `daily_digest_enabled`
- CREATE `notification_batches` table
- Backfill: existing notifications get `priority = 'normal'`, `ai_priority_assigned = false`
- No data loss — all existing functionality preserved

### 6.10 Exposed Mappings

```kotlin
// Inside existing NotificationsTable object:
val priority           = varchar("priority", 16).default("normal")  // critical | high | normal | low
val batchId            = uuid("batch_id").nullable()
val scheduledFor       = timestamp("scheduled_for").nullable()
val aiPriorityAssigned = bool("ai_priority_assigned").default(false)

// Inside existing NotificationPreferencesTable object:
val quietHoursStart    = varchar("quiet_hours_start", 8).default("21:00")
val quietHoursEnd      = varchar("quiet_hours_end", 8).default("07:00")
val customPriority     = varchar("custom_priority", 16).nullable()
val dailyDigestEnabled = bool("daily_digest_enabled").default(true)

// New table:
object NotificationBatchesTable : UUIDTable("notification_batches", "id") {
    val userId          = uuid("user_id")
    val schoolId        = uuid("school_id")
    val notificationIds = text("notification_ids")  // JSON array
    val summaryText     = text("summary_text")
    val sentAt          = timestamp("sent_at")
    val createdAt       = timestamp("created_at")
}
```

Register `NotificationBatchesTable` in `DatabaseFactory.allTables`.

### 6.11 Seed Data

N/A — notification data created by system events.

---

## 7. State Machines

### Notification Priority State Machine

```
created ──ai_classifies──> priority_assigned ──routed──> delivered
  │                              │
  │──user_override──>            │
  │  priority_assigned           │
  │  (custom priority)           │
  │                              │
  └──no_ai_available──>          │
     priority = 'normal'         │
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `created` | AI classifies | `priority_assigned` | AI service available |
| `created` | User override exists | `priority_assigned` | `custom_priority` set in preferences |
| `created` | AI unavailable | `priority_assigned` (normal) | Fallback to 'normal' |
| `priority_assigned` | Route: critical | `delivered` | Sent immediately via all channels |
| `priority_assigned` | Route: high | `delivered` | Sent within 5 min via FCM + in-app |
| `priority_assigned` | Route: normal | `batched` → `delivered` | Batched, sent within 30 min |
| `priority_assigned` | Route: low | `in_app_only` → `delivered` | In-app only, included in daily digest |

### Batch State Machine

```
pending ──batch_processor──> batched ──sent──> delivered
  │                            │
  │──quiet_hours──> queued     │──rate_limited──> deferred
  │                  │         │
  │                  └──quiet_hours_end──> batched
  └──1_hour_timeout──> batched
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `pending` | Batch processor runs (≥3 notifications) | `batched` | Grouped by user, AI summary generated |
| `pending` | Batch processor runs (<3 notifications) | `pending` | Wait for next cycle (max 1 hour) |
| `pending` | Quiet hours active | `queued` | Non-critical, within quiet hours |
| `queued` | Quiet hours end | `batched` | Flush queued notifications |
| `pending` | 1 hour timeout | `batched` | Force batch even if <3 notifications |
| `batched` | Rate limit not exceeded | `delivered` | < 3 batches in last hour |
| `batched` | Rate limit exceeded | `deferred` | Defer to next hour |

### Daily Digest State Machine

```
low_priority_notifications ──daily_digest_job──> digest_sent
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| Low-priority notifications exist | Daily digest job runs (8 AM IST) | `digest_sent` | `daily_digest_enabled = true` |
| Low-priority notifications exist | Daily digest job runs | `skipped` | `daily_digest_enabled = false` |

---

## 8. Backend Architecture

### 8.1 Component Overview

`SmartNotificationService` handles priority assignment, routing, batching, and quiet hours. `BatchProcessorJob` runs every 30 min to batch normal notifications. `DailyDigestJob` runs at 8 AM IST to send digests. `Notify.kt` is modified to route through `SmartNotificationService`.

### 8.2 Design Principles

1. **AI-assisted, not AI-only** — user overrides take precedence over AI classification
2. **Critical never delayed** — critical notifications bypass quiet hours, batching, and rate limiting
3. **Batching reduces fatigue** — normal notifications grouped, max 3/hour
4. **Quiet hours respected** — non-critical notifications queued during quiet hours
5. **Existing infrastructure preserved** — preferences filtering, rate limiting, push bridge remain

### 8.3 Core Types

#### SmartNotificationService

```kotlin
class SmartNotificationService(private val aiService: AiService) {
    suspend fun processNotification(notification: Notification): ProcessResult {
        // 1. Check user's custom priority override
        val priority = getPriority(notification)
        // 2. If no override, use AI to assign priority
        if (priority == null) {
            val aiPriority = aiService.classify(notification.content, notification.category)
            notification.priority = aiPriority
        }
        // 3. Route based on priority
        when (notification.priority) {
            "critical" -> sendImmediately(notification, channels = listOf("whatsapp", "fcm", "in_app"))
            "high" -> scheduleSend(notification, delayMinutes = 5, channels = listOf("fcm", "in_app"))
            "normal" -> batchNotification(notification)
            "low" -> storeInAppOnly(notification)  // included in daily digest
        }
    }

    suspend fun batchProcessor(): Int  // runs every 30 min, sends batches
    suspend fun dailyDigest(): Int  // runs at 8 AM IST, sends digest of low-priority notifications
}
```

### 8.4 Repositories

- `NotificationBatchRepository` — CRUD for `notification_batches`
- Existing `NotificationRepository` — extended with priority/batch queries

### 8.5 Mappers

- `NotificationBatchMapper` — maps `notification_batches` rows to `BatchDto`

### 8.6 Permission Checks

- Notification preferences: user can only view/edit own preferences
- Notification dispatch: system-internal, no user-facing permission

### 8.7 Background Jobs

- **BatchProcessorJob** — every 30 minutes
  1. Query `notifications` where `priority = 'normal'` AND `scheduled_for IS NULL` AND `created_at < now() - 5 min`
  2. Group by `user_id`
  3. If user has ≥ 3 pending normal notifications → create batch, generate AI summary, send as one FCM
  4. If < 3 → schedule for next batch cycle (max 1 hour delay)

- **DailyDigestJob** — daily 8 AM IST
  1. Query all users with `daily_digest_enabled = true`
  2. For each user, collect low-priority notifications from previous 24 hours
  3. Generate AI summary of all low-priority notifications
  4. Send as one FCM + in-app notification

### 8.8 Domain Events

- `NotificationPriorityAssigned` — emitted after AI or override assigns priority
- `NotificationBatched` — emitted when notifications grouped into a batch
- `NotificationDelivered` — emitted when notification sent via channel(s)
- `DailyDigestSent` — emitted when digest sent to user
- `QuietHoursQueued` — emitted when notification queued due to quiet hours

### 8.9 Caching

- User preferences (quiet hours, custom priority, digest enabled): cached per user, 10-minute TTL
- AI priority classification: not cached (each notification classified individually)
- Batch count per hour: cached per user, 1-hour TTL

### 8.10 Transactions

- Priority assignment: single transaction (update notification priority)
- Batch creation: single transaction (create batch, update notification batch_id + scheduled_for)
- Daily digest: single transaction per user (create digest notification, mark low-priority as digested)

### 8.11 Rate Limiting

- Existing rate limiting preserved: 50/user/day, 10/category/hour
- Additional: max 3 batches per hour per user
- Critical notifications exempt from rate limiting

### 8.12 Configuration

- `SMART_NOTIFICATIONS_ENABLED` — default `true`; enable/disable feature
- `SMART_NOTIFICATION_BATCH_INTERVAL_MINUTES` — default `30`; batch processor interval
- `SMART_NOTIFICATION_MAX_BATCHES_PER_HOUR` — default `3`; max batches per user per hour
- `SMART_NOTIFICATION_BATCH_MIN_SIZE` — default `3`; min notifications to form a batch
- `SMART_NOTIFICATION_BATCH_MAX_DELAY_MINUTES` — default `60`; max delay before forcing batch
- `SMART_NOTIFICATION_HIGH_PRIORITY_DELAY_MINUTES` — default `5`; delay for high priority
- `SMART_NOTIFICATION_DIGEST_CRON` — default `0 0 8 * * *`; daily 8 AM IST
- `SMART_NOTIFICATION_QUIET_HOURS_DEFAULT_START` — default `21:00`
- `SMART_NOTIFICATION_QUIET_HOURS_DEFAULT_END` — default `07:00`

---

## 9. API Contracts

### 9.1 Notification Preferences (Modified)

```
GET /api/v1/parent/notification-preferences
  → 200: {
    categories: [...],
    quiet_hours_start: "21:00",
    quiet_hours_end: "07:00",
    daily_digest_enabled: true,
    category_overrides: [
      {category: "fees", custom_priority: "high"},
      {category: "announcement", custom_priority: "low"}
    ]
  }

PATCH /api/v1/parent/notification-preferences
  Body: {
    quiet_hours_start: "22:00",
    quiet_hours_end: "06:00",
    daily_digest_enabled: true,
    category_overrides: [
      {category: "fees", custom_priority: "high"},
      {category: "announcement", custom_priority: "low"}
    ]
  }
  → 200: { message: "Preferences updated" }
```

### 9.2 DTO Models

All `@Serializable`, wrapped in `ApiResponse<T>` pattern.

```kotlin
@Serializable data class NotificationPreferencesDto(
    val categories: List<CategoryPreference>,
    val quietHoursStart: String,
    val quietHoursEnd: String,
    val dailyDigestEnabled: Boolean,
    val categoryOverrides: List<CategoryOverride>,
)

@Serializable data class CategoryOverride(
    val category: String,
    val customPriority: String,  // critical | high | normal | low
)

@Serializable data class ProcessResult(
    val priority: String,
    val channels: List<String>,
    val batched: Boolean,
    val scheduledFor: String?,
)
```

---

## 10. Frontend Architecture

### 10.1 Screens

| Screen | Platform | Role | Description |
|---|---|---|---|
| `NotificationPreferencesScreen` (modified) | Compose | Parent | Add quiet hours settings, priority overrides, daily digest toggle |

### 10.2 Navigation

- Profile tab → Notification Preferences → `NotificationPreferencesScreen`

### 10.3 UX Flows

#### Parent: Configure Quiet Hours
1. Parent opens Notification Preferences
2. Sets quiet hours start (e.g., 22:00) and end (e.g., 06:00)
3. Saves preferences
4. Non-critical notifications queued during quiet hours

#### Parent: Override Priority for Category
1. Parent opens Notification Preferences
2. Sees category list with AI-assigned priority
3. Overrides priority for specific category (e.g., fees → "high")
4. Saves preferences
5. Future notifications in that category use custom priority

#### Parent: Enable/Disable Daily Digest
1. Parent opens Notification Preferences
2. Toggles daily digest on/off
3. If on, receives one summary notification at 8 AM IST

### 10.4 State Management

```kotlin
data class NotificationPreferencesState(
    val quietHoursStart: String,
    val quietHoursEnd: String,
    val dailyDigestEnabled: Boolean,
    val categoryOverrides: List<CategoryOverride>,
    val isLoading: Boolean,
    val error: String?,
)
```

### 10.5 Offline Support

- Preferences cached locally
- Changes queued and synced when online

### 10.6 Loading States

- Loading preferences: "Loading notification preferences..."
- Saving: "Saving..."

### 10.7 Error Handling (UI)

- Save failure: "Failed to save preferences. Please try again."
- Invalid quiet hours: "Quiet hours start must be before end."

### 10.8 Component Integration Guidelines

| Rule | Description |
|---|---|
| **R1** | Quiet hours time picker with 24-hour format |
| **R2** | Priority override dropdown per category (critical/high/normal/low) |
| **R3** | Daily digest toggle switch |
| **R4** | Category list shows current AI priority + override (if set) |
| **R5** | Changes saved immediately on toggle, debounced on text input |

---

## 11. Shared Module Changes (KMP)

### 11.1 DTOs

All DTOs defined in section 9.2, placed in `shared/.../notifications/domain/model/NotificationModels.kt`.

### 11.2 Domain Models

```kotlin
data class SmartNotificationPreferences(
    val quietHoursStart: String,
    val quietHoursEnd: String,
    val dailyDigestEnabled: Boolean,
    val categoryOverrides: List<CategoryOverride>,
)

data class CategoryOverride(
    val category: String,
    val customPriority: NotificationPriority,
)

enum class NotificationPriority { CRITICAL, HIGH, NORMAL, LOW }
```

### 11.3 Repository Interfaces

```kotlin
interface NotificationPreferencesRepository {
    suspend fun getPreferences(token: String): NetworkResult<NotificationPreferencesDto>
    suspend fun updatePreferences(token: String, request: NotificationPreferencesDto): NetworkResult<Unit>
}
```

### 11.4 UseCases

- `GetNotificationPreferencesUseCase`
- `UpdateNotificationPreferencesUseCase`

### 11.5 Validation

- Quiet hours start/end: valid 24-hour time format (HH:mm)
- Custom priority: one of critical, high, normal, low
- Daily digest enabled: boolean

### 11.6 Serialization

Standard Kotlinx serialization. `NotificationPriority` enum serialized as lowercase string.

### 11.7 Network APIs

Ktor `@Resource` route definitions added to `NotificationApi.kt`:
- GET/PATCH notification preferences (modified to include new fields)

### 11.8 Database Models (Local Cache)

- Preferences cached locally per user
- Offline changes queued for sync

---

## 12. Permissions Matrix

| Action | Super Admin | School Admin | Teacher | Parent |
|---|---|---|---|---|
| View notification preferences | N/A | N/A | ✅ (own) | ✅ (own) |
| Update notification preferences | N/A | N/A | ✅ (own) | ✅ (own) |
| Configure quiet hours | N/A | N/A | ✅ (own) | ✅ (own) |
| Override priority per category | N/A | N/A | ✅ (own) | ✅ (own) |
| Enable/disable daily digest | N/A | N/A | ✅ (own) | ✅ (own) |
| View notification analytics | ✅ | ✅ | ❌ | ❌ |

---

## 13. Notifications

### Meta-Notification: How Smart Notifications Handle Their Own Notifications

Smart Notifications is itself a notification system. The key insight is that `SmartNotificationService` wraps the existing `Notify.kt` dispatch:

1. System event creates notification → `SmartNotificationService.processNotification()`
2. Priority assigned (AI or override)
3. Routed based on priority:
   - Critical: `Notify.kt` dispatch immediately + WhatsApp + FCM + in-app
   - High: `NotificationScheduler.kt` schedules for +5 min, then `Notify.kt` dispatch via FCM + in-app
   - Normal: stored with `scheduled_for = NULL`, batch processor picks up every 30 min
   - Low: stored in-app only, daily digest job picks up at 8 AM

### Notification Channels by Priority

| Priority | WhatsApp | FCM Push | In-App | Daily Digest |
|---|---|---|---|---|
| Critical | ✅ (immediate) | ✅ (immediate) | ✅ (immediate) | ❌ |
| High | ❌ | ✅ (within 5 min) | ✅ (immediate) | ❌ |
| Normal | ❌ | ✅ (batched, max 3/hr) | ✅ (immediate) | ❌ |
| Low | ❌ | ❌ | ✅ (immediate) | ✅ (8 AM digest) |

---

## 14. Background Jobs

### BatchProcessorJob

| Property | Value |
|---|---|
| **Name** | `BatchProcessorJob` |
| **Schedule** | Every 30 minutes |
| **Duration** | < 5 minutes |
| **Retry** | 3 attempts with 5-minute intervals |

#### Job Flow

1. Query `notifications` where `priority = 'normal'` AND `scheduled_for IS NULL` AND `created_at < now() - 5 min`
2. Group by `user_id`
3. For each user:
   a. Check quiet hours — if active, queue for later
   b. Check rate limit — if ≥ 3 batches in last hour, defer
   c. If ≥ 3 pending normal notifications → create batch
   d. Generate AI summary of batched notifications
   e. Send as one FCM notification
   f. Update notifications with `batch_id` and `scheduled_for`
4. If < 3 pending and age > 55 min → force batch (max 1 hour delay)
5. Return total batches created

### DailyDigestJob

| Property | Value |
|---|---|
| **Name** | `DailyDigestJob` |
| **Schedule** | Daily 8 AM IST |
| **Duration** | < 10 minutes for all users |
| **Retry** | 3 attempts with 10-minute intervals |

#### Job Flow

1. Query all users with `daily_digest_enabled = true`
2. For each user:
   a. Collect low-priority notifications from previous 24 hours
   b. If no low-priority notifications → skip
   c. Generate AI summary of all low-priority notifications
   d. Create one digest notification (in-app + FCM)
   e. Mark low-priority notifications as digested
3. Return total digests sent

### QuietHoursFlushJob

| Property | Value |
|---|---|
| **Name** | `QuietHoursFlushJob` |
| **Schedule** | Triggered at each user's quiet hours end |
| **Duration** | < 1 minute per user |
| **Retry** | None (next cycle will pick up) |

#### Job Flow

1. For each user whose quiet hours just ended:
   a. Query queued notifications (`scheduled_for IS NULL` AND `priority != 'critical'` AND created during quiet hours)
   b. Batch and send (same as BatchProcessorJob)

---

## 15. Integrations

### Internal Integrations

| System | Integration Point | Direction | Protocol | Error Handling |
|---|---|---|---|---|
| `Notify.kt` | Notification dispatch | Call | Direct call | Existing error handling preserved |
| `NotificationScheduler.kt` | Scheduling delayed sends | Call | Direct call | Log on failure |
| `NotificationService.kt` | FCM push dispatch | Call | Direct call | Log on failure; non-blocking |
| `NotificationPreferencesTable` | User preferences (quiet hours, overrides) | Read | Direct DB | Default values if not set |
| `NotificationsTable` | Notification records | Read/Write | Direct DB | Standard error handling |
| `AiService` | Priority classification | Call | HTTP API | Fallback to 'normal' on failure |
| WhatsApp gateway | Critical notification channel | Outbound | HTTP API | Log on failure; FCM still sent |

### External Integrations

| System | Purpose | Direction | Protocol | Authentication | Error Handling |
|---|---|---|---|---|---|
| AI Service (LLM) | Priority classification | Outbound | HTTP API | Bearer token | Fallback to 'normal' |
| WhatsApp Gateway | Critical notifications | Outbound | HTTP API | Bearer token (existing) | Log; FCM fallback |
| FCM | Push notifications | Outbound | HTTP API | Server key (existing) | Existing retry logic |

### Integration Patterns

- **AI priority:** `AiService.classify(content, category)` → returns priority string. Fallback to 'normal' on any error.
- **WhatsApp:** Only for critical notifications. Reuses existing WhatsApp gateway.
- **FCM:** Existing `NotificationService.sendPushNotification()` for all push delivery.
- **Scheduler:** `NotificationScheduler.kt` for delayed sends (high priority 5-min delay).

---

## 16. Security

### Authentication

- Notification preferences API: JWT auth via `requireAuth()`
- Notification dispatch: system-internal, no user auth

### Authorization

- User can only view/edit own notification preferences
- No cross-user preference access

### Data Protection

- Notification content — standard PII (student names, grades, fee amounts)
- Quiet hours preferences — non-sensitive user configuration
- AI classification — content sent to LLM API, no PII beyond notification content

### Input Validation

- Quiet hours start/end: valid 24-hour time format (HH:mm)
- Custom priority: one of critical, high, normal, low
- Daily digest enabled: boolean
- Category: valid notification category string

### Rate Limiting

- Existing: 50/user/day, 10/category/hour
- Additional: max 3 batches per hour per user
- Critical notifications exempt from all rate limiting

### Audit Logging

- Priority assignment: notification ID, assigned priority, source (AI/override)
- Batch creation: batch ID, user ID, notification IDs, summary
- Daily digest: user ID, notification count, digest sent
- Quiet hours queue: user ID, notification ID, queued at

### PII Handling

- Notification content sent to AI service for classification — includes student names, grades, fee amounts
- AI service should be configured to not store/retain content
- Quiet hours preferences — non-sensitive

### Multi-tenant Isolation

- `notifications.school_id` — existing, school-scoped
- `notification_batches.school_id` — school-scoped
- All queries filtered by `school_id`
- No cross-school notification access

---

## 17. Performance & Scalability

### Expected Scale

- Small school: 500 notifications/day, 200 users
- Medium school: 2,000 notifications/day, 500 users
- Large school: 5,000 notifications/day, 1,000 users
- Multi-school: 50,000 notifications/day across 20 schools

### Query Optimization

- **Batch processor:** `notifications(user_id, priority, scheduled_for)` index. Groups by user. O(n) per batch cycle.
- **Daily digest:** `notifications(user_id, priority, created_at)` index. Collects low-priority from last 24 hours.
- **Quiet hours check:** `notification_preferences(user_id)` lookup. O(1) per user.

### Indexing Strategy

- `notifications(user_id, priority, scheduled_for)` — for batch processor and digest queries
- `notifications(batch_id)` — for grouping
- `notification_batches(user_id, sent_at)` — for rate limiting checks

### Caching Strategy

- User preferences: cached per user, 10-minute TTL
- Batch count per hour: cached per user, 1-hour TTL
- AI classification: not cached (each notification unique)

### Pagination

N/A — batch processor and digest job process all pending notifications. No pagination needed.

### Connection Pooling

Uses existing HikariCP connection pool. No additional pooling needed.

### Async Processing

- AI classification: async (non-blocking, fallback to 'normal' on timeout)
- Batch processor: sequential per user, parallel across users
- Daily digest: sequential per user, parallel across users
- Notification dispatch: async (existing `NotificationService` pattern)

### Scalability Concerns

- AI classification latency: < 2 seconds per notification. 50,000 notifications/day = ~28 hours of AI time. Need parallel processing or batch classification.
- Batch processor: 50,000 notifications/day / 48 cycles = ~1,000 per cycle. Manageable.
- Daily digest: 10,000 users × 5 low-priority notifications each = 50,000 notifications to summarize. AI generates one summary per user. 10,000 AI calls in < 10 minutes = ~17 calls/second. Feasible with parallel processing.

---

## 18. Edge Cases

| # | Scenario | Expected Behavior |
|---|---|---|
| EC-1 | AI service unavailable | Fallback to 'normal' priority. Log error. |
| EC-2 | AI service timeout (> 2 seconds) | Fallback to 'normal' priority. Log timeout. |
| EC-3 | User has no quiet hours configured | Use defaults: 21:00 - 07:00 |
| EC-4 | User has quiet hours start > end (e.g., 22:00 - 06:00, crossing midnight) | Handle overnight quiet hours correctly |
| EC-5 | Critical notification during quiet hours | Send immediately. Bypass quiet hours. |
| EC-6 | User has 0 low-priority notifications | Daily digest skipped for that user. |
| EC-7 | User has daily_digest_enabled = false | Daily digest skipped. Low-priority notifications remain in-app only. |
| EC-8 | Batch processor finds 0 pending normal notifications | No batches created. Log "No pending notifications." |
| EC-9 | User has custom_priority set for category | AI classification skipped for that category. Custom priority used. |
| EC-10 | WhatsApp gateway unavailable for critical notification | FCM + in-app still sent. Log WhatsApp failure. |
| EC-11 | Rate limit exceeded (3 batches/hour) | Defer batch to next hour. Log deferral. |
| EC-12 | Notification has no category | AI classifies based on content only. Default to 'normal' if AI unavailable. |
| EC-13 | Multiple critical notifications at same time | All sent immediately. No batching for critical. |
| EC-14 | User changes quiet hours while notifications are queued | Queued notifications re-evaluated against new quiet hours. |
| EC-15 | Batch processor and daily digest run simultaneously | No conflict — batch processor handles 'normal', digest handles 'low'. |
| EC-16 | Notification created with priority already set (system override) | AI classification skipped. Use provided priority. |
| EC-17 | User has no notification preferences row | Create with defaults. All categories enabled, quiet hours 21:00-07:00, digest enabled. |
| EC-18 | AI generates invalid priority (not in enum) | Fallback to 'normal'. Log error. |

---

## 19. Error Handling

### Error Response Format

Standard `ApiResponse` error format for preferences API. Internal errors logged.

### Error Codes

| Code | HTTP Status | Description | User Message |
|---|---|---|---|
| `INVALID_QUIET_HOURS` | 400 | Quiet hours format invalid | "Quiet hours must be in HH:mm format." |
| `INVALID_PRIORITY` | 400 | Custom priority not in enum | "Priority must be one of: critical, high, normal, low." |
| `PREFERENCES_NOT_FOUND` | 404 | No preferences row for user | "Notification preferences not found." |
| `AI_CLASSIFICATION_FAILED` | 500 | AI service error | (Internal, logged, not shown to user) |

### Error Handling Strategy

- **AI service errors:** Fallback to 'normal' priority. Log error. Never block notification delivery.
- **WhatsApp gateway errors:** FCM + in-app still sent. Log failure.
- **FCM errors:** Existing `NotificationService` error handling. Log failure.
- **Batch processor errors:** Log error. Retry next cycle.
- **Daily digest errors:** Log error. Retry next day.
- **Preferences API errors:** Return 400/404/500 with clear message.

### Retry Strategy

- AI classification: no retry (fallback to 'normal')
- WhatsApp: 3 retries with 5-second intervals (existing)
- FCM: existing retry logic
- Batch processor: retried next cycle (30 min)
- Daily digest: retried next day

### Fallback Behavior

- AI unavailable: 'normal' priority
- WhatsApp unavailable: FCM + in-app only
- FCM unavailable: in-app only
- Batch processor failure: notifications remain pending, processed next cycle
- Daily digest failure: low-priority notifications remain in-app, digest retried next day

---

## 20. Analytics & Reporting

### Analytics Dashboard Data

| Metric | Source | Derivation |
|---|---|---|
| Total notifications by priority | `notifications.priority` | Group by priority, count |
| AI classification accuracy | Manual audit sample | Percentage of correct AI classifications |
| AI classification rate | `notifications.ai_priority_assigned` | true / total |
| Batch rate | `notification_batches` count / total normal notifications | Percentage batched |
| Avg batch size | `notification_batches.notification_ids` | Avg notifications per batch |
| Daily digest opt-in rate | `notification_preferences.daily_digest_enabled` | true / total users |
| Quiet hours adoption | `notification_preferences.quiet_hours_start` != default | Percentage with custom quiet hours |
| Custom priority override rate | `notification_preferences.custom_priority` | Percentage with overrides |
| Notification delivery by channel | FCM/WhatsApp/in-app counts | Per channel |

### Export Capabilities

- Notification analytics export (CSV) — date, priority, category, channel, delivery status

### Report Types

| Report | Format | Frequency | Recipient |
|---|---|---|---|
| Notification summary | JSON (API) | On-demand | School Admin |
| AI classification audit | JSON (API) | Weekly | Dev Team |
| Delivery metrics | JSON (API) | On-demand | Dev Team |

---

## 21. Testing Strategy

### Unit Tests

- `SmartNotificationService.processNotification()` — priority assignment (AI, override, fallback)
- `SmartNotificationService.batchProcessor()` — batching logic (≥3, <3, rate limit, quiet hours)
- `SmartNotificationService.dailyDigest()` — digest generation, skip disabled users
- Quiet hours: within quiet hours, outside quiet hours, overnight crossing midnight
- Rate limiting: max 3 batches/hour, critical exempt
- AI fallback: AI unavailable → 'normal', AI timeout → 'normal', AI invalid → 'normal'

### Integration Tests

- Full notification lifecycle: create → AI classify → route → deliver
- Batch lifecycle: 3 normal notifications → batch processor → batch created → FCM sent
- Daily digest: 5 low-priority notifications → digest job → one summary notification sent
- Quiet hours: notification during quiet hours → queued → flushed at quiet hours end
- Custom override: user sets fees → 'high' → fee notification gets 'high' priority (no AI call)
- Critical bypass: critical notification during quiet hours → sent immediately

### E2E Tests

- Parent configures quiet hours → receives no non-critical notifications during quiet hours → receives batched notifications after quiet hours end
- Parent enables daily digest → receives one summary at 8 AM → low-priority notifications included

### Performance Tests

- AI classification: < 2 seconds per notification
- Batch processor: 1,000 pending notifications in < 1 minute
- Daily digest: 1,000 users in < 5 minutes
- Critical notification delivery: < 30 seconds end-to-end

### Test Data

- 10 sample notifications (2 critical, 2 high, 4 normal, 2 low)
- 3 sample users with different preferences (custom quiet hours, overrides, digest disabled)
- Mock AI service (returns predefined priorities)
- Mock WhatsApp gateway
- Mock FCM service

### Test Environment

- Test database with schema migration applied
- Mock AI service for priority classification
- Mock WhatsApp gateway
- Mock FCM service
- Test JWT tokens for parent role

---

## 22. Acceptance Criteria

- [ ] AI assigns priority to notifications correctly (critical/high/normal/low)
- [ ] Critical notifications sent immediately via WhatsApp + FCM + in-app
- [ ] High priority sent within 5 minutes via FCM + in-app
- [ ] Normal notifications batched (max 3/hour) via FCM
- [ ] Low priority: in-app only, included in daily digest
- [ ] Quiet hours respected (non-critical queued)
- [ ] Quiet hours configurable per user (default 9 PM - 7 AM)
- [ ] Daily digest sent at 8 AM IST
- [ ] User can override priority per category
- [ ] Notification grouping works for same-category notifications within 30 min
- [ ] AI fallback to 'normal' when AI service unavailable
- [ ] Existing rate limiting preserved (50/user/day, 10/category/hour)
- [ ] Critical notifications bypass quiet hours and rate limiting
- [ ] Daily digest can be disabled by user
- [ ] Batch processor runs every 30 minutes

---

## 23. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration `migration_070_smart_notifications.sql`: modify `notifications` + `notification_preferences`, create `notification_batches` |
| 2 | 2 days | `SmartNotificationService.kt`: priority assignment, routing, batching, quiet hours |
| 3 | 1 day | AI priority classification prompt + integration with `AiService` |
| 4 | 2 days | `BatchProcessorJob.kt` + `DailyDigestJob.kt` + `QuietHoursFlushJob.kt` |
| 5 | 1 day | API endpoints: modify `NotificationPreferencesRouting.kt` for new fields |
| 6 | 2 days | Client UI: modify `NotificationPreferencesScreen.kt` (quiet hours, priority overrides, digest toggle) |
| 7 | 2 days | Tests: unit, integration, E2E |

### Pre-Implementation Checklist

- [ ] Verify `AiService` API for classification
- [ ] Verify WhatsApp gateway supports programmatic message sending
- [ ] Verify `NotificationScheduler.kt` supports delayed scheduling
- [ ] Verify `NotificationService.kt` FCM dispatch API
- [ ] Verify existing rate limiting in `Notify.kt` is not bypassed

---

## 24. File-Level Impact Analysis

### Server

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Modify + Add | Columns on `NotificationsTable` + `NotificationPreferencesTable`, new `NotificationBatchesTable` |
| `server/.../db/DatabaseFactory.kt` | Modify | Register `NotificationBatchesTable` in `allTables` |
| `server/.../feature/notifications/SmartNotificationService.kt` | **New** | Core service: priority, routing, batching, quiet hours |
| `server/.../feature/notifications/Notify.kt` | Modify | Route through `SmartNotificationService` for priority assignment |
| `server/.../feature/notifications/BatchProcessorJob.kt` | **New** | Batch processing job (every 30 min) |
| `server/.../feature/notifications/DailyDigestJob.kt` | **New** | Daily digest job (8 AM IST) |
| `server/.../feature/notifications/QuietHoursFlushJob.kt` | **New** | Quiet hours flush job |
| `server/.../feature/notifications/NotificationPreferencesRouting.kt` | Modify | Add quiet hours, custom priority, daily digest fields |
| `docs/db/migration_070_smart_notifications.sql` | **New** | DDL: ALTER tables + CREATE `notification_batches` |

### Shared (KMP)

| File | Change Type | Description |
|---|---|---|
| `shared/.../notifications/domain/model/NotificationModels.kt` | Modify | Add `SmartNotificationPreferences`, `CategoryOverride`, `NotificationPriority` enum |
| `shared/.../notifications/domain/repository/NotificationPreferencesRepository.kt` | Modify | Add new preference fields |
| `shared/.../notifications/data/remote/NotificationApi.kt` | Modify | Add new fields to PATCH request |

### Client (Compose)

| File | Change Type | Description |
|---|---|---|
| `composeApp/.../ui/v2/screens/parent/NotificationPreferencesScreen.kt` | Modify | Add quiet hours picker, priority override dropdowns, daily digest toggle |

---

## 25. Future Enhancements

| # | Enhancement | Priority | Effort | Notes |
|---|---|---|---|---|
| F-1 | Notification content generation | Medium | L | AI writes notification content (not just classifies) |
| F-2 | Cross-school notification aggregation | Low | M | Aggregate notifications for parents with children in multiple schools |
| F-3 | Notification analytics for parents | Low | S | Show parents their notification stats (received, read rate) |
| F-4 | Smart channel learning | Medium | M | AI learns best channel per user based on engagement |
| F-5 | Notification scheduling | Low | S | Schedule notifications for optimal delivery time per user |
| F-6 | Rich media notifications | Low | M | Image/video attachments in notifications |
| F-7 | Notification templates | Low | S | Pre-defined templates for common notification types |
| F-8 | Batch summary personalization | Medium | S | AI personalizes batch summary per user's interests |
| F-9 | Notification priority feedback | Medium | S | Users rate notification relevance to improve AI |
| F-10 | Multi-language notification classification | Medium | M | AI classifies priority in multiple languages |

---

## Appendix A: Sequence Diagrams

### A.1 Notification Processing Flow

```
System Event    SmartNotificationService    AiService    Notify.kt    WhatsApp    FCM
  │                    │                       │            │           │          │
  │  create notif      │                       │            │           │          │
  │  ────────────────> │                       │            │           │          │
  │                    │──check user override──│            │           │          │
  │                    │  (no override)        │            │           │          │
  │                    │──classify(content)──→ │            │           │          │
  │                    │←──priority="high"──── │            │           │          │
  │                    │                       │            │           │          │
  │                    │  [if critical]        │            │           │          │
  │                    │──sendImmediately──────│──────────→│           │          │
  │                    │                       │            │──send──→│          │
  │                    │                       │            │──send─────────────→│
  │                    │                       │            │           │          │
  │                    │  [if high]            │            │           │          │
  │                    │──scheduleSend(+5min)──│            │           │          │
  │                    │                       │            │           │          │
  │                    │  [if normal]          │            │           │          │
  │                    │──batchNotification────│            │           │          │
  │                    │  (wait for batch)     │            │           │          │
  │                    │                       │            │           │          │
  │                    │  [if low]             │            │           │          │
  │                    │──storeInAppOnly──────│            │           │          │
  │                    │  (wait for digest)    │            │           │          │
  │                    │                       │            │           │          │
```

### A.2 Batch Processor Flow

```
BatchProcessorJob    NotificationsTable    AiService    NotificationService
  │                       │                    │              │
  │──query pending────────→│                    │              │
  │←──notifications────────│                    │              │
  │  [group by user]       │                    │              │
  │  [user has ≥3]         │                    │              │
  │──generate summary──────────────────────────→│              │
  │←──summary text─────────│                    │              │
  │──create batch──────────│                    │              │
  │──update notifs─────────│                    │              │
  │──send FCM─────────────────────────────────────────────────→│
  │←──sent────────────────────────────────────────────────────│
  │                        │                    │              │
```

### A.3 Daily Digest Flow

```
DailyDigestJob    NotificationsTable    AiService    NotificationService
  │                    │                    │              │
  │  [8 AM IST]        │                    │              │
  │──query users──────→│                    │              │
  │←──users list───────│                    │              │
  │  [for each user]   │                    │              │
  │──query low-priority→│                    │              │
  │←──notifications────│                    │              │
  │──generate summary──────────────────────→│              │
  │←──digest text──────│                    │              │
  │──create digest notif│                   │              │
  │──send FCM + in-app─────────────────────────────────────→│
  │←──sent─────────────────────────────────────────────────│
  │  [next user]       │                    │              │
```

---

## Appendix B: Domain Model / ER Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                       notifications (modified)                        │
│  id (PK)                                                              │
│  user_id, school_id                                                   │
│  category, title, body                                                │
│  isRead, createdAt                                                    │
│  priority (NEW: critical|high|normal|low)                             │
│  batch_id (NEW: FK→notification_batches)                              │
│  scheduled_for (NEW: when to send)                                    │
│  ai_priority_assigned (NEW: bool)                                     │
│  INDEX: (user_id, priority, scheduled_for)                            │
└──────────────────────────┬───────────────────────────────────────────┘
                           │
                           │ N:1
                           ▼
┌──────────────────────────────────────────────────────────────────────┐
│                   notification_batches (new)                          │
│  id (PK)                                                              │
│  user_id, school_id                                                   │
│  notification_ids (JSON array)                                        │
│  summary_text (AI-generated)                                          │
│  sent_at, created_at                                                  │
│  INDEX: (user_id, sent_at)                                            │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│              notification_preferences (modified)                      │
│  user_id, category (existing)                                         │
│  enabled, sound (existing)                                            │
│  quiet_hours_start (NEW: default '21:00')                             │
│  quiet_hours_end (NEW: default '07:00')                               │
│  custom_priority (NEW: override AI priority)                          │
│  daily_digest_enabled (NEW: default true)                             │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Appendix C: Event Flow

### Domain Events

| Event | Emitter | Consumers | Payload | Side Effects |
|---|---|---|---|---|
| `NotificationPriorityAssigned` | `SmartNotificationService.processNotification()` | None (logged) | `notificationId, priority, source (ai/override)` | Notification routed |
| `NotificationBatched` | `SmartNotificationService.batchProcessor()` | None (logged) | `batchId, userId, notificationIds` | Batch FCM sent |
| `NotificationDelivered` | `SmartNotificationService` | None (logged) | `notificationId, channels[]` | Delivery confirmed |
| `DailyDigestSent` | `DailyDigestJob` | None (logged) | `userId, notificationCount, digestText` | Digest FCM + in-app sent |
| `QuietHoursQueued` | `SmartNotificationService` | None (logged) | `notificationId, userId, queuedUntil` | Notification queued |

### Event Delivery Guarantees

- Events emitted synchronously within service methods
- All events logged for audit
- No external consumers — events are internal audit trail

### AI Priority Classification Prompt

```
System: Classify this school notification's priority.
- critical: urgent safety, fee deadline today, exam cancellation, emergency
- high: exam results, fee reminder (within 3 days), PTM tomorrow
- normal: homework assigned, attendance marked, announcement
- low: general announcement, event promotion, newsletter

Output: single word (critical | high | normal | low)

User: Category: {{category}}
Title: {{title}}
Body: {{body}}
```

---

## Appendix D: Configuration

### Environment Variables

| Variable | Default | Description |
|---|---|---|
| `SMART_NOTIFICATIONS_ENABLED` | `true` | Enable/disable smart notifications |
| `SMART_NOTIFICATION_BATCH_INTERVAL_MINUTES` | `30` | Batch processor interval |
| `SMART_NOTIFICATION_MAX_BATCHES_PER_HOUR` | `3` | Max batches per user per hour |
| `SMART_NOTIFICATION_BATCH_MIN_SIZE` | `3` | Min notifications to form a batch |
| `SMART_NOTIFICATION_BATCH_MAX_DELAY_MINUTES` | `60` | Max delay before forcing batch |
| `SMART_NOTIFICATION_HIGH_PRIORITY_DELAY_MINUTES` | `5` | Delay for high priority |
| `SMART_NOTIFICATION_DIGEST_CRON` | `0 0 8 * * *` | Daily 8 AM IST |
| `SMART_NOTIFICATION_QUIET_HOURS_DEFAULT_START` | `21:00` | Default quiet hours start |
| `SMART_NOTIFICATION_QUIET_HOURS_DEFAULT_END` | `07:00` | Default quiet hours end |
| `SMART_NOTIFICATION_AI_TIMEOUT_MS` | `2000` | AI classification timeout |

### Feature Flags

| Flag | Default | Description |
|---|---|---|
| `SMART_NOTIFICATIONS_ENABLED` | `true` | Enable/disable smart notifications |
| `AI_PRIORITY_CLASSIFICATION_ENABLED` | `true` | Enable/disable AI priority assignment |
| `BATCH_PROCESSOR_ENABLED` | `true` | Enable/disable batch processor |
| `DAILY_DIGEST_ENABLED` | `true` | Enable/disable daily digest job |
| `QUIET_HOURS_ENABLED` | `true` | Enable/disable quiet hours |
| `WHATSAPP_CRITICAL_ENABLED` | `true` | Enable/disable WhatsApp for critical |

### School-Level Settings

N/A — notification preferences are per-user. No school-level configuration.

---

## Appendix E: Migration & Rollback

### Migration: `migration_070_smart_notifications.sql`

```sql
-- Migration 070: Smart Notifications
-- Modifies notifications + notification_preferences, creates notification_batches

BEGIN;

-- Add columns to notifications
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS priority VARCHAR(16) NOT NULL DEFAULT 'normal';
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS batch_id UUID;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS scheduled_for TIMESTAMP;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS ai_priority_assigned BOOLEAN NOT NULL DEFAULT false;

-- Add columns to notification_preferences
ALTER TABLE notification_preferences ADD COLUMN IF NOT EXISTS quiet_hours_start VARCHAR(8) DEFAULT '21:00';
ALTER TABLE notification_preferences ADD COLUMN IF NOT EXISTS quiet_hours_end VARCHAR(8) DEFAULT '07:00';
ALTER TABLE notification_preferences ADD COLUMN IF NOT EXISTS custom_priority VARCHAR(16);
ALTER TABLE notification_preferences ADD COLUMN IF NOT EXISTS daily_digest_enabled BOOLEAN NOT NULL DEFAULT true;

-- Create notification_batches table
CREATE TABLE IF NOT EXISTS notification_batches (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,
    school_id       UUID NOT NULL,
    notification_ids TEXT NOT NULL,
    summary_text    TEXT NOT NULL,
    sent_at         TIMESTAMP NOT NULL DEFAULT now(),
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

-- Add indexes
CREATE INDEX IF NOT EXISTS idx_notifications_priority_scheduled
    ON notifications (user_id, priority, scheduled_for);
CREATE INDEX IF NOT EXISTS idx_notifications_batch_id
    ON notifications (batch_id);
CREATE INDEX IF NOT EXISTS idx_notification_batches_user_sent
    ON notification_batches (user_id, sent_at);

COMMIT;
```

### Rollback: `migration_070_rollback.sql`

```sql
BEGIN;
DROP TABLE IF EXISTS notification_batches;
ALTER TABLE notifications DROP COLUMN IF EXISTS priority;
ALTER TABLE notifications DROP COLUMN IF EXISTS batch_id;
ALTER TABLE notifications DROP COLUMN IF EXISTS scheduled_for;
ALTER TABLE notifications DROP COLUMN IF EXISTS ai_priority_assigned;
ALTER TABLE notification_preferences DROP COLUMN IF EXISTS quiet_hours_start;
ALTER TABLE notification_preferences DROP COLUMN IF EXISTS quiet_hours_end;
ALTER TABLE notification_preferences DROP COLUMN IF EXISTS custom_priority;
ALTER TABLE notification_preferences DROP COLUMN IF EXISTS daily_digest_enabled;
COMMIT;
```

### Migration Validation

- Verify `priority`, `batch_id`, `scheduled_for`, `ai_priority_assigned` columns on `notifications`
- Verify `quiet_hours_start`, `quiet_hours_end`, `custom_priority`, `daily_digest_enabled` on `notification_preferences`
- Verify `notification_batches` table created with correct columns
- Verify indexes created
- Run `SELECT count(*) FROM notification_batches` — should be 0 (new feature)
- Verify existing notifications have `priority = 'normal'` (backfill)

---

## Appendix F: Observability

### Structured Logging

| Log Level | Event | Context Fields |
|---|---|---|
| INFO | Notification priority assigned | `notificationId, priority, source (ai/override/fallback)` |
| INFO | Notification batched | `batchId, userId, notificationCount` |
| INFO | Notification delivered | `notificationId, channels[]` |
| INFO | Daily digest sent | `userId, notificationCount` |
| INFO | Batch processor completed | `batchesCreated, durationMs` |
| INFO | Daily digest job completed | `digestsSent, durationMs` |
| WARN | AI classification fallback | `notificationId, reason (timeout/error/invalid)` |
| WARN | Quiet hours queue | `notificationId, userId, queuedUntil` |
| WARN | Rate limit deferred | `userId, batchDeferredTo` |
| WARN | WhatsApp delivery failed | `notificationId, error` |
| ERROR | AI service unavailable | `error, stackTrace` |
| ERROR | Batch processor failed | `error, stackTrace` |
| ERROR | Daily digest job failed | `error, stackTrace` |

### Metrics

| Metric | Type | Labels | Description |
|---|---|---|---|
| `notifications_total` | Counter | `school_id, priority, category` | Total notifications by priority and category |
| `notifications_batched_total` | Counter | `school_id` | Total notifications batched |
| `notification_batches_total` | Counter | `school_id` | Total batches created |
| `daily_digests_sent_total` | Counter | `school_id` | Total daily digests sent |
| `ai_classifications_total` | Counter | `source (ai/override/fallback)` | AI classification count by source |
| `ai_classification_duration` | Histogram | — | AI classification latency |
| `batch_processor_duration` | Histogram | — | Batch processor job duration |
| `daily_digest_duration` | Histogram | — | Daily digest job duration |
| `quiet_hours_queued_total` | Counter | `school_id` | Notifications queued due to quiet hours |
| `notification_delivery_rate` | Gauge | `channel (whatsapp/fcm/in_app)` | Delivery success rate per channel |

### Health Checks

| Check | Endpoint | Description |
|---|---|---|
| Smart notifications enabled | `/health/smart-notifications` | Verify feature flag enabled and tables accessible |
| AI service available | `/health/ai-service` | Verify AI service reachable |
| Batch processor running | `/health/batch-processor` | Verify last run within 35 minutes |
| Daily digest running | `/health/daily-digest` | Verify last run within 24 hours |

### Alerts

| Alert | Condition | Severity | Notification |
|---|---|---|---|
| AI service down | `ai_classifications_total{source="fallback"}` > 50% | Critical | Email + Slack to dev team |
| Batch processor not running | No `batch_processor_duration` in 35 min | Warning | Email to dev team |
| Daily digest not running | No `daily_digest_duration` in 24 hours | Warning | Email to dev team |
| WhatsApp delivery failure rate | WhatsApp failure rate > 10% | Warning | Email to dev team |
| AI classification latency | `ai_classification_duration` > 5 seconds | Warning | Email to dev team |

### Dashboards

| Dashboard | Panels | Audience |
|---|---|---|
| Smart Notifications Overview | Total by priority, batch rate, digest opt-in rate | Product Team |
| AI Classification | Accuracy, latency, fallback rate, override rate | Dev Team |
| Delivery Metrics | By channel (WhatsApp, FCM, in-app), success rate | Dev Team |
| Batch Processor | Batches created, avg batch size, duration | Dev Team |
| Daily Digest | Digests sent, avg notifications per digest, opt-in rate | Product Team |

### Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| AI service unavailable | Medium | Medium | Fallback to 'normal' priority. Non-blocking. |
| AI classifies critical notification as 'low' | Low | High | User overrides for critical categories (fees, safety). Manual audit sample. |
| Batch processor delays important notifications | Low | Medium | Max 1 hour delay for normal. Critical bypasses batching. |
| Quiet hours miss time zone differences | Medium | Low | Quiet hours stored in user's local time. Server converts to IST for job scheduling. |
| WhatsApp gateway rate limits | Low | Low | WhatsApp only for critical. FCM fallback. Existing rate limits. |
| Daily digest too large | Low | Low | AI summarizes into concise text. Max 500 characters. |
| Notification fatigue not reduced | Medium | Medium | Monitor engagement metrics. Adjust batch size and frequency. |
