import {
  IconAnnounce,
  IconAttendance,
  IconDashboard,
  IconFees,
  IconLeave,
  IconMarks,
  IconPeople,
  IconSettings,
  IconBolt,
  IconAlumni,
  IconShield,
} from "@/components/admin/icons";

export interface NavItem {
  href: string;
  label: string;
  icon: (p: { className?: string }) => JSX.Element;
  superAdminOnly?: boolean;
}

/** Single source of truth for the admin sidebar + route map. */
export const ADMIN_NAV: NavItem[] = [
  { href: "/admin/dashboard", label: "Dashboard", icon: IconDashboard },
  { href: "/admin/people", label: "People", icon: IconPeople },
  { href: "/admin/alumni", label: "Alumni", icon: IconAlumni },
  { href: "/admin/attendance", label: "Attendance", icon: IconAttendance },
  { href: "/admin/early-warning", label: "Early Warning", icon: IconShield },
  { href: "/admin/marks", label: "Marks", icon: IconMarks },
  { href: "/admin/fees", label: "Fees", icon: IconFees },
  { href: "/admin/announcements", label: "Announcements", icon: IconAnnounce },
  { href: "/admin/leave", label: "Leave", icon: IconLeave },
  { href: "/admin/settings", label: "Settings", icon: IconSettings },
  { href: "/admin/dev-tools", label: "Dev Tools", icon: IconBolt, superAdminOnly: true },
];
