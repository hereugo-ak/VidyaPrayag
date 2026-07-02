# AI Features Cost Sheet

> **Last updated:** 2026-06-12
> All providers are on **free tiers** — actual cost is $0. Cost columns are informational for future paid-tier planning.

## AI Gateway Overview

The platform uses a multi-provider, dual-homed AI gateway with automatic failover. All providers are free-tier, OpenAI-compatible endpoints. Provider selection is governed by capability lanes (FAST_CHAT, CLASSIFY, REASON, BATCH) with PII-aware routing.

| Lane | Primary Providers | Use Case |
|------|------------------|----------|
| FAST_CHAT | Groq Fast (8B), Cerebras | Real-time chat, quick classification |
| CLASSIFY | Groq Fast, Groq (70B) | Intent classification, triage |
| REASON | Groq (70B), Gemini, SambaNova | Multi-step reasoning, agent loops |
| BATCH | Groq (70B), Gemini, Mistral | Batch generation (report cards) |

## Provider Free-Tier Limits

| Provider | Model | RPM | RPD | TPM/TPD | Context | PII-Safe | Cost |
|----------|-------|-----|-----|---------|---------|----------|------|
| Cerebras | gpt-oss-120b | 5 | — | 1M TPD | 8K | Yes | $0 |
| Groq | openai/gpt-oss-120b | 30 | 14,400 | 12K TPM | 32K | Yes | $0 |
| Groq Fast | openai/gpt-oss-20b | 14,400 | — | 500K TPM | 8K | Yes | $0 |
| SambaNova | DeepSeek-V3.1 | 20 | 20 | 200K TPD | 64K | No | $0 |
| Mistral | mistral-small-latest | ~1 RPS | — | ~1B TPM | 32K | No | $0 |
| OpenRouter | llama-3.3-70b-instruct:free | 20 | 50 (1K w/$10) | — | 32K | Yes | $0 |
| Gemini | gemini-2.5-flash | 15 | 1,500 | 1M TPM | 1M | No | $0 |

## Feature-by-Feature Cost Breakdown

### 1. PEWS — Parent Draft Message Generation

| Field | Value |
|-------|-------|
| **AI Driver** | AiService.complete (REASON lane) |
| **Provider Chain** | Groq 70B → Gemini → SambaNova → Cerebras → OpenRouter → Mistral |
| **Model** | openai/gpt-oss-120b (primary) |
| **Free Tier** | 30 RPM, 14,400 RPD, 12K TPM |
| **Cost per call** | $0 (free tier) |
| **Tokens per call** | ~500 input, ~300 output |
| **Expected cost/month** | $0 |
| **User capacity (free tier)** | ~14,400 draft generations/day across all schools |
| **Fallback** | Deterministic templates (no AI) if all providers fail |
| **Link** | `server/.../feature/pews/act/ParentDraftService.kt` |

### 2. PEWS — Caseworker Agent (CaseFile + Intervention Planning)

| Field | Value |
|-------|-------|
| **AI Driver** | AiService.runAgent (REASON lane, tool-calling) |
| **Provider Chain** | Groq 70B → Gemini → SambaNova → Cerebras → OpenRouter |
| **Model** | openai/gpt-oss-120b (primary) |
| **Free Tier** | 30 RPM, 14,400 RPD |
| **Cost per call** | $0 (free tier) |
| **Tokens per call** | ~1,500 input, ~800 output (multi-step agent) |
| **Expected cost/month** | $0 |
| **User capacity** | ~14,400 agent runs/day |
| **Fallback** | Rule-based intervention plan (no AI) |
| **Link** | `server/.../feature/pews/caseworker/CaseworkerService.kt` |

### 3. PEWS — AI Narrative (Risk Explanation)

| Field | Value |
|-------|-------|
| **AI Driver** | AiService.complete (REASON lane) |
| **Provider Chain** | Groq 70B → Gemini → SambaNova → Cerebras → OpenRouter |
| **Model** | openai/gpt-oss-120b (primary) |
| **Free Tier** | 30 RPM, 14,400 RPD |
| **Cost per call** | $0 |
| **Tokens per call** | ~400 input, ~200 output |
| **Expected cost/month** | $0 |
| **User capacity** | ~14,400 narratives/day |
| **Fallback** | Deterministic risk label (no AI) |
| **Link** | `server/.../feature/pews/PewsRouting.kt` |

### 4. AI Tutor — Triage (Intent Classification)

