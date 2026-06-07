# Backend / Database Gap Audit & Full Wire-Up Plan — `ui/v2`

> **Re-audited 2026-06-07 (branch `backend-by-abuzar`).** The previous version of this
> document made claims that did **not** match the code. This rewrite verifies every claim
> against the actual source (file:line cited) using the method in §0, then gives a concrete,
> phased plan (§7–§10) to make the application run **end-to-end** with real backend + API +
> a connected database, including the new screens required for backend features that have none.

Legend: ✅ real & wired · 🟡 exists but not connected to UI · 🔴 missing / stubbed.

---

## 0. How this audit was performed (reproducible)

Every statement below was derived from these commands, not from prior docs:

```bash
# UI screens
find composeApp/src/commonMain/.../ui/v2/screens -name '*.kt'          # 31 files
# ViewModels that exist
find shared -name '*ViewModel.kt'                                      # 42 VMs
# VMs actually USED by the UI (excluding comments & imports)
for vm in <each VM>; do grep -rn "$vm" composeApp/src --include='*.kt' \
   | grep -vE '^\s*\*|//|import'; done                                 # only 2 hit
# API clients real vs stubbed
for api in $(find shared -name '*Api.kt'); do
   grep -cE 'client\.(get|post|put|patch|delete)' "$api"; done         # 16 real, 1 stub
# Koin registrations
grep -cE 'viewModel|factory|single' shared/.../di/Koin.kt              # 79 defs
# Server routes
grep -rhoE '(get|post|put|patch|delete)\("[^"]+"\)' server/src/main    # ~80 routes
# DB tables
grep -cE 'object [A-Za-z]+ : .*Table' server/.../db/Tables.kt          # 36 tables
```

---

## 1. ⚠️ HEADLINE FINDING — the UI is mock-driven, not backend-driven

This is the single most important correction. The previous doc claimed ~20 screens were
"backed by repository → API". **That is false.** Verified reality:

| Layer | Reality (verified) | Evidence |
|---|---|---|
| **ViewModels bound to a screen** | **2 of 42** | `grep` for each VM in `composeApp/src` (non-comment): only `AuthViewModel` (LoginScreenV2) and `MainViewModel` (App shell) appear. The other **40 VMs are registered in Koin but bound to no `ui/v2` screen.** |
| **Screens that call a real VM/repo/API** | **1** (`LoginScreenV2`) | `LoginScreenV2.kt:87` `viewModel: AuthViewModel = koinViewModel()` — the only true binding. |
| **Screens driven by `MockV2`** | **18** | `grep -rln MockV2 ui/v2/screens` → Discovery, AcademicCalendar, Notifications, all Parent*, all School*, most Teacher*, ParentLinkChild. |
| **`MockV2`** | 338 lines of hardcoded seed data, "never touches the network/`shared/` layer" | `ui/v2/data/MockV2.kt:1-12` (its own KDoc says so). |
| **"koinViewModel()s its own VM" in portals** | comment only, not code | `SchoolPortalV2.kt:28` and `TeacherPortalV2.kt:36` say this **inside KDoc** — no such call exists in any `*ScreenV2`. |

**Conclusion:** `shared/` (the backend client layer) is genuinely built — 16/17 API clients
make real Ktor HTTP calls, 42 VMs exist, all wired in Koin — but the **`ui/v2` presentation
layer is disconnected from it.** The app currently renders a faithful *prototype* off `MockV2`
and only performs one real network operation: **authentication.**

---

## 2. ✅ What IS real and connected (verified)

