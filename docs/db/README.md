# VidyaPrayag / Vidyasetu — Database Schema (Canonical)

This folder is the **single source of truth** for the database schema the Ktor
backend (`server/`) expects. It resolves the schema-source drift flagged in
`SCHOOL_SIDE_STATUS_REPORT.md` §5.3 (three different schema references existed:
root `supabase_schema`, the uploaded `Vidyasetu schema.txt`, and `Tables.kt`).

## Files

| File | Purpose |
|---|---|
| `vidyasetu_schema.sql` | The operational table set currently used by the Ktor backend (imported from the uploaded `Vidyasetu schema.txt`). 28 tables. |
| `migration_001_faculty_and_holiday_list.sql` | **Supplementary** tables the backend's `Tables.kt` maps but the base schema was missing: `faculty`, `holiday_list`. |
| `migration_002_segmentation_geo_assignments.sql` | Broadcast segmentation columns on `announcements`; `latitude`/`longitude` on `schools`; new `teacher_subject_assignments` table; `student_code` link on `children`. (P1/P2 — report §4.4, §5.5, §5.6, §5.7.) |

## How to provision a fresh Supabase / Postgres database

Run these in order in **Supabase → SQL Editor**:

1. `vidyasetu_schema.sql`  — base operational tables.
2. `migration_001_faculty_and_holiday_list.sql` — adds `faculty` + `holiday_list`.
3. `migration_002_segmentation_geo_assignments.sql` — adds segmentation, geo, teacher assignments, and the `children.student_code` link.

> ⚠️ The `faculty:` and `holiday_list:` blocks in the raw uploaded
> `Vidyasetu schema.txt` / `vidyasetu_schema.sql` are copy-paste duplicates of
> other tables (they re-create `exam_results` / `fee_records`). The CORRECT
> definitions live in `migration_001`. Always run `migration_001`; do NOT rely
> on those two blocks in the base file.

After this, every table referenced by
`server/src/main/kotlin/com/littlebridge/vidyaprayag/db/Tables.kt` exists.

## Tables mapped by the backend (`Tables.kt`)

`app_users`, `auth_otps`, `otp_delivery_attempts`, `user_sessions`,
`cms_landing_content`, `app_config`, `schools`, `school_onboarding_drafts`,
`school_classes`, `school_subjects`, `announcements`, `whatsapp_logs`,
`admission_enquiries`, `school_philosophy`, `school_media`, `storage_metrics`,
`academic_calendar`, **`holiday_list`**, **`faculty`**, `attendance_records`,
`students`, `children`, `fee_records`, `leave_requests`, `ptm_events`,
`ptm_class_progress`, `message_threads`, `messages`, `exam_results`.

> The base schema also has a `users` table (Supabase/Auth-related / legacy). The
> backend intentionally uses `app_users`, not `users`, for app auth/user records.

## Verifying after provisioning

```bash
# All three should succeed (no "relation does not exist", no empty-on-error):
curl -H "Authorization: Bearer <token>" http://<host>/api/v1/school/holidays?filter_type=yearly
curl -H "Authorization: Bearer <token>" "http://<host>/api/v1/school/attendance/daily?type=faculty"
curl -H "Authorization: Bearer <token>" http://<host>/api/v1/school/calendar
```
