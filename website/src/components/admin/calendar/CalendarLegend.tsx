"use client";

import { classColor } from "@/lib/admin/classColor";

/**
 * Colour legend under the calendar header (CALENDAR_SPEC §3). Lists the classes
 * currently in view with their stable swatch. In single-class (Control A) mode
 * the parent passes just that one class. Colour is paired with the class name —
 * never colour alone.
 */
export function CalendarLegend({ classes }: { classes: string[] }) {
  if (!classes.length) return null;
  return (
    <div className="flex flex-wrap items-center gap-x-3 gap-y-1.5" aria-label="Class colours">
      {classes.map((c) => {
        const col = classColor(c);
        return (
          <span key={c} className="inline-flex items-center gap-1.5 text-[11px] font-semibold text-ink-2">
            <span
              className="h-2.5 w-2.5 rounded-[3px]"
              style={{ backgroundColor: col.rail }}
              aria-hidden="true"
            />
            {c}
          </span>
        );
      })}
    </div>
  );
}
