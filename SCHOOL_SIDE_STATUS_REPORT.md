# VidyaPrayag — School Side Status Report

> **Branch analyzed:** `backend-by-abuzar`
> **Scope:** Entire **School (Admin)** surface — UI (`composeApp`), shared logic (`shared`), and backend (`server`).
> **Goal of this document:** For every school feature, state **what is working**, **what is NOT working**, and the **current result vs. desired result**, with concrete file references and prioritized fixes. No more silent placeholders.

---

## 0. How to read this report

Each feature is rated with one of:

| Status | Meaning |
|--------|---------|
| ✅ **WORKING** | End-to-end: UI → ViewModel → Repository → API → Server → DB, with real data, loading/error states. |
| 🟡 **PARTIAL** | Core flow works, but some sub-elements are still placeholder / cosmetic / unreachable. |
| 🔴 **BROKEN / NOT WIRED** | Feature is built but unreachable, no-op, or returns seeded/static content only. |

The architecture is **Kotlin Multiplatform**:
- `composeApp/` → Compose Multiplatform UI (screens live in `ui/screens/admin/`)
- `shared/` → domain models, ViewModels, repositories, Ktor API clients, Koin DI
- `server/` → Ktor backend (routes in `feature/school/`, `feature/onboarding/`, `feature/admissions/`, `feature/announcements/`)

---

## 1. Executive Summary

The last ~18 commits on `backend-by-abuzar` did a **large, genuine amount of wiring work**. The plumbing quality is high:

- **DI is complete** — every school ViewModel, repository, and API client is registered in `shared/.../di/Koin.kt`.
- **Every school screen is registered in navigation** (`navigation/NavGraph.kt`) and bound to a ViewModel via `koinViewModel()`.
- **Network layer is robust** — `safeApiCall` (`core/network/NetworkResult.kt`) handles success/HTTP-error/connection/parse errors uniformly.
- **Server authorization was hardened** — all school endpoints now route through `call.requireSchoolContext()` (`core/SchoolAccess.kt`), fixing cross-school data leaks.
- **Onboarding genuinely persists** real academic structure (schools, classes, subjects) to DB tables.

**However, three classes of problems remain that match your symptom ("onboarding works, the rest doesn't quite"):**

1. **🔴 The Analytics endpoints are CMS/seed-driven, not computed.** Most "analytics" content (cards, insights, performance_trend, subjects, milestones, narrative) is read verbatim from `app_config` rows seeded in `db/Seed.kt`. Only a few values are real live aggregates (attendance %, active-student counts, star-faculty ranking). So the screens *load and look real*, but the numbers are mostly **seeded constants**, not your school's data.

2. **🟡 Charts are "semi-real".** Across every analytics screen the **bar heights come from real ViewModel state**, but **axis labels and highlighted indices are hardcoded** (e.g. `"Jan".."Jun"`, `isActive = index == 3 // Mock Apr`, `isAnomaly = index == 9 // Mock Oct 26`). The **Academic Calendar grid is a fully static mock week** (days 9–15 with a hardcoded "UNIT 1 DEADLINE").

3. **🟡 A set of action buttons are still genuine no-ops** (`onClick = { }`) — filters, "see all", export/share, secondary actions on several screens.

The **401 / "Session expired"** issue noted in the most recent commit is an **environment problem, not a code bug**: a token minted against one backend (e.g. local dev) is being sent to a different backend (render.com), or the token expired. See §7.

---

## 2. Feature-by-Feature Matrix (quick view)

