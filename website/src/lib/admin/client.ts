"use client";

/**
 * Authenticated admin client over the Ktor backend. Reuses the website's
 * envelope contract ({ success, message, data }). On a 401 it performs a
 * one-shot refresh via /api/v1/auth/refresh (rotating tokens), persists the new
 * pair, and retries once. A second failure clears the session, the route guard
 * then redirects to /login.
 *
 * Every endpoint here is school_id-scoped on the server from the JWT subject
 * (core/SchoolAccess.kt → requireSchoolContext), so there is zero cross-tenant
 * leakage and the client never sends a school_id.
 */

import { API_BASE_URL, ApiError, type ApiEnvelope } from "@/lib/api";
import { eraseSession, patchTokens, readSession } from "./session";
import type {
  AnalyticsOverviewResponse,
  AnnouncementsListResponse,
  AttendanceSummaryDto,
  AuthTokenResponse,
  BulkImportStudentsResult,
  CreateAnnouncementRequest,
  CreateStudentRequest,
  CreateTeacherRequest,
  FeeLedgerDto,
  LeaveRequestListResponse,
  MarksSummaryDto,
  NotificationsDataDto,
  NotificationsSummaryDto,
  OnboardingStatusResponse,
  SchoolProfileDto,
  StudentListResponse,
  TeacherAccountDto,
  TeacherListResponse,
  UpdateSchoolProfileRequest,
  AnnouncementDto,
  LeaveRequestDto,
  StudentDto,
  DashboardIntelligenceDto,
  TimetableDto,
  CalendarResponse,
  AttendanceDailyResponse,
  OtpProvidersResponse,
  UpdateOtpProviderResponse,
  TriggerPulseResponse,
  DevSendNotificationResponse,
  AlumniDto,
  AlumniListResponse,
  AlumniCampaignDto,
  AlumniDonationDto,
  AlumniAnalyticsDto,
  AlumniMentorshipDto,
  AlumniMentorshipRequestDto,
  PewsCohort,
  PewsStudentDetail,
  PewsIntervention,
  UpdatePewsInterventionRequest,
  PewsEffectiveness,
  PewsConfig,
  PewsRunResult,
  PewsJobStatus,
  PewsEffectivenessTrend,
  PewsRiskLevel,
  PewsInterventionStatus,
  PewsDraftMessage,
  PewsSendParentResult,
  ReportCardOversightSummary,
  ReportCardPublishRequest,
  ReportCardPublishResult,
  ReportCardEffectivenessReport,
  ReportCardTermConfig,
  TutorTeacherScopeResponse,
  TutorHeatmapResponse,
  AiRateLimitEntry,
  AiHealthEntry,
  AiRecentUsageResponse,
} from "./types";

interface Opts {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  body?: unknown;
  signal?: AbortSignal;
}

let refreshing: Promise<boolean> | null = null;

async function doRefresh(): Promise<boolean> {
  // De-dupe concurrent refreshes so a burst of 401s rotates the token once.
  if (refreshing) return refreshing;
  refreshing = (async () => {
    const s = readSession();
    if (!s?.refreshToken) return false;
    try {
      const res = await fetch(`${API_BASE_URL}/api/v1/auth/refresh`, {
        method: "POST",
        headers: { "Content-Type": "application/json", Platform: "web" },
        body: JSON.stringify({ refresh_token: s.refreshToken }),
      });
      if (!res.ok) return false;
      const env = (await res.json()) as ApiEnvelope<AuthTokenResponse>;
      const data = env.data;
      if (!data?.token) return false;
      patchTokens(data.token, data.refresh_token);
      return true;
    } catch {
      return false;
    } finally {
      refreshing = null;
    }
  })();
  return refreshing;
}

async function rawRequest<T>(
  path: string,
  opts: Opts,
  token: string
): Promise<{ ok: boolean; status: number; env: ApiEnvelope<T> | null }> {
  const headers: Record<string, string> = {
    Accept: "application/json",
    Platform: "web",
    Authorization: `Bearer ${token}`,
  };
  if (opts.body !== undefined) headers["Content-Type"] = "application/json";
  const res = await fetch(`${API_BASE_URL}${path}`, {
    method: opts.method ?? "GET",
    headers,
    body: opts.body !== undefined ? JSON.stringify(opts.body) : undefined,
    cache: "no-store",
    signal: opts.signal,
  });
  let env: ApiEnvelope<T> | null = null;
  const text = await res.text();
  if (text) {
    try {
      env = JSON.parse(text) as ApiEnvelope<T>;
    } catch {
      env = null;
    }
  }
  return { ok: res.ok, status: res.status, env };
}

