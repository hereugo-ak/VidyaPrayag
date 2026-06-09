#!/usr/bin/env python3
"""
_gen_seed_expansion.py — VidyaPrayag FULL-SCHOOL mock + seed EXPANSION
======================================================================
Generated output: scripts/seed-expansion-2026-06-09.sql

WHY
---
The original dataset lives in scripts/_gen_seed.py -> scripts/seed-2026-06-07.sql.
A teammate accidentally deleted the PARENT credentials, and the team also asked
for a much richer fixture: a FULL school mock with many classes, sections,
students, teachers, subjects and parents.

This script emits ONE standalone, additive, fully-idempotent SQL file that:

  A. RECOVERS the 11 original parents (+ their children) using the exact same
     deterministic UUIDs as seed-2026-06-07.sql — so a mistaken DELETE of the
     parent rows is fixed by simply running this file.

  B. EXPANDS schools 1 & 2 with extra teachers / subjects / students / parents.

  C. ADDS a brand-new, COMPLETE School 3 ("Holy Trinity Convent") with:
       - school row + school_philosophy + storage_metrics + onboarded_at
       - 1 admin (email+password login)
       - 6 classes across 9 sections
       - subjects per class
       - 10 teachers + faculty rows + teacher_subject_assignments
       - ~40 students across the sections
       - 12 parents (OTP-only) each linked to 1–2 children

Everything reuses the SAME helpers / namespace / salt convention as
_gen_seed.py, so UUIDs never collide with the original seed and are reproducible.

SAFETY
  * IDEMPOTENT  — every INSERT has ON CONFLICT (...) DO NOTHING.
  * ATOMIC      — wrapped in BEGIN; ... COMMIT;.
  * ADDITIVE    — never UPDATEs/DELETEs an existing row.
  * PORTABLE    — pure Postgres; no SQLite-isms; deterministic UUIDv5 supplied.

PREREQS (run first, in order):
  schema-all-in-one-2026-06-07.sql  →  schema-patch-2026-06-08-part2.sql
  (the part2 patch adds children.student_code). seed-2026-06-07.sql is optional
  for this file (we re-create the originals), but recommended for the full set.

Run:
    python3 scripts/_gen_seed_expansion.py > scripts/seed-expansion-2026-06-09.sql
"""
import base64
import hashlib
import uuid
from datetime import date

# ---------------------------------------------------------------------------
# Constants — identical to scripts/_gen_seed.py (keep in lockstep)
# ---------------------------------------------------------------------------
PBKDF2_ITER = 120_000
NS = uuid.UUID("ab8a1c2c-1c4b-4b8a-9c1a-2026060700be")
ANCHOR = date(2026, 6, 7)
TS = f"{ANCHOR.isoformat()} 09:00:00"


def uid(label: str) -> str:
    return str(uuid.uuid5(NS, label))


def pwhash(pw: str, salt_ascii: str) -> str:
    salt = salt_ascii.encode("ascii")
    assert len(salt) == 16, f"salt must be 16 bytes, got {len(salt)} for {salt_ascii!r}"
    dk = hashlib.pbkdf2_hmac("sha256", pw.encode("utf-8"), salt, PBKDF2_ITER, dklen=32)
    return f"pbkdf2${PBKDF2_ITER}${base64.b64encode(salt).decode()}${base64.b64encode(dk).decode()}"


def q(v):
    if v is None:
        return "NULL"
    if isinstance(v, bool):
        return "TRUE" if v else "FALSE"
    return "'" + str(v).replace("'", "''") + "'"


def student_code(sk: str, klass: str, section: str, roll) -> str:
    grade_num = "".join(ch for ch in klass if ch.isdigit())
    return f"{sk.upper()}-G{grade_num}{section}-{int(roll):03d}"


def class_code(klass: str) -> str:
    return "CLS-" + "".join(ch for ch in klass if ch.isdigit())


def sub_code(sub: str) -> str:
    return sub[:3].upper()


out: list[str] = []
def w(line: str = ""):
    out.append(line)


# School ids — s1/s2 unchanged; s3 is brand new.
SCHOOL_KEYS = {"s1": uid("school:s1"), "s2": uid("school:s2"), "s3": uid("school:s3")}

