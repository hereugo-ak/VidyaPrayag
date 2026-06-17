"use client";

import { useEffect, useRef, useState } from "react";
import { IconPulse } from "./icons";

/**
 * Live pulse strip, a narrow band of metrics that change through the day.
 * Fed by the LIVE hooks (15s SWR poll: notifications + today's attendance) and
 * the NEAR-LIVE intelligence payload (60s). On each value change the number
 * flashes briefly so a principal glancing at the tab sees movement without a
 * page refresh. No websocket, tuned polling is the right tool here (the Ktor
 * backend exposes no Supabase realtime channel to the web client).
 */
export interface PulseMetric {
  label: string;
  value: number | string;
  tone?: "accent" | "success" | "warning" | "danger" | "neutral";
}

const toneText = {
  accent: "text-accent-deep",
  success: "text-success",
  warning: "text-warning",
  danger: "text-danger",
  neutral: "text-navy-deep",
} as const;

export function LivePulse({ metrics, live }: { metrics: PulseMetric[]; live: boolean }) {
  return (
    <div className="flex items-stretch gap-3 overflow-x-auto rounded-2xl border border-navy/8 bg-white/85 px-4 py-3 shadow-card backdrop-blur-sm">
      <div className="flex shrink-0 items-center gap-2 pr-2 text-[12px] font-semibold text-ink-3">
        <span className="relative flex h-2 w-2">
          {live && (
            <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-teal opacity-60" />
          )}
          <span className={`relative inline-flex h-2 w-2 rounded-full ${live ? "bg-teal-deep" : "bg-ink-placeholder"}`} />
        </span>
        <IconPulse width={15} height={15} className="text-teal-deep" />
        <span className="hidden sm:inline">Live</span>
      </div>
      <div className="flex flex-1 items-center divide-x divide-navy/8">
        {metrics.map((m) => (
          <PulseCell key={m.label} metric={m} />
        ))}
      </div>
    </div>
  );
}

function PulseCell({ metric }: { metric: PulseMetric }) {
  const [flash, setFlash] = useState(false);
  const prev = useRef(metric.value);
  useEffect(() => {
    if (prev.current !== metric.value) {
      prev.current = metric.value;
      setFlash(true);
      const t = setTimeout(() => setFlash(false), 700);
      return () => clearTimeout(t);
    }
  }, [metric.value]);

  return (
    <div className="flex min-w-[96px] flex-col px-4 first:pl-2">
      <span
        className={`nums text-[19px] font-bold leading-tight transition-colors duration-300 ${
          flash ? "text-teal-deep" : toneText[metric.tone ?? "neutral"]
        }`}
      >
        {metric.value}
      </span>
      <span className="truncate text-[11px] text-ink-3">{metric.label}</span>
    </div>
  );
}
