# Parent Pulse — Technical Specification

> **Document status:** Implemented (85%) — AI narrative generation pending (uses heuristic summaries currently)
> **Last updated:** 2026-06-28
> **Prerequisites:** `AI_INFRASTRUCTURE_SPEC.md`
> **Source:** `DIFFERENTIATING_FEATURES.md` §2.1
> **Template:** `_SPEC_TEMPLATE.md` v1 (25 mandatory + 6 optional sections)

---

## 1. Feature Overview

AI-generated weekly summary card for parents that distills all school activity for their child into a single, glanceable "pulse" — attendance, marks, homework, announcements, messages, and AI insights. Delivered as a notification + in-app card every Sunday at 6 PM IST.

### Goals

- Weekly digest: attendance %, marks received, homework due/completed, announcements, messages unread
- AI-generated narrative summary ("This week, Aarav's attendance was 95%, scored 85% in Math test...")
- Trend indicators (↑↓ vs last week)
- Actionable items ("2 homework submissions pending")
- Delivered as push notification + in-app card
- Parent can view past pulses (history)

### Non-goals

- [ ] Real-time pulse (only weekly, not daily)
- [ ] Teacher pulse (parent-focused only)
- [ ] Pulse customization (content is auto-generated, not configurable)
- [ ] Pulse sharing (no social features)
- [ ] Pulse export (view in-app only)

### Dependencies

- `AI_INFRASTRUCTURE_SPEC.md` — AI narrative generation (currently uses heuristic fallback)
- `AttendanceTable` — attendance records for aggregation
- `AssessmentMarksTable` — marks for aggregation
- `HomeworkTable` — homework status for aggregation
- `AnnouncementsTable` — announcements for aggregation
- `MessagesTable` — unread messages count
- `CalendarEventsTable` — upcoming events
- `ParentChildLinksTable` — parent→student relationships
- `NotificationService` — push notification delivery
- `AiService` — AI narrative generation
- `AiJobsTable` — rate-limited AI job queue

### Related Modules

- `server/.../feature/pulse/` — pulse service, routing, weekly job
- `shared/.../feature/pulse/` — shared DTOs, ViewModel
- `composeApp/.../ui/v2/screens/parent/` — parent pulse UI
- `composeApp/.../ui/v2/components/` — reusable pulse card component

---

## 2. Current System Assessment

### Existing Code

- **Implemented** in commit `db1dd67` (2026-06-27) — DB migration `migration_051_parent_pulse.sql`, service, routing, weekly job, and client UI all shipped
- `ParentPulsesTable` (`Tables.kt`) — stores weekly pulse snapshots per parent-child pair
- `ParentPulseService.kt` (`server/.../feature/pulse/`) — aggregates attendance %, marks, homework, announcements, messages into weekly pulse
- `PulseRouting.kt` — API endpoints for retrieving current and past pulses
- `PulseWeeklyJob.kt` — scheduled job generates pulses every Sunday at 6 PM IST
- `ParentPulseScreen.kt` + `PulseCard.kt` (`composeApp/.../ui/v2/screens/parent/`) — UI with attendance ring, trend indicators, and actionable insights
- `ParentPulseViewModel.kt` (shared layer) — client-side state management
- `DevToolsRouting.kt` — admin dev-tools endpoint for manual pulse regeneration
- Tests: `ParentPulseServiceTest.kt` — 310 lines covering pulse aggregation logic
- **Not yet implemented:** AI-generated narrative summary (currently uses heuristic text generation; will swap to LLM once `AI_INFRASTRUCTURE_SPEC.md` is built)
- `ParentAchievementsTable` — badges, competencies, EI metrics (separate feature, not part of pulse)

### Existing Database

- `ParentPulsesTable` — weekly pulse snapshots (already created via `migration_051`)
- `AttendanceTable` — attendance records (source for aggregation)
- `AssessmentMarksTable` — marks (source for aggregation)
- `HomeworkTable` — homework status (source for aggregation)
- `AnnouncementsTable` — announcements (source for aggregation)
- `MessagesTable` — messages (source for unread count)
- `CalendarEventsTable` — events (source for upcoming events)
- `ParentChildLinksTable` — parent→student relationships (source for batch generation)
- `AiJobsTable` — AI job queue (for rate-limited AI calls)
- `NotificationPreferencesTable` — notification opt-out

### Existing APIs

- `GET /api/v1/parent/pulse/latest/{childId}` — latest pulse
- `GET /api/v1/parent/pulse/history/{childId}?weeks=12` — pulse history
- DevTools: manual pulse regeneration endpoint

### Existing UI

- `ParentPulseScreen.kt` — pulse card with attendance ring, trend indicators, actionable insights
- `PulseCard.kt` — reusable pulse card component
- `ParentPulseViewModel.kt` — client state management

### Existing Services

- `ParentPulseService` — pulse generation and aggregation
- `PulseWeeklyJob` — weekly scheduled job
- `NotificationService` — push notification delivery
- `AiService` — AI narrative (currently heuristic fallback)

### Existing Documentation

- `DIFFERENTIATING_FEATURES.md` §2.1 — Parent Pulse
- `ParentPulseServiceTest.kt` — test coverage documentation

### Technical Debt

| # | Gap | Details |
|---|---|---|
| TD-1 | AI narrative not implemented | Uses heuristic text generation instead of LLM |
| TD-2 | No opt-out UI | Backend supports opt-out but no UI toggle |
| TD-3 | No pulse analytics | No tracking of pulse open rates or engagement |

### Gaps

| # | Gap | Impact | Severity |
|---|---|---|---|
| G1 | AI narrative pending | Pulse text is generic, not personalized | **Medium** |
| G2 | No opt-out UI | Parents can't easily disable pulse notifications | **Low** |
| G3 | No engagement tracking | Can't measure pulse effectiveness | **Low** |

---

## 3. Functional Requirements

### FR-001
| Field | Value |
|---|---|
| **Title** | Weekly Pulse Generation |
| **Description** | Weekly job (Sunday 6 PM IST) generates pulse for each parent with active children. Aggregates attendance, marks, homework, announcements, messages, events. |
| **Priority** | Critical |
| **User Roles** | System |
| **Acceptance notes** | `PulseWeeklyJob` runs every Sunday 6 PM IST. Queries all active parent→student links. Generates pulse for each. Sends FCM notification. |

### FR-002
| Field | Value |
|---|---|
| **Title** | Data Aggregation |
| **Description** | Aggregate: attendance % (week), marks received (week), homework status, announcements count, unread messages, upcoming events. |
| **Priority** | Critical |
| **User Roles** | System |
| **Acceptance notes** | `ParentPulseService.generatePulse()` aggregates from `AttendanceTable`, `AssessmentMarksTable`, `HomeworkTable`, `AnnouncementsTable`, `MessagesTable`, `CalendarEventsTable`. |

### FR-003
| Field | Value |
|---|---|
| **Title** | AI Narrative Summary |
| **Description** | AI narrative summary via `AiService` (2-3 sentences, parent-friendly). Currently uses heuristic text generation; will swap to LLM. |
| **Priority** | High |
| **User Roles** | System |
| **Acceptance notes** | Prompt template sends student name, week range, attendance %, marks, homework, announcements, messages, upcoming events. AI returns 2-3 sentence summary. |

