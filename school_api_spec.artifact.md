# VidyaPrayag Exhaustive API Specification (School Ecosystem)

This document is the formal technical contract for the **school-admin / institutional**
side of VidyaPrayag.  It complements `vidya_prayag_api_spec2.artifact.md` (which
defined the screen-level announcements / admissions / profile / drawer endpoints
already shipped) and `parent_api_spec.artifact.md` (the parent ecosystem).

**All UI strings, statistics, and configurations are backend-driven.**  Operators
edit CMS keys directly in `app_config` via the Supabase dashboard — the backend
never embeds hard-coded copy.

---

## Modules

1. School Dashboard (Home / Welcome)
2. School Analytics (Overview · Class Performance · Teacher Performance · Student Analytics · Syllabus Coverage)
3. Leave Requests
4. PTM (Parent-Teacher Meeting) Scheduling
5. Messages (Admin inbox)
6. Results (Publish & Review)

> The earlier school-side endpoints (`/school/announcements`, `/school/calendar`,
> `/school/holidays`, `/school/attendance/daily`, `/admissions/enquiries*`,
> `/user/profile*`, `/school/analytics`) are documented in
> `vidya_prayag_api_spec2.artifact.md` and remain unchanged.

---

## Module: School Dashboard

### Screen: School Dashboard (Home)

#### API Name
Get School Dashboard

#### Endpoint
`GET /api/v1/school/dashboard`

#### Authentication
**Required (JWT — role=admin/staff)**

#### Success Response
```json
{
  "success": true,
  "message": "School dashboard fetched successfully",
  "data": {
    "welcome": {
      "title": "Welcome, Admin",
      "subtitle": "Your institutional setup is ready to begin. Let's build your digital campus.",
      "cta_label": "Start Onboarding"
    },
    "onboarding_progress": {
      "completed_steps": 0,
      "total_steps": 4,
      "progress": 0.0,
      "status_label": "Pending",
      "steps": [
        { "id": 1, "title": "Institutional Basics", "description": "School name, location, and IDs.", "is_completed": false, "icon_url": "https://..." },
        { "id": 2, "title": "Branding & Identity",  "description": "Upload logos and color themes.",   "is_completed": false, "icon_url": "https://..." },
        { "id": 3, "title": "Academic Setup",        "description": "Classes, subjects, and teachers.","is_completed": false, "icon_url": "https://..." },
        { "id": 4, "title": "Final Launch",          "description": "Verify and go live.",             "is_completed": false, "icon_url": null }
      ]
    },
    "support": {
      "title": "Need help? Chat with an expert",
      "subtitle": "Available 24/7 for institutions",
      "action_label": "CHAT",
      "video_label": "Watch Onboarding Video",
      "video_url": "https://..."
    }
  }
}
```

**Compute rules**
- `onboarding_progress.steps` are populated from CMS key `school_dashboard_steps`.
- `completed_steps` = count of `BASIC`/`BRANDING`/`ACADEMIC`/`REVIEW` rows in
  `school_onboarding_drafts` that have at least one key for the authenticated
  user.  `total_steps` mirrors `len(steps)`.
- `progress` = `completed_steps / total_steps` (clamped to `[0,1]`).
- `welcome.title` uses CMS key `school_dashboard_welcome` (object with title /
  subtitle / cta_label).
- `support` uses CMS key `school_dashboard_support`.

---

## Module: School Analytics

### Screen: Analytics Dashboard

#### Endpoint
`GET /api/v1/school/analytics/overview`

#### Authentication
**Required (JWT)**

#### Success Response
```json
{
  "success": true,
  "message": "Analytics overview fetched successfully",
  "data": {
    "performance_trend": [0.4, 0.55, 0.48, 0.85, 0.65, 0.75],
    "current_growth": "+4.2%",
    "cards": [
      { "title": "Student Tracking",      "value": "94%", "sub_value": "Avg Attendance",  "icon_url": "https://...", "trend": null },
      { "title": "Syllabus Coverage",     "value": "78%", "sub_value": "Logged Progress", "icon_url": "https://...", "trend": null },
      { "title": "Teacher Accountability","value": "4.8", "sub_value": "Avg Rating",      "icon_url": "https://...", "trend": null },
      { "title": "Class Performance",     "value": "82%", "sub_value": "Proficiency",     "icon_url": "https://...", "trend": null }
    ],
    "insights": [
      { "title": "Attendance Peak", "description": "Class 10-A reached 99% attendance", "icon_name": "trending_up", "icon_color": "#10B981" }
    ]
  }
}
```

