# PEWS + AI Gateway — Implementation Plan & Build Approach

> **Branch:** `aifeatures`
> **Companion to:** `AI_FEATURES_PLAN.md` (the strategy). **This doc is the build plan** — grounded in the *actual* code on this branch, with verified Supabase columns, the cross-role flow, the key-management design, and the exact Render env keys.
> **Date:** 2026-06-28
> **Scope of slice 1:** AI Gateway (foundation) + PEWS (feature #1), Admin + Teacher surfaces working end-to-end, Parent nudge behind a flag.

---

## 0. What I verified in the codebase (so this plan stands on real ground)

| Claim in `AI_FEATURES_PLAN.md` | Verified? | Where |
|---|---|---|
| Rule-based PEWS already ships | ✅ | `SchoolIntelligenceRouting.kt` → `early_warning` block (lines 252–343), thresholds `ATT_RISK=75 / MARKS_RISK=40 / LEAVE_RISK=3` |
| `EarlyWarningStudentDto` + `signals[]` + `risk_level` shape exists | ✅ | `SchoolIntelligenceRouting.kt` lines 102–120 |
| PEWS UI teaser exists & must stay honest | ✅ | `PewsPreview.kt` (RA-S10 / LAW 6 — no fabricated students) |
| Server uses module-singleton DI, NOT Koin | ✅ | `NotificationRouting.kt`, `PulseWeeklyJob` (`private val pulseService = ParentPulseService()`) |
| `AUTO_CREATE_TABLES=OFF` in prod; migrations are manual; `validateSchema()` gates boot | ✅ | `DatabaseFactory.kt` lines 263–289, 348–379 |
| Ktor **client** deps already present | ✅ | `server/build.gradle.kts` lines 107–111 (`ktor-client-core/cio/content-negotiation/logging/auth` 3.4.3) |
| Scheduled-job pattern exists (hourly tick + run-guard) | ✅ | `PulseWeeklyJob.kt`, `NotificationScheduler` — both `.start(CoroutineScope(Dispatchers.Default))` in `Application.kt` (lines 167, 170) |
| Notification spine reusable via `Notify.toUser(...)` | ✅ | `feature/notifications/Notify.kt`, used by `PulseWeeklyJob` |
| Env read convention is `System.getenv` (+ local.properties fallback) | ✅ | `OtpService.env()`, `DatabaseFactory.resolve()` |
| HTTP-client-per-provider pattern already used | ✅ | `OtpHttpClient`, `SupabaseStorage` (`HttpClient(CIO){...}`) |
| `AI_INFRASTRUCTURE_SPEC.md` exists (Gemini+Ollama) | ✅ | `newreviewdocs/specs/AI_INFRASTRUCTURE_SPEC.md` — **provider strategy superseded**, gateway architecture kept |

**Conclusion:** the plan is accurate. We are upgrading a real, shipping surface — not greenfield.

---

## 1. Supabase column audit — do we already have what PEWS needs?

PEWS's **deterministic Sense layer** reads only existing tables. Here is the column-by-column verification against `server/.../db/Tables.kt` (the Exposed mappings, which are kept in lock-step with the real Supabase schema).

### 1.1 INPUT tables — ✅ ALL COLUMNS PRESENT (no schema change needed to compute risk)

| Signal | Table | Columns used | Status |
|---|---|---|---|
| Attendance % + trajectory | `attendance_records` | `school_id`, `date` (typed `DATE`), `type` (`student`), `status` (present/absent/late/leave), `student_id`, `person_id` (legacy) | ✅ present (`Tables.kt` 493–533) |
| Marks % + trajectory | `assessment_marks` | `assessment_id`, `student_id` (legacy code), `student_ref` (typed FK), `marks` (nullable Double), `is_absent` | ✅ present (969–1000) |
| Max marks (for %) | `assessments` | `id`, `school_id`, `max_marks`, `subject`, `name`, `exam_date`, `is_published`, `class_id`, `section` | ✅ present (918–962) |
| Leave spikes | `leave_requests` | `school_id`, `requester_role` (`student`), `requester_name`, `child_id`, `created_at` | ✅ present (671–696) |
| Identity / cohort scoping | `students` | `student_code` (unique), `full_name`, `class_name`, `section`, `is_active`, `school_id`, `parent_phone` | ✅ present (538–552) |
| Class-mean baselines (z-score) | `enrollments` | `student_id`, `class_id`, `section`, `status` | ✅ present (577–593) |
| HW non-submission (optional) | `homework_submissions` | exists | ✅ present (1143) |
| Fee-stress proxy (optional) | `fee_records` | `status` (PAID/DUE/OVERDUE), `child_id`, `due_date` | ✅ present (641–656) |
| Delivery spine | `notifications` | `school_id`, `user_id`, `category`, `title`, `body`, `deep_link`, `ref_type`, `ref_id`, `idempotency_key` | ✅ present (1298–1324) |

> **Finding:** The entire **Sense** stage can run **today** with zero schema changes. The z-score/trajectory upgrade is pure SQL/Kotlin math over columns that already exist.

### 1.2 Known data-quality caveats to handle in code (already visible in the shipped logic)

1. **Leave is matched by `requester_name` (lowercased), not FK** — see `SchoolIntelligenceRouting.kt` line 296–303. Legacy student-leave rows have no `child_id`. PEWS will prefer `child_id`/`student_code` join when present and **fall back** to name-match (documented, not silently wrong).
2. **`assessment_marks.student_id` is the student *code* (legacy text)**, `student_ref` is the typed FK and may be null on old rows. PEWS keys on `student_code` (matches existing early_warning) and uses `student_ref` when available.
3. **`attendance_records.person_id` is nullable**; rows without it can't be attributed — skip them (existing code already does, line 272).
4. **Marks % only over published assessments** for parent-facing surfaces (`is_published=true`); admin/teacher Sense can include unpublished for earliness (config flag).

These are **handled**, not blockers.

### 1.3 NEW tables required (the agent's *memory* + the AI gateway)

Nothing in §1.1 stores **history, narratives, interventions, or AI usage** — so these are genuinely new. Two migration files, applied in Supabase **before** the matching deploy (because `AUTO_CREATE_TABLES=OFF`):

**`docs/db/migration_060_ai_gateway.sql`** — 6 gateway tables (from `AI_INFRASTRUCTURE_SPEC.md` §6, unchanged shape):
`ai_provider_config`, `ai_prompt_templates`, `ai_usage_log`, `ai_response_cache`, `ai_jobs`, `ai_provider_health`.

**`docs/db/migration_061_pews.sql`** — 3 PEWS tables (from `AI_FEATURES_PLAN.md` §A.3):
`pews_risk_snapshots`, `pews_interventions`, `pews_config`.

Each table gets its Exposed mapping in `Tables.kt` and is registered in `DatabaseFactory.allTables` (so `validateSchema()` can verify it). **Run order appended to the existing chain:** `…53 → 60 → 61`.

> **Migration discipline (hard rule, already enforced in this repo):** column names/types in the `.sql` MUST match the Exposed `object` exactly, and both land in the **same commit**. `CREATE TABLE IF NOT EXISTS` + `BEGIN/COMMIT` (copy the `migration_051_parent_pulse.sql` template).

---

## 2. The approach — build order for slice 1

```
Phase 0  Gateway tables + Exposed mappings + register in allTables       (migration_060)
Phase 1  KeyVault + EncryptionService + LlmClient (one OpenAI-compatible) (key mgmt — §4)
Phase 2  AiService choke point: route → cache(L1) → circuit breaker →
         dual-home failover → guardrails(PII) → usage log                (no Ollama)
Phase 3  PEWS tables + Exposed mappings                                   (migration_061)
Phase 4  Sense: PewsSnapshotService (z-score + slopes) + PewsDailyJob     (scheduled, hourly tick)
Phase 5  Reason: AiService.complete(feature="pews", REASON, containsPii)  (narrative/cause/reco)
Phase 6  Act: PewsInterventionService + Notify.toUser(category="pews")
Phase 7  API: school/teacher/parent routes (PewsRouting.kt)              (§A.6 contract)
Phase 8  UI: PewsCohortScreen / PewsStudentDetailScreen / teacher list   (VStateHost 3-state)
Phase 9  Learn: outcome tracking on intervention close
Phase 10 Admin AI-usage screen (per-school tokens + provider health)
```

**Each phase is independently shippable and committed.** The deterministic Sense layer (Phase 4) produces a working, honest cohort even if the LLM (Phase 5) is disabled — so PEWS degrades gracefully to "today's shipped behaviour + history + interventions."

---

## 3. Cross-role communication flow (Admin ⇄ Teacher ⇄ Parent)

This is the heart of "how the feature is communicated between the three roles." Every arrow reuses existing infra.

```
                          ┌──────────────────────────────────────┐
                          │  SENSE (PewsDailyJob, 06:00 IST)      │
                          │  per school → pews_risk_snapshots      │
                          └───────────────┬──────────────────────┘
                                          │ newly high/medium
                          ┌───────────────▼──────────────────────┐
                          │  REASON (AiService, PII-safe lane)     │
                          │  narrative + cause + recommendation    │
                          │  written back onto the snapshot row    │
                          └───────────────┬──────────────────────┘
                                          │ ACT
        ┌─────────────────────────────────┼─────────────────────────────────┐
        ▼                                 ▼                                   ▼
  ┌───────────┐                    ┌─────────────┐                    ┌─────────────┐
  │  ADMIN    │  owns school-wide  │  TEACHER    │  owns per-student  │  PARENT     │
  │           │  patterns          │             │  interventions     │  (opt-in)   │
  ├───────────┤                    ├─────────────┤                    ├─────────────┤
  │ • Risk    │  auto-assigns ───► │ • My at-risk│  one-tap draft ──► │ • Gentle    │
  │   radar   │  intervention to   │   students  │  parent message    │   nudge     │
  │ • Cohort  │  class teacher     │ • Per-stu   │  (review-before-   │   (no "risk"│
  │   + AI    │                    │   card + AI │   send via         │    word, no │
  │   fields  │ ◄─── effectiveness │ • My        │   MessagesRouting) │    scores)  │
  │ • Patterns│      rolls up      │   interven- │                    │ • [View     │
  │ • Config  │      from teacher  │   tions     │ ──► Notify.toUser   │   attend.]  │
  │   (thresh,│      outcomes      │   (open/    │     (category=pews) │   [Message  │
  │   parent  │                    │   done)     │     to PARENT       │    teacher] │
  │   share)  │                    │             │                    │             │
  └───────────┘                    └─────────────┘                    └─────────────┘
        ▲                                 │                                   │
        └─────────── Notify.toUser(category="pews") to ADMIN/TEACHER ◄────────┘
                     (FCM + in-app bell, existing spine)              owner marks done
                                          │
                          ┌───────────────▼──────────────────────┐
                          │  LEARN: outcome (improved/unchanged/   │
                          │  worsened) → effectiveness view        │
                          └────────────────────────────────────────┘
```

### Who sees what (scoping rules, enforced from JWT — never request body)

| Role | Scope guard (existing) | Sees | Writes |
|---|---|---|---|
| **School Admin** | `requireSchoolAdmin()` | whole-school cohort, AI fields, school/class patterns, effectiveness, config | tune `pews_config`, reassign owners, manual `/run` |
| **Teacher** | `requireTeacherContext()` + `requireOwnedAssignment()` | **only own class/section** at-risk students + AI explanation | open/complete interventions, **draft** parent message (review-before-send) |
| **Parent** | child-scoped (`parent_child_links` / `children.student_code`) | **own child only**, gentle non-clinical nudge — **only if `pews_config.parent_share_enabled`** | nothing (read-only) + tap actions to existing screens |

### Communication channels (all already in the codebase)
- **Admin/Teacher alerts** → `Notify.toUser(category="pews", deepLink=…)` → FCM + in-app bell (same path `PulseWeeklyJob` uses).
- **Teacher → Parent message** → existing `MessagesRouting` compose path; AI only *drafts*, teacher taps send. **Never auto-sent.**
- **Parent nudge** → `Notify.toUser(category="pews", deepLink="/parent/...")` with actionable buttons (Smart-Notifications card pattern).

> **Honesty law (LAW 6) upheld end-to-end:** deterministic SQL owns *who/what*; the LLM only explains the provided signal bundle; every AI field on a snapshot traces to real rows; empty data → honest empty state (`VStateHost`), never a placeholder. `PewsPreview.kt` stays the label-free teaser; real numbers live one tap away.

---

## 4. Robust API key management (the part you specifically asked for)

You have **5 free-tier keys** (Cerebras, Groq, SambaNova, Mistral, OpenRouter) and want to drop them on Render. Here is the design that makes that **robust, rotatable, and safe** — not just five `getenv` calls scattered around.

### 4.1 Two-layer key model

```
Layer 1 — BOOTSTRAP (env, on Render)         Layer 2 — RUNTIME (DB, encrypted)
─────────────────────────────────────        ──────────────────────────────────────
AI_<PROVIDER>_API_KEY  (raw key)        ───►  ai_provider_config.api_key_encrypted
AI_ENCRYPTION_KEY      (32-byte hex)           (AES-256-GCM, IV-prefixed)
AI_<PROVIDER>_BASE_URL (optional override)     KeyVault decrypts on use, caches in-mem
```

- **On boot**, a `KeyVault` reads each `AI_<PROVIDER>_API_KEY` from env, and **seeds/refreshes** the matching `ai_provider_config` row (encrypting the key with `AI_ENCRYPTION_KEY` via `EncryptionService` — `AES/GCM/NoPadding`, random 12-byte IV, JVM-native `javax.crypto`, no new dependency).
- **At runtime**, `AiService` never reads env directly — it asks `KeyVault.keyFor(provider)`, which decrypts from DB (or serves an in-memory cache). This means:
  - **Rotation without redeploy:** update the row (admin API) → cache invalidates → new key live. (Env stays as the bootstrap source of truth; DB is the hot path.)
  - **Keys never logged, never in API responses** (masked `sk-****…****`), never in the prompt.

### 4.2 `KeyVault` contract (module singleton, no Koin)

```kotlin
object KeyVault {
    // env → encrypt → upsert ai_provider_config (idempotent), called once at boot
    suspend fun bootstrapFromEnv()
    // decrypt-on-demand with in-memory cache; null if provider not configured
    suspend fun keyFor(provider: AiProvider): String?
    fun baseUrlFor(provider: AiProvider): String          // env override or baked default
    fun isConfigured(provider: AiProvider): Boolean        // for health + graceful skip
    fun invalidate(provider: AiProvider)                   // on rotation
}
```

### 4.3 Provider lanes (the routing table the keys feed)

`taskClass` + `containsPii` pick the lane; each lane is **dual-homed** so one dead key never kills a feature (matches `AI_FEATURES_PLAN.md` §3.2):

| Lane | Primary | Fallback 1 | Fallback 2 | PII-safe? |
|---|---|---|---|---|
| `FAST_CHAT` | Cerebras | Groq | OpenRouter | ✅ |
| `CLASSIFY` | Groq | Cerebras | OpenRouter | ✅ |
| `REASON` | SambaNova | Mistral | OpenRouter | ⚠️ SambaNova/Mistral only for **non-PII / redacted**; PII REASON → Cerebras→Groq→OpenRouter |
| `BATCH` | Mistral | Groq | Cerebras | ⚠️ never raw PII to Mistral |
| `STT` | Groq (whisper) | — (degrade) | — | ✅ |

**Privacy routing is enforced at the gateway** by a per-template `pii_allowed_providers` allow-list. **For PEWS** (`containsPii=true`), REASON is pinned to **Cerebras → Groq → OpenRouter** (no-training providers); SambaNova/Mistral are only used for the **PII-redacted school-wide pattern** prompts.

### 4.4 Graceful degradation
If a provider's key is missing/empty, `KeyVault.isConfigured()` returns false → that provider is **skipped** in the lane (no crash). If **all** providers in a lane are down/unconfigured, PEWS still shows the **deterministic** cohort (narrative simply stays `null` → UI shows signals without the AI paragraph). The product never hard-fails on AI.

---

## 5. The exact Render environment variables (copy-paste list)

Set these in **Render → your service → Environment**. The 5 `*_API_KEY` values are *your* free-tier keys (paste each one). `AI_ENCRYPTION_KEY` you generate once (see below). The `*_BASE_URL` and tuning vars are optional (defaults are baked in) — listed so you can override without a code change.

### 5.1 REQUIRED — one key per provider (paste your real keys)

```bash
# ── Provider API keys (your free-tier keys) ────────────────────────────────
AI_CEREBRAS_API_KEY=          # csk-...    (https://cloud.cerebras.ai)
AI_GROQ_API_KEY=              # gsk_...    (https://console.groq.com/keys)
AI_SAMBANOVA_API_KEY=         #            (https://cloud.sambanova.ai/apis)
AI_MISTRAL_API_KEY=           #            (https://console.mistral.ai/api-keys)
AI_OPENROUTER_API_KEY=        # sk-or-...  (https://openrouter.ai/keys)

# ── Encryption key for at-rest provider keys (REQUIRED) ────────────────────
# Generate once locally and paste the output here (32 bytes = 64 hex chars):
#   openssl rand -hex 32
AI_ENCRYPTION_KEY=            # e.g. 9f1c...64-hex-chars...e2
```

### 5.2 OPTIONAL — base URLs (only set to override the baked-in defaults)

```bash
AI_CEREBRAS_BASE_URL=https://api.cerebras.ai/v1
AI_GROQ_BASE_URL=https://api.groq.com/openai/v1
AI_SAMBANOVA_BASE_URL=https://api.sambanova.ai/v1
AI_MISTRAL_BASE_URL=https://api.mistral.ai/v1
AI_OPENROUTER_BASE_URL=https://openrouter.ai/api/v1
```

### 5.3 OPTIONAL — model pins & tuning (defaults shown; re-verify against each provider's live `/models` at build time)

```bash
# Model pins per lane (pin by capability; update without redeploy via admin API)
AI_MODEL_CEREBRAS=gpt-oss-120b
AI_MODEL_GROQ_FAST=openai/gpt-oss-20b
AI_MODEL_GROQ_REASON=openai/gpt-oss-120b
AI_MODEL_SAMBANOVA=DeepSeek-V3.1
AI_MODEL_MISTRAL=mistral-large-latest
AI_MODEL_OPENROUTER=meta-llama/llama-3.3-70b-instruct:free
AI_MODEL_STT=whisper-large-v3        # Groq

# Gateway behaviour
AI_RATE_LIMIT_PER_MIN=15             # per-school token bucket
AI_DEFAULT_CACHE_TTL_MIN=1440        # PEWS narrative cache (1 day) keyed by signal hash
AI_CIRCUIT_FAILS_TO_OPEN=5
AI_CIRCUIT_COOLDOWN_SEC=30
AI_RETRY_MAX=3
AI_BATCH_CONCURRENCY=3

# PEWS scheduling
PEWS_RUN_HOUR_UTC=0                   # 06:00 IST recompute (00:30 UTC → check hour 0)
PEWS_ENABLED=true
```

### 5.4 How to generate `AI_ENCRYPTION_KEY`

```bash
openssl rand -hex 32     # → paste the 64-char output as AI_ENCRYPTION_KEY
```

> ⚠️ **Do not commit any key.** They live only in Render env. The repo's `.env.example` will document the *names* (with empty values), matching the existing OTP-provider convention. If `AI_ENCRYPTION_KEY` ever changes, all stored encrypted keys must be re-seeded from env (KeyVault does this automatically on boot since env is the bootstrap source).

---

## 6. Acceptance criteria for slice 1 (Gateway + PEWS)

- [ ] `migration_060` + `061` applied in Supabase; `validateSchema()` boots clean.
- [ ] `KeyVault.bootstrapFromEnv()` seeds 5 encrypted provider rows; missing keys skipped gracefully.
- [ ] Deterministic recompute runs on schedule (hourly tick + run-guard, `PulseWeeklyJob` pattern) and on attendance/marks write → writes `pews_risk_snapshots` with z-score relative thresholds + trajectory slopes.
- [ ] **No fabricated students/numbers** anywhere; every AI field traces to a real snapshot row (LAW 6).
- [ ] AI narrative/cause/recommendation generated **only** from the deterministic signal bundle; PII-routed to no-training providers; cached by signal hash.
- [ ] Interventions open → own → complete → outcome tracked (Learn loop).
- [ ] Admin sees cohort + patterns + effectiveness; teacher sees own-class scoped; parent sees gentle nudge **only** when `parent_share_enabled`.
- [ ] All endpoints school-scoped from JWT; teacher endpoints assignment-scoped; parent endpoints child-scoped.
- [ ] Every LLM call writes one `ai_usage_log` row (provider_used + routing_decision).
- [ ] Admin AI-usage screen shows per-school tokens + provider health.

---

## 7. Files we will add/touch (map for the implementation PRs)

**New (server):**
- `db/Tables.kt` (+9 table objects), `db/DatabaseFactory.kt` (register in `allTables`)
- `docs/db/migration_060_ai_gateway.sql`, `docs/db/migration_061_pews.sql`
- `feature/ai/EncryptionService.kt`, `feature/ai/KeyVault.kt`, `feature/ai/LlmClient.kt` (OpenAI-compatible), `feature/ai/AiService.kt`, `feature/ai/CircuitBreaker.kt`, `feature/ai/GuardrailService.kt`, `feature/ai/AiRouting.kt` (admin usage/health endpoints)
- `feature/pews/PewsSnapshotService.kt` (Sense), `feature/pews/PewsReasoningService.kt` (Reason), `feature/pews/PewsInterventionService.kt` (Act/Learn), `feature/pews/PewsDailyJob.kt`, `feature/pews/PewsRouting.kt`

**Touched (server):**
- `Application.kt` (wire `KeyVault.bootstrapFromEnv()`, `PewsDailyJob.start(...)`, `pewsRouting()`, `aiRouting()`)
- `core/SchoolAccess.kt` (add `requirePlatformAdmin()` for provider config)
- `.env.example` (document the key names, empty values)

**New (composeApp):**
- `ui/v2/screens/school/PewsCohortScreen.kt`, `PewsStudentDetailScreen.kt`, teacher `PewsMyStudentsScreen.kt` (reuse `VStateHost` + V* tokens; `PewsPreview.kt` stays the teaser)

---

## 8. Risks & mitigations (slice-1 specific)

| Risk | Mitigation |
|---|---|
| Free-tier model deleted / rate-cut mid-run | Dual-home every lane; circuit breaker; OpenRouter failover; pin by capability not exact model |
| PII to training-opt-in provider (Mistral/Samba) | Per-template `pii_allowed_providers` allow-list; PEWS PII-REASON pinned to Cerebras/Groq/OpenRouter; `GuardrailService.redactPii()` pre-flight |
| LLM fabricates a student/number | Deterministic layer owns who/what; LLM constrained to provided bundle; every AI field traces to a snapshot; LAW 6 in UI |
| Legacy leave-by-name mismatch | Prefer `child_id`/`student_code` join, fall back to name-match (documented) |
| `AI_ENCRYPTION_KEY` rotation | Env is bootstrap source; KeyVault re-seeds on boot; rotation = redeploy or admin re-seed |
| All AI providers down | PEWS degrades to deterministic cohort (narrative `null`); never hard-fails |

---

**Next step on your go-ahead:** start Phase 0–2 (gateway tables + KeyVault + AiService) as the first committed PR, then Phase 3–7 (PEWS end-to-end Admin+Teacher). Parent nudge ships behind `parent_share_enabled` in the same slice.
