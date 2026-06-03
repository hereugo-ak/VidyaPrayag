# VidyaPrayag — School Side Status Report

> **Audit branch requested:** `backend-by-abuzar`.
> **Repository state checked:** local `main` at `f3f33c9`, which is `Merge pull request #9 from hereugo-ak/backend-by-abuzar`; remote `origin/backend-by-abuzar` at `682d682`.
> **Audit date:** 2026-06-03.
> **Scope:** Whole school-side codebase: Compose UI (`composeApp`), shared KMP client/API layer (`shared`), Ktor backend (`server`), database schema docs/mappings, last 30 commits, uploaded Android Studio log, and attached screenshots.
> **Important:** The issues below include the known screenshot issues, but they are not the only issues. This audit found additional build, security, routing, data-model, and architecture risks.

---

## 1. Executive summary

The latest code has several intended fixes merged from `backend-by-abuzar`, including calendar DTO compatibility, message conversation route code, backend target visibility, canonical DB docs, parent announcements/messages work, and several UI overflow fixes. However, the current repository is **not in a deployable backend state** because `:server:compileKotlin` fails.

The screenshots and logs prove the tested APK was hitting `https://vidyaprayag-1.onrender.com`, not a local backend. Therefore, some screenshot failures are deployment drift: the code now contains fixes that the Render backend used by the phone did not have at the time of the run. That does not remove the problem; it means the team must fix build, redeploy, and add version verification before any school-side retest.

Highest-risk findings:

| Priority | Area | Core reason | Current impact |
|---|---|---|---|
| P0 | Backend build | `ParentRouting.kt` imports/uses unresolved `org.jetbrains.exposed.sql.inList` | Current backend cannot compile/deploy from `main`/merged `backend-by-abuzar`. |
| P0 | Message thread modal | APK hit stale Render backend; log confirms 404 for `/api/v1/school/messages/threads/{id}/messages` while source code contains the route | Conversation modal fails despite route existing in repo. |
| P0 | Academic calendar | APK hit backend/client combination where `summary.working_days` was missing and client required it | Calendar screen shows red deserialization error and no data. |
| P0 | Secret leakage | `safeApiCall` logs raw request body and response body; uploaded log contains password and JWT tokens | Severe security issue in shared Android logs. |
| P0 | Parent/school route conflicts | Legacy `ParentRouting.kt` still defines mock `/parent/track-progress` and `/parent/fees` while newer live routes define the same paths | Mock/static routes can shadow or conflict with live routes. |
| P1 | Onboarding location | UI only renders a deterministic map preview; no current-location button, permission, GPS, geocoding, lat/lng persistence in `SchoolsTable` mapping | "Use current location" requirement is not implemented. |
| P1 | Media upload | Branding/profile/gallery use placeholder URLs or URL text entry; no multipart/file-picker/storage upload path | Logo, profile picture, gallery, docs are not real uploads. |
| P1 | Class/subject management | Academic payload applies the same subject array to every class; teacher assignment is free-text only | No true grade-specific subject pool or teacher/class/subject assignment model. |
| P1 | Broadcast segmentation | Announcement model has no audience/class/subject/teacher targeting fields | Admin/teacher broadcast segmentation cannot be enforced. |
| P1 | Parent discoverability | `SchoolsTable` used by Ktor lacks latitude/longitude; parent dashboard lists active schools by city with fixed rating | Parents cannot discover onboarded schools by geographic distance. |

### Remediation update in this working tree

After the initial audit, the following P0/frontend-stability fixes were applied and validated locally:

- Redacted the sensitive password example in this report.
- Fixed the backend compile blocker in `ParentRouting.kt` by removing the unsupported `inList` usage.
- Removed legacy duplicate parent `/track-progress` and `/fees` mock routes from `ParentRouting.kt` so DB-backed route owners can respond.
- Hardened `safeApiCall` logging so request/response bodies redact password, OTP/code, token, refresh token, authorization, cookie, and API-key style fields.
- Removed/disabled school/admin no-op CTAs that previously looked tappable but did nothing.
- Replaced protected `googleusercontent`/placeholder image usage in the audited admin surfaces with local icon/initials UI or safer non-protected sample URLs.
- Validation now passes:
  - `./gradlew :server:compileKotlin -Pserver-only=true --no-daemon` — **BUILD SUCCESSFUL**.
  - `./gradlew :shared:compileDevDebugKotlinAndroid :composeApp:compileDevDebugKotlinAndroid --no-daemon` — **BUILD SUCCESSFUL**.
- Remaining console output is Gradle/Kotlin deprecation/configuration warnings, not Kotlin compile errors.

---

## 2. Validation performed

### 2.1 Git/branch validation

Commands/checks performed:

```bash
git fetch --all --prune
git status --short
git branch -a
git log --oneline --decorate -30
git log --oneline --decorate -30 origin/backend-by-abuzar
git diff --name-status main..origin/backend-by-abuzar
```

Result:

- Current local branch: `main`.
- Current local/remote main HEAD: `f3f33c9`.
- `f3f33c9` is merge PR #9 from `backend-by-abuzar`.
- `origin/backend-by-abuzar` is `682d682`.
- `main..origin/backend-by-abuzar` has no file diff because the branch was merged.

### 2.2 Backend compile validation

Command run:

```bash
./gradlew :server:compileKotlin -Pserver-only=true --no-daemon
```

Initial audit result: **FAILED** with:

```text
e: server/src/main/kotlin/com/littlebridge/vidyaprayag/feature/user/ParentRouting.kt:21:34 Unresolved reference 'inList'.
```

Fix applied in this working tree:

- Removed `org.jetbrains.exposed.sql.inList`.
- Built an Exposed OR expression from the parent's resolved `schoolIds`.
- Removed duplicate legacy mock `/api/v1/parent/track-progress` and `/api/v1/parent/fees` route handlers from `ParentRouting.kt`.

Post-fix validation:

```bash
./gradlew :server:compileKotlin -Pserver-only=true --no-daemon
```

Result: **BUILD SUCCESSFUL**.

### 2.3 Uploaded Android log validation

The full file was downloaded from the wrapper URL and inspected. Key evidence:

```text
VidyaPrayagApp I Backend -> authBaseUrl=https://vidyaprayag-1.onrender.com schoolBaseUrl=https://vidyaprayag-1.onrender.com
```

So the tested APK was using Render.

Message failure:

```text
RESPONSE ERROR BODY: {"success":false,"message":"Endpoint not found: /api/v1/school/messages/threads/876b5ba8-16ca-46fa-b9a2-4258deab3d09/messages"}
MessagesVM E getThreadMessages failed: Endpoint not found: /api/v1/school/messages/threads/876b5ba8-16ca-46fa-b9a2-4258deab3d09/messages
```

