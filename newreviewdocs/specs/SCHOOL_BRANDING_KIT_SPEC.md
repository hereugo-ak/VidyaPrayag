# School Branding Kit — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** None
> **Source:** `DIFFERENTIATING_FEATURES.md` §7.2
> **Template:** `_SPEC_TEMPLATE.md` v1 (25 mandatory + 6 optional sections)

---

## 1. Feature Overview

### What

Per-school branding customization: logo, colors, fonts, custom app icon, login screen, email templates, and report card headers. Enables white-label experience where each school's app reflects their brand identity.

### Why — Product Rationale

Schools want their app to reflect their brand identity — logo, colors, name. A white-labeled app increases school's sense of ownership and trust with parents. Without branding, all schools see the same Vidya Prayag branding, which feels generic.

This is a **differentiating feature** (Priority P2, Phase 2, effort M, "Low" value per `DIFFERENTIATING_FEATURES.md`). It's lower priority than core features but important for school retention and premium positioning.

### What Stands Out (Competitive Moat)

From `DIFFERENTIATING_FEATURES.md` §7.2:
> "School Branding Kit — logo, colors, fonts, custom app icon, login screen, email templates, report card headers. White-label experience."

Most school ERPs offer some branding (logo upload), but few offer full white-label with dynamic theming, custom subdomains, and branded communications.

### Goals

- School admin uploads logo, sets primary/secondary colors
- App UI adapts: app icon, splash screen, login screen, header bar, buttons
- Email templates, report cards, newsletters use school branding
- ID cards, certificates use school branding
- Custom subdomain (school.vidyaprayag.com) for web app
- Branding preview before applying

### Non-goals

- [ ] Custom fonts (system fonts used initially)
- [ ] Per-user themes (branding is per-school, not per-user)
- [ ] Dark mode logo variants (future enhancement)
- [ ] Custom domain (e.g., app.dpsrkpuram.com) — only subdomain
- [ ] Multi-language branding (branding is language-agnostic)
- [ ] Branded push notification icons

### Dependencies

- `SchoolsTable` — existing `name`, `logoUrl`, `board`, `mediumOfInstruction` fields
- Supabase Storage — for brand asset uploads (logo, favicon, app icon, splash)
- `VColors` / `VTheme` — existing theming system (modified for dynamic override)
- Email template system — for branded emails
- Report card generator — for branded report cards

### Related Modules

- `server/.../feature/branding/` — new branding module
- `shared/.../core/branding/` — new client branding manager
- `composeApp/.../ui/v2/theme/` — modified for dynamic theming
- `composeApp/.../ui/v2/screens/auth/` — modified login screen
- `composeApp/.../ui/v2/screens/admin/` — new branding settings screen

---

## 2. Current System Assessment

### Existing Code

- `SchoolsTable` has `name`, `logoUrl`, `board`, `mediumOfInstruction`
- `brand-assets/` directory has EnrollPlus branding (platform-level, not per-school)
- No per-school color/font customization
- `DIFFERENTIATING_FEATURES.md` §7.2: School Branding Kit, effort M

### Existing Database

- `SchoolsTable` — has `name`, `logoUrl` (existing but unused for dynamic theming)
- No `school_branding` table exists

### Existing APIs

- No branding API endpoints exist
- School info available via existing school management APIs

### Existing UI

- `VColors` / `VTheme` — hardcoded Vidya Prayag colors
- Login screen — generic Vidya Prayag branding
- No dynamic theming support

### Existing Services

- No branding service exists
- Email templates use Vidya Prayag branding
- Report cards use Vidya Prayag header

### Existing Documentation

- `DIFFERENTIATING_FEATURES.md` §7.2 — School Branding Kit

### Technical Debt

| # | Gap | Details |
|---|---|---|
| TD-1 | No per-school branding table | No `school_branding` table for colors, assets |
| TD-2 | Hardcoded theme | `VColors` hardcoded — no dynamic override |
| TD-3 | Generic login screen | No school-specific login screen |
| TD-4 | No asset upload | No branding asset upload to Supabase Storage |
| TD-5 | No subdomain routing | Web app doesn't resolve school from subdomain |
| TD-6 | Generic email/report templates | No school branding in communications |

### Gaps

| # | Gap | Impact | Severity |
|---|---|---|---|
| G1 | No dynamic theming | All schools look the same | **Medium** |
| G2 | Generic login screen | No school identity at login | **Medium** |
| G3 | No branded communications | Emails/reports look generic | **Medium** |
| G4 | No subdomain routing | Schools can't have custom URL | **Low** |
| G5 | No branding management UI | Admin can't customize branding | **Medium** |

---

## 3. Functional Requirements

### FR-001
| Field | Value |
|---|---|
| **Title** | Brand Asset Upload |
| **Description** | Admin uploads: logo (PNG/SVG), favicon, app icon, splash screen image. |
| **Priority** | High |
| **User Roles** | School Admin |
| **Acceptance notes** | Assets uploaded to Supabase Storage. URLs stored in `school_branding`. Logo: max 1MB, PNG/SVG. App icon: 512x512px. Splash: 1080x1920px. |

