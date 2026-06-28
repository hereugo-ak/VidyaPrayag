# Ibibo Farms Clone — Full Product Specification

> **Document Status:** Draft v2.0 (Audit Complete)  
> **Date:** 2026-06-28  
> **Author:** Product Spec Generation  
> **Target Platforms:** Android (primary), iOS (future), Web (future)  

---

## Table of Contents

1. [Product Overview](#1-product-overview)
2. [Target Audience & Market](#2-target-audience--market)
3. [Tech Stack](#3-tech-stack)
4. [System Architecture](#4-system-architecture)
5. [Backend Specification](#5-backend-specification)
6. [Database Schema](#6-database-schema)
7. [Frontend Specification](#7-frontend-specification)
8. [Game Mechanics](#8-game-mechanics)
9. [Social Features](#9-social-features)
10. [Monetization & Revenue Model](#10-monetization--revenue-model)
11. [User Flows](#11-user-flows)
12. [Screen-by-Screen Design](#12-screen-by-screen-design)
13. [API Specification](#13-api-specification)
14. [Push Notifications](#14-push-notifications)
15. [Security & Data Privacy](#15-security--data-privacy)
16. [Analytics & Telemetry](#16-analytics--telemetry)
17. [Testing Strategy](#17-testing-strategy)
18. [Deployment & CI/CD](#18-deployment--cicd)
19. [Development Phases & Milestones](#19-development-phases--milestones)
20. [Risk Register](#20-risk-register)
21. [Review & Approval Model](#21-review--approval-model)
22. [Implementation Questions — Resolved](#22-implementation-questions--resolved)
- [Appendix A: Crop Growth Timer Implementation](#appendix-a-crop-growth-timer-implementation)
- [Appendix B: Farm Grid Data Model](#appendix-b-farm-grid-data-model-jsonb)
- [Appendix C: Localization Keys](#appendix-c-localization-keys-sample)
- [Appendix D: Glossary](#appendix-d-glossary)
- [Appendix E: Content Moderation & Community](#appendix-e-content-moderation--community)

---

## 1. Product Overview

### 1.1 Vision

A modern Android farming simulation game inspired by the original Ibibo Farms (2008–2012). Players plant, grow, harvest, and sell crops; raise livestock; decorate their farms; visit friends' farms; trade in a marketplace; and progress through levels — all with a vibrant, casual, social-gaming experience.

### 1.2 Core Loop

```
Plant Seeds → Wait for Growth Timer → Harvest Crops → Sell at Market → Earn Coins + XP
     ↑                                                              ↓
     ←──── XP → Level Up → Unlock Crops/Animals/Grid ←──────────────┘
     ←────────── Buy More Seeds / Animals / Decor ←─────────────────
     ←────── Complete Quests → Bonus Rewards → Accelerate ←─────────
```

**Loop frequency:** Short crops (30 sec – 5 min) create a rapid engagement loop. Long crops (30–45 min) create return sessions. The mix drives both in-session engagement and daily retention.

### 1.3 Key Differentiators from Original

| Feature | Original Ibibo Farms | This Clone |
|---|---|---|
| Platform | Web browser only | Native Android app |
| Graphics | Simple 2D sprites | Modern 2D with animations & particle effects |
| Offline play | None | Full offline play with sync |
| Monetization | Ads only | Freemium + rewarded ads + in-app purchases |
| Social | ibibo.com friends only | Phone contacts, social sharing, global leaderboards |
| Notifications | None | Push notifications for crop ready, gifts, etc. |
| Livestock | Basic | Full livestock system with breeding (Phase 6) |
| Quests | None | Daily quests, story quests, seasonal events |
| Achievements | None | Badge system with milestones |
| Customer support | None | In-app help center, FAQ, contact support |

---

## 2. Target Audience & Market

### 2.1 Demographics

- **Primary:** 18–45 year-old casual gamers in India and South Asia
- **Secondary:** Global casual farming game audience
- **Tone:** Family-friendly, non-violent, relaxing

### 2.2 Market Positioning

- **Competes with:** Hay Day (Supercell), Township (Playrix), Farm Land, FarmVille successors
- **Key differentiators:**
  - Indian cultural context (regional crops, festivals, seasonal events tied to Indian calendar)
  - Lightweight footprint (< 30 MB vs competitors' 100+ MB)
  - Offline-first architecture (play without internet, sync when connected)
  - Phone OTP onboarding (frictionless for Indian users — no email required)
  - Lower IAP price points (₹49 entry vs competitors' ₹99+ entry)
- **ASO (App Store Optimization):** Keywords: farm game, farming sim, harvest, tractor, village farm, खेत वाला गेम, किसान गेम
- **Customer support:** In-app help center (FAQ + contact form), email support (support@ibibofarms.app), target response time < 48 hours

### 2.3 Success Metrics

| Metric | Target (Month 1) | Target (Month 6) |
|---|---|---|
| Downloads | 50,000 | 500,000 |
| DAU (Daily Active Users) | 5,000 | 50,000 |
| Retention D1 | 40% | 50% |
| Retention D7 | 20% | 30% |
| Retention D30 | 10% | 15% |
| ARPDAU | $0.02 | $0.05 |
| Session length | 5 min | 8 min |

---

## 3. Tech Stack

### 3.1 Client (Android App)

| Layer | Technology | Rationale |
|---|---|---|
| Language | Kotlin | Modern, concise, first-class Android support |
| UI Framework | Jetpack Compose | Declarative UI, animation support, modern Android standard |
| Min SDK | 26 (Android 8.0) | Covers ~95% of Indian Android market |
| Target SDK | 35 (Android 15) | Latest stable |
| Architecture | MVVM + Clean Architecture | Separation of concerns, testability |
| State Management | Compose StateFlow + SharedFlow | Reactive, lifecycle-aware |
| Navigation | Compose Navigation (Type-safe) | Single-activity, type-safe routes |
| Local Database | Room | SQLite wrapper, compile-time query verification, offline-first |
| Remote Sync | WorkManager + Retrofit | Background sync, retry logic |
| Dependency Injection | Hilt (Dagger) | Standard Android DI, compile-time verification |
| Image Loading | Coil | Compose-native, lightweight, GIF support for animations |
| Game Loop Timer | Coroutines + Flow | Lightweight, cancellable, lifecycle-aware |
| Animations | Compose Animations + Lottie | UI transitions, celebration effects, particle systems |
| Audio | Media3 (ExoPlayer successor) | Background music, SFX |
| Push Notifications | Firebase Cloud Messaging (FCM) | Standard, free, reliable |
| Analytics | Firebase Analytics + Mixpanel (optional) | Event tracking, funnels, retention |
| Crash Reporting | Firebase Crashlytics | Real-time crash monitoring |
| Auth | Firebase Auth (phone OTP) + Google Sign-In | Frictionless onboarding for Indian users |
| In-App Purchases | Google Play Billing Library v7 | Standard Android IAP |
| Ads | Google Mobile Ads (AdMob) | Banner, interstitial, rewarded video |

**Additional client libraries (not in table above):**

| Library | Purpose |
|---|---|
| R8 / ProGuard | Code shrinking, obfuscation, minification (release builds) |
| AndroidX Lifecycle | ViewModel, LifecycleOwner, lifecycle-aware components |
| AndroidX DataStore | Preferences (settings, notification prefs) — replaces SharedPreferences |
| Play Integrity API | Anti-cheat: verify app integrity and device attestation |
| Google Play Billing Library v7 | In-app purchases and subscriptions |
| Desugaring (Java 8+) | Enable `java.time` and other APIs on older Android |
| SplashScreen API | Android 12+ splash screen support |
| App Startup | Initialize libraries in correct order |
| Chucker (debug only) | In-app HTTP inspector for debugging API calls |

### 3.2 Backend

| Layer | Technology | Rationale |
|---|---|---|
| Language | Kotlin | Shared language with client, type safety |
| Framework | Ktor | Lightweight, coroutine-native, Kotlin idiomatic |
| Database | PostgreSQL 16 | Relational, ACID, JSONB for flexible game config |
| Cache | Redis 7 | Session cache, leaderboards, real-time presence |
| Object Storage | AWS S3 / Cloudflare R2 | Farm screenshots, profile avatars, asset CDN |
| Message Queue | Redis Streams / RabbitMQ | Async job processing (gift delivery, notifications) |
| Push Delivery | FCM (via backend) | Centralized notification dispatch |
| Auth | JWT + refresh tokens | Stateless, scalable |
| API Style | REST + WebSocket | REST for CRUD, WebSocket for real-time social |
| Deployment | Docker + AWS ECS / Fly.io | Containerized, auto-scaling |
| Connection Pooling | HikariCP | Fast, reliable JDBC connection pooling |
| API Docs | OpenAPI 3.1 (auto-generated via Ktor plugin) | Interactive Swagger UI + ReDoc |
| Email Service | AWS SES / Resend | Transactional emails (receipts, support replies) |
| Background Jobs | Quartz Scheduler / Ktor jobs | Cron: quest reset, listing expiry, notification cleanup, streak reset |

### 3.3 Infrastructure

| Component | Technology |
|---|---|
| Cloud Provider | AWS (primary) / GCP (fallback) |
| Container Orchestration | AWS ECS Fargate |
| Load Balancer | AWS ALB |
| CDN | Cloudflare |
| DNS | Cloudflare DNS |
| Monitoring | Grafana + Prometheus |
| Logs | AWS CloudWatch + Loki |
| CI/CD | GitHub Actions |
| Secrets | AWS Secrets Manager / Doppler |

---

## 4. System Architecture

### 4.1 High-Level Diagram

```
┌─────────────────────────────────────────────────────────┐
│                    Android App (Client)                   │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌─────────┐ │
│  │  Compose  │  │  Game    │  │   Room   │  │  Work   │ │
│  │   UI      │  │  Engine  │  │   DB     │  │ Manager │ │
│  │  Layer    │  │  (Logic) │  │ (Offline)│  │  (Sync) │ │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬────┘ │
│       │              │              │              │      │
│       └──────────────┴──────┬───────┴──────────────┘      │
│                             │                              │
│                    ┌────────▼────────┐                     │
│                    │  API Client     │                     │
│                    │  (Retrofit +    │                     │
│                    │   WebSocket)    │                     │
│                    └────────┬────────┘                     │
└─────────────────────────────┼─────────────────────────────┘
                               │ HTTPS / WSS
                               │
┌──────────────────────────────▼─────────────────────────────┐
│                      Backend (Ktor)                         │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌─────────┐   │
│  │   Auth   │  │  Game    │  │  Social  │  │  Market │   │
│  │ Service  │  │  Service │  │  Service │  │ Service │   │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬────┘   │
│       │              │              │              │        │
│  ┌────▼──────────────▼──────────────▼──────────────▼────┐  │
│  │              PostgreSQL  +  Redis                     │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                            │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────────┐    │
│  │  FCM Push   │  │  S3/R2 Store  │  │  Job Queue     │    │
│  │  Service    │  │  (Assets)     │  │  (Redis Str.)  │    │
│  └─────────────┘  └──────────────┘  └────────────────┘    │
└────────────────────────────────────────────────────────────┘
```

### 4.2 Offline-First Strategy

1. **All game actions write to local Room DB first** — zero latency for the player.
2. **WorkManager syncs changes to backend** when network is available (exponential backoff).
3. **Conflict resolution:** Optimistic concurrency with per-tile last-write-wins. The `farms.version` field tracks the number of accepted actions. On sync, the server compares the client's `baseVersion` against the server's `version` to detect conflicts. Conflicts are resolved per-tile using `clientTimestamp` vs `lastModifiedAt` comparison (see Section 22.1, Q2). Server-authoritative for economy (coins, purchases).
4. **Timers run on device time** — crop growth uses real-world timestamps, not active session time.
5. **If offline > 24h:** On reconnect, server reconciles farm state, validates no cheating (time manipulation detection).
6. **Game config caching:** Crop/animal/decoration/quest definitions are fetched on app launch and cached in Room DB. A `config_version` check (single API call) determines if re-fetch is needed. Full re-fetch only when version bumps.
7. **Asset delivery:** Static assets (sprites, Lottie, audio) served via Cloudflare CDN with long-lived cache headers. Seasonal event assets delivered via Play Asset Delivery (on-demand) to keep base APK small.
8. **WebSocket reconnection:** Exponential backoff (1s, 2s, 4s, 8s, 16s, max 30s). On reconnect: re-send presence, re-subscribe to friend status, replay any queued messages. Max 5 retries before falling back to REST polling (every 30s) for presence.

### 4.3 Game Engine Architecture (Client-Side)

```kotlin
// GameEngine is a Hilt @Singleton — survives Activity/Fragment recreation
// but is lifecycle-aware (pauses timers when app backgrounded)
```

```
┌─────────────────────────────────────────┐
│           GameEngine (Singleton)          │
│                                          │
│  ┌─────────────┐  ┌──────────────────┐  │
│  │  FarmGrid   │  │  CropManager     │  │
│  │  (N×N grid) │  │  (plant/grow/    │  │
│  │             │  │   harvest logic) │  │
│  └─────────────┘  └──────────────────┘  │
│                                          │
│  ┌─────────────┐  ┌──────────────────┐  │
│  │  Livestock  │  │  EconomyManager  │  │
│  │  Manager    │  │  (coins, XP,     │  │
│  │             │  │   shop, market)  │  │
│  └─────────────┘  └──────────────────┘  │
│                                          │
│  ┌─────────────┐  ┌──────────────────┐  │
│  │  QuestMgr   │  │  InventoryMgr    │  │
│  │  (daily,    │  │  (seeds, items,  │  │
│  │   story)    │  │   decorations)   │  │
│  └─────────────┘  └──────────────────┘  │
│                                          │
│  ┌─────────────────────────────────────┐ │
│  │         TimerService                │ │
│  │  (coroutine-based, lifecycle-aware) │ │
│  └─────────────────────────────────────┘ │
└─────────────────────────────────────────┘
```

### 4.4 Rendering Strategy

This game is a **casual, tile-based farming sim** — not a real-time action game. There is no physics engine, no 60fps game loop, no 3D rendering, and no collision detection. The grid is mostly static; only the tapped tile animates. Timers are timestamp-based (`System.currentTimeMillis()` vs `maturesAt`), not frame-based. This makes it fundamentally a **UI app with game-like state management**, which Compose handles natively.

#### 4.4.1 Compose vs Canvas — When to Use Which

| Approach | How It Works | Best For | Trade-off |
|---|---|---|---|
| **Compose Composables** (default) | Each tile is a `Box` with `Image` + `Modifier.clickable`. Compose handles layout, recomposition, and hit-testing. | MVP / Phase 1–2. Grids up to 8×8 (64 tiles). Rapid development, easy to build and debug. | 50+ simultaneously animating tiles (e.g., all showing growth progress bars) can cause excessive recomposition and jank. |
| **Compose Canvas** (optimized) | Entire grid drawn in a single `Canvas` composable using `drawImage`, `drawRoundRect`, etc. Hit-testing via `pointerInput` + manual coordinate mapping. | Phase 3+. Grids up to 12×12 (144 tiles). Many simultaneously animating tiles. | More manual code (hit-testing, coordinate math, invalidation). No automatic recomposition per-tile. |
| **LibGDX interop** (fallback) | Embed a LibGDX `SurfaceView` inside Compose via `AndroidView`. LibGDX handles the rendering loop; Compose handles all non-game UI (shop, settings, profile). | Future: isometric view, custom shaders, heavy particle systems, 3D. | Adds LibGDX dependency (~2MB), steeper learning curve, two rendering paradigms in one app. |

#### 4.4.2 Decision Matrix

```
Is the grid ≤ 8×8 (≤ 64 tiles)?
  ├─ YES → Use Compose Composables (default). Simple, fast enough, easy to build.
  └─ NO → Are most tiles simultaneously animating?
          ├─ NO → Compose Composables still fine. LazyVerticalGrid handles off-screen culling.
          └─ YES → Switch to Compose Canvas. Single draw call, no per-tile recomposition.

Need isometric view, custom shaders, or 3D?
  └─ YES → Use LibGDX interop. Keep Compose for all menus/screens, embed LibGDX only for the farm grid.
```

#### 4.4.3 Architecture for Swappability

The grid rendering is abstracted behind an interface so the renderer can change without touching game logic:

```kotlin
interface FarmGridRenderer {
    @Composable
    fun Render(
        grid: FarmGridState,
        onTileTap: (tileId: String) -> Unit,
        onTileLongPress: (tileId: String) -> Unit,
        modifier: Modifier = Modifier,
    )
}

// Phase 1–2: Default implementation using Compose Composables
class ComposeFarmGridRenderer : FarmGridRenderer { ... }

// Phase 3+: Optimized implementation using Canvas
class CanvasFarmGridRenderer : FarmGridRenderer { ... }

// Future: LibGDX-backed implementation
class LibgdxFarmGridRenderer : FarmGridRenderer { ... }
```

The renderer is injected via Hilt, so switching is a one-line DI change with zero game-logic refactoring.

#### 4.4.4 Animation Strategy

| Animation Type | Technology | Rationale |
|---|---|---|
| Tile tap feedback (scale, ripple) | Compose `Modifier.clickable` (built-in ripple) + `animateScaleAsState` | Native, zero-cost, GPU-accelerated |
| Crop growth progress bar | Compose `Canvas.drawRoundRect` inside the tile composable | Lightweight, only visible tiles recompose |
| Harvest effect (items fly to HUD) | Compose `AnimatedContent` + `animateOffsetAsState` | Simple path animation, 300ms duration |
| Celebration (confetti, level-up) | Lottie (`LottieAnimation` composable) | Pre-made JSON animations, 50–100KB each, one-shot playback |
| Screen transitions | Compose Navigation `AnimatedContent` | Built-in slide/fade transitions |
| Particle effects (dirt, sparkles) | Lottie (preferred) or custom `Canvas` particle system (20–50 particles max) | Lottie is cheaper to produce; Canvas is cheaper at runtime for simple effects |
| Animal idle animations | Animated WebP or sprite-sheet via `AnimatedImageVector` (Coil) | Small file size, loopable, no code needed |

#### 4.4.5 Performance Guardrails

| Guardrail | Threshold | Action |
|---|---|---|
| Frame rate during grid interaction | ≥ 58 fps (target 60) | If drops below, profile with Android Studio Profiler; switch tile composables to Canvas |
| Recomposition count per grid tap | ≤ 3 recompositions | Use `derivedStateOf` + `key` to scope recomposition to tapped tile only |
| App cold start time | < 2 seconds | Lazy-load game assets via Coil; defer non-critical initializations |
| App size (APK/AAB) | < 30 MB | Use WebP for sprites, Play Asset Delivery for seasonal content, R8 minification |
| Memory usage on farm screen | < 150 MB | Recycle off-screen bitmaps; use `LazyVerticalGrid` for tile culling |
| Lottie animation count on screen | ≤ 2 simultaneous | Queue celebration animations; never overlap more than 2 Lottie compositions |

#### 4.4.6 Why Not Unity/LibGDX from the Start?

| Factor | Compose-Only (chosen) | Unity | LibGDX |
|---|---|---|---|
| App size | ~15–25 MB | ~40–80 MB | ~20–30 MB |
| Build complexity | Single Gradle project | Separate Unity project + Android export | Separate module + interop layer |
| UI development speed | Fast (declarative, hot reload) | Slow (UGUI/C#, no hot reload) | Slow (Scene2D, manual layout) |
| Learning curve (for Kotlin devs) | None | High (C#, Unity editor) | Medium (Kotlin-friendly but new API) |
| Suitability for tile-based casual game | ✅ Perfect | ❌ Overkill | ⚠️ Viable but unnecessary |
| Suitability for future 3D/isometric | ❌ Not viable | ✅ Ideal | ✅ Good |
| Monetization SDK integration | Native Android (trivial) | Unity Ads/IAP (separate ecosystem) | Android native (via interop) |

**Conclusion:** Start with Compose. The game's visual complexity does not justify a game engine. If the game evolves to isometric/3D in a future version, migrate the farm grid to LibGDX interop (section 4.4.3 architecture makes this a contained change) or Unity-as-a-library. All non-game screens (shop, market, friends, settings) stay in Compose regardless.

---

## 5. Backend Specification

### 5.1 Service Modules

#### 5.1.1 Auth Service

- **Phone OTP registration/login** (via Firebase Auth)
- **Google Sign-In** as alternative
- **Guest mode** — play without account; upgrade to full account later (farm state migrates)
- **JWT issuance** — access token (15 min TTL) + refresh token (30 day TTL)
- **Device management** — max 3 devices per account

#### 5.1.2 Game Service

- **Farm state sync** — receive farm grid state, validate, persist
- **Crop lifecycle** — server validates plant time, growth duration, harvest eligibility
- **Economy validation** — anti-cheat: verify coin balance, transaction history, purchase legitimacy
- **Level/XP management** — server-authoritative XP calculation
- **Inventory sync** — seeds, harvested crops, decorations, animals

#### 5.1.3 Social Service

- **Friends system** — add by phone contact, username, or friend code
- **Farm visits** — read-only view of friend's farm (cached snapshot)
- **Gifts** — send seeds/items/coins to friends (daily limit: 10 gifts)
- **Leaderboards** — weekly/seasonal, by XP, by farm value, by harvest count
- **Real-time presence** — WebSocket: see which friends are online

#### 5.1.4 Marketplace Service

- **Player-to-player trading** — list items for sale, buy from others
- **NPC shop** — buy seeds, animals, decorations at fixed prices
- **Dynamic pricing** — server adjusts NPC prices based on supply/demand metrics
- **Transaction log** — full audit trail

#### 5.1.5 Notification Service

- **Push dispatch** — crop ready, gift received, friend visited, quest completed, market sale
- **Notification preferences** — per-category opt-in/opt-out
- **Rate limiting** — max 5 push notifications per day per user (configurable)
- **Quiet hours** — user-configurable do-not-disturb window

#### 5.1.6 Admin Service

- **Game config management** — crop definitions, prices, growth times, seasonal events
- **User management** — ban, suspend, refund, search by username/phone
- **Economy monitoring** — inflation tracking, coin supply dashboard, faucet/sink analysis
- **Content management** — seasonal events, quest definitions, shop rotations
- **Admin auth** — separate admin JWT with IP allowlist; 2FA required; audit log for all admin actions

#### 5.1.7 Cron Jobs (Background Scheduled Tasks)

| Job | Schedule | Description |
|---|---|---|
| Daily quest reset | 00:00 local time per user timezone | Expire yesterday's daily quests, assign new set |
| Market listing expiry | Every 1 hour | Expire listings past their 48h window, refund items to seller inventory |
| Gift expiry | Every 1 hour | Expire unaccepted gifts past 24h, return items to sender |
| Streak reset | 00:00 UTC | Reset streak_days to 0 for users who didn't claim reward yesterday |
| Notification cleanup | Daily at 03:00 UTC | Delete notifications older than 30 days |
| Leaderboard snapshot | Every Monday 00:00 UTC | Snapshot weekly leaderboard, reset weekly counters |
| Subscription check | Every 1 hour | Check for expired subscriptions, update is_premium flag |
| Farm value recalc | Every 6 hours | Recalculate farm_value for all users based on current grid contents |
| Seasonal event rotation | Configurable | Activate/deactivate seasonal events based on start/end dates |
| Crop notification dispatch | Every 1 minute | Query `planted_crops` for matured/wilting crops, send FCM pushes (see Section 22.5, Q20/Q21) |
| Quiet hours delivery | Every 5 minutes | Scan Redis quiet_queue for notifications past quiet hours end, deliver via FCM (see Section 22.5, Q22) |

### 5.2 API Design Principles

- RESTful endpoints under `/api/v1/`
- All responses wrapped in `{ "success": Boolean, "data": T?, "error": Error? }`
- Pagination via cursor-based approach (`?cursor=xxx&limit=20`)
- Idempotency keys for write operations (`X-Idempotency-Key` header)
- Rate limiting: 100 req/min per user, 1000 req/min per IP
- WebSocket events under `/ws/v1/` for real-time features
- **API versioning:** URL-based (`/api/v1/`). Breaking changes require new version. Old version maintained for 6 months after new version ships. Deprecation header (`Sunset`) included in responses.
- **Request timeout:** 30 seconds for standard endpoints, 60 seconds for sync/batch endpoints
- **Max request body:** 1 MB (farm state sync)
- **CORS:** Restricted to app package signature + web dashboard domain only

### 5.3 Anti-Cheat System

| Detection | Method | Action |
|---|---|---|
| Time manipulation | Compare device time vs server time on sync | Flag + revert farm state |
| Coin duplication | Server-authoritative balance, double-entry ledger | Reject transaction, flag account |
| Bot farming | Heuristic: action frequency, pattern analysis | Shadow-ban from leaderboards |
| Modified client | App integrity check (Play Integrity API) | Block sync, force update |
| Multi-accounting | Device fingerprint + phone number | Limit rewards per device |

### 5.4 Backup & Disaster Recovery

| Item | Strategy |
|---|---|
| PostgreSQL | Automated daily snapshots (RDS), point-in-time recovery up to 35 days. WAL archiving enabled. |
| Redis | RDB snapshot every 6 hours + AOF for durability. Non-critical data (cache, presence) — acceptable to lose. |
| S3 / R2 | Versioning enabled. Cross-region replication for critical assets. |
| RPO (Recovery Point Objective) | < 1 hour for transactional data, < 24 hours for game state |
| RTO (Recovery Time Objective) | < 4 hours |
| Disaster recovery drill | Quarterly, documented in runbook |
| Multi-AZ | PostgreSQL: Multi-AZ standby. Redis: ElastiCache with replica. Application: ECS across 2 AZs minimum. |

---

## 6. Database Schema

### 6.1 PostgreSQL Schema

```sql
-- ============ USERS ============
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone           VARCHAR(15) UNIQUE,
    email           VARCHAR(255) UNIQUE,
    username        VARCHAR(30) UNIQUE NOT NULL,
    display_name    VARCHAR(50) NOT NULL,
    avatar_url      TEXT,
    level           INT NOT NULL DEFAULT 1,
    xp              BIGINT NOT NULL DEFAULT 0,
    coins           BIGINT NOT NULL DEFAULT 500,  -- starting balance
    gems            INT NOT NULL DEFAULT 10,       -- premium currency
    farm_name       VARCHAR(50) NOT NULL,
    farm_grid_size  INT NOT NULL DEFAULT 6,        -- 6x6 starting grid
    grid_expansion_discount INT NOT NULL DEFAULT 0,  -- one-time discount from Starter Bundle (500 = 500 coins off next expansion)
    friend_code     VARCHAR(6) UNIQUE NOT NULL,      -- 6-char alphanumeric for friend invites
    phone_hash      VARCHAR(64) UNIQUE,              -- SHA-256 hash of normalized phone for contact sync lookup
    discoverable_by_phone BOOLEAN NOT NULL DEFAULT TRUE,  -- user can opt out of phone contact discovery
    total_harvest_count BIGINT NOT NULL DEFAULT 0,  -- for leaderboards
    streak_days     INT NOT NULL DEFAULT 0,         -- daily login streak
    last_reward_claimed_at TIMESTAMPTZ,             -- last daily reward claim
    language        VARCHAR(10) NOT NULL DEFAULT 'en',  -- 'en', 'hi', 'ta', 'te', 'bn', 'mr'
    timezone        VARCHAR(50) NOT NULL DEFAULT 'Asia/Kolkata',  -- IANA timezone for cron scheduling
    notification_prefs JSONB NOT NULL DEFAULT '{"crop_ready":true,"animal_product":true,"gift_received":true,"farm_visited":false,"quest_completed":true,"daily_reward":true,"market_sold":true,"crop_wilting":true,"seasonal_event":false,"level_up":false}'::jsonb,
    quiet_hours_start VARCHAR(5) NOT NULL DEFAULT '22:00',  -- local time HH:MM
    quiet_hours_end   VARCHAR(5) NOT NULL DEFAULT '08:00',  -- local time HH:MM
    is_premium      BOOLEAN NOT NULL DEFAULT FALSE,
    premium_expires_at TIMESTAMPTZ,                -- null if not premium
    is_guest        BOOLEAN NOT NULL DEFAULT FALSE,
    role            VARCHAR(10) NOT NULL DEFAULT 'user',  -- 'user' or 'admin'; admin accounts access admin panel only
    CHECK(role IN ('user', 'admin')),
    is_banned       BOOLEAN NOT NULL DEFAULT FALSE,
    ban_reason      VARCHAR(200),           -- reason shown to user in banned flow
    banned_at       TIMESTAMPTZ,            -- when ban was applied
    ban_until       TIMESTAMPTZ,            -- null = permanent; otherwise temp ban expiry
    deleted_at      TIMESTAMPTZ,            -- soft delete timestamp (null = active)
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_seen_at    TIMESTAMPTZ
);

-- ============ FARMS ============
CREATE TABLE farms (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    grid_data       JSONB NOT NULL,  -- full grid state (tiles, crops, decorations, animals)
    farm_value      BIGINT NOT NULL DEFAULT 0,  -- total asset value for leaderboards
    last_synced_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         INT NOT NULL DEFAULT 1,  -- optimistic concurrency
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id)
);

-- ============ INVENTORY ============
CREATE TABLE inventory_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    item_type       VARCHAR(30) NOT NULL,  -- 'seed', 'crop', 'animal', 'decoration', 'tool'
    item_id         VARCHAR(50) NOT NULL,  -- e.g. 'wheat_seed', 'cow', 'fence'
    quantity        INT NOT NULL DEFAULT 0,
    acquired_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, item_type, item_id)
);

-- ============ CROP DEFINITIONS (Game Config) ============
CREATE TABLE crop_definitions (
    id              VARCHAR(50) PRIMARY KEY,  -- 'wheat', 'rice', 'tomato', etc.
    display_name    VARCHAR(50) NOT NULL,
    category        VARCHAR(20) NOT NULL,  -- 'grain', 'vegetable', 'fruit', 'flower', 'cash_crop'
    seed_cost       INT NOT NULL,
    sell_price      INT NOT NULL,
    growth_time_sec INT NOT NULL,  -- real-world seconds to mature
    xp_reward       INT NOT NULL,
    level_required  INT NOT NULL DEFAULT 1,
    icon_url        TEXT,
    is_seasonal     BOOLEAN NOT NULL DEFAULT FALSE,
    season          VARCHAR(10),  -- 'spring', 'summer', 'monsoon', 'autumn', 'winter'
    is_premium_only BOOLEAN NOT NULL DEFAULT FALSE,  -- premium cosmetic crop variants
    -- Note: Wither multiplier is per-user (2.0 free, 3.0 premium), not per-crop. See Section 8.2.
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============ LIVESTOCK DEFINITIONS ============
CREATE TABLE animal_definitions (
    id              VARCHAR(50) PRIMARY KEY,  -- 'cow', 'chicken', 'goat', 'sheep'
    display_name    VARCHAR(50) NOT NULL,
    buy_cost        INT NOT NULL,
    product_id      VARCHAR(50) NOT NULL,  -- 'milk', 'egg', 'wool'
    product_interval_sec INT NOT NULL,  -- time between product yields
    product_sell_price   INT NOT NULL,
    xp_reward       INT NOT NULL,
    level_required  INT NOT NULL DEFAULT 1,
    feed_cost       INT NOT NULL DEFAULT 0,           -- deprecated: feeding uses 1 crop from inventory (any type). Kept for backward compat.
    feed_interval_sec INT NOT NULL DEFAULT 43200,     -- 12 hours default
    tile_size       INT NOT NULL DEFAULT 1,           -- 1 = 1×1 tile, 2 = 2×2 tiles
    icon_url        TEXT
);

-- ============ TRANSACTIONS (Economy Ledger) ============
CREATE TABLE transactions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type            VARCHAR(20) NOT NULL,  -- 'earn', 'spend', 'gift_sent', 'gift_received', 'purchase', 'reward'
    currency        VARCHAR(10) NOT NULL,  -- 'coins', 'gems'
    amount          BIGINT NOT NULL,       -- positive for credit, negative for debit
    reference_type  VARCHAR(30),  -- 'harvest', 'shop_buy', 'shop_sell', 'market_trade', 'quest', 'iap'
    reference_id    UUID,
    balance_after   BIGINT NOT NULL,  -- running balance for audit
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============ FRIENDSHIPS ============
CREATE TABLE friendships (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id_1       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user_id_2       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status          VARCHAR(15) NOT NULL DEFAULT 'pending',  -- 'pending', 'accepted' (blocking handled via separate block system)
    initiated_by    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    accepted_at     TIMESTAMPTZ,
    UNIQUE(user_id_1, user_id_2),
    CHECK(user_id_1 < user_id_2),  -- ensure consistent ordering
    CHECK(status IN ('pending', 'accepted'))
);

-- ============ GIFTS ============
CREATE TABLE gifts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    receiver_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    item_type       VARCHAR(30) NOT NULL,
    item_id         VARCHAR(50) NOT NULL,
    quantity        INT NOT NULL DEFAULT 1,
    CHECK(quantity > 0),
    message         VARCHAR(200),
    sender_balance_after BIGINT,  -- running balance for anti-cheat audit
    status          VARCHAR(15) NOT NULL DEFAULT 'pending',  -- 'pending', 'accepted', 'expired', 'rejected'
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMPTZ NOT NULL DEFAULT NOW() + INTERVAL '24 hours'
);

-- ============ MARKETPLACE LISTINGS ============
CREATE TABLE market_listings (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seller_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    item_type       VARCHAR(30) NOT NULL,
    item_id         VARCHAR(50) NOT NULL,
    quantity        INT NOT NULL,
    unit_price      INT NOT NULL,  -- in coins
    CHECK(quantity > 0),
    CHECK(unit_price > 0),
    buyer_id        UUID REFERENCES users(id) ON DELETE SET NULL,  -- null until sold or buyer deleted
    status          VARCHAR(15) NOT NULL DEFAULT 'active',  -- 'active', 'sold', 'cancelled', 'expired'
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    sold_at         TIMESTAMPTZ,
    expires_at      TIMESTAMPTZ NOT NULL DEFAULT NOW() + INTERVAL '48 hours'
);

-- ============ QUESTS ============
CREATE TABLE quest_definitions (
    id              VARCHAR(50) PRIMARY KEY,
    display_name    VARCHAR(100) NOT NULL,
    description     TEXT NOT NULL,
    type            VARCHAR(20) NOT NULL,  -- 'daily', 'story', 'seasonal', 'achievement'
    objective_type  VARCHAR(30) NOT NULL,  -- 'harvest_count', 'plant_count', 'sell_count', 'visit_friends', 'reach_level'
    objective_target INT NOT NULL,
    reward_coins    INT NOT NULL DEFAULT 0,
    reward_xp       INT NOT NULL DEFAULT 0,
    reward_gems     INT NOT NULL DEFAULT 0,
    reward_item_type VARCHAR(30),
    reward_item_id  VARCHAR(50),
    reward_item_qty INT,
    level_required  INT NOT NULL DEFAULT 1,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    objectives      JSONB,  -- nullable; story quests use multi-step objectives array, daily quests use single-objective fields above
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE user_quests (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    quest_id        VARCHAR(50) NOT NULL REFERENCES quest_definitions(id),
    progress        INT NOT NULL DEFAULT 0,
    status          VARCHAR(15) NOT NULL DEFAULT 'active',  -- 'active', 'completed', 'claimed'
    assigned_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    assigned_date   DATE,  -- NULL for story/achievement quests; set to CURRENT_DATE for daily quests
    completed_at    TIMESTAMPTZ,
    claimed_at      TIMESTAMPTZ,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
-- Note: For daily quests, assigned_date is set to CURRENT_DATE. For story/achievement quests, assigned_date is NULL.
-- UNIQUE(user_id, quest_id, assigned_date) handles daily quest dedup.
-- UNIQUE(user_id, quest_id) WHERE assigned_date IS NULL handles story/achievement quest dedup.

-- ============ NOTIFICATIONS ============
CREATE TABLE notification_log (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type            VARCHAR(30) NOT NULL,
    title           VARCHAR(100) NOT NULL,
    body            TEXT NOT NULL,
    data            JSONB,
    fcm_message_id  VARCHAR(100),  -- for delivery tracking and dedup
    is_read         BOOLEAN NOT NULL DEFAULT FALSE,
    sent_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    read_at         TIMESTAMPTZ
);

-- ============ IN-APP PURCHASES ============
CREATE TABLE iap_transactions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    product_id      VARCHAR(100) NOT NULL,  -- Google Play product ID
    purchase_token  TEXT NOT NULL,
    purchase_type   VARCHAR(20) NOT NULL,  -- 'coins', 'gems', 'bundle', 'subscription'
    google_order_id VARCHAR(100),  -- Google Play order ID for refund reconciliation
    amount_cents    INT NOT NULL,
    currency_code   VARCHAR(3) NOT NULL DEFAULT 'INR',
    reward_coins    BIGINT,
    reward_gems     INT,
    status          VARCHAR(15) NOT NULL DEFAULT 'pending',  -- 'pending', 'fulfilled', 'failed', 'refunded'
    CHECK(status IN ('pending', 'fulfilled', 'failed', 'refunded')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fulfilled_at    TIMESTAMPTZ
);

-- ============ SEASONAL EVENTS ============
CREATE TABLE seasonal_events (
    id              VARCHAR(50) PRIMARY KEY,  -- 'harvest_festival_2026', 'spring_festival_2026'
    display_name    VARCHAR(100) NOT NULL,
    description     TEXT NOT NULL,
    start_date      TIMESTAMPTZ NOT NULL,
    end_date        TIMESTAMPTZ NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    event_config    JSONB NOT NULL DEFAULT '{}'::jsonb,  -- special crops, quests, decorations, bonuses
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============ ACHIEVEMENTS ============
CREATE TABLE achievement_definitions (
    id              VARCHAR(50) PRIMARY KEY,  -- 'first_harvest', 'animal_collector', 'social_butterfly'
    display_name    VARCHAR(100) NOT NULL,
    description     TEXT NOT NULL,
    category        VARCHAR(30) NOT NULL,  -- 'farming', 'social', 'economic', 'special'
    objective_type  VARCHAR(30),  -- 'harvest_count', 'plant_count', 'earn_coins', 'reach_level', 'streak_days', etc.
    objective_target INT,  -- target value for progress tracking
    icon_url        TEXT,
    tier            VARCHAR(10) NOT NULL DEFAULT 'bronze',  -- 'bronze', 'silver', 'gold', 'platinum'
    xp_reward       INT NOT NULL DEFAULT 0,
    gem_reward      INT NOT NULL DEFAULT 0,
    is_hidden       BOOLEAN NOT NULL DEFAULT FALSE,  -- hidden until unlocked
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE user_achievements (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    achievement_id  VARCHAR(50) NOT NULL REFERENCES achievement_definitions(id),
    unlocked_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, achievement_id)
);

-- ============ AD WATCH LOG ============
CREATE TABLE ad_watch_log (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    placement       VARCHAR(50) NOT NULL,  -- 'shop_coins', 'revive_crop', 'speed_up', 'daily_double', 'free_seed'
    ad_unit_id      VARCHAR(100) NOT NULL,
    reward_type     VARCHAR(20) NOT NULL,  -- 'coins', 'crop_revive', 'speed_up', 'quest_double', 'seed'
    reward_amount   INT NOT NULL DEFAULT 0,
    watched_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============ BLOCKED USERS ============
CREATE TABLE blocked_users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    blocker_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    blocked_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(blocker_id, blocked_id),
    CHECK(blocker_id != blocked_id)  -- can't block yourself
);

-- ============ FRIEND REQUESTS ============
CREATE TABLE friend_requests (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    receiver_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status          VARCHAR(15) NOT NULL DEFAULT 'pending',  -- 'pending', 'accepted', 'rejected', 'expired'
    message         VARCHAR(200),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMPTZ NOT NULL DEFAULT NOW() + INTERVAL '7 days',
    responded_at    TIMESTAMPTZ,
    -- Note: No UNIQUE constraint on (sender_id, receiver_id) because a user should be able
    -- to send a new request after the previous one expired or was rejected.
    -- Use a partial unique index for pending requests only:
    -- CREATE UNIQUE INDEX idx_friend_requests_pending_unique ON friend_requests(sender_id, receiver_id) WHERE status = 'pending';
);

-- ============ DECORATION DEFINITIONS ============
CREATE TABLE decoration_definitions (
    id              VARCHAR(50) PRIMARY KEY,  -- 'fence_wood', 'lamp_post', 'garden_gnome', etc.
    display_name    VARCHAR(50) NOT NULL,
    category        VARCHAR(20) NOT NULL,  -- 'fence', 'path', 'lighting', 'nature', 'misc'
    buy_cost        INT NOT NULL,
    sell_cost       INT NOT NULL,           -- refund value when removed
    tile_size       INT NOT NULL DEFAULT 1, -- 1 = 1×1, 2 = 1×2, etc.
    level_required  INT NOT NULL DEFAULT 1,
    is_premium_only BOOLEAN NOT NULL DEFAULT FALSE,
    icon_url        TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============ BUILDING DEFINITIONS ============
CREATE TABLE building_definitions (
    id              VARCHAR(50) PRIMARY KEY,  -- 'barn', 'silo', 'farmhouse', 'windmill'
    display_name    VARCHAR(50) NOT NULL,
    buy_cost        INT NOT NULL,
    tile_size       INT NOT NULL DEFAULT 1,   -- footprint in tiles (e.g. barn = 4 for 2×2)
    storage_bonus   INT NOT NULL DEFAULT 0,   -- extra inventory capacity
    level_required  INT NOT NULL DEFAULT 1,
    icon_url        TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============ DAILY REWARD CLAIMS (Log) ============
CREATE TABLE daily_reward_claims (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    streak_days     INT NOT NULL,
    CHECK(streak_days >= 0),
    claimed_reward  JSONB NOT NULL,  -- { "coins": 100, "gems": 1, "item": "wheat_seed", "qty": 5 }
    claimed_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
-- Note: This is a claim history log (multiple rows per user). The authoritative streak
-- counter is users.streak_days. users.last_reward_claimed_at tracks the last claim timestamp.

-- ============ FARM VISITS ============
CREATE TABLE farm_visits (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    visitor_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    host_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    actions_performed JSONB NOT NULL DEFAULT '[]'::jsonb,  -- ["watered", "fed", "rated"]
    rating          INT,  -- 1–5 stars, null if not rated
    CHECK(rating IS NULL OR (rating >= 1 AND rating <= 5)),
    visited_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
-- Note: PostgreSQL UNIQUE with expression DATE(visited_at) requires a unique index instead:
-- CREATE UNIQUE INDEX idx_farm_visits_unique ON farm_visits(visitor_id, host_id, DATE(visited_at));

-- ============ FARM RATINGS ============
CREATE TABLE farm_ratings (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    farm_user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rater_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating          INT NOT NULL CHECK(rating >= 1 AND rating <= 5),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(farm_user_id, rater_id)  -- one rating per rater per farm
);

-- ============ DEVICES ============
CREATE TABLE user_devices (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_fingerprint VARCHAR(255) NOT NULL,
    device_name     VARCHAR(100),  -- 'Pixel 7', 'Galaxy S24', etc.
    platform        VARCHAR(20) NOT NULL DEFAULT 'android',
    app_version     VARCHAR(20),
    last_active_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, device_fingerprint)
);

-- ============ SUBSCRIPTION STATUS ============
CREATE TABLE subscription_status (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    plan            VARCHAR(30) NOT NULL DEFAULT 'premium_monthly',
    status          VARCHAR(20) NOT NULL DEFAULT 'active',  -- 'active', 'cancelled', 'expired', 'grace_period'
    CHECK(status IN ('active', 'cancelled', 'expired', 'grace_period')),
    started_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMPTZ NOT NULL,
    auto_renew      BOOLEAN NOT NULL DEFAULT TRUE,
    google_sub_token TEXT NOT NULL,  -- Google Play subscription purchase token
    cancelled_at    TIMESTAMPTZ,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id)
);

-- ============ PLANTED CROPS (Server-Side Push Tracking) ============
-- Projection table for push notification scheduling. NOT the source of truth
-- for farm grid state (that's farms.grid_data JSONB). One row per growing crop,
-- deleted on harvest or wither. See Section 22.5, Q20.
CREATE TABLE planted_crops (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    farm_id         UUID NOT NULL REFERENCES farms(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tile_id         VARCHAR(10) NOT NULL,
    crop_id         VARCHAR(50) NOT NULL REFERENCES crop_definitions(id),
    planted_at      TIMESTAMPTZ NOT NULL,
    matures_at      TIMESTAMPTZ NOT NULL,
    withers_at      TIMESTAMPTZ NOT NULL,
    wither_warning_at TIMESTAMPTZ NOT NULL,
    notification_sent BOOLEAN NOT NULL DEFAULT FALSE,
    wither_notification_sent BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============ SYNCED ACTIONS (Idempotency Log) ============
-- Records each accepted sync-batch action to prevent replay on retry.
-- See Section 22.1, Q1.
CREATE TABLE synced_actions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    action_id       UUID NOT NULL UNIQUE,  -- client-generated actionId
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    action_type     VARCHAR(30) NOT NULL,
    processed_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_synced_actions_user ON synced_actions(user_id, processed_at DESC);

-- ============ LEADERBOARD SNAPSHOTS ============
-- Historical snapshots of weekly/seasonal leaderboards for records.
-- See Section 22.7, Q29.
CREATE TABLE leaderboard_snapshots (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    board_type      VARCHAR(30) NOT NULL,  -- 'weekly_harvest', 'seasonal'
    period          VARCHAR(20) NOT NULL,  -- '2026-W26', 'harvest_festival_2026'
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rank            INT NOT NULL,
    score           BIGINT NOT NULL,
    snapshot_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_leaderboard_snapshots_board ON leaderboard_snapshots(board_type, period, rank);

-- ============ ADMIN AUDIT LOG ============
-- Records all admin panel actions for accountability.
-- See Section 22.7, Q30.
CREATE TABLE admin_audit_log (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_user_id   UUID NOT NULL REFERENCES users(id),
    action          VARCHAR(50) NOT NULL,
    target_type     VARCHAR(30),
    target_id       UUID,
    details         JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_admin_audit_admin ON admin_audit_log(admin_user_id, created_at DESC);
CREATE INDEX idx_admin_audit_target ON admin_audit_log(target_type, target_id);

-- ============ INDEXES ============
-- Existing indexes
CREATE INDEX idx_farms_user_id ON farms(user_id);
CREATE INDEX idx_farms_farm_value ON farms(farm_value DESC);  -- farm value leaderboard
CREATE INDEX idx_inventory_user_id ON inventory_items(user_id);
CREATE INDEX idx_transactions_user_id_created ON transactions(user_id, created_at DESC);
CREATE INDEX idx_transactions_reference ON transactions(reference_type, reference_id);  -- lookup by reference
CREATE INDEX idx_friendships_user1 ON friendships(user_id_1, status);
CREATE INDEX idx_friendships_user2 ON friendships(user_id_2, status);
CREATE INDEX idx_friendships_initiated_by ON friendships(initiated_by, status);  -- pending requests I sent
CREATE INDEX idx_gifts_receiver_status ON gifts(receiver_id, status);
CREATE INDEX idx_gifts_sender_status ON gifts(sender_id, status);  -- sent gifts history
CREATE INDEX idx_market_listings_active ON market_listings(status, item_type, item_id);
CREATE INDEX idx_market_seller_status ON market_listings(seller_id, status);  -- my listings
CREATE INDEX idx_market_expires ON market_listings(expires_at);  -- cron to expire stale listings
CREATE INDEX idx_user_quests_user_active ON user_quests(user_id, status);
CREATE INDEX idx_user_quests_assigned_date ON user_quests(assigned_date);  -- daily quest reset
CREATE INDEX idx_notification_log_user_unread ON notification_log(user_id, is_read);
CREATE INDEX idx_notification_log_sent_at ON notification_log(sent_at);  -- retention cleanup cron
-- New table indexes
CREATE INDEX idx_users_level ON users(level DESC);  -- leaderboard by level
CREATE INDEX idx_users_total_harvest ON users(total_harvest_count DESC);  -- harvest leaderboard
CREATE INDEX idx_daily_reward_claims_user ON daily_reward_claims(user_id, claimed_at DESC);
CREATE INDEX idx_farm_visits_host_date ON farm_visits(host_id, DATE(visited_at));  -- who visited today
CREATE INDEX idx_farm_visits_visitor_date ON farm_visits(visitor_id, DATE(visited_at));  -- my visits today
CREATE INDEX idx_farm_ratings_farm ON farm_ratings(farm_user_id);
CREATE INDEX idx_user_devices_user ON user_devices(user_id);
CREATE INDEX idx_subscription_status_user ON subscription_status(user_id, status);
CREATE INDEX idx_iap_user_status ON iap_transactions(user_id, status);  -- purchase history
CREATE INDEX idx_planted_crops_matures ON planted_crops(matures_at) WHERE notification_sent = FALSE;
CREATE INDEX idx_planted_crops_wither_warn ON planted_crops(wither_warning_at) WHERE wither_notification_sent = FALSE;
CREATE INDEX idx_planted_crops_user ON planted_crops(user_id);
CREATE INDEX idx_iap_purchase_token ON iap_transactions(purchase_token);  -- dedup — same token not redeemed twice
-- New table indexes (added in audit)
CREATE UNIQUE INDEX idx_farm_visits_unique ON farm_visits(visitor_id, host_id, (visited_at::date));  -- 1 visit/day/friend
CREATE INDEX idx_seasonal_events_active ON seasonal_events(is_active, start_date, end_date);
CREATE INDEX idx_user_achievements_user ON user_achievements(user_id);
CREATE INDEX idx_ad_watch_log_user_date ON ad_watch_log(user_id, watched_at);  -- frequency cap enforcement
CREATE INDEX idx_friend_requests_receiver_status ON friend_requests(receiver_id, status);  -- incoming requests
CREATE INDEX idx_friend_requests_sender_status ON friend_requests(sender_id, status);  -- outgoing requests
CREATE INDEX idx_friend_requests_expires ON friend_requests(expires_at);  -- cron to expire old requests
CREATE UNIQUE INDEX idx_friend_requests_pending_unique ON friend_requests(sender_id, receiver_id) WHERE status = 'pending';  -- only 1 pending request per pair
CREATE INDEX idx_blocked_users_blocker ON blocked_users(blocker_id);
CREATE INDEX idx_blocked_users_blocked ON blocked_users(blocked_id);
CREATE UNIQUE INDEX idx_user_quests_daily_unique ON user_quests(user_id, quest_id, assigned_date) WHERE assigned_date IS NOT NULL;  -- daily quest dedup (one per day)
CREATE UNIQUE INDEX idx_user_quests_story_unique ON user_quests(user_id, quest_id) WHERE assigned_date IS NULL;  -- story/achievement quest dedup (no daily reset)
CREATE INDEX idx_gifts_receiver_status ON gifts(receiver_id, status);  -- pending received gifts
CREATE INDEX idx_gifts_sender_status ON gifts(sender_id, status);  -- sent gifts history
CREATE INDEX idx_gifts_expires ON gifts(expires_at);  -- gift expiry cron
CREATE INDEX idx_friendships_user1 ON friendships(user_id_1);
CREATE INDEX idx_friendships_user2 ON friendships(user_id_2);
```

### 6.2 Updated_At Triggers

All tables with `updated_at` columns require a trigger to auto-update on row modification:

```sql
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply to all tables with updated_at:
CREATE TRIGGER trg_users_updated_at BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER trg_farms_updated_at BEFORE UPDATE ON farms FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER trg_inventory_updated_at BEFORE UPDATE ON inventory_items FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER trg_market_listings_updated_at BEFORE UPDATE ON market_listings FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER trg_user_quests_updated_at BEFORE UPDATE ON user_quests FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER trg_subscription_status_updated_at BEFORE UPDATE ON subscription_status FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER trg_iap_transactions_updated_at BEFORE UPDATE ON iap_transactions FOR EACH ROW EXECUTE FUNCTION update_updated_at();
```

### 6.3 Room Database (Client-Side)

Mirrors server schema but simplified for offline use:

- `UserEntity` — local cached profile
- `FarmEntity` — full grid state as JSON
- `InventoryEntity` — local inventory
- `TransactionEntity` — pending sync transactions
- `QuestEntity` — active quests
- `PendingActionEntity` — queue of actions to sync when online
- `CropDefEntity`, `AnimalDefEntity` — cached game config (synced from server)
- `DecorationDefEntity`, `BuildingDefEntity` — cached game config (synced from server)
- `AchievementDefEntity` — cached achievement definitions
- `SeasonalEventEntity` — cached active events
- `DailyRewardClaimEntity` — daily reward claim history (streak snapshot + reward details)
- `BlockedUserEntity` — blocked users list (local cache for offline block enforcement)
- `AdWatchLogEntity` — local ad watch log for frequency cap enforcement offline
- `ConfigVersionEntity` — single-row table tracking last synced game config version
- `PlantedCropEntity` — server-side push tracking table (client cache for local notification scheduling when app is foregrounded)
- `SyncedActionEntity` — idempotency log for sync-batch actions (prevents replay on retry)

---

## 7. Frontend Specification

### 7.1 App Architecture (MVVM + Clean Architecture)

```
┌─────────────────────────────────────────────┐
│                  UI Layer (Compose)           │
│  Screens · Components · Animations · Theme    │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│              ViewModel Layer                  │
│  StateFlow<UiState> · One-off events (Channel)│
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│             Domain Layer (Use Cases)          │
│  PlantCropUseCase · HarvestUseCase ·          │
│  BuySeedUseCase · SellCropUseCase ·           │
│  VisitFriendUseCase · SendGiftUseCase · ...   │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│              Data Layer (Repositories)        │
│  FarmRepository · EconomyRepository ·         │
│  SocialRepository · QuestRepository ·         │
│  (implements both local Room + remote API)    │
└─────────────────────────────────────────────┘
```

### 7.2 Design System

| Element | Specification |
|---|---|
| Primary color | #4CAF50 (farm green) |
| Secondary color | #FF9800 (harvest orange) |
| Accent color | #2196F3 (sky blue) |
| Background | #F1F8E9 (light farm field) |
| Error | #F44336 |
| Typography | Poppins (headings) / Inter (body) |
| Corner radius | 12dp (cards), 24dp (buttons), 50% (avatars) |
| Spacing scale | 4, 8, 12, 16, 24, 32, 48 dp |
| Icon style | Rounded, filled, 24dp default |
| Animation duration | 200ms (micro), 300ms (small), 400ms (large) |
| Dark mode | Supported — earthy dark palette (#1B1B1B bg, #2E7D32 primary) |

### 7.2.1 UI State Management

Every screen must handle 4 states explicitly:

| State | When | UI |
|---|---|---|
| **Loading** | Fetching data (API, Room) | Shimmer placeholder / circular progress indicator |
| **Content** | Data loaded successfully | Normal screen content |
| **Empty** | No data (e.g., no friends, no quests, no market listings) | Illustration + helpful text + CTA button |
| **Error** | API error, network failure, server error | Error illustration + message + "Retry" button |

### 7.2.2 Accessibility

| Requirement | Implementation |
|---|---|
| TalkBack support | All interactive elements have `contentDescription` / `semantics` |
| Minimum touch target | 48dp × 48dp (Material Design guideline) |
| Color contrast | WCAG AA minimum (4.5:1 for text, 3:1 for large text) |
| Font scaling | Respect system font scale (sp units, test at 200%) |
| Reduced motion | Respect `Settings.Global.ANIMATOR_DURATION_SCALE` — skip Lottie/animations if 0 |
| Screen reader | Farm grid tiles announce: crop name, growth stage, time remaining |
| Dynamic type | Compose `MaterialTheme.typography` scales with system settings |

### 7.2.3 Orientation & Form Factor

| Form Factor | Support |
|---|---|
| Phone portrait | Primary — all screens designed for this |
| Phone landscape | Supported — farm grid uses full width, bottom nav becomes side rail |
| Tablet (7"+) | Supported — farm grid centered with max width 600dp, side navigation rail |
| Foldable | Not prioritized for MVP; ensure no layout breaks on hinge fold |

### 7.3 Asset Requirements

| Asset Type | Count (MVP) | Count (Full) | Format |
|---|---|---|---|
| Crop sprites (growth stages) | 5 crops × 4 stages = 20 | 30 crops × 4 = 120 | PNG/WebP, 128×128 |
| Animal sprites | 4 | 15 | PNG/WebP, 128×128 |
| Decoration sprites | 10 | 50 | PNG/WebP, varies |
| Tile backgrounds | 5 (soil, grass, water, path, tilled) | 10 | PNG/WebP, 128×128 |
| UI icons | 30 | 80 | SVG / vector drawables |
| Profile avatars | 10 | 30 | PNG, 256×256 |
| Lottie animations | 5 (harvest, plant, level-up, gift, coin) | 20 | JSON (Lottie) |
| Background music | 2 tracks | 5 tracks | OGG, loopable |
| SFX | 15 | 50 | OGG/WAV, < 50KB each |

---

## 8. Game Mechanics

### 8.1 Farm Grid

- **Starting grid:** 6×6 = 36 tiles
- **Expandable:** Up to 12×12 = 144 tiles (unlocked with coins + level requirements)
- **Tile types:**
  - `GRASS` — default, buildable
  - `TILLED` — prepared for planting
  - `PLANTED` — has a growing crop
  - `WATER` — decorative pond (fish farming later)
  - `PATH` — decorative walkway
  - `BUILDING` — barn, silo, farmhouse (decorations with storage bonuses)

### 8.2 Crop System

#### Crop Lifecycle

```
Empty Tile → Till (instant) → Plant Seed (choose crop) → Growing (real-time timer)
→ Mature (ready to harvest) → Harvest (tap) → Receive crop + XP → Sell or store
```

#### Crop Catalog (MVP)

> Crops marked with **[MVP]** are available in Phase 2. Others are Phase 3+.

| Crop | Category | Seed Cost | Sell Price | Growth Time | XP | Level Req | Phase |
|---|---|---|---|---|---|---|---|
| Wheat | Grain | 5 | 12 | 30 sec | 1 | 1 | MVP |
| Rice | Grain | 10 | 25 | 2 min | 2 | 1 | MVP |
| Tomato | Vegetable | 15 | 35 | 5 min | 4 | 2 | MVP |
| Carrot | Vegetable | 20 | 50 | 10 min | 6 | 3 | MVP |
| Mango | Fruit | 50 | 150 | 30 min | 15 | 5 | MVP |
| Sugarcane | Cash Crop | 30 | 90 | 20 min | 10 | 4 | Phase 3 |
| Cotton | Cash Crop | 40 | 120 | 45 min | 18 | 6 | Phase 3 |
| Sunflower | Flower | 25 | 60 | 15 min | 8 | 3 | Phase 3 |
| Rose | Flower | 35 | 100 | 25 min | 12 | 5 | Phase 3 |
| Chili | Vegetable | 18 | 55 | 8 min | 5 | 2 | MVP |

#### Withering Mechanic

- If a mature crop is not harvested within **2× its growth time** (or **3× for premium subscribers**), it withers.
- Withered crops yield 0 coins and must be cleared (costs 1 coin).
- Premium players can use "Revive" to restore withered crops (costs gems or watch ad).
- **Edge case:** If a player's device time is set backwards after planting, the crop will appear to have been growing longer. Server sync validates actual growth time — if discrepancy > 5 min, server reverts crop to correct stage.

### 8.3 Livestock System

| Animal | Buy Cost | Product | Interval | Product Price | XP | Level Req |
|---|---|---|---|---|---|---|
| Chicken | 100 | Egg | 30 min | 15 | 5 | 3 |
| Cow | 500 | Milk | 1 hr | 40 | 15 | 5 |
| Goat | 300 | Milk (goat) | 45 min | 25 | 10 | 4 |
| Sheep | 800 | Wool | 2 hr | 80 | 25 | 8 |

- Animals occupy 1 tile each (or 2×2 for larger animals in full version).
- Animals need to be "fed" (uses crops from inventory — any crop type, 1 crop per feeding) every 12 hours; unfed animals stop producing.
- **Feeding cost:** 1 crop from inventory (any type). Feeding resets the 12h hunger timer.
- **If unfed > 24h:** Animal becomes "sick" — requires medicine (gem cost) to recover, or auto-recovers when fed.
- **Breeding** (Phase 6): Combine 2 adult, fed animals of same type → offspring after timer. Offspring matures into adult after additional timer.

### 8.4 Leveling & XP

| Level | XP Required (cumulative) | Unlock |
|---|---|---|
| 1 | 0 | Wheat, Rice, Chicken |
| 2 | 50 | Tomato, Chili |
| 3 | 150 | Carrot, Sunflower, Goat |
| 4 | 350 | Sugarcane |
| 5 | 700 | Mango, Rose, Cow |
| 6 | 1,200 | Cotton |
| 7 | 2,000 | Grid expansion to 7×7 (cost: 2,000 coins) |
| 8 | 3,500 | Sheep |
| 9 | 5,500 | Quest slot 4 (4th daily quest) |
| 10 | 8,000 | Grid expansion to 8×8 (cost: 5,000 coins) |
| 11 | 12,000 | Premium decoration: Garden Gnome |
| 12 | 17,000 | Co-house 2 small animals of same type on 1 tile (chicken, goat only) |
| 13 | 23,000 | Market listing slot 5 (5 active listings) |
| 14 | 30,000 | Story Quest Chapter 2 |
| 15 | 40,000 | Grid expansion to 9×9 (cost: 12,000 coins) |
| 16 | 55,000 | Second farm theme (autumn palette) |
| 17 | 75,000 | Premium decoration: Windmill |
| 18 | 100,000 | Market listing slot 10 |
| 19 | 130,000 | Story Quest Chapter 3 |
| 20 | 160,000 | Grid expansion to 10×10 (cost: 25,000 coins) |
| 21 | 200,000 | Story Quest Chapter 4 |
| 22 | 250,000 | Premium decoration: Fountain |
| 23 | 300,000 | Market listing slot 20 |
| 24 | 350,000 | Story Quest Chapter 5 |
| 25 | 400,000 | Third farm theme (winter palette) |
| 26 | 450,000 | Premium decoration: Statue |
| 27 | 500,000 | Quest slot 5 (5th daily quest) |
| 28 | 550,000 | Story Quest Chapter 6 |
| 29 | 600,000 | Premium decoration: Garden Bridge |
| 30 | 700,000 | Grid expansion to 12×12 max (cost: 50,000 coins) |

> **Note:** XP values are cumulative. Level N requires `xp_for_level_N` total XP. XP to advance from level N to N+1 = `xp_for_level_(N+1) - xp_for_level_N`.

**Max level content (30+):** After level 30, XP continues to accumulate for leaderboard ranking but no new gameplay unlocks. Players at max level are incentivized by:
- Seasonal event exclusives
- Achievement hunting
- Leaderboard competition
- Farm decoration prestige (farm value ranking)

### 8.5 Quest System

#### Daily Quests (3 per day, refresh at midnight local time)

| Quest | Objective | Reward |
|---|---|---|
| Daily Harvest | Harvest 10 crops | 50 coins + 20 XP |
| Daily Planter | Plant 15 seeds | 40 coins + 15 XP |
| Daily Seller | Sell 5 items at market | 60 coins + 25 XP |
| Social Butterfly | Visit 3 friends' farms | 30 coins + 10 XP |
| Animal Lover | Collect 5 animal products | 45 coins + 20 XP |

#### Story Quests (linear progression)

- Unlocked at specific levels
- Multi-step objectives (e.g., "Build a barn" → "Fill barn with 5 animals" → "Earn 1000 coins from animal products")
- One-time completion, significant rewards

#### Seasonal Events

- **Harvest Festival (Oct–Nov):** Special crops (pumpkin, sorghum), exclusive decorations
- **Spring Festival (Mar–Apr):** Flower-focused quests, limited-time crops
- **Monsoon Special (Jul–Sep):** Water-dependent crops get growth speed bonus

### 8.6 Economy Balancing

#### Coin Faucets (sources)

| Source | Est. Daily Coins (active player) |
|---|---|
| Crop harvesting | 200–500 |
| Animal products | 100–300 |
| Quest rewards | 150–250 |
| Daily login bonus | 50–200 (streak-based) |
| Gifts from friends | 0–100 |
| Rewarded ads | 50–150 |
| Market trading | Variable |

#### Gem Faucets (sources)

| Source | Est. Daily Gems |
|---|---|
| Daily reward (streak day 5, 7, 14, 21, 28) | 0–5 |
| Achievement unlock | Variable (one-time) |
| Seasonal event rewards | Variable |

#### Coin Sinks (expenditures)

| Sink | Est. Daily Coins |
|---|---|
| Seed purchases | 100–300 |
| Animal feed | 50–100 |
| Decorations | Variable (one-time) |
| Grid expansion | Large one-time costs |
| Market purchases | Variable |

#### Gem Sinks (expenditures)

| Sink | Est. Daily Gems |
|---|---|
| Speed-up crop growth | 1–5 |
| Revive withered crop | 1–3 |
| Premium decorations | Variable (one-time) |
| Grid expansion discount | Variable (one-time) |

**Target inflation rate:** < 5% per month (monitored via admin dashboard).

### 8.7 Daily Reward Tiers

| Streak Day | Reward |
|---|---|
| 1 | 50 coins |
| 2 | 75 coins |
| 3 | 100 coins + 5 wheat seeds |
| 4 | 125 coins |
| 5 | 150 coins + 1 gem |
| 6 | 200 coins |
| 7 | 300 coins + 3 gems + 1 random decoration |
| 8–13 | 150 coins/day (base tier, resets weekly cycle) |
| 14 | 400 coins + 5 gems + 1 premium decoration (if premium) |
| 15–20 | 200 coins/day |
| 21 | 500 coins + 10 gems |
| 22–27 | 250 coins/day |
| 28 | 750 coins + 15 gems + exclusive seasonal decoration |
| 29+ | 300 coins/day (ongoing, resets to tier 1 equivalent monthly) |

- **Streak break:** If a player misses a day, streak resets to 0. No "streak freeze" mechanic (keeps it simple).
- **Premium bonus:** Premium subscribers get +50% coins on all daily rewards.
- **Source of truth:** `users.streak_days` is the authoritative streak counter. `daily_reward_claims` table logs claim history only (does not store a separate streak count).

### 8.8 Inventory Capacity

| Item Type | Free Capacity | With Barn Building | With Barn + Silo | Premium |
|---|---|---|---|---|
| Seeds | 50 per type | 100 per type | 200 per type | Unlimited |
| Harvested crops | 100 total | 250 total | 500 total | Unlimited |
| Animal products | 50 total | 100 total | 200 total | Unlimited |
| Decorations | 30 total | 60 total | 100 total | Unlimited |
| Tools | 10 per type | 20 per type | 50 per type | Unlimited |

- **When inventory is full:** Player cannot harvest/sell-to-inventory. UI shows "Inventory Full" warning with CTA to sell at market or upgrade storage.
- **Buildings increase capacity:** Barn (+150 crop slots), Silo (+100 seed slots), Farmhouse (+50 decoration slots).

---

## 9. Social Features

### 9.1 Friends System

- **Add friends by:** Phone contact sync, username search, friend code (6-char alphanumeric, auto-generated on account creation, regenerable once per 30 days), QR code
- **Friend limit:** 50 (free), 200 (premium)
- **Friend list:** Shows name, avatar, level, online status, last active
- **Friend requests:** Expire after 7 days if not responded. Max 20 pending outgoing requests. Max 50 pending incoming requests.
- **Block / Report:** Users can block other users (removes friendship, prevents future requests, hides from leaderboards). Report users for inappropriate farm names, display names, or behavior. Reports reviewed by admin within 48 hours.

### 9.2 Farm Visits

- Tap a friend → load their farm grid (read-only snapshot)
- **Interactions:** Water their crops (speeds growth by 10% for all growing crops on their farm, once per friend per day), feed their animals (restores 12h feed timer, once per friend per day)
- **Watering mechanics:** Watering reduces remaining growth time by 10% for all currently growing crops on the visited farm. Does not affect already-mature crops. Stacks with monsoon bonus (additive, not multiplicative).
- **Reward for visiting:** 5 coins + 2 XP per visit (max 10 visits/day for rewards)
- **Farm rating:** Visitors can rate farm (1–5 stars); average displayed on profile

### 9.3 Gifting

- Send: seeds, harvested crops, decorations, coins (limited)
- **Daily gift limit:** 10 gifts sent, unlimited received
- **Gift expiry:** 24 hours to accept, then auto-expire
- **Gift notification:** Push notification when gift received

### 9.4 Leaderboards

| Board | Scope | Metric | Reset |
|---|---|---|---|
| Weekly Harvest Champion | Global | Total harvests this week | Weekly (Monday) |
| Farm Value Top 100 | Global | Total farm asset value | Never (all-time) |
| Friends Leaderboard | Friends only | XP / level | Never |
| Seasonal Event | Global | Event-specific metric | End of season |

### 9.5 Chat (Phase 6)

- **Quick messages only** — pre-defined templates and emojis (no free-text to moderate)
- 20 template messages (e.g., "Nice farm!", "Thanks for watering!", "Harvest ready!")
- Farm visit comments (leave a quick message on friend's farm — also templates only)
- No 1:1 free-text chat (moderation cost too high for casual game)

---

## 10. Monetization & Revenue Model

### 10.1 Revenue Streams

| Stream | Mechanism | Est. % of Revenue |
|---|---|---|
| In-App Purchases (IAP) | Buy coins, gems, bundles | 60% |
| Rewarded Video Ads | Watch ad for coins/revive/speed-up | 25% |
| Banner Ads | Small banner on non-game screens (shop, market) | 5% |
| Subscription (optional) | "Premium Farmer" monthly sub | 10% |

### 10.2 In-App Purchase Catalog

| Product ID | Name | Price (INR) | Contents |
|---|---|---|---|
| coins_small | Pocket of Coins | ₹49 | 500 coins |
| coins_medium | Bag of Coins | ₹99 | 1,200 coins (+20% bonus) |
| coins_large | Chest of Coins | ₹199 | 3,000 coins (+50% bonus) |
| coins_mega | Vault of Coins | ₹499 | 8,000 coins (+60% bonus) |
| gems_small | handful of Gems | ₹99 | 20 gems |
| gems_medium | Pile of Gems | ₹299 | 75 gems (+25% bonus) |
| starter_bundle | Starter Bundle | ₹149 | 1,000 coins + 10 gems + 500-coin grid expansion discount |
| premium_monthly | Premium Farmer (Monthly) | ₹99/mo | See below |

### 10.3 Premium Farmer Subscription

| Perk | Details |
|---|---|
| No ads | All ads removed |
| Daily bonus | +50% daily login bonus |
| Exclusive crops | 2 premium-only cosmetic crops per season (visual variants only, same stats as free crops) |
| Extended harvest window | 3× growth time before withering (vs 2×) |
| Gift limit | 20 gifts/day (vs 10) |
| Friend limit | 200 (vs 50) |
| Exclusive decorations | Monthly premium decoration |
| Priority sync | Faster server sync interval |

### 10.4 Rewarded Ad Placements

| Trigger | Reward | Frequency Cap |
|---|---|---|
| "Watch ad for coins" button (shop) | 50 coins | 5×/day |
| Crop withered → "Revive by watching ad" | Restore crop | 3×/day |
| "Speed up growth" (any growing crop) | Instant mature | 3×/day |
| Daily double rewards (quest claim) | 2× quest reward | 1×/day |
| Free seed of the day | Random seed ×5 | 1×/day |

### 10.5 Economy Guardrails

- **Never sell power:** All purchasable items are cosmetic or convenience — no exclusive gameplay advantage that free players can't eventually earn.
- **Gem exclusivity:** Gems used only for: speed-ups, premium decorations, revive, grid expansion discount. Never for direct gameplay progression.
- **Free-to-play viability:** A free player can reach max level and unlock all gameplay content — it just takes longer.

### 10.6 Refund Policy

| Scenario | Policy |
|---|---|
| Accidental purchase | Refund within 48 hours if coins/gems unspent. Contact support via in-app help. |
| Subscription cancellation | Pro-rated refund for remaining days if cancelled within 24 hours of renewal. |
| Banned account | No refunds for IAP if ban is for cheating. Refund for unused subscription period. |
| Google Play refund | Handled via Google Play's standard refund flow. Server verifies revocation. |
| Chargeback | Account suspended pending resolution. Items purchased with charged-back coins removed. |

### 10.7 Subscription Grace Period & Billing Retry

- **Grace period:** 3 days after subscription expires. Player retains premium benefits during grace period.
- **Billing retry:** Google Play retries failed payments for up to 30 days (Google's standard). Server checks `subscription_status` via Google Play Developer API.
- **Account hold:** If payment fails after grace period, premium benefits removed. Player retains all progress and items earned during premium period.
- **Promo offers:** First-time subscriber discount (₹49 for first month, then ₹99/month). Limited-time gem sales (e.g., 2× gems for 48 hours). Triggered by analytics: users who opened gem shop 3× but didn't purchase.

---

## 11. User Flows

### 11.1 First-Time User Onboarding

```
App Launch
  │
  ├─→ Splash Screen (2s, logo animation)
  │
  ├─→ Permission Requests (notifications, storage — with rationale)
  │
  ├─→ Auth Screen
  │     ├─→ "Continue with Google" (one-tap)
  │     ├─→ "Continue with Phone" (OTP flow)
  │     └─→ "Play as Guest" (no account, can upgrade later)
  │
  ├─→ Avatar Selection (choose from 10 preset avatars)
  │
  ├─→ Farm Name Input ("Name your farm!")
  │
  ├─→ Interactive Tutorial (guided, 5 steps):
  │     1. "Tap a grass tile to till the soil" (highlights first tile)
  │     2. "Open the shop and buy wheat seeds" (opens shop, pre-selected)
  │     3. "Tap the tilled soil to plant" (plants automatically)
  │     4. "Wait for it to grow... (30 sec — sped up for tutorial)"
  │     5. "Tap the mature crop to harvest! Congratulations!"
  │
  ├─→ Tutorial Reward: 100 coins + 50 XP
  │
  └─→ Farm Home Screen (free play begins)
```

### 11.2 Core Gameplay Flow

```
Farm Home Screen
  │
  ├─→ Tap empty tile → Tile Action Sheet
  │     ├─→ "Till Soil" (free, instant)
  │     ├─→ "Place Decoration" (opens decoration inventory)
  │     └─→ "Build" (opens building menu — barn, silo, etc.)
  │
  ├─→ Tap tilled tile → Seed Selection Sheet
  │     ├─→ Shows available seeds (filtered by level)
  │     ├─→ Shows growth time, sell price, XP
  │     └─→ Confirm → Seed planted, timer starts
  │
  ├─→ Tap growing crop → Crop Info Popup
  │     ├─→ Shows time remaining
  │     ├─→ "Speed up" (watch ad or use gems)
  │     └─→ Cancel (close popup)
  │
  ├─→ Tap mature crop → Harvest!
  │     ├─→ Animation: crop harvested, coins/XP fly to HUD
  │     ├─→ Crop added to inventory
  │     └─→ Tile returns to tilled state
  │
  ├─→ Tap animal → Animal Action Sheet
  │     ├─→ If product ready: "Collect" (adds product to inventory)
  │     ├─→ If hungry: "Feed" (consumes crop from inventory)
  │     └─→ Info: shows next product time
  │
  ├─→ Bottom Nav Bar:
  │     ├─→ 🏠 Farm (current screen)
  │     ├─→ 🛒 Shop (buy seeds, animals, decorations)
  │     ├─→ 📋 Quests (daily + story quest list)
  │     ├─→ 👥 Friends (friend list, visit farms, gifts)
  │     └─→ 🏪 Market (player-to-player trading)
  │
  └─→ Top HUD Bar:
        ├─→ Coins balance (tap to open coin shop)
        ├─→ Gems balance (tap to open gem shop)
        ├─→ Level + XP progress bar
        └─→ Settings gear (settings, profile, help)
```

### 11.3 Social Flow — Visit Friend

```
Friends Tab
  │
  ├─→ Friend List (sorted by online status, then level)
  │
  ├─→ Tap friend → Friend Farm Screen (loads snapshot)
  │     ├─→ Read-only farm grid
  │     ├─→ "Water crops" button (if crops growing — 1×/day/friend)
  │     ├─→ "Feed animals" button (if animals hungry — 1×/day/friend)
  │     ├─→ "Rate farm" (1–5 stars)
  │     ├─→ "Send gift" button → Gift selection sheet
  │     └─→ "Leave a note" (quick message — full version)
  │
  └─→ Back → Returns to Friends Tab
```

### 11.4 Market Trading Flow

```
Market Tab
  │
  ├─→ "Buy" tab → Browse listings
  │     ├─→ Filter by item type, sort by price
  │     ├─→ Tap listing → Confirm purchase (deducts coins)
  │     └─→ Item added to inventory
  │
  └─→ "Sell" tab → Create listing
        ├─→ Select item from inventory
        ├─→ Set quantity + unit price
        ├─→ Confirm → Listing created (active for 48 hours)
        └─→ Notification when sold
```

### 11.5 Offline → Online Sync Flow

```
Player performs actions while offline
  │
  ├─→ All actions saved to Room DB + PendingActionEntity queue
  │
  ├─→ WorkManager detects network available
  │
  ├─→ Batch sync:
  │     ├─→ Send pending actions to server
  │     ├─→ Server validates each action (anti-cheat)
  │     ├─→ Server returns updated state (coins, XP, inventory)
  │     └─→ Client updates Room DB with server-confirmed state
  │
  ├─→ Conflict handling:
  │     ├─→ If time manipulation detected → revert to server state, show warning
  │     ├─→ If coin discrepancy → server-authoritative, adjust client
  │     └─→ If farm state conflict → last-write-wins for decorations, server-authoritative for economy
  │
  └─→ Sync complete → UI refreshes with confirmed state
```

### 11.6 Guest → Full Account Upgrade Flow

```
Guest player taps "Save your progress" banner (or tries to access social features)
  │
  ├─→ Auth Screen (same as onboarding, but pre-filled)
  │     ├─→ "Continue with Google" → Link Google account to guest
  │     └─→ "Continue with Phone" → OTP → Link phone to guest
  │
  ├─→ Server: migrate guest farm state, coins, XP, inventory to new account
  │
  ├─→ Guest account marked as "upgraded" (not deleted, for audit trail)
  │
  └─→ Full account active → Social features unlocked
```

### 11.7 Account Deletion Flow

```
Settings → Account → "Delete Account"
  │
  ├─→ Warning screen: "This will permanently delete your farm, coins, items, and history. This cannot be undone."
  │
  ├─→ Confirmation: "Type DELETE to confirm" (text input)
  │
  ├─→ If premium subscription active: "Your subscription will be cancelled. No refund for remaining days."
  │
  ├─→ Server: mark account for deletion (soft delete)
  │     ├─→ Farm data, inventory, friendships removed immediately
  │     ├─→ Transaction records retained for 90 days (legal compliance)
  │     ├─→ User removed from leaderboards immediately
  │     └─→ FCM token unregistered
  │
  └─→ App navigates to Auth screen → "Account deleted" toast
```

### 11.8 Banned User Flow

```
Banned user opens app
  │
  ├─→ Server returns 403 with error code "ACCOUNT_BANNED"
  │
  ├─→ App shows full-screen banned notice:
  │     "Your account has been suspended for violating our terms.
  │      Reason: {ban_reason}
  │      Duration: {ban_duration or 'permanent'}
  │      If you believe this is an error, contact support@ibibofarms.app"
  │
  └─→ All API calls blocked. App cannot proceed past this screen.
```

### 11.9 Subscription Cancellation Flow

```
Settings → Account → "Manage Subscription"
  │
  ├─→ Opens Google Play Subscription management page (deep link)
  │
  ├─→ User cancels in Google Play
  │
  ├─→ Server receives RTDN (Real-Time Developer Notification) from Google Play
  │     ├─→ subscription_status updated to 'cancelled' (still active until expires_at)
  │     └─→ User retains premium benefits until expires_at
  │
  ├─→ On expires_at:
  │     ├─→ is_premium set to FALSE
  │     ├─→ Premium-only decorations remain (kept as earned)
  │     ├─→ Premium-only crops: currently growing ones finish, cannot plant new ones
  │     └─→ Friend/gift limits revert to free tier
  │
  └─→ Push notification: "Your Premium Farmer subscription has ended. Resubscribe to keep your benefits!"
```

---

## 12. Screen-by-Screen Design

### 12.1 Screen Inventory

| # | Screen | Route | Description |
|---|---|---|---|
| 1 | Splash | `/splash` | Animated logo, auto-transition |
| 2 | Auth | `/auth` | Google/Phone/Guest options |
| 3 | Phone OTP | `/auth/otp` | 6-digit OTP entry |
| 4 | Avatar Selection | `/onboarding/avatar` | Grid of preset avatars |
| 5 | Farm Naming | `/onboarding/name` | Text input + random name button |
| 6 | Tutorial | `/tutorial` | Guided overlay on farm screen |
| 7 | Farm Home | `/farm` | Main game screen — grid + HUD + nav |
| 8 | Shop | `/shop` | Tabs: Seeds, Animals, Decorations, Bundles |
| 9 | Quests | `/quests` | Daily quests + story quests + claim rewards |
| 10 | Friends | `/friends` | Friend list, add friends, online status |
| 11 | Friend Farm | `/friends/{id}/farm` | Read-only friend farm visit |
| 12 | Gift Center | `/gifts` | Send/receive gifts |
| 13 | Market | `/market` | Buy/sell tabs, listings |
| 14 | Profile | `/profile` | User stats, farm value, achievements |
| 15 | Settings | `/settings` | Notifications, sound, language, account |
| 16 | Coin Shop | `/shop/coins` | IAP coin packages |
| 17 | Gem Shop | `/shop/gems` | IAP gem packages |
| 18 | Premium Subscribe | `/premium` | Subscription benefits + subscribe |
| 19 | Leaderboard | `/leaderboard` | Global + friends leaderboards |
| 20 | Achievements | `/achievements` | Badge grid, locked/unlocked |
| 21 | Help & FAQ | `/help` | Common questions, contact support |
| 22 | Seasonal Event | `/event/{id}` | Event-specific screen, special quests/shop |
| 23 | Daily Reward | `/daily-reward` | Streak calendar, claim today's reward |
| 24 | Friend Requests | `/friends/requests` | Pending incoming/outgoing friend requests |
| 25 | Account Settings | `/settings/account` | Change username, avatar, delete account, manage subscription |
| 26 | Notification Preferences | `/settings/notifications` | Per-category toggles, quiet hours config |
| 27 | Blocked Users | `/settings/blocked` | List of blocked users, unblock action |
| 28 | Edit Profile | `/profile/edit` | Change display name, avatar, farm name |
| 29 | Support / Contact | `/support` | Contact form, bug report, attach screenshot |

### 12.2 Farm Home Screen Layout

```
┌─────────────────────────────────────┐
│  🪙 1,250   💎 12   Lvl 7 ████░░  │  ← Top HUD
├─────────────────────────────────────┤
│                                     │
│   ┌───┬───┬───┬───┬───┬───┐       │
│   │ 🌾│ 🌾│ 🟫│ 🟫│ 🌿│ 🐔│       │  ← Farm Grid
│   ├───┼───┼───┼───┼───┼───┤       │     (scrollable
│   │ 🟫│ 🌻│ 🌻│ 🟫│ 🟫│ 🟫│       │      if larger
│   ├───┼───┼───┼───┼───┼───┤       │      than screen)
│   │ 🐄│ 🟫│ 🌾│ 🌾│ 🌾│ 🟫│       │
│   ├───┼───┼───┼───┼───┼───┤       │
│   │ 🟫│ 🟫│ 🟫│ 🌹│ 🌹│ 🟫│       │
│   ├───┼───┼───┼───┼───┼───┤       │
│   │ 🌿│ 🌿│ 🟫│ 🟫│ 🏠│ 🟫│       │
│   ├───┼───┼───┼───┼───┼───┤       │
│   │ 🟫│ 🟫│ 🐐│ 🟫│ 🟫│ 🟫│       │
│   └───┴───┴───┴───┴───┴───┘       │
│                                     │
├─────────────────────────────────────┤
│  🏠 Farm  🛒 Shop  📋 Quests        │  ← Bottom Nav
│  👥 Friends  🏪 Market              │
└─────────────────────────────────────┘
```

---

## 13. API Specification

### 13.1 REST Endpoints

#### Auth

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/auth/register` | Register with phone/email + OTP |
| POST | `/api/v1/auth/verify-otp` | Verify OTP, get JWT tokens |
| POST | `/api/v1/auth/google` | Google Sign-In callback |
| POST | `/api/v1/auth/guest` | Create guest account |
| POST | `/api/v1/auth/upgrade-guest` | Convert guest to full account |
| POST | `/api/v1/auth/refresh` | Refresh access token |
| POST | `/api/v1/auth/logout` | Invalidate refresh token |
| POST | `/api/v1/auth/ws-ticket` | Get short-lived WebSocket auth ticket (30s TTL) |

#### Farm

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/farm` | Get current user's farm state |
| PUT | `/api/v1/farm` | Sync full farm state (with version) |
| POST | `/api/v1/farm/sync-batch` | Batch sync offline actions (max 500, action replay) |
| POST | `/api/v1/farm/plant` | Plant a crop on a tile |
| POST | `/api/v1/farm/harvest` | Harvest a mature crop |
| POST | `/api/v1/farm/till` | Till a grass tile |
| POST | `/api/v1/farm/place` | Place decoration/animal/building |
| POST | `/api/v1/farm/remove` | Remove item from tile |
| POST | `/api/v1/farm/expand` | Expand grid size |
| POST | `/api/v1/farm/animal/feed` | Feed own animal (consumes crop from inventory) |
| POST | `/api/v1/farm/animal/collect` | Collect animal product (adds to inventory) |
| POST | `/api/v1/farm/speed-up` | Speed up crop growth (gems or ad reward) |
| POST | `/api/v1/farm/revive` | Revive withered crop (gems or ad reward) |
| GET | `/api/v1/farm/{userId}` | Get friend's farm snapshot (read-only) |

#### Economy

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/economy/balance` | Get coin + gem balance |
| GET | `/api/v1/economy/transactions` | Get transaction history (paginated) |
| POST | `/api/v1/economy/shop/buy` | Buy from NPC shop |
| POST | `/api/v1/economy/shop/sell` | Sell item to NPC shop |
| GET | `/api/v1/economy/shop/catalog` | Get current shop catalog + prices |

#### Inventory

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/inventory` | Get full inventory |
| POST | `/api/v1/inventory/sync` | Batch sync inventory changes |

#### Social

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/friends` | Get friend list |
| POST | `/api/v1/friends/add` | Send friend request (by username, phone, or friend code) |
| POST | `/api/v1/friends/accept` | Accept friend request |
| POST | `/api/v1/friends/reject` | Reject friend request |
| DELETE | `/api/v1/friends/{id}` | Remove friend |
| GET | `/api/v1/friends/requests` | Get pending friend requests (incoming + outgoing) |
| POST | `/api/v1/friends/block/{userId}` | Block a user |
| POST | `/api/v1/friends/unblock/{userId}` | Unblock a user |
| GET | `/api/v1/friends/blocked` | Get blocked users list |
| POST | `/api/v1/friends/visit/{userId}` | Record farm visit |
| POST | `/api/v1/friends/water/{userId}` | Water friend's crops |
| POST | `/api/v1/friends/feed/{userId}` | Feed friend's animals |
| POST | `/api/v1/friends/rate/{userId}` | Rate friend's farm |
| GET | `/api/v1/friends/search` | Search users by username or friend code (preview before sending request) |
| POST | `/api/v1/friends/contacts-lookup` | Batch lookup hashed phone numbers for contact sync |
| GET | `/api/v1/friends/code` | Get own friend code |
| POST | `/api/v1/friends/code/regenerate` | Regenerate friend code (max once per 30 days) |

#### Gifts

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/gifts/received` | Get received gifts (pending + history) |
| GET | `/api/v1/gifts/sent` | Get sent gifts history |
| POST | `/api/v1/gifts/send` | Send a gift |
| POST | `/api/v1/gifts/{id}/accept` | Accept a gift |
| POST | `/api/v1/gifts/{id}/reject` | Reject a gift |

#### Market

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/market/listings` | Browse market listings (filters, pagination) |
| GET | `/api/v1/market/my-listings` | Get own listings (active + history) |
| POST | `/api/v1/market/listings` | Create a listing |
| DELETE | `/api/v1/market/listings/{id}` | Cancel own listing |
| POST | `/api/v1/market/listings/{id}/buy` | Buy from a listing |

#### Quests

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/quests/active` | Get active quests + progress |
| GET | `/api/v1/quests/daily` | Get today's daily quests |
| GET | `/api/v1/quests/story` | Get story quests + progress |
| POST | `/api/v1/quests/{id}/claim` | Claim quest reward |

#### Leaderboards

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/leaderboard/weekly` | Weekly harvest leaderboard |
| GET | `/api/v1/leaderboard/farm-value` | All-time farm value leaderboard |
| GET | `/api/v1/leaderboard/friends` | Friends-only leaderboard |
| GET | `/api/v1/leaderboard/seasonal` | Seasonal event leaderboard |

#### IAP

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/iap/verify` | Verify Google Play purchase token |
| POST | `/api/v1/iap/subscribe` | Subscribe to premium |
| GET | `/api/v1/iap/products` | Get available IAP products |
| POST | `/api/v1/iap/subscription/cancel` | Cancel premium subscription |
| GET | `/api/v1/iap/subscription/status` | Get current subscription status |

#### Daily Rewards

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/daily-reward/status` | Get streak status + today's reward |
| POST | `/api/v1/daily-reward/claim` | Claim today's daily reward |

#### Achievements

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/achievements` | Get all achievement definitions + user unlock status |

#### Profile

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/profile` | Get own profile (stats, farm value, achievements count) |
| GET | `/api/v1/profile/{userId}` | Get public profile (read-only) |
| PUT | `/api/v1/profile` | Update profile (display name, avatar, farm name) |
| POST | `/api/v1/profile/avatar` | Upload avatar image |

#### Ads

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/ads/reward` | Report ad watched (server validates + grants reward) |
| GET | `/api/v1/ads/caps` | Get remaining ad watch counts for today |
| GET | `/api/v1/ads/available` | Check if ads are loaded (fill rate) for each placement |

#### Account

| Method | Path | Description |
|---|---|---|
| DELETE | `/api/v1/account` | Delete account (soft delete) |
| GET | `/api/v1/account/export` | Export user data (GDPR/DPDP compliance) |
| POST | `/api/v1/account/report` | Report another user |

#### Notifications

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/notifications` | Get notification list (paginated, filterable by type) |
| GET | `/api/v1/notifications/preferences` | Get notification preferences + quiet hours |
| PUT | `/api/v1/notifications/preferences` | Update notification preferences + quiet hours |
| GET | `/api/v1/notifications/unread-count` | Get unread notification count (for badge) |
| POST | `/api/v1/notifications/{id}/read` | Mark notification as read |
| POST | `/api/v1/notifications/read-all` | Mark all notifications as read |

#### Events

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/events` | Get active seasonal events (user-facing, with event-specific quests/shop) |
| GET | `/api/v1/events/{id}` | Get event details (quests, special crops, decorations, leaderboard) |

#### Game Config

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/config/version` | Get current config version (for cache check) |
| GET | `/api/v1/config/crops` | Get all crop definitions |
| GET | `/api/v1/config/animals` | Get all animal definitions |
| GET | `/api/v1/config/quests` | Get all quest definitions |
| GET | `/api/v1/config/events` | Get active seasonal events (config cache) |
| GET | `/api/v1/config/decorations` | Get all decoration definitions |
| GET | `/api/v1/config/buildings` | Get all building definitions |
| GET | `/api/v1/config/achievements` | Get all achievement definitions |
| GET | `/api/v1/config/levels` | Get level/XP table |
| GET | `/api/v1/config/daily-rewards` | Get daily reward tier table |

#### Admin (separate web app, IP allowlist + 2FA required)

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/admin/login` | Admin login (email + password + 2FA TOTP) |
| GET | `/api/v1/admin/users` | Search/list users (filter by username, phone, ban status) |
| POST | `/api/v1/admin/users/{id}/ban` | Ban user (specify reason + duration) |
| POST | `/api/v1/admin/users/{id}/unban` | Unban user |
| GET | `/api/v1/admin/economy` | Economy dashboard data (coin supply, faucet/sink, inflation rate) |
| GET | `/api/v1/admin/flagged` | Flagged accounts (cheating, market manipulation, multi-account) |
| GET | `/api/v1/admin/config/crops` | View crop definitions |
| PUT | `/api/v1/admin/config/crops/{id}` | Update crop definition |
| POST | `/api/v1/admin/config/crops` | Create new crop definition |
| GET | `/api/v1/admin/config/quests` | View quest definitions |
| PUT | `/api/v1/admin/config/quests/{id}` | Update quest definition |
| GET | `/api/v1/admin/events` | View/manage seasonal events |
| POST | `/api/v1/admin/events` | Create seasonal event |
| PUT | `/api/v1/admin/events/{id}` | Update seasonal event |
| GET | `/api/v1/admin/audit-log` | View admin action audit log |

### 13.2 WebSocket Events

#### Client → Server

| Event | Payload | Description |
|---|---|---|
| `presence` | `{ status: "online" }` | Announce online status |
| `farm_action` | `{ action, tileId, ... }` | Real-time farm action (for friend viewing) |
| `chat_message` | `{ toUserId, message }` | Send quick message to friend |

#### Server → Client

| Event | Payload | Description |
|---|---|---|
| `gift_received` | `{ fromUser, itemType, itemId, qty }` | Real-time gift notification |
| `friend_online` | `{ userId }` | Friend came online |
| `friend_offline` | `{ userId }` | Friend went offline |
| `market_sold` | `{ listingId, amount }` | Your market listing sold |
| `farm_visited` | `{ byUser }` | Someone visited your farm |
| `quest_completed` | `{ questId }` | Quest objective completed |
| `chat_message` | `{ fromUser, message }` | Received a quick message |
| `subscription_expired` | `{ }` | Premium subscription ended (benefits removed) |
| `config_updated` | `{ newVersion }` | Game config version changed (client should re-fetch) |

### 13.3 Standard API Response Format

```json
{
  "success": true,
  "data": { ... },
  "error": null,
  "meta": {
    "requestId": "uuid",
    "timestamp": "2026-06-28T12:00:00Z",
    "pagination": {
      "cursor": "base64encodedcursor",
      "hasMore": true,
      "limit": 20
    }
  }
}
```

Error response:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "INSUFFICIENT_COINS",
    "message": "Not enough coins to complete this purchase",
    "details": { "required": 500, "available": 120 }
  },
  "meta": {
    "requestId": "uuid",
    "timestamp": "2026-06-28T12:00:00Z"
  }
}
```

---

## 14. Push Notifications

### 14.1 Notification Types

| Type | Trigger | Title | Body Example |
|---|---|---|---|
| `CROP_READY` | Crop matured | 🌾 Your crops are ready! | "Your wheat is ready to harvest on {farm_name}" |
| `ANIMAL_PRODUCT` | Animal product ready | 🐄 Your cow produced milk! | "Collect milk from your cow on {farm_name}" |
| `GIFT_RECEIVED` | Friend sent gift | 🎁 You received a gift! | "{friend_name} sent you {item_name}" |
| `FARM_VISITED` | Friend visited | 👋 {friend_name} visited your farm | "They watered your crops!" |
| `QUEST_COMPLETED` | Quest objective met | ✅ Quest completed! | "Daily Harvest is ready to claim" |
| `DAILY_REWARD` | Daily login available | 🎉 Daily reward waiting! | "Claim your {day} day streak reward" |
| `MARKET_SOLD` | Listing sold | 💰 Market sale! | "Your {item} sold for {price} coins" |
| `CROP_WILTING` | Crop about to wither (50% warning) | ⚠️ Crops wilting soon! | "Harvest your {crop} before it withers" |
| `SEASONAL_EVENT` | New event started | 🎊 {event_name} is here! | "Limited-time crops and quests available" |
| `LEVEL_UP` | Reached new level | 🎊 Level up! | "You reached level {n}! New crops unlocked" |

### 14.2 Notification Preferences (User Configurable)

| Category | Default | Premium Default |
|---|---|---|
| Crop ready | ON | ON |
| Animal product | ON | ON |
| Gift received | ON | ON |
| Farm visited | OFF | ON |
| Quest completed | ON | ON |
| Daily reward | ON | ON |
| Market sold | ON | ON |
| Crop wilting | ON | ON |
| Seasonal event | OFF | ON |
| Level up | OFF | OFF |

### 14.3 Quiet Hours

- Default: 10 PM – 8 AM (local time)
- User configurable
- Critical notifications (gift received, market sold) bypass quiet hours
- Non-critical notifications queued and delivered after quiet hours end

### 14.4 Notification Channels (Android)

| Channel ID | Name | Types | Importance |
|---|---|---|---|
| `crops` | Crop & Animal | CROP_READY, ANIMAL_PRODUCT, CROP_WILTING | HIGH |
| `social` | Social | GIFT_RECEIVED, FARM_VISITED | HIGH |
| `quests` | Quests & Rewards | QUEST_COMPLETED, DAILY_REWARD, LEVEL_UP | DEFAULT |
| `market` | Market | MARKET_SOLD | HIGH |
| `events` | Events | SEASONAL_EVENT | LOW |

- Users can customize per-channel settings in Android system settings (no need to build custom UI for this — Android handles it).
- Channel IDs are created on first launch and never changed (Android restriction).

### 14.5 Deep Links

| Notification Type | Deep Link Route | Behavior |
|---|---|---|
| CROP_READY | `ibibofarms://farm` | Opens farm screen, scrolls to ready crop |
| ANIMAL_PRODUCT | `ibibofarms://farm` | Opens farm screen, scrolls to ready animal |
| GIFT_RECEIVED | `ibibofarms://gifts` | Opens gift center |
| FARM_VISITED | `ibibofarms://friends` | Opens friends list |
| QUEST_COMPLETED | `ibibofarms://quests` | Opens quests screen |
| DAILY_REWARD | `ibibofarms://daily-reward` | Opens daily reward screen |
| MARKET_SOLD | `ibibofarms://market` | Opens market screen |
| SEASONAL_EVENT | `ibibofarms://event/{eventId}` | Opens event screen |
| CROP_WILTING | `ibibofarms://farm` | Opens farm screen, scrolls to wilting crop |
| LEVEL_UP | `ibibofarms://farm` | Opens farm screen |

- Deep links use custom URI scheme `ibibofarms://` registered in AndroidManifest.
- App links (verified `https://ibibofarms.app/`) also configured for web-to-app deep linking.

---

## 15. Security & Data Privacy

### 15.1 Authentication & Authorization

- JWT access tokens (15-min TTL), refresh tokens (30-day TTL, httpOnly cookie or secure storage)
- Phone OTP via Firebase Auth (6-digit, 60-second expiry, max 5 attempts)
- Google Sign-In via Google Identity Services
- Guest accounts: device-bound, upgradeable, 30-day expiry (auto-deleted if not upgraded)
- Device limit: 3 concurrent sessions per account; oldest session evicted

### 15.2 Data Protection

| Data Type | Storage | Encryption |
|---|---|---|
| Passwords | N/A (OTP/Google auth, no passwords) | N/A |
| JWT tokens | Android Keystore (EncryptedSharedPreferences) | AES-256 |
| Local game data (Room) | App private storage | SQLCipher (optional, for sensitive data) |
| Transit | HTTPS/TLS 1.2+ | TLS |
| Server DB | PostgreSQL at rest | AES-256 (AWS EBS encryption) |
| Backups | AWS S3 | SSE-KMS |

### 15.3 Privacy Compliance

- **DPDP Act (India):** Explicit consent on first launch, data processing notice, right to access/delete, data localization option
- **GDPR (if EU users):** Consent dialog, right to erasure, data portability
- **COPPA:** Age gate (13+); users under 13 require parental consent flow
- **Data retention:** User data retained for 90 days post-account deletion, then permanently purged (except legally required transaction records)
- **Data export format:** JSON file containing profile, farm state, transaction history, friend list, achievements. Delivered via email or in-app download.
- **Children's data:** Users under 13 (age gate) require parental consent. Data collection limited to minimum necessary for gameplay. No behavioral ads for under-13 users.

### 15.4 Anti-Cheat (Detailed)

| Vector | Detection | Response |
|---|---|---|
| Time manipulation (device clock change) | Server compares device timestamp vs server timestamp on every sync; delta > 5 min flagged | Revert farm to last server-confirmed state; 3 strikes = 7-day ban |
| Coin/gem duplication | Server-authoritative balance; every transaction logged in ledger with running balance | Reject client state; restore server balance; flag for review |
| Modified APK | Google Play Integrity API check on app launch + periodic | Block sync; force re-install from Play Store |
| Bot automation | Action frequency analysis (actions/min), pattern detection (identical intervals) | Shadow-ban from leaderboards; throttle API; flag for review |
| Multi-account farming | Device fingerprint + phone number + IP correlation | Limit daily rewards to 1 per device; flag for review |
| Market manipulation | Price anomaly detection (listing at extreme prices for coin transfer) | Cancel suspicious listings; flag accounts |

### 15.5 API Key & Secrets Management

| Secret | Storage | Rotation |
|---|---|---|
| JWT signing key | AWS Secrets Manager | Rotated every 90 days |
| Firebase service account | AWS Secrets Manager | Rotated on personnel change |
| Google Play Developer API key | AWS Secrets Manager | Rotated on personnel change |
| AdMob ad unit IDs | Hardcoded in app (not secret) | N/A |
| Database credentials | AWS Secrets Manager | Rotated every 90 days |
| Redis password | AWS Secrets Manager | Rotated every 90 days |
| S3/R2 access keys | IAM role (no hardcoded keys) | Auto-rotated by AWS |

- **No secrets in source code:** All API keys, passwords, and private keys stored in AWS Secrets Manager / Doppler. CI/CD injects secrets at build time.
- **Secret scanning:** GitGuardian / GitHub secret scanning enabled on repository.

### 15.6 Security Testing & Audits

| Activity | Frequency | Scope |
|---|---|---|
| Penetration testing | Pre-launch + quarterly | API, auth, anti-cheat, data exposure |
| OWASP Top 10 scan | Every CI pipeline | Automated SAST + DAST |
| Dependency vulnerability scan | Every CI pipeline | Dependabot / Snyk |
| Bug bounty | Post-launch | Private program via HackerOne / Bugcrowd (critical + high findings only) |
| Security audit | Pre-launch | Third-party security firm review of architecture + code |

---

## 16. Analytics & Telemetry

### 16.1 Key Events

| Event | Trigger | Properties |
|---|---|---|
| `app_open` | App launched | `session_id`, `is_first_open`, `connection_type` |
| `tutorial_step` | Tutorial step completed | `step_number`, `step_name`, `time_spent_sec` |
| `tutorial_complete` | Tutorial finished | `total_time_sec` |
| `tutorial_skip` | Tutorial skipped | `step_skipped_at` |
| `crop_planted` | Seed planted | `crop_id`, `tile_count`, `seed_source` |
| `crop_harvested` | Crop harvested | `crop_id`, `quantity`, `growth_time_actual` |
| `crop_withered` | Crop withered | `crop_id`, `overtime_sec` |
| `animal_product_collected` | Animal product collected | `animal_id`, `product_id` |
| `shop_purchase` | NPC shop buy | `item_id`, `quantity`, `total_cost`, `currency` |
| `shop_sell` | NPC shop sell | `item_id`, `quantity`, `total_earned` |
| `market_listing_created` | Player creates market listing | `item_id`, `qty`, `unit_price` |
| `market_purchase` | Player buys from market | `item_id`, `qty`, `total_cost` |
| `quest_claimed` | Quest reward claimed | `quest_id`, `reward_coins`, `reward_xp` |
| `friend_visit` | Visited friend's farm | `friend_id`, `actions_performed` |
| `gift_sent` | Gift sent | `friend_id`, `item_type`, `item_id` |
| `gift_received` | Gift received | `from_user_id`, `item_type`, `item_id` |
| `iap_initiated` | IAP flow started | `product_id` |
| `iap_completed` | IAP successful | `product_id`, `amount`, `currency`, `reward` |
| `iap_failed` | IAP failed | `product_id`, `error_code` |
| `ad_watched` | Rewarded ad completed | `placement`, `reward_type`, `reward_amount` |
| `ad_skipped` | Rewarded ad skipped | `placement`, `skip_after_sec` |
| `level_up` | Player leveled up | `new_level`, `xp_total` |
| `grid_expanded` | Farm grid expanded | `new_size` |
| `session_end` | App backgrounded/closed | `session_duration`, `actions_count` |
| `offline_sync` | Offline actions synced | `actions_synced`, `conflicts_count` |
| `daily_reward_claimed` | Daily reward claimed | `streak_days`, `reward_type`, `is_premium` |
| `achievement_unlocked` | Achievement earned | `achievement_id`, `tier`, `xp_reward` |
| `subscription_started` | Premium subscription started | `plan`, `is_first_time`, `promo_code` |
| `subscription_cancelled` | Premium subscription cancelled | `reason`, `days_remaining` |
| `friend_blocked` | User blocked another user | `blocked_user_id` |
| `profile_updated` | Profile changed | `fields_changed` (array) |
| `offline_sync_failed` | Offline sync failed | `error_code`, `actions_lost` |
| `config_updated` | Game config version changed | `old_version`, `new_version` |
| `app_update` | App updated to new version | `old_version`, `new_version` |
| `language_changed` | User changed language | `old_lang`, `new_lang` |
| `grid_expansion_purchased` | Grid expansion bought | `new_size`, `cost_coins` |

### 16.2 Funnels to Track

1. **Onboarding funnel:** App open → Auth → Avatar → Farm name → Tutorial complete → First crop planted
2. **IAP funnel:** Shop open → Coin shop → Product selected → Payment initiated → Payment completed
3. **Ad funnel:** Ad prompt shown → Ad started → Ad completed → Reward claimed
4. **Social funnel:** Friends tab open → Add friend → Friend request sent → Friend accepted → First farm visit
5. **Retention funnel:** D1 → D3 → D7 → D14 → D30

### 16.3 Dashboards

| Dashboard | Purpose |
|---|---|
| Overview | DAU, WAU, MAU, new installs, retention curves |
| Economy | Coin supply, faucet/sink balance, inflation rate, IAP revenue |
| Gameplay | Most planted crops, average session length, quest completion rates |
| Social | Friend graph density, gifts/day, market volume |
| Monetization | ARPDAU, IAP conversion rate, ad fill rate, subscription count |
| Technical | API latency, error rates, crash-free sessions, sync success rate |

---

## 17. Testing Strategy

### 17.1 Client Testing

| Level | Tool | Coverage Target |
|---|---|---|
| Unit tests | JUnit 5 + MockK | 80% of domain layer (use cases, game logic) |
| ViewModel tests | JUnit 5 + Turbine (Flow testing) | 70% of ViewModels |
| Compose UI tests | Compose Testing Framework | Critical flows: onboarding, plant/harvest, shop, market |
| Integration tests | AndroidX Test + Room testing | Repository layer (local DB + API mock) |
| E2E tests | Macrobenchmark / UI Automator | Smoke test: install → onboard → plant → harvest → sell |
| Screenshot tests | Paparazzi / Roborazzi | All screens in light/dark mode |

### 17.2 Backend Testing

| Level | Tool | Coverage Target |
|---|---|---|
| Unit tests | Kotest + MockK | 80% of service layer |
| API tests | Ktor Test Engine | All endpoints (happy path + error cases) |
| Integration tests | Testcontainers (PostgreSQL, Redis) | Repository + service integration |
| Load tests | Gatling / k6 | 10,000 concurrent users, < 200ms p95 latency |
| Security tests | OWASP ZAP scan | CI pipeline, no high-severity findings |

### 17.3 Game Balance Testing

- **Simulated economy:** Script that simulates 10,000 players over 30 days to check coin inflation
- **Crop timing validation:** Automated tests for all crop growth timers, withering thresholds
- **Quest progression:** Verify all daily quests are completable within a normal play session
- **Anti-cheat validation:** Penetration tests for time manipulation, coin duplication, API fuzzing

### 17.4 Test Data

- Seed script generates: 100 test users, 50 friendships, 200 farm states, 500 transactions
- Staging environment uses anonymized production data snapshot (weekly refresh)

### 17.5 Device Test Matrix

| Tier | Devices | Android Version | Priority |
|---|---|---|---|
| Low-end | Redmi 9A (2GB RAM), Moto E13 | Android 8–10 | Must pass — represents largest Indian user segment |
| Mid-range | Samsung Galaxy M14, Redmi Note 12 | Android 12–13 | Must pass |
| High-end | Pixel 7, Samsung Galaxy S24 | Android 14–15 | Must pass |
| Tablet | Samsung Galaxy Tab A8 | Android 13 | Should pass (layout verification) |

- **Test criteria:** Cold start < 3s on low-end, no ANRs, no OOM crashes, farm grid scrolls at 60fps on mid-range, 30fps on low-end.

### 17.6 Network Condition Testing

| Condition | Simulated | Expected Behavior |
|---|---|---|
| WiFi (fast) | 50 Mbps, 20ms latency | Normal operation |
| 4G (good) | 10 Mbps, 50ms latency | Normal operation |
| 3G (slow) | 1 Mbps, 300ms latency | Actions work offline, sync slower but succeeds |
| Intermittent | Random disconnect every 30s | No data loss, WorkManager retries, UI shows sync pending |
| Airplane mode | No network | Full offline play, pending actions queued |
| High latency | 2000ms latency | UI responsive (local-first), sync completes when response arrives |

---

## 18. Deployment & CI/CD

### 18.1 CI/CD Pipeline (GitHub Actions)

```
Push to feature branch
  │
  ├─→ Lint (ktlint + detekt)
  ├─→ Unit tests (client + backend, parallel)
  ├─→ Build (debug APK + backend Docker image)
  └─→ Security scan (dependency check + SAST)

PR to main
  │
  ├─→ All above +
  ├─→ Integration tests
  ├─→ Screenshot tests (diff against baseline)
  └─→ Code review approval

Merge to main
  │
  ├─→ Build release APK (signed)
  ├─→ Build backend Docker image (tagged)
  ├─→ Deploy backend to staging
  ├─→ Run E2E smoke tests on staging
  └─→ Auto-deploy to production (backend) on tag

Release tag (vX.Y.Z)
  │
  ├─→ Build production APK (signed, minified)
  ├─→ Upload to Google Play Internal Testing track
  ├─→ Deploy backend to production (blue/green)
  ├─→ Run production smoke tests
  └─→ Promote to Play Store production (manual approval)
```

### 18.2 Environment Matrix

| Environment | Backend | App | Database |
|---|---|---|---|
| Local dev | Ktor (localhost:8080) | Debug APK | PostgreSQL (Docker) |
| Staging | ECS (staging cluster) | Internal testing track | PostgreSQL (staging RDS) |
| Production | ECS (prod cluster, blue/green) | Play Store production | PostgreSQL (prod RDS, Multi-AZ) |

### 18.3 Backend Deployment

- Docker container image, published to AWS ECR
- ECS Fargate (no server management)
- Blue/green deployment via AWS CodeDeploy
- Auto-scaling: 2–10 containers based on CPU + request count
- Health check: `GET /health` → 200 OK
- Database migrations: Flyway, run as pre-deploy step
- **Rollback strategy:**
  - Blue/green: instant rollback by switching ALB target group back to blue (previous) environment
  - Database: Flyway supports `flyway undo` for non-destructive migrations. Destructive migrations require manual rollback script, pre-approved before deployment.
  - Maximum rollback window: 30 minutes (blue environment kept running for 30 min after switch)
- **Zero-downtime deploy:** Blue/green ensures no downtime. WebSocket connections on old environment are gracefully drained (10-second timeout with reconnect message).

### 18.4 App Release

- Google Play Console
- Release tracks: Internal Testing → Closed Testing → Open Testing → Production
- Phased rollout: 10% → 25% → 50% → 100% (over 7 days)
- A/B testing via Firebase Remote Config (feature flags)
- **Rollback:** Play Console supports halting phased rollout. For critical bugs, push a hotfix build to the same track. Cannot truly "rollback" an APK version — users who updated stay on new version until hotfix.
- **Staged rollout monitoring:** Check crash rate at each stage. If crash-free session rate drops below 99.5%, halt rollout immediately.
- **Firebase Remote Config kill switches:** Critical features can be disabled server-side without app update (e.g., disable market trading if economy bug detected).

---

## 19. Development Phases & Milestones

### Phase 1: Prototype (Weeks 1–3)

- [ ] Project setup (Gradle, Compose, Hilt, Room, navigation)
- [ ] Farm grid UI (6×6, tap interactions)
- [ ] Crop system (plant, grow timer, harvest) — 3 crops only
- [ ] Local-only economy (coins, buy seeds, sell crops)
- [ ] Basic HUD (coins, level, XP)
- [ ] No backend, no auth, no social

**Deliverable:** Playable offline prototype with core farming loop.

### Phase 2: MVP (Weeks 4–8)

- [ ] Backend setup (Ktor, PostgreSQL, Redis, Docker)
- [ ] Auth (phone OTP, Google, guest)
- [ ] Offline-first sync (Room + WorkManager + Retrofit)
- [ ] Full crop catalog (10 crops)
- [ ] Livestock system (chicken, cow)
- [ ] Shop (NPC buy/sell)
- [ ] Daily quests (3 of 5 types randomly assigned per day)
- [ ] Leveling system (levels 1–10)
- [ ] Push notifications (crop ready, daily reward)
- [ ] Settings screen
- [ ] Analytics integration
- [ ] Crash reporting

**Deliverable:** Fully playable single-player game with online sync.

### Phase 3: Social (Weeks 9–14)

- [ ] Friends system (add, list, online status)
- [ ] Farm visits (read-only, water crops, feed animals)
- [ ] Gifting system
- [ ] Leaderboards (weekly, farm value, friends)
- [ ] Marketplace (player-to-player trading)
- [ ] WebSocket real-time presence
- [ ] Full livestock (goat, sheep)
- [ ] Leveling to 20
- [ ] Grid expansion (up to 10×10)
- [ ] Story quests (first 5)
- [ ] Achievements system (Phase 3 — was incorrectly listed in Phase 6)

**Deliverable:** Social farming game with trading.

### Phase 4: Monetization (Weeks 15–18)

- [ ] Google Play Billing integration
- [ ] IAP shop (coins, gems, bundles)
- [ ] Rewarded video ads (5 placements)
- [ ] Banner ads (shop, market screens)
- [ ] Premium subscription
- [ ] Ad mediation (optional: AdMob + Meta Audience Network)
- [ ] Server-side IAP verification
- [ ] Anti-cheat hardening

**Deliverable:** Monetized game ready for soft launch.

### Phase 5: Polish & Launch (Weeks 19–24)

- [ ] Tutorial refinement (A/B test skip rates)
- [ ] Onboarding optimization (reduce drop-off)
- [ ] All animations and Lottie effects
- [ ] Sound design (music + SFX)
- [ ] Dark mode polish
- [ ] Localization (Hindi, Tamil, Telugu, Bengali, Marathi — + English)
- [ ] Performance optimization (app size < 30MB, cold start < 2s)
- [ ] Load testing (10K concurrent users)
- [ ] Security audit
- [ ] Play Store listing (screenshots, description, ASO)
- [ ] Soft launch (India, 10K users)
- [ ] Data-driven iteration based on soft launch metrics

**Deliverable:** Production-ready game, hard launch.

### Phase 6: Post-Launch (Ongoing)

- [ ] Seasonal events (quarterly)
- [ ] New crops and animals (monthly)
- [ ] Breeding system
- [ ] Chat system (quick messages)
- [ ] Farm sharing (screenshot to social media)
- [ ] Web version (optional)
- [ ] iOS port (optional)

---

## 20. Risk Register

| # | Risk | Probability | Impact | Mitigation |
|---|---|---|---|---|
| 1 | Economy inflation (coin oversupply) | Medium | High | Server-side monitoring, dynamic price adjustments, coin sinks |
| 2 | Anti-cheat circumvention | Medium | High | Play Integrity API, server-authoritative economy, heuristic monitoring |
| 3 | Low retention after D1 | Medium | High | Tutorial A/B testing, push notification optimization, daily rewards |
| 4 | Backend scaling issues at launch | Low | High | Load testing, auto-scaling, blue/green deploy, CDN |
| 5 | Google Play policy violation | Low | Critical | Pre-launch policy review, no real-money gambling, COPPA compliance |
| 6 | Offline sync conflicts | Medium | Medium | Last-write-wins + server-authoritative economy, conflict resolution UI |
| 7 | Asset size bloat | Medium | Medium | WebP compression, on-demand asset delivery (Play Asset Delivery) |
| 8 | Ad fill rate low in India | Medium | Low | Ad mediation, fallback to rewarded-only, premium subscription as alternative |
| 9 | IAP conversion below target | Medium | Medium | A/B test pricing, bundle optimization, first-purchase discount |
| 10 | Negative reviews at launch | Medium | High | Soft launch to gather feedback, rapid bug fix SLA, community management |
| 11 | Firebase service outage / deprecation | Low | High | Abstract FCM/Auth behind interface; fallback to self-hosted OTP via SES; cache auth locally |
| 12 | Google Play policy change (ads, IAP, data) | Low | Critical | Stay updated on Play Policy blog, maintain compliance review checklist per release |
| 13 | Data breach (PII leak) | Low | Critical | Encryption at rest + in transit, least-privilege IAM, quarterly pen test, incident response plan |
| 14 | Key personnel dependency (single dev for a module) | Medium | Medium | Code reviews, pair programming on critical modules, documentation requirement for all modules |

---

## 21. Review & Approval Model

### 21.1 Spec Review Process

```
Draft Spec → Internal Review (dev team) → Revision → Stakeholder Review → Approval → Baseline
```

### 21.2 Review Checkpoints

| Checkpoint | Reviewer | Focus | Gate |
|---|---|---|---|
| Spec Review | Tech lead + Product manager | Completeness, feasibility, scope | Spec approved before implementation |
| Architecture Review | Tech lead + Senior devs | Tech choices, scalability, security | Architecture approved before Phase 1 |
| Design Review | UX designer + Product manager | Screen flows, usability, accessibility | Designs approved before Phase 2 UI |
| Sprint Review | Full team | Demo working increment | Every 2 weeks |
| Pre-Launch Review | Tech lead + Product + QA | Readiness checklist, bugs, performance | Go/No-go for soft launch |
| Post-Soft Launch Review | Product + Data analyst | Metrics vs targets, user feedback | Go/No-go for hard launch |

### 21.3 Change Management

- Any change to this spec after baseline requires a **Change Request (CR)**
- CR must include: description, rationale, impact assessment, effort estimate
- CR approved by: Tech lead + Product manager
- CR logged in: `/docs/changelog/spec-changes.md`

### 21.4 Approval Sign-off

| Role | Name | Date | Signature |
|---|---|---|---|
| Product Manager | | | |
| Tech Lead | | | |
| UX Designer | | | |
| QA Lead | | | |

---

## Appendix A: Crop Growth Timer Implementation

```kotlin
// Crop timer uses real-world timestamps, not active session time
data class PlantedCrop(
    val cropId: String,
    val plantedAt: Long,  // epoch millis
    val growthTimeSec: Long,
    val witherMultiplier: Float = 2.0f,  // 2.0 for free, 3.0 for premium
    val wateringCount: Int = 0,           // 0–3 (max 3 waterings by different friends)
    val wateredBy: List<String> = emptyList(),  // user UUIDs of friends who watered (max 3)
    val wateredAt: List<Long> = emptyList(),    // epoch millis when each watering happened
    val wateringReductionSec: Long = 0L,  // cumulative reduction from all waterings
) {
    val maturesAt: Long
        get() = plantedAt + (growthTimeSec * 1000) - (wateringReductionSec * 1000)

    val withersAt: Long
        get() = maturesAt + (growthTimeSec * 1000 * witherMultiplier.toLong())  // 2x or 3x original growth time

    fun water(currentTime: Long = System.currentTimeMillis()): PlantedCrop {
        if (wateringCount >= 3) return this  // cap reached
        val remainingSec = ((maturesAt - currentTime) / 1000).coerceAtLeast(0)
        if (remainingSec == 0L) return this  // already mature, no effect
        val reductionSec = (remainingSec * 0.1).toLong()  // 10% of remaining time
        return copy(
            wateringCount = wateringCount + 1,
            wateredBy = wateredBy + "friend_uuid",  // caller sets actual UUID
            wateredAt = wateredAt + currentTime,
            wateringReductionSec = wateringReductionSec + reductionSec
        )
    }

    fun getStage(currentTime: Long = System.currentTimeMillis()): CropStage {
        return when {
            currentTime >= withersAt -> CropStage.WITHERED
            currentTime >= maturesAt -> CropStage.MATURE
            progress(currentTime) < 0.25f -> CropStage.SEED
            progress(currentTime) < 0.50f -> CropStage.SPROUT
            else -> CropStage.GROWING
        }
    }

    private fun progress(currentTime: Long): Float =
        (currentTime - plantedAt).toFloat() / (maturesAt - plantedAt)
}

enum class CropStage { SEED, SPROUT, GROWING, MATURE, WITHERED }
```

## Appendix B: Farm Grid Data Model (JSONB)

```json
{
  "version": 3,
  "size": 6,
  "tiles": [
    {
      "id": "0,0",
      "type": "PLANTED",
      "crop": {
        "cropId": "wheat",
        "plantedAt": 1719561600000,
        "growthTimeSec": 30,
        "witherMultiplier": 2.0,
        "wateringCount": 1,
        "wateredBy": ["user_uuid_here"],
        "wateredAt": [1719561700000],
        "wateringReductionSec": 3
      }
    },
    {
      "id": "0,1",
      "type": "TILLED"
    },
    {
      "id": "1,0",
      "type": "ANIMAL",
      "animal": {
        "animalId": "cow",
        "fedAt": 1719561000000,
        "lastCollectedAt": 1719560700000,
        "isFed": true,
        "isSick": false
      }
    },
    {
      "id": "5,5",
      "type": "BUILDING",
      "building": {
        "buildingId": "barn",
        "storageBonus": 50
      }
    }
  ],
  "overlays": [
    { "tileId": "0,1", "decorationId": "lamp_post" }
  ]
}
```

## Appendix C: Localization Keys (Sample)

| Key | English | Hindi | Tamil | Telugu | Bengali | Marathi |
|---|---|---|---|---|---|---|
| `welcome_title` | Welcome to your farm! | अपने खेत में आपका स्वागत है! | உங்கள் பண்ணைக்கு வரவேற்கிறோம்! | మీ పొలానికి స్వాగతం! | আপনার খামারে আপনাকে স্বাগতম! | तुमच्या शेतात तुमचे स्वागत आहे! |
| `action_plant` | Plant | बोना | நடவு செய் | నాటు | বোনা | लावणे |
| `action_harvest` | Harvest | कटाई | அறுவடை | పంట కోయు | কাটা | कापणे |
| `action_till` | Till Soil | जुताई | மண்ணை தயார் செய் | మట్టి సిద్ధం చెయ్ | জমি কর্ষণ | नांगरणे |
| `action_sell` | Sell | बेचना | விற் | అమ్ము | বিক্রি | विक्री |
| `action_buy` | Buy | खरीदना | வாங்க | కొను | কেনা | खरेदी |
| `coins` | Coins | सिक्के | நாணயங்கள் | నాణెం | মুদ্রা | नाणी |
| `level` | Level | स्तर | நிலை | స్థాయి | স্তর | स्तर |
| `quest_daily` | Daily Quest | दैनिक कार्य | தினசரி பணி | రోజువారీ పని | দৈনিক কাজ | दैनिक कार्य |
| `crop_ready` | Your crops are ready! | आपकी फसल तैयार है! | உங்கள் பயிர்கள் தயாராக உள்ளன! | మీ పంటలు సిద్ధంగా ఉన్నాయి! | আপনার ফসল প্রস্তুত! | तुमची पिके तयार आहेत! |

---

> **End of Specification**  
> This document is a living spec. All changes must go through the Change Management process defined in Section 21.3.

---

## Appendix D: Glossary

| Term | Definition |
|---|---|
| ARPDAU | Average Revenue Per Daily Active User |
| DAU / WAU / MAU | Daily / Weekly / Monthly Active Users |
| D1 / D7 / D30 | Retention rate on day 1, 7, 30 after install |
| IAP | In-App Purchase |
| FCM | Firebase Cloud Messaging (push notifications) |
| RTDN | Real-Time Developer Notification (Google Play billing events) |
| RPO | Recovery Point Objective (max data loss tolerance) |
| RTO | Recovery Time Objective (max downtime tolerance) |
| NPC | Non-Player Character (system shop, not a real player) |
| Coin faucet | Any mechanism that adds coins to the economy (harvest, rewards) |
| Coin sink | Any mechanism that removes coins from the economy (seeds, decorations) |
| Wither multiplier | Multiplier of growth time after which unharvested mature crops die (2× free, 3× premium) |
| Friend code | 6-character alphanumeric unique code for adding friends without phone number |
| Shadow-ban | User can play but is hidden from leaderboards and social features without being notified |
| Play Integrity API | Google's API for verifying app authenticity and device attestation |
| DPDP Act | Digital Personal Data Protection Act (India, 2023) |
| ASO | App Store Optimization |
| Config version | Server-side version number for game definitions; client checks to decide re-fetch |

---

## Appendix E: Content Moderation & Community

### E.1 User-Generated Content (UGC)

| Content | Moderation |
|---|---|
| Display name | Profanity filter on creation + edit. Max 50 chars. No special symbols except spaces and hyphens. |
| Farm name | Same rules as display name. |
| Gift message | Max 200 chars. Profanity filter. No links allowed. |
| Quick chat messages | Pre-defined templates only (no free-text). 20 templates available. |
| Farm visit comments | Pre-defined templates only (Phase 6 feature). |
| Avatar | Preset avatars only (no custom upload in MVP). Custom avatar upload in Phase 5 with image moderation (AWS Rekognition). |

### E.2 Reporting & Enforcement

| Action | Trigger | Response Time |
|---|---|---|
| User report submitted | Player taps "Report" on another user's profile | Auto-acknowledged immediately, reviewed within 48 hours |
| Farm name violation | Profanity filter flag or user report | Auto-rename to "Player's Farm" + warning notification |
| Display name violation | Same as above | Auto-rename to "Player" + warning |
| Repeat violation (3x) | User accumulates 3 violations | 7-day ban from social features (chat, gifts, visits) |
| Severe violation | Hate speech, harassment, explicit content | Permanent ban, reported to Google Play if required |

### E.3 Customer Support

| Channel | Details |
|---|---|
| In-app help center | FAQ (30+ articles), searchable, categorized |
| In-app contact form | Subject + description + optional screenshot attachment |
| Email | support@ibibofarms.app |
| Response SLA | First response within 48 hours (business days) |
| Refund requests | Routed to Google Play's refund flow for IAP; subscription refunds handled per Section 10.6 |
| Bug reports | Auto-include: device model, Android version, app version, logs (if user consents) |
| FAQ content | Updated monthly based on top support tickets |

---

## 22. Implementation Questions — Resolved

> The following questions were identified during an implementation-readiness review. Each answer has been reviewed from the perspective of a **senior developer** (technical feasibility), **product manager** (player experience & economy balance), **QA engineer** (edge cases & testability), and **end user** (is this fun and fair?). Answers are normative — they amend and clarify the spec. Where an answer modifies existing spec text, the conflict is noted.
>
> **Critical questions** (would block implementation immediately): Q1, Q10, Q20, Q26, Q33.

### 22.1 Offline Sync

**Q1: How does the server validate individual offline actions?**

**Answer:** The server uses **action replay**, not full-state replacement. The sync flow:

1. Client sends the `PendingActionEntity` queue to `POST /api/v1/farm/sync-batch` (new endpoint) as an ordered array of actions, each with a client-generated `actionId` (UUID), `actionType` (`PLANT`, `HARVEST`, `TILL`, `WATER`, `FEED`, `BUY`, `SELL`, `PLACE_DECORATION`, `REMOVE_DECORATION`, `PLACE_BUILDING`, `EXPAND_GRID`), `tileId`, relevant `cropId`/`itemId`/`animalId`, `clientTimestamp` (epoch millis), and `clientBalanceSnapshot` (coins/gems at time of action).
2. Server processes actions **sequentially in order**. For each action:
   - Validates timestamp: `|clientTimestamp - serverTimeAtReceipt| < 5 min` (grace for network latency). Actions outside this window are flagged but not auto-rejected — they enter a reconciliation path (see Q3).
   - Validates economy: checks that the client's coin/gem balance snapshot is consistent with the server's authoritative ledger. If the action would result in a negative balance, the action is **rejected**.
   - Validates game logic: e.g., can't plant on a tile that already has a crop, can't harvest an immature crop, can't sell an item not in inventory.
   - If valid: applies the action to the server-side `farms.grid_data`, updates the `transactions` ledger, increments `version`, and records `actionId` in a new `synced_actions` log table (for idempotency — prevents replay).
   - If invalid: **skips the action**, records it in the response as `{ actionId, status: "rejected", reason: "..." }`, and **continues processing remaining actions**. No rollback of previously accepted actions in the same batch.
3. Server returns the full updated state (farm grid, coins, gems, XP, inventory) plus a per-action result array. Client replaces its local state with the server-confirmed state.

**New endpoint:** `POST /api/v1/farm/sync-batch` (max 500 actions per request, 60s timeout).

**Rationale (developer):** Sequential replay with per-action accept/reject is the only model that preserves server-authoritative economy while allowing partial offline progress. Full-state replacement would make anti-cheat impossible. Rollback-on-failure would punish players for a single invalid action in a long offline session.

**Rationale (QA):** Test cases: (a) all actions valid → all accepted, (b) action #3 invalid → #1–#2 accepted, #3 rejected, #4–#10 accepted, (c) duplicate actionId (retry) → idempotent, returns original result, (d) > 500 actions → 400 error, client must chunk.

**Rationale (end user):** If one action is rejected, you keep all your other progress. You see a non-intrusive toast: "1 action couldn't be synced — your farm has been updated."

---

**Q2: What's the conflict resolution for concurrent device sessions?**

**Answer:** **Per-tile last-write-wins with timestamp comparison.** Each tile in `grid_data` carries a `lastModifiedAt` timestamp. When a sync-batch action modifies a tile, the server compares the action's `clientTimestamp` against the tile's current `lastModifiedAt`:

- If `clientTimestamp > lastModifiedAt`: the new action wins. The tile is updated, `lastModifiedAt` is set to `clientTimestamp`.
- If `clientTimestamp <= lastModifiedAt`: the action is **rejected** with reason `STALE_TILE`. The client receives the current server state for that tile.

**Scenario:** Device A plants wheat on tile (0,0) at 10:00. Device B plants rice on tile (0,0) at 10:01. Both sync at 10:05.
- Device A syncs first → server tile (0,0) = wheat, `lastModifiedAt` = 10:00.
- Device B syncs → action timestamp 10:01 > 10:00 → rice replaces wheat. Device A's wheat seed is **refunded** (server detects seed was consumed by a now-overwritten action and credits it back via the `transactions` ledger as a `refund` type).
- If Device B syncs first → rice at 10:01. Device A syncs → 10:00 < 10:01 → rejected as `STALE_TILE`. Device A's wheat seed is refunded (it was never consumed server-side since the action was rejected).

**Seed refund mechanism:** When a `PLANT` action is rejected (`STALE_TILE` or `TILE_OCCUPIED`), the sync response includes a `refunds` array: `[{ actionId, itemType: "seed", itemId: "wheat_seed", quantity: 1 }]`. The client applies refunds to local inventory.

**Rationale (product):** Refunding seeds on conflict is critical for player trust. A player who loses seeds because their other device synced first would feel cheated. The refund costs the economy nothing (the seed was never planted server-side).

**Rationale (QA):** Test: two devices, same tile, sync in both orders. Verify refund. Test: three devices, rapid succession. Test: same-timestamp actions (resolve by `actionId` lexicographic order as tiebreaker).

---

**Q3: How long can a user stay offline before server rejects the sync?**

**Answer:**

| Offline Duration | Behavior |
|---|---|
| 0–24 hours | Normal sync-batch. All actions processed via standard validation. |
| 24–72 hours | **Reconciliation mode.** Server processes actions but applies additional checks: (a) crop growth times re-validated against real elapsed time — if a player claims to have harvested a crop that couldn't have matured in the offline period, the action is rejected. (b) Total coins earned offline cannot exceed the theoretical maximum (all tiles planted + harvested at optimal cycles for the duration). If exceeded, excess is clawed back. (c) A `reconciliation_report` is included in the sync response showing any adjustments. |
| 72 hours – 7 days | **Capped reconciliation.** Same as above, but the server only honors the **last 500 actions** (FIFO from the pending queue). Older actions are discarded. The player receives: "Some actions older than 3 days could not be synced. Your farm has been updated to the latest confirmed state." |
| > 7 days | **Hard cutoff.** Sync is rejected with `409 CONFLICT` and `error.code = "OFFLINE_TOO_LONG"`. The client must discard its local pending queue and fetch the server state fresh. Player sees: "You've been offline for over 7 days. Your farm has been restored to its last saved state." |

**Max batch size:** 500 actions per `sync-batch` request. If the pending queue exceeds 500, the client sends multiple sequential requests (chunked by timestamp order). WorkManager handles chunking automatically.

**Rationale (developer):** 7 days is the hard cutoff because beyond that, the theoretical-max-earnings validation becomes unreliable. 500 actions caps request size at ~1 MB.

**Rationale (end user):** 72 hours covers a long weekend trip. 7 days covers a vacation without internet. Beyond that, it's reasonable to say "we couldn't save your progress."

**Rationale (QA):** Test boundary conditions: 23h59m (normal), 24h01m (reconciliation), 71h59m (capped), 7d01m (hard cutoff). Test 501 actions → 2 chunks.

### 22.2 Economy & Transactions

**Q4: What's the exact gem cost formula for speed-ups and revives?**

**Answer:**

**Speed-up (instantly mature a growing crop):**
```
gemCost = speedUpTier(remainingTimeSec)
```
Tiered pricing (non-linear — longer waits cost progressively more per minute saved):

| Remaining Time | Gem Cost |
|---|---|
| < 10 min | 1 gem |
| 10–20 min | 2 gems |
| 20–30 min | 3 gems |
| 30–45 min | 5 gems |
| 45–60 min | 6 gems |
| 1–2 hours | 10 gems |
| 2+ hours | 15 gems (cap) |

**Revive (restore a withered crop):**
```
gemCost = ceil(originalGrowthTimeSec / 900) × 1 gem per 15-minute block
```
| Crop Growth Time | Revive Cost |
|---|---|
| 30 sec (wheat) | 1 gem |
| 5 min (tomato) | 1 gem |
| 10 min (carrot) | 1 gem |
| 30 min (mango) | 2 gems |
| 45 min (cotton) | 3 gems |

**Rationale (product):** Speed-up cost scales with remaining time so it's never "cheaper" to speed up early. Revive cost scales with original growth time (longer crops are more valuable to revive). Both are rounded up to ensure the gem sink is meaningful.

**Rationale (end user):** A player with 1 gem can always revive a short crop. Speed-ups for long crops cost more, which feels fair — you're saving more waiting time.

**Rationale (QA):** Test boundary: remaining time = exactly 10 min → 2 gems (not 1). Test revive on premium crop. Test speed-up on a crop with 1 second remaining → 1 gem (minimum).

---

**Q5: How does "grid expansion discount" with gems work?**

**Answer:** Players can spend gems to reduce the coin cost of grid expansion by **50%**. The gem cost is fixed per expansion tier:

| Grid Expansion | Coin Cost (normal) | Gem Cost (for 50% discount) | Coin Cost (with gem discount) |
|---|---|---|---|
| 6×6 → 7×7 | 2,000 | 5 gems | 1,000 |
| 7×7 → 8×8 | 5,000 | 10 gems | 2,500 |
| 8×8 → 9×9 | 12,000 | 15 gems | 6,000 |
| 9×9 → 10×10 | 25,000 | 20 gems | 12,500 |
| 10×10 → 12×12 | 50,000 | 30 gems | 25,000 |

The player sees a toggle in the grid expansion dialog: "Use 10 gems to save 2,500 coins." If they have enough gems, they can choose either option.

**Rationale (product):** 50% flat discount is simple to communicate. The gem cost scales with the coin savings, making it a meaningful sink at all levels. A high-level player with excess gems but coin-poor can use this; a low-level player with few gems will pay full coin price.

---

**Q6: What's the coin-to-gem conversion rate (if any)?**

**Answer:** **No conversion in either direction.** Coins and gems are separate currencies with separate faucets and sinks. This is deliberate:

- Coins are the "soft" currency — earned through gameplay, inflated over time, used for standard operations.
- Gems are the "hard" currency — earned sparingly (daily rewards, achievements, IAP), used for convenience and premium content.
- Allowing coin→gem conversion would let free players bypass the gem sink entirely, devaluing IAP. Allowing gem→coin conversion would let paying players skip the gameplay loop, reducing engagement.

**Rationale (product):** This follows the standard freemium mobile game economy model (e.g., Clash of Clans: gold/elixir vs. gems). Mixing the two currencies undermines both.

**Rationale (end user):** "I can't buy my way to gems, but I earn them by playing every day. That feels fair."

---

**Q7: How are market prices validated server-side?**

**Answer:**

| Rule | Value |
|---|---|
| **Min listing price** | 1 coin per unit |
| **Max listing price** | 10× the NPC sell price for that item (e.g., wheat sell price = 12 → max listing = 120 coins/unit) |
| **Suggested price range** | 50%–200% of NPC sell price (shown to player as a "fair price" guide in the UI) |
| **Price anomaly detection** | Listings outside 50%–200% of NPC sell price are flagged for review. Listings above 5× NPC sell price are **auto-cancelled** at creation with error `PRICE_TOO_HIGH`. Listings below 25% of NPC sell price are allowed but flagged (could be a legitimate quick-sell). |
| **Bulk listing threshold** | If a single user lists > 50 units of the same item in 24 hours, all their listings are flagged for review (potential coin-transfer via mule account). |

**Rationale (developer):** The NPC sell price is the natural anchor — it's the price the game guarantees for any item. Capping at 10× prevents extreme coin transfers between accounts (the primary market abuse vector). The 50%–200% "fair price" guide helps players price reasonably without forcing them.

**Rationale (QA):** Test: list at 1 coin → allowed. List at 11× NPC price → rejected. List at exactly 10× → allowed (boundary). List 51 units → flagged. Test: two accounts listing same item at extreme prices to transfer coins → both flagged.

---

**Q8: What happens when a buyer purchases a listing but the seller has simultaneously cancelled it?**

**Answer:** The buy operation uses a **PostgreSQL row-level lock** (`SELECT ... FOR UPDATE`) on the `market_listings` row. The flow:

1. Buyer calls `POST /api/v1/market/listings/{id}/buy`.
2. Server begins transaction, locks the listing row.
3. Checks `status == 'active'`. If `status == 'cancelled'` (seller cancelled first), returns `409 CONFLICT` with `error.code = "LISTING_CANCELLED"`. Buyer's coins are not deducted. Buyer sees: "This listing was cancelled by the seller."
4. If `status == 'active'`, sets `status = 'sold'`, deducts buyer coins, credits seller coins, transfers item to buyer inventory, commits transaction.

**Seller cancellation flow:**
1. Seller calls `DELETE /api/v1/market/listings/{id}`.
2. Server begins transaction, locks the listing row.
3. Checks `status == 'active'`. If `status == 'sold'` (buyer purchased first), returns `409 CONFLICT` with `error.code = "ALREADY_SOLD"`. Seller sees: "This item was already sold."
4. If `status == 'active'`, sets `status = 'cancelled'`, returns item to seller inventory, commits.

**Guarantee:** Exactly one of the two operations succeeds. The row lock ensures atomicity. No double-spend, no duplicate items.

**Rationale (developer):** `SELECT FOR UPDATE` is the standard PostgreSQL pattern for this race condition. The transaction is short-lived (single row lock + 2 table updates), so contention is minimal.

**Rationale (QA):** Test: simulate concurrent buy + cancel on same listing. Verify exactly one succeeds. Test: 10 concurrent buyers on same listing → only 1 succeeds, 9 get `LISTING_SOLD`.

### 22.3 Game Mechanics Gaps

**Q9: What are the 4 crop growth stages?**

**Answer:**

| Stage | Name | Threshold | Sprite Description |
|---|---|---|---|
| 1 | Seed | 0% – <25% | Small mound of soil with a tiny sprout barely visible |
| 2 | Sprout | 25% – <50% | Green sprout with 2 small leaves |
| 3 | Growing | 50% – <100% | Larger plant, leaves spread, buds forming (crop-specific shape) |
| 4 | Mature | 100% | Full-grown crop, ready to harvest (golden wheat, red tomato, etc.) |

**Implementation:** The `getStage()` function in Appendix A should be updated to return granular stages for sprite selection:

```kotlin
enum class CropStage { SEED, SPROUT, GROWING, MATURE, WITHERED }

fun getSpriteStage(currentTime: Long = System.currentTimeMillis()): CropStage {
    val progress = (currentTime - plantedAt).toFloat() / (maturesAt - plantedAt)
    return when {
        currentTime >= withersAt -> CropStage.WITHERED
        currentTime >= maturesAt -> CropStage.MATURE
        progress < 0.25f -> CropStage.SEED
        progress < 0.50f -> CropStage.SPROUT
        else -> CropStage.GROWING
    }
}
```

**Rationale (end user):** Four visually distinct stages give players a clear sense of progress. The 25/50/100 thresholds mean the first stage change happens quickly (encouraging), and the final stage is the payoff.

**Note:** This updates the `CropStage` enum in Appendix A. The existing `GROWING` stage is split into `SEED`, `SPROUT`, and `GROWING`. `MATURE` and `WITHERED` remain unchanged.

---

**Q10: How does "watering" actually work in the data model?**

**Answer:** The spec text in Section 9.2 is correct: watering **reduces remaining growth time by 10%**. The Appendix A code is **wrong** and must be updated. The discrepancy:

- **Appendix A (incorrect):** `effectiveGrowthTimeSec = growthTimeSec * 0.9` — reduces *total* growth time by 10%, regardless of when watering happens.
- **Section 9.2 (correct):** "reduces remaining growth time by 10%" — reduces *remaining* time at the moment of watering.

**Corrected implementation (replaces Appendix A logic):**

```kotlin
data class PlantedCrop(
    val cropId: String,
    val plantedAt: Long,
    val growthTimeSec: Long,
    val witherMultiplier: Float = 2.0f,
    val watered: Boolean = false,
    val wateredBy: String? = null,
    val wateredAt: Long? = null,
    val wateringReductionSec: Long = 0L,
) {
    val maturesAt: Long
        get() = plantedAt + (growthTimeSec * 1000) - (wateringReductionSec * 1000)

    val withersAt: Long
        get() = maturesAt + (growthTimeSec * 1000 * witherMultiplier.toLong())

    fun water(currentTime: Long = System.currentTimeMillis()): PlantedCrop {
        if (watered) return this
        val remainingSec = ((maturesAt - currentTime) / 1000).coerceAtLeast(0)
        val reductionSec = (remainingSec * 0.1).toLong()
        return copy(
            watered = true,
            wateredAt = currentTime,
            wateringReductionSec = reductionSec
        )
    }
}
```

**Watering is applied at calculation time.** The `wateringReductionSec` is computed once when watering happens and stored. Watering early (e.g., at 10% progress) removes 10% of remaining 90% = 9% of total growth time. Watering late (e.g., at 90% progress) removes 10% of remaining 10% = 1% of total. This rewards early watering.

**Rationale (product):** "Remaining time" reduction is more intuitive: "My friend watered my crops and they'll be ready sooner." It also makes the social visit loop more meaningful — visiting a friend who just planted helps more.

**Rationale (QA):** Test: plant wheat (30s), water at t=0 → matures at 27s (10% of 30s = 3s reduction). Water at t=27s (3s remaining) → matures at 26.7s (10% of 3s = 0.3s). Verify `maturesAt` and `withersAt` calculations.

**Note:** This corrects Appendix A. The `effectiveGrowthTimeSec` property is removed and replaced with `wateringReductionSec`.

---

**Q11: Can crops be watered multiple times by different friends?**

**Answer:** **Yes, but with diminishing returns and a per-crop cap of 3 waterings.**

- Each crop can be watered a **maximum of 3 times** (by 3 different friends).
- Each watering applies 10% reduction to the *remaining* time at the moment of that watering.
- Waterings are **multiplicative**: the second watering applies 10% to the already-reduced remaining time.

**Example:** Wheat (30s), planted at t=0, all waterings at t=0 for simplicity:
- Friend 1: remaining = 30s → reduction = 3s → new remaining = 27s.
- Friend 2: remaining = 27s → reduction = 2.7s → new remaining = 24.3s.
- Friend 3: remaining = 24.3s → reduction = 2.43s → new remaining = 21.87s.
- Friend 4 → rejected, cap reached. Max reduction ~27%.

**Data model update:** `PlantedCrop` gains `wateringCount: Int` (0–3). `watered: Boolean` becomes `wateringCount > 0`. `wateredBy` becomes `wateredBy: List<String>` (max 3 UUIDs). `wateredAt` becomes `wateredAt: List<Long>`.

**Per-friend-per-day limit:** Each friend can water a given farm once per day (existing spec, Section 9.2). Tracked in `farm_visits.actions_performed`.

**Rationale (product):** 3 waterings × 10% multiplicative = max ~27% reduction. Meaningful but not game-breaking. Encourages having 3+ active friends without making watering mandatory.

**Rationale (QA):** Test: 3 friends water same crop → verify cumulative reduction. Test: 4th friend → rejected. Test: same friend waters twice → rejected. Test: watering after maturity → no effect.

---

**Q12: What does "second animal pen (place 2 animals on same tile)" at Level 12 mean?**

**Answer:** At Level 12, players unlock **co-housing** — placing 2 animals of the same type on a single tile.

| Rule | Detail |
|---|---|
| Which animals | Same species only (2 chickens, 2 cows, 2 goats, 2 sheep). Cannot mix types. |
| Production | **Doubled.** Each animal produces independently. 2 chickens = 2 eggs every 30 min. |
| Feed cost | **Doubled.** Each animal needs to be fed separately. 2 chickens = 2 crops per feeding cycle. |
| Tile occupancy | Still 1 tile. |
| Sick state | If unfed > 24h, both animals become sick. Medicine cost is per-animal. |
| Visual | Tile sprite shows 2 animals side-by-side (scaled down slightly). |
| Restriction | Only animals with `tile_size = 1` (chicken, goat). Larger animals (`tile_size = 2`) cannot be co-housed. |

**Note:** The Level 12 unlock text in the leveling table (Section 8.4) has been updated in-place to read: "Co-house 2 small animals of same type on 1 tile (chicken, goat only)."

**Rationale (product):** Mid-game capacity upgrade. Rewards livestock-focused players without requiring grid expansion. Doubled feed cost prevents it from being a pure free upgrade.

**Rationale (QA):** Test: place 2 chickens on 1 tile → verify 2 egg production. Test: place chicken + cow → rejected. Test: feed 1, leave 1 unfed → only 1 stops producing. Test: co-house with `tile_size = 2` animal → rejected.

---

**Q13: What are the story quest definitions?**

**Answer:**

| Chapter | Unlock Level | Quest ID | Objectives (sequential) | Rewards |
|---|---|---|---|---|
| 1 | 1 | `story_ch1_new_beginnings` | 1. Till 5 tiles → 2. Plant 10 wheat → 3. Harvest 10 wheat → 4. Sell 10 wheat at NPC shop | 200 coins + 50 XP + 5 carrot seeds |
| 2 | 14 | `story_ch2_livestock_baron` | 1. Build a barn → 2. Place 5 animals → 3. Collect 20 animal products → 4. Earn 1,000 coins from animal products | 1,000 coins + 200 XP + 1 goat |
| 3 | 19 | `story_ch3_market_mogul` | 1. Create 10 market listings → 2. Sell 5 items on market → 3. Buy 5 items from market → 4. Earn 2,000 coins from market sales | 2,000 coins + 300 XP + 10 gems |
| 4 | 21 | `story_ch4_social_farmer` | 1. Add 10 friends → 2. Visit 20 friend farms → 3. Water 50 crops (friends') → 4. Send 20 gifts | 1,500 coins + 250 XP + exclusive "Social Butterfly" decoration |
| 5 | 24 | `story_ch5_master_grower` | 1. Plant every crop type → 2. Harvest 100 crops → 3. Reach farm value 10,000 → 4. Expand grid to 10×10 | 3,000 coins + 500 XP + 15 gems + exclusive "Master Grower" statue |
| 6 | 28 | `story_ch6_farm_legend` | 1. Reach level 30 → 2. Earn 50,000 total coins → 3. Complete all daily quests 7 consecutive days → 4. Achieve top 100 on any leaderboard | 5,000 coins + 1,000 XP + 30 gems + exclusive "Legend" farm theme + title "Farm Legend" |

**Implementation:** Each chapter is a `quest_definitions` row with `type = 'story'`. Multi-step objectives stored as JSONB in a new `objectives` column:

```json
{
  "steps": [
    { "id": "till_5", "objective_type": "till_count", "target": 5 },
    { "id": "plant_10_wheat", "objective_type": "plant_count", "target": 10, "crop_id": "wheat" }
  ]
}
```

`user_quests.progress` tracks the current step index (0-based). Steps are sequential — step N+1 doesn't start tracking until step N is complete.

**Schema change:** Add `objectives JSONB` column to `quest_definitions` (nullable — daily quests use existing single-objective fields; story quests use the `objectives` array).

**Rationale (product):** Story quests provide long-term progression goals. Chapter 6 is aspirational — most players won't complete it for months.

---

**Q14: What are the achievement definitions?**

**Answer:** 30 achievements across 4 categories:

| ID | Display Name | Category | Objective | Tier | XP | Gems |
|---|---|---|---|---|---|---|
| `first_harvest` | First Harvest | Farming | Harvest 1 crop | Bronze | 10 | 0 |
| `harvest_100` | Centurion Farmer | Farming | Harvest 100 crops | Bronze | 25 | 0 |
| `harvest_1000` | Master Harvester | Farming | Harvest 1,000 crops | Silver | 50 | 2 |
| `harvest_10000` | Legend of the Harvest | Farming | Harvest 10,000 crops | Gold | 100 | 5 |
| `first_plant` | First Seed | Farming | Plant 1 seed | Bronze | 10 | 0 |
| `plant_500` | Dedicated Planter | Farming | Plant 500 seeds | Silver | 50 | 2 |
| `all_crops_planted` | Crop Encyclopedia | Farming | Plant every crop type at least once | Gold | 100 | 5 |
| `first_animal` | Animal Lover | Farming | Buy first animal | Bronze | 10 | 0 |
| `animal_collector_5` | Livestock Baron | Farming | Own 5 animals simultaneously | Silver | 50 | 2 |
| `animal_collector_20` | Noah's Farm | Farming | Own 20 animals simultaneously | Gold | 100 | 5 |
| `product_100` | Dairy Tycoon | Farming | Collect 100 animal products | Silver | 50 | 2 |
| `first_sell` | First Sale | Economic | Sell 1 item | Bronze | 10 | 0 |
| `earn_10000` | First Fortune | Economic | Earn 10,000 total coins | Silver | 50 | 2 |
| `earn_100000` | Wealthy Farmer | Economic | Earn 100,000 total coins | Gold | 100 | 5 |
| `earn_1000000` | Coin Magnate | Economic | Earn 1,000,000 total coins | Platinum | 200 | 10 |
| `market_first_sale` | Market Trader | Economic | Complete first market sale | Bronze | 10 | 0 |
| `market_100_sales` | Market Mogul | Economic | Complete 100 market sales | Silver | 50 | 2 |
| `market_500_sales` | Tycoon | Economic | Complete 500 market sales | Gold | 100 | 5 |
| `first_friend` | Friendly Farmer | Social | Add first friend | Bronze | 10 | 0 |
| `friends_10` | Social Butterfly | Social | Add 10 friends | Silver | 50 | 2 |
| `friends_50` | Popular Farmer | Social | Add 50 friends | Gold | 100 | 5 |
| `first_visit` | Good Neighbor | Social | Visit 1 friend's farm | Bronze | 10 | 0 |
| `visit_100` | Frequent Visitor | Social | Visit 100 friend farms | Silver | 50 | 2 |
| `water_100` | Helpful Waterer | Social | Water friends' crops 100 times | Silver | 50 | 2 |
| `gift_50` | Generous Soul | Social | Send 50 gifts | Silver | 50 | 2 |
| `level_10` | Rising Star | Special | Reach level 10 | Bronze | 25 | 0 |
| `level_20` | Experienced Farmer | Special | Reach level 20 | Silver | 50 | 2 |
| `level_30` | Farm Veteran | Special | Reach level 30 | Gold | 100 | 5 |
| `streak_7` | Weekly Devotion | Special | 7-day login streak | Bronze | 25 | 1 |
| `streak_30` | Monthly Dedication | Special | 30-day login streak | Gold | 100 | 5 |

**Hidden achievements** (`is_hidden = true`): `all_crops_planted`, `animal_collector_20`, `earn_1000000`, `market_500_sales`, `friends_50`, `streak_30`. Not visible until unlocked — provides surprise delight.

**Implementation:** Seeded into `achievement_definitions` via migration. Progress tracking is event-driven: when a relevant analytics event fires (e.g., `crop_harvested`), the server checks achievements with matching `objective_type` and increments progress. When `progress >= target`, achievement is unlocked and reward granted.

**Schema change:** Add `objective_type VARCHAR(30)` and `objective_target INT` columns to `achievement_definitions` for server-side progress tracking.

**Rationale (product):** 30 achievements is enough for variety without overwhelming. Rewards are small enough to not disrupt economy balance but meaningful enough to feel rewarding.

---

**Q15: How is `farm_value` calculated?**

**Answer:**

```
farm_value = sum(crop_current_value) + sum(animal_value) + sum(decoration_value) + sum(building_value) + grid_expansion_value
```

| Component | Formula |
|---|---|
| Crop current value | For each growing/mature crop: `crop_definitions.sell_price × 0.5` (50% of sell price). Withered crops: 0. |
| Animal value | For each animal: `animal_definitions.buy_cost` (full). Sick animals: `buy_cost × 0.5`. |
| Decoration value | For each placed decoration: `decoration_definitions.buy_cost` (full). |
| Building value | For each placed building: `building_definitions.buy_cost` (full). |
| Grid expansion value | `(current_grid_size² - 6²) × 100` — each tile beyond starting 6×6 is worth 100 coins. E.g., 8×8 = (64-36) × 100 = 2,800. |

**No depreciation.** Farm value measures total investment, not current market value. This keeps calculation simple and makes farm value always increase as players invest.

**Cron job:** Runs every 6 hours (existing spec, Section 5.1.7). Parses `grid_data` JSONB, looks up current prices, computes sum, updates `farms.farm_value`.

**Performance:** At 50,000 users, 6-hour cron = ~8,333 farms/hour = ~2.3 farms/sec. Each parse + lookup ~5ms. Total: ~12 seconds of processing per hour. No concern.

**Rationale (product):** Growing crops at 50% value prevents exploiting farm_value by planting expensive crops right before the cron runs. Animals and buildings at full value rewards permanent investments.

**Rationale (QA):** Test: farm with 10 wheat (sell=12) growing, 1 cow (buy=500), 1 fence (buy=20), 1 barn (buy=1000), 8×8 grid. Expected: (10×6) + 500 + 20 + 1000 + 2800 = 4,380. Test withered crops contribute 0. Test sick animal at 50%.

### 22.4 Social — Missing Details

**Q16: How is the friend's farm snapshot generated and cached?**

**Answer:** The snapshot is a **point-in-time copy of `farms.grid_data`** served from the server, cached in Redis for **5 minutes** per visitor-host pair.

**Flow:**
1. Visitor taps friend → client calls `POST /api/v1/friends/visit/{userId}`.
2. Server fetches `farms.grid_data` for the host user.
3. Server creates a read-only snapshot: strips sensitive data, adds computed fields (current crop stages, animal states at current server time).
4. Server caches the snapshot in Redis under key `farm_snapshot:{visitorId}:{hostId}` with TTL 5 minutes.
5. Returns the snapshot to the visitor.
6. Subsequent visits within 5 minutes return the cached snapshot (no DB hit).
7. After 5 minutes, the cache expires and a fresh snapshot is generated on next visit.

**Watering affects the live farm, not the snapshot.** When a visitor waters crops, the server modifies the host's `farms.grid_data` directly. The visitor's snapshot is not updated (they already see the "before" state). The host sees the watering effect when they next load their farm.

**Why 5 minutes?** Short enough that the farm feels fresh. Long enough to avoid hammering the DB if a visitor navigates away and back. The TTL is per-visitor, so different visitors get independent snapshots.

**Rationale (developer):** Redis caching with per-visitor TTL is the standard pattern for read-heavy, slightly-stale-OK data. 5 minutes balances freshness with DB load.

**Rationale (QA):** Test: visit farm, wait 4 min, visit again → same snapshot. Wait 6 min → fresh snapshot. Test: host modifies farm while visitor is viewing → visitor doesn't see changes. Test: visitor waters → host's `grid_data` updated server-side.

---

**Q17: What happens when you water a friend's crops but they're offline?**

**Answer:** The watering effect is applied **immediately server-side** to the host's `farms.grid_data`. The host doesn't need to be online or sync.

**Flow:**
1. Visitor calls `POST /api/v1/friends/water/{userId}`.
2. Server loads host's `farms.grid_data` from `farms` table.
3. For each growing crop (not yet mature, not yet at 3 waterings), applies the watering reduction (per Q11 rules).
4. Updates `farms.grid_data` with new crop states, increments `farms.version`.
5. Records the visit in `farm_visits` with `actions_performed: ["watered"]`.
6. Sends a push notification (`FARM_VISITED`) to the host if notifications enabled.
7. If host is online (WebSocket connected), sends a `farm_visited` WebSocket event so their client can refresh the farm grid in real-time.
8. If host is offline, the watering is already in their server-side `grid_data`. When they next open the app, their client fetches the latest `grid_data` and sees the watered state.

**Concurrent watering by multiple friends:** The server processes watering requests sequentially (each request loads `grid_data`, applies watering, saves). If 2 friends water simultaneously, each is a separate DB transaction with row-level lock on the `farms` row. No conflict.

**Rationale (developer):** Server-side immediate application is the only safe model. If watering were deferred to the host's next sync, the host could make conflicting changes (e.g., harvest the crop before the watering applies), creating complex rollback scenarios.

---

**Q18: How does phone contact sync work?**

**Answer:** **Privacy-first approach — no bulk contact upload.**

1. **Permission:** App requests `READ_CONTACTS` permission with rationale: "Find friends already playing Ibibo Farms! We'll check your contacts' phone numbers — we never store your contact list."
2. **Client-side hashing:** App reads contacts locally, extracts phone numbers, normalizes them (E.164 format), computes SHA-256 hashes.
3. **Batch lookup:** App sends hashed phone numbers to `POST /api/v1/friends/contacts-lookup` (new endpoint). Server compares hashes against `users.phone_hash` column.
4. **Response:** Server returns matches: `[{ userId, display_name, avatar_url, level, is_friend: false }]` — only users who have `discoverable_by_phone = true`.
5. **Friend requests:** User sees matched contacts and can send friend requests individually. No auto-add.
6. **No storage:** Server does **not** store the hashed contact list. The lookup is stateless — hashes are compared and discarded.

**New endpoint:** `POST /api/v1/friends/contacts-lookup`

**Schema changes:** Add `phone_hash VARCHAR(64) UNIQUE` and `discoverable_by_phone BOOLEAN NOT NULL DEFAULT TRUE` columns to `users`.

**Compliance:**
- **DPDP Act:** No bulk contact data stored. Only hashed numbers transmitted for one-time lookup. Privacy policy states: "We use hashed phone numbers to find friends already on Ibibo Farms. We do not store your contact list."
- **GDPR:** Hashed phone numbers with a static app-level salt are not considered personal data under GDPR. The hash is deterministic for lookup but not reversible without the salt.
- **Permission:** `READ_CONTACTS` requested with rationale. User can deny and still use the app. Contact sync is never auto-triggered — only when user taps "Find friends from contacts."

**Rationale (product):** Phone contact sync is the #1 driver of social graph growth in Indian mobile games. But privacy is paramount — the hashed-lookup approach gives the social benefit without the privacy risk.

**Rationale (QA):** Test: 100 contacts, 5 matches → only 5 returned. Test: user with `discoverable_by_phone = false` → not returned. Test: deny permission → graceful fallback to username/friend code search.

---

**Q19: What's the QR code flow?**

**Answer:**

**My QR code (share):**
- The QR encodes a deep link: `https://ibibofarms.app/add?code={friend_code}` (using the app link domain — works both in-app and in a browser).
- Displayed on the user's profile screen as a QR image (generated client-side using the `zxing` library — no server round-trip needed).
- User can screenshot and share via WhatsApp, etc.

**Scan QR code (add friend):**
- In the Friends tab, a "Scan QR" button opens the device camera (using Google's ML Kit Barcode Scanning — on-device, no network needed).
- On scanning a valid Ibibo Farms QR, the app parses the friend code and navigates to a friend preview screen (shows display name, level, avatar) with an "Add Friend" button.
- If the QR is not a valid Ibibo Farms link: "This isn't an Ibibo Farms friend code."

**Deep link handling:**
- QR link opened on device with app installed → app opens directly to friend preview screen (via Android App Links).
- QR link opened on device without app → web page with "Get Ibibo Farms" (Play Store link) + friend code displayed for later use.

**Rationale (end user):** "I show my QR code, my friend scans it, and we're connected. No typing codes. Works in person or over WhatsApp."

**Rationale (developer):** ZXing for QR generation and ML Kit for scanning are both lightweight, well-maintained, and work offline.

**Rationale (QA):** Test: scan valid QR → friend preview. Test: scan random QR → error message. Test: scan own QR → "You can't add yourself!" Test: deep link from browser → app opens correctly.

### 22.5 Push Notifications — Timing & Logic

**Q20: When exactly is CROP_READY sent?**

**Answer:** CROP_READY is sent **server-side** when the crop matures, using a **dedicated `planted_crops` tracking table** (not by parsing `grid_data` JSONB).

**New table:**
```sql
CREATE TABLE planted_crops (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    farm_id         UUID NOT NULL REFERENCES farms(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tile_id         VARCHAR(10) NOT NULL,
    crop_id         VARCHAR(50) NOT NULL REFERENCES crop_definitions(id),
    planted_at      TIMESTAMPTZ NOT NULL,
    matures_at      TIMESTAMPTZ NOT NULL,
    withers_at      TIMESTAMPTZ NOT NULL,
    wither_warning_at TIMESTAMPTZ NOT NULL,
    notification_sent BOOLEAN NOT NULL DEFAULT FALSE,
    wither_notification_sent BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_planted_crops_matures ON planted_crops(matures_at) WHERE notification_sent = FALSE;
CREATE INDEX idx_planted_crops_wither_warn ON planted_crops(wither_warning_at) WHERE wither_notification_sent = FALSE;
CREATE INDEX idx_planted_crops_user ON planted_crops(user_id);
```

**Flow:**
1. When a `PLANT` action is processed (via sync-batch or real-time API), the server inserts a row into `planted_crops` with computed `matures_at`, `withers_at`, and `wither_warning_at` timestamps.
2. A **cron job runs every 1 minute** (Quartz Scheduler), queries `planted_crops WHERE matures_at <= NOW() AND notification_sent = FALSE`, and for each result:
   - Checks if the user has `crop_ready` notification preference enabled.
   - Checks quiet hours (see Q22).
   - Sends FCM push notification.
   - Sets `notification_sent = TRUE`.
3. When a crop is harvested or withered, the `planted_crops` row is deleted.

**Dual-write requirement:** Any server-side action that modifies crop state (PLANT, HARVEST, WITHER, WATER) must update **both** `farms.grid_data` JSONB (source of truth for client rendering) AND the `planted_crops` table (for push scheduling) within the same database transaction. On PLANT: insert `planted_crops` row. On HARVEST/WITHER: delete `planted_crops` row. On WATER (by friend): update `planted_crops.matures_at` and `planted_crops.withers_at` to reflect the reduced growth time. This ensures push notifications are always scheduled from the same state the client sees.

**Why a separate table instead of parsing JSONB?** Parsing `grid_data` JSONB every minute for 50,000 users would be extremely expensive. The `planted_crops` table with an index on `matures_at` is a simple, indexed query: `SELECT ... WHERE matures_at <= NOW() AND notification_sent = FALSE LIMIT 1000`. This scales to millions of planted crops.

**Performance:** At 50,000 DAU with ~10 crops planted per day per user = 500,000 rows/day. The 1-minute cron processes ~350 rows per minute. Each FCM send takes ~50ms. Total: ~17 seconds of processing per minute. Well within capacity.

**Client-side fallback:** If the app is open when a crop matures, the client's own timer detects this immediately and shows the harvest-ready state without waiting for the push. The push is only needed when the app is closed or backgrounded.

**Rationale (developer):** The separate table is the only scalable approach. JSONB parsing for push scheduling is an anti-pattern. The table is lightweight (one row per growing crop, deleted on harvest).

**Rationale (QA):** Test: plant crop → verify `planted_crops` row created. Test: wait until `matures_at` → verify push sent. Test: harvest before maturity → row deleted, no push. Test: notification preference OFF → no push sent. Test: quiet hours → push queued (see Q22).

---

**Q21: How does CROP_WILTING (50% warning) work?**

**Answer:** CROP_WILTING is sent when **50% of the wither window has elapsed** — i.e., the crop has been mature for half of its wither duration.

**Definition:**
- `witherWindow = growthTimeSec × witherMultiplier` (2× free, 3× premium)
- `witherWarningAt = maturesAt + (witherWindow × 0.5)`
- CROP_WILTING push is sent at `witherWarningAt`

**Example:** Wheat (30s growth, free player, 2× wither):
- `maturesAt` = plantedAt + 30s
- `witherWindow` = 30s × 2 = 60s
- `withersAt` = maturesAt + 60s
- `witherWarningAt` = maturesAt + 30s (50% of 60s wither window)
- Player gets a warning 30 seconds after maturity, with 30 seconds left to harvest.

**Implementation:** The `planted_crops` table (from Q20) includes `wither_warning_at` and `wither_notification_sent` columns. The same 1-minute cron job that sends CROP_READY also checks for `wither_warning_at <= NOW() AND wither_notification_sent = FALSE` and sends CROP_WILTING pushes.

**Rationale (product):** "50% of the wither window remains" is the most useful warning point. Too early (at maturity) and players ignore it. Too late (80% elapsed) and they've already lost the crop. 50% gives a fair chance to harvest.

**Rationale (QA):** Test: verify `witherWarningAt` = `maturesAt + (growthTimeSec × witherMultiplier × 0.5)`. Test: premium player (3×) gets warning later than free player (2×). Test: crop harvested before warning → no CROP_WILTING sent. Test: crop already withered before cron runs → no CROP_WILTING sent.

---

**Q22: How are quiet hours enforced server-side?**

**Answer:** A **delayed queue** using Redis Sorted Sets, processed by a cron job.

**Flow:**
1. When the notification cron (from Q20) prepares to send a non-critical notification, it checks the user's `quiet_hours_start` and `quiet_hours_end` (in `users` table) against the **user's timezone** (`users.timezone` column, IANA format).
2. If the current time (in the user's timezone) falls within quiet hours:
   - The notification is **not sent immediately**.
   - Instead, it's added to a Redis Sorted Set: `quiet_queue:{userId}` with score = epoch timestamp of quiet hours end (converted to UTC).
   - The notification content is stored as the member value (JSON: type, title, body, data).
3. A **cron job runs every 5 minutes** that:
   - Scans all `quiet_queue:*` keys (via Redis SCAN).
   - For each key, checks if current UTC time >= score (quiet hours end).
   - If yes, sends all queued notifications via FCM and deletes the key.
4. **Critical notifications** (`GIFT_RECEIVED`, `MARKET_SOLD`) **bypass quiet hours entirely** — sent immediately regardless of quiet hours (per Section 14.3).

**Timezone handling:** Server converts the user's `quiet_hours_end` (e.g., "08:00") to UTC using the user's `timezone` (e.g., "Asia/Kolkata" → UTC+5:30 → "08:00" IST = "02:30" UTC). This correctly handles DST (not applicable to India but relevant for future global users).

**Max queue size:** 10 notifications per user in quiet queue. If more than 10 non-critical notifications accumulate during quiet hours, only the 10 most recent are delivered (older ones dropped). Prevents a notification flood when quiet hours end.

**Rationale (developer):** Redis Sorted Sets with timestamp scores is the standard pattern for delayed delivery. The 5-minute cron is lightweight (SCAN + ZRANGEBYSCORE). No heavy DB queries needed.

**Rationale (product):** Players in India who sleep 10 PM – 8 AM won't be woken up by "your crop is ready" but will still get "you received a gift" immediately. When they wake up, they get the queued crop notifications.

**Rationale (QA):** Test: notification at 10:30 PM IST with quiet hours 22:00–08:00 → queued, delivered at 08:00 IST. Test: critical notification at 10:30 PM → sent immediately. Test: 15 non-critical notifications during quiet hours → only 10 delivered. Test: timezone conversion.

### 22.6 Monetization Edge Cases

**Q23: What happens to premium-only crops when subscription expires?**

**Answer:**

| State | Behavior |
|---|---|
| Currently growing in farm | **Finishes normally.** The crop grows to maturity and can be harvested. Premium crop status is "grandfathered" for crops already planted. |
| Seeds in inventory | **Can be sold at NPC shop** (at 50% of seed cost, same as any seed sell-back). **Cannot be planted.** UI shows: "Premium subscription required to plant this crop." |
| Harvested crop in inventory | **Can be sold normally** (NPC shop or market). No restriction — the player earned these while premium. |
| Premium decorations placed on farm | **Stay placed.** Decorations are permanent once placed. |
| Premium decorations in inventory (not placed) | **Can be placed.** Once owned, decorations can be placed regardless of subscription status. Only the *purchase* requires premium. |

**Rationale (product):** The principle is: "you keep what you earned." Premium is about access, not ownership. This is player-friendly and avoids the negative experience of losing items you paid for.

**Rationale (end user):** "My subscription expired but my premium crops still grow and I can sell my premium seeds back. I just can't plant new ones until I resubscribe."

**Rationale (QA):** Test: premium crop growing when sub expires → verify it matures and can be harvested. Test: try to plant premium seed with expired sub → blocked. Test: sell premium seed to NPC → allowed at 50%. Test: place premium decoration with expired sub → allowed.

---

**Q24: How does the "daily double" ad reward interact with quest rewards?**

**Answer:**

- **Applies to:** Daily quests only (not story quests, not seasonal quests).
- **Doubles:** Coins only. XP is **not** doubled. Gems (if any) are **not** doubled. Item rewards are **not** doubled.
- **Frequency:** 1×/day (existing spec, Section 10.4).
- **Flow:**
  1. Player completes a daily quest and taps "Claim."
  2. UI shows: "Claim 50 coins + 20 XP" with a "Watch ad for 2× coins" button.
  3. If player watches ad → claims 100 coins + 20 XP (coins doubled, XP unchanged).
  4. If player skips → claims 50 coins + 20 XP.
  5. The daily double is consumed for the day. No other quest can use it that day.
- **Ad reward log:** Recorded in `ad_watch_log` with `placement = 'daily_double'`, `reward_type = 'quest_double'`, `reward_amount = 50` (the extra coins).

**Rationale (product):** Doubling coins (the soft currency) is valuable but not game-breaking. Doubling XP would accelerate progression too much. Limiting to daily quests keeps the scope tight. 1×/day prevents ad spam.

**Rationale (end user):** "I finished my daily quest and got double coins by watching an ad. Worth it for 30 seconds."

**Rationale (QA):** Test: claim with ad → verify 2× coins, 1× XP. Test: claim second daily quest with ad → "Daily double already used today." Test: claim story quest with ad → no daily double option shown.

---

**Q25: What's the "free tiles" in the Starter Bundle?**

**Answer:** "5 free tiles" means a **flat 500-coin discount** on the next grid expansion — not a literal 5-tile addition (which would break the N×N grid model).

**How it works:**
- The player receives a one-time `grid_expansion_discount` of 500 coins, stored as a column `grid_expansion_discount INT NOT NULL DEFAULT 0` on the `users` table. When applied, it is set to 0.
- When the player next expands their grid (e.g., 6×6 → 7×7, which costs 2,000 coins), the discount is applied: cost = 1,500 coins.
- The discount is consumed on first use.

**Note:** The Starter Bundle description in Section 10.2 has been updated in-place to: "1,000 coins + 10 gems + 500-coin grid expansion discount."

**Rationale (product):** The flat discount is simpler for players to understand and for developers to implement. Pro-rating tiles adds complexity for minimal benefit.

**Rationale (QA):** Test: buy Starter Bundle → verify discount applied on next grid expansion. Test: discount consumed after first expansion. Test: discount not applied to non-grid purchases.

### 22.7 Technical Architecture

**Q26: How does the server know which crops are planted to send CROP_READY pushes?**

**Answer:** See **Q20** — a dedicated `planted_crops` table is used for server-side crop tracking and push scheduling. The server does **not** parse `grid_data` JSONB for push notifications.

**Summary:**
- `planted_crops` table stores one row per growing crop with `matures_at`, `withers_at`, and `wither_warning_at` timestamps.
- 1-minute cron job queries `planted_crops WHERE matures_at <= NOW() AND notification_sent = FALSE` and sends pushes.
- On harvest/wither, the `planted_crops` row is deleted.
- On watering (by friend), the `planted_crops` row is updated with new `matures_at`.
- `farms.grid_data` JSONB remains the source of truth for the farm grid state (what the client renders). `planted_crops` is a server-side projection table for push scheduling only.

---

**Q27: How does "priority sync" for premium work?**

**Answer:**

| Aspect | Free Users | Premium Users |
|---|---|---|
| WorkManager sync interval | 15 minutes | 5 minutes |
| API rate limit | 100 req/min | 200 req/min |
| Sync endpoint | `POST /api/v1/farm/sync-batch` | Same endpoint, but premium requests are prioritized in the server queue |
| Server queue priority | Standard queue | Priority queue (processed first when server is under load) |

**Implementation:**
- **Client:** WorkManager uses different `PeriodicWorkRequest` intervals based on subscription status. When subscription status changes (upgrade/downgrade), the WorkManager request is rescheduled.
- **Server:** The sync-batch endpoint checks `is_premium` and routes premium requests to a priority queue (Redis). Under normal load, both queues are processed immediately. Under high load (> 80% CPU), the priority queue is drained first, then the standard queue.

**Rationale (product):** 5-minute vs 15-minute sync interval is a meaningful perk for active premium players who want their actions confirmed faster. The priority queue ensures premium users aren't affected by server congestion during peak hours.

**Rationale (end user):** "As a premium player, my farm syncs every 5 minutes instead of 15. My progress is saved faster."

**Rationale (QA):** Test: premium user sync interval = 5 min. Test: free user sync interval = 15 min. Test: downgrade from premium → interval changes to 15 min. Test: server under load → premium requests processed first.

---

**Q28: What's the WebSocket authentication flow?**

**Answer:**

**Connection:**
1. Client obtains a JWT access token (15-min TTL) via standard auth flow.
2. Client calls `POST /api/v1/auth/ws-ticket` (REST, authenticated with JWT) to get a short-lived (30-second) ticket.
3. Client opens WebSocket connection to `wss://api.ibibofarms.app/ws/v1?ticket={ticket}`.
4. Server validates the ticket on connection upgrade. If invalid/expired, returns `401 Unauthorized` and refuses the upgrade. Ticket is single-use and discarded after validation.
5. If valid, server establishes the WebSocket connection, associates the socket with the user ID, and registers presence in Redis (`presence:{userId} = socket_id`, TTL 60 seconds, refreshed by heartbeat).

**JWT expiry mid-session:**
1. Client sets a timer 1 minute before JWT expiry.
2. Client uses the refresh token to obtain a new JWT via `POST /api/v1/auth/refresh` (REST call, not over WebSocket).
3. Client sends a `auth_refresh` event over WebSocket with the new JWT: `{ event: "auth_refresh", token: "new_jwt" }`.
4. Server validates the new JWT, updates the association, and responds with `{ event: "auth_refreshed" }`.
5. If the client fails to refresh before expiry, the server sends `{ event: "auth_expired" }` and closes the connection with code `4401`. The client must reconnect with a fresh token.

**Heartbeat:**
- Client sends `{ event: "ping" }` every 30 seconds.
- Server responds with `{ event: "pong" }`.
- If no ping received for 90 seconds, server considers the connection dead and closes it (presence removed).

**Security:**
- The ticket pattern is used instead of passing JWT in the URL. WebSocket APIs don't support custom headers on upgrade, so a short-lived ticket (30s TTL, single-use) avoids exposing the JWT in URLs or server logs.
- The `auth_refresh` event carries the new JWT over the already-authenticated WebSocket channel, not in a URL.

**New endpoint:** `POST /api/v1/auth/ws-ticket` → returns `{ ticket: "short_lived_token", expiresIn: 30 }`.

**Rationale (developer):** The ticket pattern is the standard for WebSocket auth in mobile apps. It avoids JWT exposure in URLs/logs while keeping the flow simple.

**Rationale (QA):** Test: valid ticket → connection established. Test: expired ticket → connection refused. Test: JWT expires mid-session → `auth_refresh` event → connection maintained. Test: no refresh before expiry → `auth_expired` + disconnect. Test: no ping for 90s → connection closed.

---

**Q29: How are leaderboard rankings computed and cached?**

**Answer:** **Redis Sorted Sets** for real-time ranking, with periodic PostgreSQL snapshots for persistence.

**Implementation:**

| Board | Redis Key | Score | Member | Update Trigger |
|---|---|---|---|---|
| Weekly Harvest | `leaderboard:weekly_harvest` | `total_harvest_count` (weekly) | `userId` | On every harvest action (server increments score) |
| Farm Value | `leaderboard:farm_value` | `farm_value` | `userId` | On every `farm_value` recalculation (6-hour cron) |
| Friends | `leaderboard:friends:{userId}` | `xp` | `friendUserId` | On XP change (computed per-user from friends list) |
| Seasonal Event | `leaderboard:seasonal:{eventId}` | Event-specific metric | `userId` | On event action (e.g., harvest event crop) |

**Read flow:**
1. Client calls `GET /api/v1/leaderboard/weekly?limit=20&offset=0`.
2. Server uses Redis `ZREVRANGE leaderboard:weekly_harvest {offset} {offset+limit-1} WITHSCORES` — O(log N + limit) time complexity.
3. Server enriches with user display names/avatars from PostgreSQL (batch `SELECT` by user IDs).
4. Returns ranked list.

**Pagination:** Cursor-based. `limit=20` per page. `offset` is the rank position. For "Top 100" boards, the client can request up to 100 entries. For friends leaderboard, all friends are returned (max 50 or 200).

**Caching:** Redis Sorted Sets are the live cache — always up-to-date. No separate cache layer needed.

**Persistence:** Every Monday 00:00 UTC (existing cron, Section 5.1.7), the weekly leaderboard is snapshotted to PostgreSQL (`leaderboard_snapshots` table) for historical records, then the Redis key is reset (scores zeroed).

**Friends leaderboard:** Computed on-demand. Server fetches the user's friend list, then `ZMSCORE leaderboard:farm_value {friendIds...}` to get scores. Sorted client-side or server-side. Cached in Redis for 5 minutes under key `leaderboard:friends:{userId}`.

**Performance:** Redis `ZREVRANGE` is O(log N + limit). For 500,000 users, log(500000) ≈ 19. With limit=20, total ~39 operations — sub-millisecond. Enrichment with PostgreSQL: batch SELECT by 20 user IDs, ~2ms. Total: < 5ms per leaderboard request.

**Rationale (developer):** Redis Sorted Sets are purpose-built for leaderboards. They maintain sorted order on insert/update with O(log N) complexity. No need for periodic PostgreSQL queries.

**Rationale (QA):** Test: 1000 users, verify correct ranking. Test: user harvests → score increments in Redis. Test: weekly reset → scores zeroed, snapshot saved. Test: friends leaderboard with 50 friends → all returned, correctly sorted. Test: pagination (offset=20, limit=20) → returns ranks 21-40.

---

**Q30: How does the admin panel work?**

**Answer:** The admin panel is a **separate web application** — not part of the Android app.

**Tech stack:**

| Layer | Technology | Rationale |
|---|---|---|
| Frontend | React + TailwindCSS | Fast to build, responsive, wide ecosystem |
| Hosting | AWS S3 + CloudFront (static hosting) | Simple, cheap, no server needed for SPA |
| Auth | Same JWT system as app, but with admin-specific claims (`role: "admin"`) | Reuses existing auth infrastructure |
| API | Same Ktor backend, under `/api/v1/admin/` prefix | No separate backend needed |
| Access control | IP allowlist (configured in Ktor) + 2FA (TOTP via Google Authenticator) | Per Section 5.1.6 |

**Admin API endpoints (new, under `/api/v1/admin/`):**

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/admin/login` | Admin login (email + password + 2FA TOTP) |
| GET | `/api/v1/admin/users` | Search/list users (filter by username, phone, ban status) |
| POST | `/api/v1/admin/users/{id}/ban` | Ban user (specify reason + duration) |
| POST | `/api/v1/admin/users/{id}/unban` | Unban user |
| GET | `/api/v1/admin/economy` | Economy dashboard data (coin supply, faucet/sink, inflation rate) |
| GET | `/api/v1/admin/flagged` | Flagged accounts (cheating, market manipulation, multi-account) |
| GET | `/api/v1/admin/config/crops` | View crop definitions |
| PUT | `/api/v1/admin/config/crops/{id}` | Update crop definition (price, growth time, etc.) |
| POST | `/api/v1/admin/config/crops` | Create new crop definition |
| GET | `/api/v1/admin/config/quests` | View quest definitions |
| PUT | `/api/v1/admin/config/quests/{id}` | Update quest definition |
| GET | `/api/v1/admin/events` | View/manage seasonal events |
| POST | `/api/v1/admin/events` | Create seasonal event |
| PUT | `/api/v1/admin/events/{id}` | Update seasonal event |
| GET | `/api/v1/admin/audit-log` | View admin action audit log |

**Admin UI screens:**
1. **Dashboard** — Overview: DAU, new users, economy health, flagged accounts count
2. **User Management** — Search, ban/unban, view user details (farm, transactions, devices)
3. **Economy Monitor** — Coin supply chart, faucet/sink breakdown, inflation rate, price logs
4. **Game Config** — CRUD for crops, animals, decorations, buildings, quests, achievements
5. **Events Manager** — Create/edit/schedule seasonal events
6. **Flagged Accounts** — Review flagged users, take action (ban, shadow-ban, dismiss)
7. **Audit Log** — All admin actions with timestamp, admin user, action, target

**Security:**
- Admin JWT has `role: "admin"` claim. Validated on every admin API request.
- IP allowlist: admin endpoints only accessible from configured IP ranges (office VPN, specific IPs).
- 2FA: TOTP required at login. Admin JWT has 1-hour TTL (shorter than user JWT).
- Audit log: every admin action is logged in `admin_audit_log` table (admin_user_id, action, target_id, details, timestamp).

**New table:**
```sql
CREATE TABLE admin_audit_log (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_user_id   UUID NOT NULL REFERENCES users(id),
    action          VARCHAR(50) NOT NULL,
    target_type     VARCHAR(30),
    target_id       UUID,
    details         JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_admin_audit_admin ON admin_audit_log(admin_user_id, created_at DESC);
CREATE INDEX idx_admin_audit_target ON admin_audit_log(target_type, target_id);
```

**Rationale (developer):** A separate web app is the standard pattern for admin panels. It keeps the Android app focused on the player experience and allows non-technical staff (product managers, community managers) to manage the game without an Android device.

**Rationale (QA):** Test: admin login with 2FA. Test: non-admin JWT → 403 on admin endpoints. Test: IP outside allowlist → 403. Test: ban user → user can't login. Test: update crop definition → config version bumps, clients re-fetch. Test: audit log records all actions.

### 22.8 Data Model Ambiguities

**Q31: Where are decoration/animal placements stored on the grid?**

**Answer:** Both decorations and animals are stored as **tiles** in the `tiles` array within `grid_data` JSONB. The `decorations` array in Appendix B is **removed** — it was an inconsistency. The unified model:

**Tile types in `grid_data.tiles`:**

| `type` | Contents | Can overlap with crops? |
|---|---|---|
| `GRASS` | Empty, buildable | N/A (no crop) |
| `TILLED` | Prepared for planting | N/A (no crop yet) |
| `PLANTED` | Has a `crop` object | N/A (the crop IS the tile content) |
| `ANIMAL` | Has an `animal` object | No — animal occupies the tile |
| `BUILDING` | Has a `building` object | No — building occupies the tile |
| `DECORATION` | Has a `decoration` object | **Depends on decoration type** |
| `WATER` | Decorative pond | No |
| `PATH` | Decorative walkway | No |

**Decoration overlap rules:**

| Decoration Category | Can overlap crops? | Can overlap tilled soil? | Can overlap other decorations? |
|---|---|---|---|
| `fence` | No | No | No |
| `path` | No | No | No |
| `lighting` | **Yes** (lamp post on a tilled tile) | Yes | Yes |
| `nature` (tree, bush, flower bed) | No | No | No |
| `misc` (garden gnome, statue, fountain) | No | No | No |

**Implementation:** Lighting decorations (lamp posts, string lights) are "overlays" — they can be placed on top of any tile except `WATER`. They're stored as a separate `overlays` array in `grid_data`:

```json
{
  "tiles": [ ... ],
  "overlays": [
    { "tileId": "0,0", "decorationId": "lamp_post" }
  ]
}
```

All other decorations are tiles with `type: "DECORATION"`. You cannot place a fence on a tilled tile — the UI prevents it. If a player tries, they see: "Clear this tile first."

**Note:** Appendix B has been updated in-place — the `decorations` array is replaced by the `overlays` array (for lighting only). All other decorations are tiles.

**Rationale (developer):** Unifying decorations as tiles (except lighting overlays) simplifies the grid model. Each tile has exactly one primary content type. Overlays are the exception for lighting, which logically sits "on top of" a tile without replacing its content.

**Rationale (end user):** "I can put a lamp post next to my crops to light up the farm at night. But I can't put a fence on top of my wheat — that doesn't make sense."

**Rationale (QA):** Test: place fence on tilled tile → rejected. Test: place lamp post on tilled tile → allowed (overlay). Test: place lamp post on water tile → rejected. Test: place tree on planted crop → rejected. Test: remove crop → tile returns to `TILLED`, overlay (lamp post) stays.

---

**Q32: How does inventory capacity interact with buildings on the grid?**

**Answer:**

| Rule | Detail |
|---|---|
| Capacity bonus is **global** | Barn gives +150 crop slots to the player's total inventory capacity, not per-barn. |
| Stacking | **Yes, stacking is allowed.** 2 barns = +300 crop slots. 3 barns = +450. |
| Stacking limit | **Max 3 buildings of the same type.** A player can place at most 3 barns, 3 silos, 3 farmhouses. This prevents unlimited capacity. |
| Building footprint | Each barn occupies 4 tiles (2×2). 3 barns = 12 tiles. On a 12×12 grid (144 tiles), this is 8.3% — a meaningful trade-off. |
| Capacity recalculation | When a building is placed, inventory capacity increases immediately. When a building is removed/sold, capacity decreases. If current inventory exceeds new capacity, the player cannot harvest/collect until they sell items to get under capacity. |

**Capacity formula:**
```
total_crop_capacity = base_capacity (100) + (num_barns × 150)
total_seed_capacity = base_capacity (50 per type) + (num_silos × 100)
total_decoration_capacity = base_capacity (30) + (num_farmhouses × 50)
```

**Premium override:** Premium subscribers have **unlimited** inventory capacity regardless of buildings. If premium expires, the capacity reverts to the building-based formula. If current inventory exceeds the new capacity, the player can still sell items but cannot harvest/collect new ones.

**Rationale (product):** Stacking with a limit of 3 gives players flexibility to invest in storage without making it unlimited. The tile footprint cost (12 tiles for 3 barns) creates a meaningful trade-off between storage and farm space.

**Rationale (end user):** "I can build up to 3 barns for more storage, but each one takes 4 tiles. Do I want more storage or more farming space?"

**Rationale (QA):** Test: place 1 barn → +150 capacity. Test: place 3 barns → +450. Test: place 4th barn → rejected. Test: remove barn with over-capacity inventory → blocked with "Sell items first." Test: premium with 0 barns → unlimited. Test: premium expires with 500 crops and 0 barns → can sell but not harvest.

---

**Q33: What's the `version` field in `farms` for?**

**Answer:** The `version` field is for **optimistic concurrency control**, and the spec's mention of "last-write-wins" in Section 4.2 is **misleading and corrected here**.

**Corrected conflict resolution model:**

The system uses **optimistic concurrency with server-side validation**, not naive last-write-wins. Here's how it works:

1. **Client reads farm state:** Client receives `grid_data` with `version: N`.
2. **Client makes actions offline:** Actions are queued in `PendingActionEntity` with the `version` at the time of each action.
3. **Client syncs:** Client sends `sync-batch` with actions and the `baseVersion` (the `version` the client had when the actions were performed).
4. **Server validates:**
   - If `baseVersion == server.version`: **No conflict.** Server applies all actions, increments `version` by the number of accepted actions, returns new state with `version: N + accepted_count`.
   - If `baseVersion < server.version`: **Conflict detected.** Another device/session has synced since this client last fetched. Server processes actions using **per-tile last-write-wins** (per Q2): each action's `clientTimestamp` is compared against the tile's `lastModifiedAt`. Accepted actions update the tile and increment `version`. Rejected actions (`STALE_TILE`) don't increment `version`.
   - If `baseVersion > server.version`: **Impossible** (client has a newer version than server). This indicates a bug or tampering. Server rejects the entire batch with `400 BAD_REQUEST` and `error.code = "VERSION_AHEAD"`.

5. **Client updates:** Client replaces local state with server-confirmed state and `version`.

**Why this isn't "last-write-wins":** Naive last-write-wins would accept the entire client farm state regardless of what changed on the server. This would overwrite other devices' changes. The per-tile timestamp comparison (Q2) ensures that only tiles the client actually modified are updated, and only if they haven't been modified more recently on the server.

**Note:** Section 4.2 has been updated in-place to reflect this corrected conflict resolution model.

**Rationale (developer):** Optimistic concurrency is the standard pattern for multi-device sync. The `version` field is the optimistic lock token. Per-tile resolution (rather than full-state rejection) ensures that concurrent changes to different tiles don't conflict — only same-tile changes trigger the `STALE_TILE` path.

**Rationale (QA):** Test: sync with `baseVersion == server.version` → all actions processed, version increments. Test: sync with `baseVersion < server.version` → per-tile conflict resolution. Test: sync with `baseVersion > server.version` → rejected. Test: two devices modify different tiles → both syncs succeed, no conflict. Test: two devices modify same tile → second sync gets `STALE_TILE` for that tile only.

---

> **End of Section 22 — Implementation Questions Resolved**
>
> All 33 questions have been answered with normative specifications. All corrections have been **propagated to the original sections** — the original text has been updated in-place. The changes are:
> - **Q9:** Updates `CropStage` enum in Appendix A (adds `SEED`, `SPROUT` stages). ✅ Applied to Appendix A.
> - **Q10:** Corrects Appendix A watering logic (replaces `effectiveGrowthTimeSec` with `wateringReductionSec`). ✅ Applied to Appendix A.
> - **Q11:** Updates `PlantedCrop` data model (adds `wateringCount`, changes `wateredBy` to list). ✅ Applied to Appendix A and Appendix B.
> - **Q12:** Corrects Level 12 unlock description. ✅ Applied to leveling table in Section 8.
> - **Q13:** Adds `objectives JSONB` column to `quest_definitions`. ✅ Applied to Section 6 schema.
> - **Q14:** Adds `objective_type` and `objective_target` columns to `achievement_definitions`. ✅ Applied to Section 6 schema.
> - **Q18:** Adds `phone_hash` and `discoverable_by_phone` columns to `users`. ✅ Applied to Section 6 schema.
> - **Q20:** Adds `planted_crops` table. ✅ Applied to Section 6 schema + indexes + Room DB entities.
> - **Q25:** Updates Starter Bundle description. ✅ Applied to Section 10.2 IAP catalog. Adds `grid_expansion_discount` column to `users`.
> - **Q28:** Adds `POST /api/v1/auth/ws-ticket` endpoint. ✅ Applied to Section 13.1 Auth endpoints. Ticket flow is the sole auth method (query-param JWT removed).
> - **Q30:** Adds `admin_audit_log` table and admin API endpoints. ✅ Applied to Section 6 schema + Section 13.1 admin endpoints. Adds `role` column to `users` for admin accounts.
> - **Q31:** Replaces `decorations` array in Appendix B with `overlays` array (lighting only); other decorations become tiles. ✅ Applied to Appendix B.
> - **Q33:** Corrects Section 4.2 conflict resolution description. ✅ Applied to Section 4.2.
>
> New endpoints added (all in Section 13.1): `POST /api/v1/farm/sync-batch`, `POST /api/v1/friends/contacts-lookup`, `POST /api/v1/auth/ws-ticket`, admin endpoints under `/api/v1/admin/`.
>
> New tables added (all in Section 6 schema): `planted_crops`, `admin_audit_log`, `synced_actions` (idempotency log for Q1), `leaderboard_snapshots` (historical snapshots for Q29).
>
> New columns added to `users`: `phone_hash`, `discoverable_by_phone`, `role`, `grid_expansion_discount`.
>
> New cron jobs added to Section 5.1.7: crop notification dispatch (1 min), quiet hours delivery (5 min).
>
> These changes must go through the Change Management process defined in Section 21.3.
