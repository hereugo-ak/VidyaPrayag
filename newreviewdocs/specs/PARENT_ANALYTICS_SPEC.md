# Parent Analytics Dashboard — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** None
> **Source:** `DIFFERENTIATING_FEATURES.md` §10.1
> **Template:** `_SPEC_TEMPLATE.md` v1 (25 mandatory + 6 optional sections)

---

## 1. Feature Overview

### What

A comprehensive analytics dashboard for parents showing their child's academic performance trends, attendance patterns, engagement metrics, and comparative insights (anonymized class average).

### Why — Product Rationale

Parents currently see raw data (attendance %, marks) but no trends, comparisons, or insights. An analytics dashboard transforms raw data into actionable insights: "Your child's math scores are declining," "Attendance dropped this month," "Below class average in science." This helps parents take informed action.

This is a **differentiating feature** (Priority P1, Phase 2, effort M, "High" value per `DIFFERENTIATING_FEATURES.md`). All data already exists — this is a presentation and aggregation layer.

### What Stands Out (Competitive Moat)

From `DIFFERENTIATING_FEATURES.md` §10.1:
> "Parent Analytics — attendance trends, marks trends, class comparison, engagement score. Data readiness: All data exists."

No major school ERP provides parent-facing analytics with trend charts, class comparison, and engagement scores. Most show only current-term raw data.

### Goals

- Attendance trend chart (monthly, term-wise)
- Marks trend per subject (line chart across terms)
- Class comparison: child vs class average (anonymized)
- Engagement score: homework completion rate, message response rate
- Growth indicators: improving/declining subjects
- Calendar heat map: attendance visualization

### Non-goals

- [ ] Predictive analytics (AI predictions of future performance)
- [ ] Cross-student comparison (only class average, no individual comparison)
- [ ] Teacher analytics (separate spec)
- [ ] School-wide analytics (separate spec)
- [ ] Export to PDF (future enhancement)
- [ ] Custom date ranges (only term/year/all presets)

### Dependencies

- `AttendanceRecordsTable` — existing attendance data
- `AssessmentMarksTable` — existing marks data
- `HomeworkSubmissionsTable` — existing homework submission data
- `MessagesTable` — existing message data
- `StudentsTable` — student class info for class average
- `AssessmentsTable` — assessment metadata (subject, max marks, date)

### Related Modules

- `server/.../feature/analytics/` — new analytics module
- `composeApp/.../ui/v2/screens/parent/` — parent analytics UI
- `composeApp/.../ui/v2/components/charts/` — new chart composables

---

## 2. Current System Assessment

### Existing Code

- All data exists: `AttendanceRecordsTable`, `AssessmentMarksTable`, `HomeworkSubmissionsTable`, `MessagesTable`
- `feature_audit.csv` L108: Parent Dashboard at 60% — basic data shown but no analytics/charts
- `DIFFERENTIATING_FEATURES.md` §10.1: Parent Analytics, effort M, data readiness: "All data exists"

### Existing Database

- `AttendanceRecordsTable` — daily attendance records (student_id, date, status)
- `AssessmentMarksTable` — marks per assessment (student_id, assessment_id, marks)
- `AssessmentsTable` — assessment metadata (subject_name, assessment_name, max_marks, date, status)
- `HomeworkSubmissionsTable` — homework submission records (student_id, homework_id, submitted, submitted_at)
- `MessagesTable` — message records (read status, timestamps)
- `StudentsTable` — student info (class_id for class average)

### Existing APIs

- Parent dashboard API — returns raw data (attendance %, latest marks)
- No analytics/trend API endpoints

### Existing UI

- Parent dashboard — shows current attendance %, latest marks, basic info
- No charts, trends, or analytics visualizations

### Existing Services

- `DashboardService` — returns raw dashboard data
- No analytics aggregation service

### Existing Documentation

- `DIFFERENTIATING_FEATURES.md` §10.1 — Parent Analytics
- `feature_audit.csv` L108 — Parent Dashboard at 60%

### Technical Debt

| # | Gap | Details |
|---|---|---|
| TD-1 | No analytics aggregation | No service to aggregate data into trends |
| TD-2 | No chart UI | No chart composables in the app |
| TD-3 | No class comparison | No anonymized class average calculation |
| TD-4 | No engagement score | No homework/message engagement metric |
| TD-5 | No growth indicators | No term-over-term comparison |
| TD-6 | No heat map | No calendar visualization for attendance |

### Gaps

| # | Gap | Impact | Severity |
|---|---|---|---|
| G1 | No trends | Parents see snapshots, not patterns | **High** |
| G2 | No class comparison | Parents don't know if child is above/below average | **High** |
| G3 | No engagement score | Parents don't know engagement level | **Medium** |
| G4 | No growth indicators | Parents don't see improving/declining subjects | **Medium** |
| G5 | No heat map | Attendance not visually intuitive | **Low** |

---

## 3. Functional Requirements

### FR-001
| Field | Value |
|---|---|
| **Title** | Attendance Trend Chart |
| **Description** | Attendance trend: monthly attendance % over academic year (line chart). |
| **Priority** | Critical |
| **User Roles** | Parent |
| **Acceptance notes** | Line chart with X-axis = month, Y-axis = attendance %. Filterable by term/year/all. |

### FR-002
| Field | Value |
|---|---|
| **Title** | Marks Trend Chart |
| **Description** | Marks trend: per-subject marks across terms (multi-line chart). |
| **Priority** | Critical |
| **User Roles** | Parent |
| **Acceptance notes** | Multi-line chart, one line per subject. X-axis = assessment date, Y-axis = marks percentage. |

### FR-003
| Field | Value |
|---|---|
| **Title** | Class Comparison |
| **Description** | Class comparison: child's marks vs class average (anonymized, no other student names). |
| **Priority** | High |
| **User Roles** | Parent |
| **Acceptance notes** | Bar chart comparing child's percentage vs class average percentage per subject. No individual student data. |

