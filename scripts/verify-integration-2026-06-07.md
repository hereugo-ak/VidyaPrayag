# Integration Verification Matrix — 2026-06-07

> Manual end-to-end checklist for confirming every feature works against the
> seeded database (`scripts/seed-2026-06-07.sql` on top of
> `scripts/schema-patch-2026-06-07.sql`). This is **not** an automated test
> suite — it is a structured walk every reviewer can run with `curl`/HTTPie/
> Postman/the KMP client against a local or staging deployment.
>
> Credentials and student-codes referenced here live in
> `seed-credentials-2026-06-07.md`.
>
> Every test follows the same five-line schema:
>
> ```
> ### [TEST-ID] — [Feature]
> **Actor:** role (School / credential ref)
> **Action:** the exact API call (or UI step) to perform
> **Expected:** what a passing response looks like
> **Data isolation check:** what the OTHER school's data must NOT do
> **Pass / Fail:** [ ]
> ```
>
> "Data isolation check" is mandatory for every endpoint that returns
> tenant-scoped data — the whole point of the dual-school seed.

---

## Pre-flight

```
PRE-01  [ ]  Apply scripts/schema-patch-2026-06-07.sql        — `psql -f` should print PATCH-001/002 SELECTs all == 'exists' / 'present'.
PRE-02  [ ]  Apply scripts/seed-2026-06-07.sql                 — final verification block reports the expected counts AND every "bad" column is 0.
PRE-03  [ ]  Server boots — `validateSchema()` passes; no "missing required table" in stdout.
PRE-04  [ ]  `OTP_DEV_RETURN_CODE` is unset or true (so parent OTP flow returns `dev_code` for testing). For prod-style runs set false and use the SMS provider's sandbox.
```

Throughout, `$BASE` = the server's HTTPS base URL, and tokens captured from
`/login` or `/verify-otp` are passed as `Authorization: Bearer <token>`.

---

## A. School Admin — School 1

> Login as Anjali Verma — `admin@sunrise.edu.in` / `Sunrise@2026`.
> Capture the `access_token` into `$S1_ADMIN_T`.

### TEST-AUTH-A1 — Admin login
**Actor:** School 1 admin (`admin@sunrise.edu.in`)
**Action:** `POST $BASE/api/v1/auth/login` body `{"email":"admin@sunrise.edu.in","password":"Sunrise@2026"}`
**Expected:** `200 OK` with `access_token`, `refresh_token`, and `user.role == "school_admin"`. `user.school_id` matches School 1's id (`SELECT id FROM schools WHERE slug='sunrise-public-school'`).
**Data isolation check:** swapping the password to `Greenfield@2026` MUST fail with `401`.
**Pass / Fail:** [ ]

### TEST-DASH-A1 — School admin dashboard loads
**Actor:** School 1 admin (`$S1_ADMIN_T`)
**Action:** `GET $BASE/api/v1/school/dashboard`
**Expected:** `200`. The response carries `school.name == "Sunrise Public School"`, brand color `#1d4ed8`, and a non-zero `class_count` (= 3 from seed). No "ONBOARDING_INCOMPLETE" flag — the seeded school has `onboarded_at` set + `logo_url` + classes (so `OnboardingStatus.kt` returns `complete`).
**Data isolation check:** the body MUST NOT contain "Greenfield" anywhere.
**Pass / Fail:** [ ]

### TEST-CLASS-A1 — Class & subject management read
**Actor:** School 1 admin
**Action:** `GET $BASE/api/v1/school/classes`
**Expected:** `200`. Exactly 3 classes — `Grade 3` (sections A,B), `Grade 5`, `Grade 8`. Each class lists its subjects (English/Mathematics/etc.).
**Data isolation check:** none of the returned rows have `school_id` of School 2.
**Pass / Fail:** [ ]

### TEST-TEACH-A1 — Teacher directory read
**Actor:** School 1 admin
**Action:** `GET $BASE/api/v1/school/teachers` (or the equivalent route in `TeacherProvisioningRouting.kt`)
**Expected:** `200`. Four teachers: Meena Sharma, Rahul Mehra, Priya Nair, Arjun Khanna.
**Data isolation check:** Kavita Iyengar (School 2) MUST NOT appear.
**Pass / Fail:** [ ]

