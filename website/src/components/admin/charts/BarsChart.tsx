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
    <div className="rounded-xl border border-navy/10 bg-white px-3 py-2 shadow-cardHover">
      <p className="text-[12px] font-semibold text-navy-deep">{d.label}</p>
      <p className="nums text-[15px] font-bold text-accent-deep">
        {payload[0].value}
        {unit}
      </p>
      {d.meta && <p className="text-[11px] text-ink-3">{d.meta}</p>}
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
          domain={[0, max]}
          tickFormatter={(v) => `${v}${unit}`}
        />
        <Tooltip content={<BarTooltip unit={unit} />} cursor={{ fill: "#6C5CE0", fillOpacity: 0.06 }} />
        <Bar
          dataKey="value"
          radius={[6, 6, 0, 0]}
          animationDuration={600}
          onClick={(d) => onSelect?.(d as unknown as BarDatum)}
          cursor={onSelect ? "pointer" : "default"}
        >
          {data.map((d, i) => {
            const ratio = Math.min(1, d.value / max);
            // teal→accent gradient by value, both from the system palette
            const color = ratio >= 0.85 ? "#3CB9A9" : ratio >= 0.6 ? "#6C5CE0" : "#B3651A";
            return <Cell key={i} fill={color} fillOpacity={0.85} />;
          })}
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  );
}
