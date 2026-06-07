import { ReactNode, CSSProperties, ButtonHTMLAttributes, InputHTMLAttributes, useState } from "react";
import { motion, AnimatePresence } from "motion/react";
import { Check, Loader2 } from "lucide-react";

// ─── utility ──────────────────────────────────────────────────────────────────
export function cx(...parts: (string | false | null | undefined)[]) {
  return parts.filter(Boolean).join(" ");
}

// ─── VLogo ────────────────────────────────────────────────────────────────────
// "Bridge" mark — a soft arc spanning two grounded pillars, signalling
// connection between school and home. Used everywhere; respects `tone`.
export function VLogo({
  size = 56,
  withWord = false,
  tone = "ink",
}: { size?: number; withWord?: boolean; tone?: "ink" | "white" | "teal" | "navy" }) {
  const inkColor = tone === "white" ? "#ffffff" : tone === "teal" ? "var(--teal-deep)" : tone === "navy" ? "var(--navy)" : "var(--ink)";
  const accent = tone === "white" ? "var(--teal)" : "var(--teal)";
  return (
    <div className="inline-flex items-center gap-2.5">
      <svg width={size} height={size} viewBox="0 0 56 56" fill="none">
        {/* outer rounded square plate */}
        <rect x="2" y="2" width="52" height="52" rx="14" fill={accent} opacity="0.12" />
        {/* upper bridge arc */}
        <path d="M12 32 Q28 12 44 32" stroke={accent} strokeWidth="3" strokeLinecap="round" fill="none" />
        {/* lower deck */}
        <path d="M10 40 H46" stroke={inkColor} strokeWidth="3.2" strokeLinecap="round" />
        {/* suspension cables */}
        <path d="M18 32 V40 M28 22 V40 M38 32 V40" stroke={inkColor} strokeWidth="1.4" strokeLinecap="round" opacity="0.7" />
        {/* pillars */}
        <circle cx="12" cy="32" r="2.2" fill={inkColor} />
        <circle cx="44" cy="32" r="2.2" fill={inkColor} />
      </svg>
      {withWord && (
        <span style={{ fontWeight: 800, fontSize: size * 0.38, letterSpacing: "-0.02em", color: inkColor, fontFamily: "'Plus Jakarta Sans', system-ui" }}>
          Vidya<span style={{ color: accent }}>S</span>etu
        </span>
      )}
    </div>
  );
}

// ─── VCard ────────────────────────────────────────────────────────────────────
// `dark` is now treated as a request for "elevated card on warm bg" — we
// render a white card in both modes so admin/teacher code keeps working.
export function VCard({ children, className, dark, padded = true, style }: { children: ReactNode; className?: string; dark?: boolean; padded?: boolean; style?: CSSProperties }) {
  return (
    <div
      className={cx("rounded-[16px]", padded && "p-4", className)}
      style={{
        background: "var(--cloud-pure)",
        border: "1px solid var(--border-light-1)",
        boxShadow: "var(--shadow-light-1)",
        ...style,
      }}
    >
      {children}
    </div>
  );
}

// ─── VButton ──────────────────────────────────────────────────────────────────
// Premium multi-state button: idle → loading (spinner) → success (check) → reset.
// `tone` lets each screen pick a palette that matches its surface, not just navy.
type Variant = "primary" | "secondary" | "ghost" | "destructive";
type Tone = "navy" | "teal" | "sky" | "peach" | "lavender" | "sand" | "rose" | "mint";