### FR-004
| Field | Value |
|---|---|
| **Title** | Engagement Score |
| **Description** | Engagement score: homework completion rate, message read rate, event participation. |
| **Priority** | Medium |
| **User Roles** | Parent |
| **Acceptance notes** | Composite score (0-100). Homework completion %, message read %, event participation %. |

### FR-005
| Field | Value |
|---|---|
| **Title** | Subject Radar Chart |
| **Description** | Subject performance radar chart (all subjects on one chart). |
| **Priority** | Medium |
| **User Roles** | Parent |
| **Acceptance notes** | Radar chart with one axis per subject. Shows current term performance percentage. |

### FR-006
| Field | Value |
|---|---|
| **Title** | Growth Indicators |
| **Description** | Growth indicators: ↑↓ per subject comparing current vs previous term. |
| **Priority** | Medium |
| **User Roles** | Parent |
| **Acceptance notes** | Green ↑ for improvement, red ↓ for decline. Shows percentage change. |

### FR-007
| Field | Value |
|---|---|
| **Title** | Attendance Heat Map |
| **Description** | Attendance heat map: calendar view with color-coded attendance. |
| **Priority** | Low |
| **User Roles** | Parent |
| **Acceptance notes** | Calendar grid, green = present, red = absent, gray = no school. Monthly view. |

### FR-008
| Field | Value |
|---|---|
| **Title** | Time Range Selector |
| **Description** | Time range selector: this term, this year, all years. |
| **Priority** | High |
| **User Roles** | Parent |
| **Acceptance notes** | Segmented control: Term | Year | All. Filters all charts and data. |

### Non-Functional Requirements

| ID | Requirement |
|---|---|
| NFR-1 | Analytics API responds in < 2 seconds per endpoint |
| NFR-2 | Charts render in < 500ms |
| NFR-3 | Class average calculation handles classes with 50+ students |
| NFR-4 | All charts work offline (cached data) |
| NFR-5 | No student names in class comparison (anonymized) |
| NFR-6 | Analytics data cached for 5 minutes |

---

## 4. User Stories

### Parent
- [ ] View my child's attendance trend over the year
- [ ] View my child's marks trend per subject across terms
- [ ] Compare my child's performance to class average (anonymized)
- [ ] See my child's engagement score (homework, messages, events)
- [ ] View subject performance radar chart
- [ ] See which subjects are improving and which are declining
- [ ] View attendance heat map for any month
- [ ] Switch between term, year, and all-years views

### System
- [ ] Aggregate attendance data into monthly trends
- [ ] Aggregate marks data into per-subject trends
- [ ] Calculate anonymized class averages
- [ ] Calculate engagement score from homework, messages, events
- [ ] Calculate growth indicators (term-over-term)
- [ ] Generate heat map data for calendar visualization

---

## 5. Business Rules

### BR-001
**Rule:** Class comparison is anonymized.
**Enforcement:** Only class average shown. No individual student names or marks. Query aggregates all students in class.

### BR-002
**Rule:** Only published assessments included in analytics.
**Enforcement:** `AssessmentsTable.status = 'PUBLISHED'` filter in all marks queries. Draft/pending assessments excluded.

### BR-003
**Rule:** Engagement score is composite (0-100).
**Enforcement:** `(homework_completion_rate * 0.4) + (message_read_rate * 0.3) + (event_participation_rate * 0.3)`. Each component 0-100.

### BR-004
**Rule:** Growth indicators compare current vs previous term.
**Enforcement:** For each subject, compare current term average % vs previous term average %. Green ↑ if improvement > 2%, red ↓ if decline > 2%, no indicator if within ±2%.

### BR-005
**Rule:** Time range presets only (no custom date range).
**Enforcement:** Term = current academic term, Year = current academic year, All = all available data. No custom date picker.

### BR-006
**Rule:** Parent can only view analytics for linked children.
**Enforcement:** `ChildAccessResolver` validates parent-child link. Parent can only access analytics for children in `parent_child_links`.

### BR-007
**Rule:** Attendance heat map shows one month at a time.
**Enforcement:** Parent selects month. API returns daily attendance status for that month. Default: current month.

### BR-008
**Rule:** Analytics data cached for 5 minutes.
**Enforcement:** Server-side cache per student per endpoint, 5-minute TTL. Reduces DB load for repeated requests.

---

## 6. Database Design

### 6.1 Entity Relationship Summary

No new tables — all data aggregated from existing tables via queries.

### 6.2 New Tables

N/A — no new tables. All analytics computed from existing data.

### 6.3 Modified Tables

N/A — no existing tables modified.

### 6.4 Indexes

Existing indexes sufficient:
- `attendance_records(student_id, date)` — for attendance trend queries
- `assessment_marks(student_id, assessment_id)` — for marks trend queries
- `assessments(subject_name, date, status)` — for published assessment filtering
- `homework_submissions(student_id, submitted)` — for engagement score
- `students(class_id)` — for class average queries

### 6.5 Constraints

N/A — no new constraints.

### 6.6 Foreign Keys

N/A — no new foreign keys.

### 6.7 Soft Delete Strategy

N/A — analytics reads existing data. No soft delete needed.

### 6.8 Audit Fields

N/A — no new audit fields.

### 6.9 Migration Notes

No migration needed — no schema changes. All data from existing tables.

### 6.10 Exposed Mappings

N/A — no new Exposed table objects.

### 6.11 Seed Data

N/A — analytics computed from existing student data.

---

## 7. State Machines

### Analytics Data State Machine

