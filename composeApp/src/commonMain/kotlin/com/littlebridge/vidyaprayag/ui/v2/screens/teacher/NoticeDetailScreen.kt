package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.ui.v2.components.EnrollCard
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.theme.Enroll
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import com.littlebridge.vidyaprayag.ui.v2.theme.pressScale

/**
 * NoticeDetailScreen — the simple read-only notice/announcement view (Loop task P7-T4).
 *
 * The terminal destination for a system-notice notification ([TeacherDestination
 * .NoticeDetail], produced by `TeacherNavRouter.routeNotification` for Announcement /
 * Homework / General types). It is intentionally a plain text reader — no actions, no
 * editing — so a tapped announcement always lands somewhere meaningful instead of a
 * dead end (the Phase 7 goal).
 *
 * Layout: a calm-canvas screen with a custom back top-bar (mirroring the portal's
 * ChatTopBar idiom — no Material3 TopAppBar) over one [EnrollCard] holding a
 * `Megaphone` plate, the notice `title` in `headingMedium`, an optional `timeLabel`
 * caption, and the full `body` in `bodyLarge`. The body scrolls for long notices.
 *
 * Pure & data-driven: every string comes from the caller (the notification's own
 * title/message become the body so the screen is never empty). All colour/type via
 * `Enroll.*` → VTheme; no new hex.
 */
@Composable
fun NoticeDetailScreen(
    title: String,
    body: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    timeLabel: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Enroll.colors.surfaceBase),
    ) {
        NoticeTopBar(onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Enroll.space.screen, vertical = Enroll.space.lg),
        ) {
            EnrollCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(Enroll.shape.chip)
                            .background(Enroll.colors.primarySoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            VIcons.Megaphone,
                            contentDescription = null,
                            tint = Enroll.colors.primaryDeep,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(Enroll.space.md))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = Enroll.type.headingMedium.colored(Enroll.colors.textPrimary),
                        )
                        if (!timeLabel.isNullOrBlank()) {
                            Spacer(Modifier.height(Enroll.space.xs))
                            Text(
                                text = timeLabel,
                                style = Enroll.type.bodySmall.colored(Enroll.colors.textTertiary),
                                maxLines = 1,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(Enroll.space.lg))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Enroll.colors.hairline),
                )
                Spacer(Modifier.height(Enroll.space.lg))

                Text(
                    text = body,
                    style = Enroll.type.bodyLarge.colored(Enroll.colors.textSecondary),
                )
            }
        }
    }
}

/** Custom back top-bar — same idiom as ChatTopBar (no Material3 TopAppBar). */
@Composable
private fun NoticeTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Enroll.colors.surfaceCard)
            .statusBarsPadding()
            .padding(horizontal = Enroll.space.md, vertical = Enroll.space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val ix = remember { MutableInteractionSource() }
        Box(
            Modifier
                .pressScale(ix)
                .size(36.dp)
                .clip(Enroll.shape.pill)
                .clickable(interactionSource = ix, indication = null, onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                VIcons.ArrowLeft,
                contentDescription = "Back",
                tint = Enroll.colors.textPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(Enroll.space.sm))
        Text(
            text = "Notice",
            style = Enroll.type.labelBold.colored(Enroll.colors.textPrimary),
            maxLines = 1,
        )
    }
}
