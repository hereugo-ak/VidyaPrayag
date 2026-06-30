# Festival & Cultural Calendar — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-28
> **Prerequisites:** None
> **Source:** `DIFFERENTIATING_FEATURES.md` §7.1
> **Template:** `_SPEC_TEMPLATE.md` v1 (25 mandatory + 6 optional sections)

---

## 1. Feature Overview

Pre-built Indian festival and cultural calendar with automatic holiday setup, festival greetings, and cultural event integration. Schools get region-specific festival calendar auto-populated with optional customization.

### Goals

- Pre-built database of Indian festivals (national, regional, religious)
- Auto-populate school calendar with festivals as holidays/events
- Region-specific (North India vs South India festival variations)
- Festival greeting cards sent to parents on festival day
- Admin can customize: mark as holiday, half-day, or regular day
- Cultural event suggestions (decorate classroom, dress code, special assembly)

### Non-goals

- [ ] Custom festival creation by admin (only overrides on pre-built festivals)
- [ ] International festival calendars (India-focused)
- [ ] Festival image generation (uses pre-built greeting templates)
- [ ] Festival analytics (engagement tracking on greetings)
- [ ] Multi-school regional festival aggregation

### Dependencies

- `AcademicCalendarTable` — existing holiday/event calendar for auto-population
- `CalendarEventsTable` — existing school events table
- `NotificationService` — existing notification infrastructure for greetings
- `WhatsappLogsTable` — existing WhatsApp infrastructure for greetings
- `SchoolsTable` — school records with region info
- `AcademicYearsTable` — academic year records

### Related Modules

- `server/.../feature/festival/` — new festival calendar module
- `shared/.../feature/festival/` — shared DTOs and API
- `composeApp/.../ui/v2/screens/admin/` — admin festival management UI
- `composeApp/.../ui/v2/screens/parent/` — parent upcoming festivals UI

---

## 2. Current System Assessment

### Existing Code

- `AcademicCalendarTable` (`Tables.kt:1360-1380`) — holiday/event calendar with `title`, `date`, `type`
- `CalendarEventsTable` (`Tables.kt:1370-1395`) — school events
- No festival database, no auto-population
- `DIFFERENTIATING_FEATURES.md` §7.1: Festival Calendar, effort S, data readiness: "AcademicCalendarTable exists"

### Existing Database

- `AcademicCalendarTable` — existing holiday/event calendar (target for auto-population)
- `CalendarEventsTable` — existing school events
- `SchoolsTable` — school records (region info needed)
- `AcademicYearsTable` — academic year records
- `AppUsersTable` — user accounts for greeting distribution
- `WhatsappLogsTable` — WhatsApp message logs
- `NotificationPreferencesTable` — notification preferences per user

### Existing APIs

- Academic calendar CRUD
- Calendar event CRUD
- Notification delivery via `NotificationService`
- Auth: OTP login, password login

### Existing UI

- School portal with admin tabs (`SchoolPortalV2.kt`)
- Parent portal with calendar/announcement access
- Web admin dashboard (`website/src/app/admin/`)

### Existing Services

- `NotificationService` — multi-channel notifications (push, in-app, WhatsApp)
- `NotifyRecipients.kt` — recipient resolution

### Existing Documentation

- `DIFFERENTIATING_FEATURES.md` §7.1 — Festival Calendar
- `Tables.kt` — `AcademicCalendarTable` schema

### Technical Debt

| # | Gap | Details |
|---|---|---|
| TD-1 | No festival database | No `festival_master` table, no seed data |
| TD-2 | No auto-population | No mechanism to auto-populate calendar with festivals |
| TD-3 | No lunar calendar support | No Hindu/Islamic calendar conversion |
| TD-4 | No festival greeting system | No greeting templates or daily greeting job |

### Gaps

| # | Gap | Impact | Severity |
|---|---|---|---|
| G1 | No festival database | Can't auto-populate calendar | **High** |
| G2 | No region-specific festivals | All schools get same festivals | **Medium** |
| G3 | No festival greetings | No automated parent engagement on festivals | **Medium** |
| G4 | No cultural suggestions | No guidance for cultural activities | **Low** |

---

## 3. Functional Requirements

### FR-001
| Field | Value |
|---|---|
| **Title** | Pre-built Festival Database |
| **Description** | Pre-built festival database: 100+ Indian festivals with date, name, description, region, type. Stored in `festival_master` table. |
| **Priority** | Critical |
| **User Roles** | System |
| **Acceptance notes** | Seed data includes national, Hindu, Islamic, Christian, Sikh, Jain, Buddhist, and regional festivals. Each has name, Hindi name, regional name, description, type, regions, default day type, cultural suggestions, and greeting template. |

### FR-002
| Field | Value |
|---|---|
| **Title** | Auto-Populate School Calendar |
| **Description** | Auto-populate school calendar at academic year start based on school's region. Creates `school_festival_overrides` + `AcademicCalendarTable` entries. |
| **Priority** | Critical |
| **User Roles** | School Admin |
| **Acceptance notes** | `POST /api/v1/school/festivals/auto-populate` with `academic_year_id` and `region`. Returns count of festivals populated. |

### FR-003
| Field | Value |
|---|---|
| **Title** | Admin Customization |
| **Description** | Admin can customize: holiday, half-day, regular day, cultural event. Override default day type per festival per academic year. |
| **Priority** | High |
| **User Roles** | School Admin |
| **Acceptance notes** | `PATCH /api/v1/school/festivals/{id}/override` with `day_type`, `is_greeting_enabled`, `custom_greeting`. |

### FR-004
| Field | Value |
|---|---|
| **Title** | Festival Greeting Cards |
| **Description** | Festival greeting card: auto-generated and sent to parents on festival day (in-app + WhatsApp). Daily job at 8 AM IST. |
| **Priority** | High |
| **User Roles** | System |
| **Acceptance notes** | `FestivalGreetingJob` runs daily at 8 AM IST. Checks `school_festival_overrides` for today's date with `is_greeting_enabled = true`. Renders greeting template and sends via WhatsApp + in-app. |

### FR-005
| Field | Value |
|---|---|
| **Title** | Cultural Event Suggestions |
| **Description** | Cultural event suggestions per festival (assembly theme, dress code, activities). Stored as JSON in `festival_master.cultural_suggestions`. |
| **Priority** | Medium |
| **User Roles** | School Admin |
| **Acceptance notes** | Displayed in admin festival management UI. Not automatically actioned. |

### FR-006
| Field | Value |
|---|---|
| **Title** | Multi-language Festival Names |
| **Description** | Multi-language festival names (Hindi, regional languages). Stored in `festival_master.name_hindi` and `festival_master.name_regional`. |
| **Priority** | Low |
| **User Roles** | System |
| **Acceptance notes** | Names displayed based on school's language preference or user's language setting. |

### FR-007
| Field | Value |
|---|---|
| **Title** | Lunar Calendar Support |
| **Description** | Lunar calendar support (Hindu/Islamic festivals shift dates each year). Actual date stored per academic year in `school_festival_overrides.festival_date`. |
| **Priority** | High |
| **User Roles** | System |
| **Acceptance notes** | Pre-calculated date table for 2026-2030 OR calendar conversion library. `festival_date` in `school_festival_overrides` stores the actual Gregorian date for each academic year. |

