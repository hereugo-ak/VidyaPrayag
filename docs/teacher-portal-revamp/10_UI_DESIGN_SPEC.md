# 10 — UI DESIGN SPEC (teacher visual system)

> **Scope:** The teacher portal's complete visual system, built as a faithful **extension of the parents portal** (not a new look): exact color tokens, card/elevation/shadow, typography hierarchy, icons, interactions, long-list handling, greeting bar, biometric prompt design, and every loading/empty/error state — with extreme accessibility for a 19–60yo range.
> **Source (verbatim tokens):** `ui/v2/theme/VColors.kt`, `VType.kt`, `VDimens.kt`, `VElevation.kt`, `ui/v2/components/VCard.kt`, `UI screens/src/styles/theme.css`, `website/tailwind.config.ts`. Reference: `ParentPortalV2.kt`, `ParentDock.kt`, `ParentScheduleCard.kt`.
> **Lens:** Aanya (tech-native, will notice cheap UI) and **Mr. Rao (58, low confidence, needs large legible targets)** — the 58yo is the binding accessibility constraint.
> **Law:** Nothing hardcodes a hex. Every color reads from `VColors` via `LocalVColors`. The teacher portal must feel like the *same product* as the parents portal — premium, calm, ParentDock-class.
> **Branch:** `backend-by-abuzar_v1.0.3`

---

## 0. Executive Summary

The teacher portal already *uses* the design system (`VColors/VType/VDimens/VElevation`) but applies it to a hollow IA (Docs 03/04). The visual system itself is sound and **must be preserved and extended**, not redesigned. The job of this doc is to pin down — exactly — how the new IA (Doc 04) and the new surfaces (Docs 05–09) render, so the rebuild has zero visual ambiguity.

Core principles:
1. **Same skin as parents.** Teacher portal = parents portal's visual language with a teacher IA. Same tokens, same `VCard`, same dock physics (ParentDock), same header rhythm, same three tones (Light/Warm/Night).
2. **The 58yo sets the floor.** Minimum tap target 48dp, minimum body text 14sp (prefer 16sp for primary), high-contrast ink, no reliance on color alone, generous spacing. If it works for Mr. Rao it works for Aanya.
3. **Numbers are data, not prose.** Every marks/attendance/rate number renders in **DM Mono, tabular figures**; every label/heading in **Plus Jakarta Sans**.
4. **Three states, always.** Every data surface ships Loading, Empty, and Error states via `VStateHost` — no blank screens, no fake content (kills the fabricated-tasks class of defect).

---

## 1. Color Tokens (verbatim from `VColors.kt`)

All values below are the authoritative tokens. **Never hardcode** — read `LocalVColors.current`.

### 1.1 Brand
| Token | Light hex | Role |
|-------|-----------|------|
| `teal` | `#3CB9A9` | brand primary |
| `tealDeep` | `#006A60` | brand deep / ink-on-teal |
| `navy` | `#26234D` | headings, shadow tint base |
| `navyDeep` | `#1A1838` | deepest navy |
| `lavender` | `#FCF8FF` | **app background (light)** |
| `lavenderLight` | `#EAE6FA` | secondary lavender |
| `cream` | `#F5F5F3` | secondary / input surface |
| `warmOrange` | `#9E421A` | warm accent |

### 1.2 Accent (violet — the active-state family)
| Token | Light hex | Role |
|-------|-----------|------|
| `accent` | `#6C5CE0` | **primary active** (active tab lozenge, rings, primary CTA) |
| `accentSoft` | `#8B7EE8` | gradients/hovers; **leave** status fill |
| `accentDeep` | `#544AB8` | ink on accent tints, eyebrow text |
| `accentTint` | `#F4F3FA` | cool off-white canvas behind floating cards |

The **sliding violet lozenge** in ParentDock uses `accent` — the teacher dock reuses it identically.

### 1.3 Ink (text)
| Token | Light hex | Role |
|-------|-----------|------|
| `ink` | `#1A2422` | primary text |
| `ink2` | `#3D4947` | secondary text |
| `ink3` | `#6D7A77` | tertiary/captions |
| `placeholder` | `#BCC9C6` | placeholder |

