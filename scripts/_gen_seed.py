#!/usr/bin/env python3
"""
Generator for scripts/seed-2026-06-07.sql.

Why a generator (not hand-typed SQL):
  * The seed has ~30 days of attendance per student, ~5 marks rows per student,
    7 teachers, 11 parents, 26 students, 15 children, plus assignments,
    homework, fees, calendar, syllabus, notifications, scholarships. Hand-typed
    that is ~2000+ INSERTs — impossible to keep consistent.
  * UUIDs must be deterministic per "logical entity" so the script is idempotent
    on re-run. We derive them via UUIDv5 from a stable namespace + a label, so
    the same label always produces the same UUID.
  * Password hashes must match server PasswordHasher.kt exactly. We reproduce
    PBKDF2-HMAC-SHA256 / 120k iters / 16-byte salt / 256-bit key here and inline
    the resulting "pbkdf2$..." strings into the SQL.

Re-run with:  python3 scripts/_gen_seed.py
"""

from __future__ import annotations
import hashlib, base64, uuid, sys
from datetime import date, timedelta, datetime

# -----------------------------------------------------------------------------
# Constants — keep these in lockstep with server config
# -----------------------------------------------------------------------------
PBKDF2_ITER = 120_000           # PasswordHasher.kt: ITERATIONS
NS = uuid.UUID("ab8a1c2c-1c4b-4b8a-9c1a-2026060700be")  # stable seed namespace
ANCHOR = date(2026, 6, 7)       # "today" for the seed — same as audit date


def uid(label: str) -> str:
    """Deterministic UUIDv5 from a stable namespace + label."""
    return str(uuid.uuid5(NS, label))


def pwhash(pw: str, salt_ascii: str) -> str:
    """
    Reproduce server/.../feature/auth/PasswordHasher.kt exactly:
      ALGO=PBKDF2WithHmacSHA256, ITERATIONS=120000, SALT_BYTES=16, KEY_BITS=256
      format: pbkdf2$<iter>$<b64 salt>$<b64 key>
    Salt is a fixed ASCII string (16 bytes, deterministic per account) so the
    SQL is reproducible and idempotent. Salt still differs per user, which is
    what PBKDF2 actually requires for security.
    """
    salt = salt_ascii.encode("ascii")
    assert len(salt) == 16, f"salt must be 16 bytes, got {len(salt)} for {salt_ascii!r}"
    dk = hashlib.pbkdf2_hmac("sha256", pw.encode("utf-8"), salt, PBKDF2_ITER, dklen=32)
    return f"pbkdf2${PBKDF2_ITER}${base64.b64encode(salt).decode()}${base64.b64encode(dk).decode()}"


def sql_str(s):
    """SQL string literal with single-quote escaping. None -> NULL."""
    if s is None:
        return "NULL"
    if isinstance(s, bool):
        return "TRUE" if s else "FALSE"
    if isinstance(s, (int, float)):
        return str(s)
    return "'" + str(s).replace("'", "''") + "'"


# -----------------------------------------------------------------------------
# 2 SCHOOLS + their staff & rosters
# -----------------------------------------------------------------------------
SCHOOLS = [
    {
        "key": "s1", "slug": "sunrise-public-school", "name": "Sunrise Public School",
        "board": "CBSE",  "medium": "English", "gender": "co_ed",
        "phone": "+919876500001", "email": "office@sunrise.edu.in",
        "principal": "Dr. Anjali Verma", "principal_phone": "+919876500002",
        "principal_email": "principal@sunrise.edu.in",
        "city": "Lucknow", "district": "Lucknow", "state": "Uttar Pradesh",
        "pincode": "226010", "address": "12 Vidya Path, Gomti Nagar, Lucknow",
        "logo": "https://cdn.vidyaprayag.local/seed/sunrise-logo.png",
        "brand": "#1d4ed8", "lat": 26.8467, "lng": 80.9462,
        "core_mission": "Joyful, rigorous learning rooted in Indian values.",
        "learning_model": "NEP-2020 aligned competency-based learning.",
        "primary_language": "English / Hindi",
    },
    {
        "key": "s2", "slug": "greenfield-academy", "name": "Greenfield Academy",
        "board": "ICSE", "medium": "English", "gender": "co_ed",
        "phone": "+919876500101", "email": "office@greenfield.edu.in",
        "principal": "Mr. Rakesh Iyer", "principal_phone": "+919876500102",
        "principal_email": "principal@greenfield.edu.in",
        "city": "Kanpur", "district": "Kanpur Nagar", "state": "Uttar Pradesh",
        "pincode": "208001", "address": "55 Civil Lines, Kanpur",
        "logo": "https://cdn.vidyaprayag.local/seed/greenfield-logo.png",
        "brand": "#047857", "lat": 26.4499, "lng": 80.3319,
        "core_mission": "Curious minds, kind hearts, global outlook.",
        "learning_model": "Inquiry-led pedagogy with arts integration.",
        "primary_language": "English",
    },
]
SK = {s["key"]: s for s in SCHOOLS}

# Admins (one per school)
ADMINS = [
    {"school": "s1", "key": "s1_admin", "full_name": "Anjali Verma",
     "email": "admin@sunrise.edu.in",  "phone": "+919876500010",
     "pw": "Sunrise@2026",   "salt": "VP_SEED_S1_ADMIN"},
    {"school": "s2", "key": "s2_admin", "full_name": "Rakesh Iyer",
     "email": "admin@greenfield.edu.in", "phone": "+919876500110",
     "pw": "Greenfield@2026", "salt": "VP_SEED_S2_ADMIN"},
]

# Teachers (4 + 3 = 7)
TEACHERS = [
    {"school": "s1", "key": "s1_t_meena", "full_name": "Meena Sharma",
     "email": "meena.sharma@sunrise.edu.in",  "phone": "+919876500020",
     "pw": "Teacher@2026", "salt": "VP_SEED_S1_TEA01"},
    {"school": "s1", "key": "s1_t_rahul", "full_name": "Rahul Mehra",
     "email": "rahul.mehra@sunrise.edu.in",   "phone": "+919876500021",
     "pw": "Teacher@2026", "salt": "VP_SEED_S1_TEA02"},
    {"school": "s1", "key": "s1_t_priya", "full_name": "Priya Nair",
     "email": "priya.nair@sunrise.edu.in",    "phone": "+919876500022",
     "pw": "Teacher@2026", "salt": "VP_SEED_S1_TEA03"},
    {"school": "s1", "key": "s1_t_arjun", "full_name": "Arjun Khanna",
     "email": "arjun.khanna@sunrise.edu.in",  "phone": "+919876500023",
     "pw": "Teacher@2026", "salt": "VP_SEED_S1_TEA04"},
    {"school": "s2", "key": "s2_t_kavita", "full_name": "Kavita Iyengar",
     "email": "kavita.iyengar@greenfield.edu.in", "phone": "+919876500120",
     "pw": "Teacher@2026", "salt": "VP_SEED_S2_TEA01"},
    {"school": "s2", "key": "s2_t_vivek", "full_name": "Vivek Banerjee",
     "email": "vivek.banerjee@greenfield.edu.in", "phone": "+919876500121",
     "pw": "Teacher@2026", "salt": "VP_SEED_S2_TEA02"},
    {"school": "s2", "key": "s2_t_neha", "full_name": "Neha Goswami",
     "email": "neha.goswami@greenfield.edu.in",   "phone": "+919876500122",
     "pw": "Teacher@2026", "salt": "VP_SEED_S2_TEA03"},
]
TKEY = {t["key"]: t for t in TEACHERS}

