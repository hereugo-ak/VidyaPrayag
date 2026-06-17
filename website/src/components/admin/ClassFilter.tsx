"use client";

import { useEffect, useRef, useState } from "react";
import { IconChevronRight } from "./icons";

/**
 * Class filter dropdown. ONE component, used by BOTH the two independent
 * controls (the two-filter law):
 *   • Control A — calendar-scoped, lives in the calendar header.
 *   • Control B — dashboard-global, lives in the greeting bar.
 * Each instance owns its own value; they never read or write each other.
 *
 * "All classes" is the default (value = null). Fully keyboard-accessible:
 * Enter/Space opens, Escape closes, click-outside closes.
 */
export function ClassFilter({
  classes,
  value,
  onChange,
  label = "All classes",
  size = "md",
  tone = "light",
}: {
  classes: string[];
  value: string | null;
  onChange: (v: string | null) => void;
  label?: string;
  size?: "sm" | "md";
  /** "light" on white surfaces, "onDark" inside the navy greeting bar. */
  tone?: "light" | "onDark";
}) {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    const onClick = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") setOpen(false);
    };
    document.addEventListener("mousedown", onClick);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onClick);
      document.removeEventListener("keydown", onKey);
    };
  }, [open]);

  const pad = size === "sm" ? "px-2.5 py-1.5 text-[12px]" : "px-3 py-2 text-[13px]";
  const trigger =
    tone === "onDark"
      ? "bg-white/10 text-white ring-1 ring-inset ring-white/15 hover:bg-white/15"
      : "bg-white text-navy-deep ring-1 ring-inset ring-navy/10 hover:bg-navy/[0.03]";
  const display = value ?? label;
  const active = value != null;

  return (
    <div ref={ref} className="relative">
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        className={`inline-flex items-center gap-1.5 rounded-xl font-semibold transition-colors ${pad} ${trigger} focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent/40`}
        aria-haspopup="listbox"
        aria-expanded={open}
      >
        {active && (
          <span className="h-1.5 w-1.5 shrink-0 rounded-full bg-accent" aria-hidden="true" />
        )}
        <span className="max-w-[140px] truncate">{display}</span>
        <IconChevronRight
          width={14}
          height={14}
          className={`shrink-0 transition-transform ${open ? "rotate-90" : "rotate-90"} opacity-60`}
        />
      </button>

      {open && (
        <div
          role="listbox"
          className="absolute right-0 z-30 mt-2 max-h-[300px] w-[220px] overflow-y-auto rounded-2xl border border-navy/10 bg-white p-1.5 shadow-cardHover"
        >
          <Option
            label={label}
            selected={value == null}
            onClick={() => {
              onChange(null);
              setOpen(false);
            }}
          />
          {classes.length === 0 && (
            <p className="px-3 py-2 text-[12px] text-ink-3">No classes yet</p>
          )}
          {classes.map((c) => (
            <Option
              key={c}
              label={c}
              selected={value === c}
              onClick={() => {
                onChange(c);
                setOpen(false);
              }}
            />
          ))}
        </div>
      )}
    </div>
  );
}

function Option({
  label,
  selected,
  onClick,
}: {
  label: string;
  selected: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      role="option"
      aria-selected={selected}
      onClick={onClick}
      className={`flex w-full items-center justify-between gap-2 rounded-xl px-3 py-2 text-left text-[13px] font-medium transition-colors ${
        selected ? "bg-accent/10 text-accent-deep" : "text-ink-2 hover:bg-navy/[0.04]"
      }`}
    >
      <span className="truncate">{label}</span>
      {selected && <span className="h-1.5 w-1.5 shrink-0 rounded-full bg-accent" />}
    </button>
  );
}