---

## 4. User Stories

### School Admin
- [ ] Auto-populate festival calendar for academic year
- [ ] View list of festivals for academic year with dates and day types
- [ ] Customize day type for individual festivals (holiday, half-day, regular, cultural event)
- [ ] Enable/disable festival greetings per festival
- [ ] Set custom greeting message for a festival
- [ ] View cultural suggestions per festival
- [ ] View upcoming festivals

### Parent
- [ ] View upcoming festivals (next 30 days)
- [ ] Receive festival greeting on festival day (in-app + WhatsApp)
- [ ] See festival name in preferred language

### System
- [ ] Auto-populate calendar entries from festival master
- [ ] Send festival greetings daily at 8 AM IST
- [ ] Calculate lunar festival dates for each academic year
- [ ] Create `AcademicCalendarTable` entries for festivals

---

## 5. Business Rules

### BR-001
**Rule:** Festival master is a read-only reference table.
**Enforcement:** Only system seed data can modify `festival_master`. Admin can only override via `school_festival_overrides`.

### BR-002
**Rule:** One override per school per festival per academic year.
**Enforcement:** `UNIQUE(school_id, festival_id, academic_year_id)` constraint on `school_festival_overrides`.

### BR-003
**Rule:** Auto-populate creates entries for school's region only.
**Enforcement:** `festival_master.regions` JSON array filtered by school's region. Festivals with `regions = ["all"]` are always included.

### BR-004
**Rule:** Festival greetings sent only if `is_greeting_enabled = true`.
**Enforcement:** Daily greeting job checks `is_greeting_enabled` before sending.

### BR-005
**Rule:** Lunar festival dates are pre-calculated or converted.
**Enforcement:** `school_festival_overrides.festival_date` stores the actual Gregorian date. Solar festivals (Republic Day, etc.) have fixed dates.

### BR-006
**Rule:** Auto-populate is idempotent.
**Enforcement:** If `school_festival_overrides` already has entries for `(school_id, academic_year_id)`, auto-populate skips or updates existing entries.

### BR-007
**Rule:** Only school admin can auto-populate and override festivals.
**Enforcement:** `requireSchoolAdmin()` on all admin endpoints.

---

## 6. Database Design

### 6.1 Entity Relationship Summary

Two new tables: `festival_master` (read-only reference with 100+ Indian festivals) and `school_festival_overrides` (per-school per-year customization with actual dates).

### 6.2 New Tables

#### `festival_master` table

```sql
CREATE TABLE festival_master (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            TEXT NOT NULL,
    name_hindi      TEXT,
    name_regional   TEXT,
    description     TEXT,
    festival_type   VARCHAR(32) NOT NULL,          -- national | hindu | islamic | christian | sikh | jain | buddhist | regional
    regions         TEXT NOT NULL DEFAULT '[]',    -- JSON: ["north", "south", "east", "west", "all"]
    default_day_type VARCHAR(16) NOT NULL DEFAULT 'holiday', -- holiday | half_day | cultural_event
    cultural_suggestions TEXT,                     -- JSON: {"dress_code": "Ethnic", "assembly_theme": "...", "activities": [...]}
    greeting_template TEXT,                        -- "Wishing you a blessed {{festival_name}}! - {{school_name}}"
    is_active       BOOLEAN NOT NULL DEFAULT true
);
```

#### `school_festival_overrides` table

```sql
CREATE TABLE school_festival_overrides (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL REFERENCES schools(id),
    festival_id     UUID NOT NULL REFERENCES festival_master(id),
    academic_year_id UUID NOT NULL,
    festival_date   DATE NOT NULL,                 -- actual date for this year (lunar calendar)
    day_type        VARCHAR(16) NOT NULL,           -- holiday | half_day | regular | cultural_event
    is_greeting_enabled BOOLEAN NOT NULL DEFAULT true,
    custom_greeting TEXT,                           -- override default greeting
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(school_id, festival_id, academic_year_id)
);
CREATE INDEX idx_school_festival_overrides_school_year ON school_festival_overrides(school_id, academic_year_id, festival_date);
CREATE INDEX idx_school_festival_overrides_date ON school_festival_overrides(festival_date) WHERE is_greeting_enabled = true;
```

### 6.3 Modified Tables

None. Auto-populate creates entries in existing `AcademicCalendarTable` but does not modify its schema.

### 6.4 Indexes

- `idx_school_festival_overrides_school_year` — listing by school + year + date
- `idx_school_festival_overrides_date` — daily greeting job lookup by date (only greeting-enabled)

### 6.5 Constraints

- `festival_master.name` — NOT NULL, TEXT
- `festival_master.festival_type` — NOT NULL, one of national/hindu/islamic/christian/sikh/jain/buddhist/regional
- `festival_master.regions` — NOT NULL, JSON array
- `festival_master.default_day_type` — NOT NULL, one of holiday/half_day/cultural_event
- `festival_master.is_active` — NOT NULL, BOOLEAN
- `school_festival_overrides.school_id` — NOT NULL, FK to schools
- `school_festival_overrides.festival_id` — NOT NULL, FK to festival_master
- `school_festival_overrides.academic_year_id` — NOT NULL
- `school_festival_overrides.festival_date` — NOT NULL, DATE
- `school_festival_overrides.day_type` — NOT NULL, one of holiday/half_day/regular/cultural_event
- `school_festival_overrides.is_greeting_enabled` — NOT NULL, BOOLEAN
- `UNIQUE(school_id, festival_id, academic_year_id)` — one override per school per festival per year

### 6.6 Foreign Keys

- `school_festival_overrides.school_id` → `schools.id`
- `school_festival_overrides.festival_id` → `festival_master.id`

### 6.7 Soft Delete Strategy

- `festival_master.is_active` — soft delete for festivals (deactivate instead of delete)
- `school_festival_overrides` — not deleted (historical record). Admin can change `day_type` to `regular` to effectively disable.

### 6.8 Audit Fields

- `school_festival_overrides.created_at` — creation timestamp
- `festival_master` — no audit fields (read-only reference table)

### 6.9 Migration Notes

Migration: `docs/db/migration_074_festival_calendar.sql`
- Creates 2 new tables with FK constraints and indexes
- Includes seed data for `festival_master` (100+ festivals)
- No modifications to existing tables

### 6.10 Exposed Mappings

Add 2 new table objects in `server/.../db/Tables.kt`:

- `FestivalMasterTable` — festival reference data
- `SchoolFestivalOverridesTable` — per-school overrides

Register in `DatabaseFactory.kt` `allTables` array. Order matters (FK dependencies):
1. `FestivalMasterTable`
2. `SchoolFestivalOverridesTable` (FK to festival_master)

### 6.11 Seed Data

Seed `festival_master` with 100+ Indian festivals:
- National: Republic Day, Independence Day, Gandhi Jayanti
- Hindu: Diwali, Holi, Navratri, Dussehra, Raksha Bandhan, Janmashtami, Ganesh Chaturthi, Makar Sankranti, Pongal, Onam, Baisakhi
- Islamic: Eid al-Fitr, Eid al-Adha, Muharram, Ramadan
- Christian: Christmas, Good Friday, Easter
- Sikh: Guru Nanak Jayanti, Baisakhi
- Regional: Bihu, Puthandu, Vishu, Ugadi, Gudi Padwa

