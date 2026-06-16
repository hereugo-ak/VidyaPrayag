"use client";

import { useMemo } from "react";
import { useAttendanceSummary } from "@/lib/admin/hooks";
import type { AttendanceClassRow } from "@/lib/admin/types";
import { pct } from "@/lib/admin/format";
import { Card, CardHeader, EmptyState, FadeIn, Skeleton, Badge } from "@/components/admin/Primitives";
import { StatTile } from "@/components/admin/StatTile";
import { BarsChart, type BarDatum } from "@/components/admin/charts/BarsChart";
import { DataTable, type Column } from "@/components/admin/DataTable";
import { IconAttendance } from "@/components/admin/icons";

export default function AttendancePage() {
  const { data, isLoading } = useAttendanceSummary();

  const byClass: BarDatum[] = useMemo(
    () =>
      (data?.by_class ?? []).map((r) => ({
        label: r.grade,
        value: r.rate,
        meta: `${r.present}/${r.total} present`,
      })),
    [data]
  );

  const columns: Column<AttendanceClassRow>[] = [
    { key: "grade", header: "Class", accessor: (r) => r.grade, sortable: true },
    { key: "present", header: "Present", accessor: (r) => r.present, sortable: true, align: "right" },
    { key: "absent", header: "Absent", accessor: (r) => r.absent, sortable: true, align: "right" },
    { key: "late", header: "Late", accessor: (r) => r.late, sortable: true, align: "right" },
    { key: "total", header: "Total", accessor: (r) => r.total, sortable: true, align: "right" },
    {
      key: "rate",
      header: "Rate",
      accessor: (r) => r.rate,
      sortable: true,
      align: "right",
      cell: (r) => (
        <Badge tone={r.rate >= 90 ? "success" : r.rate >= 75 ? "warning" : "danger"}>
          {pct(r.rate)}
        </Badge>
      ),
    },
  ];

  const hasData = (data?.total ?? 0) > 0;

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        <FadeIn>
          <StatTile
            label="Present today"
            value={pct(data?.rate ?? 0)}
            caption={data?.latest_date ? `As of ${data.latest_date}` : "No record yet"}
            loading={isLoading && !data}
            accent
          />
        </FadeIn>
        <FadeIn delay={0.04}>
          <StatTile label="Present" value={(data?.present ?? 0).toLocaleString("en-IN")} loading={isLoading && !data} />
        </FadeIn>
        <FadeIn delay={0.08}>
          <StatTile label="Absent" value={(data?.absent ?? 0).toLocaleString("en-IN")} loading={isLoading && !data} />
        </FadeIn>
        <FadeIn delay={0.12}>
          <StatTile label="Late" value={(data?.late ?? 0).toLocaleString("en-IN")} loading={isLoading && !data} />
        </FadeIn>
      </div>

      <FadeIn delay={0.06}>
        <Card className="pb-5">
          <CardHeader title="Present-rate by class" subtitle="Today — hover a bar for the exact split" />
          <div className="px-2 pt-2">
            {isLoading && !data ? (
              <Skeleton className="mx-3 h-[240px]" />
            ) : byClass.length ? (
              <BarsChart data={byClass} unit="%" />
            ) : (
              <EmptyState
                icon={<IconAttendance width={26} height={26} />}
                title="No attendance recorded today"
                hint="Once teachers mark attendance, the live breakdown appears here."
              />
            )}
          </div>
        </Card>
      </FadeIn>

      <FadeIn delay={0.1}>
        <Card>
          <CardHeader title="Class register" subtitle="Sortable — click a column header" />
          <div className="mt-2">
            <DataTable
              columns={columns}
              rows={data?.by_class ?? []}
              rowKey={(r) => r.grade}
              loading={isLoading && !data}
              initialSort={{ key: "rate", dir: "desc" }}
              emptyState={<EmptyState title={hasData ? "No per-class data" : "No attendance yet"} />}
            />
          </div>
        </Card>
      </FadeIn>
    </div>
  );
}
