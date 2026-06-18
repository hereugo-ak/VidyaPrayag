"use client";

import { useMemo, useState } from "react";
import Link from "next/link";

import type { FeeLedgerDto, FeeRow } from "@/lib/admin/types";
import { Card, CardHeader, EmptyState, Skeleton, Badge } from "./Primitives";
import { BarsChart, type BarDatum } from "./charts/BarsChart";
import { DonutChart, type DonutSlice } from "./charts/DonutChart";
import { SidePanel } from "./SidePanel";
import { compactMoney, money } from "@/lib/admin/format";
import { IconFees, IconChevronRight } from "./icons";

/**
 * FinanceTab — the reference-grade fee command center (premium screenshot
 * language, website palette). Composition:
 *   • headline tiles (collected / outstanding / overdue) already render above
 *     this in the hero KPI row — here we go deeper.
 *   • a collection DONUT (paid / due / overdue split) with a centred rate KPI
 *     and a legend, beside a RECENT FEES table (right) — the "donut + table"
 *     reference layout instead of one tall card.
 *   • a "collected by month" bar strip + an upcoming-dues callout below.
 * Every number is from the real /fees/ledger aggregation; empty-first.
 */
function monthKey(d: string | null): string | null {
  if (!d) return null;
  const dt = new Date(d);
  if (isNaN(dt.getTime())) return null;
  return dt.toLocaleDateString("en-IN", { month: "short" });
}

const STATUS_TONE: Record<string, { dot: string; text: string; wash: string; label: string }> = {
  paid: { dot: "bg-teal", text: "text-teal-deep", wash: "bg-wash-mint", label: "Paid" },
  due: { dot: "bg-accent", text: "text-accent-deep", wash: "bg-wash-lavender", label: "Due" },
  overdue: { dot: "bg-danger", text: "text-danger", wash: "bg-[#FCEAE9]", label: "Overdue" },
};

