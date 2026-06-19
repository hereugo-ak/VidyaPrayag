"use client";

import { motion, useReducedMotion } from "framer-motion";
import { PhoneMockup } from "./PhoneMockup";
import { PARENTS_PAGE } from "@/lib/content";
import { EASE_OUT_CUBIC } from "@/lib/motion";

/**
 * ParentsAppPreview, the ultra-premium "Parents portal in Enroll+ app" preview.
 *
 * Composition:
 *   • LEFT, the rich, hand-built PhoneMockup (titanium body + aurora-washed
 *     display + live attendance ring + sparkline result + fees ledger + glass
 *     tab bar). Floats with a calm scroll-linked drift, NOT a loop.
 *   • RIGHT, editorial copy + a quiet bullet list + a single "coming soon"
 *     acknowledgement (no duplicate download CTA, that lives in the hero).
 *
 * The section sits inside a soft, full-bleed lavender panel so it reads as a
 * deliberate "the app" moment, not just another card. One sanctioned aurora
 * wash, no fabricated metrics.
 */
export function ParentsAppPreview() {
  const reduce = useReducedMotion();

  return (
    <section className="relative scroll-mt-24 py-24 md:py-32">
      <div className="shell">
        <div className="relative overflow-hidden rounded-[2.25rem] border border-white/70 bg-white/60 px-6 py-16 shadow-[0_2px_6px_rgba(38,35,77,0.05),0_36px_72px_-28px_rgba(38,35,77,0.22)] ring-1 ring-navy/[0.05] backdrop-blur-xl sm:px-12 sm:py-20">
          {/* Aurora wash inside the panel (single sanctioned glow). */}
          <div
            aria-hidden
            className="pointer-events-none absolute inset-0 -z-10"
            style={{
              background:
                "radial-gradient(55% 60% at 18% 12%, rgba(108,92,224,0.13) 0%, rgba(108,92,224,0) 65%), radial-gradient(50% 60% at 90% 90%, rgba(60,185,169,0.10) 0%, rgba(60,185,169,0) 65%)",
            }}
          />

          <div className="grid items-center gap-14 lg:grid-cols-[0.95fr_1.05fr] lg:gap-16">
            {/* LEFT, device */}
            <motion.div
              initial={{ opacity: 0, y: reduce ? 0 : 24 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true, amount: 0.25 }}
              transition={{ duration: 0.7, ease: EASE_OUT_CUBIC }}
              className="relative"
            >
              <PhoneMockup />
            </motion.div>

            {/* RIGHT, editorial */}
            <div>
              <motion.p
                initial={{ opacity: 0, y: reduce ? 0 : 12 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true, amount: 0.5 }}
                transition={{ duration: 0.5, ease: EASE_OUT_CUBIC }}
                className="inline-flex items-center gap-2 rounded-full border border-navy/10 bg-white/60 px-3.5 py-1.5 text-[11px] font-bold uppercase tracking-[0.16em] text-accent-deep backdrop-blur"
              >
                <span className="h-1.5 w-1.5 rounded-full bg-accent" />
                {PARENTS_PAGE.app.eyebrow}
              </motion.p>

              <motion.h2
                initial={{ opacity: 0, y: reduce ? 0 : 18 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true, amount: 0.5 }}
                transition={{ duration: 0.55, ease: EASE_OUT_CUBIC, delay: 0.05 }}
                className="mt-5 display text-[2.4rem] leading-[1.04] tracking-tighter text-navy-deep sm:text-[2.9rem] md:text-[3.4rem]"
              >
                {PARENTS_PAGE.app.title}{" "}
                <span className="text-accent">{PARENTS_PAGE.app.titleAccent}</span>
              </motion.h2>

              <motion.p
                initial={{ opacity: 0, y: reduce ? 0 : 14 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true, amount: 0.5 }}
                transition={{ duration: 0.5, ease: EASE_OUT_CUBIC, delay: 0.12 }}
                className="mt-5 max-w-xl text-[16px] leading-relaxed text-ink-2"
              >
                {PARENTS_PAGE.app.lede}
              </motion.p>

              <motion.ul
                initial="hidden"
                whileInView="show"
                viewport={{ once: true, amount: 0.4 }}
                transition={{ staggerChildren: 0.06, delayChildren: 0.18 }}
                className="mt-7 grid max-w-xl gap-3 sm:grid-cols-2"
              >
                {PARENTS_PAGE.app.bullets.map((b) => (
                  <motion.li
                    key={b}
                    variants={{
                      hidden: { opacity: 0, y: reduce ? 0 : 10 },
                      show: { opacity: 1, y: 0, transition: { duration: 0.4, ease: EASE_OUT_CUBIC } },
                    }}
                    className="flex items-start gap-2.5 text-[14px] font-medium text-navy-deep"
                  >
                    <span className="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-accent/12 text-accent-deep">
                      <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
                        <path d="M20 6 9 17l-5-5" />
                      </svg>
                    </span>
                    {b}
                  </motion.li>
                ))}
              </motion.ul>

              {/* Coming-soon acknowledgement (no second download CTA). */}
              <motion.div
                initial={{ opacity: 0, y: reduce ? 0 : 12 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true, amount: 0.5 }}
                transition={{ duration: 0.5, ease: EASE_OUT_CUBIC, delay: 0.24 }}
                className="mt-9 inline-flex items-center gap-3 rounded-2xl border border-navy/10 bg-white/70 px-4 py-3 ring-1 ring-white/60 backdrop-blur"
              >
                <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-accent/12 text-accent-deep">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
                    <circle cx="12" cy="12" r="9" />
                    <path d="M12 7v5l3 2" />
                  </svg>
                </span>
                <p className="text-[13.5px] font-medium leading-snug text-ink-2">
                  {PARENTS_PAGE.app.note}
                </p>
              </motion.div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