### TEST-ANN-A1 — Announcements list
**Actor:** School 1 admin
**Action:** `GET $BASE/api/v1/school/announcements`
**Expected:** `200`. Three rows — Summer Break, PTM, Class Picnic (CLASS-scoped to Grade 5-A). `event_id` values all start with `s1-`.
**Data isolation check:** no `s2-` event_id and no Greenfield content appears.
**Pass / Fail:** [ ]

### TEST-ANN-A2 — Create announcement (write path)
**Actor:** School 1 admin
**Action:** `POST $BASE/api/v1/school/announcements` body `{"type":"Events","title":"Surprise Holiday","description":"Tomorrow is a holiday","date":"2026-06-08","audience_type":"ALL_SCHOOL"}`
**Expected:** `201`. New row appears in `SELECT * FROM announcements WHERE school_id=<s1>`. `event_id` is server-generated.
**Data isolation check:** School 2 admin's `GET /api/v1/school/announcements` MUST NOT include this row.
**Pass / Fail:** [ ]

### TEST-RES-A1 — Results overview
**Actor:** School 1 admin
**Action:** `GET $BASE/api/v1/school/results`
**Expected:** `200`. Has at least one published test ("Unit Test I") across classes Grade 3/5/8, with real `student_name` and numeric `score` (55–95 range).
**Data isolation check:** no Greenfield student names (Reyansh, Saanvi, Udayan, …) appear.
**Pass / Fail:** [ ]

### TEST-FEE-A1 — Fees overview / outstanding
**Actor:** School 1 admin
**Action:** `GET $BASE/api/v1/school/fees` (or the equivalent admin fee endpoint)
**Expected:** `200`. Aggregate row count reflects the 5 School-1 parents × 1–2 children × 3 line-items = 21 fee rows (seed-side).
**Data isolation check:** none of the line items reference Greenfield parents or children.
**Pass / Fail:** [ ]

### TEST-SYL-A1 — Syllabus coverage
**Actor:** School 1 admin
**Action:** `GET $BASE/api/v1/school/syllabus` (or analytics endpoint that reads `syllabus_units`)
**Expected:** `200`. Each `(class, section, subject)` covered by a School-1 assignment shows 4 units; 2 are `is_covered=true` (Units 1 & 2), 2 not.
**Data isolation check:** none of the returned units come from a Grade 4/6/9 class (those are School 2).
**Pass / Fail:** [ ]

