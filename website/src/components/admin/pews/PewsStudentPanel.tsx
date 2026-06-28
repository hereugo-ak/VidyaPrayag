"use client";

import { useState } from "react";
import useSWR from "swr";
import { adminApi } from "@/lib/admin/client";
import type {
  PewsStudent,
  PewsStudentDetail,
  PewsIntervention,
  PewsOutcome,
} from "@/lib/admin/types";
import { Badge, Skeleton } from "@/components/admin/Primitives";
import { SidePanel } from "@/components/admin/SidePanel";
import { IconSparkle } from "@/components/admin/icons";
import {
  RISK_TONE,
  RISK_LABEL,
  STATUS_TONE,
  STATUS_LABEL,
  OUTCOME_LABEL,
  severityColor,
  describeSlope,
  shortDate,
  humanizeAction,
} from "./pewsUi";

/**
 * Admin "Student Signal" drill-down — the web twin of the mobile
 * PewsStudentDetailScreenV2. Shows the deterministic snapshot (attendance /
 * marks / leaves + trajectory), the WHY (real signals), the (nullable) AI
 * explanation, the risk history, and the interventions with Improved / No
 * change / Dismiss actions. Closing/updating an intervention PATCHes the server
 * (now returning the full DTO — the crash on mobile is fixed by the same
 * server change) and revalidates the cohort + interventions caches.
 */
export function PewsStudentPanel({
  studentCode,
  onClose,
  onMutated,
}: {
  studentCode: string | null;
  onClose: () => void;
  /** Called after a successful intervention update so the parent can revalidate. */
  onMutated?: () => void;
}) {
  const open = !!studentCode;
  const { data, isLoading, mutate } = useSWR(
    open ? ["pews/student", studentCode] : null,
    () => adminApi.pewsStudent(studentCode as string)
  );
  const { data: interventions, mutate: mutateInterventions } = useSWR(
    open ? "pews/interventions/all" : null,
    () => adminApi.pewsInterventions()
  );

  const current = data?.current ?? null;
  const mine = (interventions ?? []).filter((i) => i.student_code === studentCode);

  return (
    <SidePanel
      open={open}
      onClose={onClose}
      title={current?.name ?? "Student signal"}
      subtitle={
        current
          ? `Class ${current.class_name}-${current.section} · ${current.student_code}`
          : undefined
      }
    >
      {isLoading && !data ? (
        <div className="space-y-4">
          <Skeleton className="h-24" />
          <Skeleton className="h-20" />
          <Skeleton className="h-32" />
        </div>
      ) : !current ? (
        <p className="px-1 py-8 text-center text-[13px] text-ink-3">
          No current snapshot for this student. They may have dropped out of the
          at-risk cohort since the last run.
        </p>
      ) : (
        <div className="space-y-6">
          <RiskHeader s={current} />
          <Metrics s={current} />
          <Signals s={current} />
          <AiExplanation s={current} aiEnabled={current.ai_narrative != null} />
          <History history={data?.history ?? []} />
          <Interventions
            rows={mine}
            onUpdate={async (id, body) => {
              await adminApi.pewsUpdateIntervention(id, body);
              await Promise.all([mutate(), mutateInterventions()]);
              onMutated?.();
            }}
          />
        </div>
      )}
    </SidePanel>
  );
}

function RiskHeader({ s }: { s: PewsStudent }) {
  return (
    <div className="flex items-center justify-between gap-3 rounded-2xl bg-navy/[0.03] px-4 py-3.5 ring-1 ring-inset ring-navy/[0.05]">
      <div className="min-w-0">
        <p className="text-[13px] font-semibold text-navy-deep">
          Risk score {s.risk_score}
        </p>
        <p className="mt-0.5 text-[12px] text-ink-3">As of {s.run_date}</p>
      </div>
      <Badge tone={RISK_TONE[s.risk_level]}>{RISK_LABEL[s.risk_level]}</Badge>
    </div>
  );
}

