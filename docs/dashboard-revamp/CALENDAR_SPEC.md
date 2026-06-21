# Calendar Specification — the signature feature

> The calendar is the product's signature surface. It shows the entire school's day across every class, and lets a principal drill into any moment to see operational reality. This spec is complete before any code is written.

## 0. Honest data foundation (verified)

The calendar fuses **four** real data sources. Every cell, slot, and stat traces to one of them. Nothing is invented.

| Source | Endpoint | What it gives the calendar | Refresh |
|---|---|---|---|
| **Timetable** | `GET /api/v1/school/timetable` *(NEW — see §9)* | The weekly schedule: for each weekday (1–7), each period — class, section, subject, teacher, room, start/end time. School-wide. | SLOW 300s (changes rarely) |
| **Calendar events** | `GET /api/v1/school/calendar?date=&view_type=week\|month` | Holidays, exams, events on specific dates (`academic_calendar` + `holidays`). | SLOW 300s |
| **Attendance reality** | `GET /api/v1/school/attendance/daily?type=student&grade=&date=` and `…/attendance/summary` | For today/past: present vs enrolled per class, and whether attendance was marked. | **On-demand** when a slot opens |
| **Syllabus coverage** | `dashboard/intelligence.academic_health` (`syllabus-coverage` blob) | Covered vs total units per class×subject → "scheduled topic vs covered". | NEAR-LIVE 60s (already loaded) |
| **Homework** *(see §8 honesty note)* | `HomeworkTable` exists; **no school-side read endpoint** today. | Homework-assigned count per class on a date — **only if** we expose it; otherwise the drill-down omits the homework row with an honest "not available" affordance, never a fake count. | — |

### What the timetable can and cannot truthfully claim
- `teacher_periods` is keyed by **weekday (1–7)**, not by calendar date. So the schedule is a **recurring weekly pattern**, correct for any given week. The calendar renders that pattern onto the dates of the viewed week/month. This is honest: a Tuesday slot shows the periods scheduled for Tuesdays.
- Holidays/exam events from `/calendar` are date-specific and **override** the recurring pattern visually (a holiday greys the day; an exam pins a marker).

---

## 1. Default view & layout

