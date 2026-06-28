# Family Circle — Technical Specification

> **Document status:** Partial (40%) — multi-parent links, primary guardian enforcement, relation field, approval workflow exist; role-based visibility, delegation, notification routing, shared RSVP TODO
> **Last updated:** 2026-06-28
> **Prerequisites:** None
> **Source:** `DIFFERENTIATING_FEATURES.md` §2.2
> **Template:** `_SPEC_TEMPLATE.md` v1 (25 mandatory + 6 optional sections)

---

## 1. Feature Overview

### What

Multi-parent support for a single child: both parents, grandparents, and guardians can be linked to a student with configurable access levels. Enables shared visibility into a child's school life with role-appropriate permissions.

### Why — Product Rationale

Indian families are multi-generational and joint. A child's school life is often co-managed by father, mother, grandparents, and sometimes aunts/guardians. Every school ERP on the market — including PowerSchool, the global leader — assumes **one parent = one account**. There's no way for a grandparent to see attendance without also seeing fees, or for a parent to delegate PTM attendance to a grandparent.

This is a **differentiating feature** (Priority P1, Phase 2, effort M, "High" value per `DIFFERENTIATING_FEATURES.md`). It is one of the reasons schools would choose Vidya Prayag over competitors. The cultural insight creates deep lock-in: once a family configures their circle, switching ERPs means re-establishing all those relationships.

### What Stands Out (Competitive Moat)

From `DIFFERENTIATING_FEATURES.md:159`:
> "Indian families are multi-generational. Every ERP assumes one parent = one account. This is a cultural insight that creates deep lock-in."

From `DIFFERENTIATING_FEATURES.md:163`:
> "No ERP supports multi-guardian with role-based visibility. PowerSchool has 'portal access' but not family delegation."

### Four Core Capabilities

1. **Role-based visibility** — A grandparent sees attendance + events but not fees. A parent sees everything. Currently the system is all-or-nothing.
2. **Delegation** — "I can't attend the PTM, assigning grandparent to attend." No ERP supports this.
3. **Notification routing** — "Notify all family members for absences, only primary parent for fees." Currently `NotificationPreferencesTable` is per-user, not per-guardian-role.
4. **Shared calendar/RSVP** — Any family member can RSVP for PTMs and events, not just the primary parent.

### Scope of This Spec

This spec covers **all four capabilities**. Capabilities 1 and 4 are fully specified for implementation. Capabilities 2 and 3 (delegation, notification routing) are specified with database design and API contracts but marked as **Phase 2** in the roadmap — they build on the foundation of capability 1.

### Goals

- Role-based visibility with `full`, `limited`, `view_only` access levels
- Family invitation flow via phone + OTP
- Admin approval still required for family links
- Notification routing per family role
- Delegation of event attendance
- Shared RSVP for family members

### Non-goals

- [ ] Custom access levels beyond the three predefined ones
- [ ] Family member-to-family member messaging
- [ ] Family circle analytics
- [ ] Cross-school family linking (one family circle per school)
- [ ] Family member self-registration (must be invited by primary parent)

### Dependencies

- `ParentChildLinksTable` — existing table, modified with new columns
- `ChildrenTable` — existing table, used for child resolution
- `AppUsersTable` — existing table, used for user accounts
- `StudentAggregationService.enforceSinglePrimaryGuardian()` — existing enforcement
- `ParentLinkRouting.kt` — existing admin approval flow
- `NotifyRecipients.kt` — existing notification recipient resolution
- `NotificationPreferencesTable` — existing per-user notification preferences
- OTP gateway (`setup_otp_gateway.sql`) — existing OTP infrastructure

### Related Modules

- `server/.../feature/family/` — new module for family circle
- `server/.../feature/parent/` — existing parent endpoints (modified)
- `server/.../feature/notifications/` — existing notification system (modified)
- `shared/.../parent/` — shared parent models, repository, API
- `composeApp/.../ui/v2/screens/parent/` — parent UI screens

---

## 2. Current System Assessment

### Existing Code

The codebase **already supports** multiple parents linked to a single student with a primary guardian designation:

- **`ParentChildLinksTable`** (`server/.../db/Tables.kt:1453-1481`) — links a parent to a student with:
  - `parentId` (UUID) — the parent's `app_users.id`
  - `schoolId` (UUID, nullable) — the school scope
  - `studentCode` (varchar 64, nullable) — the canonical student identifier
  - `childId` (UUID, nullable) — `children.id` once the link is approved
  - `status` (varchar 24) — `pending` | `approved` | `rejected` | `needs_review`
  - `relation` (varchar 32, nullable) — `Father` | `Mother` | `Guardian` | etc. (RA-SP)
  - `isPrimaryGuardian` (bool, default false) — marks the single primary point-of-contact (RA-SP)
  - `requestedAt`, `actionedBy`, `actionedAt` — audit trail

- **`StudentAggregationService.enforceSinglePrimaryGuardian()`** — already enforces "at most one primary guardian per (school, student_code)". Called during admin approval of a link request (`ParentLinkRouting.kt:801-824`).

- **`ChildrenTable`** (`server/.../db/Tables.kt:602-620`) — child records with:
  - `parentId` (UUID) — FK to `app_users.id` (the primary parent who registered the child)
  - `schoolId` (UUID, nullable) — set when the child is enrolled
  - `studentCode` (text, nullable) — links to the school's canonical `students` record
  - `childName`, `dateOfBirth`, `gender`, `currentGrade`, `interests`, `profilePic`, etc.

- **`AppUsersTable`** (`server/.../db/Tables.kt:55-81`) — user accounts with:
  - `role` (varchar 32, default `parent`)
  - `phone` (varchar 32, nullable, unique) — phone-OTP-first signup
  - `isPhoneVerified` (bool)

### Existing Database

- `ParentChildLinksTable` — parent→student links with status, relation, isPrimaryGuardian
- `ChildrenTable` — child records owned by primary parent
- `AppUsersTable` — user accounts
- `NotificationPreferencesTable` — per-user notification preferences
- `FeeRecordsTable` — fee records keyed to primary parent
- `AttendanceTable`, `AssessmentMarksTable`, `HomeworkTable`, `AnnouncementsTable` — source data

### Existing APIs

- `ParentLinkRouting.kt` — admin approval/rejection of parent→child links
- `ParentDashboardRouting.kt` — parent dashboard (resolves children via `ChildrenTable.parentId`)
- `ParentAcademicsRouting.kt` — academics (uses `requireOwnedChild()`)
- `ParentFeesRouting.kt` — fees (keyed to `FeeRecordsTable.parentId`)
- `HealthRouting.kt` — health records (ownership gate)
- `ParentLeaveRouting.kt` — leave requests (ownership gate)
- `PulseRouting.kt` — parent pulse (validates via `ChildrenTable.parentId`)
- `ParentMessagesRouting.kt` — messages (school resolution via `ChildrenTable.parentId`)
- `ParentRouting.kt` — announcements + notifications (school resolution via `ChildrenTable.parentId`)

### Existing UI

- `ParentPortalV2.kt` — 5 hardcoded tabs (Home, Academics, Fees, Conversations, Profile)
- `ParentHomeScreenV2.kt` — home screen with alerts
- `ParentFeesScreenV2.kt` — fees screen
- `ParentHealthScreenV2.kt` — health screen
- `ParentAcademicsScreenV2.kt` — academics screen
- Child switcher dropdown in `ParentPortalV2.kt:384-427`
- Unlinked-parent gate in `ParentPortalV2.kt:116`

### Existing Services

- `StudentAggregationService` — enforces single primary guardian
- `NotifyRecipients` — resolves parent recipients via `ChildrenTable.parentId`
- OTP gateway — sends OTP via WhatsApp/SMS

### Existing Documentation

- `DIFFERENTIATING_FEATURES.md` §2.2 — Family Circle
- `ParentLinkRouting.kt` — admin approval flow documentation

### Technical Debt

| # | Gap | Details |
|---|---|---|
| TD-1 | No access level scoping | All linked parents see everything — no `access_level` column |
| TD-2 | No family invitation flow | No `family_invitations` table, no invite-by-phone |
| TD-3 | Child resolution broken for family members | All endpoints resolve via `ChildrenTable.parentId` only |
| TD-4 | No notification routing per role | `NotifyRecipients` only finds primary parent |
| TD-5 | No delegation | No `event_delegations` table |
| TD-6 | Client tabs hardcoded | `ParentPortalV2.kt:208-220` — no access-level filtering |

### Gaps

| # | Gap | Impact | Severity |
|---|---|---|---|
| G1 | Access level scoping | Grandparents see fees, health — privacy concern | **High** |
| G2 | Family invitation flow | No way to invite family members | **High** |
| G3 | Child resolution for family members | Family members get zero children, hit unlinked-parent gate | **High** |
| G4 | Notification routing per role | Family members don't receive any notifications | **High** |
| G5 | Delegation | Can't delegate PTM attendance | **Medium** |
| G6 | Client tab visibility | All tabs shown regardless of access level | **Medium** |

### What the Original Spec Got Wrong

The original spec (pre-revision) claimed "one parent per child (primary). No multi-parent or family member support." This was incorrect — `isPrimaryGuardian` and `relation` already exist on `ParentChildLinksTable`, and `StudentAggregationService.enforceSinglePrimaryGuardian()` already enforces the single-primary constraint. The original spec also proposed adding duplicate columns (`is_primary`, `relationship`) that overlap with existing `is_primary_guardian` and `relation`.

---

## 3. Functional Requirements

### FR-001
| Field | Value |
|---|---|
| **Title** | Family Member Invitation |
| **Description** | Primary parent can invite family members via phone number with OTP verification. Invitee receives OTP, enters it, and a `pending` `parent_child_links` row is created. |
| **Priority** | Critical |
| **User Roles** | Primary Parent, Family Member |
| **Acceptance notes** | Primary parent specifies phone, name (optional), relationship, and access level. OTP sent via existing gateway. 10-minute expiry, max 3 attempts. |

### FR-002
| Field | Value |
|---|---|
| **Title** | Invitation Acceptance & Account Creation |
| **Description** | Invited family member creates an account (or links existing) → linked to child via `parent_child_links` with `status = 'pending'`. |
| **Priority** | Critical |
| **User Roles** | Family Member |
| **Acceptance notes** | If phone matches existing `app_users` row, use that account. Otherwise create new `app_users` with `role='parent'`, `isPhoneVerified=true`. Link created with `status='pending'` — admin must still approve. |

### FR-003
| Field | Value |
|---|---|
| **Title** | Access Levels |
| **Description** | Access levels: `full` (all data), `limited` (academics + attendance + homework), `view_only` (announcements + events). |
| **Priority** | Critical |
| **User Roles** | System |
| **Acceptance notes** | Enforced server-side via `ChildAccessResolver.resolveChildWithAccess()`. Client tab hiding is UX, not security. |

### FR-004
| Field | Value |
|---|---|
| **Title** | Primary Parent Designation |
| **Description** | Only primary receives fee-related notifications and can approve changes. Uses existing `isPrimaryGuardian` column and `enforceSinglePrimaryGuardian()`. |
| **Priority** | High |
| **User Roles** | Primary Parent |
| **Acceptance notes** | Primary guardian can be transferred. `enforceSinglePrimaryGuardian()` ensures at most one primary per student. |

### FR-005
| Field | Value |
|---|---|
| **Title** | Remove Family Member |
| **Description** | Primary parent can remove a family member. Sets `parent_child_links.status = 'rejected'` (soft delete). |
| **Priority** | High |
| **User Roles** | Primary Parent |
| **Acceptance notes** | Cannot remove self if only linked parent. Audit trail preserved. |

### FR-006
| Field | Value |
|---|---|
| **Title** | Access Level Enforcement |
| **Description** | Each family member sees child's data per their access level — enforced server-side via `ChildAccessResolver`. |
| **Priority** | Critical |
| **User Roles** | System |
| **Acceptance notes** | Every endpoint uses `resolveChildWithAccess()` with appropriate `requiredLevels` set. No endpoint does ad-hoc resolution. |

