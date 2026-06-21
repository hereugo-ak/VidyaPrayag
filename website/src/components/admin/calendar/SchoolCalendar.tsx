"use client";

import { useMemo } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { Card } from "../Primitives";
import { EmptyState, Skeleton } from "../Primitives";
import { IconCalendar, IconWarning } from "../icons";
import { CalendarHeader } from "./CalendarHeader";
import { CalendarLegend } from "./CalendarLegend";
import { WeekGrid } from "./WeekGrid";
import { MonthGrid } from "./MonthGrid";
import type { SlotStatus } from "./PeriodChip";
import { useTimetable, useCalendar, useAttendanceSummary } from "@/lib/admin/hooks";
import type { CalView } from "@/lib/admin/useDashboardUrlState";
import {
  addDays,
  addMonths,
  classesInView,
  effectiveCalendarScope,
  isoDate,
  isToday,
  parseIso,
  shortDate,
  weekDates,
  monthGridDates,
} from "@/lib/admin/calendarUtils";
import type { TimetablePeriod } from "@/lib/admin/types";

/**
 * SchoolCalendar — the product's signature surface (CALENDAR_SPEC).
 *
 * Orchestrates the all-classes weekly schedule (timetable), date-specific events
 * (calendar), and today's live attendance reality into one navigable widget.
 * Owns view/date/scope through props the parent maps to the URL (deep-linkable).
 * The drill-down itself is opened via `onOpenSlot`/`onOpenDay` so the parent can
 * mount the CalendarSlotPanel and mirror it to ?panel=.
 *
 * Effective scope = calClass ?? globalClass ?? all (Control A wins; otherwise the
 * embedded calendar honours the dashboard-global scope — see §2).
 */