| # | School Feature | Screen | Status | Headline |
|---|----------------|--------|--------|----------|
| 1 | School Onboarding (4 steps) | `*OBScreen.kt` | ✅ WORKING | Persists schools/classes/subjects; completion logic unified. |
| 2 | School Dashboard | `SchoolDashboardScreen.kt` | ✅ WORKING | Driven by `/user/details`; correct pre/post-onboarding states. |
| 3 | Institutional Profile | `InstitutionalProfileScreen.kt` | ✅ WORKING | Real save, gallery/video add-remove, visibility toggle, storage text. |
| 4 | Admissions CRM | `AdmissionCRMDashboard.kt` | ✅ WORKING | Enquiry list, status dropdown, PTM-schedule dialog wired to API. |
| 5 | Messages (inbox + thread) | `MessagesScreen.kt` | ✅ WORKING | Threads, conversation view, send/reply, mark-read — full end-to-end. |
| 6 | Announcements | `SchoolAnnouncementsScreen.kt` | 🟡 PARTIAL | Create/list/search/filter wired; some affordances (`onClick={}`) remain. |
| 7 | Results | `ResultsScreen.kt` | ✅ WORKING | Real test/class/subject selectors, distribution chart, trend, search. |
| 8 | Daily Attendance | `DailyAttendanceScreen.kt` | 🟡 PARTIAL | Data wired; date-picker / "see all" button is a no-op. |
| 9 | Leave Requests | `LeaveRequestsScreen.kt` | ✅ WORKING | List + approve/reject scoped by school. |
| 10 | Academic Calendar | `AcademicCalendarScreen.kt` | 🔴 PARTIAL/MOCK | Event list wired, but **calendar grid is a static mock week**. |
| 11 | Schedule PTM | `SchedulePTMScreen.kt` | 🟡 PARTIAL | Create dialog wired; several secondary buttons are no-ops. |
| 12 | Analytics Dashboard | `AnalyticsDashboardScreen.kt` | 🟡 PARTIAL | Loads from API, but content is **CMS-seeded**, chart labels hardcoded. |
| 13 | Student Analytics (cohort) | `StudentAnalyticsScreen.kt` | 🟡 PARTIAL | Mostly CMS-seeded; anomaly index hardcoded. |
| 14 | Teacher Performance | `TeacherPerformanceScreen.kt` | 🟡 PARTIAL | `star_faculty` is real ranking; rest is CMS-seeded; chart cosmetic. |
| 15 | Class Performance | `ClassPerformanceScreen.kt` | 🟡 PARTIAL | `active_students` real; rest CMS-seeded. |
| 16 | Syllabus Coverage | `SyllabusCoverageScreen.kt` | 🔴 CMS-ONLY | 100% seeded `app_config` blob; subject labels hardcoded. |

---

## 3. WHAT IS WORKING (verified end-to-end)

### 3.1 ✅ School Onboarding — **FULLY WORKING**
**Files:** `feature/onboarding/OnboardingRouting.kt`, `OnboardingStatus.kt`, `AcademicInfoOBViewModel.kt`, `*OBScreen.kt`

- `ensureSchoolForUser()` creates/updates a real row in `SchoolsTable` and links it to `app_users.school_id`.
- `persistAcademicStructure()` upserts classes (`SchoolClassesTable`) by `(school, code)` and replaces subjects (`SchoolSubjectsTable`) — **idempotent**, real persistence.
- A sensible **default academic structure** is created when the client submits ACADEMIC without a payload, so the dashboard reflects reality.
- Completion logic unified (commit `6489ada`): the dashboard's onboarding status (`NOT_STARTED` / `IN_PROGRESS` / `COMPLETED`) is computed server-side from persisted steps.

**Current = Desired.** ✅

### 3.2 ✅ School Dashboard
**Files:** `SchoolDashboardScreen.kt`, `SchoolDashboardViewModel.kt`, `feature/school/SchoolDashboardRouting.kt`, `feature/user/UserDetailsRouting.kt`

- Reads `/api/v1/user/details`, drives the hero card between the onboarding state (progress %, "Continue/Start Onboarding") and the "Campus Live" state.
- Each onboarding step navigates to the correct step screen via `serverKey.toOnboardingDestination()`.
- Navigation to the rest of the app:
  - **Bottom bar** (`ui/components/SchoolBottomBar.kt`): Home, Messages, Announcements, Enquiries (Admissions CRM), Profile.
  - **Navigation drawer** (`ui/components/VidyaPrayagDrawer.kt`, gated on `userRole == "ADMIN"`): Analytics, Academic Calendar, Daily Attendance, Leave Request, Results, Schedule PTM.

> ⚠️ **Discoverability note (not a bug, but UX risk):** *Calendar / Attendance / Leave / Results / PTM are reachable **only** from the hamburger drawer.* There are **no cards on the dashboard** for them. If the drawer feels hidden, these features will appear "missing" to users. See §6.3.

### 3.3 ✅ Institutional Profile
**Files:** `InstitutionalProfileScreen.kt`, `InstitutionalProfileViewModel.kt`, `feature/user/UserProfileRouting.kt`, `UserProfileApi.kt`

