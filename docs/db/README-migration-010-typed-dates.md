# Migration 010 — Typed Dates (run guide)

**Task:** Teacher Portal Rebuild · Doc 11 **T-004** · closes root defect **X-3**
**File:** [`docs/db/migration_010_typed_dates.sql`](./migration_010_typed_dates.sql)
**Branch:** `backend-by-abuzar_v1.0.3` · **Commit:** `364146a`

---

## 1. What this migration does

It promotes six columns that *are* dates but were stored as `varchar(12)` ISO
text (`"YYYY-MM-DD"`) to the native Postgres **`date`** type, and the matching
Kotlin/Exposed mappings (`db/Tables.kt`) from `varchar(...)` to `date(...)`
(which Exposed reads/writes as `java.time.LocalDate`).

| Table | Column | Nullable | Before | After |
|---|---|---|---|---|
| `attendance_records` | `date` | NO | `varchar(12)` | `date` |
| `assessments` | `exam_date` | YES | `varchar(12)` | `date` |
| `homework` | `due_date` | NO | `varchar(12)` | `date` |
| `syllabus_units` | `covered_on` | YES | `varchar(12)` | `date` |
| `calendar_events` | `start_date` | NO | `varchar(12)` | `date` |
| `calendar_events` | `end_date` | NO | `varchar(12)` | `date` |

### Why
String dates force every read site to `LocalDate.parse()`, turn every range
query into a lexical string compare (only correct because the format happened to
be zero-padded ISO), make ordering string-ordering, and let a malformed value
silently break a screen instead of failing loudly. Native `date` makes the
**database** the source of truth for date semantics: real ordering, real range
predicates, real NULL handling, and a hard guarantee that no non-date text can
ever enter these columns again.

### Explicitly **out of scope** (left as `varchar`)
- `academic_years.start_date` / `academic_years.end_date`
- `fee_records.due_date`

These are not in the T-004 column list and were deliberately **not** touched.

---

## 2. The wire contract did not change

Every API still serializes these dates as `"YYYY-MM-DD"` ISO strings — the
backend now does `LocalDate.toString()` at the read boundary (which produces
exactly that format) instead of returning the raw varchar. **No mobile/web
client change is required.**

---

## 3. How to run

### Supabase (recommended)
1. Open **Supabase → SQL Editor**.
2. Paste the entire contents of `docs/db/migration_010_typed_dates.sql`.
3. **Run.**

### psql
```bash
psql "$DATABASE_URL" -f docs/db/migration_010_typed_dates.sql
```

### Run order
Append after `009`, before the seed:
```
...
8.  docs/db/migration_008_enrollments.sql
9.  docs/db/migration_009_tsa_fks.sql
10. docs/db/migration_010_typed_dates.sql   <-- this file
    scripts/seed-2026-06-07.sql
```
(Already registered in `docs/db/PROVISION.sql`.)

---

## 4. Safety properties

- **Idempotent / re-runnable.** Each `ALTER ... TYPE date` is guarded by an
  `information_schema.columns` check that skips the column if it is *already*
  `date`. Re-running on a converted DB is a clean no-op.
- **Nothing is destroyed.** Any value that is not a valid ISO date is first
  copied into a quarantine table `public.vp_bad_dates_010`
  (`table_name, column_name, row_id, bad_value, noted_at`) with
  `ON CONFLICT DO NOTHING`.
- **Nullable columns** (`exam_date`, `covered_on`): bad text is logged, then set
  to `NULL`, then the column is converted.
- **NOT-NULL columns** (`attendance_records.date`, `homework.due_date`,
  `calendar_events.start_date/end_date`): if any invalid text is present the
  migration **aborts the whole transaction** with `RAISE EXCEPTION` rather than
  silently coercing data. You must triage those rows (see below) and re-run.
- Real calendar validity is enforced via `vp_is_iso_date()`, which actually
  casts to `date` under a sub-transaction — so `2026-02-31` / `2026-13-01` are
  rejected, not just regex-matched.

---

## 5. Verification (after running)

**A. All six columns are now `date`:**
```sql
SELECT table_name, column_name, data_type
FROM information_schema.columns
WHERE (table_name, column_name) IN (
  ('attendance_records','date'), ('assessments','exam_date'),
  ('homework','due_date'), ('syllabus_units','covered_on'),
  ('calendar_events','start_date'), ('calendar_events','end_date')
)
ORDER BY table_name, column_name;
-- expect data_type = 'date' for every row
```