Each festival includes: name, name_hindi, name_regional, description, festival_type, regions, default_day_type, cultural_suggestions (JSON), greeting_template.

---

## 7. State Machines

### Festival Override State Machine

Festival overrides don't have a complex state machine. The `day_type` field can be changed by admin at any time:

| Current `day_type` | Event | New `day_type` | Guard / Condition |
|---|---|---|---|
| `holiday` | Admin overrides | `half_day` / `regular` / `cultural_event` | Admin action |
| `half_day` | Admin overrides | `holiday` / `regular` / `cultural_event` | Admin action |
| `regular` | Admin overrides | `holiday` / `half_day` / `cultural_event` | Admin action |
| `cultural_event` | Admin overrides | `holiday` / `half_day` / `regular` | Admin action |

### Greeting Enable/Disable State Machine

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `greeting_enabled` | Admin disables | `greeting_disabled` | Admin action |
| `greeting_disabled` | Admin enables | `greeting_enabled` | Admin action |

### Auto-Populate State Machine

```
NOT_POPULATED ──admin_triggers_auto_populate──> POPULATED
POPULATED ──admin_re-runs_auto_populate──> POPULATED (idempotent: updates existing, adds new)
```

| Current State | Event | Next State | Guard / Condition |
|---|---|---|---|
| `not_populated` | Admin triggers auto-populate | `populated` | Creates overrides + calendar entries |
| `populated` | Admin re-runs auto-populate | `populated` | Idempotent: updates existing, adds new festivals |

---

## 8. Backend Architecture

### 8.1 Component Overview

`FestivalCalendarService` handles auto-population, overrides, greeting sending, and upcoming festival queries. `FestivalGreetingJob` runs daily at 8 AM IST to send greetings. `FestivalRouting` exposes admin and parent endpoints.

### 8.2 Design Principles

1. **Read-only master** — `festival_master` is a reference table, not editable by admin
2. **Per-school overrides** — all customization via `school_festival_overrides`
3. **Idempotent auto-populate** — re-running auto-populate updates existing, adds new
4. **Lunar calendar support** — actual dates stored per academic year
5. **Multi-tenant isolation** — all override queries filtered by `school_id`

### 8.3 Core Types

```kotlin
class FestivalCalendarService {
    // Admin
    suspend fun autoPopulate(schoolId: UUID, academicYearId: UUID, region: String): Int {
        // 1. Fetch all active festivals for school's region
        // 2. For each, calculate actual date for academic year (lunar → Gregorian)
        // 3. Create school_festival_overrides + AcademicCalendarTable entries
        // 4. Return count populated
    }

    suspend fun listFestivals(schoolId: UUID, academicYearId: UUID): List<FestivalOverrideDto>
    suspend fun overrideDayType(schoolId: UUID, festivalId: UUID, academicYearId: UUID, dayType: String, isGreetingEnabled: Boolean?, customGreeting: String?): FestivalOverrideDto
    suspend fun getFestivalDetails(schoolId: UUID, festivalId: UUID, academicYearId: UUID): FestivalDetailsDto

    // Parent
    suspend fun getUpcomingFestivals(schoolId: UUID, days: Int): List<FestivalDto>

    // System
    suspend fun sendFestivalGreetings(): Int  // daily job at 8 AM IST
}
```

### 8.4 Repositories

- `FestivalMasterRepository` — read-only access to festival master data
- `SchoolFestivalOverrideRepository` — CRUD for school-specific overrides

### 8.5 Mappers

- `FestivalMapper` — maps `festival_master` rows to DTOs with parsed JSON regions and cultural suggestions
- `FestivalOverrideMapper` — maps `school_festival_overrides` rows to DTOs with joined festival master data

### 8.6 Permission Checks

- Admin endpoints: `requireSchoolContext()` + `requireSchoolAdmin()` for all writes
- Parent endpoints: `requireAuth()` — any authenticated parent
- Auto-populate: `requireSchoolAdmin()`
- Greeting job: no auth (system background job)

### 8.7 Background Jobs

- `FestivalGreetingJob` — daily at 8 AM IST; checks for today's festivals with greetings enabled, sends greetings via WhatsApp + in-app

### 8.8 Domain Events

- `FestivalsAutoPopulated` — emitted on auto-populate; count of festivals populated
- `FestivalOverrideUpdated` — emitted on override change
- `FestivalGreetingSent` — emitted on greeting delivery

### 8.9 Caching

- Festival master data: cached indefinitely (read-only, changes only on seed updates)
- School festival overrides: cached per school + academic year, invalidated on override change
- Upcoming festivals: cached for 1 hour

### 8.10 Transactions

- Auto-populate: insert/update overrides + insert AcademicCalendarTable entries in transaction
- Override: update override in transaction
- Greeting: read overrides + send notifications (notifications async)

### 8.11 Rate Limiting

- Standard API rate limiting for all endpoints
- No special rate limiting needed

### 8.12 Configuration

- `FESTIVAL_GREETING_JOB_ENABLED` — default `true`; enable/disable daily greeting job
- `FESTIVAL_GREETING_JOB_CRON` — default `0 0 8 * * ?` (8 AM IST)
- `FESTIVAL_UPCOMING_DAYS` — default `30`; number of days for upcoming festivals
- `FESTIAL_CACHE_TTL` — default `3600`; festival master cache TTL in seconds

---

## 9. API Contracts

### 9.1 Admin Endpoints

All require `requireSchoolContext()`. Writes require `requireSchoolAdmin()`.

```
# Festival Management
GET    /api/v1/school/festivals?academic_year_id={uuid}
POST   /api/v1/school/festivals/auto-populate  { academic_year_id, region }
GET    /api/v1/school/festivals/{festivalId}?academic_year_id={uuid}
PATCH  /api/v1/school/festivals/{festivalId}/override  { academic_year_id, day_type, is_greeting_enabled, custom_greeting }
```

### 9.2 Parent Endpoints

```
GET    /api/v1/parent/festivals/upcoming  -- next 30 days
```

### 9.3 DTO Models

All `@Serializable`, wrapped in `ApiResponse<T>` pattern.

```kotlin
@Serializable data class FestivalMasterDto(
    val id: String, val name: String, val nameHindi: String?,
    val nameRegional: String?, val description: String?,
    val festivalType: String, val regions: List<String>,
    val defaultDayType: String,
    val culturalSuggestions: CulturalSuggestionsDto?,
    val greetingTemplate: String?
)

@Serializable data class CulturalSuggestionsDto(
    val dressCode: String?,
    val assemblyTheme: String?,
    val activities: List<String>?
)

@Serializable data class FestivalOverrideDto(
    val id: String, val schoolId: String,
    val festivalId: String, val academicYearId: String,
    val festivalDate: String, val dayType: String,
    val isGreetingEnabled: Boolean, val customGreeting: String?,
    val festival: FestivalMasterDto,
    val createdAt: String
)

@Serializable data class FestivalDto(
    val festivalId: String, val name: String, val nameHindi: String?,
    val festivalDate: String, val dayType: String,
    val description: String?, val festivalType: String
)

@Serializable data class AutoPopulateDto(
    val academicYearId: String, val region: String  // north | south | east | west | all
)

@Serializable data class OverrideDto(
    val academicYearId: String,
    val dayType: String,  // holiday | half_day | regular | cultural_event
    val isGreetingEnabled: Boolean? = null,
    val customGreeting: String? = null
)

@Serializable data class AutoPopulateResultDto(
    val count: Int, val academicYearId: String
)
```

