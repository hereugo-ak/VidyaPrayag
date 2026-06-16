"use client";

import Link from "next/link";
import { motion, useReducedMotion } from "framer-motion";
import { EnrollMark } from "./ui/Logo";
import { Button } from "./ui/Button";
import {
  FOOTER_NAV,
  FOOTER_CONTACT,
  FOOTER_SOCIAL,
  FOOTER_TAGLINE,
} from "@/lib/content";
import { EASE_OUT_CUBIC } from "@/lib/motion";

/* Minimal, hand-drawn social glyphs (no icon dependency, on-brand stroke). */
const SOCIAL_ICON: Record<string, React.ReactNode> = {
  X: (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
      <path d="M18.244 2H21.5l-7.5 8.57L23 22h-6.844l-5.36-7.006L4.66 22H1.4l8.02-9.165L1 2h7.02l4.844 6.404L18.244 2Zm-1.2 18h1.9L7.04 4H5.01l12.034 16Z" />
    </svg>
  ),
  LinkedIn: (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
      <path d="M20.45 20.45h-3.56v-5.57c0-1.33-.02-3.04-1.85-3.04-1.85 0-2.14 1.45-2.14 2.94v5.67H9.34V9h3.42v1.56h.05c.48-.9 1.64-1.85 3.37-1.85 3.6 0 4.27 2.37 4.27 5.46v6.28ZM5.34 7.43a2.06 2.06 0 1 1 0-4.13 2.06 2.06 0 0 1 0 4.13ZM7.12 20.45H3.55V9h3.57v11.45ZM22.22 0H1.77C.79 0 0 .77 0 1.73v20.54C0 23.22.79 24 1.77 24h20.45c.98 0 1.78-.78 1.78-1.73V1.73C24 .77 23.2 0 22.22 0Z" />
    </svg>
  ),
  Instagram: (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden>
      <rect x="2" y="2" width="20" height="20" rx="5.5" />
      <circle cx="12" cy="12" r="4" />
      <circle cx="17.5" cy="6.5" r="1.1" fill="currentColor" stroke="none" />
    </svg>
  ),
  GitHub: (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
      <path d="M12 .5C5.37.5 0 5.87 0 12.5c0 5.3 3.44 9.8 8.21 11.39.6.11.82-.26.82-.58v-2.03c-3.34.73-4.04-1.61-4.04-1.61-.55-1.39-1.34-1.76-1.34-1.76-1.09-.75.08-.73.08-.73 1.2.08 1.84 1.24 1.84 1.24 1.07 1.84 2.81 1.31 3.49 1 .11-.78.42-1.31.76-1.61-2.67-.3-5.47-1.34-5.47-5.93 0-1.31.47-2.38 1.24-3.22-.12-.31-.54-1.52.12-3.18 0 0 1.01-.32 3.3 1.23a11.5 11.5 0 0 1 6 0c2.29-1.55 3.3-1.23 3.3-1.23.66 1.66.24 2.87.12 3.18.77.84 1.23 1.91 1.23 3.22 0 4.6-2.8 5.62-5.48 5.92.43.37.81 1.1.81 2.22v3.29c0 .32.22.7.83.58A12 12 0 0 0 24 12.5C24 5.87 18.63.5 12 .5Z" />
    </svg>
  ),
};

/**
 * Footer — rebuilt to a premium "panel" footer in the brand system:
 *   • a deep-navy rounded slab (the travel-reference card pattern, recoloured to
 *     Enroll+ navy/lavender), floating on the lavender canvas;
 *   • a brand + contact column, three nav columns, and an onboarding CTA;
 *   • a giant translucent "Enroll+" wordmark bleeding off the bottom edge
 *     (the Quantum pattern), with the bridge mark + tagline — confident, not loud;
 *   • everything reveals on scroll, once, with the house ease.
 */
