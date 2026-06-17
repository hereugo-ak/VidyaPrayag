"use client";

/**
 * Hero stat tiles — the dashboard's headline KPI row, modelled on the premium
 * reference decks (the Balance / Spending / Investing tiles): a soft pastel icon
 * chip, an oversized tabular number, a signed delta pill, and a quiet sparkline
 * that traces the metric's recent trajectory.
 *
 * Every value is REAL (see source per tile). The sparkline uses the 30-day
 * attendance timeline (intelligence) for the attendance tiles; tiles with no
 * trend series omit the sparkline rather than fake one. Tiles collapse to a
 * 2-up grid on mobile and a 4-up row on desktop — no horizontal scroll.
 *
 * SOURCE per tile is annotated inline where the parent assembles `tiles`.
 */

import { IconArrowUp, IconArrowDown } from "./icons";

export type HeroTone = "accent" | "success" | "teal" | "peach" | "sky" | "warning";

export interface HeroTile {
  key: string;
  label: string;
  value: string;
  /** Optional secondary line under the value (e.g. "of 480 enrolled"). */
  sub?: string;
  /** Signed delta, already formatted (e.g. "+4.2%"). Omit when none is real. */
  delta?: string;
  deltaDir?: "up" | "down" | "flat";
  /** "up is good" (attendance) vs "up is bad" (overdue) — colours the delta. */
  goodWhenUp?: boolean;
  tone: HeroTone;
  icon: React.ReactNode;
  /** Sparkline series (0..n). Omitted → no sparkline drawn. */
  spark?: number[];
  /**
   * 0..1 quiet progress meter shown when a tile has no sparkline series, so the
   * KPI row reads balanced (no half-empty white tiles). Real ratio only.
   */
  meter?: number;
  /** Optional href makes the whole tile a link to the deep surface. */
  href?: string;
}

const toneMap: Record<
  HeroTone,
  { chip: string; spark: string; sparkFill: string; meter: string; meterTrack: string }
> = {
  accent: { chip: "bg-accent/12 text-accent-deep", spark: "#6C5CE0", sparkFill: "rgba(108,92,224,0.18)", meter: "bg-accent", meterTrack: "bg-accent/12" },
  success: { chip: "bg-success/10 text-success", spark: "#1F7A4D", sparkFill: "rgba(31,122,77,0.16)", meter: "bg-success", meterTrack: "bg-success/10" },
  teal: { chip: "bg-teal/12 text-teal-deep", spark: "#3CB9A9", sparkFill: "rgba(60,185,169,0.18)", meter: "bg-teal", meterTrack: "bg-teal/12" },
  peach: { chip: "bg-peach-soft text-[#9A5414]", spark: "#FF8A65", sparkFill: "rgba(255,138,101,0.18)", meter: "bg-peach", meterTrack: "bg-peach/15" },
  sky: { chip: "bg-sky-soft text-sky", spark: "#6C8DF5", sparkFill: "rgba(108,141,245,0.18)", meter: "bg-sky", meterTrack: "bg-sky/15" },
  warning: { chip: "bg-warning/12 text-warning", spark: "#B3651A", sparkFill: "rgba(179,101,26,0.16)", meter: "bg-warning", meterTrack: "bg-warning/12" },
};

export function HeroStatTiles({ tiles }: { tiles: HeroTile[] }) {
  if (!tiles.length) return null;
  return (
    <div className="grid grid-cols-2 gap-3.5 lg:grid-cols-4 lg:gap-4">
      {tiles.map((t) => (
        <Tile key={t.key} tile={t} />
      ))}
    </div>
  );
}