---

## 10. Frontend Architecture

### 10.1 Screens

| Screen | Platform | Role | Description |
|---|---|---|---|
| `FestivalCalendarScreen` | Compose | Admin | Festival list for academic year with dates, day types, override controls |
| `FestivalDetailsScreen` | Compose | Admin | Festival details with cultural suggestions, greeting settings |
| `UpcomingFestivalsScreen` | Compose | Parent | Upcoming festivals (next 30 days) with names and dates |
| Web admin festival page | Web | Admin | Festival management dashboard |

### 10.2 Navigation

- Admin portal → Festivals → `FestivalCalendarScreen`
- Admin portal → Festivals → {festival} → `FestivalDetailsScreen`
- Parent portal → Festivals → `UpcomingFestivalsScreen`
- Web admin → /admin/festivals → festival list page

### 10.3 UX Flows

#### Admin: Auto-Populate Festival Calendar

1. Admin opens Festivals → sees empty list or previous year's festivals
2. Clicks "Auto-Populate" → selects academic year and region
3. System populates festivals → list shows all festivals with dates and day types
4. Admin reviews and customizes individual festivals as needed

#### Admin: Override Festival

1. Admin clicks on a festival in the list
2. Views festival details (date, type, cultural suggestions, greeting template)
3. Changes day type (holiday → half-day, etc.)
4. Toggles greeting enabled/disabled
5. Optionally sets custom greeting message
6. Saves override

#### Parent: View Upcoming Festivals

1. Parent opens app → navigates to Festivals
2. Sees list of upcoming festivals (next 30 days)
3. Each festival shows name, date, day type (holiday/half-day)
4. On festival day, receives greeting via WhatsApp + in-app

### 10.4 State Management

```kotlin
data class FestivalState(
    val festivals: List<FestivalOverrideDto>,
    val upcomingFestivals: List<FestivalDto>,
    val currentFestival: FestivalOverrideDto?,
    val autoPopulateResult: AutoPopulateResultDto?,
    val isLoading: Boolean,
    val error: String?,
)
```

### 10.5 Offline Support

- Festival list cached locally for offline viewing
- Upcoming festivals cached for 1 hour

### 10.6 Loading States

- Loading festivals: "Loading festival calendar..."
- Auto-populating: "Populating festivals for academic year..."
- Sending override: "Saving festival settings..."

### 10.7 Error Handling (UI)

- Auto-populate failed: "Failed to populate festivals. Please try again."
- No festivals found: "No festivals found. Try auto-populating for this academic year."
- Festival not found: "Festival not found."

### 10.8 Component Integration Guidelines

| Rule | Description |
|---|---|
| **R1** | Auto-populate button with academic year and region selectors |
| **R2** | Festival list sorted by date with day type badges |
| **R3** | Day type selector (holiday, half-day, regular, cultural event) |
| **R4** | Greeting enable/disable toggle |
| **R5** | Custom greeting text field |
| **R6** | Cultural suggestions display (dress code, assembly theme, activities) |
| **R7** | Festival name in Hindi/regional (if available) |
| **R8** | Upcoming festivals list for parents (next 30 days) |
| **R9** | Festival type badge (national, Hindu, Islamic, etc.) |

---

## 11. Shared Module Changes (KMP)

### 11.1 DTOs

All DTOs defined in section 9.3, placed in `shared/.../feature/festival/data/remote/`.

### 11.2 Domain Models

```kotlin
data class FestivalMaster(
    val id: UUID, val name: String, val nameHindi: String?,
    val nameRegional: String?, val description: String?,
    val festivalType: FestivalType, val regions: List<Region>,
    val defaultDayType: DayType,
    val culturalSuggestions: CulturalSuggestions?,
    val greetingTemplate: String?,
)

enum class FestivalType { NATIONAL, HINDU, ISLAMIC, CHRISTIAN, SIKH, JAIN, BUDDHIST, REGIONAL }
enum class Region { NORTH, SOUTH, EAST, WEST, ALL }
enum class DayType { HOLIDAY, HALF_DAY, REGULAR, CULTURAL_EVENT }

data class CulturalSuggestions(
    val dressCode: String?,
    val assemblyTheme: String?,
    val activities: List<String>?,
)

data class SchoolFestivalOverride(
    val id: UUID, val schoolId: UUID,
    val festivalId: UUID, val academicYearId: UUID,
    val festivalDate: LocalDate, val dayType: DayType,
    val isGreetingEnabled: Boolean, val customGreeting: String?,
    val festival: FestivalMaster,
)

data class Festival(
    val festivalId: UUID, val name: String, val nameHindi: String?,
    val festivalDate: LocalDate, val dayType: DayType,
    val description: String?, val festivalType: FestivalType,
)
```

### 11.3 Repository Interfaces

```kotlin
interface FestivalRepository {
    suspend fun listFestivals(academicYearId: String): NetworkResult<List<FestivalOverrideDto>>
    suspend fun getFestival(festivalId: String, academicYearId: String): NetworkResult<FestivalOverrideDto>
    suspend fun autoPopulate(dto: AutoPopulateDto): NetworkResult<AutoPopulateResultDto>
    suspend fun overrideFestival(festivalId: String, dto: OverrideDto): NetworkResult<FestivalOverrideDto>
    suspend fun getUpcomingFestivals(): NetworkResult<List<FestivalDto>>
}
```

### 11.4 UseCases

- `ListFestivalsUseCase`, `GetFestivalUseCase`
- `AutoPopulateUseCase`, `OverrideFestivalUseCase`
- `GetUpcomingFestivalsUseCase`

### 11.5 Validation

- Region: one of north, south, east, west, all
- Day type: one of holiday, half_day, regular, cultural_event
- Academic year ID: valid UUID
- Custom greeting: max 500 characters

### 11.6 Serialization

Standard Kotlinx serialization with JSON for regions and cultural suggestions.

### 11.7 Network APIs

Ktor `@Resource` route definitions:
- `SchoolFestivalsApi` — admin endpoints
- `ParentFestivalsApi` — parent endpoints

### 11.8 Database Models (Local Cache)

- Festival list cached locally for offline viewing
- Upcoming festivals cached for 1 hour

---

## 12. Permissions Matrix

| Action | Super Admin | School Admin | Teacher | Parent |
|---|---|---|---|---|
| Auto-populate festivals | ✅ | ✅ | ❌ | ❌ |
| View festival list | ✅ | ✅ | ✅ | ❌ |
| Override festival day type | ✅ | ✅ | ❌ | ❌ |
| Enable/disable greeting | ✅ | ✅ | ❌ | ❌ |
| Set custom greeting | ✅ | ✅ | ❌ | ❌ |
| View cultural suggestions | ✅ | ✅ | ✅ | ❌ |
| View upcoming festivals | N/A | N/A | ✅ | ✅ |
| Receive festival greetings | N/A | N/A | ✅ | ✅ |

---

## 13. Notifications

### Festival-Specific Notification Triggers

