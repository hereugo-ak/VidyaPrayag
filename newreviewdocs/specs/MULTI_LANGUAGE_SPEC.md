# Multi-Language Support — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** None
> **Source:** `DIFFERENTIATING_FEATURES.md` §8.1

---

## 1. Feature Overview

Full multi-language support for the app UI and content: 10 Indian languages (English, Hindi, Bengali, Tamil, Telugu, Marathi, Gujarati, Kannada, Malayalam, Punjabi) with per-user language preference, server-side content translation, and locale-aware formatting.

### Goals

- App UI translated into 10 languages (Compose Multiplatform string resources)
- Per-user language preference (stored in `app_users.languagePref` — field already exists)
- Server-side content translation for announcements, notifications (AI-powered)
- Locale-aware date, number, and currency formatting
- RTL support (for Urdu/Arabic if added later)
- Language switcher in settings (no app restart required)

---

## 2. Current System Assessment

- `AppUsersTable` has `languagePref` field (VARCHAR, default 'en')
- `SchoolsTable` has `mediumOfInstruction` field
- WhatsApp templates support multi-language (from `WHATSAPP_INTEGRATION_SPEC.md`)
- No UI translations exist — all strings are hardcoded in English
- `DIFFERENTIATING_FEATURES.md` §8.1: Multi-Language, effort L, data readiness: "languagePref field exists"

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | All UI strings externalized to string resources for 10 languages |
| FR-2 | User selects language in settings → instant switch (no restart) |
| FR-3 | Server-side content translation: announcements, notifications translated to user's languagePref via AI |
| FR-4 | Locale-aware date formatting (e.g., "१५ जुलाई" in Hindi) |
| FR-5 | Locale-aware number/currency formatting (₹1,00,000 in Indian format) |
| FR-6 | Language preference synced to server (`app_users.languagePref`) |
| FR-7 | WhatsApp notifications sent in user's preferred language |
| FR-8 | Fallback to English if translation unavailable |

---

## 4. Supported Languages

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

---

## 5. Frontend Architecture

### 5.1 String Resources

```kotlin
// shared/src/commonMain/resources/strings/
// strings_en.xml, strings_hi.xml, strings_bn.xml, etc.

// Access via:
@Composable
fun stringResource(key: String): String {
    val locale = LocalLocale.current
    return StringResources.get(key, locale)
}
```

### 5.2 Locale Management

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

### 5.3 Locale-Aware Formatting

```kotlin
expect class DateFormatter(locale: String) {
    fun format(date: LocalDate): String  // "15 July" / "१५ जुलाई"
    fun formatLong(date: LocalDate): String  // "Monday, 15 July 2026"
}

expect class CurrencyFormatter(locale: String) {
    fun format(amount: Double): String  // "₹1,00,000" (Indian numbering)
}
```

---

## 6. Backend Architecture

### 6.1 Content Translation Service

```kotlin
class ContentTranslationService(private val aiService: AiService) {
    suspend fun translate(content: String, targetLang: String): String {
        if (targetLang == "en") return content
        return aiService.complete(null, null, "translation", "translate_v1",
            mapOf("content" to content, "target_language" to targetLang))
    }

    suspend fun translateNotification(notification: Notification, userLang: String): Notification {
        if (userLang == "en") return notification
        notification.title = translate(notification.title, userLang)
        notification.body = translate(notification.body, userLang)
        return notification
    }
}
```

### 6.2 Notification Integration

In `Notify.kt`, before sending notification:
1. Fetch recipient's `languagePref`
2. If not English, translate title + body via `ContentTranslationService`
3. Send translated notification

### 6.3 AI Translation Prompt

```
System: Translate the following text to {{target_language}}. Maintain tone and context.
Do not translate proper nouns (school names, person names). Keep it natural and concise.

User: {{content}}
```

---

## 7. API Contracts

```
PATCH /api/v1/user/language-pref  { "language": "hi" }
```

No other new endpoints — translation happens server-side transparently.

---

## 8. Translation Pipeline

| Content Type | Translation Method |
|---|---|
| UI strings | Pre-translated string resources (manual + AI-assisted) |
| Announcements | AI translation on publish (cache per language) |
| Notifications | AI translation before send (cache per user+content) |
| Report cards | AI translation (from `AI_REPORT_CARD_SPEC.md`) |
| WhatsApp templates | Pre-approved Meta templates per language (from `WHATSAPP_INTEGRATION_SPEC.md`) |

---

## 9. Acceptance Criteria

- [ ] App UI available in 10 languages
- [ ] Language switch in settings works instantly
- [ ] Server-side content translation for announcements + notifications
- [ ] Locale-aware date/number/currency formatting
- [ ] WhatsApp notifications sent in user's language
- [ ] Fallback to English if translation unavailable
- [ ] Language preference synced to server

---

## 10. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 3 days | Externalize all UI strings to resource files |
| 2 | 5 days | Translate string resources to 9 languages (AI-assisted + human review) |
| 3 | 2 days | LocaleManager + CompositionLocal setup |
| 4 | 2 days | Locale-aware formatters (date, number, currency) |
| 5 | 2 days | ContentTranslationService + AI prompt |
| 6 | 1 day | Notify.kt integration (translate before send) |
| 7 | 2 days | Client UI (language switcher, settings) |
| 8 | 2 days | Tests |

---

## 11. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `shared/src/commonMain/resources/strings/*.xml` | New | 10 language string resource files |
| `shared/.../core/locale/LocaleManager.kt` | New | Locale state management |
| `shared/.../core/locale/DateFormatter.kt` | New + expect/actual | Locale-aware formatting |
| `shared/.../core/locale/CurrencyFormatter.kt` | New + expect/actual | Currency formatting |
| `server/.../feature/i18n/ContentTranslationService.kt` | New | AI content translation |
| `server/.../feature/notifications/Notify.kt` | Modify | Translate before send |
| `composeApp/.../ui/v2/screens/settings/SettingsScreen.kt` | Modify | Language switcher |
| `composeApp/.../ui/v2/screens/**/*.kt` | Modify | Replace hardcoded strings with resources |
