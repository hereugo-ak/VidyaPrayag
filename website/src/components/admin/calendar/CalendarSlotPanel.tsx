"use client";

import { useEffect, useState, useCallback } from "react";
import { SidePanel } from "../SidePanel";
import { Skeleton, Badge } from "../Primitives";
import { classColor } from "@/lib/admin/classColor";
import { adminApi } from "@/lib/admin/client";
import { useDashboardIntelligence, useStudents } from "@/lib/admin/hooks";
import {
  classSectionLabel,
  dayRelation,
  longDate,
  parseIso,
  type DayRelation,
} from "@/lib/admin/calendarUtils";
import type { AttendanceDailyResponse, TimetablePeriod } from "@/lib/admin/types";

/**
 * Slot drill-down (CALENDAR_SPEC §4). Slides in from the right (reuses SidePanel,
 * already mirrored to ?panel= by the parent). Shows, for one period on one date:
 * teacher + attendance-marked status, present vs enrolled, syllabus coverage,
 * notes. Every state is designed (today-live / past / future / empty / loading /
 * error). The attendance read is ON-DEMAND — fetched once when the panel opens,
 * never polled.
 *
 * Homework honesty (§8): there is no school-admin homework read endpoint, so the
 * homework row is OMITTED rather than faked — never a fabricated "0 homework".
 */
export function CalendarSlotPanel({
  open,
  onClose,
  period,
  dateIso,
}: {
  open: boolean;
  onClose: () => void;
  period: TimetablePeriod | null;
  dateIso: string;
}) {
  const title = period
    ? `${period.subject || "Period"} · ${classSectionLabel(period)}`
    : "Slot";
  const date = parseIso(dateIso);
  const relation = dayRelation(date);

  return (
    <SidePanel
      open={open}
      onClose={onClose}
      title={title}
      subtitle={
        period
          ? `${longDate(date)} · ${period.start_time}–${period.end_time}`
          : longDate(date)
      }
    >
      {period ? (
        <SlotBody period={period} dateIso={dateIso} relation={relation} />
      ) : (
        <p className="text-[13px] text-ink-3">This slot is no longer available.</p>
      )}
    </SidePanel>
  );
}

function SlotBody({
  period,
  dateIso,
  relation,
}: {
  period: TimetablePeriod;
  dateIso: string;
  relation: DayRelation;
}) {
  const col = classColor(period.class_name);

  return (
    <div className="flex flex-col gap-5">
      {/* class-colour rail + relation tag */}
      <div className="flex items-center gap-2.5">
        <span className="h-8 w-1 rounded-full" style={{ backgroundColor: col.rail }} aria-hidden />
        <RelationTag relation={relation} />
      </div>

      <TeacherRow period={period} />

      <AttendanceSection period={period} dateIso={dateIso} relation={relation} />

      <SyllabusSection period={period} />

      {/* Homework row intentionally omitted — no school-admin homework read
          endpoint exists; we never fabricate a count (CALENDAR_SPEC §8). */}

      <NotesSection />
    </div>
  );
}

function RelationTag({ relation }: { relation: DayRelation }) {
  if (relation === "today") return <Badge tone="accent">Today · live</Badge>;
  if (relation === "past") return <Badge tone="neutral">Past</Badge>;
  return <Badge tone="neutral">Scheduled</Badge>;
}

function Section({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <h4 className="mb-2 text-[11px] font-bold uppercase tracking-[0.1em] text-ink-3">{label}</h4>
      {children}
    </div>
  );
}

function TeacherRow({ period }: { period: TimetablePeriod }) {
  return (
    <Section label="Teacher">
      <div className="flex items-center justify-between gap-2 rounded-xl bg-white/70 px-3.5 py-2.5">
        <div>
          <p className="text-[14px] font-semibold text-navy-deep">
            {period.teacher_name || "Unassigned"}
          </p>
          {period.room && <p className="text-[12px] text-ink-3">Room {period.room}</p>}
        </div>
      </div>
    </Section>
  );
}

