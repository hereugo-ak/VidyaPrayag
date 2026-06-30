package com.littlebridge.enrollplus.ui.v2.screens.school

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
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.littlebridge.enrollplus.feature.admin.domain.model.Message
import com.littlebridge.enrollplus.feature.admin.domain.model.MessageThread
import com.littlebridge.enrollplus.feature.admin.presentation.ComposeState
import com.littlebridge.enrollplus.feature.admin.presentation.ConversationState
import com.littlebridge.enrollplus.feature.admin.presentation.MessageRecipient
import com.littlebridge.enrollplus.feature.admin.presentation.MessagesState
import com.littlebridge.enrollplus.feature.admin.presentation.MessagesViewModel
import com.littlebridge.enrollplus.ui.v2.components.VAvatar
import com.littlebridge.enrollplus.ui.v2.components.VBackHeader
import com.littlebridge.enrollplus.ui.v2.components.VIcons
import com.littlebridge.enrollplus.ui.v2.screens.VStateHost
import com.littlebridge.enrollplus.ui.v2.screens.collectAsStateV2
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * MessagesScreenV2 — admin Messages inbox + conversation detail.
 *
 * Wired to the real [MessagesViewModel] (`GET /api/v1/school/messages/threads`,
 * `POST /api/v1/school/messages`, `POST /threads/{id}/read`).
 *
 * Two modes:
 *   • Thread list (default) — VCard rows with avatar, sender name/role, last message,
 *     time, unread VBadge (Arctic tone).
 *   • Conversation detail (when [ConversationState.threadId] is non-null) — bubble
 *     messages (mine = teal-tint, theirs = cream), bottom VInput + Send VButton.
 *
 * Rendered as a portal overlay; back chevron returns to [SchoolPortalV2] tabs.
 * No MockV2. Three states via [VStateHost] (LAW 3).
 */
