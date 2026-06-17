"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { EnrollMark } from "@/components/ui/Logo";
import { ADMIN_NAV } from "@/lib/admin/nav";
import { useAdminAuth } from "@/lib/admin/session";
import { useSchoolProfile } from "@/lib/admin/hooks";
import { useSidebar } from "./SidebarContext";
import { Avatar } from "./Primitives";
import { IconChevronLeft, IconClose, IconLogout } from "./icons";

/**
 * Admin sidebar — a collapsible command rail.
 *
 *  • Desktop: persistent. Toggles between a full labelled rail (264px) and a
 *    lean icon-rail (76px). The collapse preference is remembered across
 *    sessions (SidebarContext → localStorage). Collapsed items reveal their
 *    label as a floating tooltip on hover, so the rail never loses meaning.
 *  • Tablet / mobile: a drawer that slides in over a scrim.
 *
 * Active state is a single dark "pill" with a soft accent edge-marker — the
 * borderless, floating language of the rest of the dashboard, not a flat
 * highlight. The whole rail is a frosted white panel resting on the canvas.
 */
export function Sidebar() {
  const pathname = usePathname();
  const { session, signOut } = useAdminAuth();
  const { data: school } = useSchoolProfile();
  const { collapsed, toggleCollapsed, mobileOpen, setMobileOpen } = useSidebar();

  const close = () => setMobileOpen(false);

  return (
    <>
      {/* Scrim (mobile only) */}
      <div
        className={`fixed inset-0 z-40 bg-navy-deep/30 backdrop-blur-sm transition-opacity duration-200 lg:hidden ${
          mobileOpen ? "opacity-100" : "pointer-events-none opacity-0"
        }`}
        onClick={close}
        aria-hidden="true"
      />

      <aside
        data-collapsed={collapsed}
        className={`group/rail fixed inset-y-0 left-0 z-50 flex flex-col border-r border-navy/[0.05] bg-white/85 backdrop-blur-xl shadow-[10px_0_50px_-30px_rgba(38,35,77,0.4)] transition-[transform,width] duration-300 ease-out-cubic lg:translate-x-0 ${
          mobileOpen ? "translate-x-0" : "-translate-x-full"
        } ${collapsed ? "w-[76px]" : "w-[264px]"}`}
        aria-label="Admin navigation"
      >
        {/* Brand / school */}
        <div
          className={`flex items-center gap-2.5 pb-4 pt-5 ${
            collapsed ? "justify-center px-0" : "px-5"
          }`}
        >
          <Link
            href="/admin/dashboard"
            onClick={close}
            className="flex items-center gap-2.5 rounded-2xl outline-none focus-visible:ring-2 focus-visible:ring-accent/40"
            aria-label="Enroll+ dashboard"
          >
            <EnrollMark className="h-9 w-9 shrink-0" />
            {!collapsed && (
              <div className="min-w-0 flex-1 overflow-hidden">
                <p className="truncate text-[14px] font-bold leading-tight text-navy-deep">
                  {school?.name ?? "Enroll+"}
                </p>
                <p className="truncate text-[11px] text-ink-3">
                  {school ? `${school.board} · ${school.city}` : "School admin"}
                </p>
              </div>
            )}
          </Link>

          {/* Close (mobile drawer) */}
          {!collapsed && (
            <button
              onClick={close}
              className="rounded-lg p-1.5 text-ink-3 hover:bg-navy/5 lg:hidden"
              aria-label="Close menu"
            >
              <IconClose />
            </button>
          )}
        </div>

        {/* Nav */}
        <nav
          className={`flex-1 space-y-1 overflow-y-auto overflow-x-hidden py-2 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden ${
            collapsed ? "px-2.5" : "px-3"
          }`}
        >
          {ADMIN_NAV.map((item) => {
            const active =
              pathname === item.href || pathname.startsWith(item.href + "/");
            const Icon = item.icon;
            return (
              <Link
                key={item.href}
                href={item.href}
                onClick={close}
                aria-current={active ? "page" : undefined}
                title={collapsed ? item.label : undefined}
                className={`group/item relative flex items-center rounded-2xl text-[14px] font-semibold transition-all duration-200 ${
                  collapsed ? "justify-center px-0 py-3" : "gap-3 px-3 py-2.5"
                } ${
                  active
                    ? "bg-navy-deep text-white shadow-pill"
                    : "text-ink-2 hover:bg-navy/[0.05] hover:text-navy-deep"
                }`}
              >
                {/* Accent edge-marker for the active item (premium tell). */}
                {active && (
                  <span
                    className="absolute left-0 top-1/2 h-5 w-[3px] -translate-x-[1px] -translate-y-1/2 rounded-full bg-accent-soft"
                    aria-hidden="true"
                  />
                )}
                <span
                  className={`relative flex h-5 w-5 shrink-0 items-center justify-center ${
                    active
                      ? "text-white"
                      : "text-ink-3 group-hover/item:text-navy-deep"
                  }`}
                >
                  <Icon />
                </span>
                {!collapsed && <span className="truncate">{item.label}</span>}

                {/* Floating tooltip when collapsed */}
                {collapsed && (
                  <span
                    role="tooltip"
                    className="pointer-events-none absolute left-[calc(100%+10px)] z-50 origin-left scale-90 whitespace-nowrap rounded-xl bg-navy-deep px-2.5 py-1.5 text-[12px] font-semibold text-white opacity-0 shadow-cardHover transition-all duration-150 group-hover/item:scale-100 group-hover/item:opacity-100"
                  >
                    {item.label}
                  </span>
                )}
              </Link>
            );
          })}
        </nav>

        {/* Collapse toggle (desktop only) */}
        <div className={`hidden lg:block ${collapsed ? "px-2.5" : "px-3"} pb-1`}>
          <button
            onClick={toggleCollapsed}
            className={`flex w-full items-center rounded-2xl py-2.5 text-[13px] font-semibold text-ink-3 transition-colors hover:bg-navy/[0.05] hover:text-navy-deep ${
              collapsed ? "justify-center px-0" : "gap-3 px-3"
            }`}
            aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"}
            title={collapsed ? "Expand" : "Collapse"}
          >
            <span className="flex h-5 w-5 shrink-0 items-center justify-center">
              <IconChevronLeft
                className={`transition-transform duration-300 ${
                  collapsed ? "rotate-180" : ""
                }`}
              />
            </span>
            {!collapsed && <span>Collapse</span>}
          </button>
        </div>

        {/* User */}
        <div className={`border-t border-navy/[0.06] p-3 ${collapsed ? "px-2.5" : ""}`}>
          {collapsed ? (
            <div className="group/user relative flex justify-center">
              <button
                onClick={() => signOut()}
                className="rounded-full outline-none focus-visible:ring-2 focus-visible:ring-accent/40"
                aria-label="Log out"
                title="Log out"
              >
                <Avatar name={session?.name ?? "Admin"} size={40} />
              </button>
            </div>
          ) : (
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
          )}
        </div>
      </aside>
    </>
  );
}
