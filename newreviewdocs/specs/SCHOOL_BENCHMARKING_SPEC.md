# School Benchmarking — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** `MULTI_BRANCH_SPEC.md`
> **Source:** `DIFFERENTIATING_FEATURES.md` §10.2

---

## 1. Feature Overview

Anonymized school benchmarking dashboard that compares a school's performance metrics (attendance, academic results, fee collection, engagement) against aggregated averages from similar schools (same board, similar size, same region).

### Goals

- Compare school metrics against peer group averages (anonymized)
- Peer groups: same board, similar enrollment size, same state/region
- Metrics: attendance rate, pass rate, fee collection rate, parent engagement, teacher retention
- Trend comparison over academic years
- Identify strengths and improvement areas
- All data anonymized — no individual school identifiable

---

## 2. Current System Assessment

- All metrics data exists across tables: `AttendanceRecordsTable`, `AssessmentMarksTable`, `FeeRecordsTable`, `NotificationsTable`
- `SchoolsTable` has `board`, `city`, `state` for peer grouping
- `DIFFERENTIATING_FEATURES.md` §10.2: School Benchmarking, effort M, data readiness: "All metrics data exists"
- No benchmarking/aggregation system exists

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Calculate school metrics: attendance rate, pass rate, fee collection rate, parent engagement rate, teacher-student ratio |
| FR-2 | Peer group: same board + similar enrollment (±20%) + same state |
| FR-3 | Anonymized comparison: "Your attendance rate: 92% | Peer average: 87% | Top quartile: 95%" |
| FR-4 | Percentile ranking: where does school stand among peers |
| FR-5 | Trend: metrics over last 3 academic years |
| FR-6 | Strengths/weaknesses: AI-generated summary of where school excels/lags |
| FR-7 | Privacy: no school can see another school's individual data |

---

## 4. Database Design

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

---

## 5. Backend Architecture

### 5.1 MetricsCalculatorService

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

### 5.2 Scheduled Jobs

- **Monthly:** Calculate metrics for all schools → update `school_metrics`
- **Monthly:** Aggregate peer group benchmarks → update `benchmark_summaries`

---

## 6. API Contracts

```
GET /api/v1/school/benchmark?academic_year_id={uuid}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "peer_group": {"board": "CBSE", "size_range": "500-1000", "state": "Maharashtra", "peer_count": 47},
    "metrics": [
      {
        "metric": "attendance_rate",
        "school_value": 92.5,
        "peer_avg": 87.3,
        "peer_median": 88.0,
        "percentile": 78,
        "quartile": "Q3",
        "trend": [{"year": "2024-25", "value": 89.0}, {"year": "2025-26", "value": 91.0}, {"year": "2026-27", "value": 92.5}]
      }
    ],
    "ai_summary": "Your school performs above peer average in attendance (92.5% vs 87.3%) and fee collection. Improvement needed in parent engagement (45% vs peer avg 62%)."
  }
}
```

---

## 7. Privacy & Anonymization

- Peer group minimum size: 10 schools (if fewer, expand criteria — drop state, then size range)
- No individual school data exposed — only aggregates (avg, median, percentiles)
- School count shown but no names
- Metrics calculation is server-side only

---

## 8. Acceptance Criteria

- [ ] School metrics calculated monthly (attendance, pass rate, fee collection, engagement)
- [ ] Peer group determined by board + size + state
- [ ] Anonymized comparison with peer averages and percentiles
- [ ] Trend over multiple academic years
- [ ] AI summary of strengths/weaknesses
- [ ] Privacy: no individual school identifiable
- [ ] Minimum peer group size enforced

---

## 9. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration, Exposed tables |
| 2 | 3 days | MetricsCalculatorService (all 5 metrics) |
| 3 | 2 days | Peer group matching + benchmark aggregation |
| 4 | 1 day | Monthly batch jobs |
| 5 | 1 day | AI summary integration |
| 6 | 1 day | API endpoint |
| 7 | 3 days | Client UI (benchmark dashboard, charts, trend) |
| 8 | 1 day | Tests |

---

## 10. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | 2 benchmark tables |
| `server/.../feature/benchmark/MetricsCalculatorService.kt` | New | Metrics calculation |
| `server/.../feature/benchmark/BenchmarkService.kt` | New | Peer comparison |
| `server/.../feature/benchmark/BenchmarkJob.kt` | New | Monthly batch |
| `docs/db/migration_079_school_benchmarking.sql` | New | DDL |
| `composeApp/.../ui/v2/screens/admin/BenchmarkScreen.kt` | New | Benchmark dashboard |
