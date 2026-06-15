"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import {
  useAnalyticsOverview,
  useAttendanceSummary,
  useFeeLedger,
  useMarksSummary,
  useNotifications,
  useOnboardingStatus,
  useStudents,
  useTeachers,
} from "@/lib/admin/hooks";
import { compactMoney, deltaSign, pct } from "@/lib/admin/format";
import { Card, CardHeader, EmptyState, FadeIn, Skeleton } from "@/components/admin/Primitives";
import { StatTile } from "@/components/admin/StatTile";
import { TrendChart, type TrendPoint } from "@/components/admin/charts/TrendChart";
import { BarsChart, type BarDatum } from "@/components/admin/charts/BarsChart";
import { DonutChart } from "@/components/admin/charts/DonutChart";
import {
  IconAnnounce,
  IconChevronRight,
  IconPeople,
  IconLeave,
  IconPlus,
} from "@/components/admin/icons";

export default function DashboardPage() {
  const analytics = useAnalyticsOverview();
  const attendance = useAttendanceSummary();
  const fees = useFeeLedger();
  const marks = useMarksSummary();
  const students = useStudents();
  const teachers = useTeachers();
  const activity = useNotifications();
  const onboarding = useOnboardingStatus();

  // ── derived metrics (all real) ─────────────────────────────────────────────
  const studentCount = students.data?.students.length ?? 0;
  const teacherCount = teachers.data?.teachers.length ?? 0;
  const attRate = attendance.data?.rate ?? 0;
  const growth = analytics.data?.current_growth;

  const trend: TrendPoint[] = useMemo(() => {
    const a = analytics.data;
    if (!a?.performance_trend?.length) return [];
    return a.performance_trend.map((v, i) => ({
      label: a.trend_labels[i] ?? `M${i + 1}`,
      value: Math.round(v * 100),
    }));
  }, [analytics.data]);

  const byClass: BarDatum[] = useMemo(() => {
    const rows = attendance.data?.byClass ?? [];
    return rows.map((r) => ({
      label: r.grade,
      value: r.rate,
      meta: `${r.present}/${r.total} present`,
    }));
  }, [attendance.data]);

  const feeData = fees.data;
  const feeCurrency = feeData?.currency ?? "INR";
  const feeSlices = feeData
    ? [
        { label: "Paid", value: feeData.paidTotal, color: "#3CB9A9" },
        { label: "Due", value: feeData.dueTotal, color: "#6C5CE0" },
        { label: "Overdue", value: feeData.overdueTotal, color: "#B3261E" },
      ]
    : [];
  const feeCollected = feeData
    ? feeData.paidTotal + feeData.dueTotal + feeData.overdueTotal > 0
      ? Math.round(
          (feeData.paidTotal /
            (feeData.paidTotal + feeData.dueTotal + feeData.overdueTotal)) *
            100
        )
      : 0
    : 0;

  return (
    <div className="space-y-6">
      {/* Onboarding nudge (only if incomplete — real status) */}
      {onboarding.data && !onboarding.data.is_complete && (
        <FadeIn>
          <Link
            href="/onboarding"
            className="flex items-center justify-between gap-4 rounded-2xl border border-accent/25 bg-accent/[0.06] px-5 py-3.5 transition-colors hover:bg-accent/[0.1]"
          >
            <p className="text-[13px] font-semibold text-navy-deep">
              Finish setting up your school — {onboarding.data.completion_percent}% complete
            </p>
            <span className="inline-flex items-center gap-1 text-[13px] font-bold text-accent-deep">
              Resume <IconChevronRight width={15} height={15} />
            </span>
          </Link>
        </FadeIn>
      )}

      {/* ── Live metrics bar ─────────────────────────────────────────────── */}
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        <FadeIn delay={0}>
          <StatTile
            label="Attendance today"
            value={pct(attRate)}
            caption={attendance.data?.date ? `As of ${attendance.data.date}` : "No record yet"}
            loading={attendance.isLoading && !attendance.data}
            accent
          />
        </FadeIn>
        <FadeIn delay={0.04}>
          <StatTile
            label="Students"
            value={studentCount.toLocaleString("en-IN")}
            caption="Active roster"
            loading={students.isLoading && !students.data}
          />
        </FadeIn>
        <FadeIn delay={0.08}>
          <StatTile
            label="Teachers"
            value={teacherCount.toLocaleString("en-IN")}
            caption="Provisioned accounts"
            loading={teachers.isLoading && !teachers.data}
          />
        </FadeIn>
        <FadeIn delay={0.12}>
          <StatTile
            label="Fees collected"
            value={`${feeCollected}%`}
            delta={feeData ? compactMoney(feeData.paidTotal, feeCurrency) : undefined}
            deltaTone="up"
            caption={feeData ? `${feeData.paidCount} paid records` : "No ledger yet"}
            loading={fees.isLoading && !fees.data}
          />
        </FadeIn>
      </div>

      {/* ── Primary chart + secondary panels ─────────────────────────────── */}
      <div className="grid gap-6 lg:grid-cols-3">
        {/* Trend (primary) */}
        <FadeIn delay={0.06} className="lg:col-span-2">
          <Card className="h-full pb-5">
            <CardHeader
              title="Attendance performance"
              subtitle="Monthly present-rate, last 6 months"
              action={
                growth ? (
                  <span
                    className={`nums rounded-full px-2.5 py-1 text-[12px] font-bold ${
                      deltaSign(growth) === "down"
                        ? "bg-danger/10 text-danger"
                        : "bg-success/10 text-success"
                    }`}
                  >
                    {growth}
                  </span>
                ) : null
              }
            />
            <div className="px-2 pt-2">
              {analytics.isLoading && !analytics.data ? (
                <Skeleton className="mx-3 h-[240px]" />
              ) : trend.length ? (
                <TrendChart data={trend} />
              ) : (
                <EmptyState
                  title="No attendance trend yet"
                  hint="Once teachers start marking attendance, the 6-month trend appears here."
                />
              )}
            </div>
          </Card>
        </FadeIn>

        {/* Fees donut (secondary) */}
        <FadeIn delay={0.1}>
          <Card className="flex h-full flex-col pb-5">
            <CardHeader title="Fee collection" subtitle="Paid vs outstanding" />
            <div className="flex flex-1 flex-col items-center justify-center gap-4 px-5 pt-2">
              {fees.isLoading && !fees.data ? (
                <Skeleton className="h-[200px] w-[200px] rounded-full" />
              ) : feeData ? (
                <>
                  <DonutChart
                    data={feeSlices}
                    centerLabel="collected"
                    centerValue={`${feeCollected}%`}
                    fmt={(n) => compactMoney(n, feeCurrency)}
                  />
                  <div className="grid w-full grid-cols-3 gap-2 text-center">
                    {feeSlices.map((s) => (
                      <div key={s.label}>
                        <span
                          className="mx-auto mb-1 block h-1.5 w-6 rounded-full"
                          style={{ background: s.color }}
                        />
                        <p className="nums text-[13px] font-bold text-navy-deep">
                          {compactMoney(s.value, feeCurrency)}
                        </p>
                        <p className="text-[11px] text-ink-3">{s.label}</p>
                      </div>
                    ))}
                  </div>
                </>
              ) : (
                <EmptyState title="No fee records yet" />
              )}
            </div>
          </Card>
        </FadeIn>
      </div>

      {/* ── Attendance by class + activity feed ──────────────────────────── */}
      <div className="grid gap-6 lg:grid-cols-3">
        <FadeIn delay={0.06} className="lg:col-span-2">
          <Card className="h-full pb-5">
            <CardHeader
              title="Attendance by class"
              subtitle="Today's present-rate per grade — click a bar to drill in"
              action={
                <Link
                  href="/admin/attendance"
                  className="text-[12px] font-semibold text-accent hover:underline"
                >
                  See all
                </Link>
              }
            />
            <div className="px-2 pt-2">
              {attendance.isLoading && !attendance.data ? (
                <Skeleton className="mx-3 h-[240px]" />
              ) : byClass.length ? (
                <BarsChart data={byClass} unit="%" />
              ) : (
                <EmptyState title="No class attendance for today yet" />
              )}
            </div>
          </Card>
        </FadeIn>

        {/* Live activity feed */}
        <FadeIn delay={0.1}>
          <Card className="flex h-full flex-col">
            <CardHeader title="Recent activity" subtitle="Live across your school" />
            <div className="flex-1 overflow-y-auto px-2 py-2">
              {activity.isLoading && !activity.data ? (
                <div className="space-y-3 p-3">
                  {Array.from({ length: 5 }).map((_, i) => (
                    <Skeleton key={i} className="h-12" />
                  ))}
                </div>
              ) : (activity.data?.notifications.length ?? 0) === 0 ? (
                <EmptyState title="No recent activity" hint="New events show up here in real time." />
              ) : (
                <ul className="divide-y divide-navy/6">
                  {activity.data!.notifications.slice(0, 8).map((n) => (
                    <li key={n.id} className="flex gap-3 px-3 py-3">
                      <span
                        className={`mt-1.5 h-2 w-2 shrink-0 rounded-full ${
                          n.unread ? "bg-accent" : "bg-navy/15"
                        }`}
                        aria-hidden="true"
                      />
                      <div className="min-w-0">
                        <p className="truncate text-[13px] font-semibold text-navy-deep">{n.title}</p>
                        <p className="line-clamp-1 text-[12px] text-ink-3">{n.body}</p>
                        <p className="mt-0.5 text-[11px] text-ink-placeholder">{n.time}</p>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </Card>
        </FadeIn>
      </div>

      {/* ── Quick actions ────────────────────────────────────────────────── */}
      <FadeIn delay={0.06}>
        <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
          <QuickAction href="/admin/people?add=student" icon={<IconPlus />} label="Add student" />
          <QuickAction href="/admin/people?add=teacher" icon={<IconPeople />} label="Add teacher" />
          <QuickAction href="/admin/announcements?new=1" icon={<IconAnnounce />} label="New announcement" />
          <QuickAction href="/admin/leave" icon={<IconLeave />} label="Review leave" />
        </div>
      </FadeIn>
    </div>
  );
}

function QuickAction({
  href,
  icon,
  label,
}: {
  href: string;
  icon: React.ReactNode;
  label: string;
}) {
  return (
    <Link
      href={href}
      className="group flex items-center gap-3 rounded-2xl border border-navy/8 bg-white/85 px-4 py-4 transition-all duration-200 hover:-translate-y-0.5 hover:border-accent/30 hover:shadow-card"
    >
      <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-accent/10 text-accent-deep">
        {icon}
      </span>
      <span className="text-[14px] font-semibold text-navy-deep">{label}</span>
    </Link>
  );
}
