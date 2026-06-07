// Mock data — single school used across all portals so cross-references hold up.

export const school = {
  id: "svm",
  name: "Saraswati Vidya Mandir",
  shortName: "SVM",
  code: "SVM001",
  board: "CBSE",
  type: "Private Unaided",
  medium: "English",
  address: "12 Civil Lines, Lucknow, UP 226001",
  phone: "+91 522 220 4421",
  email: "office@svm.edu.in",
  principal: "Dr. Anita Verma",
  founded: 1987,
  academicYear: "2025–26",
  dayOfYear: 47,
  totalDays: 220,
};

export const classes = [
  { id: "9A", name: "Class 9", section: "A", strength: 32 },
  { id: "9B", name: "Class 9", section: "B", strength: 30 },
  { id: "10A", name: "Class 10", section: "A", strength: 34 },
  { id: "10B", name: "Class 10", section: "B", strength: 31 },
  { id: "12S", name: "Class 12", section: "Science", strength: 28 },
  { id: "12C", name: "Class 12", section: "Commerce", strength: 25 },
];

export const subjects = [
  { code: "MAT", name: "Mathematics", type: "Core" },
  { code: "SCI", name: "Science", type: "Core" },
  { code: "ENG", name: "English", type: "Core" },
  { code: "HIN", name: "Hindi", type: "Language" },
  { code: "SST", name: "Social Studies", type: "Core" },
  { code: "COMP", name: "Computer Applications", type: "Core" },
  { code: "PE", name: "Physical Education", type: "Co-curricular" },
];

export const teachers = [
  { id: "T01", name: "Dr. Ramesh Sharma", subjects: ["Chemistry", "Science"], classes: ["10A", "10B", "12S"], lastActive: "2h ago", username: "SVM001.T01", active: true, photo: "" },
  { id: "T02", name: "Mrs. Priya Iyer", subjects: ["Mathematics"], classes: ["9A", "9B", "10A"], lastActive: "15m ago", username: "SVM001.T02", active: true, photo: "" },
  { id: "T03", name: "Mr. Arjun Mehta", subjects: ["English"], classes: ["9A", "10A", "12S"], lastActive: "1d ago", username: "SVM001.T03", active: true, photo: "" },
  { id: "T04", name: "Ms. Kavita Singh", subjects: ["Hindi"], classes: ["9A", "9B"], lastActive: "3h ago", username: "SVM001.T04", active: true, photo: "" },
  { id: "T05", name: "Mr. Suresh Pillai", subjects: ["Social Studies"], classes: ["9A", "10A", "10B"], lastActive: "9d ago", username: "SVM001.T05", active: false, photo: "" },
  { id: "T06", name: "Mrs. Deepika Rao", subjects: ["Computer Applications"], classes: ["10A", "12C"], lastActive: "30m ago", username: "SVM001.T06", active: true, photo: "" },
  { id: "T07", name: "Mr. Vikram Joshi", subjects: ["Physical Education"], classes: ["9A", "9B", "10A", "10B"], lastActive: "4h ago", username: "SVM001.T07", active: true, photo: "" },
  { id: "T08", name: "Dr. Meera Kapoor", subjects: ["Physics"], classes: ["12S"], lastActive: "5h ago", username: "SVM001.T08", active: true, photo: "" },
];

