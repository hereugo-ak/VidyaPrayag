# Enroll+ Website — Architecture & Technology Decisions

> The production marketing + onboarding website for **Enroll+** (codebase name:
> *VidyaPrayag / VidyaSetu*). It shares the **same Ktor backend and Supabase
> database** as the Kotlin Multiplatform mobile app. School onboarding happens
> **exclusively from this website** — the website calls the existing
> `/api/v1/auth/register-school` + `/api/v1/onboarding/*` endpoints; it does not
> duplicate any business logic.

This document is written **before any website code** and records *why* each layer
was chosen, evaluated across: developer velocity, production performance, SEO,
animation quality, deployment simplicity, bundle size, and TypeScript support.

---

## 0. What the backend actually exposes (read first)

The website integrates with these **already-existing** Ktor routes (read from
`server/src/main/kotlin/.../feature/**`):

| Flow | Endpoint | Auth | Notes |
|---|---|---|---|
| Register a school + admin | `POST /api/v1/auth/register-school` | public | Atomically creates `app_users` (role=`school_admin`, `profile_completed=false`) + a `schools` row (`onboarded_at=NULL`, status `pending`), returns a JWT (`token`, `refresh_token`). **This is the single sanctioned way to self-mint a school admin.** Email + password (min 8 chars) only. |
| Check identifier | `POST /api/v1/auth/check-user` | public | `{ is_new_user, auth_method_required }` |
| Login | `POST /api/v1/auth/login` | public | Email→password, phone→OTP. Returns `{ token, refresh_token, user_id, name, role, profile_completed, must_change_password }`. |
| Onboarding step schema | `GET /api/v1/onboarding/step?obStepType={BASIC\|BRANDING\|ACADEMIC\|REVIEW}` | JWT | Returns field schema + saved draft values. |
| Submit a step (draft + persist) | `POST /api/v1/onboarding/submit` | JWT | `{ ob_step_type, is_final_submission, data_payload }`. Upserts drafts into `school_onboarding_drafts`, then materialises real rows in `schools` / `school_classes` / `school_subjects` / `app_users(teacher)` / `students`. Final `REVIEW` submit stamps `schools.onboarded_at` → status `active`. |
| Class details | `GET /api/v1/onboarding/academic/class-details?classId={code}` | JWT | |
| Onboarding status | `GET /api/v1/onboarding/status` | JWT | Server-truth completion %, `resume_step`. |
| Finalize (idempotent) | `POST /api/v1/onboarding/complete` | JWT | Mirrors REVIEW final submit. |
| Landing CMS | `GET /api/v1/content/landing` | public | Tagline/feature copy KV store. |

**Server response envelope** (from `core/ApiResponse.kt` / `ResponseExtensions.kt`):
```jsonc
{ "success": true, "message": "…", "data": { … } }      // ok / created
{ "success": false, "message": "…", "error_code": "…" }  // fail
```

**Onboarding step field contracts** (from `OnboardingRouting.kt`, authoritative):

- **BASIC** keys: `school_name`, `board` (CBSE|ICSE|UP_STATE|…), `medium`,
  `school_gender` (co_ed|boys|girls), `contact_email`, `contact_phone`, `city`,
  `district`, `state`, `pincode`, `full_address`, `latitude`, `longitude`.
- **BRANDING** keys: `logo_url`, `brand_color`.
- **ACADEMIC** payload: `{ classes: [{ code, name, sections[], subjects:[{sub_name, sub_code, teacher_assigned?}] }], teachers?: [...], students?: [...] }`. An empty payload seeds sensible defaults server-side.
- **REVIEW**: `is_final_submission: true` finalizes.

The website mirrors **exactly** these keys — no invented fields.

---

## 1. Framework — **Next.js 14 (App Router)** + React 18 + TypeScript

**Decision: Next.js App Router with a hybrid rendering strategy.**

Why it wins on the most dimensions for *this* site (content-rich marketing pages
+ one real, stateful, backend-wired multi-step form):

| Dimension | Why Next.js wins |
|---|---|
| **SEO** | Marketing pages (`/`, `/features`, `/pricing`, `/privacy`, `/terms`) are **statically generated** (SSG) — fully crawlable HTML, perfect for a company that wants principals + investors to find it. This is the decisive factor a pure SPA (Vite/CRA) loses on. |
| **Rendering strategy** | Hybrid: marketing pages are `export const dynamic = 'force-static'` (SSG); the `/onboarding`, `/login` flows are client components (`"use client"`) because they are interactive, auth-gated, and must persist state locally — no SEO value in indexing a private wizard. Best of both worlds in one framework. |
| **Developer velocity** | File-based routing, layouts, built-in `next/image`, `next/font`, and zero-config TS. One toolchain for routing + bundling + image optimization. |
| **Production performance** | Automatic code-splitting per route, streaming, `next/image` AVIF/WebP + blur placeholders out of the box (directly satisfies the "real images, blur placeholder, lazy-load, never stretched" requirement). |
| **Bundle size** | Per-route splitting keeps the marketing pages tiny; Framer Motion only ships on pages that import it. |
| **TypeScript** | First-class, strict mode on. |
| **Deployment simplicity** | `next build` produces a self-contained output deployable to Vercel (1 click), or to **Cloudflare Pages** via `@cloudflare/next-on-pages`, or a Node server. The human can run `next dev` locally and verify against the local Ktor server first. |

