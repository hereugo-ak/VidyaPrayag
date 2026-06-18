"use client";

import { useRef } from "react";
import { motion, useScroll, useTransform, useReducedMotion } from "framer-motion";
import { Photo } from "../ui/Photo";
import { DownloadApp } from "./DownloadApp";
import { PHOTOS } from "@/lib/images";
import { PARENTS_PAGE } from "@/lib/content";
import { fadeUp, EASE_OUT_CUBIC } from "@/lib/motion";

/**
 * ParentsHero, the dedicated hero for the /parents experience.
 *
 * It deliberately echoes the homepage Hero's editorial composition (so the page
 * feels like the same product) but stands on its own: a parent-first eyebrow,
 * an oversized display headline, a hairline rule, calm subhead, the
 * coming-soon DownloadApp control, then a tall portrait of a real parent-and-
 * child moment with two overlapping glass "live update" chips that hint at the
 * app surface. One sanctioned aurora bloom sits behind the copy; all motion is
 * reduced-motion aware and settles fast.
 */
export function ParentsHero() {
  const reduce = useReducedMotion();
  const ref = useRef<HTMLElement>(null);
  const { scrollYProgress } = useScroll({
    target: ref,
    offset: ["start start", "end start"],
  });
  const photoY = useTransform(scrollYProgress, [0, 1], [0, reduce ? 0 : -56]);
  const auroraY = useTransform(scrollYProgress, [0, 1], [0, reduce ? 0 : 40]);

  return (
    <section ref={ref} className="relative overflow-hidden pt-32 pb-20 sm:pt-36 md:pb-28">
      {/* Single sanctioned aurora bloom behind the copy. */}
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
              "radial-gradient(45% 50% at 30% 32%, rgba(108,92,224,0.16) 0%, rgba(108,92,224,0) 70%), radial-gradient(40% 45% at 76% 26%, rgba(60,185,169,0.10) 0%, rgba(60,185,169,0) 70%)",
          }}
        />
      </motion.div>

      <div className="shell grid items-center gap-12 lg:grid-cols-[1.05fr_1fr] lg:gap-12">
        {/* LEFT, typography-led column */}
        <div>
          <motion.p
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.4, ease: EASE_OUT_CUBIC }}
            className="mb-7 inline-flex items-center gap-2 rounded-full border border-navy/10 bg-white/60 px-3.5 py-1.5 text-[11px] font-bold uppercase tracking-[0.16em] text-accent-deep backdrop-blur"
          >
            <span className="h-1.5 w-1.5 rounded-full bg-accent" />
            {PARENTS_PAGE.hero.eyebrow}
          </motion.p>

          <motion.h1
            initial={{ opacity: 0, y: 18 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, ease: EASE_OUT_CUBIC, delay: 0.05 }}
            className="display text-[3.1rem] leading-[0.98] tracking-tightest sm:text-6xl md:text-[4.6rem]"
          >
            {PARENTS_PAGE.hero.title}
            <br />
            <span className="text-accent">{PARENTS_PAGE.hero.titleAccent}</span>
          </motion.h1>

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
            {PARENTS_PAGE.hero.lede}
          </motion.p>

          {/* App download control (coming soon). */}
          <motion.div
            initial={{ opacity: 0, y: 18 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, ease: EASE_OUT_CUBIC, delay: 0.26 }}
            className="mt-9"
          >
            <DownloadApp variant="hero" />
          </motion.div>
        </div>

        {/* RIGHT, portrait + overlapping glass chips */}
        <motion.div style={{ y: photoY }} className="relative">
          <motion.div
            variants={fadeUp}
            initial="hidden"
            animate="show"
            transition={{ duration: 0.6, ease: EASE_OUT_CUBIC, delay: 0.12 }}
          >
            <Photo
              photo={PHOTOS.parentsHero}
              ratio="4/5"
              priority
              sizes="(max-width: 1024px) 100vw, 46vw"
              rounded="rounded-[2rem]"
              className="shadow-cardHover"
            />
          </motion.div>
        </motion.div>
      </div>
    </section>
  );
}
