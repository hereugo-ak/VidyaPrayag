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
3. **Conflict resolution:** Last-write-wins for farm state; server-authoritative for economy (coins, purchases).
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
    friend_code     VARCHAR(6) UNIQUE NOT NULL,      -- 6-char alphanumeric for friend invites
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
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE user_quests (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    quest_id        VARCHAR(50) NOT NULL REFERENCES quest_definitions(id),
    progress        INT NOT NULL DEFAULT 0,
    status          VARCHAR(15) NOT NULL DEFAULT 'active',  -- 'active', 'completed', 'claimed'
    assigned_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    assigned_date   DATE NOT NULL DEFAULT CURRENT_DATE,  -- for daily quest reset/expiry
    completed_at    TIMESTAMPTZ,
    claimed_at      TIMESTAMPTZ,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
-- Note: UNIQUE(user_id, quest_id) is NOT suitable for daily quests since the same quest_id
-- is re-assigned daily. Use UNIQUE(user_id, quest_id, assigned_date) for daily quests.
-- For story/achievement quests, use UNIQUE(user_id, quest_id).

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
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
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
    streak_days     INT NOT NULL,              -- streak count at time of claim (snapshot)
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
CREATE UNIQUE INDEX idx_user_quests_daily_unique ON user_quests(user_id, quest_id, assigned_date);  -- daily quest dedup
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
| 12 | 17,000 | Second animal pen (place 2 animals on same tile) |
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
| starter_bundle | Starter Bundle | ₹149 | 1,000 coins + 10 gems + 5 free tiles |
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

#### Farm

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/farm` | Get current user's farm state |
| PUT | `/api/v1/farm` | Sync full farm state (with version) |
| POST | `/api/v1/farm/plant` | Plant a crop on a tile |
| POST | `/api/v1/farm/harvest` | Harvest a mature crop |
| POST | `/api/v1/farm/till` | Till a grass tile |
| POST | `/api/v1/farm/place` | Place decoration/animal/building |
| POST | `/api/v1/farm/remove` | Remove item from tile |
| POST | `/api/v1/farm/expand` | Expand grid size |
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
| POST | `/api/v1/market/listings` | Create a listing |
| DELETE | `/api/v1/market/listings/{id}` | Cancel own listing |
| POST | `/api/v1/market/listings/{id}/buy` | Buy from a listing |

#### Quests

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/quests/active` | Get active quests + progress |
| GET | `/api/v1/quests/daily` | Get today's daily quests |
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

#### Notification Preferences

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/notifications/preferences` | Get notification preferences + quiet hours |
| PUT | `/api/v1/notifications/preferences` | Update notification preferences + quiet hours |
| GET | `/api/v1/notifications/unread-count` | Get unread notification count (for badge) |
| POST | `/api/v1/notifications/{id}/read` | Mark notification as read |
| POST | `/api/v1/notifications/read-all` | Mark all notifications as read |

#### Game Config

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/config/version` | Get current config version (for cache check) |
| GET | `/api/v1/config/crops` | Get all crop definitions |
| GET | `/api/v1/config/animals` | Get all animal definitions |
| GET | `/api/v1/config/quests` | Get all quest definitions |
| GET | `/api/v1/config/events` | Get active seasonal events |
| GET | `/api/v1/config/decorations` | Get all decoration definitions |
| GET | `/api/v1/config/buildings` | Get all building definitions |
| GET | `/api/v1/config/achievements` | Get all achievement definitions |
| GET | `/api/v1/config/levels` | Get level/XP table |
| GET | `/api/v1/config/daily-rewards` | Get daily reward tier table |

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
    val watered: Boolean = false,        // true if a friend watered this crop
    val wateredBy: String? = null,       // user UUID of friend who watered
    val wateredAt: Long? = null,         // epoch millis when watered
) {
    // Watering reduces remaining growth time by 10%. Applied at calculation time, not retroactively.
    val effectiveGrowthTimeSec: Long get() =
        if (watered) (growthTimeSec * 0.9).toLong() else growthTimeSec

    val maturesAt: Long get() = plantedAt + (effectiveGrowthTimeSec * 1000)
    val withersAt: Long get() = maturesAt + (growthTimeSec * 1000 * witherMultiplier.toLong())  // 2x or 3x original growth time

    fun getStage(currentTime: Long = System.currentTimeMillis()): CropStage {
        return when {
            currentTime < maturesAt -> CropStage.GROWING(progress = ((currentTime - plantedAt).toFloat() / (maturesAt - plantedAt)))
            currentTime < withersAt -> CropStage.MATURE
            else -> CropStage.WITHERED
        }
    }
}

enum class CropStage { GROWING, MATURE, WITHERED }
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
        "watered": true,
        "wateredBy": "user_uuid_here",
        "wateredAt": 1719561700000
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
  "decorations": [
    { "tileId": "3,4", "decorationId": "fence_wood" }
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
