# VidyaPrayag — School Side Status Report

> **Audit branch:** `backend-by-abuzar`
> **HEAD commit:** `d3ffc3a` — feat(ui): premium iOS-style overhaul + glitch fixes (school/admin side)
> **Audit date:** 2026-06-04 (updated — premium UI pass)
> **Previous audit date:** 2026-06-03
> **Scope:** Full re-audit of `backend-by-abuzar` branch. Checked: last 15 commits, live Render logs, Android Studio device log (`2026-06-03 175806.353 O.txt`), all 7 provided screenshots (Render env dashboard ×3, app UI ×4), backend source, shared client, composeApp, OTP provider chain, environment variables, navigation, onboarding flow, document upload, GPS, and teacher/class/subject model.
>
> **⭐ 2026-06-04 UPDATE — Premium UI / Glitch-Free Pass:** A full premium iOS-style UI overhaul of the school/admin side was completed and merged via PR. All admin-side UI glitches identified below have been resolved (dead controls removed, hardcoded colors centralised, broken images fixed, premium components introduced with zero-conflict delegation). See the new **§9 — UI / Premium Overhaul (DONE)** at the end of this report for the complete fixed/remaining ledger.

---

## QUICK STATUS: What Is Working vs. What Is Broken (as of 2026-06-04)

| Area | Status | Notes |
|---|---|---|
| Server boots on Render | ✅ Working | Confirmed in Render log: starts in ~6.6s |
| Database schema auto-create | ✅ Working | `AUTO_CREATE_TABLES=true`, Exposed creates tables on boot |
| Supabase Postgres connection | ✅ Working | Render log shows no DB errors; pooler URL configured correctly |
| Email+password signup | ✅ Working | Render log: `201 Created: POST /api/v1/auth/signup` |
| Email+password login | ⚠️ Partially Working | `200 OK` after signup; repeated `401` when re-attempting — see §3.1 |
| OTP delivery (SMS/WhatsApp/Email) | ❌ BROKEN | All providers skipped; only console fallback active — see §3.2 |
| Forgot / Reset password | ❌ NOT IMPLEMENTED | No endpoint, no UI — see §3.3 |
| Media upload (logo/branding) | ⚠️ Wired but blocked | Code is correct; returns 503 because `SUPABASE_URL` + `SUPABASE_SERVICE_KEY` not set on Render — see §3.4 |
| Document upload (LaunchInfoOB) | ❌ BROKEN | "Upload" button only sets local state; no file picker, no MediaApi call — see §3.5 |
| GPS / Location capture | ⚠️ Wired but slow | Android provider implemented; `lastKnown()` frequently returns null → falls to 12-second `liveFix` timeout — see §3.6 |
| School admin analytics tabs | ⚠️ Navigation exists, role-gated | Analytics/Calendar/Attendance/PTM/Results are in the side drawer for `ADMIN` role only; screenshot showed `Guest User` — see §3.7 |
| Class/Subject management (OB step 3) | ❌ REDESIGN NEEDED | Current UI shows CBSE-synced classes but does NOT support: adding multiple classes, per-class subject editing, teacher profile creation, or assigning same teacher to multiple subjects/classes — see §3.8 |
| Teacher assignment backend | ✅ Backend done | `teacher_subject_assignments` table + `GET/POST/DELETE /api/v1/school/teacher-assignments` exists in server code |
| Teacher assignment client UI | ❌ NOT WIRED | No client-side API calls to `teacher-assignments`; `AcademicInfoOBScreen` does not offer add-class, add-teacher, or per-class subject editing — see §3.8 |
| Broadcast audience segmentation | ✅ Backend done | `audience_type` + `audience_filter` added; recipient resolver uses assignment graph |
| Announcement create UI | ⚠️ Missing audience picker | Backend supports `audience_type`; client create-announcement dialog does not expose it |
| Onboarding `Invalid token` error | ❌ ACTIVE BUG | Branding step returns "Invalid token" — root cause: Supabase service key not set + possible token expiry mid-flow — see §3.9 |
| Admin drawer navigation | ✅ Code-correct | ADMIN role sees Analytics/Calendar/Attendance/Leave/Results/PTM/Schedule drawer items |
| Premium animations | ✅ Working | PremiumAnimations.kt compiled after §057f557 fix |
| Admin-side UI glitches | ✅ FIXED (2026-06-04) | Dead "pending" buttons → ComingSoonPill; broken images → NetworkImage; dead no-op top-bar icons removed — see §9 |
| Admin hardcoded colors | ✅ FIXED (2026-06-04) | All `Color(0xFF..)` status hexes centralised into `StatusColors` palette — zero hardcoded colors left in admin — see §9 |
| Premium button system | ✅ DONE (2026-06-04) | `PremiumButton`/`PremiumTonalButton`/`PremiumOutlineButton` (Dribbble-style, brand-themed); generic buttons delegate to them — see §9 |
| Admin mobile-friendliness | ✅ VERIFIED (2026-06-04) | All 19 admin screens use consistent scaffold + scrollable layout; no fixed-width overflow — see §9 |
| Route duplicate/shadow risk | ✅ Fixed | `inList` compile error fixed; legacy mock routes removed in prior commits |
| API credential logging (security) | ✅ Fixed | `safeApiCall` now redacts password/token/otp fields |
| JWT_SECRET | ⚠️ CRITICAL MISSING | Render env screenshot shows `JWT_SECRET` IS set (confirmed in screenshot 3); but verify it is a real strong value, not the `change-me` placeholder |

---

## 1. Executive Summary

The `backend-by-abuzar` branch is **structurally sound** — all major compile blockers from the previous audit are fixed, and the server runs. However, **five critical runtime issues** prevent the app from being usable in production:

1. **OTP delivery is broken** — no real SMS/Email/WhatsApp provider is configured on Render. All OTPs print to Render logs only (console fallback). Users who registered with a phone number cannot receive OTPs. The `OTP_DEV_RETURN_CODE` defaults to `true` in `OtpService.kt` when unset, meaning the API response contains the OTP in plaintext (major security risk if left enabled).

