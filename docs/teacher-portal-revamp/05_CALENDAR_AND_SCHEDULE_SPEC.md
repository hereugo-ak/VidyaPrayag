# 05 — CALENDAR & SCHEDULE SPEC

> **Scope:** The weekly timetable data model (audit + target) and the schedule/calendar interaction that anchors the teacher's daily workflow on the **Today** tab. Covers `TeacherPeriodsTable`, `CalendarEventsTable`/`AcademicYearsTable` (VP-CAL), `HolidayListTable`, and the read path in `SchoolTimetableRouting.kt`.
> **Source:** `db/Tables.kt`, `feature/school/SchoolTimetableRouting.kt`, `ParentScheduleCard.kt` (interaction reference). Defects from Docs 01–02 (D-TT-1..5, B-FEED-1, B-FEED-2).
> **Lens:** Aanya (needs "where am I now?") and Mr. Rao (needs "my whole week, big and legible").
> **Law:** The schedule is the spine of the day. Every during-class action hangs off a period; if the timetable is wrong or empty, the whole portal is hollow (X-5, B-FEED-2).
> **Branch:** `backend-by-abuzar_v1.0.3`

---

## 0. Executive Summary

The timetable is the **single most load-bearing data structure** in the teacher portal — Doc 04's entire "context inference" depends on it. Today it is:
- **Stored** in `TeacherPeriodsTable` as a recurring weekly pattern keyed by `weekday` (1=Mon..7=Sun) with `startTime`/`endTime` as `varchar8` "HH:mm" strings.
- **Read** school-wide via `GET /api/v1/school/timetable` (`SchoolTimetableRouting.kt`).
- **Written**: *no clear teacher-facing write path exists* (D-TT-4 / B-FEED-2) — and crucially **no seed data** populates it (X-5), so a fresh teacher's Today tab has **no periods at all**, which cascades into "no current period → no scoped attendance → fabricated tasks."

The calendar (VP-CAL: `CalendarEventsTable` + `AcademicYearsTable`) is a *separate, richer* platform with typed events (EXAM/HOLIDAY/PTM…), DRAFT/PUBLISHED lifecycle, audience targeting, and date ranges — but it is **not surfaced anywhere in the teacher portal** (B-FEED-1). The teacher cannot see "exam today," "holiday tomorrow," "PTM Friday." There is also a parallel `HolidayListTable` (D-TT-5) creating two sources of holiday truth.

This spec defines: (1) the corrected timetable model (typed times, FK binding to TSA, term validity, exceptions), (2) the timetable write/seed path, (3) the merge of timetable + VP-CAL + holidays into a single **resolved day**, and (4) the schedule interaction on Today (a 3-face swipe card modeled on `ParentScheduleCard`).

---

## 1. Timetable Data Model — Current State Audit

### 1.1 `TeacherPeriodsTable` ("teacher_periods") as built
| Column | Type | Notes / Defect |
|--------|------|----------------|
| schoolId | uuid | ok |
| teacherId | uuid | ok |
| weekday | int (1=Mon..7=Sun) | ok, but ISO ambiguity not documented (D-TT-1) |
| startTime / endTime | varchar8 "HH:mm" | **string time, no validation** (D-TT-2). No timezone; relies on school-local. |
| className | text | **free-text class identity (X-1)** — drifts vs TSA/Students ("7"/"Grade 7"/"VII"). |
| section | text | free-text section (X-1). |
| subject | text | free-text; not FK to `SchoolSubjectsTable` (D-TT-3 / X-1). |
| room | text (def '') | ok |
| position | int | ordering within a day; redundant if sorted by startTime (D-TT-1). |

