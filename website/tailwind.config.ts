import type { Config } from "tailwindcss";

/**
 * Enroll+ design tokens.
 * Brand families lifted verbatim from the app's design source of truth
 * (composeApp/.../ui/v2/theme/VColors.kt + VType.kt), harmonised with the
 * brief's lavender base (#E6E6FA). Nothing here is a generic Tailwind default.
 */
const config: Config = {
  content: ["./src/**/*.{ts,tsx,mdx}"],
  theme: {
    extend: {
      colors: {
        // Surfaces
        lavender: {
          DEFAULT: "#E6E6FA", // brief base
          soft: "#FCF8FF", // app `lavender` near-white
          tint: "#F1EFFB",
        },
        cream: "#F5F5F3",
        // Soft pastel data fills (sparingly, one per surface — reference language)
        peach: { DEFAULT: "#FF8A65", soft: "#FFE9E1" },
        sky: { DEFAULT: "#6C8DF5", soft: "#E6EEFD" },
        mint: { DEFAULT: "#3CB9A9", soft: "#DFF4EF" },
        // canvas behind the floating cards (cool off-white lavender)
        canvas: "#F4F3FA",
        // Brand, navy (primary ink + primary CTA)
        navy: {
          DEFAULT: "#26234D",
          deep: "#1A1838",
        },
        // Accent, lavender/violet family, used sparingly for active states
        accent: {
          DEFAULT: "#6C5CE0",
          soft: "#8B7EE8",
          deep: "#544AB8",
        },
        // Support, teal, one highlight per section max
        teal: {
          DEFAULT: "#3CB9A9",
          deep: "#006A60",
        },
        // Ink scale (text)
        ink: {
          DEFAULT: "#1A2422",
          2: "#3D4947",
          3: "#6D7A77",
          placeholder: "#BCC9C6",
        },
        // Semantic (data states only)
        success: "#1F7A4D",
        warning: "#B3651A",
        danger: "#B3261E",
      },
      fontFamily: {
        sans: ["var(--font-jakarta)", "ui-sans-serif", "system-ui", "sans-serif"],
        mono: ["var(--font-dmmono)", "ui-monospace", "monospace"],
      },
      letterSpacing: {
        tightest: "-0.03em",
        tighter: "-0.02em",
        tight: "-0.01em",
      },
      /**
       * Opacity scale, EXTENDED so every value we actually use generates a
       * utility. Tailwind's default scale (…20,25,30…) omits the fine low-end
       * steps this design leans on (4, 6, 8, 12, 18, 55, 85). Without these,
       * classes like `bg-white/85`, `bg-navy/8`, `divide-navy/6`, `bg-accent/12`
       * and `bg-warning/12` silently render NO colour, which read as missing
       * card surfaces, invisible dividers, and broken badges across the
       * dashboard. Defining the full set fixes that distortion at the root.
       */
      opacity: {
        0: "0",
        4: "0.04",
        5: "0.05",
        6: "0.06",
        8: "0.08",
        10: "0.10",
        12: "0.12",
        15: "0.15",
        18: "0.18",
        20: "0.20",
        25: "0.25",
        30: "0.30",
        40: "0.40",
        50: "0.50",
        55: "0.55",
        60: "0.60",
        70: "0.70",
        75: "0.75",
        80: "0.80",
        85: "0.85",
        90: "0.90",
        95: "0.95",
        100: "1",
      },
      maxWidth: {
        prose: "68ch",
        shell: "1200px",
      },
      boxShadow: {
        /**
         * Pillowy elevation system — tuned to the premium reference dashboards.
         * Cards are borderless and float on a soft canvas using high-blur,
         * low-opacity navy-tinted shadows (no harsh 1px lines). Two ambient
         * layers (a tight contact shadow + a wide diffuse halo) give the soft,
         * expensive "resting on velvet" feel that generic admin panels miss.
         */
        soft: "0 1px 2px rgba(38,35,77,0.03), 0 10px 30px -12px rgba(38,35,77,0.10)",
        card: "0 1px 2px rgba(38,35,77,0.03), 0 18px 44px -20px rgba(38,35,77,0.16)",
        cardHover: "0 2px 6px rgba(38,35,77,0.05), 0 30px 60px -24px rgba(38,35,77,0.24)",
        pillow: "0 2px 4px rgba(38,35,77,0.03), 0 24px 60px -28px rgba(38,35,77,0.22)",
        float: "0 8px 18px -8px rgba(108,92,224,0.30), 0 24px 48px -20px rgba(108,92,224,0.20)",
        cta: "0 8px 24px rgba(38,35,77,0.18)",
        ctaHover: "0 14px 34px rgba(38,35,77,0.26)",
        // dark high-contrast pill (active segmented tab)
        pill: "0 6px 16px -6px rgba(26,24,56,0.45)",
        inset: "inset 0 1px 0 0 rgba(255,255,255,0.6)",
      },
      borderRadius: {
        xl2: "1.25rem", // 20px
        "3xl": "1.5rem", // 24px
        "4xl": "1.75rem", // 28px — reference card radius
        "5xl": "2rem", // 32px — hero / large surfaces
      },
      backgroundImage: {
        // Soft pastel washes lifted from the reference designs (peach/lavender/mint)
        "wash-peach": "linear-gradient(135deg, #FFE9E1 0%, #FFD9CE 100%)",
        "wash-lavender": "linear-gradient(135deg, #ECE9FE 0%, #DDD6FB 100%)",
        "wash-mint": "linear-gradient(135deg, #DFF4EF 0%, #C9EBE3 100%)",
        "wash-sky": "linear-gradient(135deg, #E6EEFD 0%, #D6E3FB 100%)",
        // signature aurora behind the greeting hero
        "hero-aurora":
          "radial-gradient(120% 140% at 0% 0%, rgba(108,92,224,0.55) 0%, rgba(108,92,224,0) 55%), radial-gradient(120% 140% at 100% 0%, rgba(60,185,169,0.40) 0%, rgba(60,185,169,0) 55%), linear-gradient(135deg, #211E47 0%, #2B2756 55%, #1A1838 100%)",
      },
      keyframes: {
        shimmer: {
          "100%": { transform: "translateX(100%)" },
        },
        floaty: {
          "0%,100%": { transform: "translateY(0)" },
          "50%": { transform: "translateY(-4px)" },
        },
      },
      animation: {
        shimmer: "shimmer 1.6s infinite",
        floaty: "floaty 5s ease-in-out infinite",
      },
      transitionTimingFunction: {
        "out-cubic": "cubic-bezier(0.16, 1, 0.3, 1)",
      },
    },
  },
  plugins: [],
};

export default config;
