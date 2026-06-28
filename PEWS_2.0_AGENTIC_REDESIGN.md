# PEWS 2.0 — The Agentic Student-Success Operating System

> **Branch:** `aifeatures`
> **Date:** 2026-06-28
> **Author:** AI Engineering — deep redesign brief
> **Status:** Vision + grounded build plan. Nothing here is faked; every claim is checked against the code on this branch.
> **Companion docs:**
> - `AI_FEATURES_PLAN.md` — the AI program strategy
> - `PEWS_AI_GATEWAY_IMPLEMENTATION_PLAN.md` — the gateway/key build plan
> - `PEWS_STATUS_AND_GAP_PLAN.md` — the honest current-state audit
> - **This doc supersedes the *ambition ceiling* of all three.** They describe a correct v1. This describes the v2 that is actually worth a ₹100Cr+ valuation.

---

## 0. Why this document exists (read this first)

We shipped a real PEWS v1. The backend log proves it runs end-to-end:

```
PewsSnapshotService - PEWS recompute: school=… runDate=2026-06-28 → 2 at-risk snapshots
PewsReasoningService - PEWS reasoning: school=… narrated 2/2 snapshots
PewsInterventionService - PEWS act: school=… opened 1 interventions
200 OK: POST /api/v1/school/pews/run in 23529ms
```

That is a working **Sense → Reason → Act → Learn** loop. It is honest, it degrades gracefully, and it is wired across all three portals. **And it is still a generic, shallow version of what this should be.** The same log also shows the three problems that cap its value today:

1. **`429 RATE_LIMITED` on the only REASON provider** — a single free OpenRouter model (`llama-3.3-70b:free`) is the de-facto brain. One upstream rate-limit = no reasoning. This is fragile, not robust.
2. **`POST /pews/run` took 23.5 seconds** for **2 students**. That is sequential, blocking, per-student LLM calls. At 400 students it is unusable. This is not how an agent that "thinks" should be architected.
3. **`FIREBASE_INIT … push dispatch DISABLED`** — the ACT stage's most important channel (push to the people who must act *today*) is silently off. The loop opens an intervention nobody is pinged about.

Beyond those operational issues, the *product* is generic:

- It looks at **3 signals** (attendance, marks, leave) out of **~15 signals the database already holds**.
- It produces **one paragraph per student** with **no plan, no sequence, no follow-up, no escalation, no memory of what was already tried.** That is a *label generator*, not an *agent*.
- The LLM is a **single-shot text completer** (`LlmClient` has no tool/function calling — verified). A real agent *uses tools*: it queries the DB, reads the history of past interventions, drafts a message, checks the calendar, and decides the next step. We are pasting the same static prompt template to every student, every day.
- There is **no closed feedback flywheel** — `effectiveness` counts outcomes but nothing *feeds that back* into which action gets recommended next time, or into the risk score itself.

**This document designs the version that is genuinely useful, genuinely agentic, and built almost entirely on resources we already have or can get for free.** It is opinionated, it is deep, and it is grounded in the exact tables, services, and constraints of this repo.

---

## 1. The reframe: from "Early Warning System" to "Student Success Agent"

PEWS today answers one question: *"Who is at risk?"*

A ₹100Cr operating system for a campus must answer a **chain** of questions, autonomously, and only pull a human in when judgment is genuinely required:

| # | Question | Today | PEWS 2.0 |
|---|---|---|---|
| 1 | **Who** is at risk, and how confident are we? | ✅ 3 signals, z-score | 15 signals, calibrated confidence, *leading* not lagging |
| 2 | **Why** — what is the most likely driver? | 🟡 1 LLM paragraph | Multi-signal causal hypothesis, ranked, with evidence links |
| 3 | **What** is the right next action *for this specific child*? | 🟡 1 static recommendation | A **sequenced playbook** chosen from what has *worked before* for similar cases |
| 4 | **Who** should do it and **by when**? | ✅ class teacher, no due date | Owner + SLA + escalation ladder |
| 5 | **Did it work?** | 🟡 manual outcome tag | Auto-measured from the next snapshot delta + outcome tag |
| 6 | **What should we learn?** | ❌ counts only | The flywheel: outcomes re-weight both the *recommendation policy* and the *risk model* |
| 7 | **What is the school-wide pattern?** | ❌ not built | Cohort/class/grade trend agent: "9-B science marks −12%/3wk after teacher change" |
| 8 | **What do we tell the parent — kindly, in their language?** | 🟡 nudge card only | A reviewed, vernacular, non-clinical draft the teacher sends in one tap |