### FR-007
| Field | Value |
|---|---|
| **Title** | Family Circle View |
| **Description** | Family circle view: see all linked family members and their access levels. |
| **Priority** | Medium |
| **User Roles** | Primary Parent |
| **Acceptance notes** | `GET /api/v1/parent/family-circle/{schoolId}/{studentCode}` returns list of `FamilyMemberDto`. |

### FR-008
| Field | Value |
|---|---|
| **Title** | Relationship Tracking |
| **Description** | Relationship type tracked (father, mother, grandmother, guardian, etc.) — reuses existing `relation` column. |
| **Priority** | Medium |
| **User Roles** | System |
| **Acceptance notes** | No new column — existing `ParentChildLinksTable.relation` is reused. Values: father, mother, grandfather, grandmother, guardian, sibling, aunt, uncle. |

### FR-009
| Field | Value |
|---|---|
| **Title** | Shared RSVP (Phase 2) |
| **Description** | Family members can RSVP for PTMs and events (not just primary parent). |
| **Priority** | Medium |
| **User Roles** | Family Member |
| **Acceptance notes** | Phase 2. Calendar/RSVP endpoints updated to accept any family member's RSVP. |

### FR-010
| Field | Value |
|---|---|
| **Title** | Event Delegation (Phase 2) |
| **Description** | Primary parent can delegate event attendance to a family member. |
| **Priority** | Medium |
| **User Roles** | Primary Parent, Family Member |
| **Acceptance notes** | Phase 2. `event_delegations` table. Family member can accept/decline. |

### FR-011
| Field | Value |
|---|---|
| **Title** | Notification Routing (Phase 2) |
| **Description** | Notification routing: absence/marks/homework → all family members; fees → primary only. |
| **Priority** | High |
| **User Roles** | System |
| **Acceptance notes** | `NotifyRecipients.familyMembersOfStudent()` resolves all family members. Fee notifications filtered to `isPrimaryGuardian = true` only. |

### FR-012
| Field | Value |
|---|---|
| **Title** | Per-User Notification Preferences (Phase 2) |
| **Description** | Each family member can configure their own notification preferences per category. |
| **Priority** | Medium |
| **User Roles** | Family Member |
| **Acceptance notes** | `NotificationPreferencesTable` is already per-user per-category. No change needed — routing change ensures dispatch, preferences control delivery. |

### Non-Functional Requirements

| ID | Requirement |
|---|---|
| NFR-1 | Access level enforcement is server-side — client tab hiding is UX, not security |
| NFR-2 | Family invitation OTP uses the same OTP gateway as signup (`setup_otp_gateway.sql`) |
| NFR-3 | Invitation acceptance does NOT bypass school admin approval — it creates a `pending` `parent_child_links` row that the admin must approve |
| NFR-4 | Removing a family member sets their `parent_child_links.status` to `rejected` (soft delete, audit trail preserved) |
| NFR-5 | The primary parent cannot remove themselves if they are the only linked parent |
| NFR-6 | All changes to `parent_child_links` (access level, primary designation, removal) are auditable |

---

## 4. User Stories

### Primary Parent
- [ ] Invite a family member (grandparent, aunt, guardian) by phone number
- [ ] Specify relationship and access level for each family member
- [ ] View all family members linked to my child
- [ ] Change a family member's access level
- [ ] Remove a family member from the circle
- [ ] Transfer primary guardian designation to another family member
- [ ] Delegate PTM/event attendance to a family member (Phase 2)

### Family Member
- [ ] Receive an invitation via phone (OTP)
- [ ] Accept invitation by entering OTP
- [ ] View child's data per my access level
- [ ] See only the tabs/data I have access to
- [ ] RSVP for PTMs and events (Phase 2)
- [ ] Accept or decline event delegation (Phase 2)
- [ ] Configure my own notification preferences (Phase 2)

### School Admin
- [ ] See family circle link requests in the approval queue
- [ ] Approve or reject family member link requests
- [ ] See which parent invited the family member (`invitedBy` field)

### System
- [ ] Enforce access levels on every parent endpoint
- [ ] Route notifications to appropriate family members based on category
- [ ] Filter fee notifications to primary guardian only
- [ ] Prevent primary parent from removing themselves if only linked parent

---

## 5. Business Rules

### BR-001
**Rule:** Only one primary guardian per student per school.
**Enforcement:** `StudentAggregationService.enforceSinglePrimaryGuardian()` — existing, reused by `setPrimary()`.

### BR-002
**Rule:** Family members are never primary guardian by default.
**Enforcement:** `parent_child_links.isPrimaryGuardian = false` for all family circle invitations. Primary can be transferred later.

### BR-003
**Rule:** School admin approval required for all family links.
**Enforcement:** Invitation acceptance creates `parent_child_links` with `status = 'pending'`. Admin approves via existing `ParentLinkRouting.kt` flow.

### BR-004
**Rule:** Access levels are enforced server-side.
**Enforcement:** `ChildAccessResolver.resolveChildWithAccess()` checks `access_level` against `requiredLevels` set. Returns 403 if insufficient.

### BR-005
**Rule:** Primary parent cannot remove themselves if they are the only linked parent.
**Enforcement:** `removeFamilyMember()` checks count of approved links. If count == 1 and member is self, return 409.

### BR-006
**Rule:** OTP expires after 10 minutes. Max 3 attempts.
**Enforcement:** `family_invitations.expires_at` and `otp_attempts` column. After 3 failed attempts, invitation invalidated.

### BR-007
**Rule:** No duplicate pending invitations for same phone + student.
**Enforcement:** `UNIQUE(school_id, student_code, invitee_phone) WHERE status = 'pending'` on `family_invitations`.

### BR-008
**Rule:** Fee notifications go only to primary guardian.
**Enforcement:** `NotifyRecipients.familyMembersOfStudent(schoolId, code, setOf("full"))` filtered to `isPrimaryGuardian = true`.

### BR-009
**Rule:** Removing a family member is a soft delete.
**Enforcement:** Sets `parent_child_links.status = 'rejected'`. Does not delete the row. Audit trail preserved.

### BR-010
**Rule:** Existing approved links get `access_level = 'full'` on migration.
**Enforcement:** Backfill: `UPDATE parent_child_links SET access_level = 'full' WHERE status = 'approved' AND access_level IS NULL`.

---

## 6. Database Design

### 6.1 Entity Relationship Summary

Three tables: modified `parent_child_links` (add `access_level`, `invited_by`), new `family_invitations` (invitation workflow), new `event_delegations` (Phase 2 delegation).

### 6.2 New Tables

#### `family_invitations` table

```sql
CREATE TABLE IF NOT EXISTS family_invitations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    student_code    VARCHAR(64) NOT NULL,           -- canonical student identifier (NOT child_id)
    invited_by      UUID NOT NULL,                  -- primary parent's app_users.id
    invitee_phone   VARCHAR(32) NOT NULL,           -- phone number to send OTP to
    invitee_name    TEXT,                            -- optional display name
    relationship    VARCHAR(32) NOT NULL,           -- father | mother | grandfather | grandmother | guardian | sibling | aunt | uncle
    access_level    VARCHAR(16) NOT NULL DEFAULT 'limited',
    status          VARCHAR(16) NOT NULL DEFAULT 'pending',  -- pending | accepted | rejected | expired
    otp             VARCHAR(8),
    otp_attempts    INTEGER NOT NULL DEFAULT 0,
    expires_at      TIMESTAMP NOT NULL,
    accepted_at     TIMESTAMP,
    accepted_by     UUID,                            -- app_users.id of the invitee who accepted
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

-- Prevent duplicate pending invitations to the same phone for the same student
CREATE UNIQUE INDEX IF NOT EXISTS ux_family_invitations_pending
    ON family_invitations (school_id, student_code, invitee_phone)
    WHERE status = 'pending';
```

**Why `student_code` instead of `child_id`**: The `children` table row is only created **after** school admin approval of a `parent_child_links` request (`ParentLinkRouting.kt:757-791`). If a primary parent tries to invite a family member before the child link is approved, there's no `child_id` yet. The invitation keys on `(school_id, student_code)` — the same identifiers `parent_child_links` uses.

#### `event_delegations` table (Phase 2)

```sql
CREATE TABLE IF NOT EXISTS event_delegations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    event_id        UUID NOT NULL,                  -- calendar_events.id
    student_code    VARCHAR(64) NOT NULL,
    delegated_by    UUID NOT NULL,                  -- primary parent's app_users.id
    delegated_to    UUID NOT NULL,                  -- family member's app_users.id
    status          VARCHAR(16) NOT NULL DEFAULT 'pending',  -- pending | accepted | declined
    message         TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    responded_at    TIMESTAMP
);
```

### 6.3 Modified Tables

#### `parent_child_links` — add two columns

```sql
-- Migration: docs/db/migration_052_family_circle.sql

-- Access level scoping (NEW — does not exist yet)
ALTER TABLE parent_child_links ADD COLUMN IF NOT EXISTS access_level VARCHAR(16) NOT NULL DEFAULT 'full';
-- full | limited | view_only

-- Who invited this family member (NEW — does not exist yet)
ALTER TABLE parent_child_links ADD COLUMN IF NOT EXISTS invited_by UUID;
-- References app_users.id of the primary parent who sent the invitation
```

**What we do NOT add** (already exists):
- `is_primary` → use existing `is_primary_guardian` (bool, default false)
- `relationship` → use existing `relation` (varchar 32, nullable)

**Backfill existing rows**: All existing approved links get `access_level = 'full'` (they are primary parents who already have full access).

```sql
UPDATE parent_child_links SET access_level = 'full' WHERE status = 'approved' AND access_level IS NULL;
```

### 6.4 Indexes

- `ux_family_invitations_pending` — UNIQUE on `(school_id, student_code, invitee_phone) WHERE status = 'pending'`
- `parent_child_links(parent_id, status)` — for `resolveAccessibleChildren()` query (add if not exists)

### 6.5 Constraints

- `family_invitations.school_id` — NOT NULL
- `family_invitations.student_code` — NOT NULL, VARCHAR(64)
- `family_invitations.invited_by` — NOT NULL
- `family_invitations.invitee_phone` — NOT NULL, VARCHAR(32)
- `family_invitations.relationship` — NOT NULL, VARCHAR(32)
- `family_invitations.access_level` — NOT NULL, default 'limited'
- `family_invitations.status` — NOT NULL, default 'pending'
- `family_invitations.expires_at` — NOT NULL
- `parent_child_links.access_level` — NOT NULL, default 'full'
- `event_delegations.school_id` — NOT NULL
- `event_delegations.event_id` — NOT NULL
- `event_delegations.delegated_by` — NOT NULL
- `event_delegations.delegated_to` — NOT NULL

### 6.6 Foreign Keys

- `family_invitations.school_id` → `schools.id` (implicit)
- `family_invitations.invited_by` → `app_users.id` (implicit)
- `family_invitations.accepted_by` → `app_users.id` (implicit)
- `parent_child_links.invited_by` → `app_users.id` (implicit)
- `event_delegations.event_id` → `calendar_events.id` (implicit)
- `event_delegations.delegated_by` → `app_users.id` (implicit)
- `event_delegations.delegated_to` → `app_users.id` (implicit)

### 6.7 Soft Delete Strategy

- Family member removal: `parent_child_links.status = 'rejected'` (soft delete, audit trail preserved)
- Invitation rejection: `family_invitations.status = 'rejected'` or `'expired'`
- Delegation decline: `event_delegations.status = 'declined'`

### 6.8 Audit Fields

- `parent_child_links`: `requestedAt`, `actionedBy`, `actionedAt` (existing)
- `family_invitations`: `created_at`, `accepted_at`, `accepted_by`
- `event_delegations`: `created_at`, `responded_at`

### 6.9 Migration Notes

Migration: `docs/db/migration_052_family_circle.sql`
- ALTER `parent_child_links` — add `access_level`, `invited_by`
- CREATE `family_invitations` table
- Backfill existing approved links with `access_level = 'full'`
- No data loss — all existing functionality preserved

Migration: `docs/db/migration_053_event_delegations.sql` (Phase 2)
- CREATE `event_delegations` table

### 6.10 Exposed Mappings