**Compute rules**
- `performance_trend` & `current_growth` come from `school_analytics_overview` CMS key
  (so ops can hand-curate them while we build the real ML pipeline).
- `cards[0].value` = avg attendance % across last 7 days of `attendance_records`
  (school-scoped) — falls back to CMS if no rows.
- `cards[1].value` = CMS-driven `school_analytics_syllabus_coverage_card`.
- `cards[2].value` = CMS-driven `school_analytics_teacher_rating_card`.
- `cards[3].value` = CMS-driven `school_analytics_class_proficiency_card`.
- `insights` come from CMS key `school_analytics_insights`.

---

### Screen: Class Performance

#### Endpoint
`GET /api/v1/school/analytics/class-performance?class=Grade%2010-A`

#### Success Response
```json
{
  "success": true,
  "message": "Class performance fetched successfully",
  "data": {
    "grade_distribution": [
      { "grade": "F", "percentage": 12, "value": 0.20 },
      { "grade": "A", "percentage": 38, "value": 0.65 }
    ],
    "summary": {
      "avg_proficiency": "78.4%",
      "active_students": 428,
      "median_grade": "B+"
    },
    "subject_matrix": [
      { "name": "Mathematics", "percentage": 82, "trend": "up" },
      { "name": "Literature",  "percentage": 54, "trend": "down" }
    ],
    "risk": {
      "critical_count": 12,
      "moderate_count": 28,
      "proficiency_target_reach": 75
    },
    "top_performer": { "name": "Elena Rodriguez", "details": "GPA: 3.98 • Grade 11-B" },
    "recent_progress": [
      { "name": "Jordan Davis", "initials": "JD",
        "math": "92%", "science": "88%", "literature": "85%", "attendance": "98%",
        "status": "EXCELLING" }
    ]
  }
}
```

**Compute rules** — `active_students` is `count(students)` for the school
(optionally filtered by `class`). All other fields are CMS-driven via the
`school_class_performance` key (single JSON blob), and `recent_progress` is
derived from the latest 5 entries in `exam_results`.

---

### Screen: Teacher Performance

#### Endpoint
`GET /api/v1/school/analytics/teacher-performance`

#### Success Response
```json
{
  "success": true,
  "message": "Teacher performance fetched successfully",
  "data": {
    "aggregate_compliance": "94.2%",
    "compliance_trend": "+2.4% from last month",
    "syllabus_update_trend": [0.4, 0.6, 0.5, 0.85, 0.7, 1.0, 0.5, 0.75],
    "star_faculty": [
      { "rank": 1, "name": "Dr. Sarah Jenkins", "department": "Mathematics", "score": 99.8, "image_url": "https://..." }
    ],
    "accountability_matrix": [
      { "id": "1", "name": "James Miller", "department": "Chemistry Dept.",
        "compliance_score": 92, "avg_update_delay": "1.2 Days",
        "student_avg_mark": "84.5%", "risk_correlation": "Stable", "initials": "JM" }
    ],
    "dept_efficiencies": [
      { "name": "Science & Technology", "percentage": 96 }
    ]
  }
}
```

**Compute rules** — `star_faculty` is derived from `faculty` table for the school
(ordered by `is_active=true` first, capped at top 3); the score is CMS-curated
in `school_teacher_performance` until the real signal is wired. The rest is CMS.

---

### Screen: Student Analytics

#### Endpoint
`GET /api/v1/school/analytics/student/{studentId}`

#### Success Response
```json
{
  "success": true,
  "message": "Student analytics fetched successfully",
  "data": {
    "student": {
      "id": "ST_501",
      "name": "Aarav Sharma",
      "class": "Grade 10-A",
      "roll_number": "12",
      "profile_pic": "https://..."
    },
    "kpi": { "attendance": "92%", "average": "78.4%", "rank": 7 },
    "subjects": [
      { "name": "Mathematics", "score": 92, "trend": "up" }
    ],
    "milestones": [
      { "title": "Quarter 3 honors list", "date": "2025-09-01", "is_unlocked": true }
    ],
    "narrative": "Aarav has shown a strong rebound in numeracy this quarter."
  }
}
```

**Compute rules**
- `student` block is sourced from `students` (mandatory; 404 if missing).
- `kpi.attendance` = present% over the last 30 days from `attendance_records`.
- `subjects` & `milestones` come from `school_student_analytics_template` CMS
  key (we render the template after substituting the student name).
