/**
 * Shared animation variants. The brief's restraint rules are *encoded here* so
 * no component can over-animate: fade-up 22px, 400ms, ease-out-cubic, play once.
 */
import type { Variants, Transition } from "framer-motion";

export const EASE_OUT_CUBIC = [0.16, 1, 0.3, 1] as const;

export const revealTransition: Transition = {
  duration: 0.4,
  ease: EASE_OUT_CUBIC,
};

/** Content fades up 22px on enter. Triggered once (viewport once:true at call site). */
export const fadeUp: Variants = {
  hidden: { opacity: 0, y: 22 },
  show: { opacity: 1, y: 0, transition: revealTransition },
};

/** Stagger container for lists — children reveal in a calm cascade, once. */
export const staggerContainer: Variants = {
  hidden: {},
  show: {
    transition: { staggerChildren: 0.08, delayChildren: 0.04 },
  },
};

/** Standard viewport config: animate ONCE when ~18% enters the viewport. */
export const viewportOnce = { once: true, amount: 0.18 } as const;

/** Page transition — fast, non-theatrical fade. */
export const pageFade: Variants = {
  hidden: { opacity: 0 },
  show: { opacity: 1, transition: { duration: 0.2, ease: "easeOut" } },
};
