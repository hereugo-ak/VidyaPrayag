# AI Report Card 2.0 — The Agentic Term-Narrative Engine

> **Branch:** `aifeatures`
> **Date:** 2026-06-29
> **Author:** AI Engineering — deep redesign brief
> **Status:** Vision + grounded build plan. Nothing here is faked; every claim is checked against the code on this branch.
> **Companion docs (read in this order):**
> - `AI_FEATURES_PLAN.md` — the AI program strategy (Report Card is **Feature #2**, §"Feature #2 — AI Report Card")
> - `PEWS_AI_GATEWAY_IMPLEMENTATION_PLAN.md` — the gateway/key-management build plan
> - `PEWS_2.0_AGENTIC_REDESIGN.md` — the agentic pattern this doc **reuses verbatim** (Sense → Triage → Caseworker → Act → Learn, tools, grounding guard, flywheel)
> - `newreviewdocs/specs/AI_REPORT_CARD_SPEC.md` — the **v1 skeleton this doc supersedes** (it describes a correct-but-generic single-shot LLM; this is the v2 worth selling)
> - `newreviewdocs/specs/NEP_COMPLIANCE_SPEC.md` — the Holistic Progress Card (HPC) tables this consumes

---

## 0. Why this document exists (read this first)

We already shipped a real **AI Gateway** and a real **agentic PEWS 2.0** on this branch. The gateway is not a slide — it is running code:

- `feature/ai/AiService.kt` — **the single choke point** for every LLM call: lane routing → jittered candidate shuffle → PII guardrail → L1 cache → circuit-breaker failover across **7 providers** → usage log. It also has `runAgent(...)` — a real **tool-calling agent loop** (verified, lines 439–590).
- `feature/ai/KeyVault.kt` — AES-256-GCM at-rest keys, env-bootstrap, rotation, `.env`-aware; **7 providers** wired: `CEREBRAS, GROQ, GROQ_FAST, SAMBANOVA, MISTRAL, OPENROUTER, GEMINI` with `noTraining` PII flags.
- `feature/ai/LlmClient.kt` — one OpenAI-compatible client with **`tools` + `tool_choice` + tool-result message role** (verified — function calling is live).
- `feature/pews/caseworker/` — a **Tier-2 caseworker agent** that calls 6 read-only, school-scoped tools and emits a **grounded structured Case File**, with a `GroundingGuard` that drops any number/name the model invented. **This is the exact pattern we clone for report cards.**

So the question is **not** "can we build an agentic report card?" — the plumbing exists. The question is: **what does a report card look like when it is built like PEWS 2.0 — a tiered, tool-using, grounded, multi-provider agent — instead of the generic single-LLM-call stub in `AI_REPORT_CARD_SPEC.md`?**

That stub (v1) is honest about being shallow:
- It makes **one LLM call per subject** (`generateNarratives`), **one** for the summary, **one** for prediction. At 40 students × 6 subjects that is **~320 sequential blocking calls per class** — the *exact* 23.5-second-for-2-students failure PEWS 2.0 was redesigned to kill.
- Its prompt is a **fixed `{{var}}` template** pasted per subject. Two students with a 64% in English get a near-identical "satisfactory, work on grammar" sentence. That is a **mail-merge**, not an agent.
- It has **no memory** — it never looks at last term's report, never knows the child was flagged by PEWS, never knows the parent doesn't read English, never checks the board's grading rules. It cannot *use a tool*.
- Its "predictive insight" is **one ungrounded LLM guess** (`{"likely_grade": "B"}`) with no deterministic projection behind it — a direct LAW-6 violation risk (the model can hallucinate a grade trajectory that the marks don't support).
- It is **parent-as-afterthought**: English-only narrative, no vernacular, no "what do I do tonight," no link to the focus area PEWS already computed.

**This document designs the version that is genuinely agentic, genuinely grounded, multilingual, and built almost entirely on assets already in this repo** — the gateway, the agent loop, the grounding guard, the `ai_jobs` async queue, the notification spine, the PEWS signal moat, and the NEP HPC tables. It is opinionated, deep, and grounded in the exact tables and services on this branch.

---

## 1. The reframe: from "narrative generator" to "Term-Close Reporting Agent"

The v1 spec answers one question: *"What sentence describes this 64% in English?"*

A ₹100Cr campus OS must answer the **chain** a real class teacher answers at term close — autonomously, grounded, and only pulling the human in to **review and approve**, never to draft from scratch:

| # | Question | v1 stub | Report Card 2.0 |
|---|---|---|---|
| 1 | **What** are the real, auditable numbers for this child this term? | 🟡 marks only | Deterministic **academic rollup** (marks % + grade + rank band + attendance + co-scholastic + holistic), term-over-term, **owns every number (LAW 6)** |
| 2 | **How** is this child *moving* — improving, sliding, plateaued? | ❌ none | Deterministic **trajectory** (term-over-term slope per subject; reuses PEWS slope math) |
| 3 | **Why** did a subject move — and is it real or noise? | 🟡 1 LLM sentence | Multi-signal **cause hypothesis** grounded in attendance/HW/exam-analysis topic gaps, ranked with evidence |
| 4 | **What** should this specific child focus on next term? | 🟡 generic "work harder" | A **specific, conditioned focus area** — seeded by PEWS recommended-focus + exam-analysis topic gaps + what worked for similar students |
| 5 | **What** is the likely next-term band, honestly? | ❌ ungrounded LLM guess | **Deterministic projection** (trajectory + attendance) with a confidence band; LLM only *phrases* it, never invents it |
| 6 | **Who** writes the 40 comments, and who approves? | 🟡 AI writes, no review loop | Agent **drafts**; teacher **reviews/edits/bulk-approves**; admin **publishes** — reusing the existing results-publish + parent-notify path |
| 7 | **What** does the parent actually read — in their language? | ❌ English only | A **warm, vernacular, non-clinical** parent summary with a concrete next step and one-tap actions (links to PEWS focus, attendance, message-teacher) |
| 8 | **Is the narrative even true?** | ❌ prompt-discipline only | A **GroundingGuard** (already built for PEWS) verifies every number/name traces to the deterministic rollup; ungrounded fields are dropped |

The mental model shifts from a **mail-merge** to an **employee**: a tireless senior teacher who, the night the term closes, reads every child's full record (this term *and* last term, plus the PEWS flags and exam analysis), drafts a specific, honest, board-correct comment per subject, writes the parent a kind note in their own language, projects next term from the real trajectory — and hands the class teacher a **review queue**, not a blank page.

---

## 2. The pain points, named honestly (the "think deep" part)

Before designing, here are the *real* pains — for each role and for the platform — because the design must kill these specifically.

### 2.1 For the Teacher (the doer — primary user)
- **"40 students × 6 subjects = 240 comments, by Friday."** Term-close is the single most hated admin burden in Indian K-12. Teachers copy-paste "Good student, can do better" because there is no time. The report card becomes generic *because the human is overloaded* — not because they don't care.
- **"The system gives me no help, just a form."** v1's "narrative editor" is still a text box the teacher fills. The win is not an editor; it is a **pre-filled, specific, editable draft** so the teacher's job becomes *review*, not *write*.
- **"It doesn't know what I already know."** No awareness that this child was PEWS-flagged for attendance, that the exam analysis showed the whole class bombed ratios (so a low Math mark is partly a teaching-gap, not the child), or that last term this child improved 12% (so "needs to work harder" is *wrong and demoralising*).
- **"English comments for Hindi-medium parents are useless."** The teacher then hand-translates 40 summaries. 

### 2.2 For the School Admin (the owner / publisher)
- **"I publish, but I can't trust the AI text blindly."** Admin needs a **grounding guarantee** and a sign-off trail — not "the AI wrote 800 comments, hope they're right."
- **"Report cards are disconnected from everything else."** The same child PEWS flagged, the same exam analysis I ran, the same fee-stress note — none of it shows up coherently on the one document the parent keeps forever.
- **"Board compliance is manual."** CBSE/ICSE/State + NEP HPC have different grading scales, descriptor bands, and co-scholastic dimensions. Getting the template wrong is a compliance risk.
- **"Batch generation for 800 students must not melt the server"** — the v1 design's ~6,400 sequential calls would (this is the PEWS 23.5s problem at 100× scale).

### 2.3 For the Parent (the one who keeps the report forever)
- **"I get a number with no meaning."** "64 in English" tells a parent nothing they can act on.
- **"It's in a language I half-read, and it's clinical."** No "here's the one thing to do at home this holiday."
- **"It contradicts the app."** The report says "satisfactory" but the PEWS nudge last month said "at risk" — incoherence destroys trust.

### 2.4 For the Platform / Engineering
- **Single-provider brain → `429` = the whole batch fails** (the live PEWS log proved this; the gateway already fixes it with 7-provider failover — Report Card must *use* that, not re-introduce a single call).
- **Sequential blocking calls** → unscalable at class/school batch size (the `ai_jobs` async queue exists and is the answer).
- **No grounding test** → a hallucinated grade on a permanent legal document is a *much* worse LAW-6 violation than a wrong PEWS nudge. The grounding guard is non-negotiable here.
- **Cache waste** → 40 students in one class with the same "improved in Science" pattern shouldn't each burn a separate REASON call for the *boilerplate* part (cohort dedup applies here too).

---

## 3. Resources we already have (so we build on them, not around them)

A core constraint: **use what we have or what's free.** Verified inventory on this branch.

### 3.1 The AI Gateway — already a real, agentic asset (`feature/ai/`)
| Asset | What it gives Report Card 2.0 | Verified |
|---|---|---|
| `AiService.complete(...)` | lane routing, L1 cache (school-scoped SHA-256), 7-provider failover, PII guardrail, one `ai_usage_log` row per call | ✅ `AiService.kt` |
| `AiService.runAgent(...)` | **tool-calling agent loop** (max-steps capped, tool-result messages) — the engine for the report agent | ✅ `AiService.kt` 439–590 |
| `LlmClient` tool support | OpenAI `tools`/`tool_choice` over all providers | ✅ `LlmClient.kt` 50–95 |
| `KeyVault` (7 providers) | `CEREBRAS, GROQ, GROQ_FAST, SAMBANOVA, MISTRAL, OPENROUTER, GEMINI`; `noTraining` PII flags | ✅ `KeyVault.kt` 47–150 |
| `GuardrailService` | `looksLikePii`, `filterProvidersForPii`, `validateResponse` | ✅ used in `AiService` |
| `CircuitBreaker` | per-(provider,model) open/closed state; jitter shuffles closed circuits | ✅ `AiService.jitterCandidates` |
| `ai_jobs` table | **async batch queue** (status, total/completed, result) — *exists, used by PEWS, ready for class/school report batches* | ✅ `Tables.kt` `AiJobsTable` 2337 |
| `ai_prompt_templates` table | versioned prompts, per-template `pii_allowed_providers`, A/B `traffic_weight` | ✅ `Tables.kt` 2280 |
| `ai_response_cache`, `ai_usage_log`, `ai_provider_config`, `ai_provider_health` | cache, observability, keys, health | ✅ `Tables.kt` |

### 3.2 The PEWS 2.0 agentic pattern — clone it, don't reinvent it (`feature/pews/`)
The Report Card agent is a **sibling agent on the same gateway**, reusing the exact tier structure:

| PEWS component | Report Card 2.0 analogue | Reuse |
|---|---|---|
| `PewsSnapshotService` (Tier-0 Sense: deterministic feature vector) | `ReportRollupService` (deterministic academic rollup + trajectory) | **same math** (z-score/slope) |
| `TriageService` (Tier-1 cheap CLASSIFY prefilter + cohort dedup) | `ReportTriageService` (bucket students by trajectory pattern → cohort-dedup boilerplate) | **same pattern** |
| `CaseworkerService` + `CaseworkerTools` + `GroundingGuard` (Tier-2 tool agent) | `NarratorService` + `NarratorTools` + reuse `GroundingGuard` | **same `runAgent` engine, same guard** |
| `PewsInterventionService` / `ActModule` (Tier-3 deterministic Act) | `ReportAssemblyService` (assemble HPC, queue for review, publish) | **same orchestration shape** |
| `LearnService` + `EffectivenessPriors` (Tier-4 flywheel) | `ReportLearnService` (did the predicted band hold? which focus areas actually moved marks?) | **same flywheel** |
| `KillSwitchGuard`, `ModuleRegistry`, `PewsJobQueue`, `AuditLogger` | reuse directly under module name `reportcard` | **direct reuse** |

> **Finding:** Report Card 2.0 needs **almost no new infrastructure.** It is the PEWS five-tier loop pointed at a different question (term narrative instead of risk), reading mostly the same tables, writing to report-specific memory tables, and using the same `runAgent` + `GroundingGuard` + `ai_jobs` machinery. This is the single biggest "free" leverage available.

### 3.3 Data signals already in the DB (verified tables)
| Signal | Table(s) | Feeds the report |
|---|---|---|
| Per-subject marks % + grade | `assessment_marks` + `assessments` (`maxMarks`, `isPublished`, `subject`, `classId`) | the academic core |
| **Term-over-term trajectory** | same, grouped by `academicYearId` / term | "improving / sliding" (reuses PEWS slope) |
| Attendance % + slope | `attendance_records` (4-state) | attendance line + engagement context |
| **PEWS risk + recommended focus** | `pews_risk_snapshots` (`causeFamily`, `signalsJson`, `aiRecommendation`) | seeds focus area; ensures coherence with nudges |
| **Exam-analysis topic gaps** | (Feature #3 Teacher Copilot, when present) `assessment_marks` per-question | "low Math = class-wide ratios gap, not just this child" |
| Co-scholastic / holistic (NEP) | `holistic_assessments`, `co_scholastic_records` (NEP spec — to add) | HPC dimensions beyond marks |
| Competency / EI badges | `parent_achievements` (COMPETENCY / EI_METRIC) | strengths section |
| Board + medium of instruction | `schools.board`, `schools.medium` (verify column) | template + narrative language |
| Parent language preference | `app_users.languagePref` | vernacular routing |
| Last term's report | new `report_card_drafts` (the agent's memory) | "don't say 'work harder' to a child who improved 12%" |

### 3.4 Free capabilities we can add at zero marginal cost
- **Tool/function calling** — already wired in `LlmClient`; the report agent reuses it.
- **JSON / structured-output mode** — supported across providers; makes the per-subject narrative array parse robustly (no regex).
- **Cohort dedup** — the boilerplate ("the class did well in Science") is reasoned once per class-subject pattern, not per child.
- **Vernacular drafting** — same as PEWS parent-draft; Hindi + English first, expand on demand. Free.

---

## 4. The Report Card 2.0 architecture — a real agent loop with tools

The leap from v1 to v2 is identical to PEWS's leap: **stop sending one static prompt per subject. Give the model tools and a goal, run it cheaply in tiers, and let it produce a structured, grounded report draft — not a pile of mail-merge sentences.**

```
                       ┌─────────────────────────────────────────────────────────┐
                       │  TIER 0 — ROLLUP (deterministic, cheap, ALWAYS runs)      │
                       │  Per student: academic rollup + grade + trajectory +      │
                       │  attendance + co-scholastic + holistic + PEWS focus.      │
                       │  NO LLM. Owns every number/grade/name (LAW 6).            │
                       └───────────────┬─────────────────────────────────────────┘
                                       │ grounded report-fact bundle per student
                       ┌───────────────▼─────────────────────────────────────────┐
                       │  TIER 1 — TRIAGE / COHORT-DEDUP (cheap CLASSIFY, batched) │
                       │  Bucket each student's per-subject movement into patterns │
                       │  (improved / steady / slid / volatile). Cluster the       │
                       │  CLASS-LEVEL boilerplate ("9-B Science +8% this term")    │
                       │  so it is reasoned ONCE, not per child.                   │
                       └───────────────┬─────────────────────────────────────────┘
                                       │ per-student deltas + shared cohort context
                       ┌───────────────▼─────────────────────────────────────────┐
                       │  TIER 2 — NARRATOR AGENT (REASON model + TOOLS)           │
                       │  Per student, a tool-using agent that can:                │
                       │   • get_last_term_report(code)   ← memory / continuity    │
                       │   • get_subject_trajectory(code) ← per-subject slope      │
                       │   • get_pews_context(code)       ← risk + recommended focus│
                       │   • get_exam_topic_gaps(class,subject) ← is it class-wide? │
                       │   • get_board_rubric(board,class)← grading scale/descriptors│
                       │   • get_competency_badges(code)  ← strengths evidence      │
                       │  Produces a STRUCTURED REPORT DRAFT (not prose):          │
                       │   {subject_narratives[], overall_summary, focus_areas[],  │
                       │    projection{band,confidence}, parent_summary{lang,body}}│
                       └───────────────┬─────────────────────────────────────────┘
                                       │ structured draft, GROUNDING-GUARDED
                       ┌───────────────▼─────────────────────────────────────────┐
                       │  TIER 3 — ASSEMBLE & PUBLISH (deterministic orchestration)│
                       │  Render into the board-correct HPC template; write        │
                       │  report_card_drafts (status=draft); enqueue TEACHER review;│
                       │  on bulk-approve → ADMIN publish → reuse results-publish + │
                       │  Notify parents (existing spine). NEVER auto-published.   │
                       └───────────────┬─────────────────────────────────────────┘
                                       │
                       ┌───────────────▼─────────────────────────────────────────┐
                       │  TIER 4 — LEARN (the flywheel)                            │
                       │  Next term: did the projected band hold? Which focus      │
                       │  areas actually moved marks? Re-weight projection model + │
                       │  focus-area recommendations per school. Feeds PEWS too.   │
                       └─────────────────────────────────────────────────────────┘
```

### Why the tiering matters (this fixes the v1 ~6,400-call meltdown)
- **Tier 0 is pure SQL/Kotlin** — it produces *every grade and number*. The LLM is never asked "what was the mark"; it is only asked "phrase this grounded fact specifically and kindly." This is the LAW-6 firewall.
- **Tier 1 cohort-dedup** collapses the class-shared boilerplate: 40 students in 9-B who all rode the same "Science +8% after the new lab" wave share **one** reasoned class-context paragraph; only their *individual* deltas get a per-child sentence. This is the anti-generic mechanism *and* the cost saver.
- **Tier 2 runs through the async `ai_jobs` queue with bounded concurrency** (`AI_BATCH_CONCURRENCY`), so `POST /report-cards/batch-generate` returns in <1s ("queued"), and 800 students are narrated in the background across 7 failing-over providers — never 6,400 blocking calls on one rate-limited model.
- **Per-student cache by fact-bundle hash** (the existing `ai_response_cache` keyed on the Tier-0 bundle): regenerating one child after a marks correction only re-narrates that child.

### Why tool-use matters (this is what makes it *agentic*, not a mail-merge)
v1 pastes the same `{{var}}` skeleton per subject. The Tier-2 agent **pulls context**: it reads *last term's report* (so it never tells an improving child to "work harder"), reads the *PEWS recommended focus* (so the report and the app agree), reads the *exam topic-gap analysis* (so it can say "the whole class found ratios hard — Aarav's dip is partly that, and here's the one thing to practise" instead of blaming the child), and reads the *board rubric* (so the grade descriptor is compliant). **Two children with the same 64% but different histories get different, correct, specific narratives.** No template paste.

---

## 5. TIER 0 — the deterministic Rollup layer (free, the LAW-6 firewall)

Reuse PEWS's z-score/slope engine; emit a **report-fact bundle** per student that owns every number. The LLM never sees a fact that isn't in this bundle, and the grounding guard later verifies the narrative against it.

### 5.1 The report-fact bundle (deterministic, per student per term)
```jsonc
{
  "student_code": "G9B-014-2",
  "student_name": "Aarav Sharma",
  "class": "9", "section": "B", "term": "term2", "academic_year": "2025-26",
  "subjects": [
    {
      "subject": "Mathematics", "marks": 58, "max": 100, "pct": 58,
      "grade": "C1",                         // from board rubric (Tier-0 deterministic map)
      "prev_term_pct": 71, "delta": -13,     // term-over-term (real rows)
      "slope": -0.42,                        // PEWS slope math over the term's assessments
      "rank_band": "middle",                 // deterministic, NOT a fabricated rank number
      "topic_gap_flag": true                 // class-wide gap from exam analysis (if present)
    }
    // ... one per subject, ALL from assessment_marks + assessments
  ],
  "attendance_pct": 79, "attendance_slope": -0.3,
  "co_scholastic": [ {"dimension": "Sports", "grade": "A"}, ... ],   // NEP HPC, if present
  "holistic": [ {"area": "Teamwork", "descriptor": "Consistently collaborative"} ],
  "competencies": ["Critical Thinking"],     // parent_achievements badges (real)
  "pews_context": {                          // from pews_risk_snapshots (coherence!)
     "flagged": true, "cause_family": "academic",
     "recommended_focus": "Mathematics — foundational ratios"
  },
  "data_confidence": 0.86,                    // completeness × signal agreement (LAW 6 honesty)
  "projection": {                             // DETERMINISTIC, not an LLM guess
     "method": "trajectory+attendance",
     "likely_band": "C1–B2", "confidence": 0.7,
     "basis": ["Math slope -0.42 over 4 assessments", "attendance 79% sliding"]
  }
}
```

> **Why the deterministic projection (not an LLM guess):** v1's `{"likely_grade": "B"}` is a hallucination waiting to land on a permanent document. Here, the **next-term band is computed** from the trajectory + attendance (the same predictive math PEWS uses for `leading_score`), with an explicit `basis[]`. The LLM only *phrases* it ("Aarav is on track to move from C1 toward a low B if attendance steadies") — it can never invent a band the numbers don't support. The grounding guard enforces this.

### 5.2 Grade & descriptor mapping is deterministic and board-aware
The marks→grade→descriptor map (CBSE 9-point, ICSE %, State, NEP HPC descriptor bands) is a **pure Kotlin/SQL lookup** keyed on `schools.board` + class. The LLM never decides a grade. (This also kills the v1 compliance risk.)

### 5.3 Data-confidence + honest empty states (LAW 6)
A child with only one assessment this term has `data_confidence` low → the report renders an **honest "limited data this term"** band (existing `VStateHost` 3-state pattern), never a confident fabricated narrative. No marks at all → no AI narrative, just the empty state.

**Cost: ₹0. Pure Kotlin/SQL over tables we already have. This is the layer that makes the AI safe to put on a legal document.**

---

## 6. TIER 1 & 2 — the agentic core (the real differentiator)

### 6.1 Tier-1 Triage / cohort-dedup (cheap CLASSIFY, batched)
For each class, bucket per-subject movement into `{improved, steady, slid, volatile}` (deterministic from slopes), then ask **one cheap CLASSIFY call per class-subject cluster** to phrase the **shared class context** ("9-B Mathematics dipped class-wide this term — the ratios unit was hard"). This shared paragraph is cached and injected into every affected child's Tier-2 prompt as *context*, so the agent spends its REASON budget only on the **child-specific** sentence. This directly attacks the "same paragraph to everyone" problem and slashes cost.

### 6.2 The Narrator tools (all over existing tables/services, read-only, school-scoped)
Mirrors `CaseworkerTools` exactly — `schoolId` is injected from JWT context, never from the model; every query is SELECT-only.

| Tool | Backed by | Returns | Why it kills genericness |
|---|---|---|---|
| `get_last_term_report(code)` | `report_card_drafts` (the agent's memory) | last term's narrative + projection | continuity — never contradict last term; reward real improvement |
| `get_subject_trajectory(code, subject)` | `assessment_marks`/`assessments` (PEWS slope) | per-subject term-over-term slope + points | "improving" vs "low" is the difference between encouragement and a wrong scolding |
| `get_pews_context(code)` | `pews_risk_snapshots` | risk, cause_family, recommended_focus | report ⇄ app coherence; seeds focus area |
| `get_exam_topic_gaps(class, subject)` | exam-analysis (Feature #3) / per-question marks | "18/40 missed ratios" | attributes a dip to a class-wide gap, not the child |
| `get_board_rubric(board, class)` | deterministic rubric map | grade scale + descriptor language | board-compliant descriptors, no compliance risk |
| `get_competency_badges(code)` | `parent_achievements` (COMPETENCY/EI) | earned competencies | a *specific, evidenced* strength, not "good student" |

### 6.3 The structured Report Draft (replaces the per-subject mail-merge)
```jsonc
{
  "subject_narratives": [
    {"subject": "Mathematics",
     "comment": "Aarav slipped from 71% to 58%, mostly on the ratios unit the whole class found hard. His earlier algebra work shows he can recover quickly — 15 minutes of ratio practice over the break will rebuild the foundation.",
     "grade": "C1"}            // grade copied from Tier-0, NOT invented
  ],
  "overall_summary": "A capable student whose confidence dipped in one tough unit; strong in language subjects and a consistent team player.",
  "focus_areas": [             // seeded by PEWS recommended_focus + topic gaps
    {"area": "Mathematics — ratios & proportion", "why": "class-wide gap + term dip", "action": "daily 15-min practice set"}
  ],
  "projection": {              // phrased FROM the deterministic Tier-0 projection
    "text": "On track to move back toward a low B next term if attendance steadies and ratios are revised.",
    "likely_band": "C1–B2", "confidence": 0.7
  },
  "parent_summary": {          // vernacular, warm, non-clinical — one-tap parent view
    "language": "hi",
    "tone": "warm, encouraging, concrete",
    "body": "नमस्ते! आरव इस सत्र में भाषा विषयों में बहुत अच्छा कर रहा है। गणित में अनुपात वाला भाग पूरी कक्षा को कठिन लगा — छुट्टियों में रोज़ 15 मिनट अभ्यास से यह आसानी से सुधर जाएगा। [पढ़ाई देखें] [शिक्षक से बात करें]"
  }
}
```

> **This is the anti-generic mechanism.** Every sentence is conditioned on history (last-term tool), on movement (trajectory tool), on whether the dip is class-wide (topic-gap tool), and on the board's language (rubric tool). Two children with the same 64% but different stories get **different, true** narratives. No template paste.

### 6.4 Structured output + grounding guard (LAW 6 enforced by code, not hope)
- Request **JSON mode**; validate against the Report Draft schema; on parse failure, one repair retry, else **degrade to a deterministic report** (grades + numbers, no AI prose — still a valid report card).
- **Reuse `GroundingGuard.verify(...)`** (already built for PEWS): every number/grade/name in the narrative must exist in the Tier-0 fact bundle. Any invented figure → that sentence is dropped and replaced by the deterministic descriptor. **On a permanent legal document, this is the line that lets an admin publish without re-reading 800 comments.**

---

## 7. TIER 3 — ASSEMBLE & PUBLISH: the human review loop (never auto-published)

This is the cross-role spine. **Nothing is ever published without a teacher review and an admin publish** — exactly the existing results-publish discipline.

| Step | Who | What | Reuses |
|---|---|---|---|
| Generate batch | Admin/Teacher triggers | `POST /report-cards/batch-generate` → returns `ai_jobs.id` in <1s | `ai_jobs` queue |
| Draft fills | (async) | Tier 0→1→2 fills `report_card_drafts.status='draft'` per student | `ai_jobs`, gateway |
| Review queue | **Teacher** | sees per-student drafts; edit any sentence; **regenerate one**; **bulk-approve** the clean ones | new review screen |
| Approve | **Teacher** | `status='approved'`, `edited_by` set if touched | `report_card_drafts` |
| Publish | **Admin** | `status='published'` → renders board template → notifies parents | **existing results-publish + `Notify.toUser(category="report")`** |
| Read | **Parent** | reads narrative + projection + vernacular summary on the Report tab (fills the existing `AiReportCardPreview` honest placeholder) | existing parent academics screen |

### Escalation / safety rails (deterministic)
```
draft ──(teacher edits)──► edited (is_edited=true; audit who/what)
      ──(teacher bulk-approve)──► approved
      ──(admin publish)──► published → parent notified (existing trigger)
      ──(grounding guard dropped a field)──► flagged_for_review (teacher must look)
      NEVER: draft ──► published   (no auto-publish path exists)
```

---

## 8. TIER 4 — LEARN: the flywheel that compounds

v1 has no feedback. v2 closes the loop, reusing PEWS's `LearnService` pattern.

### 8.1 Did the projection hold?
Next term, compare the **deterministic projected band** to the **actual band**. Track projection accuracy per school → calibrate the projection model's confidence (and surface "projection accuracy 82%" honestly).

### 8.2 Which focus areas actually moved marks?
For each `focus_area` recommended, measure next term's delta in that subject. Maintain per school:
```
focus_area_type × subject → {n_recommended, n_improved, avg_delta}
```
This feeds back into **which focus the Narrator recommends first** (evidence-based for THIS school) — and is **shared with PEWS** (the report's focus and PEWS's recommended focus reinforce each other; the flywheel is one system, not two).

### 8.3 The admin "Reporting Effectiveness" view becomes prescriptive
Instead of "800 reports published," the admin sees: **"Projected bands held for 82% of students; 'daily practice set' focus areas improved Math marks +6 on average here, 'extra worksheets' did not — recommend the former."**

---

## 9. The multi-provider optimization — one smart router across ALL AI features

> **This is the section you specifically asked for: a system that optimizes the multi-provider setup and smartly uses every provider across every AI feature.** The good news: the optimal place for this is **the gateway that already exists** (`AiService` + `KeyVault` + `CircuitBreaker`). Report Card 2.0 must *use and sharpen* it, not bypass it with its own calls.

### 9.1 The 7 providers and what each is actually good for (June 2026, verified in `KeyVault.kt`)
| Provider | `noTraining` (PII-safe?) | Free-tier reality | Sweet spot in our workloads |
|---|---|---|---|
| **GROQ** (70B) | ✅ yes | ~30 RPM, ~14.4k RPD, best throughput | **default REASON** — narrative quality at volume |
| **GROQ_FAST** (8B) | ✅ yes | ~14.4k RPM, 500k TPM | **FAST_CHAT / CLASSIFY** — Tier-1 triage, cohort-context phrasing |
| **CEREBRAS** (gpt-oss-120b) | ✅ yes | 1M tok/day, 5 RPM | secondary fast/reason; same gpt-oss weights as Groq → clean failover |
| **GEMINI** (2.5-flash) | ⚠️ no (trains) | 15 RPM, 1M TPM, 1.5k RPD | **non-PII batch REASON** — huge TPM for class-context/boilerplate |
| **SAMBANOVA** (DeepSeek) | ⚠️ no (trains) | 20 RPD (low) | **non-PII frontier reasoning** — hardest summaries, redacted only |
| **MISTRAL** (small) | ⚠️ no (trains) | ~1B tok/month, 1 RPS | **non-PII high-volume BATCH** — the boilerplate paragraphs |
| **OPENROUTER** | ✅ yes | 50 RPD (1k w/ $10) | **last-resort failover** + A/B model testing |

### 9.2 The lane table (already in `AiService.laneProviders`, June-2026 tuned) — Report Card maps onto it
| Report Card workload | Lane | PII? | Provider order (PII-filtered at call time) |
|---|---|---|---|
| Tier-1 class-context / triage phrasing | `CLASSIFY` | ❌ no PII (aggregate class stats) | GROQ_FAST → GROQ → CEREBRAS → **GEMINI/MISTRAL allowed** → OPENROUTER |
| Tier-2 per-child narrative (PII: names+marks) | `REASON` | ✅ **PII** | GROQ → CEREBRAS → OPENROUTER *(Gemini/Samba/Mistral DROPPED by guardrail)* |
| Bulk boilerplate / non-PII summary | `BATCH` | ❌ no PII (redacted) | GROQ → GEMINI → MISTRAL → OPENROUTER |
| Vernacular parent draft (PII) | `REASON` | ✅ **PII** | GROQ → CEREBRAS → OPENROUTER |

> **The smart bit:** `GuardrailService.filterProvidersForPii(...)` removes every `noTraining=false` provider (Gemini, SambaNova, Mistral) the instant a prompt carries a child's name+marks. So the **same lane definition** safely serves both the PII per-child narrative (collapses to Groq→Cerebras→OpenRouter) and the non-PII class boilerplate (can use the high-TPM Gemini/Mistral). One router, two privacy postures, zero per-feature special-casing.

### 9.3 Five optimizations Report Card must turn on (mostly already in the gateway)
1. **PII-redact, don't PII-restrict, the bulk work.** The *class-shared boilerplate* ("9-B Science rose this term") has **no PII** — route it to the high-TPM training-OK providers (Gemini 1M TPM, Mistral 1B/month). Only the *per-child sentence with name+marks* is PII and pinned to the no-training subset. This frees the scarce no-training capacity for the work that truly needs it. **(Lever: feature passes `containsPii=false` for cohort context, `true` for per-child.)**
2. **Jittered diversification across closed circuits** — already in `AiService.jitterCandidates`: shuffle providers whose circuit is CLOSED so 800 concurrent narrations don't all hammer Groq's RPM. **(Already live; Report Card benefits for free.)**
3. **Cohort-dedup before spending REASON** — Tier-1 collapses identical patterns; the L1 cache (`ai_response_cache`, fact-bundle hash) means a re-run after a marks fix re-narrates one child, not the class.
4. **Async `ai_jobs` queue with bounded concurrency** — `AI_BATCH_CONCURRENCY` caps in-flight calls so we ride under every provider's RPM while still draining 800 students in the background; `POST /batch-generate` returns immediately.
5. **Circuit-breaker + cross-feature health sharing** — `ai_provider_health` is global. If PEWS's nightly run just tripped Groq's circuit, Report Card's batch automatically routes around it (and vice-versa). **The provider health learned by one feature optimizes every other feature** — this is the "smartly use all providers across all AI features" property, and it is a *property of using the shared gateway* rather than per-feature clients.

### 9.4 Per-feature provider preference (a small, optional sharpening)
`ai_prompt_templates` already has `pii_allowed_providers` and `traffic_weight`. We add an **optional per-template preferred-lane hint** so, e.g., the *narrative-quality* template can prefer the 70B Groq / DeepSeek tier while the *parent-draft translation* template prefers a fast model — **without** bypassing the failover chain. The router treats it as a re-ordering hint on top of health + PII filtering, never as a hard pin (so a dead "preferred" provider still fails over). This is the one new gateway knob; everything else in §9 is already built.

---

## 10. Cross-role workflow — how the three users are integrated (the heart of the ask)

Every arrow reuses existing infra. The report card is the one artifact all three roles touch in sequence.

```
   TERM CLOSE (admin sets term window, or scheduled job at term boundary)
        │
        ▼
   ┌──────────────────────────────────────────────────────────────────┐
   │  ASYNC BATCH (ai_jobs):  Tier0 Rollup → Tier1 Triage → Tier2 Narrator │
   │  per student → report_card_drafts(status='draft'), grounding-guarded │
   └───────────────┬──────────────────────────────────────────────────┘
                   │  Notify.toUser(category="report", "drafts ready") → TEACHER
   ┌───────────────▼────────────┐      ┌────────────────────────┐      ┌─────────────────────────┐
   │  TEACHER (the doer)         │      │  ADMIN (the publisher)  │      │  PARENT (the keeper)      │
   ├─────────────────────────────┤      ├────────────────────────┤      ├──────────────────────────┤
   │ • Review queue (own classes │      │ • Oversight: which      │      │ • Reads the published     │
   │   only — requireTeacher     │      │   classes are reviewed  │      │   report on the Report tab│
   │   Context + owned assignment)│      │ • One-tap PUBLISH (per  │      │   (fills AiReportCardPreview│
   │ • Edit any sentence          │ ───► │   class) → reuses        │ ───► │   honest placeholder)     │
   │ • Regenerate one student     │ appr │   results-publish path   │ publ │ • Vernacular summary +    │
   │ • Bulk-approve clean drafts  │ oved │ • Sees grounding-flagged │ ish  │   focus area + projection │
   │ • Vernacular parent summary  │      │   drafts that need a look│      │ • [View attendance]       │
   │   auto-drafted (edit before  │      │ • Reporting-Effectiveness│      │   [Message teacher]       │
   │   approve)                   │      │   (prescriptive, Tier-4) │      │   [See PEWS focus]        │
   └───────────────┬─────────────┘      └────────────┬───────────┘      └──────────────┬───────────┘
                   │                                  │                                  │
                   └──────── Notify.toUser(category="report") ◄──────────────────────────┘
                              (FCM + in-app bell, existing spine)        parent opens → read receipt
                                              │
                              ┌───────────────▼──────────────────────┐
                              │  TIER 4 LEARN: next term, did the      │
                              │  projection hold? did the focus move   │
                              │  marks? → prescriptive admin view +    │
                              │  feeds PEWS focus flywheel             │
                              └────────────────────────────────────────┘
```

### Who sees what (scoping rules, enforced from JWT — never request body)
| Role | Scope guard (existing) | Sees | Writes |
|---|---|---|---|
| **Teacher** | `requireTeacherContext()` + `requireOwnedAssignment()` | **only own class/section** drafts + AI narratives + the grounded facts behind them | edit narratives, regenerate one, bulk-approve, edit parent draft |
| **School Admin** | `requireSchoolAdmin()` | whole-school review status, grounding-flagged drafts, Reporting-Effectiveness, board template config | trigger batch, **publish** (per class), set term window/template |
| **Parent** | child-scoped (`parent_child_links` / `children.student_code`) | **own child only**, published report: narrative + grade + projection + vernacular summary + focus | nothing (read-only) + tap actions to existing screens |

### Communication channels (all already in the codebase)
- **Drafts-ready / published alerts** → `Notify.toUser(category="report", deepLink=…)` → FCM + in-app bell (same spine PEWS + Parent Pulse use).
- **Parent summary** is **read-only on publish** — but each report carries one-tap actions into existing screens (`[View attendance]`, `[Message teacher]` via `MessagesRouting`, `[See focus]` into the PEWS focus). **The parent draft is never auto-sent as a message** — it is *part of the published report*, and the teacher reviewed it.

> **Honesty law (LAW 6) upheld end-to-end:** Tier-0 deterministic SQL owns every grade/number/name; the agent only *phrases* the provided bundle; the GroundingGuard drops anything ungrounded; the projection is computed, not guessed; empty data → honest empty state; nothing reaches a parent without a teacher review and an admin publish.

---

## 11. The other-portal features that integrate with Report Card (the ecosystem)

Report Card is both a **signal sink** (it consumes the school's data moat) and a **signal source** (its focus areas and projections feed other agents). You asked which features on each portal integrate — here is the map.

### 11.1 Already in the codebase — wire them in
| Module | Role for Report Card | Integration |
|---|---|---|
| **Assessments / Marks** | the academic core | Tier-0 rollup; on marks-publish, mark the term's drafts stale → offer regenerate |
| **Attendance** | engagement line + projection input | Tier-0; attendance slope feeds the deterministic projection |
| **PEWS** | risk + recommended-focus → report focus; report focus → PEWS next run | bidirectional flywheel (`get_pews_context` tool + shared focus priors) |
| **Teacher Copilot / Exam Analysis** (Feature #3) | class-wide topic gaps | `get_exam_topic_gaps` tool — "the class found ratios hard," not "the child is weak" |
| **NEP HPC** (holistic/co-scholastic) | dimensions beyond marks | Tier-0 reads `holistic_assessments`, `co_scholastic_records` |
| **Competency badges** (`parent_achievements`) | evidenced strengths section | `get_competency_badges` tool |
| **Notifications** | the alert spine | drafts-ready + published alerts |
| **Messages** | parent ↔ teacher follow-up | the `[Message teacher]` action on the published report |
| **Parent Pulse** | weekly digest | coordinate so "report published" isn't double-notified; Pulse can surface "report is out" |
| **Multi-language** (`languagePref`, MULTI_LANGUAGE_SPEC) | vernacular parent summary | language routing for the parent draft |
| **School Branding Kit** | board-correct, branded PDF | the published HPC renders with the school's template/logo |

### 11.2 New sibling AI features that extend the same gateway + pattern
Build once (the tiered tool-agent), apply many — these reuse Report Card 2.0's machinery:
1. **Term Pattern Agent (admin)** — the cohort path surfaced school-wide: "9-B Math slid for 60% of students this term — likely the ratios unit / teacher change." (Same cohort-dedup tier, admin-only, framed as support.)
2. **Parent Conference Pack** — for PTM, assemble each child's report + PEWS context + attendance into a one-page talking-points sheet for the teacher (a tool-using agent over the same bundle).
3. **Promotion / Stream-Recommendation Agent** — end-of-year, project subject fit (Science vs Commerce vs Arts) from the multi-term trajectory; advisory, teacher-reviewed.
4. **Counsellor handoff** — for a child whose report + PEWS both flag a slide, auto-assemble the case (reuses PEWS Counsellor Copilot from `PEWS_2.0` §10.2).

### 11.3 The shared flywheel (why this is a moat, not a feature)
Report Card focus areas → measured next-term deltas → effectiveness priors → better focus recommendations *and* better PEWS interventions. PEWS risk → report focus → report projection accuracy → re-calibrated risk weights. **The two agents share one flywheel and one provider-health brain.** Across many schools (anonymized, opt-in) the "what focus actually moves marks here" library becomes proprietary knowledge no day-one competitor has. *That* is the ₹100Cr asset — not the LLM, which everyone rents.

---

## 12. Database design (the agent's memory — minimal new tables)

Production runs `AUTO_CREATE_TABLES=OFF`; each table ships a migration under `docs/db/` applied **before** the deploy, with the Exposed `object` in lock-step (same discipline as `migration_060/061`). Append to the existing chain.

### 12.1 NEW — `report_card_drafts` (the core memory + review state)
```sql
CREATE TABLE IF NOT EXISTS report_card_drafts (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id          UUID NOT NULL,
    student_code       TEXT NOT NULL,
    academic_year_id   UUID,
    term               VARCHAR(16) NOT NULL,           -- term1|term2|annual
    -- Tier-0 grounded facts (the LAW-6 source of truth for the grounding guard)
    fact_bundle_json   TEXT NOT NULL,                  -- the deterministic rollup
    fact_hash          VARCHAR(64) NOT NULL,           -- cache key (re-narrate only on change)
    -- Tier-2 AI draft
    subject_narratives_json TEXT,                      -- [{subject, comment, grade}]
    overall_summary    TEXT,
    focus_areas_json   TEXT,                           -- [{area, why, action}]
    projection_json    TEXT,                           -- {likely_band, confidence, basis[]}  (deterministic)
    parent_summary     TEXT,                           -- vernacular body
    parent_language    VARCHAR(8),
    -- provenance + grounding
    ai_provider_used   VARCHAR(32),
    ai_model_used      VARCHAR(64),
    grounded           BOOLEAN NOT NULL DEFAULT true,   -- false ⇒ guard dropped a field
    -- review/publish state machine
    status             VARCHAR(16) NOT NULL DEFAULT 'draft', -- draft|approved|published|flagged_for_review
    is_edited          BOOLEAN NOT NULL DEFAULT false,
    edited_by          UUID,
    approved_by        UUID,
    published_at       TIMESTAMP,
    created_at         TIMESTAMP NOT NULL DEFAULT now(),
    updated_at         TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (school_id, student_code, academic_year_id, term)
);
CREATE INDEX IF NOT EXISTS idx_rcd_review ON report_card_drafts (school_id, term, status);
```

### 12.2 NEW — `report_focus_effectiveness` (Tier-4 flywheel priors)
```sql
CREATE TABLE IF NOT EXISTS report_focus_effectiveness (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id          UUID NOT NULL,
    subject            TEXT NOT NULL,
    focus_area_type    VARCHAR(48) NOT NULL,           -- e.g. daily_practice|remedial|peer_pairing
    n_recommended      INTEGER NOT NULL DEFAULT 0,
    n_improved         INTEGER NOT NULL DEFAULT 0,
    avg_delta          DOUBLE PRECISION,
    projection_n       INTEGER NOT NULL DEFAULT 0,     -- projection-accuracy tracking
    projection_hit     INTEGER NOT NULL DEFAULT 0,
    updated_at         TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (school_id, subject, focus_area_type)
);
```

### 12.3 REUSED (no new table) — gateway + NEP + PEWS
- `ai_jobs` (batch), `ai_response_cache` (fact-hash cache), `ai_usage_log` (one row/call), `ai_prompt_templates` (versioned prompts + `pii_allowed_providers` + optional preferred-lane hint), `ai_provider_config/health`.
- NEP: `report_card_templates`, `holistic_assessments`, `co_scholastic_records` (add per `NEP_COMPLIANCE_SPEC.md` if not present — Tier-0 reads them).
- PEWS: `pews_risk_snapshots` (read by `get_pews_context`), shared focus priors.
- `report_cards` (the final published, board-rendered artifact) — assembled at publish; `report_card_drafts` is the working/review layer.

> The v1 spec's `ai_report_card_narratives` is **superseded** by `report_card_drafts` (which adds the fact-bundle, grounding flag, projection, parent language, and the full review state machine v1 lacked).

---

## 13. Backend & client file map (for the implementation PRs)

**New (server) — mirrors `feature/pews/` structure:**
- `db/Tables.kt` (+2 objects: `ReportCardDraftsTable`, `ReportFocusEffectivenessTable`) + register in `DatabaseFactory.allTables`
- `docs/db/migration_06X_report_card.sql`
- `feature/reportcard/rollup/ReportRollupService.kt` (Tier-0, deterministic, reuses PEWS slope util)
- `feature/reportcard/triage/ReportTriageService.kt` (Tier-1, cohort-dedup)
- `feature/reportcard/narrator/NarratorService.kt` + `NarratorTools.kt` (Tier-2, `runAgent` + 6 tools; reuse `GroundingGuard`)
- `feature/reportcard/assemble/ReportAssemblyService.kt` (Tier-3, draft→review→publish; reuse results-publish + `Notify`)
- `feature/reportcard/learn/ReportLearnService.kt` (Tier-4 flywheel)
- `feature/reportcard/ReportCardJob.kt` (term-close scheduled trigger; `ai_jobs` worker)
- `feature/reportcard/ReportCardRouting.kt` (school/teacher/parent routes)
- `feature/reportcard/core/` — reuse PEWS `KillSwitchGuard`/`ModuleRegistry`/`AuditLogger` under module name `reportcard`

**Touched (server):**
- `Application.kt` (wire `ReportCardJob.start(...)`, `reportCardRouting()`)
- `feature/ai/AiService.kt` (add optional per-template preferred-lane hint — the one §9.4 knob)
- `.env.example` (document `REPORTCARD_ENABLED`, term-window vars)

**New (composeApp) — reuse `VStateHost` + V* tokens:**
- Teacher: `ui/v2/screens/teacher/ReportReviewQueueScreen.kt`, `ReportDraftEditorScreen.kt`
- Admin: `ui/v2/screens/school/ReportPublishScreen.kt`, `ReportingEffectivenessScreen.kt`
- Parent: replace `AiReportCardPreview.kt` placeholder with the real `ParentReportScreen.kt` (renders only published, grounded content)

---

## 14. Build sequence (each phase shippable, grounded, committed)

> Ordered by **value ÷ effort**. Tier-0 first (it makes the AI safe), then the agent, then the human loop, then the flywheel. Mirrors the PEWS phase discipline.

### Phase 0 — Tier-0 Rollup + tables (week) — *the LAW-6 firewall, biggest safety jump*
1. `migration_06X` + Exposed objects + register in `allTables`.
2. `ReportRollupService`: deterministic per-student fact bundle (marks→grade via board rubric, trajectory via PEWS slope, attendance, NEP/co-scholastic, PEWS focus, **deterministic projection**).
3. Honest empty/low-confidence states (LAW 6) on the existing parent placeholder.

### Phase 1 — Tier-2 Narrator agent + grounding (week+) — *the agentic leap*
4. `NarratorTools` (6 read-only, school-scoped) + `NarratorService` via `AiService.runAgent`.
5. JSON-mode structured Report Draft + **reuse `GroundingGuard`** (drop ungrounded fields → `flagged_for_review`).
6. Per-student cache by `fact_hash`; PII routing (per-child = PII lane, class context = non-PII lane).

### Phase 2 — Tier-1 cohort-dedup + async batch (week) — *fixes the v1 6,400-call meltdown*
7. `ReportTriageService` cohort-dedup; class-context phrased once (CLASSIFY/non-PII lane → high-TPM providers).
8. `ReportCardJob` + `ai_jobs` worker; `POST /batch-generate` returns immediately; bounded concurrency.

### Phase 3 — Tier-3 review & publish (week) — *the human loop across 3 roles*
9. Teacher review queue: edit / regenerate-one / bulk-approve; vernacular parent-draft edit.
10. Admin publish (per class) → reuse results-publish + `Notify(category="report")`.
11. Parent `ParentReportScreen` (published, grounded content only; one-tap actions).

### Phase 4 — Tier-4 LEARN flywheel (week) — *the compounding moat*
12. Projection-accuracy tracking + focus-area effectiveness priors (`report_focus_effectiveness`).
13. Prescriptive admin Reporting-Effectiveness view; feed focus priors into PEWS.

### Phase 5 — multi-provider sharpening + siblings (parallelizable)
14. Per-template preferred-lane hint (§9.4); verify cross-feature `ai_provider_health` sharing under load.
15. Sibling agents (Term Pattern, Conference Pack) on the same pattern.

---

## 15. Acceptance criteria — "AI Report Card 2.0 is real, not a mail-merge"

- [ ] `POST /report-cards/batch-generate` returns in **<1s** (work queued via `ai_jobs`); 800 students narrate async.
- [ ] No single-provider outage: with one provider rate-limited, narration completes via failover; the run logs which provider answered each draft.
- [ ] **Every grade and number** on the report traces to the Tier-0 deterministic bundle (LAW 6); the **GroundingGuard** drops any invented figure and sets `flagged_for_review`.
- [ ] The next-term projection is **computed deterministically** (with a `basis[]`), never an ungrounded LLM guess; the LLM only phrases it.
- [ ] Two students with the **same marks but different histories** receive **measurably different narratives** (proves tool-conditioning, not templating).
- [ ] Cohort-dedup: a class-wide pattern is reasoned **once**; per-child sentences are unique.
- [ ] **No PII** (name+marks) ever reaches a `noTraining=false` provider (Gemini/SambaNova/Mistral); class-context boilerplate (non-PII) *may* use them to spare the no-training capacity.
- [ ] Teacher can **edit / regenerate-one / bulk-approve**; the **vernacular** parent summary is editable before approve.
- [ ] **Nothing is auto-published**: draft → teacher approve → admin publish → parent notified (existing trigger).
- [ ] Parent reads a published report with narrative + grade + **deterministic projection** + vernacular summary + one-tap actions; the old `AiReportCardPreview` placeholder is replaced with real, grounded content.
- [ ] Next term, projection accuracy and focus-area effectiveness are measured; the admin view is **prescriptive**; focus priors feed PEWS.
- [ ] Every LLM call writes one `ai_usage_log` row; the AI-usage screen shows per-school report tokens + provider health (shared with PEWS).

---

## 16. Risks & guardrails

| Risk | Guardrail |
|---|---|
| Agent fabricates a grade/number **on a permanent legal document** | Tier-0 owns every number; tools are read-only & scoped; **GroundingGuard** drops ungrounded fields → `flagged_for_review`; LAW 6 in UI; **no auto-publish**. |
| 800-student batch melts the server (the v1 ~6,400-call problem) | `ai_jobs` async queue + bounded concurrency + cohort-dedup + fact-hash cache; `POST` returns immediately. |
| PII to a training-opt-in provider | `GuardrailService.filterProvidersForPii` drops Gemini/Samba/Mistral for per-child (name+marks) prompts; only redacted class-context uses them. |
| Wrong/awkward vernacular | Always teacher-reviewed before approve; templates per language; Hindi+English first, expand on demand. |
| Demoralising/contradictory narrative ("work harder" to an improving child) | `get_last_term_report` + `get_subject_trajectory` tools condition the tone; `get_pews_context` keeps report ⇄ app coherent. |
| Board-compliance error | Deterministic board rubric map (not the LLM) owns grade scale + descriptor language; `get_board_rubric` only supplies wording. |
| Single-provider `429` (the live PEWS failure) | Shared 7-provider gateway with circuit breaker + jitter + cross-feature health; Report Card inherits it by *using* `AiService`, never calling a provider directly. |
| Projection over-promises | Deterministic band + confidence + `basis[]`; Tier-4 calibrates accuracy and surfaces it honestly. |

---

## 17. One-paragraph summary for the team

`AI_REPORT_CARD_SPEC.md` (v1) is a correct but generic mail-merge: one fixed-template LLM call per subject (~6,400 sequential calls for an 800-student school — the same blocking-on-one-rate-limited-model failure PEWS 2.0 was built to kill), no memory, no tools, an ungrounded LLM "grade guess" on a permanent legal document, and English-only. **Report Card 2.0 is the PEWS five-tier agentic loop pointed at term narratives:** a deterministic **Tier-0 Rollup** that owns every grade/number and computes the next-term projection (the LAW-6 firewall), a **Tier-1 cohort-dedup** that reasons class-shared boilerplate once on high-TPM non-PII providers, a **Tier-2 Narrator agent** that *uses read-only tools* (last term's report, per-subject trajectory, PEWS focus, class topic-gaps, board rubric, competency badges) to write a **specific, grounded, board-correct, conditioned** narrative — different for every child, never a template paste — guarded by the **same `GroundingGuard`** that drops any invented figure, a **Tier-3 human loop** (teacher edits/bulk-approves, admin publishes, parent reads a vernacular summary — *never auto-published*), and a **Tier-4 flywheel** that measures whether projections held and which focus areas actually moved marks, feeding both itself and PEWS. It runs **async** (fixing the meltdown), **fails over across 7 free providers** through the shared gateway (fixing the 429), routes **PII away from training-opt-in providers automatically**, and is built almost entirely on assets already on this branch (`AiService.runAgent`, `GroundingGuard`, `ai_jobs`, `ai_prompt_templates`, the notification spine, the PEWS signal moat, and the NEP HPC tables). It is the difference between a feature and the coherent, grounded, compounding term-reporting agent a ₹100Cr campus OS is actually sold on.

---

*End of AI Report Card 2.0 redesign brief. Next session: start at §14 Phase 0 (Tier-0 Rollup + tables — the LAW-6 firewall) → Phase 1 (Narrator agent + grounding) → Phase 2 (cohort-dedup + async batch).*

