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
    <div className="sticky top-[72px] z-20 -mx-4 px-4 md:mx-0 md:px-0">
      <div
        role="tablist"
        aria-label="Dashboard sections"
        className="inline-flex max-w-full items-center gap-1 overflow-x-auto rounded-full bg-white/85 p-1.5 shadow-soft ring-1 ring-navy/[0.05] backdrop-blur-xl [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
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
              className={`group inline-flex shrink-0 items-center gap-2 rounded-full px-4 py-2.5 text-[13px] font-semibold transition-all duration-300 ease-out-cubic focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent/40 ${
                active
                  ? "bg-navy-deep text-white shadow-pill"
                  : "text-ink-2 hover:bg-navy/[0.05] hover:text-navy-deep"
              }`}
            >
              <Icon className={active ? "text-white" : "text-ink-3 group-hover:text-navy-deep"} />
              {t.label}
            </button>
          );
        })}
      </div>
    </div>
  );
}
