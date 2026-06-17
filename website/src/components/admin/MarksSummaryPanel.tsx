"use client";

/**
 * Marks summary — published & in-progress assessments from the real
 * /api/v1/school/marks/summary aggregate (subject, assessment, class, class
 * average, graded count, published state). Pairs with Academic Health on the
 * Academics tab: coverage tells you how much was taught, marks tell you how
 * much was learned. Every row is real; empty when nothing is graded.
 *
 * SOURCE: marks/summary → assessments[]  ·  REFRESH: SLOW (marks publish in batches)
 */

import type { MarksSummaryDto } from "@/lib/admin/types";
import { Card, CardHeader, EmptyState, Skeleton, Badge, ProgressBar } from "./Primitives";

export function MarksSummaryPanel({
  data,
  loading,
  scopeClass = null,
}: {
  data: MarksSummaryDto | undefined;
  loading: boolean;
  scopeClass?: string | null;
}) {
  const rows = (data?.assessments ?? []).filter(
    (a) => !scopeClass || a.class_name === scopeClass,
  );

  return (
    <Card className="h-full pb-5" hover>
      <CardHeader
        title="Marks summary"
        subtitle="Assessment averages from published & in-progress grading"
        action={
          data && data.overall_average_pct > 0 ? (
            <Badge tone={data.overall_average_pct >= 60 ? "success" : "warning"}>
              {Math.round(data.overall_average_pct)}% avg
            </Badge>
          ) : null
        }
      />
      <div className="px-5 pt-3">
        {loading && !data ? (
          <div className="space-y-2">
            {Array.from({ length: 4 }).map((_, i) => (
              <Skeleton key={i} className="h-14" />
            ))}
          </div>
        ) : rows.length === 0 ? (
          <EmptyState
            title="No assessments graded yet"
            hint="When teachers grade and publish assessments, subject averages and pass bands appear here per class."
          />
        ) : (
          <ul className="space-y-2">
            {rows.map((a, i) => {
              // Clamp defensively — a backend row where average > max_marks must
              // never render >100% or overflow the progress bar.
              const pctOfMax =
                a.max_marks > 0 ? Math.min(100, Math.round((a.average / a.max_marks) * 100)) : 0;
              return (
                <li
                  key={`${a.subject}-${a.assessment}-${a.class_name}-${i}`}
                  className="rounded-2xl bg-navy/[0.03] px-4 py-3 ring-1 ring-inset ring-navy/[0.05]"
                >
                  <div className="flex items-center justify-between gap-3">
                    <div className="min-w-0">
                      <p className="truncate text-[13px] font-semibold text-navy-deep">
                        {a.subject} · {a.assessment}
                      </p>
                      <p className="truncate text-[12px] text-ink-3">
                        {a.class_name} · avg {a.average}/{a.max_marks} · {a.graded_count} graded
                      </p>
                    </div>
                    <div className="flex shrink-0 items-center gap-2">
                      <span className="nums text-[14px] font-extrabold text-navy-deep">
                        {pctOfMax}%
                      </span>
                      <Badge tone={a.is_published ? "success" : "neutral"}>
                        {a.is_published ? "Published" : "Draft"}
                      </Badge>
                    </div>
                  </div>
                  <ProgressBar
                    value={pctOfMax}
                    tone={pctOfMax >= 60 ? "success" : pctOfMax >= 40 ? "peach" : "accent"}
                    className="mt-2.5 h-1.5"
                  />
                </li>
              );
            })}
          </ul>
        )}
      </div>
    </Card>
  );
}
