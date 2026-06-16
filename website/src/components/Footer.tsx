import Link from "next/link";
import { Logo } from "./ui/Logo";
import { FOOTER_NAV, FOOTER_TAGLINE } from "@/lib/content";

export function Footer() {
  const year = new Date().getFullYear();
  return (
    <footer className="border-t border-navy/8 bg-white/40">
      <div className="shell py-16">
        <div className="grid gap-12 md:grid-cols-2 lg:grid-cols-[1.6fr_repeat(4,1fr)]">
          {/* Brand column — logo + one-line descriptor + social slots */}
          <div className="max-w-xs">
            <Logo />
            <p className="mt-4 text-sm leading-relaxed text-ink-3">
              The operating system for your school. One platform for your
              office, your teachers, and every parent.
            </p>
            {/* Social slots — left intentionally empty until real accounts exist. */}
            <div className="mt-6 flex gap-3" aria-label="Social links">
              {["X", "in", "IG"].map((s) => (
                <span
                  key={s}
                  className="flex h-9 w-9 items-center justify-center rounded-full bg-navy/5 text-[11px] font-semibold text-ink-3"
                  aria-hidden
                >
                  {s}
                </span>
              ))}
            </div>
          </div>

          {Object.entries(FOOTER_NAV).map(([group, links]) => (
            <div key={group}>
              <h3 className="text-[11px] font-bold uppercase tracking-[0.12em] text-ink-3">
                {group}
              </h3>
              <ul className="mt-4 space-y-3">
                {links.map((l) => {
                  const external = l.href.startsWith("mailto:");
                  return (
                    <li key={l.href}>
                      {external ? (
                        <a
                          href={l.href}
                          className="text-sm text-ink-2 transition-colors duration-150 hover:text-navy-deep"
                        >
                          {l.label}
                        </a>
                      ) : (
                        <Link
                          href={l.href}
                          className="text-sm text-ink-2 transition-colors duration-150 hover:text-navy-deep"
                        >
                          {l.label}
                        </Link>
                      )}
                    </li>
                  );
                })}
              </ul>
            </div>
          ))}
        </div>

        <div className="mt-14 flex flex-col gap-4 border-t border-navy/8 pt-8 text-sm text-ink-3 sm:flex-row sm:items-center sm:justify-between">
          <p>© {year} Enroll+. All rights reserved.</p>
          <p className="font-medium text-navy-deep/70">{FOOTER_TAGLINE}</p>
          <div className="flex gap-6">
            <Link
              href="/privacy"
              className="transition-colors duration-150 hover:text-navy-deep"
            >
              Privacy
            </Link>
            <Link
              href="/terms"
              className="transition-colors duration-150 hover:text-navy-deep"
            >
              Terms
            </Link>
            <Link
              href="/cookies"
              className="transition-colors duration-150 hover:text-navy-deep"
            >
              Cookies
            </Link>
          </div>
        </div>
      </div>
    </footer>
  );
}
