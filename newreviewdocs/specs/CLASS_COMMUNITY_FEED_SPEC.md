# Class Community Feed — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** `MESSAGING_SYSTEM_SPEC.md`
> **Source:** `DIFFERENTIATING_FEATURES.md` §2.3
> **Template:** `_SPEC_TEMPLATE.md` v1 (25 mandatory + 6 optional sections)

---

## 1. Feature Overview

### What

A class-level community feed where parents of the same class can share posts, photos, and events (birthdays, achievements, carpools, lost-and-found). Moderated by class teacher/admin with post approval workflow.

### Why — Product Rationale

Indian school parents form close-knit communities around their children's classes. They coordinate carpools, celebrate birthdays, share lost-and-found items, and organize informal events. Currently, this happens in WhatsApp groups that are disconnected from the school system. A class community feed within the app creates a safe, moderated space for these interactions, keeping parents engaged within the school's platform rather than external messaging apps.

This is a **differentiating feature** (Priority P1, Phase 2, effort M, "High" value per `DIFFERENTIATING_FEATURES.md`). It increases parent engagement and app stickiness.

### What Stands Out (Competitive Moat)

From `DIFFERENTIATING_FEATURES.md` §2.3:
> "Class Community Feed — parents of same class can share posts, photos, events. Moderated by teacher. Data readiness: Enrollment data exists to identify class parents."

No major school ERP offers a moderated, class-scoped social feed. WhatsApp groups are unmoderated and disconnected from school data.

### Goals

- Class-scoped feed: parents of same class see posts
- Post types: text, photo, event (birthday, carpool, lost-and-found, general)
- Teacher/admin moderation: posts require approval before visible
- Reactions (like, celebrate, helpful) — no comments to keep it simple
- Teacher can pin important posts
- Auto-generated posts: birthday reminders for classmates
- Privacy: only parents of same class + teacher can see feed

### Non-goals

- [ ] Comments on posts (intentionally excluded to keep moderation simple)
- [ ] Direct messaging between parents (use existing messaging system)
- [ ] Cross-class feed (each class has its own feed)
- [ ] Video posts (photos only for initial version)
- [ ] Feed search/filtering (future enhancement)
- [ ] Parent-to-parent private posts

### Dependencies

- `MESSAGING_SYSTEM_SPEC.md` — prerequisite spec
- `AnnouncementsTable` — existing admin-to-parent announcements
- `ParentChildLinksTable` — identify parents of same class via student enrollment
- `StudentsTable` — student data for birthday auto-posts
- `ClassesTable` — class identification
- Supabase Storage — photo uploads

### Related Modules

- `server/.../feature/feed/` — new module for community feed
- `server/.../feature/parent/` — parent endpoints (feed access)
- `server/.../feature/teacher/` — teacher endpoints (moderation)
- `composeApp/.../ui/v2/screens/parent/` — parent feed UI
- `composeApp/.../ui/v2/screens/teacher/` — teacher moderation UI

---

## 2. Current System Assessment

### Existing Code

- `MESSAGING_SYSTEM_SPEC.md` — thread-based messaging, not a feed. No social/community features.
- `AnnouncementsTable` — admin-to-parent, one-directional. No parent-to-parent communication.
- `ParentChildLinksTable` — can identify parents of same class via student enrollment
- `StudentsTable` — student data including date of birth for birthday auto-posts
- `ClassesTable` — class identification and enrollment

### Existing Database

- `AnnouncementsTable` — one-directional announcements (not a feed)
- `ParentChildLinksTable` — parent→student links with class info
- `StudentsTable` — student records with DOB
- `ClassesTable` — class records
- `MessageThreadsTable` — thread-based messaging (separate from feed)

### Existing APIs

- `ParentRouting.kt` — parent announcements (one-directional)
- `ParentMessagesRouting.kt` — thread-based messaging (not a feed)
- No existing feed or social features

### Existing UI

- `ParentPortalV2.kt` — parent dashboard with tabs (no feed tab currently)
- `ParentHomeScreenV2.kt` — home screen (no feed)
- No existing feed or social UI

### Existing Services

- `NotificationService` — push notifications (reusable for feed notifications)
- `NotifyRecipients` — notification recipient resolution (reusable for class parents)

### Existing Documentation

- `DIFFERENTIATING_FEATURES.md` §2.3 — Class Community Feed
- `MESSAGING_SYSTEM_SPEC.md` — messaging system (prerequisite)

### Technical Debt

| # | Gap | Details |
|---|---|---|
| TD-1 | No community feed | No `community_feed_posts` or `community_feed_reactions` tables |
| TD-2 | No parent-to-parent communication | Only admin→parent announcements and thread-based messaging |
| TD-3 | No moderation workflow | No post approval/rejection system |
| TD-4 | No birthday auto-posts | No automated birthday reminder posts |

### Gaps

| # | Gap | Impact | Severity |
|---|---|---|---|
| G1 | No class community feed | Parents use external WhatsApp groups, disconnected from school | **High** |
| G2 | No parent-to-parent social interaction | Low engagement, no community building | **High** |
| G3 | No moderation workflow | No safe, moderated space for parent interactions | **Medium** |
| G4 | No birthday reminders | Missed community engagement opportunities | **Low** |

---

## 3. Functional Requirements

### FR-001
| Field | Value |
|---|---|
| **Title** | Create Post |
| **Description** | Parent creates post (text, photo, or event type) for their child's class feed. |
| **Priority** | Critical |
| **User Roles** | Parent |
| **Acceptance notes** | Post types: text, photo, event. Photo posts upload to Supabase Storage. Event posts have title and date. Posts start with `status = 'pending'`. |

### FR-002
| Field | Value |
|---|---|
| **Title** | Post Moderation |
| **Description** | Posts require teacher/admin approval before visible to other parents. |
| **Priority** | Critical |
| **User Roles** | Teacher, School Admin |
| **Acceptance notes** | Teacher can approve, reject (with reason), or delete posts. Rejected posts not visible to other parents. Author notified of approval/rejection. |

### FR-003
| Field | Value |
|---|---|
| **Title** | Reactions |
| **Description** | Other parents can react (like, celebrate, helpful) — no comments. |
| **Priority** | Medium |
| **User Roles** | Parent |
| **Acceptance notes** | One reaction per user per post. Reactions visible to all feed viewers. `UNIQUE(post_id, user_id)` constraint. |

### FR-004
| Field | Value |
|---|---|
| **Title** | Pin Posts |
| **Description** | Teacher can pin posts (stay at top). |
| **Priority** | Medium |
| **User Roles** | Teacher, School Admin |
| **Acceptance notes** | Pinned posts appear first in feed, sorted by `is_pinned DESC, created_at DESC`. |

### FR-005
| Field | Value |
|---|---|
| **Title** | Delete Posts |
| **Description** | Teacher can reject/delete posts. |
| **Priority** | High |
| **User Roles** | Teacher, School Admin |
| **Acceptance notes** | Deleted posts removed from feed. Reactions cascade-deleted. Author notified. |

### FR-006
| Field | Value |
|---|---|
| **Title** | Birthday Auto-Posts |
| **Description** | Auto-post: birthday reminder when classmate's birthday is approaching. |
| **Priority** | Medium |
| **User Roles** | System |
| **Acceptance notes** | Daily 8 AM IST job. Queries birthdays in next 3 days. Auto-approved (no moderation). Type=birthday, event_date=birthday, child_name=student name. |