| Item | Status | Evidence |
|---|---|---|
| **Auth flow** (login / signup / OTP / refresh) | ✅ real, wired UI→API→DB | `LoginScreenV2`→`AuthViewModel`→`AuthRepositoryImpl`→`AuthApi.kt` (6 real `client.*` calls)→server `AuthRouting.kt`→`AppUsersTable`/`AuthOtpsTable`/`UserSessionsTable`. |
| **Session persistence** | ✅ | `AuthRepositoryImpl.saveSession` → `PreferenceRepository.set{Token,Role}`; `MainViewModel` combines role+token → `authState`; `NavGraphV2` routes on it. |
| **Backend HTTP clients** | ✅ 16/17 real | `AdmissionApi`(4) `AnalyticsApi`(6) `AnnouncementsApi`(4) `AttendanceApi`(1) `CalendarApi`(1) `LeaveRequestsApi`(3) `MediaApi`(2) `MessagesApi`(4) `OnboardingApi`(3) `PtmApi`(2) `ResultsApi`(2) `UserProfileApi`(5) `AuthApi`(6) `ContentApi`(1) `ParentApi`(4) `TeacherApi`(11). |
| **Ktor server** | ✅ ~80 routes across 30+ routing files | `server/src/main/.../feature/**/*Routing.kt`; entry `Application.kt`; port 8080 (`application.conf`). |
| **Database** | ✅ 36 Exposed tables, Supabase-PG (prod) / SQLite (dev) | `db/Tables.kt` (36 `*Table`), `db/DatabaseFactory.kt` (Hikari + dual-mode), `db/Seed.kt`. |
| **OTP delivery** | ✅ pluggable (console/Twilio/MSG91/Fast2SMS/SMTP/WhatsApp) | `feature/auth/delivery/providers/*`. |

---

## 3. 🔴 The one stubbed API client (not real HTTP)

| API client | Reality | Evidence |
|---|---|---|
| `KtorSchoolApi.fetchSchools()` | **Stub** — returns 3 hardcoded `School`s with `picsum.photos` images. Comment: *"In a real app, this would be an actual URL. For now, we simulate a network response."* | `KtorSchoolApi.kt:13-50` (0 `client.*` calls). The server **does** expose `GET /schools/discover` (`SchoolRouting.kt`), so this client just needs to be implemented against it. |

---

## 4. 🟡 Backend features fully built but with NO connected UI (the 40 unbound VMs)

These have API client + repository + ViewModel + (mostly) a server route, but **no `ui/v2`
screen consumes them.** They are not "missing backend" — they are "missing the wire + (in
some cases) a screen." Grouped by whether a *host screen already exists* to wire into.

### 4.1 Backend exists + a host screen EXISTS → just wire the VM in (no new screen)
The screen is built (off `MockV2`); swap mock for the VM. Verified VM↔API↔route chain:

| Feature | ViewModel | API → server route | Host screen to wire into |
|---|---|---|---|
| Parent track-progress | `TrackProgressViewModel` | `ParentApi` → `GET /track-progress` | `ParentAcademicsScreenV2` |
| Parent fees | `FeeViewModel` | `ParentApi` → `GET /fees` | `ParentFeesScreenV2` |
| Parent announcements | `ParentAnnouncementViewModel` | `ParentApi`/`AnnouncementsApi` → `GET /announcements` | `ParentActivityScreenV2` |
| Parent dashboard | `ParentDashboardViewModel` | `ParentApi` → `GET /dashboard` | `ParentHomeScreenV2` |
| Teacher home | `TeacherHomeViewModel` | `TeacherApi` → `GET /home` | `TeacherHomeScreenV2` |
| Teacher attendance | `TeacherAttendanceViewModel` | `TeacherApi` → attendance routes | `TeacherAttendanceScreenV2` |
| Teacher classes | `TeacherClassesViewModel` | `TeacherApi` → `GET /classes` | `TeacherClassesScreenV2` |
| Teacher marks | `TeacherMarksViewModel` | `TeacherApi` → assessment routes | `TeacherMarksScreenV2` |
| Teacher homework | `TeacherHomeworkViewModel` | `TeacherApi` → homework routes | `TeacherHomeworkScreenV2` |
| Teacher syllabus | `TeacherSyllabusViewModel` | `TeacherApi` → syllabus routes | `TeacherSyllabusScreenV2` |
| Teacher profile | `TeacherProfileViewModel` | `UserProfileApi` → `GET /profile` | `TeacherProfileScreenV2` |
| School dashboard | `SchoolDashboardViewModel` | `AnalyticsApi`/Dashboard → `GET /dashboard` | `SchoolHomeScreenV2` |
| School announcements | `SchoolAnnouncementsViewModel` | `AnnouncementsApi` → `GET/POST /announcements` | `SchoolCommsScreenV2` |
| Student analytics | `StudentAnalyticsViewModel` | `AnalyticsApi` → `GET /student/{id}`,`/student-cohort` | `SchoolPeopleScreenV2` |
| Syllabus coverage | `SyllabusCoverageViewModel` | `AnalyticsApi` → `GET /syllabus-coverage` | `SchoolRecordsScreenV2` |
| Institutional profile | `InstitutionalProfileViewModel` | `UserProfileApi` → profile routes | `SchoolSettingsScreenV2` |
| Academic calendar | `AcademicCalendarViewModel` | `CalendarApi` → `GET /calendar`,`/holidays` | `AcademicCalendarScreenV2` |
| Discovery schools | (new VM or reuse) | `KtorSchoolApi`→`GET /schools/discover` (after §3 fix) | `DiscoveryScreenV2` |
| 4× School onboarding | `InstitutionalBasicOB`/`BrandingInfoOB`/`AcademicInfoOB`/`LaunchInfoOB` | `OnboardingApi` → `POST /submit`,`/step` | `SchoolOnboardingScreenV2` |

