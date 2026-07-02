/**
 * Admin API types, mirror the Ktor server DTOs verbatim (snake_case keys as
 * serialized by kotlinx.serialization @SerialName). Source of truth:
 *   server/src/main/kotlin/.../feature/school/**  +  feature/notifications/**
 *
 * Nothing here is invented. Every field maps to a real serialized response key.
 */

// ── Auth ──────────────────────────────────────────────────────────────────
export interface AuthTokenResponse {
  token: string;
  refresh_token: string;
  user_id: string;
  name: string;
  role: string;
  profile_completed: boolean;
  must_change_password?: boolean;
}

// ── Notifications (GET /api/v1/notifications, /summary) ──────────────────────
export interface NotificationDto {
  id: string;
  category: string;
  title: string;
  body: string;
  time: string;
  unread: boolean;
}
export interface NotificationsDataDto {
  notifications: NotificationDto[];
  unread_count: number;
}
export interface NotificationsSummaryDto {
  unread_count: number;
}

// ── Analytics overview (GET /api/v1/school/analytics/overview) ───────────────
export interface AnalyticsCard {
  title?: string;
  value?: string;
  delta?: string;
  caption?: string;
  [k: string]: unknown;
}
export interface AnalyticsOverviewResponse {
  performance_trend: number[];
  trend_labels: string[];
  current_growth: string;
  cards: AnalyticsCard[];
  insights: unknown[];
}

// ── Attendance summary (GET /api/v1/school/attendance/summary) ───────────────
// Keys are the EXACT wire format kotlinx.serialization emits (@SerialName in
// SchoolRecordsRouting.kt). The client returns env.data verbatim with no
// case transform, so these MUST be snake_case to read at runtime.
export interface AttendanceClassRow {
  grade: string;
  present: number;
  absent: number;
  late: number;
  total: number;
  rate: number;
}
export interface AttendanceSummaryDto {
  latest_date: string | null;
  present: number;
  absent: number;
  late: number;
  total: number;
  rate: number;
  by_class: AttendanceClassRow[];
}

// ── Marks summary (GET /api/v1/school/marks/summary) ─────────────────────────
export interface MarksAssessmentRow {
  subject: string;
  assessment: string;
  class_name: string;
  average: number;
  max_marks: number;
  graded_count: number;
  exam_date: string | null;
  is_published: boolean;
}
export interface MarksSummaryDto {
  assessment_count: number;
  overall_average_pct: number;
  assessments: MarksAssessmentRow[];
}

// ── Fee ledger (GET /api/v1/school/fees/ledger) ──────────────────────────────
export interface FeeRow {
  title: string;
  amount: number;
  currency: string;
  status: string;
  due_date: string | null;
  category: string | null;
}
export interface FeeLedgerDto {
  paid_total: number;
  due_total: number;
  overdue_total: number;
  paid_count: number;
  due_count: number;
  overdue_count: number;
  currency: string;
  recent: FeeRow[];
}

// ── Students (GET/POST/DELETE /api/v1/school/students) ───────────────────────
export interface StudentDto {
  id: string;
  student_code: string;
  full_name: string;
  class_name: string;
  section: string;
  roll_number: string;
  profile_photo_url: string | null;
}
export interface StudentListResponse {
  students: StudentDto[];
}
export interface CreateStudentRequest {
  full_name: string;
  class_name: string;
  section?: string;
  roll_number: string;
  student_code?: string;
}
export interface BulkImportRowResult {
  row: number;
  success: boolean;
  student_code?: string | null;
  error?: string | null;
}
export interface BulkImportStudentsResult {
  total: number;
  inserted: number;
  failed: number;
  results: BulkImportRowResult[];
}

// ── Teachers (GET/POST/DELETE /api/v1/school/teachers) ───────────────────────
export interface TeacherAccountDto {
  id: string;
  name: string;
  email: string | null;
  phone: string | null;
  role: string;
  schoolId: string;
}
export interface TeacherListResponse {
  teachers: TeacherAccountDto[];
}
export interface CreateTeacherRequest {
  name: string;
  identifier: string;
  initial_password?: string;
}