| Field | Value |
|-------|-------|
| **AI Driver** | AiService.complete (CLASSIFY lane) |
| **Provider Chain** | Groq Fast (8B) → Groq (70B) → Cerebras → OpenRouter |
| **Model** | openai/gpt-oss-20b (primary) |
| **Free Tier** | 14,400 RPM, 500K TPM |
| **Cost per call** | $0 |
| **Tokens per call** | ~200 input, ~100 output |
| **Expected cost/month** | $0 |
| **User capacity** | ~500K tokens/min — effectively unlimited for chat |
| **Fallback** | Skip triage, go directly to agent |
| **Link** | `server/.../feature/tutor/triage/TutorTriageService.kt` |

### 5. AI Tutor — Agent (Doubt Resolution)

| Field | Value |
|-------|-------|
| **AI Driver** | AiService.runAgent (REASON lane, tool-calling) |
| **Provider Chain** | Groq 70B → Gemini → SambaNova → Cerebras → OpenRouter |
| **Model** | openai/gpt-oss-120b (primary) |
| **Free Tier** | 30 RPM, 14,400 RPD |
| **Cost per call** | $0 |
| **Tokens per call** | ~1,200 input, ~600 output (multi-step) |
| **Expected cost/month** | $0 |
| **User capacity** | ~14,400 doubt resolutions/day |
| **Fallback** | Deterministic Socratic step (no AI) |
| **Link** | `server/.../feature/tutor/agent/TutorAgentService.kt` |

### 6. AI Tutor — Practice Grading

| Field | Value |
|-------|-------|
| **AI Driver** | AiService.complete (CLASSIFY lane) |
| **Provider Chain** | Groq Fast → Groq → Cerebras → OpenRouter |
| **Model** | openai/gpt-oss-20b (primary) |
| **Free Tier** | 14,400 RPM, 500K TPM |
| **Cost per call** | $0 |
| **Tokens per call** | ~300 input, ~150 output |
| **Expected cost/month** | $0 |
| **User capacity** | Effectively unlimited |
| **Fallback** | Keyword-based grading (no AI) |
| **Link** | `server/.../feature/tutor/practice/PracticeService.kt` |

### 7. AI Report Card — Rollup (Fact Bundle)

| Field | Value |
|-------|-------|
| **AI Driver** | Deterministic (no LLM) |
| **Provider Chain** | N/A — pure database aggregation |
| **Cost per call** | $0 |
| **Tokens per call** | 0 |
| **Expected cost/month** | $0 |
| **User capacity** | Unlimited |
| **Link** | `server/.../feature/reportcard/rollup/ReportRollupService.kt` |

### 8. AI Report Card — Narrator (Report Generation)

| Field | Value |
|-------|-------|
| **AI Driver** | AiService.complete (BATCH lane) |
| **Provider Chain** | Groq 70B → Gemini → Mistral → OpenRouter |
| **Model** | openai/gpt-oss-120b (primary) |
| **Free Tier** | 30 RPM, 14,400 RPD (Groq); 1,500 RPD (Gemini) |
| **Cost per call** | $0 |
| **Tokens per call** | ~800 input, ~500 output per student |
| **Expected cost/month** | $0 |
| **User capacity** | ~14,400 reports/day (batch, async job queue) |
| **Fallback** | Deterministic template-based report (no AI) |
| **Link** | `server/.../feature/reportcard/narrator/NarratorService.kt` |

### 9. AI Report Card — Class Context (Tier 1)

| Field | Value |
|-------|-------|
| **AI Driver** | AiService.complete (BATCH lane) |
| **Provider Chain** | Groq 70B → Gemini → Mistral → OpenRouter |
| **Model** | openai/gpt-oss-120b (primary) |
| **Free Tier** | 30 RPM, 14,400 RPD |
| **Cost per call** | $0 |
| **Tokens per call** | ~600 input, ~300 output per class |
| **Expected cost/month** | $0 |
| **User capacity** | ~14,400 class contexts/day |
| **Fallback** | Skip class context (narrator runs without it) |
| **Link** | `server/.../feature/reportcard/assemble/ReportAssemblyService.kt` |

### 10. AI Report Card — Flywheel (Effectiveness Learning)

| Field | Value |
|-------|-------|
| **AI Driver** | AiService.complete (BATCH lane) |
| **Provider Chain** | Groq 70B → Gemini → Mistral → OpenRouter |
| **Model** | openai/gpt-oss-120b (primary) |
| **Free Tier** | 30 RPM, 14,400 RPD |
| **Cost per call** | $0 |
| **Tokens per call** | ~500 input, ~300 output |
| **Expected cost/month** | $0 |
| **User capacity** | ~14,400 flywheel analyses/day |
| **Fallback** | Skip effectiveness analysis |
| **Link** | `server/.../feature/reportcard/learn/LearnRouting.kt` |

## Summary

