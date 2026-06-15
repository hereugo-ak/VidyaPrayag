"use client";

import { useMemo, useState } from "react";
import { Skeleton } from "./Primitives";

export interface Column<T> {
  key: string;
  header: string;
  /** value used for sorting + default render */
  accessor: (row: T) => string | number;
  /** custom cell render (defaults to accessor) */
  cell?: (row: T) => React.ReactNode;
  sortable?: boolean;
  align?: "left" | "right" | "center";
  className?: string;
}

/**
 * Generic sortable data table. Sorting is client-side on the accessor.
 * Filtering is the caller's job (passed-in rows are already filtered) so the
 * same table serves server-filtered and client-filtered lists.
 */
export function DataTable<T>({
  columns,
  rows,
  rowKey,
  loading,
  emptyState,
  onRowClick,
  initialSort,
}: {
  columns: Column<T>[];
  rows: T[];
  rowKey: (row: T) => string;
  loading?: boolean;
  emptyState?: React.ReactNode;
  onRowClick?: (row: T) => void;
  initialSort?: { key: string; dir: "asc" | "desc" };
}) {
  const [sort, setSort] = useState<{ key: string; dir: "asc" | "desc" } | null>(
    initialSort ?? null
  );

  const sorted = useMemo(() => {
    if (!sort) return rows;
    const col = columns.find((c) => c.key === sort.key);
    if (!col) return rows;
    const copy = [...rows];
    copy.sort((a, b) => {
      const av = col.accessor(a);
      const bv = col.accessor(b);
      if (typeof av === "number" && typeof bv === "number") {
        return sort.dir === "asc" ? av - bv : bv - av;
      }
      const r = String(av).localeCompare(String(bv), undefined, { numeric: true });
      return sort.dir === "asc" ? r : -r;
    });
    return copy;
  }, [rows, sort, columns]);

  function toggleSort(key: string, sortable?: boolean) {
    if (!sortable) return;
    setSort((cur) => {
      if (cur?.key !== key) return { key, dir: "asc" };
      if (cur.dir === "asc") return { key, dir: "desc" };
      return null;
    });
  }

  if (loading) {
    return (
      <div className="space-y-2 p-4">
        {Array.from({ length: 6 }).map((_, i) => (
          <Skeleton key={i} className="h-11" />
        ))}
      </div>
    );
  }

  if (sorted.length === 0) return <>{emptyState}</>;

  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[640px] border-collapse text-left">
        <thead>
          <tr className="border-b border-navy/8">
            {columns.map((c) => {
              const active = sort?.key === c.key;
              return (
                <th
                  key={c.key}
                  scope="col"
                  className={`px-4 py-3 text-[11px] font-bold uppercase tracking-wide text-ink-3 ${
                    c.align === "right" ? "text-right" : c.align === "center" ? "text-center" : "text-left"
                  } ${c.sortable ? "cursor-pointer select-none hover:text-navy-deep" : ""}`}
                  onClick={() => toggleSort(c.key, c.sortable)}
                  aria-sort={active ? (sort!.dir === "asc" ? "ascending" : "descending") : undefined}
                >
                  <span className="inline-flex items-center gap-1">
                    {c.header}
                    {c.sortable && (
                      <span className={`text-[9px] ${active ? "text-accent" : "text-ink-placeholder"}`}>
                        {active ? (sort!.dir === "asc" ? "▲" : "▼") : "↕"}
                      </span>
                    )}
                  </span>
                </th>
              );
            })}
          </tr>
        </thead>
        <tbody>
          {sorted.map((row) => (
            <tr
              key={rowKey(row)}
              onClick={onRowClick ? () => onRowClick(row) : undefined}
              className={`border-b border-navy/5 transition-colors last:border-0 ${
                onRowClick ? "cursor-pointer hover:bg-accent/[0.04]" : "hover:bg-navy/[0.02]"
              }`}
            >
              {columns.map((c) => (
                <td
                  key={c.key}
                  className={`px-4 py-3 text-[13.5px] text-ink-2 ${
                    c.align === "right" ? "text-right" : c.align === "center" ? "text-center" : "text-left"
                  } ${c.className ?? ""}`}
                >
                  {c.cell ? c.cell(row) : c.accessor(row)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
