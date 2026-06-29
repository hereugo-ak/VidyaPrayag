"use client";

import { useMemo, useRef, useEffect, useState } from "react";
import {
  Card,
  CardHeader,
  Badge,
  ProgressBar,
  FadeIn,
  EmptyState,
  Skeleton,
} from "@/components/admin/Primitives";
import { useAiRateLimits, useAiHealth, useAiRecentUsage } from "@/lib/admin/hooks";
import type {
  AiRateLimitEntry,
  AiHealthEntry,
  AiRecentUsageEntry,
} from "@/lib/admin/types";

// ── Helpers ──────────────────────────────────────────────────────────────────

function fmtTokens(n: number): string {
  if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
  if (n >= 1_000) return `${(n / 1_000).toFixed(1)}K`;
  return String(n);
}

function fmtPct(current: number, limit: number): number {
  if (limit <= 0) return 0;
  return Math.min(100, Math.round((current / limit) * 100));
}

function timeAgo(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime();
  if (diff < 5_000) return "just now";
  if (diff < 60_000) return `${Math.floor(diff / 1000)}s ago`;
  if (diff < 3_600_000) return `${Math.floor(diff / 60_000)}m ago`;
  return `${Math.floor(diff / 3_600_000)}h ago`;
}

const STATUS_TONE: Record<string, "success" | "warning" | "danger" | "neutral"> = {
  success: "success",
  cached: "success",
  failed: "danger",
  guardrail_blocked: "warning",
};

const CIRCUIT_TONE: Record<string, "success" | "warning" | "danger"> = {
  closed: "success",
  open: "danger",
  half_open: "warning",
};

// ── Main Component ───────────────────────────────────────────────────────────

export function AiTokenMonitor() {
  const { data: rateLimits, isLoading: rlLoading } = useAiRateLimits();
  const { data: health, isLoading: hLoading } = useAiHealth();
  const { data: usage, isLoading: uLoading } = useAiRecentUsage(50, 60);

  return (
    <div className="space-y-5">
      {/* Provider Rate-Limit Gauges */}
      <FadeIn>
        <Card>
          <CardHeader
            title="AI Provider Rate Limits"
            subtitle="Real-time RPM / RPD / TPM usage per provider — 10% reserve held back. Updates every 10s."
            action={
              <Badge tone="accent">
                {rateLimits?.length ?? 0} providers
              </Badge>
            }
          />
          <div className="px-6 pb-6 pt-4">
            {rlLoading ? (
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
                {[1, 2, 3].map((i) => (
                  <Skeleton key={i} className="h-36 rounded-2xl" />
                ))}
              </div>
            ) : rateLimits && rateLimits.length > 0 ? (
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
                {rateLimits.map((rl) => (
                  <ProviderGauge key={`${rl.provider}-${rl.model}`} entry={rl} />
                ))}
              </div>
            ) : (
              <EmptyState
                title="No rate-limit data yet"
                hint="Rate limiter tracks usage once AI calls start flowing."
                icon={<span className="text-[20px]">📊</span>}
              />
            )}
          </div>
        </Card>
      </FadeIn>

      {/* Circuit Breaker Health */}
      <FadeIn delay={0.05}>
        <Card>
          <CardHeader
            title="Circuit Breaker Health"
            subtitle="Per-provider circuit state — CLOSED (healthy), OPEN (skipped for 30s), HALF_OPEN (trial)."
            action={
              <Badge tone="neutral">
                {health?.length ?? 0} circuits
              </Badge>
            }
          />
          <div className="px-6 pb-6 pt-4">
            {hLoading ? (
              <Skeleton className="h-20 rounded-2xl" />
            ) : health && health.length > 0 ? (
              <div className="grid grid-cols-1 gap-2 sm:grid-cols-2 lg:grid-cols-3">
                {health.map((h) => (
                  <CircuitRow key={`${h.provider}-${h.model}`} entry={h} />
                ))}
              </div>
            ) : (
              <EmptyState
                title="No circuit data"
                hint="Circuits initialize on first AI call."
                icon={<span className="text-[20px]">🔌</span>}
              />
            )}
          </div>
        </Card>
      </FadeIn>

      {/* Live Usage Log Feed */}
      <FadeIn delay={0.1}>
        <Card>
          <CardHeader
            title="Live AI Usage Log"
            subtitle="Real-time stream of AI calls — feature, provider, tokens, status. Updates every 10s."
            action={
              <div className="flex items-center gap-2">
                <span className="relative flex h-2 w-2">
                  <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-success/60" />
                  <span className="relative inline-flex h-2 w-2 rounded-full bg-success" />
                </span>
                <span className="text-[11px] font-semibold text-ink-3">LIVE</span>
              </div>
            }
          />
          <div className="px-6 pb-6 pt-4">
            {uLoading ? (
              <Skeleton className="h-64 rounded-2xl" />
            ) : usage && usage.entries.length > 0 ? (
              <UsageLogFeed entries={usage.entries} windowMin={usage.window_min} />
            ) : (
              <EmptyState
                title="No AI calls in the last hour"
                hint="Usage log entries will appear here as AI features are used."
                icon={<span className="text-[20px]">📜</span>}
              />
            )}
          </div>
        </Card>
      </FadeIn>
    </div>
  );
}

