"use client";

import { useState } from "react";
import Link from "next/link";
import type { IntelligenceMeta } from "@/lib/admin/types";
import { ClassFilter } from "./ClassFilter";
import { IconAttendance, IconAnnounce, IconLeave, IconPlus, IconSearch } from "./icons";

/**
 * Greeting bar — persistent at the top of every dashboard tab (PHASE 2 law).
 *
 *  • Time-aware greeting: "Good morning|afternoon|evening, <first name>".
 *    First name comes from the authenticated session (never hardcoded).
 *  • Context: today's date, academic week, school name (from intelligence meta).
 *  • Hosts the GLOBAL class filter (Control B) — scopes every panel — and a
 *    global search. Both always visible.
 *  • Carries the highest-frequency one-click actions (real navigations the Ktor
 *    backend supports: attendance, announce, leave, add student).
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
    <div className="flex flex-col gap-4 rounded-2xl border border-navy/8 bg-gradient-to-br from-navy-deep to-navy px-5 py-5 text-white shadow-card md:px-6">
      {/* Row 1: greeting + context + global controls */}
      <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
        <div className="min-w-0">
          <p className="text-[12px] font-medium uppercase tracking-[0.14em] text-white/55">
            {dateLabel}
            {meta?.academic_week ? ` · Academic week ${meta.academic_week}` : ""}
          </p>
          <h1 className="mt-1 truncate text-[22px] font-bold tracking-tight !text-white md:text-[26px]">
            {greeting}, {first}
          </h1>
          <div className="mt-1 flex items-center gap-2">
            <p className="truncate text-[13px] text-white/65">
              {meta?.school_name ?? "Your school"}
            </p>
            <span className="hidden shrink-0 items-center gap-1.5 rounded-full bg-white/10 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-[0.12em] text-white/80 ring-1 ring-inset ring-white/15 sm:inline-flex">
              <span className="relative flex h-1.5 w-1.5">
                <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-teal opacity-75" />
                <span className="relative inline-flex h-1.5 w-1.5 rounded-full bg-teal" />
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
              className="pointer-events-none absolute left-3 text-white/50"
            />
            <input
              value={search}
              onChange={(e) => onSearch(e.target.value)}
              placeholder="Search students, staff…"
              className="w-full rounded-xl bg-white/10 py-2 pl-9 pr-3 text-[13px] text-white placeholder:text-white/45 ring-1 ring-inset ring-white/15 transition-colors focus:bg-white/15 focus:outline-none focus:ring-2 focus:ring-white/40 sm:w-[220px]"
              aria-label="Search"
            />
          </label>

          {/* Control B — dashboard-global class filter */}
          <div className="flex items-center gap-2">
            <span className="hidden text-[11px] font-semibold uppercase tracking-[0.1em] text-white/45 md:inline">
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
      <div className="grid grid-cols-2 gap-2 sm:grid-cols-4 md:flex md:flex-wrap md:gap-2.5">
        <CmdAction href="/admin/attendance" icon={<IconAttendance width={16} height={16} />} label="Attendance" />
        <CmdButton onClick={onCompose} icon={<IconAnnounce width={16} height={16} />} label="Announce" />
        <CmdAction href="/admin/leave?status=Pending" icon={<IconLeave width={16} height={16} />} label="Leave" />
        <CmdAction href="/admin/people?add=student" icon={<IconPlus width={16} height={16} />} label="Add student" primary />
      </div>
    </div>
  );
}

function shell(primary?: boolean) {
  return `inline-flex items-center justify-center gap-1.5 rounded-xl px-3.5 py-2.5 text-[13px] font-semibold transition-all duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/40 ${
    primary ? "bg-white text-navy-deep hover:bg-white/90" : "bg-white/10 text-white hover:bg-white/[0.18]"
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