const tonePalette: Record<Tone, { bg: string; fg: string; shadow: string; soft: string; softFg: string; softBorder: string; softShadow: string }> = {
  navy:     { bg: "var(--navy)",      fg: "#ffffff", shadow: "rgba(38,35,77,0.30)",   soft: "#d8d2f1", softFg: "#26234d", softBorder: "rgba(38,35,77,0.22)",  softShadow: "rgba(38,35,77,0.16)" },
  teal:     { bg: "var(--teal-deep)", fg: "#ffffff", shadow: "rgba(0,106,96,0.28)",   soft: "#b9e6df", softFg: "#005048", softBorder: "rgba(0,106,96,0.30)",  softShadow: "rgba(0,106,96,0.18)" },
  sky:      { bg: "#3b78e7",          fg: "#ffffff", shadow: "rgba(59,120,231,0.30)", soft: "#cddcff", softFg: "#1a3f99", softBorder: "rgba(31,79,181,0.28)", softShadow: "rgba(31,79,181,0.18)" },
  peach:    { bg: "#e08a3c",          fg: "#ffffff", shadow: "rgba(224,138,60,0.30)", soft: "#fad0a8", softFg: "#7a3f0a", softBorder: "rgba(138,74,16,0.28)", softShadow: "rgba(138,74,16,0.18)" },
  lavender: { bg: "#7a6cf0",          fg: "#ffffff", shadow: "rgba(122,108,240,0.30)",soft: "#d6cdff", softFg: "#3527a8", softBorder: "rgba(61,45,181,0.28)", softShadow: "rgba(61,45,181,0.18)" },
  sand:     { bg: "#a88b5c",          fg: "#ffffff", shadow: "rgba(168,139,92,0.28)", soft: "#e8d8b6", softFg: "#5a4626", softBorder: "rgba(107,82,48,0.30)", softShadow: "rgba(107,82,48,0.18)" },
  rose:     { bg: "#c14a6a",          fg: "#ffffff", shadow: "rgba(193,74,106,0.28)", soft: "#f6cad6", softFg: "#6e1730", softBorder: "rgba(126,31,58,0.28)", softShadow: "rgba(126,31,58,0.18)" },
  mint:     { bg: "#2f9b7a",          fg: "#ffffff", shadow: "rgba(47,155,122,0.28)", soft: "#bce5d2", softFg: "#0e4d36", softBorder: "rgba(19,87,63,0.28)",  softShadow: "rgba(19,87,63,0.18)" },
};

