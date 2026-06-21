# Component Map — nothing gets built that isn't here

> Every new/modified component: props interface, data source, variants, composition. Existing components are reused, not reinvented. Path root: `website/src/components/admin/` unless noted.

## Conventions (inherited from the existing system)
- Surfaces use `Card` / `CardHeader` / `Badge` / `Skeleton` / `EmptyState` / `FadeIn` / `Avatar` from `Primitives.tsx`. **Do not** introduce a parallel card system.
- Side panels use `SidePanel.tsx` (right slide-in, Esc/backdrop close, `?panel=` mirror).
- Tokens: `navy`, `accent`, `teal`, `ink(.2/.3/.placeholder)`, `lavender(.soft/.tint)`, `success/warning/danger` — semantic colours are **data-state only**, never branding (per `VColors.kt` doctrine).
- Data via SWR hooks in `lib/admin/hooks.ts`; never `fetch` directly in a component (except the on-demand calendar attendance read, which goes through `adminApi`).
- All client state that should be shareable lives in the **URL** via a small `useDashboardUrlState` hook (new).

---

## 1. Shell & navigation

### `DashboardWorkspace` (NEW) — `app/admin/dashboard/page.tsx` (rewritten)
- **Role:** owns URL state, renders `GreetingBar` + `TabStrip` + active tab content.
- **State (URL):** `tab`, `cls` (global), `calcls` (calendar), `calview`, `caldate`, `panel`.
- **Composition:** `GreetingBar` › `TabStrip` › one of `OverviewTab | AcademicsTab | FinanceTab | PeopleTab | CalendarTab`.

### `useDashboardUrlState` (NEW) — `lib/admin/useDashboardUrlState.ts`
- **Props/return:** `{ tab, setTab, globalClass, setGlobalClass, calClass, setCalClass, calView, setCalView, calDate, setCalDate, panel, openPanel, closePanel }`.
- **Impl:** `useSearchParams` + `useRouter().replace` with a serialized query; debounced writes; defaults applied on read.
- **Source:** none (pure URL).

