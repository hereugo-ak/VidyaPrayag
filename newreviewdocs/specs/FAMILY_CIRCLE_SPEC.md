# Family Circle — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** None
> **Source:** `DIFFERENTIATING_FEATURES.md` §2.2

---

## 1. Feature Overview

### 1.1 What

Multi-parent support for a single child: both parents, grandparents, and guardians can be linked to a student with configurable access levels. Enables shared visibility into a child's school life with role-appropriate permissions.

### 1.2 Why — Product Rationale

Indian families are multi-generational and joint. A child's school life is often co-managed by father, mother, grandparents, and sometimes aunts/guardians. Every school ERP on the market — including PowerSchool, the global leader — assumes **one parent = one account**. There's no way for a grandparent to see attendance without also seeing fees, or for a parent to delegate PTM attendance to a grandparent.

This is a **differentiating feature** (Priority P1, Phase 2, effort M, "High" value per `DIFFERENTIATING_FEATURES.md`). It is one of the reasons schools would choose Vidya Prayag over competitors. The cultural insight creates deep lock-in: once a family configures their circle, switching ERPs means re-establishing all those relationships.

### 1.3 What Stands Out (Competitive Moat)

From `DIFFERENTIATING_FEATURES.md:159`:
> "Indian families are multi-generational. Every ERP assumes one parent = one account. This is a cultural insight that creates deep lock-in."

From `DIFFERENTIATING_FEATURES.md:163`:
> "No ERP supports multi-guardian with role-based visibility. PowerSchool has 'portal access' but not family delegation."

### 1.4 Four Core Capabilities

1. **Role-based visibility** — A grandparent sees attendance + events but not fees. A parent sees everything. Currently the system is all-or-nothing.
2. **Delegation** — "I can't attend the PTM, assigning grandparent to attend." No ERP supports this.
3. **Notification routing** — "Notify all family members for absences, only primary parent for fees." Currently `NotificationPreferencesTable` is per-user, not per-guardian-role.
4. **Shared calendar/RSVP** — Any family member can RSVP for PTMs and events, not just the primary parent.

### 1.5 Scope of This Spec

This spec covers **all four capabilities**. Capabilities 1 and 4 are fully specified for implementation. Capabilities 2 and 3 (delegation, notification routing) are specified with database design and API contracts but marked as **Phase 2** in the roadmap — they build on the foundation of capability 1.

---

## 2. Current System Assessment

### 2.1 What Already Exists (Do NOT Rebuild)

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

### 2.2 What's Missing (What This Spec Builds)

| Gap | Description |
|---|---|
| **Access level scoping** | No `access_level` column on `parent_child_links`. All linked parents see everything. |
| **Family invitation flow** | No `family_invitations` table. No way for a primary parent to invite a family member by phone. |
| **Child resolution for family members** | Every endpoint resolves children via `ChildrenTable.parentId eq uid`. Family members linked via `parent_child_links` won't have a `children` row — they'll get zero children and hit the unlinked-parent gate. |
| **Access level enforcement** | No middleware or guard checks `access_level` before returning child data. Every endpoint returns all data to any authenticated parent. |
| **Notification routing per role** | `NotifyRecipients` resolves parents via `ChildrenTable.parentId`. Family members linked via `parent_child_links` won't receive notifications. `NotificationPreferencesTable` is per-user, not per-guardian-role. |
| **Delegation** | No `event_delegations` table. No way to delegate PTM/event attendance. |
| **Client tab visibility** | `ParentPortalV2.kt:208-220` hardcodes 5 tabs. No access-level-based filtering. |
| **Client unlinked-parent gate** | `ParentPortalV2.kt:116` fires when `dashboard.children.isEmpty()`. Family members will hit this. |

### 2.3 What the Original Spec Got Wrong

The original spec (pre-revision) claimed "one parent per child (primary). No multi-parent or family member support." This was incorrect — `isPrimaryGuardian` and `relation` already exist on `ParentChildLinksTable`, and `StudentAggregationService.enforceSinglePrimaryGuardian()` already enforces the single-primary constraint. The original spec also proposed adding duplicate columns (`is_primary`, `relationship`) that overlap with existing `is_primary_guardian` and `relation`.

