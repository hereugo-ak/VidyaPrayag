# Dynamic Theming Spec — School Branding → App Theme

> **Status:** Implementation spec for wiring saved school branding colors into the live app theme.
> **Depends on:** `SCHOOL_BRANDING_KIT_SPEC.md` (Phase 1–5 complete)

---

## 1. Objective

When a school admin saves branding colors via the Branding Kit screen, those colors should **immediately affect the app's appearance** for all users associated with that school — admin, teachers, and parents.

The app's accent colors, buttons, active states, headers, and highlights should reflect the school's brand identity.

---

## 2. Current State

- `VColors` defines the color token system (`accent`, `accentSoft`, `accentDeep`, `accentTint`, `teal`, `tealDeep`, etc.)
- `VTheme` provides colors via `CompositionLocal` — every screen reads from `VTheme.colors`
- `VThemeRegistry` has `registerDynamic()` for runtime themes, but it's unused
- `NavGraphV2` resolves theme from user preference (system/light/dark/custom)
- Branding colors are saved to DB and fetched via `BrandingApi`, but **never applied to the UI**

---

## 3. Design

### 3.1 Color Mapping

The school branding has three hex colors. They map to `VColors` tokens as follows:

| Branding Field | VColors Token | Rationale |
|---|---|---|
| `primaryColor` | `accent` | Main interactive color — buttons, active tabs, rings |
| `primaryColor` | `teal` | Legacy brand color used in headers/progress bars |
| `secondaryColor` | `accentDeep` | Deep step — text on accent tints, eyebrow labels |
| `secondaryColor` | `tealDeep` | Legacy deep brand color |
| `accentColor` | `accentSoft` | Soft step — gradients, hovers, secondary buttons |
| `primaryColor` (derived) | `accentTint` | 8% opacity of primary over white — card canvas behind branded elements |

### 3.2 Dark Mode Preservation

The school branding colors are applied **on top of** the resolved theme (light or dark). This means:
- If user is in dark mode → branding colors override accent family on `NightVColors`
- If user is in light mode → branding colors override accent family on `LightVColors`
- The user's theme mode preference is **never overridden** — branding only overrides colors, not the light/dark choice

### 3.3 Architecture

```
NavGraphV2
  ├── resolveThemeDef(mode, customId, role, isAuthenticated) → VThemeDef
  ├── BrandingThemeManager.schoolBrandingFlow → SchoolBranding?
  └── applyBrandingColors(def, branding) → VThemeDef (with overridden VColors)
        └── VTheme(themeDef = brandedDef) { ... all screens ... }
```

### 3.4 Components

#### `BrandingThemeManager` (shared module)

- Singleton scoped to app lifecycle
- Exposes `StateFlow<SchoolBranding?>` — null when no branding loaded
- `loadBranding(token)` — fetches branding from API, stores in StateFlow
- `clear()` — resets to null on logout
- Registered in Koin as singleton

#### `BrandingColorMapper` (composeApp module)

- Pure function: `applyBrandingColors(base: VColors, branding: SchoolBranding): VColors`
- Parses hex strings to `Color`
- Derives `accentTint` as `primaryColor.copy(alpha = 0.08f)` blended over white
- Returns `base.copy(accent = ..., accentSoft = ..., ...)` — only overrides accent family + teal

#### `NavGraphV2` wiring

- Inject `BrandingThemeManager`
- Observe `schoolBrandingFlow` via `collectAsState`
- When `isAuthenticated` becomes true, trigger `loadBranding(token)`
- When branding is available, call `applyBrandingColors(def.colors, branding)` and pass the modified `VThemeDef` to `VTheme`
- When branding is null (not yet loaded or no customization), use the unmodified `VThemeDef`

### 3.5 Lifecycle

| Event | Action |
|---|---|
| App launch (authenticated) | `BrandingThemeManager.loadBranding(token)` |
| Branding saved in Branding Kit screen | `loadBranding(token)` re-fetches → colors update live |
| Logout | `BrandingThemeManager.clear()` → colors revert to default |
| Theme mode change (light/dark) | Branding colors re-apply on new base palette |

### 3.6 Performance

- `BrandingThemeManager` is a singleton — branding is fetched once per session
- `applyBrandingColors` is a pure `.copy()` on `VColors` — no allocation concern
- `AnimatedContent` crossfade in `NavGraphV2` handles smooth transition when branding colors change
- No extra recomposition beyond normal theme change

---

## 4. Files to Create/Modify

| File | Action | Module |
|---|---|---|
| `shared/.../branding/presentation/BrandingThemeManager.kt` | **Create** | shared |
| `composeApp/.../v2/theme/BrandingColorMapper.kt` | **Create** | composeApp |
| `shared/.../di/Koin.kt` | **Modify** — register `BrandingThemeManager` | shared |
| `composeApp/.../v2/navigation/NavGraphV2.kt` | **Modify** — wire branding into theme resolution | composeApp |
| `composeApp/.../v2/screens/school/BrandingSettingsScreen.kt` | **Modify** — trigger re-fetch after save | composeApp |

---

## 5. Acceptance Criteria

1. School admin saves colors in Branding Kit → app accent colors change immediately
2. Teacher/parent logging in sees the school's branded colors
3. Dark mode still works — branding colors apply on dark base
4. Logout reverts to default theme
5. No crashes when branding is null or colors are invalid
6. No unnecessary recompositions or performance regressions