| Feature | AI Driver | Lane | Cost/Call | Free Tier Capacity | Monthly Cost |
|---------|-----------|------|-----------|-------------------|--------------|
| PEWS Parent Draft | LLM complete | REASON | $0 | 14,400/day | $0 |
| PEWS Caseworker Agent | LLM agent | REASON | $0 | 14,400/day | $0 |
| PEWS AI Narrative | LLM complete | REASON | $0 | 14,400/day | $0 |
| Tutor Triage | LLM complete | CLASSIFY | $0 | 500K TPM | $0 |
| Tutor Agent | LLM agent | REASON | $0 | 14,400/day | $0 |
| Tutor Practice Grading | LLM complete | CLASSIFY | $0 | 500K TPM | $0 |
| Report Card Rollup | Deterministic | N/A | $0 | Unlimited | $0 |
| Report Card Narrator | LLM complete | BATCH | $0 | 14,400/day | $0 |
| Report Card Class Context | LLM complete | BATCH | $0 | 14,400/day | $0 |
| Report Card Flywheel | LLM complete | BATCH | $0 | 14,400/day | $0 |

**Total monthly AI cost: $0** (all on free tiers with deterministic fallbacks)

### Scaling Notes

- **Bottleneck:** Groq 70B at 30 RPM / 14,400 RPD is the primary constraint for REASON/BATCH lanes
- **Mitigation (4 layers):**
  1. **Proactive RateLimiter** (`RateLimiter.kt`) — per-(provider, model) sliding-window RPM + daily RPD + sliding-window TPM tracking at 90% capacity (10% reserve). Prevents 429s before they happen by skipping providers that are near their limits, instead of burning requests that are doomed to fail.
  2. **Circuit Breaker** (`CircuitBreaker.kt`) — per-(provider, model) 3-state machine (CLOSED → OPEN → HALF_OPEN). After 5 consecutive failures, circuit opens for 30s cooldown. Rate-limit (429) hits tracked separately. Persists to `ai_provider_health` table.
  3. **Provider Jitter + Failover** (`AiService.kt`) — shuffles candidates with CLOSED circuits to spread load. On failure, fails over to next provider with 200-800ms random backoff.
  4. **L1 Response Cache** — 24h TTL (env-tunable). Absorbs duplicate prompts before they ever hit a provider.
- **Priority Queue:** BATCH lane (report cards) gets a 50ms yield before each provider attempt, letting real-time requests (FAST_CHAT/CLASSIFY/REASON — tutor chats, PEWS drafts) go first. Prevents a 40-student report card batch from starving a student's tutor doubt.
- **Admin Visibility:** `GET /api/v1/admin/ai/rate-limits` shows live RPM/RPD/TPM usage vs limits per provider. `GET /api/v1/admin/ai/health` shows circuit-breaker state.
- **If free tiers are exhausted:** All features have deterministic fallbacks — the platform degrades gracefully without AI
- **Paid tier estimate:** If moving to Groq paid ($0.59/1M output tokens), ~1M output tokens/month would cost ~$0.59/month

## Daily Sustainable User Capacity (with 10% Token Reserve)

The following analysis calculates how many concurrent users the platform can sustain daily on free tiers, holding back 10% of the token/request budget as a safety reserve for retries, caching misses, and burst traffic.

### Methodology

- **Reserve:** 10% of each provider's daily request/token budget is held back
- **Effective capacity** = Total capacity × 0.90
- **Per-user consumption** = average AI calls per active user per day × tokens per call
- **Sustainable users** = Effective capacity ÷ Per-user consumption
- **Bottleneck lane** = the lane that limits each feature (features sharing a lane compete for the same budget)

### Provider Effective Daily Capacity (after 10% reserve)

| Provider | Raw RPD | Reserve (10%) | Effective RPD | Raw TPM/TPD | Effective TPM/TPD |
|----------|---------|---------------|---------------|-------------|---------------------|
| Groq 70B | 14,400 | 1,440 | **12,960** | 12K TPM | **10,800 TPM** |
| Groq Fast (8B) | Unlimited RPM* | — | **Unlimited** | 500K TPM | **450K TPM** |
| Gemini | 1,500 | 150 | **1,350** | 1M TPM | **900K TPM** |
| SambaNova | 20 | 2 | **18** | 200K TPD | **180K TPD** |
| Cerebras | 5 RPM | — | **~260/day** | 1M TPD | **900K TPD** |
| OpenRouter | 50 | 5 | **45** | — | — |
| Mistral | ~1 RPS | — | **~77,760** | ~1B TPM | **~900M TPM** |

*Groq Fast RPM is 14,400/min — effectively unlimited for our scale.

### Lane-Level Sustainable Capacity (after 10% reserve)

Multiple features share each lane. The sustainable user count is determined by the lane's total effective capacity divided by the sum of per-user consumption across all features in that lane.

