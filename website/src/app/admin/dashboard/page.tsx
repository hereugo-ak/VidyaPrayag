"use client";

import { Suspense, useMemo, useState } from "react";
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
  useTimetable,
} from "@/lib/admin/hooks";
import { useAdminAuth } from "@/lib/admin/session";
import { useDashboardUrlState, type TabId } from "@/lib/admin/useDashboardUrlState";
import { decodePanel, encodeSlotPanel } from "@/lib/admin/calendarUtils";
import type { TimetablePeriod } from "@/lib/admin/types";

import { FadeIn } from "@/components/admin/Primitives";
import { GreetingBar } from "@/components/admin/GreetingBar";
import { TabStrip } from "@/components/admin/TabStrip";
import { ScopeBadge } from "@/components/admin/ScopeBadge";
import { LivePulse, type PulseMetric } from "@/components/admin/LivePulse";
import { AttendanceIntelligence } from "@/components/admin/AttendanceIntelligence";
import { EarlyWarning } from "@/components/admin/EarlyWarning";
import { FinancialPulse } from "@/components/admin/FinancialPulse";
import { AcademicHealth } from "@/components/admin/AcademicHealth";
import { ActivityFeed } from "@/components/admin/ActivityFeed";
import { PeopleSnapshot } from "@/components/admin/PeopleSnapshot";
import { QuickCompose } from "@/components/admin/QuickCompose";
import { SchoolCalendar } from "@/components/admin/calendar/SchoolCalendar";
import { CalendarSlotPanel } from "@/components/admin/calendar/CalendarSlotPanel";
import { IconChevronRight } from "@/components/admin/icons";

/**
 * Command Center — the school admin home, now a multi-tab WORKSPACE rather than
 * one scrolling page. Each tab is a distinct cognitive mode (see
 * TAB_STRUCTURE.md); a persistent greeting bar + tab strip frame every mode.
 *
 * Every metric is sourced from Supabase via the Ktor backend; nothing is
 * hardcoded. Refresh strategy is documented per-hook in lib/admin/hooks.ts
 * (LIVE 15s / NEAR-LIVE 60s / SLOW 300s + on-demand drill-downs).
 *
 * URL is the single source of truth for view state (tab, the two independent
 * class filters, calendar view/date, drill-down panel) — see useDashboardUrlState.
 */
export default function DashboardPage() {
  // useSearchParams (inside useDashboardUrlState) requires a Suspense boundary
  // under the Next.js App Router.
  return (
    <Suspense fallback={<div className="h-40" />}>
      <DashboardWorkspace />
    </Suspense>
  );
}

