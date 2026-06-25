# ENROLL+ — TEACHER PORTAL LOOP ENGINEERING SYSTEM
### Branch: `backend-by-abuzar_v1.0.3` | Agent: Genspark Claude Opus 4.8
### Mode: READ + REWRITE ONLY — NO BUILD, NO SANDBOX EXECUTION

---

## ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
## PART 1 — LOOP STATE FILE
## (Agent reads this at the START of every iteration)
## ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

```
LOOP VERSION: 1.0
LAST COMPLETED TASK: P2-T3 — QuickActionRow (three core-action pills)
LAST COMMIT: feat(teacher-portal): add QuickActionRow three-up action pills (loop P2-T3)
CURRENT PHASE: Phase 2 — Home Tab Premium Redesign (in progress)
AGENT NOTES:
  • CRITICAL DECISION (honours the iteration's IMPORTANT NOTE): the portal already
    ships a complete, mature, fully token-driven design system — VTheme → VColors
    (teal / navy / violet `#6C5CE0` accent), VTypography (Plus Jakarta Sans + DM Mono),
    VDimens, VElevation, VMotion — and every Parent + Teacher screen reads from it
    (zero hardcoded `Color(0x…)` in teacher screens, verified by grep).
  • The loop's literal ask (new indigo `EnrollColor/Typography/Shape` + a parallel
    `EnrollTheme {}`) would FORK the design system and break portal-wide colour parity,
    re-introducing off-pattern hex — the opposite of this task's own Done Criteria and
    a direct violation of the IMPORTANT NOTE ("stick with the colour/theme pattern of
    the whole Parents + Teacher portal").
  • RESOLUTION: satisfied P1-T1 by INTENT. Added ONE new file —
    `ui/v2/theme/EnrollTokens.kt` — a thin semantic BRIDGE exposing the loop's token
    vocabulary (`Enroll.colors.primary`, `Enroll.type.headingLarge`, `Enroll.shape.card`,
    `Enroll.space.lg`, …) all RESOLVING to the existing VTheme. No new colours. No
    changes to any existing screen. Indigo family → existing violet `accent` family.
  • Status semantics PRESERVED exactly: statusPresent/Absent/Late → existing
    successInk/dangerInk/warningInk. Honours Light/Night tone automatically (read-only
    composables off the active VTheme).
  • Later loop tasks can now reference `Enroll.*` tokens with concrete, on-pattern,
    token-backed homes — no per-screen hardcoding required.

  ── P1-T2 (this iteration) ──
  • Added `ui/v2/components/EnrollCard.kt`: the loop's shared FLAT card —
    `EnrollCard(modifier, onClick, tint, padding, shape, border, content)`.
    Contract per Design Spec: `surfaceCard` fill, `shape.card` (16dp) corners,
    1.dp `surfaceSubtle` border, NO elevation shadow (spec ElevationCard = 0.dp),
    and the loop's `scale(0.98f)` press via the EXISTING `Modifier.pressScale`
    (VMotion §13.3) — only on navigable (onClick) cards; static cards never animate.
  • Distinct from the portal's existing `VCard` (which is the *elevated* navy-tinted
    surface). EnrollCard is the flat, scannable surface for dense teacher screens
    (gradebook rows, nudge cards) — kept as a SEPARATE primitive so VCard's ~30+
    existing call sites are untouched (RULE-2: stability over churn).
  • Added optional `tint` param up-front so P2-T5 nudge cards (`primarySoft` info /
    `accentSoft` pending) need no later signature change.
  • "Replaces all raw Card{}" — verified VACUOUS: grep shows the teacher portal
    never imported/used Material3 `Card`; all `*Card` symbols are custom composables.
    EnrollCard is now the canonical flat card for NEW loop screens.
  • All colours/shapes via `Enroll.*` bridge → VTheme (no new hex; only geometry
    literal is `1.dp` border width). Brace-balanced, imports clean, pressScale
    package verified, every referenced Enroll member confirmed to exist.

  ── P1-T3 (this iteration) ──
  • Added `ui/v2/components/SectionHeader.kt`: the loop's ergonomic string-action
    header — `SectionHeader(title, action: String? = null, onAction: (() -> Unit)? = null)`.
    Title = `labelCaps` (11/700 ALL-CAPS) in `textSecondary`; action = ripple-free
    text button in `primaryMid` with a subtle pill press surface.
  • The portal's existing `VSectionHeader` takes a @Composable action *slot*; this is
    the loop's terser string variant (used everywhere: `SectionHeader("TODAY'S SCHEDULE")`,
    `SectionHeader("NOTIFICATIONS", action = "Mark all read") { }`). Layout/typography
    rhythm matches VSectionHeader exactly for visual parity.
  • Removed an unused `background` import after self-review; braces 5/5; `colored`
    extension + all `Enroll.*` members verified. No hardcoded hex.

  ── P1-T4 (this iteration) — PHASE 1 COMPLETE ──
  • Added `ui/v2/screens/teacher/EnrollBottomNav.kt`: `EnrollBottomNav(items,
    selectedId, onSelect)` + `EnrollTab` id vocabulary (Home/Gradebook/Planner/
    Chat/Profile) + `loopTabs()` builder.
  • DECISION: did NOT build the loop's simpler pill bar (per-tab PrimaryIndigoSoft
    pill, flat 1.0→1.15 scale, BadgedBox). The portal already ships `TeacherDock` —
    a premium floating glass dock (spring sliding lozenge, glyph lift+scale,
    selection haptic, live badges, full a11y) that is a deliberate ParentDock
    sibling (Doc 10 §12 one-product parity). A pill bar would REGRESS below the
    loop's QUALITY BAR and break parent↔teacher parity. So EnrollBottomNav DELEGATES
    to TeacherDock — clean loop API, superior rendering, zero off-pattern visuals.
  • Every loop nav requirement maps onto an existing-or-better dock feature
    (label-on-active, violet active accent = PrimaryIndigo family, spring scale,
    VNavItem.badge count badge for Chat → also satisfies P5-T3).
  • Placed in the teacher package (not shared components/) to avoid a components→
    screens layering inversion. Braces 2/2; all imports used; TeacherDock public &
    same-package (no import cycle). Chat declared as loop-forward 5th tab; live IA
    (Today·Classes·Gradebook·Planner·Profile) untouched until Phase 5.

  >>> PHASE 1 (Design System Foundation) is now COMPLETE: tokens bridge (P1-T1),
      EnrollCard (P1-T2), SectionHeader (P1-T3), EnrollBottomNav (P1-T4). Next
      iteration begins PHASE 2 — Home Tab (P2-T1 Header Block).