// ── Announcements (GET/POST /api/v1/school/announcements) ────────────────────
export interface AnnouncementDto {
  type: string;
  event_id: string;
  title: string;
  sub_title: string | null;
  description: string;
  event_image: string | null;
  date: string;
  audience_type: string;
  audience_filter: unknown;
}
export interface AnnouncementsListResponse {
  announcements: AnnouncementDto[];
}
export interface CreateAnnouncementRequest {
  type: string;
  title: string;
  sub_title?: string | null;
  description: string;
  event_image?: string | null;
  date: string;
  audience_type?: string;
  audience_filter?: unknown;
  scheduled_at?: string | null;
}

// ── Leave requests (GET/PATCH /api/v1/school/leave-requests) ─────────────────
export interface LeaveRequestDto {
  id: string;
  requester_name: string;
  requester_role: string;
  date_from: string;
  date_to: string;
  date_range: string;
  reason: string;
  image_url: string | null;
  status: string;
}
export interface LeaveRequestListResponse {
  type: string;
  approval_rate: number;
  weekly_count: number;
  requests: LeaveRequestDto[];
}

// ── School profile (GET/PUT /api/v1/school/profile) ──────────────────────────
export interface SchoolProfileDto {
  id: string;
  name: string;
  board: string;
  medium: string;
  school_gender: string;
  contact_phone: string | null;
  contact_email: string | null;
  principal_name: string | null;
  principal_phone: string | null;
  principal_email: string | null;
  full_address: string | null;
  city: string;
  district: string;
  state: string;
  pincode: string | null;
  logo_url: string | null;
  brand_color: string;
  latitude: number | null;
  longitude: number | null;
  school_type: string | null;
  affiliation_number: string | null;
  year_established: number | null;
  website: string | null;
  total_students: number | null;
  total_classes: number | null;
  academic_year_start_month: string | null;
  grading_system: string | null;
}
export type UpdateSchoolProfileRequest = Partial<
  Omit<SchoolProfileDto, "id">
>;

// ── Dashboard intelligence (GET /api/v1/school/dashboard/intelligence) ───────
// Command Center payload. Every field is computed server-side from real tables
// (see SchoolIntelligenceRouting.kt). Nothing here is fabricated.
export interface IntelligenceMeta {
  school_name: string;
  academic_week: number;
  today: string;
  attendance_as_of: string | null;
}
export interface AttendancePoint {
  date: string;
  rate: number;
  present: number;
  absent: number;
  total: number;
  is_anomaly: boolean;
  exam: string | null;
}
export interface RiskSignal {
  kind: "attendance" | "marks" | "leave";
  label: string;
  severity: number;
}
export interface EarlyWarningStudent {
  student_code: string;
  name: string;
  class_name: string;
  section: string;
  attendance_pct: number | null;
  marks_pct: number | null;
  leave_count: number;
  risk_level: "high" | "medium" | "watch";
  signals: RiskSignal[];
}
export interface HealthCell {
  subject: string;
  percentage: number;
  covered_units: number;
  total_units: number;
}
export interface HealthRow {
  class_name: string;
  cells: HealthCell[];
  class_average: number;
}
export interface AcademicHealth {
  subjects: string[];
  rows: HealthRow[];
}
export interface ActivityItem {
  id: string;
  category: string;
  actor: string;
  action: string;
  target: string;
  iso_time: string;
}
export interface DashboardIntelligenceDto {
  meta: IntelligenceMeta;
  attendance_timeline: AttendancePoint[];
  early_warning: EarlyWarningStudent[];
  academic_health: AcademicHealth;
  activity_feed: ActivityItem[];
}

// ── Timetable (GET /api/v1/school/timetable) ─────────────────────────────────
// School-wide weekly schedule for the Command Center calendar. Source of truth:
// server/.../feature/school/SchoolTimetableRouting.kt (reads teacher_periods,
// joined to app_users for teacher_name). The timetable is a RECURRING WEEKLY
// PATTERN keyed by weekday (1=Mon..7=Sun); the client paints it onto the dates
// of the week it is viewing. Honest empty payload when no timetable entered.
export interface TimetablePeriod {
  id: string;
  start_time: string; // "HH:mm"
  end_time: string; // "HH:mm"
  class_name: string;
  section: string;
  subject: string;
  room: string;
  teacher_id: string;
  teacher_name: string; // "" when unassigned/unknown
}
export interface TimetableWeekday {
  weekday: number; // 1=Mon … 7=Sun (java.time.DayOfWeek.value)
  periods: TimetablePeriod[];
}
export interface TimetableDto {
  weekdays: TimetableWeekday[];
  classes: string[]; // distinct class_name present, sorted — feeds class filters
}

