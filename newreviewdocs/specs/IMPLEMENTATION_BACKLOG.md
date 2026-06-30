# Vidya Prayag — Implementation Backlog & Dependency Graph

> **Generated:** 2026-06-27
> **Last Updated:** 2026-06-28
> **Source documents:** `feature_audit.csv`, `DIFFERENTIATING_FEATURES.md`, `COMPETITIVE_GAP_ANALYSIS.md`
> **Reference spec:** `MESSAGING_SYSTEM_SPEC.md`
> **Schema reference:** `server/.../db/Tables.kt` (70+ tables, Exposed ORM)

---

## 1. Consolidated Feature Inventory

Features requiring implementation specs, deduplicated across all three source documents and grouped by phase.

### Phase 0 — Survival / Platform Infrastructure (1-2 months)

| # | Spec File | Feature | Source | Status | Priority | Effort | Prerequisites |
|---|---|---|---|---|---|---|---|
| P0-01 | `FEE_PAYMENT_SPEC.md` | Online Fee Payment Gateway + Fee Flexibility | Audit L25, L135; Diff §7.1; Gap #1, #17 | 🔴 TODO (0%) | Critical | L | None |
| P0-02 | `WHATSAPP_INTEGRATION_SPEC.md` | WhatsApp Business API for General Notifications | Audit L143; Diff §10.1; Gap #5 | 🟡 Partial (40%) | Critical | M | None |
| P0-03 | `NEP_COMPLIANCE_SPEC.md` | NEP 2020 HPC + UDISE+ + Board Templates + Grading Scales | Gap #2, #3, #14, #15; Diff §8.3 | 🔴 Missing | Critical | L | None |
| P0-04 | `DPDP_COMPLIANCE_SPEC.md` | DPDP Act Compliance (Consent, Audit, Export, Erasure) | Gap #4; Audit L151 | 🔴 Missing | Critical | L | `AUDIT_LOG_SPEC.md` |
| P0-05 | `BULK_IMPORT_EXPORT_SPEC.md` | CSV Import/Export for Students, Teachers, Marks | Gap #12; Audit L159 | 🟡 Partial (30%) | High | M | None |
| P0-06 | `AI_INFRASTRUCTURE_SPEC.md` | LLM Integration Layer (Shared AI Service) | Diff §1.1-1.4; Gap AI features | 🔴 Missing | Critical | M | None |
| P0-07 | `OFFLINE_MODE_SPEC.md` | Full Offline-First Architecture | Audit L146; MESSAGING_SPEC §6 | 🟡 Partial (15%) | High | L | None |
| P0-08 | `DARK_MODE_SPEC.md` | Dark Theme + System-Aware Switching | Audit L147 | � Implemented (90%) | Medium | M | None |
| P0-09 | `TABLET_LAYOUT_SPEC.md` | Responsive Tablet Layouts | Audit L148 | 🟡 Partial (20%) | Medium | M | None |
| P0-10 | `WEB_APP_SPEC.md` | Web Application (wasmJs/js Target) | Audit L145 | 🟡 Partial (40%) | High | L | None |
| P0-11 | `IOS_PUSH_SPEC.md` | APNs Push for iOS | Audit L144 | 🔴 Missing | High | M | None |
| P0-12 | `TWO_FACTOR_AUTH_SPEC.md` | 2FA for Admin Accounts | Audit L149 | 🔴 Missing | High | M | None |
| P0-13 | `AUDIT_LOG_SPEC.md` | Activity Audit Trail | Audit L150 | 🔴 Missing | High | L | None |

### Phase 1 — Catch-Up: AI + Standard ERP Modules (2-4 months)