```kotlin
// Inside existing ParentChildLinksTable object:
val accessLevel = varchar("access_level", 16).default("full")  // full | limited | view_only
val invitedBy   = uuid("invited_by").nullable()

// New tables:
object FamilyInvitationsTable : UUIDTable("family_invitations", "id") {
    val schoolId       = uuid("school_id")
    val studentCode    = varchar("student_code", 64)
    val invitedBy      = uuid("invited_by")
    val inviteePhone   = varchar("invitee_phone", 32)
    val inviteeName    = text("invitee_name").nullable()
    val relationship   = varchar("relationship", 32)
    val accessLevel    = varchar("access_level", 16).default("limited")
    val status         = varchar("status", 16).default("pending")
    val otp            = varchar("otp", 8).nullable()
    val otpAttempts    = integer("otp_attempts").default(0)
    val expiresAt      = timestamp("expires_at")
    val acceptedAt     = timestamp("accepted_at").nullable()
    val acceptedBy     = uuid("accepted_by").nullable()
    val createdAt      = timestamp("created_at")
}

object EventDelegationsTable : UUIDTable("event_delegations", "id") {
    val schoolId       = uuid("school_id")
    val eventId        = uuid("event_id")
    val studentCode    = varchar("student_code", 64)
    val delegatedBy    = uuid("delegated_by")
    val delegatedTo    = uuid("delegated_to")
    val status         = varchar("status", 16).default("pending")
    val message        = text("message").nullable()
    val createdAt      = timestamp("created_at")
    val respondedAt    = timestamp("responded_at").nullable()
}
```

Register both new tables in `DatabaseFactory.allTables`.

### 6.11 Seed Data

N/A — family circle data created by users.

---

## 7. State Machines

### Invitation State Machine

```
pending ──otp_verified──> accepted ──admin_approves──> link_approved
  │                          │
  │──otp_expired──> expired  │──admin_rejects──> link_rejected
  │                          │
  └──3_failed_attempts──> expired
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `pending` | OTP verified by invitee | `accepted` | OTP correct, not expired, attempts < 3 |
| `pending` | OTP expired (10 min) | `expired` | `expires_at < now()` |
| `pending` | 3 failed OTP attempts | `expired` | `otp_attempts >= 3` |
| `accepted` | Admin approves link | `link_approved` | Admin approves via `ParentLinkRouting.kt` |
| `accepted` | Admin rejects link | `link_rejected` | Admin rejects via `ParentLinkRouting.kt` |

### Parent-Child Link State Machine (Extended)

Existing states: `pending` → `approved` / `rejected` / `needs_review`

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `pending` | Admin approves | `approved` | Existing flow |
| `pending` | Admin rejects | `rejected` | Existing flow |
| `approved` | Primary parent removes member | `rejected` | Soft delete; not self if only link |
| `approved` | Access level changed | `approved` | `access_level` column updated |

### Delegation State Machine (Phase 2)

```
pending ──family_member_accepts──> accepted
  │
  └──family_member_declines──> declined
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `pending` | Family member accepts | `accepted` | Family member is linked and approved |
| `pending` | Family member declines | `declined` | Family member is linked and approved |

---

## 8. Backend Architecture

### 8.1 Component Overview

`FamilyCircleService` handles invitation, acceptance, removal, access level updates, and primary transfer. `ChildAccessResolver` is the core child resolution refactor that replaces `ChildrenTable.parentId eq uid` pattern. `FamilyCircleRouting` exposes management endpoints. `FamilyAcceptRouting` handles OTP acceptance.

### 8.2 Design Principles

1. **Access pattern over ownership pattern** — `resolveAccessibleChildren()` replaces `ChildrenTable.parentId eq uid` across all endpoints
2. **Server-side enforcement** — access levels checked server-side; client tab hiding is UX only
3. **Reuse existing infrastructure** — OTP gateway, admin approval flow, `enforceSinglePrimaryGuardian()`, `NotificationPreferencesTable`
4. **No duplication** — existing `relation` and `isPrimaryGuardian` columns reused, not duplicated
5. **Admin approval preserved** — family invitations create `pending` links, not `approved` ones

### 8.3 Core Types

#### ChildAccessResolver

```kotlin
// server/.../feature/family/ChildAccessResolver.kt

data class AccessibleChild(
    val childId: UUID,           // children.id (resolved from parent_child_links)
    val childName: String,
    val schoolId: UUID?,
    val studentCode: String?,
    val currentGrade: String?,
    val profilePic: String?,
    val overallProgress: Double,
    val currentLevel: Int,
    val attendanceStatus: String,
    val accessLevel: String,     // full | limited | view_only
    val isPrimaryGuardian: Boolean,
    val relation: String?,
)

object ChildAccessResolver {
    /**
     * Resolve ALL children the authenticated user can access.
     * Path 1 (primary parent): children WHERE children.parent_id = uid
     * Path 2 (family member): parent_child_links WHERE parent_id = uid AND status = 'approved'
     *   → resolve children via (school_id, student_code) join
     * Both paths UNIONed and deduplicated by child_id.
     */
    suspend fun resolveAccessibleChildren(uid: UUID): List<AccessibleChild>

    /**
     * Resolve a single child by ID, verifying the user has access.
     * Returns null (404) if child doesn't exist or user has no link.
     * Does NOT check access_level — caller decides what to gate.
     */
    suspend fun resolveAccessibleChild(uid: UUID, childId: UUID): AccessibleChild?

    /**
     * Resolve a single child by ID, verifying access AND access_level sufficiency.
     * Returns 403 if access_level is insufficient.
     */
    suspend fun resolveChildWithAccess(
        uid: UUID,
        childId: UUID,
        requiredLevels: Set<String>  // e.g. setOf("full") for fees
    ): AccessibleChild?

    /**
     * Resolve school IDs for all children the user can access.
     * Replaces pattern in ParentRouting.kt and NotificationsRouting.kt.
     */
    suspend fun resolveAccessibleSchoolIds(uid: UUID): List<UUID>
}
```

#### FamilyCircleService

```kotlin
// server/.../feature/family/FamilyCircleService.kt

class FamilyCircleService {
    suspend fun inviteFamilyMember(parentId: UUID, schoolId: UUID, studentCode: String, request: InviteRequest): UUID
    suspend fun acceptInvitation(phone: String, otp: String): AcceptResult
    suspend fun removeFamilyMember(parentId: UUID, schoolId: UUID, studentCode: String, memberUserId: UUID)
    suspend fun updateAccessLevel(parentId: UUID, schoolId: UUID, studentCode: String, memberUserId: UUID, newLevel: String)
    suspend fun getFamilyCircle(parentId: UUID, schoolId: UUID, studentCode: String): List<FamilyMemberDto>
    suspend fun setPrimary(parentId: UUID, schoolId: UUID, studentCode: String, newPrimaryId: UUID)
}
```

### 8.4 Repositories

- `FamilyInvitationRepository` — CRUD for `family_invitations`
- `EventDelegationRepository` — CRUD for `event_delegations` (Phase 2)

### 8.5 Mappers

- `FamilyMemberMapper` — maps `parent_child_links` + `app_users` rows to `FamilyMemberDto`
- `AccessibleChildMapper` — maps `children` + `parent_child_links` rows to `AccessibleChild`

### 8.6 Permission Checks

- Family circle management: caller must be primary guardian for `(schoolId, studentCode)`
- Invitation acceptance: OTP verification (no JWT required for this endpoint)
- Child access: `resolveChildWithAccess()` with appropriate `requiredLevels` set
- Admin approval: existing `requireSchoolAdmin()` in `ParentLinkRouting.kt`

### 8.7 Background Jobs

N/A — no background jobs for family circle. All operations are user-initiated.

### 8.8 Domain Events

- `FamilyMemberInvited` — emitted on invitation creation; triggers OTP send
- `FamilyInvitationAccepted` — emitted on OTP acceptance; triggers admin notification
- `FamilyMemberRemoved` — emitted on removal; logged
- `AccessLevelChanged` — emitted on access level update; logged
- `PrimaryGuardianTransferred` — emitted on primary transfer; triggers `enforceSinglePrimaryGuardian()`
- `EventDelegated` — emitted on delegation creation (Phase 2)

### 8.9 Caching

- Accessible children: cached per user, 5-minute TTL, invalidated on link change
- Family circle: cached per (schoolId, studentCode), 5-minute TTL, invalidated on member change
- No cache for invitations (real-time)

### 8.10 Transactions

- Invitation acceptance: single transaction (verify OTP → create/find user → create link → mark invitation accepted)
- Primary transfer: single transaction (update old primary → update new primary → `enforceSinglePrimaryGuardian()`)
- Access level update: single transaction

### 8.11 Rate Limiting

- OTP sending: rate-limited per phone per hour (existing OTP gateway limits)
- Invitation creation: rate-limited per primary parent per hour
- Standard API rate limiting for all endpoints

### 8.12 Configuration

- `FAMILY_CIRCLE_ENABLED` — default `true`; enable/disable feature
- `FAMILY_INVITATION_OTP_EXPIRY_MINUTES` — default `10`; OTP expiry
- `FAMILY_INVITATION_MAX_ATTEMPTS` — default `3`; max OTP attempts
- `FAMILY_CIRCLE_CACHE_TTL` — default `300`; cache TTL in seconds

---

## 9. API Contracts

### 9.1 Family Circle Management (Phase 1)

All require `requireAuth()` + primary guardian verification.

```
GET    /api/v1/parent/family-circle/{schoolId}/{studentCode}
  → 200: { members: [FamilyMemberDto] }
  → 403: Not primary guardian

POST   /api/v1/parent/family-circle/{schoolId}/{studentCode}/invite
  Body: { phone, name?, relationship, access_level }
  → 200: { invitationId }
  → 403: Not primary guardian
  → 409: Pending invitation already exists for this phone

DELETE /api/v1/parent/family-circle/{schoolId}/{studentCode}/members/{userId}
  → 200: { message: "Member removed" }
  → 403: Not primary guardian
  → 409: Cannot remove last remaining parent

PATCH  /api/v1/parent/family-circle/{schoolId}/{studentCode}/members/{userId}
  Body: { access_level }
  → 200: { message: "Access level updated" }
  → 403: Not primary guardian

POST   /api/v1/parent/family-circle/{schoolId}/{studentCode}/set-primary
  Body: { userId }
  → 200: { message: "Primary guardian updated" }
  → 403: Not current primary guardian
```

### 9.2 Invitation Acceptance (Phase 1)

No JWT required — OTP verification is the auth mechanism.

```
POST   /api/v1/auth/family-accept
  Body: { phone, otp }
  → 200: { success: true, message: "Linked — awaiting school admin approval", linkStatus: "pending" }
  → 200: { success: true, message: "Already linked to this child", linkStatus: "already_linked" }
  → 400: Invalid or expired OTP
  → 404: No pending invitation found for this phone
```

**Note**: The `userId` comes from the JWT token if the invitee is already logged in, or a new account is created during acceptance. If the invitee has no account, the response includes a temporary token for onboarding completion.

### 9.3 Delegation (Phase 2)

```
POST   /api/v1/parent/family-circle/{schoolId}/{studentCode}/delegate
  Body: { event_id, delegated_to_user_id, message? }
  → 200: { delegationId }

GET    /api/v1/parent/delegations
  → 200: { delegations: [DelegationDto] }

POST   /api/v1/parent/delegations/{id}/respond
  Body: { status: "accepted" | "declined" }
  → 200: { message: "Delegation responded" }
```

### 9.4 DTO Models

All `@Serializable`, wrapped in `ApiResponse<T>` pattern.

```kotlin
@Serializable data class InviteRequest(
    val phone: String,
    val name: String? = null,
    val relationship: String,
    val accessLevel: String,
)

@Serializable data class FamilyMemberDto(
    val userId: String,
    val name: String,
    val phone: String?,
    val relationship: String,
    val accessLevel: String,
    val isPrimaryGuardian: Boolean,
    val profilePic: String? = null,
    val linkedAt: String,
)

@Serializable data class AcceptResult(
    val success: Boolean,
    val message: String,
    val linkStatus: String,  // pending | already_linked
)

@Serializable data class FamilyCircleResponse(
    val members: List<FamilyMemberDto>,
)
```

### 9.5 Dashboard Response (Modified)

