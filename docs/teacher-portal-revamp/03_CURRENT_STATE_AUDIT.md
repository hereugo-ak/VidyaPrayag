# 03 — CURRENT STATE AUDIT (Frontend, screen by screen)

> **Scope:** Every teacher composable in `composeApp/.../ui/v2/screens/teacher` and its `shared/.../feature/teacher` ViewModel/data layer. What each screen does, what's broken, what's missing, the UX failures, and the root-cause layer (Schema / Backend / Frontend) for each.
> **Lens:** Two personas, applied to every screen — **Aanya, 19, first-year teacher, tech-native, anxious** and **Mr. Rao, 58, principal-teacher, low tech confidence, 6 classes × 40 students.**
> **Law:** Do not be gentle. Root cause first.
> **Branch:** `backend-by-abuzar_v1.0.3`

---

## 0. Executive Summary

The teacher portal is a **4-tab shell (Home · Update · My Classes · Profile)** that is *visually* consistent with the design system but **operationally hollow**. Its defining failure is the **"Update" tab**: it crams the four heaviest tools in the product — Attendance, Marks, Syllabus, Homework — behind a *shared, manual class/subject/exam picker* that the teacher must re-operate every single time, with no memory, no context, and (because of the empty-schema problem, Doc 01 X-5/D-SYL-2/B-FEED-2) frequently nothing to pick.

The hard truths:

1. **The most-used daily action (attendance) is buried two-to-three taps deep** behind a horizontal class chip-scroller, and resets to "no class selected" on every visit.
2. **Three of the four Update sub-tools land empty on a real, fresh install** — Syllabus (no units exist, B-SYL-1), Marks (no assessments exist until you create one inline), and Home periods (no timetable seeded, B-FEED-2).
3. **"Today's tasks" on Home is fabricated noise** — one identical "Mark attendance" card per *subject-assignment*, never reflecting real state (B-HOME-4).
4. **The Classes tab opens to a `VComingSoon` placeholder** for the one thing it promises — the student roster.
5. **Homework's primary button is dead** (`onAssign = { /* Phase 3E */ }`).
6. **There is no check-in, no schedule-first morning view, no student profile, no homework review** — entire workflow stages have no screen.

For **Aanya**, the app gives her anxiety: she can't tell whether an empty screen means "you're done" or "it's broken," and the Update picker makes her re-declare context constantly. For **Mr. Rao**, marking 6 classes means 6× the picker dance, tiny P/A/L pills on a dense list, and a marks grid of number fields with no keyboard affordance for 40 students.

---

## 1. The shell — `TeacherPortalV2.kt`

### 1.1 What it does
- 4 bottom-nav tabs: **Home, Update (badge=1 hardcoded), My Classes, Profile.**
- The **Update** tab renders an `<h1>Update</h1>` + a `VTopTabs` row (Attendance / Marks / Syllabus / Homework).
- Holds three pieces of cross-tab state: `selectedClassId`, `selectedSubject`, `selectedExamId` — all reset on class change.
- For Attendance/Marks/Syllabus it renders `TeacherClassPicker` *above* the tool. Marks additionally renders `TeacherExamPicker`. Homework "authors its own class field" — except it doesn't (see §6).
- Pushes Notifications / Calendar / Leave as full-screen overlays. Tone = `Warm`.

### 1.2 Defects
- **F-SHELL-1 (Critical, Frontend) — "Update" is a junk-drawer tab.** Four distinct products (Doc 04 §"every subtab is a product") are flattened into one tab gated by one picker. This is the root IA failure. The picker's selection is *modal state the teacher must re-establish on every entry* — there is no "current class/period" context inherited from the schedule.
- **F-SHELL-2 (High, Frontend) — `badge = 1` is hardcoded** on the Update nav item. It always shows a "1," crying wolf — never tied to real pending work.
- **F-SHELL-3 (High, Frontend) — Homework is inside Update but ignores the shared picker**, creating an inconsistent mental model: three sub-tabs need a class picked up top, the fourth doesn't, and the comment says Homework "authors its own class field" though the screen has no create form at all (§6).
- **F-SHELL-4 (Med, Frontend) — No schedule/morning context.** The brief's morning cognitive context (check schedule, mark own attendance) has no home in this shell. The first thing a teacher sees is "Welcome back" + tasks, not "here's your day."
- **F-SHELL-5 (Med, Frontend) — Calendar overlay reuses the discovery/parent `AcademicCalendarScreenV2`** — a read-only month grid, not a teacher timetable. Tapping a Home task opens this generic calendar (`onTap = onOpenCalendar` for *every* task), which is unrelated to the task.

