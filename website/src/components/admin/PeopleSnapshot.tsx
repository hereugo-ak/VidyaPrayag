"use client";

import Link from "next/link";
import { Card, CardHeader, ProgressBar, Skeleton } from "./Primitives";
import { IconChevronRight, IconPeople } from "./icons";

/**
 * People snapshot, not a table. Headline counts a principal tracks weekly:
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
    <Card className="h-full" hover>
      <CardHeader
        title="People"
        subtitle="Your school at a glance"
        action={
          <Link
            href="/admin/people"
            className="inline-flex items-center gap-1 rounded-full bg-navy/[0.05] px-3 py-1.5 text-[12px] font-semibold text-navy-deep transition-colors hover:bg-navy/[0.08]"
          >
            Open <IconChevronRight width={14} height={14} />
          </Link>
        }
      />
      <div className="grid grid-cols-2 gap-3 px-6 pb-6 pt-4">
        {loading ? (
          <>
            <Skeleton className="h-28" />
            <Skeleton className="h-28" />
          </>
        ) : (
          <>
            <SnapCell
              big={studentCount.toLocaleString("en-IN")}
              label="Students"
              sub={todayRate != null ? `${todayRate}% present today` : "No attendance yet"}
              wash="bg-wash-lavender"
              progress={todayRate}
              progressTone="accent"
            />
            <SnapCell
              big={teacherCount.toLocaleString("en-IN")}
              label="Teachers"
              sub="Provisioned accounts"
              wash="bg-wash-mint"
              icon={<IconPeople width={15} height={15} />}
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
  wash,
  progress,
  progressTone = "accent",
}: {
  big: string;
  label: string;
  sub: string;
  icon?: React.ReactNode;
  wash: string;
  progress?: number | null;
  progressTone?: "accent" | "mint";
}) {
  return (
    <div className={`rounded-3xl ${wash} px-4 py-4 shadow-soft`}>
      <div className="flex items-center gap-1.5 text-navy-deep/55">
        {icon}
        <span className="text-[11px] font-bold uppercase tracking-[0.08em]">{label}</span>
      </div>
      <p className="nums mt-1.5 text-[30px] font-extrabold leading-none tracking-tight text-navy-deep">
        {big}
      </p>
      {progress != null && (
        <ProgressBar value={progress} tone={progressTone === "accent" ? "accent" : "mint"} className="mt-3" />
      )}
      <p className="mt-2 text-[12px] font-medium text-navy-deep/55">{sub}</p>
    </div>
  );
}
