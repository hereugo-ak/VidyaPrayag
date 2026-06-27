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
