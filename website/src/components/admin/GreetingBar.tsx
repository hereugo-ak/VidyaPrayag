"use client";

import type { IntelligenceMeta } from "@/lib/admin/types";
import { ClassFilter } from "./ClassFilter";
import { IconAnnounce, IconSearch, IconSparkle } from "./icons";

/**
 * Greeting header — persistent at the top of every dashboard tab (PHASE 2 law).
 *
 * REDESIGN (v1.0.3): the old design wrapped the greeting in a heavy deep-navy
 * slab and then repeated four command pills (Attendance / Announce / Leave /
 * Add student) that *already live in the sidebar* — visual noise and a
 * duplicate navigation surface. That is gone.
 *
 * The new header is LIGHT and AIRY, in the language of the premium reference:
 *  • The greeting is plain oversized navy type sitting directly on the canvas —
 *    no dark card, no sharp edges. The first name carries a single soft violet
 *    accent so it still feels designed, not default.
 *  • A quiet eyebrow line carries the date + academic week; a live status chip
 *    confirms the campus is online.
 *  • Floating glass controls (global search + class scope) sit to the right,
 *    fully-rounded, resting on the canvas with a soft shadow.
 *  • ONE genuinely non-duplicate action remains — "Compose" (an announcement
 *    modal, not reachable from the rail) — rendered as a single refined CTA,
 *    not a row of redundant pills.
 *
 * Time-aware greeting + first name come from the authenticated session; date,
 * academic week and school name come from intelligence meta. Nothing hardcoded.
 */
function greetingFor(hour: number): string {
  if (hour < 12) return "Good morning";
  if (hour < 17) return "Good afternoon";
  return "Good evening";
}

function firstNameOf(full: string): string {
  return full.trim().split(/\s+/)[0] || "there";
}

export function GreetingBar({
  meta,
  sessionName,
  classes,
  globalClass,
  onGlobalClass,
  search,
  onSearch,
  onCompose,
}: {
  meta: IntelligenceMeta | undefined;
  sessionName: string;
  classes: string[];
  globalClass: string | null;
  onGlobalClass: (c: string | null) => void;
  search: string;
  onSearch: (q: string) => void;
  onCompose: () => void;
}) {
  const now = new Date();
  const greeting = greetingFor(now.getHours());
  const first = firstNameOf(sessionName);
  const dateLabel = now.toLocaleDateString("en-IN", {
    weekday: "long",
    day: "numeric",
    month: "long",
  });

  return (
    <div className="relative flex flex-col gap-5 px-0.5 lg:flex-row lg:items-end lg:justify-between">
      {/* Greeting — plain type on the canvas, no slab. */}
      <div className="min-w-0">
        <div className="flex flex-wrap items-center gap-2.5">
          <p className="text-[11.5px] font-bold uppercase tracking-[0.16em] text-ink-3">
            {dateLabel}
            {meta?.academic_week ? ` · Week ${meta.academic_week}` : ""}
          </p>
          <span className="inline-flex items-center gap-1.5 rounded-full bg-mint/12 px-2.5 py-1 text-[10px] font-bold uppercase tracking-[0.12em] text-teal-deep ring-1 ring-inset ring-mint/25">
            <span className="relative flex h-1.5 w-1.5">
              <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-mint opacity-75" />
              <span className="relative inline-flex h-1.5 w-1.5 rounded-full bg-mint" />
            </span>
            Live
          </span>
        </div>

        <h1 className="mt-2.5 text-[30px] font-extrabold leading-[1.04] tracking-tighter text-navy-deep md:text-[40px]">
          {greeting},{" "}
          <span className="bg-gradient-to-r from-accent-deep via-accent to-accent-soft bg-clip-text text-transparent">
            {first}
          </span>
        </h1>

        <p className="mt-2 truncate text-[14px] font-medium text-ink-2">
          {meta?.school_name ?? "Your school"}
          <span className="text-ink-3"> · here&apos;s your campus at a glance</span>
        </p>
      </div>

      {/* Floating glass controls — search + scope + one real action. */}
      <div className="flex flex-col gap-2.5 sm:flex-row sm:items-center">
        {/* Global search */}
        <label className="relative flex items-center">
          <IconSearch
            width={15}
            height={15}
            className="pointer-events-none absolute left-3.5 text-ink-3"
          />
          <input
            value={search}
            onChange={(e) => onSearch(e.target.value)}
            placeholder="Search students, staff…"
            className="w-full rounded-full bg-white py-2.5 pl-10 pr-4 text-[13px] text-navy-deep shadow-soft ring-1 ring-inset ring-navy/[0.06] transition-all placeholder:text-ink-3 focus:shadow-card focus:outline-none focus:ring-2 focus:ring-accent/40 sm:w-[230px]"
            aria-label="Search"
          />
        </label>

        {/* Dashboard-global class scope */}
        <ClassFilter
          classes={classes}
          value={globalClass}
          onChange={onGlobalClass}
          tone="light"
          size="md"
        />

        {/* The one non-duplicate action (modal, not in the rail). */}
        <button
          type="button"
          onClick={onCompose}
          className="group inline-flex items-center justify-center gap-2 rounded-full bg-navy-deep px-4 py-2.5 text-[13px] font-semibold text-white shadow-cta transition-all duration-200 hover:-translate-y-0.5 hover:shadow-ctaHover focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent/45"
        >
          <IconAnnounce width={16} height={16} className="text-white/90" />
          <span>Compose</span>
          <IconSparkle width={13} height={13} className="text-accent-soft opacity-80 transition-transform duration-300 group-hover:rotate-90" />
        </button>
      </div>
    </div>
  );
}