// ── Provider Gauge ───────────────────────────────────────────────────────────

function ProviderGauge({ entry }: { entry: AiRateLimitEntry }) {
  const rpmPct = fmtPct(entry.rpm_current, entry.rpm_limit);
  const rpdPct = fmtPct(entry.rpd_current, entry.rpd_limit);
  const tpmPct = fmtPct(entry.tpm_current, entry.tpm_limit);
  const hasRpm = entry.rpm_limit > 0;
  const hasRpd = entry.rpd_limit > 0;
  const hasTpm = entry.tpm_limit > 0;

  const anyNearLimit =
    (hasRpm && rpmPct >= 80) ||
    (hasRpd && rpdPct >= 80) ||
    (hasTpm && tpmPct >= 80);

  const barTone = (pct: number) =>
    pct >= 90 ? "peach" : pct >= 70 ? "sky" : "accent";

  return (
    <div className={`rounded-2xl border p-4 transition-colors ${
      anyNearLimit ? "border-peach/30 bg-peach/[0.03]" : "border-navy/8 bg-navy/[0.02]"
    }`}>
      <div className="mb-3 flex items-center justify-between">
        <div>
          <span className="text-[13px] font-bold capitalize text-navy-deep">
            {entry.provider.replace("_", " ")}
          </span>
          <p className="text-[10.5px] text-ink-3">{entry.model}</p>
        </div>
        {anyNearLimit && (
          <Badge tone="warning">Near limit</Badge>
        )}
      </div>

      <div className="space-y-2.5">
        {/* RPM */}
        <div>
          <div className="mb-1 flex items-center justify-between text-[11px]">
            <span className="font-semibold text-ink-2">RPM</span>
            <span className="text-ink-3">
              {entry.rpm_current} / {hasRpm ? entry.rpm_limit : "∞"}
            </span>
          </div>
          {hasRpm && <ProgressBar value={rpmPct} tone={barTone(rpmPct)} />}
          {!hasRpm && <div className="h-1.5 rounded-full bg-navy/8" />}
        </div>

        {/* RPD */}
        <div>
          <div className="mb-1 flex items-center justify-between text-[11px]">
            <span className="font-semibold text-ink-2">RPD (daily)</span>
            <span className="text-ink-3">
              {entry.rpd_current} / {hasRpd ? entry.rpd_limit : "∞"}
            </span>
          </div>
          {hasRpd && <ProgressBar value={rpdPct} tone={barTone(rpdPct)} />}
          {!hasRpd && <div className="h-1.5 rounded-full bg-navy/8" />}
        </div>

        {/* TPM */}
        <div>
          <div className="mb-1 flex items-center justify-between text-[11px]">
            <span className="font-semibold text-ink-2">TPM</span>
            <span className="text-ink-3">
              {fmtTokens(entry.tpm_current)} / {hasTpm ? fmtTokens(entry.tpm_limit) : "∞"}
            </span>
          </div>
          {hasTpm && <ProgressBar value={tpmPct} tone={barTone(tpmPct)} />}
          {!hasTpm && <div className="h-1.5 rounded-full bg-navy/8" />}
        </div>
      </div>

      <div className="mt-3 border-t border-navy/6 pt-2">
        <span className="text-[10px] text-ink-3">
          {entry.reserve_pct}% reserve held
        </span>
      </div>
    </div>
  );
}

// ── Circuit Breaker Row ──────────────────────────────────────────────────────