### FR-002
| Field | Value |
|---|---|
| **Title** | Color Customization |
| **Description** | Admin sets: primary color, secondary color, accent color (hex). |
| **Priority** | High |
| **User Roles** | School Admin |
| **Acceptance notes** | Hex color codes (e.g., #2563EB). Applied to VColors tokens. Preview before applying. |

### FR-003
| Field | Value |
|---|---|
| **Title** | Dynamic Theming |
| **Description** | App UI dynamically uses school colors (VColors tokens overridden per school). |
| **Priority** | Critical |
| **User Roles** | System |
| **Acceptance notes** | `BrandingManager.applyBranding()` overrides VColors. MaterialTheme uses custom color scheme. |

### FR-004
| Field | Value |
|---|---|
| **Title** | Branded Login Screen |
| **Description** | Login screen shows school logo + name. |
| **Priority** | High |
| **User Roles** | Parent, Teacher, School Admin |
| **Acceptance notes** | School logo displayed above login form. School name shown. Fallback to Vidya Prayag logo if not customized. |

### FR-005
| Field | Value |
|---|---|
| **Title** | Branded Communications |
| **Description** | Email templates, report cards, newsletters, ID cards use school branding. |
| **Priority** | Medium |
| **User Roles** | System |
| **Acceptance notes** | Email headers use school logo + colors. Report cards use school header. ID cards use school logo. |

### FR-006
| Field | Value |
|---|---|
| **Title** | Custom Subdomain |
| **Description** | Custom subdomain mapping (school.vidyaprayag.com). |
| **Priority** | Low |
| **User Roles** | School Admin |
| **Acceptance notes** | Admin sets subdomain (e.g., "dpsrkpuram"). Web app resolves school from subdomain. Subdomain unique across platform. |

### FR-007
| Field | Value |
|---|---|
| **Title** | Branding Preview |
| **Description** | Branding preview before applying. |
| **Priority** | Medium |
| **User Roles** | School Admin |
| **Acceptance notes** | Admin sees preview of login screen, header, buttons with selected colors/logo before saving. |

### FR-008
| Field | Value |
|---|---|
| **Title** | Default Fallback |
| **Description** | Default fallback to Vidya Prayag branding if school hasn't customized. |
| **Priority** | Critical |
| **User Roles** | System |
| **Acceptance notes** | `is_customized = false` → use default Vidya Prayag colors/logo. No blank or broken UI. |

### Non-Functional Requirements

| ID | Requirement |
|---|---|
| NFR-1 | Branding API responds in < 500ms |
| NFR-2 | Branding applied to UI in < 200ms (no visible flash) |
| NFR-3 | Logo image max 1MB, WebP/PNG/SVG |
| NFR-4 | App icon 512x512px, PNG |
| NFR-5 | Splash screen 1080x1920px, PNG/WebP |
| NFR-6 | Branding cached client-side for app session |
| NFR-7 | Subdomain resolution in < 100ms |

---

## 4. User Stories

### School Admin
- [ ] Upload school logo, favicon, app icon, splash screen
- [ ] Set primary, secondary, and accent colors
- [ ] Preview branding before applying
- [ ] Set custom subdomain for web app
- [ ] See branding applied across app (login, header, buttons, emails)

### Parent
- [ ] See school logo and name on login screen
- [ ] See school colors throughout the app
- [ ] Receive branded emails and report cards
- [ ] Access app via school's custom subdomain (web)

### Teacher
- [ ] See school branding throughout the app
- [ ] Receive branded communications

### System
- [ ] Fetch branding on login and apply to UI
- [ ] Use default Vidya Prayag branding if school hasn't customized
- [ ] Resolve school from subdomain for web app
- [ ] Apply school branding to email templates, report cards, ID cards

---

## 5. Business Rules

### BR-001
**Rule:** Branding is per-school, not per-user.
**Enforcement:** `school_branding.school_id` — one branding config per school. All users in school see same branding.

### BR-002
**Rule:** Default fallback to Vidya Prayag branding.
**Enforcement:** If `school_branding` row doesn't exist or `is_customized = false`, use default colors (#2563EB, #1E40AF, #3B82F6) and default logo.

### BR-003
**Rule:** Subdomain must be unique across platform.
**Enforcement:** `school_branding.custom_subdomain` — checked for uniqueness before assignment. Return 409 if already taken.

### BR-004
**Rule:** Branding applied at app launch, not per-screen.
**Enforcement:** `BrandingManager.applyBranding()` called once after login. VColors tokens overridden globally. All screens use overridden colors.

### BR-005
**Rule:** Branding preview before applying.
**Enforcement:** Admin sees live preview in branding settings screen. Changes saved only on "Apply" button click. Preview is client-side only.

### BR-006
**Rule:** Branding assets stored in Supabase Storage.
**Enforcement:** Logos, icons, splash screens uploaded to Supabase Storage bucket `school-branding`. URLs stored in `school_branding` table.

### BR-007
**Rule:** Subdomain format: lowercase alphanumeric + hyphens.
**Enforcement:** Regex validation: `^[a-z0-9][a-z0-9-]{2,30}[a-z0-9]$`. Min 4, max 32 characters. No leading/trailing hyphens.

### BR-008
**Rule:** Branding cached for app session.
**Enforcement:** Branding fetched once after login, cached in memory for app session. Re-fetched on app restart. No real-time branding updates.

---

## 6. Database Design

### 6.1 Entity Relationship Summary

One new table: `school_branding` (1:1 with `schools`). Stores brand assets (URLs), colors, and subdomain per school.

### 6.2 New Tables

#### `school_branding` table

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

### 6.3 Modified Tables

N/A — no existing tables modified. `SchoolsTable.logoUrl` remains but is superseded by `school_branding.logo_url`.

### 6.4 Indexes

- `school_branding(school_id)` — UNIQUE, for school lookup
- `school_branding(custom_subdomain)` — for subdomain resolution (unique constraint via application logic)

### 6.5 Constraints

- `school_branding.school_id` — NOT NULL, UNIQUE
- `school_branding.primary_color` — VARCHAR(8), default '#2563EB'
- `school_branding.secondary_color` — VARCHAR(8), default '#1E40AF'
- `school_branding.accent_color` — VARCHAR(8), default '#3B82F6'
- `school_branding.is_customized` — NOT NULL, default false
- `school_branding.custom_subdomain` — nullable, unique across non-null values

### 6.6 Foreign Keys

- `school_branding.school_id` → `schools.id` (implicit)

### 6.7 Soft Delete Strategy

N/A — branding row is never deleted. If school wants to revert, set `is_customized = false` and reset colors to defaults.

### 6.8 Audit Fields

- `school_branding.created_at` — when branding row created
- `school_branding.updated_at` — when branding last updated

### 6.9 Migration Notes

Migration: `docs/db/migration_075_school_branding.sql`
- CREATE `school_branding` table
- No data migration (new feature)
- Existing `SchoolsTable.logoUrl` not migrated (superseded by `school_branding.logo_url`)

### 6.10 Exposed Mappings

```kotlin
object SchoolBrandingTable : UUIDTable("school_branding", "id") {
    val schoolId          = uuid("school_id").unique()
    val logoUrl           = text("logo_url").nullable()
    val logoDarkUrl       = text("logo_dark_url").nullable()
    val faviconUrl        = text("favicon_url").nullable()
    val appIconUrl        = text("app_icon_url").nullable()
    val splashScreenUrl   = text("splash_screen_url").nullable()
    val primaryColor      = varchar("primary_color", 8).default("#2563EB")
    val secondaryColor    = varchar("secondary_color", 8).default("#1E40AF")
    val accentColor       = varchar("accent_color", 8).default("#3B82F6")
    val customSubdomain   = text("custom_subdomain").nullable()
    val loginBackgroundUrl = text("login_background_url").nullable()
    val isCustomized      = bool("is_customized").default(false)
    val createdAt         = timestamp("created_at")
    val updatedAt         = timestamp("updated_at")
}
```

Register in `DatabaseFactory.allTables`.

### 6.11 Seed Data

N/A — branding created by school admin. Default row can be auto-created on first school setup.

---

## 7. State Machines

### Branding Customization State Machine

```
not_customized ──admin_sets_colors──> preview ──admin_applies──> customized
  │                                      │
  └──admin_uploads_logo──>               │──admin_cancels──> not_customized
     preview                             │
                                         └──admin_applies──> customized
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `not_customized` | Admin sets colors/logo | `preview` | Admin in branding settings |
| `preview` | Admin clicks "Apply" | `customized` | `is_customized = true`, save to DB |
| `preview` | Admin clicks "Cancel" | `not_customized` | Discard changes |
| `customized` | Admin updates branding | `preview` | Admin in branding settings |
| `customized` | Admin resets to default | `not_customized` | `is_customized = false`, reset colors |

### Subdomain State Machine

```
no_subdomain ──admin_enters_subdomain──> checking ──available──> assigned
  │                                         │
  └──admin_removes_subdomain──>             │──taken──> no_subdomain (error)
  no_subdomain                              │
                                             └──invalid_format──> no_subdomain (error)
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `no_subdomain` | Admin enters subdomain | `checking` | Non-empty input |
| `checking` | Subdomain available | `assigned` | Unique across platform |
| `checking` | Subdomain taken | `no_subdomain` | Return error "Subdomain already taken" |
| `checking` | Invalid format | `no_subdomain` | Return error "Invalid subdomain format" |
| `assigned` | Admin removes subdomain | `no_subdomain` | Set `custom_subdomain = null` |

### Branding Resolution State Machine (Client)

```
app_launch ──fetch_branding──> fetching ──success──> applied ──app_session──> applied
  │                               │
  └──no_school_id──>              │──failure──> default_branding
     default_branding             │
                                   └──not_customized──> default_branding
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `app_launch` | User logged in, has school_id | `fetching` | JWT contains school_id |
| `app_launch` | No school_id (pre-login) | `default_branding` | Use Vidya Prayag defaults |
| `fetching` | Branding fetched, `is_customized = true` | `applied` | Apply custom colors/logo |
| `fetching` | Branding fetched, `is_customized = false` | `default_branding` | Use default colors/logo |
| `fetching` | Fetch failed | `default_branding` | Fallback to defaults |
| `applied` | App session continues | `applied` | Branding persists for session |

---

## 8. Backend Architecture

### 8.1 Component Overview

`BrandingService` handles branding CRUD, asset uploads, and subdomain management. Branding fetched by client after login and applied to UI theming. Public endpoints allow pre-login branding resolution (for login screen and subdomain routing).

### 8.2 Design Principles

1. **Default fallback** — always works with Vidya Prayag defaults, even if school hasn't customized
2. **Public read, admin write** — branding read is public (no auth), write is admin-only
3. **Cache for session** — branding fetched once per app session, not per screen
4. **Assets in Supabase Storage** — logos/icons stored as files, URLs in DB
5. **Subdomain unique** — custom subdomain unique across entire platform

### 8.3 Core Types

#### BrandingService

```kotlin
class BrandingService {
    suspend fun getBranding(schoolId: UUID): SchoolBrandingDto
    suspend fun updateBranding(schoolId: UUID, request: UpdateBrandingRequest): SchoolBrandingDto
    suspend fun uploadAsset(schoolId: UUID, assetType: String, file: ByteArray): String  // returns URL
    suspend fun checkSubdomainAvailable(subdomain: String): Boolean
    suspend fun resolveSubdomain(subdomain: String): SchoolBrandingDto
}
```

### 8.4 Repositories

- `BrandingRepository` — CRUD for `school_branding` table

### 8.5 Mappers

- `BrandingMapper` — maps `school_branding` rows to `SchoolBrandingDto`

### 8.6 Permission Checks

- `getBranding` — public (no auth), for login screen and subdomain resolution
- `updateBranding` — School Admin only
- `uploadAsset` — School Admin only
- `checkSubdomainAvailable` — School Admin only
- `resolveSubdomain` — public (no auth), for web app subdomain routing

### 8.7 Background Jobs

N/A — no background jobs. Branding is on-demand read/write.

### 8.8 Domain Events

- `BrandingUpdated` — emitted when admin updates branding (colors, logo, assets)
- `SubdomainAssigned` — emitted when admin sets custom subdomain
- `SubdomainRemoved` — emitted when admin removes custom subdomain
- `BrandingAssetUploaded` — emitted when admin uploads a brand asset

### 8.9 Caching

- Server-side: branding cached per school, 10-minute TTL
- Client-side: branding cached in memory for app session (no TTL — persists until app restart)
- Subdomain resolution: cached per subdomain, 1-hour TTL

### 8.10 Transactions

- Branding update: single transaction (update `school_branding` row)
- Asset upload: upload to Supabase Storage (no transaction), then update DB URL (single transaction)
- Subdomain assignment: single transaction (update `custom_subdomain`)

### 8.11 Rate Limiting

- Branding read: no rate limiting (public, cached)
- Branding update: 10 updates per school per hour
- Asset upload: 20 uploads per school per hour
- Subdomain check: 30 checks per school per hour

### 8.12 Configuration

- `BRANDING_ENABLED` — default `true`; enable/disable feature
- `BRANDING_CACHE_TTL_SECONDS` — default `600` (10 minutes)
- `BRANDING_MAX_LOGO_SIZE_KB` — default `1024` (1MB)
- `BRANDING_MAX_ICON_SIZE_KB` — default `512`
- `BRANDING_MAX_SPLASH_SIZE_KB` — default `2048` (2MB)
- `BRANDING_SUBDOMAIN_MIN_LENGTH` — default `4`
- `BRANDING_SUBDOMAIN_MAX_LENGTH` — default `32`
- `BRANDING_DEFAULT_PRIMARY_COLOR` — default `#2563EB`
- `BRANDING_DEFAULT_SECONDARY_COLOR` — default `#1E40AF`
- `BRANDING_DEFAULT_ACCENT_COLOR` — default `#3B82F6`

---

## 9. API Contracts

### 9.1 Admin Endpoints

```
GET /api/v1/school/branding
  → 200: SchoolBrandingDto

PATCH /api/v1/school/branding
  Body: { primary_color: "#2563EB", secondary_color: "#1E40AF", accent_color: "#3B82F6" }
  → 200: SchoolBrandingDto

POST /api/v1/school/branding/upload
  Body: multipart { asset_type: "logo", file: <binary> }
  → 200: { url: "https://supabase.url/logo.webp" }

POST /api/v1/school/branding/subdomain
  Body: { subdomain: "dpsrkpuram" }
  → 200: { subdomain: "dpsrkpuram" }
  → 409: Subdomain already taken
  → 400: Invalid subdomain format
```

### 9.2 Public Endpoints

```
GET /api/v1/branding/{schoolId}
  → 200: SchoolBrandingDto
  → 404: School not found

GET /api/v1/branding/subdomain/{subdomain}
  → 200: { schoolId: "uuid", schoolName: "DPS R.K. Puram", branding: SchoolBrandingDto }
  → 404: Subdomain not found
```

### 9.3 DTO Models

All `@Serializable`, wrapped in `ApiResponse<T>` pattern.

```kotlin
@Serializable data class SchoolBrandingDto(
    val schoolId: String,
    val schoolName: String,
    val logoUrl: String?,
    val logoDarkUrl: String?,
    val faviconUrl: String?,
    val appIconUrl: String?,
    val splashScreenUrl: String?,
    val primaryColor: String,    // hex: "#2563EB"
    val secondaryColor: String,  // hex: "#1E40AF"
    val accentColor: String,     // hex: "#3B82F6"
    val customSubdomain: String?,
    val loginBackgroundUrl: String?,
    val isCustomized: Boolean,
)

@Serializable data class UpdateBrandingRequest(
    val primaryColor: String? = null,
    val secondaryColor: String? = null,
    val accentColor: String? = null,
    val loginBackgroundUrl: String? = null,
)

@Serializable data class SubdomainRequest(
    val subdomain: String,
)

@Serializable data class SubdomainResponse(
    val subdomain: String,
)
```

---

## 10. Frontend Architecture

### 10.1 Screens

| Screen | Platform | Role | Description |
|---|---|---|---|
| `LoginScreen` (modified) | Compose | All | Show school logo + name, use school colors |
| `BrandingSettingsScreen` | Compose | School Admin | Branding management: upload assets, set colors, preview, subdomain |
| `SplashScreen` (modified) | Compose | All | Show school splash screen image |

### 10.2 Navigation

- Admin: Admin tab → Settings → Branding → Branding Settings
- Login: Pre-login → branding fetched → school-specific login screen

### 10.3 UX Flows

#### Admin: Configure Branding
1. Admin opens Branding Settings
2. Sees current branding (logo, colors, subdomain)
3. Uploads new logo → sees preview
4. Sets primary/secondary/accent colors → sees live preview
5. Clicks "Apply" → branding saved
6. Optionally sets subdomain → checks availability → saves

#### User: Login with School Branding
1. User opens app (or visits subdomain URL)
2. App fetches branding for school
3. Login screen shows school logo, name, and colors
4. User logs in → branding applied throughout app

### 10.4 State Management

```kotlin
data class BrandingState(
    val branding: SchoolBrandingDto?,
    val isLoading: Boolean,
    val error: String?,
)

data class BrandingSettingsState(
    val currentBranding: SchoolBrandingDto,
    val previewPrimaryColor: String,
    val previewSecondaryColor: String,
    val previewAccentColor: String,
    val previewLogoUrl: String?,
    val subdomainInput: String,
    val subdomainAvailable: Boolean?,
    val isSaving: Boolean,
    val error: String?,
)
```

### 10.5 Dynamic Theming

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

### 10.6 Branding Manager

```kotlin
class BrandingManager {
    fun applyBranding(branding: SchoolBrandingDto) {
        VColors.lightPrimary = Color(branding.primaryColor.hexToInt())
        VColors.lightSecondary = Color(branding.secondaryColor.hexToInt())
        // ... override all tokens
    }
}
```

### 10.7 App Icon (Android)

Android supports dynamic app icons via `<activity-alias>` in AndroidManifest. However, changing app icon dynamically requires user interaction on some launchers. Alternative: use adaptive icons with school logo as foreground.

### 10.8 Subdomain Routing (Web)

Web app resolves school from subdomain:
- `dpsrkpuram.vidyaprayag.com` → fetch branding for school with subdomain "dpsrkpuram"
- Show school-specific login screen

### 10.9 Offline Support

- Branding cached in memory for app session
- If branding fetch fails, use default Vidya Prayag branding
- No offline branding management (requires online to save)

### 10.10 Loading States

- Branding fetch: no loading state (use defaults until fetched, then apply)
- Asset upload: "Uploading logo..."
- Subdomain check: "Checking availability..."
- Save: "Saving branding..."

### 10.11 Error Handling (UI)

- Branding fetch failure: silent fallback to defaults
- Asset upload failure: "Failed to upload. Please try again."
- Subdomain taken: "Subdomain already taken. Try another."
- Subdomain invalid: "Invalid subdomain. Use lowercase letters, numbers, and hyphens."
- Save failure: "Failed to save branding. Please try again."

### 10.12 Component Integration Guidelines

| Rule | Description |
|---|---|
| **R1** | `VidyaPrayagTheme` wraps entire app with branding-aware color scheme |
| **R2** | Login screen shows school logo (or default) above form |
| **R3** | Color picker in branding settings with live preview |
| **R4** | Logo upload with image picker and crop/resize |
| **R5** | Subdomain input with real-time availability check |
| **R6** | Preview shows login screen, header, and button with selected branding |
| **R7** | "Apply" button saves branding; "Cancel" discards changes |
| **R8** | SplashScreen shows school splash image (or default) |
| **R9** | All UI components use VColors tokens (not hardcoded colors) |
| **R10** | Email/report templates use school branding (server-side rendering) |

---

## 11. Shared Module Changes (KMP)

### 11.1 DTOs

All DTOs defined in section 9.3, placed in `shared/.../branding/domain/model/BrandingModels.kt`.

### 11.2 Domain Models

```kotlin
data class SchoolBranding(
    val schoolId: String,
    val schoolName: String,
    val logoUrl: String?,
    val primaryColor: String,
    val secondaryColor: String,
    val accentColor: String,
    val customSubdomain: String?,
    val isCustomized: Boolean,
)

object DefaultBranding {
    val PRIMARY_COLOR = "#2563EB"
    val SECONDARY_COLOR = "#1E40AF"
    val ACCENT_COLOR = "#3B82F6"
}
```

### 11.3 Repository Interfaces

```kotlin
interface BrandingRepository {
    suspend fun getBranding(token: String): NetworkResult<SchoolBrandingDto>
    suspend fun updateBranding(token: String, request: UpdateBrandingRequest): NetworkResult<SchoolBrandingDto>
    suspend fun uploadAsset(token: String, assetType: String, file: ByteArray): NetworkResult<String>
    suspend fun checkSubdomain(token: String, subdomain: String): NetworkResult<Boolean>
    suspend fun getPublicBranding(schoolId: String): NetworkResult<SchoolBrandingDto>
    suspend fun resolveSubdomain(subdomain: String): NetworkResult<SubdomainResolutionDto>
}
```

### 11.4 UseCases

- `GetBrandingUseCase`
- `UpdateBrandingUseCase`
- `UploadBrandingAssetUseCase`
- `CheckSubdomainUseCase`
- `ResolveSubdomainUseCase`
- `ApplyBrandingUseCase` (client-side, applies to VColors)

### 11.5 Validation

- Colors: valid hex format `^#[0-9A-Fa-f]{6}$`
- Subdomain: `^[a-z0-9][a-z0-9-]{2,30}[a-z0-9]$`, min 4, max 32 chars
- Logo file: PNG/SVG/WebP, max 1MB
- App icon: PNG, 512x512px, max 512KB
- Splash screen: PNG/WebP, 1080x1920px, max 2MB

### 11.6 Serialization

Standard Kotlinx serialization. Colors serialized as hex strings.

### 11.7 Network APIs

Ktor `@Resource` route definitions in `BrandingApi.kt`:
- GET/PATCH `/api/v1/school/branding`
- POST `/api/v1/school/branding/upload`
- POST `/api/v1/school/branding/subdomain`
- GET `/api/v1/branding/{schoolId}` (public)
- GET `/api/v1/branding/subdomain/{subdomain}` (public)

### 11.8 Database Models (Local Cache)

- Branding cached in DataStore as JSON (for app session persistence)
- Cache key: `branding:{schoolId}`

---

## 12. Permissions Matrix

| Action | Super Admin | School Admin | Teacher | Parent |
|---|---|---|---|---|
| View school branding (public) | ✅ | ✅ | ✅ | ✅ |
| Update branding colors | ✅ | ✅ | ❌ | ❌ |
| Upload brand assets | ✅ | ✅ | ❌ | ❌ |
| Set custom subdomain | ✅ | ✅ | ❌ | ❌ |
| Reset branding to default | ✅ | ✅ | ❌ | ❌ |
| View all schools' branding | ✅ | ❌ | ❌ | ❌ |

---

## 13. Notifications

N/A — branding changes don't trigger notifications. Branding is silently applied on next app launch.

---

## 14. Background Jobs

N/A — no background jobs. Branding is on-demand read/write.

---

## 15. Integrations

### Internal Integrations

| System | Integration Point | Direction | Protocol | Error Handling |
|---|---|---|---|---|
| `SchoolsTable` | School name | Read | Direct DB | Use "Unknown School" if not found |
| Supabase Storage | Brand asset storage | Upload/Read | HTTP API | Log on failure |
| `VColors` / `VTheme` | Dynamic theming | Write (client) | Direct call | Fallback to defaults |
| Email template system | Branded emails | Read | Direct call | Use default branding |
| Report card generator | Branded report cards | Read | Direct call | Use default branding |
| ID card generator | Branded ID cards | Read | Direct call | Use default branding |

### External Integrations

| System | Purpose | Direction | Protocol | Authentication | Error Handling |
|---|---|---|---|---|---|
| Supabase Storage | Brand asset hosting | Outbound | HTTP API | Service key (existing) | Log on failure |

### Integration Patterns

- **Supabase Storage:** Admin uploads asset → Supabase URL returned → URL stored in `school_branding`
- **Dynamic theming:** `BrandingManager.applyBranding()` overrides `VColors` tokens after login
- **Email/report branding:** Server-side template rendering fetches `school_branding` for school-specific headers
- **Subdomain routing:** Web app middleware resolves subdomain → school_id → branding

---

## 16. Security

### Authentication

- Admin endpoints: JWT auth via `requireAuth()`, school admin role
- Public endpoints (`GET /branding/{schoolId}`, `GET /branding/subdomain/{subdomain}`): no auth

### Authorization

- School admin can only update branding for own school
- Super admin can update branding for any school
- Public read access for branding (needed for login screen)

### Data Protection

- Brand assets — public (logos, icons visible to all users)
- Colors — public (visible in UI)
- Subdomain — public (visible in URL)
- No PII in branding data

### Input Validation

- Colors: valid hex format `^#[0-9A-Fa-f]{6}$`
- Subdomain: `^[a-z0-9][a-z0-9-]{2,30}[a-z0-9]$`
- Logo: PNG/SVG/WebP, max 1MB
- App icon: PNG, 512x512px, max 512KB
- Splash: PNG/WebP, 1080x1920px, max 2MB

### Rate Limiting

- Branding read: no rate limiting (public, cached)
- Branding update: 10 per school per hour
- Asset upload: 20 per school per hour
- Subdomain check: 30 per school per hour

### Audit Logging

- Branding updated: admin ID, school ID, changes (colors, assets)
- Subdomain assigned/removed: admin ID, school ID, subdomain
- Asset uploaded: admin ID, school ID, asset type, URL

### PII Handling

- No PII in branding data
- School name is public information
- Brand assets are public (logos, icons)

### Multi-tenant Isolation

- `school_branding.school_id` — school-scoped
- Admin can only update own school's branding
- Public read access doesn't expose cross-school data (only requested school's branding)

