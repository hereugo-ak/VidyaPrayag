# VidyaPrayag — School Side Status Report

> **Branch analyzed:** `main`
> **HEAD at audit time:** `0f4f865` (`Merge pull request #8 from hereugo-ak/fix/school-session-onboarding`)
> **Audit date:** 2026-06-03
> **Scope:** School/Admin side across Compose UI (`composeApp`), shared KMP client code (`shared`), Ktor backend (`server`), recent Android Studio logs/screenshots, and uploaded Supabase schema (`Vidyasetu schema.txt`).
> **Purpose:** Capture every known school-side issue before the next fixing pass, including frontend glitches, backend/API mismatches, Supabase schema mismatches, Android dev-network confusion, and parent-school communication gaps.

---

## 0. Executive summary from the latest screenshots/logs

The project **compiles**, but the latest screenshots prove the school side is still not production-complete. The biggest problems are not one single crash; they are a mix of **API contract mismatches**, **deployed-backend drift**, **missing Supabase tables**, **placeholder media/location workflows**, and **parent-school data not sharing the same source of truth**.

### P0 issues discovered in this audit

| Priority | Area | Evidence | Current result | Required fix |
|---|---|---|---|---|
| P0 | Academic Calendar API contract | Screenshot: `Illegal input: Field 'working_days' is required... at $.data.summary`; code: client expects `working_days`, server sends `total_working_days` | Calendar shows an error and no usable data | Align DTO/response. Prefer server returning `working_days` and optionally `total_working_days` for backward compatibility, or client accepting `total_working_days`. |
| P0 | Messages conversation endpoint | Screenshot: `Endpoint not found: /api/v1/school/messages/threads/{id}/messages` | Thread list can show, but opening Admin Desk conversation can fail | Ensure deployed backend includes `GET /api/v1/school/messages/threads/{id}/messages`; redeploy backend after `c81aebc`, and add this endpoint to API docs/Postman guide. |
| P0 | Deployed backend / local backend drift | Build logs print: `devBaseUrl NOT set ... dev flavor will use https://vidyaprayag-1.onrender.com` | Phone may hit Render while developer expects laptop backend; token/server/schema may not match | Add `devBaseUrl=http://<laptop-LAN-ip>:8080` to local `local.properties`, rebuild app, and verify `/api/v1/config/app-status` from the phone browser. |
| P0 | Supabase schema mismatch | Uploaded schema has no `faculty` or `holiday_list`; server uses both | Attendance faculty, teacher analytics, holidays, calendar summaries can fail on production Supabase if those tables are missing | Apply supplementary SQL / add `faculty` and `holiday_list` tables to production schema. |
| P0 | Parent-school communication not harmonized | School announcements/messages use school routes; parent announcements/messages are static or separate | School can create announcements/messages that parents do not actually receive/read | Rewire parent announcements/messages/PTM/fees to the same school-scoped operational tables. |

---

## 1. Recent commit review — last 10 commits

| Commit | Summary | School-side impact | Audit note |
|---|---|---|---|
| `0f4f865` | Merge PR #8 | Brings school session/onboarding fixes to `main` | Current HEAD. |
| `bfca368` | Fix school onboarding session handling | Redacts Authorization headers, clears session on onboarding 401, removes some expiring image usage | Good security/UX fix, but global session handling is still not complete. |
| `0e33daf` | Merge PR #7 | Merges school/backend work | Baseline for newer school fixes. |
| `03ede1b` | Replace placeholder analytics with real data-driven charts & aggregation | Improves analytics routes and UI | Still hybrid; some metrics remain CMS/template-derived. |
| `3fa0be2` | Add detailed school-side status report | Created earlier report | This file is now updated with latest screenshot/schema findings. |
| `c81aebc` | Wire conversation view end-to-end; diagnose dev backend/401 | Adds message conversation client/server route | Screenshot still shows endpoint not found, meaning deployed backend/device target likely does not contain this commit or route is not documented/tested. |
| `da1fe3c` | Enforce school authorization + remove placeholders across school surface | Adds school scoping and many route implementations | Strong improvement, but some parent-facing surfaces remain disconnected. |
| `6489ada` | Persist real academic structure + unify completion logic | Onboarding academic structure now persists | Core academic onboarding is more real. |
| `90707b9` | Android build parser helper fix | Build hygiene | No product change. |
| `18fabe6` | Merge PR #6 | Older backend merge | Historical. |

