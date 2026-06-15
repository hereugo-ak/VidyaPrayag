"use client";

import { motion } from "framer-motion";
import { Button } from "../ui/Button";
import { Photo } from "../ui/Photo";
import { PHOTOS } from "@/lib/images";
import { fadeUp, EASE_OUT_CUBIC } from "@/lib/motion";

export function Hero() {
  return (
    <section className="relative overflow-hidden pt-32 pb-20 sm:pt-40 md:pb-28">
      <div className="shell grid items-center gap-14 lg:grid-cols-[1.05fr_1fr]">
        {/* Copy */}
        <div>
          <motion.p
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.4, ease: EASE_OUT_CUBIC }}
            className="eyebrow mb-5"
          >
            For schools · teachers · parents
          </motion.p>

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
            className="mt-6 text-sm text-ink-3"
          >
            Set up in minutes — no sales call required.
          </motion.p>
        </div>

        {/* Real photograph — editorial, on-location school environment. */}
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
          {/* Floating data chip — DM Mono, tabular, restrained. */}
          <div className="absolute -left-4 bottom-8 hidden rounded-2xl border border-navy/8 bg-white/85 p-4 shadow-card backdrop-blur-md sm:block">
            <p className="text-[11px] font-semibold uppercase tracking-wide text-ink-3">
              Attendance today
            </p>
            <p className="mt-1 font-mono text-2xl font-medium text-navy-deep">96.4%</p>
            <p className="text-[12px] text-teal-deep">synced to every parent</p>
          </div>
        </motion.div>
      </div>
    </section>
  );
}