### FR-007
| Field | Value |
|---|---|
| **Title** | Feed Privacy |
| **Description** | Feed scoped to class: only parents with children in that class + teacher. |
| **Priority** | Critical |
| **User Roles** | System |
| **Acceptance notes** | Server validates parent has child enrolled in class. Teacher must teach that class. Admin can see all feeds. |

### FR-008
| Field | Value |
|---|---|
| **Title** | Photo Upload |
| **Description** | Photo posts: image uploaded to Supabase Storage. |
| **Priority** | High |
| **User Roles** | Parent |
| **Acceptance notes** | Photo URL stored in `photo_url` column. Image served from Supabase Storage CDN. |

### FR-009
| Field | Value |
|---|---|
| **Title** | Feed Pagination |
| **Description** | Feed pagination (20 posts per page). |
| **Priority** | Medium |
| **User Roles** | System |
| **Acceptance notes** | Page-based pagination. 20 approved posts per page. Pinned posts included in page 1. |

### Non-Functional Requirements

| ID | Requirement |
|---|---|
| NFR-1 | Feed queries < 200ms for 20 posts |
| NFR-2 | Photo uploads limited to 5MB per image |
| NFR-3 | Birthday auto-post job completes in < 5 minutes for all schools |
| NFR-4 | Feed access validated server-side — client class ID is not trusted |
| NFR-5 | Posts soft-deleted (status='rejected') for audit trail, except admin hard-deletes |

---

## 4. User Stories

### Parent
- [ ] View my child's class community feed
- [ ] Create a text post for the class feed
- [ ] Create a photo post (upload image)
- [ ] Create an event post (birthday, carpool, lost-and-found)
- [ ] React to posts (like, celebrate, helpful)
- [ ] Remove my reaction from a post
- [ ] See birthday auto-posts for classmates
- [ ] Get notified when my post is approved/rejected

### Teacher
- [ ] View pending posts for my class feed
- [ ] Approve posts
- [ ] Reject posts with reason
- [ ] Delete inappropriate posts
- [ ] Pin important posts
- [ ] View all approved posts in my class feed

### School Admin
- [ ] View pending posts for all classes
- [ ] Approve/reject/delete posts for any class
- [ ] Pin posts for any class

### System
- [ ] Auto-generate birthday posts daily at 8 AM IST
- [ ] Auto-approve birthday posts
- [ ] Enforce feed privacy (class-scoped access only)
- [ ] Notify authors of approval/rejection

---

## 5. Business Rules

### BR-001
**Rule:** Posts require approval before visible to other parents.
**Enforcement:** `community_feed_posts.status` starts as `pending`. Only `approved` posts returned in feed. Author can see own pending posts.

### BR-002
**Rule:** Birthday auto-posts are auto-approved.
**Enforcement:** `autoGenerateBirthdayPosts()` creates posts with `status = 'approved'`. No moderation needed for system-generated birthday posts.

### BR-003
**Rule:** One reaction per user per post.
**Enforcement:** `UNIQUE(post_id, user_id)` constraint on `community_feed_reactions`. Changing reaction type updates existing row.

### BR-004
**Rule:** Feed is class-scoped.
**Enforcement:** Server validates parent has child enrolled in class via `ParentChildLinksTable` + `StudentsTable`. Teacher must teach class. Admin has full access.

### BR-005
**Rule:** Pinned posts appear first.
**Enforcement:** Feed query orders by `is_pinned DESC, created_at DESC`.

### BR-006
**Rule:** Only one birthday post per student per year.
**Enforcement:** `autoGenerateBirthdayPosts()` checks if birthday post already exists for `(class_id, child_name, event_date)` for the current year before creating.

### BR-007
**Rule:** Post author can see own pending posts.
**Enforcement:** Feed query includes `author_id = uid` for pending posts, in addition to `status = 'approved'`.

### BR-008
**Rule:** Reactions cascade-deleted with post.
**Enforcement:** `ON DELETE CASCADE` on `community_feed_reactions.post_id` FK.

---

## 6. Database Design

### 6.1 Entity Relationship Summary

Two tables: `community_feed_posts` (posts with moderation) and `community_feed_reactions` (user reactions). Posts reference `classes` and `app_users` implicitly.

### 6.2 New Tables

#### `community_feed_posts` table

```sql
CREATE TABLE community_feed_posts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    class_id        UUID NOT NULL,
    author_id       UUID NOT NULL,                 -- FK app_users.id (parent or teacher)
    author_name     TEXT NOT NULL,
    author_role     VARCHAR(32) NOT NULL,          -- parent | teacher | admin
    post_type       VARCHAR(16) NOT NULL,          -- text | photo | event | birthday
    content         TEXT,                          -- text content
    photo_url       TEXT,                          -- Supabase Storage URL for photo posts
    event_date      DATE,                          -- for event/birthday posts
    event_title     TEXT,                          -- "Aarav's Birthday", "Carpool to museum"
    child_name      TEXT,                          -- for birthday posts (whose birthday)
    status          VARCHAR(16) NOT NULL DEFAULT 'pending', -- pending | approved | rejected
    reviewed_by     UUID,
    reviewed_at     TIMESTAMP,
    rejection_reason TEXT,
    is_pinned       BOOLEAN NOT NULL DEFAULT false,
    reaction_count  INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_feed_posts_class ON community_feed_posts(class_id, status, created_at DESC);
```

#### `community_feed_reactions` table

```sql
CREATE TABLE community_feed_reactions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id         UUID NOT NULL REFERENCES community_feed_posts(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL,
    reaction_type   VARCHAR(16) NOT NULL,          -- like | celebrate | helpful
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(post_id, user_id)
);
```

### 6.3 Modified Tables

N/A — no existing tables modified.

### 6.4 Indexes

- `idx_feed_posts_class` — on `(class_id, status, created_at DESC)` for feed queries
- `UNIQUE(post_id, user_id)` on `community_feed_reactions` — prevents duplicate reactions

### 6.5 Constraints

- `community_feed_posts.school_id` — NOT NULL
- `community_feed_posts.class_id` — NOT NULL
- `community_feed_posts.author_id` — NOT NULL
- `community_feed_posts.author_name` — NOT NULL
- `community_feed_posts.author_role` — NOT NULL, VARCHAR(32)
- `community_feed_posts.post_type` — NOT NULL, VARCHAR(16)
- `community_feed_posts.status` — NOT NULL, default 'pending'
- `community_feed_posts.is_pinned` — NOT NULL, default false
- `community_feed_reactions.post_id` — NOT NULL, FK with CASCADE DELETE
- `community_feed_reactions.user_id` — NOT NULL
- `community_feed_reactions.reaction_type` — NOT NULL, VARCHAR(16)

### 6.6 Foreign Keys

- `community_feed_posts.school_id` → `schools.id` (implicit)
- `community_feed_posts.class_id` → `classes.id` (implicit)
- `community_feed_posts.author_id` → `app_users.id` (implicit)
- `community_feed_posts.reviewed_by` → `app_users.id` (implicit)
- `community_feed_reactions.post_id` → `community_feed_posts.id` (explicit, ON DELETE CASCADE)
- `community_feed_reactions.user_id` → `app_users.id` (implicit)

### 6.7 Soft Delete Strategy

- Post rejection: `status = 'rejected'` (soft delete, preserved for audit)
- Post deletion by admin/teacher: hard delete (`DELETE FROM`)
- Reactions: cascade-deleted with post

### 6.8 Audit Fields

- `community_feed_posts`: `reviewed_by`, `reviewed_at`, `rejection_reason`, `created_at`, `updated_at`
- `community_feed_reactions`: `created_at`

### 6.9 Migration Notes

