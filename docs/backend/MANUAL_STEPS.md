# Manual Steps — Things YOU Need to Do

The backend in `backend-by-abuzar` is fully functional with sensible
dev-defaults, but a few items require human / DevOps decisions that I
deliberately did **not** automate. Each one is listed here with the
exact command / file to touch.

---

## 1. Production database (REQUIRED before deploy)

The backend transparently falls back to a local SQLite file (`data.db`) when
no `DATABASE_URL` env is set. For production:

```bash
# .env (or Render / Railway / Supabase dashboard)
DATABASE_URL=postgres://postgres:[PASSWORD]@db.[REF].supabase.co:5432/postgres
DATABASE_USER=postgres
DATABASE_PASSWORD=[PASSWORD]
```

Example for Render PostgreSQL:
```
DATABASE_URL=jdbc:postgresql://dpg-XXXX.oregon-postgres.render.com/vidyaprayag?user=USER&password=PASS
```

`DatabaseFactory.kt` adds the `jdbc:` prefix automatically if you omit it.

## 2. JWT secret (REQUIRED before deploy)

In `.env` / Render env vars:

```bash
JWT_SECRET=<run: openssl rand -base64 64>
```

Optional overrides:

```bash
JWT_ISSUER=vidyaprayag-api
JWT_AUDIENCE=vidyaprayag-app
JWT_REALM=vidyaprayag
JWT_EXPIRY_SECS=604800
```

The dev fallback (`vidyaprayag-dev-secret-change-me`) is **insecure**. Leaving
it as-is in prod is treated as a critical security finding.

## 3. Replace SHA-256 with BCrypt (recommended)

`feature/auth/AuthRouting.kt` → `hashPassword()` currently uses SHA-256
because BCrypt isn't on the classpath yet. Add to `server/build.gradle.kts`:

```kotlin
implementation("at.favre.lib:bcrypt:0.10.2")
```
and update the helper. Existing users will need a one-time password reset.

## 4. Wire WhatsApp Business API (currently mocked)

`feature/announcements/AnnouncementRouting.kt::syncWhatsApp` currently:
- marks announcements as `synced_to_wa = true`
- inserts `whatsapp_logs` rows with status `QUEUED`
- returns a fake `job_id`

To make it real:

- Sign up for Twilio's WhatsApp Business or Meta Cloud API.
- Add `WHATSAPP_API_TOKEN` env var.
- Inside the `dbQuery` loop, replace the `WhatsappLogTable.insert { … }` block
  with an `HttpClient.post(...)` to the provider API.
- Move long-running work to a background queue (Redis + a worker) so HTTP
  responses stay snappy.

## 5. File / media upload provider (optional)

The spec's PUT `/profile/gallery` and `/profile/tour-videos` accept **URLs**,
not raw files. If you want users to upload from the app:

1. Pick a provider: Supabase Storage / Cloudinary / AWS S3.
2. Add a new endpoint `POST /api/v1/uploads` that:
   - receives multipart form data,
   - uploads to the provider,
   - returns `{ "url": "…" }`.
3. Mobile UI: upload first, then PUT the returned URLs into the gallery.

I deliberately left this out because the spec doesn't define an upload
endpoint and your UI hasn't built that flow yet.

## 6. Redis caching (optional / scale)

Spec mentions Redis TTLs (24h for landing, 5min for app-status). For now the
backend serves fresh from DB — plenty fast for <1k QPS. When you need it:

```kotlin
// In LandingRouting.kt / AppStatusRouting.kt
val cached = redis.get("landing:v1") ?: dbQuery { … }.also { redis.setex("landing:v1", 86400, it) }
```
Add `io.lettuce:lettuce-core` and wire from env `REDIS_URL`.

## 7. Mobile-side API client migration

The existing `shared/src/commonMain/.../feature/auth/data/remote/AuthApi.kt`
points to `/auth/*` (no `/api/v1` prefix). I kept those paths alive as
**aliases** so the app keeps working — but please migrate `AuthApi.kt` to use
`/api/v1/auth/*` and update its DTOs to:

- send `identifier` instead of `contact`
- accept `data.token`, `data.user_id`, `data.role` from inside the envelope

After migration, delete the legacy block at the bottom of `AuthRouting.kt`
(the `listOf(v1, legacy).forEach { base -> … }` loop becomes
`route(v1) { … }`).

You will also need to create matching `…Api.kt` classes in `shared/` for the
new endpoints (Landing, AppStatus, UserDetails, Onboarding, Announcements,
Admissions, Profile, School). Use `AuthApi.kt` as the template — it already
imports `HttpClient` from Koin.

## 8. Verify on a real device

```bash
cd /home/user/webapp
./gradlew :server:run
```

On the same Wi-Fi, find your machine's LAN IP (`ifconfig | grep inet`) and
have the mobile build point at `http://<ip>:8080`. The CORS plugin is set to
`anyHost()` to make this painless during dev — restrict it for production.

---

When in doubt, refer to the per-feature docs in this folder. Every routing
file also has a header comment block that lists the exact tables it touches
and any TODOs.