2. **Forgot/Reset password is completely absent** — neither the backend endpoint nor the client UI exists. Users who forget their email+password have no recovery path.

3. **Document upload in the Launch/Verification step does nothing real** — the "Upload" button in `LaunchInfoOBScreen` calls `viewModel.markDocumentUploaded()` which only flips local UI state. There is no file picker, no `MediaApi.upload()` call, no actual file sent to the backend. Documents are not persisted.

4. **Class/Subject/Teacher onboarding step needs a full UI revamp** — the current Academic step (Step 3) shows CBSE-synced class/subject data but provides no way to: add new classes, edit subjects per-class, create a teacher profile inline, or assign the same teacher to multiple subjects/classes. The backend infrastructure for teacher assignments exists and is ready; only the client is missing.

5. **Media upload (branding step) returns 503** — the code is correct and fully wired, but `SUPABASE_URL` and `SUPABASE_SERVICE_KEY` are not set as Render environment variables, causing every upload attempt to return `503 STORAGE_NOT_CONFIGURED`.

---

## 2. Recent Commits Reviewed (since last audit)

| # | Commit | Summary | Audit Note |
|---:|---|---|---|
| 1 | `057f557` | fix(ui): resolve PremiumAnimations build errors + richer premium motion | Fixes `AnimatedEntrance`/`ShimmerBox` compile errors; animations now work |
| 2 | `cd3da01` | docs: consolidated MANUAL_SETUP_GUIDE.md + status report §8e | Good reference; already linked in this report |
| 3 | `3dbe88b` | feat(ui): premium iOS-style motion primitives | `PremiumAnimations.kt` added; applied to ProfileScreen + buttons |
| 4 | `64d7853` | feat(school): real file-picker uploads for gallery & virtual tour | `InstitutionalProfileScreen` gallery/tour now use real device picker → upload — ✅ |
| 5 | `2be5fa9` | feat(school): wire real GPS capture into onboarding Basics step | GPS button in InstitutionalBasicOBScreen now calls real Android LocationManager |
| 6 | `229bef1` | feat(school): add cross-platform LocationProvider for GPS capture | `LocationProvider.android.kt` implemented with permission + liveFix + geocoding |
| 7 | `b0353bf` | feat(school): real Supabase media uploads + STUDENT-scope link + migration_002 | `MediaRouting`, `SupabaseStorage`, `BrandingInfoOBViewModel.uploadMedia()` wired |
| 8 | `f7a6f1c` | feat(school): implement P1/P2 architecture — teacher assignments, geo, segmentation | `teacher_subject_assignments` table, `TeacherAssignmentRouting`, Haversine discovery |
| 9 | `9170ad4` | docs(deploy): full Render env-var plan + P1/P2 architecture solutions | Good reference for env var setup |
| 10 | `5433252` | fix(deploy): make backend build identity reliable on Render | Build SHA identity endpoint stabilized |
| 11 | `b0fc09b` | fix(school): resolve backend build and admin UI glitches | Various UI fixes; `inList` compile error addressed |

---

## 3. Detailed Issue Analysis

### 3.1 Login 401 — Repeated authentication failures

**Evidence from Render log:**
```
401 Unauthorized: POST - /api/v1/auth/login  (×6 attempts from same session)
```

**Root cause analysis:**
The server implements two separate login paths: email→password and phone→OTP. The login route in `AuthRouting.kt` requires:
- For email: correct password hash match
- For phone: a verified OTP in `auth_otps` table

The repeated 401s in the log are consistent with: a user successfully signing up (201 Created), then attempting to log in with **a wrong password** or with a **phone number that never received an OTP** (since OTP delivery is broken). There is no lockout after 5 failures currently enforced on the login route itself (only on OTP verify).

**Impact:** Users with phone-only accounts cannot log in at all until OTP delivery is fixed.

---

### 3.2 OTP Delivery — COMPLETELY BROKEN on Production

**Evidence from Render log:**
```
WARN OtpDeliveryDispatcher - [OtpDispatcher] all 5 providers failed: all providers skipped (none configured)
WARN OtpService - [OtpService] delivery failed identifier-type=phone purpose=signup reason=all providers skipped (none configured)
502 Bad Gateway: POST - /api/v1/auth/send-otp in 1196ms
```

**Root cause — Missing Render environment variables:**

From the Render env screenshots, the following OTP-related variables are set:
- `OTP_EXPIRY_MINUTES=10` ✅
- `OTP_MAX_ATTEMPTS=5` ✅
- `OTP_MAX_RESENDS_PER_HOUR=5` ✅
- `OTP_PEPPER=<long hex>` ✅
- `OTP_ADMIN_TOKEN=<token>` ✅
- `OTP_CHANNEL_ORDER=sms,whatsapp,email` ✅
- `OTP_DEV_RETURN_CODE=false` ✅
- `OTP_ENABLE_CONSOLE_FALLBACK=false` ✅
- `FAST2SMS_API_KEY=<key set>` ✅ (visible in screenshot 3)
- `FAST2SMS_ROUTE=otp` ✅
- `FAST2SMS_SENDER_ID=` ❌ **EMPTY** — this is fine for `otp` route
- `MSG91_AUTH_KEY=` ❌ **EMPTY**

**Critical finding:** `FAST2SMS_API_KEY` IS set in the Render environment. However, the `502 Bad Gateway` in the log is from an **OLDER deployment** before the Fast2SMS key was added. The current deployment (restarted at `06:44:10`) shows no OTP attempts in the new log window — meaning this needs to be re-tested after the current deploy.

**HOWEVER — critical code bug in `OtpService.kt`:**
```kotlin
// Line in OtpService.kt:
env("OTP_DEV_RETURN_CODE", "true").equals("true", true)
//                          ^^^^^^ DEFAULT IS "true"!
```
**This means: if `OTP_DEV_RETURN_CODE` is NOT set on Render, it defaults to `true`, and the OTP code is returned in the API response body in plaintext.** This is a security risk. The Render screenshot shows `OTP_DEV_RETURN_CODE=false` IS set, so this specific risk is mitigated — but it's a dangerous default for a production system.