# Classes per school (with sections)
CLASSES = {
    "s1": [
        ("CLS-3",  "Grade 3", ["A", "B"]),
        ("CLS-5",  "Grade 5", ["A"]),
        ("CLS-8",  "Grade 8", ["A"]),
    ],
    "s2": [
        ("CLS-4",  "Grade 4", ["A"]),
        ("CLS-6",  "Grade 6", ["A", "B"]),
        ("CLS-9",  "Grade 9", ["A"]),
    ],
}

# Subjects per class (kept small but realistic)
SUBJECTS_BY_CLASS = {
    "Grade 3": ["English", "Mathematics", "EVS"],
    "Grade 4": ["English", "Mathematics", "EVS"],
    "Grade 5": ["English", "Mathematics", "Science", "Social Studies"],
    "Grade 6": ["English", "Mathematics", "Science", "Social Studies"],
    "Grade 8": ["English", "Mathematics", "Science", "Social Studies", "Hindi"],
    "Grade 9": ["English", "Mathematics", "Science", "Social Studies", "Hindi"],
}

# Teacher → assignments (who teaches what to whom). Each tuple is
# (teacher_key, class_name, section, subject). Every teacher gets ≥1.
ASSIGNMENTS = [
    # School 1
    ("s1_t_meena", "Grade 3", "A", "English"),
    ("s1_t_meena", "Grade 3", "A", "EVS"),
    ("s1_t_rahul", "Grade 5", "A", "Mathematics"),
    ("s1_t_rahul", "Grade 5", "A", "Science"),
    ("s1_t_priya", "Grade 8", "A", "English"),
    ("s1_t_priya", "Grade 8", "A", "Social Studies"),
    ("s1_t_arjun", "Grade 8", "A", "Mathematics"),
    # School 2
    ("s2_t_kavita", "Grade 4", "A", "English"),
    ("s2_t_kavita", "Grade 4", "A", "Mathematics"),
    ("s2_t_vivek",  "Grade 6", "A", "Science"),
    ("s2_t_neha",   "Grade 9", "A", "English"),
]

# Students per school per class+section. roll_numbers unique within (school, class, section).
# student_codes are globally unique (StudentsTable.studentCode is uniqueIndex).
STUDENTS = {
    # school → list of (class_name, section, roll, full_name)
    "s1": [
        ("Grade 3", "A", "1", "Aarav Singh"),
        ("Grade 3", "A", "2", "Bhavya Tiwari"),
        ("Grade 3", "A", "3", "Chirag Yadav"),
        ("Grade 3", "B", "1", "Diya Mishra"),
        ("Grade 5", "A", "1", "Ishan Kapoor"),
        ("Grade 5", "A", "2", "Jiya Saxena"),
        ("Grade 5", "A", "3", "Karan Trivedi"),
        ("Grade 5", "A", "4", "Lavanya Pandey"),
        ("Grade 8", "A", "1", "Manav Joshi"),
        ("Grade 8", "A", "2", "Nisha Bansal"),
        ("Grade 8", "A", "3", "Om Prakash"),
        ("Grade 8", "A", "4", "Pari Agarwal"),
    ],
    "s2": [
        ("Grade 4", "A", "1", "Reyansh Kumar"),
        ("Grade 4", "A", "2", "Saanvi Bose"),
        ("Grade 4", "A", "3", "Tanvi Roy"),
        ("Grade 6", "A", "1", "Udayan Das"),
        ("Grade 6", "A", "2", "Vihaan Sen"),
        ("Grade 6", "A", "3", "Wamika Dutta"),
        ("Grade 6", "B", "1", "Xen Chowdhury"),
        ("Grade 9", "A", "1", "Yash Banerjee"),
        ("Grade 9", "A", "2", "Zara Mukherjee"),
        ("Grade 9", "A", "3", "Aadya Ghosh"),
        ("Grade 9", "A", "4", "Bodhi Chatterjee"),
        ("Grade 9", "A", "5", "Charvi Lahiri"),
        ("Grade 9", "A", "6", "Divit Pal"),
        ("Grade 9", "A", "7", "Ela Bhattacharya"),
    ],
}


def student_code(sk: str, klass: str, section: str, roll: str) -> str:
    """Stable globally-unique student_code, e.g. S1-G3A-001."""
    grade_num = "".join(ch for ch in klass if ch.isdigit())
    return f"{sk.upper()}-G{grade_num}{section}-{int(roll):03d}"


# Parents (5 for school 1, 6 for school 2 — total 11; the brief said 5-7 per school)
# Each parent links to 1–2 children. Children are drawn from STUDENTS so they
# satisfy the (school_id, roll_number) lookup in ParentLinkRouting.
PARENTS = [
    # ---- School 1 ----
    {"key": "s1_p_arav",  "school": "s1", "phone": "+919811100001", "full_name": "Rohit Singh",
     "children": [("Grade 3", "A", "1", "Aarav Singh")]},
    {"key": "s1_p_diya",  "school": "s1", "phone": "+919811100002", "full_name": "Sunita Mishra",
     "children": [("Grade 3", "B", "1", "Diya Mishra")]},
    {"key": "s1_p_ishan", "school": "s1", "phone": "+919811100003", "full_name": "Vikram Kapoor",
     "children": [("Grade 5", "A", "1", "Ishan Kapoor"),
                  ("Grade 8", "A", "4", "Pari Agarwal")]},   # 2nd child
    {"key": "s1_p_jiya",  "school": "s1", "phone": "+919811100004", "full_name": "Reena Saxena",
     "children": [("Grade 5", "A", "2", "Jiya Saxena")]},
    {"key": "s1_p_manav", "school": "s1", "phone": "+919811100005", "full_name": "Pankaj Joshi",
     "children": [("Grade 8", "A", "1", "Manav Joshi"),
                  ("Grade 8", "A", "2", "Nisha Bansal")]},   # 2 unrelated kids same parent
    # ---- School 2 ----
    {"key": "s2_p_rey",  "school": "s2", "phone": "+919822200001", "full_name": "Anand Kumar",
     "children": [("Grade 4", "A", "1", "Reyansh Kumar")]},
    {"key": "s2_p_saa",  "school": "s2", "phone": "+919822200002", "full_name": "Madhuri Bose",
     "children": [("Grade 4", "A", "2", "Saanvi Bose")]},
    {"key": "s2_p_uday", "school": "s2", "phone": "+919822200003", "full_name": "Subhash Das",
     "children": [("Grade 6", "A", "1", "Udayan Das"),
                  ("Grade 9", "A", "1", "Yash Banerjee")]},  # 2nd child (diff class)
    {"key": "s2_p_vih",  "school": "s2", "phone": "+919822200004", "full_name": "Priyanka Sen",
     "children": [("Grade 6", "A", "2", "Vihaan Sen")]},
    {"key": "s2_p_xen",  "school": "s2", "phone": "+919822200005", "full_name": "Soumitra Chowdhury",
     "children": [("Grade 6", "B", "1", "Xen Chowdhury")]},
    {"key": "s2_p_zar",  "school": "s2", "phone": "+919822200006", "full_name": "Tanvi Mukherjee",
     "children": [("Grade 9", "A", "2", "Zara Mukherjee"),
                  ("Grade 9", "A", "3", "Aadya Ghosh")]},
]


