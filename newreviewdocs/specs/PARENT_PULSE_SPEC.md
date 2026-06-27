# Parent Pulse — Technical Specification

> **Document status:** Implemented (85%) — AI narrative generation pending (uses heuristic summaries currently)
> **Last updated:** 2026-06-28
> **Prerequisites:** `AI_INFRASTRUCTURE_SPEC.md`
> **Source:** `DIFFERENTIATING_FEATURES.md` §2.1

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

---

## 2. Current System Assessment

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

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Weekly job (Sunday 6 PM IST) generates pulse for each parent with active children |
| FR-2 | Aggregate: attendance % (week), marks received (week), homework status, announcements count, unread messages |
| FR-3 | AI narrative summary via `AiService` (2-3 sentences, parent-friendly) |
| FR-4 | Trend indicators comparing to previous week |
| FR-5 | Actionable items list (pending homework, unread messages, upcoming PTM) |
| FR-6 | Push notification: "Aarav's weekly pulse is ready" |
| FR-7 | In-app card with visual summary (progress bars, trend arrows) |
| FR-8 | Pulse history (last 12 weeks) |
| FR-9 | Parent can opt out of pulse notifications |

---

## 4. Database Design

```sql
CREATE TABLE parent_pulses (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
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

---

## 5. Backend Architecture

### 5.1 ParentPulseService

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
    suspend fun getPulseHistory(parentId: UUID, studentId: UUID): List<ParentPulseDto>
}
```

### 5.2 AI Prompt

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

### 5.3 Scheduled Job

Every Sunday 6 PM IST:
1. Query all active parent→student links (`ParentChildLinksTable`)
2. For each, generate pulse (batch via `AiJobsTable` for rate limiting)
3. Send FCM notification: "📚 {student_name}'s weekly pulse is ready"

---

## 6. API Contracts

```
GET /api/v1/parent/pulse/latest/{childId}
GET /api/v1/parent/pulse/history/{childId}?weeks=12
```

**Response:**
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

## 7. Acceptance Criteria

- [ ] Weekly pulse generated every Sunday 6 PM IST
- [ ] Aggregates attendance, marks, homework, announcements, messages, events
- [ ] AI narrative is specific and parent-friendly
- [ ] Trend indicators compare to previous week
- [ ] Actionable items listed
- [ ] Push notification sent
- [ ] In-app card with visual summary
- [ ] Pulse history (12 weeks) available
- [ ] Parent can opt out

---

## 8. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration, Exposed table |
| 2 | 2 days | Data aggregation service |
| 3 | 1 day | AI narrative prompt + integration |
| 4 | 1 day | Scheduled weekly job |
| 5 | 1 day | API endpoints |
| 6 | 3 days | Client UI (pulse card, history, visual summary) |
| 7 | 1 day | Tests |

---

## 9. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | `ParentPulsesTable` |
| `server/.../feature/pulse/ParentPulseService.kt` | New | Core service |
| `server/.../feature/pulse/PulseWeeklyJob.kt` | New | Scheduled job |
| `server/.../feature/pulse/PulseRouting.kt` | New | API endpoints |
| `docs/db/migration_067_parent_pulse.sql` | New | DDL |
| `composeApp/.../ui/v2/screens/parent/ParentPulseScreen.kt` | New | Pulse card UI |
| `composeApp/.../ui/v2/components/PulseCard.kt` | New | Reusable pulse card component |
