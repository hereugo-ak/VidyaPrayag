"use client";

import { useState, useMemo } from "react";
import useSWR from "swr";
import { adminApi } from "@/lib/admin/client";
import type {
  PewsStudent,
  PewsStudentDetail,
  PewsIntervention,
  PewsOutcome,
  PewsDraftMessage,
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
            onDraftMessage={async (id, lang) => {
              const res = await adminApi.pewsDraftMessage(id, lang);
              return res;
            }}
            onSendParentMessage={async (id) => {
              const res = await adminApi.pewsSendParentMessage(id);
              await Promise.all([mutate(), mutateInterventions()]);
              onMutated?.();
              return res;
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
  onDraftMessage,
  onSendParentMessage,
}: {
  rows: PewsIntervention[];
  onUpdate: (
    id: string,
    body: { status?: PewsIntervention["status"]; outcome?: PewsOutcome }
  ) => Promise<void>;
  onDraftMessage: (id: string, lang: string) => Promise<PewsDraftMessage | null>;
  onSendParentMessage: (id: string) => Promise<{ sent_count: number } | null>;
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
            <InterventionRow
              key={iv.id}
              iv={iv}
              onUpdate={onUpdate}
              onDraftMessage={onDraftMessage}
              onSendParentMessage={onSendParentMessage}
            />
          ))}
        </ul>
      )}
    </div>
  );
}

const LANGUAGES = [
  { code: "en", label: "English" },
  { code: "hi", label: "हिन्दी" },
  { code: "mr", label: "मराठी" },
  { code: "ta", label: "தமிழ்" },
  { code: "te", label: "తెలుగు" },
  { code: "bn", label: "বাংলা" },
];

const RULE_BASED_ACTIONS: Record<string, { label: string; description: string; outcome?: PewsOutcome }> = {
  parent_call: { label: "Call parent", description: "Contact the parent by phone to discuss the student's situation.", outcome: "improved" },
  parent_message: { label: "Message parent", description: "Send a message to the parent through the in-app messaging system.", outcome: "improved" },
  home_visit: { label: "Home visit", description: "Schedule a home visit to meet the parents and discuss concerns.", outcome: "improved" },
  remedial_class: { label: "Assign remedial class", description: "Enroll the student in extra support classes for the identified subject.", outcome: "improved" },
  counselling: { label: "Schedule counselling", description: "Arrange a counselling session for the student with the school counsellor.", outcome: "improved" },
  mentor_pairing: { label: "Pair with mentor", description: "Assign a peer mentor or teacher mentor to support the student.", outcome: "improved" },
  fee_counselling: { label: "Fee counselling", description: "Discuss fee structure and available support with the parents.", outcome: "improved" },
  observe: { label: "Continue observing", description: "No immediate action — continue monitoring the student's signals.", outcome: "unchanged" },
};