@Composable
fun MessagesScreenV2(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: MessagesViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    val isLoading by viewModel.isLoading.collectAsStateV2()
    val errorMessage by viewModel.errorMessage.collectAsStateV2()
    val conversation by viewModel.conversation.collectAsStateV2()
    val compose by viewModel.compose.collectAsStateV2()

    // RA-S07: the compose-new sheet is the topmost layer — back closes it first.
    if (compose.isOpen) {
        ComposeNewContent(
            compose = compose,
            isSending = state.isSending,
            onSend = viewModel::composeNew,
            onClose = viewModel::closeCompose,
            modifier = modifier.fillMaxSize(),
        )
        return
    }

    // If a conversation is open, back closes it first; otherwise back exits the overlay.
    val backHandler: () -> Unit = {
        if (conversation.threadId != null) viewModel.closeConversation() else onBack()
    }

    val title = if (conversation.threadId != null) {
        conversation.senderName.ifBlank { "Conversation" }
    } else {
        "Messages"
    }

    Column(modifier.fillMaxSize().statusBarsPadding()
        .imePadding()
        .navigationBarsPadding()) {
        VBackHeader(title = title, onBack = backHandler)

        if (conversation.threadId != null) {
            ConversationContent(
                conversation = conversation,
                onSend = viewModel::sendReply,
                onClearError = viewModel::clearConversationError,
                onMarkRead = viewModel::markAsRead,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            ThreadListContent(
                state = state,
                isLoading = isLoading,
                error = errorMessage,
                onOpenThread = { threadId ->
                    viewModel.markAsRead(threadId)
                    viewModel.openConversation(threadId)
                },
                onRetry = viewModel::refresh,
                onClearError = viewModel::clearError,
                onCompose = viewModel::openCompose,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ThreadListContent(
    state: MessagesState,
    isLoading: Boolean,
    error: String?,
    onOpenThread: (String) -> Unit,
    onRetry: () -> Unit,
    onClearError: () -> Unit,
    onCompose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    Box(modifier) {
        VStateHost(
            loading = isLoading,
            error = error,
            isEmpty = state.threads.isEmpty(),
            emptyTitle = "No messages yet",
            emptyBody = "Your inbox will populate as parents and teachers reach out.",
            emptyIcon = VIcons.Chat,
            onRetry = onRetry,
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 0.dp,
                    end = 0.dp,
                    top = 8.dp,
                    bottom = 80.dp,
                ),
            ) {
                items(state.threads, key = { it.id }) { thread ->
                    ThreadRow(thread = thread, onClick = { onOpenThread(thread.id) })
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
private fun ThreadRow(thread: MessageThread, onClick: () -> Unit) {
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
        VAvatar(name = thread.senderName.ifBlank { "?" }, src = thread.senderImageUrl, size = 52.dp)
        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    thread.senderName,
                    style = VTheme.type.bodyStrong.colored(c.ink)
                        .copy(fontWeight = if (thread.isRead) FontWeight.SemiBold else FontWeight.Bold),
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

@Composable
private fun ConversationContent(
    conversation: ConversationState,
    onSend: (String) -> Unit,
    onClearError: () -> Unit,
    onMarkRead: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    var reply by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()

    // P1-13: Mark thread as read on open
    LaunchedEffect(conversation.threadId) {
        conversation.threadId?.let { onMarkRead(it) }
    }

    // P1-9: Auto-scroll to bottom when new messages arrive
    LaunchedEffect(conversation.messages.size) {
        if (conversation.messages.isNotEmpty()) {
            listState.animateScrollToItem(conversation.messages.lastIndex)
        }
    }

    Column(modifier) {
        // Chat surface
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(c.accentTint),
        ) {
            VStateHost(
                loading = conversation.isLoading,
                error = conversation.error,
                isEmpty = conversation.messages.isEmpty(),
                emptyTitle = "No messages yet",
                emptyBody = "Start the conversation by sending a message below.",
                emptyIcon = VIcons.Chat,
                onRetry = onClearError,
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
                    itemsIndexed(conversation.messages, key = { _, msg -> msg.id }) { index, msg ->
                        // P1-7: Date header when date changes from previous message
                        val showDateHeader = index == 0 ||
                            !msg.time.contentEquals(conversation.messages[index - 1].time, true)
                        // P1-8: Message grouping — tighter spacing for consecutive same-sender
                        val isGroupStart = index == 0 ||
                            conversation.messages[index - 1].senderId != msg.senderId

                        if (showDateHeader && msg.time.isNotBlank()) {
                            DateHeader(date = msg.time)
                        }
                        MessageBubble(
                            msg = msg,
                            isGroupStart = isGroupStart,
                        )
                    }
                }
            }
        }

        // WhatsApp-style compose bar (P1-10: unified with parent)
        SharedComposeBar(
            text = reply,
            onTextChange = { reply = it },
            placeholder = "Type a message…",
            enabled = !conversation.isSending,
            sending = conversation.isSending,
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

/**
 * RA-S07 — compose-new screen: pick a recipient (a teacher in the school), type a message, send.
 * `onSend(recipientUserId, body)` starts a real 1:1 conversation via the two-row engine.
 */
@Composable
private fun ComposeNewContent(
    compose: ComposeState,
    isSending: Boolean,
    onSend: (String, String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    var selected by remember { mutableStateOf<MessageRecipient?>(null) }
    var body by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current

    Column(modifier.statusBarsPadding().imePadding().navigationBarsPadding()) {
        VBackHeader(title = "New message", onBack = onClose)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            VStateHost(
                loading = compose.isLoadingRecipients,
                error = compose.error,
                isEmpty = compose.candidates.isEmpty(),
                emptyTitle = "No recipients",
                emptyBody = "Add teachers or enroll students with parents to start a conversation.",
                emptyIcon = VIcons.Chat,
                onRetry = onClose,
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    item {
                        Text(
                            "Select recipient",
                            style = VTheme.type.label.colored(c.ink3),
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    items(compose.candidates, key = { it.id }) { recipient ->
                        RecipientRow(
                            recipient = recipient,
                            isSelected = selected?.id == recipient.id,
                            onClick = { selected = recipient },
                        )
                    }
                }
            }
        }

        // WhatsApp-style compose bar (P1-10: unified)
        SharedComposeBar(
            text = body,
            onTextChange = { body = it },
            placeholder = if (selected == null) "Pick a recipient above…" else "Message ${selected!!.name}…",
            enabled = selected != null && !isSending,
            sending = isSending,
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
private fun RecipientRow(recipient: MessageRecipient, isSelected: Boolean, onClick: () -> Unit) {
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
private fun MessageBubble(msg: Message, isGroupStart: Boolean = true) {
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
            RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isGroupStart) 18.dp else 4.dp,
                bottomEnd = 4.dp,
            )
        } else {
            RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = 4.dp,
                bottomEnd = if (isGroupStart) 18.dp else 4.dp,
            )
        }

        Column(
            Modifier
                .widthIn(max = 280.dp)
                .clip(bubbleShape)
                .background(bubbleColor)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            if (isDeleted) {
                // P2-10: Tombstone display
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

/** P1-7: Date header for message grouping by date. */
@Composable
private fun DateHeader(date: String) {
    val c = VTheme.colors
    Box(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(c.border2)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                date,
                style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 11.sp),
            )
        }
    }
}

/**
 * P1-10: Shared WhatsApp-style compose bar — unified between admin and parent screens.
 * Rounded pill input with an embedded circular send button.
 */
@Composable
private fun SharedComposeBar(
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
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
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
