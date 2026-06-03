# VidyaPrayag — School Side Status Report

> **Branch analyzed:** `main`
> **HEAD at audit time:** `0e33daf` plus local fixes applied in this working tree
> **Audit date:** 2026-06-03
> **Scope:** Entire **School/Admin** side — Compose UI (`composeApp`), shared client logic (`shared`), and Ktor backend (`server`).
> **Purpose:** Show what is working, what is not working, current vs desired result, and every known connection/placeholder gap so the school side can be brought to production quality.

---

## 0. Immediate finding from attached screenshot/log

### 0.1 401 on `POST /api/v1/onboarding/submit`

The screenshot error is caused by this authenticated request returning 401:

- **Endpoint:** `POST https://vidyaprayag-1.onrender.com/api/v1/onboarding/submit`
- **Server response:** `{"success":false,"message":"Session expired, please login again","errorCode":"UNAUTHORIZED"}`
- **UI shown:** “Your session was rejected by the server…”

**Current result:** The BASIC onboarding form stays on screen and shows a manual logout/re-login instruction. The debug logger also printed the raw Authorization header in Android Studio logs.

**Root cause:** The JWT was rejected by the server. This happens when the token is expired, signed by a different backend/secret, or the app is pointed to a different backend than the one that issued the token. The app is currently calling Render (`vidyaprayag-1.onrender.com`).

**Fix applied in this working tree:**

1. `shared/.../core/network/NetworkResult.kt`
   - Redacts sensitive headers (`Authorization`, cookies, API keys) before printing request headers.
   - This prevents bearer-token leakage in future Android Studio logs.

2. `shared/.../feature/admin/presentation/*OBViewModel.kt`
   - On 401 during onboarding submit/load flows, clears the local session via `PreferenceRepository.clearSession()`.
   - BASIC, BRANDING, ACADEMIC and REVIEW/LAUNCH now use the same sign-in-again behavior instead of leaving the user stranded with a rejected token.

3. `composeApp/.../App.kt`
   - NavHost is now keyed by auth token/role so clearing the token recreates navigation with `Destination.Landing`.
   - This prevents the user being stranded on an authenticated screen after session invalidation.

### 0.2 Coil HTTP 403 image failure

The log also shows Coil failing to load a temporary Google-hosted `lh3.googleusercontent.com/aida/...` image with HTTP 403.

**Affected screen:** `InstitutionalBasicOBScreen.kt` location/map preview.

**Fix applied:** Replaced that remote image with a local deterministic Compose map preview, and removed additional onboarding review default `lh3.googleusercontent.com/aida/...` image usage so onboarding no longer depends on expiring external image URLs.

---

## 1. Status legend

| Status | Meaning |
|---|---|
| ✅ **Working** | UI → ViewModel → Repository/API → Server → DB is wired with real data or real mutation, with loading/error handling. |
| 🟡 **Partial** | Core route works, but some UI controls are still no-op, CMS-seeded, cosmetic, or missing deeper production logic. |
| 🔴 **Broken / Placeholder** | Feature is unreachable, not connected, returns placeholder-only data, or has a visible no-op. |

---

## 2. Architecture and connection map

The codebase is Kotlin Multiplatform, not Flutter:

- `composeApp/` — Compose Multiplatform UI screens and navigation.
- `shared/` — KMP ViewModels, repositories, DTOs, Ktor API clients, preferences, Koin DI.
- `server/` — Ktor backend routes, DB access, auth, and school-scoped endpoints.

### 2.1 School client-side route registration

All school/admin screens are registered in `composeApp/.../navigation/NavGraph.kt`:

- `SchoolDashboard`
- `InstitutionalBasicOB`, `BrandingInfoOB`, `AcademicInfoOB`, `LaunchInfoOB`
- `InstitutionalProfile`
- `AdmissionCRMDashboard`
- `SchoolAnnouncements`
- `Messages`
- `SchedulePTM`
- `AcademicCalendar`
- `DailyAttendance`
- `LeaveRequests`
- `Results`
- `AnalyticsDashboard`
- `StudentAnalytics`
- `TeacherPerformance`
- `ClassPerformance`
- `SyllabusCoverage`

### 2.2 Backend endpoint inventory for school side