function DashboardWorkspace() {
  const url = useDashboardUrlState();
  const { session } = useAdminAuth();

  // ── data ──
  const intel = useDashboardIntelligence();
  const attendance = useAttendanceSummary();
  const fees = useFeeLedger();
  const students = useStudents(undefined, url.globalClass ?? undefined);
  const teachers = useTeachers();
  const notif = useNotifications();
  const notifSummary = useNotificationsSummary();
  const onboarding = useOnboardingStatus();
  const timetable = useTimetable();

  const [composeOpen, setComposeOpen] = useState(false);
  const [search, setSearch] = useState("");

  const meta = intel.data?.meta;
  const classes = timetable.data?.classes ?? [];
  const scoped = url.globalClass;

  // ── live pulse (scope-aware where the data supports it) ──
  const pulse = useLivePulse({
    attendance: attendance.data,
    earlyWarning: intel.data?.early_warning,
    unread: notifSummary.data?.unread_count ?? notif.data?.unread_count ?? null,
    scope: scoped,
  });
  const live = !attendance.isLoading || !intel.isLoading;

  // ── calendar drill-down (URL-driven) ──
  const decoded = decodePanel(url.panel);
  const activePeriod = useMemo<TimetablePeriod | null>(() => {
    if (!decoded || decoded.kind !== "slot" || !decoded.periodId) return null;
    for (const wd of timetable.data?.weekdays ?? []) {
      const found = wd.periods.find((p) => p.id === decoded.periodId);
      if (found) return found;
    }
    return null;
  }, [decoded, timetable.data]);

  function refreshFeed() {
    intel.mutate();
    notif.mutate();
  }

  const openSlot = (period: TimetablePeriod, dateIso: string) =>
    url.openPanel(encodeSlotPanel(period.id, dateIso));

  return (
    <div className="space-y-5">
      {/* Onboarding nudge (only if incomplete, real status) */}
      {onboarding.data && !onboarding.data.is_complete && (
        <FadeIn>
          <Link
            href="/onboarding"
            className="flex items-center justify-between gap-4 rounded-2xl border border-accent/25 bg-accent/[0.06] px-5 py-3.5 transition-colors hover:bg-accent/[0.1]"
          >
            <p className="text-[13px] font-semibold text-navy-deep">
              Finish setting up your school, {onboarding.data.completion_percent}% complete
            </p>
            <span className="inline-flex items-center gap-1 text-[13px] font-bold text-accent-deep">
              Resume <IconChevronRight width={15} height={15} />
            </span>
          </Link>
        </FadeIn>
      )}

      {/* Persistent greeting bar (Control B global filter lives here) */}
      <FadeIn>
        <GreetingBar
          meta={meta}
          sessionName={session?.name ?? ""}
          classes={classes}
          globalClass={url.globalClass}
          onGlobalClass={url.setGlobalClass}
          search={search}
          onSearch={setSearch}
          onCompose={() => setComposeOpen(true)}
        />
      </FadeIn>

      {/* Cognitive-mode switcher */}
      <TabStrip tab={url.tab} onTab={url.setTab} />

      {/* Live pulse strip — present on every tab (PHASE 2 law) */}
      {pulse.length > 0 && (
        <FadeIn delay={0.04}>
          <LivePulse metrics={pulse} live={live} />
        </FadeIn>
      )}

      {/* Tab content */}
      <TabContent
        tab={url.tab}
        intel={intel}
        attendance={attendance}
        fees={fees}
        students={students}
        teachers={teachers}
        scoped={scoped}
        url={url}
        onOpenSlot={openSlot}
      />

      {/* Calendar slot drill-down (URL ?panel=) */}
      <CalendarSlotPanel
        open={decoded?.kind === "slot"}
        onClose={url.closePanel}
        period={activePeriod}
        dateIso={decoded?.date ?? url.calDate}
      />

      <QuickCompose open={composeOpen} onClose={() => setComposeOpen(false)} onPosted={refreshFeed} />
    </div>
  );
}

// ── Tab router ───────────────────────────────────────────────────────────────
type Url = ReturnType<typeof useDashboardUrlState>;