The mental model shifts from a **report** to an **employee**: a tireless junior counsellor who reads every child's record every night, drafts the casework, escalates the urgent ones, writes the parent note, remembers what was tried, and tells you on Monday which of last week's actions actually moved the needle.

---

## 2. The pain points, named honestly (the "think deep" part)

Before designing, here are the *real* pains around the current PEWS — for each role and for the platform — because the design must kill these specifically, not add features for their own sake.

### 2.1 For the School Admin (the owner)
- **"It tells me who, but not what to do about it at scale."** 40 flagged kids, 40 identical-feeling cards. No triage, no "these 5 are urgent and unowned for 3 days."
- **"I can't see if any of this is working."** Effectiveness is raw counts, not "parent calls resolve attendance dips 3× better than remedial classes here."
- **"It's blind on the web portal I actually use."** (Confirmed in the audit — web admin still shows the legacy `early_warning` list.)
- **"It runs once a night."** A child who stopped coming on Monday is flagged… eventually. There is no on-event reaction.

### 2.2 For the Teacher (the doer)
- **"Another to-do with no help attached."** The intervention is a task, not assistance. Where's the drafted message? The talking points? The history of what the *previous* teacher tried?
- **"I get a card but no push"** (Firebase disabled) — so I find out when I happen to open the app.
- **"It doesn't know my reality."** No awareness of the calendar (exam week, holidays), of my actual class load, of which parents are responsive.

### 2.3 For the Parent (the one who can change the outcome)
- **"I hear from school only when it's already bad."** The nudge is reactive. And it's in English, often clinical-feeling, with no clear "what do I do tonight."
- **No push** (Firebase) — the gentlest, most important channel is off.

### 2.4 For the Platform / Engineering
- **Single-provider brain** → `429` = outage. (Seen in the log.)
- **Sequential blocking LLM calls** → 23.5s for 2 students; O(n) and unscalable.
- **No tool-use** → the "agent" can't look anything up; it only completes a fixed prompt. Every student gets a near-identical template.
- **No batching / no async job queue** despite `ai_jobs` table existing.
- **Reasoning re-run waste** — cache is by signal hash (good) but there's no *cohort-level* dedup or cheap-model pre-filter before spending a REASON call.
- **No evaluation harness** — we can't prove the AI output is grounded; LAW 6 is enforced by prompt discipline only, not by a check.

---

## 3. Resources we already have (so we build on them, not around them)

A core constraint you set: **use what we have or what's free.** Here is the verified inventory.

### 3.1 The AI Gateway — already a real asset (verified in `feature/ai/`)
- `AiService` — single choke point: cache → circuit breaker → dual-home failover → guardrails (PII redact) → usage log. Lanes `FAST_CHAT / CLASSIFY / REASON / BATCH`.
- `KeyVault` + `EncryptionService` — AES-256-GCM at-rest keys, env-bootstrap, rotation, `.env`-aware (fixed this session).
- `LlmClient` — one OpenAI-compatible client for **5 free providers** (Cerebras, Groq, SambaNova, Mistral, OpenRouter).
- `CircuitBreaker`, `GuardrailService`.
- Tables: `ai_provider_config`, `ai_prompt_templates`, `ai_usage_log`, `ai_response_cache`, **`ai_jobs`** (async queue — *exists but unused*), `ai_provider_health`.

> **Two latent assets we are NOT using yet:** the `ai_jobs` async-queue table, and `ai_prompt_templates` (versioned, per-template `pii_allowed_providers`). PEWS 2.0 turns both on.

### 3.2 Data signals already in the DB but unused by PEWS (verified table list)
PEWS v1 reads attendance, marks, leave. The schema already holds **far more**:

| Untapped signal | Table | What it predicts |
|---|---|---|
| **Homework non-submission streak** | `homework_submissions`, `homework` | Disengagement *before* marks drop (leading indicator) |
| **Fee stress** | `fee_records` (PAID/DUE/OVERDUE) | Strong correlate of dropout risk in Indian K-12 |
| **Health incidents / chronic** | `student_health_incidents`, `student_health_profiles`, `student_immunizations` | Absence cause; safeguarding |
| **Behaviour / check-ins** | `teacher_check_ins` | Mood / engagement notes teachers already write |
| **Parent responsiveness** | `messages`, `message_status`, `notifications` | Who actually reads nudges → route differently |
| **Exam trajectory (term-over-term)** | `exam_results` | Longer-arc academic slide vs single assessment |
| **PTM attendance / progress** | `ptm_events`, `ptm_class_progress` | Parent engagement signal |
| **Transport disruption** | `transport_assignments` | A real, fixable absence cause |
| **Scholarship dependency** | `scholarships`, `scholarship_applications` | Fee-stress mitigation context |
| **Leave by proper FK** | `leave_requests.child_id` | Fix the fragile name-match in v1 |

> **Finding:** PEWS 2.0's richer Sense layer needs **zero new input tables.** Every signal above is already stored. This is the single biggest "free" upgrade available.

### 3.3 The communication & action spine (already built)
- `Notify.toUser/toUsers(category="pews", deepLink=…)` — in-app bell + FCM (FCM currently off; see §9).
- `MessagesRouting` — real parent↔teacher threads (the channel for AI-drafted messages).
- `ParentPulse` weekly digest — a coordination point so we don't double-notify.
- Existing scheduled-job pattern (`PulseWeeklyJob`, `NotificationScheduler`).

### 3.4 Free capabilities we can add at zero marginal cost
- **Tool/function calling** — Groq, Cerebras, Mistral, OpenRouter all support OpenAI-style `tools`. Our `LlmClient` just doesn't send them yet. Adding it unlocks the *agentic* leap.
- **Whisper STT (Groq, free)** — voice notes from teachers → structured check-in signal.
- **Embeddings for "similar past cases"** — Mistral/OpenRouter offer free embedding endpoints; or a cheap local cosine over a tiny set. Enables "what worked for kids like this."
- **JSON / structured-output mode** — supported by these providers; makes parsing robust (kills the regex `parseJsonish`).

---

## 4. The PEWS 2.0 architecture — a real agent loop with tools

The leap from v1 to v2 is: **stop sending one static prompt per student. Give the model tools and a goal, run it cheaply in tiers, and let it produce a structured *case file* and *plan*, not a paragraph.**

```
                       ┌─────────────────────────────────────────────────────────┐
                       │  TIER 0 — SENSE (deterministic, cheap, ALWAYS runs)       │
                       │  15-signal feature vector per student → risk + confidence │
                       │  NO LLM. Owns who/what/numbers (LAW 6).                    │
                       └───────────────┬─────────────────────────────────────────┘
                                       │ feature vectors for the whole school
                       ┌───────────────▼─────────────────────────────────────────┐
                       │  TIER 1 — TRIAGE (cheap CLASSIFY model, batched)          │
                       │  Groq 8B / Cerebras: "is this worth a deep look?" +       │
                       │  bucket the cause family. Filters 400 → ~30 deep cases.   │
                       │  Cohort-dedup: identical signal clusters reasoned once.   │
                       └───────────────┬─────────────────────────────────────────┘
                                       │ only the cases that need judgment
                       ┌───────────────▼─────────────────────────────────────────┐
                       │  TIER 2 — CASEWORKER AGENT (REASON model + TOOLS)         │
                       │  Per selected case, a tool-using agent that can:          │
                       │   • get_student_history(code)      ← past snapshots       │
                       │   • get_past_interventions(code)   ← what was tried       │
                       │   • get_similar_resolved_cases()   ← what WORKED (flywheel)│
                       │   • get_calendar_context()         ← exam week? holiday?  │
                       │   • get_parent_responsiveness()    ← which channel works  │
                       │  Produces a STRUCTURED CASE FILE (not prose):             │
                       │   {hypotheses[], evidence[], plan[steps], parent_draft,   │
                       │    urgency, owner_hint, confidence}                        │
                       └───────────────┬─────────────────────────────────────────┘
                                       │ structured case file persisted
                       ┌───────────────▼─────────────────────────────────────────┐
                       │  TIER 3 — ACT (deterministic orchestration)               │
                       │  Open/seq interventions w/ SLA + escalation ladder;       │
                       │  Notify (bell+FCM) owner; queue parent draft for review;   │
                       │  schedule follow-up checkpoint.                            │
                       └───────────────┬─────────────────────────────────────────┘
                                       │
                       ┌───────────────▼─────────────────────────────────────────┐
                       │  TIER 4 — LEARN (the flywheel, the part that compounds)   │
                       │  Auto-measure outcome from NEXT snapshot delta + tag;     │
                       │  update action-effectiveness priors per cause-family;     │
                       │  feed back into Tier-1 triage + Tier-2 recommendations +  │
                       │  (optionally) recalibrate Tier-0 weights.                 │
                       └─────────────────────────────────────────────────────────┘
```

