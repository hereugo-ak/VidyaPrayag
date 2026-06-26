# 06 — ATTENDANCE SYSTEM SPEC

> **Scope:** The correct, complete attendance system for the teacher portal: the teacher's **own check-in** (biometric in Compose + fallback), **student attendance** scoped to a class, date defaulting + correction, leave-derived defaults, bulk/override, and every edge case (transfer, cancelled class, already-marked, holiday).
> **Source:** `AttendanceRecordsTable`, `feature/teacher/TeacherRoutingTasks.kt` (`/attendance` GET+POST, `VALID_ATTENDANCE={present,absent,late}`), `TeacherAttendanceViewModel.kt`, `TeacherAttendanceScreenV2.kt`, `TeacherLeaveRouting.kt`. Defects: D-ATT-1..4, B-ATT-1..7, F-ATT-1..7, B-LV-2.
> **Lens:** Aanya (fast, low-anxiety, can't be allowed to mark the wrong class) and Mr. Rao (40 students × 6 classes, needs bulk + huge tap targets).
> **Law:** Attendance is the #1 daily action. It must be 1 tap to reach, scoped automatically, correct by default, and impossible to misfile.
> **Branch:** `backend-by-abuzar_v1.0.3`

---

## 0. Executive Summary

Attendance is the most-used and most-broken surface. Current failures:
1. **No teacher self check-in at all** — there is no faculty check-in endpoint, no biometric anywhere in the codebase (grep-verified), no teacher-writes-`type=faculty` path (B-ATT-5).
2. **Student attendance has only 3 states** — `present | absent | late` (`VALID_ATTENDANCE`), with **no `leave`** (D-ATT-1) — so an approved leave cannot be reflected, and approved leave does **not** auto-write attendance (B-LV-2).
3. **Dates are strings** (`varchar12`, D-ATT-3/X-3) and the date field on screen is **disabled** — no back-dating to correct a missed day (F-ATT-2).
4. **`personId` is free-text** (D-ATT-2) and `grade` is a packed `"<class>-<section>"` string parsed by `lastIndexOf('-')` (D-ATT-4) — fragile, no FK to students/enrollment.
5. **Buried 2–3 taps** behind the shared Update picker (F-ATT-1); resets scope every visit.
6. **No edge-case handling**: holiday, cancelled class, already-marked, transferred student, new student — all undefined.

This spec defines the corrected end state: a typed `attendance_records`, a `teacher_check_ins` table, a biometric check-in flow with graceful fallback, a 1-tap scoped student-attendance flow driven by Doc 05's resolved period, leave-derived defaults, and explicit handling for every edge case.

---

## 1. Data Model — Current vs Target

### 1.1 `AttendanceRecordsTable` ("attendance_records") — current
| Column | Type | Defect |
|--------|------|--------|
| schoolId | uuid | ok |
| date | varchar12 | **string date** (D-ATT-3 / X-3) — no range queries, no calendar math. |
| type | varchar16 (student\|faculty) | ok concept, but teacher never writes `faculty` (B-ATT-5). |
| personId | text | **free text, no FK** (D-ATT-2) — student or faculty id as string. |
| grade | text? "<class>-<section>" | **packed string** parsed by `lastIndexOf('-')` (D-ATT-4); breaks on "Grade 4-A". |
| status | varchar16 (present\|absent\|late) | **no `leave`** (D-ATT-1). |
| markedBy | uuid? | ok. |
| unique | (schoolId, date, type, personId) | reasonable, but personId untyped. |

### 1.2 Target — typed student attendance
```
attendance_records (revised)
  id          uuid pk
  school_id   uuid
  date        date NOT NULL                         -- TYPED (X-3)
  type        text  CHECK (type IN ('student','faculty'))
  student_id  uuid NULL fk -> students              -- typed FK when type='student' (D-ATT-2)
  enrollment_id uuid NULL fk -> enrollments         -- scopes to the exact class membership (X-1/D-STU)
  faculty_id  uuid NULL fk -> app_users             -- when type='faculty'
  assignment_id uuid NULL fk -> teacher_subject_assignments  -- which period/class this mark belongs to
  status      text CHECK (status IN ('present','absent','late','leave'))  -- +leave (D-ATT-1)
  source      text DEFAULT 'manual'                 -- manual | leave_auto | bulk | biometric
  marked_by   uuid fk -> app_users
  marked_at   timestamptz DEFAULT now()
  UNIQUE (school_id, date, type, student_id, assignment_id)   -- one mark per student per class per day
```
- `grade` string is **dropped**; class/section derive via `assignment_id` → TSA (kills D-ATT-4/X-1 for attendance).
- `leave` status added (D-ATT-1).
- `assignment_id` ties the record to the authorizing TSA (Doc 05 binding), so scope is provable.

### 1.3 Target — teacher self check-in
```
teacher_check_ins
  id            uuid pk
  school_id     uuid
  teacher_id    uuid fk -> app_users
  date          date NOT NULL
  checked_in_at timestamptz NOT NULL
  method        text CHECK (method IN ('biometric','pin','manual'))   -- fallback ladder
  device_id     text NULL
  UNIQUE (school_id, teacher_id, date)              -- one check-in per teacher per day
```
Faculty attendance can be **derived** from this (a check-in row ⇒ a `attendance_records(type=faculty,status=present)` for the day) or stored directly with `type=faculty`. Either way it fixes B-ATT-5.

---

## 2. Teacher Self Check-In (biometric + fallback)

**Context:** Doc 04 §5.1 — the morning check-in band on Today.

### 2.1 The biometric ladder (Compose Multiplatform)
There is **no biometric code today** (verified). Target a platform-abstracted prompt with a strict fallback ladder so it works for every teacher and never blocks check-in:

1. **Biometric (preferred):** Android `BiometricPrompt` / iOS `LAContext` behind an `expect/actual` `BiometricAuthenticator` in `shared` (or a composeApp platform module). On success → `method='biometric'`.
2. **Device PIN/passcode fallback:** if biometric hardware/enrollment is unavailable, allow device credential → `method='pin'`.
3. **Manual confirm fallback:** if neither is available (older device, denied), a clear **"Confirm check-in"** button → `method='manual'`. This guarantees Mr. Rao on an old phone is never locked out.

> Design rule: **biometric verifies it's really this teacher; it must never be a hard gate.** The fallback ladder always reaches a working manual confirm. (Accessibility — Doc 10.)

### 2.2 Flow
1. Today loads → `/teacher/day` (Doc 05) + check-in status (today's `teacher_check_ins`).
2. Card state: **Not checked in** (amber) → tap **Check in**.
3. Launch biometric prompt → success → optimistic flip to **Checked in HH:mm** (green); POST `/teacher/checkin {method}`.
4. On server confirm, persist; on failure, revert pill + toast.
5. Already checked in today → card shows "Checked in 08:42 ✓"; tapping shows a passive detail, no re-check.

### 2.3 Endpoint
```
POST /api/v1/teacher/checkin   body { method, deviceId? }
→ { checkedInAt, method }      (idempotent per (teacher, date))
GET  /api/v1/teacher/checkin?date=…   → status
```

### 2.4 Edge cases
| Case | Handling |
|------|----------|
| Biometric unavailable | drop to PIN, then manual confirm; never block. |
| Already checked in | idempotent; return existing row. |
| Holiday | check-in still allowed (teacher may work); not solicited aggressively. |
| Clock skew | server stamps `checked_in_at` (authoritative). |
| Offline | queue locally, sync on reconnect; pill shows "pending sync." |

---

## 3. Student Attendance — the core flow

### 3.1 Entry (1 tap, pre-scoped) — kills F-ATT-1
- **Primary entry:** Today → Now/Next period card → **Mark attendance** (class+section+subject+date carried via `assignmentId` from Doc 05). No picker.
- **Secondary entry:** Classes → class detail → **Mark attendance**.
- **Correction entry:** Today obligations strip ("3 unmarked") → list → tap class.

### 3.2 Scoped class selection
- The roster shown is **exactly** the students enrolled in the `assignmentId`'s class+section, resolved server-side via `enrollments` (not the in-memory `ClassNaming.sameClassSection` heuristic — that becomes a fallback only). This fixes the free-text drift (X-1) and the packed-grade parse (D-ATT-4).
- If entered via Classes (no period), scope = that class assignment; date defaults to today.

### 3.3 Date: default + correctable
- **Default = today** (real `date`, server-resolved — fixes string date D-ATT-3).
- **Correctable:** an **enabled** date picker (fixes disabled field F-ATT-2) constrained to: not in the future, not before term start, not on a holiday (warn), and within an admin-set back-dating window (e.g. ≤7 days) — beyond that requires admin.
- Re-opening a date that's already marked **loads existing marks for editing** (not a blank slate) — see already-marked edge case.

### 3.4 Status set (4 states)
`present · absent · late · leave` (adds `leave`, D-ATT-1). UI = 4 segmented pills per student (P/A/L/Lv) with large tap targets (Doc 10). Color tokens: present=success, absent=danger, late=warning, leave=accent-soft.

### 3.5 Leave-derived default (fixes B-LV-2)
- On load, the server pre-marks any student with an **Approved** `LeaveRequestsTable` row covering `date` (dateFrom..dateTo) as **leave**, `source='leave_auto'`.
- These default to `leave` but remain **overridable** (a student on approved leave who actually showed up can be set present).
- Conversely, **approving a leave** (TeacherLeaveRouting PATCH) must **write/queue** the `leave` attendance for the covered dates — closing B-LV-2 from both directions.

### 3.6 Bulk + override
- **Bulk "Mark all present"** (Mr. Rao's 40-student reality) → sets every unset student present in one tap; teacher then flips the few exceptions. (Default-present is the common case.)
- **Bulk by selection:** multi-select students → set a status.
- **Override:** any auto/bulk value is individually overridable before save.
- **Running counter:** "38 present · 1 absent · 1 leave · 0 unmarked" always visible.

### 3.7 Save (result-driven, not auto-publish)
- Uses the existing result-driven submit pattern (`isSubmitting`/`submitSuccess`/`submitError` — `TeacherAttendanceViewModel`).
- POST upserts on `(school, date, type, student, assignment)`; `markedBy=me`, `markedAt=now`, `source` per origin.
- Success → Today's period badge flips ✓; obligations strip decrements.
- **No silent publish side effects** (contrast B-MK-1 marks bug — attendance has no "publish," just save).

### 3.8 Endpoints
```
GET  /api/v1/teacher/attendance?assignmentId=…&date=YYYY-MM-DD
   → { students:[{studentId, name, roll, status?, source?}], date, scope, alreadyMarked, leaveDefaults:[…] }
POST /api/v1/teacher/attendance
   body { assignmentId, date, marks:[{studentId, status}] }
   → { saved:n, date }   (upsert; authorized via requireOwnedAssignment)
```

---

## 4. Every Edge Case (explicit)

| # | Edge case | Required behavior |
|---|-----------|-------------------|
| E1 | **Holiday** (calendar HOLIDAY published, Doc 05) | Attendance not solicited; if teacher force-opens, show "This is a holiday ({name}). Mark anyway?" — block by default, allow with confirm (e.g. exam day). |
| E2 | **Cancelled class** (`period_exceptions CANCELLED`) | Period shows struck-through; no unmarked-obligation raised; opening it warns "This class was cancelled for {date}." |
| E3 | **Already marked** | Opening loads existing marks for **edit**, shows "Last marked by {name} at {time}"; saving updates (audit `marked_by/at`). Never silently overwrite without showing prior state. |
| E4 | **Student transferred out** | Student no longer in active `enrollments` for the class → excluded from roster from transfer date; historical marks retained. |
| E5 | **Student transferred in / new admission mid-term** | Appears in roster from enrollment start date; no attendance expected before. |
| E6 | **Approved leave** | Pre-defaulted to `leave` (§3.5), overridable; leave approval writes leave marks (B-LV-2). |
| E7 | **Late then present** | `late` is its own status; not coerced to present. |
| E8 | **Double class teacher** (subject + class teacher both teach same students) | Mark is per `assignment_id`; unique constraint prevents conflicting same-period double marks; cross-period marks for same student same day are independent (period attendance, not day attendance). Decision flag: school config "per-period vs per-day attendance" — default per-period for secondary, per-day for primary (configurable). |
| E9 | **Back-dating beyond window** | Blocked with "Contact admin to mark older than {N} days." |
| E10 | **Future date** | Blocked entirely. |
| E11 | **Offline / flaky network** | Optimistic local save, queued; badge shows "pending sync"; reconcile on reconnect; conflict → server wins with notice. |
| E12 | **No students enrolled** | "No students enrolled in this class yet." (real on fresh seed, X-5) — not a crash, not VComingSoon. |
| E13 | **Timetable unseeded** (no period) | Reached only via Classes path; date=today; if no enrollment data either, E12. |
| E14 | **Substitute teacher** | If I am the substitute (`period_exceptions SUBSTITUTION`), the period appears in my day and I am authorized to mark it for that date only. |
| E15 | **Wrong-class guard** | Roster header always shows "7B · Maths · {date}" prominently; a confirm on save when scope was changed manually (anti-misfile for Aanya). |

---

## 5. Persona Validation

- **Aanya:** Today → "7B Maths · ! unmarked" → Mark attendance → roster pre-loaded → "Mark all present" → flips 1 absent → Save → badge ✓. The big "7B · Maths · Today" header reassures her she's in the right class. ~25 seconds.
- **Mr. Rao:** Obligations strip "5 of 6 unmarked" → taps through each, "all present" + a couple flips, leave-defaults already applied for 2 students on approved leave. Large pills, no string-date parsing, no picker. Old phone: check-in falls back to manual confirm, still works.

---

## 6. Cross-refs & Dependencies

- Scope/`assignmentId` from **Doc 05** (resolved period ⇄ TSA via `requireOwnedAssignment`).
- `enrollments`, typed `student_id` from **Doc 01** target schema (X-1, D-STU).
- Leave auto-write closes **B-LV-2** (Doc 02) and is sequenced in **Doc 11**.
- Biometric `expect/actual` and UI states (loading/empty/error, pill colors, tap targets) specified in **Doc 10**.
- Today badges (`attendanceMarked`) and obligations strip consume this (Doc 04 §5.2/5.5).

---

*End of 06_ATTENDANCE_SYSTEM_SPEC.md*