// ── Academic calendar (GET /api/v1/school/calendar) ──────────────────────────
// Date-specific events/holidays/exams layered onto the recurring timetable.
// Source: server/.../feature/school/SchoolRouting.kt (academic_calendar +
// holidays). Keys are the exact @SerialName wire format.
export interface CalendarEventDto {
  date: string; // YYYY-MM-DD
  day: string;
  event_id: string;
  event_title: string;
  event_description: string;
}
export interface CalendarSummaryDto {
  working_days: number;
  total_working_days: number;
  public_holidays: number;
  school_holidays: number;
}
export interface CalendarResponse {
  calendar_events: CalendarEventDto[];
  summary: CalendarSummaryDto;
}

// ── Daily attendance (GET /api/v1/school/attendance/daily) ───────────────────
// On-demand read for the calendar slot drill-down: present vs enrolled for a
// class on a date, and whether attendance was marked. Source: SchoolRouting.kt.
// status ∈ "present" | "absent" | "late" | "half_day".
export interface AttendanceDailyEntry {
  profile_pic: string | null;
  name: string;
  id: string;
  status: string;
}
export interface AttendanceDailyResponse {
  type: string;
  grade: string | null;
  present_count: number;
  absent_count: number;
  total_count: number;
  attendance_percentage: string;
  attendance_list: AttendanceDailyEntry[];
}

// ── Onboarding status (GET /api/v1/onboarding/status) ────────────────────────
export interface OnboardingStatusResponse {
  school_id: string | null;
  is_complete: boolean;
  completion_percent: number;
  resume_step: string;
  total_step_count: number;
  steps: { step: string; current_step_count: number; is_done: boolean }[];
}

// ── Dev Tools (super_admin only) ─────────────────────────────────────────────
export interface OtpProviderInfo {
  name: string;
  channel: string;
  configured: boolean;
}
export interface OtpProvidersResponse {
  providers: OtpProviderInfo[];
  envPinnedProvider: string;
  runtimeOverride: string | null;
  effectiveProvider: string;
}
export interface UpdateOtpProviderResponse {
  provider: string;
  isOverride: boolean;
}
export interface TriggerPulseResponse {
  weekStart: string;
  pulsesGenerated: number;
}
export interface DevSendNotificationResponse {
  sent: boolean;
}

// ── Alumni Management (ALUMNI_MANAGEMENT_SPEC.md) ───────────────────────────
export interface AlumniDto {
  id: string;
  schoolId: string;
  studentId: string | null;
  userId: string | null;
  name: string;
  graduationYear: number;
  lastClass: string | null;
  currentProfession: string | null;
  company: string | null;
  city: string | null;
  email: string | null;
  phone: string | null;
  linkedinUrl: string | null;
  photoUrl: string | null;
  skills: string | null;
  achievements: string | null;
  isMentor: boolean;
  mentorExpertise: string | null;
  isFeatured: boolean;
  verificationStatus: string;
  verifiedAt: string | null;
  showPhone: boolean;
  showEmail: boolean;
  showLinkedin: boolean;
  visibilityLevel: string;
  profileCompleteness: number;
  lastActiveAt: string | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  careerHistory: CareerHistoryDto[];
}
export interface CareerHistoryDto {
  id: string;
  alumniId: string;
  jobTitle: string;
  company: string;
  industry: string | null;
  startDate: string | null;
  endDate: string | null;
  isCurrent: boolean;
  createdAt: string;
}
export interface AlumniListResponse {
  alumni: AlumniDto[];
  page: number;
  limit: number;
  total: number;
}
export interface AlumniCampaignDto {
  id: string;
  schoolId: string;
  title: string;
  description: string | null;
  cause: string | null;
  targetAmount: number;
  amountRaised: number;
  targetBatchYear: number | null;
  startDate: string;
  endDate: string | null;
  status: string;
  isActive: boolean;
  donorCount: number;
  createdAt: string;
  updatedAt: string;
}
export interface AlumniDonationDto {
  id: string;
  schoolId: string;
  alumniId: string;
  alumniName: string;
  campaignId: string | null;
  campaignTitle: string | null;
  amount: number;
  purpose: string | null;
  donationDate: string;
  paymentMode: string | null;
  referenceNumber: string | null;
  receiptNumber: string | null;
  receiptIssued: boolean;
  is80gEligible: boolean;
  createdAt: string;
}
export interface AlumniAnalyticsDto {
  totalAlumni: number;
  activeAlumni: number;
  pendingVerifications: number;
  byGraduationYear: Record<string, number>;
  byProfession: Record<string, number>;
  byCity: Record<string, number>;
  totalDonations: number;
  donationCount: number;
  activeCampaigns: number;
  activeMentorships: number;
  mentorshipRequestsPending: number;
  engagementRate: number;
}

