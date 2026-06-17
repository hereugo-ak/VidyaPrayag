"use client";

import { useMemo, useState } from "react";
import Link from "next/link";

import {
  useAttendanceSummary,
  useDashboardIntelligence,
  useFeeLedger,
  useMarksSummary,
  useNotifications,
  useOnboardingStatus,
  useStudents,
  useTeachers,
  useTimetable,
} from "@/lib/admin/hooks";
import { useAdminAuth } from "@/lib/admin/session";
import { useDashboardUrlState, type TabId } from "@/lib/admin/useDashboardUrlState";
import { decodePanel, encodeSlotPanel } from "@/lib/admin/calendarUtils";
import { compactMoney } from "@/lib/admin/format";
import type { TimetablePeriod } from "@/lib/admin/types";

import { FadeIn } from "@/components/admin/Primitives";
import { GreetingBar } from "@/components/admin/GreetingBar";
import { TabStrip } from "@/components/admin/TabStrip";
import { ScopeBadge } from "@/components/admin/ScopeBadge";
import { HeroStatTiles, type HeroTile } from "@/components/admin/HeroStatTiles";
import { AttendanceIntelligence } from "@/components/admin/AttendanceIntelligence";
import { EarlyWarning } from "@/components/admin/EarlyWarning";
import { FinancialPulse } from "@/components/admin/FinancialPulse";
import { FinanceTab } from "@/components/admin/FinanceTab";
import { AcademicHealth } from "@/components/admin/AcademicHealth";
import { ActivityFeed } from "@/components/admin/ActivityFeed";
import { PeopleTab } from "@/components/admin/PeopleTab";
import { MarksSummaryPanel } from "@/components/admin/MarksSummaryPanel";
import { QuickCompose } from "@/components/admin/QuickCompose";
import { SchoolCalendar } from "@/components/admin/calendar/SchoolCalendar";
import { CalendarSlotPanel } from "@/components/admin/calendar/CalendarSlotPanel";
import {
  IconChevronRight,
  IconPeople,
  IconAttendance,
  IconPulse,
  IconFees,
  IconWarning,
} from "@/components/admin/icons";

/**
 * DashboardWorkspace — the school-admin command center as a multi-tab workspace
 * (TAB_STRUCTURE.md), redesigned to the premium reference language: a greeting
 * hero, a headline KPI tile row, then a signature "main column + intelligence
 * rail" composition rather than a flat uniform grid.
 *
 * Every metric is REAL (Supabase via the Ktor backend). Refresh strategy is
 * documented per hook in lib/admin/hooks.ts (LIVE 15s / NEAR-LIVE 60s /
 * SLOW 300s + on-demand drill-downs). All view state (tab, the two independent
 * class filters, calendar view/date, open drill-down) lives in the URL.
 */