### FR-004
| Field | Value |
|---|---|
| **Title** | Trend Indicators |
| **Description** | Trend indicators comparing to previous week. Shows ↑↓ or stable for attendance, marks, homework. |
| **Priority** | High |
| **User Roles** | System |
| **Acceptance notes** | Fetches previous week's pulse from `ParentPulsesTable`. Compares attendance %, marks average, homework completion. Stores trend in `attendance_trend` field. |

### FR-005
| Field | Value |
|---|---|
| **Title** | Actionable Items |
| **Description** | Actionable items list (pending homework, unread messages, upcoming PTM). Displayed in pulse card. |
| **Priority** | Medium |
| **User Roles** | System |
| **Acceptance notes** | Built from aggregated data: pending homework titles, unread message count, upcoming events. Stored as JSON array in `actionable_items` field. |

### FR-006
| Field | Value |
|---|---|
| **Title** | Push Notification |
| **Description** | Push notification: "📚 {student_name}'s weekly pulse is ready". Sent via FCM after pulse generation. |
| **Priority** | High |
| **User Roles** | System |
| **Acceptance notes** | Uses `NotificationService` to send FCM push. Notification includes student name. Deep links to `ParentPulseScreen`. |

### FR-007
| Field | Value |
|---|---|
| **Title** | In-App Pulse Card |
| **Description** | In-app card with visual summary (progress bars, trend arrows, attendance ring). |
| **Priority** | High |
| **User Roles** | Parent |
| **Acceptance notes** | `PulseCard.kt` renders attendance ring, marks summary, homework progress, trend arrows, AI narrative, actionable items. |

### FR-008
| Field | Value |
|---|---|
| **Title** | Pulse History |
| **Description** | Pulse history (last 12 weeks). Parent can view past pulses. |
| **Priority** | Medium |
| **User Roles** | Parent |
| **Acceptance notes** | `GET /api/v1/parent/pulse/history/{childId}?weeks=12`. Returns list of past pulses sorted by week_start_date DESC. |

### FR-009
| Field | Value |
|---|---|
| **Title** | Opt Out |
| **Description** | Parent can opt out of pulse notifications. |
| **Priority** | Low |
| **User Roles** | Parent |
| **Acceptance notes** | Uses `NotificationPreferencesTable` with pulse-specific preference. Backend checks before sending notification. No UI toggle yet (TD-2). |

---

## 4. User Stories

### Parent
- [ ] Receive weekly pulse notification every Sunday evening
- [ ] View current week's pulse with attendance, marks, homework, announcements
- [ ] See AI narrative summary of child's week
- [ ] View trend indicators (better/worse than last week)
- [ ] See actionable items (pending homework, unread messages, upcoming events)
- [ ] View pulse history (last 12 weeks)
- [ ] Opt out of pulse notifications

### System
- [ ] Generate pulses for all active parent→student pairs every Sunday 6 PM IST
- [ ] Aggregate data from attendance, marks, homework, announcements, messages, events
- [ ] Generate AI narrative (or heuristic fallback)
- [ ] Compare to previous week for trends
- [ ] Send push notifications
- [ ] Store pulse snapshots for history

### Admin (DevTools)
- [ ] Manually regenerate pulse for a specific parent→student pair
- [ ] View pulse generation logs

---

## 5. Business Rules

### BR-001
**Rule:** Pulse generated once per week per parent→student pair.
**Enforcement:** `UNIQUE(parent_id, student_id, week_start_date)` constraint on `parent_pulses` table.

### BR-002
**Rule:** Pulse generated only for active parent→student links.
**Enforcement:** `PulseWeeklyJob` queries `ParentChildLinksTable` where link is active and student is enrolled.

### BR-003
**Rule:** Pulse notification sent only if parent hasn't opted out.
**Enforcement:** Check `NotificationPreferencesTable` for pulse-specific preference before sending FCM.

### BR-004
**Rule:** AI narrative is 2-3 sentences, warm and encouraging.
**Enforcement:** AI prompt enforces tone and length. Heuristic fallback generates similar text.

### BR-005
**Rule:** Trend comparison uses previous week's pulse.
**Enforcement:** Fetch previous week's pulse from `ParentPulsesTable`. If no previous pulse, trend is "stable" or null.

### BR-006
**Rule:** Pulse history limited to 12 weeks.
**Enforcement:** API endpoint accepts `weeks` parameter (default 12, max 52).

### BR-007
**Rule:** Pulse data is read-only after generation.
**Enforcement:** No update endpoint. Pulse is a snapshot. Regeneration (via DevTools) creates new record if week_start_date differs, or overwrites if same.

---

## 6. Database Design

### 6.1 Entity Relationship Summary

One table: `parent_pulses` — stores weekly pulse snapshots per parent-child pair with aggregated data, AI narrative, and actionable items.

### 6.2 New Tables

#### `parent_pulses` table

```sql
CREATE TABLE parent_pulses (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL REFERENCES schools(id),
    parent_id       UUID NOT NULL,
    student_id      UUID NOT NULL,
    student_name    TEXT NOT NULL,
    week_start_date DATE NOT NULL,
    week_end_date   DATE NOT NULL,
    attendance_percentage REAL,
    attendance_trend VARCHAR(8),                   -- up | down | stable
    marks_summary   TEXT,                          -- JSON: [{"subject": "Math", "test": "Unit Test", "marks": 85, "max": 100}]
    homework_pending INTEGER NOT NULL DEFAULT 0,
    homework_completed INTEGER NOT NULL DEFAULT 0,
    announcements_count INTEGER NOT NULL DEFAULT 0,
    unread_messages INTEGER NOT NULL DEFAULT 0,
    upcoming_events TEXT,                          -- JSON: [{"title": "PTM", "date": "2026-07-01"}]
    ai_narrative    TEXT NOT NULL,                 -- AI-generated summary
    actionable_items TEXT,                         -- JSON: ["Submit Math homework", "Read PTM invitation"]
    model_used      VARCHAR(64),
    tokens_used     INTEGER,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(parent_id, student_id, week_start_date)
);
CREATE INDEX idx_parent_pulses_parent ON parent_pulses(parent_id, week_start_date DESC);
```

### 6.3 Modified Tables

None. All source tables are read-only for pulse aggregation.

### 6.4 Indexes

- `idx_parent_pulses_parent` — listing by parent + week (DESC for latest first)
- `UNIQUE(parent_id, student_id, week_start_date)` — prevents duplicate pulses

### 6.5 Constraints

- `parent_pulses.school_id` — NOT NULL, FK to schools
- `parent_pulses.parent_id` — NOT NULL
- `parent_pulses.student_id` — NOT NULL
- `parent_pulses.student_name` — NOT NULL, TEXT
- `parent_pulses.week_start_date` — NOT NULL, DATE
- `parent_pulses.week_end_date` — NOT NULL, DATE
- `parent_pulses.ai_narrative` — NOT NULL, TEXT
- `parent_pulses.homework_pending` — NOT NULL, default 0
- `parent_pulses.homework_completed` — NOT NULL, default 0
- `parent_pulses.announcements_count` — NOT NULL, default 0
- `parent_pulses.unread_messages` — NOT NULL, default 0
- `UNIQUE(parent_id, student_id, week_start_date)` — one pulse per week per pair

