# Dashboard Revamp — Information Architecture

> Branch `backend-by-abuzar_v1.0.3` · Target: `website/` (Next.js 14 App Router + React 18 + SWR + Recharts + Framer Motion + Tailwind).
> Mode: static analysis + file edits. Human builds locally.

## 0. What this system actually is (verified from source)

This is **not** a React-only product with Supabase Realtime. It is a **Kotlin Multiplatform** app (`composeApp` + `shared`) plus a **Next.js web admin** (`website/`), both talking to a **plain Ktor REST backend** (`server/`) backed by Supabase/Postgres via Exposed.

Verified facts that constrain every decision below:

| Claim in the brief | Reality in this repo | Consequence |
|---|---|---|
| "Supabase Realtime" | **No** WebSocket / SSE / realtime channel exists. `grep` for `install(WebSockets)`, `webSocket(`, `sse(` in `server/src` → **zero**. | "Real-time" = **tuned SWR polling**. Documented honestly per metric (LIVE 15s / NEAR-LIVE 60s / SLOW 300s). Already the discipline in `website/src/lib/admin/hooks.ts`. |
| "React Query" | Project uses **SWR 2.4** (`package.json`). | We use SWR. |
| "all classes' schedule on a calendar" | `teacher_periods` table exists (weekday, start/end, class, section, subject, room, teacher) but is **only read teacher-scoped** (`TeacherRouting.kt`). No school-wide timetable endpoint. | **Backend gap.** We add one read-only route `GET /api/v1/school/timetable` (school-scoped) so the signature calendar is REAL, not mocked. See `CALENDAR_SPEC.md` §Data. |
| "every number from Supabase" | A purpose-built `GET /api/v1/school/dashboard/intelligence` already assembles attendance timeline (+anomaly +exam overlay), early-warning, academic-health grid, activity feed, and meta (school name, academic week, today) — all from real tables (`SchoolIntelligenceRouting.kt`). | This is the spine of the Overview tab. |

The current web dashboard (`app/admin/dashboard/page.tsx`) is a **single scrolling page**. The brief mandates **multi-tab cognitive modes**. This document defines that structure.

---

## 1. First principles: a principal's day

What discrete mental contexts does a school administrator actually move through? Derived from the data we hold and from how principals work:

1. **"What is happening right now / today?"** — Morning triage. Who is in the building, what's scheduled across every class, what flagged overnight, what needs a decision today. *High urgency, glanceable.*
2. **"Show me one class / one room in depth."** — Drill-down. The calendar slot panel + the dashboard-global class filter serve this. *Surgical.*
3. **"Are we academically healthy?"** — Weekly review. Attendance trend, syllabus coverage by class×subject, early-warning cohort, marks. *Analytical, less time-pressured.*
4. **"Are we financially healthy?"** — Fee collection vs outstanding, overdue follow-ups. *Periodic.*
5. **"People."** — Roster of students and staff, who's at risk, provisioning. *Reference + action.*
6. **"Communications & approvals."** — Announcements out, leave requests in. *Inbox-style action queue.*

These map to tabs. The full per-tab spec lives in `TAB_STRUCTURE.md`; this file states the IA rationale and the cross-cutting laws.

---

## 2. Tab structure (cognitive modes, not pages)

The dashboard route `/admin/dashboard` becomes a **tabbed workspace**. Tabs are URL-encoded (`?tab=`) so state is shareable and the browser back button works.

| Tab | Cognitive mode | One-line purpose |
|---|---|---|
| **Overview** | "Right now / today" | The morning command center: greeting, pulse, calendar, attendance intelligence, early warning, activity. |
| **Academics** | "Are we academically healthy?" | Syllabus-coverage heat grid, attendance trend, marks summary, early-warning cohort in full. |
| **Finance** | "Are we financially healthy?" | Fee ledger: collected / due / overdue, recent transactions, collection trajectory. |
| **People** | "People" | Students + staff roster snapshots, at-risk list, headcount. |
| **Calendar** | "The schedule across the school" | The signature calendar, full-bleed, with both filter controls and slot drill-down. (Also embedded compactly on Overview.) |

