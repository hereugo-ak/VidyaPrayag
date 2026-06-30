"use client";

import { Suspense, useState } from "react";
import { useSearchParams } from "next/navigation";
import { mutate } from "swr";
import { useAnnouncements } from "@/lib/admin/hooks";
import { adminApi } from "@/lib/admin/client";
import { ApiError } from "@/lib/api";
import type { AnnouncementDto } from "@/lib/admin/types";
import { Card, EmptyState, FadeIn, Skeleton, Badge } from "@/components/admin/Primitives";
import { Modal, AdminButton } from "@/components/admin/Toolbar";
import { IconPlus, IconAnnounce } from "@/components/admin/icons";

const TYPES = [
  { value: "announcement", label: "Announcement" },
  { value: "event", label: "Event" },
  { value: "holiday", label: "Holiday" },
];

export default function AnnouncementsPage() {
  return (
    <Suspense fallback={<div className="h-40 animate-pulse rounded-2xl bg-navy/5" />}>
      <AnnouncementsInner />
    </Suspense>
  );
}

function AnnouncementsInner() {
  const params = useSearchParams();
  const { data, isLoading } = useAnnouncements();
  const list = data?.announcements ?? [];
  const [open, setOpen] = useState(params.get("new") === "1");

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-end">
        <AdminButton onClick={() => setOpen(true)}>
          <IconPlus width={16} height={16} /> New announcement
        </AdminButton>
      </div>

      {isLoading && !data ? (
        <div className="grid gap-4 md:grid-cols-2">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-32" />
          ))}
        </div>
      ) : list.length === 0 ? (
        <Card>
          <EmptyState
            icon={<IconAnnounce width={26} height={26} />}
            title="No announcements yet"
            hint="Post your first update, it reaches parents and teachers instantly."
          />
        </Card>
      ) : (
        <div className="grid gap-4 md:grid-cols-2">
          {list.map((a, i) => (
            <FadeIn key={a.event_id} delay={Math.min(i * 0.03, 0.2)}>
              <AnnouncementCard a={a} />
            </FadeIn>
          ))}
        </div>
      )}

      <CreateAnnouncementModal
        open={open}
        onClose={() => setOpen(false)}
        onDone={() => mutate("announcements")}
      />
    </div>
  );
}

function AnnouncementCard({ a }: { a: AnnouncementDto }) {
  return (
    <Card className="flex h-full flex-col p-5">
      <div className="mb-2 flex items-center justify-between gap-3">
        <Badge tone="accent">{a.type}</Badge>
        <span className="text-[12px] text-ink-3">{a.date}</span>
      </div>
      <h3 className="text-[15px] font-bold text-navy-deep">{a.title}</h3>
      {a.sub_title && <p className="mt-0.5 text-[13px] font-medium text-ink-2">{a.sub_title}</p>}
      <p className="mt-2 line-clamp-4 text-[13px] leading-relaxed text-ink-2">{a.description}</p>
      <div className="mt-auto pt-3">
        <span className="text-[11px] uppercase tracking-wide text-ink-3">
          To: {a.audience_type || "everyone"}
        </span>
      </div>
    </Card>
  );
}

