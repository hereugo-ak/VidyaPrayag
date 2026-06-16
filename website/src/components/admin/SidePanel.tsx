"use client";

import { AnimatePresence, motion } from "framer-motion";
import { useEffect } from "react";
import { IconClose } from "./icons";

/**
 * Drill-down side panel — slides in from the right with no page reload. Used by
 * the dashboard intelligence panels (anomaly day, class fees, syllabus cell,
 * at-risk student). Locks body scroll, closes on Escape / backdrop click, and
 * is fully keyboard-accessible. State is owned by the caller (which mirrors it
 * into the URL via ?panel=… so a principal can share a specific view).
 */
export function SidePanel({
  open,
  onClose,
  title,
  subtitle,
  children,
}: {
  open: boolean;
  onClose: () => void;
  title: string;
  subtitle?: string;
  children: React.ReactNode;
}) {
  useEffect(() => {
    if (!open) return;
    const prev = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    document.addEventListener("keydown", onKey);
    return () => {
      document.body.style.overflow = prev;
      document.removeEventListener("keydown", onKey);
    };
  }, [open, onClose]);

  return (
    <AnimatePresence>
      {open && (
        <div className="fixed inset-0 z-50" role="dialog" aria-modal="true" aria-label={title}>
          <motion.div
            className="absolute inset-0 bg-navy-deep/30 backdrop-blur-[2px]"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.2 }}
            onClick={onClose}
          />
          <motion.aside
            className="absolute right-0 top-0 flex h-full w-full max-w-[440px] flex-col border-l border-navy/10 bg-lavender-soft shadow-cardHover"
            initial={{ x: "100%" }}
            animate={{ x: 0 }}
            exit={{ x: "100%" }}
            transition={{ duration: 0.32, ease: [0.16, 1, 0.3, 1] }}
          >
            <div className="flex items-start justify-between gap-4 border-b border-navy/8 px-6 py-5">
              <div className="min-w-0">
                <h2 className="truncate text-[16px] font-bold tracking-tight text-navy-deep">
                  {title}
                </h2>
                {subtitle && <p className="mt-0.5 text-[13px] text-ink-3">{subtitle}</p>}
              </div>
              <button
                onClick={onClose}
                className="-mr-1.5 shrink-0 rounded-full p-2 text-ink-2 transition-colors hover:bg-navy/6 hover:text-navy-deep focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent/40"
                aria-label="Close panel"
              >
                <IconClose />
              </button>
            </div>
            <div className="flex-1 overflow-y-auto px-6 py-5">{children}</div>
          </motion.aside>
        </div>
      )}
    </AnimatePresence>
  );
}
