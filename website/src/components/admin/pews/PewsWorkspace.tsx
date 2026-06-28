"use client";

import { useMemo, useState } from "react";
import {
  usePewsCohort,
  usePewsEffectiveness,
  usePewsConfig,
} from "@/lib/admin/hooks";
import { adminApi } from "@/lib/admin/client";
import type { PewsRiskLevel, PewsConfig } from "@/lib/admin/types";
import {
  Avatar,
  Badge,
  Card,
  CardHeader,
  EmptyState,
  FadeIn,
  Skeleton,
} from "@/components/admin/Primitives";
import { IconShield, IconSparkle } from "@/components/admin/icons";
import { PewsStudentPanel } from "./PewsStudentPanel";
import { RISK_TONE, RISK_LABEL, severityColor } from "./pewsUi";

type Filter = "all" | "medium" | "high";

/**
 * The web School-Admin PEWS surface — the admin is the owner of the whole
 * Sense → Reason → Act → Learn loop. This single workspace mirrors (and extends)
 * the mobile Early Warning + Student Signal screens:
 *
 *   • Risk bands (High / Medium / Watch) for the latest run
 *   • Filterable cohort list → click a student to drill down (PewsStudentPanel)
 *   • Effectiveness rollup (the LEARN loop) — admin-only, not on mobile yet
 *   • Config (thresholds, run frequency, AI + parent-share toggles) — admin-only
 *   • One-tap Recompute (manual run)
 *
 * Every value is a real, deterministic snapshot served by the school-scoped
 * PEWS API; AI fields render only when present.
 */
