# Parents Portal — Profile tab "Missions & Achievements" backend

This note explains the data backing the redesigned **Profile tab** (the swipe-down
*Missions & Achievements* sheet) and exactly what — if anything — you need to do in Supabase.

## TL;DR — do you need to run any SQL?

**No, not to make the feature work.** The Profile sheet is served by an endpoint that
already exists and already has a working data source. Running the optional migration only
*upgrades* it from CMS-wide templates to **real, per-child** achievements.

| You want… | Action |
| --- | --- |
| The sheet to just work (CMS / derived data) | Nothing — it already works |
| Real per-child achievements stored in the DB | Run `migration_004_parent_achievements.sql` (optional, idempotent) |

## How the data flows today (no new table needed)

The sheet is driven by **`GET /api/v1/parent/track-progress`**
(`server/.../feature/parent/TrackProgressRouting.kt`). It composes its response from:

1. **`children`** — `overall_progress` + `current_level` → the hero "journey" line.
2. **`app_config`** (CMS rows) — the badges, NEP competencies, EI metrics and play/discovery
   missions. Operators edit these from Supabase without redeploying. Relevant keys:
   - `parent_track_badges`
   - `parent_track_academic_competencies`
   - `parent_track_academic_label`
   - `parent_track_ei_description`
   - `parent_track_ei_metrics`
   - `parent_track_play_discovery`
   - `parent_track_journey_description`

If a key is missing, the endpoint uses sensible built-in defaults, and the app additionally
falls back to honest, locally-derived achievements (e.g. "On track" from real attendance %).
**So the sheet is never empty and never errors — with or without the optional table.**

## Optional upgrade — real per-child achievements

If you'd rather store genuine, per-child earned achievements (instead of CMS-wide templates):

1. Open **Supabase → SQL Editor**.
2. Paste **all** of `docs/db/migration_004_parent_achievements.sql` and click **Run**.
3. (Optional) Uncomment the seed block at the bottom and set a real `children.id`.

### Why it's "pure, error-free run"

Every statement is guarded:

- `CREATE TABLE IF NOT EXISTS public.parent_achievements (...)`
- `ALTER TABLE ... ADD COLUMN IF NOT EXISTS ...` for every column
- `CREATE INDEX IF NOT EXISTS ...`

So if the table is **already** in your Supabase, re-running is a **harmless no-op** — it will
not raise an error. The server-side Exposed mapping (`ParentAchievementsTable` in
`server/.../db/Tables.kt`) matches the SQL column-for-column, and on the local SQLite dev
fallback Exposed auto-creates it.

### Table shape

`parent_achievements` uses a single `kind` discriminator so one table powers every section:

| `kind` | Sheet section | Key fields |
| --- | --- | --- |
| `BADGE` | Achievements badges | `title`, `icon`, `colors`, `is_locked`, `status` (EARNED/LOCKED) |
| `COMPETENCY` | Academic core bars | `title`, `progress` (0..1) |
| `EI_METRIC` | Emotional intelligence | `title`, `progress` (0..1) |
| `MISSION` | Play & discovery | `title`, `description`, `status` (MET/IN_PROGRESS/LOCKED) |

> Note: wiring `track-progress` to read this table (instead of `app_config`) is a small,
> optional follow-up in `TrackProgressRouting.kt`. The schema, Exposed mapping, and migration
> are in place so that change is a drop-in when you want per-child data.
