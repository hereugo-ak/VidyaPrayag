# 07 — MARKS & ASSESSMENT SPEC

> **Scope:** The correct assessment + marks system: assessment creation scoped to class/section/subject, calendar-tied scheduled tests, entry-time validation (max/pass), large-class handling, scheduled-vs-surprise flows, publish discipline, and history/comparison.
> **Source:** `AssessmentsTable`, `AssessmentMarksTable`, `ExamResultsTable` (parallel model), `feature/teacher/TeacherRoutingTasks.kt` (`/marks` GET+POST force-publishes, `/assessments` GET+POST), `TeacherMarksScreenV2.kt`, `TeacherUpdateSelector.kt` (exam picker). Defects: D-ASMT-1..6, B-MK-1..7, F-MK-1..7, X-6.
> **Lens:** Aanya (fears mis-entering / accidental publish) and Mr. Rao (40-student grids, six classes, wants speed + safety).
> **Law:** Saving marks must never mean publishing them. Entry must be validated. An assessment is scoped, dated, and (optionally) calendar-tied — never a free-floating string.
> **Branch:** `backend-by-abuzar_v1.0.3`

---

## 0. Executive Summary

The gradebook has the most dangerous defect in the product:

> **B-MK-1: POST `/marks` force-sets `isPublished=true` and notifies parents.** Every time a teacher *saves* marks — even a half-entered draft, even a typo — it is **immediately published to parents.** There is no draft. There is no review. There is no undo.

Around that core danger sit structural problems:
- **Two parallel marks models** (`AssessmentMarksTable` with numeric `marks` Double, and `ExamResultsTable` with string scores — X-6/D-ASMT-1) → ambiguous source of truth.
- **No entry validation** — nothing stops `marks > maxMarks` (105/100) or negative marks (B-MK-3 / F-MK-3); no pass-mark concept at all (D-ASMT-4).
- **`examDate` is a `varchar12` string** (D-ASMT-2/X-3) — can't sort by date, can't tie to calendar.
- **No calendar tie** — `CalendarEventsTable(type=EXAM)` and `AssessmentsTable` are unrelated (D-ASMT-5) → a "scheduled test" on the school calendar has no gradebook object.
- **`studentId`/`studentName` are free text in marks** (D-ASMT-3) — no FK, names denormalized as truth (X-4).
- **No history/comparison** surface (B-MK-7).
- **Assessment creation is via the shared Update picker's inline `TeacherExamPicker`** (F-SHELL-3) — stateless, easy to scope wrong.

This spec defines: one canonical marks model, a draft→review→publish lifecycle, validated entry, calendar-tied scheduled tests vs surprise tests, large-class ergonomics, and history/comparison.

---

## 1. Data Model — Current vs Target

### 1.1 Current
**`AssessmentsTable` ("assessments"):** schoolId, teacherId?, className, section, subject, name, maxMarks(int=100), examDate(varchar12?), isActive, isPublished(def false), publishedAt?.
**`AssessmentMarksTable` ("assessment_marks"):** assessmentId, studentId(text), studentName(text), marks(double?), enteredBy?. Unique (assessmentId, studentId).
**`ExamResultsTable`:** a *second*, parallel string-scored marks model (X-6).

### 1.2 Defects
- D-ASMT-1 / X-6: duplicate marks models → pick **one** (`assessments` + `assessment_marks`); deprecate/migrate `ExamResultsTable`.
- D-ASMT-2 / X-3: `examDate` string → `date`.
- D-ASMT-3: `studentId` text, `studentName` denormalized → FK to `students`, drop name as truth (join for display).
- D-ASMT-4: no pass mark, no assessment type.
- D-ASMT-5: no calendar link.
- D-ASMT-6: free-text class/section/subject scope (X-1) instead of binding to a class/subject reference.

### 1.3 Target
```
assessments (revised)
  id              uuid pk
  school_id       uuid
  academic_year_id uuid fk -> academic_years
  assignment_id   uuid NULL fk -> teacher_subject_assignments  -- scope binding (X-1/D-ASMT-6)
  class_id        uuid fk -> school_classes      -- canonical scope (not free text)
  section         text
  subject_id      uuid fk -> school_subjects
  teacher_id      uuid fk -> app_users
  name            text NOT NULL
  type            text CHECK (type IN ('scheduled','surprise','assignment','project','exam'))  -- (D-ASMT-4)
  max_marks       numeric NOT NULL CHECK (max_marks > 0)
  pass_marks      numeric NULL CHECK (pass_marks >= 0 AND pass_marks <= max_marks)  -- (D-ASMT-4)
  exam_date       date NULL                       -- TYPED (D-ASMT-2)
  calendar_event_id uuid NULL fk -> calendar_events  -- calendar tie (D-ASMT-5)
  status          text CHECK (status IN ('draft','scheduled','marks_pending','published','archived'))
  published_at    timestamptz NULL
  created_by      uuid

assessment_marks (revised)
  id            uuid pk
  assessment_id uuid fk -> assessments
  student_id    uuid fk -> students              -- FK (D-ASMT-3); name joined for display (X-4)
  marks         numeric NULL CHECK (marks >= 0)   -- per-entry max check in app + trigger
  is_absent     boolean DEFAULT false             -- "AB" distinct from 0
  remark        text NULL
  entered_by    uuid
  entered_at    timestamptz
  UNIQUE (assessment_id, student_id)
-- DB trigger / app guard: marks <= (SELECT max_marks ...) when not absent
```
`ExamResultsTable` → migrated into this model and deprecated (X-6).

