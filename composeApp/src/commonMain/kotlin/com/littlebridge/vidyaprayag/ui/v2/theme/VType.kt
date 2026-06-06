package com.littlebridge.vidyaprayag.ui.v2.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * VidyaSetu type scale — from design `theme.css` base layer + rebuild plan §3.3.
 *
 * Primary UI typeface is "Plus Jakarta Sans"; data/number typeface is "DM Mono". Custom font
 * resources are not yet bundled in `composeResources`, so [uiFamily] falls back to the platform
 * default and [dataFamily] to [FontFamily.Monospace] (tabular numerals). The token API is stable:
 * once the .ttf resources are added, only [VTypography] construction changes — no call sites.
 */
@Immutable
data class VTypography(
    val uiFamily: FontFamily,
    val dataFamily: FontFamily,

    // UI scale (Plus Jakarta Sans)
    val h1: TextStyle,        // 32 / 800 / -0.02em
    val h2: TextStyle,        // 22 / 700 / -0.01em
    val h3: TextStyle,        // 17 / 700
    val h4: TextStyle,        // 14 / 600
    val body: TextStyle,      // 14 / 400
    val bodyStrong: TextStyle,// 14 / 600
    val caption: TextStyle,   // 12 / 500
    val label: TextStyle,     // 11 / 600 UPPER 0.08em

    // Data scale (DM Mono — tabular)
    val data: TextStyle,      // 15 / 400
    val dataSm: TextStyle,    // 13 / 400
    val dataLg: TextStyle,    // 22 / 500 (hero numbers)
)

fun defaultVTypography(
    uiFamily: FontFamily = FontFamily.SansSerif,
    dataFamily: FontFamily = FontFamily.Monospace,
): VTypography = VTypography(
    uiFamily = uiFamily,
    dataFamily = dataFamily,
    h1 = TextStyle(fontFamily = uiFamily, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 35.sp, letterSpacing = (-0.02).em),
    h2 = TextStyle(fontFamily = uiFamily, fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp, letterSpacing = (-0.01).em),
    h3 = TextStyle(fontFamily = uiFamily, fontSize = 17.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp),
    h4 = TextStyle(fontFamily = uiFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp),
    body = TextStyle(fontFamily = uiFamily, fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 21.sp),
    bodyStrong = TextStyle(fontFamily = uiFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 21.sp),
    caption = TextStyle(fontFamily = uiFamily, fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 17.sp),
    label = TextStyle(fontFamily = uiFamily, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, lineHeight = 14.sp, letterSpacing = 0.08.em),
    data = TextStyle(fontFamily = dataFamily, fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 21.sp),
    dataSm = TextStyle(fontFamily = dataFamily, fontSize = 13.sp, fontWeight = FontWeight.Normal, lineHeight = 17.sp),
    dataLg = TextStyle(fontFamily = dataFamily, fontSize = 22.sp, fontWeight = FontWeight.Medium, lineHeight = 26.sp),
)
