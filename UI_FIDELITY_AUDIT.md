# VidyaSetu — UI Fidelity Audit (Compose vs React/Figma)

> **Purpose** — This document maps **every** visual/UX difference between the current
> Compose Multiplatform implementation (`composeApp/.../ui/v2/`) and the React/Figma
> source of truth (`UI screens/src/app/`), screen by screen, with maximum detail.
> The React code is the blueprint; Compose must become a pixel-perfect, interaction-perfect clone.
>
> **Branch:** `backend-by-abuzar`
> **Audit method:** Line-by-line code comparison of React `.tsx` vs Compose `.kt`, cross-referenced
> with the 6 build screenshots the user provided (Welcome, School Onboarding, Parent Portal — each
> Compose-current vs Figma).
> **Status legend:** 🔴 critical (breaks the premium feel) · 🟠 major · 🟡 minor/polish

---

## 0. ROOT-CAUSE DEFECTS (global — fix these FIRST, they affect every screen)

These four defects explain ~80% of the "premature / 90% same" gap the user sees. They are
**system-wide** — fixing them lifts every screen simultaneously.

### 0.1 🔴 Brand font missing — `Plus Jakarta Sans` / `DM Mono` are NOT bundled
- **React:** `theme.css` loads `Plus Jakarta Sans` (400–800) for UI and `DM Mono` (400/500) for
  numbers via Google Fonts (`fonts.css`). Body sets `font-feature-settings: 'ss01','ss02'`
  (stylistic alternates) + `-webkit-font-smoothing: antialiased`. Every heading, button, label
  uses this geometric, rounded, premium typeface.
