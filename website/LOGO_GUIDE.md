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
on a strict **64-unit grid** and elevated:

| Element | App mark (old) | Website mark (new) | Rationale |
|---|---|---|---|
| Span arc | teal quadratic arc `M12 32 Q28 12 44 32` | lavender-violet gradient parabola `M14 40 Q32 14 50 40` | Same bridge silhouette; accent moved from teal → the system lavender `#E6E6FA`/`#6C5CE0` family (brief). |
| Deck | navy line | navy line `M12 44 H52` | Unchanged — the grounding baseline. |
| Cables | 3 vertical cables | 3 cables stepping **up to the keystone** | Same count/positions, now lead the eye to the apex. |
| Apex node | navy dot (centre) | navy **keystone "+"** | The "+" of *Enroll+* is now *built into the mark* — one symbol does double duty. |
| Plate | teal-tinted / glass | restrained lavender `#E6E6FA` plate (or navy card for dark) | Lavender used with restraint per brief; infrastructure, not classroom imagery. |

Recognisable at **16×16** (favicon), premium at **400×400** (embossed on a black
card → use the dark variant).

The **wordmark "Enroll+" is unchanged** — same spelling, casing, and "+".

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
| 1 | `composeApp/src/commonMain/kotlin/com/littlebridge/vidyaprayag/ui/v2/components/VLogo.kt` | Replace `drawBridge`-style geometry with the new 64-unit span: arc `M14 40 Q32 14 50 40`, deck `M12 44 H52`, cables at x=20/32/44, pillar caps at (14,40)/(50,40), and the keystone **"+"** at (32,27) drawn as two crossed strokes. Recolor the arc to the lavender-violet gradient (`#544AB8`→`#6C5CE0`→`#544AB8`); keep deck + keystone navy. |
| 2 | `composeApp/src/commonMain/kotlin/com/littlebridge/vidyaprayag/ui/v2/components/VBrandLogo.kt` | Same geometry update in `drawBridge`; for the dark/teal hero plate use white deck/cables + lavender keystone (mirror `enrollplus-mark-dark.svg`). |
| 3 | `composeApp/src/androidMain/res/drawable-v24/ic_launcher_foreground.xml` | Regenerate from `enrollplus-mark.svg` (vector drawable). The launcher icon should be the **mark only**. |
| 4 | `composeApp/src/androidMain/res/drawable-v24/ic_launcher_monochrome.xml` | Regenerate single-colour silhouette of the new mark (deck + arc + keystone). |
| 5 | `composeApp/src/androidMain/res/drawable/ic_launcher_background.xml` | Set background to lavender `#E6E6FA` (light) to match the new plate. |
| 6 | `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/` | Replace the iOS app icon PNGs by exporting `enrollplus-mark-dark.svg` (navy card) at all required sizes. |
| 7 | Wordmark usages (search `VidyaSetu`/`buildAnnotatedString` in `VLogo.kt`) | The brief keeps the **"Enroll+"** wordmark — ensure the app's wordmark text reads `Enroll` + accent `+`, matching `EnrollWordmark` in the website. |

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
- [ ] (Mobile, when applied) launcher icon + in-app `VLogo` match the website.
