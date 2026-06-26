# 08 — SYLLABUS & HOMEWORK SPEC

> **Scope:** Syllabus (a tracking interaction lighter than a form) and Homework (assign with attachment + due-date picker, the no-submit-past-due rule with teacher extension, and a submissions status board).
> **Source:** `SyllabusUnitsTable`, `HomeworkTable`, `HomeworkSubmissionsTable`, `feature/teacher/TeacherRoutingTasks.kt` (`/syllabus` GET+PATCH — no create; `/homework` GET+POST — no board/extend), `TeacherSyllabusScreenV2.kt`, `TeacherHomeworkScreenV2.kt` (dead `onAssign = { /* Phase 3E */ }`). Defects: D-SYL-1..4, D-HW-1..5, B-SYL-1..3, B-HW-1..6, F-SYL-1..4, F-HW-1..4.
> **Lens:** Aanya (wants to log coverage in seconds between activities) and Mr. Rao (wants a glanceable "who didn't submit" board, large text).
> **Law:** Syllabus must be lighter than a form — a tap. Homework must be a real lifecycle, not a dead button.
> **Branch:** `backend-by-abuzar_v1.0.3`

---

## 0. Executive Summary

**Syllabus** is conceptually close to right (a checklist of units you toggle covered) but is crippled by data + endpoint gaps:
- **No create endpoint** (B-SYL-1) — `/syllabus` supports GET + PATCH(toggle) only. Units must already exist, and **none are seeded** (X-5) → the screen lands **empty with nothing to do** (F-SYL-2).
- Scope is free-text class/section/subject (X-1, D-SYL-3); `coveredOn` is a `varchar12` string date (D-SYL-2/X-3).
- The toggle interaction itself is fine and should be preserved and made *lighter* (F-SYL-1: keep it a single tap, never a modal form).

**Homework** is the most visibly broken surface:
- **The primary button is dead:** `onAssign = { /* Phase 3E */ }` (F-HW-1) — you literally cannot assign homework from the UI.
- Backend has `/homework` GET + POST but **no submissions board, no extension, no attachment handling** (B-HW-3/4/5/6).
- `HomeworkSubmissionsTable` exists (status submitted|graded|late) but is **never surfaced** (B-HW-3) and is **student-written only** in concept — the teacher cannot see or act on it.
- `dueDate` is a string (D-HW-2/X-3); there is no rule preventing past-due submission and no teacher extension (D-HW-4/5).

This spec defines: a syllabus model with create/reorder and a one-tap toggle; a homework lifecycle with assign (attachment + typed due-date), the no-submit-past-due rule + teacher extension, and a real submissions board.

---

## PART A — SYLLABUS

## 1. Data Model — Current vs Target

### 1.1 Current `SyllabusUnitsTable` ("syllabus_units")
schoolId, className, section, subject, title, position(int), isCovered(bool), coveredOn(varchar12?), coveredBy?.
- **D-SYL-1:** no create path (B-SYL-1); units must be pre-inserted; none seeded (X-5).
- **D-SYL-2:** `coveredOn` string date (X-3).
- **D-SYL-3:** free-text scope (X-1).
- **D-SYL-4:** flat list — no chapter/unit hierarchy, no link to subject curriculum.

### 1.2 Target — curriculum units
```
curriculum_units            -- the syllabus template (per class+subject)
  id            uuid pk
  school_id     uuid
  class_id      uuid fk -> school_classes
  subject_id    uuid fk -> school_subjects
  parent_id     uuid NULL fk -> curriculum_units   -- chapter ▸ topic hierarchy (D-SYL-4)
  title         text NOT NULL
  position      int
  is_active     boolean DEFAULT true

syllabus_progress           -- coverage state, per section
  id            uuid pk
  unit_id       uuid fk -> curriculum_units
  section       text
  assignment_id uuid fk -> teacher_subject_assignments  -- who/which class (scope, X-1)
  is_covered    boolean DEFAULT false
  covered_on    date NULL                         -- TYPED (D-SYL-2)
  covered_by    uuid NULL
  note          text NULL
  UNIQUE (unit_id, section, assignment_id)
```
Separating the **template** (curriculum_units) from **progress** (syllabus_progress) lets two sections of the same class track coverage independently, and lets an admin seed the curriculum once (X-5).

## 2. Interaction — "lighter than a form" (F-SYL-1)

