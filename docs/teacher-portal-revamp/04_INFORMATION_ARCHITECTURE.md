# 04 — INFORMATION ARCHITECTURE (from first principles)

> **Scope:** The complete new information architecture of the teacher portal, derived from the teacher's actual job and their cognitive contexts across a school day — not from the existing 4-tab shell. Defines the tab/subtab structure, and for every surface: purpose, primary action, secondary actions, what data surfaces, empty state, loaded state, refresh strategy, and available interactions.
> **Source:** Current shell = `TeacherPortalV2.kt` (4 tabs: Home · Update · My Classes · Profile). Design reference = `ParentPortalV2.kt` (5 tabs + ParentDock). Defects from Docs 01–03.
> **Lens:** **Aanya (19, first-year, anxious, tech-native)** and **Mr. Rao (58, principal-teacher, 6 classes × 40 students, low tech confidence).**
> **Law:** The IA must match the teacher's mental model of their day, not the database's table list.
> **Branch:** `backend-by-abuzar_v1.0.3`

---

## 0. Executive Summary

The current IA is **tool-centric**: it groups the four heaviest write-tools (Attendance, Marks, Syllabus, Homework) behind a single "Update" tab and a shared, stateless class/subject/exam picker (F-SHELL-2, F-SHELL-3). This forces the teacher to *re-declare context* — which class, which subject, which exam — every single time, with no memory of "the class I am about to teach." The database's shape (one table per concern) has leaked directly into the navigation.

The correct IA is **context-centric**: it follows the teacher through the four cognitive states of a school day and surfaces the right action *pre-scoped to the current or selected class*. The organizing insight:

> **A teacher does not think "I want to use the Attendance tool." A teacher thinks "I am about to teach 7B Maths — what do I need to do for this period?"**

The unit of work is the **period** (a class × section × subject at a time), not the tool. Everything the teacher does — attendance, syllabus, homework, marks — hangs off a period or a class. The IA must make **"the class I am in / about to be in"** the primary axis, and the tool the secondary axis.

This produces a **5-tab structure** that mirrors the parents portal's rhythm (familiar, premium, ParentDock-class) while encoding the teacher's day:

| Tab | Cognitive context it serves | Core question it answers |
|-----|-----------------------------|--------------------------|
| **Today** | Morning + throughout-day glance | "What is happening now and next, and what do I owe?" |
| **Classes** | During-class + drill-down | "Everything about the class I'm teaching / a specific student." |
| **Gradebook** | After-class | "Assessments, marks, history, comparisons." |
| **Planner** | During + after class | "Syllabus coverage and homework lifecycle." |
| **Profile** | Identity / settings / leave | "Me, my schedule, my leave, my settings." |

Attendance is **not a tab** — it is an *action* reachable from Today (the active/next period card) and from Classes (the class detail). It is too frequent and too contextual to live behind a picker.

---

## 1. The Teacher's Job (first principles)

Strip away the app. A schoolteacher's recurring obligations are:

1. **Be where they are supposed to be.** Know the day's schedule; arrive at the right room; (institutionally) record their own presence.
2. **Account for who is present.** Mark student attendance for each class they teach, every day, ideally in the first minutes of the period.
3. **Move the curriculum forward.** Cover syllabus units and record what was covered.
4. **Set and chase work.** Assign homework, set due dates, see who submitted.
5. **Measure learning.** Create assessments, enter marks, publish results, look at trends.
6. **Know their students.** Recognize who is falling behind (attendance, marks, flags).
7. **Stay in the loop.** Receive school notices, leave decisions, messages.
8. **Manage their own status.** Apply for leave; update profile/password.

Note the **temporal clustering**: 2–4 happen *during* a period; 5 happens *after* a period; 1 and 7 happen *at the edges of the day*; 6 and 8 are *on demand*. The IA must respect this clustering. The current "Update" tab violates it by flattening during-class (attendance/syllabus/homework) and after-class (marks) into one undifferentiated picker-driven list.

---

## 2. The Four Cognitive Contexts (the day, mapped)

### 2.1 MORNING (pre-first-period) — *"What's my day?"*
Mental state: orienting, sometimes rushed. Wants a single screen that answers "where do I need to be and when, and is anything unusual today (holiday, exam, PTM, my leave)."
- See **today's timetable** (periods in order, with the *next/current* one emphasized).
- Record **own check-in** (faculty attendance) — currently impossible (B-ATT-5, no endpoint, no biometric anywhere — verified absent).
- See **today's calendar overlay** (exam scheduled, holiday, PTM) — currently absent (B-FEED-1).