function Tile({ tile }: { tile: HeroTile }) {
  const t = toneMap[tile.tone];
  const deltaGood =
    tile.deltaDir === "flat"
      ? "neutral"
      : (tile.deltaDir === "up") === (tile.goodWhenUp ?? true)
        ? "good"
        : "bad";
  const deltaClass =
    deltaGood === "good"
      ? "bg-success/10 text-success"
      : deltaGood === "bad"
        ? "bg-danger/10 text-danger"
        : "bg-navy/[0.05] text-ink-2";

  const hasSpark = !!tile.spark && tile.spark.length > 1;

  const inner = (
    <div className="group relative flex h-full flex-col gap-3 overflow-hidden rounded-4xl bg-white p-5 shadow-card ring-1 ring-navy/[0.04] transition-all duration-300 ease-out-cubic hover:-translate-y-1 hover:shadow-cardHover">
      {/* hairline top sheen — the quiet 'expensive glass' tell */}
      <div
        className="pointer-events-none absolute inset-x-5 top-0 h-px bg-gradient-to-r from-transparent via-navy/[0.06] to-transparent"
        aria-hidden
      />
      <div className="flex items-start justify-between gap-2">
        <span className={`inline-flex h-9 w-9 items-center justify-center rounded-2xl ${t.chip}`}>
          {tile.icon}
        </span>
        {tile.delta && (
          <span
            className={`inline-flex items-center gap-0.5 rounded-full px-2 py-1 text-[11px] font-bold ${deltaClass}`}
          >
            {tile.deltaDir === "up" ? (
              <IconArrowUp width={11} height={11} />
            ) : tile.deltaDir === "down" ? (
              <IconArrowDown width={11} height={11} />
            ) : null}
            {tile.delta}
          </span>
        )}
      </div>

      <div className="min-w-0 flex-1">
        <p className="nums text-[30px] font-extrabold leading-none tracking-tighter text-navy-deep">
          {tile.value}
        </p>
        <p className="mt-2 truncate text-[12.5px] font-semibold text-ink-2">{tile.label}</p>
        {tile.sub && <p className="truncate text-[11.5px] text-ink-3">{tile.sub}</p>}
      </div>

      {/* Baseline visual — sparkline when we have a series, else a quiet meter,
          else a hairline. Every tile keeps the SAME footprint so the row reads
          balanced rather than half-empty (image-audit finding). */}
      <div className="h-[30px] shrink-0">
        {hasSpark ? (
          <Sparkline values={tile.spark!} stroke={t.spark} fill={t.sparkFill} idKey={tile.key} />
        ) : typeof tile.meter === "number" ? (
          <Meter value={tile.meter} track={t.meterTrack} bar={t.meter} />
        ) : (
          <div className="mt-[14px] h-px w-full bg-gradient-to-r from-navy/[0.06] via-navy/[0.04] to-transparent" />
        )}
      </div>
    </div>
  );

  if (tile.href) {
    return (
      <a href={tile.href} className="block rounded-4xl focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent/40">
        {inner}
      </a>
    );
  }
  return inner;
}

/** Quiet horizontal meter for tiles without a trend series (real ratio). */
function Meter({ value, track, bar }: { value: number; track: string; bar: string }) {
  const pct = Math.max(0, Math.min(1, value)) * 100;
  return (
    <div className="flex h-full flex-col justify-end">
      <div className={`h-2 w-full overflow-hidden rounded-full ${track}`}>
        <div
          className={`h-full rounded-full ${bar} transition-[width] duration-700 ease-out-cubic`}
          style={{ width: `${pct}%` }}
        />
      </div>
    </div>
  );
}

/**
 * Tiny inline area sparkline (no library; crisp at the small size). Uses a
 * per-tile gradient fill (rich → transparent) and a slight inset top/bottom so
 * the curve never touches — and so never visually clips against — the card edge.
 */
function Sparkline({
  values,
  stroke,
  fill,
  idKey,
}: {
  values: number[];
  stroke: string;
  fill: string;
  idKey: string;
}) {
  const w = 120;
  const h = 30;
  const padY = 3;
  const min = Math.min(...values);
  const max = Math.max(...values);
  const range = max - min || 1;
  const step = w / (values.length - 1);
  const pts = values.map((v, i) => {
    const x = i * step;
    const y = h - padY - ((v - min) / range) * (h - padY * 2);
    return [x, y] as const;
  });
  const line = pts.map(([x, y], i) => `${i === 0 ? "M" : "L"}${x.toFixed(1)},${y.toFixed(1)}`).join(" ");
  // Close the area straight down from the LAST point so the fill never leaves a
  // flat gap at the right edge (image-audit "abruptly drops off" finding).
  const last = pts[pts.length - 1];
  const area = `${line} L${last[0].toFixed(1)},${h} L0,${h} Z`;
  const gid = `spark-${idKey}`;
  return (
    <svg viewBox={`0 0 ${w} ${h}`} className="h-[30px] w-full" preserveAspectRatio="none" aria-hidden>
      <defs>
        <linearGradient id={gid} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor={fill} />
          <stop offset="100%" stopColor={fill} stopOpacity={0} />
        </linearGradient>
      </defs>
      <path d={area} fill={`url(#${gid})`} />
      <path
        d={line}
        fill="none"
        stroke={stroke}
        strokeWidth={2}
        strokeLinecap="round"
        strokeLinejoin="round"
        vectorEffect="non-scaling-stroke"
      />
    </svg>
  );
}