Additional recent school-side commits in history also matter:

- `aafe10e` — announcement filters + CRM report navigation.
- `77a65b7` — academic calendar nav + dashboard support actions.
- `48d16f2` — PTM scheduling dialog + enquiry status dropdown.

---

## 2. Validation performed in this audit

### 2.1 Backend compile

Command run:

```bash
./gradlew :server:compileKotlin -Pserver-only=true --no-daemon
```

Result: **[Working] BUILD SUCCESSFUL**.

### 2.2 Android/KMP compile

The old report command `:shared:compileKotlinAndroid` is now invalid because Gradle reports it as ambiguous. Correct command run:

```bash
./gradlew :shared:compileDevDebugKotlinAndroid :composeApp:compileDevDebugKotlinAndroid --no-daemon
```

Result: **[Working] BUILD SUCCESSFUL**.

Important non-blocking warnings observed:

- `devBaseUrl NOT set ... dev flavor will use https://vidyaprayag-1.onrender.com`.
- Kotlin/Native target/version warnings.
- AGP deprecated API warnings.
- Room schema export warning.
- Compose icon/menu deprecation warnings.

These warnings do not block compilation, but the `devBaseUrl` warning directly explains why the real phone may hit Render instead of the laptop backend.

---

## 3. Architecture and connection map

The codebase is Kotlin Multiplatform, not Flutter:

- `composeApp/` — Compose Multiplatform UI screens and navigation.
- `shared/` — KMP ViewModels, repositories, DTOs, Ktor API clients, preferences, Koin DI.
- `server/` — Ktor backend routes, DB access, JWT auth, and school-scoped endpoints.
- Supabase/Postgres schema is consumed via Exposed table mappings in `server/src/main/kotlin/.../db/Tables.kt`.

### 3.1 School client-side route registration

School/admin screens registered in `NavGraph.kt` include:

- School dashboard.
- Onboarding: Institutional Basics, Branding, Academic, Launch/Review.
- Institutional profile.
- Admission CRM.
- School announcements.
- Direct messages.
- Schedule PTM.
- Academic calendar.
- Daily attendance.
- Leave requests.
- Results.
- Analytics overview, student analytics, teacher performance, class performance, syllabus coverage.

### 3.2 Backend endpoint inventory for school side

| Area | Backend route file | Endpoints/status |
|---|---|---|
| User details/dashboard state | `feature/user/UserDetailsRouting.kt` | `GET /api/v1/user/details` |
| Onboarding | `feature/onboarding/OnboardingRouting.kt` | `GET /api/v1/onboarding/step`, `GET /api/v1/onboarding/academic/class-details`, `POST /api/v1/onboarding/submit` |
| School dashboard | `feature/school/SchoolDashboardRouting.kt` | `GET /api/v1/school/dashboard` |
| Calendar/holidays/attendance | `feature/school/SchoolRouting.kt` | `GET /api/v1/school/calendar`, `/holidays`, `/attendance/daily` |
| Admissions | `feature/admissions/AdmissionRouting.kt` | `GET /api/v1/admissions/enquiries`, `/summary`, `PATCH /{id}/status` |
| Announcements | `feature/announcements/AnnouncementRouting.kt` | School list/search/create/sync are present |
| Messages | `feature/school/MessagesRouting.kt` | Code has `GET /threads`, `GET /threads/{id}/messages`, `POST /threads/{id}/read`, `POST /messages`; deployed backend may lag |
| Leave requests | `feature/school/LeaveRequestsRouting.kt` | `GET /api/v1/school/leave-requests`, `PATCH /{id}/status` |
| PTM | `feature/school/PtmRouting.kt` | `GET/POST /api/v1/school/ptm`, metrics/progress/complete endpoints |
| Results | `feature/school/ResultsRouting.kt` | `GET /api/v1/school/results` and selectors/upsert support |
| Analytics | `feature/school/SchoolAnalyticsRouting.kt` | Overview/class/teacher/student/syllabus/cohort routes |
| Profile | `feature/user/UserProfileRouting.kt` | Profile, philosophy, tour videos, gallery, visibility |