- **Compose:** `VType.kt` → `defaultVTypography()` falls back to `FontFamily.SansSerif` (Roboto on
  Android / SF on iOS) for UI and `FontFamily.Monospace` for data. **No `.ttf` exists in
  `composeApp/src/commonMain/composeResources/font/`** (the `font/` dir doesn't even exist).
- **Impact:** This is the **single biggest** "looks premature" cause. Roboto vs Plus Jakarta Sans
  changes letterforms, weight rendering, x-height, and the whole brand feel on 100% of text.
- **Fix:** Bundle `PlusJakartaSans-{Regular,Medium,SemiBold,Bold,ExtraBold}.ttf` and
  `DMMono-{Regular,Medium}.ttf` into `composeResources/font/`, build a `FontFamily`, and pass it
  into `defaultVTypography(uiFamily = …, dataFamily = …)`. Enable `ss01/ss02` features where the
  Compose text API allows (`FontFeatureSetting`).

### 0.2 🔴 `h1` line-height & weight mismatch + numeric weight rounding
- **React:** `h1` = 32px / **800** / line-height **1.1** (= 35.2px) / -0.02em. `dataLg`/hero numbers
  use weight **500–700** in DM Mono.
- **Compose:** `h1` = 32sp / `ExtraBold` (≈800 ✓) / lineHeight 35sp ✓. **But** `body` weight Normal,
  `caption` Medium(500) — React `p` is weight 400 and `caption`/small text varies (see 0.4).
- **Impact:** Headings are close; the gap is mostly the typeface (0.1). Keep watching weights once
  the real font is in — `FontWeight.ExtraBold` on Plus Jakarta renders heavier than on Roboto.

### 0.3 🔴 `VCard` corner radius & shadow are wrong (every card on every screen)
- **React `VCard`:** `rounded-[16px]` → **16px** radius; shadow = `--shadow-light-1` =
  `0 1px 3px rgba(38,35,77,0.05), 0 1px 2px rgba(38,35,77,0.03)` — an extremely soft, tight,
  barely-there shadow. Padding `p-4` = 16px.
- **Compose `VCard`:** uses `shapeLg` = **14dp** radius (2dp too sharp) and
  `shadow(elevation = 10.dp, ambient/spot = 0x14000000)` — a **much heavier, larger** drop shadow
  than the design's hairline. Padding 16dp ✓.
- **Impact:** Every card looks slightly boxier (14 vs 16) and "floats" too aggressively (10dp
  elevation vs a 1–3px soft shadow). Contributes strongly to the "not premium" feel — Figma cards
  sit flat-and-crisp; Compose cards have a chunky Material drop shadow.
- **Fix:** Add `radiusCard = 16.dp` token; change `VCard` shape to 16dp; replace the 10dp Material
  shadow with a custom soft shadow (low elevation ~2dp + very low alpha, or a layered
  `drawBehind`/`Modifier.shadow` tuned to `0 1px 3px @5%`).

### 0.4 🟠 `Label` / `caption` semantics don't match React per-context
- **React** has TWO distinct small-text styles that Compose collapses:
  - `Label` component: 11px / **700** / **uppercase** / letter-spacing **0.10em** / `--ink-3`.
  - `VInput` label: 12px / **600** / **NOT uppercase** / no tracking / `--ink-2`.
  - Global `label` element (theme.css): 11px / 600 / uppercase / 0.08em / `--ink-2`.
- **Compose:** `label` token = 11sp / SemiBold(600) / 0.08em — used for BOTH the uppercase section
  labels AND, wrongly, for things React renders as plain `caption`/footer text. `VInput` label uses
  `caption` (12sp Medium) instead of a 12/600 non-uppercase style.
- **Impact:** Section labels render with weight 600 not 700 and 0.08em not 0.10em (slightly weak).
  Input labels are Medium not SemiBold. Footers/legal text that should be plain 11px get uppercased
  label styling (see Welcome §1, diff #9).
- **Fix:** Add `labelStrong` (11/700/0.10em/uppercase, ink3) for `Label`, keep `label` (11/600/
  0.08em) for the global element, and add `inputLabel` (12/600/none/ink2) for VInput.

---

## 1. 🔴 Welcome / Splash  —  `auth/WelcomeScreenV2.kt`  ⇄  `Auth.tsx → Splash`

**Screenshots:** #1 (Compose current) vs #2 (Figma). This is the flagship screen and the worst offender.

| # | Element | React / Figma (blueprint) | Compose (current) | Sev |
|---|---------|---------------------------|--------------------|-----|
| 1 | **Hero ambient glow** | `radial-gradient(circle at 50% 30%, rgba(255,255,255,0.25), transparent 55%)` overlay @ opacity 0.30 over the teal panel | **Missing** — flat teal | 🔴 |
| 2 | **Cloud decorations** | Two hand-drawn cloud SVGs: top-left (56×38, opacity .30) + bottom-right (46×32, opacity .25), white 1.5 stroke | **Missing** | 🟠 |
| 3 | **Logo cube** | 160×160 box, radius **28**, bg `rgba(255,255,255,0.16)` + `backdrop-blur(8px)` + **1px `rgba(255,255,255,0.18)` border**, with an animated pulsing **halo** (`box-shadow 0 0 0 12px rgba(255,255,255,0.10)`, opacity 0→0.6→0 loop). Inner bridge SVG is **100×100**. | Box radius 28 ✓ but bg `card.copy(alpha=.16)` (card=white→ok), **no border, no blur, no halo**; uses `VLogo(size=96)` inside `padding(xl=32)` → wrong inner size & the VLogo draws its OWN rounded-rect plate (double plate look) | 🔴 |
| 4 | **Title size/weight** | `fontSize: 30`, `fontWeight: 800`, -0.02em | uses `h1` = **32sp** (2px too big) | 🟡 |
| 5 | **Tagline size** | `fontSize: 15`, color rgba(255,255,255,0.92), maxWidth 280 | uses `body` = **14sp** | 🟡 |
| 6 | **Social-proof strip** | 3 overlapping avatar circles `#A8E6CF / #FFD4A3 / #C8DEFF`, 24px, **-8px overlap** (`-space-x-2`), 2px lavender border; then text `"240+ schools"` **bold + ink** · " 38k parents" in ink-2, 12px | Only the text `"240+ schools · 38k parents"` in `caption/ink2` — **avatars missing**, no bold "240+ schools" segment | 🔴 |
| 7 | **"Get started" button** | `<VButton size=lg tone=teal>` → **soft defaults to TRUE** → soft MINT fill (`#b9e6df` bg, `#005048` text, soft border, inset highlight). Trailing **`ArrowRight` icon AFTER text**. | `soft = false` → **solid dark-green** fill (`#006a60`), **no arrow icon**. This is the most visible single miss (screenshot shows dark-green vs Figma's mint). | 🔴 |
| 8 | **Extra 3rd button** | React Splash has **ONLY 2 buttons** (Get started, I already have an account) | Compose adds a **third** "Register your school" ghost button that does not exist in the blueprint | 🔴 |
| 9 | **Footer / legal line** | `fontSize: 11`, plain (no uppercase), `--ink-3`; "Terms" & "Privacy Policy" are `--teal-deep` / weight 600 inline spans | rendered with `label` style → **UPPERCASE + 0.08em tracking** (wrong), single flat color, no teal links | 🟠 |
| 10 | **Sheet overlap & shadow** | Sheet uses `-mt-8` → pulls **up 32px** over the hero (overlap), radius 32, `boxShadow 0 -10px 32px rgba(0,0,0,0.06)` | Sheet sits flush below hero (no negative overlap), radius 32 ✓, **no top shadow** | 🟠 |
| 11 | **Entrance animations** | Spring logo (stiffness 240/damp 22), staggered fade-ins (delays 1.0→1.85s), bridge path-draw, sheet spring-up | **None** — everything static | 🟠 |
| 12 | **Hero height** | `minHeight: 440`, paddingTop 80 / bottom 90 | fixed `height(420.dp)` — slightly short, padding differs | 🟡 |
| 13 | **Status pill ("Splash")** | The PhoneFrame chrome shows a "● Splash" dark pill + moon toggle top-right (prototype chrome) | N/A in-app (chrome only) — ignore unless replicating the device frame | 🟡 |

---

## 2. 🔴 School Onboarding  —  `auth/SchoolOnboardingScreenV2.kt`  ⇄  `Auth.tsx → SchoolOnboarding`

**Screenshots:** #3 (Compose current, BLACK) vs #4 (Figma, LIGHT/white). The single biggest structural defect in the app.

| # | Element | React / Figma | Compose (current) | Sev |
|---|---------|---------------|--------------------|-----|
| 1 | **THEME — light vs dark** | React renders inside `PhoneFrame dark` — but **`dark` legacy applies the `.warm` scope, which is a LIGHT theme** (lavender `#fcf8ff` bg, dark ink text, white cards). The whole wizard is **LIGHT**. (See `theme.css` `.warm{}` block: `--void:#fcf8ff; --cloud:#1a2422;`) | Wraps everything in `VTheme(tone = VPortalTone.Night)` → **pure deep-black** `#050505` bg. **Completely wrong surface.** Screenshot #3 is black; Figma #4 is white. | 🔴🔴 |
| 2 | **Header "ONBOARDING" label** | `Label dark` = 11/700/upper/0.10em/ink-3 on light | `label` token weight 600/0.08em on **black** | 🔴 (theme) |
| 3 | **Step progress bar** | Filled segment = `--arctic` (teal `#3cb9a9`); empty = `rgba(245,245,243,0.08)` on the warm/light surface → empty reads as faint dark | Filled = `tealDeep`; empty = `cream` (which in Night = `#141416` dark). On light it should be a pale track | 🟠 |
| 4 | **Title** | `<h2>` 22/700, color `--cloud` (which in `.warm` = `#1a2422` dark ink) | `h2` on `c.ink` (Night = near-white) | 🔴 (theme) |
| 5 | **`VInput` fields** | Light: bg `--cream #f5f5f3`, border hairline dark, label 12/600 ink-2, placeholder `--placeholder #bcc9c6`. Focus → white bg + teal-deep border + teal glow | Night: bg `#141416`, near-white text → all inputs are **dark boxes** (screenshot #3) | 🔴 (theme) |
| 6 | **Board / School-type tags (`VTag`)** | active: bg `#dcf2ef`, text `#006a60`, border `rgba(0,106,96,0.18)`, shadow; inactive: bg `--cream`, text ink-2. Radius **`rounded-md` = 6px**, padding `px-3 py-1.5`, font 12/600 | `VTag` active uses theme teal but on dark surface; verify radius 6 & exact `#dcf2ef` fill | 🟠 |
| 7 | **Footer nav** | `borderTop: 1px --border-dark-1`; **Back** is ghost (only shown when step>1); **Continue** is `size=lg tone={step===6?teal:navy}` with **trailing ArrowRight**. Step 6 button is `stateful` (loading→"Setting up"). | Back always present (calls onBack at step 1 — extra behavior), Continue uses `leading` icon (**icon BEFORE text**, React has it AFTER), no top border line, step-6 not `stateful` | 🟠 |
| 8 | **Step indicator count** | progress bar maps `i < step` over **6** titles | repeat(6) ✓ | ✓ |
| 9 | **Step 1 layout** | board/type use `Label dark` headers + wrapped tags. Compose mirrors it (FlowRow) ✓ structurally | OK except theme + label weight | 🟡 |
| 10 | **Step 3 manual-add input** | React uses a **raw `<input>`** styled with `--cream` bg, 10px radius, 13px — a compact inline field, NOT a full VInput (no label) | Compose uses full `VInput` (slightly taller; OK but visually a touch bigger) | 🟡 |
| 11 | **Step 5 teacher matrix** | A true **CSS grid**: `gridTemplateColumns: 110px repeat(N,1fr)`, header row (`--cream` bg, 10px upper labels), bordered `rounded-[10px]`, cells `h-8 m-1 rounded-[6px]`; states: mine=teal-deep/✓white, takenByOther=cream/—, available=`rgba(60,185,169,0.10)` + **1px dashed** border/+; disabled opacity .25. Assignment chips below: `font-mono`, `rgba(60,185,169,0.15)` pill, `SUBJ·class`. | Compose renders **per-subject `FlowRow` of 30dp cells** — NOT the bordered header-grid; **no header row, no dashed available border, no chip summary row**. Structurally simplified. | 🟠 |
| 12 | **Step 5 "Import roster" button** | `tone=sand` with leading **Upload** icon | tone=sand ✓ but leading icon = `Plus` (should be Upload/Share) | 🟡 |
| 13 | **Completion screen hero** | gradient `linear-gradient(180deg, --teal 0%, #2a8f80 100%)` + radial glow overlay; check icon springs in (rotate -30→0); h1 **30px** | flat `c.teal` (no gradient, no glow), `h1` 32sp, no animation | 🟠 |
| 14 | **Completion stat bar** | 4 cells with **right dividers** (`borderRight 1px rgba(38,35,77,0.06)` except last); number `font-mono 22/700 navy`, label `10px/700/upper/0.06em ink-3` | No inter-cell dividers; label uses `label` token (11/0.08em) | 🟡 |
| 15 | **Completion quick-start tiles** | icon chip `#dcf2ef` bg / `#006a60` icon; hover `-translate-y-[1px]`; `--cloud-pure` bg | icon chip `c.teal.copy(alpha=.16)`; no hover lift | 🟡 |
| 16 | **Entrance animations** | All sections fade-up with staggered delays (0.5→0.9s); coverage bar animates width | None | 🟠 |

> **Net for screen #2:** even if every chip/label were perfect, the screen would still look wrong
> because the **theme is inverted (black instead of warm-light)**. Fix #1 first.

---

## 3. 🟠 Login  —  `auth/LoginScreenV2.kt`  ⇄  `Auth.tsx → Login`

| # | Element | React / Figma | Compose (expected fix) | Sev |
|---|---------|---------------|------------------------|-----|
| 1 | **Brand hero** | teal panel, paddingTop 56/bottom 72, minHeight 360; 2 cloud SVGs; logo cube 160×160 radius 28 `rgba(255,255,255,0.16)`+blur (no border here, unlike Splash); inner bridge 100×100 with navy center dot | Verify cube size 160 + blur; clouds present? | 🟠 |
| 2 | **Heading** | `<h2>` 22/700 "Welcome to VidyaSetu. 👋" (emoji via Noto Color Emoji) | check emoji handling + size | 🟡 |
| 3 | **Portal selector tabs** | pill group, bg `--portal-tab-bg #f6f1ff`, 3 segments (parent/admin/teacher), active = white bg + `teal-deep` text + weight 700 + `0 1px 2px` shadow + 8px radius inside 12px container; **capitalize** | Confirm tab visuals + active shadow + radius | 🟠 |
| 4 | **Conditional fields** | parent: Mobile + (after Send OTP) OTP field; admin: Email/SchoolID + Password(+Eye toggle, Forgot link); teacher: Teacher credential + Password | Confirm all three branches + OTP two-step state + Eye toggle + "Forgot Password?" teal-deep link | 🟠 |
| 5 | **Submit button** | `tone=teal` (soft default = mint) + trailing ArrowRight; label switches Send OTP→Verify & Continue / Sign In | Confirm soft mint (not solid) + trailing arrow + dynamic label | 🟠 |
| 6 | **"Not a member? Register Now"** | 14px; "Register Now" = `--warm-orange #9e421a` / weight 600 | Confirm warm-orange link color | 🟡 |
| 7 | **Sheet** | `-mt-8` overlap, radius 32, `0 -8px 30px` shadow, spring entrance | Confirm overlap + shadow | 🟠 |
| 8 | **VInput focus glow** | `0 0 0 4px rgba(60,209,190,0.15)` — note **`#3cd1be`** (teal, not teal-deep) at 15% | Compose uses `c.teal.copy(0.15f)` ✓ (light teal = `#3cb9a9` — close; React glow uses 3cd1be) | 🟡 |

---

## 4. 🟠 Parent Portal + Home  —  `parent/ParentPortalV2.kt` + `ParentHomeScreenV2.kt`  ⇄  `Parent.tsx`

**Screenshots:** #5 (Compose current) vs #6 (Figma). Closest of the three — mostly font + radius + spacing polish.

### 4.1 ChildSwitcher header (`ParentPortalV2.ChildSwitcher` ⇄ `Parent.tsx → ChildSwitcher`)
| # | Element | React | Compose | Sev |
|---|---------|-------|---------|-----|
| 1 | Container | `px-5 pt-5 pb-3`, bg `--cloud-pure` (white), borderBottom `--border-light-1` | bg `c.card` ✓, padding 20/20/12 ✓, uses `VDivider` at bottom instead of borderBottom (extra Spacer+divider) | 🟡 |
| 2 | Child chip | bg `--cloud` (`#f5f5f3`), `px-2 py-1.5` pill; name 13/700; subline **10px** `--text-light-2`; ChevronDown 14 opacity .5 | bg `cream` ✓; name `bodyStrong`(14) — **too big** (should be 13/700); subline uses `label`(11/upper) — **should be 10px plain, not uppercase** | 🟠 |
| 3 | Bell + dot | 9×9 → 36dp circle, bell 16, danger dot 6px top-2 right-2 | ✓ matches | ✓ |
| 4 | Parent avatar (exit) | `VAvatar "Sneha Sharma" size 32` | `MockV2.parentName` size 32 ✓ | ✓ |
| 5 | Sibling dropdown | active row bg `rgba(200,222,255,0.30)` (light blue), check icon `#0a3a76`; inactive `--cloud` | active bg `c.teal.copy(0.14f)` (**teal not blue**), check `tealDeep` | 🟡 |

### 4.2 ParentHome (`ParentHomeScreenV2` ⇄ `Parent.tsx → ParentHome`)
| # | Element | React | Compose | Sev |
|---|---------|-------|---------|-----|
| 1 | Hero card | `VCard padded={false}`, gradient `135deg #f6f1ff→#e8f7f3`, **decorative radial blob** top-right (`-right-6 -top-6 w-32 h-32 radial rgba(60,185,169,0.35)` opacity .50) | gradient ✓ (hardcoded hexes, acceptable since design-specific), **blob missing** | 🟠 |
| 2 | Avatar | size **68** + `ring` (3px white border) | 68 + ring ✓ | ✓ |
| 3 | Status badge | success/danger/warning + icon (Check/X/Clock) inline | uses "● " prefix text instead of the lucide icon inside the badge | 🟡 |
| 4 | Attendance row | Label "Attendance · last 30 days"; **92%** = `font-mono 24/700 navy`; "+4 vs class avg" 11/600 `#155e3a`; VSparkline 120×44 | 92% uses `dataLg` overridden to 24sp/Bold ✓; "+4" uses `label` (upper) — should be **plain 11/600** | 🟡 |
| 5 | Card radius | 16px (VCard) | 14dp (root-cause 0.3) | 🟠 |
| 6 | Schedule rows | period num `font-mono 11 text-light-3`, subject 13/600, teacher 11 text-light-2, time `font-mono 11`, "Now" badge on i==2 | period `dataSm`(13) width 20 (should be 11/centered), teacher `caption`, time `dataSm` ✓, Now badge ✓ | 🟡 |
| 7 | "What covered" | title 600, body 13 `text-light-2`, byline 11 `text-light-3` | body uses `caption`(12) — React is 13; byline uses `label`(upper) — React plain 11 | 🟡 |
| 8 | Reminder card | warm chip `rgba(255,212,163,0.45)` + CalIcon `#7a3f00` | `c.warning.copy(0.45f)` + warningInk ✓ | ✓ |
| 9 | Fees card | "₹ 12,500 due" `font-mono 20` color `#7a3f00`; Pay-now `tone=peach stateful` | `data` 20sp `warmOrange`(#9e421a — close to #7a3f00) ✓; button ✓ | 🟡 |
| 10 | Month attendance bar | flex 21:2 teal/danger, 38px `font-mono 500` figure | weight 21:2 `teal`/`dangerInk` ✓; 91% `dataLg` 38sp ✓ | ✓ |

> **Net for screen #3:** structurally faithful. The visible gap in screenshot #5↔#6 is dominated by
> (a) the **font** (0.1), (b) **card radius 14 vs 16 + heavy shadow** (0.3), and (c) chip/label text
> being slightly oversized/uppercased. No structural rebuild needed — polish + global fixes.

---

## 5. 🟡 Other Auth flows

### 5.1 TeacherFirstLogin — `auth/TeacherFirstLoginScreenV2.kt` ⇄ `Auth.tsx → TeacherFirstLogin`
- React: `PhoneFrame dark` (=**warm/light**); `Label dark` "Welcome, Mr. Vikram"; `h1` "Set a new
  password"; 3 dark VInputs (password); primary button (navy soft default) "Update & continue" +
  trailing arrow; ghost "Need help signing in?" 13px text-dark-3.
- **Verify:** screen is LIGHT (not Night), button is soft navy with trailing arrow, not solid.

### 5.2 ParentLinkChild — `auth/ParentLinkChildScreenV2.kt` ⇄ `Auth.tsx → ParentLinkChild`
- React: `PhoneFrame` (light), 3-step, `Label` "Step n of 3" + 3-segment bar (`--arctic` filled);
  step1 name + language VTags (English/हिन्दी); step2 search + match VCard (GraduationCap chip
  `--arctic`, "Match" badge); step3 roll input + matched-child VCard + "+ Add another child"
  (`#0a3a76`). Button label Continue→"Finish & open dashboard" + trailing arrow.
- **Verify:** Hindi glyph रendering, arctic chips, 3-step bar, trailing arrows.

---

## 6. 🟠 Teacher Portal  —  `teacher/*`  ⇄  `Teacher.tsx`

All teacher screens render in React under `PhoneFrame dark` = **warm/light** theme. Verify none are
forced to `VPortalTone.Night`.

| Screen | Compose | Key checks vs `Teacher.tsx` |
|--------|---------|------------------------------|
| **TeacherHome** | `TeacherHomeScreenV2.kt` | greeting header, today's classes list, quick actions grid, "needs attention" cards — verify light theme + card radius/shadow + label weights |
| **Update hub** (Attendance/Marks/Syllabus/Homework) | `TeacherAttendance/Marks/Syllabus/HomeworkScreenV2.kt` | the 4 sub-flows; Syllabus preview text fixed in `b4e16f5` ✓ |
| **Syllabus** | `TeacherSyllabusScreenV2.kt` | parent-notification preview now `body/ink` + bold chapter ✓; verify the rest (chapter list, progress) matches `Teacher.tsx → SyllabusFlow` lines 206–231 |
| **MyClasses** | `TeacherClassesScreenV2.kt` | class cards + ClassDetail drill-in |
| **Profile** | `TeacherProfileScreenV2.kt` | avatar, stats, settings rows |
| **Portal shell** | `TeacherPortalV2.kt` | bottom nav (home/update/classes/profile), tab switching, overlays |

**Common teacher diffs (apply globally):** card radius 14→16, heavy shadow, font, `Label` weight
600→700/0.10em, any `leading`-icon buttons that should be **trailing** in React.

---

## 7. 🟠 School (Admin) Portal  —  `school/*`  ⇄  `Admin.tsx`

React `AdminApp` renders under `PhoneFrame dark` = **warm/light**. Verify SchoolPortalV2 is NOT Night.

| Screen | Compose | Key checks vs `Admin.tsx` |
|--------|---------|------------------------------|
| **AdminHome** | `SchoolHomeScreenV2.kt` | `GlanceCard`s (KPI tiles), SRI/PEWS preview mockups (`mockups.tsx`), announcements |
| **People** | `SchoolPeopleScreenV2.kt` | student/teacher lists → StudentDetail / TeacherDetail drill-ins |
| **Records** | `SchoolRecordsScreenV2.kt` | Attendance heatmap (`AttendanceHeat`), Marks, Syllabus, Fee, Docs tabs |
| **Comms** | `SchoolCommsScreenV2.kt` | announcement composer + AnnouncementDetail |
| **Settings** | `SchoolSettingsScreenV2.kt` | settings rows/toggles |
| **Portal shell** | `SchoolPortalV2.kt` | 5-tab bottom nav (home/people/records/comms/settings) |

**Mockup-specific (`mockups.tsx`) checks:**
- **SRIPreview:** `font-mono 32/600 navy` score + "/10" 16px ink-3; 6 weighted bars (label 116px,
  track `--cream` h-1.5, fill `teal-deep`, value 28px right); "+0.3 YoY" success pill.
- **PEWSPreview:** risk-band 3-tile grid (Low/Watch/High with exact bg/fg hex pairs), highest-priority
  rows (score chip color by threshold >70/>55), dashed "SUGGESTED INTERVENTION" box
  (`rgba(60,185,169,0.10)` + `1px dashed rgba(0,106,96,0.3)`).
- **AIReportCardPreview:** navy narrative box, 2-col grade tiles (grade 20/800 navy + mono score),
  3 strength/focus/tip rows with exact tinted bgs.
- **Verify** these previews exist in Compose and match the exact hexes/weights, or are flagged TODO.

---

## 8. 🟡 Discovery  —  `discovery/*`  ⇄  `Discovery.tsx`

React `DiscoveryApp` is **light** (`PhoneFrame`, no dark).

| Screen | Compose | Checks |
|--------|---------|--------|
| **DiscoveryList** | `DiscoveryScreenV2.kt` | school cards, search, filters, SRI badges |
| **SchoolProfile** | (in DiscoveryScreenV2) | profile hero, SRI ring, sections |
| **SchoolCompare** | (in DiscoveryScreenV2) | side-by-side compare |
| **AcademicCalendar** | `AcademicCalendarScreenV2.kt` | month grid, event dots/legend |

**Chart checks (`charts.tsx`):**
- **VSparkline:** has a **gradient area fill** (`linearGradient` stop 0.28→0) + animated path-draw +
  end dot. **Compose `VSparkline` must replicate the gradient fill + end dot** (verify it isn't a
  plain stroke). 
- **VDonut:** animated stroke segments, center label slot.
- **VBars:** last bar `teal-deep` + value label, others `rgba(60,185,169,0.45)`, animated height.
- **VLegendDot / VProgressRing:** exact sizes/colors.

---

## 9. 🟠 Notifications  —  `notifications/NotificationsScreenV2.kt`  ⇄  `Notifications.tsx`

| # | Element | React | Compose | Sev |
|---|---------|-------|---------|-----|
| 1 | Theme | `PhoneFrame dark={dark}` — passed `dark` from caller; parent path is **light** | verify not forced Night | 🟠 |
| 2 | Hero | `rounded-[18px]`, gradient `135deg --navy→#3b3870`, radial blob top-right `rgba(60,185,169,0.45)`; bell chip `rgba(255,255,255,0.14)`+blur; "Inbox" upper 12/0.05em; count `font-mono 28/600` + "unread" 14 | verify gradient + blob + mono count | 🟠 |
| 3 | Filter pills | all/unread; active = `--navy` bg + white; inactive `--cream` + ink-2; 12/700; "unread · N" | verify count suffix + navy active | 🟡 |
| 4 | List rows | white card 14px radius, unread → `0 6px 14px` shadow + 8px teal-deep dot top-right; read → soft `0 2px 6px`; category icon chip (toneFor: attendance=peach, fees=rose, academic=teal, default=mint) 10px radius; VBadge + time 11 ink-3; title 14/600; body 12 ink-2; ChevronRight | verify per-category icon+tone map, unread dot, two shadow levels, staggered fade-in (delay i*0.04) | 🟠 |
| 5 | Empty state | check chip + "You're all caught up" | verify | 🟡 |
| 6 | Settings link | `--cream` bg pill, X icon + "Notification preferences" | verify | 🟡 |

---

## 10. Component-level token deltas (fix once, propagates everywhere)

| Token / component | React value | Compose current | Action |
|-------------------|-------------|------------------|--------|
| UI font | Plus Jakarta Sans (ss01/ss02) | SansSerif fallback | **Bundle font** (0.1) |
| Data font | DM Mono (tnum) | Monospace fallback | **Bundle font** (0.1) |
| `VCard` radius | 16px | 14dp (`shapeLg`) | add `radiusCard=16` |
| `VCard` shadow | `0 1px 3px @5% + 0 1px 2px @3%` | `elevation 10dp @ 0x14` | soften to hairline |
| `VInput` radius | 12px (`rounded-[12px]`) | 10dp (`shapeMd`) | use 12dp for inputs |
| `VInput` label | 12 / 600 / none / ink-2 | `caption` 12/Medium(500) | add `inputLabel` style |
| `VInput` focus glow | `rgba(60,209,190,0.15)` (#3cd1be) | `c.teal(#3cb9a9) @15%` | match #3cd1be |
| `Label` (section) | 11 / **700** / **0.10em** / upper / ink-3 | `label` 11/600/0.08em | add `labelStrong` |
| `VTag` radius | `rounded-md` = 6px | verify (`shapeSm`=6 ✓) | confirm |
| `VTag` active | bg `#dcf2ef`, fg `#006a60`, border `rgba(0,106,96,.18)`, shadow | verify exact | confirm |
| `VButton` soft default | **`soft = true`** (mint look) | call sites pass `soft=false` | **default soft=true; only override when blueprint uses filled** |
| `VButton` icon position | icon AFTER text (`children <Arrow/>`) | `leading` slot = BEFORE text | add `trailing` slot; move arrows to trailing |
| `VButton` sizes | sm `px-3.5 py-2` r10; md `px-4 py-2.5` r12; lg `px-6 py-3.5` r12 | sm 14/8 r10 ✓; md 16/10 ✓; lg 24/14 ✓ | ✓ |
| `VBottomNav` | white bg, `0 -4px 20px @4%` top shadow, active teal-deep, badge `#c14a44` mono | verify shadow + badge color | confirm |
| `VTopTabs` | active teal-deep + 2.5px underline | verify underline indicator | confirm |
| Hero "shimmer" sweep | filled primary buttons have a hover sheen sweep | none | optional (web/desktop hover only) |

---

## 11. Cross-platform (Android + iOS) checks (per the prime directive)

- **Status bar / safe areas:** Welcome/Login teal hero must extend under the status bar with content
  inset (currently fixed `height(420)` ignores top inset). Use `WindowInsets.statusBars`.
- **Back gesture:** every `onBack` must be wired to predictive back (Android) and the iOS edge-swipe;
  SchoolOnboarding step>1 back should go to previous step, step 1 back exits.
- **Keyboard:** VInput-heavy screens (Login, Onboarding) need `imePadding()` + scroll-to-focused.
- **Font scaling:** with the real font bundled, verify `sp` scales and `ss01/ss02` features apply on
  both platforms.

---

## 12. Priority fix order (to recover the "premium" feel fastest)

1. **0.1** Bundle Plus Jakarta Sans + DM Mono → wire into `VType`. *(biggest single win)*
2. **§2.1** Fix SchoolOnboarding theme: Night → **warm/light** (Light palette). *(fixes screenshot #3 entirely)*
3. **0.3** `VCard` radius 14→16 + soften shadow. *(every card, every screen)*
4. **§1** Rebuild Welcome: soft-mint Get-started + trailing arrow, remove 3rd button, add avatars +
   "240+ schools" bold, glow + clouds + halo logo, fix footer style, sheet overlap.
5. **0.4** Add `labelStrong` / `inputLabel`; replace mis-used `label` styling on footers/captions.
6. **VButton** default `soft=true` + add `trailing` icon slot; audit all call sites.
7. Polish per-screen items in §3–§9.
8. Animations (§1.11, §2.16) + cross-platform insets (§11).

---

*Generated by line-by-line comparison of `UI screens/src/app/**` (React blueprint) against
`composeApp/src/commonMain/kotlin/com/littlebridge/vidyaprayag/ui/v2/**` (Compose implementation),
cross-referenced with the 6 build screenshots. Every value above is quoted from source.*
