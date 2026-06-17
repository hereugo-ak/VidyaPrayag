"use client";

import { ClassFilter } from "../ClassFilter";
import { IconChevronRight } from "../icons";
import type { CalView } from "@/lib/admin/useDashboardUrlState";

/**
 * Calendar widget header (CALENDAR_SPEC §1–2). Carries:
 *   • the range label (the viewed week range, or "June 2026" in month view)
 *   • ‹ › navigation + a "Today" snap-back
 *   • a Week | Month segmented control
 *   • Control A — the CALENDAR-SCOPED class filter (independent of the global one)
 *
 * The global-scope context (Control B), when set, is shown as a read-only hint
 * so a user understands why the calendar is pre-scoped, without Control A
 * mutating the global filter.
 */
export function CalendarHeader({
  rangeLabel,
  view,
  onView,
  onPrev,
  onNext,
  onToday,
  isCurrentRange,
  classes,
  calClass,
  onCalClass,
  globalClass,
}: {
  rangeLabel: string;
  view: CalView;
  onView: (v: CalView) => void;
  onPrev: () => void;
  onNext: () => void;
  onToday: () => void;
  isCurrentRange: boolean;
  classes: string[];
  calClass: string | null;
  onCalClass: (c: string | null) => void;
  globalClass: string | null;
}) {
  return (
    <div className="flex flex-col gap-3 border-b border-navy/8 px-5 py-4 lg:flex-row lg:items-center lg:justify-between">
      {/* Left: range + navigation */}
      <div className="flex items-center gap-2">
        <div className="flex items-center gap-0.5 rounded-xl border border-navy/10 bg-white p-0.5">
          <NavBtn label="Previous" onClick={onPrev} flip />
          <button
            type="button"
            onClick={onToday}
            disabled={isCurrentRange}
            className="rounded-lg px-2.5 py-1.5 text-[12px] font-semibold text-ink-2 transition-colors hover:bg-navy/[0.04] hover:text-navy-deep disabled:cursor-default disabled:opacity-40 disabled:hover:bg-transparent"
          >
            Today
          </button>
          <NavBtn label="Next" onClick={onNext} />
        </div>
        <h3 className="text-[15px] font-bold tracking-tight text-navy-deep">{rangeLabel}</h3>
      </div>

      {/* Right: view toggle + Control A */}
      <div className="flex flex-wrap items-center gap-2.5">
        <div className="inline-flex rounded-xl border border-navy/10 bg-white p-0.5" role="tablist" aria-label="Calendar view">
          <Seg active={view === "week"} onClick={() => onView("week")} label="Week" />
          <Seg active={view === "month"} onClick={() => onView("month")} label="Month" />
        </div>
        <div className="flex items-center gap-1.5">
          {globalClass && !calClass && (
            <span className="hidden text-[11px] font-medium text-ink-3 sm:inline" title="Scoped by the dashboard-global filter">
              global scope
            </span>
          )}
          <ClassFilter
            classes={classes}
            value={calClass}
            onChange={onCalClass}
            tone="light"
            size="sm"
          />
        </div>
      </div>
    </div>
  );
}

function NavBtn({ label, onClick, flip }: { label: string; onClick: () => void; flip?: boolean }) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-label={label}
      className="rounded-lg p-1.5 text-ink-2 transition-colors hover:bg-navy/[0.04] hover:text-navy-deep focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent/40"
    >
      <IconChevronRight width={16} height={16} className={flip ? "rotate-180" : ""} />
    </button>
  );
}

function Seg({ active, onClick, label }: { active: boolean; onClick: () => void; label: string }) {
  return (
    <button
      type="button"
      role="tab"
      aria-selected={active}
      onClick={onClick}
      className={`rounded-lg px-3 py-1.5 text-[12px] font-semibold transition-colors ${
        active ? "bg-accent text-white shadow-sm" : "text-ink-2 hover:text-navy-deep"
      }`}
    >
      {label}
    </button>
  );
}