# =============================================================================
# Build the SQL
# =============================================================================
out: list[str] = []


def w(line: str = ""):
    out.append(line)


HEADER = f"""\
-- =============================================================================
-- seed-2026-06-07.sql
-- VidyaPrayag — Integration Seed (PostgreSQL / Supabase)
-- -----------------------------------------------------------------------------
-- Generated by scripts/_gen_seed.py — do not hand-edit. Re-run the generator if
-- you need to change the dataset. Anchor date: {ANCHOR.isoformat()}.
--
-- ORDER OF EXECUTION (run BEFORE this file):
--   1. PROVISION.sql (and its 4 referenced .sql files) — base 36 tables.
--   2. schema-patch-2026-06-07.sql — adds scholarships + scholarship_applications
--      (RE-AUDIT RA-05). Without this patch the scholarship INSERTs below fail.
--
-- DESIGN PROPERTIES:
--   * IDEMPOTENT       — every INSERT uses ON CONFLICT DO NOTHING (or DO UPDATE
--                        where the target row is the one we always want to win).
--                        Running this file twice is a no-op.
--   * ATOMIC           — wrapped in BEGIN; ... COMMIT;. A failure anywhere
--                        rolls back the entire seed.
--   * POSTGRES-CLEAN   — zero SQLite-isms. timestamp without time zone,
--                        gen_random_uuid() unused (we supply deterministic
--                        UUIDv5s up-front).
--   * MULTI-TENANT     — every row carries the correct school_id (or, for
--                        parents, school_id=NULL — matching the OTP-signup path).
--   * NO PLAINTEXT     — admin/teacher passwords stored only as PBKDF2 hashes
--                        produced by the same algorithm as server PasswordHasher.kt.
--   * VERIFIABLE       — final SELECT block reports row counts and tenancy
--                        invariants. Any 'bad' count > 0 means the seed failed.
--
-- CREDENTIALS for testing live in seed-credentials-2026-06-07.md (repo root).
--
-- Section index:
--   1.  UUID + value constants (informational header only)
--   2.  Schools                                  (table: schools)
--   3.  School admin users + auth records        (table: app_users)
--   4.  School onboarding state                  (school_philosophy, storage_metrics, schools.onboarded_at, classes)
--   5.  Classes + sections per school            (table: school_classes)
--   6.  Subjects per class                       (table: school_subjects)
--   7.  Students (canonical roster)              (table: students)
--   8.  Teacher users + auth records             (table: app_users + faculty)
--   9.  Teacher -> class/subject assignments     (table: teacher_subject_assignments)
--   10. Parent users (OTP-verified state)        (table: app_users)
--   11. Parent -> child links                    (table: children)
--   12. Attendance records (last 30 days)        (table: attendance_records)
--   13. Assessments + per-student marks          (tables: assessments, assessment_marks)
--   14. Exam results (school-admin view)         (table: exam_results)
--   15. Homework + a few submissions             (tables: homework, homework_submissions)
--   16. Syllabus coverage progress               (table: syllabus_units)
--   17. Announcements (school & class scoped)    (table: announcements)
--   18. Academic calendar + holidays             (tables: academic_calendar, holiday_list)
--   19. Fee records (PAID/DUE/OVERDUE mix)       (table: fee_records)
--   20. Scholarships catalogue + applications    (tables: scholarships, scholarship_applications)
--   21. Verification SELECTs (run after COMMIT)
-- =============================================================================
"""
w(HEADER)

# -----------------------------------------------------------------------------
# 1. UUID + value constants (informational header)
# -----------------------------------------------------------------------------
w("-- -----------------------------------------------------------------------------")
w("-- 1. UUID + value constants (informational — actual UUIDs are inlined below)")
w("-- -----------------------------------------------------------------------------")
w(f"-- ANCHOR DATE                : {ANCHOR.isoformat()}")
w(f"-- PBKDF2 ITERATIONS          : {PBKDF2_ITER}")
w(f"-- UUIDv5 NAMESPACE           : {NS}")
w(f"-- SCHOOLS                    : {SK['s1']['name']} ({uid('school:s1')})")
w(f"--                              {SK['s2']['name']} ({uid('school:s2')})")
w("")

w("BEGIN;")
w("")

# -----------------------------------------------------------------------------
# 2. Schools
# -----------------------------------------------------------------------------
w("-- -----------------------------------------------------------------------------")
w("-- 2. Schools")
w("-- -----------------------------------------------------------------------------")
for s in SCHOOLS:
    sid = uid(f"school:{s['key']}")
    w(f"INSERT INTO schools (id, name, slug, board, medium, school_gender, contact_phone, contact_email, "
      f"principal_name, principal_phone, principal_email, full_address, city, district, state, pincode, "
      f"logo_url, brand_color, latitude, longitude, is_active, onboarded_at, created_at, updated_at)")
    w(f"VALUES ({sql_str(sid)}, {sql_str(s['name'])}, {sql_str(s['slug'])}, {sql_str(s['board'])}, "
      f"{sql_str(s['medium'])}, {sql_str(s['gender'])}, {sql_str(s['phone'])}, {sql_str(s['email'])}, "
      f"{sql_str(s['principal'])}, {sql_str(s['principal_phone'])}, {sql_str(s['principal_email'])}, "
      f"{sql_str(s['address'])}, {sql_str(s['city'])}, {sql_str(s['district'])}, {sql_str(s['state'])}, "
      f"{sql_str(s['pincode'])}, {sql_str(s['logo'])}, {sql_str(s['brand'])}, {s['lat']}, {s['lng']}, "
      f"TRUE, '{ANCHOR.isoformat()} 09:00:00', '{ANCHOR.isoformat()} 09:00:00', '{ANCHOR.isoformat()} 09:00:00')")
    w("ON CONFLICT (id) DO NOTHING;")
    w("")

# -----------------------------------------------------------------------------
# 3. School admin users + auth records
# -----------------------------------------------------------------------------
w("-- -----------------------------------------------------------------------------")
w("-- 3. School admin users (role=school_admin, email-verified, profile_completed)")
w("-- -----------------------------------------------------------------------------")
for a in ADMINS:
    aid = uid(f"user:{a['key']}")
    sid = uid(f"school:{a['school']}")
    h = pwhash(a["pw"], a["salt"])
    w(f"INSERT INTO app_users (id, school_id, role, full_name, phone, email, password_hash, "
      f"language_pref, is_phone_verified, is_email_verified, profile_completed, is_active, "
      f"created_at, updated_at)")
    w(f"VALUES ({sql_str(aid)}, {sql_str(sid)}, 'school_admin', {sql_str(a['full_name'])}, "
      f"{sql_str(a['phone'])}, {sql_str(a['email'])}, {sql_str(h)}, 'en', "
      f"TRUE, TRUE, TRUE, TRUE, "
      f"'{ANCHOR.isoformat()} 09:00:00', '{ANCHOR.isoformat()} 09:00:00')")
    w("ON CONFLICT (id) DO NOTHING;")
    w("")

