"use client";

import { useEffect } from "react";
import { usePathname, useRouter } from "next/navigation";
import { useAdminAuth } from "@/lib/admin/session";
import { ADMIN_NAV } from "@/lib/admin/nav";
import { EnrollMark } from "@/components/ui/Logo";
import { Sidebar } from "./Sidebar";
import { Topbar } from "./Topbar";
import { SidebarProvider, useSidebar } from "./SidebarContext";

const SCHOOL_ROLES = new Set(["school_admin", "school_staff", "admin"]);

function titleForPath(pathname: string): string {
  const match = ADMIN_NAV.find(
    (i) => pathname === i.href || pathname.startsWith(i.href + "/")
  );
  return match?.label ?? "Admin";
}

/**
 * The protected admin shell. Every /admin route renders inside this.
 *  - Unauthenticated → redirect to /login (with a returnTo).
 *  - Authenticated but wrong role → redirect to /login.
 *  - Authenticated school role → render the dashboard chrome.
 */
export function AdminShell({ children }: { children: React.ReactNode }) {
  return (
    <SidebarProvider>
      <AdminShellInner>{children}</AdminShellInner>
    </SidebarProvider>
  );
}

function AdminShellInner({ children }: { children: React.ReactNode }) {
  const { session, ready } = useAdminAuth();
  const router = useRouter();
  const pathname = usePathname();
  const { collapsed, mobileOpen, setMobileOpen } = useSidebar();

  useEffect(() => {
    if (!ready) return;
    if (!session) {
      const returnTo = encodeURIComponent(pathname);
      router.replace(`/login?returnTo=${returnTo}`);
      return;
    }
    if (!SCHOOL_ROLES.has(session.role)) {
      router.replace("/login");
    }
  }, [ready, session, router, pathname]);

  // Close the drawer on route change.
  useEffect(() => setMobileOpen(false), [pathname, setMobileOpen]);

  // A11y: Escape closes the mobile drawer.
  useEffect(() => {
    if (!mobileOpen) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") setMobileOpen(false);
    };
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [mobileOpen, setMobileOpen]);

  if (!ready || !session || !SCHOOL_ROLES.has(session.role)) {
    return (
      <div className="admin-canvas flex min-h-screen items-center justify-center">
        <div className="flex flex-col items-center gap-3">
          <EnrollMark className="h-12 w-12 animate-pulse" />
          <p className="text-sm font-medium text-ink-3">Loading your dashboard…</p>
        </div>
      </div>
    );
  }

  return (
    <div className="admin-canvas min-h-screen">
      <a
        href="#admin-content"
        className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-[60] focus:rounded-lg focus:bg-navy-deep focus:px-4 focus:py-2 focus:text-sm focus:font-semibold focus:text-white"
      >
        Skip to content
      </a>
      <Sidebar />
      <div
        className={`transition-[padding] duration-300 ease-out-cubic ${
          collapsed ? "lg:pl-[88px]" : "lg:pl-[268px]"
        }`}
      >
        <Topbar title={titleForPath(pathname)} />
        <main
          id="admin-content"
          className="mx-auto w-full max-w-[1340px] px-4 pb-12 pt-5 md:px-8 md:pb-16 md:pt-7"
        >
          {children}
        </main>
      </div>
    </div>
  );
}