function CreateAnnouncementModal({
  open,
  onClose,
  onDone,
}: {
  open: boolean;
  onClose: () => void;
  onDone: () => void;
}) {
  const [type, setType] = useState("announcement");
  const [title, setTitle] = useState("");
  const [sub_title, setSub] = useState("");
  const [description, setDesc] = useState("");
  const [date, setDate] = useState(() => new Date().toISOString().slice(0, 10));
  const [audience_type, setAudience] = useState("all");
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const [scheduleEnabled, setScheduleEnabled] = useState(false);
  const [scheduleDate, setScheduleDate] = useState(() => new Date().toISOString().slice(0, 10));
  const [scheduleTime, setScheduleTime] = useState("09:00");

  function reset() {
    setType("announcement"); setTitle(""); setSub(""); setDesc("");
    setDate(new Date().toISOString().slice(0, 10)); setAudience("all"); setErr(null);
    setScheduleEnabled(false);
    setScheduleDate(new Date().toISOString().slice(0, 10));
    setScheduleTime("09:00");
  }

  async function submit() {
    setErr(null);
    if (!title.trim() || !description.trim()) {
      setErr("A title and description are required.");
      return;
    }
    setBusy(true);
    try {
      const scheduled_at = scheduleEnabled
        ? `${scheduleDate}T${scheduleTime}:00Z`
        : null;
      await adminApi.createAnnouncement({
        type,
        title: title.trim(),
        sub_title: sub_title.trim() || null,
        description: description.trim(),
        date,
        audience_type,
        scheduled_at,
      });
      onDone();
      reset();
      onClose();
    } catch (e) {
      setErr(e instanceof ApiError ? e.message : "Could not post announcement.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="New announcement"
      description={scheduleEnabled ? "Schedule this announcement for a future date and time." : "Posts immediately to your selected audience."}
      size="lg"
      footer={
        <>
          <AdminButton variant="ghost" onClick={onClose}>Cancel</AdminButton>
          <AdminButton onClick={submit} disabled={busy}>
            {busy ? "Sending…" : scheduleEnabled ? "Schedule" : "Post"}
          </AdminButton>
        </>
      }
    >
      <div className="grid gap-3.5">
        <div className="grid grid-cols-2 gap-3.5">
          <Select label="Type" value={type} onChange={setType} options={TYPES} />
          <Field label="Date" value={date} onChange={setDate} type="date" />
        </div>
        <Field label="Title" value={title} onChange={setTitle} />
        <Field label="Subtitle (optional)" value={sub_title} onChange={setSub} />
        <label className="block">
          <span className="mb-1.5 block text-[11px] font-semibold uppercase tracking-wide text-ink-3">Description</span>
          <textarea
            value={description}
            onChange={(e) => setDesc(e.target.value)}
            rows={4}
            className="w-full rounded-xl border border-navy/12 bg-white/80 p-3 text-[14px] text-ink outline-none focus:border-accent focus:bg-white"
          />
        </label>
        <Select
          label="Audience"
          value={audience_type}
          onChange={setAudience}
          options={[
            { value: "all", label: "Everyone" },
            { value: "parents", label: "Parents" },
            { value: "teachers", label: "Teachers" },
          ]}
        />
        {err && <p className="text-[13px] font-medium text-danger">{err}</p>}
        <div className="rounded-xl border border-navy/12 bg-white/60 p-3.5">
          <label className="flex items-center gap-2.5 cursor-pointer">
            <input
              type="checkbox"
              checked={scheduleEnabled}
              onChange={(e) => setScheduleEnabled(e.target.checked)}
              className="h-4 w-4 rounded accent-[#6C5CE0]"
            />
            <span className="text-[13px] font-semibold text-navy-deep">Schedule for later</span>
          </label>
          {scheduleEnabled && (
            <div className="mt-3 grid grid-cols-2 gap-3">
              <Field label="Schedule date" value={scheduleDate} onChange={setScheduleDate} type="date" />
              <Field label="Schedule time" value={scheduleTime} onChange={setScheduleTime} type="time" />
            </div>
          )}
        </div>
      </div>
    </Modal>
  );
}

function Field({
  label,
  value,
  onChange,
  type = "text",
}: {
  label: string;
  value: string;
  onChange: (v: string) => void;
  type?: string;
}) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-[11px] font-semibold uppercase tracking-wide text-ink-3">{label}</span>
      <input
        type={type}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="w-full rounded-xl border border-navy/12 bg-white/80 px-3.5 py-2.5 text-[14px] text-ink outline-none focus:border-accent focus:bg-white"
      />
    </label>
  );
}

function Select({
  label,
  value,
  onChange,
  options,
}: {
  label: string;
  value: string;
  onChange: (v: string) => void;
  options: { value: string; label: string }[];
}) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-[11px] font-semibold uppercase tracking-wide text-ink-3">{label}</span>
      <select
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="w-full rounded-xl border border-navy/12 bg-white/80 px-3 py-2.5 text-[14px] text-ink outline-none focus:border-accent focus:bg-white"
      >
        {options.map((o) => (
          <option key={o.value} value={o.value}>{o.label}</option>
        ))}
      </select>
    </label>
  );
}