### Why the tiering matters (this fixes the 23.5s / 429 problem)
- **Tier 1 is a cheap, batched classify** on a fast 8B model — it spends one tiny call (or a single batched call) to decide which ~30 of 400 students deserve the expensive Tier-2 agent. v1 spends a 70B REASON call on *every* flagged student, sequentially.
- **Tier 2 runs only on the filtered set, concurrently** (bounded by `AI_BATCH_CONCURRENCY`), through the **async `ai_jobs` queue** so `POST /pews/run` returns in <1s ("recompute queued") and the agent works in the background.
- **Cohort dedup**: 12 kids in 9-B with the identical "attendance −15% post-Diwali" pattern get **one** class-level reasoning + a shared plan, not 12 near-identical paragraphs. (This directly attacks your "not pasting the same paragraph to everyone" requirement.)

### Why tool-use matters (this is what makes it *agentic*, not generic)
v1's prompt is the same skeleton for everyone; the model only sees the current signals. The Tier-2 agent *pulls context*: it sees that "we already called this parent twice with no change" → it escalates to a counsellor referral instead of recommending a third call. **The recommendation is conditioned on history and on what worked for similar children** — that is the difference between an agent and a template.

---

## 5. TIER 0 — the 15-signal Sense layer (free, deterministic, biggest single upgrade)

Keep v1's z-score/slope math; **add the untapped signals** (§3.2) and emit a **feature vector + calibrated confidence**, not just a score.

### 5.1 The expanded signal set
| Signal | Source | Computation | Leading? |
|---|---|---|---|
| Attendance % + slope | `attendance_records` | (v1) keep | lagging |
| Marks % + slope | `assessment_marks`/`assessments` | (v1) keep | lagging |
| Leave count | `leave_requests` (**now by `child_id` FK first**, name fallback) | (v1, fixed join) | mixed |
| **HW non-submission streak** | `homework_submissions` vs assigned `homework` | consecutive missed / 30d | **leading** |
| **Fee status** | `fee_records` | any OVERDUE; days overdue | context |
| **Health flag** | `student_health_incidents` | incident in window; chronic profile | context |
| **Behaviour note sentiment** | `teacher_check_ins` | count of negative-tag check-ins | leading |
| **Exam term trajectory** | `exam_results` | term-over-term delta | lagging-arc |
| **Parent engagement** | `messages`/`ptm_events` | read-rate, PTM no-shows | modifier |
| **Transport disruption** | `transport_assignments` | route change/unassigned | fixable cause |

### 5.2 Output: a structured feature vector (not just an int)
```jsonc
{
  "student_code": "G10A-001-3",
  "risk_score": 72, "risk_level": "high",
  "confidence": 0.81,              // NEW: how much data backs this (data completeness × signal agreement)
  "leading_score": 64,            // NEW: weight leading indicators so we flag BEFORE marks collapse
  "signals": [ ... typed, with evidence refs ... ],
  "cause_family": "disengagement", // NEW: cheap heuristic bucket (attendance|academic|disengagement|wellbeing|financial|external)
  "deltas_vs_last_run": { "attendance_pct": -6 }  // NEW: movement, for auto-outcome later
}
```

> **Why confidence + leading_score:** today a slipping high-performer with sparse data and a kid with months of low data look the same. Confidence lets the UI say "watch — limited data" honestly (LAW 6) and lets Tier-1 skip low-confidence noise. Leading_score is what makes it *predictive* rather than a rear-view mirror.

