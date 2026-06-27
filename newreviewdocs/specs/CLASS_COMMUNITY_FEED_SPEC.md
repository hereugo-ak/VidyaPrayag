# Class Community Feed — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** `MESSAGING_SYSTEM_SPEC.md`
> **Source:** `DIFFERENTIATING_FEATURES.md` §2.3

---

## 1. Feature Overview

A class-level community feed where parents of the same class can share posts, photos, and events (birthdays, achievements, carpools, lost-and-found). Moderated by class teacher/admin with post approval workflow.

### Goals

- Class-scoped feed: parents of same class see posts
- Post types: text, photo, event (birthday, carpool, lost-and-found, general)
- Teacher/admin moderation: posts require approval before visible
- Reactions (like, celebrate, helpful) — no comments to keep it simple
- Teacher can pin important posts
- Auto-generated posts: birthday reminders for classmates
- Privacy: only parents of same class + teacher can see feed

---

## 2. Current System Assessment

- `MESSAGING_SYSTEM_SPEC.md` — thread-based messaging, not a feed
- `AnnouncementsTable` — admin-to-parent, one-directional
- `ParentChildLinksTable` — can identify parents of same class via student enrollment
- `DIFFERENTIATING_FEATURES.md` §2.3: Class Community Feed, effort M, data readiness: "Enrollment data exists to identify class parents"

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Parent creates post (text, photo, or event type) for their child's class feed |
| FR-2 | Posts require teacher/admin approval before visible to other parents |
| FR-3 | Other parents can react (like, celebrate, helpful) — no comments |
| FR-4 | Teacher can pin posts (stay at top) |
| FR-5 | Teacher can reject/delete posts |
| FR-6 | Auto-post: birthday reminder when classmate's birthday is approaching |
| FR-7 | Feed scoped to class: only parents with children in that class + teacher |
| FR-8 | Photo posts: image uploaded to Supabase Storage |
| FR-9 | Feed pagination (20 posts per page) |

---

## 4. Database Design

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

CREATE TABLE community_feed_reactions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id         UUID NOT NULL REFERENCES community_feed_posts(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL,
    reaction_type   VARCHAR(16) NOT NULL,          -- like | celebrate | helpful
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(post_id, user_id)
);
```

---

## 5. Backend Architecture

### 5.1 CommunityFeedService

```kotlin
class CommunityFeedService {
    suspend fun createPost(parentId: UUID, classId: UUID, request: CreatePostRequest): PostDto
    suspend fun approvePost(postId: UUID, reviewerId: UUID)
    suspend fun rejectPost(postId: UUID, reviewerId: UUID, reason: String)
    suspend fun deletePost(postId: UUID, userId: UUID)
    suspend fun pinPost(postId: UUID)
    suspend fun reactToPost(postId: UUID, userId: UUID, reactionType: String)
    suspend fun removeReaction(postId: UUID, userId: UUID)
    suspend fun getFeed(classId: UUID, page: Int): PaginatedResult<PostDto>
    suspend fun autoGenerateBirthdayPosts(): Int  // daily job
}
```

### 5.2 Access Control

- Parent can only post to their child's class feed
- Parent can only see feeds for classes their children are enrolled in
- Teacher can see feeds for classes they teach
- Admin can see all class feeds

### 5.3 Birthday Auto-Post Job

Daily 8 AM IST:
1. Query `StudentsTable` for birthdays in next 3 days
2. For each, check if birthday post already exists for this year
3. If not, create auto-post: type=birthday, event_date=birthday, child_name=student name
4. Auto-approve birthday posts (no moderation needed)

---

## 6. API Contracts

```
# Parent
GET /api/v1/parent/community-feed/{classId}?page={n}
POST /api/v1/parent/community-feed/{classId}/posts  { post_type, content, photo_url, event_title, event_date }
POST /api/v1/parent/community-feed/posts/{id}/react  { reaction_type }
DELETE /api/v1/parent/community-feed/posts/{id}/react

# Teacher/Admin (moderation)
GET /api/v1/school/community-feed/pending?class_id={uuid}
POST /api/v1/school/community-feed/posts/{id}/approve
POST /api/v1/school/community-feed/posts/{id}/reject  { reason }
DELETE /api/v1/school/community-feed/posts/{id}
POST /api/v1/school/community-feed/posts/{id}/pin
```

---

## 7. Acceptance Criteria

- [ ] Parent creates post (text/photo/event) for class feed
- [ ] Posts require approval before visible
- [ ] Reactions (like, celebrate, helpful) work
- [ ] Teacher can approve/reject/pin/delete posts
- [ ] Birthday auto-posts generated daily
- [ ] Feed scoped to class (privacy enforced)
- [ ] Pagination works

---

## 8. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration, Exposed tables |
| 2 | 2 days | CommunityFeedService (CRUD, moderation, reactions) |
| 3 | 1 day | Birthday auto-post job |
| 4 | 1 day | Access control + API endpoints |
| 5 | 3 days | Client UI (feed list, post composer, moderation queue, reactions) |
| 6 | 1 day | Tests |

---

## 9. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | 2 feed tables |
| `server/.../feature/feed/CommunityFeedService.kt` | New | Core service |
| `server/.../feature/feed/CommunityFeedRouting.kt` | New | API endpoints |
| `docs/db/migration_069_community_feed.sql` | New | DDL |
| `composeApp/.../ui/v2/screens/parent/CommunityFeedScreen.kt` | New | Feed UI |
| `composeApp/.../ui/v2/screens/teacher/FeedModerationScreen.kt` | New | Moderation queue |
