# AI Fee Reminder Optimization — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** `AI_INFRASTRUCTURE_SPEC.md`, `FEE_PAYMENT_SPEC.md`

---

## 1. Feature Overview

AI-optimized fee reminders that personalize message content, timing, and channel based on parent behavior patterns. Uses LLM to generate contextual, non-generic reminders that improve payment conversion rates.

### Goals

- Personalize reminder message based on parent's payment history and relationship
- Optimize send time based on when parent is most likely to respond
- Vary tone (gentle, urgent, empathetic) based on overdue duration
- Include payment link and specific amount
- Track reminder effectiveness (opened → paid conversion)

---

## 2. Current System Assessment

- `FeeRecordsTable` has `lastRemindedAt` — basic reminder tracking
- No AI personalization — all reminders are generic
- `WhatsAppCloudProvider` exists for sending messages
- No reminder analytics or A/B testing

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Generate personalized reminder message per parent using LLM |
| FR-2 | Adjust tone based on overdue duration (1-7 days: gentle, 8-30: firm, 30+: urgent) |
| FR-3 | Include student name, specific amount, due date, payment link |
| FR-4 | Track reminder effectiveness: sent → opened → paid conversion |
| FR-5 | Limit reminders: max 3 per fee record, spaced ≥3 days apart |
| FR-6 | Send via WhatsApp (primary) + FCM push (fallback) |

---

## 4. Database Design

### 4.1 New Table: `ai_fee_reminders`

```sql
CREATE TABLE ai_fee_reminders (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    fee_record_id   UUID NOT NULL,
    parent_id       UUID NOT NULL,
    student_id      UUID,
    reminder_number INTEGER NOT NULL,              -- 1, 2, 3
    tone            VARCHAR(16) NOT NULL,          -- gentle | firm | urgent
    channel         VARCHAR(16) NOT NULL,          -- whatsapp | fcm | both
    message_body    TEXT NOT NULL,
    payment_link    TEXT,
    sent_at         TIMESTAMP NOT NULL DEFAULT now(),
    opened_at       TIMESTAMP,
    paid_at         TIMESTAMP,                     -- if payment made after this reminder
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_ai_fee_reminders_school ON ai_fee_reminders(school_id, sent_at DESC);
CREATE INDEX idx_ai_fee_reminders_fee ON ai_fee_reminders(fee_record_id);
```

---

## 5. Backend Architecture

### 5.1 FeeReminderService

```kotlin
class FeeReminderService(
    private val aiService: AiService,
    private val whatsAppService: WhatsAppService,
    private val notify: Notify
) {
    suspend fun sendReminder(feeRecord: FeeRecord, parentId: UUID): UUID {
        // 1. Check reminder count (max 3, ≥3 days apart)
        // 2. Determine tone based on overdue days
        // 3. Build context: parent name, student name, amount, due date, history
        // 4. Call AiService with "fee_reminder" template
        // 5. Generate Razorpay payment link
        // 6. Send via WhatsApp + FCM
        // 7. Store in ai_fee_reminders
        // 8. Update fee_records.lastRemindedAt
    }
}
```

### 5.2 Prompt Template

**System:**
```
You are writing a fee reminder message to a parent. Be respectful and specific.
Include: student name, exact amount, due date, payment link.
Tone: {{tone}} (gentle=friendly nudge, firm=clear expectation, urgent=final notice)
Keep under 160 characters for SMS, or 300 for WhatsApp.
```

### 5.3 Scheduled Job

Daily 9 AM IST:
1. Query `fee_records` where `status IN ('DUE', 'OVERDUE')` AND `lastRemindedAt IS NULL OR lastRemindedAt < now() - 3 days`
2. For each, generate and send AI reminder
3. Respect max 3 reminders per fee record

---

## 6. API Contracts

```
POST /api/v1/school/fees/ai-remind
{
  "fee_record_id": "uuid"
}
```

```
GET /api/v1/school/fees/ai-reminders/stats
```

Returns conversion rate: reminders sent → payments received within 7 days.

---

## 7. Acceptance Criteria

- [ ] Reminders personalized per parent with student name and amount
- [ ] Tone varies based on overdue duration
- [ ] Max 3 reminders per fee record, spaced ≥3 days
- [ ] Payment link included in reminder
- [ ] Reminder effectiveness tracked (sent → paid conversion)
- [ ] Daily scheduled job runs automatically

---

## 8. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration, Exposed table |
| 2 | 2 days | FeeReminderService + prompt template |
| 3 | 1 day | Scheduled job |
| 4 | 1 day | API endpoint + stats |
| 5 | 1 day | Tests |

---

## 9. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | `AiFeeRemindersTable` |
| `server/.../feature/fees/AiFeeReminderService.kt` | New | Core service |
| `server/.../feature/fees/FeeReminderJob.kt` | New | Scheduled job |
| `docs/db/migration_040_ai_fee_reminder.sql` | New | DDL |
