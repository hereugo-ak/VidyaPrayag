# VidyaSetu — Full Frontend Build Plan

## Context

The user provided the complete VidyaSetu frontend rebuild blueprint (1908-line doc) plus two aesthetic-reference screenshots, and asked to build **every screen mentioned in the doc**. The doc is the source of truth for *what screens exist, what each one contains, what role sees what, and the color/type system*. Screenshots inform the warm/premium mobile UI feel (especially for the Parent portal). The user's color preference (white-with-a-hint-of-grey light base, pure deep black for dark, "whitewashed" accents) lines up exactly with the doc's three-color system: **Void #080808, Cloud #F5F5F3, Arctic #C8DEFF** — so we follow the doc tokens verbatim.

Scope: this is a non-functional React + Tailwind prototype (no real Supabase / FastAPI). All data is mocked. The product spans **4 portals** (Admin, Teacher, Parent, Discovery) + auth/onboarding + cross-portal screens. Goal is high-fidelity visual coverage of every screen, navigable via an in-app portal/role switcher, not a real auth system.

## Design tokens (single source of truth)

Write tokens to `src/styles/theme.css` (existing) and a `src/app/lib/tokens.ts` re-export:

- Colors: `--void:#080808`, `--cloud:#F5F5F3`, `--cloud-pure:#FFFFFF`, `--arctic:#C8DEFF`, `--arctic-deep:#A0C4F7`, `--success:#A8E6CF`, `--warning:#FFD4A3`, `--danger:#FFADA8`
- Fonts: Plus Jakarta Sans (UI) + DM Mono (data) — imported in `src/styles/fonts.css`
- Radius: chip 6, input 10, card 14, sheet 20, pill 999
- Spacing: 4/8/16/24/32/48/64
- Two surface modes: **dark** (Admin + Teacher) borders via `rgba(245,245,243,0.08–0.20)`; **light** (Parent + Discovery) white cards with soft shadows
- Icons: `lucide-react` (already installed — substitute for Phosphor; one icon family throughout)

## App shell & routing

Single-page React app, no real router needed. Build a **Portal Shell** in `src/app/App.tsx` that holds:
- A top-level state machine: `route = { portal, screen, params }`
- A persistent **dev portal switcher** (small floating pill, top-right) to jump between Splash / Login / Admin / Teacher / Parent / Discovery flows without auth — this is the navigation harness for the prototype
- Per-portal bottom nav components handle in-portal navigation
- Dark theme wrapper for Admin/Teacher; light wrapper for Parent/Discovery

## Component library — build first

Create under `src/app/components/v/` (atoms → molecules → organisms). Wrap shadcn primitives from `components/ui/` where they fit; only build custom when needed.

Atoms/molecules: `VText`, `VBadge`, `VTag`, `VAvatar`, `VCard`, `VInput`, `VButton`, `VSearchBar`, `VListItem`, `VStatusDot`, `VProgressBar`, `VProgressRing`, `VDataChip`, `VDivider`.

Organisms: `VStudentCard`, `VTeacherCard`, `VAttendanceCell`, `VSubjectPerformanceBar`, `VFeeStatusCard`, `VAnnouncementCard`, `VNotificationItem`, `VSchoolCard`, `VClassGrid`, `VComingSoon`, `VEmptyState`.

Navigation: `VBottomNav` (dark+light variants), `VTopTabs`, `VBackHeader`, `VChildSwitcher`, `VPortalSelector` (login), `VProgressStepper` (onboarding).

Charts via `recharts`: `VLineChart`, `VBarChart`, `VDonutChart`, plus a custom `VHeatmapCalendar`.

Logo: SVG component `VLogo` (bridge mark — two stacked arcs, lower heavier; wordmark in Plus Jakarta ExtraBold with Arctic "S").

## Mock data

`src/app/lib/mock.ts` — one school ("Saraswati Vidya Mandir"), 3 classes × 2 sections, ~30 students, 8 teachers, attendance/marks/syllabus/fee records, announcements, notifications, 6 discovery schools. All numbers consistent so screens cross-reference cleanly.

## Screen list (every screen the doc defines)

Each one lives in `src/app/screens/<portal>/<ScreenName>.tsx` and is reachable via the portal switcher.