### 6.6 Foreign Keys

- `parent_pulses.school_id` → `schools.id`

### 6.7 Soft Delete Strategy

N/A — pulses are never deleted. Historical snapshots retained indefinitely (or archived after 1 year).

### 6.8 Audit Fields

- `created_at` — pulse generation timestamp
- `model_used` — AI model used (if AI narrative)
- `tokens_used` — AI token count (if AI narrative)

### 6.9 Migration Notes

Migration: `docs/db/migration_051_parent_pulse.sql` (already applied)
- Creates 1 table with FK constraint and index
- No data backfill (new feature)

### 6.10 Exposed Mappings

`ParentPulsesTable` already registered in `Tables.kt` and `DatabaseFactory.kt`.

### 6.11 Seed Data

N/A — pulse data generated by weekly job.

---

## 7. State Machines

### Pulse Generation State Machine

Pulse generation is a batch process, not a stateful entity. The flow is:

```
NOT_GENERATED ──weekly_job_runs──> GENERATING ──aggregation_complete──> NARRATIVE_PENDING ──ai/heuristic_complete──> GENERATED ──notification_sent──> DELIVERED
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `not_generated` | Weekly job runs (Sunday 6 PM) | `generating` | Parent→student link is active |
| `generating` | Data aggregation complete | `narrative_pending` | All source data fetched |
| `narrative_pending` | AI/heuristic narrative complete | `generated` | Narrative text produced |
| `generated` | Push notification sent | `delivered` | FCM notification sent successfully |
| `generating` | Error during aggregation | `failed` | Logged; retried in next batch |
| `narrative_pending` | AI service unavailable | `generated` | Heuristic fallback used |

### Notification Opt-Out State Machine

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `notifications_enabled` | Parent opts out | `notifications_disabled` | Update `NotificationPreferencesTable` |
| `notifications_disabled` | Parent opts in | `notifications_enabled` | Update `NotificationPreferencesTable` |

---

## 8. Backend Architecture

### 8.1 Component Overview

`ParentPulseService` handles data aggregation, AI narrative generation, pulse storage, and notification. `PulseWeeklyJob` runs every Sunday 6 PM IST. `PulseRouting` exposes parent endpoints.

### 8.2 Design Principles

1. **Snapshot model** — pulse is a weekly snapshot, not live data
2. **AI-first narrative** — uses AI when available, heuristic fallback otherwise
3. **Rate-limited AI** — batch generation uses `AiJobsTable` for rate limiting
4. **Parent-scoped** — all queries filtered by `parent_id`
5. **Idempotent generation** — `UNIQUE(parent_id, student_id, week_start_date)` prevents duplicates

### 8.3 Core Types

```kotlin
class ParentPulseService(private val aiService: AiService) {
    suspend fun generatePulse(parentId: UUID, studentId: UUID, weekStart: LocalDate): ParentPulseDto {
        // 1. Aggregate week's data:
        //    - Attendance: count present/total for week
        //    - Marks: all AssessmentMarks published this week
        //    - Homework: due this week + submitted this week
        //    - Announcements: count for child's class this week
        //    - Messages: unread count
        //    - Events: upcoming in next 7 days
        // 2. Fetch previous week's pulse for trend comparison
        // 3. Call AiService for narrative summary
        // 4. Build actionable items list
        // 5. Store in parent_pulses
        // 6. Send notification
    }

    suspend fun batchGenerateAllSchools(): Int  // weekly job, returns count generated
    suspend fun getPulseHistory(parentId: UUID, studentId: UUID, weeks: Int): List<ParentPulseDto>
    suspend fun getLatestPulse(parentId: UUID, studentId: UUID): ParentPulseDto?
    suspend fun regeneratePulse(parentId: UUID, studentId: UUID, weekStart: LocalDate): ParentPulseDto  // DevTools
}
```

### 8.4 Repositories

- `ParentPulseRepository` — CRUD for pulse records

### 8.5 Mappers

- `PulseMapper` — maps DB rows to DTOs with parsed JSON fields (marks_summary, upcoming_events, actionable_items)

### 8.6 Permission Checks

- Parent endpoints: `requireAuth()` — parent can only view own children's pulses
- DevTools: `requireSchoolAdmin()` — admin can regenerate pulses

### 8.7 Background Jobs

- `PulseWeeklyJob` — every Sunday 6 PM IST; generates pulses for all active parent→student pairs

### 8.8 Domain Events

- `PulseGenerated` — emitted on pulse creation; triggers notification
- `PulseGenerationFailed` — emitted on generation error; logged

### 8.9 Caching

- Latest pulse: cached per parent→student pair, 1-hour TTL
- Pulse history: cached per parent→student pair, 1-hour TTL
- No cache for generation (real-time)

### 8.10 Transactions

- Pulse generation: single transaction (aggregate + AI + store). AI call is outside transaction (external API).
- Batch generation: each pulse generated independently. One failure doesn't block others.

### 8.11 Rate Limiting

- AI calls: rate-limited via `AiJobsTable` (existing infrastructure)
- Batch generation: sequential with 1-second delay between pulses to avoid API rate limits

### 8.12 Configuration

- `PULSE_WEEKLY_JOB_ENABLED` — default `true`; enable/disable weekly job
- `PULSE_WEEKLY_JOB_CRON` — default `0 0 18 * * SUN` (Sunday 6 PM IST)
- `PULSE_HISTORY_WEEKS` — default `12`; max weeks for history query
- `PULSE_AI_ENABLED` — default `false`; use AI narrative (vs heuristic)
- `PULSE_AI_MODEL` — default `gpt-4o-mini`; AI model for narrative
- `PULSE_BATCH_DELAY_MS` — default `1000`; delay between pulse generations in batch

---

## 9. API Contracts

### 9.1 Parent Endpoints

All require `requireAuth()`.

```
GET    /api/v1/parent/pulse/latest/{childId}
GET    /api/v1/parent/pulse/history/{childId}?weeks=12
```

### 9.2 DevTools Endpoints

```
POST   /api/v1/dev-tools/pulse/regenerate  { parent_id, student_id, week_start_date }
```

### 9.3 DTO Models

All `@Serializable`, wrapped in `ApiResponse<T>` pattern.

```kotlin
@Serializable data class ParentPulseDto(
    val id: String, val studentName: String,
    val weekStartDate: String, val weekEndDate: String,
    val weekRange: String,  // "Jun 21 - Jun 27, 2026"
    val attendancePercentage: Double?, val attendanceTrend: String?,
    val marksSummary: List<MarksSummaryDto>?,
    val homeworkPending: Int, val homeworkCompleted: Int,
    val announcementsCount: Int, val unreadMessages: Int,
    val upcomingEvents: List<UpcomingEventDto>?,
    val aiNarrative: String,
    val actionableItems: List<String>?,
    val modelUsed: String?, val tokensUsed: Int?,
    val createdAt: String
)

@Serializable data class MarksSummaryDto(
    val subject: String, val test: String,
    val marks: Double, val max: Double
)

@Serializable data class UpcomingEventDto(
    val title: String, val date: String
)