### 4.2 Backend exists but NO host screen → build a NEW screen, then wire
| Feature | ViewModel | API → route | New screen needed |
|---|---|---|---|
| Admissions CRM | `AdmissionCRMViewModel` | `AdmissionApi` → `/admissions/enquiries*`,`/summary`,`PATCH /{id}/status` | `AdmissionsCrmScreenV2` |
| Daily attendance register (admin) | `DailyAttendanceViewModel` | `AttendanceApi` → `GET /attendance/daily` | `DailyAttendanceScreenV2` |
| Class performance analytics | `ClassPerformanceViewModel` | `AnalyticsApi` → `GET /class-performance` | `ClassPerformanceScreenV2` |
| Teacher performance analytics | `TeacherPerformanceViewModel` | `AnalyticsApi` → `GET /teacher-performance` | `TeacherPerformanceScreenV2` |
| Analytics dashboard (admin) | `AnalyticsDashboardViewModel` | `AnalyticsApi` → `/analytics`,`/overview`,`/trend` | `AnalyticsDashboardScreenV2` |
| Leave requests (approve/reject) | `LeaveRequestsViewModel` | `LeaveRequestsApi` → `/leave-requests*`,`PATCH /{id}/status` | `LeaveRequestsScreenV2` |
| Messages / threads (school inbox) | `MessagesViewModel` | `MessagesApi` → `/threads*`,`POST /messages` | `MessagesScreenV2` |
| Results publishing | `ResultsViewModel` | `ResultsApi` → `GET/POST /results` | `ResultsPublishScreenV2` |
| Schedule PTM (school) | `SchedulePTMViewModel` | `PtmApi` → `/ptm`,`PUT /{id}/class-progress` | `SchedulePtmScreenV2` |
| Scholarships (parent) | `ScholarshipsViewModel` | `ParentApi` → `GET /scholarships` | `ScholarshipsScreenV2` |
| Media upload | (needs thin VM) `MediaApi` | `POST /media/upload`,`DELETE`,`PUT /gallery|tour-videos` | wire into onboarding / `SchoolSettingsScreenV2` gallery |

### 4.3 VMs that are genuinely local-only (no API — do NOT wire to a server)
`CareerPathViewModel`, `ParentReportsViewModel`, `ParentMessageViewModel`,
`ParentSchedulePTMViewModel`, `LocationRequestViewModel`, `ChildBasicInfoViewModel`,
`YourPreferencesViewModel`, `DailyStatusViewModel` — all are `MutableStateFlow`-only with no
repository (verified: no `*Repository` reference in their bodies). `LandingViewModel`/
`MainViewModel` are app-shell. These need backend built *first* (see §5) if they are to persist.

---

## 5. 🔴 Screens whose backend genuinely does NOT exist yet (build API + schema)

