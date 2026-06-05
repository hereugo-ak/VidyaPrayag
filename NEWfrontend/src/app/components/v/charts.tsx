import { ReactNode } from "react";
import { motion } from "motion/react";

// VDonut — animated donut with center label. Stroke-based, no extra deps.
export function VDonut({
  data, size = 168, thickness = 18, center,
}: { data: { label: string; value: number; color: string }[]; size?: number; thickness?: number; center?: ReactNode }) {
  const total = data.reduce((a, b) => a + b.value, 0) || 1;
  const r = (size - thickness) / 2;
  const c = 2 * Math.PI * r;
  let offset = 0;
  return (
    <div className="relative inline-flex items-center justify-center" style={{ width: size, height: size }}>
      <svg width={size} height={size} className="-rotate-90">
        <circle cx={size / 2} cy={size / 2} r={r} stroke="var(--cream)" strokeWidth={thickness} fill="none" />
        {data.map((d, i) => {
          const len = (d.value / total) * c;
          const seg = (
            <motion.circle
              key={d.label}
              cx={size / 2} cy={size / 2} r={r}
              stroke={d.color} strokeWidth={thickness} fill="none" strokeLinecap="butt"
              strokeDasharray={`${len} ${c}`}
              strokeDashoffset={-offset}
              initial={{ opacity: 0, strokeDasharray: `0 ${c}` }}
              animate={{ opacity: 1, strokeDasharray: `${len} ${c}` }}
              transition={{ duration: 0.8, delay: i * 0.08, ease: "easeOut" }}
            />
          );
          offset += len;
          return seg;
        })}
      </svg>
      <div className="absolute inset-0 flex flex-col items-center justify-center text-center px-2">{center}</div>
    </div>
  );
}

// VSparkline — minimal area sparkline for hero/glance cards.
export function VSparkline({ values, width = 120, height = 36, color = "var(--teal-deep)" }: { values: number[]; width?: number; height?: number; color?: string }) {
  const min = Math.min(...values);
  const max = Math.max(...values);
  const span = max - min || 1;
  const stepX = width / (values.length - 1 || 1);
  const pts = values.map((v, i) => [i * stepX, height - ((v - min) / span) * (height - 4) - 2] as const);
  const path = pts.map(([x, y], i) => (i ? `L${x},${y}` : `M${x},${y}`)).join(" ");
  const area = `${path} L${width},${height} L0,${height} Z`;
  const id = `spark-${Math.random().toString(36).slice(2, 9)}`;
  return (
    <svg width={width} height={height} fill="none" style={{ display: "block" }}>
      <defs>
        <linearGradient id={id} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor={color} stopOpacity="0.28" />
          <stop offset="100%" stopColor={color} stopOpacity="0" />
        </linearGradient>
      </defs>
      <path d={area} fill={`url(#${id})`} />
      <motion.path d={path} stroke={color} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" fill="none"
        initial={{ pathLength: 0 }} animate={{ pathLength: 1 }} transition={{ duration: 1.1, ease: "easeOut" }} />
      <circle cx={pts[pts.length - 1][0]} cy={pts[pts.length - 1][1]} r="3" fill={color} />
    </svg>
  );
}

// VBars — vertical bar chart with hover highlight; reads on white cards.
export function VBars({ data, height = 140 }: { data: { label: string; value: number }[]; height?: number }) {
  const max = Math.max(...data.map((d) => d.value)) || 1;
  return (
    <div className="flex items-end gap-1.5" style={{ height }}>
      {data.map((d, i) => {
        const h = (d.value / max) * (height - 22);
        return (
          <div key={d.label} className="flex-1 flex flex-col items-center gap-1.5">
            <motion.div
              initial={{ height: 0 }} animate={{ height: h }}
              transition={{ duration: 0.6, delay: i * 0.05, ease: "easeOut" }}
              className="w-full rounded-t-[6px] relative"
              style={{ background: i === data.length - 1 ? "var(--teal-deep)" : "rgba(60,185,169,0.45)" }}>
              {i === data.length - 1 && (
                <span className="absolute -top-5 left-1/2 -translate-x-1/2 font-mono" style={{ fontSize: 10, color: "var(--teal-deep)", fontWeight: 700 }}>{d.value}</span>
              )}
            </motion.div>
            <span style={{ fontSize: 10, color: "var(--ink-3)", fontWeight: 600 }}>{d.label}</span>
          </div>
        );
      })}
    </div>
  );
}

// VLegendDot — chip used alongside donut/bar charts.
export function VLegendDot({ color, label, value }: { color: string; label: string; value?: string }) {
  return (
    <div className="inline-flex items-center gap-2">
      <span className="rounded-full" style={{ width: 8, height: 8, background: color }} />
      <span style={{ fontSize: 12, color: "var(--ink-2)" }}>{label}</span>
      {value && <span className="font-mono" style={{ fontSize: 12, color: "var(--ink)", fontWeight: 600 }}>{value}</span>}
    </div>
  );
}