export function DashboardWorkspace() {
  const url = useDashboardUrlState();
  const { session } = useAdminAuth();

  // ── data ──
  const intel = useDashboardIntelligence();
  const attendance = useAttendanceSummary();
  const fees = useFeeLedger();
  const marks = useMarksSummary();
  const students = useStudents(undefined, url.globalClass ?? undefined);
  const teachers = useTeachers();
  const notif = useNotifications();
  const onboarding = useOnboardingStatus();
  const timetable = useTimetable();

  const [composeOpen, setComposeOpen] = useState(false);
  const [search, setSearch] = useState("");

  const meta = intel.data?.meta;
  const scoped = url.globalClass;

  // Class option list — union of every real source so the filter is honest even
  // before a timetable is entered (COMPONENT_MAP §ClassFilter).
  const classes = useMemo(() => {
    const set = new Set<string>();
    timetable.data?.classes?.forEach((c) => set.add(c));
    attendance.data?.by_class?.forEach((r) => set.add(r.grade));
    intel.data?.academic_health?.rows?.forEach((r) => set.add(r.class_name));
    return Array.from(set).sort();
  }, [timetable.data, attendance.data, intel.data]);

  // ── hero KPI tiles (all real) ──
  const heroTiles = useHeroTiles({ intel, attendance, fees, scoped });

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

  const calendar = (
    <SchoolCalendar
      view={url.calView}
      date={url.calDate}
      calClass={url.calClass}
      globalClass={url.globalClass}
      onView={url.setCalView}
      onDate={url.setCalDate}
      onCalClass={url.setCalClass}
      onOpenSlot={openSlot}
      onOpenDay={(dateIso) => url.setCalDate(dateIso)}
    />
  );

  return (
    <div className="space-y-5">
      {/* Onboarding nudge — only when genuinely incomplete (real status). */}
      {onboarding.data && !onboarding.data.is_complete && (
        <FadeIn>
          <Link
            href="/onboarding"
            className="flex items-center justify-between gap-4 rounded-2xl bg-accent/[0.06] px-5 py-3.5 ring-1 ring-inset ring-accent/15 transition-colors hover:bg-accent/[0.1]"
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

      {/* Greeting hero — Control B global filter + search + command actions. */}
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

      {/* Cognitive-mode switcher. */}
      <TabStrip tab={url.tab} onTab={url.setTab} />

      {/* Tab content. */}
      <TabContent
        tab={url.tab}
        heroTiles={heroTiles}
        intel={intel}
        attendance={attendance}
        fees={fees}
        marks={marks}
        students={students}
        teachers={teachers}
        scoped={scoped}
        calendar={calendar}
      />

      {/* Calendar slot drill-down (URL ?panel=). */}
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
function TabContent({
  tab,
  heroTiles,
  intel,
  attendance,
  fees,
  marks,
  students,
  teachers,
  scoped,
  calendar,
}: {
  tab: TabId;
  heroTiles: HeroTile[];
  intel: ReturnType<typeof useDashboardIntelligence>;
  attendance: ReturnType<typeof useAttendanceSummary>;
  fees: ReturnType<typeof useFeeLedger>;
  marks: ReturnType<typeof useMarksSummary>;
  students: ReturnType<typeof useStudents>;
  teachers: ReturnType<typeof useTeachers>;
  scoped: string | null;
  calendar: React.ReactNode;
}) {
  switch (tab) {
    case "calendar":
      return (
        <div className="space-y-5">
          <FadeIn delay={0.04}>
            <HeroStatTiles tiles={heroTiles} />
          </FadeIn>
          <FadeIn delay={0.08}>{calendar}</FadeIn>
        </div>
      );

    case "academics":
      return (
        <div className="space-y-5">
          <FadeIn delay={0.04}>
            <HeroStatTiles tiles={heroTiles} />
          </FadeIn>
          <FadeIn delay={0.06}>
            <Scoped scoped={scoped}>
              <AttendanceIntelligence data={intel.data?.attendance_timeline} loading={intel.isLoading} />
            </Scoped>
          </FadeIn>
          <div className="grid gap-5 lg:grid-cols-2">
            <FadeIn delay={0.08}>
              <Scoped scoped={scoped}>
                <AcademicHealth data={intel.data?.academic_health} loading={intel.isLoading} />
              </Scoped>
            </FadeIn>
            <FadeIn delay={0.1}>
              <MarksSummaryPanel data={marks.data} loading={marks.isLoading} scopeClass={scoped} />
            </FadeIn>
          </div>
          <FadeIn delay={0.12}>
            <Scoped scoped={scoped}>
              <EarlyWarning data={intel.data?.early_warning} loading={intel.isLoading} />
            </Scoped>
          </FadeIn>
        </div>
      );

    case "finance":
      return (
        <div className="space-y-5">
          <FadeIn delay={0.04}>
            <HeroStatTiles tiles={heroTiles} />
          </FadeIn>
          {/* Fees are school-wide in the current backend — honest note, no fake scoping. */}
          {scoped && (
            <FadeIn delay={0.06}>
              <p className="px-1 text-[12px] font-medium text-ink-3">
                Showing <span className="font-bold text-navy-deep">school-wide</span> fees — the backend does not segment fees by class.
              </p>
            </FadeIn>
          )}
          <FadeIn delay={0.08}>
            <FinanceTab data={fees.data} loading={fees.isLoading} />
          </FadeIn>
        </div>
      );

    case "people":
      return (
        <div className="space-y-5">
          <FadeIn delay={0.04}>
            <HeroStatTiles tiles={heroTiles} />
          </FadeIn>
          <FadeIn delay={0.06}>
            <PeopleTab
              students={students.data}
              teachers={teachers.data}
              attendance={attendance.data}
              loading={(students.isLoading || teachers.isLoading) && !students.data}
              scopeClass={scoped}
            />
          </FadeIn>
          <FadeIn delay={0.1}>
            <Scoped scoped={scoped}>
              <EarlyWarning data={intel.data?.early_warning} loading={intel.isLoading} />
            </Scoped>
          </FadeIn>
        </div>
      );

    case "overview":
    default:
      return (
        <div className="space-y-5">
          {/* Headline KPIs — the first 60 seconds (METRICS_BRIEF §A). */}
          <FadeIn delay={0.02}>
            <HeroStatTiles tiles={heroTiles} />
          </FadeIn>

          {/* Signature row: calendar (main) + early-warning rail. */}
          <div className="grid gap-5 xl:grid-cols-[1.62fr_1fr]">
            <FadeIn delay={0.06}>{calendar}</FadeIn>
            <FadeIn delay={0.1}>
              <Scoped scoped={scoped}>
                <EarlyWarning data={intel.data?.early_warning} loading={intel.isLoading} variant="preview" />
              </Scoped>
            </FadeIn>
          </div>

          {/* Trend (main) + financial pulse (rail). */}
          <div className="grid gap-5 xl:grid-cols-[1.62fr_1fr]">
            <FadeIn delay={0.06}>
              <Scoped scoped={scoped} note={scoped ? "Trend is school-wide" : undefined}>
                <AttendanceIntelligence data={intel.data?.attendance_timeline} loading={intel.isLoading} />
              </Scoped>
            </FadeIn>
            <FadeIn delay={0.1}>
              <Scoped scoped={scoped} note={scoped ? "Fees are school-wide" : undefined}>
                <FinancialPulse data={fees.data} loading={fees.isLoading} />
              </Scoped>
            </FadeIn>
          </div>

          {/* Academic health + activity feed. */}
          <div className="grid gap-5 lg:grid-cols-2">
            <FadeIn delay={0.06}>
              <Scoped scoped={scoped}>
                <AcademicHealth data={intel.data?.academic_health} loading={intel.isLoading} />
              </Scoped>
            </FadeIn>
            <FadeIn delay={0.1}>
              <ActivityFeed data={intel.data?.activity_feed} loading={intel.isLoading} />
            </FadeIn>
          </div>
        </div>
      );
  }
}

/** Global-scope indicator chip wrapper (the two-filter law). */
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
      <div className="pointer-events-none absolute right-4 top-4 z-10">
        <ScopeBadge className={scoped} note={note} />
      </div>
      {children}
    </div>
  );
}

// ── Hero KPI tile builder (real metrics only) ──────────────────────────────────
function useHeroTiles({
  intel,
  attendance,
  fees,
  scoped,
}: {
  intel: ReturnType<typeof useDashboardIntelligence>;
  attendance: ReturnType<typeof useAttendanceSummary>;
  fees: ReturnType<typeof useFeeLedger>;
  scoped: string | null;
}): HeroTile[] {
  return useMemo(() => {
    const tiles: HeroTile[] = [];
    const att = attendance.data;
    const timeline = intel.data?.attendance_timeline ?? [];
    const spark = timeline.map((p) => p.rate);

    // Attendance figures honour the global scope where by_class supports it.
    const row = scoped && att ? att.by_class.find((r) => r.grade === scoped) : null;
    const presentNow = row ? row.present + row.late : att ? att.present + att.late : null;
    const enrolled = row ? row.total : att?.total ?? null;
    const rate = row ? row.rate : att?.rate ?? null;

    // Delta on attendance rate = latest point vs 7 days prior (real series).
    let rateDelta: { delta: string; dir: "up" | "down" | "flat" } | undefined;
    if (timeline.length >= 8 && !scoped) {
      const last = timeline[timeline.length - 1].rate;
      const prior = timeline[timeline.length - 8].rate;
      const d = last - prior;
      rateDelta = {
        delta: `${d >= 0 ? "+" : ""}${d}%`,
        dir: d > 0 ? "up" : d < 0 ? "down" : "flat",
      };
    }

    if (presentNow != null) {
      tiles.push({
        key: "present",
        label: "Present today",
        value: String(presentNow),
        sub: enrolled != null ? `of ${enrolled} enrolled` : undefined,
        tone: "success",
        icon: <IconPeople width={18} height={18} />,
        spark: spark.length > 1 ? timeline.map((p) => p.present) : undefined,
        href: "/admin/attendance",
      });
    }
    if (rate != null) {
      tiles.push({
        key: "rate",
        label: "Attendance rate",
        value: `${rate}%`,
        delta: rateDelta?.delta,
        deltaDir: rateDelta?.dir,
        goodWhenUp: true,
        tone: "accent",
        icon: <IconAttendance width={18} height={18} />,
        spark: spark.length > 1 ? spark : undefined,
      });
    }

    // Fees — collection rate (school-wide; honest when scoped).
    if (fees.data) {
      const billed = fees.data.paid_total + fees.data.due_total + fees.data.overdue_total;
      const collected = billed > 0 ? Math.round((fees.data.paid_total / billed) * 100) : 0;
      tiles.push({
        key: "fees",
        label: scoped ? "Fees collected (school-wide)" : "Fees collected",
        value: compactMoney(fees.data.paid_total, fees.data.currency),
        sub: `${collected}% of billed`,
        tone: "teal",
        icon: <IconFees width={18} height={18} />,
        href: "/admin/fees",
      });
    }

    // At-risk — early-warning cohort (scoped by class_name when scoped).
    const warnings = intel.data?.early_warning ?? [];
    const warnCount = scoped ? warnings.filter((w) => w.class_name === scoped).length : warnings.length;
    if (intel.data) {
      tiles.push({
        key: "risk",
        label: "Students at risk",
        value: String(warnCount),
        sub: warnCount === 0 ? "All above thresholds" : "Need attention",
        tone: warnCount > 0 ? "warning" : "sky",
        icon: <IconWarning width={18} height={18} />,
      });
    }

    return tiles;
  }, [intel.data, attendance.data, fees.data, scoped]);
}