# ===========================================================================
# A. ORIGINAL parents (verbatim from _gen_seed.py) — RECOVERY
# ===========================================================================
ORIGINAL_PARENTS = [
    {"key": "s1_p_arav",  "school": "s1", "phone": "+919811100001", "full_name": "Rohit Singh",
     "children": [("Grade 3", "A", "1", "Aarav Singh")]},
    {"key": "s1_p_diya",  "school": "s1", "phone": "+919811100002", "full_name": "Sunita Mishra",
     "children": [("Grade 3", "B", "1", "Diya Mishra")]},
    {"key": "s1_p_ishan", "school": "s1", "phone": "+919811100003", "full_name": "Vikram Kapoor",
     "children": [("Grade 5", "A", "1", "Ishan Kapoor"), ("Grade 8", "A", "4", "Pari Agarwal")]},
    {"key": "s1_p_jiya",  "school": "s1", "phone": "+919811100004", "full_name": "Reena Saxena",
     "children": [("Grade 5", "A", "2", "Jiya Saxena")]},
    {"key": "s1_p_manav", "school": "s1", "phone": "+919811100005", "full_name": "Pankaj Joshi",
     "children": [("Grade 8", "A", "1", "Manav Joshi"), ("Grade 8", "A", "2", "Nisha Bansal")]},
    {"key": "s2_p_rey",  "school": "s2", "phone": "+919822200001", "full_name": "Anand Kumar",
     "children": [("Grade 4", "A", "1", "Reyansh Kumar")]},
    {"key": "s2_p_saa",  "school": "s2", "phone": "+919822200002", "full_name": "Madhuri Bose",
     "children": [("Grade 4", "A", "2", "Saanvi Bose")]},
    {"key": "s2_p_uday", "school": "s2", "phone": "+919822200003", "full_name": "Subhash Das",
     "children": [("Grade 6", "A", "1", "Udayan Das"), ("Grade 9", "A", "1", "Yash Banerjee")]},
    {"key": "s2_p_vih",  "school": "s2", "phone": "+919822200004", "full_name": "Priyanka Sen",
     "children": [("Grade 6", "A", "2", "Vihaan Sen")]},
    {"key": "s2_p_xen",  "school": "s2", "phone": "+919822200005", "full_name": "Soumitra Chowdhury",
     "children": [("Grade 6", "B", "1", "Xen Chowdhury")]},
    {"key": "s2_p_zar",  "school": "s2", "phone": "+919822200006", "full_name": "Tanvi Mukherjee",
     "children": [("Grade 9", "A", "2", "Zara Mukherjee"), ("Grade 9", "A", "3", "Aadya Ghosh")]},
]

# ===========================================================================
# B. EXPANSION to schools 1 & 2 (extra teachers / subjects / students / parents)
# ===========================================================================
B_TEACHERS = [
    {"school": "s1", "key": "s1_t_sunil", "full_name": "Sunil Rastogi",
     "email": "sunil.rastogi@sunrise.edu.in", "phone": "+919876500024",
     "pw": "Teacher@2026", "salt": "VP_SEED_S1_TEA05"},
    {"school": "s1", "key": "s1_t_geeta", "full_name": "Geeta Bhatt",
     "email": "geeta.bhatt@sunrise.edu.in", "phone": "+919876500025",
     "pw": "Teacher@2026", "salt": "VP_SEED_S1_TEA06"},
    {"school": "s2", "key": "s2_t_farah", "full_name": "Farah Khan",
     "email": "farah.khan@greenfield.edu.in", "phone": "+919876500123",
     "pw": "Teacher@2026", "salt": "VP_SEED_S2_TEA04"},
    {"school": "s2", "key": "s2_t_deepak", "full_name": "Deepak Rao",
     "email": "deepak.rao@greenfield.edu.in", "phone": "+919876500124",
     "pw": "Teacher@2026", "salt": "VP_SEED_S2_TEA05"},
]
B_SUBJECTS = [
    ("s1", "Grade 3", "Hindi"), ("s1", "Grade 5", "Computer Science"),
    ("s1", "Grade 8", "Physical Education"), ("s2", "Grade 4", "Hindi"),
    ("s2", "Grade 6", "Computer Science"), ("s2", "Grade 9", "Physical Education"),
]
B_ASSIGNMENTS = [
    ("s1_t_sunil", "s1", "Grade 3", "A", "Hindi"),
    ("s1_t_sunil", "s1", "Grade 5", "A", "Computer Science"),
    ("s1_t_geeta", "s1", "Grade 8", "A", "Physical Education"),
    ("s2_t_farah", "s2", "Grade 4", "A", "Hindi"),
    ("s2_t_farah", "s2", "Grade 6", "A", "Computer Science"),
    ("s2_t_deepak", "s2", "Grade 9", "A", "Physical Education"),
]
B_STUDENTS = [
    ("s1", "Grade 3", "A", "4", "Tara Sethi"), ("s1", "Grade 5", "A", "5", "Veer Malhotra"),
    ("s1", "Grade 8", "A", "5", "Anya Chopra"), ("s2", "Grade 4", "A", "4", "Kabir Sinha"),
    ("s2", "Grade 6", "A", "4", "Myra Pillai"), ("s2", "Grade 9", "A", "8", "Rehan Qureshi"),
]
B_PARENTS = [
    {"key": "s1_p_tara", "school": "s1", "phone": "+919811100006", "full_name": "Sneha Sethi",
     "children": [("Grade 3", "A", "4", "Tara Sethi")]},
    {"key": "s1_p_veer", "school": "s1", "phone": "+919811100007", "full_name": "Gaurav Malhotra",
     "children": [("Grade 5", "A", "5", "Veer Malhotra")]},
    {"key": "s1_p_anya", "school": "s1", "phone": "+919811100008", "full_name": "Ritu Chopra",
     "children": [("Grade 8", "A", "5", "Anya Chopra")]},
    {"key": "s2_p_kabir", "school": "s2", "phone": "+919822200007", "full_name": "Alok Sinha",
     "children": [("Grade 4", "A", "4", "Kabir Sinha")]},
    {"key": "s2_p_myra", "school": "s2", "phone": "+919822200008", "full_name": "Lakshmi Pillai",
     "children": [("Grade 6", "A", "4", "Myra Pillai")]},
    {"key": "s2_p_rehan", "school": "s2", "phone": "+919822200009", "full_name": "Imran Qureshi",
     "children": [("Grade 9", "A", "8", "Rehan Qureshi")]},
]

