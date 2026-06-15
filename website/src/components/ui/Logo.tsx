import Link from "next/link";

type Tone = "default" | "light";

/**
 * Enroll+ — the "Span" mark + wordmark.
 *
 * Evolution (not replacement) of the mobile app's "Setu" bridge mark
 * (composeApp/.../ui/v2/components/VLogo.kt + VBrandLogo.kt): a parabolic span
 * arcing between two grounded pillars over a deck, signalling the connection
 * between school and home — infrastructure, not classroom imagery.
 *
 * $100M redesign: redrawn on a strict 64-unit grid as a precise monoline system.
 * The "+" keystone is removed — the apex now resolves to a single solid node,
 * the true terminus of the centre cable (exactly the app's original centre dot).
 * Geometry is identical to /public/brand/enrollplus-*.svg so the lockup is
 * byte-recognisable everywhere (favicon → hero → embossed card).
 *
 * The lavender base (#E6E6FA) is used with restraint as the plate / span only.
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
  const cable = light ? 0.45 : 0.38;
  const node = light ? "#E6E6FA" : "#26234D";
  return (
    <svg
      viewBox="0 0 64 64"
      fill="none"
      className={className}
      role="img"
      aria-hidden="true"
    >
      <defs>
        <linearGradient id="ep-span-c" x1="14" y1="42" x2="50" y2="42" gradientUnits="userSpaceOnUse">
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
          <rect x="2" y="2" width="60" height="60" rx="18" fill="#E6E6FA" />
          <rect x="2.5" y="2.5" width="59" height="59" rx="17.5" stroke="#26234D" strokeOpacity="0.06" />
        </>
      )}
      <path d="M16 42 Q32 16 48 42" stroke="url(#ep-span-c)" strokeWidth="3.6" strokeLinecap="round" />
      <path d="M14 46 H50" stroke={ink} strokeWidth="3.8" strokeLinecap="round" />
      <path d="M22 38.2 V46" stroke={ink} strokeOpacity={cable} strokeWidth="1.9" strokeLinecap="round" />
      <path d="M32 26 V46" stroke={ink} strokeOpacity={cable} strokeWidth="1.9" strokeLinecap="round" />
      <path d="M42 38.2 V46" stroke={ink} strokeOpacity={cable} strokeWidth="1.9" strokeLinecap="round" />
      <circle cx="16" cy="42" r="3" fill={ink} />
      <circle cx="48" cy="42" r="3" fill={ink} />
      <circle cx="32" cy="24" r="3.6" fill={node} />
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
