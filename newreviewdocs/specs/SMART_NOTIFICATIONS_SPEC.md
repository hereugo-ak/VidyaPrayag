# Smart Notifications — Technical Specification

> **Document status:** Partial (65%) — preferences filtering, rate limiting, scheduler, push bridge implemented; AI priority, batching, quiet hours, daily digest TODO
> **Last updated:** 2026-06-28
> **Prerequisites:** `AI_INFRASTRUCTURE_SPEC.md`, `WHATSAPP_INTEGRATION_SPEC.md`
> **Source:** `DIFFERENTIATING_FEATURES.md` §4.1

---

## 1. Feature Overview

AI-powered notification prioritization, batching, and smart delivery. Instead of sending every notification immediately, the system groups, prioritizes, and times notifications to reduce notification fatigue while ensuring critical alerts are never missed.

### Goals

- Priority levels: critical (immediate), high (within 5 min), normal (batched), low (digest)
- Smart batching: non-critical notifications batched and sent together (max 3 per hour)
- AI priority assignment: LLM categorizes notification importance based on content + context
- Quiet hours: no non-critical notifications 9 PM - 7 AM (configurable)
- Smart channel selection: critical → WhatsApp + FCM, normal → FCM only, low → in-app only
- Notification grouping: related notifications grouped (e.g., 3 fee reminders → 1 grouped notification)

---

## 2. Current System Assessment

- `NotificationsTable` (`Tables.kt`) — per-recipient notifications with `category`, `isRead`
- `NotificationPreferencesTable` (`Tables.kt`) — per-category enable/disable + sound
- `Notify.kt` — now includes **preferences filtering** (drops recipients who disabled a category), **rate limiting** (max 50/user/day, max 10/category/hour), and **push bridge** (fire-and-forget FCM dispatch via `NotificationService`)
- `NotificationPreferencesRouting.kt` (`server/.../feature/notifications/`) — API for reading/updating per-category notification preferences
- `NotificationScheduler.kt` (`server/.../feature/notifications/`) — scheduling infrastructure for delayed/batched notifications
- `NotificationService.kt` (`server/.../feature/notification/service/`) — FCM push dispatch with `SendNotificationRequest`
- `DeviceTokenRepository.kt` — manages FCM device tokens
- `NotificationPermissionLauncher` — cross-platform permission request flow (Android, iOS, JVM, web)
- **Not yet implemented:** AI priority assignment, smart batching (grouping related notifications), quiet hours, daily digest, smart channel selection (WhatsApp + FCM routing), notification grouping
- `DIFFERENTIATING_FEATURES.md` §4.1: Smart Notifications, effort M

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | AI assigns priority (critical/high/normal/low) based on notification content + user context |
| FR-2 | Critical notifications sent immediately via WhatsApp + FCM + in-app |
| FR-3 | High priority sent within 5 minutes via FCM + in-app |
| FR-4 | Normal notifications batched (max 3 per hour) via FCM |
| FR-5 | Low priority: in-app only, included in daily digest |
| FR-6 | Quiet hours: 9 PM - 7 AM, non-critical notifications queued (configurable per user) |
| FR-7 | Notification grouping: same-category notifications within 30 min grouped |
| FR-8 | User can override: set custom priority for categories |
| FR-9 | Daily digest: all low-priority notifications sent as one summary at 8 AM |

---

## 4. Database Design

### 4.1 Modify Existing: `notifications`

```sql
ALTER TABLE notifications ADD COLUMN priority VARCHAR(16) NOT NULL DEFAULT 'normal';
-- critical | high | normal | low
ALTER TABLE notifications ADD COLUMN batch_id UUID;  -- grouped notifications share batch_id
ALTER TABLE notifications ADD COLUMN scheduled_for TIMESTAMP;  -- when to send (for batched/digest)
ALTER TABLE notifications ADD COLUMN ai_priority_assigned BOOLEAN NOT NULL DEFAULT false;
```

### 4.2 Modify Existing: `notification_preferences`

```sql
ALTER TABLE notification_preferences ADD COLUMN quiet_hours_start VARCHAR(8) DEFAULT '21:00';
ALTER TABLE notification_preferences ADD COLUMN quiet_hours_end VARCHAR(8) DEFAULT '07:00';
ALTER TABLE notification_preferences ADD COLUMN custom_priority VARCHAR(16); -- override AI priority
ALTER TABLE notification_preferences ADD COLUMN daily_digest_enabled BOOLEAN NOT NULL DEFAULT true;
```

### 4.3 New Table: `notification_batches`

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

---

## 5. Backend Architecture

### 5.1 SmartNotificationService

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

### 5.2 AI Priority Classification Prompt

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

### 5.3 Batching Logic

Every 30 minutes:
1. Query `notifications` where `priority = 'normal'` AND `scheduled_for IS NULL` AND `created_at < now() - 5 min`
2. Group by user_id
3. If user has ≥ 3 pending normal notifications → create batch, generate AI summary, send as one FCM
4. If < 3 → schedule for next batch cycle (max 1 hour delay)

### 5.4 Quiet Hours

- Check user's `quiet_hours_start` and `quiet_hours_end`
- If current time is within quiet hours AND priority != critical → queue notification
- At quiet hours end, flush queued notifications (batched)

---

## 6. API Contracts

```
# User preferences
GET /api/v1/parent/notification-preferences
PATCH /api/v1/parent/notification-preferences
{
  "quiet_hours_start": "22:00",
  "quiet_hours_end": "06:00",
  "daily_digest_enabled": true,
  "category_overrides": [
    {"category": "fees", "custom_priority": "high"},
    {"category": "announcement", "custom_priority": "low"}
  ]
}
```

---

## 7. Acceptance Criteria

- [ ] AI assigns priority to notifications correctly
- [ ] Critical notifications sent immediately via WhatsApp + FCM
- [ ] Normal notifications batched (max 3/hour)
- [ ] Low priority: in-app only, included in daily digest
- [ ] Quiet hours respected (non-critical queued)
- [ ] Daily digest sent at 8 AM IST
- [ ] User can override priority per category
- [ ] Notification grouping works for same-category notifications

---

## 8. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration, modify notifications + preferences |
| 2 | 2 days | SmartNotificationService (priority, batching, quiet hours) |
| 3 | 1 day | AI priority classification prompt |
| 4 | 2 days | Batch processor + daily digest job |
| 5 | 1 day | API endpoints for preferences |
| 6 | 2 days | Client UI (priority preferences, quiet hours settings) |
| 7 | 2 days | Tests |

---

## 9. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Modify + Add | Columns on notifications + preferences, new `NotificationBatchesTable` |
| `server/.../feature/notifications/SmartNotificationService.kt` | New | Core service |
| `server/.../feature/notifications/Notify.kt` | Modify | Route through SmartNotificationService |
| `server/.../feature/notifications/BatchProcessorJob.kt` | New | Batching job |
| `server/.../feature/notifications/DailyDigestJob.kt` | New | Digest job |
| `docs/db/migration_070_smart_notifications.sql` | New | DDL |
| `composeApp/.../ui/v2/screens/parent/NotificationPreferencesScreen.kt` | Modify | Add quiet hours + priority overrides |