> **Subtraction note (LAW: premium is subtraction):** We deliberately do **not** add a "Reports", "Settings", or "Messaging" tab to the dashboard workspace. Those are first-class sidebar destinations already (`ADMIN_NAV`). The dashboard tabs are *only* the cognitive modes a principal sits inside; everything else is a navigation away. Adding more tabs would dilute the morning-triage clarity that is the whole point.

The existing sidebar nav (`/admin/people`, `/admin/attendance`, `/admin/marks`, `/admin/fees`, `/admin/announcements`, `/admin/leave`, `/admin/settings`) **remains** as the deep task surfaces. The dashboard tabs are the *synthesis* layer; the sidebar routes are the *operate* layer. Command-bar actions and "open full →" affordances bridge the two.

---

## 3. Per-tab IA

For each tab: **shows / primary action / empty state / data sources / real-time strategy.** (Panel-level detail in `TAB_STRUCTURE.md` and `METRICS_BRIEF.md`.)

### 3.1 Overview
- **Shows:** time-aware greeting bar (name + date + academic week + school name), command bar, live pulse strip, embedded **week calendar** (signature), attendance intelligence (trend + anomalies + exam overlay), early-warning cohort (top 5 + "view all"), financial pulse summary, academic-health preview, activity feed.
- **Primary action:** open a calendar slot drill-down / act on a flagged item / compose announcement.
- **Empty state:** if onboarding incomplete → a resume-onboarding hero is the dominant element (already present); per-panel empty states described in `METRICS_BRIEF.md`.
- **Data sources:** `dashboard/intelligence` (NEAR-LIVE 60s), `attendance/summary` (LIVE 15s), `notifications/summary` (LIVE 15s), `fees/ledger` (SLOW 300s), `timetable` (SLOW 300s — schedules change rarely), `calendar` (SLOW 300s).
- **Real-time strategy:** pulse = LIVE; everything else NEAR-LIVE/SLOW. Documented in each hook.

### 3.2 Academics
- **Shows:** full academic-health grid (class × subject coverage), attendance trend chart (30-day), marks summary table, full early-warning cohort with signal breakdown.
- **Primary action:** drill into a class/subject cell → side panel; jump to `/admin/marks` for grading.
- **Empty state:** "No syllabus units yet — coverage appears once teachers log their syllabus." CTA → teacher provisioning / `/admin/marks`.
- **Data sources:** `dashboard/intelligence` (academic_health, early_warning, attendance_timeline), `marks/summary`.
- **Real-time strategy:** NEAR-LIVE (60s) for intelligence; SLOW (300s) for marks.

### 3.3 Finance
- **Shows:** collected / due / overdue tiles, recent fee rows, collection-rate vs outstanding visual.
- **Primary action:** view a fee row's context.
- **Empty state:** "No fee records yet." (Read-only on web today — no fee-create endpoint exists; we do not fabricate one.)
- **Data sources:** `fees/ledger`.
- **Real-time strategy:** SLOW (300s) — fee ledger moves slowly.

### 3.4 People
- **Shows:** student count, teacher count, today's attendance rate, at-risk students.
- **Primary action:** add student → `/admin/people?add=student`; open `/admin/people`.
- **Empty state:** "No students yet — import your roster." CTA → bulk import.
- **Data sources:** `students`, `teachers`, `attendance/summary`, `dashboard/intelligence` (early_warning).
- **Real-time strategy:** NEAR-LIVE (60s).