```kotlin
@Serializable data class DashboardResponse(
    // ... existing fields ...
    val accessLevel: String = "full",  // NEW — caller's access level for selected child
)
```

### 9.6 How Each Endpoint Changes

| Endpoint | Current Resolution | New Resolution | Access Gate |
|---|---|---|---|
| `GET /parent/dashboard` | `ChildrenTable.parentId eq uid` | `resolveAccessibleChildren(uid)` | Filter alerts by access_level |
| `GET /parent/child/{id}/attendance` | `requireOwnedChild()` | `resolveChildWithAccess(uid, childId, setOf("full", "limited"))` | 403 if `view_only` |
| `GET /parent/child/{id}/marks` | `requireOwnedChild()` | `resolveChildWithAccess(uid, childId, setOf("full", "limited"))` | 403 if `view_only` |
| `GET /parent/child/{id}/syllabus` | `requireOwnedChild()` | `resolveChildWithAccess(uid, childId, setOf("full", "limited"))` | 403 if `view_only` |
| `GET /parent/child/{id}/timetable` | `requireOwnedChild()` | `resolveChildWithAccess(uid, childId, setOf("full", "limited"))` | 403 if `view_only` |
| `GET /parent/fees` | `FeeRecordsTable.parentId eq uid` | Resolve primary parent's fee records via `parent_child_links`; 403 if access_level != `full` | 403 if not `full` |
| `GET /parent/health/{childId}` | `ChildrenTable.parentId eq uid` | `resolveChildWithAccess(uid, childId, setOf("full"))` | 403 if not `full` |
| `GET /parent/leave` | `ChildrenTable.parentId eq uid` | `resolveAccessibleChildren(uid)` for listing; `resolveChildWithAccess(uid, childId, setOf("full", "limited"))` for creating | `view_only` can list but not create |
| `GET /parent/pulse/latest/{childId}` | `service.getLatestPulse(uid, childId)` | `resolveChildWithAccess(uid, childId, setOf("full", "limited"))` | 403 if `view_only` |
| `GET /parent/announcements` | Schools via `ChildrenTable.parentId` | `resolveAccessibleSchoolIds(uid)` | All access levels |
| `GET /parent/notifications` | Schools via `ChildrenTable.parentId` + fees via `FeeRecordsTable.parentId` | `resolveAccessibleSchoolIds(uid)` + fee notifications only if `full` access | Filter fee notifications by access_level |
| `GET /parent/messages/threads` | `MessageThreadsTable.ownerUserId eq uid` | No change (already per-user) | All access levels |
| `GET /parent/messages/recipients` | `resolveParentSchoolId()` via `ChildrenTable.parentId` | `resolveAccessibleSchoolIds(uid).first()` | All access levels |

### 9.7 Fees Special Case

Fee records are keyed to `FeeRecordsTable.parentId` — the primary parent who registered the child. Family members don't have fee records. The fees endpoint needs to:

1. Find the primary parent for the selected child via `parent_child_links` where `isPrimaryGuardian = true` and `status = 'approved'`
2. Query `FeeRecordsTable` with that primary parent's ID (filtered by `child_id` if provided)
3. Return 403 if the caller's `access_level` is not `full`

This means a grandparent (`limited` or `view_only`) gets a 403 on the fees endpoint, and the client hides the Fees tab.

---

## 10. Frontend Architecture

### 10.1 Screens

| Screen | Platform | Role | Description |
|---|---|---|---|
| `FamilyCircleScreenV2` | Compose | Primary Parent | Family circle management: list, invite, remove, change access, transfer primary |
| `FamilyAcceptScreen` | Compose | Family Member | OTP entry for family invitation acceptance |
| `ParentPortalV2` (modified) | Compose | All | Tab visibility filtered by `accessLevel` |
| `ParentFeesScreenV2` (modified) | Compose | All | 403 handling for non-`full` access |
| `ParentHealthScreenV2` (modified) | Compose | All | 403 handling for non-`full` access |
| `ParentAcademicsScreenV2` (modified) | Compose | All | 403 handling for `view_only` access |

### 10.2 Navigation

- Profile tab → Family Circle → `FamilyCircleScreenV2`
- Auth/onboarding → Family Accept → `FamilyAcceptScreen` (deep link from OTP SMS)

### 10.3 UX Flows

#### Primary Parent: Invite Family Member

1. Primary parent opens Family Circle screen from Profile tab
2. Sees list of current family members with name, phone, relationship, access level, primary badge
3. Taps "Invite Member"
4. Enters phone number, name (optional), relationship, access level
5. Server sends OTP to invitee's phone
6. Primary parent sees "Invitation sent" confirmation

#### Family Member: Accept Invitation

1. Family member receives OTP on phone
2. Opens app → enters OTP on `FamilyAcceptScreen`
3. Server verifies OTP, creates `pending` link
4. Family member sees "Linked — awaiting school admin approval"
5. After admin approval, family member's dashboard shows the child

#### Primary Parent: Manage Family Circle

1. View all members
2. Change access level (dropdown: full / limited / view_only)
3. Remove member (with confirmation dialog)
4. Transfer primary (with confirmation dialog — warns about fee notification transfer)

### 10.4 State Management

```kotlin
data class ParentHomeState(
    // ... existing fields ...
    val accessLevel: String = "full",  // NEW
)

data class FamilyCircleState(
    val members: List<FamilyMemberDto>,
    val isLoading: Boolean,
    val error: String?,
    val showInviteDialog: Boolean,
    val showRemoveConfirmation: Boolean,
)
```

### 10.5 Offline Support

- Family circle list cached locally
- Access level cached locally (determines tab visibility offline)

### 10.6 Loading States

- Loading family circle: "Loading family members..."
- No members: "No family members linked yet. Invite someone to get started."

### 10.7 Error Handling (UI)

- 403 on fees: "You don't have access to fee details"
- 403 on health: "You don't have access to health records"
- 403 on academics: "You don't have access to academic details"
- 409 on remove self: "You cannot remove yourself as the only linked parent"
- 409 on duplicate invite: "An invitation is already pending for this phone number"

### 10.8 Component Integration Guidelines

| Rule | Description |
|---|---|
| **R1** | Tab list filtered by `accessLevel` from dashboard response |
| **R2** | `full` → all tabs (Home, Academics, Fees, Conversations, Profile) |
| **R3** | `limited` → all tabs except Fees |
| **R4** | `view_only` → only Home, Conversations, Profile |
| **R5** | 403 responses handled gracefully on all gated screens |
| **R6** | Family circle screen accessible from Profile tab |
| **R7** | Invite dialog with phone, name, relationship, access level fields |
| **R8** | Remove confirmation dialog with member name |
| **R9** | Primary transfer confirmation with warning about fee notification transfer |
| **R10** | Child switcher works for family members (dashboard returns accessible children) |

### 10.9 Tab Visibility Implementation

```kotlin
val allTabs = listOf(
    VNavItem("home", "Home", VIcons.Home),
    VNavItem("academics", "Academics", VIcons.School),
    VNavItem("fees", "Fees", VIcons.Wallet),
    VNavItem("conversations", "Conversations", VIcons.Chat, badge = notifications.unreadCount),
    VNavItem("profile", "Profile", VIcons.User),
)

val visibleTabs = when (dashboard.accessLevel) {
    "full"      -> allTabs
    "limited"   -> allTabs.filter { it.id != "fees" }
    "view_only" -> allTabs.filter { it.id in setOf("home", "conversations", "profile") }
    else        -> allTabs
}
```

---

## 11. Shared Module Changes (KMP)

### 11.1 DTOs

All DTOs defined in section 9.4, placed in `shared/.../parent/domain/model/ParentFeatureModels.kt`.

### 11.2 Domain Models

```kotlin
data class FamilyMember(
    val userId: String,
    val name: String,
    val phone: String?,
    val relationship: String,
    val accessLevel: AccessLevel,
    val isPrimaryGuardian: Boolean,
    val profilePic: String?,
    val linkedAt: Instant,
)

enum class AccessLevel { FULL, LIMITED, VIEW_ONLY }
```

### 11.3 Repository Interfaces

```kotlin
// shared/.../parent/domain/repository/ParentRepository.kt
suspend fun getFamilyCircle(token: String, schoolId: String, studentCode: String): NetworkResult<FamilyCircleResponse>
suspend fun inviteFamilyMember(token: String, schoolId: String, studentCode: String, request: InviteRequest): NetworkResult<InviteResponse>
suspend fun removeFamilyMember(token: String, schoolId: String, studentCode: String, userId: String): NetworkResult<Unit>
suspend fun updateAccessLevel(token: String, schoolId: String, studentCode: String, userId: String, level: String): NetworkResult<Unit>
suspend fun setPrimaryGuardian(token: String, schoolId: String, studentCode: String, userId: String): NetworkResult<Unit>
suspend fun acceptFamilyInvitation(phone: String, otp: String): NetworkResult<AcceptResult>
```

### 11.4 UseCases

- `GetFamilyCircleUseCase`
- `InviteFamilyMemberUseCase`
- `RemoveFamilyMemberUseCase`
- `UpdateAccessLevelUseCase`
- `SetPrimaryGuardianUseCase`
- `AcceptFamilyInvitationUseCase`

### 11.5 Validation

- Phone: valid phone format
- Relationship: one of father, mother, grandfather, grandmother, guardian, sibling, aunt, uncle
- Access level: one of full, limited, view_only
- OTP: 6 digits

### 11.6 Serialization

Standard Kotlinx serialization. `AccessLevel` enum serialized as lowercase string.

### 11.7 Network APIs

Ktor `@Resource` route definitions added to `ParentApi.kt`:
- Family circle management endpoints
- Family accept endpoint

### 11.8 Database Models (Local Cache)

- Family circle list cached locally
- Access level cached locally per child

---

## 12. Permissions Matrix

| Action | Super Admin | School Admin | Primary Parent | Family Member (full) | Family Member (limited) | Family Member (view_only) |
|---|---|---|---|---|---|---|
| View child dashboard | N/A | N/A | ✅ | ✅ | ✅ | ✅ |
| View academics | N/A | N/A | ✅ | ✅ | ✅ | ❌ |
| View fees | N/A | N/A | ✅ | ✅ | ❌ | ❌ |
| View health | N/A | N/A | ✅ | ✅ | ❌ | ❌ |
| View leave (list) | N/A | N/A | ✅ | ✅ | ✅ | ✅ |
| Create leave request | N/A | N/A | ✅ | ✅ | ✅ | ❌ |
| View pulse | N/A | N/A | ✅ | ✅ | ✅ | ❌ |
| View announcements | N/A | N/A | ✅ | ✅ | ✅ | ✅ |
| View messages | N/A | N/A | ✅ | ✅ | ✅ | ✅ |
| RSVP for events (Phase 2) | N/A | N/A | ✅ | ✅ | ✅ | ✅ |
| Invite family member | N/A | N/A | ✅ | ❌ | ❌ | ❌ |
| Remove family member | N/A | N/A | ✅ | ❌ | ❌ | ❌ |
| Change access level | N/A | N/A | ✅ | ❌ | ❌ | ❌ |
| Transfer primary | N/A | N/A | ✅ | ❌ | ❌ | ❌ |
| Accept delegation (Phase 2) | N/A | N/A | ✅ | ✅ | ✅ | ✅ |
| Approve/reject family link | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |

### Access Level → Endpoint Mapping

| Access Level | Allowed Endpoints | Blocked Endpoints |
|---|---|---|
| `full` | Everything (academics, attendance, marks, syllabus, timetable, fees, health, leave, pulse, messages, announcements, notifications) | Nothing |
| `limited` | Academics (attendance, marks, syllabus, timetable), leave (list + create), pulse, messages, announcements, notifications | Fees, Health |
| `view_only` | Announcements, notifications (announcements only, no fee notifications), messages, calendar/events | Academics, fees, health, leave (create), pulse |

### Dashboard Filtering by Access Level

- `full`: children list + all alerts (including overdue fees) + featured schools
- `limited`: children list + alerts minus fee alerts + featured schools
- `view_only`: children list + no alerts + featured schools

---

## 13. Notifications

### Family Circle Notification Triggers