@Serializable data class RegeneratePulseDto(
    val parentId: String, val studentId: String, val weekStartDate: String
)
```

### 9.4 Response Example

```json
{
  "success": true,
  "data": {
    "student_name": "Aarav Sharma",
    "week_range": "Jun 21 - Jun 27, 2026",
    "attendance_percentage": 95.0,
    "attendance_trend": "up",
    "marks_summary": [
      {"subject": "Mathematics", "test": "Unit Test 2", "marks": 85, "max": 100}
    ],
    "homework_pending": 2,
    "homework_completed": 5,
    "announcements_count": 3,
    "unread_messages": 1,
    "upcoming_events": [{"title": "PTM", "date": "2026-07-01"}],
    "ai_narrative": "Aarav had a great week with 95% attendance and scored 85% in Math! Two homework submissions are pending — let's complete those before Monday.",
    "actionable_items": ["Submit Math homework", "Submit Science homework", "Confirm PTM attendance"]
  }
}
```

---

## 10. Frontend Architecture

### 10.1 Screens

| Screen | Platform | Role | Description |
|---|---|---|---|
| `ParentPulseScreen` | Compose | Parent | Pulse card with attendance ring, marks, homework, trends, AI narrative, actionable items |
| `PulseHistoryScreen` | Compose | Parent | List of past pulses (12 weeks) |
| `PulseCard` | Compose | Parent | Reusable pulse card component |

### 10.2 Navigation

- Parent portal → Pulse → `ParentPulseScreen`
- Parent portal → Pulse → History → `PulseHistoryScreen`

### 10.3 UX Flows

#### Parent: View Weekly Pulse

1. Parent receives push notification "📚 Aarav's weekly pulse is ready"
2. Taps notification → opens `ParentPulseScreen`
3. Sees attendance ring (95%), trend arrow (↑), marks summary, homework progress
4. Reads AI narrative ("Aarav had a great week...")
5. Reviews actionable items (pending homework, upcoming PTM)
6. Can scroll to view pulse history

### 10.4 State Management

```kotlin
data class ParentPulseState(
    val latestPulse: ParentPulseDto?,
    val pulseHistory: List<ParentPulseDto>,
    val isLoading: Boolean,
    val error: String?,
)
```

### 10.5 Offline Support

- Latest pulse cached locally for offline viewing
- Pulse history cached locally

### 10.6 Loading States

- Loading pulse: "Loading weekly pulse..."
- No pulse available: "Pulse not yet generated. Check back after Sunday 6 PM."

### 10.7 Error Handling (UI)

- Pulse not found: "No pulse available for this week."
- Network error: "Failed to load pulse. Please check your connection."
- AI narrative unavailable: Show heuristic text (transparent to user)

### 10.8 Component Integration Guidelines

| Rule | Description |
|---|---|
| **R1** | Attendance ring showing percentage with color (green >90%, yellow >75%, red <75%) |
| **R2** | Trend arrows (↑ green, ↓ red, → gray) for attendance, marks, homework |
| **R3** | Marks summary as list with subject, test name, marks/max |
| **R4** | Homework progress bar (completed/total) |
| **R5** | AI narrative in highlighted card with warm styling |
| **R6** | Actionable items as checklist with icons |
| **R7** | Upcoming events with date badges |
| **R8** | Pulse history as scrollable list with week labels |
| **R9** | Student name header with avatar |

---

## 11. Shared Module Changes (KMP)

### 11.1 DTOs

All DTOs defined in section 9.3, placed in `shared/.../feature/pulse/data/remote/`.

### 11.2 Domain Models

```kotlin
data class ParentPulse(
    val id: UUID, val studentName: String,
    val weekStartDate: LocalDate, val weekEndDate: LocalDate,
    val attendancePercentage: Double?, val attendanceTrend: Trend,
    val marksSummary: List<MarksSummary>?,
    val homeworkPending: Int, val homeworkCompleted: Int,
    val announcementsCount: Int, val unreadMessages: Int,
    val upcomingEvents: List<UpcomingEvent>?,
    val aiNarrative: String,
    val actionableItems: List<String>?,
    val modelUsed: String?, val tokensUsed: Int?,
    val createdAt: Instant,
)

enum class Trend { UP, DOWN, STABLE }

data class MarksSummary(
    val subject: String, val test: String,
    val marks: Double, val max: Double,
)