# -----------------------------------------------------------------------------
# 4. School onboarding state — landing-on-dashboard requires:
#    name + contact_email/phone + logo_url + >=1 school_classes + onboarded_at.
#    onboarded_at is already set above; logo + contact above; classes follow §5.
#    Also seed school_philosophy + storage_metrics rows so the school profile
#    endpoint has data to render.
# -----------------------------------------------------------------------------
w("-- -----------------------------------------------------------------------------")
w("-- 4. School onboarding state (philosophy, storage metrics)")
w("-- -----------------------------------------------------------------------------")
for s in SCHOOLS:
    sid = uid(f"school:{s['key']}")
    w(f"INSERT INTO school_philosophy (school_id, core_mission, learning_model, primary_language, "
      f"public_profile, updated_at)")
    w(f"VALUES ({sql_str(sid)}, {sql_str(s['core_mission'])}, {sql_str(s['learning_model'])}, "
      f"{sql_str(s['primary_language'])}, TRUE, '{ANCHOR.isoformat()} 09:00:00')")
    w("ON CONFLICT (school_id) DO NOTHING;")
    w("")
    w(f"INSERT INTO storage_metrics (school_id, total_storage, storage_used, bytes_used, updated_at)")
    w(f"VALUES ({sql_str(sid)}, '10 GB', '0 B', 0, '{ANCHOR.isoformat()} 09:00:00')")
    w("ON CONFLICT (school_id) DO NOTHING;")
    w("")

# -----------------------------------------------------------------------------
# 5. Classes + sections
# -----------------------------------------------------------------------------
w("-- -----------------------------------------------------------------------------")
w("-- 5. Classes per school (with sections as JSON-array text)")
w("-- -----------------------------------------------------------------------------")
for sk, lst in CLASSES.items():
    sid = uid(f"school:{sk}")
    for code, name, sections in lst:
        cid = uid(f"class:{sk}:{code}")
        sections_json = "[" + ",".join(f'"{s}"' for s in sections) + "]"
        w(f"INSERT INTO school_classes (id, school_id, code, name, sections, created_at)")
        w(f"VALUES ({sql_str(cid)}, {sql_str(sid)}, {sql_str(code)}, {sql_str(name)}, "
          f"{sql_str(sections_json)}, '{ANCHOR.isoformat()} 09:00:00')")
        w("ON CONFLICT (id) DO NOTHING;")
        w("")

# -----------------------------------------------------------------------------
# 6. Subjects per class
# -----------------------------------------------------------------------------
w("-- -----------------------------------------------------------------------------")
w("-- 6. Subjects per class")
w("-- -----------------------------------------------------------------------------")
for sk, lst in CLASSES.items():
    for code, name, _sections in lst:
        cid = uid(f"class:{sk}:{code}")
        for sub in SUBJECTS_BY_CLASS[name]:
            sid_sub = uid(f"subject:{sk}:{code}:{sub}")
            sub_code = sub[:3].upper()
            w(f"INSERT INTO school_subjects (id, class_id, sub_name, sub_code, teacher_assigned, created_at)")
            w(f"VALUES ({sql_str(sid_sub)}, {sql_str(cid)}, {sql_str(sub)}, {sql_str(sub_code)}, "
              f"NULL, '{ANCHOR.isoformat()} 09:00:00')")
            w("ON CONFLICT (id) DO NOTHING;")
            w("")

# -----------------------------------------------------------------------------
# 7. Students (canonical roster)
#
# RE-AUDIT RA-01: there is no API write path for `students`. The seed is the
# only producer; teacher dashboards + parent-link both depend on these rows.
# -----------------------------------------------------------------------------
w("-- -----------------------------------------------------------------------------")
w("-- 7. Students (canonical roster — closes RE-AUDIT RA-01 for seed scope)")
w("-- -----------------------------------------------------------------------------")
for sk, roster in STUDENTS.items():
    sid = uid(f"school:{sk}")
    for klass, section, roll, name in roster:
        code = student_code(sk, klass, section, roll)
        stuid = uid(f"student:{code}")
        w(f"INSERT INTO students (id, school_id, student_code, full_name, class_name, section, "
          f"roll_number, is_active, created_at)")
        w(f"VALUES ({sql_str(stuid)}, {sql_str(sid)}, {sql_str(code)}, {sql_str(name)}, "
          f"{sql_str(klass)}, {sql_str(section)}, {sql_str(roll)}, TRUE, "
          f"'{ANCHOR.isoformat()} 09:00:00')")
        w("ON CONFLICT (id) DO NOTHING;")
        w("")

# -----------------------------------------------------------------------------
# 8. Teacher users + faculty rows
#
# Each teacher gets an app_users row AND a faculty row (the school admin view
# reads faculty for the staff directory; teacher login + scoping reads app_users).
# -----------------------------------------------------------------------------
w("-- -----------------------------------------------------------------------------")
w("-- 8. Teacher users + faculty directory rows")
w("-- -----------------------------------------------------------------------------")
for t in TEACHERS:
    tid = uid(f"user:{t['key']}")
    sid = uid(f"school:{t['school']}")
    h = pwhash(t["pw"], t["salt"])
    w(f"INSERT INTO app_users (id, school_id, role, full_name, phone, email, password_hash, "
      f"language_pref, is_phone_verified, is_email_verified, profile_completed, is_active, "
      f"created_at, updated_at)")
    w(f"VALUES ({sql_str(tid)}, {sql_str(sid)}, 'teacher', {sql_str(t['full_name'])}, "
      f"{sql_str(t['phone'])}, {sql_str(t['email'])}, {sql_str(h)}, 'en', "
      f"TRUE, TRUE, TRUE, TRUE, "
      f"'{ANCHOR.isoformat()} 09:00:00', '{ANCHOR.isoformat()} 09:00:00')")
    w("ON CONFLICT (id) DO NOTHING;")
    w("")
    fid = uid(f"faculty:{t['key']}")
    ext = t["key"].upper()
    w(f"INSERT INTO faculty (id, school_id, external_id, user_id, name, department, is_active, created_at)")
    w(f"VALUES ({sql_str(fid)}, {sql_str(sid)}, {sql_str(ext)}, {sql_str(tid)}, "
      f"{sql_str(t['full_name'])}, 'Academics', TRUE, '{ANCHOR.isoformat()} 09:00:00')")
    w("ON CONFLICT (id) DO NOTHING;")
    w("")

