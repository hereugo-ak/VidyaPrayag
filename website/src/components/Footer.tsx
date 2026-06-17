"use client";

import Image from "next/image";
import Link from "next/link";
import { motion, useReducedMotion } from "framer-motion";
import { EnrollMark } from "./ui/Logo";
import { Button } from "./ui/Button";
import {
  FOOTER_NAV,
  FOOTER_CONTACT,
  FOOTER_SOCIAL,
} from "@/lib/content";
import { PHOTOS } from "@/lib/images";
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
 * Footer, rebuilt LIGHT, so it dissolves into the lavender canvas instead of
 * sitting on it as a dark slab. The structure follows the Quantum reference
 * (generous whitespace, clean link columns, a giant low-contrast wordmark
 * watermark at the very bottom) but everything stays in the Enroll+ light
 * system: navy ink on lavender, a single hairline separating it from the page.
 *
 * Because the homepage no longer has a full-bleed final CTA, the footer also
 * carries the closing onboarding prompt as a quiet, bordered strip.
 */
export function Footer() {
  const reduce = useReducedMotion();
  const year = new Date().getFullYear();

  return (
    <footer className="relative mt-12 overflow-hidden border-t border-navy/10">
      <div className="shell pt-20 md:pt-24">
        {/* Closing onboarding prompt, a premium LIGHT panel (no dark slab).
            A real, muted campus photograph sits behind the content under a heavy
            light overlay, so it reads as commissioned atmosphere, not stock. The
            single sanctioned violet/teal aurora, a hairline ring and soft
            elevation finish a designed closing moment, all on-brand, all light. */}
        <motion.div
          initial={{ opacity: 0, y: reduce ? 0 : 18 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, amount: 0.3 }}
          transition={{ duration: 0.55, ease: EASE_OUT_CUBIC }}
          className="relative overflow-hidden rounded-[2rem] border border-white/70 bg-white/55 px-7 py-12 shadow-[0_2px_6px_rgba(38,35,77,0.05),0_28px_60px_-24px_rgba(38,35,77,0.20)] ring-1 ring-navy/[0.06] backdrop-blur-xl sm:px-12 sm:py-16"
        >
          {/* Background photograph, real, light, institutional. Sits furthest
              back, low intensity, never competing with the copy. */}
          <Image
            aria-hidden
            src={PHOTOS.footerOnboarding.src}
            alt=""
            fill
            sizes="(max-width: 1024px) 100vw, 1100px"
            placeholder="blur"
            blurDataURL={PHOTOS.footerOnboarding.blur}
            className="pointer-events-none absolute inset-0 -z-30 object-cover"
          />
          {/* Light legibility overlay, washes the photo into the lavender
              palette and guarantees text contrast (lighter on the left where the
              copy sits, a touch more transparent on the right). */}
          <div
            aria-hidden
            className="pointer-events-none absolute inset-0 -z-20"
            style={{
              background:
                "linear-gradient(105deg, rgba(252,248,255,0.94) 0%, rgba(252,248,255,0.90) 42%, rgba(230,230,250,0.78) 100%)",
            }}
          />
          {/* Soft aurora wash inside the panel, the only glow, low opacity. */}
          <div
            aria-hidden
            className="pointer-events-none absolute inset-0 -z-10"
            style={{
              background:
                "radial-gradient(60% 120% at 12% 0%, rgba(108,92,224,0.12) 0%, rgba(108,92,224,0) 60%), radial-gradient(50% 120% at 92% 100%, rgba(60,185,169,0.10) 0%, rgba(60,185,169,0) 60%)",
            }}
          />

          <div className="flex flex-col gap-7 sm:flex-row sm:items-end sm:justify-between sm:gap-10">
            <div className="max-w-xl">
              <p className="eyebrow">Get started</p>
              <p className="mt-3 display text-[2rem] leading-[1.05] tracking-tighter text-navy-deep sm:text-[2.6rem]">
                Bring your whole school
                <br className="hidden sm:block" /> online<span className="text-accent">.</span>
              </p>
              <p className="mt-4 max-w-md text-[15px] leading-relaxed text-ink-2">
                Set up in minutes, no credit card, no sales call. One source of
                truth from day one.
              </p>
            </div>
            <div className="shrink-0">
              <Button href="/onboarding" size="lg">
                Onboard your school →
              </Button>
            </div>
          </div>
        </motion.div>

        {/* Link + brand grid */}
        <div className="mt-16 grid gap-12 lg:grid-cols-[1.5fr_repeat(3,1fr)] md:mt-20">
          {/* Brand + contact column */}
          <div className="max-w-sm">
            <Link
              href="/"
              aria-label="Enroll+ home"
              className="group inline-flex items-center gap-2.5"
            >
              <EnrollMark className="h-9 w-9 shrink-0 transition-transform duration-200 ease-out-cubic group-hover:scale-[1.04]" />
              <span className="text-[19px] font-extrabold tracking-tighter text-navy-deep">
                Enroll<span className="text-accent">+</span>
              </span>
            </Link>

            <p className="mt-5 text-sm leading-relaxed text-ink-2">
              {FOOTER_CONTACT.blurb}
            </p>

            <div className="mt-6 space-y-2.5 text-sm">
              <a
                href={`mailto:${FOOTER_CONTACT.email}`}
                className="flex items-center gap-2.5 text-ink-2 transition-colors hover:text-navy-deep"
              >
                <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-accent/10 text-accent-deep">
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="2" y="4" width="20" height="16" rx="2" /><path d="m22 7-10 6L2 7" /></svg>
                </span>
                {FOOTER_CONTACT.email}
              </a>
              <a
                href={`mailto:${FOOTER_CONTACT.support}`}
                className="flex items-center gap-2.5 text-ink-2 transition-colors hover:text-navy-deep"
              >
                <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-accent/10 text-accent-deep">
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5Z" /></svg>
                </span>
                {FOOTER_CONTACT.support}
              </a>
            </div>

            {/* Social slots, honest placeholders (disabled until real accounts). */}
            <div className="mt-7 flex gap-2.5" aria-label="Social links">
              {FOOTER_SOCIAL.map((s) => {
                const cls =
                  "flex h-9 w-9 items-center justify-center rounded-full border border-navy/10 bg-white/60 text-ink-2 transition-colors";
                return s.href ? (
                  <a
                    key={s.label}
                    href={s.href}
                    aria-label={s.label}
                    className={`${cls} hover:border-accent hover:bg-accent hover:text-white`}
                  >
                    {SOCIAL_ICON[s.label]}
                  </a>
                ) : (
                  <span
                    key={s.label}
                    aria-label={`${s.label} (coming soon)`}
                    title={`${s.label} (coming soon)`}
                    className={`${cls} cursor-default opacity-45`}
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
                <h3 className="text-[11px] font-bold uppercase tracking-[0.16em] text-ink-3">
                  {group}
                </h3>
                <ul className="mt-5 space-y-3.5">
                  {links.map((l) => {
                    const external = l.href.startsWith("mailto:");
                    const klass =
                      "group inline-flex items-center text-sm text-ink-2 transition-colors hover:text-navy-deep";
                    const underline =
                      "bg-gradient-to-r from-accent to-accent bg-[length:0%_1px] bg-left-bottom bg-no-repeat pb-0.5 transition-[background-size] duration-300 ease-out-cubic group-hover:bg-[length:100%_1px]";
                    return (
                      <li key={l.href}>
                        {external ? (
                          <a href={l.href} className={klass}>
                            <span className={underline}>{l.label}</span>
                          </a>
                        ) : (
                          <Link href={l.href} className={klass}>
                            <span className={underline}>{l.label}</span>
                          </Link>
                        )}
                      </li>
                    );
                  })}
                </ul>
              </div>
            ))}
        </div>

        {/* Legal + copyright row, wide-spaced, Quantum-style metadata line. */}
        <div className="mt-16 flex flex-col gap-4 border-t border-navy/8 pt-7 text-[13px] text-ink-3 sm:flex-row sm:items-center sm:justify-between md:mt-20">
          <p>© {year} Enroll+. All rights reserved.</p>
          <div className="flex flex-wrap gap-x-7 gap-y-2">
            {FOOTER_NAV.Legal.map((l) => (
              <Link
                key={l.href}
                href={l.href}
                className="transition-colors hover:text-navy-deep"
              >
                {l.label}
              </Link>
            ))}
          </div>
        </div>
      </div>

      {/* Quantum-style giant wordmark, ultra-low-contrast navy on the lavender
          canvas, bleeding off the bottom edge, masked at the baseline. Reads as
          atmosphere, not a heading. Decorative only (aria-hidden). */}
      <div aria-hidden className="relative mt-12 select-none overflow-hidden">
        <div className="pointer-events-none flex items-end justify-center">
          <span
            className="block w-full text-center font-extrabold leading-[0.74] tracking-tighter text-navy-deep/[0.05]"
            style={{
              fontSize: "clamp(4rem, 19vw, 17rem)",
              maskImage:
                "linear-gradient(180deg, #000 28%, rgba(0,0,0,0.22) 76%, transparent 100%)",
              WebkitMaskImage:
                "linear-gradient(180deg, #000 28%, rgba(0,0,0,0.22) 76%, transparent 100%)",
            }}
          >
            Enroll<span className="text-accent/[0.18]">+</span>
          </span>
        </div>
        <p className="relative -mt-3 pb-8 text-center text-[10px] font-bold uppercase tracking-[0.34em] text-ink-3/55 sm:-mt-6 md:-mt-10">
          The operating system for your school
        </p>
      </div>
    </footer>
  );
}
