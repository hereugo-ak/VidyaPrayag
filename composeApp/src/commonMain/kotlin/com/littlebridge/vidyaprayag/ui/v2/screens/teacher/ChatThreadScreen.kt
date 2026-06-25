package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
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
// P5-T2 — Chat Thread Screen
//
// The one-on-one parent conversation. A custom top bar (back + student name +
// parent subtitle + video-call stub), a reversed message list of MessageBubbles
// (teacher right / parent left, with a sent/read double-check receipt), and a
// ReplyBar with a template affordance (→ QuickReplySheet) and a send button that
// only enables when text is present.
//
// All visuals via the Enroll.* bridge → VTheme (PrimaryIndigoSoft → primarySoft;
// no new hex; Parents↔Teacher parity per the IMPORTANT NOTE). The list is keyed.
// ─────────────────────────────────────────────────────────────────────────────

/** A single chat message. VM-agnostic UI model. */
data class ChatMessage(
    val id: String,
    val text: String,
    val timeLabel: String,        // "10:24 AM"
    val fromTeacher: Boolean,     // true = right-aligned teacher bubble
    val read: Boolean,            // teacher messages: read receipt state
)

/**
 * ChatThreadScreen — the parent conversation (P5-T2).
 *
 * @param studentName  top-bar title
 * @param parentName   top-bar subtitle
 * @param messages     newest-LAST (the LazyColumn reverses for chat order)
 * @param onBack       back arrow
 * @param onVideoCall  video-call icon stub
 * @param onSend       send the composed message text
 */
@Composable
fun ChatThreadScreen(
    studentName: String,
    parentName: String,
    messages: List<ChatMessage>,
    onBack: () -> Unit,
    onVideoCall: () -> Unit,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember { mutableStateOf("") }
    var templatesOpen by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    Column(modifier.fillMaxSize().background(Enroll.colors.surfaceBase)) {
        ChatTopBar(studentName, parentName, onBack, onVideoCall)

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            reverseLayout = true,
            contentPadding = PaddingValues(horizontal = Enroll.space.lg, vertical = Enroll.space.md),
            verticalArrangement = Arrangement.spacedBy(Enroll.space.sm),
        ) {
            items(items = messages.asReversed(), key = { it.id }) { msg ->
                MessageBubble(msg)
            }
        }

        ReplyBar(
            value = draft,
            onValueChange = { draft = it },
            onTemplates = { templatesOpen = true },
            onSend = {
                if (draft.isNotBlank()) { onSend(draft.trim()); draft = "" }
            },
        )
    }

    QuickReplySheet(
        visible = templatesOpen,
        onDismiss = { templatesOpen = false },
        onPick = { draft = it; templatesOpen = false },
    )
}

@Composable
private fun ChatTopBar(student: String, parent: String, onBack: () -> Unit, onVideoCall: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Enroll.colors.surfaceCard)
            .padding(horizontal = Enroll.space.md, vertical = Enroll.space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThreadIconButton(VIcons.ArrowLeft, "Back", Enroll.colors.textPrimary, onBack)
        Spacer(Modifier.width(Enroll.space.sm))
        Column(Modifier.weight(1f)) {
            Text(text = student, style = Enroll.type.labelBold.colored(Enroll.colors.textPrimary), maxLines = 1)
            Text(text = parent, style = Enroll.type.bodySmall.colored(Enroll.colors.textSecondary), maxLines = 1)
        }
        ThreadIconButton(VIcons.Phone, "Video call", Enroll.colors.primaryMid, onVideoCall)
    }
}

