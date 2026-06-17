/**
 * DESIGN PREVIEW FIXTURES — dev-only.
 *
 * These mirror the real Ktor DTOs (see types.ts) with realistic values so the
 * dashboard composition can be designed/reviewed without a live backend. They
 * are ONLY consumed by /admin/preview (a dev design harness) via an SWR
 * `fallback` map keyed by the exact SWR keys in hooks.ts. Production never
 * imports this — the real hooks fetch from the authed client.
 */

import { unstable_serialize } from "swr";
import type {
  DashboardIntelligenceDto,
  AttendanceSummaryDto,
  FeeLedgerDto,
  TimetableDto,
  CalendarResponse,
  StudentListResponse,
  TeacherListResponse,
  NotificationsDataDto,
  NotificationsSummaryDto,
  OnboardingStatusResponse,
  MarksSummaryDto,
} from "./types";

const CLASSES = ["Grade 4", "Grade 5", "Grade 6", "Grade 7"];

export const fxIntelligence: DashboardIntelligenceDto = {
  meta: {
    school_name: "Vidya Prayag Public School",
    academic_week: 24,
    today: new Date().toISOString().slice(0, 10),
    attendance_as_of: new Date().toISOString().slice(0, 10),
  },
  attendance_timeline: Array.from({ length: 30 }).map((_, i) => {
    const d = new Date();
    d.setDate(d.getDate() - (29 - i));
    const base = 92 + Math.sin(i / 3) * 4;
    const dip = i === 17 || i === 24;
    const rate = Math.round(Math.max(70, dip ? base - 16 : base));
    const total = 480;
    const present = Math.round((rate / 100) * total);
    return {
      date: d.toISOString().slice(0, 10),
      rate,
      present,
      absent: total - present,
      total,
      is_anomaly: dip,
      exam: i === 25 ? "Unit Test II" : null,
    };
  }),
  early_warning: [
    {
      student_code: "VP-0421",
      name: "Aarav Sharma",
      class_name: "Grade 6",
      section: "B",
      attendance_pct: 61,
      marks_pct: 44,
      leave_count: 6,
      risk_level: "high",
      signals: [
        { kind: "attendance", label: "Attendance 61% (below 75%)", severity: 3 },
        { kind: "marks", label: "Avg 44% — failing 2 subjects", severity: 3 },
        { kind: "leave", label: "6 leaves this month", severity: 2 },
      ],
    },
    {
      student_code: "VP-0188",
      name: "Diya Patel",
      class_name: "Grade 5",
      section: "A",
      attendance_pct: 68,
      marks_pct: 57,
      leave_count: 4,
      risk_level: "high",
      signals: [
        { kind: "attendance", label: "Attendance 68%", severity: 3 },
        { kind: "marks", label: "Avg 57% — slipping", severity: 2 },
      ],
    },
    {
      student_code: "VP-0533",
      name: "Kabir Singh",
      class_name: "Grade 7",
      section: "A",
      attendance_pct: 73,
      marks_pct: 62,
      leave_count: 3,
      risk_level: "medium",
      signals: [
        { kind: "attendance", label: "Attendance 73%", severity: 2 },
        { kind: "marks", label: "Avg 62%", severity: 1 },
      ],
    },
    {
      student_code: "VP-0299",
      name: "Ananya Rao",
      class_name: "Grade 4",
      section: "C",
      attendance_pct: 79,
      marks_pct: 66,
      leave_count: 2,
      risk_level: "medium",
      signals: [{ kind: "marks", label: "Avg 66% — watch", severity: 2 }],
    },
    {
      student_code: "VP-0612",
      name: "Vivaan Mehta",
      class_name: "Grade 6",
      section: "A",
      attendance_pct: 82,
      marks_pct: 70,
      leave_count: 1,
      risk_level: "watch",
      signals: [{ kind: "attendance", label: "Attendance 82% — watch", severity: 1 }],
    },
  ],
  academic_health: {
    subjects: ["Mathematics", "Science", "English", "Social", "Hindi"],
    rows: CLASSES.map((c, ci) => ({
      class_name: c,
      class_average: [78, 71, 64, 88][ci] ?? 70,
      cells: ["Mathematics", "Science", "English", "Social", "Hindi"].map((s, si) => {
        const total = 24;
        const pct = Math.max(20, Math.min(100, 90 - ci * 9 - si * 6 + (si === 0 ? 12 : 0)));
        return {
          subject: s,
          percentage: pct,
          covered_units: Math.round((pct / 100) * total),
          total_units: total,
        };
      }),
    })),
  },
  activity_feed: [
    { id: "a1", category: "attendance", actor: "Asha Rao", action: "marked attendance for", target: "Grade 5-A", iso_time: new Date(Date.now() - 6 * 60_000).toISOString() },
    { id: "a2", category: "leave", actor: "Rohan Das", action: "filed a leave request", target: "12–13 Jun", iso_time: new Date(Date.now() - 42 * 60_000).toISOString() },
    { id: "a3", category: "announcement", actor: "Principal", action: "published", target: "Annual Day notice", iso_time: new Date(Date.now() - 3 * 3600_000).toISOString() },
    { id: "a4", category: "marks", actor: "Neha Gupta", action: "published marks for", target: "Unit Test II · Science", iso_time: new Date(Date.now() - 5 * 3600_000).toISOString() },
    { id: "a5", category: "attendance", actor: "Sanjay Kumar", action: "marked attendance for", target: "Grade 7-A", iso_time: new Date(Date.now() - 7 * 3600_000).toISOString() },
  ],
};

