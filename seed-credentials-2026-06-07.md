# Seed Credentials — 2026-06-07

> **2026-06-09 update:** the parent credential tables were accidentally deleted
> and have been **restored** below. We also added a large **expansion set** —
> more parents/teachers/subjects for schools 1 & 2 (*§ Expansion (2026-06-09)*),
> **plus a complete new School 3 "Holy Trinity Convent"** with 6 classes / 9
> sections / 10 teachers / ~36 students / 12 parents (*§ NEW School 3*).
> Everything is inserted by **`scripts/seed-expansion-2026-06-09.sql`** (generated
> by `scripts/_gen_seed_expansion.py`) — **idempotent + additive**, and it also
> re-creates the original parents from the same deterministic UUIDs, so a future
> accidental delete is recoverable by simply re-running it.

> Source of truth for every test account inserted by
> `scripts/seed-2026-06-07.sql`. **No application code reads this file.** The
> credentials are *not* hardcoded anywhere in the codebase — they live in the
> seed alone, and the seed inserts only the PBKDF2 hash (never the plaintext).
> This document is the only place the plaintext exists.
>
> Keep this file out of any production deploy bundle.

---

## How the accounts authenticate

| Role            | Login path                                                                     | What you supply                                          |
|-----------------|--------------------------------------------------------------------------------|----------------------------------------------------------|
| `school_admin`  | `POST /api/v1/auth/login`                                                      | `email` + `password` (the table below)                    |
| `teacher`       | `POST /api/v1/auth/login`                                                      | `email` + `password` (same shape as admin)                |
| `parent`        | `POST /api/v1/auth/send-otp` → `POST /api/v1/auth/verify-otp`                  | `phone` (no password — OTP-only path)                     |

### Parent OTP — how to "log in" with the seed

Parent accounts are seeded with `is_phone_verified = true` and **no
password**, matching the OTP-signup state the server produces. To complete
a login flow against a seeded parent:

1. `POST /api/v1/auth/send-otp` with body `{"identifier": "<phone>", "purpose": "login"}`.
2. In dev mode the server echoes the code in the response field
   `dev_code` (default behaviour while `OTP_DEV_RETURN_CODE` is unset or
   `true` — see RE-AUDIT RA-17 in `AUDIT_2026-06-07.md` for the security
   implication on production).
3. `POST /api/v1/auth/verify-otp` with body
   `{"identifier": "<phone>", "code": "<dev_code>", "purpose": "login"}`
   → returns `access_token` + `refresh_token`.

If `OTP_DEV_RETURN_CODE=false` is configured (recommended for any
internet-reachable deploy), the OTP must be retrieved from the configured
SMS/WhatsApp provider's sandbox or log stream instead — there is no way
to "guess" it from this file. The seed never inserts an `auth_otps` row
(those are produced live on every `send-otp` call).

---

## School 1 — Sunrise Public School

- **Slug:** `sunrise-public-school`
- **Board / Medium:** CBSE / English
- **City:** Lucknow, Uttar Pradesh
- **Brand:** `#1d4ed8`
- **School UUID:** `uuid_v5(NS, "school:s1")` — resolved deterministically at
  seed time; query with `SELECT id FROM schools WHERE slug='sunrise-public-school'`.

### Admin (1)

| Full name      | Email                          | Phone           | Password           |
|----------------|--------------------------------|-----------------|--------------------|
| Anjali Verma   | `admin@sunrise.edu.in`         | `+919876500010` | `Sunrise@2026`     |

### Teachers (4)

| Full name      | Email                                | Phone           | Password       | Assignments                                                       |
|----------------|--------------------------------------|-----------------|----------------|-------------------------------------------------------------------|
| Meena Sharma   | `meena.sharma@sunrise.edu.in`        | `+919876500020` | `Teacher@2026` | Grade 3-A — **English**, **EVS**                                  |
| Rahul Mehra    | `rahul.mehra@sunrise.edu.in`         | `+919876500021` | `Teacher@2026` | Grade 5-A — **Mathematics**, **Science**                          |
| Priya Nair     | `priya.nair@sunrise.edu.in`          | `+919876500022` | `Teacher@2026` | Grade 8-A — **English**, **Social Studies**                       |
| Arjun Khanna   | `arjun.khanna@sunrise.edu.in`        | `+919876500023` | `Teacher@2026` | Grade 8-A — **Mathematics**                                       |

