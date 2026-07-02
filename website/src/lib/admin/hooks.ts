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

/**
 * Signature calendar — school-wide weekly timetable (all classes).
 *
 * REFRESH STRATEGY: polling:300s (SLOW) + revalidateOnFocus
 * REASON: a timetable is a recurring weekly pattern that changes on the order
 *   of terms, not minutes. The live operational reality of any given slot
 *   (present-now, attendance-marked) is read ON-DEMAND when the drill-down
 *   opens, not polled. No websocket — the Ktor backend publishes no Supabase
 *   realtime channel to the web client.
 * SOURCE: GET /api/v1/school/timetable → teacher_periods (school-scoped) ⨝ app_users.full_name
 */
export const useTimetable = (className?: string) =>
  useSWR(["timetable", className ?? ""], () => adminApi.timetable(className), SLOW);

/**
 * Academic calendar events/holidays/exams for the viewed range.
 *
 * REFRESH STRATEGY: polling:300s (SLOW) + revalidateOnFocus
 * REASON: holidays/exams/events are scheduled well ahead and change rarely;
 *   the calendar re-keys (and revalidates) when the view/anchor date changes.
 * SOURCE: GET /api/v1/school/calendar → academic_calendar + holidays (school-scoped)
 */
export const useCalendar = (date?: string, viewType: "week" | "month" = "week") =>
  useSWR(["calendar", date ?? "", viewType], () => adminApi.calendar(date, viewType), SLOW);

// NOTE: attendanceDaily is intentionally NOT a polling hook. It is the
// ON-DEMAND read behind the calendar slot drill-down (adminApi.attendanceDaily),
// fetched once when a slot panel opens. Polling it for every class×date would be
// wasteful and pointless — the panel is a momentary read, not a live surface.

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

// ── PEWS hooks ────────────────────────────────────────────────────────────────
// The cohort + interventions + effectiveness move on the order of minutes (a
// recompute runs hourly / on demand, a teacher closes an intervention), so
// NEAR-LIVE (60s) is the right cadence. Config is SLOW (changes rarely).
import type { PewsRiskLevel, PewsInterventionStatus } from "./types";

export const usePewsCohort = (minLevel?: PewsRiskLevel) =>
  useSWR(["pews/cohort", minLevel ?? ""], () => adminApi.pewsCohort(minLevel), NEAR_LIVE);

export const usePewsStudent = (studentCode: string | null) =>
  useSWR(studentCode ? ["pews/student", studentCode] : null, () => adminApi.pewsStudent(studentCode as string), NEAR_LIVE);

export const usePewsInterventions = (status?: PewsInterventionStatus) =>
  useSWR(["pews/interventions", status ?? ""], () => adminApi.pewsInterventions(status), NEAR_LIVE);

export const usePewsEffectiveness = () =>
  useSWR("pews/effectiveness", adminApi.pewsEffectiveness, NEAR_LIVE);

export const usePewsTrend = (days?: number) =>
  useSWR(["pews/trend", days ?? ""], () => adminApi.pewsTrend(days), NEAR_LIVE);

export const usePewsConfig = () => useSWR("pews/config", adminApi.pewsConfig, SLOW);

// ── AI Report Card 2.0 hooks ──────────────────────────────────────────────────
// Oversight and effectiveness move on the order of minutes (a teacher approves
// a draft, an admin publishes a class), so NEAR-LIVE (60s) is the right cadence.
// Term config is SLOW (changes rarely).

export const useReportCardOversight = (term: string | null, academicYearId?: string) =>
  useSWR(term ? ["report-card/oversight", term, academicYearId ?? ""] : null,
    () => adminApi.reportCardOversight(term as string, academicYearId), NEAR_LIVE);

export const useReportCardEffectiveness = () =>
  useSWR("report-card/effectiveness", adminApi.reportCardEffectiveness, NEAR_LIVE);

export const useReportCardTermConfig = () =>
  useSWR("report-card/term-config", adminApi.reportCardTermConfig, SLOW);

// ── AI Tutor 2.0 hooks ────────────────────────────────────────────────────────
// Teacher scope (assigned classes/subjects) is SLOW. Heatmap data is NEAR-LIVE
// (mastery updates as kids practice, but not second-by-second).

export const useTutorTeacherScope = () =>
  useSWR("tutor/scope", adminApi.tutorTeacherScope, SLOW);

export const useTutorHeatmap = (classId: string | null, subjectId: string | null) =>
  useSWR(classId && subjectId ? ["tutor/heatmap", classId, subjectId] : null,
    () => adminApi.tutorHeatmap(classId as string, subjectId as string), NEAR_LIVE);

// ── AI Token Monitor hooks (Dev Tools — super admin) ──────────────────────────
// Rate-limiter and health data are LIVE (10s poll) so the admin sees
// near-real-time RPM/RPD/TPM usage. Recent usage log is also LIVE for
// the scrolling log feed.

const AI_LIVE: SWRConfiguration = {
  refreshInterval: 10_000,
  revalidateOnFocus: true,
  keepPreviousData: true,
};

export const useAiRateLimits = () =>
  useSWR("ai/rate-limits", adminApi.aiRateLimits, AI_LIVE);

export const useAiHealth = () =>
  useSWR("ai/health", adminApi.aiHealth, AI_LIVE);

export const useAiRecentUsage = (limit: number = 50, windowMin: number = 60) =>
  useSWR(["ai/recent-usage", limit, windowMin],
    () => adminApi.aiRecentUsage(limit, windowMin), AI_LIVE);

// ── School Day Configuration hooks ───────────────────────────────────────────
// Bell-schedule configs change rarely (term-level), so SLOW (300s) is the right cadence.

export const useSchoolDayConfigs = () =>
  useSWR("school-day-configs", adminApi.schoolDayConfigs, SLOW);

// ── Academics: Classes, Subjects, Timetable hooks ─────────────────────────────
// Classes and subjects change rarely (term-level), so SLOW (300s) is the right cadence.

export const useSchoolClasses = () =>
  useSWR("school-classes", adminApi.schoolClasses, SLOW);

export const useSchoolSubjects = (classId: string | null) =>
  useSWR(classId ? ["school-subjects", classId] : null,
    () => adminApi.schoolSubjects(classId as string), SLOW);