data class UpcomingEvent(
    val title: String, val date: LocalDate,
)
```

### 11.3 Repository Interfaces

```kotlin
interface PulseRepository {
    suspend fun getLatestPulse(childId: String): NetworkResult<ParentPulseDto>
    suspend fun getPulseHistory(childId: String, weeks: Int): NetworkResult<List<ParentPulseDto>>
}
```

### 11.4 UseCases

- `GetLatestPulseUseCase`
- `GetPulseHistoryUseCase`

### 11.5 Validation

- Child ID: valid UUID
- Weeks: positive integer, max 52

### 11.6 Serialization

Standard Kotlinx serialization with JSON for marks_summary, upcoming_events, actionable_items.

### 11.7 Network APIs

Ktor `@Resource` route definitions:
- `ParentPulseApi` — parent endpoints

### 11.8 Database Models (Local Cache)

- Latest pulse cached locally
- Pulse history cached locally

---

## 12. Permissions Matrix

| Action | Super Admin | School Admin | Teacher | Parent |
|---|---|---|---|---|
| View own child's pulse | N/A | N/A | N/A | ✅ |
| View pulse history | N/A | N/A | N/A | ✅ |
| Regenerate pulse (DevTools) | ✅ | ✅ | ❌ | ❌ |
| Opt out of pulse notifications | N/A | N/A | N/A | ✅ |
| View pulse generation logs | ✅ | ✅ | ❌ | ❌ |

---

## 13. Notifications

### Pulse-Specific Notification Triggers

| Type | Trigger | Recipient | Channel | Message |
|---|---|---|---|---|
| Weekly Pulse Ready | Pulse generation complete (Sunday 6 PM) | Parent | Push (FCM) | "📚 {student_name}'s weekly pulse is ready" |

### Notification System Integration

- Reuse `NotificationService` for FCM push notifications
- Deep link to `ParentPulseScreen` on tap
- Check `NotificationPreferencesTable` for opt-out before sending
- No WhatsApp or email for pulse (push only)

### Notification Opt-Out

- Parent can opt out via `NotificationPreferencesTable`
- Pulse-specific preference key: `pulse_notifications`
- Backend checks preference before sending FCM
- No UI toggle yet (TD-2)

---

## 14. Background Jobs

### Pulse Weekly Job

| Field | Value |
|---|---|
| **Name** | `PulseWeeklyJob` |
| **Trigger** | Weekly |
| **Frequency** | Every Sunday at 6 PM IST |
| **Description** | Generates weekly pulse for all active parent→student pairs across all schools |
| **Timeout** | 600 seconds (large number of parents) |
| **Retry** | None (next week's job will generate) |
| **On failure** | Logged; failed pulses skipped. Successful pulses still delivered. |

### Batch Generation Flow

1. Query all active parent→student links from `ParentChildLinksTable`
2. For each pair:
   a. Aggregate week's data (attendance, marks, homework, announcements, messages, events)
   b. Fetch previous week's pulse for trend comparison
   c. Generate AI narrative (or heuristic fallback)
   d. Build actionable items list
   e. Store in `parent_pulses`
   f. Send FCM notification (if not opted out)
3. Delay 1 second between pulses (rate limiting)
4. Return total count generated

---

## 15. Integrations

### Internal Integrations

| System | Integration Point | Direction | Protocol | Error Handling |
|---|---|---|---|---|
| `AttendanceTable` | Attendance aggregation (present/total for week) | Read | Direct DB | Default to 0% if no records |
| `AssessmentMarksTable` | Marks aggregation (published this week) | Read | Direct DB | Skip if no marks |
| `HomeworkTable` | Homework status (due/submitted this week) | Read | Direct DB | Default to 0 if no homework |
| `AnnouncementsTable` | Announcements count for child's class | Read | Direct DB | Default to 0 |
| `MessagesTable` | Unread messages count | Read | Direct DB | Default to 0 |
| `CalendarEventsTable` | Upcoming events (next 7 days) | Read | Direct DB | Empty list if none |
| `ParentChildLinksTable` | Active parent→student links for batch | Read | Direct DB | Skip inactive links |
| `NotificationService` | FCM push notification | Outbound | Direct call | Logged; non-blocking |
| `NotificationPreferencesTable` | Check opt-out before notification | Read | Direct DB | Default to enabled |
| `AiService` | AI narrative generation | Outbound | Direct call | Heuristic fallback on failure |
| `AiJobsTable` | Rate-limited AI job queue | Write | Direct DB | Logged; fallback to heuristic |
| `SchoolsTable` | School context for batch | Read | Direct DB | Skip if school inactive |

### External Integrations

| System | Purpose | Direction | Protocol | Authentication | Error Handling |
|---|---|---|---|---|---|
| OpenAI API (via `AiService`) | AI narrative generation | Outbound | HTTP API | Bearer token (via `AiService`) | Heuristic fallback; log error |

### Integration Patterns

- **Data aggregation:** Direct DB queries against existing tables. All reads, no writes to source tables.
- **AI narrative:** Call `AiService` with prompt template. If AI unavailable or `PULSE_AI_ENABLED=false`, use heuristic text generation.
- **Notification:** `NotificationService.sendPushNotification()` after pulse stored. Check `NotificationPreferencesTable` first.
- **Batch processing:** Sequential with 1-second delay. Each pulse independent. One failure doesn't block others.

---

## 16. Security

### Authentication

- Parent endpoints: standard JWT auth via `requireAuth()`
- DevTools endpoint: `requireSchoolAdmin()`

### Authorization

- Parent can only view pulses for their own linked children
- Server validates parent→student relationship via `ParentChildLinksTable`
- DevTools regeneration requires school admin

### Data Protection

- Pulse data is parent-scoped — parent can only see own children's pulses
- Student data in pulse (attendance, marks, homework) — standard PII
- AI narrative contains student name — standard PII
- No sensitive data beyond standard school management

### Input Validation

- Child ID: valid UUID, must be linked to authenticated parent
- Weeks parameter: positive integer, max 52
- DevTools: parent_id, student_id, week_start_date all required and valid

### Rate Limiting

- Standard API rate limiting for all endpoints
- AI calls rate-limited via `AiJobsTable`
- Batch generation: 1-second delay between pulses

### Audit Logging

- Pulse generation (weekly job): count per school, errors logged
- Pulse regeneration (DevTools): admin ID, parent ID, student ID, week
- Pulse view: not logged (read-only, high frequency)

### PII Handling

- Student name in pulse — standard PII
- Attendance percentage, marks, homework — student performance data
- AI narrative contains student name — generated, not stored externally
- No additional PII beyond standard school management

### Multi-tenant Isolation

- `parent_pulses.school_id` — NOT NULL, FK to schools
- Batch generation queries per school
- Parent endpoints validate parent→student→school chain
- No cross-school pulse access

---

## 17. Performance & Scalability

### Expected Scale

- Small school: 50-200 parent→student pairs
- Medium school: 200-1,000 parent→student pairs
- Large school: 1,000-5,000 parent→student pairs
- Multi-school: 10,000+ total pairs

### Query Optimization

- **Latest pulse:** `idx_parent_pulses_parent` on `(parent_id, week_start_date DESC)`. Single row.
- **Pulse history:** Same index, LIMIT 12. Small dataset.
- **Batch generation:** Sequential. 1-second delay. 1,000 pulses = ~17 minutes.
- **Data aggregation:** 6 queries per pulse (attendance, marks, homework, announcements, messages, events). All indexed.

### Indexing Strategy

- `idx_parent_pulses_parent` — listing by parent + week (DESC for latest first)
- `UNIQUE(parent_id, student_id, week_start_date)` — prevents duplicates

### Caching Strategy

- Latest pulse: cached per parent→student pair, 1-hour TTL
- Pulse history: cached per parent→student pair, 1-hour TTL
- No cache for generation (real-time batch)

### Pagination

- Pulse history: LIMIT 12 (or `weeks` parameter, max 52). No cursor pagination needed.

### Connection Pooling

- Uses existing HikariCP connection pool
- No additional pooling needed

### Async Processing

- Batch generation: sequential (not parallel) to avoid AI rate limits
- Notification delivery: async (existing `NotificationService` pattern)
- AI narrative: synchronous within pulse generation (with timeout)

### Scalability Concerns

- 10,000+ pulses per week: ~3 hours at 1-second delay. Acceptable for weekly job.
- AI rate limits: `AiJobsTable` manages rate limiting. Heuristic fallback if AI overwhelmed.
- Pulse storage: ~52 pulses per pair per year. 10,000 pairs = 520,000 rows/year. Manageable.
- Aggregation queries: 6 queries × 10,000 pairs = 60,000 queries. Spread over 3 hours. Fine.

---

## 18. Edge Cases

| # | Scenario | Expected Behavior |
|---|---|---|
| EC-1 | Parent has multiple children | Generate separate pulse for each child. Parent receives multiple notifications. |
| EC-2 | Student has no attendance records for week | Attendance percentage = null. AI narrative mentions "no attendance data." |
| EC-3 | Student has no marks for week | Marks summary = empty list. AI narrative skips marks. |
| EC-4 | Student has no homework for week | Homework pending = 0, completed = 0. AI narrative skips homework. |
| EC-5 | No previous week's pulse exists | Trend = "stable" or null. No comparison shown. |
| EC-6 | AI service unavailable | Use heuristic text generation. Log warning. Store `model_used = "heuristic"`. |
| EC-7 | AI service returns error | Same as EC-6. Heuristic fallback. |
| EC-8 | Parent opted out of notifications | Pulse generated and stored. No FCM notification sent. Parent can still view in-app. |
| EC-9 | Parent→student link deactivated mid-batch | Skip that pair. Already-generated pulses remain. |
| EC-10 | Pulse already exists for this week (re-run) | `UNIQUE` constraint. Upsert: overwrite existing pulse. |
| EC-11 | Student name changed after pulse generation | Pulse retains old name (snapshot). Next week's pulse uses new name. |
| EC-12 | School has no active parent→student links | Batch returns 0. No pulses generated. |
| EC-13 | Week spans across months | Week range displayed as "Jun 28 - Jul 4, 2026". Normal behavior. |
| EC-14 | Parent views pulse before Sunday 6 PM | No pulse for current week. Show last week's pulse or "Pulse not yet generated." |
| EC-15 | DevTools regenerate for future week | Return 400 "Cannot generate pulse for future week." |
| EC-16 | DevTools regenerate for past week (>52 weeks ago) | Allowed. Generates historical pulse. |
| EC-17 | AI tokens exceed limit | `AiJobsTable` handles rate limiting. Pulse waits in queue. Heuristic fallback if timeout. |
| EC-18 | Student has 100% attendance | Attendance ring shows green. AI narrative highlights perfect attendance. |

---

## 19. Error Handling

### Error Response Format

Standard `ApiResponse` error format:
```json
{
  "success": false,
  "error": {
    "code": "PULSE_NOT_FOUND",
    "message": "No pulse available",
    "details": {}
  }
}
```

### Error Codes

| Code | HTTP Status | Description | User Message |
|---|---|---|---|
| `PULSE_NOT_FOUND` | 404 | No pulse found for this week | "No pulse available for this week." |
| `CHILD_NOT_LINKED` | 403 | Parent doesn't have access to this child | "You don't have access to this child's pulse." |
| `INVALID_WEEKS_PARAM` | 400 | Weeks parameter invalid | "Weeks must be between 1 and 52." |
| `PULSE_GENERATION_FAILED` | 500 | Pulse generation error | "Failed to generate pulse. Please try again." |
| `FUTURE_WEEK_NOT_ALLOWED` | 400 | DevTools: future week | "Cannot generate pulse for future week." |

### Error Handling Strategy

- **Validation errors:** Return 400 with field-specific message
- **Auth errors:** Return 401/403 with clear message
- **Not found:** Return 404
- **Server errors:** Return 500 with generic message; log full error
- **AI errors:** Fallback to heuristic; log warning (not user-facing)

### Retry Strategy

- Client retries: 3 attempts with exponential backoff for 5xx errors
- No retry for 4xx errors (client errors)
- AI narrative: 1 retry, then heuristic fallback
- Batch generation: no retry for individual pulses (next week's job handles it)

### Fallback Behavior

- AI unavailable: Heuristic text generation (template-based)
- Source data missing: Default values (0%, empty lists, 0 counts)
- Notification failure: Logged; pulse still stored and viewable in-app
- Previous pulse missing: Trend = "stable" or null

---

## 20. Analytics & Reporting

### Analytics Dashboard Data

| Metric | Source | Derivation |
|---|---|---|
| Total pulses generated | `parent_pulses` count | Direct count |
| Pulses generated this week | `parent_pulses` where week_start_date = current week | Direct count |
| AI vs heuristic usage | `parent_pulses.model_used` | Group by model, count |
| AI token usage | `parent_pulses.tokens_used` sum | Aggregate sum |
| Pulse notification opt-out rate | `NotificationPreferencesTable` | Count opted out / total parents |
| Pulse view rate (future) | Analytics events (not yet implemented) | Views / pulses delivered |

### Export Capabilities

- Pulse export (CSV) — student, week, attendance %, marks, homework, narrative

### Report Types

| Report | Format | Frequency | Recipient |
|---|---|---|---|
| Pulse generation summary | JSON (API) | On-demand | School Admin |
| AI usage report | JSON (API) | On-demand | Dev Team |
| Pulse engagement (future) | JSON (API) | On-demand | School Admin |

---

## 21. Testing Strategy

### Unit Tests

- `ParentPulseService` — all methods (generatePulse, batchGenerateAllSchools, getPulseHistory, getLatestPulse, regeneratePulse)
- Data aggregation: attendance %, marks, homework, announcements, messages, events
- Trend comparison: up, down, stable
- Actionable items building
- Heuristic narrative generation
- AI narrative integration (mock `AiService`)
- Opt-out check

### Integration Tests

- Full pulse generation: create test data → generate pulse → verify all fields
- Batch generation: multiple parent→student pairs → verify all pulses generated
- Pulse history: generate 3 weeks → query history → verify 3 results sorted DESC
- DevTools regeneration: generate → regenerate → verify overwrite
- Multi-tenant: school A pulses not accessible to school B parent
- Parent authorization: parent can only view own children's pulses

### E2E Tests

- Weekly job runs → pulse generated → notification sent → parent views pulse in app
- AI unavailable → heuristic fallback → pulse still generated

### Performance Tests

- Batch generation for 1,000 pairs: < 20 minutes (with 1-second delay)
- Pulse history query with 52 records: < 200ms
- Data aggregation for single pulse: < 500ms

### Test Data

- 5 sample parent→student pairs
- 3 weeks of attendance, marks, homework data
- Sample announcements and messages
- Mock AI service responses
- Parent with opt-out preference

### Test Environment

- Test database with schema migration applied
- Mock `AiService` for AI narrative tests
- Mock `NotificationService` for notification tests
- Test JWT tokens for parent and admin roles

---

## 22. Acceptance Criteria

- [ ] Weekly pulse generated every Sunday 6 PM IST
- [ ] Aggregates attendance, marks, homework, announcements, messages, events
- [ ] AI narrative is specific and parent-friendly (or heuristic fallback)
- [ ] Trend indicators compare to previous week
- [ ] Actionable items listed
- [ ] Push notification sent (if not opted out)
- [ ] In-app card with visual summary (attendance ring, trend arrows, marks, homework)
- [ ] Pulse history (12 weeks) available
- [ ] Parent can opt out of pulse notifications
- [ ] DevTools regeneration works
- [ ] Parent can only view own children's pulses
- [ ] Batch generation handles 1,000+ pairs
- [ ] AI fallback to heuristic on service failure

---

## 23. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration (`051`), Exposed table (already done) |
| 2 | 2 days | Data aggregation service (already done) |
| 3 | 1 day | AI narrative prompt + integration (pending — currently heuristic) |
| 4 | 1 day | Scheduled weekly job (already done) |
| 5 | 1 day | API endpoints (already done) |
| 6 | 3 days | Client UI (pulse card, history, visual summary) (already done) |
| 7 | 1 day | Tests (already done — 310 lines) |

### Remaining Work

| Task | Effort | Status |
|---|---|---|
| AI narrative via `AiService` | 1 day | Pending — requires `AI_INFRASTRUCTURE_SPEC.md` |
| Opt-out UI toggle | 0.5 day | Pending — TD-2 |
| Pulse engagement tracking | 1 day | Pending — TD-3 |

### Pre-Implementation Checklist

- [x] DB migration applied
- [x] Exposed table registered
- [x] Data aggregation service implemented
- [x] Weekly job scheduled
- [x] API endpoints deployed
- [x] Client UI shipped
- [x] Tests written
- [ ] AI infrastructure built (`AI_INFRASTRUCTURE_SPEC.md`)
- [ ] AI narrative prompt tuned
- [ ] Opt-out UI toggle

---

## 24. File-Level Impact Analysis

### Already Implemented (7 files)

| # | File | Change Type | Description |
|---|---|---|---|
| 1 | `docs/db/migration_051_parent_pulse.sql` | New | DDL: `parent_pulses` table |
| 2 | `server/.../db/Tables.kt` | Modify | Add `ParentPulsesTable` |
| 3 | `server/.../feature/pulse/ParentPulseService.kt` | New | Core service (aggregation, AI, storage, notification) |
| 4 | `server/.../feature/pulse/PulseWeeklyJob.kt` | New | Weekly scheduled job (Sunday 6 PM IST) |
| 5 | `server/.../feature/pulse/PulseRouting.kt` | New | API endpoints + DTOs |
| 6 | `composeApp/.../ui/v2/screens/parent/ParentPulseScreen.kt` | New | Pulse card UI |
| 7 | `composeApp/.../ui/v2/components/PulseCard.kt` | New | Reusable pulse card component |

### Additional Already Implemented (2 files)

| # | File | Change Type | Description |
|---|---|---|---|
| 8 | `shared/.../feature/pulse/ParentPulseViewModel.kt` | New | Client state management |
| 9 | `server/.../feature/devtools/DevToolsRouting.kt` | Modify | Manual pulse regeneration endpoint |

### Tests (1 file)

| # | File | Change Type | Description |
|---|---|---|---|
| 10 | `server/.../feature/pulse/ParentPulseServiceTest.kt` | New | 310 lines covering pulse aggregation logic |

### Pending (1 file)

| # | File | Change Type | Description |
|---|---|---|---|
| 11 | `composeApp/.../ui/v2/screens/parent/NotificationSettingsScreen.kt` | Modify | Add pulse opt-out toggle (TD-2) |

---

## 25. Future Enhancements

| # | Enhancement | Priority | Effort | Notes |
|---|---|---|---|---|
| F-1 | AI narrative via LLM | High | S | Replace heuristic with `AiService` once AI infra is built |
| F-2 | Pulse engagement tracking | Medium | M | Track open rates, time spent viewing pulse |
| F-3 | Pulse opt-out UI toggle | Medium | S | Add toggle in notification settings |
| F-4 | Daily pulse option | Low | M | Optional daily mini-pulse for engaged parents |
| F-5 | Pulse sharing | Low | S | Share pulse with co-parent or family member |
| F-6 | Pulse insights dashboard | Low | M | Admin dashboard showing pulse engagement trends |
| F-7 | Multi-language pulse | Medium | M | AI narrative in parent's preferred language |
| F-8 | Pulse comparisons | Low | S | Compare child's pulse to class average (anonymized) |
| F-9 | Pulse reminders | Low | S | Remind parent to view pulse if not opened by Monday |
| F-10 | Custom pulse frequency | Low | M | Parent chooses weekly/bi-weekly/monthly pulse |

---

## Appendix A: Sequence Diagrams

### A.1 Weekly Pulse Generation (Batch Job)

```
PulseWeeklyJob    ParentChildLinksTable    ParentPulseService    AiService    NotificationService
  │                    │                        │                   │                │
  │──query active links──→│                     │                   │                │
  │←──list of pairs──│                            │                   │                │
  │  [for each pair] │                        │                   │                │
  │──generatePulse(parentId,studentId,weekStart)──→│                │                │
  │                    │  ──aggregate attendance──│                │                │
  │                    │  ──aggregate marks──────│                │                │
  │                    │  ──aggregate homework───│                │                │
  │                    │  ──aggregate announcements│               │                │
  │                    │  ──aggregate messages───│                │                │
  │                    │  ──aggregate events─────│                │                │
  │                    │  ──fetch prev week pulse│                │                │
  │                    │  ──generate narrative──│────────────────→│                │
  │                    │  ←──narrative text──│←──────────────────│                │
  │                    │  ──build actionable items│               │                │
  │                    │  ──store pulse──────────│                │                │
  │                    │  ──check opt-out────────│                │                │
  │                    │  ──send notification─────────────────────────────────────→│
  │←──count──────────│                        │                   │                │
  │  [delay 1s, next pair]                    │                   │                │
  │                    │                        │                   │                │
