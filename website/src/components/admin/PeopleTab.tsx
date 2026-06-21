"use client";

import { useMemo } from "react";
import Link from "next/link";

import type {
  StudentListResponse,
  TeacherListResponse,
  AttendanceSummaryDto,
} from "@/lib/admin/types";
import { Avatar, Card, CardHeader, EmptyState, ProgressBar, Skeleton } from "./Primitives";
import { RosterPreview } from "./RosterPreview";
import { IconChevronRight, IconPeople } from "./icons";

/**
 * PeopleTab — the people command center, in the premium reference rhythm
 * (website palette). Composition:
 *   • a snapshot strip: students / teachers / today's presence — pastel washes.
 *   • class distribution (real counts by class_name) so the empty gap that the
 *     old single snapshot card left is now a meaningful breakdown.
 *   • roster preview (left, real students) + teachers directory (right, real
 *     teacher accounts) side-by-side.
 * Every number is from real reads (students / teachers / attendance). Empty-first.
 */
export function PeopleTab({
  students,
  teachers,
  attendance,
  loading,
  scopeClass,
}: {
  students: StudentListResponse | undefined;
  teachers: TeacherListResponse | undefined;
  attendance: AttendanceSummaryDto | undefined;
  loading: boolean;
  scopeClass: string | null;
}) {
  const studentCount = students?.students.length ?? 0;
  const teacherCount = teachers?.teachers.length ?? 0;
  const todayRate = attendance?.rate ?? null;

  // Real distribution by class (honours scope by highlighting, not hiding).
  const distribution = useMemo(() => {
    const map = new Map<string, number>();
    (students?.students ?? []).forEach((s) => {
      map.set(s.class_name, (map.get(s.class_name) ?? 0) + 1);
    });
    return Array.from(map.entries())
      .map(([cls, count]) => ({ cls, count }))
      .sort((a, b) => a.cls.localeCompare(b.cls));
  }, [students]);
  const maxClass = Math.max(1, ...distribution.map((d) => d.count));

  return (
    <div className="space-y-5">
      {/* Snapshot strip */}
      <div className="grid gap-4 sm:grid-cols-3">
        <SnapTile
          label="Students"
          value={loading && !students ? "—" : studentCount.toLocaleString("en-IN")}
          sub={todayRate != null ? `${todayRate}% present today` : "No attendance yet"}
          wash="bg-wash-lavender"
          progress={todayRate}
          progressTone="accent"
        />
        <SnapTile
          label="Teachers"
          value={loading && !teachers ? "—" : teacherCount.toLocaleString("en-IN")}
          sub="Provisioned accounts"
          wash="bg-wash-mint"
          progressTone="mint"
        />
        <SnapTile
          label="Classes"
          value={loading && !students ? "—" : String(distribution.length)}
          sub={distribution.length ? "With enrolled students" : "No classes yet"}
          wash="bg-wash-sky"
          progressTone="accent"
        />
      </div>

      {/* Class distribution + roster */}
      <div className="grid gap-5 xl:grid-cols-[1fr_1.55fr]">
        <Card hover>
          <CardHeader
            title="Class distribution"
            subtitle="Students enrolled per class"
            action={
              <Link
                href="/admin/people"
                className="inline-flex items-center gap-1 rounded-full bg-navy/[0.05] px-3 py-1.5 text-[12px] font-semibold text-navy-deep transition-colors hover:bg-navy/[0.08]"
              >
                Open <IconChevronRight width={14} height={14} />
              </Link>
            }
          />
          <div className="px-5 pb-5 pt-2">
            {loading && !students ? (
              <div className="space-y-3">
                {Array.from({ length: 4 }).map((_, i) => (
                  <Skeleton key={i} className="h-7" />
                ))}
              </div>
            ) : distribution.length === 0 ? (
              <EmptyState
                icon={<IconPeople />}
                title="No students yet"
                hint="Import your roster from People to see how students spread across classes."
              />
            ) : (
              <ul className="space-y-3.5">
                {distribution.map((d) => {
                  const active = scopeClass && d.cls === scopeClass;
                  return (
                    <li key={d.cls}>
                      <div className="mb-1 flex items-center justify-between">
                        <span
                          className={`text-[13px] font-semibold ${
                            active ? "text-accent-deep" : "text-navy-deep"
                          }`}
                        >
                          {d.cls}
                          {active ? " ·" : ""}
                        </span>
                        <span className="nums text-[12px] font-bold text-ink-3">{d.count}</span>
                      </div>
                      <ProgressBar
                        value={Math.round((d.count / maxClass) * 100)}
                        tone={active ? "accent" : "mint"}
                      />
                    </li>
                  );
                })}
              </ul>
            )}
          </div>
        </Card>

        <RosterPreview data={students} loading={loading} scopeClass={scopeClass} />
      </div>

      {/* Teachers directory */}
      <Card hover>
        <CardHeader
          title="Teaching staff"
          subtitle="Provisioned teacher accounts"
          action={
            <Link
              href="/admin/people"
              className="inline-flex items-center gap-1 rounded-full px-3 py-1.5 text-[12.5px] font-bold text-accent-deep transition-colors hover:bg-accent/[0.06]"
            >
              Manage staff <IconChevronRight width={14} height={14} />
            </Link>
          }
        />
        <div className="px-3 pb-4 pt-2">
          {loading && !teachers ? (
            <div className="grid gap-2 p-2 sm:grid-cols-2 lg:grid-cols-3">
              {Array.from({ length: 6 }).map((_, i) => (
                <Skeleton key={i} className="h-14" />
              ))}
            </div>
          ) : teacherCount === 0 ? (
            <EmptyState
              icon={<IconPeople />}
              title="No teacher accounts yet"
              hint="Add staff in People to assign them to classes and let them mark attendance and marks."
            />
          ) : (
            <ul className="grid gap-1.5 sm:grid-cols-2 lg:grid-cols-3">
              {teachers!.teachers.map((t) => (
                <li key={t.id}>
                  <Link
                    href="/admin/people"
                    className="flex items-center gap-3 rounded-2xl px-3 py-2.5 transition-colors hover:bg-navy/[0.035]"
                  >
                    <Avatar name={t.name} size={36} />
                    <div className="min-w-0">
                      <p className="truncate text-[13px] font-semibold text-navy-deep">{t.name}</p>
                      <p className="truncate text-[12px] text-ink-3">{t.email ?? "Staff account"}</p>
                    </div>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </div>
      </Card>
    </div>
  );
}

function SnapTile({
  label,
  value,
  sub,
  wash,
  progress,
  progressTone = "accent",
}: {
  label: string;
  value: string;
  sub: string;
  wash: string;
  progress?: number | null;
  progressTone?: "accent" | "mint";
}) {
  return (
    <div className={`rounded-4xl ${wash} px-5 py-5 shadow-soft`}>
      <div className="flex items-center gap-1.5 text-navy-deep/55">
        <IconPeople width={15} height={15} />
        <span className="text-[11px] font-bold uppercase tracking-[0.08em]">{label}</span>
      </div>
      <p className="nums mt-2 text-[32px] font-extrabold leading-none tracking-tight text-navy-deep">
        {value}
      </p>
      {progress != null && (
        <ProgressBar value={progress} tone={progressTone === "accent" ? "accent" : "mint"} className="mt-3" />
      )}
      <p className="mt-2 text-[12px] font-medium text-navy-deep/55">{sub}</p>
    </div>
  );
}
