# Offline Mode & Sync — Engineering Plan (Vidya Prayag)

> Status: PROPOSED · Owner: AI-features · Branch: `ai-features-fix`
> Scope: Kotlin Multiplatform (Android / iOS / Desktop-JVM / Web) + Ktor server
> Goal: A **robust, no-failure offline-first system** that lets users keep working
> with no connectivity and **automatically, safely syncs** when the network returns —
> with zero data loss, zero duplicate writes, and zero UI crashes at any layer.

---

## 0. TL;DR

We introduce an **offline-first data pipeline** built on the project's *existing*
Clean-Architecture + MVVM + Koin + Room + Ktor foundation. Nothing in the current
feature code is rewritten; we **wrap** the data layer:

1. **Connectivity** — an `expect/actual` `NetworkMonitor` exposing `Flow<NetworkStatus>`.
2. **Local cache (read path)** — Room is the single source of truth (SSOT). UI reads
   from Room; the network only *refreshes* Room (the pattern `SchoolRepositoryImpl`
   already uses).
3. **Outbox (write path)** — every offline mutation is appended to a durable
   `outbox_operation` table with an **idempotency key**, then replayed by a
   `SyncEngine` when online.
4. **Sync engine** — a single-flight, retry-with-backoff, conflict-aware worker that
   drains the outbox and reconciles server truth back into Room.
5. **No-failure guarantees** — every layer is total (no uncaught exceptions), every
   write is idempotent, every conflict has a deterministic resolution policy, and the
   web target (which has no Room) degrades gracefully to online-only.

The work is delivered through a **Plan → Build → Test → Review → Iterate loop**
(§9) so each feature reaches an "ideal" bar before the next is started.

---

## 1. Where this fits the current architecture

Confirmed from the codebase (do **not** deviate from these patterns):

| Concern | Existing mechanism | Offline mode reuses it by… |
|---|---|---|
| Layering | Clean Arch: `feature/<x>/{domain,data,presentation}` | Adding a `data/local` + outbox under the same feature folders |
| Presentation | MVVM — `ViewModel` + `StateFlow`, Koin `factory` | ViewModels gain an `isOffline`/`syncState` field; no new pattern |
| DI | Koin `commonModule` + `expect fun platformModule()` | New singles registered in `commonModule`; platform bits in each `platformModule.*` |
| Local DB | Room `roomMain` source set (android/jvm/ios), `AppDatabase` v1 | Bump schema, add DAOs/entities, add a migration |
| Web | **No Room** — `InMemorySchoolLocalDataSource`, `LocalStoragePreferenceManager` | Web uses an in-memory/no-op outbox → online-only, never crashes |
| Remote | Ktor `HttpClient` single + `safeApiCall` → `NetworkResult` | `ConnectionError` is the offline signal; reused unchanged |
| Auth | `installTokenAuth` + `TokenAuthenticator` (401 refresh) | Sync engine pauses on auth-failure, resumes after re-login |

**Design principles we commit to** (and how):

- **SOLID**
  - *S* — `NetworkMonitor`, `OutboxDao`, `SyncEngine`, `ConflictResolver`,
    `OutboxOperationSerializer` are each one responsibility.
  - *O* — new mutations are added by registering an `OutboxHandler`, never by editing
    the engine. (Strategy/registry pattern.)
  - *L* — `OfflineFirstRepository<T>` substitutes anywhere a plain repository is used;
    same interface, richer behaviour.
  - *I* — small interfaces: `Syncable`, `OutboxHandler`, `ConflictResolver<T>`.
  - *D* — engine depends on `OutboxHandler`/`NetworkMonitor` abstractions, resolved
    via Koin; no concrete feature dependency.
- **MVVM** — UI ↔ ViewModel ↔ UseCase ↔ Repository ↔ (Local SSOT + Remote). Sync is a
  background concern invisible to Composables except via a small `SyncStatus` state.
- **Offline-first / SSOT** — the screen *always* renders Room; the network is a
  best-effort updater. This is the single rule that removes "offline crash" classes.

---

## 2. Module / package layout (new files only)

