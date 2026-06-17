"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useNotifications } from "@/lib/admin/hooks";
import { adminApi } from "@/lib/admin/client";
import { IconBell, IconExternal, IconMenu } from "./icons";

/**
 * Admin top bar, mobile menu toggle, page title, live notification bell
 * (real unread count from /api/v1/notifications, polled live), Open Site link.
 */
export function Topbar({
  title,
  onMenu,
}: {
  title: string;
  onMenu: () => void;
}) {
  const { data, mutate } = useNotifications();
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  const unread = data?.unread_count ?? 0;
  const items = data?.notifications ?? [];

  useEffect(() => {
    const onClick = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") setOpen(false);
    };
    document.addEventListener("mousedown", onClick);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onClick);
      document.removeEventListener("keydown", onKey);
    };
  }, []);

  async function markAll() {
    try {
      await adminApi.markAllNotificationsRead();
      mutate();
    } catch {
      /* ignore */
    }
  }

  return (
    <header className="sticky top-0 z-30 flex h-16 items-center gap-3 border-b border-navy/8 bg-white/80 px-4 backdrop-blur-md md:px-6">
      <button
        onClick={onMenu}
        className="rounded-lg p-2 text-ink-2 hover:bg-navy/5 lg:hidden"
        aria-label="Open menu"
      >
        <IconMenu />
      </button>

      <h1 className="flex-1 truncate text-[17px] font-bold tracking-tight text-navy-deep">
        {title}
      </h1>

      <Link
        href="/"
        className="hidden items-center gap-1.5 rounded-full border border-navy/12 bg-white/70 px-3.5 py-1.5 text-[13px] font-semibold text-ink-2 transition-colors hover:border-navy/25 hover:text-navy-deep sm:inline-flex"
      >
        Open site <IconExternal width={14} height={14} />
      </Link>

      {/* Bell */}
      <div className="relative" ref={ref}>
        <button
          onClick={() => setOpen((v) => !v)}
          className="relative rounded-full p-2.5 text-ink-2 transition-colors hover:bg-navy/5"
          aria-label={`Notifications${unread ? `, ${unread} unread` : ""}`}
          aria-expanded={open}
        >
          <IconBell />
          {unread > 0 && (
            <span className="absolute right-1 top-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-accent px-1 text-[10px] font-bold text-white">
              {unread > 99 ? "99+" : unread}
            </span>
          )}
        </button>

        {open && (
          <div className="absolute right-0 mt-2 w-[340px] overflow-hidden rounded-2xl border border-navy/10 bg-white shadow-cardHover">
            <div className="flex items-center justify-between border-b border-navy/8 px-4 py-3">
              <p className="text-[14px] font-bold text-navy-deep">Notifications</p>
              {unread > 0 && (
                <button
                  onClick={markAll}
                  className="text-[12px] font-semibold text-accent hover:underline"
                >
                  Mark all read
                </button>
              )}
            </div>
            <div className="max-h-[360px] overflow-y-auto">
              {items.length === 0 ? (
                <p className="px-4 py-8 text-center text-[13px] text-ink-3">
                  You&apos;re all caught up.
                </p>
              ) : (
                items.slice(0, 12).map((n) => (
                  <div
                    key={n.id}
                    className={`flex gap-3 px-4 py-3 ${n.unread ? "bg-accent/4" : ""}`}
                  >
                    <span
                      className={`mt-1.5 h-2 w-2 shrink-0 rounded-full ${
                        n.unread ? "bg-accent" : "bg-transparent"
                      }`}
                      aria-hidden="true"
                    />
                    <div className="min-w-0">
                      <p className="truncate text-[13px] font-semibold text-navy-deep">{n.title}</p>
                      <p className="line-clamp-2 text-[12px] text-ink-3">{n.body}</p>
                      <p className="mt-0.5 text-[11px] text-ink-placeholder">{n.time}</p>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        )}
      </div>
    </header>
  );
}