| Screen | Bound VM (local-only) | Missing API | Missing tables |
|---|---|---|---|
| `ParentLinkChildScreenV2` | `ChildBasicInfoViewModel`, `YourPreferencesViewModel` (no repo) | `POST /parent/link-child`, `GET /parent/schools/search`, `GET /parent/children` | `parent_child_links`, `parent_preferences`, `student_admission_index` |
| `TeacherFirstLoginScreenV2` | none (local `validate()`) | `POST /auth/change-password` | `app_users.must_change_password BOOLEAN` (gates the forced step) |
| `NotificationsScreenV2` | none — `items` param **defaults to `MockV2.notifications`** (NOT empty, contrary to the old doc) | `GET /notifications`, `POST /{id}/read`, `POST /read-all`, `GET/PUT /preferences` | `notifications`, `notification_preferences` |

> Correction logged: old §2.3 said Notifications "is driven by an empty list." Verified false —
> `NotificationsScreenV2.kt:77` defaults `items = MockV2.notifications.map { … }`.

Nice-to-have (already noted in UI audit): `calendar_events.event_type` for typed calendar dots;
Discovery map/geo (`G11`, rendered as `VComingSoon`).

---

## 6. Corrections vs the previous version of this file

| Old claim | Verified reality |
|---|---|
| §1 "SchoolOnboarding / AcademicCalendar / Login are fully backed" | Only **Login** is. Onboarding uses hardcoded `OBClass/OBSubject/OBTeacher` lists; AcademicCalendar reads `MockV2.calendarEvents`; their VMs are **bound nowhere** in `composeApp` (grep = 0). |
| §5 "all other v2 screens bind a VM that calls repo→API" | False. 18 screens read `MockV2`; only Login binds a VM. |
| §2.3 Notifications "empty list → all caught up" | False. Defaults to `MockV2.notifications`. |
| "24 screens (+Shared.kt)" | 31 screen `.kt` files now. |
| Implicit: `KtorSchoolApi` is a real client | It is a **stub** (3 picsum schools). |

The §4 "reverse gap" lists (backend without UI) were **directionally correct**, but understated
the problem: it is not just ~10 features missing a screen — it is **the entire feature UI** that
is unwired (40 of 42 VMs).

---

## 7. PLAN — make the app run end-to-end (real backend + API + connected DB)

Five phases. Each phase is independently shippable and committed to `backend-by-abuzar`.

### Phase A — Stand the backend + DB up locally (no app changes)
1. `cp .env.example .env`. For **zero-setup local dev**, leave `DATABASE_URL` empty → server
   auto-creates a **SQLite `data.db`** and runs `SchemaUtils.createMissingTablesAndColumns`
   (`DatabaseFactory.kt`). Set `JWT_SECRET` to any long random string.
2. Run the server: `./gradlew :server:run` → boots Ktor on `:8080`, seeds CMS rows
   (`APP_SEED_CMS=true`), and (SQLite) creates all 36 tables.
3. Smoke-test routes: `curl localhost:8080/version`, `/app-status`, `POST /check-user`,
   `/schools/discover`. The server already has `RouteInventoryTest` + `CalendarContractTest`.
4. **Production DB:** set `DATABASE_URL`/`DATABASE_USER`/`DATABASE_PASSWORD` to Supabase PG.
   Schema is **not** auto-migrated against PG by design — run the two SQL files by hand:
   `/supabase_schema` then `/docs/backend/sql/01_supplementary_schema.sql` (per
   `DatabaseFactory` KDoc). Confirm row counts, then point the app at it.

**Exit criteria:** server boots; `/diagnostic` green; auth round-trips against the DB.

### Phase B — Point the app at the backend & prove the live path
1. In `local.properties` set `devBaseUrl=http://<laptop-LAN-ip>:8080` (Android dev flavor
   reads it via `BuildConfig.*_BASE_URL` → `Config.android.kt`). For non-Android, set the
   `expect/actual Config` base URLs.
2. The **auth path is already live** — verify real login/OTP against the running server end
   to end on device/emulator. This is the reference pattern every other screen will copy.

**Exit criteria:** a real account logs in against the local/Supabase server; token persists;
`NavGraphV2` routes to the role portal.