- `narrative` is CMS-driven `school_student_analytics_narrative`.

---

### Screen: Syllabus Coverage

#### Endpoint
`GET /api/v1/school/analytics/syllabus-coverage`

#### Success Response
```json
{
  "success": true,
  "message": "Syllabus coverage fetched successfully",
  "data": {
    "overall": { "percentage": 78, "trend": "+2.1%" },
    "by_subject": [
      { "name": "Mathematics", "percentage": 82, "behind_by_days": 0 },
      { "name": "Literature",  "percentage": 64, "behind_by_days": 8 }
    ],
    "by_class": [
      { "class": "Grade 10-A", "percentage": 88 }
    ]
  }
}
```

**CMS key** — `school_syllabus_coverage`.

---

## Module: Leave Requests

### Screen: Leave Requests

#### Endpoint
`GET /api/v1/school/leave-requests?type=student|teacher&status=pending|approved|rejected`

```json
{
  "success": true,
  "message": "Leave requests fetched successfully",
  "data": {
    "type": "student",
    "approval_rate": 94,
    "weekly_count": 12,
    "requests": [
      {
        "id": "uuid",
        "requester_name": "Marcus Holloway",
        "requester_role": "student",
        "date_from": "2025-12-12",
        "date_to": "2025-12-14",
        "date_range": "Dec 12 - Dec 14",
        "reason": "Family Function",
        "image_url": "https://...",
        "status": "Pending"
      }
    ]
  }
}
```

#### Create
`POST /api/v1/school/leave-requests` (JWT) — body matches a single `request` object minus `id`/`status`/`approval_rate`/`weekly_count`/`date_range`.

#### Update status
`PATCH /api/v1/school/leave-requests/{id}/status` (JWT)
```json
{ "status": "Approved" }   // Pending | Approved | Rejected
```

**Compute rules**
- `approval_rate` = `100 * approved / (approved + rejected)` over the last 30 days; `0` when denominator is 0.
- `weekly_count` = count of requests created in the last 7 days.

---

## Module: PTM

### Screen: Schedule PTM

#### Endpoint
`GET /api/v1/school/ptm` — returns active event + history + class progress.

```json
{
  "success": true,
  "message": "PTM data fetched successfully",
  "data": {
    "active_event": {
      "id": "uuid",
      "title": "School-Wide PTM",
      "date": "2025-10-28",
      "slot": "09:00 - 13:00",
      "expected_parents": 450,
      "checked_in_parents": 312,
      "invites_delivered": 96,
      "read_receipts": 78
    },
    "history": [
      { "id": "uuid", "date": "2025-09-15", "title": "Term 1 Performance Review",
        "turnout": 78, "total_met": 342 }
    ],
    "class_progress": [
      { "id": "uuid", "class_name": "10A", "teacher_name": "Ms. Sarah Jenkins",
        "met_count": 28, "total_count": 30, "progress": 0.93 }
    ]
  }
}
```

#### Create
`POST /api/v1/school/ptm` — schedules a new PTM.
```json
{
  "title": "Mid-Year PTM",
  "date": "2026-01-15",
  "slot": "10:00 - 14:00",
  "expected_parents": 600
}
```

Returns the newly created `active_event` block.

**Compute rules**
- "Active event" = the most recent row with `date >= today`; if none, the latest past row.
- `progress` per class = `met_count / total_count` (0 when `total_count == 0`).

---

## Module: Messages

### Screen: Messages (Admin Inbox)

#### List threads
`GET /api/v1/school/messages/threads`

```json
{
  "success": true,
  "message": "Threads fetched successfully",
  "data": {
    "threads": [
      {
        "id": "uuid",
        "sender_name": "Mr. Adrian Chen",
        "sender_role": "Class 10-A Teacher",
        "last_message": "The project submission is due tomorrow…",
        "time": "10:45 AM",
        "unread_count": 2,
        "sender_image_url": "https://...",
        "icon_name": null,
        "is_read": false
      }
    ]
  }
}
```

#### Mark read
`POST /api/v1/school/messages/threads/{id}/read` — sets unread_count=0 / is_read=true.

#### Send message (create thread on the fly)
`POST /api/v1/school/messages`
```json
{
  "thread_id": null,
  "sender_name": "Admin Desk",
  "sender_role": "Support",
  "recipient_user_id": "uuid-or-null",
  "body": "Please verify your contact details."
}
```
Returns `{ "thread_id": "...", "message_id": "..." }`.