```
raw_data ──aggregation_query──> aggregated ──cached──> cached ──expired──> stale
  │                                │
  │                                │──served──> delivered
  └──no_data──> empty
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `raw_data` | Aggregation query executed | `aggregated` | Data exists for student |
| `raw_data` | Aggregation query executed | `empty` | No data for student |
| `aggregated` | Cache stored | `cached` | Cache write succeeds |
| `cached` | Cache hit (within TTL) | `delivered` | Cache not expired |
| `cached` | Cache expired (5 min) | `stale` | TTL exceeded |
| `stale` | New request | `raw_data` → re-aggregate | Re-query and re-cache |
| `empty` | None | (terminal) | Show "No data available" |

### Time Range Selection State Machine

```
term ──user_selects──> year ──user_selects──> all
  │                       │                       │
  └──user_selects──>      └──user_selects──>      └──user_selects──>
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `term` | User selects "Year" | `year` | UI toggle |
| `year` | User selects "All" | `all` | UI toggle |
| `all` | User selects "Term" | `term` | UI toggle |
| Any | User selects any | Selected state | UI toggle |

---

## 8. Backend Architecture

### 8.1 Component Overview

`ParentAnalyticsService` handles all analytics aggregation queries. No new tables — all data computed from existing tables via SQL aggregation. Results cached server-side for 5 minutes per student per endpoint.

### 8.2 Design Principles

1. **Read-only** — analytics is purely read-only; no writes to any table
2. **Aggregate on demand** — no pre-computed tables; SQL aggregation at query time
3. **Cache aggressively** — 5-minute TTL per student per endpoint reduces DB load
4. **Anonymize class data** — only class averages, never individual student data
5. **Parent-scoped** — parent can only access analytics for linked children

### 8.3 Core Types

#### ParentAnalyticsService

```kotlin
class ParentAnalyticsService {
    suspend fun getAttendanceTrend(studentId: UUID, range: TimeRange): List<AttendanceTrendPoint>
    suspend fun getMarksTrend(studentId: UUID, range: TimeRange): List<SubjectMarksTrend>
    suspend fun getClassComparison(studentId: UUID, term: String): List<SubjectComparison>
    suspend fun getEngagementScore(studentId: UUID): EngagementScore
    suspend fun getSubjectRadar(studentId: UUID, term: String): List<SubjectRadarPoint>
    suspend fun getGrowthIndicators(studentId: UUID): List<SubjectGrowth>
    suspend fun getAttendanceHeatMap(studentId: UUID, month: LocalDate): List<HeatMapDay>
}
```

### 8.4 Repositories

- `AnalyticsRepository` — raw SQL queries against existing tables (attendance, assessments, homework, messages)

### 8.5 Mappers

- `AnalyticsMapper` — maps SQL result rows to analytics DTOs

### 8.6 Permission Checks

- Parent-child link verification via `ChildAccessResolver`
- Parent can only access analytics for children in `parent_child_links`

### 8.7 Background Jobs

N/A — no background jobs. All analytics computed on-demand with caching.

### 8.8 Domain Events

N/A — analytics is read-only. No domain events emitted.

### 8.9 Caching

- Server-side cache: per student per endpoint, 5-minute TTL
- Client-side cache: per student per endpoint, 5-minute TTL (in DataStore)
- Cache key: `analytics:{studentId}:{endpoint}:{params}`

### 8.10 Transactions

N/A — all queries are read-only. No transactions needed.

### 8.11 Rate Limiting

- Analytics API: 60 requests per parent per minute (prevent abuse)

### 8.12 Configuration

- `ANALYTICS_CACHE_TTL_SECONDS` — default `300` (5 minutes)
- `ANALYTICS_RATE_LIMIT_PER_MINUTE` — default `60`
- `ANALYTICS_GROWTH_THRESHOLD_PERCENT` — default `2` (±2% = no indicator)

---

## 9. API Contracts

### 9.1 Analytics Endpoints

```
GET /api/v1/parent/analytics/{childId}/attendance-trend?range=term|year|all
  → 200: { points: [{ month: "2026-01", percentage: 95.2 }] }

GET /api/v1/parent/analytics/{childId}/marks-trend?range=term|year|all
  → 200: { subjects: [{ subject: "Math", points: [{ date: "2026-06-15", percentage: 85.0 }] }] }

GET /api/v1/parent/analytics/{childId}/class-comparison?term=term1
  → 200: { subjects: [{ subject: "Math", childPercentage: 85.0, classAverage: 78.5 }] }

GET /api/v1/parent/analytics/{childId}/engagement
  → 200: { score: 82.5, homeworkRate: 90.0, messageReadRate: 75.0, eventParticipationRate: 80.0 }

GET /api/v1/parent/analytics/{childId}/subject-radar?term=term1
  → 200: { subjects: [{ subject: "Math", percentage: 85.0 }] }

GET /api/v1/parent/analytics/{childId}/growth?term=term1
  → 200: { subjects: [{ subject: "Math", currentTerm: 85.0, previousTerm: 80.0, direction: "up", change: 5.0 }] }

GET /api/v1/parent/analytics/{childId}/attendance-heatmap?month=2026-06
  → 200: { days: [{ date: "2026-06-01", status: "present" }] }
```

### 9.2 DTO Models

All `@Serializable`, wrapped in `ApiResponse<T>` pattern.

```kotlin
@Serializable data class AttendanceTrendPoint(
    val month: String,       // "2026-01"
    val percentage: Double,  // 95.2
)

@Serializable data class SubjectMarksTrend(
    val subject: String,
    val points: List<MarksTrendPoint>,
)

@Serializable data class MarksTrendPoint(
    val date: String,
    val assessmentName: String,
    val percentage: Double,
)

@Serializable data class SubjectComparison(
    val subject: String,
    val childPercentage: Double,
    val classAverage: Double,
)

@Serializable data class EngagementScore(
    val score: Double,                  // composite 0-100
    val homeworkRate: Double,           // 0-100
    val messageReadRate: Double,        // 0-100
    val eventParticipationRate: Double, // 0-100
)

@Serializable data class SubjectRadarPoint(
    val subject: String,
    val percentage: Double,
)

@Serializable data class SubjectGrowth(
    val subject: String,
    val currentTerm: Double,
    val previousTerm: Double,
    val direction: String,  // "up" | "down" | "flat"
    val change: Double,     // percentage points
)

@Serializable data class HeatMapDay(
    val date: String,       // "2026-06-01"
    val status: String,     // "present" | "absent" | "no_school"
)

enum class TimeRange { TERM, YEAR, ALL }
```

