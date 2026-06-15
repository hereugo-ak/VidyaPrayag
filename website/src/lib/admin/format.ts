/** Small formatting helpers for the admin surface. Pure, no side effects. */

export function initials(name: string): string {
  return name
    .trim()
    .split(/\s+/)
    .map((p) => p[0]?.toUpperCase() ?? "")
    .slice(0, 2)
    .join("");
}

const CURRENCY_SYMBOL: Record<string, string> = {
  INR: "₹",
  USD: "$",
  EUR: "€",
  GBP: "£",
};

export function money(amount: number | undefined | null, currency = "INR"): string {
  const n = amount ?? 0;
  const sym = CURRENCY_SYMBOL[currency] ?? `${currency} `;
  return `${sym}${n.toLocaleString("en-IN", { maximumFractionDigits: 0 })}`;
}

export function compactMoney(amount: number | undefined | null, currency = "INR"): string {
  const n = amount ?? 0;
  const sym = CURRENCY_SYMBOL[currency] ?? `${currency} `;
  if (n >= 1_00_00_000) return `${sym}${(n / 1_00_00_000).toFixed(2)} Cr`;
  if (n >= 1_00_000) return `${sym}${(n / 1_00_000).toFixed(2)} L`;
  if (n >= 1_000) return `${sym}${(n / 1_000).toFixed(1)}k`;
  return `${sym}${n.toLocaleString("en-IN")}`;
}

export function pct(n: number): string {
  return `${Math.round(n)}%`;
}

/** A short, stable avatar background from a name (deterministic). */
export function avatarHue(name: string): string {
  let h = 0;
  for (let i = 0; i < name.length; i++) h = (h * 31 + name.charCodeAt(i)) % 360;
  return `hsl(${h} 42% 92%)`;
}
export function avatarInkHue(name: string): string {
  let h = 0;
  for (let i = 0; i < name.length; i++) h = (h * 31 + name.charCodeAt(i)) % 360;
  return `hsl(${h} 38% 34%)`;
}

/** Parse a server "delta" string like "+4.2%" → numeric sign for colouring. */
export function deltaSign(s: string | undefined | null): "up" | "down" | "flat" {
  if (!s) return "flat";
  if (s.trim().startsWith("-")) return "down";
  if (/[1-9]/.test(s)) return "up";
  return "flat";
}
