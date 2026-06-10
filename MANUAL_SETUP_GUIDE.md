# VidyaPrayag — Manual Setup Guide (one-time)

This is the **single, consolidated checklist** of everything a human must do by
hand to take the deployed backend + KMP client from "compiles" to "fully
functional" — the steps no code in the repo can perform for you (creating cloud
resources, pasting secrets, running SQL).

Do these once. After that, media uploads, GPS capture, OTP delivery and audience
segmentation all work end-to-end.

> **Safe-by-default:** the server **never crashes** when these are missing. Media
> uploads return `503 STORAGE_NOT_CONFIGURED`, OTP falls back to the console, and
> geo fields stay null. So you can deploy first and wire these up incrementally.

---

## Quick checklist

| # | Task | Where | Status when done |
|---|------|-------|------------------|
| 1 | Run `migration_002` SQL | Supabase → SQL Editor | geo + segmentation columns exist |
| 2 | Create **public** Storage bucket `school-media` | Supabase → Storage | bucket visible |
| 3 | Set 3 Supabase env vars on Render | Render → Environment | uploads work (no 503) |
| 4 | Set security env vars (`JWT_SECRET`, `OTP_PEPPER`, `OTP_ADMIN_TOKEN`) | Render → Environment | no insecure dev defaults |
| 5 | Wire ≥1 OTP provider + harden OTP flags | Render → Environment | real OTP delivery |
| 6 | (Android) location permission prompt verified on device | device | "Use current location" works |

---

## 1. Run the database migration

Supabase Dashboard → your project → **SQL Editor** → **New query**.

Open `docs/db/migration_002_segmentation_geo_assignments.sql` from the repo, paste
its **entire** contents, click **Run**.

It is safe to re-run — every statement is `IF NOT EXISTS` guarded. It adds:
- `schools.latitude` / `schools.longitude` (+ a geo index) — needed for **GPS capture**.
- announcement audience columns — needed for **broadcast segmentation**.
- the `teacher_subject_assignments` table.
- `children.student_code` — needed for **STUDENT-scope** broadcasts.

> Also run `migration_001_faculty_and_holiday_list.sql` first if you haven't already.

---

## 2. Create the Storage bucket

Supabase Dashboard → **Storage** → **New bucket**.

- **Name:** `school-media`  ← must match exactly (case-sensitive).
- **Public bucket:** **ON** (so uploaded logos/photos/videos are publicly viewable).

That's the only bucket required. (RLS still protects writes — uploads go through
the server using the service-role key, so anonymous clients can't write.)

---

## 3. Supabase env vars on Render

Render → your service → **Environment** → add these three:

| Key | Value | Notes |
|-----|-------|-------|
| `SUPABASE_URL` | `https://<your-project-ref>.supabase.co` | Project base URL (Supabase → Project Settings → API). |
| `SUPABASE_SERVICE_KEY` | the **service_role** secret | ⚠️ **NOT** the `anon` key. Supabase → Project Settings → API → `service_role`. The anon key cannot write to storage and uploads will silently fail with 403. |
| `SUPABASE_BUCKET` | `school-media` | Must exactly match the bucket name from step 2. |

Click **Save** → Render redeploys automatically. After the redeploy, uploads work
end-to-end (onboarding cover/logo, profile gallery, virtual-tour video).

> **How to confirm:** in the app, open School onboarding → Branding, tap the cover
> photo, pick an image. A successful upload shows the image preview. A `503`
> message means one of the three vars above is missing or wrong.

---

## 4. Security env vars (do NOT skip)

Without these, the server uses insecure hardcoded dev defaults.

| Key | Value | Why |
|-----|-------|-----|
| `JWT_SECRET` | a long random string (≥ 32 chars) | Signs auth tokens. Leaving the default makes every token forgeable. |
| `OTP_PEPPER` | a long random string | Hardens OTP hashes against a DB dump. Rotating it invalidates in-flight OTPs. |
| `OTP_ADMIN_TOKEN` | a long random string | Protects OTP admin/debug endpoints. |

Generate each with e.g. `openssl rand -base64 48`.

---

## 5. OTP delivery (pick at least one provider)

Out of the box, OTP runs on the **console fallback** (the code is only printed to
the server log) — fine for testing, not for production.

**Step A — configure a provider.** Pick one and set its keys on Render:

- **Fast2SMS** (India SMS, cheapest): `FAST2SMS_API_KEY`, `FAST2SMS_ROUTE=otp`.
- **MSG91** (DLT-compliant SMS): `MSG91_AUTH_KEY`, template + sender keys.
- **SMTP** (email OTP): `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD`, `SMTP_FROM`.
- **WhatsApp Cloud**: the WhatsApp Cloud API keys.

**Step B — harden the flags** (only after a provider is verified working):

| Key | Set to | Effect |
|-----|--------|--------|
| `OTP_DEV_RETURN_CODE` | `false` | Stop returning the plain OTP in API responses. |
| `OTP_ENABLE_CONSOLE_FALLBACK` | `false` | Stop printing OTPs to the log. |
| `OTP_CHANNEL_ORDER` | e.g. `sms,whatsapp,email` | Delivery preference order. |

Leave `OTP_DEV_RETURN_CODE=true` and console fallback **on** while testing, then
flip both off once real delivery is confirmed.

### Optional / ops env vars

| Key | Recommended | Note |
|-----|-------------|------|
| `PORT` | *(unset)* | Render injects it; don't hardcode. |
| `DB_POOL_SIZE` | `5` | DB connection pool. |
| `DEBUG_ERRORS` | `false` | Hide stack traces in prod responses. |
| `LOG_LEVEL` | `info` | Root log level (env-driven). |
| `AUTO_CREATE_TABLES` | as-is | Keep your existing value. |

> **Supabase pooler caveat:** if `DATABASE_URL` points at the Supabase pooler, use
> port **6543** (transaction pooler), not **5432**, or connections may hang.

---

## 6. Android location permission (GPS capture)

The "Use current location" button in **School onboarding → Institutional Basics**
requests `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` at runtime (declared in
`composeApp/src/androidMain/AndroidManifest.xml`).

- On a **real device / emulator with location**, grant the permission when prompted
  → the app captures lat/lng, reverse-geocodes it, and pre-fills the address.
- On **desktop / web** the button degrades gracefully to manual address entry
  (no crash, just a friendly "enter the address manually" notice).

Nothing to configure server-side — the captured `latitude`/`longitude` are sent in
the BASIC onboarding payload and persisted to `schools.latitude/longitude` (this
requires step 1's migration).

---

## What's already done in code (no action needed)

- ✅ Server media routes (`POST /api/v1/school/media/upload`, `DELETE /api/v1/school/media`) with a dependency-free Supabase Storage wrapper.
- ✅ Cross-platform `MediaApi` + `rememberMediaPicker` (real Android/desktop pickers).
- ✅ All school media surfaces upload **real binaries** — no URL-paste placeholders remain (onboarding cover/logo, profile gallery, virtual tour).
- ✅ Cross-platform `LocationProvider` (real Android GPS + reverse geocoding; graceful stubs elsewhere).
- ✅ Broadcast audience segmentation incl. exact STUDENT scope via `children.student_code`.
- ✅ Premium iOS-style motion primitives (`PremiumAnimations.kt`).

---

_Last updated for branch `backend-by-abuzar`. See `SCHOOL_SIDE_STATUS_REPORT.md` for the full engineering log._