export function PewsWorkspace() {
  const [filter, setFilter] = useState<Filter>("all");
  const minLevel: PewsRiskLevel = filter === "high" ? "high" : filter === "medium" ? "medium" : "watch";
  const { data: cohort, isLoading, mutate } = usePewsCohort(minLevel);
  const { data: effectiveness, mutate: mutateEff } = usePewsEffectiveness();
  const [selected, setSelected] = useState<string | null>(null);
  const [running, setRunning] = useState(false);
  const [runMsg, setRunMsg] = useState<string | null>(null);

  const students = cohort?.students ?? [];

  async function recompute() {
    setRunning(true);
    setRunMsg(null);
    try {
      const res = await adminApi.pewsRun();
      setRunMsg(`Recompute complete — ${res.at_risk} student${res.at_risk === 1 ? "" : "s"} at risk.`);
      await Promise.all([mutate(), mutateEff()]);
    } catch {
      setRunMsg("Recompute failed. Please try again.");
    } finally {
      setRunning(false);
    }
  }

  return (
    <div className="space-y-6">
      {/* Risk band + recompute */}
      <FadeIn>
        <Card className="pb-6">
          <CardHeader
            title="Risk band"
            subtitle={cohort?.run_date ? `As of ${cohort.run_date}` : "No run recorded yet"}
            action={
              <button
                type="button"
                onClick={recompute}
                disabled={running}
                className="rounded-full bg-navy-deep px-4 py-2.5 text-[13px] font-semibold text-white shadow-pill transition-all duration-200 hover:-translate-y-0.5 hover:bg-navy disabled:cursor-not-allowed disabled:opacity-60 disabled:hover:translate-y-0"
              >
                {running ? "Recomputing…" : "Recompute now"}
              </button>
            }
          />
          <div className="grid grid-cols-3 gap-3 px-6 pt-4">
            <BandTile label="High" count={cohort?.high ?? 0} tone="danger" loading={isLoading && !cohort} />
            <BandTile label="Medium" count={cohort?.medium ?? 0} tone="warning" loading={isLoading && !cohort} />
            <BandTile label="Watch" count={cohort?.watch ?? 0} tone="mint" loading={isLoading && !cohort} />
          </div>
          <div className="mt-4 flex items-center gap-2 px-6">
            <AiStatusBadge enabled={cohort?.ai_enabled ?? false} />
            {runMsg && <span className="text-[12px] text-ink-3">{runMsg}</span>}
          </div>
        </Card>
      </FadeIn>

      {/* Filter + cohort list */}
      <FadeIn delay={0.05}>
        <Card>
          <CardHeader
            title="At-risk students"
            subtitle="Click a student to see the signals, AI explanation & interventions"
            action={
              <div className="flex gap-1.5">
                {(["all", "medium", "high"] as Filter[]).map((f) => (
                  <button
                    key={f}
                    type="button"
                    onClick={() => setFilter(f)}
                    className={`rounded-full px-3.5 py-1.5 text-[12.5px] font-semibold capitalize transition-colors ${
                      filter === f
                        ? "bg-navy-deep text-white"
                        : "bg-navy/[0.05] text-ink-2 hover:bg-navy/[0.08]"
                    }`}
                  >
                    {f === "all" ? "All" : f === "medium" ? "Medium+" : "High only"}
                  </button>
                ))}
              </div>
            }
          />
          <div className="px-3 pb-3 pt-2">
            {isLoading && !cohort ? (
              <div className="space-y-2 p-2">
                {Array.from({ length: 4 }).map((_, i) => (
                  <Skeleton key={i} className="h-16" />
                ))}
              </div>
            ) : students.length === 0 ? (
              <EmptyState
                icon={<IconShield width={26} height={26} />}
                title="No students at risk"
                hint="When attendance, marks, or leave signals cross a threshold, flagged students appear here ranked by severity."
              />
            ) : (
              <ul className="space-y-1.5">
                {students.map((s) => (
                  <li key={s.student_code}>
                    <button
                      type="button"
                      onClick={() => setSelected(s.student_code)}
                      className="flex w-full items-center gap-3 rounded-2xl px-3 py-3 text-left transition-colors hover:bg-navy/[0.035] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent/30"
                    >
                      <Avatar name={s.name} size={40} />
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center gap-2">
                          <p className="truncate text-[13.5px] font-semibold text-navy-deep">
                            {s.name}
                          </p>
                          <Badge tone={RISK_TONE[s.risk_level]}>{RISK_LABEL[s.risk_level]}</Badge>
                        </div>
                        <p className="mt-0.5 truncate text-[12px] text-ink-3">
                          Class {s.class_name}-{s.section}
                          {s.signals[0] ? ` · ${s.signals[0].label}` : ""}
                        </p>
                      </div>
                      <div className="flex shrink-0 items-center gap-1.5">
                        {s.ai_narrative && (
                          <IconSparkle width={13} height={13} className="text-accent-deep" />
                        )}
                        {s.signals.map((sig) => (
                          <span
                            key={sig.kind}
                            className="inline-block h-1.5 w-1.5 rounded-full"
                            style={{ background: severityColor(sig.severity) }}
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
        </Card>
      </FadeIn>

      {/* Effectiveness + Config */}
      <div className="grid gap-6 lg:grid-cols-2">
        <FadeIn delay={0.08}>
          <EffectivenessCard
            data={effectiveness}
            loading={!effectiveness}
          />
        </FadeIn>
        <FadeIn delay={0.1}>
          <ConfigCard />
        </FadeIn>
      </div>

      <PewsStudentPanel
        studentCode={selected}
        onClose={() => setSelected(null)}
        onMutated={() => {
          mutate();
          mutateEff();
        }}
      />
    </div>
  );
}

function AiStatusBadge({ enabled }: { enabled: boolean }) {
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-[11px] font-bold ${
        enabled ? "bg-accent/10 text-accent-deep" : "bg-navy/[0.05] text-ink-3"
      }`}
    >
      <IconSparkle width={12} height={12} />
      {enabled ? "AI reasoning on" : "AI reasoning off"}
    </span>
  );
}

const BAND_BG = {
  danger: "bg-danger/10 text-danger",
  warning: "bg-warning/12 text-warning",
  mint: "bg-success/10 text-success",
} as const;

function BandTile({
  label,
  count,
  tone,
  loading,
}: {
  label: string;
  count: number;
  tone: keyof typeof BAND_BG;
  loading: boolean;
}) {
  return (
    <div className={`rounded-2xl px-4 py-5 text-center ${BAND_BG[tone]}`}>
      {loading ? (
        <Skeleton className="mx-auto h-8 w-10 bg-white/40" />
      ) : (
        <p className="nums text-[28px] font-extrabold leading-none">{count}</p>
      )}
      <p className="mt-1.5 text-[12px] font-bold">{label}</p>
    </div>
  );
}

function EffectivenessCard({
  data,
  loading,
}: {
  data:
    | {
        total: number;
        open: number;
        done: number;
        dismissed: number;
        improved: number;
        unchanged: number;
        worsened: number;
      }
    | undefined;
  loading: boolean;
}) {
  const resolved = (data?.done ?? 0) + (data?.dismissed ?? 0);
  const improvedRate =
    data && data.improved + data.unchanged + data.worsened > 0
      ? Math.round((data.improved / (data.improved + data.unchanged + data.worsened)) * 100)
      : null;
  return (
    <Card className="h-full pb-6">
      <CardHeader
        title="Effectiveness"
        subtitle="What the intervention loop is achieving (Learn)"
      />
      {loading ? (
        <div className="space-y-3 px-6 pt-4">
          <Skeleton className="h-16" />
          <Skeleton className="h-12" />
        </div>
      ) : !data || data.total === 0 ? (
        <div className="px-6 pt-2">
          <EmptyState title="No interventions yet" hint="Once teachers open and close interventions, the outcome rollup appears here." />
        </div>
      ) : (
        <div className="space-y-4 px-6 pt-4">
          <div className="grid grid-cols-3 gap-3">
            <MiniStat label="Open" value={data.open} />
            <MiniStat label="Resolved" value={resolved} />
            <MiniStat
              label="Improved"
              value={improvedRate != null ? `${improvedRate}%` : "—"}
              tone="success"
            />
          </div>
          <div className="space-y-2">
            <OutcomeBar label="Improved" value={data.improved} total={data.total} tone="success" />
            <OutcomeBar label="No change" value={data.unchanged} total={data.total} tone="neutral" />
            <OutcomeBar label="Worsened" value={data.worsened} total={data.total} tone="danger" />
          </div>
        </div>
      )}
    </Card>
  );
}

function MiniStat({
  label,
  value,
  tone = "neutral",
}: {
  label: string;
  value: string | number;
  tone?: "neutral" | "success";
}) {
  return (
    <div className="rounded-2xl bg-navy/[0.03] px-3 py-3.5 text-center ring-1 ring-inset ring-navy/[0.05]">
      <p
        className={`nums text-[20px] font-extrabold ${
          tone === "success" ? "text-success" : "text-navy-deep"
        }`}
      >
        {value}
      </p>
      <p className="mt-0.5 text-[11px] text-ink-3">{label}</p>
    </div>
  );
}

function OutcomeBar({
  label,
  value,
  total,
  tone,
}: {
  label: string;
  value: number;
  total: number;
  tone: "success" | "neutral" | "danger";
}) {
  const pct = total > 0 ? Math.round((value / total) * 100) : 0;
  const color =
    tone === "success" ? "bg-success" : tone === "danger" ? "bg-danger" : "bg-ink-3/50";
  return (
    <div>
      <div className="mb-1 flex items-center justify-between text-[12px]">
        <span className="text-ink-2">{label}</span>
        <span className="nums font-semibold text-navy-deep">{value}</span>
      </div>
      <div className="h-2 w-full overflow-hidden rounded-full bg-navy/[0.07]">
        <div className={`h-full rounded-full ${color}`} style={{ width: `${pct}%` }} />
      </div>
    </div>
  );
}

function ConfigCard() {
  const { data, mutate } = usePewsConfig();
  const [draft, setDraft] = useState<PewsConfig | null>(null);
  const [saving, setSaving] = useState(false);
  const [savedMsg, setSavedMsg] = useState<string | null>(null);

  const cfg = draft ?? data ?? null;

  function patch(p: Partial<PewsConfig>) {
    if (!cfg) return;
    setDraft({ ...cfg, ...p });
    setSavedMsg(null);
  }

  async function save() {
    if (!cfg) return;
    setSaving(true);
    setSavedMsg(null);
    try {
      const next = await adminApi.pewsUpdateConfig(cfg);
      await mutate(next, { revalidate: false });
      setDraft(null);
      setSavedMsg("Configuration saved.");
    } catch {
      setSavedMsg("Save failed. Please try again.");
    } finally {
      setSaving(false);
    }
  }

  const dirty = !!draft && JSON.stringify(draft) !== JSON.stringify(data);

  return (
    <Card className="h-full pb-6">
      <CardHeader
        title="Configuration"
        subtitle="Thresholds, run frequency and what gets shared"
        action={
          dirty ? (
            <button
              type="button"
              onClick={save}
              disabled={saving}
              className="rounded-full bg-navy-deep px-4 py-2 text-[12.5px] font-semibold text-white shadow-pill transition-all hover:-translate-y-0.5 hover:bg-navy disabled:opacity-60"
            >
              {saving ? "Saving…" : "Save"}
            </button>
          ) : null
        }
      />
      {!cfg ? (
        <div className="space-y-3 px-6 pt-4">
          <Skeleton className="h-12" />
          <Skeleton className="h-12" />
          <Skeleton className="h-12" />
        </div>
      ) : (
        <div className="space-y-4 px-6 pt-4">
          <Toggle
            label="Relative thresholds"
            hint="Use z-scores across the cohort rather than fixed floors"
            checked={cfg.use_relative_thresholds}
            onChange={(v) => patch({ use_relative_thresholds: v })}
          />
          <NumberRow
            label="Attendance floor"
            suffix="%"
            value={cfg.attendance_floor_pct}
            onChange={(v) => patch({ attendance_floor_pct: v })}
          />
          <NumberRow
            label="Marks floor"
            suffix="%"
            value={cfg.marks_floor_pct}
            onChange={(v) => patch({ marks_floor_pct: v })}
          />
          <NumberRow
            label="Leave count floor"
            value={cfg.leave_floor_count}
            onChange={(v) => patch({ leave_floor_count: v })}
          />
          <SelectRow
            label="Run frequency"
            value={cfg.run_frequency}
            options={[
              { value: "daily", label: "Daily" },
              { value: "weekly", label: "Weekly" },
            ]}
            onChange={(v) => patch({ run_frequency: v })}
          />
          <Toggle
            label="AI narrative"
            hint="Let the LLM write a plain-language explanation of the signals"
            checked={cfg.ai_narrative_enabled}
            onChange={(v) => patch({ ai_narrative_enabled: v })}
          />
          <Toggle
            label="Share with parents"
            hint="When on, parents see a gentle, label-free nudge for their child"
            checked={cfg.parent_share_enabled}
            onChange={(v) => patch({ parent_share_enabled: v })}
          />
          {savedMsg && <p className="text-[12px] text-ink-3">{savedMsg}</p>}
        </div>
      )}
    </Card>
  );
}

function Toggle({
  label,
  hint,
  checked,
  onChange,
}: {
  label: string;
  hint?: string;
  checked: boolean;
  onChange: (v: boolean) => void;
}) {
  return (
    <div className="flex items-start justify-between gap-4">
      <div className="min-w-0">
        <p className="text-[13px] font-semibold text-navy-deep">{label}</p>
        {hint && <p className="mt-0.5 text-[11.5px] leading-snug text-ink-3">{hint}</p>}
      </div>
      <button
        type="button"
        role="switch"
        aria-checked={checked}
        onClick={() => onChange(!checked)}
        className={`relative mt-0.5 h-6 w-11 shrink-0 rounded-full transition-colors ${
          checked ? "bg-accent" : "bg-navy/15"
        }`}
      >
        <span
          className={`absolute top-0.5 h-5 w-5 rounded-full bg-white shadow transition-transform ${
            checked ? "translate-x-5" : "translate-x-0.5"
          }`}
        />
      </button>
    </div>
  );
}

function NumberRow({
  label,
  value,
  suffix,
  onChange,
}: {
  label: string;
  value: number;
  suffix?: string;
  onChange: (v: number) => void;
}) {
  return (
    <div className="flex items-center justify-between gap-4">
      <p className="text-[13px] font-semibold text-navy-deep">{label}</p>
      <div className="flex items-center gap-1.5">
        <input
          type="number"
          value={value}
          onChange={(e) => onChange(Number(e.target.value))}
          className="w-20 rounded-xl border border-navy/12 bg-white px-3 py-1.5 text-right text-[13px] font-semibold text-navy-deep focus:border-accent focus:outline-none"
        />
        {suffix && <span className="text-[12px] text-ink-3">{suffix}</span>}
      </div>
    </div>
  );
}

function SelectRow({
  label,
  value,
  options,
  onChange,
}: {
  label: string;
  value: string;
  options: { value: string; label: string }[];
  onChange: (v: string) => void;
}) {
  return (
    <div className="flex items-center justify-between gap-4">
      <p className="text-[13px] font-semibold text-navy-deep">{label}</p>
      <select
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="rounded-xl border border-navy/12 bg-white px-3 py-1.5 text-[13px] font-semibold text-navy-deep focus:border-accent focus:outline-none"
      >
        {options.map((o) => (
          <option key={o.value} value={o.value}>
            {o.label}
          </option>
        ))}
      </select>
    </div>
  );
}
