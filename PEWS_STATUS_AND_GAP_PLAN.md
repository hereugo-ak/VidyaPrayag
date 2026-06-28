# PEWS — Current State, Gap Analysis & Cross-Portal Completion Plan

> **Branch:** `aifeatures`
> **Date:** 2026-06-28
> **Author:** AI Engineering (audit + plan)
> **Companion docs:** `AI_FEATURES_PLAN.md` (strategy) · `PEWS_AI_GATEWAY_IMPLEMENTATION_PLAN.md` (build plan)
> **Purpose:** A single, honest, code-grounded snapshot of *what PEWS actually is right now* across **all three portals** (School Admin, Teacher, Parent) at **both** the backend and frontend levels — plus a prioritised plan of *what is missing* to make the full Sense → Reason → Act → Learn ecosystem work end-to-end, with every party in the loop.
>
> This document is the working brief for the **next** session. Nothing here is fixed yet; this is the map.

---

## 0. TL;DR — the one-paragraph truth

The **PEWS backend is ~90% built and correctly wired** (tables, migrations, snapshot/reason/act/learn services, the daily job, and a full role-scoped API across admin/teacher/parent). The **mobile app (Compose `composeApp`) has working Admin + Teacher PEWS screens** — those are the screenshots you sent (Early Warning, Student Signal). However there are **four real gaps** that break or hollow the ecosystem: (1) a **serialization bug** that produces the "Something went wrong / Fields … missing at path: $.data" crash you saw when an admin updates an intervention; (2) the **AI reasoning layer is dormant** because the provider keys are still empty placeholders (the log line `PEWS reasoning skipped … no AI provider configured` confirms this); (3) the **web School-Admin portal (`website/`) has NO PEWS surface at all** — it still shows only the *legacy* rule-based `early_warning` list and none of the new cohort / interventions / AI / effectiveness / config; and (4) the **Parent loop is half-built** (server endpoint + client API exist, but there is **no parent screen/ViewModel** consuming the nudge, and the **Teacher → Parent "one-tap draft message"** Act step from the plan was never implemented). Fixing #1 is a 10-line change; #2 is a config/key task; #3 and #4 are the real product work to "close the loop."

---

## 0.5 SESSION UPDATE (2026-06-28, evening) — ROOT CAUSE of "AI still off + no parent UI" FIXED

The previous two commits on `aifeatures` fixed the intervention-crash and added the
parent/web surfaces, **but the user still saw (a) no AI in the logs even after
setting all 5 keys, and (b) no parent nudge**. The real root causes were deeper
than the doc above assumed:

