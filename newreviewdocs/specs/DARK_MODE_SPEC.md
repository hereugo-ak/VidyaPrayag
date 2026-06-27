# Theming System — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Audience:** Senior engineer / AI agent implementing the system
> **Last updated:** 2026-06-27
> **Prerequisites:** None
> **Supersedes:** Previous DARK_MODE_SPEC.md (which was out of sync with the codebase)

---

## Table of Contents

1. [Feature Overview](#1-feature-overview)
2. [Current Implementation Assessment](#2-current-implementation-assessment)
3. [Gap Analysis](#3-gap-analysis)
4. [Updated Architecture](#4-updated-architecture)
5. [Theme Token Organization](#5-theme-token-organization)
6. [Theme Registration Strategy](#6-theme-registration-strategy)
7. [Design Token Hierarchy](#7-design-token-hierarchy)
8. [Theme Selection Flow](#8-theme-selection-flow)
9. [Persistence Strategy](#9-persistence-strategy)
10. [Runtime Switching Behaviour](#10-runtime-switching-behaviour)
11. [Component Integration Guidelines](#11-component-integration-guidelines)
12. [Migration Plan](#12-migration-plan)
13. [Testing Strategy](#13-testing-strategy)
14. [Performance Considerations](#14-performance-considerations)
15. [Accessibility Considerations](#15-accessibility-considerations)
16. [Future Extensibility Guidelines](#16-future-extensibility-guidelines)
17. [File Organization Recommendations](#17-file-organization-recommendations)
18. [Example Directory Structure](#18-example-directory-structure)
19. [Implementation Roadmap](#19-implementation-roadmap)
20. [File-Level Impact Analysis](#20-file-level-impact-analysis)
21. [Risks & Mitigations](#21-risks--mitigations)

---

## 1. Feature Overview

A production-ready theming system that supports system-aware dark mode, light mode, and multiple custom themes — with an architecture designed so that **adding a new theme requires modifying only a single file**.

### Goals

- **System theme** — Follow OS dark/light setting (default)
- **Light theme** — Crisp, bright palette (existing)
- **Dark theme** — Deep-black premium night palette (existing)
- **Multiple custom themes** — Extensible without architectural changes
- **Per-user preference** — Each user selects their own theme
- **Theme picker UI** — Users choose from available themes in Settings
- **Smooth transitions** — Animated colour changes on theme switch
- **Zero hardcoded colours** — All UI reads from `VColors` tokens
- **WCAG AA compliance** — 4.5:1 contrast for text, 3:1 for large text
- **Minimal recomposition** — Theme switches don't trigger unnecessary recompositions

### Primary Design Goal

> **Adding a new custom theme requires modifying only a single file** (`VThemeRegistry.kt`). No changes elsewhere in the codebase for registering, wiring, or enabling a new theme. The architecture scales to dozens of themes with zero maintenance overhead.

---

## 2. Current Implementation Assessment

### 2.1 What Exists (Preserved as-is)

The project already has a mature, token-driven design system. The following are **kept unchanged** unless explicitly noted:

#### VColors — Colour Token Data Class

`@/Users/priyanshupatel/AndroidStudioProjects/Vidya Prayag/composeApp/src/commonMain/kotlin/com/littlebridge/enrollplus/ui/v2/theme/VColors.kt`

`VColors` is an `@Immutable data class` with ~30 colour tokens across 6 categories:

| Category | Tokens |
|---|---|
| **Brand** | `teal`, `tealDeep`, `navy`, `navyDeep`, `lavender`, `lavenderLight`, `cream`, `warmOrange` |
| **Accent** | `accent`, `accentSoft`, `accentDeep`, `accentTint` |
| **Ink (text)** | `ink`, `ink2`, `ink3`, `placeholder` |
| **Surfaces** | `background`, `card`, `border1`, `border2`, `hairline`, `shadowTint` |
| **Semantic** | `success`, `successInk`, `warning`, `warningInk`, `danger`, `dangerInk` |
| **Meta** | `isNight: Boolean` |

Two pre-built instances exist:
- `LightVColors` — light/warm palette (parent, discovery, admin/teacher)
- `NightVColors` — deep-black night palette

Raw hex values are declared once in a `private object Raw` — the single declaration site mirroring `theme.css :root / .theme-night`.

#### VPortalTone — Theme Enum

```kotlin
enum class VPortalTone { Light, Warm, Night }
```

Currently maps to `VColors` via:
```kotlin
fun vColorsFor(tone: VPortalTone): VColors = when (tone) {
    VPortalTone.Light, VPortalTone.Warm -> LightVColors
    VPortalTone.Night -> NightVColors
}
```

**Note:** `Warm` and `Light` currently resolve to the same `LightVColors` instance. This is by design — the "warm" aesthetic is a light theme with the same tokens.

#### VTheme — CompositionLocal Provider

`@/Users/priyanshupatel/AndroidStudioProjects/Vidya Prayag/composeApp/src/commonMain/kotlin/com/littlebridge/enrollplus/ui/v2/theme/VTheme.kt`

```kotlin
@Composable
fun VTheme(
    tone: VPortalTone = VPortalTone.Light,
    colors: VColors = vColorsFor(tone),
    typography: VTypography = vidyaSetuTypography(),
    dimens: VDimens = DefaultVDimens,
    content: @Composable () -> Unit,
)
```

Provides four `CompositionLocal`s:
- `LocalVColors` → `VColors`
- `LocalVType` → `VTypography`
- `LocalVDimens` → `VDimens`
- `LocalVPortalTone` → `VPortalTone`

Ergonomic accessor: `VTheme.colors`, `VTheme.type`, `VTheme.dimens`, `VTheme.tone`.

**This file is the core that will be evolved** (see §4).

#### VTypography — Type Scale

`@/Users/priyanshupatel/AndroidStudioProjects/Vidya Prayag/composeApp/src/commonMain/kotlin/com/littlebridge/enrollplus/ui/v2/theme/VType.kt`

Plus Jakarta Sans (UI) + DM Mono (data). 13 text styles. **Theme-independent** — same for all themes. Preserved unchanged.

#### VDimens — Spacing & Radii

`@/Users/priyanshupatel/AndroidStudioProjects/Vidya Prayag/composeApp/src/commonMain/kotlin/com/littlebridge/enrollplus/ui/v2/theme/VDimens.kt`

Base-4 spacing scale + border radii. **Theme-independent**. Preserved unchanged.

#### VElevation — Shadow System

`@/Users/priyanshupatel/AndroidStudioProjects/Vidya Prayag/composeApp/src/commonMain/kotlin/com/littlebridge/enrollplus/ui/v2/theme/VElevation.kt`

3-tier navy-tinted shadow system. Already checks `VTheme.colors.isNight` to skip shadows in dark mode. Preserved unchanged.

#### VMotion — Animation Tokens

`@/Users/priyanshupatel/AndroidStudioProjects/Vidya Prayag/composeApp/src/commonMain/kotlin/com/littlebridge/enrollplus/ui/v2/theme/VMotion.kt`

Spring/entrance/transition tokens. **Theme-independent**. Preserved unchanged.

#### EnrollTokens — Semantic Bridge

`@/Users/priyanshupatel/AndroidStudioProjects/Vidya Prayag/composeApp/src/commonMain/kotlin/com/littlebridge/enrollplus/ui/v2/theme/EnrollTokens.kt`

Maps the teacher-portal loop vocabulary (`Enroll.colors.primary`, `Enroll.type.headingLarge`, etc.) onto `VTheme` tokens. All accessors are `@Composable` and resolve from the active `VTheme`, so they automatically honour the active theme. Preserved unchanged.

#### PreferenceRepository — Theme Persistence

`@/Users/priyanshupatel/AndroidStudioProjects/Vidya Prayag/shared/src/commonMain/kotlin/com/littlebridge/enrollplus/core/prefs/PreferenceRepository.kt`

```kotlin
interface PreferenceRepository {
    fun getThemeName(): Flow<String>
    suspend fun setThemeName(name: String)
    // ... other prefs
}
```

Three implementations:
- `PreferenceManager` (Android/JVM — DataStore) — defaults to `"LIGHT"`
- `LocalStoragePreferenceManager` (js — `window.localStorage`) — defaults to `"LIGHT"`
- `LocalStoragePreferenceManager` (wasmJs — external JS bridge) — defaults to `"LIGHT"`

Theme is deliberately **preserved across logout** (`clearSession()` does not remove the theme key).

#### Theme Switching — Teacher Portal Only

`TeacherPortalV2.kt` reads the theme preference and applies it:
```kotlin
val themeName by preferenceRepository.getThemeName().collectAsState(initial = "WARM")
val tone = when (themeName.uppercase()) {
    "LIGHT" -> VPortalTone.Light
    "NIGHT" -> VPortalTone.Night
    else -> VPortalTone.Warm
}
VTheme(tone = tone) { /* portal content */ }
```

`TeacherProfileScreenV2.kt` has a `ThemeCard` with three options: Warm, Light, Night.

#### Status Bar Adaptation (Android)

`MainActivity.kt` already detects system dark mode for edge-to-edge styling:
```kotlin
val isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES
enableEdgeToEdge(
    statusBarStyle = if (isDarkMode) SystemBarStyle.dark(...) else SystemBarStyle.light(...),
    navigationBarStyle = if (isDarkMode) SystemBarStyle.dark(...) else SystemBarStyle.light(...),
)
```

#### Server-Side — AppUsersTable

`@/Users/priyanshupatel/AndroidStudioProjects/Vidya Prayag/server/src/main/kotlin/com/littlebridge/enrollplus/db/Tables.kt`

```kotlin
object AppUsersTable : UUIDTable("app_users", "id") {
    val languagePref = varchar("language_pref", 8).default("hi")
    // ... no themePref column
}
```

No `theme_pref` column exists. Theme preference is client-side only.

### 2.2 What's Missing

| # | Gap | Details |
|---|---|---|
| M1 | No "System" theme mode | Only LIGHT/WARM/NIGHT — no `isSystemInDarkTheme()` integration |
| M2 | Theme only works in Teacher portal | `SchoolPortalV2` hardcodes `VPortalTone.Warm`; `ParentPortalV2` and `NavGraphV2` use role-based tone, not user preference |
| M3 | No server-side persistence | No `theme_pref` column in `AppUsersTable`; theme lost on device switch |
| M4 | `VPortalTone` is a closed enum | Adding a new theme requires modifying the enum + `vColorsFor()` + every `when(tone)` site |
| M5 | VButton has 16 hardcoded `Color(0x...)` values | `tonePalette()` in `VButton.kt` doesn't read from `VColors` — won't adapt to dark mode |
| M6 | 126 hardcoded `Color(0x...)` across 22 files | Screens and components with raw hex values that bypass the token system |
| M7 | No smooth theme transition | Theme switch is instant — no animated colour crossfade |
| M8 | No Material 3 bridge | `VTheme` doesn't wrap `MaterialTheme` — Material 3 components (`Text`, `Icon`) don't get colour scheme |
| M9 | No theme picker for parents/admins | Only teachers have `ThemeCard`; parents and admins can't change theme |
| M10 | `VPortalTone.Warm` == `LightVColors` | No distinct warm palette — Warm is just an alias for Light |

---

## 3. Gap Analysis

| # | Gap | Impact | Severity |
|---|---|---|---|
| G1 | Closed enum (`VPortalTone`) blocks extensibility | Every new theme requires touching 5+ files | **Critical** |
| G2 | No system-mode detection | Users can't follow OS dark mode setting | **High** |
| G3 | Theme only applied in teacher portal | Parents/admins stuck with role-based tone | **High** |
| G4 | No server-side persistence | Theme lost on device switch or reinstall | **Medium** |
| G5 | VButton hardcoded colours | Buttons won't adapt to dark/custom themes | **High** |
| G6 | 126 hardcoded `Color(0x...)` across screens | Won't adapt to theme changes | **High** |
| G7 | No Material 3 colour scheme bridge | Material components ignore theme | **Medium** |
| G8 | No animated theme transition | Jarring flash on theme switch | **Low** |
| G9 | No theme picker for parents/admins | Only teachers can choose theme | **High** |

---

## 4. Updated Architecture

### 4.1 Design Principles

1. **Evolve, don't redesign** — The existing `VColors`/`VTheme`/`CompositionLocal` system is sound. We extend it, not replace it.
2. **Registry over enum** — Replace the closed `VPortalTone` enum with a string-keyed registry so themes are added in one file.
3. **Single-file extensibility** — Adding a theme = adding one `VThemeDef` to `VThemeRegistry.kt`.
4. **Backward compatible** — Existing `VPortalTone` references continue to work via compatibility mapping.
5. **System-aware** — A special `"system"` preference resolves to light or dark based on `isSystemInDarkTheme()`.

### 4.2 Core Type: `VThemeDef`

Replaces `VPortalTone` as the theme definition unit. A `VThemeDef` is a self-contained theme specification:

```kotlin
@Immutable
data class VThemeDef(
    val id: String,              // unique stable key: "light", "dark", "warm", "ocean", ...
    val displayName: String,     // user-facing label: "Light", "Dark", "Warm", "Ocean Blue"
    val description: String,     // user-facing caption: "Crisp & bright"
    val colors: VColors,         // the full colour token set
    val isDark: Boolean,         // whether this is a dark theme (controls shadows, status bar)
    val icon: ImageVector,       // picker UI icon (from VIcons)
)
```

### 4.3 Core Type: `VThemeMode`

The user's *intent* — which theme to use, including system-follow:

```kotlin
enum class VThemeMode {
    SYSTEM,   // follow OS dark/light
    LIGHT,    // force light
    DARK,     // force dark
    CUSTOM,   // force a specific custom theme (id stored separately)
}
```

When mode is `SYSTEM`, the resolved theme is `"light"` or `"dark"` based on `isSystemInDarkTheme()`.
When mode is `LIGHT` / `DARK`, the resolved theme is the theme with that id.
When mode is `CUSTOM`, the resolved theme is the one whose `id` matches the stored custom theme id.

### 4.4 Theme Registry: `VThemeRegistry`

The **single file** that defines all available themes. This is the only file that needs modification to add a new theme:

```kotlin
object VThemeRegistry {
    val themes: List<VThemeDef> = listOf(
        VThemeDef(
            id = "light",
            displayName = "Light",
            description = "Crisp & bright",
            colors = LightVColors,
            isDark = false,
            icon = VIcons.Star,
        ),
        VThemeDef(
            id = "dark",
            displayName = "Dark",
            description = "Easy on the eyes",
            colors = NightVColors,
            isDark = true,
            icon = VIcons.Bookmark,
        ),
        VThemeDef(
            id = "warm",
            displayName = "Warm",
            description = "Cream & lavender",
            colors = LightVColors,  // warm == light tokens (existing behaviour)
            isDark = false,
            icon = VIcons.Sparkles,
        ),
        // ── Add new themes below this line ──────────────────────────────────
        // VThemeDef(
        //     id = "ocean",
        //     displayName = "Ocean Blue",
        //     description = "Cool & calming",
        //     colors = OceanVColors,
        //     isDark = false,
        //     icon = VIcons.Water,
        // ),
    )

    val byId: Map<String, VThemeDef> = themes.associateBy { it.id }

    val defaultTheme: VThemeDef = themes.first { it.id == "light" }
    val defaultDarkTheme: VThemeDef = themes.first { it.id == "dark" }

    fun resolve(themeId: String): VThemeDef =
        byId[themeId] ?: defaultTheme

    fun resolveSystem(isSystemDark: Boolean): VThemeDef =
        if (isSystemDark) defaultDarkTheme else defaultTheme
}
```

**Adding a new theme = add one `VThemeDef` entry + its `VColors` instance to this file.** No other file changes needed.

### 4.5 Updated `VTheme` Composable

The `VTheme` composable is evolved to accept a `VThemeDef` instead of (or alongside) `VPortalTone`:

```kotlin
@Composable
fun VTheme(
    themeDef: VThemeDef = VThemeRegistry.defaultTheme,
    typography: VTypography = vidyaSetuTypography(),
    dimens: VDimens = DefaultVDimens,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalVColors provides themeDef.colors,
        LocalVType provides typography,
        LocalVDimens provides dimens,
        LocalVThemeDef provides themeDef,
        // Material 3 bridge (see §4.7)
        LocalMaterialTheme provides materialColorSchemeFor(themeDef),
        content = content,
    )
}
```

### 4.6 Backward Compatibility with `VPortalTone`

`VPortalTone` is preserved as a deprecated alias to avoid breaking existing call sites during migration:

```kotlin
@Deprecated("Use VThemeDef via VThemeRegistry instead")
enum class VPortalTone { Light, Warm, Night }

@Deprecated("Use VThemeRegistry.resolve(id) instead")
fun vColorsFor(tone: VPortalTone): VColors = when (tone) {
    VPortalTone.Light -> VThemeRegistry.resolve("light").colors
    VPortalTone.Warm -> VThemeRegistry.resolve("warm").colors
    VPortalTone.Night -> VThemeRegistry.resolve("dark").colors
}

@Deprecated("Use VTheme(themeDef = ...) instead")
@Composable
fun VTheme(
    tone: VPortalTone,
    content: @Composable () -> Unit,
) {
    val def = when (tone) {
        VPortalTone.Light -> VThemeRegistry.resolve("light")
        VPortalTone.Warm -> VThemeRegistry.resolve("warm")
        VPortalTone.Night -> VThemeRegistry.resolve("dark")
    }
    VTheme(themeDef = def, content = content)
}
```

Existing call sites like `VTheme(tone = VPortalTone.Warm) { ... }` continue to compile and work. They are migrated to `VTheme(themeDef = ...)` incrementally.

### 4.7 Material 3 Bridge

To ensure Material 3 components (`Text`, `Icon`, `Slider`, `Switch`, etc.) honour the active theme, `VTheme` also wraps content in `MaterialTheme` with a derived `ColorScheme`:

```kotlin
@Composable
private fun materialColorSchemeFor(def: VThemeDef): ColorScheme {
    val c = def.colors
    return if (def.isDark) darkColorScheme(
        primary = c.accent,
        onPrimary = c.card,
        background = c.background,
        onBackground = c.ink,
        surface = c.card,
        onSurface = c.ink,
        surfaceVariant = c.cream,
        onSurfaceVariant = c.ink2,
        outline = c.border1,
        outlineVariant = c.hairline,
        error = c.dangerInk,
        onError = c.card,
    ) else lightColorScheme(
        primary = c.accent,
        onPrimary = c.card,
        background = c.background,
        onBackground = c.ink,
        surface = c.card,
        onSurface = c.ink,
        surfaceVariant = c.cream,
        onSurfaceVariant = c.ink2,
        outline = c.border1,
        outlineVariant = c.hairline,
        error = c.dangerInk,
        onError = c.card,
    )
}
```

This is wrapped inside `VTheme`:
```kotlin
MaterialTheme(colorScheme = materialColorSchemeFor(themeDef)) {
    CompositionLocalProvider(
        LocalVColors provides themeDef.colors,
        // ... other locals
        content = content,
    )
}
```

**This does not replace `VColors`** — it's a bridge so that any Material 3 component that reads `MaterialTheme.colorScheme` gets the right colours. The canonical source of truth remains `VTheme.colors`.

### 4.8 New CompositionLocal: `LocalVThemeDef`

Replaces `LocalVPortalTone`:

```kotlin
val LocalVThemeDef: ProvidableCompositionLocal<VThemeDef> =
    staticCompositionLocalOf { VThemeRegistry.defaultTheme }
```

The `VTheme.tone` accessor is replaced by `VTheme.themeDef`:
```kotlin
object VTheme {
    val colors: VColors @Composable get() = LocalVColors.current
    val type: VTypography @Composable get() = LocalVType.current
    val dimens: VDimens @Composable get() = LocalVDimens.current
    val themeDef: VThemeDef @Composable get() = LocalVThemeDef.current

    // Deprecated backward-compat accessor
    @Deprecated("Use themeDef instead")
    val tone: VPortalTone @Composable get() = when (themeDef.id) {
        "dark" -> VPortalTone.Night
        "warm" -> VPortalTone.Warm
        else -> VPortalTone.Light
    }
}
```

### 4.9 Theme Resolution Flow

```
User preference (PreferenceRepository)
    │
    ├── mode: "system" ──→ isSystemInDarkTheme() ──→ VThemeRegistry.resolveSystem()
    ├── mode: "light"  ──→ VThemeRegistry.resolve("light")
    ├── mode: "dark"   ──→ VThemeRegistry.resolve("dark")
    └── mode: "custom" ──→ VThemeRegistry.resolve(customThemeId)
                                │
                                ▼
                         VThemeDef (id, colors, isDark, ...)
                                │
                                ▼
                         VTheme(themeDef = resolved)
                                │
                                ▼
                    CompositionLocalProvider
                                │
                    ┌───────────┼───────────┐
                    ▼           ▼           ▼
              LocalVColors  LocalVThemeDef  MaterialTheme
                    │           │
                    ▼           ▼
              VTheme.colors  VTheme.themeDef
              (all UI)     (shadows, status bar)
```

---

## 5. Theme Token Organization

### 5.1 Token Categories (unchanged)

The `VColors` data class categories are preserved:

| Category | Purpose | Theme-variant? |
|---|---|---|
| **Brand** | School identity colours | Yes — each theme defines its own |
| **Accent** | Primary interactive colour | Yes |
| **Ink** | Text colours | Yes |
| **Surfaces** | Background, card, borders | Yes |
| **Semantic** | Data states (success/warning/danger) | Yes — dark themes use brighter inks |
| **Meta** | `isNight` flag | Yes — controls shadow/elevation behaviour |

### 5.2 Token Naming Convention

Existing token names are preserved. New custom themes use the same `VColors` fields — a theme is just a different set of values for the same fields, not new field names.

### 5.3 Raw Value Declaration

Each theme's raw hex values are declared as `private val` properties inside `VThemeRegistry.kt` (or in a companion object of the registry). This keeps all colour definitions in one file:

```kotlin
object VThemeRegistry {
    // ── Light palette (existing, from VColors.kt Raw object) ──
    private val lightColors = VColors(
        teal = Color(0xFF3CB9A9),
        // ... all 30 tokens
        isNight = false,
    )

    // ── Dark palette (existing, from VColors.kt Raw object) ──
    private val darkColors = VColors(
        teal = Color(0xFF3CD1BE),
        // ... all 30 tokens
        isNight = true,
    )

    // ── New custom theme: Ocean Blue ──
    private val oceanColors = VColors(
        teal = Color(0xFF3CB9A9),
        // ... all 30 tokens with ocean-blue values
        isNight = false,
    )

    val themes = listOf(
        VThemeDef("light", "Light", "Crisp & bright", lightColors, isDark = false, icon = VIcons.Star),
        VThemeDef("dark", "Dark", "Easy on the eyes", darkColors, isDark = true, icon = VIcons.Bookmark),
        VThemeDef("warm", "Warm", "Cream & lavender", lightColors, isDark = false, icon = VIcons.Sparkles),
        VThemeDef("ocean", "Ocean Blue", "Cool & calming", oceanColors, isDark = false, icon = VIcons.Water),
    )
}
```

### 5.4 Typography, Dimens, Shapes, Motion, Elevation

These token systems are **theme-independent** and shared across all themes:
- `VTypography` — Plus Jakarta Sans + DM Mono (same for all themes)
- `VDimens` — Base-4 spacing + radii (same for all themes)
- `VElevation` — Shadow system (already adapts via `isNight`)
- `VMotion` — Animation tokens (same for all themes)

If a future theme needs different typography or shapes, `VThemeDef` can be extended with optional `typography` and `dimens` fields (see §16).

---

## 6. Theme Registration Strategy

### 6.1 Single-File Registration

All themes are registered in `VThemeRegistry.kt`. This is the **only file** that changes when adding a theme.

### 6.2 Adding a New Theme — Step by Step

1. **Open** `VThemeRegistry.kt`
2. **Define** the `VColors` instance with all 30 tokens
3. **Add** a `VThemeDef` entry to the `themes` list
4. **Done** — the theme automatically appears in the theme picker UI, is selectable by users, and persists correctly

No changes needed in:
- `VTheme.kt` — the composable reads from the registry
- `PreferenceManager.kt` — stores any string theme id
- `ThemePicker.kt` — iterates `VThemeRegistry.themes` dynamically
- `NavGraphV2.kt` — resolves theme from preference
- Any screen or component — all read from `VTheme.colors`

### 6.3 Theme ID Stability

Theme IDs are **stable strings** that must never change once released (they're persisted in DataStore and server-side). Renaming an id breaks existing user preferences. The `displayName` can change freely.

### 6.4 Reserved IDs

| ID | Meaning |
|---|---|
| `"light"` | The canonical light theme |
| `"dark"` | The canonical dark theme |
| `"warm"` | The warm-light variant (backward compat with teacher portal) |
| `"system"` | Not a theme id — it's a mode value stored in preferences |

---

## 7. Design Token Hierarchy

```
VThemeDef
├── VColors (colour tokens — per-theme)
│   ├── Brand: teal, tealDeep, navy, navyDeep, lavender, lavenderLight, cream, warmOrange
│   ├── Accent: accent, accentSoft, accentDeep, accentTint
│   ├── Ink: ink, ink2, ink3, placeholder
│   ├── Surfaces: background, card, border1, border2, hairline, shadowTint
│   ├── Semantic: success, successInk, warning, warningInk, danger, dangerInk
│   └── Meta: isNight
├── VTypography (type scale — shared, but can be overridden per-theme in future)
│   ├── UI: h1, h2, h3, h4, body, bodyStrong, caption, label, labelStrong, inputLabel
│   └── Data: data, dataSm, dataLg
├── VDimens (spacing + radii — shared)
│   ├── Spacing: xs, sm, md, lg, xl, xxl, xxxl, screenPadding
│   └── Radii: radiusSm, radiusMd, radiusInput, radiusLg, radiusCard, radiusXl, radiusSheet, radiusPill
├── VElevation (shadow system — adapts via isNight)
│   └── Levels: Card, Raised, Modal
├── VMotion (animation tokens — shared)
│   ├── Springs: springSoft, springSheet, springCard, springSnappy
│   └── Transitions: forwardSlide, modalRise, quietFade, fadeUp
└── Material 3 ColorScheme (derived bridge — auto-generated from VColors)
    ├── primary, onPrimary, primaryContainer, onPrimaryContainer
    ├── secondary, onSecondary, surface, onSurface
    ├── background, onBackground, error, onError
    └── outline, outlineVariant, surfaceVariant, onSurfaceVariant
```

---

## 8. Theme Selection Flow

### 8.1 User Flow

1. User opens **Settings** (Profile tab → Appearance section)
2. A **theme picker** shows all themes from `VThemeRegistry.themes` as selectable cards
3. A **mode selector** offers: System / Light / Dark / (Custom themes appear as individual cards)
4. User taps a theme → `PreferenceRepository.setThemeMode(mode)` + `setCustomThemeId(id)` (if custom)
5. The `StateFlow<String>` emits → the portal recomposes with the new `VThemeDef`
6. A smooth crossfade animation transitions the colours

### 8.2 Unified Theme Picker

The existing `ThemeCard` in `TeacherProfileScreenV2.kt` is extracted into a shared component:

```kotlin
@Composable
fun VThemePicker(
    currentMode: VThemeMode,
    currentCustomId: String?,
    onSelect: (mode: VThemeMode, customId: String?) -> Unit,
)
```

This iterates `VThemeRegistry.themes` dynamically — new themes appear automatically. The picker shows:
- **System** option (follows OS)
- One card per registered theme (Light, Dark, Warm, Ocean, ...)
- The active selection is highlighted with the accent colour

### 8.3 Theme Application at App Root

Instead of per-portal theme application (current: only `TeacherPortalV2`), theme is applied at the **`NavGraphV2` level** so all portals honour it:

```kotlin
@Composable
fun NavGraphV2(...) {
    val preferenceRepository = koinInject<PreferenceRepository>()
    val themeMode by preferenceRepository.getThemeMode().collectAsState(initial = "system")
    val customThemeId by preferenceRepository.getCustomThemeId().collectAsState(initial = null)

    val resolvedDef = resolveThemeDef(themeMode, customThemeId)

    VTheme(themeDef = resolvedDef) {
        // AuthedFlow / UnauthFlow — all inherit the user's theme
    }
}

@Composable
private fun resolveThemeDef(mode: String, customId: String?): VThemeDef {
    return when (mode) {
        "light" -> VThemeRegistry.resolve("light")
        "dark" -> VThemeRegistry.resolve("dark")
        "custom" -> VThemeRegistry.resolve(customId ?: "light")
        else -> VThemeRegistry.resolveSystem(isSystemInDarkTheme())
    }
}
```

**This replaces the current role-based tone logic** in `EntryRole.tone()`. The role-based tone was a design decision to give admins a "warm" look, but user preference should take priority. The role-based default can be used as the *initial* default before the user has chosen (see §9).

---

## 9. Persistence Strategy

### 9.1 Client-Side (DataStore / LocalStorage)

Two new preference keys:

| Key | Type | Default | Values |
|---|---|---|---|
| `theme_mode` | String | `"system"` | `"system"` / `"light"` / `"dark"` / `"custom"` |
| `custom_theme_id` | String? | `null` | Any registered theme id (e.g. `"ocean"`) |

#### PreferenceRepository Extension

```kotlin
interface PreferenceRepository {
    // Existing
    fun getThemeName(): Flow<String>
    suspend fun setThemeName(name: String)

    // New
    fun getThemeMode(): Flow<String>
    suspend fun setThemeMode(mode: String)

    fun getCustomThemeId(): Flow<String?>
    suspend fun setCustomThemeId(id: String?)

    // ... rest unchanged
}
```

**Backward compatibility:** `getThemeName()` / `setThemeName()` are preserved. The existing `"LIGHT"` / `"WARM"` / `"NIGHT"` values are mapped:
- `"LIGHT"` → mode = `"light"`
- `"WARM"` → mode = `"custom"`, customId = `"warm"`
- `"NIGHT"` → mode = `"dark"`

A one-time migration in `PreferenceManager` reads the old `theme_name` key and writes the new `theme_mode` + `custom_theme_id` keys if `theme_mode` is not yet set.

#### Implementation per platform

| Platform | File | Mechanism |
|---|---|---|
| Android / JVM | `PreferenceManager.kt` | DataStore `stringPreferencesKey("theme_mode")` + `stringPreferencesKey("custom_theme_id")` |
| JS | `LocalStoragePreferenceManager.kt` | `localStorage["vp.themeMode"]` + `localStorage["vp.customThemeId"]` |
| wasmJs | `LocalStoragePreferenceManager.kt` | Same as JS via external bridge |

Theme preferences are **preserved across logout** (same as current `theme_name`).

### 9.2 Server-Side (Database)

Add `theme_pref` column to `app_users`:

```sql
ALTER TABLE app_users ADD COLUMN theme_pref VARCHAR(32) NOT NULL DEFAULT 'system';
-- Values: "system" | "light" | "dark" | "custom:<theme_id>" (e.g. "custom:ocean")
```

Using a single column with a composite value (`"custom:ocean"`) keeps the schema simple. The client parses it into mode + customId.

#### Exposed Mapping

```kotlin
object AppUsersTable : UUIDTable("app_users", "id") {
    // ... existing fields
    val themePref = varchar("theme_pref", 32).default("system")
}
```

#### API Endpoint

Add `PATCH /api/v1/user/theme-pref`:

```json
// Request
{ "themePref": "system" }  // or "light", "dark", "custom:ocean"

// Response
{ "ok": true }
```

The client syncs to the server in the background after a local DataStore write (best-effort, non-blocking). On login, the server's `themePref` is fetched via `GET /api/v1/user/details` and reconciled with the local preference (server wins if local is at default, local wins if user has already chosen on this device).

### 9.3 Migration SQL

File: `docs/db/migration_037_theme_pref.sql`

```sql
-- Migration 037: Add theme_pref column to app_users
ALTER TABLE app_users ADD COLUMN theme_pref VARCHAR(32) NOT NULL DEFAULT 'system';
```

---

## 10. Runtime Switching Behaviour

### 10.1 Reactive Flow

Theme switching is fully reactive via `StateFlow`:

```
User taps theme
    │
    ▼
PreferenceRepository.setThemeMode("dark")
    │
    ▼
DataStore emits new value
    │
    ▼
StateFlow<String> emits in NavGraphV2
    │
    ▼
resolveThemeDef() returns new VThemeDef
    │
    ▼
VTheme(themeDef = newDef) recomposes
    │
    ▼
CompositionLocalProvider provides new VColors
    │
    ▼
All @Composable functions reading VTheme.colors recompose
    │
    ▼
UI renders with new colours
```

### 10.2 Smooth Transition

To avoid a jarring flash, wrap the theme switch in an `AnimatedContent`:

```kotlin
AnimatedContent(
    targetState = resolvedDef,
    transitionSpec = {
        fadeIn(tween(300)) togetherWith fadeOut(tween(200))
    },
    label = "theme-switch",
) { def ->
    VTheme(themeDef = def) {
        content()
    }
}
```

This crossfades the entire app tree from the old theme to the new theme over 300ms. The `staticCompositionLocalOf` for `VColors` ensures only the `VTheme` wrapper recomposes — not every individual component — because `CompositionLocalProvider` with a new value invalidates only the content lambda.

### 10.3 Recomposition Efficiency

- `VColors` is `@Immutable` → Compose skips equality checks on its fields
- `LocalVColors` is `staticCompositionLocalOf` → no snapshot tracking overhead
- Only the `VTheme` content lambda recomposes when the `VColors` instance changes
- Individual `@Composable` functions that read `VTheme.colors` recompose only if their output changes (which it will, since colours changed) — but this is unavoidable and correct

### 10.4 Status Bar Adaptation

The status bar should adapt to the resolved theme, not just the OS setting. In `MainActivity.kt`:

```kotlin
// Observe the resolved theme def's isDark flag
val isDark = resolvedThemeDef.isDark
enableEdgeToEdge(
    statusBarStyle = if (isDark) SystemBarStyle.dark(TRANSPARENT) else SystemBarStyle.light(TRANSPARENT, TRANSPARENT),
    navigationBarStyle = if (isDark) SystemBarStyle.dark(TRANSPARENT) else SystemBarStyle.light(TRANSPARENT, TRANSPARENT),
)
```

This requires the resolved theme to be observable at the `MainActivity` level. The simplest approach: hoist the theme `StateFlow` above `App()` and pass the `isDark` flag down as a parameter, or use a `SideEffect` to update the system bars whenever `VTheme.themeDef.isDark` changes.

---

## 11. Component Integration Guidelines

### 11.1 Rules for All Components

| Rule | Description |
|---|---|
| **R1** | Never hardcode `Color(0x...)` — always read from `VTheme.colors` |
| **R2** | Default colour parameters should reference `VTheme.colors` — e.g. `color: Color = VTheme.colors.ink` |
| **R3** | Never read `MaterialTheme.colorScheme` directly — use `VTheme.colors` (the Material 3 bridge is for third-party components only) |
| **R4** | Never reference `VPortalTone` in new code — use `VThemeDef` via `VThemeRegistry` |
| **R5** | Semantic colours (`success`, `warning`, `danger`) are for **data states only**, never branding |

### 11.2 VButton — Hardcoded Colour Fix

`VButton.kt`'s `tonePalette()` has 16 hardcoded `Color(0x...)` values. These must be replaced with `VColors` token reads:

**Before (hardcoded):**
```kotlin
private fun tonePalette(tone: VButtonTone): TonePal = when (tone) {
    VButtonTone.Navy -> TonePal(
        bg = Color(0xFF26234D), fg = Color.White, shadow = Color(0x4D26234D),
        soft = Color(0xFFD8D2F1), softFg = Color(0xFF26234D), ...)
}
```

**After (token-driven):**
```kotlin
@Composable
private fun tonePalette(tone: VButtonTone): TonePal {
    val c = VTheme.colors
    return when (tone) {
        VButtonTone.Navy -> TonePal(
            bg = c.navy, fg = c.card, shadow = c.navy.copy(alpha = 0.3f),
            soft = c.lavenderLight, softFg = c.navy, softBorder = c.navy.copy(alpha = 0.22f), softShadow = c.navy.copy(alpha = 0.16f))
        VButtonTone.Teal -> TonePal(
            bg = c.tealDeep, fg = c.card, shadow = c.tealDeep.copy(alpha = 0.28f),
            soft = c.teal.copy(alpha = 0.25f), softFg = c.tealDeep, ...)
        // ... map all 8 tones to VColors tokens
    }
}
```

This makes `tonePalette` a `@Composable` function — a minor but necessary change. The `VButton` composable already reads `VTheme.colors`, so this is consistent.

### 11.3 EnrollTokens Bridge

`EnrollTokens.kt` already maps semantic names to `VColors` tokens. **No changes needed** — it automatically honours any theme because it reads from `VTheme.colors`.

### 11.4 VElevation

Already checks `VTheme.colors.isNight` to skip shadows in dark mode. **No changes needed.**

### 11.5 Hardcoded Colour Audit

126 `Color(0x...)` matches across 22 files in `composeApp/src/commonMain`. These must be replaced with `VTheme.colors` token reads. The audit is a mechanical process:

1. `grep -rn "Color(0x" composeApp/src/commonMain --include="*.kt"`
2. For each match, identify the semantic purpose (background, text, border, etc.)
3. Replace with the corresponding `VTheme.colors.*` token
4. If no existing token fits, add a new token to `VColors` and populate it in all theme instances

**Files with hardcoded colours (by count):**

| File | Count | Priority |
|---|---|---|
| `VColors.kt` | 48 | **Keep** — this is the token declaration site |
| `VButton.kt` | 16 | **High** — fix `tonePalette()` |
| `VAvatar.kt` | 7 | High |
| `ParentProfileCardScreenV2.kt` | 7 | High |
| `ParentLinkChildScreenV2.kt` | 5 | Medium |
| `NotificationsScreenV2.kt` | 5 | Medium |
| `SchoolHomeScreenV2.kt` | 5 | Medium |
| `CommonLandingScreenV3.kt` | 4 | Medium |
| `DiscoveryScreenV2.kt` | 4 | Medium |
| `PewsPreview.kt` | 4 | Low |
| `VBadge.kt` | 3 | High |
| `SchoolOnboardingScreenV2.kt` | 3 | Medium |
| `SriPreview.kt` | 3 | Low |
| `VNavigation.kt` | 2 | High |
| `ParentPalette.kt` | 2 | Medium |
| `TeacherKit.kt` | 2 | Medium |
| `App.kt` | 1 | Low (debug banner) |
| `VInput.kt` | 1 | High |
| `ParentActivityScreenV2.kt` | 1 | Medium |
| `ParentFeesScreenV2.kt` | 1 | Medium |
| `LinkRequestsScreenV2.kt` | 1 | Medium |
| `EnrollTokens.kt` | 1 | **Keep** — `Color.White` for `onPrimary` |

**Note:** `VColors.kt` (48 matches) and `EnrollTokens.kt` (1 match for `Color.White`) are the declaration sites and are correct. The remaining ~77 matches across 20 files need fixing.

---

## 12. Migration Plan

### Phase 1: Core Architecture (non-breaking)

1. Create `VThemeDef.kt` and `VThemeRegistry.kt` with the three existing themes (light, dark, warm)
2. Add `LocalVThemeDef` CompositionLocal
3. Add `VTheme(themeDef = ...)` overload alongside existing `VTheme(tone = ...)`
4. Add Material 3 bridge inside `VTheme`
5. Add backward-compat `VPortalTone` → `VThemeDef` mapping

**No existing code breaks.** All `VTheme(tone = VPortalTone.Warm)` calls continue to work.

### Phase 2: Preference System

1. Add `theme_mode` + `custom_theme_id` keys to `PreferenceRepository` interface
2. Implement in `PreferenceManager`, `LocalStoragePreferenceManager` (js + wasmJs)
3. Add one-time migration from `theme_name` → `theme_mode` + `custom_theme_id`
4. Add `getThemeMode()` / `setThemeMode()` to `MainViewModel`

### Phase 3: Theme Application at NavGraphV2

1. Update `NavGraphV2` to read theme preference and resolve via `VThemeRegistry`
2. Replace role-based `EntryRole.tone()` with user-preference-based resolution
3. Use role-based tone only as the default before user has chosen (first launch)
4. Wrap in `AnimatedContent` for smooth transition

### Phase 4: Theme Picker UI

1. Extract `ThemeCard` from `TeacherProfileScreenV2.kt` into shared `VThemePicker.kt`
2. Add `VThemePicker` to parent and admin profile/settings screens
3. Picker iterates `VThemeRegistry.themes` dynamically

### Phase 5: Hardcoded Colour Audit

1. Fix `VButton.tonePalette()` — make it `@Composable`, read from `VTheme.colors`
2. Fix remaining 77 hardcoded `Color(0x...)` across 20 files
3. Add new `VColors` tokens if needed for colours that don't map to existing tokens

### Phase 6: Server-Side Persistence

1. Add `theme_pref` column to `AppUsersTable` + migration SQL
2. Add `PATCH /api/v1/user/theme-pref` endpoint
3. Include `themePref` in `GET /api/v1/user/details` response
4. Client syncs preference to server (best-effort) and reconciles on login

### Phase 7: Status Bar + Polish

1. Update `MainActivity.kt` to observe resolved theme's `isDark` for system bars
2. Add animated theme transition
3. Final WCAG contrast audit
4. Screenshot tests for key screens in all themes

### Phase 8: Deprecation Cleanup

1. Migrate all `VTheme(tone = ...)` calls to `VTheme(themeDef = ...)`
2. Migrate all `VTheme.tone` reads to `VTheme.themeDef`
3. Mark `VPortalTone` as `@Deprecated`
4. Remove `vColorsFor(tone: VPortalTone)` (keep `VThemeRegistry.resolve()`)

---

## 13. Testing Strategy

### 13.1 Unit Tests

| Test | What it verifies |
|---|---|
| `VThemeRegistry.resolve("light")` | Returns the light theme def |
| `VThemeRegistry.resolve("nonexistent")` | Falls back to default |
| `VThemeRegistry.resolveSystem(true/false)` | Returns dark/light based on flag |
| `VThemeRegistry.themes` contains expected ids | Registry integrity |
| `VThemeRegistry.byId` matches `themes` | No duplicate ids |
| Preference migration: old `theme_name` → new `theme_mode` | One-time migration correctness |
| DataStore read/write of `theme_mode` | Persistence round-trip |
| Server `theme_pref` column default | Database migration correctness |

### 13.2 UI Tests

| Test | What it verifies |
|---|---|
| Each screen renders in light theme | No crashes, colours applied |
| Each screen renders in dark theme | No crashes, colours applied |
| Theme picker shows all registered themes | Dynamic iteration works |
| Tapping a theme switches the UI | Reactive flow works |
| System mode follows OS setting | `isSystemInDarkTheme()` integration |
| Theme persists across app restart | DataStore persistence |
| Theme persists across logout/login | Not cleared in `clearSession()` |

### 13.3 Visual Regression

Screenshot tests for key screens (Home, Profile, Messages, Notifications, Settings) in each registered theme. Use `paparazzi` or `roborazzi` for Compose Multiplatform screenshot testing.

### 13.4 Contrast Audit

Automated WCAG AA contrast check for each theme's colour pairs:

```kotlin
@Test
fun `light theme meets WCAG AA contrast`() {
    val c = LightVColors
    assertContrastRatio(c.ink, c.background, 4.5)          // body text on bg
    assertContrastRatio(c.ink2, c.card, 4.5)                // secondary text on card
    assertContrastRatio(c.accent, c.card, 4.5)              // accent on card
    assertContrastRatio(c.successInk, c.success, 4.5)       // status ink on status fill
    // ... all critical pairs
}
```

---

## 14. Performance Considerations

### 14.1 CompositionLocal Efficiency

`LocalVColors` uses `staticCompositionLocalOf` — this is the most efficient form. It means:
- No snapshot tracking overhead
- The entire content lambda of `VTheme` recomposes when the `VColors` instance changes
- Individual composables that read `VTheme.colors` recompose only if their output changes

**This is already optimal and preserved unchanged.**

### 14.2 Theme Registry Lookup

`VThemeRegistry.byId` is a `Map<String, VThemeDef>` built once at class init. Lookups are O(1). The `themes` list is a `List<VThemeDef>` — iterating it in the theme picker is O(n) where n is the number of themes (typically 3–10).

### 14.3 AnimatedContent for Theme Switch

The `AnimatedContent` wrapper for smooth theme transition composes both the old and new tree briefly during the 300ms crossfade. This is acceptable because:
- The crossfade is short (300ms)
- Only the top-level `VTheme` wrapper is in the `AnimatedContent` — not the entire nav graph
- Memory impact is one extra composition tree for 300ms

### 14.4 VButton tonePalette

Making `tonePalette` a `@Composable` function adds a negligible overhead — it's called once per `VButton` composition and reads from an already-provided `CompositionLocal`.

### 14.5 Material 3 Bridge

The `materialColorSchemeFor()` function creates a `ColorScheme` on each theme change. This is a lightweight data class construction — no measurable overhead.

---

## 15. Accessibility Considerations

### 15.1 WCAG AA Contrast

Every theme must meet:
- **4.5:1** for normal text (< 18pt) against its background
- **3:1** for large text (≥ 18pt or ≥ 14pt bold) against its background
- **3:1** for UI components and graphical objects against adjacent colours

Critical pairs to verify per theme:

| Foreground | Background | Min Ratio |
|---|---|---|
| `ink` | `background` | 4.5:1 |
| `ink` | `card` | 4.5:1 |
| `ink2` | `background` | 4.5:1 |
| `ink2` | `card` | 4.5:1 |
| `ink3` | `background` | 3:1 |
| `accent` | `card` | 4.5:1 |
| `accent` | `background` | 4.5:1 |
| `successInk` | `success` | 4.5:1 |
| `warningInk` | `warning` | 4.5:1 |
| `dangerInk` | `danger` | 4.5:1 |
| `navy` | `lavender` | 4.5:1 |

### 15.2 Dark Theme Considerations

- The existing `NightVColors` uses a deep-black background (`#050505`) with near-white ink (`#F4F4F6`) — contrast ratio is ~19:1, well above AA
- Accent colours are brightened on dark backgrounds (e.g. accent `#6C5CE0` → `#8B7EE8`) to maintain vibrancy
- Shadows are disabled in dark mode (`VElevation` checks `isNight`) — elevation is conveyed through surface colour differences instead

### 15.3 Reduced Motion

The theme switch crossfade respects `LocalAccessibilityManager` / system "reduce motion" settings:
- If reduce motion is on → instant switch (no `AnimatedContent`)
- If reduce motion is off → 300ms crossfade

### 15.4 Theme Picker Accessibility

- Each theme card has a semantic label: "Select ${displayName} theme, ${description}"
- The active theme is announced: "Currently selected"
- Cards are keyboard navigable (focus order = visual order)

---

## 16. Future Extensibility Guidelines

### 16.1 Adding a New Theme

1. Open `VThemeRegistry.kt`
2. Define a `VColors` instance with all 30 tokens
3. Add a `VThemeDef` to the `themes` list
4. Done — the theme appears in the picker, is selectable, persists, and syncs to the server

### 16.2 Per-Theme Typography (Future)

If a theme needs different fonts or type sizes, extend `VThemeDef`:

```kotlin
data class VThemeDef(
    // ... existing fields
    val typography: VTypography? = null,  // null = use default
)
```

Then in `VTheme`:
```kotlin
val activeTypography = themeDef.typography ?: vidyaSetuTypography()
```

This is a **non-breaking** extension — existing themes that don't specify typography get the default.

### 16.3 Per-Theme Dimensions (Future)

Same pattern as typography:
```kotlin
data class VThemeDef(
    // ... existing fields
    val dimens: VDimens? = null,  // null = use default
)
```

### 16.4 Dynamic Colour (Android 12+)

For Material You dynamic colour support, add a special `VThemeDef` with `id = "dynamic"` that reads from `dynamicColorScheme(context)`:

```kotlin
// In VThemeRegistry
VThemeDef(
    id = "dynamic",
    displayName = "System Dynamic",
    description = "Matches your wallpaper",
    colors = dynamicVColors(context),  // @Composable, reads dynamicColorScheme
    isDark = isSystemInDarkTheme(),
    icon = VIcons.Palette,
)
```

This is only registered on Android 12+ (expect/actual pattern). On other platforms, the "dynamic" theme is not shown in the picker.

### 16.5 School-Branded Themes

Schools can define custom themes with their brand colours. These would be fetched from the server and registered dynamically:

```kotlin
// After fetching school branding
VThemeRegistry.registerDynamic(
    VThemeDef("school_brand", "School Brand", "Your school's colours", schoolColors, ...)
)
```

The registry supports a `registerDynamic()` method for runtime-added themes (e.g. from server config) alongside the compile-time `themes` list.

### 16.6 Theme Inheritance (Future)

For themes that share most tokens with a base theme but override a few:

```kotlin
val oceanColors = LightVColors.copy(
    accent = Color(0xFF006994),
    accentSoft = Color(0xFF4DA6CB),
    accentDeep = Color(0xFF004D6B),
    accentTint = Color(0xFFE8F4FA),
)
```

This is already possible because `VColors` is a `data class` — the `.copy()` method is free.

---

## 17. File Organization Recommendations

### 17.1 New Files

| File | Location | Purpose |
|---|---|---|
| `VThemeDef.kt` | `composeApp/.../ui/v2/theme/` | `VThemeDef` data class + `VThemeMode` enum |
| `VThemeRegistry.kt` | `composeApp/.../ui/v2/theme/` | All theme definitions + registry logic |
| `VThemePicker.kt` | `composeApp/.../ui/v2/components/` | Shared theme picker component |
| `migration_037_theme_pref.sql` | `docs/db/` | Database migration |

### 17.2 Modified Files

| File | Changes |
|---|---|
| `VColors.kt` | Move `LightVColors` / `NightVColors` instances into `VThemeRegistry` (or keep as-is and reference from registry) |
| `VTheme.kt` | Add `VTheme(themeDef = ...)` overload, `LocalVThemeDef`, Material 3 bridge, deprecate `VPortalTone` path |
| `VButton.kt` | Make `tonePalette()` `@Composable`, replace hardcoded colours with `VTheme.colors` tokens |
| `PreferenceRepository.kt` | Add `getThemeMode()` / `setThemeMode()` / `getCustomThemeId()` / `setCustomThemeId()` |
| `PreferenceManager.kt` | Implement new preference keys + one-time migration |
| `LocalStoragePreferenceManager.kt` (js) | Implement new preference keys |
| `LocalStoragePreferenceManager.kt` (wasmJs) | Implement new preference keys |
| `MainViewModel.kt` | Add `themeMode` / `customThemeId` StateFlows |
| `NavGraphV2.kt` | Read theme preference, resolve via `VThemeRegistry`, apply `VTheme(themeDef = ...)` |
| `TeacherPortalV2.kt` | Remove per-portal theme logic (now handled at NavGraphV2 level) |
| `TeacherProfileScreenV2.kt` | Replace `ThemeCard` with shared `VThemePicker` |
| `SchoolPortalV2.kt` | Remove hardcoded `VPortalTone.Warm` (now handled at NavGraphV2 level) |
| `ParentPortalV2.kt` | Add `VThemePicker` to parent profile/settings |
| `MainActivity.kt` | Observe resolved theme's `isDark` for system bar styling |
| `App.kt` | Use `VThemeRegistry.defaultTheme` for splash colours |
| `Tables.kt` | Add `themePref` column to `AppUsersTable` |
| `UserDetailsRouting.kt` | Include `themePref` in user details response |
| 20 screen/component files | Replace hardcoded `Color(0x...)` with `VTheme.colors` tokens |

### 17.3 Files Preserved Unchanged

| File | Reason |
|---|---|
| `VType.kt` | Typography is theme-independent |
| `VDimens.kt` | Dimensions are theme-independent |
| `VElevation.kt` | Already adapts via `isNight` |
| `VMotion.kt` | Animation tokens are theme-independent |
| `EnrollTokens.kt` | Already reads from `VTheme.colors` — auto-adapts |
| `VAtoms.kt` | Already reads from `VTheme.colors` |
| `VCard.kt` | Already reads from `VTheme.colors` |
| `VInput.kt` | Already reads from `VTheme.colors` (1 hardcoded colour to fix) |

---

## 18. Example Directory Structure

```
composeApp/src/commonMain/kotlin/com/littlebridge/enrollplus/ui/v2/theme/
├── VColors.kt              # VColors data class + Raw values (existing, preserved)
├── VThemeDef.kt            # NEW: VThemeDef data class + VThemeMode enum
├── VThemeRegistry.kt       # NEW: All theme definitions + registry (THE single file to edit)
├── VTheme.kt               # MODIFIED: VTheme(themeDef=...) + LocalVThemeDef + M3 bridge
├── VType.kt                # Typography (existing, preserved)
├── VDimens.kt              # Dimensions (existing, preserved)
├── VElevation.kt           # Shadow system (existing, preserved)
├── VMotion.kt              # Animation tokens (existing, preserved)
└── EnrollTokens.kt         # Semantic bridge (existing, preserved)

composeApp/src/commonMain/kotlin/com/littlebridge/enrollplus/ui/v2/components/
├── VThemePicker.kt         # NEW: Shared theme picker component
├── VButton.kt              # MODIFIED: tonePalette reads from VTheme.colors
├── VCard.kt                # (existing, preserved)
├── VAtoms.kt               # (existing, preserved)
└── ... other components

shared/src/commonMain/kotlin/com/littlebridge/enrollplus/core/prefs/
└── PreferenceRepository.kt # MODIFIED: + getThemeMode/setThemeMode/getCustomThemeId/setCustomThemeId

shared/src/roomMain/kotlin/com/littlebridge/enrollplus/core/prefs/
└── PreferenceManager.kt    # MODIFIED: + new DataStore keys + migration

server/src/main/kotlin/com/littlebridge/enrollplus/db/
└── Tables.kt               # MODIFIED: + themePref column

docs/db/
└── migration_037_theme_pref.sql  # NEW: SQL migration
```

---

## 19. Implementation Roadmap

| Phase | Duration | Tasks | Breaking? |
|---|---|---|---|
| 1 | 1 day | Create `VThemeDef.kt` + `VThemeRegistry.kt` + `LocalVThemeDef` + Material 3 bridge | No |
| 2 | 1 day | Extend `PreferenceRepository` + implementations + migration | No |
| 3 | 0.5 day | Update `NavGraphV2` to resolve theme from preference | No |
| 4 | 1 day | Extract `VThemePicker` + add to parent/admin profiles | No |
| 5 | 2 days | Fix `VButton.tonePalette()` + audit 77 hardcoded colours | No |
| 6 | 1 day | Server: `theme_pref` column + API endpoint + client sync | No |
| 7 | 1 day | Status bar adaptation + animated transition + WCAG audit | No |
| 8 | 0.5 day | Deprecation cleanup (migrate `VPortalTone` references) | No |

**Total: ~8 days**

---

## 20. File-Level Impact Analysis

| File | Change Type | Lines Changed (est.) | Risk |
|---|---|---|---|
| `VThemeDef.kt` | **New** | ~30 | Low |
| `VThemeRegistry.kt` | **New** | ~80 | Low |
| `VThemePicker.kt` | **New** | ~80 | Low |
| `VTheme.kt` | Modify | ~40 | Medium (core file) |
| `VColors.kt` | Modify | ~10 (move instances to registry) | Low |
| `VButton.kt` | Modify | ~30 (tonePalette) | Medium |
| `PreferenceRepository.kt` | Modify | ~8 | Low |
| `PreferenceManager.kt` | Modify | ~20 | Low |
| `LocalStoragePreferenceManager.kt` (js) | Modify | ~15 | Low |
| `LocalStoragePreferenceManager.kt` (wasmJs) | Modify | ~15 | Low |
| `MainViewModel.kt` | Modify | ~10 | Low |
| `NavGraphV2.kt` | Modify | ~20 | Medium (nav root) |
| `TeacherPortalV2.kt` | Modify | ~10 (remove theme logic) | Low |
| `TeacherProfileScreenV2.kt` | Modify | ~15 (use VThemePicker) | Low |
| `SchoolPortalV2.kt` | Modify | ~5 (remove hardcoded tone) | Low |
| `ParentPortalV2.kt` | Modify | ~10 (add VThemePicker) | Low |
| `MainActivity.kt` | Modify | ~10 | Low |
| `App.kt` | Modify | ~5 | Low |
| `Tables.kt` | Modify | ~2 | Low |
| `UserDetailsRouting.kt` | Modify | ~10 | Low |
| `migration_037_theme_pref.sql` | **New** | ~3 | Low |
| 20 screen/component files | Modify | ~2–5 each (colour fixes) | Low |

---

## 21. Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Hardcoded colours missed in audit | High | Low | Automated grep CI check: `grep -rn "Color(0x" composeApp/src/commonMain --include="*.kt" \| grep -v VColors.kt \| grep -v EnrollTokens.kt` must return 0 |
| Contrast insufficient in custom themes | Medium | Medium | Automated WCAG contrast test per theme (§13.4) |
| Theme switch causes flash/flicker | Low | Low | `AnimatedContent` crossfade (§10.2) |
| `VPortalTone` deprecation breaks callers | Low | Medium | Backward-compat overload + `@Deprecated` annotation; migrate in Phase 8 |
| Server `theme_pref` migration fails | Low | High | Migration is additive (`ALTER TABLE ADD COLUMN` with default); test on staging |
| Theme preference conflicts between devices | Medium | Low | Server is source of truth on login; local preference wins if already set on device |
| `VButton.tonePalette` becomes `@Composable` breaks callers | Low | Low | `tonePalette` is `private` — only called inside `VButton` which is already `@Composable` |
| Dynamic colour (Android 12+) not available on other platforms | Low | Low | Conditional registration via `expect/actual`; "dynamic" theme only appears in picker on Android 12+ |
| Performance regression from Material 3 bridge | Low | Low | `ColorScheme` construction is a lightweight data class; only rebuilt on theme change |
