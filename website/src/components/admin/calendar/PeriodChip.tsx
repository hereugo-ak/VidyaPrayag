"use client";

import { classColor } from "@/lib/admin/classColor";
import { classSectionLabel, teacherFirstName, type DayRelation } from "@/lib/admin/calendarUtils";
import type { TimetablePeriod } from "@/lib/admin/types";

/**
 * A single period in the week grid. Left rail in the class colour; two compact
 * lines (subject·class, then time·teacher·room). For TODAY it carries a live
 * status dot (see CALENDAR_SPEC §1):
 *   • green (pulsing) = attendance marked for that class today
 *   • amber (pulsing) = scheduled today, not yet marked
 *   • grey           = future period (today, but later than now) / unknown
 * Colour is never the only signal — the accessible label spells the status out.
 *
 * `dense` (Control-A single-class mode) gives the chip room to also show the
 * full teacher name + room on their own line.
 */
export type SlotStatus = "marked" | "pending" | "future" | "none";

export function PeriodChip({
  period,
  relation,
  status = "none",
  dense = false,
  onOpen,
}: {
  period: TimetablePeriod;
  relation: DayRelation;
  status?: SlotStatus;
  dense?: boolean;
  onOpen: () => void;
}) {
  const c = classColor(period.class_name);
  const cls = classSectionLabel(period);
  const teacher = teacherFirstName(period.teacher_name);
  const showDot = relation === "today" && status !== "none";

  const a11y = [
    period.subject || "Period",
    cls,
    `${period.start_time}–${period.end_time}`,
    teacher || "teacher unassigned",
    relation === "today" ? STATUS_LABEL[status] : relation,
  ].join(", ");

  return (
    <button
      type="button"
      onClick={onOpen}
      aria-label={a11y}
      className="group relative flex w-full items-stretch gap-2 overflow-hidden rounded-2xl bg-white px-2.5 py-2 text-left shadow-soft ring-1 ring-navy/[0.05] transition-all duration-200 hover:-translate-y-0.5 hover:shadow-card focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent/40"
      style={{ backgroundColor: c.tintBg }}
    >
      <span
        aria-hidden="true"
        className="absolute inset-y-0 left-0 w-[3px] rounded-l-xl"
        style={{ backgroundColor: c.rail }}
      />
      <div className="min-w-0 flex-1 pl-1.5">
        <div className="flex items-center gap-1.5">
          <p className="truncate text-[12.5px] font-bold leading-tight text-navy-deep">
            {period.subject || "Period"}
          </p>
          {showDot && <StatusDot status={status} />}
        </div>
        <p className="truncate text-[11px] font-semibold leading-tight" style={{ color: c.ink }}>
          {cls}
        </p>
        <p className="mt-0.5 truncate text-[10.5px] leading-tight text-ink-3">
          {period.start_time}–{period.end_time}
          {teacher ? ` · ${teacher}` : ""}
          {period.room ? ` · ${period.room}` : ""}
        </p>
        {dense && (period.teacher_name || period.room) && (
          <p className="mt-1 truncate text-[10.5px] leading-tight text-ink-2">
            {period.teacher_name || "Unassigned"}
            {period.room ? ` · Room ${period.room}` : ""}
          </p>
        )}
      </div>
    </button>
  );
}

const STATUS_LABEL: Record<SlotStatus, string> = {
  marked: "attendance marked",
  pending: "attendance not marked yet",
  future: "upcoming today",
  none: "",
};

function StatusDot({ status }: { status: SlotStatus }) {
  if (status === "future") {
    return <span className="h-1.5 w-1.5 shrink-0 rounded-full bg-ink-3/50" aria-hidden="true" />;
  }
  const color = status === "marked" ? "bg-success" : "bg-warning";
  return (
    <span className="relative flex h-1.5 w-1.5 shrink-0" aria-hidden="true">
      <span className={`absolute inline-flex h-full w-full animate-ping rounded-full ${color} opacity-70`} />
      <span className={`relative inline-flex h-1.5 w-1.5 rounded-full ${color}`} />
    </span>
  );
}