### 1.4 Surfaces / borders
| Token | Light | Role |
|-------|-------|------|
| `background` | `#FCF8FF` (lavender) | page bg |
| `card` | `#FFFFFF` | elevated card |
| `border1` | `rgba(8,8,8,.06)` | subtle border |
| `border2` | `rgba(8,8,8,.10)` | stronger border |
| `hairline` | `rgba(38,35,77,.06)` navy-tinted | every 1px divider |
| `shadowTint` | `#26234D` navy | tinted elevation base |

### 1.5 Semantic (data states ONLY — never branding)
| Token | Fill | Ink | Maps to |
|-------|------|-----|---------|
| `success` / `successInk` | `#A8E6CF` | `#1F7A4D` | **present**, on-track attendance, pass |
| `warning` / `warningInk` | `#FFD4A3` | `#B3651A` | **late**, recent-absence flag, dropping |
| `danger` / `dangerInk` | `#FFADA8` | `#B3261E` | **absent**, low attendance, fail |
| `accentSoft` | `#8B7EE8` | `accentDeep` | **leave** (Doc 06) |

> **Accessibility rule:** status is *never* color-only. Present/Absent/Late/Leave pills also carry a letter (P/A/L/Lv) and, where space allows, an icon — so a color-blind or low-vision teacher (and Mr. Rao) reads the state without relying on hue.

### 1.6 Night palette
All tokens have a `.theme-night` variant (deep black `#050505` bg, `#0E0E10` card, near-white ink, accents kept punchy `#8B7EE8`). Teacher portal supports **Light / Warm / Night** via `VPortalTone` exactly like parents. Profile → Settings exposes the toggle (Doc 04 §5.14).

---

## 2. Typography (from `VType.kt`)

Two families: **Plus Jakarta Sans** (UI text) and **DM Mono** (data/numbers, tabular figures).

| Style | Size / weight / tracking | Use |
|-------|--------------------------|-----|
| `h1` | 32 / 800 / -0.02em | greeting ("Good morning, Aanya") |
| `h2` | 22 / 700 | screen titles, section headers |
| `h3` | 17 / 700 | card titles, class names |
| `h4` | 14 / 600 | sub-labels, list group headers |
| `body` | 14 / 400 | descriptions, content |
| `caption` | 12 / 500 | meta (time, room, "marked at") |
| `label` | 11 / 600 | chips, eyebrow text (uppercase tracked) |
| `data` | DM Mono, tabular | **all marks, rates, counts, times** |

**58yo adjustments (must-haves):**
- Primary actionable text (CTA labels, roster names) rendered at **≥16sp** even though `body` token is 14 — define a `bodyLarge` (16/500) for primary lists.
- Honor system font-scale; layouts reflow, never clip, up to 200% scale.
- Marks/attendance numbers in `data` (DM Mono tabular) so columns align and digits don't dance.

---

## 3. Spacing, Radius, Dimensions (from `VDimens.kt`)

- **Spacing:** base-4 scale (4/8/12/16/20/24/32). Default screen padding 16; card inner padding 16; list row vertical 12.
- **Radii:** card `16`, input `12`, small `6`, **pill `999`**.
- **Tap targets:** **minimum 48×48dp** for every interactive element (status pills, toggles, dock items, list rows). This is the hard floor for Mr. Rao.
- **Min row height:** roster/list rows ≥ 56dp; period cards ≥ 72dp.
- **Hit-slop:** small icons get expanded hit areas to reach 48dp.

---

## 4. Elevation & Cards (from `VElevation.kt` + `VCard.kt`)

- **3-tier navy-tinted shadow system** (`shadowTint = #26234D`):
  - **Card** (resting surfaces: list cards, summary cards) — soft, low.
  - **Raised** (active period card, primary CTA, FAB-like) — medium.
  - **Modal** (sheets, dialogs, biometric prompt) — high.
- **`VCard`** is the universal elevated surface — every card in the teacher portal uses it (consistency with parents). White (`card`) on lavender (`background`); 16 radius; navy-tinted shadow; optional accent border for class-teacher / current-period emphasis.
- **Hairlines** (`hairline`, navy@6%) for every 1px divider — never pure black lines.

