"use client";

import { useState, useEffect, useCallback } from "react";
import { mutate } from "swr";
import { useSchoolDayConfigs } from "@/lib/admin/hooks";
import { adminApi } from "@/lib/admin/client";
import { ApiError } from "@/lib/api";
import type { SchoolDayConfigDto, SchoolDaySlotDto } from "@/lib/admin/types";
import { Card, CardHeader, FadeIn, Skeleton } from "@/components/admin/Primitives";
import { AdminButton } from "@/components/admin/Toolbar";

const SLOT_TYPE_COLORS: Record<string, string> = {
  TEACHING: "bg-accent/10 text-accent-deep",
  BREAK: "bg-amber-100 text-amber-700",
  ASSEMBLY: "bg-teal-100 text-teal-700",
  LAB: "bg-violet-100 text-violet-700",
  FREE: "bg-gray-100 text-gray-600",
  ZERO: "bg-gray-100 text-gray-600",
};

const SLOT_TYPES = ["TEACHING", "BREAK", "ASSEMBLY", "LAB", "FREE", "ZERO"];
const CLASS_LEVELS = ["ALL", "PRIMARY", "SECONDARY"];

function emptySlot(index: number): SchoolDaySlotDto {
  return { slot_index: index, slot_type: "TEACHING", label: "", start_time: "08:00", end_time: "08:45", is_double: false, double_group: 0 };
}