---

## 10. Frontend Architecture

### 10.1 Screens

| Screen | Platform | Role | Description |
|---|---|---|---|
| `AnalyticsScreen` | Compose | Parent | Analytics dashboard with all charts and visualizations |

### 10.2 Navigation

- Parent: Home tab → Analytics → (scrollable dashboard with all charts)
- Time range selector at top (segmented control: Term | Year | All)

### 10.3 UX Flows

#### Parent: View Analytics
1. Parent opens Analytics tab
2. Selects child (if multiple children)
3. Sees attendance trend line chart at top
4. Scrolls down to marks trend multi-line chart
5. Scrolls to class comparison bar chart
6. Scrolls to engagement score card
7. Scrolls to subject radar chart
8. Scrolls to growth indicators list
9. Scrolls to attendance heat map (month selector)
10. Taps time range selector to switch between Term/Year/All

### 10.4 State Management

```kotlin
data class AnalyticsState(
    val selectedChildId: String?,
    val timeRange: TimeRange,
    val attendanceTrend: List<AttendanceTrendPoint>,
    val marksTrend: List<SubjectMarksTrend>,
    val classComparison: List<SubjectComparison>,
    val engagementScore: EngagementScore?,
    val subjectRadar: List<SubjectRadarPoint>,
    val growthIndicators: List<SubjectGrowth>,
    val heatMap: List<HeatMapDay>,
    val selectedHeatMapMonth: String,
    val isLoading: Boolean,
    val error: String?,
)
```

### 10.5 Chart Components

Charts using Compose Multiplatform charting library:
- **Line charts:** attendance trend, marks trend
- **Radar chart:** subject performance
- **Bar chart:** class comparison
- **Heat map:** custom composable for attendance calendar
- **Growth indicators:** ↑↓ arrows with color (green/red)

Recommended library: `compose-charts` or custom Canvas-based composables.

### 10.6 Offline Support

- Analytics data cached in DataStore (5-minute TTL)
- Charts render from cached data when offline
- "Data may be up to 5 minutes old" indicator when offline

### 10.7 Loading States

- Initial load: "Loading analytics..."
- Chart loading: shimmer placeholder for each chart
- Refresh: pull-to-refresh on dashboard

### 10.8 Error Handling (UI)

- No data: "No analytics data available yet."
- Load failure: "Failed to load analytics. Pull to refresh."
- No child linked: "No children linked to your account."

### 10.9 Component Integration Guidelines

| Rule | Description |
|---|---|
| **R1** | Time range selector as segmented control at top |
| **R2** | Line charts use different colors per subject |
| **R3** | Growth indicators: green ↑ for improvement, red ↓ for decline |
| **R4** | Heat map: green = present, red = absent, gray = no school |
| **R5** | Radar chart axes labeled with subject names |
| **R6** | Class comparison bars side-by-side (child vs average) |
| **R7** | Engagement score shown as circular progress (0-100) |
| **R8** | All charts scrollable in single dashboard view |
| **R9** | Charts use Canvas-based rendering for performance |
| **R10** | Child selector dropdown if parent has multiple children |

---

## 11. Shared Module Changes (KMP)

### 11.1 DTOs

All DTOs defined in section 9.2, placed in `shared/.../analytics/domain/model/AnalyticsModels.kt`.

### 11.2 Domain Models

```kotlin
data class AttendanceTrend(val points: List<AttendanceTrendPoint>)
data class MarksTrend(val subjects: List<SubjectMarksTrend>)
data class ClassComparison(val subjects: List<SubjectComparison>)
data class Engagement(val score: EngagementScore)
data class SubjectRadar(val subjects: List<SubjectRadarPoint>)
data class GrowthIndicators(val subjects: List<SubjectGrowth>)
data class AttendanceHeatMap(val days: List<HeatMapDay>)
```

### 11.3 Repository Interfaces

```kotlin
interface AnalyticsRepository {
    suspend fun getAttendanceTrend(token: String, childId: String, range: TimeRange): NetworkResult<List<AttendanceTrendPoint>>
    suspend fun getMarksTrend(token: String, childId: String, range: TimeRange): NetworkResult<List<SubjectMarksTrend>>
    suspend fun getClassComparison(token: String, childId: String, term: String): NetworkResult<List<SubjectComparison>>
    suspend fun getEngagementScore(token: String, childId: String): NetworkResult<EngagementScore>
    suspend fun getSubjectRadar(token: String, childId: String, term: String): NetworkResult<List<SubjectRadarPoint>>
    suspend fun getGrowthIndicators(token: String, childId: String, term: String): NetworkResult<List<SubjectGrowth>>
    suspend fun getAttendanceHeatMap(token: String, childId: String, month: String): NetworkResult<List<HeatMapDay>>
}
```

### 11.4 UseCases

- `GetAttendanceTrendUseCase`
- `GetMarksTrendUseCase`
- `GetClassComparisonUseCase`
- `GetEngagementScoreUseCase`
- `GetSubjectRadarUseCase`
- `GetGrowthIndicatorsUseCase`
- `GetAttendanceHeatMapUseCase`

### 11.5 Validation

- `childId`: valid UUID, must be linked to parent
- `range`: one of `term`, `year`, `all`
- `term`: valid term identifier (e.g., "term1", "term2")
- `month`: valid year-month format ("YYYY-MM")

### 11.6 Serialization

Standard Kotlinx serialization. `TimeRange` enum serialized as lowercase string.

### 11.7 Network APIs