export function Footer() {
  const reduce = useReducedMotion();
  const year = new Date().getFullYear();

  return (
    <footer className="px-4 pb-6 pt-10 sm:px-6 md:pb-8">
      <motion.div
        initial={{ opacity: 0, y: reduce ? 0 : 28 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true, amount: 0.15 }}
        transition={{ duration: 0.6, ease: EASE_OUT_CUBIC }}
        className="relative mx-auto w-full max-w-shell overflow-hidden rounded-[2rem] bg-navy-deep text-white shadow-cta"
      >
        {/* Soft corner aurora — the same restrained bloom used on the CTA slab. */}
        <div
          aria-hidden
          className="pointer-events-none absolute -left-24 -top-24 h-[360px] w-[360px] rounded-full opacity-50"
          style={{
            background:
              "radial-gradient(circle, rgba(108,92,224,0.42) 0%, rgba(108,92,224,0) 68%)",
          }}
        />
        {/* Hairline top edge catches the light. */}
        <div
          aria-hidden
          className="pointer-events-none absolute inset-x-0 top-0 h-px"
          style={{
            background:
              "linear-gradient(90deg, transparent, rgba(255,255,255,0.22), transparent)",
          }}
        />

        <div className="relative px-6 pt-14 sm:px-10 md:px-14 md:pt-16">
          <div className="grid gap-12 lg:grid-cols-[1.5fr_repeat(3,1fr)]">
            {/* Brand + contact column */}
            <div className="max-w-sm">
              <Link
                href="/"
                aria-label="Enroll+ home"
                className="group inline-flex items-center gap-2.5"
              >
                <EnrollMark
                  className="h-9 w-9 shrink-0 transition-transform duration-200 ease-out-cubic group-hover:scale-[1.04]"
                  tone="light"
                />
                <span className="text-[19px] font-extrabold tracking-tighter text-white">
                  Enroll<span className="text-accent-soft">+</span>
                </span>
              </Link>

              <p className="mt-5 text-sm leading-relaxed text-white/65">
                {FOOTER_CONTACT.blurb}
              </p>

              <div className="mt-6 space-y-2.5 text-sm">
                <a
                  href={`mailto:${FOOTER_CONTACT.email}`}
                  className="flex items-center gap-2.5 text-white/75 transition-colors hover:text-white"
                >
                  <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-white/8 text-accent-soft">
                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="2" y="4" width="20" height="16" rx="2" /><path d="m22 7-10 6L2 7" /></svg>
                  </span>
                  {FOOTER_CONTACT.email}
                </a>
                <a
                  href={`mailto:${FOOTER_CONTACT.support}`}
                  className="flex items-center gap-2.5 text-white/75 transition-colors hover:text-white"
                >
                  <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-white/8 text-accent-soft">
                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5Z" /></svg>
                  </span>
                  {FOOTER_CONTACT.support}
                </a>
              </div>

              {/* Social slots — honest placeholders (disabled until real accounts). */}
              <div className="mt-7 flex gap-2.5" aria-label="Social links">
                {FOOTER_SOCIAL.map((s) => {
                  const cls =
                    "flex h-9 w-9 items-center justify-center rounded-full bg-white/8 text-white/70 transition-colors";
                  return s.href ? (
                    <a
                      key={s.label}
                      href={s.href}
                      aria-label={s.label}
                      className={`${cls} hover:bg-accent hover:text-white`}
                    >
                      {SOCIAL_ICON[s.label]}
                    </a>
                  ) : (
                    <span
                      key={s.label}
                      aria-label={`${s.label} — coming soon`}
                      title={`${s.label} — coming soon`}
                      className={`${cls} cursor-default opacity-50`}
                    >
                      {SOCIAL_ICON[s.label]}
                    </span>
                  );
                })}
              </div>
            </div>

            {/* Nav columns */}
            {Object.entries(FOOTER_NAV)
              .filter(([group]) => group !== "Legal")
              .map(([group, links]) => (
                <div key={group}>
                  <h3 className="text-[11px] font-bold uppercase tracking-[0.14em] text-white/45">
                    {group}
                  </h3>
                  <ul className="mt-5 space-y-3.5">
                    {links.map((l) => {
                      const external = l.href.startsWith("mailto:");
                      const klass =
                        "group inline-flex items-center text-sm text-white/70 transition-colors hover:text-white";
                      return (
                        <li key={l.href}>
                          {external ? (
                            <a href={l.href} className={klass}>
                              <span className="bg-gradient-to-r from-accent-soft to-accent-soft bg-[length:0%_1px] bg-left-bottom bg-no-repeat pb-0.5 transition-[background-size] duration-300 ease-out-cubic group-hover:bg-[length:100%_1px]">
                                {l.label}
                              </span>
                            </a>
                          ) : (
                            <Link href={l.href} className={klass}>
                              <span className="bg-gradient-to-r from-accent-soft to-accent-soft bg-[length:0%_1px] bg-left-bottom bg-no-repeat pb-0.5 transition-[background-size] duration-300 ease-out-cubic group-hover:bg-[length:100%_1px]">
                                {l.label}
                              </span>
                            </Link>
                          )}
                        </li>
                      );
                    })}
                  </ul>
                </div>
              ))}
          </div>

          {/* CTA strip */}
          <div className="mt-14 flex flex-col gap-5 rounded-2xl border border-white/10 bg-white/[0.04] px-6 py-6 sm:flex-row sm:items-center sm:justify-between sm:px-8">
            <div>
              <p className="text-lg font-extrabold tracking-tight text-white">
                Bring your whole school online.
              </p>
              <p className="mt-1 text-sm text-white/60">
                {FOOTER_TAGLINE} · Set up in minutes, no credit card.
              </p>
            </div>
            <Button href="/onboarding" size="md" variant="secondary">
              Onboard your school
            </Button>
          </div>

          {/* Legal + copyright row */}
          <div className="mt-10 flex flex-col gap-4 border-t border-white/10 pt-7 text-sm text-white/55 sm:flex-row sm:items-center sm:justify-between">
            <p>© {year} Enroll+. All rights reserved.</p>
            <div className="flex flex-wrap gap-x-6 gap-y-2">
              {FOOTER_NAV.Legal.map((l) => (
                <Link
                  key={l.href}
                  href={l.href}
                  className="transition-colors hover:text-white"
                >
                  {l.label}
                </Link>
              ))}
            </div>
          </div>
        </div>

        {/* Quantum-style giant wordmark, bleeding off the bottom edge. Pure
            type, ultra-low-contrast, masked at the baseline so it reads as a
            watermark, not a heading. Decorative only (aria-hidden). */}
        <div
          aria-hidden
          className="relative mt-10 select-none overflow-hidden px-4"
        >
          <div className="pointer-events-none flex items-end justify-center">
            <span
              className="block w-full text-center font-extrabold leading-[0.78] tracking-tighter text-white/[0.05]"
              style={{
                fontSize: "clamp(4rem, 19vw, 17rem)",
                maskImage:
                  "linear-gradient(180deg, #000 30%, rgba(0,0,0,0.25) 78%, transparent 100%)",
                WebkitMaskImage:
                  "linear-gradient(180deg, #000 30%, rgba(0,0,0,0.25) 78%, transparent 100%)",
              }}
            >
              Enroll<span className="text-accent/[0.14]">+</span>
            </span>
          </div>
          <p className="relative -mt-3 pb-7 text-center text-[10px] font-bold uppercase tracking-[0.3em] text-white/25 sm:-mt-6 md:-mt-10">
            The operating system for your school
          </p>
        </div>
      </motion.div>
    </footer>
  );
}