- **Surface:** Planner → Syllabus (Doc 04 §5.12), scoped to a class+subject (defaults to last-used / current period).
- **The core gesture is a single tap on a unit row** → toggles `is_covered`, stamps `covered_on=today`, `covered_by=me`, optimistic. No modal, no form, no save button. This is the whole point.
- **Progress bar** at top: "12 / 30 units covered (40%)."
- **Hierarchy:** chapters expand to topics; toggling a chapter offers "mark all topics covered?" (one confirm).
- **Secondary (deliberate, behind a long-press / edit mode):** add unit, rename, reorder (drag), add a note/date-other-than-today. These are rare and must not clutter the one-tap path.
- **Add first unit:** when empty (no curriculum seeded), show "No syllabus yet" + a single **Add unit** affordance (fixes B-SYL-1 dead-empty F-SYL-2) — creating goes through the new POST.

## 3. Syllabus Endpoints

```
GET   /api/v1/teacher/syllabus?assignmentId=…        -- units + progress, hierarchical
POST  /api/v1/teacher/syllabus/units                 -- create unit (fixes B-SYL-1)
PATCH /api/v1/teacher/syllabus/units/{id}            -- rename/reorder
PATCH /api/v1/teacher/syllabus/progress              -- toggle covered (the one-tap), typed covered_on
```
Authorized via `requireOwnedAssignment`. Toggle is idempotent and optimistic.

## 4. Syllabus Edge Cases
| Case | Handling |
|------|----------|
| No curriculum seeded | "No syllabus yet — add your first unit." (not blank/dead, F-SYL-2) |
| Two sections, same class | independent progress via `(unit, section, assignment)` |
| Untoggle (marked by mistake) | tap again → uncovered; clears covered_on |
| Covered on a past date | edit-mode note lets you set `covered_on` ≠ today |
| Reorder | drag in edit mode; persists `position` |

---

## PART B — HOMEWORK

## 5. Data Model — Current vs Target

### 5.1 Current
**`HomeworkTable`:** schoolId, teacherId?, className, section, subject, title, description(def ''), dueDate(varchar12), isActive.
**`HomeworkSubmissionsTable`:** homeworkId, studentId(text), status(varchar16 def 'submitted' — submitted|graded|late), submittedAt. Unique(homeworkId, studentId).

### 5.2 Defects
- D-HW-1: free-text scope (X-1).
- D-HW-2: `dueDate` string (X-3) — no date math, no "is it past due?".
- D-HW-3: no attachment column/handling (B-HW-5).
- D-HW-4: no "no submit past due" rule.
- D-HW-5: no teacher extension mechanism.
- B-HW-1: assign button dead (F-HW-1).
- B-HW-3: submissions never surfaced to teacher.
- B-HW-6: `studentId` text, no FK.

### 5.3 Target
```
homework (revised)
  id            uuid pk
  school_id     uuid
  assignment_id uuid fk -> teacher_subject_assignments   -- scope (X-1)
  class_id      uuid fk -> school_classes
  section       text
  subject_id    uuid fk -> school_subjects
  teacher_id    uuid
  title         text NOT NULL
  description   text DEFAULT ''
  due_date      date NOT NULL                     -- TYPED (D-HW-2)
  due_time      time NULL                         -- optional cutoff time
  allow_late    boolean DEFAULT false             -- teacher policy
  is_active     boolean DEFAULT true
  created_at    timestamptz

homework_attachments                              -- (D-HW-3 / B-HW-5)
  id            uuid pk
  homework_id   uuid fk -> homework
  url           text
  filename      text
  mime          text
  size_bytes    bigint
  uploaded_by   uuid

homework_submissions (revised)
  id            uuid pk
  homework_id   uuid fk -> homework
  student_id    uuid fk -> students               -- FK (B-HW-6)
  status        text CHECK (status IN ('submitted','late','graded','not_submitted'))
  submitted_at  timestamptz NULL
  grade         text NULL
  reviewed_by   uuid NULL
  reviewed_at   timestamptz NULL
  UNIQUE (homework_id, student_id)

homework_extensions                               -- teacher override (D-HW-5)
  id            uuid pk
  homework_id   uuid fk -> homework
  student_id    uuid NULL fk -> students           -- NULL = extension for whole class
  new_due_date  date NOT NULL
  new_due_time  time NULL
  granted_by    uuid
  reason        text NULL
```

## 6. Assign Homework (fixes the dead button F-HW-1 / B-HW-1)