### 2.2 DURING CLASS — *"I'm in 7B Maths now."*
Mental state: time-boxed, hands busy, often standing. Wants the **fewest taps** to:
- **Mark student attendance** for *this* class (the dominant daily action).
- **Log syllabus** — tap the unit(s) covered today.
- **Assign homework** for this class (title, due date, optional attachment).
All three should be reachable **without re-picking the class** — the class is already known from the active period.

### 2.3 AFTER CLASS — *"Now I'll grade / review."*
Mental state: seated, deliberate, longer session. Wants to:
- **Enter test marks** (with validation: max, pass).
- **Review homework submissions** (who submitted / not / late; grant extensions).
- **Look at history/trends** (this class over time; compare assessments).

### 2.4 THROUGHOUT THE DAY — *"Anything I need to know?"*
Mental state: ambient, interrupt-driven. Wants:
- **Notifications** (leave approved/rejected, school notice, calendar publish).
- **Messages** (if/when teacher messaging is enabled).
- **Outstanding obligations** ("3 classes still unmarked today; 1 assessment unpublished").

---

## 3. Mapping Contexts → Navigation

| Cognitive context | Lives primarily in | Secondary entry |
|-------------------|--------------------|-----------------|
| Morning glance | **Today** tab | Profile → My Schedule (full week) |
| Own check-in | **Today** tab (check-in card) | — |
| Mark student attendance | **Today** (active period CTA) | **Classes** → class detail → Mark Attendance |
| Log syllabus | **Today** (active period CTA) | **Planner** → Syllabus |
| Assign homework | **Today** (active period CTA) | **Planner** → Homework |
| Enter marks | **Gradebook** | **Classes** → class detail → enter marks |
| Review submissions | **Planner** → Homework board | **Today** ("submissions to review") |
| History/trends | **Gradebook** → assessment history | **Classes** → class detail → performance |
| Notifications | **Today** (notification bell + strip) | global |
| Leave / profile / password | **Profile** | — |

**Key consequence:** every "during-class" action is reachable in **one tap from Today's active-period card**, *and* in a deliberate path from Classes. There is no longer a stateless shared picker (kills F-SHELL-2/3).

---

## 4. The Tab Structure (final)

```
┌─ Today ──────────── (default landing)
│   ├─ Greeting + date + own check-in
│   ├─ Now / Next period card  → [Mark attendance] [Syllabus] [Homework]
│   ├─ Today's schedule (period list)
│   ├─ Today's calendar overlay (exam/holiday/PTM)
│   └─ Obligations strip ("2 classes unmarked", "1 result unpublished")
│
├─ Classes ────────── (operational class views)
│   ├─ Class list (only my assignments, grouped)
│   ├─ Class detail → roster, timetable, next period,
│   │                 attendance summary, assessment schedule, active homework
│   └─ Student profile (drill-down)
│
├─ Gradebook ─────── (assessment + marks)
│   ├─ Assessment list (scheduled / past / draft) per class
│   ├─ Create assessment (scoped class/section/subject)
│   ├─ Marks entry (validated)
│   └─ History & comparison
│
├─ Planner ───────── (syllabus + homework)
│   ├─ Syllabus (coverage per class/subject)
│   └─ Homework (assign + submissions board + extensions)
│
└─ Profile ───────── (identity + schedule + leave + settings)
    ├─ Identity + role + school
    ├─ My weekly schedule (full timetable)
    ├─ My leave (apply + status)
    └─ Settings (password, notifications, theme, logout)
```

**Why 5 and not 4:** the existing 4-tab shell collapses Gradebook + Planner + Attendance into "Update," which is the root of the picker problem. Splitting *after-class* (Gradebook) from *during-class planning* (Planner) matches §2.2/§2.3 and matches the parents portal's 5-tab cadence the teacher's colleagues already see — consistency across portals (Doc 10).

**Why Attendance is not a tab:** it is the single most frequent action and is *always* scoped to a specific class+date. Burying it behind a tab+picker (current state) adds 2–3 taps to the #1 task. Promoting it to a context-driven CTA on the active-period card (Today) and the class detail (Classes) makes it 1 tap from the natural place a teacher looks.

---

## 5. Per-Surface Specification

For each surface: **Purpose · Primary action · Secondary actions · Data surfaced · Empty state · Loaded state · Refresh strategy · Interactions.** Cross-refs to defects in Docs 01–03 and to the deep specs (05–10).

