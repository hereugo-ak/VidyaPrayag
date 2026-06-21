"use client";

/**
 * Roster preview — a glanceable slice of the student roster from the real
 * /api/v1/school/students list (scoped client-side to the global class filter
 * when set). It mirrors /admin/people and links there for the full surface;
 * this is the dashboard's "people at a glance", not the operate screen.
 *
 * SOURCE: students[]  ·  REFRESH: NEAR-LIVE (60s)
 */

import Link from "next/link";
import type { StudentListResponse } from "@/lib/admin/types";
import { Avatar, Card, CardHeader, EmptyState, Skeleton } from "./Primitives";
import { IconChevronRight, IconPeople } from "./icons";

export function RosterPreview({
  data,
  loading,
  scopeClass = null,
  limit = 8,
}: {
  data: StudentListResponse | undefined;
  loading: boolean;
  scopeClass?: string | null;
  limit?: number;
}) {
  const all = (data?.students ?? []).filter(
    (s) => !scopeClass || s.class_name === scopeClass,
  );
  const rows = all.slice(0, limit);

  return (
    <Card className="h-full pb-4" hover>
      <CardHeader
        title="Roster"
        subtitle={scopeClass ? `Students in ${scopeClass}` : "A glance across your students"}
        action={
          <Link
            href="/admin/people"
            className="inline-flex items-center gap-1 rounded-full px-3 py-1.5 text-[12.5px] font-bold text-accent-deep transition-colors hover:bg-accent/[0.06]"
          >
            Open roster <IconChevronRight width={14} height={14} />
          </Link>
        }
      />
      <div className="px-3 pt-2">
        {loading && !data ? (
          <div className="space-y-2 p-2">
            {Array.from({ length: 5 }).map((_, i) => (
              <Skeleton key={i} className="h-12" />
            ))}
          </div>
        ) : rows.length === 0 ? (
          <EmptyState
            icon={<IconPeople />}
            title={scopeClass ? `No students in ${scopeClass}` : "No students yet"}
            hint="Import your roster from People to see students here and unlock attendance, marks and fee tracking."
          />
        ) : (
          <ul className="grid gap-1 sm:grid-cols-2">
            {rows.map((s) => (
              <li key={s.id}>
                <Link
                  href="/admin/people"
                  className="flex items-center gap-3 rounded-2xl px-3 py-2.5 transition-colors hover:bg-navy/[0.035]"
                >
                  <Avatar name={s.full_name} size={34} />
                  <div className="min-w-0">
                    <p className="truncate text-[13px] font-semibold text-navy-deep">{s.full_name}</p>
                    <p className="truncate text-[12px] text-ink-3">
                      {s.class_name}
                      {s.section ? `-${s.section}` : ""} · Roll {s.roll_number}
                    </p>
                  </div>
                </Link>
              </li>
            ))}
          </ul>
        )}
        {all.length > rows.length && (
          <p className="px-3 py-2 text-[12px] text-ink-3">
            +{all.length - rows.length} more in the full roster
          </p>
        )}
      </div>
    </Card>
  );
}