```
shared/src/commonMain/.../core/
├── connectivity/
│   ├── NetworkMonitor.kt              (expect interface + NetworkStatus enum)
│   └── ConnectivityModule.kt          (Koin wiring helper, optional)
├── offline/
│   ├── outbox/
│   │   ├── OutboxOperation.kt         (domain model: id, idempotencyKey, type, payload, status, attempts, createdAt, lastError)
│   │   ├── OutboxStatus.kt            (PENDING / IN_FLIGHT / FAILED / DONE)
│   │   ├── OutboxRepository.kt        (interface: enqueue/peek/markDone/markFailed/observePending)
│   │   ├── OutboxHandler.kt           (interface: type tag + suspend execute(payload): SyncResult)
│   │   └── OutboxHandlerRegistry.kt   (maps type -> handler; OCP extension point)
│   ├── sync/
│   │   ├── SyncEngine.kt              (single-flight drain loop, backoff, pause/resume)
│   │   ├── SyncState.kt              (IDLE / SYNCING / ERROR + pendingCount)
│   │   ├── SyncResult.kt             (Success / RetryableFailure / PermanentFailure / Conflict)
│   │   └── BackoffPolicy.kt          (exponential + jitter, capped)
│   └── conflict/
│       └── ConflictResolver.kt        (interface + default Last-Write-Wins/Server-Wins)
└── offline/OfflineFirstRepository.kt  (small reusable base: cacheThenNetwork helper)

shared/src/roomMain/.../core/offline/
├── OutboxOperationEntity.kt + OutboxDao.kt
└── (AppDatabase gains outboxDao(); version 1 -> 2 + Migration)

Platform actuals:
shared/src/androidMain/.../core/connectivity/NetworkMonitor.android.kt   (ConnectivityManager)
shared/src/iosMain/.../core/connectivity/NetworkMonitor.ios.kt          (NWPathMonitor)
shared/src/jvmMain/.../core/connectivity/NetworkMonitor.jvm.kt          (reachability poll)
shared/src/jsMain + wasmJsMain/.../NetworkMonitor.web.kt                (navigator.onLine + events)
shared/src/jsMain + wasmJsMain/.../offline/InMemoryOutboxRepository.kt  (no-op/online-only)
```

Server (`/server`): add **idempotency support** — accept an `Idempotency-Key`
header on every mutating endpoint, store `(key -> response)` for a TTL window, and
replay the stored response on duplicate keys. This is what makes retries truly safe.

---

## 3. Core contracts (the no-failure spine)

### 3.1 NetworkMonitor (expect/actual)
```kotlin
enum class NetworkStatus { Available, Unavailable, Unknown }

interface NetworkMonitor {
    val status: StateFlow<NetworkStatus>   // hot, always has a current value
    fun start(); fun stop()
}
```
- Android: `ConnectivityManager.NetworkCallback` + `NetworkCapabilities.NET_CAPABILITY_VALIDATED`.
- iOS: `NWPathMonitor`. JVM: lightweight reachability poll (e.g. HEAD to base URL, 15s).
- Web: `window.navigator.onLine` + `online`/`offline` events.
- **Total**: any failure to determine status → `Unknown` (treated as "try, but don't assume online").

### 3.2 OutboxOperation (durable intent)
```kotlin
data class OutboxOperation(
    val id: String,                 // local UUID
    val idempotencyKey: String,     // == id, sent as Idempotency-Key header
    val type: String,               // e.g. "attendance.markDaily"
    val payloadJson: String,        // serialized request DTO
    val status: OutboxStatus,
    val attempts: Int,
    val nextAttemptAt: Long,        // backoff schedule
    val createdAt: Long,
    val lastError: String? = null,
)
```
Ordering: FIFO per `(entityType, entityId)`; independent entities sync in parallel.

### 3.3 OutboxHandler (OCP extension point — one per mutation type)
```kotlin
interface OutboxHandler {
    val type: String
    suspend fun execute(op: OutboxOperation): SyncResult
}
sealed interface SyncResult {
    data object Success : SyncResult
    data class Retryable(val reason: String) : SyncResult     // network/5xx
    data class Permanent(val reason: String) : SyncResult     // 4xx validation
    data class Conflict(val serverState: String) : SyncResult // 409
}
```
Adding a new offline-capable action = implement a handler + register it. **The engine
never changes.** (Open/Closed.)

