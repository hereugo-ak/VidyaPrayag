# Multi-Language Support — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** None
> **Source:** `DIFFERENTIATING_FEATURES.md` §8.1
> **Template:** `_SPEC_TEMPLATE.md` v1 (25 mandatory + 6 optional sections)

---

## 1. Feature Overview

### What

Full multi-language support for the app UI and content: 10 Indian languages (English, Hindi, Bengali, Tamil, Telugu, Marathi, Gujarati, Kannada, Malayalam, Punjabi) with per-user language preference, server-side content translation, and locale-aware formatting.

### Why — Product Rationale

India has 22 official languages and hundreds of dialects. Most school ERP apps are English-only, excluding non-English-speaking parents (grandparents, rural families). Multi-language support is critical for adoption across Tier 2/3/4 cities and rural areas where parents may be more comfortable in their regional language.

This is a **differentiating feature** (Priority P1, Phase 2, effort L, "High" value per `DIFFERENTIATING_FEATURES.md`). It dramatically expands the addressable market by making the app accessible to non-English-speaking parents.

### What Stands Out (Competitive Moat)

From `DIFFERENTIATING_FEATURES.md` §8.1:
> "Multi-Language — 10 Indian languages, per-user preference, server-side content translation. Data readiness: languagePref field exists."

No major school ERP supports 10 Indian languages with AI-powered content translation. Most offer English only or 2-3 languages.

### Goals

- App UI translated into 10 languages (Compose Multiplatform string resources)
- Per-user language preference (stored in `app_users.languagePref` — field already exists)
- Server-side content translation for announcements, notifications (AI-powered)
- Locale-aware date, number, and currency formatting
- RTL support (for Urdu/Arabic if added later)
- Language switcher in settings (no app restart required)

### Non-goals

- [ ] Voice-based language switching
- [ ] Auto-detection of user's language from phone settings (manual selection only)
- [ ] Translation of user-generated content (posts, messages)
- [ ] OCR translation of uploaded documents
- [ ] RTL layout for existing languages (only LTR for initial 10 languages)
- [ ] Language-specific fonts (system fonts used)

### Dependencies

- `AppUsersTable.languagePref` — existing field (VARCHAR, default 'en')
- `SchoolsTable.mediumOfInstruction` — existing field
- `WHATSAPP_INTEGRATION_SPEC.md` — WhatsApp templates support multi-language
- `AI_INFRASTRUCTURE_SPEC.md` — AI service for content translation
- `Notify.kt` — notification dispatch (modified for translation)
- `AI_REPORT_CARD_SPEC.md` — report card translation (existing integration)

### Related Modules

- `shared/.../core/locale/` — new locale management module
- `server/.../feature/i18n/` — new internationalization module
- `server/.../feature/notifications/` — notification dispatch (modified)
- `composeApp/.../ui/v2/screens/settings/` — settings UI (modified)
- `composeApp/.../ui/v2/screens/**/*.kt` — all screens (modified for string resources)

---

## 2. Current System Assessment

### Existing Code

- `AppUsersTable` has `languagePref` field (VARCHAR, default 'en')
- `SchoolsTable` has `mediumOfInstruction` field
- WhatsApp templates support multi-language (from `WHATSAPP_INTEGRATION_SPEC.md`)
- No UI translations exist — all strings are hardcoded in English
- `DIFFERENTIATING_FEATURES.md` §8.1: Multi-Language, effort L, data readiness: "languagePref field exists"

### Existing Database

- `AppUsersTable.languagePref` — VARCHAR, default 'en'. Already exists, not used.
- `SchoolsTable.mediumOfInstruction` — existing field for school's medium of instruction

### Existing APIs

- No language-related API endpoints exist
- `PATCH /api/v1/user/profile` — could be extended, but dedicated endpoint preferred

### Existing UI

- All UI strings hardcoded in English
- No string resource files
- No locale management
- Settings screen exists but no language switcher

### Existing Services

- `Notify.kt` — notification dispatch (no translation)
- `NotificationService.kt` — FCM push dispatch (no translation)
- No content translation service

### Existing Documentation

- `DIFFERENTIATING_FEATURES.md` §8.1 — Multi-Language
- `WHATSAPP_INTEGRATION_SPEC.md` — WhatsApp multi-language templates
- `AI_INFRASTRUCTURE_SPEC.md` — AI service (for translation)
- `AI_REPORT_CARD_SPEC.md` — Report card translation (existing integration)

### Technical Debt

| # | Gap | Details |
|---|---|---|
| TD-1 | All UI strings hardcoded | No string resource files — strings embedded in Compose code |
| TD-2 | No locale management | No `LocaleManager`, no `CompositionLocal` for locale |
| TD-3 | No content translation | No `ContentTranslationService` — all content English only |
| TD-4 | No locale-aware formatting | Dates, numbers, currency all in English/US format |
| TD-5 | `languagePref` field unused | Field exists in DB but never read or written |
| TD-6 | No language switcher UI | No settings UI for language selection |

### Gaps

| # | Gap | Impact | Severity |
|---|---|---|---|
| G1 | English-only UI | Excludes non-English-speaking parents | **High** |
| G2 | No content translation | Announcements, notifications in English only | **High** |
| G3 | No locale-aware formatting | Dates/numbers in US format, not Indian | **Medium** |
| G4 | `languagePref` field unused | Existing infrastructure wasted | **Low** |
| G5 | No language switcher | Users cannot change language | **High** |

---

## 3. Functional Requirements

### FR-001
| Field | Value |
|---|---|
| **Title** | UI String Externalization |
| **Description** | All UI strings externalized to string resources for 10 languages. |
| **Priority** | Critical |
| **User Roles** | System |
| **Acceptance notes** | 10 string resource files (one per language). All hardcoded strings replaced with resource references. |

### FR-002
| Field | Value |
|---|---|
| **Title** | Instant Language Switch |
| **Description** | User selects language in settings → instant switch (no restart). |
| **Priority** | Critical |
| **User Roles** | Parent, Teacher, School Admin |
| **Acceptance notes** | `LocaleManager.setLocale()` updates `StateFlow` → triggers recomposition. No app restart. |

### FR-003
| Field | Value |
|---|---|
| **Title** | Server-Side Content Translation |
| **Description** | Server-side content translation: announcements, notifications translated to user's languagePref via AI. |
| **Priority** | High |
| **User Roles** | System |
| **Acceptance notes** | `ContentTranslationService` uses AI to translate content. Cached per language. Fallback to English on failure. |