# ===========================================================================
# C. FULL NEW SCHOOL 3 — "Holy Trinity Convent" (Prayagraj)
# ===========================================================================
SCHOOL3 = {
    "key": "s3", "slug": "holy-trinity-convent", "name": "Holy Trinity Convent",
    "board": "CBSE", "medium": "English", "gender": "co_ed",
    "phone": "+919876500201", "email": "office@holytrinity.edu.in",
    "principal": "Sr. Maria Joseph", "principal_phone": "+919876500202",
    "principal_email": "principal@holytrinity.edu.in",
    "city": "Prayagraj", "district": "Prayagraj", "state": "Uttar Pradesh",
    "pincode": "211001", "address": "7 Cathedral Road, Civil Lines, Prayagraj",
    "logo": "https://cdn.vidyaprayag.local/seed/holytrinity-logo.png",
    "brand": "#7c3aed", "lat": 25.4358, "lng": 81.8463,
    "core_mission": "Faith, knowledge and service for every child.",
    "learning_model": "Holistic CBSE curriculum with values education.",
    "primary_language": "English",
}
S3_ADMIN = {
    "school": "s3", "key": "s3_admin", "full_name": "Maria Joseph",
    "email": "admin@holytrinity.edu.in", "phone": "+919876500210",
    "pw": "Trinity@2026", "salt": "VP_SEED_S3_ADMIN",  # 16 bytes
}
# 6 classes, several with multiple sections (9 sections total).
S3_CLASSES = [
    ("Grade 1",  ["A", "B"]),
    ("Grade 2",  ["A"]),
    ("Grade 6",  ["A", "B"]),
    ("Grade 7",  ["A"]),
    ("Grade 10", ["A", "B"]),
    ("Grade 12", ["A"]),
]
S3_SUBJECTS_BY_CLASS = {
    "Grade 1":  ["English", "Mathematics", "EVS"],
    "Grade 2":  ["English", "Mathematics", "EVS"],
    "Grade 6":  ["English", "Mathematics", "Science", "Social Studies", "Hindi"],
    "Grade 7":  ["English", "Mathematics", "Science", "Social Studies", "Hindi"],
    "Grade 10": ["English", "Mathematics", "Science", "Social Studies", "Computer Science"],
    "Grade 12": ["English", "Physics", "Chemistry", "Mathematics", "Computer Science"],
}
# 10 teachers
S3_TEACHERS = [
    {"school": "s3", "key": "s3_t_anita",  "full_name": "Anita D'Souza",  "email": "anita.dsouza@holytrinity.edu.in",  "phone": "+919876500220", "pw": "Teacher@2026", "salt": "VP_SEED_S3_TEA01"},
    {"school": "s3", "key": "s3_t_brian",  "full_name": "Brian Pereira",  "email": "brian.pereira@holytrinity.edu.in",  "phone": "+919876500221", "pw": "Teacher@2026", "salt": "VP_SEED_S3_TEA02"},
    {"school": "s3", "key": "s3_t_clara",  "full_name": "Clara Fernandes","email": "clara.fernandes@holytrinity.edu.in","phone": "+919876500222", "pw": "Teacher@2026", "salt": "VP_SEED_S3_TEA03"},
    {"school": "s3", "key": "s3_t_david",  "full_name": "David Thomas",   "email": "david.thomas@holytrinity.edu.in",   "phone": "+919876500223", "pw": "Teacher@2026", "salt": "VP_SEED_S3_TEA04"},
    {"school": "s3", "key": "s3_t_esha",   "full_name": "Esha Varghese",  "email": "esha.varghese@holytrinity.edu.in",  "phone": "+919876500224", "pw": "Teacher@2026", "salt": "VP_SEED_S3_TEA05"},
    {"school": "s3", "key": "s3_t_fiona",  "full_name": "Fiona Mathew",   "email": "fiona.mathew@holytrinity.edu.in",   "phone": "+919876500225", "pw": "Teacher@2026", "salt": "VP_SEED_S3_TEA06"},
    {"school": "s3", "key": "s3_t_george", "full_name": "George Kurian",  "email": "george.kurian@holytrinity.edu.in",  "phone": "+919876500226", "pw": "Teacher@2026", "salt": "VP_SEED_S3_TEA07"},
    {"school": "s3", "key": "s3_t_hema",   "full_name": "Hema Nair",      "email": "hema.nair@holytrinity.edu.in",      "phone": "+919876500227", "pw": "Teacher@2026", "salt": "VP_SEED_S3_TEA08"},
    {"school": "s3", "key": "s3_t_irfan",  "full_name": "Irfan Ali",      "email": "irfan.ali@holytrinity.edu.in",      "phone": "+919876500228", "pw": "Teacher@2026", "salt": "VP_SEED_S3_TEA09"},
    {"school": "s3", "key": "s3_t_jaya",   "full_name": "Jaya Menon",     "email": "jaya.menon@holytrinity.edu.in",     "phone": "+919876500229", "pw": "Teacher@2026", "salt": "VP_SEED_S3_TEA10"},
]
# (teacher_key, class, section, subject)
S3_ASSIGNMENTS = [
    ("s3_t_anita",  "Grade 1",  "A", "English"),
    ("s3_t_anita",  "Grade 1",  "B", "English"),
    ("s3_t_brian",  "Grade 1",  "A", "Mathematics"),
    ("s3_t_clara",  "Grade 2",  "A", "English"),
    ("s3_t_clara",  "Grade 2",  "A", "EVS"),
    ("s3_t_david",  "Grade 6",  "A", "Mathematics"),
    ("s3_t_david",  "Grade 6",  "B", "Mathematics"),
    ("s3_t_esha",   "Grade 6",  "A", "Science"),
    ("s3_t_fiona",  "Grade 7",  "A", "English"),
    ("s3_t_fiona",  "Grade 7",  "A", "Social Studies"),
    ("s3_t_george", "Grade 10", "A", "Mathematics"),
    ("s3_t_george", "Grade 10", "B", "Mathematics"),
    ("s3_t_hema",   "Grade 10", "A", "Science"),
    ("s3_t_irfan",  "Grade 12", "A", "Physics"),
    ("s3_t_irfan",  "Grade 12", "A", "Mathematics"),
    ("s3_t_jaya",   "Grade 12", "A", "Computer Science"),
]
# Students — many across sections. (class, section, roll, name)
S3_STUDENTS = [
    ("Grade 1", "A", 1, "Aanya Joseph"), ("Grade 1", "A", 2, "Ben Mathew"),
    ("Grade 1", "A", 3, "Cyrus Paul"),   ("Grade 1", "A", 4, "Daniela Rosario"),
    ("Grade 1", "B", 1, "Ethan Vaz"),    ("Grade 1", "B", 2, "Faith Lobo"),
    ("Grade 1", "B", 3, "Gabriel Pinto"),
    ("Grade 2", "A", 1, "Hannah Crasto"), ("Grade 2", "A", 2, "Ivan Misquitta"),
    ("Grade 2", "A", 3, "Julia Dias"),    ("Grade 2", "A", 4, "Kevin Almeida"),
    ("Grade 6", "A", 1, "Liam Saldanha"), ("Grade 6", "A", 2, "Maria Coelho"),
    ("Grade 6", "A", 3, "Nathan Rebello"),("Grade 6", "A", 4, "Olivia Gomes"),
    ("Grade 6", "B", 1, "Peter Noronha"), ("Grade 6", "B", 2, "Queenie Dsa"),
    ("Grade 6", "B", 3, "Ryan Castelino"),
    ("Grade 7", "A", 1, "Sara Menezes"),  ("Grade 7", "A", 2, "Tobias Rodrigues"),
    ("Grade 7", "A", 3, "Ursula Baptista"),("Grade 7", "A", 4, "Victor Mascarenhas"),
    ("Grade 10","A", 1, "Wendy Carvalho"),("Grade 10","A", 2, "Xavier Furtado"),
    ("Grade 10","A", 3, "Yvonne Cardoso"),("Grade 10","A", 4, "Zane Monteiro"),
    ("Grade 10","B", 1, "Aaron Pereira"), ("Grade 10","B", 2, "Bianca Tellis"),
    ("Grade 10","B", 3, "Caleb Sequeira"),("Grade 10","B", 4, "Diana Quadros"),
    ("Grade 12","A", 1, "Elias Mendonca"),("Grade 12","A", 2, "Farida Shaikh"),
    ("Grade 12","A", 3, "Gavin Dcosta"),  ("Grade 12","A", 4, "Helena Rodricks"),
    ("Grade 12","A", 5, "Ishaan Barreto"),("Grade 12","A", 6, "Joanna Vaz"),
]
# Parents (12) each linked to 1–2 of the S3 students above.
S3_PARENTS = [
    {"key": "s3_p_aanya", "phone": "+919833300001", "full_name": "Rosario Joseph",
     "children": [("Grade 1", "A", 1, "Aanya Joseph")]},
    {"key": "s3_p_ben",   "phone": "+919833300002", "full_name": "Linette Mathew",
     "children": [("Grade 1", "A", 2, "Ben Mathew")]},
    {"key": "s3_p_ethan", "phone": "+919833300003", "full_name": "Wilson Vaz",
     "children": [("Grade 1", "B", 1, "Ethan Vaz"), ("Grade 6", "A", 4, "Olivia Gomes")]},
    {"key": "s3_p_hannah","phone": "+919833300004", "full_name": "Agnes Crasto",
     "children": [("Grade 2", "A", 1, "Hannah Crasto")]},
    {"key": "s3_p_liam",  "phone": "+919833300005", "full_name": "Joseph Saldanha",
     "children": [("Grade 6", "A", 1, "Liam Saldanha")]},
    {"key": "s3_p_peter", "phone": "+919833300006", "full_name": "Cynthia Noronha",
     "children": [("Grade 6", "B", 1, "Peter Noronha")]},
    {"key": "s3_p_sara",  "phone": "+919833300007", "full_name": "Ralph Menezes",
     "children": [("Grade 7", "A", 1, "Sara Menezes"), ("Grade 7", "A", 2, "Tobias Rodrigues")]},
    {"key": "s3_p_wendy", "phone": "+919833300008", "full_name": "Glenda Carvalho",
     "children": [("Grade 10", "A", 1, "Wendy Carvalho")]},
    {"key": "s3_p_aaron", "phone": "+919833300009", "full_name": "Melwyn Pereira",
     "children": [("Grade 10", "B", 1, "Aaron Pereira")]},
    {"key": "s3_p_elias", "phone": "+919833300010", "full_name": "Savio Mendonca",
     "children": [("Grade 12", "A", 1, "Elias Mendonca")]},
    {"key": "s3_p_farida","phone": "+919833300011", "full_name": "Naseem Shaikh",
     "children": [("Grade 12", "A", 2, "Farida Shaikh")]},
    {"key": "s3_p_ishaan","phone": "+919833300012", "full_name": "Royston Barreto",
     "children": [("Grade 12", "A", 5, "Ishaan Barreto"), ("Grade 12", "A", 6, "Joanna Vaz")]},
]

