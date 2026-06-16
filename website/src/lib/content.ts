/**
 * Site copy — derived from the ACTUAL feature set in the codebase
 * (composeApp/.../ui/v2/screens/{school,teacher,parent} + the server routes).
 * No invented features. Every capability below maps to a real screen/endpoint.
 */

export const BRAND = {
  name: "Enroll+",
  tagline: "The operating system for your school.",
  // Derived from LandingRouting defaults + the product's positioning.
  subtagline:
    "One platform connecting your office, your teachers, and every parent — attendance, results, fees, and messaging, in real time.",
};

// Sparse social-proof: derived from what the platform actually supports.
export const SOCIAL_PROOF = [
  { value: "3", label: "Boards supported", note: "CBSE · ICSE · State" },
  { value: "4", label: "Connected roles", note: "Admin · Teacher · Parent · Staff" },
  { value: "20+", label: "Built-in modules", note: "From attendance to analytics" },
  { value: "1", label: "Source of truth", note: "Web + mobile, one backend" },
];

// FOR SCHOOLS — from SchoolDashboard, SchoolPeople, SchoolComms, SchoolRecords,
// AnalyticsDashboard, AdmissionsCrm, SchedulePtm, ResultsPublish, LeaveRequests,
// LinkRequests, DailyAttendance, StudentRoster, StaffProfile.
export const SCHOOL_FEATURES = [
  {
    key: "people",
    title: "People, in one roster",
    body: "Teachers, students and non-teaching staff in a single school-scoped directory. Provision a teacher and they can log in the same day — students and parents link to the exact roll number you approve.",
    points: ["Teacher provisioning", "Student roster", "Non-teaching staff", "Parent link approvals"],
  },
  {
    key: "comms",
    title: "Broadcasts that reach the right parents",
    body: "Send an announcement to the whole school, a single class, a section, a subject group, or named students. Every message is audience-scoped — no more group-chat noise.",
    points: ["Segmented announcements", "Class & section targeting", "WhatsApp delivery log", "Read receipts"],
  },
  {
    key: "records",
    title: "Records that roll up automatically",
    body: "Attendance, marks and a fees ledger aggregate into clean admin rollups. The data teachers enter on the ground becomes the report you read at your desk.",
    points: ["Attendance summary", "Marks summary", "Fees ledger", "Exam results publishing"],
  },
  {
    key: "analytics",
    title: "Analytics built on real entries",
    body: "Class performance, teacher performance, per-student trends and syllabus coverage — computed from the marks and attendance your staff actually record, never fabricated.",
    points: ["Class performance", "Teacher performance", "Student trends", "Syllabus coverage"],
  },
  {
    key: "admissions",
    title: "Admissions as a pipeline",
    body: "Capture enquiries, track them through stages, and convert a prospect into an enrolled student without re-typing a single field.",
    points: ["Enquiry capture", "Stage tracking", "Conversion", "Source attribution"],
  },
  {
    key: "ptm",
    title: "Parent-teacher meetings, organised",
    body: "Schedule a PTM, invite parents, and watch per-class turnout fill in as meetings happen. The day runs itself.",
    points: ["Schedule PTMs", "Invites & RSVPs", "Per-class progress", "Turnout metrics"],
  },
];

// FOR PARENTS — from ParentHome, ParentAcademics, ParentAttendanceCalendar,
// ParentFees, ParentLeave, ParentMessages, Scholarships, AiReportCard.
export const PARENT_FEATURES = [
  {
    title: "Your child's day, as it happens",
    body: "Attendance marked in class shows up on your phone the same morning. No more guessing whether they made it in.",
  },
  {
    title: "Marks and progress you can read",
    body: "Published results, subject-by-subject, with trends over the term — not a paper card you see twice a year.",
  },
  {
    title: "Fees without the front-office queue",
    body: "See exactly what's paid, what's due, and what's overdue, with a clear line-item breakdown per child.",
  },
  {
    title: "Leave and messages, two taps",
    body: "Apply for your child's leave straight to their class teacher, and message the school without exchanging phone numbers.",
  },
];

// FOR TEACHERS — from TeacherHome, TeacherClasses, TeacherAttendance,
// TeacherMarks, TeacherSyllabus, TeacherHomework, TeacherLeave, TeacherMessages.
export const TEACHER_FEATURES = [
  {
    title: "Only your classes",
    body: "Your home shows the classes and subjects you actually teach — scoped by the school's assignment graph, nothing else.",
  },
  {
    title: "Attendance in under a minute",
    body: "Mark a whole section present in a couple of taps; corrections sync instantly to parents and the admin rollup.",
  },
  {
    title: "Marks once, everywhere",
    body: "Enter an assessment's marks once. They publish to parents and feed analytics — no duplicate registers.",
  },
  {
    title: "Homework & syllabus tracking",
    body: "Set homework, track submissions, and tick off syllabus units as you cover them, all from one place.",
  },
];

// HOW IT WORKS — the real onboarding → teacher setup → parent app sequence.
export const HOW_IT_WORKS = [
  {
    step: "01",
    title: "Onboard your school",
    body: "Create your admin account and walk through four steps right here on the web — basics, branding, your academic structure, then launch. It takes minutes, not meetings.",
  },
  {
    step: "02",
    title: "Add your teachers",
    body: "Provision teachers as you set up classes and subjects. Each gets their own login and sees exactly the classes they teach.",
  },
  {
    step: "03",
    title: "Parents connect",
    body: "Parents download the app, verify their phone, and link to their child by roll number — which you approve. From then on, everything flows.",
  },
];

export const FOOTER_NAV = {
  Product: [
    { label: "For Schools", href: "/features#schools" },
    { label: "For Teachers", href: "/features#teachers" },
    { label: "For Parents", href: "/features#parents" },
    { label: "Pricing", href: "/pricing" },
  ],
  Company: [
    { label: "About us", href: "/about" },
    { label: "How it works", href: "/#how-it-works" },
    { label: "Onboard your school", href: "/onboarding" },
    { label: "Sign in", href: "/login" },
  ],
  Legal: [
    { label: "Privacy Policy", href: "/privacy" },
    { label: "Terms of Service", href: "/terms" },
    { label: "Cookie Policy", href: "/cookies" },
  ],
  Contact: [
    { label: "hello@enrollplus.app", href: "mailto:hello@enrollplus.app" },
    { label: "Support", href: "mailto:support@enrollplus.app" },
    { label: "Onboard your school", href: "/onboarding" },
  ],
};

// One-line product descriptor + closing line for the footer.
export const FOOTER_TAGLINE = "Built for schools that move fast.";