---

## 4. Screenshot-by-screenshot findings

### 4.1 Direct Messaging — endpoint not found

Screenshot shows a modal for **Admin Desk** with:

```text
Endpoint not found: /api/v1/school/messages/threads/{threadId}/messages
```

**Current code state:**

- Client calls `GET api/v1/school/messages/threads/$threadId/messages` in `MessagesApi.kt`.
- Server contains `get("/threads/{id}/messages")` inside `route("/api/v1/school/messages")` in `MessagesRouting.kt`.
- `Application.kt` registers `messagesRouting()`.

**Interpretation:** the code at HEAD contains the route, so the screenshot likely came from one of these states:

1. The phone is hitting a deployed Render backend that does not include commit `c81aebc` or later.
2. Android Studio dev build is pointed to Render because `local.properties` has no `devBaseUrl`.
3. API guide/Postman spec omitted the conversation endpoint, so deployment/tests may not include it.

**Required next fix:**

- Redeploy backend after HEAD.
- Add `GET /api/v1/school/messages/threads/{id}/messages` to `SCHOOL_API_GUIDE.md` and test chain.
- Add a temporary app-status/version response that prints backend git SHA so phone screenshots can prove which backend is being hit.

### 4.2 Academic Calendar — `working_days` deserialization failure

Screenshot shows:

```text
Illegal input: Field 'working_days' is required for type CalendarSummaryDto, but it was missing at path: $.data.summary
```

**Current code mismatch:**

- Client DTO: `CalendarSummaryDto(@SerialName("working_days") val workingDays: Int, ...)`.
- Server DTO: `CalendarSummary(@SerialName("total_working_days") val totalWorkingDays: Int, ...)`.

**Current result:** The endpoint may return data successfully, but the app fails to decode it, so the user sees the red error card and calendar empty state.

**Required next fix:** Align server and client names. Recommended server response for compatibility:

```json
{
  "working_days": 22,
  "total_working_days": 22,
  "public_holidays": 2,
  "school_holidays": 1
}
```

Then update client DTO or keep `working_days` as canonical.

### 4.3 Institutional Profile — public toggle layout glitch

Screenshot shows the **PUBLIC** toggle text clipped/stacked vertically as `P U B L I C`.

**Current result:** Functionality may work, but UI is visibly broken on real phone width.

**Required next fix:** Increase toggle chip width or redesign it as a normal row switch with label outside the switch container. Test on 360–393 dp width.

### 4.4 Admission enquiries — empty/zero state

Screenshot shows:

- `0 Follow-ups`
- `0 Converted`
- `No enquiries yet`

**Possible current result:** If the logged-in school has no `admission_enquiries` rows for its `school_id`, this is expected. If sample/enquiry data exists under a different `school_id`, this is a school-scoping/data-seeding mismatch.

**Required next fix:** Verify the logged-in school admin's `app_users.school_id` equals the `admission_enquiries.school_id` rows being tested. Add seeded enquiries for the same school used by the school-admin login.

### 4.5 Announcements — inappropriate/test content visible

Screenshot shows live announcement content with abusive/profane test strings.

**Current result:** The school announcements feature is connected enough to display DB-created rows, but test data/content moderation is not production-safe.

**Required next fix:** Clean production/test seed rows and add input validation/moderation rules before announcements can be published/synced to WhatsApp/parent side.

### 4.6 Onboarding welcome/dashboard

Screenshot shows onboarding progress at `0%` and setup steps.

**Current result:** This aligns with a school user whose onboarding is not complete. Earlier session-expired fix improves the 401 experience, but the user must still be hitting the same backend that issued the token.

**Required next fix:** Finish global unauthorized handling and backend-target visibility.

---

## 5. Supabase schema audit from uploaded `Vidyasetu schema.txt`

Uploaded schema unique tables found: 28.