### TEST-ATT-A1 — Attendance overview
**Actor:** School 1 admin
**Action:** `GET $BASE/api/v1/school/attendance/daily?type=student&date=2026-06-06`
**Expected:** `200`. Roughly 92% PRESENT, with the remaining mix LATE + ABSENT (per the seed's deterministic 92/5/3 mix).
**Data isolation check:** `person_id` values are all `S1-*` student codes, never `S2-*`.
**Pass / Fail:** [ ]

### TEST-ADM-A1 — Admission CRM
**Actor:** School 1 admin
**Action:** `GET $BASE/api/v1/admissions/enquiries`
**Expected:** `200`. May be empty (seed does not insert `admission_enquiries` rows by design — admins create them through the UI). Endpoint must return `{"enquiries": []}` (or equivalent) with `200`, not 500.
**Data isolation check:** any creation through this endpoint, when read later from School 2, MUST NOT appear.
**Pass / Fail:** [ ]

---

## B. School Admin — School 2

> Login as Rakesh Iyer — `admin@greenfield.edu.in` / `Greenfield@2026`.
> Capture token into `$S2_ADMIN_T`. Repeat A1–A11 with these adjustments:

### TEST-DASH-B1 — School admin dashboard (School 2)
**Actor:** School 2 admin (`$S2_ADMIN_T`)
**Action:** `GET $BASE/api/v1/school/dashboard`
**Expected:** `200`. `school.name == "Greenfield Academy"`, brand color `#047857`, board ICSE.
**Data isolation check:** body MUST NOT contain "Sunrise" anywhere.
**Pass / Fail:** [ ]

### TEST-ANN-B1 — Announcements (School 2)
**Actor:** School 2 admin
**Action:** `GET $BASE/api/v1/school/announcements`
**Expected:** Three rows, all with `s2-` `event_id` prefix, including a Grade 6-A CLASS-scoped picnic.
**Data isolation check:** the row TEST-ANN-A2 created in School 1 does NOT appear here.
**Pass / Fail:** [ ]

### TEST-TEACH-B1 — Teacher directory (School 2)
**Actor:** School 2 admin
**Action:** `GET $BASE/api/v1/school/teachers`
**Expected:** Three teachers — Kavita Iyengar, Vivek Banerjee, Neha Goswami.
**Data isolation check:** Meena / Rahul / Priya / Arjun MUST NOT appear.
**Pass / Fail:** [ ]

---

## C. Teacher — School 1

> Login as Meena Sharma — `meena.sharma@sunrise.edu.in` / `Teacher@2026`.
> Capture token into `$S1_T1_T`. Meena's seeded assignment is Grade 3-A
> English + EVS.

### TEST-AUTH-C1 — Teacher login
**Actor:** Meena Sharma
**Action:** `POST $BASE/api/v1/auth/login` with her email + `Teacher@2026`.
**Expected:** `200`, `user.role == "teacher"`, `user.school_id == School 1`.
**Data isolation check:** `Teacher@2026` MUST NOT log in against a School 2 email.
**Pass / Fail:** [ ]

### TEST-DASH-C1 — Teacher home (non-empty)
**Actor:** Meena Sharma
**Action:** `GET $BASE/api/v1/teacher/home`
**Expected:** `200`. `classes_count >= 1` (she has 1 unique class — Grade 3-A — across 2 subjects); a non-empty homework strip (seed inserts 2 HW rows for her), greeting computed from server time.
**Data isolation check:** none of the listed classes are Grade 4/6/9 (School 2 classes).
**Pass / Fail:** [ ]

### TEST-CLASS-C1 — Class roster
**Actor:** Meena Sharma
**Action:** `GET $BASE/api/v1/teacher/classes`
**Expected:** `200`. Returns Grade 3-A with subjects English, EVS. Roster: Aarav Singh, Bhavya Tiwari, Chirag Yadav.
**Data isolation check:** Diya Mishra (Grade 3-B) NOT in the section-A roster; no School 2 student appears.
**Pass / Fail:** [ ]

### TEST-ATT-C1 — Mark attendance
**Actor:** Meena Sharma
**Action:** `POST $BASE/api/v1/teacher/attendance` body `{"class_id":"<grade3>", "section":"A", "date":"2026-06-07", "entries":[{"student_id":"S1-G3A-001","status":"PRESENT"},{"student_id":"S1-G3A-002","status":"LATE"}]}`
**Expected:** `200`/`204`. `SELECT … FROM attendance_records WHERE date='2026-06-07' AND school_id=<s1>` reflects the upsert (existing seeded rows for 2026-06-07 are updated, not duplicated — the unique index `(school_id,date,type,person_id)` ensures it).
**Data isolation check:** submitting `student_id="S2-G4A-001"` (a real School-2 student code) currently writes (RE-AUDIT RA-07/RA-19) — record observed behaviour. The CORRECT behaviour is a 400. Mark Fail if it returns 200.
**Pass / Fail:** [ ]

### TEST-MARKS-C1 — Submit marks
**Actor:** Meena Sharma
**Action:** `POST $BASE/api/v1/teacher/marks` against her English assessment for Grade 3-A.
**Expected:** Marks upserted into `assessment_marks`; reading back via `GET /api/v1/teacher/marks` returns the new values.
**Data isolation check:** submitting marks for `S2-G4A-001` is the same audit hazard as TEST-ATT-C1 — record observed.
**Pass / Fail:** [ ]

### TEST-HW-C1 — Create homework
**Actor:** Meena Sharma
**Action:** `POST $BASE/api/v1/teacher/homework` for Grade 3-A English with a 3-day due date.
**Expected:** `201`. `GET /api/v1/teacher/homework` includes the new row.
**Data isolation check:** School 2 teachers do NOT see this homework on their `/homework` reads.
**Pass / Fail:** [ ]

### TEST-SYL-C1 — Mark syllabus unit covered
**Actor:** Meena Sharma
**Action:** `PATCH $BASE/api/v1/teacher/syllabus/{unit_id}` flipping `is_covered=true` on Unit 3 of Grade 3-A English.
**Expected:** `200`. `covered_on` set to today's date; `covered_by` = her user id.
**Data isolation check:** she cannot flip a Grade 4-A (School 2) unit — server should 403/404. Record observed.
**Pass / Fail:** [ ]

---

## D. Teacher — School 2

### TEST-DASH-D1 — Vivek Banerjee dashboard
**Actor:** Vivek Banerjee — `vivek.banerjee@greenfield.edu.in` / `Teacher@2026`
**Action:** Login → `GET /api/v1/teacher/home`
**Expected:** 1 class (Grade 6-A Science), 1 homework row pre-seeded.
**Data isolation check:** Meena's Grade 3-A English homework MUST NOT show up here.
**Pass / Fail:** [ ]

### TEST-CLASS-D1 — Roster scoping
**Actor:** Vivek Banerjee
**Action:** `GET /api/v1/teacher/classes`
**Expected:** Grade 6-A with students Udayan Das, Vihaan Sen, Wamika Dutta.
**Data isolation check:** Xen Chowdhury (Grade 6-B) is in a different section — NOT in Vivek's roster (his assignment is section A only).
**Pass / Fail:** [ ]

---

## E. Parent — School 1

> Login as Vikram Kapoor — phone `+919811100003`, OTP-only.
> Get the dev code: `POST /api/v1/auth/send-otp {"identifier":"+919811100003","purpose":"login"}` → read `dev_code`.
> Then `POST /api/v1/auth/verify-otp {"identifier":"+919811100003","code":"<dev_code>","purpose":"login"}` → capture `$S1_PARENT_T`.
> He has TWO children: Ishan Kapoor (Grade 5-A) and Pari Agarwal (Grade 8-A).

### TEST-OTP-E1 — Parent OTP flow
**Actor:** Vikram Kapoor's device
**Action:** send-otp → verify-otp (above).
**Expected:** Both return `200`. `verify-otp` returns access+refresh tokens. User shape has `role="parent"`, `school_id == null` (parent tenancy is via children).
**Data isolation check:** `school_id` MUST be NULL — non-null indicates the OTP-signup write path drifted.
**Pass / Fail:** [ ]

### TEST-DASH-E1 — Parent dashboard non-empty
**Actor:** Vikram Kapoor (`$S1_PARENT_T`)
**Action:** `GET $BASE/api/v1/parent/dashboard`
**Expected:** `200`. Two children. Each child block shows current grade, attendance status PRESENT, and at least one fee summary. Greeting computed by server clock (note RE-AUDIT RA-14: server TZ leak — may say "Good Morning" when it's evening IST).
**Data isolation check:** no Greenfield branding/announcements appear. `featured_schools` may show School 2 (discovery is intentionally global — see RA-12 / cross-school discovery is acceptable design).
**Pass / Fail:** [ ]

### TEST-ACAD-E1 — Per-child academic summary
**Actor:** Vikram Kapoor
**Action:** `GET $BASE/api/v1/parent/track-progress?child_id=<ishan>`
**Expected:** `200`. Returns a competency template (RE-AUDIT RA-09 — template is identical regardless of child). Hero band shows Ishan's `overall_progress` and `current_level` from the seed.
**Data isolation check:** Asking for `child_id=<random uuid>` MUST 404; asking for a child belonging to a School 2 parent MUST 404 (the route scopes on parent_id).
**Pass / Fail:** [ ]

### TEST-FEE-E1 — Parent fee status
**Actor:** Vikram Kapoor
**Action:** `GET $BASE/api/v1/parent/fees`
**Expected:** `200`. Six rows total (3 line items × 2 children). Stats reflect: 1 PAID + 1 DUE + 1 OVERDUE per child.
**Data isolation check:** no fee row belongs to a child of another parent.
**Pass / Fail:** [ ]

### TEST-ATT-E1 — Attendance view
**Actor:** Vikram Kapoor
**Action:** `GET $BASE/api/v1/parent/attendance?child_id=<ishan>&from=2026-05-08&to=2026-06-07`
**Expected:** `200`. About 30 calendar days of attendance, ~92% PRESENT.
**Data isolation check:** zero entries for any student code other than Ishan's.
**Pass / Fail:** [ ]

### TEST-ANN-E1 — Announcements feed
**Actor:** Vikram Kapoor
**Action:** `GET $BASE/api/v1/parent/announcements`
**Expected:** `200`. School-1 ALL_SCHOOL + the CLASS-scoped Grade 5-A picnic (Ishan's class) — the Grade 6-A picnic from School 2 MUST NOT appear.
**Data isolation check:** zero `s2-` event_id rows.
**Pass / Fail:** [ ]

### TEST-NOTIF-E1 — Notifications cross-school
**Actor:** Vikram Kapoor
**Action:** `GET $BASE/api/v1/parent/notifications`
**Expected:** `200`. Notifications derive from `announcements` joined through `children.school_id`. All entries are for School 1.
**Data isolation check:** zero notifications referencing announcement ids that begin with `s2-`.
**Pass / Fail:** [ ]

### TEST-SCH-E1 — Scholarships catalogue
**Actor:** Vikram Kapoor
**Action:** `GET $BASE/api/v1/parent/scholarships`
**Expected:** `200`. Three catalogue rows (STEM, Sports, Need-based). Applications block shows at most his own application row (if among the first 4 seeded parents).
**Data isolation check:** he MUST NOT see another parent's application.
**Pass / Fail:** [ ]

---

## F. Parent — School 2

### TEST-DASH-F1 — Parent dashboard (School 2)
**Actor:** Tanvi Mukherjee — phone `+919822200006`, OTP-only. She has TWO children: Zara Mukherjee + Aadya Ghosh.
**Action:** OTP flow → `GET /api/v1/parent/dashboard`
**Expected:** Two children, both Grade 9-A; brand/colors from Greenfield.
**Data isolation check:** zero School-1 announcements; her child cards do NOT show Ishan/Pari.
**Pass / Fail:** [ ]

---

## G. Cross-school isolation — direct attempts

### TEST-ISO-01 — Admin token cannot read other school's endpoints
**Actor:** School 1 admin (`$S1_ADMIN_T`)
**Action:** `GET $BASE/api/v1/school/dashboard` (no school_id is ever sent — guard resolves it from the JWT/db)
**Expected:** Always returns School 1's data. There is no path to pass an explicit school_id query param; even crafted ones must be ignored.
**Data isolation check:** body MUST NOT contain `"Greenfield"` or any `s2-` event_id.
**Pass / Fail:** [ ]

### TEST-ISO-02 — Parent reading another parent's children
**Actor:** Vikram Kapoor (School 1 parent)
**Action:** `GET $BASE/api/v1/parent/children/<some-other-parent-child-id>`
**Expected:** 404 (the parent routes scope by `children.parent_id`). 200 with another parent's data = fail.
**Data isolation check:** none of his children's UUIDs from School 1 are equal to any other parent's. Validate by inspecting `children.parent_id` in DB.
**Pass / Fail:** [ ]

### TEST-ISO-03 — Teacher querying another school's class
**Actor:** Meena Sharma (`$S1_T1_T`)
**Action:** `GET $BASE/api/v1/teacher/marks?class_name=Grade%206&section=A&subject=Science` (a Greenfield Grade-6-A subject)
**Expected:** 403/404 — `requireOwnedAssignment` fails because no `teacher_subject_assignments` row exists for her against that tuple.
**Data isolation check:** the response MUST NOT include Udayan/Vihaan/Wamika's names.
**Pass / Fail:** [ ]

### TEST-ISO-04 — Cross-school parent link-child (IDOR check — RE-AUDIT RA-03)
**Actor:** Vikram Kapoor (School 1 parent)
**Action:** `POST $BASE/api/v1/parent/link-child` body `{"school_id":"<school-2-uuid>", "roll_number":"1", "class_name":"Grade 4"}` (Reyansh Kumar's slot)
**Expected (current/insecure):** server LINKS him to Reyansh — confirms RA-03. The CORRECT behaviour is 403. Mark Pass for "audit confirmed" or Fail for "audit issue still present and unmitigated"; in either case record exact response.
**Data isolation check:** after the call, `SELECT * FROM children WHERE parent_id=<vikram> AND school_id=<school-2>` returns 0 rows in a fixed system, 1 row today.
**Pass / Fail:** [ ]

### TEST-ISO-05 — Cross-school announcement read
**Actor:** Tanvi Mukherjee (School 2 parent token)
**Action:** `GET $BASE/api/v1/parent/announcements`
**Expected:** Only School 2 events (3 seeded + any TEST-ANN-A2 creation does NOT appear).
**Data isolation check:** every returned `event_id` starts with `s2-`.
**Pass / Fail:** [ ]

---

## H. Notification propagation

### TEST-NOTIF-01 — Admin announcement reaches teachers + parents in same school only
**Actor:** School 1 admin (`$S1_ADMIN_T`)
**Action:**
1. `POST /api/v1/school/announcements` body `{"type":"Events","title":"Pop Quiz","description":"For all teachers","date":"2026-06-08","audience_type":"ALL_SCHOOL"}`.
2. As School 1 teacher (`$S1_T1_T`), `GET /api/v1/teacher/notifications` (or `/teacher/announcements`).
3. As School 1 parent (`$S1_PARENT_T`), `GET /api/v1/parent/notifications`.
4. As School 2 admin / teacher / parent, repeat steps 2–3.
**Expected:** Steps 2 + 3 see the new announcement; step 4 sees nothing related to it.
**Data isolation check:** no School 2 actor surfaces the new announcement.
**Pass / Fail:** [ ]

### TEST-NOTIF-02 — Class-scoped announcement targets only that class
**Actor:** School 1 admin
**Action:** Create an announcement with `audience_type=CLASS`, `audience_filter={"class_name":"Grade 8","section":"A"}`.
**Expected:** Parents whose children are in Grade 8-A (Pankaj Joshi, Vikram Kapoor via Pari) see it; Sunita Mishra (Grade 3-B child) does NOT.
**Data isolation check:** no Greenfield parent sees it. (Server expansion via `children.school_id` — TEST-ANN-* row of audit RA-04 still applies.)
**Pass / Fail:** [ ]

### TEST-NOTIF-03 — WhatsApp ALL_SCHOOL fan-out (RE-AUDIT RA-04 confirmation)
**Actor:** School 1 admin
**Action:** `POST /api/v1/school/announcements/sync-whatsapp` for an ALL_SCHOOL announcement.
**Expected (current):** `resolveRecipientPhones` returns 0 because the expansion still filters on `app_users.school_id` for parents, which is NULL for all OTP-signed-up parents (RE-AUDIT RA-04). Record observed count.
**Data isolation check:** even when fixed, the expansion MUST resolve only School-1 phones.
**Pass / Fail:** [ ]

---

## I. New school onboarding (third fresh admin)

> Use a third, fresh admin account NOT in the seed: e.g. signup
> `freshadmin+2026@example.com` via the onboarding entry point.

### TEST-ONBOARD-01 — Full onboarding flow
**Actor:** Brand-new admin
**Action:** Sign up, walk every onboarding step (BASIC → BRANDING → ACADEMIC → REVIEW → Finalise), end on the school dashboard.
**Expected:** `schools.onboarded_at` is set, ≥1 row in `school_classes`, a `school_philosophy` row exists, dashboard returns `school.name` matching what was entered (not the seed names).
**Data isolation check:** the new school MUST NOT see Sunrise or Greenfield data on any endpoint.
**Pass / Fail:** [ ]

### TEST-ONBOARD-02 — Re-login lands on dashboard (no onboarding loop)
**Actor:** Same brand-new admin
**Action:** Log out, log back in.
**Expected:** Lands on dashboard directly, NOT on onboarding (per `OnboardingStatus.kt` rules — name + contact + logo + ≥1 class + onboarded_at).
**Pass / Fail:** [ ]

---

## J. New parent signup (live OTP path)

### TEST-SIGNUP-01 — New parent end-to-end
**Actor:** Brand-new phone, e.g. `+919900000001`.
**Action:**
1. `POST /api/v1/auth/send-otp {"identifier":"+919900000001","purpose":"signup"}` — read `dev_code`.
2. `POST /api/v1/auth/verify-otp {...,"purpose":"signup"}`.
3. `POST /api/v1/auth/signup` body `{"phone":"+919900000001","full_name":"Test Parent","language_pref":"en"}` (or the existing signup endpoint).
4. `POST /api/v1/parent/link-child` with `(school_id=<School 1>, class_name="Grade 5", roll_number="3")` → Karan Trivedi.
5. `GET /api/v1/parent/dashboard`.
**Expected:** New `app_users` row (role=parent, `school_id=NULL`, `is_phone_verified=true`); new `children` row; dashboard now non-empty.
**Data isolation check:** Karan must show only on the new parent's dashboard; pre-seeded Reena Saxena (Jiya Saxena's mum) MUST NOT see Karan.
**Pass / Fail:** [ ]

### TEST-SIGNUP-02 — Search nearby school (geo discovery)
**Actor:** New parent token
**Action:** `GET /api/v1/parent/schools/discover?lat=26.84&lng=80.94` (Lucknow coordinates).
**Expected:** Sunrise Public School appears at the top by distance; Greenfield (Kanpur, ~80 km away) appears further down.
**Data isolation check:** both schools appear (discovery is intentionally global), but neither leaks anything beyond name/city/logo/lat/lng + brand color.
**Pass / Fail:** [ ]

### TEST-SIGNUP-03 — OTP replay (RE-AUDIT RA-06 confirmation)
**Actor:** Brand-new phone
**Action:** Run send-otp twice rapidly; in a third request use the FIRST code.
**Expected:** Per `OtpService`, the second `send-otp` resends the same code within the resend window (or generates a new one — record behaviour); the first code remains valid for verify until expiry. Document what happens if verify-otp succeeds but the subsequent `signup` write crashes (RE-AUDIT RA-06 says the OTP is consumed yet the user is not created).
**Pass / Fail:** [ ]

---

## K. School-admin teacher provisioning

### TEST-PROV-01 — Admin creates a new teacher
**Actor:** School 1 admin (`$S1_ADMIN_T`)
**Action:** `POST $BASE/api/v1/school/teachers` body `{"full_name":"Test Teacher","email":"test.teacher@sunrise.edu.in","phone":"+919876509999","password":"Teacher@Init"}`.
**Expected:** `201`, new `app_users` row with `role="teacher"`, `school_id=<School 1>`, `is_active=true`. The credentials returned/printed (verify the exact contract) can be used immediately.
**Data isolation check:** the new teacher's `school_id` is School 1; Greenfield admin's `/school/teachers` MUST NOT see this user.
**Pass / Fail:** [ ]

### TEST-PROV-02 — New teacher logs in to an EMPTY dashboard (RE-AUDIT RA-08 confirmation)
**Actor:** The brand-new teacher
**Action:** Log in with the new credentials; `GET /api/v1/teacher/home` and `GET /api/v1/teacher/classes`.
**Expected (current):** `classes_count = 0` because provisioning does not create a `teacher_subject_assignments` row (RA-08). The admin must call the assignment endpoint to fix it — see TEST-PROV-03.
**Data isolation check:** the new teacher sees only School 1's class list (when an assignment is added) and NEVER any School 2 class.
**Pass / Fail:** [ ]

### TEST-PROV-03 — Admin adds an assignment, new teacher sees the class
**Actor:** School 1 admin
**Action:** `POST $BASE/api/v1/school/teacher-assignments` with the new teacher's id + Grade 5-A + Science (or any unique tuple). Then re-run TEST-PROV-02 as the teacher.
**Expected:** `/teacher/classes` now returns Grade 5-A Science.
**Data isolation check:** still no School-2 class on her side.
**Pass / Fail:** [ ]

---

## L. Token-lifecycle edge cases

### TEST-EDGE-01 — Expired access token
**Actor:** Any account
**Action:** Wait until access token expires (1 day per `JwtConfig.kt`), then call any protected endpoint. Then call `/api/v1/auth/refresh` with the refresh token.
**Expected:** First call returns `401`; refresh returns a new access token; retry succeeds.
**Pass / Fail:** [ ]

### TEST-EDGE-02 — Deactivated teacher (RE-AUDIT RA-02 confirmation)
**Actor:** A teacher with a live JWT
**Action:** Through DB, `UPDATE app_users SET is_active=false WHERE id=<teacher>`. Without re-issuing tokens, the teacher continues to call `/api/v1/teacher/home`.
**Expected (current):** Endpoint still returns `200` — `is_active` is not enforced in any guard (RA-02). Mark Fail for "audit confirmed defect."
**Pass / Fail:** [ ]

### TEST-EDGE-03 — Offboarded school
**Actor:** Any user of School 1
**Action:** `UPDATE schools SET is_active=false WHERE id=<s1>`. Calls continue with existing tokens.
**Expected (current):** Endpoints continue to serve until token expiry (RA-02). Discovery may also still surface the school (depends on `featured_schools` query).
**Pass / Fail:** [ ]

---

## M. Idempotency / determinism

### TEST-IDEMP-01 — Re-run the seed
**Actor:** DBA
**Action:** Run `scripts/seed-2026-06-07.sql` a second time.
**Expected:** Zero errors. Row counts identical to the first run. No duplicate rows. The final verification block returns the same numbers.
**Pass / Fail:** [ ]

### TEST-IDEMP-02 — Re-run the schema patch
**Actor:** DBA
**Action:** Run `scripts/schema-patch-2026-06-07.sql` again.
**Expected:** Zero errors. Verification reports tables `exists`, FK `present`.
**Pass / Fail:** [ ]

---

## Summary

| Section | Tests | Pass | Fail | Notes                                                                                  |
|---------|-------|------|------|----------------------------------------------------------------------------------------|
| A — School 1 admin       | 11 |   |   |                                                                                       |
| B — School 2 admin       | 3  |   |   | repeats subset of A with School-2 expectations                                         |
| C — Teacher (S1)         | 8  |   |   | TEST-ATT-C1 / MARKS-C1 expose RA-07 if `student_id` is unvalidated                     |
| D — Teacher (S2)         | 2  |   |   |                                                                                       |
| E — Parent (S1)          | 8  |   |   |                                                                                       |
| F — Parent (S2)          | 1  |   |   |                                                                                       |
| G — Cross-school iso     | 5  |   |   | TEST-ISO-04 confirms RA-03 IDOR                                                        |
| H — Notification         | 3  |   |   | TEST-NOTIF-03 confirms RA-04 (zero ALL_SCHOOL WhatsApp recipients)                     |
| I — Onboarding           | 2  |   |   |                                                                                       |
| J — Parent signup        | 3  |   |   | TEST-SIGNUP-03 confirms RA-06 transaction gap                                          |
| K — Teacher provisioning | 3  |   |   | TEST-PROV-02 confirms RA-08 (empty dashboard until assignment added)                   |
| L — Token lifecycle      | 3  |   |   | TEST-EDGE-02/03 confirm RA-02 (no `is_active` enforcement)                             |
| M — Idempotency          | 2  |   |   |                                                                                       |
| **Total**                | **54** |   |   |                                                                                       |

*End of verify-integration-2026-06-07.md.*
