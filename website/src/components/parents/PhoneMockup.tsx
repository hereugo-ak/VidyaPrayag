"use client";

import { motion, useReducedMotion } from "framer-motion";
import { EASE_OUT_CUBIC } from "@/lib/motion";

/**
 * PhoneMockup, an ultra-premium, hand-built CSS device frame that previews the
 * Parents portal in the Enroll+ app (coming soon).
 *
 * Every pixel is rendered in code, no screenshot, no stock device photo, so the
 * preview holds up at any resolution and reads exactly on-brand. The frame
 * itself is a layered device body (titanium ring + glossy bezel + dark display
 * + screen-glow ring + inner highlight) and the home screen inside is a quietly
 * loaded micro-product: a greeting hero with an aurora wash, four feature rails
 * each with their own iconography and chip state, a live-progress attendance
 * meter, a fees ledger preview, a published-result card with a sparkline, and a
 * messages preview. Two floating glass plates flank the device for the
 * "in your pocket" promise, all on the same lavender canvas, all light, all
 * reduced-motion aware.
 */
export function PhoneMockup({ className = "" }: { className?: string }) {
  const reduce = useReducedMotion();

  return (
    <motion.div
      initial={{ opacity: 0, y: reduce ? 0 : 28, rotate: reduce ? 0 : -1.5 }}
      whileInView={{ opacity: 1, y: 0, rotate: 0 }}
      viewport={{ once: true, amount: 0.3 }}
      transition={{ duration: 0.8, ease: EASE_OUT_CUBIC }}
      className={`relative mx-auto w-[300px] ${className}`}
    >
      {/* Soft floor glow under the device, sits on the canvas, NOT behind the
          phone, so it reads as the phone resting on a lit surface. */}
      <div
        aria-hidden
        className="pointer-events-none absolute -inset-x-12 -bottom-10 -z-10 h-32 rounded-[100%]"
        style={{
          background:
            "radial-gradient(60% 80% at 50% 50%, rgba(108,92,224,0.18) 0%, rgba(108,92,224,0) 70%)",
          filter: "blur(8px)",
        }}
      />

      {/* DEVICE: titanium edge → bezel → display ───────────────────────────── */}
      <div
        className="relative rounded-[2.9rem] p-[3px] shadow-[0_2px_8px_rgba(38,35,77,0.10),0_50px_100px_-30px_rgba(38,35,77,0.55)]"
        style={{
          background:
            "linear-gradient(160deg, #C8C2DC 0%, #6E6685 28%, #1A1838 50%, #6E6685 72%, #C8C2DC 100%)",
        }}
      >
        {/* Bezel */}
        <div className="relative rounded-[2.75rem] bg-[#0B0A1A] p-[6px] ring-1 ring-white/5">
          {/* Side buttons (titanium tone) */}
          <span aria-hidden className="absolute -left-[3px] top-[88px] h-12 w-[3px] rounded-l bg-gradient-to-b from-[#8E879F] to-[#3A3550]" />
          <span aria-hidden className="absolute -left-[3px] top-[148px] h-10 w-[3px] rounded-l bg-gradient-to-b from-[#8E879F] to-[#3A3550]" />
          <span aria-hidden className="absolute -right-[3px] top-[120px] h-16 w-[3px] rounded-r bg-gradient-to-b from-[#8E879F] to-[#3A3550]" />

          {/* Display: layered lavender canvas, with an aurora wash + grain */}
          <div className="relative overflow-hidden rounded-[2.35rem]" style={{ background: "linear-gradient(180deg, #FCF8FF 0%, #F1EFFB 100%)" }}>
            {/* Inner screen highlight (top sheen) */}
            <div
              aria-hidden
              className="pointer-events-none absolute inset-x-0 top-0 h-24"
              style={{
                background:
                  "linear-gradient(180deg, rgba(255,255,255,0.85) 0%, rgba(255,255,255,0) 100%)",
                mixBlendMode: "screen",
              }}
            />
            {/* Aurora wash at the top of the app */}
            <div
              aria-hidden
              className="pointer-events-none absolute inset-x-0 top-0 h-56"
              style={{
                background:
                  "radial-gradient(70% 90% at 20% 0%, rgba(108,92,224,0.18) 0%, rgba(108,92,224,0) 70%), radial-gradient(60% 80% at 90% 10%, rgba(60,185,169,0.14) 0%, rgba(60,185,169,0) 70%)",
              }}
            />

            {/* Status bar + Dynamic Island */}
            <div className="relative flex h-9 items-center justify-between px-6 pt-1.5 text-[10px] font-bold text-navy-deep">
              <span className="nums">9:41</span>
              <div
                aria-hidden
                className="absolute left-1/2 top-1.5 h-[22px] w-[92px] -translate-x-1/2 rounded-full bg-[#0B0A1A]"
              >
                {/* Camera dot in the island */}
                <span className="absolute right-3 top-1/2 block h-2 w-2 -translate-y-1/2 rounded-full bg-[#161429] ring-1 ring-[#2D2A4E]" />
              </div>
              <span className="flex items-center gap-1.5">
                <svg width="14" height="10" viewBox="0 0 18 12" fill="currentColor" aria-hidden>
                  <rect x="0" y="7" width="3" height="5" rx="1" />
                  <rect x="5" y="4" width="3" height="8" rx="1" />
                  <rect x="10" y="1" width="3" height="11" rx="1" />
                </svg>
                <svg width="11" height="10" viewBox="0 0 16 12" fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden>
                  <path d="M8 10v.01M2 6.5a8 8 0 0 1 12 0M4.5 8.5a4.5 4.5 0 0 1 7 0" strokeLinecap="round" />
                </svg>
                <svg width="18" height="10" viewBox="0 0 26 14" fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden>
                  <rect x="1" y="3" width="20" height="8" rx="2.4" />
                  <rect x="3" y="5" width="14" height="4" rx="1" fill="currentColor" />
                  <path d="M23 5.5v3" strokeLinecap="round" />
                </svg>
              </span>
            </div>

            {/* Greeting hero */}
            <div className="relative px-5 pb-3 pt-2">
              <div className="flex items-center justify-between">
                <p className="text-[9.5px] font-bold uppercase tracking-[0.2em] text-accent-deep">
                  Enroll<span className="text-accent">+</span> · Parents portal
                </p>
                <span className="flex h-6 w-6 items-center justify-center rounded-full border border-navy/10 bg-white/70 text-navy-deep backdrop-blur">
                  <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
                    <path d="M18 8A6 6 0 1 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
                    <path d="M13.73 21a2 2 0 0 1-3.46 0" />
                  </svg>
                </span>
              </div>
              <div className="mt-2.5 flex items-center gap-3">
                <div className="relative">
                  <span className="relative flex h-11 w-11 items-center justify-center rounded-2xl bg-gradient-to-br from-accent via-accent-soft to-accent-deep text-[16px] font-extrabold text-white shadow-[0_8px_18px_-6px_rgba(108,92,224,0.55)] ring-1 ring-white/30">
                    A
                  </span>
                  <span className="absolute -bottom-0.5 -right-0.5 flex h-3.5 w-3.5 items-center justify-center rounded-full bg-success ring-2 ring-white">
                    <svg width="8" height="8" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="3.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
                      <path d="M20 6 9 17l-5-5" />
                    </svg>
                  </span>
                </div>
                <div className="leading-tight">
                  <p className="text-[15px] font-extrabold tracking-tight text-navy-deep">
                    Aarav&apos;s day
                  </p>
                  <p className="text-[10.5px] text-ink-3">Class 6 · Section B · Roll 14</p>
                </div>
              </div>
            </div>

            {/* HERO ATTENDANCE CARD, the calm, premium feature row */}
            <div className="mx-4 mb-2.5 overflow-hidden rounded-[1.3rem] border border-white/70 bg-white/85 p-3.5 shadow-[0_1px_2px_rgba(38,35,77,0.04),0_14px_30px_-18px_rgba(38,35,77,0.22)] ring-1 ring-navy/[0.04] backdrop-blur">
              <div className="flex items-center justify-between">
                <span className="flex items-center gap-2 text-[10.5px] font-bold uppercase tracking-[0.12em] text-ink-3">
                  <span className="h-1.5 w-1.5 rounded-full bg-success" />
                  Live · today
                </span>
                <span className="rounded-full bg-success/12 px-2 py-0.5 text-[9.5px] font-bold text-success">
                  Present
                </span>
              </div>
              <p className="mt-2 text-[15px] font-extrabold tracking-tight text-navy-deep">
                Marked present, 8:42 AM
              </p>
              <p className="text-[10.5px] text-ink-3">By Ms. Iyer · Class 6-B teacher</p>

              {/* Attendance ring this month */}
              <div className="mt-3 flex items-center gap-3">
                <div className="relative h-12 w-12 shrink-0">
                  <svg viewBox="0 0 36 36" className="h-full w-full -rotate-90" aria-hidden>
                    <circle cx="18" cy="18" r="15.5" stroke="#E7E3F5" strokeWidth="3" fill="none" />
                    <circle
                      cx="18"
                      cy="18"
                      r="15.5"
                      stroke="url(#ringGrad)"
                      strokeWidth="3"
                      fill="none"
                      strokeLinecap="round"
                      strokeDasharray="97.39"
                      strokeDashoffset="9.74"
                    />
                    <defs>
                      <linearGradient id="ringGrad" x1="0" y1="0" x2="36" y2="36">
                        <stop offset="0%" stopColor="#6C5CE0" />
                        <stop offset="100%" stopColor="#3CB9A9" />
                      </linearGradient>
                    </defs>
                  </svg>
                  <span className="absolute inset-0 flex items-center justify-center text-[10px] font-extrabold tracking-tight text-navy-deep">
                    92%
                  </span>
                </div>
                <div className="min-w-0 flex-1 leading-tight">
                  <p className="text-[11px] font-bold tracking-tight text-navy-deep">
                    Attendance this month
                  </p>
                  <p className="text-[10px] text-ink-3">
                    18 of 19 school days · 1 leave on Mar 12
                  </p>
                </div>
              </div>
            </div>

            {/* RESULTS CARD with sparkline */}
            <div className="mx-4 mb-2.5 overflow-hidden rounded-[1.3rem] border border-white/70 bg-white/85 p-3.5 shadow-[0_1px_2px_rgba(38,35,77,0.04),0_14px_30px_-18px_rgba(38,35,77,0.22)] ring-1 ring-navy/[0.04] backdrop-blur">
              <div className="flex items-start justify-between">
                <div>
                  <p className="text-[10.5px] font-bold uppercase tracking-[0.12em] text-ink-3">
                    Maths · Unit test
                  </p>
                  <p className="mt-1 text-[15px] font-extrabold tracking-tight text-navy-deep">
                    87<span className="text-ink-3">/100</span>
                    <span className="ml-1.5 align-middle text-[10px] font-bold text-success">
                      +6 vs last
                    </span>
                  </p>
                </div>
                <span className="rounded-full bg-accent/12 px-2 py-0.5 text-[9.5px] font-bold text-accent-deep">
                  Published
                </span>
              </div>
              {/* Sparkline */}
              <svg viewBox="0 0 120 28" className="mt-2 h-7 w-full" aria-hidden>
                <defs>
                  <linearGradient id="sparkFill" x1="0" y1="0" x2="0" y2="28">
                    <stop offset="0%" stopColor="#6C5CE0" stopOpacity="0.28" />
                    <stop offset="100%" stopColor="#6C5CE0" stopOpacity="0" />
                  </linearGradient>
                </defs>
                <path
                  d="M0,22 L18,18 L36,20 L54,14 L72,16 L90,9 L108,11 L120,5 L120,28 L0,28 Z"
                  fill="url(#sparkFill)"
                />
                <path
                  d="M0,22 L18,18 L36,20 L54,14 L72,16 L90,9 L108,11 L120,5"
                  fill="none"
                  stroke="#6C5CE0"
                  strokeWidth="1.6"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
                <circle cx="120" cy="5" r="2.2" fill="#6C5CE0" />
              </svg>
            </div>

            {/* FEES CARD */}
            <div className="mx-4 mb-2.5 flex items-center gap-3 rounded-[1.3rem] border border-white/70 bg-white/85 px-3.5 py-3 shadow-[0_1px_2px_rgba(38,35,77,0.04),0_14px_30px_-18px_rgba(38,35,77,0.22)] ring-1 ring-navy/[0.04] backdrop-blur">
              <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-gradient-to-br from-[#FFE9E1] to-[#FFD9CE] text-warning">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
                  <rect x="2" y="5" width="20" height="14" rx="2" />
                  <path d="M2 10h20" />
                </svg>
              </span>
              <span className="min-w-0 flex-1 leading-tight">
                <span className="block text-[12.5px] font-bold tracking-tight text-navy-deep">
                  Term fees · Q4
                </span>
                <span className="block text-[10px] text-ink-3">Due Apr 5 · Line-item breakdown</span>
              </span>
              <span className="shrink-0 text-right leading-tight">
                <span className="block text-[12.5px] font-extrabold tracking-tight text-navy-deep">
                  ₹18,400
                </span>
                <span className="block text-[9.5px] font-bold text-warning">Due in 9 days</span>
              </span>
            </div>

            {/* MESSAGES preview */}
            <div className="mx-4 mb-4 rounded-[1.3rem] border border-white/70 bg-white/85 px-3.5 py-3 shadow-[0_1px_2px_rgba(38,35,77,0.04),0_14px_30px_-18px_rgba(38,35,77,0.22)] ring-1 ring-navy/[0.04] backdrop-blur">
              <div className="flex items-center justify-between">
                <span className="flex items-center gap-2 text-[12.5px] font-bold tracking-tight text-navy-deep">
                  <span className="flex h-6 w-6 items-center justify-center rounded-lg bg-accent/12 text-accent-deep">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
                      <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5Z" />
                    </svg>
                  </span>
                  Messages
                </span>
                <span className="flex min-w-[18px] items-center justify-center rounded-full bg-accent px-1.5 text-[9.5px] font-bold text-white" style={{ height: 18 }}>
                  2
                </span>
              </div>

              {/* Latest message preview */}
              <div className="mt-2.5 flex items-start gap-2.5 rounded-xl bg-lavender-tint/60 px-2.5 py-2 ring-1 ring-white/50">
                <span className="mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-accent to-accent-deep text-[10px] font-extrabold text-white ring-1 ring-white/30">
                  I
                </span>
                <span className="min-w-0 leading-snug">
                  <span className="block text-[11px] font-bold tracking-tight text-navy-deep">
                    Class teacher · Ms. Iyer
                  </span>
                  <span className="block truncate text-[10.5px] text-ink-3">
                    PTM scheduled for Saturday, 10:30 AM. Tap to RSVP.
                  </span>
                </span>
              </div>
            </div>

            {/* GLASS TAB BAR */}
            <div className="sticky bottom-0 mt-1">
              <div className="mx-3 mb-3 flex items-center justify-between rounded-[1.4rem] border border-white/70 bg-white/80 px-4 py-2.5 shadow-[0_-1px_2px_rgba(38,35,77,0.03),0_18px_36px_-20px_rgba(38,35,77,0.30)] ring-1 ring-navy/[0.04] backdrop-blur-xl">
                {[
                  {
                    label: "Home",
                    active: true,
                    glyph: (
                      <path d="M3 9.5 12 3l9 6.5V20a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1V9.5Z" />
                    ),
                  },
                  {
                    label: "Results",
                    active: false,
                    glyph: (
                      <>
                        <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
                        <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2Z" />
                      </>
                    ),
                  },
                  {
                    label: "Fees",
                    active: false,
                    glyph: (
                      <>
                        <rect x="2" y="5" width="20" height="14" rx="2" />
                        <path d="M2 10h20" />
                      </>
                    ),
                  },
                  {
                    label: "More",
                    active: false,
                    glyph: (
                      <>
                        <circle cx="5" cy="12" r="1.6" />
                        <circle cx="12" cy="12" r="1.6" />
                        <circle cx="19" cy="12" r="1.6" />
                      </>
                    ),
                  },
                ].map((t) => (
                  <span
                    key={t.label}
                    className={`flex flex-col items-center gap-1 ${
                      t.active ? "text-accent-deep" : "text-ink-3"
                    }`}
                  >
                    <svg
                      width="18"
                      height="18"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="2"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      aria-hidden
                    >
                      {t.glyph}
                    </svg>
                    <span className="text-[8.5px] font-bold tracking-tight">{t.label}</span>
                  </span>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Floating glass plate, top-left: live attendance. */}
      <motion.div
        initial={{ opacity: 0, y: reduce ? 0 : 14, x: reduce ? 0 : -8 }}
        whileInView={{ opacity: 1, y: 0, x: 0 }}
        viewport={{ once: true, amount: 0.4 }}
        transition={{ duration: 0.6, ease: EASE_OUT_CUBIC, delay: 0.3 }}
        className="absolute -left-10 top-16 hidden items-center gap-2.5 rounded-2xl border border-white/70 bg-white/85 px-3.5 py-2.5 shadow-[0_2px_6px_rgba(38,35,77,0.06),0_24px_60px_-22px_rgba(38,35,77,0.34)] ring-1 ring-navy/[0.05] backdrop-blur-xl sm:flex"
      >
        <span className="flex h-8 w-8 items-center justify-center rounded-full bg-success/12 text-success">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
            <path d="M20 6 9 17l-5-5" />
          </svg>
        </span>
        <span className="leading-tight">
          <span className="block text-[12.5px] font-extrabold tracking-tight text-navy-deep">
            Marked present
          </span>
          <span className="block text-[10.5px] text-ink-3">Today · 8:42 AM</span>
        </span>
      </motion.div>

      {/* Floating glass plate, bottom-right: results published. */}
      <motion.div
        initial={{ opacity: 0, y: reduce ? 0 : 14, x: reduce ? 0 : 8 }}
        whileInView={{ opacity: 1, y: 0, x: 0 }}
        viewport={{ once: true, amount: 0.4 }}
        transition={{ duration: 0.6, ease: EASE_OUT_CUBIC, delay: 0.42 }}
        className="absolute -right-10 bottom-24 hidden items-center gap-2.5 rounded-2xl border border-white/70 bg-white/85 px-3.5 py-2.5 shadow-[0_2px_6px_rgba(38,35,77,0.06),0_24px_60px_-22px_rgba(38,35,77,0.34)] ring-1 ring-navy/[0.05] backdrop-blur-xl sm:flex"
      >
        <span className="flex h-8 w-8 items-center justify-center rounded-full bg-accent/12 text-accent-deep">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
            <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
            <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2Z" />
          </svg>
        </span>
        <span className="leading-tight">
          <span className="block text-[12.5px] font-extrabold tracking-tight text-navy-deep">
            Maths result published
          </span>
          <span className="block text-[10.5px] text-ink-3">Subject breakdown ready</span>
        </span>
      </motion.div>
    </motion.div>
  );
}
