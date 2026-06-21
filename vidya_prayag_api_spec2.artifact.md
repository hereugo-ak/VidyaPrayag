# Vidya Prayag API Specification - Part 2

## Screen: School Dashboard (Announcement Tab)

### API Name
**Get School Announcements**

---

### Purpose
Fetches a list of announcements, holidays, PTMs, and events relevant to the user for the school dashboard.

**Used when:**
- User opens the Announcement tab in the School Dashboard.
- User pulls to refresh the announcement list.
- App resumes from background while on the Announcement screen.

---

### Endpoint
`GET /api/v1/school/announcements`

**Base URL:**
`https://api.vidyaprayag.com`

**Full URL:**
`https://api.vidyaprayag.com/api/v1/school/announcements`

---

### Authentication
**Required (JWT Token)**

---

### Headers
| Key | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| Content-Type | String | Yes | application/json |
| Authorization | String | Yes | Bearer <access_token> |
| Device-Id | String | Yes | Unique device identifier |
| Platform | String | Yes | android / ios |

---

### Request Body Schema
*This is a GET request, so parameters are typically passed as Query Params.*

**Query Parameters:**
| Key | Type | Required | Example | Description |
| :--- | :--- | :--- | :--- | :--- |
| user_id | Long | Yes | 182 | The ID of the user (student/parent) fetching announcements |

---

### Success Response
**Response Code: 200 OK**

**Response JSON**
```json
{
  "success": true,
  "message": "Announcements fetched successfully",
  "data": {
    "announcements": [
      {
        "type": "Holidays",
        "event_id": "EVT_101",
        "title": "Summer Vacation",
        "sub_title": "Starting from 1st June",
        "description": "The school will remain closed for summer vacation from June 1st to June 30th. Enjoy your holidays!",
        "event_image": "https://assets.vidyaprayag.com/images/summer_vacation.png",
        "date": "2024-06-01"
      },
      {
        "type": "PTM",
        "event_id": "EVT_102",
        "title": "Parent Teacher Meeting",
        "sub_title": "Quarterly Result Discussion",
        "description": "Discuss the academic progress of your ward with the class teacher in the upcoming PTM.",
        "event_image": null,
        "date": "2024-05-25"
      },
      {
        "type": "Events",
        "event_id": "EVT_103",
        "title": "Annual Sports Day",
        "sub_title": "Let the games begin",
        "description": "Join us for a day filled with athletic competitions and school spirit.",
        "event_image": "https://assets.vidyaprayag.com/images/sports_day.jpg",
        "date": "2024-11-15"
      }
    ]
  }
}
```

**Response Field Definitions**
| Key | Type | Nullable | Description |
| :--- | :--- | :--- | :--- |
| success | Boolean | No | API status indicator |
| message | String | No | User-readable status message |
| data | Object | No | Container for the response data |
| announcements | Array | No | List of announcement objects |
| type | String | No | Category: Holidays/PTM/Events/Special/Remainder |
| event_id | String | No | Unique identifier for the event/announcement |
| title | String | No | Main heading of the announcement |
| sub_title | String | No | Short summary or subtitle |
| description | String | No | Detailed content of the announcement |
| event_image | String | Yes | URL of the image associated with the event |
| date | String | No | Relevant date for the announcement (YYYY-MM-DD) |

---

### Failure Responses

**Unauthorized Access**
```json
{
  "success": false,
  "message": "Session expired, please login again"
}
```
**HTTP: 401 Unauthorized**

**User Not Found**
```json
{
  "success": false,
  "message": "User not associated with any school"
}
```
**HTTP: 404 Not Found**

---

### Database Tables Involved
| Table Name | Operation |
| :--- | :--- |
| announcements | Read |
| school_events | Read |
| student_enrollment | Read (to filter by class/section) |

---

### Dependencies
| Dependency | Description |
| :--- | :--- |
| User Profile | Required to determine which school/class announcements to show |
| Asset Server | Needed for hosting and serving `event_image` URLs |

---

### Used In Screens
- School Dashboard (Announcement Tab)
- Notification Center

---

### Caching Strategy
| Type | Details |
| :--- | :--- |
| Local Cache | Room database or Encrypted SharedPreferences to store the last 20 announcements for offline viewing. |
| Expiry | 4 hours (TTL) |

---

### Analytics Events
| Event | Attributes |
| :--- | :--- |
| announcement_viewed | user_id, school_id |
| announcement_clicked | event_id, type |