export const fxAttendance: AttendanceSummaryDto = {
  latest_date: new Date().toISOString().slice(0, 10),
  present: 421,
  absent: 47,
  late: 12,
  total: 480,
  rate: 90,
  by_class: CLASSES.map((c, i) => {
    const total = [120, 118, 122, 120][i];
    const absent = [9, 14, 16, 8][i];
    const late = [3, 4, 3, 2][i];
    const present = total - absent - late;
    return {
      grade: c,
      present,
      absent,
      late,
      total,
      rate: Math.round(((present + late) / total) * 100),
    };
  }),
};

export const fxFees: FeeLedgerDto = {
  paid_total: 1842000,
  due_total: 386000,
  overdue_total: 124000,
  paid_count: 312,
  due_count: 74,
  overdue_count: 22,
  currency: "INR",
  recent: [
    { title: "Term II Tuition — Grade 6", amount: 18500, currency: "INR", status: "paid", due_date: "2026-06-10", category: "Tuition" },
    { title: "Transport — June", amount: 3200, currency: "INR", status: "due", due_date: "2026-06-20", category: "Transport" },
    { title: "Lab Fee — Grade 7", amount: 4500, currency: "INR", status: "overdue", due_date: "2026-06-05", category: "Lab" },
    { title: "Term II Tuition — Grade 5", amount: 17500, currency: "INR", status: "paid", due_date: "2026-06-10", category: "Tuition" },
    { title: "Annual Day — contribution", amount: 1500, currency: "INR", status: "due", due_date: "2026-06-25", category: "Event" },
  ],
};

function period(
  id: string,
  start: string,
  end: string,
  cls: string,
  section: string,
  subject: string,
  room: string,
  teacher: string,
) {
  return { id, start_time: start, end_time: end, class_name: cls, section, subject, room, teacher_id: id, teacher_name: teacher };
}

