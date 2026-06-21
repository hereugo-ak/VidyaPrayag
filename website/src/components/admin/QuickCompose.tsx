"use client";

import { useState } from "react";
import { adminApi } from "@/lib/admin/client";
import { SidePanel } from "./SidePanel";

/**
 * Quick compose, post an announcement without leaving the dashboard. Submits
 * to the real POST /api/v1/school/announcements endpoint (school-scoped on the
 * server from the JWT). On success it calls onPosted() so the parent can
 * revalidate the activity feed immediately.
 */
const TYPES = ["Events", "Holidays", "PTM", "Special", "Reminder"];
const AUDIENCES = [
  { value: "ALL_SCHOOL", label: "Whole school" },
  { value: "CLASS", label: "A specific class" },
];

export function QuickCompose({
  open,
  onClose,
  onPosted,
}: {
  open: boolean;
  onClose: () => void;
  onPosted: () => void;
}) {
  const [type, setType] = useState("Events");
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const [audience, setAudience] = useState("ALL_SCHOOL");
  const [className, setClassName] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  function reset() {
    setTitle("");
    setBody("");
    setAudience("ALL_SCHOOL");
    setClassName("");
    setError(null);
  }

  async function submit() {
    if (!title.trim() || !body.trim()) {
      setError("Add a title and a message.");
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await adminApi.createAnnouncement({
        type,
        title: title.trim(),
        description: body.trim(),
        date: new Date().toISOString().slice(0, 10),
        audience_type: audience,
        audience_filter:
          audience === "CLASS" && className.trim()
            ? { class_name: className.trim() }
            : undefined,
      });
      reset();
      onPosted();
      onClose();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Could not post. Try again.");
    } finally {
      setBusy(false);
    }
  }

  const labelCls = "mb-1.5 block text-[12px] font-semibold text-navy-deep";
  const fieldCls =
    "w-full rounded-xl border border-navy/12 bg-white/80 px-3.5 py-2.5 text-[14px] text-navy-deep transition-colors placeholder:text-ink-placeholder focus:border-accent/50 focus:outline-none focus:ring-2 focus:ring-accent/20";

  return (
    <SidePanel open={open} onClose={onClose} title="New announcement" subtitle="Posts instantly to your school">
      <div className="space-y-4">
        <div>
          <label className={labelCls}>Type</label>
          <div className="flex flex-wrap gap-2">
            {TYPES.map((t) => (
              <button
                key={t}
                onClick={() => setType(t)}
                className={`rounded-full px-3 py-1.5 text-[12px] font-semibold transition-colors ${
                  type === t
                    ? "bg-navy-deep text-white"
                    : "bg-navy/6 text-ink-2 hover:bg-navy/10"
                }`}
              >
                {t}
              </button>
            ))}
          </div>
        </div>

        <div>
          <label htmlFor="qc-title" className={labelCls}>Title</label>
          <input
            id="qc-title"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="Parent-teacher meeting on Friday"
            className={fieldCls}
          />
        </div>

        <div>
          <label htmlFor="qc-body" className={labelCls}>Message</label>
          <textarea
            id="qc-body"
            value={body}
            onChange={(e) => setBody(e.target.value)}
            rows={5}
            placeholder="Write the details parents and staff should see…"
            className={`${fieldCls} resize-none`}
          />
        </div>

        <div>
          <label className={labelCls}>Audience</label>
          <div className="flex gap-2">
            {AUDIENCES.map((a) => (
              <button
                key={a.value}
                onClick={() => setAudience(a.value)}
                className={`flex-1 rounded-xl border px-3 py-2.5 text-[13px] font-semibold transition-colors ${
                  audience === a.value
                    ? "border-accent/40 bg-accent/8 text-accent-deep"
                    : "border-navy/10 bg-white/60 text-ink-2 hover:border-navy/20"
                }`}
              >
                {a.label}
              </button>
            ))}
          </div>
          {audience === "CLASS" && (
            <input
              value={className}
              onChange={(e) => setClassName(e.target.value)}
              placeholder="Class name, e.g. Grade 5"
              className={`${fieldCls} mt-2`}
            />
          )}
        </div>

        {error && (
          <p className="rounded-lg bg-danger/8 px-3 py-2 text-[13px] font-medium text-danger">{error}</p>
        )}

        <button
          onClick={submit}
          disabled={busy}
          className="w-full rounded-xl bg-navy-deep px-4 py-3 text-[14px] font-semibold text-white transition-all duration-200 hover:bg-navy disabled:cursor-not-allowed disabled:opacity-60"
        >
          {busy ? "Posting…" : "Post announcement"}
        </button>
      </div>
    </SidePanel>
  );
}
