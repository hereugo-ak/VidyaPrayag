# Enroll+ Website — Local Development & Verification

This is the production marketing **and** onboarding website for **Enroll+**. It lives
in the same monorepo as the mobile app and **talks to the same Ktor backend + Supabase
database** — it does not have its own backend or its own copy of the data. Onboarding a
new school happens **exclusively** through this website; the wizard creates real rows in
Supabase via the existing server routes.

```
repo-root/
├── server/        ← Ktor backend (Supabase Postgres)  — shared, do not duplicate
├── composeApp/    ← Kotlin Multiplatform mobile app
└── website/       ← THIS — Next.js marketing + onboarding site
```

---

## 1. Prerequisites

| Tool | Version | Why |
| --- | --- | --- |
| **Node.js** | 18.17+ (or 20 LTS) | Next.js 14 requires Node ≥ 18.17 |
| **npm** | 9+ | Bundled with Node |
| **JDK** | 17+ | To run the Ktor backend locally |
| A reachable **backend** | — | Local Ktor on `:8080` **or** a deployed URL |

You do **not** need the Android SDK or Xcode to work on the website.

---

## 2. One-time setup

```bash
# from repo root
cd website
npm install
cp .env.example .env.local
```

Then open `.env.local` and point it at a backend:

```bash
# Local Ktor backend (recommended for full onboarding testing):
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080

# …or a deployed backend if you only want to test the UI against staging:
# NEXT_PUBLIC_API_BASE_URL=https://your-backend.example.com
```

> Only this one variable is required. The website never connects to Supabase directly —
> it only calls the backend's public auth routes and the JWT-authed onboarding routes.

---

## 3. Run the backend (for real onboarding)

The onboarding wizard and `/login` make live calls to the backend. To exercise them
end-to-end, start the Ktor server from the **repo root** in a separate terminal:

```bash
# Fast path — skips the Android/iOS/Compose modules (see SERVER_QUICKSTART.md)
./gradlew :server:run -Pserver-only=true      # macOS/Linux
gradlew.bat :server:run -Pserver-only=true    # Windows
```

The server boots on **`http://localhost:8080`**. Leave it running.

> The backend needs its own `local.properties` / env for the Supabase `DATABASE_URL`.
> See the repo-root `SERVER_QUICKSTART.md` and `local.properties.example`. The website
> does not need those credentials.

**Front-end only?** If you just want to work on layout/styling and don't need live
onboarding, you can skip the backend — every marketing page renders without it. Only the
`/onboarding` submit steps and `/login` require a reachable backend.

---

## 4. Run the website

```bash
cd website
npm run dev
```

Open **http://localhost:3000**.

| Script | What it does |
| --- | --- |
| `npm run dev` | Dev server with hot reload (port 3000) |
| `npm run build` | Production build (also type-checks the whole app) |
| `npm run start` | Serve the production build |
| `npm run lint` | ESLint (next/core-web-vitals) |

---

## 5. Verification checklist

Walk through this after `npm run dev` (backend running for the starred items ★):

### Marketing pages
- [ ] `/` — hero, social proof, For Schools (tabbed), For Parents, For Teachers, How it works, testimonials placeholder, final CTA all render.
- [ ] Scroll past 80px → the header gains a blurred background.
- [ ] Resize to mobile → hamburger opens an overlay; body scroll locks; links close it.
- [ ] `/features`, `/pricing`, `/privacy`, `/terms` render with real copy.
- [ ] Reveal animations play **once** on scroll-in (no loops, no parallax on text).
- [ ] All photos load from `images.unsplash.com` with a blur-up, no layout shift.

### Onboarding (★ backend required)
- [ ] `/onboarding` shows the stepper starting on **Account**.
- [ ] Fill **Account** with a fresh email + 8+ char password → "Create account & continue".
- [ ] A real `school_admin` + `school` row is created (check Supabase / server logs).
- [ ] Continue through **Basics → Branding → Academics → Students**; each "Continue" POSTs to `/api/v1/onboarding/submit`.
- [ ] **Students step**: download the template, or upload/paste `sample-students.csv`. The live preview table shows parsed rows + row-level errors. "Import & continue" POSTs the CSV to `/api/v1/school/students/import`; "Skip for now" advances without importing.
- [ ] **Refresh the page mid-wizard** → your inputs and current step are restored (localStorage key `enrollplus.onboarding.v1`).
- [ ] Required-field validation blocks progress and shows inline errors.
- [ ] **Review** lets you jump back to edit any section and shows the imported student count.
- [ ] "Launch my school" → final submission → redirect to `/onboarding/success`, greeted by name.

### Login (★ backend required)
- [ ] `/login` with the admin you just created signs in and routes to `/admin/dashboard`.
- [ ] Wrong credentials show the server's error message inline.

