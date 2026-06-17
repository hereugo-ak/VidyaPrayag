"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { Logo } from "./ui/Logo";
import { Button } from "./ui/Button";

const NAV = [
  { label: "Product", href: "/features" },
  { label: "About", href: "/about" },
  { label: "How it works", href: "/#how-it-works" },
  { label: "Pricing", href: "/pricing" },
];

// Active when the current path starts with the link's base path.
// "/" -segments only, anchor links (/#...) never count as active.
function isActivePath(pathname: string, href: string): boolean {
  if (href.includes("#")) return false;
  if (href === "/") return pathname === "/";
  return pathname === href || pathname.startsWith(`${href}/`);
}

export function Header() {
  const [scrolled, setScrolled] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const pathname = usePathname() ?? "/";

  // Backdrop blur appears AFTER 60px, a subtle reveal, not a jarring swap.
  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 60);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  // Lock body scroll + allow Escape to close while the mobile menu is open.
  useEffect(() => {
    document.body.style.overflow = menuOpen ? "hidden" : "";
    if (!menuOpen) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") setMenuOpen(false);
    };
    document.addEventListener("keydown", onKey);
    return () => {
      document.body.style.overflow = "";
      document.removeEventListener("keydown", onKey);
    };
  }, [menuOpen]);

  return (
    <header className="fixed inset-x-0 top-0 z-50">
      <div
        className={`transition-[background-color,backdrop-filter,box-shadow,border-color] duration-300 ease-out-cubic border-b ${
          scrolled
            ? "border-navy/8 bg-lavender/70 shadow-[0_1px_0_rgba(38,35,77,0.04)] backdrop-blur-xl backdrop-saturate-150"
            : "border-transparent bg-transparent"
        }`}
      >
        <nav className="shell flex h-[72px] items-center justify-between">
          <Logo />

          {/* Centre nav, evenly spaced links with subtle dot separators
              between them (the Grow+ reference treatment). */}
          <div className="hidden items-center gap-5 md:flex lg:gap-6">
            {NAV.map((item, i) => {
              const active = isActivePath(pathname, item.href);
              return (
                <div key={item.href} className="flex items-center gap-5 lg:gap-6">
                  <Link
                    href={item.href}
                    aria-current={active ? "page" : undefined}
                    className={`group relative text-sm text-navy-deep transition-colors hover:text-navy-deep ${
                      active ? "font-bold" : "font-medium text-ink-2"
                    }`}
                  >
                    {item.label}
                    {/* Underline: present (full) when active; otherwise slides
                        in from the left over 150ms on hover. */}
                    <span
                      className={`pointer-events-none absolute -bottom-1 left-0 h-[2px] rounded-full bg-navy-deep transition-[width] duration-150 ease-out-cubic ${
                        active ? "w-full" : "w-0 group-hover:w-full"
                      }`}
                    />
                  </Link>
                  {i < NAV.length - 1 && (
                    <span
                      aria-hidden
                      className="h-1 w-1 rounded-full bg-navy/20"
                    />
                  )}
                </div>
              );
            })}
          </div>

          <div className="hidden items-center gap-4 md:flex">
            {/* Vertical separator before the right-side actions (Grow+ pattern). */}
            <span aria-hidden className="h-5 w-px bg-navy/12" />
            <Link
              href="/login"
              className="text-sm font-semibold text-navy-deep transition-colors hover:text-accent"
            >
              Sign in
            </Link>
            <Button href="/onboarding" size="md">
              Onboard your school
            </Button>
          </div>

          {/* Mobile hamburger */}
          <button
            type="button"
            aria-label={menuOpen ? "Close menu" : "Open menu"}
            aria-expanded={menuOpen}
            onClick={() => setMenuOpen((v) => !v)}
            className="relative z-50 flex h-10 w-10 items-center justify-center rounded-full text-navy-deep md:hidden"
          >
            <span className="sr-only">Menu</span>
            <div className="flex flex-col gap-[5px]">
              <motion.span
                animate={menuOpen ? { rotate: 45, y: 7 } : { rotate: 0, y: 0 }}
                className="block h-[2px] w-6 bg-navy-deep"
              />
              <motion.span
                animate={menuOpen ? { opacity: 0 } : { opacity: 1 }}
                className="block h-[2px] w-6 bg-navy-deep"
              />
              <motion.span
                animate={menuOpen ? { rotate: -45, y: -7 } : { rotate: 0, y: 0 }}
                className="block h-[2px] w-6 bg-navy-deep"
              />
            </div>
          </button>
        </nav>
      </div>

      <AnimatePresence>
        {menuOpen && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.2 }}
            className="fixed inset-0 z-40 bg-lavender/95 backdrop-blur-xl md:hidden"
          >
            <div className="shell flex flex-col gap-1 pt-28">
              {NAV.map((item, i) => {
                const active = isActivePath(pathname, item.href);
                return (
                  <motion.div
                    key={item.href}
                    initial={{ opacity: 0, y: 12 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: 0.04 * i + 0.05, duration: 0.3 }}
                  >
                    <Link
                      href={item.href}
                      onClick={() => setMenuOpen(false)}
                      aria-current={active ? "page" : undefined}
                      className={`flex items-center gap-3 border-b border-navy/8 py-4 text-2xl tracking-tight text-navy-deep ${
                        active ? "font-extrabold" : "font-bold"
                      }`}
                    >
                      {active && (
                        <span className="h-2 w-2 rounded-full bg-accent" />
                      )}
                      {item.label}
                    </Link>
                  </motion.div>
                );
              })}
              <div className="mt-8 flex flex-col gap-3">
                <Button href="/onboarding" size="lg">
                  Onboard your school
                </Button>
                <Button href="/login" variant="secondary" size="lg">
                  Sign in
                </Button>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </header>
  );
}