### 5.3 Cause-family bucketing (cheap, deterministic, pre-LLM)
A simple rule map from dominant signals → one of 6 families. This (a) routes to the right **playbook**, (b) is the dedup key for cohort reasoning, (c) is the join key for the effectiveness flywheel ("for *financial* cause, fee-counselling works; for *disengagement*, mentor pairing works").

**Cost: ₹0. Pure Kotlin/SQL over existing tables. This alone roughly doubles PEWS's predictive surface.**

---

## 6. TIER 1 & 2 — the agentic core (the real differentiator)

### 6.1 Add tool-calling to `LlmClient` (the missing primitive)
`LlmClient` currently sends `{messages}` only. Add optional `tools` + `tool_choice` (OpenAI shape, supported by Groq/Cerebras/Mistral/OpenRouter) and a tool-result message role. `AiService.complete()` gains a `tools` param and an agent-loop helper `AiService.runAgent(...)` that:
1. sends messages + tool schemas,
2. if the model returns `tool_calls`, executes the **whitelisted, read-only, school-scoped** Kotlin tool, appends the result, loops (max N steps, hard-capped),
3. returns the final structured JSON.

> **Safety:** tools are **read-only and tenant-scoped** (the tool functions take `schoolId` from the JWT context, never from the model). The model can *ask* for data; it can never *write*. All writes stay in deterministic Tier-3. This keeps LAW 6 intact and makes the agent safe.

