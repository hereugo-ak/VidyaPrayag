"use client";

import { useEffect, useState } from "react";
import { mutate } from "swr";
import { useSchoolProfile } from "@/lib/admin/hooks";
import { adminApi } from "@/lib/admin/client";
import { ApiError } from "@/lib/api";
import type { SchoolProfileDto, UpdateSchoolProfileRequest } from "@/lib/admin/types";
import { Card, CardHeader, FadeIn, Skeleton } from "@/components/admin/Primitives";
import { AdminButton } from "@/components/admin/Toolbar";

/** Only these fields are editable from the web settings surface. */
type EditableKey =
  | "name"
  | "contact_phone"
  | "contact_email"
  | "principal_name"
  | "principal_phone"
  | "principal_email"
  | "full_address"
  | "city"
  | "district"
  | "state"
  | "pincode"
  | "website"
  | "brand_color";

const FIELDS: { key: EditableKey; label: string; type?: string; span?: boolean }[] = [
  { key: "name", label: "School name", span: true },
  { key: "contact_email", label: "Contact email", type: "email" },
  { key: "contact_phone", label: "Contact phone" },
  { key: "principal_name", label: "Principal name" },
  { key: "principal_email", label: "Principal email", type: "email" },
  { key: "principal_phone", label: "Principal phone" },
  { key: "website", label: "Website" },
  { key: "full_address", label: "Full address", span: true },
  { key: "city", label: "City" },
  { key: "district", label: "District" },
  { key: "state", label: "State" },
  { key: "pincode", label: "Pincode" },
];

export default function SettingsPage() {
  const { data, isLoading } = useSchoolProfile();
  const [form, setForm] = useState<Partial<Record<EditableKey, string>>>({});
  const [dirty, setDirty] = useState(false);
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState<string | null>(null);
  const [err, setErr] = useState<string | null>(null);

  // Seed the form once real data arrives (and whenever it's refetched while clean).
  useEffect(() => {
    if (data && !dirty) {
      const seed: Partial<Record<EditableKey, string>> = {};
      FIELDS.forEach((f) => {
        const v = (data as SchoolProfileDto)[f.key];
        seed[f.key] = v == null ? "" : String(v);
      });
      seed.brand_color = data.brand_color ?? "#6C5CE0";
      setForm(seed);
    }
  }, [data, dirty]);

  function set(key: EditableKey, value: string) {
    setDirty(true);
    setMsg(null);
    setForm((f) => ({ ...f, [key]: value }));
  }

  async function save() {
    setErr(null);
    setMsg(null);
    setBusy(true);
    try {
      const payload: UpdateSchoolProfileRequest = {};
      (Object.keys(form) as EditableKey[]).forEach((k) => {
        payload[k] = form[k] ?? "";
      });
      await adminApi.updateSchoolProfile(payload);
      await mutate("school/profile");
      setDirty(false);
      setMsg("Saved.");
    } catch (e) {
      setErr(e instanceof ApiError ? e.message : "Could not save changes.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="max-w-3xl space-y-6">
      <FadeIn>
        <Card>
          <CardHeader
            title="Institutional profile"
            subtitle="The details parents and teachers see across your school's apps."
            action={
              <AdminButton onClick={save} disabled={busy || !dirty}>
                {busy ? "Saving…" : dirty ? "Save changes" : "Saved"}
              </AdminButton>
            }
          />

          <div className="p-5">
            {isLoading && !data ? (
              <div className="grid gap-4 sm:grid-cols-2">
                {Array.from({ length: 8 }).map((_, i) => <Skeleton key={i} className="h-16" />)}
              </div>
            ) : (
              <>
                <div className="grid gap-4 sm:grid-cols-2">
                  {FIELDS.map((f) => (
                    <label key={f.key} className={`block ${f.span ? "sm:col-span-2" : ""}`}>
                      <span className="mb-1.5 block text-[11px] font-semibold uppercase tracking-wide text-ink-3">
                        {f.label}
                      </span>
                      <input
                        type={f.type ?? "text"}
                        value={form[f.key] ?? ""}
                        onChange={(e) => set(f.key, e.target.value)}
                        className="w-full rounded-xl border border-navy/12 bg-white/80 px-3.5 py-2.5 text-[14px] text-ink outline-none transition-colors duration-200 focus:border-accent focus:bg-white"
                      />
                    </label>
                  ))}

                  <label className="block">
                    <span className="mb-1.5 block text-[11px] font-semibold uppercase tracking-wide text-ink-3">
                      Brand colour
                    </span>
                    <div className="flex items-center gap-3">
                      <input
                        type="color"
                        value={form.brand_color ?? "#6C5CE0"}
                        onChange={(e) => set("brand_color", e.target.value)}
                        className="h-10 w-12 cursor-pointer rounded-lg border border-navy/15 bg-white p-1"
                      />
                      <span className="font-mono text-[13px] text-ink-2">{form.brand_color}</span>
                    </div>
                  </label>
                </div>

                <div className="mt-5 flex items-center gap-3">
                  {msg && <p className="text-[13px] font-medium text-teal-deep">{msg}</p>}
                  {err && <p className="text-[13px] font-medium text-danger">{err}</p>}
                </div>
              </>
            )}
          </div>
        </Card>
      </FadeIn>

      {/* Read-only institutional facts the web surface doesn't edit. */}
      {data && (
        <FadeIn delay={0.06}>
          <Card>
            <CardHeader title="Configuration" subtitle="Set during onboarding — manage in the mobile admin app." />
            <dl className="grid grid-cols-2 gap-px overflow-hidden rounded-b-2xl bg-navy/6 sm:grid-cols-3">
              <Fact label="Board" value={data.board} />
              <Fact label="Medium" value={data.medium} />
              <Fact label="Type" value={data.school_gender?.replace("_", "-")} />
              <Fact label="Affiliation no." value={data.affiliation_number ?? "—"} />
              <Fact label="Established" value={data.year_established ? String(data.year_established) : "—"} />
              <Fact label="Grading" value={data.grading_system ?? "—"} />
            </dl>
          </Card>
        </FadeIn>
      )}
    </div>
  );
}

function Fact({ label, value }: { label: string; value: string }) {
  return (
    <div className="bg-white px-5 py-4">
      <dt className="text-[11px] font-semibold uppercase tracking-wide text-ink-3">{label}</dt>
      <dd className="mt-1 text-[14px] font-semibold text-navy-deep">{value || "—"}</dd>
    </div>
  );
}