### 5.1 Tables the server uses but the uploaded schema does not contain

| Missing table | Server usage | Impact if absent in production Supabase |
|---|---|---|
| `faculty` | `FacultyTable`; daily attendance faculty mode; teacher performance analytics | Faculty attendance and teacher analytics can fail with relation-not-found or always empty. |
| `holiday_list` | `HolidayListTable`; `/api/v1/school/holidays`; calendar holiday summary | Holidays API and calendar holiday counts can fail or be empty. |

### 5.2 Uploaded table that server does not use directly

| Uploaded table | Note |
|---|---|
| `users` | Server intentionally uses `app_users` for app auth/user records; this `users` table may be Supabase/Auth-related or legacy. It is not mapped in `Tables.kt`. |

### 5.3 Schema-source drift

There are at least three schema references:

1. Repo root `supabase_schema` — a larger v2.1 schema beginning with `school_directory` and many enums.
2. Uploaded `Vidyasetu schema.txt` — operational table set used by the current Ktor backend, but missing `faculty` and `holiday_list`.
3. `Tables.kt` comment says to run root `/supabase_schema` plus supplementary SQL.

**Risk:** Different developers/environments can have different table sets. This explains why features compile locally but fail against actual Supabase.

**Required next fix:** Create one canonical SQL migration set for the Ktor backend and update docs so production Supabase includes exactly the tables mapped by `Tables.kt`.

---

## 6. Feature-by-feature status matrix

| # | Feature | Current status | Current vs desired |
|---|---|---|---|
| 1 | School onboarding — BASIC | [Partial] Mostly working | Saves basics when token/backend match. Still needs editable real address/location and global 401 handling. |
| 2 | School onboarding — BRANDING | [Partial] Partial | Logo/cover actions are URL/stable placeholder based, not binary upload. Mission/vision fields are not fully persisted to backend schema. |
| 3 | School onboarding — ACADEMIC | [Working] Core working | Class/subject setup is connected. Needs more validation and duplicate handling UX. |
| 4 | School onboarding — LAUNCH/REVIEW | [Partial] Partial | Completion can work, but document upload is local UI state only, not real storage/verification. |
| 5 | School dashboard | [Working] Core working | Dashboard reflects onboarding state. Needs backend target/version diagnostics and better discoverability. |
| 6 | Institutional profile | [Partial] Partial | Philosophy/gallery/tour/visibility are wired by URL. UI toggle has real-phone layout glitch. No binary upload. |
| 7 | Admissions CRM | [Partial] Partial | Summary/list/status are wired, but empty screenshot needs school-id seed verification. Some secondary controls remain no-op. |
| 8 | Announcements | [Partial] Partial | School list/search/filter/create/sync wired. Parent side does not consume same rows yet. Test/profane data visible. |
| 9 | Messages | [P0] Broken in latest screenshot / code mostly present | Thread list can show. Conversation route exists in HEAD but screenshot backend returns 404. Parent side messaging is static, so harmony is missing. |
| 10 | Academic calendar | [P0] Runtime broken | DTO mismatch (`working_days` vs `total_working_days`) breaks decoding. Also depends on missing `holiday_list` in uploaded schema. |
| 11 | Daily attendance | [Partial] Partial | Student/faculty data can load if schema and seed are correct. Faculty path depends on missing `faculty` table in uploaded schema. Date/see-all control remains no-op. |
| 12 | Leave requests | [Working] Core working | List + approve/reject are school-scoped. Needs attachments/detail screen. |
| 13 | Results | [Working] Core working | Selectors/search/results are wired. Needs upload/edit records if school staff need full write workflows. |
| 14 | Schedule PTM | [Partial] Partial | Create/list works; metrics/progress/complete endpoints exist, but several UI buttons are still no-op. Parent PTM harmony still needs verification. |
| 15 | Analytics overview | [Partial] Hybrid | More live than before, but some cards/insights are still CMS/template-derived. |
| 16 | Student analytics cohort | [Partial] Hybrid | Some live computations, but risk model is incomplete. |
| 17 | Teacher performance | [Partial] Hybrid / schema risk | Uses faculty/attendance concepts; uploaded schema lacks `faculty`. |
| 18 | Class performance | [Partial] Hybrid | Needs deeper real aggregation from results/attendance. |
| 19 | Syllabus coverage | [Partial] Hybrid | Coverage still partly inferred/proxy; needs real syllabus/chapter completion tables. |
| 20 | Parent-school harmony | [P0] Not complete | Parent announcements/messages are not using the same operational school message/announcement tables. |

