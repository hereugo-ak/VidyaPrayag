"use client";

import {
  BOARD_OPTIONS,
  MEDIUM_OPTIONS,
  GENDER_OPTIONS,
  type RegisterData,
  type BasicData,
  type BrandingData,
  type AcademicData,
  type ClassRow,
  type WizardState,
  type WizardStep,
  type Errors,
} from "@/lib/onboarding";
import { TextField, TextArea, SelectField, RadioGroup } from "../ui/Field";

function StepHead({ title, lede }: { title: string; lede: string }) {
  return (
    <header className="mb-7">
      <h2 className="text-2xl font-extrabold tracking-tighter text-navy-deep">{title}</h2>
      <p className="mt-2 text-sm leading-relaxed text-ink-2">{lede}</p>
    </header>
  );
}

// ─── REGISTER ───────────────────────────────────────────────────────────────
export function RegisterStep({
  data,
  errors,
  onChange,
}: {
  data: RegisterData;
  errors: Errors<RegisterData>;
  onChange: (patch: Partial<RegisterData>) => void;
}) {
  return (
    <div>
      <StepHead
        title="Create your admin account"
        lede="This is the head-office login that controls your whole school. You can add teachers and staff once you're inside."
      />
      <div className="grid gap-4">
        <TextField
          label="Your full name"
          value={data.adminName}
          onChange={(v) => onChange({ adminName: v })}
          error={errors.adminName}
          autoComplete="name"
        />
        <TextField
          label="Work email"
          type="email"
          inputMode="email"
          value={data.email}
          onChange={(v) => onChange({ email: v })}
          error={errors.email}
          autoComplete="email"
        />
        <TextField
          label="Choose a password"
          type="password"
          value={data.password}
          onChange={(v) => onChange({ password: v })}
          error={errors.password}
          autoComplete="new-password"
        />
        <TextField
          label="School name"
          value={data.schoolName}
          onChange={(v) => onChange({ schoolName: v })}
          error={errors.schoolName}
          autoComplete="organization"
        />
      </div>
    </div>
  );
}

// ─── BASIC ──────────────────────────────────────────────────────────────────
export function BasicStep({
  data,
  errors,
  onChange,
}: {
  data: BasicData;
  errors: Errors<BasicData>;
  onChange: (patch: Partial<BasicData>) => void;
}) {
  return (
    <div>
      <StepHead
        title="Institutional basics"
        lede="The essentials we use to scope your dashboards and address your school correctly."
      />
      <div className="grid gap-5">
        <div className="grid gap-4 sm:grid-cols-2">
          <SelectField
            label="Board"
            value={data.board}
            onChange={(v) => onChange({ board: v })}
            options={BOARD_OPTIONS}
            error={errors.board}
          />
          <SelectField
            label="Medium of instruction"
            value={data.medium}
            onChange={(v) => onChange({ medium: v })}
            options={MEDIUM_OPTIONS}
            error={errors.medium}
          />
        </div>
        <RadioGroup
          label="School type"
          value={data.school_gender}
          onChange={(v) => onChange({ school_gender: v })}
          options={GENDER_OPTIONS}
          error={errors.school_gender}
        />
        <div className="grid gap-4 sm:grid-cols-2">
          <TextField
            label="Contact email (optional)"
            type="email"
            inputMode="email"
            value={data.contact_email}
            onChange={(v) => onChange({ contact_email: v })}
            error={errors.contact_email}
          />
          <TextField
            label="Contact phone (optional)"
            inputMode="tel"
            value={data.contact_phone}
            onChange={(v) => onChange({ contact_phone: v })}
          />
        </div>
        <div className="grid gap-4 sm:grid-cols-3">
          <TextField label="City" value={data.city} onChange={(v) => onChange({ city: v })} error={errors.city} />
          <TextField label="District (optional)" value={data.district} onChange={(v) => onChange({ district: v })} />
          <TextField label="State" value={data.state} onChange={(v) => onChange({ state: v })} error={errors.state} />
        </div>
        <div className="grid gap-4 sm:grid-cols-[1fr_2fr]">
          <TextField label="Pincode (optional)" inputMode="numeric" value={data.pincode} onChange={(v) => onChange({ pincode: v })} />
          <TextField label="Full address (optional)" value={data.full_address} onChange={(v) => onChange({ full_address: v })} />
        </div>
      </div>
    </div>
  );
}

// ─── BRANDING ─────────────────────────────────────────────────────────────────
const SWATCHES = ["#6C5CE0", "#26234D", "#006A60", "#B3261E", "#B3651A", "#1F7A4D"];

export function BrandingStep({
  data,
  onChange,
}: {
  data: BrandingData;
  onChange: (patch: Partial<BrandingData>) => void;
}) {
  return (
    <div>
      <StepHead
        title="Branding & identity"
        lede="Pick the accent colour parents and teachers will see in your school's app. You can change it later — and add a logo URL if you have one."
      />
      <div className="grid gap-6">
        <div>
          <p className="mb-2 text-[11px] font-semibold uppercase tracking-wide text-ink-3">Brand colour</p>
          <div className="flex flex-wrap items-center gap-3">
            {SWATCHES.map((c) => {
              const active = data.brand_color.toLowerCase() === c.toLowerCase();
              return (
                <button
                  key={c}
                  type="button"
                  aria-label={`Use ${c}`}
                  onClick={() => onChange({ brand_color: c })}
                  className={`h-10 w-10 rounded-full ring-2 ring-offset-2 transition-transform duration-200 ease-out-cubic ${
                    active ? "ring-navy-deep scale-110" : "ring-transparent hover:scale-105"
                  }`}
                  style={{ backgroundColor: c }}
                />
              );
            })}
            <label className="ml-1 inline-flex items-center gap-2 text-sm text-ink-2">
              <input
                type="color"
                value={data.brand_color}
                onChange={(e) => onChange({ brand_color: e.target.value })}
                className="h-10 w-10 cursor-pointer rounded-lg border border-navy/15 bg-white p-1"
              />
              Custom
            </label>
          </div>
        </div>
        <TextField
          label="Logo URL (optional)"
          value={data.logo_url}
          onChange={(v) => onChange({ logo_url: v })}
        />
      </div>
    </div>
  );
}