function CircuitRow({ entry }: { entry: AiHealthEntry }) {
  const tone = CIRCUIT_TONE[entry.state] ?? "neutral";
  const failureRate = entry.total_requests > 0
    ? Math.round((entry.total_failures / entry.total_requests) * 100)
    : 0;

  return (
    <div className="rounded-xl bg-navy/[0.03] px-3.5 py-3">
      <div className="flex items-center justify-between">
        <div>
          <span className="text-[12.5px] font-bold capitalize text-navy-deep">
            {entry.provider.replace("_", " ")}
          </span>
          <p className="text-[10px] text-ink-3">{entry.model}</p>
        </div>
        <Badge tone={tone}>{entry.state.replace("_", " ")}</Badge>
      </div>
      <div className="mt-2 flex items-center gap-3 text-[10.5px] text-ink-3">
        <span>{entry.total_requests} reqs</span>
        <span>·</span>
        <span>{failureRate}% fail</span>
        <span>·</span>
        <span>{entry.rate_limit_hits} × 429</span>
        <span>·</span>
        <span>{entry.avg_latency_ms}ms avg</span>
      </div>
    </div>
  );
}

// ── Live Usage Log Feed ──────────────────────────────────────────────────────

function UsageLogFeed({ entries, windowMin }: { entries: AiRecentUsageEntry[]; windowMin: number }) {
  const scrollRef = useRef<HTMLDivElement>(null);
  const [autoScroll, setAutoScroll] = useState(true);

  // Track previous entry IDs to highlight new entries
  const prevIds = useRef<Set<string>>(new Set());
  const newIds = useMemo(() => {
    const currentIds = new Set(entries.map((e) => e.id));
    const fresh = new Set<string>();
    for (const e of entries) {
      if (!prevIds.current.has(e.id)) fresh.add(e.id);
    }
    prevIds.current = currentIds;
    return fresh;
  }, [entries]);

  // Auto-scroll to top (newest) when new entries arrive if autoScroll is on
  useEffect(() => {
    if (autoScroll && scrollRef.current) {
      scrollRef.current.scrollTop = 0;
    }
  }, [entries, autoScroll]);

  const handleScroll = () => {
    if (!scrollRef.current) return;
    const el = scrollRef.current;
    // If user scrolls down more than 50px from top, pause auto-scroll
    setAutoScroll(el.scrollTop < 50);
  };

  return (
    <div>
      <div className="mb-2 flex items-center justify-between">
        <span className="text-[11px] text-ink-3">
          {entries.length} calls in last {windowMin}min
        </span>
        {!autoScroll && (
          <button
            onClick={() => {
              setAutoScroll(true);
              if (scrollRef.current) scrollRef.current.scrollTop = 0;
            }}
            className="text-[11px] font-semibold text-accent hover:text-accent-deep"
          >
            ↑ Back to latest
          </button>
        )}
      </div>
      <div
        ref={scrollRef}
        onScroll={handleScroll}
        className="max-h-[420px] space-y-1.5 overflow-y-auto rounded-2xl bg-navy/[0.02] p-2 [scrollbar-width:thin]"
      >
        {entries.map((e) => {
          const isNew = newIds.has(e.id);
          const totalTokens = e.input_tokens + e.output_tokens;
          const tone = STATUS_TONE[e.status] ?? "neutral";
          return (
            <div
              key={e.id}
              className={`flex items-start gap-3 rounded-xl px-3 py-2.5 transition-colors ${
                isNew ? "bg-accent/[0.06] ring-1 ring-accent/20" : "hover:bg-navy/[0.03]"
              }`}
            >
              {/* Status dot */}
              <span
                className={`mt-1 h-2 w-2 flex-shrink-0 rounded-full ${
                  e.status === "success"
                    ? "bg-success"
                    : e.status === "cached"
                    ? "bg-accent"
                    : e.status === "failed"
                    ? "bg-danger"
                    : "bg-warning"
                }`}
              />

              {/* Content */}
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2">
                  <span className="text-[12px] font-bold text-navy-deep">
                    {e.feature}
                  </span>
                  <Badge tone={tone} className="!px-1.5 !py-0 !text-[9.5px]">
                    {e.status}
                  </Badge>
                  {e.routing_decision !== "direct" && (
                    <span className="text-[10px] font-medium text-ink-3">
                      → {e.routing_decision.replace("_", " ")}
                    </span>
                  )}
                  <span className="ml-auto text-[10px] text-ink-3">
                    {timeAgo(e.created_at)}
                  </span>
                </div>
                <div className="mt-0.5 flex items-center gap-2.5 text-[10.5px] text-ink-3">
                  <span className="font-medium text-ink-2">
                    {e.provider_used ?? "—"}
                  </span>
                  <span>·</span>
                  <span>{fmtTokens(e.input_tokens)} in / {fmtTokens(e.output_tokens)} out</span>
                  <span>·</span>
                  <span>{e.latency_ms}ms</span>
                  {e.error_message && (
                    <>
                      <span>·</span>
                      <span className="truncate text-danger">
                        {e.error_message.slice(0, 60)}
                      </span>
                    </>
                  )}
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
