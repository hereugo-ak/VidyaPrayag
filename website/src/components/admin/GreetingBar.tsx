"use client";

import Link from "next/link";
import type { IntelligenceMeta } from "@/lib/admin/types";
import { ClassFilter } from "./ClassFilter";
import { IconAttendance, IconAnnounce, IconLeave, IconPlus, IconSearch } from "./icons";

/**
 * Greeting hero — persistent at the top of every dashboard tab (PHASE 2 law).
 *
 * Redesigned to the premium reference language:
 *  • A deep navy surface lit by a soft aurora (lavender + mint light-sources)
 *    so it reads as an expensive "command surface", not a flat banner.
 *  • Oversized, low-tracking greeting headline ("Good evening, Rakesh").
 *  • Glassy, fully-rounded pill controls (search + scope filter) on the right.
 *  • Action row rendered as soft rounded pills; one solid-white primary CTA for
 *    high contrast (mirrors the dark/white pill contrast in the references).
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
    <div className="hero-surface relative isolate overflow-hidden rounded-5xl p-6 text-white shadow-pillow ring-1 ring-inset ring-white/[0.06] md:p-9">
      {/* faint top sheen for depth (kept above the base, below content) */}
      <div
        className="pointer-events-none absolute inset-0 -z-10 opacity-[0.6]"
        style={{
          background:
            "radial-gradient(80% 120% at 50% -20%, rgba(255,255,255,0.12) 0%, rgba(255,255,255,0) 60%)",
        }}
        aria-hidden
      />
      {/* hairline highlight along the top edge — the 'expensive glass' tell */}
      <div
        className="pointer-events-none absolute inset-x-0 top-0 -z-10 h-px bg-gradient-to-r from-transparent via-white/25 to-transparent"
        aria-hidden
      />

      <div className="relative flex flex-col gap-6">
        {/* Row 1: greeting + context + global controls */}
        <div className="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
          <div className="min-w-0">
            <p className="text-[11.5px] font-semibold uppercase tracking-[0.18em] text-white/55">
              {dateLabel}
              {meta?.academic_week ? ` · Academic week ${meta.academic_week}` : ""}
            </p>
            <h1 className="mt-2 text-[30px] font-extrabold leading-[1.04] tracking-tighter text-white md:text-[38px]">
              {greeting},{" "}
              <span className="bg-gradient-to-r from-white via-white to-[#cdc6ff] bg-clip-text text-transparent">
                {first}
              </span>
            </h1>
            <div className="mt-3 flex flex-wrap items-center gap-2.5">
              <p className="truncate text-[13.5px] font-medium text-white/75">
                {meta?.school_name ?? "Your school"}
              </p>
              <span className="inline-flex shrink-0 items-center gap-1.5 rounded-full bg-white/10 px-2.5 py-1 text-[10px] font-bold uppercase tracking-[0.12em] text-white/85 ring-1 ring-inset ring-white/15">
                <span className="relative flex h-1.5 w-1.5">
                  <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-mint opacity-75" />
                  <span className="relative inline-flex h-1.5 w-1.5 rounded-full bg-mint" />
                </span>
                Live
              </span>
            </div>
          </div>

          <div className="flex flex-col gap-2.5 sm:flex-row sm:items-center">
            {/* Global search */}
            <label className="relative flex items-center">
              <IconSearch
                width={15}
                height={15}
                className="pointer-events-none absolute left-3.5 text-white/50"
              />
              <input
                value={search}
                onChange={(e) => onSearch(e.target.value)}
                placeholder="Search students, staff…"
                className="w-full rounded-full bg-white/10 py-2.5 pl-10 pr-4 text-[13px] text-white placeholder:text-white/45 ring-1 ring-inset ring-white/15 transition-colors focus:bg-white/[0.16] focus:outline-none focus:ring-2 focus:ring-white/45 sm:w-[230px]"
                aria-label="Search"
              />
            </label>

            {/* Control B — dashboard-global class filter */}
            <div className="flex items-center gap-2">
              <span className="hidden text-[10.5px] font-bold uppercase tracking-[0.12em] text-white/45 md:inline">
                Scope
              </span>
              <ClassFilter
                classes={classes}
                value={globalClass}
                onChange={onGlobalClass}
                tone="onDark"
                size="sm"
              />
            </div>
          </div>
        </div>

        {/* Row 2: command actions (high-frequency tasks the backend supports) */}
        <div className="grid grid-cols-2 gap-2.5 sm:grid-cols-4 md:flex md:flex-wrap">
          <CmdAction href="/admin/attendance" icon={<IconAttendance width={16} height={16} />} label="Attendance" />
          <CmdButton onClick={onCompose} icon={<IconAnnounce width={16} height={16} />} label="Announce" />
          <CmdAction href="/admin/leave?status=Pending" icon={<IconLeave width={16} height={16} />} label="Leave" />
          <CmdAction href="/admin/people?add=student" icon={<IconPlus width={16} height={16} />} label="Add student" primary />
        </div>
      </div>
    </div>
  );
}

function shell(primary?: boolean) {
  return `inline-flex items-center justify-center gap-2 rounded-full px-4 py-2.5 text-[13px] font-semibold transition-all duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/45 ${
    primary
      ? "bg-white text-navy-deep shadow-pill hover:-translate-y-0.5 hover:bg-white/95"
      : "bg-white/10 text-white ring-1 ring-inset ring-white/15 hover:bg-white/[0.18]"
  }`;
}

function CmdAction({
  href,
  icon,
  label,
  primary,
}: {
  href: string;
  icon: React.ReactNode;
  label: string;
  primary?: boolean;
}) {
  return (
    <Link href={href} className={shell(primary)}>
      {icon}
      <span>{label}</span>
    </Link>
  );
}

function CmdButton({
  onClick,
  icon,
  label,
}: {
  onClick: () => void;
  icon: React.ReactNode;
  label: string;
}) {
  return (
    <button type="button" onClick={onClick} className={shell(false)}>
      {icon}
      <span>{label}</span>
    </button>
  );
}