- Real save (UPDATE philosophy), add/remove gallery photos & tour videos by URL, real storage-usage text, snackbar error surfacing, loading state.
- New `PUT /api/v1/user/profile/visibility` persists `public_profile`; `togglePublic` does optimistic update + rollback on failure.
- All profile endpoints hardened with `requireSchoolContext`.

### 3.4 ✅ Admissions CRM
**Files:** `AdmissionCRMDashboard.kt`, `AdmissionCRMViewModel.kt`, `AdmissionApi.kt`, `feature/admissions/AdmissionRouting.kt`

- Enquiry list from `/api/v1/admissions/enquiries`, status dropdown (PATCH scoped by `school_id`), PTM scheduling dialog, and a link into Analytics.
- Mutations are now **school-scoped** (was id-only — a cross-school mutation risk, fixed in `da1fe3c`).

### 3.5 ✅ Messages (best-implemented feature)
**Files:** `MessagesScreen.kt`, `MessagesViewModel.kt`, `MessagesApi.kt`, `MessagesRepositoryImpl.kt`, `feature/school/MessagesRouting.kt`

- `GET /threads`, `GET /threads/{id}/messages`, `POST /threads/{id}/read`, `POST /messages` all wired.
- Conversation view with message bubbles + reply box, optimistic mark-as-read with rollback, send + refresh, separate `isLoading`/`isSending` states. **No no-op thread tap anymore.**

### 3.6 ✅ Results
**Files:** `ResultsScreen.kt`, `ResultsViewModel.kt`, `ResultsApi.kt`, `feature/school/ResultsRouting.kt`

- Test cycle nav from real `available_tests`; class/subject dropdowns from real selectors.
- Distribution chart from real `exceeding/meeting/below` counts; `average_trend` computed **for real** server-side (current test class avg − previous test avg; `scoreToNumeric()` handles `"98"`, `"92%"`, and grade-style `A+..F`).
- Local name/ID search, real student-count footer, loading/error-retry/empty states.

### 3.7 ✅ Leave Requests
**Files:** `LeaveRequestsScreen.kt`, `LeaveRequestsViewModel.kt`, `feature/school/LeaveRequestsRouting.kt`
- List + approve/reject; mutations scoped by `school_id`.

---

## 4. WHAT IS NOT WORKING / PLACEHOLDER (the real gaps)

### 4.1 🔴 Analytics is seeded CMS content, not computed analytics
**File:** `server/.../feature/school/SchoolAnalyticsRouting.kt` + `server/.../db/Seed.kt`

The analytics endpoints return **`app_config` template rows** (seeded in `Seed.kt`) with only thin live overlays:

| Endpoint | What's REAL | What's SEEDED (static) |
|----------|-------------|------------------------|
| `GET /analytics/overview` | `cards[0].value` patched with live avg attendance % | `performance_trend`, `current_growth`, all other cards, all insights |
| `GET /analytics/class-performance` | `summary.active_students` (live count) | everything else in the blob (`avg_proficiency`, `median_grade`, at-risk lists, …) |
| `GET /analytics/teacher-performance` | `star_faculty` (real 30-day attendance ranking) | the rest of the blob |
| `GET /analytics/student/{id}` | KPI `attendance` (live 30-day), real student header | `average`, `rank`, subjects, milestones, narrative (all CMS) |
| `GET /analytics/syllabus-coverage` | **nothing** | 100% CMS blob `school_syllabus_coverage` |
| `GET /analytics/student-cohort` | `risk.low_count` derived from live active count | `critical_count`, `medium_count`, all other fields |

- **Current result:** Screens load and look populated, but the figures are demo constants seeded at boot, **identical for every school**.
- **Desired result:** Figures computed from each school's real `students`, `attendance_records`, `results`, `faculty`, `school_classes` tables.

> This is the **single biggest "looks done but isn't" item.** The seeding was an intentional pattern (ops can iterate copy without redeploy), but for production each metric needs a real aggregation query.

### 4.2 🔴 Academic Calendar grid is a static mock
**File:** `AcademicCalendarScreen.kt` (lines ~252–300)

