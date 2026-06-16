"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import {
  useAttendanceSummary,
  useDashboardIntelligence,
  useFeeLedger,
  useNotifications,
  useNotificationsSummary,
  useOnboardingStatus,
  useStudents,
  useTeachers,
} from "@/lib/admin/hooks";
import { FadeIn } from "@/components/admin/Primitives";
import { CommandBar } from "@/components/admin/CommandBar";
import { LivePulse, type PulseMetric } from "@/components/admin/LivePulse";
import { AttendanceIntelligence } from "@/components/admin/AttendanceIntelligence";
import { EarlyWarning } from "@/components/admin/EarlyWarning";
import { FinancialPulse } from "@/components/admin/FinancialPulse";
import { AcademicHealth } from "@/components/admin/AcademicHealth";
import { ActivityFeed } from "@/components/admin/ActivityFeed";
import { PeopleSnapshot } from "@/components/admin/PeopleSnapshot";
import { QuickCompose } from "@/components/admin/QuickCompose";
import { IconChevronRight } from "@/components/admin/icons";

/**
 * Command Center — the school admin home. Every metric is sourced from
 * Supabase via the Ktor backend; nothing is hardcoded. Visual hierarchy:
 *
 *   1. Command bar      — who/when + the four first-reach actions
 *   2. Live pulse strip — second-by-second metrics (LIVE 15s hooks)
 *   3. Attendance intelligence (primary) + Early warning (urgent) — top row
 *   4. Financial pulse + Academic health grid                    — review row
 *   5. Activity feed + People snapshot                           — context row
 *
 * Real-time strategy (also documented per-hook in lib/admin/hooks.ts):
 *   • LIVE 15s   : pulse strip (notifications summary + today's attendance)
 *   • NEAR-LIVE  : intelligence payload, fee ledger (60s)
 *   • ON-DEMAND  : every drill-down side panel fetches/derives on user action
 */
export default function DashboardPage() {
  const intel = useDashboardIntelligence();
  const attendance = useAttendanceSummary();
  const fees = useFeeLedger();
  const students = useStudents();
  const teachers = useTeachers();
  const notif = useNotifications();
  const notifSummary = useNotificationsSummary();
  const onboarding = useOnboardingStatus();

  const [composeOpen, setComposeOpen] = useState(false);

  const meta = intel.data?.meta;

  // ── live pulse metrics (all real) ──────────────────────────────────────────
  const presentNow = attendance.data ? attendance.data.present + attendance.data.late : null;
  const pulse: PulseMetric[] = useMemo(() => {
    const m: PulseMetric[] = [];
    if (attendance.data) {
      m.push({ label: "Present now", value: presentNow ?? 0, tone: "success" });
      m.push({ label: "Absent", value: attendance.data.absent, tone: "danger" });
      m.push({ label: "Attendance", value: `${attendance.data.rate}%`, tone: "accent" });
    }
    const warnCount = intel.data?.early_warning.length;
    if (warnCount != null) m.push({ label: "At risk", value: warnCount, tone: warnCount > 0 ? "warning" : "neutral" });
    const unread = notifSummary.data?.unread_count ?? notif.data?.unread_count;
    if (unread != null) m.push({ label: "Unread", value: unread, tone: unread > 0 ? "accent" : "neutral" });
    return m;
  }, [attendance.data, presentNow, intel.data, notifSummary.data, notif.data]);

  const live = !attendance.isLoading || !intel.isLoading;

  function refreshFeed() {
    intel.mutate();
    notif.mutate();
  }

  return (
    <div className="space-y-5">
      {/* Onboarding nudge (only if incomplete — real status) */}
      {onboarding.data && !onboarding.data.is_complete && (
        <FadeIn>
          <Link
            href="/onboarding"
            className="flex items-center justify-between gap-4 rounded-2xl border border-accent/25 bg-accent/[0.06] px-5 py-3.5 transition-colors hover:bg-accent/[0.1]"
          >
            <p className="text-[13px] font-semibold text-navy-deep">
              Finish setting up your school — {onboarding.data.completion_percent}% complete
            </p>
            <span className="inline-flex items-center gap-1 text-[13px] font-bold text-accent-deep">
              Resume <IconChevronRight width={15} height={15} />
            </span>
          </Link>
        </FadeIn>
      )}

      {/* 1. Command bar */}
      <FadeIn>
        <CommandBar meta={meta} onCompose={() => setComposeOpen(true)} />
      </FadeIn>

      {/* 2. Live pulse strip */}
      {pulse.length > 0 && (
        <FadeIn delay={0.04}>
          <LivePulse metrics={pulse} live={live} />
        </FadeIn>
      )}

      {/* 3. Primary intelligence + early warning */}
      <div className="grid gap-5 lg:grid-cols-3">
        <FadeIn delay={0.06} className="lg:col-span-2">
          <AttendanceIntelligence data={intel.data?.attendance_timeline} loading={intel.isLoading} />
        </FadeIn>
        <FadeIn delay={0.1}>
          <EarlyWarning data={intel.data?.early_warning} loading={intel.isLoading} />
        </FadeIn>
      </div>

      {/* 4. Financial pulse + academic health */}
      <div className="grid gap-5 lg:grid-cols-2">
        <FadeIn delay={0.06}>
          <FinancialPulse data={fees.data} loading={fees.isLoading} />
        </FadeIn>
        <FadeIn delay={0.1}>
          <AcademicHealth data={intel.data?.academic_health} loading={intel.isLoading} />
        </FadeIn>
      </div>

      {/* 5. Activity feed + people snapshot */}
      <div className="grid gap-5 lg:grid-cols-3">
        <FadeIn delay={0.06} className="lg:col-span-2">
          <ActivityFeed data={intel.data?.activity_feed} loading={intel.isLoading} />
        </FadeIn>
        <FadeIn delay={0.1}>
          <PeopleSnapshot
            teacherCount={teachers.data?.teachers.length ?? 0}
            studentCount={students.data?.students.length ?? 0}
            todayRate={attendance.data ? attendance.data.rate : null}
            loading={(students.isLoading || teachers.isLoading) && !students.data}
          />
        </FadeIn>
      </div>

      <QuickCompose open={composeOpen} onClose={() => setComposeOpen(false)} onPosted={refreshFeed} />
    </div>
  );
}
