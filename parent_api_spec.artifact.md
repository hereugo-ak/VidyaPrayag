# VidyaPrayag Exhaustive API Specification (Parent Ecosystem)

This document serves as the formal technical contract for the VidyaPrayag ecosystem. **All UI strings, statistics, and layout configurations are backend-driven.**

---

## Module: Child Onboarding

### Screen: Child Basic Info (Step 1)

#### API Name
Get Onboarding Metadata

#### Purpose
Fetches reference data for onboarding dropdowns and the visual configuration for the screen.

#### Endpoint
`GET /api/v1/parent/onboarding/metadata`

#### Success Response
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

### Screen: Your Preferences (Step 2)

#### API Name
Get Preference Options

#### Purpose
Fetches available search filters for the preferences screen.

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
The primary Handshake API driving the entire home screen.

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
      "attendance_status": "PRESENT"
    },
    "alerts": [
      { "id": "a1", "title": "Fees Due", "value": "$1,200", "type": "CRITICAL" }
    ],
    "featured_schools": [
      { "id": "s1", "name": "St. Xavier", "rating": 4.8, "location": "Noida" }
    ]
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
      { "title": "Agility", "description": "Gross motor met", "image_url": "...", "status": "MET" }
    ]
  }
}
```

---

## Module: Career Path & Future Roadmap

### Screen: Career Path Discovery

#### API Name
Get AI Career Path Discovery

#### Purpose
Provides trajectory insights based on child’s talent mapping. **UI styling including floating app bar config and padding are backend-driven.**

#### Endpoint
`GET /api/v1/parent/career-path`

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
      "app_bar": {
        "title": "Future Roadmap",
        "is_floating": true,
        "is_transparent": true,
        "background_colors": ["#10B9811A", "#FFFFFF"]
      },
      "layout": {
        "vertical_padding": 24,
        "horizontal_padding": 24,
        "system_nav_padding": true
      },
      "celebration_icon": "verified",
      "main_heading": "Career Insight!",
      "sub_heading_template": "We've found {count} career paths matching {name}'s profile!",
      "footer_description": "We've analyzed thousands of data points to predict the perfect fit."
    },
    "career_stats": {
      "predicted_count": 12,
      "top_match": {
        "title": "Aerospace Engineering",
        "match_percentage": 98,
        "image_url": "https://...",
        "industry_growth_label": "High Growth Industry",
        "industry_growth_value": "+12.4% YoY",
        "tags": [
          { "label": "STEM Focus", "is_highlight": true },
          { "label": "Innovation", "is_highlight": false }
        ]
      }
    },
    "action_buttons": {
      "primary": { "text": "Explore Detailed Roadmap", "action": "OPEN_DETAILS" },
      "secondary": { "text": "Recalibrate Profile", "action": "RESTART_ONBOARDING" }
    }
  }
}
```

#### Response Field Definitions
| Key | Type | Nullable | Description |
|---|---|---|---|
| app_bar | Object | No | Config for the floating top navigation |
| system_nav_padding | Boolean | No | If true, applies window insets for gesture bars |
| top_match | Object | No | The primary AI recommendation |
| is_highlight | Boolean | No | Determines if the tag chip uses a distinctive color |

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
- `career_path_recalibrated`: Tracks user engagement with talent mapping.

### Database Tables Involved
| Table Name | Operation | Purpose |
|---|---|---|
| app_users | Read | Profile & Role data |
| students | Read | Growth & Progress metrics |
| fee_records | Read | Financial tracking |
| announcements | Read | Communications |
| app_config | Read | Global feature flags & text |
| reference_grades | Read | Metadata for dropdowns |
| student_talent_map | Read | AI Career predictions |
