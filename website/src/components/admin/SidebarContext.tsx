"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";

/**
 * Sidebar layout state, shared between the <Sidebar/> and the content shell so
 * the main column reclaims space the instant the rail collapses (no layout
 * jank, no prop-drilling). Collapse is a *desktop-only* affordance; on
 * tablet/mobile the sidebar is a drawer and this flag is ignored.
 *
 * The collapsed preference is persisted to localStorage so an admin who prefers
 * the lean icon-rail keeps it across sessions — a small "this product remembers
 * me" detail that separates premium tools from generic dashboards.
 */
const STORAGE_KEY = "enroll.admin.sidebar.collapsed";

interface SidebarCtx {
  /** Desktop: lean icon-rail vs full labelled rail. */
  collapsed: boolean;
  toggleCollapsed: () => void;
  setCollapsed: (v: boolean) => void;
  /** Mobile/tablet: drawer open. */
  mobileOpen: boolean;
  setMobileOpen: (v: boolean) => void;
}

const Ctx = createContext<SidebarCtx | null>(null);

export function SidebarProvider({ children }: { children: React.ReactNode }) {
  const [collapsed, setCollapsedState] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);
  const [hydrated, setHydrated] = useState(false);

  // Restore the persisted preference after mount (avoids SSR/CSR mismatch).
  useEffect(() => {
    try {
      const raw = window.localStorage.getItem(STORAGE_KEY);
      if (raw === "1") setCollapsedState(true);
    } catch {
      /* private mode / no storage — fall back to expanded */
    }
    setHydrated(true);
  }, []);

  const setCollapsed = useCallback((v: boolean) => {
    setCollapsedState(v);
    try {
      window.localStorage.setItem(STORAGE_KEY, v ? "1" : "0");
    } catch {
      /* ignore */
    }
  }, []);

  const toggleCollapsed = useCallback(
    () => setCollapsed(!collapsed),
    [collapsed, setCollapsed]
  );

  const value = useMemo<SidebarCtx>(
    () => ({
      // Until hydrated, render expanded (the SSR default) to avoid a flash.
      collapsed: hydrated ? collapsed : false,
      toggleCollapsed,
      setCollapsed,
      mobileOpen,
      setMobileOpen,
    }),
    [collapsed, hydrated, toggleCollapsed, setCollapsed, mobileOpen]
  );

  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

export function useSidebar(): SidebarCtx {
  const ctx = useContext(Ctx);
  if (!ctx) throw new Error("useSidebar must be used within <SidebarProvider>");
  return ctx;
}
