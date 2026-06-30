package com.littlebridge.enrollplus.ui.v2.screens.school

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.littlebridge.enrollplus.feature.scheduling.domain.model.ScheduledMessageDto
import com.littlebridge.enrollplus.feature.scheduling.presentation.ScheduledMessagesViewModel
import com.littlebridge.enrollplus.ui.v2.components.VBackHeader
import com.littlebridge.enrollplus.ui.v2.components.VBadge
import com.littlebridge.enrollplus.ui.v2.components.VBadgeTone
import com.littlebridge.enrollplus.ui.v2.components.VButton
import com.littlebridge.enrollplus.ui.v2.components.VButtonSize
import com.littlebridge.enrollplus.ui.v2.components.VButtonVariant
import com.littlebridge.enrollplus.ui.v2.components.VCard
import com.littlebridge.enrollplus.ui.v2.components.VIcons
import com.littlebridge.enrollplus.ui.v2.components.VPullRefresh
import com.littlebridge.enrollplus.ui.v2.screens.VStateHost
import com.littlebridge.enrollplus.ui.v2.screens.collectAsStateV2
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored
import com.littlebridge.enrollplus.ui.v2.theme.shapeInput
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ScheduledMessagesScreenV2(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScheduledMessagesViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()

    LaunchedEffect(Unit) { viewModel.load() }

    Column(
        modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp),
    ) {
        VBackHeader(title = "Scheduled Messages", onBack = onBack)
        Spacer(Modifier.height(12.dp))

        StatusFilterRow(
            selected = state.statusFilter,
            onSelect = { viewModel.setStatusFilter(it) },
        )
        Spacer(Modifier.height(12.dp))

        VPullRefresh(isRefreshing = state.isLoading, onRefresh = { viewModel.load() }) {
            VStateHost(
                loading = state.isLoading,
                error = state.errorMessage,
                isEmpty = state.messages.isEmpty(),
                emptyTitle = "No scheduled messages",
                emptyBody = "Schedule announcements and broadcasts to send later.",
                emptyIcon = VIcons.Clock,
                onRetry = { viewModel.load() },
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
                ) {
                    items(state.messages) { msg ->
                        ScheduledMessageCard(
                            message = msg,
                            onCancel = { viewModel.cancelScheduledMessage(msg.id) },
                            onDispatchNow = { viewModel.dispatchNow(msg.id) },
                        )
                    }
                }
            }
        }
    }

    state.infoMessage?.let {
        androidx.compose.runtime.LaunchedEffect(it) {
            viewModel.clearMessages()
        }
    }
}

@Composable
private fun StatusFilterRow(
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    val filters = listOf(
        null to "All",
        "SCHEDULED" to "Scheduled",
        "DISPATCHED" to "Dispatched",
        "FAILED" to "Failed",
        "CANCELLED" to "Cancelled",
    )
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        filters.forEach { (value, label) ->
            StatusChip(
                label = label,
                active = selected == value,
                onClick = { onSelect(value) },
            )
        }
    }
}

@Composable
private fun StatusChip(label: String, active: Boolean, onClick: () -> Unit) {
    val c = VTheme.colors
    val shape = VTheme.dimens.shapeInput
    val bg = if (active) c.teal.copy(alpha = 0.16f) else c.cream
    val fg = if (active) c.tealDeep else c.ink2
    Text(
        text = label,
        style = VTheme.type.label.colored(fg),
        modifier = Modifier
            .clip(shape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
private fun ScheduledMessageCard(
    message: ScheduledMessageDto,
    onCancel: () -> Unit,
    onDispatchNow: () -> Unit,
) {
    val c = VTheme.colors
    val (badgeText, badgeTone) = statusBadge(message.status)

    VCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = message.title ?: message.messageType,
                    style = VTheme.type.h3.colored(c.ink),
                    modifier = Modifier.weight(1f),
                )
                VBadge(text = badgeText, tone = badgeTone)
            }

            message.bodyPreview?.let {
                Text(
                    text = it,
                    style = VTheme.type.body.colored(c.ink2),
                    maxLines = 2,
                )
            }

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(VIcons.Clock, contentDescription = null, tint = c.ink3, modifier = Modifier.size(14.dp))
                Text(
                    text = formatScheduledTime(message.scheduledAt),
                    style = VTheme.type.caption.colored(c.ink3),
                )
                message.audienceLabel?.let {
                    Spacer(Modifier.size(8.dp))
                    Icon(VIcons.Users, contentDescription = null, tint = c.ink3, modifier = Modifier.size(14.dp))
                    Text(it, style = VTheme.type.caption.colored(c.ink3))
                }
            }

            if (message.status == "SCHEDULED") {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    VButton(
                        text = "Send now",
                        onClick = onDispatchNow,
                        variant = VButtonVariant.Primary,
                        size = VButtonSize.Sm,
                        modifier = Modifier.weight(1f),
                    )
                    VButton(
                        text = "Cancel",
                        onClick = onCancel,
                        variant = VButtonVariant.Ghost,
                        size = VButtonSize.Sm,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            message.lastError?.let {
                Text(
                    text = "Error: $it",
                    style = VTheme.type.caption.colored(c.dangerInk),
                )
            }
        }
    }
}

private fun statusBadge(status: String): Pair<String, VBadgeTone> = when (status) {
    "SCHEDULED" -> "Scheduled" to VBadgeTone.Arctic
    "DISPATCHING" -> "Dispatching" to VBadgeTone.Accent
    "DISPATCHED" -> "Dispatched" to VBadgeTone.Success
    "FAILED" -> "Failed" to VBadgeTone.Danger
    "CANCELLED" -> "Cancelled" to VBadgeTone.Neutral
    else -> status to VBadgeTone.Neutral
}

private fun formatScheduledTime(iso: String): String {
    return iso.replace("T", " ").removeSuffix("Z").take(16)
}