| # | Spec File | Feature | Source | Status | Priority | Effort | Prerequisites |
|---|---|---|---|---|---|---|---|
| P1-01 | `AI_EXAM_ANALYSIS_SPEC.md` | AI Exam Analysis (weak subjects, trends, feedback) | Gap #19; Diff §1.4 | 🔴 Missing | High | M | `AI_INFRASTRUCTURE_SPEC.md` |
| P1-02 | `AI_FEE_REMINDER_SPEC.md` | AI Fee Reminder Optimization | Gap #17; Diff §7.1 | 🔴 Missing | High | S | `AI_INFRASTRUCTURE_SPEC.md`, `FEE_PAYMENT_SPEC.md` |
| P1-03 | `AI_TIMETABLE_SPEC.md` | AI Timetable Auto-Generator | Gap #18 | 🔴 Missing | Medium | M | `AI_INFRASTRUCTURE_SPEC.md` |
| P1-04 | `AI_NL_QUERY_SPEC.md` | AI Natural Language Query | Gap #21 | 🔴 Missing | Medium | M | `AI_INFRASTRUCTURE_SPEC.md` |
| P1-05 | `AI_REPORT_CARD_SPEC.md` | AI Report Card (Narrative + Predictive + Assembly) | Audit L23, L107; Diff §1.2; Gap #23 | 🟠 Stub (20%) | High | L | `AI_INFRASTRUCTURE_SPEC.md`, `NEP_COMPLIANCE_SPEC.md` |
| P1-06 | `OCR_ADMISSION_SPEC.md` | OCR Admission Form Scanner | Gap #26 | 🔴 Missing | Medium | M | `AI_INFRASTRUCTURE_SPEC.md` |
| P1-07 | `TRANSPORT_TRACKING_SPEC.md` | GPS Bus Tracking + Transport Management | Audit L117-118; Diff §6.2; Gap #6, #46 | 🔴 Missing | High | L | None |
| P1-08 | `LIBRARY_MANAGEMENT_SPEC.md` | Library Book Management | Gap #7 | 🔴 Missing | Medium | L | None |
| P1-09 | `HOSTEL_MANAGEMENT_SPEC.md` | Hostel Management | Gap #8 | 🔴 Missing | Medium | L | None |
| P1-10 | `PAYROLL_MANAGEMENT_SPEC.md` | Staff Payroll + Payslips | Gap #9 | 🔴 Missing | Medium | L | None |
| P1-11 | `ID_CARD_GENERATION_SPEC.md` | ID Card Generation | Gap #11 | 🔴 Missing | Medium | S | None |
| P1-12 | `HEALTH_RECORDS_SPEC.md` | Student Health & Immunization Records | Audit L120; Gap #47 | � Implemented (90%) | High | L | None |
| P1-13 | `MULTI_BRANCH_SPEC.md` | Multi-Branch / School Chain Support | Gap #13; Audit L156 | 🔴 Missing | Medium | L | None |
| P1-14 | `EXPENSE_MANAGEMENT_SPEC.md` | School Expense Tracking | Audit L136; Gap #10 | 🔴 Missing | Medium | L | None |
| P1-15 | `INVENTORY_MANAGEMENT_SPEC.md` | School Inventory / Asset Tracking | Audit L139; Gap #49 | 🔴 Missing | Low | L | None |
| P1-16 | `EMAIL_INTEGRATION_SPEC.md` | Email Notifications (Extend SMTP) | Audit L142 | 🟡 Partial (30%) | Medium | M | None |
| P1-17 | `SCHEDULED_ANNOUNCEMENTS_SPEC.md` | Schedule Announcements for Future | Audit L160 | 🔴 Missing | Low | S | None |
| P1-18 | `BROADCAST_GROUPS_SPEC.md` | Admin-Managed Broadcast Groups | Audit L123 | 🟡 Partial (30%) | Medium | M | None |
| P1-19 | `EXAM_TIMETABLE_SPEC.md` | Exam Schedule Management | Audit L125 | 🔴 Missing | Medium | M | None |
| P1-20 | `LESSON_PLANNING_SPEC.md` | Teacher Lesson Planning | Audit L127 | � Implemented (90%) | Medium | M | None |
| P1-21 | `ONLINE_ASSIGNMENTS_SPEC.md` | Student-Facing Assignment Portal | Audit L128 | 🟡 Partial (40%) | High | L | `STUDENT_APP_SPEC.md` |
| P1-22 | `SCHOLARSHIP_WORKFLOW_SPEC.md` | Scholarship Application + Approval Workflow | Audit L138 | 🟡 Partial (40%) | Medium | M | None |
| P1-23 | `DOCUMENTS_MANAGEMENT_SPEC.md` | School Documents / Media Storage | Audit L69, L108 | 🟠 Stub (10%) | Medium | M | None |
| P1-24 | `ADMIN_NOTIFICATIONS_SPEC.md` | Admin Notification Composer UI | Audit L74, L109 | � Partial (50%) | Medium | M | None |
| P1-25 | `BIOMETRIC_IOS_SPEC.md` | iOS Biometric Authentication | Audit L11, L114 | 🟠 Stub (10%) | Medium | M | None |
| P1-26 | `CUSTOM_REPORT_BUILDER_SPEC.md` | Custom Report Generation | Audit L133 | 🔴 Missing | Medium | L | None |
| P1-27 | `CALENDAR_SYNC_SPEC.md` | Google/Outlook Calendar Sync | Audit L152; Diff §10.2 | 🔴 Missing | Low | S | None |
| P1-28 | `CURRICULUM_TEMPLATES_SPEC.md` | Pre-built Curriculum Templates by Board | Audit L153 | 🔴 Missing | Medium | L | `NEP_COMPLIANCE_SPEC.md` |
| P1-29 | `EVENT_REGISTRATION_SPEC.md` | Event RSVP / PTM Slot Booking | Audit L155 | 🟡 Partial (30%) | Medium | M | None |
| P1-30 | `VISITOR_MANAGEMENT_SPEC.md` | Visitor Check-in/Check-out | Audit L140; Gap #50 | 🔴 Missing | Low | M | None |
| P1-31 | `ALUMNI_MANAGEMENT_SPEC.md` | Alumni Network & Tracking | Audit L141; Gap #48 | � Implemented (85%) | Low | L | None |