### FR-004
| Field | Value |
|---|---|
| **Title** | Locale-Aware Date Formatting |
| **Description** | Locale-aware date formatting (e.g., "१५ जुलाई" in Hindi). |
| **Priority** | Medium |
| **User Roles** | System |
| **Acceptance notes** | `DateFormatter` expect/actual class per platform. Uses ICU/locale-aware formatting. |

### FR-005
| Field | Value |
|---|---|
| **Title** | Locale-Aware Number/Currency Formatting |
| **Description** | Locale-aware number/currency formatting (₹1,00,000 in Indian format). |
| **Priority** | Medium |
| **User Roles** | System |
| **Acceptance notes** | `CurrencyFormatter` expect/actual class. Indian numbering system (lakhs, crores). |

### FR-006
| Field | Value |
|---|---|
| **Title** | Language Preference Sync |
| **Description** | Language preference synced to server (`app_users.languagePref`). |
| **Priority** | High |
| **User Roles** | Parent, Teacher, School Admin |
| **Acceptance notes** | `PATCH /api/v1/user/language-pref` updates `app_users.languagePref`. Persisted locally in DataStore. |

### FR-007
| Field | Value |
|---|---|
| **Title** | WhatsApp in Preferred Language |
| **Description** | WhatsApp notifications sent in user's preferred language. |
| **Priority** | High |
| **User Roles** | System |
| **Acceptance notes** | Uses pre-approved Meta templates per language. Template selected based on `languagePref`. |

### FR-008
| Field | Value |
|---|---|
| **Title** | English Fallback |
| **Description** | Fallback to English if translation unavailable. |
| **Priority** | Critical |
| **User Roles** | System |
| **Acceptance notes** | If AI translation fails or string resource missing, fall back to English. Never show blank or error. |

### Non-Functional Requirements

| ID | Requirement |
|---|---|
| NFR-1 | AI content translation completes in < 3 seconds |
| NFR-2 | Language switch triggers recomposition in < 500ms |
| NFR-3 | String resource files < 500KB per language |
| NFR-4 | Translation cache hit rate > 80% for repeated content |
| NFR-5 | All 10 languages render correctly on Android, iOS, JVM, web |
| NFR-6 | No app restart required for language switch |

---

## 4. User Stories

### Parent
- [ ] Select my preferred language from 10 options in settings
- [ ] See the entire app UI in my selected language instantly
- [ ] Receive announcements translated to my language
- [ ] Receive notifications (push + WhatsApp) in my language
- [ ] See dates formatted in my locale (e.g., "१५ जुलाई" in Hindi)
- [ ] See currency in Indian format (₹1,00,000)
- [ ] Switch back to English anytime

### Teacher
- [ ] Select my preferred language from 10 options
- [ ] See the entire app UI in my selected language
- [ ] Create announcements in my language (translated to other languages for recipients)

### School Admin
- [ ] Select my preferred language from 10 options
- [ ] See the entire app UI in my selected language
- [ ] View school's medium of instruction

### System
- [ ] Translate announcements to all recipient languages on publish
- [ ] Translate notifications to recipient's language before send
- [ ] Cache translations to avoid repeated AI calls
- [ ] Fall back to English if translation unavailable
- [ ] Sync language preference to server

---

## 5. Business Rules

### BR-001
**Rule:** Default language is English.
**Enforcement:** `app_users.languagePref` defaults to 'en'. `LocaleManager` initializes to 'en' if no preference.

### BR-002
**Rule:** Language switch is instant — no app restart.
**Enforcement:** `LocaleManager.setLocale()` updates `StateFlow` → `CompositionLocal` → recomposition.

### BR-003
**Rule:** AI translation does not translate proper nouns.
**Enforcement:** AI prompt includes: "Do not translate proper nouns (school names, person names)."

### BR-004
**Rule:** Fallback to English on translation failure.
**Enforcement:** If AI translation fails, times out, or returns empty → use original English content. Log error.

### BR-005
**Rule:** Translations are cached per content + language.
**Enforcement:** Translation cache keyed by `(content_hash, target_lang)`. Cache TTL: 24 hours. Avoids repeated AI calls for same content.

### BR-006
**Rule:** WhatsApp templates are pre-approved per language.
**Enforcement:** Meta requires pre-approved templates. Each template has language-specific versions. Template selected based on `languagePref`.

### BR-007
**Rule:** Locale-aware formatting uses Indian numbering system.
**Enforcement:** `CurrencyFormatter` uses Indian numbering (lakhs, crores) for all languages. ₹1,00,000 not $100,000.

### BR-008
**Rule:** `languagePref` synced to server on change.
**Enforcement:** `LocaleManager.setLocale()` calls `PATCH /api/v1/user/language-pref` in background. Offline changes queued for sync.

---

## 6. Database Design

### 6.1 Entity Relationship Summary

No new tables. Uses existing `app_users.languagePref` field. Optional: translation cache table for server-side caching.

### 6.2 New Tables

#### `translation_cache` table (optional, for server-side caching)

```sql
CREATE TABLE translation_cache (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content_hash    VARCHAR(64) NOT NULL,          -- SHA-256 hash of original content
    target_lang     VARCHAR(8) NOT NULL,            -- "hi", "bn", etc.
    translated_text TEXT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    expires_at      TIMESTAMP NOT NULL,
    UNIQUE(content_hash, target_lang)
);
```

### 6.3 Modified Tables

N/A — `app_users.languagePref` already exists. No schema changes needed.

### 6.4 Indexes

- `translation_cache(content_hash, target_lang)` — UNIQUE, for cache lookups
- `translation_cache(expires_at)` — for cache cleanup

### 6.5 Constraints

- `translation_cache.content_hash` — NOT NULL, VARCHAR(64)
- `translation_cache.target_lang` — NOT NULL, VARCHAR(8)
- `translation_cache.translated_text` — NOT NULL
- `translation_cache.expires_at` — NOT NULL
- `UNIQUE(content_hash, target_lang)` — one translation per content+language

### 6.6 Foreign Keys

N/A — no FK relationships for translation cache.

### 6.7 Soft Delete Strategy

N/A — translation cache entries expire and are cleaned up. No soft delete.

### 6.8 Audit Fields

- `translation_cache.created_at` — when translation was cached
- `translation_cache.expires_at` — when cache entry expires (24 hours)

### 6.9 Migration Notes

Migration: `docs/db/migration_071_translation_cache.sql`
- CREATE `translation_cache` table (optional, for server-side caching)
- No data migration needed (new feature)
- `app_users.languagePref` already exists — no ALTER needed

### 6.10 Exposed Mappings