export interface AlumniMentorshipDto {
  id: string;
  schoolId: string;
  alumniId: string;
  alumniName: string;
  studentId: string;
  studentName: string;
  requestId: string | null;
  status: string;
  startDate: string;
  endDate: string | null;
  notes: string | null;
  sessionCount: number;
  createdAt: string;
}

export interface AlumniMentorshipRequestDto {
  id: string;
  schoolId: string;
  alumniId: string;
  alumniName: string;
  studentId: string;
  studentName: string;
  requestedBy: string;
  requestedByName: string;
  expertiseArea: string | null;
  message: string | null;
  status: string;
  respondedAt: string | null;
  createdAt: string;
}

// ──────────────────────────────────────────────────────────────────────────────
// PEWS — Predictive Early Warning System
// Mirrors server feature.pews.PewsRouting.kt DTOs (snake_case JSON) exactly, so
// the same envelope (.data) decodes here. AI fields are nullable; the UI shows
// them only when present and never fabricates a reason/number (LAW 6).
// ──────────────────────────────────────────────────────────────────────────────

export type PewsRiskLevel = "watch" | "medium" | "high";
export type PewsInterventionStatus = "open" | "in_progress" | "done" | "dismissed";
export type PewsOutcome = "improved" | "unchanged" | "worsened";

export interface PewsSignal {
  kind: string;
  label: string;
  severity: number; // 1..3
}

export interface PewsStudent {
  student_code: string;
  name: string;
  class_name: string;
  section: string;
  run_date: string;
  risk_score: number;
  risk_level: PewsRiskLevel;
  attendance_pct: number | null;
  marks_pct: number | null;
  leave_count: number;
  attendance_slope: number | null;
  marks_slope: number | null;
  signals: PewsSignal[];
  ai_narrative: string | null;
  ai_cause: string | null;
  ai_recommendation: string | null;
  ai_provider_used: string | null;
}

export interface PewsCohort {
  run_date: string | null;
  total: number;
  high: number;
  medium: number;
  watch: number;
  students: PewsStudent[];
  ai_enabled: boolean;
}

export interface PewsStudentDetail {
  current: PewsStudent | null;
  history: PewsStudent[];
}

export interface PewsIntervention {
  id: string;
  student_code: string;
  name: string;
  class_name: string;
  section: string;
  owner_user_id: string;
  action_type: string;
  status: PewsInterventionStatus;
  notes: string | null;
  outcome: PewsOutcome | null;
  opened_at: string;
  resolved_at: string | null;
  // PEWS 2.0 — managed casework fields
  escalation_level?: number;
  sla_days?: number | null;
  follow_up_date?: string | null;
  urgency?: string | null;
  cause_family?: string | null;
  plan_json?: string | null;
}

export interface UpdatePewsInterventionRequest {
  status?: PewsInterventionStatus;
  notes?: string;
  outcome?: PewsOutcome;
  action_type?: string;
}

export interface PewsEffectiveness {
  total: number;
  open: number;
  done: number;
  dismissed: number;
  improved: number;
  unchanged: number;
  worsened: number;
}

