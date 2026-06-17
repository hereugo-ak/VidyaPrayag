"use client";

/**
 * Data hooks, SWR with a per-metric refresh strategy (documented in
 * ARCHITECTURE.md §Real-time). The Ktor backend exposes REST aggregates (no
 * Supabase realtime channel is published to the web client), so the right tool
 * is SWR polling tuned per metric type:
 *
 *   LIVE        notifications bell + activity feed, today's attendance
 *               → 15s poll + revalidateOnFocus (feels real-time)
 *   NEAR-LIVE   analytics overview, leave queue
 *               → 60s poll
 *   SLOW        fee ledger, marks summary, institutional profile
 *               → 5min poll, focus-revalidate only
 *
 * All requests go through the authed client, so a 401 transparently refreshes
 * the JWT before the hook ever sees an error.
 */

import useSWR, { type SWRConfiguration } from "swr";
import { adminApi } from "./client";

const LIVE: SWRConfiguration = {
  refreshInterval: 15_000,
  revalidateOnFocus: true,
  keepPreviousData: true,
};
const NEAR_LIVE: SWRConfiguration = {
  refreshInterval: 60_000,
  revalidateOnFocus: true,
  keepPreviousData: true,
};
const SLOW: SWRConfiguration = {
  refreshInterval: 300_000,
  revalidateOnFocus: true,
  keepPreviousData: true,
};

export const useOnboardingStatus = () =>
  useSWR("onboarding/status", adminApi.onboardingStatus, SLOW);

export const useAnalyticsOverview = () =>
  useSWR("analytics/overview", adminApi.analyticsOverview, NEAR_LIVE);

/**
 * Command Center intelligence, NEAR-LIVE (60s poll). This is the right cadence:
 * the attendance timeline, early-warning signals, academic-health grid and
 * activity feed change on the order of minutes (a teacher submits attendance,
 * publishes marks, files leave), not seconds. The genuinely second-by-second
 * metrics (unread bell, students-present-now) ride the separate LIVE hooks
 * (useNotifications / useAttendanceSummary, 15s). No websocket is used because
 * the Ktor backend publishes no Supabase realtime channel to the web client,
 * tuned polling is the correct tool (documented in ARCHITECTURE.md §Real-time).
 */
export const useDashboardIntelligence = () =>
  useSWR("dashboard/intelligence", adminApi.dashboardIntelligence, NEAR_LIVE);

export const useAttendanceSummary = () =>
  useSWR("attendance/summary", adminApi.attendanceSummary, LIVE);

export const useMarksSummary = () =>
  useSWR("marks/summary", adminApi.marksSummary, SLOW);

export const useFeeLedger = () => useSWR("fees/ledger", adminApi.feeLedger, SLOW);

export const useNotifications = () =>
  useSWR("notifications", adminApi.notifications, LIVE);

export const useNotificationsSummary = () =>
  useSWR("notifications/summary", adminApi.notificationsSummary, LIVE);

export const useStudents = (q?: string, klass?: string) =>
  useSWR(["students", q ?? "", klass ?? ""], () => adminApi.students(q, klass), NEAR_LIVE);

export const useTeachers = () => useSWR("teachers", adminApi.teachers, NEAR_LIVE);

export const useAnnouncements = () =>
  useSWR("announcements", adminApi.announcements, NEAR_LIVE);

export const useLeaveRequests = (type?: string, status?: string) =>
  useSWR(["leave", type ?? "", status ?? ""], () => adminApi.leaveRequests(type, status), NEAR_LIVE);

export const useSchoolProfile = () =>
  useSWR("school/profile", adminApi.schoolProfile, SLOW);