---

## 3. Functional Requirements

### 3.1 Core Requirements

| ID | Requirement | Phase |
|---|---|---|
| FR-1 | Primary parent can invite family members via phone number with OTP verification | 1 |
| FR-2 | Invited family member creates an account (or links existing) → linked to child via `parent_child_links` | 1 |
| FR-3 | Access levels: `full` (all data), `limited` (academics + attendance + homework), `view_only` (announcements + events) | 1 |
| FR-4 | Primary parent designation: only primary receives fee-related notifications and can approve changes | 1 |
| FR-5 | Family member can be removed by primary parent | 1 |
| FR-6 | Each family member sees child's data per their access level — enforced server-side | 1 |
| FR-7 | Family circle view: see all linked family members and their access levels | 1 |
| FR-8 | Relationship type tracked (father, mother, grandmother, guardian, etc.) — reuses existing `relation` column | 1 |
| FR-9 | Family members can RSVP for PTMs and events (not just primary parent) | 2 |
| FR-10 | Primary parent can delegate event attendance to a family member | 2 |
| FR-11 | Notification routing: absence/marks/homework → all family members; fees → primary only | 2 |
| FR-12 | Each family member can configure their own notification preferences per category | 2 |

### 3.2 Non-Functional Requirements

| ID | Requirement |
|---|---|
| NFR-1 | Access level enforcement is server-side — client tab hiding is UX, not security |
| NFR-2 | Family invitation OTP uses the same OTP gateway as signup (`setup_otp_gateway.sql`) |
| NFR-3 | Invitation acceptance does NOT bypass school admin approval — it creates a `pending` `parent_child_links` row that the admin must approve (same as the existing link-child flow) |
| NFR-4 | Removing a family member sets their `parent_child_links.status` to `rejected` (soft delete, audit trail preserved) |
| NFR-5 | The primary parent cannot remove themselves if they are the only linked parent |
| NFR-6 | All changes to `parent_child_links` (access level, primary designation, removal) are auditable |

---

## 4. Database Design

### 4.1 Modify Existing: `parent_child_links`

The table already has `relation` (varchar 32) and `isPrimaryGuardian` (bool). We only add two genuinely new columns:

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

### 4.2 New Table: `family_invitations`

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

### 4.3 New Table: `event_delegations` (Phase 2)

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

### 4.4 Exposed Table Definitions (Kotlin)

Add to `server/.../db/Tables.kt`:

```kotlin
// =====================================================================
// family_invitations  (Family Circle — invitation workflow)
// =====================================================================
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

// =====================================================================
// event_delegations  (Family Circle Phase 2 — PTM/event delegation)
// =====================================================================
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

Also add to `ParentChildLinksTable` in `Tables.kt`:

```kotlin
// Inside existing ParentChildLinksTable object:
val accessLevel = varchar("access_level", 16).default("full")  // full | limited | view_only
val invitedBy   = uuid("invited_by").nullable()
```

Register both new tables in `DatabaseFactory.allTables`.

---

## 5. The Core Problem: Child Resolution Architecture

### 5.1 Current Pattern (Broken for Family Members)

Every parent API endpoint resolves child data the same way:

```kotlin
ChildrenTable.selectAll()
    .where { (ChildrenTable.parentId eq uid) and (ChildrenTable.isActive eq true) }
