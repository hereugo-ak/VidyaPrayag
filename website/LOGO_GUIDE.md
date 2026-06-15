# Enroll+ Logo — Replacement Guide

The Enroll+ brand mark was redesigned at the $100M level. This document records
**what changed**, **why it is an evolution (not a replacement)** of the mobile
app's existing mark, where the new assets live on the website, and **exactly
which mobile-app files must be updated** to bring the app in line.

> No mobile-app (`composeApp/`) files were modified by this change — per the
> brief, the app touchpoints are only **documented** here.

---

## 1. The design

The mobile app's mark is the **"Setu" bridge** — an arc spanning two grounded
pillars over a deck, with three suspension cables, signalling the connection
between school and home (see `composeApp/.../ui/v2/components/VLogo.kt` and
`VBrandLogo.kt`).

The new **"Span"** mark keeps that exact metaphor and geometry family, redrawn
on a strict **64-unit grid** and elevated to the $100M Series-A level:

| Element | App mark (old) | Website mark (new) | Rationale |
|---|---|---|---|
| Span arc | teal quadratic arc `M12 32 Q28 12 44 32` | lavender-violet gradient parabola `M16 42 Q32 16 48 42` | Same bridge silhouette; accent moved from teal → the system lavender `#E6E6FA`/`#6C5CE0` family (brief). |
| Deck | navy line | navy line `M14 46 H50` | Unchanged in spirit — the grounding baseline. |
| Cables | 3 vertical cables | 3 cables at x=22/32/42 stepping **up to the apex** | Same count/positions, now lead the eye to the apex node. |
| Apex node | navy dot (centre) | **navy solid node** at (32,24), `r=3.6` | **No "+" sign in the mark.** The apex is a single clean node — the true terminus of the centre cable, exactly the app's original centre dot. The "+" lives only in the *wordmark text*, never the symbol. |
| Plate | teal-tinted / glass | restrained lavender `#E6E6FA` plate (or navy card for dark) | Lavender used with restraint per brief; infrastructure, not classroom imagery. |

> **Design rule (per the latest brief): the graphic mark contains NO "+" symbol.**
> Earlier drafts built the "+" into the keystone — that has been removed. The
> mark is now a pure monoline bridge resolving to a solid apex node, which reads
> as confident infrastructure rather than a literal plus.

Recognisable at **16×16** (favicon), premium at **400×400** (embossed on a black
card → use the dark variant).

The **wordmark "Enroll+" is unchanged** — same spelling, casing, and the "+" in
the *text* (this is the only place the "+" appears).

---

## 2. Website assets (already replaced)

| Form | File | Use |
|---|---|---|
| Mark only (light) | `website/public/brand/enrollplus-mark.svg` | favicon, app icon, inline |
| Mark only (dark) | `website/public/brand/enrollplus-mark-dark.svg` | embossed-on-black card, Apple touch icon |
| Full lockup | `website/public/brand/enrollplus-lockup.svg` | mark + wordmark side by side |
| Stacked lockup | `website/public/brand/enrollplus-stacked.svg` | mark above wordmark |
| React component (single source) | `website/src/components/ui/Logo.tsx` | `<Logo/>`, `<EnrollMark/>`, `<EnrollWordmark/>` — geometry identical to the SVGs |

The component is the live source for the header (`components/Header.tsx`), footer
(`components/Footer.tsx`), the admin sidebar, and the favicon is wired in
`src/app/layout.tsx → metadata.icons`.

---

## 3. Mobile-app files to update (NOT modified here)

To bring the Compose app in line with the new mark, update these files. The new
geometry (64-unit viewBox) maps cleanly onto the existing Canvas draw routines.

| # | Path | What to change |
|---|---|---|
| 1 | `composeApp/src/commonMain/kotlin/com/littlebridge/vidyaprayag/ui/v2/components/VLogo.kt` | Update the `Canvas` geometry to the new 64-unit span: arc `M16 42 Q32 16 48 42`, deck `M14 46 H50`, cables at x=22/32/42 (top y=38.2/26/38.2 → 46), pillar caps at (16,42)/(48,42), and a **solid apex node** at (32,24) `r≈3.6`. **Do NOT draw a "+".** Recolor the arc to the lavender-violet gradient (`#544AB8`→`#6C5CE0`→`#544AB8`); keep deck + node navy. |
| 2 | `composeApp/src/commonMain/kotlin/com/littlebridge/vidyaprayag/ui/v2/components/VBrandLogo.kt` | Same geometry update in `drawBridge`; for the dark/teal hero plate use white deck/cables + lavender apex node (mirror `enrollplus-mark-dark.svg`). Still **no "+"**. |
| 3 | `composeApp/src/androidMain/res/drawable-v24/ic_launcher_foreground.xml` | Regenerate from `enrollplus-mark.svg` (vector drawable). The launcher icon should be the **mark only** (no "+"). |
| 4 | `composeApp/src/androidMain/res/drawable-v24/ic_launcher_monochrome.xml` | Regenerate single-colour silhouette of the new mark (deck + arc + apex node). |
| 5 | `composeApp/src/androidMain/res/drawable/ic_launcher_background.xml` | Set background to lavender `#E6E6FA` (light) to match the new plate. |
| 6 | `composeApp/src/androidMain/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher*.webp` | Re-export the raster launcher icons (`ic_launcher.webp`, `ic_launcher_round.webp`, `ic_launcher_foreground.webp`) at every density from the new mark. |
| 7 | `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/app-icon-1024.png` (+ any other sizes) | Replace the iOS app icon PNG(s) by exporting `enrollplus-mark-dark.svg` (navy card) at all required sizes. |
| 8 | Wordmark usages (search `VidyaSetu`/`buildAnnotatedString` in `VLogo.kt`) | The brief keeps the **"Enroll+"** wordmark text — ensure the app's wordmark reads `Enroll` + accent `+`, matching `EnrollWordmark` in the website. The "+" stays in the *text only*. |

> Tip: the SVG `path` data in `website/public/brand/enrollplus-mark.svg` can be
> pasted almost verbatim into an Android `<vector>`/`<path>` (convert the
> `viewBox` to `viewportWidth/Height="64"`).

---

## 4. Verification checklist

- [ ] Favicon shows the new mark in the browser tab (`/brand/enrollplus-mark.svg`).
- [ ] Header + footer render the full lockup via `<Logo/>`.
- [ ] Admin sidebar shows the mark + school name.
- [ ] Dark variant legible when embossed on a black/navy card.
- [ ] Mark legible at 16px (zoom the favicon).
- [ ] **No "+" sign anywhere in the graphic mark** (only in the wordmark text).
- [ ] (Mobile, when applied) launcher icon + in-app `VLogo` match the website.
