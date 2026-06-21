"use client";

import { useState } from "react";
import type { AcademicHealth as AcademicHealthData, HealthCell } from "@/lib/admin/types";
import { Card, CardHeader, EmptyState, Skeleton } from "./Primitives";
import { SidePanel } from "./SidePanel";

/**
 * Academic health grid, syllabus coverage heat map. Classes on the Y axis,
 * subjects on the X axis, coverage % as colour intensity (from syllabus_units:
 * covered / total). Immediately surfaces which class-subject pairs are behind.
 * Click any cell → drill-down with the covered/total breakdown for that pair.
 * Empty when no syllabus units exist (honest, never a fabricated heat map).
 */
function cellColor(pct: number, hasData: boolean): { bg: string; fg: string } {
  if (!hasData) return { bg: "#F1EFFB", fg: "#BCC9C6" };
  // teal (good) → amber (mid) → red (behind) ramp, low alpha for density
  if (pct >= 85) return { bg: "rgba(31,122,77,0.16)", fg: "#1F7A4D" };
  if (pct >= 70) return { bg: "rgba(60,185,169,0.18)", fg: "#006A60" };
  if (pct >= 50) return { bg: "rgba(179,101,26,0.16)", fg: "#B3651A" };
  if (pct >= 30) return { bg: "rgba(179,101,26,0.26)", fg: "#8a4e14" };
  return { bg: "rgba(179,38,30,0.18)", fg: "#B3261E" };
}

export function AcademicHealth({
  data,
  loading,
}: {
  data: AcademicHealthData | undefined;
  loading: boolean;
}) {
  const [selected, setSelected] = useState<{ className: string; cell: HealthCell } | null>(null);

  const subjects = data?.subjects ?? [];
  const rows = data?.rows ?? [];

  return (
    <>
      <Card className="h-full pb-6" hover>
        <CardHeader
          title="Academic health"
          subtitle="Syllabus coverage by class & subject, click a cell to inspect"
        />
        <div className="px-5 pt-3">
          {loading && !data ? (
            <Skeleton className="h-[220px]" />
          ) : rows.length === 0 || subjects.length === 0 ? (
            <EmptyState
              title="No syllabus coverage yet"
              hint="When teachers start marking syllabus units covered, the heat map fills in and behind-schedule cells turn amber and red."
            />
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full border-separate border-spacing-1">
                <thead>
                  <tr>
                    <th className="sticky left-0 z-10 bg-white/0 px-2 py-1 text-left text-[11px] font-semibold uppercase tracking-wide text-ink-3">
                      Class
                    </th>
                    {subjects.map((s) => (
                      <th
                        key={s}
                        className="px-1 py-1 text-center text-[11px] font-semibold text-ink-3"
                        title={s}
                      >
                        <span className="block max-w-[64px] truncate">{s}</span>
                      </th>
                    ))}
                    <th className="px-2 py-1 text-center text-[11px] font-semibold uppercase tracking-wide text-ink-3">
                      Avg
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((row) => (
                    <tr key={row.class_name}>
                      <td className="sticky left-0 z-10 whitespace-nowrap bg-white px-2 py-1 text-[12px] font-semibold text-navy-deep">
                        {row.class_name}
                      </td>
                      {row.cells.map((cell) => {
                        const has = cell.total_units > 0;
                        const c = cellColor(cell.percentage, has);
                        return (
                          <td key={cell.subject} className="p-0">
                            <button
                              disabled={!has}
                              onClick={() => setSelected({ className: row.class_name, cell })}
                              style={{ background: c.bg, color: c.fg }}
                              className={`nums flex h-11 w-full min-w-[52px] items-center justify-center rounded-xl text-[12px] font-bold transition-transform duration-150 ${
                                has ? "hover:scale-[1.06] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent/40" : "cursor-default"
                              }`}
                              title={has ? `${cell.subject}: ${cell.covered_units}/${cell.total_units} units` : "No units"}
                            >
                              {has ? `${cell.percentage}%` : "-"}
                            </button>
                          </td>
                        );
                      })}
                      <td className="px-2 text-center">
                        <span className="nums text-[12px] font-bold text-navy-deep">
                          {row.class_average}%
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <div className="mt-3 flex flex-wrap items-center gap-3 text-[11px] text-ink-3">
                <LegendChip color="rgba(31,122,77,0.16)" label="On track ≥85%" />
                <LegendChip color="rgba(179,101,26,0.16)" label="Behind 50–70%" />
                <LegendChip color="rgba(179,38,30,0.18)" label="Critical <30%" />
              </div>
            </div>
          )}
        </div>
      </Card>

      <SidePanel
        open={!!selected}
        onClose={() => setSelected(null)}
        title={selected ? `${selected.cell.subject}` : ""}
        subtitle={selected ? `${selected.className} · syllabus coverage` : undefined}
      >
        {selected && (
          <div className="space-y-5">
            <div className="rounded-3xl bg-wash-lavender p-6 text-center shadow-soft">
              <p className="nums text-[44px] font-extrabold leading-none text-navy-deep">
                {selected.cell.percentage}%
              </p>
              <p className="mt-1.5 text-[13px] text-ink-3">
                {selected.cell.covered_units} of {selected.cell.total_units} units covered
              </p>
            </div>
            <div className="rounded-2xl bg-navy/[0.03] p-4 ring-1 ring-inset ring-navy/[0.05]">
              <p className="text-[13px] font-semibold text-navy-deep">Status</p>
              <p className="mt-1 text-[13px] leading-relaxed text-ink-2">
                {selected.cell.percentage >= 85
                  ? "On track. Coverage is healthy for this class-subject pair."
                  : selected.cell.percentage >= 50
                  ? "Slightly behind. Worth a check-in with the assigned teacher to confirm pacing."
                  : "Significantly behind schedule. This class-subject pair needs attention before the next assessment."}
              </p>
            </div>
          </div>
        )}
      </SidePanel>
    </>
  );
}

function LegendChip({ color, label }: { color: string; label: string }) {
  return (
    <span className="inline-flex items-center gap-1.5">
      <span className="h-3 w-3 rounded" style={{ background: color }} />
      {label}
    </span>
  );
}
