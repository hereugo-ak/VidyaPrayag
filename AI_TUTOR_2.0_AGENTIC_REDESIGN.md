# AI Tutor 2.0 — Agentic Redesign

### *The "Personal Learning Agent" that turns VidyaPrayag's school data into a 1:1 tutor for every child — and the foundation for the agentic RAG system we build next*

> **Status:** Design / RFC. Supersedes the v1 stub at `newreviewdocs/specs/VIDYASETU_AI_TUTOR_SPEC.md`.
> **Pattern parent:** `PEWS_2.0_AGENTIC_REDESIGN.md` (the five-tier agentic loop) and `AI_REPORT_CARD_2.0_AGENTIC_REDESIGN.md` (grounded, multi-provider, cross-role).
> **One-line:** *Don't ship a chatbot bolted onto an LLM. Ship a tool-using, curriculum-grounded learning agent that knows this child's marks, syllabus position, homework history and weak topics — and is architected so the RAG layer drops in as one more tool, not a rewrite.*

---

## 0. Why this document exists (read this first)

The brief was explicit: *"AI-powered tutoring assistant: Personalized learning recommendations, doubt resolution. Build a next-gen agentic system (scalable — in future we will build an agentic RAG system based on this). Don't just limit yourself around these features. Build an absolute beast of a workflow. Targeting a $100M valuation — these should be of that level, no generic fucked-up [chatbot]. Use the best possible resources, generate the most robust features based on resources we already have, check for free GitHub repos that make the experience better, plan the whole workflow."*

So this is **not** "wrap GPT, stream tokens, store chat history." That is the v1 stub, and it is a commodity — Khanmigo, Socratic, ChatGPT all do it better and they will never lose that race. **The moat is the school's own data.** Khanmigo does not know that *this* child scored 38% on Fractions last term, that her class is currently on Chapter 7 of NCERT Class 8 Maths, that she has missed two homework submissions on this exact topic, and that her teacher flagged "needs reinforcement on word problems." VidyaPrayag knows all of that — it is sitting in `AssessmentMarksTable`, `SyllabusProgressTable`, `HomeworkSubmissionsTable`, `CurriculumUnitsTable` right now.