- **Entry:** Planner → Homework → **Assign** (Doc 04 §5.13), or Today's period card → Homework (pre-scoped), or Classes → class detail.
- **Sheet fields:** title (required), description, **due-date picker** (typed, ≥ today), optional due-time, optional **attachment** (file/image upload → `homework_attachments`), allow-late toggle (default off → enforces no-submit-past-due).
- **Scope** pre-filled from the period/class; no shared picker.
- **Submit** = result-driven (`isSubmitting`/`submitSuccess`/`submitError`); on success → homework appears in the active list; parents/students notified.
- Endpoint: `POST /api/v1/teacher/homework` (+ multipart for attachment, or pre-signed upload then attach).

## 7. The No-Submit-Past-Due Rule + Teacher Extension (D-HW-4/5)

- **Rule:** once `due_date`(+`due_time`) passes, students **cannot submit** unless `allow_late=true` (then it lands `status='late'`) — enforced server-side on the student submission path.
- **Teacher extension:** the teacher can grant an extension:
  - **Whole class:** `homework_extensions(student_id=NULL, new_due_date)` → moves the cutoff for everyone.
  - **Single student:** `homework_extensions(student_id=…)` → reopens submission for that one student (the common "she was sick" case Aanya will hit).
- Extension is logged (who/when/why) and reflected on the board.
- Endpoint: `POST /api/v1/teacher/homework/{id}/extend { studentId?, newDueDate, newDueTime?, reason? }`.

## 8. Submissions Status Board (fixes B-HW-3)

- **Surface:** Planner → Homework → tap a homework card → **board**.
- **Columns/filters:** **Submitted · Not submitted · Late · Reviewed/Graded**, with counts ("28 submitted · 9 not · 3 late").
- **Per student row:** name, roll, status chip, submitted-at, attachment (if student uploaded), [Mark reviewed] / [Grade] / [Grant extension] actions.
- **Board is the teacher's chase tool** — Mr. Rao opens it, sees the 9 "not submitted" in red, taps "grant extension" for the 2 with reasons.
- Endpoint: `GET /api/v1/teacher/homework/{id}/submissions` (server-joined to roster so even *not-submitted* students appear — not just rows that exist in `homework_submissions`). `PATCH …/submissions/{studentId}` to set reviewed/graded.

## 9. Homework Edge Cases
| # | Case | Handling |
|---|------|----------|
| H1 | due date in past at create | blocked (≥ today). |
| H2 | student submits after due, allow_late off | server rejects submission. |
| H3 | allow_late on | late submission accepted, flagged `late`. |
| H4 | single-student extension | reopens for that student only. |
| H5 | class extension after some submitted | already-submitted unaffected; others get new cutoff. |
| H6 | attachment upload fails | assign still succeeds; attachment retried; card shows "attachment pending." |
| H7 | not-submitted students | appear on board via roster join (B-HW-3 fix), not missing. |
| H8 | student transferred out | excluded from board from transfer date. |
| H9 | homework closed/deactivated | board read-only; no new submissions. |
| H10 | no students enrolled | "No students in this class." |

## 10. Persona Validation

- **Aanya (during class):** Today period card → Homework → "Ex 4.2", due Friday, attaches a worksheet photo, Assign. Next day a parent says her child was absent → Aanya opens the board → grants that one student a 2-day extension. One tap each, no picker.
- **Mr. Rao (after class):** Planner → Homework → opens the board for "Essay" → sees 9 red "not submitted" at a large font → marks the submitted ones reviewed in a column → done. Syllabus: between activities he taps "Chapter 3: Photosynthesis" covered — one tap, progress bar moves to 55%.

## 11. Cross-refs & Dependencies
- Scope via `assignment_id` + `requireOwnedAssignment` (Doc 05 / TeacherAccess).
- Typed dates resolve X-3 across syllabus + homework.
- Curriculum + homework seeding is an X-5 task in **Doc 11** (else Planner lands empty).
- Submissions board roster-join mirrors the "show everyone, not just rows that exist" principle used in marks (Doc 07) and attendance (Doc 06).
- UI: one-tap toggle, board status chips, upload states, due-date picker — all in **Doc 10**.
- Active homework + submission counts surface on Classes → class detail (Doc 09) and Today obligations (Doc 04 §5.5).

---

*End of 08_SYLLABUS_AND_HOMEWORK_SPEC.md*
