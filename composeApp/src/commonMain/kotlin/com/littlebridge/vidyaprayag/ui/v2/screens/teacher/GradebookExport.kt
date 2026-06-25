package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.littlebridge.vidyaprayag.ui.v2.components.EnrollCard
import com.littlebridge.vidyaprayag.ui.v2.components.SectionHeader
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.theme.Enroll
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import com.littlebridge.vidyaprayag.ui.v2.theme.pressScale

// ─────────────────────────────────────────────────────────────────────────────
// P3-T6 — Export / Share
//
// A bottom-right FAB (export icon) that opens ExportSheet with two routes:
//   • Share as PDF        → host generates the class marks PDF (util / stub)
//   • Share via WhatsApp  → host opens an intent with a pre-filled summary
//
// FAB = primary fill, white icon, ShapeFAB corners, pressScale. All tokens Enroll.*.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * GradebookExportFab — the circular export FAB. Place it bottom-right in the host
 * (e.g. a Box with Alignment.BottomEnd + padding).
 */
@Composable
fun GradebookExportFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ix = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .pressScale(ix)
            .size(56.dp)
            .clip(Enroll.shape.fab)
            .background(Enroll.colors.primary)
            .clickable(interactionSource = ix, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = VIcons.Share,
            contentDescription = "Export class marks",
            tint = Enroll.colors.onPrimary,
            modifier = Modifier.size(22.dp),
        )
    }
}

/**
 * ExportSheet — the two-option export menu.
 *
 * @param visible        show/hide
 * @param onDismiss      close
 * @param onSharePdf     generate + share the class marks PDF (host owns the util)
 * @param onShareWhatsApp open WhatsApp with a pre-filled performance summary
 */
@Composable
fun ExportSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSharePdf: () -> Unit,
    onShareWhatsApp: () -> Unit,
) {
    if (!visible) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Enroll.shape.sheet)
                .background(Enroll.colors.surfaceBase)
                .padding(horizontal = Enroll.space.lg)
                .padding(bottom = Enroll.space.xxl),
        ) {
            Spacer(Modifier.height(Enroll.space.md))
            SectionHeader(title = "EXPORT")
            Spacer(Modifier.height(Enroll.space.md))

            ExportOption(
                icon = VIcons.FileText,
                title = "Share as PDF",
                subtitle = "A printable class marks sheet",
                onClick = onSharePdf,
            )
            Spacer(Modifier.height(Enroll.space.sm))
            ExportOption(
                icon = VIcons.Send,
                title = "Share via WhatsApp",
                subtitle = "A quick class performance summary",
                onClick = onShareWhatsApp,
            )
        }
    }
}

@Composable
private fun ExportOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    EnrollCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(Enroll.shape.pill)
                    .background(Enroll.colors.primarySoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Enroll.colors.primaryMid,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.size(Enroll.space.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = Enroll.type.labelBold.colored(Enroll.colors.textPrimary))
                Text(
                    text = subtitle,
                    style = Enroll.type.bodySmall.colored(Enroll.colors.textTertiary),
                )
            }
            Icon(
                imageVector = VIcons.ChevronRight,
                contentDescription = null,
                tint = Enroll.colors.textTertiary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Build the pre-filled WhatsApp summary text for a class+exam. Host passes this to
 * the platform share intent. Kept here so the copy lives with the export UI.
 *
 * TODO(host): wire to the platform share util (expect/actual) — currently the host
 * is responsible for invoking the OS share sheet with this string.
 */
fun whatsAppSummaryText(
    className: String,
    examName: String,
    classAverage: Float?,
    topScorer: String?,
): String {
    val avg = classAverage?.let { "${it.toInt()}%" } ?: "—"
    val top = topScorer?.let { "\n🏆 Top scorer: $it" } ?: ""
    return "📊 $className — $examName results are in!\n" +
        "Class average: $avg$top\n\n" +
        "Shared via Enroll+"
}
