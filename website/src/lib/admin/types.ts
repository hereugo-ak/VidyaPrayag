/**
 * Admin API types — mirror the Ktor server DTOs verbatim (snake_case keys as
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
export interface AttendanceClassRow {
  grade: string;
  present: number;
  absent: number;
  late: number;
  total: number;
  rate: number;
}
export interface AttendanceSummaryDto {
  date: string | null;
  present: number;
  absent: number;
  late: number;
  total: number;
  rate: number;
  byClass: AttendanceClassRow[];
}

// ── Marks summary (GET /api/v1/school/marks/summary) ─────────────────────────
export interface MarksAssessmentRow {
  subject: string;
  assessmentName: string;
  className: string;
  average: number;
  maxMarks: number;
  gradedCount: number;
  examDate: string | null;
  isPublished: boolean;
}
export interface MarksSummaryDto {
  assessmentCount: number;
  overallAveragePct: number;
  assessments: MarksAssessmentRow[];
}

// ── Fee ledger (GET /api/v1/school/fees/ledger) ──────────────────────────────
export interface FeeRow {
  title: string;
  amount: number;
  currency: string;
  status: string;
  dueDate: string | null;
  category: string | null;
}
export interface FeeLedgerDto {
  paidTotal: number;
  dueTotal: number;
  overdueTotal: number;
  paidCount: number;
  dueCount: number;
  overdueCount: number;
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

// ── Onboarding status (GET /api/v1/onboarding/status) ────────────────────────
export interface OnboardingStatusResponse {
  school_id: string | null;
  is_complete: boolean;
  completion_percent: number;
  resume_step: string;
  total_step_count: number;
  steps: { step: string; current_step_count: number; is_done: boolean }[];
}