export const fxTimetable: TimetableDto = {
  classes: CLASSES,
  weekdays: [1, 2, 3, 4, 5].map((wd) => ({
    weekday: wd,
    periods: [
      period(`${wd}-1`, "08:30", "09:15", "Grade 5", "A", "Mathematics", "201", "Asha Rao"),
      period(`${wd}-2`, "08:30", "09:15", "Grade 6", "B", "Science", "Lab 1", "Neha Gupta"),
      period(`${wd}-3`, "09:20", "10:05", "Grade 4", "C", "English", "104", "Rohan Das"),
      period(`${wd}-4`, "09:20", "10:05", "Grade 7", "A", "Social", "302", "Sanjay Kumar"),
      period(`${wd}-5`, "10:25", "11:10", "Grade 5", "A", "Hindi", "201", "Meera Joshi"),
      period(`${wd}-6`, "11:15", "12:00", "Grade 6", "B", "Mathematics", "205", "Asha Rao"),
      period(`${wd}-7`, "12:40", "13:25", "Grade 7", "A", "Science", "Lab 2", "Neha Gupta"),
    ].slice(0, wd === 5 ? 5 : 7),
  })),
};

export const fxCalendar: CalendarResponse = {
  calendar_events: [
    { date: new Date().toISOString().slice(0, 10), day: "Today", event_id: "e1", event_title: "Unit Test II", event_description: "Science · Grade 7" },
  ],
  summary: { working_days: 5, total_working_days: 6, public_holidays: 0, school_holidays: 1 },
};

export const fxStudents: StudentListResponse = {
  students: Array.from({ length: 18 }).map((_, i) => ({
    id: `s${i}`,
    student_code: `VP-0${100 + i}`,
    full_name: ["Aarav Sharma", "Diya Patel", "Kabir Singh", "Ananya Rao", "Vivaan Mehta", "Ishaan Verma", "Saanvi Nair", "Aditya Iyer"][i % 8],
    class_name: CLASSES[i % CLASSES.length],
    section: ["A", "B", "C"][i % 3],
    roll_number: String(i + 1),
    profile_photo_url: null,
  })),
};

export const fxTeachers: TeacherListResponse = {
  teachers: ["Asha Rao", "Neha Gupta", "Rohan Das", "Sanjay Kumar", "Meera Joshi", "Priya Menon"].map((n, i) => ({
    id: `t${i}`,
    name: n,
    email: `${n.split(" ")[0].toLowerCase()}@vidyaprayag.edu`,
    phone: null,
    role: "school_staff",
    schoolId: "sch-1",
  })),
};

export const fxNotifications: NotificationsDataDto = {
  unread_count: 4,
  notifications: [],
};
export const fxNotificationsSummary: NotificationsSummaryDto = { unread_count: 4 };

export const fxOnboarding: OnboardingStatusResponse = {
  school_id: "sch-1",
  is_complete: true,
  completion_percent: 100,
  resume_step: "",
  total_step_count: 6,
  steps: [],
};

export const fxMarks: MarksSummaryDto = {
  assessment_count: 8,
  overall_average_pct: 71,
  assessments: [
    { subject: "Science", assessment: "Unit Test II", class_name: "Grade 7", average: 68, max_marks: 50, graded_count: 38, exam_date: "2026-06-12", is_published: true },
    { subject: "Mathematics", assessment: "Unit Test II", class_name: "Grade 6", average: 74, max_marks: 50, graded_count: 41, exam_date: "2026-06-11", is_published: true },
    { subject: "English", assessment: "Class Test", class_name: "Grade 5", average: 80, max_marks: 25, graded_count: 36, exam_date: "2026-06-09", is_published: false },
  ],
};

/** SWR fallback map keyed by the exact keys hooks.ts uses. */
export function previewFallback(): Record<string, unknown> {
  return {
    "dashboard/intelligence": fxIntelligence,
    "attendance/summary": fxAttendance,
    "fees/ledger": fxFees,
    "notifications": fxNotifications,
    "notifications/summary": fxNotificationsSummary,
    "onboarding/status": fxOnboarding,
    "marks/summary": fxMarks,
    "teachers": fxTeachers,
    // array-keyed hooks (SWR serializes arrays via JSON.stringify)
    [unstable_serialize(["timetable", ""])]: fxTimetable,
    [unstable_serialize(["calendar", isoToday(), "week"])]: fxCalendar,
    [unstable_serialize(["students", "", ""])]: fxStudents,
  };
}

function isoToday() {
  return new Date().toISOString().slice(0, 10);
}
