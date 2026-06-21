"use client";

import { motion, useReducedMotion } from "framer-motion";
import { fadeUp, staggerContainer, viewportOnce } from "@/lib/motion";

// For users who prefer reduced motion: a plain, instant opacity fade with no
// transform/scale/blur, keeps the page accessible without the "settle".
const reducedFade = {
  hidden: { opacity: 0 },
  show: { opacity: 1, transition: { duration: 0.2 } },
} as const;

/**
 * Reveal, settles a block into focus once when it enters the viewport
 * (fade + 22px rise + a whisper of scale & blur, see lib/motion fadeUp).
 * `delay` nudges sequence without a stagger container. Plays ONCE.
 */
export function Reveal({
  children,
  className = "",
  delay = 0,
}: {
  children: React.ReactNode;
  className?: string;
  delay?: number;
}) {
  const reduce = useReducedMotion();
  return (
    <motion.div
      className={className}
      variants={reduce ? reducedFade : fadeUp}
      initial="hidden"
      whileInView="show"
      viewport={viewportOnce}
      transition={{ delay }}
    >
      {children}
    </motion.div>
  );
}

/** Stagger container, children should be <RevealItem>. Cascades once. */
export function RevealGroup({
  children,
  className = "",
}: {
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <motion.div
      className={className}
      variants={staggerContainer}
      initial="hidden"
      whileInView="show"
      viewport={viewportOnce}
    >
      {children}
    </motion.div>
  );
}

export function RevealItem({
  children,
  className = "",
}: {
  children: React.ReactNode;
  className?: string;
}) {
  const reduce = useReducedMotion();
  return (
    <motion.div className={className} variants={reduce ? reducedFade : fadeUp}>
      {children}
    </motion.div>
  );
}
