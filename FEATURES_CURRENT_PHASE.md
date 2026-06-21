# VidyaSetu — Current-Phase Feature List

**Branch:** `backend-by-abuzar_v1.0.2`
**Date:** 2026-06-09
**Stack:** Kotlin Compose Multiplatform (`composeApp` UI + `shared` domain/data) · Ktor server (`server`) · Supabase Postgres (Exposed) · Koin DI · DataStore prefs.
**Roles:** School Admin · Teacher · Parent (multi-tenant, isolated by `school_id` derived from the JWT-resolved DB row — never the request body).

> This document lists the features **actually implemented and shipping in this phase**, grounded in the live screens (`ui/v2/screens/*`) and server routes (`server/.../feature/*`). It is not a roadmap — items not listed here are either future phases or explicitly honest "Coming soon" surfaces.

---

## Shared / Cross-Cutting (all roles)

- **Common landing screen** — premium landing surface in the V* design system: hero (value taglines + real campus photo), Featured Institutions photo rail, "TRUSTED BY 500+ INSTITUTIONS" social-proof strip, role entry points (Parent / School), Next-Gen Intelligence showcase, Portal access rail, footer. Parent CTA → OTP auth; School CTA → credential auth.
- **Role-aware authentication & routing** — post-login routing reads the fresh JWT every time: Parent → parent portal, Admin → admin portal, Teacher → teacher portal. No fallthrough, no stale role.
- **Secure session lifecycle** — logout clears the DataStore session **and** the Ktor bearer-token cache (`SessionManager.clearAuthCache()`), so a re-login as a different role never serves a stale token. Auth screen input fields reset on entry after logout.
- **Multi-tenant isolation** — every school-scoped query filters on `school_id` resolved server-side from the JWT-keyed `app_users` row. No cross-school data on any endpoint.
- **Notifications spine** — in-app, DB-backed notifications (`NotificationsTable`), school-scoped, with deep links. Every cross-role write that affects another user emits a notification.
- **Three-state screens** — every data screen renders loading skeleton + error state + empty state via `VStateHost`.
- **Design system** — single V* design system (V* primitives, `VColors` tokens, `VTheme` typography, `VMotion`). No legacy Material chrome, no fabricated/hardcoded metrics in production render paths.

---

## 👪 Parent

### Onboarding & account
- **OTP phone sign-in / sign-up** — returning parents are asked for OTP only; new families also provide a name. Name field is gated on the signup flow.
- **Opt-in child linking** — a freshly authenticated parent lands straight on their portal and links a child whenever they choose, from **Profile → Linked children**. Linking creates a PENDING admin-approval request (never force-linked).
- **Multi-child support** — parents with more than one child get a child switcher; the selected child scopes the academic tabs.

### Child academics (live, child-scoped from Supabase)
- **Attendance — month calendar** — a true month-grid calendar with per-day colour coding (present / late / absent), month paging, summary header (rate, present/late/absent counts), and a legend.
- **Marks** — published assessment results for the child (exam, subject, score / max). The class/section join relaxes gracefully so linked-but-section-mismatched children still see their published marks.
- **Syllabus** — per-subject coverage log (units covered / pending, progress %).
- **Progress overview** — academic competencies + emotional-intelligence radar + journey level (from the track-progress endpoint).

### Day-to-day
- **Home dashboard** — child-scoped overview.
- **Fees** — fee status / dues for the child (`ParentFeesScreen` → fees endpoint).
- **Apply for leave** — submit a leave request for an owned child; routed to the child's class teacher; status tracked.
- **Messaging** — full two-way messaging. Parents can **reply** and **initiate new conversations** (compose-new): the recipient picker resolves the child's class teacher (flagged first) + the school office/admins via a dedicated `/recipients` endpoint. Messages are immutable (no delete).
- **Activity feed & scholarships** — activity timeline and scholarships surface.
- **Profile** — manage profile, linked children, sign out.

---

## 🧑‍🏫 Teacher

### Account
- **First-login flow** — teachers sign in with credentials minted inside their school's admin account.
- **Class/subject/exam selector** — `TeacherUpdateSelector` reads real `GET /teacher/classes` + `GET /teacher/assessments`, so the write plane is always reachable.

