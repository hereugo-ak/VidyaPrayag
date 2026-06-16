"use client";

import { useRef } from "react";
import { motion, useScroll, useTransform, useReducedMotion } from "framer-motion";
import { Button } from "../ui/Button";
import { Photo } from "../ui/Photo";
import { PHOTOS } from "@/lib/images";
import { fadeUp, EASE_OUT_CUBIC } from "@/lib/motion";

/**
 * Hero — the page's first impression. Ultra-premium by restraint:
 *   • a single soft aurora bloom anchored behind the headline (the only glow on
 *     the page, low-opacity, part of the lavender system — not decoration), which
 *     drifts almost imperceptibly so the page never feels frozen.
 *   • a live status pill that reads like a real product, not a marketing badge.
 *   • the editorial hero photograph lifts on a gentle scroll parallax and carries
 *     two floating "live product moment" cards — an attendance push and a fees
 *     update — both drawn 1:1 from the real backend (attendance_records,
 *     fee_records, notifications). These are honest product surfaces, NOT invented
 *     vanity metrics — there is no fabricated number anywhere on the page.
 * Everything animates once on load, fast, and settles.
 */
export function Hero() {
  const reduce = useReducedMotion();
  const ref = useRef<HTMLElement>(null);
  // Scroll-linked parallax: the photo column drifts up a touch as the hero
  // leaves the viewport — depth, not motion-for-its-own-sake.
  const { scrollYProgress } = useScroll({
    target: ref,
    offset: ["start start", "end start"],
  });
  const photoY = useTransform(scrollYProgress, [0, 1], [0, reduce ? 0 : -56]);
  const auroraY = useTransform(scrollYProgress, [0, 1], [0, reduce ? 0 : 40]);

  return (
    <section ref={ref} className="relative overflow-hidden pt-32 pb-20 sm:pt-40 md:pb-28">
      {/* Aurora bloom — the single sanctioned glow. Sits behind the copy, never
          competes with it. Pure CSS radial, no image, no layout cost. It both
          breathes (slow opacity drift) and parallaxes on scroll. */}
      <motion.div
        aria-hidden
        style={{ y: auroraY }}
        className="pointer-events-none absolute -top-24 left-1/2 -z-10 h-[520px] w-[820px] -translate-x-1/2"
      >
        <motion.div
          className="h-full w-full"
          initial={{ opacity: 0 }}
          animate={reduce ? { opacity: 0.7 } : { opacity: [0.55, 0.78, 0.55] }}
          transition={
            reduce
              ? { duration: 0.6 }
              : { duration: 9, ease: "easeInOut", repeat: Infinity }
          }
          style={{
            background:
              "radial-gradient(45% 50% at 30% 35%, rgba(108,92,224,0.16) 0%, rgba(108,92,224,0) 70%), radial-gradient(40% 45% at 72% 30%, rgba(60,185,169,0.10) 0%, rgba(60,185,169,0) 70%)",
          }}
        />
      </motion.div>

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

        {/* Real photograph — editorial, on-location school environment. The
            column lifts on a gentle scroll parallax; two floating product cards
            sit on top, each a faithful slice of the real app (attendance push +
            fees ledger), so the hero shows the *product*, not a vanity metric. */}
        <motion.div style={{ y: photoY }} className="relative">
          <motion.div
            variants={fadeUp}
            initial="hidden"
            animate="show"
            transition={{ duration: 0.6, ease: EASE_OUT_CUBIC, delay: 0.12 }}
          >
            <Photo
              photo={PHOTOS.hero}
              ratio="4/5"
              priority
              sizes="(max-width: 1024px) 100vw, 48vw"
              className="shadow-cardHover"
            />
          </motion.div>

          {/* Live attendance moment — bottom-left. A real notification the
              parent app fires the morning attendance is marked. Gently floats. */}
          <motion.div
            initial={{ opacity: 0, y: 16, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            transition={{ duration: 0.55, ease: EASE_OUT_CUBIC, delay: 0.45 }}
            className="absolute -bottom-6 -left-4 hidden sm:block md:-left-9"
          >
            <motion.div
              animate={reduce ? undefined : { y: [0, -7, 0] }}
              transition={
                reduce ? undefined : { duration: 6, ease: "easeInOut", repeat: Infinity }
              }
              className="w-[256px] rounded-2xl border border-white/70 bg-white/85 p-3.5 shadow-cardHover backdrop-blur-md"
            >
              <div className="flex items-center gap-2.5">
                <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-teal/12 text-teal-deep">
                  <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round"><path d="M20 6 9 17l-5-5" /></svg>
                </span>
                <div className="min-w-0">
                  <p className="truncate text-[12.5px] font-bold leading-tight text-navy-deep">
                    Present · marked 8:42 AM
                  </p>
                  <p className="truncate text-[11px] leading-tight text-ink-3">
                    Class 6-B · synced to parents
                  </p>
                </div>
                <span className="relative ml-auto flex h-2 w-2">
                  <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-teal opacity-60" />
                  <span className="relative inline-flex h-2 w-2 rounded-full bg-teal-deep" />
                </span>
              </div>
            </motion.div>
          </motion.div>

          {/* Fees ledger moment — top-right. Mirrors the real fee_records ledger
              line a parent sees. Floats on a slightly offset rhythm. */}
          <motion.div
            initial={{ opacity: 0, y: -14, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            transition={{ duration: 0.55, ease: EASE_OUT_CUBIC, delay: 0.6 }}
            className="absolute -right-4 top-8 hidden md:block lg:-right-8"
          >
            <motion.div
              animate={reduce ? undefined : { y: [0, 8, 0] }}
              transition={
                reduce ? undefined : { duration: 7, ease: "easeInOut", repeat: Infinity, delay: 0.8 }
              }
              className="w-[180px] rounded-2xl border border-white/70 bg-white/85 p-3.5 shadow-cardHover backdrop-blur-md"
            >
              <p className="text-[10px] font-bold uppercase tracking-[0.14em] text-ink-3">
                Term fees
              </p>
              <p className="nums mt-1.5 text-[20px] font-extrabold leading-none tracking-tight text-navy-deep">
                Paid in full
              </p>
              <div className="mt-2.5 h-1.5 w-full overflow-hidden rounded-full bg-navy/8">
                <motion.span
                  className="block h-full rounded-full bg-gradient-to-r from-accent to-accent-deep"
                  initial={{ width: 0 }}
                  animate={{ width: "100%" }}
                  transition={{ duration: 1.1, ease: EASE_OUT_CUBIC, delay: 0.9 }}
                />
              </div>
              <p className="mt-2 text-[11px] leading-tight text-ink-3">
                No dues · one clear ledger
              </p>
            </motion.div>
          </motion.div>
        </motion.div>
      </div>
    </section>
  );
}