---

### Retry Behaviour
| Scenario | Behaviour |
| :--- | :--- |
| Network Timeout | Retry automatically up to 2 times with exponential backoff. |
| 500 Internal Error | Show "Something went wrong" with a manual Retry button. |
| 401 Unauthorized | Redirect to Login screen. |

---
---

### API Name
**Search Announcements**

---

### Purpose
Searches for announcements based on a text query.

**Used when:**
- User types in the search bar within the Announcement tab.

---

### Endpoint
`GET /api/v1/school/announcements/search`

**Base URL:**
`https://api.vidyaprayag.com`

**Full URL:**
`https://api.vidyaprayag.com/api/v1/school/announcements/search`

---

### Authentication
**Required (JWT Token)**

---

### Headers
| Key | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| Content-Type | String | Yes | application/json |
| Authorization | String | Yes | Bearer <access_token> |

---

### Request Body Schema
**Query Parameters:**
| Key | Type | Required | Example | Description |
| :--- | :--- | :--- | :--- | :--- |
| query | String | Yes | "vacation" | The text to search for in title or description |
| user_id | Long | Yes | 182 | User ID to filter relevant announcements |

---

### Success Response
**Response Code: 200 OK**

**Response JSON**
*(Returns the same list structure as Get School Announcements)*
```json
{
  "success": true,
  "message": "Search results fetched",
  "data": {
    "announcements": [
      {
        "type": "Holidays",
        "event_id": "EVT_101",
        "title": "Summer Vacation",
        "sub_title": "Starting from 1st June",
        "description": "The school will remain closed for summer vacation.",
        "event_image": "https://assets.vidyaprayag.com/images/summer_vacation.png",
        "date": "2024-06-01"
      }
    ]
  }
}
```

---

### Database Tables Involved
| Table Name | Operation |
| :--- | :--- |
| announcements | Read (Full-text search) |

---
---

### API Name
**Sync Announcements to WhatsApp**

---

### Purpose
Syncs all unsynced announcements to parents via WhatsApp.

**Used when:**
- Admin/Staff triggers a sync from the dashboard to ensure all parents receive updates on WhatsApp.

---

### Endpoint
`POST /api/v1/school/announcements/sync-whatsapp`

**Base URL:**
`https://api.vidyaprayag.com`

**Full URL:**
`https://api.vidyaprayag.com/api/v1/school/announcements/sync-whatsapp`

---

### Authentication
**Required (Admin/Staff Token)**

---

### Headers
| Key | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| Content-Type | String | Yes | application/json |
| Authorization | String | Yes | Bearer <admin_access_token> |

---

### Request Body Schema
```json
{
  "school_id": 45,
  "announcement_ids": ["EVT_101", "EVT_102"]
}
```

**Request Field Definitions**
| Key | Type | Required | Example | Description |
| :--- | :--- | :--- | :--- | :--- |
| school_id | Long | Yes | 45 | ID of the school |
| announcement_ids | Array | No | ["EVT_101"] | Optional list of specific IDs to sync. If empty, syncs all unsynced. |

---

### Success Response
**Response Code: 202 Accepted**

**Response JSON**
```json
{
  "success": true,
  "message": "Sync process initiated successfully",
  "data": {
    "job_id": "SYNC_WA_9982",
    "total_queued": 150,
    "estimated_time_minutes": 5
  }
}
```

---

### Database Tables Involved
| Table Name | Operation |
| :--- | :--- |
| announcements | Read/Update (mark as synced) |
| parents | Read (fetch phone numbers) |
| whatsapp_logs | Insert |

---

### Dependencies
| Dependency | Description |
| :--- | :--- |
| WhatsApp Business API | Required to send messages |
| Message Queue (Redis/RabbitMQ) | To handle bulk message processing asynchronously |

---
---

## Screen: Admission Enquiries Dashboard

### API Name
**Get Admission Enquiries Summary**

---

### Purpose
Fetches a summary of admission enquiries, a list of recent enquiries, and an efficiency metric for the school dashboard.

**Used when:**
- Staff/Admin opens the Admission Enquiries dashboard.
- Dashboard is refreshed to see latest stats.

---

### Endpoint
`GET /api/v1/admissions/enquiries/summary`

**Base URL:**
`https://api.vidyaprayag.com`

**Full URL:**
`https://api.vidyaprayag.com/api/v1/admissions/enquiries/summary`

---