| Area | Backend route file | Endpoints |
|---|---|---|
| User details/dashboard state | `feature/user/UserDetailsRouting.kt` | `GET /api/v1/user/details` |
| Onboarding | `feature/onboarding/OnboardingRouting.kt` | `GET /api/v1/onboarding/step`, `GET /api/v1/onboarding/academic/class-details`, `POST /api/v1/onboarding/submit` |
| School dashboard | `feature/school/SchoolDashboardRouting.kt` | `GET /api/v1/school/dashboard` |
| Calendar/holidays/attendance | `feature/school/SchoolRouting.kt` | `GET /api/v1/school/calendar`, `GET /api/v1/school/holidays`, `GET /api/v1/school/attendance/daily` |
| Admissions | `feature/admissions/AdmissionRouting.kt` | `GET /api/v1/admissions/enquiries`, `GET /summary`, `PATCH /{id}/status` |
| Announcements | `feature/announcements/AnnouncementRouting.kt` | `GET /api/v1/school/announcements/search`, `POST /sync-whatsapp` plus base announcement list/create handlers in the same route block |
| Messages | `feature/school/MessagesRouting.kt` | `GET /threads`, `GET /threads/{id}/messages`, `POST /threads/{id}/read`, `POST /messages` |
| Leave requests | `feature/school/LeaveRequestsRouting.kt` | `GET /api/v1/school/leave-requests`, `PATCH /{id}/status` |
| PTM | `feature/school/PtmRouting.kt` | `GET /api/v1/school/ptm`, `POST /api/v1/school/ptm`, `PATCH /{id}/metrics`, `PUT /{id}/class-progress`, `POST /{id}/complete` |
| Results | `feature/school/ResultsRouting.kt` | `GET /api/v1/school/results` and supporting result selectors in the same route |
| Analytics | `feature/school/SchoolAnalyticsRouting.kt` | `GET /overview`, `/class-performance`, `/teacher-performance`, `/student/{studentId}`, `/syllabus-coverage`, `/student-cohort` |
| Profile | `feature/user/UserProfileRouting.kt` | `GET /api/v1/user/profile`, `PUT /philosophy`, `PUT /tour-videos`, `PUT /gallery`, `PUT /visibility` |

---

## 3. Feature-by-feature status matrix

