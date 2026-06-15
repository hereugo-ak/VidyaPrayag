"use client";

import Link from "next/link";
import { motion } from "framer-motion";
import { forwardRef } from "react";

type Variant = "primary" | "secondary" | "ghost";
type Size = "md" | "lg";

const base =
  "inline-flex items-center justify-center gap-2 rounded-full font-semibold transition-colors duration-200 ease-out-cubic focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-2 focus-visible:ring-offset-lavender disabled:cursor-not-allowed disabled:opacity-60";

const variants: Record<Variant, string> = {
  primary:
    "bg-navy-deep text-white shadow-cta hover:bg-navy hover:shadow-ctaHover",
  secondary:
    "bg-white/80 text-navy-deep ring-1 ring-inset ring-navy/15 backdrop-blur hover:bg-white hover:ring-navy/25",
  ghost: "text-navy-deep hover:bg-navy/5",
};

const sizes: Record<Size, string> = {
  md: "h-11 px-5 text-sm",
  lg: "h-14 px-7 text-[15px]",
};

// Subtle scale (1.02) + shadow depth on hover. Nothing bounces.
const hover = { scale: 1.02 };
const tap = { scale: 0.99 };
const motionTransition = { type: "spring" as const, stiffness: 420, damping: 30 };

interface CommonProps {
  variant?: Variant;
  size?: Size;
  className?: string;
  children: React.ReactNode;
}

interface LinkProps extends CommonProps {
  href: string;
  onClick?: never;
  type?: never;
  disabled?: never;
}
interface BtnProps extends CommonProps {
  href?: never;
  onClick?: () => void;
  type?: "button" | "submit";
  disabled?: boolean;
}

type Props = LinkProps | BtnProps;

export const Button = forwardRef<HTMLElement, Props>(function Button(
  { variant = "primary", size = "md", className = "", children, ...rest },
  _ref
) {
  const cls = `${base} ${variants[variant]} ${sizes[size]} ${className}`;

  if ("href" in rest && rest.href) {
    return (
      <motion.div className="inline-flex" whileHover={hover} whileTap={tap} transition={motionTransition}>
        <Link href={rest.href} className={cls}>
          {children}
        </Link>
      </motion.div>
    );
  }

  const { onClick, type = "button", disabled } = rest as BtnProps;
  return (
    <motion.button
      type={type}
      onClick={onClick}
      disabled={disabled}
      className={cls}
      whileHover={disabled ? undefined : hover}
      whileTap={disabled ? undefined : tap}
      transition={motionTransition}
    >
      {children}
    </motion.button>
  );
});