```

### A.2 Parent Views Latest Pulse

```
Parent       PulseRouting    ParentPulseService    (cache)    ParentPulsesTable
  │                │                │                │              │
  │──GET /pulse/latest/{childId}──→│               │              │
  │                │──check cache──│──────────────→│              │
  │                │  [cache hit]  │                │              │
  │                │←──cached pulse│──────────────│              │
  │                │  [cache miss] │                │              │
  │                │──query latest──│──────────────────────────────→│
  │                │←──pulse row──│───────────────────────────────│
  │                │──cache result──│──────────────→│              │
  │←──ParentPulseDto│              │                │              │
  │                │                │                │              │
```

### A.3 DevTools Pulse Regeneration

```
Admin       DevToolsRouting    ParentPulseService    ParentPulsesTable
  │                │                    │                    │
  │──POST /regenerate──→│               │                    │
  │                │──regeneratePulse(parentId,studentId,week)──→│
  │                │                    │──check existing──│
  │                │                    │←──existing pulse──│
  │                │                    │──re-aggregate data│
  │                │                    │──re-generate narrative│
  │                │                    │──upsert pulse──│
  │                │                    │←──updated pulse──│
  │←──ParentPulseDto│                    │                    │
  │                │                    │                    │
```

---

## Appendix B: Domain Model / ER Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                          parent_pulses                                │
│  id (PK)                                                              │
│  school_id (FK→schools)                                               │
│  parent_id                                                            │
│  student_id                                                           │
│  student_name                                                         │
│  week_start_date, week_end_date                                       │
│  attendance_percentage, attendance_trend                              │
│  marks_summary (JSON)                                                 │
│  homework_pending, homework_completed                                 │
│  announcements_count, unread_messages                                 │
│  upcoming_events (JSON)                                               │
│  ai_narrative                                                         │
│  actionable_items (JSON)                                              │
│  model_used, tokens_used                                              │
│  created_at                                                           │
│  UNIQUE(parent_id, student_id, week_start_date)                       │
└──────────────────────────────────────────────────────────────────────┘

Source tables (read-only for aggregation):
┌──────────────────┐  ┌──────────────────────┐  ┌──────────────────┐
│ AttendanceTable   │  │ AssessmentMarksTable  │  │  HomeworkTable    │
└──────────────────┘  └──────────────────────┘  └──────────────────┘
┌──────────────────┐  ┌──────────────────┐  ┌────────────────────┐
│AnnouncementsTable │  │  MessagesTable    │  │ CalendarEventsTable │
└──────────────────┘  └──────────────────┘  └────────────────────┘
┌──────────────────────┐  ┌──────────────────────────┐
│ ParentChildLinksTable │  │ NotificationPreferencesTable │
└──────────────────────┘  └──────────────────────────┘
```

