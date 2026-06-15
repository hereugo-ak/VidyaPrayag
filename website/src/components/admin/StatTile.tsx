"use client";

import { IconArrowDown, IconArrowUp } from "./icons";
import { Skeleton } from "./Primitives";

/**
 * Live metric tile — big number, optional signed delta, caption. The bar of
 * these across the top of the dashboard mirrors the reference image's metric
 * row, with clearer hierarchy and real values.
 */
export function StatTile({
  label,
  value,
  delta,
  deltaTone,
  caption,
  loading,
  accent,
}: {
  label: string;
  value: string;
  delta?: string;
  deltaTone?: "up" | "down" | "flat";
  caption?: string;
  loading?: boolean;
  accent?: boolean;
}) {
  return (
    <div
      className={`flex flex-col justify-between rounded-2xl border p-5 transition-shadow duration-200 hover:shadow-card ${
        accent ? "border-accent/20 bg-accent/[0.04]" : "border-navy/8 bg-white/85"
      }`}
    >
      <div className="flex items-center justify-between gap-2">
        <p className="text-[12px] font-semibold uppercase tracking-wide text-ink-3">{label}</p>
        {delta && !loading && (
          <span
            className={`inline-flex items-center gap-0.5 rounded-full px-2 py-0.5 text-[11px] font-bold ${
              deltaTone === "down"
                ? "bg-danger/10 text-danger"
                : deltaTone === "up"
                ? "bg-success/10 text-success"
                : "bg-navy/6 text-ink-3"
            }`}
          >
            {deltaTone === "down" ? (
              <IconArrowDown width={11} height={11} />
            ) : deltaTone === "up" ? (
              <IconArrowUp width={11} height={11} />
            ) : null}
            {delta}
          </span>
        )}
      </div>
      {loading ? (
        <Skeleton className="mt-3 h-9 w-24" />
      ) : (
        <p className="nums mt-2 text-[30px] font-extrabold leading-none tracking-tight text-navy-deep">
          {value}
        </p>
      )}
      {caption && !loading && <p className="mt-2 text-[12px] text-ink-3">{caption}</p>}
    </div>
  );
}