---

## Module: Results

### Screen: Results

#### Endpoint
`GET /api/v1/school/results?test=Unit%20Test%20II&class=Grade%2010-A&subject=Chemistry`

```json
{
  "success": true,
  "message": "Results fetched successfully",
  "data": {
    "filters": {
      "selected_test": "Unit Test II",
      "selected_class": "Grade 10-A",
      "selected_subject": "Chemistry",
      "available_tests": ["Unit Test I","Unit Test II","Final"],
      "available_classes": ["Grade 10-A","Grade 10-B"],
      "available_subjects": ["Chemistry","Physics","Maths"]
    },
    "summary": {
      "class_average": "78.4",
      "average_trend": "+4.2%",
      "exceeding_count": 12,
      "meeting_count": 18,
      "below_count": 4
    },
    "students": [
      { "id": "ED-001", "name": "Marcus Holloway", "image_url": "https://...",
        "attendance": "100%", "score": "98", "status": "Exceeding", "trend": "+2.4%" }
    ]
  }
}
```

`available_*` arrays come from CMS key `school_results_filters` so operators can
manage curriculum metadata without redeploys.

#### Publish results (bulk upsert)
`POST /api/v1/school/results`
```json
{
  "test": "Unit Test II",
  "class": "Grade 10-A",
  "subject": "Chemistry",
  "results": [
    { "student_id": "ED-001", "score": "98", "attendance": "100%", "status": "Exceeding" }
  ]
}
```

Returns `{ "upserted": <int> }`.

---

## Analytics & Operational

### Database tables added
| Table Name           | Purpose                                              |
|----------------------|------------------------------------------------------|
| leave_requests       | Student / teacher leave applications                 |
| ptm_events           | PTM master record (title/date/slot/turnout stats)    |
| ptm_class_progress   | Per-class PTM "met / total" rollup                   |
| message_threads      | Admin-inbox thread metadata                          |
| messages             | Individual messages within a thread                  |
| exam_results         | Per-student results upserted by Results screen       |

All other data is sourced from existing tables (`app_users`, `schools`, `school_onboarding_drafts`,
`students`, `faculty`, `attendance_records`, `app_config`).

### Analytics events (emitted by client)
- `school_dashboard_opened`
- `analytics_card_clicked`        (props: `card_title`)
- `leave_request_actioned`        (props: `request_id`, `action`)
- `ptm_scheduled`                 (props: `event_id`)
- `results_published`             (props: `test`, `class`, `subject`, `count`)

---

## Spec → Implementation map (this PR)

| Endpoint                                              | File |
|-------------------------------------------------------|------|
| `GET /api/v1/school/dashboard`                        | `feature/school/SchoolDashboardRouting.kt` |
| `GET /api/v1/school/analytics/overview`               | `feature/school/SchoolAnalyticsRouting.kt` |
| `GET /api/v1/school/analytics/class-performance`      | `feature/school/SchoolAnalyticsRouting.kt` |
| `GET /api/v1/school/analytics/teacher-performance`    | `feature/school/SchoolAnalyticsRouting.kt` |
| `GET /api/v1/school/analytics/student/{studentId}`    | `feature/school/SchoolAnalyticsRouting.kt` |
| `GET /api/v1/school/analytics/syllabus-coverage`      | `feature/school/SchoolAnalyticsRouting.kt` |
| `GET /api/v1/school/leave-requests`                   | `feature/school/LeaveRequestsRouting.kt`   |
| `POST /api/v1/school/leave-requests`                  | `feature/school/LeaveRequestsRouting.kt`   |
| `PATCH /api/v1/school/leave-requests/{id}/status`     | `feature/school/LeaveRequestsRouting.kt`   |
| `GET /api/v1/school/ptm`                              | `feature/school/PtmRouting.kt`             |
| `POST /api/v1/school/ptm`                             | `feature/school/PtmRouting.kt`             |
| `GET /api/v1/school/messages/threads`                 | `feature/school/MessagesRouting.kt`        |
| `POST /api/v1/school/messages/threads/{id}/read`      | `feature/school/MessagesRouting.kt`        |
| `POST /api/v1/school/messages`                        | `feature/school/MessagesRouting.kt`        |
| `GET /api/v1/school/results`                          | `feature/school/ResultsRouting.kt`         |
| `POST /api/v1/school/results`                         | `feature/school/ResultsRouting.kt`         |