export function SchoolCalendar({
  view,
  date,
  calClass,
  globalClass,
  onView,
  onDate,
  onCalClass,
  onOpenSlot,
  onOpenDay,
}: {
  view: CalView;
  date: string; // YYYY-MM-DD anchor
  calClass: string | null;
  globalClass: string | null;
  onView: (v: CalView) => void;
  onDate: (d: string) => void;
  onCalClass: (c: string | null) => void;
  onOpenSlot: (period: TimetablePeriod, dateIso: string) => void;
  onOpenDay: (dateIso: string) => void;
}) {
  const scope = effectiveCalendarScope(calClass, globalClass);
  const dense = scope != null; // single-class mode → richer chips

  // Timetable is fetched UNSCOPED so the Control-A dropdown can always list all
  // classes; we filter client-side by `scope`. Calendar events are range-keyed.
  const { data: tt, isLoading: ttLoading, error: ttError, mutate } = useTimetable();
  const { data: cal } = useCalendar(date, view);
  const { data: att } = useAttendanceSummary();

  const anchor = parseIso(date);
  const dates = useMemo(
    () => (view === "week" ? weekDates(anchor) : monthGridDates(anchor)),
    [view, date] // eslint-disable-line react-hooks/exhaustive-deps
  );

  const rangeLabel = useMemo(() => {
    if (view === "month") {
      return anchor.toLocaleDateString("en-IN", { month: "long", year: "numeric" });
    }
    const wk = weekDates(anchor);
    return `${shortDate(wk[0])} – ${shortDate(wk[6])}`;
  }, [view, date]); // eslint-disable-line react-hooks/exhaustive-deps

  const isCurrentRange = useMemo(() => dates.some((d) => isToday(d)), [dates]);

  const allClasses = tt?.classes ?? [];
  const legendClasses = useMemo(
    () => classesInView(tt?.classes, tt?.weekdays, scope),
    [tt, scope]
  );

  // ── Today live-status resolver (CALENDAR_SPEC §1) ──
  // A class is "marked" today if attendance/summary's latest_date is today and
  // that class appears in by_class with any recorded total. Otherwise, for a
  // period earlier than now it's "pending", later it's "future".
  const markedToday = useMemo(() => {
    const set = new Set<string>();
    if (att?.latest_date && isToday(parseIso(att.latest_date))) {
      att.by_class.forEach((r) => {
        if (r.total > 0) set.add(r.grade);
      });
    }
    return set;
  }, [att]);

  const statusFor = (period: TimetablePeriod, d: Date): SlotStatus => {
    if (!isToday(d)) return "none";
    if (markedToday.has(period.class_name)) return "marked";
    const now = new Date();
    const [eh, em] = period.end_time.split(":").map(Number);
    const ended = now.getHours() * 60 + now.getMinutes() > eh * 60 + em;
    return ended ? "pending" : "future";
  };

  // ── Navigation ──
  const step = (dir: 1 | -1) => {
    const next = view === "week" ? addDays(anchor, dir * 7) : addMonths(anchor, dir);
    onDate(isoDate(next));
  };
  const goToday = () => onDate(isoDate(new Date()));

  const hasTimetable = (tt?.weekdays?.length ?? 0) > 0;

  return (
    <Card className="overflow-hidden">
      <CalendarHeader
        rangeLabel={rangeLabel}
        view={view}
        onView={onView}
        onPrev={() => step(-1)}
        onNext={() => step(1)}
        onToday={goToday}
        isCurrentRange={isCurrentRange}
        classes={allClasses}
        calClass={calClass}
        onCalClass={onCalClass}
        globalClass={globalClass}
      />

      {legendClasses.length > 0 && (
        <div className="border-b border-navy/8 px-5 py-2.5">
          <CalendarLegend classes={legendClasses} />
        </div>
      )}

      {ttError ? (
        <CalendarError onRetry={() => mutate()} />
      ) : ttLoading ? (
        <CalendarSkeleton view={view} />
      ) : !hasTimetable ? (
        <EmptyState
          icon={<IconCalendar width={28} height={28} />}
          title="No timetable yet"
          hint="Once class periods are added, the whole school's week appears here — colour-coded by class, with live attendance status on today's slots."
        />
      ) : (
        <AnimatePresence mode="wait">
          <motion.div
            key={`${view}-${date}`}
            initial={{ opacity: 0, scale: 0.992 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.992 }}
            transition={{ duration: 0.28, ease: [0.16, 1, 0.3, 1] }}
          >
            {view === "week" ? (
              <WeekGrid
                dates={dates}
                weekdays={tt?.weekdays}
                scopeClass={scope}
                events={cal?.calendar_events ?? []}
                dense={dense}
                statusFor={statusFor}
                onOpenSlot={(p, d) => onOpenSlot(p, isoDate(d))}
                onOpenDay={(d) => onOpenDay(isoDate(d))}
              />
            ) : (
              <MonthGrid
                anchorMonth={anchor.getMonth()}
                dates={dates}
                weekdays={tt?.weekdays}
                scopeClass={scope}
                events={cal?.calendar_events ?? []}
                onOpenDay={(d) => onOpenDay(isoDate(d))}
              />
            )}
          </motion.div>
        </AnimatePresence>
      )}
    </Card>
  );
}

function CalendarSkeleton({ view }: { view: CalView }) {
  if (view === "month") {
    return (
      <div className="grid grid-cols-7 gap-1.5 px-5 py-4">
        {Array.from({ length: 35 }).map((_, i) => (
          <Skeleton key={i} className="h-[78px]" />
        ))}
      </div>
    );
  }
  return (
    <div className="grid grid-cols-2 gap-2 px-5 py-4 md:grid-cols-7">
      {Array.from({ length: 7 }).map((_, i) => (
        <div key={i} className="flex flex-col gap-1.5">
          <Skeleton className="mx-auto h-7 w-7 rounded-full" />
          <Skeleton className="h-14" />
          <Skeleton className="h-14" />
        </div>
      ))}
    </div>
  );
}

function CalendarError({ onRetry }: { onRetry: () => void }) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 px-6 py-12 text-center">
      <div className="text-danger">
        <IconWarning width={26} height={26} />
      </div>
      <p className="text-sm font-semibold text-ink-2">Couldn&apos;t load the timetable</p>
      <button
        type="button"
        onClick={onRetry}
        className="rounded-xl bg-accent px-4 py-2 text-[13px] font-semibold text-white transition-colors hover:bg-accent-deep"
      >
        Retry
      </button>
    </div>
  );
}