**Defects (from Doc 01):**
- **D-TT-1** ordering ambiguity: both `position` and `startTime` order periods; no single source.
- **D-TT-2** string times: cannot reliably compute "current period" / overlaps; `lastIndexOf`-style parsing fragile.
- **D-TT-3** subject as free text, not FK → duplicate/inconsistent subjects.
- **D-TT-4** **no binding to `TeacherSubjectAssignmentsTable`** — a period and the assignment it implements are not linked. So "the class I'm teaching now" cannot be tied to the TSA row that authorizes attendance/marks (overlaps X-1).
- **D-TT-5** holiday duplication: `HolidayListTable` vs `CalendarEventsTable(type=HOLIDAY)` — two truths.
- **No term/validity window:** a period is "forever every Monday"; no academic-term start/end, no mid-term revisions.

### 1.2 Read path — `SchoolTimetableRouting.kt`
- `GET /api/v1/school/timetable` reads `teacher_periods` **school-wide**, recurring weekly by `weekday`.
- Returns the pattern; the client derives "today" by current weekday and "current period" by string-time comparison.
- **No teacher-scoped endpoint** that returns *just my* timetable resolved for *a specific date* with holiday/calendar overlay applied (B-FEED-1/2). The teacher portal's Today tab cannot ask "what is my resolved schedule for 2026-06-22?"

### 1.3 Seed state — the silent killer
Grep confirms **no seed rows** for `teacher_periods` in `Seed.kt`/`DemoSeed.kt` (X-5). Therefore on a fresh Supabase + fresh teacher:
- Today's Now/Next card has nothing → falls back to fabricated tasks (B-HOME-4).
- Class context inference (Doc 04 §7) is impossible.
- "Mark attendance for this period" has no period to scope to.

**This is the highest-leverage data fix in the entire rebuild** — without seeded/authorable timetable, the new Today tab is empty.

---

## 2. Timetable Data Model — Target

### 2.1 Typed, bound, term-scoped periods
Promote `teacher_periods` to a properly typed, FK-bound table (DDL belongs in Doc 11 migration set; shape here):

```
teacher_periods (revised)
  id              uuid pk
  school_id       uuid  fk -> schools
  academic_year_id uuid fk -> academic_years        -- NEW: term validity
  assignment_id   uuid  fk -> teacher_subject_assignments  -- NEW (D-TT-4): binds period to the TSA it implements
  teacher_id      uuid  fk -> app_users              -- denormalized for fast "my schedule" (kept in sync via assignment)
  weekday         smallint  CHECK (weekday BETWEEN 1 AND 7)  -- ISO: 1=Mon..7=Sun (documented)
  start_time      time NOT NULL                      -- TYPED (D-TT-2)
  end_time        time NOT NULL  CHECK (end_time > start_time)
  room            text DEFAULT ''
  -- className/section/subject DROPPED as source of truth; derived via assignment_id (X-1 fix)
  valid_from      date NULL                          -- term revisions
  valid_to        date NULL
  is_active        boolean DEFAULT true
  UNIQUE (school_id, teacher_id, weekday, start_time)  -- no double-booking
```

**Why bind to `assignment_id`:** a period now *points at* the exact TSA row that authorizes the teacher for that class/section/subject. "Current period" → `assignment_id` → `requireOwnedAssignment` (TeacherAccess) → attendance/marks scope is **automatic and authorized**. This is the linchpin of Doc 04 §7 context inference and kills the free-text drift (X-1) for the schedule.

### 2.2 Period exceptions (one-off changes)
Recurring weekly patterns break in reality (substitution, cancelled class, room change, exam slot). Add:

```
period_exceptions
  id            uuid pk
  school_id     uuid
  period_id     uuid fk -> teacher_periods NULL   -- which recurring period this overrides
  date          date NOT NULL
  kind          text  -- CANCELLED | RESCHEDULED | ROOM_CHANGE | SUBSTITUTION | EXTRA
  new_start     time NULL
  new_end       time NULL
  new_room      text NULL
  substitute_teacher_id uuid NULL
  note          text DEFAULT ''
```

This lets Today resolve a *specific date* correctly: a normally-scheduled Monday 7B Maths can be CANCELLED for one date (so attendance is not expected — Doc 06 edge case), or a SUBSTITUTION inserted.

