/**
 * Deterministic class → on-brand colour.
 *
 * The system has no per-class colour field, so the signature calendar derives a
 * STABLE colour from the class name: the same class always gets the same colour
 * across week view, month dots, the legend and the drill-down header.
 *
 * The palette is drawn from the design tokens (accent / teal / navy families +
 * tints) and deliberately AVOIDS the semantic colours (success / warning /
 * danger), which are reserved for data state and must never read as branding
 * (VColors.kt doctrine). Each entry carries a solid rail/dot colour and a soft
 * tinted background for chips.
 */

export interface ClassColor {
  /** Solid colour for the 3px chip rail + month dot. */
  rail: string;
  /** Soft tinted background for the chip surface. */
  tintBg: string;
  /** A readable ink colour to pair with tintBg. */
  ink: string;
}

// Curated, on-brand, semantic-safe palette. Hex values mirror tailwind tokens
// (accent #6C5CE0, teal #3CB9A9 / deep #006A60, navy #26234D) plus extra
// harmonised hues that stay clear of success/warning/danger.
const PALETTE: ClassColor[] = [
  { rail: "#6C5CE0", tintBg: "rgba(108,92,224,0.10)", ink: "#544AB8" }, // accent
  { rail: "#3CB9A9", tintBg: "rgba(60,185,169,0.12)", ink: "#006A60" }, // teal
  { rail: "#26234D", tintBg: "rgba(38,35,77,0.08)", ink: "#26234D" }, // navy
  { rail: "#8B7EE8", tintBg: "rgba(139,126,232,0.12)", ink: "#544AB8" }, // accent soft
  { rail: "#2F8FAE", tintBg: "rgba(47,143,174,0.12)", ink: "#1F6A82" }, // cerulean
  { rail: "#7A5CC0", tintBg: "rgba(122,92,192,0.12)", ink: "#553B91" }, // violet
  { rail: "#0F8A7E", tintBg: "rgba(15,138,126,0.12)", ink: "#006A60" }, // teal deep
  { rail: "#4C6EF0", tintBg: "rgba(76,110,240,0.10)", ink: "#3851B5" }, // indigo
];

/** Stable string hash (djb2) → palette index. */
function hashIndex(s: string, mod: number): number {
  let h = 5381;
  for (let i = 0; i < s.length; i++) {
    h = (h * 33) ^ s.charCodeAt(i);
  }
  return Math.abs(h) % mod;
}

export function classColor(className: string): ClassColor {
  const key = (className || "").trim().toLowerCase();
  if (!key) return PALETTE[0];
  return PALETTE[hashIndex(key, PALETTE.length)];
}