| Type | Trigger | Recipient | Channel | Message |
|---|---|---|---|---|
| Family Invitation OTP | Primary parent invites member | Invitee | WhatsApp/SMS (OTP) | "Your family circle OTP is {code}. Expires in 10 minutes." |
| Family Link Pending | Invitee accepts invitation | School Admin | Push + Dashboard | "New family link request from {parent_name} for {student_name}" |
| Family Link Approved | Admin approves family link | Family Member | Push | "Your family link to {student_name} has been approved" |
| Family Member Removed | Primary parent removes member | Removed Member | Push | "Your access to {student_name}'s school data has been removed" |
| Primary Guardian Transferred | Primary transferred | New Primary | Push | "You are now the primary guardian for {student_name}" |
| Event Delegation (Phase 2) | Primary delegates event | Family Member | Push | "You've been delegated to attend {event_name} for {student_name}" |

### Notification Routing Changes

#### Current State

`NotifyRecipients.kt` resolves parent recipients via `ChildrenTable.parentId`:

```kotlin
// NotifyRecipients.parentsOfStudent()
ChildrenTable.selectAll().where {
    (ChildrenTable.schoolId eq schoolId) and
        (ChildrenTable.studentCode eq studentCode) and
        (ChildrenTable.isActive eq true)
}.map { it[ChildrenTable.parentId] }.distinct()
```

This only finds the primary parent. Family members linked via `parent_child_links` are invisible to the notification system.

#### Required Changes

Add a new resolver that queries `parent_child_links` in addition to `children`:

```kotlin
// NotifyRecipients.kt — new method

suspend fun familyMembersOfStudent(
    schoolId: UUID,
    studentCode: String,
    accessLevelFilter: Set<String>? = null  // null = all; setOf("full") = primary only for fees
): List<UUID> = dbQuery {
    // Path 1: primary parent via children.parent_id
    val primaryIds = ChildrenTable.selectAll().where {
        (ChildrenTable.schoolId eq schoolId) and
            (ChildrenTable.studentCode eq studentCode) and
            (ChildrenTable.isActive eq true)
    }.map { it[ChildrenTable.parentId] }

    // Path 2: all approved family members via parent_child_links
    val familyIds = ParentChildLinksTable.selectAll().where {
        (ParentChildLinksTable.schoolId eq schoolId) and
            (ParentChildLinksTable.studentCode eq studentCode) and
            (ParentChildLinksTable.status eq "approved")
    }.let { rows ->
        if (accessLevelFilter != null) {
            rows.filter { it[ParentChildLinksTable.accessLevel] in accessLevelFilter }
        } else rows
    }.map { it[ParentChildLinksTable.parentId] }

    (primaryIds + familyIds).distinct()
}
```

### Routing Rules

| Event Category | Who Gets Notified | Resolver Change |
|---|---|---|
| `attendance` (absent/late) | All family members | `familyMembersOfStudent(schoolId, code)` |
| `marks` (results published) | All family members | `familyMembersOfStudent(schoolId, code)` |
| `homework` (new assignment) | All family members | `familyMembersOfStudent(schoolId, code)` via `parentsOfClass` equivalent |
| `announcement` (school post) | All family members | `familyMembersOfStudent` per audience scope |
| `fees` (due/overdue reminder) | Primary parent only | `familyMembersOfStudent(schoolId, code, setOf("full"))` — but only the `isPrimaryGuardian = true` one |
| `leave` (request status change) | All family members with `full` or `limited` | `familyMembersOfStudent(schoolId, code, setOf("full", "limited"))` |

### Per-User Preferences (Already Supported)

`NotificationPreferencesTable` is already per-user per-category. Each family member can independently enable/disable notification categories. No change needed here — the routing change above ensures notifications are **dispatched** to the right people; preferences control whether they're **delivered**.

---

## 14. Background Jobs

N/A — family circle has no background jobs. All operations are user-initiated (invitation, acceptance, removal, access level changes, primary transfer). Notifications are sent inline via `NotificationService`.

---

## 15. Integrations

### Internal Integrations

| System | Integration Point | Direction | Protocol | Error Handling |
|---|---|---|---|---|
| `ParentChildLinksTable` | Access level, invited_by, link status | Read/Write | Direct DB | Standard error handling |
| `ChildrenTable` | Child resolution for primary parent path | Read | Direct DB | Return empty if no children |
| `AppUsersTable` | User account creation/lookup for invitee | Read/Write | Direct DB | Create if not found |
| `StudentAggregationService` | `enforceSinglePrimaryGuardian()` | Call | Direct call | Throws on violation |
| `ParentLinkRouting.kt` | Admin approval queue | Read | Existing flow | No change needed |
| `NotifyRecipients.kt` | `familyMembersOfStudent()` | Read | Direct DB | Return empty list if no family members |
| `NotificationService` | Push notifications for events | Outbound | Direct call | Logged; non-blocking |
| `NotificationPreferencesTable` | Per-user notification preferences | Read | Direct DB | Default to enabled |
| OTP gateway | Send OTP via WhatsApp/SMS | Outbound | HTTP API | Logged; retry 3x |
| `FeeRecordsTable` | Fee records for primary parent | Read | Direct DB | Return empty if no fees |
| `CalendarEventsTable` | Events for delegation (Phase 2) | Read | Direct DB | Return 404 if event not found |

### External Integrations

| System | Purpose | Direction | Protocol | Authentication | Error Handling |
|---|---|---|---|---|---|
| OTP Gateway (WhatsApp/SMS) | Send family invitation OTP | Outbound | HTTP API | Bearer token (existing) | Retry 3x; log on failure |

### Integration Patterns

- **Child resolution:** `ChildAccessResolver` replaces `ChildrenTable.parentId eq uid` across 7 endpoint files. UNION of primary parent path and family member path.
- **Notification routing:** `NotifyRecipients.familyMembersOfStudent()` replaces `parentsOfStudent()` for family-inclusive notifications. Filter by `accessLevel` for fee-only notifications.
- **Admin approval:** Family invitations create `pending` links that flow through existing `ParentLinkRouting.kt` approval queue. No new approval flow.
- **OTP:** Reuses existing OTP gateway infrastructure. Same rate limits, same delivery channels.

### Existing Code That Must Not Be Duplicated

| Existing Capability | Location | What It Does | How This Spec Uses It |
|---|---|---|---|
| Primary guardian enforcement | `StudentAggregationService.enforceSinglePrimaryGuardian()` | Ensures at most one primary per student | `setPrimary()` calls this — does not reimplement |
| `relation` column | `ParentChildLinksTable.relation` | Stores Father/Mother/Guardian/etc. | Reused — no new `relationship` column |
| `isPrimaryGuardian` column | `ParentChildLinksTable.isPrimaryGuardian` | Marks the primary parent | Reused — no new `is_primary` column |
| Admin approval flow | `ParentLinkRouting.kt` decision endpoint | Approves/rejects pending links | Family invitations create pending links that flow through this same endpoint |
| OTP gateway | `setup_otp_gateway.sql` + OTP service | Sends OTP via WhatsApp/SMS | Family invitation OTP uses the same gateway |
| Notification spine | `Notify.toUsers()` + `NotifyRecipients` | Dispatches notifications to users | Extended with `familyMembersOfStudent()` — does not bypass |
| Notification preferences | `NotificationPreferencesTable` | Per-user per-category opt-in/out | No change — works as-is for family members |
| Child switcher | `ParentHeader` dropdown in `ParentPortalV2.kt` | Switches active child | Works as-is — dashboard returns accessible children |

---

## 16. Security

### Authentication

- Family circle management: JWT auth via `requireAuth()` + primary guardian verification
- Invitation acceptance: OTP verification (no JWT required — OTP is the auth mechanism)
- Child access: JWT auth via `requireAuth()` + `resolveChildWithAccess()`
- Admin approval: `requireSchoolAdmin()` (existing)

### Authorization

- Only primary guardian can manage family circle (invite, remove, change access, transfer primary)
- Family members can only view data per their access level
- School admin approves/rejects all family link requests
- Server validates parent→student relationship on every endpoint

### Data Protection

- Family circle data is parent-scoped — primary parent manages, family members view per access level
- Student data visible to family members — standard PII, access-controlled
- Phone numbers in invitations — PII, used for OTP only
- Fee data restricted to `full` access level only
- Health data restricted to `full` access level only

### Input Validation

- Phone: valid phone format, max 32 characters
- Name: max 100 characters (optional)
- Relationship: one of father, mother, grandfather, grandmother, guardian, sibling, aunt, uncle
- Access level: one of full, limited, view_only
- OTP: 6 digits, numeric
- User ID: valid UUID

### Rate Limiting

- OTP sending: rate-limited per phone per hour (existing OTP gateway limits)
- Invitation creation: rate-limited per primary parent per hour
- Standard API rate limiting for all endpoints

### Audit Logging

- Family member invitation: primary parent ID, invitee phone, relationship, access level
- Invitation acceptance: invitee phone, OTP verified, account created/linked
- Family member removal: primary parent ID, removed member ID
- Access level change: primary parent ID, member ID, old level, new level
- Primary guardian transfer: old primary ID, new primary ID
- Admin approval/rejection: admin ID, link ID, decision

### PII Handling

- Phone numbers in `family_invitations` — PII, used for OTP delivery
- Phone numbers in `app_users` — existing PII, reused for family members
- Student data visible to family members — standard PII, access-controlled
- No additional PII beyond standard school management

### Multi-tenant Isolation

- `parent_child_links.school_id` — school-scoped
- `family_invitations.school_id` — school-scoped
- `event_delegations.school_id` — school-scoped
- All queries filtered by `school_id`
- Family circle is per-school (no cross-school family linking)

---

## 17. Performance & Scalability

### Expected Scale

- Small school: 1-2 family members per child, 50-200 children
- Medium school: 2-3 family members per child, 200-1,000 children
- Large school: 2-4 family members per child, 1,000-3,000 children
- Total family links: 2,000-12,000 per school

### Query Optimization

- **`resolveAccessibleChildren()`:** UNION of two indexed lookups — `children.parent_id` (PK) and `parent_child_links(parent_id, status)` (new index). O(1) per parent.
- **`familyMembersOfStudent()`:** Two indexed lookups — `children(school_id, student_code)` and `parent_child_links(school_id, student_code, status)`. Deduplicated. O(1) per student.
- **Family circle listing:** `parent_child_links(school_id, student_code, status='approved')` joined with `app_users`. Small dataset (2-4 rows).
- **Invitation lookup:** `family_invitations(invitee_phone, status='pending')`. Indexed via unique partial index.

### Indexing Strategy

- `parent_child_links(parent_id, status)` — for `resolveAccessibleChildren()` (add if not exists)
- `parent_child_links(school_id, student_code, status)` — for `familyMembersOfStudent()` (existing)
- `ux_family_invitations_pending` — UNIQUE on `(school_id, student_code, invitee_phone) WHERE status = 'pending'`
- `family_invitations(invitee_phone, status)` — for OTP verification lookup

### Caching Strategy

- Accessible children: cached per user, 5-minute TTL, invalidated on link change
- Family circle: cached per (schoolId, studentCode), 5-minute TTL, invalidated on member change
- No cache for invitations (real-time)

### Pagination

N/A — family circle is small (2-4 members). No pagination needed.

### Connection Pooling

Uses existing HikariCP connection pool. No additional pooling needed.

### Async Processing

- OTP sending: async (fire-and-forget with logging)
- Notification delivery: async (existing `NotificationService` pattern)
- All other operations: synchronous

### Scalability Concerns

- `resolveAccessibleChildren()` called on every dashboard load: UNION of two indexed queries. <10ms per call.
- `familyMembersOfStudent()` called per notification dispatch: two indexed queries. <5ms per call.
- Family link count: 2,000-12,000 per school. Small table. No performance concerns.

---

## 18. Edge Cases