---

## Appendix C: Event Flow

### Domain Events

| Event | Emitter | Consumers | Payload | Side Effects |
|---|---|---|---|---|
| `PulseGenerated` | `ParentPulseService.generatePulse()` | NotificationService | `pulseId, parentId, studentId, studentName, weekStart` | FCM notification sent (if not opted out) |
| `PulseGenerationFailed` | `ParentPulseService.generatePulse()` | None (logged) | `parentId, studentId, weekStart, error` | Logged; no notification |
| `PulseBatchCompleted` | `PulseWeeklyJob.batchGenerateAllSchools()` | None (logged) | `totalGenerated, totalFailed, duration` | Logged |
| `PulseRegenerated` | `ParentPulseService.regeneratePulse()` | None | `pulseId, parentId, studentId, weekStart, adminId` | Logged |

### Event Delivery Guarantees

- Events are emitted synchronously within pulse generation
- Notification delivery is async (fire-and-forget with logging)
- Failed notifications are logged; not retried (pulse still viewable in-app)
- No event bus / message queue — direct function calls

### AI Prompt Template

```
System: You are writing a weekly summary for a parent about their child's school week.
Be warm, specific, and encouraging. 2-3 sentences max. Include one highlight and one area to focus on.

User: Student: {{name}}, Week: {{week_range}}
Attendance: {{attendance}}% (last week: {{prev_attendance}}%)
Marks: {{marks_summary}}
Homework: {{completed}} completed, {{pending}} pending
Announcements: {{announcements_count}}
Messages: {{unread}} unread
Upcoming: {{upcoming_events}}
```

