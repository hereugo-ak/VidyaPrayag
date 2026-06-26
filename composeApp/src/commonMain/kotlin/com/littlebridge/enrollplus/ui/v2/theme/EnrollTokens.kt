package com.littlebridge.enrollplus.ui.v2.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * ENROLL+ TEACHER PORTAL — SEMANTIC TOKEN BRIDGE
 * (Loop task P1-T1 — "Design System Foundation")
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * WHY THIS FILE EXISTS (and is NOT a new indigo `EnrollTheme`)
 * ─────────────────────────────────────────────────────────────────────────────
 * The TEACHER_PORTAL_LOOP spec (PART 2) names its tokens in an "EnrollTheme /
 * indigo" vocabulary — `PrimaryIndigo`, `SurfaceCard`, `StatusPresent`,
 * `LabelCaps`, `SpaceLG`, `ShapeCard`, … — and asks for `EnrollColor.kt`,
 * `EnrollTypography.kt`, `EnrollShape.kt` wired into an `EnrollTheme {}`.
 *
 * BUT the whole Parents + Teacher portal already ships a complete, mature,
 * fully token-driven design system: [VTheme] → [VColors] (teal / navy / the
 * violet `#6C5CE0` accent family), [VTypography] (Plus Jakarta Sans + DM Mono),
 * [VDimens], [VElevation], [VMotion]. Every existing screen reads from it, and
 * there is not a single hardcoded `Color(0x…)` in the teacher screens.
 *
 * The iteration's standing IMPORTANT NOTE overrides the loop on exactly this
 * point: *"If the loop asks to use any other colour that breaks the current
 * colour pattern you can avoid that — stick with the colour and theme pattern
 * followed in the whole Parents and Teacher portal right now."* Forking a second
 * indigo theme would (a) break portal-wide colour parity, (b) duplicate the
 * design system, and (c) re-introduce hardcoded hex — the exact opposite of the
 * loop's own Done Criteria.
 *
 * So P1-T1 is satisfied by its *intent*, not its letter: this file is the single
 * foundation that gives every later loop task ("use `PrimaryIndigo`", "use
 * `LabelCaps`", "use `ShapeCard`") a concrete, token-backed home — by mapping the
 * loop's semantic vocabulary onto the EXISTING [VTheme]. No new colours are
 * introduced; every value below resolves to an established design token.
 *
 * USAGE (identical ergonomics to the loop's `EnrollTheme.colors.PrimaryIndigo`):
 *   `Enroll.colors.primary`        // == VTheme.colors.accent  (#6C5CE0)
 *   `Enroll.type.headingLarge`     // == VTheme.type.h2
 *   `Enroll.shape.card`            // == RoundedCornerShape(VTheme.dimens.radiusCard)
 *   `Enroll.space.lg`              // == VTheme.dimens.md  (16dp screen rhythm)
 *
 * All accessors are @Composable read-only and resolve from the active [VTheme],
 * so they automatically honour the Light / Night portal tone — exactly like the
 * rest of the system. Nothing here is a literal hex.
 */
object Enroll {
    // NOTE: these accessors are plain @Composable (NOT @ReadOnlyComposable) on
    // purpose — they read from [VTheme.colors] / [VTheme.type] / [VTheme.dimens],
    // whose own getters are @Composable only. A @ReadOnlyComposable getter may
    // only call other @ReadOnlyComposable composables, so marking these as
    // @ReadOnlyComposable would not compile. They mirror VTheme's own contract.
    val colors: EnrollColors
        @Composable get() = EnrollColors(VTheme.colors)

    val type: EnrollType
        @Composable get() = EnrollType(VTheme.type)

    val shape: EnrollShape
        @Composable get() = EnrollShape(VTheme.dimens)

    val space: EnrollSpace
        @Composable get() = EnrollSpace(VTheme.dimens)
}

// ─────────────────────────────────────────────────────────────────────────────
// COLOUR BRIDGE
//
// The loop's "Trusted Authority" intent — a calm, premium, single dominant brand
// accent with preserved data-only status colours — is *already* what the portal
// does: the violet `accent` (#6C5CE0) is the portal-wide active accent (rings,
// pills, active tabs), and `success/warning/danger` inks are the data semantics.
// We therefore map the loop's indigo family onto the violet accent family (NOT a
// new indigo), keeping every screen visually identical to Parents.
// ─────────────────────────────────────────────────────────────────────────────
class EnrollColors(private val v: VColors) {
    // Brand — loop `PrimaryIndigo*` → portal violet accent family (no new hex).
    val primary: Color get() = v.accent          // #6C5CE0  (loop: PrimaryIndigo)
    val primaryMid: Color get() = v.accentSoft    // #8B7EE8  (loop: PrimaryIndigoMid)
    val primaryDeep: Color get() = v.accentDeep    // #544AB8  (interactive/ink-on-tint)
    val primarySoft: Color get() = v.accentTint    // #F4F3FA  (loop: PrimaryIndigoSoft — chips/selected)

    // Surfaces — loop `Surface*` → portal surface tokens.
    val surfaceBase: Color get() = v.background     // app canvas        (loop: SurfaceBase)
    val surfaceCard: Color get() = v.card           // card fill         (loop: SurfaceCard)
    val surfaceSubtle: Color get() = v.cream         // inactive areas    (loop: SurfaceSubtle)
    val border: Color get() = v.border1            // 1px card border   (ElevationCard rule)
    val hairline: Color get() = v.hairline          // 1px divider

    // Text — loop `Text*` → portal ink scale.
    val textPrimary: Color get() = v.ink            // headlines/names   (loop: TextPrimary)
    val textSecondary: Color get() = v.ink2          // labels/captions   (loop: TextSecondary)
    val textTertiary: Color get() = v.ink3           // placeholder       (loop: TextTertiary)
    val onPrimary: Color get() = Color.White         // text on accent fill

    // Semantic status — PRESERVED per Done Criteria (data-only, never branding).
    // Mapped to the portal's status *inks* (the legible foreground form).
    val statusPresent: Color get() = v.successInk    // present/A grade   (loop: StatusPresent)
    val statusAbsent: Color get() = v.dangerInk      // absent/F grade    (loop: StatusAbsent)
    val statusLate: Color get() = v.warningInk       // late/C grade      (loop: StatusLate)
    val statusPending: Color get() = v.accent        // pending/info      (loop: StatusPending)

    // Soft fills behind the status inks (chips / row tints).
    val statusPresentSoft: Color get() = v.success
    val statusAbsentSoft: Color get() = v.danger
    val statusLateSoft: Color get() = v.warning

    // Accent (CTAs / exam markers) — loop `AccentAmber*` → portal warning family.
    val accent: Color get() = v.warningInk           // amber CTA ink     (loop: AccentAmber)
    val accentSoft: Color get() = v.warning          // amber chip bg     (loop: AccentAmberSoft)

    // Brand gradient — loop wants it ONLY on Home/Profile headers; the portal's
    // canonical hero gradient is accentDeep → accent (violet), matching Parents.
    val gradientStart: Color get() = v.accentDeep    // (loop: GradientStart)
    val gradientEnd: Color get() = v.accent          // (loop: GradientEnd)

    /** The signature header gradient — use only on Home + Profile heroes (loop §SIGNATURE). */
    val headerGradient: Brush
        get() = Brush.horizontalGradient(listOf(gradientStart, gradientEnd))
}

// ─────────────────────────────────────────────────────────────────────────────
// TYPOGRAPHY BRIDGE
//
// Loop names → existing VTypography styles. The portal scale is already the
// premium Plus Jakarta Sans / DM Mono system with the exact negative tracking and
// tabular-number data styles the loop spec asks for, so we alias rather than
// redefine sizes (which would drift from Parents).
// ─────────────────────────────────────────────────────────────────────────────
class EnrollType(private val t: VTypography) {
    // Titles
    val headingLarge: TextStyle get() = t.h2          // 22/700/-0.01em  (loop: HeadingLarge 24/700)
    val headingMedium: TextStyle get() = t.h3          // 17/700          (loop: HeadingMedium)
    val headingSmall: TextStyle get() = t.h4           // 14/600          (loop: HeadingSmall)
    val display: TextStyle get() = t.h1                // 32/800 hero headline

    // Body
    val bodyLarge: TextStyle get() = t.body            // 14/400          (loop: BodyLarge)
    val bodyMedium: TextStyle get() = t.body            // 14/400          (loop: BodyMedium)
    val bodySmall: TextStyle get() = t.caption          // 12/500          (loop: BodySmall)

    // Labels / Data
    val labelBold: TextStyle get() = t.bodyStrong       // 14/600          (loop: LabelBold)
    val labelCaps: TextStyle get() = t.labelStrong      // 11/700 UPPER    (loop: LabelCaps — section headers)
    val dataLarge: TextStyle get() = t.dataLg           // 22/500 tnum     (loop: DataLarge — stats)
    val dataMedium: TextStyle get() = t.data            // 15/400 tnum     (loop: DataMedium)
    val dataSmall: TextStyle get() = t.dataSm           // 13/400 tnum
}

// ─────────────────────────────────────────────────────────────────────────────
// SHAPE BRIDGE — loop `Shape*` → RoundedCornerShape from VDimens radii.
// ─────────────────────────────────────────────────────────────────────────────
class EnrollShape(private val d: VDimens) {
    val card: RoundedCornerShape get() = RoundedCornerShape(d.radiusCard)   // 16dp (loop: ShapeCard)
    val chip: RoundedCornerShape get() = RoundedCornerShape(d.radiusSm)     // 6dp  (loop: ShapeChip)
    val input: RoundedCornerShape get() = RoundedCornerShape(d.radiusInput) // 12dp
    val sheet: RoundedCornerShape
        get() = RoundedCornerShape(topStart = d.radiusSheet, topEnd = d.radiusSheet) // (loop: ShapeSheet)
    val fab: RoundedCornerShape get() = RoundedCornerShape(d.radiusXl)      // 20dp (loop: ShapeFAB)
    val pill: RoundedCornerShape get() = RoundedCornerShape(d.radiusPill)   // 999dp
}

// ─────────────────────────────────────────────────────────────────────────────
// SPACING BRIDGE — loop `Space*` / `ScreenPadding` → VDimens scale (base-4).
// ─────────────────────────────────────────────────────────────────────────────
class EnrollSpace(private val d: VDimens) {
    val xs: Dp get() = d.xs                 // 4dp   (loop: SpaceXS)
    val sm: Dp get() = d.sm                 // 8dp   (loop: SpaceSM)
    val md: Dp get() = 12.dp                // 12dp  (loop: SpaceMD)
    val lg: Dp get() = d.md                 // 16dp  (loop: SpaceLG)
    val xl: Dp get() = 20.dp                // 20dp  (loop: SpaceXL)
    val xxl: Dp get() = d.lg                // 24dp  (loop: Space2XL)
    val xxxl: Dp get() = d.xl               // 32dp  (loop: Space3XL)
    val screen: Dp get() = d.screenPadding  // 16dp  (loop: ScreenPadding)
}