```

This pattern is repeated in **7 endpoint files**:

| File | Line(s) | What It Does |
|---|---|---|
| `ParentDashboardRouting.kt` | 164-166 | Dashboard children list |
| `ParentAcademicsRouting.kt` | 176-180 | `requireOwnedChild()` — attendance, marks, syllabus, timetable |
| `ParentFeesRouting.kt` | 104-105 | Fee records (keyed to `FeeRecordsTable.parentId`) |
| `HealthRouting.kt` | 580-587 | Health records (ownership gate) |
| `ParentLeaveRouting.kt` | 123-128 | Leave requests (ownership gate) |
| `PulseRouting.kt` | 34-43 | Parent Pulse (validates via `ChildrenTable.parentId`) |
| `ParentMessagesRouting.kt` | 223-226 | Message recipient resolution (school from child) |

Additionally, `NotifyRecipients.kt` resolves parents via `ChildrenTable.parentId` in:
- `parentsOfStudent()` (line 28-34)
- `parentsOfClass()` (line 41-47)
- `parentsInSchool()` (line 50-54)
- `parentsForAudience()` (line 87-134)

And `ParentRouting.kt` (announcements + notifications) resolves schools via `ChildrenTable.parentId` at lines 225-230 and 291-296.

### 5.2 Why Family Members Get Nothing

A grandparent invited via Family Circle is linked through `parent_child_links` (approved, with an `access_level`), **not** through `children.parentId`. So:

1. **Dashboard returns zero children** → the unlinked-parent gate fires (`ParentPortalV2.kt:116`) → they see "Link a Child" screen instead of the dashboard
2. **Every `requireOwnedChild()` call returns 404** → academics, health, leave, pulse all fail
3. **Fees returns nothing** — `FeeRecordsTable.parentId eq uid` won't match (fee records are keyed to the primary parent)
4. **Announcements returns nothing** — school resolution via `ChildrenTable.parentId` finds no schools
5. **Notifications returns nothing** — same school resolution issue
6. **Messages recipient resolution fails** — `resolveParentSchoolId()` uses `ChildrenTable.parentId`

### 5.3 The Solution: `resolveAccessibleChild()` + `resolveAccessibleChildren()`

Replace the ownership pattern with an access pattern. Two new shared helpers:

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
     *
     * Path 1 (primary parent): children WHERE children.parent_id = uid
     *   — the primary parent owns the children row directly.
     *
     * Path 2 (family member): parent_child_links WHERE parent_id = uid AND status = 'approved'
     *   → resolve children via (school_id, student_code) join
     *   — family members do NOT have a children row; they access through the link.
     *
     * Both paths are UNIONed and deduplicated by child_id.
     */
    suspend fun resolveAccessibleChildren(uid: UUID): List<AccessibleChild>

    /**
     * Resolve a single child by ID, verifying the user has access.
     * Returns null (404) if the child doesn't exist or the user has no link.
     * Does NOT check access_level — the caller decides what to gate.
     */
    suspend fun resolveAccessibleChild(uid: UUID, childId: UUID): AccessibleChild?

    /**
     * Resolve a single child by ID, verifying the user has access AND
     * the access_level is in the allowed set. Returns 403 if access_level
     * is insufficient.
     */
    suspend fun resolveChildWithAccess(
        uid: UUID,
        childId: UUID,
        requiredLevels: Set<String>  // e.g. setOf("full") for fees
    ): AccessibleChild?

    /**
     * Resolve the school IDs for all children the user can access.
     * Replaces the pattern in ParentRouting.kt and NotificationsRouting.kt
     * that resolves schools via ChildrenTable.parentId.
     */
    suspend fun resolveAccessibleSchoolIds(uid: UUID): List<UUID>
}
```

### 5.4 How Each Endpoint Changes