- **Default: academic week.** Monday→Sunday columns (or Mon→Sat if the school's working week is 6 days — we infer from which weekdays have periods; days with zero periods across all classes collapse to a thin "no classes" rail rather than wasting a full column).
- **Month toggle:** one click switches to a month grid. Week ⇄ Month is a segmented control in the widget header (`Week | Month`), mirroring the reference screenshots' `Week / Month / Year` control (we ship Week+Month; Year is out of scope because the timetable is a weekly pattern and a year grid adds no operational signal — subtraction).
- **Today** is always visually distinguished: a filled accent ring on the date chip + a subtle column tint in week view.
- **Navigation:** `‹ ›` chevrons move by one week (week view) or one month (month view); a "Today" button snaps back. The anchor date is URL-encoded as `?caldate=YYYY-MM-DD` and `?calview=week|month`.

### Week view (default) — information density
Each day column is a vertical time-ordered stack of **period chips**. Each chip:
- Left 3px rail in the **class colour** (see §3).
- Line 1: `Subject` (bold) · `Class-Section` (e.g. `Grade 5-A`).
- Line 2 (muted): `HH:mm–HH:mm` · teacher first name · room (if present).
- For **today**, a tiny live status dot: green = attendance marked for that class today, amber = not yet, grey = future period.
- Multiple classes in the same time band stack as separate chips; we do **not** merge them. Legibility at a glance is achieved by colour rail + compact two-line chips, capped at a max height with "+N more" overflow that opens the day in the drill-down.

### Month view
Each day cell shows: the date, up to 3 condensed dots (one per class that has periods that weekday, in class colour) + "+N", and any date-specific event badge (holiday/exam). Clicking a day opens that day's agenda in the drill-down panel.

---

## 2. The two filter controls (independent)

### Control A — calendar-scoped class filter
- A compact `<select>`-style dropdown in the **calendar widget header**, labelled "All classes ▾".
- Default: **All classes** (every class's schedule rendered, colour-coded).
- Selecting a class **collapses** the calendar to only that class's schedule across the week. This view is **denser**: chips expand to show the topic scheduled vs covered (from syllabus coverage), the assigned teacher in full, and room — because there's room when only one class is shown. This serves a subject-head / class teacher who wants their week in full.
- Deselect ("All classes") returns to all-classes view.
- **Affects only the calendar widget.** URL: `?calcls=<class>`.

### Control B — dashboard-global class filter
- Lives in the **greeting bar** (top command bar), not the calendar.
- When set, scopes **every panel** on the dashboard (attendance, academic health, early warning, people) to that one class — including the embedded calendar, which respects Control B as a baseline scope.
- Every scoped panel shows a visible "Scoped to <class>" chip.
- Default: all classes (`null`). URL: `?cls=<class>`.

### Interaction between A and B (precisely defined)
- They are **independent state**. Neither mutates the other.
- **Effective calendar scope** = `calcls ?? cls ?? "all"`. That is: Control A wins inside the calendar; if A is "all" but the global B is set, the embedded calendar still honours the global scope (so a globally-scoped dashboard stays consistent), but the user can locally widen the calendar back to "all" via A without touching B. On the dedicated **Calendar tab**, Control A is the primary control and is always visible; the global chip is shown as context.

---

## 3. Class colour coding

- Colours must be **consistent with how the system identifies classes**. The system has no per-class colour field, so we derive a **stable, deterministic** colour from the class name via a hash → index into a curated palette drawn from the design tokens (`accent`, `teal`, `navy`, plus tints) so colours stay on-brand and never collide with semantic colours (success/warning/danger are reserved for data state, never branding — enforced by `VColors.kt` doctrine).
- The same class always gets the same colour across week view, month dots, drill-down header, and legend.
- A **legend** sits under the calendar header listing the classes currently in view with their colour swatch. In single-class (Control A) mode the legend shows just that class.

---

## 4. Slot drill-down panel

Clicking any period chip (or a month-day cell) slides a panel in **from the right** — reusing the existing `SidePanel` component (it already slides from the right, locks scroll, closes on Esc/backdrop, mirrors to `?panel=`). It does **not** displace or reset the calendar; closing it returns to the exact calendar position.

### Panel content — clean hierarchy (top → bottom)
1. **Header:** `Subject` + `Class-Section`, date, time band. A class-colour rail.
2. **Teacher:** name + role. For **today**, a status row: *"Attendance marked ✓ at HH:mm"* or *"Attendance not marked yet"* (derived from `attendance/daily` for that grade+today — if there's any record for that class today, it's marked).
3. **Students present vs enrolled:** `present / enrolled` with a thin bar. Today = live (`attendance/daily`), past = historical (same endpoint with `date=`), future = "Scheduled — N students enrolled" (enrolment from the student roster count for that class; attendance N/A).
4. **Syllabus — scheduled vs covered:** the subject's coverage for that class from `academic_health` (covered units / total units, %). Framed as "Topic progress: X% covered".
5. **Homework:** *if* a school-side homework read is available, show count + titles assigned for that class on that date; otherwise this row is omitted (no fake "0 homework"). See §8.
6. **Notes:** any `event_description` from a calendar event on that date for that class/standard, or leave/announcement context surfaced from the activity feed for that day. If none: "No notes logged."

### States (every one designed)
- **Today (live):** all live reads; status dots; "as of HH:mm".
- **Past (historical):** attendance/coverage as recorded; header tagged "Past".
- **Future (scheduled):** schedule + enrolment only; attendance section reads "Scheduled" with the enrolled count; coverage shows current % (the running coverage to date).
- **Empty:** slot exists in pattern but no teacher/coverage data → "Scheduled, no operational data yet."
- **Loading:** skeleton rows for teacher / attendance / coverage (reuse `Skeleton`).
- **Error:** inline retry (the on-demand attendance read can fail independently of the calendar) — "Couldn't load today's attendance for this class. Retry."

---

## 5. Animation & motion
- Calendar week/month switch: cross-fade + slight scale (Framer Motion, `ease: [0.16,1,0.3,1]`, 0.32s) consistent with `FadeIn`.
- Week/month navigation: horizontal slide of the grid (direction follows nav direction).
- Drill-down: existing `SidePanel` spring-in from right (`x:100%→0`, 0.32s).
- Today's live status dots: a subtle pulse only on the green/amber dots, reusing the `LivePulse` ping treatment. No gratuitous motion.

## 6. Mobile behaviour
- Week view becomes a **vertical agenda**: a day selector (horizontal scroller of date chips, today centered) atop a vertical list of that day's period chips. Tapping a chip opens the drill-down **full-screen** with a back chevron in the header.
- Month view becomes a compact month grid (dots only); tapping a day switches the agenda to that day.
- Both filter controls collapse into a single "Filters" sheet on mobile; the active scope chips remain visible.
- Touch targets ≥ 44px; horizontal scrollers have momentum and snap.

## 7. Accessibility
- Calendar is a labelled region; period chips are buttons with an accessible label: `"<Subject>, <Class-Section>, <time>, <teacher>, <status>"`.
- Keyboard: arrow keys move focus across chips/days; Enter opens drill-down; Esc closes.
- Colour is never the only signal — every class chip also carries text; status uses icon + text, not colour alone.

## 8. Homework honesty note
`HomeworkTable` and `HomeworkSubmissionsTable` exist, and the **teacher** API reads homework, but there is **no school-admin read endpoint** for homework-by-class-by-date. We have two truthful options:
- **(Chosen for this pass)** Omit the homework row in the drill-down rather than show a fabricated/zero value, with a one-line "Homework view coming via the teacher feed" only where genuinely useful — but **never a fake number**.
- (Future) add `GET /api/v1/school/homework?class=&date=` reading `HomeworkTable`. Flagged in `BACKEND_GAPS`-style note; out of scope for the signature-calendar pass unless time permits.

## 9. NEW backend endpoint required: `GET /api/v1/school/timetable`
To render *all classes' schedules* for the admin (today only teacher-scoped reads exist), we add a small, read-only, school-scoped route.

**Request:** `GET /api/v1/school/timetable` (JWT, school-scoped via `requireSchoolContext`). Optional `?class=` to pre-filter server-side.

**Response (envelope `data`):**
```jsonc
{
  "weekdays": [
    {
      "weekday": 1,                 // 1=Mon … 7=Sun (java.time.DayOfWeek.value)
      "periods": [
        {
          "id": "uuid",
          "start_time": "09:00",
          "end_time": "09:45",
          "class_name": "Grade 5",
          "section": "A",
          "subject": "Mathematics",
          "room": "201",
          "teacher_id": "uuid",
          "teacher_name": "Asha Rao"   // joined from app_users.full_name; "" if unassigned
        }
      ]
    }
    // … only weekdays that have ≥1 period are returned; empty = honest empty list
  ],
  "classes": ["Grade 4", "Grade 5", "Grade 6"]  // distinct class_name present, sorted — feeds Control A/B options
}
```
- **Source:** `TeacherPeriodsTable` (school-wide, `schoolId eq ctx.schoolId`), left-joined to `AppUsersTable` for `teacher_name`.
- **Empty:** if a school hasn't entered a timetable, `weekdays: []`, `classes: []` → calendar shows its designed empty state. Never fabricated.
- **Sort:** periods within a weekday sorted by `start_time` then `position`.
- **Refresh strategy (documented in the hook):** SLOW (300s). Timetables change on the order of terms, not minutes.

This is the single new backend surface the signature calendar needs, and it is purely additive and read-only.
