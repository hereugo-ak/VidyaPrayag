# Backend / Supabase Schema Gap List — `ui/v2` Screen Parity

This document answers the explicit request: *"if those new screens don't have proper backend API /
Supabase schema then give me [the list]."*

It enumerates every screen ported from the React design (`UI screens/src/app/screens/*.tsx`) into the
Compose-Multiplatform `ui/v2` layer, and states whether a **real backend API + Supabase schema**
already backs it. Where a gap exists, the screen still ships with the **exact React look/feel**, but —
per the project's hard UI rule — it renders only real (or locally-held) state and **never fabricates
server data**. Each gap lists the endpoint(s) + tables a backend engineer must add to "light it up".

Legend: ✅ backed by a real API · ⚠️ partially backed · ❌ no API / schema yet

> **Two-way audit (updated):** Sections 1–3 cover *screens that lack a backend*. **Sections 4–5 cover
> the reverse — backend APIs/features that lack a screen** — and were produced by reading the actual
> Kotlin code (`*Api.kt`, repositories, and every `koinViewModel()` binding), not by reading docs.

---

## 1. Screens that ARE fully backed (no gap)

| Screen (v2) | React source | Endpoint(s) | ViewModel |
|---|---|---|---|
| `SchoolOnboardingScreenV2` | `Auth.tsx → SchoolOnboarding` | ✅ `POST /api/v1/onboarding/submit` (`ob_step_type` = BASIC / BRANDING / ACADEMIC + final) | `InstitutionalBasicOB` / `BrandingInfoOB` / `AcademicInfoOB` / `LaunchInfoOB` |
| `AcademicCalendarScreenV2` | `Discovery.tsx → AcademicCalendar` | ✅ `GET /api/v1/school/calendar` | `AcademicCalendarViewModel` |
| `LoginScreenV2` | `Auth.tsx → Login` | ✅ `auth/check-user`, `auth/login`, `auth/signup`, `auth/send-otp`, `auth/verify-otp` | `AuthViewModel` |

---

## 2. Screens with BACKEND GAPS (built to spec, awaiting API)

### 2.1 ❌ `ParentLinkChildScreenV2` — Parent → Child linking
**React source:** `Auth.tsx → ParentLinkChild` (3-step wizard)
**Current binding:** `ChildBasicInfoViewModel`, `YourPreferencesViewModel` — these are **local-state
only** (no `submit()`, no repository call).

**Missing API:**
- `POST /api/v1/parent/link-child` — body `{ child_name, grade, dob, roll_or_admission_no, school_id, interests[] }`
- `GET  /api/v1/parent/schools/search?q=<name>` — to power the "Find your child's school" match card
- `GET  /api/v1/parent/children` — list of already-linked children (for "Add another child")

**Missing Supabase schema:**
- `parent_child_links` — `(id, parent_user_id FK, student_id FK, status, created_at)`
- `student_admission_index` (or a searchable view) keyed by `(school_id, roll_no / admission_no)` so a
  parent's entry can be matched to a real student record.
- `parent_preferences` — `(parent_user_id, preference_key, budget_min, budget_max)` to persist the
  step-2 "what matters to you" selection (currently held only in `YourPreferencesViewModel`).

**Behaviour until then:** the wizard collects input and routes to Login (to obtain a real session). It
does not persist. Wire a `submit { onDone() }` into step 3 when the link endpoint lands.

---

### 2.2 ❌ `TeacherFirstLoginScreenV2` — one-time password change
**React source:** `Auth.tsx → TeacherFirstLogin`
**Current binding:** none — `AuthViewModel` only supports login / OTP / signup. No change-password.

**Missing API:**
- `POST /api/v1/auth/change-password` — body `{ current_password, new_password }`, auth required.

**Missing Supabase schema:**
- `users.must_change_password BOOLEAN DEFAULT false` — set `true` when an admin provisions a teacher
  with a temporary password; cleared on first successful change. **This flag is also what should gate
  the screen** — without it the app cannot know *when* to force first-login, so the screen is currently
  built and available but **not** inserted as a mandatory step in the auth flow.

**Behaviour until then:** the screen validates locally (current present · new ≥ 8 chars · confirm
matches) and continues. It does not persist a new password. When `change-password` + the flag land,
swap the local `validate()` for `viewModel.changePassword(...) { onDone() }` and gate it on the flag in
`NavGraphV2`.

---

### 2.3 ❌ `NotificationsScreenV2` — notification inbox
**React source:** `Notifications.tsx → NotificationsScreen` (was driven entirely by `lib/mock`)
**Current binding:** none — there is **no notifications API or table** anywhere in `shared/`.

**Missing API:**
- `GET   /api/v1/notifications?filter=all|unread` — returns `[{ id, category, title, body, time, unread }]`
  (categories: `attendance` | `academic` | `fees` | `announcement`).
- `POST  /api/v1/notifications/{id}/read`
- `POST  /api/v1/notifications/read-all`
- `GET/PUT /api/v1/notifications/preferences` — backs the "Notification preferences" footer.

