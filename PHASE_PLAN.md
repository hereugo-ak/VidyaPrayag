# VidyaSetu UI Rebuild — Phase Plan & Batch Ledger

> Working branch: **`backend-by-abuzar`** (source of truth).
> Stack: **Compose Multiplatform 1.10.3**, Kotlin 2.2.10, Material3 1.10.0-alpha05, Koin 4, Coil3 3.4.0.
> Strategy: author new UI in a **parallel `ui/v2/` package**, build-verify, then swap `App.kt` and delete old `ui/`.
> Design source of truth: `UI screens/` (React/shadcn prototype) — **translated to Compose**, never shipped.

---

## Guiding rules (unchanged across all phases)

1. **`shared/` is untouchable.** All APIs, repos, ViewModels and Koin DI stay exactly as-is. Phase 2 only *adds* a teacher vertical; it never rewrites existing wiring.
2. **`commonMain` must compile on every target** (android, iosArm64, iosSimulatorArm64, jvm, js, wasmJs). No platform-only APIs in `ui/v2/`.
3. **Tokens, never hardcodes.** Every color/type/dimension flows from `VTheme` CompositionLocals (`VTheme.colors`, `VTheme.type`, `VTheme.dimens`). No raw hex/sp/dp literals inside screens.
4. **Old `ui/` stays live** until `ui/v2/` reaches feature parity and `App.kt` is swapped (Phase 3+). The app keeps building the entire time.

---

## ⚠️ Build-verification constraint (important context)

The Genspark sandbox has **~985 MB total RAM (~330 MB free)**. The project's `gradle.properties` requests `org.gradle.jvmargs=-Xmx4096M` + `kotlin.daemon.jvmargs=-Xmx3072M` (≈7 GB). A full Compose/Kotlin-MPP Gradle build **OOMs / freezes the sandbox** even at the *configuration* phase — verified with a memory watchdog that watched available RAM fall to the 80 MB safety floor before Gradle finished configuring, on every capped-heap attempt (`-Xmx420m`, `--no-daemon`, `--offline`, `--max-workers=1`).

**Consequence:** Phase 1 was verified by **rigorous static type-review against the project's *proven* API surface** rather than a Gradle compile. This is a disciplined, defensible substitute:

- Every Compose import used in `ui/v2/` was cross-checked against imports **already compiling in the existing `ui/`** of this same repo (e.g. `Canvas`, `drawArc`, `Stroke`, `AnimatedContent`, `togetherWith`, `rotate`, `BasicTextField`, `collectIs*AsState`, `Brush.verticalGradient`, `coil3.compose.AsyncImage`, `Text(style/textAlign/maxLines/overflow)`).
- New API surfaces with no in-repo precedent were verified against official AndroidX references: `Path.quadraticTo` (current, non-deprecated), `ImageVector.Builder(name, defaultWidth, defaultHeight, viewportWidth, viewportHeight, …)`, the `path(name, fill, stroke, strokeLineWidth, strokeLineCap, strokeLineJoin, pathBuilder)` DSL, `PathBuilder.{moveTo,lineTo,curveTo,close}`, `DrawScope.drawArc(color, …, topLeft, size, style)`, `DrawScope.drawRoundRect(color, topLeft, size, cornerRadius)`.
- Scope-bound modifiers (`Modifier.weight` in Row/Column scope, `Modifier.align` in Box scope) were audited per call-site.
- Unused-import sweep run across all files (false positives for `getValue`/`setValue` operator imports under `by`-delegates were retained; genuinely unused `Color` imports removed).

**A real Gradle compile is still owed.** It must be run on a host with adequate RAM (≥8 GB) before the Phase 3 entrypoint swap. Tracked below as an explicit open item.

---

## PHASE 1 — Design-system foundation ✅ (this batch)

**Goal:** Stand up the complete token system + every reusable `V*` primitive/chart/nav atom, so Phase 3 screens are pure composition with zero new styling.

### Delivered files (all under `composeApp/src/commonMain/kotlin/com/littlebridge/vidyaprayag/ui/v2/`)