Migration: `docs/db/migration_069_community_feed.sql`
- CREATE `community_feed_posts` table
- CREATE `community_feed_reactions` table
- CREATE indexes
- No data migration needed (new feature)

### 6.10 Exposed Mappings

```kotlin
object CommunityFeedPostsTable : UUIDTable("community_feed_posts", "id") {
    val schoolId        = uuid("school_id")
    val classId         = uuid("class_id")
    val authorId        = uuid("author_id")
    val authorName      = text("author_name")
    val authorRole      = varchar("author_role", 32)
    val postType        = varchar("post_type", 16)
    val content         = text("content").nullable()
    val photoUrl        = text("photo_url").nullable()
    val eventDate       = date("event_date").nullable()
    val eventTitle      = text("event_title").nullable()
    val childName       = text("child_name").nullable()
    val status          = varchar("status", 16).default("pending")
    val reviewedBy      = uuid("reviewed_by").nullable()
    val reviewedAt      = timestamp("reviewed_at").nullable()
    val rejectionReason = text("rejection_reason").nullable()
    val isPinned        = bool("is_pinned").default(false)
    val reactionCount   = integer("reaction_count").default(0)
    val createdAt       = timestamp("created_at")
    val updatedAt       = timestamp("updated_at")
}

object CommunityFeedReactionsTable : UUIDTable("community_feed_reactions", "id") {
    val postId          = uuid("post_id").references(CommunityFeedPostsTable.id, onDelete = ReferenceOption.CASCADE)
    val userId          = uuid("user_id")
    val reactionType    = varchar("reaction_type", 16)
    val createdAt       = timestamp("created_at")
}
```

Register both tables in `DatabaseFactory.allTables`.

### 6.11 Seed Data

N/A — feed data created by users and auto-post job.

---

## 7. State Machines

### Post Moderation State Machine

```
pending ──teacher_approves──> approved ──admin_deletes──> deleted
  │                              │
  │──teacher_rejects──> rejected │──admin_deletes──> deleted
  │                              │
  └──admin_deletes──> deleted    └──teacher_deletes──> deleted
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `pending` | Teacher/Admin approves | `approved` | Post becomes visible in feed |
| `pending` | Teacher/Admin rejects | `rejected` | Rejection reason stored; author notified |
| `pending` | Teacher/Admin deletes | `deleted` (hard delete) | Post removed entirely |
| `approved` | Teacher/Admin deletes | `deleted` (hard delete) | Post removed; reactions cascade-deleted |
| `approved` | Teacher/Admin pins | `approved` (is_pinned=true) | Post appears at top of feed |
| `approved` | Teacher/Admin unpins | `approved` (is_pinned=false) | Post returns to chronological order |
| `rejected` | Teacher/Admin deletes | `deleted` (hard delete) | Post removed entirely |

### Birthday Auto-Post State Machine

```
(non-existent) ──daily_job──> approved (auto-approved)
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| (non-existent) | Daily birthday job | `approved` | Birthday in next 3 days; no existing post for this year |

### Reaction State Machine

```
(no reaction) ──user_reacts──> reaction_exists
reaction_exists ──user_changes_reaction──> reaction_exists (updated type)
reaction_exists ──user_removes_reaction──> (no reaction)
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| (no reaction) | User reacts | `reaction_exists` | `UNIQUE(post_id, user_id)` — one per user |
| `reaction_exists` | User changes reaction type | `reaction_exists` (type updated) | Same row updated |
| `reaction_exists` | User removes reaction | (no reaction) | Row deleted |

---

## 8. Backend Architecture

### 8.1 Component Overview

`CommunityFeedService` handles post CRUD, moderation, reactions, and birthday auto-post generation. `CommunityFeedRouting` exposes parent and teacher/admin endpoints. Access control validates class membership.

### 8.2 Design Principles

1. **Moderation first** — all user-created posts require approval; only system-generated birthday posts are auto-approved
2. **Class-scoped privacy** — server validates parent has child in class before returning feed
3. **No comments** — reactions only, to keep moderation simple and reduce toxicity
4. **Pagination** — 20 posts per page, pinned posts first

### 8.3 Core Types

#### CommunityFeedService

```kotlin
class CommunityFeedService {
    suspend fun createPost(parentId: UUID, classId: UUID, request: CreatePostRequest): PostDto
    suspend fun approvePost(postId: UUID, reviewerId: UUID)
    suspend fun rejectPost(postId: UUID, reviewerId: UUID, reason: String)
    suspend fun deletePost(postId: UUID, userId: UUID)
    suspend fun pinPost(postId: UUID)
    suspend fun unpinPost(postId: UUID)
    suspend fun reactToPost(postId: UUID, userId: UUID, reactionType: String)
    suspend fun removeReaction(postId: UUID, userId: UUID)
    suspend fun getFeed(classId: UUID, page: Int, requesterId: UUID): PaginatedResult<PostDto>
    suspend fun getPendingPosts(classId: UUID?, schoolId: UUID): List<PostDto>
    suspend fun autoGenerateBirthdayPosts(): Int  // daily job
}
```

### 8.4 Repositories

- `CommunityFeedPostRepository` — CRUD for `community_feed_posts`
- `CommunityFeedReactionRepository` — CRUD for `community_feed_reactions`

### 8.5 Mappers

- `PostMapper` — maps `community_feed_posts` rows to `PostDto`
- `ReactionMapper` — maps `community_feed_reactions` rows to `ReactionDto`

### 8.6 Permission Checks

- Parent can only post to their child's class feed
- Parent can only see feeds for classes their children are enrolled in
- Teacher can see feeds for classes they teach
- Admin can see all class feeds
- Teacher/Admin can approve/reject/pin/delete posts

### 8.7 Background Jobs

- **Birthday Auto-Post Job** — daily 8 AM IST
  1. Query `StudentsTable` for birthdays in next 3 days
  2. For each, check if birthday post already exists for this year
  3. If not, create auto-post: type=birthday, event_date=birthday, child_name=student name
  4. Auto-approve birthday posts (no moderation needed)

### 8.8 Domain Events

- `PostCreated` — emitted on post creation; triggers notification to teacher for moderation
- `PostApproved` — emitted on approval; triggers notification to author
- `PostRejected` — emitted on rejection; triggers notification to author with reason
- `PostDeleted` — emitted on deletion; triggers notification to author
- `PostPinned` — emitted on pin; logged
- `ReactionAdded` — emitted on reaction; updates `reaction_count`
- `BirthdayPostGenerated` — emitted by daily job; logged

### 8.9 Caching

- Feed page 1: cached per class, 5-minute TTL, invalidated on new post/approval/pin
- Pending posts: not cached (real-time for moderation)
- Birthday posts: not cached (generated daily)

### 8.10 Transactions

- Post creation: single transaction (insert post)
- Post approval: single transaction (update status, set reviewed_by/at)
- Reaction: single transaction (insert/update reaction, update reaction_count)
- Birthday auto-post: batch transaction per school

### 8.11 Rate Limiting

- Post creation: rate-limited per parent per hour (e.g., 10 posts/hour)
- Reactions: rate-limited per user per minute (e.g., 30 reactions/minute)
- Standard API rate limiting for all endpoints

### 8.12 Configuration

- `COMMUNITY_FEED_ENABLED` — default `true`; enable/disable feature
- `COMMUNITY_FEED_PAGE_SIZE` — default `20`; posts per page
- `COMMUNITY_FEED_BIRTHDAY_LOOKAHEAD_DAYS` — default `3`; birthday auto-post lookahead
- `COMMUNITY_FEED_MAX_PHOTO_SIZE_MB` — default `5`; max photo upload size
- `COMMUNITY_FEED_POST_RATE_LIMIT` — default `10`; max posts per parent per hour

---

## 9. API Contracts

### 9.1 Parent Endpoints

All require `requireAuth()` + class membership validation.

```
GET /api/v1/parent/community-feed/{classId}?page={n}
  → 200: { posts: [PostDto], page: Int, totalPages: Int }
  → 403: Not parent of any child in this class

