"use client";

import { useRef } from "react";
import { motion, useScroll, useTransform, useReducedMotion } from "framer-motion";
import { Button } from "../ui/Button";
import { Photo } from "../ui/Photo";
import { PHOTOS } from "@/lib/images";
import { fadeUp, EASE_OUT_CUBIC } from "@/lib/motion";

/**
 * Hero — rebuilt to the editorial "Grow+" composition, translated 1:1 into the
 * Enroll+ light system (lavender canvas, navy ink, violet accent — never a dark
 * slab).
 *
 * Structure (Grow+ reference, honest content):
 *   • LEFT column is typography-led. A small underlined stat badge anchors the
 *     top, then an oversized display headline at maximum scale, a hairline rule,
 *     a calm sans subhead, the CTAs, and a compact avatar + rating trust strip
 *     at the very bottom — credibility without clutter.
 *   • RIGHT column is a tall, large-radius portrait of a real school moment with
 *     overlapping frosted-glass UI chips floating over it. These chips are the
 *     Grow+ "question bubble" pattern, but every one maps to a REAL product
 *     surface (a parent's check-in question, a teacher confirming attendance).
 *     No fabricated metrics, no invented numbers — just the live product feel.
 *
 * A single sanctioned aurora bloom sits behind the headline (the only glow on
 * the page) and drifts almost imperceptibly. All motion is reduced-motion aware
 * and settles within ~1.1s.
 */