**Rejected alternatives:**
- *Vite + React SPA* — no SSG/SSR ⇒ weak SEO for a marketing site; would need a separate image-optimization story.
- *Astro* — superb for static content, but the onboarding wizard is a genuinely
  stateful React island and we'd still pull in React anyway; Next gives one
  mental model for both halves without islands ceremony.
- *Compose/Wasm web (already in the repo)* — huge bundle, poor SEO, not how a
  Series-A marketing site is built. The mobile app uses it; the public website
  should not.
- *Remix* — excellent, but its data-loading model is oriented around its own
  server actions; here the data source is an **external Ktor API**, so Next's
  client-fetch + SSG split is a cleaner fit and has the simpler deploy story.

---

## 2. Styling — **Tailwind CSS v3** with a custom design-token theme

**Decision: Tailwind, configured with the project's real brand tokens.**

| Dimension | Why Tailwind wins |
|---|---|
| **Design control** | Utility classes give per-element control with **zero abstraction overhead** — exactly what a bespoke, non-templated premium design needs. We extend `tailwind.config` with the **real tokens lifted from the app's `VColors.kt` / `VType.kt`** (navy `#26234D`, teal `#3CB9A9`, lavender, ink scale; Plus Jakarta Sans + DM Mono), so the website is visually consistent with the product. |
| **No template look** | We do **not** use a component kit (no shadcn defaults, no MUI). Every component is hand-built so it can't look like a 2022 SaaS template. |
| **Bundle size** | JIT purges to only the classes used → tiny CSS. |
| **Velocity** | Co-located styles, no context-switching to CSS files. |

**Rejected:** CSS Modules (more files, less velocity), styled-components/Emotion
(runtime cost + SSR serialization overhead), a UI kit (templated look — explicitly forbidden).

---

## 3. Animation — **Framer Motion** (motion/react), used with restraint

**Decision: Framer Motion, scoped to the pages that need it.**

| Dimension | Why |
|---|---|
| **Production-quality scroll animations** | `whileInView` + `viewport={{ once: true }}` gives entrance animations that **play once and settle** — directly satisfies "triggered once when the element enters the viewport, nothing loops". |
| **No layout shift** | We animate only `opacity` + `transform` (never layout properties), so there is **zero CLS**. Text never parallaxes. |
| **Micro-interactions** | `whileHover={{ scale: 1.02 }}` + shadow transitions on buttons; nothing bounces/pulses. |
| **Bundle** | Tree-shakeable; only imported in client components. The static marketing shell stays light. |

The house rule encoded in `lib/motion.ts`: fade-up 20–24px, 400ms,
`ease-out-cubic` ([0.16, 1, 0.3, 1]); page fades 200ms. If an animation would be
*noticed as an animation*, it's removed.

