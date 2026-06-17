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
    <div className="max-w-[220px] rounded-xl border border-navy/10 bg-white px-3 py-2.5 shadow-cardHover">
      <p className="text-[11px] font-semibold uppercase tracking-wide text-ink-3">{label}</p>
      <p className="nums mt-0.5 text-[16px] font-bold text-navy-deep">
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
        <p className="mt-1.5 rounded-md bg-accent/8 px-2 py-1 text-[11px] font-semibold text-accent-deep">
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

  const fmtDay = (s: string) => {
    const d = new Date(s);
    return isNaN(d.getTime()) ? s.slice(5) : d.toLocaleDateString("en-IN", { day: "numeric", month: "short" });
  };

  return (
    <>
      <Card className="h-full pb-4">
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
              <div className="h-[260px] w-full">
                <ResponsiveContainer width="100%" height="100%">
                  <ComposedChart data={points} margin={{ top: 8, right: 12, left: -16, bottom: 0 }}>
                    <defs>
                      <linearGradient id="attFill" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="0%" stopColor="#6C5CE0" stopOpacity={0.18} />
                        <stop offset="100%" stopColor="#6C5CE0" stopOpacity={0} />
                      </linearGradient>
                    </defs>
                    <CartesianGrid stroke="#26234D" strokeOpacity={0.06} vertical={false} />
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
                      stroke="#6C5CE0"
                      strokeWidth={2.25}
                      fill="url(#attFill)"
                      dot={false}
                      activeDot={{ r: 4, fill: "#6C5CE0" }}
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
            <div className="rounded-xl border border-navy/8 bg-white/70 p-4">
              <p className="text-[13px] font-semibold text-navy-deep">Why this is flagged</p>
              <p className="mt-1 text-[13px] leading-relaxed text-ink-2">
                Present-rate of <b>{selected.rate}%</b> fell below the period mean of{" "}
                <b>{mean}%</b>, {mean - selected.rate} points under normal. {selected.absent} of{" "}
                {selected.total} students were absent.
              </p>
            </div>
            {selected.exam ? (
              <div className="rounded-xl border border-accent/20 bg-accent/[0.06] p-4">
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
    <div className="rounded-xl border border-navy/8 bg-white/70 px-3 py-3 text-center">
      <p className={`nums text-[20px] font-bold ${map[tone]}`}>{value}</p>
      <p className="mt-0.5 text-[11px] text-ink-3">{label}</p>
    </div>
  );
}