POST /api/v1/parent/community-feed/{classId}/posts
  Body: { post_type, content?, photo_url?, event_title?, event_date? }
  → 200: { post: PostDto }  (status: pending)
  → 403: Not parent of any child in this class

POST /api/v1/parent/community-feed/posts/{id}/react
  Body: { reaction_type }  // like | celebrate | helpful
  → 200: { message: "Reaction added" }

DELETE /api/v1/parent/community-feed/posts/{id}/react
  → 200: { message: "Reaction removed" }
```

### 9.2 Teacher/Admin Endpoints (Moderation)

All require `requireAuth()` + teacher/admin role.

```
GET /api/v1/school/community-feed/pending?class_id={uuid}
  → 200: { posts: [PostDto] }  (all pending posts, optionally filtered by class)

POST /api/v1/school/community-feed/posts/{id}/approve
  → 200: { message: "Post approved" }

POST /api/v1/school/community-feed/posts/{id}/reject
  Body: { reason }
  → 200: { message: "Post rejected" }

DELETE /api/v1/school/community-feed/posts/{id}
  → 200: { message: "Post deleted" }

POST /api/v1/school/community-feed/posts/{id}/pin
  → 200: { message: "Post pinned" }

DELETE /api/v1/school/community-feed/posts/{id}/pin
  → 200: { message: "Post unpinned" }
```

### 9.3 DTO Models

All `@Serializable`, wrapped in `ApiResponse<T>` pattern.

```kotlin
@Serializable data class CreatePostRequest(
    val postType: String,       // text | photo | event
    val content: String? = null,
    val photoUrl: String? = null,
    val eventTitle: String? = null,
    val eventDate: String? = null,  // ISO date
)

@Serializable data class PostDto(
    val id: String,
    val classId: String,
    val authorId: String,
    val authorName: String,
    val authorRole: String,
    val postType: String,
    val content: String?,
    val photoUrl: String?,
    val eventDate: String?,
    val eventTitle: String?,
    val childName: String?,
    val status: String,
    val isPinned: Boolean,
    val reactionCount: Int,
    val userReaction: String?,   // current user's reaction type, null if none
    val createdAt: String,
)

@Serializable data class PaginatedResult<T>(
    val items: List<T>,
    val page: Int,
    val totalPages: Int,
)
```

---

## 10. Frontend Architecture

### 10.1 Screens

| Screen | Platform | Role | Description |
|---|---|---|---|
| `CommunityFeedScreen` | Compose | Parent | Feed list with posts, reactions, post composer |
| `FeedModerationScreen` | Compose | Teacher/Admin | Moderation queue: pending posts, approve/reject/delete/pin |

### 10.2 Navigation

- Parent: Home tab or dedicated Community tab → `CommunityFeedScreen`
- Teacher: Sidebar/menu → `FeedModerationScreen`

### 10.3 UX Flows

#### Parent: Create Post
1. Parent opens class feed
2. Taps "Create Post"
3. Selects type: text, photo, or event
4. Enters content / uploads photo / enters event details
5. Post created with "Pending approval" status
6. Parent sees own pending post in feed with "Pending" badge

#### Parent: React to Post
1. Parent sees post in feed
2. Taps reaction button (like, celebrate, helpful)
3. Reaction count increments
4. Tapping again removes reaction

#### Teacher: Moderate Posts
1. Teacher opens moderation queue
2. Sees list of pending posts
3. Reviews content
4. Approves (post becomes visible) or rejects (with reason)
5. Can pin approved posts
6. Can delete any post

### 10.4 State Management

```kotlin
data class CommunityFeedState(
    val posts: List<PostDto>,
    val isLoading: Boolean,
    val page: Int,
    val totalPages: Int,
    val showCreateDialog: Boolean,
    val error: String?,
)

data class FeedModerationState(
    val pendingPosts: List<PostDto>,
    val isLoading: Boolean,
    val selectedClassId: String?,
    val error: String?,
)
```

### 10.5 Offline Support

- Feed page 1 cached locally for offline viewing
- Reactions queued and synced when online

### 10.6 Loading States

- Loading feed: "Loading community feed..."
- No posts: "No posts yet. Be the first to share something!"
- No pending posts: "No posts pending approval."

### 10.7 Error Handling (UI)

- 403 on feed access: "You don't have access to this class feed."
- Photo upload failure: "Failed to upload photo. Please try again."
- Post creation failure: "Failed to create post. Please try again."

### 10.8 Component Integration Guidelines

| Rule | Description |
|---|---|
| **R1** | Feed sorted by `is_pinned DESC, created_at DESC` |
| **R2** | Pending posts shown only to author (with "Pending" badge) |
| **R3** | Reactions displayed as emoji counters (👍 ❤️ 💯) |
| **R4** | Post composer supports text, photo, event types |
| **R5** | Photo posts show image thumbnail from Supabase Storage |
| **R6** | Birthday posts have special styling (cake icon, birthday theme) |
| **R7** | Moderation queue shows pending posts with approve/reject buttons |
| **R8** | Pinned posts have pin icon indicator |
| **R9** | Pagination with "Load More" button |
| **R10** | Feed access validated server-side — client class ID not trusted |

---

## 11. Shared Module Changes (KMP)

### 11.1 DTOs

All DTOs defined in section 9.3, placed in `shared/.../feed/domain/model/FeedModels.kt`.

### 11.2 Domain Models

```kotlin
data class CommunityPost(
    val id: String,
    val classId: String,
    val authorId: String,
    val authorName: String,
    val authorRole: String,
    val postType: PostType,
    val content: String?,
    val photoUrl: String?,
    val eventDate: LocalDate?,
    val eventTitle: String?,
    val childName: String?,
    val status: PostStatus,
    val isPinned: Boolean,
    val reactionCount: Int,
    val userReaction: ReactionType?,
    val createdAt: Instant,
)

