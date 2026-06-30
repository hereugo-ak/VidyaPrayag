# Vidya Prayag — Differentiating Feature Brainstorm

> **Goal:** Move beyond "another school ERP" into a category-defining product that parents, teachers, and admins genuinely love and can't switch away from.
>
> **Context:** The app already has solid ERP fundamentals (attendance, marks, homework, messaging, fees, analytics, school discovery). The features below are designed to create **emotional lock-in, network effects, and unique value** that no competitor offers in this combination.

---

## 1. AI-Powered Features

### 1.1 VidyaSetu AI — Personalized Learning Companion
**What:** An AI tutor that knows the child's syllabus, marks, attendance, and homework patterns. It generates:
- **Personalized practice questions** based on weak areas identified from marks data
- **Doubt resolution** — student/parent snaps a photo of a homework problem, AI explains step-by-step
- **Study schedule recommendations** — "Your child has a Science test on Friday and hasn't completed Chapter 3. Here's a 4-day plan."

**Why it stands out:** No Indian school ERP has this. Khanmigo (Khan Academy) and Google's Socratic exist but aren't integrated with school data. We have the syllabus + marks + homework data — that's the moat.

**Roles:** Parent (view), Student (interact), Teacher (oversight)
**Effort:** L (3-4 months)
**Competitor gap:** PowerSchool, Infinite Campus, ClassDojo — none have AI tutoring integrated with school data

**Data Readiness:**
- ✅ `AssessmentMarksTable` — per-student scores with max marks, can identify weak areas
- ✅ `SyllabusUnitsTable` + `SyllabusProgressTable` — syllabus coverage data per chapter
- ✅ `HomeworkTable` + `HomeworkSubmissionsTable` — homework patterns and completion status
- ✅ `AttendanceRecordsTable` — attendance patterns per student
- ✅ `AssessmentsTable` — assessment types (scheduled, surprise, exam), exam dates
- ❌ **Missing:** AI/LLM integration (no OpenAI/Gemini API key configured)
- ❌ **Missing:** Student-facing app/portal (no student role exists in `AppUsersTable`)
- ❌ **Missing:** Practice question bank table (no `practice_questions` or `question_bank` table)
- ❌ **Missing:** AI conversation/chat history table (no `ai_chat_sessions` or `ai_messages` table)
- ❌ **Missing:** Study plan/schedule table (no `study_plans` table)
- ❌ **Missing:** Photo/doubt submission table (no `doubt_submissions` table)

---

### 1.2 AI Report Card — Narrative + Predictive
**What:** Goes beyond the current stub. At end of each term:
- Generates a **narrative report** (not just grades) — "Aarav shows strong analytical skills in Mathematics but struggles with verbal reasoning in English. His attendance has improved 12% this term."
- **Predictive insights** — "Based on current trajectory, Aarav is on track for A in Science but at risk of B- in Hindi. Recommended focus areas: ..."
- **Comparative context** — "Aarav is in the top 15% of his class in Mathematics" (anonymized, opt-in)

**Why it stands out:** Traditional report cards are grade sheets. This is a **conversation starter** between parent and child.

**Roles:** Parent (consume), Teacher (review/edit before publish), School Admin (publish)
**Effort:** L
**Competitor gap:** PowerSchool has basic comments. No narrative AI generation.

**Data Readiness:**
- ✅ `AssessmentMarksTable` — all marks data with assessment scope (class, subject, max marks)
- ✅ `AttendanceRecordsTable` — 4-state attendance (present/absent/late/leave) with timestamps
- ✅ `ParentAchievementsTable` — already has `COMPETENCY` and `EI_METRIC` types with 0..1 progress values
- ✅ `AssessmentsTable` — assessment types, publish status, exam dates
- ✅ `AcademicYearsTable` — term/year scoping for historical aggregation
- ❌ **Missing:** AI/LLM integration for narrative generation
- ❌ **Missing:** Report card template table (no `report_card_templates` or `report_cards` table)
- ❌ **Missing:** Teacher remarks/comments table (no `teacher_remarks` table — `HomeworkSubmissionsTable.grade` is the closest but is homework-specific)
- ❌ **Missing:** Historical term aggregation logic (marks exist but no term-wise rollup materialized)
- ❌ **Missing:** Peer percentile calculation (requires cross-student aggregation within class)
- ❌ **Missing:** Report card PDF generation pipeline

---

### 1.3 Teacher Copilot — AI Lesson & Assessment Generator
**What:** AI assistant for teachers that:
- **Generates lesson plans** from the syllabus unit — "Generate a 40-min lesson plan for Chapter 5: Photosynthesis, including activities, key terms, and homework"
- **Creates assessments** — "Generate 20 MCQs + 5 short answer questions for Chapter 5, difficulty: medium, based on CBSE pattern"
- **Auto-grades** objective assessments with confidence scores
- **Suggests interventions** — "3 students scored below 40% on the last test. Recommended remedial topics: ..."

**Why it stands out:** Teachers spend 60% of their time on content creation and grading. This saves hours per week. Google Classroom doesn't do this. Canvas has some AI but not India-specific (CBSE/ICSE patterns).

**Roles:** Teacher (primary), School Admin (oversight)
**Effort:** L
**Competitor gap:** No Indian ERP has AI content generation. MagicSchool.ai exists but isn't integrated with school SIS.

**Data Readiness:**
- ✅ `SyllabusUnitsTable` + `CurriculumUnitsTable` — syllabus structure for lesson plan generation
- ✅ `AssessmentsTable` — assessment definitions for auto-grading target
- ✅ `AssessmentMarksTable` — existing marks for intervention analysis
- ✅ `SchoolSubjectsTable` — subject context
- ✅ `SchoolsTable.board` — board context (CBSE/ICSE/State) for pattern matching
- ❌ **Missing:** AI/LLM integration for lesson plan and question generation
- ❌ **Missing:** Lesson plan table (no `lesson_plans` table)
- ❌ **Missing:** Question bank table (no `question_bank` or `generated_questions` table)
- ❌ **Missing:** Auto-grading confidence score storage (no field in `AssessmentMarksTable`)
- ❌ **Missing:** Intervention recommendation table (no `intervention_suggestions` table)
- ❌ **Missing:** CBSE/ICSE pattern templates for question generation

---

### 1.4 Smart Attendance Insights
**What:** Beyond marking attendance:
- **Pattern detection** — "Aarav has been absent every Monday for 3 weeks. Flag for follow-up."
- **Correlation analysis** — "Students with <85% attendance score 23% lower on average in assessments"
- **Auto-alerts** to parents when patterns emerge, with gentle nudge language
- **Class heatmaps** — visual grid showing which days/hours have lowest attendance

**Why it stands out:** Attendance is data every ERP collects but none analyze. Turn raw data into action.

**Roles:** Teacher (view class), School Admin (view school-wide), Parent (receive alerts)
**Effort:** M
**Competitor gap:** PowerSchool has attendance reports but no pattern detection or correlation analysis.

**Data Readiness:**
- ✅ `AttendanceRecordsTable` — rich 4-state attendance with date, class, student, marked_by
- ✅ `AssessmentMarksTable` — marks data for correlation analysis
- ✅ `NotificationsTable` — can auto-send pattern alerts to parents
- ✅ `EnrollmentsTable` — class membership for aggregation scoping
- ❌ **Missing:** Pattern detection logic (no stored procedures or scheduled jobs for pattern analysis)
- ❌ **Missing:** Attendance pattern table (no `attendance_patterns` or `attendance_insights` table for cached results)
- ❌ **Missing:** Correlation analysis engine (no marks-attendance correlation computed)
- ❌ **Missing:** Heatmap aggregation query (data exists but no pre-computed heatmap cache)
- ❌ **Missing:** Auto-alert threshold configuration (no `alert_thresholds` table)