### 3.5 Calendar
- **Shows:** the signature calendar (week default, month toggle), all classes colour-coded, two filter controls, slot drill-down. See `CALENDAR_SPEC.md`.
- **Primary action:** click any slot → drill-down panel.
- **Empty state:** "No schedule entered yet" / "No events this week." Designed, with CTA where an action exists.
- **Data sources:** `timetable` (school-wide periods), `calendar` (events/holidays), `attendance/daily` + `attendance/summary` (today's reality), `syllabus-coverage` / academic_health (covered vs scheduled), homework (per-class, if exposed).
- **Real-time strategy:** schedule + events SLOW (300s, changes rarely); today's per-slot attendance fetched **on-demand** when a slot opens (the drill-down is the live read).

---

## 4. Cross-cutting laws (binding on every tab)

1. **Greeting bar on every tab.** Time-aware (`Good morning/afternoon/evening, <first name>`), first name from `session.name` (split on whitespace), with today's date, academic week (`meta.academic_week`), school name (`meta.school_name`). Hosts **global class filter (Control B)** and **global search**. Always visible. See `PHASE 2` in the brief and `COMPONENT_MAP.md`.
2. **Two independent class filters.**
   - *Control A* — calendar-scoped, lives in the calendar widget header. Collapses the calendar to one class. Affects **only** the calendar.
   - *Control B* — dashboard-global, lives in the greeting bar. Scopes **every panel** to one class. Every scoped panel shows a visible "Scoped to <class>" indicator.
   - They never read or write each other's state. Both are URL-encoded (`?cls=` global, `?calcls=` calendar).
3. **Every number from a real endpoint.** No fabricated aggregates. Where the backend cannot supply a metric, the panel shows a designed empty state, never a fake number or "coming soon" inside a live panel.
4. **Empty states are product.** Every panel has a designed, informative, actionable-where-possible empty state.
5. **URL state for everything.** `tab`, `cls`, `calcls`, `calview` (week|month), `caldate` (anchor date), `panel` (open drill-down id). Deep-linkable & shareable.
6. **Real-time strategy documented per metric** in the hook (LIVE/NEAR-LIVE/SLOW/on-demand) with the reason and the source table.
7. **Mobile is not degraded.** Calendar collapses to a day-list/agenda; drill-down becomes full-screen with a back affordance; tabs become a horizontal scroller; panels stack.
8. **Premium is subtraction.** Cut anything that doesn't serve the principal's workflow.

---

## 5. Global class filter (Control B) — propagation model

The global class filter is a single string (the class name, e.g. `"Grade 5"`), `null` = all classes. It scopes panels **client-side** against data we already hold, because:
- `attendance/summary` returns `by_class[]` → we can filter to one class with zero extra requests.
- `dashboard/intelligence.academic_health.rows[]` is per-class → filter the grid.
- `dashboard/intelligence.early_warning[]` carries `class_name` → filter the cohort.
- `students` accepts a `class` query param → we refetch scoped.
- `fees/ledger` has **no per-class breakdown** on the server. When Control B is active, the Finance panel shows an honest indicator: *"Fee ledger is school-wide; per-class fees aren't broken out by the backend yet."* — we never fake a per-class split.

This client-side scoping is the correct call: it's instant, it avoids N round-trips, and it's truthful about the one place (fees) where the data genuinely isn't class-segmented.

---

## 6. Source-of-truth references

- Server intelligence: `server/.../feature/school/SchoolIntelligenceRouting.kt`
- Server calendar/attendance: `server/.../feature/school/SchoolRouting.kt`
- Server records (attendance/marks/fees summaries): `server/.../feature/school/SchoolRecordsRouting.kt`
- Timetable table: `server/.../db/Tables.kt` → `TeacherPeriodsTable`
- Web data layer: `website/src/lib/admin/{client,hooks,types,session,nav}.ts(x)`
- Web primitives: `website/src/components/admin/Primitives.tsx`, `SidePanel.tsx`, `LivePulse.tsx`
- Design tokens: `website/tailwind.config.ts` (navy/accent/teal/ink/lavender/semantic), mirrored from `composeApp/.../theme/VColors.kt`.
