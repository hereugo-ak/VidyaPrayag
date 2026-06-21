# Metrics Brief — product decisions, not just engineering

> For every panel: **why this metric earns its place**, the exact source, the calculation, the refresh strategy, the empty-state copy. A principal has finite attention; every number costs cognitive load. Each one is justified or cut.

Legend for refresh: **LIVE** = 15s SWR poll + focus revalidate · **NEAR-LIVE** = 60s · **SLOW** = 300s + focus only · **ON-DEMAND** = fetched/derived on user action. (Defined in `website/src/lib/admin/hooks.ts`.) No websocket exists in this backend — polling is the correct tool, documented per hook.

---

## A. First 60 seconds of the morning (top visual weight)

A principal walking in needs four things instantly: *who's here, what's scheduled, what flagged, what needs me today.*

### A1. Present now
- **Why:** the single most-asked morning question. It is the heartbeat of the building.
- **Source:** `attendance/summary` → `present + late`. Live "as of `latest_date`".
- **Calc:** `present + late`. (Late counts as in-building.)
- **Refresh:** LIVE (15s). Attendance is submitted through the morning.
- **Empty:** "Attendance not submitted yet today." (No teacher has marked.)

### A2. Absent
- **Why:** the actionable counterpart to A1 — who to chase.
- **Source:** `attendance/summary.absent`. **Refresh:** LIVE.
- **Empty:** hidden until any attendance exists.

### A3. Attendance rate
- **Why:** one number that says "is today normal?"
- **Source:** `attendance/summary.rate`. **Refresh:** LIVE.
- **Empty:** "—" until data.

### A4. At-risk count
- **Why:** the "anything flagged" signal — early-warning students needing attention.
- **Source:** `dashboard/intelligence.early_warning.length`. **Refresh:** NEAR-LIVE.
- **Empty:** shows `0` with neutral tone (a real, healthy zero — not an empty state).

### A5. Unread
- **Why:** "is anyone waiting on me" — messages/notifications.
- **Source:** `notifications/summary.unread_count`. **Refresh:** LIVE.
- **Empty:** `0`, neutral.

> These five are the **Live Pulse strip** (already implemented; we keep it and make it respect the global class scope where the data allows: A1–A3 can be recomputed from `attendance/summary.by_class` for a scoped class; A4 from filtering `early_warning` by `class_name`; A5 is account-level and stays school-wide with a note when scoped).

### A6. Today's schedule (the calendar)
- **Why:** "what is the day across every class" — the brief's headline. Glanceable before a word is spoken.
- **Source:** `timetable` (NEW) + `calendar` events. **Refresh:** SLOW for the grid; per-slot attendance ON-DEMAND.
- **Empty:** "No schedule entered yet — add your class timetable." (CTA where a write path exists; today timetable write is teacher-side, so the CTA points to that workflow / explains it.)

---

## B. Trends that matter across the academic period (medium weight, Overview + Academics)

### B1. Attendance trajectory (30-day)
- **Why:** the clearest health signal — is presence slipping? Anomaly days + exam overlay turn a line into an insight ("attendance dipped the day before the unit test").
- **Source:** `dashboard/intelligence.attendance_timeline[]` — each point has `rate`, `present`, `absent`, `total`, `is_anomaly`, `exam`.
- **Calc:** server already computes per-day rate and a dynamic anomaly threshold (mean − 1σ, floored at mean − 10). We render the line, mark anomalies, pin exam markers.
- **Refresh:** NEAR-LIVE (60s).
- **Empty:** "Attendance trend appears once daily attendance is recorded for a few days."

### B2. Syllabus completion rate (academic health grid)
- **Why:** the leading indicator of whether classes will finish the curriculum — a principal's quarterly anxiety, made visible per class × subject.
- **Source:** `dashboard/intelligence.academic_health` → `subjects[]`, `rows[]` (`class_name`, `cells[].percentage` = covered/total units, `class_average`).
- **Calc:** server-computed coverage. We render a heat grid; cell colour = coverage band.
- **Refresh:** NEAR-LIVE.
- **Empty:** "No syllabus units yet — coverage appears once teachers log their syllabus."

### B3. Marks / academic average
- **Why:** outcome signal to pair with coverage (covered ≠ learned).
- **Source:** `marks/summary` → `overall_average_pct`, `assessments[]`.
- **Refresh:** SLOW (marks publish in batches).
- **Empty:** "No assessments graded yet."