#### REASON Lane (Groq 70B primary — 12,960 effective RPD)

| Feature | Calls/User/Day | Tokens/Call | Daily Tokens/User |
|---------|---------------|-------------|-------------------|
| PEWS Parent Draft | 0.5 | 800 | 400 |
| PEWS Caseworker Agent | 0.2 | 2,300 | 460 |
| PEWS AI Narrative | 0.3 | 600 | 180 |
| Tutor Agent (Doubt) | 2.0 | 1,800 | 3,600 |
| **Total per user/day** | | | **4,640 tokens** |

| Metric | Value |
|--------|-------|
| Effective daily requests | 12,960 |
| Avg requests/user/day | 3.0 |
| **Sustainable concurrent users (REASON)** | **~4,320 users** |
| Effective daily tokens | 10,800 TPM × 1,440 min = 15,552,000 |
| Daily tokens/user | 4,640 |
| **Token-limited users (REASON)** | **~3,352 users** |
| **REASON lane bottleneck** | **~3,350 concurrent active users/day** |

#### CLASSIFY Lane (Groq Fast 8B — 450K effective TPM)

| Feature | Calls/User/Day | Tokens/Call | Daily Tokens/User |
|---------|---------------|-------------|-------------------|
| Tutor Triage | 5.0 | 300 | 1,500 |
| Tutor Practice Grading | 3.0 | 450 | 1,350 |
| **Total per user/day** | | | **2,850 tokens** |

| Metric | Value |
|--------|-------|
| Effective TPM | 450,000 |
| Peak hour tokens (assume 20% daily load) | 13,680 per user |
| **Sustainable concurrent users (CLASSIFY)** | **~32 users/min peak** (~15,500/day at 8% peak concurrency) |
| **CLASSIFY lane bottleneck** | **~15,500 users/day** (not a real constraint) |

#### BATCH Lane (Groq 70B + Gemini fallback — 12,960 + 1,350 = 14,310 effective RPD)

| Feature | Calls/User/Day | Tokens/Call | Daily Tokens/User |
|---------|---------------|-------------|-------------------|
| Report Card Narrator | 0.1 (1 report/10 days) | 1,300 | 130 |
| Report Card Class Context | 0.02 (1 per class/50 days) | 900 | 18 |
| Report Card Flywheel | 0.01 (1 per term) | 800 | 8 |
| **Total per user/day** | | | **156 tokens** |

| Metric | Value |
|--------|-------|
| Effective daily requests | 14,310 |
| Avg requests/user/day | 0.13 |
| **Sustainable concurrent users (BATCH)** | **~110,076 users** |
| **BATCH lane bottleneck** | **~110,000 users/day** (not a real constraint) |

### Overall Platform Sustainable Capacity

| Constraint | Sustainable Users/Day |
|------------|----------------------|
| REASON lane (Groq 70B) | **~3,350** |
| CLASSIFY lane (Groq Fast) | ~15,500 |
| BATCH lane (Groq 70B + Gemini) | ~110,000 |
| **Platform bottleneck** | **~3,350 concurrent active users/day** |

### What "Active User" Means

An "active user" is a student or teacher who:
- Asks 2 AI tutor doubts per day (REASON lane)
- Generates 0.5 PEWS parent drafts per day (averaged across teachers)
- Triggers 0.3 PEWS AI narratives per day (averaged across admin runs)
- Triggers 0.2 PEWS caseworker agent runs per day (averaged across interventions)
- Practices 3 problems with AI grading per day (CLASSIFY lane)
- Goes through 5 triage classifications per day (CLASSIFY lane)
- Generates 1 report card per 10 days (BATCH lane, amortized)

### Scaling Path

| Users/Day | Strategy | Monthly Cost |
|-----------|----------|--------------|
| 0 – 3,350 | Free tiers only (current) | $0 |
| 3,350 – 10,000 | Add Groq paid tier ($0.59/1M tokens) | ~$5 – $15 |
| 10,000 – 50,000 | Groq paid + Gemini paid ($1.25/1M tokens) | ~$25 – $125 |
| 50,000+ | Multi-provider paid + on-prem fallback | ~$150+ |

### Per-School Capacity Estimate

Assuming a school has ~500 students and ~30 teachers (530 active users):

| Schools Supported | Total Users | Lane Stress | Feasible on Free Tier? |
|-------------------|-------------|-------------|------------------------|
| 1 | 530 | 16% of REASON budget | ✅ Yes |
| 3 | 1,590 | 47% of REASON budget | ✅ Yes |
| 6 | 3,180 | 95% of REASON budget | ⚠️ Near limit |
| 7+ | 3,710+ | >110% of REASON budget | ❌ Needs paid tier |