// ── Attendance: the on-demand read, with all states ──
function AttendanceSection({
  period,
  dateIso,
  relation,
}: {
  period: TimetablePeriod;
  dateIso: string;
  relation: DayRelation;
}) {
  const [state, setState] = useState<"loading" | "ok" | "error">("loading");
  const [data, setData] = useState<AttendanceDailyResponse | null>(null);

  // Enrolment fallback for FUTURE slots (no attendance to read yet).
  const { data: roster } = useStudents(undefined, period.class_name);
  const enrolled = roster?.students.length ?? null;

  const load = useCallback(() => {
    setState("loading");
    adminApi
      .attendanceDaily("student", period.class_name, dateIso)
      .then((d) => {
        setData(d);
        setState("ok");
      })
      .catch(() => setState("error"));
  }, [period.class_name, dateIso]);

  useEffect(() => {
    // Future slots have no attendance — skip the call entirely.
    if (relation === "future") {
      setState("ok");
      setData(null);
      return;
    }
    load();
  }, [load, relation]);

  if (relation === "future") {
    return (
      <Section label="Students">
        <div className="rounded-xl bg-white/70 px-3.5 py-3">
          <p className="text-[13px] font-semibold text-navy-deep">Scheduled</p>
          <p className="mt-0.5 text-[12px] text-ink-3">
            {enrolled != null
              ? `${enrolled} student${enrolled === 1 ? "" : "s"} enrolled · attendance not applicable yet`
              : "Enrolment loading…"}
          </p>
        </div>
      </Section>
    );
  }

  return (
    <Section label={relation === "today" ? "Attendance — live" : "Attendance — recorded"}>
      {state === "loading" && (
        <div className="flex flex-col gap-2">
          <Skeleton className="h-5 w-40" />
          <Skeleton className="h-2.5 w-full" />
        </div>
      )}

      {state === "error" && (
        <div className="rounded-xl bg-danger/[0.06] px-3.5 py-3">
          <p className="text-[13px] text-ink-2">Couldn&apos;t load attendance for this class.</p>
          <button
            type="button"
            onClick={load}
            className="mt-2 rounded-lg bg-accent px-3 py-1.5 text-[12px] font-semibold text-white transition-colors hover:bg-accent-deep"
          >
            Retry
          </button>
        </div>
      )}

      {state === "ok" &&
        data &&
        (data.total_count > 0 ? (
          <div className="rounded-xl bg-white/70 px-3.5 py-3">
            <div className="mb-2 flex items-end justify-between">
              <p className="text-[20px] font-bold leading-none text-navy-deep">
                {data.present_count}
                <span className="text-[13px] font-medium text-ink-3"> / {data.total_count} present</span>
              </p>
              <Badge tone={relation === "today" ? "success" : "neutral"}>
                {relation === "today" ? "Marked" : "Recorded"} · {data.attendance_percentage}%
              </Badge>
            </div>
            <Bar value={data.present_count} total={data.total_count} />
            {data.absent_count > 0 && (
              <p className="mt-1.5 text-[12px] text-ink-3">{data.absent_count} absent</p>
            )}
          </div>
        ) : (
          <div className="rounded-xl bg-warning/[0.08] px-3.5 py-3">
            <p className="text-[13px] font-semibold text-warning">
              {relation === "today" ? "Attendance not marked yet" : "No attendance recorded"}
            </p>
            <p className="mt-0.5 text-[12px] text-ink-3">
              {enrolled != null ? `${enrolled} students enrolled.` : ""}
            </p>
          </div>
        ))}
    </Section>
  );
}

function Bar({ value, total }: { value: number; total: number }) {
  const pct = total > 0 ? Math.round((value / total) * 100) : 0;
  return (
    <div className="h-2 w-full overflow-hidden rounded-full bg-navy/8">
      <div className="h-full rounded-full bg-teal transition-all" style={{ width: `${pct}%` }} />
    </div>
  );
}

// ── Syllabus coverage (from academic_health, already loaded) ──
function SyllabusSection({ period }: { period: TimetablePeriod }) {
  const { data: intel, isLoading } = useDashboardIntelligence();
  const row = intel?.academic_health.rows.find((r) => r.class_name === period.class_name);
  const cell = row?.cells.find((c) => c.subject === period.subject);

  return (
    <Section label="Syllabus — topic progress">
      {isLoading ? (
        <Skeleton className="h-12 w-full" />
      ) : cell && cell.total_units > 0 ? (
        <div className="rounded-xl bg-white/70 px-3.5 py-3">
          <div className="mb-2 flex items-end justify-between">
            <p className="text-[14px] font-semibold text-navy-deep">
              {cell.covered_units}
              <span className="text-[12px] font-medium text-ink-3"> / {cell.total_units} units</span>
            </p>
            <Badge tone="accent">{Math.round(cell.percentage)}% covered</Badge>
          </div>
          <div className="h-2 w-full overflow-hidden rounded-full bg-navy/8">
            <div
              className="h-full rounded-full bg-accent transition-all"
              style={{ width: `${Math.min(100, Math.round(cell.percentage))}%` }}
            />
          </div>
        </div>
      ) : (
        <p className="rounded-xl bg-white/70 px-3.5 py-3 text-[13px] text-ink-3">
          Scheduled, no coverage data yet for {period.subject || "this subject"}.
        </p>
      )}
    </Section>
  );
}

// ── Notes ──
function NotesSection() {
  return (
    <Section label="Notes">
      <p className="rounded-xl bg-white/70 px-3.5 py-3 text-[13px] text-ink-3">
        No notes logged for this slot.
      </p>
    </Section>
  );
}
