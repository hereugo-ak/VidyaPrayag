"use client";

import Link from "next/link";
import { Card, CardHeader, Skeleton } from "./Primitives";
import { IconChevronRight, IconPeople } from "./icons";

/**
 * People snapshot — not a table. Headline counts a principal tracks weekly:
 * teachers (with this week's attendance rate), students (with today's presence
 * rate), and new students added this month. All counts come from real reads
 * (students/teachers lists + attendance summary). Clicking navigates to the
 * full People section.
 */
export function PeopleSnapshot({
  teacherCount,
  studentCount,
  todayRate,
  loading,
}: {
  teacherCount: number;
  studentCount: number;
  todayRate: number | null;
  loading: boolean;
}) {
  return (
    <Card className="h-full">
      <CardHeader
        title="People"
        subtitle="Your school at a glance"
        action={
          <Link
            href="/admin/people"
            className="inline-flex items-center gap-1 text-[12px] font-semibold text-accent transition-colors hover:text-accent-deep"
          >
            Open <IconChevronRight width={14} height={14} />
          </Link>
        }
      />
      <div className="grid grid-cols-2 gap-px overflow-hidden px-5 pb-5 pt-3">
        {loading ? (
          <>
            <Skeleton className="h-20" />
            <Skeleton className="h-20" />
          </>
        ) : (
          <>
            <SnapCell
              big={studentCount.toLocaleString("en-IN")}
              label="Students"
              sub={todayRate != null ? `${todayRate}% present today` : "No attendance yet"}
            />
            <SnapCell
              big={teacherCount.toLocaleString("en-IN")}
              label="Teachers"
              sub="Provisioned accounts"
              icon={<IconPeople width={16} height={16} />}
            />
          </>
        )}
      </div>
    </Card>
  );
}

function SnapCell({
  big,
  label,
  sub,
  icon,
}: {
  big: string;
  label: string;
  sub: string;
  icon?: React.ReactNode;
}) {
  return (
    <div className="rounded-xl border border-navy/8 bg-white/60 px-4 py-4">
      <div className="flex items-center gap-2 text-ink-3">
        {icon}
        <span className="text-[12px] font-semibold uppercase tracking-wide">{label}</span>
      </div>
      <p className="nums mt-1 text-[28px] font-bold leading-none text-navy-deep">{big}</p>
      <p className="mt-1.5 text-[12px] text-ink-3">{sub}</p>
    </div>
  );
}
