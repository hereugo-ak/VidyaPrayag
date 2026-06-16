"use client";

import { useMarksSummary } from "@/lib/admin/hooks";
import type { MarksAssessmentRow } from "@/lib/admin/types";
import { pct } from "@/lib/admin/format";
import { Card, CardHeader, EmptyState, FadeIn, Badge } from "@/components/admin/Primitives";
import { StatTile } from "@/components/admin/StatTile";
import { DataTable, type Column } from "@/components/admin/DataTable";
import { IconMarks } from "@/components/admin/icons";

export default function MarksPage() {
  const { data, isLoading } = useMarksSummary();

  const columns: Column<MarksAssessmentRow>[] = [
    { key: "subject", header: "Subject", accessor: (r) => r.subject, sortable: true },
    {
      key: "assessment",
      header: "Assessment",
      accessor: (r) => r.assessment,
      sortable: true,
      cell: (r) => (
        <div>
          <p className="font-semibold text-navy-deep">{r.assessment}</p>
          <p className="text-[12px] text-ink-3">{r.class_name}</p>
        </div>
      ),
    },
    {
      key: "average",
      header: "Average",
      accessor: (r) => (r.max_marks > 0 ? (r.average / r.max_marks) * 100 : 0),
      sortable: true,
      align: "right",
      cell: (r) => {
        const p = r.max_marks > 0 ? Math.round((r.average / r.max_marks) * 100) : 0;
        return (
          <span className="nums">
            <Badge tone={p >= 75 ? "success" : p >= 40 ? "warning" : "danger"}>{pct(p)}</Badge>
            <span className="ml-2 text-ink-3">{r.average.toFixed(1)}/{r.max_marks}</span>
          </span>
        );
      },
    },
    { key: "graded", header: "Graded", accessor: (r) => r.graded_count, sortable: true, align: "right" },
    {
      key: "status",
      header: "Status",
      accessor: (r) => (r.is_published ? "Published" : "Draft"),
      align: "right",
      cell: (r) => (
        <Badge tone={r.is_published ? "success" : "neutral"}>
          {r.is_published ? "Published" : "Draft"}
        </Badge>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-3">
        <FadeIn>
          <StatTile
            label="Assessments"
            value={(data?.assessment_count ?? 0).toLocaleString("en-IN")}
            caption="Active this term"
            loading={isLoading && !data}
            accent
          />
        </FadeIn>
        <FadeIn delay={0.04}>
          <StatTile
            label="Overall average"
            value={pct(data?.overall_average_pct ?? 0)}
            caption="Across published assessments"
            loading={isLoading && !data}
          />
        </FadeIn>
        <FadeIn delay={0.08}>
          <StatTile
            label="Published"
            value={(data?.assessments.filter((a) => a.is_published).length ?? 0).toLocaleString("en-IN")}
            caption="Visible to parents"
            loading={isLoading && !data}
          />
        </FadeIn>
      </div>

      <FadeIn delay={0.06}>
        <Card>
          <CardHeader title="Assessments" subtitle="Subject averages and publish status — sortable" />
          <div className="mt-2">
            <DataTable
              columns={columns}
              rows={data?.assessments ?? []}
              rowKey={(r) => `${r.subject}-${r.assessment}-${r.class_name}`}
              loading={isLoading && !data}
              initialSort={{ key: "average", dir: "desc" }}
              emptyState={
                <EmptyState
                  icon={<IconMarks width={26} height={26} />}
                  title="No assessments yet"
                  hint="When teachers grade assessments, subject averages appear here."
                />
              }
            />
          </div>
        </Card>
      </FadeIn>
    </div>
  );
}