enum class PostType { TEXT, PHOTO, EVENT, BIRTHDAY }
enum class PostStatus { PENDING, APPROVED, REJECTED }
enum class ReactionType { LIKE, CELEBRATE, HELPFUL }
```

### 11.3 Repository Interfaces

```kotlin
interface CommunityFeedRepository {
    suspend fun getFeed(token: String, classId: String, page: Int): NetworkResult<PaginatedResult<PostDto>>
    suspend fun createPost(token: String, classId: String, request: CreatePostRequest): NetworkResult<PostDto>
    suspend fun reactToPost(token: String, postId: String, reactionType: String): NetworkResult<Unit>
    suspend fun removeReaction(token: String, postId: String): NetworkResult<Unit>
    suspend fun getPendingPosts(token: String, classId: String?): NetworkResult<List<PostDto>>
    suspend fun approvePost(token: String, postId: String): NetworkResult<Unit>
    suspend fun rejectPost(token: String, postId: String, reason: String): NetworkResult<Unit>
    suspend fun deletePost(token: String, postId: String): NetworkResult<Unit>
    suspend fun pinPost(token: String, postId: String): NetworkResult<Unit>
    suspend fun unpinPost(token: String, postId: String): NetworkResult<Unit>
}
```

### 11.4 UseCases

- `GetFeedUseCase`
- `CreatePostUseCase`
- `ReactToPostUseCase`
- `RemoveReactionUseCase`
- `GetPendingPostsUseCase`
- `ApprovePostUseCase`
- `RejectPostUseCase`
- `DeletePostUseCase`
- `PinPostUseCase`

### 11.5 Validation

- Post type: one of text, photo, event
- Content: max 1000 characters (for text posts)
- Photo URL: valid URL, max 5MB upload
- Event title: max 200 characters
- Reaction type: one of like, celebrate, helpful
- Page: positive integer

### 11.6 Serialization

Standard Kotlinx serialization. Enums serialized as lowercase strings.

### 11.7 Network APIs

Ktor `@Resource` route definitions added to `CommunityFeedApi.kt`:
- Parent feed endpoints
- Teacher/Admin moderation endpoints

### 11.8 Database Models (Local Cache)

- Feed page 1 cached locally per class
- Pending posts not cached (real-time)

---

## 12. Permissions Matrix

| Action | Super Admin | School Admin | Teacher | Parent (of class) | Parent (other class) |
|---|---|---|---|---|---|
| View class feed | ✅ | ✅ | ✅ (own class) | ✅ | ❌ |
| Create post | N/A | N/A | ✅ (own class) | ✅ (own class) | ❌ |
| React to post | N/A | N/A | ✅ | ✅ | ❌ |
| Approve post | ✅ | ✅ | ✅ (own class) | ❌ | ❌ |
| Reject post | ✅ | ✅ | ✅ (own class) | ❌ | ❌ |
| Delete post | ✅ | ✅ | ✅ (own class) | ❌ (own post only) | ❌ |
| Pin post | ✅ | ✅ | ✅ (own class) | ❌ | ❌ |
| View pending queue | ✅ | ✅ | ✅ (own class) | ❌ | ❌ |

---

## 13. Notifications

### Feed Notification Triggers

| Type | Trigger | Recipient | Channel | Message |
|---|---|---|---|---|
| Post Pending Review | Parent creates post | Class Teacher | Push + Dashboard | "New post by {parent_name} pending review in {class_name} feed" |
| Post Approved | Teacher approves post | Post Author | Push | "Your post has been approved and is now visible in the class feed" |
| Post Rejected | Teacher rejects post | Post Author | Push | "Your post was not approved. Reason: {reason}" |
| Post Deleted | Teacher/admin deletes post | Post Author | Push | "Your post has been removed from the class feed" |
| New Post in Feed | Post approved | Class Parents | Push (optional) | "New post in {class_name} community feed" |
| Birthday Reminder | Auto-post generated | Class Parents | Push (optional) | "🎂 {child_name}'s birthday is on {date}!" |

### Notification Preferences

- Post pending review: always sent to teacher (no opt-out)
- Post approved/rejected/deleted: always sent to author (no opt-out)
- New post in feed: opt-out per user (via `NotificationPreferencesTable`)
- Birthday reminder: opt-out per user (via `NotificationPreferencesTable`)

---

## 14. Background Jobs

### Birthday Auto-Post Job

| Property | Value |
|---|---|
| **Name** | `BirthdayAutoPostJob` |
| **Schedule** | Daily 8 AM IST |
| **Duration** | < 5 minutes for all schools |
| **Retry** | 3 attempts with 5-minute intervals |

#### Job Flow

1. Query `StudentsTable` for birthdays in next 3 days (across all schools)
2. For each student:
   a. Determine class_id from enrollment
   b. Check if birthday post already exists for `(class_id, child_name, event_date)` this year
   c. If not, create auto-post: type=birthday, status=approved, event_date=birthday, child_name=student name
   d. Increment counter
3. Return total posts generated
4. Log summary: total generated, per school breakdown

#### Error Handling

- Student with no class enrollment: skipped, logged
- Duplicate birthday post (race condition): `UNIQUE` check prevents duplicate
- School inactive: skipped
- Job failure: logged, retried next day

---

## 15. Integrations

### Internal Integrations

| System | Integration Point | Direction | Protocol | Error Handling |
|---|---|---|---|---|
| `StudentsTable` | Birthday data for auto-posts | Read | Direct DB | Skip if no DOB |
| `ClassesTable` | Class identification | Read | Direct DB | Skip if class not found |
| `ParentChildLinksTable` | Class membership validation | Read | Direct DB | Return 403 if not member |
| `NotificationService` | Push notifications | Outbound | Direct call | Logged; non-blocking |
| `NotificationPreferencesTable` | User notification preferences | Read | Direct DB | Default to enabled |
| Supabase Storage | Photo uploads | Outbound | HTTP API | Return error on upload failure |
| `AppUsersTable` | Author info | Read | Direct DB | Skip if user not found |

### External Integrations

| System | Purpose | Direction | Protocol | Authentication | Error Handling |
|---|---|---|---|---|---|
| Supabase Storage | Photo storage and CDN | Outbound | HTTP API | Bearer token (existing) | Return error; log failure |

### Integration Patterns

- **Class membership:** Validated via `ParentChildLinksTable` + `StudentsTable` join
- **Photo upload:** Client uploads to Supabase Storage, passes URL to API
- **Notifications:** `NotificationService.sendPushNotification()` after post events
- **Birthday job:** Direct DB queries, batch processing per school

---

## 16. Security

### Authentication

- Parent endpoints: JWT auth via `requireAuth()`
- Teacher/Admin endpoints: `requireAuth()` + role check

### Authorization

- Parent can only access feeds for classes their children are enrolled in
- Teacher can only moderate feeds for classes they teach
- Admin can access all class feeds
- Server validates class membership on every request

### Data Protection

- Feed posts contain parent names — standard PII
- Photo URLs — public CDN URLs (Supabase Storage)
- Child names in birthday posts — standard PII
- No sensitive data beyond standard school management

### Input Validation

- Class ID: valid UUID, must be enrolled
- Post type: one of text, photo, event
- Content: max 1000 characters
- Photo URL: valid URL format
- Event title: max 200 characters
- Event date: valid date
- Reaction type: one of like, celebrate, helpful

### Rate Limiting

- Post creation: 10 posts per parent per hour
- Reactions: 30 per user per minute
- Standard API rate limiting for all endpoints

### Audit Logging

- Post creation: author ID, class ID, post type
- Post approval/rejection: reviewer ID, post ID, decision, reason
- Post deletion: deleter ID, post ID
- Post pin/unpin: actor ID, post ID
- Birthday auto-post: count per school, errors logged

### PII Handling

- Parent names in posts — standard PII, visible to class parents only
- Child names in birthday posts — standard PII, visible to class parents only
- Photo URLs — public CDN, but only accessible via feed (class-scoped)
- No additional PII beyond standard school management

### Multi-tenant Isolation

- `community_feed_posts.school_id` — NOT NULL, school-scoped
- All queries filtered by `school_id`
- Feed access validates parent→child→class→school chain
- No cross-school feed access

---

## 17. Performance & Scalability

### Expected Scale

- Small school: 5-20 classes, 20-50 parents per class, 5-10 posts per week per class
- Medium school: 20-50 classes, 30-60 parents per class, 10-20 posts per week per class
- Large school: 50-100 classes, 40-80 parents per class, 20-50 posts per week per class

### Query Optimization

- **Feed query:** `idx_feed_posts_class` on `(class_id, status, created_at DESC)`. 20 rows per page. Indexed.
- **Pending posts:** Same index, filtered by `status = 'pending'`. Small dataset.
- **Birthday job:** `StudentsTable` indexed on `date_of_birth`. Batch query per school.

### Indexing Strategy

- `idx_feed_posts_class` — `(class_id, status, created_at DESC)` for feed and pending queries
- `UNIQUE(post_id, user_id)` on reactions — prevents duplicates, serves as index

### Caching Strategy

- Feed page 1: cached per class, 5-minute TTL, invalidated on new post/approval/pin
- Pending posts: not cached (real-time for moderation)
- Birthday posts: not cached (generated daily)

### Pagination

- Page-based: 20 posts per page
- `LIMIT 20 OFFSET (page-1)*20`
- Total pages calculated from total approved post count

### Connection Pooling

Uses existing HikariCP connection pool. No additional pooling needed.

### Async Processing

- Birthday auto-post job: sequential per school
- Notification delivery: async (existing `NotificationService` pattern)
- Photo upload: client-side (not server-processed)

### Scalability Concerns

- Feed table growth: ~50 posts/week/class × 100 classes = 5,000 posts/week = 260,000 posts/year. Manageable with index.
- Reaction table growth: ~5 reactions/post × 260,000 posts = 1.3M reactions/year. Manageable with index.
- Birthday job: ~100 schools × 30 students/class × 50 classes = 150,000 queries. Spread over 5 minutes. Fine.

---

## 18. Edge Cases

| # | Scenario | Expected Behavior |
|---|---|---|
| EC-1 | Parent tries to access feed for class their child is not in | Return 403 "You don't have access to this class feed." |
| EC-2 | Parent creates post with both content and photo_url | Post type determines which is primary; both stored. |
| EC-3 | Parent creates event post without event_date | Return 400 "Event date is required for event posts." |
| EC-4 | Parent creates photo post without photo_url | Return 400 "Photo URL is required for photo posts." |
| EC-5 | Parent tries to react to own post | Allowed. Self-reaction counts. |
| EC-6 | Parent tries to react to pending post | Return 403 "Cannot react to posts that are not yet approved." |
| EC-7 | Teacher tries to moderate feed for class they don't teach | Return 403. |
| EC-8 | Birthday auto-post for student with no DOB | Skip. Logged. |
| EC-9 | Birthday auto-post for student with no class enrollment | Skip. Logged. |
| EC-10 | Duplicate birthday post (job re-run) | Check existing post for `(class_id, child_name, event_date)` this year. Skip if exists. |
| EC-11 | Parent deletes own post | Only via teacher/admin deletion. Parent cannot self-delete. |
| EC-12 | Post rejected twice (re-created after rejection) | Allowed. New post with `status = 'pending'`. |
| EC-13 | Feed page beyond total pages | Return empty list. `totalPages` indicates max. |
| EC-14 | Photo URL points to external site (not Supabase) | Accepted but logged. Consider validating domain in future. |
| EC-15 | Parent has multiple children in same class | Can access feed. No duplicate posts. |
| EC-16 | Class has no posts | Return empty feed. "No posts yet. Be the first to share something!" |
| EC-17 | Teacher pins already-pinned post | Idempotent. No error. `is_pinned` stays true. |
| EC-18 | Admin deletes post with reactions | Post hard-deleted. Reactions cascade-deleted via FK. |

---

## 19. Error Handling

### Error Response Format

Standard `ApiResponse` error format:
```json
{
  "success": false,
  "error": {
    "code": "FEED_ACCESS_DENIED",
    "message": "You don't have access to this class feed",
    "details": {}
  }
}
```

### Error Codes

| Code | HTTP Status | Description | User Message |
|---|---|---|---|
| `FEED_ACCESS_DENIED` | 403 | Not parent of any child in this class | "You don't have access to this class feed." |
| `POST_NOT_FOUND` | 404 | Post ID not found | "Post not found." |
| `INVALID_POST_TYPE` | 400 | Post type not in text/photo/event | "Invalid post type." |
| `MISSING_CONTENT` | 400 | Text post without content | "Content is required for text posts." |
| `MISSING_PHOTO_URL` | 400 | Photo post without photo_url | "Photo URL is required for photo posts." |
| `MISSING_EVENT_DATE` | 400 | Event post without event_date | "Event date is required for event posts." |
| `ALREADY_REACTED` | 409 | User already reacted (should update, not create) | "You already reacted to this post." |
| `NOT_MODERATOR` | 403 | Non-teacher/admin trying to moderate | "Only teachers and admins can moderate posts." |
| `PHOTO_UPLOAD_FAILED` | 500 | Supabase Storage upload failed | "Failed to upload photo. Please try again." |

### Error Handling Strategy

- **Validation errors:** Return 400 with field-specific message
- **Auth errors:** Return 401/403 with clear message
- **Not found:** Return 404
- **Conflicts:** Return 409
- **Server errors:** Return 500 with generic message; log full error

### Retry Strategy

- Client retries: 3 attempts with exponential backoff for 5xx errors
- No retry for 4xx errors (client errors)
- Birthday job: 3 retries with 5-minute intervals

### Fallback Behavior

- Feed query failure: Show "Failed to load feed. Pull to refresh."
- Photo upload failure: Show "Failed to upload photo. Please try again."
- Reaction failure: Show error, revert UI state

---

## 20. Analytics & Reporting

### Analytics Dashboard Data

| Metric | Source | Derivation |
|---|---|---|
| Total posts | `community_feed_posts` count | Direct count |
| Posts by type | `community_feed_posts.post_type` | Group by type, count |
| Posts by status | `community_feed_posts.status` | Group by status, count |
| Approval rate | approved / (approved + rejected) | Percentage |
| Total reactions | `community_feed_reactions` count | Direct count |
| Reactions by type | `community_feed_reactions.reaction_type` | Group by type, count |
| Birthday posts generated | `community_feed_posts` where `post_type = 'birthday'` | Direct count |
| Avg posts per class | total posts / distinct classes | Aggregate |
| Moderation turnaround | avg(reviewed_at - created_at) for approved/rejected | Duration |

### Export Capabilities

- Feed export (CSV) — class, author, type, content, status, reactions, created_at

### Report Types

| Report | Format | Frequency | Recipient |
|---|---|---|---|
| Feed activity summary | JSON (API) | On-demand | School Admin |
| Moderation summary | JSON (API) | On-demand | School Admin |
| Feed engagement | JSON (API) | On-demand | School Admin |

---

## 21. Testing Strategy

### Unit Tests

- `CommunityFeedService` — createPost, approvePost, rejectPost, deletePost, pinPost, reactToPost, removeReaction, getFeed, autoGenerateBirthdayPosts
- Access control: parent can only access own class feed, teacher can only moderate own class
- Birthday auto-post: generates posts for birthdays in next 3 days, skips duplicates, skips students without DOB
- Reaction: one per user per post, changing reaction updates type, removing reaction deletes row

### Integration Tests

- Full post lifecycle: create → pending → approve → visible in feed → react → pin
- Rejection flow: create → reject with reason → author notified
- Birthday job: create test students with upcoming birthdays → run job → verify auto-posts created and auto-approved
- Feed pagination: create 25 posts → verify 20 on page 1, 5 on page 2
- Multi-tenant: school A feed not accessible to school B parent

### E2E Tests

- Parent creates post → teacher approves → post appears in feed → other parent reacts
- Birthday auto-post appears in feed on correct date

### Performance Tests

- Feed query with 1000 posts: < 200ms for page 1
- Birthday job for 100 schools: < 5 minutes
- Reaction add: < 100ms

### Test Data

- 5 sample posts (text, photo, event, birthday, pending)
- 3 sample reactions (like, celebrate, helpful)
- 2 sample students with upcoming birthdays
- Test JWT tokens for parent, teacher, admin roles

### Test Environment

- Test database with schema migration applied
- Mock Supabase Storage for photo upload tests
- Mock `NotificationService` for notification tests
- Test JWT tokens for parent (of class), parent (other class), teacher, admin roles

---

## 22. Acceptance Criteria

- [ ] Parent creates post (text/photo/event) for class feed
- [ ] Posts require approval before visible to other parents
- [ ] Author can see own pending posts with "Pending" badge
- [ ] Reactions (like, celebrate, helpful) work — one per user per post
- [ ] Teacher can approve/reject/pin/delete posts
- [ ] Rejected posts include reason and author is notified
- [ ] Birthday auto-posts generated daily at 8 AM IST
- [ ] Birthday auto-posts are auto-approved (no moderation needed)
- [ ] Feed scoped to class (privacy enforced server-side)
- [ ] Parent without child in class gets 403
- [ ] Pagination works (20 posts per page)
- [ ] Pinned posts appear first in feed
- [ ] Photo posts display image from Supabase Storage
- [ ] Post creation rate-limited (10/hour)
- [ ] Admin can moderate any class feed

---

## 23. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration `migration_069_community_feed.sql`, Exposed tables, register in `DatabaseFactory` |
| 2 | 2 days | `CommunityFeedService.kt`: CRUD, moderation, reactions, access control |
| 3 | 1 day | Birthday auto-post daily job |
| 4 | 1 day | API endpoints: `CommunityFeedRouting.kt` (parent + teacher/admin) |
| 5 | 3 days | Client UI: `CommunityFeedScreen.kt` (feed list, post composer, reactions), `FeedModerationScreen.kt` (moderation queue) |
| 6 | 1 day | Tests: unit, integration, access control |

### Pre-Implementation Checklist

- [ ] Verify `StudentsTable` has `date_of_birth` column
- [ ] Verify `ClassesTable` schema for class identification
- [ ] Verify Supabase Storage bucket exists for feed photos
- [ ] Verify `NotificationService` supports class-parent recipient resolution
- [ ] Verify daily job scheduling infrastructure (existing cron/scheduler)

---

## 24. File-Level Impact Analysis

### Server

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | `CommunityFeedPostsTable`, `CommunityFeedReactionsTable` |
| `server/.../db/DatabaseFactory.kt` | Modify | Register feed tables in `allTables` |
| `server/.../feature/feed/CommunityFeedService.kt` | **New** | Core service: CRUD, moderation, reactions, birthday job |
| `server/.../feature/feed/CommunityFeedRouting.kt` | **New** | API endpoints (parent + teacher/admin) |
| `server/.../feature/feed/BirthdayAutoPostJob.kt` | **New** | Daily birthday auto-post job |
| `docs/db/migration_069_community_feed.sql` | **New** | DDL: 2 feed tables + indexes |

### Shared (KMP)

| File | Change Type | Description |
|---|---|---|
| `shared/.../feed/domain/model/FeedModels.kt` | **New** | DTOs, domain models, enums |
| `shared/.../feed/domain/repository/CommunityFeedRepository.kt` | **New** | Repository interface |
| `shared/.../feed/data/repository/CommunityFeedRepositoryImpl.kt` | **New** | Repository implementation |
| `shared/.../feed/data/remote/CommunityFeedApi.kt` | **New** | HTTP API definitions |
| `shared/.../feed/presentation/CommunityFeedViewModel.kt` | **New** | View model for feed state |

### Client (Compose)

| File | Change Type | Description |
|---|---|---|
| `composeApp/.../ui/v2/screens/parent/CommunityFeedScreen.kt` | **New** | Feed UI: list, post composer, reactions |
| `composeApp/.../ui/v2/screens/teacher/FeedModerationScreen.kt` | **New** | Moderation queue: pending posts, approve/reject/delete/pin |
| `composeApp/.../ui/v2/components/PostCard.kt` | **New** | Reusable post card component |

---

## 25. Future Enhancements

| # | Enhancement | Priority | Effort | Notes |
|---|---|---|---|---|
| F-1 | Comments on posts | Medium | M | Threaded comments with moderation |
| F-2 | Video posts | Medium | M | Short video uploads (30 seconds max) |
| F-3 | Feed search | Low | S | Search posts by content within class feed |
| F-4 | Feed filtering | Low | S | Filter by post type (text, photo, event, birthday) |
| F-5 | Cross-class feed | Low | M | School-wide feed for school-wide announcements |
| F-6 | Post scheduling | Low | S | Schedule posts for future date/time |
| F-7 | Rich text posts | Low | M | Markdown support for text posts |
| F-8 | Post analytics | Low | S | Views, unique reactors, engagement metrics |
| F-9 | Feed moderation AI | Medium | L | AI-powered content moderation for auto-approval |
| F-10 | Parent directory | Low | M | Opt-in parent directory with contact info |

---

## Appendix A: Sequence Diagrams

### A.1 Parent Creates Post → Teacher Approves

```
Parent (app)       Server (FeedService)       Teacher (app)       NotificationService
  │                    │                          │                      │
  │  POST /posts       │                          │                      │
  │  ────────────────> │                          │                      │
  │                    │──validate class membership│                      │
  │                    │──insert post (pending)──│                      │
  │  ←──PostDto────────│                          │                      │
  │                    │──notify teacher──────────────────────────────→│
  │                    │                          │                      │
  │                    │                  Teacher sees pending post      │
  │                    │  POST /approve   │      │                      │
  │                    │  <───────────────│      │                      │
  │                    │──update status='approved'│                     │
  │                    │──notify author───────────────────────────────→│
  │  ←──push "approved"───────────────────────────────────────────────│
  │                    │                          │                      │