### Phase 2 — Differentiate: Unique Features (4-8 months)

| # | Spec File | Feature | Source | Status | Priority | Effort | Prerequisites |
|---|---|---|---|---|---|---|---|
| P2-01 | `PARENT_PULSE_SPEC.md` | Parent Engagement Gamification | Diff §2.1; Gap #52 | � Implemented (85%) | High | M | None |
| P2-02 | `FAMILY_CIRCLE_SPEC.md` | Multi-Parent with Role-Based Visibility | Diff §2.2; Gap #53 | 🟡 Partial (multi-parent links + primary guardian exist; role-based visibility + delegation TODO) | High | M | None |
| P2-03 | `CLASS_COMMUNITY_FEED_SPEC.md` | Class-Scoped Social Feed | Diff §2.3; Gap #35 | 🔴 Missing | High | M | None |
| P2-04 | `SMART_NOTIFICATIONS_SPEC.md` | Actionable Notifications + Quiet Hours + Digest | Diff §2.4 | 🟡 Partial (65% — prefs filtering + rate limiting + scheduler + push bridge implemented; AI priority, batching, quiet hours, digest TODO) | High | M | None |
| P2-05 | `MULTI_LANGUAGE_SPEC.md` | Multi-Language Content Translation Engine | Audit L121; Diff §8.1; Gap #38 | 🔴 Missing | High | M | None |
| P2-06 | `MARKETPLACE_SPEC.md` | School Discovery Full Flow (SRI + Reviews + Admission) | Audit L33, L110; Diff §3.1 | 🟡 Partial (40%) | High | L | None |
| P2-07 | `PARENT_ANALYTICS_SPEC.md` | Parent-Facing Analytics (Growth, Radar, Predictive) | Diff §9.1 | 🔴 Missing | High | M | None |
| P2-08 | `FORMS_SURVEYS_SPEC.md` | Forms, Surveys, Permission Slips, Polls | Audit L154; Gap #36 | 🔴 Missing | Medium | M | None |
| P2-09 | `NEWSLETTER_BUILDER_SPEC.md` | Drag-and-Drop Newsletter Builder | Gap #37 | 🔴 Missing | Medium | M | None |
| P2-10 | `FESTIVAL_CULTURAL_CALENDAR_SPEC.md` | Festival & Cultural Calendar Integration | Diff §8.2; Gap #56 | 🔴 Missing | Low | S | None |
| P2-11 | `SCHOOL_BRANDING_KIT_SPEC.md` | School Digital Presence + QR Codes | Diff §3.2 | 🟡 Partial (media exists) | Low | M | None |

### Phase 3 — Leapfrog: Blue Ocean Features (8-12 months)