export function SchoolDayConfigPanel() {
  const { data, isLoading } = useSchoolDayConfigs();
  const [showCreate, setShowCreate] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [name, setName] = useState("");
  const [days, setDays] = useState("1,2,3,4,5");
  const [level, setLevel] = useState("ALL");
  const [slots, setSlots] = useState<SchoolDaySlotDto[]>([]);
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState<string | null>(null);
  const [err, setErr] = useState<string | null>(null);
  const [confirmDeactivate, setConfirmDeactivate] = useState<string | null>(null);

  // L-1: auto-clear messages after 5 seconds
  useEffect(() => {
    if (msg || err) {
      const t = setTimeout(() => { setMsg(null); setErr(null); }, 5000);
      return () => clearTimeout(t);
    }
  }, [msg, err]);

  const resetForm = useCallback(() => {
    setName(""); setDays("1,2,3,4,5"); setLevel("ALL"); setSlots([]);
    setEditingId(null);
  }, []);

  function startEdit(config: SchoolDayConfigDto) {
    setEditingId(config.id);
    setName(config.name);
    setDays(config.applicable_days);
    setLevel(config.class_level);
    setSlots(config.slots.length > 0 ? config.slots.map(s => ({ ...s })) : []);
    setShowCreate(false);
  }

  function addSlot() {
    const nextIndex = slots.length > 0 ? Math.max(...slots.map(s => s.slot_index)) + 1 : 0;
    setSlots([...slots, emptySlot(nextIndex)]);
  }

  function updateSlot(idx: number, patch: Partial<SchoolDaySlotDto>) {
    setSlots(slots.map((s, i) => i === idx ? { ...s, ...patch } : s));
  }

  function removeSlot(idx: number) {
    setSlots(slots.filter((_, i) => i !== idx));
  }

  async function create() {
    setErr(null); setMsg(null); setBusy(true);
    try {
      await adminApi.schoolDayConfigCreate({
        name: name.trim(),
        applicable_days: days.trim(),
        class_level: level.trim(),
        slots,
      });
      await mutate("school-day-configs");
      setShowCreate(false);
      resetForm();
      setMsg("Config created.");
    } catch (e) {
      setErr(e instanceof ApiError ? e.message : "Could not create config.");
    } finally {
      setBusy(false);
    }
  }

  async function update() {
    if (!editingId) return;
    setErr(null); setMsg(null); setBusy(true);
    try {
      await adminApi.schoolDayConfigUpdate(editingId, {
        name: name.trim(),
        applicable_days: days.trim(),
        class_level: level.trim(),
        slots,
        is_active: true,
      });
      await mutate("school-day-configs");
      setEditingId(null);
      resetForm();
      setMsg("Config updated.");
    } catch (e) {
      setErr(e instanceof ApiError ? e.message : "Could not update config.");
    } finally {
      setBusy(false);
    }
  }

  async function deactivate(id: string) {
    setBusy(true); setErr(null);
    try {
      await adminApi.schoolDayConfigDeactivate(id);
      await mutate("school-day-configs");
      setMsg("Config deactivated.");
    } catch (e) {
      setErr(e instanceof ApiError ? e.message : "Could not deactivate.");
    } finally {
      setBusy(false);
      setConfirmDeactivate(null);
    }
  }

  const isEditing = editingId !== null;
  const showForm = showCreate || isEditing;

  return (
    <div className="max-w-3xl space-y-6">
      <FadeIn>
        <Card>
          <CardHeader
            title="School Day Configuration"
            subtitle="Define bell schedules with periods, breaks, and assembly slots."
            action={
              <AdminButton onClick={() => { setShowCreate(!showCreate); if (isEditing) resetForm(); }}>
                {showForm ? "Cancel" : "New Config"}
              </AdminButton>
            }
          />

          <div className="p-5">
            {showForm && (
              <div className="mb-6 rounded-xl border border-navy/10 bg-cream/40 p-4">
                <div className="grid gap-3 sm:grid-cols-2">
                  <label className="block">
                    <span className="mb-1 block text-[11px] font-semibold uppercase tracking-wide text-ink-3">Name</span>
                    <input
                      value={name}
                      onChange={(e) => setName(e.target.value)}
                      placeholder="e.g. Default Weekday"
                      className="w-full rounded-lg border border-navy/12 bg-white px-3 py-2 text-[14px] outline-none focus:border-accent"
                    />
                  </label>
                  <label className="block">
                    <span className="mb-1 block text-[11px] font-semibold uppercase tracking-wide text-ink-3">Applicable Days</span>
                    <input
                      value={days}
                      onChange={(e) => setDays(e.target.value)}
                      placeholder="1,2,3,4,5"
                      className="w-full rounded-lg border border-navy/12 bg-white px-3 py-2 text-[14px] outline-none focus:border-accent"
                    />
                  </label>
                  <label className="block">
                    <span className="mb-1 block text-[11px] font-semibold uppercase tracking-wide text-ink-3">Class Level</span>
                    <select
                      value={level}
                      onChange={(e) => setLevel(e.target.value)}
                      className="w-full rounded-lg border border-navy/12 bg-white px-3 py-2 text-[14px] outline-none focus:border-accent"
                    >
                      {CLASS_LEVELS.map(l => <option key={l} value={l}>{l}</option>)}
                    </select>
                  </label>
                </div>

                {/* Slot editor (C-4) */}
                <div className="mt-4">
                  <div className="mb-2 flex items-center justify-between">
                    <span className="text-[11px] font-semibold uppercase tracking-wide text-ink-3">Slots ({slots.length})</span>
                    <button
                      onClick={addSlot}
                      className="text-[12px] font-semibold text-accent hover:text-accent-deep"
                    >
                      + Add Slot
                    </button>
                  </div>
                  {slots.length === 0 ? (
                    <p className="text-[12px] text-ink-3 py-2">No slots defined. Add slots to define the bell schedule.</p>
                  ) : (
                    <div className="space-y-2">
                      {slots.map((slot, idx) => (
                        <SlotEditor
                          key={idx}
                          slot={slot}
                          onChange={(patch) => updateSlot(idx, patch)}
                          onRemove={() => removeSlot(idx)}
                        />
                      ))}
                    </div>
                  )}
                </div>

                <div className="mt-3 flex items-center gap-3">
                  <AdminButton onClick={isEditing ? update : create} disabled={busy || !name.trim() || !days.trim()}>
                    {busy ? (isEditing ? "Updating…" : "Creating…") : (isEditing ? "Update" : "Create")}
                  </AdminButton>
                </div>
              </div>
            )}

            {isLoading && !data ? (
              <div className="space-y-3">
                {Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-24" />)}
              </div>
            ) : data?.configs?.length ? (
              <div className="space-y-4">
                {data.configs.map((cfg) => (
                  <ConfigCard
                    key={cfg.id}
                    config={cfg}
                    onDeactivate={(id) => setConfirmDeactivate(id)}
                    onEdit={() => startEdit(cfg)}
                    busy={busy}
                  />
                ))}
              </div>
            ) : (
              <p className="py-8 text-center text-[14px] text-ink-3">
                No day configs yet. Create one to define your bell schedule.
              </p>
            )}

            <div className="mt-4 flex items-center gap-3">
              {msg && <p className="text-[13px] font-medium text-teal-deep">{msg}</p>}
              {err && <p className="text-[13px] font-medium text-danger">{err}</p>}
            </div>
          </div>
        </Card>
      </FadeIn>

      {/* H-6: Deactivate confirmation dialog */}
      {confirmDeactivate && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40" onClick={() => setConfirmDeactivate(null)}>
          <div className="rounded-xl bg-white p-6 shadow-xl" onClick={e => e.stopPropagation()}>
            <h3 className="text-[16px] font-bold text-navy-deep">Deactivate config?</h3>
            <p className="mt-2 text-[14px] text-ink-3">This will deactivate the school day configuration. You can reactivate it later.</p>
            <div className="mt-4 flex justify-end gap-3">
              <button
                onClick={() => setConfirmDeactivate(null)}
                className="rounded-lg px-4 py-2 text-[13px] font-semibold text-ink-3 hover:bg-gray-100"
              >
                Cancel
              </button>
              <button
                onClick={() => deactivate(confirmDeactivate)}
                disabled={busy}
                className="rounded-lg bg-danger px-4 py-2 text-[13px] font-semibold text-white hover:bg-danger/90 disabled:opacity-50"
              >
                {busy ? "Deactivating…" : "Deactivate"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function ConfigCard({
  config,
  onDeactivate,
  onEdit,
  busy,
}: {
  config: SchoolDayConfigDto;
  onDeactivate: (id: string) => void;
  onEdit: () => void;
  busy: boolean;
}) {
  return (
    <div className="rounded-xl border border-navy/10 bg-white p-4">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <span className={`rounded-full px-2 py-0.5 text-[11px] font-bold ${config.is_active ? "bg-teal-100 text-teal-700" : "bg-gray-100 text-gray-500"}`}>
            {config.is_active ? "ACTIVE" : "INACTIVE"}
          </span>
          <h3 className="text-[16px] font-bold text-navy-deep">{config.name}</h3>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={onEdit}
            disabled={busy}
            className="text-[12px] font-semibold text-accent hover:text-accent-deep disabled:opacity-50"
          >
            Edit
          </button>
          {config.is_active && (
            <button
              onClick={() => onDeactivate(config.id)}
              disabled={busy}
              className="text-[12px] font-semibold text-ink-3 hover:text-danger disabled:opacity-50"
            >
              Deactivate
            </button>
          )}
        </div>
      </div>
      <p className="mt-1 text-[12px] text-ink-3">
        Days: {config.applicable_days} · Level: {config.class_level}
      </p>
      {config.slots.length > 0 && (
        <div className="mt-3 divide-y divide-navy/6">
          {config.slots.map((slot) => (
            <SlotRow key={slot.slot_index} slot={slot} />
          ))}
        </div>
      )}
    </div>
  );
}

function SlotRow({ slot }: { slot: SchoolDaySlotDto }) {
  const colorClass = SLOT_TYPE_COLORS[slot.slot_type] ?? "bg-gray-100 text-gray-600";
  return (
    <div className="flex items-center gap-3 py-2">
      <span className={`rounded-md px-2 py-0.5 text-[10px] font-bold ${colorClass}`}>
        {slot.slot_type}
      </span>
      <div className="flex-1">
        {slot.label && <p className="text-[13px] font-semibold text-ink">{slot.label}</p>}
        <p className="text-[12px] text-ink-3">{slot.start_time} – {slot.end_time}</p>
      </div>
      <span className="text-[12px] font-mono text-ink-3">#{slot.slot_index}</span>
    </div>
  );
}

function SlotEditor({
  slot,
  onChange,
  onRemove,
}: {
  slot: SchoolDaySlotDto;
  onChange: (patch: Partial<SchoolDaySlotDto>) => void;
  onRemove: () => void;
}) {
  return (
    <div className="grid grid-cols-[auto_1fr_1fr_1fr_1fr_auto] items-center gap-2 rounded-lg border border-navy/8 bg-white p-2">
      <select
        value={slot.slot_type}
        onChange={e => onChange({ slot_type: e.target.value })}
        className="rounded border border-navy/12 bg-white px-2 py-1 text-[12px] outline-none focus:border-accent"
      >
        {SLOT_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
      </select>
      <input
        value={slot.label}
        onChange={e => onChange({ label: e.target.value })}
        placeholder="Label"
        className="rounded border border-navy/12 bg-white px-2 py-1 text-[12px] outline-none focus:border-accent"
      />
      <input
        type="time"
        value={slot.start_time}
        onChange={e => onChange({ start_time: e.target.value })}
        className="rounded border border-navy/12 bg-white px-2 py-1 text-[12px] outline-none focus:border-accent"
      />
      <input
        type="time"
        value={slot.end_time}
        onChange={e => onChange({ end_time: e.target.value })}
        className="rounded border border-navy/12 bg-white px-2 py-1 text-[12px] outline-none focus:border-accent"
      />
      <label className="flex items-center gap-1 text-[11px] text-ink-3">
        <input
          type="checkbox"
          checked={slot.is_double}
          onChange={e => onChange({ is_double: e.target.checked })}
        />
        Double
      </label>
      <button
        onClick={onRemove}
        className="text-[14px] font-bold text-ink-3 hover:text-danger"
      >
        ×
      </button>
    </div>
  );
}
