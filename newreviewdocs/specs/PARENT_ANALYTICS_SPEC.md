# Parent Analytics Dashboard — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** None
> **Source:** `DIFFERENTIATING_FEATURES.md` §10.1

---

## 1. Feature Overview

A comprehensive analytics dashboard for parents showing their child's academic performance trends, attendance patterns, engagement metrics, and comparative insights (anonymized class average).

### Goals

- Attendance trend chart (monthly, term-wise)
- Marks trend per subject (line chart across terms)
- Class comparison: child vs class average (anonymized)
- Engagement score: homework completion rate, message response rate
- Growth indicators: improving/declining subjects
- Calendar heat map: attendance visualization

---

## 2. Current System Assessment

- All data exists: `AttendanceRecordsTable`, `AssessmentMarksTable`, `HomeworkSubmissionsTable`, `MessagesTable`
- `feature_audit.csv` L108: Parent Dashboard at 60% — basic data shown but no analytics/charts
- `DIFFERENTIATING_FEATURES.md` §10.1: Parent Analytics, effort M, data readiness: "All data exists"

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Attendance trend: monthly attendance % over academic year (line chart) |
| FR-2 | Marks trend: per-subject marks across terms (multi-line chart) |
| FR-3 | Class comparison: child's marks vs class average (anonymized, no other student names) |
| FR-4 | Engagement score: homework completion rate, message read rate, event participation |
| FR-5 | Subject performance radar chart (all subjects on one chart) |
| FR-6 | Growth indicators: ↑↓ per subject comparing current vs previous term |
| FR-7 | Attendance heat map: calendar view with color-coded attendance |
| FR-8 | Time range selector: this term, this year, all years |

---

## 4. Database Design

No new tables — all data aggregated from existing tables via queries.

### 4.1 Aggregation Queries

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

---

## 5. Backend Architecture

### 5.1 ParentAnalyticsService

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

---

## 6. API Contracts

```
GET /api/v1/parent/analytics/{childId}/attendance-trend?range=term|year|all
GET /api/v1/parent/analytics/{childId}/marks-trend?range=term|year|all
GET /api/v1/parent/analytics/{childId}/class-comparison?term=term1
GET /api/v1/parent/analytics/{childId}/engagement
GET /api/v1/parent/analytics/{childId}/subject-radar?term=term1
GET /api/v1/parent/analytics/{childId}/growth?term=term1
GET /api/v1/parent/analytics/{childId}/attendance-heatmap?month=2026-06
```

---

## 7. Frontend Architecture

Charts using Compose Multiplatform charting library:
- **Line charts:** attendance trend, marks trend
- **Radar chart:** subject performance
- **Bar chart:** class comparison
- **Heat map:** custom composable for attendance calendar
- **Growth indicators:** ↑↓ arrows with color (green/red)

Recommended library: `compose-charts` or custom Canvas-based composables.

---

## 8. Acceptance Criteria

- [ ] Attendance trend chart shows monthly attendance %
- [ ] Marks trend chart shows per-subject performance across terms
- [ ] Class comparison shows child vs anonymized class average
- [ ] Engagement score calculated (homework, messages, events)
- [ ] Subject radar chart shows all subjects
- [ ] Growth indicators show ↑↓ per subject
- [ ] Attendance heat map visualizes daily attendance
- [ ] Time range selector works (term/year/all)

---

## 9. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 2 days | ParentAnalyticsService (aggregation queries) |
| 2 | 1 day | API endpoints |
| 3 | 4 days | Client UI (charts, heat map, radar, growth indicators) |
| 4 | 1 day | Tests |

---

## 10. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../feature/analytics/ParentAnalyticsService.kt` | New | Aggregation service |
| `server/.../feature/analytics/ParentAnalyticsRouting.kt` | New | API endpoints |
| `shared/.../feature/analytics/ParentAnalyticsApi.kt` | New | Client API |
| `composeApp/.../ui/v2/screens/parent/AnalyticsScreen.kt` | New | Analytics dashboard |
| `composeApp/.../ui/v2/components/charts/*.kt` | New | Chart composables |