### Write paths (all persist to Supabase, scoped to the teacher's own assignments)
- **Attendance marking** — upserts `attendance_records` per student (present / absent / late); notifies parents of absent/late. The Submit button is **result-driven** (reflects the real network outcome — spinner in flight, confirmed success, inline errors).
- **Marks entry** — upserts `assessment_marks` (clamped to max), auto-publishes the assessment so parents can read it, and notifies class parents. Save is result-driven like attendance.
- **Homework** — creates homework and notifies class parents.
- **Syllabus** — toggles unit coverage (`is_covered` + covered-on / covered-by) for the teacher's own class+subject.
- **Leave approvals** — approve/reject leave requests for the teacher's own classes; notifies the applicant parent.

### Communication
- **Messaging** — reply, **compose-new** (pick a recipient), and **class broadcast** (fan-out to every class parent), each emitting notifications. Messages are immutable.

### Other
- **Home dashboard**, **Classes**, **Profile**.

---

## 🏫 School Admin

### Dashboard & analytics
- **Home dashboard** — real onboarding hero + live campus metrics (from the school dashboard endpoint).
- **Analytics dashboard** — overview with trend chart, metric cards and insights from `GET /school/analytics/overview` (honest empty state).
- **Early-warning radar (PEWS)** — opens the real at-risk student cohort (from the student-cohort aggregate). The home teaser is a label-free schematic (no fabricated students).

### People management
- **Teacher roster & profiles** — provision teachers, view teacher profiles, class/teacher performance.
- **Student roster & profiles** — student roster + student profile screens.
- **Link requests** — approve/reject parent child-link requests.

### Academics & operations
- **Daily attendance** — school-wide attendance view.
- **Results publishing** — publish assessment results; **notifies class parents** on publish (parity with the teacher marks path).
- **Admissions CRM** — admissions pipeline.
- **PTM scheduling** — schedule parent-teacher meetings.
- **Leave requests** — review and decide leave requests.
- **School records** — institutional records surface.

### Communication
- **Announcements** — compose audience-targeted announcements (ALL_SCHOOL / CLASS / SUBJECT / STUDENT); audience-scoped parent (+ teacher for all-school) notifications.
- **Messaging** — reply **and initiate new conversations** (compose-new with a teacher recipient picker); every send notifies the recipient. Messages are immutable.

### Settings
- **Edit institutional profile**, **school settings**.

---

## Communication system — capability matrix

| Initiator → Recipient | Can initiate | Can reply | Notifies recipient |
|-----------------------|:---:|:---:|:---:|
| Parent → Teacher | ✅ | ✅ | ✅ |
| Parent → Admin | ✅ | ✅ | ✅ |
| Teacher → Parent | ✅ (incl. class broadcast) | ✅ | ✅ |
| Teacher → Admin | ✅ | ✅ | ✅ |
| Admin → Teacher | ✅ | ✅ | ✅ |
| Admin → Parent | ✅ | ✅ | ✅ |

- **Immutable by design** — there is **no delete route** for messages anywhere on the server.
- **School-scoped** — every conversation and message query filters by `school_id` from the JWT. No cross-tenant message visibility.

---

## Notification triggers (cross-role)

| Trigger | Notifies |
|---------|----------|
| Teacher marks attendance absent/late | Parents of the student |
| Teacher submits marks (auto-publishes) | Class parents |
| Teacher creates homework | Class parents |
| Teacher decides a leave request | Applicant parent |
| Teacher sends message / class broadcast | Recipient(s) |
| Admin publishes an announcement | Audience-scoped parents (+ teachers for all-school) |
| Admin publishes results | Class parents |
| Admin / Teacher / Parent direct message | The recipient |

---

## Honesty notes (what is intentionally "Coming soon")

These surfaces are **honest placeholders** (clearly labelled, no fabricated data) — not shipped features this phase:

- Notifications **delivery log** (push/SMS) — DB notifications ship; external delivery is a future phase.
- AI Report Card (parent academics "Report" tab) — schematic preview only.
- Discovery / nearby-schools deep features, school records detail — partial / preview surfaces.

*Generated for branch `backend-by-abuzar_v1.0.2` — reflects the live code as of 2026-06-09.*