# -----------------------------------------------------------------------------
# 9. Teacher → class/subject assignments
#
# Critical: teacher_id MUST be set or the teacher's dashboard is empty
# (RE-AUDIT RA-08). class_id + subject_id are best-effort joined.
# -----------------------------------------------------------------------------
w("-- -----------------------------------------------------------------------------")
w("-- 9. Teacher -> class/subject assignments  (teacher_id set; closes RA-08 for seed)")
w("-- -----------------------------------------------------------------------------")
# Build a quick lookup of (school, class_name) -> class_id and (school, class_name, subject) -> subject_id
CLASS_ID = {}
SUBJECT_ID = {}
for sk, lst in CLASSES.items():
    for code, name, _ in lst:
        CLASS_ID[(sk, name)] = (uid(f"class:{sk}:{code}"), code)
        for sub in SUBJECTS_BY_CLASS[name]:
            SUBJECT_ID[(sk, name, sub)] = uid(f"subject:{sk}:{code}:{sub}")

for tk, cname, sec, sub in ASSIGNMENTS:
    t = TKEY[tk]
    sk = t["school"]
    sid = uid(f"school:{sk}")
    tid = uid(f"user:{tk}")
    aid = uid(f"tsa:{tk}:{cname}:{sec}:{sub}")
    cid, _code = CLASS_ID[(sk, cname)]
    subid = SUBJECT_ID[(sk, cname, sub)]
    w(f"INSERT INTO teacher_subject_assignments (id, school_id, class_id, class_name, section, "
      f"subject_id, subject, teacher_id, teacher_name, is_active, created_at, updated_at)")
    w(f"VALUES ({sql_str(aid)}, {sql_str(sid)}, {sql_str(cid)}, {sql_str(cname)}, {sql_str(sec)}, "
      f"{sql_str(subid)}, {sql_str(sub)}, {sql_str(tid)}, {sql_str(t['full_name'])}, TRUE, "
      f"'{ANCHOR.isoformat()} 09:00:00', '{ANCHOR.isoformat()} 09:00:00')")
    w("ON CONFLICT (id) DO NOTHING;")
    w("")

# -----------------------------------------------------------------------------
# 10. Parent users
#
# Parents authenticate via phone + OTP. school_id is intentionally NULL — that
# matches the OTP-signup AuthRouting.kt path. Their tenancy comes from
# children.school_id (§11).
# -----------------------------------------------------------------------------
w("-- -----------------------------------------------------------------------------")
w("-- 10. Parent users (role=parent, is_phone_verified=true, school_id=NULL)")
w("-- -----------------------------------------------------------------------------")
for p in PARENTS:
    pid = uid(f"user:{p['key']}")
    w(f"INSERT INTO app_users (id, school_id, role, full_name, phone, email, password_hash, "
      f"language_pref, is_phone_verified, is_email_verified, profile_completed, is_active, "
      f"created_at, updated_at)")
    w(f"VALUES ({sql_str(pid)}, NULL, 'parent', {sql_str(p['full_name'])}, "
      f"{sql_str(p['phone'])}, NULL, NULL, 'en', "
      f"TRUE, FALSE, TRUE, TRUE, "
      f"'{ANCHOR.isoformat()} 09:00:00', '{ANCHOR.isoformat()} 09:00:00')")
    w("ON CONFLICT (id) DO NOTHING;")
    w("")

# -----------------------------------------------------------------------------
# 11. Parent → child links
#
# children.school_id is the parent-side tenant boundary. studentCode is kept
# equal to students.student_code so RA-15 / RA-19 invariants hold.
# -----------------------------------------------------------------------------
w("-- -----------------------------------------------------------------------------")
w("-- 11. Parent -> child links (children.school_id is the parent-side tenancy key)")
w("-- -----------------------------------------------------------------------------")
N_CHILDREN = 0
for p in PARENTS:
    pid = uid(f"user:{p['key']}")
    sk = p["school"]
    sid = uid(f"school:{sk}")
    for klass, section, roll, name in p["children"]:
        code = student_code(sk, klass, section, roll)
        cid = uid(f"child:{p['key']}:{code}")
        N_CHILDREN += 1
        w(f"INSERT INTO children (id, parent_id, school_id, student_code, child_name, "
          f"date_of_birth, gender, current_grade, interests, overall_progress, current_level, "
          f"attendance_status, is_active, created_at, updated_at)")
        w(f"VALUES ({sql_str(cid)}, {sql_str(pid)}, {sql_str(sid)}, {sql_str(code)}, "
          f"{sql_str(name)}, '2015-04-12', 'OTHER', {sql_str(klass)}, '[\"reading\",\"sports\"]', "
          f"0.72, 4, 'PRESENT', TRUE, '{ANCHOR.isoformat()} 09:00:00', '{ANCHOR.isoformat()} 09:00:00')")
        w("ON CONFLICT (id) DO NOTHING;")
        w("")

# -----------------------------------------------------------------------------
# 12. Attendance records (last 30 days, student-type)
#
# Realistic mix: ~92% PRESENT, ~5% LATE, ~3% ABSENT. Deterministic per (code, day).
# Skip Sundays (weekday 6). marked_by is the assigned teacher of that class.
# -----------------------------------------------------------------------------
w("-- -----------------------------------------------------------------------------")
w("-- 12. Attendance records (last 30 days, student-type, ~92% PRESENT mix)")
w("-- -----------------------------------------------------------------------------")
# build map: (sk, class_name) -> teacher_id (first matching assignment)
CLASS_TEACHER = {}
for tk, cname, sec, sub in ASSIGNMENTS:
    key = (TKEY[tk]["school"], cname)
    CLASS_TEACHER.setdefault(key, uid(f"user:{tk}"))

ATT_ROWS = 0
for sk, roster in STUDENTS.items():
    sid = uid(f"school:{sk}")
    for klass, section, roll, name in roster:
        code = student_code(sk, klass, section, roll)
        marker = CLASS_TEACHER.get((sk, klass))
        for i in range(30):
            d = ANCHOR - timedelta(days=i)
            if d.weekday() == 6:           # Sunday
                continue
            # deterministic status from hash
            h = abs(hash((code, d.isoformat()))) % 100
            if h < 92:
                status = "PRESENT"
            elif h < 97:
                status = "LATE"
            else:
                status = "ABSENT"
            arid = uid(f"att:{code}:{d.isoformat()}")
            ATT_ROWS += 1
            w(f"INSERT INTO attendance_records (id, school_id, date, type, person_id, grade, "
              f"status, marked_by, created_at) VALUES "
              f"({sql_str(arid)}, {sql_str(sid)}, {sql_str(d.isoformat())}, 'student', "
              f"{sql_str(code)}, {sql_str(klass)}, {sql_str(status)}, {sql_str(marker)}, "
              f"'{d.isoformat()} 09:00:00') ON CONFLICT (id) DO NOTHING;")
w("")