# ===========================================================================
# EMIT
# ===========================================================================
w("-- =============================================================================")
w("-- seed-expansion-2026-06-09.sql")
w("-- VidyaPrayag — FULL-SCHOOL mock + additive seed expansion (PostgreSQL / Supabase)")
w("-- -----------------------------------------------------------------------------")
w("-- Generated by scripts/_gen_seed_expansion.py — DO NOT hand-edit.")
w("--")
w("-- CONTENTS")
w("--   A. Recovers the 11 ORIGINAL parents (+children) — same deterministic UUIDs.")
w("--   B. Expands schools 1 & 2 (extra teachers/subjects/students/parents).")
w("--   C. Adds a COMPLETE new School 3 'Holy Trinity Convent' (Prayagraj):")
w("--        admin + philosophy + storage + 6 classes / 9 sections + subjects +")
w("--        10 teachers (+faculty +assignments) + ~36 students + 12 parents.")
w("--")
w("-- SAFETY: idempotent (ON CONFLICT DO NOTHING), atomic (BEGIN/COMMIT), additive.")
w("-- PREREQS: schema-all-in-one-2026-06-07.sql + schema-patch-2026-06-08-part2.sql")
w("--          (children.student_code). seed-2026-06-07.sql optional (we recover the")
w("--          originals), recommended for the full combined dataset.")
w("--")
w("-- Plaintext credentials: seed-credentials-2026-06-07.md (keep out of prod).")
w("-- =============================================================================")
w("")
w("BEGIN;")
w("")