| Endpoint | Current Resolution | New Resolution | Access Gate |
|---|---|---|---|
| `GET /parent/dashboard` | `ChildrenTable.parentId eq uid` | `resolveAccessibleChildren(uid)` | Filter alerts by access_level (hide fee alerts for non-`full`) |
| `GET /parent/child/{id}/attendance` | `requireOwnedChild()` | `resolveChildWithAccess(uid, childId, setOf("full", "limited"))` | 403 if `view_only` |
| `GET /parent/child/{id}/marks` | `requireOwnedChild()` | `resolveChildWithAccess(uid, childId, setOf("full", "limited"))` | 403 if `view_only` |
| `GET /parent/child/{id}/syllabus` | `requireOwnedChild()` | `resolveChildWithAccess(uid, childId, setOf("full", "limited"))` | 403 if `view_only` |
| `GET /parent/child/{id}/timetable` | `requireOwnedChild()` | `resolveChildWithAccess(uid, childId, setOf("full", "limited"))` | 403 if `view_only` |
| `GET /parent/fees` | `FeeRecordsTable.parentId eq uid` | Resolve primary parent's fee records via `parent_child_links`; return 403 if access_level != `full` | 403 if not `full` |
| `GET /parent/health/{childId}` | `ChildrenTable.parentId eq uid` | `resolveChildWithAccess(uid, childId, setOf("full"))` | 403 if not `full` |
| `GET /parent/leave` | `ChildrenTable.parentId eq uid` | `resolveAccessibleChildren(uid)` for listing; `resolveChildWithAccess(uid, childId, setOf("full", "limited"))` for creating | `view_only` can list but not create |
| `GET /parent/pulse/latest/{childId}` | `service.getLatestPulse(uid, childId)` validates via `ChildrenTable.parentId` | `resolveChildWithAccess(uid, childId, setOf("full", "limited"))` | 403 if `view_only` |
| `GET /parent/announcements` | Schools via `ChildrenTable.parentId` | `resolveAccessibleSchoolIds(uid)` | All access levels (announcements are `view_only`-safe) |
| `GET /parent/notifications` | Schools via `ChildrenTable.parentId` + fees via `FeeRecordsTable.parentId` | `resolveAccessibleSchoolIds(uid)` + fee notifications only if `full` access | Filter fee notifications by access_level |
| `GET /parent/messages/threads` | `MessageThreadsTable.ownerUserId eq uid` | No change (already per-user) | All access levels |
| `GET /parent/messages/recipients` | `resolveParentSchoolId()` via `ChildrenTable.parentId` | `resolveAccessibleSchoolIds(uid).first()` | All access levels |

### 5.5 Fees Special Case

Fee records are keyed to `FeeRecordsTable.parentId` — the primary parent who registered the child. Family members don't have fee records. The fees endpoint needs to:

1. Find the primary parent for the selected child via `parent_child_links` where `isPrimaryGuardian = true` and `status = 'approved'`
2. Query `FeeRecordsTable` with that primary parent's ID (filtered by `child_id` if provided)
3. Return 403 if the caller's `access_level` is not `full`

This means a grandparent (`limited` or `view_only`) gets a 403 on the fees endpoint, and the client hides the Fees tab.

---

## 6. Notification Routing Changes

### 6.1 Current State

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

### 6.2 Required Changes

Add a new resolver that queries `parent_child_links` in addition to `children`:

```kotlin
// NotifyRecipients.kt — new method

/**
 * ALL family members (app_users.id) linked to a student, including
 * those linked via parent_child_links (family circle) and the primary
 * parent linked via children.parent_id. Deduplicated.
 */
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

### 6.3 Routing Rules

| Event Category | Who Gets Notified | Resolver Change |
|---|---|---|
| `attendance` (absent/late) | All family members | `familyMembersOfStudent(schoolId, code)` |
| `marks` (results published) | All family members | `familyMembersOfStudent(schoolId, code)` |
| `homework` (new assignment) | All family members | `familyMembersOfStudent(schoolId, code)` via `parentsOfClass` equivalent |
| `announcement` (school post) | All family members | `familyMembersOfStudent` per audience scope |
| `fees` (due/overdue reminder) | Primary parent only | `familyMembersOfStudent(schoolId, code, setOf("full"))` — but only the `isPrimaryGuardian = true` one |
| `leave` (request status change) | All family members with `full` or `limited` | `familyMembersOfStudent(schoolId, code, setOf("full", "limited"))` |

### 6.4 Per-User Preferences (Already Supported)

`NotificationPreferencesTable` is already per-user per-category. Each family member can independently enable/disable notification categories. No change needed here — the routing change above ensures notifications are **dispatched** to the right people; preferences control whether they're **delivered**.

---

## 7. Backend Architecture

### 7.1 FamilyCircleService

```kotlin
// server/.../feature/family/FamilyCircleService.kt

class FamilyCircleService {