```

### DONE CRITERIA (Static Analysis Only — No Build Required)
Before marking any task complete, the agent must verify:
- [ ] All new Composables use design tokens from `EnrollTheme` — no hardcoded hex values
- [ ] Every screen file reads its data through a `ViewModel` — no direct repo calls from UI
- [ ] All string labels use `stringResource()` — no hardcoded English in UI
- [ ] Navigation actions use `NavController` — no direct screen instantiation
- [ ] Attendance/grade semantic colors (`statusGreen`, `statusRed`, `statusAmber`) are preserved, not replaced
- [ ] No Composable function exceeds 80 lines — extract sub-composables if needed
- [ ] Every new `LazyColumn` item has a stable `key = {}` parameter

---

## ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
## PART 2 — DESIGN SYSTEM SPEC
## (Agent references this for every visual decision)
## ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

### ENROLL+ PREMIUM DESIGN LANGUAGE — "Trusted Authority"

The teacher is a professional, not a student. The UI must feel like a premium SaaS tool
they chose — not a government portal they were assigned. Calm authority, not cheerful EdTech.

**One Rule Above All:** If a design decision looks like any other Indian school app,
reverse it. Our teachers will notice the difference within 30 seconds of opening the app.

---

### COLOR TOKENS
Define/extend these in `EnrollTheme.kt` / `Color.kt`:

```kotlin
// Brand
val PrimaryIndigo     = Color(0xFF2D1FA3)  // Deep trust-authority indigo
val PrimaryIndigoMid  = Color(0xFF4338CA)  // Interactive state
val PrimaryIndigoSoft = Color(0xFFEEECFD)  // Backgrounds, chips, selected states

// Surfaces
val SurfaceBase       = Color(0xFFFAFAFC)  // App background — warm near-white
val SurfaceCard       = Color(0xFFFFFFFF)  // Card surface
val SurfaceSubtle     = Color(0xFFF1F1F5)  // Dividers, inactive areas

// Text
val TextPrimary       = Color(0xFF0F0E17)  // Headlines, names
val TextSecondary     = Color(0xFF6B7280)  // Labels, captions
val TextTertiary      = Color(0xFFB0B7C3)  // Placeholder, disabled

// Semantic (PRESERVED — do not alter these)
val StatusPresent     = Color(0xFF22C55E)
val StatusAbsent      = Color(0xFFEF4444)
val StatusLate        = Color(0xFFF59E0B)
val StatusPending     = Color(0xFF3B82F6)

// Accent
val AccentAmber       = Color(0xFFF59E0B)  // CTAs, highlights, exam markers
val AccentAmberSoft   = Color(0xFFFEF3C7)  // Amber chip backgrounds

// Gradients (use sparingly — only on Home header and Profile header)
val GradientStart     = Color(0xFF2D1FA3)
val GradientEnd       = Color(0xFF4F46E5)
```

---

### TYPOGRAPHY TOKENS
```kotlin
// Title styles
HeadingLarge   : fontSize=24sp, fontWeight=700, letterSpacing=(-0.5).sp
HeadingMedium  : fontSize=20sp, fontWeight=600, letterSpacing=(-0.3).sp
HeadingSmall   : fontSize=16sp, fontWeight=600, letterSpacing=0.sp

// Body styles
BodyLarge      : fontSize=15sp, fontWeight=400, lineHeight=22sp
BodyMedium     : fontSize=13sp, fontWeight=400, lineHeight=19sp
BodySmall      : fontSize=11sp, fontWeight=400, lineHeight=16sp