---

## 17. Performance & Scalability

### Expected Scale

- 1 branding row per school
- 1-5 brand assets per school (logo, favicon, icon, splash, login background)
- Branding fetched once per app session per user

### Query Optimization

- `school_branding(school_id)` — UNIQUE index, O(1) lookup
- `school_branding(custom_subdomain)` — indexed for subdomain resolution

### Indexing Strategy

- `school_branding(school_id)` — UNIQUE, for school lookup
- `school_branding(custom_subdomain)` — for subdomain resolution

### Caching Strategy

- Server-side: branding cached per school, 10-minute TTL
- Client-side: branding cached in memory for app session
- Subdomain resolution: cached per subdomain, 1-hour TTL

### Pagination

N/A — single branding row per school.

### Connection Pooling

Uses existing HikariCP connection pool. No additional pooling needed.

### Async Processing

- Branding fetch: synchronous (with caching)
- Asset upload: async (Supabase Storage upload)
- Branding apply: synchronous (client-side VColors override)

### Scalability Concerns

- Branding fetch volume: 1 per user per app session. With 10,000 users, ~10,000 fetches/day. Cache hit rate > 90%.
- Asset storage: 5 assets × 100 schools = 500 files in Supabase Storage. Negligible.
- Subdomain resolution: 1 per web app visit. Cached for 1 hour.

