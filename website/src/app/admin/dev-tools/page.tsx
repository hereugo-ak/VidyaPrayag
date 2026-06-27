"use client";

import { useState, useCallback } from "react";
import { adminApi } from "@/lib/admin/client";
import { useAdminAuth } from "@/lib/admin/session";
import { Card, CardHeader, Badge, FadeIn, EmptyState } from "@/components/admin/Primitives";
import { AdminButton } from "@/components/admin/Toolbar";
import { IconBolt, IconCheck, IconPulse, IconMessage, IconWarning } from "@/components/admin/icons";
import type {
  OtpProvidersResponse,
  TriggerPulseResponse,
  DevSendNotificationResponse,
} from "@/lib/admin/types";

export default function DevToolsPage() {
  const { session } = useAdminAuth();
  const isSuperAdmin = session?.role === "super_admin";

  if (!isSuperAdmin) {
    return (
      <Card>
        <EmptyState
          title="Access Denied"
          hint="Dev Tools are only available to super admin accounts."
          icon={<IconWarning />}
        />
      </Card>
    );
  }

  return (
    <div className="space-y-6">
      <FadeIn>
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-accent/10 text-accent-deep">
            <IconBolt />
          </div>
          <div>
            <h1 className="text-[22px] font-bold tracking-tight text-navy-deep">Dev Tools</h1>
            <p className="text-[13px] text-ink-3">
              Developer utilities for OTP, pulse, and notifications — super admin only.
            </p>
          </div>
        </div>
      </FadeIn>

      <FadeIn delay={0.05}>
        <OtpProviderCard />
      </FadeIn>

      <FadeIn delay={0.1}>
        <PulseTriggerCard />
      </FadeIn>

      <FadeIn delay={0.15}>
        <SendNotificationCard />
      </FadeIn>
    </div>
  );
}

// ── OTP Provider Card ──────────────────────────────────────────────────────

function OtpProviderCard() {
  const [data, setData] = useState<OtpProvidersResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState("");
  const [saving, setSaving] = useState(false);
  const [msg, setMsg] = useState<{ text: string; ok: boolean } | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await adminApi.otpProviders();
      setData(res);
      setSelected(res.runtimeOverride ?? res.envPinnedProvider ?? "auto");
    } catch (e: unknown) {
      setMsg({ text: `Failed to load: ${(e as Error).message}`, ok: false });
    } finally {
      setLoading(false);
    }
  }, []);

  // Load on mount
  useState(() => { load(); });

  const handleSave = async () => {
    setSaving(true);
    setMsg(null);
    try {
      const res = await adminApi.updateOtpProvider(selected);
      setMsg({
        text: res.isOverride
          ? `Provider set to "${res.provider}" (runtime override)`
          : `Provider set to "auto" (env chain restored)`,
        ok: true,
      });
      await load();
    } catch (e: unknown) {
      setMsg({ text: `Failed: ${(e as Error).message}`, ok: false });
    } finally {
      setSaving(false);
    }
  };

  return (
    <Card>
      <CardHeader
        title="OTP Provider"
        subtitle="Switch the active OTP delivery provider at runtime. 'auto' uses the env var chain."
        action={
          data && (
            <Badge tone={data.runtimeOverride ? "warning" : "neutral"}>
              {data.runtimeOverride ? "Override active" : "Env default"}
            </Badge>
          )
        }
      />
      <div className="px-6 pb-6 pt-4">
        {loading ? (
          <p className="text-[13px] text-ink-3">Loading providers…</p>
        ) : data ? (
          <div className="space-y-4">
            {/* Provider dropdown */}
            <div>
              <label className="mb-1.5 block text-[13px] font-semibold text-ink-2">
                Active provider
              </label>
              <select
                value={selected}
                onChange={(e) => setSelected(e.target.value)}
                className="w-full max-w-sm rounded-xl border border-navy/12 bg-white/80 px-3.5 py-2.5 text-[14px] text-ink outline-none transition-colors focus:border-accent"
              >
                <option value="auto">auto (env chain)</option>
                {data.providers.map((p) => (
                  <option key={p.name} value={p.name} disabled={!p.configured}>
                    {p.name} ({p.channel}){!p.configured ? " — not configured" : ""}
                  </option>
                ))}
              </select>
            </div>

            {/* Provider status grid */}
            <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
              {data.providers.map((p) => (
                <div
                  key={p.name}
                  className="rounded-xl bg-navy/[0.03] px-3 py-2.5"
                >
                  <div className="flex items-center justify-between">
                    <span className="text-[12.5px] font-semibold text-navy-deep">{p.name}</span>
                    <Badge tone={p.configured ? "success" : "neutral"}>
                      {p.configured ? "Ready" : "Off"}
                    </Badge>
                  </div>
                  <p className="mt-0.5 text-[11px] text-ink-3">{p.channel}</p>
                </div>
              ))}
            </div>

            {/* Effective provider info */}
            <div className="rounded-xl bg-navy/[0.03] px-3.5 py-2.5">
              <p className="text-[12px] text-ink-3">
                <span className="font-semibold text-ink-2">Effective:</span>{" "}
                {data.effectiveProvider || "auto (env chain)"}
              </p>
              {data.envPinnedProvider && (
                <p className="mt-0.5 text-[12px] text-ink-3">
                  <span className="font-semibold text-ink-2">Env pin:</span> {data.envPinnedProvider}
                </p>
              )}
            </div>

            {/* Action */}
            <div className="flex items-center gap-3">
              <AdminButton onClick={handleSave} disabled={saving || selected === (data.runtimeOverride ?? data.envPinnedProvider ?? "auto")}>
                {saving ? "Saving…" : "Apply provider"}
              </AdminButton>
              {msg && (
                <span className={`text-[12.5px] font-medium ${msg.ok ? "text-success" : "text-danger"}`}>
                  {msg.text}
                </span>
              )}
            </div>
          </div>
        ) : (
          <p className="text-[13px] text-danger">Failed to load provider data.</p>
        )}
      </div>
    </Card>
  );
}