export interface PewsConfig {
  use_relative_thresholds: boolean;
  attendance_floor_pct: number;
  marks_floor_pct: number;
  leave_floor_count: number;
  run_frequency: string; // "daily" | "weekly"
  ai_narrative_enabled: boolean;
  parent_share_enabled: boolean;
}

export interface PewsRunResult {
  at_risk: number;
}

export interface PewsJobStatus {
  job_id: string;
  status: string; // queued|processing|completed|failed
  total_items: number;
  completed_items: number;
  result: string | null;
  created_at: string;
  completed_at: string | null;
}

export interface PewsTrendPoint {
  run_date: string;
  total: number;
  high: number;
  medium: number;
  watch: number;
}

export interface PewsEffectivenessTrend {
  points: PewsTrendPoint[];
  effectiveness: PewsEffectiveness;
}

export interface PewsDraftMessage {
  language: string;
  body: string;
}

export interface PewsSendParentResult {
  sent_count: number;
}

// ──────────────────────────────────────────────────────────────────────────────
// AI REPORT CARD 2.0
// Mirrors server feature.reportcard.assemble.AssembleRouting.kt +
// feature.reportcard.learn.LearnRouting.kt + EcosystemRouting.kt DTOs.
// ──────────────────────────────────────────────────────────────────────────────