### 5.1 TODAY — Greeting + Check-in band
- **Purpose:** orient the teacher in <2s; record own presence.
- **Primary action:** **Check in** (faculty attendance, biometric/Compose with fallback — see Doc 06 §biometric). Currently impossible (B-ATT-5; no biometric in codebase — verified).
- **Secondary:** open notifications; open full schedule.
- **Data surfaced:** teacher name + first-name greeting, today's date (real DATE, not string — D-ATT-3/X-3), check-in status (Not checked in / Checked in HH:mm), school name.
- **Empty state:** never truly empty; if check-in endpoint unavailable, show check-in card disabled with "Check-in unavailable" caption (graceful, Doc 10 error states).
- **Loaded state:** "Good morning, Aanya" + date + green "Checked in 08:42" pill OR amber "Tap to check in."
- **Refresh:** on app foreground + pull-to-refresh; check-in state is local-optimistic then server-confirmed.
- **Interactions:** tap check-in → biometric prompt → optimistic pill flip; tap bell → notifications sheet.

### 5.2 TODAY — Now / Next period card
- **Purpose:** answer "what am I teaching right now / next" and launch all during-class actions pre-scoped.
- **Primary action:** **Mark attendance for this period** (1 tap, class already known).
- **Secondary:** Log syllabus (for this class+subject); Assign homework (this class+subject); View class detail.
- **Data surfaced:** class+section+subject, room, start–end time, "in 12 min" / "now" / "ended" status, attendance-marked badge (✓ marked / ! unmarked) — requires real per-period attendance state, currently fabricated (B-HOME-4/F-HOME-3).
- **Empty state:** "No more classes today" card (after last period) / "No classes scheduled today" (holiday or no timetable seeded — D-TT/B-FEED-2). If timetable unseeded, show "Your timetable isn't set up yet — contact your admin" (honest, not a fake card).
- **Loaded state:** highlighted card for current period; muted card for next; period list below.
- **Refresh:** time-driven (recompute current/next every minute) + foreground; attendance-marked badge updates after a mark write.
- **Interactions:** tap CTA → respective scoped tool; long-press → class detail; swipe between current/next (optional, mirrors ParentScheduleCard faces — Doc 05).

### 5.3 TODAY — Today's schedule list
- **Purpose:** full ordered view of today's periods.
- **Primary action:** tap a period → its actions (scoped).
- **Data surfaced:** each period: time, class+section, subject, room, marked/unmarked badge.
- **Empty:** "No classes today" (holiday) vs "Timetable not set up" (unseeded) — distinguish (Doc 05).
- **Loaded:** vertical timeline, current period emphasized.
- **Refresh:** foreground + minute tick.
- **Interactions:** tap row → period sheet; mark badge inline.

### 5.4 TODAY — Calendar overlay
- **Purpose:** surface today's school-calendar items (exam, holiday, PTM) from VP-CAL (`CalendarEventsTable`, status PUBLISHED).
- **Primary action:** tap event → detail.
- **Data surfaced:** today's PUBLISHED events for the teacher's audience; "Exam: 7B Maths Unit Test today."
- **Empty:** hidden if no events today (do not show an empty card).
- **Loaded:** compact chips/strip above or within schedule.
- **Refresh:** foreground; cheap.
- **Interactions:** tap → event detail; if EXAM tied to teacher's class → deep-link to Gradebook assessment (Doc 05/07).

### 5.5 TODAY — Obligations strip
- **Purpose:** replace fabricated "Today's tasks" (B-HOME-4) with *real* outstanding work.
- **Primary action:** tap an obligation → jump to resolve it.
- **Data surfaced (computed, real):** "{n} of {m} classes unmarked today"; "{n} unpublished results"; "{n} homework sets with submissions to review"; "{n} pending leave decisions" (if approver).
- **Empty:** "You're all caught up ✓" (genuinely earned, not faked).
- **Loaded:** list of real obligation chips with counts.
- **Refresh:** foreground + after any write that resolves one (optimistic decrement).
- **Interactions:** tap → deep-link to the exact scoped surface.

### 5.6 CLASSES — Class list
- **Purpose:** entry to operational class views; only the teacher's own assignments.
- **Primary action:** tap class → class detail.
- **Secondary:** filter by class-teacher vs subject-teacher.
- **Data surfaced:** per assignment: class+section, subject, student count, is-class-teacher flag (currently hardcoded false — F-CLS-3/B-CLS-3), next period for it.
- **Empty:** "No classes assigned to you yet — contact your admin" (real possibility on fresh seed — X-5).
- **Loaded:** card grid, grouped by class then subject.
- **Refresh:** foreground + pull-to-refresh.
- **Interactions:** tap → detail; long-press → quick actions (mark attendance / assign homework).

