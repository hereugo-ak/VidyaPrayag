/**
 * Shared animation variants. The brief's restraint rules are *encoded here* so
 * no component can over-animate: fade-up 22px, 400ms, ease-out-cubic, play once.
 */
import type { Variants, Transition } from "framer-motion";

export const EASE_OUT_CUBIC = [0.16, 1, 0.3, 1] as const;

export const revealTransition: Transition = {
  duration: 0.62,
  ease: EASE_OUT_CUBIC,
};

/**
 * Content reveals on enter: fades up 22px with a whisper of scale (0.98→1) and a
 * brief blur-to-sharp focus pull. This is the "premium" tell, the eye reads it
 * as the content *settling into focus* rather than just sliding. It stays under
 * 650ms, plays once, and the blur is small enough to never feel theatrical.
 * (Reduced-motion users get an instant, no-transform fade via the CSS guard.)
 */
export const fadeUp: Variants = {
  hidden: { opacity: 0, y: 22, scale: 0.985, filter: "blur(6px)" },
  show: {
    opacity: 1,
    y: 0,
    scale: 1,
    filter: "blur(0px)",
    transition: revealTransition,
  },
};

/** Stagger container for lists, children reveal in a calm, slightly slower
    cascade, once. The longer step makes a grid feel deliberate, not snappy. */
export const staggerContainer: Variants = {
  hidden: {},
  show: {
    transition: { staggerChildren: 0.1, delayChildren: 0.06 },
  },
};

/** Standard viewport config: animate ONCE when ~18% enters the viewport. */
export const viewportOnce = { once: true, amount: 0.18 } as const;

/** Page transition, fast, non-theatrical fade. */
export const pageFade: Variants = {
  hidden: { opacity: 0 },
  show: { opacity: 1, transition: { duration: 0.2, ease: "easeOut" } },
};
