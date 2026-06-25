package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.littlebridge.vidyaprayag.ui.v2.components.SectionHeader
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.theme.Enroll
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

// ─────────────────────────────────────────────────────────────────────────────
// P2-T7 — Notification Bottom Sheet
//
// The bell-icon destination: a bottom-anchored sheet of the teacher's recent
// notifications, grouped TODAY / YESTERDAY / EARLIER, each row swipeable-right to
// dismiss and tappable to deep-link into the relevant screen (P7 router).
//
// The portal has no Material3 ModalBottomSheet in use; to stay on-pattern (and clear
// the "no default components" bar) this is a custom sheet built on the same
// `androidx.compose.ui.window.Dialog` primitive the portal already uses for dialogs,
// styled with ShapeSheet top corners + a handle bar. All tokens via Enroll.*.
// ─────────────────────────────────────────────────────────────────────────────

/** What a notification is about — drives its icon + accent. */
enum class NotificationType { Grade, Attendance, Message, Announcement, Homework, General }

/** Recency bucket the sheet groups rows under. */
enum class NotificationGroup(val label: String) {
    Today("TODAY"), Yesterday("YESTERDAY"), Earlier("EARLIER")
}

/** One row in the notification sheet. `timeLabel` is pre-formatted (e.g. "9:14 AM"). */
data class TeacherNotification(
    val id: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val timeLabel: String,
    val group: NotificationGroup,
    val unread: Boolean = false,
)

private fun NotificationType.icon(): ImageVector = when (this) {
    NotificationType.Grade -> VIcons.GraduationCap
    NotificationType.Attendance -> VIcons.Check
    NotificationType.Message -> VIcons.Chat
    NotificationType.Announcement -> VIcons.Megaphone
    NotificationType.Homework -> VIcons.ClipboardList
    NotificationType.General -> VIcons.Bell
}

/**
 * NotificationSheet — bottom sheet triggered from the Home bell.
 *
 * @param visible        controls show/hide (returns early when false)
 * @param notifications  VM-owned list, already sorted newest-first within groups
 * @param onDismiss      close the sheet (scrim tap / back)
 * @param onMarkAllRead  "Mark all read" header action
 * @param onOpen         tap a row → close + navigate (P7 router resolves the type)
 * @param onDismissItem  swipe-right a row → remove it (id passed back)
 */
@Composable
fun NotificationSheet(
    visible: Boolean,
    notifications: List<TeacherNotification>,
    onDismiss: () -> Unit,
    onMarkAllRead: () -> Unit,
    onOpen: (TeacherNotification) -> Unit,
    onDismissItem: (String) -> Unit,
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
            // Handle bar — 32dp × 4dp, centered, SurfaceSubtle.
            Box(
                modifier = Modifier
                    .padding(vertical = Enroll.space.md)
                    .align(Alignment.CenterHorizontally)
                    .width(32.dp)
                    .height(4.dp)
                    .clip(Enroll.shape.pill)
                    .background(Enroll.colors.surfaceSubtle),
            )

            SectionHeader(
                title = "NOTIFICATIONS",
                action = "Mark all read",
                onAction = onMarkAllRead,
            )
            Spacer(Modifier.height(Enroll.space.sm))

            if (notifications.isEmpty()) {
                NotificationEmpty()
            } else {
                val grouped = NotificationGroup.entries.mapNotNull { g ->
                    notifications.filter { it.group == g }
                        .takeIf { it.isNotEmpty() }
                        ?.let { g to it }
                }

                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
                    grouped.forEach { (group, rows) ->
                        item(key = "h-${group.name}") {
                            Text(
                                text = group.label,
                                style = Enroll.type.labelCaps.colored(Enroll.colors.textTertiary),
                                modifier = Modifier.padding(
                                    top = Enroll.space.md,
                                    bottom = Enroll.space.sm,
                                ),
                            )
                        }
                        items(rows, key = { it.id }) { n ->
                            NotificationRow(
                                notification = n,
                                onOpen = { onOpen(n) },
                                onDismissItem = { onDismissItem(n.id) },
                            )
                            Spacer(Modifier.height(Enroll.space.sm))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(
    notification: TeacherNotification,
    onOpen: () -> Unit,
    onDismissItem: () -> Unit,
) {
    var offsetX by remember { mutableStateOf(0f) }
    val animatedOffset by animateFloatAsState(targetValue = offsetX, label = "swipe")
    // Swipe RIGHT past a threshold dismisses the row.
    val dismissThresholdPx = 220f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { translationX = animatedOffset }
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    offsetX = (offsetX + delta).coerceAtLeast(0f)
                },
                onDragStopped = {
                    if (offsetX > dismissThresholdPx) onDismissItem() else offsetX = 0f
                },
            )
            .clip(Enroll.shape.card)
            .background(Enroll.colors.surfaceCard)
            .clickable(onClick = onOpen)
            .padding(Enroll.space.lg),
        verticalAlignment = Alignment.Top,
    ) {
        // Unread dot on the left edge.
        Box(
            modifier = Modifier
                .padding(top = Enroll.space.xs, end = Enroll.space.sm)
                .size(8.dp)
                .clip(CircleShape)
                .background(if (notification.unread) Enroll.colors.primary else Color.Transparent),
        )

        // Type icon disc.
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Enroll.colors.primarySoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = notification.type.icon(),
                contentDescription = null,
                tint = Enroll.colors.primaryMid,
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(Modifier.width(Enroll.space.md))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = notification.title,
                style = Enroll.type.labelBold.colored(Enroll.colors.textPrimary),
            )
            Spacer(Modifier.height(Enroll.space.xs))
            Text(
                text = notification.message,
                style = Enroll.type.bodyMedium.colored(Enroll.colors.textSecondary),
                maxLines = 3,
            )
            Spacer(Modifier.height(Enroll.space.xs))
            Text(
                text = notification.timeLabel,
                style = Enroll.type.bodySmall.colored(Enroll.colors.textTertiary),
            )
        }
    }
}

@Composable
private fun NotificationEmpty() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Enroll.space.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Enroll.space.md),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Enroll.colors.surfaceSubtle),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = VIcons.Bell,
                contentDescription = null,
                tint = Enroll.colors.textTertiary,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = "You're all caught up",
            style = Enroll.type.labelBold.colored(Enroll.colors.textSecondary),
        )
    }
}