---

## 2. Assessment Lifecycle (the publish-discipline fix)

The single most important behavioral change: **a clear status machine** replacing the force-publish.

```
draft ──(schedule for a date)──▶ scheduled ──(date passes / teacher opens entry)──▶ marks_pending
  │                                                                                    │
  └────────────────────────────────(enter marks, Save = stays draft/marks_pending)────┘
                                                                                       │
                                              (explicit Publish, with confirm)─────────▶ published
                                                                                       │
                                                       (unpublish allowed, audited)◀────┘
```

- **Save ≠ publish.** Saving writes `assessment_marks` and keeps status `marks_pending` (or `draft`). **Fixes B-MK-1.**
- **Publish** is a separate, explicit, confirmed action ("Publish to parents? This notifies {n} parents.") → sets `status=published`, `published_at`, *then* notifies parents.
- **Unpublish/correct:** allowed (audited); re-notify only if asked. Protects Aanya from one-tap irreversible mistakes.

Endpoints:
```
POST  /api/v1/teacher/assessments               -- create (scoped); returns draft
GET   /api/v1/teacher/assessments?assignmentId=…&status=…
GET   /api/v1/teacher/assessments/{id}/marks    -- roster + existing marks
PUT   /api/v1/teacher/assessments/{id}/marks    -- SAVE (no publish)  ← fixes B-MK-1
POST  /api/v1/teacher/assessments/{id}/publish  -- explicit publish (+notify)
POST  /api/v1/teacher/assessments/{id}/unpublish
GET   /api/v1/teacher/assessments/history?assignmentId=…  -- trends (B-MK-7)
```

---

## 3. Assessment Creation (scoped)

- **Entry:** Gradebook → **Create assessment** (Doc 04 §5.9), or Classes → class detail → Create assessment (pre-scoped to that class).
- **Scope is pre-filled, not re-picked:** when launched from a class/period, `assignment_id`/`class_id`/`section`/`subject_id` are carried; the shared `TeacherExamPicker` (F-SHELL-3) is removed.
- **Fields:** name, **type** (scheduled / surprise / assignment / project / exam), **max marks** (>0), **pass marks** (0..max, optional), **date** (typed picker; for scheduled, future allowed; for surprise, defaults today), optional **calendar tie**.
- **Validation at create:** max>0; pass≤max; name non-empty; scope valid (teacher owns the assignment via `requireOwnedAssignment`).

---

## 4. Calendar-Tied Scheduled Tests vs Surprise Tests

### 4.1 Scheduled test (calendar-tied)
- Created with `type='scheduled'`, a future `exam_date`, and (optionally) linked to / creating a `calendar_events(type=EXAM, status=PUBLISHED)` so it appears on Today's calendar overlay (Doc 05 §3.3) for the teacher *and* the relevant audience.
- Status `scheduled` until the date arrives or the teacher opens entry → `marks_pending`.
- Today overlay shows "Exam: 7B Maths Unit Test today" → tapping deep-links to the assessment's marks entry. **Closes D-ASMT-5 / B-FEED-1.**