### 5.7 CLASSES — Class detail
- **Purpose:** the complete operational view of one class (full spec in Doc 09).
- **Primary action:** **Mark attendance** (this class, today).
- **Secondary:** create assessment, assign homework, log syllabus, open roster, view timetable.
- **Data surfaced:** enrolled students (with attendance rate, latest performance, flags), weekly timetable for the class, next period, attendance summary, assessment schedule, active homework.
- **Empty:** roster currently shows `VComingSoon` (F-CLS-5) — must show real roster or "No students enrolled."
- **Loaded:** sectioned scroll (header → roster → summary cards).
- **Refresh:** foreground + after writes.
- **Interactions:** tap student → profile; tap summary card → its tool.

### 5.8 CLASSES — Student profile
- **Purpose:** drill-down on one student.
- **Primary action:** none destructive; view.
- **Secondary:** (future) message parent, flag.
- **Data surfaced:** name, roll, photo, attendance rate, recent marks, flags, parent contact (scoped).
- **Empty:** n/a (reached only with a real student).
- **Loaded:** profile header + attendance + performance + flags.
- **Refresh:** foreground.
- **Interactions:** scroll; tap an assessment → its detail.

### 5.9 GRADEBOOK — Assessment list
- **Purpose:** all assessments for the teacher's classes, grouped scheduled / past / draft.
- **Primary action:** **Create assessment** (scoped class/section/subject — Doc 07).
- **Secondary:** filter by class; open an assessment to enter/edit marks.
- **Data surfaced:** name, class+section+subject, examDate (real DATE), maxMarks, published state, entered/total count.
- **Empty:** "No assessments yet — create your first" (real on fresh seed).
- **Loaded:** grouped list with status chips (Draft / Scheduled / Marks pending / Published).
- **Refresh:** foreground + after create/publish.
- **Interactions:** tap → marks entry; long-press → publish/unpublish/delete.

### 5.10 GRADEBOOK — Marks entry
- **Purpose:** enter/edit marks with validation; **publish is a separate, explicit step** (fixes B-MK-1 where save force-publishes).
- **Primary action:** **Save (draft)**; then explicit **Publish**.
- **Secondary:** bulk-fill absent; sort by roll/name.
- **Data surfaced:** roster with mark fields, max, pass line, running entered count.
- **Empty:** "No students in this class."
- **Loaded:** dense numeric grid (DM Mono tabular figures — Doc 10), validation inline (>max blocked).
- **Refresh:** autosave-draft optimistic; foreground reload guarded against unsaved edits.
- **Interactions:** numeric keypad; tab-to-next; validation toast; publish confirm dialog.

### 5.11 GRADEBOOK — History & comparison
- **Purpose:** trends for a class/subject across assessments (Doc 07).
- **Data surfaced:** class average per assessment over time; per-student trajectory; comparison between two assessments.
- **Empty:** "Not enough data yet."
- **Loaded:** sparkline/bars + table.
- **Refresh:** foreground.
- **Interactions:** select assessments to compare; tap student → profile.

### 5.12 PLANNER — Syllabus
- **Purpose:** track curriculum coverage per class+subject (Doc 08).
- **Primary action:** **toggle unit covered** (one tap, lighter than a form — F-SYL-1 critique).
- **Secondary:** add unit, reorder, add note/date.
- **Data surfaced:** ordered units, covered/uncovered, coveredOn date, % progress.
- **Empty:** "No syllabus set up — add your first unit" (B-SYL-1: no create endpoint today).
- **Loaded:** checklist with progress bar.
- **Refresh:** optimistic toggle; foreground reload.
- **Interactions:** tap to toggle; long-press to edit; drag to reorder.

### 5.13 PLANNER — Homework
- **Purpose:** assign homework and run its lifecycle (Doc 08).
- **Primary action:** **Assign homework** (title, due-date picker, optional attachment) — fixes dead button (F-HW-1).
- **Secondary:** open submissions board; grant extension; close.
- **Data surfaced:** active/past homework; per-item submission board (submitted / not / late / reviewed).
- **Empty:** "No homework assigned — assign your first."
- **Loaded:** list of homework cards each with submission counts; tap → board.
- **Refresh:** foreground + after assign; submissions poll on board open.
- **Interactions:** assign sheet; submissions board with status filters; extend due date (teacher override of no-submit-past-due rule, Doc 08).