export function Hero() {
  const reduce = useReducedMotion();
  const ref = useRef<HTMLElement>(null);
  // Scroll-linked parallax: the portrait column drifts up a touch as the hero
  // leaves the viewport — depth, not motion-for-its-own-sake.
  const { scrollYProgress } = useScroll({
    target: ref,
    offset: ["start start", "end start"],
  });
  const photoY = useTransform(scrollYProgress, [0, 1], [0, reduce ? 0 : -56]);
  const auroraY = useTransform(scrollYProgress, [0, 1], [0, reduce ? 0 : 40]);

  return (
    <section ref={ref} className="relative overflow-hidden pt-32 pb-20 sm:pt-36 md:pb-28">
      {/* Aurora bloom — the single sanctioned glow. Sits behind the copy, never
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
          {/* Underlined stat badge (Grow+ "20M+ User / Read Our Success Stories"),
              made honest: the connected roles the platform actually serves. */}
          <motion.div
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.4, ease: EASE_OUT_CUBIC }}
            className="mb-8 inline-flex items-center gap-3"
          >
            <span className="flex -space-x-1.5">
              {["bg-navy-deep", "bg-accent", "bg-teal-deep"].map((c, i) => (
                <span
                  key={i}
                  className={`h-6 w-6 rounded-full ring-2 ring-lavender ${c}`}
                />
              ))}
            </span>
            <span className="text-[13px] leading-tight text-ink-2">
              <span className="font-extrabold text-navy-deep">One platform</span>{" "}
              for{" "}
              <span className="font-semibold text-navy-deep underline decoration-accent/40 decoration-2 underline-offset-[5px]">
                schools, teachers &amp; parents
              </span>
            </span>
          </motion.div>

          {/* Oversized display headline — the Grow+ scale move, on-brand copy. */}
          <motion.h1
            initial={{ opacity: 0, y: 18 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, ease: EASE_OUT_CUBIC, delay: 0.05 }}
            className="display text-[3.4rem] leading-[0.96] tracking-tightest sm:text-7xl md:text-[5.4rem]"
          >
            Run the
            <br />
            whole school<span className="text-accent">.</span>
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
            Enroll+ connects your office, your teachers and every parent —
            attendance, results, fees and messaging, in real time and from one
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

          {/* Compact trust strip (Grow+ avatar + rating), honest framing. */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ duration: 0.5, delay: 0.36 }}
            className="mt-9 flex items-center gap-4"
          >
            <span className="flex h-10 w-10 items-center justify-center rounded-full bg-teal/12 text-teal-deep">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round"><path d="M20 6 9 17l-5-5" /></svg>
            </span>
            <div className="h-9 w-px bg-navy/10" />
            <div>
              <p className="text-[13px] font-bold leading-tight text-navy-deep">
                Set up in minutes
              </p>
              <p className="text-[12.5px] leading-tight text-ink-3">
                No credit card · no sales call required
              </p>
            </div>
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

          {/* Question chip — top-left, offset (Grow+ "How is the fit?"). A real
              question a parent asks the school, in-app. */}
          <motion.div
            initial={{ opacity: 0, y: 14, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            transition={{ duration: 0.55, ease: EASE_OUT_CUBIC, delay: 0.45 }}
            className="absolute -left-4 top-10 hidden sm:block md:-left-8"
          >
            <motion.div
              animate={reduce ? undefined : { y: [0, -7, 0] }}
              transition={reduce ? undefined : { duration: 6, ease: "easeInOut", repeat: Infinity }}
              className="flex items-center gap-2.5 rounded-full border border-white/70 bg-white/85 py-2 pl-2 pr-4 shadow-[0_2px_6px_rgba(38,35,77,0.05),0_18px_40px_-12px_rgba(38,35,77,0.20)] backdrop-blur-md"
            >
              <span className="flex h-7 w-7 items-center justify-center rounded-full bg-accent/12 text-accent-deep">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round"><path d="M20 6 9 17l-5-5" /></svg>
              </span>
              <span className="text-[13px] font-semibold text-navy-deep">
                Was my child present today?
              </span>
            </motion.div>
          </motion.div>

          {/* Question chip — second, slightly lower & inset (Grow+ "Do you like
              the design?"). A teacher-side confirmation, in-app. */}
          <motion.div
            initial={{ opacity: 0, y: 14, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            transition={{ duration: 0.55, ease: EASE_OUT_CUBIC, delay: 0.58 }}
            className="absolute left-6 top-28 hidden sm:block md:left-10"
          >
            <motion.div
              animate={reduce ? undefined : { y: [0, -6, 0] }}
              transition={reduce ? undefined : { duration: 7, ease: "easeInOut", repeat: Infinity, delay: 0.6 }}
              className="flex items-center gap-2.5 rounded-full border border-white/70 bg-white/85 py-2 pl-2 pr-4 shadow-[0_2px_6px_rgba(38,35,77,0.05),0_18px_40px_-12px_rgba(38,35,77,0.20)] backdrop-blur-md"
            >
              <span className="flex h-7 w-7 items-center justify-center rounded-full bg-teal/15 text-teal-deep">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round"><path d="M20 6 9 17l-5-5" /></svg>
              </span>
              <span className="text-[13px] font-semibold text-navy-deep">
                Attendance synced to parents
              </span>
            </motion.div>
          </motion.div>

          {/* Frosted stat card — top-right (Grow+ "60% More sales this week"),
              made honest: the platform's real-time delivery promise. */}
          <motion.div
            initial={{ opacity: 0, y: -14, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            transition={{ duration: 0.55, ease: EASE_OUT_CUBIC, delay: 0.66 }}
            className="absolute -right-3 top-6 hidden md:block lg:-right-7"
          >
            <motion.div
              animate={reduce ? undefined : { y: [0, 8, 0] }}
              transition={reduce ? undefined : { duration: 7.5, ease: "easeInOut", repeat: Infinity, delay: 0.4 }}
              className="w-[182px] rounded-[1.4rem] border border-white/55 bg-white/30 p-5 shadow-[0_2px_6px_rgba(38,35,77,0.05),0_24px_50px_-12px_rgba(38,35,77,0.22)] backdrop-blur-xl"
            >
              <p className="text-[10px] font-bold uppercase tracking-[0.16em] text-ink-3">
                — In real time
              </p>
              <p className="mt-2 display text-[2.2rem] leading-none tracking-tighter text-navy-deep">
                One
              </p>
              <p className="mt-2 text-[12.5px] leading-snug text-ink-2">
                source of truth across web &amp; mobile
              </p>
            </motion.div>
          </motion.div>

          {/* Role card — bottom-right (Grow+ product card). Mirrors the real
              role model: the connected people the platform links. */}
          <motion.div
            initial={{ opacity: 0, y: 16, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            transition={{ duration: 0.55, ease: EASE_OUT_CUBIC, delay: 0.78 }}
            className="absolute -bottom-6 right-2 hidden sm:block md:right-6"
          >
            <motion.div
              animate={reduce ? undefined : { y: [0, -6, 0] }}
              transition={reduce ? undefined : { duration: 6.5, ease: "easeInOut", repeat: Infinity, delay: 1 }}
              className="flex items-center gap-3.5 rounded-[1.4rem] border border-white/70 bg-white/85 p-3.5 pr-5 shadow-[0_2px_6px_rgba(38,35,77,0.05),0_24px_50px_-12px_rgba(38,35,77,0.22)] backdrop-blur-md"
            >
              <span className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-navy-deep text-white">
                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" /><circle cx="9" cy="7" r="4" /><path d="M23 21v-2a4 4 0 0 0-3-3.87" /><path d="M16 3.13a4 4 0 0 1 0 7.75" /></svg>
              </span>
              <div>
                <p className="text-[13.5px] font-extrabold leading-tight text-navy-deep">
                  Admin · Teacher · Parent
                </p>
                <p className="mt-0.5 text-[11.5px] leading-tight text-ink-3">
                  Every role, one connected backend
                </p>
              </div>
            </motion.div>
          </motion.div>
        </motion.div>
      </div>

      {/* Trust strip — the boards & standards Enroll+ is built around, rendered
          as a clean monochrome wordmark row. No borrowed brand logos. */}
      <motion.div
        initial={{ opacity: 0, y: reduce ? 0 : 14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, ease: EASE_OUT_CUBIC, delay: 0.85 }}
        className="shell mt-24 md:mt-32"
      >
        <p className="text-center text-[11px] font-semibold uppercase tracking-[0.2em] text-ink-3/70">
          Built around how Indian schools actually run
        </p>
        <div className="mt-6 flex flex-wrap items-center justify-center gap-x-10 gap-y-4 text-ink-3 sm:gap-x-14">
          {["CBSE", "ICSE", "State Boards", "Multi-medium", "K–12"].map((b) => (
            <span
              key={b}
              className="text-[15px] font-extrabold tracking-tight text-navy-deep/35 transition-colors duration-200 hover:text-navy-deep/60"
            >
              {b}
            </span>
          ))}
        </div>
      </motion.div>
    </section>
  );
}
