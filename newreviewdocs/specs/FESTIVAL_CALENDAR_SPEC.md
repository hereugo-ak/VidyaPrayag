# Festival & Cultural Calendar — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** None
> **Source:** `DIFFERENTIATING_FEATURES.md` §7.1

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

---

## 2. Current System Assessment

- `AcademicCalendarTable` (`Tables.kt:1360-1380`) — holiday/event calendar with `title`, `date`, `type`
- `CalendarEventsTable` (`Tables.kt:1370-1395`) — school events
- No festival database, no auto-population
- `DIFFERENTIATING_FEATURES.md` §7.1: Festival Calendar, effort S, data readiness: "AcademicCalendarTable exists"

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Pre-built festival database: 100+ Indian festivals with date, name, description, region, type |
| FR-2 | Auto-populate school calendar at academic year start (based on school's region) |
| FR-3 | Admin can customize: holiday, half-day, regular day, cultural event |
| FR-4 | Festival greeting card: auto-generated and sent to parents on festival day (in-app + WhatsApp) |
| FR-5 | Cultural event suggestions per festival (assembly theme, dress code, activities) |
| FR-6 | Multi-language festival names (Hindi, regional languages) |
| FR-7 | Lunar calendar support (Hindu/Islamic festivals shift dates each year) |

---

## 4. Database Design

```sql
CREATE TABLE festival_master (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            TEXT NOT NULL,
    name_hindi      TEXT,
    name_regional   TEXT,                          -- in school's regional language
    description     TEXT,
    festival_type   VARCHAR(32) NOT NULL,          -- national | hindu | islamic | christian | sikh | jain | buddhist | regional
    regions         TEXT NOT NULL DEFAULT '[]',    -- JSON: ["north", "south", "east", "west", "all"]
    default_day_type VARCHAR(16) NOT NULL DEFAULT 'holiday', -- holiday | half_day | cultural_event
    cultural_suggestions TEXT,                     -- JSON: {"dress_code": "Ethnic", "assembly_theme": "...", "activities": [...]}
    greeting_template TEXT,                        -- "Wishing you a blessed {{festival_name}}! - {{school_name}}"
    is_active       BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE school_festival_overrides (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    festival_id     UUID NOT NULL REFERENCES festival_master(id),
    academic_year_id UUID NOT NULL,
    festival_date   DATE NOT NULL,                 -- actual date for this year (lunar calendar)
    day_type        VARCHAR(16) NOT NULL,           -- holiday | half_day | regular | cultural_event
    is_greeting_enabled BOOLEAN NOT NULL DEFAULT true,
    custom_greeting TEXT,                           -- override default greeting
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(school_id, festival_id, academic_year_id)
);
```

---

## 5. Backend Architecture

### 5.1 FestivalCalendarService

```kotlin
class FestivalCalendarService {
    suspend fun autoPopulate(schoolId: UUID, academicYearId: UUID, region: String): Int {
        // 1. Fetch all active festivals for school's region
        // 2. For each, calculate actual date for academic year (lunar → Gregorian)
        // 3. Create school_festival_overrides + AcademicCalendarTable entries
        // 4. Return count populated
    }

    suspend fun overrideDayType(schoolId: UUID, festivalId: UUID, dayType: String)
    suspend fun sendFestivalGreetings(): Int  // daily job at 8 AM IST
    suspend fun getUpcomingFestivals(schoolId: UUID, days: Int): List<FestivalDto>
}
```

### 5.2 Daily Greeting Job

Every day 8 AM IST:
1. Check `school_festival_overrides` for today's date
2. For each with `is_greeting_enabled = true`:
   - Render greeting template with school name + festival name
   - Send via WhatsApp (image + text) + in-app notification to all parents

### 5.3 Lunar Calendar Conversion

For Hindu/Islamic festivals, dates shift each year. Options:
- Maintain a pre-calculated date table for 2026-2030
- Use a calendar conversion library (e.g., `com.github.calimt:indian-calendar`)

---

## 6. API Contracts

```
# Admin
GET /api/v1/school/festivals?academic_year_id={uuid}
POST /api/v1/school/festivals/auto-populate  { academic_year_id, region }
PATCH /api/v1/school/festivals/{id}/override  { day_type, is_greeting_enabled, custom_greeting }

# Parent
GET /api/v1/parent/festivals/upcoming  -- next 30 days
```

---

## 7. Seed Data

Seed `festival_master` with 100+ Indian festivals:
- National: Republic Day, Independence Day, Gandhi Jayanti
- Hindu: Diwali, Holi, Navratri, Dussehra, Raksha Bandhan, Janmashtami, Ganesh Chaturthi, Makar Sankranti, Pongal, Onam, Baisakhi
- Islamic: Eid al-Fitr, Eid al-Adha, Muharram, Ramadan
- Christian: Christmas, Good Friday, Easter
- Sikh: Guru Nanak Jayanti, Baisakhi
- Regional: Bihu, Puthandu, Vishu, Ugadi, Gudi Padwa

---

## 8. Acceptance Criteria

- [ ] Festival database seeded with 100+ festivals
- [ ] Auto-populate creates calendar entries for academic year
- [ ] Admin can customize day type (holiday/half-day/regular/cultural)
- [ ] Festival greetings sent automatically on festival day
- [ ] Cultural suggestions available per festival
- [ ] Multi-language festival names
- [ ] Upcoming festivals viewable by parents

---

## 9. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration, Exposed tables |
| 2 | 2 days | Seed festival master data (100+ festivals) |
| 3 | 2 days | FestivalCalendarService (auto-populate, overrides, greetings) |
| 4 | 1 day | Daily greeting job |
| 5 | 1 day | API endpoints |
| 6 | 2 days | Client UI (festival calendar, override settings, parent upcoming view) |
| 7 | 1 day | Tests |

---

## 10. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | 2 festival tables |
| `server/.../feature/festival/FestivalCalendarService.kt` | New | Core service |
| `server/.../feature/festival/FestivalGreetingJob.kt` | New | Daily greeting job |
| `docs/db/migration_074_festival_calendar.sql` | New | DDL + seed data |
| `composeApp/.../ui/v2/screens/admin/FestivalCalendarScreen.kt` | New | Admin festival management |
| `composeApp/.../ui/v2/screens/parent/UpcomingFestivalsScreen.kt` | New | Parent view |
