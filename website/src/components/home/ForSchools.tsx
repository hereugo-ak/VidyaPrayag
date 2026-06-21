"use client";

import { useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { SCHOOL_FEATURES } from "@/lib/content";
import { SectionHeading } from "../ui/SectionHeading";
import { EASE_OUT_CUBIC } from "@/lib/motion";

/**
 * For Schools, a tabbed deep-dive (not a standard feature grid). The active
 * feature is revealed on the right with a calm cross-fade; the rail on the left
 * is a vertical index of the real admin modules.
 */
export function ForSchools() {
  const [active, setActive] = useState(0);
  const feature = SCHOOL_FEATURES[active];

  return (
    <section id="schools" className="scroll-mt-24 py-24 md:py-32">
      <div className="shell">
        <SectionHeading
          eyebrow="For schools"
          title={
            <>
              The admin office, <span className="text-accent">rebuilt.</span>
            </>
          }
          lede="Every module below is something the platform actually does, derived from the dashboard, people, comms, records and analytics surfaces your administrators use daily."
        />

        <div className="mt-14 grid gap-10 lg:grid-cols-[0.9fr_1.1fr]">
          {/* Index rail */}
          <div className="flex flex-col gap-1">
            {SCHOOL_FEATURES.map((f, i) => {
              const on = i === active;
              return (
                <button
                  key={f.key}
                  onClick={() => setActive(i)}
                  className={`group relative rounded-2xl border px-5 py-4 text-left transition-colors duration-200 ease-out-cubic ${
                    on
                      ? "border-accent/40 bg-accent/[0.06]"
                      : "border-transparent hover:bg-navy/[0.03]"
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <span
                      className={`font-mono text-[12px] font-medium ${
                        on ? "text-accent" : "text-ink-3"
                      }`}
                    >
                      {String(i + 1).padStart(2, "0")}
                    </span>
                    <span
                      className={`text-[15px] font-bold tracking-tight ${
                        on ? "text-navy-deep" : "text-ink-2"
                      }`}
                    >
                      {f.title}
                    </span>
                  </div>
                </button>
              );
            })}
          </div>

          {/* Active detail panel */}
          <div className="relative min-h-[320px] rounded-xl2 border border-navy/8 bg-white/60 p-8 shadow-card md:p-10">
            <AnimatePresence mode="wait">
              <motion.div
                key={feature.key}
                initial={{ opacity: 0, y: 14 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -8 }}
                transition={{ duration: 0.3, ease: EASE_OUT_CUBIC }}
              >
                <h3 className="text-2xl font-extrabold tracking-tight text-navy-deep">
                  {feature.title}
                </h3>
                <p className="mt-4 max-w-xl text-[15px] leading-relaxed text-ink-2">
                  {feature.body}
                </p>
                <ul className="mt-8 grid grid-cols-1 gap-3 sm:grid-cols-2">
                  {feature.points.map((p) => (
                    <li
                      key={p}
                      className="flex items-center gap-3 rounded-xl bg-lavender-tint/70 px-4 py-3"
                    >
                      <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-teal/15 text-teal-deep">
                        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
                          <path d="M20 6 9 17l-5-5" />
                        </svg>
                      </span>
                      <span className="text-sm font-medium text-ink">{p}</span>
                    </li>
                  ))}
                </ul>
              </motion.div>
            </AnimatePresence>
          </div>
        </div>
      </div>
    </section>
  );
}