// ─── ACADEMIC ───────────────────────────────────────────────────────────────
const EMPTY_ROW: ClassRow = { name: "", sections: "A", subjects: "" };

export function AcademicStep({
  data,
  onChange,
}: {
  data: AcademicData;
  onChange: (classes: ClassRow[]) => void;
}) {
  const rows = data.classes;
  const patchRow = (i: number, patch: Partial<ClassRow>) =>
    onChange(rows.map((r, idx) => (idx === i ? { ...r, ...patch } : r)));
  const addRow = () => onChange([...rows, { ...EMPTY_ROW }]);
  const removeRow = (i: number) => onChange(rows.filter((_, idx) => idx !== i));

  return (
    <div>
      <StepHead
        title="Academic structure"
        lede="Add the classes you run, the sections in each, and the subjects taught. Separate sections and subjects with commas — you can refine all of this inside the dashboard."
      />
      <div className="space-y-4">
        {rows.map((row, i) => (
          <div key={i} className="rounded-xl border border-navy/12 bg-lavender-soft/50 p-4">
            <div className="mb-3 flex items-center justify-between">
              <span className="font-mono text-xs text-ink-3">Class {i + 1}</span>
              {rows.length > 1 && (
                <button
                  type="button"
                  onClick={() => removeRow(i)}
                  className="text-xs font-semibold text-danger hover:underline"
                >
                  Remove
                </button>
              )}
            </div>
            <div className="grid gap-3 sm:grid-cols-3">
              <TextField label="Class name" value={row.name} onChange={(v) => patchRow(i, { name: v })} />
              <TextField label="Sections (A, B…)" value={row.sections} onChange={(v) => patchRow(i, { sections: v })} />
              <TextField label="Subjects" value={row.subjects} onChange={(v) => patchRow(i, { subjects: v })} />
            </div>
          </div>
        ))}
      </div>
      <button
        type="button"
        onClick={addRow}
        className="mt-4 inline-flex items-center gap-2 rounded-full border border-navy/15 bg-white/70 px-4 py-2 text-sm font-semibold text-navy-deep transition-colors hover:border-accent hover:text-accent"
      >
        + Add another class
      </button>
    </div>
  );
}

// ─── REVIEW ───────────────────────────────────────────────────────────────────
function Row({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex justify-between gap-6 border-b border-navy/8 py-2.5 last:border-0">
      <span className="text-sm text-ink-3">{label}</span>
      <span className="text-right text-sm font-medium text-navy-deep">{value || "—"}</span>
    </div>
  );
}

function Card({
  title,
  step,
  onJump,
  children,
}: {
  title: string;
  step: WizardStep;
  onJump: (s: WizardStep) => void;
  children: React.ReactNode;
}) {
  return (
    <div className="rounded-xl border border-navy/10 bg-lavender-soft/50 p-5">
      <div className="mb-2 flex items-center justify-between">
        <h3 className="text-sm font-bold uppercase tracking-wide text-ink-2">{title}</h3>
        <button type="button" onClick={() => onJump(step)} className="text-xs font-semibold text-accent hover:underline">
          Edit
        </button>
      </div>
      {children}
    </div>
  );
}

export function ReviewStep({
  state,
  onJump,
}: {
  state: WizardState;
  onJump: (s: WizardStep) => void;
}) {
  const validClasses = state.academic.classes.filter((c) => c.name.trim());
  return (
    <div>
      <StepHead
        title="Review & launch"
        lede="A final look before we create everything. Edit any section, then launch — your school goes live immediately."
      />
      <div className="grid gap-4">
        <Card title="Account & school" step="REGISTER" onJump={onJump}>
          <Row label="Admin" value={state.register.adminName} />
          <Row label="Email" value={state.register.email} />
          <Row label="School" value={state.register.schoolName} />
        </Card>
        <Card title="Basics" step="BASIC" onJump={onJump}>
          <Row label="Board" value={state.basic.board} />
          <Row label="Medium" value={state.basic.medium} />
          <Row label="Type" value={state.basic.school_gender.replace("_", "-")} />
          <Row label="Location" value={[state.basic.city, state.basic.state].filter(Boolean).join(", ")} />
        </Card>
        <Card title="Branding" step="BRANDING" onJump={onJump}>
          <div className="flex items-center gap-3 py-1">
            <span className="h-6 w-6 rounded-full ring-1 ring-navy/15" style={{ backgroundColor: state.branding.brand_color }} />
            <span className="font-mono text-sm text-navy-deep">{state.branding.brand_color}</span>
          </div>
        </Card>
        <Card title="Academic structure" step="ACADEMIC" onJump={onJump}>
          {validClasses.length === 0 ? (
            <p className="text-sm text-ink-3">No classes added yet.</p>
          ) : (
            <ul className="space-y-1.5">
              {validClasses.map((c, i) => (
                <li key={i} className="text-sm text-navy-deep">
                  <span className="font-medium">{c.name}</span>
                  <span className="text-ink-3">
                    {" "}
                    — sections {c.sections || "—"}
                    {c.subjects ? ` · ${c.subjects}` : ""}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>
    </div>
  );
}