| # | Spec File | Feature | Source | Status | Priority | Effort | Prerequisites |
|---|---|---|---|---|---|---|---|
| P3-01 | `VIDYASETU_AI_TUTOR_SPEC.md` | AI Tutor Integrated with School Data | Diff §1.1; Gap #30 | 🔴 Missing | High | L | `AI_INFRASTRUCTURE_SPEC.md`, `STUDENT_APP_SPEC.md` |
| P3-02 | `TEACHER_COPILOT_SPEC.md` | AI Lesson Plans, Assessment Generation, Auto-Grading | Diff §1.3; Gap #22 | 🔴 Missing | High | L | `AI_INFRASTRUCTURE_SPEC.md` |
| P3-03 | `VIDYA_PASSPORT_SPEC.md` | Portable Student Digital Identity | Diff §5.1; Gap #51 | 🔴 Missing | Medium | L | `STUDENT_APP_SPEC.md` |
| P3-04 | `SCHOOL_BENCHMARKING_SPEC.md` | Cross-School Anonymous Analytics | Diff §9.2; Gap #54 | 🔴 Missing | Medium | M | `MULTI_BRANCH_SPEC.md` |
| P3-05 | `STUDENT_WELLNESS_SPEC.md` | Student Mental Health, Mood Tracking, AI Chatbot | Gap #42 | 🔴 Missing | High | L | `AI_INFRASTRUCTURE_SPEC.md`, `STUDENT_APP_SPEC.md` |
| P3-06 | `TRANSPARENCY_DASHBOARD_SPEC.md` | Public School Health Metrics | Diff §6.1; Gap #58 | 🔴 Missing | Medium | M | None |
| P3-07 | `TEACHER_WELLNESS_SPEC.md` | Teacher Workload & Burnout Analytics | Diff §4.2; Gap #57 | 🔴 Missing | Low | M | None |
| P3-08 | `FEE_EXPENSE_TRANSPARENCY_SPEC.md` | "Where Does My Fee Go" Breakdown | Diff §7.2; Gap #55 | 🔴 Missing | Low | S | `EXPENSE_MANAGEMENT_SPEC.md` |
| P3-09 | `STUDENT_GOALS_SPEC.md` | Student Goals & Self-Reflection | Diff §5.2; Gap #60 | 🔴 Missing | Low | M | `STUDENT_APP_SPEC.md` |
| P3-10 | `VOICE_PHOTO_CAPTURE_SPEC.md` | One-Tap Voice/Photo Lesson Capture | Diff §4.1; Gap #39 | 🔴 Missing | Medium | M | None |
| P3-11 | `SOS_SAFETY_SPEC.md` | SOS & Safety Button + Incident Logging | Diff §6.3 | 🔴 Missing | Medium | M | None |

### Phase 4 — Future: Emerging Tech (12+ months)

| # | Spec File | Feature | Source | Status | Priority | Effort | Prerequisites |
|---|---|---|---|---|---|---|---|
| P4-01 | `FACIAL_RECOGNITION_SPEC.md` | Facial Recognition Attendance + Face Login | Gap #27, #28, #50 | 🔴 Missing | Medium | L | None |
| P4-02 | `LIVE_CLASSES_SPEC.md` | Live Classes / LMS Integration | Gap #32 | 🔴 Missing | Medium | L | `STUDENT_APP_SPEC.md` |
| P4-03 | `BEHAVIOR_TRACKING_SPEC.md` | Student Behavior Points & Incidents | Audit L130; Gap #33 | 🔴 Missing | Medium | M | None |
| P4-04 | `STUDENT_PORTFOLIO_SPEC.md` | Student Digital Portfolios | Audit L131; Gap #34 | 🔴 Missing | Medium | L | `STUDENT_APP_SPEC.md` |
| P4-05 | `STUDENT_APP_SPEC.md` | Student-Facing App/Portal | Audit L131; Diff §5 | 🔴 Missing | High | L | None |
| P4-06 | `VOICE_VIDEO_CALLS_SPEC.md` | In-App Voice/Video Calls | Audit L122 | 🔴 Missing | Medium | L | None |
| P4-07 | `APaar_ID_SPEC.md` | APAAR ID Integration | Gap #43 | 🔴 Missing | Medium | M | `NEP_COMPLIANCE_SPEC.md` |
| P4-08 | `OPEN_API_SPEC.md` | Open API + Webhooks + Zapier Integration | Diff §10.2; Gap #47 | 🔴 Missing | Low | M | None |
| P4-09 | `AI_SPOKEN_ENGLISH_SPEC.md` | AI Spoken English Practice | Gap #31 | 🔴 Missing | Low | M | `AI_INFRASTRUCTURE_SPEC.md`, `STUDENT_APP_SPEC.md` |
| P4-10 | `AI_QUESTION_BANK_SPEC.md` | AI Question Bank Generation | Gap #22 | 🔴 Missing | Low | M | `AI_INFRASTRUCTURE_SPEC.md` |
| P4-11 | `MULTI_SCHOOL_MANAGEMENT_SPEC.md` | Multi-Campus / Franchise Management | Audit L156 | 🔴 Missing | Medium | L | `MULTI_BRANCH_SPEC.md` |

