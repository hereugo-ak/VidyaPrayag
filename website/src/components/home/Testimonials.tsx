"use client";

import { motion, useReducedMotion } from "framer-motion";
import { EASE_OUT_CUBIC } from "@/lib/motion";

/**
 * Partner schools, a deliberate, confident pre-launch statement.
 *
 * Per the brief: no fabricated quotes, schools or logos. Instead of faking
 * social proof, this section makes the absence intentional and premium. It is
 * NOT a placeholder, it is a designed moment from a company that hasn't
 * launched yet and isn't hiding it.
 *
 * Composition (all in the Enroll+ light system, navy ink on lavender):
 *   • A centred eyebrow + oversized display line that owns the position
 *     ("We'd rather show you the product than borrow a quote.").
 *   • A hairline rule and a calm sub-line of honest framing.
 *   • A quiet "Partner schools" status row: a soft pulsing dot beside a tracked
 *     "Coming soon" label, set on a single full-width hairline that reads as a
 *     reserved space for the first named school, not an empty card grid.
 *
 * The typographic scale and generous whitespace do the work; the only motion is
 * a near-imperceptible reveal, reduced-motion aware.
 */
export function Testimonials() {
  const reduce = useReducedMotion();

  return (
    <section className="py-24 md:py-32">
      <div className="shell">
        <motion.div
          initial={{ opacity: 0, y: reduce ? 0 : 16 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, amount: 0.4 }}
          transition={{ duration: 0.55, ease: EASE_OUT_CUBIC }}
          className="mx-auto max-w-4xl text-center"
        >
          <p className="eyebrow mb-5">Why schools choose us</p>

          <h2 className="display text-[2rem] leading-[1.04] tracking-tighter text-navy-deep sm:text-5xl md:text-[3.4rem]">
            We&apos;d rather show you the product
            <br className="hidden sm:block" /> than borrow a{" "}
            <span className="text-accent">quote.</span>
          </h2>

          {/* Hairline rule, centred, the same divider language as the hero. */}
          <div
            aria-hidden
            className="mx-auto mt-10 h-px w-full max-w-md bg-gradient-to-r from-transparent via-navy/15 to-transparent"
          />

          <p className="mx-auto mt-8 max-w-xl text-lg leading-relaxed text-ink-2">
            Customer stories will live here once our first partner schools are
            ready to be named. Until then, we let the platform speak for itself:
            every number it shows is computed from real entries, never invented.
          </p>
        </motion.div>

        {/* Partner-schools status band, a reserved, confident space for the
            first named institution, not an empty placeholder grid. */}
        <motion.div
          initial={{ opacity: 0, y: reduce ? 0 : 16 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, amount: 0.4 }}
          transition={{ duration: 0.55, ease: EASE_OUT_CUBIC, delay: 0.08 }}
          className="mx-auto mt-16 flex max-w-3xl items-center justify-center gap-4 border-t border-navy/10 pt-10"
        >
          <span className="relative flex h-2.5 w-2.5 shrink-0">
            {!reduce && (
              <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-accent/40" />
            )}
            <span className="relative inline-flex h-2.5 w-2.5 rounded-full bg-accent" />
          </span>
          <p className="text-[12px] font-bold uppercase tracking-[0.28em] text-ink-3">
            Partner schools · Coming soon
          </p>
        </motion.div>
      </div>
    </section>
  );
}