### Parents (5)

| Parent name        | Phone           | Linked child(ren)                                |
|--------------------|-----------------|--------------------------------------------------|
| Rohit Singh        | `+919811100001` | Aarav Singh (Grade 3-A, roll 1)                   |
| Sunita Mishra      | `+919811100002` | Diya Mishra (Grade 3-B, roll 1)                   |
| Vikram Kapoor      | `+919811100003` | Ishan Kapoor (Grade 5-A, roll 1); Pari Agarwal (Grade 8-A, roll 4) |
| Reena Saxena       | `+919811100004` | Jiya Saxena (Grade 5-A, roll 2)                   |
| Pankaj Joshi       | `+919811100005` | Manav Joshi (Grade 8-A, roll 1); Nisha Bansal (Grade 8-A, roll 2) |

> **OTP login:** `POST /api/v1/auth/send-otp { "identifier": "+9198…", "purpose": "login" }`
> → grab `dev_code` from response → `POST /api/v1/auth/verify-otp`.

---

## School 2 — Greenfield Academy

- **Slug:** `greenfield-academy`
- **Board / Medium:** ICSE / English
- **City:** Kanpur, Uttar Pradesh
- **Brand:** `#047857`
- **School UUID:** `SELECT id FROM schools WHERE slug='greenfield-academy'`.

### Admin (1)

| Full name      | Email                              | Phone           | Password            |
|----------------|------------------------------------|-----------------|---------------------|
| Rakesh Iyer    | `admin@greenfield.edu.in`          | `+919876500110` | `Greenfield@2026`   |

### Teachers (3)

| Full name        | Email                                       | Phone           | Password       | Assignments                              |
|------------------|---------------------------------------------|-----------------|----------------|------------------------------------------|
| Kavita Iyengar   | `kavita.iyengar@greenfield.edu.in`          | `+919876500120` | `Teacher@2026` | Grade 4-A — **English**, **Mathematics** |
| Vivek Banerjee   | `vivek.banerjee@greenfield.edu.in`          | `+919876500121` | `Teacher@2026` | Grade 6-A — **Science**                  |
| Neha Goswami     | `neha.goswami@greenfield.edu.in`            | `+919876500122` | `Teacher@2026` | Grade 9-A — **English**                  |

### Parents (6)

| Parent name           | Phone           | Linked child(ren)                                                   |
|-----------------------|-----------------|---------------------------------------------------------------------|
| Anand Kumar           | `+919822200001` | Reyansh Kumar (Grade 4-A, roll 1)                                   |
| Madhuri Bose          | `+919822200002` | Saanvi Bose (Grade 4-A, roll 2)                                     |
| Subhash Das           | `+919822200003` | Udayan Das (Grade 6-A, roll 1); Yash Banerjee (Grade 9-A, roll 1)    |
| Priyanka Sen          | `+919822200004` | Vihaan Sen (Grade 6-A, roll 2)                                      |
| Soumitra Chowdhury    | `+919822200005` | Xen Chowdhury (Grade 6-B, roll 1)                                   |
| Tanvi Mukherjee       | `+919822200006` | Zara Mukherjee (Grade 9-A, roll 2); Aadya Ghosh (Grade 9-A, roll 3)  |

---

## Roster reference (canonical `students` rows)

Use these `(school, class, section, roll)` tuples when testing the parent
link-child endpoint (`POST /api/v1/parent/link-child`). The `student_code`
format is `{S1|S2}-G{grade}{section}-{roll:03d}`, e.g. `S1-G3A-001`.