| Type | Trigger | Recipient | Channel | Message |
|---|---|---|---|---|
| Festival Greeting | Daily job at 8 AM IST on festival day | All parents | Push + WhatsApp | Greeting template rendered with festival name + school name |
| Festival Reminder | Day before festival (optional) | All parents | Push | "Tomorrow is {festival_name}. School will be {holiday/half-day}." |

### Notification System Integration

- Reuse `NotificationService` for in-app push notifications
- WhatsApp: render greeting text + festival image → send via `WhatsAppService`
- In-app: create notification with festival name and greeting
- Reuse `NotifyRecipients.kt` for parent audience resolution (ALL_SCHOOL)
- New WhatsApp template: `festival_greeting` (with festival name + school name variables)

### Greeting Template Rendering

- Default template: `"Wishing you a blessed {{festival_name}}! - {{school_name}}"`
- Custom greeting: admin can override per festival per year
- Template variables: `{{festival_name}}`, `{{school_name}}`, `{{festival_name_hindi}}`

---

## 14. Background Jobs

### Festival Greeting Job

| Field | Value |
|---|---|
| **Name** | `FestivalGreetingJob` |
| **Trigger** | Daily |
| **Frequency** | Daily at 8 AM IST |
| **Description** | Checks `school_festival_overrides` for today's date with `is_greeting_enabled = true`, sends greetings via WhatsApp + in-app to all parents |
| **Timeout** | 120 seconds |
| **Retry** | None |
| **On failure** | Logged; retried next day |

### Auto-Populate (On-Demand)

| Field | Value |
|---|---|
| **Name** | `FestivalAutoPopulate` |
| **Trigger** | Admin request |
| **Frequency** | On-demand |
| **Description** | Populates school festival overrides + AcademicCalendarTable entries from festival master |
| **Timeout** | 60 seconds |
| **Retry** | None |
| **On failure** | Return error to admin |

---

## 15. Integrations

### Internal Integrations

| System | Integration Point | Direction | Protocol | Error Handling |
|---|---|---|---|---|
| `AcademicCalendarTable` | Auto-populate creates calendar entries | Write | Direct DB | Logged; transaction rollback |
| `CalendarEventsTable` | Cultural event creation (optional) | Write | Direct DB | Logged; non-blocking |
| `NotificationService` | Festival greeting delivery | Outbound | Direct call | Logged; non-blocking |
| `NotifyRecipients.kt` | Parent audience resolution for greetings | Read | Direct code | Fallback: skip if no recipients |
| `SchoolsTable` | School name + region for auto-populate | Read | Direct DB | Use default region if not set |
| `AcademicYearsTable` | Academic year ID validation | Read | Direct DB | Return error if not found |
| `WhatsappLogsTable` | WhatsApp message logging | Write | Direct DB | Logged |

### External Integrations

| System | Purpose | Direction | Protocol | Authentication | Error Handling |
|---|---|---|---|---|---|
| Meta WhatsApp Business API | Festival greeting WhatsApp messages | Outbound | HTTP API | Bearer token | Retry 3x; log to `WhatsappLogsTable` |

### Integration Patterns

- **Auto-populate:** Fetch festivals from `festival_master` filtered by region → calculate dates → insert `school_festival_overrides` + `AcademicCalendarTable` entries in transaction.
- **Greetings:** Daily job queries `school_festival_overrides` for today → render greeting template → send via `NotificationService` (push + WhatsApp).
- **Lunar calendar:** Pre-calculated date table for 2026-2030 stored in `school_festival_overrides.festival_date`. No external library needed for initial implementation.

---

## 16. Security

### Authentication

- Admin endpoints: standard JWT auth via `requireSchoolContext()` + `requireSchoolAdmin()`
- Parent endpoints: standard JWT auth via `requireAuth()`
- Greeting job: no auth (system background job)

### Authorization

- Only school admin can auto-populate and override festivals
- Any authenticated parent/teacher can view upcoming festivals
- Greeting job: system-level, no user auth

### Data Protection

- Festival master: public reference data, no PII
- School festival overrides: school-scoped, no PII
- Greeting messages: school name + festival name only, no PII
- No sensitive data in festival system

### Input Validation

- Region: one of north, south, east, west, all
- Day type: one of holiday, half_day, regular, cultural_event
- Academic year ID: valid UUID
- Custom greeting: max 500 characters
- Festival date: valid date, within academic year range

### Rate Limiting

- Standard API rate limiting for all endpoints
- No special rate limiting needed

### Audit Logging

- Auto-populate triggered (school, academic year, region, count)
- Festival override changed (school, festival, field changed, old/new value)
- Greeting sent (school, festival, channel, recipient count)

### PII Handling

- No PII collected or stored in festival system
- Greeting messages contain school name and festival name only
- No user-specific data in festival tables

### Multi-tenant Isolation

- All override queries filtered by `school_id`
- `school_festival_overrides.school_id` — NOT NULL, FK to schools
- Festival master is shared across all schools (read-only reference)
- Server validates school context on all admin endpoints

---

## 17. Performance & Scalability

### Expected Scale

- Festival master: ~100-200 records (static, read-only)
- School overrides: ~100-200 per school per academic year
- Greeting job: process all schools with festivals today (typically 10-100 schools per day)

### Query Optimization

- **Festival listing:** `idx_school_festival_overrides_school_year` on `(school_id, academic_year_id, festival_date)`. Sorted by date.
- **Greeting job:** `idx_school_festival_overrides_date` on `(festival_date) WHERE is_greeting_enabled = true`. Efficient daily lookup.
- **Festival master:** cached indefinitely (read-only, rarely changes)
- **Upcoming festivals:** query by date range, cached for 1 hour

### Indexing Strategy

- `idx_school_festival_overrides_school_year` — listing by school + year + date
- `idx_school_festival_overrides_date` — daily greeting job lookup
- `UNIQUE(school_id, festival_id, academic_year_id)` — prevents duplicate overrides

### Caching Strategy

- Festival master: cached indefinitely (read-only reference)
- School overrides: cached per school + academic year, invalidated on override change
- Upcoming festivals: 1-hour TTL cache

### Pagination

- Festival listing: max 100 per page (typically <200 festivals per year)
- Upcoming festivals: no pagination (typically <10 in 30 days)

### Connection Pooling

- Uses existing HikariCP connection pool
- No additional pooling needed

### Async Processing

- Greeting delivery: async (existing `NotificationService` pattern)
- Auto-populate: synchronous (admin waits for result)
- Override: synchronous (immediate response)

### Scalability Concerns

- Greeting job for 100+ schools: process sequentially with 1-second delay between schools
- Auto-populate for 200 festivals: <5 seconds (bulk insert)
- Festival master cache: single cache entry, minimal memory

---

## 18. Edge Cases

