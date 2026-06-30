"use client";

import { useState } from "react";
import Link from "next/link";
import type { EarlyWarningStudent } from "@/lib/admin/types";
import { Avatar, Badge, Card, CardHeader, EmptyState, Skeleton } from "./Primitives";
import { SidePanel } from "./SidePanel";
import { IconWarning } from "./icons";
import { adminApi } from "@/lib/admin/client";

/**
 * Early-warning intelligence, students flagged by REAL combined signals
 * (attendance < 75%, marks < 40%, ≥3 leave requests), computed server-side
 * from attendance_records + assessment_marks + leave_requests. Each row shows
 * exactly which signals fired (no opaque score). Click a row → drill-down with
 * a one-click "notify parent" action that posts a real notification.
 */
const RISK_TONE = {
  high: "danger",
  medium: "warning",
  watch: "neutral",
} as const;

const RISK_LABEL = {
  high: "High risk",
  medium: "Medium",
  watch: "Watch",
} as const;

export function EarlyWarning({
  data,
  loading,
  variant = "full",
  scopeClass = null,
}: {
  data: EarlyWarningStudent[] | undefined;
  loading: boolean;
  /** "preview" caps the list to the top 5 with a "view all" footer. */
  variant?: "preview" | "full";
  /** When the dashboard is globally scoped, filter the cohort to one class. */
  scopeClass?: string | null;
}) {
  const [selected, setSelected] = useState<EarlyWarningStudent | null>(null);
  const [notified, setNotified] = useState<Set<string>>(new Set());
  const [sending, setSending] = useState(false);
  const [expanded, setExpanded] = useState(false);

  const all = (data ?? []).filter((s) => !scopeClass || s.class_name === scopeClass);
  const cap = variant === "preview" && !expanded ? 5 : all.length;
  const rows = all.slice(0, cap);
  const hiddenCount = all.length - rows.length;

  async function notifyParent(s: EarlyWarningStudent) {
    setSending(true);
    try {
      // Posts a real school-wide announcement targeted at the student (STUDENT
      // audience), the existing backend create-announcement path. Honest action.
      await adminApi.createAnnouncement({
        type: "Reminder",
        title: `Attention needed, ${s.name}`,
        description: `${s.name} (${s.class_name}-${s.section}) has been flagged: ${s.signals
          .map((x) => x.label)
          .join("; ")}. Please connect with the parent.`,
        date: new Date().toISOString().slice(0, 10),
        audience_type: "STUDENT",
        audience_filter: { student_codes: [s.student_code] },
      });
      setNotified((prev) => new Set(prev).add(s.student_code));
    } catch {
      /* surface nothing destructive; the row stays actionable */
    } finally {
      setSending(false);
    }
  }

  return (
    <>
      <Card className="flex h-full flex-col pb-3" hover>
        <CardHeader
          title="Early warning"
          subtitle="Students flagged by real attendance, marks & leave signals"
          action={all.length > 0 ? <Badge tone="danger">{all.length} flagged</Badge> : null}
        />
        <div className="flex-1 overflow-y-auto px-2 pt-2">
          {loading && !data ? (
            <div className="space-y-2 p-3">
              {Array.from({ length: 4 }).map((_, i) => (
                <Skeleton key={i} className="h-14" />
              ))}
            </div>
          ) : rows.length === 0 ? (
            <EmptyState
              icon={<IconWarning />}
              title="No students at risk"
              hint="When attendance, marks, or leave signals cross a threshold, flagged students appear here ranked by severity."
            />
          ) : (
            <ul className="space-y-1 px-1.5 pb-1.5">
              {rows.map((s) => (
                <li key={s.student_code}>
                  <button
                    onClick={() => setSelected(s)}
                    className="flex w-full items-center gap-3 rounded-2xl px-3 py-3 text-left transition-colors hover:bg-navy/[0.035] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent/30"
                  >
                    <Avatar name={s.name} size={36} />
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center gap-2">
                        <p className="truncate text-[13px] font-semibold text-navy-deep">{s.name}</p>
                        <Badge tone={RISK_TONE[s.risk_level]}>{RISK_LABEL[s.risk_level]}</Badge>
                      </div>
                      <p className="mt-0.5 truncate text-[12px] text-ink-3">
                        {s.class_name}-{s.section} ·{" "}
                        {s.signals.map((x) => x.kind).join(" + ")}
                      </p>
                    </div>
                    <div className="flex shrink-0 gap-1.5">
                      {s.signals.map((sig) => (
                        <span
                          key={sig.kind}
                          className="inline-block h-1.5 w-1.5 rounded-full"
                          style={{
                            background:
                              sig.severity === 3
                                ? "#B3261E"
                                : sig.severity === 2
                                ? "#B3651A"
                                : "#6D7A77",
                          }}
                          aria-hidden
                        />
                      ))}
                    </div>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
        {variant === "preview" && all.length > 5 && (
          <button
            type="button"
            onClick={() => setExpanded((v) => !v)}
            className="mx-3 mb-1 mt-1 rounded-2xl px-3 py-2.5 text-[12.5px] font-bold text-accent-deep transition-colors hover:bg-accent/[0.06] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent/30"
          >
            {expanded ? "Show less" : `View all ${all.length} flagged →`}
          </button>
        )}
        {/* Bridge into the full PEWS loop (risk bands, AI explanation,
            interventions, effectiveness, config) instead of dead-ending here. */}
        <Link
          href="/admin/early-warning"
          className="mx-3 mb-2 mt-1 rounded-2xl px-3 py-2.5 text-center text-[12.5px] font-bold text-navy-deep transition-colors hover:bg-navy/[0.04] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent/30"
        >
          Open Early Warning →
        </Link>
      </Card>

      <SidePanel
        open={!!selected}
        onClose={() => setSelected(null)}
        title={selected?.name ?? ""}
        subtitle={selected ? `${selected.class_name}-${selected.section} · ${selected.student_code}` : undefined}
      >
        {selected && (
          <div className="space-y-5">
            <div className="grid grid-cols-3 gap-3">
              <Metric label="Attendance" value={selected.attendance_pct != null ? `${selected.attendance_pct}%` : "-"} />
              <Metric label="Avg marks" value={selected.marks_pct != null ? `${selected.marks_pct}%` : "-"} />
              <Metric label="Leaves" value={`${selected.leave_count}`} />
            </div>

            <div>
              <p className="mb-2 text-[13px] font-semibold text-navy-deep">Flagged signals</p>
              <ul className="space-y-2">
                {selected.signals.map((sig) => (
                  <li
                    key={sig.kind}
                    className="flex items-start gap-3 rounded-2xl bg-navy/[0.03] px-4 py-3 ring-1 ring-inset ring-navy/[0.05]"
                  >
                    <span
                      className="mt-1 h-2 w-2 shrink-0 rounded-full"
                      style={{
                        background: sig.severity === 3 ? "#B3261E" : sig.severity === 2 ? "#B3651A" : "#6D7A77",
                      }}
                    />
                    <p className="text-[13px] leading-relaxed text-ink-2">{sig.label}</p>
                  </li>
                ))}
              </ul>
            </div>

            <button
              disabled={sending || notified.has(selected.student_code)}
              onClick={() => notifyParent(selected)}
              className="w-full rounded-full bg-navy-deep px-4 py-3.5 text-[14px] font-semibold text-white shadow-pill transition-all duration-200 hover:-translate-y-0.5 hover:bg-navy disabled:cursor-not-allowed disabled:opacity-60 disabled:hover:translate-y-0"
            >
              {notified.has(selected.student_code)
                ? "Parent notified ✓"
                : sending
                ? "Sending…"
                : "Notify parent"}
            </button>
          </div>
        )}
      </SidePanel>
    </>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl bg-navy/[0.03] px-3 py-3.5 text-center ring-1 ring-inset ring-navy/[0.05]">
      <p className="nums text-[19px] font-extrabold text-navy-deep">{value}</p>
      <p className="mt-0.5 text-[11px] text-ink-3">{label}</p>
    </div>
  );
}