export function VButton({
  variant = "primary",
  dark,
  full,
  size = "md",
  tone = "navy",
  soft = true,
  stateful,
  loading: loadingProp,
  successLabel,
  successDuration = 1400,
  onClick,
  children,
  ...rest
}: {
  variant?: Variant;
  dark?: boolean;
  full?: boolean;
  size?: "sm" | "md" | "lg";
  tone?: Tone;
  soft?: boolean;
  stateful?: boolean;
  loading?: boolean;
  successLabel?: ReactNode;
  successDuration?: number;
} & Omit<ButtonHTMLAttributes<HTMLButtonElement>, "tone">) {
  const pad = size === "sm" ? "px-3.5 py-2" : size === "lg" ? "px-6 py-3.5" : "px-4 py-2.5";
  const radius = size === "sm" ? 10 : 12;
  const pal = tonePalette[tone];
  const [phase, setPhase] = useState<"idle" | "loading" | "success">("idle");
  const active = stateful ? phase : loadingProp ? "loading" : "idle";

  const styles: Record<Variant, CSSProperties> = {
    primary: soft
      ? { background: pal.soft, color: pal.softFg, border: `1px solid ${pal.softBorder}`, boxShadow: `0 8px 20px -8px ${pal.softShadow}, inset 0 1px 0 rgba(255,255,255,0.55)` }
      : { background: pal.bg, color: pal.fg, boxShadow: `0 6px 14px -4px ${pal.shadow}, 0 2px 4px -2px ${pal.shadow}` },
    secondary: { background: "var(--cloud-pure)", color: "var(--ink)", border: "1px solid var(--border-light-2)" },
    ghost: { background: "transparent", color: soft ? pal.softFg : "var(--ink-2)" },
    destructive: { background: "#b3261e", color: "#ffffff", boxShadow: "0 6px 14px -4px rgba(179,38,30,0.30)" },
  };
  void dark;
  const isPrimaryFilled = variant === "primary" && !soft;

  const handleClick = (e: React.MouseEvent<HTMLButtonElement>) => {
    if (stateful && phase === "idle") {
      setPhase("loading");
      Promise.resolve(onClick?.(e)).finally(() => {
        setTimeout(() => setPhase("success"), 650);
        setTimeout(() => setPhase("idle"), 650 + successDuration);
      });
    } else {
      onClick?.(e);
    }
  };

  return (
    <button
      {...rest}
      onClick={handleClick}
      disabled={rest.disabled || active !== "idle"}
      className={cx("inline-flex items-center justify-center gap-2 transition-all hover:opacity-95 active:scale-[0.98] relative overflow-hidden group", pad, full && "w-full", rest.className)}
      style={{ borderRadius: radius, ...styles[variant], ...rest.style }}
    >
      {isPrimaryFilled && (
        <span
          aria-hidden
          className="pointer-events-none absolute inset-y-0 -translate-x-full group-hover:translate-x-[220%]"
          style={{
            width: "55%",
            background: "linear-gradient(90deg, transparent, rgba(255,255,255,0.32), transparent)",
            transform: "skewX(-20deg)",
            transition: "transform 900ms ease",
          }}
        />
      )}
      <AnimatePresence mode="wait" initial={false}>
        {active === "idle" && (
          <motion.span key="idle" initial={{ opacity: 0, y: 6 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -6 }} transition={{ duration: 0.18 }}
            className="relative inline-flex items-center gap-2">{children}</motion.span>
        )}
        {active === "loading" && (
          <motion.span key="loading" initial={{ opacity: 0, scale: 0.85 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0, scale: 0.85 }} transition={{ duration: 0.18 }}
            className="relative inline-flex items-center gap-2">
            <motion.span animate={{ rotate: 360 }} transition={{ duration: 0.9, repeat: Infinity, ease: "linear" }} className="inline-flex">
              <Loader2 size={size === "sm" ? 14 : 18} strokeWidth={2.4} />
            </motion.span>
          </motion.span>
        )}
        {active === "success" && (
          <motion.span key="success" initial={{ opacity: 0, scale: 0.7 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0, scale: 0.7 }} transition={{ duration: 0.22, type: "spring", stiffness: 400 }}
            className="relative inline-flex items-center gap-2">
            <Check size={size === "sm" ? 14 : 18} strokeWidth={3} />
            {successLabel || "Done"}
          </motion.span>
        )}
      </AnimatePresence>
    </button>
  );
}

// ─── VInput ───────────────────────────────────────────────────────────────────
export function VInput({ label, hint, dark: _dark, icon, ...rest }: { label?: string; hint?: string; dark?: boolean; icon?: ReactNode } & InputHTMLAttributes<HTMLInputElement>) {
  const [focused, setFocused] = useState(false);
  return (
    <label className="block">
      {label && (
        <span className="block mb-2" style={{ fontSize: 12, fontWeight: 600, color: "var(--ink-2)", letterSpacing: 0, textTransform: "none" }}>
          {label}
        </span>
      )}
      <div
        className="flex items-center gap-2 rounded-[12px] px-4 py-3.5 transition-all"
        style={{
          background: focused ? "var(--cloud-pure)" : "var(--cream)",
          border: focused ? "1px solid var(--teal-deep)" : "1px solid var(--border-light-1)",
          boxShadow: focused ? "0 0 0 4px rgba(60,209,190,0.15)" : "none",
          color: "var(--ink)",
        }}
      >
        {icon && <span style={{ color: focused ? "var(--teal-deep)" : "var(--ink-3)", transition: "color 200ms" }}>{icon}</span>}
        <input {...rest} onFocus={(e) => { setFocused(true); rest.onFocus?.(e); }} onBlur={(e) => { setFocused(false); rest.onBlur?.(e); }}
          className="bg-transparent outline-none flex-1" style={{ color: "inherit" }} />
      </div>
      {hint && <span className="block mt-1.5" style={{ fontSize: 12, color: "var(--ink-3)" }}>{hint}</span>}
    </label>
  );
}