```kotlin
object TranslationCacheTable : UUIDTable("translation_cache", "id") {
    val contentHash    = varchar("content_hash", 64)
    val targetLang     = varchar("target_lang", 8)
    val translatedText = text("translated_text")
    val createdAt      = timestamp("created_at")
    val expiresAt      = timestamp("expires_at")
}
```

Register in `DatabaseFactory.allTables` (optional).

### 6.11 Seed Data

N/A — translations generated on demand by AI service.

---

## 7. State Machines

### Language Preference State Machine

```
en (default) ──user_selects──> selected_lang ──synced──> synced
  │                                │
  │──user_changes──>               │
  │  selected_lang                 │
  │                                │
  └──offline_change──>             │
     pending_sync                   │
     ──online──> synced             │
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `en` (default) | User selects language | `selected_lang` | Valid language code (en/hi/bn/ta/te/mr/gu/kn/ml/pa) |
| `selected_lang` | Sync to server | `synced` | Online, PATCH succeeds |
| `selected_lang` | Offline | `pending_sync` | No network |
| `pending_sync` | Back online | `synced` | PATCH succeeds |
| `synced` | User changes language | `selected_lang` | New language code |

### Content Translation State Machine

```
original_content ──ai_translates──> translated ──cached──> cached
  │                                    │
  │──already_cached──> cached          │──ai_fails──> fallback_english
  │                                    │──ai_timeout──> fallback_english
  └──target_is_english──> no_translation
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `original_content` | Target lang is English | `no_translation` | `targetLang == 'en'` |
| `original_content` | Check cache | `cached` | Cache hit (content_hash + target_lang) |
| `original_content` | AI translates | `translated` | AI service available |
| `translated` | Cache result | `cached` | Store in translation_cache |
| `original_content` | AI fails | `fallback_english` | AI error or empty response |
| `original_content` | AI timeout (> 3s) | `fallback_english` | Timeout |
| `fallback_english` | None | (terminal) | Use original English content |

---

## 8. Backend Architecture

### 8.1 Component Overview

`ContentTranslationService` handles AI-powered content translation. `Notify.kt` is modified to translate notifications before sending. A translation cache table avoids repeated AI calls. A new API endpoint syncs language preference.

### 8.2 Design Principles

1. **English is default and fallback** — always works, even if AI unavailable
2. **Cache aggressively** — translations cached per content+language to reduce AI calls
3. **Translate on server, not client** — client only handles UI string resources
4. **No proper noun translation** — AI prompt explicitly excludes names
5. **Offline-first language pref** — preference stored locally, synced when online

### 8.3 Core Types

#### ContentTranslationService

```kotlin
class ContentTranslationService(private val aiService: AiService) {
    suspend fun translate(content: String, targetLang: String): String {
        if (targetLang == "en") return content
        // Check cache
        val cached = translationCacheRepository.get(content.hash(), targetLang)
        if (cached != null) return cached
        // AI translate
        val translated = aiService.complete(null, null, "translation", "translate_v1",
            mapOf("content" to content, "target_language" to targetLang))
        // Cache result
        translationCacheRepository.put(content.hash(), targetLang, translated, ttlHours = 24)
        return translated
    }

    suspend fun translateNotification(notification: Notification, userLang: String): Notification {
        if (userLang == "en") return notification
        notification.title = translate(notification.title, userLang)
        notification.body = translate(notification.body, userLang)
        return notification
    }
}
```

### 8.4 Repositories

- `TranslationCacheRepository` — CRUD for `translation_cache` table

### 8.5 Mappers

N/A — translation cache stores and retrieves text directly.

### 8.6 Permission Checks

- Language preference: user can only update own preference
- Translation: system-internal, no user-facing permission

### 8.7 Background Jobs

- **Translation Cache Cleanup** — hourly
  1. Query `translation_cache` where `expires_at < now()`
  2. Delete expired entries
  3. Return count deleted

### 8.8 Domain Events

- `LanguagePreferenceChanged` — emitted when user changes language preference
- `ContentTranslated` — emitted when content translated (logged)
- `TranslationFailed` — emitted when AI translation fails (logged, fallback to English)
- `TranslationCacheHit` — emitted when cache hit (logged for metrics)

### 8.9 Caching

- Translation cache: `translation_cache` table, 24-hour TTL, keyed by `(content_hash, target_lang)`
- User language preference: cached per user, 10-minute TTL
- String resources: loaded once per language, cached in memory

### 8.10 Transactions

- Language preference update: single transaction (update `app_users.languagePref`)
- Translation cache store: single transaction (insert/update cache entry)
- Notification translation: no transaction (read from cache or AI, update notification in memory)

### 8.11 Rate Limiting

- AI translation: rate-limited per school per hour (e.g., 1000 translations/hour)
- Language preference update: standard API rate limiting
- Cache cleanup: no rate limiting (internal job)

### 8.12 Configuration

- `MULTI_LANGUAGE_ENABLED` — default `true`; enable/disable feature
- `TRANSLATION_CACHE_TTL_HOURS` — default `24`; cache entry TTL
- `TRANSLATION_AI_TIMEOUT_MS` — default `3000`; AI translation timeout
- `TRANSLATION_CACHE_CLEANUP_CRON` — default `0 0 * * * *` (hourly)
- `SUPPORTED_LANGUAGES` — default `en,hi,bn,ta,te,mr,gu,kn,ml,pa`

---

## 9. API Contracts

### 9.1 Language Preference

```
PATCH /api/v1/user/language-pref
  Body: { "language": "hi" }
  → 200: { message: "Language preference updated" }
  → 400: Invalid language code
```

No other new endpoints — translation happens server-side transparently.

### 9.2 DTO Models

All `@Serializable`, wrapped in `ApiResponse<T>` pattern.

```kotlin
@Serializable data class LanguagePrefRequest(
    val language: String,  // en | hi | bn | ta | te | mr | gu | kn | ml | pa
)

@Serializable data class LanguagePrefResponse(
    val language: String,
)
```

---

## 10. Frontend Architecture

### 10.1 Screens

| Screen | Platform | Role | Description |
|---|---|---|---|
| `SettingsScreen` (modified) | Compose | All | Add language switcher with 10 language options |

### 10.2 Navigation

- Profile tab → Settings → Language → Language selection dialog

### 10.3 UX Flows

#### User: Change Language
1. User opens Settings
2. Taps "Language"
3. Sees list of 10 languages with native names (e.g., "हिन्दी", "বাংলা", "தமிழ்")
4. Selects language
5. UI instantly switches to selected language
6. Preference synced to server in background

