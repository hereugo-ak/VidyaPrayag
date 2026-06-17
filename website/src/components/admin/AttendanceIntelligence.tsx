"use client";

import { useMemo, useState } from "react";
import {
  Area,
  ComposedChart,
  CartesianGrid,
  ReferenceLine,
  ResponsiveContainer,
  Scatter,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import type { AttendancePoint } from "@/lib/admin/types";
import { Card, CardHeader, EmptyState, Skeleton, Badge } from "./Primitives";
import { SidePanel } from "./SidePanel";

/**
 * Primary intelligence panel, daily attendance present-rate over the last 30
 * days. Anomaly days (rate below the server's dynamic threshold) are rendered
 * as red dots; exam days (from assessments + academic_calendar) get a vertical
 * marker so the correlation between exams and attendance dips is visible.
 *
 * Interactive: click any anomaly dot → a side panel slides in with that day's
 * absent/present breakdown and any exam that fell on it. No page reload.
 * Every value comes from the /dashboard/intelligence payload (real data).
 */
function ChartTooltip({
  active,
  payload,
}: {
  active?: boolean;
  payload?: { payload: AttendancePoint }[];
}) {
  if (!active || !payload?.length) return null;
  const p = payload[0].payload;
  const d = new Date(p.date);
  const label = isNaN(d.getTime())
    ? p.date
    : d.toLocaleDateString("en-IN", { weekday: "short", day: "numeric", month: "short" });
  return (
    <div className="max-w-[220px] rounded-2xl bg-white px-3.5 py-3 shadow-cardHover ring-1 ring-navy/[0.05]">
      <p className="text-[11px] font-semibold uppercase tracking-wide text-ink-3">{label}</p>
      <p className="nums mt-0.5 text-[17px] font-extrabold text-navy-deep">
        {p.rate}% present
      </p>
      <p className="nums text-[12px] text-ink-3">
        {p.present} of {p.total} students
      </p>
      {p.is_anomaly && (
        <p className="mt-1.5 text-[11px] font-semibold text-danger">
          ↓ Below normal, click to inspect
        </p>
      )}
      {p.exam && (
        <p className="mt-1.5 rounded-lg bg-accent/8 px-2 py-1 text-[11px] font-semibold text-accent-deep">
          Exam: {p.exam}
        </p>
      )}
    </div>
  );
}

export function AttendanceIntelligence({
  data,
  loading,
}: {
  data: AttendancePoint[] | undefined;
  loading: boolean;
}) {
  const [selected, setSelected] = useState<AttendancePoint | null>(null);

  const points = useMemo(() => data ?? [], [data]);
  const anomalies = useMemo(() => points.filter((p) => p.is_anomaly), [points]);
  const examDays = useMemo(() => points.filter((p) => p.exam), [points]);
  const mean = useMemo(
    () => (points.length ? Math.round(points.reduce((s, p) => s + p.rate, 0) / points.length) : 0),
    [points]
  );

  // Recent-trend delta — last week's average present-rate vs the prior week.
  // Drives the floating gradient badge (the reference "+24%" highlight chip),
  // but built from REAL data, so it reads "+3 pts" / "−2 pts" / "On par".
  const delta = useMemo(() => {
    if (points.length < 4) return null;
    const avg = (arr: AttendancePoint[]) =>
      arr.reduce((s, p) => s + p.rate, 0) / Math.max(1, arr.length);
    const recent = points.slice(-7);
    const prior = points.slice(-14, -7);
    if (!prior.length) return null;
    const diff = Math.round(avg(recent) - avg(prior));
    return { diff, recentAvg: Math.round(avg(recent)) };
  }, [points]);

  const fmtDay = (s: string) => {
    const d = new Date(s);
    return isNaN(d.getTime()) ? s.slice(5) : d.toLocaleDateString("en-IN", { day: "numeric", month: "short" });
  };

  return (
    <>
      <Card className="h-full pb-5" hover>
        <CardHeader
          title="Attendance intelligence"
          subtitle="Daily present-rate · anomalies flagged · exam days overlaid"
          action={
            <div className="flex items-center gap-2">
              {anomalies.length > 0 && (
                <Badge tone="danger">{anomalies.length} anomaly{anomalies.length > 1 ? " days" : " day"}</Badge>
              )}
              {examDays.length > 0 && <Badge tone="accent">{examDays.length} exam day{examDays.length > 1 ? "s" : ""}</Badge>}
            </div>
          }
        />
        <div className="px-2 pt-3">
          {loading && !data ? (
            <Skeleton className="mx-3 h-[260px]" />
          ) : points.length >= 2 ? (
            <>
              <div className="relative h-[280px] w-full">
                {/* Floating recent-trend badge — the reference "+24%" highlight,
                    but real: last 7 days' present-rate average vs the prior 7. */}
                {delta && (
                  <div className="pointer-events-none absolute right-4 top-2 z-10 animate-floaty">
                    <div
                      className={`rounded-2xl px-3.5 py-2 shadow-float ${
                        delta.diff >= 0 ? "bg-wash-lavender" : "bg-wash-peach"
                      }`}
                    >
                      <p
                        className={`nums text-[18px] font-extrabold leading-none ${
                          delta.diff >= 0 ? "text-accent-deep" : "text-[#C2410C]"
                        }`}
                      >
                        {delta.diff > 0 ? "+" : ""}
                        {delta.diff} pts
                      </p>
                      <p className="mt-0.5 text-[10px] font-semibold text-navy-deep/60">
                        7-day vs prior
                      </p>
                    </div>
                  </div>
                )}
                <ResponsiveContainer width="100%" height="100%">
                  <ComposedChart data={points} margin={{ top: 8, right: 12, left: -16, bottom: 0 }}>
                    <defs>
                      <linearGradient id="attFill" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="0%" stopColor="#6C5CE0" stopOpacity={0.28} />
                        <stop offset="55%" stopColor="#6C5CE0" stopOpacity={0.08} />
                        <stop offset="100%" stopColor="#6C5CE0" stopOpacity={0} />
                      </linearGradient>
                      <linearGradient id="attStroke" x1="0" y1="0" x2="1" y2="0">
                        <stop offset="0%" stopColor="#8B7EE8" />
                        <stop offset="100%" stopColor="#6C5CE0" />
                      </linearGradient>
                    </defs>
                    <CartesianGrid stroke="#26234D" strokeOpacity={0.05} vertical horizontal={false} />
                    <XAxis
                      dataKey="date"
                      tickFormatter={fmtDay}
                      tick={{ fontSize: 11, fill: "#6D7A77" }}
                      tickLine={false}
                      axisLine={false}
                      minTickGap={28}
                    />
                    <YAxis
                      domain={[0, 100]}
                      tick={{ fontSize: 11, fill: "#6D7A77" }}
                      tickLine={false}
                      axisLine={false}
                      width={42}
                      tickFormatter={(v) => `${v}%`}
                    />
                    <Tooltip content={<ChartTooltip />} cursor={{ stroke: "#6C5CE0", strokeOpacity: 0.25 }} />
                    {/* mean reference */}
                    {mean > 0 && (
                      <ReferenceLine
                        y={mean}
                        stroke="#3CB9A9"
                        strokeDasharray="4 4"
                        strokeOpacity={0.7}
                      />
                    )}
                    {/* exam-day vertical markers */}
                    {examDays.map((e) => (
                      <ReferenceLine
                        key={`x-${e.date}`}
                        x={e.date}
                        stroke="#544AB8"
                        strokeOpacity={0.35}
                        strokeDasharray="2 3"
                      />
                    ))}
                    <Area
                      type="monotone"
                      dataKey="rate"
                      stroke="url(#attStroke)"
                      strokeWidth={3}
                      strokeLinecap="round"
                      fill="url(#attFill)"
                      dot={false}
                      activeDot={{ r: 5, fill: "#6C5CE0", stroke: "#fff", strokeWidth: 2 }}
                    />
                    {/* anomaly dots, clickable */}
                    <Scatter
                      data={anomalies}
                      dataKey="rate"
                      fill="#B3261E"
                      shape={(props: { cx?: number; cy?: number; payload?: AttendancePoint }) => {
                        const { cx, cy, payload } = props;
                        if (cx == null || cy == null) return <g />;
                        return (
                          <g
                            style={{ cursor: "pointer" }}
                            onClick={() => payload && setSelected(payload)}
                          >
                            <circle cx={cx} cy={cy} r={9} fill="#B3261E" fillOpacity={0.14} />
                            <circle cx={cx} cy={cy} r={4.5} fill="#B3261E" stroke="#fff" strokeWidth={1.5} />
                          </g>
                        );
                      }}
                    />
                  </ComposedChart>
                </ResponsiveContainer>
              </div>
              <div className="flex flex-wrap items-center gap-4 px-4 pt-2 text-[11px] text-ink-3">
                <span className="inline-flex items-center gap-1.5">
                  <span className="h-2.5 w-2.5 rounded-full bg-danger" /> Anomaly day
                </span>
                <span className="inline-flex items-center gap-1.5">
                  <span className="h-0.5 w-4 bg-teal-deep" /> Period mean {mean}%
                </span>
                <span className="inline-flex items-center gap-1.5">
                  <span className="h-3 w-0.5 bg-accent-deep/50" /> Exam day
                </span>
              </div>
            </>
          ) : (
            <EmptyState
              title="Not enough attendance history yet"
              hint="Once teachers mark attendance across several days, the daily trend, anomalies, and exam correlation appear here."
            />
          )}
        </div>
      </Card>

      <SidePanel
        open={!!selected}
        onClose={() => setSelected(null)}
        title={selected ? "Attendance anomaly" : ""}
        subtitle={
          selected
            ? new Date(selected.date).toLocaleDateString("en-IN", {
                weekday: "long",
                day: "numeric",
                month: "long",
              })
            : undefined
        }
      >
        {selected && (
          <div className="space-y-5">
            <div className="grid grid-cols-3 gap-3">
              <Stat label="Present" value={`${selected.present}`} tone="success" />
              <Stat label="Absent" value={`${selected.absent}`} tone="danger" />
              <Stat label="Rate" value={`${selected.rate}%`} tone="accent" />
            </div>
            <div className="rounded-2xl bg-navy/[0.03] p-4 ring-1 ring-inset ring-navy/[0.05]">
              <p className="text-[13px] font-semibold text-navy-deep">Why this is flagged</p>
              <p className="mt-1 text-[13px] leading-relaxed text-ink-2">
                Present-rate of <b>{selected.rate}%</b> fell below the period mean of{" "}
                <b>{mean}%</b>, {mean - selected.rate} points under normal. {selected.absent} of{" "}
                {selected.total} students were absent.
              </p>
            </div>
            {selected.exam ? (
              <div className="rounded-2xl bg-wash-lavender p-4 shadow-soft">
                <p className="text-[13px] font-semibold text-accent-deep">Exam on this day</p>
                <p className="mt-1 text-[13px] leading-relaxed text-ink-2">
                  {selected.exam} was scheduled. Attendance dips on exam days are worth a closer
                  look, confirm whether absences were exam-related.
                </p>
              </div>
            ) : (
              <p className="text-[13px] leading-relaxed text-ink-3">
                No exam was scheduled this day, so the dip is not exam-driven. Review whether a
                holiday, weather event, or local factor explains it.
              </p>
            )}
          </div>
        )}
      </SidePanel>
    </>
  );
}

function Stat({
  label,
  value,
  tone,
}: {
  label: string;
  value: string;
  tone: "success" | "danger" | "accent";
}) {
  const map = {
    success: "text-success",
    danger: "text-danger",
    accent: "text-accent-deep",
  } as const;
  return (
    <div className="rounded-2xl bg-navy/[0.03] px-3 py-3.5 text-center ring-1 ring-inset ring-navy/[0.05]">
      <p className={`nums text-[21px] font-extrabold ${map[tone]}`}>{value}</p>
      <p className="mt-0.5 text-[11px] text-ink-3">{label}</p>
    </div>
  );
}