---

## 2. Parent Engagement & Community

### 2.1 Parent Pulse — Engagement Score & Gamification
**What:** A gamified engagement system for parents:
- **Engagement Score** (0-100) based on: app opens, message reads, attendance checks, homework reviews, event RSVPs, fee timeliness
- **Weekly streaks** — "You've checked Aarav's homework 5 days in a row!"
- **School leaderboard** (opt-in, anonymized) — "Top 10 engaged parents this month"
- **Badges** — "Homework Hero", "Attendance Aware", "Fee Champion", "PTM Pro"
- **School-wide engagement metric** on admin dashboard (already partially exists)

**Why it stands out:** ClassDojo gamifies student behavior. Nobody gamifies **parent engagement**. Schools struggle with parent apathy — this directly addresses it.

**Roles:** Parent (primary), School Admin (view aggregate), Teacher (view class parent engagement)
**Effort:** M
**Competitor gap:** ClassDojo has student points. No ERP gamifies parent behavior.

**Data Readiness:**
- ✅ `ParentAchievementsTable` — **already exists** with `BADGE` and `MISSION` types, status (EARNED/LOCKED/MET/IN_PROGRESS), icon, colors, progress. This is a strong foundation.
- ✅ `NotificationsTable` — has `isRead` and `readAt` for tracking notification engagement
- ✅ `AppUsersTable.lastLoginAt` — tracks last app open
- ✅ `MessageStatusTable` — tracks message read receipts
- ✅ `FeeRecordsTable` — has `status` and `dueDate` for fee timeliness scoring
- ✅ `PtmEventsTable` + `PtmClassProgressTable` — PTM attendance tracking
- ❌ **Missing:** Engagement score calculation logic (no computed score stored)
- ❌ **Missing:** Streak tracking table (no `parent_streaks` or `engagement_streaks` table)
- ❌ **Missing:** App-open tracking table (only `lastLoginAt` — no daily open log)
- ❌ **Missing:** Leaderboard aggregation (no materialized view or cached ranking)
- ❌ **Missing:** Engagement event log table (no `engagement_events` to track each interaction)
- ❌ **Missing:** Admin-facing aggregate engagement metric table

---

### 2.2 Family Circle — Multi-Parent Support
**What:** Support for India's joint family reality:
- **Multiple guardians** linked to one child — father, mother, grandparent, aunt
- **Role-based visibility** — grandparent sees attendance + events but not fees; parent sees everything
- **Shared calendar** — any family member can RSVP for PTM, events
- **Delegation** — "I can't attend PTM, assigning grandparent to attend"
- **Notification routing** — "Notify all family members for absences, only primary parent for fees"

**Why it stands out:** Indian families are multi-generational. Every ERP assumes one parent = one account. This is a cultural insight that creates deep lock-in.

**Roles:** Parent (primary), Family Member (secondary with scoped access)
**Effort:** M
**Competitor gap:** No ERP supports multi-guardian with role-based visibility. PowerSchool has "portal access" but not family delegation.