# -----------------------------------------------------------------------------
# 13. Assessments + per-student marks
#
# For every assignment, create 1 assessment (Unit Test I) and marks for every
# student in (school, class, section). Marks ∈ [55, 95] deterministic per code.
# -----------------------------------------------------------------------------
w("-- -----------------------------------------------------------------------------")
w("-- 13. Assessments + per-student marks (one Unit Test I per assignment)")
w("-- -----------------------------------------------------------------------------")
MARK_ROWS = 0
for tk, cname, sec, sub in ASSIGNMENTS:
    t = TKEY[tk]
    sk = t["school"]
    sid = uid(f"school:{sk}")
    tid = uid(f"user:{tk}")
    aid = uid(f"asmt:{tk}:{cname}:{sec}:{sub}")
    exam_date = (ANCHOR - timedelta(days=10)).isoformat()
    w(f"INSERT INTO assessments (id, school_id, teacher_id, class_name, section, subject, "
      f"name, max_marks, exam_date, is_active, created_at, updated_at) VALUES "
      f"({sql_str(aid)}, {sql_str(sid)}, {sql_str(tid)}, {sql_str(cname)}, {sql_str(sec)}, "
      f"{sql_str(sub)}, 'Unit Test I', 100, {sql_str(exam_date)}, TRUE, "
      f"'{ANCHOR.isoformat()} 09:00:00', '{ANCHOR.isoformat()} 09:00:00') ON CONFLICT (id) DO NOTHING;")
    # marks for every matching student
    for klass, section, roll, name in STUDENTS[sk]:
        if klass != cname or section != sec:
            continue
        code = student_code(sk, klass, section, roll)
        mid = uid(f"mark:{aid}:{code}")
        marks = 55 + (abs(hash(code + sub)) % 41)   # 55..95
        MARK_ROWS += 1
        w(f"INSERT INTO assessment_marks (id, assessment_id, student_id, student_name, marks, "
          f"entered_by, created_at, updated_at) VALUES "
          f"({sql_str(mid)}, {sql_str(aid)}, {sql_str(code)}, {sql_str(name)}, {float(marks)}, "
          f"{sql_str(tid)}, '{ANCHOR.isoformat()} 09:00:00', '{ANCHOR.isoformat()} 09:00:00') "
          f"ON CONFLICT (id) DO NOTHING;")
w("")

# -----------------------------------------------------------------------------
# 14. Exam results (school-admin view of the same scores, stringified)
# -----------------------------------------------------------------------------
w("-- -----------------------------------------------------------------------------")
w("-- 14. Exam results (school-admin Results screen view, stringified)")
w("-- -----------------------------------------------------------------------------")
EXAM_ROWS = 0
for tk, cname, sec, sub in ASSIGNMENTS:
    t = TKEY[tk]
    sk = t["school"]
    sid = uid(f"school:{sk}")
    for klass, section, roll, name in STUDENTS[sk]:
        if klass != cname or section != sec:
            continue
        code = student_code(sk, klass, section, roll)
        mark = 55 + (abs(hash(code + sub)) % 41)
        status = "Exceeding" if mark >= 85 else ("Meeting" if mark >= 65 else "Below")
        rid = uid(f"exam:{sk}:UT1:{cname}:{sub}:{code}")
        EXAM_ROWS += 1
        w(f"INSERT INTO exam_results (id, school_id, test, class_name, subject, student_id, "
          f"student_name, attendance, score, status, trend, created_at, updated_at) VALUES "
          f"({sql_str(rid)}, {sql_str(sid)}, 'Unit Test I', {sql_str(cname)}, {sql_str(sub)}, "
          f"{sql_str(code)}, {sql_str(name)}, '92%', {sql_str(str(mark))}, {sql_str(status)}, "
          f"'+1.2%', '{ANCHOR.isoformat()} 09:00:00', '{ANCHOR.isoformat()} 09:00:00') "
          f"ON CONFLICT (id) DO NOTHING;")
w("")

# -----------------------------------------------------------------------------
# 15. Homework + a few submissions
# -----------------------------------------------------------------------------
w("-- -----------------------------------------------------------------------------")
w("-- 15. Homework + a few submissions per assignment")
w("-- -----------------------------------------------------------------------------")
HW_ROWS = 0
HW_SUB_ROWS = 0
for tk, cname, sec, sub in ASSIGNMENTS:
    t = TKEY[tk]
    sk = t["school"]
    sid = uid(f"school:{sk}")
    tid = uid(f"user:{tk}")
    hid = uid(f"hw:{tk}:{cname}:{sec}:{sub}")
    due = (ANCHOR + timedelta(days=3)).isoformat()
    HW_ROWS += 1
    w(f"INSERT INTO homework (id, school_id, teacher_id, class_name, section, subject, title, "
      f"description, due_date, is_active, created_at, updated_at) VALUES "
      f"({sql_str(hid)}, {sql_str(sid)}, {sql_str(tid)}, {sql_str(cname)}, {sql_str(sec)}, "
      f"{sql_str(sub)}, {sql_str(sub + ' — Worksheet 1')}, {sql_str('Solve Q1-Q10 from the worksheet.')}, "
      f"{sql_str(due)}, TRUE, '{ANCHOR.isoformat()} 09:00:00', '{ANCHOR.isoformat()} 09:00:00') "
      f"ON CONFLICT (id) DO NOTHING;")
    # submit ~half the class
    for i, (klass, section, roll, name) in enumerate(STUDENTS[sk]):
        if klass != cname or section != sec:
            continue
        if i % 2 != 0:    # alternate students
            continue
        code = student_code(sk, klass, section, roll)
        sub_id = uid(f"hwsub:{hid}:{code}")
        HW_SUB_ROWS += 1
        w(f"INSERT INTO homework_submissions (id, homework_id, student_id, status, submitted_at) "
          f"VALUES ({sql_str(sub_id)}, {sql_str(hid)}, {sql_str(code)}, 'submitted', "
          f"'{ANCHOR.isoformat()} 09:00:00') ON CONFLICT (id) DO NOTHING;")
w("")

# -----------------------------------------------------------------------------
# 16. Syllabus coverage progress
# -----------------------------------------------------------------------------
w("-- -----------------------------------------------------------------------------")
w("-- 16. Syllabus coverage (4 units per (class, subject), 2 covered)")
w("-- -----------------------------------------------------------------------------")
SYL_ROWS = 0
seen = set()
for tk, cname, sec, sub in ASSIGNMENTS:
    sk = TKEY[tk]["school"]
    key = (sk, cname, sec, sub)
    if key in seen:
        continue
    seen.add(key)
    sid = uid(f"school:{sk}")
    tid = uid(f"user:{tk}")
    for pos, title in enumerate(["Unit 1 — Foundations", "Unit 2 — Application",
                                  "Unit 3 — Advanced", "Unit 4 — Review"]):
        covered = pos < 2
        suid = uid(f"syl:{sk}:{cname}:{sec}:{sub}:{pos}")
        covered_on = (ANCHOR - timedelta(days=20 - pos*5)).isoformat() if covered else None
        SYL_ROWS += 1
        w(f"INSERT INTO syllabus_units (id, school_id, class_name, section, subject, title, "
          f"position, is_covered, covered_on, covered_by, created_at, updated_at) VALUES "
          f"({sql_str(suid)}, {sql_str(sid)}, {sql_str(cname)}, {sql_str(sec)}, {sql_str(sub)}, "
          f"{sql_str(title)}, {pos}, {sql_str(covered)}, {sql_str(covered_on)}, "
          f"{sql_str(tid) if covered else 'NULL'}, '{ANCHOR.isoformat()} 09:00:00', "
          f"'{ANCHOR.isoformat()} 09:00:00') ON CONFLICT (id) DO NOTHING;")
w("")

