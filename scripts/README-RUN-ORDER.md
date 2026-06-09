# How to load the database — read this first

If you only read one file, read this one.

You're going to run these files in the Supabase SQL Editor, in this order:

| Step | File | What it does |
|------|------|--------------|
| 1 | `scripts/schema-all-in-one-2026-06-07.sql` | Creates every table the backend + seed need (39 CREATE TABLE IF NOT EXISTS). |
| 2 | `scripts/schema-patch-2026-06-08-part2.sql` | Adds `children.student_code` (required by the expansion seed). |
| 3 | `scripts/seed-2026-06-07.sql` | Inserts 2 schools + ~15 users + filler data. |
| 4 | `scripts/seed-expansion-2026-06-09.sql` | **(new 2026-06-09)** Recovers the original parents, expands schools 1 & 2, and adds a **complete School 3** (Holy Trinity Convent: 6 classes / 9 sections / 10 teachers / ~36 students / 12 parents). Idempotent + additive. |

That's it. Don't run `PROVISION.sql` — it's documentation only. Don't run the 5 individual schema files one-by-one — they're already bundled into the all-in-one above.

> **Step 4 is idempotent and additive** — every INSERT uses `ON CONFLICT DO NOTHING`,
> wrapped in `BEGIN; ... COMMIT;`. Run it as many times as you like; it never
> touches an existing row. If a teammate ever deletes the parent rows again, just
> re-run step 4 to restore them (same deterministic UUIDs).

---

## Step-by-step

### 1. Open Supabase SQL Editor

1. https://supabase.com/dashboard → your project
2. Left sidebar → **SQL Editor**
3. Top-left → **+ New query**

### 2. Run `schema-all-in-one-2026-06-07.sql`

1. In Android Studio (or any text editor), open `scripts/schema-all-in-one-2026-06-07.sql`
2. **Ctrl+A** to select all, **Ctrl+C** to copy
3. Switch to Supabase SQL Editor, **Ctrl+V** to paste
4. Click **Run** (bottom right)
5. Wait ~5–10 seconds
6. You should see a result at the bottom: `tables_present | 23`
   - **If you see 23 → ✅ all good, move to step 3**
   - If you see a smaller number, scroll up in the output and find the first red error — paste it back so we can fix it
   - If you see `Success. No rows returned` and no count → scroll up; the verification SELECT might have been split — re-run just the final `SELECT 'tables_present' …` block on its own to confirm

**If you get `ERROR: relation "X" already exists`**:
That should be impossible because we use `IF NOT EXISTS` everywhere. If it happens, that table was probably created by an earlier botched run with a slightly different schema. Either:
- Drop the database and start fresh in Supabase (Settings → Database → reset), then re-run step 2; OR
- Tell me which table and I'll patch the file.

### 3. Run `seed-2026-06-07.sql`

1. Back in Supabase SQL Editor, click **+ New query** for a fresh tab
2. Open `scripts/seed-2026-06-07.sql`, **Ctrl+A** → **Ctrl+C**
3. Paste into the new Supabase query
4. Click **Run**
5. Wait ~5–15 seconds (it's a big file, 1011 INSERTs)
6. At the bottom you should see a results table with rows like:
   - `schools | 2`
   - `app_users | ≈15`
   - `children | 11`
   - `orphan_parents_with_school | 0` ← must be 0
   - `children_without_school | 0` ← must be 0
   - `mismatched_child_school | 0` ← must be 0

All three "must be 0" rows being 0 means **multi-tenancy is intact** in your seed.

### 4. Sanity check (optional but recommended)

In a fresh Supabase query, run:

```sql
SELECT email, role FROM app_users
  WHERE role IN ('admin','super_admin','teacher')
  ORDER BY role, email;

SELECT name FROM schools ORDER BY name;
```

Expected output:
- 2 schools (Sunrise + Greenfield, or whatever the seed names them)
- 2 admins (`admin@sunrise.edu.in`, `admin@greenfield.edu.in`)
- 7 teachers (4 Sunrise + 3 Greenfield)

---

## What if I already partially ran the OLD broken `vidyasetu_schema.sql`?

The earlier version had a bug (`academic_calender:` label crashed the parse). If you ran the OLD file and hit the error you saw, **the failed CREATE TABLE statement before the error did NOT execute**, but everything BEFORE that line might have. So your database could be in a half-baked state.

Two ways to recover:

**Easy way** — drop and re-create the public schema in Supabase, then run the two files above:

```sql
-- DANGER: nukes ALL tables in the public schema. Only do this on dev/staging.
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO public;
```

Then re-run step 2 and step 3 above.

**Conservative way** — just re-run the all-in-one (step 2). Because every CREATE uses `IF NOT EXISTS`, it will skip tables that already exist and create the rest. Then run the seed (step 3); since every INSERT uses `ON CONFLICT DO NOTHING`, it will skip rows that already exist.

The easy way is recommended on a fresh dev database. The conservative way is fine if you don't want to lose existing data.

---

## Credentials

After step 3 succeeds, open **`seed-credentials-2026-06-07.md`** in the repo root for all test logins.

Quick summary:

| Role | Email | Password |
|------|-------|----------|
| Sunrise admin | `admin@sunrise.edu.in` | `Sunrise@2026` |
| Greenfield admin | `admin@greenfield.edu.in` | `Greenfield@2026` |
| Holy Trinity admin (School 3, step 4) | `admin@holytrinity.edu.in` | `Trinity@2026` |
| All seeded teachers | see file | `Teacher@2026` |
| All seeded parents | OTP only (phones in file) | — |

For parent OTP login, set `OTP_DEV_RETURN_CODE=true` on Render (non-production env)
so the dev code is returned in the `/send-otp` API response — it is also printed
to the Render logs by `ConsoleProvider`. **The code cannot be read from the
Supabase `auth_otps` table** (it is stored hashed). See the *§ Retrieving the
parent OTP while testing* section in `seed-credentials-2026-06-07.md` for the
full step-by-step.