---

## 18. Edge Cases

| # | Scenario | Expected Behavior |
|---|---|---|
| EC-1 | School has no branding row | Return default branding (`is_customized = false`). |
| EC-2 | Branding fetch fails | Use default Vidya Prayag branding. Silent fallback. |
| EC-3 | Logo URL broken/expired | Show default logo. Log error. |
| EC-4 | Invalid hex color in DB | Use default color. Log error. |
| EC-5 | Subdomain already taken | Return 409 "Subdomain already taken." |
| EC-6 | Subdomain invalid format | Return 400 "Invalid subdomain format." |
| EC-7 | Logo upload exceeds size limit | Return 400 "File too large. Max 1MB." |
| EC-8 | Logo upload invalid format | Return 400 "Invalid format. Use PNG, SVG, or WebP." |
| EC-9 | Admin resets branding to default | Set `is_customized = false`, reset colors to defaults. Keep assets. |
| EC-10 | Web app accessed via root domain (no subdomain) | Show default Vidya Prayag login. |
| EC-11 | Web app accessed via unknown subdomain | Show 404 "School not found." |
| EC-12 | School admin from school A tries to update school B branding | Return 403 "Access denied." |
| EC-13 | Branding applied mid-session | Not supported. Branding applied at app launch only. User must restart app. |
| EC-14 | Dark mode with no dark logo variant | Use light logo. Log warning. |
| EC-15 | App icon change on Android | May require launcher restart. Show "App icon may take a moment to update." |
| EC-16 | Subdomain removed by admin | Web app at that subdomain shows 404. |
| EC-17 | Multiple admins editing branding simultaneously | Last write wins. No conflict resolution. |
| EC-18 | Splash screen image not set | Use default Vidya Prayag splash. |

