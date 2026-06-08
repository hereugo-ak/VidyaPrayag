package com.littlebridge.vidyaprayag.ui.v2.theme

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import kotlin.math.sqrt

/**
 * VMotion — the design's spring/entrance tokens (UI_FIDELITY_AUDIT §13.2/§13.3).
 *
 * React (`Auth.tsx`) orchestrates a timed reveal ladder using framer-motion springs with literal
 * stiffness/damping numbers. Compose springs use `dampingRatio` + `stiffness`, so we convert the
 * framer (stiffness, damping) pairs exactly:  dampingRatio = damping / (2 * sqrt(stiffness)).
 *
 * Nothing per-screen hardcodes a spring — screens read these tokens.
 */
object VMotion {
    /** framer spring(stiffness=240, damping=22) — logo/cards. */
    val springSoft: AnimationSpec<Float> = framerSpring(stiffness = 240f, damping = 22f)

    /** framer spring(stiffness=220, damping=28) — bottom sheet rise. */
    val springSheet: AnimationSpec<Float> = framerSpring(stiffness = 220f, damping = 28f)

    /** framer spring(stiffness=260, damping=30) — login card. */
    val springCard: AnimationSpec<Float> = framerSpring(stiffness = 260f, damping = 30f)

    /** framer spring(stiffness=300) — success tick / snappy. */
    val springSnappy: AnimationSpec<Float> = framerSpring(stiffness = 300f, damping = 20f)

    /** fadeUp(delayMs): opacity 0→1 + slide y from +12 (matches the React reveal ladder). */
    fun fadeUp(delayMs: Int, durationMs: Int = 450, fromY: Int = 12): EnterTransition =
        fadeIn(animationSpec = tween(durationMs, delayMillis = delayMs, easing = FastOutSlowInEasing)) +
            slideInVertically(
                animationSpec = tween(durationMs, delayMillis = delayMs, easing = FastOutSlowInEasing),
                initialOffsetY = { fromY },
            )

    // ── Screen transitions (Feature 4) ────────────────────────────────────────
    // Subtle "momentum" cues: the incoming screen slides a short distance (NOT a
    // full-screen swipe) while fading in; the outgoing screen drifts the other way
    // while fading out. Offsets are intentionally small (≈30px) so the motion reads
    // as a directional hint, not a carousel. Easing/duration reuse the design tokens
    // (FastOutSlowInEasing) already used by fadeUp. All APIs are commonMain-safe.

    /** Momentum distance for screen transitions, in px. Within the 20–40px spec band. */
    private const val MOMENTUM_PX = 30
    private const val SCREEN_MS = 280

    private fun screenTween() = tween<Float>(SCREEN_MS, easing = FastOutSlowInEasing)
    private fun screenTweenInt() = tween<androidx.compose.ui.unit.IntOffset>(SCREEN_MS, easing = FastOutSlowInEasing)

    /**
     * Forward horizontal momentum: incoming enters from the right (+px) and the
     * outgoing exits to the left (−px), both cross-fading. Used for funnel screens
     * that advance "deeper" (landing → auth → discovery …).
     */
    fun forwardSlide(): ContentTransform =
        (fadeIn(screenTween()) + slideInHorizontally(screenTweenInt()) { MOMENTUM_PX }) togetherWith
            (fadeOut(screenTween()) + slideOutHorizontally(screenTweenInt()) { -MOMENTUM_PX })

    /**
     * Modal vertical momentum: incoming rises from below (+px) and fades in while the
     * outgoing fades out. Used for one-time gate steps (link-child / onboarding /
     * first-login) which read as modal sheets layered over the portal.
     */
    fun modalRise(): ContentTransform =
        (fadeIn(screenTween()) + slideInVertically(screenTweenInt()) { MOMENTUM_PX }) togetherWith
            (fadeOut(screenTween()) + slideOutVertically(screenTweenInt()) { -MOMENTUM_PX / 2 })

    /** Quiet cross-fade only — for the brief resolving frame (no directional cue). */
    fun quietFade(): ContentTransform =
        fadeIn(screenTween()) togetherWith fadeOut(screenTween())
}

/** Convert a framer-motion (stiffness, damping) pair to a Compose float spring. */
private fun framerSpring(stiffness: Float, damping: Float): AnimationSpec<Float> {
    val ratio = (damping / (2f * sqrt(stiffness))).coerceIn(0.05f, 1f)
    return spring(dampingRatio = ratio, stiffness = stiffness)
}

/**
 * pressScale — every tappable surface should visibly *give* on press (scale → .98), matching the
 * React `active:scale-[0.98]` micro-interaction (§13.3). Drive it from an [interactionSource] so
 * it works identically on Android touch and iOS.
 */
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.98f,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale",
    )
    this.scale(scale)
}