- The calendar grid renders a **single hardcoded week** (`val day = (col + 9) // Just a mock start`) with a hardcoded `"UNIT 1 DEADLINE"` badge on day 15.
- The **event list** below the grid *is* wired to `AcademicCalendarViewModel` (real API data), but the **visual month/week grid ignores the API entirely**.
- **Current:** Grid always shows days 9–15, same fake event, regardless of month or real holidays/events.
- **Desired:** Grid built from the real `calendar`/`holidays` API for the selected month, with real event markers.

### 4.3 🟡 Charts render real heights but hardcoded labels/highlights
**Files:** `AnalyticsDashboardScreen.kt`, `StudentAnalyticsScreen.kt`, `TeacherPerformanceScreen.kt`, `SyllabusCoverageScreen.kt`

| Screen | Real | Hardcoded |
|--------|------|-----------|
| Analytics Dashboard | bar heights = `state.performanceTrend` | labels `Jan..Jun`; `isActive = index == 3 // Mock Apr` |
| Student Analytics | bar heights = `trend` | `isAnomaly = index == 9 // Mock Oct 26` |
| Teacher Performance | bar heights = `trend` | no x-axis labels at all (cosmetic bars) |
| Syllabus Coverage | bar heights = `stats` | labels `Science/Math/Arts/Languages/History` hardcoded |

- **Desired:** Labels and highlighted points derived from the same API series that supplies the heights.

### 4.4 🟡 Remaining no-op buttons (`onClick = { }`)
Confirmed genuine no-ops on the school surface:

| File | Line (approx) | Element |
|------|------|---------|
| `AdmissionCRMDashboard.kt` | 222 | a `TextButton` |
| `AnalyticsDashboardScreen.kt` | 242, 286 | card action / header action |
| `DailyAttendanceScreen.kt` | 217 | "see all" / date `TextButton` |
| `MessagesScreen.kt` | 428 | a header/action button |
| `SchedulePTMScreen.kt` | 237, 337, 402, 414 | secondary actions + an `IconButton` |
| `SchoolAnnouncementsScreen.kt` | 357, 453 | "see all" / secondary action |
| `StudentAnalyticsScreen.kt` | 303 | an `IconButton` |
| `SyllabusCoverageScreen.kt` | 170, 257 | action button + `IconButton` |
| `TeacherPerformanceScreen.kt` | 337 | an action button |

> Note: onboarding screens (`AcademicInfoOBScreen.kt`, `BrandingInfoOBScreen.kt`, `LaunchInfoOBScreen.kt`) also contain a few `onClick = { /* … */ }` placeholders (e.g. "Change", "Assign", "Show more"), but onboarding's primary path works.

`placeholder = "…"` matches in `TextField`s are **legitimate hint text**, not bugs.

---

## 5. Backend (server) — Detailed Status

| Area | File | Status | Notes |
|------|------|--------|-------|
| Authorization guard | `core/SchoolAccess.kt` | ✅ | `requireSchoolContext()` → 401/403/404. Role read from DB, not JWT (good). |
| Routing registration | `Application.kt` | ✅ | All school routes mounted under `authenticate("jwt")`. |
| Auth (OTP/JWT) | `feature/auth/AuthRouting.kt` | ✅ | Real OTP, no hardcoded codes/demo users; role normalized (`ADMIN`→`school_admin`). |
| Onboarding | `feature/onboarding/OnboardingRouting.kt` | ✅ | Real persistence (schools/classes/subjects). |
| Admissions | `feature/admissions/AdmissionRouting.kt` | ✅ | School-scoped mutations. |
| Announcements | `feature/announcements/AnnouncementRouting.kt` | ✅ | Real recipient count; platform-admin override gated; foreign IDs rejected. |
| Messages | `feature/school/MessagesRouting.kt` | ✅ | Full thread/message CRUD, school-scoped. |
| Leave Requests | `feature/school/LeaveRequestsRouting.kt` | ✅ | School-scoped approve/reject. |
| Results | `feature/school/ResultsRouting.kt` | ✅ | Real `average_trend` + numeric/grade parsing. |
| PTM | `feature/school/PtmRouting.kt` | 🟡 | Create works; some metrics DTOs added but frontend wiring pending. |
| School (calendar/holidays/attendance) | `feature/school/SchoolRouting.kt` | 🟡 | Range-scoped holiday counts (good); calendar grid not consumed by UI. |
| **Analytics** | `feature/school/SchoolAnalyticsRouting.kt` | 🔴 | **CMS/seed-driven** (see §4.1) with thin live overlays. |
| Seed data | `db/Seed.kt` | ⚠️ | Seeds `school_analytics_*` `app_config` rows — this is the source of the "fake" analytics numbers. |

