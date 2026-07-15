package com.littlebridge.vidyaprayag.ui.screens.teacher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.TeacherHomework
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherHomeworkViewModel
import com.littlebridge.vidyaprayag.navigation.LocalAppNavigator
import com.littlebridge.vidyaprayag.ui.components.BaseScreen
import com.littlebridge.vidyaprayag.ui.components.VidyaPrayagCard
import com.littlebridge.vidyaprayag.ui.components.VidyaPrayagPrimaryButton
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherHomeworkScreen() {
    val viewModel: TeacherHomeworkViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val navigator = LocalAppNavigator.current
    var showCreateDialog by remember { mutableStateOf(false) }

    BaseScreen(
        onBackClick = { navigator.goBack() },
        immersiveTopBar = true
    ) { paddingValues, scrollModifier ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().then(scrollModifier),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Homework",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            FloatingActionButton(
                                onClick = { showCreateDialog = true },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Create homework")
                            }
                        }
                    }

                    if (state.error != null) {
                        item {
                            Text(
                                state.error!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    if (state.homework.isEmpty()) {
                        item {
                            VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Assignment,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "No homework assigned yet",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    VidyaPrayagPrimaryButton(
                                        text = "Create homework",
                                        onClick = { showCreateDialog = true }
                                    )
                                }
                            }
                        }
                    } else {
                        items(state.homework) { hw ->
                            HomeworkCard(hw = hw)
                        }
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        if (showCreateDialog) {
            CreateHomeworkBottomSheet(
                isCreating = state.isCreating,
                onDismiss = {
                    showCreateDialog = false
                    viewModel.clearError()
                },
                onCreate = { title, subject, className, description, dueDate ->
                    viewModel.createHomework(
                        title = title,
                        subject = subject,
                        className = className,
                        description = description,
                        dueDate = dueDate,
                        onCreated = { showCreateDialog = false }
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateHomeworkBottomSheet(
    isCreating: Boolean,
    onDismiss: () -> Unit,
    onCreate: (title: String, subject: String, className: String, description: String, dueDate: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var className by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var titleError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Create Homework",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    titleError = if (it.trim().length < 3) "Title must be at least 3 characters" else null
                },
                label = { Text("Title (min 3 characters)") },
                singleLine = true,
                isError = titleError != null,
                supportingText = {
                    if (titleError != null) {
                        Text(titleError!!, color = MaterialTheme.colorScheme.error)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text("Subject") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = className,
                onValueChange = { className = it },
                label = { Text("Class") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = dueDate,
                onValueChange = { dueDate = it },
                label = { Text("Due date (YYYY-MM-DD)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    enabled = !isCreating
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val trimmedTitle = title.trim()
                        if (trimmedTitle.length < 3) {
                            titleError = "Title must be at least 3 characters"
                            return@Button
                        }
                        onCreate(trimmedTitle, subject.trim(), className.trim(), description.trim(), dueDate.trim())
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isCreating && title.trim().length >= 3 && subject.isNotBlank() && className.isNotBlank()
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Create")
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeworkCard(hw: TeacherHomework) {
    VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                hw.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${hw.className} - ${hw.subject}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (hw.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    hw.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Due: ${hw.dueDate}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
