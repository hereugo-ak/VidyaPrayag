"use client";

import { motion } from "framer-motion";

/**
 * Surface card — the base container for every admin panel.
 *
 * Premium reference language: BORDERLESS, solid-white, pillowy. The depth comes
 * entirely from a soft, high-blur navy-tinted shadow (`shadow-card`) and large
 * corner radius (28px / `rounded-4xl`), not from a 1px line. This is what makes
 * the panel read as "floating on velvet" rather than "boxed in a grid", the
 * single biggest difference between the old admin look and the reference decks.
 */
export function Card({
  children,
  className = "",
  as = "div",
  hover = false,
}: {
  children: React.ReactNode;
  className?: string;
  as?: "div" | "section";
  hover?: boolean;
}) {
  const Comp = as;
  return (
    <Comp
      className={`rounded-[28px] bg-white shadow-card ring-1 ring-navy/[0.03] ${
        hover ? "transition-shadow duration-300 ease-out-cubic hover:shadow-cardHover" : ""
      } ${className}`}
    >
      {children}
    </Comp>
  );
}

/**
 * A pastel "feature" card — soft gradient wash surface used for the highlight
 * tiles (course cards / hero stat in the references). Pick a wash via `tone`.
 */
const washMap = {
  peach: "bg-wash-peach",
  lavender: "bg-wash-lavender",
  mint: "bg-wash-mint",
  sky: "bg-wash-sky",
} as const;

export function WashCard({
  tone = "lavender",
  className = "",
  children,
}: {
  tone?: keyof typeof washMap;
  className?: string;
  children: React.ReactNode;
}) {
  return (
    <div className={`rounded-4xl ${washMap[tone]} shadow-soft ${className}`}>{children}</div>
  );
}

export function CardHeader({
  title,
  subtitle,
  action,
  className = "",
}: {
  title: string;
  subtitle?: string;
  action?: React.ReactNode;
  className?: string;
}) {
  return (
    <div className={`flex items-start justify-between gap-4 px-6 pt-6 ${className}`}>
      <div className="min-w-0">
        <h2 className="text-[16px] font-bold tracking-tight text-navy-deep">{title}</h2>
        {subtitle && <p className="mt-1 text-[12.5px] leading-snug text-ink-3">{subtitle}</p>}
      </div>
      {action && <div className="shrink-0">{action}</div>}
    </div>
  );
}

type Tone = "neutral" | "success" | "warning" | "danger" | "accent";
const toneMap: Record<Tone, string> = {
  neutral: "bg-navy/[0.05] text-ink-2",
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
      className={`inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-[11px] font-bold tracking-tight ${toneMap[tone]} ${className}`}
    >
      {children}
    </span>
  );
}

/**
 * Fully-rounded pill progress bar — the reference "Student Progress" widget.
 * Track is faint; the fill is a soft gradient. `tone` selects the gradient.
 */
const fillMap = {
  accent: "from-accent to-accent-soft",
  success: "from-success to-teal",
  peach: "from-peach to-[#FFB59B]",
  sky: "from-sky to-[#9FB6F8]",
  mint: "from-mint to-[#7FD8CB]",
} as const;

export function ProgressBar({
  value,
  tone = "accent",
  className = "",
}: {
  value: number; // 0..100
  tone?: keyof typeof fillMap;
  className?: string;
}) {
  const pct = Math.max(0, Math.min(100, value));
  return (
    <div className={`h-2.5 w-full overflow-hidden rounded-full bg-navy/[0.07] ${className}`}>
      <div
        className={`h-full rounded-full bg-gradient-to-r ${fillMap[tone]} transition-[width] duration-700 ease-out-cubic`}
        style={{ width: `${pct}%` }}
      />
    </div>
  );
}

/** Loading skeleton bar with a soft sweeping shimmer (not just a flat pulse). */
export function Skeleton({ className = "" }: { className?: string }) {
  return (
    <div className={`relative overflow-hidden rounded-2xl bg-navy/[0.06] ${className}`}>
      <div className="absolute inset-0 -translate-x-full animate-shimmer bg-gradient-to-r from-transparent via-white/55 to-transparent" />
    </div>
  );
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
    <div className="flex flex-col items-center justify-center gap-2.5 px-6 py-14 text-center">
      {icon && (
        <div className="mb-1 flex h-12 w-12 items-center justify-center rounded-2xl bg-navy/[0.04] text-ink-3">
          {icon}
        </div>
      )}
      <p className="text-[14px] font-semibold text-ink-2">{title}</p>
      {hint && <p className="max-w-sm text-[12.5px] leading-relaxed text-ink-3">{hint}</p>}
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
      initial={{ opacity: 0, y: 14 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.42, delay, ease: [0.16, 1, 0.3, 1] }}
    >
      {children}
    </motion.div>
  );
}

export function Avatar({ name, size = 36 }: { name: string; size?: number }) {
  // deterministic, no external image, soft pastel-tinted initials chip
  const initials = name
    .trim()
    .split(/\s+/)
    .map((p) => p[0]?.toUpperCase() ?? "")
    .slice(0, 2)
    .join("");
  return (
    <span
      className="inline-flex shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-accent/[0.16] to-accent/[0.06] font-bold text-accent-deep ring-1 ring-inset ring-accent/10"
      style={{ width: size, height: size, fontSize: size * 0.38 }}
      aria-hidden="true"
    >
      {initials}
    </span>
  );
}