| school | class    | section | roll | student_code      | full name           |
|--------|----------|---------|------|-------------------|---------------------|
| S1     | Grade 3  | A       | 1    | `S1-G3A-001`      | Aarav Singh         |
| S1     | Grade 3  | A       | 2    | `S1-G3A-002`      | Bhavya Tiwari       |
| S1     | Grade 3  | A       | 3    | `S1-G3A-003`      | Chirag Yadav        |
| S1     | Grade 3  | B       | 1    | `S1-G3B-001`      | Diya Mishra         |
| S1     | Grade 5  | A       | 1    | `S1-G5A-001`      | Ishan Kapoor        |
| S1     | Grade 5  | A       | 2    | `S1-G5A-002`      | Jiya Saxena         |
| S1     | Grade 5  | A       | 3    | `S1-G5A-003`      | Karan Trivedi       |
| S1     | Grade 5  | A       | 4    | `S1-G5A-004`      | Lavanya Pandey      |
| S1     | Grade 8  | A       | 1    | `S1-G8A-001`      | Manav Joshi         |
| S1     | Grade 8  | A       | 2    | `S1-G8A-002`      | Nisha Bansal        |
| S1     | Grade 8  | A       | 3    | `S1-G8A-003`      | Om Prakash          |
| S1     | Grade 8  | A       | 4    | `S1-G8A-004`      | Pari Agarwal        |
| S2     | Grade 4  | A       | 1    | `S2-G4A-001`      | Reyansh Kumar       |
| S2     | Grade 4  | A       | 2    | `S2-G4A-002`      | Saanvi Bose         |
| S2     | Grade 4  | A       | 3    | `S2-G4A-003`      | Tanvi Roy           |
| S2     | Grade 6  | A       | 1    | `S2-G6A-001`      | Udayan Das          |
| S2     | Grade 6  | A       | 2    | `S2-G6A-002`      | Vihaan Sen          |
| S2     | Grade 6  | A       | 3    | `S2-G6A-003`      | Wamika Dutta        |
| S2     | Grade 6  | B       | 1    | `S2-G6B-001`      | Xen Chowdhury       |
| S2     | Grade 9  | A       | 1    | `S2-G9A-001`      | Yash Banerjee       |
| S2     | Grade 9  | A       | 2    | `S2-G9A-002`      | Zara Mukherjee      |
| S2     | Grade 9  | A       | 3    | `S2-G9A-003`      | Aadya Ghosh         |
| S2     | Grade 9  | A       | 4    | `S2-G9A-004`      | Bodhi Chatterjee    |
| S2     | Grade 9  | A       | 5    | `S2-G9A-005`      | Charvi Lahiri       |
| S2     | Grade 9  | A       | 6    | `S2-G9A-006`      | Divit Pal           |
| S2     | Grade 9  | A       | 7    | `S2-G9A-007`      | Ela Bhattacharya    |

---

## Expansion (2026-06-09) — more parents, teachers & subjects

> Inserted by **`scripts/seed-expansion-2026-06-09.sql`** (generated by
> `scripts/_gen_seed_expansion.py`). The script is **idempotent + additive**:
> it re-creates the 11 original parents (recovery) AND adds everything below.
> Run order: `schema-all-in-one-2026-06-07.sql` → `schema-patch-2026-06-08-part2.sql`
> → `seed-2026-06-07.sql` → **`seed-expansion-2026-06-09.sql`**.

### New teachers (4 — login with `email` + `password`)

| School | Full name      | Email                               | Phone           | Password       | Assignments                                   |
|--------|----------------|-------------------------------------|-----------------|----------------|-----------------------------------------------|
| S1     | Sunil Rastogi  | `sunil.rastogi@sunrise.edu.in`      | `+919876500024` | `Teacher@2026` | Grade 3-A **Hindi**; Grade 5-A **Computer Science** |
| S1     | Geeta Bhatt    | `geeta.bhatt@sunrise.edu.in`        | `+919876500025` | `Teacher@2026` | Grade 8-A **Physical Education**              |
| S2     | Farah Khan     | `farah.khan@greenfield.edu.in`      | `+919876500123` | `Teacher@2026` | Grade 4-A **Hindi**; Grade 6-A **Computer Science** |
| S2     | Deepak Rao     | `deepak.rao@greenfield.edu.in`      | `+919876500124` | `Teacher@2026` | Grade 9-A **Physical Education**              |

### New subjects (6 — attached to existing classes)

| School | Class    | Subject              | `sub_code` |
|--------|----------|----------------------|------------|
| S1     | Grade 3  | Hindi                | `HIN`      |
| S1     | Grade 5  | Computer Science     | `COM`      |
| S1     | Grade 8  | Physical Education   | `PHY`      |
| S2     | Grade 4  | Hindi                | `HIN`      |
| S2     | Grade 6  | Computer Science     | `COM`      |
| S2     | Grade 9  | Physical Education   | `PHY`      |

### New students (6 — fresh roll numbers, reuse existing classes)