# ---- C1. School 3 row + philosophy + storage --------------------------------
s3 = SCHOOL3
sid3 = SCHOOL_KEYS["s3"]
w("-- -----------------------------------------------------------------------------")
w("-- C1. School 3 — schools + school_philosophy + storage_metrics")
w("-- -----------------------------------------------------------------------------")
w("INSERT INTO schools (id, name, slug, board, medium, school_gender, contact_phone, "
  "contact_email, principal_name, principal_phone, principal_email, full_address, city, "
  "district, state, pincode, logo_url, brand_color, latitude, longitude, is_active, "
  "onboarded_at, created_at, updated_at)")
w(f"VALUES ({q(sid3)}, {q(s3['name'])}, {q(s3['slug'])}, {q(s3['board'])}, {q(s3['medium'])}, "
  f"{q(s3['gender'])}, {q(s3['phone'])}, {q(s3['email'])}, {q(s3['principal'])}, "
  f"{q(s3['principal_phone'])}, {q(s3['principal_email'])}, {q(s3['address'])}, {q(s3['city'])}, "
  f"{q(s3['district'])}, {q(s3['state'])}, {q(s3['pincode'])}, {q(s3['logo'])}, {q(s3['brand'])}, "
  f"{s3['lat']}, {s3['lng']}, TRUE, '{TS}', '{TS}', '{TS}')")
w("ON CONFLICT (id) DO NOTHING;")
w("INSERT INTO school_philosophy (school_id, core_mission, learning_model, primary_language, "
  "public_profile, updated_at)")
w(f"VALUES ({q(sid3)}, {q(s3['core_mission'])}, {q(s3['learning_model'])}, "
  f"{q(s3['primary_language'])}, TRUE, '{TS}')")
w("ON CONFLICT (school_id) DO NOTHING;")
w("INSERT INTO storage_metrics (school_id, total_storage, storage_used, bytes_used, updated_at)")
w(f"VALUES ({q(sid3)}, '10 GB', '0 B', 0, '{TS}')")
w("ON CONFLICT (school_id) DO NOTHING;")
w("")