---

## 7. Remaining visible no-op/incomplete controls

Static scan still finds these school/admin no-op handlers:

| File | Line approx | Gap | Desired action |
|---|---:|---|---|
| `AdmissionCRMDashboard.kt` | 222 | Secondary `TextButton` | Wire to full enquiry list/detail/export or remove. |
| `AnalyticsDashboardScreen.kt` | 258, 302 | Card/header actions | Navigate to drilldowns/export/report filters or remove. |
| `DailyAttendanceScreen.kt` | 217 | `TextButton` | Open date picker or full attendance list. |
| `MessagesScreen.kt` | 428 | Action button | New message/search/filter action or remove. |
| `SchedulePTMScreen.kt` | 237, 337, 402, 414 | Several buttons/icons | Wire detail/reschedule/share/complete/metrics actions. |
| `SchoolAnnouncementsScreen.kt` | 357, 453 | See-all/secondary actions | Wire full list/audience details or remove. |
| `StudentAnalyticsScreen.kt` | 318 | Icon button | Wire export/filter/detail. |
| `SyllabusCoverageScreen.kt` | 182, 269 | Action button/icon | Wire syllabus sync/export/detail. |
| `TeacherPerformanceScreen.kt` | 350 | Action button | Wire teacher detail/export. |

Normal `placeholder = "..."` text in input fields is not a bug.

---

## 8. Known issue register

### 8.1 Images/media upload

**Status:** [P0] Not real upload yet.

Affected areas:

- Branding logo/cover.
- Institutional profile gallery and tour videos.
- Launch compliance documents.
- Announcement images.
- Student/result/avatar images if school staff need to add them.

Current backend accepts/persists URLs in several places, but no multipart/storage upload flow exists. `UserProfileRouting.kt` explicitly notes MVP storage estimates until binary uploads land.

**Desired:** Supabase Storage or equivalent upload endpoint, file picker on Android, server-side validation, persisted public/signed URL, and deletion/replacement flow.

### 8.2 Location/address

**Status:** [P0] Not real geolocation.

Current onboarding/profile uses address strings and local map preview. There is no real map picker, permission handling, geocoding, latitude/longitude persistence in the school table used by Ktor, or parent-side distance computation tied to selected school.

**Desired:** Editable address fields + optional Android location permission + lat/lng columns + geocoding/map picker + parent distance display from same data.

### 8.3 Session handling

**Status:** [Partial] Improved but not global.

Onboarding 401 behavior was improved in `bfca368`, but unauthorized handling should be centralized in the network layer/session manager so every school and parent screen behaves consistently.

### 8.4 Backend target visibility

**Status:** [P0] Missing.

When a real phone is connected over WiFi, the app must clearly show/log which backend it is using. Current Gradle warning says dev fallback is Render unless `devBaseUrl` is set.

**Desired:** Add debug-only backend banner/log containing base URL and backend git SHA/app-status version.

### 8.5 Data quality/content moderation

**Status:** [P0] Required before demo/production.

Screenshots show inappropriate test content in messages/announcements. Production seed/test data must be cleaned, and user-generated school content needs validation/moderation.

---

## 9. Parent-school harmony gaps

The user specifically asked that school and parent side communicate properly. Current state is not there yet.

### 9.1 Announcements

- School side creates/reads `announcements` through school routes.
- Parent route `GET /api/v1/parent/announcements` currently returns static/mock list in `ParentRouting.kt`.

**Result:** A school announcement can appear on school side but not parent side.

**Fix:** Parent announcements endpoint must query `AnnouncementsTable` scoped by the parent's child/school and return the same rows.

### 9.2 Messages