/** Authed request with transparent single-retry refresh. */
export async function authRequest<T>(path: string, opts: Opts = {}): Promise<T> {
  const s = readSession();
  if (!s?.token) {
    throw new ApiError("Your session has ended. Please sign in again.", 401, "NO_SESSION");
  }

  let attempt = await rawRequest<T>(path, opts, s.token);

  if (attempt.status === 401) {
    const refreshed = await doRefresh();
    if (refreshed) {
      const s2 = readSession();
      if (s2?.token) attempt = await rawRequest<T>(path, opts, s2.token);
    }
    if (attempt.status === 401) {
      eraseSession();
      throw new ApiError("Your session has ended. Please sign in again.", 401, "SESSION_EXPIRED");
    }
  }

  const { ok, status, env } = attempt;
  if (!ok || (env && env.success === false)) {
    throw new ApiError(env?.message ?? `Request failed (${status}).`, status, env?.error_code);
  }
  return (env?.data ?? (env as unknown)) as T;
}

// ── Typed endpoints ──────────────────────────────────────────────────────────

export const adminApi = {
  // dashboard data sources
  onboardingStatus: () => authRequest<OnboardingStatusResponse>("/api/v1/onboarding/status"),
  analyticsOverview: () => authRequest<AnalyticsOverviewResponse>("/api/v1/school/analytics/overview"),
  attendanceSummary: () => authRequest<AttendanceSummaryDto>("/api/v1/school/attendance/summary"),
  marksSummary: () => authRequest<MarksSummaryDto>("/api/v1/school/marks/summary"),
  feeLedger: () => authRequest<FeeLedgerDto>("/api/v1/school/fees/ledger"),
  // Command Center intelligence, one read assembles attendance timeline +
  // anomalies + exam overlay, early-warning students, academic-health grid,
  // and the institutional activity feed. All server-computed from real tables.
  dashboardIntelligence: () =>
    authRequest<DashboardIntelligenceDto>("/api/v1/school/dashboard/intelligence"),

  // signature calendar — school-wide weekly schedule (all classes) from the
  // new GET /api/v1/school/timetable; optional class pre-filter (Control A/B).
  timetable: (className?: string) => {
    const qs = className ? `?class=${encodeURIComponent(className)}` : "";
    return authRequest<TimetableDto>(`/api/v1/school/timetable${qs}`);
  },
  // date-specific events/holidays/exams layered onto the recurring timetable.
  calendar: (date?: string, viewType: "week" | "month" = "week") => {
    const params = new URLSearchParams();
    if (date) params.set("date", date);
    params.set("view_type", viewType);
    return authRequest<CalendarResponse>(`/api/v1/school/calendar?${params.toString()}`);
  },
  // ON-DEMAND read for the calendar slot drill-down: present vs enrolled for a
  // class on a date, and whether attendance was marked. Not a polling hook —
  // called directly when a slot panel opens (see CalendarSlotPanel).
  attendanceDaily: (type: "student" | "faculty", grade: string, date?: string) => {
    const params = new URLSearchParams();
    params.set("type", type);
    if (type === "student") params.set("grade", grade);
    if (date) params.set("date", date);
    return authRequest<AttendanceDailyResponse>(
      `/api/v1/school/attendance/daily?${params.toString()}`
    );
  },

  // notifications (real-time bell + activity feed)
  notifications: () => authRequest<NotificationsDataDto>("/api/v1/notifications"),
  notificationsSummary: () => authRequest<NotificationsSummaryDto>("/api/v1/notifications/summary"),
  markNotificationRead: (id: string) =>
    authRequest<unknown>(`/api/v1/notifications/${id}/read`, { method: "PATCH" }),
  markAllNotificationsRead: () =>
    authRequest<unknown>("/api/v1/notifications/read-all", { method: "POST" }),

  // people, students
  students: (q?: string, klass?: string) => {
    const params = new URLSearchParams();
    if (q) params.set("q", q);
    if (klass) params.set("class", klass);
    const qs = params.toString();
    return authRequest<StudentListResponse>(`/api/v1/school/students${qs ? `?${qs}` : ""}`);
  },
  createStudent: (body: CreateStudentRequest) =>
    authRequest<StudentDto>("/api/v1/school/students", { method: "POST", body }),
  deleteStudent: (id: string) =>
    authRequest<unknown>(`/api/v1/school/students/${id}`, { method: "DELETE" }),
  bulkImportStudents: (students: CreateStudentRequest[]) =>
    authRequest<BulkImportStudentsResult>("/api/v1/school/students/import", {
      method: "POST",
      body: { students },
    }),
  importStudentsCsv: (csv: string) =>
    authRequest<BulkImportStudentsResult>("/api/v1/school/students/import", {
      method: "POST",
      body: { csv },
    }),

  // people, teachers
  teachers: () => authRequest<TeacherListResponse>("/api/v1/school/teachers"),
  createTeacher: (body: CreateTeacherRequest) =>
    authRequest<TeacherAccountDto>("/api/v1/school/teachers", { method: "POST", body }),
  deleteTeacher: (id: string) =>
    authRequest<unknown>(`/api/v1/school/teachers/${id}`, { method: "DELETE" }),
  resetTeacherPassword: (id: string) =>
    authRequest<unknown>(`/api/v1/school/teachers/${id}/reset-password`, { method: "POST" }),

  // announcements
  announcements: () => authRequest<AnnouncementsListResponse>("/api/v1/school/announcements"),
  createAnnouncement: (body: CreateAnnouncementRequest) =>
    authRequest<AnnouncementDto>("/api/v1/school/announcements", { method: "POST", body }),

  // leave
  leaveRequests: (type?: string, status?: string) => {
    const params = new URLSearchParams();
    if (type) params.set("type", type);
    if (status) params.set("status", status);
    const qs = params.toString();
    return authRequest<LeaveRequestListResponse>(
      `/api/v1/school/leave-requests${qs ? `?${qs}` : ""}`
    );
  },
  setLeaveStatus: (id: string, status: "Approved" | "Rejected" | "Pending") =>
    authRequest<LeaveRequestDto>(`/api/v1/school/leave-requests/${id}/status`, {
      method: "PATCH",
      body: { status },
    }),

  // settings / institutional profile
  schoolProfile: () => authRequest<SchoolProfileDto>("/api/v1/school/profile"),
  updateSchoolProfile: (body: UpdateSchoolProfileRequest) =>
    authRequest<SchoolProfileDto>("/api/v1/school/profile", { method: "PUT", body }),

  // dev tools (super_admin only)
  otpProviders: () => authRequest<OtpProvidersResponse>("/api/v1/admin/dev/otp-providers"),
  updateOtpProvider: (provider: string) =>
    authRequest<UpdateOtpProviderResponse>("/api/v1/admin/dev/otp-provider", { method: "PUT", body: { provider } }),
  triggerPulse: () =>
    authRequest<TriggerPulseResponse>("/api/v1/admin/dev/trigger-pulse", { method: "POST" }),
  devSendNotification: (body: { user_id: string; title: string; body: string; deep_link?: string; category?: string; school_id?: string }) =>
    authRequest<DevSendNotificationResponse>("/api/v1/admin/dev/send-notification", { method: "POST", body }),

  // alumni management
  alumniList: (params?: { year?: number; profession?: string; city?: string; q?: string; page?: number; limit?: number }) => {
    const qs = new URLSearchParams();
    if (params?.year) qs.set("year", String(params.year));
    if (params?.profession) qs.set("profession", params.profession);
    if (params?.city) qs.set("city", params.city);
    if (params?.q) qs.set("q", params.q);
    qs.set("page", String(params?.page ?? 1));
    qs.set("limit", String(params?.limit ?? 20));
    return authRequest<AlumniListResponse>(`/api/v1/school/alumni?${qs.toString()}`);
  },
  alumniGet: (id: string) =>
    authRequest<AlumniDto>(`/api/v1/school/alumni/${id}`),
  alumniCreate: (body: Record<string, unknown>) =>
    authRequest<AlumniDto>("/api/v1/school/alumni", { method: "POST", body }),
  alumniUpdate: (id: string, body: Record<string, unknown>) =>
    authRequest<AlumniDto>(`/api/v1/school/alumni/${id}`, { method: "PATCH", body }),
  alumniDeactivate: (id: string) =>
    authRequest<unknown>(`/api/v1/school/alumni/${id}/deactivate`, { method: "PATCH" }),
  alumniVerify: (id: string, action: "approve" | "decline") =>
    authRequest<AlumniDto>(`/api/v1/school/alumni/${id}/verify`, { method: "PATCH", body: { action } }),
  alumniToggleFeatured: (id: string) =>
    authRequest<AlumniDto>(`/api/v1/school/alumni/${id}/feature`, { method: "PATCH" }),
  alumniPending: () =>
    authRequest<AlumniDto[]>("/api/v1/school/alumni/pending"),
  alumniCampaigns: () =>
    authRequest<AlumniCampaignDto[]>("/api/v1/school/alumni/campaigns"),
  alumniCampaignCreate: (body: Record<string, unknown>) =>
    authRequest<AlumniCampaignDto>("/api/v1/school/alumni/campaigns", { method: "POST", body }),
  alumniCampaignGet: (id: string) =>
    authRequest<AlumniCampaignDto>(`/api/v1/school/alumni/campaigns/${id}`),
  alumniCampaignUpdate: (id: string, status: string) =>
    authRequest<AlumniCampaignDto>(`/api/v1/school/alumni/campaigns/${id}`, { method: "PATCH", body: { status } }),
  alumniDonations: (campaignId?: string, alumniId?: string) => {
    const qs = new URLSearchParams();
    if (campaignId) qs.set("campaign_id", campaignId);
    if (alumniId) qs.set("alumni_id", alumniId);
    return authRequest<AlumniDonationDto[]>(`/api/v1/school/alumni/donations${qs.toString() ? `?${qs.toString()}` : ""}`);
  },
  alumniDonationCreate: (body: Record<string, unknown>) =>
    authRequest<AlumniDonationDto>("/api/v1/school/alumni/donations", { method: "POST", body }),
  alumniAnalytics: () =>
    authRequest<AlumniAnalyticsDto>("/api/v1/school/alumni/analytics/overview"),
  alumniEngagement: () =>
    authRequest<Record<string, unknown>>("/api/v1/school/alumni/analytics/engagement"),
  alumniDonationAnalytics: () =>
    authRequest<Record<string, unknown>>("/api/v1/school/alumni/analytics/donations"),
  alumniCareerAnalytics: () =>
    authRequest<Record<string, unknown>>("/api/v1/school/alumni/analytics/career"),
  alumniReceipt: (donationId: string) =>
    `/api/v1/school/alumni/donations/${donationId}/receipt`,
  alumniForm10BD: (year?: number) =>
    `/api/v1/school/alumni/donations/80g/form10bd${year ? `?year=${year}` : ""}`,
  alumniMentorships: () =>
    authRequest<AlumniMentorshipDto[]>("/api/v1/school/alumni/mentorships"),
  alumniMentorshipRequests: () =>
    authRequest<AlumniMentorshipRequestDto[]>("/api/v1/school/alumni/mentorship-requests"),
  alumniMentorshipRequestOverride: (requestId: string, action: string) =>
    authRequest<AlumniMentorshipRequestDto>(`/api/v1/school/alumni/mentorship-requests/${requestId}`, { method: "PATCH", body: { action } }),

  // ── PEWS (Predictive Early Warning System) ───────────────────────────────────
  // The admin is the owner of the whole Sense→Reason→Act→Learn loop. Every route
  // is school-scoped from the JWT (requireSchoolAdmin) — the client never sends a
  // school_id. AI narrative fields are nullable and shown only when present.
  pewsCohort: (minLevel?: PewsRiskLevel) => {
    const qs = minLevel ? `?min_level=${encodeURIComponent(minLevel)}` : "";
    return authRequest<PewsCohort>(`/api/v1/school/pews/cohort${qs}`);
  },
  pewsStudent: (studentCode: string) =>
    authRequest<PewsStudentDetail>(`/api/v1/school/pews/student/${encodeURIComponent(studentCode)}`),
  pewsInterventions: (status?: PewsInterventionStatus) => {
    const qs = status ? `?status=${encodeURIComponent(status)}` : "";
    return authRequest<PewsIntervention[]>(`/api/v1/school/pews/interventions${qs}`);
  },
  // Server now returns the full updated intervention DTO (richer contract), so
  // callers can update their list in place; we still re-fetch for simplicity.
  pewsUpdateIntervention: (id: string, body: UpdatePewsInterventionRequest) =>
    authRequest<PewsIntervention>(`/api/v1/school/pews/interventions/${id}`, { method: "PATCH", body }),
  pewsEffectiveness: () =>
    authRequest<PewsEffectiveness>("/api/v1/school/pews/effectiveness"),
  pewsConfig: () => authRequest<PewsConfig>("/api/v1/school/pews/config"),
  pewsUpdateConfig: (body: PewsConfig) =>
    authRequest<PewsConfig>("/api/v1/school/pews/config", { method: "PUT", body }),
  pewsRun: () => authRequest<PewsRunResult>("/api/v1/school/pews/run", { method: "POST" }),
  pewsJobStatus: (jobId: string) =>
    authRequest<PewsJobStatus>(`/api/v1/school/pews/run/${encodeURIComponent(jobId)}`),
  pewsTrend: (days?: number) => {
    const qs = days ? `?days=${days}` : "";
    return authRequest<PewsEffectivenessTrend>(`/api/v1/school/pews/trend${qs}`);
  },
  pewsDraftMessage: (interventionId: string, lang: string = "en") =>
    authRequest<PewsDraftMessage>(`/api/v1/school/pews/interventions/${encodeURIComponent(interventionId)}/draft-message?lang=${encodeURIComponent(lang)}`, { method: "POST" }),
  pewsSendParentMessage: (interventionId: string) =>
    authRequest<PewsSendParentResult>(`/api/v1/school/pews/interventions/${encodeURIComponent(interventionId)}/send-parent-message`, { method: "POST" }),

  // ── AI Report Card 2.0 ─────────────────────────────────────────────────────
  // Admin (school-scoped) endpoints for oversight, publishing, and effectiveness.
  reportCardOversight: (term: string, academicYearId?: string) => {
    const qs = new URLSearchParams({ term });
    if (academicYearId) qs.set("academicYearId", academicYearId);
    return authRequest<ReportCardOversightSummary>(`/api/v1/report-card/oversight?${qs.toString()}`);
  },
  reportCardPublish: (body: ReportCardPublishRequest) =>
    authRequest<ReportCardPublishResult>("/api/v1/report-card/publish", { method: "POST", body }),
  reportCardEffectiveness: () =>
    authRequest<ReportCardEffectivenessReport[]>("/api/v1/report-card/learn/effectiveness"),
  reportCardTermConfig: () =>
    authRequest<ReportCardTermConfig>("/api/v1/report-card/term-config"),

  // ── AI Tutor 2.0 ───────────────────────────────────────────────────────────
  // Teacher/admin heatmap scope + heatmap data. The server routes are JWT-scoped
  // (teacher context), so the admin sees the same data as a teacher for their
  // assigned classes.
  tutorTeacherScope: () =>
    authRequest<TutorTeacherScopeResponse>("/api/v1/tutor/heatmap/scope"),
  tutorHeatmap: (classId: string, subjectId: string) =>
    authRequest<TutorHeatmapResponse>(`/api/v1/tutor/heatmap/${encodeURIComponent(classId)}/${encodeURIComponent(subjectId)}`),

  // ── AI Token Monitor (Dev Tools — super admin only) ────────────────────────
  aiRateLimits: () =>
    authRequest<AiRateLimitEntry[]>("/api/v1/admin/ai/rate-limits"),
  aiHealth: () =>
    authRequest<AiHealthEntry[]>("/api/v1/admin/ai/health"),
  aiRecentUsage: (limit: number = 50, windowMin: number = 60) =>
    authRequest<AiRecentUsageResponse>(`/api/v1/admin/ai/recent-usage?limit=${limit}&window=${windowMin}`),
};