The beast is: **a learning agent grounded in that data, that reasons with tools, schedules its own follow-ups, and gets smarter every time a child answers a question.** And it is built so the future agentic RAG system (NCERT textbook retrieval, semantic search over the school's own material) plugs in as **one more tool on the same agent loop** — not a parallel system.

This document mirrors the structure of the PEWS 2.0 and Report Card 2.0 redesigns deliberately: same gateway, same tiered loop, same grounding law, same multi-provider router. That consistency *is* the architecture.

---

## 1. The reframe: from "homework chatbot" to "Personal Learning Agent"

| | v1 stub (`VIDYASETU_AI_TUTOR_SPEC.md`) | **AI Tutor 2.0 (this doc)** |
|---|---|---|
| **Mental model** | A chat box that answers questions | A **persistent learning agent** per child that owns a mastery model, a plan, and a memory |
| **Grounding** | System prompt says "use school data" (hope) | **Tools** pull this child's real marks/syllabus/homework; `GroundingGuard` rejects fabricated facts (LAW 6 enforced by code) |
| **Doubt resolution** | Text question → one LLM call | Text **or photo/voice** doubt → OCR/STT → curriculum-aware Socratic loop that *guides*, links to the exact chapter, logs the misconception |
| **Personalization** | A `mastery_level` float updated ad-hoc | **FSRS spaced-repetition engine** (free, JVM-native) drives *what to review when*; adaptive path is computed, not vibes |
| **Practice** | Generate N questions, grade them | Practice is **targeted at the weakest FSRS-due topic**, auto-graded, and the result feeds the mastery model and the teacher dashboard |
| **Who it serves** | "Students" (but no student role exists!) | Parent-on-behalf-of-child today; **dedicated student role** as Phase 1; teacher gets a class heatmap; admin gets efficacy analytics |
| **Cost posture** | 50 calls/student/day, one big model | Tiered: deterministic first, cheap classify, expensive reason only when needed, cached, async-batched |
| **RAG future** | Not addressed | **Architected in**: retrieval is a tool; `tutor_knowledge_chunks` + pgvector table specced; the agent loop never changes when RAG lands |

The shift in one sentence: **the v1 answers questions; the 2.0 manages a child's learning.**

---

## 2. The pain points, named honestly (the "think deep" part)

### 2.1 For the Child / Student (the learner — primary user)
- Generic AI tutors give the *answer*, which kills learning. A real tutor makes you *find* it.
- A child stuck on Q3 of tonight's homework at 9pm has no one to ask. The teacher is asleep; the parent forgot Class-8 algebra.
- "Practice more" is useless advice. Practice **what**, in **what order**, **how much**? A 38%-on-fractions child and a 91% child need totally different next steps.
- Doubts arrive as a **photo of a textbook problem** or a **spoken question in Hinglish**, not a clean typed string. Text-only tutors are a wall.

### 2.2 For the Parent (the sponsor / co-pilot)
- Wants to *see* their child improving, not just pay for a tool. Needs evidence: "she's now 70% on fractions, up from 38%."
- Worried about safety: is the AI giving answers? Saying anything inappropriate? Replacing the teacher?
- Today there is **no student login** — so the parent is the access path. The tutor must work *through* the parent account and respect that.

### 2.3 For the Teacher (the authority — must stay in the loop)
- Fears the AI undermines them ("it told my kid X, but I teach it as Y").
- Wants the *opposite*: an AI that reinforces **their** syllabus position and flags which kids are struggling on the topic **they** just taught — before the test, not after.
- Has zero time. Will not configure anything. Needs a class heatmap that appears for free.

### 2.4 For the School Admin / Platform
- Needs this to be a **differentiator that sells the platform**, not a cost centre that burns tokens.
- Needs efficacy proof: "schools using the tutor improved X% on weak-topic re-assessment." That is the $100M story.
- Needs it **safe by construction** — PII-routing, no fabrication, human override — because one bad incident with a child kills the brand.

---

## 3. Resources we already have (so we build on them, not around them)

### 3.1 The AI Gateway — already a real, agentic asset (`feature/ai/`)
The single most important fact: **we do not need to build an agent framework. We have one.**
- `AiService` — the choke point for all AI. Has `runAgent()`: a real **tool-calling agent loop** (model emits tool calls → we execute → feed results back → repeat → final answer).
- `LlmClient` — OpenAI-compatible, supports `tools` + `tool_choice`.
- `KeyVault` — AES-256-GCM encrypted keys for **7 providers** (CEREBRAS, GROQ, GROQ_FAST, SAMBANOVA, MISTRAL, OPENROUTER, GEMINI), each tagged with a `noTraining` PII flag.
- `GuardrailService` — PII filtering before egress.
- `CircuitBreaker` — per-provider failure isolation.
- `AiService.laneProviders` — a tuned lane→provider routing table (June 2026).

### 3.2 The PEWS 2.0 / Caseworker agentic pattern — clone it, don't reinvent it (`feature/pews/caseworker/`)
This is the **exact blueprint** for the Tutor agent. It already proves the full pattern in production code:
- `CaseworkerService.kt` — runs the agent via `AiService.runAgent`, parses a **structured** result object, runs `GroundingGuard.verify`, falls back to a deterministic result if the model misbehaves.
- `CaseworkerTools.kt` — **6 read-only, school-scoped tools**. `schoolId` is injected from the JWT, never accepted from the model. All `SELECT`-only. Each returns JSON. Schemas built with `buildJsonObject`.
- `GroundingGuard.kt` — verifies every name/number in AI output traces back to a deterministic bundle; strips or rejects anything that doesn't.
- `CaseFile.kt` / `CaseworkerModule.kt` — the structured output contract + module-singleton DI.

**The Tutor is a Caseworker for one child's learning.** Same shape: deterministic bundle → tools → agent reasons → structured output → grounding guard → deterministic fallback.

### 3.3 Data signals already in the DB (verified in `server/.../db/Tables.kt`)
Everything the tutor needs to be *personal* already exists:

| Signal | Table (verified line) | What the tutor does with it |
|---|---|---|
| What was taught & where the class is now | `CurriculumUnitsTable` (1043, chapter▸topic hierarchy), `SyllabusProgressTable` (1063, `isCovered`/`coveredOn`) | Never teach ahead of the class; ground explanations in the *current* chapter |
| This child's actual performance | `AssessmentMarksTable` (969) | Identify weak topics with real numbers, not guesses |
| Homework load & misses | `HomeworkTable` (1094), `HomeworkSubmissionsTable` (1143, `status`, `studentUuid`) | Detect "missed homework on a weak topic" — the highest-value intervention signal |
| Class / subject / teacher structure | `SchoolClassesTable` (269), `SchoolSubjectsTable` (280), `TeacherSubjectAssignmentsTable` (302) | Scope correctly; route teacher insights to the right teacher |
| Child & guardian linkage | `ChildrenTable` (609), `ParentChildLinksTable` (1460) | Resolve "which child" from the parent JWT |
| Calendar / academic year | `CalendarEventsTable` (1583), `AcademicYearsTable` (1631) | "Test on Friday" → ramp practice; term-aware mastery |
| Teacher's lesson intent | `LessonPlansTable` (1738) | Align tutor explanations to how the teacher framed it |

### 3.4 Two real constraints we design around (named honestly)
1. **No student role exists.** `AppUsersTable` (line 55) defaults `role = "parent"`; roles are parent/teacher/admin only. → **Phase 1 adds a `student` role**; until then, the tutor runs under the parent account, scoped to a chosen child.
2. **The AI gateway is text-only today.** No STT / vision / embedding methods. → Doubt-by-photo and doubt-by-voice are **explicit additions** (see §3.5), and embeddings are the RAG addition (see §8).

### 3.5 Free, open-source repos we fold in (zero-licence-cost force multipliers)
Researched and chosen for JVM/KMP fit and permissive licences:

| Capability | Repo / source | Licence | Why it's the right pick |
|---|---|---|---|
| **Spaced repetition / scheduling** | `open-spaced-repetition/FSRS-Kotlin` (FSRS v6) | MIT | JVM-native, no JNI; the modern successor to SM-2/Anki; *this is the mastery engine's spine* |
| **OCR for doubt-photos** | Tesseract (`tesseract-ocr/tesseract`, Apache-2.0, ~73k★) or PaddleOCR for math; fallback = route image to a **vision-capable provider** | Apache-2.0 | Turn a snapped textbook problem into text the agent can reason over |
| **Voice doubts (Hinglish/regional)** | Whisper via **Groq** (already a provider in `KeyVault`) | — (API) | Spoken-question capture with near-zero new infra; PII-safe provider |
| **Curriculum content / RAG seed** | `hatchedland/ncert-mcp` (NCERT/CBSE Grades 7–12 structured REST/MCP), `KadamParth/NCERT_Dataset` (HF), `aayushdutt/ncert-downloader` | MIT/open | Ground explanations in *actual* NCERT text; the corpus that seeds the RAG layer |
| **Math rendering in Compose** | `huarangmeng/latex` (KMP LaTeX) or **KotlinTeX** (`io.github.darriousliu`, Compose Multiplatform) | Apache-2.0/MIT | Render generated equations natively on `composeApp` — no WebView hack |

> Rule for all of these (same as the gateway): they are **tools the agent calls or renderers the client uses** — never a parallel brain. The agent loop is unchanged whether OCR/RAG/voice are present or not.

---

## 4. The AI Tutor 2.0 architecture — a real agent loop with tools

The Tutor is **one persistent agent per (child, subject)** that runs through the same five-tier loop as PEWS, specialised for learning. It never "just calls an LLM" — it senses deterministically, triages cheaply, reasons with tools only when needed, acts deterministically, and learns.

```
                          ┌───────────────────────────────────────────────────────────┐
                          │  TIER 0 — SENSE  (deterministic, free, the LAW-6 firewall)  │
   marks / syllabus /     │  Build the Learner Bundle: weak topics (real %), syllabus   │
   homework / calendar ──▶│  position, missed HW, FSRS-due reviews, upcoming tests.     │
                          │  No LLM. This is ground truth the agent may NOT contradict. │
                          └───────────────────────────────┬───────────────────────────┘
                                                           │ bundle
                          ┌────────────────────────────────▼──────────────────────────┐
                          │  TIER 1 — TRIAGE  (cheap CLASSIFY, GROQ_FAST)               │
   incoming event ───────▶│  What is this? {doubt | practice-request | check-in |       │
   (doubt / nightly /     │  plan-review}. Is it on-syllabus? Is it a known            │
    test-soon / idle)     │  misconception? Cache hit? → decide if Tier 2 is needed.    │
                          └───────────────────────────────┬───────────────────────────┘
                                                           │ (only if reasoning needed)
                          ┌────────────────────────────────▼──────────────────────────┐
                          │  TIER 2 — TUTOR AGENT  (REASON + tools, the differentiator) │
                          │  AiService.runAgent loop with the Tutor toolset:            │
                          │   • getLearnerBundle / getWeakTopics / getSyllabusPosition  │
                          │   • getCurriculumContent (NCERT) / retrieveKnowledge (RAG*) │
                          │   • getDueReviews(FSRS) / getHomeworkContext                │
                          │  → emits a STRUCTURED TutorTurn (Socratic step, hint,       │
                          │    practice set, plan delta) — never a fabricated number.   │
                          └───────────────────────────────┬───────────────────────────┘
                                                           │ structured TutorTurn
                          ┌────────────────────────────────▼──────────────────────────┐
                          │  TIER 3 — ACT  (deterministic)                              │
                          │  GroundingGuard.verify → render (LaTeX) → auto-grade        │
                          │  practice → update FSRS state → write mastery → notify      │
                          │  teacher/parent if thresholds tripped. NEVER auto-"answers". │
                          └───────────────────────────────┬───────────────────────────┘
                                                           │ outcomes
                          ┌────────────────────────────────▼──────────────────────────┐
                          │  TIER 4 — LEARN  (the flywheel)                             │
                          │  Did mastery actually move? Which hints worked? Which       │
                          │  misconceptions recur class-wide? Feed teacher heatmap +     │
                          │  prompt-template priors + provider-quality signals.         │
                          └─────────────────────────────────────────────────────────────┘
                              * RAG tools are inert stubs today; they light up in Phase 5
                                without changing any other tier. THIS is the scalability.
```

### Why the tiering matters (cost + safety, not bureaucracy)
- The v1 budget of **50 LLM calls/student/day** explodes at scale (10k students = 500k calls/day). Tier 0–1 answer most events with **zero or one cheap call**; the expensive REASON model only fires for genuine doubts/plans.
- Every Tier-2 output passes through Tier-3's `GroundingGuard` **before** a child ever sees it. The deterministic layer owns who/what/numbers; the model owns explanation and pedagogy.

### Why tool-use makes it agentic (not a mail-merge or a chatbot)
A chatbot has the child's question and a frozen system prompt. **Our agent can go get what it needs**: pull this child's fraction marks, check whether the class has even covered the chapter yet, fetch the NCERT text for the exact topic, look up which review is due tonight. It decides *which* tools to call. That decision loop is the product.

---

## 5. TIER 0 — the deterministic SENSE layer (free, the LAW-6 firewall)

### 5.1 The Learner Bundle (deterministic, per child per subject)
Computed in plain Kotlin/SQL, **no LLM**, cached. This is the agent's ground truth.

```
LearnerBundle {
  childId, classId, subjectId, academicYearId
  syllabusPosition:   { currentChapter, currentTopic, coveredTopicIds[], notYetCoveredIds[] }   // SyllabusProgressTable
  performance:        { perTopicScore: { topicId -> {pct, attempts, lastAssessedOn} } }          // AssessmentMarksTable
  weakTopics:         [ {topicId, pct, severity} ]   // pct below board threshold, COVERED only
  homeworkContext:    { dueSoon[], missed[], missedOnWeakTopic[] }                                // HomeworkSubmissionsTable
  reviewQueue:        [ {topicId, dueAt, stability, difficulty} ]   // FSRS state, see §6.4
  upcoming:           { tests[], events[] }                          // CalendarEventsTable
  dataConfidence:     { hasMarks, hasSyllabus, hasHomework }         // honest empty states (LAW 6)
}
```

### 5.2 Mastery & "weak" are board-aware and deterministic
- "Weak topic" = covered topic where `pct < board_threshold` (configurable per school/board). The **number comes from `AssessmentMarksTable`**, never from the model.
- The tutor may only target topics that are **already covered** (`isCovered = true`). It must never teach ahead of the teacher. This is a hard rule enforced in Tier 0, not a polite request in a prompt.

### 5.3 Data-confidence + honest empty states (LAW 6)
If a child has no marks yet, the bundle says so and the agent is told to run in **"diagnostic" mode** (gentle placement questions) rather than inventing a weakness. No data → no claim. Ever.

---

## 6. TIER 1 & 2 — the agentic core (the real differentiator)

### 6.1 Tier-1 Triage (cheap CLASSIFY, GROQ_FAST, batched)
Every inbound event is classified before any expensive reasoning:
- **Intent:** `doubt | practice_request | concept_explain | plan_review | check_in`
- **On-syllabus?** (matches `coveredTopicIds`) — off-syllabus doubts get a "we haven't covered this yet, want me to flag it for your teacher?" path instead of teaching ahead.
- **Known misconception?** (matches the misconception library, §8) — skip straight to the proven remediation.
- **Cache hit?** (`ai_response_cache` keyed on fact-hash of bundle+question) — return cached explanation, zero new calls.

Only events that survive triage reach Tier 2.

### 6.2 The Tutor tools (read-only, school-scoped, cloned from `CaseworkerTools.kt`)
All `SELECT`-only, `schoolId`/`childId` injected from JWT (never from the model), JSON return, `buildJsonObject` schemas:

| Tool | Reads | Purpose |
|---|---|---|
| `getLearnerBundle` | Tier-0 bundle | One-shot context (the agent usually starts here) |
| `getWeakTopics` | `AssessmentMarksTable` | Real weak-topic list with real percentages |
| `getSyllabusPosition` | `SyllabusProgressTable`, `CurriculumUnitsTable` | What's been covered; never teach beyond it |
| `getHomeworkContext` | `HomeworkSubmissionsTable` | Missed/due homework, esp. on weak topics |
| `getDueReviews` | `tutor_review_state` (FSRS) | What to review tonight, in order |
| `getCurriculumContent` | NCERT dataset / `ncert-mcp` | Ground the explanation in actual textbook text |
| `retrieveKnowledge` *(RAG, Phase 5)* | `tutor_knowledge_chunks` (pgvector) | Semantic recall over school's own material — **inert stub until Phase 5** |
| `logMisconception` | writes `tutor_misconceptions` | The one write tool — records *what the child got wrong and why* |

### 6.3 The structured `TutorTurn` (replaces the free-text chat blob)
The agent never returns raw prose to the client. It returns a typed object (the `CaseFile` analogue):

```
TutorTurn {
  mode:        SOCRATIC_STEP | HINT | EXPLANATION | PRACTICE_SET | PLAN_UPDATE | ESCALATE
  groundedRefs: [ {topicId, source: MARKS|SYLLABUS|NCERT|RAG, value} ]   // every fact must cite a ref
  studentFacing: { text, mathBlocks[] (LaTeX), nextPrompt }              // what the child sees
  practice:    [ {questionId, stem, options?, answerKey, topicId, difficulty} ]?  // if PRACTICE_SET
  planDelta:   { addReviews[], adjustDifficulty }?                       // if PLAN_UPDATE
  teacherFlag: { topicId, reason, severity }?                            // if a pattern emerges
  misconception: { topicId, type, evidence }?                            // feeds logMisconception
}
```

### 6.4 The doubt-resolution flow (Socratic, multi-modal, grounded)
This is the headline feature, done right:

1. **Capture** — child submits a doubt as **text, photo, or voice**.
   - Photo → **OCR** (Tesseract/PaddleOCR) or a vision-capable provider → problem text.
   - Voice → **Whisper via Groq** → transcript (handles Hinglish).
2. **Triage** (Tier 1) — on-syllabus? known misconception? cache?
3. **Reason** (Tier 2) — agent calls `getSyllabusPosition` (is it covered?), `getCurriculumContent` (the NCERT framing), `getWeakTopics` (does this child historically struggle here?).
4. **Respond Socratically** — `mode = SOCRATIC_STEP`: **guide, don't solve.** "What do you get when you find a common denominator first?" Hints escalate only if the child is stuck; the full worked solution is a *last* resort, gated, and logged.
5. **Render** — math via LaTeX (KotlinTeX) natively in Compose.
6. **Log** — `logMisconception` records the error type. This is gold for Tier 4 and the teacher.

### 6.5 Personalized learning recommendations (the adaptive path, computed not guessed)
- The **FSRS engine** (FSRS-Kotlin) holds per-(child, topic) `stability` + `difficulty` and computes the *due date* of the next review. This is the scheduler, not the LLM.
- Each night (async, Tier 0/1), the deterministic layer assembles `getDueReviews` + weak topics + upcoming tests into a **prioritised plan**; the agent (Tier 2) only writes the *human framing* and picks the practice mix.
- A test on Friday (`CalendarEventsTable`) deterministically **ramps** the relevant topics to the front of the queue. No LLM needed to know a test is soon.

### 6.6 Practice generation + auto-grading (closed loop)
- Practice is **always targeted** at the top FSRS-due / weakest covered topic — never random.
- Generated questions are validated against an answer key the agent must also produce; MCQs are auto-graded deterministically; free-response is graded by the agent with a rubric and the result is **grounded** (it must cite the rubric, not invent a score).
- Every graded answer → FSRS update → mastery write → (if it crosses a threshold) a teacher/parent signal. **The loop closes**: practice changes the plan changes the next practice.

---

## 7. TIER 3 — ACT: deterministic effects + the safety rails

Nothing the model says reaches a child or a record until Tier 3 has run, deterministically:
- **`GroundingGuard.verify`** — every number/name/percentage in `studentFacing` must trace to a `groundedRef`. Ungrounded facts are stripped or the turn is rejected and a deterministic fallback is served.
- **Render** — LaTeX → image/native; sanitise.
- **Auto-grade & FSRS update** — deterministic; write `tutor_review_state`, `tutor_mastery`.
- **Escalation rails (deterministic, never the model's call):**
  - Child asks for the *answer* repeatedly → switch to "let's break it down" + optionally notify parent/teacher; never just hand over solutions.
  - **Safety classifier** trips (self-harm, abuse, distress signals in a doubt) → immediately surface to the school's safeguarding contact and the parent; the AI does **not** counsel. This is a hard, audited path.
  - Off-syllabus or beyond-grade content → decline + offer to flag for the teacher.
- **Notify** — teacher heatmap and parent progress card are updated here, deterministically, from real outcomes.

---

## 8. TIER 4 — LEARN: the flywheel + the RAG on-ramp

### 8.1 Did mastery actually move?
After each re-assessment, compare predicted vs actual mastery. Tutors/topics/hints that *moved marks* get reinforced; ones that didn't get down-weighted. This is the efficacy number that sells the platform.

### 8.2 The misconception library (class-wide intelligence)
`tutor_misconceptions` aggregates *what children get wrong and why*. When 11 kids in a class trip the same misconception on the chapter the teacher just taught, that surfaces on the teacher heatmap **before the test** — the single most valuable thing the system can tell a teacher.

### 8.3 Prompt-template + provider priors
Winning Socratic phrasings and hint ladders are promoted via `ai_prompt_templates` (versioned, `traffic_weight`); provider quality per lane feeds back into the router.

### 8.4 The RAG on-ramp (why this design is "scalable for the future agentic RAG")
The future agentic RAG system is **not a rewrite** — it is two additions to *this* architecture:
1. A populated `tutor_knowledge_chunks` table (pgvector) seeded from the **NCERT corpus** (`ncert-mcp` / HF datasets) + the school's own uploaded notes, chunked + embedded.
2. The already-specced `retrieveKnowledge` **tool** flips from stub to live.
The agent loop, the tiers, the grounding guard, the `TutorTurn` contract — **all unchanged.** The agent simply gains a new tool to call when it needs source material. That is the entire point of building it agentic now: *RAG becomes a capability, not a project.* (Embeddings are the one gateway addition required — `AiService.embed()` — mirroring how STT/vision are added.)

---

## 9. The multi-provider optimization — the same smart router, tutor-tuned

The Tutor rides the existing 7-provider router (`AiService.laneProviders`, `KeyVault.kt`). No new infra — just the right lane per task.

### 9.1 The 7 providers and what each is good for (verified in `KeyVault.kt`)
| Provider | PII-safe (`noTraining`) | Tutor sweet spot |
|---|---|---|
| **GROQ_FAST** | ✅ | Tier-1 triage/classify (cheapest, fastest), Whisper voice transcription |
| **GROQ** | ✅ | Real-time Socratic doubt turns (low latency matters when a child is waiting) |
| **CEREBRAS** | ✅ | Fast reasoning for practice generation + grading |
| **OPENROUTER** | ✅ | Overflow / model diversity, A/B of hint styles |
| **GEMINI** | ⚠️ training-opt-in | **Vision** (photo-doubt OCR fallback), long-context curriculum reasoning — **only on de-PII'd content** |
| **SAMBANOVA** | ⚠️ | High-throughput batch (nightly plan generation) on de-PII'd bundles |
| **MISTRAL** | ⚠️ | Cost-efficient bulk explanation drafting on de-PII'd content |

### 9.2 PII routing is non-negotiable here
A child's name + marks are sensitive. **Anything carrying child PII routes only to PII-safe providers** (Groq/Cerebras/OpenRouter). When we *must* use Gemini vision for a photo, the request is de-identified first (no name, no ID — just the problem image and a topic id). `GuardrailService` enforces this on egress; `ai_prompt_templates.pii_allowed_providers` declares it per template.

### 9.3 Latency lanes (a child is waiting — UX is a feature)
- **Interactive doubt turn:** GROQ first (sub-second), Cerebras fallback. Stream tokens.
- **Nightly plan / batch practice:** SambaNova/Mistral, async via `ai_jobs`, no latency pressure, cheapest tokens.
- **Triage:** GROQ_FAST, always.
This is the difference between "feels like a tutor" and "feels like a form submission."

### 9.4 Five optimizations to turn on (mostly already in the gateway)
1. **Cache** (`ai_response_cache`) — identical doubt + identical bundle-hash → no new call.
2. **Cohort dedup** — 30 kids ask the same chapter-7 doubt → reason once, personalise the framing cheaply.
3. **Async batch** (`ai_jobs`) — all nightly plans generated off-peak on the cheapest provider.
4. **Circuit breaker** — a flaky provider is skipped automatically; the child never sees a 500.
5. **Token budget per lane** — interactive turns are short; batch jobs can be verbose. Enforced, logged in `ai_usage_log`.

---

## 10. Cross-role workflow — how four roles are integrated (the heart of the ask)

The tutor is not a child's private toy. It is a **three-way loop** (child ↔ teacher ↔ parent) with the admin watching efficacy. This integration is the moat — no competitor has the school's roles wired together.

```
   ┌─────────────┐   doubt/photo/voice    ┌──────────────────────┐   grounded TutorTurn   ┌──────────────┐
   │   STUDENT    │ ─────────────────────▶ │  AI TUTOR AGENT      │ ─────────────────────▶ │   STUDENT     │
   │ (or PARENT   │                        │  (Tier 0→4 loop)     │   Socratic, never the  │  learns, not  │
   │  on behalf)  │ ◀───────────────────── │                      │   answer               │  cheats       │
   └─────────────┘   adaptive plan/practice└──────────┬───────────┘                        └──────────────┘
                                                       │ teacherFlag / misconception (Tier 3/4)
                                                       ▼
   ┌─────────────────────────────────────────────────────────────────────────────────────────────────────┐
   │  TEACHER  — class heatmap (appears for free): "11 kids tripped the 'borrowing in subtraction'          │
   │            misconception on Ch.7 — the chapter you covered Tuesday." + one-tap "assign remediation".   │
   │            Teacher can OVERRIDE/CORRECT any tutor framing → that correction trains the prompt-prior.    │
   └─────────────────────────────────────────────────────────────────────────────────────────────────────┘
                                                       │ progress signals (Tier 3)
                                                       ▼
   ┌─────────────────────────────────────────────────────────────────────────────────────────────────────┐
   │  PARENT  — progress card: "Fractions: 38% → 70% over 3 weeks. 12 doubts resolved, 0 answers given."    │
   │            Safety transparency: every session is logged & viewable. Can pause the tutor anytime.        │
   └─────────────────────────────────────────────────────────────────────────────────────────────────────┘
                                                       │ efficacy rollup (Tier 4)
                                                       ▼
   ┌─────────────────────────────────────────────────────────────────────────────────────────────────────┐
   │  ADMIN  — "Tutor Effectiveness" view: weak-topic recovery rate, doubts resolved, teacher hours saved,   │
   │            token cost per improvement point. THE $100M efficacy story, per school & per board.          │
   └─────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

### Who sees what (scoping, enforced from JWT — never request body)
- **Student/Parent:** only their own child's sessions, plan, mastery. `childId` resolved from `ParentChildLinksTable`.
- **Teacher:** aggregate + per-student heatmap **only for classes/subjects in `TeacherSubjectAssignmentsTable`**. No cross-class peeking.
- **Admin:** school-wide aggregates, no individual chat content (privacy by default).
- All scoping injected from the JWT in the tool layer — identical discipline to `CaseworkerTools.kt`.

### Communication channels (all already in the codebase)
Teacher flags, parent progress cards, and "test-soon, practice ramped" nudges all ride the **existing notification/calendar plumbing** — no new channel to build.

---

## 11. The ecosystem — sibling features on the same gateway + pattern

### 11.1 Already in the codebase — wire them in
- **PEWS** — a child the tutor finds chronically stuck on covered topics is an *early-warning signal*; feed it to PEWS. Conversely, a PEWS-flagged at-risk child gets the tutor's gentle diagnostic mode proactively.
- **Report Card 2.0** — the tutor's mastery deltas become a *grounded* "areas of growth" input for the narrative ("improved markedly on fractions this term") — with real numbers the Report Card's own GroundingGuard accepts.
- **Homework / Syllabus** — the tutor is *driven* by these; a missed homework on a weak topic auto-suggests a practice set.

### 11.2 New siblings that extend the same gateway + pattern
- **Parent Co-pilot** — "how do I help my child with fractions at home?" (grounded in the same bundle).
- **Teacher Lesson Assistant** — "11 kids tripped this misconception, here's a 10-min reteach plan."
- **Exam Prep Agent** — test on Friday → a deterministic ramp + agentic mock-test generation.
All clone the Tutor's tier-loop; none is a new architecture.

### 11.3 The shared flywheel (why this is a moat, not a feature)
Every feature writes to the same misconception library, the same mastery model, the same efficacy ledger, the same prompt-priors. The more the school uses *any* AI feature, the smarter *all* of them get. A competitor would have to rebuild the whole data graph to catch up. **That is the $100M defensibility.**

---

## 12. Database design (the agent's memory — minimal new tables)

Reuse everything in §3.3 + the gateway tables. New tables are small, additive, and migration-friendly (mirrors `migration_063_pews2_casework.sql`).

### 12.1 NEW — `student` role + optional `student_accounts` (Phase 1)
Add `student` to the allowed `AppUsersTable.role` set; a child can be issued a login linked via `ParentChildLinksTable`/`ChildrenTable`. Until rolled out, the tutor runs under the parent account scoped to a `childId`. **No schema rewrite — one enum value + a link.**

### 12.2 NEW — `tutor_sessions` (supersedes v1 `ai_tutor_sessions`)
```
tutor_sessions(
  id, school_id, child_id, subject_id, academic_year_id,
  mode,                       -- DOUBT | PRACTICE | CONCEPT | PLAN | DIAGNOSTIC
  intent_class,               -- Tier-1 classification
  turns jsonb,                -- array of grounded TutorTurn objects
  grounded_refs jsonb,        -- audit: every fact + its source
  provider_used, tokens_used, cache_hit bool,
  safety_flag varchar null,   -- if escalation tripped
  created_at, updated_at
)
```

### 12.3 NEW — `tutor_review_state` (the FSRS spine)
```
tutor_review_state(
  id, school_id, child_id, topic_id,
  stability double, difficulty double,   -- FSRS-Kotlin state
  due_at timestamptz, reps int, lapses int,
  last_grade int, last_reviewed_at,
  UNIQUE(child_id, topic_id)
)
```

### 12.4 NEW — `tutor_mastery` (grounded mastery, supersedes v1)
```
tutor_mastery(
  id, school_id, child_id, subject_id, topic_id,
  mastery double,             -- derived from real marks + practice outcomes
  source varchar,             -- MARKS | PRACTICE | BLENDED (LAW 6: never model-invented)
  attempts int, correct int,
  updated_at, UNIQUE(child_id, topic_id)
)
```

### 12.5 NEW — `tutor_misconceptions` (the class-wide intelligence)
```
tutor_misconceptions(
  id, school_id, class_id, subject_id, topic_id,
  child_id, misconception_type, evidence text,
  resolved bool, surfaced_to_teacher bool, created_at
)
```

### 12.6 NEW — `tutor_knowledge_chunks` (RAG, specced now, populated Phase 5)
```
tutor_knowledge_chunks(
  id, school_id null,         -- null = shared NCERT corpus; set = school's own material
  source, board, class_label, subject, topic_id,
  chunk_text text, embedding vector(768),   -- pgvector; populated when RAG lands
  created_at
)  -- the retrieveKnowledge tool reads this; INERT until Phase 5
```

### 12.7 REUSED (no new table)
`ai_jobs` (async), `ai_response_cache` (fact-hash cache), `ai_usage_log` (cost/audit), `ai_prompt_templates` (versioned prompts + `pii_allowed_providers` + `traffic_weight`), plus all §3.3 academic tables.

---

## 13. Backend & client file map (for the implementation PRs)

Mirrors the PEWS/caseworker package layout so reviewers already know the shape.

```
server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/
  TutorModule.kt                 // module-singleton DI wiring (clone CaseworkerModule.kt)
  TutorRoutes.kt                 // Ktor routes: /tutor/doubt, /tutor/practice, /tutor/plan, /tutor/teacher-heatmap
  sense/
    LearnerBundleBuilder.kt      // TIER 0 — deterministic bundle from real tables (the LAW-6 firewall)
    BoardThresholds.kt           // board-aware "weak" thresholds
  triage/
    TutorTriageService.kt        // TIER 1 — cheap CLASSIFY (GROQ_FAST), cache + cohort dedup
  agent/
    TutorAgentService.kt         // TIER 2 — runs AiService.runAgent, parses TutorTurn, GroundingGuard, fallback
    TutorTools.kt                // read-only school/child-scoped tools (clone CaseworkerTools.kt)
    TutorTurn.kt                 // structured output contract (clone CaseFile.kt)
    TutorGroundingGuard.kt       // reuse/extend feature/pews/caseworker/GroundingGuard.kt
  act/
    TutorActService.kt           // TIER 3 — render(LaTeX), auto-grade, FSRS update, notify, escalate
    FsrsScheduler.kt             // wraps FSRS-Kotlin; owns tutor_review_state
    SafetyClassifier.kt          // deterministic escalation rails
  learn/
    TutorEfficacyService.kt      // TIER 4 — mastery-moved analysis, misconception rollups, prompt-priors
  ingest/
    OcrService.kt                // photo-doubt → text (Tesseract/PaddleOCR or vision-provider)
    VoiceService.kt              // voice-doubt → text (Whisper via Groq)
  rag/                           // PHASE 5 — inert until then
    KnowledgeIngestJob.kt        // NCERT corpus → chunk → embed → tutor_knowledge_chunks
    RetrieveKnowledgeTool.kt     // the retrieveKnowledge tool (stub → live)

server/.../feature/ai/   (gateway additions, mirror existing methods)
  AiService.kt   += embed()          // for RAG (Phase 5)
                 += transcribe()     // for voice doubts
                 += visionExtract()  // for photo doubts (or via OcrService)

docs/db/
  migration_064_tutor_2.sql       // tutor_sessions, tutor_review_state, tutor_mastery, tutor_misconceptions
  migration_065_tutor_rag.sql     // tutor_knowledge_chunks + pgvector (Phase 5)

composeApp/src/commonMain/kotlin/.../tutor/
  TutorChatScreen.kt              // Socratic doubt UI, photo/voice capture, LaTeX render (KotlinTeX)
  TutorPlanScreen.kt              // today's adaptive plan + due reviews
  TutorPracticeScreen.kt          // targeted practice + auto-grade feedback
  TeacherHeatmapScreen.kt         // class misconception heatmap + one-tap remediation
  ParentProgressScreen.kt         // mastery deltas, safety transparency, pause control
shared/src/commonMain/kotlin/.../tutor/presentation/
  TutorViewModel.kt, TutorPlanViewModel.kt, TeacherHeatmapViewModel.kt
```

---

## 14. Build sequence (each phase shippable, grounded, committed)

### Phase 0 — Tier-0 Learner Bundle + tables (week) — *the LAW-6 firewall, biggest safety jump*
`LearnerBundleBuilder` over real tables, board thresholds, `tutor_*` migrations (064). No LLM yet. Deterministic weak-topic + plan view ships — already useful, already grounded.

### Phase 1 — `student` role + Tier-2 doubt agent + grounding (week+) — *the agentic leap*
Add `student` role; `TutorAgentService` + `TutorTools` + `TutorTurn` + grounding guard; text-only Socratic doubt resolution end-to-end. **This is the demo that sells it.**

### Phase 2 — FSRS adaptive path + targeted practice + auto-grade (week) — *the personalization*
`FsrsScheduler` (FSRS-Kotlin), `tutor_review_state`, nightly plan via `ai_jobs`, targeted practice + auto-grading closed loop.

### Phase 3 — multi-modal capture (week) — *the "wow", removes the text-only wall*
`OcrService` (photo) + `VoiceService` (Whisper via Groq); de-PII vision routing. Doubt-by-photo and doubt-by-voice live.

### Phase 4 — cross-role loop + Tier-4 flywheel (week) — *the moat*
Teacher heatmap + misconception library, parent progress card + safety transparency, admin efficacy view, prompt-priors. The three-role loop closes.

### Phase 5 — agentic RAG (parallelizable, the future) — *scalability realised*
`AiService.embed()`, `migration_065` (pgvector), `KnowledgeIngestJob` (NCERT corpus + school material), `RetrieveKnowledgeTool` flips live. **No other tier changes.** The agent simply gains source-grounded retrieval.

---

## 15. Acceptance criteria — "AI Tutor 2.0 is real, not a chatbot"

1. **Grounded:** every percentage/fact in a tutor turn traces to a `groundedRef` (MARKS/SYLLABUS/NCERT/RAG); `GroundingGuard` rejects fabrications. Zero invented numbers in a 500-session audit.
2. **On-syllabus:** the tutor never teaches a topic with `isCovered = false`; off-syllabus doubts route to "flag for teacher," verified by test.
3. **Socratic, not answer-key:** in a 100-doubt sample, the tutor gives a direct final answer in < X% of cases (gated, logged); parent "0 answers given" metric is real.
4. **Personalized:** the adaptive plan is driven by FSRS due-dates + real weak topics, not random; test-soon ramps verified deterministically.
5. **Closed loop:** a graded practice answer measurably updates `tutor_mastery` and `tutor_review_state` and (on threshold) emits a teacher flag.
6. **Multi-modal:** a photographed problem and a spoken Hinglish doubt both resolve to a correct topic + grounded turn.
7. **Cross-role:** teacher heatmap shows class misconceptions on the *covered* chapter; parent sees mastery delta; admin sees efficacy; scoping enforced from JWT (no cross-class leakage).
8. **PII-safe:** any payload with child PII only ever hits PII-safe providers; vision/RAG calls are de-identified; verified in `ai_usage_log`.
9. **Cost-sane:** median event resolves in ≤ 1 LLM call (cache/triage/dedup); the v1 50-call/day budget is comfortably beaten at 10k students.
10. **RAG-ready:** flipping `retrieveKnowledge` from stub to live requires **no change** to any tier, the agent loop, or `TutorTurn`. Proven by Phase 5 landing as additive PRs only.

---

## 16. Risks & guardrails

| Risk | Guardrail |
|---|---|
| **AI gives answers → kills learning** | Socratic-by-default; answer is gated/logged; parent "answers given" metric; teacher can tighten per class |
| **Fabricated marks/feedback** | `GroundingGuard` + deterministic Tier-0 owns all numbers (LAW 6); deterministic fallback on guard failure |
| **Child PII leaks to training providers** | PII-safe lanes only for PII; de-identify before vision/RAG; `GuardrailService` egress filter; per-template `pii_allowed_providers` |
| **Safeguarding (distress in a doubt)** | Deterministic safety classifier → immediate human (safeguarding + parent); AI does not counsel; audited path |
| **Teaching ahead of the class** | Tier-0 only exposes `isCovered` topics; off-syllabus → flag-for-teacher |
| **Cost blowout at scale** | Tiering + cache + cohort dedup + async batch + per-lane token budgets; circuit breaker |
| **No student role yet** | Phase-1 role add; until then parent-scoped `childId`; no architectural debt |
| **Hallucinated math / wrong solution** | Auto-grade against agent-produced answer key; rubric-grounded free-response; teacher override trains priors |
| **Provider outage** | `CircuitBreaker` + 7-provider fallback; interactive lane degrades to deterministic plan, never a 500 |
| **OCR misreads handwriting** | Confidence threshold → ask child to confirm transcribed problem before reasoning |

---

## 17. One-paragraph summary for the team

**AI Tutor 2.0 is a curriculum-grounded, tool-using learning agent — one per child per subject — built on the gateway and the PEWS caseworker pattern we already have.** It senses deterministically (this child's real marks, syllabus position, missed homework, FSRS-due reviews), triages cheaply, reasons with read-only school-scoped tools, and acts under a grounding guard that makes fabrication impossible (LAW 6). It resolves doubts Socratically across text, photo (OCR), and voice (Whisper), drives a personalized plan with the free FSRS-Kotlin engine, generates and auto-grades targeted practice, and closes a three-role loop: the student learns, the teacher gets a misconception heatmap on the chapter they just taught, the parent sees real mastery deltas with full safety transparency, and the admin sees the efficacy number that sells the platform. It rides the 7-provider router with strict PII lanes, caches and batches to crush cost, and — critically — is architected so the **future agentic RAG system lands as one more tool (`retrieveKnowledge` over a pgvector NCERT corpus) with zero changes to the agent loop.** That is the difference between a generic homework chatbot and a $100M learning platform: not the model, but the grounded data graph, the agentic loop, and the compounding flywheel underneath it.