    /**
     * Primary parent invites a family member by phone.
     *
     * Flow:
     * 1. Verify parentId is the primary guardian for (schoolId, studentCode)
     * 2. Check no existing pending invitation for the same (schoolId, studentCode, phone)
     * 3. Generate 6-digit OTP, store in family_invitations with 10-min expiry
     * 4. Send OTP via WhatsApp/SMS using the existing OTP gateway
     *
     * Returns: invitation ID
     */
    suspend fun inviteFamilyMember(
        parentId: UUID,
        schoolId: UUID,
        studentCode: String,
        request: InviteRequest
    ): UUID

    /**
     * Invitee accepts the invitation by entering the OTP.
     *
     * Flow:
     * 1. Find the invitation by (invitee_phone, otp) where status = 'pending' and not expired
     * 2. Verify OTP (increment otp_attempts, max 3 attempts)
     * 3. Find or create the invitee's app_users account:
     *    - If phone matches an existing app_users row → use that account
     *    - If not → create a new app_users row with role='parent', isPhoneVerified=true
     * 4. Create a parent_child_links row with status='pending' (admin must still approve)
     *    - parentId = invitee's app_users.id
     *    - schoolId, studentCode from the invitation
     *    - relation from the invitation
     *    - accessLevel from the invitation
     *    - invitedBy = the primary parent who sent the invitation
     *    - isPrimaryGuardian = false (family members are never primary)
     * 5. Mark invitation as accepted (status='accepted', acceptedAt, acceptedBy)
     * 6. Notify the school admin that a new family link request needs approval
     *
     * Returns: AcceptResult (either LinkedPending or AlreadyLinked)
     */
    suspend fun acceptInvitation(phone: String, otp: String): AcceptResult

    /**
     * Primary parent removes a family member.
     * Sets parent_child_links.status = 'rejected' (soft delete).
     * Cannot remove self if only linked parent.
     */
    suspend fun removeFamilyMember(
        parentId: UUID,
        schoolId: UUID,
        studentCode: String,
        memberUserId: UUID
    )

    /**
     * Primary parent updates a family member's access level.
     */
    suspend fun updateAccessLevel(
        parentId: UUID,
        schoolId: UUID,
        studentCode: String,
        memberUserId: UUID,
        newLevel: String  // full | limited | view_only
    )

    /**
     * Returns all family members linked to a student.
     */
    suspend fun getFamilyCircle(
        parentId: UUID,
        schoolId: UUID,
        studentCode: String
    ): List<FamilyMemberDto>

    /**
     * Transfer primary guardian designation.
     * Uses existing StudentAggregationService.enforceSinglePrimaryGuardian()
     * to ensure only one primary remains.
     */
    suspend fun setPrimary(
        parentId: UUID,
        schoolId: UUID,
        studentCode: String,
        newPrimaryId: UUID
    )
}
```

### 7.2 DTOs

```kotlin
@Serializable
data class InviteRequest(
    val phone: String,          // invitee's phone number
    val name: String? = null,   // optional display name
    val relationship: String,   // father | mother | grandfather | grandmother | guardian | sibling | aunt | uncle
    val accessLevel: String,    // full | limited | view_only
)

@Serializable
data class FamilyMemberDto(
    val userId: String,
    val name: String,
    val phone: String?,
    val relationship: String,
    val accessLevel: String,
    val isPrimaryGuardian: Boolean,
    val profilePic: String? = null,
    val linkedAt: String,       // ISO timestamp
)

@Serializable
data class AcceptResult(
    val success: Boolean,
    val message: String,
    val linkStatus: String,     // pending (awaiting admin approval) | already_linked
)

