# Tab Structure — every tab is a product decision

> Each tab exists because removing it would cost a principal a distinct cognitive context. Tabs are URL-encoded (`?tab=`). Default `overview`.

The greeting bar + global class filter (Control B) + global search + the **tab strip** are persistent across all tabs (they live in the dashboard workspace shell, above the tab content).

---

## Tab 1 — Overview  (`?tab=overview`, default)
**Purpose:** the morning command center — synthesize "what's happening today" in one screen.

**Primary panels (high weight):**
- **Live Pulse strip** — present now / absent / rate / at-risk / unread (A1–A5).
- **Week Calendar (embedded, signature)** — current academic week, all classes colour-coded, Control A inline, click→drill-down. "Open full calendar →" jumps to the Calendar tab.
- **Attendance Intelligence** — 30-day trend with anomalies + exam overlay (B1), anomaly callout (C3).
- **Early Warning** — top 5 at-risk with signal reasons (C2); "View all N →".

**Secondary panels (review weight):**
- **Financial Pulse** — collected/due/overdue summary (B4).
- **Academic Health preview** — compact coverage grid (B2); "Open academics →".
- **Activity Feed** — institutional event stream (C4).
- **People Snapshot** — student/teacher counts + today's rate.

**Key actions:** open calendar slot · act on flagged student · compose announcement (QuickCompose) · jump to attendance/leave.
**Data sources:** `dashboard/intelligence`, `attendance/summary`, `notifications/summary`, `fees/ledger`, `timetable`, `calendar`, `students`, `teachers`.
**Empty:** onboarding-incomplete hero dominates if `onboarding.is_complete === false`; otherwise per-panel empty states.

---

## Tab 2 — Academics  (`?tab=academics`)
**Purpose:** "are we academically healthy?" — the weekly/monthly academic review.

**Primary panels:**
- **Academic Health grid (full)** — class × subject coverage heat grid (B2), class averages, click a cell → side panel (class+subject coverage detail).
- **Attendance Trend (full)** — 30-day line, anomalies, exam markers (B1).

**Secondary panels:**
- **Early-Warning cohort (full)** — every flagged student, grouped by risk level, with signal breakdown (C2).
- **Marks Summary** — `marks/summary` table: subject, assessment, class, average, published state (B3).

**Key actions:** drill class/subject cell · open `/admin/marks` to grade · open a student.
**Data sources:** `dashboard/intelligence` (academic_health, attendance_timeline, early_warning), `marks/summary`.
**Empty:** "No syllabus units yet" (grid) / "No assessments graded yet" (marks) / "No students flagged" (cohort).

---

## Tab 3 — Finance  (`?tab=finance`)
**Purpose:** "are we financially healthy?" — collection posture.

**Primary panels:**
- **Collection summary** — collected / due / overdue tiles + collection-rate donut (B4).
- **Recent fees** — `fees/ledger.recent[]` table (title, amount, status, due date, category).

**Key actions:** view a fee row context. (Read-only — no fee-create endpoint exists; we don't fake one.)
**Data sources:** `fees/ledger`.
**Empty:** "No fee records yet."
**Scope note:** when Control B active → "Fee ledger is school-wide; per-class fees aren't broken out by the backend yet."

---

## Tab 4 — People  (`?tab=people`)
**Purpose:** the people of the school — reference + quick action.

**Primary panels:**
- **Headcount** — students, teachers, today's attendance rate.
- **At-risk students** — early-warning cohort (C2), the human view.

**Secondary panels:**
- **Roster preview** — first N students (`students`), search box (mirrors `/admin/people`), "Open full roster →".
- **Staff preview** — `teachers` list preview.

**Key actions:** add student (`/admin/people?add=student`) · open roster · open a student/teacher.
**Data sources:** `students`, `teachers`, `attendance/summary`, `dashboard/intelligence.early_warning`.
**Empty:** "No students yet — import your roster." (CTA → bulk import.)

---

## Tab 5 — Calendar  (`?tab=calendar`)
**Purpose:** the schedule across the entire school — the signature surface, full-bleed.

**Primary panel:**
- **Full Calendar** — week (default) / month toggle, all classes colour-coded, Control A primary + always visible, legend, `‹ Today ›` navigation, slot/day drill-down. Full spec in `CALENDAR_SPEC.md`.

**Secondary (contextual rail, desktop only):**
- **Week summary** — working days, holidays this view (`calendar.summary`), count of classes scheduled, today's marked-vs-pending classes.

**Key actions:** click slot → drill-down (teacher status, present/enrolled, syllabus, notes) · switch view · filter to one class (Control A).
**Data sources:** `timetable`, `calendar`, `attendance/daily` (on-demand), `attendance/summary`, `dashboard/intelligence.academic_health`.
**Empty:** "No schedule entered yet" (no timetable) / "No events this week" (calendar.summary empty) — both designed, with explanation of what makes data appear.

---

## Tab strip behaviour
- Desktop: horizontal pill tabs under the greeting bar; active = filled accent, others = ghost.
- Mobile: horizontal scroller, active tab auto-scrolls into view; sticky under the greeting bar.
- Switching tabs updates `?tab=` (replaceState — no history spam within a session, but deep links work).
- The global class filter (Control B) and search persist across tab switches (they're shell-level).
- Each tab content mounts with a `FadeIn`; SWR `keepPreviousData` means re-entering a tab is instant (cached) and revalidates in the background.

## What we deliberately did NOT make a tab (subtraction log)
- **Announcements / Leave / Messages / Settings / Marks-grading / Attendance-marking** → these are *operate* surfaces, already first-class sidebar routes. The dashboard is the *synthesize* layer. Duplicating them as tabs would blur the morning-triage focus.
- **Reports / Exports** → no export endpoint exists; we won't add a tab that can only show "coming soon."
- **Year calendar view** → the timetable is a weekly pattern; a year grid adds chrome, not signal.