### 3.4 SyncEngine (the worker)
- **Single-flight**: a `Mutex` ensures only one drain runs; new triggers coalesce.
- **Triggers**: (a) connectivity → `Available`, (b) new enqueue, (c) app foreground,
  (d) periodic safety tick.
- **Drain loop**: read PENDING ops due now → set IN_FLIGHT → call handler → map result:
  - `Success` → markDone, reconcile cache.
  - `Retryable` → increment attempts, schedule `nextAttemptAt` via `BackoffPolicy`,
    set back to PENDING. Cap attempts (e.g. 12) → then FAILED (surfaced to user, kept).
  - `Permanent` → FAILED, surface to user, keep payload for manual review/discard.
  - `Conflict` → run `ConflictResolver`, then Success or FAILED.
- **Auth pause**: on refresh-failure (`onRefreshFailed`) the engine pauses; resumes on
  next successful auth/login. No tight error loop.
- **Crash-safety**: IN_FLIGHT ops are reset to PENDING on engine start (a process kill
  mid-flight is recovered; the server idempotency key prevents double-apply).

### 3.5 ConflictResolver (deterministic)
Default policy: **Server-wins for reads, Last-Write-Wins for user's own edits**, with
a per-feature override slot. Health/marks/attendance use **Server-authoritative +
re-queue** (never silently overwrite graded data). Each feature declares its policy;
the default is safe.

---

## 4. Read path vs write path

**Read (cache-then-network)** — generalize the existing `SchoolRepositoryImpl`:
```
ViewModel collects repo.observeX()  ->  Room Flow (instant, even offline)
repo.refreshX()  ->  safeApiCall  ->  Success: upsert Room  |  ConnectionError: no-op, keep cache
```
UI shows cached data + a subtle "offline / last updated …" banner. **Never blank, never crash.**

**Write (enqueue-then-sync)**:
```
ViewModel calls useCase  ->  repo.mutateX(dto)
   1. optimistic local apply to Room (UI updates instantly)
   2. outbox.enqueue(type, dto, idempotencyKey)
   3. trigger SyncEngine (fires now if online, else waits for connectivity)
SyncEngine later  ->  handler.execute  ->  server (Idempotency-Key)  ->  reconcile Room
```

---

## 5. Server-side requirements (the other half of "no failure")

1. **Idempotency**: middleware that reads `Idempotency-Key`; on first sight, process &
   store the response keyed by `(key, userId, route)` for ~24h; on replay, return the
   stored response without re-executing. *This is mandatory* — without it, mobile retries
   create duplicates.
2. **Server timestamps / versions**: mutating responses return `updatedAt`/`version` so
   the client can do `updatedAt`-based conflict resolution.
3. **Stable IDs**: server must accept the client-generated UUID (or return a mapping) so
   optimistic local rows can be reconciled to their server identity.
4. **Batch-friendly**: optionally a `/sync/batch` endpoint to drain many ops in one round
   trip (phase 2 optimization, not required for correctness).

---

## 6. Phased delivery (each phase ends "ideal", then loop to next)

- **Phase 0 — Foundations (no behaviour change)**
  `NetworkMonitor` (+actuals), `SyncState`/`SyncStatus` Koin singles, an offline banner
  composable. Bump `AppDatabase` to v2 with `outbox_operation` + Migration. Web no-ops.
- **Phase 1 — Read-offline for high-value screens**
  Convert 2–3 read-heavy features (e.g. Announcements, Today/schedule, Student roster)
  to cache-then-network. Verify they render fully offline.
- **Phase 2 — Outbox + SyncEngine + one write feature**
  Wire the engine; make **Attendance marking** offline-capable end-to-end (enqueue,
  optimistic UI, idempotent replay, conflict policy). This is the reference implementation.
- **Phase 3 — Roll out writes** to remaining safe mutations (leave requests, messages,
  homework submissions, health notes) one handler at a time.