| # | Scenario | Expected Behavior |
|---|---|---|
| EC-1 | Primary parent invites someone already linked to the child | Return 200 with `linkStatus: "already_linked"` |
| EC-2 | Primary parent invites themselves | Return 400 "You cannot invite yourself." |
| EC-3 | OTP expired (10 minutes) | Return 400 "Invalid or expired OTP." |
| EC-4 | 3 failed OTP attempts | Invitation status → `expired`. Return 400 "Invitation invalidated due to too many attempts." |
| EC-5 | Duplicate pending invitation for same phone + student | Return 409 "Pending invitation already exists for this phone." |
| EC-6 | Non-primary parent tries to invite | Return 403 "Only the primary guardian can manage the family circle." |
| EC-7 | Primary parent tries to remove themselves as only linked parent | Return 409 "Cannot remove last remaining parent." |
| EC-8 | Family member with `view_only` tries to access fees | Return 403 "You don't have access to fee details." |
| EC-9 | Family member with `limited` tries to access health | Return 403 "You don't have access to health records." |
| EC-10 | Family member with `view_only` tries to create leave request | Return 403 "You don't have access to create leave requests." |
| EC-11 | Admin rejects family link request | Link status → `rejected`. Family member notified. Cannot access child data. |
| EC-12 | Primary parent transfers primary to a `view_only` member | Member's access level upgraded to `full` automatically. `enforceSinglePrimaryGuardian()` called. |
| EC-13 | Family member's link is `pending` (admin not yet approved) | `resolveAccessibleChildren()` does NOT include this child. Family member sees no children. |
| EC-14 | Invitee's phone matches existing app_users account | Use existing account. No new account created. Link created with that account's ID. |
| EC-15 | Invitee's phone does not match any account | Create new `app_users` with `role='parent'`, `isPhoneVerified=true`. Return temporary token for onboarding. |
| EC-16 | Family member tries to access another school's child | Return 403. `resolveChildWithAccess()` validates school scope. |
| EC-17 | Primary parent changes access level of another primary | Not possible — only one primary exists. `setPrimary()` is a separate operation. |
| EC-18 | Invitation accepted but admin never approves | Link remains `pending`. Family member cannot access child data. Invitation remains `accepted`. |

---

## 19. Error Handling

### Error Response Format

Standard `ApiResponse` error format:
```json
{
  "success": false,
  "error": {
    "code": "NOT_PRIMARY_GUARDIAN",
    "message": "Only the primary guardian can manage the family circle",
    "details": {}
  }
}
```

### Error Codes

| Code | HTTP Status | Description | User Message |
|---|---|---|---|
| `NOT_PRIMARY_GUARDIAN` | 403 | Caller is not primary guardian | "Only the primary guardian can manage the family circle." |
| `INSUFFICIENT_ACCESS` | 403 | Access level too low | "You don't have access to {feature}." |
| `DUPLICATE_INVITATION` | 409 | Pending invitation already exists | "An invitation is already pending for this phone number." |
| `CANNOT_REMOVE_LAST_PARENT` | 409 | Trying to remove only linked parent | "Cannot remove last remaining parent." |
| `INVALID_OTP` | 400 | OTP incorrect or expired | "Invalid or expired OTP." |
| `INVITATION_NOT_FOUND` | 404 | No pending invitation for phone | "No pending invitation found for this phone." |
| `INVITATION_EXPIRED` | 400 | Invitation expired (time or attempts) | "Invitation has expired." |
| `CHILD_NOT_ACCESSIBLE` | 403 | User has no link to this child | "You don't have access to this child." |
| `CANNOT_INVITE_SELF` | 400 | Primary parent inviting themselves | "You cannot invite yourself." |

### Error Handling Strategy

- **Validation errors:** Return 400 with field-specific message
- **Auth errors:** Return 401/403 with clear message
- **Not found:** Return 404
- **Conflicts:** Return 409
- **Server errors:** Return 500 with generic message; log full error

### Retry Strategy

- Client retries: 3 attempts with exponential backoff for 5xx errors
- No retry for 4xx errors (client errors)
- OTP sending: 3 retries with 5-second intervals (existing OTP gateway pattern)

### Fallback Behavior

- OTP gateway unavailable: Return 500 "Failed to send OTP. Please try again."
- Notification delivery failure: Logged; non-blocking
- `resolveAccessibleChildren()` returns empty: Client shows unlinked-parent gate (existing behavior)

---

## 20. Analytics & Reporting

### Analytics Dashboard Data

| Metric | Source | Derivation |
|---|---|---|
| Total family circles | `parent_child_links` count where `invited_by IS NOT NULL` | Distinct (school_id, student_code) with family members |
| Average family members per child | `parent_child_links` count / distinct children | Aggregate |
| Access level distribution | `parent_child_links.access_level` | Group by access_level, count |
| Pending family link requests | `parent_child_links` where `status='pending'` and `invited_by IS NOT NULL` | Direct count |
| Pending invitations | `family_invitations` where `status='pending'` | Direct count |
| Invitation acceptance rate | `family_invitations` accepted / total | Percentage |
| Admin approval rate | `parent_child_links` approved / total family links | Percentage |

### Export Capabilities

- Family circle export (CSV) — child, member, relationship, access level, status

### Report Types

| Report | Format | Frequency | Recipient |
|---|---|---|---|
| Family circle summary | JSON (API) | On-demand | School Admin |
| Access level distribution | JSON (API) | On-demand | School Admin |
| Pending approvals | JSON (API) | On-demand | School Admin |

---

## 21. Testing Strategy

### Unit Tests

- `ChildAccessResolver` — `resolveAccessibleChildren()`, `resolveChildWithAccess()`, `resolveAccessibleSchoolIds()`
- `FamilyCircleService` — invite, accept, remove, updateAccessLevel, getFamilyCircle, setPrimary
- Access level enforcement: `full` sees all, `limited` sees academics not fees/health, `view_only` sees announcements only
- OTP verification: correct OTP, expired OTP, max attempts exceeded
- Primary guardian enforcement: `enforceSinglePrimaryGuardian()` called on transfer
- Remove last parent: returns 409
- Duplicate invitation: returns 409

### Integration Tests

- Full invitation flow: invite → OTP → accept → admin approval → family member sees child
- Access level enforcement: `limited` member gets 403 on fees, `view_only` gets 403 on academics
- Notification routing: family members receive attendance notifications, only primary receives fee notifications
- Dashboard filtering: `limited` dashboard has no fee alerts, `view_only` dashboard has no alerts
- Primary transfer: old primary loses primary, new primary gains primary, `enforceSinglePrimaryGuardian` validates
- Multi-tenant: school A family circle not accessible to school B
- Child resolution: family member sees child via `parent_child_links`, not `children.parentId`

### E2E Tests

- Primary parent invites grandparent → grandparent enters OTP → admin approves → grandparent sees child with `limited` access → grandparent sees academics but not fees
- Primary parent removes family member → member loses access → member's dashboard shows no children
- Primary parent transfers primary → new primary can invite members → old primary cannot

### Performance Tests

- `resolveAccessibleChildren()` with 10 children: < 50ms
- `familyMembersOfStudent()` with 5 family members: < 10ms
- Dashboard load with family member: < 200ms (same as primary parent)

### Test Data

- 3 sample children with different family circles
- 5 sample family members with different access levels (full, limited, view_only)
- 3 sample invitations (pending, accepted, expired)
- 2 sample primary guardian transfers
- Mock OTP gateway for invitation tests

### Test Environment

- Test database with schema migration applied
- Mock OTP gateway for invitation tests
- Mock `NotificationService` for notification tests
- Test JWT tokens for primary parent, family member (full/limited/view_only), admin roles

---

## 22. Acceptance Criteria

### Phase 1

- [ ] AC-1: Primary parent can invite a family member via phone number with relationship + access level
- [ ] AC-2: Invitee receives OTP on their phone
- [ ] AC-3: Invitee enters OTP → account created (or existing found) → `parent_child_links` row created with `status = 'pending'`
- [ ] AC-4: School admin sees the family link request in the approval queue and can approve/reject
- [ ] AC-5: After admin approval, family member's dashboard shows the child (via `resolveAccessibleChildren`)
- [ ] AC-6: Family member with `full` access sees all tabs and all data (academics, fees, health, leave, pulse, messages)
- [ ] AC-7: Family member with `limited` access sees Academics but NOT Fees or Health (403 on those endpoints)
- [ ] AC-8: Family member with `view_only` access sees only Announcements + Messages (403 on academics, fees, health, leave-create, pulse)
- [ ] AC-9: Dashboard for `limited`/`view_only` members does not show overdue fee alerts
- [ ] AC-10: Primary parent can remove a family member (link status → `rejected`)
- [ ] AC-11: Primary parent can change a family member's access level
- [ ] AC-12: Primary parent can transfer primary guardianship (uses `enforceSinglePrimaryGuardian`)
- [ ] AC-13: Family circle view shows all members with name, phone, relationship, access level, primary badge
- [ ] AC-14: Relationship type is tracked using existing `relation` column (father, grandmother, guardian, etc.)
- [ ] AC-15: Notifications for attendance/marks/homework reach ALL family members (not just primary)
- [ ] AC-16: Fee notifications reach ONLY the primary guardian
- [ ] AC-17: Expired OTP (after 10 minutes) is rejected
- [ ] AC-18: More than 3 failed OTP attempts invalidates the invitation
- [ ] AC-19: Duplicate pending invitation to the same phone for the same student is rejected (409)
- [ ] AC-20: Primary parent cannot remove themselves if they are the only linked parent

### Phase 2

- [ ] AC-21: Family member can RSVP for PTMs and events
- [ ] AC-22: Primary parent can delegate event attendance to a family member
- [ ] AC-23: Family member can accept/decline delegation
- [ ] AC-24: Each family member can configure their own notification preferences per category

---

## 23. Implementation Roadmap

### Phase 1: Foundation + Access Scoping (15 days)

