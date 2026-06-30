/**
 * Shared PEWS UI helpers — risk tone/label maps, slope/trajectory formatting,
 * and intervention status/outcome vocabulary. Pure, no side effects. Keeps the
 * web surface honest: we only describe what the deterministic snapshot says and
 * label-free language is reserved for the parent loop (not here).
 */
import type {
  PewsRiskLevel,
  PewsInterventionStatus,
  PewsOutcome,
} from "@/lib/admin/types";

export const RISK_TONE: Record<PewsRiskLevel, "danger" | "warning" | "neutral"> = {
  high: "danger",
  medium: "warning",
  watch: "neutral",
};

export const RISK_LABEL: Record<PewsRiskLevel, string> = {
  high: "High risk",
  medium: "Medium",
  watch: "Watch",
};

export const STATUS_TONE: Record<
  PewsInterventionStatus,
  "accent" | "warning" | "success" | "neutral"
> = {
  open: "warning",
  in_progress: "accent",
  done: "success",
  dismissed: "neutral",
};

export const STATUS_LABEL: Record<PewsInterventionStatus, string> = {
  open: "Open",
  in_progress: "In progress",
  done: "Resolved",
  dismissed: "Dismissed",
};

export const OUTCOME_TONE: Record<PewsOutcome, "success" | "neutral" | "danger"> = {
  improved: "success",
  unchanged: "neutral",
  worsened: "danger",
};

export const OUTCOME_LABEL: Record<PewsOutcome, string> = {
  improved: "Improved",
  unchanged: "No change",
  worsened: "Worsened",
};

/** Map a signal severity (1..3) to a stable dot colour. */
export function severityColor(severity: number): string {
  if (severity >= 3) return "#B3261E";
  if (severity === 2) return "#B3651A";
  return "#6D7A77";
}

/**
 * Describe a per-week trajectory slope in human terms. Positive marks/attendance
 * slope is improving; negative is declining. Returns null when there isn't
 * enough signal to be honest about a trend.
 */
export function describeSlope(
  slope: number | null | undefined,
  unit = "%"
): { text: string; tone: "up" | "down" | "flat" } | null {
  if (slope == null || Number.isNaN(slope)) return null;
  const rounded = Math.round(slope * 10) / 10;
  if (Math.abs(rounded) < 0.5) return { text: "Flat", tone: "flat" };
  const sign = rounded > 0 ? "+" : "";
  return {
    text: `${sign}${rounded}${unit}/week`,
    tone: rounded > 0 ? "up" : "down",
  };
}

/** Short, locale-aware date from an ISO timestamp; falls back to the raw value. */
export function shortDate(iso: string | null | undefined): string {
  if (!iso) return "—";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" });
}

/** Title-case a snake_case action type, e.g. "parent_call" → "Parent call". */
export function humanizeAction(action: string): string {
  const s = action.replace(/_/g, " ").trim();
  return s.charAt(0).toUpperCase() + s.slice(1);
}