Ktor `@Resource` route definitions in `AnalyticsApi.kt`:
- GET `/api/v1/parent/analytics/{childId}/attendance-trend`
- GET `/api/v1/parent/analytics/{childId}/marks-trend`
- GET `/api/v1/parent/analytics/{childId}/class-comparison`
- GET `/api/v1/parent/analytics/{childId}/engagement`
- GET `/api/v1/parent/analytics/{childId}/subject-radar`
- GET `/api/v1/parent/analytics/{childId}/growth`
- GET `/api/v1/parent/analytics/{childId}/attendance-heatmap`

### 11.8 Database Models (Local Cache)

- Analytics responses cached in DataStore as JSON (5-minute TTL)
- Cache key: `analytics:{childId}:{endpoint}:{params}`

---

## 12. Permissions Matrix

| Action | Super Admin | School Admin | Teacher | Parent |
|---|---|---|---|---|
| View own child analytics | N/A | N/A | N/A | ✅ |
| View class analytics | ✅ | ✅ | ✅ | ❌ |
| View school analytics | ✅ | ✅ | ❌ | ❌ |
| Export analytics | ✅ | ✅ | ❌ | ❌ |

---

## 13. Notifications

N/A — analytics is a passive dashboard. No notifications triggered by analytics views.

---

## 14. Background Jobs

N/A — all analytics computed on-demand with caching. No background jobs needed.

---

## 15. Integrations

### Internal Integrations

| System | Integration Point | Direction | Protocol | Error Handling |
|---|---|---|---|---|
| `AttendanceRecordsTable` | Attendance data | Read | Direct DB | Return empty if no data |
| `AssessmentMarksTable` | Marks data | Read | Direct DB | Return empty if no data |
| `AssessmentsTable` | Assessment metadata | Read | Direct DB | Return empty if no data |
| `HomeworkSubmissionsTable` | Homework data | Read | Direct DB | Return 0% if no data |
| `MessagesTable` | Message read data | Read | Direct DB | Return 0% if no data |
| `StudentsTable` | Student class info | Read | Direct DB | Return error if student not found |
| `ChildAccessResolver` | Parent-child link | Read | Direct call | Return 403 if not linked |

### External Integrations

N/A — no external integrations. All data from internal DB.

### Integration Patterns

- **Aggregation queries:** Direct SQL against existing tables. No joins with external systems.
- **Child access:** `ChildAccessResolver.resolve(parentId, childId)` validates parent-child link before serving analytics.

---

## 16. Security

### Authentication

- All analytics APIs: JWT auth via `requireAuth()`
- Parent role required

### Authorization

- Parent can only view analytics for linked children
- `ChildAccessResolver` validates parent-child link
- No cross-parent access

### Data Protection

- Class comparison is anonymized — only class average, no individual student data
- Analytics data — same sensitivity as existing dashboard data
- No PII in class comparison

### Input Validation

- `childId`: valid UUID, linked to parent
- `range`: one of `term`, `year`, `all`
- `term`: valid term identifier
- `month`: valid year-month format (YYYY-MM)

### Rate Limiting

- Analytics API: 60 requests per parent per minute

### Audit Logging

- Analytics view: parent ID, child ID, endpoint (logged for usage metrics)

### PII Handling

- No PII in class comparison (anonymized)
- Child's own data shown to parent (already authorized)
- No data sent to external services

### Multi-tenant Isolation

- All queries scoped by student's school_id (implicit via student → class → school)
- No cross-school analytics access

---

## 17. Performance & Scalability

### Expected Scale

- 1 parent views analytics for 1-3 children
- 7 analytics endpoints per child
- Aggregation queries over 1 academic year (~200 school days, ~20 assessments)
- Class average: 30-50 students per class

### Query Optimization

- Attendance trend: `GROUP BY month` with `student_id` filter — indexed
- Marks trend: `JOIN assessments` with `student_id` filter — indexed
- Class average: `GROUP BY subject_name` with `class_id` filter — indexed
- Engagement: count queries with `student_id` filter — indexed

### Indexing Strategy

Existing indexes sufficient:
- `attendance_records(student_id, date)` — attendance trend
- `assessment_marks(student_id, assessment_id)` — marks trend
- `assessments(status, subject_name)` — published assessments
- `homework_submissions(student_id, submitted)` — engagement
- `students(class_id)` — class average

### Caching Strategy

- Server-side: per student per endpoint, 5-minute TTL
- Client-side: per student per endpoint, 5-minute TTL (DataStore)
- Cache invalidation: time-based (TTL expiry)

### Pagination

N/A — analytics returns complete datasets (trends, comparisons). No pagination needed.

### Connection Pooling

Uses existing HikariCP connection pool. Read-only queries.

### Async Processing

- All analytics queries: synchronous (with caching)
- Chart rendering: client-side (Compose Canvas)

### Scalability Concerns

- Class average query: O(n) where n = students in class. With 50 students, negligible.
- Marks trend: O(m) where m = assessments per subject. With 20 assessments, negligible.
- Cache hit rate: high (parents view same data repeatedly within 5 minutes).

---

## 18. Edge Cases