Calendar failure:

```text
UNKNOWN ERROR: Illegal input: Field 'working_days' is required ... CalendarSummaryDto ... missing at path: $.data.summary
AcademicCalendarVM E getCalendar error: Illegal input: Field 'working_days' is required ...
```

Image failures:

```text
RealImageLoader E Failed - https://share.google/LqjE0becQmFwQrPl5
ImageDecoder$DecodeException: Failed to create image decoder ... Input contained an error.
BitmapFactory returned a null bitmap.
RealImageLoader E coil3.network.HttpException: HTTP 403
RealImageLoader E UnknownHostException: Unable to resolve host "assets.vidyaprayag.com"
```

Security leak in logs:

```text
REQUEST BODY: {"identifier":"abuzarkn99@gmail.com","password":"[REDACTED]","role":"ADMIN"}
RESPONSE BODY: ApiResponse(... token=..., refreshToken=...)
```

This is a high-risk logging problem, not just debug noise.

---

## 3. Last 30 commits reviewed

| # | Commit | Author | Summary | Audit note |
|---:|---|---|---|---|
| 1 | `f3f33c9` | MD ABUZAR SALIM | Merge pull request #9 from hereugo-ak/backend-by-abuzar | Current `main`; merges backend-by-abuzar. |
| 2 | `682d682` | MD ABUZAR SALIM | fix(build): resolve 'Unresolved reference: time' in server/build.gradle.kts | Did not catch current `ParentRouting.kt` compile failure. |
| 3 | `c381371` | MD ABUZAR SALIM | fix(ui): prevent phone-screen overflow/clipping on admin screens | Addresses visible narrow-phone layout issues. |
| 4 | `090a68b` | MD ABUZAR SALIM | fix(parent-school harmony + ui): real announcements, parent messages, toggle | Introduced `inList` compile failure; also leaves duplicate parent routes. |
| 5 | `a3b8a68` | MD ABUZAR SALIM | fix(school): P0 fixes - calendar DTO, backend visibility, canonical schema | Adds intended fixes for screenshots, but requires successful deploy. |
| 6 | `6d8ffab` | MD ABUZAR SALIM | docs(school): update school-side issue audit | Previous status report update. |
| 7 | `0f4f865` | MD ABUZAR SALIM | Merge pull request #8 from hereugo-ak/fix/school-session-onboarding | Merged school session fixes. |
| 8 | `bfca368` | MD ABUZAR SALIM | Fix school onboarding session handling | Improved onboarding 401/session behavior. |
| 9 | `0e33daf` | MD ABUZAR SALIM | Merge pull request #7 from hereugo-ak/backend-by-abuzar | Earlier backend-by-abuzar merge. |
| 10 | `03ede1b` | MD ABUZAR SALIM | Replace placeholder analytics with real data-driven charts & aggregation | Analytics improved but still hybrid/CMS-derived. |
| 11 | `3fa0be2` | MD ABUZAR SALIM | docs(school): add detailed school-side status report | Earlier audit. |
| 12 | `c81aebc` | MD ABUZAR SALIM | feat(messages): wire conversation view end-to-end; diagnose dev backend/401 | Source route exists after this; Render in log was stale. |
| 13 | `da1fe3c` | MD ABUZAR SALIM | feat(school): enforce school authorization + remove placeholders across school surface | Important school scoping work. |
| 14 | `6489ada` | MD ABUZAR SALIM | feat(onboarding): persist real academic structure + unify completion logic | Academic persistence added, but subject model remains too generic. |
| 15 | `90707b9` | MD ABUZAR SALIM | fix(android-build): convert expression-body parse helpers to block body | Build hygiene. |
| 16 | `18fabe6` | MD ABUZAR SALIM | Merge pull request #6 from hereugo-ak/backend-by-abuzar | Historical merge. |
| 17 | `aafe10e` | MD ABUZAR SALIM | feat(school): wire announcement filters + CRM Generate Report nav | Adds UI wiring. |
| 18 | `77a65b7` | MD ABUZAR SALIM | feat(school): wire AcademicCalendar nav + SchoolDashboard support actions | Calendar navigation work. |
| 19 | `48d16f2` | MD ABUZAR SALIM | feat(school): wire PTM scheduling dialog + Enquiry status dropdown | PTM/enquiry wiring. |
| 20 | `305f36c` | MD ABUZAR SALIM | feat(school): wire UI affordances for Messages compose + Announcements create/search | UI affordances added. |
| 21 | `d132863` | MD ABUZAR SALIM | feat(school): wire 5 remaining admin screens (analytics+profile) to API | Profile/API work. |
| 22 | `da3ca17` | MD ABUZAR SALIM | fix(analytics): replace expression-body return with block body in parseCard/parseInsight | Build hygiene. |
| 23 | `632ffcf` | MD ABUZAR SALIM | fix(analytics): restore AnalyticsCardData/InsightItem + add local.properties dev URL support | Added local URL support, but dev fallback still points to Render. |
| 24 | `a97a2df` | MD ABUZAR SALIM | feat(school): wire AnalyticsDashboard & Results screens to API | API wiring. |
| 25 | `287197d` | MD ABUZAR SALIM | feat(school): wire AcademicCalendar, LeaveRequests & DailyAttendance screens to API | Calendar/attendance API wiring. |
| 26 | `31d3c2e` | MD ABUZAR SALIM | fix(config): point dev flavor at render.com + wire Announcements & PTM screens to API | Explains why dev APK hits Render unless `devBaseUrl` is set. |
| 27 | `2718ac8` | MD ABUZAR SALIM | feat(messages): wire admin Messages screen to /api/v1/school/messages endpoints | Message thread list route added. |
| 28 | `7bd19fd` | MD ABUZAR SALIM | feat(admissions): wire AdmissionCRM to /api/v1/admissions/enquiries endpoints | Admissions API wiring. |
| 29 | `4b0a092` | MD ABUZAR SALIM | fix(school-dashboard): drive UI from /user/details + correct post-onboarding state | Dashboard/onboarding status fix. |
| 30 | `7951bfa` | MD ABUZAR SALIM | fix(onboarding): restore Koin DI bindings clobbered by main merge | DI fix. |

---

## 4. Screenshot-by-screenshot root cause analysis

### 4.1 Direct Messaging modal: endpoint not found

Screenshot shows:

```text
Endpoint not found: /api/v1/school/messages/threads/876b5ba8-16ca-46fa-b9a2-4258deab3d09/messages
```

Source code state:

- Client calls `GET api/v1/school/messages/threads/$threadId/messages` in `MessagesApi.kt`.
- Server contains `get("/threads/{id}/messages")` in `server/.../feature/school/MessagesRouting.kt`.
- `Application.kt` registers `messagesRouting()`.

Core reason:

- The phone log proves the APK hit Render.
- Render returned 404, so Render did not have the route deployed at the time of the run, or it was running a stale build.
- Current source contains the route, but current source cannot be deployed until the P0 compile error is fixed.

Required fix:

1. Fix backend compile first.
2. Redeploy Render from current commit.
3. Hit `GET /api/v1/config/version` from the phone and verify SHA/build time.
4. Retest Messages -> Admin Desk conversation.

### 4.2 Academic Calendar: missing `working_days`

Screenshot/log shows:

```text
Field 'working_days' is required ... CalendarSummaryDto ... missing at path: $.data.summary
```

Current source state:

- `CalendarSummaryDto` now has defaults and supports both `working_days` and `total_working_days`.
- Server `CalendarSummary` now emits both fields.

Core reason:

- The APK/backend pair used during the log was stale relative to current source.
- The log still proves an active integration failure: no version pinning was used, and deployed backend/client contract drift reached the user.

Required fix:

1. Compile and redeploy current backend.
2. Rebuild/reinstall APK from current client.
3. Confirm `GET /api/v1/school/calendar` response contains both fields.
4. Add a contract test for calendar summary to prevent regression.

### 4.3 Gallery photo dialog: URL accepted but image fails

Screenshot shows the profile gallery accepting "Image URL". Log shows failures for `https://share.google/...`, `lh3.googleusercontent.com/...` HTTP 403, and `assets.vidyaprayag.com` DNS failure.

Core reason:

- The product has no real upload infrastructure here.
- The UI accepts arbitrary URLs but does not validate that they are direct, decodable image URLs.
- Google share links are HTML/redirect/share pages, not stable image bytes.
- Some seeded/placeholder asset hosts are invalid or protected.

Required fix:

- Add Android file picker + multipart upload to Supabase Storage or another storage backend.
- Persist signed/public URLs returned by the backend.
- Validate content type and size server-side.
- Reject non-direct URLs with a user-friendly message until upload exists.

### 4.4 Onboarding location / "use current location"

Code evidence:

- `InstitutionalBasicOBScreen.kt` has a `LocationPicker(address = address)` that renders a static map-style box.
- There is no permission request, no platform location provider, no GPS API call, no geocoding, and no latitude/longitude persistence in the Ktor `SchoolsTable` mapping.
- `supabase_schema` has geo fields in `school_directory`, but the Ktor backend uses `schools` and does not map lat/lng there.

Core reason:

- Location is still a UI preview/address string feature, not a real location workflow.

Required fix:

- Add lat/lng columns to the active `schools` model or synchronize `schools` to `school_directory`.
- Add Android location permission and provider implementation.
- Add reverse geocoding/manual map picker fallback.
- Persist `{full_address, city, district, state, pincode, latitude, longitude}`.
- Use this data in parent school discovery.

---

## 5. Additional problems found beyond screenshots

### 5.1 P0: Current backend does not compile

File:

- `server/src/main/kotlin/com/littlebridge/vidyaprayag/feature/user/ParentRouting.kt`

Problem:

```kotlin
import org.jetbrains.exposed.sql.inList
.where { AnnouncementsTable.schoolId inList schoolIds }
```

Compile result:

```text
Unresolved reference 'inList'
```

Impact:

- None of the recently merged fixes can be reliably deployed.
- Render may remain stale, which keeps producing message/calendar failures.

Fix direction:

- Use the correct Exposed DSL import/operator for the project version, or replace with a portable filter/query strategy.
- Add `:server:compileKotlin` as required CI before merging.

### 5.2 P0: API logging leaks credentials and tokens

File:

- `shared/src/commonMain/kotlin/com/littlebridge/vidyaprayag/core/network/NetworkResult.kt`

Problem:

```kotlin
AppLogger.d("API_CALL", "REQUEST BODY: ${requestBody.text}")
AppLogger.d("API_CALL", "RESPONSE BODY: $body")
```

Uploaded log contains:

- Plain email/password login body (redacted in this report, but present in the original uploaded log).
- JWT access token.
- JWT refresh token.

Impact:

- Any shared Android Studio log can compromise user accounts.
- This must be treated as a security vulnerability.

Fix direction:

- Never log request bodies for auth endpoints.
- Redact `password`, `otp`, `token`, `refreshToken`, `Authorization`, cookies, and API keys recursively.
- Gate verbose API logs behind a debug flag and disable in release builds.
- Consider rotating tokens/passwords exposed in shared logs.

### 5.3 P0/P1: Duplicate parent routes shadow live implementations

Files:

- `server/src/main/kotlin/com/littlebridge/vidyaprayag/feature/user/ParentRouting.kt`
- `server/src/main/kotlin/com/littlebridge/vidyaprayag/feature/parent/TrackProgressRouting.kt`
- `server/src/main/kotlin/com/littlebridge/vidyaprayag/feature/parent/ParentFeesRouting.kt`
- `server/src/main/kotlin/com/littlebridge/vidyaprayag/Application.kt`

Problem:

`Application.kt` registers both old and new route groups:

```kotlin
parentRouting()
trackProgressRouting()
parentFeesRouting()
```

But `ParentRouting.kt` still defines:

```kotlin
get("/track-progress") { // Mock data matching the UI requirements }
get("/fees") { ... static data ... }
```

Newer files also define the same paths with DB-driven logic.

Impact:

- Depending on route resolution, old mock endpoints can shadow newer live endpoints.
- Parent progress bars and fees may remain static even after live routes were added.
- This directly relates to the reported "Student progress tracking bars are non-interactive placeholders" problem.

Fix direction:

- Split or delete legacy `ParentRouting.kt` endpoints that have been replaced.
- Keep one route owner per path.
- Add route inventory tests that fail on duplicate method/path registrations.

### 5.4 P1: Branding/profile/logo upload is fake or URL-only

Files:

- `composeApp/.../BrandingInfoOBScreen.kt`
- `shared/.../BrandingInfoOBViewModel.kt`
- `composeApp/.../InstitutionalProfileScreen.kt`
- `server/.../feature/user/UserProfileRouting.kt`

Initial evidence:

```kotlin
viewModel.updateCoverImage("https://placehold.co/1920x820/...")
viewModel.updateLogo("https://placehold.co/512x512/...")
```

Current status: those placeholder-save actions were removed in this working tree and replaced with a clear disabled/upload-not-connected notice. `BrandingInfoOBViewModel` still notes that mission/vision/tour are not part of the server schema for that step and remain local.