### 4.2 Surprise test
- Created with `type='surprise'`, `exam_date=today`, no calendar event (it's a surprise), goes straight to `marks_pending`.
- Same entry/validation/publish flow.

### 4.3 Admin-scheduled exams
- If an admin published an EXAM calendar event for the teacher's class, the teacher sees it on Today and can **"Create assessment from this exam"** (pre-fills scope + date + `calendar_event_id`). Two-way link.

---

## 5. Marks Entry (validated, large-class ready)

### 5.1 The grid
- Roster (from `enrollments`, ordered by roll) × one numeric field each. DM Mono tabular figures, right-aligned (Doc 10).
- Per-row: name + roll, mark field, **AB** toggle (absent ≠ 0), optional remark.
- Header shows: name · type · **/max** · pass line · **entered {k}/{n}** · status chip.

### 5.2 Entry-time validation (fixes B-MK-3 / F-MK-3)
- **> max_marks:** blocked inline ("max {max}"), field shakes, value not accepted.
- **< 0 / negative:** blocked.
- **Non-numeric:** numeric keypad only; decimals allowed if school config permits (else integer).
- **Pass-mark feedback:** marks below `pass_marks` render in danger ink (passive signal, not a block) so the teacher sees fails at a glance.
- **AB vs 0:** absent is a distinct state (`is_absent=true`), excluded from averages; a real 0 is a 0.

### 5.3 Large-class handling (Mr. Rao, 40×6)
- **Sticky header** with running entered count and /max always visible while scrolling.
- **Tab/next**: entering a mark advances focus to the next student (keyboard "next").
- **Search/jump** to a roll number.
- **Bulk:** "Mark all absent as AB," "Fill remaining with __" (guarded).
- **Autosave draft** every few edits (optimistic) so a phone lock never loses 35 entries; explicit **Save** flushes; **Publish** is separate.
- **Progress persistence:** reopening a `marks_pending` assessment restores entered values (no blank slate — contrast attendance E3).

### 5.4 Save / Publish UX (result-driven)
- **Save** → result-driven submit (`isSubmitting`/`submitSuccess`/`submitError`); toast "Saved (not published)"; status stays `marks_pending`.
- **Publish** → confirm dialog naming parent-notify count → on confirm, publish + notify. Clear visual transition to `published` chip.
- Aanya can never publish by reflex; Mr. Rao gets a fast Save loop and one deliberate Publish at the end.

---

## 6. History & Comparison (B-MK-7)

- **Per-class timeline:** class average per assessment over the term (sparkline/bars, DM Mono axis labels).
- **Per-student trajectory:** a student's marks across assessments (drill from Classes → student profile, Doc 09).
- **Compare two assessments:** side-by-side distribution (e.g. mid-term vs final) — who improved, who dropped.
- **Distribution view:** histogram of an assessment (how many failed/topped) for the teacher to gauge difficulty.
- **Empty:** "Not enough data yet — publish a couple of assessments to see trends."

Endpoint `GET /assessments/history?assignmentId=…` returns the aggregates **computed server-side** (no client N+1; contrast B-CLS aggregate bugs).

---

## 7. Edge Cases (explicit)

| # | Case | Handling |
|---|------|----------|
| M1 | marks > max | blocked at entry (§5.2). |
| M2 | negative / non-numeric | blocked. |
| M3 | student absent for test | AB toggle, excluded from average. |
| M4 | new student added after assessment created | appears in roster with empty mark; enterable. |
| M5 | student transferred out before test | excluded from roster (enrollment-scoped). |
| M6 | accidental publish | Unpublish (audited); optional re-notify suppressed. |
| M7 | partially entered, leave screen | autosaved draft; status stays marks_pending; safe. |
| M8 | duplicate assessment name same class | allowed but warned ("An assessment named X exists for this class"). |
| M9 | calendar exam deleted/unpublished | linked assessment keeps its data; loses overlay chip; warned. |
| M10 | two teachers same subject (co-teaching) | `created_by`/`entered_by` audited; ownership via assignment; both can enter if both own the assignment. |
| M11 | pass_marks not set | pass-fail coloring disabled; no error. |
| M12 | very large class (>60) | virtualized list; sticky header; jump-to-roll. |

---

## 8. Persona Validation

- **Aanya:** creates "Unit Test 1" from 7B class detail (scope pre-filled), max 25, pass 10, today. Enters marks; types 27 by mistake → blocked with "max 25." Saves — sees "Saved (not published)" and relaxes. Reviews, then taps **Publish** → "Publish to 38 parents?" → confirms. No accidental publish, no out-of-range marks.
- **Mr. Rao:** opens a `marks_pending` exam for 8A (40 students), scrolls a sticky-header grid, taps each field, autosave protects him when a call interrupts; fails show in red so he spots the struggling students; one final Publish. Six classes, same muscle memory.

---

## 9. Cross-refs & Dependencies

- One marks model resolves **X-6 / D-ASMT-1**; migration in **Doc 11**.
- Scope binding via `assignment_id` + `requireOwnedAssignment` from **Doc 05 / TeacherAccess**.
- Calendar tie consumes **Doc 05** VP-CAL EXAM events; surfaces on Today (Doc 04 §5.4).
- Publish discipline closes **B-MK-1** (Doc 02) — the highest-severity behavioral defect.
- Grid ergonomics, validation visuals, DM Mono tabular figures in **Doc 10**.
- Student trajectory feeds **Doc 09** student profile.

---

*End of 07_MARKS_AND_ASSESSMENT_SPEC.md*