function TabContent({
  tab,
  intel,
  attendance,
  fees,
  students,
  teachers,
  scoped,
  url,
  onOpenSlot,
}: {
  tab: TabId;
  intel: ReturnType<typeof useDashboardIntelligence>;
  attendance: ReturnType<typeof useAttendanceSummary>;
  fees: ReturnType<typeof useFeeLedger>;
  students: ReturnType<typeof useStudents>;
  teachers: ReturnType<typeof useTeachers>;
  scoped: string | null;
  url: Url;
  onOpenSlot: (p: TimetablePeriod, dateIso: string) => void;
}) {
  const calendar = (
    <SchoolCalendar
      view={url.calView}
      date={url.calDate}
      calClass={url.calClass}
      globalClass={url.globalClass}
      onView={url.setCalView}
      onDate={url.setCalDate}
      onCalClass={url.setCalClass}
      onOpenSlot={onOpenSlot}
      onOpenDay={(dateIso) => url.setCalDate(dateIso)}
    />
  );

  switch (tab) {
    case "calendar":
      return <FadeIn delay={0.06}>{calendar}</FadeIn>;

    case "academics":
      return (
        <div className="grid gap-5 lg:grid-cols-2">
          <FadeIn delay={0.06} className="lg:col-span-2">
            <Scoped scoped={scoped}>
              <AcademicHealth data={intel.data?.academic_health} loading={intel.isLoading} />
            </Scoped>
          </FadeIn>
          <FadeIn delay={0.1} className="lg:col-span-2">
            <Scoped scoped={scoped}>
              <EarlyWarning data={intel.data?.early_warning} loading={intel.isLoading} />
            </Scoped>
          </FadeIn>
        </div>
      );

    case "finance":
      return (
        <FadeIn delay={0.06}>
          {/* Fees are school-wide in the current backend — honest note, no fake scoping. */}
          <Scoped scoped={scoped} note={scoped ? "Fees are school-wide (not class-segmented)" : undefined}>
            <FinancialPulse data={fees.data} loading={fees.isLoading} />
          </Scoped>
        </FadeIn>
      );

    case "people":
      return (
        <div className="grid gap-5 lg:grid-cols-3">
          <FadeIn delay={0.06} className="lg:col-span-2">
            <Scoped scoped={scoped}>
              <ActivityFeed data={intel.data?.activity_feed} loading={intel.isLoading} />
            </Scoped>
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
      );

    case "overview":
    default:
      return (
        <div className="space-y-5">
          <div className="grid gap-5 lg:grid-cols-3">
            <FadeIn delay={0.06} className="lg:col-span-2">
              <Scoped scoped={scoped}>
                <AttendanceIntelligence
                  data={intel.data?.attendance_timeline}
                  loading={intel.isLoading}
                />
              </Scoped>
            </FadeIn>
            <FadeIn delay={0.1}>
              <Scoped scoped={scoped}>
                <EarlyWarning data={intel.data?.early_warning} loading={intel.isLoading} />
              </Scoped>
            </FadeIn>
          </div>

          {/* Signature calendar, embedded on the overview (honours global scope) */}
          <FadeIn delay={0.08}>{calendar}</FadeIn>

          <div className="grid gap-5 lg:grid-cols-2">
            <FadeIn delay={0.06}>
              <Scoped scoped={scoped} note={scoped ? "Fees are school-wide" : undefined}>
                <FinancialPulse data={fees.data} loading={fees.isLoading} />
              </Scoped>
            </FadeIn>
            <FadeIn delay={0.1}>
              <Scoped scoped={scoped}>
                <AcademicHealth data={intel.data?.academic_health} loading={intel.isLoading} />
              </Scoped>
            </FadeIn>
          </div>
        </div>
      );
  }
}

/**
 * Wraps a panel with the global-scope indicator chip (the two-filter law: every
 * scoped panel must show it is scoped). When no global filter is set, renders
 * the panel as-is.
 */
function Scoped({
  scoped,
  note,
  children,
}: {
  scoped: string | null;
  note?: string;
  children: React.ReactNode;
}) {
  if (!scoped) return <>{children}</>;
  return (
    <div className="relative">
      <div className="absolute right-4 top-4 z-10">
        <ScopeBadge className={scoped} note={note} />
      </div>
      {children}
    </div>
  );
}

// ── Live pulse builder (real metrics only) ─────────────────────────────────────
function useLivePulse({
  attendance,
  earlyWarning,
  unread,
  scope,
}: {
  attendance: ReturnType<typeof useAttendanceSummary>["data"];
  earlyWarning: { length: number } | undefined;
  unread: number | null;
  scope: string | null;
}): PulseMetric[] {
  return useMemo(() => {
    const m: PulseMetric[] = [];
    if (attendance) {
      // When globally scoped to a class, prefer that class's by_class row so the
      // pulse reflects the scope honestly; else show school-wide totals.
      const row = scope ? attendance.by_class.find((r) => r.grade === scope) : null;
      if (scope && row) {
        const presentNow = row.present + row.late;
        m.push({ label: "Present now", value: presentNow, tone: "success" });
        m.push({ label: "Absent", value: row.absent, tone: "danger" });
        m.push({ label: "Attendance", value: `${row.rate}%`, tone: "accent" });
      } else {
        const presentNow = attendance.present + attendance.late;
        m.push({ label: "Present now", value: presentNow, tone: "success" });
        m.push({ label: "Absent", value: attendance.absent, tone: "danger" });
        m.push({ label: "Attendance", value: `${attendance.rate}%`, tone: "accent" });
      }
    }
    const warnCount = earlyWarning?.length;
    if (warnCount != null)
      m.push({ label: "At risk", value: warnCount, tone: warnCount > 0 ? "warning" : "neutral" });
    if (unread != null)
      m.push({ label: "Unread", value: unread, tone: unread > 0 ? "accent" : "neutral" });
    return m;
  }, [attendance, earlyWarning, unread, scope]);
}