### 10.4 State Management

```kotlin
class LocaleManager {
    val currentLocale: StateFlow<String>  // "en", "hi", etc.

    fun setLocale(lang: String) {
        // 1. Update StateFlow (triggers recomposition)
        // 2. Persist to DataStore
        // 3. Sync to server: PATCH /api/v1/user/language-pref
    }
}

// Compose provider
@Composable
fun AppRoot() {
    val locale by localeManager.currentLocale.collectAsState()
    CompositionLocalProvider(LocalLocale provides locale) {
        VidyaPrayagTheme { AppContent() }
    }
}
```

### 10.5 String Resources

```kotlin
// shared/src/commonMain/resources/strings/
// strings_en.xml, strings_hi.xml, strings_bn.xml, etc.

@Composable
fun stringResource(key: String): String {
    val locale = LocalLocale.current
    return StringResources.get(key, locale)
}
```

### 10.6 Locale-Aware Formatting

```kotlin
expect class DateFormatter(locale: String) {
    fun format(date: LocalDate): String  // "15 July" / "१५ जुलाई"
    fun formatLong(date: LocalDate): String  // "Monday, 15 July 2026"
}

expect class CurrencyFormatter(locale: String) {
    fun format(amount: Double): String  // "₹1,00,000" (Indian numbering)
}
```

### 10.7 Offline Support

- Language preference cached locally in DataStore
- Offline language change queued for sync
- String resources bundled in app (no network needed for UI)

### 10.8 Loading States

- Language switch: instant (no loading state)
- Translation: transparent (user sees English until translation arrives)

### 10.9 Error Handling (UI)

- Language sync failure: "Language preference saved locally. Will sync when online."
- Translation failure: Show English content (silent fallback)

### 10.10 Component Integration Guidelines

| Rule | Description |
|---|---|
| **R1** | All UI strings use `stringResource(key)` — no hardcoded strings |
| **R2** | Language switcher shows native language names (हिन्दी, not Hindi) |
| **R3** | Locale-aware date formatting via `DateFormatter` |
| **R4** | Locale-aware currency formatting via `CurrencyFormatter` (Indian numbering) |
| **R5** | `CompositionLocalProvider(LocalLocale)` wraps entire app |
| **R6** | Language preference persisted in DataStore before server sync |
| **R7** | Fallback to English if string resource missing for selected language |
| **R8** | No app restart required for language switch |
| **R9** | RTL support ready (but not enabled for initial 10 LTR languages) |
| **R10** | All screens use `stringResource()` instead of hardcoded strings |

---

## 11. Shared Module Changes (KMP)

### 11.1 DTOs

All DTOs defined in section 9.2, placed in `shared/.../i18n/domain/model/LanguageModels.kt`.

### 11.2 Domain Models

```kotlin
data class LanguagePreference(
    val code: String,       // en, hi, bn, etc.
    val nativeName: String, // हिन्दी, বাংলা, etc.
    val englishName: String, // Hindi, Bengali, etc.
    val script: String,     // Devanagari, Bengali, etc.
)

val SUPPORTED_LANGUAGES = listOf(
    LanguagePreference("en", "English", "English", "Latin"),
    LanguagePreference("hi", "हिन्दी", "Hindi", "Devanagari"),
    LanguagePreference("bn", "বাংলা", "Bengali", "Bengali"),
    LanguagePreference("ta", "தமிழ்", "Tamil", "Tamil"),
    LanguagePreference("te", "తెలుగు", "Telugu", "Telugu"),
    LanguagePreference("mr", "मराठी", "Marathi", "Devanagari"),
    LanguagePreference("gu", "ગુજરાતી", "Gujarati", "Gujarati"),
    LanguagePreference("kn", "ಕನ್ನಡ", "Kannada", "Kannada"),
    LanguagePreference("ml", "മലയാളം", "Malayalam", "Malayalam"),
    LanguagePreference("pa", "ਪੰਜਾਬੀ", "Punjabi", "Gurmukhi"),
)
```

### 11.3 Repository Interfaces

```kotlin
interface LanguageRepository {
    suspend fun getLanguagePref(token: String): NetworkResult<String>
    suspend fun updateLanguagePref(token: String, language: String): NetworkResult<Unit>
}
```

### 11.4 UseCases

- `GetLanguagePrefUseCase`
- `UpdateLanguagePrefUseCase`
- `TranslateContentUseCase` (client-side, for UI labels if needed)

### 11.5 Validation

- Language code: one of en, hi, bn, ta, te, mr, gu, kn, ml, pa
- Content for translation: non-empty string, max 5000 characters

### 11.6 Serialization

Standard Kotlinx serialization. Language codes serialized as lowercase strings.

### 11.7 Network APIs

Ktor `@Resource` route definitions added to `LanguageApi.kt`:
- PATCH `/api/v1/user/language-pref`

### 11.8 Database Models (Local Cache)

- Language preference stored in DataStore (key-value)
- String resources bundled in app (no local DB)

---

## 12. Permissions Matrix

| Action | Super Admin | School Admin | Teacher | Parent |
|---|---|---|---|---|
| View own language preference | ✅ | ✅ | ✅ | ✅ |
| Update own language preference | ✅ | ✅ | ✅ | ✅ |
| View school medium of instruction | ✅ | ✅ | ✅ | ✅ |
| View supported languages list | ✅ | ✅ | ✅ | ✅ |
| View translation cache stats | ✅ | ❌ | ❌ | ❌ |

---

## 13. Notifications

### Notification Translation Integration

In `Notify.kt`, before sending notification:
1. Fetch recipient's `languagePref`
2. If not English, translate title + body via `ContentTranslationService`
3. Send translated notification

### Notification Channels by Language

| Channel | Translation | Fallback |
|---|---|---|
| FCM Push | Translated title + body | English if translation fails |
| WhatsApp | Pre-approved template in user's language | English template if not available |
| In-App | Translated title + body | English if translation fails |

### WhatsApp Multi-Language Templates

From `WHATSAPP_INTEGRATION_SPEC.md`:
- Each WhatsApp template has language-specific versions
- Template selected based on `languagePref`
- Meta requires pre-approval for each language version
- If template not available in user's language, English template used

---

## 14. Background Jobs

### Translation Cache Cleanup Job

| Property | Value |
|---|---|
| **Name** | `TranslationCacheCleanupJob` |
| **Schedule** | Hourly |
| **Duration** | < 1 minute |
| **Retry** | None (next cycle will clean up) |

#### Job Flow

1. Query `translation_cache` where `expires_at < now()`
2. Delete expired entries
3. Return count deleted
4. Log summary