export function FinanceTab({
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

  const donut: DonutSlice[] = data
    ? [
        { label: "Collected", value: Math.round(data.paid_total), color: "#3CB9A9" },
        { label: "Due", value: Math.round(data.due_total), color: "#6C5CE0" },
        { label: "Overdue", value: Math.round(data.overdue_total), color: "#B3261E" },
      ]
    : [];

  const counts: Record<string, number> = {
    Collected: data?.paid_count ?? 0,
    Due: data?.due_count ?? 0,
    Overdue: data?.overdue_count ?? 0,
  };

  const upcoming = useMemo(() => {
    if (!data) return [] as FeeRow[];
    const now = Date.now();
    const week = now + 7 * 86_400_000;
    return data.recent.filter((r) => {
      if (r.status?.toLowerCase() === "paid" || !r.due_date) return false;
      const t = new Date(r.due_date).getTime();
      return !isNaN(t) && t >= now && t <= week;
    });
  }, [data]);

  const monthly: BarDatum[] = useMemo(() => {
    if (!data) return [];
    const map = new Map<string, number>();
    data.recent
      .filter((r) => r.status?.toLowerCase() === "paid")
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

  function openDrill(label: string) {
    if (!data) return;
    const want = label.toLowerCase() === "collected" ? "paid" : label.toLowerCase();
    const rows = data.recent.filter((r) => r.status?.toLowerCase() === want);
    setDrill({ label, rows });
  }

  if (loading && !data) {
    return (
      <div className="grid gap-5 xl:grid-cols-[1fr_1.4fr]">
        <Skeleton className="h-[340px]" />
        <Skeleton className="h-[340px]" />
      </div>
    );
  }

  if (!data || billed === 0) {
    return (
      <Card>
        <CardHeader title="Finance" subtitle="Fee collection from real records" />
        <div className="px-5 pb-6 pt-2">
          <EmptyState
            title="No fee records yet"
            hint="Once fees are billed, collection rate, the paid/due/overdue split and upcoming dues appear here."
          />
        </div>
      </Card>
    );
  }

  const recentSorted = [...data.recent]
    .sort((a, b) => (b.due_date ?? "").localeCompare(a.due_date ?? ""))
    .slice(0, 8);

  return (
    <div className="space-y-5">
      {/* Donut + recent fees table — the reference "split" layout. */}
      <div className="grid gap-5 xl:grid-cols-[1fr_1.42fr]">
        {/* Collection breakdown */}
        <Card hover>
          <CardHeader
            title="Collection breakdown"
            subtitle="Paid · due · overdue from real records"
            action={
              <Badge tone={rate >= 75 ? "success" : rate >= 50 ? "warning" : "danger"}>
                {rate}% collected
              </Badge>
            }
          />
          <div className="flex flex-col items-center gap-5 px-5 pb-6 pt-2 sm:flex-row sm:items-center">
            <div className="shrink-0">
              <DonutChart
                data={donut}
                centerLabel="collected"
                centerValue={`${rate}%`}
                size={184}
                fmt={(n) => compactMoney(n, currency)}
              />
            </div>
            <ul className="w-full space-y-2">
              {donut.map((s) => (
                <li key={s.label}>
                  <button
                    type="button"
                    onClick={() => openDrill(s.label)}
                    className="flex w-full items-center justify-between gap-3 rounded-2xl bg-navy/[0.03] px-3.5 py-2.5 text-left ring-1 ring-inset ring-navy/[0.04] transition-colors hover:bg-navy/[0.06]"
                  >
                    <span className="flex items-center gap-2.5">
                      <span className="h-2.5 w-2.5 rounded-full" style={{ background: s.color }} />
                      <span className="text-[13px] font-semibold text-navy-deep">{s.label}</span>
                      <span className="text-[11px] text-ink-3">{counts[s.label]} records</span>
                    </span>
                    <span className="nums text-[13px] font-bold text-navy-deep">
                      {compactMoney(s.value, currency)}
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          </div>
        </Card>

        {/* Recent fees table */}
        <Card hover>
          <CardHeader
            title="Recent fees"
            subtitle="Latest billed items across the school"
            action={
              <Link
                href="/admin/fees"
                className="inline-flex items-center gap-1 text-[12px] font-bold text-accent-deep transition-colors hover:text-accent"
              >
                Open fees <IconChevronRight width={14} height={14} />
              </Link>
            }
          />
          <div className="px-3 pb-4 pt-1">
            <div className="hidden grid-cols-[1fr_auto_auto] gap-3 px-3 pb-2 text-[11px] font-semibold uppercase tracking-wide text-ink-3 sm:grid">
              <span>Item</span>
              <span className="text-right">Amount</span>
              <span className="text-right">Status</span>
            </div>
            <ul className="space-y-1">
              {recentSorted.map((r, i) => {
                const tone = STATUS_TONE[r.status?.toLowerCase()] ?? STATUS_TONE.due;
                return (
                  <li
                    key={i}
                    className="grid grid-cols-[1fr_auto] items-center gap-3 rounded-2xl px-3 py-2.5 transition-colors hover:bg-navy/[0.03] sm:grid-cols-[1fr_auto_92px]"
                  >
                    <div className="min-w-0">
                      <p className="truncate text-[13px] font-semibold text-navy-deep">{r.title}</p>
                      <p className="text-[11.5px] text-ink-3">
                        {r.category ?? "Fee"}
                        {r.due_date ? ` · due ${formatDue(r.due_date)}` : ""}
                      </p>
                    </div>
                    <span className="nums text-right text-[13px] font-bold text-navy-deep">
                      {money(r.amount, r.currency)}
                    </span>
                    <span className="hidden justify-self-end sm:inline-flex">
                      <span
                        className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-[11px] font-bold ${tone.wash} ${tone.text}`}
                      >
                        <span className={`h-1.5 w-1.5 rounded-full ${tone.dot}`} />
                        {tone.label}
                      </span>
                    </span>
                  </li>
                );
              })}
            </ul>
          </div>
        </Card>
      </div>

      {/* Collected by month + upcoming dues. */}
      <div className="grid gap-5 xl:grid-cols-[1.42fr_1fr]">
        <Card hover>
          <CardHeader title="Collected by month" subtitle="From paid fee records" />
          <div className="px-3 pb-5 pt-1">
            {monthly.length > 0 ? (
              <BarsChart
                data={monthly}
                unit=""
                height={210}
                max={Math.max(1, ...monthly.map((m) => m.value))}
              />
            ) : (
              <div className="px-2 py-6">
                <EmptyState title="No paid records dated yet" hint="Monthly collection appears once paid fees carry dates." />
              </div>
            )}
          </div>
        </Card>

        <Card hover>
          <CardHeader title="Upcoming dues" subtitle="Next 7 days" />
          <div className="px-5 pb-5 pt-1">
            {upcoming.length > 0 ? (
              <div className="space-y-3">
                <div className="rounded-2xl bg-wash-peach px-4 py-3.5 shadow-soft">
                  <p className="text-[12px] font-semibold text-[#9A5414]">
                    {upcoming.length} payment{upcoming.length > 1 ? "s" : ""} due this week
                  </p>
                  <p className="nums mt-0.5 text-[18px] font-extrabold text-navy-deep">
                    {money(upcoming.reduce((s, r) => s + r.amount, 0), currency)}
                  </p>
                </div>
                <ul className="space-y-1.5">
                  {upcoming.slice(0, 4).map((r, i) => (
                    <li key={i} className="flex items-center justify-between gap-3 px-1">
                      <div className="min-w-0">
                        <p className="truncate text-[13px] font-semibold text-navy-deep">{r.title}</p>
                        <p className="text-[11.5px] text-ink-3">due {formatDue(r.due_date)}</p>
                      </div>
                      <span className="nums shrink-0 text-[13px] font-bold text-navy-deep">
                        {money(r.amount, r.currency)}
                      </span>
                    </li>
                  ))}
                </ul>
              </div>
            ) : (
              <div className="flex h-full min-h-[180px] flex-col items-center justify-center gap-2 py-4 text-center">
                <span className="grid h-11 w-11 place-items-center rounded-2xl bg-wash-mint text-teal-deep">
                  <IconFees width={20} height={20} />
                </span>
                <p className="text-[13px] font-semibold text-navy-deep">All clear this week</p>
                <p className="text-[12px] text-ink-3">No payments fall due in the next 7 days.</p>
              </div>
            )}
          </div>
        </Card>
      </div>

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
                <li
                  key={i}
                  className="flex items-center justify-between gap-3 rounded-2xl bg-navy/[0.03] px-4 py-3 ring-1 ring-inset ring-navy/[0.05]"
                >
                  <div className="min-w-0">
                    <p className="truncate text-[13px] font-semibold text-navy-deep">{r.title}</p>
                    <p className="text-[12px] text-ink-3">
                      {r.category ?? "Fee"}
                      {r.due_date ? ` · due ${formatDue(r.due_date)}` : ""}
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
    </div>
  );
}

function formatDue(d: string | null): string {
  if (!d) return "";
  const dt = new Date(d);
  if (isNaN(dt.getTime())) return d;
  return dt.toLocaleDateString("en-IN", { day: "numeric", month: "short" });
}
