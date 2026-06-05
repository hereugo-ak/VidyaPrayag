# VidyaSetu — Complete Frontend Rebuild Plan
### *Bridging Schools, Parents & Data — India's School Intelligence Layer*
**Document Type:** Internal Engineering & Design Blueprint  
**Version:** 1.0  
**Status:** Pre-Build Reference — Replace Entire Frontend  
**Author:** Solo Developer  
**Confidential — Founding Team Only**

---

> **Before anything else:** Take a deep breath. Read this entire document before writing a single line of code. This is not a feature list — it is the complete nervous system of the product. Every screen, every data flow, every navigation path, every role boundary is documented here. The goal is zero ambiguity before a single component is touched.

---

## Table of Contents

1. [The First Principle — Why We're Rebuilding](#1-the-first-principle)
2. [Backend Audit — What Exists, What Doesn't](#2-backend-audit)
3. [Design System — The Visual Language](#3-design-system)
4. [App Logo & Identity](#4-app-logo--identity)
5. [Loading & Splash Experience](#5-loading--splash-experience)
6. [Role Architecture & Data Model](#6-role-architecture--data-model)
7. [Authentication Flow — All Portals](#7-authentication-flow)
8. [School Onboarding — The Foundation Layer](#8-school-onboarding)
9. [Administration Dashboard & Screens](#9-administration-portal)
10. [Teacher Portal — Screens & Flows](#10-teacher-portal)
11. [Parent Portal — Screens & Flows](#11-parent-portal)
12. [School Discovery Marketplace](#12-school-discovery-marketplace)
13. [Cross-Portal Shared Screens](#13-cross-portal-shared-screens)
14. [Navigation Architecture](#14-navigation-architecture)
15. [Coming Soon — Backend Not Yet Ready](#15-coming-soon-features)
16. [Information Flow Matrix](#16-information-flow-matrix)
17. [Component Library](#17-component-library)
18. [Rebuild Execution Order](#18-rebuild-execution-order)

---

## 1. The First Principle

The old frontend is dead. Do not migrate, salvage, or reference any element from it. Start from zero.

**Why the old frontend failed:**
- No coherent role-boundary UX — teachers saw admin controls, parents saw nothing useful
- No consistent design language — screens felt like different apps
- Navigation was a maze — users couldn't find what they needed in under 3 taps
- Generic placeholder data everywhere — destroyed trust on first impression
- No personality — looked like every other Indian SaaS app built in a weekend

**What this rebuild must achieve:**
- A parent opens the app and understands their child's school day in under 10 seconds
- A teacher marks attendance in under 30 seconds without reading any instruction
- A school principal can see the health of their entire institution on one screen
- Every screen looks like it costs money to use — premium but approachable

---

## 2. Backend Audit

> **Instruction for Claude (when auditing backend before build):** Pull every route from the FastAPI codebase. For each route, document: endpoint URL, HTTP method, request payload, response shape, auth requirement, and which UI screen consumes it. Group them by domain. Mark each screen in this document as LIVE (backend ready), PARTIAL (backend exists but needs work), or COMING SOON (no backend yet). Do not wire a screen to a non-existent endpoint — show "Coming Soon" UI instead.

### Known Backend Domains (Supabase + FastAPI)

| Domain | Estimated Status | Notes |
|--------|-----------------|-------|
| Auth (Admin/Teacher/Parent login) | LIVE | Supabase Auth with JWT. Role-based. |
| School Registration & Onboarding | PARTIAL | Basic school record exists. Class/subject/teacher linking needs audit. |
| Attendance (Mark & Read) | LIVE | Teacher marks via app. Parent notifications likely broken. |
| Marks Entry & Analytics | PARTIAL | Entry exists. Trend computation and analytics need audit. |
| Syllabus Coverage Log | PARTIAL | Basic entry exists. CBSE/board mapping may be absent. |
| Fee Structure & Reminders | PARTIAL | Structure setup likely exists. Razorpay not integrated yet. |
| Homework & Assignments | UNKNOWN | Audit required. |
| Parent-Teacher Messaging | UNKNOWN | Audit required. |
| School Discovery & SRI | COMING SOON | SRI algorithm not built. |
| PEWS (Early Warning) | COMING SOON | ML model not trained. |
| AI Report Cards | COMING SOON | Gemini API integration not built. |
| WhatsApp Bot | PARTIAL | Webhook handler built. Full flow needs audit. |
| Compliance Report Generation | COMING SOON | Not built. |
| Alumni Tracking | COMING SOON | Long-term feature. |

### Audit Command for Developer

```bash
# Run this against your FastAPI codebase before starting build
grep -r "@app\.\|@router\." --include="*.py" -h | sort | uniq
# Then document each route in the matrix above
```

---

## 3. Design System

### 3.1 The Color Rule — Non-Negotiable

There are exactly **three base colors** in VidyaSetu. Nothing else.

```
COLOR 1 — VOID BLACK
Hex:    #080808
Usage:  All backgrounds in dark contexts, navigation bars, modal overlays, card backgrounds in dark mode
Rule:   This is the ONLY black used anywhere in the application. No #000000. No #111111. No #1A1A1A. 
        Not in dark mode. Not in light mode. Only #080808.
Name:   "Void"

COLOR 2 — CLOUD WHITE  
Hex:    #F5F5F3
Usage:  Primary background in light contexts, card surfaces, input field backgrounds
Rule:   This is not pure white. It is white with an almost imperceptible warm grey undertone.
        It should feel like premium writing paper, not a hospital wall.
Name:   "Cloud"

COLOR 3 — ARCTIC BLUE
Hex:    #C8DEFF
Usage:  All interactive elements (CTAs, active states, links, progress indicators, selected tabs, 
        badges, chart accent lines, notification dots)
Rule:   This is sky blue leaning toward silver — NOT the saturated electric blue of generic SaaS.
        On Void Black backgrounds it creates a cool, premium, almost frosted-glass feel.
        On Cloud White backgrounds it reads as a soft institutional accent.
Name:   "Arctic"
```

### 3.2 Extended Palette — Derived, Never Arbitrary

These are computed from the three base colors. No new colors are introduced.

```
VOID BLACK at opacity layers:
  #080808 at 100%  → Pure void (backgrounds)
  #080808 at 85%   → Modal overlays
  #080808 at 60%   → Disabled state backgrounds
  #080808 at 12%   → Subtle dividers on Cloud backgrounds

CLOUD WHITE at opacity layers:
  #F5F5F3 at 100%  → Primary surface
  #F5F5F3 at 70%   → Secondary text on dark backgrounds
  #F5F5F3 at 40%   → Placeholder text on dark backgrounds
  #F5F5F3 at 15%   → Hover states on dark backgrounds

ARCTIC BLUE at opacity layers:
  #C8DEFF at 100%  → Primary CTA, selected states
  #C8DEFF at 70%   → Secondary interactive elements
  #C8DEFF at 30%   → Background tints, tag fills
  #C8DEFF at 10%   → Very subtle background accents

SEMANTIC COLORS (derived, not arbitrary):
  Success Green:  #A8E6CF  (minty, not neon — used for "present", "paid", "on track")
  Warning Amber:  #FFD4A3  (warm cream-amber — used for "absent warning", "fee due")
  Danger Coral:   #FFADA8  (soft coral — used for "chronic absent", "failed", "overdue")
  
  Rule: Semantic colors are used ONLY for data states (attendance, marks, fees). Never for branding.
```

### 3.3 Typography

```
PRIMARY TYPEFACE: "Plus Jakarta Sans"
  — Google Fonts, free, works beautifully at every weight
  — Feels premium without trying to look premium
  — Excellent Devanagari-adjacent legibility for Hindi future-proofing
  
SECONDARY / DATA TYPEFACE: "DM Mono"
  — Used exclusively for numbers, IDs, roll numbers, fees, marks
  — Tabular figures align perfectly in lists and tables
  — The mono font signals "this is data you can trust"

Scale (mobile-first):
  Display:   36sp  Plus Jakarta Sans  ExtraBold   Line height: 1.1
  Title:     24sp  Plus Jakarta Sans  Bold        Line height: 1.2
  Subtitle:  18sp  Plus Jakarta Sans  SemiBold    Line height: 1.3
  Body:      15sp  Plus Jakarta Sans  Regular     Line height: 1.5
  Caption:   12sp  Plus Jakarta Sans  Medium      Line height: 1.4
  Label:     11sp  Plus Jakarta Sans  SemiBold    Line height: 1.3  (ALL CAPS, 0.8 letter-spacing)
  Data:      15sp  DM Mono            Regular     Line height: 1.4
  Data Sm:   13sp  DM Mono            Regular     Line height: 1.3
```

### 3.4 Elevation & Surfaces

```
On Void Black backgrounds, depth is created by LIGHT, not shadow.

SURFACE LEVELS (for dark mode / dark UI):
  Level 0 — Background:  #080808
  Level 1 — Card:        #080808 + 1px border rgba(248,248,247,0.08)
  Level 2 — Sheet:       #080808 + 1px border rgba(248,248,247,0.14)
  Level 3 — Dialog:      #080808 + 1px border rgba(248,248,247,0.20)
  Level 4 — Tooltip:     #F5F5F3 on #080808 (inverted for maximum contrast)

On Cloud White backgrounds, depth is created by SHADOW:
  Level 0 — Background:  #F5F5F3
  Level 1 — Card:        #FFFFFF, shadow: 0 2px 8px rgba(8,8,8,0.06)
  Level 2 — Sheet:       #FFFFFF, shadow: 0 4px 20px rgba(8,8,8,0.10)
  Level 3 — Dialog:      #FFFFFF, shadow: 0 8px 40px rgba(8,8,8,0.16)

RULE: The app uses DARK MODE for the Admin and Teacher portals.
      The app uses LIGHT MODE for the Parent portal and Discovery marketplace.
      Rationale: Admins/teachers are power users who stare at screens all day.
                 Parents are casual, emotional users — light feels warmer and trustworthy.
```

### 3.5 Border Radius & Spacing

```
BORDER RADIUS:
  Chip / Tag:     6px
  Input field:    10px
  Card:           14px
  Modal / Sheet:  20px (top corners only for bottom sheets)
  Full pill:      999px (for status badges and primary CTAs)
  Avatar:         50% circle always

SPACING SYSTEM (base 4):
  xs:   4px
  sm:   8px
  md:  16px
  lg:  24px
  xl:  32px
  2xl: 48px
  3xl: 64px

GRID:
  Mobile:  16px horizontal padding. No columns — single content stream.
  Tablet:  24px horizontal padding. Two-column cards where applicable.
```

### 3.6 Icons

```
ICON SET: Phosphor Icons (MIT License, free, available for React Native / Android)
  — 7,000+ icons. Multiple weights. Used at "Regular" weight primarily.
  — Do NOT use Material Icons. Do NOT use Heroicons. Only Phosphor.
  — This is a visual consistency decision — mixing icon sets is how apps look cheap.

SIZES:
  Navigation bar icons:  24px
  Card action icons:     20px
  Inline/label icons:    16px
  Hero/feature icons:    48px

COLOR:
  Icons on Void backgrounds:   #F5F5F3 (Cloud White) at 70%
  Active/selected icons:       #C8DEFF (Arctic Blue) at 100%
  Icons on Cloud backgrounds:  #080808 at 60%
  Active/selected icons:       #080808 at 100%
  Semantic state icons:        Use semantic color (green/amber/coral) at 100%
```

### 3.7 Motion & Animation

```
PRINCIPLES:
  — Physics-based, never linear. Everything uses spring curves.
  — Fast responses (< 200ms for interactive feedback), slower transitions (300-400ms for screen changes)
  — Reduce motion setting respected at OS level

STANDARD TIMINGS:
  Micro-interaction (button press, toggle):     150ms, ease-out
  Card/item appearance (staggered list):        200ms each, 30ms stagger, spring
  Screen transition (push/pop navigation):      320ms, spring(stiffness:300, damping:30)
  Modal / bottom sheet entry:                   380ms, spring(stiffness:250, damping:28)
  Loading skeleton pulse:                       1400ms, ease-in-out, infinite

SPLASH/LOGO ANIMATION:  See Section 5 — this is special.
```

---

## 4. App Logo & Identity

### 4.1 Logo Concept

The VidyaSetu logo is an abstract mark, not a literal illustration. It is designed to feel like it was crafted by a senior identity designer — geometric precision, optical corrections, deliberate negative space.

**Mark:** A bridge form (सेतु) made of two abstract arcs that simultaneously read as:
- An open book (विद्या — knowledge)
- A bridge span
- A data flow connection between two nodes

The arcs are drawn with slightly different weights — the lower arc is heavier than the upper — creating the subconscious perception of structural solidity, the way suspension bridges carry more weight at the base.

**Wordmark:** "VidyaSetu" in Plus Jakarta Sans ExtraBold, with the 'S' in Arctic Blue to visually anchor the brand color.

**Color variants:**
```
Primary (on dark):  Mark in #F5F5F3 (Cloud White). "S" in #C8DEFF (Arctic).
Reversed (on light): Mark in #080808 (Void). "S" in #A0C4F7 (slightly deeper arctic for contrast).
Monochrome:         Full mark in single color when needed for embossing/print.
App Icon:           Mark only (no wordmark). Void Black background. Cloud White mark. Arctic "S" accent.
```

**App Icon Shape:**
- Rounded rectangle, iOS 18 / Android 14 adaptive icon spec
- Corner radius: system default (do not hard-code)
- Background: #080808 — pure Void
- The bridge mark sits centered, slightly oversized (fills ~68% of the icon space)
- A single thin Arctic ring — 1px — traces the inner border of the rounded rect, adding premium depth

### 4.2 Logo Usage Rules

```
MINIMUM SIZE: 32px height for digital. Below this, use icon mark only.
CLEAR SPACE:  Equal to the height of the capital "V" on all four sides.
NEVER:
  — Stretch or distort
  — Place on busy photographic backgrounds without a backing
  — Use drop shadows on the mark
  — Recreate in off-brand colors
  — Use the wordmark without the mark
```

---

## 5. Loading & Splash Experience

This is the most important 3 seconds of the entire app. It must be extraordinary.

### 5.1 Sequence

```
Frame 0 (0ms):
  — Screen: Pure #080808 void
  — Nothing. Complete darkness.

Frame 1 (0–400ms):
  — The bridge mark materializes from the center
  — Not a fade-in — a physics-based "breath": starts at 60% scale, 0% opacity
  — Spring animation to 100% scale, 100% opacity
  — Duration: 400ms, spring(stiffness:180, damping:18) — a single clean bounce

Frame 2 (400ms–700ms):
  — The mark is fully visible, Cloud White on Void Black
  — A thin Arctic glow "traces" the outline of the bridge arcs (like a neon tube lighting up)
  — This is a SVG stroke-dashoffset animation from 0% to 100%
  — Duration: 280ms, ease-in-out

Frame 3 (700ms–1000ms):
  — The wordmark slides up from 12px below with opacity 0→1
  — Not a bouncy slide — smooth, authoritative, ease-out
  — Duration: 260ms

Frame 4 (1000ms–1400ms):
  — Hold for the auth/role check to complete
  — A minimal progress indicator: a 1px Arctic line that grows left-to-right below the wordmark
  — Indeterminate loop during auth check

Frame 5 (1400ms+, transition out):
  — The entire splash "blends" into the target screen (Login or Dashboard)
  — NOT a cut or a cross-fade — a blending dissolve where the splash mark 
    appears to merge into the destination screen's header/logo area
  — Achieved via: shared element transition on the logo + simultaneous full-screen 
    opacity transition from Void to destination background
  — Duration: 500ms, ease-in-out
  — The user feels like the screen grew out of the logo, not that one screen replaced another
```

### 5.2 Technical Notes

```
Android: Use Splashscreen API (Android 12+) for the initial system splash.
         Then hand off to an in-app Compose animation for the full sequence above.
         The system splash should show only the icon mark on Void background.

Frame rate target: 60fps minimum, 120fps on devices that support it.
Do not use Lottie for this — use native Compose/View animations for physics accuracy.

The brand glow trace (Frame 2) is the single most important detail.
Get this right. It should look like the app is waking up, not loading.
```

---

## 6. Role Architecture & Data Model

### 6.1 Role Hierarchy

```
                        ┌─────────────────────┐
                        │   VidyaSetu Super   │
                        │   Admin (Internal)  │
                        └──────────┬──────────┘
                                   │
              ┌────────────────────┼────────────────────┐
              │                                         │
    ┌─────────▼──────────┐               ┌─────────────▼──────────┐
    │  School Admin       │               │  Parent                 │
    │  (Per school)       │               │  (Per family)           │
    │  Full power over    │               │  Read-only on their     │
    │  their school data  │               │  child's enrolled school│
    └─────────┬──────────┘               └─────────────────────────┘
              │
    ┌─────────▼──────────┐
    │  Teacher            │
    │  (Created by Admin) │
    │  Scoped to their    │
    │  classes/subjects   │
    └─────────────────────┘
```

### 6.2 What Each Role Can Do

| Capability | Super Admin | School Admin | Teacher | Parent |
|------------|:-----------:|:------------:|:-------:|:------:|
| Create school | ✅ | ❌ | ❌ | ❌ |
| Onboard school (setup) | ✅ | ✅ | ❌ | ❌ |
| Create classes | ✅ | ✅ | ❌ | ❌ |
| Create subjects | ✅ | ✅ | ❌ | ❌ |
| Assign teacher to subject | ✅ | ✅ | ❌ | ❌ |
| Create teacher accounts | ✅ | ✅ | ❌ | ❌ |
| Upload student CSV | ✅ | ✅ | ❌ | ❌ |
| Mark attendance | ✅ | ✅ | ✅ (own classes) | ❌ |
| Enter marks | ✅ | ✅ | ✅ (own classes) | ❌ |
| Update syllabus | ✅ | ✅ | ✅ (own classes) | ❌ |
| Send announcements | ✅ | ✅ | ✅ (own classes) | ❌ |
| View all students | ✅ | ✅ | ✅ (own classes) | ❌ |
| View own child data | ❌ | ❌ | ❌ | ✅ |
| View school comparison | ✅ | ✅ | ❌ | ✅ |
| **Modify past data** | ✅ (override) | ❌ | ❌ | ❌ |
| Delete historical records | ✅ | ❌ | ❌ | ❌ |

**THE IMMUTABILITY RULE:** Once attendance, marks, or syllabus entries are submitted by a teacher or admin, they become read-only. Admin can flag a record for correction (with reason), which creates a correction request logged with timestamp and user. The original data is preserved. Only VidyaSetu Super Admin can hard-delete. This is displayed to admins clearly: *"Submitted records are locked for integrity. Contact VidyaSetu support to initiate a correction."*

### 6.3 Multi-Child Profile (Parent)

A parent account can be linked to multiple children. Rules:
- Children may be in different schools (both must be on VidyaSetu)
- Each child is a separate profile with its own data stream
- Parent sees a **profile switcher** on the home screen — prominently placed, not buried in settings
- Notifications are tagged with child name: "Riya - Absent today" not just "Absent today"
- Each child profile has its own notification settings

### 6.4 Teacher Scoping

A teacher account is created by school admin. Their access is scoped at creation:
- Teacher is linked to: Class(es) + Subject(s) within those classes
- A teacher may teach the same subject in Class 9A and 9B but NOT Class 10 (unless assigned)
- The teacher sees ONLY their assigned classes in their dashboard
- They cannot see marks entered by another teacher, even for the same student
- They CAN see attendance for any student in their assigned classes (not just their subject)

---

## 7. Authentication Flow

### 7.1 Login Screen Design

**The login screen has a portal selector — not multiple apps or multiple login URLs.**

```
Screen Structure:
  — Top half: App logo + tagline on Void Black background
  — Bottom half: Bottom sheet that slides up (always visible, not triggered by tap)
  — The sheet is Cloud White. Corner radius 24px on top corners only.

Portal Tabs (inside the bottom sheet):
  Three pill tabs, side by side:
  ┌──────────┐ ┌──────────────┐ ┌──────────┐
  │  Parent  │ │  School /    │ │  Teacher │
  │          │ │   Admin      │ │          │
  └──────────┘ └──────────────┘ └──────────┘
  
  Active tab: Arctic Blue fill. Inactive tabs: Cloud at 30% with Void text.

Parent login:
  — Mobile number (primary)
  — OTP verification (via SMS / WhatsApp)
  — After OTP: lands on Parent Dashboard
  — New parent with no linked child → guided to "Find Your Child's School"

School/Admin login:
  — School ID or registered email
  — Password
  — "Forgot password" → OTP to registered admin mobile
  — After login: lands on Admin Dashboard

Teacher login:
  — Credential format: schoolcode.teachercode (generated by admin, e.g., SJH001.TCH07)
  — Password (set by admin during teacher profile creation, must change on first login)
  — After login: lands on Teacher Dashboard
  
Below the tabs, input fields and CTA update to match the selected portal.
```

### 7.2 First Login (Teacher)

```
Sequence:
  1. Teacher enters schoolcode.teachercode + temporary password
  2. System detects first_login = true
  3. Forced password change screen (cannot skip)
  4. Optional: upload profile photo
  5. Verify personal mobile number (for notifications)
  6. Brief onboarding carousel: "Here's what you'll do on VidyaSetu" (3 screens, skippable)
  7. Land on Teacher Dashboard
```

### 7.3 First Login (Parent — New Registration)

```
Sequence:
  1. Parent enters mobile number
  2. OTP sent via SMS (WhatsApp fallback if SMS fails)
  3. OTP verified
  4. "Tell us about yourself": name, language preference (Hindi / English)
  5. "Link your child": search by school name → enter child's roll number or admission number
  6. System validates roll number against school's student database
  7. If match: child profile linked, parent can see child's name + class + photo (if uploaded)
  8. "Add another child?" option
  9. Land on Parent Dashboard
```

---

## 8. School Onboarding

This is the most critical flow in the product. A school that doesn't complete onboarding correctly will have broken data everywhere. It must be:
- Structured (wizard, not freeform)
- Saveable at every step (progress preserved if admin closes app)
- Clear about what's mandatory vs. optional
- Tested against real school structures before shipping

### 8.1 Onboarding Wizard — Step by Step

```
PROGRESS INDICATOR: A horizontal stepper at the top. 6 steps. Each step shows a check when complete.
```

#### Step 1 — School Identity

**Fields (all required unless marked optional):**
```
School Name (full legal name)
School Short Name (max 20 chars — used in Teacher credential format)
Registration / Affiliation Number
Board: [CBSE] [ICSE] [UP State Board] [Other — specify]
Type: [Government] [Private Aided] [Private Unaided] [Central Govt]
Medium of Instruction: [English] [Hindi] [English + Hindi] [Other]
School Logo (optional — upload JPG/PNG, used in report cards and notifications)
School Address: Street, City, District, PIN
Founded Year (optional)
School Phone Number
School Email
Principal Name
Principal Mobile
```

**What this creates in Supabase:** `schools` record with all above fields + `school_id` (UUID) + `school_code` (auto-generated 6-char alphanumeric, editable once).

#### Step 2 — Academic Year Setup

```
Current Academic Year: [2025-26] [2026-27] (dropdown, current auto-selected)
Academic Year Start Date: (date picker)
Academic Year End Date: (date picker)
Working Days Per Week: (toggle: Mon-Fri / Mon-Sat)
School Timings: Start time / End time (time picker)
Number of Periods Per Day: (stepper: 1-12)
```

**What this creates:** `academic_years` record. All subsequent data (attendance, marks) is scoped to this year.

#### Step 3 — Class & Section Setup

This is the most important structural step. It defines the skeleton of the school.

```
UI: An expandable list builder.

"Add Class" button creates a row:
  Class Name: [Class 1] [Class 2] ... [Class 12] [Nursery] [KG 1] [KG 2] [Custom]
  Sections: Multi-select chips: [A] [B] [C] [D] [E] [F] — tap to add/remove
  
Example result:
  Class 9 — Sections: A, B, C
  Class 10 — Sections: A, B
  Class 12 — Sections: Science, Commerce, Arts

Minimum: 1 class, 1 section.
Maximum: No limit.

IMPORTANT: Classes cannot be deleted after Step 5 (students uploaded). 
Admin must be warned: "Review carefully — you'll be uploading students to these classes."
```

**What this creates:** `classes` records + `sections` records, all linked to `school_id`.

#### Step 4 — Subject Setup

```
UI: Class-by-class subject assignment.

Selector: Choose a class/section combination (tabs across top)

For each class-section:
  "Add Subject" button:
    Subject Name: Text input with auto-suggest from CBSE/ICSE subject list
    Subject Code: Auto-generated, editable
    Subject Type: [Core / Theory] [Practical / Lab] [Co-curricular] [Language]
    Is Exam Subject? Toggle (affects marks and report cards)

Example for Class 10-A:
  Mathematics (MAT001) — Core/Theory — Yes
  Science (SCI001) — Core/Theory — Yes  
  Science Practical (SCIP01) — Practical/Lab — Yes
  English (ENG001) — Core/Theory — Yes
  Hindi (HIN001) — Language — Yes
  Computer Applications (COMP01) — Core/Theory — Yes
  Physical Education (PE001) — Co-curricular — No
  
IMPORTANT NOTE: Same subject across different classes/sections is a SEPARATE record.
"Mathematics for Class 9-A" and "Mathematics for Class 10-A" are different subjects 
even if the name is the same. This allows different teachers, different syllabi, different marks.
```

**What this creates:** `subjects` records, each linked to a specific `class_id` + `section_id`.

#### Step 5 — Teacher Setup

```
UI: A form that creates teacher profiles AND their login credentials.

"Add Teacher" opens a slide-up form:
  Full Name (required)
  Mobile Number (required — for notifications and WhatsApp)
  Email (optional)
  Employee ID (optional — school's internal ID)
  
  Subjects Assigned: Multi-select from subjects created in Step 4
    — Shows: [Class Name — Subject Name] for each option
    — Teacher can be assigned multiple subjects across multiple classes
    — CUSTOMISATION IS AT SUBJECT LEVEL: same teacher → different classes of same subject is fine
    — Different teachers → same subject in different classes is fine
  
  Login Credentials (auto-generated, admin can edit):
    Username: school_code.T001 (T = teacher, 001 = auto-incremented)
    Temporary Password: Random 8-char alphanumeric (shown once, copy button)
  
  Class Teacher Of: (optional) [Class 9-A] — makes them the primary contact for that section
  
"Import Teachers from CSV" option:
  Download template CSV
  Fill: name, mobile, email, employee_id, assigned_subjects (comma-separated codes)
  Upload CSV → preview → confirm
  System auto-generates credentials for each imported teacher
  
After adding all teachers, admin gets a PDF: 
  "Teacher Login Credentials — [School Name]"
  One row per teacher: Name, Username, Temporary Password
  Printed and distributed to teachers for first login.
```

**What this creates:** `teachers` records + Supabase Auth users for each teacher (linked to school).

#### Step 6 — Student Upload

```
UI: CSV/Excel upload with visual preview.

Download Template:
  Required columns: name, roll_number, date_of_birth, gender, class, section, parent_name, parent_mobile
  Optional columns: admission_number, photo_url, address, blood_group, medical_notes

Upload Flow:
  1. Drop CSV file or browse
  2. System shows preview table: first 10 rows
  3. Column mapping step: "Does column 'Roll No' match 'roll_number'?" → auto-mapped, admin confirms
  4. Validation check: 
     — Duplicate roll numbers flagged in red
     — Unknown class/section values flagged in orange (must match Step 3 data exactly)
     — Missing required fields shown per row
  5. Admin reviews errors and either fixes CSV or overrides
  6. "Upload 347 students" confirmation button
  7. Progress indicator as rows are written to database
  8. Summary: "347 students uploaded. 3 skipped due to errors. Download error report."
  
Parent accounts:
  System automatically creates parent accounts for every unique parent_mobile found.
  If parent_mobile already registered → child is linked to existing account.
  New parents receive SMS: "Your child [Name] has been enrolled on VidyaSetu at [School Name]. 
  Download the app and enter your mobile number to access their academic records."

Photo Upload:
  After CSV upload, optional bulk photo upload:
  ZIP file of photos named as roll_number.jpg (e.g., 23.jpg for roll number 23)
  System matches and uploads. Unmatched photos shown for manual linking.
```

**Onboarding Complete Screen:**
```
Celebratory moment. Not a generic "success" screen.
  
Visual: The VidyaSetu bridge mark animates with a warm glow
Headline: "VidyaSetu is ready for [School Name]"
Stats pill row:
  [X Classes] [X Teachers] [X Students] [X Parents notified]
  
Two CTAs:
  Primary (Arctic Blue): "Go to Dashboard"
  Secondary (ghost): "Invite Teachers" (opens share sheet with credential PDF)
```

---

## 9. Administration Portal

> **Portal color mode: DARK** — Void Black backgrounds throughout.

### 9.1 Admin Bottom Navigation

```
5 tabs in the bottom navigation bar:

[🏠 Home] [👥 People] [📋 Records] [📣 Comms] [⚙ Settings]

Active state: Arctic Blue icon + Arctic Blue label
Inactive state: Cloud White at 40%

A floating notification badge on each tab shows count of pending actions.
```

### 9.2 Admin Home — Dashboard

**Purpose:** One-screen health check of the entire school. No scroll required for critical data.

```
TOP SECTION — Header Bar:
  Left: School name (truncated) + School logo (small circle)
  Center: Today's date + day count in academic year ("Day 47 of 220")
  Right: Notification bell (with unread count) + Admin avatar

SECTION 1 — Today at a Glance (horizontal scroll cards):
  Card 1 — Attendance Today:
    Large number: "87%" with trend arrow (vs yesterday)
    Sub: "274 present / 316 total"
    Color ring chart (donut): present (green) / absent (coral) / not-marked-yet (amber)
    CTA: "View Details →"
  
  Card 2 — Pending from Teachers:
    Number: "3 teachers haven't marked attendance"
    List: Teacher names in compact chips (up to 3 shown, "+2 more")
    CTA: "Send Reminder →"
  
  Card 3 — Fee Collection Today:
    Amount: "₹ 24,500 collected today"
    Sub: "₹ 2,18,400 outstanding"
    Bar: small progress bar of this month's target
    CTA: "Fee Dashboard →"
  
  Card 4 — Upcoming Events:
    Next event: "PTM — Class 10 | Tomorrow, 10 AM"
    CTA: "View Calendar →"

SECTION 2 — Class-Wise Attendance Matrix:
  Title: "Attendance by Class — Today"
  A compact grid (not a table, a visual grid):
    Each cell = one class-section, colored by attendance %:
      80%+ → Arctic Blue tint
      60-80% → Amber tint  
      Below 60% → Coral tint
    Tap a cell → drills into that class's full attendance list

SECTION 3 — Academic Progress Overview:
  Title: "Syllabus Coverage — This Month"
  Horizontal bar chart per class (DM Mono numbers):
    Class 9-A: ████████░░ 78%
    Class 9-B: ██████████ 94%
    Class 10-A: ██████░░░░ 61%  ← shown in amber (behind schedule)
  "Behind schedule" classes get a warning icon
  CTA: "Full Report →"

SECTION 4 — Teacher Activity (Admin only, never shown to parents):
  Mini list: last 3 teacher updates with timestamp
  "Dr. Sharma updated Class 10-A Chemistry syllabus — 2 hours ago"
  CTA: "Teacher Dashboard →"

SECTION 5 — Pending Actions Queue:
  Actionable cards for things requiring admin's attention:
  — "2 parent messages awaiting reply (>24 hrs)"
  — "Fee reminder batch due to run tomorrow"
  — "Exam timetable not uploaded for Class 12"
  Each card has a quick action button.
```

### 9.3 People Screen — Students & Teachers

```
Two tab switcher at top: [Students] [Teachers]

STUDENTS TAB:
  Search bar (prominent, not an afterthought)
  Filter row: [All Classes ▾] [All Sections ▾] [Status ▾]
  
  Student list items:
    Left: Student photo (circle, 40px) or initials avatar if no photo
    Center: Name (Body weight), Roll No (Caption, DM Mono), Class-Section (Label)
    Right: PEWS status dot (green/amber/red — Coming Soon) + attendance % (DM Mono)
    Tap → Student Detail Screen
  
  FAB (+): "Add Student" (individual add for mid-year admissions)

TEACHERS TAB:
  Teacher list items:
    Left: Teacher photo or initials
    Center: Name, Subject(s) (up to 2 shown + "and X more"), Classes
    Right: Last activity timestamp
    Status dot: green (active recently) / grey (inactive >7 days)
    Tap → Teacher Detail Screen
  
  FAB (+): "Add Teacher"

STUDENT DETAIL SCREEN (full screen, not modal):
  Header: Student photo (80px), Name, Class-Section, Roll No, Admission No
  Quick stats row: Attendance %, Last marks %, Outstanding fees (if any)
  
  Tabs: [Overview] [Attendance] [Marks] [Fees] [Notes]
  
  Overview tab:
    Parent info (name, mobile, linked status — verified/unverified)
    Personal details (DOB, gender, blood group, medical notes)
    Academic year summary
  
  Attendance tab:
    Monthly calendar heatmap
    DM Mono percentage per month
    "3 consecutive absences" alert if present
  
  Marks tab:
    Subject-wise bar chart
    All assessments in descending date order
    Class rank (if enabled by admin)
  
  Fees tab:
    Outstanding amount (large, coral if overdue)
    Full payment history (date, amount, receipt link)
    "Send Reminder" button
  
  Notes tab:
    Internal notes visible only to admin (medical, behavioral, special needs)
    Timestamped, author-tagged

TEACHER DETAIL SCREEN:
  Header: Teacher photo, Name, Employee ID, Subjects/Classes
  
  Tabs: [Profile] [Activity] [Classes]
  
  Activity tab (admin only — never accessible to teacher about themselves):
    "Update Frequency" — chart showing days with at least one update (past 30 days)
    Last 10 syllabus updates with timestamp
    "Class Average Trend" per subject they teach (aggregate, not per student)
  
  Destructive Actions (at bottom, behind confirmation):
    "Edit Credentials" — resets teacher's login
    "Deactivate Account" — removes login access, preserves all historical data
```

### 9.4 Records Screen

```
Sub-navigation (top horizontal tabs):
  [Attendance] [Marks] [Syllabus] [Fee] [Documents]

ATTENDANCE TAB:
  Date picker (defaults to today)
  Class-section filter
  Full class roster with present/absent/late toggles
  Bulk actions: "Mark All Present" quick toggle
  Export button: CSV of attendance for date range

MARKS TAB:
  Filter: Class → Section → Subject → Assessment Type
  Assessment types: [Unit Test] [Mid-Term] [Annual] [Practical] [Project] [Other]
  "Add Assessment" — creates a new marks entry event
    — Assessment name, date, max marks
    — Then fill marks per student (tabular entry, keyboard-optimised)
  Class statistics: average, median, highest, lowest (computed live as marks entered)
  Export: CSV / PDF report

SYLLABUS TAB:
  Class → Subject navigator
  Shows the chapter/topic list for that subject
  Teacher-entered updates appear here with date and teacher name
  Admin can see coverage % vs. academic calendar target
  "Chapters not yet started" shown in amber if behind schedule

FEE TAB:
  "Fee Structure" sub-tab: Define fee heads, amounts, due dates per class
  "Collections" sub-tab: All payments, outstanding, overdue
  "Send Reminders" bulk action: select students with dues, send WhatsApp reminder
  Export: Full fee report as Excel

DOCUMENTS TAB:
  Upload school documents:
  — Circular / Notice (date, recipients: all / specific class / all parents)
  — Exam Timetable (PDF upload)
  — Holiday List (structured date picker entry, not PDF)
  — Report Cards (uploaded per term — Coming Soon: auto-generated)
  — School Calendar
  All documents are timestamped, versioned (new upload replaces old but old is archived)
```

### 9.5 Communications Screen

```
Sub-navigation: [Announcements] [Messages] [PTM] [Notifications]

ANNOUNCEMENTS TAB:
  Compose button (prominent, top right)
  Compose flow:
    Title (required)
    Message body (rich text: bold, bullet list, link)
    Recipients: [All School] [All Parents] [All Teachers] [Specific Class] [Specific Section]
    Delivery channels: [App Push] [WhatsApp] — both selected by default
    Schedule: Send now / Schedule for later (date-time picker)
    Preview before send
  
  Posted announcements list with delivery stats:
    "Parent-Teacher Meeting Notice — Sent 3 days ago — 234/316 parents opened"

MESSAGES TAB:
  Inbox of parent-initiated messages
  Sorted by: Unanswered first → then by recency
  Each message shows: Parent name, child name, preview, timestamp
  "Overdue" badge (red) on messages older than 24 hours without reply
  Admin can reassign a message to a specific teacher
  Reply opens a thread view

PTM TAB (Parent-Teacher Meeting):
  "Schedule PTM" button → form:
    PTM Date, Time slots (define 15/20/30 min slots)
    Classes involved
    Mode: Physical / Online (video link)
  
  After creation → send announcement to parents → they book slots via app
  Admin sees: Slot booking grid (who booked which slot)
  Coming Soon badge: Online PTM video integration

NOTIFICATIONS TAB:
  History of all automated notifications sent (attendance alerts, fee reminders, marks updates)
  Delivery status per notification (sent, delivered, read)
  Filter by type and date range
```

### 9.6 Settings Screen

```
Sections:

SCHOOL PROFILE:
  Edit all Step 1 onboarding data
  School logo management
  
ACADEMIC YEAR:
  Current year display
  "Start New Academic Year" — initiates next year setup (classes, sections, promote students)
  Historical years (read-only access)

CLASSES & SUBJECTS:
  Review all classes/sections/subjects
  Add new class or section mid-year
  Add new subject mid-year
  NOTE: Cannot delete a class/section/subject that has student data attached

TEACHER MANAGEMENT:
  (Same as People → Teachers but with admin-specific controls)
  "Regenerate Credentials" for a teacher
  "Download All Credentials PDF"

FEE STRUCTURE:
  Edit fee heads and amounts (for next billing cycle — cannot retroactively change)

NOTIFICATIONS SETTINGS:
  Toggle which events trigger WhatsApp vs. app push vs. both
  Time restrictions: "No notifications between 9 PM and 7 AM"

DATA EXPORT:
  Full school data export (UDISE format — Coming Soon)
  Student records CSV
  Attendance summary PDF
  Marks report PDF

ACCOUNT:
  Change admin email/password
  Add co-admin (second admin account for same school — Coming Soon)
  "Contact VidyaSetu Support"
```

---

## 10. Teacher Portal

> **Portal color mode: DARK** — Void Black backgrounds throughout.

### 10.1 Teacher Bottom Navigation

```
4 tabs:
[🏠 Home] [📝 Update] [👥 My Classes] [👤 Profile]
```

### 10.2 Teacher Home — Dashboard

```
HEADER:
  "Good morning, [First Name]" (time-aware greeting — morning/afternoon/evening)
  Today's date
  Notification bell

TODAY'S TASKS (the most important section):
  Designed like a task list — teachers need to know exactly what they need to do today.
  
  Card 1 — Attendance:
    Status indicator:
      If today's attendance marked → Green check + "Marked at 9:12 AM — 28/32 present"
      If not marked → Amber warning + "Attendance not marked yet"
    CTA: "Mark Now →" (primary, Arctic Blue) or "View Details →" (if already marked)
  
  Card 2 — Syllabus Update:
    "Have you updated today's syllabus?"
    Shows last updated subject + what was covered
    CTA: "Update Now →"
  
  Card 3 — Upcoming Tests:
    "Class 10-A Chemistry Unit Test — in 3 days"
    CTA: "View →"
  
  Card 4 — Pending Homework Checks:
    "4 students haven't submitted yesterday's assignment"
    CTA: "View →"

MY CLASSES TODAY (timetable strip):
  Horizontal scroll of periods:
    Each period card: Period number, Subject, Class-Section, Time
    Current period highlighted in Arctic Blue
    Past periods greyed out
    "Free Period" shown distinctly

RECENT ACTIVITY FEED:
  Last 5 actions the teacher took (attendance marked, marks entered, syllabus updated)
  With timestamps
```

### 10.3 Update Screen (Core Teaching Actions)

This is the "action hub" for teachers — where they spend most of their time.

```
Sub-navigation: [Attendance] [Marks] [Syllabus] [Homework]

ATTENDANCE SUB-SCREEN:
  Step 1: Select class-section (if teacher teaches multiple — shows their options only)
  Step 2: Select date (defaults to today — cannot mark for future dates)
  
  Student roster:
    Each student: Photo, Name, Roll No
    Right side: 3 tap targets:
      [✓ Present] [✗ Absent] [⏰ Late]
    Visual state: Present = Arctic tint, Absent = Coral tint, Late = Amber tint
    Default: all unset (not defaulted to Present — forces conscious action)
  
  "Select All Present" quick action at top
  
  Bottom bar:
    Count: "28 marked, 4 remaining"
    "Submit" CTA (disabled until all students marked)
  
  After submit: 
    Summary screen: "Attendance for Class 9-A marked — 28 present, 3 absent, 1 late"
    "View absent list" → shows absent students (these trigger parent notifications)
    Cannot edit after submission — shows "Request Correction" link instead

MARKS SUB-SCREEN:
  Step 1: Select class-section
  Step 2: Select subject
  Step 3: Select assessment (from existing assessments) OR "Create New Assessment"
    Create Assessment form:
      Name: "Unit Test 2 — Organic Chemistry"
      Date: (date picker)
      Maximum Marks: (number input)
      Assessment Type: [Unit Test] [Mid-Term] [Annual] [Practical] [Assignment]
  Step 4: Enter marks
    Student roster with marks input:
      Each student: Photo, Name, Roll No | Marks input (DM Mono keyboard)
      Class average shows live as marks are entered (small floating bar)
      "AB" button for absent students (marks them as absent for this assessment)
  
  Submit → confirmation with class stats summary

SYLLABUS SUB-SCREEN:
  Step 1: Select class-section
  Step 2: Select subject
  
  Shows: Subject chapter list (from board syllabus mapping if available, else freeform)
  For each chapter: completion status + dates when covered
  
  "Log Today's Progress" CTA:
    Chapter: (select from list or type)
    Topics Covered: (text input with suggestions from chapter topic list)
    Homework Given: (toggle + description if yes)
    Teaching Note (optional — visible to admin only): "Class struggled with topic X"
  
  After logging: Parent-facing notification drafted:
    "Class 9-A covered [Topics] in [Subject] today."
    Teacher can preview and edit before it's sent

HOMEWORK SUB-SCREEN:
  Active assignments list (what the teacher has assigned, not completed yet)
  Each assignment: Subject, Description, Due Date, Submission count vs. class strength
  
  "Assign Homework":
    Class-Section, Subject
    Description (rich text)
    Due Date
    Type: [Written] [Reading] [Project] [Revision]
  
  "Mark Submissions": update who submitted (Coming Soon: parent confirmation flow)
```

### 10.4 My Classes Screen

```
Shows all class-sections assigned to this teacher.
Each class card:
  Class name + section
  Subjects taught in this class (chips)
  Students count
  Attendance today (quick percentage)
  
Tap a class card → Class Detail:
  Full student roster for that class
  (Teacher sees: Name, Roll, Photo, Attendance %, Last marks for their subject)
  (Teacher CANNOT see marks entered by other teachers for the same students)
  
  "Message Class" — send announcement to all parents of this class (Coming Soon)
```

### 10.5 Teacher Profile Screen

```
Personal info display (not editable except profile photo and mobile number)
Subjects Assigned display
Login Credentials display (username shown, password masked)
"Change Password"
"Notification Preferences"
Logout
```

---

## 11. Parent Portal

> **Portal color mode: LIGHT** — Cloud White backgrounds throughout.
> This is the most emotionally important portal. Every design decision must feel warm, trustworthy, and human — not clinical or data-heavy.

### 11.1 Parent Bottom Navigation

```
4 tabs:
[🏠 Home] [📊 Academics] [💰 Fees] [🔔 Activity]

Navigation bar: White background, 1px top border in Void at 8% opacity
```

### 11.2 Child Profile Switcher

```
APPEARS AT THE TOP OF EVERY SCREEN (not just Home).

If only one child: Shows child's name + class + school name + small photo. Tap → child profile screen.

If multiple children: Shows the active child's name/photo + a small dropdown arrow.
  Tap → dropdown shows all children:
    Each: Photo, Name, Class, School name
    Active child: Arctic Blue check mark
    Tap to switch → all data on current screen updates to selected child

Visual design: Looks like a premium app switcher — NOT a generic dropdown.
  It's a small horizontal pill component that sits at the top of the screen.
  Name in Body weight, class in Caption, school in Caption.
```

### 11.3 Parent Home — Dashboard

```
CHILD HERO SECTION:
  Large (not tiny) display of today's status for the active child
  Child photo (80px circle, Cloud White border 3px)
  Child name (Title weight)
  Class-Section + School name (Caption)
  
  STATUS PILL ROW:
    [✓ Present Today] or [✗ Absent Today] or [⏰ Late Today] or [--Holiday]
    Color-coded. If absent: gentle warm coral. Not alarming, but clear.

TODAY'S SUMMARY CARDS (vertical scroll, not horizontal):
  
  Card 1 — Classes Today:
    "Today's Schedule"
    Mini timetable: Subject, Period, Teacher name
    Currently ongoing subject highlighted in Arctic Blue
    
  Card 2 — Recent Academic Update:
    "What was covered today"
    Last syllabus update from teacher: "[Subject] — [Topics covered]"
    Timestamp: "2 hours ago"
    If no update today: "No updates yet — check back later"
    
  Card 3 — Upcoming Test Alert (if any):
    "Remind [Child Name] about this:"
    "Mathematics Unit Test — Day after tomorrow"
    Chapters to be covered (from syllabus data)
    
  Card 4 — Fee Status:
    "All fees paid" (Arctic tint) OR "₹X due on [date]" (Amber tint) OR "₹X overdue" (Coral tint)
    CTA: "Pay Now" (if applicable — Coming Soon: Razorpay) or "View Details"
    
  Card 5 — Messages:
    "Unread message from [Teacher/School]" if any
    OR "No new messages"

ATTENDANCE QUICK LOOK:
  This month's attendance: large percentage number (DM Mono, Display size)
  Small bar of present days (Arctic) vs absent (Coral) vs holidays (Cloud grey)
  "[X] days present this month out of [Y] school days"
```

### 11.4 Academics Screen

```
Sub-navigation: [Overview] [Attendance] [Marks] [Syllabus] [Report]

OVERVIEW TAB:
  Subject-wise performance snapshot:
    Each subject: name, last marks %, trend arrow (up/down vs last assessment)
    A small bar chart per subject (last 3 assessments)
  
  Overall standing (class rank — shown only if admin has enabled this)

ATTENDANCE TAB:
  Month navigator (< Previous | Current Month | Next >)
  Calendar view:
    Each day color-coded:
      Dark Arctic circle: Present
      Coral circle: Absent
      Amber circle: Late
      Empty circle with faint border: Holiday or weekend
      Grey: Future
  
  Monthly stats below calendar:
    Present: 21 | Absent: 2 | Late: 1 | Holidays: 4
    "Attendance this month: 91%" (DM Mono, Title size)
  
  PEWS alert (if applicable — Coming Soon):
    If child hits 3 consecutive absences, a gentle but clear alert card appears:
    "We've noticed [Name] has been absent 3 days in a row. Is everything okay?"
    Action: "Talk to School" or "Mark as Medical Leave"

MARKS TAB:
  Subject filter (horizontal scroll chips)
  All assessments in reverse chronological order:
    Assessment name, date, marks obtained / total, class average
    "You scored 72 — Class average was 68" (contextualises the number)
  
  Performance trend chart (line chart per subject — last 6 assessments)
    Two lines: Child's marks (Arctic Blue) vs Class average (Cloud Grey)
    Clean chart, no grid clutter, smooth curves

SYLLABUS TAB:
  Select subject → see chapter-by-chapter coverage log
  Shows: Chapter name, topics covered, date covered, homework set
  "Coming Up Next" section: next chapter based on syllabus sequence
  This helps parents know what to ask their child about

REPORT TAB:
  Term-end AI narrative report card (Coming Soon):
    Show "Coming Soon" placeholder that explains what this will show
    "At the end of each term, VidyaSetu will generate a personalised academic summary for [Name]."
  
  Previous report cards (uploaded by school as PDFs):
    If admin has uploaded: show PDF thumbnail with download button
```

### 11.5 Fees Screen

```
BALANCE SUMMARY (top card, prominent):
  If paid up: "All dues cleared ✓" — Arctic tint background
  If due: "₹[amount] due by [date]" — Amber tint
  If overdue: "₹[amount] overdue" — Coral tint

FEE HEADS BREAKDOWN:
  Each fee category:
    Name: Tuition Fee / Transport / Lab Fee / Activity Fee
    Amount: DM Mono
    Status: Paid / Due / Overdue
    Due Date

PAYMENT HISTORY:
  Reverse chronological list of payments
  Each: Date, Amount, Fee Head, Receipt button (download PDF)
  DM Mono amounts throughout

PAY NOW BUTTON (Coming Soon — Razorpay integration):
  Shows a "Coming Soon" label currently
  When ready: Full payment flow within app — select fee heads, choose UPI/card/netbanking

SEND MESSAGE TO SCHOOL re: Fees:
  Small link at bottom: "Have a question about your fees? Message the school →"
```

### 11.6 Activity Screen (Notifications & Feed)

```
Full history of everything related to this child — the parent's "receipt" of school life.

FILTER ROW:
  [All] [Attendance] [Academic] [Fees] [Announcements] [Messages]

NOTIFICATION FEED:
  Each item:
    Icon (Phosphor icon matching category, 20px)
    Title: Short, human. "Priya was marked present today" not "ATTENDANCE_STATUS: PRESENT"
    Body: One line of detail
    Timestamp (relative: "2 hours ago" / "Yesterday 3:40 PM" / "Jun 3" for older)
    
  Categories styled distinctly but not garishly:
    Attendance: Slight Arctic tint on icon background
    Marks: Slight amber tint on icon background
    Fees: Slight coral tint if overdue, else Arctic tint
    Announcement: Slight Void tint (school message = authority = dark = serious)

MESSAGES (inline, not separate screen):
  School-initiated messages appear in this feed tagged "From School"
  Parent replies inline:
    "Reply to school" → expands a text input
    "Send" → message goes to admin's inbox in the admin portal
  Thread collapses back to a single "Conversation" card after reply
```

---

## 12. School Discovery Marketplace

> **Portal color mode: LIGHT** — This is the "market" section, accessible to Parents who haven't linked a child yet, or as a secondary screen for existing parents.

```
ACCESS: 
  Unregistered parents: This IS their onboarding screen after mobile verification
  Registered parents: Accessible via "Discover Schools" in home screen or settings
```

### 12.1 Discovery Home

```
SEARCH BAR (hero, prominent):
  "Find schools near you or by name"
  Tap → expands with keyboard + filters

LOCATION-BASED RESULTS:
  Requires location permission (ask with clear explanation: "To find schools near you")
  If denied: manual city/area search

FILTER BAR (horizontal scroll below search):
  [📍 Within 3 km ▾] [🎓 CBSE ▾] [💰 Fee Range ▾] [🏫 Type ▾] [⭐ SRI ▾]

SCHOOL CARDS (vertical list):
  Each card:
    School photo/facade (real photo, not generic icon — schools prompted to upload)
    School name (Title)
    Board badge + Type badge (chips)
    SRI Score (Coming Soon): "⭐ 8.4" in Arctic
    Distance: "2.3 km" (if location enabled)
    Fee range: "₹ 25,000 – ₹ 45,000 / year"
    Quick chips: "English Medium" | "Co-ed" | "CBSE Board Result: 94%"
    
    Two actions (always visible on card, no long press required):
      "Compare" (ghost button) — adds to comparison tray (max 3)
      "Enquire" (Arctic fill) — sends admission inquiry

COMPARE TRAY:
  Fixed at bottom when comparison schools are selected:
    Mini thumbnails of selected schools (2-3)
    "Compare Now" CTA
    "×" to remove a school
```

### 12.2 School Profile Page

```
HERO SECTION:
  School photo carousel (if multiple uploaded)
  School name, board, type, city
  SRI score (Coming Soon) — prominent, with tooltip: "What is this score?"
  Distance from user's location
  
  Action row:
    [❤️ Save] [↗ Share] [📋 Compare]
    [📞 Enquire Now] ← Primary CTA, Arctic Blue, full width below

SECTIONS (vertical scroll):
  About School:
    Founded year, description (as entered by school admin)
    Facilities chips: [Smart Classrooms] [Library] [Sports Ground] [Computer Lab] [Canteen] etc.
    
  Academics:
    Board: CBSE/ICSE/State
    Classes offered: KG–12 (or subset)
    Medium: English / Hindi / Both
    Board Result % (last year — shown if school has entered, else hidden)
    Teacher-Student Ratio (Coming Soon — from VidyaSetu data)
    
  Fee Structure:
    Table: Fee Head / Amount / Frequency
    (Shown only if school has made fees public — they choose this in settings)
    
  SRI Score Breakdown (Coming Soon):
    Expandable section: "How this score is calculated"
    Shows each of the 11 SRI signals with icons and values
    
  Parent Reviews:
    Verified reviews from parents whose children are enrolled
    Rating categories: Academics / Safety / Communication / Facilities
    Star ratings + written reviews
    "Only verified VidyaSetu parents can review" trust badge
    
  Location:
    Map thumbnail (static, tap to open in Google Maps)
    Full address
    Nearby landmarks
    
  About Admission:
    Age criteria (if entered)
    Registration form process (as entered by school)
    Contact: admission officer name + direct mobile (if school has entered)

ENQUIRY FLOW:
  Bottom sheet slides up:
    Parent name (pre-filled if logged in)
    Child name, current class, desired class for admission
    Message (optional)
    "Submit Enquiry" → sent to school's admin portal admission CRM
    Parent gets WhatsApp confirmation with school's response timeline
```

### 12.3 School Comparison Screen

```
Up to 3 schools side by side.
Table layout with schools as columns, parameters as rows.

Parameters compared:
  Board
  Type
  Medium
  Fee Range
  SRI Score (Coming Soon)
  Distance
  Facilities (checkmarks)
  Board Results %
  Teacher-Student Ratio
  Reviews (average star rating)
  
"Best value" cell highlighted with Arctic tint where one school clearly wins.

"Enquire All" button — sends inquiry to all compared schools simultaneously.
"Remove" button per school column.
"Add another school" if fewer than 3 selected.
```

---

## 13. Cross-Portal Shared Screens

These screens appear across multiple portals with role-appropriate data.

### 13.1 Announcement / Notice Screen

```
School announcements seen by:
  — Admin: Full list, with compose button, delivery analytics
  — Teacher: List filtered to "for all teachers" + "for their classes"
  — Parent: List filtered to their child's class + school-wide

Announcement detail:
  Title (Title weight)
  Date posted (Caption)
  Posted by: "School Administration" or "Class 9-A Teachers"
  Body (Body weight — supports basic formatting)
  Attached document (PDF thumbnail if any, tap to view)
```

### 13.2 Academic Calendar

```
Shared calendar visible to all roles (with appropriate data per role):
  Admin sees: All events + ability to add
  Teacher sees: All events + ability to see their class's exam schedule
  Parent sees: Their child's class events + school-wide events

Visual: Monthly calendar with event dots per day.
  Tap a day → list of events for that day
  Events color-coded by type:
    Arctic: Academic (exam, PTM)
    Amber: Holiday
    Cloud White: School event (sports day, function)
    Coral: Important deadline (fee due, form submission)
```

### 13.3 In-App Notification Tray

```
Universal across all portals.
Bell icon in top bar with count badge.

Notification items:
  Icon (category-matched Phosphor icon)
  Title + one-line description
  Timestamp (relative)
  Unread indicator: thin Arctic left border
  Tap → goes to relevant screen
  
"Mark all read" at top
Empty state: Not a generic "no notifications" — instead:
  "You're all caught up ✓" with a small illustration
```

---

## 14. Navigation Architecture

### 14.1 Global Navigation Rules

```
THREE NAVIGATION PATTERNS IN USE (never mix within a portal):

1. BOTTOM NAVIGATION (primary navigation between main sections)
   — Fixed, always visible
   — 4-5 tabs maximum
   — No labels on inactive tabs (icon only, saves space)
   — Active tab: icon + label visible

2. TOP NAVIGATION / HEADER (contextual sub-navigation within a section)
   — Horizontal scrollable tabs with underline indicator
   — Used for: Records sub-sections, Academics sub-sections, etc.

3. MODAL / BOTTOM SHEET (secondary flows that don't need full screen)
   — Compose announcement
   — Add student
   — Quick filter selection
   — NEVER for flows with more than 3 steps (use full screen for those)
```

### 14.2 Navigation Flow Diagrams

**Admin navigation:**
```
[Login] → [Admin Dashboard]
  │
  ├── People Tab
  │     ├── Student List → Student Detail → [Attendance] [Marks] [Fees] [Notes]
  │     └── Teacher List → Teacher Detail → [Profile] [Activity] [Classes]
  │
  ├── Records Tab
  │     ├── Attendance → Mark/View → Class Attendance Detail
  │     ├── Marks → Select Assessment → Enter Marks → Summary
  │     ├── Syllabus → Class-Subject → Log Entry
  │     ├── Fee → Structure/Collections → Individual Student Fee
  │     └── Documents → Upload/View
  │
  ├── Communications Tab
  │     ├── Announcements → Compose/View
  │     ├── Messages → Thread View → Reply
  │     ├── PTM → Schedule/View/Manage
  │     └── Notifications → History
  │
  └── Settings Tab
        ├── School Profile
        ├── Academic Year
        ├── Classes & Subjects
        ├── Teacher Management
        └── Account
```

**Teacher navigation:**
```
[Login] → [Teacher Dashboard]
  │
  ├── Update Tab
  │     ├── Attendance → Select Class → Mark → Submit
  │     ├── Marks → Select Class/Subject/Assessment → Enter → Submit
  │     ├── Syllabus → Select Class/Subject → Log → Preview Notification
  │     └── Homework → Assign / Mark Submissions
  │
  ├── My Classes Tab
  │     └── Class List → Class Detail → Student List
  │
  └── Profile Tab
```

**Parent navigation:**
```
[Login / Register] → [Link Child] → [Parent Dashboard]
  │
  ├── [Child Switcher — always at top]
  │
  ├── Home Tab
  │     └── Today's summary → detail screens as needed
  │
  ├── Academics Tab
  │     ├── Overview
  │     ├── Attendance → Monthly calendar
  │     ├── Marks → Subject detail → Chart
  │     ├── Syllabus → Subject → Chapter list
  │     └── Report → AI report (Coming Soon) / PDF view
  │
  ├── Fees Tab
  │     └── Balance → Payment history → Pay (Coming Soon)
  │
  └── Activity Tab (Notification Feed + Messages)
```

### 14.3 Deep Link Structure

All major screens must have a deep link for WhatsApp notification CTA buttons:
```
vidyasetu://parent/attendance/{child_id}/{date}
vidyasetu://parent/marks/{child_id}/{assessment_id}
vidyasetu://parent/fee/{child_id}
vidyasetu://admin/teacher-activity/{teacher_id}
vidyasetu://teacher/attendance/{class_id}/{date}
```

---

## 15. Coming Soon Features

Every "Coming Soon" screen must:
1. Show a polished placeholder — not a blank screen or "Work in Progress"
2. Describe WHAT the feature will do (generate excitement)
3. Show a realistic preview (a mockup of what the data will look like)
4. Have a "Notify me when this is ready" toggle

| Feature | Portal | Coming Soon Reason |
|---------|--------|-------------------|
| SRI Score (School Reputation Index) | Admin + Parent Discovery | ML algorithm not built yet |
| PEWS (Predictive Early Warning) | Admin + Parent | Risk model not trained |
| AI Narrative Report Cards | Parent | Gemini API integration pending |
| Razorpay Fee Payment | Parent | Payment gateway integration pending |
| WhatsApp Bot (Teacher flows) | Teacher | Bot backend needs audit |
| Online PTM (Video) | Admin + Parent | Video API integration pending |
| Compliance Reports (UDISE) | Admin | Regulatory format mapping pending |
| Class Rank (per student) | Parent + Admin | Configuration option, needs admin setting |
| Co-admin account | Admin | Multi-admin support pending |
| iOS App | All | Android first |
| Alumni Tracking | Admin | Long-term feature |
| Advanced Analytics Dashboard | Admin | PostHog integration pending |
| Parent-to-Parent Community | Parent | Phase 2 feature |

---

## 16. Information Flow Matrix

This table defines exactly what each role sees about each data type.

| Data Type | School Admin Sees | Teacher Sees | Parent Sees |
|-----------|:-----------------:|:------------:|:-----------:|
| All students in school | ✅ Full details | ✅ Only their classes | ❌ |
| Student personal details | ✅ Full | ✅ Name, roll, photo only | ✅ Own child only |
| Attendance — all classes | ✅ | ✅ Own classes | ✅ Own child |
| Attendance — other class | ✅ | ❌ | ❌ |
| Marks — all subjects | ✅ | ✅ Own subjects | ✅ Own child, all subjects |
| Marks — other teacher's | ✅ | ❌ | ✅ Own child |
| Syllabus updates | ✅ | ✅ Own subjects | ✅ Own child's subjects |
| Fee details (all students) | ✅ | ❌ | ✅ Own child |
| Teacher activity/performance | ✅ | ❌ (own only) | ❌ |
| PEWS risk score | ✅ | ✅ Own classes | ✅ Own child (text only, no number) |
| SRI Score | ✅ | ❌ | ✅ (in discovery) |
| Parent contact details | ✅ | ❌ | N/A |
| School's fee structure | ✅ | ❌ | ✅ (own child's) |
| Internal admin notes | ✅ | ❌ | ❌ |
| Teacher notes on students | ✅ | ✅ Own notes | ❌ |

---

## 17. Component Library

These are the reusable components that must be built first. Every screen is built from these.

### 17.1 Core Components

```
ATOMS (cannot be broken down further):
  — VText: Typography wrapper with size/weight/color props
  — VIcon: Phosphor icon wrapper with size/color props
  — VDivider: 1px divider with opacity variants
  — VBadge: Status pill (text + optional icon, semantic color variants)
  — VTag: Chip for category/filter labels
  — VAvatar: Circular image or initials fallback

MOLECULES:
  — VCard: Surface container with border/shadow variants
  — VInput: Text field with label, error, helper text, icon prefix/suffix
  — VButton: CTA with primary/secondary/ghost/destructive variants
  — VSearchBar: Search input with clear button and filter trigger
  — VListItem: Row item with left content, center content, right content slots
  — VStatusDot: Colored dot with optional pulse animation (for live states)
  — VProgressBar: Linear progress with label and percentage
  — VProgressRing: Circular donut for percentage display
  — VDataChip: A chip specifically for DM Mono data values

ORGANISMS:
  — VStudentCard: Student list item (photo/initials, name, roll, class, status)
  — VTeacherCard: Teacher list item (photo, name, subjects, last active)
  — VAttendanceCell: A single day cell for calendar view
  — VSubjectPerformanceBar: Horizontal bar for marks overview
  — VFeeStatusCard: Fee balance display with payment CTA
  — VAnnouncementCard: Notice/circular card
  — VNotificationItem: Notification feed item
  — VSchoolCard: Discovery marketplace school card
  — VClassGrid: The attendance/performance grid of class sections
  — VComingSoon: Standardised placeholder for upcoming features

CHARTS (use MPAndroidChart for Android, Recharts for web):
  — VLineChart: Performance trends (Arctic + Cloud lines, Void/Cloud background)
  — VBarChart: Syllabus coverage, marks comparison
  — VDonutChart: Attendance split (present/absent/late)
  — VHeatmapCalendar: Monthly attendance calendar
```

### 17.2 Navigation Components

```
  — VBottomNav: Bottom navigation bar (dark/light variant per portal)
  — VTopTabs: Horizontal scrollable tab bar
  — VBackHeader: Screen header with back button, title, optional action
  — VChildSwitcher: Child profile pill with switcher dropdown (Parent portal)
  — VPortalSelector: Three-tab selector on login screen
  — VProgressStepper: Onboarding wizard step indicator
```

### 17.3 Empty States

Every list/data screen needs a designed empty state. Never show a blank screen.

```
Standard empty states:
  — No students yet: "No students uploaded yet. Upload your first batch."
  — No attendance data: "Attendance for this date hasn't been marked yet."
  — No marks: "No assessments have been entered for this subject."
  — No notifications: "You're all caught up" 
  — No messages: "No messages yet. Parents and teachers can reach you here."
  — No schools found: "No schools found near you. Try expanding your search area."

Each empty state:
  — A small, clean illustration (NOT AI-generated stock — use simple geometric / abstract SVG)
  — Headline (Title weight)
  — One-line description (Caption)
  — CTA if an action is possible
```

---

## 18. Rebuild Execution Order

> **This is the order to build in. Do not skip ahead. Each phase must be functionally complete before proceeding.**

### Phase 0 — Foundation (Do Before Any Screen)

```
1. Read and audit every backend endpoint. Document: LIVE / PARTIAL / COMING SOON.
2. Set up the design token file (colors, typography, spacing) as constants. 
   Nothing is hardcoded anywhere else.
3. Build the component library from Section 17 — atoms first, then molecules, then organisms.
4. Build the app logo (exact spec from Section 4).
5. Build the splash screen sequence (exact spec from Section 5).
6. Set up navigation architecture (bottom nav, routing) as empty shells.
   Each tab navigates to a placeholder that says what screen goes here.
7. Set up the theme engine: dark mode (Admin/Teacher) vs light mode (Parent) switching.
8. Confirm Supabase auth is wired correctly: all three roles log in and reach the right portal.
```

### Phase 1 — Authentication & Onboarding (Week 1)

```
1. Login screen with portal selector
2. Admin login flow (functional)
3. Teacher first-login + password change flow
4. Parent mobile + OTP registration
5. School onboarding wizard (all 6 steps, back-end integrated)
6. Parent child-linking flow
```

### Phase 2 — Admin Core (Weeks 2–3)

```
1. Admin Dashboard home (wire to real attendance data from backend)
2. People screen — Students list + Student detail
3. People screen — Teachers list + Teacher detail
4. Records — Attendance (view and mark)
5. Records — Marks entry
6. Records — Syllabus log
7. Settings — school profile, classes/subjects view
```

### Phase 3 — Teacher Portal (Week 3–4)

```
1. Teacher Dashboard home
2. Update — Attendance marking
3. Update — Marks entry
4. Update — Syllabus log
5. My Classes screen
```

### Phase 4 — Parent Portal (Weeks 4–5)

```
1. Parent Dashboard home (child switcher, today's summary)
2. Academics — all sub-tabs
3. Fees screen
4. Activity/notifications feed
5. Parent-teacher messaging (basic)
```

### Phase 5 — Communications & Discovery (Week 5–6)

```
1. Announcements (admin compose + all portals view)
2. PTM scheduling (basic, no video)
3. School Discovery — search, filter, school card list
4. School Profile page
5. Enquiry flow
6. School Comparison
```

### Phase 6 — Polish & Coming Soon (Week 6–7)

```
1. All "Coming Soon" placeholders implemented (per Section 15)
2. All empty states implemented (per Section 17.3)
3. Loading skeleton screens on all data-dependent screens
4. Deep link routing
5. Push notification routing (tap notification → correct screen)
6. Error states (network error, API error — NOT generic Android system dialogs)
7. End-to-end testing of all role-boundary rules (Section 6)
8. Performance audit: no Jetpack Compose recompositions without cause
```

---

## Appendix A — What "No Hardcoding" Means

Every piece of text that a user sees must come from one of:
1. A backend API response (student names, marks, fees)
2. A string resource file (UI labels, empty state copy, error messages)
3. A user preference (language setting)

Never:
- School name in code: `"St. Joseph's High School"` — must come from the `schools` table
- Student count: `"34 students"` — must be computed from `students` table where `class_id = x`
- Fee amount: `"₹2500"` — must come from `fee_structure` table
- Any lorem ipsum or placeholder names visible in production

---

## Appendix B — What "No AI-Generated Look" Means Practically

**Checklist before declaring any screen done:**

- [ ] No gradients with more than 2 stops and no "rainbow" gradients anywhere
- [ ] No glowing blob shapes in backgrounds
- [ ] No illustrations that look like undifferentiated flat vector packs (Storyset, Undraw in their generic form)
- [ ] No rounded everything — some elements (dividers, data tables) are sharp or lightly rounded
- [ ] No color that was not declared in Section 3 — no "let me add a little purple accent here"
- [ ] No font mixing — exactly the two fonts declared
- [ ] Icons from Phosphor ONLY. Not a mix.
- [ ] No card stacks with 3 depth layers and soft shadows — one card, one shadow rule
- [ ] No "hero section with big illustration + text on left" layout on every screen
- [ ] The data on screen actually means something — never simulated or random

---

## Appendix C — Testing Checkpoints

Before shipping each phase, test with these user journeys:

**Journey 1 — New school, zero data:**
New school admin logs in → completes onboarding → dashboard shows "0 students, 0 teachers" empty states gracefully, not broken.

**Journey 2 — Teacher, first day:**
Teacher receives credentials → logs in → changes password → sees their assigned classes → marks attendance for Class 9-A → 3 parents receive attendance notification.

**Journey 3 — Parent, multiple children:**
Parent with children in two different schools (both on VidyaSetu) → switches between child profiles → sees correct attendance, marks, and fee data for each child separately.

**Journey 4 — Role boundary:**
Log in as Teacher X (assigned to Class 9-A and 10-B) → attempt to access Class 11 data → must be blocked. Attempt to see Admin-only teacher performance data → must be blocked.

**Journey 5 — Data immutability:**
Admin marks attendance for a past date → teacher attempts to edit that record → edit is blocked → "Request Correction" option appears correctly.

**Journey 6 — Coming Soon screens:**
Navigate to every Coming Soon feature → each shows a proper placeholder, not a blank screen or error.

---

*VidyaSetu Frontend Rebuild Plan — v1.0*  
*Internal Engineering Document — Do Not Distribute*  
*Generated as part of full frontend replacement initiative*  
*Backend audit must precede all build phases*