| School | Class    | Section | Roll | `student_code` | Full name        |
|--------|----------|---------|------|----------------|------------------|
| S1     | Grade 3  | A       | 4    | `S1-G3A-004`   | Tara Sethi       |
| S1     | Grade 5  | A       | 5    | `S1-G5A-005`   | Veer Malhotra    |
| S1     | Grade 8  | A       | 5    | `S1-G8A-005`   | Anya Chopra      |
| S2     | Grade 4  | A       | 4    | `S2-G4A-004`   | Kabir Sinha      |
| S2     | Grade 6  | A       | 4    | `S2-G6A-004`   | Myra Pillai      |
| S2     | Grade 9  | A       | 8    | `S2-G9A-008`   | Rehan Qureshi    |

### New parents (6 — OTP-only login, no password)

| School | Parent name      | Phone           | Linked child (Grade-Section, roll) |
|--------|------------------|-----------------|------------------------------------|
| S1     | Sneha Sethi      | `+919811100006` | Tara Sethi (Grade 3-A, roll 4)     |
| S1     | Gaurav Malhotra  | `+919811100007` | Veer Malhotra (Grade 5-A, roll 5)  |
| S1     | Ritu Chopra      | `+919811100008` | Anya Chopra (Grade 8-A, roll 5)    |
| S2     | Alok Sinha       | `+919822200007` | Kabir Sinha (Grade 4-A, roll 4)    |
| S2     | Lakshmi Pillai   | `+919822200008` | Myra Pillai (Grade 6-A, roll 4)    |
| S2     | Imran Qureshi    | `+919822200009` | Rehan Qureshi (Grade 9-A, roll 8)  |

> **Parent OTP login (same as originals):**
> `POST /api/v1/auth/send-otp { "identifier": "+9198…", "purpose": "login" }`
> → read `dev_code` from the response (dev mode) **or** the Render/server logs
> → `POST /api/v1/auth/verify-otp { "identifier": "+9198…", "code": "<code>", "purpose": "login" }`.
> See *§ Retrieving the parent OTP while testing* below for the full how-to.

---

## NEW School 3 — Holy Trinity Convent (full mock school)

A complete third school, inserted by the same `seed-expansion-2026-06-09.sql`.
Everything below is created in one shot: school row + philosophy + storage +
**6 classes / 9 sections**, subjects per class, **10 teachers**, **~36 students**
and **12 parents**.

- **Slug:** `holy-trinity-convent`
- **Board / Medium:** CBSE / English · **City:** Prayagraj, Uttar Pradesh
- **Brand:** `#7c3aed` · **School UUID:** `SELECT id FROM schools WHERE slug='holy-trinity-convent'`.

### Admin (1 — login with `email` + `password`)

| Full name      | Email                          | Phone           | Password        |
|----------------|--------------------------------|-----------------|-----------------|
| Maria Joseph   | `admin@holytrinity.edu.in`     | `+919876500210` | `Trinity@2026`  |

### Classes & sections

| Class    | Sections | Subjects                                                    |
|----------|----------|-------------------------------------------------------------|
| Grade 1  | A, B     | English, Mathematics, EVS                                   |
| Grade 2  | A        | English, Mathematics, EVS                                   |
| Grade 6  | A, B     | English, Mathematics, Science, Social Studies, Hindi        |
| Grade 7  | A        | English, Mathematics, Science, Social Studies, Hindi        |
| Grade 10 | A, B     | English, Mathematics, Science, Social Studies, Computer Sci |
| Grade 12 | A        | English, Physics, Chemistry, Mathematics, Computer Science  |

### Teachers (10 — login with `email` + `password` = `Teacher@2026`)

| Full name          | Email                                  | Phone           | Main assignments                       |
|--------------------|----------------------------------------|-----------------|----------------------------------------|
| Anita D'Souza      | `anita.dsouza@holytrinity.edu.in`      | `+919876500220` | Grade 1-A/B English                    |
| Brian Pereira      | `brian.pereira@holytrinity.edu.in`     | `+919876500221` | Grade 1-A Mathematics                  |
| Clara Fernandes    | `clara.fernandes@holytrinity.edu.in`   | `+919876500222` | Grade 2-A English, EVS                 |
| David Thomas       | `david.thomas@holytrinity.edu.in`      | `+919876500223` | Grade 6-A/B Mathematics                |
| Esha Varghese      | `esha.varghese@holytrinity.edu.in`     | `+919876500224` | Grade 6-A Science                      |
| Fiona Mathew       | `fiona.mathew@holytrinity.edu.in`      | `+919876500225` | Grade 7-A English, Social Studies      |
| George Kurian      | `george.kurian@holytrinity.edu.in`     | `+919876500226` | Grade 10-A/B Mathematics               |
| Hema Nair          | `hema.nair@holytrinity.edu.in`         | `+919876500227` | Grade 10-A Science                     |
| Irfan Ali          | `irfan.ali@holytrinity.edu.in`         | `+919876500228` | Grade 12-A Physics, Mathematics        |
| Jaya Menon         | `jaya.menon@holytrinity.edu.in`        | `+919876500229` | Grade 12-A Computer Science            |

