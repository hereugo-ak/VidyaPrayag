/*
 * File: ParentPewsScreenV2.kt
 * Module: ui.v2.screens.parent
 *
 * The parent's PEWS view — a gentle, label-free nudge about their child.
 * No "risk" word, no score. Shows a supportive prompt + deep links to
 * attendance / message-teacher screens.
 *
 * Backed by GET /api/v1/parent/pews/{childId} (gated by pews_config.parent_share_enabled).
 */
package com.littlebridge.enrollplus.ui.v2.screens.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.enrollplus.feature.pews.domain.model.PewsParentActionDto
import com.littlebridge.enrollplus.feature.pews.domain.model.PewsParentNudgeDto
import com.littlebridge.enrollplus.feature.pews.presentation.ParentNudgeState
import com.littlebridge.enrollplus.feature.pews.presentation.ParentNudgeViewModel
import com.littlebridge.enrollplus.ui.v2.components.VBackHeader
import com.littlebridge.enrollplus.ui.v2.components.VButton
import com.littlebridge.enrollplus.ui.v2.components.VButtonSize
import com.littlebridge.enrollplus.ui.v2.components.VButtonVariant
import com.littlebridge.enrollplus.ui.v2.components.VCard
import com.littlebridge.enrollplus.ui.v2.components.VIcons
import com.littlebridge.enrollplus.ui.v2.screens.VStateHost
import com.littlebridge.enrollplus.ui.v2.screens.collectAsStateV2
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ParentPewsScreenV2(
    childId: String,
    childName: String,
    onBack: () -> Unit = {},
    onDeepLink: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ParentNudgeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()

    Column(modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        VBackHeader(title = childName, onBack = onBack)
        ParentPewsContent(
            state = state,
            childName = childName,
            onRetry = { viewModel.load(childId) },
            onAction = onDeepLink,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun ParentPewsContent(
    state: ParentNudgeState,
    childName: String,
    onRetry: () -> Unit,
    onAction: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    VStateHost(
        loading = state.isLoading,
        error = state.error,
        isEmpty = !state.isLoading && state.error == null && state.nudge == null,
        emptyIcon = VIcons.ShieldCheck,
        emptyTitle = "All good!",
        emptyBody = "There's no specific concern for $childName right now. Keep up the great support!",
        onRetry = onRetry,
        modifier = modifier,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.nudge?.let { nudge ->
                if (nudge.show) {
                    item { NudgeCard(nudge, onAction) }
                } else {
                    item { AllClearCard(childName) }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun NudgeCard(nudge: PewsParentNudgeDto, onAction: (String) -> Unit) {
    val c = VTheme.colors
    VCard {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(c.lavenderLight),
                contentAlignment = Alignment.Center,
            ) {
                Icon(VIcons.Sparkles, contentDescription = null, tint = c.accent, modifier = Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    nudge.headline,
                    style = VTheme.type.bodyStrong.colored(c.ink).copy(fontSize = 15.sp),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    nudge.message,
                    style = VTheme.type.body.colored(c.ink2).copy(fontSize = 13.sp, lineHeight = 19.sp),
                )
            }
        }

        nudge.attendancePct?.let { att ->
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(c.cream).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Attendance", style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 12.sp))
                Text("$att%", style = VTheme.type.bodyStrong.colored(c.ink).copy(fontSize = 14.sp))
            }
        }

        if (nudge.actions.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            nudge.actions.forEach { action ->
                VButton(
                    text = action.label,
                    onClick = { onAction(action.deepLink) },
                    variant = VButtonVariant.Secondary,
                    size = VButtonSize.Md,
                )
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun AllClearCard(childName: String) {
    val c = VTheme.colors
    VCard {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(c.success),
                contentAlignment = Alignment.Center,
            ) {
                Icon(VIcons.ShieldCheck, contentDescription = null, tint = c.successInk, modifier = Modifier.size(18.dp))
            }
            Column {
                Text(
                    "All on track",
                    style = VTheme.type.bodyStrong.colored(c.ink).copy(fontSize = 15.sp),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "$childName is doing well. No specific concerns at this time.",
                    style = VTheme.type.body.colored(c.ink2).copy(fontSize = 13.sp, lineHeight = 19.sp),
                )
            }
        }
    }
}
