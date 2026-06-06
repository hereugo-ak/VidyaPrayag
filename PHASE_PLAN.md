# VidyaSetu Rebuild — Phase Plan & Reality Audit

> Working branch: **`backend-by-abuzar`** (source of truth).
> Stack: **Compose Multiplatform**, Kotlin 2.2.10, Material3, Koin 4, Coil3 (client) · **Ktor 3.4.3 + Exposed 0.50 + HikariCP** (server, JVM) · Postgres/Supabase (prod) / SQLite (dev).
> Master plan: `UI screens/VIDYASETU_MASTER_REBUILD_DOC.md`. **Where plan and code disagree, the code wins.**
> Strategy (per master doc): keep `shared/` untouched, author new UI in a **parallel `ui/v2/`** package, build-verify, **then** swap `App.kt` and delete old `ui/`; add the teacher vertical (client + backend).

---

## 0. How to read this file

This document was **rewritten on 2026-06-06 from a fresh, file-by-file audit** of the actual code on
`backend-by-abuzar`, cross-checked against every commit message and the open PR. The goal of the
rewrite was to separate **what was claimed** in commits/PR from **what is actually on disk and
actually wired**. Sections are ordered: audit verdict → claims-vs-reality ledger → per-pillar status
→ remaining work. ✅ = real & wired · ⚠️ = built but not live / partial · ❌ = absent.

---

## 1. Executive verdict (audit, 2026-06-06)