Impact:

- School logo/profile picture placeholders are not connected to upload infrastructure.
- Cover image is not submitted in the branding payload.
- Mission/vision/tour entered during onboarding can be lost/not persisted in the expected place.

Fix direction:

- Add upload endpoint and storage table updates.
- Submit cover image, mission, vision, and tour fields through a defined backend contract.
- Reuse `school_philosophy` and `school_media` consistently instead of local-only state.

### 5.5 P1: Academic class/subject management is still too generic

Files:

- `shared/.../AcademicInfoOBViewModel.kt`
- `server/.../feature/onboarding/OnboardingRouting.kt`
- `server/.../db/Tables.kt`

Problem:

`buildAcademicPayload()` creates one `subjectsArray` from the currently selected class subjects, then sends that same array for every available class:

```kotlin
s.availableClasses.forEach { className ->
    put("subjects", subjectsArray)
}
```

`SchoolSubjectsTable.teacherAssigned` is just text, not a `faculty`/user FK.

Initial UI no-op evidence:

- `AcademicInfoOBScreen.kt` had comment-only Change/Assign handlers. Current status: those controls are now visibly disabled/status-only until a real teacher-assignment workflow exists.

Impact:

- All classes can receive the same uniform subject list.
- There is no grade-specific subject pool.
- There is no enforceable teacher-class-subject assignment.
- Teacher broadcasts cannot be scoped to classes/subjects they teach because the backend lacks a reliable assignment graph.

Fix direction:

- Introduce `teacher_subject_assignments` or equivalent table: `school_id`, `faculty_id/user_id`, `class_id`, `section`, `subject_id`.
- Make academic UI edit per-class subject sets, not one global list.
- Replace teacher free text with selected faculty records.

### 5.6 P1: Broadcast segmentation is not implemented

Files:

- `server/.../feature/announcements/AnnouncementRouting.kt`
- `server/.../db/Tables.kt`

Current model:

- Announcement has `school_id`, `type`, title, description, event image, date.
- WhatsApp sync queues every active parent user in that school with a phone.
- There is no announcement audience field, class/section/subject field, or teacher-owned scope.

Impact:

- Admin broadcasts cannot be cleanly audited as "all students" versus selected audience.
- Teacher broadcasts cannot be restricted to the classes/subjects they teach.
- Parent delivery can over-send or under-send once multiple classes/teachers exist.

Fix direction:

- Add audience model: `ALL_SCHOOL`, `CLASS`, `SECTION`, `SUBJECT`, `STUDENT`, `CUSTOM`.
- Add recipient expansion service using children/students/class/subject/teacher assignment data.
- Store resolved recipients for audit and delivery retry.

### 5.7 P1: Parent school discoverability is not geographically connected to onboarding

Files:

- `server/.../db/Tables.kt`
- `server/.../feature/parent/ParentDashboardRouting.kt`
- `supabase_schema`

Problem:

- Ktor `SchoolsTable` maps city/address/logo only, no lat/lng.
- Parent dashboard picks first five active schools and uses fixed rating `4.5`.
- `supabase_schema` has `school_directory.latitude/longitude`, but the school-side onboarding writes to `schools`, not that directory.

Impact:

- Onboarded schools are not discoverable by actual parent location.
- "Near me" preferences cannot work reliably.
- School profile sync to parent side is incomplete.

Fix direction:

- Decide canonical school directory table.
- Persist geo coordinates during onboarding/profile update.
- Add parent discovery endpoint with distance filtering/sorting.
- Keep public profile visibility in sync.

### 5.8 P1: OTP/auth environment is only partially production-ready

Known team issue:

- Missing OTP environment settings on Render.

Code state:

- OTP providers exist (`Fast2SMS`, `MSG91`, WhatsApp, SMTP, console fallback).
- `.env.example` documents Render-related OTP settings.
- `OTP_DEV_RETURN_CODE` defaults to true in code if unset.
- Console fallback can print OTPs to server logs if enabled.

Impact:

- Account creation via email/password can work, but phone OTP may silently degrade or fail depending on Render env.
- Leaving dev code return/console fallback enabled in production leaks OTPs.

Fix direction:

- Set real provider credentials on Render.
- Set `OTP_DEV_RETURN_CODE=false` in production.
- Set console fallback policy explicitly.
- Add `/api/v1/auth/otp/health` or admin diagnostics gated by `OTP_ADMIN_TOKEN`.

### 5.9 P2: School/admin screens still contain visible no-op controls

Initial static scan found no-op handlers in school/admin surfaces. These were cleaned in this working tree by removing the fake action, replacing it with non-clickable status text, or making the not-yet-implemented CTA visibly disabled:

| File | Example issue |
|---|---|
| `AcademicInfoOBScreen.kt` | Show more / Change / Assign controls are comments/no-op. |
| `AdmissionCRMDashboard.kt` | Secondary action no-op. |
| `AnalyticsDashboardScreen.kt` | Header/card actions no-op. |
| `DailyAttendanceScreen.kt` | See-all/date action no-op. |
| `MessagesScreen.kt` | Floating/action button no-op. |
| `SchedulePTMScreen.kt` | Multiple detail/share/reschedule/complete actions no-op. |
| `SchoolAnnouncementsScreen.kt` | "Read detailed schedule" and PTM "Book Slot" no-op. |
| `StudentAnalyticsScreen.kt` | Icon action no-op. |
| `SyllabusCoverageScreen.kt` | Sync/export/detail actions no-op. |
| `TeacherPerformanceScreen.kt` | Detail/export action no-op. |

Impact:

- Even when data loads, user workflows are incomplete.
- Some controls imply functionality that does not exist.

Fix direction / current status:

- Completed for the audited admin/school surfaces: fake actions were removed or visibly disabled.
- Follow-up still recommended: add UI tests that tap major CTAs and assert expected state/navigation.

### 5.10 P2: Image placeholders and external protected URLs

Initial evidence:

- Attached logs show Google share/direct image 403 and invalid host failures.
- `SchoolAnnouncementsScreen.kt` used hardcoded `lh3.googleusercontent.com/aida-public/...` participant avatar URLs for event cards.
- Legacy parent mock routes included external image placeholders.

Current status: the audited admin/school screens no longer reference protected `googleusercontent` URLs or placeholder-save URLs. Legacy parent mock `/track-progress` and `/fees` routes were removed from `ParentRouting.kt`. A broader follow-up should still replace remaining sample media with bundled assets or controlled storage URLs.

Impact:

- UI can show broken images and repeated error logs.
- External protected URLs are not reliable assets.

Fix direction:

- Replace placeholder external image URLs with bundled assets or controlled storage URLs.
- Validate image URL reachability before saving.
- Add fallback images in UI.

---

## 6. Feature status matrix: school side

| Feature | Status | Root issue / note |
|---|---|---|
| Email/password login | Mostly working | Log shows successful email/password login; client body logging has now been redacted in this working tree. Rotate any credentials/tokens exposed in previously shared logs. |
| OTP auth | Partial | Provider infrastructure exists, but Render env must be configured and dev fallback disabled. |
| School dashboard | Partial | Loads `/user/details`; repeated cancelled jobs appear during navigation, likely lifecycle cancellation but noisy. |
| Institutional basics | Partial | Saves address strings; no real current location/GPS/lat-lng. |
| Branding | Partial | Fake placeholder URL save actions removed; real upload/storage endpoint and mission/vision/tour persistence are still needed. |
| Academic structure | Partial | Persists classes/subjects, but same subject list can be applied to every class; teacher assignment remains free-text/schema-limited, with fake UI actions disabled. |
| Launch/review | Partial | Compliance docs are static false/verified placeholders. |
| Institutional profile | Partial | Philosophy/gallery/tour URL persistence exists; no upload; invalid URLs break images. |
| Admissions CRM | Partial | Endpoints exist; empty states may be real no-data or school-id seed mismatch. |
| Announcements | Partial | Create/search/sync exist; no audience segmentation or moderation; fake local action buttons were removed/disabled. |
| Messages | Partial/Broken on tested APK | Source route exists and backend now compiles locally; Render still must be redeployed and retested because the uploaded APK hit stale Render. |
| Academic calendar | Partial/Broken on tested APK | Source has compatibility fix; tested APK/backend had DTO drift; depends on redeploy and schema. |
| Attendance | Partial | Student/faculty endpoint exists; faculty depends on `faculty` table and data. |
| PTM | Partial | Create/list/progress exists; fake local CTAs were removed/disabled; parent harmony still needs retest after redeploy. |
| Results | Partial/Working core | Read/selectors exist; write/import workflows still limited. |
| Analytics | Partial | More DB-driven than before, but still CMS/default driven in places. |
| Parent-school announcements | Locally buildable | `inList` compile error fixed; requires deploy and end-to-end parent account retest. |
| Parent-school messages | Partial | New parent messages route exists and compile blocker is fixed; route needs end-to-end test after deploy. |
| Parent progress | Improved locally | Duplicate legacy `/parent/track-progress` mock route removed from `ParentRouting.kt`; live route still needs data QA. |
| School discovery by location | Not implemented | No active lat/lng flow from school onboarding to parent discovery. |

---

## 7. Prioritized remediation plan

### P0: Must fix before any further QA

1. Done locally: fixed `ParentRouting.kt` `inList` compile error.
2. Run and require success for:
   ```bash
   ./gradlew :server:compileKotlin -Pserver-only=true --no-daemon
   ./gradlew :shared:compileDevDebugKotlinAndroid :composeApp:compileDevDebugKotlinAndroid --no-daemon
   ```
3. Done locally: removed/redacted sensitive API body/response logging for auth and token responses.
4. Redeploy Render from the successfully compiled commit.
5. Confirm phone backend with:
   - log banner: `Backend -> authBaseUrl=... schoolBaseUrl=...`
   - `GET /api/v1/config/version`
   - `GET /api/v1/config/app-status`
6. Retest screenshots paths: Messages modal, Academic Calendar, Profile gallery.

### P1: Architecture fixes for reported product requirements

1. Implement real media upload infrastructure.
2. Implement current-location/GPS/geocoding and lat/lng persistence.
3. Redesign class/subject/teacher assignment model.
4. Add broadcast audience segmentation.
5. Remove duplicate parent routes and keep only live DB-driven implementations.
6. Add parent school discovery by location.

### P2: Product polish and hidden issue cleanup

1. Replace hardcoded/protected external image URLs.
2. Wire or remove no-op CTAs.
3. Add input validation/content moderation for announcements/messages.
4. Add contract tests for high-risk DTOs (`CalendarSummary`, messages, parent announcements).
5. Add route inventory test to catch duplicate paths.

---

## 8. Immediate checklist for the next developer

1. Fix this compile blocker first:
   - `server/src/main/kotlin/com/littlebridge/vidyaprayag/feature/user/ParentRouting.kt:21`
   - `AnnouncementsTable.schoolId inList schoolIds`
2. Re-run backend compile.
3. Remove secret logging from `safeApiCall`.
4. Delete or split legacy duplicate endpoints from `ParentRouting.kt`.
5. Redeploy Render.
6. Install a fresh APK and confirm backend SHA in `/api/v1/config/version`.
7. Retest:
   - Messages -> Admin Desk thread modal.
   - Academic Calendar -> This Month.
   - Profile -> Add Gallery Photo with a real direct image URL and with an invalid URL.
   - Onboarding -> location flow after GPS implementation.
   - Academic structure -> different classes with different subjects/teachers.
   - Broadcast -> admin all-school and teacher class/subject scoped delivery.

---

## 8b. Re-verification + deploy-hardening (this pass)