# -----------------------------------------------------------------------------
# 17. Announcements
#   Each school: 2 ALL_SCHOOL + 1 CLASS-scoped. event_id is school-prefixed
#   (RE-AUDIT RA-12 says event_id is globally unique — prefix avoids collisions).
# -----------------------------------------------------------------------------
w("-- -----------------------------------------------------------------------------")
w("-- 17. Announcements (school-wide + class-scoped). event_id prefixed by school.")
w("-- -----------------------------------------------------------------------------")
ANN_ROWS = 0
for sk, sdef in SK.items():
    sid = uid(f"school:{sk}")
    admin_id = uid(f"user:{sk}_admin")
    items = [
        ("ALL_SCHOOL", None,              "Holidays", "Summer Break Notification",
         "Summer break starts next Monday. Reopens after 4 weeks.", (ANCHOR - timedelta(days=3)).isoformat()),
        ("ALL_SCHOOL", None,              "PTM",      "Parent-Teacher Meeting",
         "PTM scheduled for next Saturday across all grades.",       (ANCHOR + timedelta(days=4)).isoformat()),
        ("CLASS",      '{"class_name":"Grade 5","section":"A"}' if sk == "s1" else
                       '{"class_name":"Grade 6","section":"A"}',
                                          "Events",  "Class Picnic",
         "Picnic this Friday — packed lunch + cap.",                 (ANCHOR + timedelta(days=2)).isoformat()),
    ]
    for i, (audience_type, audience_filter, typ, title, desc, dt) in enumerate(items):
        aid = uid(f"ann:{sk}:{i}")
        evt = f"{sk}-ann-{i:03d}"
        ANN_ROWS += 1
        w(f"INSERT INTO announcements (id, school_id, event_id, type, title, sub_title, description, "
          f"event_image, date, audience_type, audience_filter, author_role, synced_to_wa, created_by, "
          f"created_at, updated_at) VALUES "
          f"({sql_str(aid)}, {sql_str(sid)}, {sql_str(evt)}, {sql_str(typ)}, {sql_str(title)}, NULL, "
          f"{sql_str(desc)}, NULL, {sql_str(dt)}, {sql_str(audience_type)}, "
          f"{sql_str(audience_filter)}, 'school_admin', FALSE, {sql_str(admin_id)}, "
          f"'{ANCHOR.isoformat()} 09:00:00', '{ANCHOR.isoformat()} 09:00:00') "
          f"ON CONFLICT (id) DO NOTHING;")
w("")

# -----------------------------------------------------------------------------
# 18. Academic calendar + holidays
# -----------------------------------------------------------------------------
w("-- -----------------------------------------------------------------------------")
w("-- 18. Academic calendar events + holiday list")
w("-- -----------------------------------------------------------------------------")
CAL_ROWS = 0; HOL_ROWS = 0
for sk in SK:
    sid = uid(f"school:{sk}")
    cal_items = [
        ("Independence Day",      "Holiday — flag hoisting at 8 AM.", "2026-08-15",  True),
        ("Annual Sports Day",     "Track & field events for all grades.", "2026-09-12", False),
        ("Mid-term Exams Begin",  "Grades 5-9.",                       "2026-10-05",  False),
        ("Diwali Break",          "School closed for Diwali week.",    "2026-11-02",  True),
        ("Annual Day",            "Cultural showcase by students.",    "2026-12-15",  False),
    ]
    for i, (title, desc, dt, is_h) in enumerate(cal_items):
        cid = uid(f"cal:{sk}:{i}")
        day_name = datetime.strptime(dt, "%Y-%m-%d").strftime("%A")
        evt = f"{sk}-cal-{i:03d}"
        CAL_ROWS += 1
        w(f"INSERT INTO academic_calendar (id, school_id, event_id, date, day, event_title, "
          f"event_description, standard, is_holiday, created_at) VALUES "
          f"({sql_str(cid)}, {sql_str(sid)}, {sql_str(evt)}, {sql_str(dt)}, {sql_str(day_name)}, "
          f"{sql_str(title)}, {sql_str(desc)}, NULL, {sql_str(is_h)}, "
          f"'{ANCHOR.isoformat()} 09:00:00') ON CONFLICT (id) DO NOTHING;")
    # holidays
    hol_items = [
        ("Republic Day", "2026-01-26", "Public", "yearly"),
        ("Holi",         "2026-03-06", "Public", "yearly"),
        ("Eid",          "2026-04-21", "Public", "yearly"),
    ]
    for i, (title, dt, typ, freq) in enumerate(hol_items):
        hid = uid(f"hol:{sk}:{i}")
        HOL_ROWS += 1
        w(f"INSERT INTO holiday_list (id, school_id, date, title, type, frequency, created_at) "
          f"VALUES ({sql_str(hid)}, {sql_str(sid)}, {sql_str(dt)}, {sql_str(title)}, "
          f"{sql_str(typ)}, {sql_str(freq)}, '{ANCHOR.isoformat()} 09:00:00') "
          f"ON CONFLICT (id) DO NOTHING;")
w("")

# -----------------------------------------------------------------------------
# 19. Fee records (mix of PAID / DUE / OVERDUE for every parent-child pair)
# -----------------------------------------------------------------------------
w("-- -----------------------------------------------------------------------------")
w("-- 19. Fee records — 3 rows per child (PAID + DUE + OVERDUE), realistic spread")
w("-- -----------------------------------------------------------------------------")
FEE_ROWS = 0
fee_items = [
    ("Term 1 Tuition",       "Tuition fee for Term 1",      25000.0, "PAID",    "Tuition",   (ANCHOR - timedelta(days=60)).isoformat()),
    ("Term 2 Tuition",       "Tuition fee for Term 2",      25000.0, "DUE",     "Tuition",   (ANCHOR + timedelta(days=15)).isoformat()),
    ("Transport — Quarter 2","School bus, Quarter 2",        4500.0, "OVERDUE", "Transport", (ANCHOR - timedelta(days=10)).isoformat()),
]
for p in PARENTS:
    pid = uid(f"user:{p['key']}")
    sk = p["school"]; sid = uid(f"school:{sk}")
    for klass, section, roll, name in p["children"]:
        code = student_code(sk, klass, section, roll)
        cid = uid(f"child:{p['key']}:{code}")
        for i, (title, desc, amt, status, cat, due) in enumerate(fee_items):
            fid = uid(f"fee:{p['key']}:{code}:{i}")
            FEE_ROWS += 1
            w(f"INSERT INTO fee_records (id, parent_id, child_id, school_id, title, description, "
              f"amount, currency, due_date, status, category, created_at, updated_at) VALUES "
              f"({sql_str(fid)}, {sql_str(pid)}, {sql_str(cid)}, {sql_str(sid)}, {sql_str(title)}, "
              f"{sql_str(desc)}, {amt}, 'INR', {sql_str(due)}, {sql_str(status)}, {sql_str(cat)}, "
              f"'{ANCHOR.isoformat()} 09:00:00', '{ANCHOR.isoformat()} 09:00:00') "
              f"ON CONFLICT (id) DO NOTHING;")
w("")