### Parents (12 — OTP-only login, no password)

| Parent name        | Phone           | Linked child(ren)                                          |
|--------------------|-----------------|------------------------------------------------------------|
| Rosario Joseph     | `+919833300001` | Aanya Joseph (Grade 1-A, roll 1)                           |
| Linette Mathew     | `+919833300002` | Ben Mathew (Grade 1-A, roll 2)                             |
| Wilson Vaz         | `+919833300003` | Ethan Vaz (Grade 1-B, roll 1); Olivia Gomes (Grade 6-A, roll 4) |
| Agnes Crasto       | `+919833300004` | Hannah Crasto (Grade 2-A, roll 1)                          |
| Joseph Saldanha    | `+919833300005` | Liam Saldanha (Grade 6-A, roll 1)                          |
| Cynthia Noronha    | `+919833300006` | Peter Noronha (Grade 6-B, roll 1)                          |
| Ralph Menezes      | `+919833300007` | Sara Menezes (Grade 7-A, roll 1); Tobias Rodrigues (Grade 7-A, roll 2) |
| Glenda Carvalho    | `+919833300008` | Wendy Carvalho (Grade 10-A, roll 1)                        |
| Melwyn Pereira     | `+919833300009` | Aaron Pereira (Grade 10-B, roll 1)                         |
| Savio Mendonca     | `+919833300010` | Elias Mendonca (Grade 12-A, roll 1)                        |
| Naseem Shaikh      | `+919833300011` | Farida Shaikh (Grade 12-A, roll 2)                         |
| Royston Barreto    | `+919833300012` | Ishaan Barreto (Grade 12-A, roll 5); Joanna Vaz (Grade 12-A, roll 6) |

> Student codes follow the same convention: `S3-G{grade}{section}-{roll:03d}`,
> e.g. `S3-G1A-001`, `S3-G12A-005`.

### Totals after running the expansion on a clean original seed

| Entity   | Original | s1/s2 expansion | School 3 | **Total** |
|----------|----------|-----------------|----------|-----------|
| Schools  | 2        | 0               | 1        | **3**     |
| Admins   | 2        | 0               | 1        | **3**     |
| Teachers | 7        | 4               | 10       | **21**    |
| Parents  | 11       | 6               | 12       | **29**    |
| Students | 26       | 6               | 36       | **68**    |
| Children | 15       | 6               | 14       | **35**    |

> Run the **Verification** block at the bottom of `seed-expansion-2026-06-09.sql`
> after `COMMIT` to confirm these counts in Supabase.

---

## Retrieving the parent OTP while testing (Android Studio build)

Parents have **no password** — they log in via phone OTP. Here is exactly how
to get the code while testing the Android build against the live (Render +
Supabase) backend.

**How the OTP is produced:** when the app calls `POST /api/v1/auth/send-otp`,
`OtpService` generates a 6-digit code, **UPSERTs only a *hashed* row** into
`auth_otps` (columns `code_hash` + `code_salt` — the **plaintext is never
stored**), and hands the plain code to the configured delivery provider. In dev
the provider is `ConsoleProvider`, which **prints the code to stdout** (your
Render logs). Optionally the `/send-otp` response also echoes the code in a
`dev_code` field — but **only** when `OTP_DEV_RETURN_CODE=true` *and* the server
is **not** in production (the flag is hard-gated off in prod, exactly like
`JwtConfig.isProduction` — see `OtpService.devReturnCode`).

> ⚠️ **You cannot recover an OTP from Supabase.** `auth_otps` holds only
> `code_hash`/`code_salt`, so reading the table will NOT show you the digits.
> Use the Render log (method 1) or the `dev_code` response (method 2) — those
> are the only ways to see the actual code.