| Step | Duration | Tasks |
|---|---|---|
| 1.1 | 1 day | DB migration `migration_052_family_circle.sql`: add `access_level` + `invited_by` to `parent_child_links`, create `family_invitations` table, backfill existing rows |
| 1.2 | 1 day | Exposed table definitions: `FamilyInvitationsTable`, new columns on `ParentChildLinksTable`, register in `DatabaseFactory.allTables` |
| 1.3 | 3 days | `ChildAccessResolver.kt`: `resolveAccessibleChildren()`, `resolveChildWithAccess()`, `resolveAccessibleSchoolIds()` — the core child resolution refactor |
| 1.4 | 2 days | `FamilyCircleService.kt`: invite, accept, remove, updateAccessLevel, getFamilyCircle, setPrimary |
| 1.5 | 2 days | Refactor `ParentDashboardRouting.kt` to use `resolveAccessibleChildren()` + filter alerts by access_level + return `accessLevel` in response |
| 1.6 | 2 days | Refactor `ParentAcademicsRouting.kt` (`requireOwnedChild` → `resolveChildWithAccess`), `HealthRouting.kt`, `PulseRouting.kt` |
| 1.7 | 1 day | Refactor `ParentFeesRouting.kt` (resolve via primary parent's fee records, 403 for non-`full`) |
| 1.8 | 1 day | Refactor `ParentRouting.kt` (announcements + notifications) to use `resolveAccessibleSchoolIds()` + filter fee notifications by access_level |
| 1.9 | 1 day | Refactor `NotifyRecipients.kt`: add `familyMembersOfStudent()`, update `parentsOfStudent`/`parentsOfClass`/`parentsForAudience` to include family members |
| 1.10 | 1 day | API endpoints: family circle management routes (`FamilyCircleRouting.kt`) + family-accept auth route |

### Phase 1: Client (5 days)

| Step | Duration | Tasks |
|---|---|---|
| 1.11 | 1 day | `ParentRepository` + `ParentApi`: add family circle methods + `acceptFamilyInvitation` |
| 1.12 | 1 day | `ParentHomeViewModel`: carry `accessLevel` in state; `ParentPortalV2`: filter tabs by `accessLevel` |
| 1.13 | 1 day | `FamilyCircleScreenV2.kt`: family circle management UI (list, invite, remove, change access, transfer primary) |
| 1.14 | 1 day | 403 handling on `ParentFeesScreenV2`, `ParentHealthScreenV2`, academics screens |
| 1.15 | 1 day | Invitation acceptance flow in the auth/onboarding screens (enter OTP after receiving invite) |

### Phase 1: Testing (2 days)

| Step | Duration | Tasks |
|---|---|---|
| 1.16 | 1 day | Server tests: `ChildAccessResolver` tests, `FamilyCircleService` tests, endpoint access-level enforcement tests |
| 1.17 | 1 day | Notification routing tests: verify family members receive attendance/marks notifications, verify only primary receives fee notifications |

**Phase 1 Total: 22 days**

### Phase 2: Delegation + RSVP (7 days)

| Step | Duration | Tasks |
|---|---|---|
| 2.1 | 1 day | DB migration `migration_053_event_delegations.sql`: create `event_delegations` table |
| 2.2 | 2 days | Delegation service + API endpoints |
| 2.3 | 2 days | Calendar/RSVP changes: allow any family member to RSVP |
| 2.4 | 2 days | Client: delegation UI in family circle screen + calendar RSVP for family members |

**Phase 2 Total: 7 days**

### Grand Total: 29 days

### Pre-Implementation Checklist

- [ ] Verify `ParentChildLinksTable` schema for `access_level` and `invited_by` columns
- [ ] Verify `StudentAggregationService.enforceSinglePrimaryGuardian()` API signature
- [ ] Verify OTP gateway supports programmatic OTP sending
- [ ] Verify `ParentLinkRouting.kt` admin approval queue handles `invitedBy` field
- [ ] Verify `NotifyRecipients` can be extended without breaking existing notifications
- [ ] Verify all 7 endpoint files use `ChildrenTable.parentId eq uid` pattern (for replacement)

---

## 24. File-Level Impact Analysis

### Server

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Modify | Add `accessLevel`, `invitedBy` to `ParentChildLinksTable`; add `FamilyInvitationsTable`, `EventDelegationsTable` |
| `server/.../db/DatabaseFactory.kt` | Modify | Register `FamilyInvitationsTable`, `EventDelegationsTable` in `allTables` |
| `server/.../feature/family/ChildAccessResolver.kt` | **New** | Core child resolution: `resolveAccessibleChildren()`, `resolveChildWithAccess()`, `resolveAccessibleSchoolIds()` |
| `server/.../feature/family/FamilyCircleService.kt` | **New** | Invite, accept, remove, updateAccessLevel, getFamilyCircle, setPrimary |
| `server/.../feature/family/FamilyCircleRouting.kt` | **New** | REST endpoints for family circle management |
| `server/.../feature/family/FamilyAcceptRouting.kt` | **New** | `POST /api/v1/auth/family-accept` endpoint |
| `server/.../feature/parent/ParentDashboardRouting.kt` | **Modify** | Use `resolveAccessibleChildren()` instead of `ChildrenTable.parentId eq uid`; filter alerts by access_level; add `accessLevel` to response |
| `server/.../feature/parent/ParentAcademicsRouting.kt` | **Modify** | Replace `requireOwnedChild()` with `resolveChildWithAccess()`; add access_level gates |
| `server/.../feature/parent/ParentFeesRouting.kt` | **Modify** | Resolve fee records via primary parent; return 403 for non-`full` access |
| `server/.../feature/parent/ParentLeaveRouting.kt` | **Modify** | Use `resolveAccessibleChildren()` for listing; `resolveChildWithAccess()` for creating; block `view_only` from creating |
| `server/.../feature/parent/ParentRouting.kt` | **Modify** | Announcements + notifications: use `resolveAccessibleSchoolIds()`; filter fee notifications by access_level |
| `server/.../feature/health/HealthRouting.kt` | **Modify** | Replace ownership gate with `resolveChildWithAccess(uid, childId, setOf("full"))` |
| `server/.../feature/pulse/PulseRouting.kt` | **Modify** | Replace `service.getLatestPulse(uid, childId)` validation with `resolveChildWithAccess()` |
| `server/.../feature/user/ParentMessagesRouting.kt` | **Modify** | `resolveParentSchoolId()` → `resolveAccessibleSchoolIds().first()` |
| `server/.../feature/notifications/NotifyRecipients.kt` | **Modify** | Add `familyMembersOfStudent()`; update `parentsOfStudent`/`parentsOfClass`/`parentsForAudience` to include family members |
| `server/.../feature/parent/ParentLinkRouting.kt` | **Modify** | Admin approval queue: show `invitedBy` for family-circle-originated requests; approval flow unchanged |
| `server/.../feature/school/StudentAggregationService.kt` | No change | `enforceSinglePrimaryGuardian()` is reused as-is by `setPrimary()` |
| `docs/db/migration_052_family_circle.sql` | **New** | DDL: ALTER `parent_child_links` + CREATE `family_invitations` |
| `docs/db/migration_053_event_delegations.sql` | **New** | DDL: CREATE `event_delegations` (Phase 2) |

### Shared (KMP)

| File | Change Type | Description |
|---|---|---|
| `shared/.../parent/domain/model/ParentFeatureModels.kt` | Modify | Add `accessLevel` to `ParentDashboardData`; add `FamilyMemberDto`, `InviteRequest`, `AcceptResult`, `FamilyCircleResponse` |
| `shared/.../parent/domain/repository/ParentRepository.kt` | Modify | Add family circle repository methods |
| `shared/.../parent/data/repository/ParentRepositoryImpl.kt` | Modify | Implement family circle methods |
| `shared/.../parent/data/remote/ParentApi.kt` | Modify | Add family circle HTTP calls |
| `shared/.../parent/presentation/ParentHomeViewModel.kt` | Modify | Carry `accessLevel` in state |

### Client (Compose)

| File | Change Type | Description |
|---|---|---|
| `composeApp/.../ui/v2/screens/parent/ParentPortalV2.kt` | Modify | Filter tabs by `accessLevel` |
| `composeApp/.../ui/v2/screens/parent/FamilyCircleScreenV2.kt` | **New** | Family circle management UI |
| `composeApp/.../ui/v2/screens/parent/ParentFeesScreenV2.kt` | Modify | 403 handling |
| `composeApp/.../ui/v2/screens/parent/ParentHealthScreenV2.kt` | Modify | 403 handling |
| `composeApp/.../ui/v2/screens/parent/ParentAcademicsScreenV2.kt` | Modify | 403 handling |
| `composeApp/.../ui/v2/screens/auth/FamilyAcceptScreen.kt` | **New** | OTP entry for family invitation acceptance |

---

## 25. Future Enhancements

| # | Enhancement | Priority | Effort | Notes |
|---|---|---|---|---|
| F-1 | Custom access levels | Low | L | Beyond full/limited/view_only — granular per-feature toggles |
| F-2 | Family member-to-family member messaging | Low | M | Chat within family circle |
| F-3 | Family circle analytics | Low | M | Engagement tracking per family member |
| F-4 | Cross-school family linking | Low | L | One family circle spanning multiple schools |
| F-5 | Family member self-registration | Low | M | Allow family members to request access without invitation |
| F-6 | Temporary access | Medium | S | Time-limited access for temporary guardians |
| F-7 | Family circle export | Low | S | Export family circle configuration for backup |
| F-8 | Access level audit log | Medium | S | Track what each family member viewed |
| F-9 | Family circle templates | Low | S | Pre-configured access level templates for common family structures |
| F-10 | Co-parent equal access | Medium | S | Both parents as co-primary with equal rights |

---

## Appendix A: Sequence Diagrams

### A.1 Primary Parent Sends Invitation

```
Primary Parent (app)          Server                        OTP Gateway
     │                          │                              │
     │  POST /family-circle/    │                              │
     │  ──────────────────────> │                              │
     │  {phone, name,           │                              │
     │   relationship,          │                              │
     │   access_level}          │                              │
     │                          │                              │
     │                  Verify caller is primary guardian      │
     │                  Check no pending invite for phone      │
     │                  Generate 6-digit OTP                   │
     │                  Store in family_invitations            │
     │                  (expires_at = now + 10 min)            │
     │                          │                              │
     │                          │  Send OTP via WhatsApp/SMS   │
     │                          │  ──────────────────────────> │
     │                          │                              │
     │  200 {invitationId}      │                              │
     │  <────────────────────── │                              │
     │                          │                              │
```

### A.2 Family Member Accepts

```
Family Member (app)           Server                        School Admin
     │                          │                              │
     │  POST /auth/family-accept│                              │
     │  ──────────────────────> │                              │
     │  {phone, otp}            │                              │
     │                          │                              │
     │                  Find invitation by (phone, otp)        │
     │                  Verify not expired, attempts < 3        │
     │                          │                              │
     │                  Find app_users by phone:               │
     │                    - exists → use that account          │
     │                    - not found → create new app_users   │
     │                      (role=parent, isPhoneVerified=true)│
     │                          │                              │
     │                  Create parent_child_links row:         │
     │                    status = 'pending'                   │
     │                    relation = from invitation            │
     │                    accessLevel = from invitation         │
     │                    invitedBy = primary parent's id       │
     │                    isPrimaryGuardian = false             │
     │                          │                              │
     │                  Mark invitation as accepted             │
     │                          │                              │
     │                          │  Notify school admin:         │
     │                          │  "New family link request     │
     │                          │   needs approval"             │
     │                          │  ───────────────────────────> │
     │                          │                              │
     │  200 {success,            │                              │
     │   linkStatus: "pending"} │                              │
     │  <────────────────────── │                              │
     │                          │                              │
     │                          │                  Admin reviews & approves
     │                          │                  (existing flow in
     │                          │                   ParentLinkRouting.kt)
     │                          │                              │
     │                          │  Link status → 'approved'    │
     │                          │  children row resolved       │
     │                          │  <────────────────────────── │
     │                          │                              │
     │  Dashboard now returns   │                              │
     │  the child (via          │                              │
     │  resolveAccessibleChildren)                              │
     │                          │                              │
```

### A.3 Family Member Accesses Child Data

```
Family Member (app)       Server (ChildAccessResolver)       ChildrenTable    ParentChildLinksTable
     │                          │                                │                    │
     │  GET /parent/dashboard   │                                │                    │
     │  ──────────────────────> │                                │                    │
     │                          │──resolveAccessibleChildren(uid)│                    │
     │                          │  Path 1: children.parent_id = uid                    │
     │                          │──query──────────────────────────→│                    │
     │                          │←──(empty for family member)───│                    │
     │                          │  Path 2: parent_child_links.parent_id = uid           │
     │                          │    AND status = 'approved'                            │
     │                          │──query──────────────────────────────────────────────→│
     │                          │←──links (school_id, student_code)───────────────────│
     │                          │  Join links with children via (school_id, student_code)│
     │                          │──query──────────────────────────→│                    │
     │                          │←──children rows───────────────│                    │
     │                          │  UNION + deduplicate by child_id                     │
     │                          │  Filter alerts by access_level                       │
     │  ←──DashboardResponse────│                                │                    │
     │  (with accessLevel field) │                                │                    │
     │                          │                                │                    │
```

### A.4 Primary Parent Removes Family Member

```
Primary Parent (app)       Server (FamilyCircleService)       ParentChildLinksTable
     │                          │                                │
     │  DELETE /members/{userId}│                                │
     │  ──────────────────────> │                                │
     │                          │──verify caller is primary guardian│
     │                          │──count approved links──────────→│
     │                          │←──count > 1 (safe to remove)──│
     │                          │──set status = 'rejected'──────→│
     │                          │←──updated─────────────────────│
     │                          │──invalidate family circle cache│
     │                          │──send notification to removed member│
     │  ←──200 "Member removed"─│                                │
     │                          │                                │
```

---

## Appendix B: Domain Model / ER Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                          app_users                                     │
│  id (PK)  phone  role  isPhoneVerified  ...                           │
└──────────────────────────┬───────────────────────────────────────────┘
                           │
           ┌───────────────┼───────────────┐
           │               │               │
           ▼               ▼               ▼
┌──────────────────┐ ┌──────────────────────┐  ┌──────────────────────┐
│   children        │ │ parent_child_links    │  │ family_invitations    │
│ id (PK)           │ │ id (PK)               │  │ id (PK)               │
│ parent_id (FK)    │ │ parent_id             │  │ school_id             │
│ school_id         │ │ school_id             │  │ student_code          │
│ student_code      │ │ student_code          │  │ invited_by            │
│ child_name        │ │ child_id              │  │ invitee_phone         │
│ ...               │ │ status                │  │ relationship          │
│                   │ │ relation              │  │ access_level          │
│                   │ │ isPrimaryGuardian     │  │ status                │
│                   │ │ access_level (NEW)    │  │ otp, otp_attempts     │
│                   │ │ invited_by (NEW)      │  │ expires_at            │
│                   │ │ requestedAt           │  │ accepted_at, accepted_by│
│                   │ │ actionedBy, actionedAt│  │ created_at            │
└──────────────────┘ └──────────────────────┘  └──────────────────────┘

┌──────────────────────┐
│ event_delegations     │ (Phase 2)
│ id (PK)               │
│ school_id             │
│ event_id              │
│ student_code          │
│ delegated_by          │
│ delegated_to          │
│ status                │
│ message               │
│ created_at            │
│ responded_at          │
└──────────────────────┘

Existing tables (read-only for family circle):
┌──────────────────┐  ┌──────────────────────┐  ┌──────────────────────┐
│ FeeRecordsTable   │  │ NotificationPrefs     │  │ CalendarEventsTable   │
└──────────────────┘  └──────────────────────┘  └──────────────────────┘
```

---

## Appendix C: Event Flow

### Domain Events

| Event | Emitter | Consumers | Payload | Side Effects |
|---|---|---|---|---|
| `FamilyMemberInvited` | `FamilyCircleService.inviteFamilyMember()` | OTP Gateway | `invitationId, schoolId, studentCode, inviteePhone, relationship, accessLevel` | OTP sent to invitee |
| `FamilyInvitationAccepted` | `FamilyCircleService.acceptInvitation()` | NotificationService (admin) | `invitationId, schoolId, studentCode, inviteeUserId, invitedBy` | Admin notified of pending link |
| `FamilyLinkApproved` | `ParentLinkRouting.kt` (existing) | NotificationService (family member) | `linkId, schoolId, studentCode, userId` | Family member notified of approval |
| `FamilyMemberRemoved` | `FamilyCircleService.removeFamilyMember()` | NotificationService (removed member) | `linkId, schoolId, studentCode, removedUserId, removedBy` | Removed member notified |
| `AccessLevelChanged` | `FamilyCircleService.updateAccessLevel()` | None (logged) | `linkId, schoolId, studentCode, userId, oldLevel, newLevel` | Logged; cache invalidated |
| `PrimaryGuardianTransferred` | `FamilyCircleService.setPrimary()` | `enforceSinglePrimaryGuardian()` | `schoolId, studentCode, oldPrimaryId, newPrimaryId` | Single primary enforced; cache invalidated |
| `EventDelegated` | `FamilyCircleService.delegateEvent()` (Phase 2) | NotificationService (delegatee) | `delegationId, eventId, studentCode, delegatedTo, delegatedBy` | Delegatee notified |

### Event Delivery Guarantees

- Events are emitted synchronously within the service method
- OTP sending is async (fire-and-forget with logging)
- Notification delivery is async (existing `NotificationService` pattern)
- Failed notifications are logged; not retried
- Cache invalidation is synchronous

### Why Admin Approval Is Still Required

The existing system requires school admin approval for any parent→child link (`ParentLinkRouting.kt:233-578`). This is a security measure — a parent cannot self-link to any roll number. Family circle invitations must follow the same rule: the school admin vets who gets access to a student's data. The invitation creates a `pending` link, not an `approved` one.

The admin approval queue (`ParentLinkRouting.kt` decision endpoint) already handles `parent_child_links` with `status = 'pending'`. The new family-member links will appear in the same queue with the `invitedBy` field indicating they came from a family circle invitation.

---

## Appendix D: Configuration

### Environment Variables

| Variable | Default | Description |
|---|---|---|
| `FAMILY_CIRCLE_ENABLED` | `true` | Enable/disable family circle feature |
| `FAMILY_INVITATION_OTP_EXPIRY_MINUTES` | `10` | OTP expiry in minutes |
| `FAMILY_INVITATION_MAX_ATTEMPTS` | `3` | Max OTP attempts before invalidation |
| `FAMILY_CIRCLE_CACHE_TTL` | `300` | Cache TTL in seconds (5 min) |
| `FAMILY_INVITATION_RATE_LIMIT` | `5` | Max invitations per primary parent per hour |

### Feature Flags

| Flag | Default | Description |
|---|---|---|
| `FAMILY_CIRCLE_ENABLED` | `true` | Enable/disable family circle feature |
| `FAMILY_DELEGATION_ENABLED` | `false` | Enable/disable event delegation (Phase 2) |
| `FAMILY_SHARED_RSVP_ENABLED` | `false` | Enable/disable shared RSVP (Phase 2) |

### School-Level Settings

N/A — family circle is parent-initiated. No school-level configuration beyond standard admin approval.

---

## Appendix E: Migration & Rollback

### Migration: `migration_052_family_circle.sql`

```sql
-- Migration 052: Family Circle
-- Adds access_level and invited_by to parent_child_links
-- Creates family_invitations table

BEGIN;

-- Add access_level to parent_child_links
ALTER TABLE parent_child_links ADD COLUMN IF NOT EXISTS access_level VARCHAR(16) NOT NULL DEFAULT 'full';

-- Add invited_by to parent_child_links
ALTER TABLE parent_child_links ADD COLUMN IF NOT EXISTS invited_by UUID;

-- Backfill existing approved links with full access
UPDATE parent_child_links SET access_level = 'full' WHERE status = 'approved' AND access_level IS NULL;

-- Create family_invitations table
CREATE TABLE IF NOT EXISTS family_invitations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    student_code    VARCHAR(64) NOT NULL,
    invited_by      UUID NOT NULL,
    invitee_phone   VARCHAR(32) NOT NULL,
    invitee_name    TEXT,
    relationship    VARCHAR(32) NOT NULL,
    access_level    VARCHAR(16) NOT NULL DEFAULT 'limited',
    status          VARCHAR(16) NOT NULL DEFAULT 'pending',
    otp             VARCHAR(8),
    otp_attempts    INTEGER NOT NULL DEFAULT 0,
    expires_at      TIMESTAMP NOT NULL,
    accepted_at     TIMESTAMP,
    accepted_by     UUID,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_family_invitations_pending
    ON family_invitations (school_id, student_code, invitee_phone)
    WHERE status = 'pending';

-- Add index for child resolution
CREATE INDEX IF NOT EXISTS idx_parent_child_links_parent_status
    ON parent_child_links (parent_id, status);

COMMIT;
```

### Migration: `migration_053_event_delegations.sql` (Phase 2)

```sql
-- Migration 053: Event Delegations (Family Circle Phase 2)

BEGIN;

CREATE TABLE IF NOT EXISTS event_delegations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    event_id        UUID NOT NULL,
    student_code    VARCHAR(64) NOT NULL,
    delegated_by    UUID NOT NULL,
    delegated_to    UUID NOT NULL,
    status          VARCHAR(16) NOT NULL DEFAULT 'pending',
    message         TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    responded_at    TIMESTAMP
);

