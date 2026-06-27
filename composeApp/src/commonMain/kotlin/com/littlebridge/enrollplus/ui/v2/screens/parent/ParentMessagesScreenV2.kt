package com.littlebridge.enrollplus.ui.v2.screens.parent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.enrollplus.feature.parent.domain.model.ParentMessageDto
import com.littlebridge.enrollplus.feature.parent.domain.model.ParentMessageThreadDto
import com.littlebridge.enrollplus.feature.parent.domain.model.ParentRecipientDto
import com.littlebridge.enrollplus.feature.parent.presentation.ParentMessageViewModel
import com.littlebridge.enrollplus.ui.v2.components.VAvatar
import com.littlebridge.enrollplus.ui.v2.components.VBackHeader
import com.littlebridge.enrollplus.ui.v2.components.VIcons
import com.littlebridge.enrollplus.ui.v2.screens.VStateHost
import com.littlebridge.enrollplus.ui.v2.screens.collectAsStateV2
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * RA-51: parent Messages inbox + conversation detail. Mirror of the admin
 * [com.littlebridge.enrollplus.ui.v2.screens.school.MessagesScreenV2] but on
 * the parent endpoints. Wired to the real [ParentMessageViewModel]
 * (`GET /api/v1/parent/messages/threads`, `…/{id}/messages`, `POST /parent/messages`).
 *
 * No MockV2 — replaces the old hardcoded fake-thread stub. Three states via
 * [VStateHost] (LAW 3) for both the list and the open conversation.
 */
@Composable
fun ParentMessagesScreenV2(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ParentMessageViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()

    LaunchedEffect(Unit) { viewModel.loadThreads() }

    // Back peels layers in order: compose-new → open conversation → exit.
    val backHandler: () -> Unit = {
        when {
            state.composeOpen -> viewModel.closeCompose()
            state.openThreadId != null -> viewModel.closeThread()
            else -> onBack()
        }
    }
    val title = when {
        state.composeOpen -> "New message"
        state.openThreadId != null -> state.openThreadName.ifBlank { "Conversation" }
        else -> "Messages"
    }

    Column(modifier.fillMaxSize().statusBarsPadding()
        .imePadding()
        .navigationBarsPadding()) {
        VBackHeader(title = title, onBack = backHandler)

        ParentMessagesBody(viewModel = viewModel, modifier = Modifier.weight(1f).fillMaxWidth())
    }
}

/**
 * Phase 3 (commit 9) — the chrome-less messaging body, hosted *inside* the Conversations tab.
 *
 * Identical messaging surface as [ParentMessagesScreenV2] (inbox → conversation → compose-new
 * layers driven by the SAME [ParentMessageViewModel]), but WITHOUT the standalone status-bar
 * padding + [VBackHeader] — the Conversations hub owns that chrome. Drilling into a thread or
 * compose-new is handled by the shared VM state, so the segmented hub's back is layered by the
 * caller via [ParentMessageViewModel.composeOpen]/[ParentMessageViewModel.openThreadId].
 */