The repo is **substantially further along than the master doc's Step-3/Step-5 status tables suggest**
(those tables were authored from old `main`, before most of this branch's work landed). Three pillars:

| Pillar | Reality | One-line truth |
|---|---|---|
| **A. `shared/` data layer** | ✅ intact + extended | Untouched per plan, **plus** a complete greenfield `feature/teacher` vertical (API + repo + 7 VMs) wired into Koin. |
| **B. `ui/v2/` new UI** | ⚠️ **built but DORMANT** | 40 Kotlin files (theme + 12 `V*` component files + 30 screens incl. 8 teacher + portal shells + `NavGraphV2`). **`App.kt` still launches the OLD `NavGraph`. `NavGraphV2` is currently dead code.** Step 8 (entrypoint swap + old-UI deletion) has **not** happened. |
| **C. `server/` backend** | ✅ teacher vertical now closed | The teacher backend (gap **G1**) — the one genuinely-missing piece — was built this session: schema (6 tables) + 11 routes, JWT-scoped. DI loop client↔server is now complete. |

**Net:** the new design is **implemented in parallel** and the backend gap is **closed**, but the app a
user launches **still renders the old UI** because the entrypoint was never swapped. That swap +
old-UI deletion + a real ≥8 GB Gradle build is the headline remaining work.

---

## 2. Claims-vs-reality ledger (commit/PR audit)

| Commit(s) | What it claimed | What is actually true | Verdict |
|---|---|---|---|
| `a5713a5` Phase 1 | design-system foundation | `ui/v2/theme/{VColors,VType,VDimens,VTheme}.kt` + 12 `V*` component files exist | ✅ accurate |
| `3bea961`→`d420ab5` Phase 2 | teacher data layer + 7 VMs + Koin DI | `shared/feature/teacher/{data/remote,data/repository,domain,presentation}` all present; Koin registers `TeacherApi` (single), `TeacherRepository` (single), **7 VM factories** | ✅ accurate |
| `2a4bb7f`→`784570a` Phase 3 | auth/teacher/parent/admin/discovery screens; "parent portal complete", "admin portal complete", "portal-aware NavGraphV2" | All 30 screen files + portal shells + `NavGraphV2.kt` exist **and compile (commonMain)**. **BUT** "complete" means *authored in `ui/v2`* — **not wired into the running app**. `App.kt` imports/uses old `NavGraph`; `NavGraphV2` is referenced only inside its own file (**dead code**); start-destination has **no TEACHER branch**. | ⚠️ **screens real, NOT live** — the commits never claimed the swap, but a casual reader could over-read "complete" |
| `16e09b1` (this session) | re-audit + Phase 5 plan | `PHASE_PLAN.md` re-audit section + Phase 5 plan | ✅ accurate |
| `6553df2` Phase 5A | 6 teacher tables + registration + migration SQL | `Tables.kt` lines 629-733 (6 `UUIDTable`s), `DatabaseFactory.kt` `allTables` +6, `docs/backend/sql/02_teacher_schema.sql`. **Note:** the root `supabase_schema` (1043 lines) was **NOT** updated — drift vs master-doc Step 7.1 ("update Tables.kt *and* supabase_schema"). | ✅ code real · ⚠️ `supabase_schema` drift owed |
| `59470ec` Phase 5B-5D | 11 teacher routes, scoped | `core/TeacherAccess.kt` + `feature/teacher/{TeacherRouting,TeacherRoutingTasks}.kt`; mounted in `Application.kt`. 11 routes confirmed. Type-checked clean (embedded kotlinc). | ✅ accurate |
| `cc5c151` (this session) | fix Android build break | `TeacherApi.kt` KDoc had `api/v1/teacher/*` — the `/*` opened a **nested** Kotlin block comment that ate the closing `*/`, leaving the comment unclosed → `:shared:kspDevDebugKotlinAndroid` FAILED in Android Studio. Fixed to `teacher/…`. | ✅ accurate — real reported build break, now fixed |

---

## 3. Pillar A — `shared/` data layer ✅

Per the master doc's locked decision, `shared/` is kept. Audit confirms it is intact and **extended**
with the teacher vertical:

```
shared/.../feature/teacher/
  data/remote/TeacherApi.kt              11-route Ktor client (the backend contract)
  data/repository/TeacherRepositoryImpl.kt
  domain/model/TeacherModels.kt          all DTOs (@SerialName snake_case)
  domain/repository/TeacherRepository.kt
  presentation/ TeacherHome/Classes/Attendance/Marks/Syllabus/Homework/ProfileViewModel.kt (7)
```
- **DI:** `di/Koin.kt` registers `TeacherApi`, `TeacherRepository`, and all 7 VMs (factory). Verified.
- Existing `auth / content / parent / admin / schools` features unchanged.

---

## 4. Pillar B — `ui/v2/` new UI ⚠️ built, **not yet live**

40 Kotlin files under `composeApp/.../ui/v2/`:
- **theme/** (4): `VColors`, `VType`, `VDimens`, `VTheme` — teal/navy/lavender + night tokens.
- **components/** (12): `VAtoms, VAvatar, VBadge, VButton, VCard, VCharts, VIcons, VInput, VLogo, VNavigation, VProgress, VStructure`.
- **screens/** (23): `auth/{Welcome,Login}`, `discovery/Discovery`, `parent/{Home,Fees,Academics,Activity,Portal}`, `school/{Home,People,Records,Comms,Settings,Portal}`, `teacher/{Home,Classes,Attendance,Marks,Syllabus,Homework,Profile,Portal}`, `Shared`.
- **navigation/** (1): `NavGraphV2` (portal-aware: Admin 5-tab / Teacher 4-tab / Parent 4-tab / Discovery).

**The honest gap (Step 8, NOT done):**
- `App.kt` (line 27, 105) imports and renders the **old** `navigation.NavGraph`.
- `NavGraphV2` is **dead code** — no production reference outside its own file.
- `App.kt` start-destination: `ADMIN→SchoolDashboard`, `PARENT→ParentDashboard`, else `Landing` — **no `TEACHER` branch**, so a teacher login cannot reach the (already-built) teacher portal.
- The old UI is still present and live: `ui/screens/` (35 files) + `ui/components/` (15 files) + `ui/theme/` + `ui/auth/`.

So: launching the app today shows the **old** UI. The new UI is complete-but-unreachable until the swap.

---

## 5. Pillar C — `server/` backend ✅ (teacher vertical = gap G1, closed this session)

### 5A — schema (`6553df2`)
- `db/Tables.kt` +6 Exposed tables: `assessments`, `assessment_marks`, `syllabus_units`, `homework`, `homework_submissions`, `teacher_periods`. Marks **normalized** (numeric `max_marks` + `exam_id`, master-doc G4) rather than overloading the string-scored admin `exam_results`; attendance reuses `attendance_records`.
- `db/DatabaseFactory.kt`: all 6 registered in `allTables`.
- `docs/backend/sql/02_teacher_schema.sql`: idempotent Supabase DDL (FKs, indexes, unique constraints).
- ⚠️ **Owed:** mirror these 6 into the root `supabase_schema` (Step 7.1 drift).

### 5B-5D — endpoints (`59470ec`) — all 11 routes the client speaks
- `core/TeacherAccess.kt` — `requireTeacherContext()` (401/403/404) + `requireOwnedAssignment()`. **G1 enforced:** every class-scoped call resolves the client `class_id` to a `teacher_subject_assignments` row and asserts it is in the caller's school **and** assigned to the caller (`teacher_id`/`teacher_name`), else **403**. `school_admin`/`admin` may stand in. Role read from DB, not the JWT.
- `feature/teacher/TeacherRouting.kt` — `GET /home /classes /profile`.
- `feature/teacher/TeacherRoutingTasks.kt` — `GET/POST /attendance`, `GET/POST /marks`, `GET/PATCH /syllabus`, `GET/POST /homework`.
- `Application.kt` — imports + mounts `teacherRouting()` after `mediaRouting()`.
- Server DTOs mirror `shared/.../TeacherModels.kt` `@SerialName` field-for-field; reads → `{success,data}`, writes → `{success,message}`. **No fabricated data** — empty rosters/units returned honestly.

### Other backend (already present from this branch, beyond the doc's stale tables)
Routing files now on disk that the master-doc Step-3 table marked ❌/⚠️ but **do exist**:
`school/{Messages,Ptm,Results,SchoolAnalytics,SchoolDashboard,LeaveRequests,TeacherAssignment}Routing.kt`,
`media/MediaRouting.kt`, `parent/{ParentDashboard,ParentFees,ParentOnboarding,TrackProgress}Routing.kt`,
`user/ParentMessagesRouting.kt`, `config/VersionRouting.kt`, `content/SupportRouting.kt`. (Their
depth vs. the design still warrants a per-route audit, but they are not "missing".)

---

## 6. Build & verification state

| Item | State |
|---|---|
| Android Studio `:composeApp:assembleDevDebug` | **WAS failing** at `:shared:kspDevDebugKotlinAndroid` — `TeacherApi.kt:137 Unclosed comment`. **Fixed** in `cc5c151` (nested-block-comment in KDoc). |
| Server teacher vertical compile | Type-checked clean via embedded `kotlinc 2.2.10` (JDK 21, `-Xmx700m`) against cached jars — **0 errors in any teacher/core file**. Full Gradle `:server` OOMs the 985 MB sandbox at plugin-classpath resolution. |
| Full `:composeApp` Gradle build (all targets) | **Not run in-sandbox** (OOM). Must be run on the dev machine / ≥8 GB host. |
| Boot + curl smoke of 11 teacher routes | **Not yet run.** |

> ⚠️ Sandbox constraint: do **not** launch a full Gradle build here — it OOMs the 985 MB sandbox.
> Use the dev machine (Android Studio) for the real `:composeApp` + `:server` build.

---

## 7. Remaining work (priority order)

1. **Verify the Android build is green** on the dev machine after `cc5c151` (the `TeacherApi.kt` fix). _(unblocks everything)_
2. **`supabase_schema` parity** — add the 6 teacher tables to the root `supabase_schema` so prod DDL matches `Tables.kt` (Step 7.1).
3. **Teacher backend smoke** — boot on SQLite, seed a demo teacher + `teacher_subject_assignments`, curl all 11 routes (assert 200/401/403 scope behaviour).
4. **Step 8 — make `ui/v2` live (the big one):**
   - Add a `TEACHER` branch to `App.kt` start-destination + point `App.kt` at `NavGraphV2` (replacing old `NavGraph`).
   - Build-verify all targets green.
   - Delete old `ui/{screens,components,theme,auth}` (35+15+… files) and stale `navigation/NavGraph.kt` destinations; rename `ui/v2` → `ui`.
5. **Flip master-doc §3 / §5.4 teacher rows ❌→✅** and refresh the Step-3/Step-5 tables to match the current backend reality (many ⚠️/❌ rows are now ✅).
6. **Per-route depth audit** of the already-present school/parent routes vs. the design (are they full or stubs?).

---

## 8. Status board

- [x] Phase 1 — design system (`a5713a5`)
- [x] Phase 2 — `shared/feature/teacher` data + 7 VMs + Koin (`3bea961`…`d420ab5`)
- [x] Phase 3 — `ui/v2` screens + portal shells + `NavGraphV2` authored (`2a4bb7f`…`784570a`)
- [x] **Step 8 / Phase 3E-2 — `App.kt` → `NavGraphV2`, old `ui/` DELETED in full** (`c8ae0db`) — UI cutover live
- [x] **`VInput.kt` missing `shapeMd` import fixed** (Android `compileDevDebugKotlinAndroid` error)
- [x] Phase 5A — teacher schema (`6553df2`) — ⚠️ `supabase_schema` parity owed
- [x] Phase 5B-5D — teacher backend (11 routes, scoped) (`59470ec`)
- [x] Android build break fixed (`cc5c151`)
- [ ] Verify Android build green on dev machine (in progress — fixing compile errors as they surface)
- [ ] `supabase_schema` parity + teacher backend smoke
- [ ] Rename `ui/v2`→`ui` (cosmetic, after build is green)
- [ ] Flip master-doc teacher statuses; refresh stale §3/§5 tables
- [ ] Full `:composeApp` + `:server` Gradle build green (≥8 GB host)

---

## Phase 3 batch ledger (detail)

**Goal:** Compose the actual screens from Phase 1 primitives, per portal, then flip the app over.
Built in small batches (~3 files); each batch → commit + push + PR update + this ledger refreshed.

### Batch 3A — auth + shared scaffolding ✅
| File | Contents |
|---|---|
| `ui/v2/screens/Shared.kt` | `collectAsStateV2()` (terse `StateFlow` collector), `VSectionHeader`, `VPortalHeader` (avatar + name + subtitle) — shared screen vocabulary. |
| `ui/v2/screens/auth/WelcomeScreenV2.kt` | Splash/welcome from `Auth.tsx → Splash`: teal hero (logo + wordmark + tagline) + lifted lavender CTA sheet (Get started / I already have an account). Pure `V*` composition. |
| `ui/v2/screens/auth/LoginScreenV2.kt` | Portal selector + identifier/password/OTP, **re-bound 1:1 to existing `AuthViewModel`**. Teacher portal shown as disabled "COMING SOON" chip until backend teacher-auth ships (G1) — never wires a missing route. |

- [x] `ui/v2/auth/` — Welcome + Login (portal selector, phone/email→OTP/password) bound to `AuthViewModel`.

### Batch 3B-1 — teacher Home / Classes / Profile ✅
| File | VM bound | Contents |
|---|---|---|
| `ui/v2/screens/teacher/TeacherHomeScreenV2.kt` | `TeacherHomeViewModel` | Today's glance (4 stat cards) + period timeline + task list; empty states via `VEmptyState`. |
| `ui/v2/screens/teacher/TeacherClassesScreenV2.kt` | `TeacherClassesViewModel` | Assigned-class cards: subject/headcount, syllabus + attendance `VProgressBar`, class-teacher badge, tap → `onOpenClass(id)`. |
| `ui/v2/screens/teacher/TeacherProfileScreenV2.kt` | `TeacherProfileViewModel` | Identity card, subject/class chips (`FlowRow`), contact rows, sign-out. |

- [x] `ui/v2/teacher/` (part 1) — Home / MyClasses / Profile bound to teacher VMs.

### Batch 3B-2 — teacher Update sub-tabs (Attendance / Marks / Syllabus) ✅
| File | VM bound | Contents |
|---|---|---|
| `ui/v2/screens/teacher/TeacherAttendanceScreenV2.kt` | `TeacherAttendanceViewModel` | `load(classId,date)`; per-student P/A/L `VTag` toggles, "Mark all present", live present/absent/late tallies, immutable submit. |
| `ui/v2/screens/teacher/TeacherMarksScreenV2.kt` | `TeacherMarksViewModel` | `load(classId,examId)`; per-student `VInput` score (VM clamps 0..maxMarks), entered/total counter, submit (enabled when ≥1 entered). |
| `ui/v2/screens/teacher/TeacherSyllabusScreenV2.kt` | `TeacherSyllabusViewModel` | `load(classId,subject)`; `VProgressRing` coverage + unit list with optimistic `toggleUnit` (disabled while `updatingUnitId`). |

- [x] `ui/v2/teacher/` (part 2a) — Attendance · Marks · Syllabus.

### Batch 3B-3 — teacher Homework + portal shell ✅ (Teacher portal complete)
| File | VM bound | Contents |
|---|---|---|
| `ui/v2/screens/teacher/TeacherHomeworkScreenV2.kt` | `TeacherHomeworkViewModel` | "Assign new" composer (class/title/desc/due) → `create()`; one-shot `createSuccess` clears the form; assigned-list cards with submission-ratio `VProgressBar`. |
| `ui/v2/screens/teacher/TeacherPortalV2.kt` | — (shell) | 4-tab `VBottomNav` (Home·Update·Classes·Profile) + inner `VTopTabs` for the Update sub-tabs; each leaf is a `*ScreenV2` that `koinViewModel()`s its own VM. |

- [x] `ui/v2/teacher/` — **complete** (Home / Classes / Profile / Update[Attendance·Marks·Syllabus·Homework] + portal shell). 8 files, all bound to the 7 teacher VMs.

### Batch 3C-1 — parent Home / Fees / Academics ✅
| File | VM bound | Contents |
|---|---|---|
| `ui/v2/screens/parent/ParentHomeScreenV2.kt` | `ParentDashboardViewModel` | Child hero (`userDetails` → name/photo), today's-glance status strip, quick actions, shortlisted-schools list (`schools`+`shortlist`, Discovery cross-link). |
| `ui/v2/screens/parent/ParentFeesScreenV2.kt` | `FeeViewModel` | Outstanding/collected summary + collection `VProgressBar`, overdue counter, fee-announcements feed; all monetary strings pre-formatted by the VM. |
| `ui/v2/screens/parent/ParentAcademicsScreenV2.kt` | `TrackProgressViewModel` | Journey hero (`VProgressRing` + level), achievement badges (`FlowRow`), per-competency `VProgressBar`s, emotional-intelligence meters. |

- [x] `ui/v2/parent/` (part 1) — Home / Fees / Academics bound to parent VMs.

### Batch 3C-2 — parent Activity + portal shell ✅ (Parent portal complete)
| File | VM bound | Contents |
|---|---|---|
| `ui/v2/screens/parent/ParentActivityScreenV2.kt` | `ParentAnnouncementViewModel` | School-announcements feed (featured-first), category badges, WhatsApp-sync `Switch` (`toggleWhatsAppSync`). Messaging (G7) intentionally not wired. |
| `ui/v2/screens/parent/ParentPortalV2.kt` | — (shell) | 4-tab `VBottomNav` (Home·Academics·Fees·Activity); each leaf `koinViewModel()`s its own VM; `tone = Light` (lavender) via host `VTheme`. |

- [x] `ui/v2/parent/` — **complete** (Home / Academics / Fees / Activity + portal shell). 5 files, bound to `ParentDashboard`/`Fee`/`TrackProgress`/`ParentAnnouncement` VMs.

### Batch 3D-1 — admin Home / Comms + Discovery ✅
| File | VM bound | Contents |
|---|---|---|
| `ui/v2/screens/school/SchoolHomeScreenV2.kt` | `SchoolDashboardViewModel` + `AnalyticsDashboardViewModel` | Setup-progress `VProgressBar` + onboarding step list (status dot/badge), analytics cards grid, performance `VSparkline`. |
| `ui/v2/screens/school/SchoolCommsScreenV2.kt` | `SchoolAnnouncementsViewModel` | Announcement list + category filter chips (`setCategoryFilter`); messaging/PTM (G7/G8) left for `VComingSoon`. |
| `ui/v2/screens/discovery/DiscoveryScreenV2.kt` | `ParentDashboardViewModel` (wraps `GetSchoolsUseCase`) | School marketplace: search + board filter, school cards (verified/SRI badges), shortlist toggle (max-3 enforced in VM), `onOpenSchool` hook. |

- [x] `ui/v2/school/` (admin) part 1 — Home / Comms.
- [x] `ui/v2/discovery/` — Discovery school list (SRI/reviews/compare = G11 COMING SOON).

### Batch 3D-2 — admin People / Records ✅
| File | VM bound | Contents |
|---|---|---|
| `ui/v2/screens/school/SchoolPeopleScreenV2.kt` | `StudentAnalyticsViewModel` | Retention-risk counts (Critical/Medium/Low cards), at-risk student list (`VAvatar` + risk badge), subject-engagement `VProgressBar`s. Teacher roster = G1 `VComingSoon`. |
| `ui/v2/screens/school/SchoolRecordsScreenV2.kt` | `SyllabusCoverageViewModel` | Overall coverage % + bar, per-department progress, lagging alerts (`VStatusDot` + delay badge), academic-milestone rows. Attendance/marks (G3/G4) not fabricated. |

- [x] `ui/v2/school/` (admin) part 2a — People / Records.

### Batch 3D-3 — admin Settings + 5-tab portal shell ✅ (Admin portal complete)
| File | VM bound | Contents |
|---|---|---|
| `ui/v2/screens/school/SchoolSettingsScreenV2.kt` | `InstitutionalProfileViewModel` | Identity card, public-listing `Switch` (`togglePublic`), profile-completion + storage `VProgressBar`s, sign-out (`VButton` Destructive). |
| `ui/v2/screens/school/SchoolPortalV2.kt` | — (shell) | 5-tab `VBottomNav` (Home·People·Records·Comms·Settings); each leaf `koinViewModel()`s its own VM; `tone = Warm` via host `VTheme`. |

- [x] `ui/v2/school/` (admin) — **complete** (Home / People / Records / Comms / Settings + 5-tab shell). 6 files, bound to `SchoolDashboard`/`Analytics`/`StudentAnalytics`/`SyllabusCoverage`/`SchoolAnnouncements`/`InstitutionalProfile` VMs.
- [x] `ui/v2/discovery/` — Discovery school list (SRI/reviews/compare = G11 COMING SOON).

### Phase 3E — NavGraphV2 + entrypoint swap (in progress)

#### Batch 3E-1 — portal-aware NavGraphV2 ✅
| File | Contents |
|---|---|
| `ui/v2/navigation/NavGraphV2.kt` | Role-driven root (Compose translation of `App.tsx`'s screen graph). Picks portal by `authState.role` and applies the right `VPortalTone` — PARENT/Discovery = `Light`, TEACHER/ADMIN = `Warm`. Unauth flow = Welcome → Login → Discovery (browse-first) via `AnimatedContent`. Each portal owns its own tabbed nav (`*PortalV2`); on auth success the host `MainViewModel.authState` flips and the graph recomposes into the role portal. |

- [x] `ui/v2/navigation/NavGraphV2.kt` — portal-aware root (TEACHER branch included; no flat 35-route graph).

#### Batch 3E-2 — entrypoint swap + total deletion of the old UI ✅ (this batch)
| Change | Detail |
|---|---|
| **`App.kt` rewritten** | Now drives `ui/v2/` **exclusively**. `KoinContext` → `MainViewModel` → Coil loader → while `!authState.isLoaded` a minimal lavender `VTheme(Light)` + `CircularProgressIndicator` splash, else `NavGraphV2(role = authState.role, isAuthenticated = !token.isNullOrBlank(), onLogout = { viewModel.logout() })`. The TEACHER role is handled inside `NavGraphV2` (`role.uppercase()` switch), so no separate start-destination branch is needed. The dev backend banner is preserved. **All imports of the old `ui.theme`/`ui.screens`/`navigation` removed.** |
| **Old `ui/` deleted in full** | Removed `ui/{theme,components,auth,screens,location,media}` from `commonMain` — every old `VidyaPrayag*` component, `VidyaPrayagTheme`, `MidnightColors`, all 17 admin + 14 parent screens, `SplashScreen`, `CommonLandingScreen`, `AuthBottomSheet`, the `expect` `LocationProvider`/`MediaPicker`/`PlatformAppearance`. Only `ui/v2/` and `App.kt` remain in `commonMain`. |
| **Old `navigation/` deleted** | `navigation/NavGraph.kt` (35-destination `NavHost`) + `AppNavigator.kt` (`ProvideAppNavigator`/`LocalAppNavigator`) removed — superseded by `NavGraphV2`. |
| **Platform actuals deleted** | The `actual` `LocationProvider`/`MediaPicker`/`PlatformAppearance` under `androidMain`/`iosMain`/`jsMain`/`jvmMain`/`wasmJsMain` removed (their only consumers were the deleted old screens; `ui/v2` uses none of them). Every platform entrypoint (`MainActivity`, jvm/web `main`, `MainViewController`) only calls `App()` — untouched. |

**Static verification (real Gradle build still owed — see constraint):**
- Every `App.kt` `ui/v2` import target exists (`NavGraphV2`, `VTheme`, `VColors`, `VPortalTone`, `vColorsFor`); `MainViewModel.authState`/`logout()` confirmed present.
- `NavGraphV2` → portal/screen function signatures match 1:1 (`onLogout`, `modifier`, `onAuthSuccess`, `onGetStarted`/`onHaveAccount`, `onOpenSchool`).
- **All 18 ViewModels** resolved by `koinViewModel()` across `ui/v2` are registered in `shared/di/Koin.kt` (Auth, Parent×4, School×6, Teacher×7).
- Repo-wide grep: **0 remaining code references** to any deleted symbol/package.

- [x] **`App.kt` swapped** to `ui/v2` via `NavGraphV2` (TEACHER role handled in the graph) + `logout()` wired.
- [x] **Old `ui/` + `navigation/` + platform actuals deleted** — `commonMain` now holds only `App.kt` + `ui/v2/`.
- [x] **Android compile fix:** `VInput.kt` was missing `import …ui.v2.theme.shapeMd` (the `shape*` helpers are top-level extensions on `VDimens`, so they must be imported into the `components` package). Resolved the `Unresolved reference 'shapeMd'` from `:composeApp:compileDevDebugKotlinAndroid`.
- [ ] **Run the real Gradle build** on a ≥8 GB host / dev machine (sandbox OOM-starves at config). This is the last gate before the next deploy.

## PHASE 4 — Polish

- [ ] Bundle Plus Jakarta Sans + DM Mono into `composeResources`; swap `defaultVTypography()` font families (single construction-site change, no call-site churn).
- [ ] Night-tone QA pass across all screens.
- [ ] Per-target smoke (android + jvm desktop + wasmJs).

---

*Rewritten 2026-06-06 from a file-by-file audit of `backend-by-abuzar`, cross-checked against every
commit message and PR #2. Claims that could be over-read (notably "portal complete" = authored, not
live) are flagged explicitly above.*
