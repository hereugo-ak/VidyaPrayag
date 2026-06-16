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

// ABOUT — editorial, founder-voice copy. Every claim is derived from what the
// product actually does (see SCHOOL/TEACHER/PARENT_FEATURES and the server
// routes). Numbers here are honest: capabilities the platform genuinely ships,
// NOT customer counts or revenue we don't have.
export const ABOUT = {
  hero: {
    eyebrow: "About Enroll+",
    title: "We built the school office we wished existed.",
    lede: "Enroll+ started with a simple frustration: a school's most important information — who showed up, how they're doing, what's owed — was scattered across registers, WhatsApp groups, and the memory of whoever sat at the front desk. We thought a school deserved one source of truth. So we built it.",
  },
  // The story, told in editorial long-form paragraphs.
  story: [
    {
      heading: "The problem was never the people. It was the plumbing.",
      paragraphs: [
        "Walk into most schools and you'll find extraordinary people held back by ordinary tools. A class teacher marks attendance on paper, then someone re-enters it into a spreadsheet, then a parent calls the office to ask whether their child made it in. The same fact travels three times and arrives late every time.",
        "We didn't want to add another app to the pile. We wanted to remove the re-typing, the phone tag, the twice-a-year report card. The goal was one system where the data a teacher enters on the ground becomes the report an admin reads at their desk and the update a parent sees on their phone — without anyone copying anything by hand.",
      ],
    },
    {
      heading: "One backend. Three honest points of view.",
      paragraphs: [
        "Enroll+ models a school the way it actually runs and then gives each role the exact slice they need. Admins run the whole institution from one desk — people, broadcasts, records, analytics, admissions, PTMs. Teachers see only the classes they teach and mark attendance or enter marks once. Parents get their child's day as it happens: attendance the same morning, published results subject by subject, a clear fees ledger, leave and messages in two taps.",
        "The web platform and the mobile apps share a single backend. There is no second copy of the truth to keep in sync, no export-import dance at the end of term. What you change in one place is what everyone sees.",
      ],
    },
    {
      heading: "We refuse to fabricate a single number.",
      paragraphs: [
        "Every figure in Enroll+ — class performance, teacher performance, per-student trends, syllabus coverage, the early-warning list that flags at-risk students — is computed from marks and attendance your staff actually recorded. If the data isn't there, we show you that it isn't, rather than inventing a confident-looking chart. A school runs on trust; analytics that lie are worse than no analytics at all.",
      ],
    },
  ],
  // Honest numbers — capabilities, not vanity metrics.
  numbers: [
    { value: "1", label: "Source of truth", note: "Web + mobile, one backend" },
    { value: "4", label: "Connected roles", note: "Admin · Teacher · Parent · Staff" },
    { value: "20+", label: "Built-in modules", note: "From attendance to analytics" },
    { value: "0", label: "Fabricated metrics", note: "Every number comes from real entries" },
  ],
  // What we believe — the principles the product is built on.
  principles: [
    {
      title: "Premium by subtraction",
      body: "The best school software gets out of the way. We earn the screen by removing steps, not adding them.",
    },
    {
      title: "Real-time where it matters",
      body: "Attendance, messages and dues update when they change. The things a parent worries about shouldn't wait for a nightly batch.",
    },
    {
      title: "One source of truth",
      body: "The website and the apps speak to the same backend. No reconciliation, no two versions of the same student.",
    },
    {
      title: "Honest data, always",
      body: "If we can't compute a number from real entries, we don't show one. No placeholder scores, no invented trends.",
    },
  ],
  // Team — placeholder STRUCTURE, clearly labelled as roles to be filled.
  // No invented names or photos; honest about being early.
  team: {
    note: "We're an early, focused team. These are the seats — names go here as we grow in public.",
    roles: [
      { role: "Product & Engineering", focus: "The platform, the apps, the backend that ties them together." },
      { role: "Schools & Onboarding", focus: "Getting institutions live and making sure the first week goes well." },
      { role: "Design", focus: "The craft — interactions, type, and the calm the product is known for." },
      { role: "Support", focus: "Real humans, fast answers, for admins, teachers and parents alike." },
    ],
  },
};

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
