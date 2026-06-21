"use client";

import { classColor } from "@/lib/admin/classColor";
import {
  WEEKDAY_SHORT,
  isToday,
  isoDate,
  isoWeekday,
  periodsForWeekday,
} from "@/lib/admin/calendarUtils";
import type { CalendarEventDto, TimetableWeekday } from "@/lib/admin/types";

/**
 * Month view (CALENDAR_SPEC §1). A clean Mon→Sun rectangle of full weeks. Each
 * in-month day cell shows the date, up to 3 condensed class dots (one per class
 * with periods that weekday, in class colour) + "+N", and any date-specific
 * event badge. Adjacent-month days are greyed. Clicking a day opens that day's
 * agenda in the drill-down panel.
 */
const MAX_DOTS = 3;

export function MonthGrid({
  anchorMonth,
  dates,
  weekdays,
  scopeClass,
  events,
  onOpenDay,
}: {
  anchorMonth: number; // 0-11, the month being viewed
  dates: Date[]; // full month-grid rectangle
  weekdays: TimetableWeekday[] | undefined;
  scopeClass: string | null;
  events: CalendarEventDto[];
  onOpenDay: (date: Date) => void;
}) {
  return (
    <div className="px-5 py-4">
      {/* weekday header row */}
      <div className="mb-2 grid grid-cols-7 gap-1.5">
        {WEEKDAY_SHORT.map((w) => (
          <div key={w} className="text-center text-[10.5px] font-semibold uppercase tracking-wide text-ink-3">
            {w}
          </div>
        ))}
      </div>

      <div className="grid grid-cols-7 gap-1.5">
        {dates.map((date) => {
          const inMonth = date.getMonth() === anchorMonth;
          const periods = periodsForWeekday(weekdays, isoWeekday(date), scopeClass);
          const classNames = distinctClasses(periods);
          const event = events.find((e) => e.date === isoDate(date));
          const today = isToday(date);

          return (
            <button
              key={isoDate(date)}
              type="button"
              onClick={() => onOpenDay(date)}
              className={`flex min-h-[78px] flex-col rounded-xl border p-1.5 text-left transition-all duration-200 hover:-translate-y-px hover:shadow-card ${
                inMonth ? "border-navy/8 bg-white" : "border-transparent bg-navy/[0.015]"
              } ${today ? "ring-1 ring-inset ring-accent/40" : ""}`}
            >
              <span
                className={`flex h-6 w-6 items-center justify-center rounded-full text-[12px] font-bold ${
                  today ? "bg-accent text-white" : inMonth ? "text-navy-deep" : "text-ink-3/60"
                }`}
              >
                {date.getDate()}
              </span>

              {event && inMonth && (
                <span className="mt-1 truncate rounded bg-warning/12 px-1 py-0.5 text-[9.5px] font-semibold text-warning" title={event.event_title}>
                  {event.event_title}
                </span>
              )}

              {inMonth && classNames.length > 0 && (
                <div className="mt-auto flex flex-wrap items-center gap-0.5 pt-1">
                  {classNames.slice(0, MAX_DOTS).map((c) => (
                    <span
                      key={c}
                      className="h-1.5 w-1.5 rounded-full"
                      style={{ backgroundColor: classColor(c).rail }}
                      title={c}
                    />
                  ))}
                  {classNames.length > MAX_DOTS && (
                    <span className="text-[9px] font-semibold text-ink-3">
                      +{classNames.length - MAX_DOTS}
                    </span>
                  )}
                </div>
              )}
            </button>
          );
        })}
      </div>
    </div>
  );
}

function distinctClasses(periods: { class_name: string }[]): string[] {
  const set = new Set<string>();
  periods.forEach((p) => set.add(p.class_name));
  return [...set];
}