---

## 19. Error Handling

### Error Response Format

Standard `ApiResponse` error format.

### Error Codes

| Code | HTTP Status | Description | User Message |
|---|---|---|---|
| `BRANDING_NOT_FOUND` | 404 | School branding not found | (Internal — return defaults) |
| `SUBDOMAIN_TAKEN` | 409 | Subdomain already in use | "Subdomain already taken. Try another." |
| `INVALID_SUBDOMAIN` | 400 | Subdomain format invalid | "Invalid subdomain. Use lowercase letters, numbers, and hyphens (4-32 chars)." |
| `INVALID_COLOR` | 400 | Color not valid hex | "Invalid color. Use hex format (#RRGGBB)." |
| `FILE_TOO_LARGE` | 400 | Upload exceeds size limit | "File too large. Max {size}." |
| `INVALID_FILE_FORMAT` | 400 | Upload format not supported | "Invalid format. Use {formats}." |
| `SUBDOMAIN_NOT_FOUND` | 404 | Subdomain doesn't resolve to any school | "School not found for this subdomain." |

### Error Handling Strategy

- **Branding fetch failure:** Silent fallback to defaults. Log error.
- **Asset upload failure:** Return error to admin. Retry available.
- **Subdomain conflict:** Return 409. Admin chooses different subdomain.
- **Color validation:** Return 400. Admin corrects hex code.
- **File validation:** Return 400 with specific message. Admin corrects file.