**Theme tokens — `theme/`** (created in prior batch, verified this batch)
| File | Contents |
|---|---|
| `VColors.kt` | `VColors` immutable token set, `VPortalTone {Light,Warm,Night}`, `LightVColors`, `NightVColors`, `vColorsFor()`. All hex lifted verbatim from `theme.css`. |
| `VType.kt` | `VTypography` + `defaultVTypography()` — full scale (h1–h4, body, caption, label, data/dataSm/dataLg). SansSerif/Monospace fallback until fonts bundled. |
| `VDimens.kt` | `VDimens` spacing (base-4) + radii + `maxContentWidth=440`, `DefaultVDimens`, `shapeSm/Md/Lg/Xl/Pill` extensions. |
| `VTheme.kt` | `VTheme(...)` provider, `LocalVColors/LocalVType/LocalVDimens/LocalVPortalTone`, `VTheme` accessor object, `TextStyle.colored()`. |

**Components — `components/`** (created this batch)
| File | Components | Source in `primitives.tsx` / `charts.tsx` |
|---|---|---|
| `VCard.kt` | `VCard` (elevated surface, optional click, night-flat) | `VCard` |
| `VAtoms.kt` | `VDivider`, `VLabel`, `VStatusDot` | `VDivider`, `Label`, `VStatusDot` |
| `VBadge.kt` | `VBadge` (+`VBadgeTone`), `VTag` (selectable chip) | `VBadge`, `VTag` |
| `VAvatar.kt` | `VAvatar` (deterministic name-hash palette, Coil image fallback) | `VAvatar` |
| `VProgress.kt` | `VProgressBar`, `VProgressRing` (Canvas arc) | `VProgressBar`, `VProgressRing` |
| `VLogo.kt` | `VLogo` (bridge mark on Canvas + wordmark) (+`VLogoTone`) | `VLogo` |
| `VButton.kt` | `VButton` — 4 variants × 8 tones × soft/filled × 3 sizes × stateful idle→loading→success (+`VButtonVariant/Tone/Size`) | `VButton`, `tonePalette` |
| `VInput.kt` | `VInput` — BasicTextField w/ focus teal border + 4dp glow ring | `VInput` |
| `VIcons.kt` | `VIcons` object — lucide→Material core mappings + hand-authored ImageVectors (`Spinner`, `GraduationCap`, `Target`, `Wallet`, `Sparkles`) | icon set across design |
| `VNavigation.kt` | `VTopTabs`, `VBottomNav` (+`VNavItem`), `VBackHeader` | `VTopTabs`, `VBottomNav`, `VBackHeader` |
| `VStructure.kt` | `VScreenScaffold` (440dp phone frame), `VEmptyState`, `VComingSoon` | `PhoneFrame`, `VEmptyState`, `VComingSoon` |
| `VCharts.kt` | `VDonut`, `VSparkline`, `VBars`, `VLegendDot` (+`VChartDatum`) | `VDonut`, `VSparkline`, `VBars`, `VLegendDot` |

### Phase 1 status
- [x] All 4 theme files in place & reviewed.
- [x] All 12 component files authored (≈30 public composables + supporting enums/data classes).
- [x] Static type-review passed (imports proven, scopes audited, cross-refs resolve, unused imports trimmed).
- [ ] **OPEN: real Gradle compile** of `:composeApp` on an ≥8 GB host (blocked by sandbox RAM — see constraint above).

---

## PHASE 2 — Teacher vertical in `shared/` (🔄 in progress)

**Goal:** Add the entirely-missing teacher domain (gap **G1** in the master doc) to the data layer, mirroring the existing parent/admin verticals. **No UI yet.**

### Delivered so far (Batch 2a — committed)
All under `shared/src/commonMain/kotlin/com/littlebridge/vidyaprayag/feature/teacher/`:

| File | Layer | Contents |
|---|---|---|
| `domain/model/TeacherModels.kt` | domain | All teacher DTOs — home glance + today's periods/tasks, my-classes, attendance roster + submit, marks entry + submit, syllabus units + update, homework list + create, profile. `@Serializable`/`@SerialName` snake_case; read envelopes `{success,data}`; writes → `ApiResponse<Unit>`. |
| `data/remote/TeacherApi.kt` | data | Ktor client — 7 reads (`home`,`classes`,`attendance`,`marks`,`syllabus`,`homework`,`profile`) + 4 writes (`submitAttendance`,`submitMarks`,`updateSyllabus`,`createHomework`). Bearer auth, `safeApiCall`, routes `api/v1/teacher/*`. |
| `domain/repository/TeacherRepository.kt` | domain | Interface — 7 read + 4 write suspend fns. |
| `data/repository/TeacherRepositoryImpl.kt` | data | Delegates to `TeacherApi`. |
| `presentation/TeacherHomeViewModel.kt` | presentation | Home dashboard VM — state + StateFlow, `init{load()}`, `getUserToken().first()`, `when(NetworkResult)`, `toUi()` mappers. |
| `presentation/TeacherClassesViewModel.kt` | presentation | My-classes list VM. |
| `presentation/TeacherAttendanceViewModel.kt` | presentation | Roster load (classId+date) + local status edits + `submit()` → `submitAttendance`. Derived present/absent/late counts. |

### Remaining in Phase 2
- [ ] `presentation/TeacherMarksViewModel.kt` — `getMarks(classId,examId)`, local mark edits (clamped 0..maxMarks), `submit()` → `submitMarks`.
- [ ] `presentation/TeacherSyllabusViewModel.kt` — `getSyllabus(classId,subject)`, toggle unit → `updateSyllabus`.
- [ ] `presentation/TeacherHomeworkViewModel.kt` — `getHomework`, `createHomework`.
- [ ] `presentation/TeacherProfileViewModel.kt` — `getProfile`.
- [ ] Register `TeacherApi` + `TeacherRepository` + 7 VM factories in `di/Koin.kt` (additive only).
- [ ] Backend (Ktor) teacher routes — author/document the matching `server/.../feature/teacher` endpoints (master doc G1; the legacy plan wrongly assumed FastAPI — this is **Ktor**).
- [ ] `MainViewModel` already role-agnostic (`AuthState{role,token,isLoaded}`) — no change needed; the `TEACHER` branch is added in `App.kt` start-destination logic in Phase 3.

---

## PHASE 3 — Portal screens in `ui/v2/` + entrypoint swap

**Goal:** Compose the actual screens from Phase 1 primitives, per portal, then flip the app over.

- [ ] `ui/v2/auth/` — Landing + AuthBottomSheet (phone/OTP, multi-provider) using `VInput`/`VButton`.
- [ ] `ui/v2/parent/` — Parent dashboard, fees, attendance, marks, announcements, notifications.
- [ ] `ui/v2/teacher/` — Home / Update (Attendance·Marks·Syllabus·Homework sub-tabs) / MyClasses / Profile (from `Teacher.tsx`).
- [ ] `ui/v2/school/` (admin) — School dashboard + admin verticals.
- [ ] `ui/v2/discovery/` — Discovery schools.
- [ ] `ui/v2/navigation/NavGraphV2.kt` — new `Destination` set incl. TEACHER start destination.
- [ ] **Swap `App.kt`** to drive `ui/v2/` (`VTheme` + `NavGraphV2`); add the TEACHER role branch in start-destination logic.
- [ ] **Run the real Gradle build** (the deferred Phase 1 item) — must pass before deletion.
- [ ] **Delete old `ui/`** once parity confirmed and build is green.

## PHASE 4 — Polish

- [ ] Bundle Plus Jakarta Sans + DM Mono into `composeResources`; swap `defaultVTypography()` font families (single construction-site change, no call-site churn).
- [ ] Night-tone QA pass across all screens.
- [ ] Per-target smoke (android + jvm desktop + wasmJs).

---

## Quick reference — what changed in THIS batch
- **Added:** 12 files under `ui/v2/components/` (all `V*` primitives, nav, structure, charts, icons).
- **Verified:** 4 theme files (`ui/v2/theme/`).
- **Added:** this `PHASE_PLAN.md`.
- **Untouched:** `shared/`, old `ui/`, `App.kt`, `NavGraph.kt`, `build.gradle.kts`, `gradle.properties`.