- School side uses `message_threads` and `messages` via `MessagesRouting.kt`.
- Parent message screen uses `ParentMessageViewModel` static sample threads; no parent messages backend route was found.

**Result:** Direct messaging is not a true parent-school conversation system yet.

**Fix:** Add parent message API using the same `message_threads/messages` tables with participant modeling. The current `owner_user_id` model is too admin-inbox-centric for two-way parent-school conversations.

### 9.3 PTM

- School PTM routes exist.
- Parent PTM state needs verification against same `ptm_events` and `ptm_class_progress` tables.

**Fix:** Ensure parent PTM screen lists school-created PTMs for that child/class and can confirm/check in if required.

### 9.4 Fees/results/attendance

Some parent features are still static/CMS or separate. Long-term, parent side should read the same school-scoped operational tables for child-linked records.

---

## 10. Prioritized fix plan

### P0 — Fix runtime blockers shown in screenshots

1. Fix calendar DTO mismatch: `working_days` vs `total_working_days`.
2. Verify/redeploy messages conversation endpoint and add it to docs/tests.
3. Add `faculty` and `holiday_list` to canonical Supabase migration.
4. Add debug backend target/version visibility.
5. Set `devBaseUrl` locally when testing real phone against laptop backend.

### P1 — Make parent-school communication real

1. Parent announcements must read school `announcements` table.
2. Parent messages must use backend + same message tables.
3. PTM parent side must read school-created `ptm_events`.
4. Verify school_id linkage: `app_users`, `children`, `students`, and parent-child-school relations.

### P2 — Remove visible glitches/no-ops

1. Fix Institutional Profile public toggle layout.
2. Wire/remove all no-op handlers listed in §7.
3. Clean test/profane seed data.
4. Add empty-state copy that differentiates “no data yet” from API failure.

### P3 — Real media/location workflows

1. Add Supabase Storage upload endpoint(s).
2. Add Android image/document picker.
3. Add address/geolocation columns/workflow.
4. Replace URL-only UI with real upload + preview + delete/replace.

### P4 — Analytics productionization

1. Remove CMS-only metrics or label as configured content.
2. Add real syllabus/chapter completion model.
3. Add student risk scoring from attendance/results/engagement.
4. Compute teacher KPIs from faculty attendance, PTM, results, and feedback.

---

## 11. Updated validation checklist

Run after next fixes:

```bash
./gradlew :server:compileKotlin -Pserver-only=true --no-daemon
./gradlew :shared:compileDevDebugKotlinAndroid :composeApp:compileDevDebugKotlinAndroid --no-daemon
./gradlew :composeApp:assembleDevDebug --no-daemon
```

Manual phone/backend checks:

1. On laptop, start backend on `0.0.0.0:8080`.
2. From phone browser, open `http://<laptop-LAN-ip>:8080/api/v1/config/app-status`.
3. In Android Studio project `local.properties`, set:

```properties
devBaseUrl=http://<laptop-LAN-ip>:8080
```

4. Rebuild/reinstall dev app.
5. Login as school admin.
6. Confirm app logs/base-url banner show laptop backend, not Render.
7. Open Messages → Admin Desk → conversation; expect no 404.
8. Open Academic Calendar; expect no `working_days` serialization error.
9. Toggle Institutional Profile public/private; expect no clipped vertical label.
10. Create announcement on school side; verify the same announcement appears on parent side after parent endpoint is rewired.
11. Test image/document upload only after real storage endpoint exists; until then, treat URL/local marker UI as incomplete.

---

## 12. Bottom line

The school side has real progress: onboarding, school dashboard, admissions, announcements, messages, leave requests, PTM, results, and analytics all have meaningful code and several real backend routes. However, the latest screenshots identify **active runtime blockers**:

1. Calendar response/client field mismatch.
2. Message conversation endpoint missing on the backend currently reached by the phone.
3. Supabase table mismatch for `faculty` and `holiday_list`.
4. Parent-side communication is not yet using the same school data.
5. Media upload and location are still placeholder/URL/local workflows.
6. Several UI controls remain no-op or visually broken.

Next fixing pass should start with the P0 list above before polishing UI or adding new features.
