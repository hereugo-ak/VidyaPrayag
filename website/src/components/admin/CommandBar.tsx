"use client";

import Link from "next/link";
import type { IntelligenceMeta } from "@/lib/admin/types";
import { IconAttendance, IconAnnounce, IconLeave, IconPlus } from "./icons";

/**
 * Command bar, the top of the Command Center. Shows the real school name,
 * today's date and academic week (from the intelligence payload), and the
 * four contextual one-click actions a principal reaches for first. These are
 * real navigations / action triggers, not decoration.
 */
export function CommandBar({
  meta,
  onCompose,
}: {
  meta: IntelligenceMeta | undefined;
  onCompose: () => void;
}) {
  const today = new Date();
  const dateLabel = today.toLocaleDateString("en-IN", {
    weekday: "long",
    day: "numeric",
    month: "long",
  });

  return (
    <div className="flex flex-col gap-4 rounded-2xl border border-navy/8 bg-gradient-to-br from-navy-deep to-navy px-5 py-5 text-white shadow-card md:flex-row md:items-center md:justify-between md:px-6">
      <div className="min-w-0">
        <p className="text-[12px] font-medium uppercase tracking-[0.14em] text-white/55">
          {dateLabel}
          {meta?.academic_week ? ` · Academic week ${meta.academic_week}` : ""}
        </p>
        <div className="mt-1 flex items-center gap-2.5">
          <h1 className="truncate text-[22px] font-bold tracking-tight !text-white md:text-[26px]">
            {meta?.school_name ?? "Command Center"}
          </h1>
          <span className="hidden shrink-0 items-center gap-1.5 rounded-full bg-white/10 px-2.5 py-1 text-[10.5px] font-semibold uppercase tracking-[0.12em] text-white/80 ring-1 ring-inset ring-white/15 sm:inline-flex">
            <span className="relative flex h-1.5 w-1.5">
              <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-teal opacity-75" />
              <span className="relative inline-flex h-1.5 w-1.5 rounded-full bg-teal" />
            </span>
            Live
          </span>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-2 sm:grid-cols-4 md:flex md:gap-2.5">
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
    primary
      ? "bg-white text-navy-deep hover:bg-white/90"
      : "bg-white/10 text-white hover:bg-white/18"
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
