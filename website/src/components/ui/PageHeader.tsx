"use client";

import Link from "next/link";
import { motion, useReducedMotion } from "framer-motion";
import { EASE_OUT_CUBIC } from "@/lib/motion";

export interface Crumb {
  label: string;
  href?: string;
}

/**
 * PageHeader — the distinct, per-page header band that sits directly under the
 * global nav on every sub-page (Privacy, Terms, Cookies, Support, About,
 * Features, Pricing). It gives each page its own identity while staying 100% in
 * the Enroll+ system:
 *   • an eyebrow + breadcrumb that says exactly where you are,
 *   • a tight display title + optional lede,
 *   • the single sanctioned aurora bloom behind it (low-opacity, lavender),
 *   • a hairline baseline that separates the header from the page body,
 *   • a calm, staggered load animation (once, fast, settles).
 *
 * `tone="navy"` renders the dark slab variant (used where a page wants a bolder
 * opening, e.g. Support); default is the light lavender variant.
 */
export function PageHeader({
  eyebrow,
  title,
  lede,
  crumbs,
  align = "left",
  tone = "light",
  children,
}: {
  eyebrow?: string;
  title: React.ReactNode;
  lede?: React.ReactNode;
  crumbs?: Crumb[];
  align?: "left" | "center";
  tone?: "light" | "navy";
  children?: React.ReactNode;
}) {
  const reduce = useReducedMotion();
  const navy = tone === "navy";
  const centered = align === "center";

  const item = {
    hidden: { opacity: 0, y: reduce ? 0 : 16 },
    show: { opacity: 1, y: 0 },
  };
  const container = {
    hidden: {},
    show: { transition: { staggerChildren: 0.07, delayChildren: 0.04 } },
  };

  return (
    <section
      className={`relative overflow-hidden ${
        navy ? "text-white" : ""
      }`}
    >
      {navy && (
        <div className="absolute inset-x-4 inset-y-0 -z-10 rounded-[2rem] bg-navy-deep sm:inset-x-6" />
      )}

      {/* Aurora — the single sanctioned glow, anchored behind the title. */}
      <div
        aria-hidden
        className="pointer-events-none absolute -top-24 left-1/2 -z-10 h-[420px] w-[760px] -translate-x-1/2 opacity-70"
        style={{
          background: navy
            ? "radial-gradient(45% 55% at 28% 30%, rgba(108,92,224,0.40) 0%, rgba(108,92,224,0) 70%), radial-gradient(40% 45% at 75% 25%, rgba(60,185,169,0.18) 0%, rgba(60,185,169,0) 70%)"
            : "radial-gradient(45% 55% at 30% 30%, rgba(108,92,224,0.16) 0%, rgba(108,92,224,0) 70%), radial-gradient(40% 45% at 72% 28%, rgba(60,185,169,0.10) 0%, rgba(60,185,169,0) 70%)",
        }}
      />

      <div
        className={`shell ${navy ? "py-16 sm:py-20 md:py-24" : "pb-12 pt-28 md:pb-16 md:pt-32"}`}
      >
        <motion.div
          variants={container}
          initial="hidden"
          animate="show"
          className={`max-w-3xl ${centered ? "mx-auto text-center" : ""}`}
        >
          {/* Breadcrumb */}
          {crumbs && crumbs.length > 0 && (
            <motion.nav
              variants={item}
              transition={{ duration: 0.4, ease: EASE_OUT_CUBIC }}
              aria-label="Breadcrumb"
              className={`mb-5 flex items-center gap-1.5 text-[12px] font-medium ${
                centered ? "justify-center" : ""
              } ${navy ? "text-white/55" : "text-ink-3"}`}
            >
              {crumbs.map((c, i) => (
                <span key={`${c.label}-${i}`} className="inline-flex items-center gap-1.5">
                  {i > 0 && <span aria-hidden className="opacity-50">/</span>}
                  {c.href ? (
                    <Link
                      href={c.href}
                      className={`transition-colors ${
                        navy ? "hover:text-white" : "hover:text-navy-deep"
                      }`}
                    >
                      {c.label}
                    </Link>
                  ) : (
                    <span className={navy ? "text-white/80" : "text-navy-deep"}>{c.label}</span>
                  )}
                </span>
              ))}
            </motion.nav>
          )}

          {eyebrow && (
            <motion.p
              variants={item}
              transition={{ duration: 0.4, ease: EASE_OUT_CUBIC }}
              className={`mb-3 text-[11px] font-bold uppercase tracking-[0.14em] ${
                navy ? "text-accent-soft" : "text-accent"
              }`}
            >
              {eyebrow}
            </motion.p>
          )}

          <motion.h1
            variants={item}
            transition={{ duration: 0.5, ease: EASE_OUT_CUBIC }}
            className={`font-extrabold tracking-tighter ${
              navy ? "text-white" : "text-navy-deep"
            } text-[2.1rem] leading-[1.06] sm:text-4xl md:text-[2.9rem]`}
          >
            {title}
          </motion.h1>

          {lede && (
            <motion.p
              variants={item}
              transition={{ duration: 0.5, ease: EASE_OUT_CUBIC }}
              className={`mt-5 text-lg leading-relaxed ${
                navy ? "text-white/70" : "text-ink-2"
              } ${centered ? "mx-auto max-w-prose" : "max-w-prose"}`}
            >
              {lede}
            </motion.p>
          )}

          {children && (
            <motion.div
              variants={item}
              transition={{ duration: 0.5, ease: EASE_OUT_CUBIC }}
              className={`mt-8 ${centered ? "flex justify-center" : ""}`}
            >
              {children}
            </motion.div>
          )}
        </motion.div>
      </div>

      {/* Hairline baseline — separates header from page body (light tone only). */}
      {!navy && (
        <div
          aria-hidden
          className="shell"
        >
          <div className="h-px w-full bg-gradient-to-r from-transparent via-navy/10 to-transparent" />
        </div>
      )}
    </section>
  );
}
