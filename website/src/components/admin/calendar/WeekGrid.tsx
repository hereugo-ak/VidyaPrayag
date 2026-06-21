"use client";

import { useState } from "react";
import { PeriodChip, type SlotStatus } from "./PeriodChip";
import {
  WEEKDAY_SHORT,
  dayRelation,
  isToday,
  isoDate,
  isoWeekday,
  periodsForWeekday,
} from "@/lib/admin/calendarUtils";
import type { CalendarEventDto, TimetablePeriod, TimetableWeekday } from "@/lib/admin/types";

/**
 * Week view (CALENDAR_SPEC §1). Desktop: a 7-column Mon→Sun grid, each column a
 * time-ordered stack of PeriodChips. Mobile: a vertical agenda with a horizontal
 * date selector (today centred). Date-specific calendar events (holidays/exams)
 * overlay the day header. Columns with zero periods collapse to a thin
 * "no classes" rail rather than wasting space.
 *
 * The recurring weekly pattern (keyed by weekday 1–7) is painted onto the actual
 * dates of `dates`. `statusFor` resolves the today-live status dot — the
 * orchestrator owns that knowledge (which classes have attendance marked today).
 */
const MAX_CHIPS = 5;

export function WeekGrid({
  dates,
  weekdays,
  scopeClass,
  events,
  dense,
  statusFor,
  onOpenSlot,
  onOpenDay,
}: {
  dates: Date[];
  weekdays: TimetableWeekday[] | undefined;
  scopeClass: string | null;
  events: CalendarEventDto[];
  dense: boolean;
  statusFor: (period: TimetablePeriod, date: Date) => SlotStatus;
  onOpenSlot: (period: TimetablePeriod, date: Date) => void;
  onOpenDay: (date: Date) => void;
}) {
  // Mobile selected day index (default = today's column if in range, else 0).
  const todayIdx = dates.findIndex(isToday);
  const [sel, setSel] = useState(todayIdx >= 0 ? todayIdx : 0);

  return (
    <>
      {/* ── Desktop: 7-column grid ── */}
      <div className="hidden gap-2 px-5 py-4 md:grid md:grid-cols-7">
        {dates.map((date) => {
          const periods = periodsForWeekday(weekdays, isoWeekday(date), scopeClass);
          const event = eventOn(events, date);
          return (
            <DayColumn
              key={isoDate(date)}
              date={date}
              periods={periods}
              event={event}
              dense={dense}
              statusFor={statusFor}
              onOpenSlot={onOpenSlot}
              onOpenDay={onOpenDay}
            />
          );
        })}
      </div>

      {/* ── Mobile: date selector + agenda ── */}
      <div className="md:hidden">
        <div className="flex gap-1.5 overflow-x-auto px-4 py-3 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
          {dates.map((date, i) => {
            const active = i === sel;
            const today = isToday(date);
            return (
              <button
                key={isoDate(date)}
                type="button"
                onClick={() => setSel(i)}
                className={`flex min-w-[52px] shrink-0 flex-col items-center rounded-xl px-2 py-1.5 transition-colors ${
                  active ? "bg-accent text-white" : "text-ink-2 hover:bg-navy/[0.04]"
                }`}
              >
                <span className="text-[10px] font-semibold uppercase tracking-wide opacity-70">
                  {WEEKDAY_SHORT[isoWeekday(date) - 1]}
                </span>
                <span className={`mt-0.5 flex h-7 w-7 items-center justify-center rounded-full text-[13px] font-bold ${today && !active ? "ring-1 ring-accent text-accent-deep" : ""}`}>
                  {date.getDate()}
                </span>
              </button>
            );
          })}
        </div>
        <div className="px-4 pb-4">
          <MobileAgenda
            date={dates[sel]}
            periods={periodsForWeekday(weekdays, isoWeekday(dates[sel]), scopeClass)}
            event={eventOn(events, dates[sel])}
            dense={dense}
            statusFor={statusFor}
            onOpenSlot={onOpenSlot}
          />
        </div>
      </div>
    </>
  );
}

