"use client";

import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

export interface TrendPoint {
  label: string;
  value: number; // 0..100 (percentage)
}

function TrendTooltip({
  active,
  payload,
  label,
  unit,
}: {
  active?: boolean;
  payload?: { value: number }[];
  label?: string;
  unit: string;
}) {
  if (!active || !payload?.length) return null;
  return (
    <div className="rounded-xl border border-navy/10 bg-white px-3 py-2 shadow-cardHover">
      <p className="text-[11px] font-semibold uppercase tracking-wide text-ink-3">{label}</p>
      <p className="nums text-[15px] font-bold text-navy-deep">
        {payload[0].value}
        {unit}
      </p>
    </div>
  );
}

/**
 * Interactive performance trend — hover shows exact values. Animates once on
 * mount, then static. Data is the real monthly attendance present-rate from
 * /api/v1/school/analytics/overview.
 */
export function TrendChart({
  data,
  unit = "%",
  height = 240,
}: {
  data: TrendPoint[];
  unit?: string;
  height?: number;
}) {
  return (
    <ResponsiveContainer width="100%" height={height}>
      <AreaChart data={data} margin={{ top: 8, right: 8, left: -18, bottom: 0 }}>
        <defs>
          <linearGradient id="trendFill" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#6C5CE0" stopOpacity={0.22} />
            <stop offset="100%" stopColor="#6C5CE0" stopOpacity={0.01} />
          </linearGradient>
        </defs>
        <CartesianGrid stroke="#26234D" strokeOpacity={0.06} vertical={false} />
        <XAxis
          dataKey="label"
          tickLine={false}
          axisLine={false}
          tick={{ fontSize: 11, fill: "#6D7A77" }}
          dy={6}
        />
        <YAxis
          tickLine={false}
          axisLine={false}
          tick={{ fontSize: 11, fill: "#6D7A77" }}
          width={42}
          domain={[0, 100]}
          tickFormatter={(v) => `${v}${unit}`}
        />
        <Tooltip
          content={<TrendTooltip unit={unit} />}
          cursor={{ stroke: "#6C5CE0", strokeOpacity: 0.25, strokeWidth: 1 }}
        />
        <Area
          type="monotone"
          dataKey="value"
          stroke="#6C5CE0"
          strokeWidth={2.5}
          fill="url(#trendFill)"
          dot={{ r: 0 }}
          activeDot={{ r: 5, strokeWidth: 2, stroke: "#fff", fill: "#6C5CE0" }}
          animationDuration={650}
        />
      </AreaChart>
    </ResponsiveContainer>
  );
}