**Auth / Onboarding**
1. Splash (animated bridge mark sequence per §5)
2. Login (portal selector tabs: Parent / Admin / Teacher) per §7.1
3. Teacher first-login password change
4. Parent OTP + child-link flow (3 steps)
5. School onboarding wizard — 6 steps (Identity, Academic Year, Classes/Sections, Subjects, Teachers, Students CSV) + Complete celebration screen

**Admin portal (dark)** — bottom nav: Home / People / Records / Comms / Settings
6. Admin Dashboard (Today-at-a-Glance cards, Class-wise attendance grid, Syllabus coverage, Teacher activity, Pending actions)
7. People → Students list + Student detail (Overview / Attendance / Marks / Fees / Notes tabs)
8. People → Teachers list + Teacher detail (Profile / Activity / Classes)
9. Records → Attendance / Marks / Syllabus / Fee / Documents (5 sub-screens via VTopTabs)
10. Communications → Announcements (list + compose sheet) / Messages (inbox + thread) / PTM / Notifications history
11. Settings (School Profile, Academic Year, Classes & Subjects, Teacher Mgmt, Fee Structure, Notifications, Data Export, Account)

**Teacher portal (dark)** — bottom nav: Home / Update / My Classes / Profile
12. Teacher Dashboard (Today's Tasks cards, timetable strip, activity feed)
13. Update → Attendance / Marks / Syllabus / Homework sub-screens
14. My Classes list + Class detail (roster)
15. Teacher Profile

**Parent portal (light)** — bottom nav: Home / Academics / Fees / Activity. `VChildSwitcher` persistent at top.
16. Parent Dashboard (Child hero, today's summary cards, attendance quick-look)
17. Academics → Overview / Attendance (calendar heatmap) / Marks (line chart) / Syllabus / Report
18. Fees (balance, breakdown, history, Pay-Now Coming-Soon)
19. Activity (filtered notification feed + inline messages)

**Discovery marketplace (light)**
20. Discovery Home (search, filter bar, school card list, compare tray)
21. School Profile (hero, About, Academics, Fees, SRI breakdown CS, Reviews, Location, Admission, Enquiry sheet)
22. School Comparison (up to 3 columns)

**Cross-portal**
23. Announcement detail screen
24. Academic Calendar
25. In-app Notification Tray (drawer)
26. Coming-Soon placeholder template (used by SRI, PEWS, AI report, Razorpay, Online PTM, etc.)
27. Empty-state set (6 variants from §17.3)

## Aesthetic guardrails (from doc Appendix B + user preference)

- Only the 3 base colors + 3 semantic tints. No purple/teal accents.
- Two fonts only — Plus Jakarta Sans + DM Mono. All numeric data in DM Mono.
- Lucide icons throughout (single family).
- Dark surfaces: no shadows, depth via 1px translucent Cloud borders.
- Light surfaces: pure white cards on `--cloud` page bg, soft `rgba(8,8,8,0.06)` shadow.
- No gradients-with-3-stops, no glowing blobs, no AI-stocky illustrations. Empty-state art = simple geometric SVG.
- Don't override Tailwind defaults for font-size/weight/line-height — set them in `theme.css` element styles per the project rule.

## Execution order

1. Tokens + fonts + `VLogo` + base components (atoms/molecules)
2. App shell + portal switcher + bottom navs
3. Splash + Login + onboarding wizard
4. Admin portal (Dashboard → People → Records → Comms → Settings)
5. Teacher portal
6. Parent portal
7. Discovery + cross-portal screens
8. Coming-Soon placeholders + empty states pass

## Verification

- Open the preview, use the floating portal switcher to walk every screen in the list above; each one renders without console errors, uses only declared tokens, and shows mock data consistent across portals (same school, same students).
- Spot-check: Parent's view of student X's attendance matches what Teacher marked for that class in mock data.
- Confirm dark portals (Admin/Teacher) and light portals (Parent/Discovery) render in their correct theme.
- Confirm role-boundary copy on relevant screens (Teacher sees only own classes, Parent sees only own child) — enforced in mock data slicing, not real auth.

## Out of scope

- Real Supabase / FastAPI wiring (everything is mocked)
- Real OTP, payments, WhatsApp, push notifications
- Animation polish beyond the splash sequence and basic Motion transitions on screen change
- i18n (English only for now; doc mentions Hindi as future)