### 5.14 PROFILE — Identity / Schedule / Leave / Settings
- **Purpose:** the teacher's own data and controls; replaces inert setting rows (F-PROF-1/2).
- **Primary action:** context-dependent (apply leave; change password; toggle theme).
- **Secondary:** view full weekly schedule; logout.
- **Data surfaced:** name, role, school, photo; full week timetable; leave list+status (LeaveRequestsTable); settings.
- **Empty:** leave list empty → "No leave requests."
- **Loaded:** sectioned profile.
- **Refresh:** foreground; leave status updates on notification.
- **Interactions:** apply-leave sheet (date-from/to, reason, optional image); password change; theme switch (Warm/Light/Night — Doc 10); logout.

---

## 6. Navigation Mechanics

- **Bottom dock** = teacher equivalent of **ParentDock** (`ParentDock.kt`): floating glass dock, sliding violet lozenge, haptics, badges. 5 destinations (Today · Classes · Gradebook · Planner · Profile). Badge on Today reflects real obligation count (not hardcoded `1`, fixing F-SHELL-4).
- **Header** = teacher equivalent of `ParentHeader`, *without* a child switcher; instead an optional **class context chip** when inside Classes/Gradebook/Planner showing the currently scoped class, tappable to change.
- **Deep links:** Today's CTAs and obligations deep-link directly into scoped tool states (e.g., "Mark attendance · 7B Maths · today") — no re-picking.
- **Back behavior:** drill-downs (class detail → student profile; assessment → marks) use standard back; tab switches preserve scroll/state.

---

## 7. State Memory (the anti-picker rule)

The single biggest IA fix: **context is remembered and inferred, never re-declared.**

1. **Active period inference:** Today derives the current class from the timetable + clock. Marking attendance from there carries class+section+subject+date automatically.
2. **Last-used scope:** when the teacher enters Gradebook/Planner without a deep link, default to the **most recently used class** (persisted), not "nothing selected."
3. **Class context chip:** changing scope is one tap on the header chip, not a full re-pick across three scrollers.
4. **No shared cross-tool picker:** the current `TeacherUpdateSelector` (shared class/exam picker, F-SHELL-2/3) is **eliminated**; scoping is local to each tool and pre-filled.

This directly serves both personas: **Aanya** stops fearing "did I pick the right subject?"; **Mr. Rao** stops re-scrolling chip rows for 6 classes every time.

---

## 8. Persona Walkthroughs (validation)

### 8.1 Aanya, 08:40, first period in 5 minutes
Opens app → **Today** → sees greeting, taps **Check in** (biometric, confirmed). Now/Next card shows **7B Maths · now · Room 12 · ! unmarked**. Taps **Mark attendance** → roster pre-loaded for 7B → marks in <60s → badge flips ✓. During class, taps **Syllabus** on the same card → toggles "Fractions: addition" covered. End of period, taps **Homework** → assigns "Ex 4.2, due Fri" with one date-pick. **Zero pickers operated.**

### 8.2 Mr. Rao, after lunch, 6 classes to reconcile
Opens **Today** → Obligations strip: "**3 of 6 classes unmarked today.**" Taps it → list of the 3 unmarked classes → taps first → roster → bulk "all present" then flips the 2 absentees → next. Later opens **Gradebook** → it defaults to his last class → "Unit Test · marks pending" → enters marks (validation stops a 105/100 typo) → **Save**, then deliberate **Publish**. He never hunted through a shared picker; the system told him exactly what he owed.

---

## 9. What This IA Kills / Adds vs Current

**Kills:** the "Update" tab; the shared stateless class/subject/exam picker (F-SHELL-2/3); fabricated "Today's tasks" (B-HOME-4); the hardcoded badge=1 (F-SHELL-4); attendance buried 2–3 taps deep (F-ATT-1).

**Adds:** Today tab as a true daily cockpit; own check-in (Doc 06); real obligations strip; context inference/memory (§7); a 5th tab to separate after-class (Gradebook) from during-class planning (Planner); calendar overlay (Doc 05); class-context chip.

**Carries forward (good):** the design-system fidelity, VStateHost loading/empty/error pattern, Koin VM pattern, ParentDock-class navigation feel.

---

*End of 04_INFORMATION_ARCHITECTURE.md*