### 6.2 The Caseworker tools (all over existing tables/services)
| Tool | Backed by | Returns |
|---|---|---|
| `get_student_history(code, n)` | `PewsSnapshotService.studentHistory` (exists) | trend of risk/signals |
| `get_past_interventions(code)` | `PewsInterventionService.listInterventions` (exists) | what was tried + outcomes |
| `get_similar_resolved_cases(cause_family)` | NEW query on `pews_interventions` joined to outcomes | "for this cause, X action resolved Y%" |
| `get_calendar_context(date, class)` | `AcademicCalendarTable`/`HolidayListTable` | exam week? holiday? (don't nag during exams) |
| `get_parent_responsiveness(code)` | `messages`/`message_status` | best channel + responsiveness |
| `get_homework_detail(code)` | `homework_submissions` | which subjects are being skipped |

### 6.3 The structured Case File (replaces the 3-string output)
```jsonc
{
  "narrative": "1–2 grounded sentences (LAW 6).",
  "hypotheses": [
    {"cause": "post-festival disengagement", "confidence": 0.6, "evidence": ["attendance -15% since 14 Oct", "2 HW missed"]}
  ],
  "plan": [                              // a SEQUENCE, not one action
    {"step": 1, "action": "parent_call", "owner": "class_teacher", "sla_days": 2,
     "rationale": "first contact; parent is responsive on WhatsApp"},
    {"step": 2, "action": "mentor_pairing", "owner": "class_teacher", "sla_days": 7,
     "condition": "if no improvement after step 1"}
  ],
  "parent_draft": {                      // ready for one-tap review-and-send
    "language": "hi",                    // vernacular by default (free, big UX win)
    "tone": "warm, non-clinical",
    "body": "नमस्ते … (no 'risk', no scores, concrete next step)"
  },
  "urgency": "high",                     // drives SLA + escalation
  "skip_reason": null                    // e.g. "exam week — defer parent contact" (calendar tool)
}
```

> **This is the anti-generic mechanism.** The plan is conditioned on history (tool: past interventions), on what worked (tool: similar resolved cases), on timing (tool: calendar), and on the parent (tool: responsiveness). Two children with the same raw signals but different histories get **different plans**. No template paste.

### 6.4 Structured output + grounding check (kills the regex parser & enforces LAW 6 by code)
- Request **JSON mode** from the provider; validate against the Case File schema; on failure, one repair retry, else degrade to deterministic-only.
- **Grounding guard:** post-generate, verify every number/name in `narrative`+`hypotheses.evidence` exists in the deterministic bundle (string/number membership check). If the model invented a figure → drop that field (never show it). This makes LAW 6 a *test*, not a hope.

---

## 7. TIER 3 — ACT: from "a task" to "managed casework"

| Upgrade | Why |
|---|---|
| **Sequenced interventions** (the `plan[]`) with `step`, `condition`, `sla_days` | A plan, not a single to-do |
| **SLA + escalation ladder** | Unowned/overdue high-urgency case → escalate to admin after SLA; this is the "5 urgent unowned for 3 days" triage admins begged for |
| **One-tap parent draft** (`POST /teacher/pews/interventions/{id}/draft-message`) | The single highest-value missing action (also in v1 backlog). AI drafts in the parent's language; teacher reviews; sends via existing `MessagesRouting`. **Never auto-sent.** |
| **Follow-up checkpoint** auto-scheduled | "Re-check this child in 7 days" → next run auto-measures the delta |
| **Reassign owner** (admin) | v1 auto-resolves only |
| **Calendar-aware suppression** | Don't nag parents during exam week / holidays (tool-driven) |

### Escalation ladder (deterministic, config-driven)
```
open ──(SLA_1 elapsed, no progress)──► remind owner (push)
     ──(SLA_2 elapsed)──────────────► escalate to admin + co-own
     ──(worsened on next snapshot)───► bump urgency, surface top-of-cohort
```

---

## 8. TIER 4 — LEARN: the flywheel that compounds (the ₹100Cr part)

v1 counts outcomes. v2 **closes the loop so the system gets smarter every week** — this is what turns a feature into a moat.

### 8.1 Auto-measured outcomes (not just manual tags)
On the next run, compare the child's new snapshot to the snapshot when the intervention opened:
- attendance/marks/HW deltas → `auto_outcome ∈ {improved, unchanged, worsened}`
- combine with the owner's manual tag for a **trusted outcome**.

### 8.2 Effectiveness priors per cause-family (the policy memory)
Maintain, per school, a small table:
```
cause_family × action_type → {n_tried, n_improved, improve_rate, avg_days_to_improve}
```
This is exactly what `get_similar_resolved_cases()` reads. Now the recommendation is **evidence-based for THIS school**: "for *disengagement* in this school, *mentor_pairing* improves 68% vs *remedial_class* 31% → recommend mentor pairing first."

### 8.3 Feedback into all three upper tiers
- **Into Tier-2:** the priors are a tool input → better plans.
- **Into Tier-1:** cause-families that historically resolve themselves can be triaged lower.
- **Into Tier-0 (optional, careful):** if "fee overdue" consistently precedes dropout in this school, nudge its weight up — but **bounded, logged, and reversible** (never let the model rewrite the deterministic core; this is a slow, audited recalibration, not live learning).

### 8.4 The admin Effectiveness view becomes prescriptive
Instead of "12 done / 4 improved," the admin sees: **"Parent calls resolve attendance dips in ~5 days here; remedial classes don't move attendance — use them for marks. 3 high-urgency cases are unowned past SLA."**

---

## 9. The non-negotiable operational fixes (from the live log)

These are not features — they are why v1 *feels* generic/fragile. Fix first.

| Problem (from log) | Fix | Effort |
|---|---|---|
| **`429` on the single OpenRouter free model** | Already have 5 providers — the issue is REASON lane defaults to overloaded free models. **Diversify model pins per provider; ensure ≥3 live REASON providers; add jittered backoff + the existing circuit breaker actually rotates.** Add a paid OpenRouter credit as cheap insurance (optional). | S |
| **23.5s for 2 students (sequential blocking)** | Move REASON to the **async `ai_jobs` queue**; `POST /pews/run` returns immediately; Tier-1 prefilter + bounded concurrency. | M |
| **`FIREBASE_INIT … push DISABLED`** | Provide Firebase creds (free) via `FIREBASE_CREDENTIALS_JSON` (already supported per the log's own list). Without push, ACT is half-dead. | S (config) |
| **Same prompt to everyone** | Tiering + tools + cohort dedup (§4, §6). | L |
| **No grounding test** | §6.4 grounding guard. | S |

---

## 10. What else connects to PEWS (the ecosystem — features we have + features to build)

You asked which other features integrate with PEWS, and which new ones to build. PEWS is the **action layer**; almost every module is either a *signal source* or an *action channel* for it.

### 10.1 Already in the codebase — wire them into PEWS
| Module | Role for PEWS | Integration |
|---|---|---|
| **Attendance/Marks** | primary signals | already; add on-write recompute trigger |
| **Homework** | leading signal | add to Sense (§5) |
| **Fees** | financial-cause signal + fee-counselling action | Sense + a "fee plan" intervention type |
| **Messages** | the parent-draft delivery channel | `draft-message` → existing thread |
| **Notifications** | the alert spine | already (fix FCM) |
| **Parent Pulse** | weekly digest | coordinate so PEWS nudge isn't duplicated; surface "what we did this week" |
| **Health** | wellbeing-cause signal + safeguarding flag | Sense + a counsellor-referral path |
| **Calendar** | timing guard | tool in Tier-2 |
| **Transport** | fixable absence cause | Sense + "fix route" action |
| **PTM** | parent-engagement signal | Sense modifier |
| **Alumni/Mentorship** | mentor_pairing action pool | source mentors for disengagement cases |
| **Scholarships** | financial mitigation | suggest scholarship for fee-stress cases |

### 10.2 New AI features that *extend* PEWS (sibling agents on the same gateway)
These reuse the tiered-agent + flywheel pattern — build once, apply many:

1. **Wellness Early-Warning (non-academic sibling)** — same loop over `student_health_incidents` + check-in sentiment; safeguarding-grade routing (counsellor only, stricter PII). *Already named in `AI_FEATURES_PLAN.md` Feature #8.*
2. **Teacher-Effectiveness Patterns** — the cohort agent (§4 Tier-2 cohort path) surfaced school-wide: "9-B science slid after the teacher change." Sensitive; admin-only, framed as support not blame.
3. **Parent-Engagement Agent** — predicts which parents are disengaging (read-rates, PTM no-shows) and nudges *the school* to re-engage them *before* the child slips. PEWS's upstream.
4. **Retention / Dropout-Risk Agent** — long-arc model (fees + attendance + exam trajectory) for the business-critical "who might not re-enroll." Directly tied to revenue → directly tied to valuation.
5. **Intervention-Playbook Library** — the flywheel productized: a living, per-school library of "what works," shareable (anonymized) across schools as a network effect (the real moat at scale).
6. **Counsellor Copilot** — for high/wellbeing cases, a tool-using agent that assembles the full case file for a human counsellor (history, attempts, family context) — turns a 30-min prep into 30s.

### 10.3 The signal flywheel (why this becomes a moat)
Every action's outcome feeds the priors; priors improve recommendations; better recommendations → better outcomes → richer priors. Across many schools (anonymized, opt-in), the playbook library becomes **proprietary knowledge no competitor's day-one product has**. *That* is the ₹100Cr asset — not the LLM, which everyone rents.

---

## 11. Build sequence (each phase shippable, grounded, committed)

> Ordered by **value ÷ effort**, operational fixes first (they unblock everything).

### Phase 0 — Operational hardening (days) — *fixes the log*
1. Firebase creds → push ON (config).
2. REASON lane: diversify model pins, ensure ≥3 live providers, verify circuit breaker rotation + jittered backoff. Kill the single-`429`-outage.
3. Fix `leave_requests` join to use `child_id` FK first.

### Phase 1 — Tier-0 richness (week) — *biggest free predictive jump*
4. Add HW, fee, health, behaviour, transport signals to `PewsSnapshotService`.
5. Emit `confidence`, `leading_score`, `cause_family`, `deltas_vs_last_run`.
6. Surface these on existing admin/teacher cards (they already render nullable fields).

### Phase 2 — Tiering + async (week) — *fixes the 23.5s + scales*
7. `LlmClient` JSON mode + tool-calling; `AiService.runAgent()`.
8. Tier-1 cheap CLASSIFY prefilter + cohort dedup.
9. Move REASON to `ai_jobs` async queue; `POST /pews/run` returns immediately.

### Phase 3 — Caseworker agent (week+) — *the agentic leap*
10. Implement the 6 read-only, school-scoped tools.
11. Structured Case File schema + grounding guard.
12. Replace single-paragraph REASON with the tool-using caseworker.

### Phase 4 — Managed ACT (week) — *the human loop*
13. Sequenced interventions + SLA + escalation ladder.
14. `POST /teacher/pews/interventions/{id}/draft-message` (vernacular, review-before-send).
15. Reassign owner; calendar-aware suppression; follow-up checkpoints.

### Phase 5 — LEARN flywheel (week) — *the compounding moat*
16. Auto-measured outcomes from snapshot deltas.
17. Cause-family × action effectiveness priors table + `get_similar_resolved_cases`.
18. Prescriptive admin Effectiveness view.
19. (Careful, audited) optional Tier-0 weight recalibration.

### Phase 6 — Surfaces & siblings (parallelizable)
20. Web admin PEWS module (the big v1 gap).
21. Patterns / cohort agent UI.
22. AI-usage / provider-health admin screen.
23. Sibling agents (Wellness, Retention) on the same pattern.

---

## 12. Acceptance criteria — "PEWS 2.0 is real, not generic"

- [ ] `POST /pews/run` returns in **<1s** (work queued); reasoning completes async via `ai_jobs`.
- [ ] No single-provider outage: with one provider rate-limited (the `429` case), REASON still completes via failover; the run logs which provider answered.
- [ ] Push notifications actually fire (Firebase configured); owner gets a deep-linked alert the moment a high case opens.
- [ ] Sense uses **≥8 signals**; each flagged student shows `confidence` and ≥1 **leading** signal where present.
- [ ] Two students with identical raw signals but different histories receive **measurably different plans** (proves tool-conditioning, not templating).
- [ ] Cohort dedup: a class-wide pattern is reasoned **once**, not per-student.
- [ ] Every AI field passes the **grounding guard** (no number/name absent from the deterministic bundle) — LAW 6 enforced by code.
- [ ] Teacher can one-tap a **vernacular** parent draft, review, and send via Messages (never auto-sent).
- [ ] Closing/measuring an intervention updates the **cause-family effectiveness priors**, and the next recommendation for a similar case reflects them.
- [ ] Admin Effectiveness view is **prescriptive** ("X works for cause Y here"), and shows SLA-breached unowned cases.
- [ ] Every LLM call writes one `ai_usage_log` row; AI-usage screen shows per-school tokens + provider health.

---

## 13. Risks & guardrails

| Risk | Guardrail |
|---|---|
| Agent fabricates a student/number | Tools are read-only & scoped; deterministic layer owns who/what; **grounding guard** drops ungrounded fields; LAW 6 in UI. |
| Tool-loop runs away / cost blowup | Hard max-steps cap; bounded concurrency; cheap Tier-1 prefilter; cache by signal hash + cohort dedup. |
| PII to training-opt-in provider | Per-template `pii_allowed_providers` allow-list; PEWS PII pinned to no-training lane; `GuardrailService.redactPii()` pre-flight. |
| Over-automation harms a child | **Nothing is auto-sent to parents**; high/wellbeing cases route to humans; calendar suppression; escalation to admin, not auto-action. |
| Live "learning" drifts the model | Tier-0 recalibration is **slow, bounded, logged, reversible**; the LLM never edits the deterministic core. |
| Free-tier model deleted/rate-cut | 5 providers, dual+ homed per lane; circuit breaker; pin by capability not exact model; optional paid OpenRouter insurance. |
| Vernacular draft is wrong/awkward | Always teacher-reviewed before send; templates per language; start with Hindi+English, expand on demand. |

---

## 14. One-paragraph summary for the team

PEWS v1 is a correct but generic early-warning *report*: 3 signals, one static LLM paragraph per student, sequential calls that took 23s for 2 kids on a single rate-limited free model with push notifications silently off. PEWS 2.0 turns it into a **tiered, tool-using Student-Success Agent**: a 15-signal deterministic Sense layer (all from tables we already have), a cheap batched Triage tier that filters the school down to the cases needing judgment, a **Caseworker agent that uses read-only tools** (history, past attempts, what-worked, calendar, parent-responsiveness) to produce a **structured, conditioned case file and sequenced plan** — different for every child, never a template paste — and a **LEARN flywheel** that auto-measures outcomes and feeds per-school "what works" priors back into the recommendations. It runs **async** (fixing the 23s), **fails over** across 5 free providers (fixing the 429), drafts **vernacular** parent messages a teacher sends in one tap, and enforces honesty (LAW 6) with a code-level grounding guard. Built almost entirely on assets already in this repo (`AiService`, `ai_jobs`, `ai_prompt_templates`, the notification spine, and ten unused signal tables), it is the difference between a feature and the compounding, proprietary student-success moat a ₹100Cr campus OS is actually sold on.

---

*End of PEWS 2.0 redesign brief. Next session: start at §11 Phase 0 (operational fixes from the log) → Phase 1 (signal richness) → Phase 2 (async + tools).*