@Composable
fun ParentMessagesBody(
    viewModel: ParentMessageViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateV2()

    // P3-1: Only load if not already loaded (prevents double-fetch flash when
    // hosted inside ParentMessagesScreenV2 which also calls loadThreads).
    LaunchedEffect(Unit) {
        if (state.threads.isEmpty() && !state.loading) {
            viewModel.loadThreads()
        }
    }

    Column(modifier) {
        when {
            // RA-S07: compose-new is the topmost layer (back closes it first).
            state.composeOpen -> {
                Column(Modifier.fillMaxSize()) {
                    VBackHeader(
                        title = "New message",
                        onBack = viewModel::closeCompose,
                    )
                    ParentComposeNewContent(
                        recipients = state.composeRecipients,
                        loading = state.composeLoadingRecipients,
                        error = state.composeError,
                        isEmpty = state.composeEmpty,
                        sending = state.sending,
                        onSend = viewModel::composeNew,
                        onRetry = viewModel::openCompose,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                }
            }
            state.openThreadId != null -> {
                Column(Modifier.fillMaxSize()) {
                    VBackHeader(
                        title = state.openThreadName.ifBlank { "Conversation" },
                        onBack = viewModel::closeThread,
                    )
                    ParentConversationContent(
                        messages = state.messages,
                        loading = state.conversationLoading,
                        error = state.conversationError,
                        isEmpty = state.conversationEmpty,
                        sending = state.sending,
                        replyError = state.replyError,
                        onSend = viewModel::reply,
                        onDismissReplyError = viewModel::clearReplyError,
                        onRetry = { state.openThreadId?.let { viewModel.openThread(it, state.openThreadName) } },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                }
            }
            else -> {
                ParentThreadListContent(
                    threads = state.threads,
                    loading = state.loading,
                    error = state.error,
                    isEmpty = state.isEmpty,
                    onOpenThread = { t -> viewModel.openThread(t.id, t.senderName) },
                    onCompose = viewModel::openCompose,
                    onRetry = viewModel::loadThreads,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun ParentThreadListContent(
    threads: List<ParentMessageThreadDto>,
    loading: Boolean,
    error: String?,
    isEmpty: Boolean,
    onOpenThread: (ParentMessageThreadDto) -> Unit,
    onCompose: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    Box(modifier) {
        VStateHost(
            loading = loading,
            error = error,
            isEmpty = isEmpty,
            emptyTitle = "No messages yet",
            emptyBody = "Messages from your child's teachers and the school office will appear here.",
            emptyIcon = VIcons.Chat,
            onRetry = onRetry,
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = 0.dp,
                    vertical = 8.dp,
                ),
            ) {
                items(threads, key = { it.id }) { thread ->
                    ParentThreadRow(
                        thread = thread,
                        onClick = { onOpenThread(thread) },
                    )
                }
            }
        }

        // Floating compose-new FAB — WhatsApp-style
        val interaction = remember { MutableInteractionSource() }
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(c.accent)
                .clickable(interactionSource = interaction, indication = null, onClick = onCompose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                VIcons.Edit3,
                contentDescription = "New message",
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun ParentThreadRow(thread: ParentMessageThreadDto, onClick: () -> Unit) {
    val c = VTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Avatar with online-style ring for unread
        Box(contentAlignment = Alignment.Center) {
            VAvatar(
                name = thread.senderName.ifBlank { "?" },
                src = thread.senderImageUrl,
                size = 52.dp,
            )
        }

        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    thread.senderName,
                    style = VTheme.type.bodyStrong.colored(c.ink).copy(fontWeight = if (thread.isRead) FontWeight.SemiBold else FontWeight.Bold),
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    thread.time,
                    style = VTheme.type.caption.colored(if (thread.isRead) c.ink3 else c.accent),
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    thread.lastMessage,
                    style = VTheme.type.body.colored(if (thread.isRead) c.ink3 else c.ink2),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (thread.unreadCount > 0) {
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(c.accent)
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (thread.unreadCount > 99) "99+" else thread.unreadCount.toString(),
                            style = VTheme.type.caption.colored(Color.White).copy(fontWeight = FontWeight.Bold),
                        )
                    }
                }
            }
        }
    }
}

/**
 * RA-S07 — parent compose-NEW: pick a recipient (the child's class teacher / school office),
 * type a message, send. `onSend(recipientUserId, body)` starts a real 1:1 conversation.
 */
@Composable
private fun ParentComposeNewContent(
    recipients: List<ParentRecipientDto>,
    loading: Boolean,
    error: String?,
    isEmpty: Boolean,
    sending: Boolean,
    onSend: (String, String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    var selected by remember { mutableStateOf<ParentRecipientDto?>(null) }
    var body by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current

    Column(modifier) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            VStateHost(
                loading = loading,
                error = error,
                isEmpty = isEmpty,
                emptyTitle = "No one to message yet",
                emptyBody = "Link your child to a school to message their teachers and the office.",
                emptyIcon = VIcons.Chat,
                onRetry = onRetry,
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = 20.dp,
                        vertical = 16.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    item {
                        Text(
                            "Select recipient",
                            style = VTheme.type.label.colored(c.ink3),
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    items(recipients, key = { it.id }) { recipient ->
                        ParentRecipientRow(
                            recipient = recipient,
                            isSelected = selected?.id == recipient.id,
                            onClick = { selected = recipient },
                        )
                    }
                }
            }
        }

        ParentComposeBar(
            text = body,
            onTextChange = { body = it },
            placeholder = if (selected == null) "Pick a recipient above…" else "Message ${selected!!.name}…",
            enabled = selected != null && !sending,
            sending = sending,
            onSend = {
                val r = selected
                if (r != null && body.isNotBlank()) {
                    onSend(r.id, body.trim())
                    body = ""
                    keyboard?.hide()
                }
            },
        )
    }
}

@Composable
private fun ParentRecipientRow(recipient: ParentRecipientDto, isSelected: Boolean, onClick: () -> Unit) {
    val c = VTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val bg = if (isSelected) c.accentTint else Color.Transparent
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        VAvatar(name = recipient.name.ifBlank { "?" }, src = recipient.imageUrl, size = 48.dp)
        Column(Modifier.weight(1f)) {
            Text(
                recipient.name,
                style = VTheme.type.bodyStrong.colored(c.ink),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                recipient.subtitle,
                style = VTheme.type.caption.colored(c.ink3),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (isSelected) {
            Box(
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(c.accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    VIcons.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun ParentConversationContent(
    messages: List<ParentMessageDto>,
    loading: Boolean,
    error: String?,
    isEmpty: Boolean,
    sending: Boolean,
    replyError: String? = null,
    onSend: (String) -> Unit,
    onDismissReplyError: () -> Unit = {},
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    var reply by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(modifier) {
        // Chat surface — WhatsApp-style patterned background
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(c.accentTint),
        ) {
            VStateHost(
                loading = loading,
                error = error,
                isEmpty = isEmpty,
                emptyTitle = "No messages yet",
                emptyBody = "Send a message below to start the conversation.",
                emptyIcon = VIcons.Chat,
                onRetry = onRetry,
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = 16.dp,
                        vertical = 12.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ParentMessageBubble(msg)
                    }
                }
            }
        }

        // Inline reply error banner
        AnimatedVisibility(
            visible = replyError != null,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut(),
        ) {
            if (replyError != null) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(c.danger)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        replyError,
                        style = VTheme.type.caption.colored(c.dangerInk),
                        modifier = Modifier.weight(1f),
                    )
                    val dismissInteraction = remember { MutableInteractionSource() }
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(c.dangerInk.copy(alpha = 0.12f))
                            .clickable(interactionSource = dismissInteraction, indication = null, onClick = onDismissReplyError),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            VIcons.Close,
                            contentDescription = "Dismiss",
                            tint = c.dangerInk,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        // WhatsApp-style compose bar
        ParentComposeBar(
            text = reply,
            onTextChange = { reply = it },
            placeholder = "Type a message…",
            enabled = !sending,
            sending = sending,
            onSend = {
                if (reply.isNotBlank()) {
                    onSend(reply.trim())
                    reply = ""
                    keyboard?.hide()
                }
            },
        )
    }
}

@Composable
private fun ParentMessageBubble(msg: ParentMessageDto) {
    val c = VTheme.colors
    val isMine = msg.isMine
    val isDeleted = msg.deletedAt != null

    val bubbleColor = if (isMine) c.accent else c.card
    val textColor = if (isMine) Color.White else c.ink
    val timeColor = if (isMine) Color.White.copy(alpha = 0.7f) else c.ink3

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
    ) {
        val bubbleShape = if (isMine) {
            RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
        } else {
            RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
        }

        Column(
            Modifier
                .widthIn(max = 280.dp)
                .clip(bubbleShape)
                .background(bubbleColor)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            if (isDeleted) {
                Text(
                    "This message was deleted",
                    style = VTheme.type.body.colored(textColor).copy(
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    ),
                )
            } else {
                Text(
                    msg.body,
                    style = VTheme.type.body.colored(textColor),
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                // P2-5: Status ticks for own messages
                if (isMine && !isDeleted) {
                    when (msg.status?.uppercase()) {
                        "READ" -> {
                            Icon(
                                VIcons.Check,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.size(2.dp))
                            Icon(
                                VIcons.Check,
                                contentDescription = "Read",
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(14.dp),
                            )
                        }
                        "DELIVERED" -> {
                            Icon(
                                VIcons.Check,
                                contentDescription = "Delivered",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.size(2.dp))
                            Icon(
                                VIcons.Check,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp),
                            )
                        }
                        "SENT" -> {
                            Icon(
                                VIcons.Check,
                                contentDescription = "Sent",
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                    Spacer(Modifier.size(4.dp))
                }
                Text(
                    msg.time,
                    style = VTheme.type.caption.colored(timeColor).copy(fontSize = 10.sp),
                )
                // P2-10: Edited label
                if (msg.editedAt != null && !isDeleted) {
                    Spacer(Modifier.size(4.dp))
                    Text(
                        "edited",
                        style = VTheme.type.caption.colored(timeColor).copy(fontSize = 9.sp),
                    )
                }
            }
        }
    }
}

/**
 * Shared WhatsApp-style compose bar used by both the conversation and compose-new screens.
 * Rounded pill input with an embedded send button — no separate Send button row.
 */
@Composable
private fun ParentComposeBar(
    text: String,
    onTextChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean,
    sending: Boolean,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    val canSend = text.isNotBlank() && enabled

    Column(
        modifier
            .fillMaxWidth()
            .background(c.card),
    ) {
        // Subtle top hairline
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Text input in a rounded pill
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(c.cream)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            placeholder,
                            style = VTheme.type.body.colored(c.placeholder),
                        )
                    },
                    enabled = enabled,
                    singleLine = false,
                    maxLines = 4,
                    shape = RoundedCornerShape(22.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        disabledBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        cursorColor = c.accent,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                    keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() }),
                    textStyle = VTheme.type.body.colored(c.ink),
                )
            }

            // Circular send button — WhatsApp-style
            val sendInteraction = remember { MutableInteractionSource() }
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (canSend) c.accent else c.border2)
                    .clickable(interactionSource = sendInteraction, indication = null, enabled = canSend, onClick = onSend),
                contentAlignment = Alignment.Center,
            ) {
                if (sending) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Icon(
                        VIcons.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
