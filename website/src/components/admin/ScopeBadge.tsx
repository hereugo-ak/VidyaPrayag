"use client";

import { IconGrid } from "./icons";

/**
 * Visible indicator shown on every panel that the GLOBAL class filter
 * (Control B) is scoping (the two-filter law: "every scoped panel shows a
 * visible indicator that it is scoped"). Optionally carries a `note` for the
 * honest cases where a panel cannot truly scope (e.g. fees are school-wide).
 */
export function ScopeBadge({
  className: cls,
  note,
}: {
  className: string;
  note?: string;
}) {
  return (
    <span
      className="inline-flex items-center gap-1.5 rounded-full bg-accent/10 px-2.5 py-0.5 text-[11px] font-semibold text-accent-deep"
      title={note}
    >
      <IconGrid width={12} height={12} />
      {note ? note : `Scoped to ${cls}`}
    </span>
  );
}