// ─── VBadge / VTag ────────────────────────────────────────────────────────────
export function VBadge({ children, tone = "arctic", solid }: { children: ReactNode; tone?: "arctic" | "success" | "warning" | "danger" | "neutral"; solid?: boolean }) {
  const map: Record<string, { bg: string; fg: string }> = {
    arctic: { bg: "rgba(60,185,169,0.16)", fg: "var(--teal-deep)" },
    success: { bg: "rgba(168,230,207,0.42)", fg: "#155e3a" },
    warning: { bg: "rgba(255,212,163,0.55)", fg: "#7a3f00" },
    danger: { bg: "rgba(255,173,168,0.55)", fg: "#7a1c18" },
    neutral: { bg: "var(--cream)", fg: "var(--ink-2)" },
  };
  const { bg, fg } = map[tone];
  return (
    <span className="inline-flex items-center px-2.5 py-1 rounded-full" style={{ background: solid ? bg : bg, color: fg, fontSize: 11, fontWeight: 600, letterSpacing: "0.04em" }}>
      {children}
    </span>
  );
}

export function VTag({ children, active, onClick, dark }: { children: ReactNode; active?: boolean; onClick?: () => void; dark?: boolean }) {
  return (
    <button
      onClick={onClick}
      className="px-3 py-1.5 rounded-md whitespace-nowrap transition-all hover:scale-[1.02] active:scale-[0.98]"
      style={{
        fontSize: 12, fontWeight: 600,
        background: active ? "#dcf2ef" : "var(--cream)",
        color: active ? "#006a60" : "var(--ink-2)",
        border: active ? "1px solid rgba(0,106,96,0.18)" : "1px solid rgba(38,35,77,0.04)",
        boxShadow: active ? "0 2px 6px -2px rgba(0,106,96,0.18)" : "none",
        ...(void dark, {}),
      }}
    >
      {children}
    </button>
  );
}

// ─── VAvatar ──────────────────────────────────────────────────────────────────
export function VAvatar({ name, size = 40, src, ring }: { name: string; size?: number; src?: string; ring?: boolean }) {
  const initials = name.split(" ").slice(0, 2).map((p) => p[0]).join("").toUpperCase();
  const hash = name.split("").reduce((a, c) => a + c.charCodeAt(0), 0);
  const palette = ["#C8DEFF", "#A8E6CF", "#FFD4A3", "#FFADA8", "#E0D4FF", "#D4F3FF"];
  const bg = palette[hash % palette.length];
  return (
    <div
      className="rounded-full inline-flex items-center justify-center overflow-hidden shrink-0"
      style={{
        width: size, height: size,
        background: bg, color: "#080808",
        fontSize: size * 0.36, fontWeight: 700,
        border: ring ? "3px solid var(--cloud-pure)" : "none",
      }}
    >
      {src ? <img src={src} alt={name} className="w-full h-full object-cover" /> : initials}
    </div>
  );
}

// ─── VStatusDot ───────────────────────────────────────────────────────────────
export function VStatusDot({ tone = "arctic", pulse }: { tone?: "arctic" | "success" | "warning" | "danger"; pulse?: boolean }) {
  const map = { arctic: "var(--arctic)", success: "var(--success)", warning: "var(--warning)", danger: "var(--danger)" };
  return <span className={cx("inline-block rounded-full", pulse && "animate-pulse")} style={{ width: 8, height: 8, background: map[tone] }} />;
}

// ─── VProgressBar / VProgressRing ─────────────────────────────────────────────
export function VProgressBar({ value, tone = "arctic", dark: _dark }: { value: number; tone?: "arctic" | "success" | "warning" | "danger"; dark?: boolean }) {
  const colorMap = { arctic: "var(--teal-deep)", success: "#5fbf8f", warning: "#e08a3c", danger: "#c14a44" };
  return (
    <div className="w-full rounded-full overflow-hidden" style={{ height: 6, background: "var(--cream)" }}>
      <div className="h-full rounded-full transition-all" style={{ width: `${value}%`, background: colorMap[tone] }} />
    </div>
  );
}