| # | Feature | UI | Client connection | Server/DB connection | Status | Current vs desired |
|---|---|---|---|---|---|---|
| 1 | School onboarding — BASIC | `InstitutionalBasicOBScreen.kt` | `InstitutionalBasicOBViewModel` → `OnboardingRepository` → `OnboardingApi.submitStep` | `OnboardingRouting.post('/submit')` persists BASIC payload | ✅ Working after session fix | Current: saves real school basics when token valid. Desired: same, plus better form validation and editable address. |
| 2 | School onboarding — BRANDING | `BrandingInfoOBScreen.kt` | ViewModel submits `logo_url` + `brand_color` | Backend stores branding payload | 🟡 Partial | Current: primary submit works; Save Draft submits current data; logo/cover controls now populate stable URLs instead of no-op; mission/vision/tour are UI-only unless backend schema accepts them later. Desired: persist all branding fields and replace URL helper with real storage upload. |
| 3 | School onboarding — ACADEMIC | `AcademicInfoOBScreen.kt` | Loads classes + class details, submits academic structure | Backend creates/updates classes and subjects | ✅ Working | Current equals desired for core class/subject setup. |
| 4 | School onboarding — LAUNCH/REVIEW | `LaunchInfoOBScreen.kt` | Loads review payload, final submit completes onboarding | Backend finalizes school and user school link | 🟡 Partial | Current: final launch works; Save Draft now gives explicit local-review feedback, upload button marks documents for verification instead of doing nothing, and default expiring image URL was removed. Desired: real document picker/storage/verification flow. |
| 5 | School dashboard | `SchoolDashboardScreen.kt` | `SchoolDashboardViewModel` uses `/user/details` | User details includes onboarding details | ✅ Working | Current: correctly changes pre/post onboarding. Desired: add quick-action cards so drawer-only features are discoverable. |
| 6 | Institutional profile | `InstitutionalProfileScreen.kt` | `UserProfileRepository` + API | `UserProfileRouting.kt` | ✅ Working | Current: save philosophy, gallery URLs, tour URLs, public visibility. Desired: replace URL-only media entry with file upload/storage. |
| 7 | Admissions CRM | `AdmissionCRMDashboard.kt` | `AdmissionCRMViewModel` | `AdmissionRouting.kt` | ✅ Working | Current: list, summary, status mutation, PTM scheduling entry path. Desired: remove remaining secondary no-op and add richer filters/export. |
| 8 | Announcements | `SchoolAnnouncementsScreen.kt` | `SchoolAnnouncementsViewModel` | `AnnouncementRouting.kt` | 🟡 Partial | Current: list/search/filter/create/sync wired. Some secondary controls remain no-op and participant avatars are cosmetic. Desired: every CTA wired or removed. |
| 9 | Messages | `MessagesScreen.kt` | `MessagesViewModel` | `MessagesRouting.kt` | ✅ Core working / 🟡 one UI no-op | Current: threads, conversation, send, read-state work. Desired: header action button should be wired or removed. |
| 10 | Academic calendar | `AcademicCalendarScreen.kt` | `AcademicCalendarViewModel` | `SchoolRouting.get('/calendar')` | ✅ Mostly working | Current: real month grid is computed from API events and visible month; sync/holiday filter work in-memory; bulk upload card now triggers the existing sync flow instead of a no-op. Desired: dedicated create/edit/import calendar events and first-class holidays UI. |
| 11 | Daily attendance | `DailyAttendanceScreen.kt` | `DailyAttendanceViewModel` | `SchoolRouting.get('/attendance/daily')` | 🟡 Partial | Current: data loads for student/faculty and grade/date params. Date/see-all control remains no-op. Desired: date picker + paginated/all list + mutation if school marks attendance. |
| 12 | Leave requests | `LeaveRequestsScreen.kt` | `LeaveRequestsViewModel` | `LeaveRequestsRouting.kt` | ✅ Working | Current: list + approve/reject scoped to school. Desired: optional detail screen/attachments. |
| 13 | Results | `ResultsScreen.kt` | `ResultsViewModel` | `ResultsRouting.kt` | ✅ Working | Current: real test/class/subject selectors, distribution, trend, search. Desired: upload/edit result records if school staff need write access. |
| 14 | Schedule PTM | `SchedulePTMScreen.kt` | `SchedulePTMViewModel` | `PtmRouting.kt` | 🟡 Partial | Current: list/create PTM works; several secondary buttons are no-op. Desired: wire metrics, class progress, complete-flow, detail/share actions. |
| 15 | Analytics overview | `AnalyticsDashboardScreen.kt` | `AnalyticsDashboardViewModel` | `SchoolAnalyticsRouting.get('/overview')` | 🟡 Partial | Current: trend/growth now can be live attendance-based; cards/insights still CMS template with attendance patch. Desired: all cards/insights computed per school. |
| 16 | Student analytics cohort | `StudentAnalyticsScreen.kt` | `StudentAnalyticsViewModel` | `/analytics/student-cohort` | 🟡 Partial | Current: live volatility and cohort comparison can be computed; risk buckets and at-risk list are still CMS/hybrid. Desired: full risk scoring from real attendance/results/engagement. |
| 17 | Teacher performance | `TeacherPerformanceScreen.kt` | `TeacherPerformanceViewModel` | `/analytics/teacher-performance` | 🟡 Partial | Current: star faculty ranking is live from attendance/faculty; rest is CMS. Desired: teacher metrics from real PTM, attendance, results, feedback. |
| 18 | Class performance | `ClassPerformanceScreen.kt` | `ClassPerformanceViewModel` | `/analytics/class-performance` | 🟡 Partial | Current: active students can be live; rest CMS. Desired: class averages, proficiency, median, risk list from real results/attendance. |
| 19 | Syllabus coverage | `SyllabusCoverageScreen.kt` | `SyllabusCoverageViewModel` | `/analytics/syllabus-coverage` | 🟡 Partial | Current: subject coverage/overall can be overlaid from exam results; alerts/milestones still CMS. Desired: coverage based on actual syllabus plan and chapter completion records. |

---

## 4. What is working now

### 4.1 Authentication and session basics

- Auth uses JWT bearer tokens validated by Ktor auth (`SecurityModule.kt`).
- Protected school endpoints are under `authenticate("jwt")`.
- `requireSchoolContext()` is used across school routes to scope by real user/school instead of trusting only JWT role claims.
- Local session is persisted through `PreferenceRepository`.
- **Fix applied:** Onboarding 401 handling now clears local session across BASIC, BRANDING, ACADEMIC and REVIEW/LAUNCH, and app navigation resets to landing.

### 4.2 Onboarding core path

The main school onboarding journey is functionally wired:

