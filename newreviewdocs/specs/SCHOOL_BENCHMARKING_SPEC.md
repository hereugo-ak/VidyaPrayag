# School Benchmarking — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** `MULTI_BRANCH_SPEC.md`
> **Source:** `DIFFERENTIATING_FEATURES.md` §10.2
> **Template:** `_SPEC_TEMPLATE.md` v1 (25 mandatory + 6 optional sections)

---

## 1. Feature Overview

### What

Anonymized school benchmarking dashboard that compares a school's performance metrics (attendance, academic results, fee collection, engagement) against aggregated averages from similar schools (same board, similar size, same region).

### Why — Product Rationale

School admins operate in isolation — they don't know how their school compares to peers. Benchmarking provides context: "Is 92% attendance good?" becomes "92% vs peer average of 87% — you're above average." This drives data-driven improvement.

This is a **differentiating feature** (Priority P2, Phase 3, effort M, "Medium" value per `DIFFERENTIATING_FEATURES.md`). It's unique among Indian school ERPs.

### What Stands Out (Competitive Moat)

From `DIFFERENTIATING_FEATURES.md` §10.2:
> "School Benchmarking — anonymized comparison against peer schools. Data readiness: All metrics data exists."

No major Indian school ERP offers anonymized peer benchmarking. The key moat is **aggregated multi-school data** — only possible with a multi-tenant SaaS with many schools on the same platform.

### Goals

- Compare school metrics against peer group averages (anonymized)
- Peer groups: same board, similar enrollment size, same state/region
- Metrics: attendance rate, pass rate, fee collection rate, parent engagement, teacher retention
- Trend comparison over academic years
- Identify strengths and improvement areas
- All data anonymized — no individual school identifiable

### Non-goals