**Missing Supabase schema:**
- `notifications` — `(id, user_id FK, category, title, body, created_at, read_at NULLABLE)`
- `notification_preferences` — `(user_id, category, channel, enabled)`
- (optional) a `notification_events` source feeding the above from attendance / fee / announcement
  domain events.

**Behaviour until then:** the screen renders the **exact** React layout (navy→indigo "Inbox" hero,
all/unread filter pills, category-tinted item cards, "all caught up" empty state, preferences footer)
but is driven by an **empty** list, so it always shows the genuine "You're all caught up" state. When
`GET /api/v1/notifications` lands, pass the real list (or bind a `NotificationsViewModel`) into the
screen's `items` param and the whole layout lights up unchanged.

---

## 3. Partial gaps on already-shipped portal screens (context)

| Screen | Status | Note |
|---|---|---|
| `AcademicCalendarScreenV2` | ⚠️ | Backend has no per-event **type** (academic/holiday/deadline) or a conflicts count; the React colored dots were mock. We render every event with the single arctic accent. Add `calendar_events.event_type` to restore typed dots. A `/holidays` endpoint exists server-side and can power a dedicated filter later. |
| `DiscoveryScreenV2` | ⚠️ | Map / nearby-by-geo is gap **G11** — rendered as `VComingSoon`, never fabricated. |

---

## 4. REVERSE GAP — backend APIs / features that have **NO `ui/v2` screen**

> **This section was produced by reading the actual Kotlin code** — every `*Api.kt` under
> `shared/.../data/remote`, every repository, and every `*ViewModel` — and cross-referencing it
> against the real list of `ui/v2/screens/*.kt` files and the `koinViewModel()` bindings inside each
> screen. It is **not** derived from any prior document.
>
> **How it was verified:**
> - All built v2 screens: `find ui/v2/screens -name "*.kt"` → 24 screens (+ `Shared.kt`).
> - VMs each screen binds: grepped `koinViewModel()` / typed `: XxxViewModel` in every screen file.
> - VMs that exist: `find shared -name "*ViewModel.kt"` → 42 VMs.
> - **VMs that exist but are bound by NO v2 screen** (`comm -13` of the two lists) → the 16 below.
> - For each, the backing repository/API was confirmed by grepping the VM body for `*Repository`
>   and then reading the matching `*Api.kt` for the exact route.

These backend features are **fully implemented end-to-end** (API client + repository + ViewModel +,
in most cases, server route under `server/`) yet there is **no screen in `ui/v2` that surfaces them**.
They are the inverse of Section 2: *backend without UI*.

### 4.1 School / Admin features with a complete backend but NO v2 screen

| Backend feature | ViewModel (exists, unbound) | API endpoint(s) — verified in code | Repository |
|---|---|---|---|
| **Admissions CRM** (enquiry pipeline) | `AdmissionCRMViewModel` | `GET/POST /api/v1/admissions/enquiries`, `GET .../enquiries/summary`, `PATCH .../enquiries/{id}/status` | `AdmissionRepository` |
| **Daily Attendance register** | `DailyAttendanceViewModel` | `GET /api/v1/school/attendance/daily` | `AttendanceRepository` |
| **Class performance analytics** | `ClassPerformanceViewModel` | `GET /api/v1/school/analytics/class-performance` | `AnalyticsRepository` |
| **Teacher performance analytics** | `TeacherPerformanceViewModel` | `GET /api/v1/school/analytics/teacher-performance` | `AnalyticsRepository` |
| **Leave requests** (approve/reject) | `LeaveRequestsViewModel` | `GET/POST /api/v1/school/leave-requests`, `PATCH .../{id}/status` | `LeaveRequestsRepository` |
| **Messages / threads** (school inbox) | `MessagesViewModel` | `GET /api/v1/school/messages/threads`, `GET .../threads/{id}/messages`, `POST .../threads/{id}/read`, `POST /api/v1/school/messages` | `MessagesRepository` |
| **Results publishing** | `ResultsViewModel` | `GET /api/v1/school/results`, `POST /api/v1/school/results` | `ResultsRepository` |
| **Schedule PTM** (school side) | `SchedulePTMViewModel` | `GET /api/v1/school/ptm`, `POST /api/v1/school/ptm` | `PtmRepository` |

> Note: `StudentAnalyticsViewModel` (cohort/`student/{id}`) and `SyllabusCoverageViewModel` **are**
> bound — by `SchoolPeopleScreenV2` and `SchoolRecordsScreenV2` respectively — so the Analytics and
> Syllabus-coverage endpoints are partly surfaced; only `class-performance` and `teacher-performance`
> remain screen-less.
> Note: the **Media upload** API (`POST /api/v1/school/media/upload`, `DELETE .../media`) has a working
> `MediaApi` but **no ViewModel and no screen** — it is intended to be consumed by the onboarding /
> profile gallery flows.