---

## 5. Navigation Chrome

### 5.1 Teacher Dock (extends `ParentDock.kt`)
- Floating **glass dock**, same physics: rounded pill container, blur/translucency, **sliding violet (`accent`) lozenge** tracking the active tab, haptics on switch, badge support.
- 5 destinations: **Today · Classes · Gradebook · Planner · Profile** (Doc 04 §4).
- Each item: icon + short label; active = lozenge + `accent` ink; inactive = `ink3`.
- **Badge on Today** = real obligation count (Doc 04 §5.5) — **not** hardcoded `1` (fixes F-SHELL-4). Badge hidden at 0.
- Items ≥48dp; dock respects bottom safe-area inset.

### 5.2 Header (extends `ParentHeader`, minus child switcher)
- Greeting/title left; notification bell right (with count badge).
- Inside Classes/Gradebook/Planner: a tappable **class-context chip** showing the current scope ("7B · Maths"); tapping changes scope (Doc 04 §6/§7) — replaces the old shared picker.
- Sticky on scroll for long screens (roster, marks grid).

---

## 6. Signature Surfaces — visual spec

### 6.1 Greeting bar (Today)
- `h1` greeting + `caption` date (DM Mono date) + school name; right: bell.
- Below: **check-in card** — amber (`warning`) "Tap to check in" → green (`success`) "Checked in 08:42 ✓" pill. State change animates (pill fill + check icon), haptic on success.

### 6.2 Now/Next period card (Today, `Raised` elevation, `accent` left border when "now")
- Class+section (`h3`) · subject (`h4`) · room+time (`caption` DM Mono) · live "now/ends in 12 min" chip.
- **Attendance badge:** ✓ (success) marked / ! (warning) unmarked — letter+icon, not color-only.
- CTA row: [Mark attendance] (primary `accent` button) · [Syllabus] · [Homework] (secondary outline). Buttons ≥48dp.
- Swipeable 3 faces (Now/Timeline/Weekly) like `ParentScheduleCard`, 3-dot indicator, haptics (Doc 05 §5).

### 6.3 Status pills (attendance) — the most-tapped control
- Four pills per student: **P / A / L / Lv**, pill radius 999, ≥48dp tall.
- Selected: filled with semantic token (success/danger/warning/accentSoft) + ink; unselected: outline `border2` + `ink3`.
- Letter always visible (color-independent). Tap = haptic + fill animation.

### 6.4 Marks grid (Gradebook)
- Dense rows: name (`bodyLarge`) + roll (`caption`) left; numeric field right, **DM Mono tabular, right-aligned**.
- Sticky header: name · `/max` · pass line · "entered k/n" (DM Mono).
- Validation: out-of-range value rejected with field shake + inline `dangerInk` "max 25"; below-pass values render in `dangerInk` (passive). AB toggle distinct from 0.

### 6.5 Roster row (Classes)
- Avatar/photo (circle, placeholder = initials on `accentTint`) · name (`bodyLarge`) · roll (`caption`) · attendance rate (DM Mono, colored by threshold) · latest mark (DM Mono) · flag dots (semantic) · chevron.
- Row ≥56dp; entire row tappable → student profile.

### 6.6 Biometric prompt (Doc 06 §2)
- Presented as a **Modal-elevation** sheet over a scrim.
- Content: lock/fingerprint icon (large), "Confirm it's you," primary biometric trigger, secondary "Use PIN," tertiary "Confirm manually" (the always-available fallback).
- Never traps the user: the manual-confirm path is always visible. Large buttons (≥48dp), high contrast. On success: scrim dismiss + check-in pill flips with haptic.

---

## 7. Long-List Handling (40×6 reality — Mr. Rao)