export const students = [
  { id: "s1", name: "Aarav Khanna", roll: "01", class: "10A", attendance: 94, lastMarks: 86, fees: 0, parentMobile: "+91 98100 12345", parentName: "Rajeev Khanna", dob: "12 Aug 2010", gender: "M", today: "present" as const, pews: "ok" as const },
  { id: "s2", name: "Riya Sharma", roll: "02", class: "10A", attendance: 71, lastMarks: 74, fees: 12500, parentMobile: "+91 98100 22221", parentName: "Sneha Sharma", dob: "03 Jan 2010", gender: "F", today: "absent" as const, pews: "warn" as const },
  { id: "s3", name: "Kabir Singh", roll: "03", class: "10A", attendance: 88, lastMarks: 92, fees: 0, parentMobile: "+91 98100 33345", parentName: "Pratap Singh", dob: "21 Feb 2010", gender: "M", today: "present" as const, pews: "ok" as const },
  { id: "s4", name: "Ananya Verma", roll: "04", class: "10A", attendance: 96, lastMarks: 91, fees: 0, parentMobile: "+91 98100 44321", parentName: "Anita Verma", dob: "07 May 2010", gender: "F", today: "present" as const, pews: "ok" as const },
  { id: "s5", name: "Vihaan Kapoor", roll: "05", class: "10A", attendance: 58, lastMarks: 61, fees: 28500, parentMobile: "+91 98100 55512", parentName: "Mohan Kapoor", dob: "18 Sep 2010", gender: "M", today: "absent" as const, pews: "risk" as const },
  { id: "s6", name: "Ishaan Reddy", roll: "06", class: "10A", attendance: 90, lastMarks: 79, fees: 0, parentMobile: "+91 98100 66631", parentName: "Lakshmi Reddy", dob: "11 Oct 2010", gender: "M", today: "late" as const, pews: "ok" as const },
  { id: "s7", name: "Diya Patel", roll: "07", class: "10A", attendance: 92, lastMarks: 88, fees: 0, parentMobile: "+91 98100 77741", parentName: "Mahesh Patel", dob: "04 Jul 2010", gender: "F", today: "present" as const, pews: "ok" as const },
  { id: "s8", name: "Aryan Bose", roll: "08", class: "10A", attendance: 81, lastMarks: 72, fees: 5500, parentMobile: "+91 98100 88811", parentName: "Sanjay Bose", dob: "29 Nov 2010", gender: "M", today: "present" as const, pews: "ok" as const },
  { id: "s9", name: "Saanvi Joshi", roll: "09", class: "10A", attendance: 97, lastMarks: 95, fees: 0, parentMobile: "+91 98100 99921", parentName: "Reena Joshi", dob: "15 Mar 2010", gender: "F", today: "present" as const, pews: "ok" as const },
  { id: "s10", name: "Rohan Das", roll: "10", class: "10A", attendance: 64, lastMarks: 58, fees: 18200, parentMobile: "+91 98101 11111", parentName: "Bikash Das", dob: "08 Jun 2010", gender: "M", today: "absent" as const, pews: "warn" as const },
  { id: "s11", name: "Tara Menon", roll: "01", class: "9A", attendance: 89, lastMarks: 84, fees: 0, parentMobile: "+91 98102 22231", parentName: "Asha Menon", dob: "13 Apr 2011", gender: "F", today: "present" as const, pews: "ok" as const },
  { id: "s12", name: "Yash Agarwal", roll: "02", class: "9A", attendance: 76, lastMarks: 68, fees: 8200, parentMobile: "+91 98102 33341", parentName: "Sumit Agarwal", dob: "30 Jan 2011", gender: "M", today: "present" as const, pews: "ok" as const },
];

export const childForParent = students[1]; // "Riya Sharma" — the active parent's child
export const siblings = [students[1], students[10]]; // Riya + Tara as siblings for switcher demo

export const timetableToday = [
  { period: 1, subject: "Mathematics", class: "10A", time: "08:00 – 08:45", teacher: "Mrs. Priya Iyer" },
  { period: 2, subject: "Science", class: "10A", time: "08:45 – 09:30", teacher: "Dr. Ramesh Sharma" },
  { period: 3, subject: "English", class: "10A", time: "09:30 – 10:15", teacher: "Mr. Arjun Mehta" },
  { period: 4, subject: "Hindi", class: "10A", time: "10:30 – 11:15", teacher: "Ms. Kavita Singh" },
  { period: 5, subject: "Computer", class: "10A", time: "11:15 – 12:00", teacher: "Mrs. Deepika Rao" },
  { period: 6, subject: "PE", class: "10A", time: "12:00 – 12:45", teacher: "Mr. Vikram Joshi" },
];

export const classAttendanceGrid = classes.map((c, i) => ({
  ...c,
  pct: [92, 87, 84, 71, 95, 88][i],
}));

