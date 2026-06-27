# WhatsApp Business API Integration — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Audience:** Senior engineer / AI agent implementing the system
> **Last updated:** 2026-06-27
> **Prerequisites:** None
> **Unblocks:** `SMART_NOTIFICATIONS_SPEC.md`, `FEE_PAYMENT_SPEC.md` (payment links), `SCHEDULED_ANNOUNCEMENTS_SPEC.md`

---

## Table of Contents

1. [Feature Overview](#1-feature-overview)
2. [Current System Assessment](#2-current-system-assessment)
3. [Gap Analysis](#3-gap-analysis)
4. [Functional Requirements](#4-functional-requirements)
5. [User Roles & Permissions](#5-user-roles--permissions)
6. [Database Design](#6-database-design)
7. [Backend Architecture](#7-backend-architecture)
8. [API Contracts](#8-api-contracts)
9. [Notifications](#9-notifications)
10. [Security](#10-security)
11. [Validation Rules](#11-validation-rules)
12. [Error Handling](#12-error-handling)
13. [Edge Cases](#13-edge-cases)
14. [Performance Considerations](#14-performance-considerations)
15. [Background Processing](#15-background-processing)
16. [Monitoring](#16-monitoring)
17. [Feature Flags](#17-feature-flags)
18. [Configuration](#18-configuration)
19. [Migration Strategy](#19-migration-strategy)
20. [Testing Strategy](#20-testing-strategy)
21. [Acceptance Criteria](#21-acceptance-criteria)
22. [Implementation Roadmap](#22-implementation-roadmap)
23. [File-Level Impact Analysis](#23-file-level-impact-analysis)
24. [Risks & Mitigations](#24-risks--mitigations)
25. [Future Extensibility](#25-future-extensibility)

---

## 1. Feature Overview

Extend the existing WhatsApp Cloud API integration from OTP-only delivery to a full multi-channel notification system. This enables sending announcements, fee reminders, attendance alerts, exam results, PTM invitations, and payment links via WhatsApp Business API, with template management, two-way messaging, and delivery tracking.

### Goals

- Replace/supplement FCM push with WhatsApp for high-priority notifications in India
- Support WhatsApp template messages (pre-approved by Meta) for transactional alerts
- Support two-way messaging (parent replies routed to school admin inbox)
- Template management UI for admins to create and submit templates for approval
- Delivery status tracking (sent, delivered, read) via webhooks
- Per-category opt-out (parent can stop specific WhatsApp notification types)
- Rate limit compliance with WhatsApp Business API (tier-based messaging limits)

---

## 2. Current System Assessment

### 2.1 What Exists

- **`WhatsAppCloudProvider`** — exists but only for OTP delivery (`feature_audit.csv` L143: "WhatsAppCloudProvider exists but only for OTP delivery")
- **`WhatsappLogsTable`** (`Tables.kt:345-354`) — logs WhatsApp sends with `status` (QUEUED/sent/failed), `providerMessageId`, `errorMessage`
- **`AnnouncementsTable`** has `syncedToWa` flag and `POST /announcements/{id}/sync-whatsapp` endpoint
- **`NotificationsTable`** — per-recipient notification rows with category, deepLink, actorId
- **`NotificationPreferencesTable`** (`Tables.kt:1675-1687`) — per-user per-category enable/disable + sound
- OTP gateway infrastructure: `OtpGatewayDevicesTable`, `SmsRequestsTable` (SMS via OTPSender Android app)

### 2.2 What's Missing

- No general-purpose WhatsApp message sending (only OTP + announcement sync)
- No WhatsApp template management
- No two-way messaging (inbound webhook handler)
- No delivery status tracking (sent/delivered/read)
- No per-category WhatsApp opt-out
- No media message support (images, documents)
- No rate limiting for WhatsApp API

---

## 3. Gap Analysis

| # | Gap | Impact |
|---|---|---|
| G1 | WhatsApp limited to OTP only | Most popular channel in India unused for notifications |
| G2 | No template management | Cannot send structured transactional messages |
| G3 | No inbound message handling | Parent replies lost |
| G4 | No delivery tracking | Cannot measure WhatsApp effectiveness |
| G5 | No media messages | Cannot send report cards, receipts as PDF |
| G6 | No rate limiting | Risk of WhatsApp API throttling/ban |

---

## 4. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Send template messages for: attendance alerts, fee reminders, exam results, PTM invitations, announcement broadcasts, payment links |
| FR-2 | Admin can create, submit, and manage WhatsApp message templates (Meta approval workflow) |
| FR-3 | Send media messages (image + caption, document) for receipts, report cards, circulars |
| FR-4 | Two-way messaging: inbound messages from parents routed to admin inbox or auto-reply |
| FR-5 | Delivery status tracking via webhook (sent, delivered, read, failed) |
| FR-6 | Parent can opt out of specific WhatsApp categories via reply ("STOP FEES", "STOP ATTENDANCE") |
| FR-7 | Rate limiting per school based on WhatsApp tier (1000/10000/unlimited per 24h) |
| FR-8 | Bulk broadcast to class/section/all-school via WhatsApp (respecting template + opt-out) |
| FR-9 | WhatsApp notification preference integrated with existing `NotificationPreferencesTable` |
| FR-10 | Fallback to FCM push if WhatsApp send fails |

---

## 5. User Roles & Permissions

| Action | Parent | School Admin | Super Admin |
|---|---|---|---|
| Receive WhatsApp notifications | ✅ | ✅ | ✅ |
| Opt out of categories | ✅ | ✅ | ✅ |
| Send WhatsApp broadcast | ❌ | ✅ | ✅ |
| Create/edit templates | ❌ | ❌ | ✅ |
| View delivery stats | ❌ | ✅ | ✅ |
| Reply to inbound messages | ❌ | ✅ | ✅ |

---

## 6. Database Design

### 6.1 New Table: `whatsapp_templates`

```sql
CREATE TABLE whatsapp_templates (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID,                          -- null = global template
    template_name   TEXT NOT NULL,                 -- unique per WhatsApp Business account
    language        VARCHAR(8) NOT NULL DEFAULT 'en', -- en | hi | bn | ta | te | mr | gu | kn | ml
    category        VARCHAR(32) NOT NULL,          -- TRANSACTIONAL | MARKETING | UTILITY
    body_text       TEXT NOT NULL,                 -- template body with {{1}}, {{2}} placeholders
    header_type     VARCHAR(16),                   -- text | image | document | null
    header_text     TEXT,
    footer_text     TEXT,
    buttons         TEXT NOT NULL DEFAULT '[]',    -- JSON array: [{"type":"quick_reply","text":"Yes"}, {"type":"url","text":"Pay","url":"{{1}}"}]
    meta_status     VARCHAR(16) NOT NULL DEFAULT 'pending', -- pending | approved | rejected
    meta_template_id TEXT,                         -- WhatsApp template ID from Meta
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(template_name, language)
);
CREATE INDEX idx_wa_templates_school ON whatsapp_templates(school_id, meta_status);
```

### 6.2 New Table: `whatsapp_messages`

```sql
CREATE TABLE whatsapp_messages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    recipient_phone VARCHAR(32) NOT NULL,
    recipient_name  TEXT,
    recipient_user_id UUID,                        -- FK app_users.id (if known)
    template_id     UUID,                          -- FK whatsapp_templates.id (if template message)
    category        VARCHAR(32) NOT NULL,          -- attendance | fees | exam | ptm | announcement | payment | otp | general
    message_type    VARCHAR(16) NOT NULL,          -- template | text | media
    body            TEXT,                          -- rendered message text
    media_url       TEXT,                          -- Supabase Storage URL for media messages
    media_type      VARCHAR(16),                   -- image | document | audio
    wamid           TEXT,                          -- WhatsApp message ID (from API response)
    status          VARCHAR(16) NOT NULL DEFAULT 'queued', -- queued | sent | delivered | read | failed
    error_code      INTEGER,
    error_message   TEXT,
    notification_id UUID,                          -- FK notifications.id (if triggered by notification)
    sent_at         TIMESTAMP,
    delivered_at    TIMESTAMP,
    read_at         TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_wa_messages_school_status ON whatsapp_messages(school_id, status, created_at DESC);
CREATE INDEX idx_wa_messages_phone ON whatsapp_messages(recipient_phone, created_at DESC);
```

### 6.3 New Table: `whatsapp_inbound_messages`

```sql
CREATE TABLE whatsapp_inbound_messages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID,                          -- resolved from sender's phone → app_users
    sender_phone    VARCHAR(32) NOT NULL,
    sender_user_id  UUID,                          -- FK app_users.id (resolved)
    sender_name     TEXT,
    message_body    TEXT,
    message_type    VARCHAR(16),                   -- text | image | document | audio | button_reply
    media_url       TEXT,
    wamid           TEXT,
    replied_to_message_id UUID,                    -- FK whatsapp_messages.id (if reply to outbound)
    is_processed    BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_wa_inbound_school ON whatsapp_inbound_messages(school_id, created_at DESC);
CREATE INDEX idx_wa_inbound_unprocessed ON whatsapp_inbound_messages(is_processed, created_at);
```

### 6.4 Modify Existing: `whatsapp_logs`

Add column for linking to `whatsapp_messages`:
```sql
ALTER TABLE whatsapp_logs ADD COLUMN whatsapp_message_id UUID;
```

### 6.5 Modify Existing: `notification_preferences`

Add `whatsapp_enabled` column:
```sql
ALTER TABLE notification_preferences ADD COLUMN whatsapp_enabled BOOLEAN NOT NULL DEFAULT true;
```

---

## 7. Backend Architecture

### 7.1 Component Overview

```
┌──────────────────────────────────────────────────────┐
│              Notification Triggers                    │
│  (Attendance, Fees, Exams, Announcements, PTM)        │
└──────────────────┬───────────────────────────────────┘
                   │ Notify.toUser() / Notify.toClass()
                   ▼
┌──────────────────────────────────────────────────────┐
│              NotificationService (existing)            │
│  - Checks NotificationPreferencesTable                │
│  - If whatsapp_enabled → enqueue WhatsApp send        │
│  - Always writes NotificationsTable row               │
│  - FCM push as fallback                               │
└──────────────────┬───────────────────────────────────┘
                   │
                   ▼
┌──────────────────────────────────────────────────────┐
│              WhatsAppService                           │
│  - Template resolution + variable injection           │
│  - Rate limiter check                                  │
│  - Opt-out check                                       │
│  - Media upload (if needed)                            │
│  - Call WhatsAppCloudApi                               │
│  - Log to whatsapp_messages                            │
│  - Update delivery status from webhook                 │
└──────────────────┬───────────────────────────────────┘
                   │
                   ▼
┌──────────────────────────────────────────────────────┐
│              WhatsAppCloudApi                          │
│  - POST /messages (send template/text/media)           │
│  - POST /templates (create template)                   │
│  - GET /templates (check status)                       │
│  - Webhook handler (inbound + status)                  │
└──────────────────────────────────────────────────────┘
```

### 7.2 WhatsAppService

```kotlin
class WhatsAppService(
    private val api: WhatsAppCloudApi,
    private val templates: WhatsAppTemplateRepository,
    private val messages: WhatsAppMessageRepository,
    private val rateLimiter: WhatsAppRateLimiter,
    private val preferences: NotificationPreferenceRepository
) {
    suspend fun sendTemplate(
        schoolId: UUID,
        recipientPhone: String,
        recipientUserId: UUID?,
        category: String,
        templateName: String,
        language: String,
        variables: List<String>,
        notificationId: UUID? = null
    ): WhatsAppSendResult

    suspend fun sendText(
        schoolId: UUID,
        recipientPhone: String,
        category: String,
        body: String,
        notificationId: UUID? = null
    ): WhatsAppSendResult

    suspend fun sendMedia(
        schoolId: UUID,
        recipientPhone: String,
        category: String,
        mediaType: String,    // image | document
        mediaUrl: String,
        caption: String,
        notificationId: UUID? = null
    ): WhatsAppSendResult

    suspend fun broadcast(
        schoolId: UUID,
        recipients: List<String>,
        category: String,
        templateName: String,
        variablesPerRecipient: Map<String, List<String>>
    ): BatchResult

    suspend fun handleInbound(message: WhatsAppInboundEvent): UUID
    suspend fun handleStatusUpdate(status: WhatsAppStatusEvent)
    suspend fun createTemplate(request: CreateTemplateRequest): WhatsAppTemplateDto
    suspend fun syncTemplateStatus(): Int  // poll Meta for pending templates
}
```

### 7.3 WhatsAppCloudApi

```kotlin
class WhatsAppCloudApi(
    httpClient: HttpClient,
    phoneNumberId: String,
    accessToken: String,
    apiVersion: String = "v21.0"
) {
    // Send template message
    suspend fun sendTemplateMessage(
        to: String,
        templateName: String,
        language: String,
        components: List<TemplateComponent>
    ): WhatsAppApiResponse

    // Send text message
    suspend fun sendTextMessage(to: String, body: String): WhatsAppApiResponse

    // Send media message
    suspend fun sendMediaMessage(to: String, mediaType: String, mediaUrl: String, caption: String?): WhatsAppApiResponse

    // Template management
    suspend fun createTemplate(template: TemplateDefinition): String  // returns template ID
    suspend fun getTemplateStatus(templateId: String): MetaTemplateStatus

    // Base URL: https://graph.facebook.com/v21.0/{phoneNumberId}/messages
}
```

### 7.4 Template Resolution

```kotlin
fun resolveTemplate(
    template: WhatsAppTemplate,
    variables: List<String>
): String {
    var body = template.bodyText
    for ((index, value) in variables.withIndex()) {
        body = body.replace("{{${index + 1}}}", value)
    }
    return body
}
```

### 7.5 Rate Limiter

WhatsApp Business API has tier-based limits:
- Tier 1: 1,000 messages per 24h
- Tier 2: 10,000 messages per 24h
- Tier 3: 100,000 messages per 24h
- Tier 4: Unlimited

```kotlin
class WhatsAppRateLimiter {
    suspend fun checkLimit(schoolId: UUID, messageCount: Int): Boolean {
        val today = LocalDate.now()
        val sentToday = messageRepository.countBySchoolAndDate(schoolId, today)
        val limit = getSchoolTierLimit(schoolId)
        return sentToday + messageCount <= limit
    }
}
```

### 7.6 Opt-Out Handling

Parents can reply with keywords:
- "STOP" → disable all WhatsApp notifications
- "STOP FEES" → disable fee category
- "STOP ATTENDANCE" → disable attendance category
- "START" → re-enable all

Inbound message handler checks for opt-out keywords and updates `notification_preferences.whatsapp_enabled`.

---

## 8. API Contracts

### 8.1 Send WhatsApp Notification (Internal)

Called by `NotificationService`, not directly exposed.

### 8.2 Admin: Send Broadcast

```
POST /api/v1/school/whatsapp/broadcast
{
  "category": "announcement",
  "template_name": "announcement_alert",
  "language": "hi",
  "audience_type": "CLASS",
  "audience_filter": {"class_name": "Grade 5", "section": "A"},
  "variables_per_recipient": {
    "phone1": ["Annual Day", "15 July"],
    "phone2": ["Annual Day", "15 July"]
  }
}
```

### 8.3 Admin: Template Management

```
GET /api/v1/super/whatsapp/templates
POST /api/v1/super/whatsapp/templates
PATCH /api/v1/super/whatsapp/templates/{id}
DELETE /api/v1/super/whatsapp/templates/{id}
POST /api/v1/super/whatsapp/templates/sync-status
```

### 8.4 Admin: Delivery Stats

```
GET /api/v1/school/whatsapp/stats?date_from={YYYY-MM-DD}&date_to={YYYY-MM-DD}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "total_sent": 5000,
    "delivered": 4800,
    "read": 3200,
    "failed": 200,
    "delivery_rate": 0.96,
    "read_rate": 0.64,
    "by_category": [
      {"category": "attendance", "sent": 2000, "delivered": 1950},
      {"category": "fees", "sent": 1000, "delivered": 980}
    ]
  }
}
```

### 8.5 WhatsApp Webhook (Inbound + Status)

```
POST /api/v1/webhooks/whatsapp
```

**Verification:**
```
GET /api/v1/webhooks/whatsapp?hub.mode=subscribe&hub.challenge={challenge}&hub.verify_token={token}
```

**Inbound message event:**
```json
{
  "entry": [{
    "changes": [{
      "value": {
        "messages": [{
          "from": "919876543210",
          "id": "wamid.xxx",
          "text": {"body": "STOP FEES"},
          "timestamp": "1719475200"
        }]
      }
    }]
  }]
}
```

**Status update event:**
```json
{
  "entry": [{
    "changes": [{
      "value": {
        "statuses": [{
          "id": "wamid.xxx",
          "status": "delivered",
          "recipient_id": "919876543210",
          "timestamp": "1719475300"
        }]
      }
    }]
  }]
}
```

### 8.6 Parent: WhatsApp Preferences

```
GET /api/v1/parent/whatsapp/preferences
PATCH /api/v1/parent/whatsapp/preferences
{
  "categories": {
    "attendance": true,
    "fees": false,
    "exam": true,
    "announcement": true,
    "ptm": true
  }
}
```

---

## 9. Notifications

WhatsApp IS the notification channel. Integration with existing `Notify.kt`:

```kotlin
// In Notify.toUser()
if (preferences.whatsappEnabled(category) && user.phone != null) {
    try {
        whatsAppService.sendTemplate(...)
    } catch (e: Exception) {
        // Fallback to FCM
        fcmService.send(...)
    }
} else {
    fcmService.send(...)
}
```

---

## 10. Security

- WhatsApp Business Access Token stored encrypted in `AppConfigTable`
- Webhook verify token stored in env variable
- Webhook signature verification (X-Hub-Signature-256 header, HMAC-SHA256)
- Template content reviewed by Meta before approval (no spam, no prohibited content)
- Recipient phone numbers validated (E.164 format)
- School-scoped: one school cannot send to another school's parents
- Inbound messages stored but PII (message body) purged after 90 days

---

## 11. Validation Rules

| Field | Rule |
|---|---|
| recipient_phone | E.164 format, +91 prefix for India |
| template_name | Alphanumeric + underscores, ≤ 512 chars |
| body (text message) | ≤ 4096 chars |
| variables count | Must match template placeholder count |
| media_url | Must be publicly accessible HTTPS URL |
| media_size | Image ≤ 5MB, Document ≤ 100MB, Audio ≤ 16MB |

---

## 12. Error Handling

| Code | HTTP | Message |
|---|---|---|
| `WA_TEMPLATE_NOT_APPROVED` | 400 | Template not approved by Meta |
| `WA_RATE_LIMITED` | 429 | WhatsApp daily limit reached for this school |
| `WA_RECIPIENT_OPTED_OUT` | 403 | Recipient has opted out of this category |
| `WA_INVALID_PHONE` | 400 | Invalid phone number format |
| `WA_API_ERROR` | 502 | WhatsApp API error: {message} |
| `WA_TEMPLATE_NOT_FOUND` | 404 | Template not found |
| `WA_MEDIA_UPLOAD_FAILED` | 500 | Failed to upload media |

---

## 13. Edge Cases

- **Parent has no phone number:** Skip WhatsApp, send FCM only
- **Template pending approval:** Queue message, send when approved (or fall back to FCM)
- **WhatsApp API throttling (429):** Exponential backoff retry (3 attempts), then FCM fallback
- **Inbound message from unknown number:** Store in `whatsapp_inbound_messages` with `school_id = null`, admin can manually assign
- **Opt-out reply:** Process immediately, confirm with "You will no longer receive {category} messages. Reply START to resume."
- **Media message with expired URL:** WhatsApp caches media; if URL expires before send, re-upload
- **Multi-language:** Template sent in parent's `languagePref` if available, else English

---

## 14. Performance Considerations

- WhatsApp API calls have 2-5s latency — always async
- Bulk broadcast processes recipients in batches of 100 (WhatsApp API limit)
- Rate limiter checks are in-memory (Redis or ConcurrentHashMap with TTL)
- Delivery status updates from webhook are batch-processed
- Template sync (polling Meta for approval status) runs every 6 hours
- Media upload to WhatsApp: upload to Supabase first, then pass URL to WhatsApp API

---

## 15. Background Processing

| Job | Schedule | Description |
|---|---|---|
| Template status sync | Every 6 hours | Poll Meta for pending template approval status |
| Retry failed sends | Every 5 min | Retry `status=failed` messages (max 3 attempts) |
| Inbound message routing | Real-time (webhook) | Route inbound to admin inbox or auto-reply |
| Opt-out keyword processing | Real-time (webhook) | Process STOP/START replies |
| Daily stats aggregation | Daily 2 AM IST | Aggregate delivery stats per school |
| Purge old inbound messages | Daily 3 AM IST | Delete inbound messages older than 90 days |

---

## 16. Monitoring

| Metric | Type |
|---|---|
| `whatsapp.sent_total` | Counter (by category, school) |
| `whatsapp.delivered_total` | Counter |
| `whatsapp.read_total` | Counter |
| `whatsapp.failed_total` | Counter |
| `whatsapp.api_latency_ms` | Histogram |
| `whatsapp.rate_limit_hits` | Counter |
| `whatsapp.inbound_total` | Counter |
| `whatsapp.opt_outs_total` | Counter |
| `whatsapp.template_pending` | Gauge |

**Alerts:**
- Failure rate > 10% in 15 min → Warning
- Rate limit hit > 5x in 1 hour → Warning
- Template stuck in pending > 48h → Warning
- Webhook not received in 24h → Critical

---

## 17. Feature Flags

| Flag | Default | Description |
|---|---|---|
| `WHATSAPP_GENERAL_ENABLED` | false | Enable WhatsApp for general notifications (beyond OTP) |
| `WHATSAPP_TWO_WAY_ENABLED` | false | Enable inbound message handling |
| `WHATSAPP_MEDIA_ENABLED` | false | Enable media message sending |
| `WHATSAPP_BROADCAST_ENABLED` | false | Enable bulk broadcast |

---

## 18. Configuration

### 18.1 Environment Variables

| Variable | Description |
|---|---|
| `WHATSAPP_ACCESS_TOKEN` | Meta WhatsApp Business API access token |
| `WHATSAPP_PHONE_NUMBER_ID` | WhatsApp Business phone number ID |
| `WHATSAPP_BUSINESS_ID` | WhatsApp Business account ID |
| `WHATSAPP_WEBHOOK_VERIFY_TOKEN` | Webhook verification token |
| `WHATSAPP_APP_SECRET` | App secret for webhook signature verification |

### 18.2 AppConfigTable Keys

| Key | Description |
|---|---|
| `whatsapp_tier_{schoolId}` | School's WhatsApp tier (1/2/3/4) |
| `whatsapp_daily_limit_{schoolId}` | Override daily message limit |

---

## 19. Migration Strategy

### 19.1 Migration File

`docs/db/migration_033_whatsapp_integration.sql`

Creates `whatsapp_templates`, `whatsapp_messages`, `whatsapp_inbound_messages` tables. Adds `whatsapp_message_id` to `whatsapp_logs` and `whatsapp_enabled` to `notification_preferences`.

### 19.2 Seed Templates

```sql
INSERT INTO whatsapp_templates (id, school_id, template_name, language, category, body_text, meta_status, created_at, updated_at)
VALUES
  (gen_random_uuid(), NULL, 'attendance_alert_en', 'en', 'TRANSACTIONAL',
   'Dear Parent, your child {{1}} was marked {{2}} on {{3}}. - {{4}}', 'pending', now(), now()),
  (gen_random_uuid(), NULL, 'fee_reminder_en', 'en', 'UTILITY',
   'Dear Parent, fee of ₹{{1}} for {{2}} is due on {{3}}. Pay now: {{4}} - {{5}}', 'pending', now(), now()),
  (gen_random_uuid(), NULL, 'exam_result_en', 'en', 'TRANSACTIONAL',
   'Dear Parent, {{1}} scored {{2}}/{{3}} in {{4}} ({{5}}). - {{6}}', 'pending', now(), now()),
  (gen_random_uuid(), NULL, 'ptm_invitation_en', 'en', 'UTILITY',
   'Dear Parent, PTM is scheduled on {{1}} from {{2}}. Please attend. - {{3}}', 'pending', now(), now()),
  (gen_random_uuid(), NULL, 'announcement_en', 'en', 'UTILITY',
   '{{1}}: {{2}} - {{3}}', 'pending', now(), now())
ON CONFLICT (template_name, language) DO NOTHING;
```

**Note:** Templates must be submitted to Meta for approval before use. The seed inserts them with `meta_status='pending'`; admin must submit via API.

### 19.3 Rollback

Drop new tables, remove added columns. Existing OTP WhatsApp functionality unaffected.

---

## 20. Testing Strategy

### 20.1 Unit Tests

- Template resolution — variables correctly injected
- Rate limiter — over-limit rejected, under-limit allowed
- Opt-out keyword parsing — "STOP FEES" → correct category disabled
- Phone validation — E.164 format enforcement
- Fallback logic — WhatsApp fail → FCM sent

### 20.2 Integration Tests

- Send template message via WhatsApp test API
- Webhook inbound → stored in `whatsapp_inbound_messages`
- Webhook status update → `whatsapp_messages.status` updated
- Broadcast to 100 recipients → all queued, rate limit checked
- Opt-out reply → preference updated, next send skipped
- Template creation → Meta API called with correct payload

### 20.3 WhatsApp Test Number

Use WhatsApp Business test number (+1 555-555-5555) for development. Production requires verified business account.

---

## 21. Acceptance Criteria

- [ ] WhatsApp template messages can be sent for attendance, fees, exams, PTM, announcements
- [ ] Delivery status (sent/delivered/read/failed) tracked via webhook
- [ ] Admin can create and submit templates for Meta approval
- [ ] Admin can broadcast WhatsApp messages to class/section/all-school
- [ ] Parent can opt out of specific categories via reply
- [ ] Two-way messaging routes inbound to admin inbox
- [ ] Media messages (receipts, report cards) can be sent
- [ ] Rate limiting prevents exceeding WhatsApp tier limits
- [ ] FCM fallback works when WhatsApp send fails
- [ ] Delivery stats available in admin dashboard
- [ ] Multi-language templates supported

---

## 22. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 2 days | DB migration, Exposed tables |
| 2 | 3 days | WhatsAppCloudApi (send template/text/media, template CRUD) |
| 3 | 3 days | WhatsAppService (template resolution, rate limiter, opt-out, fallback) |
| 4 | 2 days | Webhook handler (inbound + status) |
| 5 | 2 days | NotificationService integration (extend Notify.kt) |
| 6 | 2 days | Broadcast endpoint + audience expansion (reuse announcement segmentation) |
| 7 | 2 days | Seed templates + Meta submission workflow |
| 8 | 3 days | Client UI (admin WhatsApp stats, template management, parent preferences) |
| 9 | 2 days | Background jobs (retry, template sync, purge) |
| 10 | 3 days | Tests (unit + integration with WhatsApp test API) |

---

## 23. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add + Modify | 3 new tables + columns on whatsapp_logs + notification_preferences |
| `server/.../db/DatabaseFactory.kt` | Modify | Register new tables |
| `server/.../feature/whatsapp/WhatsAppCloudApi.kt` | New | Meta API client |
| `server/.../feature/whatsapp/WhatsAppService.kt` | New | Core service |
| `server/.../feature/whatsapp/WhatsAppRateLimiter.kt` | New | Tier-based rate limiting |
| `server/.../feature/whatsapp/WhatsAppWebhookRouting.kt` | New | Inbound + status webhook |
| `server/.../feature/whatsapp/WhatsAppRouting.kt` | New | Admin endpoints (broadcast, templates, stats) |
| `server/.../feature/notifications/Notify.kt` | Modify | Integrate WhatsApp send + FCM fallback |
| `server/.../Application.kt` | Modify | Register WhatsApp routes + webhook |
| `docs/db/migration_033_whatsapp_integration.sql` | New | DDL + seed templates |
| `shared/.../feature/whatsapp/WhatsAppApi.kt` | New | Client API |
| `composeApp/.../ui/v2/screens/admin/WhatsAppStatsScreen.kt` | New | Delivery stats dashboard |
| `composeApp/.../ui/v2/screens/admin/WhatsAppTemplatesScreen.kt` | New | Template management |
| `composeApp/.../ui/v2/screens/parent/WhatsAppPreferencesScreen.kt` | New | Parent opt-out preferences |

---

## 24. Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Meta rejects templates | Medium | Medium | Follow template guidelines; have admin review before submission |
| WhatsApp API throttling | Medium | Medium | Rate limiter; batch processing; FCM fallback |
| Access token expiry | Medium | High | Monitor token expiry; auto-refresh via Meta API |
| Parents mark as spam | Low | High | Opt-out mechanism; only send approved categories; respect preferences |
| Webhook missed events | Low | Medium | Poll API for status if webhook gap detected |
| Media URL expired before WhatsApp fetch | Low | Low | Re-upload to Supabase with long-lived URL |

---

## 25. Future Extensibility

- **WhatsApp Interactive Messages:** buttons, list messages for PTM slot booking
- **WhatsApp Flows:** in-chat forms for admission enquiry, feedback surveys
- **WhatsApp Business Catalog:** school services/fee structure catalog
- **AI auto-reply:** LLM-powered responses to common parent queries (requires `AI_INFRASTRUCTURE_SPEC.md`)
- **WhatsApp Communities:** broadcast groups for class/section parent groups
