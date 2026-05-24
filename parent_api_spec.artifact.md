# VidyaPrayag Exhaustive API Specification (Parent Ecosystem)

This document serves as the formal technical contract for the VidyaPrayag ecosystem. **All UI strings, statistics, and configurations are backend-driven.**

---

## Module: Child Onboarding

### Screen: Child Basic Info (Step 1)

#### API Name
Get Onboarding Metadata

#### Purpose
Fetches reference data for onboarding dropdowns and the visual configuration for the screen.

#### Used when:
- User lands on Step 1.
- Data refresh is triggered.

#### Endpoint
`GET /api/v1/parent/onboarding/metadata`

#### Authentication
**Required (JWT)**

#### Success Response
**Response Code: 200 OK**

**Response JSON**
```json
{
  "success": true,
  "data": {
    "screen_config": {
      "header_title": "Let's build a profile for your child.",
      "header_subtitle": "We use this information to curate the best learning path.",
      "progress_label": "Step 1 of 3",
      "progress_value": 0.33
    },
    "available_grades": ["Grade 1", "Grade 2", "Grade 3", "Nursery", "KG"],
    "available_interests": ["Music", "Art", "STEM", "Sports", "Languages"],
    "footer_text": "Your data is encrypted and secure."
  }
}
```

---

#### API Name
Submit Child Basic Details

#### Purpose
Registers the child's identity.

#### Endpoint
`POST /api/v1/parent/onboarding/child-info`

#### Request JSON
```json
{
  "child_name": "Aarav Sharma",
  "date_of_birth": "2018-05-15",
  "gender": "MALE",
  "current_grade": "Grade 1",
  "interests": ["Music", "STEM"]
}
```

---

### Screen: Your Preferences (Step 2)

#### API Name
Get Preference Options

#### Purpose
Fetches available search filters (Boards, Budget ranges, etc.) for the preferences screen.

#### Endpoint
`GET /api/v1/parent/onboarding/preference-options`

#### Success Response
```json
{
  "success": true,
  "data": {
    "available_boards": ["CBSE", "ICSE", "IB", "State Board"],
    "available_focus_areas": [
      { "id": "acad", "title": "Academics", "icon": "school" },
      { "id": "sports", "title": "Sports", "icon": "sports_soccer" }
    ],
    "budget_config": {
      "min_value": 0,
      "max_value": 10000,
      "default_range": [2000, 5000],
      "currency_symbol": "$"
    }
  }
}
```

---

## Module: Core Dashboard & Progress

### Screen: Parent Dashboard (Home)

#### API Name
Get Dashboard Overview

#### Purpose
The primary "Handshake" API. Drives the entire home screen content including child stats and alerts.

#### Endpoint
`GET /api/v1/parent/dashboard`

#### Success Response
```json
{
  "success": true,
  "data": {
    "greeting": "Good Morning, Arjun",
    "child_summary": {
      "id": "std_101",
      "name": "Aarav",
      "overall_progress": 0.75,
      "current_level": 4,
      "attendance_status": "PRESENT",
      "profile_pic": "https://..."
    },
    "alerts": [
      { "id": "a1", "title": "Fees Due", "value": "$1,200", "type": "CRITICAL" },
      { "id": "a2", "title": "Upcoming PTM", "value": "Nov 25", "type": "INFO" }
    ],
    "featured_schools": [
      { "id": "s1", "name": "St. Xavier", "rating": 4.8, "location": "Noida", "image": "..." }
    ],
    "curation_logic": "Curation aligned with NEP 2020 developmental milestones."
  }
}
```

---

### Screen: Track Progress (Holistic Growth)

#### API Name
Get Holistic Progress

#### Purpose
Drives the complex visual charts and milestones on the Track Progress screen.

#### Endpoint
`GET /api/v1/parent/track-progress`

#### Success Response
```json
{
  "success": true,
  "data": {
    "hero_section": {
      "progress_percentage": 75,
      "level_label": "LEVEL 4 REACHED",
      "journey_description": "On track for Grade 2 transition"
    },
    "badges": [
      { "title": "Social Star", "icon": "workspace_premium", "is_locked": false, "colors": ["#B6C7EB", "#006C49"] }
    ],
    "academic_core": {
      "label": "NEP ALIGNED",
      "competencies": [
        { "title": "Literacy", "progress": 0.85, "icon": "translate" }
      ]
    },
    "emotional_intelligence": {
      "description": "Significant growth in Social Interaction this month.",
      "metrics": { "Empathy": 0.8, "Resilience": 0.7, "Social": 0.9 }
    },
    "play_discovery": [
      { "title": "Agility", "description": "Gross motor met", "image": "...", "status": "MET" }
    ],
    "last_updated": "Today, 10:45 AM"
  }
}
```

---

## Module: School Management

### Screen: Fees

#### API Name
Get Detailed Fee Status

#### Purpose
Fetches collection stats, overdue counts, and payment announcements.

#### Endpoint
`GET /api/v1/parent/fees`

#### Success Response
```json
{
  "success": true,
  "data": {
    "stats": {
      "total_collected": "$428,500",
      "progress": 0.85,
      "outstanding": "$72,120",
      "overdue_count": 145
    },
    "announcements": [
      { "id": "f1", "title": "Deadline", "time": "2h ago", "desc": "Submit Q3 fees.", "type": "Payment" }
    ]
  }
}
```

---

### Screen: School Dashboard (Announcement Tab)

#### API Name
Get School Announcements

#### Purpose
Fetches the categorized announcement feed for parents.

#### Endpoint
`GET /api/v1/school/announcements`

#### Success Response
```json
{
  "success": true,
  "data": {
    "announcements": [
      {
        "type": "Holidays",
        "event_id": "EVT_101",
        "title": "Summer Vacation",
        "sub_title": "Starting from 1st June",
        "description": "School closed for 30 days.",
        "event_image": "https://...",
        "date": "2024-06-01"
      }
    ]
  }
}
```

---

## Module: Drawer & Background

### Screen: Support & Legal

#### API Name
Get Support Config

#### Purpose
Drives the contact information and support categories dynamically.

#### Endpoint
`GET /api/v1/content/support`

#### Success Response
```json
{
  "success": true,
  "data": {
    "support_contact": "+91-9876543210",
    "categories": ["TECHNICAL", "ACADEMIC", "ADMISSIONS", "FEES"],
    "help_center_url": "https://vidyaprayag.com/help"
  }
}
```

---

## Analytics & Operational

### Analytics Events
- `onboarding_abandoned`: Tracks which screen user left during onboarding.
- `payment_failed`: Captures error codes for payment gateway issues.
- `report_downloaded`: Tracks user interest in printable results.

### Database Tables Involved
| Table Name | Operation | Purpose |
|---|---|---|
| app_users | Read | Profile & Role data |
| students | Read | Growth & Progress metrics |
| fee_records | Read | Financial tracking |
| announcements | Read | Communications |
| app_config | Read | Global feature flags & text |
| reference_grades | Read | Metadata for dropdowns |
