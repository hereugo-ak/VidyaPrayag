"use client";

import { useMemo, useState } from "react";
import type { FeeLedgerDto, FeeRow } from "@/lib/admin/types";
import { Card, CardHeader, EmptyState, Skeleton, Badge } from "./Primitives";
import { BarsChart, type BarDatum } from "./charts/BarsChart";
import { SidePanel } from "./SidePanel";
import { compactMoney, money } from "@/lib/admin/format";

/**
 * Financial pulse — real fee_records aggregation (from /fees/ledger):
 *   • collection rate (paid / billed) with paid vs outstanding split
 *   • overdue breakdown (amount + count)
 *   • upcoming due dates in the next 7 days (from recent rows' due_date)
 *   • monthly collection bars derived from recent paid rows' due_date month
 * Clicking a category bar opens a filtered fee table for that bucket. Every
 * number is from Supabase; empty when there are no fee records.
 */
function monthKey(d: string | null): string | null {
  if (!d) return null;
  const dt = new Date(d);
  if (isNaN(dt.getTime())) return null;
  return dt.toLocaleDateString("en-IN", { month: "short" });
}

export function FinancialPulse({
  data,
  loading,
}: {
  data: FeeLedgerDto | undefined;
  loading: boolean;
}) {
  const [drill, setDrill] = useState<{ label: string; rows: FeeRow[] } | null>(null);

  const currency = data?.currency ?? "INR";
  const billed = data ? data.paid_total + data.due_total + data.overdue_total : 0;
  const rate = data && billed > 0 ? Math.round((data.paid_total / billed) * 100) : 0;

  const upcoming = useMemo(() => {
    if (!data) return [] as FeeRow[];
    const now = Date.now();
    const week = now + 7 * 86_400_000;
    return data.recent.filter((r) => {
      if (r.status?.toUpperCase() === "PAID" || !r.due_date) return false;
      const t = new Date(r.due_date).getTime();
      return !isNaN(t) && t >= now && t <= week;
    });
  }, [data]);

  const monthly: BarDatum[] = useMemo(() => {
    if (!data) return [];
    const map = new Map<string, number>();
    data.recent
      .filter((r) => r.status?.toUpperCase() === "PAID")
      .forEach((r) => {
        const k = monthKey(r.due_date);
        if (!k) return;
        map.set(k, (map.get(k) ?? 0) + r.amount);
      });
    return Array.from(map.entries()).map(([label, value]) => ({
      label,
      value: Math.round(value),
      meta: compactMoney(value, currency),
    }));
  }, [data, currency]);

  const categoryBars: BarDatum[] = data
    ? [
        { label: "Paid", value: Math.round(data.paid_total), meta: `${data.paid_count} records` },
        { label: "Due", value: Math.round(data.due_total), meta: `${data.due_count} records` },
        { label: "Overdue", value: Math.round(data.overdue_total), meta: `${data.overdue_count} records` },
      ]
    : [];
  const maxCat = Math.max(1, ...categoryBars.map((b) => b.value));

  function openDrill(label: string) {
    if (!data) return;
    const want = label.toUpperCase();
    const rows = data.recent.filter((r) => r.status?.toUpperCase() === want);
    setDrill({ label, rows });
  }

  return (
    <>
      <Card className="h-full pb-5">
        <CardHeader
          title="Financial pulse"
          subtitle="Fee collection from real records — click a bar to drill in"
          action={
            data ? (
              <Badge tone={rate >= 75 ? "success" : rate >= 50 ? "warning" : "danger"}>
                {rate}% collected
              </Badge>
            ) : null
          }
        />
        <div className="px-5 pt-3">
          {loading && !data ? (
            <Skeleton className="h-[240px]" />
          ) : !data || billed === 0 ? (
            <EmptyState
              title="No fee records yet"
              hint="Once fees are billed, collection rate, overdue breakdown and upcoming dues appear here."
            />
          ) : (
            <div className="space-y-5">
              <div className="grid grid-cols-3 gap-3">
                <Tile label="Collected" value={compactMoney(data.paid_total, currency)} tone="success" />
                <Tile label="Outstanding" value={compactMoney(data.due_total + data.overdue_total, currency)} tone="warning" />
                <Tile label="Overdue" value={`${data.overdue_count}`} sub={compactMoney(data.overdue_total, currency)} tone="danger" />
              </div>

              <div>
                <p className="mb-1 px-1 text-[12px] font-semibold text-ink-3">Billed by status</p>
                <BarsChart
                  data={categoryBars}
                  unit=""
                  height={170}
                  max={maxCat}
                  onSelect={(d) => openDrill(d.label)}
                />
              </div>

              {monthly.length > 0 && (
                <div>
                  <p className="mb-1 px-1 text-[12px] font-semibold text-ink-3">Collected by month</p>
                  <BarsChart data={monthly} unit="" height={150} max={Math.max(1, ...monthly.map((m) => m.value))} />
                </div>
              )}

              {upcoming.length > 0 && (
                <div className="rounded-xl border border-warning/25 bg-warning/[0.06] px-4 py-3">
                  <p className="text-[12px] font-semibold text-warning">
                    {upcoming.length} payment{upcoming.length > 1 ? "s" : ""} due in the next 7 days
                  </p>
                  <p className="mt-0.5 nums text-[13px] font-bold text-navy-deep">
                    {money(upcoming.reduce((s, r) => s + r.amount, 0), currency)}
                  </p>
                </div>
              )}
            </div>
          )}
        </div>
      </Card>

      <SidePanel
        open={!!drill}
        onClose={() => setDrill(null)}
        title={drill ? `${drill.label} fees` : ""}
        subtitle={drill ? `${drill.rows.length} record${drill.rows.length === 1 ? "" : "s"}` : undefined}
      >
        {drill && (
          <ul className="space-y-2">
            {drill.rows.length === 0 ? (
              <p className="text-[13px] text-ink-3">No {drill.label.toLowerCase()} records.</p>
            ) : (
              drill.rows.map((r, i) => (
                <li key={i} className="flex items-center justify-between gap-3 rounded-xl border border-navy/8 bg-white/70 px-4 py-3">
                  <div className="min-w-0">
                    <p className="truncate text-[13px] font-semibold text-navy-deep">{r.title}</p>
                    <p className="text-[12px] text-ink-3">
                      {r.category ?? "Fee"}
                      {r.due_date ? ` · due ${r.due_date}` : ""}
                    </p>
                  </div>
                  <span className="nums shrink-0 text-[13px] font-bold text-navy-deep">
                    {money(r.amount, r.currency)}
                  </span>
                </li>
              ))
            )}
          </ul>
        )}
      </SidePanel>
    </>
  );
}

function Tile({
  label,
  value,
  sub,
  tone,
}: {
  label: string;
  value: string;
  sub?: string;
  tone: "success" | "warning" | "danger";
}) {
  const map = { success: "text-success", warning: "text-warning", danger: "text-danger" } as const;
  return (
    <div className="rounded-xl border border-navy/8 bg-white/70 px-3 py-3">
      <p className={`nums text-[16px] font-bold ${map[tone]}`}>{value}</p>
      {sub && <p className="nums text-[11px] text-ink-3">{sub}</p>}
      <p className="mt-0.5 text-[11px] text-ink-3">{label}</p>
    </div>
  );
}