### 2.3 Holiday truth — single source
Resolve D-TT-5: make `CalendarEventsTable(type=HOLIDAY, status=PUBLISHED)` the **single source** for holidays; deprecate `HolidayListTable` (migrate its rows into calendar events in Doc 11). The resolved-day computation reads holidays only from VP-CAL.

---

## 3. VP-CAL (Calendar) — Audit & Role

### 3.1 Tables (from Doc 01)
- `academic_years` — term boundaries; periods and assessments should reference an academic year.
- `calendar_events` — `type` (EXAM | HOLIDAY | PTM | EVENT | …), `status` (DRAFT | PUBLISHED | …), `startDate`/`endDate` (varchar12 — string, **same X-3 disease**), `audience`, notify flags.

### 3.2 Defects
- **B-FEED-1:** calendar events are **never surfaced** in the teacher portal. The teacher is blind to exams/holidays/PTMs that the admin published.
- **X-3 (dates as strings):** `startDate`/`endDate` are `varchar12` → cannot reliably range-query "events overlapping today." Target: `date` columns.
- **No teacher-relevant filtering:** events have `audience` but the teacher portal has no endpoint that returns "events relevant to me on date D" (my classes' exams + school-wide holidays/PTMs).

### 3.3 Target role of VP-CAL in teacher portal
The calendar **overlays** the timetable; it does not replace it:
- **HOLIDAY** (published) → the whole day is "no school"; Today shows holiday banner; no attendance expected (Doc 06 edge case "holiday").
- **EXAM** tied to a teacher's class/subject → appears on Today's overlay AND deep-links to the Gradebook assessment (Doc 07 calendar-tied scheduled tests).
- **PTM / EVENT** → informational chip on Today.

---

## 4. The Resolved Day — server computation

Introduce a teacher-scoped, date-parameterized endpoint that does the merge server-side (kills client-side string-time fragility and B-FEED-1/2):

```
GET /api/v1/teacher/day?date=YYYY-MM-DD   (auth: teacher)
→ {
    date, weekday,
    isHoliday: bool, holidayName: string?,            -- from calendar_events(HOLIDAY,PUBLISHED)
    periods: [                                         -- from teacher_periods + period_exceptions resolved
      { periodId, assignmentId, className, section, subject, room,
        startTime, endTime,                            -- typed times serialized HH:mm
        status: SCHEDULED|CANCELLED|RESCHEDULED|SUBSTITUTION,
        attendanceMarked: bool,                        -- real per-period attendance state (fixes B-HOME-4)
        substituteTeacherName: string? }
    ],
    calendar: [                                        -- relevant published events for this date
      { eventId, type, title, audience, classRef? } ],
    nowIndex: int?,                                    -- index of current period (server clock, authoritative)
    nextIndex: int?
  }
```

**Why server-resolved:**
1. Authoritative clock & timezone (no device-clock drift between Aanya's phone and the school).
2. Holiday/exception merge in one place (no duplicated client logic).
3. `attendanceMarked` computed by joining `attendance_records` for each period's class+section+date → Today's badges are **real**, not fabricated (B-HOME-4 fixed).
4. `assignmentId` carried through → "Mark attendance" CTA is pre-authorized (Doc 04 §7, Doc 06).

Also expose:
```
GET /api/v1/teacher/week        → resolved weekly pattern (for Profile → My Schedule, Doc 04 §5.14)
POST/PATCH /api/v1/teacher/periods (or school-admin authored)  → timetable write path (B-FEED-2)
```
Authoring of timetable is typically an **admin** responsibility; minimum viable: an admin-side write + a **seed** (X-5) so every demo/fresh school has a realistic week. Teacher-side may at most request changes (period_exceptions: "I need a substitute").

---

## 5. Schedule Interaction on Today (the daily anchor)

Model on `ParentScheduleCard.kt` (the parents portal schedule card has **three faces** the user swipes between: *current*, *timeline*, *weekly*). The teacher schedule card mirrors this for cross-portal consistency (Doc 10) and because it solves both personas at once.

### 5.1 Face A — NOW / NEXT (default)
- **Purpose:** answer "what am I teaching right now / next" instantly.
- **Surfaces:** current period emphasized (class+section · subject · room · time · live "now" / "ends in 12 min"), next period muted; **attendanceMarked badge** (✓/!).
- **CTAs:** [Mark attendance] [Syllabus] [Homework] — all pre-scoped via `assignmentId`.
- **Empty:** holiday → "Holiday: {name} — no classes today"; no-timetable → "Your timetable isn't set up yet" (honest, distinct messages — never a fake card).
- **Refresh:** minute tick recomputes now/next locally using server `nowIndex` as anchor; foreground refetches `/day`.

### 5.2 Face B — TIMELINE (today, full)
- **Purpose:** the whole day in order.
- **Surfaces:** vertical timeline of all periods; current emphasized; cancelled periods struck through with reason; calendar chips (EXAM/PTM) inline at their times.
- **CTAs:** tap a period → period sheet (its scoped actions + marked badge).
- **Empty:** holiday banner spanning the day.

### 5.3 Face C — WEEKLY (this week)
- **Purpose:** orientation across the week (Mr. Rao's "show me everything, big").
- **Surfaces:** 5–6 column grid (Mon–Sat) × period rows; today's column highlighted; tap a cell → that period's detail.
- **Empty:** "No timetable set up."
- **Note:** also reachable full-screen from Profile → My Schedule for an even larger, scrollable layout (accessibility for 58yo — Doc 10).

### 5.4 Interaction mechanics
- **Swipe** horizontally between faces A/B/C with a face indicator (3 dots), exactly like ParentScheduleCard, with haptics.
- **Live "now" line** in faces B/C drawn at the server-anchored current time.
- **Tap-through always pre-scopes** the destination tool — no picker.

---

## 6. Edge Cases the Resolved Day Must Handle

| Case | Resolution |
|------|------------|
| Holiday today | `isHoliday=true`; Today shows banner; no periods expected; attendance not solicited (Doc 06). |
| Period cancelled (one-off) | `period_exceptions(kind=CANCELLED)`; period shown struck-through; attendance not expected for it. |
| Substitution | period shown with `substituteTeacherName`; if *I* am the substitute, it appears in *my* day with scope to mark. |
| Two periods overlap (data error) | server flags overlap; client shows warning chip; does not silently drop one. |
| No timetable seeded | distinct "not set up" state, never a fabricated period (X-5 honesty). |
| Exam slot replaces a period | calendar EXAM event + (optional) CANCELLED exception; Today shows exam chip and deep-links to Gradebook. |
| Day spans term boundary | `valid_from/valid_to` on periods filters out periods not valid on `date`. |
| Device clock wrong | server `nowIndex`/`nextIndex` are authoritative; client clock only animates the "now" line. |

---

## 7. Persona Validation

- **Aanya:** opens Today, Face A says "7B Maths · now · ! unmarked," taps Mark attendance — no thinking about *which* class. Swipes to Face C once on Monday to learn her week. Anxiety reduced: the system tells her where she is.
- **Mr. Rao:** lands on Face A; swipes once to Face C (weekly grid, large cells) to see all 6 classes at a glance; taps Friday's PTM chip to read details. Never parses a string time or re-picks a class.

---

## 8. Dependencies & Cross-refs

- Powers **Doc 04** context inference (§7) and the Today tab (§5.1–5.5).
- `assignmentId` binding feeds **Doc 06** (attendance scope) and **Doc 07** (marks scope) via `requireOwnedAssignment`.
- Calendar EXAM events feed **Doc 07** (calendar-tied scheduled tests).
- HOLIDAY single-source feeds **Doc 06** holiday edge case.
- Seed requirement (X-5) is an explicit task in **Doc 11**.

---

*End of 05_CALENDAR_AND_SCHEDULE_SPEC.md*
