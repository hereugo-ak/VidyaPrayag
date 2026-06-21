# VidyaSetu — Master Rebuild & System-of-Record Document

**Status:** Pre-build ground truth. Authored after a full read of the codebase on branch `main`
(which already contains the merged `backend-by-abuzar` work) plus the design source in this
`UI screens/` folder.
**Scope of this document:** Steps 1–6 + 9 of the engineering mandate (find everything → schema →
routes → old frontend → connection map → gaps → handoff). Step 7 (build the missing backend) and
Step 8 (delete the old frontend, swap to the new Compose UI) are tracked here as an execution plan
and will be performed in subsequent, build-verified passes.
**Decision locked with product owner:** *Option A* — **keep the entire `shared/` data layer**
(APIs, repositories, ViewModels, Koin DI). Rebuild only the `composeApp/ui/` layer to this design,
authored in a parallel `ui/v2/` package, then swap the entrypoint and delete the old `ui/`. Add the
new `feature/teacher` vertical + backend teacher routes. The stack stays **Compose Multiplatform** —
the React/TSX files in this folder are the *design source*, to be translated to Compose, not shipped.

> **Provenance note (read this).** The original `VidyaSetu_Frontend_Rebuild_Plan.md` in
> `src/imports/` assumed a **FastAPI + Supabase** backend and a **Void/Cloud/Arctic** palette. The
> code on disk tells a different, authoritative story: the backend is **Ktor (Kotlin)** talking to
> **Postgres/Supabase**, and the live design tokens in `src/styles/theme.css` have evolved to a
> **warm teal / navy / lavender** system with a deep-black *night* variant. **Where the plan and the
> code disagree, the code wins.** This document reflects the code.

---

## Table of Contents