### Admin console (★ backend required)
- [ ] After login, `/admin/dashboard` loads **real** metrics for your school (no mock data) with charts + a live activity feed.
- [ ] The sidebar shows your school name + logo (top) and the signed-in user + logout (bottom); the notification bell shows a **real** unread count.
- [ ] Visiting `/admin/*` while signed out (or after `logout`) redirects to `/login`.
- [ ] `/admin/people` → **Teachers** tab → add a teacher. Confirm a `faculty` row is created in Supabase (`external_id = "U-<userId>"`), not just an `app_users` row.
- [ ] `/admin/people` → **Students** tab → **Import** → upload `sample-students.csv`. The response reports `inserted` / `failed`; students appear in the table.
- [ ] `/admin/attendance`, `/admin/marks`, `/admin/fees`, `/admin/announcements`, `/admin/leave`, `/admin/settings` each render real school-scoped data.
- [ ] `/admin/leave` → Approve/Reject updates the request without a full page reload (SWR mutate).
- [ ] Numbers refresh on their own (e.g. unread count / today's attendance ~15s) without manual reload.
- [ ] **A11y**: `Tab` shows a visible focus ring; the "Skip to content" link appears on first Tab; `Escape` closes the mobile drawer / notification dropdown / any modal.

### Quality gate
- [ ] `npm run build` completes with **no type errors**.
- [ ] `npm run lint` is clean.

---

## 6. How the website talks to the backend

All requests go through the typed client in `src/lib/api.ts`, which wraps the server's
uniform envelope (`{ success, message, data }` / `{ success:false, message, error_code }`).

| Flow | Endpoint | Notes |
| --- | --- | --- |
| Create admin + school | `POST /api/v1/auth/register-school` | Step 1 of onboarding; returns a JWT |
| Submit each onboarding step | `POST /api/v1/onboarding/submit` | JWT-authed; `ob_step_type` = BASIC / BRANDING / ACADEMIC / REVIEW |
| Admin sign-in | `POST /api/v1/auth/login` | `role: "school_admin"` |
| Token refresh | `POST /api/v1/auth/refresh` | Admin client calls this once on a 401 before bouncing to `/login` |
| Admin data | `GET\|POST\|DELETE /api/v1/school/*` | JWT-authed; `school_id` derived from the token — the client never sends it |
| Bulk student import | `POST /api/v1/school/students/import` | Body is a JSON array **or** raw `csv`; returns `{ total, inserted, failed, results[] }` |
| (optional) Resume state | `GET /api/v1/onboarding/status` | Server-truth completion |

The onboarding step order (`REGISTER → BASIC → BRANDING → ACADEMIC → STUDENTS → REVIEW`)
mirrors the server contract in `server/.../feature/onboarding/OnboardingRouting.kt`
(the `STUDENTS` step is a client-side convenience that posts to the existing
`/api/v1/school/students/import` endpoint — it is not a server `obStepType`).

### Real-time & data fetching

The admin console uses **SWR per-metric polling** (`src/lib/admin/hooks.ts`) with three
tiers — `LIVE` (15s: unread count, today's attendance), `NEAR_LIVE` (60s: dashboard tiles,
fees, leave), `SLOW` (5min: profile/config). Mutations call `mutate(key)` to refresh only
the affected query. There is **no websocket** — the backend exposes plain REST.

---

## 7. Troubleshooting

| Symptom | Fix |
| --- | --- |
| "Cannot reach the server" on submit | Backend not running / wrong `NEXT_PUBLIC_API_BASE_URL`. Start Ktor on `:8080`. |
| CORS error in console | Ensure the backend allows the website origin (`http://localhost:3000`) in its CORS config. |
| Email already registered | Use a fresh email — `register-school` enforces a unique email. |
| Images don't load | Confirm network access to `images.unsplash.com`; the host is allow-listed in `next.config.mjs`. |
| Wizard "stuck" with old data | Clear the draft: in DevTools → Application → Local Storage, remove `enrollplus.onboarding.v1`. |
| Type error on build | Run `npm run build` and fix the reported file; the build is the type gate. |
| `/admin/*` keeps redirecting to `/login` | Token expired/missing, or the role isn't a school admin. Sign in again; the client auto-refreshes once on a 401 then bounces. |
| Added a teacher but no `faculty` row | Fixed — `POST /api/v1/school/teachers` now mirrors a `faculty` row (`external_id = "U-<userId>"`). Re-pull the backend if you're on an old build. |
| CSV import reports `failed` rows | The response's `results[]` carries per-row errors. Check headers (`full_name,class_name,roll_number,section,student_code`) and required fields; partial imports still insert the valid rows. |

---

## 8. Project layout

```
website/
├── ARCHITECTURE.md        ← tech decisions + backend API surface
├── LOCAL_DEV.md           ← this file
├── STUDENT_CSV_PROMPT.md  ← prompt + column contract for a ≥20-student roster
├── sample-students.csv    ← validated 24-row example roster
├── next.config.mjs        ← image hosts, formats
├── tailwind.config.ts     ← design tokens (lavender base, navy/accent)
└── src/
    ├── app/               ← App Router pages
    │   ├── (site)/                marketing/onboarding shell (Header + Footer)
    │   │   ├── page.tsx           (/)  + features/ pricing/ privacy/ terms/ login/
    │   ├── onboarding/            (/onboarding + /onboarding/success)
    │   └── admin/                 authenticated console (own chrome)
    │       ├── dashboard/ people/ attendance/ marks/
    │       └── fees/ announcements/ leave/ settings/
    ├── components/
    │   ├── Header.tsx Footer.tsx
    │   ├── ui/            (Button, Field, Photo, Reveal, SectionHeading, Logo)
    │   ├── home/          (Hero, SocialProof, ForSchools, …, FinalCta)
    │   ├── onboarding/    (Wizard, Stepper, steps)
    │   ├── legal/         (LegalLayout)
    │   └── admin/         (AdminShell, Sidebar, Topbar, DataTable, Toolbar,
    │                       Primitives, StatTile, charts)
    └── lib/               (api, auth, content, images, motion, onboarding,
                            admin/{client,hooks,session,types,format})
```

---

**Important:** This website is the *only* place a school is onboarded. The mobile app's
onboarding is intentionally left in place for now and is **not** removed by this work.