// ── Pulse Trigger Card ─────────────────────────────────────────────────────

function PulseTriggerCard() {
  const [triggering, setTriggering] = useState(false);
  const [result, setResult] = useState<TriggerPulseResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleTrigger = async () => {
    setTriggering(true);
    setError(null);
    setResult(null);
    try {
      const res = await adminApi.triggerPulse();
      setResult(res);
    } catch (e: unknown) {
      setError((e as Error).message);
    } finally {
      setTriggering(false);
    }
  };

  return (
    <Card>
      <CardHeader
        title="Weekly Pulse Generator"
        subtitle="Manually trigger parent pulse generation for the current week without waiting for the Sunday cron."
        action={<IconPulse />}
      />
      <div className="px-6 pb-6 pt-4">
        <div className="flex items-center gap-3">
          <AdminButton onClick={handleTrigger} disabled={triggering}>
            {triggering ? "Generating…" : "Trigger now"}
          </AdminButton>
          {result && (
            <div className="flex items-center gap-2">
              <span className="flex h-5 w-5 items-center justify-center rounded-full bg-success/10 text-success">
                <IconCheck width={14} height={14} />
              </span>
              <span className="text-[12.5px] font-medium text-ink-2">
                {result.pulsesGenerated} pulses generated for week of {result.weekStart}
              </span>
            </div>
          )}
          {error && (
            <span className="text-[12.5px] font-medium text-danger">{error}</span>
          )}
        </div>
      </div>
    </Card>
  );
}

// ── Send Notification Card ─────────────────────────────────────────────────

function SendNotificationCard() {
  const [userId, setUserId] = useState("");
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const [deepLink, setDeepLink] = useState("");
  const [category, setCategory] = useState("dev_tools");
  const [sending, setSending] = useState(false);
  const [result, setResult] = useState<DevSendNotificationResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSending(true);
    setError(null);
    setResult(null);
    try {
      const res = await adminApi.devSendNotification({
        user_id: userId,
        title,
        body,
        deep_link: deepLink || undefined,
        category: category || "dev_tools",
      });
      setResult(res);
      setUserId("");
      setTitle("");
      setBody("");
      setDeepLink("");
    } catch (e: unknown) {
      setError((e as Error).message);
    } finally {
      setSending(false);
    }
  };

  return (
    <Card>
      <CardHeader
        title="Send Notification"
        subtitle="Send an in-app + push notification to any user from the web."
        action={<IconMessage />}
      />
      <div className="px-6 pb-6 pt-4">
        <form onSubmit={handleSubmit} className="space-y-3">
          <div>
            <label className="mb-1 block text-[13px] font-semibold text-ink-2">User ID (UUID)</label>
            <input
              type="text"
              value={userId}
              onChange={(e) => setUserId(e.target.value)}
              placeholder="e.g. 550e8400-e29b-41d4-a716-446655440000"
              required
              className="w-full rounded-xl border border-navy/12 bg-white/80 px-3.5 py-2.5 text-[14px] text-ink outline-none transition-colors focus:border-accent"
            />
          </div>
          <div>
            <label className="mb-1 block text-[13px] font-semibold text-ink-2">Title</label>
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="Notification title"
              required
              className="w-full rounded-xl border border-navy/12 bg-white/80 px-3.5 py-2.5 text-[14px] text-ink outline-none transition-colors focus:border-accent"
            />
          </div>
          <div>
            <label className="mb-1 block text-[13px] font-semibold text-ink-2">Body</label>
            <textarea
              value={body}
              onChange={(e) => setBody(e.target.value)}
              placeholder="Notification message"
              required
              rows={3}
              className="w-full rounded-xl border border-navy/12 bg-white/80 px-3.5 py-2.5 text-[14px] text-ink outline-none transition-colors focus:border-accent"
            />
          </div>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <label className="mb-1 block text-[13px] font-semibold text-ink-2">Category (optional)</label>
              <input
                type="text"
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                placeholder="dev_tools"
                className="w-full rounded-xl border border-navy/12 bg-white/80 px-3.5 py-2.5 text-[14px] text-ink outline-none transition-colors focus:border-accent"
              />
            </div>
            <div>
              <label className="mb-1 block text-[13px] font-semibold text-ink-2">Deep link (optional)</label>
              <input
                type="text"
                value={deepLink}
                onChange={(e) => setDeepLink(e.target.value)}
                placeholder="/calendar/event/123"
                className="w-full rounded-xl border border-navy/12 bg-white/80 px-3.5 py-2.5 text-[14px] text-ink outline-none transition-colors focus:border-accent"
              />
            </div>
          </div>
          <div className="flex items-center gap-3 pt-1">
            <AdminButton type="submit" disabled={sending || !userId || !title || !body}>
              {sending ? "Sending…" : "Send notification"}
            </AdminButton>
            {result && (
              <div className="flex items-center gap-2">
                <span className="flex h-5 w-5 items-center justify-center rounded-full bg-success/10 text-success">
                  <IconCheck width={14} height={14} />
                </span>
                <span className="text-[12.5px] font-medium text-ink-2">Notification sent</span>
              </div>
            )}
            {error && (
              <span className="text-[12.5px] font-medium text-danger">{error}</span>
            )}
          </div>
        </form>
      </div>
    </Card>
  );
}
