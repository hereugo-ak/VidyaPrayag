# Family Circle — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** None
> **Source:** `DIFFERENTIATING_FEATURES.md` §2.2

---

## 1. Feature Overview

Multi-parent support for a single child: both parents, grandparents, and guardians can be linked to a student with configurable access levels. Enables shared visibility into child's school life with role-appropriate permissions.

### Goals

- Link multiple family members to a single student
- Configurable access per family member (view-only, full access, limited)
- Primary parent designation (for fee-related communications)
- Family member invitation via phone number
- Each family member gets their own login and sees child's data per their access level

---

## 2. Current System Assessment

- `ParentChildLinksTable` (`Tables.kt:1320-1340`) — links parent to child with `isApproved`, `approvedAt`
- `ChildrenTable` — child records with `parentUserId` (single parent)
- `AppUsersTable` — user accounts with role `parent`
- Current: one parent per child (primary). No multi-parent or family member support
- `DIFFERENTIATING_FEATURES.md` §2.2: Family Circle, effort S, data readiness: "ParentChildLinksTable exists but single-parent only"

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Primary parent can invite family members (phone number) |
| FR-2 | Invited family member creates account (or links existing) → linked to child |
| FR-3 | Access levels: full (all data), limited (academics + attendance only), view_only (announcements + events) |
| FR-4 | Primary parent designation: only primary can receive fee-related notifications and approve changes |
| FR-5 | Family member can be removed by primary parent |
| FR-6 | Each family member sees child's data per their access level |
| FR-7 | Family circle view: see all linked family members and their access levels |

---

## 4. Database Design

### 4.1 Modify Existing: `parent_child_links`

```sql
ALTER TABLE parent_child_links ADD COLUMN access_level VARCHAR(16) NOT NULL DEFAULT 'full';
-- full | limited | view_only
ALTER TABLE parent_child_links ADD COLUMN is_primary BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE parent_child_links ADD COLUMN relationship VARCHAR(32) NOT NULL DEFAULT 'parent';
-- parent | father | mother | grandfather | grandmother | guardian | sibling
ALTER TABLE parent_child_links ADD COLUMN invited_by UUID;
```

### 4.2 New Table: `family_invitations`

```sql
CREATE TABLE family_invitations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    child_id        UUID NOT NULL,
    invited_by      UUID NOT NULL,                 -- primary parent
    invitee_phone   VARCHAR(32) NOT NULL,
    invitee_name    TEXT,
    relationship    VARCHAR(32) NOT NULL,
    access_level    VARCHAR(16) NOT NULL DEFAULT 'limited',
    status          VARCHAR(16) NOT NULL DEFAULT 'pending', -- pending | accepted | rejected | expired
    otp             VARCHAR(8),
    expires_at      TIMESTAMP NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
```

---

## 5. Backend Architecture

### 5.1 FamilyCircleService

```kotlin
class FamilyCircleService {
    suspend fun inviteFamilyMember(parentId: UUID, childId: UUID, request: InviteRequest): UUID {
        // 1. Verify parentId is primary for child
        // 2. Create invitation with OTP
        // 3. Send OTP via WhatsApp/SMS to invitee_phone
    }

    suspend fun acceptInvitation(phone: String, otp: String, userId: UUID): AcceptResult {
        // 1. Verify OTP
        // 2. Create or verify app_users account
        // 3. Create parent_child_links row with specified access_level
        // 4. Mark invitation as accepted
    }

    suspend fun removeFamilyMember(parentId: UUID, childId: UUID, memberUserId: UUID)
    suspend fun updateAccessLevel(parentId: UUID, childId: UUID, memberUserId: UUID, level: String)
    suspend fun getFamilyCircle(parentId: UUID, childId: UUID): List<FamilyMemberDto>
    suspend fun setPrimary(parentId: UUID, childId: UUID, newPrimaryId: UUID)
}
```

### 5.2 Access Level Enforcement

Middleware checks `parent_child_links.access_level` before returning child data:
- `full`: all endpoints (academics, attendance, fees, messages, health, transport)
- `limited`: academics + attendance + homework only
- `view_only`: announcements + events only

---

## 6. API Contracts

```
# Primary Parent
GET /api/v1/parent/family-circle/{childId}
POST /api/v1/parent/family-circle/{childId}/invite  { phone, name, relationship, access_level }
DELETE /api/v1/parent/family-circle/{childId}/members/{userId}
PATCH /api/v1/parent/family-circle/{childId}/members/{userId}  { access_level }
POST /api/v1/parent/family-circle/{childId}/set-primary  { userId }

# Invitee
POST /api/v1/auth/family-accept  { phone, otp }  -- accepts and links
```

---

## 7. Acceptance Criteria

- [ ] Primary parent can invite family members via phone
- [ ] Invitee receives OTP, accepts, and is linked to child
- [ ] Access levels enforced (limited/view_only restricted)
- [ ] Primary parent designation works (fee notifications only to primary)
- [ ] Family member can be removed
- [ ] Family circle view shows all members with access levels
- [ ] Relationship type tracked (father, grandmother, guardian, etc.)

---

## 8. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration, modify parent_child_links + new invitations table |
| 2 | 2 days | FamilyCircleService (invite, accept, remove, access levels) |
| 3 | 2 days | Access level enforcement middleware |
| 4 | 1 day | API endpoints |
| 5 | 2 days | Client UI (family circle view, invite flow, access management) |
| 6 | 1 day | Tests |

---

## 9. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Modify + Add | Columns on parent_child_links + `FamilyInvitationsTable` |
| `server/.../feature/family/FamilyCircleService.kt` | New | Core service |
| `server/.../feature/family/AccessLevelMiddleware.kt` | New | Access enforcement |
| `docs/db/migration_068_family_circle.sql` | New | DDL |
| `composeApp/.../ui/v2/screens/parent/FamilyCircleScreen.kt` | New | Family circle management |
