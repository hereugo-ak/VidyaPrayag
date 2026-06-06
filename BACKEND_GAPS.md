# Backend / Supabase Schema Gap List — `ui/v2` Screen Parity

This document answers the explicit request: *"if those new screens don't have proper backend API /
Supabase schema then give me [the list]."*

It enumerates every screen ported from the React design (`UI screens/src/app/screens/*.tsx`) into the
Compose-Multiplatform `ui/v2` layer, and states whether a **real backend API + Supabase schema**
already backs it. Where a gap exists, the screen still ships with the **exact React look/feel**, but —
per the project's hard UI rule — it renders only real (or locally-held) state and **never fabricates
server data**. Each gap lists the endpoint(s) + tables a backend engineer must add to "light it up".

Legend: ✅ backed by a real API · ⚠️ partially backed · ❌ no API / schema yet

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

## 4. Summary for the backend team (TL;DR)

Add these to unblock full parity:

1. **Parent linking** — `parent_child_links`, `parent_preferences`, `student_admission_index` +
   `POST /parent/link-child`, `GET /parent/schools/search`, `GET /parent/children`.
2. **Teacher first login** — `users.must_change_password` + `POST /auth/change-password`.
3. **Notifications** — `notifications`, `notification_preferences` +
   `GET /notifications`, `POST /notifications/{id}/read`, `POST /notifications/read-all`,
   `GET/PUT /notifications/preferences`.
4. **Calendar event typing** (nice-to-have) — `calendar_events.event_type`.

Every screen above is already built on Compose Multiplatform with the exact v2 design tokens, so each
gap is a pure "wire the data" task on the client once the endpoint exists.
