package com.littlebridge.vidyaprayag.ui.v2.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * VIcons — the curated icon set for the new UI.
 *
 * The design (`UI screens`) uses `lucide-react`. We map each lucide glyph to the closest stable
 * Material core icon (proven to compile in this project's existing UI). For glyphs without a safe
 * 1:1 Material core equivalent we hand-author a Material-style 24dp [ImageVector] (e.g. [Spinner],
 * [GraduationCap], [Target], [Wallet], [Sparkles]) so the set is fully self-contained and compiles
 * on every Compose target with no reliance on the extended icon pack's membership.
 */
object VIcons {
    // ── Direct Material core mappings (lucide → Material) ───────────────────────
    val Home get() = Icons.Filled.Home                       // Home
    val User get() = Icons.Filled.Person                     // User
    val Users get() = Icons.Filled.Person                    // Users (fallback to Person)
    val Search get() = Icons.Filled.Search                   // Search
    val Bell get() = Icons.Filled.Notifications              // Bell
    val Calendar get() = Icons.Filled.CalendarMonth          // Calendar
    val Megaphone get() = Icons.Filled.Campaign              // Megaphone
    val School get() = Icons.Filled.School                   // (school plate)
    val MapPin get() = Icons.Filled.LocationOn               // MapPin
    val Phone get() = Icons.Filled.Phone                     // Phone
    val Mail get() = Icons.Filled.Email                      // Mail
    val Lock get() = Icons.Filled.Lock                       // Lock
    val Check get() = Icons.Filled.Check                     // Check
    val Close get() = Icons.Filled.Close                     // X
    val Plus get() = Icons.Filled.Add                        // Plus
    val Send get() = Icons.Filled.Send                       // Send
    val Share get() = Icons.Filled.Share                     // Share2
    val Star get() = Icons.Filled.Star                       // Star
    val Heart get() = Icons.Filled.Favorite                  // Heart
    val More get() = Icons.Filled.MoreHoriz                  // MoreHorizontal
    val Menu get() = Icons.Filled.Menu                       // PanelLeft / menu
    val Settings get() = Icons.Filled.Settings               // settings
    val Bookmark get() = Icons.Filled.Bookmark               // bookmark
    val ChevronDown get() = Icons.Filled.KeyboardArrowDown   // ChevronDown
    val ChevronLeft get() = Icons.Filled.KeyboardArrowLeft   // ChevronLeft
    val ChevronRight get() = Icons.Filled.KeyboardArrowRight // ChevronRight
    val ArrowLeft get() = Icons.AutoMirrored.Filled.ArrowBack    // ArrowLeft
    val ArrowRight get() = Icons.AutoMirrored.Filled.ArrowForward // ArrowRight
    val Chat get() = Icons.AutoMirrored.Filled.Chat          // MessageSquare

    // ── Hand-authored Material-style vectors (no safe core equivalent) ──────────

    /** Indeterminate loading spinner (¾ ring) used by [VButton]'s loading phase. */
    val Spinner: ImageVector by lazy {
        materialStroke("v_spinner") {
            // arc approximated by an open ring: top-right gap
            moveTo(12f, 3f)
            // right side down
            curveTo(16.97f, 3f, 21f, 7.03f, 21f, 12f)
            curveTo(21f, 16.97f, 16.97f, 21f, 12f, 21f)
            curveTo(7.03f, 21f, 3f, 16.97f, 3f, 12f)
            curveTo(3f, 9.5f, 4.01f, 7.24f, 5.64f, 5.6f)
        }
    }

    /** Graduation cap (lucide GraduationCap). */
    val GraduationCap: ImageVector by lazy {
        materialFill("v_gradcap") {
            moveTo(12f, 3f)
            lineTo(1f, 9f)
            lineTo(12f, 15f)
            lineTo(21f, 10.09f)
            lineTo(21f, 17f)
            lineTo(23f, 17f)
            lineTo(23f, 9f)
            close()
            moveTo(5f, 13.18f)
            lineTo(5f, 17.18f)
            lineTo(12f, 21f)
            lineTo(19f, 17.18f)
            lineTo(19f, 13.18f)
            lineTo(12f, 17f)
            close()
        }
    }

    /** Target / GPS reticle (lucide Target). */
    val Target: ImageVector by lazy {
        materialStroke("v_target") {
            moveTo(12f, 3f); curveTo(7.03f, 3f, 3f, 7.03f, 3f, 12f); curveTo(3f, 16.97f, 7.03f, 21f, 12f, 21f); curveTo(16.97f, 21f, 21f, 16.97f, 21f, 12f); curveTo(21f, 7.03f, 16.97f, 3f, 12f, 3f); close()
            moveTo(12f, 7f); curveTo(9.24f, 7f, 7f, 9.24f, 7f, 12f); curveTo(7f, 14.76f, 9.24f, 17f, 12f, 17f); curveTo(14.76f, 17f, 17f, 14.76f, 17f, 12f); curveTo(17f, 9.24f, 14.76f, 7f, 12f, 7f); close()
            moveTo(12f, 10.5f); curveTo(11.17f, 10.5f, 10.5f, 11.17f, 10.5f, 12f); curveTo(10.5f, 12.83f, 11.17f, 13.5f, 12f, 13.5f); curveTo(12.83f, 13.5f, 13.5f, 12.83f, 13.5f, 12f); curveTo(13.5f, 11.17f, 12.83f, 10.5f, 12f, 10.5f); close()
        }
    }

    /** Wallet (lucide Wallet). */
    val Wallet: ImageVector by lazy {
        materialFill("v_wallet") {
            moveTo(21f, 7f)
            lineTo(21f, 6f)
            curveTo(21f, 4.9f, 20.1f, 4f, 19f, 4f)
            lineTo(5f, 4f)
            curveTo(3.9f, 4f, 3f, 4.9f, 3f, 6f)
            lineTo(3f, 18f)
            curveTo(3f, 19.1f, 3.9f, 20f, 5f, 20f)
            lineTo(19f, 20f)
            curveTo(20.1f, 20f, 21f, 19.1f, 21f, 18f)
            lineTo(21f, 17f)
            lineTo(12f, 17f)
            curveTo(10.9f, 17f, 10f, 16.1f, 10f, 15f)
            lineTo(10f, 9f)
            curveTo(10f, 7.9f, 10.9f, 7f, 12f, 7f)
            close()
            moveTo(12f, 9f)
            lineTo(22f, 9f)
            lineTo(22f, 15f)
            lineTo(12f, 15f)
            close()
            moveTo(16f, 13.5f)
            curveTo(15.17f, 13.5f, 14.5f, 12.83f, 14.5f, 12f)
            curveTo(14.5f, 11.17f, 15.17f, 10.5f, 16f, 10.5f)
            curveTo(16.83f, 10.5f, 17.5f, 11.17f, 17.5f, 12f)
            curveTo(17.5f, 12.83f, 16.83f, 13.5f, 16f, 13.5f)
            close()
        }
    }

    /** Sparkles (lucide Sparkles) — discovery accent. */
    val Sparkles: ImageVector by lazy {
        materialFill("v_sparkles") {
            moveTo(12f, 2f)
            lineTo(14f, 9f)
            lineTo(21f, 11f)
            lineTo(14f, 13f)
            lineTo(12f, 20f)
            lineTo(10f, 13f)
            lineTo(3f, 11f)
            lineTo(10f, 9f)
            close()
            moveTo(19f, 3f)
            lineTo(20f, 6f)
            lineTo(23f, 7f)
            lineTo(20f, 8f)
            lineTo(19f, 11f)
            lineTo(18f, 8f)
            lineTo(15f, 7f)
            lineTo(18f, 6f)
            close()
        }
    }
}

// ── Builders ───────────────────────────────────────────────────────────────────

private inline fun materialFill(
    name: String,
    pathBuilder: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit,
): ImageVector = ImageVector.Builder(
    name = name, defaultWidth = 24.dp, defaultHeight = 24.dp,
    viewportWidth = 24f, viewportHeight = 24f,
).apply {
    path(fill = SolidColor(Color.Black), pathBuilder = pathBuilder)
}.build()

private inline fun materialStroke(
    name: String,
    strokeWidth: Float = 2f,
    pathBuilder: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit,
): ImageVector = ImageVector.Builder(
    name = name, defaultWidth = 24.dp, defaultHeight = 24.dp,
    viewportWidth = 24f, viewportHeight = 24f,
).apply {
    path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = strokeWidth,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = pathBuilder,
    )
}.build()