1. BASIC posts `school_name`, `board`, `contact_email`, `contact_phone`, `full_address`.
2. BRANDING posts `logo_url` and `brand_color`.
3. ACADEMIC loads active classes and class details, then posts real class/subject structure.
4. REVIEW loads accumulated review data and final submit completes onboarding.

### 4.3 Dashboard and navigation

- Dashboard reads `/api/v1/user/details` and reflects onboarding state.
- Drawer and bottom bar expose the main school modules.
- All school feature screens are registered in NavGraph.

### 4.4 School operations

- Admissions enquiry list/status update is school-scoped.
- Announcements search/filter/create/sync route exists.
- Messages have real thread/message/read/send flow.
- Leave requests support approve/reject.
- Results screen uses real selectors and computed server values.
- Calendar month grid is now real in the UI (not the older static week described in previous report revisions).

---

## 5. What is still not production-complete

### 5.1 Remaining no-op controls

Static scan after the fixes still finds these school-side no-op handlers:

| File | Line approx | Control/gap | Desired action |
|---|---:|---|---|
| `AdmissionCRMDashboard.kt` | 222 | secondary `TextButton` | Wire to full enquiry list/detail/export or remove. |
| `AnalyticsDashboardScreen.kt` | 258, 302 | analytics card/header actions | Navigate to drilldowns/export/report filters or remove. |
| `DailyAttendanceScreen.kt` | 217 | `TextButton` | Open date picker or full attendance list. |
| `MessagesScreen.kt` | 428 | action button | New message/search/filter action or remove. |
| `SchedulePTMScreen.kt` | 237, 337, 402, 414 | several secondary buttons/icons | Wire to detail, reschedule, share, complete, metrics actions. |
| `SchoolAnnouncementsScreen.kt` | 357, 453 | “see all”/secondary actions | Wire to full list/audience details or remove. |
| `StudentAnalyticsScreen.kt` | 318 | icon button | Wire to export/filter/detail. |
| `SyllabusCoverageScreen.kt` | 182, 269 | action button/icon | Wire to syllabus sync/export/detail. |
| `TeacherPerformanceScreen.kt` | 350 | action button | Wire to teacher detail/export. |
| `LaunchInfoOBScreen.kt` | — | Compliance upload is now locally actionable | Still needs real file picker/storage/verification API for production. |
| `AcademicCalendarScreen.kt` | — | Bulk upload card now calls sync | Still needs real CSV/PDF import endpoint for production. |

`placeholder = "..."` in text fields is normal hint text, not a bug.

### 5.2 Analytics remains hybrid/CMS-driven

The analytics server has improved from the older report, but it is still not fully real analytics.

| Endpoint | Real/live today | Still CMS/template today | Desired result |
|---|---|---|---|
| `/analytics/overview` | Monthly attendance trend, trend labels, growth when attendance exists; first card attendance patch | Most card definitions and insight copy | All cards and insights computed from school data. |
| `/analytics/class-performance` | `summary.active_students` live | Proficiency, median, trend, risk/subject sections | Compute from exam results + attendance. |
| `/analytics/teacher-performance` | `star_faculty` from faculty + attendance | Most teacher KPIs/charts | Compute per teacher from attendance, PTM, result outcomes. |
| `/analytics/student/{studentId}` | Student header, attendance, average, rank | Subjects, milestones, narrative | Compute subjects/milestones/narrative from real records. |
| `/analytics/syllabus-coverage` | Subject coverage and overall can come from exam result proxy | Alerts/milestones; no real curriculum schedule | Add syllabus/chapter completion tables and compute coverage from them. |
| `/analytics/student-cohort` | active low-risk count, volatility, cohort comparison | critical/medium risk buckets and at-risk list | Real risk model from attendance + result decline + engagement. |

### 5.3 Server route naming issue

`SchoolRouting.kt` previously had `GET /api/v1/school/analytics` returning a public “coming soon” response, while real analytics live under `/api/v1/school/analytics/*` in `SchoolAnalyticsRouting.kt`.

**Fix applied:** The base endpoint now returns an endpoint-index style response pointing clients to `/overview`, `/class-performance`, `/teacher-performance`, `/student/{studentId}`, `/student-cohort`, and `/syllabus-coverage` instead of saying the feature is coming soon.

**Desired:** In a later backend cleanup, either remove the base endpoint or make it return the authenticated overview directly.

### 5.4 Media upload is not complete

Several screens accept URLs rather than uploading files:

