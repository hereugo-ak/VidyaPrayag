package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.ui.v2.components.EnrollCard
import com.littlebridge.vidyaprayag.ui.v2.components.SectionHeader
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatar
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VInput
import com.littlebridge.vidyaprayag.ui.v2.theme.Enroll
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

// ─────────────────────────────────────────────────────────────────────────────
// P5-T1 — Chat List, organized by class
//
// The portal's signature THREAD-FIRST chat (Design Spec §SIGNATURE #4): teachers
// don't think "last messaged", they think "Class 8B → Rohan". So the list is
// grouped by class, NOT a flat WhatsApp-style recency feed.
//
// Top: a single-line search bar (surfaceSubtle bg) over parent/student names.
// Below: a LazyColumn of class groups — each a SectionHeader(className) + its
// ParentThreadRows (student avatar, student + parent name, last-message preview,
// timestamp, unread count pill, and a category badge chip). Tap → ChatThreadScreen.
//
// All visuals via the Enroll.* bridge → VTheme (no new hex; Parents↔Teacher
// parity per the IMPORTANT NOTE). Items are keyed; rows read from a VM.
// ─────────────────────────────────────────────────────────────────────────────

/** The conversation category — drives the badge chip colour. */
enum class ChatCategory { Academic, Attendance, Behavioral, General }

/** One parent conversation thread. VM-agnostic UI model. */
data class ParentChatThread(
    val id: String,                 // threadId
    val className: String,          // group key, e.g. "Class 9B"
    val studentName: String,        // "Rohan Sharma"
    val parentName: String,         // "Mr. Sharma (Father)"
    val avatarUrl: String?,         // null → initials fallback
    val lastMessage: String,        // preview (1 line)
    val timeLabel: String,          // "10:24 AM" / "Yesterday"
    val unreadCount: Int,
    val category: ChatCategory,
)

/**
 * TeacherChatScreen — the class-grouped chat list (P5-T1).
 *
 * @param threads      all parent threads (any class); grouped by `className` here.
 * @param onOpenThread tap a row → navigate to [ChatThreadScreen](threadId).
 */
@Composable
fun TeacherChatScreen(
    threads: List<ParentChatThread>,
    onOpenThread: (threadId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }

    val filtered = remember(threads, query) {
        if (query.isBlank()) threads
        else threads.filter {
            it.studentName.contains(query, ignoreCase = true) ||
                it.parentName.contains(query, ignoreCase = true)
        }
    }
    val grouped = filtered.groupBy { it.className }

    Column(modifier.fillMaxSize().background(Enroll.colors.surfaceBase)) {
        Box(Modifier.fillMaxWidth().padding(horizontal = Enroll.space.lg, vertical = Enroll.space.md)) {
            VInput(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search parent or student…",
                leadingIcon = VIcons.Search,
            )
        }

        if (grouped.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(Enroll.space.xxl), contentAlignment = Alignment.Center) {
                Text(
                    text = if (query.isBlank()) "No conversations yet." else "No matches for \"$query\".",
                    style = Enroll.type.bodyMedium.colored(Enroll.colors.textTertiary),
                )
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = Enroll.space.lg, vertical = Enroll.space.sm),
            verticalArrangement = Arrangement.spacedBy(Enroll.space.sm),
        ) {
            grouped.forEach { (className, rows) ->
                item(key = "hdr-$className") {
                    Spacer(Modifier.height(Enroll.space.sm))
                    SectionHeader(title = className)
                    Spacer(Modifier.height(Enroll.space.xs))
                }
                items(items = rows, key = { it.id }) { thread ->
                    ParentThreadRow(thread = thread, onClick = { onOpenThread(thread.id) })
                }
            }
        }
    }
}

@Composable
private fun ParentThreadRow(thread: ParentChatThread, onClick: () -> Unit) {
    EnrollCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        padding = Enroll.space.md,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VAvatar(name = thread.studentName, src = thread.avatarUrl, size = 40.dp)
            Spacer(Modifier.width(Enroll.space.md))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = thread.studentName,
                        style = Enroll.type.labelBold.colored(Enroll.colors.textPrimary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(Enroll.space.sm))
                    CategoryBadge(thread.category)
                }
                Text(
                    text = thread.parentName,
                    style = Enroll.type.bodySmall.colored(Enroll.colors.textSecondary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(Enroll.space.xs))
                Text(
                    text = thread.lastMessage,
                    style = Enroll.type.bodyMedium.colored(Enroll.colors.textSecondary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(Enroll.space.md))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = thread.timeLabel,
                    style = Enroll.type.bodySmall.colored(Enroll.colors.textTertiary),
                )
                Spacer(Modifier.height(Enroll.space.xs))
                if (thread.unreadCount > 0) UnreadPill(thread.unreadCount)
            }
        }
    }
}

/** The unread-count pill — primary fill, white text. */
@Composable
private fun UnreadPill(count: Int) {
    Box(
        Modifier
            .clip(Enroll.shape.pill)
            .background(Enroll.colors.primary)
            .padding(horizontal = Enroll.space.sm, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            style = Enroll.type.labelCaps.colored(Enroll.colors.onPrimary),
        )
    }
}

/** Category chip — soft-tinted by type (Academic/Attendance/Behavioral/General). */
@Composable
private fun CategoryBadge(category: ChatCategory) {
    val (label, ink, fill) = when (category) {
        ChatCategory.Academic -> Triple("Academic", Enroll.colors.primaryMid, Enroll.colors.primarySoft)
        ChatCategory.Attendance -> Triple("Attendance", Enroll.colors.statusPresent, Enroll.colors.statusPresentSoft)
        ChatCategory.Behavioral -> Triple("Behavioral", Enroll.colors.accent, Enroll.colors.accentSoft)
        ChatCategory.General -> Triple("General", Enroll.colors.textSecondary, Enroll.colors.surfaceSubtle)
    }
    Text(
        text = label,
        style = Enroll.type.labelCaps.colored(ink),
        modifier = Modifier
            .clip(Enroll.shape.chip)
            .background(fill)
            .padding(horizontal = Enroll.space.sm, vertical = 2.dp),
    )
}
