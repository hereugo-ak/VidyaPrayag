# Scheduled Announcements — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** `WHATSAPP_INTEGRATION_SPEC.md` (for WhatsApp delivery)

---

## 1. Feature Overview

Allow school admins to schedule announcements for future delivery at a specific date/time, with multi-channel distribution (in-app, WhatsApp, email, FCM).

### Goals

- Admin creates announcement with scheduled send time
- Announcement stored with `scheduled_at` timestamp
- Background job picks up due announcements and sends them
- Admin can edit/cancel before sent
- Recurring announcements (daily, weekly, monthly)

---

## 2. Current System Assessment

- `AnnouncementsTable` (`Tables.kt:310-328`) — has `createdAt`, `syncedToWa`, `audienceType`, `classIds`, `sectionIds`
- No scheduling field — announcements are sent immediately on creation
- `feature_audit.csv` L160: Scheduled Announcements missing (0%)

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Add `scheduled_at` column to `AnnouncementsTable` |
| FR-2 | Announcement status: draft | scheduled | sent | cancelled |
| FR-3 | Background job checks every minute for due announcements |
| FR-4 | Admin can edit/cancel scheduled announcements before send time |
| FR-5 | Recurring schedule: daily, weekly (specific day), monthly (specific date) |
| FR-6 | Multi-channel: in-app notification + WhatsApp + email + FCM |

---

## 4. Database Design

```sql
ALTER TABLE announcements ADD COLUMN scheduled_at TIMESTAMP;
ALTER TABLE announcements ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'sent';
ALTER TABLE announcements ADD COLUMN recurrence VARCHAR(16); -- null | daily | weekly | monthly
ALTER TABLE announcements ADD COLUMN recurrence_end_date DATE;
```

---

## 5. Backend Architecture

### 5.1 Scheduled Job

Every 1 minute:
1. `SELECT * FROM announcements WHERE status = 'scheduled' AND scheduled_at <= now()`
2. For each: send via Notify (in-app + WhatsApp + email + FCM)
3. Update status to 'sent'
4. If recurrence set, create next occurrence with `scheduled_at` + interval

---

## 6. API Contracts

```
POST /api/v1/school/announcements
{
  "title": "Annual Day Reminder",
  "body": "...",
  "audience_type": "ALL_SCHOOL",
  "scheduled_at": "2026-07-15T09:00:00Z",
  "recurrence": "weekly",
  "recurrence_end_date": "2026-08-15"
}
```

```
PATCH /api/v1/school/announcements/{id}  -- edit before sent
DELETE /api/v1/school/announcements/{id}  -- cancel
GET /api/v1/school/announcements/scheduled
```

---

## 7. Acceptance Criteria

- [ ] Admin can schedule announcement for future date/time
- [ ] Announcement sent automatically at scheduled time
- [ ] Admin can edit/cancel before send
- [ ] Recurring announcements create next occurrence
- [ ] Multi-channel delivery (in-app + WhatsApp + email + FCM)

---

## 8. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration (add columns to announcements) |
| 2 | 2 days | Scheduled job + recurrence logic |
| 3 | 1 day | API modifications (edit/cancel scheduled) |
| 4 | 1 day | Client UI (date/time picker, recurrence options, scheduled list) |
| 5 | 1 day | Tests |

---

## 9. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Modify | Add columns to AnnouncementsTable |
| `server/.../feature/announcements/AnnouncementScheduler.kt` | New | Scheduled job |
| `server/.../feature/announcements/AnnouncementRouting.kt` | Modify | Add scheduling params |
| `docs/db/migration_055_scheduled_announcements.sql` | New | ALTER TABLE |
| `composeApp/.../ui/v2/screens/admin/AnnouncementComposerScreen.kt` | Modify | Add scheduling UI |