- [ ] Public leaderboard (privacy concerns — schools don't want to be ranked publicly)
- [ ] Real-time benchmarking (monthly batch is sufficient)
- [ ] Cross-board comparison (CBSE vs ICSE — not meaningful)
- [ ] Individual teacher benchmarking (school-level only)
- [ ] Student-level benchmarking (school-level only)
- [ ] Benchmarking against specific named schools (anonymized only)

### Dependencies

- `MULTI_BRANCH_SPEC.md` — multi-branch/multi-school support
- `AttendanceRecordsTable` — attendance data
- `AssessmentMarksTable` — academic performance data
- `FeeRecordsTable` — fee collection data
- `NotificationsTable` — parent engagement data (app opens)
- `SchoolsTable` — school metadata (board, city, state, enrollment)
- `UsersTable` — teacher count for teacher-student ratio
- `AcademicYearsTable` — academic year context

### Related Modules

- `server/.../feature/benchmark/` — new benchmarking module
- `composeApp/.../ui/v2/screens/admin/` — benchmark dashboard UI

---

## 2. Current System Assessment

### Existing Code

- All metrics data exists across tables: `AttendanceRecordsTable`, `AssessmentMarksTable`, `FeeRecordsTable`, `NotificationsTable`
- `SchoolsTable` has `board`, `city`, `state` for peer grouping
- `DIFFERENTIATING_FEATURES.md` §10.2: School Benchmarking, effort M, data readiness: "All metrics data exists"
- No benchmarking/aggregation system exists

### Existing Database

- `AttendanceRecordsTable` — daily attendance records (present/absent)
- `AssessmentMarksTable` — marks per student per assessment
- `FeeRecordsTable` — fee payment records
- `NotificationsTable` — notification delivery records (for engagement tracking)
- `SchoolsTable` — school metadata (board, city, state, enrollment count)
- `UsersTable` — user records (teachers, parents)
- `AcademicYearsTable` — academic year definitions
- No benchmarking tables exist

### Existing APIs

- School admin APIs (existing) — school management
- No benchmarking API exists

### Existing UI

- School admin dashboard (existing) — basic school info
- No benchmarking dashboard exists

### Existing Services

- No metrics calculation service
- No benchmark aggregation service
- No peer group matching logic

### Existing Documentation

- `DIFFERENTIATING_FEATURES.md` §10.2 — School Benchmarking
- `MULTI_BRANCH_SPEC.md` — multi-branch support

### Technical Debt

| # | Gap | Details |
|---|---|---|
| TD-1 | No metrics tables | No `school_metrics`, `benchmark_summaries` tables |
| TD-2 | No metrics calculator | No service to calculate school-level metrics from raw data |
| TD-3 | No peer group matching | No logic to group similar schools |
| TD-4 | No benchmark aggregation | No service to aggregate peer group statistics |
| TD-5 | No benchmarking UI | No dashboard for comparison display |
| TD-6 | No batch jobs | No monthly calculation jobs |

### Gaps

| # | Gap | Impact | Severity |
|---|---|---|---|
| G1 | No school-level metrics | Admins can't see aggregate performance | **High** |
| G2 | No peer comparison | No context for whether metrics are good/bad | **High** |
| G3 | No trend analysis | Can't see if school is improving/declining | **Medium** |
| G4 | No AI summary | No actionable insights from metrics | **Medium** |

---

## 3. Functional Requirements

### FR-001
| Field | Value |
|---|---|
| **Title** | Calculate School Metrics |
| **Description** | Calculate school metrics: attendance rate, pass rate, fee collection rate, parent engagement rate, teacher-student ratio. |
| **Priority** | High |
| **User Roles** | System |
| **Acceptance notes** | Monthly batch job calculates 5 metrics per school per academic year. Stored in `school_metrics` with UNIQUE(school_id, academic_year_id, metric_type). |

### FR-002
| Field | Value |
|---|---|
| **Title** | Peer Group Matching |
| **Description** | Peer group: same board + similar enrollment (±20%) + same state. |
| **Priority** | High |
| **User Roles** | System |
| **Acceptance notes** | Peer group determined by board, enrollment size (±20%), and state. Minimum 10 schools in peer group. If fewer, expand criteria (drop state, then size range). |

### FR-003
| Field | Value |
|---|---|
| **Title** | Anonymized Comparison |
| **Description** | Anonymized comparison: "Your attendance rate: 92% | Peer average: 87% | Top quartile: 95%". |
| **Priority** | High |
| **User Roles** | School Admin |
| **Acceptance notes** | Shows school's value, peer average, peer median, percentiles (P25, P75, P90), school's percentile rank, and quartile. No individual school data exposed. |

### FR-004
| Field | Value |
|---|---|
| **Title** | Percentile Ranking |
| **Description** | Percentile ranking: where does school stand among peers. |
| **Priority** | Medium |
| **User Roles** | School Admin |
| **Acceptance notes** | School's percentile rank calculated for each metric. Shown as "78th percentile" and quartile (Q1-Q4). |

### FR-005
| Field | Value |
|---|---|
| **Title** | Trend Comparison |
| **Description** | Trend: metrics over last 3 academic years. |
| **Priority** | Medium |
| **User Roles** | School Admin |
| **Acceptance notes** | Shows school's metric values for last 3 academic years. Line chart in UI. Helps identify improvement/decline trends. |

### FR-006
| Field | Value |
|---|---|
| **Title** | AI Summary |
| **Description** | Strengths/weaknesses: AI-generated summary of where school excels/lags. |
| **Priority** | Medium |
| **User Roles** | School Admin |
| **Acceptance notes** | AI generates natural language summary: "Your school performs above peer average in attendance and fee collection. Improvement needed in parent engagement." Uses existing `AiService`. |

### FR-007
| Field | Value |
|---|---|
| **Title** | Privacy & Anonymization |
| **Description** | Privacy: no school can see another school's individual data. |
| **Priority** | Critical |
| **User Roles** | System |
| **Acceptance notes** | Only aggregates (avg, median, percentiles) shown. School count shown but no names. Minimum peer group size: 10 schools. Metrics calculation server-side only. |

### Non-Functional Requirements

| ID | Requirement |
|---|---|
| NFR-1 | Metrics calculation batch job: < 30 minutes for 100 schools |
| NFR-2 | Benchmark aggregation: < 15 minutes for all peer groups |
| NFR-3 | Benchmark API response: < 2 seconds |
| NFR-4 | AI summary generation: < 10 seconds |
| NFR-5 | Minimum peer group size: 10 schools |
| NFR-6 | All metrics stored with 2 decimal places |

---

## 4. User Stories

### School Admin
- [ ] View my school's benchmark dashboard
- [ ] See how my school compares to peer schools on attendance, academics, fees, engagement
- [ ] See my school's percentile rank for each metric
- [ ] View trend over last 3 academic years
- [ ] Read AI-generated summary of strengths and weaknesses
- [ ] See peer group description (board, size range, state, peer count)

### Super Admin
- [ ] View benchmarking across all schools (aggregated)
- [ ] Configure peer group matching criteria
- [ ] Trigger manual metrics recalculation

### System
- [ ] Monthly: calculate metrics for all schools
- [ ] Monthly: aggregate peer group benchmarks
- [ ] Enforce minimum peer group size (10 schools)
- [ ] Expand peer group criteria if minimum not met

---

## 5. Business Rules

### BR-001
**Rule:** Peer group minimum size is 10 schools.
**Enforcement:** If peer group has fewer than 10 schools, expand criteria: first drop state (national same-board), then expand size range (±50%). If still < 10, use all same-board schools. If still < 10, use all schools.

### BR-002
**Rule:** No individual school data is exposed.
**Enforcement:** Only aggregate statistics (avg, median, percentiles) stored in `benchmark_summaries`. School count shown but no names. Raw `school_metrics` not exposed to other schools.

### BR-003
**Rule:** Metrics calculated monthly.
**Enforcement:** Monthly batch job calculates metrics for all schools for current academic year. `school_metrics` updated with latest values. Historical values preserved (UNIQUE on school_id + academic_year_id + metric_type).

### BR-004
**Rule:** Peer group defined by board + size + state.
**Enforcement:** `SchoolsTable.board` (same board), enrollment size (±20%), `SchoolsTable.state` (same state). Peer group stored as JSON in `benchmark_summaries.peer_group`.

### BR-005
**Rule:** AI summary uses anonymized data only.
**Enforcement:** AI summary generated from school's metrics and peer aggregates. No raw data or school names sent to LLM. Only metric values and comparisons.

### BR-006
**Rule:** Benchmarking is school admin-only.
**Enforcement:** Benchmark API requires school admin role. Teachers, parents, and students cannot access benchmarking data.

### BR-007
**Rule:** Metrics are per academic year.
**Enforcement:** `school_metrics` has `academic_year_id`. Each academic year has its own set of metrics. Trend analysis compares across academic years.

### BR-008
**Rule:** Teacher-student ratio is inverted for comparison.
**Enforcement:** Raw ratio is students/teachers (e.g., 25:1). For benchmarking, lower is better. Percentile calculated with this understanding.

---

## 6. Database Design

### 6.1 Entity Relationship Summary

Two new tables: `school_metrics` (per-school per-year metric values) and `benchmark_summaries` (aggregated peer group statistics).

### 6.2 New Tables

#### `school_metrics` table

```sql
CREATE TABLE school_metrics (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    academic_year_id UUID NOT NULL,
    metric_type     VARCHAR(32) NOT NULL,          -- attendance_rate | pass_rate | fee_collection_rate | parent_engagement | teacher_student_ratio
    metric_value    DOUBLE PRECISION NOT NULL,
    calculated_at   TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(school_id, academic_year_id, metric_type)
);
CREATE INDEX idx_school_metrics_year ON school_metrics(academic_year_id, metric_type);
```

#### `benchmark_summaries` table

```sql
CREATE TABLE benchmark_summaries (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    academic_year_id UUID NOT NULL,
    metric_type     VARCHAR(32) NOT NULL,
    peer_group      TEXT NOT NULL,                 -- JSON: {"board": "CBSE", "size_range": "500-1000", "state": "Maharashtra"}
    avg_value       DOUBLE PRECISION NOT NULL,
    median_value    DOUBLE PRECISION NOT NULL,
    p25_value       DOUBLE PRECISION NOT NULL,     -- 25th percentile
    p75_value       DOUBLE PRECISION NOT NULL,     -- 75th percentile
    p90_value       DOUBLE PRECISION NOT NULL,     -- 90th percentile
    school_count    INTEGER NOT NULL,
    calculated_at   TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(academic_year_id, metric_type, peer_group)
);
```

### 6.3 Modified Tables

N/A — no existing tables modified.

### 6.4 Indexes

- `school_metrics(academic_year_id, metric_type)` — benchmark aggregation lookup
- `school_metrics(school_id, academic_year_id, metric_type)` — UNIQUE, school metrics lookup
- `benchmark_summaries(academic_year_id, metric_type, peer_group)` — UNIQUE, benchmark lookup

### 6.5 Constraints

- `school_metrics.school_id` — NOT NULL
- `school_metrics.academic_year_id` — NOT NULL
- `school_metrics.metric_type` — NOT NULL, VARCHAR(32)
- `school_metrics.metric_value` — NOT NULL, DOUBLE PRECISION
- `school_metrics(school_id, academic_year_id, metric_type)` — UNIQUE
- `benchmark_summaries.academic_year_id` — NOT NULL
- `benchmark_summaries.metric_type` — NOT NULL
- `benchmark_summaries.peer_group` — NOT NULL (TEXT, JSON)
- `benchmark_summaries.school_count` — NOT NULL, ≥ 10 (enforced by application logic)
- `benchmark_summaries(academic_year_id, metric_type, peer_group)` — UNIQUE

### 6.6 Foreign Keys

- `school_metrics.school_id` → `schools.id` (implicit)
- `school_metrics.academic_year_id` → `academic_years.id` (implicit)
- `benchmark_summaries.academic_year_id` → `academic_years.id` (implicit)

### 6.7 Soft Delete Strategy

N/A — metrics are calculated values, not user-created records. Old metrics retained for trend analysis. No soft delete.

### 6.8 Audit Fields

- `school_metrics.calculated_at` — when metric was last calculated
- `benchmark_summaries.calculated_at` — when benchmark was last aggregated

### 6.9 Migration Notes

Migration: `docs/db/migration_079_school_benchmarking.sql`
- CREATE 2 tables: `school_metrics`, `benchmark_summaries`
- No data migration (new feature)
- Indexes and UNIQUE constraints created in same migration

### 6.10 Exposed Mappings

```kotlin
object SchoolMetricsTable : UUIDTable("school_metrics", "id") {
    val schoolId       = uuid("school_id")
    val academicYearId = uuid("academic_year_id")
    val metricType     = varchar("metric_type", 32)
    val metricValue    = double("metric_value")
    val calculatedAt   = timestamp("calculated_at")

    init {
        uniqueIndex("idx_school_metrics_unique", schoolId, academicYearId, metricType)
    }
}

object BenchmarkSummariesTable : UUIDTable("benchmark_summaries", "id") {
    val academicYearId = uuid("academic_year_id")
    val metricType     = varchar("metric_type", 32)
    val peerGroup      = text("peer_group")  // JSON
    val avgValue       = double("avg_value")
    val medianValue    = double("median_value")
    val p25Value       = double("p25_value")
    val p75Value       = double("p75_value")
    val p90Value       = double("p90_value")
    val schoolCount    = integer("school_count")
    val calculatedAt   = timestamp("calculated_at")

    init {
        uniqueIndex("idx_benchmark_summaries_unique", academicYearId, metricType, peerGroup)
    }
}
```

Register both in `DatabaseFactory.allTables`.

### 6.11 Seed Data

N/A — metrics and benchmarks calculated by batch jobs. No seed data needed.

---

## 7. State Machines

### Metrics Calculation State Machine

```
not_calculated ──batch_job_runs──> calculating ──success──> calculated
  │                                    │
  │                                    │──error──> error
  │                                    │
  │                                    └──no_data──> skipped
  │
  └──already_calculated──> updating ──success──> calculated
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `not_calculated` | Monthly batch job | `calculating` | School has data for academic year |
| `not_calculated` | No data for academic year | `skipped` | No attendance/marks/fee data |
| `calculating` | Metrics calculated | `calculated` | All 5 metrics computed |
| `calculating` | Calculation error | `error` | DB query or computation fails |
| `calculated` | Next month's batch job | `updating` | Already has metrics for this year |
| `updating` | Metrics updated | `calculated` | Updated with latest values |
| `error` | Next month's batch job | `calculating` | Retry |
| `skipped` | Data becomes available | `calculating` | Next batch job detects data |

### Benchmark Aggregation State Machine

```
not_aggregated ──batch_job_runs──> aggregating ──peer_group_≥_10──> aggregated
  │                                    │
  │                                    │──peer_group_<_10──> expanding
  │                                    │
  │                                    └──error──> error
  │
  └──expanding ──criteria_relaxed──> aggregating
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `not_aggregated` | Monthly batch job | `aggregating` | Start aggregation |
| `aggregating` | Peer group ≥ 10 schools | `aggregated` | Sufficient peer group |
| `aggregating` | Peer group < 10 schools | `expanding` | Need to relax criteria |
| `expanding` | Criteria relaxed (drop state) | `aggregating` | Retry with wider criteria |
| `expanding` | Criteria relaxed (expand size) | `aggregating` | Retry with wider criteria |
| `aggregating` | Error | `error` | DB or computation fails |
| `aggregated` | Next month's batch job | `aggregating` | Update with latest data |
| `error` | Next month's batch job | `aggregating` | Retry |

### Benchmark View State Machine

```
no_data ──admin_opens──> loading ──data_available──> displayed
  │                         │
  │                         │──no_metrics──> empty
  │                         │
  │                         │──error──> error
  │
  └──data_available──> loading ──success──> displayed
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `no_data` | Admin opens benchmark | `loading` | API call |
| `loading` | Data available | `displayed` | Metrics and benchmarks exist |
| `loading` | No metrics for school | `empty` | School has no calculated metrics |
| `loading` | API error | `error` | Server error |
| `displayed` | Admin refreshes | `loading` | New API call |
| `empty` | Metrics calculated (next month) | `loading` | Admin reopens after batch job |

---

## 8. Backend Architecture

### 8.1 Component Overview

`MetricsCalculatorService` calculates per-school metrics from raw data (attendance, marks, fees, engagement). `BenchmarkService` handles peer group matching and anonymized comparison. `BenchmarkJob` runs monthly to calculate metrics and aggregate benchmarks. AI summary uses existing `AiService`.

### 8.2 Design Principles

1. **Anonymized first** — no individual school data ever exposed
2. **Monthly batch** — metrics calculated monthly, not real-time
3. **Peer group minimum** — 10 schools minimum, expand criteria if needed
4. **Server-side calculation** — all metrics computed server-side, no client-side aggregation
5. **Historical preservation** — metrics per academic year, trend across years
6. **AI-assisted insights** — natural language summary of strengths/weaknesses

### 8.3 Core Types

#### MetricsCalculatorService

```kotlin
class MetricsCalculatorService {
    suspend fun calculateSchoolMetrics(schoolId: UUID, academicYearId: UUID): List<SchoolMetricDto> {
        // Attendance rate: present_days / total_days * 100
        // Pass rate: students with >= 40% in all subjects / total students * 100
        // Fee collection: paid_amount / total_due * 100
        // Parent engagement: parents who opened app in last 30 days / total parents * 100
        // Teacher-student ratio: total_students / total_teachers
    }

    suspend fun calculateAllSchools(): Int  // batch job, monthly

    suspend fun getBenchmark(schoolId: UUID, academicYearId: UUID): BenchmarkDto {
        // 1. Get school's metrics
        // 2. Determine peer group (board + size + state)
        // 3. Fetch benchmark_summaries for peer group
        // 4. Calculate school's percentile rank
        // 5. Return comparison
    }
}
```

#### BenchmarkService

```kotlin
class BenchmarkService(
    private val metricsCalculator: MetricsCalculatorService,
    private val aiService: AiService,
) {
    suspend fun getBenchmark(schoolId: UUID, academicYearId: UUID): BenchmarkDto
    suspend fun aggregatePeerGroups(academicYearId: UUID): Int  // batch job
    suspend fun generateAiSummary(schoolMetrics: List<SchoolMetricDto>, peerBenchmarks: List<BenchmarkSummary>): String
    suspend fun getTrend(schoolId: UUID, metricType: String): List<TrendPoint>
}
```

### 8.4 Repositories

- `SchoolMetricsRepository` — CRUD for `school_metrics`
- `BenchmarkSummariesRepository` — CRUD for `benchmark_summaries`

### 8.5 Mappers

- `SchoolMetricMapper` — maps `school_metrics` rows to `SchoolMetricDto`
- `BenchmarkSummaryMapper` — maps `benchmark_summaries` rows to `BenchmarkSummaryDto`

### 8.6 Permission Checks

- Benchmark API: school admin role + JWT auth via `requireAuth()`
- Super admin: can view all schools' benchmarks and configure criteria
- Teachers, parents, students: no access

### 8.7 Background Jobs

- **Metrics Calculation Job** — monthly (1st of month, 3 AM IST)
  1. For each school, calculate 5 metrics for current academic year
  2. Upsert into `school_metrics` (UNIQUE constraint handles updates)
  3. Return count of schools processed

- **Benchmark Aggregation Job** — monthly (1st of month, 4 AM IST, after metrics job)
  1. For each metric type, group schools by peer group (board + size + state)
  2. Calculate avg, median, P25, P75, P90 for each peer group
  3. If peer group < 10 schools, expand criteria (drop state, then expand size)
  4. Upsert into `benchmark_summaries`
  5. Return count of peer groups aggregated

### 8.8 Domain Events

- `SchoolMetricsCalculated` — emitted when school's metrics calculated (includes school_id, year, metric count)
- `BenchmarkAggregated` — emitted when peer group benchmark aggregated (includes peer_group, school_count)
- `BenchmarkViewed` — emitted when admin views benchmark dashboard

### 8.9 Caching

- Benchmark results: cached per school per academic year, 1-hour TTL
- AI summary: cached per school per academic year, 24-hour TTL
- Peer group definitions: cached globally, 1-hour TTL

### 8.10 Transactions

- Metrics calculation: single transaction per school (upsert 5 metrics)
- Benchmark aggregation: single transaction per peer group (upsert benchmark summary)
- Benchmark view: no transaction (read-only)

### 8.11 Rate Limiting

N/A — benchmarking is read-only for admins. Batch jobs run once monthly.

### 8.12 Configuration

- `BENCHMARKING_ENABLED` — default `true`; enable/disable feature
- `BENCHMARKING_MIN_PEER_GROUP` — default `10`; minimum peer group size
- `BENCHMARKING_SIZE_RANGE_PERCENT` — default `20`; enrollment size range (±20%)
- `BENCHMARKING_AI_SUMMARY_ENABLED` — default `true`; enable/disable AI summary
- `BENCHMARKING_TREND_YEARS` — default `3`; number of years for trend

### 8.13 Peer Group Matching

```kotlin
fun determinePeerGroup(school: School): PeerGroup {
    // Level 1: same board + ±20% enrollment + same state
    val level1 = schools.filter { 
        it.board == school.board && 
        abs(it.enrollment - school.enrollment) <= school.enrollment * 0.2 &&
        it.state == school.state
    }
    if (level1.size >= 10) return PeerGroup(level1, "board+size+state")

    // Level 2: same board + ±20% enrollment (drop state)
    val level2 = schools.filter {
        it.board == school.board &&
        abs(it.enrollment - school.enrollment) <= school.enrollment * 0.2
    }
    if (level2.size >= 10) return PeerGroup(level2, "board+size")

    // Level 3: same board + ±50% enrollment
    val level3 = schools.filter {
        it.board == school.board &&
        abs(it.enrollment - school.enrollment) <= school.enrollment * 0.5
    }
    if (level3.size >= 10) return PeerGroup(level3, "board+wide_size")

    // Level 4: same board (all)
    val level4 = schools.filter { it.board == school.board }
    if (level4.size >= 10) return PeerGroup(level4, "board_only")

    // Level 5: all schools
    return PeerGroup(schools, "all")
}
```

---

## 9. API Contracts

### 9.1 Admin Endpoints

```
GET /api/v1/school/benchmark?academic_year_id={uuid}
  → 200: BenchmarkDto
  → 404: No metrics found for school/academic year
```

### 9.2 DTO Models

All `@Serializable`, wrapped in `ApiResponse<T>` pattern.

```kotlin
@Serializable data class BenchmarkDto(
    val peerGroup: PeerGroupDto,
    val metrics: List<MetricComparisonDto>,
    val aiSummary: String?,
)

@Serializable data class PeerGroupDto(
    val board: String,
    val sizeRange: String,       // "500-1000"
    val state: String?,          // null if expanded
    val peerCount: Int,
    val matchLevel: String,      // "board+size+state" | "board+size" | "board+wide_size" | "board_only" | "all"
)

@Serializable data class MetricComparisonDto(
    val metric: String,          // attendance_rate | pass_rate | fee_collection_rate | parent_engagement | teacher_student_ratio
    val schoolValue: Double,
    val peerAvg: Double,
    val peerMedian: Double,
    val p25: Double,
    val p75: Double,
    val p90: Double,
    val percentile: Int,         // school's percentile rank
    val quartile: String,        // Q1 | Q2 | Q3 | Q4
    val trend: List<TrendPointDto>,
)

@Serializable data class TrendPointDto(
    val academicYear: String,    // "2024-25"
    val value: Double,
)

@Serializable data class SchoolMetricDto(
    val metricType: String,
    val metricValue: Double,
    val academicYearId: String,
    val calculatedAt: String,
)

enum class MetricType { 
    ATTENDANCE_RATE, PASS_RATE, FEE_COLLECTION_RATE, 
    PARENT_ENGAGEMENT, TEACHER_STUDENT_RATIO 
}
enum class Quartile { Q1, Q2, Q3, Q4 }
```

### 9.3 Response Example

```json
{
  "success": true,
  "data": {
    "peer_group": {"board": "CBSE", "size_range": "500-1000", "state": "Maharashtra", "peer_count": 47, "match_level": "board+size+state"},
    "metrics": [
      {
        "metric": "attendance_rate",
        "school_value": 92.5,
        "peer_avg": 87.3,
        "peer_median": 88.0,
        "p25": 82.0,
        "p75": 91.0,
        "p90": 95.0,
        "percentile": 78,
        "quartile": "Q3",
        "trend": [
          {"academic_year": "2024-25", "value": 89.0},
          {"academic_year": "2025-26", "value": 91.0},
          {"academic_year": "2026-27", "value": 92.5}
        ]
      }
    ],
    "ai_summary": "Your school performs above peer average in attendance (92.5% vs 87.3%) and fee collection. Improvement needed in parent engagement (45% vs peer avg 62%)."
  }
}
```

---

## 10. Frontend Architecture

### 10.1 Screens

| Screen | Platform | Role | Description |
|---|---|---|---|
| `BenchmarkScreen` | Compose | School Admin | Benchmark dashboard with metric comparisons |
| `BenchmarkDetailScreen` | Compose | School Admin | Detailed view of single metric with trend chart |

### 10.2 Navigation

- Admin: Dashboard → Benchmarking → Overview
- Admin: Benchmarking → Metric Detail → Trend chart

### 10.3 UX Flows

#### Admin: View Benchmark Dashboard
1. Admin opens Benchmarking
2. Sees peer group description (board, size range, state, peer count)
3. Sees 5 metric cards: attendance, pass rate, fee collection, engagement, teacher-student ratio
4. Each card shows: school value, peer average, percentile, quartile badge
5. Color coding: green (above peer avg), yellow (near peer avg), red (below peer avg)
6. AI summary at top: natural language strengths/weaknesses
7. Tap metric → detail view with trend chart (last 3 years)

### 10.4 State Management

```kotlin
data class BenchmarkState(
    val peerGroup: PeerGroupDto?,
    val metrics: List<MetricComparisonDto>,
    val aiSummary: String?,
    val isLoading: Boolean,
    val error: String?,
)

data class BenchmarkDetailState(
    val metric: MetricComparisonDto?,
    val isLoading: Boolean,
    val error: String?,
)
```

### 10.5 Offline Support

- Benchmark data: cached locally (last fetched, 1-hour TTL)
- Trend data: cached locally (last fetched)

### 10.6 Loading States

- Dashboard: "Loading benchmark data..."
- AI summary: "Generating insights..." (if not cached)
- Detail: "Loading metric details..."

### 10.7 Error Handling (UI)

- No metrics: "Benchmarking data not available yet. Metrics are calculated monthly."
- No peer group: "Not enough peer schools for comparison. Please check back later."
- Network error: "Connection issue. Please check your internet."

### 10.8 Component Integration Guidelines

| Rule | Description |
|---|---|
| **R1** | Peer group description shown at top (board, size range, state, peer count) |
| **R2** | 5 metric cards in grid layout (2 columns) |
| **R3** | Each card: metric name, school value (large), peer avg (small), percentile badge |
| **R4** | Color: green if school > peer avg, yellow if within ±2%, red if below |
| **R5** | Quartile badge: Q1 (red), Q2 (yellow), Q3 (light green), Q4 (green) |
| **R6** | AI summary in highlighted card at top |
| **R7** | Tap card → detail screen with line chart (trend over 3 years) |
| **R8** | Trend chart: x-axis = academic years, y-axis = metric value |
| **R9** | "Last updated: {date}" shown at bottom |
| **R10** | No school names or individual peer data shown anywhere |

---

## 11. Shared Module Changes (KMP)

### 11.1 DTOs

All DTOs defined in section 9.2, placed in `shared/.../benchmark/domain/model/BenchmarkModels.kt`.

### 11.2 Domain Models

```kotlin
data class Benchmark(
    val peerGroup: PeerGroup,
    val metrics: List<MetricComparison>,
    val aiSummary: String?,
)

data class PeerGroup(
    val board: String,
    val sizeRange: String,
    val state: String?,
    val peerCount: Int,
    val matchLevel: String,
)

data class MetricComparison(
    val metric: MetricType,
    val schoolValue: Double,
    val peerAvg: Double,
    val peerMedian: Double,
    val p25: Double,
    val p75: Double,
    val p90: Double,
    val percentile: Int,
    val quartile: Quartile,
    val trend: List<TrendPoint>,
)

data class TrendPoint(
    val academicYear: String,
    val value: Double,
)
```

### 11.3 Repository Interfaces

```kotlin
interface BenchmarkRepository {
    suspend fun getBenchmark(token: String, academicYearId: String): NetworkResult<BenchmarkDto>
}
```

### 11.4 UseCases

- `GetBenchmarkUseCase`

### 11.5 Validation

- `academic_year_id`: valid UUID, exists in `AcademicYearsTable`

### 11.6 Serialization

Standard Kotlinx serialization. Enums serialized as lowercase strings.

### 11.7 Network APIs

Ktor `@Resource` route definitions in `BenchmarkApi.kt`:
- GET `/api/v1/school/benchmark?academic_year_id={uuid}`

### 11.8 Database Models (Local Cache)

- Benchmark data cached in local DB (last fetched, 1-hour TTL)

---

## 12. Permissions Matrix

| Action | Super Admin | School Admin | Teacher | Parent |
|---|---|---|---|---|
| View own school benchmark | N/A | ✅ | ❌ | ❌ |
| View all schools' benchmarks | ✅ | ❌ | ❌ | ❌ |
| Configure peer group criteria | ✅ | ❌ | ❌ | ❌ |
| Trigger manual recalculation | ✅ | ❌ | ❌ | ❌ |

---

## 13. Notifications

N/A — benchmarking is a dashboard feature. No notifications triggered by benchmarking.

---

## 14. Background Jobs

### Metrics Calculation Job

| Property | Value |
|---|---|
| **Name** | `MetricsCalculationJob` |
| **Schedule** | Monthly (1st of month, 3 AM IST) |
| **Duration** | < 30 minutes for 100 schools |
| **Retry** | None (next month) |

#### Job Flow

1. Get all active schools
2. For each school, calculate 5 metrics for current academic year
3. Upsert into `school_metrics` (UNIQUE constraint handles updates)
4. Return count of schools processed

### Benchmark Aggregation Job

| Property | Value |
|---|---|
| **Name** | `BenchmarkAggregationJob` |
| **Schedule** | Monthly (1st of month, 4 AM IST, after metrics job) |
| **Duration** | < 15 minutes for all peer groups |
| **Retry** | None (next month) |

#### Job Flow

1. For each metric type, group schools by peer group
2. For each peer group, calculate avg, median, P25, P75, P90
3. If peer group < 10, expand criteria (drop state, then expand size)
4. Upsert into `benchmark_summaries`
5. Return count of peer groups aggregated

---

## 15. Integrations

### Internal Integrations

| System | Integration Point | Direction | Protocol | Error Handling |
|---|---|---|---|---|
| `AttendanceRecordsTable` | Attendance data | Read | Direct DB | Skip metric if no data |
| `AssessmentMarksTable` | Academic performance | Read | Direct DB | Skip metric if no data |
| `FeeRecordsTable` | Fee collection data | Read | Direct DB | Skip metric if no data |
| `NotificationsTable` | Parent engagement | Read | Direct DB | Skip metric if no data |
| `SchoolsTable` | School metadata | Read | Direct DB | Skip school if missing |
| `UsersTable` | Teacher/parent count | Read | Direct DB | Skip metric if no data |
| `AcademicYearsTable` | Academic year context | Read | Direct DB | Required |
| `AiService` (`AI_INFRASTRUCTURE_SPEC.md`) | AI summary generation | Call | Direct call | Return null on failure |

### External Integrations

N/A — no external integrations. All data is internal.

### Integration Patterns

- **Metrics calculation:** Direct DB queries to aggregate raw data into school-level metrics
- **Peer group matching:** In-memory grouping using `SchoolsTable` metadata
- **AI summary:** `aiService.complete(null, null, "benchmark", "benchmark_v1", context)` — uses existing AI infrastructure
- **Benchmark view:** Read from `school_metrics` + `benchmark_summaries`, join in service layer

---

## 16. Security

### Authentication

- Benchmark API: JWT auth via `requireAuth()`, school admin role

### Authorization

- School admin can only view own school's benchmark
- Super admin can view all schools and configure criteria
- No teacher, parent, or student access

### Data Protection

- **Anonymization is critical** — no individual school data ever exposed
- Only aggregate statistics (avg, median, percentiles) stored in `benchmark_summaries`
- School count shown but no names
- Raw `school_metrics` not exposed to other schools
- AI summary uses anonymized data only (no school names sent to LLM)

### Input Validation

- `academic_year_id`: valid UUID, exists in `AcademicYearsTable`

### Rate Limiting

N/A — benchmarking is read-only, accessed once per admin session.

### Audit Logging

- Benchmark viewed: admin ID, school ID, academic year
- Metrics calculated: school ID, academic year, metric count (batch job log)
- Benchmarks aggregated: peer group, school count (batch job log)
- AI summary generated: school ID, academic year

### PII Handling

- No PII in benchmarking data — all metrics are aggregate school-level values
- No student, teacher, or parent individual data in metrics
- AI summary: only metric values and comparisons sent to LLM, no school names

### Multi-tenant Isolation

- `school_metrics.school_id` — school-scoped
- Benchmark API: filtered by admin's school_id
- `benchmark_summaries` — aggregate, no school_id (peer group level)
- No cross-school benchmark data access (only own school's metrics + peer aggregates)

---

## 17. Performance & Scalability

### Expected Scale

- 100 schools on platform
- 5 metrics per school per academic year = 500 metric records per year
- 10-20 peer groups × 5 metrics = 50-100 benchmark summaries per year
- Monthly batch: 100 schools × 5 metrics = 500 calculations

### Query Optimization

- School metrics: `idx_school_metrics_unique(school_id, academic_year_id, metric_type)` — UNIQUE, O(1) lookup
- Benchmark aggregation: `idx_school_metrics_year(academic_year_id, metric_type)` — group by year + metric
- Benchmark lookup: `idx_benchmark_summaries_unique(academic_year_id, metric_type, peer_group)` — UNIQUE

### Indexing Strategy

- `school_metrics(school_id, academic_year_id, metric_type)` — UNIQUE, school metrics lookup
- `school_metrics(academic_year_id, metric_type)` — benchmark aggregation
- `benchmark_summaries(academic_year_id, metric_type, peer_group)` — UNIQUE, benchmark lookup

### Caching Strategy

- Benchmark results: cached per school per academic year, 1-hour TTL
- AI summary: cached per school per academic year, 24-hour TTL
- Peer group definitions: cached globally, 1-hour TTL

### Pagination

N/A — benchmark returns all 5 metrics in single response.

### Connection Pooling

Uses existing HikariCP connection pool. No additional pooling needed.

### Async Processing

- Metrics calculation: background job (async)
- Benchmark aggregation: background job (async)
- AI summary: synchronous (with timeout)
- Benchmark view: synchronous (read from cache or DB)

### Scalability Concerns

- 100 schools × 5 metrics = 500 records/year. Negligible.
- Peer group aggregation: 100 schools grouped into 10-20 peer groups. Negligible.
- AI summary: 1 LLM call per school per view (cached for 24 hours). ~100 calls/day max. Manageable.
- As platform grows to 1,000 schools: metrics calculation may take longer. Batch job can be parallelized.

---

## 18. Edge Cases

| # | Scenario | Expected Behavior |
|---|---|---|
| EC-1 | School has no attendance data for academic year | Skip attendance metric. Log warning. Continue with other metrics. |
| EC-2 | School has no assessment marks for academic year | Skip pass rate metric. Log warning. |
| EC-3 | Peer group has fewer than 10 schools | Expand criteria: drop state, then expand size range, then board-only, then all. |
| EC-4 | Only 1 school on platform | No peer comparison. Return school's metrics without benchmark. |
| EC-5 | School has no board specified | Use "unknown" board. May result in wider peer group. |
| EC-6 | School has no state specified | Drop state criteria. Use board + size only. |
| EC-7 | AI summary generation fails | Return null for ai_summary. Benchmark still displayed. |
| EC-8 | School is new (no historical data) | No trend data. Show current year only. |
| EC-9 | Academic year not found | Return 404 "Academic year not found." |
| EC-10 | School has no metrics calculated yet | Return 404 "Benchmarking data not available yet." |
| EC-11 | Teacher-student ratio: 0 teachers | Skip metric. Can't divide by zero. Log warning. |
| EC-12 | Fee collection: no fees due | Skip metric. Can't calculate rate. Log warning. |
| EC-13 | Parent engagement: no parents registered | Engagement = 0%. Valid but flagged. |
| EC-14 | Multiple academic years with same metrics | UNIQUE constraint handles upsert. Latest calculation wins. |
| EC-15 | School changes board mid-year | Use current board for peer group. Historical metrics remain. |

---

## 19. Error Handling

### Error Response Format

Standard `ApiResponse` error format.

### Error Codes

| Code | HTTP Status | Description | User Message |
|---|---|---|---|
| `METRICS_NOT_FOUND` | 404 | No metrics calculated for school/academic year | "Benchmarking data not available yet. Metrics are calculated monthly." |
| `ACADEMIC_YEAR_NOT_FOUND` | 404 | Academic year ID not found | "Academic year not found." |
| `AI_SUMMARY_FAILED` | 200 | AI summary generation failed | (Not an error — ai_summary is null, benchmark still returned) |
| `PEER_GROUP_TOO_SMALL` | 200 | Peer group < 10 after all expansions | (Not an error — benchmark returned with wider peer group, match_level shown) |

### Error Handling Strategy

- **Metrics calculation:** Log error for individual school. Continue with next school. No 500 — it's a batch job.
- **Benchmark aggregation:** Log error for individual peer group. Continue with next.
- **AI summary:** Return null on failure. Benchmark still displayed without summary.
- **Benchmark view:** Return 404 if no metrics. Return 200 with data if available.

### Retry Strategy

- Metrics calculation job: no retry (next month's job handles it)
- Benchmark aggregation job: no retry (next month's job handles it)
- AI summary: no retry (return null on failure)
- Client: retry on network error (3 attempts)

### Fallback Behavior

- No metrics: "Benchmarking data not available yet."
- No peer group: "Not enough peer schools for comparison."
- AI summary failed: benchmark displayed without summary
- No trend data: current year only shown

---

## 20. Analytics & Reporting

### Analytics Dashboard Data

| Metric | Source | Derivation |
|---|---|---|
| Schools with metrics calculated | `school_metrics` | Count distinct school_id |
| Peer groups aggregated | `benchmark_summaries` | Count distinct peer_group |
| Benchmark views per month | Audit logs | Count of benchmark API calls |
| AI summaries generated | Audit logs | Count of AI summary calls |
| Most viewed metric | Audit logs | Count by metric type |

### Export Capabilities

- Benchmark export (CSV) — metric, school value, peer avg, percentile, year

### Report Types

| Report | Format | Frequency | Recipient |
|---|---|---|---|
| Benchmarking adoption | JSON (API) | Monthly | Product Team |
| Platform-wide metrics | JSON (API) | Monthly | Super Admin |
| Peer group coverage | JSON (API) | Monthly | Dev Team |

---

## 21. Testing Strategy

### Unit Tests

- `MetricsCalculatorService.calculateSchoolMetrics()` — each of 5 metrics calculation
- `BenchmarkService.determinePeerGroup()` — peer group matching at all 5 levels
- `BenchmarkService.aggregatePeerGroups()` — aggregation with min 10 schools
- Percentile calculation — verify P25, P75, P90, median
- Quartile assignment — verify Q1-Q4 boundaries
- AI summary generation — prompt building, response parsing

### Integration Tests

- Full metrics flow: set up test data → run job → verify metrics in DB
- Full benchmark flow: set up schools → calculate metrics → aggregate benchmarks → verify
- Peer group expansion: set up < 10 schools same board+state → verify criteria expansion
- Benchmark API: calculate metrics → call API → verify response structure
- Trend: calculate metrics for 3 years → verify trend data

### E2E Tests

- Admin opens benchmark dashboard → sees 5 metrics with peer comparison
- Admin taps metric → sees trend chart with 3 years
- Admin reads AI summary

### Performance Tests

- Metrics calculation: < 30 minutes for 100 schools
- Benchmark aggregation: < 15 minutes for all peer groups
- Benchmark API: < 2 seconds
- AI summary: < 10 seconds

### Test Data

- 20 schools with varying boards, sizes, states
- 3 academic years of attendance, marks, fee, engagement data
- Schools with no data (edge cases)
- Schools with same board but different states
- Schools with same board+state but different sizes

### Test Environment

- Test database with benchmark tables
- Mock LLM (returns controlled AI summary)
- Test JWT tokens for admin and super admin roles

---

## 22. Acceptance Criteria

- [ ] School metrics calculated monthly (attendance, pass rate, fee collection, engagement, teacher-student ratio)
- [ ] Peer group determined by board + size + state
- [ ] Anonymized comparison with peer averages and percentiles
- [ ] Trend over multiple academic years (3 years)
- [ ] AI summary of strengths/weaknesses
- [ ] Privacy: no individual school identifiable
- [ ] Minimum peer group size enforced (10 schools)
- [ ] Peer group expansion when minimum not met
- [ ] Quartile and percentile ranking displayed
- [ ] Color-coded metric cards (green/yellow/red)

---

## 23. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration `migration_079_school_benchmarking.sql`, Exposed tables, register in `DatabaseFactory` |
| 2 | 3 days | `MetricsCalculatorService` (all 5 metrics) |
| 3 | 2 days | Peer group matching + benchmark aggregation |
| 4 | 1 day | Monthly batch jobs (`MetricsCalculationJob`, `BenchmarkAggregationJob`) |
| 5 | 1 day | AI summary integration |
| 6 | 1 day | API endpoint |
| 7 | 3 days | Client UI: `BenchmarkScreen` (dashboard), `BenchmarkDetailScreen` (trend chart) |
| 8 | 1 day | Tests: unit, integration, E2E |

### Pre-Implementation Checklist

- [ ] Verify `MULTI_BRANCH_SPEC.md` is implemented
- [ ] Verify `AttendanceRecordsTable` has test data
- [ ] Verify `AssessmentMarksTable` has test data
- [ ] Verify `FeeRecordsTable` has test data
- [ ] Verify `NotificationsTable` has engagement data
- [ ] Verify `SchoolsTable` has board, city, state, enrollment fields
- [ ] Verify `AiService` available for AI summary
- [ ] Verify at least 20 schools on platform for meaningful peer groups

---

## 24. File-Level Impact Analysis

### Server

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | `SchoolMetricsTable`, `BenchmarkSummariesTable` |
| `server/.../db/DatabaseFactory.kt` | Modify | Register 2 benchmark tables in `allTables` |
| `server/.../feature/benchmark/MetricsCalculatorService.kt` | **New** | Metrics calculation (5 metrics) |
| `server/.../feature/benchmark/BenchmarkService.kt` | **New** | Peer comparison + AI summary |
| `server/.../feature/benchmark/BenchmarkJob.kt` | **New** | Monthly batch jobs |
| `server/.../feature/benchmark/BenchmarkRouting.kt` | **New** | API endpoint |
| `docs/db/migration_079_school_benchmarking.sql` | **New** | DDL |

### Shared (KMP)

| File | Change Type | Description |
|---|---|---|
| `shared/.../benchmark/domain/model/BenchmarkModels.kt` | **New** | DTOs, domain models, enums |
| `shared/.../benchmark/domain/repository/BenchmarkRepository.kt` | **New** | Repository interface |
| `shared/.../benchmark/data/remote/BenchmarkApi.kt` | **New** | HTTP API definition |

### Client (Compose)

| File | Change Type | Description |
|---|---|---|
| `composeApp/.../ui/v2/screens/admin/BenchmarkScreen.kt` | **New** | Benchmark dashboard |
| `composeApp/.../ui/v2/screens/admin/BenchmarkDetailScreen.kt` | **New** | Metric detail with trend chart |

---

## 25. Future Enhancements

| # | Enhancement | Priority | Effort | Notes |
|---|---|---|---|---|
| F-1 | Custom metric definitions | Low | M | Allow schools to define custom metrics |
| F-2 | Benchmarking alerts | Medium | S | Alert when metric drops below peer average |
| F-3 | Regional benchmarking | Low | S | Compare by city/district instead of state |
| F-4 | Historical peer comparison | Low | M | Compare peer groups over time |
| F-5 | Export benchmark report | Medium | S | PDF export of benchmark dashboard |
| F-6 | Metric drill-down | Low | M | Drill into metric components (e.g., attendance by grade) |
| F-7 | Best practices recommendations | Medium | M | AI recommends actions based on weak metrics |
| F-8 | Inter-school collaboration | Low | L | Connect schools with similar improvement goals |
| F-9 | Real-time metrics | Low | L | Calculate metrics in real-time instead of monthly |
| F-10 | Custom peer group selection | Low | S | Admin selects specific peer criteria |

---

## Appendix A: Sequence Diagrams

### A.1 Metrics Calculation Flow

```
MetricsCalculationJob    Server              DB
  │                       │                   │
  │  execute()            │                   │
  │  ───────────────────> │                   │
  │                       │──get all schools──>│
  │                       │←──school list──────│
  │                       │                   │
  │                       │──for each school──│
  │                       │  calculate:       │
  │                       │  - attendance rate│
  │                       │  - pass rate      │
  │                       │  - fee collection │
  │                       │  - engagement     │
  │                       │  - teacher ratio  │
  │                       │──upsert metrics──>│
  │                       │←──success─────────│
  │                       │                   │
  │  ←──count processed   │                   │
  │                       │                   │
```

### A.2 Benchmark View Flow

```
Admin (app)       Server              DB
  │                │                   │
  │  GET /benchmark│                   │
  │  ?year={uuid}  │                   │
  │  ────────────> │                   │
  │                │──get school metrics──────────>│
  │                │←──5 metric values─────────────│
  │                │──determine peer group─────────│
  │                │──get benchmark summaries──────>│
  │                │←──avg, median, percentiles────│
  │                │──calculate percentile rank────│
  │                │──get trend (3 years)──────────>│
  │                │←──trend data──────────────────│
  │                │──generate AI summary──────────│
  │                │←──summary text────────────────│
  │  ←──200: BenchmarkDto              │
  │                │                   │
  │  ──render dashboard──              │
  │                │                   │
```

---

## Appendix B: Domain Model / ER Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                      school_metrics (new)                             │
│  id (PK)                                                              │
│  school_id, academic_year_id                                          │
│  metric_type (attendance_rate|pass_rate|fee_collection_rate|          │
│               parent_engagement|teacher_student_ratio)                │
│  metric_value (DOUBLE)                                                │
│  calculated_at                                                        │
│  UNIQUE: (school_id, academic_year_id, metric_type)                   │
│  INDEX: (academic_year_id, metric_type)                               │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│                   benchmark_summaries (new)                           │
│  id (PK)                                                              │
│  academic_year_id, metric_type                                        │
│  peer_group (JSON: {board, size_range, state})                        │
│  avg_value, median_value                                              │
│  p25_value, p75_value, p90_value                                      │
│  school_count (≥ 10)                                                  │
│  calculated_at                                                        │
│  UNIQUE: (academic_year_id, metric_type, peer_group)                  │
└──────────────────────────────────────────────────────────────────────┘

Existing tables used (read-only):
┌────────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│ attendance_records  │  │ assessment_marks │  │ fee_records      │
│ (existing)          │  │ (existing)       │  │ (existing)       │
└────────────────────┘  └──────────────────┘  └──────────────────┘
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│ notifications     │  │ schools          │  │ users            │
│ (existing)        │  │ (existing)       │  │ (existing)       │
└──────────────────┘  └──────────────────┘  └──────────────────┘
┌──────────────────┐
│ academic_years    │
│ (existing)        │
└──────────────────┘
```

---

## Appendix C: Event Flow

### Domain Events

| Event | Emitter | Consumers | Payload | Side Effects |
|---|---|---|---|---|
| `SchoolMetricsCalculated` | `MetricsCalculationJob` | None (logged) | `schoolId, academicYearId, metricCount` | Metrics upserted |
| `BenchmarkAggregated` | `BenchmarkAggregationJob` | None (logged) | `peerGroup, metricType, schoolCount` | Benchmark upserted |
| `BenchmarkViewed` | `BenchmarkService.getBenchmark()` | None (logged) | `adminId, schoolId, academicYearId` | None |
| `AiSummaryGenerated` | `BenchmarkService.generateAiSummary()` | None (logged) | `schoolId, academicYearId` | Summary cached |

### Event Delivery Guarantees

- Events emitted synchronously within service methods
- All events logged for audit
- No external consumers — events are internal audit trail

---

## Appendix D: Configuration

### Environment Variables

| Variable | Default | Description |
|---|---|---|
| `BENCHMARKING_ENABLED` | `true` | Enable/disable benchmarking feature |
| `BENCHMARKING_MIN_PEER_GROUP` | `10` | Minimum peer group size |
| `BENCHMARKING_SIZE_RANGE_PERCENT` | `20` | Enrollment size range (±20%) |
| `BENCHMARKING_AI_SUMMARY_ENABLED` | `true` | Enable/disable AI summary |
| `BENCHMARKING_TREND_YEARS` | `3` | Number of years for trend |
| `BENCHMARKING_AI_MODEL` | `benchmark_v1` | AI model identifier |

### Feature Flags

| Flag | Default | Description |
|---|---|---|
| `BENCHMARKING_ENABLED` | `true` | Enable/disable benchmarking |
| `BENCHMARKING_AI_SUMMARY_ENABLED` | `true` | Enable/disable AI summary |
| `BENCHMARKING_TREND_ENABLED` | `true` | Enable/disable trend display |

### School-Level Settings

| Setting | Default | Description |
|---|---|---|
| `benchmarking_enabled` | `true` | Per-school enable/disable |

---

## Appendix E: Migration & Rollback

### Migration: `migration_079_school_benchmarking.sql`

```sql
-- Migration 079: School Benchmarking
-- Creates school_metrics, benchmark_summaries tables

BEGIN;

CREATE TABLE IF NOT EXISTS school_metrics (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    academic_year_id UUID NOT NULL,
    metric_type     VARCHAR(32) NOT NULL,
    metric_value    DOUBLE PRECISION NOT NULL,
    calculated_at   TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(school_id, academic_year_id, metric_type)
);

CREATE INDEX IF NOT EXISTS idx_school_metrics_year
    ON school_metrics (academic_year_id, metric_type);

CREATE TABLE IF NOT EXISTS benchmark_summaries (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    academic_year_id UUID NOT NULL,
    metric_type     VARCHAR(32) NOT NULL,
    peer_group      TEXT NOT NULL,
    avg_value       DOUBLE PRECISION NOT NULL,
    median_value    DOUBLE PRECISION NOT NULL,
    p25_value       DOUBLE PRECISION NOT NULL,
    p75_value       DOUBLE PRECISION NOT NULL,
    p90_value       DOUBLE PRECISION NOT NULL,
    school_count    INTEGER NOT NULL,
    calculated_at   TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(academic_year_id, metric_type, peer_group)
);

COMMIT;
```

### Rollback: `migration_079_rollback.sql`

```sql
BEGIN;
DROP TABLE IF EXISTS benchmark_summaries;
DROP TABLE IF EXISTS school_metrics;
COMMIT;
```

### Migration Validation

- Verify 2 tables created with correct columns
- Verify `school_metrics(school_id, academic_year_id, metric_type)` UNIQUE constraint
- Verify `benchmark_summaries(academic_year_id, metric_type, peer_group)` UNIQUE constraint
- Verify index on `school_metrics(academic_year_id, metric_type)`
- Run `SELECT count(*) FROM school_metrics` — should be 0 (new feature)

---

## Appendix F: Observability

### Structured Logging

| Log Level | Event | Context Fields |
|---|---|---|
| INFO | Metrics calculated for school | `schoolId, academicYearId, metricCount` |
| INFO | All schools metrics calculated | `academicYearId, schoolCount, duration` |
| INFO | Benchmark aggregated | `peerGroup, metricType, schoolCount` |
| INFO | All benchmarks aggregated | `academicYearId, peerGroupCount, duration` |
| INFO | Benchmark viewed | `adminId, schoolId, academicYearId` |
| INFO | AI summary generated | `schoolId, academicYearId, tokensUsed` |
| WARN | School has no data for metric | `schoolId, academicYearId, metricType` |
| WARN | Peer group too small, expanding | `peerGroup, schoolCount, newMatchLevel` |
| WARN | AI summary generation failed | `schoolId, academicYearId, error` |
| ERROR | Metrics calculation failed | `schoolId, academicYearId, error` |
| ERROR | Benchmark aggregation failed | `peerGroup, metricType, error` |

### Metrics

| Metric | Type | Labels | Description |
|---|---|---|---|
| `benchmark_metrics_calculated` | Counter | `academic_year_id` | Total metrics calculated |
| `benchmark_aggregations` | Counter | `academic_year_id, match_level` | Total peer groups aggregated |
| `benchmark_views` | Counter | `school_id` | Benchmark dashboard views |
| `benchmark_ai_summaries` | Counter | `school_id` | AI summaries generated |
| `benchmark_calculation_duration` | Histogram | — | Metrics calculation job duration |
| `benchmark_aggregation_duration` | Histogram | — | Benchmark aggregation job duration |
| `benchmark_api_duration` | Histogram | — | Benchmark API response latency |
| `benchmark_peer_group_size` | Gauge | `match_level` | Average peer group size |

### Health Checks

| Check | Endpoint | Description |
|---|---|---|
| Benchmark service | `/health/benchmark` | Verify service and DB accessible |
| Metrics job | `/health/metrics-job` | Verify last job run and status |

### Alerts

| Alert | Condition | Severity | Notification |
|---|---|---|---|
| Metrics calculation job failed | Job error rate > 10% | Warning | Email to dev team |
| Metrics job slow | Duration > 60 minutes | Info | Email to dev team |
| AI summary failure rate | > 20% | Info | Email to dev team |
| Peer group expansion rate | > 50% | Info | Email to product team (may need more schools) |

### Dashboards

| Dashboard | Panels | Audience |
|---|---|---|
| Benchmarking Usage | Views/day, AI summaries/day, schools with metrics | Product Team |
| Job Performance | Calculation duration, aggregation duration, error rate | Dev Team |
| Peer Group Coverage | Peer groups by match level, avg peer group size | Product Team |
| AI Summary | Generation count, failure rate, token usage | Dev Team |

### Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Privacy breach (school data exposed) | Low | Critical | Anonymized aggregates only. No raw data exposed. Server-side calculation. |
| Peer group too small | Medium | Medium | Expansion logic. Minimum 10 schools. Fall back to all schools. |
| AI summary incorrect | Medium | Low | AI is advisory. Admin reviews. Based on factual metrics. |
| Metrics calculation error | Medium | Medium | Log error per school. Continue with next. Next month's job retries. |
| Platform too small for benchmarking | Medium | Medium | If < 10 schools total, show own metrics without peer comparison. |
| Metrics misleading (different calculation methods) | Low | Medium | Standardized calculation formulas. Documented in spec. |
| Admin misinterprets benchmark | Low | Low | AI summary provides context. Color coding helps. |
| Batch job takes too long | Low | Low | Can be parallelized. Currently < 30 min for 100 schools. |
