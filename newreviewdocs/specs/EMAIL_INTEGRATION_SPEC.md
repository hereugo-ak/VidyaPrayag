# Email Integration — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** None

---

## 1. Feature Overview

Extend the existing SMTP email provider from OTP-only to general notification delivery. Enables sending announcements, fee receipts, report cards, and newsletters via email alongside WhatsApp and FCM.

### Goals

- Send transactional emails (receipts, report cards, PTM invitations)
- Send announcement emails to class/school
- HTML email templates with school branding
- Email delivery tracking (sent, bounced, opened)
- Integration with existing `NotificationPreferencesTable` (email_enabled per category)

---

## 2. Current System Assessment

- `feature_audit.csv` L142: "SmtpEmailProvider exists but only for OTP" — 30% complete
- `WhatsappLogsTable` pattern can be mirrored for email logs
- `NotificationPreferencesTable` has `enabled` but no `email_enabled` column (add in this spec)

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Send HTML emails via SMTP (existing provider extended) |
| FR-2 | Email templates: receipt, report_card, announcement, ptm_invitation, fee_reminder |
| FR-3 | Bulk email to class/school (BCC or individual) |
| FR-4 | Delivery tracking: sent, bounced, opened (via tracking pixel) |
| FR-5 | Per-category email opt-out in notification preferences |
| FR-6 | Attachments: PDF receipts, report cards |

---

## 4. Database Design

```sql
CREATE TABLE email_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    recipient_email TEXT NOT NULL,
    recipient_name  TEXT,
    recipient_user_id UUID,
    subject         TEXT NOT NULL,
    body_html       TEXT NOT NULL,
    template_name   VARCHAR(64),
    category        VARCHAR(32),
    status          VARCHAR(16) NOT NULL DEFAULT 'queued', -- queued | sent | bounced | opened
    smtp_message_id TEXT,
    error_message   TEXT,
    notification_id UUID,
    sent_at         TIMESTAMP,
    opened_at       TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_email_logs_school ON email_logs(school_id, created_at DESC);

ALTER TABLE notification_preferences ADD COLUMN email_enabled BOOLEAN NOT NULL DEFAULT true;
```

---

## 5. Backend Architecture

```kotlin
class EmailService(private val smtpProvider: SmtpEmailProvider) {
    suspend fun send(request: EmailRequest): EmailResult
    suspend fun sendBulk(recipients: List<EmailRecipient>, template: String, variables: Map<String, String>): BatchResult
    suspend fun handleBounce(emailLogId: UUID)
}
```

Integration with `Notify.kt`: if `email_enabled` for category and user has email, send email alongside FCM/WhatsApp.

---

## 6. API Contracts

```
POST /api/v1/school/email/send  { to, subject, template_name, variables }
POST /api/v1/school/email/broadcast  { category, template_name, audience_type, audience_filter }
GET /api/v1/school/email/stats
```

---

## 7. Acceptance Criteria

- [ ] Transactional emails sent via SMTP
- [ ] HTML templates with school branding
- [ ] Bulk email to class/school
- [ ] Delivery tracking (sent, bounced, opened)
- [ ] Per-category email opt-out
- [ ] PDF attachments (receipts, report cards)

---

## 8. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration, Exposed table |
| 2 | 2 days | EmailService + HTML template engine |
| 3 | 1 day | Notify.kt integration |
| 4 | 1 day | Tracking pixel + bounce handling |
| 5 | 2 days | Client UI (email stats dashboard) |
| 6 | 1 day | Tests |

---

## 9. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add + Modify | `EmailLogsTable` + column on notification_preferences |
| `server/.../feature/email/EmailService.kt` | New | Core email service |
| `server/.../feature/email/EmailTemplates.kt` | New | HTML templates |
| `server/.../feature/notifications/Notify.kt` | Modify | Add email channel |
| `docs/db/migration_054_email_integration.sql` | New | DDL |