```

### A.2 Birthday Auto-Post Job

```
BirthdayAutoPostJob    StudentsTable    CommunityFeedPostsTable    NotificationService
  │                        │                      │                        │
  │──query birthdays──────→│                      │                        │
  │←──students list────────│                      │                        │
  │  [for each student]    │                      │                        │
  │──check existing post──────────────────────────→│                        │
  │←──no existing─────────────────────────────────│                        │
  │──create post (approved)───────────────────────→│                        │
  │──notify class parents─────────────────────────────────────────────────→│
  │  [next student]        │                      │                        │
  │←──count────────────────│                      │                        │
  │                        │                      │                        │
```

---

## Appendix B: Domain Model / ER Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                      community_feed_posts                             │
│  id (PK)                                                              │
│  school_id, class_id                                                  │
│  author_id, author_name, author_role                                  │
│  post_type (text|photo|event|birthday)                                │
│  content, photo_url, event_date, event_title, child_name              │
│  status (pending|approved|rejected)                                   │
│  reviewed_by, reviewed_at, rejection_reason                           │
│  is_pinned, reaction_count                                            │
│  created_at, updated_at                                               │
│  INDEX: (class_id, status, created_at DESC)                           │
└──────────────────────────┬───────────────────────────────────────────┘
                           │
                           │ 1:N
                           ▼
┌──────────────────────────────────────────────────────────────────────┐
│                   community_feed_reactions                             │
│  id (PK)                                                              │
│  post_id (FK→posts, CASCADE DELETE)                                   │
│  user_id                                                              │
│  reaction_type (like|celebrate|helpful)                               │
│  created_at                                                           │
│  UNIQUE(post_id, user_id)                                             │
└──────────────────────────────────────────────────────────────────────┘

Source tables (read-only):
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────────┐
│ StudentsTable     │  │ ClassesTable      │  │ ParentChildLinksTable  │
│ (DOB for birthday)│  │ (class info)      │  │ (class membership)     │
└──────────────────┘  └──────────────────┘  └──────────────────────┘
```