| # | Scenario | Expected Behavior |
|---|---|---|
| EC-1 | No attendance records for student | Return empty list. UI shows "No attendance data." |
| EC-2 | No marks published for student | Return empty list. UI shows "No marks data." |
| EC-3 | No homework assigned | Engagement homework rate = 100% (no pending homework). |
| EC-4 | No messages sent to parent | Engagement message read rate = 100% (no messages to read). |
| EC-5 | Student has no class_id | Class comparison returns empty. UI shows "No class data." |
| EC-6 | Only one term of data | Growth indicators show "No previous term data." |
| EC-7 | Parent has no linked children | Return 403. UI shows "No children linked." |
| EC-8 | Parent tries to view another parent's child | Return 403 "Access denied." |
| EC-9 | Invalid time range | Return 400 "Invalid time range." |
| EC-10 | Invalid month format | Return 400 "Invalid month format. Use YYYY-MM." |
| EC-11 | Class has only 1 student (the child) | Class average = child's score. Show "No comparison available." |
| EC-12 | All assessments in draft (none published) | Marks trend empty. UI shows "No published results." |
| EC-13 | Future month selected for heat map | Return empty list. UI shows "No data for future dates." |
| EC-14 | Very old data (previous academic years) | Include in "All" range. May be slow — cache helps. |
| EC-15 | Student transferred schools mid-year | Only show data from current school. |
| EC-16 | Cache miss | Query DB, cache result, return. |
| EC-17 | Cache expired | Re-query DB, update cache, return. |
| EC-18 | DB query timeout | Return 500. UI shows "Failed to load. Pull to refresh." |

---

## 19. Error Handling

### Error Response Format

Standard `ApiResponse` error format.

### Error Codes

| Code | HTTP Status | Description | User Message |
|---|---|---|---|
| `CHILD_NOT_LINKED` | 403 | Parent does not have access to this child | "You do not have access to this child's data." |
| `CHILD_NOT_FOUND` | 404 | Child ID not found | "Child not found." |
| `INVALID_TIME_RANGE` | 400 | Time range not in term/year/all | "Invalid time range." |
| `INVALID_TERM` | 400 | Term identifier not valid | "Invalid term." |
| `INVALID_MONTH` | 400 | Month format invalid | "Invalid month format. Use YYYY-MM." |
| `ANALYTICS_UNAVAILABLE` | 500 | DB query error | "Failed to load analytics. Please try again." |

### Error Handling Strategy

- **No data:** Return empty list/score 0. UI shows "No data available."
- **DB errors:** Return 500. Log error. UI shows retry.
- **Permission errors:** Return 403. UI shows access denied.
- **Validation errors:** Return 400 with specific message.

### Retry Strategy

- Client retries on 500 errors (3 attempts with 2-second intervals)
- Pull-to-refresh on dashboard

### Fallback Behavior

- No attendance data: empty chart with "No data" label
- No marks data: empty chart with "No data" label
- No engagement data: score 0 with "No data" label
- DB unavailable: cached data shown (if available) with "Data may be stale" indicator

---

## 20. Analytics & Reporting

### Analytics Dashboard Data (Meta-Analytics)

| Metric | Source | Derivation |
|---|---|---|
| Analytics views per day | Audit logs | Count of analytics API calls |
| Most viewed chart | Audit logs | Count per endpoint |
| Average time on analytics | Client telemetry | Time from open to close |
| Cache hit rate | Server cache logs | Cache hits / total requests |

### Export Capabilities

- Future: PDF export of analytics dashboard (not in initial scope)

### Report Types

| Report | Format | Frequency | Recipient |
|---|---|---|---|
| Analytics usage | JSON (API) | On-demand | School Admin |
| Parent engagement with analytics | JSON (API) | Weekly | Dev Team |

---

## 21. Testing Strategy

### Unit Tests

- `ParentAnalyticsService.getAttendanceTrend()` — data aggregation, percentage calculation, empty data
- `ParentAnalyticsService.getMarksTrend()` — per-subject grouping, percentage calculation
- `ParentAnalyticsService.getClassComparison()` — anonymized class average, no individual data
- `ParentAnalyticsService.getEngagementScore()` — composite score calculation, component weights
- `ParentAnalyticsService.getGrowthIndicators()` — term comparison, direction (up/down/flat)
- `ParentAnalyticsService.getAttendanceHeatMap()` — daily status mapping
- `ChildAccessResolver` — parent-child link validation

### Integration Tests

- Full analytics flow: parent requests → child access verified → data aggregated → response
- Class average: verify no individual student data in response
- Cache: first request queries DB, second request hits cache
- Empty data: student with no attendance/marks → empty responses

### E2E Tests

- Parent opens analytics → sees attendance trend chart → switches to year range → chart updates
- Parent views class comparison → verifies no other student names visible
- Parent views growth indicators → sees ↑↓ arrows with correct colors

### Performance Tests

- Attendance trend query: < 500ms for 200 school days
- Class average query: < 500ms for 50 students
- All 7 endpoints: < 2 seconds total (parallel)
- Chart rendering: < 500ms per chart

### Test Data

- 3 students with varying attendance (high, medium, low)
- 5 subjects with 4 assessments each across 2 terms
- Homework submissions (complete, incomplete, none)
- Messages (read, unread)
- Student with no data (edge case)

### Test Environment

- Test database with attendance, assessment, homework, message tables
- Test parent-child links
- Test JWT tokens for parent role

---

## 22. Acceptance Criteria

- [ ] Attendance trend chart shows monthly attendance %
- [ ] Marks trend chart shows per-subject performance across terms
- [ ] Class comparison shows child vs anonymized class average
- [ ] Engagement score calculated (homework, messages, events)
- [ ] Subject radar chart shows all subjects
- [ ] Growth indicators show ↑↓ per subject
- [ ] Attendance heat map visualizes daily attendance
- [ ] Time range selector works (term/year/all)
- [ ] No individual student names in class comparison
- [ ] Analytics data cached for 5 minutes
- [ ] Parent can only view analytics for linked children
- [ ] Charts render in < 500ms

---

## 23. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 2 days | `ParentAnalyticsService` (aggregation queries, caching) |
| 2 | 1 day | API endpoints (7 endpoints) |
| 3 | 4 days | Client UI: `AnalyticsScreen`, chart composables (line, radar, bar, heat map, growth indicators) |
| 4 | 1 day | Tests: unit, integration, E2E |

### Pre-Implementation Checklist

- [ ] Verify `AttendanceRecordsTable` has sufficient test data
- [ ] Verify `AssessmentMarksTable` has published assessments
- [ ] Verify `HomeworkSubmissionsTable` has submission data
- [ ] Verify `ChildAccessResolver` works correctly
- [ ] Verify Compose charting library supports line, radar, bar charts
- [ ] Verify Canvas-based heat map rendering

