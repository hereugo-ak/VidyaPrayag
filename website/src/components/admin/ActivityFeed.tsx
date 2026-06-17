"use client";

import { useMemo } from "react";
import type { ActivityItem } from "@/lib/admin/types";
import { Card, CardHeader, EmptyState, Skeleton } from "./Primitives";

/**
 * Activity feed, real institutional events merged server-side from
 * notifications + leave_requests + announcements (real DB timestamps). Grouped
 * by Today / Yesterday / This week / Earlier. Each item shows actor, action,
 * target and a relative time. NEAR-LIVE via the intelligence poll (60s).
 */
const CATEGORY_DOT: Record<string, string> = {
  attendance: "#6C5CE0",
  marks: "#006A60",
  homework: "#B3651A",
  announcement: "#544AB8",
  leave: "#B3261E",
  fees: "#1F7A4D",
  link: "#3CB9A9",
  general: "#6D7A77",
};

function startOfDay(d: Date) {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();
}

function bucketOf(iso: string): string {
  const t = new Date(iso).getTime();
  if (isNaN(t)) return "Earlier";
  const today = startOfDay(new Date());
  const day = 86_400_000;
  if (t >= today) return "Today";
  if (t >= today - day) return "Yesterday";
  if (t >= today - 7 * day) return "This week";
  return "Earlier";
}

function relTime(iso: string): string {
  const t = new Date(iso).getTime();
  if (isNaN(t)) return "";
  const diff = Date.now() - t;
  const min = Math.floor(diff / 60_000);
  if (min < 1) return "just now";
  if (min < 60) return `${min}m ago`;
  const hr = Math.floor(min / 60);
  if (hr < 24) return `${hr}h ago`;
  const d = Math.floor(hr / 24);
  if (d < 7) return `${d}d ago`;
  return new Date(t).toLocaleDateString("en-IN", { day: "numeric", month: "short" });
}

const ORDER = ["Today", "Yesterday", "This week", "Earlier"];

export function ActivityFeed({
  data,
  loading,
}: {
  data: ActivityItem[] | undefined;
  loading: boolean;
}) {
  const grouped = useMemo(() => {
    const map = new Map<string, ActivityItem[]>();
    (data ?? []).forEach((item) => {
      const b = bucketOf(item.iso_time);
      if (!map.has(b)) map.set(b, []);
      map.get(b)!.push(item);
    });
    return ORDER.filter((k) => map.has(k)).map((k) => [k, map.get(k)!] as const);
  }, [data]);

  return (
    <Card className="flex h-full flex-col" hover>
      <CardHeader title="Activity" subtitle="Live across your school" />
      <div className="flex-1 overflow-y-auto px-2 py-2">
        {loading && !data ? (
          <div className="space-y-3 p-3">
            {Array.from({ length: 6 }).map((_, i) => (
              <Skeleton key={i} className="h-12" />
            ))}
          </div>
        ) : (data?.length ?? 0) === 0 ? (
          <EmptyState title="No recent activity" hint="New events show up here as they happen." />
        ) : (
          <div className="space-y-1">
            {grouped.map(([bucket, items]) => (
              <div key={bucket}>
                <p className="sticky top-0 z-[1] bg-white/95 px-3 py-1.5 text-[10px] font-bold uppercase tracking-[0.12em] text-ink-3 backdrop-blur-sm">
                  {bucket}
                </p>
                <ul className="space-y-0.5">
                  {items.map((item) => (
                    <li key={item.id} className="flex gap-3 rounded-2xl px-3 py-2.5 transition-colors hover:bg-navy/[0.03]">
                      <span
                        className="mt-1.5 h-2 w-2 shrink-0 rounded-full"
                        style={{ background: CATEGORY_DOT[item.category] ?? CATEGORY_DOT.general }}
                        aria-hidden
                      />
                      <div className="min-w-0 flex-1">
                        <p className="text-[13px] leading-snug text-navy-deep">
                          <span className="font-semibold">{item.actor}</span>{" "}
                          <span className="text-ink-2">{item.action}</span>
                        </p>
                        {item.target && (
                          <p className="truncate text-[12px] text-ink-3">{item.target}</p>
                        )}
                        <p className="mt-0.5 text-[11px] text-ink-placeholder">{relTime(item.iso_time)}</p>
                      </div>
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
        )}
      </div>
    </Card>
  );
}