@Composable
private fun ThreadIconButton(icon: ImageVector, cd: String, tint: Color, onClick: () -> Unit) {
    val ix = remember { MutableInteractionSource() }
    Box(
        Modifier
            .pressScale(ix)
            .size(36.dp)
            .clip(Enroll.shape.pill)
            .clickable(interactionSource = ix, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = cd, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun MessageBubble(msg: ChatMessage) {
    val teacher = msg.fromTeacher
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (teacher) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(Enroll.shape.card)
                .background(if (teacher) Enroll.colors.primarySoft else Enroll.colors.surfaceSubtle)
                .padding(horizontal = Enroll.space.md, vertical = Enroll.space.sm),
        ) {
            Text(
                text = msg.text,
                style = Enroll.type.bodyMedium.colored(
                    if (teacher) Enroll.colors.textPrimary else Enroll.colors.textSecondary,
                ),
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = msg.timeLabel,
                    style = Enroll.type.bodySmall.colored(Enroll.colors.textTertiary),
                )
                if (teacher) {
                    Spacer(Modifier.width(Enroll.space.xs))
                    // Read receipt — check icon, primary when read, tertiary when only sent.
                    Icon(
                        imageVector = VIcons.Check,
                        contentDescription = if (msg.read) "Read" else "Sent",
                        tint = if (msg.read) Enroll.colors.primary else Enroll.colors.textTertiary,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReplyBar(
    value: String,
    onValueChange: (String) -> Unit,
    onTemplates: () -> Unit,
    onSend: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Enroll.colors.surfaceCard)
            .padding(horizontal = Enroll.space.md, vertical = Enroll.space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThreadIconButton(VIcons.FileText, "Templates", Enroll.colors.primaryMid, onTemplates)
        Spacer(Modifier.width(Enroll.space.sm))
        Box(
            Modifier
                .weight(1f)
                .clip(Enroll.shape.pill)
                .background(Enroll.colors.surfaceSubtle)
                .padding(horizontal = Enroll.space.lg, vertical = Enroll.space.md),
        ) {
            if (value.isEmpty()) {
                Text(
                    text = "Write a message…",
                    style = Enroll.type.bodyMedium.colored(Enroll.colors.textTertiary),
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = Enroll.type.bodyMedium.colored(Enroll.colors.textPrimary),
                cursorBrush = SolidColor(Enroll.colors.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.width(Enroll.space.sm))
        val enabled = value.isNotBlank()
        val ix = remember { MutableInteractionSource() }
        Box(
            Modifier
                .pressScale(ix)
                .size(40.dp)
                .clip(Enroll.shape.pill)
                .background(if (enabled) Enroll.colors.primary else Enroll.colors.surfaceSubtle)
                .clickable(interactionSource = ix, indication = null, enabled = enabled, onClick = onSend),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                VIcons.Send,
                contentDescription = "Send",
                tint = if (enabled) Enroll.colors.onPrimary else Enroll.colors.textTertiary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** The four pre-written reply templates from the spec (+ an add-custom row). */
private val QUICK_REPLIES = listOf(
    "Your child was absent today.",
    "Please check the homework submitted.",
    "Your child's performance has improved.",
    "Please schedule a meeting with me.",
)

@Composable
private fun QuickReplySheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    if (!visible) return
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Enroll.shape.sheet)
                .background(Enroll.colors.surfaceBase)
                .padding(horizontal = Enroll.space.lg)
                .padding(bottom = Enroll.space.xxl),
        ) {
            Spacer(Modifier.height(Enroll.space.md))
            SectionHeader(title = "QUICK REPLIES")
            Spacer(Modifier.height(Enroll.space.md))
            QUICK_REPLIES.forEach { reply ->
                EnrollCard(modifier = Modifier.fillMaxWidth(), onClick = { onPick(reply) }, padding = Enroll.space.md) {
                    Text(text = reply, style = Enroll.type.bodyMedium.colored(Enroll.colors.textPrimary))
                }
                Spacer(Modifier.height(Enroll.space.sm))
            }
            // Add-custom affordance (host opens its own composer; stub here).
            val ix = remember { MutableInteractionSource() }
            Text(
                text = "+ Add custom template",
                style = Enroll.type.labelBold.colored(Enroll.colors.primaryMid),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Enroll.shape.pill)
                    .clickable(interactionSource = ix, indication = null, onClick = {})
                    .padding(vertical = Enroll.space.md),
            )
        }
    }
}