This pass re-audited the merged code (`main` @ `b951fc3`, PR #10) and **independently
re-validated every P0 fix by compiling**, then closed the one remaining gap that
made the verification endpoint unreliable on Render.

### What was re-verified (already fixed in code, now proven by compile)

| Item | File | Verified state |
|---|---|---|
| `inList` compile blocker | `feature/user/ParentRouting.kt` | Replaced with an Exposed `or`-reduce over `schoolIds`. No `inList` import remains. |
| Legacy duplicate parent routes | `feature/user/ParentRouting.kt` | Mock `/track-progress` and `/fees` handlers removed; only `parent/scholarships` + DB-backed `parent/announcements` remain. Live owners are `TrackProgressRouting.kt` / `ParentFeesRouting.kt`. |
| Secret leakage in logs | `core/network/NetworkResult.kt` | `redactedBodyText()` + `redactedHeaders()` strip password/otp/code/token/refresh_token/authorization/cookie/api_key. Request/response bodies are sanitized before logging. |
| Calendar `working_days` DTO | client `CalendarModels.kt` / server `SchoolRouting.kt` | Client decodes both `working_days` and `total_working_days` with defaults; server emits **both** fields. No deserialization crash possible. |
| Message thread route | server `MessagesRouting.kt` + `ParentMessagesRouting.kt` | `GET /threads/{id}/messages` and `POST /threads/{id}/read` exist and are registered in `Application.kt`. |
| Version/status verification endpoints | `feature/config/VersionRouting.kt`, `AppStatusRouting.kt` | `GET /api/v1/config/version` and `GET /api/v1/config/app-status` exist and are registered. |

Compile validation run this pass (both **BUILD SUCCESSFUL**):

```bash
./gradlew :server:compileKotlin -Pserver-only=true --no-daemon
./gradlew :shared:compileDevDebugKotlinAndroid :composeApp:compileDevDebugKotlinAndroid --no-daemon
./gradlew :server:installDist -Pserver-only=true --no-daemon   # exactly what Render runs
```

### New deploy-hardening applied this pass

The root cause of the original screenshot failures was **deploy drift** — the phone
hit a stale Render backend. `/api/v1/config/version` is the tool to detect that,
but it only works if the deployed build actually knows its own commit SHA.

1. **`server/build.gradle.kts` — robust git SHA resolution.** On Render the build
   container is frequently a shallow checkout with no usable `.git`, so the old
   `git rev-parse` returned `"unknown"` and the verification endpoint was useless.
   It now prefers CI/PaaS commit env vars first:
   `RENDER_GIT_COMMIT` → `GIT_COMMIT` → `GITHUB_SHA` → `SOURCE_COMMIT` →
   `VIDYAPRAYAG_GIT_SHA`, then falls back to local `git`, then `"unknown"`.
   Verified: with `RENDER_GIT_COMMIT=abc123def456789` the install start script
   bakes in `vidyaprayag.git.sha=abc123def456`; without it, the laptop SHA
   (`b951fc3`) is used.
2. **Added `.dockerignore`.** Keeps the Render build context small/reproducible
   (excludes `build/`, `.gradle`, mobile modules, secrets, runtime DB/logs, `.git`).
   Safe because the Dockerfile always builds with `-Pserver-only=true`, so
   `:composeApp`/`:shared` are never configured in the container.

### Manual actions still required by the team (cannot be automated here)

These need a person with Render/credentials access:

1. **Redeploy Render from this commit** (PR on `backend-by-abuzar`). The fixed
   code is worthless until the live backend is rebuilt from it.
2. **Set Render env vars:** real OTP provider creds, `OTP_DEV_RETURN_CODE=false`,
   explicit console-fallback policy, and `OTP_ADMIN_TOKEN`. See `.env.example`.
3. **After deploy, from the phone browser hit** `GET /api/v1/config/version`
   and confirm `git_sha` matches the deployed commit (not "unknown", not the old SHA).
4. **Rotate** any password/JWT that appeared in previously shared Android logs
   (the leak is fixed going forward, but already-shared logs remain compromised).
5. **Rebuild/reinstall the APK** from current client and confirm `devBaseUrl`
   points at the intended backend (see commit `31d3c2e` note about Render default).
6. **Retest the screenshot paths** once redeployed: Messages → thread modal;
   Academic Calendar → This Month; Profile → Add Gallery Photo (valid + invalid URL).

### Still-open architecture work (P1/P2 — needs product decisions, not a hotfix)

These are tracked but intentionally **not** changed in this pass because they
require schema/migration + product design, not a build fix:

- Real media upload (file picker + storage backend) instead of URL-only entry.
- Current-location/GPS/geocoding + lat/lng persistence and parent distance discovery.
- Per-class subject pools + a real `teacher_subject_assignments` table.
- Broadcast audience segmentation (`ALL_SCHOOL/CLASS/SECTION/SUBJECT/STUDENT/CUSTOM`).
- Contract tests for high-risk DTOs + a route-inventory test for duplicate paths.

---

## 10. Render deployment — log analysis + complete environment-variable plan

> Added after reviewing the post-deploy Render TRACE log and the Render
> Environment screenshot. This section tells the team **exactly** which env
> vars exist, which are missing, and what each one does.

### 10.1 The Render log is NOT showing errors

The pasted log is full of lines like:

```text
/api/v1/school [(authenticate jwt)], segment:2 -> FAILURE "Selector didn't match"
```

These are **normal Ktor `TRACE`-level routing diagnostics**, not failures. For every
incoming request, Ktor walks the whole route tree; each route it tries and skips
prints a `FAILURE "Selector didn't match"`, and the matching one prints `SUCCESS`.
The log actually ends each request with:

```text
Matched routes: "" -> "api" -> "v1" -> "auth" -> "send-otp" -> "(method:POST)"
Routing resolve result: SUCCESS @ /api/v1/auth/send-otp [(method:POST)]
200 OK: POST - /api/v1/auth/send-otp in 1498ms
```

So `check-user`, `send-otp`, and `signup` all returned **200 OK**. The backend is
healthy. The noise is caused by `logback.xml` shipping `<root level="trace">`.

**Action (code):** change the root log level to `info` for production (ideally make
it env-driven via `LOG_LEVEL`). TRACE in production wastes I/O and, combined with the
old body logging, is exactly how secrets leaked before.

### 10.2 Two real things the log reveals

1. **Malformed identifier accepted.** The signup used `abuzarmohd1212gmail.com`
   (missing `@`). The server picks phone-vs-email via `identifier.contains("@")`,
   so a broken email was treated as a **phone number**, all SMS providers were
   "not configured" → skipped, and it fell through to the console code. Fix:
   validate email/phone format on the client AND in `AuthRouting`/`OtpService`
   before sending an OTP.
2. **OTP is running 100% on the console fallback.** Every provider logged
   `reason='not configured', status='skipped'` (`fast2sms`, `msg91`, `twilio`,
   `whatsapp_cloud`) and only `console` succeeded (`CODE: 286912`). This is the
   #1 env gap: **no real OTP delivery provider is configured on Render.**

### 10.3 Env vars currently set on Render (from the screenshot) — KEEP

| Key | Status | Note |
|---|---|---|
| `AUTO_CREATE_TABLES=true` | OK | Creates Exposed tables on boot. Safe; can stay `true`. |
| `DATABASE_URL=jdbc:postgresql://…supabase.com:6543/postgres?sslmode=require` | OK | Supabase **transaction pooler** (port 6543). Works. See note in 10.6. |
| `DATABASE_USER=postgres.dumo1ojpk1zxkzzxdzss` | OK | Pooler username form is correct. |
| `DATABASE_PASSWORD=********` | OK | Set. |

### 10.4 MUST-ADD now (security + correctness)

These are **required for production** and are currently missing on Render:

| Key | Value to set | Why |
|---|---|---|
| `JWT_SECRET` | `openssl rand -hex 64` output | Without it the server uses an **insecure hardcoded dev secret** — anyone can forge tokens. **Highest priority.** |
| `OTP_PEPPER` | `openssl rand -hex 32` output | Server-side pepper for OTP hashing. Missing = weaker OTP hashing + dev fallback value. |
| `OTP_DEV_RETURN_CODE` | `false` | Currently defaults to `true`, which returns the **plain OTP in the API response** — must be off in prod. |
| `OTP_ENABLE_CONSOLE_FALLBACK` | `false` (after a real provider is wired) | Stops printing OTP codes to Render logs. Keep `true` ONLY until a provider is configured. |
| `OTP_ADMIN_TOKEN` | `openssl rand -hex 32` output | When blank, the `/api/v1/admin/otp/*` diagnostics group is unmounted. Set it to enable gated ops/health checks. |
| `LOG_LEVEL` | `info` | Stop TRACE flooding (requires the small logback change in 10.1). |

### 10.5 MUST-ADD: at least ONE real OTP provider

Pick the cheapest path for your audience (India = Fast2SMS or MSG91; global =
Twilio; free = email via SMTP or WhatsApp Cloud). Set ONLY the group you use — the
dispatcher auto-skips any provider whose creds are unset.

**Option A — Fast2SMS (India SMS, simplest):**

| Key | Value |
|---|---|
| `FAST2SMS_API_KEY` | from fast2sms.com → Dev API |
| `FAST2SMS_ROUTE` | `otp` (no DLT template needed) |

**Option B — MSG91 (India, DLT flow):** `MSG91_AUTH_KEY`, `MSG91_FLOW_ID`,
`MSG91_OTP_VAR_NAME=OTP`, `MSG91_SENDER_ID`.

**Option C — Email OTP via SMTP (free, works anywhere):**

| Key | Example |
|---|---|
| `SMTP_HOST` | `smtp.gmail.com` |
| `SMTP_PORT` | `465` |
| `SMTP_USERNAME` | your gmail address |
| `SMTP_PASSWORD` | a Gmail **App Password** (not your login password) |
| `SMTP_FROM` | `VidyaPrayag <noreply@yourdomain.com>` |
| `SMTP_USE_SSL` | `true` |

**Option D — WhatsApp Cloud (free up to 1000 conv/mo):** `WHATSAPP_ACCESS_TOKEN`,
`WHATSAPP_PHONE_NUMBER_ID`, `WHATSAPP_TEMPLATE_NAME`, `WHATSAPP_TEMPLATE_LANG`.

Then set the delivery order, e.g. for SMS-first with email fallback:

| Key | Value |
|---|---|
| `OTP_CHANNEL_ORDER` | `sms,email` (or `email` if you only set SMTP) |

> Note: SMS providers only accept `identifierType == "phone"` and SMTP only accepts
> `"email"`. So if a user signs up with an email, you MUST have an email provider
> (SMTP) configured, otherwise it falls to console again.

### 10.6 OPTIONAL / recommended

| Key | Value | Why |
|---|---|---|
| `PORT` | leave **unset** | Render injects it automatically; the app reads it. Don't hardcode. |
| `DB_POOL_SIZE` | `5` | Supabase pooler has connection limits; keep the Hikari pool small. |
| `DEBUG_ERRORS` | `false` | Avoid leaking stack traces in API error responses. |
| `APP_SEED_CMS` | `true` | Safe; only inserts missing CMS keys. |
| `JWT_ACCESS_MINUTES` / `JWT_REFRESH_DAYS` | `60` / `30` | Defaults are fine. |
| `OTP_EXPIRY_MINUTES` / `OTP_MAX_ATTEMPTS` / `OTP_MAX_RESENDS_PER_HOUR` | `10` / `5` / `5` | Defaults are fine. |

**Supabase pooler caveat:** the current `DATABASE_URL` uses port **6543**
(transaction pooler). For a long-lived JDBC server, Supabase's **session pooler
(port 5432)** is more appropriate and avoids prepared-statement issues under the
transaction pooler. If you see intermittent `prepared statement "S_x" already
exists` errors, switch to the 5432 session-pooler string.

### 10.7 Copy-paste checklist for Render → Environment

```text
# --- security (generate fresh values) ---
JWT_SECRET=<openssl rand -hex 64>
OTP_PEPPER=<openssl rand -hex 32>
OTP_ADMIN_TOKEN=<openssl rand -hex 32>

# --- production OTP hardening ---
OTP_DEV_RETURN_CODE=false
OTP_ENABLE_CONSOLE_FALLBACK=false          # only after a provider below is set
OTP_CHANNEL_ORDER=sms,email                 # match the providers you configure

# --- pick at least one provider group ---
FAST2SMS_API_KEY=...
FAST2SMS_ROUTE=otp
# and/or
SMTP_HOST=smtp.gmail.com
SMTP_PORT=465
SMTP_USERNAME=...
SMTP_PASSWORD=...
SMTP_FROM=VidyaPrayag <noreply@yourdomain.com>
SMTP_USE_SSL=true

# --- ops ---
LOG_LEVEL=info
DEBUG_ERRORS=false
DB_POOL_SIZE=5

# --- already set, keep ---
# AUTO_CREATE_TABLES=true
# DATABASE_URL / DATABASE_USER / DATABASE_PASSWORD
```

After saving, Render auto-redeploys. Then verify from the phone:
`GET /api/v1/config/version` (confirm `git_sha`), then a real OTP send and check
the provider in `otp_delivery_attempts` shows `status='sent'` for your provider
(not `console`).

---

## 11. P1/P2 architecture — recommended solutions and build order

These are intentionally **not** hotfixes; each needs a schema change + UI + a
deliberate product decision. Recommended sequencing puts the lowest-risk,
highest-reuse foundation first (storage), because upload, media, and audience
segmentation all depend on it.

### 11.1 Real media upload (logo / profile / gallery / docs)  — **do first**

**Best option:** Supabase Storage (you already use Supabase for Postgres, so no new
vendor, and it gives public/signed URLs out of the box).

Plan:
1. Create Storage buckets: `school-logos`, `school-gallery`, `profiles`, `docs`.
2. Backend: add `POST /api/v1/uploads` (multipart) that
   - validates content-type (`image/png|jpeg|webp`, `application/pdf`) and size (e.g. ≤5 MB),
   - uploads bytes to Supabase Storage via the Storage REST API using the
     service-role key (new env: `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY`),
   - returns the public/signed URL.
3. Persist returned URLs in the existing `school_media` / `school_philosophy`
   tables instead of free-text URL fields.
4. Client: add an Android file picker / `PickVisualMedia` + multipart upload; replace
   the "paste a URL" boxes; show upload progress and a fallback image on error.
5. Reject non-direct URLs (Google share links, `lh3.googleusercontent.com`) with a
   clear message until upload exists. This directly kills the HTTP 403 / decode
   failures seen in the logs.

**Cheaper interim option:** keep URL entry but add a server-side reachability +
content-type HEAD check before saving, and bundle local placeholder assets for
fallback. Use only as a stop-gap.

### 11.2 Current-location / GPS / geocoding + lat/lng persistence

**Best option:** make `schools` the canonical directory and add geo columns
(avoid maintaining two tables; the `supabase_schema.school_directory` geo fields
are currently disconnected from the Ktor `schools` table).

Plan:
1. Schema: add `latitude DOUBLE`, `longitude DOUBLE`, `full_address`, `city`,
   `district`, `state`, `pincode` to the Ktor `SchoolsTable`.
2. Client onboarding: add a real "Use current location" button →
   `ACCESS_FINE_LOCATION` runtime permission → fused location provider → reverse
   geocode to address; allow manual map-pin/address fallback.
3. Persist `{full_address, city, district, state, pincode, latitude, longitude}`
   in the onboarding payload.
4. Parent side: add `GET /api/v1/parent/schools/nearby?lat=&lng=&radiusKm=` that
   sorts active schools by haversine distance (compute in SQL or in-memory), and
   replace the hardcoded rating `4.5` with a real/derived value or hide it.

### 11.3 Per-class subject pool + real teacher assignment

**Best option:** introduce a proper assignment table; stop sending one global
subject array to every class.

Plan:
1. Schema: `teacher_subject_assignments(id, school_id, faculty_id/user_id,
   class_id, section, subject_id, created_at)` with FKs to `app_users`(faculty),
   classes, and subjects. Replace `SchoolSubjectsTable.teacherAssigned` free text
   with `faculty_id`.
2. Backend onboarding: change `buildAcademicPayload()` so each class carries its
   own subject set (no shared `subjectsArray`); accept per-class subject lists.
3. Client: make the academic UI edit subjects **per class**, and select a teacher
   from real faculty records instead of typing a name.
4. This assignment graph is the prerequisite for teacher-scoped broadcasts (11.4).

### 11.4 Broadcast audience segmentation

**Best option:** add an audience model on announcements + a recipient-expansion
service (depends on 11.3 for class/subject/teacher scoping).

Plan:
1. Schema: add to announcements `audience_type` enum
   (`ALL_SCHOOL`,`CLASS`,`SECTION`,`SUBJECT`,`STUDENT`,`CUSTOM`) and an
   `announcement_recipients` table storing the resolved recipient set (for audit +
   delivery retry).
2. Backend: a `RecipientResolver` that expands an audience into concrete
   parents/students using children/class/subject/teacher-assignment data; gate
   teacher broadcasts to the classes/subjects they actually teach.
3. WhatsApp/notification sync should send to the **resolved** recipient set, not
   "every active parent in the school".
4. Client: add an audience picker in the create-announcement flow.

### 11.5 Cross-cutting: tests to lock it all in (P2)

- Contract tests for high-risk DTOs (`CalendarSummary`, messages, parent
  announcements) so a future field rename can't silently break the client again.
- A **route-inventory test** that fails the build on duplicate method+path
  registrations (prevents the old mock-route shadowing problem from returning).

### Recommended build order

`11.1 storage/upload → 11.2 location → 11.3 subject/teacher model → 11.4 broadcast
segmentation → 11.5 tests`. Storage and the teacher-assignment graph are
foundations the other features reuse, so doing them first avoids rework.

---

## 8c. P1/P2 architecture implementation (this pass)

This pass moved several previously **documentation-only** P1/P2 items into real,
compiled, test-backed backend code on `backend-by-abuzar`. All changes compile
(`:server:compileKotlin -Pserver-only=true`) and the server test suite is green
(`:server:test`).

### Implemented

| Item | Report ref | What was added |
|---|---|---|
| Broadcast audience segmentation | §5.6 | `announcements.audience_type` + `audience_filter` + `author_role` columns. `POST /school/announcements` validates `audience_type` ∈ `{ALL_SCHOOL, CLASS, SECTION, SUBJECT, STUDENT, CUSTOM}` and requires a filter for non-ALL_SCHOOL. `sync-whatsapp` now expands recipients **per announcement** via `resolveRecipientPhones()` instead of blasting every parent. Teacher-authored broadcasts are constrained to the classes/subjects they teach. |
| Teacher ⇄ class ⇄ subject model | §5.5 | New `teacher_subject_assignments` table + `TeacherAssignmentRouting` (`GET/POST/DELETE /api/v1/school/teacher-assignments`). Replaces free-text `teacher_assigned` with a structured, school-scoped assignment graph (upsert + soft delete). Confirmed onboarding already persists **per-class** subject arrays (not one global list) server-side. |
| Geo discovery | §4.4 / §5.7 | `schools.latitude` / `schools.longitude` columns; onboarding BASIC step now persists `latitude`/`longitude` drafts (insert + sync paths). New `GET /api/v1/parent/schools/discover?lat=&lng=&radius_km=&city=&limit=` with Haversine distance sort (nearest-first), city fallback, and name fallback. |
| Contract + inventory tests | §8 P2(4,5) | `CalendarContractTest` locks the `working_days`/`total_working_days` serialization (regression §4.2). `RouteInventoryTest` statically guards against re-introducing the `inList` import and legacy mock `/track-progress`/`/fees` routes, and asserts the live owners + new teacher-assignments route stay registered. Fixed a stale assertion in `ApplicationTest`. |

### Still requires team action / product decisions

- **Media upload (§4.3 / §5.4):** still URL-only. Real file picker + Supabase
  Storage bucket + signed-URL persistence needs storage credentials/bucket
  provisioning that only the team can do; left documented intentionally.
- **Client-side GPS capture (§4.4):** the backend now stores and serves lat/lng
  and exposes distance discovery; the Compose "use current location" permission +
  provider + reverse-geocoding still needs to be wired on the client to fill
  the `latitude`/`longitude` onboarding drafts.
- **STUDENT-scope expansion:** currently falls back to grade match because no
  reliable `children` ⇄ `students.student_code` link exists yet.

---

## 9. Bottom line

The known screenshot issues are real, but the root causes are broader than the visible UI errors. The current merged code contains several intended fixes, yet the backend cannot currently compile, the tested phone build was pointed at stale Render, API logs expose secrets, legacy parent routes can shadow newer live routes, and major product requirements such as location, upload, subject/teacher assignment, and segmented broadcasts remain incomplete.

This working tree now compiles for the backend and Android frontend after the P0 compile/security/frontend-cleanup fixes listed above. Do not treat the school side as production-ready until these changes are committed, Render is redeployed from the fixed SHA, a fresh APK is installed, and the phone verifies the expected backend SHA via `/api/v1/config/version`.