# ---- C2. School 3 admin -----------------------------------------------------
w("-- -----------------------------------------------------------------------------")
w("-- C2. School 3 admin user (email + password login)")
w("-- -----------------------------------------------------------------------------")
a = S3_ADMIN
aid = uid(f"user:{a['key']}")
ah = pwhash(a["pw"], a["salt"])
w("INSERT INTO app_users (id, school_id, role, full_name, phone, email, password_hash, "
  "language_pref, is_phone_verified, is_email_verified, profile_completed, is_active, "
  "created_at, updated_at)")
w(f"VALUES ({q(aid)}, {q(sid3)}, 'school_admin', {q(a['full_name'])}, {q(a['phone'])}, "
  f"{q(a['email'])}, {q(ah)}, 'en', TRUE, TRUE, TRUE, TRUE, '{TS}', '{TS}')")
w("ON CONFLICT (id) DO NOTHING;")
w("")

# ---- C3. School 3 classes ---------------------------------------------------
w("-- -----------------------------------------------------------------------------")
w("-- C3. School 3 classes + sections")
w("-- -----------------------------------------------------------------------------")
for klass, sections in S3_CLASSES:
    code = class_code(klass)
    cid = uid(f"class:s3:{code}")
    sections_json = "[" + ",".join(f'"{s}"' for s in sections) + "]"
    w("INSERT INTO school_classes (id, school_id, code, name, sections, created_at)")
    w(f"VALUES ({q(cid)}, {q(sid3)}, {q(code)}, {q(klass)}, {q(sections_json)}, '{TS}')")
    w("ON CONFLICT (id) DO NOTHING;")
    w("")

# ---- C4. School 3 subjects --------------------------------------------------
w("-- -----------------------------------------------------------------------------")
w("-- C4. School 3 subjects (per class)")
w("-- -----------------------------------------------------------------------------")
for klass, _sections in S3_CLASSES:
    code = class_code(klass)
    cid = uid(f"class:s3:{code}")
    for sub in S3_SUBJECTS_BY_CLASS[klass]:
        sub_id = uid(f"subject:s3:{code}:{sub}")
        w("INSERT INTO school_subjects (id, class_id, sub_name, sub_code, teacher_assigned, created_at)")
        w(f"VALUES ({q(sub_id)}, {q(cid)}, {q(sub)}, {q(sub_code(sub))}, NULL, '{TS}')")
        w("ON CONFLICT (id) DO NOTHING;")
    w("")

# ---- C5. School 3 teachers + faculty ---------------------------------------
w("-- -----------------------------------------------------------------------------")
w("-- C5. School 3 teachers (app_users) + faculty directory rows")
w("-- -----------------------------------------------------------------------------")
for t in S3_TEACHERS:
    tid = uid(f"user:{t['key']}")
    th = pwhash(t["pw"], t["salt"])
    w("INSERT INTO app_users (id, school_id, role, full_name, phone, email, password_hash, "
      "language_pref, is_phone_verified, is_email_verified, profile_completed, is_active, "
      "created_at, updated_at)")
    w(f"VALUES ({q(tid)}, {q(sid3)}, 'teacher', {q(t['full_name'])}, {q(t['phone'])}, "
      f"{q(t['email'])}, {q(th)}, 'en', TRUE, TRUE, TRUE, TRUE, '{TS}', '{TS}')")
    w("ON CONFLICT (id) DO NOTHING;")
    fid = uid(f"faculty:{t['key']}")
    w("INSERT INTO faculty (id, school_id, external_id, user_id, name, department, is_active, created_at)")
    w(f"VALUES ({q(fid)}, {q(sid3)}, {q('VP_' + t['key'].upper())}, {q(tid)}, {q(t['full_name'])}, "
      f"'Academics', TRUE, '{TS}')")
    w("ON CONFLICT (id) DO NOTHING;")
    w("")

# ---- C6. School 3 students --------------------------------------------------
w("-- -----------------------------------------------------------------------------")
w("-- C6. School 3 students (canonical roster)")
w("-- -----------------------------------------------------------------------------")
for klass, section, roll, name in S3_STUDENTS:
    code = student_code("s3", klass, section, roll)
    stu_id = uid(f"student:{code}")
    w("INSERT INTO students (id, school_id, student_code, full_name, class_name, section, "
      "roll_number, is_active, created_at)")
    w(f"VALUES ({q(stu_id)}, {q(sid3)}, {q(code)}, {q(name)}, {q(klass)}, {q(section)}, "
      f"{q(str(roll))}, TRUE, '{TS}')")
    w("ON CONFLICT (student_code) DO NOTHING;")
w("")

# ---- C7. School 3 teacher->subject assignments ------------------------------
w("-- -----------------------------------------------------------------------------")
w("-- C7. School 3 teacher -> class/subject assignments")
w("-- -----------------------------------------------------------------------------")
S3_TNAME = {t["key"]: t["full_name"] for t in S3_TEACHERS}
for tk, klass, section, sub in S3_ASSIGNMENTS:
    code = class_code(klass)
    cid = uid(f"class:s3:{code}")
    sub_id = uid(f"subject:s3:{code}:{sub}")
    tid = uid(f"user:{tk}")
    aid_ = uid(f"assign:{tk}:{code}:{section}:{sub}")
    w("INSERT INTO teacher_subject_assignments (id, school_id, class_id, class_name, section, "
      "subject_id, subject, teacher_id, teacher_name, is_active, created_at, updated_at)")
    w(f"VALUES ({q(aid_)}, {q(sid3)}, {q(cid)}, {q(klass)}, {q(section)}, {q(sub_id)}, {q(sub)}, "
      f"{q(tid)}, {q(S3_TNAME[tk])}, TRUE, '{TS}', '{TS}')")
    w("ON CONFLICT (id) DO NOTHING;")
