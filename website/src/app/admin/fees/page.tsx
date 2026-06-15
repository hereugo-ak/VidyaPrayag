"use client";

import { useMemo } from "react";
import { useFeeLedger } from "@/lib/admin/hooks";
import type { FeeRow } from "@/lib/admin/types";
import { compactMoney, money } from "@/lib/admin/format";
import { Card, CardHeader, EmptyState, FadeIn, Skeleton, Badge } from "@/components/admin/Primitives";
import { StatTile } from "@/components/admin/StatTile";
import { DonutChart } from "@/components/admin/charts/DonutChart";
import { DataTable, type Column } from "@/components/admin/DataTable";
import { IconFees } from "@/components/admin/icons";

function statusTone(status: string): "success" | "warning" | "danger" | "neutral" {
  const s = status.toLowerCase();
  if (s.includes("paid")) return "success";
  if (s.includes("overdue")) return "danger";
  if (s.includes("due") || s.includes("pending")) return "warning";
  return "neutral";
}

export default function FeesPage() {
  const { data, isLoading } = useFeeLedger();
  const currency = data?.currency ?? "INR";

  const total = data ? data.paidTotal + data.dueTotal + data.overdueTotal : 0;
  const collectedPct = data && total > 0 ? Math.round((data.paidTotal / total) * 100) : 0;

  const slices = useMemo(
    () =>
      data
        ? [
            { label: "Paid", value: data.paidTotal, color: "#3CB9A9" },
            { label: "Due", value: data.dueTotal, color: "#6C5CE0" },
            { label: "Overdue", value: data.overdueTotal, color: "#B3261E" },
          ]
        : [],
    [data]
  );

  const columns: Column<FeeRow>[] = [
    {
      key: "title",
      header: "Item",
      accessor: (r) => r.title,
      sortable: true,
      cell: (r) => (
        <div>
          <p className="font-semibold text-navy-deep">{r.title}</p>
          {r.category && <p className="text-[12px] text-ink-3">{r.category}</p>}
        </div>
      ),
    },
    {
      key: "amount",
      header: "Amount",
      accessor: (r) => r.amount,
      sortable: true,
      align: "right",
      cell: (r) => <span className="nums font-semibold text-navy-deep">{money(r.amount, r.currency || currency)}</span>,
    },
    { key: "due", header: "Due date", accessor: (r) => r.dueDate ?? "", sortable: true, cell: (r) => r.dueDate ?? "—" },
    {
      key: "status",
      header: "Status",
      accessor: (r) => r.status,
      align: "right",
      cell: (r) => <Badge tone={statusTone(r.status)}>{r.status}</Badge>,
    },
  ];

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        <FadeIn>
          <StatTile label="Collected" value={`${collectedPct}%`} caption={`${data?.paidCount ?? 0} paid`} loading={isLoading && !data} accent />
        </FadeIn>
        <FadeIn delay={0.04}>
          <StatTile label="Paid" value={compactMoney(data?.paidTotal ?? 0, currency)} loading={isLoading && !data} />
        </FadeIn>
        <FadeIn delay={0.08}>
          <StatTile label="Due" value={compactMoney(data?.dueTotal ?? 0, currency)} caption={`${data?.dueCount ?? 0} records`} loading={isLoading && !data} />
        </FadeIn>
        <FadeIn delay={0.12}>
          <StatTile label="Overdue" value={compactMoney(data?.overdueTotal ?? 0, currency)} caption={`${data?.overdueCount ?? 0} records`} deltaTone="down" loading={isLoading && !data} />
        </FadeIn>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <FadeIn delay={0.06}>
          <Card className="flex h-full flex-col pb-5">
            <CardHeader title="Collection" subtitle="Paid vs outstanding" />
            <div className="flex flex-1 flex-col items-center justify-center gap-4 px-5 pt-2">
              {isLoading && !data ? (
                <Skeleton className="h-[200px] w-[200px] rounded-full" />
              ) : data && total > 0 ? (
                <>
                  <DonutChart data={slices} centerLabel="collected" centerValue={`${collectedPct}%`} fmt={(n) => compactMoney(n, currency)} />
                  <div className="grid w-full grid-cols-3 gap-2 text-center">
                    {slices.map((s) => (
                      <div key={s.label}>
                        <span className="mx-auto mb-1 block h-1.5 w-6 rounded-full" style={{ background: s.color }} />
                        <p className="nums text-[13px] font-bold text-navy-deep">{compactMoney(s.value, currency)}</p>
                        <p className="text-[11px] text-ink-3">{s.label}</p>
                      </div>
                    ))}
                  </div>
                </>
              ) : (
                <EmptyState icon={<IconFees width={26} height={26} />} title="No fee records yet" />
              )}
            </div>
          </Card>
        </FadeIn>

        <FadeIn delay={0.1} className="lg:col-span-2">
          <Card className="h-full">
            <CardHeader title="Recent ledger" subtitle="Latest fee items across your school" />
            <div className="mt-2">
              <DataTable
                columns={columns}
                rows={data?.recent ?? []}
                rowKey={(r) => `${r.title}-${r.dueDate}-${r.amount}`}
                loading={isLoading && !data}
                emptyState={<EmptyState title="No fee items yet" />}
              />
            </div>
          </Card>
        </FadeIn>
      </div>
    </div>
  );
}