**Root cause A — the AI keys in `.env` were never read by the AI layer.**
The server reads config from **three** sources via the `dotenv-kotlin` library:
`.env` (dotenv) → `System.getenv()` → `local.properties`. `DatabaseFactory` used
all three (that's why `DATABASE_URL` works). But `KeyVault`, `EncryptionService`
and `AiService` had their **own** env helpers that only read `System.getenv()` +
`local.properties` — **they never consulted `.env`**. On a normal local/Android-Studio
run a `.env` file is *not* loaded into the JVM process environment, so
`System.getenv("AI_CEREBRAS_API_KEY")` was `null` even though the key sat in `.env`
(the exact location `.env.example` tells you to put it). Result:
`KeyVault.bootstrapFromEnv()` seeded **0** providers →
`AiService.anyProviderConfigured()` = false → every run logged
*"PEWS reasoning skipped … no AI provider configured."*
**Fix:** a single shared, `.env`-aware resolver `core/EnvConfig.kt` (same 3-source
order as `DatabaseFactory`), now used by `KeyVault`, `EncryptionService` and
`AiService`. The AI keys in `.env` are finally visible → providers seed → REASON
turns on.

**Root cause B — the parent UI couldn't appear because the admin could never
enable parent-sharing.** The log shows three consecutive
`400 Bad Request: PUT /api/v1/school/pews/config`. The parent nudge only renders
when `pews_config.parent_share_enabled = true` **and** a real concern exists. The
admin's three attempts to save that toggle all failed because the **server's**
`PewsConfigDto` had **no default values** (every field required) while the JSON
parser lacked `coerceInputValues`; any partial/null field → `MissingFieldException`
→ 400 → toggle never saved → parents saw nothing. The parent VM/screen wiring from
the last commit was actually correct; it was simply never given a `show=true`.
**Fix:** added `coerceInputValues = true` to the server JSON config, gave the
server `PewsConfigDto` the same defaults as the shared client DTO, and made the
config PUT log the real parse reason instead of an opaque 400.

**Net effect of this session (no behaviour was faked):**
- AI REASON lights up the moment the keys (already set) are read → `ai_narrative`
  starts populating on high/medium snapshots; the existing admin/teacher cards
  already render those nullable fields.
- The admin can now save the PEWS config, including `parent_share_enabled`; once
  on (and a real concern exists for a child), the **already-built** parent nudge
  card appears on the parent dashboard.
- The web School-Admin "Early Warning" surface (added last commit) is confirmed to
  build and route (`/admin/early-warning`, `next build` ✓) — it was never a UI
  regression; it shows once deployed.

**Root cause C — admin mobile could not reach Config when the cohort was empty.**
`PewsCohortScreenV2` rendered Effectiveness + Config **inside** the cohort list,
and the screen's `VStateHost` short-circuited to a full-screen "No students need
attention" empty state whenever `cohort.students` was empty. So a school with no
*current* at-risk students could never open the Config card → never flip
`parent_share_enabled` → parents never get a nudge (chicken-and-egg).
**Fix:** the cohort screen now only shows the full-screen empty state when there is
truly nothing to manage (no cohort AND no config AND no effectiveness); otherwise
it renders the list with an inline "all on track" note and keeps the Config +
Effectiveness cards reachable — matching the web workspace, which already renders
those independently of the cohort.

**Files changed this session:**
- `server/.../core/EnvConfig.kt` (new shared resolver)
- `server/.../feature/ai/KeyVault.kt`, `EncryptionService.kt`, `AiService.kt`
  (use `EnvConfig` → read `.env`)
- `server/.../Application.kt` (`coerceInputValues = true`)
- `server/.../feature/pews/PewsRouting.kt` (`PewsConfigDto` defaults + better
  400 diagnostics)
- `composeApp/.../ui/v2/screens/school/PewsCohortScreenV2.kt` (Config/Effectiveness
  reachable when cohort is empty)

---

## 0.6 TRI-PORTAL ACCESS AUDIT (2026-06-28) — what each portal HAS, verified in code

Re-verified end-to-end that every portal has its screens + access wired to a real,
scope-guarded endpoint. Result: **all three portals are complete for the current
phase** (the remaining items are *new features* from §6, not missing wiring).

**🏫 SCHOOL ADMIN — mobile (`composeApp`) + web (`website`)**
| Surface | Mobile | Web | Endpoint (guard `requireSchoolAdmin`) |
|---|---|---|---|
| Cohort + risk bands + band filter | ✅ `PewsCohortScreenV2` | ✅ `PewsWorkspace` | `GET /school/pews/cohort` |
| Per-student drill-down (signals + AI + history) | ✅ `PewsStudentDetailScreenV2` | ✅ `PewsStudentPanel` | `GET /school/pews/student/{code}` |
| Interventions list | ✅ | ✅ | `GET /school/pews/interventions` |
| Update intervention (outcome/close) | ✅ (full-DTO contract, crash fixed) | ✅ | `PATCH /school/pews/interventions/{id}` |
| Effectiveness (LEARN) | ✅ EffectivenessCard | ✅ EffectivenessCard | `GET /school/pews/effectiveness` |
| Config incl. **Share with parents** | ✅ ConfigCard (now reachable when empty) | ✅ ConfigCard | `GET`+`PUT /school/pews/config` |
| Manual Recompute | ✅ | ✅ | `POST /school/pews/run` |
| Entry point | ✅ `onOpenPews` from school home | ✅ "Early Warning" nav item | — |

**🧑‍🏫 TEACHER — mobile**
| Surface | Status | Endpoint (guard `requireTeacherContext` + ownership) |
|---|---|---|
| Own-class at-risk ("Needs Attention") | ✅ `TeacherPewsScreenV2` | `GET /teacher/pews/students` |
| Signals + AI line per student | ✅ | (in the students payload) |
| Interventions (open/own) | ✅ | `GET /teacher/pews/interventions` |
| Mark done / dismiss (outcome) | ✅ (`{updated:true}` contract, matches client) | `PATCH /teacher/pews/interventions/{id}` |
| Entry point | ✅ "Needs Attention" `VActionCard` on teacher home | — |

**👪 PARENT — mobile (read-only, opt-in, gentle)**
| Surface | Status | Endpoint (child-scoped + `parent_share_enabled`) |
|---|---|---|
| Gentle nudge card (label-free) | ✅ `ParentNudgeCard` in `ParentHomeScreenV2` | `GET /parent/pews/{childId}` |
| Nudge ViewModel (3-state, per active child) | ✅ `ParentNudgeViewModel` (Koin-registered) | — |
| Actions deep-link (View attendance / Message teacher) | ✅ routed to academics/messages | — |
| Visibility | ✅ shows only when server returns `show=true` | (gated by config + real concern) |

**Still NEW work (not wiring gaps), per §6 backlog:** teacher→parent one-tap
draft-message (`POST /teacher/pews/interventions/{id}/draft-message`), patterns
endpoint+UI, AI-usage/provider-health admin screen, parent **push** fan-out,
on-write recompute hooks, reassign-owner.

---

## 1. Architecture recap (so the gap map makes sense)

PEWS is an **agent with four stages**, layered on the existing deterministic early-warning SQL:

```
SENSE (deterministic SQL, scheduled)   → pews_risk_snapshots
   │  z-scores / floors + trajectory slopes over attendance/marks/leave
REASON (LLM via AiService, PII-safe)    → ai_narrative / ai_cause / ai_recommendation on the snapshot
   │  only explains the provided signal bundle; never invents a student/number
ACT (auto-assign + notify)              → pews_interventions (owner = class teacher) + Notify(category="pews")
   │  (planned: also draft a gentle parent message for one-tap send)
LEARN (outcome on close)               → intervention.outcome (improved/unchanged/worsened) → effectiveness rollup
```

**Three portals, three scopes (all enforced from the JWT, never the request body):**

| Portal | Scope guard | Should own |
|---|---|---|
| **School Admin** | `requireSchoolAdmin()` | whole-school cohort, AI fields, school/class **patterns**, **effectiveness**, **config**, manual run |
| **Teacher** | `requireTeacherContext()` + assignment scope | **own class only** at-risk students + AI explanation; open/close interventions; draft parent msg |
| **Parent** | child-scoped + gated by `parent_share_enabled` | **own child only**, gentle label-free nudge (no "risk" word, no score) |

---

## 2. CURRENT STATE — component-by-component inventory

Legend: ✅ done & wired · 🟡 partial / built-but-not-connected · ❌ missing · 🐞 has a bug

### 2.1 Database & migrations — ✅ COMPLETE

| Artifact | Status | Where |
|---|---|---|
| `migration_060_ai_gateway.sql` (6 gateway tables) | ✅ | `docs/db/migration_060_ai_gateway.sql` |
| `migration_061_pews.sql` (3 PEWS tables) | ✅ | `docs/db/migration_061_pews.sql` |
| Exposed mappings: `Ai*Table` ×6, `PewsRiskSnapshotsTable`, `PewsInterventionsTable`, `PewsConfigTable` | ✅ | `server/.../db/Tables.kt` (2260–2440) |
| Registered in `DatabaseFactory.allTables` (so `validateSchema()` gates boot) | ✅ | `server/.../db/DatabaseFactory.kt` (233–243) |

> ⚠️ **Action item:** confirm migrations `060` + `061` have been **applied in the live Supabase** (DEV `10.77.243.54:8080`). Since `AUTO_CREATE_TABLES=OFF`, the server only boots if the columns exist — the running log proves they do on DEV. Re-verify before any other environment.

### 2.2 AI Gateway (server) — ✅ BUILT · 🟡 DORMANT (no keys)

| Component | Status | Notes |
|---|---|---|
| `EncryptionService.kt` (AES-256-GCM, IV-prefixed) | ✅ | JVM-native, no new dep |
| `KeyVault.kt` (env→encrypt→`ai_provider_config`, decrypt-on-use cache, rotation) | ✅ | `bootstrapFromEnv()` called in `Application.kt:180` |
| `LlmClient.kt` (one OpenAI-compatible client, swappable base/key/model) | ✅ | covers all 5 providers |
| `AiService.kt` (route → cache → circuit breaker → dual-home failover → guardrails → usage log) | ✅ | lanes `FAST_CHAT/CLASSIFY/REASON/BATCH`; `anyProviderConfigured()` gate |
| `CircuitBreaker.kt`, `GuardrailService.kt` (PII redact) | ✅ | |
| `AiRouting.kt` endpoints | ✅ | `GET /api/v1/school/ai/usage` · `GET /api/v1/admin/ai/providers` · `GET /api/v1/admin/ai/health` · `POST /api/v1/admin/ai/providers/{provider}/rotate` |
| Provider lanes incl. PII pinning (REASON → Cerebras/Groq/OpenRouter for PII) | ✅ | `AiService.laneProviders()` |

> 🟡 **Why nothing AI shows yet:** the 5 `AI_*_API_KEY` values in `.env` / `local.properties` are still **empty placeholders**. `KeyVault.bootstrapFromEnv()` seeds nothing → `AiService.anyProviderConfigured()` returns false → `PewsReasoningService` logs *"PEWS reasoning skipped … no AI provider configured"* and writes `ai_narrative = null`. **This is the graceful-degradation path working as designed** — the deterministic cohort still renders (which is exactly what your screenshots show: real risk bands + signals, but no AI paragraph).

### 2.3 PEWS services (server) — ✅ COMPLETE

| Service | Stage | Status |
|---|---|---|
| `PewsSnapshotService.kt` | SENSE (z-score/floors + slopes, cohort, history, identity enrich) | ✅ |
| `PewsReasoningService.kt` | REASON (LLM explain, PII-safe, cache by signal hash, graceful skip) | ✅ |
| `PewsInterventionService.kt` | ACT (auto-open, owner resolution, notify) + LEARN (close+outcome, effectiveness) | ✅ |
| `PewsDailyJob.kt` | scheduler (hourly tick + run-guard) + `runSchool()` for manual run | ✅ |
| `PewsRouting.kt` | full role-scoped API (admin/teacher/parent) | ✅ (one bug — see §3) |

### 2.4 PEWS API surface (server) — ✅ mostly, with 2 spec gaps

| Endpoint | Status |
|---|---|
| `GET /api/v1/school/pews/cohort?min_level=` | ✅ |
| `GET /api/v1/school/pews/student/{code}` | ✅ |
| `GET /api/v1/school/pews/interventions?status=` | ✅ |
| `PATCH /api/v1/school/pews/interventions/{id}` | 🐞 returns `{updated:true}` but client expects full DTO (the crash) |
| `GET /api/v1/school/pews/effectiveness` | ✅ |
| `GET /api/v1/school/pews/config` · `PUT …/config` | ✅ |
| `POST /api/v1/school/pews/run` | ✅ |
| `GET /api/v1/teacher/pews/students` | ✅ |
| `GET /api/v1/teacher/pews/interventions?status=` | ✅ |
| `PATCH /api/v1/teacher/pews/interventions/{id}` | ✅ (returns `{updated:true}`, client matches) |
| `GET /api/v1/parent/pews/{childId}` | ✅ |
| `GET /api/v1/school/pews/patterns` (school/class sliding cohorts) | ❌ **never implemented** (in plan §A.6) |
| `POST /api/v1/teacher/pews/interventions/{id}/draft-message` | ❌ **never implemented** (Act→Parent, in plan §A.6) |

### 2.5 Shared layer (`shared/` — KMP DTOs / API / repo / VMs) — ✅ · 🐞

| Artifact | Status |
|---|---|
| `domain/model/PewsModels.kt` (all DTOs, mirror server) | ✅ |
| `data/remote/PewsApi.kt` | 🐞 admin `updateIntervention` typed `ApiResponse<PewsInterventionDto>` — **mismatch** (server returns `{updated:true}`) |
| `data/repository/PewsRepositoryImpl.kt` + `domain/repository/PewsRepository.kt` | 🐞 same mismatch propagated |
| `presentation/PewsCohortViewModel.kt` (load, setMinLevel, runNow, loadConfig, saveConfig) | ✅ (no effectiveness/patterns) |
| `presentation/PewsStudentDetailViewModel.kt` (load, interventions, updateIntervention) | ✅ (triggers the crash via the bad API type) |
| `presentation/TeacherPewsViewModel.kt` | ✅ |
| Parent nudge ViewModel | ❌ **missing** (API + repo exist, no VM/screen) |
| DI in `di/Koin.kt` (repo + 3 VMs) | ✅ (lines 388, 514–516) |

### 2.6 Mobile app (Compose `composeApp`) — ✅ Admin + Teacher · ❌ Parent

| Screen | Status | Wiring |
|---|---|---|
| `PewsCohortScreenV2.kt` (admin "Early Warning" + risk bands + filter + Recompute) | ✅ | `SchoolPortalV2.kt:256` |
| `PewsStudentDetailScreenV2.kt` (admin "Student Signal" + interventions) | ✅ 🐞 | `SchoolPortalV2.kt:270` (crash on update) |
| `TeacherPewsScreenV2.kt` (teacher own-class at-risk) | ✅ | `TeacherPortalV2.kt:127` |
| `PewsPreview.kt` (honest home teaser, label-free) | ✅ | home |
| Admin **effectiveness** screen (Learn rollup) | ❌ | endpoint exists, no UI |
| Admin **config** screen (thresholds / parent_share toggle / run freq) | 🟡 | VM has `loadConfig/saveConfig`, no dedicated screen surfacing it |
| Admin **patterns** screen (class sliding cohorts) | ❌ | no endpoint, no UI |
| **Parent** nudge card (in parent dashboard) | ❌ | no screen/VM at all |
| Admin **AI-usage / provider-health** screen | ❌ | `/ai/usage`, `/admin/ai/*` exist, no UI |

> The screenshots you sent (Early Warning risk band, Student Signal with `parent call` intervention + Improved/No change/Dismiss, and the "Something went wrong" crash) are **all the mobile Compose app**, and they match this inventory exactly.

### 2.7 Web School-Admin portal (`website/`, Next.js) — ❌ NO PEWS AT ALL

This is the **largest gap**. The web admin portal:

- Shows only the **legacy** rule-based early-warning via `GET /api/v1/school/dashboard/intelligence` → `early_warning[]` (component `EarlyWarning.tsx`, hook `useDashboardIntelligence`, types `early_warning: EarlyWarningStudent[]`).
- Its only "action" is `notifyParent()` which actually **creates an announcement** — it does **not** touch the PEWS intervention loop.
- Has **none** of: PEWS cohort screen, risk bands by z-score, trajectory arrows, AI narrative/cause/recommendation, interventions (open/own/close), effectiveness, config, patterns, AI-usage.

Per the plan, **Admin is the primary owner of PEWS** — but the portal an admin actually uses on the web is blind to everything PEWS added.

---

## 3. THE CRASH YOU SAW — root cause & exact fix

**Symptom (screenshot):**
> *Illegal input: Fields [id, student_code, name, class_name, section, owner_user_id, action_type, status, opened_at] are required for type … `PewsInterventionDto`, but they were missing at path: $.data*
> on the **Student Signal** screen, after tapping an intervention action (Improved / No change / Dismiss).

**Root cause — a server/client contract mismatch:**

- **Server** `PATCH /api/v1/school/pews/interventions/{id}` returns:
  ```kotlin
  call.ok(mapOf("updated" to true), "Intervention updated")   // → { "data": { "updated": true }, ... }
  ```
- **Client** `PewsApi.updateIntervention()` (admin) is typed:
  ```kotlin
  ): NetworkResult<ApiResponse<PewsInterventionDto>>          // expects a full intervention object in $.data
  ```
- `safeApiCall` deserializes `$.data` as a `PewsInterventionDto`; the object `{updated:true}` has none of the required fields → kotlinx.serialization throws → the screen's `VStateHost` shows "Something went wrong."

**Proof it's only the admin path:** the **teacher** equivalent is already correct:
```kotlin
updateTeacherIntervention(...): NetworkResult<ApiResponse<Map<String, Boolean>>>   // matches {updated:true} ✅
```
The admin Student-Signal screen uses the admin `updateIntervention`, so only it crashes.

**Fix (smallest, safest — align client to the existing server contract):** change the **admin** `updateIntervention` return type from `ApiResponse<PewsInterventionDto>` to `ApiResponse<Map<String, Boolean>>` in three files, and adjust the VM (it already ignores the body and just calls `loadInterventionsFor`):

1. `shared/.../pews/data/remote/PewsApi.kt` — `updateIntervention` return type.
2. `shared/.../pews/data/repository/PewsRepositoryImpl.kt` — same.
3. `shared/.../pews/domain/repository/PewsRepository.kt` — same.
4. `shared/.../pews/presentation/PewsStudentDetailViewModel.kt` — `when (val r = repository.updateIntervention(...))` already only reads success/error, no body field used → no change beyond the type.

> **Alternative (richer) fix:** make the server return the **updated** `PewsInterventionDto` (re-read the row after update and serialize it), so the client can update its list in place without a re-fetch. Slightly more work server-side but a nicer contract. Either is fine; pick the Map fix for speed, the DTO fix for polish. **Do not** leave the current mismatch.

**Regression guard:** after the fix, tapping Improved/No change/Dismiss must (a) not crash, (b) refresh the intervention list, (c) reflect the new `outcome`/`status`.

---

## 4. GAP MAP — what's needed to "make it work as it should," per portal

This is the heart of your question: *what should each party see, what should they add, and what other features connect to PEWS at each portal* to complete the ecosystem.

### 4.1 🏫 SCHOOL ADMIN — the owner of the whole loop

**What they see today (mobile only):** cohort with risk bands + signals; per-student detail; can recompute; can update interventions (once the crash is fixed).

**What's missing for Admin:**

| Gap | Portal | Effort | Priority |
|---|---|---|---|
| **Bring PEWS to the WEB admin portal** (the surface admins actually use): cohort screen, per-student drill-down, interventions board, AI fields, recompute, config | `website/` | L | **P0** |
| **Effectiveness view** (Learn rollup): "X opened / Y resolved; which action types work" — endpoint exists, no UI on either portal | web + mobile | M | P1 |
| **Config screen** (relative-vs-absolute thresholds, floors, run frequency, `ai_narrative_enabled`, **`parent_share_enabled`** toggle) — VM exists, surface it | web + mobile | M | P1 |
| **Patterns view** (class/grade sliding cohorts, e.g. "9-B attendance −6%/week") — **no endpoint yet** + no UI | server + web + mobile | L | P2 |
| **AI-usage / provider-health screen** (`/ai/usage`, `/admin/ai/providers`, `/admin/ai/health`, key rotation) — endpoints exist, no UI | web (admin) | M | P1 |
| **Replace/merge legacy `early_warning`** so the web portal's existing card links into the new PEWS cohort instead of dead-ending at an announcement | web | S | P1 |

**Features that should connect at Admin level:**
- **Announcements / Notifications** — already the spine (`Notify(category="pews")`); web portal should deep-link bell → PEWS student.
- **Attendance & Marks analytics** (the `dashboard/intelligence` data) — PEWS is the *action layer* on top; link the existing attendance/academic-health panels to "open in PEWS."
- **Staff/Teachers** — admin can **reassign** an intervention owner (needs a small endpoint/UI; today owner is auto-resolved only).
- **Students** — per-student profile should show a "Risk history" tab fed by `pews/student/{code}`.

### 4.2 🧑‍🏫 TEACHER — the default intervention owner

**What they see today (mobile):** own-class at-risk students (`TeacherPewsScreenV2`); their interventions; can update them (teacher path already correct).

**What's missing for Teacher:**

| Gap | Effort | Priority |
|---|---|---|
| **One-tap "draft parent message"** (`POST /teacher/pews/interventions/{id}/draft-message`) — the Act→Parent bridge; **no endpoint, no UI**. AI drafts, teacher reviews, sends via existing `MessagesRouting` (never auto-send) | L | **P1** |
| **Deep-link from the PEWS notification** ("Early-warning: <name>") straight into the student card + the recommended action | S | P1 |
| **Per-student card: show AI narrative/cause/recommendation** once keys are live (UI already renders nullable AI fields — verify it shows them) | S | P2 |
| Teacher web surface | n/a (teachers are mobile-only today) | — |

**Features that should connect at Teacher level:**
- **Messaging** — the draft-message flow reuses the existing compose path; this is the single most valuable missing teacher action.
- **Attendance / Marks entry** — on-write hooks should nudge PEWS recompute (today it's the scheduled job + manual run; an on-write trigger would make it "real-time" per the plan).
- **Homework submissions** — optional new signal (non-submission streak) feeding Sense.

### 4.3 👪 PARENT — read-only, opt-in, gentle

**What they see today:** **nothing** — there is no parent PEWS UI. The server endpoint `GET /api/v1/parent/pews/{childId}` and the client `getParentNudge` exist, but **no ViewModel and no screen** consume them.

**What's missing for Parent:**

| Gap | Effort | Priority |
|---|---|---|
| **Parent nudge card** in the parent dashboard (Compose parent portal) — gentle, label-free, with `[View attendance]` / `[Message teacher]` actions; only shows when `show=true` (concern present **and** `parent_share_enabled`) | M | P1 |
| **Parent nudge ViewModel** (consume `getParentNudge`, drive the card's 3-state) | S | P1 |
| **Parent push notification** (`category="pews"` to parent) when a high/medium concern opens — server `notifyOwners` notifies teacher/admin today; **parent fan-out is not wired** and must respect `parent_share_enabled` | M | P1 |
| **Deep-link handling** for the nudge actions into existing parent attendance/messages screens | S | P2 |

**Features that should connect at Parent level:**
- **Attendance screen** (already exists) — the nudge's `[View attendance]` deep-links here.
- **Messages / threads** (already exists) — `[Message teacher]` deep-links here; the teacher's AI-drafted message lands in this thread.
- **Parent Pulse** (existing weekly digest) — the PEWS nudge should be coordinated with Pulse so the parent isn't double-notified; consider surfacing the nudge *inside* Pulse.

---

## 5. The full ecosystem, "in the loop" — target end-state diagram

```
              SENSE (PewsDailyJob, hourly tick + on attendance/marks write)
                         │  writes pews_risk_snapshots (z-score + slopes)
              REASON (AiService, keys live, PII-safe → Cerebras/Groq/OpenRouter)
                         │  ai_narrative / cause / recommendation on the snapshot
                         ▼ ACT
   ┌──────────────────────┼───────────────────────────┬───────────────────────────┐
   ▼                      ▼                           ▼                           ▼
 ADMIN (web+mobile)   TEACHER (mobile)            PARENT (mobile)            PLATFORM ADMIN
 • cohort + bands     • my at-risk students       • gentle nudge card        • AI usage / tokens
 • per-student + AI   • per-student + AI          • [View attendance]        • provider health
 • interventions      • open/close intervention   • [Message teacher]        • key rotation
 • effectiveness  ◄── • outcome (Learn) ──────────► (gets teacher's          (AiRouting endpoints
 • config (incl.      • ONE-TAP DRAFT PARENT MSG ──► AI-drafted message via   already exist)
   parent_share)        (review → send via Messages)  existing thread)
 • patterns          • PEWS push (bell+FCM)       • PEWS push (only if
 • reassign owner                                   parent_share_enabled)
   │                                                        │
   └────────── Notify(category="pews") + deep links ────────┘
                         │  LEARN
              outcome (improved/unchanged/worsened) → effectiveness rollup → admin
```

**Bold = not yet built.** Everything else is built and (mostly) wired.

---

## 6. PRIORITISED BACKLOG for the next session

### P0 — make what exists not crash (hours)
1. **Fix the intervention-update serialization bug** (§3) — 3 type changes in `shared`, verify VM, test the Student Signal action buttons. *This is the visible crash in your screenshot.*

### P0/P1 — turn the AI on (config, no code)
2. **Populate the 5 provider keys** (`AI_CEREBRAS/GROQ/SAMBANOVA/MISTRAL/OPENROUTER_API_KEY`) + `AI_ENCRYPTION_KEY` (`openssl rand -hex 32`) on the server env (Render + local DEV). Restart → `KeyVault.bootstrapFromEnv()` seeds `ai_provider_config` → `anyProviderConfigured()=true` → next recompute writes `ai_narrative`. Verify via `GET /api/v1/admin/ai/providers` and a `POST /pews/run`. *No code change; this lights up REASON.*
3. **Verify the AI fields render** on the existing admin/teacher student cards once narratives exist (UI already supports nullable AI fields).

### P1 — close the human loop
4. **Teacher → Parent draft-message**: implement `POST /api/v1/teacher/pews/interventions/{id}/draft-message` (AiService BATCH/REASON, non-clinical) + teacher review-before-send UI wiring into existing Messages. *Highest-value missing action.*
5. **Parent nudge UI**: parent ViewModel + dashboard card consuming `getParentNudge`, gated on `show`; wire `parent_share_enabled` toggle in admin config; wire **parent push** fan-out (respecting the flag).
6. **Admin config + effectiveness screens** (mobile first, then web): surface the existing endpoints.

### P1 — bring PEWS to the WEB admin portal (the big one)
7. **`website/` PEWS module**: types + client + hooks + screens (cohort, student drill-down, interventions board, config, effectiveness, AI fields), reusing the existing admin design system (`Primitives`, `SidePanel`). Link the legacy `EarlyWarning.tsx` card into it.
8. **Admin AI-usage/provider-health screen** in `website/` (consume `/ai/usage`, `/admin/ai/*`).

### P2 — the "predictive/patterns" depth + real-time
9. **Patterns endpoint + UI** (`GET /api/v1/school/pews/patterns`) for class/grade sliding cohorts.
10. **On-write recompute hooks** (attendance/marks submit → enqueue a light PEWS recompute) to make Sense "real-time" rather than only scheduled/manual.
11. **Reassign-owner** endpoint + admin UI.
12. **Connect cross-feature signals** (homework non-submission, fee-stress proxy) into Sense as optional inputs, per `AI_FEATURES_PLAN.md` §A.3.

---

## 7. Verification checklist (definition of "PEWS works as it should")

- [ ] Admin can update an intervention on mobile **without the crash** (§3 fixed).
- [ ] With keys set, `GET /admin/ai/providers` shows ≥1 configured; a `POST /pews/run` writes `ai_narrative` on high/medium snapshots; cards show the AI paragraph.
- [ ] Teacher can open a flagged student, see AI explanation, **draft a parent message**, review, and send via Messages (never auto-sent).
- [ ] When `parent_share_enabled=true` and a real concern exists, the **parent** sees a gentle nudge card + gets a push; with it off, parent sees nothing.
- [ ] Admin (web) has a real PEWS cohort/interventions/effectiveness/config surface — not just the legacy announcement action.
- [ ] Closing an intervention with an outcome updates the **effectiveness** rollup the admin sees.
- [ ] Every LLM call writes an `ai_usage_log` row; the AI-usage screen reflects per-school tokens + provider health.
- [ ] No fabricated students/numbers anywhere; every AI field traces to a real snapshot (LAW 6).

---

## 8. Files touched / to-touch (map for the implementation PRs)

**Fix now (P0):**
- `shared/.../pews/data/remote/PewsApi.kt`, `.../data/repository/PewsRepositoryImpl.kt`, `.../domain/repository/PewsRepository.kt` — `updateIntervention` return type.

**Config (P0/P1):** server env only (keys) — no code.

**New (server, P1–P2):**
- `feature/pews/PewsRouting.kt` — add `/teacher/pews/interventions/{id}/draft-message`, `/school/pews/patterns`, (optional) reassign-owner; change admin `PATCH …/interventions/{id}` to return the full DTO if going the "richer fix" route; wire parent push in `PewsInterventionService` (gated by `parent_share_enabled`).

**New (mobile `composeApp`, P1):**
- parent PEWS nudge card + ViewModel; admin config & effectiveness screens; teacher draft-message UI.

**New (web `website/`, P1):**
- `src/lib/admin/` PEWS types/client/hooks; `src/components/admin/pews/*` (Cohort, StudentDetail, Interventions, Config, Effectiveness, AiUsage); link from `EarlyWarning.tsx` / `DashboardWorkspace.tsx`.

---

*End of audit. Next session: start at §6 P0 (crash fix) → P0/P1 (keys) → close the loop.*
