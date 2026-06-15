"use client";

import { motion } from "framer-motion";
import { WIZARD_STEPS, type WizardStep } from "@/lib/onboarding";

/**
 * Progress indicator for the onboarding wizard. Renders every step in
 * WIZARD_STEPS, the current one highlighted, completed ones ticked. Calm, no looping animation —
 * just a width transition on the connecting rail.
 */
export function Stepper({ current }: { current: WizardStep }) {
  const currentIndex = WIZARD_STEPS.findIndex((s) => s.id === current);
  const progress = currentIndex / (WIZARD_STEPS.length - 1);

  return (
    <div>
      {/* Mobile: compact "Step n of m" + bar */}
      <div className="md:hidden">
        <div className="flex items-baseline justify-between">
          <p className="text-sm font-semibold text-navy-deep">
            {WIZARD_STEPS[currentIndex]?.label}
          </p>
          <p className="font-mono text-xs text-ink-3">
            {currentIndex + 1} / {WIZARD_STEPS.length}
          </p>
        </div>
        <div className="mt-3 h-1.5 overflow-hidden rounded-full bg-navy/10">
          <motion.div
            className="h-full rounded-full bg-accent"
            initial={false}
            animate={{ width: `${progress * 100}%` }}
            transition={{ duration: 0.4, ease: [0.16, 1, 0.3, 1] }}
          />
        </div>
      </div>

      {/* Desktop: full labelled rail */}
      <ol className="hidden items-center md:flex">
        {WIZARD_STEPS.map((s, i) => {
          const done = i < currentIndex;
          const active = i === currentIndex;
          return (
            <li key={s.id} className="flex flex-1 items-center last:flex-none">
              <div className="flex items-center gap-3">
                <span
                  className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-full border text-sm font-bold transition-colors duration-300 ${
                    done
                      ? "border-accent bg-accent text-white"
                      : active
                        ? "border-accent bg-accent/10 text-accent"
                        : "border-navy/15 bg-white/60 text-ink-3"
                  }`}
                >
                  {done ? (
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M5 13l4 4L19 7" />
                    </svg>
                  ) : (
                    i + 1
                  )}
                </span>
                <span
                  className={`text-sm font-semibold transition-colors duration-300 ${
                    active ? "text-navy-deep" : done ? "text-ink-2" : "text-ink-3"
                  }`}
                >
                  {s.short}
                </span>
              </div>
              {i < WIZARD_STEPS.length - 1 && (
                <span className="mx-3 h-px flex-1 bg-navy/10">
                  <motion.span
                    className="block h-px bg-accent"
                    initial={false}
                    animate={{ width: done ? "100%" : "0%" }}
                    transition={{ duration: 0.4, ease: [0.16, 1, 0.3, 1] }}
                  />
                </span>
              )}
            </li>
          );
        })}
      </ol>
    </div>
  );
}