### Authentication
**Required (Staff/Admin Token)**

---

### Headers
| Key | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| Content-Type | String | Yes | application/json |
| Authorization | String | Yes | Bearer <access_token> |

---

### Request Body Schema
**Query Parameters:**
| Key | Type | Required | Example | Description |
| :--- | :--- | :--- | :--- | :--- |
| user_id | Long | Yes | 182 | ID of the staff/admin user |

---

### Success Response
**Response Code: 200 OK**

**Response JSON**
```json
{
  "success": true,
  "message": "Enquiry summary fetched successfully",
  "data": {
    "summary_count": {
      "total": 450,
      "new": 120,
      "follow_ups": 200,
      "converted": 130
    },
    "recent_enquiries": [
      {
        "student_name": "Aarav Sharma",
        "parent_name": "Rajesh Sharma",
        "class": "Grade 5",
        "date": "2024-05-18",
        "status": "new",
        "profile_pic": "https://assets.vidyaprayag.com/profiles/student1.jpg"
      },
      {
        "student_name": "Ishita Verma",
        "parent_name": "Sanjay Verma",
        "class": "Grade 1",
        "date": "2024-05-15",
        "status": "followup",
        "profile_pic": null
      }
    ],
    "efficiency": "85%"
  }
}
```

**Response Field Definitions**
| Key | Type | Nullable | Description |
| :--- | :--- | :--- | :--- |
| success | Boolean | No | API status indicator |
| summary_count | Object | No | Numeric stats for different statuses |
| total | Integer | No | Total number of enquiries |
| new | Integer | No | Enquiries with 'new' status |
| follow_ups | Integer | No | Enquiries with 'followup' status |
| converted | Integer | No | Enquiries with 'converted' status |
| recent_enquiries | Array | No | List of latest enquiry objects |
| student_name | String | No | Name of the prospective student |
| parent_name | String | No | Name of the parent |
| class | String | No | Target class for admission |
| date | String | No | Date of enquiry (YYYY-MM-DD) |
| status | String | No | Status: new / followup / converted |
| profile_pic | String | Yes | URL of student profile image |
| efficiency | String | No | Calculated performance metric |

---

### Database Tables Involved
| Table Name | Operation |
| :--- | :--- |
| admission_enquiries | Read (Aggregations and recent list) |

---
---

### API Name
**View All Enquiries**

---

### Purpose
Fetches a complete list of all admission enquiries for the school.

**Used when:**
- User clicks "View All" from the dashboard.
- User navigates to the full Enquiry List screen.

---

### Endpoint
`GET /api/v1/admissions/enquiries`

**Base URL:**
`https://api.vidyaprayag.com`

**Full URL:**
`https://api.vidyaprayag.com/api/v1/admissions/enquiries`

---

### Authentication
**Required (Staff/Admin Token)**

---

### Headers
| Key | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| Content-Type | String | Yes | application/json |
| Authorization | String | Yes | Bearer <access_token> |

---

### Request Body Schema
**Query Parameters:**
| Key | Type | Required | Example | Description |
| :--- | :--- | :--- | :--- | :--- |
| user_id | Long | Yes | 182 | ID of the staff/admin user |
| page | Integer | No | 1 | For pagination |
| limit | Integer | No | 20 | Items per page |

---

### Success Response
**Response Code: 200 OK**

**Response JSON**
```json
{
  "success": true,
  "message": "Enquiries fetched",
  "data": {
    "enquiries": [
      {
        "student_name": "Aarav Sharma",
        "parent_name": "Rajesh Sharma",
        "class": "Grade 5",
        "date": "2024-05-18",
        "status": "new",
        "profile_pic": "https://assets.vidyaprayag.com/profiles/student1.jpg"
      }
    ],
    "pagination": {
      "current_page": 1,
      "total_pages": 23,
      "total_records": 450
    }
  }
}
```

---

### Database Tables Involved
| Table Name | Operation |
| :--- | :--- |
| admission_enquiries | Read |

---
---

## Screen: User Profile / School Profile

### API Name
**Get User Profile**

---