---

## 2. Dependency Graph

```
┌─────────────────────────────────────────────────────────────────────┐
│                    PHASE 0 — INFRASTRUCTURE                         │
│                                                                     │
│  AUDIT_LOG ──┬──> DPDP_COMPLIANCE                                   │
│              │                                                      │
│  AI_INFRASTRUCTURE ──┬──> all AI_* specs                            │
│                       │                                              │
│  FEE_PAYMENT ──┬──> AI_FEE_REMINDER                                 │
│                │                                                     │
│  NEP_COMPLIANCE ──┬──> AI_REPORT_CARD                                │
│                   ├──> CURRICULUM_TEMPLATES                          │
│                   ├──> APAAR_ID                                      │
│                   │                                                  │
│  WHATSAPP_INTEGRATION    OFFLINE_MODE    DARK_MODE    TABLET_LAYOUT  │
│  BULK_IMPORT_EXPORT      WEB_APP        IOS_PUSH     TWO_FACTOR_AUTH │
└─────────────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    PHASE 1 — CATCH-UP                               │
│                                                                     │
│  AI_INFRASTRUCTURE ──┬──> AI_EXAM_ANALYSIS                           │
│                       ├──> AI_FEE_REMINDER (also needs FEE_PAYMENT)  │
│                       ├──> AI_TIMETABLE                              │
│                       ├──> AI_NL_QUERY                               │
│                       ├──> AI_REPORT_CARD (also needs NEP_COMPLIANCE)│
│                       └──> OCR_ADMISSION                             │
│                                                                     │
│  TRANSPORT_TRACKING   LIBRARY_MGMT   HOSTEL_MGMT   PAYROLL_MGMT     │
│  ID_CARD_GENERATION   HEALTH_RECORDS EXPENSE_MGMT  INVENTORY_MGMT   │
│  MULTI_BRANCH ──> MULTI_SCHOOL_MANAGEMENT                           │
│  EMAIL_INTEGRATION    SCHEDULED_ANNOUNCEMENTS   BROADCAST_GROUPS    │
│  EXAM_TIMETABLE       LESSON_PLANNING    ONLINE_ASSIGNMENTS          │
│  SCHOLARSHIP_WORKFLOW DOCUMENTS_MGMT   ADMIN_NOTIFICATIONS          │
│  BIOMETRIC_IOS        CUSTOM_REPORT_BUILDER   CALENDAR_SYNC          │
│  EVENT_REGISTRATION   VISITOR_MGMT   ALUMNI_MGMT                    │
└─────────────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    PHASE 2 — DIFFERENTIATE                          │
│                                                                     │
│  PARENT_PULSE    FAMILY_CIRCLE    CLASS_COMMUNITY_FEED               │
│  SMART_NOTIFICATIONS    MULTI_LANGUAGE    MARKETPLACE                │
│  PARENT_ANALYTICS    FORMS_SURVEYS    NEWSLETTER_BUILDER             │
│  FESTIVAL_CULTURAL_CALENDAR    SCHOOL_BRANDING_KIT                   │
└─────────────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    PHASE 3 — LEAPFROG                               │
│                                                                     │
│  STUDENT_APP ──┬──> VIDYASETU_AI_TUTOR (also needs AI_INFRA)        │
│                ├──> VIDYA_PASSPORT                                   │
│                ├──> STUDENT_WELLNESS (also needs AI_INFRA)          │
│                ├──> STUDENT_GOALS                                    │
│                ├──> STUDENT_PORTFOLIO                                │
│                └──> LIVE_CLASSES                                     │
│                                                                     │
│  AI_INFRASTRUCTURE ──> TEACHER_COPILOT                               │
│  EXPENSE_MANAGEMENT ──> FEE_EXPENSE_TRANSPARENCY                     │
│  MULTI_BRANCH ──> SCHOOL_BENCHMARKING                                │
│  TRANSPARENCY_DASHBOARD    TEACHER_WELLNESS                          │
│  VOICE_PHOTO_CAPTURE    SOS_SAFETY                                   │
└─────────────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    PHASE 4 — FUTURE                                 │
│                                                                     │
│  FACIAL_RECOGNITION    BEHAVIOR_TRACKING    VOICE_VIDEO_CALLS       │
│  AI_INFRASTRUCTURE ──┬──> AI_SPOKEN_ENGLISH (also needs STUDENT_APP)│
│                       └──> AI_QUESTION_BANK                          │
│  NEP_COMPLIANCE ──> APAAR_ID                                         │
│  OPEN_API                                                            │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 3. Recommended Implementation Order

### Wave 1: Foundation (Weeks 1-4)

| Order | Spec | Rationale |
|---|---|---|
| 1 | `AUDIT_LOG_SPEC.md` | Required by DPDP compliance; foundational for all subsequent features |
| 2 | `AI_INFRASTRUCTURE_SPEC.md` | Unblocks all AI features; shared LLM service layer |
| 3 | `FEE_PAYMENT_SPEC.md` | #1 competitive gap; revenue-critical; every competitor has this |
| 4 | `WHATSAPP_INTEGRATION_SPEC.md` | #1 communication channel for Indian parents; existing provider to extend |
| 5 | `NEP_COMPLIANCE_SPEC.md` | Government mandate; unblocks report cards, APAAR ID |
| 6 | `DPDP_COMPLIANCE_SPEC.md` | Legal requirement; depends on audit log |
| 7 | `BULK_IMPORT_EXPORT_SPEC.md` | Data migration efficiency; needed for onboarding new schools |

### Wave 2: Platform (Weeks 3-6, parallel with Wave 1)

| Order | Spec | Rationale |
|---|---|---|
| 8 | `OFFLINE_MODE_SPEC.md` | Architecture-level; messaging spec already defines SQLDelight approach |
| 9 | `IOS_PUSH_SPEC.md` | iOS users have no push; critical for iOS launch |
| 10 | `TWO_FACTOR_AUTH_SPEC.md` | Admin account security |
| 11 | `DARK_MODE_SPEC.md` | Quick win; user preference |
| 12 | `TABLET_LAYOUT_SPEC.md` | Important for teacher tablets |
| 13 | `WEB_APP_SPEC.md` | Long-running; can parallelize |

### Wave 3: AI Catch-Up (Weeks 5-12, after AI_INFRASTRUCTURE)

| Order | Spec | Rationale |
|---|---|---|
| 14 | `AI_EXAM_ANALYSIS_SPEC.md` | Uses existing marks data; high impact |
| 15 | `AI_REPORT_CARD_SPEC.md` | Replaces existing stub; needs NEP compliance |
| 16 | `AI_FEE_REMINDER_SPEC.md` | Needs fee payment; quick win with AI |
| 17 | `AI_TIMETABLE_SPEC.md` | Complex but high value |
| 18 | `AI_NL_QUERY_SPEC.md` | Admin productivity |
| 19 | `OCR_ADMISSION_SPEC.md` | Admission efficiency |

### Wave 4: Standard ERP Modules (Weeks 7-14, parallel)

| Order | Spec | Rationale |
|---|---|---|
| 20 | `TRANSPORT_TRACKING_SPEC.md` | Safety; standard in Indian ERPs |
| 21 | `HEALTH_RECORDS_SPEC.md` | Student safety; compliance |
| 22 | `EXPENSE_MANAGEMENT_SPEC.md` | Financial management; unblocks fee transparency |
| 23 | `LIBRARY_MANAGEMENT_SPEC.md` | Standard module |
| 24 | `PAYROLL_MANAGEMENT_SPEC.md` | Standard HR module |
| 25 | `ID_CARD_GENERATION_SPEC.md` | Quick win |
| 26 | `MULTI_BRANCH_SPEC.md` | Scalability; unblocks benchmarking |
| 27 | Remaining modules (hostel, inventory, visitor, alumni) | Lower priority |

### Wave 5: Differentiation (Weeks 12-24)

| Order | Spec | Rationale |
|---|---|---|
| 28 | `SMART_NOTIFICATIONS_SPEC.md` | High impact; existing prefs table to extend |
| 29 | `PARENT_PULSE_SPEC.md` | Blue ocean; existing achievements table |
| 30 | `FAMILY_CIRCLE_SPEC.md` | Cultural insight; existing relation field |
| 31 | `CLASS_COMMUNITY_FEED_SPEC.md` | Parent engagement; existing media infra |
| 32 | `MULTI_LANGUAGE_SPEC.md` | Existing languagePref field |
| 33 | `MARKETPLACE_SPEC.md` | Existing discovery to extend |
| 34 | `PARENT_ANALYTICS_SPEC.md` | All source data exists |
| 35 | `FORMS_SURVEYS_SPEC.md` | Paperless data collection |
| 36 | Remaining (newsletter, festival, branding kit) | Lower priority |

### Wave 6: Leapfrog (Weeks 20-36)

| Order | Spec | Rationale |
|---|---|---|
| 37 | `STUDENT_APP_SPEC.md` | Unblocks AI tutor, passport, wellness, goals, portfolio |
| 38 | `TEACHER_COPILOT_SPEC.md` | High teacher value |
| 39 | `VIDYASETU_AI_TUTOR_SPEC.md` | Data moat; needs student app |
| 40 | `SOS_SAFETY_SPEC.md` | Safety critical |
| 41 | `TRANSPARENCY_DASHBOARD_SPEC.md` | Trust building |
| 42 | `VOICE_PHOTO_CAPTURE_SPEC.md` | Teacher productivity |
| 43 | Remaining (passport, benchmarking, wellness, goals, fee transparency) | Lower priority |

### Wave 7: Future (Months 12+)

| Order | Spec | Rationale |
|---|---|---|
| 44 | `FACIAL_RECOGNITION_SPEC.md` | Emerging tech; needs hardware |
| 45 | `BEHAVIOR_TRACKING_SPEC.md` | ClassDojo core feature |
| 46 | `OPEN_API_SPEC.md` | Platform play |
| 47 | `LIVE_CLASSES_SPEC.md` | Needs student app |
| 48 | Remaining future specs | As needed |

---

## 4. Cross-Cutting Infrastructure Dependencies

These are shared infrastructure components needed by multiple features:

| Infrastructure | Needed By | Spec |
|---|---|---|
| **AI/LLM Service** | AI Exam Analysis, AI Report Card, AI Fee Reminder, AI Timetable, AI NL Query, OCR Admission, AI Tutor, Teacher Copilot, AI Spoken English, AI Question Bank | `AI_INFRASTRUCTURE_SPEC.md` |
| **Payment Gateway** | Fee Payment, Fee Flexibility, Marketplace admission fee, WhatsApp payment | `FEE_PAYMENT_SPEC.md` |
| **Audit Log** | DPDP Compliance, all admin actions, data access tracking | `AUDIT_LOG_SPEC.md` |
| **Offline Sync Engine** | Messaging (already speced), all portal features offline | `OFFLINE_MODE_SPEC.md` |
| **Student App/Role** | AI Tutor, Student Goals, Student Wellness, Vidya Passport, Student Portfolio, Live Classes, Online Assignments, AI Spoken English | `STUDENT_APP_SPEC.md` |
| **Expense Module** | Fee Expense Transparency, Transparency Dashboard | `EXPENSE_MANAGEMENT_SPEC.md` |
| **Multi-Branch** | Multi-School Management, School Benchmarking | `MULTI_BRANCH_SPEC.md` |
| **WhatsApp Business API** | General notifications, two-way messaging, group sync, payment links | `WHATSAPP_INTEGRATION_SPEC.md` |
| **NEP 2020 Templates** | AI Report Card, Board-specific report cards, APAAR ID, Curriculum Templates | `NEP_COMPLIANCE_SPEC.md` |

---

## 5. Existing Architecture Summary

| Layer | Technology | Key Files |
|---|---|---|
| **Backend** | Kotlin + Ktor 3.4.3 (Netty), Exposed ORM 0.50.0 | `server/.../Application.kt` |
| **Database** | PostgreSQL (Supabase prod) / H2 (dev), HikariCP pool 5 | `server/.../db/Tables.kt` |
| **Shared client** | KMP (Android + iOS + JVM + JS + wasmJs) | `shared/.../di/Koin.kt` |
| **Compose UI** | Compose Multiplatform 1.10.3 | `composeApp/.../ui/v2/` |
| **Auth** | JWT HS256 (15-min access, 30-day rotated refresh), OTP (6 providers) | `server/.../core/JwtConfig.kt` |
| **Notifications** | FCM (Firebase Admin SDK 9.4.3), NotificationsTable, DeviceTokensTable | `server/.../feature/notifications/` |
| **Media** | Supabase Storage REST, multipart upload | `server/.../feature/media/` |
| **DI** | Koin 4.0.0 | `shared/.../di/Koin.kt` |
| **Networking** | Ktor Client 3.4.3 (OkHttp/Darwin/CIO), NetworkResult<T> | `shared/.../core/network/` |
| **Design System** | V Design System (VTheme, VColors, VDimens, VType, VAtoms) | `composeApp/.../ui/v2/components/` |

### Existing Tables (70+)

`AppUsersTable`, `AuthOtpsTable`, `OtpDeliveryAttemptsTable`, `UserSessionsTable`, `LandingContentTable`, `AppConfigTable`, `SchoolsTable`, `OnboardingDraftsTable`, `SchoolClassesTable`, `SchoolSubjectsTable`, `TeacherSubjectAssignmentsTable`, `AnnouncementsTable`, `WhatsappLogsTable`, `AdmissionEnquiriesTable`, `SchoolPhilosophyTable`, `SchoolMediaTable`, `StorageMetricsTable`, `AcademicCalendarTable`, `HolidayListTable` (deprecated), `FacultyTable`, `AttendanceRecordsTable`, `StudentsTable`, `EnrollmentsTable`, `ChildrenTable`, `FeeRecordsTable`, `LeaveRequestsTable`, `PtmEventsTable`, `PtmClassProgressTable`, `MessageThreadsTable`, `MessagesTable`, `ConversationSeqTable`, `MessageStatusTable`, `MessageAttachmentsTable`, `ExamResultsTable` (deprecated), `AssessmentsTable`, `AssessmentMarksTable`, `SyllabusUnitsTable`, `CurriculumUnitsTable`, `SyllabusProgressTable`, `HomeworkTable`, `HomeworkAttachmentsTable`, `HomeworkSubmissionsTable`, `HomeworkExtensionsTable`, `TeacherPeriodsTable`, `PeriodExceptionsTable`, `ScholarshipsTable`, `ScholarshipApplicationsTable`, `NotificationsTable`, `DeviceTokensTable`, `OtpGatewayDevicesTable`, `SmsRequestsTable`, `ParentChildLinksTable`, `NonTeachingStaffTable`, `ParentAchievementsTable`, `CalendarEventsTable`, `AcademicYearsTable`, `TeacherCheckInsTable`, `NotificationPreferencesTable`, `LessonPlansTable`, `LessonPlanTemplatesTable`, `LessonPlanAttachmentsTable`, `StudentHealthProfilesTable`, `StudentImmunizationsTable`, `StudentHealthIncidentsTable`, `ParentPulsesTable`, `AlumniTable`, `AlumniDonationCampaignsTable`, `AlumniDonationsTable`, `AlumniMentorshipRequestsTable`, `AlumniMentorshipsTable`, `AlumniCareerHistoryTable`, `AlumniMentorshipSettingsTable`

---

## 6. Specification Count Summary

| Phase | Spec Count | Effort Distribution |
|---|---|---|
| Phase 0 — Infrastructure | 13 | 7L, 4M, 2S, 1 implemented (dark mode) |
| Phase 1 — Catch-Up | 31 | 12L, 14M, 2S, 3 implemented (health, lesson planning, alumni) |
| Phase 2 — Differentiate | 11 | 1L, 8M, 2S, 1 implemented (parent pulse), 2 partial (family circle, smart notifications) |
| Phase 3 — Leapfrog | 11 | 3L, 7M, 1S |
| Phase 4 — Future | 11 | 4L, 6M, 1S |
| **Total** | **77** | **5 implemented, 2 partial** |
