"use client";

import { motion } from "framer-motion";
import { Button } from "../ui/Button";
import { Photo } from "../ui/Photo";
import { PHOTOS } from "@/lib/images";
import { fadeUp, EASE_OUT_CUBIC } from "@/lib/motion";

/**
 * Hero — the page's first impression. Ultra-premium by restraint:
 *   • a single soft aurora bloom anchored behind the headline (the only glow on
 *     the page, low-opacity, part of the lavender system — not decoration)
 *   • a live status pill that reads like a real product, not a marketing badge
 *   • the editorial hero photograph carries one floating glass stat card whose
 *     numbers are honest platform facts (one backend, real-time), never invented
 * Everything animates once, fast, and settles.
 */
export function Hero() {
  return (
    <section className="relative overflow-hidden pt-32 pb-20 sm:pt-40 md:pb-28">
      {/* Aurora bloom — the single sanctioned glow. Sits behind the copy, never
          competes with it. Pure CSS radial, no image, no layout cost. */}
      <div
        aria-hidden
        className="pointer-events-none absolute -top-24 left-1/2 -z-10 h-[520px] w-[820px] -translate-x-1/2 opacity-70"
        style={{
          background:
            "radial-gradient(45% 50% at 30% 35%, rgba(108,92,224,0.16) 0%, rgba(108,92,224,0) 70%), radial-gradient(40% 45% at 72% 30%, rgba(60,185,169,0.10) 0%, rgba(60,185,169,0) 70%)",
        }}
      />

      <div className="shell grid items-center gap-14 lg:grid-cols-[1.05fr_1fr]">
        {/* Copy */}
        <div>
          <motion.div
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.4, ease: EASE_OUT_CUBIC }}
            className="mb-6 inline-flex items-center gap-2 rounded-full border border-navy/10 bg-white/60 py-1.5 pl-2 pr-3.5 text-[12px] font-semibold text-ink-2 shadow-[0_1px_2px_rgba(38,35,77,0.04)] backdrop-blur"
          >
            <span className="relative flex h-2 w-2">
              <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-teal opacity-60" />
              <span className="relative inline-flex h-2 w-2 rounded-full bg-teal-deep" />
            </span>
            One platform · schools, teachers &amp; parents
          </motion.div>

          {/* Headline — under seven words. */}
          <motion.h1
            initial={{ opacity: 0, y: 18 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, ease: EASE_OUT_CUBIC, delay: 0.05 }}
            className="display text-[2.6rem] leading-[1.04] sm:text-6xl md:text-[4rem]"
          >
            Run your whole school
            <br />
            <span className="text-accent">on one platform.</span>
          </motion.h1>

          <motion.p
            initial={{ opacity: 0, y: 18 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, ease: EASE_OUT_CUBIC, delay: 0.12 }}
            className="mt-6 max-w-xl text-lg leading-relaxed text-ink-2"
          >
            Enroll+ connects your office, your teachers and every parent — attendance,
            results, fees and messaging, in real time and from one source of truth.
          </motion.p>

          <motion.div
            initial={{ opacity: 0, y: 18 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, ease: EASE_OUT_CUBIC, delay: 0.18 }}
            className="mt-9 flex flex-col gap-3 sm:flex-row sm:items-center"
          >
            <Button href="/onboarding" size="lg">
              Onboard your school →
            </Button>
            <Button href="/#how-it-works" variant="secondary" size="lg">
              See how it works
            </Button>
          </motion.div>

          <motion.p
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ duration: 0.5, delay: 0.3 }}
            className="mt-6 flex items-center gap-2 text-sm text-ink-3"
          >
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="text-teal-deep">
              <path d="M20 6 9 17l-5-5" />
            </svg>
            Set up in minutes — no sales call required.
          </motion.p>
        </div>

        {/* Real photograph — editorial, on-location school environment, with one
            floating glass stat card of honest platform facts. */}
        <motion.div
          variants={fadeUp}
          initial="hidden"
          animate="show"
          transition={{ duration: 0.6, ease: EASE_OUT_CUBIC, delay: 0.12 }}
          className="relative"
        >
          <Photo
            photo={PHOTOS.hero}
            ratio="4/5"
            priority
            sizes="(max-width: 1024px) 100vw, 48vw"
            className="shadow-cardHover"
          />

          {/* Floating glass card — anchored bottom-left, lifts on the photo.
              Numbers are honest platform facts, not customer vanity metrics. */}
          <motion.div
            initial={{ opacity: 0, y: 16, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            transition={{ duration: 0.55, ease: EASE_OUT_CUBIC, delay: 0.45 }}
            className="absolute -bottom-5 -left-4 hidden w-[230px] rounded-2xl border border-white/70 bg-white/85 p-4 shadow-cardHover backdrop-blur-md sm:block md:-left-8"
          >
            <div className="flex items-center gap-2">
              <span className="relative flex h-2 w-2">
                <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-teal opacity-60" />
                <span className="relative inline-flex h-2 w-2 rounded-full bg-teal-deep" />
              </span>
              <p className="text-[11px] font-bold uppercase tracking-[0.12em] text-ink-3">
                Live, one backend
              </p>
            </div>
            <div className="mt-3 grid grid-cols-2 gap-3">
              <div>
                <p className="nums text-[22px] font-extrabold leading-none tracking-tight text-navy-deep">
                  4
                </p>
                <p className="mt-1 text-[11px] leading-tight text-ink-3">
                  Connected roles
                </p>
              </div>
              <div>
                <p className="nums text-[22px] font-extrabold leading-none tracking-tight text-navy-deep">
                  20+
                </p>
                <p className="mt-1 text-[11px] leading-tight text-ink-3">
                  Built-in modules
                </p>
              </div>
            </div>
          </motion.div>
        </motion.div>
      </div>
    </section>
  );
}