---

## 24. File-Level Impact Analysis

### Server

| File | Change Type | Description |
|---|---|---|
| `server/.../feature/analytics/ParentAnalyticsService.kt` | **New** | Aggregation service with 7 methods |
| `server/.../feature/analytics/AnalyticsRepository.kt` | **New** | SQL aggregation queries |
| `server/.../feature/analytics/ParentAnalyticsRouting.kt` | **New** | API endpoints (7 routes) |
| `server/.../feature/analytics/AnalyticsCache.kt` | **New** | Server-side cache (5-min TTL) |

### Shared (KMP)

| File | Change Type | Description |
|---|---|---|
| `shared/.../analytics/domain/model/AnalyticsModels.kt` | **New** | DTOs, domain models, enums |
| `shared/.../analytics/domain/repository/AnalyticsRepository.kt` | **New** | Repository interface |
| `shared/.../analytics/data/remote/AnalyticsApi.kt` | **New** | HTTP API definitions |

### Client (Compose)

| File | Change Type | Description |
|---|---|---|
| `composeApp/.../ui/v2/screens/parent/AnalyticsScreen.kt` | **New** | Analytics dashboard screen |
| `composeApp/.../ui/v2/components/charts/LineChart.kt` | **New** | Line chart composable |
| `composeApp/.../ui/v2/components/charts/RadarChart.kt` | **New** | Radar chart composable |
| `composeApp/.../ui/v2/components/charts/BarChart.kt` | **New** | Bar chart composable |
| `composeApp/.../ui/v2/components/charts/HeatMap.kt` | **New** | Calendar heat map composable |
| `composeApp/.../ui/v2/components/charts/GrowthIndicator.kt` | **New** | Growth indicator (↑↓) composable |

---

## 25. Future Enhancements

| # | Enhancement | Priority | Effort | Notes |
|---|---|---|---|---|
| F-1 | PDF export of analytics dashboard | Medium | S | Generate PDF report for printing/sharing |
| F-2 | Predictive analytics (AI) | Medium | L | AI predictions for future performance |
| F-3 | Custom date ranges | Low | S | Custom date picker instead of presets |
| F-4 | Push notifications for declining performance | Medium | S | Alert parent when marks drop significantly |
| F-5 | Comparative analytics across terms | Low | M | Compare term-over-term across all subjects |
| F-6 | Subject-wise recommendations | Medium | M | AI-powered recommendations per subject |
| F-7 | Peer group comparison (anonymized) | Low | M | Compare to top 10% / bottom 10% of class |
| F-8 | Attendance prediction | Low | M | Predict attendance issues before they occur |
| F-9 | Multi-child comparison | Low | S | Compare siblings' performance |
| F-10 | Share analytics with tutor | Low | S | Share dashboard link with private tutor |

---

## Appendix A: Sequence Diagrams

### A.1 Parent Views Analytics

```
Parent (app)       Server              Cache              DB
  │                  │                    │                 │
  │  GET /analytics/{childId}/attendance-trend              │
  │  ?range=term     │                    │                 │
  │  ──────────────> │                    │                 │
  │                  │──check cache──────→│                 │
  │                  │←──cache miss───────│                 │
  │                  │──query attendance──────────────────→│
  │                  │←──attendance records────────────────│
  │                  │──aggregate by month                  │
  │                  │──store in cache───→│                 │
  │  ←──200: attendance trend points                        │
  │                  │                    │                 │
  │  ──render line chart──                                  │
  │                  │                    │                 │
  │  GET /analytics/{childId}/marks-trend                   │
  │  ?range=term     │                    │                 │
  │  ──────────────> │                    │                 │
  │                  │──check cache──────→│                 │
  │                  │←──cache hit────────│                 │
  │  ←──200: marks trend (from cache)                       │
  │                  │                    │                 │
```

### A.2 Class Comparison Flow

```
Parent (app)       Server              ChildAccessResolver   DB
  │                  │                    │                    │
  │  GET /analytics/{childId}/class-comparison?term=term1     │
  │  ──────────────> │                    │                    │
  │                  │──resolve(parentId, childId)──────────→│
  │                  │←──access granted──────────────────────│
  │                  │──get student's class_id──────────────→│
  │                  │←──class_id───────────────────────────│
  │                  │──query class average (anonymized)───→│
  │                  │←──class averages per subject─────────│
  │                  │──query child's marks────────────────→│
  │                  │←──child's marks per subject──────────│
  │                  │──compare child vs class average       │
  │  ←──200: comparison data (no student names)               │
  │                  │                    │                    │
```

---

## Appendix B: Domain Model / ER Diagram

```
No new tables. Analytics reads from existing tables:

┌────────────────────────┐     ┌────────────────────────┐
│  attendance_records     │     │  assessment_marks       │
│  (existing)             │     │  (existing)             │
│  student_id, date,      │     │  student_id,            │
│  status                 │     │  assessment_id, marks   │
└───────────┬────────────┘     └──────────┬─────────────┘
            │                              │
            │  aggregate                   │  join + aggregate
            ▼                              ▼
┌────────────────────────┐     ┌────────────────────────┐
│  AttendanceTrendPoint   │     │  SubjectMarksTrend      │
│  month, percentage      │     │  subject, points[]      │
└────────────────────────┘     └────────────────────────┘

┌────────────────────────┐     ┌────────────────────────┐
│  homework_submissions   │     │  messages               │
│  (existing)             │     │  (existing)             │
│  student_id, submitted  │     │  read status, timestamps│
└───────────┬────────────┘     └──────────┬─────────────┘
            │                              │
            │  count + calculate %         │  count + calculate %
            └──────────┬───────────────────┘
                       │
                       ▼
              ┌────────────────────┐
              │  EngagementScore    │
              │  score (0-100),     │
              │  homeworkRate,      │
              │  messageReadRate,   │
              │  eventRate          │
              └────────────────────┘

┌────────────────────────┐
│  students (existing)    │
│  id, class_id           │
└───────────┬────────────┘
            │
            │  join via class_id for class average
            ▼
┌────────────────────────┐
│  SubjectComparison      │
│  subject, childPct,     │
│  classAverage           │
│  (anonymized)           │
└────────────────────────┘
```