w("")

# ---- B1. Schools 1&2 extra teachers ----------------------------------------
w("-- -----------------------------------------------------------------------------")
w("-- B1. Schools 1 & 2 — extra teachers (+faculty)")
w("-- -----------------------------------------------------------------------------")
w("-- Guard — only proceed if the prerequisite schools exist.")
w("DO $$")
w("BEGIN")
w(f"  IF NOT EXISTS (SELECT 1 FROM public.schools WHERE id = {q(SCHOOL_KEYS['s1'])}) THEN")
w("    RAISE NOTICE 'school s1 missing — skipping s1/s2 expansion (run seed-2026-06-07.sql for those).';")
w("  END IF;")
w("END $$;")
w("")
for t in B_TEACHERS:
    tid = uid(f"user:{t['key']}")
    sid = SCHOOL_KEYS[t["school"]]
    th = pwhash(t["pw"], t["salt"])
    w("INSERT INTO app_users (id, school_id, role, full_name, phone, email, password_hash, "
      "language_pref, is_phone_verified, is_email_verified, profile_completed, is_active, "
      "created_at, updated_at)")
    w(f"SELECT {q(tid)}, {q(sid)}, 'teacher', {q(t['full_name'])}, {q(t['phone'])}, {q(t['email'])}, "
      f"{q(th)}, 'en', TRUE, TRUE, TRUE, TRUE, '{TS}', '{TS}'")
    w(f"WHERE EXISTS (SELECT 1 FROM public.schools WHERE id = {q(sid)})")
    w("ON CONFLICT (id) DO NOTHING;")
    fid = uid(f"faculty:{t['key']}")
    w("INSERT INTO faculty (id, school_id, external_id, user_id, name, department, is_active, created_at)")
    w(f"SELECT {q(fid)}, {q(sid)}, {q('VP_' + t['key'].upper())}, {q(tid)}, {q(t['full_name'])}, "
      f"'Academics', TRUE, '{TS}'")
    w(f"WHERE EXISTS (SELECT 1 FROM public.app_users WHERE id = {q(tid)})")
    w("ON CONFLICT (id) DO NOTHING;")
    w("")

# ---- B2. Schools 1&2 extra subjects ----------------------------------------
w("-- -----------------------------------------------------------------------------")
w("-- B2. Schools 1 & 2 — extra subjects (attached to existing classes)")
w("-- -----------------------------------------------------------------------------")
for sk, klass, sub in B_SUBJECTS:
    code = class_code(klass)
    cid = uid(f"class:{sk}:{code}")
    sub_id = uid(f"subject:{sk}:{code}:{sub}")
    w("INSERT INTO school_subjects (id, class_id, sub_name, sub_code, teacher_assigned, created_at)")
    w(f"SELECT {q(sub_id)}, {q(cid)}, {q(sub)}, {q(sub_code(sub))}, NULL, '{TS}'")
    w(f"WHERE EXISTS (SELECT 1 FROM public.school_classes WHERE id = {q(cid)})")
    w("ON CONFLICT (id) DO NOTHING;")
w("")

# ---- B3. Schools 1&2 extra students ----------------------------------------
w("-- -----------------------------------------------------------------------------")
w("-- B3. Schools 1 & 2 — extra students")
w("-- -----------------------------------------------------------------------------")
for sk, klass, section, roll, name in B_STUDENTS:
    sid = SCHOOL_KEYS[sk]
    code = student_code(sk, klass, section, roll)
    stu_id = uid(f"student:{code}")
    w("INSERT INTO students (id, school_id, student_code, full_name, class_name, section, "
      "roll_number, is_active, created_at)")
    w(f"SELECT {q(stu_id)}, {q(sid)}, {q(code)}, {q(name)}, {q(klass)}, {q(section)}, "
      f"{q(str(roll))}, TRUE, '{TS}'")
    w(f"WHERE EXISTS (SELECT 1 FROM public.schools WHERE id = {q(sid)})")
    w("ON CONFLICT (student_code) DO NOTHING;")
w("")

