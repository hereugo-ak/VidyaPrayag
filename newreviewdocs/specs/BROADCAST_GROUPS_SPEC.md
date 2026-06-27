# Broadcast Groups — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** None

---

## 1. Feature Overview

Admin-managed broadcast groups for targeted communication: create custom groups (e.g., "Grade 5 Parents", "Science Teachers", "Sports Team") and send announcements/messages to specific groups instead of whole school or single class.

### Goals

- Admin creates custom broadcast groups with members (parents, teachers, staff)
- Groups can be static (manual members) or dynamic (rule-based: all parents of Grade 5)
- Send announcement to a group
- Group management UI (add/remove members)
- Reusable across announcements and messages

---

## 2. Current System Assessment

- `feature_audit.csv` L123: Broadcast Groups partially implemented (30%)
- `AnnouncementsTable` has `audienceType` (ALL_SCHOOL, GRADES, CLASSES, SECTIONS, TEACHERS, PARENTS) — but no custom groups
- `CalendarEventsTable` has similar audience targeting pattern

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Admin creates broadcast group with name, description, type (static | dynamic) |
| FR-2 | Static groups: manually add/remove members |
| FR-3 | Dynamic groups: rule-based (e.g., all parents of students in Grade 5, all science teachers) |
| FR-4 | Send announcement to group (audience_type = "GROUP") |
| FR-5 | Group membership list viewable |
| FR-6 | Dynamic groups auto-resolve at send time (not cached) |

---

## 4. Database Design

```sql
CREATE TABLE broadcast_groups (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    name            TEXT NOT NULL,
    description     TEXT,
    group_type      VARCHAR(16) NOT NULL,          -- static | dynamic
    dynamic_rule    TEXT,                          -- JSON: {"role": "parent", "class_name": "Grade 5"}
    created_by      UUID,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE broadcast_group_members (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id        UUID NOT NULL REFERENCES broadcast_groups(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL,                 -- FK app_users.id
    user_name       TEXT NOT NULL,
    user_role       VARCHAR(32) NOT NULL,
    added_at        TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(group_id, user_id)
);
```

Modify `AnnouncementsTable`:
```sql
ALTER TABLE announcements ADD COLUMN broadcast_group_id UUID;
```

---

## 5. API Contracts

```
GET/POST /api/v1/school/broadcast-groups
GET/POST /api/v1/school/broadcast-groups/{id}/members
DELETE /api/v1/school/broadcast-groups/{id}/members/{userId}
POST /api/v1/school/announcements  { audience_type: "GROUP", broadcast_group_id: "uuid" }
```

---

## 6. Acceptance Criteria

- [ ] Admin can create static and dynamic broadcast groups
- [ ] Static groups: add/remove members manually
- [ ] Dynamic groups: auto-resolve members at send time
- [ ] Announcement can be sent to a group
- [ ] Group membership viewable

---

## 7. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration, Exposed tables |
| 2 | 2 days | BroadcastGroupService (CRUD, dynamic resolution) |
| 3 | 1 day | Announcement integration |
| 4 | 2 days | Client UI (group management, member picker) |
| 5 | 1 day | Tests |

---

## 8. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add + Modify | 2 new tables + column on announcements |
| `server/.../feature/broadcast/BroadcastGroupService.kt` | New | Group management |
| `docs/db/migration_056_broadcast_groups.sql` | New | DDL |
| `composeApp/.../ui/v2/screens/admin/BroadcastGroupScreen.kt` | New | Group management UI |