export const syllabusCoverage = [
  { class: "9-A", pct: 78 },
  { class: "9-B", pct: 94 },
  { class: "10-A", pct: 61 },
  { class: "10-B", pct: 72 },
  { class: "12-S", pct: 85 },
  { class: "12-C", pct: 80 },
];

export const recentTeacherActivity = [
  { who: "Dr. Ramesh Sharma", what: "updated Class 10-A Chemistry syllabus", when: "2h ago" },
  { who: "Mrs. Priya Iyer", what: "entered Class 9-A Math Unit Test marks", when: "3h ago" },
  { who: "Ms. Kavita Singh", what: "marked Class 9-B attendance", when: "5h ago" },
];

export const pendingActions = [
  { title: "2 parent messages awaiting reply", sub: "Overdue > 24 hrs", cta: "Reply" },
  { title: "Fee reminder batch due tomorrow", sub: "84 parents will be notified", cta: "Review" },
  { title: "Exam timetable not uploaded for Class 12", sub: "Half-yearly in 18 days", cta: "Upload" },
];

export const announcements = [
  { id: "a1", title: "Parent–Teacher Meeting — Class 10", date: "Tomorrow, 10:00 AM", body: "Half-yearly PTM for all Class 10 sections. Please carry the report card stub.", recipients: "Class 10 parents", opens: "234 / 316" },
  { id: "a2", title: "School closed on 12 June", date: "Sent 3 days ago", body: "Local civic holiday. Buses will not operate.", recipients: "All school", opens: "1,084 / 1,210" },
  { id: "a3", title: "Annual Sports Day fixtures", date: "Sent 6 days ago", body: "Trials begin Monday across all sections. Sports kit mandatory.", recipients: "All teachers + parents", opens: "942 / 1,210" },
];

export const messagesInbox = [
  { id: "m1", parent: "Sneha Sharma", child: "Riya Sharma — 10A", preview: "Riya was unwell yesterday, can the homework be shared?", time: "26h ago", overdue: true },
  { id: "m2", parent: "Mohan Kapoor", child: "Vihaan Kapoor — 10A", preview: "Following up on fee deferment request from last week.", time: "8h ago", overdue: false },
  { id: "m3", parent: "Asha Menon", child: "Tara Menon — 9A", preview: "Thank you for the swift response on the PTM slot.", time: "2d ago", overdue: false },
];

export const notifications = [
  { id: "n1", category: "attendance", title: "Riya was marked absent today", body: "By Mrs. Priya Iyer • Class 10-A", time: "2h ago", unread: true },
  { id: "n2", category: "academic", title: "Science syllabus updated", body: "Chapter 6 — Periodic Table covered today", time: "3h ago", unread: true },
  { id: "n3", category: "fees", title: "Term-2 fee due in 6 days", body: "₹12,500 — Tuition + Transport", time: "Yesterday", unread: false },
  { id: "n4", category: "announcement", title: "PTM scheduled — Class 10", body: "Tomorrow at 10:00 AM in the main hall", time: "Yesterday", unread: false },
  { id: "n5", category: "academic", title: "Mathematics Unit Test marks released", body: "Riya scored 74 / 100. Class avg 68.", time: "3 Jun", unread: false },
];

export const marksHistory = [
  { name: "Unit Test 1", date: "12 Apr", subject: "Mathematics", marks: 78, total: 100, avg: 64 },
  { name: "Unit Test 1", date: "12 Apr", subject: "Science", marks: 82, total: 100, avg: 71 },
  { name: "Mid Term", date: "18 May", subject: "Mathematics", marks: 71, total: 100, avg: 66 },
  { name: "Mid Term", date: "18 May", subject: "Science", marks: 75, total: 100, avg: 70 },
  { name: "Unit Test 2", date: "02 Jun", subject: "Mathematics", marks: 74, total: 100, avg: 68 },
  { name: "Unit Test 2", date: "02 Jun", subject: "Science", marks: 88, total: 100, avg: 73 },
];