@Serializable
data class FamilyCircleResponse(
    val members: List<FamilyMemberDto>,
)
```

### 7.3 Access Level Enforcement

There is no separate "middleware" — Ktor doesn't have a middleware pattern for per-route access checks. Instead, access enforcement is done via `ChildAccessResolver` (§5.3) called at the top of each endpoint handler.

**Access level → endpoint mapping:**

| Access Level | Allowed Endpoints | Blocked Endpoints |
|---|---|---|
| `full` | Everything (academics, attendance, marks, syllabus, timetable, fees, health, leave, pulse, messages, announcements, notifications) | Nothing |
| `limited` | Academics (attendance, marks, syllabus, timetable), leave (list + create), pulse, messages, announcements, notifications | Fees, Health |
| `view_only` | Announcements, notifications (announcements only, no fee notifications), messages, calendar/events | Academics, fees, health, leave (create), pulse |

**Dashboard filtering by access level:**

The dashboard endpoint (`GET /parent/dashboard`) returns different data based on access level:
- `full`: children list + all alerts (including overdue fees) + featured schools
- `limited`: children list + alerts minus fee alerts + featured schools
- `view_only`: children list + no alerts + featured schools

The dashboard response gains a new field:

```kotlin
@Serializable
data class DashboardResponse(
    // ... existing fields ...
    val accessLevel: String = "full",  // NEW — the caller's access level for the selected child
)
```

This tells the client which tabs to show.

---

## 8. API Contracts

### 8.1 Family Circle Management (Phase 1)

```
# Primary Parent — Family Circle Management

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

### 8.2 Invitation Acceptance (Phase 1)

```
# Invitee — Accept Invitation

POST   /api/v1/auth/family-accept
  Body: { phone, otp }
  → 200: { success: true, message: "Linked — awaiting school admin approval", linkStatus: "pending" }
  → 200: { success: true, message: "Already linked to this child", linkStatus: "already_linked" }
  → 400: Invalid or expired OTP
  → 404: No pending invitation found for this phone
```

**Note**: The `userId` comes from the JWT token if the invitee is already logged in, or a new account is created during acceptance. If the invitee has no account, the response includes a temporary token for onboarding completion.

### 8.3 Delegation (Phase 2)

```
# Primary Parent — Delegate Event Attendance

POST   /api/v1/parent/family-circle/{schoolId}/{studentCode}/delegate
  Body: { event_id, delegated_to_user_id, message? }
  → 200: { delegationId }

GET    /api/v1/parent/delegations
  → 200: { delegations: [DelegationDto] }

POST   /api/v1/parent/delegations/{id}/respond
  Body: { status: "accepted" | "declined" }
  → 200: { message: "Delegation responded" }
```

---

## 9. Client Architecture Changes

### 9.1 ParentPortalV2 — Tab Visibility

Current (`ParentPortalV2.kt:208-220`): 5 hardcoded tabs (Home, Academics, Fees, Conversations, Profile).

New: Tab list filtered by `accessLevel` from the dashboard response:

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

### 9.2 Unlinked-Parent Gate

Current (`ParentPortalV2.kt:116`): fires when `dashboard.children.isEmpty()`.

This gate must NOT fire for family members. The dashboard endpoint now returns children via `resolveAccessibleChildren()`, so family members will get a non-empty children list. The gate remains unchanged — it only fires for users with truly zero children (genuine unlinked parents).

### 9.3 Child Switcher

Current (`ParentPortalV2.kt:384-427`): dropdown lists `dashboard.children`.

No change needed — the dashboard now returns children from `resolveAccessibleChildren()`, which includes family-member-linked children. The dropdown will work for family members.

### 9.4 Home Screen — Alert Filtering

Current (`ParentHomeScreenV2.kt`): shows all alerts from the dashboard response, including overdue fee alerts.

The dashboard endpoint now filters alerts by access_level server-side. No client change needed — a `view_only` grandparent simply won't receive fee alerts in the response.

### 9.5 Fees Screen — 403 Handling

`ParentFeesScreenV2` needs to handle a 403 response gracefully:

```kotlin
when (result) {
    is NetworkResult.Error -> {
        if (result.code == 403) {
            // Access denied — shouldn't happen since tab is hidden,
            // but handle gracefully (deep link edge case)
            _state.update { it.copy(isLoading = false, error = "You don't have access to fee details") }
        } else {
            _state.update { it.copy(isLoading = false, error = result.message) }
        }
    }
}
```

### 9.6 Health Overlay — 403 Handling

`ParentOverlay.Health` (`ParentPortalV2.kt:191-199`) passes `child.id` from the dashboard. For `limited`/`view_only` members, the health endpoint returns 403. The `ParentHealthScreenV2` needs the same 403 handling as fees.

