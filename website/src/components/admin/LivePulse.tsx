"use client";

import { useEffect, useRef, useState } from "react";

/**
 * Live pulse — the at-a-glance KPI row, present on every tab (PHASE 2 law).
 *
 * Redesigned to the reference language: instead of one cramped strip carved up
 * by divider lines, each metric is its own borderless white "pillow" card with a
 * soft pastel icon chip, a big tabular number, and a quiet label. A small "Live"
 * marker on the first card signals the values are polling. On each value change
 * the number flashes briefly so a principal glancing at the tab sees movement
 * without a page refresh.
 *
 * Fed by the LIVE hooks (15s SWR poll: notifications + today's attendance) and
 * the NEAR-LIVE intelligence payload (60s). No websocket — tuned polling is the
 * right tool (the Ktor backend exposes no realtime channel to the web client).
 */
export interface PulseMetric {
  label: string;
  value: number | string;
  tone?: "accent" | "success" | "warning" | "danger" | "neutral";
}

const toneStyle = {
  accent: { text: "text-accent-deep", chip: "bg-accent/10 text-accent-deep" },
  success: { text: "text-success", chip: "bg-success/10 text-success" },
  warning: { text: "text-warning", chip: "bg-warning/12 text-warning" },
  danger: { text: "text-danger", chip: "bg-danger/10 text-danger" },
  neutral: { text: "text-navy-deep", chip: "bg-navy/[0.06] text-ink-2" },
} as const;

export function LivePulse({ metrics, live }: { metrics: PulseMetric[]; live: boolean }) {
  return (
    <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
      {metrics.map((m, i) => (
        <PulseCard key={m.label} metric={m} live={live && i === 0} />
      ))}
    </div>
  );
}

function PulseCard({ metric, live }: { metric: PulseMetric; live: boolean }) {
  const [flash, setFlash] = useState(false);
  const prev = useRef(metric.value);
  useEffect(() => {
    if (prev.current !== metric.value) {
      prev.current = metric.value;
      setFlash(true);
      const t = setTimeout(() => setFlash(false), 800);
      return () => clearTimeout(t);
    }
  }, [metric.value]);

  const s = toneStyle[metric.tone ?? "neutral"];
  const dot = metric.tone ? s.chip : "bg-navy/[0.06]";

  return (
    <div className="group relative overflow-hidden rounded-3xl bg-white px-4 py-3.5 shadow-soft ring-1 ring-navy/[0.04] transition-shadow duration-300 hover:shadow-card">
      <div className="flex items-center justify-between">
        <span className="truncate text-[11px] font-semibold uppercase tracking-[0.08em] text-ink-3">
          {metric.label}
        </span>
        {live ? (
          <span className="inline-flex items-center gap-1 text-[10px] font-bold uppercase tracking-[0.1em] text-teal-deep">
            <span className="relative flex h-1.5 w-1.5">
              <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-mint opacity-70" />
              <span className="relative inline-flex h-1.5 w-1.5 rounded-full bg-teal-deep" />
            </span>
            Live
          </span>
        ) : (
          <span className={`h-1.5 w-1.5 rounded-full ${dot}`} aria-hidden />
        )}
      </div>
      <p
        className={`nums mt-1.5 text-[26px] font-extrabold leading-none tracking-tight transition-colors duration-300 ${
          flash ? "text-teal-deep" : s.text
        }`}
      >
        {metric.value}
      </p>
    </div>
  );
}