### Retry Strategy

- Branding fetch: no retry (fallback to defaults)
- Asset upload: admin can retry upload
- Subdomain check: real-time (no retry needed)

### Fallback Behavior

- No branding row: default Vidya Prayag branding
- Branding fetch failure: default Vidya Prayag branding
- Logo URL broken: default Vidya Prayag logo
- Invalid color in DB: default color for that token
- Subdomain not found: 404 page (web)

---

## 20. Analytics & Reporting

### Analytics Dashboard Data

| Metric | Source | Derivation |
|---|---|---|
| Schools with custom branding | `school_branding.is_customized = true` | Count |
| Schools with custom subdomain | `school_branding.custom_subdomain IS NOT NULL` | Count |
| Most popular primary colors | `school_branding.primary_color` | Group by color, count |
| Branding update frequency | Audit logs | Count per school per month |

### Export Capabilities

N/A — branding data not exportable.

### Report Types

| Report | Format | Frequency | Recipient |
|---|---|---|---|
| Branding adoption | JSON (API) | On-demand | Super Admin |
| Subdomain registry | JSON (API) | On-demand | Super Admin |

---

## 21. Testing Strategy

### Unit Tests

- `BrandingService.getBranding()` — existing branding, no branding (defaults), fetch failure
- `BrandingService.updateBranding()` — color validation, partial updates
- `BrandingService.checkSubdomainAvailable()` — available, taken, invalid format
- `BrandingService.resolveSubdomain()` — valid subdomain, unknown subdomain
- `BrandingManager.applyBranding()` — VColors override, default fallback
- Color validation — valid hex, invalid hex
- Subdomain validation — valid, invalid, edge cases (hyphens, length)

### Integration Tests

- Full branding flow: admin sets colors → fetch branding → verify colors applied
- Asset upload: upload logo → verify URL stored → fetch branding → verify URL
- Subdomain: set subdomain → resolve via public API → verify school returned
- Default fallback: school with no branding → fetch → verify defaults returned

### E2E Tests

- Admin configures branding → user logs in → sees school logo and colors
- Web app accessed via subdomain → shows school-specific login
- Admin resets branding → user sees default Vidya Prayag branding on next launch

### Performance Tests

- Branding fetch: < 500ms (cached: < 50ms)
- Asset upload: < 3 seconds (1MB file)
- Subdomain resolution: < 100ms
- Branding apply (client): < 200ms (no visible flash)

### Test Data

- 3 schools: one with full branding, one with partial, one with no branding
- Sample logos (PNG, SVG, WebP)
- Sample subdomains (valid, invalid, taken)

### Test Environment

- Test database with `school_branding` table
- Mock Supabase Storage (returns URLs)
- Test JWT tokens for admin and parent roles

---

## 22. Acceptance Criteria

- [ ] Admin uploads logo, favicon, app icon, splash screen
- [ ] Admin sets primary/secondary/accent colors
- [ ] App UI dynamically uses school colors
- [ ] Login screen shows school logo + name
- [ ] Email templates, report cards use school branding
- [ ] Custom subdomain works for web app
- [ ] Branding preview available before applying
- [ ] Default fallback when not customized
- [ ] Branding cached for app session
- [ ] Subdomain uniqueness enforced
- [ ] Color validation (hex format)
- [ ] Asset size and format validation

---

## 23. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration `migration_075_school_branding.sql`, Exposed table, register in `DatabaseFactory` |
| 2 | 2 days | `BrandingService` (CRUD, asset upload, subdomain management) |
| 3 | 2 days | Dynamic theming in `VColors`/`VTheme` — `BrandingManager` |
| 4 | 2 days | Login screen + splash screen branding |
| 5 | 2 days | Email/report card/newsletter branding integration |
| 6 | 2 days | Client UI: `BrandingSettingsScreen` (upload, color picker, preview, subdomain) |
| 7 | 1 day | Subdomain routing (web app middleware) |
| 8 | 1 day | Tests: unit, integration, E2E |

