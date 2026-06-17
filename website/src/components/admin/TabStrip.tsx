"use client";

import type { TabId } from "@/lib/admin/useDashboardUrlState";
import {
  IconDashboard,
  IconMarks,
  IconFees,
  IconPeople,
  IconCalendar,
} from "./icons";

/**
 * Dashboard tab strip — the cognitive-mode switcher (see TAB_STRUCTURE.md).
 * Pills on desktop; a sticky horizontal scroller on mobile. Active tab is the
 * URL-encoded ?tab= value. Each tab is a distinct administrative mental context,
 * not a page.
 */
const TABS: { id: TabId; label: string; icon: (p: { className?: string }) => JSX.Element }[] = [
  { id: "overview", label: "Overview", icon: IconDashboard },
  { id: "academics", label: "Academics", icon: IconMarks },
  { id: "finance", label: "Finance", icon: IconFees },
  { id: "people", label: "People", icon: IconPeople },
  { id: "calendar", label: "Calendar", icon: IconCalendar },
];

export function TabStrip({
  tab,
  onTab,
}: {
  tab: TabId;
  onTab: (t: TabId) => void;
}) {
  return (
    <div className="sticky top-0 z-20 -mx-4 px-4 py-1 md:mx-0 md:px-0">
      <div
        role="tablist"
        aria-label="Dashboard sections"
        className="flex items-center gap-1.5 overflow-x-auto rounded-2xl border border-navy/8 bg-white/85 p-1.5 shadow-card backdrop-blur-sm [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
      >
        {TABS.map((t) => {
          const active = t.id === tab;
          const Icon = t.icon;
          return (
            <button
              key={t.id}
              role="tab"
              aria-selected={active}
              onClick={() => onTab(t.id)}
              className={`inline-flex shrink-0 items-center gap-1.5 rounded-xl px-3.5 py-2 text-[13px] font-semibold transition-all duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent/40 ${
                active
                  ? "bg-accent text-white shadow-sm"
                  : "text-ink-2 hover:bg-navy/[0.04] hover:text-navy-deep"
              }`}
            >
              <Icon className={active ? "text-white" : "text-ink-3"} />
              {t.label}
            </button>
          );
        })}
      </div>
    </div>
  );
}