COMMIT;
```

### Rollback: `migration_052_rollback.sql`

```sql
BEGIN;
DROP TABLE IF EXISTS family_invitations;
ALTER TABLE parent_child_links DROP COLUMN IF EXISTS access_level;
ALTER TABLE parent_child_links DROP COLUMN IF EXISTS invited_by;
COMMIT;
```

### Rollback: `migration_053_rollback.sql`

```sql
BEGIN;
DROP TABLE IF EXISTS event_delegations;
COMMIT;
```

### Migration Validation

- Verify `access_level` column added to `parent_child_links`
- Verify `invited_by` column added to `parent_child_links`
- Verify existing approved links have `access_level = 'full'`
- Verify `family_invitations` table created with correct columns
- Verify `ux_family_invitations_pending` unique index created
- Verify `idx_parent_child_links_parent_status` index created
- Run `SELECT count(*) FROM family_invitations` — should be 0 (new feature)

---

## Appendix F: Observability

### Structured Logging

| Log Level | Event | Context Fields |
|---|---|---|
| INFO | Family member invited | `invitationId, schoolId, studentCode, invitedBy, inviteePhone, relationship, accessLevel` |
| INFO | Invitation accepted | `invitationId, schoolId, studentCode, inviteeUserId, invitedBy` |
| INFO | Family link approved | `linkId, schoolId, studentCode, userId, adminId` |
| INFO | Family member removed | `linkId, schoolId, studentCode, removedUserId, removedBy` |
| INFO | Access level changed | `linkId, schoolId, studentCode, userId, oldLevel, newLevel` |
| INFO | Primary guardian transferred | `schoolId, studentCode, oldPrimaryId, newPrimaryId` |
| WARN | OTP expired | `invitationId, inviteePhone, expiredAt` |
| WARN | OTP max attempts exceeded | `invitationId, inviteePhone, attempts` |
| WARN | Duplicate invitation rejected | `schoolId, studentCode, inviteePhone` |
| WARN | Non-primary attempted management | `userId, schoolId, studentCode, action` |
| ERROR | Invitation acceptance failed | `inviteePhone, error` |
| ERROR | Child resolution failed | `userId, childId, error` |

### Metrics

| Metric | Type | Labels | Description |
|---|---|---|---|
| `family_invitations_sent_total` | Counter | `school_id` | Total invitations sent |
| `family_invitations_accepted_total` | Counter | `school_id` | Total invitations accepted |
| `family_invitations_expired_total` | Counter | `school_id` | Total invitations expired |
| `family_links_approved_total` | Counter | `school_id` | Total family links approved by admin |
| `family_links_rejected_total` | Counter | `school_id` | Total family links rejected by admin |
| `family_members_total` | Gauge | `school_id, access_level` | Total family members by access level |
| `family_circle_size` | Histogram | — | Average family circle size |
| `child_resolution_duration` | Histogram | — | `resolveAccessibleChildren()` latency |
| `access_denied_total` | Counter | `school_id, endpoint` | 403 responses by endpoint |

### Health Checks

| Check | Endpoint | Description |
|---|---|---|
| Family tables exist | `/health/family` | Verify `family_invitations` and `parent_child_links.access_level` are accessible |

### Alerts

| Alert | Condition | Severity | Notification |
|---|---|---|---|
| Invitation acceptance rate low | Accepted / sent < 30% over 7 days | Warning | Email to product team |
| Admin approval backlog | Pending family links > 50 | Warning | Push to school admin |
| OTP failure rate high | OTP expired / sent > 20% | Warning | Email to dev team |
| Access denied spike | `access_denied_total` > 100/hour | Warning | Email to dev team (possible misconfiguration) |

### Dashboards

| Dashboard | Panels | Audience |
|---|---|---|
| Family Circle Overview | Total circles, avg size, access level distribution | Product Team |
| Invitation Funnel | Sent → Accepted → Approved conversion rates | Product Team |
| Admin Approval Queue | Pending links, approval rate, avg approval time | School Admin |
| Access Enforcement | 403 rates by endpoint, access level distribution | Dev Team |

### Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Child resolution refactor breaks existing primary parent flow | Medium | High | `resolveAccessibleChildren()` includes the `ChildrenTable.parentId` path — primary parents see exactly what they saw before. Full regression test on primary parent dashboard. |
| Access level not enforced on some endpoint | Medium | High | Centralize via `resolveChildWithAccess()` — no endpoint should do ad-hoc resolution. Code review checklist: every `ChildrenTable.parentId eq uid` must be replaced. |
| Family member sees fee data through notification | Low | Medium | Notification routing: fee notifications only go to `isPrimaryGuardian = true`. Dashboard alerts: filtered server-side by access_level. |
| Admin approval queue gets confusing with family invitations | Low | Low | The `invitedBy` field on `parent_child_links` distinguishes family invitations from self-initiated link requests. The admin queue UI can show a "Family Circle invitation from {parent name}" label. |
| OTP gateway rate limits block family invitations | Low | Low | Family invitation OTP uses the same gateway as signup. Rate limits are per-phone-per-hour. If the invitee's phone is already verified, skip OTP. |
| Performance: `parent_child_links` join for every dashboard call | Low | Low | Add index on `(parent_id, status)` on `parent_child_links`. The query is a UNION of two indexed lookups — O(1) per parent. |
