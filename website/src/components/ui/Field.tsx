"use client";

import { useId } from "react";

const fieldBase =
  "peer w-full rounded-xl border bg-white/70 px-4 pb-2.5 pt-6 text-[15px] text-ink outline-none transition-colors duration-200 ease-out-cubic placeholder-transparent focus:bg-white";
const okBorder = "border-navy/15 focus:border-accent";
const errBorder = "border-danger/60 focus:border-danger";

const labelBase =
  "pointer-events-none absolute left-4 top-4 z-10 origin-left text-ink-3 transition-all duration-200 ease-out-cubic peer-placeholder-shown:top-4 peer-placeholder-shown:text-[15px] peer-focus:top-2 peer-focus:text-[11px] peer-focus:font-semibold peer-focus:tracking-wide peer-[:not(:placeholder-shown)]:top-2 peer-[:not(:placeholder-shown)]:text-[11px] peer-[:not(:placeholder-shown)]:font-semibold";

function ErrorText({ msg }: { msg?: string }) {
  if (!msg) return null;
  return <p className="mt-1.5 text-[12px] font-medium text-danger">{msg}</p>;
}

export function TextField({
  label,
  value,
  onChange,
  error,
  type = "text",
  autoComplete,
  inputMode,
  onBlur,
}: {
  label: string;
  value: string;
  onChange: (v: string) => void;
  error?: string;
  type?: string;
  autoComplete?: string;
  inputMode?: "text" | "email" | "tel" | "numeric";
  onBlur?: () => void;
}) {
  const id = useId();
  return (
    <div>
      <div className="relative">
        <input
          id={id}
          type={type}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          onBlur={onBlur}
          placeholder={label}
          autoComplete={autoComplete}
          inputMode={inputMode}
          aria-invalid={!!error}
          className={`${fieldBase} ${error ? errBorder : okBorder}`}
        />
        <label htmlFor={id} className={`${labelBase} ${error ? "text-danger" : "peer-focus:text-accent"}`}>
          {label}
        </label>
      </div>
      <ErrorText msg={error} />
    </div>
  );
}

export function TextArea({
  label,
  value,
  onChange,
  error,
  rows = 3,
  placeholder,
  className = "",
}: {
  label: string;
  value: string;
  onChange: (v: string) => void;
  error?: string;
  rows?: number;
  /** Custom placeholder (defaults to the floating label text). */
  placeholder?: string;
  /** Extra classes merged onto the textarea (e.g. monospace). */
  className?: string;
}) {
  const id = useId();
  return (
    <div>
      <div className="relative">
        <textarea
          id={id}
          rows={rows}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          placeholder={placeholder ?? label}
          className={`${fieldBase} resize-none ${error ? errBorder : okBorder} ${className}`}
        />
        <label htmlFor={id} className={`${labelBase} ${error ? "text-danger" : "peer-focus:text-accent"}`}>
          {label}
        </label>
      </div>
      <ErrorText msg={error} />
    </div>
  );
}

export function SelectField({
  label,
  value,
  onChange,
  options,
  error,
}: {
  label: string;
  value: string;
  onChange: (v: string) => void;
  options: { value: string; label: string }[];
  error?: string;
}) {
  const id = useId();
  return (
    <div>
      <label htmlFor={id} className="mb-1.5 block text-[11px] font-semibold uppercase tracking-wide text-ink-3">
        {label}
      </label>
      <select
        id={id}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className={`h-13 w-full appearance-none rounded-xl border bg-white/70 px-4 py-3 text-[15px] text-ink outline-none transition-colors duration-200 focus:bg-white ${
          error ? errBorder : okBorder
        }`}
        style={{
          backgroundImage:
            "url(\"data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='20' height='20' viewBox='0 0 24 24' fill='none' stroke='%236D7A77' stroke-width='2'><path d='M6 9l6 6 6-6'/></svg>\")",
          backgroundRepeat: "no-repeat",
          backgroundPosition: "right 0.9rem center",
        }}
      >
        {options.map((o) => (
          <option key={o.value} value={o.value}>
            {o.label}
          </option>
        ))}
      </select>
      <ErrorText msg={error} />
    </div>
  );
}

export function RadioGroup({
  label,
  value,
  onChange,
  options,
  error,
}: {
  label: string;
  value: string;
  onChange: (v: string) => void;
  options: { value: string; label: string }[];
  error?: string;
}) {
  return (
    <div>
      <p className="mb-2 text-[11px] font-semibold uppercase tracking-wide text-ink-3">{label}</p>
      <div className="grid grid-cols-1 gap-2 sm:grid-cols-3">
        {options.map((o) => {
          const active = value === o.value;
          return (
            <button
              key={o.value}
              type="button"
              onClick={() => onChange(o.value)}
              className={`rounded-xl border px-4 py-3 text-left text-sm font-medium transition-colors duration-200 ease-out-cubic ${
                active
                  ? "border-accent bg-accent/8 text-navy-deep ring-1 ring-accent/40"
                  : "border-navy/12 bg-white/60 text-ink-2 hover:border-navy/25"
              }`}
            >
              {o.label}
            </button>
          );
        })}
      </div>
      <ErrorText msg={error} />
    </div>
  );
}