export function VProgressRing({ value, size = 72, label }: { value: number; size?: number; label?: string }) {
  const r = (size - 10) / 2;
  const c = 2 * Math.PI * r;
  return (
    <div className="relative inline-flex items-center justify-center" style={{ width: size, height: size }}>
      <svg width={size} height={size} className="-rotate-90">
        <circle cx={size / 2} cy={size / 2} r={r} stroke="var(--cream)" strokeWidth="6" fill="none" />
        <circle cx={size / 2} cy={size / 2} r={r} stroke="var(--teal-deep)" strokeWidth="6" fill="none" strokeLinecap="round" strokeDasharray={`${(value / 100) * c} ${c}`} />
      </svg>
      <div className="absolute inset-0 flex flex-col items-center justify-center">
        <span className="font-mono" style={{ fontSize: size * 0.28, fontWeight: 500 }}>{value}%</span>
        {label && <span style={{ fontSize: 10, opacity: 0.6 }}>{label}</span>}
      </div>
    </div>
  );
}

// ─── VDivider ─────────────────────────────────────────────────────────────────
export function VDivider({ dark, className }: { dark?: boolean; className?: string }) {
  return <div className={cx("w-full", className)} style={{ height: 1, background: dark ? "var(--border-dark-1)" : "var(--border-light-1)" }} />;
}

// ─── VLabel ───────────────────────────────────────────────────────────────────
export function Label({ children, dark: _dark }: { children: ReactNode; dark?: boolean }) {
  return <span style={{ fontSize: 11, fontWeight: 700, letterSpacing: "0.10em", textTransform: "uppercase", color: "var(--ink-3)" }}>{children}</span>;
}