### Batch Translation Job (Optional)

| Property | Value |
|---|---|
| **Name** | `BatchTranslationJob` |
| **Schedule** | On announcement publish |
| **Duration** | < 30 seconds per language |
| **Retry** | 3 attempts with 5-second intervals |

#### Job Flow

1. Announcement published in English
2. For each target language (hi, bn, ta, te, mr, gu, kn, ml, pa):
   a. Translate announcement title + body
   b. Store in translation cache
3. When users fetch announcement, translated version served from cache

---

## 15. Integrations

### Internal Integrations

| System | Integration Point | Direction | Protocol | Error Handling |
|---|---|---|---|---|
| `AppUsersTable.languagePref` | User language preference | Read | Direct DB | Default 'en' if not set |
| `Notify.kt` | Notification translation | Call | Direct call | Fallback to English |
| `NotificationService.kt` | FCM push with translated content | Call | Direct call | Existing error handling |
| WhatsApp gateway | Language-specific templates | Outbound | HTTP API | English fallback |
| `AiService` | Content translation | Call | HTTP API | Fallback to English on failure |
| `TranslationCacheTable` | Translation caching | Read/Write | Direct DB | Bypass cache on error |

### External Integrations

| System | Purpose | Direction | Protocol | Authentication | Error Handling |
|---|---|---|---|---|---|
| AI Service (LLM) | Content translation | Outbound | HTTP API | Bearer token | Fallback to English |
| WhatsApp Gateway | Language-specific templates | Outbound | HTTP API | Bearer token (existing) | English template fallback |

### Integration Patterns

- **AI translation:** `AiService.complete(null, null, "translation", "translate_v1", params)` → returns translated text. Fallback to English on any error.
- **WhatsApp templates:** Template ID selected based on `languagePref`. Pre-approved by Meta per language.
- **Notification translation:** `ContentTranslationService.translateNotification()` called in `Notify.kt` before dispatch.

---

## 16. Security

### Authentication

- Language preference API: JWT auth via `requireAuth()`
- Translation: system-internal, no user auth

### Authorization

- User can only update own language preference
- No cross-user preference access

### Data Protection

- Language preference — non-sensitive user configuration
- Translated content — same sensitivity as original content
- AI translation — content sent to LLM API, same PII as original notification/announcement

### Input Validation

- Language code: one of en, hi, bn, ta, te, mr, gu, kn, ml, pa
- Content for translation: non-empty, max 5000 characters

### Rate Limiting

- AI translation: 1000 per school per hour
- Language preference update: standard API rate limiting
- Translation cache: no rate limiting (internal)

### Audit Logging

- Language preference change: user ID, old language, new language
- Translation success: content hash, target language, cache hit/miss
- Translation failure: content hash, target language, error

### PII Handling

- Notification/announcement content sent to AI for translation — includes student names, grades, fee amounts
- AI service should be configured to not store/retain content
- Language preference — non-sensitive

### Multi-tenant Isolation

- `translation_cache` — not school-scoped (content hash is language-agnostic)
- `app_users.languagePref` — user-scoped
- No cross-school concerns for translation

---

## 17. Performance & Scalability

### Expected Scale

- 10 languages × 1000 strings = 10,000 string resources total
- ~500 translations per day per school (announcements + notifications)
- Translation cache: ~500 entries per day per school, 24-hour TTL

### Query Optimization

- Translation cache: `UNIQUE(content_hash, target_lang)` — O(1) lookup
- User language pref: `app_users.languagePref` — O(1) lookup
- String resources: in-memory map, O(1) lookup

### Indexing Strategy

- `translation_cache(content_hash, target_lang)` — UNIQUE index for cache lookups
- `translation_cache(expires_at)` — for cleanup job

### Caching Strategy

- Translation cache: 24-hour TTL, keyed by `(content_hash, target_lang)`
- User language preference: cached per user, 10-minute TTL
- String resources: loaded once per language, cached in memory for app lifetime

### Pagination

N/A — translation operates on individual content items.

### Connection Pooling

Uses existing HikariCP connection pool. No additional pooling needed.

### Async Processing

- AI translation: async (non-blocking, fallback to English on timeout)
- Notification translation: async (fire-and-forget with fallback)
- Language preference sync: async (background, offline queue)

### Scalability Concerns

- AI translation volume: 500 translations/day/school × 100 schools = 50,000/day. Cache hit rate > 80% reduces AI calls to ~10,000/day. Feasible.
- String resource size: 10 languages × ~50KB = 500KB total. Acceptable for mobile app.
- Translation cache growth: 500 entries/day/school × 100 schools = 50,000 entries/day. 24-hour TTL keeps table small.

---

## 18. Edge Cases

| # | Scenario | Expected Behavior |
|---|---|---|
| EC-1 | User selects language not in supported list | Return 400 "Invalid language code." |
| EC-2 | AI translation service unavailable | Fallback to English. Log error. |
| EC-3 | AI translation timeout (> 3 seconds) | Fallback to English. Log timeout. |
| EC-4 | AI returns empty translation | Fallback to English. Log error. |
| EC-5 | String resource missing for selected language | Fallback to English string. Log missing key. |
| EC-6 | User has no `languagePref` set | Default to 'en'. |
| EC-7 | Translation cache miss | Call AI, cache result, return translation. |
| EC-8 | Translation cache expired | Call AI, update cache, return translation. |
| EC-9 | WhatsApp template not available in user's language | Use English template. Log missing template. |
| EC-10 | User changes language while offline | Save locally, sync when online. |
| EC-11 | Content contains proper nouns (school name, person name) | AI prompt excludes proper nouns from translation. |
| EC-12 | Content is already in target language | AI detects and returns as-is. No error. |
| EC-13 | Content is mixed language (English + Hindi) | AI translates English parts to target language. |
| EC-14 | User switches language rapidly | Each switch triggers recomposition. Last selection wins. Server synced with debounce. |
| EC-15 | Translation cache table full | Evict oldest entries. Log eviction. |
| EC-16 | App launched for first time (no language pref) | Default to English. Show language selection on first launch (optional). |
| EC-17 | Content exceeds 5000 characters | Truncate for translation. Log truncation. |
| EC-18 | Multiple users with same content + language | Cache hit — same translation served. |

---

## 19. Error Handling

### Error Response Format

Standard `ApiResponse` error format for language preference API. Internal errors logged.

### Error Codes