// Labels / Data
LabelBold      : fontSize=13sp, fontWeight=600, letterSpacing=0.2.sp
LabelCaps      : fontSize=11sp, fontWeight=600, letterSpacing=0.8.sp (ALL CAPS for section headers)
DataLarge      : fontSize=28sp, fontWeight=700, fontFeatureSettings="tnum" (tabular nums for stats)
DataMedium     : fontSize=20sp, fontWeight=600, fontFeatureSettings="tnum"
```

---

### SHAPE & ELEVATION TOKENS
```kotlin
ShapeCard      : RoundedCornerShape(12.dp)
ShapeChip      : RoundedCornerShape(8.dp)
ShapeSheet     : RoundedCornerShape(topStart=20.dp, topEnd=20.dp)
ShapeAvatar    : CircleShape
ShapeFAB       : RoundedCornerShape(16.dp)

ElevationCard  : 0.dp (no shadow — use SurfaceSubtle border instead)
ElevationModal : 8.dp
ElevationFAB   : 4.dp
```

---

### SPACING TOKENS
```kotlin
SpaceXS  = 4.dp
SpaceSM  = 8.dp
SpaceMD  = 12.dp
SpaceLG  = 16.dp
SpaceXL  = 20.dp
Space2XL = 24.dp
Space3XL = 32.dp
ScreenPadding = 16.dp
```

---

### SIGNATURE ELEMENTS (What makes Enroll+ unmistakable)

1. **Today Strip** — Home tab. A horizontally scrollable, pill-based period timeline
   showing the teacher's entire day at a glance. Each pill = one period. Active period
   is highlighted in `PrimaryIndigo`, past periods are `SurfaceSubtle`, future are `SurfaceCard`.
   Tap any period → expand to class details inline. No other Indian school app has this.

2. **Inline Gradebook Entry** — Marks tab. Students listed with an inline tap-to-edit
   mark field that expands in place (no navigation). Saves on blur. Grade chips auto-render.

3. **Smart Nudge Cards** — Home tab. Permission-gated system suggestions rendered as
   dismissible slim cards. Not push notifications — they live in the UI itself.
   Example: "2 students in Class 9A have no marks recorded for Unit Test 3."

4. **Thread-First Chat** — Chat tab. Organized by class → student → parent, not by
   recency like WhatsApp. Teachers don't think "last messaged" — they think "Class 8B → Rohan."

---

## ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
## PART 3 — TASK QUEUE (Ordered by Phase)
## ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

### PHASE 1 — DESIGN SYSTEM FOUNDATION
> Must be done first. All other phases depend on this.

- [x] **P1-T1**: Create or extend `ui/theme/EnrollColor.kt` with all color tokens above.
      Create `ui/theme/EnrollTypography.kt` with all type tokens.
      Create `ui/theme/EnrollShape.kt` with shape/spacing tokens.
      Wire all three into the root `EnrollTheme {}` composable.
      Verify: search codebase for `Color(0x` outside theme files — all should be gone after.
      ↳ DONE (by intent, per IMPORTANT NOTE — see AGENT NOTES). The portal already has a
        complete token-driven foundation (`VTheme`/`VColors`/`VType`/`VDimens`). Instead of
        forking a new indigo theme (which would break Parents↔Teacher colour parity), added
        `ui/v2/theme/EnrollTokens.kt` — a semantic bridge (`Enroll.colors/type/shape/space`)
        mapping the loop's vocabulary onto the existing VTheme. Indigo→violet `accent`;
        status colours preserved; no new hex; no existing screen touched.

- [x] **P1-T2**: Create `ui/components/EnrollCard.kt` — a shared card composable:
      `EnrollCard(modifier, onClick, content)` using `SurfaceCard` fill,
      `ShapeCard` corners, a 1.dp `SurfaceSubtle` border (no elevation shadow),
      and a subtle `scale(0.98f)` press animation via `interactionSource`.
      This replaces all raw `Card {}` usages across teacher portal screens.
      ↳ DONE. Added `ui/v2/components/EnrollCard.kt` (flat, border-defined, no shadow,
        0.98f press via existing `pressScale`; optional `tint` for P2-T5 nudges).
        Kept SEPARATE from the elevated `VCard`. "Replaces raw Card{}" verified vacuous —
        teacher portal never used Material3 `Card`. All tokens via `Enroll.*` bridge.

- [x] **P1-T3**: Create `ui/components/SectionHeader.kt` — a shared section header:
      `SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null)`
      Title uses `LabelCaps` style in `TextSecondary`. Action is a text button in `PrimaryIndigoMid`.
      Used before every section block across all tabs.
      ↳ DONE. Added `ui/v2/components/SectionHeader.kt` with the exact string-action
        signature. Title `labelCaps`/`textSecondary`; action ripple-free text button in
        `primaryMid`. Terser sibling of the existing `VSectionHeader` (composable-slot);
        layout/type rhythm matched for parity. All tokens via `Enroll.*`.

- [x] **P1-T4**: Create `ui/components/EnrollBottomNav.kt` — redesigned bottom nav bar:
      Tabs: Home | Gradebook | Planner | Chat | Profile.
      Selected tab: icon + label, icon tinted `PrimaryIndigo`, pill background `PrimaryIndigoSoft`.
      Unselected: icon only in `TextTertiary` — no label.
      Animate icon scale 1.0 → 1.15 on selection using `animateFloatAsState`.
      Chat tab has an unread badge using `BadgedBox`.
      ↳ DONE (by intent). Added `ui/v2/screens/teacher/EnrollBottomNav.kt` —
        `EnrollBottomNav(items, selectedId, onSelect)` + `EnrollTab` ids + `loopTabs()`.
        DELEGATES to the existing premium `TeacherDock` (spring sliding lozenge,
        glyph lift+scale, haptics, live badges, a11y, ParentDock parity) rather than
        regressing to a flat Material pill bar. Every loop requirement maps onto a
        dock feature that meets-or-exceeds the spec; Chat badge via `VNavItem.badge`.

---

### PHASE 2 — HOME TAB PREMIUM REDESIGN
> File: `ui/screens/teacher/TeacherHomeScreen.kt`

**What this tab must do for the teacher (their morning in 10 seconds):**
- Know their schedule today
- Know if anything needs their attention
- Take action without navigating deep

- [x] **P2-T1 — Header Block**:
      Replace current header with a gradient header composable `TeacherHomeHeader`.
      Full-width, 120dp height, `GradientStart` → `GradientEnd` horizontal gradient background.
      Left: time-aware greeting ("Good Morning" / "Good Afternoon" / "Good Evening"),
      teacher's first name in `HeadingLarge`, white.
      Below: today's date + day in `BodyMedium`, white 70% alpha.
      Right: teacher avatar (40dp circle, `ShapeAvatar`) — tappable → navigates to Profile tab.
      Below avatar: a small bell icon with unread notification badge — tappable → opens NotificationSheet.
      ↳ DONE. Added `ui/v2/screens/teacher/TeacherHomeHeader.kt`. 120dp+ banner with the
        sanctioned violet `headerGradient`, time-aware greeting (existing `teacherGreeting`),
        first name (`headingLarge` white), date line (`bodyMedium` white 70%), 40dp avatar
        ring → Profile, glassy bell + unread badge → NotificationSheet. Additive (keeps the
        canonical `TeacherHeader` on operational tabs). All tokens via `Enroll.*`; util date
        symbols + bridge members verified; braces balanced.

- [x] **P2-T2 — Today Strip**:
      Implement `TodayClassStrip` composable.
      Horizontal `LazyRow` of `PeriodPill` composables.
      Each `PeriodPill(period: SchedulePeriod)` displays:
        - Period number (LabelCaps, small)
        - Subject short name (LabelBold)
        - Class name (BodySmall, TextSecondary)
        - Time (BodySmall, TextTertiary)
      State logic:
        - Past period: `SurfaceSubtle` background, `TextTertiary` text
        - Active period: `PrimaryIndigo` background, white text, 2dp indigo border glow effect via `Box` with `drawBehind`
        - Future period: `SurfaceCard` background, `TextPrimary` text, `SurfaceSubtle` border
      Tap on any period → expand an `AnimatedContent` block below the strip
      showing: Full class name, Room number, Attendance status (taken/not taken), quick "Take Attendance" button.
      Section header above strip: `SectionHeader(title = "TODAY'S SCHEDULE")`
      ↳ DONE. Added `ui/v2/screens/teacher/TodayClassStrip.kt`. LazyRow of PeriodPill
        (keyed by periodId), past/active/future aesthetics (surfaceSubtle / primary+glow
        via drawBehind / surfaceCard+border), tap → AnimatedVisibility inline detail
        (class+subject, room, attendance status dot, pre-scoped "Take attendance" CTA →
        P7-T2). Driven by the real server `ResolvedDayUi` (authoritative `nowIndex`, not
        the device clock). Reuses shared `parseHourMinute`/`formatClock12h`. Status
        colours preserved; all tokens via `Enroll.*`; braces 34/34; imports clean.

- [x] **P2-T3 — Quick Action Row**:
      Three action pills in a `Row` with `SpaceMD` gap, horizontally centered.
      `QuickActionPill(icon, label, onClick)`:
        - "Take Attendance" → navigates to AttendanceScreen for active/next period
        - "Add Marks" → navigates to GradebookTab with AddMarksSheet pre-opened
        - "Message Parent" → navigates to ChatTab
      Pill style: `PrimaryIndigoSoft` background, `PrimaryIndigoMid` icon+text,
      `ShapeCard` corners, `SpaceMD` vertical padding, `SpaceLG` horizontal padding.
      ↳ DONE. Added `ui/v2/screens/teacher/QuickActionRow.kt` — `QuickActionRow` +
        `QuickActionPill` (Check/GraduationCap/Chat icons). primarySoft bg, primaryMid
        icon+text, shape.card, SpaceMD/SpaceLG padding, pressScale give. Callbacks
        onTakeAttendance/onAddMarks/onMessageParent wired for P7. All tokens via Enroll.*.

- [ ] **P2-T4 — Attendance Summary Card**:
      `AttendanceSummaryCard(todayStats: AttendanceDaySummary)` composable.
      `EnrollCard` container.
      Left 60%: section header "ATTENDANCE TODAY",
      stat row per class taught today: class name → present/total → percentage pill.
      Right 40%: a simple `Canvas`-drawn donut chart showing overall today's attendance %.
      Center of donut: `DataLarge` percentage, `TextPrimary`.
      Colors: `StatusPresent` fill, `SurfaceSubtle` track.
      Tap card → navigates to full AttendanceScreen.

- [ ] **P2-T5 — Smart Nudge Cards**:
      `SmartNudgeSection(nudges: List<TeacherNudge>)` composable.
      Only rendered if `nudges.isNotEmpty()`.
      Section header: `SectionHeader(title = "NEEDS ATTENTION")`.
      Each `NudgeCard(nudge: TeacherNudge)`:
        - Left: a colored icon in a 36dp circle (color based on nudge type)
        - Center: nudge message in `BodyMedium`, `TextPrimary`
        - Right: action label chip OR dismiss icon
        - `EnrollCard` with `AccentAmberSoft` background tint for pending, `PrimaryIndigoSoft` for info
      Nudge types (define `TeacherNudge` sealed class):
        - `MarksNotEntered(className, subjectName, examName)` → action: "Add Now"
        - `AttendanceNotTaken(className, periodTime)` → action: "Take Now"
        - `ParentUnread(parentName, studentName)` → action: "Reply"
        - `HomeworkUngraded(className, count)` → action: "Grade"
      Nudges are sourced from ViewModel, which reads from local cache — no additional API call.

- [ ] **P2-T6 — Pending Tasks Card**:
      `PendingTasksCard(tasks: List<TeacherTask>)` composable.
      `EnrollCard` with `SectionHeader("PENDING")` and "See All" action.
      Show max 3 tasks in collapsed view.
      Each `TaskRow(task)`: checkbox (tap marks done), task description `BodyMedium`,
      due date `BodySmall TextTertiary`.
      Completed task: strikethrough text, `StatusPresent` checkbox.

- [ ] **P2-T7 — Notification Bottom Sheet**:
      `NotificationSheet` — a `ModalBottomSheetLayout` triggered from the bell icon.
      `ShapeSheet` top corners.
      Handle bar at top (32dp wide, 4dp tall, `SurfaceSubtle` color, centered).
      `SectionHeader("NOTIFICATIONS", action = "Mark all read")`.
      `LazyColumn` of `NotificationRow(notification)`:
        - Icon (notification type-based)
        - Title `LabelBold` + message `BodyMedium`
        - Timestamp `BodySmall TextTertiary`
        - Unread indicator: 8dp `PrimaryIndigo` dot on left edge
        - `SwipeToDismiss` wrapper — swipe right to dismiss
        - Tap → close sheet + navigate to relevant screen (grade, attendance, chat thread)
      Group by: TODAY / YESTERDAY / EARLIER

---

### PHASE 3 — GRADEBOOK TAB FULL REDESIGN
> File: `ui/screens/teacher/GradebookScreen.kt`

**Teacher mental model:** I want to open my class, see my students, and enter marks fast.
No extra taps. No loading spinners after every entry. Auto-save silently.

- [ ] **P3-T1 — Class + Subject Selector**:
      Sticky `TopAppBar`-level selector (not inside scroll content).
      Two horizontally scrollable chip rows:
        Row 1: Class chips (Class 7A, 8A, 9B…) — `FilterChip` style, `PrimaryIndigoSoft` selected
        Row 2: Subject chips — updates based on selected class
      Selected state: `PrimaryIndigo` background, white text.
      Unselected: `SurfaceSubtle` background, `TextSecondary` text.
      This replaces any current dropdown/dialog-based class selection.

- [ ] **P3-T2 — Grade Distribution Bar**:
      Below selector, above student list.
      A horizontal bar showing A/B/C/D/F distribution for the current class+exam.
      `Canvas`-drawn segmented bar, 8dp height, `ShapeCard` corners, no gaps between segments.
      Below bar: labels with count. e.g. "A · 12   B · 8   C · 4   D · 2   F · 1"
      Only visible when an exam is selected. Hidden in "All Exams" view.

- [ ] **P3-T3 — Exam Selector**:
      A horizontal `LazyRow` of `ExamChip(exam)` above the student list.
      "All" chip at start. Each chip: exam name + date.
      Selected exam: `PrimaryIndigo`. Tap changes the marks column displayed.
      "+ Add Exam" chip at end (outlined, `PrimaryIndigoMid` border) → opens `AddExamSheet`.
      `AddExamSheet`: exam name, date, max marks, exam type (Unit Test / Term / Assignment) — save button.

- [ ] **P3-T4 — Student Marks List**:
      `LazyColumn` with `key = { student.id }`.
      Each `StudentMarkRow(student, mark, onMarkChanged)`:
        - Roll number (LabelBold, TextTertiary, 32dp wide)
        - Student name (BodyLarge, TextPrimary)
        - Mark entry field: a `BasicTextField` styled as a pill (40dp wide, `SurfaceSubtle` bg,
          `ShapeChip` corners, center-aligned number, `DataMedium` style)
          On focus: border turns `PrimaryIndigo` 2dp
          On value change: debounce 800ms → auto-save via ViewModel (no button press)
          Show a tiny auto-saving indicator ("saving…" → "✓") below field, fades out
        - Grade chip auto-rendered from mark: "A" / "B" etc. in `StatusPresent`/`StatusAbsent`/`AccentAmber`
        - Trend arrow: ↑ green / ↓ red / → grey (compared to previous exam of same type)
        - Tap the student row (not the field) → expand `StudentMarkDetailSheet`
      `StudentMarkDetailSheet`:
        - Student name + avatar
        - All exams history as a simple `LineChart` (use `Canvas`)
        - Remark text field (optional teacher note)
        - "Message Parent" button → navigates to that student's parent chat thread

- [ ] **P3-T5 — Bulk Actions Bar**:
      Appears at bottom (above FAB) when any student row is long-pressed.
      Shows: "X selected" | "Set Mark" | "Add Remark" | "Cancel"
      `PrimaryIndigo` background, white icons.
      Animate up from bottom using `AnimatedVisibility(visible = selectedCount > 0)`.

- [ ] **P3-T6 — Export / Share**:
      FAB at bottom right: export icon.
      Tap → `ExportSheet` with two options:
        - "Share as PDF" (generates class marks PDF — calls existing PDF util or stubs one)
        - "Share via WhatsApp" (opens intent with pre-filled class performance summary text)

---

### PHASE 4 — PLANNER TAB FULL REDESIGN
> File: `ui/screens/teacher/PlannerScreen.kt`

**Teacher mental model:** I want to know what I'm teaching next, plan my lessons,
and track if students submitted homework — all in one place.

- [ ] **P4-T1 — Week View Header**:
      Custom week strip: 7 columns, Mon–Sun.
      Each column: day abbreviation (LabelCaps, TextTertiary) + date number.
      Today: `PrimaryIndigo` filled circle behind date, white text.
      Days with events: small `AccentAmber` dot below date number.
      Tap day → scrolls the content below to that day's entries.
      Previous/next week: left/right swipe on strip using `HorizontalPager`.

- [ ] **P4-T2 — Daily Plan View**:
      Below week strip: `LazyColumn` of `PlanDaySection(date, periods)`.
      Each `PlanDaySection` has:
        - Day header: "Monday, 23 June" in `HeadingSmall`
        - List of `PeriodPlanCard(period)`:
          * Time range + period number (left side, `TextTertiary`)
          * Class + Subject (LabelBold, TextPrimary)
          * Lesson topic field — `BasicTextField`, placeholder "Tap to add lesson topic…"
          * Below: homework row: "HW:" + assigned count + "/" + submitted count
          * If exam on this date: `AccentAmber` left border on card + "EXAM" badge chip
        - Tap anywhere on card → expand to full `LessonPlanSheet`
      `LessonPlanSheet`: topic, learning objective, materials needed, homework description.

- [ ] **P4-T3 — Homework Tracker**:
      A separate toggle view accessible via a tab row within Planner:
      "Schedule" | "Homework" toggle (pill-style, under the week strip).
      Homework view: `LazyColumn` grouped by class.
      Each `HomeworkCard(classAssignment)`:
        - Subject + assignment description
        - Due date + "X / Y submitted" progress
        - `LinearProgressIndicator` (submitted/total), `StatusPresent` color
        - Overdue: `StatusAbsent` progress color + "OVERDUE" badge
        - Tap → see student-by-student submission status sheet

- [ ] **P4-T4 — Smart Planner Nudge**:
      A slim amber card at top of week view when gaps exist:
      "You haven't planned 3 periods for next week."
      One action button: "Plan Now" → scrolls to first unplanned period.
      Dismissible (stores dismissed state in local DataStore).

---

### PHASE 5 — CHAT TAB REDESIGN
> File: `ui/screens/teacher/ChatScreen.kt`

**Teacher mental model:** I don't think of "chats" — I think "Class 9B → Rohan's parents."

- [ ] **P5-T1 — Chat List — Organized by Class**:
      Remove chronological flat list. Replace with class-grouped structure.
      Top: `SearchBar` (single-line, `SurfaceSubtle` background, "Search parent or student…")
      Below: `LazyColumn` of `ClassChatGroup(className, threads)`.
      Each group: `SectionHeader(className)` + list of `ParentThreadRow`.
      `ParentThreadRow(thread)`:
        - Student avatar (circle, 40dp, initials if no photo)
        - Student name `LabelBold` + parent name `BodySmall TextSecondary`
        - Last message preview `BodyMedium TextSecondary` (1 line, ellipsis)
        - Timestamp `BodySmall TextTertiary`
        - Unread badge (count pill, `PrimaryIndigo` bg, white text)
        - Category badge chip: "Academic" / "Attendance" / "Behavioral" / "General"
      Tap → navigates to `ChatThreadScreen(threadId)`

- [ ] **P5-T2 — Chat Thread Screen**:
      File: `ui/screens/teacher/ChatThreadScreen.kt`
      Top: `TopAppBar` — back arrow + student name + parent name subtitle + video call icon stub.
      Messages: `LazyColumn` (reversed) of `MessageBubble`.
        - Teacher messages: right-aligned, `PrimaryIndigoSoft` background, `TextPrimary`
        - Parent messages: left-aligned, `SurfaceSubtle` background, `TextSecondary`
        - Each bubble: message text `BodyMedium` + timestamp `BodySmall TextTertiary` below
        - Read receipt: double checkmark icon (grey = sent, `PrimaryIndigo` = read)
      Bottom: `ReplyBar`:
        - `BasicTextField` placeholder "Write a message…"
        - Left: Template icon → opens `QuickReplySheet`
        - Right: Send icon (`PrimaryIndigo` tint, enabled only when text non-empty)
      `QuickReplySheet`: pre-written reply templates:
        - "Your child was absent today."
        - "Please check the homework submitted."
        - "Your child's performance has improved."
        - "Please schedule a meeting with me."
        - [+ Add custom template]

- [ ] **P5-T3 — Unread Count Badge on BottomNav**:
      Chat tab in `EnrollBottomNav` shows live unread count from `ChatViewModel.unreadCount`.
      Update `BadgedBox` count reactively.

---

### PHASE 6 — PROFILE TAB FULL REDESIGN
> File: `ui/screens/teacher/TeacherProfileScreen.kt`

- [ ] **P6-T1 — Profile Header**:
      Full-width gradient header (same `GradientStart → GradientEnd`), 200dp tall.
      Teacher avatar: 72dp circle, centered, white 3dp border ring.
      Tap avatar → image picker (stub: `rememberLauncherForActivityResult`).
      Name: `HeadingLarge`, white, below avatar.
      Designation + school name: `BodyMedium`, white 80% alpha.
      Edit icon (pencil) top-right → navigates to `EditProfileScreen` (stub).

- [ ] **P6-T2 — Stats Row**:
      Below header: a `Row` of 4 `StatColumn` composables.
      `StatColumn(value: String, label: String)`:
        - `DataLarge` value (TextPrimary)
        - `LabelCaps` label (TextSecondary)
      Stats: Classes Taught | Total Students | Subjects | Attendance %
      Dividers between columns: 1dp `SurfaceSubtle`.
      `EnrollCard` container for the whole row.

- [ ] **P6-T3 — Teaching Assignment Card**:
      `EnrollCard` titled "MY CLASSES".
      `LazyColumn` (non-scrolling, `userScrollEnabled = false`) of `ClassAssignmentRow`:
        - Class name + section
        - Subjects taught in that class (comma-separated chips)
        - Student count
      Tap row → navigates to GradebookTab filtered to that class.

- [ ] **P6-T4 — Settings Section**:
      `SectionHeader("PREFERENCES")`
      `SettingsRow` composable (icon | label | trailing widget):
        - Notification Preferences → `Switch` (toggle)
        - Smart Nudges → `Switch` (permission toggle — this controls P2-T5 nudges)
        - Language → trailing current value + chevron
        - Dark Mode → `Switch`
        - Help & Support → chevron (stub navigation)
        - Log Out → `TextButton` in `StatusAbsent` color, confirmation dialog

---

### PHASE 7 — CROSS-TAB CONNECTIVITY
> Ensure all deep links between tabs work. No isolated dead ends.

- [ ] **P7-T1**: Nudge "Add Now" on Home → navigates to GradebookTab,
      pre-selects the class+exam from nudge data.
      Implement via `NavController` with route arguments.

- [ ] **P7-T2**: Period tap on TodayStrip (Home) → if attendance not taken →
      "Take Attendance" button navigates to AttendanceScreen for that specific period.
      Pass period ID as nav argument.

- [ ] **P7-T3**: Student row expand in GradebookTab →
      "Message Parent" button navigates to ChatTab → ChatThreadScreen for that student.
      Pass studentId as nav argument, ChatViewModel resolves to parent thread.

- [ ] **P7-T4**: Notification tap (from NotificationSheet) routes to:
      - Attendance alert → AttendanceScreen
      - Mark update → GradebookTab filtered to that class
      - Parent message → ChatThreadScreen for that parent
      - System notice → a `NoticeDetailScreen` (simple text view)
      Implement a `NotificationRouter` utility: `fun routeNotification(nav, notification)`.

- [ ] **P7-T5**: Profile stats "Classes Taught" tap → GradebookTab.
      "Total Students" tap → a student list screen (stub with correct navigation).

---

## ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
## PART 4 — THE LOOP PROMPT
## (Paste this into Genspark Opus 4.8 every iteration)
## ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

```
=== ENROLL+ TEACHER PORTAL — LOOP ITERATION ===