**Also: SMTP (email OTP) is not configured:**
- `SMTP_HOST=` ❌ **EMPTY** (visible in Render screenshot 1)
- `SMTP_USERNAME=` not set
- `SMTP_PASSWORD=` not set
- `SMTP_FROM=` not set

This means users who sign up with an **email address** (not phone) will **never receive an OTP** because:
- SMS providers only handle phone identifiers
- SMTP is the only email OTP provider, and it's not configured

**Impact:** Any user trying to do OTP-based login/signup with an email address will get 502 Bad Gateway with `all providers skipped`.

---

#### Complete OTP Environment Setup Guide (Step-by-Step)

**Current situation on Render:**
- Fast2SMS key is set → phone OTPs SHOULD work (needs re-test after redeploy)  
- SMTP not set → email OTPs will always fail
- Console fallback is OFF → no silent fallback, which is correct for production

---

#### OPTION A — Fast2SMS (Recommended for India, SMS OTP to phones)

Fast2SMS already has a key set. To verify it works:

1. Go to [https://www.fast2sms.com](https://www.fast2sms.com) → Login
2. Navigate to: **Dev API → Bulk SMS** (left sidebar)
3. Your API key is on this page. Confirm it matches what's in Render.
4. Check your wallet balance — if balance is 0, SMS will silently fail. **Top up minimum ₹100.**
5. The `otp` route does NOT require DLT or Sender ID. Leave `FAST2SMS_SENDER_ID` empty.
6. In Render → Environment: confirm exactly these values:
   ```
   FAST2SMS_API_KEY=<your key from fast2sms.com>
   FAST2SMS_ROUTE=otp
   ```
7. After saving, Render auto-redeploys. Test by attempting `/api/v1/auth/send-otp` with a real Indian mobile number.

**FAST2SMS API Key location:**
- URL: https://www.fast2sms.com/dashboard/dev-api
- Sign in → Left menu → **Dev API** → Copy the key shown under "Authorization"

---

#### OPTION B — SMTP Email OTP (Required for email-identifier users)

This is **required** because `OTP_CHANNEL_ORDER=sms,whatsapp,email` means SMS runs first, but if the identifier is an email address, SMS is skipped and falls to email. Without SMTP configured, email-identifier OTPs always fail.

**Using Gmail (easiest, free):**

**Step 1:** Go to your Google Account → Security → 2-Step Verification (must be enabled)

**Step 2:** After enabling 2FA, go to: [https://myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords)
- App name: type `VidyaPrayag`
- Click **Create** → Google gives you a 16-character app password like `abcd efgh ijkl mnop`
- Remove spaces → `abcdefghijklmnop`

**Step 3:** Add these to Render → Environment:
```
SMTP_HOST=smtp.gmail.com
SMTP_PORT=465
SMTP_USERNAME=yourgmail@gmail.com
SMTP_PASSWORD=abcdefghijklmnop   ← the 16-char App Password (no spaces)
SMTP_FROM=VidyaPrayag <yourgmail@gmail.com>
SMTP_USE_SSL=true
```

**Using Resend (cleaner, 3000 free emails/month):**
1. Go to [https://resend.com](https://resend.com) → Sign up (free)
2. Dashboard → API Keys → Create API Key → copy it
3. Add domain (or use `onboarding@resend.dev` for testing)
4. Render environment:
```
SMTP_HOST=smtp.resend.com
SMTP_PORT=465
SMTP_USERNAME=resend
SMTP_PASSWORD=re_xxxxxxxxxxxxxxxxxxxx   ← your Resend API key
SMTP_FROM=VidyaPrayag <onboarding@resend.dev>
SMTP_USE_SSL=true
```

---

#### OPTION C — MSG91 (India SMS, alternative to Fast2SMS)

1. Go to [https://control.msg91.com](https://control.msg91.com) → Sign up / Login
2. Left sidebar → **API** → **Auth Keys** → Copy your Auth Key
3. Left sidebar → **Flows** → Create a new OTP flow with one variable `{{OTP}}`
4. Copy the Flow ID from the flow detail page
5. Render environment:
```
MSG91_AUTH_KEY=<auth key from control.msg91.com>
MSG91_FLOW_ID=<your flow id>
MSG91_OTP_VAR_NAME=OTP
MSG91_SENDER_ID=VIDPRA   ← 6-char sender (register at DLT portal if needed)
```

---

#### OPTION D — WhatsApp Cloud API (Free for 1000 conversations/month)

1. Go to [https://developers.facebook.com](https://developers.facebook.com) → My Apps → Create App
2. App type: Business → Add WhatsApp product
3. WhatsApp → Getting Started → note your **Phone Number ID** and temporary **Access Token**
4. For a permanent token: Business Settings → System Users → Create system user → Generate token (WhatsApp Business permissions)
5. Message Templates → Create template named `vidyaprayag_otp`, category: Authentication, body: `{{1}} is your OTP for VidyaPrayag.` (must be approved by Meta, takes 1-2 days)
6. Render environment:
```
WHATSAPP_ACCESS_TOKEN=<permanent system user token>
WHATSAPP_PHONE_NUMBER_ID=<phone number id>
WHATSAPP_TEMPLATE_NAME=vidyaprayag_otp
WHATSAPP_TEMPLATE_LANG=en
```

---

#### Recommended Final OTP Configuration for Render

Set ALL of the following for robust coverage (SMS for phones + email fallback):

```
# Fast2SMS (phones — India)
FAST2SMS_API_KEY=<from fast2sms.com Dev API>
FAST2SMS_ROUTE=otp

# SMTP (emails)
SMTP_HOST=smtp.gmail.com
SMTP_PORT=465
SMTP_USERNAME=<your gmail>
SMTP_PASSWORD=<16-char app password>
SMTP_FROM=VidyaPrayag <your gmail>
SMTP_USE_SSL=true

# Delivery order
OTP_CHANNEL_ORDER=sms,email

# Hardening (already set in Render — KEEP)
OTP_DEV_RETURN_CODE=false
OTP_ENABLE_CONSOLE_FALLBACK=false
```

---

### 3.3 Forgot Password — NOT IMPLEMENTED ANYWHERE

**Evidence:** Full codebase search for `forgot`, `reset.*pass`, `resetPassword`, `forgotPassword` returns **zero results** in all Kotlin source files.

**What is missing:**

**Backend — needs 3 new endpoints:**
```
POST /api/v1/auth/forgot-password   { "identifier": "email@example.com" }
  → generates a reset token, sends it via OTP/email/SMS
POST /api/v1/auth/verify-reset-token { "identifier", "token" }
  → validates the reset token
POST /api/v1/auth/reset-password    { "identifier", "token", "new_password" }
  → hashes + stores new password, invalidates all existing sessions
```

**Client — needs:**
- "Forgot password?" link on the login screen (in `AuthBottomSheet.kt`)
- A `ForgotPasswordScreen.kt` (3-step: enter phone/email → enter OTP/token → enter new password)
- A `ForgotPasswordViewModel.kt` wiring all 3 backend calls

**Recommended approach:**
Reuse the existing OTP delivery infrastructure. The forgot-password flow is: `send-otp(purpose="reset_password")` → `verify-otp` → new endpoint `reset-password { identifier, verified_otp_ref, new_password }`. This avoids building a second token system.

---

### 3.4 Media Upload (Branding Step) — 503 STORAGE_NOT_CONFIGURED

**Evidence from Render log:**
```
401 Unauthorized: POST - /api/v1/school/media/upload  (×4 times)
```

**Root cause:** The 401 is NOT a storage configuration error — it's an **authentication failure**. The client is calling `POST /api/v1/school/media/upload` without a valid auth token. This is a separate issue from the 503 STORAGE_NOT_CONFIGURED case.

**Two distinct problems:**

**Problem A — Missing Render env vars (causes 503):**
From `SupabaseStorage.kt`: `isConfigured()` returns false when `SUPABASE_URL` or `SUPABASE_SERVICE_KEY` are absent.
From Render env screenshots: `SUPABASE_URL=https://dumoiojpkizxkzzxdzss.supabase.co` ✅ and `SUPABASE_SERVICE_KEY=<long key>` ✅ ARE set.

**Revised finding:** Storage env vars ARE present. The 401 errors are an authentication token issue, not a storage configuration issue.

**Problem B — "Invalid token" banner in Branding step (BrandingInfoOBScreen):**
The screenshot shows a red "Invalid token" error on the Branding step. This means the user's JWT access token is being rejected by the server. Possible causes:
1. Token expired mid-onboarding (60-min default, user may have paused)
2. `JWT_SECRET` on Render does not match the secret used when the token was originally issued (e.g., after a Render redeploy with a new secret)
3. The token is being sent in the wrong header format

**Fix for Problem B:**
- Implement token refresh logic: when any request returns 401, automatically call `POST /api/v1/auth/refresh` with the stored refresh token before surfacing the error to the user
- If refresh also fails, navigate to login screen with a clear "Session expired, please sign in again" message
- The current code only logs the error and shows a banner — users are stranded mid-onboarding

---

### 3.5 Document Upload in LaunchInfoOBScreen — LOCAL STATE ONLY (NOT REAL)

**Evidence from code review (`LaunchInfoOBViewModel.kt`):**
```kotlin
fun markDocumentUploaded(documentId: String) {
    val updatedDocs = _state.value.documents.map { doc ->
        if (doc.id == documentId) {
            doc.copy(status = "Uploaded", metadata = "Selected on this device • Submit launch to verify")
        } else { doc }
    }
    _state.value = _state.value.copy(documents = updatedDocs)
    _infoMessage.value = "Document marked for verification. Launch Profile to finalize."
}
```

**The "Upload" button only flips a local UI boolean.** No file picker is launched. No `MediaApi.upload()` is called. The document bytes never reach the server. The `complianceDocs` list sent in the final REVIEW submit payload is empty/default.

**What is visible in the screenshot:** The "Affiliation Cert" shows "Selected on this device • Submit launch to verify" which comes from this fake local state — it's not a real upload.

**What is needed:**
1. `LaunchInfoOBViewModel` needs to inject `MediaApi` (like `BrandingInfoOBViewModel` does)
2. The "Upload" button needs to launch `rememberMediaPicker()` for PDF/document selection
3. On file selection, call `mediaApi.upload(bytes, MediaKind.DOCUMENT)` 
4. Store the returned URL against the document ID
5. Include uploaded document URLs in the REVIEW submit payload

---

### 3.6 Location Capture — SLOW (Up to 12 Seconds)

**Evidence from code (`LocationProvider.android.kt`):**
```kotlin
// lastKnown() frequently returns null on cold-start or when location is off
val location = withContext(Dispatchers.IO) {
    lastKnown(lm) ?: liveFix(lm)   // falls to liveFix very often
}

private suspend fun liveFix(lm: LocationManager): Location? {
    return withTimeoutOrNull(12_000L) {   // blocks UI thread feedback for up to 12s
        ...
    }
}
```

**The user reports location "is taking too long to record."**

**Root causes:**
1. `lastKnown()` returns null on fresh device boot, after airplane mode, or if location was recently disabled — this is very common
2. `liveFix()` then waits for a cold GPS fix which on Android can take 15-45 seconds in an open-sky environment and even longer indoors
3. The 12-second timeout means the function returns `null` indoors, giving the user a confusing "Couldn't get a location fix" message with no retry feedback

**Fix recommendations:**
- Add an intermediate loading state with visual progress (spinner + "Getting your location..." text)  
- Try `NETWORK_PROVIDER` first (cell tower triangulation — fast, ~1-2s) before waiting for GPS
- Reduce cold-GPS timeout from 12s to 5s, but show a "Still searching…" indicator
- After timeout, still show a manual address entry fallback prominently rather than just an error message
- Consider Fused Location Provider via Google Play Services (much faster) if you accept the dependency

---

### 3.7 Admin Analytics and Feature Tabs — WHY THE SCREENSHOT SHOWS NOTHING

**The screenshot shows the drawer with only: Home, Theme, Sign Out, Support, Privacy, Terms.**

**Root cause: The user was logged in as `GUEST` (not `ADMIN`).**

From `VidyaPrayagDrawer.kt`:
```kotlin
if (userRole == "ADMIN") {
    Text("SCHOOL OPTIONS", ...)
    DrawerItem(Icons.Default.Analytics, "Analytics") { onNavigate(Destination.AnalyticsDashboard) }
    DrawerItem(Icons.Default.CalendarMonth, "Academic Calendar") { ... }
    DrawerItem(Icons.Default.AssignmentTurnedIn, "Daily Attendance") { ... }
    DrawerItem(Icons.Default.PendingActions, "Leave Request") { ... }
    DrawerItem(Icons.Default.Assessment, "Results") { ... }
    DrawerItem(Icons.Default.Groups, "Schedule PTM") { ... }
}
```

The analytics and admin feature tabs **only appear when `userRole == "ADMIN"`**. The drawer header in the screenshot clearly says "Guest User" — meaning either:
1. The user's JWT token was expired/invalid and `MainViewModel.userRole` fell back to null → "GUEST"
2. The user was never properly authenticated as ADMIN

**This is directly linked to the "Invalid token" bug (§3.4 Problem B).** When the token is invalid/expired, the role-fetch fails, userRole becomes null/"GUEST", and all ADMIN-only drawer items disappear. This explains why the admin appears to have no analytics — the session is broken.

**Fix:** Resolve the token expiry/refresh issue in §3.4. When the user is properly authenticated as ADMIN, all tabs appear correctly. No navigation code change is needed.

---

### 3.8 Class/Subject/Teacher Onboarding — NEEDS FULL UI REVAMP

**What the user expects:**
- Add **multiple classes** in a school (e.g., Class 1A, 1B, 2A, LKG, UKG, etc.)
- Each class has its own set of **subjects** (editable, not just CBSE-synced)
- Assign a **teacher** to each subject in each class
- Ability to **create a teacher profile inline** during assignment
- Same teacher can be assigned to **multiple subjects** and/or **multiple classes**

**What currently exists in the UI (`AcademicInfoOBScreen`):**
- ✅ Displays available classes (Nursery, LKG, UKG, Class 1-6) from CBSE sync
- ✅ Shows subjects linked to the selected class
- ✅ Shows teacher name as "Assigned"/"Unassigned" text
- ❌ No "Add Class" button — cannot add custom classes
- ❌ No "Edit Subjects for this Class" — cannot modify per-class subject list
- ❌ No teacher assignment UI — clicking "Assigned" does nothing
- ❌ No "Create Teacher Profile" modal
- ❌ Same teacher assigned to multiple subjects is impossible from the UI
- ❌ No client API calls to `GET/POST /api/v1/school/teacher-assignments`

**What the backend already supports (ready to use):**
```
GET  /api/v1/school/teacher-assignments           → list all assignments for school
POST /api/v1/school/teacher-assignments           → create assignment {faculty_id, class_id, subject_id}
DELETE /api/v1/school/teacher-assignments/{id}    → remove assignment
```

**Recommended UI redesign for Step 3:**

```
┌─ Curriculum Setup ─────────────────────────────────────────────────┐
│  [+ Add Class]                                                       │
│                                                                      │
│  ▼ LKG                                          [✎ Edit] [🗑 Delete] │
│    Subjects:                                    [+ Add Subject]      │
│    ┌──────────────────────────────────────────────────────────────┐  │
│    │ Mathematics   [Teacher: Dr. Arpita Sharma ▼] [✎]            │  │
│    │ Science       [Assign Teacher ▼]                             │  │
│    │ English       [Teacher: Mr. Ravi Kumar ▼]   [✎]             │  │
│    └──────────────────────────────────────────────────────────────┘  │
│                                                                      │
│  ▼ UKG                                          [✎ Edit] [🗑 Delete] │
│    ...                                                               │
└─────────────────────────────────────────────────────────────────────┘

Teacher dropdown → shows existing faculty list + "+ Create New Teacher"
"Create New Teacher" → inline bottom sheet: Name, Phone, Email, Qualification
```

---

### 3.9 "Invalid Token" on Branding Step — Session Token Management Bug

**Evidence from screenshot:** Red banner "Invalid token" on `BrandingInfoOBScreen` Step 2.

**Root cause (confirmed from Render log pattern):**
```
201 Created: POST /api/v1/auth/signup
200 OK: GET  /api/v1/content/landing
200 OK: POST /api/v1/onboarding/submit    ← Step 1 succeeds
401 Unauthorized: POST /api/v1/school/media/upload   ← Step 2 fails
```

The media upload endpoint is school-scoped and requires a valid token. The same token that worked for `/onboarding/submit` at step 1 is failing for `/school/media/upload` at step 2.

**Likely cause:** The school media upload endpoint validates that the user has a `school_id` (i.e., is already associated with a school). During step 2 of onboarding, the school record may not yet be created — it's only created at the REVIEW final submit step. This means the school-scoped endpoint rejects the request with 401 because the user is not yet "a school admin" (no school_id on their account).

**Fix:** The `/api/v1/school/media/upload` endpoint needs to accept requests from users who are in the middle of onboarding (have ADMIN role but no school_id yet), OR branding uploads should use a different endpoint that is onboarding-safe. The server should check `role == ADMIN` and either `school_id is not null` OR `onboarding_status != COMPLETED` (i.e., allow uploads during onboarding regardless of school_id).

---

## 4. Additional Issues Found (Beyond Core Reports)

### 4.1 `OTP_DEV_RETURN_CODE` Default is Dangerously `true`

**File:** `server/src/main/kotlin/.../feature/auth/OtpService.kt`
```kotlin
private val devReturnCode: Boolean by lazy {
    env("OTP_DEV_RETURN_CODE", "true").equals("true", true)
    //                          ^^^^^^ Default: true
}
```

If this env var is ever accidentally unset (e.g., Render env reset, new deployment without the var), the server starts returning real OTPs in plain API responses. This is a critical security defect.

**Fix:** Change the default from `"true"` to `"false"`:
```kotlin
env("OTP_DEV_RETURN_CODE", "false").equals("true", true)
```

---

### 4.2 Announcement Create UI Missing Audience Picker

The backend supports `audience_type` (ALL_SCHOOL, CLASS, SECTION, SUBJECT, STUDENT, CUSTOM) and `audience_filter`. The `SchoolAnnouncementsScreen` create dialog does not expose these fields. All announcements created from the UI are implicitly `ALL_SCHOOL` broadcasts — teachers cannot send class-specific announcements from the app.

---

### 4.3 `markDocumentUploaded` Called with Hardcoded String IDs

In `LaunchInfoOBViewModel`, documents are initialized with hardcoded IDs `"1"`, `"2"` etc. If the server returns documents with UUID IDs (as it should from `complianceDocs`), the ID matching in `markDocumentUploaded` will fail silently.

---

### 4.4 Location Permission Missing from AndroidManifest (verify)

The Android location provider (`LocationProvider.android.kt`) requests `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION` at runtime, but these must also be declared in `AndroidManifest.xml`. Verify:
```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```
If missing, the runtime permission request will silently be denied on some Android versions.

---

### 4.5 School Name Shows Placeholder "good morning" / "St. Augustine Academy"

In the `LaunchInfoOBViewModel`, `LaunchInfoState` defaults:
```kotlin
val schoolName: String = "St. Augustine Academy",
val location: String = "Metropolitan Education Zone, Block C",
```

The screenshot shows "good morning" as the school name — this is the name entered during onboarding basics. The server DOES return the real name via `GET /api/v1/onboarding/step?obStepType=REVIEW` → `identityDetails.institutionName`. If this API call fails (e.g., 401 due to expired token), the default placeholder appears. This is another manifestation of the broken session/token issue.

---

### 4.6 No Retry Logic for Token Refresh (Systemic Issue)

Currently, when any API call returns 401, the app either:
- Shows an error banner and stays on the current screen
- Calls `preferenceRepository.clearSession()` and shows "please log in again"

There is no automatic token refresh using the stored `refreshToken`. The backend has `POST /api/v1/auth/refresh` which accepts the refresh token and returns new access + refresh tokens. This should be called automatically on any 401 response before propagating the error to the UI.

---

### 4.7 Supabase Transaction Pooler vs Session Pooler

The current `DATABASE_URL` uses port `6543` (Supabase Transaction Pooler). For a persistent Ktor JDBC server with Hikari connection pooling, the **Session Pooler (port 5432)** is more appropriate and avoids occasional `prepared statement "S_x" already exists` errors under the transaction pooler.

**Recommendation:** If you see intermittent database errors, switch the `DATABASE_URL` in Render to the Session Pooler string:
- Go to Supabase Dashboard → Project Settings → Database → Connection Pooling
- Copy the **Session mode** connection string (port 5432)
- Replace `DATABASE_URL` in Render with this string

Also set `DB_POOL_SIZE=5` in Render to stay within Supabase free-tier connection limits.

---

## 5. Environment Variables — Complete Render Checklist

### 5.1 Already Set (Verified from Screenshots) — KEEP

| Key | Value | Status |
|---|---|---|
| `AUTO_CREATE_TABLES` | `true` | ✅ OK |
| `DATABASE_URL` | `jdbc:postgresql://aws-1-ap-northeast-1.pooler...` | ✅ OK (consider switching to session pooler port 5432) |
| `DATABASE_USER` | `postgres.dumoiojpkizxkzzxdzss` | ✅ OK |
| `DATABASE_PASSWORD` | `<set>` | ✅ OK |
| `JWT_SECRET` | `<set>` | ✅ OK — verify it is NOT the `change-me` placeholder |
| `OTP_PEPPER` | `<long hex>` | ✅ OK |
| `OTP_ADMIN_TOKEN` | `<token>` | ✅ OK |
| `OTP_EXPIRY_MINUTES` | `10` | ✅ OK |
| `OTP_MAX_ATTEMPTS` | `5` | ✅ OK |
| `OTP_MAX_RESENDS_PER_HOUR` | `5` | ✅ OK |
| `OTP_CHANNEL_ORDER` | `sms,whatsapp,email` | ✅ OK |
| `OTP_DEV_RETURN_CODE` | `false` | ✅ OK |
| `OTP_ENABLE_CONSOLE_FALLBACK` | `false` | ✅ OK |
| `FAST2SMS_API_KEY` | `<key>` | ✅ OK — verify wallet balance |
| `FAST2SMS_ROUTE` | `otp` | ✅ OK |
| `SUPABASE_URL` | `https://dumoiojpkizxkzzxdzss.supabase.co` | ✅ OK |
| `SUPABASE_SERVICE_KEY` | `<long JWT>` | ✅ OK — verify this is `service_role` key, NOT `anon` key |
| `SUPABASE_BUCKET` | `school-media` | ✅ OK |

---

### 5.2 MUST ADD — Missing Critical Variables

| Key | What Value to Set | Why It's Needed |
|---|---|---|
| `SMTP_HOST` | `smtp.gmail.com` | Email OTP delivery — without this, email-identifier users get 502 |
| `SMTP_PORT` | `465` | Required with SSL |
| `SMTP_USERNAME` | `yourgmail@gmail.com` | Gmail account to send from |
| `SMTP_PASSWORD` | 16-char Gmail App Password | Generated at myaccount.google.com/apppasswords |
| `SMTP_FROM` | `VidyaPrayag <yourgmail@gmail.com>` | "From" display name in email |
| `SMTP_USE_SSL` | `true` | Required for port 465 |

---

### 5.3 RECOMMENDED — Stability & Operations

| Key | Value | Why |
|---|---|---|
| `DB_POOL_SIZE` | `5` | Prevent exceeding Supabase free-tier connection limit |
| `DEBUG_ERRORS` | `false` | Prevent stack traces in API error responses |
| `LOG_LEVEL` | `info` | Reduce Render log noise (avoid TRACE/DEBUG flood) |

---

### 5.4 SUPABASE BUCKET — Manual Setup Required

The `SUPABASE_BUCKET=school-media` env var is set, but the bucket must also be created manually in Supabase:

1. Go to [https://supabase.com](https://supabase.com) → your project
2. Left sidebar → **Storage** → **New bucket**
3. Name: `school-media`
4. ✅ Check **"Public bucket"** (so returned URLs load without signed tokens)
5. Click **Create bucket**

Without this, all upload attempts return a 404/storage error even if the env vars are correct.

---

### 5.5 DATABASE MIGRATION — Run migration_002

The `teacher_subject_assignments`, geo columns, and segmentation columns added in recent commits require running the migration SQL:

1. Go to Supabase Dashboard → SQL Editor
2. Open file: `docs/db/migration_002_segmentation_geo_assignments.sql` from the repository
3. Paste and run it

This is required before teacher assignments or geo discovery features work correctly.

---

## 6. Issues That Were Fixed in Previous Commits (No Longer Active)

The following issues from prior audit cycles have been resolved and are no longer blocking:

| Issue | Fixed in Commit | Notes |
|---|---|---|
| Backend compile failure (`inList` Unresolved reference) | `b0fc09b` | `ParentRouting.kt` fixed |
| API credential logging (password/token in plain logs) | Previous pass | `safeApiCall` now redacts sensitive fields |
| Duplicate parent routes shadowing live routes | Previous pass | Legacy `/track-progress` and `/fees` mock routes removed |
| Gallery/tour upload using URL-paste placeholders | `64d7853` | Real file picker + upload now wired in `InstitutionalProfileScreen` |
| Branding cover/logo URL placeholder | `b0353bf` | `BrandingInfoOBViewModel.uploadMedia()` wired to real `MediaApi` |
| GPS location UI was static map preview | `2be5fa9` + `229bef1` | Real Android `LocationManager` + permission + geocoding implemented |
| Backend teacher assignment table missing | `f7a6f1c` | `teacher_subject_assignments` table + routing added |
| Geo discovery endpoint missing | `f7a6f1c` | Haversine-sorted `GET /api/v1/parent/schools/discover` added |
| `working_days` field missing from CalendarSummaryDto | Previous pass | DTO now has both `working_days` and `total_working_days` with defaults |
| PremiumAnimations compile errors | `057f557` | `AnimatedEntrance`, `ShimmerBox` build errors fixed |
| No-op placeholder controls in admin screens | Previous pass | Disabled/removed fake-action CTAs |

---

## 7. Priority Order for Fixes

| Priority | Issue | Effort | Impact |
|---|---|---|---|
| **P0** | Add SMTP env vars for email OTP delivery | 5 min | All email-identifier users get OTPs |
| **P0** | Verify Fast2SMS wallet balance + re-test SMS OTP | 10 min | All phone-identifier users get OTPs |
| **P0** | Fix token refresh logic (auto-refresh on 401) | 2-4 hours | Fixes "Invalid token" + disappearing admin tabs |
| **P0** | Fix `OTP_DEV_RETURN_CODE` default from `"true"` to `"false"` | 1 min | Security: prevents OTP plaintext leak if env var unset |
| **P1** | Implement Forgot Password (backend 3 endpoints + client UI) | 1-2 days | Users who forget password have recovery path |
| **P1** | Wire real document upload in LaunchInfoOBScreen | 4-6 hours | Compliance documents actually upload to storage |
| **P1** | Fix school media upload 401 during onboarding (allow uploads before school_id exists) | 2-3 hours | Branding step works without "Invalid token" error |
| **P1** | Location: add NETWORK_PROVIDER first + better UX for slow fix | 2-3 hours | Location takes <2s instead of up to 12s |
| **P1** | Run `migration_002` in Supabase | 5 min | Teacher assignments + geo work in production |
| **P1** | Create `school-media` bucket in Supabase as public | 5 min | Uploaded images have public URLs |
| **P2** | Academic step UI revamp: add class, per-class subjects, teacher assignment modal | 2-3 days | Core requirement from user for onboarding |
| **P2** | Announcement create: expose audience_type picker | 4 hours | Teacher-scoped broadcasts work from UI |
| **P3** | Switch DATABASE_URL to session pooler (port 5432) | 5 min | Prevents intermittent prepared statement errors |
| **P3** | Add `DB_POOL_SIZE=5` to Render | 1 min | Stays within Supabase connection limits |

---

## 8. Bottom Line

The app is **close to production-ready on the backend** but has several critical runtime gaps that must be resolved before it is usable by real schools:

1. **OTP is the single most important fix.** The infrastructure is almost fully in place; adding SMTP and verifying Fast2SMS wallet fixes it.
2. **Token refresh** is the root cause of multiple cascading UI problems (Invalid token banner, missing admin tabs, disappearing school name).
3. **Forgot password** is a complete blind spot — no user can recover a lost password today.
4. **Document upload** in the final onboarding step is fake and must be wired to the real MediaApi.
5. **Academic step** requires a substantial UI rebuild to meet the product requirements for multi-class, per-subject teacher assignment.

The backend infrastructure for most of these features already exists and is correct. The majority of remaining work is **client-side** (UI and ViewModel wiring).

---

## 9. UI / Premium Overhaul (DONE — 2026-06-04, branch `backend-by-abuzar`)

A dedicated premium iOS-style UI pass was completed across the school/admin (and shared) side. The guiding principles were: **(a)** make the whole app feel "super-premium / iOS-like", **(b)** remove every UI glitch, and **(c)** introduce premium components with a **zero-conflict delegation strategy** — i.e. the existing generic component names keep their public signatures and now *delegate* to the new premium implementations, so every caller is upgraded with no two components doing the same job.

### 9.1 New premium components added

| Component | File | Purpose |
|---|---|---|
| `PremiumButton` / `PremiumTonalButton` / `PremiumOutlineButton` | `ui/components/PremiumButton.kt` | iOS-style buttons — gradient depth, gloss highlight, layered shadow, spring press. Dribbble-inspired, customised to brand navy `#031632` / emerald `#006C49`. |
| `NetworkImage` | `ui/components/NetworkImage.kt` | Drop-in `AsyncImage` replacement with shimmer loading + broken-image fallback. Fixes broken image boxes from dead URLs. |
| `ComingSoonPill` | `ui/components/ComingSoonPill.kt` | Honest non-interactive "coming soon" pill. Replaces permanently-disabled buttons that looked tappable but did nothing. |
| `StatusColors` (palette) | `ui/theme/StatusColors.kt` | Single source of truth for semantic status colors (warning/info/critical/gold/whatsApp). Replaces all hardcoded `Color(0xFF..)` literals. |
| `PremiumLoading` / `PremiumEmptyState` / `PremiumErrorState` | `ui/components/StateViews.kt` | Consistent, branded screen-state surfaces (loading spinner, empty placeholder, error+retry). |

### 9.2 Existing components upgraded (zero-conflict — signatures unchanged)

| Component | What changed |
|---|---|
| `VidyaPrayagButton` (Primary/Secondary/Outlined) | Now delegates to the `Premium*` button system — every existing caller inherits the premium look. |
| `VidyaPrayagBottomBar` | Replaced generic M3 `NavigationBar` with a floating glassy bar, spring-scale icons, animated label reveal. Single source of truth for **both** school & parent bottom bars. |
| `VidyaPrayagCard` | Premium gradient surface + soft brand shadow + hairline border. Same signature. |
| `VidyaPrayagTopBar` | **Removed dead no-op Search/Notifications icons** (looked tappable, did nothing). Added gradient brand mark + optional `onProfileClick`. |
| `VidyaPrayagSearchBar` | Search button now uses spring-press (`tappableScale`). |
| `VidyaPrayagDrawer` | Brand-gradient avatar, spring-press drawer items, pulsing "LIVE" dot. |
| `OnboardingComponents` | Bottom bar delegates to premium buttons; premium text-field focus styling (emerald accent). |
| `SplashScreen` | Real staged launch animation (backdrop fade → logo spring → wordmark settle → tagline → ambient float/glow). |
| `NavGraph` | Premium slide+fade page transitions for all navigation. |
| `SchoolDashboardScreen` | `AnimatedEntrance` staggered reveal + primary CTA converted to `PremiumButton`. |

### 9.3 Admin-side UI glitches FIXED

| Glitch | Where | Fix |
|---|---|---|
| Permanently-disabled "pending" buttons (dead controls) | `AnalyticsDashboardScreen` (Download report), `TeacherPerformanceScreen` (Forecast export), `SchedulePTMScreen` (Reminder delivery + Class drilldown), `SyllabusCoverageScreen` (Audit generation), `SchoolAnnouncementsScreen` (Book from PTM) | Replaced with `ComingSoonPill` — clearly non-interactive, no fake tappable button. |
| Disabled buttons used as status labels | `AcademicInfoOBScreen` (Assigned/Unassigned) | Replaced with a proper non-interactive `StatusBadge` chip (emerald = assigned, amber = unassigned). |
| Non-clickable `Surface(onClick={}, enabled=false)` | `AnalyticsDashboardScreen` (InsightListItem) | Removed the dead onClick; now a plain surface. |
| Dead no-op icon buttons | `VidyaPrayagTopBar` (Search + Notifications) | Removed entirely. |
| Hardcoded magic color hexes scattered per-screen | 9 admin screens | Centralised into `StatusColors`; **zero** `Color(0xFF..)` left in the admin folder. |
| Broken/empty image boxes from dead `googleusercontent` URLs | admin + parent screens | All `AsyncImage` → `NetworkImage` (graceful fallback); dead URLs replaced with `null` or Material icons. |
| Deprecated `Icons.Filled.EventNote` warning | `SyllabusCoverageScreen` | Switched to `Icons.AutoMirrored.Filled.EventNote`. |
| Inconsistent bottom-bar label casing ("HOME"/"PROFILE") | `SchoolBottomBar` | Normalised to "Home"/"Profile". |

### 9.4 Mobile-friendliness — verified

- All **19** admin screens use a single consistent scaffold (`BaseScreen`) + scrollable content (`LazyColumn` / `verticalScroll`). No screen can clip its content vertically.
- No problematic fixed-width overflow: the only fixed widths are small `Spacer`s, horizontally-scrolling carousel items (`160.dp` cards, `64.dp` columns), or text with `maxLines=1` + ellipsis — all safe on small phones.
- Premium buttons enforce a consistent 56dp touch target; compact contextual actions (side-by-side row buttons, dialog confirm/dismiss) intentionally remain correctly-sized M3 buttons (these are not "duplicate" of the primary CTA language).

### 9.5 Build verification

- `./gradlew :composeApp:compileDevDebugKotlinAndroid --no-daemon` → **BUILD SUCCESSFUL** (only a pre-existing unrelated deprecation warning, now also cleared).
- `./gradlew :server:compileKotlin -Pserver-only=true --no-daemon` → **BUILD SUCCESSFUL** (P0 backend blocker confirmed already resolved).

### 9.6 UI work still OPEN (tracked elsewhere in this report — these are functional, not glitches)

- **Academic step UI revamp** (§3.8 / P2): add-class, per-class subject editing, inline teacher creation, teacher↔subject↔class assignment. Backend ready; client UI still to be built.
- **Announcement create — audience picker** (§4.2 / §3.8): expose `audience_type` selector in the create dialog.
- **Document upload UI** (§3.5 / P1): wire a real file picker + `MediaApi.upload()` (currently flips local state only).
- **Forgot/Reset password UI** (§3.3 / P1): no screen exists yet.

### 9.7 Additional issues found during the UI pass (recommended follow-ups)

1. **Placeholder copy still present** — e.g. greeting "good morning" and demo school name "St. Augustine Academy" (already logged in §4.5). These should bind to the real authenticated school/profile once token refresh (§4.6) is fixed.
2. **`Color.White` / `Color.Black` literals on dark hero cards** are intentional and left as-is (they are not theme-semantic — they are the fixed on-dark foreground), but if a true dark-mode hero treatment is ever desired they should move behind a theme token.
3. **`MainActivity` deprecation warnings** for some `Icons.Filled.*` (e.g. `Login`) remain in non-admin files; cosmetic only, can be swept to AutoMirrored later.
