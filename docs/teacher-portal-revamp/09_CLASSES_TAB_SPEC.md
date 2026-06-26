# 09 — CLASSES TAB SPEC

> **Scope:** The complete operational class view: the class list, the class **detail** (enrolled students with attendance rate, latest performance, flags; weekly timetable; next period; attendance summary; assessment schedule; active homework), and the **student profile** drill-down.
> **Source:** `feature/teacher/TeacherRouting.kt` (`/classes` with 3× N+1 per class, `is_class_teacher` hardcoded false; helpers `studentCountFor`, `studentsForAssignment`, `avgAttendanceFor`), `TeacherClassesScreenV2.kt` (grid + ClassDetail showing `VComingSoon` roster). Defects: B-CLS-1..5, F-CLS-1..6, F-PROF-3, D-STU-1..3, X-1.
> **Lens:** Aanya (wants to *know* her one class deeply) and Mr. Rao (6 classes, wants to spot the at-risk students fast).
> **Law:** The Classes tab is where the teacher *understands* their students. It must show real rosters and real signals — not `VComingSoon`.
> **Branch:** `backend-by-abuzar_v1.0.3`

---

## 0. Executive Summary

The Classes tab promises the richest view in the portal and delivers a placeholder:

> **F-CLS-5: the class detail's roster is a `VComingSoon` component.** The one thing "My Classes" is for — seeing your students — is unbuilt.

Around it:
- **B-CLS-1 / N+1:** `/classes` runs ~3 separate per-class queries (student count, syllabus progress, avg attendance) in a loop — slow and fragile at 6 classes.
- **B-CLS-3 / F-CLS-3:** `is_class_teacher` is **hardcoded `false`** — the portal can't distinguish a class teacher (owns the whole class) from a subject teacher.
- **`avgAttendanceFor`** parses the packed `grade` string by `lastIndexOf('-')` (D-ATT-4) and uses `ClassNaming.sameClassSection` in-memory matching (X-1) → unreliable aggregates (B-CLS-2).
- **No student profile drill-down at all** (B-PROF-1/2, F-PROF-3).
- Student identity is free-text `studentCode` with no FK and denormalized `className`/`section` as truth (D-STU-1..3, X-2, X-4).

This spec defines the corrected Classes tab end-to-end: a fast list (single aggregated query), a complete class detail with real roster + signals, and a student profile drill-down — all built on the typed `enrollments` model.

---

## 1. Data Foundation (what Classes needs that doesn't exist yet)

Classes depends on the **`enrollments`** table from Doc 01 target schema (resolves X-1/X-2/D-STU):
```
enrollments
  id            uuid pk
  school_id     uuid
  student_id    uuid fk -> students
  class_id      uuid fk -> school_classes
  section       text
  roll_number   int
  status        text CHECK (status IN ('active','transferred','withdrawn'))
  start_date    date
  end_date      date NULL                    -- transfers/withdrawals (Doc 06 E4/E5)
  UNIQUE (student_id, class_id, section, start_date)
```
This is the join that makes "enrolled students of 7B" a **typed query**, replacing the in-memory `ClassNaming.sameClassSection` heuristic and the packed-grade parsing.

It also needs the **`is_class_teacher`** signal to be real: derive from `TeacherSubjectAssignmentsTable` having a class-teacher role marker (add `is_class_teacher boolean` to TSA, or a `class_teacher_id` on `school_classes`). Fixes B-CLS-3/F-CLS-3.

---

## 2. Class List

- **Purpose:** entry to operational views; only the teacher's own assignments (via `teacherAssignmentsFor`).
- **Primary action:** tap class card → class detail.
- **Secondary:** filter chips — **Class teacher** vs **Subject teacher** (now possible with real flag); search by class.
- **Data per card (single aggregated endpoint, fixes B-CLS-1 N+1):**
  - class + section + subject
  - **student count** (from enrollments)
  - **class-teacher badge** (real, B-CLS-3 fix)
  - **next period** for this class (from Doc 05 resolved week)
  - **today's attendance state** (✓ marked / ! unmarked) — quick signal
  - **at-risk count** (n students flagged — §5)
- **Empty state:** "No classes assigned to you yet — contact your admin." (real on fresh seed, X-5)
- **Loaded state:** card grid grouped by class then subject; class-teacher cards visually distinguished (accent border).
- **Refresh:** foreground + pull-to-refresh.
- **Interactions:** tap → detail; long-press → quick actions (Mark attendance / Assign homework / Create assessment), all pre-scoped.

**Endpoint (replaces looping `/classes`):**
```
GET /api/v1/teacher/classes
→ [ { assignmentId, classId, className, section, subjectId, subject,
      studentCount, isClassTeacher, nextPeriod{…}?, todayAttendanceMarked,
      atRiskCount } ]   -- all aggregates computed in ONE query set (no per-class N+1)
```

---

## 3. Class Detail (the heart of the tab)

A single sectioned screen. Server provides it via one composite endpoint to avoid client N+1:
```
GET /api/v1/teacher/classes/{assignmentId}
→ { header{className, section, subject, isClassTeacher, studentCount},
    nextPeriod{…}?, weeklyTimetable[…],
    attendanceSummary{ todayMarked, weekRate, monthRate, byStatusToday{…} },
    assessmentSchedule[ {assessmentId, name, type, examDate, status} ],
    activeHomework[ {homeworkId, title, dueDate, submittedCount, notSubmittedCount} ],
    roster[ {studentId, name, roll, photoUrl, attendanceRate, latestMark{name, marks, max}?, flags[…]} ] }
```