function Metrics({ s }: { s: PewsStudent }) {
  const att = describeSlope(s.attendance_slope, "%");
  const marks = describeSlope(s.marks_slope, "%");
  return (
    <div className="grid grid-cols-3 gap-3">
      <Metric
        label="Attendance"
        value={s.attendance_pct != null ? `${s.attendance_pct}%` : "—"}
        trend={att}
      />
      <Metric
        label="Marks"
        value={s.marks_pct != null ? `${s.marks_pct}%` : "—"}
        trend={marks}
      />
      <Metric label="Leaves" value={`${s.leave_count}`} />
    </div>
  );
}

function Metric({
  label,
  value,
  trend,
}: {
  label: string;
  value: string;
  trend?: { text: string; tone: "up" | "down" | "flat" } | null;
}) {
  return (
    <div className="rounded-2xl bg-navy/[0.03] px-3 py-3.5 text-center ring-1 ring-inset ring-navy/[0.05]">
      <p className="nums text-[19px] font-extrabold text-navy-deep">{value}</p>
      <p className="mt-0.5 text-[11px] text-ink-3">{label}</p>
      {trend && (
        <p
          className={`mt-1 text-[10.5px] font-bold ${
            trend.tone === "down"
              ? "text-danger"
              : trend.tone === "up"
              ? "text-success"
              : "text-ink-3"
          }`}
        >
          {trend.text}
        </p>
      )}
    </div>
  );
}

function Signals({ s }: { s: PewsStudent }) {
  if (!s.signals.length) return null;
  return (
    <div>
      <p className="mb-2 text-[11px] font-bold uppercase tracking-wide text-ink-3">
        Why this student
      </p>
      <ul className="space-y-2">
        {s.signals.map((sig) => (
          <li
            key={sig.kind}
            className="flex items-start gap-3 rounded-2xl bg-navy/[0.03] px-4 py-3 ring-1 ring-inset ring-navy/[0.05]"
          >
            <span
              className="mt-1 h-2 w-2 shrink-0 rounded-full"
              style={{ background: severityColor(sig.severity) }}
            />
            <p className="text-[13px] leading-relaxed text-ink-2">{sig.label}</p>
          </li>
        ))}
      </ul>
    </div>
  );
}

function AiExplanation({ s, aiEnabled }: { s: PewsStudent; aiEnabled: boolean }) {
  // Honesty: only render when the LLM actually produced a narrative. When AI is
  // dormant (no keys) we show a quiet status line, never a fabricated reason.
  const hasAny = s.ai_narrative || s.ai_cause || s.ai_recommendation;
  return (
    <div className="rounded-2xl bg-accent/[0.05] px-4 py-4 ring-1 ring-inset ring-accent/15">
      <div className="mb-2 flex items-center gap-2">
        <IconSparkle width={15} height={15} className="text-accent-deep" />
        <p className="text-[11px] font-bold uppercase tracking-wide text-accent-deep">
          AI explanation
        </p>
        {s.ai_provider_used && (
          <Badge tone="accent" className="ml-auto">
            {s.ai_provider_used}
          </Badge>
        )}
      </div>
      {hasAny ? (
        <div className="space-y-2.5 text-[13px] leading-relaxed text-ink-2">
          {s.ai_narrative && <p>{s.ai_narrative}</p>}
          {s.ai_cause && (
            <p>
              <span className="font-semibold text-navy-deep">Likely cause: </span>
              {s.ai_cause}
            </p>
          )}
          {s.ai_recommendation && (
            <p>
              <span className="font-semibold text-navy-deep">Recommended: </span>
              {s.ai_recommendation}
            </p>
          )}
        </div>
      ) : (
        <p className="text-[12.5px] leading-relaxed text-ink-3">
          {aiEnabled
            ? "No AI explanation for this snapshot yet — it will appear after the next recompute."
            : "AI reasoning is not configured. The deterministic signals above are the full picture; add provider keys to enable a written explanation."}
        </p>
      )}
    </div>
  );
}

