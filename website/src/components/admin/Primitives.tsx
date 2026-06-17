"use client";

import { motion } from "framer-motion";

/** Surface card, the base container for every admin panel. */
export function Card({
  children,
  className = "",
  as = "div",
}: {
  children: React.ReactNode;
  className?: string;
  as?: "div" | "section";
}) {
  const Comp = as;
  return (
    <Comp
      className={`rounded-2xl border border-navy/8 bg-white/85 shadow-card backdrop-blur-sm ${className}`}
    >
      {children}
    </Comp>
  );
}

export function CardHeader({
  title,
  subtitle,
  action,
}: {
  title: string;
  subtitle?: string;
  action?: React.ReactNode;
}) {
  return (
    <div className="flex items-start justify-between gap-4 px-5 pt-5">
      <div>
        <h2 className="text-[15px] font-bold tracking-tight text-navy-deep">{title}</h2>
        {subtitle && <p className="mt-0.5 text-[13px] text-ink-3">{subtitle}</p>}
      </div>
      {action}
    </div>
  );
}

type Tone = "neutral" | "success" | "warning" | "danger" | "accent";
const toneMap: Record<Tone, string> = {
  neutral: "bg-navy/6 text-ink-2",
  success: "bg-success/10 text-success",
  warning: "bg-warning/12 text-warning",
  danger: "bg-danger/10 text-danger",
  accent: "bg-accent/10 text-accent-deep",
};

export function Badge({
  children,
  tone = "neutral",
  className = "",
}: {
  children: React.ReactNode;
  tone?: Tone;
  className?: string;
}) {
  return (
    <span
      className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-[11px] font-semibold ${toneMap[tone]} ${className}`}
    >
      {children}
    </span>
  );
}

/** Loading skeleton bar. Plays no looping animation beyond a subtle shimmer. */
export function Skeleton({ className = "" }: { className?: string }) {
  return <div className={`animate-pulse rounded-lg bg-navy/8 ${className}`} />;
}

export function EmptyState({
  title,
  hint,
  icon,
}: {
  title: string;
  hint?: string;
  icon?: React.ReactNode;
}) {
  return (
    <div className="flex flex-col items-center justify-center gap-2 px-6 py-12 text-center">
      {icon && <div className="mb-1 text-ink-3">{icon}</div>}
      <p className="text-sm font-semibold text-ink-2">{title}</p>
      {hint && <p className="max-w-sm text-[13px] text-ink-3">{hint}</p>}
    </div>
  );
}

/** Small reveal used for grid items; plays once, settles. */
export function FadeIn({
  children,
  delay = 0,
  className = "",
}: {
  children: React.ReactNode;
  delay?: number;
  className?: string;
}) {
  return (
    <motion.div
      className={className}
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.32, delay, ease: [0.16, 1, 0.3, 1] }}
    >
      {children}
    </motion.div>
  );
}

export function Avatar({ name, size = 36 }: { name: string; size?: number }) {
  // deterministic, no external image, clean monochrome initials chip
  return (
    <span
      className="inline-flex shrink-0 items-center justify-center rounded-full bg-accent/12 font-bold text-accent-deep"
      style={{ width: size, height: size, fontSize: size * 0.38 }}
      aria-hidden="true"
    >
      {name
        .trim()
        .split(/\s+/)
        .map((p) => p[0]?.toUpperCase() ?? "")
        .slice(0, 2)
        .join("")}
    </span>
  );
}