---

## Appendix C: Event Flow

### Domain Events

| Event | Emitter | Consumers | Payload | Side Effects |
|---|---|---|---|---|
| `PostCreated` | `CommunityFeedService.createPost()` | NotificationService (teacher) | `postId, classId, authorId, postType` | Teacher notified for review |
| `PostApproved` | `CommunityFeedService.approvePost()` | NotificationService (author) | `postId, classId, reviewerId` | Author notified; post visible |
| `PostRejected` | `CommunityFeedService.rejectPost()` | NotificationService (author) | `postId, classId, reviewerId, reason` | Author notified with reason |
| `PostDeleted` | `CommunityFeedService.deletePost()` | NotificationService (author) | `postId, classId, deletedBy` | Author notified; reactions cascade-deleted |
| `PostPinned` | `CommunityFeedService.pinPost()` | None (logged) | `postId, classId, actorId` | Post appears at top |
| `ReactionAdded` | `CommunityFeedService.reactToPost()` | None (logged) | `postId, userId, reactionType` | `reaction_count` incremented |
| `BirthdayPostGenerated` | `BirthdayAutoPostJob` | NotificationService (class parents) | `postId, classId, childName, eventDate` | Class parents notified (optional) |

### Event Delivery Guarantees

- Events emitted synchronously within service methods
- Notification delivery is async (fire-and-forget with logging)
- Failed notifications logged; not retried
- Birthday job events: batch-processed, logged

