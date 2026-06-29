package com.littlebridge.enrollplus.ui.v2.screens.tutor

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.enrollplus.feature.tutor.presentation.ChatMessage
import com.littlebridge.enrollplus.feature.tutor.presentation.TutorChatViewModel
import com.littlebridge.enrollplus.ui.v2.components.VBackHeader
import com.littlebridge.enrollplus.ui.v2.components.VButton
import com.littlebridge.enrollplus.ui.v2.components.VButtonSize
import com.littlebridge.enrollplus.ui.v2.components.VButtonTone
import com.littlebridge.enrollplus.ui.v2.components.VButtonVariant
import com.littlebridge.enrollplus.ui.v2.components.VEmptyState
import com.littlebridge.enrollplus.ui.v2.components.VIcons
import com.littlebridge.enrollplus.ui.v2.screens.collectAsStateV2
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * TutorChatScreen — the Socratic doubt UI.
 *
 * Children ask questions and get grounded, Socratic guidance back.
 * Shows conversation history with user/tutor message bubbles.
 * The tutor never gives direct answers — it guides step by step.
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §6.3 (TutorTurn)
 */
@Composable
fun TutorChatScreen(
    onBack: () -> Unit = {},
    subjectId: String = "",
    modifier: Modifier = Modifier,
    viewModel: TutorChatViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    val c = VTheme.colors
    val listState = rememberLazyListState()

    LaunchedEffect(subjectId) {
        if (subjectId.isNotBlank()) viewModel.updateSubject(subjectId)
    }

    LaunchedEffect(state.conversationHistory.size) {
        if (state.conversationHistory.isNotEmpty()) {
            listState.animateScrollToItem(state.conversationHistory.lastIndex)
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .background(c.background)
    ) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            VBackHeader(
                title = "AI Tutor",
                onBack = onBack,
                action = {
                    if (state.conversationHistory.isNotEmpty()) {
                        Text(
                            "Clear",
                            style = VTheme.type.caption.colored(c.accent),
                            modifier = Modifier.clickable { viewModel.clearConversation() },
                        )
                    }
                },
            )

            if (state.error != null) {
                VEmptyState(
                    title = "Error",
                    body = state.error!!,
                    icon = VIcons.AlertTriangle,
                    modifier = Modifier.fillMaxSize(),
                )
            } else if (state.conversationHistory.isEmpty() && !state.isLoading) {
                VEmptyState(
                    title = "Ask a question",
                    body = "Type your doubt below and the AI tutor will guide you step by step.",
                    icon = VIcons.BookOpen,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
                ) {
                    items(state.conversationHistory) { msg ->
                        ChatBubble(msg)
                    }
                    if (state.isLoading) {
                        item {
                            Row(
                                Modifier.fillMaxWidth().padding(8.dp),
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                CircularProgressIndicator(color = c.teal, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }

            if (state.subjectId.isBlank()) {
                Box(
                    Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Select a subject first",
                        style = VTheme.type.caption.colored(c.ink3),
                    )
                }
            } else {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(c.card)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = state.question,
                        onValueChange = viewModel::updateQuestion,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Type your doubt...", style = VTheme.type.caption.colored(c.placeholder)) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = c.accent,
                            unfocusedBorderColor = c.border2,
                        ),
                        maxLines = 3,
                    )
                    VButton(
                        text = "Ask",
                        modifier = Modifier.fillMaxWidth(),
                        size = VButtonSize.Md,
                        variant = VButtonVariant.Primary,
                        tone = VButtonTone.Lavender,
                        enabled = state.question.isNotBlank() && !state.isLoading,
                        onClick = viewModel::askDoubt,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(msg: ChatMessage) {
    val c = VTheme.colors
    val isUser = msg.role == "user"

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp,
                    )
                )
                .background(if (isUser) c.accent else c.card)
                .padding(12.dp)
                .fillMaxWidth(0.85f),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    msg.text,
                    style = VTheme.type.body.colored(if (isUser) c.card else c.ink),
                )
                if (msg.nextPrompt != null) {
                    Text(
                        msg.nextPrompt!!,
                        style = VTheme.type.caption.colored(if (isUser) c.accentTint else c.accent),
                        fontWeight = FontWeight.Medium,
                    )
                }
                if (msg.isPractice && msg.practiceQuestions != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Practice questions ready!",
                        style = VTheme.type.caption.colored(if (isUser) c.accentTint else c.teal),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}