---

## Appendix C: Event Flow

### Domain Events

N/A — analytics is read-only. No domain events emitted.

### Aggregation Queries

```sql
-- Attendance trend (monthly)
SELECT
    DATE_TRUNC('month', date) AS month,
    COUNT(*) FILTER (WHERE status = 'PRESENT') AS present,
    COUNT(*) AS total,
    ROUND(COUNT(*) FILTER (WHERE status = 'PRESENT')::numeric / COUNT(*) * 100, 1) AS percentage
FROM attendance_records
WHERE student_id = :studentId AND date BETWEEN :start AND :end
GROUP BY month ORDER BY month;

-- Marks trend per subject
SELECT
    a.subject_name,
    a.assessment_name,
    am.marks,
    a.max_marks,
    a.date
FROM assessment_marks am
JOIN assessments a ON am.assessment_id = a.id
WHERE am.student_id = :studentId AND a.status = 'PUBLISHED'
ORDER BY a.subject_name, a.date;

-- Class average (anonymized)
SELECT
    a.subject_name,
    AVG(am.marks / a.max_marks * 100) AS class_avg_percentage
FROM assessment_marks am
JOIN assessments a ON am.assessment_id = a.id
JOIN students s ON am.student_id = s.id
WHERE s.class_id = :classId AND a.status = 'PUBLISHED'
GROUP BY a.subject_name;
```

### Event Delivery Guarantees

N/A — no events.

---

## Appendix D: Configuration

### Environment Variables

| Variable | Default | Description |
|---|---|---|
| `ANALYTICS_CACHE_TTL_SECONDS` | `300` | Server-side cache TTL in seconds (5 min) |
| `ANALYTICS_RATE_LIMIT_PER_MINUTE` | `60` | Max API requests per parent per minute |
| `ANALYTICS_GROWTH_THRESHOLD_PERCENT` | `2` | Percentage change threshold for growth indicators (±2%) |

### Feature Flags

| Flag | Default | Description |
|---|---|---|
| `PARENT_ANALYTICS_ENABLED` | `true` | Enable/disable parent analytics feature |
| `ANALYTICS_CLASS_COMPARISON_ENABLED` | `true` | Enable/disable class comparison chart |
| `ANALYTICS_ENGAGEMENT_SCORE_ENABLED` | `true` | Enable/disable engagement score |
| `ANALYTICS_HEAT_MAP_ENABLED` | `true` | Enable/disable attendance heat map |

### School-Level Settings

N/A — analytics available to all parents. No school-level configuration.

---

## Appendix E: Migration & Rollback

### Migration

N/A — no database migration needed. No new tables, no schema changes. All analytics computed from existing tables.

### Rollback

N/A — no schema changes to roll back. Feature can be disabled via `PARENT_ANALYTICS_ENABLED` feature flag.

### Migration Validation

N/A — no migration to validate.

---

## Appendix F: Observability

### Structured Logging

| Log Level | Event | Context Fields |
|---|---|---|
| INFO | Analytics viewed | `parentId, childId, endpoint, range, durationMs` |
| INFO | Cache hit | `childId, endpoint, cacheAge` |
| INFO | Cache miss | `childId, endpoint` |
| WARN | No data for student | `childId, endpoint` |
| WARN | Parent access denied | `parentId, childId` |
| ERROR | DB query failed | `childId, endpoint, error` |
| ERROR | Cache write failed | `childId, endpoint, error` |

### Metrics

| Metric | Type | Labels | Description |
|---|---|---|---|
| `analytics_requests_total` | Counter | `endpoint, range` | Total analytics API requests |
| `analytics_cache_hit_rate` | Gauge | `endpoint` | Cache hit percentage per endpoint |
| `analytics_query_duration` | Histogram | `endpoint` | DB query latency per endpoint |
| `analytics_response_duration` | Histogram | `endpoint` | Total response latency per endpoint |
| `analytics_no_data_count` | Counter | `endpoint` | Requests returning empty data |

### Health Checks

| Check | Endpoint | Description |
|---|---|---|
| Analytics service | `/health/analytics` | Verify analytics service and DB connectivity |

### Alerts

| Alert | Condition | Severity | Notification |
|---|---|---|---|
| Analytics query slow | `analytics_query_duration` > 2 seconds | Warning | Email to dev team |
| Cache hit rate low | Cache hit rate < 50% | Warning | Email to dev team (possible cache issue) |
| Analytics errors high | Error rate > 5% | Warning | Email to dev team |

### Dashboards

| Dashboard | Panels | Audience |
|---|---|---|
| Analytics Usage | Requests per day, most viewed charts, cache hit rate | Dev Team |
| Analytics Performance | Query duration, response duration, error rate | Dev Team |
| Parent Engagement | Analytics views per parent, time on dashboard | Product Team |

### Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Slow aggregation queries | Low | Medium | 5-minute cache. Indexed queries. |
| Class average leaks student data | Very Low | High | Anonymized by design. Only averages returned. No individual data. |
| Chart rendering performance | Low | Low | Canvas-based rendering. < 500ms target. |
| No data for new students | High | Low | Graceful "No data available" UI. |
| Cache staleness | Low | Low | 5-minute TTL. Acceptable for analytics. |
| Parent accesses unlinked child | Low | High | `ChildAccessResolver` validates every request. |
| Large class size (100+ students) | Low | Low | Class average query is O(n). 100 students still fast. |
