"use client";

import { useState, useMemo } from "react";
import {
  useReportCardOversight,
  useReportCardEffectiveness,
  useReportCardTermConfig,
} from "@/lib/admin/hooks";
import { adminApi } from "@/lib/admin/client";
import type { ReportCardOversightRow } from "@/lib/admin/types";
import {
  Avatar,
  Badge,
  Card,
  CardHeader,
  EmptyState,
  FadeIn,
  Skeleton,
} from "@/components/admin/Primitives";
import { StatTile } from "@/components/admin/StatTile";
import { DataTable, type Column } from "@/components/admin/DataTable";
import { IconReport, IconSparkle, IconCheck, IconWarning } from "@/components/admin/icons";

const STATUS_TONE: Record<string, "neutral" | "warning" | "success" | "accent"> = {
  draft: "neutral",
  flagged_for_review: "warning",
  approved: "success",
  published: "accent",
};
const STATUS_LABEL: Record<string, string> = {
  draft: "Draft",
  flagged_for_review: "Flagged",
  approved: "Approved",
  published: "Published",
};

export function ReportCardWorkspace() {
  const { data: config } = useReportCardTermConfig();
  const [term, setTerm] = useState(config?.currentTerm ?? "Term 1");
  const { data: oversight, isLoading: oversightLoading } = useReportCardOversight(
    term || null,
  );
  const { data: effectiveness, isLoading: effLoading } = useReportCardEffectiveness();
  const [publishing, setPublishing] = useState<string | null>(null);
  const [publishMsg, setPublishMsg] = useState<string | null>(null);

  const classes = oversight?.classes ?? [];

  const totals = useMemo(() => {
    return classes.reduce(
      (acc, c) => ({
        total: acc.total + c.totalDrafts,
        draft: acc.draft + c.draftCount,
        flagged: acc.flagged + c.flaggedCount,
        approved: acc.approved + c.approvedCount,
        published: acc.published + c.publishedCount,
      }),
      { total: 0, draft: 0, flagged: 0, approved: 0, published: 0 },
    );
  }, [classes]);

  const columns: Column<ReportCardOversightRow>[] = [
    {
      key: "class",
      header: "Class",
      accessor: (r) => `${r.className} ${r.section}`,
      sortable: true,
      cell: (r) => (
        <div>
          <p className="font-semibold text-navy-deep">{r.className} {r.section}</p>
          <p className="text-[12px] text-ink-3">{r.term}</p>
        </div>
      ),
    },
    { key: "total", header: "Total", accessor: (r) => r.totalDrafts, sortable: true, align: "right" },
    {
      key: "draft",
      header: "Draft",
      accessor: (r) => r.draftCount,
      sortable: true,
      align: "right",
      cell: (r) => <Badge tone="neutral">{r.draftCount}</Badge>,
    },
    {
      key: "flagged",
      header: "Flagged",
      accessor: (r) => r.flaggedCount,
      sortable: true,
      align: "right",
      cell: (r) =>
        r.flaggedCount > 0 ? (
          <Badge tone="warning">{r.flaggedCount}</Badge>
        ) : (
          <span className="text-ink-3">—</span>
        ),
    },
    {
      key: "approved",
      header: "Approved",
      accessor: (r) => r.approvedCount,
      sortable: true,
      align: "right",
      cell: (r) =>
        r.approvedCount > 0 ? (
          <Badge tone="success">{r.approvedCount}</Badge>
        ) : (
          <span className="text-ink-3">—</span>
        ),
    },
    {
      key: "published",
      header: "Published",
      accessor: (r) => r.publishedCount,
      sortable: true,
      align: "right",
      cell: (r) =>
        r.publishedCount > 0 ? (
          <Badge tone="accent">{r.publishedCount}</Badge>
        ) : (
          <span className="text-ink-3">—</span>
        ),
    },
    {
      key: "action",
      header: "Publish",
      accessor: () => "",
      align: "right",
      cell: (r) => (
        <button
          onClick={() => handlePublish(r)}
          disabled={publishing === `${r.className}-${r.section}` || r.approvedCount === 0}
          className="rounded-full bg-accent px-3 py-1.5 text-[12px] font-semibold text-white transition-colors hover:bg-accent-deep disabled:opacity-40 disabled:cursor-not-allowed"
        >
          {publishing === `${r.className}-${r.section}` ? "Publishing…" : `Publish (${r.approvedCount})`}
        </button>
      ),
    },
  ];

  const handlePublish = async (row: ReportCardOversightRow) => {
    const key = `${row.className}-${row.section}`;
    setPublishing(key);
    setPublishMsg(null);
    try {
      const result = await adminApi.reportCardPublish({
        className: row.className,
        section: row.section,
        term: row.term,
      });
      setPublishMsg(`Published ${result.published} report(s) for ${row.className} ${row.section}`);
    } catch (e) {
      setPublishMsg(`Failed to publish: ${(e as Error).message}`);
    } finally {
      setPublishing(null);
    }
  };

  return (
    <div className="space-y-6">
      {/* Term selector */}
      <div className="flex items-center gap-3">
        <label className="text-[14px] font-semibold text-navy-deep">Term:</label>
        <select
          value={term}
          onChange={(e) => setTerm(e.target.value)}
          className="rounded-xl border border-navy/12 bg-white px-3 py-2 text-[14px] text-navy-deep outline-none focus:border-accent"
        >
          <option value="Term 1">Term 1</option>
          <option value="Term 2">Term 2</option>
          <option value="Term 3">Term 3</option>
          <option value="Term 4">Term 4</option>
        </select>
      </div>

      {/* Summary tiles */}
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-5">
        <FadeIn>
          <StatTile label="Total Drafts" value={String(totals.total)} caption={`Across all classes (${term})`} loading={oversightLoading && !oversight} accent />
        </FadeIn>
        <FadeIn delay={0.04}>
          <StatTile label="Draft" value={String(totals.draft)} caption="Awaiting teacher review" loading={oversightLoading && !oversight} />
        </FadeIn>
        <FadeIn delay={0.08}>
          <StatTile label="Flagged" value={String(totals.flagged)} caption="Grounding issues" loading={oversightLoading && !oversight} />
        </FadeIn>
        <FadeIn delay={0.12}>
          <StatTile label="Approved" value={String(totals.approved)} caption="Ready to publish" loading={oversightLoading && !oversight} />
        </FadeIn>
        <FadeIn delay={0.16}>
          <StatTile label="Published" value={String(totals.published)} caption="Visible to parents" loading={oversightLoading && !oversight} />
        </FadeIn>
      </div>

      {/* Publish message */}
      {publishMsg && (
        <FadeIn>
          <div className="flex items-center gap-2 rounded-xl bg-navy/[0.03] px-4 py-3 text-[14px] font-medium text-navy-deep">
            <IconCheck width={16} height={16} className="text-success" />
            {publishMsg}
          </div>
        </FadeIn>
      )}

      {/* Oversight table */}
      <FadeIn delay={0.06}>
        <Card>
          <CardHeader title="Class Oversight" subtitle="Draft status across all classes — publish approved drafts to parents" />
          <div className="mt-2">
            <DataTable
              columns={columns}
              rows={classes}
              rowKey={(r) => `${r.className}-${r.section}-${r.term}`}
              loading={oversightLoading && !oversight}
              initialSort={{ key: "class", dir: "asc" }}
              emptyState={
                <EmptyState
                  icon={<IconReport width={26} height={26} />}
                  title="No drafts for this term"
                  hint="Teachers generate report card drafts from the mobile app. They appear here for oversight and publishing."
                />
              }
            />
          </div>
        </Card>
      </FadeIn>

      {/* Effectiveness (Learn loop) */}
      <FadeIn delay={0.1}>
        <Card>
          <CardHeader title="AI Effectiveness" subtitle="Focus area impact from the Learn flywheel — which interventions moved marks" />
          <div className="mt-2">
            {effLoading && !effectiveness ? (
              <div className="space-y-2">
                {[1, 2, 3].map((i) => <Skeleton key={i} className="h-16 w-full" />)}
              </div>
            ) : effectiveness && effectiveness.length > 0 ? (
              <div className="space-y-3">
                {effectiveness.map((eff) => (
                  <div key={eff.focusArea} className="flex items-center justify-between rounded-xl bg-navy/[0.02] px-4 py-3">
                    <div className="flex items-center gap-3">
                      <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-accent/10 text-accent">
                        <IconSparkle width={16} height={16} />
                      </div>
                      <div>
                        <p className="text-[14px] font-semibold capitalize text-navy-deep">{eff.focusArea.replace(/_/g, " ")}</p>
                        <p className="text-[12px] text-ink-3">
                          {eff.studentsImproved} of {eff.studentsTargeted} improved
                        </p>
                      </div>
                    </div>
                    <div className="flex items-center gap-3">
                      <div className="text-right">
                        <p className="nums text-[18px] font-bold text-navy-deep">
                          {Math.round(eff.effectivenessScore * 100)}%
                        </p>
                        <p className="text-[11px] text-ink-3 capitalize">{eff.confidence} confidence</p>
                      </div>
                      <Badge tone={eff.effectivenessScore >= 0.5 ? "success" : "warning"}>
                        {eff.effectivenessScore >= 0.5 ? "Effective" : "Low impact"}
                      </Badge>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <EmptyState
                icon={<IconSparkle width={26} height={26} />}
                title="No effectiveness data yet"
                hint="After two terms of report cards, the flywheel compares focus area interventions to actual mark changes."
              />
            )}
          </div>
        </Card>
      </FadeIn>

      {/* AI info banner */}
      <FadeIn delay={0.14}>
        <div className="flex items-start gap-3 rounded-2xl border border-accent/20 bg-accent/[0.03] p-4">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-accent/10 text-accent">
            <IconSparkle width={18} height={18} />
          </div>
          <div>
            <p className="text-[14px] font-semibold text-navy-deep">AI-Powered Report Cards</p>
            <p className="mt-0.5 text-[13px] text-ink-3">
              Narratives are generated by AI from real attendance, marks, and engagement data, then grounded
              against facts to prevent hallucination. Teachers review and edit every draft before publishing.
              The Learn flywheel tracks which focus area interventions actually improved marks term-over-term.
            </p>
          </div>
        </div>
      </FadeIn>
    </div>
  );
}