- Branding logo/cover/tour.
- Institutional profile gallery/tour videos.
- Launch compliance document upload.

**Desired:** Use a real upload path/storage provider and persist returned URLs.

### 5.5 Address/location is still not real geolocation

BASIC onboarding sends a default full address and shows a local map preview.

**Current:** The old external map image issue is fixed, but this is not real geolocation.

**Desired:** Editable address fields + optional device location permission + map picker/geocoding.

---

## 6. Current vs desired result by major workflow

### 6.1 School onboarding

- **Current:** Main flow works with a valid token. Expired/rejected token now clears session and returns app to login across onboarding steps. BASIC no longer loads a 403 remote map image, and REVIEW no longer uses an expiring default school image URL.
- **Desired:** Add form validation, editable address/location, document upload, and persistence for all branding/review fields.

### 6.2 School daily operations

- **Current:** Admissions, announcements, messages, leave requests, calendar, attendance, PTM, and results have real API routes and most primary flows are wired.
- **Desired:** Remove remaining no-op secondary buttons and add detail/export/date-picker/media workflows.

### 6.3 Analytics

- **Current:** Hybrid. Some server values are real, but many cards/lists/insights remain seeded CMS rows.
- **Desired:** No seeded metric should be presented as live. Every numeric card/chart/list must be either computed per school or clearly labelled as configured content.

### 6.4 API/security logging

- **Current after fix:** Authorization headers are redacted in client API logs.
- **Desired:** Keep redaction; avoid logging request bodies containing sensitive PII in release builds.

---

## 7. Prioritized fix plan

### P0 — Stop session and debug pain

1. ✅ Redact Authorization headers in `NetworkResult.kt`.
2. ✅ Clear session on BASIC onboarding 401 and reset navigation on token removal.
3. 🔲 Apply the same unauthorized-session clearing pattern to every ViewModel/API flow, not only BASIC onboarding.
4. 🔲 Add a single global session-expired handler so each screen does not implement 401 logic separately.

### P1 — Remove visible placeholders/no-ops

1. Wire or remove the no-op handlers listed in §5.1.
2. Replace Launch Upload with a real document picker/storage upload or hide it until supported.
3. Remove or repurpose `GET /api/v1/school/analytics` “coming soon”.

### P2 — Make analytics production-real

1. Replace CMS-only metrics with real aggregation queries.
2. Add curriculum/syllabus completion data model so syllabus coverage is not inferred from exam scores.
3. Implement risk scoring for student analytics.
4. Add teacher KPI computation from faculty attendance, PTM, results, and feedback records.

### P3 — UX completeness

1. Add school dashboard quick-action cards for Calendar, Attendance, Leave, Results, PTM, Analytics.
2. Add full date picker to attendance/calendar screens.
3. Add media/document upload across onboarding/profile.
4. Add empty-state copy for every feature when a newly onboarded school has no data yet.

---

## 8. Validation checklist

Run these after pulling this working tree:

```bash
./gradlew :shared:compileKotlinAndroid :composeApp:compileDevDebugKotlinAndroid
./gradlew :server:compileKotlin -Pserver-only=true
./gradlew :composeApp:assembleDevDebug
```

Manual app checks:

1. Log in as a school admin against the same backend that the app points to.
2. Complete BASIC onboarding with a fresh token: expect success and no 401 banner.
3. Force an expired token: expect local session clear and return to landing/login.
4. Check Android Studio logs: Authorization header must show `[REDACTED]`.
5. Reopen BASIC onboarding: no Coil 403 should appear for the map image.
6. Visit every drawer/bottom-bar school feature and verify no primary flow is blocked.
7. Search for remaining no-ops:

```bash
rg -n "onClick\s*=\s*\{\s*\}|clickable\s*\{\s*\}|/\* Save draft \*/" composeApp/src/commonMain/kotlin/com/littlebridge/vidyaprayag/ui/screens/admin
```

---

## 9. Summary

The school side is **not just placeholder code**: onboarding, dashboard, profile, admissions, messages, leave requests, results, and much of calendar/attendance/PTM are genuinely connected. The main remaining production blockers are:

1. Session-expired handling needs to be global, not screen-specific.
2. Several secondary UI buttons still do nothing.
3. Analytics is still hybrid: some live overlays, many CMS-seeded values.
4. Media/document upload is not a real file flow yet.
5. Dashboard discoverability should improve for drawer-only tools.

This file should be kept updated as each placeholder/no-op is removed.
