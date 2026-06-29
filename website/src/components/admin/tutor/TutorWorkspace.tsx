"use client";

import { useState, useMemo } from "react";
import { useTutorTeacherScope, useTutorHeatmap } from "@/lib/admin/hooks";
import type { TutorHeatmapCell } from "@/lib/admin/types";
import {
  Badge,
  Card,
  CardHeader,
  EmptyState,
  FadeIn,
  Skeleton,
} from "@/components/admin/Primitives";
import { StatTile } from "@/components/admin/StatTile";
import { IconBook, IconSparkle, IconWarning } from "@/components/admin/icons";

const SEVERITY_TONE: Record<string, "neutral" | "warning" | "danger"> = {
  high: "danger",
  medium: "warning",
  low: "neutral",
};

export function TutorWorkspace() {
  const { data: scope, isLoading: scopeLoading } = useTutorTeacherScope();
  const [selectedClassId, setSelectedClassId] = useState<string | null>(null);
  const [selectedSubjectId, setSelectedSubjectId] = useState<string | null>(null);

  const classes = scope?.classes ?? [];

  const firstClass = classes[0];
  const activeClassId = selectedClassId ?? firstClass?.classId ?? null;
  const activeClass = classes.find((c) => c.classId === activeClassId);
  const activeSubjectId = selectedSubjectId ?? activeClass?.subjects[0]?.subjectId ?? null;

  const { data: heatmap, isLoading: heatmapLoading } = useTutorHeatmap(
    activeClassId,
    activeSubjectId,
  );

  const cells = heatmap?.cells ?? [];

  const summary = useMemo(() => {
    const total = cells.length;
    const high = cells.filter((c) => c.severity === "high").length;
    const medium = cells.filter((c) => c.severity === "medium").length;
    const low = cells.filter((c) => c.severity === "low").length;
    const affectedChildren = heatmap?.totalChildren ?? 0;
    return { total, high, medium, low, affectedChildren };
  }, [cells, heatmap]);

  return (
    <div className="space-y-6">
      {/* Class + Subject selectors */}
      <div className="flex flex-wrap items-center gap-4">
        <div className="flex items-center gap-2">
          <label className="text-[14px] font-semibold text-navy-deep">Class:</label>
          <select
            value={activeClassId ?? ""}
            onChange={(e) => {
              setSelectedClassId(e.target.value || null);
              setSelectedSubjectId(null);
            }}
            disabled={scopeLoading || classes.length === 0}
            className="rounded-xl border border-navy/12 bg-white px-3 py-2 text-[14px] text-navy-deep outline-none focus:border-accent"
          >
            {classes.length === 0 && <option value="">No classes assigned</option>}
            {classes.map((c) => (
              <option key={c.classId} value={c.classId}>
                {c.className} {c.section}
              </option>
            ))}
          </select>
        </div>

        {activeClass && activeClass.subjects.length > 0 && (
          <div className="flex items-center gap-2">
            <label className="text-[14px] font-semibold text-navy-deep">Subject:</label>
            <select
              value={activeSubjectId ?? ""}
              onChange={(e) => setSelectedSubjectId(e.target.value || null)}
              className="rounded-xl border border-navy/12 bg-white px-3 py-2 text-[14px] text-navy-deep outline-none focus:border-accent"
            >
              {activeClass.subjects.map((s) => (
                <option key={s.subjectId} value={s.subjectId}>
                  {s.subjectName}
                </option>
              ))}
            </select>
          </div>
        )}
      </div>

      {/* Summary tiles */}
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        <FadeIn>
          <StatTile
            label="Total Misconceptions"
            value={String(summary.total)}
            caption={`Across ${summary.affectedChildren} students`}
            loading={heatmapLoading && !heatmap}
            accent
          />
        </FadeIn>
        <FadeIn delay={0.04}>
          <StatTile
            label="High Severity"
            value={String(summary.high)}
            caption="Urgent intervention"
            loading={heatmapLoading && !heatmap}
          />
        </FadeIn>
        <FadeIn delay={0.08}>
          <StatTile
            label="Medium Severity"
            value={String(summary.medium)}
            caption="Monitor closely"
            loading={heatmapLoading && !heatmap}
          />
        </FadeIn>
        <FadeIn delay={0.12}>
          <StatTile
            label="Low Severity"
            value={String(summary.low)}
            caption="Minor gaps"
            loading={heatmapLoading && !heatmap}
          />
        </FadeIn>
      </div>

      {/* Heatmap grid */}
      <FadeIn delay={0.06}>
        <Card>
          <CardHeader
            title="Misconception Heatmap"
            subtitle="Topics where students struggle most — updated live as kids practice"
          />
          <div className="mt-2">
            {heatmapLoading && !heatmap ? (
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
                {[1, 2, 3, 4, 5, 6].map((i) => (
                  <Skeleton key={i} className="h-24 w-full" />
                ))}
              </div>
            ) : cells.length > 0 ? (
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
                {cells
                  .sort((a, b) => {
                    const order = { high: 0, medium: 1, low: 2 };
                    return order[a.severity as keyof typeof order] - order[b.severity as keyof typeof order];
                  })
                  .map((cell) => (
                    <HeatmapCell key={`${cell.topicId}-${cell.misconceptionType}`} cell={cell} />
                  ))}
              </div>
            ) : (
              <EmptyState
                icon={<IconBook width={26} height={26} />}
                title="No misconception data"
                hint="Heatmap data appears after students practice on the AI Tutor. Select a class and subject above."
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
            <p className="text-[14px] font-semibold text-navy-deep">AI Tutor — Adaptive Learning</p>
            <p className="mt-0.5 text-[13px] text-ink-3">
              The heatmap aggregates real student misconceptions from practice sessions. Topics with high
              severity affect many students and have low average mastery — these are the areas to re-teach
              in class. The Learn loop tracks which tutoring interventions actually improved mastery over time.
            </p>
          </div>
        </div>
      </FadeIn>
    </div>
  );
}

function HeatmapCell({ cell }: { cell: TutorHeatmapCell }) {
  const tone = SEVERITY_TONE[cell.severity] ?? "neutral";
  const masteryPct = Math.round(cell.avgMastery);
  const severityColor =
    tone === "danger"
      ? "border-danger/20 bg-danger/[0.04]"
      : tone === "warning"
      ? "border-warning/20 bg-warning/[0.04]"
      : "border-navy/8 bg-white/85";

  return (
    <div className={`rounded-2xl border p-4 ${severityColor}`}>
      <div className="flex items-start justify-between gap-2">
        <div className="min-w-0">
          <p className="truncate text-[14px] font-semibold capitalize text-navy-deep">
            {cell.misconceptionType.replace(/_/g, " ")}
          </p>
          <p className="mt-0.5 text-[12px] text-ink-3">
            {cell.affectedChildren} student{cell.affectedChildren !== 1 ? "s" : ""} affected
          </p>
        </div>
        <Badge tone={tone}>{cell.severity}</Badge>
      </div>
      <div className="mt-3">
        <div className="flex items-center justify-between text-[12px]">
          <span className="text-ink-3">Avg mastery</span>
          <span className="nums font-bold text-navy-deep">{masteryPct}%</span>
        </div>
        <div className="mt-1.5 h-2 overflow-hidden rounded-full bg-navy/[0.06]">
          <div
            className={`h-full rounded-full transition-all ${
              masteryPct >= 60 ? "bg-success" : masteryPct >= 30 ? "bg-warning" : "bg-danger"
            }`}
            style={{ width: `${masteryPct}%` }}
          />
        </div>
      </div>
    </div>
  );
}