### `GreetingBar` (NEW, replaces/augments `CommandBar`) — `GreetingBar.tsx`
- **Props:** `{ meta?: IntelligenceMeta; sessionName: string; classes: string[]; globalClass: string|null; onGlobalClass(c:string|null): void; onSearch(q:string):void; onCompose():void }`.
- **Renders:** time-aware greeting (`Good morning/afternoon/evening, <first name>`), date, academic week, school name; **global class filter (Control B)** as `ClassFilter`; **global search**; the command actions (re-using `CommandBar`'s action buttons).
- **Source:** `session.name` (greeting) + `dashboard/intelligence.meta` (date/week/school).
- **Variants:** scoped (global class set → shows active scope chip) / unscoped.

### `TabStrip` (NEW) — `TabStrip.tsx`
- **Props:** `{ tab: TabId; onTab(t:TabId):void }`.
- **Renders:** Overview / Academics / Finance / People / Calendar pills. Mobile = scroller.

### `ClassFilter` (NEW) — `ClassFilter.tsx`
- **Props:** `{ classes: string[]; value: string|null; onChange(v:string|null):void; label?: string; size?: "sm"|"md" }`.
- **Renders:** accessible dropdown; "All classes" default; current value chip. Used by **both** Control A (calendar header) and Control B (greeting bar) — same component, independent state.
- **Source:** `timetable.classes` (preferred) ∪ `attendance/summary.by_class[].grade` ∪ `academic_health.rows[].class_name` (union, deduped) — so the option list is real even if a timetable isn't entered.

### `ScopeBadge` (NEW) — small inline indicator
- **Props:** `{ className: string }` → renders `Scoped to <class>` chip (accent tone) on any globally-scoped panel.

---

## 2. The signature calendar

### `SchoolCalendar` (NEW) — `calendar/SchoolCalendar.tsx`
- **Props:** `{ timetable?: TimetableDto; events?: CalendarResponse; todaySummary?: AttendanceSummaryDto; health?: AcademicHealth; view: "week"|"month"; anchorDate: string; classScope: string|null; classes: string[]; onView(v):void; onAnchor(d):void; onClassScope(c):void; onOpenSlot(slot: CalendarSlot):void; loading: boolean }`.
- **Renders:** header (`Week|Month` toggle, `‹ Today ›`, Control A `ClassFilter`), legend (`CalendarLegend`), and `WeekGrid` or `MonthGrid`.
- **Variants:** all-classes / single-class (denser chips); week / month; today-distinguished; loading (skeleton grid); empty (`EmptyState`).
- **Composition:** `CalendarHeader` › `CalendarLegend` › (`WeekGrid` | `MonthGrid`) › chips (`PeriodChip`).
- **Data:** assembled by the tab from `timetable` + `calendar` + `attendance/summary` + `academic_health`.

### `WeekGrid` / `MonthGrid` (NEW) — `calendar/WeekGrid.tsx`, `calendar/MonthGrid.tsx`
- **Week props:** `{ days: CalendarDay[]; today: string; onOpenSlot }`. Day = `{ date, weekday, periods: PeriodChipModel[], events: CalendarEventDto[], isHoliday, status }`.
- **Month props:** `{ weeks: CalendarDay[][]; today; onOpenDay }`.

### `PeriodChip` (NEW) — `calendar/PeriodChip.tsx`
- **Props:** `{ model: PeriodChipModel; dense: boolean; onClick() }`.
- **PeriodChipModel:** `{ id, subject, className, section, start, end, teacherName, room, classColor, liveStatus: "marked"|"pending"|"future"|"none" }`.
- **Variants:** dense (single-class view) shows coverage line; compact (all-classes) two lines; status dot.

### `CalendarLegend` (NEW) — `calendar/CalendarLegend.tsx`
- **Props:** `{ classes: {name:string; color:string}[] }`.

### `classColor` util (NEW) — `lib/admin/classColor.ts`
- **Signature:** `classColor(className: string): { rail: string; dot: string; tintBg: string }`.
- **Impl:** deterministic hash → index into a curated on-brand palette (accent/teal/navy tints), never semantic colours.

### `CalendarSlotPanel` (NEW) — `calendar/CalendarSlotPanel.tsx`
- **Props:** `{ open: boolean; slot: CalendarSlot|null; onClose() }`.
- **Renders (in `SidePanel`):** header (subject·class, date, time, class-colour rail) › teacher + today's attendance-marked status › present/enrolled bar › syllabus coverage › notes. Homework row only if a real source exists (else omitted — never fake).
- **Data:** **on-demand** `adminApi.attendanceDaily(class, date)` (NEW client method) for present/enrolled + marked-status; coverage from `health`; enrolment from roster count.
- **States:** today-live / past / future-scheduled / empty / loading (skeleton) / error (inline retry).
- **CalendarSlot type:** `{ date, weekday, className, section, subject, teacherId, teacherName, start, end, room, kind: "today"|"past"|"future" }`.

---

## 3. Tab content components (each composes existing + new panels)

### `OverviewTab` (NEW) — `tabs/OverviewTab.tsx`
- **Props:** `{ scope: DashboardScope }` (scope = `{ globalClass, calClass, calView, calDate, openPanel }` + the SWR data bundle).
- **Composition:** `LivePulse` · `SchoolCalendar` (week, embedded) · `AttendanceIntelligence` · `EarlyWarning` · `FinancialPulse` · `AcademicHealth` (preview) · `ActivityFeed` · `PeopleSnapshot`. Existing components reused as-is, extended only to accept an optional `scopeClass` prop + render `ScopeBadge`.

### `AcademicsTab` (NEW) — `tabs/AcademicsTab.tsx`
- **Composition:** `AcademicHealth` (full) · `AttendanceIntelligence` (full) · `EarlyWarning` (full) · `MarksSummaryPanel` (NEW, from `marks/summary`).

### `FinanceTab` (NEW) — `tabs/FinanceTab.tsx`
- **Composition:** `FinancialPulse` (full) · `FeeLedgerTable` (NEW or extracted from FinancialPulse) · scope note when globalClass set.

### `PeopleTab` (NEW) — `tabs/PeopleTab.tsx`
- **Composition:** `PeopleSnapshot` · `EarlyWarning` · `RosterPreview` (NEW, from `students`) · `StaffPreview` (NEW, from `teachers`).

### `CalendarTab` (NEW) — `tabs/CalendarTab.tsx`
- **Composition:** `SchoolCalendar` (full, Control A primary) · `CalendarSummaryRail` (NEW, from `calendar.summary` + timetable counts).

### `MarksSummaryPanel` (NEW) — `MarksSummaryPanel.tsx`
- **Props:** `{ data?: MarksSummaryDto; loading; scopeClass?: string|null }`. Table of assessments; empty "No assessments graded yet."

---

## 4. Existing components — reuse & minimal extension
| Component | Change |
|---|---|
| `LivePulse` | + optional `live` already present; pass scope-adjusted metrics. No structural change. |
| `AttendanceIntelligence` | + optional `scopeNote?` (since trend is school-wide). Renders `ScopeBadge`-style note when globally scoped. |
| `EarlyWarning` | + optional `scopeClass` to filter rows + `variant: "preview"|"full"`. |
| `AcademicHealth` | + `variant: "preview"|"full"` + `scopeClass`. |
| `FinancialPulse` | + `scoped` flag to show the honest "school-wide" note. |
| `ActivityFeed`, `PeopleSnapshot`, `QuickCompose`, `CommandBar` | reused; `CommandBar` actions are absorbed into `GreetingBar`. |
| `SidePanel` | reused verbatim for `CalendarSlotPanel`. |

---

## 5. Data layer additions (`lib/admin/`)
| File | Addition |
|---|---|
| `types.ts` | `TimetablePeriod`, `TimetableWeekday`, `TimetableDto`; `CalendarEventDto`, `CalendarSummaryDto`, `CalendarResponse`; `AttendanceDailyEntry`, `AttendanceDailyResponse`. |
| `client.ts` | `adminApi.timetable(className?)`, `adminApi.calendar(date, viewType)`, `adminApi.attendanceDaily(type, grade, date)`. |
| `hooks.ts` | `useTimetable()` (SLOW), `useCalendar(date, view)` (SLOW). Each with the REFRESH-STRATEGY doc comment. (attendanceDaily is on-demand → not a polling hook; called from the slot panel.) |
| `classColor.ts` | deterministic class→colour. |
| `useDashboardUrlState.ts` | URL state hook. |

## 6. Backend addition (`server/`)
| File | Addition |
|---|---|
| `feature/school/SchoolTimetableRouting.kt` (NEW) | `GET /api/v1/school/timetable` — school-scoped read of `TeacherPeriodsTable` joined to `AppUsersTable`. Response per `CALENDAR_SPEC §9`. Registered in `Application.kt` routing. |

That's the complete surface. Implementation proceeds tab-by-tab and commit-by-commit per the brief's sequence.
