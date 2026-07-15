package com.littlebridge.vidyaprayag.ui.screens.teacher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.feature.admin.domain.model.MessageThread
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherMessagesViewModel
import com.littlebridge.vidyaprayag.ui.components.BaseScreen
import com.littlebridge.vidyaprayag.ui.components.TeacherBottomBar
import com.littlebridge.vidyaprayag.ui.components.TeacherTab
import com.littlebridge.vidyaprayag.ui.components.VidyaPrayagCard
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TeacherMessagesScreen() {
    val viewModel: TeacherMessagesViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()

    BaseScreen(
        bottomBar = { TeacherBottomBar(selectedTab = TeacherTab.MESSAGES) }
    ) { paddingValues, scrollModifier ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@BaseScreen
        }

        if (state.error != null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        state.error!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { viewModel.refresh() }) { Text("Retry") }
                }
            }
            return@BaseScreen
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(scrollModifier)
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Messages",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (state.threads.isEmpty()) {
                item {
                    VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "No messages yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(state.threads) { thread ->
                    MessageThreadCard(thread = thread)
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun MessageThreadCard(thread: MessageThread) {
    VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    thread.senderName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    thread.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    thread.time,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                if (thread.unreadCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    BadgedBox(
                        badge = {
                            Badge {
                                Text(thread.unreadCount.toString())
                            }
                        }
                    ) {}
                }
            }
        }
    }
}
