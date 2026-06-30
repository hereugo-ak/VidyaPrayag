package com.littlebridge.enrollplus.ui.v2.theme

import androidx.compose.ui.graphics.Color
import com.littlebridge.enrollplus.feature.branding.domain.model.SchoolBranding

/**
 * Maps school branding hex colors to [VColors] token overrides.
 *
 * Only the accent family and legacy teal tokens are overridden —
 * ink, surfaces, borders, and semantic colors remain unchanged
 * so the app stays readable and accessible.
 */
object BrandingColorMapper {

    /**
     * Returns a [VColors] with branding colors applied on top of [base].
     * If branding is null or not customized, returns [base] unchanged.
     */
    fun apply(base: VColors, branding: SchoolBranding?): VColors {
        if (branding == null || !branding.isCustomized) return base

        val primary = parseHex(branding.primaryColor) ?: return base
        val secondary = parseHex(branding.secondaryColor) ?: primary
        val accentSoft = parseHex(branding.accentColor) ?: primary

        val tint = deriveTint(primary, base.isNight)

        return base.copy(
            accent = primary,
            accentSoft = accentSoft,
            accentDeep = secondary,
            accentTint = tint,
            teal = primary,
            tealDeep = secondary,
        )
    }

    private fun parseHex(hex: String): Color? {
        return try {
            val value = hex.removePrefix("#")
            if (value.length == 6) {
                val rgb = value.toLong(16)
                Color(
                    red = ((rgb shr 16) and 0xFF) / 255f,
                    green = ((rgb shr 8) and 0xFF) / 255f,
                    blue = (rgb and 0xFF) / 255f,
                )
            } else if (value.length == 3) {
                val r = value[0].digitToInt(16)
                val g = value[1].digitToInt(16)
                val b = value[2].digitToInt(16)
                Color(
                    red = (r * 17) / 255f,
                    green = (g * 17) / 255f,
                    blue = (b * 17) / 255f,
                )
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Derives a subtle tint color from the primary brand color.
     * Light mode: 8% primary over white.
     * Dark mode: 12% primary over near-black.
     */
    private fun deriveTint(primary: Color, isNight: Boolean): Color {
        return if (isNight) {
            Color(
                red = primary.red * 0.12f,
                green = primary.green * 0.12f,
                blue = primary.blue * 0.12f,
                alpha = 1f,
            )
        } else {
            Color(
                red = primary.red * 0.08f + 1f * 0.92f,
                green = primary.green * 0.08f + 1f * 0.92f,
                blue = primary.blue * 0.08f + 1f * 0.92f,
                alpha = 1f,
            )
        }
    }
}