# -----------------------------------------------------------------------------
# 20. Scholarships catalogue + parent applications
#
# Requires schema-patch-2026-06-07.sql to have run first (RA-05).
# -----------------------------------------------------------------------------
w("-- -----------------------------------------------------------------------------")
w("-- 20. Scholarships catalogue (global) + per-parent applications")
w("-- REQUIRES: scripts/schema-patch-2026-06-07.sql to have run first.")
w("-- -----------------------------------------------------------------------------")
SCH_ROWS = 0; APP_ROWS = 0
scholarships = [
    ("STEM Excellence Award",   "For grades 6-9 showing strong STEM aptitude.", "₹45,000", "5d : 12h", "Merit Based", False),
    ("Sports Star Scholarship", "For state-level sportspersons.",                "₹30,000", "9d : 02h", "Sports",      False),
    ("Need-based Grant",        "Income-based tuition support.",                 "₹60,000", "2d : 03h", "Need Based",  True),
]
for i, (title, desc, amt, tl, cat, crit) in enumerate(scholarships):
    sch_id = uid(f"scholarship:{i}")
    SCH_ROWS += 1
    w(f"INSERT INTO scholarships (id, title, description, amount, time_left, category, is_critical, "
      f"position, is_active, created_at, updated_at) VALUES "
      f"({sql_str(sch_id)}, {sql_str(title)}, {sql_str(desc)}, {sql_str(amt)}, {sql_str(tl)}, "
      f"{sql_str(cat)}, {sql_str(crit)}, {i}, TRUE, "
      f"'{ANCHOR.isoformat()} 09:00:00', '{ANCHOR.isoformat()} 09:00:00') "
      f"ON CONFLICT (id) DO NOTHING;")
# 1 application per parent (first 4 parents)
apps = [
    ("Indian Institute of Science",  "Young Scholars Program",  "Received"),
    ("IIT Kanpur Outreach",          "Math Olympiad Track",     "Under Review"),
    ("Delhi Public Trust",           "Need-based Aid",          "Shortlisted"),
    ("Tata Trusts",                  "Merit Award",             "Received"),
]
for p, (inst, prog, st) in zip(PARENTS[:4], apps):
    pid = uid(f"user:{p['key']}")
    aid = uid(f"sch_app:{p['key']}")
    APP_ROWS += 1
    w(f"INSERT INTO scholarship_applications (id, parent_id, institution, program, status, icon_name, "
      f"position, created_at, updated_at) VALUES "
      f"({sql_str(aid)}, {sql_str(pid)}, {sql_str(inst)}, {sql_str(prog)}, {sql_str(st)}, 'school', "
      f"0, '{ANCHOR.isoformat()} 09:00:00', '{ANCHOR.isoformat()} 09:00:00') "
      f"ON CONFLICT (id) DO NOTHING;")
w("")

w("COMMIT;")
w("")

# -----------------------------------------------------------------------------
# 21. Verification SELECTs
#
# Run AFTER the COMMIT above. Each labeled count or invariant has an expected
# value documented inline in the schema-patch's own verify block. Here the
# expectations are kept off the terminating `;` lines so psql sees the
# terminator cleanly.
# -----------------------------------------------------------------------------
n_students = sum(len(v) for v in STUDENTS.values())
n_children = N_CHILDREN
n_admins = len(ADMINS)
n_teachers = len(TEACHERS)
n_parents = len(PARENTS)
n_assignments = len(ASSIGNMENTS)

w("-- =============================================================================")
w("-- 21. VERIFICATION SELECTs (run after COMMIT)")
w("--")
w("-- Expected counts (left-hand label = entity, right-hand value = expected n):")
w(f"--   schools           : 2")
w(f"--   admins            : {n_admins}")
w(f"--   teachers          : {n_teachers}")
w(f"--   parents           : {n_parents}")
w(f"--   students          : {n_students}")
w(f"--   children          : {n_children}")
w(f"--   assignments       : {n_assignments}")
w(f"--   attendance        : {ATT_ROWS}")
w(f"--   assessments       : {n_assignments}")
w(f"--   marks             : {MARK_ROWS}")
w(f"--   exam_results      : {EXAM_ROWS}")
w(f"--   homework          : {HW_ROWS}")
w(f"--   fees              : {FEE_ROWS}")
w(f"--   announcements     : {ANN_ROWS}")
w(f"--   scholarships      : {SCH_ROWS}")
w(f"--   scholarship_apps  : {APP_ROWS}")
w("--")
w("-- All multi-tenancy 'bad' counts at the end must be 0.")
w("-- =============================================================================")
w("SELECT 'schools'         AS entity, count(*) AS n FROM schools WHERE slug IN ('sunrise-public-school','greenfield-academy')")
w("UNION ALL SELECT 'admins',          count(*) FROM app_users WHERE role='school_admin'")
w("UNION ALL SELECT 'teachers',        count(*) FROM app_users WHERE role='teacher'")
w("UNION ALL SELECT 'parents',         count(*) FROM app_users WHERE role='parent'")
w("UNION ALL SELECT 'students',        count(*) FROM students")
w("UNION ALL SELECT 'children',        count(*) FROM children")
w("UNION ALL SELECT 'assignments',     count(*) FROM teacher_subject_assignments")
w("UNION ALL SELECT 'attendance',      count(*) FROM attendance_records")
w("UNION ALL SELECT 'assessments',     count(*) FROM assessments")
w("UNION ALL SELECT 'marks',           count(*) FROM assessment_marks")
w("UNION ALL SELECT 'exam_results',    count(*) FROM exam_results")
w("UNION ALL SELECT 'homework',        count(*) FROM homework")
w("UNION ALL SELECT 'fees',            count(*) FROM fee_records")
w("UNION ALL SELECT 'announcements',   count(*) FROM announcements")
w("UNION ALL SELECT 'scholarships',    count(*) FROM scholarships")
w("UNION ALL SELECT 'scholarship_apps',count(*) FROM scholarship_applications")
w("ORDER BY entity;")
w("")
w("-- MULTI-TENANCY ASSERTIONS — every 'bad' count below must be 0.")
w("--   * parents must NOT carry a school_id (their tenancy is via children.school_id)")
w("--   * every seeded child must carry a school_id")
w("SELECT 'orphan_parents_with_school' AS check_name, count(*) AS bad")
w("  FROM app_users WHERE role='parent' AND school_id IS NOT NULL")
w("UNION ALL")
w("SELECT 'children_without_school', count(*)")
w("  FROM children WHERE school_id IS NULL;")
w("")
w("-- CROSS-SCHOOL LEAK ASSERTION — must return 0.")
w("--   A child's school_id must equal the canonical student's school_id.")
w("SELECT count(*) AS mismatched_child_school")
w("  FROM children c JOIN students s ON s.student_code = c.student_code")
w("  WHERE c.school_id <> s.school_id;")
w("")
w("-- =============================================================================")
w("-- END seed-2026-06-07.sql")
w("-- =============================================================================")


sql = "\n".join(out) + "\n"
with open("scripts/seed-2026-06-07.sql", "w") as f:
    f.write(sql)
print("WROTE scripts/seed-2026-06-07.sql", len(sql), "bytes,", sql.count("\n"), "lines")
print(f"students={n_students} children={n_children} assignments={n_assignments} "
      f"attendance={ATT_ROWS} marks={MARK_ROWS} exam_results={EXAM_ROWS} "
      f"homework={HW_ROWS} fees={FEE_ROWS}")