**Persona:** Mr. Rao opening "Update → Attendance" must scroll a chip row, find "6-A · Science," tap it, then mark — then to do 6-B he taps back into Update and the picker is blank again. Six times. Aanya sees the permanent "1" badge and never learns what it means.

---

## 2. Home — `TeacherHomeScreenV2.kt` + `TeacherHomeViewModel`

### 2.1 What it does
Greeting header (bell + avatar), a "Leave requests" entry card, then `VStateHost`-gated content: **Today's tasks** (colour-coded `TaskCard`s), **Today's periods** (horizontal scroller), **At a glance** (4 live counts: classes today, pending attendance, pending marks, homework due).

### 2.2 Defects
- **F-HOME-1 (Critical, Backend+Frontend) — "Today's tasks" are fake.** Each card is a uniform "Mark attendance · 6-A · Science" with `onTap = onOpenCalendar` (opens the month grid, not attendance). They never reflect marks/syllabus/homework, never mark done (B-HOME-4). The colour-coding (`toneForTask`) maps types that the backend never emits anything but `attendance` for.
- **F-HOME-2 (Critical, Backend) — Tasks/counts double per subject** (B-HOME-2): a teacher of 6-A Maths+Science sees two "Mark attendance · 6-A" cards and "pending attendance = 2" for one homeroom.
- **F-HOME-3 (High, Backend) — No "you haven't checked in" prompt** (B-HOME-5). The brief's first-open biometric check-in is entirely absent from Home.
- **F-HOME-4 (High, Frontend) — Empty state is ambiguous.** `isEmpty = periods.isEmpty() && tasks.isEmpty()` → "Nothing scheduled." On a fresh school (no timetable, no real tasks) this is the *normal* state, indistinguishable from breakage. Aanya can't tell if she's done or stuck.
- **F-HOME-5 (Med, Frontend) — Tapping a task always opens the calendar overlay**, never the relevant tool. Dead-end navigation.
- **F-HOME-6 (Med, Frontend) — "At a glance" counts aren't tappable** — they state a number but don't route to the work (pending attendance → attendance, etc.).
- **F-HOME-7 (Low, Frontend) — Greeting is time-agnostic** ("Welcome back"), not "Good morning, here's your 9:15 class," missing the time-sensitive warmth the brief asks for.

**Persona:** Aanya wants the app to tell her what to do next; instead she gets identical cards that open a calendar. Mr. Rao ignores Home entirely because the numbers don't take him anywhere.

---

## 3. Update › Attendance — `TeacherAttendanceScreenV2.kt` + `TeacherAttendanceViewModel`

### 3.1 What it does
Two disabled inputs (class name / date "Today"), **Mark all present** button, a `VCard` list of students with P/A/L pills, a sticky summary (marked/remaining + progress bar) and a result-driven **Submit** (loading/success/error states are honest — a genuine strength).