function InterventionRow({
  iv,
  onUpdate,
  onDraftMessage,
  onSendParentMessage,
}: {
  iv: PewsIntervention;
  onUpdate: (
    id: string,
    body: { status?: PewsIntervention["status"]; outcome?: PewsOutcome }
  ) => Promise<void>;
  onDraftMessage: (id: string, lang: string) => Promise<PewsDraftMessage | null>;
  onSendParentMessage: (id: string) => Promise<{ sent_count: number } | null>;
}) {
  const [busy, setBusy] = useState<string | null>(null);
  const [draftLang, setDraftLang] = useState("en");
  const [draft, setDraft] = useState<PewsDraftMessage | null>(null);
  const [draftLoading, setDraftLoading] = useState(false);
  const [confirmAction, setConfirmAction] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const closed = iv.status === "done" || iv.status === "dismissed";

  // Build hybrid action list: AI-suggested from plan_json + rule-based fallback
  const actions = useMemo(() => {
    const list: { id: string; label: string; description: string; outcome?: PewsOutcome; isAiSuggested: boolean }[] = [];

    // AI-suggested actions from plan steps
    if (iv.plan_json) {
      const planSteps = parsePlanStepsWithMeta(iv.plan_json);
      planSteps.forEach((step, i) => {
        const ruleAction = RULE_BASED_ACTIONS[step.action];
        if (ruleAction) {
          list.push({
            id: `ai_${i}_${step.action}`,
            label: ruleAction.label,
            description: step.rationale || ruleAction.description,
            outcome: ruleAction.outcome,
            isAiSuggested: true,
          });
        } else {
          list.push({
            id: `ai_${i}_${step.action}`,
            label: humanizeAction(step.action),
            description: step.rationale || "AI-suggested action",
            outcome: "improved",
            isAiSuggested: true,
          });
        }
      });
    }

    // Rule-based fallback: always include the current action type if not already in list
    const currentAction = RULE_BASED_ACTIONS[iv.action_type];
    if (currentAction && !list.some((a) => a.label === currentAction.label)) {
      list.push({
        id: `rule_${iv.action_type}`,
        label: currentAction.label,
        description: currentAction.description,
        outcome: currentAction.outcome,
        isAiSuggested: false,
      });
    }

    // If no actions at all, add a generic observe
    if (list.length === 0) {
      list.push({
        id: "rule_observe",
        label: "Continue observing",
        description: "No specific action mapped — continue monitoring.",
        outcome: "unchanged",
        isAiSuggested: false,
      });
    }

    return list;
  }, [iv.plan_json, iv.action_type]);

  async function act(label: string, body: { status?: PewsIntervention["status"]; outcome?: PewsOutcome }) {
    setBusy(label);
    setError(null);
    try {
      await onUpdate(iv.id, body);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Update failed");
    } finally {
      setBusy(null);
      setConfirmAction(null);
    }
  }

  async function handleGenerateDraft() {
    setDraftLoading(true);
    setError(null);
    try {
      const result = await onDraftMessage(iv.id, draftLang);
      if (result) {
        setDraft(result);
      } else {
        setError("Failed to generate draft");
      }
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Draft generation failed");
    } finally {
      setDraftLoading(false);
    }
  }

  async function handleSendParentMessage() {
    setBusy("send_parent");
    setError(null);
    try {
      const result = await onSendParentMessage(iv.id);
      if (!result) {
        setError("Failed to send message");
      }
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Send failed");
    } finally {
      setBusy(null);
    }
  }

  const isParentAction = iv.action_type.includes("parent") || iv.action_type.includes("message") || iv.action_type.includes("call") || iv.action_type.includes("visit");

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

      {/* PEWS 2.0 — urgency, escalation, cause family badges */}
      {(iv.urgency || (iv.escalation_level && iv.escalation_level > 0) || iv.cause_family) && (
        <div className="mt-2 flex flex-wrap gap-1.5">
          {iv.urgency && (
            <Badge tone={iv.urgency === "high" ? "danger" : iv.urgency === "medium" ? "warning" : "neutral"}>
              {iv.urgency.charAt(0).toUpperCase() + iv.urgency.slice(1)} urgency
            </Badge>
          )}
          {iv.escalation_level && iv.escalation_level > 0 && (
            <Badge tone={iv.escalation_level >= 2 ? "danger" : "warning"}>
              {iv.escalation_level >= 2 ? "Escalated" : "Reminded"}
            </Badge>
          )}
          {iv.cause_family && (
            <Badge tone="neutral">{iv.cause_family}</Badge>
          )}
        </div>
      )}

      {/* SLA + follow-up */}
      {iv.sla_days != null && (
        <p className="mt-1.5 text-[11.5px] text-ink-3">
          SLA: {iv.sla_days} days{iv.follow_up_date ? ` · follow-up ${iv.follow_up_date}` : ""}
        </p>
      )}

      {iv.notes && <p className="mt-2 text-[12.5px] leading-relaxed text-ink-2">{iv.notes}</p>}

      {/* Plan steps from plan_json */}
      {iv.plan_json && parsePlanSteps(iv.plan_json).length > 0 && (
        <div className="mt-2.5">
          <p className="text-[10px] font-bold uppercase tracking-wide text-ink-3">Plan</p>
          <ol className="mt-1 space-y-1">
            {parsePlanSteps(iv.plan_json).map((step, i) => (
              <li key={i} className="text-[12px] leading-relaxed text-ink-2">
                <span className="text-ink-3">{i + 1}.</span> {step}
              </li>
            ))}
          </ol>
        </div>
      )}

      {iv.outcome && (
        <p className="mt-2 text-[12px] text-ink-3">
          Outcome:{" "}
          <span className="font-semibold text-navy-deep">{OUTCOME_LABEL[iv.outcome]}</span>
        </p>
      )}

      {error && (
        <p className="mt-2 rounded-lg bg-danger/10 px-3 py-2 text-[12px] text-danger">{error}</p>
      )}

      {/* Parent draft message section */}
      {!closed && isParentAction && (
        <div className="mt-3 rounded-xl bg-accent/[0.06] px-3 py-2.5 ring-1 ring-inset ring-accent/15">
          {draft ? (
            <div>
              <div className="flex items-center gap-2">
                <IconSparkle width={13} height={13} className="text-accent-deep" />
                <p className="text-[10px] font-bold uppercase tracking-wide text-accent-deep">
                  Parent message ({draft.language.toUpperCase()})
                </p>
                <button
                  type="button"
                  onClick={() => setDraft(null)}
                  className="ml-auto text-[11px] text-ink-3 hover:text-navy-deep"
                >
                  ✕
                </button>
              </div>
              <p className="mt-1.5 text-[12.5px] leading-relaxed text-ink-2">{draft.body}</p>
              <div className="mt-2 flex gap-2">
                <ActionBtn
                  label="Send to parent"
                  tone="accent"
                  busy={busy === "send_parent"}
                  onClick={handleSendParentMessage}
                />
                <ActionBtn
                  label="Regenerate"
                  tone="ghost"
                  busy={draftLoading}
                  onClick={handleGenerateDraft}
                />
              </div>
            </div>
          ) : (
            <div className="flex items-center gap-2">
              <ActionBtn
                label="Draft parent message"
                tone="accent"
                busy={draftLoading}
                onClick={handleGenerateDraft}
              />
              <select
                value={draftLang}
                onChange={(e) => setDraftLang(e.target.value)}
                className="rounded-lg border border-navy/15 bg-white px-2 py-1.5 text-[12px] text-navy-deep outline-none focus:border-accent"
              >
                {LANGUAGES.map((l) => (
                  <option key={l.code} value={l.code}>{l.label}</option>
                ))}
              </select>
            </div>
          )}
        </div>
      )}

      {/* Hybrid agentic actions */}
      {!closed && (
        <div className="mt-3">
          <p className="mb-1.5 text-[10px] font-bold uppercase tracking-wide text-ink-3">
            {actions.some((a) => a.isAiSuggested) ? "AI-suggested actions" : "Recommended actions"}
          </p>
          <div className="space-y-1.5">
            {actions.map((action) => (
              <div key={action.id}>
                {confirmAction === action.id ? (
                  <div className="rounded-xl bg-navy/[0.04] px-3 py-2.5 ring-1 ring-inset ring-navy/10">
                    <p className="text-[12px] leading-relaxed text-ink-2">{action.description}</p>
                    <p className="mt-1 text-[11px] text-ink-3">
                      This will mark the intervention as done with outcome: {action.outcome || "improved"}
                    </p>
                    <div className="mt-2 flex gap-2">
                      <ActionBtn
                        label="Confirm"
                        tone="accent"
                        busy={busy === action.label}
                        onClick={() => act(action.label, { status: "done", outcome: action.outcome || "improved" })}
                      />
                      <ActionBtn
                        label="Cancel"
                        tone="ghost"
                        busy={false}
                        onClick={() => setConfirmAction(null)}
                      />
                    </div>
                  </div>
                ) : (
                  <button
                    type="button"
                    onClick={() => setConfirmAction(action.id)}
                    className="flex w-full items-center gap-2 rounded-xl px-3 py-2 text-left transition-colors hover:bg-navy/[0.03]"
                  >
                    {action.isAiSuggested && (
                      <IconSparkle width={12} height={12} className="shrink-0 text-accent-deep" />
                    )}
                    <span className="text-[12.5px] font-semibold text-navy-deep">{action.label}</span>
                    {action.isAiSuggested && (
                      <Badge tone="accent" className="ml-auto text-[9px]">AI</Badge>
                    )}
                  </button>
                )}
              </div>
            ))}
          </div>
          {/* Rule-based outcome buttons as last resort */}
          <div className="mt-2 flex flex-wrap gap-2 border-t border-navy/[0.06] pt-2">
            <ActionBtn
              label="Mark improved"
              tone="accent"
              busy={busy === "Mark improved"}
              onClick={() => act("Mark improved", { status: "done", outcome: "improved" })}
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
        </div>
      )}
    </li>
  );
}

/** Parse plan_json to extract step descriptions. */
function parsePlanSteps(planJson: string): string[] {
  try {
    const obj = JSON.parse(planJson);
    const steps = obj.steps || obj.plan;
    if (!Array.isArray(steps)) return [];
    return steps.map((step: unknown) => {
      if (typeof step === "string") return step;
      if (typeof step === "object" && step !== null) {
        const s = step as Record<string, unknown>;
        return (s.description || s.action || s.text || "") as string;
      }
      return "";
    }).filter(Boolean);
  } catch {
    return [];
  }
}

/** Parse plan_json to extract structured step metadata (action + rationale). */
function parsePlanStepsWithMeta(planJson: string): { action: string; rationale?: string }[] {
  try {
    const obj = JSON.parse(planJson);
    const steps = obj.steps || obj.plan;
    if (!Array.isArray(steps)) return [];
    return steps.map((step: unknown) => {
      if (typeof step === "object" && step !== null) {
        const s = step as Record<string, unknown>;
        return {
          action: (s.action || s.description || s.text || "") as string,
          rationale: (s.rationale || undefined) as string | undefined,
        };
      }
      return { action: String(step) };
    }).filter((s) => s.action);
  } catch {
    return [];
  }
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
