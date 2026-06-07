# Seed Credentials — 2026-06-07

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