### 9.7 New Screen: FamilyCircleScreen

New screen accessible from the Profile tab:

```kotlin
// composeApp/.../ui/v2/screens/parent/FamilyCircleScreen.kt

@Composable
fun FamilyCircleScreenV2(
    schoolId: String,
    studentCode: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
)
```

Features:
- List all family members with name, phone, relationship, access level, primary badge
- Invite new member (phone, name, relationship, access level) → triggers OTP send
- Remove member (with confirmation dialog)
- Change access level (dropdown: full / limited / view_only)
- Transfer primary (with confirmation dialog — warns about fee notification transfer)

### 9.8 Shared Layer Changes

#### ParentDashboardViewModel

`ParentHomeViewModel` (`shared/.../parent/presentation/ParentHomeViewModel.kt`) calls `repository.getDashboard(token)`. The dashboard response now includes `accessLevel`. The state needs to carry this:

```kotlin
data class ParentHomeState(
    // ... existing fields ...
    val accessLevel: String = "full",  // NEW
)
```

#### ParentRepository

Add new methods:

```kotlin
suspend fun getFamilyCircle(token: String, schoolId: String, studentCode: String): NetworkResult<FamilyCircleResponse>
suspend fun inviteFamilyMember(token: String, schoolId: String, studentCode: String, request: InviteRequest): NetworkResult<InviteResponse>
suspend fun removeFamilyMember(token: String, schoolId: String, studentCode: String, userId: String): NetworkResult<Unit>
suspend fun updateAccessLevel(token: String, schoolId: String, studentCode: String, userId: String, level: String): NetworkResult<Unit>
suspend fun setPrimaryGuardian(token: String, schoolId: String, studentCode: String, userId: String): NetworkResult<Unit>
suspend fun acceptFamilyInvitation(phone: String, otp: String): NetworkResult<AcceptResult>
```

#### ParentApi

Add corresponding HTTP calls in `ParentApi.kt`.

---

## 10. Invitation Flow — Detailed Sequence

### 10.1 Primary Parent Sends Invitation

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

### 10.2 Family Member Accepts

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

### 10.3 Why Admin Approval Is Still Required

The existing system requires school admin approval for any parent→child link (`ParentLinkRouting.kt:233-578`). This is a security measure — a parent cannot self-link to any roll number. Family circle invitations must follow the same rule: the school admin vets who gets access to a student's data. The invitation creates a `pending` link, not an `approved` one.

The admin approval queue (`ParentLinkRouting.kt` decision endpoint) already handles `parent_child_links` with `status = 'pending'`. The new family-member links will appear in the same queue with the `invitedBy` field indicating they came from a family circle invitation.

---

## 11. Existing Code That Must Not Be Duplicated

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

## 12. Acceptance Criteria

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

## 13. Implementation Roadmap

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

---

## 14. File-Level Impact Analysis

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

## 15. Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Child resolution refactor breaks existing primary parent flow | Medium | High | `resolveAccessibleChildren()` includes the `ChildrenTable.parentId` path — primary parents see exactly what they saw before. Full regression test on primary parent dashboard. |
| Access level not enforced on some endpoint | Medium | High | Centralize via `resolveChildWithAccess()` — no endpoint should do ad-hoc resolution. Code review checklist: every `ChildrenTable.parentId eq uid` must be replaced. |
| Family member sees fee data through notification | Low | Medium | Notification routing: fee notifications only go to `isPrimaryGuardian = true`. Dashboard alerts: filtered server-side by access_level. |
| Admin approval queue gets confusing with family invitations | Low | Low | The `invitedBy` field on `parent_child_links` distinguishes family invitations from self-initiated link requests. The admin queue UI can show a "Family Circle invitation from {parent name}" label. |
| OTP gateway rate limits block family invitations | Low | Low | Family invitation OTP uses the same gateway as signup. Rate limits are per-phone-per-hour. If the invitee's phone is already verified, skip OTP. |
| Performance: `parent_child_links` join for every dashboard call | Low | Low | Add index on `(parent_id, status)` on `parent_child_links`. The query is a UNION of two indexed lookups — O(1) per parent. |