| Code | HTTP Status | Description | User Message |
|---|---|---|---|
| `INVALID_LANGUAGE_CODE` | 400 | Language code not in supported list | "Please select a valid language." |
| `TRANSLATION_FAILED` | 500 | AI translation error | (Internal, logged, not shown to user — English fallback) |

### Error Handling Strategy

- **AI translation errors:** Fallback to English. Log error. Never block notification delivery.
- **WhatsApp template missing:** Use English template. Log missing template.
- **String resource missing:** Fallback to English string. Log missing key.
- **Language pref sync failure:** Save locally, sync when online.
- **Cache errors:** Bypass cache, call AI directly. Log error.

### Retry Strategy

- AI translation: no retry (fallback to English)
- Language pref sync: retried when online (offline queue)
- WhatsApp: existing retry logic (3 retries)

### Fallback Behavior

- AI unavailable: English content
- WhatsApp template missing: English template
- String resource missing: English string
- Cache unavailable: call AI directly (no cache)
- Language pref sync failure: local preference used, synced later

---

## 20. Analytics & Reporting

### Analytics Dashboard Data

| Metric | Source | Derivation |
|---|---|---|
| Language distribution | `app_users.languagePref` | Group by language, count |
| Translation volume | `translation_cache` count | Direct count |
| Translation cache hit rate | Cache hits / total requests | Percentage |
| AI translation calls | `ContentTranslationService` logs | Count per day |
| AI translation failures | `ContentTranslationService` logs | Count, error types |
| WhatsApp template usage | WhatsApp gateway logs | Count per language |
| Language switch rate | `LanguagePreferenceChanged` events | Count per day |

### Export Capabilities

- Language distribution export (CSV) — user ID, language, last changed

### Report Types

| Report | Format | Frequency | Recipient |
|---|---|---|---|
| Language adoption | JSON (API) | On-demand | School Admin |
| Translation metrics | JSON (API) | On-demand | Dev Team |
| AI translation audit | JSON (API) | Weekly | Dev Team |

---

## 21. Testing Strategy

### Unit Tests

- `ContentTranslationService.translate()` — success, cache hit, AI failure fallback, timeout fallback
- `LocaleManager.setLocale()` — updates StateFlow, persists to DataStore, syncs to server
- `DateFormatter` — locale-aware date formatting for all 10 languages
- `CurrencyFormatter` — Indian numbering system (₹1,00,000)
- Language code validation — valid codes accepted, invalid rejected
- Translation cache — store, retrieve, expire, cleanup

### Integration Tests

- Full notification flow: create notification → fetch user lang → translate → send
- Language preference sync: change locally → sync to server → verify `app_users.languagePref` updated
- WhatsApp template selection: user with `languagePref = 'hi'` → Hindi template selected
- Cache lifecycle: translate content → cache → retrieve from cache → expire → re-translate

### E2E Tests

- User changes language in settings → UI switches instantly → all screens show translated strings
- Announcement published in English → user with `languagePref = 'hi'` sees Hindi translation

### Performance Tests

- AI translation: < 3 seconds per content
- Language switch recomposition: < 500ms
- String resource loading: < 100ms per language
- Translation cache lookup: < 10ms

### Test Data

- 10 sample strings in all 10 languages
- 5 sample notifications with mixed content
- Mock AI service (returns predefined translations)
- Mock WhatsApp gateway

### Test Environment

- Test database with `translation_cache` table
- Mock AI service for translation
- Mock WhatsApp gateway
- Test JWT tokens for parent, teacher, admin roles
- All 10 string resource files for testing

---

## 22. Acceptance Criteria

- [ ] App UI available in 10 languages (English, Hindi, Bengali, Tamil, Telugu, Marathi, Gujarati, Kannada, Malayalam, Punjabi)
- [ ] Language switch in settings works instantly (no app restart)
- [ ] Server-side content translation for announcements + notifications
- [ ] Locale-aware date formatting (e.g., "१५ जुलाई" in Hindi)
- [ ] Locale-aware number/currency formatting (₹1,00,000 in Indian format)
- [ ] WhatsApp notifications sent in user's preferred language
- [ ] Fallback to English if translation unavailable
- [ ] Language preference synced to server (`app_users.languagePref`)
- [ ] All UI strings externalized to string resources
- [ ] Language switcher shows native language names (हिन्दी, not Hindi)
- [ ] Translation cache reduces AI calls (> 80% hit rate)
- [ ] AI translation does not translate proper nouns
- [ ] Offline language change queued for sync

---

## 23. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 3 days | Externalize all UI strings to resource files (`strings_en.xml`) |
| 2 | 5 days | Translate string resources to 9 languages (AI-assisted + human review) |
| 3 | 2 days | `LocaleManager` + `CompositionLocal` setup |
| 4 | 2 days | Locale-aware formatters (`DateFormatter`, `CurrencyFormatter`) — expect/actual |
| 5 | 2 days | `ContentTranslationService` + AI translation prompt |
| 6 | 1 day | `Notify.kt` integration (translate before send) |
| 7 | 2 days | Client UI (language switcher in settings, native language names) |
| 8 | 2 days | Tests: unit, integration, E2E |

### Pre-Implementation Checklist

- [ ] Verify `app_users.languagePref` field exists and is accessible
- [ ] Verify `AiService` API for translation
- [ ] Verify WhatsApp templates have language-specific versions
- [ ] Verify Compose Multiplatform supports string resources
- [ ] Verify ICU/locale-aware formatting available on all platforms (Android, iOS, JVM, web)

---

## 24. File-Level Impact Analysis

### Server

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | `TranslationCacheTable` (optional) |
| `server/.../db/DatabaseFactory.kt` | Modify | Register `TranslationCacheTable` (optional) |
| `server/.../feature/i18n/ContentTranslationService.kt` | **New** | AI content translation service |
| `server/.../feature/i18n/TranslationCacheRepository.kt` | **New** | Translation cache CRUD |
| `server/.../feature/notifications/Notify.kt` | Modify | Translate notifications before send |
| `server/.../feature/user/UserRouting.kt` | Modify | Add `PATCH /api/v1/user/language-pref` |
| `docs/db/migration_071_translation_cache.sql` | **New** | DDL: `translation_cache` table (optional) |

### Shared (KMP)

