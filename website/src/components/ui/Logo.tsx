import Link from "next/link";

type Tone = "default" | "light";

/**
 * Enroll+ — the "Span" mark + wordmark.
 *
 * Evolution (not replacement) of the mobile app's "Setu" bridge mark
 * (composeApp/.../ui/v2/components/VLogo.kt + VBrandLogo.kt): an arc spanning
 * two grounded pillars over a deck, signalling the connection between school and
 * home. Redrawn here on a strict 64-unit grid — a single parabolic span, three
 * suspension cables and a navy keystone node that doubles as the "+" of Enroll+.
 * The geometry is identical to /public/brand/enrollplus-*.svg so the lockup is
 * byte-recognisable everywhere (favicon → hero → embossed card).
 *
 * The lavender base (#E6E6FA) is used with restraint as the plate only.
 */
export function EnrollMark({
  className = "",
  tone = "default",
}: {
  className?: string;
  tone?: Tone;
}) {
  const light = tone === "light";
  const ink = light ? "#ffffff" : "#26234D";
  const cable = light ? 0.5 : 0.42;
  const keystone = light ? "#E6E6FA" : "#26234D";
  return (
    <svg
      viewBox="0 0 64 64"
      fill="none"
      className={className}
      role="img"
      aria-hidden="true"
    >
      <defs>
        <linearGradient id="ep-span-c" x1="12" y1="40" x2="52" y2="40" gradientUnits="userSpaceOnUse">
          <stop offset="0" stopColor={light ? "#8B7EE8" : "#544AB8"} />
          <stop offset="0.5" stopColor={light ? "#B7AEF5" : "#6C5CE0"} />
          <stop offset="1" stopColor={light ? "#8B7EE8" : "#544AB8"} />
        </linearGradient>
      </defs>
      {light ? (
        <>
          <rect x="0" y="0" width="64" height="64" rx="16" fill="#1A1838" />
          <rect x="0.5" y="0.5" width="63" height="63" rx="15.5" stroke="#ffffff" strokeOpacity="0.08" />
        </>
      ) : (
        <>
          <rect x="2" y="2" width="60" height="60" rx="17" fill="#E6E6FA" />
          <rect x="2.5" y="2.5" width="59" height="59" rx="16.5" stroke="#26234D" strokeOpacity="0.06" />
        </>
      )}
      <path d="M14 40 Q32 14 50 40" stroke="url(#ep-span-c)" strokeWidth="3.4" strokeLinecap="round" />
      <path d="M12 44 H52" stroke={ink} strokeWidth="3.6" strokeLinecap="round" />
      <path d="M20 36.2 V44" stroke={ink} strokeOpacity={cable} strokeWidth="1.8" strokeLinecap="round" />
      <path d="M32 27 V44" stroke={ink} strokeOpacity={cable} strokeWidth="1.8" strokeLinecap="round" />
      <path d="M44 36.2 V44" stroke={ink} strokeOpacity={cable} strokeWidth="1.8" strokeLinecap="round" />
      <circle cx="14" cy="40" r="3" fill={ink} />
      <circle cx="50" cy="40" r="3" fill={ink} />
      <path d="M32 21.5 V32.5 M26.5 27 H37.5" stroke={keystone} strokeWidth="3.4" strokeLinecap="round" />
    </svg>
  );
}

/** The wordmark text — Enroll with an accent "+". */
export function EnrollWordmark({
  className = "",
  tone = "default",
}: {
  className?: string;
  tone?: Tone;
}) {
  const ink = tone === "light" ? "text-white" : "text-navy-deep";
  return (
    <span className={`font-extrabold tracking-tighter ${ink} ${className}`}>
      Enroll<span className="text-accent">+</span>
    </span>
  );
}

/**
 * Full lockup used in the marketing header/footer: mark + wordmark, links home.
 */
export function Logo({
  className = "",
  tone = "default",
}: {
  className?: string;
  tone?: Tone;
}) {
  return (
    <Link
      href="/"
      className={`group inline-flex items-center gap-2.5 ${className}`}
      aria-label="Enroll+ home"
    >
      <EnrollMark className="h-8 w-8 shrink-0 transition-transform duration-200 ease-out-cubic group-hover:scale-[1.04]" tone={tone} />
      <EnrollWordmark className="text-[18px]" tone={tone} />
    </Link>
  );
}
