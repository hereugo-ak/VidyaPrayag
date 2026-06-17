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
};