---

## 6. Current vs. Desired — the critical items

### 6.1 Analytics
- **Current:** Same seeded numbers for every school; only attendance %, active-student count, and star-faculty are live.
- **Desired:** Per-school computed aggregates for performance trend, proficiency, median grade, at-risk cohort, subject coverage, student KPI (average + rank), and narrative.

### 6.2 Academic Calendar grid
- **Current:** Static mock week ignoring the API.
- **Desired:** Month grid generated from real dates + real holiday/event markers from the calendar API.

### 6.3 Feature discoverability
- **Current:** Calendar/Attendance/Leave/Results/PTM only reachable from the hamburger drawer; no dashboard cards.
- **Desired:** Add a "Quick Actions" / management grid on the `SchoolDashboard` (Campus-Live state) linking to all school destinations, so nothing depends on finding the drawer.

### 6.4 No-op buttons
- **Current:** ~14 buttons do nothing.
- **Desired:** Each either wired to its ViewModel action or removed.

---

## 7. The 401 / "Session expired" issue (environment, not code)

From commit `c81aebc`. Root cause is **token/backend mismatch**, not the app logic:

- `shared/build.gradle.kts` & `composeApp/build.gradle.kts` resolve a **dev base URL** from `local.properties` (`devBaseUrl`), falling back to `https://vidyaprayag-1.onrender.com` when absent. The gradle script already **warns loudly** when it falls back.
- If a JWT was minted by a **local** server but the app is pointed at **render.com** (or vice versa), or the token simply expired, every authenticated school call returns **401** — which surfaces as empty screens.

**To resolve:**
1. Confirm the app and the token come from the **same** backend. Check the boot log (`authBaseUrl` / `schoolBaseUrl` are logged at startup).
2. For local testing, set `devBaseUrl=http://<your-LAN-ip>:8080` in `local.properties` and re-login so a fresh token is issued by that server.
3. Ensure `JWT_SECRET` is identical between the server that issued the token and the server validating it.
4. Re-login to refresh expired tokens (refresh flow exists: `POST /api/v1/auth/refresh`).

---

## 8. Prioritized Fix List (to remove all placeholders)

**P0 — Makes data real**
1. Replace CMS-seeded analytics with real aggregation queries in `SchoolAnalyticsRouting.kt` (overview trend, class proficiency/median, teacher metrics, student average/rank, syllabus coverage, cohort risk).
2. Rebuild the Academic Calendar grid from real dates + the calendar/holidays API.

**P1 — Removes cosmetic fakes**
3. Drive chart axis labels & highlighted indices from API series (4 analytics screens).
4. Wire or remove the ~14 no-op `onClick = { }` buttons (§4.4).

**P2 — UX / completeness**
5. Add a management/quick-action grid to `SchoolDashboard` (Campus-Live) so all features are discoverable without the drawer.
6. Finish PTM metrics wiring (DTOs exist server-side; connect the UI).
7. Resolve no-op onboarding affordances ("Change"/"Assign"/"Show more").

**P3 — Environment**
8. Document & enforce backend/token consistency to eliminate 401s (§7).

---

## 9. Verification method

This report was produced by static analysis of the `backend-by-abuzar` branch:
- Inspected DI (`di/Koin.kt`), navigation (`navigation/NavGraph.kt`), all 19 admin screens, all admin ViewModels/repositories/APIs in `shared`, and all server routes in `feature/school`, `feature/onboarding`, `feature/admissions`, `feature/announcements`, `feature/auth`.
- Cross-checked the last 18 commits' claims against the actual code.
- A full Gradle compile was **not** run here (KMP toolchain/deps require network); the latest commits report `:server:compileKotlin` and `:composeApp:compileDevDebugKotlinAndroid` SUCCESS. Recommend running `./gradlew :server:compileKotlin :composeApp:assembleDevDebug` in CI to confirm.

*Generated for the school-side audit. Update this file as fixes land.*
