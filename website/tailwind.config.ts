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
        // Brand — navy (primary ink + primary CTA)
        navy: {
          DEFAULT: "#26234D",
          deep: "#1A1838",
        },
        // Accent — lavender/violet family, used sparingly for active states
        accent: {
          DEFAULT: "#6C5CE0",
          soft: "#8B7EE8",
          deep: "#544AB8",
        },
        // Support — teal, one highlight per section max
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
      maxWidth: {
        prose: "68ch",
        shell: "1200px",
      },
      boxShadow: {
        // Navy-tinted elevation system (app §13.1)
        card: "0 1px 2px rgba(38,35,77,0.04), 0 8px 24px rgba(38,35,77,0.06)",
        cardHover: "0 2px 4px rgba(38,35,77,0.06), 0 18px 40px rgba(38,35,77,0.12)",
        cta: "0 8px 24px rgba(38,35,77,0.18)",
        ctaHover: "0 14px 34px rgba(38,35,77,0.26)",
      },
      borderRadius: {
        xl2: "1.25rem",
      },
      transitionTimingFunction: {
        "out-cubic": "cubic-bezier(0.16, 1, 0.3, 1)",
      },
    },
  },
  plugins: [],
};

export default config;