function History({ history }: { history: PewsStudent[] }) {
  if (history.length <= 1) return null;
  return (
    <div>
      <p className="mb-2 text-[11px] font-bold uppercase tracking-wide text-ink-3">
        Risk history
      </p>
      <ul className="space-y-1.5">
        {history.map((h) => (
          <li
            key={h.run_date}
            className="flex items-center justify-between gap-3 rounded-xl bg-navy/[0.025] px-3.5 py-2.5"
          >
            <span className="text-[12.5px] text-ink-2">{h.run_date}</span>
            <span className="flex items-center gap-2">
              <span className="nums text-[12.5px] font-semibold text-navy-deep">
                {h.risk_score}
              </span>
              <Badge tone={RISK_TONE[h.risk_level]}>{RISK_LABEL[h.risk_level]}</Badge>
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
}

function Interventions({
  rows,
  onUpdate,
}: {
  rows: PewsIntervention[];
  onUpdate: (
    id: string,
    body: { status?: PewsIntervention["status"]; outcome?: PewsOutcome }
  ) => Promise<void>;
}) {
  return (
    <div>
      <p className="mb-2 text-[11px] font-bold uppercase tracking-wide text-ink-3">
        Interventions
      </p>
      {rows.length === 0 ? (
        <p className="rounded-2xl bg-navy/[0.03] px-4 py-5 text-center text-[12.5px] text-ink-3 ring-1 ring-inset ring-navy/[0.05]">
          No interventions opened for this student yet.
        </p>
      ) : (
        <ul className="space-y-3">
          {rows.map((iv) => (
            <InterventionRow key={iv.id} iv={iv} onUpdate={onUpdate} />
          ))}
        </ul>
      )}
    </div>
  );
}

function InterventionRow({
  iv,
  onUpdate,
}: {
  iv: PewsIntervention;
  onUpdate: (
    id: string,
    body: { status?: PewsIntervention["status"]; outcome?: PewsOutcome }
  ) => Promise<void>;
}) {
  const [busy, setBusy] = useState<string | null>(null);
  const closed = iv.status === "done" || iv.status === "dismissed";

  async function act(label: string, body: { status?: PewsIntervention["status"]; outcome?: PewsOutcome }) {
    setBusy(label);
    try {
      await onUpdate(iv.id, body);
    } finally {
      setBusy(null);
    }
  }

  return (
    <li className="rounded-2xl bg-white px-4 py-3.5 shadow-card ring-1 ring-navy/[0.04]">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-[13.5px] font-semibold text-navy-deep">
            {humanizeAction(iv.action_type)}
          </p>
          <p className="mt-0.5 text-[12px] text-ink-3">Opened {shortDate(iv.opened_at)}</p>
        </div>
        <Badge tone={STATUS_TONE[iv.status]}>{STATUS_LABEL[iv.status]}</Badge>
      </div>

      {iv.notes && <p className="mt-2 text-[12.5px] leading-relaxed text-ink-2">{iv.notes}</p>}

      {iv.outcome && (
        <p className="mt-2 text-[12px] text-ink-3">
          Outcome:{" "}
          <span className="font-semibold text-navy-deep">{OUTCOME_LABEL[iv.outcome]}</span>
        </p>
      )}

      {!closed && (
        <div className="mt-3 flex flex-wrap gap-2">
          <ActionBtn
            label="Improved"
            tone="accent"
            busy={busy === "Improved"}
            onClick={() => act("Improved", { status: "done", outcome: "improved" })}
          />
          <ActionBtn
            label="No change"
            tone="neutral"
            busy={busy === "No change"}
            onClick={() => act("No change", { status: "done", outcome: "unchanged" })}
          />
          <ActionBtn
            label="Dismiss"
            tone="ghost"
            busy={busy === "Dismiss"}
            onClick={() => act("Dismiss", { status: "dismissed" })}
          />
        </div>
      )}
    </li>
  );
}

function ActionBtn({
  label,
  tone,
  busy,
  onClick,
}: {
  label: string;
  tone: "accent" | "neutral" | "ghost";
  busy: boolean;
  onClick: () => void;
}) {
  const styles =
    tone === "accent"
      ? "bg-accent/15 text-accent-deep hover:bg-accent/25"
      : tone === "neutral"
      ? "bg-white text-navy-deep ring-1 ring-inset ring-navy/12 hover:bg-navy/[0.03]"
      : "text-ink-2 hover:bg-navy/[0.04]";
  return (
    <button
      type="button"
      disabled={busy}
      onClick={onClick}
      className={`rounded-full px-3.5 py-2 text-[12.5px] font-semibold transition-colors disabled:cursor-not-allowed disabled:opacity-60 ${styles}`}
    >
      {busy ? "…" : label}
    </button>
  );
}