**B. Bad-row report (T-004 "done when": empty or triaged):**
```sql
SELECT table_name, column_name, count(*)
FROM public.vp_bad_dates_010
GROUP BY 1,2 ORDER BY 1,2;
-- ideally 0 rows; any rows are values that were not valid ISO dates
```

The migration also `RAISE NOTICE`s a one-line summary at the end (clean vs. N
rows quarantined).

---

## 6. If the migration ABORTS on a NOT-NULL column

It means a `attendance_records.date` / `homework.due_date` /
`calendar_events.start_date|end_date` row holds text that is not a valid date.
The offending rows are already logged. Triage them, then re-run.

```sql
-- 1. See what's wrong
SELECT * FROM public.vp_bad_dates_010
WHERE column_name IN ('date','due_date','start_date','end_date')
ORDER BY table_name, column_name;

-- 2. Fix each row in place, e.g.:
--    UPDATE public.homework SET due_date = '2026-03-15' WHERE id = '<row_id>';
--    -- or delete the junk row if it is genuinely garbage:
--    DELETE FROM public.homework WHERE id = '<row_id>';

-- 3. Re-run docs/db/migration_010_typed_dates.sql (safe to re-run).
```

---

## 7. Rollback (only if you must)

Converting back to text is lossless (every `date` renders as ISO):
```sql
BEGIN;
ALTER TABLE public.attendance_records ALTER COLUMN date       TYPE varchar(12) USING to_char(date,'YYYY-MM-DD');
ALTER TABLE public.assessments        ALTER COLUMN exam_date  TYPE varchar(12) USING to_char(exam_date,'YYYY-MM-DD');
ALTER TABLE public.homework           ALTER COLUMN due_date   TYPE varchar(12) USING to_char(due_date,'YYYY-MM-DD');
ALTER TABLE public.syllabus_units     ALTER COLUMN covered_on TYPE varchar(12) USING to_char(covered_on,'YYYY-MM-DD');
ALTER TABLE public.calendar_events    ALTER COLUMN start_date TYPE varchar(12) USING to_char(start_date,'YYYY-MM-DD');
ALTER TABLE public.calendar_events    ALTER COLUMN end_date   TYPE varchar(12) USING to_char(end_date,'YYYY-MM-DD');
COMMIT;
```
You would also need to revert `db/Tables.kt` (`date(...)` → `varchar(...)`) and
the 13 consumer edits. Prefer fixing forward.

---

## 8. Backend code changes that shipped with this migration

Because flipping the Exposed column types changes their Kotlin type from
`String` to `LocalDate`, every consumer had to be updated in the **same commit**
to keep the branch compiling (Rule 4). The transformation rule was uniform:

| Site kind | Before | After |
|---|---|---|
| read → String DTO / String helper / String map key | `row[col]` | `row[col].toString()` (nullable: `row[col]?.toString()`) |
| write of a String input | `it[col] = s` | `it[col] = LocalDate.parse(s)` |
| "today" write | `it[col] = todayIso()` | `it[col] = LocalDate.now()` |
| column comparison (`eq` / `greaterEq` / `isAfter`) | compared to a `String` var | compared to a parsed `LocalDate` local |

**14 files touched** (13 edited; `StudentAggregationService.kt` needed no change
because its only date read is inside a string template):
`feature/calendar/AcademicCalendar{Core,Routing}.kt`,
`feature/teacher/TeacherRouting{,Tasks}.kt`,
`feature/parent/ParentAcademicsRouting.kt`,
`feature/school/{SchoolAnalytics,SchoolIntelligence,SchoolRecords,SchoolRouting,SchoolStudents,TeacherProvisioning,AdminDashboardOverview,AdminDashboard}Routing.kt`.

---

## 9. Filename deviation (flagged per LAWS — docs are authority)

Doc 11 named this file `migration_012_typed_dates.sql`. The real chain in
`docs/db/` is contiguous and the previous migration is `009`, so it lands as
`migration_010_typed_dates.sql`. This is the same deviation pattern already
documented on `migration_009` (which the doc called `011`). The planning
decision (convert these columns to `date`) is honored exactly; only the filename
index is adjusted to keep the on-disk chain contiguous.