### 4.2 Parent features with a backend but NO v2 screen

| Backend feature | ViewModel | API endpoint | Repository |
|---|---|---|---|
| **Scholarships** | `ScholarshipsViewModel` | `GET /api/v1/parent/scholarships` | `ParentRepository` |

> `track-progress`, `fees`, and `announcements` parent endpoints **are** surfaced
> (`ParentAcademicsScreenV2` → `TrackProgressViewModel`, `ParentFeesScreenV2` → `FeeViewModel`,
> `ParentActivityScreenV2` → `ParentAnnouncementViewModel`). Only **scholarships** has a real endpoint
> with no screen consuming it.

### 4.3 ViewModels that are NOT a backend gap (local-only, no API — leftover from old design)

These appear in the "unbound VM" list too, but they have **no repository / no API** — they are pure
local-state holders carried over from the old UI. They are *screens-without-backend-AND-without-UI*;
listed here only so the audit is exhaustive and nobody wires a non-existent endpoint:

| ViewModel | Reality (read from code) |
|---|---|
| `CareerPathViewModel` | Hardcoded `CareerPathState` (e.g. "Aerospace Engineering 98%"). No repo. |
| `ParentReportsViewModel` | Hardcoded `ParentReportsState` sample report card. No repo. |
| `ParentMessageViewModel` | Local-only message composer state. No repo. |
| `ParentSchedulePTMViewModel` | Local-only PTM-booking form state. No repo. |
| `LocationRequestViewModel` | Local-only location-permission UX state. No repo. |
| `LandingViewModel` / `MainViewModel` | App-shell / landing bootstrap; not feature screens. |

---

## 5. SCREENS that have **NO backend** (built to spec, local-state only) — code-verified

Confirmed by reading each screen's bound ViewModel and checking it has **no `*Repository`** reference:

| Screen (v2) | Bound VM | Backend status (verified) |
|---|---|---|
| `ParentLinkChildScreenV2` | `ChildBasicInfoViewModel`, `YourPreferencesViewModel` | ❌ both VMs are `MutableStateFlow`-only, no repo. See gap 2.1. |
| `TeacherFirstLoginScreenV2` | *(none — local `validate()`)* | ❌ no change-password API. See gap 2.2. |
| `NotificationsScreenV2` | *(none — `items` param, default empty)* | ❌ no notifications API/table. See gap 2.3. |
| `SchoolOnboardingScreenV2` (step state) | 4× `*OBViewModel` | ⚠️ `submit()` hits `POST /onboarding/submit`, but per-field `DailyStatusViewModel`-style local copies are kept client-side until submit. Backed. |

All other v2 screens (`Login`, `Discovery`, `ParentHome/Academics/Fees/Activity`,
`SchoolHome/People/Records/Comms/Settings`, all 8 `Teacher*`) bind a ViewModel that **does** call a
repository → API, so they are backed.

---

## 6. Summary for the backend team (TL;DR)

### A. Screens that need a NEW backend (UI exists, API missing)
1. **Parent linking** — `parent_child_links`, `parent_preferences`, `student_admission_index` +
   `POST /parent/link-child`, `GET /parent/schools/search`, `GET /parent/children`.
2. **Teacher first login** — `users.must_change_password` + `POST /auth/change-password`.
3. **Notifications** — `notifications`, `notification_preferences` +
   `GET /notifications`, `POST /notifications/{id}/read`, `POST /notifications/read-all`,
   `GET/PUT /notifications/preferences`.
4. **Calendar event typing** (nice-to-have) — `calendar_events.event_type`.

### B. Backends that need a NEW screen (API exists, UI missing) — **build these next**
These already have API + repository + ViewModel; they only need a `ui/v2` screen built to the React
look/feel and wired to the existing VM (no backend work required):

1. **Admissions CRM** → `AdmissionCRMViewModel` (`/admissions/enquiries*`)
2. **Daily Attendance register** → `DailyAttendanceViewModel` (`/school/attendance/daily`)
3. **Class performance analytics** → `ClassPerformanceViewModel` (`/analytics/class-performance`)
4. **Teacher performance analytics** → `TeacherPerformanceViewModel` (`/analytics/teacher-performance`)
5. **Leave requests** → `LeaveRequestsViewModel` (`/school/leave-requests*`)
6. **Messages / threads** → `MessagesViewModel` (`/school/messages*`)
7. **Results publishing** → `ResultsViewModel` (`/school/results`)
8. **Schedule PTM (school)** → `SchedulePTMViewModel` (`/school/ptm`)
9. **Scholarships (parent)** → `ScholarshipsViewModel` (`/parent/scholarships`)
10. **Media upload** → `MediaApi` (needs a thin VM + screen; `/school/media/upload`)

Every screen in Section 2/5 is already built on Compose Multiplatform with the exact v2 design tokens,
so each Section-A gap is a pure "wire the data" task. Every Section-B item is a pure "build the screen"
task against an already-working API.
