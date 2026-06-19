"use client";

import { useEffect, useState } from "react";
import { AnimatePresence, motion, useReducedMotion } from "framer-motion";
import { EASE_OUT_CUBIC } from "@/lib/motion";

/**
 * DownloadApp, the parents-page app acquisition control.
 *
 * The Enroll+ parent app is not on the stores yet, so the brief is explicit:
 * the button must *look* fully shippable but, on click, calmly say "coming
 * soon" rather than dead-linking to a 404 store page. We honour that with a
 * single primary "Download the app" pill plus two real store-shaped buttons,
 * all of which resolve to the same in-place "Coming soon" acknowledgement, an
 * inline note under the buttons (announced to screen readers) and a brief, calm
 * toast. Nothing navigates away; nothing lies about availability.
 *
 * `variant="hero"` renders the larger hero treatment (primary pill + store
 * badges); `variant="inline"` renders a compact single pill for use lower down
 * the page. Both share the same coming-soon behaviour.
 */
export function DownloadApp({
  variant = "hero",
  className = "",
}: {
  variant?: "hero" | "inline";
  className?: string;
}) {
  const reduce = useReducedMotion();
  const [announced, setAnnounced] = useState(false);

  // Auto-dismiss the toast after a calm beat so it never lingers.
  useEffect(() => {
    if (!announced) return;
    const t = setTimeout(() => setAnnounced(false), 3200);
    return () => clearTimeout(t);
  }, [announced]);

  const ping = () => setAnnounced(true);

  /* Shared store-badge button: a real, tappable, store-shaped control that
     resolves to the coming-soon acknowledgement instead of a dead store link. */
  const StoreBadge = ({
    store,
    glyph,
    line1,
    line2,
  }: {
    store: string;
    glyph: React.ReactNode;
    line1: string;
    line2: string;
  }) => (
    <motion.button
      type="button"
      onClick={ping}
      whileHover={reduce ? undefined : { scale: 1.02 }}
      whileTap={reduce ? undefined : { scale: 0.99 }}
      transition={{ type: "spring", stiffness: 420, damping: 30 }}
      aria-label={`Get Enroll+ for parents on the ${store} (coming soon)`}
      className="group inline-flex h-14 items-center gap-3 rounded-2xl border border-navy/12 bg-white/80 px-5 text-left shadow-[0_1px_2px_rgba(38,35,77,0.04),0_10px_28px_-14px_rgba(38,35,77,0.22)] ring-1 ring-inset ring-white/60 backdrop-blur transition-colors duration-200 ease-out-cubic hover:border-navy/20 hover:bg-white"
    >
      <span className="flex h-8 w-8 shrink-0 items-center justify-center text-navy-deep">
        {glyph}
      </span>
      <span className="leading-tight">
        <span className="block text-[10px] font-semibold uppercase tracking-[0.12em] text-ink-3">
          {line1}
        </span>
        <span className="block text-[15px] font-extrabold tracking-tight text-navy-deep">
          {line2}
        </span>
      </span>
    </motion.button>
  );

  return (
    <div className={`relative ${className}`}>
      {variant === "hero" ? (
        <div className="flex flex-col gap-4">
          {/* Primary, unmistakably the main action. */}
          <motion.button
            type="button"
            onClick={ping}
            whileHover={reduce ? undefined : { scale: 1.02 }}
            whileTap={reduce ? undefined : { scale: 0.99 }}
            transition={{ type: "spring", stiffness: 420, damping: 30 }}
            className="inline-flex h-14 items-center justify-center gap-2.5 self-start rounded-full bg-gradient-to-b from-navy to-navy-deep px-7 text-[15px] font-semibold text-white shadow-cta ring-1 ring-inset ring-white/10 transition-shadow duration-200 ease-out-cubic hover:shadow-ctaHover hover:ring-white/[0.18]"
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
              <path d="M12 3v12" />
              <path d="m7 11 5 5 5-5" />
              <path d="M5 21h14" />
            </svg>
            Download the app
          </motion.button>

          {/* Real store-shaped badges (also coming-soon). */}
          <div className="flex flex-wrap gap-3">
            <StoreBadge
              store="App Store"
              line1="Download on the"
              line2="App Store"
              glyph={
                <svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
                  <path d="M16.365 1.43c0 1.14-.42 2.21-1.13 3.02-.84.96-2.2 1.7-3.32 1.61-.13-1.1.43-2.27 1.08-3.01.74-.84 2.07-1.49 3.18-1.55.05.31.19.62.19.93ZM20.4 17.06c-.55 1.27-.82 1.83-1.53 2.95-1 1.57-2.4 3.53-4.14 3.54-1.54.02-1.94-1-4.03-.99-2.09.01-2.53 1.01-4.07.99-1.74-.01-3.07-1.78-4.07-3.35C-.34 16.99-.65 11.7 1.7 8.9c1.18-1.42 2.7-2.25 4.13-2.25 1.46 0 2.38.8 3.59.8 1.17 0 1.88-.8 3.57-.8 1.27 0 2.62.69 3.58 1.89-3.15 1.72-2.63 6.21.83 7.52Z" />
                </svg>
              }
            />
            <StoreBadge
              store="Google Play"
              line1="Get it on"
              line2="Google Play"
              glyph={
                <svg width="20" height="20" viewBox="0 0 24 24" aria-hidden>
                  <path d="M3.6 2.3c-.2.2-.3.5-.3.9v17.6c0 .4.1.7.3.9l.1.1L13.5 12v-.2L3.7 2.2l-.1.1Z" fill="#34A853" />
                  <path d="m17 15.3-3.5-3.5v-.2L17 8.1l.1.1 4.1 2.4c1.2.7 1.2 1.8 0 2.5L17 15.3Z" fill="#FBBC04" />
                  <path d="m17.1 15.2-3.6-3.6L3.6 21.6c.4.4 1 .5 1.7.1l11.8-6.5" fill="#EA4335" />
                  <path d="M17.1 8.2 5.3 1.7c-.7-.4-1.3-.3-1.7.1l9.9 9.8 3.6-3.4Z" fill="#4285F4" />
                </svg>
              }
            />
          </div>
        </div>
      ) : (
        <motion.button
          type="button"
          onClick={ping}
          whileHover={reduce ? undefined : { scale: 1.02 }}
          whileTap={reduce ? undefined : { scale: 0.99 }}
          transition={{ type: "spring", stiffness: 420, damping: 30 }}
          className="inline-flex h-14 items-center justify-center gap-2.5 rounded-full bg-gradient-to-b from-navy to-navy-deep px-7 text-[15px] font-semibold text-white shadow-cta ring-1 ring-inset ring-white/10 transition-shadow duration-200 ease-out-cubic hover:shadow-ctaHover hover:ring-white/[0.18]"
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
            <path d="M12 3v12" />
            <path d="m7 11 5 5 5-5" />
            <path d="M5 21h14" />
          </svg>
          Download the app
        </motion.button>
      )}

      {/* Inline, persistent note (always present for screen readers; visually
          fades in once tapped). */}
      <p
        aria-live="polite"
        className={`mt-3 text-[13px] font-medium transition-opacity duration-300 ${
          announced ? "text-accent-deep opacity-100" : "text-ink-3 opacity-0"
        }`}
      >
        {announced
          ? "Parents portal in Enroll+ app, coming soon. We'll let your school know the moment it's live."
          : ""}
      </p>

      {/* Calm floating toast, the in-place acknowledgement. Never navigates. */}
      <AnimatePresence>
        {announced && (
          <motion.div
            initial={{ opacity: 0, y: reduce ? 0 : 10, scale: reduce ? 1 : 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: reduce ? 0 : 8, scale: reduce ? 1 : 0.98 }}
            transition={{ duration: 0.32, ease: EASE_OUT_CUBIC }}
            className="pointer-events-none fixed bottom-6 left-1/2 z-[60] flex -translate-x-1/2 items-center gap-3 rounded-2xl border border-white/70 bg-white/90 px-5 py-3.5 shadow-[0_2px_6px_rgba(38,35,77,0.06),0_24px_60px_-22px_rgba(38,35,77,0.34)] ring-1 ring-navy/[0.06] backdrop-blur-xl"
            role="status"
          >
            <span className="flex h-9 w-9 items-center justify-center rounded-full bg-accent/12 text-accent-deep">
              <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
                <circle cx="12" cy="12" r="9" />
                <path d="M12 7v5l3 2" />
              </svg>
            </span>
            <span className="leading-tight">
              <span className="block text-[14px] font-extrabold tracking-tight text-navy-deep">
                Coming soon
              </span>
              <span className="block text-[12.5px] text-ink-2">
                Parents portal in Enroll+ app, coming soon.
              </span>
            </span>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