- **Virtualized lists** (`LazyColumn`) for rosters/marks/submissions; never render 240 rows eagerly.
- **Sticky section headers** (class groups, status columns).
- **Sticky summary header** on marks/attendance (running counts) so the teacher never loses context while scrolling 40 students.
- **Jump/search**: jump-to-roll, search by name.
- **Bulk affordances** surfaced at top (Mark all present / Fill remaining) within thumb reach.
- **Scroll performance:** stable keys, no per-row N+1 (data pre-joined server-side per Docs 06/07/09).

---

## 8. The Three States (every data surface) — via `VStateHost`

| State | Visual | Rule |
|-------|--------|------|
| **Loading** | skeleton cards/rows in `cream`/`accentTint` shimmer (match parents); never a bare spinner on full screen where a skeleton fits. | Show within 100ms; skeleton mirrors final layout. |
| **Empty** | illustration/icon + **honest message** + a single primary action. Distinguish "nothing yet" from "not set up" (e.g. timetable unseeded vs holiday — Doc 05). | **Never fabricate content** (kills B-HOME-4 class of defect). Always offer the next step ("Add your first unit"). |
| **Error** | calm card, `dangerInk` icon, plain-language cause + **Retry**. Never a raw stack/JSON. | Retry re-runs the load; offline shows "You're offline — changes will sync." |

Result-driven submit buttons everywhere (`isSubmitting` spinner-in-button → `submitSuccess` check → `submitError` inline message), per the existing `TeacherAttendanceViewModel` pattern — applied uniformly to attendance/marks/homework/syllabus saves.

---

## 9. Interactions & Motion

- **Haptics:** dock switch, status-pill set, check-in success, save success (subtle).
- **Optimistic UI:** syllabus toggle, attendance pill, check-in pill flip — instant local change, server-confirm, revert+toast on failure.
- **Transitions:** tab switch = lozenge slide (ParentDock); drill-down = shared-axis/standard push; sheets = bottom-sheet rise (Modal elevation).
- **Confirmations** only where irreversible/notifying: **Publish marks** (names parent count), **delete assessment**, **back-date beyond window**. Everything reversible is one-tap, no dialog.
- **Animation discipline:** ≤200–250ms, ease-standard; respect "reduce motion" system setting (cut to fades).

---

## 10. Iconography

- Single consistent icon set (match parents portal). Line icons, 24dp, `ink2`/`ink3` resting, `accent` active.
- Tab icons: Today (sun/clock), Classes (people/grid), Gradebook (chart/marks), Planner (book/checklist), Profile (person).
- Status uses icon + letter + color triple-encoding (never color-only).
- Avatars: photo or initials on `accentTint` with `accentDeep` ink.

---

## 11. Accessibility Checklist (19–60yo, 58yo is the floor)

- [ ] All tap targets ≥48×48dp.
- [ ] Primary text ≥16sp; honors font scale to 200% without clipping.
- [ ] Contrast: body ink on card ≥ 4.5:1; large text ≥ 3:1 (verify ink tokens against `card`/`background`).
- [ ] No color-only signaling (status carries letter + icon).
- [ ] Every interactive element has a content description (TalkBack/VoiceOver).
- [ ] Focus order logical; keyboard "next" advances marks fields.
- [ ] Error/empty messages are plain language, no codes.
- [ ] Biometric always has a manual fallback (never a hard gate).
- [ ] Reduce-motion honored.
- [ ] Hit areas expanded for small icons.

---

## 12. Cross-Portal Consistency Contract

The teacher portal **must not** invent its own components where a parents equivalent exists:
- Use `VCard`, `VStateHost`, `VColors/VType/VDimens/VElevation`, the dock (ParentDock physics), the header pattern, the schedule card (ParentScheduleCard 3-face), result-driven submit buttons.
- A parent and a teacher in the same household should perceive **one product**. Divergence is a defect.

---

## 13. Cross-refs

- Renders the IA of **Doc 04** and the surfaces of **Docs 05–09**.
- Status colors map to attendance (Doc 06) and marks pass/fail (Doc 07).
- Biometric prompt visual ↔ **Doc 06 §2** flow.
- Long-list rules ↔ **Doc 07 §5.3 / Doc 09 §7**.
- State machine visuals consumed by every endpoint in **Doc 11**.

---

*End of 10_UI_DESIGN_SPEC.md*