---

## Appendix D: Configuration

### Environment Variables

| Variable | Default | Description |
|---|---|---|
| `COMMUNITY_FEED_ENABLED` | `true` | Enable/disable community feed feature |
| `COMMUNITY_FEED_PAGE_SIZE` | `20` | Posts per page |
| `COMMUNITY_FEED_BIRTHDAY_LOOKAHEAD_DAYS` | `3` | Birthday auto-post lookahead in days |
| `COMMUNITY_FEED_MAX_PHOTO_SIZE_MB` | `5` | Max photo upload size in MB |
| `COMMUNITY_FEED_POST_RATE_LIMIT` | `10` | Max posts per parent per hour |
| `COMMUNITY_FEED_REACTION_RATE_LIMIT` | `30` | Max reactions per user per minute |
| `COMMUNITY_FEED_CACHE_TTL` | `300` | Feed cache TTL in seconds (5 min) |
| `BIRTHDAY_JOB_CRON` | `0 0 8 * * *` | Daily 8 AM IST |

### Feature Flags

| Flag | Default | Description |
|---|---|---|
| `COMMUNITY_FEED_ENABLED` | `true` | Enable/disable community feed |
| `BIRTHDAY_AUTO_POST_ENABLED` | `true` | Enable/disable birthday auto-posts |
| `FEED_NOTIFICATIONS_ENABLED` | `true` | Enable/disable feed push notifications |

### School-Level Settings

N/A — feed is class-scoped. No school-level configuration beyond standard feature flags.

---

## Appendix E: Migration & Rollback

### Migration: `migration_069_community_feed.sql`

```sql
-- Migration 069: Class Community Feed
-- Creates 2 new tables

BEGIN;

CREATE TABLE IF NOT EXISTS community_feed_posts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    class_id        UUID NOT NULL,
    author_id       UUID NOT NULL,
    author_name     TEXT NOT NULL,
    author_role     VARCHAR(32) NOT NULL,
    post_type       VARCHAR(16) NOT NULL,
    content         TEXT,
    photo_url       TEXT,
    event_date      DATE,
    event_title     TEXT,
    child_name      TEXT,
    status          VARCHAR(16) NOT NULL DEFAULT 'pending',
    reviewed_by     UUID,
    reviewed_at     TIMESTAMP,
    rejection_reason TEXT,
    is_pinned       BOOLEAN NOT NULL DEFAULT false,
    reaction_count  INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_feed_posts_class ON community_feed_posts(class_id, status, created_at DESC);

CREATE TABLE IF NOT EXISTS community_feed_reactions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id         UUID NOT NULL REFERENCES community_feed_posts(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL,
    reaction_type   VARCHAR(16) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(post_id, user_id)
);

COMMIT;
```

### Rollback: `migration_069_rollback.sql`

```sql
BEGIN;
DROP TABLE IF EXISTS community_feed_reactions;
DROP TABLE IF EXISTS community_feed_posts;
COMMIT;
```

### Migration Validation

- Verify `community_feed_posts` table created with correct columns
- Verify `community_feed_reactions` table created with FK CASCADE DELETE
- Verify `idx_feed_posts_class` index created
- Verify `UNIQUE(post_id, user_id)` constraint on reactions
- Run `SELECT count(*) FROM community_feed_posts` — should be 0 (new feature)

---

## Appendix F: Observability

### Structured Logging

| Log Level | Event | Context Fields |
|---|---|---|
| INFO | Post created | `postId, classId, authorId, postType, status` |
| INFO | Post approved | `postId, classId, reviewerId` |
| INFO | Post rejected | `postId, classId, reviewerId, reason` |
| INFO | Post deleted | `postId, classId, deletedBy` |
| INFO | Post pinned/unpinned | `postId, classId, actorId, pinned` |
| INFO | Reaction added | `postId, userId, reactionType` |
| INFO | Birthday post generated | `postId, classId, childName, eventDate` |
| INFO | Birthday job completed | `totalGenerated, durationMs` |
| WARN | Feed access denied | `userId, classId, reason` |
| WARN | Birthday post skipped (no DOB) | `studentId, classId` |
| WARN | Birthday post skipped (no class) | `studentId` |
| WARN | Birthday post skipped (duplicate) | `studentId, classId, eventDate` |
| ERROR | Post creation failed | `authorId, classId, error` |
| ERROR | Birthday job failed | `error, stackTrace` |

### Metrics

| Metric | Type | Labels | Description |
|---|---|---|---|
| `feed_posts_total` | Counter | `school_id, post_type, status` | Total posts by type and status |
| `feed_reactions_total` | Counter | `school_id, reaction_type` | Total reactions by type |
| `feed_posts_pending` | Gauge | `school_id, class_id` | Pending posts per class |
| `feed_moderation_duration` | Histogram | — | Time from post creation to approval/rejection |
| `birthday_posts_generated` | Counter | `school_id` | Birthday auto-posts generated per school |
| `feed_query_duration` | Histogram | — | Feed query latency |
| `feed_access_denied` | Counter | `school_id` | Feed access denied count |

### Health Checks

| Check | Endpoint | Description |
|---|---|---|
| Feed tables exist | `/health/feed` | Verify `community_feed_posts` and `community_feed_reactions` are accessible |
| Birthday job last run | `/health/feed/birthday-job` | Verify birthday job ran within last 24 hours |

### Alerts

| Alert | Condition | Severity | Notification |
|---|---|---|---|
| Birthday job not running | No `birthday_posts_generated` metric in 24 hours | Warning | Email to dev team |
| Moderation backlog | `feed_posts_pending` > 50 per class | Warning | Push to school admin |
| Feed query slow | `feed_query_duration` > 500ms | Warning | Email to dev team |
| Post creation errors | `feed_posts_total` error rate > 5% | Warning | Email to dev team |

### Dashboards

| Dashboard | Panels | Audience |
|---|---|---|
| Feed Overview | Total posts, by type, by status, approval rate | School Admin |
| Moderation | Pending posts, avg turnaround, rejection rate | School Admin |
| Engagement | Reactions by type, posts per class, active classes | School Admin |
| Birthday Job | Posts generated, errors, last run time | Dev Team |