- **Phase 4 — Hardening**: storage caps + eviction (LRU on cache, never on outbox),
  encryption-at-rest review, telemetry (pending count, oldest pending age), QA matrix.

---

## 7. Failure-mode matrix (every branch is handled — "no failure at any level")

| Failure | Layer | Guaranteed behaviour |
|---|---|---|
| No network on read | repo | Serve Room cache + offline banner; never blank/crash |
| No network on write | repo | Optimistic Room write + enqueue; UI succeeds locally |
| App killed mid-sync | engine | IN_FLIGHT→PENDING on restart; idempotency key prevents double-apply |
| Server 5xx / timeout | engine | Retryable → exponential backoff + jitter, capped attempts |
| Server 4xx validation | engine | Permanent → mark FAILED, surface to user, keep payload |
| Conflict (409 / stale) | resolver | Deterministic policy per feature; graded data never auto-clobbered |
| Auth token expired | engine | Pause sync; `installTokenAuth` refresh; resume or wait for re-login |
| Duplicate enqueue (double tap) | outbox | Same idempotency key / dedup on `(type,entityId,hash)` |
| Web target (no Room) | DI | In-memory no-op outbox → online-only; identical API, never crashes |
| Corrupt cached row | repo | Lenient deserialize (`ignoreUnknownKeys`), drop-and-refetch on parse error |
| Storage full | engine | Outbox is bounded + alerts; cache eviction frees space; writes never silently lost |
| Migration failure | DB | Tested migration + fallback; never `fallbackToDestructiveMigration` on outbox table |

---

## 8. Testing strategy (gates the loop)

- **Unit (commonTest)**: `BackoffPolicy`, `OutboxHandlerRegistry`, `ConflictResolver`,
  `SyncEngine` with fake handlers/monitor (offline→online transitions, retry caps,
  single-flight, crash recovery).
- **Repository tests**: fake `NetworkMonitor` + in-memory Room → assert cache-then-network
  and enqueue-then-sync semantics.
- **Idempotency tests (server)**: same key twice → one effect, identical response.
- **Build gates**: `./gradlew :shared:compileKotlinJvm :shared:jvmTest` then
  `:composeApp:assembleDevDebug` and `:composeApp:wasmJsBrowserDistribution` (web must
  still compile with no-op outbox).
- **Manual QA matrix**: airplane-mode mark attendance → reconnect → verify single server
  row; kill app mid-sync; flaky-network (5xx) replay; concurrent edits on two devices.

---

## 9. The Plan→Build→Test→Review→Iterate loop (graph)

```
        ┌──────────────────────────────────────────────────────┐
        v                                                        │
 ┌────────────┐   ┌────────────┐   ┌────────────┐   ┌─────────────────┐
 │  PLAN node │ → │ BUILD node │ → │  TEST node │ → │  REVIEW node    │
 │ pick next  │   │ minimal,   │   │ unit+build │   │ SOLID/MVVM +    │
 │ phase item │   │ no junk    │   │ +QA gate   │   │ failure-matrix  │
 └────────────┘   └────────────┘   └────────────┘   └────────┬────────┘
                                                              │
                                  PASS (ideal) → commit ──────┘
                                  FAIL → loop back to BUILD with findings
```
**Exit criteria per item ("ideal"):** compiles on all targets · all tests green ·
every row of the failure-matrix demonstrably handled for that feature · no dead/duplicate
code · adheres to `DEVELOPMENT_STANDARDS.md` · reviewer (or GLM god-mode prompt, see
`docs/`) finds no further enhancement.

---

## 10. Non-goals / explicit constraints

- No new architecture pattern, no rewrite of existing features, **no junk/scaffolding code**.
- Web stays online-only (no Room there) but uses the identical repository API.
- Outbox rows are **never** destructively dropped by a migration.
- Sensitive payloads in the outbox follow the existing redaction rules.

---

*Companion artifact:* `docs/GLM_GODMODE_PROMPT.md` — a reusable, document-grounded prompt
that drives an LLM (GLM 5.2) to execute this plan inside the Plan→Build→Test→Review loop.