### Pre-Implementation Checklist

- [ ] Verify Supabase Storage bucket for brand assets
- [ ] Verify `VColors` token system supports dynamic override
- [ ] Verify Android adaptive icon support
- [ ] Verify web app subdomain routing middleware
- [ ] Verify email template system supports dynamic branding

---

## 24. File-Level Impact Analysis

### Server

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | `SchoolBrandingTable` |
| `server/.../db/DatabaseFactory.kt` | Modify | Register `SchoolBrandingTable` in `allTables` |
| `server/.../feature/branding/BrandingService.kt` | **New** | Core branding service (CRUD, upload, subdomain) |
| `server/.../feature/branding/BrandingRepository.kt` | **New** | Branding repository |
| `server/.../feature/branding/BrandingRouting.kt` | **New** | API endpoints (admin + public) |
| `docs/db/migration_075_school_branding.sql` | **New** | DDL: `school_branding` table |

### Shared (KMP)

| File | Change Type | Description |
|---|---|---|
| `shared/.../branding/domain/model/BrandingModels.kt` | **New** | DTOs, domain models, `DefaultBranding` |
| `shared/.../branding/domain/repository/BrandingRepository.kt` | **New** | Repository interface |
| `shared/.../branding/data/remote/BrandingApi.kt` | **New** | HTTP API definitions |
| `shared/.../core/branding/BrandingManager.kt` | **New** | Client branding application (VColors override) |

### Client (Compose)

| File | Change Type | Description |
|---|---|---|
| `composeApp/.../ui/v2/theme/VTheme.kt` | Modify | Dynamic theming support (`VidyaPrayagTheme` with branding param) |
| `composeApp/.../ui/v2/theme/VColors.kt` | Modify | Support dynamic color override |
| `composeApp/.../ui/v2/screens/auth/LoginScreen.kt` | Modify | School logo + name display |
| `composeApp/.../ui/v2/screens/SplashScreen.kt` | Modify | School splash image |
| `composeApp/.../ui/v2/screens/admin/BrandingSettingsScreen.kt` | **New** | Branding management UI (upload, colors, preview, subdomain) |

---

## 25. Future Enhancements

| # | Enhancement | Priority | Effort | Notes |
|---|---|---|---|---|
| F-1 | Dark mode logo variants | Medium | S | `logo_dark_url` for dark mode |
| F-2 | Custom fonts | Medium | M | Per-school font selection |
| F-3 | Custom domain (not subdomain) | Low | L | e.g., app.dpsrkpuram.com |
| F-4 | Branded push notification icons | Low | S | Custom notification icon per school |
| F-5 | Branding templates | Low | S | Pre-made branding templates |
| F-6 | Multi-language branding | Low | M | Different logos for different languages |
| F-7 | Branded WhatsApp templates | Medium | M | School-specific WhatsApp template headers |
| F-8 | Branding A/B testing | Low | L | Test different color schemes |
| F-9 | Animated splash screens | Low | M | Lottie/animated splash per school |
| F-10 | Branded in-app notifications | Low | S | School branding in notification cards |

---

## Appendix A: Sequence Diagrams

### A.1 Branding Application Flow

```
User (app)       Server              Cache              BrandingManager
  │                  │                    │                    │
  │  POST /login     │                    │                    │
  │  ──────────────> │                    │                    │
  │  ←──JWT (school_id)                   │                    │
  │                  │                    │                    │
  │  GET /branding/{schoolId}             │                    │
  │  ──────────────> │                    │                    │
  │                  │──check cache──────→│                    │
  │                  │←──cache miss───────│                    │
  │                  │──query DB──────────────────────────→   │
  │                  │←──branding row──────────────────────   │
  │                  │──store in cache───→│                    │
  │  ←──200: SchoolBrandingDto            │                    │
  │                  │                    │                    │
  │  ──applyBranding(branding)──────────────────────────────→│
  │                  │                    │   VColors overridden
  │  ──render UI with school colors──────→│                    │
  │                  │                    │                    │
```

### A.2 Admin Updates Branding

```
Admin (app)       Server              Supabase Storage
  │                  │                    │
  │  PATCH /school/branding               │
  │  { primary_color: "#FF0000" }         │
  │  ──────────────> │                    │
  │                  │──validate hex      │
  │                  │──update DB         │
  │  ←──200: updated branding              │
  │                  │                    │
  │  POST /school/branding/upload         │
  │  { asset_type: "logo", file }         │
  │  ──────────────> │                    │
  │                  │──upload to Supabase──────────────────→│
  │                  │←──URL─────────────────────────────────│
  │                  │──update DB with URL                   │
  │  ←──200: { url }                       │
  │                  │                    │
```

---

## Appendix B: Domain Model / ER Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                           schools (existing)                           │
│  id (PK)                                                              │
│  name, logoUrl, board, mediumOfInstruction                            │
└──────────────────────────┬───────────────────────────────────────────┘
                           │
                           │ 1:1
                           ▼