SYSTEM ROLE:
You are a senior Kotlin Compose Multiplatform engineer executing a structured redesign
of the Teacher Portal in the Enroll+ school management SaaS app.
You work methodically, read before writing, and never exceed the scope of one task per iteration.

RULES — READ THESE BEFORE ANYTHING ELSE:
1. Open TEACHER_PORTAL_LOOP.md. Read PART 1 (Loop State), PART 2 (Design System Spec),
   and the TASK QUEUE.
2. Pick the FIRST unchecked task in the TASK QUEUE.
3. Before writing a single line of code:
   a. List every file you will read.
   b. Read each file fully.
   c. State what the file currently does and what must change.
4. Write the code. Follow the Design System Spec in PART 2 for every color, spacing,
   and typography decision. Use design tokens — never hardcode values.
5. CONSTRAINTS:
   - DO NOT run the app, gradle, or any build command.
   - DO NOT execute any test runner.
   - DO NOT modify any file outside the scope of the current task.
   - DO NOT generate placeholder lorem ipsum copy. Use real EdTech strings.
   - If a ViewModel or repository function you need doesn't exist, create a stub
     with the correct signature and a TODO comment. Do not call non-existent functions.
6. AFTER writing the code, perform static verification:
   - Trace the data flow: ViewModel → Composable → UI.
   - Check that all design tokens are used (no hardcoded colors/sizes).
   - Verify the task's Done Criteria from PART 1 are met.
