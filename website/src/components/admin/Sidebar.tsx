"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { EnrollMark } from "@/components/ui/Logo";
import { ADMIN_NAV } from "@/lib/admin/nav";
import { useAdminAuth } from "@/lib/admin/session";
import { useSchoolProfile } from "@/lib/admin/hooks";
import { Avatar } from "./Primitives";
import { IconClose, IconLogout } from "./icons";

/**
 * Admin sidebar, persistent on desktop, drawer on tablet/mobile.
 * School name + logo at the top, user + logout at the bottom.
 */
export function Sidebar({
  open,
  onClose,
}: {
  open: boolean;
  onClose: () => void;
}) {
  const pathname = usePathname();
  const { session, signOut } = useAdminAuth();
  const { data: school } = useSchoolProfile();

  return (
    <>
      {/* Scrim (mobile) */}
      <div
        className={`fixed inset-0 z-40 bg-navy-deep/30 backdrop-blur-sm transition-opacity duration-200 lg:hidden ${
          open ? "opacity-100" : "pointer-events-none opacity-0"
        }`}
        onClick={onClose}
        aria-hidden="true"
      />

      <aside
        className={`fixed inset-y-0 left-0 z-50 flex w-[264px] flex-col bg-white/95 shadow-[8px_0_40px_-24px_rgba(38,35,77,0.25)] backdrop-blur-md transition-transform duration-300 ease-out-cubic lg:translate-x-0 ${
          open ? "translate-x-0" : "-translate-x-full"
        }`}
        aria-label="Admin navigation"
      >
        {/* Brand / school */}
        <div className="flex items-center gap-2.5 px-5 pb-4 pt-5">
          <EnrollMark className="h-9 w-9 shrink-0" />
          <div className="min-w-0 flex-1">
            <p className="truncate text-[14px] font-bold leading-tight text-navy-deep">
              {school?.name ?? "Enroll+"}
            </p>
            <p className="truncate text-[11px] text-ink-3">
              {school ? `${school.board} · ${school.city}` : "School admin"}
            </p>
          </div>
          <button
            onClick={onClose}
            className="rounded-lg p-1.5 text-ink-3 hover:bg-navy/5 lg:hidden"
            aria-label="Close menu"
          >
            <IconClose />
          </button>
        </div>

        {/* Nav */}
        <nav className="flex-1 space-y-1 overflow-y-auto px-3 py-2">
          {ADMIN_NAV.map((item) => {
            const active = pathname === item.href || pathname.startsWith(item.href + "/");
            const Icon = item.icon;
            return (
              <Link
                key={item.href}
                href={item.href}
                onClick={onClose}
                aria-current={active ? "page" : undefined}
                className={`group flex items-center gap-3 rounded-2xl px-3 py-2.5 text-[14px] font-semibold transition-all duration-200 ${
                  active
                    ? "bg-navy-deep text-white shadow-pill"
                    : "text-ink-2 hover:bg-navy/[0.05] hover:text-navy-deep"
                }`}
              >
                <span
                  className={`relative flex h-5 w-5 items-center justify-center ${
                    active ? "text-white" : "text-ink-3 group-hover:text-navy-deep"
                  }`}
                >
                  <Icon />
                </span>
                {item.label}
              </Link>
            );
          })}
        </nav>

        {/* User */}
        <div className="border-t border-navy/[0.06] p-3">
          <div className="flex items-center gap-3 rounded-2xl bg-navy/[0.03] px-2.5 py-2.5">
            <Avatar name={session?.name ?? "Admin"} size={36} />
            <div className="min-w-0 flex-1">
              <p className="truncate text-[13px] font-semibold text-navy-deep">
                {session?.name ?? "Admin"}
              </p>
              <p className="truncate text-[11px] capitalize text-ink-3">
                {session?.role?.replace("_", " ") ?? "school admin"}
              </p>
            </div>
            <button
              onClick={() => signOut()}
              className="rounded-lg p-2 text-ink-3 transition-colors hover:bg-danger/8 hover:text-danger"
              aria-label="Log out"
              title="Log out"
            >
              <IconLogout />
            </button>
          </div>
        </div>
      </aside>
    </>
  );
}