| # | Scenario | Expected Behavior |
|---|---|---|
| EC-1 | Admin auto-populates for academic year that already has overrides | Idempotent: update existing overrides, add new festivals. Return count of total festivals. |
| EC-2 | Admin auto-populates with invalid region | Return 400 "Invalid region. Must be one of: north, south, east, west, all." |
| EC-3 | Admin overrides festival that doesn't exist in master | Return 404 "Festival not found." |
| EC-4 | Admin overrides festival for academic year not yet populated | Return 400 "Festival not populated for this academic year. Run auto-populate first." |
| EC-5 | Greeting job runs but no festivals today | Return 0. No notifications sent. |
| EC-6 | Greeting job runs but WhatsApp unavailable | Send in-app only. Log WhatsApp failure. |
| EC-7 | Lunar festival date not available for requested year | Skip festival. Log warning. Admin can manually add override. |
| EC-8 | School has no region set | Default to "all" region. Log info. |
| EC-9 | Festival master has no greeting template | Skip greeting for that festival. Log warning. |
| EC-10 | Custom greeting is empty string | Use default greeting template. |
| EC-11 | Parent views upcoming festivals but none in next 30 days | Return empty list. UI shows "No upcoming festivals." |
| EC-12 | Admin sets day_type to 'regular' for a festival | Festival appears in calendar as regular school day. No holiday. |
| EC-13 | Festival date falls on weekend | Still created in calendar. Admin can override to 'regular' if desired. |
| EC-14 | Two festivals on same date | Both overrides created. Both greetings sent. Both calendar entries created. |
| EC-15 | Festival master deactivated (`is_active = false`) after auto-populate | Existing overrides remain. Future auto-populate skips deactivated festival. |

---

## 19. Error Handling

### Error Response Format

Standard `ApiResponse` error format:
```json
{
  "success": false,
  "error": {
    "code": "FESTIVAL_NOT_FOUND",
    "message": "Festival not found",
    "details": {}
  }
}
```

### Error Codes

| Code | HTTP Status | Description | User Message |
|---|---|---|---|
| `FESTIVAL_NOT_FOUND` | 404 | Festival not found in master | "Festival not found." |
| `OVERRIDE_NOT_FOUND` | 404 | Override not found for school/year | "Festival not populated for this academic year." |
| `INVALID_REGION` | 400 | Invalid region value | "Invalid region. Must be one of: north, south, east, west, all." |
| `INVALID_DAY_TYPE` | 400 | Invalid day type value | "Invalid day type. Must be one of: holiday, half_day, regular, cultural_event." |
| `ACADEMIC_YEAR_NOT_FOUND` | 404 | Academic year not found | "Academic year not found." |
| `AUTO_POPULATE_FAILED` | 500 | Auto-populate transaction failed | "Failed to populate festivals. Please try again." |
| `LUNAR_DATE_UNAVAILABLE` | 400 | Lunar date not available for requested year | "Festival date not available for this academic year." |

### Error Handling Strategy

- **Validation errors:** Return 400 with field-specific message
- **Auth errors:** Return 401/403 with clear message
- **Not found:** Return 404
- **Server errors:** Return 500 with generic message; log full error

### Retry Strategy

- Client retries: 3 attempts with exponential backoff for 5xx errors
- No retry for 4xx errors (client errors)
- WhatsApp delivery: 3 retries with 5-second intervals (existing pattern)
- Greeting job: no retry (next day)

### Fallback Behavior

- WhatsApp unavailable: Send in-app greeting only
- Lunar date unavailable: Skip festival, log warning
- Festival master cache miss: Query DB directly (slower but correct)
- Auto-populate partial failure: Transaction rollback, no partial entries

---

## 20. Analytics & Reporting

### Analytics Dashboard Data

| Metric | Source | Derivation |
|---|---|---|
| Total festivals per year | `school_festival_overrides` count by school + year | Direct count |
| Holiday count per year | `school_festival_overrides` where `day_type='holiday'` | Direct count |
| Greetings sent | `FestivalGreetingJob` logs | Count per festival per school |
| Greeting delivery rate | Sent vs delivered (WhatsApp logs) | Derived percentage |
| Festival type distribution | `festival_master.festival_type` | Group by type, count |

### Export Capabilities

- Festival calendar export (CSV) — festival name, date, day type, greeting enabled
- Academic year holiday list export (CSV) — holidays from festival overrides

### Report Types

| Report | Format | Frequency | Recipient |
|---|---|---|---|
| Festival calendar | CSV | On-demand | School Admin |
| Holiday list | CSV | On-demand | School Admin |
| Greeting delivery summary | JSON (API) | On-demand | School Admin |

---

## 21. Testing Strategy

### Unit Tests

- `FestivalCalendarService` — all methods (auto-populate, list, override, upcoming, greetings)
- Lunar date calculation/conversion
- Greeting template rendering with variables
- Idempotent auto-populate (run twice, verify no duplicates)
- Region filtering (north vs south vs all)

### Integration Tests

- Auto-populate: create school + academic year → auto-populate → verify overrides + calendar entries
- Override: auto-populate → override day type → verify update
- Greeting job: create override for today → run job → verify notification sent
- Greeting disabled: create override with `is_greeting_enabled=false` → run job → verify no notification
- Custom greeting: set custom greeting → run job → verify custom text used
- Upcoming festivals: create overrides for next 15 days → query → verify sorted by date
- Multi-tenant: school A overrides not visible to school B

### E2E Tests

- Admin auto-populates → overrides festival → parent views upcoming → receives greeting on festival day
- Admin re-runs auto-populate → existing overrides preserved, new festivals added

### Performance Tests

- Auto-populate with 200 festivals: < 5 seconds
- Greeting job for 50 schools: < 60 seconds
- Festival listing with 200 entries: < 200ms

### Test Data

- 20 sample festivals in `festival_master` (covering all types and regions)
- Sample overrides for 2 academic years
- Festivals with and without greeting templates
- Festivals with lunar dates (pre-calculated)

### Test Environment

- Test database with schema migration + seed data applied
- Mock WhatsApp API for greeting tests
- Mock `NotificationService` for notification tests
- Test JWT tokens for admin and parent roles

---

## 22. Acceptance Criteria

- [ ] Festival database seeded with 100+ festivals
- [ ] Auto-populate creates calendar entries for academic year
- [ ] Auto-populate is idempotent (re-running updates, no duplicates)
- [ ] Admin can customize day type (holiday/half-day/regular/cultural event)
- [ ] Festival greetings sent automatically on festival day (8 AM IST)
- [ ] Admin can enable/disable greetings per festival
- [ ] Admin can set custom greeting message
- [ ] Cultural suggestions available per festival
- [ ] Multi-language festival names (Hindi, regional)
- [ ] Upcoming festivals viewable by parents (next 30 days)
- [ ] Region-specific festival filtering works
- [ ] Lunar calendar dates correctly calculated for academic year
- [ ] All admin endpoints enforce `requireSchoolContext()` + `requireSchoolAdmin()`
- [ ] Compose app festival management UI
- [ ] Compose app upcoming festivals UI for parents
- [ ] Greeting job handles WhatsApp unavailability (fallback to in-app)

---

## 23. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration (`074`), 2 Exposed table objects, `DatabaseFactory` registration |
| 2 | 2 days | Seed `festival_master` with 100+ festivals (including lunar dates for 2026-2030) |
| 3 | 2 days | `FestivalCalendarService.kt` — auto-populate, overrides, upcoming, greeting logic |
| 4 | 1 day | `FestivalGreetingJob.kt` — daily job at 8 AM IST |
| 5 | 1 day | `FestivalRouting.kt` with all endpoints + DTOs, mount in `Application.kt` |
| 6 | 2 days | Client UI: `FestivalCalendarScreen.kt` (admin), `UpcomingFestivalsScreen.kt` (parent), wire into navigation |
| 7 | 1 day | Tests (server unit + integration, client unit) |