**Rejected:** GSAP (heavier, imperative, overkill here), pure CSS keyframes
(harder to gate "animate once on enter" cleanly across a list), Lottie (implies
the illustrated/animated-graphic aesthetic we're avoiding).

---

## 4. Images — real photographs via **Unsplash**, served by `next/image`

**Decision: Real, license-clean photographs sourced from Unsplash, delivered
through `next/image`.**

- **Source:** Unsplash (its license permits commercial use without attribution).
  Search intent: real classrooms, real Indian school buildings, real teachers
  with students, real parents with children. **Zero** AI-generated, **zero**
  illustrated, **zero** generic-stock-vector imagery.
- **Serving strategy (`next/image`):**
  - Modern formats (AVIF/WebP) negotiated automatically.
  - **Blur placeholder** while loading (`placeholder="blur"` with a tiny base64
    `blurDataURL`) — no jarring pop-in.
  - **Lazy loading** by default (hero image uses `priority`).
  - **Aspect ratio enforced** via `fill` + a fixed-ratio wrapper so images are
    **never stretched**.
  - Remote images allow-listed in `next.config` `images.remotePatterns`.
- Images are referenced by stable Unsplash CDN URLs (no binaries committed),
  keeping the repo lean and the CDN doing the heavy lifting.

**Rejected:** committing image binaries (repo bloat), hot-linking random web
images (license risk), AI generation (explicitly forbidden + looks generated).

---

## 5. HTTP client — native **`fetch`** wrapped in a typed `lib/api.ts`

**Decision: the platform `fetch` (no axios), wrapped in a small typed client.**

- Next/React 18 ship `fetch` everywhere — **zero added bytes**.
- `lib/api.ts` centralises: base URL (`NEXT_PUBLIC_API_BASE_URL`), the
  `{ success, message, data }` envelope unwrapping, bearer-token injection, and
  typed error mapping (`error_code` → friendly inline message).
- Token persistence: access + refresh tokens kept in `localStorage` for the
  onboarding session (the wizard is a same-device, short-lived flow); a 401
  triggers a one-shot `/api/v1/auth/refresh`.

**Rejected:** axios (adds ~13KB for features `fetch` already covers),
react-query/SWR (the onboarding flow is a linear wizard, not a cache-heavy data
grid — a thin client is clearer and lighter).

---

## 6. Form state persistence — `localStorage`-backed reducer

The onboarding wizard must **not lose work on accidental refresh** (Definition of
Done). Strategy:

- A single `useReducer` holds all step data.
- Every change is mirrored to `localStorage` under `enrollplus.onboarding.v1`.
- On mount, state rehydrates from `localStorage`; the JWT (from
  `register-school`) is stored under `enrollplus.auth.v1`.
- On successful final submit, the draft is cleared.

This is deliberately dependency-free (no zustand/redux) — one reducer + one
effect is the right size for a 4-step wizard.

---

## 7. Deployment target — **Cloudflare Pages** (primary) / Vercel (1-click)

**Decision: ship as a static-export-friendly Next app; recommend Cloudflare
Pages, with Vercel as the zero-config fallback.** Both are documented in
`LOCAL_DEV.md`.

- The site is **verifiable locally first**: `npm run dev` points at a local Ktor
  server (`http://localhost:8080`) via `NEXT_PUBLIC_API_BASE_URL`. The human
  completes a real onboarding and confirms a row appears in Supabase **before**
  any deploy — exactly the required gate.
- Production build: `npm run build`. The only runtime dependency is the
  `NEXT_PUBLIC_API_BASE_URL` env var pointing at the deployed Ktor backend.
- **CORS:** the backend already locks CORS in production to
  `CORS_ALLOWED_ORIGINS`; the website's deployed origin must be added there.

**Rejected:** bundling the website into the Ktor server's static resources
(couples release cadence, no edge CDN), GitHub Pages (no image optimization /
env handling).

---

## 8. Project layout

```
website/
  ARCHITECTURE.md          ← this file
  LOCAL_DEV.md             ← setup + verification guide
  package.json
  next.config.mjs
  tailwind.config.ts
  tsconfig.json
  .env.example             ← NEXT_PUBLIC_API_BASE_URL
  src/
    app/
      layout.tsx           ← root layout, fonts, header/footer
      page.tsx             ← / homepage (SSG)
      onboarding/page.tsx  ← multi-step wizard (client, backend-wired)
      onboarding/success/page.tsx
      features/page.tsx
      pricing/page.tsx
      privacy/page.tsx     ← reflects real Supabase schema
      terms/page.tsx
      login/page.tsx
      globals.css
    components/
      Header.tsx Footer.tsx
      ui/  (Button, SectionHeading, Reveal, Photo, Field, …)
      home/ (Hero, SocialProof, ForSchools, ForParents, ForTeachers, HowItWorks, Testimonials, FinalCta)
      onboarding/ (Stepper, steps…)
    lib/
      api.ts               ← typed fetch wrapper over the Ktor envelope
      onboarding.ts        ← step schema + payload builders (mirror server keys)
      motion.ts            ← shared animation variants (restraint encoded here)
      images.ts            ← curated Unsplash URLs + blurDataURLs
      auth.ts              ← token storage helpers
```

---

## 9. Brand system (lifted from the app, harmonised with the brief)

| Token | Value | Source |
|---|---|---|
| Base background | `#E6E6FA` (lavender) → `#FCF8FF` near-white | brief + app `lavender` |
| Primary ink | `#1A1838` (navy-deep) / `#26234D` (navy) | `VColors.kt` |
| Secondary ink | `#3D4947` / `#6D7A77` | `VColors.kt` |
| Accent (sparing) | `#26234D` navy for primary CTAs; lavender `#7C6FE0`-family for active states | brief: "accent from the lavender palette, used sparingly" |
| Support accent | teal `#3CB9A9` (one highlight per section max) | `VColors.kt` |
| UI type | Plus Jakarta Sans (400–800) | `VType.kt` |
| Data/number type | DM Mono | `VType.kt` |
| Heading tracking | negative (`-0.02em` h1, `-0.01em` h2) | `VType.kt` |

Headings use weight contrast (ExtraBold vs body Regular), tight negative
tracking; body capped at a readable measure. Accent is rationed — CTAs + one
highlight per section, never gradients beyond a subtle lavender→white wash.