### 3.2 Defects
- **F-ATT-1 (Critical, Frontend) — Date is a disabled, non-editable field showing "Today."** The brief explicitly requires "date defaulting to today with the ability to correct (for missed previous-day marking)." There is **no date picker** — back-dating is impossible from the UI even though the API accepts a `date` param.
- **F-ATT-2 (Critical, Backend) — Leave default not shown.** Students on approved leave still default to "present" (B-ATT-1). No `L`-from-leave state, no visual distinction.
- **F-ATT-3 (High, Frontend) — Only P/A/L; no "leave" as distinct from "late."** The pill `L` is *Late*. There is no *Leave* status at all (and the schema can't store it, D-ATT-2).
- **F-ATT-4 (High, Frontend+Backend) — No "already submitted" treatment.** Re-entering a marked class shows the marks as editable defaults with no banner, no lock, no "submitted at 9:20 by you." Mr. Rao can't tell which of his 6 classes he's already done.
- **F-ATT-5 (High, Frontend) — Dense list, tiny targets.** 40 students × 3 pills at `padding(horizontal=10, vertical=6)` on a single scroll. No search, no sort, no jump-to-roll, no bulk "mark rest absent." For Mr. Rao's 40-student classes this is a thumb marathon; for accessibility (58 y/o) the 10px pills are below comfortable tap size.
- **F-ATT-6 (Med, Frontend) — "Mark all present" then individual override is fine, but there's no "mark all absent" / "invert"** for the (rarer but real) day most are absent.
- **F-ATT-7 (Med, Frontend) — Class/date inputs are disabled text, not the selection mechanism** — selection lives in the *portal-level* picker above, so the screen's own header is decorative and confusing.

**Root cause split:** date & leave (Schema+Backend), density & targets & already-marked UX (Frontend).

**Persona:** Mr. Rao missed marking yesterday (a common reality) and **literally cannot** record it. Aanya marks 6-A, leaves, comes back to fix one student, and gets no signal she'd already submitted.

---

## 4. Update › Marks — `TeacherMarksScreenV2.kt` + `TeacherExamPicker` + `TeacherMarksViewModel` / `TeacherAssessmentsViewModel`

### 4.1 What it does
Below the class picker, an **exam picker** (lists assessments; inline "New exam" form: name + max marks) then the marks grid: assessment header, per-student number field, a live class-average footer, a result-driven **Save**.

### 4.2 Defects
- **F-MK-1 (Critical, Frontend+Backend) — Saving marks silently publishes to parents** (B-MK-1). The button says "Save marks," not "Publish results." There is no draft/publish distinction in the UI; the teacher has no idea parents are notified instantly.
- **F-MK-2 (Critical, Frontend) — Must hand-create every exam, every time, with no scheduling.** The only way to get an `exam_id` is the inline create (name + max). No date, no type, no "scheduled test arrived today" surfacing (B-MK-4). The exam picker has no date even though the API accepts `exam_date`.
- **F-MK-3 (High, Frontend) — No absent handling.** Empty field = "not entered" = same as "absent" visually. The teacher can't mark a student absent-from-exam (D-ASMT-5).
- **F-MK-4 (High, Frontend) — No entry validation feedback.** Typing 120 into a /100 exam is silently clamped on the server; the field shows 120 until save, then nothing tells the teacher it was changed (B-MK-2). No pass-mark colouring.
- **F-MK-5 (High, Frontend) — 40 number fields, no keyboard flow.** Each `VInput(keyboardType=Number)` is independent; no "next" advancing, no roll-ordered tab, no fast entry. For a 40-student class this is brutal.
- **F-MK-6 (Med, Frontend) — No history / comparison.** "Class avg (live)" is the only analytic; no previous-assessment view, no per-student trend (B-MK-6).
- **F-MK-7 (Med, Frontend) — Exam picker `selectExam` bubbles via `LaunchedEffect(state.selectedExamId)`** — a fragile round-trip through two ViewModels (assessments VM → portal state → marks VM) that can race on fast class switches.

**Persona:** Aanya, terrified of publishing wrong marks, has no "save draft." Mr. Rao has to create "Unit Test 1" by hand for each of 6 classes and then thumb-type 240 scores with no keyboard assistance.

---

## 5. Update › Syllabus — `TeacherSyllabusScreenV2.kt` + `TeacherSyllabusViewModel`

### 5.1 What it does
Class/subject (disabled) inputs, an **overall progress** card + bar, and a tappable unit checklist (circular `CoverToggle` that flips coverage and stamps `covered_on`).

### 5.2 Defects
- **F-SYL-1 (Critical, Backend/Schema) — Permanently empty on real installs.** No unit-creation path and no seed (B-SYL-1, D-SYL-2). GET returns `[]` → "No units" empty state forever. The toggle interaction (which is actually nicely lightweight, satisfying the brief's "lighter than a form" goal) has nothing to toggle.
- **F-SYL-2 (High, Frontend) — No way to add a unit.** The screen is read-then-toggle only; a teacher with a real syllabus cannot enter it. There's no "+ Add chapter."
- **F-SYL-3 (Med, Backend) — Toggle can succeed on GET but 403 on PATCH** when class labels drift (B-SYL-2): the unit shows but won't toggle for the same teacher.
- **F-SYL-4 (Low, Frontend) — No grouping by chapter/topic hierarchy** — flat list only; fine for small syllabi, weak for a full-year curriculum.

**Persona:** Both personas open Syllabus on day one and see "No units." Aanya assumes it's broken. Mr. Rao, who *has* a printed syllabus, finds no way to enter it.

---

## 6. Update › Homework — `TeacherHomeworkScreenV2.kt` + `TeacherHomeworkViewModel`

### 6.1 What it does
A list of homework cards (title, subject·class·due, submitted/total badge or "Just assigned", progress bar) + a big **"Assign new homework"** button.

### 6.2 Defects
- **F-HW-1 (Critical, Frontend) — The "Assign new homework" button is dead.** `onAssign = { /* Phase 3E: open the create sheet */ }`. The single primary action of the screen does nothing. (Backend `POST /homework` exists — the UI was never wired.)
- **F-HW-2 (Critical, Frontend+Backend) — No status board.** Tapping a homework card does nothing (no `onClick`). The brief's "who submitted, who hasn't, who late, open & review" has no screen and no endpoint (B-HW-2).
- **F-HW-3 (High, Frontend+Backend) — No attachment, no due-date picker, no class picker** for creating homework (even if the button worked) — and no extension control (B-HW-1/2/3).
- **F-HW-4 (Med, Frontend) — Cards show subject·class from drift-prone strings**; "Just assigned" vs progress depends on `total_count` (B-HW-5 denominator fragility).

**Persona:** Aanya taps "Assign new homework" expecting a form; nothing happens. She concludes the app is broken (she's right). Mr. Rao can't see who actually did the homework.

---

## 7. My Classes — `TeacherClassesScreenV2.kt` + `TeacherClassesViewModel`

### 7.1 What it does
"My Classes" title, a 2-column grid of class cards (name, subject, student count, attendance %). Tapping a card opens **ClassDetail**: 3 mini-stats (students / attendance / syllabus) + a **"Message class parents"** composer (RA-51, works) + a `VComingSoon` "Student roster" placeholder.

### 7.2 Defects
- **F-CLS-1 (Critical, Frontend) — The roster is a `VComingSoon` placeholder.** The Classes tab's core promise — "every student enrolled, with attendance rate, latest performance, flags" (Doc 09) — is an honest "coming soon" card. There is no roster endpoint (B-MISSING) and no UI.
- **F-CLS-2 (High, Backend/Frontend) — One card per (class, subject), not per class.** Teach 6-A for 3 subjects → three "6-A" cards (B-CLS-4). The tab can't represent "6-A, my homeroom, where I teach Maths + Science."
- **F-CLS-3 (High, Backend) — "Attendance %" is a lifetime average** parsed from the `grade` string (B-CLS-3), not "this week/month."
- **F-CLS-4 (Med, Frontend) — No next-period, no timetable, no assessment schedule, no active homework** on the detail (all required by Doc 09). Detail = 3 stats + message + placeholder.
- **F-CLS-5 (Med, Frontend) — No student → student-profile drill-down** (no endpoint, B-MISSING).
- **F-CLS-6 (Low, Frontend) — Edit pencil in the detail header is decorative** (`Icon(Edit3)` with no action).

**Persona:** Mr. Rao opens 6-A hoping to see his 40 students and a flagged at-risk child; he gets "coming soon." Aanya can't look up a student before a parent call.

---

## 8. Profile — `TeacherProfileScreenV2.kt` + `TeacherProfileViewModel`

### 8.1 What it does
Centered avatar + name + username + school + subject badges; setting rows (Personal details with real phone/email, Classes, Notification preferences, Change password, Help & support — only Help is wired to a mailto); confirm-gated **Log out** (good).

### 8.2 Defects
- **F-PROF-1 (Med, Frontend+Backend) — "Notification preferences" and "Change password" rows are inert** (no `onClick`). Change-password backend exists but isn't linked (B-PROF-2).
- **F-PROF-2 (Med, Frontend) — No edit profile** (photo/phone) — read-only.
- **F-PROF-3 (Low, Frontend) — No staff id / department** even though `faculty.external_id`/`department` exist.

**Persona:** Aanya wants to change her initial password from Profile (she was provisioned with `must_change_password`); the row does nothing.

---

## 9. Leave overlay — `TeacherLeaveScreenV2.kt` + `TeacherLeaveViewModel`

### 9.1 What it does
Lists student leave routed to the teacher's classes with approve/reject; honest states.

### 9.2 Defects
- **F-LV-1 (High, Backend) — Approving does not mark the student on-leave in attendance** (B-LV-2 / B-ATT-1). The two systems are disconnected, so the teacher approves leave and then still has to (incorrectly) mark the student present/absent manually.
- **F-LV-2 (Med, Backend) — Any subject teacher can decide** (B-LV-1); no single-owner clarity, possible double decisions.

---

## 10. The data layer — `TeacherApi.kt`, `TeacherRepositoryImpl`, ViewModels

- **F-DATA-1 (Med) — Marks selection round-trips through three layers** (`TeacherExamPicker` VM → portal `selectedExamId` → `TeacherMarksViewModel.load`) via `LaunchedEffect`, which can race when switching classes quickly (F-MK-7).
- **F-DATA-2 (Med) — Every screen re-fetches on entry with no caching/SWR**; switching Update sub-tabs reloads classes each time (the picker's `TeacherClassesViewModel` reloads).
- **F-DATA-3 (Low) — Token fetched per call** (`preferenceRepository.getUserToken().first()`) in each VM; fine, but no central interceptor — consistent with the parent side though.
- **F-DATA-4 (Strength) — Submit flows are result-driven** (no fake timers): `isSubmitting`/`submitSuccess`/`submitError` are honest. **Keep this pattern.**

---

## 11. Accessibility & the 19↔58 tension (applies to all screens)

| Tension point | 19 (Aanya) needs | 58 (Mr. Rao) needs | Current state | Resolution (forward ref) |
|---|---|---|---|---|
| Tap targets | dense ok | ≥44dp comfortable | 10px pills, 80dp number boxes | Doc 10 sizing floor |
| Empty vs broken | clear "you're done" | clear "nothing to do" | ambiguous "Nothing scheduled" | Doc 10 empty-state spec |
| Context memory | fast | essential | picker resets every visit | Doc 04 schedule-anchored context |
| Bulk entry (40 students) | helpful | essential | none | Doc 06/07 bulk + keyboard flow |
| Back-date | occasional | frequent (missed days) | impossible | Doc 06 date picker |
| Destructive publish | terrifying | unaware | silent auto-publish | Doc 07 explicit publish |
| Discoverability | icons ok | labels needed | mixed; dead buttons | Doc 10 labelled actions |

**The core tension:** Aanya tolerates density and icon-only UI; Mr. Rao needs larger targets, labels, and forgiveness (undo, confirm, back-date). The resolution is **not** two modes — it's a single design tuned to Mr. Rao's floor (target size, labelling, confirmation) that stays fast for Aanya (keyboard flow, bulk actions, schedule-anchored context that removes the picker entirely).

---

## 12. Root-cause attribution summary

| Screen | Worst defect | Root layer |
|---|---|---|
| Shell | "Update" junk-drawer + resetting picker | Frontend (IA) |
| Home | fabricated tasks, no check-in | Backend + Schema |
| Attendance | no back-date, no leave default | Frontend + Schema |
| Marks | silent auto-publish, manual exams | Backend + Frontend |
| Syllabus | permanently empty (no create/seed) | Backend + Schema |
| Homework | dead create button, no status board | Frontend + Backend |
| Classes | roster is "coming soon" | Backend + Frontend |
| Profile | inert setting rows | Frontend |
| Leave | approval ≠ attendance | Backend + Schema |

**Verdict:** roughly **half the teacher portal's failures are schema/backend** (empty data, no endpoints, wrong aggregates) and **half are frontend** (IA, dead buttons, missing pickers, density). A frontend-only "polish" pass would fix the dead buttons and density but leave Syllabus/Marks/Classes/Home structurally hollow. The rebuild must be full-stack and sequenced schema → backend → frontend (Doc 11).

---

*End of 03_CURRENT_STATE_AUDIT.md*
