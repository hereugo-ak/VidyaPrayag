# School Branding Kit — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** None
> **Source:** `DIFFERENTIATING_FEATURES.md` §7.2

---

## 1. Feature Overview

Per-school branding customization: logo, colors, fonts, custom app icon, login screen, email templates, and report card headers. Enables white-label experience where each school's app reflects their brand identity.

### Goals

- School admin uploads logo, sets primary/secondary colors
- App UI adapts: app icon, splash screen, login screen, header bar, buttons
- Email templates, report cards, newsletters use school branding
- ID cards, certificates use school branding
- Custom subdomain (school.vidyaprayag.com) for web app
- Branding preview before applying

---

## 2. Current System Assessment

- `SchoolsTable` has `name`, `logoUrl`, `board`, `mediumOfInstruction`
- `brand-assets/` directory has EnrollPlus branding (platform-level, not per-school)
- No per-school color/font customization
- `DIFFERENTIATING_FEATURES.md` §7.2: School Branding Kit, effort M

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Admin uploads: logo (PNG/SVG), favicon, app icon, splash screen image |
| FR-2 | Admin sets: primary color, secondary color, accent color (hex) |
| FR-3 | App UI dynamically uses school colors (VColors tokens overridden per school) |
| FR-4 | Login screen shows school logo + name |
| FR-5 | Email templates, report cards, newsletters, ID cards use school branding |
| FR-6 | Custom subdomain mapping (school.vidyaprayag.com) |
| FR-7 | Branding preview before applying |
| FR-8 | Default fallback to Vidya Prayag branding if school hasn't customized |

---

## 4. Database Design

### 4.1 New Table: `school_branding`

```sql
CREATE TABLE school_branding (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL UNIQUE,
    logo_url        TEXT,                          -- Supabase Storage URL
    logo_dark_url   TEXT,                          -- dark mode logo variant
    favicon_url     TEXT,
    app_icon_url    TEXT,                          -- for home screen icon
    splash_screen_url TEXT,
    primary_color   VARCHAR(8) DEFAULT '#2563EB',  -- hex
    secondary_color VARCHAR(8) DEFAULT '#1E40AF',
    accent_color    VARCHAR(8) DEFAULT '#3B82F6',
    custom_subdomain TEXT,                         -- "dpsrkpuram"
    login_background_url TEXT,
    is_customized   BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);
```

---

## 5. Backend Architecture

### 5.1 BrandingService

```kotlin
class BrandingService {
    suspend fun getBranding(schoolId: UUID): SchoolBrandingDto
    suspend fun updateBranding(schoolId: UUID, request: UpdateBrandingRequest): SchoolBrandingDto
    suspend fun uploadAsset(schoolId: UUID, assetType: String, file: ByteArray): String  // returns URL
    suspend fun checkSubdomainAvailable(subdomain: String): Boolean
}
```

### 5.2 Branding Resolution on Login

After user login, JWT contains `school_id`. Client fetches branding:
```
GET /api/v1/branding/{schoolId}
```

Client applies branding to VColors:
```kotlin
class BrandingManager {
    fun applyBranding(branding: SchoolBrandingDto) {
        VColors.lightPrimary = Color(branding.primaryColor.hexToInt())
        VColors.lightSecondary = Color(branding.secondaryColor.hexToInt())
        // ... override all tokens
    }
}
```

---

## 6. API Contracts

```
# Admin
GET /api/v1/school/branding
PATCH /api/v1/school/branding  { primary_color, secondary_color, accent_color }
POST /api/v1/school/branding/upload  { asset_type: "logo", file: <multipart> }
POST /api/v1/school/branding/subdomain  { subdomain: "dpsrkpuram" }

# Public (no auth, for login screen)
GET /api/v1/branding/{schoolId}
GET /api/v1/branding/subdomain/{subdomain}  -- resolve school from subdomain
```

---

## 7. Frontend Architecture

### 7.1 Dynamic Theming

```kotlin
@Composable
fun VidyaPrayagTheme(
    branding: SchoolBrandingDto?,
    content: @Composable () -> Unit
) {
    val colorScheme = if (branding != null && branding.isCustomized) {
        buildCustomColorScheme(branding)
    } else {
        defaultColorScheme
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
```

### 7.2 App Icon (Android)

Android supports dynamic app icons via `<activity-alias>` in AndroidManifest. However, changing app icon dynamically requires user interaction on some launchers. Alternative: use adaptive icons with school logo as foreground.

### 7.3 Subdomain Routing (Web)

Web app resolves school from subdomain:
- `dpsrkpuram.vidyaprayag.com` → fetch branding for school with subdomain "dpsrkpuram"
- Show school-specific login screen

---

## 8. Acceptance Criteria

- [ ] Admin uploads logo, favicon, app icon, splash screen
- [ ] Admin sets primary/secondary/accent colors
- [ ] App UI dynamically uses school colors
- [ ] Login screen shows school logo + name
- [ ] Email templates, report cards use school branding
- [ ] Custom subdomain works for web app
- [ ] Branding preview available
- [ ] Default fallback when not customized

---

## 9. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration, Exposed table |
| 2 | 2 days | BrandingService (CRUD, asset upload, subdomain) |
| 3 | 2 days | Dynamic theming in VColors/VTheme |
| 4 | 2 days | Login screen + splash screen branding |
| 5 | 2 days | Email/report card/newsletter branding integration |
| 6 | 2 days | Client UI (branding settings, preview, upload) |
| 7 | 1 day | Subdomain routing (web) |
| 8 | 1 day | Tests |

---

## 10. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | `SchoolBrandingTable` |
| `server/.../feature/branding/BrandingService.kt` | New | Core service |
| `server/.../feature/branding/BrandingRouting.kt` | New | API endpoints |
| `docs/db/migration_075_school_branding.sql` | New | DDL |
| `shared/.../core/branding/BrandingManager.kt` | New | Client branding application |
| `composeApp/.../ui/v2/theme/VTheme.kt` | Modify | Dynamic theming support |
| `composeApp/.../ui/v2/screens/auth/LoginScreen.kt` | Modify | School logo + name |
| `composeApp/.../ui/v2/screens/admin/BrandingSettingsScreen.kt` | New | Branding management UI |
