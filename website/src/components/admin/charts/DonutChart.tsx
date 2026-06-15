"use client";

import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from "recharts";

export interface DonutSlice {
  label: string;
  value: number;
  color: string;
}

function DonutTooltip({
  active,
  payload,
  fmt,
}: {
  active?: boolean;
  payload?: { payload: DonutSlice }[];
  fmt: (n: number) => string;
}) {
  if (!active || !payload?.length) return null;
  const d = payload[0].payload;
  return (
    <div className="rounded-xl border border-navy/10 bg-white px-3 py-2 shadow-cardHover">
      <p className="text-[12px] font-semibold text-navy-deep">{d.label}</p>
      <p className="nums text-[14px] font-bold" style={{ color: d.color }}>
        {fmt(d.value)}
      </p>
    </div>
  );
}

/**
 * Donut with a centred KPI. Interactive — hovering a slice shows its formatted
 * value. Data is the real fee ledger split (paid/due/overdue).
 */
export function DonutChart({
  data,
  centerLabel,
  centerValue,
  fmt = (n) => String(n),
  size = 200,
}: {
  data: DonutSlice[];
  centerLabel: string;
  centerValue: string;
  fmt?: (n: number) => string;
  size?: number;
}) {
  const total = data.reduce((s, d) => s + d.value, 0);
  return (
    <div className="relative" style={{ width: size, height: size }}>
      <ResponsiveContainer width="100%" height="100%">
        <PieChart>
          <Pie
            data={total === 0 ? [{ label: "No data", value: 1, color: "#E6E6FA" }] : data}
            dataKey="value"
            innerRadius={size * 0.32}
            outerRadius={size * 0.46}
            paddingAngle={total === 0 ? 0 : 2}
            stroke="none"
            animationDuration={600}
          >
            {(total === 0 ? [{ color: "#E6E6FA" }] : data).map((d, i) => (
              <Cell key={i} fill={d.color} />
            ))}
          </Pie>
          {total > 0 && <Tooltip content={<DonutTooltip fmt={fmt} />} />}
        </PieChart>
      </ResponsiveContainer>
      <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center">
        <span className="nums text-[22px] font-extrabold tracking-tight text-navy-deep">
          {centerValue}
        </span>
        <span className="text-[11px] font-semibold uppercase tracking-wide text-ink-3">
          {centerLabel}
        </span>
      </div>
    </div>
  );
}