### Pre-Implementation Checklist

- [ ] Verify `AcademicCalendarTable` schema for auto-population target
- [ ] Verify `SchoolsTable` has region field (or add one)
- [ ] Prepare lunar calendar date table for 2026-2030
- [ ] Verify `NotificationService` supports ALL_SCHOOL audience
- [ ] Check WhatsApp template approval for `festival_greeting`

---

## 24. File-Level Impact Analysis

### Required (6 files)

| # | File | Change Type | Description |
|---|---|---|---|
| 1 | `docs/db/migration_074_festival_calendar.sql` | New | DDL: 2 tables + seed data for 100+ festivals |
| 2 | `server/.../db/Tables.kt` | Modify | Add `FestivalMasterTable`, `SchoolFestivalOverridesTable` |
| 3 | `server/.../db/DatabaseFactory.kt` | Modify | Register 2 tables in `allTables` array |
| 4 | `server/.../feature/festival/FestivalCalendarService.kt` | New | Core service (auto-populate, overrides, greetings, upcoming) |
| 5 | `server/.../feature/festival/FestivalGreetingJob.kt` | New | Daily greeting job at 8 AM IST |
| 6 | `server/.../feature/festival/FestivalRouting.kt` | New | API endpoints + DTOs + `festivalRouting()` function |

### Additional Required (2 files)

| # | File | Change Type | Description |
|---|---|---|---|
| 7 | `server/.../Application.kt` | Modify | Import + mount `festivalRouting()` |
| 8 | `shared/.../feature/festival/` | New | Shared DTOs, domain models, repository, API client |

### Client UI (2 files)

| # | File | Change Type | Description |
|---|---|---|---|
| 9 | `composeApp/.../ui/v2/screens/admin/FestivalCalendarScreen.kt` | New | Admin festival management with auto-populate, override controls |
| 10 | `composeApp/.../ui/v2/screens/parent/UpcomingFestivalsScreen.kt` | New | Parent upcoming festivals view |

### Optional (1 file)

| # | File | Change Type | Description |
|---|---|---|---|
| 11 | `website/src/app/admin/festivals/page.tsx` | New | Web admin festival management page |

---

## 25. Future Enhancements

| # | Enhancement | Priority | Effort | Notes |
|---|---|---|---|---|
| F-1 | Custom festival creation by admin | Medium | S | Allow admin to add school-specific festivals |
| F-2 | Festival image gallery | Low | S | Pre-built festival images for greetings |
| F-3 | Festival analytics dashboard | Low | M | Track greeting open rates, engagement |
| F-4 | Multi-language greeting templates | Medium | S | Greetings in Hindi/regional languages |
| F-5 | Festival reminder notifications | Medium | S | Day-before reminder to parents |
| F-6 | Calendar export (iCal) | Low | S | Export festival calendar to iCal format |
| F-7 | Festival-related announcement templates | Low | S | Pre-built announcement templates for festivals |
| F-8 | Lunar calendar library integration | Medium | M | Replace pre-calculated dates with library |
| F-9 | Festival photo sharing | Low | M | Parents share festival celebration photos |
| F-10 | Inter-festival event suggestions | Low | S | Suggest related cultural activities between festivals |

---

## Appendix A: Sequence Diagrams

### A.1 Admin Auto-Populates Festival Calendar

```
Admin       FestivalCalendarService    FestivalMasterTable    SchoolFestivalOverridesTable    AcademicCalendarTable
  │                │                         │                        │                            │
  │──autoPopulate(yearId,region)─→│           │                        │                            │
  │                │──fetch active festivals for region──────────────→│                            │
  │                │←──festivals list────────│                        │                            │
  │                │  [for each festival]    │                        │                            │
  │                │──calculate date (lunar→Gregorian)                │                            │
  │                │──upsert override────────────────────────────────→│                            │
  │                │──insert calendar entry──────────────────────────────────────────────────────→│
  │                │  [end for]              │                        │                            │
  │←──count────────│                         │                        │                            │
  │                │                         │                        │                            │
```

### A.2 Admin Overrides Festival Day Type

```
Admin       FestivalCalendarService    SchoolFestivalOverridesTable
  │                │                         │
  │──override(festivalId,yearId,dayType)──→│  │
  │                │──check override exists──│
  │                │←──override found──────│
  │                │──update day_type──────│
  │                │←──updated override────│
  │←──FestivalOverrideDto│                  │
  │                │                         │
```

### A.3 Daily Festival Greeting Job

```
FestivalGreetingJob    FestivalCalendarService    SchoolFestivalOverridesTable    NotificationService
  │                          │                            │                            │
  │──sendGreetings()────────→│                            │                            │
  │                          │──SELECT WHERE festival_date=today AND is_greeting_enabled=true─→│
  │                          │←──today's festivals────────│                            │
  │                          │  [for each festival]       │                            │
  │                          │──render greeting template──│                            │
  │                          │──resolve audience (ALL_SCHOOL)                         │
  │                          │──send WhatsApp + in-app──────────────────────────────→│
  │                          │  [end for]                 │                            │
  │←──count sent─────────────│                            │                            │
  │                          │                            │                            │
```

### A.4 Parent Views Upcoming Festivals

```
Parent       FestivalCalendarService    SchoolFestivalOverridesTable    (cache)
  │                │                         │                    │
  │──getUpcoming(30)─→│                      │                    │
  │                │──check cache──────────│──────────────────→│
  │                │  [cache hit]          │                    │
  │                │←──cached list────────│                    │
  │                │  [cache miss]         │                    │
  │                │──SELECT WHERE festival_date BETWEEN today AND today+30─→│
  │                │←──festivals──────────│                    │
  │                │──cache result (1hr)──│──────────────────→│
  │←──List<FestivalDto>│                  │                    │
  │                │                         │                    │
```

---

## Appendix B: Domain Model / ER Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                          festival_master                              │
│  id (PK)  name  name_hindi  name_regional  description               │
│  festival_type  regions (JSON)  default_day_type                     │
│  cultural_suggestions (JSON)  greeting_template  is_active           │
│  [READ-ONLY — seeded with 100+ Indian festivals]                     │
└──────────────────────────┬───────────────────────────────────────────┘
                           │
                           │ (FK)
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────────┐
│                    school_festival_overrides                          │
│  id (PK)                                                              │
│  school_id (FK→schools)                                               │
│  festival_id (FK→festival_master)                                     │
│  academic_year_id                                                     │
│  festival_date (actual Gregorian date for this year)                  │
│  day_type (holiday|half_day|regular|cultural_event)                   │
│  is_greeting_enabled  custom_greeting  created_at                     │
│  UNIQUE(school_id, festival_id, academic_year_id)                     │
└──────────────────────────────────────────────────────────────────────┘

