# Supabase Setup — the "No Error" Guide

> **Goal:** stand up the VidyaPrayag/VidyaSetu backend on **Supabase Postgres**
> from a clean project, with **zero boot errors** and **zero `relation does not
> exist` failures** at runtime.
>
> This is the single document to follow. It fixes the gap in
> `scripts/README-RUN-ORDER.md` (which only mentions 2 SQL files and **omits the
> 2026‑06‑08 part‑2 patch**). Skipping that patch is the #1 cause of the server
> refusing to boot — see [Why it errored before](#why-it-errored-before).

---

## TL;DR — pick ONE path

| Path | When to use | Effort | Risk of errors |
|------|-------------|--------|----------------|
| **A — Let the server create tables** (`AUTO_CREATE_TABLES=true`) | Fastest. You just want it running. | 1 env var | **Lowest** — Exposed creates all 41 tables from `Tables.kt`, so schema can never drift from code. |
| **B — Run the SQL scripts yourself** (strict mode) | You want full control / a managed migration trail. | 3 SQL files in order | Low **if** you run **all three** files. Missing the part‑2 patch = boot failure. |

If you are unsure, **use Path A.** It is the no‑error path by design.

---

## 0. Prerequisites (2 minutes)

1. A Supabase project: <https://supabase.com/dashboard> → **New project**.
2. The Postgres connection string. In Supabase: **Project Settings → Database →
   Connection string → URI**. It looks like:

   ```
   postgresql://postgres.[ref]:[YOUR-PASSWORD]@aws-0-[region].pooler.supabase.com:6543/postgres
   ```

   - Use the **Session / Transaction pooler** URI (port `6543`) for the app.
   - **URL‑encode** any special characters in the password (`@` → `%40`,
     `#` → `%23`, etc.). An un‑encoded password is a very common silent
     "can't connect" cause.

3. A strong `JWT_SECRET` (any random 256‑bit string). Generate one:

   ```bash
   openssl rand -base64 48
   ```

---

## Path A — `AUTO_CREATE_TABLES=true` (recommended, no SQL needed)

The server's `DatabaseFactory.init()` (see `server/.../db/DatabaseFactory.kt`)
reads `AUTO_CREATE_TABLES`. When it is `true` **on Postgres**, it runs
`SchemaUtils.createMissingTablesAndColumns(*allTables)` for **all 41 tables**
defined in `server/.../db/Tables.kt`. Because the schema comes straight from the
code, it can **never** be missing a table the code needs — so `validateSchema()`
always passes and the server boots clean.

### Steps

1. Set these environment variables (Render dashboard → **Environment**, or local
   `.env` at the repo/server root):

   ```env
   DATABASE_URL=postgresql://postgres.[ref]:[ENCODED-PW]@aws-0-[region].pooler.supabase.com:6543/postgres
   JWT_SECRET=<paste the openssl output>
   AUTO_CREATE_TABLES=true
   APP_SEED_CMS=true          # seeds landing-page CMS + app_config (idempotent)
   OTP_PROVIDER=mock          # so parent OTP works without a real SMS gateway
   ```

2. Start the server (Render: just deploy; local: `./gradlew :server:run`).

3. In the logs you should see, in order:

   ```
   DB_INIT: isPostgres=true, AUTO_CREATE_TABLES='true' -> true
   DB_INIT: Running SchemaUtils.createMissingTablesAndColumns for 41 tables...
   DB_INIT: Schema check/creation completed.
   DB_INIT: Running CMS seed...
   DB_INIT: CMS seed completed successfully.
   ```

4. **Done.** The schema is live and complete. If you also want the demo data
   (schools/admins/teachers/parents), run the seed once — see
   [Loading the demo data](#loading-the-demo-data-optional).

> **After the first successful boot you may flip `AUTO_CREATE_TABLES=false`.**
> The tables already exist; strict mode then just *validates* them on every
> boot. (Optional — leaving it `true` is also fine and stays self‑healing.)

---

## Path B — run the SQL scripts yourself (strict mode)

Use this if you want `AUTO_CREATE_TABLES=false` (or unset) and a managed
migration trail. **You must run THREE files, in this exact order.** This is the
part the old `README-RUN-ORDER.md` got wrong.

| Step | File | What it does |
|------|------|--------------|
| 1 | `scripts/schema-all-in-one-2026-06-07.sql` | Creates **39** core tables (`CREATE TABLE IF NOT EXISTS`). |
| 2 | `scripts/schema-patch-2026-06-08-part2.sql` | **Adds the 3 missing tables + 5 columns** the server now requires. **DO NOT SKIP.** |
| 3 | `scripts/seed-2026-06-07.sql` | Inserts 2 schools + ~15 users + demo data (optional but recommended). |

> The standalone `scripts/schema-patch-2026-06-07.sql` is already folded into the
> all‑in‑one file — **do not run it separately**. Only the **part‑2** patch
> (step 2) is still required on top of the all‑in‑one.

### What step 2 adds (and why boot fails without it)

`schema-patch-2026-06-08-part2.sql` is **idempotent** (every statement uses
`IF NOT EXISTS` / `IF EXISTS`), so re‑running it is a safe no‑op. It adds:

| Patch | Object | Mirrors in `Tables.kt` |
|-------|--------|------------------------|
| PATCH‑101 | `app_users.must_change_password` column | RA‑54 teacher first‑login |
| PATCH‑102 | `assessments.is_published`, `assessments.published_at` columns | RA‑43 marks workflow |
| PATCH‑103 | `leave_requests` cross‑role columns (`class_id`, `class_name`, `section`, `teacher_id`, `child_id`, `parent_id`) | RA‑44 |
| PATCH‑104 | **`notifications` table** | RA‑41/42/46/50 |
| PATCH‑105 | **`device_tokens` table** | RA‑41 push |
| PATCH‑106 | **`parent_child_links` table** | RA‑48 |

The server's boot gate `DatabaseFactory.validateSchema()` checks that **every**
table in `Tables.kt` exists. In strict mode (`AUTO_CREATE_TABLES` ≠ `true`), a
single missing table — e.g. `notifications` — makes it **hard‑fail the boot**
on purpose, so a half‑provisioned DB can't silently 500 at request time. The
all‑in‑one file alone is missing those 3 tables ⇒ boot fails ⇒ that's the error
you were hitting.

### Steps (Supabase SQL Editor)

1. Supabase dashboard → **SQL Editor** → **+ New query**.
2. Open `scripts/schema-all-in-one-2026-06-07.sql`, **Ctrl/Cmd+A → C**, paste
   into the editor, click **Run**. Expect `tables_present | 23` (the file's own
   verification count) and no red errors.
3. **+ New query** → paste `scripts/schema-patch-2026-06-08-part2.sql` → **Run**.
   The trailing `VERIFY` SELECTs should each return the object name (no errors).
4. **+ New query** → paste `scripts/seed-2026-06-07.sql` → **Run** (optional —
   see next section). Check the verification rows at the bottom.
5. Set env (no `AUTO_CREATE_TABLES`, or `=false`):

   ```env
   DATABASE_URL=postgresql://...:6543/postgres
   JWT_SECRET=<openssl output>
   OTP_PROVIDER=mock
   ```

6. Boot. Logs should show:

   ```
   DB_INIT: isPostgres=true, AUTO_CREATE_TABLES='null' -> false
   DB_INIT: Skipping auto-creation (AUTO_CREATE_TABLES is not 'true').
   ```

   …with **no** `validateSchema` failure following it. If you see a
   `MISSING TABLE` error, you skipped step 3 (the part‑2 patch) — run it and
   reboot.

---

## Loading the demo data (optional)

`scripts/seed-2026-06-07.sql` inserts 2 schools + ~15 users + demo children/
classes/assignments. It's safe to run **once** on either path. After running,
the sanity rows at the bottom must all read `0`:

- `orphan_parents_with_school | 0`
- `children_without_school | 0`
- `mismatched_child_school | 0`

All three `0` ⇒ multi‑tenancy is intact.

### Test logins (from `seed-credentials-2026-06-07.md`)

| Role | Login | Credentials |
|------|-------|-------------|
| School admin (Sunrise) | `POST /api/v1/auth/login` | `admin@sunrise.edu.in` / `Sunrise@2026` |
| School admin (Greenfield) | `POST /api/v1/auth/login` | `admin@greenfield.edu.in` / `Greenfield@2026` |
| Teacher (Sunrise) | `POST /api/v1/auth/login` | `meena.sharma@sunrise.edu.in` / `Teacher@2026` |
| Parent (OTP only) | `send-otp` → `verify-otp` | phone `+919811100001` (Rohit Singh) |

**Parent OTP flow** (parents have **no password** — OTP only):

```http
POST /api/v1/auth/send-otp     { "identifier": "+919811100001", "purpose": "login" }
POST /api/v1/auth/verify-otp   { "identifier": "+919811100001", "code": "<otp>", "purpose": "login" }
```

Where do you get `<otp>`?

- **Local / no `DATABASE_URL`** → the `send-otp` response echoes it in `dev_code`.
- **Production (Supabase `DATABASE_URL` set)** → for security (RA‑33) the
  `dev_code` is **suppressed** in the response. Read the OTP from the server
  **logs** instead — with `OTP_PROVIDER=mock` the console provider prints the
  code. (On Render: **Logs** tab.) To make a real SMS go out, set
  `OTP_PROVIDER=msg91|twilio|gupshup` and that provider's keys.

---

## Environment variable reference

| Var | Required? | Example | Notes |
|-----|-----------|---------|-------|
| `DATABASE_URL` | **Yes** (for Postgres/Supabase) | `postgresql://...:6543/postgres` | Presence of this is the app's "production" signal. URL‑encode the password. |
| `JWT_SECRET` | **Yes in production** | `openssl rand -base64 48` | The server **refuses to boot** with a missing/default secret when `DATABASE_URL` is set. |
| `AUTO_CREATE_TABLES` | Path A: `true` · Path B: unset/`false` | `true` | `true` = server creates/upgrades all 41 tables (no‑error path). |
| `APP_SEED_CMS` | No (default `true`) | `true` | Seeds landing CMS + `app_config`; idempotent. |
| `OTP_PROVIDER` | No (default `mock`) | `mock` | `mock` prints OTP to logs; real values: `msg91`, `twilio`, `gupshup`. |
| `DATABASE_USER` / `DATABASE_PASSWORD` | Only if not embedded in the URL | — | Optional override. |
| `DB_POOL_SIZE` | No (default `5`) | `5` | HikariCP pool size. |
| `CORS_ALLOWED_ORIGINS` | No | `https://app.example.com` | In prod, locks CORS to this allow‑list. |

---

## Why it errored before

1. `README-RUN-ORDER.md` told you to run only **2** files (all‑in‑one + seed).
2. The all‑in‑one file creates **39** tables but **not** `notifications`,
   `device_tokens`, or `parent_child_links` (added later by the audit work).
3. `Tables.kt` declares **41** tables and `validateSchema()` enforces all of
   them at boot.
4. In strict mode the 3 missing tables ⇒ **boot hard‑fails** with a
   `MISSING TABLE` message; in lenient runtime paths the dependent routes would
   `500` with `relation "notifications" does not exist`.

**Both fixes here close that gap:** Path A removes the manual SQL entirely;
Path B adds the missing step 2 (`schema-patch-2026-06-08-part2.sql`).

---

## Troubleshooting quick table

| Symptom | Cause | Fix |
|---------|-------|-----|
| Boot log: `validateSchema ... MISSING TABLE: notifications` | Ran all‑in‑one but skipped the part‑2 patch | Run `schema-patch-2026-06-08-part2.sql`, reboot. Or set `AUTO_CREATE_TABLES=true`. |
| Runtime `relation "X" does not exist` | Same gap, lenient mode | Same fix. |
| `FATAL: JWT_SECRET must be set ...` | Prod (`DATABASE_URL` set) without a real `JWT_SECRET` | Set a strong `JWT_SECRET`. |
| Can't connect / auth failed | Unencoded password in `DATABASE_URL` | URL‑encode special chars (`@`→`%40`). |
| `relation "X" already exists` | Earlier botched run | Reset the DB (Supabase → Settings → Database → reset) and re‑run from step 1; everything is `IF NOT EXISTS`, so this is rare. |
| Parent login: no `dev_code` in response | You're in production (Supabase) — RA‑33 suppresses it | Read the OTP from server **Logs** (`OTP_PROVIDER=mock`) or use a real SMS provider. |
| CMS seed warning `tables are missing` | Tables not created yet | Use Path A, or run schema files (Path B) before first boot. |

---

## One‑glance checklist

- [ ] Supabase project created; pooled `DATABASE_URL` copied; password URL‑encoded.
- [ ] `JWT_SECRET` set to a strong random value.
- [ ] **Path A:** `AUTO_CREATE_TABLES=true` → boot → see `41 tables` log. **Done.**
- [ ] **Path B:** ran all‑in‑one **→ part‑2 patch →** (optional) seed, then boot
      with `AUTO_CREATE_TABLES` unset/`false` → no `validateSchema` failure.
- [ ] (Optional) seed loaded; the three `must‑be‑0` rows are `0`.
- [ ] Logged in as `admin@sunrise.edu.in` / `Sunrise@2026` and completed a parent
      OTP flow (OTP from `dev_code` locally, or from logs in prod).
