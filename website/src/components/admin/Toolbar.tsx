"use client";

import { useEffect } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { IconClose, IconSearch } from "./icons";

/**
 * Toolbar, a search input plus optional filter chips, used above every list
 * surface (People, Attendance, …). Controlled: the caller owns the query/filter
 * state so the same toolbar drives server-filtered and client-filtered lists.
 */
export function Toolbar({
  query,
  onQuery,
  placeholder = "Search…",
  filters,
  active,
  onFilter,
  trailing,
}: {
  query: string;
  onQuery: (v: string) => void;
  placeholder?: string;
  /** Optional filter chips, e.g. classes or statuses. */
  filters?: { value: string; label: string }[];
  /** Currently-active filter value ("" = all). */
  active?: string;
  onFilter?: (value: string) => void;
  /** Right-aligned actions (e.g. an "Add" button). */
  trailing?: React.ReactNode;
}) {
  return (
    <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
      <div className="flex flex-1 flex-wrap items-center gap-2">
        <label className="relative flex min-w-[200px] flex-1 items-center md:max-w-xs">
          <span className="pointer-events-none absolute left-3 text-ink-3">
            <IconSearch width={17} height={17} />
          </span>
          <input
            type="search"
            value={query}
            onChange={(e) => onQuery(e.target.value)}
            placeholder={placeholder}
            className="w-full rounded-xl border border-navy/12 bg-white/80 py-2.5 pl-9 pr-3 text-[14px] text-ink outline-none transition-colors duration-200 placeholder:text-ink-placeholder focus:border-accent focus:bg-white"
          />
        </label>

        {filters && filters.length > 0 && (
          <div className="no-scrollbar flex items-center gap-1.5 overflow-x-auto">
            <Chip label="All" active={!active} onClick={() => onFilter?.("")} />
            {filters.map((f) => (
              <Chip
                key={f.value}
                label={f.label}
                active={active === f.value}
                onClick={() => onFilter?.(f.value)}
              />
            ))}
          </div>
        )}
      </div>

      {trailing && <div className="flex shrink-0 items-center gap-2">{trailing}</div>}
    </div>
  );
}

function Chip({
  label,
  active,
  onClick,
}: {
  label: string;
  active?: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`shrink-0 rounded-full px-3 py-1.5 text-[12.5px] font-semibold transition-colors duration-200 ${
        active
          ? "bg-navy-deep text-white"
          : "bg-navy/6 text-ink-2 hover:bg-navy/10"
      }`}
    >
      {label}
    </button>
  );
}

/**
 * Modal, a centred dialog with a scrim. Closes on Escape or scrim click.
 * Restrained motion: scales/fades in once and settles (no looping). Locks body
 * scroll while open. Accessible: role=dialog + aria-modal, labelled by title.
 */
export function Modal({
  open,
  onClose,
  title,
  description,
  children,
  footer,
  size = "md",
}: {
  open: boolean;
  onClose: () => void;
  title: string;
  description?: string;
  children: React.ReactNode;
  footer?: React.ReactNode;
  size?: "sm" | "md" | "lg";
}) {
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    document.addEventListener("keydown", onKey);
    const prev = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.removeEventListener("keydown", onKey);
      document.body.style.overflow = prev;
    };
  }, [open, onClose]);

  const maxW =
    size === "sm" ? "max-w-sm" : size === "lg" ? "max-w-2xl" : "max-w-lg";

  return (
    <AnimatePresence>
      {open && (
        <motion.div
          className="fixed inset-0 z-50 flex items-center justify-center p-4"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.18 }}
        >
          {/* scrim */}
          <button
            type="button"
            aria-label="Close dialog"
            onClick={onClose}
            className="absolute inset-0 bg-navy-deep/40 backdrop-blur-[2px]"
          />

          <motion.div
            role="dialog"
            aria-modal="true"
            aria-label={title}
            className={`relative z-10 w-full ${maxW} overflow-hidden rounded-2xl border border-navy/10 bg-white shadow-xl`}
            initial={{ opacity: 0, scale: 0.96, y: 8 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.98, y: 4 }}
            transition={{ duration: 0.2, ease: [0.16, 1, 0.3, 1] }}
          >
            <div className="flex items-start justify-between gap-4 border-b border-navy/8 px-5 py-4">
              <div>
                <h2 className="text-[16px] font-bold tracking-tight text-navy-deep">
                  {title}
                </h2>
                {description && (
                  <p className="mt-0.5 text-[13px] text-ink-3">{description}</p>
                )}
              </div>
              <button
                type="button"
                onClick={onClose}
                aria-label="Close"
                className="rounded-lg p-1.5 text-ink-3 transition-colors hover:bg-navy/6 hover:text-navy-deep"
              >
                <IconClose width={18} height={18} />
              </button>
            </div>

            <div className="max-h-[70vh] overflow-y-auto px-5 py-5">{children}</div>

            {footer && (
              <div className="flex items-center justify-end gap-3 border-t border-navy/8 bg-navy/[0.015] px-5 py-3.5">
                {footer}
              </div>
            )}
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}

/** A small button used inside admin surfaces (primary / ghost / danger). */
export function AdminButton({
  children,
  onClick,
  type = "button",
  variant = "primary",
  disabled,
  className = "",
}: {
  children: React.ReactNode;
  onClick?: () => void;
  type?: "button" | "submit";
  variant?: "primary" | "ghost" | "danger";
  disabled?: boolean;
  className?: string;
}) {
  const styles: Record<string, string> = {
    primary: "bg-accent text-white hover:bg-accent-deep disabled:opacity-50",
    ghost: "border border-navy/15 bg-white/70 text-navy-deep hover:border-accent hover:text-accent",
    danger: "bg-danger/10 text-danger hover:bg-danger/15",
  };
  return (
    <button
      type={type}
      onClick={onClick}
      disabled={disabled}
      className={`inline-flex items-center justify-center gap-1.5 rounded-xl px-4 py-2.5 text-[13.5px] font-semibold transition-colors duration-200 disabled:cursor-not-allowed ${styles[variant]} ${className}`}
    >
      {children}
    </button>
  );
}
