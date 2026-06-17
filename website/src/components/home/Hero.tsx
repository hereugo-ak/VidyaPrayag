"use client";

import { useRef } from "react";
import { motion, useScroll, useTransform, useReducedMotion } from "framer-motion";
import { Button } from "../ui/Button";
import { Photo } from "../ui/Photo";
import { PHOTOS } from "@/lib/images";
import { fadeUp, EASE_OUT_CUBIC } from "@/lib/motion";

/**
 * Hero, rebuilt to the editorial "Grow+" composition, translated 1:1 into the
 * Enroll+ light system (lavender canvas, navy ink, violet accent, never a dark
 * slab).
 *
 * Structure (Grow+ reference, honest content):
 *   • LEFT column is typography-led: an eyebrow of the connected roles, an
 *     oversized display headline at maximum scale, a hairline rule, a calm sans
 *     subhead, then the two CTAs. Restraint over clutter, no vanity trust row.
 *   • RIGHT column is a tall, large-radius portrait of a real school moment.
 *     No fabricated metrics, no invented numbers, just the live product feel.
 *
 * A single sanctioned aurora bloom sits behind the headline (the only glow on
 * the page) and drifts almost imperceptibly. All motion is reduced-motion aware
 * and settles within ~1.1s.
 */
export function Hero() {
  const reduce = useReducedMotion();
  const ref = useRef<HTMLElement>(null);
  // Scroll-linked parallax: the portrait column drifts up a touch as the hero
  // leaves the viewport, depth, not motion-for-its-own-sake.
  const { scrollYProgress } = useScroll({
    target: ref,
    offset: ["start start", "end start"],
  });
  const photoY = useTransform(scrollYProgress, [0, 1], [0, reduce ? 0 : -56]);
  const auroraY = useTransform(scrollYProgress, [0, 1], [0, reduce ? 0 : 40]);

  return (
    <section ref={ref} className="relative overflow-hidden pt-32 pb-20 sm:pt-36 md:pb-28">
      {/* Aurora bloom, the single sanctioned glow. Sits behind the copy, never
          competes with it. Pure CSS radial, no image, no layout cost. */}
      <motion.div
        aria-hidden
        style={{ y: auroraY }}
        className="pointer-events-none absolute -top-24 left-1/2 -z-10 h-[560px] w-[900px] -translate-x-1/2"
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
              "radial-gradient(45% 50% at 28% 34%, rgba(108,92,224,0.16) 0%, rgba(108,92,224,0) 70%), radial-gradient(40% 45% at 74% 28%, rgba(60,185,169,0.10) 0%, rgba(60,185,169,0) 70%)",
          }}
        />
      </motion.div>

      <div className="shell grid items-center gap-12 lg:grid-cols-[1.08fr_1fr] lg:gap-10">
        {/* ── LEFT: typography-led column ─────────────────────────────────── */}
        <div>
          {/* Eyebrow, the connected roles the platform actually serves. */}
          <motion.p
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.4, ease: EASE_OUT_CUBIC }}
            className="mb-7 text-[12px] font-bold uppercase tracking-[0.2em] text-accent"
          >
            For schools · teachers · parents
          </motion.p>

          {/* Oversized display headline, the Grow+ scale move, on-brand copy. */}
          <motion.h1
            initial={{ opacity: 0, y: 18 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, ease: EASE_OUT_CUBIC, delay: 0.05 }}
            className="display text-[3.4rem] leading-[0.96] tracking-tightest sm:text-7xl md:text-[5.4rem]"
          >
            Run your
            <br />
            whole school
            <br />
            <span className="text-accent">
              on one platform<span className="tracking-tighter">...</span>
            </span>
          </motion.h1>

          {/* Hairline rule (Grow+ divider) between headline and subhead. */}
          <motion.div
            initial={{ opacity: 0, scaleX: 0 }}
            animate={{ opacity: 1, scaleX: 1 }}
            transition={{ duration: 0.6, ease: EASE_OUT_CUBIC, delay: 0.16 }}
            style={{ transformOrigin: "left" }}
            className="mt-8 h-px w-full max-w-xl bg-gradient-to-r from-navy/15 via-navy/10 to-transparent"
          />

          <motion.p
            initial={{ opacity: 0, y: 18 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, ease: EASE_OUT_CUBIC, delay: 0.2 }}
            className="mt-7 max-w-xl text-lg leading-relaxed text-ink-2"
          >
            Enroll+ connects your office, your teachers and every parent.
            Attendance, results, fees and messaging, in real time and from one
            source of truth.
          </motion.p>

          <motion.div
            initial={{ opacity: 0, y: 18 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, ease: EASE_OUT_CUBIC, delay: 0.26 }}
            className="mt-9 flex flex-col gap-3 sm:flex-row sm:items-center"
          >
            <Button href="/onboarding" size="lg">
              Onboard your school →
            </Button>
            <Button href="/#how-it-works" variant="secondary" size="lg">
              See how it works
            </Button>
          </motion.div>
        </div>

        {/* ── RIGHT: portrait + overlapping glass chips (Grow+ pattern) ────── */}
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
              rounded="rounded-[2rem]"
              className="shadow-cardHover"
            />
          </motion.div>
        </motion.div>
      </div>
    </section>
  );
}
