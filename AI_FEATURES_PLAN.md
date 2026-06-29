# VidyaPrayag — AI Features Master Plan

> **Branch:** `aifeatures` (cloned from `development_v1.0.0`)
> **Author:** AI Engineering
> **Date:** 2026-06-28
> **Status:** Planning / implementation-ready blueprint
> **Stack grounding:** Kotlin Compose Multiplatform (`composeApp` UI + `shared` domain/data) · Ktor server (`server`) · Supabase Postgres (Exposed ORM, `Tables.kt`) · server-side module-singleton DI (NOT Koin) · DataStore prefs · FCM notifications · multi-tenant by `school_id` resolved from the JWT (never the request body).

---

## 0. How to read this document

This is **not** a generic "let's add AI" wishlist. It is a grounded, agentic plan that:

1. Treats **PEWS (Predictive Early Warning System)** as the **first** AI feature, planned in full depth — because the rule-based version already ships in the codebase (`SchoolIntelligenceRouting.kt` `early_warning` block + `SchoolAnalyticsRouting.kt` `/student-cohort`) and the UI teaser already exists (`PewsPreview.kt`). We are upgrading a real, working surface — not building on sand.
2. Picks the **right free-tier LLM providers** for our use case, with their **actual June-2026 limits** (Cerebras, Groq, SambaNova, Mistral, OpenRouter), and maps each provider to the workload it fits.
3. Defines, for every AI feature: **the real pain point → the agentic solution → the data required (grounded in real tables) → the workflow → what each of the three roles (Parent / Teacher / Admin) sees → how it integrates** into the existing routing/notification/UI spine.
4. Sequences the features (PEWS is #1) so each builds on the shared AI gateway.

> **Relationship to existing specs:** The repo already has `newreviewdocs/specs/AI_INFRASTRUCTURE_SPEC.md` (a Gemini+Ollama gateway design) and a family of `AI_*_SPEC.md` feature specs. **This document supersedes the provider strategy** of `AI_INFRASTRUCTURE_SPEC.md` — we replace "Gemini free tier + local Ollama" with a **multi-provider OpenAI-compatible gateway** (Cerebras / Groq / SambaNova / Mistral / OpenRouter) because (a) we already have keys for these, (b) Ollama needs an always-on 8GB+ box we don't want to operate, and (c) dual-homing the *same open weights* across providers is far more resilient than a single Gemini key. The gateway *architecture* (choke point, circuit breaker, cache, usage log, guardrails) from that spec is kept and re-used.

---

## 1. The product, in one paragraph

VidyaPrayag is a multi-tenant school platform with three live roles — **Parent**, **Teacher**, **School Admin** — backed by a Ktor server and a Supabase Postgres database. It already captures the raw operational truth of a school: attendance (4-state), assessment marks, syllabus coverage, homework + submissions, leave requests, fees, messages, announcements, PTM, and a DB-backed notification spine. **That data is the moat.** Every competitor stores this data; almost none turn it into *action*. The AI program's job is to convert that already-collected data into **timely, explainable, automated action** for each role — starting with catching at-risk students before they fail (PEWS).

---

## 2. The single most important design rule

> **AI augments real data; it never fabricates it.**

This is already a hard law in the codebase. `PewsPreview.kt` carries an explicit comment (RA-S10 / LAW 6) that a previous build rendered *fabricated, named "at-risk" students* and invented counts on the live admin home — and that it was ripped out because "an admin could mistake fiction for signal and act on a student who does not exist." Every AI feature in this plan inherits that law:

- The **deterministic signal layer** (SQL over real tables) decides *who/what* is flagged. This is auditable and reproducible.
- The **LLM layer** only *explains, prioritises, drafts, and recommends* on top of those real signals. It never invents a student, a score, or a number.
- Every AI output is **traceable to the rows that produced it** (the `signals` array pattern already used in `EarlyWarningStudentDto`).
- If there is no data, the UI shows an **honest empty state** (the existing `VStateHost` three-state pattern), never a placeholder.

---

## 3. Free-tier LLM provider analysis (as of June 2026)

We have free-tier keys for **Cerebras, Groq, SambaNova, Mistral, and OpenRouter**. All five expose an **OpenAI-compatible `/chat/completions`** endpoint, which means one `LlmClient` with a swappable `baseUrl` + `apiKey` + `model` covers all of them — no five SDKs.

### 3.1 What each provider actually gives us (June 2026 snapshot)

> ⚠️ Free-tier numbers churn fast (Groq's per-model RPD was cut from 14,400 → ~1,000 during 2026; Cerebras pruned its free catalog from ~12 models to 2 in May 2026 without notice). **Pin by capability, not by exact model name, and keep a cross-provider fallback for every lane.** Numbers below are the best public figures as of late June 2026 and MUST be re-verified at implementation time against each provider's live `/models` endpoint with our real keys.

| Provider | Free tier shape | Headline limit | The real ceiling (TPM) | Representative free models (June 2026) | Speed | Commercial use | Best for |
|---|---|---|---|---|---|---|---|
| **Cerebras** | Permanent free, no card | **~1M tokens/day** | ~30,000 TPM | `gpt-oss-120b`, `zai-glm-4.7`, (Llama/Qwen variants come & go) | Fastest (2,000–2,600 tok/s) | Yes | **Real-time / interactive** (tutor chat, NL query) where latency matters; batch volume |
| **Groq** | Permanent free, no card | **~1,000 req/day** per model (was 14,400) | **~6,000 TPM** on small Llama (the binding limit) | `llama-3.3-70b-versatile`, `llama-3.1-8b-instant`, `llama-4-scout`, `qwen3-32b`, `gpt-oss-120b/20b`, `whisper-large-v3` | Very fast (~320 tok/s on 70B) | Yes | **Short, high-frequency classification** + **Whisper STT** (voice attendance/feedback). Broadest healthy catalog → safest default |
| **SambaNova** | $5 trial credit (then dev tier; free tier ~tokens/day) | **Free tier tokens/day**; dev tier 20M tok/day | model-specific (e.g. DeepSeek-V3.1 ~60 RPM) | Llama 3.1 **405B**, DeepSeek-V3.1, Qwen | Fast (250+ tok/s on DeepSeek) | Yes | **Hardest reasoning** (report-card narrative quality, complex intervention reasoning) via 405B / DeepSeek — the "frontier" tier of our cascade |
| **Mistral** | Permanent "Experiment" free tier | **~1B tokens/month** (very generous) | ~50,000 TPM; **global 1 req/sec/key cap** | `mistral-small`, `mistral-large`, `magistral` (reasoning), `devstral`/`codestral` (code), large vision | Fast | ⚠️ Experiment tier may require **data-training opt-in** | **High-volume batch** (fee reminders, bulk drafts) where the 1B/month budget and big context win — but **never with sensitive PII** because of the training opt-in |
| **OpenRouter** | Free models via one key | **50 req/day** (→ **1,000/day** with one-time $10 top-up) | per-model | 20+ `:free` models (Llama 3.3 70B, Qwen3 Coder 1M-ctx, gpt-oss-120b, …) | routes to provider | No-training on OpenRouter | **Failover & overflow** + **A/B'ing models** through one endpoint; the safety net when a direct provider 404s/throttles |

### 3.2 The provider strategy we will adopt

**Principle: route by task, dual-home every lane.** (Borrowed from the "same open weights, two providers" resilience pattern.)

```
                        ┌─────────────────────────────────────────────┐
   Task class           │  Primary               Fallback             │
   ─────────────────────┼─────────────────────────────────────────────┤
   Real-time chat /     │  Cerebras (fast)   →   Groq (gpt-oss-120b)   │  same gpt-oss weights both sides
   NL query             │                                              │
   ─────────────────────┼─────────────────────────────────────────────┤
   Short classification │  Groq               →  Cerebras  → OpenRouter│  cheap, frequent
   / signal labelling   │  (llama-3.1-8b)                              │
   ─────────────────────┼─────────────────────────────────────────────┤
   Deep reasoning /      │ SambaNova           →  Mistral large →      │  quality tier (cascade escalates here)
   narrative quality    │  (DeepSeek/405B)        OpenRouter           │
   ─────────────────────┼─────────────────────────────────────────────┤
   High-volume batch    │  Mistral            →   Groq → Cerebras      │  ⚠️ no PII to Mistral (training opt-in)
   (non-PII drafts)     │  (1B tok/month)                             │
   ─────────────────────┼─────────────────────────────────────────────┤
   Speech-to-text (STT) │  Groq whisper-large-v3 → (none; degrade)     │  voice attendance/feedback
   ─────────────────────┴─────────────────────────────────────────────┘
```

**Cascade for cost/quality (already in the infra spec):** cheap model first (Groq/Cerebras small) → escalate to the reasoning tier (SambaNova/Mistral-large) only when the deterministic confidence proxy is low. For PEWS specifically, escalation is *rare* because the heavy lifting is deterministic SQL — the LLM only writes the explanation.

**Privacy routing rule (hard):** any prompt that must include **student PII** (names, phone, marks tied to a name) goes **only** to providers with a clear no-training policy at our usage tier — **Cerebras, Groq, OpenRouter**. **Mistral Experiment** (training opt-in) and any trial tier are restricted to **PII-redacted, aggregate, or synthetic** prompts. This is enforced at the gateway by a per-template `pii_allowed_providers` allow-list, building on the existing `GuardrailService.redactPii()` design.

### 3.3 Why this beats the existing "Gemini + Ollama" infra spec

- **No box to babysit.** Ollama needs an always-on 8GB+ RAM host; all five providers above are hosted and free.
- **Resilience by dual-homing.** `gpt-oss-120b` is served by *both* Cerebras and Groq — identical weights, so failover doesn't change output shape. A single Gemini key has no same-weights fallback.
- **Right tool per task.** Groq Whisper gives us free STT (voice attendance) that Gemini-only didn't; SambaNova 405B/DeepSeek gives a genuine "frontier" reasoning tier for narrative quality.
- **We already have the keys.** Zero procurement.

We keep everything else from `AI_INFRASTRUCTURE_SPEC.md`: the gateway choke point, circuit breaker, two-tier cache, `ai_usage_log`, guardrails, batch job queue, and admin usage analytics.

---

## 4. The shared AI Gateway (foundation for every feature, including PEWS)

Every AI feature calls **one** internal service — `AiService` — never a provider directly. This is the choke point where caching, routing, failover, guardrails, usage logging, and cost/quota live. It mirrors the existing server convention: **module-level singletons in a `*Routing.kt` file** (the codebase does NOT use Koin on the server — see `NotificationRouting.kt`).

### 4.1 New tables (Exposed, added to `Tables.kt` + registered in `DatabaseFactory.allTables`)

Production runs `AUTO_CREATE_TABLES=OFF`, so each table ships with a migration SQL under `docs/db/` that MUST be applied before the matching deploy or `validateSchema()` refuses to boot.

| Table | Purpose |
|---|---|
| `ai_provider_config` | provider, model, `api_key_encrypted` (AES-256-GCM), `base_url`, priority, tier (`fast`/`reason`/`batch`), is_active |
| `ai_prompt_templates` | feature, name, version, system_prompt, user_prompt_template (`{{var}}`), `pii_allowed_providers`, guardrail_config, traffic_weight (A/B) |
| `ai_usage_log` | school_id, feature, provider_used, model_used, input/output tokens, latency_ms, routing_decision, status, error — per-school observability + quota |
| `ai_response_cache` | L1 SHA-256 exact-match (+ optional L2 embedding) keyed cache, school-scoped, TTL per feature |
| `ai_jobs` | batch queue (status, total/completed items, result) for class/school-wide runs |
| `ai_provider_health` | per-provider circuit-breaker state + rolling success/latency for dual-home routing |

### 4.2 The `AiService` contract (re-used by all features)

```kotlin
// module-level singletons in AiRouting.kt  (NO Koin — matches server convention)
class AiService(/* repos + httpClient + encryption + circuitBreaker + router + guardrails */) {
    suspend fun complete(
        schoolId: UUID, userId: UUID?, feature: String, templateName: String,
        variables: Map<String, String>,
        taskClass: TaskClass = TaskClass.CLASSIFY,   // FAST_CHAT | CLASSIFY | REASON | BATCH | STT
        containsPii: Boolean = false,                 // gates provider allow-list
        cacheTtlMinutes: Int = 0
    ): AiResult                                       // text + tokens + provider_used + routing_decision

    fun completeStream(...): Flow<AiStreamChunk>       // SSE for long-form (narratives)
    suspend fun submitBatch(...): UUID                 // returns ai_jobs.id
    suspend fun transcribe(audioUrl: String): String   // Groq whisper-large-v3
}
```

`taskClass` + `containsPii` together pick the provider lane from §3.2. Every call writes one `ai_usage_log` row.

### 4.3 Pipeline per request

`guardrails (PII redact / injection check) → A/B template select → L1 cache → provider route (taskClass) → circuit breaker + retry/backoff/jitter → on failure, dual-home fallback → output validation → usage log → return`.

### 4.4 Config & env

`AI_<PROVIDER>_API_KEY` for each of the five; `AI_<PROVIDER>_BASE_URL` (defaults baked in); `AI_ENCRYPTION_KEY` (AES-256-GCM for at-rest keys); per-school monthly token quota in `AppConfigTable` (`ai_monthly_token_limit_{schoolId}`). Rate limiter is in-memory token-bucket per school (no Redis — single-instance server, consistent with existing architecture).

---

# PART A — FEATURE #1: PEWS (Predictive Early Warning System)

> **This is the first AI feature we build.** It is the natural first step because the *deterministic* version already exists and ships, the UI entry point already exists, and it delivers the highest-stakes outcome — catching a child before they fail or drop out.

## A.1 The real pain point

A class teacher manages 35–50 students; an admin oversees hundreds to thousands. **By the time a student visibly fails an exam or stops attending, the slide started weeks earlier** — a few missed Mondays, two low unit tests, a spike in leave applications. The signals are *already in the database*, scattered across `attendance_records`, `assessment_marks`, and `leave_requests`. Nobody has the time to cross-join three tables per student every week. So intervention happens late, reactively, after the damage.

Today's shipped PEWS (`SchoolIntelligenceRouting.kt`) already computes a **rule-based** early-warning list: attendance < 75%, marks < 40%, leave ≥ 3 → a flagged student with a `signals[]` reason array and a `risk_level`. That is excellent and honest — but it is:
- **Static thresholds** (a 92%-average school and a 55%-average school use the same 75/40 cut-offs).
- **Not predictive** (it reports *current* state; it doesn't project trajectory — "this child is *sliding*, not just *low*").
- **Not actionable** (it lists who, with reasons, but no recommended next step, no owner, no follow-up loop).
- **Not closed-loop** (no record of "we acted, did it work?").

## A.2 The agentic solution (what "real-time agentic" means here)

PEWS becomes a **continuously-running agent** with four stages — *Sense → Reason → Act → Learn* — layered on top of (not replacing) the existing deterministic signal layer:

1. **Sense (deterministic, real-time):** A scheduled job (and on-write triggers) recomputes per-student risk signals from real tables. This is the existing `early_warning` logic, **upgraded** with:
   - **School-relative thresholds** (z-scores vs the school/class mean, not fixed 75/40) so it adapts to each school.
   - **Trajectory signals** (slope of attendance & marks over a rolling window) → catches *decline*, not just *low absolute*.
   - **New optional inputs** when those modules exist: homework non-submission streaks (`homework_submissions`), fee-stress proxy (`fee_records` overdue), wellness dips (`wellness_checkins` if Student-Wellness ships).
2. **Reason (LLM, explainable):** For each flagged student, the gateway sends the **deterministic signal bundle** (numbers + reasons, PII-safe routing) to the reasoning lane (SambaNova/Mistral-large) to produce: a **plain-language risk narrative**, a **ranked likely-cause hypothesis**, and a **specific recommended intervention** tied to the actual signals. The LLM is *constrained to the provided signals* — it cannot introduce a student or a number.
3. **Act (automated, owned):** PEWS creates an **intervention task** assigned to an owner (class teacher by default, admin for school-wide patterns), fires a notification via the existing `NotificationsTable` spine, and — where appropriate and approved — drafts a **gentle parent message** for one-tap send.
4. **Learn (closed-loop):** When the owner marks the intervention done, PEWS tracks the student's signal trend for N weeks and records **whether risk improved** — feeding an effectiveness view ("home-visit interventions resolved 70% of attendance cases").

> "Agentic" = it senses on its own schedule, decides who needs attention, drafts the action, routes it to an owner, and verifies the outcome — instead of being a dashboard a human must remember to open.

## A.3 Data required (grounded in real tables)

### Already present (the inputs — no schema change)
| Source table | Signal it feeds |
|---|---|
| `attendance_records` (type=`student`, 4-state) | attendance %, **trajectory slope**, day-of-week pattern (e.g. "absent every Monday") |
| `assessment_marks` + `assessments` (maxMarks) | average %, **trajectory slope**, subject-specific dips |
| `leave_requests` (requesterRole=`student`) | leave frequency spike |
| `students` (isActive, className, section, studentCode) | cohort scoping, identity for the flag |
| `homework_submissions` (if used) | non-submission streak |
| `fee_records` (status OVERDUE) | financial-stress proxy (optional, privacy-gated) |
| `school_classes` / `enrollments` | class-mean baselines for z-scoring |
| `notifications` | the delivery spine for alerts |

### New tables (the agent's memory — to add to `Tables.kt`)
```sql
-- 1. Snapshot of each computed risk run (auditable, reproducible)
CREATE TABLE pews_risk_snapshots (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id     UUID NOT NULL,
    student_code  TEXT NOT NULL,
    run_date      DATE NOT NULL,
    risk_score    INTEGER NOT NULL,        -- 0..100 composite, deterministic
    risk_level    VARCHAR(8) NOT NULL,     -- watch | medium | high
    attendance_pct INTEGER, marks_pct INTEGER, leave_count INTEGER,
    attendance_slope REAL, marks_slope REAL,   -- trajectory (negative = sliding)
    signals_json  TEXT NOT NULL,           -- the deterministic reasons array
    ai_narrative  TEXT,                     -- LLM explanation (nullable; null = not yet reasoned)
    ai_cause      TEXT,                     -- LLM likely-cause hypothesis
    ai_recommendation TEXT,                 -- LLM recommended intervention
    ai_provider_used VARCHAR(32),           -- observability
    created_at    TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(school_id, student_code, run_date)
);
CREATE INDEX idx_pews_snap ON pews_risk_snapshots(school_id, run_date DESC, risk_level);

-- 2. Intervention tasks (the Act + Learn loop)
CREATE TABLE pews_interventions (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id     UUID NOT NULL,
    student_code  TEXT NOT NULL,
    snapshot_id   UUID,                     -- FK pews_risk_snapshots.id
    owner_user_id UUID NOT NULL,            -- assigned teacher/admin
    action_type   VARCHAR(32) NOT NULL,     -- parent_call | home_visit | counselling | remedial_class | parent_message | observe
    status        VARCHAR(16) NOT NULL DEFAULT 'open', -- open | in_progress | done | dismissed
    notes         TEXT,
    opened_at     TIMESTAMP NOT NULL DEFAULT now(),
    resolved_at   TIMESTAMP,
    outcome       VARCHAR(16),              -- improved | unchanged | worsened (filled by Learn stage)
    created_at    TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_pews_intv ON pews_interventions(school_id, status, owner_user_id);

-- 3. Per-school tuning (so thresholds aren't hardcoded)
CREATE TABLE pews_config (
    school_id            UUID PRIMARY KEY,
    use_relative_thresholds BOOLEAN NOT NULL DEFAULT true,
    attendance_floor_pct INTEGER NOT NULL DEFAULT 75,   -- fallback absolute floor
    marks_floor_pct      INTEGER NOT NULL DEFAULT 40,
    leave_floor_count    INTEGER NOT NULL DEFAULT 3,
    run_frequency        VARCHAR(8) NOT NULL DEFAULT 'daily', -- daily | weekly
    ai_narrative_enabled BOOLEAN NOT NULL DEFAULT true,
    parent_share_enabled BOOLEAN NOT NULL DEFAULT false, -- gates parent-facing summaries
    updated_at           TIMESTAMP NOT NULL DEFAULT now()
);
```

## A.4 The full workflow (end-to-end)

```
            ┌──────────────────────────────────────────────────────────────┐
   SENSE    │ Scheduled job (06:00 IST, freq from pews_config) +            │
            │ on-write hooks (attendance/marks submit) recompute signals    │
            │ per active student from REAL tables → write pews_risk_snapshots│
            │ (deterministic risk_score, z-score relative thresholds, slopes)│
            └───────────────────────────────┬──────────────────────────────┘
                                            │ for students newly high/medium
            ┌───────────────────────────────▼──────────────────────────────┐
   REASON   │ AiService.complete(feature="pews", taskClass=REASON,          │
            │   containsPii=true → routes to Cerebras/Groq/OpenRouter only) │
            │ Input = deterministic signal bundle ONLY (numbers + reasons). │
            │ Output JSON {narrative, likely_cause, recommendation} →        │
            │ stored back on the snapshot row. Cached (L1) per signal hash.  │
            └───────────────────────────────┬──────────────────────────────┘
                                            │
            ┌───────────────────────────────▼──────────────────────────────┐
   ACT      │ Auto-create pews_interventions (owner = class teacher;         │
            │ admin for school-wide patterns). Emit NotificationsTable row   │
            │ (category="pews", deepLink to the cohort/student). Optionally  │
            │ draft a parent message (teacher one-tap approves & sends via   │
            │ existing messaging).                                          │
            └───────────────────────────────┬──────────────────────────────┘
                                            │ owner marks done
            ┌───────────────────────────────▼──────────────────────────────┐
   LEARN    │ Track student's signal trend for N weeks post-intervention.    │
            │ Set pews_interventions.outcome (improved/unchanged/worsened).  │
            │ Aggregate → "which interventions actually work" effectiveness. │
            └──────────────────────────────────────────────────────────────┘
```

## A.5 What each role sees

### 🏫 School Admin (primary owner of PEWS — entry already on admin home)
- **Risk radar (upgraded):** the existing PEWS card → real cohort, now with **risk bands by z-score**, **trajectory arrows** (sliding vs stable vs improving), and counts that are **all real** (no fabricated rows — keeps the `PewsPreview` honesty law).
- **Cohort table:** ranked at-risk students with `signals[]`, the **AI narrative + likely cause + recommended action**, and **who owns the intervention**.
- **Pattern view (school-wide):** "Grade 9-B attendance is sliding 6%/week" → admin-level intervention (not a single student).
- **Effectiveness view (Learn):** "Interventions opened vs resolved; which action types work."
- **Config:** tune thresholds / relative-vs-absolute / run frequency / enable parent sharing (`pews_config`).

### 🧑‍🏫 Teacher (the default intervention owner)
- **My at-risk students:** only their own class/section (scoped exactly like the existing teacher write-plane).
- **Per-student card:** the real signals + AI explanation + **recommended next step** + a **one-tap draft parent message** (uses existing messaging + notification fan-out; teacher reviews before send — never auto-sent without approval).
- **My interventions:** open/in-progress/done task list; mark done → triggers the Learn loop.

### 👪 Parent (read-only, opt-in, gentle, non-clinical)
- **Only if `pews_config.parent_share_enabled`** for that school. Parents never see the word "risk" or raw scores.
- A **supportive nudge** tied to a *real* fact already visible to them: e.g. "Aarav has missed 4 Mondays this month — a quick check-in might help" with **[View attendance] [Message teacher]** action buttons (reuses the Smart-Notifications actionable-card pattern).
- Parents see *their own child only*, and never another student's data (existing multi-tenant + child-scoping rules).

## A.6 API surface (extends existing school routes)

```
# Admin / Teacher (JWT, school-scoped via requireSchoolContext / teacher assignment scope)
GET  /api/v1/school/pews/cohort?level=high&class=9B     -> ranked snapshots + AI fields
GET  /api/v1/school/pews/student/{studentCode}          -> full signal + narrative + history
GET  /api/v1/school/pews/patterns                        -> school/class-level sliding cohorts
POST /api/v1/school/pews/run                             -> manual recompute (admin)
GET  /api/v1/school/pews/config  / PATCH ...             -> per-school tuning
POST /api/v1/school/pews/interventions                   -> open an intervention (owner)
PATCH /api/v1/school/pews/interventions/{id}             -> status/notes/outcome
GET  /api/v1/teacher/pews/my-students                    -> teacher-scoped at-risk list
POST /api/v1/teacher/pews/interventions/{id}/draft-message -> AI draft parent message (review-before-send)
# Parent (only when parent_share_enabled)
GET  /api/v1/parent/pews/nudge/{childId}                 -> gentle non-clinical nudge (or empty)
```

The cohort endpoint keeps the existing `/student-cohort` and `early_warning` shapes for backward compatibility; PEWS adds the AI fields + intervention loop on top.

## A.7 Integration with what already exists

- **Reuses** `SchoolIntelligenceRouting.early_warning` SQL as the deterministic core (upgraded with slopes + z-scores).
- **Reuses** the `signals[]` + `risk_level` DTO shape (`EarlyWarningStudentDto`) — clients already render it.
- **Reuses** `NotificationsTable` + `NotificationService` (FCM) for alerts — PEWS just adds `category="pews"`.
- **Reuses** the existing messaging compose path for parent drafts (no new send infra).
- **Reuses** `PewsPreview.kt` as the honest home teaser; the real numbers stay one tap away on the cohort screen.
- **New UI:** `composeApp/.../ui/v2/screens/school/PewsCohortScreen.kt`, `PewsStudentDetailScreen.kt`, teacher `PewsMyStudentsScreen.kt` (all using `VStateHost` three-state + V* design tokens).

## A.8 Provider mapping for PEWS specifically

- **Reasoning (narrative/cause/recommendation):** `taskClass=REASON`, `containsPii=true` → **Cerebras → Groq (gpt-oss-120b) → OpenRouter** (PII-safe, no-training providers only; SambaNova used only on PII-redacted aggregate prompts for the school-wide pattern view).
- **Volume:** PEWS reasons only over *newly* flagged/changed students (tens, not thousands) per run → comfortably inside free tiers. Batched class/school runs go through `ai_jobs`.
- **Caching:** narrative cached on a hash of the deterministic signal bundle → identical signals never re-call the LLM.

## A.9 PEWS acceptance criteria
- [ ] Deterministic risk recompute runs on schedule + on attendance/marks write, writes `pews_risk_snapshots` with z-score relative thresholds and trajectory slopes.
- [ ] No fabricated students/numbers anywhere (honesty law upheld; every AI field traces to a real snapshot).
- [ ] AI narrative/cause/recommendation generated only from the deterministic signal bundle, PII-routed to no-training providers, cached by signal hash.
- [ ] Interventions can be opened, owned, completed; outcome tracked (Learn loop).
- [ ] Admin sees cohort + patterns + effectiveness; teacher sees own-class scoped; parent sees gentle nudge only when enabled.
- [ ] All endpoints school-scoped from JWT; teacher endpoints assignment-scoped; parent endpoints child-scoped.
- [ ] Every LLM call writes an `ai_usage_log` row with provider_used + routing_decision.

---

# PART B — SUBSEQUENT AI FEATURES / AGENTS (the roadmap after PEWS)

Each feature below follows the same discipline as PEWS: a **real pain point**, an **agentic solution grounded in real tables**, the **data required**, the **workflow**, **what each of the three roles sees**, and **how it integrates** with the existing spine. They are numbered in the recommended build order. All reuse the §4 AI Gateway and the §3.2 provider lanes. Several have existing skeleton specs in `newreviewdocs/specs/` (noted) — this plan unifies them under one provider/gateway strategy.

---

## Feature #2 — AI Report Card (Narrative + Predictive)
*(existing skeleton: `AI_REPORT_CARD_SPEC.md`)*

**Pain point:** Report cards are grade sheets. Parents get a number with no meaning ("64 in English"), teachers hand-write 40 comments per term under deadline, and nothing tells a parent *what to do*.

**Agentic solution:** At term close, an agent aggregates each student's real marks/attendance/competency history, **drafts a narrative comment + predictive note + recommended focus area**, queues it for the **teacher to review/edit before publish** (never auto-published), then the admin publishes — reusing the existing results-publish + parent-notify path.

**Data required:** `assessment_marks` + `assessments` (term rollup), `attendance_records`, `parent_achievements` (COMPETENCY/EI_METRIC), `academic_years` (term scoping). **New:** `report_card_drafts` (student, term, narrative, prediction, focus_area, status draft/approved/published, edited_by).

**Workflow:** term-end batch job (`ai_jobs`) → per-student narrative (REASON lane, PII-safe) → teacher review queue → edit → admin publish → parent notification (existing trigger).

**Roles:**
- **Teacher:** review/edit queue, bulk-approve, regenerate one.
- **Admin:** publish (reuses existing results publish + notify), oversight.
- **Parent:** reads narrative report on the academics "Report" tab (currently a schematic placeholder per `FEATURES_CURRENT_PHASE.md` — this fills it with real content), with predictive note + focus area.

**Provider:** REASON lane (SambaNova/Mistral-large for quality), streamed (SSE) for long narratives, cached per student-term signal hash.

**Integration:** reuses results-publish notify trigger; fills the existing "AI Report Card" honest placeholder; PEWS focus-areas can seed the report's recommended-focus.

---

## Feature #3 — Teacher Copilot (Lesson Plan + Question / Assessment Generator)
*(existing skeletons: `AI_EXAM_ANALYSIS_SPEC.md`, lesson-planning tables already in `Tables.kt`)*

**Pain point:** Teachers spend ~60% of their time on content creation and grading. Lesson plans, MCQs, and exam papers are written from scratch every time.

**Agentic solution:** From a syllabus unit, generate a **board-aware lesson plan** (CBSE/ICSE/State from `schools.board`) and a **question set** (MCQ + short/long, difficulty mix) the teacher can edit and attach to an assessment. Plus **exam analysis**: after marks entry, auto-summarise class performance + flag the topics most students failed → feeds PEWS and remedial planning.

**Data required:** `syllabus_units` / `curriculum_units`, `lesson_plans` + `lesson_plan_templates` (already exist), `assessments`, `assessment_marks`, `school_subjects`, `schools.board`. **New:** `question_bank` (unit, type, difficulty, question, answer, source=ai/manual).

**Workflow:** teacher picks unit → generate (BATCH/REASON lane) → edit → save to `lesson_plans` / `question_bank` → optionally attach questions to an `assessment`. Exam analysis runs on marks-submit.

**Roles:**
- **Teacher (primary):** generate/edit lesson plans & questions; see exam analysis ("18/40 missed Q7 — ratios; suggested remedial").
- **Admin:** oversight, reuse across teachers, see school-wide topic gaps.
- **Parent:** indirect — better-targeted homework/remedials; exam analysis can surface "focus area" in the report.

**Provider:** generation can use **Mistral** (1B/month batch budget, non-PII content) and **Cerebras/Groq** for speed; auto-grading of objective questions on the fast lane.

**Integration:** writes to existing `lesson_plans`/`assessments`; exam-analysis topic-gaps feed PEWS signals and the AI Report Card focus area.

---

## Feature #4 — Smart Attendance Insights Agent

**Pain point:** Every ERP collects attendance; none *analyse* it. Patterns ("absent every Monday", "class dips before exams") and the attendance↔marks correlation are invisible.

**Agentic solution:** A pattern-detection agent over `attendance_records`: day-of-week patterns, sliding cohorts, exam-day dips (the existing intelligence endpoint already overlays exam markers + anomaly flags — extend it), and an **attendance↔marks correlation** statement. Auto-nudge parents on emerging patterns with gentle, actionable cards.

**Data required:** `attendance_records` (4-state), `assessment_marks` (correlation), `assessments` / `academic_calendar` (exam overlay — already wired in `SchoolIntelligenceRouting`), `notifications`. **New (optional cache):** `attendance_patterns` (student/class, pattern_type, detail, detected_at).

**Workflow:** scheduled scan → pattern rows → LLM phrases the nudge (FAST/CLASSIFY lane) → actionable parent notification + teacher/admin heatmap.

**Roles:**
- **Teacher:** class heatmap + per-student pattern flags.
- **Admin:** school-wide patterns + correlation insight.
- **Parent:** "Aarav has missed 4 Mondays — [View attendance] [Message teacher]".

**Provider:** mostly deterministic SQL; LLM only phrases nudges → cheap FAST/CLASSIFY lane (Groq/Cerebras).

**Integration:** extends the existing `attendance_timeline` anomaly/exam-overlay block; shares the actionable-notification pattern; feeds PEWS attendance signals.

---

## Feature #5 — AI Fee Reminder & Collection Agent
*(existing skeleton: `AI_FEE_REMINDER_SPEC.md`)*

**Pain point:** Fee follow-up is manual, generic, and often nagging — hurting goodwill while still missing dues. Reminders ignore each family's pattern (always-on-time vs chronically late vs genuinely struggling).

**Agentic solution:** An agent segments families by real payment behaviour (from `fee_records`), drafts **tone-appropriate, multilingual** reminders (firm vs gentle vs offer-help), schedules them at sensible times, and escalates only when needed. **No PII to training-opt-in providers** — names redacted or routed to no-training lanes; or send only aggregate templates filled client-side.

**Data required:** `fee_records` (amount, status PAID/DUE/OVERDUE, dueDate, lastRemindedAt, category), `app_users.languagePref` (multilingual), `notifications` + WhatsApp logs (delivery). 

**Workflow:** daily scan of due/overdue → segment → draft (BATCH lane, PII-safe) → admin approves template / auto-send per policy → actionable card ([Pay] [Request extension]) → track response.

**Roles:**
- **Admin (primary):** configure tone/policy, review drafts, see collection funnel.
- **Teacher:** not involved (clean separation).
- **Parent:** receives a respectful, language-matched reminder with actions.

**Provider:** **Mistral** for high-volume drafting of *non-PII* templates; PII-bearing personalised sends go to Cerebras/Groq/OpenRouter only.

**Integration:** reuses `fee_records`, `notifications`, WhatsApp provider, `languagePref`; pairs with the future payment-gateway work.

---

## Feature #6 — Natural-Language Query (Ask-Your-School)
*(existing skeleton: `AI_NL_QUERY_SPEC.md`)*

**Pain point:** Admins/teachers want answers ("which Grade-9 students dropped below 60% this term and also have <80% attendance?") but must click through five screens or wait for IT.

**Agentic solution:** A **guarded text-to-query** agent: natural-language question → a **whitelisted, parameterised, school-scoped** query (never free-form SQL execution) → a tabular answer + short summary. Strictly read-only, role-scoped, and **always `school_id`-filtered from the JWT**.

**Data required:** read access to the analytics views already powering dashboards (attendance, marks, fees, cohort). **New:** `nl_query_log` (question, resolved_intent, row_count, user, latency) for audit.

**Workflow:** question → intent/slot extraction (REASON lane) → map to a vetted query template (no raw SQL to DB) → execute school-scoped → summarise result (FAST lane).

**Roles:**
- **Admin:** broad school-scoped questions.
- **Teacher:** scoped to own classes.
- **Parent:** scoped to own child only ("how is my child's attendance trending?").

**Provider:** REASON for parsing, FAST for summarising; outputs validated against an allowed-template list (guardrail) — prevents prompt-injection from running arbitrary queries.

**Integration:** sits on existing analytics queries; reuses role/school scoping guards; logs every query.

---

## Feature #7 — VidyaSetu AI Tutor & Doubt Solver (needs Student surface)

**Pain point:** Parents can't help with every subject; private tuition is costly. A child stuck on a homework problem at 9pm has nowhere to turn that knows their syllabus.

**Agentic solution:** A syllabus-aware tutor: generates **personalised practice on weak areas** (from marks), explains a **photo of a homework problem step-by-step** (Groq/Mistral vision/STT + reasoning), and proposes a **study plan** before a test. Requires a **student-facing surface** (today only parent/teacher/admin roles exist) — interim version runs inside the parent app "for your child".

**Data required:** `assessment_marks` (weak areas), `syllabus_units`/`syllabus_progress`, `homework`/`homework_submissions`, `assessments` (upcoming tests). **New:** `ai_tutor_sessions`, `ai_tutor_messages`, `study_plans`, `doubt_submissions` (photo URL via existing SupabaseStorage).

**Workflow:** student/parent asks or snaps → REASON/vision lane → step-by-step solution + linked practice → optional study plan before a dated assessment.

**Roles:**
- **Parent/Student:** interact, get help, practice.
- **Teacher:** oversight (what topics students struggle with) → feeds Copilot remedials.
- **Admin:** usage + cost view.

**Provider:** Cerebras/Groq for fast interactive chat; SambaNova/Mistral for hard reasoning & vision; Groq Whisper for spoken doubts.

**Integration:** consumes the same academic data moat as PEWS/Report Card; topic-struggle signals loop back to Teacher Copilot and PEWS.

---

## Feature #8 — Student Wellness Early-Warning (sibling of PEWS, non-academic)
*(existing skeleton: `STUDENT_WELLNESS_SPEC.md`)*

**Pain point:** Academic risk is only half the story; emotional/behavioural decline is invisible until a crisis. Counsellors have no early signal.

**Agentic solution:** Mood check-ins + weekly wellness survey + teacher behavioural observations → an AI early-warning that flags **declining wellness patterns** to the counsellor/admin (NOT parents by default), with intervention tracking — the same Sense→Reason→Act→Learn shape as PEWS but on wellness data, with stricter privacy.

**Data required:** **New** `wellness_checkins`, `wellness_surveys`, `teacher_observations`, `wellness_alerts` (per the existing spec).

**Workflow:** daily 6 AM job over students with ≥5 check-ins/14 days → trend analysis (REASON lane, PII-restricted) → `wellness_alert` → FCM to counsellor/admin → intervention log.

**Roles:**
- **Teacher:** log observations; class wellness view.
- **Admin/Counsellor:** alerts dashboard, intervention tracking.
- **Parent:** opt-in, non-clinical weekly summary only ("Aarav seems to be doing well") — never raw data.

**Provider:** REASON lane, **PII-restricted to no-training providers**; this is the most privacy-sensitive feature — guardrails strictest.

**Integration:** mirrors PEWS architecture (snapshots + interventions); wellness dips can feed PEWS as an additional risk signal (with consent/role gating).

---

## Feature #9 — One-Tap Lesson Capture (Voice attendance + Photo→Homework)

**Pain point:** Teachers waste classroom time on data entry — typing attendance and homework.

**Agentic solution:** **Voice attendance** ("Aarav present, Priya absent…") via Groq Whisper → mapped to the roster; **photo of the blackboard homework** → OCR/vision extracts text → drafts a homework post with due date. Teacher confirms; saves to existing tables.

**Data required:** `attendance_records`, `students` (roster mapping), `homework` + `homework_attachments`. **New:** none required (optional `homework_templates` for quick presets).

**Workflow:** speak/snap → STT/vision lane → parsed draft → teacher confirm → write to existing attendance/homework tables → existing parent notifications fire.

**Roles:**
- **Teacher (primary):** 10× faster capture.
- **Parent:** unchanged downstream (still gets homework/absence notifications).
- **Admin:** unchanged.

**Provider:** **Groq whisper-large-v3** (STT, free), vision-capable model (Mistral/Groq) for OCR.

**Integration:** writes to existing `attendance_records`/`homework`; triggers existing notification fan-out — zero new delivery infra.

---

## Feature #10 — Multilingual Communication Engine

**Pain point:** India has 22+ official languages. A teacher writes English; a parent reads Hindi/Marathi/Tamil. Messages and announcements don't translate.

**Agentic solution:** Auto-translate **messages and announcements** to each recipient's `languagePref` at read time (cached), so teachers post once and every parent reads in their language. Optional voice-note translation.

**Data required:** `messages`, `announcements`, `app_users.languagePref` (already exists), `schools.medium`. **New:** `translation_cache` (source_hash, target_lang, text) to avoid re-translating.

**Workflow:** on read/notify → check cache → else translate (FAST lane, non-PII content) → cache → render in recipient language.

**Roles:**
- **Teacher/Admin:** write once.
- **Parent:** read in own language.
- All: set preferred language.

**Provider:** FAST lane (Cerebras/Groq) for low-latency translation; Mistral for batch announcement translation. Content-only (no sensitive PII beyond the message itself → route to no-training lanes).

**Integration:** wraps existing messaging/announcement render; reuses `languagePref`.

---

## B.x Cross-feature data & signal flywheel

```
            Smart Attendance Insights ─┐
            Exam Analysis (Copilot) ───┼──► PEWS signals (richer risk)
            Wellness Early-Warning ────┘
                    │
            PEWS recommended focus ───► AI Report Card focus area
                    │
            Tutor topic-struggles ────► Teacher Copilot remedials
```
Every feature both *consumes* the shared data moat and *emits* a new signal that makes the others smarter — the lock-in flywheel.

---

# PART C — Sequencing, effort, and risks

## C.1 Build order (why this order)

| # | Feature | Why this slot | Effort | New tables | Existing spec |
|---|---|---|---|---|---|
| 0 | **AI Gateway** (§4) | Foundation everything needs | M | 6 | `AI_INFRASTRUCTURE_SPEC.md` (provider strategy superseded) |
| 1 | **PEWS** (Part A) | Highest stakes; deterministic core + UI already exist | M | 3 | upgrades shipped code |
| 2 | AI Report Card | Reuses PEWS focus + publish path; high parent value | L | 1 | `AI_REPORT_CARD_SPEC.md` |
| 3 | Teacher Copilot | Biggest teacher time-saver; feeds PEWS/report | L | 1 | `AI_EXAM_ANALYSIS_SPEC.md` |
| 4 | Smart Attendance Insights | Mostly deterministic; quick win; feeds PEWS | M | 1 (cache) | — |
| 5 | AI Fee Reminder | Clear admin ROI; batch-friendly | M | 0 | `AI_FEE_REMINDER_SPEC.md` |
| 6 | NL Query | Leverages existing analytics; guarded | M | 1 (log) | `AI_NL_QUERY_SPEC.md` |
| 7 | AI Tutor | Needs student surface; biggest moat | L | 4 | — |
| 8 | Wellness Early-Warning | PEWS sibling; privacy-heavy | L | 4 | `STUDENT_WELLNESS_SPEC.md` |
| 9 | One-Tap Capture | STT/vision; teacher delight | M | 0–1 | — |
| 10 | Multilingual Engine | Cross-cutting; cultural moat | M | 1 (cache) | — |

## C.2 Top risks & mitigations
| Risk | Mitigation |
|---|---|
| Free-tier model deleted / rate-cut mid-run (happened to Cerebras/Groq in 2026) | **Dual-home every lane** (same open weights on two providers); circuit breaker + OpenRouter failover; weekly diff of each provider's live `/models` and alert on change |
| PII leaking to a training-opt-in provider | Per-template `pii_allowed_providers` allow-list; Mistral Experiment never gets raw PII; `GuardrailService.redactPii()` pre-flight |
| LLM fabricating a student/number | Deterministic layer owns *who/what*; LLM only explains the provided bundle; every AI field traces to a real snapshot; honesty law (LAW 6) enforced in UI |
| TPM ceiling (e.g. Groq 6,000) throttling batch | Route batch to higher-TPM lanes (Mistral ~50k, Cerebras ~30k); `ai_jobs` queue with concurrency limit; spread across providers |
| Prompt injection via user input (NL query, tutor) | Whitelisted query templates (no raw SQL exec); injection-pattern guardrail; system-prompt isolation |
| Cost surprise if we later add paid tiers | `ai_usage_log` per-school token tracking + monthly quota in `AppConfigTable`; cascade keeps cheap lane primary |

## C.3 Definition of done for the AI program's first slice (Gateway + PEWS)
1. `AiService` live with all five providers configured, dual-home routing, circuit breaker, usage log, L1 cache.
2. PEWS deterministic recompute + AI reasoning + intervention loop shipping for Admin & Teacher; parent nudge behind a flag.
3. No fabricated data anywhere; every AI output traceable; all endpoints correctly scoped.
4. Admin AI-usage screen shows per-school token usage and provider health.

---

## Appendix — Provider quick-reference (re-verify at build time)

| Provider | OpenAI-compatible base URL (verify) | Free limit (June 2026) | Use as |
|---|---|---|---|
| Cerebras | `https://api.cerebras.ai/v1` | ~1M tok/day, ~30k TPM | fast/interactive primary |
| Groq | `https://api.groq.com/openai/v1` | ~1,000 RPD/model, ~6k TPM small; Whisper STT | classify + STT; safest catalog |
| SambaNova | `https://api.sambanova.ai/v1` | free tier tokens/day; 405B/DeepSeek | reasoning/frontier tier |
| Mistral | `https://api.mistral.ai/v1` | ~1B tok/month (⚠️ training opt-in) | non-PII batch volume |
| OpenRouter | `https://openrouter.ai/api/v1` | 50 RPD (1,000 w/ $10 top-up), 20+ `:free` | failover + A/B + overflow |

> **All numbers re-verify against each provider's live `/models` + rate-limit headers with our real keys at implementation time.** Pin by capability, keep cross-provider fallback for every lane.