# ---- B4. Schools 1&2 extra assignments -------------------------------------
w("-- -----------------------------------------------------------------------------")
w("-- B4. Schools 1 & 2 — extra teacher -> subject assignments")
w("-- -----------------------------------------------------------------------------")
B_TNAME = {t["key"]: t["full_name"] for t in B_TEACHERS}
for tk, sk, klass, section, sub in B_ASSIGNMENTS:
    sid = SCHOOL_KEYS[sk]
    code = class_code(klass)
    cid = uid(f"class:{sk}:{code}")
    sub_id = uid(f"subject:{sk}:{code}:{sub}")
    tid = uid(f"user:{tk}")
    aid_ = uid(f"assign:{tk}:{code}:{section}:{sub}")
    w("INSERT INTO teacher_subject_assignments (id, school_id, class_id, class_name, section, "
      "subject_id, subject, teacher_id, teacher_name, is_active, created_at, updated_at)")
    w(f"SELECT {q(aid_)}, {q(sid)}, {q(cid)}, {q(klass)}, {q(section)}, {q(sub_id)}, {q(sub)}, "
      f"{q(tid)}, {q(B_TNAME[tk])}, TRUE, '{TS}', '{TS}'")
    w(f"WHERE EXISTS (SELECT 1 FROM public.app_users WHERE id = {q(tid)})")
    w("ON CONFLICT (id) DO NOTHING;")
w("")

# ---- Parents (recovery + B + S3) -------------------------------------------
w("-- -----------------------------------------------------------------------------")
w("-- D. Parent users (role=parent, is_phone_verified=true, school_id=NULL)")
w("--    D1 recovers the 11 ORIGINAL parents; D2 adds the s1/s2 expansion parents;")
w("--    D3 adds the 12 School-3 parents.")
w("-- -----------------------------------------------------------------------------")

def emit_parent(p):
    pid = uid(f"user:{p['key']}")
    w("INSERT INTO app_users (id, school_id, role, full_name, phone, email, password_hash, "
      "language_pref, is_phone_verified, is_email_verified, profile_completed, is_active, "
      "created_at, updated_at)")
    w(f"VALUES ({q(pid)}, NULL, 'parent', {q(p['full_name'])}, {q(p['phone'])}, NULL, NULL, "
      f"'en', TRUE, FALSE, TRUE, TRUE, '{TS}', '{TS}')")
    w("ON CONFLICT (id) DO NOTHING;")

w("-- D1 — ORIGINAL parents (recovery)")
for p in ORIGINAL_PARENTS:
    emit_parent(p); w("")
w("-- D2 — schools 1 & 2 expansion parents")
for p in B_PARENTS:
    emit_parent(p); w("")
w("-- D3 — School 3 parents")
for p in S3_PARENTS:
    emit_parent(p); w("")

# ---- Children links --------------------------------------------------------
w("-- -----------------------------------------------------------------------------")
w("-- E. Parent -> child links (children.school_id is the parent-side tenancy key)")
w("-- -----------------------------------------------------------------------------")

def emit_children(p, sk):
    pid = uid(f"user:{p['key']}")
    sid = SCHOOL_KEYS[sk]
    for klass, section, roll, name in p["children"]:
        code = student_code(sk, klass, section, roll)
        cid = uid(f"child:{p['key']}:{code}")
        w("INSERT INTO children (id, parent_id, school_id, student_code, child_name, "
          "date_of_birth, gender, current_grade, interests, overall_progress, current_level, "
          "attendance_status, is_active, created_at, updated_at)")
        w(f"VALUES ({q(cid)}, {q(pid)}, {q(sid)}, {q(code)}, {q(name)}, '2015-04-12', 'OTHER', "
          f"{q(klass)}, '[\"reading\",\"sports\"]', 0.72, 4, 'PRESENT', TRUE, '{TS}', '{TS}')")
        w("ON CONFLICT (id) DO NOTHING;")
    w("")

for p in ORIGINAL_PARENTS:
    emit_children(p, p["school"])
for p in B_PARENTS:
    emit_children(p, p["school"])
for p in S3_PARENTS:
    emit_children(p, "s3")

# ---- Verification ----------------------------------------------------------
w("COMMIT;")
w("")
w("-- -----------------------------------------------------------------------------")
w("-- F. Verification (run AFTER commit). Expected on top of a clean original seed:")
w("--      schools          = 3")
w("--      parents          = 29  (11 original + 6 s1/s2-new + 12 s3)")
w("--      teachers         = 21  (7 original + 4 s1/s2-new + 10 s3)")
w("--      children         = 33  (15 original + 6 s1/s2-new + 14 s3-child rows)")
w("--    (>= if you seeded extra elsewhere; never less.)")
w("-- -----------------------------------------------------------------------------")
w("SELECT 'schools'  AS entity, count(*) AS n FROM public.schools")
w("UNION ALL SELECT 'parents',  count(*) FROM public.app_users WHERE role='parent'")
w("UNION ALL SELECT 'teachers', count(*) FROM public.app_users WHERE role='teacher'")
w("UNION ALL SELECT 'admins',   count(*) FROM public.app_users WHERE role='school_admin'")
w("UNION ALL SELECT 'students', count(*) FROM public.students")
w("UNION ALL SELECT 'children', count(*) FROM public.children")
w("UNION ALL SELECT 'subjects', count(*) FROM public.school_subjects;")
w("")
w("-- Tenancy invariant: every parent must have school_id NULL (OTP-signup state).")
w("SELECT 'BAD_parent_tenancy' AS check, count(*) AS n")
w("FROM public.app_users WHERE role='parent' AND school_id IS NOT NULL;")
w("")

print("\n".join(out))
