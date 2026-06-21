import Link from "next/link";

type Tone = "default" | "light";

/**
 * Enroll+, the "Setu" bridge mark.
 *
 * This is the **exact mobile-app mark** rebuilt for the web, faithfully tracing
 * the app's single source of truth (composeApp/.../ui/v2/components/VBrandLogo.kt
 * → `drawBridge`): a quadratic span arcing between two grounded pillar caps over
 * a deck, with three suspension cables and a solid apex node, the connection
 * between school and home (infrastructure, not classroom imagery).
 *
 * The geometry is identical to the app, scaled 1:1 from the app's 56-unit
 * viewBox into a centred 64-unit plate (translate +4,+4). Only the *colour* is
 * changed: the teal accent of the app is replaced by the website's lavender /
 * violet system (#6C5CE0 family) on a restrained lavender plate, with navy ink
 * (#26234D) for the deck, cables and pillars, a premium, minimal lockup.
 *
 * The "+" lives only in the "Enroll+" wordmark text, never in the symbol.
 */
export function EnrollMark({
  className = "",
  tone = "default",
}: {
  className?: string;
  tone?: Tone;
}) {
  const light = tone === "light";
  // Ink for deck / cables / pillar caps.
  const ink = light ? "#FFFFFF" : "#26234D";
  // The accent that lives on the span arc + apex node.
  const accentSolid = light ? "#FFFFFF" : "#6C5CE0";
  const apex = light ? "#E6E6FA" : "#6C5CE0";
  const cableAlpha = light ? 0.7 : 0.78; // app uses 0.78

  return (
    <svg
      viewBox="0 0 64 64"
      fill="none"
      className={className}
      role="img"
      aria-hidden="true"
    >
      <defs>
        <linearGradient
          id={`ep-span-${tone}`}
          x1="16"
          y1="36"
          x2="48"
          y2="36"
          gradientUnits="userSpaceOnUse"
        >
          <stop offset="0" stopColor={light ? "#FFFFFF" : "#544AB8"} />
          <stop offset="0.5" stopColor={light ? "#FFFFFF" : "#6C5CE0"} />
          <stop offset="1" stopColor={light ? "#FFFFFF" : "#544AB8"} />
        </linearGradient>
      </defs>

      {/* Plate, restrained lavender (light) / deep navy card (dark). */}
      {light ? (
        <>
          <rect x="2" y="2" width="60" height="60" rx="18" fill="#1A1838" />
          <rect
            x="2.5"
            y="2.5"
            width="59"
            height="59"
            rx="17.5"
            stroke="#FFFFFF"
            strokeOpacity="0.08"
          />
        </>
      ) : (
        <>
          <rect x="2" y="2" width="60" height="60" rx="18" fill="#E6E6FA" />
          <rect
            x="2.5"
            y="2.5"
            width="59"
            height="59"
            rx="17.5"
            stroke="#26234D"
            strokeOpacity="0.06"
          />
        </>
      )}

      {/* === Exact app "Setu" bridge geometry (app 56-unit coords +4,+4) === */}
      {/* arc  M12 32 Q28 12 44 32  →  M16 36 Q32 16 48 36 */}
      <path
        d="M16 36 Q32 16 48 36"
        stroke={`url(#ep-span-${tone})`}
        strokeWidth="3.4"
        strokeLinecap="round"
      />

      {/* deck  M10 40 H46  →  M14 44 H50 */}
      <path d="M14 44 H50" stroke={ink} strokeWidth="4" strokeLinecap="round" />

      {/* suspension cables at x=18/28/38 → 22/32/42 (top of centre cable reaches the apex) */}
      <path
        d="M22 36 V44"
        stroke={ink}
        strokeOpacity={cableAlpha}
        strokeWidth="1.8"
        strokeLinecap="round"
      />
      <path
        d="M32 26 V44"
        stroke={ink}
        strokeOpacity={cableAlpha}
        strokeWidth="1.8"
        strokeLinecap="round"
      />
      <path
        d="M42 36 V44"
        stroke={ink}
        strokeOpacity={cableAlpha}
        strokeWidth="1.8"
        strokeLinecap="round"
      />

      {/* grounded pillar caps at (12,32)/(44,32) → (16,36)/(48,36) */}
      <circle cx="16" cy="36" r="2.9" fill={ink} />
      <circle cx="48" cy="36" r="2.9" fill={ink} />

      {/* apex node at (28,22) → (32,26), the accent keystone (no "+") */}
      <circle cx="32" cy="26" r="3" fill={apex} stroke={accentSolid} strokeWidth="0" />
    </svg>
  );
}

/** The wordmark text, Enroll with an accent "+". (Unchanged per brief.) */
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
      {/* Mark + wordmark scaled +30% from the original 32px / 18px lockup
          (→ ~42px mark, 23px wordmark) for a more confident header presence. */}
      <EnrollMark
        className="h-[42px] w-[42px] shrink-0 transition-transform duration-200 ease-out-cubic group-hover:scale-[1.04]"
        tone={tone}
      />
      <EnrollWordmark className="text-[23px]" tone={tone} />
    </Link>
  );
}