You have **two** working ways to read it (and one diagnostic):

1. **Render logs (what you're doing now — most reliable).**
   Render dashboard → your service → **Logs** → trigger *Send OTP* from the app
   → look for the `ConsoleProvider` line printing the code for that phone. Tip:
   paste the phone number (e.g. `+919811100001`) into the Render log search box
   to jump straight to it. The code is valid for the TTL (≈5 min) and a limited
   number of verify attempts (`max_attempts`, default 5).

2. **`dev_code` in the API response (no Render needed — if enabled).**
   Set `OTP_DEV_RETURN_CODE=true` on the backend env (**Render → Environment**),
   and make sure the deploy is **not** flagged as production (otherwise the gate
   forces it off). Then `/send-otp` returns:
   ```json
   { "status": "sent", "dev_code": "482913", "expires_in": 300 }
   ```
   In Android Studio watch **Logcat** (filter your app's HTTP/network tag, or
   use your OkHttp logging interceptor) and read `dev_code` off the response —
   no need to open Render at all. Point the build at the same backend base URL
   that has the flag on.

3. **Diagnostic only — confirm the OTP row exists in Supabase.**
   You can verify a send happened (you just can't read the code):
   ```sql
   -- did a fresh OTP get created for this parent? (code is HASHED — not readable)
   SELECT identifier, purpose, attempt_count, resend_count,
          is_verified, is_locked, sent_at, expires_at
   FROM public.auth_otps
   WHERE identifier = '+919811100001'
   ORDER BY sent_at DESC
   LIMIT 1;
   ```
   Useful when debugging "I never got an OTP" — if no row appears, the send
   failed before persistence; if `is_locked=true`, too many attempts.

**Recommended dev setup for painless Android testing:** on the (non-production)
backend env set `OTP_DEV_RETURN_CODE=true`. Then your only step in the app is:
enter the seeded parent phone → *Send OTP* → copy `dev_code` from Logcat → enter
it. If you keep it off, just keep the Render Logs tab open and read the
`ConsoleProvider` line. **Never enable this flag on a production deploy** — and
note the server already refuses to honour it in prod (see § Hygiene + AUDIT
RA-17).

---

## Password hashing — how to verify these match the server

The hashes inserted into `app_users.password_hash` were produced offline by
the seed generator (`scripts/_gen_seed.py`) using the exact algorithm in
`server/.../feature/auth/PasswordHasher.kt`:

* algorithm:          **PBKDF2 with HMAC-SHA256**
* iterations:         **120 000**
* salt size:          **16 bytes** (fixed deterministic ASCII per account so
                       the SQL is reproducible — still unique per user)
* derived key size:   **256 bits**
* encoded format:     `pbkdf2$120000$<base64-salt>$<base64-derived-key>`

Reproduce a single hash in Python to sanity-check the seed against the
server (no JVM required):

```python
import base64, hashlib
def h(pw, salt_ascii):
    salt = salt_ascii.encode("ascii")
    dk = hashlib.pbkdf2_hmac("sha256", pw.encode("utf-8"), salt, 120_000, dklen=32)
    return f"pbkdf2$120000${base64.b64encode(salt).decode()}${base64.b64encode(dk).decode()}"
print(h("Sunrise@2026",   "VP_SEED_S1_ADMIN"))   # School 1 admin
print(h("Greenfield@2026","VP_SEED_S2_ADMIN"))   # School 2 admin
print(h("Teacher@2026",   "VP_SEED_S1_TEA01"))   # Meena Sharma — School 1
```

The strings printed by the Python snippet match — character-for-character —
the `password_hash` columns produced by `PasswordHasher.hash(...)` on the
same plaintext + salt. The server's `PasswordHasher.verify(...)` will
therefore accept the seeded credentials on first login.

---

## Hygiene reminders

* **Treat this file as a secret outside dev/CI.** Stripping it before any
  internet-reachable deploy keeps the test password set off the public
  internet (the seed itself still works — only the plaintext mapping is
  in this file).
* **Rotate before production.** These passwords are deliberately memorable
  for local QA. None of them is suitable for any real account.
* **Parent OTP behaviour is dev-friendly by default** — see
  `AUDIT_2026-06-07.md` § RA-17. Production deploys MUST set
  `OTP_DEV_RETURN_CODE=false` so `/send-otp` does not echo the code.

*End of seed-credentials-2026-06-07.md.*