### Purpose
Fetches the detailed profile information for a user (typically a school's public/private profile), including philosophy, video tours, and gallery storage details.

**Used when:**
- User views their own profile or a school's detailed profile.
- Accessing the gallery or school philosophy section.

---

### Endpoint
`GET /api/v1/user/profile`

**Base URL:**
`https://api.vidyaprayag.com`

**Full URL:**
`https://api.vidyaprayag.com/api/v1/user/profile`

---

### Authentication
**Required (JWT Token)**

---

### Headers
| Key | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| Content-Type | String | Yes | application/json |
| Authorization | String | Yes | Bearer <access_token> |

---

### Request Body Schema
**Query Parameters:**
| Key | Type | Required | Example | Description |
| :--- | :--- | :--- | :--- | :--- |
| user_id | Long | No | 182 | Optional. If not provided, returns profile for the authenticated user. |

---

### Success Response
**Response Code: 200 OK**

**Response JSON**
```json
{
  "success": true,
  "message": "Profile fetched successfully",
  "data": {
    "public_profile": true,
    "philosophy_details": {
      "core_mission": "Empowering young minds through holistic education.",
      "learning_model": "Inquiry-based collaborative learning",
      "primary_language": "English"
    },
    "video_tour_data": [
      "https://vidyaprayag.com/videos/tour1.mp4",
      "https://vidyaprayag.com/videos/lab_intro.mp4"
    ],
    "gallery": {
      "images": [
        "https://assets.vidyaprayag.com/gallery/img1.jpg",
        "https://assets.vidyaprayag.com/gallery/img2.jpg"
      ],
      "total_storage": "10 GB",
      "storage_used": "2.4 GB"
    }
  }
}
```

**Response Field Definitions**
| Key | Type | Nullable | Description |
| :--- | :--- | :--- | :--- |
| public_profile | Boolean | No | Indicates if the profile is visible to the public |
| philosophy_details | Object | No | Contains mission and model details |
| core_mission | String | No | The core mission statement of the school/user |
| learning_model | String | No | Description of the educational approach |
| primary_language | String | No | Main medium of instruction or communication |
| video_tour_data | Array | No | List of video URLs for the school tour |
| gallery | Object | No | Contains images and storage information |
| images | Array | No | List of image URLs in the gallery |
| total_storage | String | No | Total allocated storage capacity |
| storage_used | String | No | Current storage consumption |

---

### Database Tables Involved
| Table Name | Operation |
| :--- | :--- |
| users | Read |
| school_philosophy | Read |
| school_media | Read |
| storage_metrics | Read |

---
---

### API Name
**Update Philosophy**

---

### Purpose
Updates the school's core mission, learning model, and primary language.

**Used when:**
- User edits and saves changes in the Philosophy section of their profile.

---

### Endpoint
`PUT /api/v1/user/profile/philosophy`

**Base URL:**
`https://api.vidyaprayag.com`

**Full URL:**
`https://api.vidyaprayag.com/api/v1/user/profile/philosophy`

---

### Authentication
**Required (JWT Token)**

---

### Headers
| Key | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| Content-Type | String | Yes | application/json |
| Authorization | String | Yes | Bearer <access_token> |

---

### Request Body Schema
```json
{
  "core_mission": "Updated core mission statement.",
  "learning_model": "Project-based learning",
  "primary_language": "Hindi/English"
}
```

**Request Field Definitions**
| Key | Type | Required | Example | Description |
| :--- | :--- | :--- | :--- | :--- |
| core_mission | String | No | "Updated mission..." | The new core mission statement |
| learning_model | String | No | "Project-based..." | The new educational approach |
| primary_language | String | No | "Hindi/English" | The updated medium of instruction |

---

### Success Response
**Response Code: 200 OK**

**Response JSON**
```json
{
  "success": true,
  "message": "Philosophy updated successfully"
}
```

---

### Database Tables Involved
| Table Name | Operation |
| :--- | :--- |
| school_philosophy | Update |

---
---

### API Name
**Update Tour Video**

---

### Purpose
Updates the list of video tour URLs for the school profile.

**Used when:**
- User adds, removes, or reorders videos in the Tour Video section.

---

### Endpoint
`PUT /api/v1/user/profile/tour-videos`

**Base URL:**
`https://api.vidyaprayag.com`

**Full URL:**
`https://api.vidyaprayag.com/api/v1/user/profile/tour-videos`

---

### Authentication
**Required (JWT Token)**

---

### Headers
| Key | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| Content-Type | String | Yes | application/json |
| Authorization | String | Yes | Bearer <access_token> |

---

### Request Body Schema
```json
{
  "video_tour_data": [
    "https://vidyaprayag.com/videos/tour_updated.mp4",
    "https://vidyaprayag.com/videos/new_intro.mp4"
  ]
}
```

**Request Field Definitions**
| Key | Type | Required | Example | Description |
| :--- | :--- | :--- | :--- | :--- |
| video_tour_data | Array | Yes | ["url1", "url2"] | Complete list of video URLs to be saved |

---

### Success Response
**Response Code: 200 OK**

**Response JSON**
```json
{
  "success": true,
  "message": "Tour videos updated successfully"
}
```

---

### Database Tables Involved
| Table Name | Operation |
| :--- | :--- |
| school_media | Delete / Insert (Sync list) |

---
---

### API Name
**Update Gallery**

---

### Purpose
Updates the images in the school gallery.

**Used when:**
- User uploads new photos or deletes existing ones from the gallery.

---

### Endpoint
`PUT /api/v1/user/profile/gallery`

**Base URL:**
`https://api.vidyaprayag.com`

**Full URL:**
`https://api.vidyaprayag.com/api/v1/user/profile/gallery`

---

### Authentication
**Required (JWT Token)**

---

### Headers
| Key | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| Content-Type | String | Yes | application/json |
| Authorization | String | Yes | Bearer <access_token> |

---

### Request Body Schema
```json
{
  "images": [
    "https://assets.vidyaprayag.com/gallery/img1.jpg",
    "https://assets.vidyaprayag.com/gallery/new_upload.jpg"
  ]
}
```

**Request Field Definitions**
| Key | Type | Required | Example | Description |
| :--- | :--- | :--- | :--- | :--- |
| images | Array | Yes | ["url1", "url2"] | Complete list of image URLs for the gallery |

---

### Success Response
**Response Code: 200 OK**

**Response JSON**
```json
{
  "success": true,
  "message": "Gallery updated successfully",
  "data": {
    "storage_used": "2.6 GB",
    "total_storage": "10 GB"
  }
}
```

---

### Database Tables Involved
| Table Name | Operation |
| :--- | :--- |
| school_media | Delete / Insert (Sync list) |
| storage_metrics | Update |

---
---

## Screen: Drawer Options

### API Name
**Get Analytics (Coming Soon)**

---

### Purpose
Placeholder API for school performance analytics. Currently marked as a "Coming Soon" feature.

**Used when:**
- User clicks on 'Analytics' in the side drawer.

---

### Endpoint
`GET /api/v1/school/analytics`

**Status:**
`DRAFT / COMING SOON`

---

### Success Response
**Response Code: 200 OK**

**Response JSON**
```json
{
  "success": true,
  "message": "Analytics feature is coming soon!",
  "data": {
    "is_available": false,
    "expected_release": "Q3 2024"
  }
}
```

---
---

### API Name
**Get Academic Calendar**

---

### Purpose
Fetches the academic calendar events and summary for a specific date range and standard.

**Used when:**
- User views the Academic Calendar screen.
- User toggles between week/month view.

---

### Endpoint
`GET /api/v1/school/calendar`

**Base URL:**
`https://api.vidyaprayag.com`

**Full URL:**
`https://api.vidyaprayag.com/api/v1/school/calendar`

---

### Authentication
**Required (JWT Token)**

---

### Headers
| Key | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| Content-Type | String | Yes | application/json |
| Authorization | String | Yes | Bearer <access_token> |

---

### Request Body Schema
**Query Parameters:**
| Key | Type | Required | Example | Description |
| :--- | :--- | :--- | :--- | :--- |
| date | String | Yes | "2024-05-01" | Reference date for the view |
| view_type | String | Yes | "month" | View mode: week / month |
| standard | String | No | "Grade 5" | Filter events by class/standard |

---

### Success Response
**Response Code: 200 OK**

**Response JSON**
```json
{
  "success": true,
  "message": "Academic calendar fetched successfully",
  "data": {
    "calendar_events": [
      {
        "date": "2024-05-01",
        "day": "Wednesday",
        "event_id": "CAL_101",
        "event_title": "May Day",
        "event_description": "Public holiday on account of International Labour Day."
      },
      {
        "date": "2024-05-15",
        "day": "Wednesday",
        "event_id": "CAL_102",
        "event_title": "Unit Test 1",
        "event_description": "First periodic assessment starts for Grade 5."
      }
    ],
    "summary": {
      "total_working_days": 22,
      "public_holidays": 1,
      "school_holidays": 4
    }
  }
}
```

**Response Field Definitions**
| Key | Type | Nullable | Description |
| :--- | :--- | :--- | :--- |
| calendar_events | Array | No | List of events for the requested period |
| date | String | No | Date of the event (YYYY-MM-DD) |
| day | String | No | Name of the day |
| event_id | String | No | Unique ID for the calendar event |
| event_title | String | No | Title of the event |
| event_description | String | No | Detailed description of the event |
| summary | Object | No | Aggregated stats for the period |
| total_working_days | Integer | No | Count of non-holiday weekdays |
| public_holidays | Integer | No | Count of government mandated holidays |
| school_holidays | Integer | No | Count of school-specific holidays/breaks |

---

### Database Tables Involved
| Table Name | Operation |
| :--- | :--- |
| academic_calendar | Read |
| holiday_list | Read |

---
---

### API Name
**Get Holidays List**

---

### Purpose
Fetches a list of holidays filtered by frequency (weekly/monthly/yearly).

**Used when:**
- User views the holiday list section.

---

### Endpoint
`GET /api/v1/school/holidays`

---

### Request Body Schema
**Query Parameters:**
| Key | Type | Required | Example | Description |
| :--- | :--- | :--- | :--- | :--- |
| filter_type | String | Yes | "yearly" | Frequency: weekly / monthly / yearly |

---

### Success Response
**Response Code: 200 OK**

**Response JSON**
```json
{
  "success": true,
  "message": "Holidays list fetched",
  "data": {
    "holidays": [
      {
        "date": "2024-01-26",
        "title": "Republic Day",
        "type": "Public"
      },
      {
        "date": "2024-08-15",
        "title": "Independence Day",
        "type": "Public"
      }
    ]
  }
}

---
---

### API Name
**Get Daily Attendance**

---

### Purpose
Fetches daily attendance records for students or faculty, filtered by grade.

**Used when:**
- User views the Daily Attendance screen from the side drawer.
- User toggles between Student and Faculty attendance.
- User selects a specific grade/class to view attendance details.

---

### Endpoint
`GET /api/v1/school/attendance/daily`

**Base URL:**
`https://api.vidyaprayag.com`

**Full URL:**
`https://api.vidyaprayag.com/api/v1/school/attendance/daily`

---

### Authentication
**Required (JWT Token)**

---

### Headers
| Key | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| Content-Type | String | Yes | application/json |
| Authorization | String | Yes | Bearer <access_token> |

---

### Request Body Schema
**Query Parameters:**
| Key | Type | Required | Example | Description |
| :--- | :--- | :--- | :--- | :--- |
| user_id | Long | Yes | 182 | ID of the staff/admin user requesting data |
| type | String | Yes | "student" | Filter type: student / faculty |
| grade | String | No | "Grade 5" | Specific class or grade (Required for 'student' type) |

---

### Success Response
**Response Code: 200 OK**

**Response JSON**
```json
{
  "success": true,
  "message": "Daily attendance fetched successfully",
  "data": {
    "type": "student",
    "grade": "Grade 5",
    "present_count": 35,
    "absent_count": 5,
    "total_count": 40,
    "attendance_percentage": "87.5%",
    "attendance_list": [
      {
        "profile_pic": "https://assets.vidyaprayag.com/profiles/student1.jpg",
        "name": "Aarav Sharma",
        "id": "ST_501",
        "status": "present"
      },
      {
        "profile_pic": "https://assets.vidyaprayag.com/profiles/student2.jpg",
        "name": "Isha Kapoor",
        "id": "ST_502",
        "status": "absent"
      }
    ]
  }
}
```

**Response Field Definitions**
| Key | Type | Nullable | Description |
| :--- | :--- | :--- | :--- |
| type | String | No | The type of attendance requested (student/faculty) |
| grade | String | Yes | The grade/class name (null if type is faculty) |
| present_count | Integer | No | Number of individuals present |
| absent_count | Integer | No | Number of individuals absent |
| total_count | Integer | No | Total number of individuals in the group |
| attendance_percentage | String | No | Percentage of presence (e.g., "90%") |
| attendance_list | Array | No | List of individual attendance records |
| profile_pic | String | Yes | URL of the profile image |
| name | String | No | Full name of the student or faculty member |
| id | String | No | Unique ID of the student or faculty member |
| status | String | No | Attendance status: present / absent |

---

### Database Tables Involved
| Table Name | Operation |
| :--- | :--- |
| attendance_records | Read |
| students | Read (to fetch names/pics) |
| faculty | Read (to fetch names/pics) |
```
