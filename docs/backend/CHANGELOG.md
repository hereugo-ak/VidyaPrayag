# Backend Changelog — `backend-by-abuzar`

## 2026-05-20 — Initial spec-compliant backend

Implements every endpoint listed in `vidya_prayag_api_spec.artifact.md` and
`vidya_prayag_api_spec2.artifact.md`.

### Added (new files)

#### Core infrastructure
- `core/ApiResponse.kt` — uniform { success, message, data } envelope DTO
- `core/ResponseExtensions.kt` — `call.ok/created/accepted/fail` helpers
- `core/JwtConfig.kt` — HMAC256 JWT issue + verify, env-driven
- `core/SecurityModule.kt` — Ktor Authentication plugin config + principal helpers
- `core/ErrorHandling.kt` — StatusPages → uniform error JSON envelope

#### Database layer
- `db/Tables.kt` — Exposed tables for schools, onboarding drafts, classes,
  subjects, announcements, whatsapp_logs, admission_enquiries, philosophy,
  media, storage metrics, landing CMS, app config, calendar, holidays,
  students, faculty, attendance records
- `db/Seed.kt` — idempotent first-run seeder (landing copy, app flags,
  demo school + 3 announcements, 5 enquiries, classes, subjects, calendar
  events, holidays, students/faculty, today's attendance)

#### Feature routes
- `feature/content/LandingRouting.kt` — `GET /api/v1/content/landing`
- `feature/config/AppStatusRouting.kt` — `GET /api/v1/config/app-status`
- `feature/auth/AuthRouting.kt` — `/api/v1/auth/{check-user,signup,login,send-otp}`
  (plus legacy `/auth/*` aliases for back-compat with shared/AuthApi.kt)
- `feature/user/UserDetailsRouting.kt` — `GET /api/v1/user/details`
- `feature/user/UserProfileRouting.kt` — `GET /profile`, `PUT /profile/{philosophy,tour-videos,gallery}`
- `feature/onboarding/OnboardingRouting.kt` — `GET /onboarding/step`,
  `GET /onboarding/academic/class-details`, `POST /onboarding/submit`
- `feature/announcements/AnnouncementRouting.kt` — `GET`, `GET /search`,
  `POST /sync-whatsapp` (mocked)
- `feature/admissions/AdmissionRouting.kt` — `GET /enquiries/summary`,
  `GET /enquiries` (paginated)
- `feature/school/SchoolRouting.kt` — `GET /analytics`, `/calendar`,
  `/holidays`, `/attendance/daily`

#### Docs
- `docs/backend/README.md` — high-level architecture + endpoint index
- `docs/backend/01-landing.md` through `09-drawer.md` — one doc per feature
- `docs/backend/10-database.md` — schema reference
- `docs/backend/MANUAL_STEPS.md` — DevOps actions still required
- `docs/backend/CHANGELOG.md` — this file

### Modified

- `server/build.gradle.kts` — added ktor-server-status-pages, -cors, -auth,
  -auth-jwt, -call-logging
- `server/src/main/kotlin/.../Application.kt` — installs CORS,
  CallLogging, Authentication("jwt"), StatusPages; mounts all feature routes
- `server/src/main/kotlin/.../db/DatabaseFactory.kt` — registers all new
  tables in `allTables`; calls `Seed.populateIfEmpty()` after schema setup
- `server/src/main/kotlin/.../db/UserTable.kt` — added columns
  `profile_pic`, `profile_completed`, `refresh_token`

### Removed

- `server/src/main/kotlin/.../auth/AuthRouting.kt` (legacy, replaced by
  `feature/auth/AuthRouting.kt`)

### Backward compatibility

- All four legacy auth endpoints (`/auth/check-user`, `/auth/signup`,
  `/auth/login`, `/auth/send-otp`) continue to work. They share the same
  handlers as `/api/v1/auth/*`. Remove the alias once `shared/AuthApi.kt`
  is migrated.

### Known limitations / future work

See `MANUAL_STEPS.md` for the full list. Highlights:
- Password hashing is still SHA-256 — upgrade to BCrypt.
- WhatsApp sync is mocked.
- File upload endpoint not implemented (PUT bodies accept URLs only).
- No Redis cache layer (acceptable for current scale).
- Mobile-side `shared/.../*Api.kt` clients need to be added/updated to call
  the new `/api/v1/*` endpoints.
