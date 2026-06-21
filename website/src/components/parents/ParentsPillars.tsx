"use client";

import { motion, useReducedMotion } from "framer-motion";
import { PARENTS_PAGE } from "@/lib/content";
import { SectionHeading } from "../ui/SectionHeading";
import { EASE_OUT_CUBIC } from "@/lib/motion";

/**
 * ParentsPillars, the four-pillar block (attendance / results / fees /
 * messages). Each card is a premium glass tile with a numbered marker, a calm
 * surface chip showing the in-app state, and the on-brand hover lift. The grid
 * sits on the lavender canvas with a hairline rule above, NO repetitive
 * trust-band, NO duplicated download CTA.
 */
const ICONS: Record<string, React.ReactNode> = {
  attendance: (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
      <path d="M9 11l3 3L22 4" />
      <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11" />
    </svg>
  ),
  results: (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
      <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
      <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2Z" />
    </svg>
  ),
  fees: (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
      <rect x="2" y="5" width="20" height="14" rx="2" />
      <path d="M2 10h20" />
    </svg>
  ),
  messages: (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
      <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5Z" />
    </svg>
  ),
};

export function ParentsPillars() {
  const reduce = useReducedMotion();

  return (
    <section className="relative scroll-mt-24 py-24 md:py-32">
      <div className="shell">
        <SectionHeading
          eyebrow="What parents get"
          title={
            <>
              The four things you actually need,{" "}
              <span className="text-accent">done well.</span>
            </>
          }
          lede="No notifications spam, no menu sprawl. Just the four screens a parent reaches for, each tuned for the way schools actually run."
        />

        <div className="mt-14 grid gap-5 sm:grid-cols-2">
          {PARENTS_PAGE.pillars.map((p, i) => (
            <motion.article
              key={p.key}
              initial={{ opacity: 0, y: reduce ? 0 : 24 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true, amount: 0.3 }}
              transition={{
                duration: 0.55,
                ease: EASE_OUT_CUBIC,
                delay: 0.06 * i,
              }}
              className="group relative overflow-hidden rounded-[1.75rem] border border-white/70 bg-white/65 p-7 shadow-[0_2px_6px_rgba(38,35,77,0.05),0_24px_56px_-24px_rgba(38,35,77,0.20)] ring-1 ring-navy/[0.05] backdrop-blur-xl transition-shadow duration-300 ease-out-cubic hover:shadow-cardHover"
            >
              {/* Subtle aurora wash inside the card, the only glow per tile. */}
              <div
                aria-hidden
                className="pointer-events-none absolute inset-0 -z-10 opacity-0 transition-opacity duration-500 group-hover:opacity-100"
                style={{
                  background:
                    "radial-gradient(70% 80% at 10% 0%, rgba(108,92,224,0.10) 0%, rgba(108,92,224,0) 60%), radial-gradient(60% 80% at 100% 100%, rgba(60,185,169,0.08) 0%, rgba(60,185,169,0) 60%)",
                }}
              />

              <div className="flex items-start justify-between gap-4">
                <span className="flex h-11 w-11 items-center justify-center rounded-2xl bg-gradient-to-br from-accent/14 to-accent/4 text-accent-deep ring-1 ring-accent/15">
                  {ICONS[p.key]}
                </span>
                <span className="nums text-[11px] font-bold tracking-[0.18em] text-ink-3">
                  0{i + 1}
                </span>
              </div>

              <h3 className="mt-5 text-[20px] font-extrabold leading-tight tracking-tight text-navy-deep">
                {p.title}
              </h3>
              <p className="mt-2.5 max-w-md text-[14.5px] leading-relaxed text-ink-2">
                {p.body}
              </p>

              {/* In-app state chip, hints at the real surface this maps to. */}
              <div className="mt-6 flex items-center gap-2.5 rounded-2xl border border-navy/8 bg-lavender-tint/60 px-3 py-2.5 ring-1 ring-white/50">
                <span className="h-1.5 w-1.5 shrink-0 rounded-full bg-accent" />
                <span className="text-[11.5px] font-bold tracking-tight text-navy-deep">
                  {p.surface}
                </span>
                <span aria-hidden className="text-ink-3">·</span>
                <span className="truncate text-[11px] text-ink-3">{p.detail}</span>
              </div>
            </motion.article>
          ))}
        </div>
      </div>
    </section>
  );
}
