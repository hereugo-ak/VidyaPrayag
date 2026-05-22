# VidyaPrayag Backend — Local Setup & Postman Testing Guide

> **Audience**: You (the backend owner) on Windows / macOS / Linux, using
> Command Prompt (CMD), PowerShell, or a Unix shell.
> **Goal**: From "I have the old zip on Desktop" → "I am testing every API
> against Supabase from Postman with real phone numbers."

The guide is split into **10 numbered phases**. Do them in order the first
time; later you'll only repeat Phases 6–10 for day-to-day testing.

> ⚠️ This guide replaces all previous local-setup notes. The backend now
> uses an industrial-grade OTP service (`auth_otps` table) instead of the
> previous hardcoded `123456` OTP.

---

## Table of contents

1. [Sync your local repo with the new `backend-by-abuzar` branch](#1-sync-your-local-repo-with-the-new-backend-by-abuzar-branch)
2. [Install JDK 21 + verify Gradle](#2-install-jdk-21--verify-gradle)
3. [Get your Supabase connection string](#3-get-your-supabase-connection-string)
4. [Run the SQL migrations in Supabase (one-time)](#4-run-the-sql-migrations-in-supabase-one-time)
5. [Create your local `.env` file](#5-create-your-local-env-file)
6. [Boot the Ktor server locally](#6-boot-the-ktor-server-locally)
7. [Import the Postman collection + environment](#7-import-the-postman-collection--environment)
8. [End-to-end test flow with a real phone number](#8-end-to-end-test-flow-with-a-real-phone-number)
9. [Manually seed test data in Supabase (since the backend no longer auto-seeds)](#9-manually-seed-test-data-in-supabase-since-the-backend-no-longer-auto-seeds)
10. [Industrial-grade OTP — what's protecting you and how it works](#10-industrial-grade-otp--whats-protecting-you-and-how-it-works)

---

## 1. Sync your local repo with the new `backend-by-abuzar` branch

You said you already have the previous repo zip downloaded to your Desktop.
Three scenarios — pick the one that matches you.

### 1.A You already cloned with `git`

Open CMD / PowerShell and `cd` into the project folder you already have:

```cmd
cd %USERPROFILE%\Desktop\Vidyaprayag
git fetch origin
git checkout backend-by-abuzar
git pull origin backend-by-abuzar
```

If `git pull` complains about local edits, stash them first:

```cmd
git stash
git pull origin backend-by-abuzar
git stash pop
```

### 1.B You downloaded a ZIP from GitHub (not a git clone)

A ZIP download has no `.git/` folder, so you cannot `git pull` into it.
The cleanest fix is a fresh clone next to it:

```cmd
cd %USERPROFILE%\Desktop
git clone https://github.com/<your-org>/Vidyaprayag.git Vidyaprayag-new
cd Vidyaprayag-new
git checkout backend-by-abuzar
```

Once everything works, delete the old folder.

### 1.C You don't have `git` installed

Install it from <https://git-scm.com/download/win>. After install, restart
CMD so `git` is on your PATH and re-do **1.A** or **1.B**.

### Verify

After the steps above:

```cmd
git branch
```

should show `* backend-by-abuzar` (the `*` matters — that's the active branch).

---

## 2. Install JDK 21 + verify Gradle

The server is Ktor 3.4.3 + Kotlin 2.2.10 targeting **JVM 21**. You need JDK 21
locally. Earlier versions will fail with a `Could not target platform` error.

### Windows (recommended: Temurin / Adoptium)

1. Download **Eclipse Temurin 21 LTS (Windows x64 MSI)** from
   <https://adoptium.net/temurin/releases/?version=21>.
2. Run the installer. Tick **"Set JAVA_HOME variable"** and **"Add to PATH"**.
3. Open a *new* CMD (env vars only refresh in new shells):

   ```cmd
   java -version
   ```

   You should see `openjdk version "21..."`. If not, set it manually:

   ```cmd
   setx JAVA_HOME "C:\Program Files\Eclipse Adoptium\jdk-21.x.x-hotspot"
   setx PATH "%JAVA_HOME%\bin;%PATH%"
   ```

   Close & reopen the terminal after `setx`.

### macOS

```bash
brew install --cask temurin@21
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 21)' >> ~/.zshrc
source ~/.zshrc
java -version
```

### Linux (Ubuntu/Debian)

```bash
sudo apt-get update
sudo apt-get install -y openjdk-21-jdk
java -version
```

### Verify Gradle wrapper picks up Java 21

```cmd
cd %USERPROFILE%\Desktop\Vidyaprayag
gradlew.bat --version
```

(on macOS / Linux use `./gradlew --version`)

Look for `JVM: 21.x.x`. ✅

---

## 3. Get your Supabase connection string

1. Open <https://app.supabase.com> → your project.
2. Click the **gear icon (Project Settings)** in the left sidebar.
3. Open **Database** → scroll to **Connection string**.
4. Click the **URI** tab.
5. Choose **Session Pooler** (port `5432`) — Ktor + HikariCP needs long-lived
   sessions, so don't pick the *Transaction Pooler* (port 6543) unless you
   know why.
6. Reveal the password and **copy the entire string**. It looks like:

   ```
   postgresql://postgres.[REF]:[YOUR-PASSWORD]@aws-0-ap-south-1.pooler.supabase.com:5432/postgres
   ```

   Important: **replace `[YOUR-PASSWORD]` with the real password** if Supabase
   left a placeholder. The backend won't auto-substitute it.

> 🔒 **Never commit this string.** It goes only in your local `.env`
> (Phase 5), which is git-ignored.

---

## 4. Run the SQL migrations in Supabase (one-time)

The backend **does not** create or alter tables in Supabase — all schema
changes are reviewed SQL files you run yourself. This protects production
from silent ORM-driven mutations.

You need to run **two** SQL files, in order. Both are idempotent (`CREATE
TABLE IF NOT EXISTS`, `ON CONFLICT DO NOTHING`), so running them twice is
safe.

### 4.1 `supabase_schema` (operational tables — schools, students, …)

This is the v2.1 schema you already maintain. It's checked in at the repo
root:

```
%USERPROFILE%\Desktop\Vidyaprayag\supabase_schema
```

Open it in Notepad / VS Code, **select all → copy**.

In the Supabase Dashboard:

1. Left sidebar → **SQL Editor** → **New query**.
2. Paste the contents → **Run**.
3. Wait for *"Success. No rows returned."*

If you've already run this in the past, you'll get a bunch of "skip — already
exists" notices. That's fine.

### 4.2 `docs/backend/sql/01_supplementary_schema.sql` (this backend's tables)

This file adds everything the Ktor backend needs that isn't in the
operational schema: `app_users`, `auth_otps`, `user_sessions`,
`cms_landing_content`, `app_config`, `school_onboarding_drafts`,
`school_classes`, `school_subjects`, `announcements`, `whatsapp_logs`,
`admission_enquiries`, `school_philosophy`, `school_media`,
`storage_metrics`, `academic_calendar`, `holiday_list`, `faculty`,
`attendance_records`, plus the `purge_expired_otps()` function and RLS
policies.

```
%USERPROFILE%\Desktop\Vidyaprayag\docs\backend\sql\01_supplementary_schema.sql
```

Open it, copy everything, paste into a new SQL Editor query, **Run**.

You should see all the `CREATE TABLE` / `CREATE INDEX` / `CREATE POLICY`
statements succeed. If a `policy already exists` warning appears, ignore
it — that's the idempotency guard kicking in.

### 4.3 (Optional) Enable the OTP purge cron job

If your Supabase project has the **pg_cron** extension enabled
(it does by default on the paid plans), the bottom of the SQL file tries
to schedule a job that auto-deletes expired OTPs every 5 minutes. The
backend ALSO does this in-line on each `/send-otp` and `/verify-otp` call,
so the cron is a defence-in-depth measure — not required.

If you see a `pg_cron extension not available` error, just ignore it.

---

## 5. Create your local `.env` file

The repo ships `.env.example`. Copy it to `.env` (which is git-ignored):

```cmd
cd %USERPROFILE%\Desktop\Vidyaprayag
copy .env.example .env
```

(macOS / Linux: `cp .env.example .env`)

Open `.env` in a text editor. **At minimum** set:

| Variable               | Value                                                      |
|------------------------|------------------------------------------------------------|
| `DATABASE_URL`         | The string you copied from Phase 3                         |
| `JWT_SECRET`           | Any long random string (see below)                         |
| `OTP_PEPPER`           | Any long random string (see below)                         |
| `OTP_DEV_RETURN_CODE`  | `true` while you test from Postman, `false` in production  |

### Generate good random secrets

**macOS / Linux**:

```bash
openssl rand -hex 64    # for JWT_SECRET
openssl rand -hex 32    # for OTP_PEPPER
```

**Windows (PowerShell)**:

```powershell
[Convert]::ToHexString((New-Object byte[] 64 | %{ Get-Random -Max 256 }))
```

Paste each output into the matching variable. Sample finished `.env`:

```
DATABASE_URL=postgresql://postgres.abcd:supersecret@aws-0-ap-south-1.pooler.supabase.com:5432/postgres
PORT=8080
JWT_SECRET=a3f9...256chars...
OTP_PEPPER=7b14...64chars...
OTP_EXPIRY_MINUTES=10
OTP_MAX_ATTEMPTS=5
OTP_MAX_RESENDS_PER_HOUR=5
OTP_DEV_RETURN_CODE=true
OTP_PROVIDER=mock
APP_SEED_CMS=true
```

> If you leave `DATABASE_URL` blank the server falls back to a local
> SQLite file (`./data.db`). Useful for offline dev, but won't reflect
> production schema 100%. **For "real testing with Postman against
> Supabase", you MUST set `DATABASE_URL`.**

---

## 6. Boot the Ktor server locally

```cmd
cd %USERPROFILE%\Desktop\Vidyaprayag
gradlew.bat :server:run
```

(macOS / Linux: `./gradlew :server:run`)

**First run takes 2–5 minutes** while Gradle downloads dependencies. Be
patient. Subsequent runs are seconds.

When successful you'll see:

```
[main] INFO  ktor.application - Application started
[main] INFO  ktor.application - Responding at http://0.0.0.0:8080
```

### Sanity check

In a separate CMD window:

```cmd
curl http://localhost:8080/
```

Expected response:

```
Ktor: Hello, JVM — VidyaPrayag API v1 is live
```

✅ The backend is up.

> **Keep this window open**. The server logs every request and (in mock-OTP
> mode) prints `[MOCK-OTP] +91…  → CODE: 123456` to stdout every time
> you call `/send-otp`. That's how you'll grab OTPs during testing.

### Stopping

`Ctrl+C` in the server window stops the server. Run `gradlew :server:run`
again to restart.

---

## 7. Import the Postman collection + environment

Two files are checked in:

```
docs/backend/postman/VidyaPrayag.postman_collection.json
docs/backend/postman/VidyaPrayag.local.postman_environment.json
```

### In Postman

1. Click **Import** (top-left).
2. Drag in **both** files.
3. Confirm import.
4. Top-right environment dropdown → select **VidyaPrayag • Local**.

### Environment variables you'll be using

| Variable          | Meaning                                                   |
|-------------------|-----------------------------------------------------------|
| `base_url`        | `http://localhost:8080` (already set)                     |
| `phone_number`    | The real Indian mobile you're testing with (set this!)    |
| `auth_token`      | Auto-set by Login/Signup; used by every authenticated call|
| `refresh_token`   | Auto-set by Login/Signup                                  |
| `user_id`         | Auto-set                                                  |
| `school_id`       | Auto-set after onboarding submit                          |
| `dev_otp`         | Auto-set by `/send-otp` (only when OTP_DEV_RETURN_CODE=true)|

**Update `phone_number`** before testing:

```
+919876543210
```

Use a real number you control. The backend normalises 10-digit numbers to
`+91…` automatically, but for Postman clarity prefer the full form.

---

## 8. End-to-end test flow with a real phone number

The collection is ordered. Run requests **top to bottom**.

### Step 1 — Handshake (public)

| Request                                            | Expect                       |
|----------------------------------------------------|------------------------------|
| `GET /api/v1/content/landing`                      | `success: true`, CMS strings |
| `GET /api/v1/config/app-status?app_version=1.0.0`  | `success: true`, flags JSON  |

### Step 2 — Check whether you're a new user

| Request                                | Body                                            | Expect                                                                  |
|----------------------------------------|-------------------------------------------------|-------------------------------------------------------------------------|
| `POST /api/v1/auth/check-user`         | `{ "identifier": "{{phone_number}}" }`          | First time → `is_new_user: true`, `auth_method_required: OTP`           |

### Step 3 — Send the OTP

| Request                          | Body                                                                                       |
|----------------------------------|--------------------------------------------------------------------------------------------|
| `POST /api/v1/auth/send-otp`     | `{ "identifier": "{{phone_number}}", "purpose": "login", "device_id": "postman-001" }`     |

Expected response (development mode):

```json
{
  "success": true,
  "message": "OTP sent",
  "data": {
    "expires_at": "2026-05-22T17:23:11Z",
    "resend_count": 1,
    "dev_code": "528113",
    "message": "OTP sent. Valid for 10 minutes."
  }
}
```

The included Postman test script automatically saves `dev_code` into the
`dev_otp` environment variable, so the next request can reuse it.

> 🔍 **You can also grab the code from the server log window**:
>
> ```
> >>> [MOCK-OTP] +919876543210 (login) → CODE: 528113  (valid 10 min)
> ```

### Step 4 — Verify the OTP

| Request                              | Body                                                                       |
|--------------------------------------|----------------------------------------------------------------------------|
| `POST /api/v1/auth/verify-otp`       | `{ "identifier": "{{phone_number}}", "code": "{{dev_otp}}", "purpose": "login" }` |

Expected: `success: true`, `verified: true`.

### Step 5 — Sign up (first time only)

| Request                          | Body                                                                                                                                                  |
|----------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------|
| `POST /api/v1/auth/signup`       | `{ "name": "Abuzar", "identifier": "{{phone_number}}", "role": "school_admin", "device_info": { "device_id": "postman-001", "platform": "android" } }` |

Expected: `201 Created`, `token` returned, `auth_token` saved to env.

> Signup needs a **verified OTP** to have happened in Step 4 — otherwise
> you'll get `OTP_REQUIRED`.

### Step 6 — Login (subsequent times)

Repeat Step 3 (`send-otp`) + Step 4 (`verify-otp`), then:

| Request                       | Body                                                                                                                  |
|-------------------------------|-----------------------------------------------------------------------------------------------------------------------|
| `POST /api/v1/auth/login`     | `{ "identifier": "{{phone_number}}", "role": "school_admin", "otp": "{{dev_otp}}" }`                                  |

Expected: `success: true`, `token`, `refresh_token`. The Postman test
script stores both in environment variables.

### Step 7 — Pull your details

| Request                                | Expect                                                |
|----------------------------------------|-------------------------------------------------------|
| `GET /api/v1/user/details`             | personal_details + onboarding_details (all steps PENDING) |

### Step 8 — Walk through onboarding

| # | Request                                                                                  | Notes |
|---|------------------------------------------------------------------------------------------|-------|
| 1 | `GET  /api/v1/onboarding/step?obStepType=BASIC`                                          | empty drafts |
| 2 | `POST /api/v1/onboarding/submit` with `BASIC` payload                                    | persists drafts |
| 3 | `GET  /api/v1/onboarding/step?obStepType=BRANDING`                                       | |
| 4 | `POST /api/v1/onboarding/submit` with `BRANDING` payload                                 | |
| 5 | `GET  /api/v1/onboarding/step?obStepType=ACADEMIC`                                       | empty class list (no school yet) |
| 6 | `POST /api/v1/onboarding/submit` with `ACADEMIC` payload                                 | |
| 7 | `GET  /api/v1/onboarding/step?obStepType=REVIEW`                                         | |
| 8 | `POST /api/v1/onboarding/submit { "ob_step_type": "REVIEW", "is_final_submission": true }` | Creates `schools` row, sets `app_users.school_id` |

After step 8, re-run `GET /api/v1/user/details` — `onboarding_status`
should be `COMPLETED`.

### Step 9 — Hit the dashboard endpoints

| Request                                                            |
|--------------------------------------------------------------------|
| `GET /api/v1/school/announcements`                                 |
| `POST /api/v1/school/announcements` (create one)                   |
| `POST /api/v1/school/announcements/sync-whatsapp`                  |
| `GET /api/v1/admissions/enquiries/summary`                         |
| `POST /api/v1/admissions/enquiries` (create one)                   |
| `PATCH /api/v1/admissions/enquiries/{id}/status`                   |
| `GET /api/v1/school/calendar?view_type=month`                      |
| `GET /api/v1/school/holidays?filter_type=yearly`                   |
| `GET /api/v1/school/attendance/daily?type=student&grade=10`        |

For the read endpoints to return content, you'll need test data — see
Phase 9 next.

### Step 10 — Profile & gallery

| Request                                                          |
|------------------------------------------------------------------|
| `GET /api/v1/user/profile`                                       |
| `PUT /api/v1/user/profile/philosophy`                            |
| `PUT /api/v1/user/profile/tour-videos`                           |
| `PUT /api/v1/user/profile/gallery`                               |

---

## 9. Manually seed test data in Supabase (since the backend no longer auto-seeds)

Because we **deleted** the demo-data seeder (it was the cause of the
hardcoding pain), the dashboard endpoints will return empty arrays
until you insert some test data yourself.

Replace `{{SCHOOL_ID}}` everywhere below with the real `schools.id` UUID
your onboarding flow produced. To find it, in Supabase SQL Editor:

```sql
SELECT id, name FROM schools ORDER BY created_at DESC LIMIT 5;
```

### 9.1 A few students

```sql
INSERT INTO students (school_id, student_code, full_name, class_name, section, roll_number, is_active)
VALUES
  ('{{SCHOOL_ID}}','STU_001','Aarav Sharma','10','A','1', true),
  ('{{SCHOOL_ID}}','STU_002','Diya Patel','10','A','2', true),
  ('{{SCHOOL_ID}}','STU_003','Rohan Mehta','10','A','3', true);
```

### 9.2 A faculty member

```sql
INSERT INTO faculty (school_id, external_id, name, department, is_active)
VALUES
  ('{{SCHOOL_ID}}','FAC_001','Mrs. Kavita Rao','Mathematics', true);
```

### 9.3 Today's attendance for those students

```sql
INSERT INTO attendance_records (school_id, date, type, person_id, grade, status)
VALUES
  ('{{SCHOOL_ID}}', CURRENT_DATE::text, 'student','STU_001','10','present'),
  ('{{SCHOOL_ID}}', CURRENT_DATE::text, 'student','STU_002','10','present'),
  ('{{SCHOOL_ID}}', CURRENT_DATE::text, 'student','STU_003','10','absent');
```

### 9.4 An academic calendar event

```sql
INSERT INTO academic_calendar (school_id, event_id, date, day, event_title, event_description, standard, is_holiday)
VALUES
  ('{{SCHOOL_ID}}','EVT_PTM1', CURRENT_DATE::text,'Friday','Parent-Teacher Meeting','First PTM of the term','10', false);
```

### 9.5 A holiday

```sql
INSERT INTO holiday_list (school_id, date, title, type, frequency)
VALUES
  ('{{SCHOOL_ID}}','2026-08-15','Independence Day','Public','yearly');
```

### 9.6 A class with subjects

```sql
WITH c AS (
  INSERT INTO school_classes (school_id, code, name, sections)
  VALUES ('{{SCHOOL_ID}}','10','Class 10', '["A","B"]'::jsonb)
  RETURNING id
)
INSERT INTO school_subjects (class_id, sub_name, sub_code, teacher_assigned)
SELECT c.id, 'Mathematics', 'MATH10', 'Mrs. Kavita Rao' FROM c
UNION ALL
SELECT c.id, 'Science', 'SCI10', 'Mr. Anil Verma' FROM c;
```

Now re-run the dashboard endpoints from Phase 8.9 — you'll see real data
in the responses.

> 💡 **Postman can't run multi-statement raw SQL against Supabase.** That's
> why we use the Supabase SQL Editor for seeding.  If you want, you can
> *also* add admission enquiries via Postman:
>
> ```http
> POST {{base_url}}/api/v1/admissions/enquiries
> Authorization: Bearer {{auth_token}}
> { "student_name": "...", "parent_name": "...", "class": "10" }
> ```

---

## 10. Industrial-grade OTP — what's protecting you and how it works

Per the spec, the OTP system you asked for must:

1. Auto-delete after 10 minutes; frontend rejects expired OTPs.
2. Resending within 10 minutes **overwrites** the same DB row.
3. Include "other industrial-grade features".

Here's exactly what shipped in this PR, point by point.

### 10.1 One row per (identifier, purpose) — UPSERT on resend

Schema: `auth_otps` has `UNIQUE (identifier, purpose)`. The Kotlin service
(`OtpService.send(...)`) checks for an existing row and `UPDATE`s it in
place if found, otherwise `INSERT`s a fresh row. The previous code, the
previous `expires_at`, and the previous `attempt_count` are all replaced.
This is exactly your requirement: **resend within the window overwrites**.

### 10.2 10-minute TTL enforced at *three* layers

| Layer       | Mechanism                                                                             |
|-------------|---------------------------------------------------------------------------------------|
| Schema      | `expires_at TIMESTAMPTZ NOT NULL DEFAULT NOW() + INTERVAL '10 minutes'`               |
| In-process  | `OtpService.verify(...)` rejects if `NOW() > expires_at`                              |
| Background  | `OtpService.purgeExpired()` deletes expired rows; runs on every send/verify call      |
| Optional DB | `purge_expired_otps()` PL/pgSQL fn + `pg_cron` job every 5 min                        |

If a row's `expires_at` is in the past, the very next API call to /send-otp
or /verify-otp for **any identifier** sweeps it out. Belt + suspenders.

### 10.3 Hash + per-row salt + global pepper

The plaintext OTP **never** lives in the database. We store
`code_hash = SHA-256(code:salt:pepper)`. The salt is a fresh random 16-char
string per row. The pepper is a global `OTP_PEPPER` env var. This means:

- A leaked DB dump alone can't be used to derive any OTP — the attacker
  also needs your `OTP_PEPPER` (which is only in your env, never in git).
- Two users typing the same OTP code at the same time have completely
  different hashes (different salts).

### 10.4 Constant-time compare on verify

Plain `==` on strings short-circuits on the first mismatching character,
which leaks timing info. We compare hashes with a bitwise XOR loop
(`constantTimeEquals` in `OtpService.kt`) so verification always takes the
same wall-clock time regardless of how many leading digits matched.

### 10.5 Brute-force lock (5 wrong attempts)

`auth_otps.attempt_count` is incremented on every wrong code. When it
hits `max_attempts` (default 5) the row's `is_locked = true` and any
further `verify()` returns `OTP_LOCKED` with HTTP 423.  Locked rows can
only be cleared by `/send-otp` (which generates a fresh code and resets
`attempt_count = 0`).

This blocks both:
- **Online brute-force** (an attacker hammering /verify-otp).
- **Replay attacks** after a wrong-guess streak; the locked row is
  invalidated regardless of how close they got.

### 10.6 Resend rate-limit (5 sends per identifier per hour)

`auth_otps.resend_count` and `first_sent_at` form a sliding 1-hour
window. If a caller exceeds `OTP_MAX_RESENDS_PER_HOUR` resends inside that
window, `/send-otp` returns HTTP 429 `OTP_RATE_LIMITED`. This blocks SMS
bombing and toll-fraud abuse (someone repeatedly triggering SMS to a
target number to rack up your bill).

### 10.7 Audit trail

Every row captures `ip_address`, `user_agent`, `device_id`,
`delivery_channel` (`sms`/`email`), `delivery_provider` (`mock`/`msg91`/…),
`provider_message_id`. When abuse is suspected you have a forensic trail.

### 10.8 Sealed result types

`OtpVerifyResult` is a sealed class with `Ok | NotFound | Expired | Locked
| Invalid(attemptsLeft: Int)`. The Ktor handler maps each to a distinct
HTTP status and `errorCode`, so the frontend can show precise error UX
(`"Attempts left: 2"` instead of a generic `"Invalid OTP"`).

### 10.9 Pluggable delivery

`OtpDeliveryProvider` is a single suspend function. In dev it logs to
stdout (mock). When you're ready for production, wire MSG91 / Twilio /
Gupshup behind the same interface, set `OTP_PROVIDER=msg91`, and **no
other code changes**. The Ktor server doesn't care.

### 10.10 Dev convenience without prod risk

`OTP_DEV_RETURN_CODE=true` makes `/send-otp` echo back the code in the
JSON response (`dev_code` field). The Postman test script extracts it and
chains it into `/verify-otp` so you can test the full flow without
spinning up an SMS provider. **In production, set this to `false`** —
when it's false the field is omitted entirely from the response.

### 10.11 Row-level security (RLS)

The supplementary SQL turns on RLS for `auth_otps`, `app_users`,
`user_sessions`, and everything else the backend uses. Anonymous clients
have **zero direct DB access** to OTPs even via the Supabase JS SDK —
all reads/writes go through the Ktor backend's service-role key.

---

## Troubleshooting

| Symptom                                                           | Likely cause / fix                                                                                       |
|-------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------|
| `Could not target platform: 'Java SE 21'`                         | JDK 21 not installed or `JAVA_HOME` points to old JDK. Redo Phase 2.                                     |
| `Connection to db.*.supabase.co:5432 refused`                     | Wrong port (use 5432 Session Pooler), or your IP is blocked. Check Supabase Dashboard → Network Restrictions. |
| `password authentication failed`                                  | You forgot to substitute `[YOUR-PASSWORD]` in `DATABASE_URL`.                                            |
| `/api/v1/user/details` returns `User not found`                   | You didn't sign up yet OR your JWT is for a different user. Repeat Phase 8.                              |
| `OTP_RATE_LIMITED` while testing                                  | You sent 5 OTPs in an hour for the same number. Wait, or use a different test number, or `TRUNCATE auth_otps;` in SQL Editor. |
| Server log shows no `[MOCK-OTP]` line                             | `OTP_PROVIDER` is not `mock` or the request didn't reach `/send-otp`. Check route + body.                |
| Postman shows `Could not get any response`                        | Server isn't running. Check the Gradle window.                                                           |
| Onboarding submit returns `Invalid token`                         | Your `auth_token` env var is stale. Re-run signup/login.                                                 |

---

## Daily workflow cheat-sheet

```cmd
:: Start of day
cd %USERPROFILE%\Desktop\Vidyaprayag
git pull origin backend-by-abuzar
gradlew.bat :server:run

:: New CMD window — when running migrations
:: (Just copy SQL into Supabase SQL Editor)

:: Test in Postman — VidyaPrayag • Local environment selected
```

That's it. Welcome back to a sane, non-hardcoded backend. 🎯