export const subjectTrend = [
  { x: "UT1", you: 78, avg: 64 },
  { x: "Mid", you: 71, avg: 66 },
  { x: "UT2", you: 74, avg: 68 },
];

export const attendanceMonth: { day: number; status: "present" | "absent" | "late" | "holiday" | "future" }[] = [
  ...Array.from({ length: 30 }, (_, i) => {
    const d = i + 1;
    if (d > 24) return { day: d, status: "future" as const };
    if ([1, 8, 15, 22].includes(d)) return { day: d, status: "holiday" as const };
    if ([5, 12].includes(d)) return { day: d, status: "absent" as const };
    if (d === 17) return { day: d, status: "late" as const };
    return { day: d, status: "present" as const };
  }),
];

export const feeBreakdown = [
  { head: "Tuition Fee", amount: 8500, status: "Due", dueDate: "11 Jun 2026" },
  { head: "Transport", amount: 2200, status: "Due", dueDate: "11 Jun 2026" },
  { head: "Lab Fee", amount: 800, status: "Due", dueDate: "11 Jun 2026" },
  { head: "Activity Fee", amount: 1000, status: "Due", dueDate: "11 Jun 2026" },
];

export const feeHistory = [
  { date: "12 Apr 2026", amount: 12500, head: "Term-1 Composite", receipt: "RCP-00482" },
  { date: "10 Jan 2026", amount: 12500, head: "Term-3 (2025-26) Composite", receipt: "RCP-00321" },
  { date: "08 Oct 2025", amount: 12500, head: "Term-2 (2025-26) Composite", receipt: "RCP-00198" },
];

export const discoverySchools = [
  { id: "ds1", name: "Delhi Public School — Lucknow", board: "CBSE", type: "Private", coed: true, medium: "English", distance: "1.8 km", feeRange: "₹ 48,000 – ₹ 72,000", sri: 8.7, result: "96%", photo: "https://images.unsplash.com/photo-1728206348193-9b5ae74a7d32?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=1080" },
  { id: "ds2", name: "City Montessori School", board: "ICSE", type: "Private", coed: true, medium: "English", distance: "2.3 km", feeRange: "₹ 38,000 – ₹ 56,000", sri: 8.4, result: "94%", photo: "https://images.unsplash.com/photo-1719159381916-062fa9f435a6?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=1080" },
  { id: "ds3", name: "Saraswati Vidya Mandir", board: "CBSE", type: "Private", coed: true, medium: "English", distance: "0.6 km", feeRange: "₹ 25,000 – ₹ 45,000", sri: 7.9, result: "91%", photo: "https://images.unsplash.com/photo-1613897728606-6ccdee638d66?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=1080" },
  { id: "ds4", name: "St. Francis' College", board: "ICSE", type: "Private", coed: false, medium: "English", distance: "3.7 km", feeRange: "₹ 42,000 – ₹ 60,000", sri: 8.1, result: "93%", photo: "https://images.unsplash.com/photo-1651847162993-e7d4fe011eee?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=1080" },
  { id: "ds5", name: "Loreto Convent", board: "ICSE", type: "Private", coed: false, medium: "English", distance: "4.1 km", feeRange: "₹ 36,000 – ₹ 52,000", sri: 8.0, result: "92%", photo: "https://images.unsplash.com/photo-1698360296111-98d7d6a23d6f?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=1080" },
  { id: "ds6", name: "Kendriya Vidyalaya Cantt", board: "CBSE", type: "Central Govt", coed: true, medium: "English + Hindi", distance: "5.2 km", feeRange: "₹ 6,000 – ₹ 12,000", sri: 7.6, result: "89%", photo: "https://images.unsplash.com/photo-1568667256549-094345857637?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=1080" },
];

export const calendarEvents = [
  { day: 6, label: "PTM Class 10", type: "academic" as const },
  { day: 9, label: "Sports Trials", type: "event" as const },
  { day: 11, label: "Fee Due", type: "deadline" as const },
  { day: 12, label: "Civic Holiday", type: "holiday" as const },
  { day: 18, label: "Half-Yearly Begins", type: "academic" as const },
];
