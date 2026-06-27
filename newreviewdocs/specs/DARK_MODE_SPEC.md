# Dark Mode — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Audience:** Senior engineer / AI agent implementing the system
> **Last updated:** 2026-06-27
> **Prerequisites:** None

---

## Table of Contents

1. [Feature Overview](#1-feature-overview)
2. [Current System Assessment](#2-current-system-assessment)
3. [Gap Analysis](#3-gap-analysis)
4. [Functional Requirements](#4-functional-requirements)
5. [User Roles & Permissions](#5-user-roles--permissions)
6. [Database Design](#6-database-design)
7. [Frontend Architecture](#7-frontend-architecture)
8. [Configuration](#8-configuration)
9. [Testing Strategy](#9-testing-strategy)
10. [Acceptance Criteria](#10-acceptance-criteria)
11. [Implementation Roadmap](#11-implementation-roadmap)
12. [File-Level Impact Analysis](#12-file-level-impact-analysis)
13. [Risks & Mitigations](#13-risks--mitigations)

---

## 1. Feature Overview

System-aware dark theme with manual override (light/dark/system). Extends the existing V Design System with a dark color palette while maintaining contrast ratios and accessibility.

### Goals

- Dark color palette for all V Design System tokens
- Three-mode toggle: System (default) / Light / Dark
- Preference persisted per user
- Smooth transition between themes
- All screens, components, and illustrations support dark mode
- WCAG AA contrast ratios maintained

---

## 2. Current System Assessment

### 2.1 What Exists

- **V Design System** — `VTheme`, `VColors`, `VDimens`, `VType`, `VAtoms` in `composeApp/.../ui/v2/`
- `VColors` has a tone system but **no dark variant** (`feature_audit.csv` L147: "VColors has no dark variant")
- Compose Multiplatform supports `MaterialTheme(colorScheme)` with light/dark schemes
- `AppUsersTable` has `languagePref` but no `themePref` field
- DataStore preferences used for auth tokens — can store theme preference

### 2.2 What's Missing

- No dark color palette
- No theme preference storage
- No theme switching UI
- Hardcoded colors in some screens (not using VColors tokens)

---

## 3. Gap Analysis

| # | Gap | Impact |
|---|---|---|
| G1 | No dark color palette | No dark mode support |
| G2 | No theme preference | Users can't choose theme |
| G3 | Hardcoded colors in screens | Won't adapt to dark mode |
| G4 | No system setting detection | Can't follow OS dark mode |

---

## 4. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Define dark color palette for all VColors tokens |
| FR-2 | Three-mode toggle: System / Light / Dark (default: System) |
| FR-3 | Theme preference persisted in DataStore (client) + `app_users.theme_pref` (server) |
| FR-4 | System mode follows OS dark mode setting (isSystemInDarkTheme()) |
| FR-5 | All VAtoms components use dynamic color tokens (not hardcoded) |
| FR-6 | Smooth animation on theme switch |
| FR-7 | Status bar / navigation bar adapts to theme (Android) |
| FR-8 | WCAG AA contrast ratios (4.5:1 for text, 3:1 for large text) |

---

## 5. User Roles & Permissions

All roles can change their theme preference. No role-based restrictions.

---

## 6. Database Design

### 6.1 Modify Existing: `app_users`

```sql
ALTER TABLE app_users ADD COLUMN theme_pref VARCHAR(8) NOT NULL DEFAULT 'system';
-- Values: system | light | dark
```

### 6.2 Exposed Mapping

Add to `AppUsersTable`:
```kotlin
val themePref = varchar("theme_pref", 8).default("system")
```

### 6.3 Client-Side (DataStore)

```kotlin
// ThemePreferences.kt
val THEME_KEY = stringPreferencesKey("theme_pref")
// Values: "system" | "light" | "dark"
```

---

## 7. Frontend Architecture

### 7.1 Color Tokens

```kotlin
object VColors {
    // Light (existing, refined)
    val lightPrimary = Color(0xFF2563EB)
    val lightBackground = Color(0xFFF8FAFC)
    val lightSurface = Color(0xFFFFFFFF)
    val lightOnPrimary = Color(0xFFFFFFFF)
    val lightOnBackground = Color(0xFF1E293B)
    val lightOnSurface = Color(0xFF334155)
    val lightError = Color(0xFFDC2626)
    val lightOutline = Color(0xFFCBD5E1)
    // ... all tokens

    // Dark (new)
    val darkPrimary = Color(0xFF60A5FA)
    val darkBackground = Color(0xFF0F172A)
    val darkSurface = Color(0xFF1E293B)
    val darkOnPrimary = Color(0xFF0F172A)
    val darkOnBackground = Color(0xFFF1F5F9)
    val darkOnSurface = Color(0xFFCBD5E1)
    val darkError = Color(0xFFF87171)
    val darkOutline = Color(0xFF475569)
    // ... all tokens
}

object VTheme {
    @Composable
    fun VidyaPrayagTheme(
        themePref: String = "system",
        content: @Composable () -> Unit
    ) {
        val systemDark = isSystemInDarkTheme()
        val isDark = when (themePref) {
            "dark" -> true
            "light" -> false
            else -> systemDark
        }
        val colorScheme = if (isDark) darkColorScheme(...) else lightColorScheme(...)
        MaterialTheme(colorScheme = colorScheme, typography = VType.typography, content = content)
    }
}
```

### 7.2 Theme ViewModel

```kotlin
class ThemeViewModel(
    private val dataStore: DataStore<Preferences>,
    private val api: UserApi
) : ViewModel() {
    val themePref: StateFlow<String> = dataStore.data.map { it[THEME_KEY] ?: "system" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "system")

    fun setTheme(pref: String) {
        viewModelScope.launch {
            dataStore.edit { it[THEME_KEY] = pref }
            api.updateThemePref(pref)  // sync to server
        }
    }
}
```

### 7.3 Theme Switcher UI

A simple segmented control or dropdown in Settings:
- 🌙 Dark
- ☀️ Light
- 📱 System (default)

### 7.4 Status Bar Adaptation (Android)

```kotlin
// In MainActivity
val systemDark = isSystemInDarkTheme()
WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = !systemDark
```

---

## 8. Configuration

No server configuration needed. Theme preference stored per user.

---

## 9. Testing Strategy

### 9.1 Unit Tests

- Theme preference read/write to DataStore
- Theme preference sync to server
- Color token contrast ratios (automated WCAG check)

### 9.2 UI Tests

- Each screen renders correctly in light mode
- Each screen renders correctly in dark mode
- Theme switch transitions smoothly
- System mode follows OS setting

### 9.3 Visual Regression

Screenshot tests for key screens in both themes.

---

## 10. Acceptance Criteria

- [ ] Dark color palette defined for all VColors tokens
- [ ] Three-mode toggle (System/Light/Dark) available in Settings
- [ ] Theme preference persisted across app restarts
- [ ] System mode follows OS dark mode setting
- [ ] All screens render correctly in dark mode
- [ ] No hardcoded colors remain (all use VColors tokens)
- [ ] WCAG AA contrast ratios met
- [ ] Status bar adapts to theme on Android

---

## 11. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration (add theme_pref column) |
| 2 | 2 days | Define dark color palette for all tokens |
| 3 | 1 day | ThemeViewModel + DataStore integration |
| 4 | 1 day | Theme switcher UI in Settings |
| 5 | 3 days | Audit all screens for hardcoded colors, replace with tokens |
| 6 | 1 day | Status bar adaptation (Android) |
| 7 | 2 days | Tests (unit + UI + visual regression) |

---

## 12. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Modify | Add `themePref` to AppUsersTable |
| `docs/db/migration_037_theme_pref.sql` | New | Add column |
| `shared/.../core/prefs/ThemePreferences.kt` | New | DataStore theme pref |
| `shared/.../feature/user/UserApi.kt` | Modify | Add updateThemePref endpoint |
| `composeApp/.../ui/v2/theme/VColors.kt` | Modify | Add dark color tokens |
| `composeApp/.../ui/v2/theme/VTheme.kt` | Modify | Theme switching logic |
| `composeApp/.../ui/v2/screens/settings/SettingsScreen.kt` | Modify | Add theme switcher |
| `composeApp/.../ui/v2/screens/**/*.kt` | Modify | Replace hardcoded colors with tokens |
| `composeApp/.../MainActivity.kt` (Android) | Modify | Status bar adaptation |

---

## 13. Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Hardcoded colors missed | High | Low | Comprehensive audit; grep for Color(0x) |
| Contrast insufficient | Medium | Medium | Automated WCAG contrast check |
| Images/illustrations don't adapt | Medium | Low | Use tinted vector assets; provide dark variants for raster |
| Theme switch flicker | Low | Low | Animate color transitions |