---

## Appendix D: Configuration

### Environment Variables

| Variable | Default | Description |
|---|---|---|
| `PULSE_WEEKLY_JOB_ENABLED` | `true` | Enable/disable weekly pulse job |
| `PULSE_WEEKLY_JOB_CRON` | `0 0 18 * * SUN` | Cron expression (Sunday 6 PM IST) |
| `PULSE_HISTORY_WEEKS` | `12` | Default weeks for history query |
| `PULSE_AI_ENABLED` | `false` | Use AI narrative (vs heuristic) |
| `PULSE_AI_MODEL` | `gpt-4o-mini` | AI model for narrative |
| `PULSE_BATCH_DELAY_MS` | `1000` | Delay between pulse generations in batch |
| `PULSE_CACHE_TTL` | `3600` | Pulse cache TTL in seconds (1 hour) |
| `PULSE_AI_TIMEOUT_MS` | `10000` | AI narrative call timeout |

### Feature Flags

| Flag | Default | Description |
|---|---|---|
| `PULSE_ENABLED` | `true` | Enable/disable pulse feature |
| `PULSE_AI_NARRATIVE` | `false` | Use AI for narrative (vs heuristic) |
| `PULSE_NOTIFICATIONS` | `true` | Send push notifications for new pulses |
| `PULSE_DEVTOOLS_REGEN` | `true` | Allow DevTools pulse regeneration |

### School-Level Settings

N/A — pulse is parent-scoped. No school-level configuration beyond standard notification preferences.

---

## Appendix E: Migration & Rollback

### Migration: `migration_051_parent_pulse.sql` (already applied)

```sql
-- Migration 051: Parent Pulse
-- Creates 1 new table

BEGIN;

CREATE TABLE IF NOT EXISTS parent_pulses (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL REFERENCES schools(id),
    parent_id       UUID NOT NULL,
    student_id      UUID NOT NULL,
    student_name    TEXT NOT NULL,
    week_start_date DATE NOT NULL,
    week_end_date   DATE NOT NULL,
    attendance_percentage REAL,
    attendance_trend VARCHAR(8),
    marks_summary   TEXT,
    homework_pending INTEGER NOT NULL DEFAULT 0,
    homework_completed INTEGER NOT NULL DEFAULT 0,
    announcements_count INTEGER NOT NULL DEFAULT 0,
    unread_messages INTEGER NOT NULL DEFAULT 0,
    upcoming_events TEXT,
    ai_narrative    TEXT NOT NULL,
    actionable_items TEXT,
    model_used      VARCHAR(64),
    tokens_used     INTEGER,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(parent_id, student_id, week_start_date)
);
CREATE INDEX IF NOT EXISTS idx_parent_pulses_parent ON parent_pulses(parent_id, week_start_date DESC);

COMMIT;
```

### Rollback: `migration_051_rollback.sql`

```sql
BEGIN;
DROP TABLE IF EXISTS parent_pulses;
COMMIT;
```

### Migration Validation

- Verify table created with correct columns
- Verify FK constraint on `school_id`
- Verify `UNIQUE(parent_id, student_id, week_start_date)` constraint works
- Verify index `idx_parent_pulses_parent` created
- Run `SELECT count(*) FROM parent_pulses` — should be 0 (new feature)

---

## Appendix F: Observability

### Structured Logging

| Log Level | Event | Context Fields |
|---|---|---|
| INFO | Pulse generated | `pulseId, parentId, studentId, studentName, weekStart, modelUsed, tokensUsed` |
| INFO | Batch completed | `totalGenerated, totalFailed, durationMs` |
| INFO | Pulse regenerated (DevTools) | `pulseId, parentId, studentId, weekStart, adminId` |
| WARN | AI service unavailable, using heuristic | `parentId, studentId, weekStart, error` |
| WARN | AI service error, using heuristic | `parentId, studentId, weekStart, error` |
| WARN | Parent opted out, no notification sent | `parentId, studentId` |
| WARN | No previous pulse for trend comparison | `parentId, studentId, weekStart` |
| ERROR | Pulse generation failed | `parentId, studentId, weekStart, error` |
| ERROR | Batch job failed | `error, stackTrace` |

### Metrics

| Metric | Type | Labels | Description |
|---|---|---|---|
| `pulse_generated_total` | Counter | `school_id, model_used` | Pulses generated by model (ai/heuristic) |
| `pulse_generation_duration` | Histogram | `model_used` | Pulse generation latency |
| `pulse_batch_duration` | Histogram | — | Weekly batch job duration |
| `pulse_ai_tokens_used` | Counter | `model` | AI token consumption |
| `pulse_notifications_sent` | Counter | `school_id` | FCM notifications sent |
| `pulse_notifications_opted_out` | Counter | `school_id` | Pulses where notification skipped (opt-out) |
| `pulse_views` | Counter | `school_id` | Pulse view events (future) |
| `pulse_history_queries` | Counter | `school_id` | History endpoint queries |

### Health Checks

| Check | Endpoint | Description |
|---|---|---|
| Pulse table exists | `/health/pulse` | Verify `parent_pulses` table is accessible |
| Weekly job last run | `/health/pulse/job` | Verify weekly job ran within last 7 days |

### Alerts

| Alert | Condition | Severity | Notification |
|---|---|---|---|
| Weekly job not running | No `pulse_batch_duration` metric in 7 days | Critical | PagerDuty / email |
| AI failure rate high | `pulse_generated_total` heuristic rate > 50% when `PULSE_AI_ENABLED=true` | Warning | Email to dev team |
| Batch generation slow | `pulse_batch_duration` > 60 minutes | Warning | Email to dev team |
| Pulse generation errors | `pulse_generation_failed` count > 10 in one batch | Warning | Email to dev team |

### Dashboards

| Dashboard | Panels | Audience |
|---|---|---|
| Pulse Overview | Total pulses, generated this week, AI vs heuristic rate | Dev Team |
| AI Usage | Token usage, model distribution, AI latency, error rate | Dev Team |
| Pulse Engagement | Notification sent vs opt-out, view rate (future) | School Admin |
| Batch Job Health | Last run time, duration, success/failure count | Dev Team |
