/**
 * Pure date + slot helpers for the signature calendar. No React, no I/O — every
 * function here is deterministic and unit-testable. The calendar components lean
 * on these so the rendering layers stay declarative.
 *
 * Weekday convention matches the backend timetable: 1=Mon … 7=Sun
 * (java.time.DayOfWeek.value). JavaScript's Date.getDay() is 0=Sun … 6=Sat, so
 * everything that crosses that boundary goes through isoWeekday().
 */

import type { TimetablePeriod, TimetableWeekday } from "./types";

/** YYYY-MM-DD for a Date, in LOCAL time (avoids the UTC off-by-one of toISOString). */
export function isoDate(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

/** Parse a YYYY-MM-DD string into a LOCAL Date at midnight. Falls back to today. */
export function parseIso(s: string | null | undefined): Date {
  if (s && /^\d{4}-\d{2}-\d{2}$/.test(s)) {
    const [y, m, d] = s.split("-").map(Number);
    return new Date(y, m - 1, d);
  }
  const t = new Date();
  return new Date(t.getFullYear(), t.getMonth(), t.getDate());
}

/** Backend weekday (1=Mon … 7=Sun) for a Date. */
export function isoWeekday(d: Date): number {
  const js = d.getDay(); // 0=Sun … 6=Sat
  return js === 0 ? 7 : js;
}

/** Midnight-truncated clone. */
export function startOfDay(d: Date): Date {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate());
}

export function addDays(d: Date, n: number): Date {
  const c = startOfDay(d);
  c.setDate(c.getDate() + n);
  return c;
}

export function addMonths(d: Date, n: number): Date {
  const c = startOfDay(d);
  c.setMonth(c.getMonth() + n, 1);
  return c;
}

export function sameDay(a: Date, b: Date): boolean {
  return (
    a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()
  );
}

export function isToday(d: Date): boolean {
  return sameDay(d, new Date());
}

/** "past" | "today" | "future" relative to now (day granularity). */
export type DayRelation = "past" | "today" | "future";
export function dayRelation(d: Date): DayRelation {
  const today = startOfDay(new Date());
  const day = startOfDay(d);
  if (day.getTime() < today.getTime()) return "past";
  if (day.getTime() > today.getTime()) return "future";
  return "today";
}

/** The Monday-anchored week (7 dates Mon→Sun) containing `anchor`. */
export function weekDates(anchor: Date): Date[] {
  const wd = isoWeekday(anchor); // 1..7
  const monday = addDays(anchor, -(wd - 1));
  return Array.from({ length: 7 }, (_, i) => addDays(monday, i));
}

/**
 * The month grid for `anchor`: full weeks Mon→Sun that cover the month, so the
 * grid is always a clean rectangle. Returns dates (some belong to adjacent
 * months — callers grey those out).
 */
export function monthGridDates(anchor: Date): Date[] {
  const first = new Date(anchor.getFullYear(), anchor.getMonth(), 1);
  const gridStart = addDays(first, -(isoWeekday(first) - 1));
  const last = new Date(anchor.getFullYear(), anchor.getMonth() + 1, 0);
  const gridEnd = addDays(last, 7 - isoWeekday(last));
  const out: Date[] = [];
  for (let d = gridStart; d.getTime() <= gridEnd.getTime(); d = addDays(d, 1)) {
    out.push(d);
  }
  return out;
}

/** "Mon, 16 Jun" style short label. */
export function shortDate(d: Date): string {
  return d.toLocaleDateString("en-IN", { weekday: "short", day: "numeric", month: "short" });
}

/** "16 June 2026" long label. */
export function longDate(d: Date): string {
  return d.toLocaleDateString("en-IN", {
    weekday: "long",
    day: "numeric",
    month: "long",
    year: "numeric",
  });
}

export const WEEKDAY_SHORT = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];

/** "Grade 5-A" — the class/section label used across chips, legend, panel. */
export function classSectionLabel(p: Pick<TimetablePeriod, "class_name" | "section">): string {
  return p.section ? `${p.class_name}-${p.section}` : p.class_name;
}

/** First name of a (possibly empty) teacher_name. */
export function teacherFirstName(name: string): string {
  const t = (name || "").trim();
  if (!t) return "";
  return t.split(/\s+/)[0];
}

/**
 * Index the weekly timetable by weekday for O(1) day lookup, applying the
 * effective class scope. Returns periods sorted by start_time then position-ish
 * (the backend already sorts, but we re-sort defensively for safety).
 */
export function periodsForWeekday(
  weekdays: TimetableWeekday[] | undefined,
  weekday: number,
  scopeClass: string | null
): TimetablePeriod[] {
  const day = weekdays?.find((w) => w.weekday === weekday);
  if (!day) return [];
  const list = scopeClass
    ? day.periods.filter((p) => p.class_name === scopeClass)
    : day.periods;
  return [...list].sort((a, b) => a.start_time.localeCompare(b.start_time));
}

/**
 * Distinct class names present in the timetable, sorted. Prefers the explicit
 * `classes` array from the payload; falls back to deriving from periods.
 */
export function classesInView(
  classes: string[] | undefined,
  weekdays: TimetableWeekday[] | undefined,
  scopeClass: string | null
): string[] {
  if (scopeClass) return [scopeClass];
  if (classes && classes.length) return classes;
  const set = new Set<string>();
  weekdays?.forEach((w) => w.periods.forEach((p) => set.add(p.class_name)));
  return [...set].sort((a, b) => a.localeCompare(b, undefined, { numeric: true }));
}

/**
 * Encode/decode a slot identity into the ?panel= URL token, so a drill-down is
 * deep-linkable. Format: `slot:<periodId>:<isoDate>`. A whole-day agenda token
 * (month-cell click) is `day:<isoDate>`.
 */
export function encodeSlotPanel(periodId: string, date: string): string {
  return `slot:${periodId}:${date}`;
}
export function encodeDayPanel(date: string): string {
  return `day:${date}`;
}

export interface DecodedPanel {
  kind: "slot" | "day";
  periodId: string | null;
  date: string;
}
export function decodePanel(token: string | null): DecodedPanel | null {
  if (!token) return null;
  const parts = token.split(":");
  if (parts[0] === "slot" && parts.length >= 3) {
    return { kind: "slot", periodId: parts[1], date: parts.slice(2).join(":") };
  }
  if (parts[0] === "day" && parts.length >= 2) {
    return { kind: "day", periodId: null, date: parts.slice(1).join(":") };
  }
  return null;
}

/**
 * Effective calendar scope (CALENDAR_SPEC §2): Control A (calcls) wins inside
 * the calendar; if A is null but the global Control B (cls) is set, the embedded
 * calendar still honours the global scope. null = all classes.
 */
export function effectiveCalendarScope(
  calClass: string | null,
  globalClass: string | null
): string | null {
  return calClass ?? globalClass ?? null;
}
