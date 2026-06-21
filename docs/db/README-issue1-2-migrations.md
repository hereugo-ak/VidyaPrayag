# Run guide — ISSUE 1 & ISSUE 2 migrations (005 + 006)

This is the SQL runbook for the two structural fixes shipped on
`backend-by-abuzar_v1.0.3`:

- **ISSUE 1** — Teacher ⇄ Student ⇄ Subject ⇄ Class never actually connected.
- **ISSUE 2a** — `student_code` formats were inconsistent.
- **ISSUE 2b** — Admin student-creation must capture the parent phone number.
- **ISSUE 2c / 2d** — Parent → child link redesign + full-match vs. needs-review buckets.

You only need to run **two** files, **in this order**:

| Order | File | What it does |
|---|---|---|
| 1 | `migration_005_class_normalization_and_student_code_standard.sql` | Adds `students.parent_phone`; canonicalises existing `class_name`/`section` on `students` **and** `teacher_subject_assignments` so the derived join holds (ISSUE 1); regenerates **every** `student_code` into the standard `<CLASS_TOKEN><SECTION>-<ROLL3>` and cascades the new code to all referencing tables (ISSUE 2a). |
| 2 | `migration_006_parent_link_review_fields.sql` | Adds `class_name`, `section`, `parent_phone`, `review_reason` to `parent_child_links`; widens `status` to hold `needs_review`; adds a `(school_id, status)` index (ISSUE 2c/2d). |

> These run **after** your existing base schema + `migration_001…004`. If your
> DB is already live (it is), you do **not** re-run the base schema — just run
> 005 then 006.

## How to run (Supabase → SQL Editor)

1. Open **Supabase → SQL Editor → New query**.
2. Paste the entire contents of **`migration_005_…sql`**, click **Run**.
3. Paste the entire contents of **`migration_006_…sql`**, click **Run**.

That's it. Each file wraps everything in a single `BEGIN; … COMMIT;` so a
failure rolls back cleanly — you never end up half-applied.

## Will it error if the tables/columns already exist? — No.

Both files were written specifically to be **idempotent and safe to re-run**.
Every create/alter is guarded with an `IF` condition:

- **New columns** use `ADD COLUMN IF NOT EXISTS` — re-running never errors if the
  column is already there.
- **Helper functions** (005) use `CREATE OR REPLACE FUNCTION` — replaced, never
  duplicated, and dropped again at the end.
- **Indexes** use `CREATE INDEX IF NOT EXISTS`.
- **Optional dependent tables** (005 cascade) are each wrapped in a
  `to_regclass('public.<table>') IS NOT NULL` check inside a `DO` block, so the
  migration still runs cleanly on a deployment that doesn't have, say,
  `assessment_marks` yet.
- **The `status` widen** (006) only runs `ALTER COLUMN … TYPE varchar(24)` when
  the current column is a length-limited varchar **narrower than 24** (checked
  via `information_schema.columns`). If it's already wide enough — or already
  `text` — it's a no-op.
- **Re-running 005 a second time is harmless:** class names are already
  canonical (the `IS DISTINCT FROM` guards skip them) and codes are already in
  the standard, so the regenerated value equals the stored one (zero rows
  change).

## What the new `student_code` standard looks like

`<CLASS_TOKEN><SECTION>-<ROLL3>`

- **CLASS_TOKEN** — numeric class → `G` + number (`4` → `G4`); named class →
  up to 4 upper-case alphanumerics (`Nursery` → `NUR`).
- **SECTION** — upper-cased; blank becomes `A`.
- **ROLL3** — numeric roll zero-padded to 3 (`7` → `007`); mixed/alphanumeric
  roll → up to 6 upper-case alphanumerics.

Examples: `G4A-007`, `NURA-003`, `G10B-012`. Collisions get a `-2`, `-3` … suffix.

## Quick verification after running

```sql
-- 1) parent_phone column exists on students:
SELECT column_name FROM information_schema.columns
WHERE table_name = 'students' AND column_name = 'parent_phone';

-- 2) all student codes now match the standard:
SELECT student_code FROM students
WHERE student_code !~ '^[A-Z0-9]{1,5}[A-Z0-9]+-[A-Z0-9]{3,6}(-[0-9]+)?$'
LIMIT 20;            -- expect 0 rows

-- 3) parent_child_links has the new review columns + can hold needs_review:
SELECT column_name, data_type, character_maximum_length
FROM information_schema.columns
WHERE table_name = 'parent_child_links'
  AND column_name IN ('class_name','section','parent_phone','review_reason','status');

-- 4) a class+section now matches between the two ends (ISSUE 1 sanity):
SELECT s.school_id, s.class_name, s.section, count(*) AS students
FROM students s
JOIN teacher_subject_assignments t
  ON t.school_id = s.school_id
 AND t.class_name = s.class_name
 AND t.section   = s.section
GROUP BY 1,2,3
ORDER BY 1,2,3;
```

If query (2) returns rows, they are pre-existing edge cases (e.g. a roll number
that is entirely non-alphanumeric) — send them over and we'll extend the token
rules; the migration will not have errored.