// ─── Empty state ──────────────────────────────────────────────────────────────
export function VEmptyState({ icon, title, body, action, dark }: { icon?: ReactNode; title: string; body?: string; action?: ReactNode; dark?: boolean }) {
  return (
    <div className="flex flex-col items-center justify-center text-center py-12 px-6">
      <div className="mb-4 inline-flex items-center justify-center rounded-full" style={{ width: 64, height: 64, background: dark ? "rgba(245,245,243,0.06)" : "rgba(8,8,8,0.04)" }}>
        {icon}
      </div>
      <h3 className="mb-1">{title}</h3>
      {body && <p style={{ color: dark ? "var(--text-dark-2)" : "var(--text-light-2)", maxWidth: 320, fontSize: 13 }}>{body}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  );
}

// ─── Coming Soon ──────────────────────────────────────────────────────────────
export function VComingSoon({ title, description, dark: _dark, preview }: { title: string; description: string; dark?: boolean; preview?: ReactNode }) {
  return (
    <VCard className="text-center">
      <div className="mx-auto mb-3 inline-flex items-center justify-center rounded-full px-3 py-1" style={{ background: "rgba(60,185,169,0.16)" }}>
        <span className="font-mono" style={{ fontSize: 11, fontWeight: 700, color: "var(--teal-deep)", letterSpacing: "0.06em" }}>● PREVIEW</span>
      </div>
      <h3 className="mb-1">{title}</h3>
      <p style={{ fontSize: 13, color: "var(--ink-2)" }}>{description}</p>
      {preview && <div className="mt-4">{preview}</div>}
      <button className="mt-4 inline-flex items-center gap-2 px-3 py-1.5 rounded-full" style={{ background: "var(--cream)", fontSize: 12, fontWeight: 600, color: "var(--ink-2)" }}>
        <span style={{ width: 6, height: 6, borderRadius: "50%", background: "var(--teal)" }} />
        Notify me when ready
      </button>
    </VCard>
  );
}

// ─── Top tabs ─────────────────────────────────────────────────────────────────
export function VTopTabs({ tabs, value, onChange, dark: _dark }: { tabs: string[]; value: string; onChange: (v: string) => void; dark?: boolean }) {
  return (
    <div className="flex gap-1 overflow-x-auto no-scrollbar px-4" style={{ borderBottom: "1px solid rgba(38,35,77,0.06)" }}>
      {tabs.map((t) => {
        const active = t === value;
        return (
          <button
            key={t}
            onClick={() => onChange(t)}
            className="px-3 py-3 whitespace-nowrap transition-colors relative"
            style={{
              fontSize: 13, fontWeight: 600,
              color: active ? "var(--teal-deep)" : "var(--ink-3)",
            }}
          >
            {t}
            {active && <span className="absolute left-3 right-3 -bottom-px h-[2.5px] rounded-full" style={{ background: "var(--teal-deep)" }} />}
          </button>
        );
      })}
    </div>
  );
}

// ─── Bottom nav ───────────────────────────────────────────────────────────────
export function VBottomNav({ items, value, onChange, dark: _dark }: { items: { id: string; label: string; icon: ReactNode; badge?: number }[]; value: string; onChange: (v: string) => void; dark?: boolean }) {
  return (
    <nav
      className="sticky bottom-0 left-0 right-0 px-2 pt-2 pb-3 flex items-center justify-around"
      style={{
        background: "#ffffff",
        borderTop: "1px solid rgba(38,35,77,0.06)",
        boxShadow: "0 -4px 20px rgba(38,35,77,0.04)",
      }}
    >
      {items.map((it) => {
        const active = it.id === value;
        return (
          <button key={it.id} onClick={() => onChange(it.id)} className="relative flex flex-col items-center gap-1 px-3 py-1.5 rounded-full transition-colors"
            style={{ color: active ? "var(--teal-deep)" : "var(--ink-3)" }}>
            <span className="relative">{it.icon}
              {it.badge && it.badge > 0 && (
                <span className="absolute -top-1 -right-2 rounded-full text-[9px] font-mono px-1.5 py-px"
                  style={{ background: "#c14a44", color: "#ffffff" }}>{it.badge}</span>
              )}
            </span>
            <span style={{ fontSize: 10, fontWeight: active ? 700 : 500 }}>{it.label}</span>
          </button>
        );
      })}
    </nav>
  );
}

// ─── Screen frame (phone-like surface) ────────────────────────────────────────
export function PhoneFrame({ children, dark, className }: { children: ReactNode; dark?: boolean; className?: string }) {
  // `dark` legacy: now applies the `.warm` scope so old admin/teacher styling
  // (which read --void / --cloud / --text-dark-*) renders as warm light theme.
  return (
    <div className={cx("mx-auto flex flex-col overflow-hidden relative", dark && "warm", className)}
      style={{
        width: "100%", maxWidth: 440, minHeight: "100vh",
        background: "var(--lavender)",
        color: "var(--ink)",
      }}>
      {children}
    </div>
  );
}

// ─── Header bar ───────────────────────────────────────────────────────────────
export function VBackHeader({ title, onBack, action, dark: _dark }: { title: string; onBack?: () => void; action?: ReactNode; dark?: boolean }) {
  return (
    <div className="flex items-center justify-between px-4 py-3.5" style={{ background: "#ffffff", borderBottom: "1px solid rgba(38,35,77,0.06)" }}>
      <button onClick={onBack} className="w-10 h-10 rounded-full inline-flex items-center justify-center" style={{ background: "var(--cream)", color: "var(--ink)" }}>
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2"><path d="M15 18l-6-6 6-6" /></svg>
      </button>
      <h3>{title}</h3>
      <div className="w-10 h-10 flex items-center justify-center" style={{ color: "var(--ink-2)" }}>{action}</div>
    </div>
  );
}

// ─── Hide scrollbar utility ───────────────────────────────────────────────────
const styleTag = document.createElement("style");
styleTag.innerHTML = `.no-scrollbar::-webkit-scrollbar{display:none}.no-scrollbar{scrollbar-width:none}`;
if (!document.head.querySelector("style[data-v-utils]")) {
  styleTag.setAttribute("data-v-utils", "");
  document.head.appendChild(styleTag);
}