### Phase C — Wire the 19 existing screens to their VMs (§4.1) — replace `MockV2`
For each screen: add `viewModel: XxxViewModel = koinViewModel()`, collect its state, render
real data, keep `MockV2` only as `@Preview`/empty-state fallback. Cadence: 3 screens →
commit → push → update PR #5. Suggested order (lowest risk first):
1. Parent: Home, Academics, Fees, Activity (4 VMs already HTTP-real).
2. Teacher: Home, Classes, Attendance, Marks, Homework, Syllabus, Profile.
3. School: Home, Comms, People (StudentAnalytics), Records (SyllabusCoverage), Settings.
4. Discovery + AcademicCalendar (needs §3 `KtorSchoolApi` real impl first).
5. SchoolOnboarding: wire the 4 OB VMs → `OnboardingApi.submit`/`step`.

**Per-screen checklist:** state has `loading`/`error`/`empty`; show `VComingSoon`/skeleton on
load; never fabricate data; brace/paren + unused-import sanity (no local build/JDK in sandbox).

### Phase D — Build the 11 missing screens (§4.2) + wire their VMs
Build each new `*ScreenV2` to the React look (reuse `V*` primitives, `VMotion`, tones), then
bind the already-existing VM. Add each to the correct portal's tab/overlay + `NavGraphV2`.
Priority by user value: Messages → LeaveRequests → AdmissionsCrm → Results → SchedulePtm →
DailyAttendance → Class/TeacherPerformance → AnalyticsDashboard → Scholarships → Media.

### Phase E — Build the 3 genuinely-missing backends (§5)
1. **Notifications:** tables `notifications`,`notification_preferences`; routes
   `GET /notifications`,`POST /{id}/read`,`POST /read-all`,`GET/PUT /preferences`;
   `NotificationsApi`+repo+`NotificationsViewModel`; wire into existing `NotificationsScreenV2`.
2. **Parent link-child:** tables `parent_child_links`,`parent_preferences`,
   `student_admission_index`; routes `POST /parent/link-child`,`GET /parent/schools/search`,
   `GET /parent/children`; extend `ParentApi`+repo; wire `ParentLinkChildScreenV2.submit`.
3. **Teacher first-login:** `app_users.must_change_password`; `POST /auth/change-password`;
   extend `AuthApi`/`AuthViewModel`; gate the screen in `NavGraphV2` on the flag.

---

## 8. Definition of Done (run-fully checklist)

- [ ] Server boots against a **connected DB** (SQLite dev or Supabase PG) — Phase A.
- [ ] App authenticates against the live server on device — Phase B.
- [ ] 0 of 18 feature screens still read `MockV2` for primary data (preview-only allowed) — C/D.
- [ ] All 40 currently-unbound VMs are either bound to a screen or explicitly classified
      local-only (§4.3) — C/D.
- [ ] `KtorSchoolApi` makes a real `GET /schools/discover` call — §3.
- [ ] Notifications / Parent-link / Teacher-first-login backends exist & are wired — Phase E.
- [ ] `RouteInventoryTest` + contract tests green; every wired screen has loading/error/empty.

---

## 9. Effort & sequencing summary

| Phase | Scope | New backend? | New screens? | Risk |
|---|---|---|---|---|
| A | Run server + DB | no | no | low |
| B | App → backend, prove auth | no | no | low |
| C | Wire 19 existing screens (§4.1) | no | no | medium (data shapes) |
| D | 11 new screens (§4.2) | no | **11** | medium |
| E | 3 missing backends (§5) | **yes** (3) | uses existing | high |

The bulk of "make it real" is **Phase C** (wiring) — pure plumbing against already-working
APIs. **Phase E** is the only true backend build. New screens (§4.2/Phase D) reuse the design
system, so they are "build the screen against a working API," not greenfield backend work.

---

## 10. Appendix — verified inventories (2026-06-07)

- **31** `ui/v2` screen files · **42** ViewModels · **17** API clients (16 real HTTP, 1 stub) ·
  **~80** server routes · **36** DB tables · **79** Koin definitions.
- **2** VMs used by UI (`AuthViewModel`, `MainViewModel`); **40** unbound.
- **18** screens read `MockV2`; **1** screen (`Login`) binds a real VM.
- DB modes: Supabase Postgres (`DATABASE_URL` set) | SQLite `data.db` (default). Port `8080`.