Related existing tables:
┌────────────────────┐    ┌────────────────────┐
│  AcademicCalendar   │    │     schools         │
│  (auto-populated)   │    │  id, name, region   │
└────────────────────┘    └────────────────────┘
```

---

## Appendix C: Event Flow

### Domain Events

| Event | Emitter | Consumers | Payload | Side Effects |
|---|---|---|---|---|
| `FestivalsAutoPopulated` | `FestivalCalendarService.autoPopulate()` | None | `schoolId, academicYearId, region, count` | AcademicCalendarTable entries created |
| `FestivalOverrideUpdated` | `FestivalCalendarService.overrideDayType()` | None | `schoolId, festivalId, academicYearId, field, oldValue, newValue` | Invalidate cache |
| `FestivalGreetingSent` | `FestivalGreetingJob` | None | `schoolId, festivalId, channel, recipientCount` | None (notifications already sent) |

### Event Delivery Guarantees

- Events are emitted synchronously within the same transaction
- Greeting delivery is async (fire-and-forget with logging)
- Failed greetings are logged; not retried until next day
- No event bus / message queue — direct function calls

---

## Appendix D: Configuration

### Environment Variables

| Variable | Default | Description |
|---|---|---|
| `FESTIVAL_GREETING_JOB_ENABLED` | `true` | Enable/disable daily greeting job |
| `FESTIVAL_GREETING_JOB_CRON` | `0 0 8 * * ?` | Cron expression (8 AM IST) |
| `FESTIVAL_UPCOMING_DAYS` | `30` | Number of days for upcoming festivals query |
| `FESTIVAL_CACHE_TTL` | `3600` | Festival master cache TTL in seconds |
| `FESTIVAL_OVERRIDE_CACHE_TTL` | `600` | Override cache TTL in seconds (10 min) |

### Feature Flags

| Flag | Default | Description |
|---|---|---|
| `FESTIVAL_ENABLED` | `true` | Enable/disable festival feature |
| `FESTIVAL_GREETINGS_ENABLED` | `true` | Enable/disable festival greetings |
| `FESTIVAL_AUTO_POPULATE_ENABLED` | `true` | Enable/disable auto-populate |
| `FESTIVAL_LUNAR_CALENDAR_ENABLED` | `true` | Enable/disable lunar calendar support |

### School-Level Settings

| Setting | Source | Description |
|---|---|---|
| School region | `SchoolsTable.region` | Determines which festivals are auto-populated |
| School name | `SchoolsTable.name` | Used in greeting template rendering |

---

## Appendix E: Migration & Rollback

### Migration: `migration_074_festival_calendar.sql`

```sql
-- Migration 074: Festival & Cultural Calendar
-- Creates 2 new tables + seed data

BEGIN;

-- 1. Create festival_master table
CREATE TABLE IF NOT EXISTS festival_master (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            TEXT NOT NULL,
    name_hindi      TEXT,
    name_regional   TEXT,
    description     TEXT,
    festival_type   VARCHAR(32) NOT NULL,
    regions         TEXT NOT NULL DEFAULT '[]',
    default_day_type VARCHAR(16) NOT NULL DEFAULT 'holiday',
    cultural_suggestions TEXT,
    greeting_template TEXT,
    is_active       BOOLEAN NOT NULL DEFAULT true
);

-- 2. Create school_festival_overrides table
CREATE TABLE IF NOT EXISTS school_festival_overrides (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL REFERENCES schools(id),
    festival_id     UUID NOT NULL REFERENCES festival_master(id),
    academic_year_id UUID NOT NULL,
    festival_date   DATE NOT NULL,
    day_type        VARCHAR(16) NOT NULL,
    is_greeting_enabled BOOLEAN NOT NULL DEFAULT true,
    custom_greeting TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(school_id, festival_id, academic_year_id)
);
CREATE INDEX IF NOT EXISTS idx_school_festival_overrides_school_year
    ON school_festival_overrides(school_id, academic_year_id, festival_date);
CREATE INDEX IF NOT EXISTS idx_school_festival_overrides_date
    ON school_festival_overrides(festival_date) WHERE is_greeting_enabled = true;

-- 3. Seed festival_master with 100+ Indian festivals
-- (Seed data includes national, Hindu, Islamic, Christian, Sikh, Jain, Buddhist, regional festivals)
-- Each with name, name_hindi, description, festival_type, regions, default_day_type,
-- cultural_suggestions (JSON), greeting_template
-- INSERT INTO festival_master (...) VALUES (...);
-- [100+ INSERT statements]

COMMIT;
```

### Rollback: `migration_074_rollback.sql`

```sql
BEGIN;
DROP TABLE IF EXISTS school_festival_overrides;
DROP TABLE IF EXISTS festival_master;
COMMIT;
```

### Migration Validation

- Verify both tables created with correct columns
- Verify FK constraints in place
- Verify `UNIQUE(school_id, festival_id, academic_year_id)` constraint works
- Verify both indexes created
- Verify `festival_master` has 100+ records
- Run `SELECT count(*) FROM festival_master WHERE is_active = true` — should be 100+

---

## Appendix F: Observability

### Structured Logging

| Log Level | Event | Context Fields |
|---|---|---|
| INFO | Festivals auto-populated | `schoolId, academicYearId, region, count` |
| INFO | Festival override updated | `schoolId, festivalId, academicYearId, field, oldValue, newValue` |
| INFO | Festival greeting sent | `schoolId, festivalId, channel, recipientCount` |
| INFO | Greeting job completed | `totalFestivals, totalGreetings, schoolsProcessed` |
| WARN | No festivals today | (no context needed) |
| WARN | Lunar date unavailable | `festivalId, festivalName, academicYear` |
| WARN | WhatsApp unavailable for greeting | `schoolId, festivalId` |
| WARN | Festival master has no greeting template | `festivalId, festivalName` |
| ERROR | Auto-populate failed | `schoolId, academicYearId, error` |
| ERROR | Greeting job failed | `schoolId, festivalId, error` |

### Metrics

| Metric | Type | Labels | Description |
|---|---|---|---|
| `festivals_master_total` | Gauge | `type, region` | Total festivals in master by type and region |
| `festivals_auto_populated_total` | Counter | `school_id, region` | Auto-populate calls |
| `festival_overrides_total` | Gauge | `school_id, day_type` | Overrides by day type |
| `festival_greetings_sent_total` | Counter | `school_id, channel` | Greetings sent by channel |
| `festival_greeting_job_duration` | Histogram | — | Greeting job execution time |
| `festival_auto_populate_duration` | Histogram | — | Auto-populate execution time |

### Health Checks

| Check | Endpoint | Description |
|---|---|---|
| Festival tables exist | `/health/festivals` | Verify both festival tables are accessible |
| Festival master seeded | `/health/festivals/seed` | Verify `festival_master` has 100+ active records |

### Alerts

| Alert | Condition | Severity | Notification |
|---|---|---|---|
| Greeting job not running | No `festival_greeting_job_duration` metric in 24 hours | Warning | Email to dev team |
| Auto-populate failure rate high | `festivals_auto_populated_total` error rate > 10% | Warning | Email to dev team |
| Festival master empty | `festivals_master_total` < 100 | Critical | PagerDuty / email |
| Greeting delivery failure | `festival_greetings_sent_total` failure rate > 20% | Warning | Email to dev team |

### Dashboards

| Dashboard | Panels | Audience |
|---|---|---|
| Festival Overview | Total festivals, overrides by type, greeting rate | School Admin |
| Greeting Job Health | Job duration, greetings sent, failure rate, last run | Dev Team |
| Auto-Populate Stats | Calls per day, success rate, duration, festivals per school | Dev Team |
| Festival Master | Total by type, total by region, active vs inactive | Dev Team |