**Data Readiness:**
- ✅ `ParentChildLinksTable` — **already has** `relation` (Father|Mother|Guardian|…) and `isPrimaryGuardian` boolean. Multiple parents can link to the same child.
- ✅ `ChildrenTable` — child entity that multiple parents can be linked to
- ✅ `AppUsersTable.role` — role-based access control foundation
- ❌ **Missing:** Role-based visibility configuration (no `visibility_rules` or `guardian_permissions` table — can't scope what grandparent vs parent sees)
- ❌ **Missing:** Delegation table (no `event_delegations` table for PTM/event reassignment)
- ❌ **Missing:** Notification routing rules (no per-guardian notification preferences — `NotificationPreferencesTable` is per-user, not per-guardian-role)
- ❌ **Missing:** Family group concept (no `family_groups` table to group related guardians)
- ❌ **Missing:** Secondary guardian onboarding flow (link request flow exists but doesn't differentiate primary vs secondary roles in UI)

---

### 2.3 Class Community Feed — Private Social Network
**What:** A private, class-scoped social feed (like a mini ClassDojo Story):
- Teacher posts **photos/videos from class** (activity, projects, events)
- Parents can **react** (emoji reactions, not comments to avoid noise)
- **Moment highlights** — "Today's Science experiment", "Art project showcase"
- **Auto-curation** into a term-end "Class Yearbook" digital album
- **Privacy-first** — only parents of students in that class can see posts

**Why it stands out:** ClassDojo's core feature is this feed. But ClassDojo is a separate app. Integrating it into the ERP means one app, one login, one notification stream.

**Roles:** Teacher (post), Parent (view/react), School Admin (oversight)
**Effort:** M
**Competitor gap:** ClassDojo has this but it's a standalone app. No Indian ERP has a class feed.

**Data Readiness:**
- ✅ `SchoolMediaTable` — exists with IMAGE|VIDEO types, URL, position (can store feed media)
- ✅ `SupabaseStorage` — media upload infrastructure already working
- ✅ `EnrollmentsTable` — class-scoping for privacy (can enforce only same-class parents see posts)
- ✅ `NotificationsTable` — can notify parents when new feed posts are created
- ❌ **Missing:** Feed post table (no `class_feed_posts` or `class_stories` table)
- ❌ **Missing:** Reaction table (no `feed_reactions` table for emoji reactions)
- ❌ **Missing:** Class-scoped audience resolution (EnrollmentsTable exists but no query maps parents → class for feed visibility)
- ❌ **Missing:** Yearbook auto-curation logic (no `yearbook_collections` table or aggregation)
- ❌ **Missing:** Media moderation/approval workflow (no `media_approval` table)

---

### 2.4 Smart Notifications — Context-Aware & Actionable
**What:** Notifications that aren't just alerts but **actionable cards**:
- "Aarav was marked absent today" → **[Report Reason]** **[Mark as Pre-Informed]**
- "Science test on Friday" → **[View Syllabus]** **[Set Reminder]** **[Download Practice Sheet]**
- "Fees due in 3 days" → **[Pay Now]** **[Schedule Payment]** **[Request Extension]**
- **Quiet hours** — parent sets "Don't notify between 10 PM - 7 AM"
- **Priority routing** — urgent (absence, fee overdue) vs informational (announcement, event)
- **Digest mode** — "Send me one summary at 6 PM instead of individual alerts"

**Why it stands out:** Every ERP sends notifications. None make them **actionable** or respect parent preferences. This reduces notification fatigue (the #1 reason parents mute school apps).

**Roles:** Parent, Teacher, School Admin (all benefit)
**Effort:** M
**Competitor gap:** PowerSchool, ClassDojo — all send flat push notifications. None have actionable cards or quiet hours.

**Data Readiness:**
- ✅ `NotificationPreferencesTable` — **already exists** with per-user, per-category `enabled` boolean and `sound` field
- ✅ `NotificationsTable` — rich schema with `category`, `deepLink`, `actorId`, `refType`, `refId`, `idempotencyKey`, `isRead`, `readAt`
- ✅ `DeviceTokensTable` — multi-device push delivery infrastructure
- ✅ `NotificationService` — FCM dispatch already working
- ❌ **Missing:** Quiet hours fields (no `quiet_start_time` / `quiet_end_time` in `NotificationPreferencesTable`)
- ❌ **Missing:** Priority levels (no `priority` field in `NotificationsTable` — urgent vs informational)
- ❌ **Missing:** Digest mode config (no `digest_mode` boolean or `digest_time` field)
- ❌ **Missing:** Action button data (no `action_buttons` JSON field in `NotificationsTable` for [Pay Now] [Mark Pre-Informed] etc.)
- ❌ **Missing:** Scheduled delivery (no `scheduled_for` timestamp field)
- ❌ **Missing:** Notification grouping/batching logic

---

## 3. School Discovery & Marketplace

### 3.1 Vidya Prayag Marketplace — School Discovery with Real Data
**What:** The school discovery feature (already partially built) becomes a **full marketplace**:
- **SRI Score** (School Reputation Index) — computed from real data: attendance rate, teacher:student ratio, board results, parent engagement, infrastructure, safety ratings
- **Verified reviews** — only parents with children currently enrolled can review (kills fake reviews)
- **Live availability** — "12 seats available in Grade 3 for 2026-27"
- **Virtual tours** — embedded 360° photos / video tours (already have tour-videos field)
- **Compare schools** — side-by-side comparison of 3 schools on fees, results, ratio, facilities
- **Admission tracker** — parent applies through the app, tracks status, uploads documents, pays application fee — all in one flow

**Why it stands out:** GreatSchools has reviews but no live admission flow. PowerSchool has SIS but no discovery. We connect discovery → admission → enrollment → daily engagement in one app. That's a **closed loop** nobody else offers.

**Roles:** Parent (discover/apply), School Admin (manage listings/applications)
**Effort:** L (SRI algorithm + admission flow + compare feature)
**Competitor gap:** No school ERP has a built-in marketplace. GreatSchools + ParentSquare are separate products.

**Data Readiness:**
- ✅ `SchoolsTable` — rich data: `board`, `medium`, `city`, `state`, `latitude`/`longitude`, `totalStudents`, `totalClasses`, `yearEstablished`, `affiliationNumber`, `logoUrl`, `brandColor`, `website`
- ✅ `AdmissionEnquiriesTable` — admission enquiry intake already working
- ✅ `SchoolPhilosophyTable` — `coreMission`, `learningModel`, `primaryLanguage`, `publicProfile`
- ✅ `SchoolMediaTable` — school photos/videos with position ordering
- ✅ `CalendarEventsTable` — public events for open houses
- ✅ `AnnouncementsTable` — school announcements visible on discovery
- ✅ School discovery API — `GET /api/v1/parent/schools/discover` with lat/lng/radius already working
- ❌ **Missing:** SRI score column or table (no `sri_score` in `SchoolsTable`, no `sri_scores` table)
- ❌ **Missing:** Reviews/ratings table (no `school_reviews` table)
- ❌ **Missing:** Seat availability table (no `seat_availability` or `class_capacity` table)
- ❌ **Missing:** Application workflow table (no `admission_applications` table — `AdmissionEnquiriesTable` is enquiry-only, no document upload/status tracking)
- ❌ **Missing:** Compare aggregation query (data exists but no comparison endpoint)
- ❌ **Missing:** Teacher count for ratio (no `teacher_count` field — must count from `FacultyTable`)
- ❌ **Missing:** Board results pass rate (no historical results aggregation)

---

### 3.2 School Branding Kit — Digital Presence
**What:** Schools get a **public-facing digital presence** within the app:
- Custom school profile page with logo, photos, philosophy, facilities, achievements
- **Public events calendar** — prospective parents can see upcoming open houses
- **Achievement showcase** — sports wins, academic toppers, cultural events (with privacy controls)
- **School story** — term-in-review auto-generated video/slideshow from class feed posts
- **QR code** — schools print QR on banners → parents scan → land on school profile in app

**Why it stands out:** Small/medium schools in India have no digital presence. They rely on Facebook pages or WhatsApp groups. This gives them a **professional, verified digital identity** tied to a real ERP backend.

**Roles:** School Admin (manage), Parent (view), Prospective Parent (view public profile)
**Effort:** M
**Competitor gap:** No ERP offers school branding/digital presence. Schools use separate tools (WordPress, Facebook).

**Data Readiness:**
- ✅ `SchoolsTable` — `logoUrl`, `brandColor`, `website`, `fullAddress`, `principalName`
- ✅ `SchoolPhilosophyTable` — `coreMission`, `learningModel`, `primaryLanguage`, `publicProfile` flag
- ✅ `SchoolMediaTable` — IMAGE|VIDEO with position ordering for gallery/tour
- ✅ `CalendarEventsTable` — public events for prospective parents
- ✅ `StorageMetricsTable` — tracks storage usage per school
- ❌ **Missing:** QR code generation (no QR library integrated, no `qr_codes` table)
- ❌ **Missing:** Achievement showcase table (no `school_achievements` table for sports/academic/cultural wins)
- ❌ **Missing:** School story auto-generation (no aggregation from class feed — feed doesn't exist yet)
- ❌ **Missing:** Facilities table (no `school_facilities` table for labs, sports, library, etc.)
- ❌ **Missing:** Public profile page rendering (data exists but no public-facing web endpoint)

---

## 4. Teacher Productivity

### 4.1 One-Tap Lesson Capture — "Teach, Don't Type"
**What:** Teacher productivity features that minimize screen time:
- **Voice-to-text attendance** — teacher says "Aarav present, Priya absent, Rohan late..." and AI marks the roster
- **Photo-to-homework** — teacher snaps a photo of the homework written on blackboard → AI extracts text → posts as homework assignment with due date
- **Quick capture templates** — pre-made templates for common homework types ("Read Chapter 5", "Solve Exercise 3.2", "Learn spellings list 7")
- **Voice feedback** on student submissions — teacher records 30-sec voice note instead of typing comments

**Why it stands out:** Teachers in India spend enormous time on data entry. Voice/photo capture is 10x faster. No ERP offers this.

**Roles:** Teacher (primary)
**Effort:** M (uses on-device speech recognition + OCR)
**Competitor gap:** No ERP has voice/photo-based lesson capture. Google Classroom requires typing everything.

**Data Readiness:**
- ✅ `HomeworkTable` — homework assignment storage with title, description, due date
- ✅ `HomeworkAttachmentsTable` — file attachment support exists
- ✅ `AssessmentsTable` — assessment definitions can be voice-created
- ✅ `AttendanceRecordsTable` — attendance marking target for voice input
- ✅ `HomeworkSubmissionsTable.grade` — text feedback field exists (could store voice note URL)
- ❌ **Missing:** Speech-to-text integration (no Android `SpeechRecognizer` or cloud STT configured)
- ❌ **Missing:** OCR integration (no ML Kit / Tesseract / Google Vision configured)
- ❌ **Missing:** Voice note storage (no `voice_note_url` field in `HomeworkSubmissionsTable`)
- ❌ **Missing:** Homework template table (no `homework_templates` for quick capture presets)
- ❌ **Missing:** Voice-to-attendance mapping logic

---

### 4.2 Teacher Wellness & Workload Analytics
**What:** Admin sees teacher workload metrics:
- **Workload score** — based on classes taught, assessments created, homework assigned, submissions reviewed, messages answered
- **Burnout flags** — "Teacher X has been active 14 hours/day for 5 days. Recommend check-in."
- **Peer comparison** (anonymized) — "You're creating 40% more assessments than the school average"
- **Time saved** — "VidyaSetu AI saved you 6.2 hours this week" (when AI features are used)

**Why it stands out:** Teacher burnout is a real crisis in Indian schools. No ERP tracks or addresses it. This shows the school cares about teachers, not just students.

**Roles:** School Admin (view), Teacher (view own)
**Effort:** M
**Competitor gap:** No ERP has teacher wellness analytics.

**Data Readiness:**
- ✅ `TeacherCheckInsTable` — daily check-in timestamps (can derive active hours)
- ✅ `TeacherPeriodsTable` — teaching load (classes/periods assigned)
- ✅ `AssessmentsTable` — `teacherId` + `createdAt` (can count assessments created per teacher)
- ✅ `HomeworkTable` — `teacherId` + `createdAt` (can count homework assigned)
- ✅ `HomeworkSubmissionsTable` — `reviewedBy` + `reviewedAt` (can count submissions reviewed)
- ✅ `MessagesTable` — sender activity (can count messages answered)
- ✅ `AppUsersTable.lastLoginAt` — login frequency
- ❌ **Missing:** Workload score calculation logic (data exists but no aggregation/computation)
- ❌ **Missing:** Activity tracking table (no `teacher_activity_log` for granular time tracking)
- ❌ **Missing:** Burnout flag thresholds (no `wellness_thresholds` config table)
- ❌ **Missing:** Peer comparison aggregation (no materialized view for anonymized peer stats)
- ❌ **Missing:** Time-saved tracking (no `ai_time_saved` log — depends on AI features being used)
- ❌ **Missing:** Wellness dashboard UI (analytics screens exist but no teacher wellness view)

---

## 5. Student-Centric (Future)

### 5.1 Student Digital Identity — "Vidya Passport"
**What:** A portable, verified student identity that persists across schools:
- **Digital report card** — all marks, attendance, achievements, certificates in one place
- **Skill badges** — "Math Olympiad Topper", "Science Fair Winner", "100% Attendance"
- **Transfer certificate** — when switching schools, the digital TC is generated and transferable within the Vidya Prayag network
- **Achievement timeline** — visual journey from Grade 1 to Grade 12
- **QR-verified certificates** — schools issue digital certificates with QR codes that can be verified by anyone

**Why it stands out:** This creates a **network effect**. As more schools join Vidya Prayag, the passport becomes more valuable. Parents won't switch to a different ERP because they'd lose the passport. This is the **biggest lock-in feature**.

**Roles:** Student (owner), Parent (guardian), School Admin (issuer), Prospective School (verifier)
**Effort:** L (requires blockchain/verifiable credentials or signed digital certificates)
**Competitor gap:** No Indian ERP has portable student identity. DigiLocker exists but isn't school-integrated.

**Data Readiness:**
- ✅ `StudentsTable` — `studentCode` (unique), `fullName`, `className`, `section`, `rollNumber`
- ✅ `AssessmentMarksTable` — full marks history per student
- ✅ `AttendanceRecordsTable` — full attendance history
- ✅ `ParentAchievementsTable` — `BADGE` type with title, icon, colors, status (EARNED/LOCKED)
- ✅ `AcademicYearsTable` — year-by-year academic history scoping
- ✅ `EnrollmentsTable` — enrollment history (school transfers tracked via start/end dates)
- ❌ **Missing:** Cross-school identity (student identity is school-scoped — no global student ID)
- ❌ **Missing:** Transfer certificate table (no `transfer_certificates` table)
- ❌ **Missing:** Certificate issuance table (no `digital_certificates` table with QR verification)
- ❌ **Missing:** Achievement timeline aggregation (data exists in fragments but no unified timeline view)
- ❌ **Missing:** QR code signing/verification infrastructure
- ❌ **Missing:** Blockchain/verifiable credential framework
- ❌ **Missing:** Student app (no student role in `AppUsersTable` — only parent/teacher/admin)

---

### 5.2 Student Goals & Reflection
**What:** A simple goal-setting interface for students:
- **Weekly goals** — "I will complete all homework on time this week"
- **Self-reflection prompts** — "What was the hardest part of this week? What did you learn?"
- **Teacher feedback loop** — teacher sees student goals and can encourage/adjust
- **Parent visibility** — parent sees child's goals and reflections (builds family conversation)
- **Term compilation** — goals + reflections compile into the AI Report Card narrative

**Why it stands out:** Goal-setting and reflection are proven to improve academic outcomes. No ERP includes student voice. This makes the student an active participant, not just a data point.

**Roles:** Student (primary), Parent (view), Teacher (guide)
**Effort:** M
**Competitor gap:** No ERP has student goal-setting or reflection. Seesaw has portfolios but not goal-tracking.

**Data Readiness:**
- ❌ **Missing:** Student goals table (no `student_goals` table)
- ❌ **Missing:** Reflection journal table (no `student_reflections` table)
- ❌ **Missing:** Teacher feedback on goals (no `goal_feedback` table)
- ❌ **Missing:** Student app/portal (no student role exists)
- ❌ **Missing:** Parent visibility config for goals (no `goal_visibility` settings)
- ❌ **Missing:** Term compilation logic for AI report card integration
- ⚠️ **Note:** This feature requires a student-facing app which doesn't exist yet. All data must be created from scratch.

---

## 6. Trust, Safety & Transparency

### 6.1 Transparency Dashboard — "Radical Openness"
**What:** A public-facing (or parent-facing) dashboard showing school health metrics:
- **Attendance rate** (school-wide, trend)
- **Teacher:student ratio** (live)
- **Fee transparency** — total collected, total outstanding, how fees are used (if school opts in)
- **Grievance resolution time** — average time to resolve parent complaints
- **PTM attendance rate** — what % of parents attend PTMs
- **Response time** — how quickly teachers/admin respond to parent messages

**Why it stands out:** Indian parents choose schools based on word-of-mouth and reputation. This gives them **data-driven transparency**. Schools that opt in signal confidence. It's like a "nutrition label" for schools.

**Roles:** Parent (view), School Admin (manage/opt-in), Prospective Parent (view public metrics)
**Effort:** M
**Competitor gap:** No ERP has a transparency dashboard. GreatSchools has external ratings but not real-time internal data.

**Data Readiness:**
- ✅ `AttendanceRecordsTable` — school-wide attendance rate calculable
- ✅ `FeeRecordsTable` — fee collection/outstanding data (amount, status PAID/DUE/OVERDUE)
- ✅ `PtmEventsTable` + `PtmClassProgressTable` — PTM attendance rate calculable
- ✅ `MessagesTable` + `MessageStatusTable` — response time trackable (message sent → read timestamps)
- ✅ `FacultyTable` + `StudentsTable` — teacher:student ratio calculable
- ✅ `SchoolsTable` — `totalStudents` field exists
- ❌ **Missing:** Grievance/complaint table (no `grievances` or `complaints` table for resolution time tracking)
- ❌ **Missing:** Public metrics aggregation table (no `public_school_metrics` materialized view)
- ❌ **Missing:** Opt-in configuration (no `transparency_opt_in` flag in `SchoolsTable` or `SchoolPhilosophyTable`)
- ❌ **Missing:** Expense allocation data (no `expense_categories` or `fee_allocation` table — needed for fee transparency)
- ❌ **Missing:** Public-facing endpoint (data exists but no unauthenticated public metrics API)

---

### 6.2 Safe Transport — Live Tracking + Geofence Alerts
**What:** School bus tracking with parent-facing features:
- **Live GPS tracking** — parent sees bus location on map in real-time
- **ETA notification** — "Bus will arrive at your stop in 5 minutes"
- **Boarding/alighting alerts** — "Aarav boarded the bus at 7:15 AM" / "Aarav alighted at school at 7:45 AM"
- **Geofence alerts** — parent gets alert if bus deviates from route or stops unexpectedly
- **Driver profile** — verified driver photo, name, phone (visible to parent)
- **Route history** — parent can see last 7 days of bus routes

**Why it stands out:** Safety is the #1 concern for Indian parents. Existing trackers (TrackSchoolBus, SafeBus) are separate products. Integrating into the school ERP means one app, one notification stream, and the school owns the data.

**Roles:** Parent (track), School Admin (manage routes/drivers), Teacher (view for their students)
**Effort:** L (GPS hardware integration + map UI + real-time backend)
**Competitor gap:** PowerSchool has transport info but no live tracking. Separate tracking apps exist but aren't integrated with ERP.

**Data Readiness:**
- ❌ **Missing:** Vehicle/bus table (no `vehicles` or `buses` table)
- ❌ **Missing:** Route table (no `transport_routes` table)
- ❌ **Missing:** Stop table (no `transport_stops` table)
- ❌ **Missing:** Driver table (no `drivers` table — `NonTeachingStaffTable` exists but no transport-specific fields)
- ❌ **Missing:** GPS tracking table (no `gps_locations` or `bus_locations` real-time table)
- ❌ **Missing:** Geofence configuration (no `geofences` table)
- ❌ **Missing:** Boarding/alighting records (no `boarding_records` table)
- ❌ **Missing:** Route history storage (no `route_history` table)
- ❌ **Missing:** Student-route assignment (no `student_route_assignments` table)
- ⚠️ **Note:** Entire transport module must be built from scratch. No data infrastructure exists.

---

### 6.3 SOS & Safety Button
**What:** In-app emergency features:
- **Student SOS** (if student app exists) — one-tap sends location + alert to school + parents
- **Parent panic button** — "I can't reach my child at school" → triggers immediate school notification
- **Teacher SOS** — emergency alert to admin with location
- **Drill mode** — schools can run mock drills through the app, track response times
- **Incident logging** — teachers/admins can log incidents (injury, conflict, medical) with timestamp, photos, description

**Why it stands out:** Safety features in Indian schools are reactive (CCTV after the fact). This is **proactive and instant**. Post-pandemic, safety is a top concern.

**Roles:** All roles
**Effort:** M
**Competitor gap:** No ERP has integrated SOS. Separate safety apps exist but aren't connected to school systems.

**Data Readiness:**
- ✅ `NotificationsTable` — can deliver SOS alerts to relevant parties
- ✅ `DeviceTokensTable` — push delivery for immediate alerts
- ✅ `AppUsersTable` — all user roles exist (parent, teacher, admin) for alert routing
- ❌ **Missing:** SOS alert table (no `sos_alerts` table with location, timestamp, status)
- ❌ **Missing:** Incident log table (no `incident_logs` table for injury/conflict/medical recording)
- ❌ **Missing:** Drill mode table (no `safety_drills` table for mock drill tracking)
- ❌ **Missing:** Location capture (no GPS location field in any table for SOS)
- ❌ **Missing:** Emergency contact chain configuration (no `emergency_contacts` table)

---

## 7. Financial Innovation

### 7.1 Fee Flexibility — Installments & Smart Reminders
**What:** Beyond "pay full fee by date":
- **Installment plans** — school configures "Pay in 3 installments: 40% + 30% + 30%"
- **Pay what you can** — parent requests partial payment with reason, admin approves
- **Smart reminders** — "Fee due in 7 days. Pay now to avoid late fee of ₹500." → **[Pay Now]** **[Request Extension]** **[Split Payment]**
- **Auto-receipt** — instant digital receipt with QR verification
- **Fee history export** — for tax purposes (80G receipts)
- **Scholarship integration** — approved scholarships auto-deduct from fee amount

**Why it stands out:** Indian school fees are a major financial burden. No ERP offers flexibility. This creates goodwill and reduces fee default rates.

**Roles:** Parent (pay/request), School Admin (configure/approve)
**Effort:** M (payment gateway + backend logic)
**Competitor gap:** PowerSchool has fee payment but no installments or flexibility requests.

**Data Readiness:**
- ✅ `FeeRecordsTable` — exists with `amount`, `currency` (INR default), `status` (PAID/DUE/OVERDUE), `category` (Tuition/Transport/...), `dueDate`, `lastRemindedAt`
- ✅ `ScholarshipApplicationsTable` — exists with status workflow (Received/Under Review/Shortlisted)
- ✅ `ScholarshipsTable` — scholarship definitions
- ❌ **Missing:** Payment gateway integration (no Razorpay/Stripe/UPI — no `payment_gateway_ref` or `transaction_id` field)
- ❌ **Missing:** Installment plan table (no `fee_installment_plans` table)
- ❌ **Missing:** Partial payment tracking (no `partial_payments` table — `FeeRecordsTable.status` is PAID/DUE/OVERDUE only)
- ❌ **Missing:** Receipt generation (no `fee_receipts` table or PDF generation)
- ❌ **Missing:** 80G receipt format (no tax receipt configuration)
- ❌ **Missing:** Late fee configuration (no `late_fee_config` or `late_fee_amount` field)
- ❌ **Missing:** Payment extension request table (no `fee_extension_requests` table)
- ❌ **Missing:** Scholarship-to-fee deduction logic (tables exist but no linking)

---

### 7.2 School Expense Transparency — "Where Does My Fee Go?"
**What:** Opt-in feature where schools show parents a breakdown:
- "Your ₹50,000 annual fee is allocated as: 45% teacher salaries, 20% infrastructure, 15% learning materials, 10% transport, 10% admin"
- **Visual pie chart** on parent fee screen
- **Termly update** — "This term, ₹2L was spent on new science lab equipment"

**Why it stands out:** Fee opacity is a major parent grievance in India. Schools that show this build **massive trust**. It's a differentiator that costs nothing but creates loyalty.

**Roles:** School Admin (configure/opt-in), Parent (view)
**Effort:** S
**Competitor gap:** No ERP offers fee transparency breakdown.

**Data Readiness:**
- ✅ `FeeRecordsTable` — fee collection data exists
- ✅ `SchoolsTable` — school context
- ❌ **Missing:** Expense category table (no `expense_categories` table)
- ❌ **Missing:** Fee allocation breakdown table (no `fee_allocations` table showing % split)
- ❌ **Missing:** Termly expense summary (no `expense_summaries` table)
- ❌ **Missing:** Opt-in flag (no `expense_transparency_enabled` in `SchoolsTable` or `SchoolPhilosophyTable`)
- ❌ **Missing:** Expense tracking module (no `expenses` or `vendor_payments` table at all)
- ⚠️ **Note:** This requires an expense management module which doesn't exist. The transparency view is small, but the data source doesn't exist yet.

---

## 8. Cultural & Regional Innovation

### 8.1 Multi-Language Engine — Truly Local
**What:** Not just UI translation but **content translation**:
- **Auto-translate messages** — teacher types in English, parent receives in Hindi/Marathi/Tamil (with Google Translate API or on-device NLP)
- **Announcement translation** — school posts once, parents read in their preferred language
- **Regional calendar support** — show holidays based on state (Maharashtra vs Karnataka vs UP)
- **Board-specific features** — CBSE vs ICSE vs State Board grading patterns, exam formats
- **Voice messages in any language** — teacher sends voice note in Hindi, parent hears in English (or vice versa)

**Why it stands out:** India has 22 official languages and hundreds of dialects. TalkingPoints does translation in the US but isn't India-optimized. This is a **deep cultural moat**.

**Roles:** All roles (each sets preferred language)
**Effort:** M (Google Translate API + language preference system)
**Competitor gap:** No Indian ERP has content translation. TalkingPoints (US-focused) does this but isn't integrated with SIS.

**Data Readiness:**
- ✅ `AppUsersTable.languagePref` — **already exists** with default "hi" (Hindi). Per-user language preference stored.
- ✅ `SchoolsTable.medium` — school medium of instruction
- ✅ `SchoolPhilosophyTable.primaryLanguage` — school's primary language
- ✅ `MessagesTable` — message content that needs translation
- ✅ `AnnouncementsTable` — announcement content that needs translation
- ❌ **Missing:** Translation API integration (no Google Translate / Azure Translator configured)
- ❌ **Missing:** Translation cache table (no `translation_cache` table to avoid re-translating)
- ❌ **Missing:** Regional calendar logic (no state-to-holiday mapping table — `SchoolsTable.state` exists but no `regional_holidays` table)
- ❌ **Missing:** Board-specific grading config (no `grading_scales` or `board_configs` table — `SchoolsTable.board` and `SchoolsTable.gradingSystem` exist but no detailed grading pattern)
- ❌ **Missing:** Voice message translation (no voice message storage at all)

---

### 8.2 Festival & Cultural Calendar Integration
**What:** Beyond academic calendar:
- **Festival greetings** — auto-generated, school-branded festival cards (Diwali, Eid, Christmas, Pongal) sent to parents
- **Cultural event coordination** — schools can organize cultural events with RSVP, volunteer sign-ups, contribution tracking
- **Diversity celebration** — "This week: Eid al-Fitr. School wishes all our families Eid Mubarak." (inclusive, multi-faith)
- **Local holiday auto-detection** — based on school's state/region

**Why it stands out:** Indian schools are deeply cultural. Acknowledging festivals through the app builds emotional connection. No ERP does this.

**Roles:** School Admin (send), Parent (receive)
**Effort:** S
**Competitor gap:** No ERP has cultural calendar or festival greetings.

**Data Readiness:**
- ✅ `CalendarEventsTable` — rich event model with types (event/holiday/exam/PTM/activity), status (draft/published), audience targeting, multi-day ranges
- ✅ `SchoolsTable.state` — state/region for local holiday detection
- ✅ `NotificationsTable` — can deliver festival greetings
- ✅ `AnnouncementsTable` — can post festival announcements
- ✅ `SchoolsTable.logoUrl` + `brandColor` — branding for greeting cards
- ❌ **Missing:** Festival template table (no `festival_templates` table with pre-built greeting card designs)
- ❌ **Missing:** Auto-detection logic (no scheduled job to detect upcoming festivals by region)
- ❌ **Missing:** Greeting card generation (no image composition library integrated)
- ❌ **Missing:** Cultural event RSVP table (no `cultural_event_rsvps` table — `CalendarEventsTable` has events but no RSVP)
- ❌ **Missing:** Volunteer sign-up table (no `event_volunteers` table)

---

### 8.3 Regional Curriculum Alignment
**What:** Board-specific modules:
- **CBSE mode** — NCERT syllabus templates, CBSE grading pattern (A1-E), board exam tracking
- **ICSE mode** — ICSE syllabus, different grading, project weightage
- **State Board mode** — state-specific syllabus, regional language support, SSC/HSC patterns
- **Auto-align** — onboarding wizard detects board from school registration, configures entire system accordingly

**Why it stands out:** Every Indian ERP claims to support "all boards" but does it poorly. Deep board-specific alignment shows we understand the education system.

**Roles:** School Admin (configure at onboarding), Teacher (uses board-specific tools)
**Effort:** M
**Competitor gap:** Most ERPs are one-size-fits-all. Board-specific deep alignment is rare.

**Data Readiness:**
- ✅ `SchoolsTable.board` — board type stored (CBSE/ICSE/State Board)
- ✅ `SchoolsTable.gradingSystem` — grading system field exists
- ✅ `SchoolClassesTable` — class structure
- ✅ `SchoolSubjectsTable` — subject definitions
- ✅ `CurriculumUnitsTable` — curriculum unit structure
- ✅ `AssessmentsTable.type` — assessment types (scheduled/surprise/assignment/project/exam)
- ❌ **Missing:** Grading scale config table (no `grading_scales` table — CBSE A1-E pattern, ICSE 1-10 pattern, State Board % pattern not configurable)
- ❌ **Missing:** Board-specific report card templates (no `report_card_templates` table)
- ❌ **Missing:** NEP 2020 holistic progress card format (no `hpc_templates` table)
- ❌ **Missing:** Board-specific exam pattern config (no `exam_patterns` table)
- ❌ **Missing:** CCE/formative/summative assessment weightage config (no `assessment_weightage` table)

---

## 9. Data & Analytics Innovation

### 9.1 Parent-Facing Analytics — "Your Child's Journey"
**What:** Parents see their child's data in a meaningful way:
- **Growth chart** — marks trajectory over terms/years (line chart with trend line)
- **Attendance heatmap** — GitHub-style contribution graph showing attendance patterns
- **Subject strength radar** — spider chart showing relative performance across subjects
- **Peer context** (opt-in, anonymized) — "Your child is in the top 20% in Science"
- **Improvement tracker** — "Maths score improved from 72% to 85% this term. Keep it up!"
- **Predictive alert** — "Based on current trend, your child may drop below 75% in Hindi. Recommended action: ..."

**Why it stands out:** Parents get report cards 2-3 times a year. This gives them **continuous insight**. It turns data into parenting action.

**Roles:** Parent (primary), Teacher (view to discuss in PTM)
**Effort:** M
**Competitor gap:** PowerSchool shows grades but no growth charts, radar charts, or predictive alerts for parents.

**Data Readiness:**
- ✅ `AssessmentMarksTable` — all marks with assessment scope for growth chart and radar chart
- ✅ `AttendanceRecordsTable` — attendance history for heatmap
- ✅ `SyllabusProgressTable` — syllabus coverage data
- ✅ `AcademicYearsTable` — year-over-year comparison scoping
- ✅ `AssessmentsTable` — `publishedAt` for timeline
- ✅ `ParentAchievementsTable` — `COMPETENCY` type with 0..1 progress for skill tracking
- ❌ **Missing:** Growth chart aggregation query (data exists but no pre-computed trend materialized)
- ❌ **Missing:** Heatmap aggregation (no `attendance_heatmap` cache table)
- ❌ **Missing:** Radar chart aggregation (no per-subject average materialized)
- ❌ **Missing:** Peer percentile calculation (requires cross-student aggregation — no `peer_percentiles` cache)
- ❌ **Missing:** Predictive trend logic (no ML model or trend projection)
- ❌ **Missing:** Improvement tracker (no `improvement_events` table)

---

### 9.2 School Benchmarking — Cross-School Analytics
**What:** (With privacy + opt-in) Schools compare themselves to similar schools:
- "Your school's attendance rate is 92% — 8% above the network average for CBSE schools in your city"
- "Your teacher:student ratio is 1:28 — better than 65% of similar schools"
- "Your parent engagement score is 74 — top quartile in the network"
- **Anonymous peer group** — schools of similar size, board, region
- **Best practices** — "Schools in the top 10% for parent engagement do these 3 things differently..."

**Why it stands out:** Schools operate in isolation. Benchmarking creates a **community of schools** and positions Vidya Prayag as the intelligence layer, not just a tool. It also creates network effects — more schools = better benchmarks.

**Roles:** School Admin (view)
**Effort:** M (requires aggregation pipeline + privacy framework)
**Competitor gap:** No ERP offers cross-school benchmarking. Niche consulting firms charge lakhs for this.

**Data Readiness:**
- ✅ `SchoolsTable` — `board`, `city`, `state`, `totalStudents` for peer group matching
- ✅ `AttendanceRecordsTable` — attendance rate per school calculable
- ✅ `AssessmentMarksTable` — academic performance per school calculable
- ✅ `FeeRecordsTable` — fee collection rate per school calculable
- ✅ `FacultyTable` — teacher count for ratio
- ✅ `AppUsersTable.lastLoginAt` — engagement signal per school
- ❌ **Missing:** Cross-school aggregation pipeline (no materialized views or scheduled aggregation jobs)
- ❌ **Missing:** Anonymization layer (no data masking or k-anonymity framework)
- ❌ **Missing:** Peer group matching logic (no `school_peer_groups` table)
- ❌ **Missing:** Benchmark score table (no `school_benchmarks` table for cached comparison results)
- ❌ **Missing:** Best practices correlation table (no `best_practices` table linking top performers to specific behaviors)
- ⚠️ **Note:** Requires multiple schools on the platform for meaningful comparison. Network effect dependency.

---

## 10. Integration & Ecosystem

### 10.1 WhatsApp-First Architecture
**What:** Meet parents where they already are:
- **WhatsApp bot** — parents who haven't installed the app can still get key notifications (attendance, marks, fee reminders) via WhatsApp Business API
- **WhatsApp to app funnel** — WhatsApp message includes deep link → "Open in app for full details"
- **Two-way WhatsApp** — parent replies to WhatsApp message → routed to teacher's inbox in app
- **WhatsApp group sync** — school admin posts announcement → auto-posted to class WhatsApp groups (with formatting)
- **WhatsApp payment** — fee payment link sent via WhatsApp (Razorpay/UPI)

**Why it stands out:** Indian parents live on WhatsApp. Forcing app adoption is friction. WhatsApp-first means **zero barrier to entry**, then gradual app migration. No ERP does this.

**Roles:** Parent (receive via WhatsApp), School Admin (send), Teacher (receive replies)
**Effort:** M (WhatsApp Business API already partially integrated for OTP)
**Competitor gap:** No ERP has WhatsApp-first architecture. ClassDojo has its own messaging. Remind uses SMS.

**Data Readiness:**
- ✅ `WhatsappLogsTable` — WhatsApp delivery logging exists
- ✅ `WhatsAppCloudProvider` — WhatsApp Cloud API integration exists (currently for OTP only)
- ✅ `AnnouncementsTable` — has WhatsApp sync endpoint (`sync-whatsapp` mentioned in audit)
- ✅ `NotificationsTable` — notification content that can be forwarded to WhatsApp
- ✅ `OtpGatewayDevicesTable` + `SmsRequestsTable` — OTP gateway infrastructure (reusable patterns)
- ❌ **Missing:** WhatsApp Business API for general notifications (currently OTP-only — template messages need Meta approval for general use)
- ❌ **Missing:** Two-way WhatsApp messaging (no inbound webhook handler for parent replies)
- ❌ **Missing:** WhatsApp group sync (no `whatsapp_group_mappings` table)
- ❌ **Missing:** WhatsApp payment links (no payment gateway integration)
- ❌ **Missing:** WhatsApp-to-app deep link generation (no `whatsapp_deep_links` table)
- ❌ **Missing:** WhatsApp message templates for non-OTP use cases (attendance, marks, fees, announcements)

---

### 10.2 Open API & Zapier Integration
**What:** Schools can connect Vidya Prayag to other tools:
- **REST API** with API keys (already have most endpoints)
- **Webhooks** — "When attendance is marked, call this URL"
- **Zapier/Make integration** — no-code automation: "When new student enrolled → add to Google Sheets → send welcome email"
- **Google Calendar sync** — school events auto-sync to parent/teacher Google Calendar
- **Google Drive integration** — homework attachments stored in school's Google Drive

**Why it stands out:** Indian ERPs are walled gardens. Open API makes Vidya Prayag a **platform**, not just an app. Tech-savvy schools and integrators become advocates.

**Roles:** School Admin (API access), Developer (integration)
**Effort:** M
**Competitor gap:** PowerSchool has APIs but they're enterprise-priced. No Indian ERP has public APIs or Zapier integration.

**Data Readiness:**
- ✅ Extensive REST API — 50+ endpoints already exist across all modules (auth, parent, teacher, school, admin, calendar, content, config, media, notifications, gateway)
- ✅ JWT authentication — API security foundation exists
- ✅ `AppConfigTable` — can store API keys and feature flags
- ✅ `NotificationsTable` — webhook event source (attendance marked, fee paid, etc.)
- ❌ **Missing:** API key management table (no `api_keys` table for school-issued developer keys)
- ❌ **Missing:** Webhook table (no `webhooks` or `webhook_subscriptions` table for event registration)
- ❌ **Missing:** Webhook delivery log (no `webhook_deliveries` table for retry/audit)
- ❌ **Missing:** Rate limiting per API key (no `api_rate_limits` table)
- ❌ **Missing:** Zapier/Make integration (no Zapier app published)
- ❌ **Missing:** Google Calendar sync (no `calendar_sync_configs` table or iCal export)
- ❌ **Missing:** Google Drive integration (no `drive_configs` table — Supabase Storage is used instead)
- ❌ **Missing:** API documentation (no Swagger/OpenAPI spec published)

---

## Priority Matrix

| Feature | Impact | Effort | Priority | Phase | Data Ready? |
|---|---|---|---|---|---|
| WhatsApp-First Architecture | 🔴 Critical | M | P0 | Phase 1 | 🟡 Partial (WhatsApp provider exists for OTP only) |
| Smart Notifications (Actionable) | 🔴 Critical | M | P0 | Phase 1 | 🟡 Partial (NotificationPreferencesTable exists, missing quiet hours/priority/actions) |
| Fee Flexibility + Payment Gateway | 🔴 Critical | M | P0 | Phase 1 | 🔴 Minimal (FeeRecordsTable exists, no payment gateway, no installments) |
| Multi-Language Engine | 🔴 Critical | M | P0 | Phase 1 | 🟡 Partial (languagePref field exists, no translation API) |
| Parent Pulse (Gamification) | 🟡 High | M | P1 | Phase 2 | 🟡 Strong (ParentAchievementsTable has BADGE/MISSION types, missing score/streak logic) |
| Class Community Feed | 🟡 High | M | P1 | Phase 2 | 🟡 Partial (SchoolMediaTable + Supabase exist, missing feed post/reaction tables) |
| Teacher Copilot (AI) | 🟡 High | L | P1 | Phase 2 | 🟡 Partial (syllabus/assessment data exists, no AI integration or lesson plan table) |
| Parent-Facing Analytics | 🟡 High | M | P1 | Phase 2 | 🟡 Partial (all source data exists, missing aggregation/prediction logic) |
| Family Circle (Multi-Parent) | 🟡 High | M | P1 | Phase 2 | 🟡 Strong (relation + isPrimaryGuardian exist, missing visibility rules/delegation) |
| AI Report Card (Full) | 🟡 High | L | P1 | Phase 2 | 🟡 Partial (marks/attendance/achievements exist, no AI or template table) |
| VidyaSetu AI Tutor | 🟡 High | L | P2 | Phase 3 | 🟡 Partial (data moat exists, no AI integration or student app) |
| School Benchmarking | 🟡 High | M | P2 | Phase 3 | 🟡 Partial (all source data exists, no aggregation pipeline) |
| Marketplace (Full SRI + Admission) | 🟡 High | L | P2 | Phase 3 | 🟡 Partial (SchoolsTable rich, discovery API works, missing SRI/reviews/applications) |
| Safe Transport (Live Tracking) | 🟡 High | L | P2 | Phase 3 | 🔴 None (no transport tables exist at all) |
| One-Tap Lesson Capture | 🟠 Medium | M | P2 | Phase 3 | 🟡 Partial (homework tables exist, no STT/OCR integration) |
| Student Digital Identity | 🟠 Medium | L | P3 | Phase 4 | 🟡 Partial (student data exists, no cross-school ID or certificate table) |
| Transparency Dashboard | 🟠 Medium | M | P3 | Phase 4 | 🟡 Partial (most metrics calculable, missing grievance/expense data) |
| SOS & Safety Button | 🟠 Medium | M | P3 | Phase 4 | 🔴 Minimal (notification infra exists, no SOS/incident tables) |
| Festival & Cultural Calendar | 🟢 Low | S | P3 | Phase 4 | 🟡 Strong (CalendarEventsTable rich, SchoolsTable.state exists, missing templates) |
| School Branding Kit | 🟢 Low | M | P3 | Phase 4 | 🟡 Strong (philosophy/media/logo exist, missing QR/achievements/facilities) |
| Student Goals & Reflection | 🟢 Low | M | P3 | Phase 4 | 🔴 None (no student app, no goals/reflection tables) |
| Teacher Wellness Analytics | 🟢 Low | M | P3 | Phase 4 | 🟡 Partial (all source data exists, missing aggregation/wellness tables) |
| Fee Expense Transparency | 🟢 Low | S | P3 | Phase 4 | 🔴 None (no expense tracking module exists) |
| Regional Curriculum Alignment | 🟢 Low | M | P3 | Phase 4 | 🟡 Partial (board/grading fields exist, no grading scale templates) |
| Open API & Zapier | 🟢 Low | M | P3 | Phase 4 | 🟡 Partial (50+ endpoints exist, no API keys/webhooks/Zapier) |

---

## The "Why Vidya Prayag Wins" Summary

```
Traditional ERP          →    Vidya Prayag
─────────────────              ──────────────
Data storage                   Data intelligence
Notifications                  Actionable conversations
One parent                     Family circle
English-only                   Multi-language
Pay full fee                   Fee flexibility
Static report card             AI narrative + predictive
Isolated school                Benchmarked + transparent
Download app or miss out       WhatsApp-first, app-optional
Teacher types everything       Teacher speaks/snaps, AI handles
Student = data row             Student = digital citizen with passport
School finds parents           Parents find schools (marketplace)
```

**The flywheel:** More schools → better benchmarking → more parent trust → more parent engagement → better data → better AI insights → schools can't leave → more schools.

---

## Data Readiness Summary

### ✅ Strong Foundation (Data exists, minimal gaps)
| Feature | Key Existing Data |
|---|---|
| Parent Pulse (Gamification) | `ParentAchievementsTable` with BADGE/MISSION/COMPETENCY/EI_METRIC types |
| Family Circle | `ParentChildLinksTable.relation` + `isPrimaryGuardian` |
| Festival & Cultural Calendar | `CalendarEventsTable` (rich event model) + `SchoolsTable.state` |
| School Branding Kit | `SchoolPhilosophyTable` + `SchoolMediaTable` + `SchoolsTable.logoUrl/brandColor` |
| Smart Notifications | `NotificationPreferencesTable` + `NotificationsTable` (rich schema) |

### 🟡 Partial Foundation (Core data exists, significant additions needed)
| Feature | What Exists | What's Missing |
|---|---|---|
| AI Report Card | Marks, attendance, achievements | AI/LLM, report card templates, teacher remarks table |
| VidyaSetu AI Tutor | Marks, syllabus, homework, attendance | AI integration, student app, question bank, chat history |
| Teacher Copilot | Syllabus, assessments, marks | AI integration, lesson plans, question bank, auto-grading |
| Smart Attendance Insights | Attendance records (4-state) | Pattern detection, correlation engine, heatmap cache |
| Parent-Facing Analytics | All source data | Aggregation queries, prediction logic, peer percentiles |
| School Benchmarking | All source data per school | Cross-school aggregation, anonymization, peer groups |
| Marketplace/SRI | SchoolsTable (rich), discovery API | SRI score, reviews, seat availability, application workflow |
| Multi-Language Engine | `languagePref` field, school medium | Translation API, cache table, regional holiday mapping |
| Regional Curriculum | Board/grading fields, curriculum units | Grading scale templates, board-specific report formats |
| WhatsApp-First | WhatsApp provider (OTP), logs table | General notification templates, two-way messaging, group sync |
| Open API | 50+ REST endpoints, JWT auth | API keys, webhooks, Zapier, Google Calendar sync |
| Family Circle | relation + isPrimaryGuardian | Visibility rules, delegation, notification routing per role |
| Class Community Feed | SchoolMediaTable, Supabase | Feed post table, reaction table, class audience resolution |
| One-Tap Lesson Capture | Homework tables, attachments | STT/OCR integration, voice note storage, templates |
| Teacher Wellness | CheckIns, periods, assessments, messages | Workload calculation, activity log, burnout thresholds |
| Transparency Dashboard | Attendance, fees, PTM, messages | Grievance table, expense data, public metrics API |
| Student Digital Identity | Student data, marks, attendance, badges | Cross-school ID, TC table, certificate issuance, QR signing |

### 🔴 Minimal/No Foundation (Must build from scratch)
| Feature | What's Missing |
|---|---|
| Fee Payment Gateway | No payment gateway, no transaction ref, no installments, no receipts |
| Safe Transport | No transport tables at all (vehicles, routes, stops, drivers, GPS) |
| SOS & Safety Button | No SOS alert table, no incident log, no drill mode, no location capture |
| Fee Expense Transparency | No expense module at all (no expense categories, allocations, or tracking) |
| Student Goals & Reflection | No student app, no goals table, no reflection journal |

### 🔴 Cross-Cutting Missing Infrastructure
These are needed by multiple features and represent the biggest gaps:

1. **AI/LLM Integration** — No OpenAI/Gemini/Claude API key configured. Needed by: AI Report Card, VidyaSetu Tutor, Teacher Copilot, Smart Attendance Insights, Predictive Analytics
2. **Student App/Portal** — No student role in `AppUsersTable`. Needed by: VidyaSetu Tutor, Student Goals, Student Digital Identity, Online Assignments (student-facing)
3. **Payment Gateway** — No Razorpay/Stripe/UPI. Needed by: Fee Flexibility, Marketplace admission fee, WhatsApp payment
4. **Expense Module** — No expense tracking at all. Needed by: Fee Expense Transparency, Transparency Dashboard
5. **Transport Module** — No transport data infrastructure. Needed by: Safe Transport
6. **Report Card Templates** — No board-specific templates. Needed by: AI Report Card, Regional Curriculum Alignment, NEP 2020 compliance
7. **Webhook/API Key System** — No developer API access. Needed by: Open API, Zapier integration