| File | Change Type | Description |
|---|---|---|
| `shared/src/commonMain/resources/strings/strings_en.xml` | **New** | English string resources |
| `shared/src/commonMain/resources/strings/strings_hi.xml` | **New** | Hindi string resources |
| `shared/src/commonMain/resources/strings/strings_bn.xml` | **New** | Bengali string resources |
| `shared/src/commonMain/resources/strings/strings_ta.xml` | **New** | Tamil string resources |
| `shared/src/commonMain/resources/strings/strings_te.xml` | **New** | Telugu string resources |
| `shared/src/commonMain/resources/strings/strings_mr.xml` | **New** | Marathi string resources |
| `shared/src/commonMain/resources/strings/strings_gu.xml` | **New** | Gujarati string resources |
| `shared/src/commonMain/resources/strings/strings_kn.xml` | **New** | Kannada string resources |
| `shared/src/commonMain/resources/strings/strings_ml.xml` | **New** | Malayalam string resources |
| `shared/src/commonMain/resources/strings/strings_pa.xml` | **New** | Punjabi string resources |
| `shared/.../core/locale/LocaleManager.kt` | **New** | Locale state management |
| `shared/.../core/locale/DateFormatter.kt` | **New** + expect/actual | Locale-aware date formatting |
| `shared/.../core/locale/CurrencyFormatter.kt` | **New** + expect/actual | Currency formatting (Indian numbering) |
| `shared/.../i18n/domain/model/LanguageModels.kt` | **New** | DTOs, domain models, `SUPPORTED_LANGUAGES` list |
| `shared/.../i18n/domain/repository/LanguageRepository.kt` | **New** | Repository interface |
| `shared/.../i18n/data/remote/LanguageApi.kt` | **New** | HTTP API definitions |

### Client (Compose)

| File | Change Type | Description |
|---|---|---|
| `composeApp/.../ui/v2/screens/settings/SettingsScreen.kt` | Modify | Add language switcher with 10 language options |
| `composeApp/.../ui/v2/screens/**/*.kt` | Modify | Replace all hardcoded strings with `stringResource(key)` |

---

## 25. Future Enhancements

| # | Enhancement | Priority | Effort | Notes |
|---|---|---|---|---|
| F-1 | Urdu/Arabic support with RTL | Medium | M | RTL layout, Urdu string resources |
| F-2 | Auto-detect language from phone settings | Low | S | Read system locale on first launch |
| F-3 | Translation of user-generated content | Medium | L | Translate posts, messages |
| F-4 | OCR translation of uploaded documents | Low | L | AI-powered OCR + translation |
| F-5 | Language-specific fonts | Low | M | Custom fonts for better rendering |
| F-6 | Voice-based language selection | Low | S | Voice command to change language |
| F-7 | Translation quality feedback | Medium | S | Users rate translation quality |
| F-8 | Bulk announcement translation | Low | S | Pre-translate announcements for all languages on publish |
| F-9 | Language-specific content moderation | Medium | M | AI moderation in user's language |
| F-10 | Dialect support | Low | L | Beyond standard languages to regional dialects |

---

## Appendix A: Sequence Diagrams

### A.1 User Changes Language

```
User (app)       LocaleManager       DataStore       Server
  │                  │                  │              │
  │  Select "हिन्दी"   │                  │              │
  │  ──────────────> │                  │              │
  │                  │──update StateFlow│              │
  │  ←──recomposition│                  │              │
  │                  │──persist "hi"──→ │              │
  │                  │                  │              │
  │                  │──PATCH /language-pref─────────→│
  │                  │←──200 OK───────────────────────│
  │                  │                  │              │
  │  UI now in Hindi │                  │              │
  │                  │                  │              │
```

### A.2 Notification Translation Flow

```
Notify.kt    ContentTranslationService    TranslationCache    AiService    NotificationService
  │                    │                       │                  │              │
  │  send notification │                       │                  │              │
  │  ────────────────> │                       │                  │              │
  │                    │──fetch user lang──────│                  │              │
  │                    │←──"hi"───────────────│                  │              │
  │                    │                       │                  │              │
  │                    │  [if lang == 'en']    │                  │              │
  │                    │──send as-is─────────────────────────────────────────────→│
  │                    │                       │                  │              │
  │                    │  [if lang != 'en']    │                  │              │
  │                    │──check cache─────────→│                  │              │
  │                    │←──cache miss──────────│                  │              │
  │                    │──translate(content)─────────────────────→│              │
  │                    │←──translated text────────────────────────│              │
  │                    │──store in cache──────→│                  │              │
  │                    │──send translated──────────────────────────────────────→│
  │                    │                       │                  │              │
```

---

## Appendix B: Domain Model / ER Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                          app_users                                     │
│  id (PK)                                                              │
│  phone, role, isPhoneVerified                                         │
│  languagePref (existing: VARCHAR, default 'en')                       │
│  ...                                                                  │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│                    translation_cache (new, optional)                   │
│  id (PK)                                                              │
│  content_hash (SHA-256 of original content)                           │
│  target_lang (en|hi|bn|ta|te|mr|gu|kn|ml|pa)                          │
│  translated_text                                                      │
│  created_at, expires_at (24h TTL)                                     │
│  UNIQUE(content_hash, target_lang)                                    │
└──────────────────────────────────────────────────────────────────────┘

String Resources (in-app, not in DB):
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│ strings_en.xml    │  │ strings_hi.xml    │  │ strings_pa.xml    │
│ (English)         │  │ (Hindi)           │  │ (Punjabi)         │
└──────────────────┘  └──────────────────┘  └──────────────────┘
        ...10 language files total...
```

---

## Appendix C: Event Flow

### Domain Events

| Event | Emitter | Consumers | Payload | Side Effects |
|---|---|---|---|---|
| `LanguagePreferenceChanged` | `LocaleManager.setLocale()` | Server sync | `userId, oldLang, newLang` | `app_users.languagePref` updated |
| `ContentTranslated` | `ContentTranslationService.translate()` | None (logged) | `contentHash, targetLang, cacheHit` | Translation cached |
| `TranslationFailed` | `ContentTranslationService.translate()` | None (logged) | `contentHash, targetLang, error` | English fallback |
| `TranslationCacheHit` | `ContentTranslationService.translate()` | None (logged) | `contentHash, targetLang` | Cache served |
| `TranslationCacheExpired` | `TranslationCacheCleanupJob` | None (logged) | `count` | Expired entries deleted |

### Event Delivery Guarantees

- Events emitted synchronously within service methods
- All events logged for audit
- No external consumers — events are internal audit trail

### AI Translation Prompt

```
System: Translate the following text to {{target_language}}. Maintain tone and context.
Do not translate proper nouns (school names, person names). Keep it natural and concise.