1. [Step 1 — Full project tree](#step-1)
2. [Step 2 — Database schema](#step-2)
3. [Step 3 — Backend routes & endpoints](#step-3)
4. [Step 4 — Old frontend map (the death record)](#step-4)
5. [Step 5 — Master connection document (by role)](#step-5)
6. [Step 6 — Gap analysis (design vs. backend)](#step-6)
7. [Design system — the visual language (authoritative tokens)](#design-system)
8. [The `V*` component library → Compose mapping](#components)
9. [Step 7 plan — build the missing backend](#step-7)
10. [Step 8 plan — delete old UI, swap to `ui/v2`](#step-8)
11. [Step 9 — Handoff summary](#step-9)

---

<a name="step-1"></a>
## 1. Step 1 — Full Project Tree

Monorepo: a Kotlin Multiplatform client (`composeApp` + `shared`) and a Ktor server (`server`),
plus this React design prototype (`UI screens/`).

```
/ (repo root)
├── settings.gradle.kts            KMP + server module wiring
├── build.gradle.kts               root build
├── gradle/ , gradlew              Gradle wrapper + version catalog (libs.versions.toml)
├── supabase_schema                ★ THE DATABASE SCHEMA (11 CREATE TABLE) — Postgres/Supabase DDL
├── Dockerfile                     server container build
├── README.md / SERVER_QUICKSTART.md / DEVELOPMENT_STANDARDS.md / API_DOCUMENTATION.md
├── parent_api_spec.artifact.md    parent endpoints spec
├── vidya_prayag_api_spec.artifact.md / *_spec2.artifact.md   full API specs
├── .env.example                   server + OTP provider env keys
│
├── server/                        ★ BACKEND — Ktor (Kotlin), NOT FastAPI
│   └── src/main/kotlin/com/littlebridge/vidyaprayag/
│       ├── Application.kt          Ktor app bootstrap, plugin install
│       ├── ServerEntry.kt          main()
│       ├── core/                   ApiResponse, ErrorHandling, JwtConfig, SecurityModule, ResponseExtensions
│       ├── db/
│       │   ├── DatabaseFactory.kt  HikariCP/Exposed connection
│       │   ├── Tables.kt           Exposed table objects (mirror of supabase_schema)
│       │   └── Seed.kt             dev seed data
│       └── feature/
│           ├── auth/               AuthRouting, OtpService, OtpAdminRouting,
│           │   └── delivery/       OTP via Twilio / Msg91 / Fast2SMS / WhatsAppCloud / SMTP / Console
│           ├── onboarding/         OnboardingRouting (school setup wizard backend)
│           ├── school/             SchoolRouting
│           ├── content/            LandingRouting (marketing/landing content)
│           ├── admissions/         AdmissionRouting (admission CRM / enquiries)
│           ├── announcements/      AnnouncementRouting
│           ├── config/             AppStatusRouting (app status / feature flags)
│           └── user/               ParentRouting, UserDetailsRouting, UserProfileRouting
│                                   ▲ NO teacher/ feature → confirms gap G1
│
├── shared/                        ★ KEEP — KMP data layer (APIs, repos, domain, ViewModels, DI)
│   └── src/commonMain/kotlin/com/littlebridge/vidyaprayag/
│       ├── di/Koin.kt              ★ Koin DI — 4 APIs, 4 repos, 1 usecase, 33 ViewModels
│       ├── core/ (model, network, prefs)   shared models, HTTP, PreferenceRepository (token store)
│       ├── util/                   AppConfig (authBaseUrl/schoolBaseUrl), AppLogger
│       ├── presentation/           MainViewModel (auth+theme), ParentDashboardViewModel
│       └── feature/
│           ├── auth/      data/remote(AuthApi) · data/repository · domain · presentation(AuthViewModel)
│           ├── content/   data/remote(ContentApi) · repository · domain · presentation(LandingViewModel)
│           ├── parent/    ParentApi · repository · domain · presentation(11 VMs — see §5)
│           ├── admin/     domain/model(Onboarding*) · presentation(19 VMs — see §5)
│           └── schools/   KtorSchoolApi · repository(+local) · domain(+usecase GetSchoolsUseCase)
│       (NO feature/teacher → greenfield vertical to add)
│
├── composeApp/                    ★ REPLACE the ui/ layer; keep platform plumbing
│   ├── build.gradle.kts           targets: android, ios(Arm64+SimArm64), jvm, js, wasmJs
│   └── src/
│       ├── commonMain/kotlin/com/littlebridge/vidyaprayag/
│       │   ├── App.kt              ★ entrypoint: Koin → MainViewModel → VidyaPrayagTheme → Splash|NavGraph
│       │   ├── navigation/         AppNavigator, NavGraph (Destination sealed graph — 35 destinations)
│       │   └── ui/                 ◀── EVERYTHING UNDER ui/ IS THE OLD UI TO REPLACE
│       │       ├── theme/          Color, MidnightColors, Theme, ThemeState, PlatformAppearance
│       │       ├── components/     11 VidyaPrayag* + Onboarding components + bottom bars
│       │       ├── auth/           AuthBottomSheet
│       │       └── screens/        CommonLanding, Splash, admin/* (17), parent/* (14)
│       ├── androidMain / iosMain / jvmMain / jsMain / wasmJsMain / webMain
│       │                           platform entrypoints + per-platform theme appearance
│       └── commonTest
│
├── iosApp/                        Xcode wrapper for the iOS target
├── kotlin-js-store/               JS lockfile
├── docs/                          misc docs
│
└── UI screens/                    ★ DESIGN SOURCE (this folder) — React + shadcn/ui + Tailwind prototype
    ├── VIDYASETU_MASTER_REBUILD_DOC.md   ◀── THIS FILE
    ├── README.md / ATTRIBUTIONS.md / guidelines/Guidelines.md
    ├── src/styles/theme.css        ★ AUTHORITATIVE design tokens (teal/navy/lavender + night)
    ├── src/styles/{globals,tailwind,fonts,index}.css
    ├── src/app/App.tsx             screen graph / dev portal switcher
    ├── src/app/lib/mock.ts         ★ data shapes every screen renders (school, students, fees…)
    ├── src/app/components/v/
    │   ├── primitives.tsx          ★ VLogo, VCard, VButton, VInput, VBadge, VTag, VAvatar,
    │   │                             VStatusDot, VProgressBar/Ring, VDivider, Label, VEmptyState,
    │   │                             VComingSoon, VTopTabs, VBottomNav, PhoneFrame, VBackHeader
    │   ├── charts.tsx              ★ VDonut, VSparkline, VBars, VLegendDot
    │   └── mockups.tsx             device/preview mockups
    ├── src/app/components/ui/      generic shadcn primitives (button, card, dialog, … ~45 files)
    ├── src/app/screens/
    │   ├── Auth.tsx                Splash, Login(portal selector), TeacherFirstLogin,
    │   │                             ParentLinkChild, SchoolOnboarding (6-step wizard)
    │   ├── Admin.tsx               5-tab admin portal + AnnouncementDetail
    │   ├── Teacher.tsx             4-tab teacher portal (Home/Update/MyClasses/Profile)
    │   ├── Parent.tsx              4-tab parent portal (Home/Academics/Fees/Activity)
    │   ├── Discovery.tsx           school marketplace + AcademicCalendar
    │   └── Notifications.tsx       universal notification tray
    └── src/imports/                Figma exports + VidyaSetu_Frontend_Rebuild_Plan.md (legacy spec)
```

---

<a name="step-2"></a>
## 2. Step 2 — Database Schema

Source of truth: `/supabase_schema` (Postgres/Supabase DDL) — **11 tables**, mirrored by Exposed
objects in `server/.../db/Tables.kt`. Domains and relationships below.

### 2.1 Tables by domain

| Domain | Tables |
|---|---|
| **Discovery / marketing** | `school_directory` |
| **School identity** | `schools` |
| **Identity / accounts** | `users`, `student_parent_link` |
| **Students** | `students` |
| **Academic data** | `daily_progress`, `academic_records`, `achievements`, `ai_reports` |
| **Fees** | `fee_structures`, `fee_payments` |

### 2.2 Relationships (plain text)

```
users (1) ──< student_parent_link >── (1) students        [parent ↔ child M:N link table]
schools (1) ──< students                                   [a student belongs to a school]
schools (1) ──< school_directory? (discovery mirror)       [public-facing listing]
students (1) ──< daily_progress                            [per-day status/updates]
students (1) ──< academic_records                          [marks/assessments]
students (1) ──< achievements                              [recognitions]
students (1) ──< ai_reports                                [generated narratives]
schools/students ──< fee_structures ──< fee_payments       [fee heads → payments]
```

### 2.3 What's present vs. what the design needs

**Present and load-bearing:** `users`, `schools`, `students`, `student_parent_link`,
`daily_progress`, `academic_records`, `fee_structures`, `fee_payments`. These power the **parent**
and **landing/discovery** experiences that the `shared/parent` and `shared/content` features already
consume.

**Missing tables the new design assumes (see Step 6 for full specs):**
`teachers`, `classes`, `sections`, `subjects`, `teacher_assignments`, `attendance` (discrete daily
records), `assessments` + `marks` (normalized vs. the freeform `academic_records`), `syllabus` /
`syllabus_log`, `homework` + `homework_submissions`, `announcements` (table — currently only a
route), `messages` / `message_threads`, `ptm` / `ptm_slots`, `fee_reminders`, `notifications`,
`reviews` (discovery), `enquiries` (admissions may partially cover).

> The exact column-by-column DDL of each existing table will be transcribed into the **Step 9
> handoff** section once Step 7 migrations are written, so the handoff reflects the *post-build*
> schema rather than a moving target.

---

<a name="step-3"></a>
## 3. Step 3 — Backend Routes (Ktor)

Routing files under `server/.../feature/*/`. Auth is JWT (`core/JwtConfig.kt`,
`core/SecurityModule.kt`); responses are wrapped by `core/ApiResponse.kt`. Grouped by domain.
**Flag legend:** ✅ LIVE · ⚠️ PARTIAL · ❌ MISSING (design needs it, no route).

| Domain | Routing file | Status | Notes |
|---|---|---|---|
| Auth + OTP | `auth/AuthRouting.kt`, `OtpService.kt`, `OtpAdminRouting.kt` | ✅ | Login, OTP request/verify; multi-provider delivery (Twilio, Msg91, Fast2SMS, WhatsApp Cloud, SMTP, Console). JWT issue. |
| App status / config | `config/AppStatusRouting.kt` | ✅ | App status / feature-flag style endpoint. |
| Landing / content | `content/LandingRouting.kt` | ✅ | Marketing/landing content consumed by `shared/content`. |
| School | `school/SchoolRouting.kt` | ✅/⚠️ | School read/listing; discovery-facing. |
| Onboarding | `onboarding/OnboardingRouting.kt` | ⚠️ | School setup wizard backend; class/section/subject/teacher linking needs audit against design Step-3/4/5. |
| Admissions / enquiry | `admissions/AdmissionRouting.kt` | ⚠️ | Admission CRM / enquiry intake — partial coverage of Discovery "Enquire". |
| Announcements | `announcements/AnnouncementRouting.kt` | ⚠️ | Route exists; backing table + delivery analytics need audit. |
| Parent | `user/ParentRouting.kt` | ✅ | Parent-scoped data (child, daily status, fees, etc.) — feeds `shared/parent`. |
| User details/profile | `user/UserDetailsRouting.kt`, `UserProfileRouting.kt` | ✅ | Account + profile. |
| **Teacher** | — | ❌ | **No routing file.** Entire teacher vertical absent (auth scope, classes, attendance/marks/syllabus/homework write). Gap G1. |
| Attendance (discrete) | — | ❌ | Daily mark/read endpoints absent. |
| Marks / assessments | — | ⚠️/❌ | Only via `academic_records`; no assessment-create + per-student entry + live class stats. |
| Syllabus log | — | ❌ | No chapter/coverage log endpoints. |
| Homework | — | ❌ | Not built. |
| Messaging (parent↔school) | — | ❌ | Not built. |
| PTM scheduling | — | ❌ | Not built. |
| Discovery SRI / reviews / compare | — | ❌ | COMING SOON per plan §15. |

> Per-route request/response shapes are catalogued in `vidya_prayag_api_spec.artifact.md`,
> `vidya_prayag_api_spec2.artifact.md`, and `parent_api_spec.artifact.md`. Step 7 will append the
> **new** teacher/attendance/marks/syllabus specs to the Step-9 handoff with full payloads + role
> checks.

---

<a name="step-4"></a>
## 4. Step 4 — Old Frontend Map (The Death Record)

Everything under `composeApp/src/commonMain/.../ui/` plus the two bottom-bar/navigation helpers.
This is what the new UI replaces. **`shared/` is NOT part of this — it stays.**

### 4.1 Entrypoint & navigation (KEEP the plumbing pattern, replace the screens)
- **`App.kt`** — `KoinContext` → `MainViewModel` (exposes `themeName`, `authState{isLoaded, token, role}`)
  → installs Coil image loader (Ktor fetcher, Supabase token-stripping cache mapper) → `VidyaPrayagTheme`
  → if `!isLoaded` show `SplashScreen`, else `NavGraph` with start = Landing / SchoolDashboard(ADMIN) /
  ParentDashboard(PARENT). **No TEACHER branch** (gap).
- **`navigation/NavGraph.kt`** — `Destination` sealed graph with **35** destinations; `NavHost` wires each
  to an old screen. Search/SchoolDetails are empty placeholders.
- **`navigation/AppNavigator.kt`** — `ProvideAppNavigator` CompositionLocal wrapper around `NavController`.

### 4.2 Theme (old) — to be replaced by warm `VTheme`
`ui/theme/`: `Color.kt`, `MidnightColors.kt`, `Theme.kt` (`VidyaPrayagTheme` with LIGHT/DARK/MIDNIGHT
Material3 schemes), `ThemeState.kt` (`AppTheme` enum + `LocalAppTheme`/`LocalThemeSwitcher`),
`PlatformAppearance.kt` (expect/actual status-bar). The new design supersedes these color values with
teal/navy/lavender + night.

### 4.3 Old components (`ui/components/`) — replaced by `V*`
`VidyaPrayagButton`, `VidyaPrayagCard`, `VidyaPrayagTopBar`, `VidyaPrayagSearchBar`,
`VidyaPrayagDrawer`, `VidyaPrayagBottomBar`, `SchoolBottomBar`, `ParentBottomBar`, `BaseScreen`,
`OnboardingComponents`, `ui/auth/AuthBottomSheet`.

### 4.4 Old screens by role

**Cross / auth:** `screens/SplashScreen.kt`, `screens/CommonLandingScreen.kt` (binds `LandingViewModel`),
`ui/auth/AuthBottomSheet.kt` (binds `AuthViewModel`).

**Admin (`screens/admin/`, 17 files):** `SchoolDashboardScreen` (→`SchoolDashboardViewModel`),
onboarding `InstitutionalBasicOB`/`BrandingInfoOB`/`AcademicInfoOB`/`LaunchInfoOB`
(→ matching OB ViewModels), `InstitutionalProfileScreen`, `AdmissionCRMDashboard`,
`SchoolAnnouncementsScreen`, `MessagesScreen`, `SchedulePTMScreen`, `AcademicCalendarScreen`,
`DailyAttendanceScreen`, `LeaveRequestsScreen`, `ResultsScreen`, `AnalyticsDashboardScreen`,
`StudentAnalyticsScreen`, `ClassPerformanceScreen`, `TeacherPerformanceScreen`, `SyllabusCoverageScreen`.

**Parent (`screens/parent/`, 14 files):** `ParentDashboardScreen` (→`ParentDashboardViewModel`),
`ChildBasicInfoScreen`, `YourPreferencesScreen`, `LocationRequestScreen`, `AllSetScreen`,
`TrackProgressScreen`, `ParentAnnouncementScreen`, `ParentMessageScreen`, `FeeScreen`,
`CareerPathScreen`, `ScholarshipsScreen`, `DailyStatusScreen`, `ParentReportsScreen`,
`ParentSchedulePTMScreen` (each → its matching parent ViewModel).

**Teacher:** none (no old teacher screens — consistent with the missing backend).

---

<a name="step-5"></a>
## 5. Step 5 — Master Connection Document (by role)

Maps **new-design screen → action → ViewModel (existing `shared/`) → backend route → DB tables → status**.
The ViewModels are the contract the new Compose screens re-bind to. **Binding status legend:** ✅ VM
exists & wired to live route · ⚠️ VM exists, route partial/local-only · ❌ no VM/route yet (build in Step 7).

### 5.1 Auth / shared
| New screen | Action | ViewModel (`shared`) | Route | Tables | Status |
|---|---|---|---|---|---|
| Splash | load auth+theme | `MainViewModel` | (token check) | `users` | ✅ |
| Login (portal selector) | login / OTP | `AuthViewModel` | `auth/*` | `users` | ✅ (Parent/Admin) ; Teacher ❌ |
| Landing | fetch landing content | `LandingViewModel` | `content/Landing` | `school_directory` | ✅ |

### 5.2 Parent portal (LIGHT) — VMs already registered in Koin
`ParentDashboardViewModel`, `FeeViewModel`, `ChildBasicInfoViewModel`, `YourPreferencesViewModel`,
`LocationRequestViewModel`, `CareerPathViewModel`, `ScholarshipsViewModel`, `DailyStatusViewModel`,
`ParentReportsViewModel`, `ParentSchedulePTMViewModel`, `ParentAnnouncementViewModel`,
`ParentMessageViewModel`, `TrackProgressViewModel`.

| New screen (Parent.tsx) | Action | ViewModel | Route | Tables | Status |
|---|---|---|---|---|---|
| Home — child hero/status | today status, switcher | `ParentDashboardViewModel`, `DailyStatusViewModel` | `user/Parent` | `students`,`daily_progress`,`student_parent_link` | ✅ |
| Academics — marks/syllabus | trends | `TrackProgressViewModel` | `user/Parent` | `academic_records`,`daily_progress` | ✅/⚠️ |
| Fees | balance, history | `FeeViewModel` | `user/Parent` | `fee_structures`,`fee_payments` | ✅ |
| Activity — feed | notifications | `ParentAnnouncementViewModel`,`ParentMessageViewModel` | `announcements`/(messages ❌) | `announcements`(⚠️) | ⚠️ |
| Reports | AI report | `ParentReportsViewModel` | (ai_reports) | `ai_reports` | ⚠️ COMING SOON |

### 5.3 Admin portal (DARK→warm) — VMs already registered
`SchoolDashboardViewModel`, onboarding `InstitutionalBasicOB/BrandingInfoOB/AcademicInfoOB/LaunchInfoOB`,
`InstitutionalProfileViewModel`, `AdmissionCRMViewModel`, `SchoolAnnouncementsViewModel`,
`MessagesViewModel`, `SchedulePTMViewModel`, `AcademicCalendarViewModel`, `LeaveRequestsViewModel`,
`DailyAttendanceViewModel`, `AnalyticsDashboardViewModel`, `StudentAnalyticsViewModel`,
`TeacherPerformanceViewModel`, `ClassPerformanceViewModel`, `SyllabusCoverageViewModel`, `ResultsViewModel`.

| New screen (Admin.tsx, 5 tabs) | ViewModel | Route | Tables | Status |
|---|---|---|---|---|
| Home dashboard | `SchoolDashboardViewModel`,`AnalyticsDashboardViewModel` | `school`,`onboarding` | `schools`,`students` | ⚠️ many VMs are local-only stubs |
| People (students/teachers) | `StudentAnalyticsViewModel`,`TeacherPerformanceViewModel` | (students ✅ / teachers ❌) | `students` / `teachers`❌ | ⚠️ |
| Records (attendance/marks/syllabus/fee/docs) | `DailyAttendanceViewModel`,`ResultsViewModel`,`SyllabusCoverageViewModel` | attendance❌/marks⚠️/syllabus❌ | new tables (Step 6) | ⚠️/❌ |
| Comms (announcements/messages/PTM/notif) | `SchoolAnnouncementsViewModel`,`MessagesViewModel`,`SchedulePTMViewModel` | announcements⚠️/messages❌/ptm❌ | new tables | ⚠️/❌ |
| Settings (onboarding wizard) | OB ViewModels + `InstitutionalProfileViewModel` | `onboarding` | `schools` | ⚠️ |

### 5.4 Teacher portal (DARK→warm) — **NO VMs yet → Step 7**
| New screen (Teacher.tsx, 4 tabs) | ViewModel | Route | Status |
|---|---|---|---|
| Home — today's tasks/periods | `TeacherDashboardViewModel`* | `teacher/*`* | ❌ build |
| Update — Attendance/Marks/Syllabus/Homework | `TeacherAttendanceVM`*, `TeacherMarksVM`*, `TeacherSyllabusVM`*, `TeacherHomeworkVM`* | `teacher/*`* | ❌ build |
| My Classes / Class detail | `TeacherClassesVM`* | `teacher/classes`* | ❌ build |
| Profile | `TeacherProfileVM`* | `teacher/profile`* | ❌ build |

`*` = to be created in the new `shared/feature/teacher` vertical + backend `feature/teacher` routes.

### 5.5 Discovery (LIGHT)
| New screen (Discovery.tsx) | ViewModel | Route | Tables | Status |
|---|---|---|---|---|
| Search / filter / school cards | `LandingViewModel` + `GetSchoolsUseCase` | `school`,`content/Landing` | `school_directory`,`schools` | ✅ list ; SRI/reviews ❌ |
| School profile | (schools VM) | `school` | `schools` | ⚠️ |
| Enquiry | `AdmissionCRMViewModel` | `admissions/Admission` | (enquiries) | ⚠️ |
| Compare / SRI | — | — | — | ❌ COMING SOON |

---

<a name="step-6"></a>
## 6. Step 6 — Gap Analysis (new design vs. backend)

Each gap: **what it does · endpoint(s) needed · request/response · tables · scope rule.**

### G1 — Teacher vertical (highest priority)
- **Does:** teacher auth (`schoolcode.teachercode`, force password change on first login), sees only
  assigned classes; marks attendance, enters marks, logs syllabus, assigns homework.
- **Endpoints:** `POST /teacher/login`, `POST /teacher/password` (first-login change),
  `GET /teacher/me`, `GET /teacher/classes`, `GET /teacher/classes/{id}/students`,
  `POST /teacher/attendance` (`{classId, date, entries:[{studentId,status}]}` → summary),
  `POST /teacher/assessments` + `POST /teacher/marks`, `POST /teacher/syllabus-log`,
  `POST /teacher/homework`.
- **Tables:** `teachers`, `teacher_assignments`(class+subject scope), `attendance`,
  `assessments`+`marks`, `syllabus_log`, `homework`+`homework_submissions`.
- **Scope rule:** every teacher endpoint filters by `teacher_id` ∈ JWT and the requested `class_id`
  must be in `teacher_assignments` for that teacher; reject otherwise (403). A teacher can read
  attendance for any student in an *assigned* class but only marks for their *own* subject.

### G2 — Structural school skeleton (classes/sections/subjects)
- **Does:** onboarding Steps 3–4 — define classes, sections, subjects (subject is per class+section).
- **Endpoints:** `POST /school/classes`, `POST /school/sections`, `POST /school/subjects`,
  read counterparts; `POST /school/students/bulk` (CSV) with validation report.
- **Tables:** `classes`, `sections`, `subjects` (each `subject` FK→`class`+`section`).
- **Scope rule:** admin-only, scoped to JWT `school_id`; one school can never read another's.

### G3 — Attendance (discrete, immutable)
- **Endpoints:** `GET /attendance?classId&date`, `POST /attendance` (locks on submit → read-only;
  edits create a logged correction request). **Tables:** `attendance`,
  `attendance_correction_requests`.

### G4 — Marks/assessments (normalized + live stats)
- **Endpoints:** `POST /assessments`, `POST /assessments/{id}/marks`, `GET .../stats`
  (avg/median/high/low). **Tables:** `assessments`, `marks`.

### G5 — Syllabus log · G6 — Homework · G7 — Messaging · G8 — PTM · G9 — Announcements table ·
### G10 — Notifications · G11 — Discovery SRI/reviews/compare
Each documented with endpoints + tables in the same shape; G11 + AI reports + Razorpay + PEWS remain
**COMING SOON** (render polished placeholders via `VComingSoon`, never wire to a non-existent route).

> **Hard rule for the UI rebuild:** a screen whose route is ❌ renders a `VComingSoon`/empty state,
> never a fake success or random data (design Appendix A/B).

---

<a name="design-system"></a>
## 7. Design System — Authoritative Tokens (from `src/styles/theme.css`)

These are the values the new Compose `VTheme` must encode. **Three families + semantics + night.**

### 7.1 Color tokens
```
Brand
  teal        #3cb9a9    teal-deep #006a60    (primary accent / CTAs / active)
  navy        #26234d    navy-deep #1a1838    (primary solid, headers)
  lavender    #fcf8ff    (app background, light/parent)
  cream       #f5f5f3    (input/secondary surface)
  warm-orange #9e421a    (sparing warm accent)

Ink (text)
  ink #1a2422 · ink-2 #3d4947 · ink-3 #6d7a77 · placeholder #bcc9c6

Semantic (data states only — never branding)
  success #A8E6CF  (ink #1f7a4d)    warning #FFD4A3 (ink #b3651a)    danger #FFADA8 (ink #b3261e)

Surfaces (light)
  card #FFFFFF · border-1 rgba(8,8,8,.06) · border-2 rgba(8,8,8,.10)
  shadow-1 0 1px 3px rgba(38,35,77,.05) · shadow-2 0 8px 24px rgba(38,35,77,.08) · shadow-3 0 16px 40px rgba(38,35,77,.14)

Night (.theme-night)
  bg #050505 · surface card #0e0e10 · tinted #141416 · ink #f4f4f6 / #b9bcc4 / #7a7e89
  teal #3cd1be · navy→#f3f0ff (inverted) · warm-orange #ffb37a
```

### 7.2 Type, radius, spacing, motion
```
Fonts:   Plus Jakarta Sans (UI, ss01/ss02), DM Mono (all numbers/IDs, tabular)
Scale:   h1 32/800/-.02em · h2 22/700 · h3 17/700 · h4 14/600 · body 14/400 · label 11/600 UPPER .08em
Radius:  sm 6 · md 10 · lg 14 · xl 20 · pill 999 · avatar 50%
Spacing: 4/8/16/24/32/48/64
Motion:  micro 150ms ease-out · list stagger 30ms spring · screen 320ms spring(300,30) · sheet 380ms spring(250,28)
```

### 7.3 Portal theming rule
Admin & Teacher were "dark" in the original plan; the evolved system renders them as the **warm
light** aesthetic (`.warm` scope) with teal-deep accents, and offers a true **night** mode. Parent &
Discovery are **light** (lavender). The Compose `VTheme` exposes `VPortalTone { Light, Warm, Night }`
plus the full token set as `LocalVColors`/`LocalVType`/`LocalVDimens`.

---

<a name="components"></a>
## 8. `V*` Component Library → Compose Mapping

Exact source: `src/app/components/v/{primitives,charts}.tsx`. Each React component maps 1:1 to a
Compose `@Composable` in the new `ui/v2/components` package. Behaviour notes captured from the source.

| React (`V*`) | Compose target | Key behaviour to preserve |
|---|---|---|
| `VLogo` | `VLogo` | bridge SVG → Canvas/VectorPainter; arc + deck + cables + pillars; wordmark with teal "S"; tones ink/white/teal/navy |
| `VCard` | `VCard` | white surface, 16px radius, border-1, shadow-1; `padded` flag |
| `VButton` | `VButton` | variants primary/secondary/ghost/destructive; **8 tones** (navy/teal/sky/peach/lavender/sand/rose/mint); `soft` (tinted) vs filled; sizes sm/md/lg; **stateful** idle→loading(spinner)→success(check); filled primary has sheen sweep |
| `VInput` | `VInput` | label/hint/icon; focus → white bg + teal-deep border + 4px teal glow ring; 12px radius |
| `VBadge` | `VBadge` | pill; tones arctic/success/warning/danger/neutral |
| `VTag` | `VTag` | filter chip; active = teal tint + border + shadow |
| `VAvatar` | `VAvatar` | initials fallback w/ deterministic palette by name hash; optional ring; image via Coil |
| `VStatusDot` | `VStatusDot` | 8px dot; optional pulse |
| `VProgressBar` | `VProgressBar` | 6px track, tone color fill |
| `VProgressRing` | `VProgressRing` | stroke-dash donut; center % in DM Mono |
| `VDivider` / `Label` / `VEmptyState` / `VComingSoon` | same | dividers, ALL-CAPS label, designed empty states, "PREVIEW + Notify me" placeholder |
| `VTopTabs` | `VTopTabs` | scrollable; active = teal-deep + underline indicator |
| `VBottomNav` | `VBottomNav` | white, top border, items with icon+label+badge; active teal-deep |
| `PhoneFrame` | `VScreenScaffold` | max-width 440 stream, lavender bg; `dark`→warm scope |
| `VBackHeader` | `VBackHeader` | circular back, centered title, optional action |
| `VDonut` (charts) | `VDonut` | animated multi-segment donut via Canvas; center slot |
| `VSparkline` | `VSparkline` | area+line via Path; animated draw |
| `VBars` | `VBars` | vertical bars; last bar highlighted + value label |
| `VLegendDot` | `VLegendDot` | dot + label + DM-Mono value |

**Icons:** plan specifies Phosphor; the repo already bundles `material-icons-extended`. Decision:
use Material extended (already a dependency, multiplatform-safe) mapped to the closest Phosphor
equivalents, kept in a single `VIcons` object so the set stays consistent (Appendix B rule).

---

<a name="step-7"></a>
## 9. Step 7 Plan — Build the Missing Backend

Order (each built + tested before the next, with role/scope checks):
1. **Schema migrations:** add `teachers`, `teacher_assignments`, `classes`, `sections`, `subjects`,
   `attendance`, `assessments`, `marks`, `syllabus_log`, `homework`, `homework_submissions`,
   `announcements`, `messages`, `ptm`, `notifications` (+ correction-request audit tables). Update
   `server/db/Tables.kt` and `supabase_schema`.
2. **`feature/teacher` routing** (G1) with JWT scope guard middleware.
3. **Attendance** (G3) immutable write + correction request.
4. **Marks/assessments** (G4) with live stats.
5. **Syllabus log** (G5) + parent-notification draft.
6. **Homework** (G6), **Messaging** (G7), **PTM** (G8), **Announcements table** (G9), **Notifications** (G10).
7. Mark each route LIVE in this document's Step-5 tables as it lands.
**Scope tests (design Appendix C / §16):** school isolation, teacher-class isolation,
parent-own-child isolation, data immutability.

---

<a name="step-8"></a>
## 10. Step 8 Plan — Delete Old UI, Swap to `ui/v2`

1. Author `ui/v2/theme` (`VTheme` + tokens) and `ui/v2/components` (all `V*`), **build-verify**.
2. Add `shared/feature/teacher` ViewModels + register in `Koin.kt`.
3. Build `ui/v2/screens/{auth,admin,teacher,parent,discovery,shared}` re-binding to existing VMs.
4. New `ui/v2/navigation` graph (portal-aware: Admin 5-tab, Teacher 4-tab, Parent 4-tab, Discovery).
5. Point `App.kt` at the v2 root; **delete** `composeApp/.../ui/` (old theme/components/auth/screens)
   and stale `navigation/NavGraph.kt` destinations; rename `ui/v2` → `ui`.
6. `./gradlew :composeApp:compileKotlinJvm` (+ wasm/js/android) green; server still runs.

> **Multiplatform constraint:** all new UI lives in `commonMain` and must compile on
> android/ios/jvm/js/wasmJs — no platform-only APIs; SVG/charts via Compose `Canvas`, images via Coil3.

---

<a name="step-9"></a>
## 11. Step 9 — Handoff Summary (living section)

To be completed as Steps 7–8 land. Will contain: (a) the **post-migration schema** (every table,
column, type, FK, index); (b) the **full endpoint catalogue** with request/response + auth/role;
(c) the **role → action → endpoint** capability matrix; (d) the **deferred / COMING SOON** list with
reasons (SRI, PEWS, AI reports, Razorpay, online PTM, UDISE export, co-admin, iOS, alumni).

**Deferred today (reason):** SRI score (algorithm unbuilt), PEWS (model untrained), AI report cards
(LLM integration pending), Razorpay (gateway pending), online PTM video (API pending), compliance
export (format mapping pending), co-admin (multi-admin pending), alumni (long-term).

---

*Authored from a full read of `main` (incl. merged `backend-by-abuzar`) and the `UI screens/` design
source. Where the legacy `VidyaSetu_Frontend_Rebuild_Plan.md` and the code disagree, the code is
authoritative.*