function eventOn(events: CalendarEventDto[], date: Date): CalendarEventDto | undefined {
  const iso = isoDate(date);
  return events.find((e) => e.date === iso);
}

function DayColumn({
  date,
  periods,
  event,
  dense,
  statusFor,
  onOpenSlot,
  onOpenDay,
}: {
  date: Date;
  periods: TimetablePeriod[];
  event: CalendarEventDto | undefined;
  dense: boolean;
  statusFor: (p: TimetablePeriod, d: Date) => SlotStatus;
  onOpenSlot: (p: TimetablePeriod, d: Date) => void;
  onOpenDay: (d: Date) => void;
}) {
  const today = isToday(date);
  const rel = dayRelation(date);
  const shown = periods.slice(0, MAX_CHIPS);
  const overflow = periods.length - shown.length;

  return (
    <div
      className={`flex min-h-[180px] flex-col rounded-xl ${today ? "bg-accent/[0.04] ring-1 ring-inset ring-accent/15" : ""}`}
    >
      {/* day header */}
      <div className="flex flex-col items-center gap-0.5 pb-2 pt-2.5">
        <span className="text-[10.5px] font-semibold uppercase tracking-wide text-ink-3">
          {WEEKDAY_SHORT[isoWeekday(date) - 1]}
        </span>
        <span
          className={`flex h-7 w-7 items-center justify-center rounded-full text-[13px] font-bold ${
            today ? "bg-accent text-white" : "text-navy-deep"
          }`}
        >
          {date.getDate()}
        </span>
      </div>

      {event && (
        <button
          type="button"
          onClick={() => onOpenDay(date)}
          className="mx-1.5 mb-1.5 truncate rounded-lg bg-warning/12 px-2 py-1 text-left text-[10.5px] font-semibold text-warning"
          title={event.event_description || event.event_title}
        >
          {event.event_title}
        </button>
      )}

      <div className="flex flex-1 flex-col gap-1.5 px-1.5 pb-2">
        {periods.length === 0 && !event ? (
          <div className="flex flex-1 items-center justify-center">
            <span className="text-[10.5px] text-ink-3/70">No classes</span>
          </div>
        ) : (
          <>
            {shown.map((p) => (
              <PeriodChip
                key={`${p.id}-${isoDate(date)}`}
                period={p}
                relation={rel}
                status={statusFor(p, date)}
                dense={dense}
                onOpen={() => onOpenSlot(p, date)}
              />
            ))}
            {overflow > 0 && (
              <button
                type="button"
                onClick={() => onOpenDay(date)}
                className="rounded-lg py-1 text-[11px] font-semibold text-accent-deep transition-colors hover:bg-accent/[0.06]"
              >
                +{overflow} more
              </button>
            )}
          </>
        )}
      </div>
    </div>
  );
}

function MobileAgenda({
  date,
  periods,
  event,
  dense,
  statusFor,
  onOpenSlot,
}: {
  date: Date;
  periods: TimetablePeriod[];
  event: CalendarEventDto | undefined;
  dense: boolean;
  statusFor: (p: TimetablePeriod, d: Date) => SlotStatus;
  onOpenSlot: (p: TimetablePeriod, d: Date) => void;
}) {
  const rel = dayRelation(date);
  return (
    <div className="flex flex-col gap-2">
      {event && (
        <div className="rounded-xl bg-warning/12 px-3 py-2 text-[12px] font-semibold text-warning">
          {event.event_title}
          {event.event_description ? (
            <span className="mt-0.5 block font-normal text-ink-2">{event.event_description}</span>
          ) : null}
        </div>
      )}
      {periods.length === 0 ? (
        <p className="py-8 text-center text-[13px] text-ink-3">No classes scheduled.</p>
      ) : (
        periods.map((p) => (
          <PeriodChip
            key={`${p.id}-m`}
            period={p}
            relation={rel}
            status={statusFor(p, date)}
            dense={dense}
            onOpen={() => onOpenSlot(p, date)}
          />
        ))
      )}
    </div>
  );
}