### 3.1 Header
- Class · section · subject; **class-teacher badge** if applicable.
- Primary CTA: **Mark attendance** (today, this class — Doc 06).
- Quick actions row: Create assessment (Doc 07) · Assign homework (Doc 08) · Log syllabus (Doc 08).

### 3.2 Next period
- "Next: Tue 10:15, Room 12" from Doc 05 resolved week; tap → period sheet.
- Empty: "No upcoming period this week."

### 3.3 Weekly timetable (for this class)
- The class's periods across the week (when *I* teach it); today's column emphasized.
- Empty: "Timetable not set up."

### 3.4 Attendance summary
- Today: marked/unmarked + P/A/L/Lv counts.
- **Week rate** and **month rate** (real aggregates, server-computed — fixes B-CLS-2 unreliable `avgAttendanceFor`).
- Tap → attendance history for the class.
- Empty: "No attendance recorded yet."

### 3.5 Assessment schedule
- Upcoming + recent assessments (from Doc 07), with status chips (Scheduled / Marks pending / Published).
- Tap → marks entry / detail.
- Empty: "No assessments yet — create one."

### 3.6 Active homework
- Active homework with **submitted / not-submitted counts** (from Doc 08 board).
- Tap → submissions board.
- Empty: "No active homework."

### 3.7 Roster (replaces `VComingSoon`, F-CLS-5)
The core fix. A list of **enrolled students**, each row:
- photo · name · roll
- **attendance rate** (e.g. "92%") with color (success/warning/danger thresholds — Doc 10)
- **latest performance** (most recent published mark: "Unit Test 22/25")
- **flags** (see §5): low attendance, failing trend, recent absences
- tap → **student profile** (§4)

Ordering: by roll by default; sort/filter by attendance, performance, flagged-only.
Empty: "No students enrolled in this class yet." (real on fresh seed — never `VComingSoon`).

---

## 4. Student Profile (drill-down — fixes B-PROF-1/2, F-PROF-3)

Reached from any roster row. Read-only for the teacher (no destructive actions in v1).
```
GET /api/v1/teacher/students/{studentId}   (scoped: teacher must teach this student)
→ { name, roll, photoUrl, className, section,
    attendance{ rate, recent[ {date, status} ], trend },
    performance[ {assessmentName, subject, marks, max, date} ],
    flags[…], parentContact{ name, phone }?  }   -- contact gated by role/class-teacher
```

### Sections
1. **Header:** photo, name, roll, class+section.
2. **Attendance:** rate + recent days (P/A/L/Lv chips) + trend (improving/declining). Drill from Doc 06 records.
3. **Performance:** marks across the teacher's subject(s) for this student (Doc 07 trajectory); per-row tap → assessment.
4. **Flags:** the computed risk signals (§5) with plain-language reasons.
5. **Parent contact:** visible to the **class teacher** (or per school policy); subject teachers may see limited contact — scoped, not blanket-exposed (privacy).

- **Empty/edge:** student with no marks yet → "No marks recorded yet"; new admission → attendance from enrollment start only.
- **Authorization:** server verifies the teacher actually teaches the student (via owned assignments ⇄ enrollments); otherwise 403 (no cross-class snooping).

---

## 5. Flags / At-Risk Signals (computed)

Server-computed, plain-language, so both personas act without interpretation:
| Flag | Rule (configurable thresholds) | Color |
|------|-------------------------------|-------|
| Low attendance | rate < 75% (month) | danger |
| Recent absences | ≥3 absences in last 5 sessions | warning |
| Failing trend | last 2 published marks below pass | danger |
| Dropping | latest mark down >20% vs prior | warning |
| No data | new student, insufficient data | neutral |

Flags drive: roster row badges, class-list `atRiskCount`, and (optionally) Today obligations ("3 students need attention in 7B").

---

## 6. Performance / Correctness Fixes Embedded Here

- **Kill N+1 (B-CLS-1):** class list and class detail each resolve via **one composite query set**, not per-class loops.
- **Real `is_class_teacher` (B-CLS-3):** sourced from data, not hardcoded false.
- **Reliable aggregates (B-CLS-2):** attendance rate / averages computed by typed joins on `enrollments` + typed `date`, not by parsing `grade` strings or in-memory `ClassNaming`.
- **No more `VComingSoon` (F-CLS-5):** roster is real.
- **Scoped student access (B-PROF):** authorization via owned-assignment ⇄ enrollment.

---

## 7. Persona Validation

- **Aanya (one class, 7B Maths):** opens 7B → sees her 38 students, spots 2 red attendance flags, taps one → student profile → "62% attendance, declining" → she now knows to talk to the parent. The roster *exists* and *means something*.
- **Mr. Rao (6 classes):** class list shows `atRiskCount` badges per class; he taps the class with "5 at risk," sorts roster by attendance, sees the bottom 5, drills into each. Big text, real numbers, no placeholder.

---

## 8. Cross-refs & Dependencies

- **`enrollments`** model (Doc 01 §11) is the prerequisite — without it, roster and aggregates stay heuristic.
- Roster attendance rate from **Doc 06**; latest performance + trajectory from **Doc 07**; active homework counts from **Doc 08**; next period / weekly timetable from **Doc 05**.
- Composite endpoints sequenced in **Doc 11** after `enrollments` migration.
- Card/roster/flag visuals, thresholds-to-color, photo placeholders, long lists in **Doc 10**.

---

*End of 09_CLASSES_TAB_SPEC.md*