User: {{content}}
```

### Supported Languages

| Code | Language | Script |
|---|---|---|
| en | English | Latin |
| hi | Hindi | Devanagari |
| bn | Bengali | Bengali |
| ta | Tamil | Tamil |
| te | Telugu | Telugu |
| mr | Marathi | Devanagari |
| gu | Gujarati | Gujarati |
| kn | Kannada | Kannada |
| ml | Malayalam | Malayalam |
| pa | Punjabi | Gurmukhi |

### Translation Pipeline

| Content Type | Translation Method |
|---|---|
| UI strings | Pre-translated string resources (manual + AI-assisted) |
| Announcements | AI translation on publish (cache per language) |
| Notifications | AI translation before send (cache per user+content) |
| Report cards | AI translation (from `AI_REPORT_CARD_SPEC.md`) |
| WhatsApp templates | Pre-approved Meta templates per language (from `WHATSAPP_INTEGRATION_SPEC.md`) |

---

## Appendix D: Configuration

### Environment Variables

| Variable | Default | Description |
|---|---|---|
| `MULTI_LANGUAGE_ENABLED` | `true` | Enable/disable multi-language feature |
| `TRANSLATION_CACHE_TTL_HOURS` | `24` | Translation cache entry TTL in hours |
| `TRANSLATION_AI_TIMEOUT_MS` | `3000` | AI translation timeout in milliseconds |
| `TRANSLATION_CACHE_CLEANUP_CRON` | `0 0 * * * *` | Hourly cache cleanup |
| `SUPPORTED_LANGUAGES` | `en,hi,bn,ta,te,mr,gu,kn,ml,pa` | Comma-separated supported language codes |
| `TRANSLATION_RATE_LIMIT_PER_HOUR` | `1000` | Max AI translations per school per hour |

### Feature Flags

| Flag | Default | Description |
|---|---|---|
| `MULTI_LANGUAGE_ENABLED` | `true` | Enable/disable multi-language feature |
| `AI_TRANSLATION_ENABLED` | `true` | Enable/disable AI content translation |
| `TRANSLATION_CACHE_ENABLED` | `true` | Enable/disable translation cache |
| `WHATSAPP_MULTI_LANGUAGE_ENABLED` | `true` | Enable/disable WhatsApp multi-language templates |

### School-Level Settings

- `SchoolsTable.mediumOfInstruction` — existing field, informational only. Does not override user preference.

---

## Appendix E: Migration & Rollback

### Migration: `migration_071_translation_cache.sql`

```sql
-- Migration 071: Translation Cache (optional)
-- Creates translation_cache table for AI translation caching

BEGIN;

CREATE TABLE IF NOT EXISTS translation_cache (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content_hash    VARCHAR(64) NOT NULL,
    target_lang     VARCHAR(8) NOT NULL,
    translated_text TEXT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    expires_at      TIMESTAMP NOT NULL,
    UNIQUE(content_hash, target_lang)
);

CREATE INDEX IF NOT EXISTS idx_translation_cache_expires
    ON translation_cache (expires_at);

COMMIT;
```

### Rollback: `migration_071_rollback.sql`

```sql
BEGIN;
DROP TABLE IF EXISTS translation_cache;
COMMIT;
```

### Migration Validation

- Verify `translation_cache` table created with correct columns
- Verify `UNIQUE(content_hash, target_lang)` constraint created
- Verify `idx_translation_cache_expires` index created
- Run `SELECT count(*) FROM translation_cache` — should be 0 (new feature)
- Verify `app_users.languagePref` field exists (should already exist)

---

## Appendix F: Observability

### Structured Logging

| Log Level | Event | Context Fields |
|---|---|---|
| INFO | Language preference changed | `userId, oldLang, newLang` |
| INFO | Content translated | `contentHash, targetLang, cacheHit, durationMs` |
| INFO | Translation cache hit | `contentHash, targetLang` |
| INFO | Translation cache cleanup | `entriesDeleted, durationMs` |
| WARN | Translation fallback to English | `contentHash, targetLang, reason (timeout/error/empty)` |
| WARN | String resource missing | `key, lang, fallbackToEnglish` |
| WARN | WhatsApp template missing for language | `templateId, lang, fallbackToEnglish` |
| ERROR | AI translation service unavailable | `error, stackTrace` |
| ERROR | Language pref sync failed | `userId, error` |

### Metrics

| Metric | Type | Labels | Description |
|---|---|---|---|
| `language_preference_distribution` | Gauge | `language` | User count per language |
| `translations_total` | Counter | `school_id, target_lang, source (cache/ai)` | Total translations by source |
| `translation_cache_hit_rate` | Gauge | — | Cache hit percentage |
| `translation_ai_duration` | Histogram | — | AI translation latency |
| `translation_failures_total` | Counter | `reason (timeout/error/empty)` | Translation failures by reason |
| `language_switches_total` | Counter | `old_lang, new_lang` | Language switches per day |
| `whatsapp_templates_by_language` | Counter | `template_id, lang` | WhatsApp template usage by language |

### Health Checks

| Check | Endpoint | Description |
|---|---|---|
| Multi-language enabled | `/health/i18n` | Verify feature flag enabled and translation cache accessible |
| AI translation service | `/health/ai-translation` | Verify AI service reachable for translation |

### Alerts

| Alert | Condition | Severity | Notification |
|---|---|---|---|
| AI translation failure rate high | `translation_failures_total` > 10% | Warning | Email to dev team |
| Translation cache hit rate low | Cache hit rate < 50% | Warning | Email to dev team (possible cache issue) |
| AI translation latency | `translation_ai_duration` > 5 seconds | Warning | Email to dev team |
| Language pref sync failures | Sync failure rate > 5% | Warning | Email to dev team |

### Dashboards

| Dashboard | Panels | Audience |
|---|---|---|
| Language Adoption | User distribution by language, switch rate, most popular | Product Team |
| Translation Metrics | Volume, cache hit rate, AI latency, failure rate | Dev Team |
| WhatsApp by Language | Template usage by language, missing templates | Dev Team |
| AI Translation Audit | Sample translations, quality metrics | Dev Team |

### Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| AI translation quality poor | Medium | Medium | Human review of string resources. AI only for dynamic content. User feedback mechanism. |
| AI service unavailable | Medium | Low | Fallback to English. Non-blocking. |
| String resource translation errors | Low | Medium | Human review of all 10 language files before release. |
| Rendering issues with Indic scripts | Low | Medium | Test on all platforms (Android, iOS, JVM, web). Use system fonts. |
| WhatsApp template approval delays | Medium | Low | Start Meta approval process early. English fallback. |
| Translation cache stale | Low | Low | 24-hour TTL. Cache invalidated on content update. |
| App size increase from string resources | Low | Low | 10 languages × ~50KB = 500KB. Acceptable. |