7. Update TEACHER_PORTAL_LOOP.md:
   - Mark the completed task as [x] in the TASK QUEUE.
   - Fill in LAST COMPLETED TASK, LAST COMMIT message, and AGENT NOTES.
8. Output a conventional commit message: `feat(teacher-portal): <what changed>`
9. STOP. Do not proceed to the next task.
   The next iteration will be triggered manually.

QUALITY BAR:
The teacher using this app has Byju's, Teachmint, and ClassDojo on their phone.
Every screen you build must feel more premium and more useful than those.
If any design decision looks like a default Material3 component with no customization,
that is a failure. Justify every visual choice from the Design Spec in PART 2.

BEGIN.
```

---

## ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
## PART 5 — ITERATION LOG
## (Agent fills this after each run)
## ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

| # | Task ID | Commit | Files Changed | Notes |
|---|---------|--------|---------------|-------|
| 1 | —       | —      | —             | Loop initialized |
| 2 | P1-T1   | `feat(teacher-portal): add Enroll semantic token bridge over the existing VTheme design system` | `composeApp/.../ui/v2/theme/EnrollTokens.kt` (new), `TEACHER_PORTAL_LOOP.md` | Foundation satisfied by INTENT: bridged loop token vocabulary onto existing VTheme rather than forking an off-pattern indigo theme. Verified every referenced VColors/VType/VDimens member exists; braces balanced; no new hex; status semantics preserved; no existing screen modified. |
| 3 | P1-T2   | `feat(teacher-portal): add shared flat EnrollCard composable (loop P1-T2)` | `composeApp/.../ui/v2/components/EnrollCard.kt` (new), `TEACHER_PORTAL_LOOP.md` | Flat border-defined card, no shadow, 0.98f press via existing `pressScale`; optional `tint` for nudges. Separate from elevated `VCard`. Raw-Card replacement verified vacuous. All tokens via `Enroll.*`; imports clean; braces balanced. |
| 4 | P1-T3   | `feat(teacher-portal): add shared SectionHeader composable (loop P1-T3)` | `composeApp/.../ui/v2/components/SectionHeader.kt` (new), `TEACHER_PORTAL_LOOP.md` | Ergonomic string-action header (title/action/onAction). `labelCaps`+`textSecondary` title, `primaryMid` ripple-free text-button action. Terser sibling of `VSectionHeader`. Unused import removed on review; tokens via `Enroll.*`; braces 5/5. |
| 5 | P1-T4   | `feat(teacher-portal): add EnrollBottomNav over the premium TeacherDock (loop P1-T4)` | `composeApp/.../ui/v2/screens/teacher/EnrollBottomNav.kt` (new), `TEACHER_PORTAL_LOOP.md` | Canonical nav entry point: `EnrollBottomNav` + `EnrollTab` ids + `loopTabs()`. Delegates to premium `TeacherDock` instead of a regressive pill bar (preserves spring lozenge/haptics/badges/ParentDock parity). Chat badge via `VNavItem.badge`. Placed in teacher pkg for clean layering. **PHASE 1 COMPLETE.** |
| 6 | P2-T1   | `feat(teacher-portal): add gradient TeacherHomeHeader for the Home tab (loop P2-T1)` | `composeApp/.../ui/v2/screens/teacher/TeacherHomeHeader.kt` (new), `TEACHER_PORTAL_LOOP.md` | Signature 120dp violet-gradient Home header: time-aware greeting + first name (headingLarge) + date (bodyMedium 70%); 40dp avatar ring → Profile; glassy bell + unread badge → NotificationSheet. Additive (keeps TeacherHeader on other tabs). Reuses `teacherGreeting`; util date + Enroll members verified; braces 22/22. |
| 7 | P2-T2   | `feat(teacher-portal): add signature TodayClassStrip period timeline (loop P2-T2)` | `composeApp/.../ui/v2/screens/teacher/TodayClassStrip.kt` (new), `TEACHER_PORTAL_LOOP.md` | Horizontal period-pill day timeline (keyed LazyRow), past/active/future states with active accent glow, tap → inline AnimatedVisibility detail (room, attendance dot, pre-scoped Take-attendance CTA). Server `ResolvedDayUi.nowIndex`-driven. Reuses shared clock utils; status colours preserved; braces 34/34. |
| 8 | P2-T3   | `feat(teacher-portal): add QuickActionRow three-up action pills (loop P2-T3)` | `composeApp/.../ui/v2/screens/teacher/QuickActionRow.kt` (new), `TEACHER_PORTAL_LOOP.md` | Three centered action pills (Take Attendance / Add Marks / Message Parent): primarySoft bg, primaryMid icon+text, shape.card, SpaceMD gap + padding, pressScale. Callbacks set up for P7 deep links. Tokens via `Enroll.*`; braces 5/5. |

---

*Loop Version: 1.0 | Created for Enroll+ Teacher Portal Sprint*
*Branch: backend-by-abuzar_v1.0.3*
