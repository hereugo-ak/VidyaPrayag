"use client";

import {
  Bar,
  BarChart,
  Cell,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

export interface BarDatum {
  label: string;
  value: number;
  meta?: string;
}

function BarTooltip({
  active,
  payload,
  unit,
}: {
  active?: boolean;
  payload?: { payload: BarDatum; value: number }[];
  unit: string;
}) {
  if (!active || !payload?.length) return null;
  const d = payload[0].payload;
  return (
    <div className="rounded-2xl bg-navy-deep px-3.5 py-2.5 shadow-cardHover">
      <p className="text-[11px] font-semibold uppercase tracking-wide text-white/55">{d.label}</p>
      <p className="nums text-[15px] font-extrabold text-white">
        {payload[0].value}
        {unit}
      </p>
      {d.meta && <p className="text-[11px] text-white/55">{d.meta}</p>}
    </div>
  );
}

/**
 * Interactive bar chart with per-bar hover + optional click drill-down.
 * Colour intensity scales with value so weak bars read instantly.
 */
export function BarsChart({
  data,
  unit = "%",
  height = 240,
  onSelect,
  max = 100,
}: {
  data: BarDatum[];
  unit?: string;
  height?: number;
  onSelect?: (d: BarDatum) => void;
  max?: number;
}) {
  return (
    <ResponsiveContainer width="100%" height={height}>
      <BarChart data={data} margin={{ top: 8, right: 8, left: -18, bottom: 0 }} barCategoryGap="28%">
        <defs>
          <linearGradient id="barAccent" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#8B7EE8" />
            <stop offset="100%" stopColor="#6C5CE0" />
          </linearGradient>
          <linearGradient id="barTeal" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#5ED0C0" />
            <stop offset="100%" stopColor="#3CB9A9" />
          </linearGradient>
          <linearGradient id="barPeach" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#FFB59B" />
            <stop offset="100%" stopColor="#FF8A65" />
          </linearGradient>
        </defs>
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
          tick={{ fontSize: 11, fill: "#9AA6A3" }}
          width={42}
          domain={[0, max]}
          tickFormatter={(v) => `${v}${unit}`}
        />
        <Tooltip content={<BarTooltip unit={unit} />} cursor={{ fill: "#6C5CE0", fillOpacity: 0.05 }} />
        <Bar
          dataKey="value"
          radius={[10, 10, 4, 4]}
          maxBarSize={48}
          animationDuration={650}
          onClick={(d) => onSelect?.(d as unknown as BarDatum)}
          cursor={onSelect ? "pointer" : "default"}
        >
          {data.map((d, i) => {
            const ratio = Math.min(1, d.value / max);
            // soft pastel pill bars — teal (strong) / accent (mid) / peach (low)
            const fill =
              ratio >= 0.85 ? "url(#barTeal)" : ratio >= 0.6 ? "url(#barAccent)" : "url(#barPeach)";
            return <Cell key={i} fill={fill} />;
          })}
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  );
}
