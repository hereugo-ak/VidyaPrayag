# Offline Mode — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Audience:** Senior engineer / AI agent implementing the system
> **Last updated:** 2026-06-27
> **Prerequisites:** None (but aligns with `MESSAGING_SYSTEM_SPEC.md` §6 offline-first model)
> **Unblocks:** All features benefit; messaging already has a spec for its own offline

---

## Table of Contents

1. [Feature Overview](#1-feature-overview)
2. [Current System Assessment](#2-current-system-assessment)
3. [Gap Analysis](#3-gap-analysis)
4. [Functional Requirements](#4-functional-requirements)
5. [Database Design](#5-database-design)
6. [Backend Architecture](#6-backend-architecture)
7. [Frontend Architecture](#7-frontend-architecture)
8. [Synchronization Strategy](#8-synchronization-strategy)
9. [Offline Behaviour](#9-offline-behaviour)
10. [Conflict Resolution](#10-conflict-resolution)
11. [API Contracts](#11-api-contracts)
12. [Performance Considerations](#12-performance-considerations)
13. [Infrastructure Requirements](#13-infrastructure-requirements)
14. [Background Processing](#14-background-processing)
15. [Feature Flags](#15-feature-flags)
16. [Migration Strategy](#16-migration-strategy)
17. [Testing Strategy](#17-testing-strategy)
18. [Acceptance Criteria](#18-acceptance-criteria)
19. [Implementation Roadmap](#19-implementation-roadmap)
20. [File-Level Impact Analysis](#20-file-level-impact-analysis)
21. [Risks & Mitigations](#21-risks--mitigations)

---

## 1. Feature Overview

A platform-level offline-first architecture using SQLDelight for client-side local database, a sync engine for delta synchronization, and an outbox pattern for write operations. This spec covers the shared infrastructure that all feature modules use for offline support.

### Goals

- All portal data (parent, teacher, admin) cached locally for offline access
- Write operations queued in outbox and synced when connectivity returns
- Delta sync (only fetch changes since last cursor) to minimize bandwidth
- Automatic conflict resolution (LWW + server-authoritative)
- Transparent to feature modules — repositories read/write local DB, sync engine handles network
- Clean logout clears all local data

---

## 2. Current System Assessment

### 2.1 What Exists

- **No client local DB** — `MESSAGING_SYSTEM_SPEC.md` §1.2.7: "No client local DB — all data fetched fresh per screen load"
- `feature_audit.csv` L146: "Only school discovery has Room cache, no other offline support" — 15% complete
- DataStore stores only auth tokens (not business data)
- `NetworkResult<T>` sealed class for network error handling
- Koin DI with `single { HttpClient }` — shared across features
- `MESSAGING_SYSTEM_SPEC.md` §6 already defines SQLDelight + sync engine for messaging — this spec generalizes it

### 2.2 What's Missing

- No SQLDelight setup in shared module
- No local DB schema
- No sync engine
- No outbox pattern
- No background sync (WorkManager/BackgroundTasks)
- No offline indicators in UI

---

## 3. Gap Analysis

| # | Gap | Impact |
|---|---|---|
| G1 | No local DB | App unusable without network |
| G2 | No sync engine | Data stale after first load |
| G3 | No outbox | Write operations lost on network failure |
| G4 | No conflict resolution | Concurrent edits cause data loss |
| G5 | No background sync | Data not refreshed when app in background |
| G6 | No offline UI indicators | Users confused when data is stale |

---

## 4. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | SQLDelight local DB in shared module (Android + iOS + JVM + JS + wasmJs) |
| FR-2 | Feature modules read from local DB first (show cached data + "syncing" indicator) |
| FR-3 | Write operations go to local DB + outbox first (instant UI feedback) |
| FR-4 | Sync engine performs delta sync (only changes since last cursor) |
| FR-5 | Outbox manager flushes pending writes with retry + backoff |
| FR-6 | Conflict resolution: LWW (server-authoritative timestamps) |
| FR-7 | Background sync: WorkManager (Android, 15-min periodic), BackgroundTasks (iOS) |
| FR-8 | Network status monitoring (online/offline transitions) |
| FR-9 | UI indicators: "syncing", "offline", "pending sends (N)" |
| FR-10 | Logout clears all local data (DB + DataStore) |
| FR-11 | Cache eviction: LRU if local DB exceeds 100MB |
| FR-12 | Per-feature sync cursors stored in local DB |

---

## 5. Database Design

### 5.1 SQLDelight Schema (Client-Side)

```sql
-- Sync metadata: per-feature cursors
CREATE TABLE sync_cursors (
    feature TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    cursor TEXT,                    -- server-issued cursor (timestamp or seq)
    last_synced_at INTEGER,         -- epoch millis
    PRIMARY KEY (feature, entity_type)
);

-- Generic cached entity store (key-value with JSON payload)
CREATE TABLE cached_entities (
    feature TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    entity_id TEXT NOT NULL,
    payload TEXT NOT NULL,          -- JSON serialized entity
    updated_at INTEGER NOT NULL,    -- epoch millis (from server)
    PRIMARY KEY (feature, entity_type, entity_id)
);

-- Outbox: pending write operations
CREATE TABLE outbox (
    id TEXT NOT NULL PRIMARY KEY,   -- client-generated UUID
    feature TEXT NOT NULL,
    operation TEXT NOT NULL,        -- CREATE | UPDATE | DELETE
    entity_type TEXT NOT NULL,
    entity_id TEXT NOT NULL,
    payload TEXT NOT NULL,          -- JSON serialized request body
    status TEXT NOT NULL,           -- PENDING | SENDING | FAILED | SENT
    attempts INTEGER NOT NULL DEFAULT 0,
    next_retry_at INTEGER,          -- epoch millis
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

-- Key-value cache for list responses
CREATE TABLE cached_lists (
    feature TEXT NOT NULL,
    list_key TEXT NOT NULL,         -- e.g. "attendance:2026-06-27:Grade5A"
    payload TEXT NOT NULL,          -- JSON array of entity IDs or full entities
    cached_at INTEGER NOT NULL,
    PRIMARY KEY (feature, list_key)
);
```

### 5.2 SQLDelight Queries

```sql
-- cached_entities
SELECT * FROM cached_entities WHERE feature = :feature AND entity_type = :type ORDER BY updated_at DESC;
SELECT * FROM cached_entities WHERE feature = :feature AND entity_type = :type AND entity_id = :id;
INSERT OR REPLACE INTO cached_entities (feature, entity_type, entity_id, payload, updated_at) VALUES (:feature, :type, :id, :payload, :updatedAt);
DELETE FROM cached_entities WHERE feature = :feature AND entity_type = :type AND entity_id = :id;

-- outbox
SELECT * FROM outbox WHERE status = 'PENDING' OR (status = 'FAILED' AND next_retry_at <= :now) ORDER BY created_at ASC LIMIT 50;
INSERT INTO outbox (id, feature, operation, entity_type, entity_id, payload, status, created_at, updated_at) VALUES (...);
UPDATE outbox SET status = :status, attempts = :attempts, next_retry_at = :nextRetry, updated_at = :now WHERE id = :id;
DELETE FROM outbox WHERE status = 'SENT';

-- sync_cursors
SELECT * FROM sync_cursors WHERE feature = :feature;
INSERT OR REPLACE INTO sync_cursors (feature, entity_type, cursor, last_synced_at) VALUES (:feature, :type, :cursor, :syncedAt);
```

---

## 6. Backend Architecture

### 6.1 Delta Sync Endpoints

Each feature module exposes a delta sync endpoint:

```
GET /api/v1/{role}/{feature}/sync?cursor={cursor}&limit=100
```

**Response:**
```json
{
  "success": true,
  "data": {
    "items": [...],
    "deleted_ids": ["uuid1", "uuid2"],
    "next_cursor": "2026-06-27T10:30:00Z",
    "has_more": false
  }
}
```

### 6.2 Server-Side Cursor Strategy

- Cursor = `updated_at` timestamp of last item returned
- Server queries: `WHERE school_id = ? AND updated_at > cursor ORDER BY updated_at ASC LIMIT 100`
- Deleted items tracked via `deleted_at` (soft delete) — returned in `deleted_ids`
- If no cursor (first sync), returns all data (paginated)

### 6.3 Idempotency for Outbox

Outbox writes include a `client_request_id` (UUID). Server deduplicates:
- `INSERT INTO ... ON CONFLICT (client_request_id) DO NOTHING`
- Returns 409 with existing entity data → client treats as success

---

## 7. Frontend Architecture

### 7.1 SyncEngine

```kotlin
class SyncEngine(
    private val localDb: LocalDatabase,
    private val httpClient: HttpClient,
    private val networkMonitor: NetworkMonitor
) {
    suspend fun initialSync(): SyncResult        // on app launch, if online
    suspend fun deltaSync(feature: String): SyncResult  // per-feature delta
    suspend fun flushOutbox(): OutboxFlushResult  // send pending writes
    suspend fun backgroundSync(): SyncResult      // WorkManager periodic
    suspend fun clearAll()                        // on logout
}
```

### 7.2 OutboxManager

```kotlin
class OutboxManager(
    private val localDb: LocalDatabase,
    private val httpClient: HttpClient
) {
    suspend fun enqueue(operation: OutboxOperation)
    suspend fun flush(): Int  // returns number of items sent
    // Retry: attempt 1 immediate, 2: +2s, 3: +5s, 4: +15s, 5: +60s, 6+: +300s (cap), max 20
}
```

### 7.3 NetworkMonitor

```kotlin
class NetworkMonitor {
    val isOnline: StateFlow<Boolean>
    // Android: ConnectivityManager.NetworkCallback
    // iOS: NWPathMonitor
    // JS/wasmJs: navigator.onLine + window events
}
```

### 7.4 OfflineRepository Pattern

Each feature's repository follows this pattern:

```kotlin
class AttendanceRepository(
    private val localDb: LocalDatabase,
    private val api: AttendanceApi,
    private val outbox: OutboxManager,
    private val syncEngine: SyncEngine
) {
    // READ: local first, then sync
    suspend fun getAttendance(date: LocalDate, classId: UUID): List<AttendanceRecord> {
        // 1. Read from local DB (instant)
        val cached = localDb.getCachedEntities("attendance", "records:$date:$classId")
        // 2. Trigger async delta sync (non-blocking)
        if (networkMonitor.isOnline.value) {
            syncEngine.deltaSync("attendance")  // fire-and-forget
        }
        return cached
    }

    // WRITE: local + outbox
    suspend fun markAttendance(record: AttendanceRecord) {
        // 1. Write to local DB (instant UI update)
        localDb.insertCachedEntity("attendance", record.id, Json.encodeToString(record))
        // 2. Enqueue outbox
        outbox.enqueue(OutboxOperation(
            operation = "CREATE",
            entityType = "attendance_record",
            entityId = record.id,
            payload = Json.encodeToString(record)
        ))
        // 3. Try immediate flush if online
        if (networkMonitor.isOnline.value) {
            outbox.flush()  // best-effort
        }
    }
}
```

### 7.5 UI Components

- **`OfflineIndicator`** — composable showing "Offline" banner when no network
- **`SyncingIndicator`** — small spinner in app bar when sync in progress
- **`PendingSendsBadge`** — badge on outbox-affected screens showing count of pending writes
- **`RetryButton`** — on failed outbox items, manual retry

---

## 8. Synchronization Strategy

### 8.1 Sync Triggers

| Trigger | Action |
|---|---|
| App launch | `initialSync()` for all features |
| Network restored | `deltaSync()` for all features + `flushOutbox()` |
| Screen opened | `deltaSync(feature)` for that screen's feature (non-blocking) |
| Pull-to-refresh | `deltaSync(feature)` (blocking, show spinner) |
| Background (WorkManager) | `backgroundSync()` every 15 min (Android) |
| Logout | `clearAll()` |

### 8.2 Sync Priority

1. Outbox flush (send pending writes) — highest priority
2. Delta sync for current screen's feature
3. Delta sync for other features (background)

### 8.3 Bandwidth Optimization

- Delta sync only fetches changes (typically < 10 items per sync)
- Responses compressed (gzip)
- Images loaded via Coil with disk cache (7-day TTL)
- Large lists paginated (50 items per page)

---

## 9. Offline Behaviour

| Feature | Offline Read | Offline Write |
|---|---|---|
| Messaging | ✅ (cached threads + messages) | ✅ (outbox) |
| Attendance | ✅ (last 7 days cached) | ✅ (outbox) |
| Homework | ✅ (cached list) | ❌ (requires server for student roster) |
| Marks | ✅ (cached assessments) | ✅ (outbox) |
| Announcements | ✅ (cached list) | ❌ (admin-only, requires server) |
| Fees | ✅ (cached) | ❌ (payment requires server) |
| Calendar | ✅ (cached events) | ❌ |
| Profile | ✅ (cached) | ✅ (outbox) |

---

## 10. Conflict Resolution

| Scenario | Resolution |
|---|---|
| Same entity edited offline + online | LWW: server timestamp wins; client receives server version on next sync |
| Outbox item conflicts with server state | Server returns 409 with current state; client updates local DB |
| Outbox item references deleted entity | Server returns 410; client marks as deleted locally |
| Duplicate outbox send (same client_request_id) | Server 409; client treats as success |
| Schema mismatch on sync | Clear local DB + re-sync |

---

## 11. API Contracts

### 11.1 Delta Sync (Per Feature)

```
GET /api/v1/{role}/{feature}/sync?cursor={ISO8601}&limit=100
```

### 11.2 Outbox Write (Idempotent)

```
POST /api/v1/{role}/{feature}
X-Client-Request-Id: {uuid}
{request body}
```

**Response 201:** Created
**Response 409:** Conflict (duplicate client_request_id) — return existing entity

---

## 12. Performance Considerations

- Local DB reads < 5ms (SQLite/SQLDelight)
- Delta sync response < 50KB typically (< 500ms on 3G)
- Outbox flush processes 50 items per batch
- Background sync limited to 15-min intervals (battery)
- Cache eviction: if DB > 100MB, evict oldest cached_lists entries first
- SQLDelight queries use indexed columns (feature, entity_type, entity_id)

---

## 13. Infrastructure Requirements

| Component | Technology | Notes |
|---|---|---|
| Local DB | SQLDelight 2.0+ | KMP-native, type-safe, all targets |
| Background sync (Android) | WorkManager | Periodic 15-min, network-constrained |
| Background sync (iOS) | BGAppRefreshTask | Periodic, OS-scheduled |
| Network monitoring | Platform-specific | ConnectivityManager / NWPathMonitor / navigator.onLine |
| HTTP compression | gzip | Ktor ContentNegotiation supports |

### New Dependencies

| Dependency | Version | Purpose |
|---|---|---|
| `app.cash.sqldelight:android-driver` | 2.0+ | Android SQLite driver |
| `app.cash.sqldelight:native-driver` | 2.0+ | iOS SQLite driver |
| `app.cash.sqldelight:sqljs-driver` | 2.0+ | JS/wasmJs driver |
| `app.cash.sqldelight:coroutines-extensions` | 2.0+ | Flow-based queries |

---

## 14. Background Processing

| Job | Platform | Schedule | Description |
|---|---|---|---|
| Background sync | Android | 15 min (WorkManager) | Delta sync all features + flush outbox |
| Background sync | iOS | OS-determined (BGAppRefreshTask) | Same |
| Outbox retry | All | On network restore | Flush all pending outbox items |
| Cache eviction | All | On app launch (if DB > 100MB) | Evict oldest cached_lists |

---

## 15. Feature Flags

| Flag | Default | Description |
|---|---|---|
| `OFFLINE_MODE_ENABLED` | false | Enable offline-first architecture |
| `OFFLINE_BACKGROUND_SYNC_ENABLED` | false | Enable WorkManager background sync |
| `OFFLINE_CACHE_EVICTION_ENABLED` | false | Enable automatic cache eviction |

---

## 16. Migration Strategy

### 16.1 No DB Migration

This is a client-side change only. No server DB migration needed. Server-side delta sync endpoints added incrementally per feature.

### 16.2 Rollout

1. Add SQLDelight to shared module
2. Implement SyncEngine + OutboxManager + NetworkMonitor
3. Migrate messaging feature first (per MESSAGING_SYSTEM_SPEC.md)
4. Migrate attendance, homework, marks features
5. Migrate remaining features
6. Enable background sync

### 16.3 Rollback

Disable `OFFLINE_MODE_ENABLED` flag → app reverts to online-only (fetches fresh per screen). Local DB remains but is not read.

---

## 17. Testing Strategy

### 17.1 Unit Tests

- SQLDelight CRUD operations
- Outbox retry with backoff
- Conflict resolution (LWW)
- Cache eviction logic
- Cursor advancement

### 17.2 Integration Tests

- Offline read → shows cached data
- Offline write → outbox queued → network restored → data synced
- Delta sync → only new items fetched
- Logout → local DB cleared
- Background sync → data refreshed

### 17.3 Platform Tests

- Android: WorkManager periodic sync fires
- iOS: BGAppRefreshTask scheduled
- Network transition: offline → online triggers sync

---

## 18. Acceptance Criteria

- [ ] App shows cached data when offline
- [ ] Write operations queued in outbox when offline
- [ ] Outbox flushed automatically when network restored
- [ ] Delta sync fetches only changes since last cursor
- [ ] UI shows offline/syncing indicators
- [ ] Background sync runs on schedule (Android + iOS)
- [ ] Logout clears all local data
- [ ] Cache eviction prevents unbounded growth
- [ ] Conflict resolution handles concurrent edits
- [ ] Feature modules can opt-in to offline incrementally

---

## 19. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 3 days | SQLDelight setup, driver config for all KMP targets |
| 2 | 3 days | Local DB schema, queries, LocalDatabase wrapper |
| 3 | 3 days | SyncEngine (initialSync, deltaSync, backgroundSync) |
| 4 | 2 days | OutboxManager (enqueue, flush, retry with backoff) |
| 5 | 2 days | NetworkMonitor (platform-specific implementations) |
| 6 | 2 days | OfflineRepository pattern + base classes |
| 7 | 2 days | UI components (OfflineIndicator, SyncingIndicator, PendingSendsBadge) |
| 8 | 3 days | Migrate messaging feature (align with MESSAGING_SYSTEM_SPEC.md) |
| 9 | 3 days | Migrate attendance + homework + marks features |
| 10 | 2 days | Background sync (WorkManager + BGAppRefreshTask) |
| 11 | 3 days | Tests (unit + integration + platform) |

---

## 20. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `shared/build.gradle.kts` | Modify | Add SQLDelight dependencies + plugin |
| `shared/.../core/local/LocalDatabase.kt` | New | SQLDelight database wrapper |
| `shared/.../core/local/LocalDbSchema.sq` | New | SQLDelight schema |
| `shared/.../core/sync/SyncEngine.kt` | New | Delta sync engine |
| `shared/.../core/sync/OutboxManager.kt` | New | Outbox queue + retry |
| `shared/.../core/sync/NetworkMonitor.kt` | New | Network status monitor |
| `shared/.../core/sync/OfflineRepository.kt` | New | Base offline repository pattern |
| `shared/.../di/Koin.kt` | Modify | Register local DB + sync services |
| `composeApp/.../ui/v2/components/OfflineIndicator.kt` | New | Offline UI components |
| `composeApp/.../ui/v2/components/SyncingIndicator.kt` | New | Sync status indicator |
| `server/.../feature/*/Routing.kt` | Modify | Add delta sync endpoints per feature |

---

## 21. Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| SQLDelight schema migration fails | Low | Medium | Version migrations; clear + re-sync on failure |
| Local DB grows unbounded | Medium | Medium | Cache eviction at 100MB; LRU on cached_lists |
| Battery drain from background sync | Medium | Medium | 15-min minimum interval; network-constrained |
| iOS background refresh not triggered | High | Low | OS controls timing; app handles gracefully |
| Conflict resolution loses data | Low | Medium | LWW is simple; audit log captures changes; manual merge for critical cases |
| wasmJs/JS SQLDelight driver issues | Medium | Medium | Test early; fallback to in-memory cache for web |