### B4. Fee collection trajectory
- **Why:** institutional solvency. Collected vs due vs overdue is the trajectory.
- **Source:** `fees/ledger` → `paid_total`, `due_total`, `overdue_total`, counts, `recent[]`.
- **Calc:** collection rate = `paid / (paid + due + overdue)`. Render as a stacked bar / donut + recent rows.
- **Refresh:** SLOW.
- **Empty:** "No fee records yet."

### B5. Leave-request volume
- **Why:** a secondary operational-load signal (spikes precede staffing/coverage stress).
- **Source:** `leave-requests` list → `weekly_count`, `approval_rate`. (Already on `/admin/leave`.)
- **Refresh:** NEAR-LIVE.
- **Empty:** "No leave requests this week."

---

## C. Requires action today (actionable, not just counts)

### C1. Pending leave requests
- **Source:** `leave-requests?status=Pending`. Rendered as an actionable list (approve/reject) on `/admin/leave`; surfaced on dashboard as a count + "review →".
- **Refresh:** NEAR-LIVE. **Empty:** "No leave requests awaiting you."

### C2. Early-warning students (top of cohort)
- **Why:** the students a principal can still help before exams — each row states **why** (signals), not an opaque score.
- **Source:** `dashboard/intelligence.early_warning[]` — `risk_level`, `signals[]` (attendance/marks/leave, each with a human `label` + severity), `attendance_pct`, `marks_pct`, `leave_count`.
- **Calc:** server-computed combined signals; we show top 5 on Overview (sorted by signal count then severity, as the server returns), full list on Academics/People.
- **Refresh:** NEAR-LIVE.
- **Empty:** "No students flagged — every student is above the risk thresholds." (A genuinely good empty state.)

### C3. Attendance anomalies
- **Source:** `attendance_timeline[].is_anomaly`. Surfaced as a badge on the trend chart + a one-line callout when the latest point is an anomaly.
- **Refresh:** NEAR-LIVE. **Empty:** none needed (folded into B1).

### C4. Activity that needs awareness
- **Source:** `dashboard/intelligence.activity_feed[]` (notifications + leave + announcements, newest first, real DB timestamps).
- **Refresh:** NEAR-LIVE (the bell itself is LIVE). **Empty:** "Activity from across the school will appear here."

---

## D. Weekly review (Overview, lower weight; Academics full)
- Academic health grid (B2), early-warning full cohort (C2), attendance trend (B1), financial collection rate (B4). Lower visual weight than A/C on Overview; full weight on their dedicated tabs.

## E. Monthly review (own tabs)
- Trend charts and comparisons live on **Academics** and **Finance** tabs. The backend supplies the 30-day timeline and current ledger; we do **not** invent month-over-month deltas the backend doesn't compute. Where the analytics `overview` endpoint provides `current_growth` + `performance_trend`, we render those honestly with their server labels.

---

## F. Global class scope — per-metric behaviour (Control B)
When `?cls=<class>` is active, each metric either scopes truthfully or declares it can't:

| Metric | Scoped behaviour |
|---|---|
| A1–A3 attendance | Recompute from `attendance/summary.by_class` row for that class. |
| A4 / C2 early warning | Filter `early_warning[]` by `class_name`. |
| B1 attendance trend | **School-wide only** — the timeline isn't per-class on the server. Show "Trend is school-wide" note when scoped. |
| B2 academic health | Filter `rows[]` to the one class. |
| B3 marks | Filter `assessments[]` by `class_name`. |
| B4 fees | **No per-class data** — show honest "Fees aren't broken out by class yet" note. |
| People | Students refetched with `?class=`; teachers stay school-wide. |

This table is the contract the implementation follows. Truthfulness over completeness, every time.

---

## G. Greeting bar metrics
- **Greeting:** `Good morning|afternoon|evening` by local hour (<12 / 12–17 / >17), `+ session.name.split(" ")[0]`.
- **Date:** `toLocaleDateString("en-IN", { weekday:"long", day:"numeric", month:"long" })`.
- **Academic week:** `meta.academic_week` (server, ISO week of school year).
- **School name:** `meta.school_name`.
All from real session + `dashboard/intelligence.meta`. None hardcoded.
