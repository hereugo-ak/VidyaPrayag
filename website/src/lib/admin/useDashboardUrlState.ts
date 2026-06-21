"use client";

/**
 * URL state for the entire dashboard workspace (LAW: URL state for everything).
 *
 * Every shareable piece of dashboard state lives in the query string so a
 * principal can deep-link or hand a colleague the exact view:
 *
 *   tab      active tab            overview | academics | finance | people | calendar
 *   cls      GLOBAL class filter   (Control B — scopes every panel)        e.g. "Grade 5"
 *   calcls   CALENDAR class filter (Control A — scopes only the calendar)  e.g. "Grade 5"
 *   calview  calendar view         week | month
 *   caldate  calendar anchor date  YYYY-MM-DD
 *   panel    open slot drill-down  an opaque slot id (see CalendarSlotPanel)
 *
 * cls and calcls are INDEPENDENT (the two-filter law): neither reads or writes
 * the other. Writes use router.replace (no history spam within a session) but
 * remain fully deep-linkable.
 */

import { useCallback, useMemo } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";

export type TabId = "overview" | "academics" | "finance" | "people" | "calendar";
export type CalView = "week" | "month";

const TABS: TabId[] = ["overview", "academics", "finance", "people", "calendar"];

function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

export interface DashboardUrlState {
  tab: TabId;
  globalClass: string | null;
  calClass: string | null;
  calView: CalView;
  calDate: string; // YYYY-MM-DD
  panel: string | null;

  setTab: (t: TabId) => void;
  setGlobalClass: (c: string | null) => void;
  setCalClass: (c: string | null) => void;
  setCalView: (v: CalView) => void;
  setCalDate: (d: string) => void;
  openPanel: (id: string) => void;
  closePanel: () => void;
}

export function useDashboardUrlState(): DashboardUrlState {
  const router = useRouter();
  const pathname = usePathname();
  const params = useSearchParams();

  const tabRaw = params.get("tab");
  const tab: TabId = TABS.includes(tabRaw as TabId) ? (tabRaw as TabId) : "overview";
  const globalClass = params.get("cls") || null;
  const calClass = params.get("calcls") || null;
  const calView: CalView = params.get("calview") === "month" ? "month" : "week";
  const calDate = params.get("caldate") || todayIso();
  const panel = params.get("panel") || null;

  const write = useCallback(
    (patch: Record<string, string | null>) => {
      const next = new URLSearchParams(params.toString());
      for (const [k, v] of Object.entries(patch)) {
        if (v === null || v === "") next.delete(k);
        else next.set(k, v);
      }
      const qs = next.toString();
      router.replace(qs ? `${pathname}?${qs}` : pathname, { scroll: false });
    },
    [params, pathname, router]
  );

  return useMemo<DashboardUrlState>(
    () => ({
      tab,
      globalClass,
      calClass,
      calView,
      calDate,
      panel,
      setTab: (t) => write({ tab: t === "overview" ? null : t }),
      setGlobalClass: (c) => write({ cls: c }),
      setCalClass: (c) => write({ calcls: c }),
      setCalView: (v) => write({ calview: v === "week" ? null : v }),
      setCalDate: (d) => write({ caldate: d === todayIso() ? null : d }),
      openPanel: (id) => write({ panel: id }),
      closePanel: () => write({ panel: null }),
    }),
    [tab, globalClass, calClass, calView, calDate, panel, write]
  );
}