┌──────────────────────────────────────────────────────────────────────┐
│                      school_branding (new)                             │
│  id (PK)                                                              │
│  school_id (UNIQUE, FK → schools.id)                                  │
│  logo_url, logo_dark_url, favicon_url                                 │
│  app_icon_url, splash_screen_url, login_background_url                │
│  primary_color (#2563EB), secondary_color (#1E40AF), accent_color    │
│  custom_subdomain (nullable, unique)                                  │
│  is_customized (default false)                                        │
│  created_at, updated_at                                               │
│  INDEX: (school_id) UNIQUE                                            │
│  INDEX: (custom_subdomain)                                            │
└──────────────────────────────────────────────────────────────────────┘

Brand Assets (in Supabase Storage, not in DB):
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│ logo.png          │  │ app_icon.png     │  │ splash.webp      │
│ (Supabase URL)    │  │ (Supabase URL)   │  │ (Supabase URL)   │
└──────────────────┘  └──────────────────┘  └──────────────────┘
```

---

## Appendix C: Event Flow

### Domain Events

| Event | Emitter | Consumers | Payload | Side Effects |
|---|---|---|---|---|
| `BrandingUpdated` | `BrandingService.updateBranding()` | None (logged) | `schoolId, changes` | Cache invalidated |
| `SubdomainAssigned` | `BrandingService.updateSubdomain()` | None (logged) | `schoolId, subdomain` | Web app can resolve subdomain |
| `SubdomainRemoved` | `BrandingService.removeSubdomain()` | None (logged) | `schoolId, oldSubdomain` | Web app 404 for old subdomain |
| `BrandingAssetUploaded` | `BrandingService.uploadAsset()` | None (logged) | `schoolId, assetType, url` | Asset URL stored in DB |

### Event Delivery Guarantees

- Events emitted synchronously within service methods
- All events logged for audit
- No external consumers — events are internal audit trail

### Branding Resolution on Login

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

## Appendix D: Configuration

### Environment Variables

| Variable | Default | Description |
|---|---|---|
| `BRANDING_ENABLED` | `true` | Enable/disable branding feature |
| `BRANDING_CACHE_TTL_SECONDS` | `600` | Server-side cache TTL (10 min) |
| `BRANDING_MAX_LOGO_SIZE_KB` | `1024` | Max logo file size (1MB) |
| `BRANDING_MAX_ICON_SIZE_KB` | `512` | Max app icon file size |
| `BRANDING_MAX_SPLASH_SIZE_KB` | `2048` | Max splash screen file size (2MB) |
| `BRANDING_SUBDOMAIN_MIN_LENGTH` | `4` | Min subdomain length |
| `BRANDING_SUBDOMAIN_MAX_LENGTH` | `32` | Max subdomain length |
| `BRANDING_DEFAULT_PRIMARY_COLOR` | `#2563EB` | Default primary color |
| `BRANDING_DEFAULT_SECONDARY_COLOR` | `#1E40AF` | Default secondary color |
| `BRANDING_DEFAULT_ACCENT_COLOR` | `#3B82F6` | Default accent color |

### Feature Flags

| Flag | Default | Description |
|---|---|---|
| `BRANDING_ENABLED` | `true` | Enable/disable branding feature |
| `BRANDING_SUBDOMAIN_ENABLED` | `true` | Enable/disable custom subdomains |
| `BRANDING_DYNAMIC_APP_ICON` | `false` | Enable/disable dynamic app icon (Android) |
| `BRANDING_EMAIL_INTEGRATION` | `true` | Enable/disable branded email templates |

### School-Level Settings

N/A — branding IS the school-level setting. No additional configuration needed.

---

## Appendix E: Migration & Rollback

### Migration: `migration_075_school_branding.sql`

```sql
-- Migration 075: School Branding Kit
-- Creates school_branding table for per-school branding customization

BEGIN;

CREATE TABLE IF NOT EXISTS school_branding (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL UNIQUE,
    logo_url        TEXT,
    logo_dark_url   TEXT,
    favicon_url     TEXT,
    app_icon_url    TEXT,
    splash_screen_url TEXT,
    primary_color   VARCHAR(8) DEFAULT '#2563EB',
    secondary_color VARCHAR(8) DEFAULT '#1E40AF',
    accent_color    VARCHAR(8) DEFAULT '#3B82F6',
    custom_subdomain TEXT,
    login_background_url TEXT,
    is_customized   BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_school_branding_subdomain
    ON school_branding (custom_subdomain)
    WHERE custom_subdomain IS NOT NULL;

COMMIT;
```

### Rollback: `migration_075_rollback.sql`

```sql
BEGIN;
DROP TABLE IF EXISTS school_branding;
COMMIT;
```

### Migration Validation

- Verify `school_branding` table created with correct columns
- Verify `school_id` UNIQUE constraint created
- Verify `custom_subdomain` index created
- Run `SELECT count(*) FROM school_branding` — should be 0 (new feature)
- Verify default colors match expected values

---

## Appendix F: Observability

### Structured Logging

| Log Level | Event | Context Fields |
|---|---|---|
| INFO | Branding fetched | `schoolId, isCustomized, cacheHit` |
| INFO | Branding updated | `schoolId, adminId, changes` |
| INFO | Asset uploaded | `schoolId, assetType, url, fileSize` |
| INFO | Subdomain assigned | `schoolId, subdomain` |
| INFO | Subdomain removed | `schoolId, oldSubdomain` |
| INFO | Subdomain resolved | `subdomain, schoolId` |
| WARN | Branding not found (using defaults) | `schoolId` |
| WARN | Logo URL broken | `schoolId, logoUrl` |
| WARN | Invalid color in DB | `schoolId, color, field` |
| ERROR | Asset upload failed | `schoolId, assetType, error` |
| ERROR | Branding fetch failed | `schoolId, error` |

### Metrics

| Metric | Type | Labels | Description |
|---|---|---|---|
| `branding_fetches_total` | Counter | `cache_hit` | Total branding fetches |
| `branding_cache_hit_rate` | Gauge | — | Cache hit percentage |
| `branding_updates_total` | Counter | `school_id` | Branding updates per school |
| `branding_asset_uploads_total` | Counter | `asset_type` | Asset uploads by type |
| `branding_subdomain_count` | Gauge | — | Total custom subdomains |
| `branding_customized_schools` | Gauge | — | Schools with custom branding |
| `branding_fetch_duration` | Histogram | — | Branding fetch latency |

### Health Checks

| Check | Endpoint | Description |
|---|---|---|
| Branding service | `/health/branding` | Verify branding service and DB accessible |
| Supabase Storage | `/health/storage` | Verify Supabase Storage accessible (existing) |

### Alerts

| Alert | Condition | Severity | Notification |
|---|---|---|---|
| Branding fetch failure rate high | Error rate > 5% | Warning | Email to dev team |
| Asset upload failure rate high | Upload error rate > 10% | Warning | Email to dev team |
| Subdomain resolution slow | Resolution time > 500ms | Warning | Email to dev team |

### Dashboards

| Dashboard | Panels | Audience |
|---|---|---|
| Branding Adoption | Customized schools, subdomain count, popular colors | Product Team |
| Branding Performance | Fetch duration, cache hit rate, error rate | Dev Team |
| Asset Uploads | Upload count by type, failure rate, storage usage | Dev Team |

### Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Branding fetch failure | Low | Low | Default fallback. Silent. |
| Logo URL broken | Low | Low | Default logo fallback. |
| Subdomain conflict | Medium | Low | Uniqueness check before assignment. |
| Invalid color in DB | Very Low | Low | Validation on write. Default fallback on read. |
| Asset upload failure | Medium | Low | Admin can retry. Default assets used. |
| Dynamic app icon not supported | High | Low | Use adaptive icons. May require launcher restart. |
| Subdomain DNS propagation delay | Low | Low | Use wildcard DNS. CNAME resolves immediately. |
| Color contrast/accessibility issues | Medium | Medium | Preview before applying. Admin responsibility. |