export interface ReportCardDraft {
  id: string;
  studentId: string;
  className: string;
  section: string;
  term: string;
  academicYearId: string | null;
  aiDraft: string | null;
  classContext: string | null;
  status: string;
  aiProviderUsed: string | null;
  tokensUsed: number;
  language: string;
  groundingFlags: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ReportCardBatchResult {
  jobId: string;
  totalStudents: number;
  completed: number;
  failed: number;
  grounded: number;
  flagged: number;
  fallbackUsed: number;
  errors: string[];
}

export interface ReportCardJobStatus {
  jobId: string;
  status: string;
  totalItems: number;
  completedItems: number;
  result: string | null;
  createdAt: string;
  completedAt: string | null;
}

export interface ReportCardOversightRow {
  className: string;
  section: string;
  term: string;
  totalDrafts: number;
  draftCount: number;
  flaggedCount: number;
  approvedCount: number;
  publishedCount: number;
}

export interface ReportCardOversightSummary {
  schoolId: string;
  classes: ReportCardOversightRow[];
}

export interface ReportCardPublishRequest {
  className: string;
  section: string;
  term: string;
  academicYearId?: string | null;
}

export interface ReportCardPublishResult {
  published: number;
}

export interface ReportCardEffectivenessReport {
  focusArea: string;
  studentsTargeted: number;
  studentsImproved: number;
  effectivenessScore: number;
  confidence: string;
}

export interface ReportCardTermConfig {
  currentTerm: string | null;
  termWindowDays: number;
  enabled: boolean;
  batchConcurrency: number;
  fallbackOnAiFail: boolean;
}

// ──────────────────────────────────────────────────────────────────────────────
// AI TUTOR 2.0
// Mirrors server feature.tutor.heatmap.TeacherHeatmapRouting.kt +
// feature.tutor.learn.LearnRouting.kt DTOs.
// ──────────────────────────────────────────────────────────────────────────────

export interface TutorHeatmapCell {
  topicId: string;
  misconceptionType: string;
  affectedChildren: number;
  avgMastery: number;
  severity: string;
}

export interface TutorHeatmapResponse {
  classId: string;
  subjectId: string;
  cells: TutorHeatmapCell[];
  totalChildren: number;
  totalMisconceptions: number;
}

export interface TutorTeacherScopeClass {
  classId: string;
  className: string;
  section: string;
  subjects: { subjectId: string; subjectName: string }[];
}

export interface TutorTeacherScopeResponse {
  classes: TutorTeacherScopeClass[];
}

export interface TutorEfficacyTopic {
  topicId: string;
  mastery: number;
  verdict: string;
}

export interface TutorEfficacyResponse {
  childId: string;
  subjectId: string;
  topics: TutorEfficacyTopic[];
}

// ──────────────────────────────────────────────────────────────────────────────
// AI TOKEN MONITOR (Dev Tools — super admin only)
// Mirrors server feature.ai.AiRouting.kt DTOs for rate-limiter, health, usage.
// ──────────────────────────────────────────────────────────────────────────────

export interface AiRateLimitEntry {
  provider: string;
  model: string;
  rpm_current: number;
  rpm_limit: number;
  rpd_current: number;
  rpd_limit: number;
  tpm_current: number;
  tpm_limit: number;
  reserve_pct: number;
}

export interface AiHealthEntry {
  provider: string;
  model: string;
  state: string;
  total_requests: number;
  total_failures: number;
  rate_limit_hits: number;
  avg_latency_ms: number;
}

export interface AiRecentUsageEntry {
  id: string;
  feature: string;
  provider_used: string | null;
  model_used: string | null;
  input_tokens: number;
  output_tokens: number;
  status: string;
  routing_decision: string;
  latency_ms: number;
  error_message: string | null;
  created_at: string;
}

export interface AiRecentUsageResponse {
  entries: AiRecentUsageEntry[];
  total: number;
  window_min: number;
}

// ── School Day Configuration (GET/POST/PUT/DELETE /api/v1/school/day-config) ──

export interface SchoolDaySlotDto {
  slot_index: number;
  slot_type: string;
  label: string;
  start_time: string;
  end_time: string;
  is_double: boolean;
  double_group: number;
}

export interface SchoolDayConfigDto {
  id: string;
  name: string;
  applicable_days: string;
  class_level: string;
  slots: SchoolDaySlotDto[];
  is_active: boolean;
}

export interface SchoolDayConfigListResponse {
  configs: SchoolDayConfigDto[];
}

export interface CreateSchoolDayConfigRequest {
  name: string;
  applicable_days: string;
  class_level: string;
  slots: SchoolDaySlotDto[];
}

export interface UpdateSchoolDayConfigRequest {
  name: string;
  applicable_days: string;
  class_level: string;
  slots: SchoolDaySlotDto[];
  is_active: boolean;
}

// ── School Classes & Subjects (SchoolClassesRouting.kt) ──────────────────────
export interface SchoolClassDto {
  id: string;
  code: string;
  name: string;
  sections: string[];
  subject_count: number;
}
export interface SchoolClassListResponse {
  classes: SchoolClassDto[];
}
export interface CreateSchoolClassRequest {
  code: string;
  name: string;
  sections: string[];
}
export interface UpdateSchoolClassRequest {
  code: string;
  name: string;
  sections: string[];
}
export interface SchoolSubjectDto {
  id: string;
  class_id: string;
  name: string;
  code: string;
}
export interface SchoolSubjectListResponse {
  subjects: SchoolSubjectDto[];
}
export interface CreateSchoolSubjectRequest {
  name: string;
  code: string;
}
export interface UpdateSchoolSubjectRequest {
  name: string;
  code: string;
}

// ── Timetable Periods (SchoolTimetableRouting.kt) ────────────────────────────
export interface PeriodDetailDto {
  id: string;
  teacher_id: string;
  assignment_id: string | null;
  weekday: number;
  start_time: string;
  end_time: string;
  class_name: string;
  section: string;
  subject: string;
  room: string;
  is_active: boolean;
  valid_from?: string | null;
  valid_to?: string | null;
}
export interface CreatePeriodRequest {
  teacher_id: string;
  class_name: string;
  section: string;
  subject: string;
  weekday: number;
  start_time: string;
  end_time: string;
  room?: string;
  valid_from?: string | null;
  valid_to?: string | null;
}
export interface UpdatePeriodRequest {
  weekday?: number;
  start_time?: string;
  end_time?: string;
  room?: string;
  is_active?: boolean;
  valid_from?: string | null;
  valid_to?: string | null;
}
export interface BulkPeriodItem {
  teacher_id: string;
  class_name: string;
  section: string;
  subject: string;
  start_time: string;
  end_time: string;
  room?: string;
}
export interface BulkCreatePeriodsRequest {
  weekday: number;
  periods: BulkPeriodItem[];
}
export interface BulkCreatePeriodsResponse {
  created: PeriodDetailDto[];
  errors: string[];
  created_count: number;
  error_count: number;
}
