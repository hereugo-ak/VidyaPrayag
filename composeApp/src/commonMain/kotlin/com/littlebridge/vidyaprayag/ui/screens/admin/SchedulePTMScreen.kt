package com.littlebridge.vidyaprayag.ui.screens.admin

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.feature.admin.presentation.ClassPTMProgress
import com.littlebridge.vidyaprayag.feature.admin.presentation.PTMHistoryItem
import com.littlebridge.vidyaprayag.feature.admin.presentation.SchedulePTMViewModel
import com.littlebridge.vidyaprayag.navigation.LocalAppNavigator
import com.littlebridge.vidyaprayag.ui.components.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulePTMScreen() {
    val viewModel: SchedulePTMViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val navigator = LocalAppNavigator.current
    var showScheduleDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadPtm() }

    BaseScreen(
        onBackClick = { navigator.goBack() },
        immersiveTopBar = true
    ) { paddingValues, scrollModifier ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(scrollModifier)
                .padding(paddingValues),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            item {
                PrimaryActionSection(onClick = { showScheduleDialog = true })
            }

            state.errorMessage?.let { msg ->
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(msg, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                            TextButton(onClick = { viewModel.clearMessages() }) { Text("Dismiss") }
                        }
                    }
                }
            }

            item {
                ActiveEventSection(
                    title = state.activeEventTitle,
                    date = state.activeEventDate,
                    slot = state.activeEventSlot
                )
            }

            item {
                LivePulseSection(
                    expected = state.expectedParents,
                    checkedIn = state.checkedInParents
                )
            }

            item {
                LiveCommunicationCard(
                    invites = state.invitesDelivered,
                    receipts = state.readReceipts
                )
            }

            item {
                HistorySection(state.history)
            }

            item {
                ClassProgressSection(state.classProgress)
            }

            item {
                LiveSyncFooter()
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        if (showScheduleDialog) {
            SchedulePtmDialog(
                isCreating = state.isCreating,
                onDismiss = { showScheduleDialog = false },
                onCreate = { title, date, slot ->
                    viewModel.createPtm(
                        title = title,
                        date = date,
                        slot = slot,
                        onCreated = { showScheduleDialog = false }
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SchedulePtmDialog(
    isCreating: Boolean,
    onDismiss: () -> Unit,
    onCreate: (title: String, date: String, slot: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var slot by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        title = { Text("Schedule PTM", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (e.g. Q3 Parent-Teacher Meet)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCreating
                )
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCreating
                )
                OutlinedTextField(
                    value = slot,
                    onValueChange = { slot = it },
                    label = { Text("Slot (e.g. 10:00 AM - 12:00 PM)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCreating
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !isCreating && title.isNotBlank() && date.isNotBlank() && slot.isNotBlank(),
                onClick = { onCreate(title.trim(), date.trim(), slot.trim()) }
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Schedule")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isCreating) { Text("Cancel") }
        }
    )
}

@Composable
private fun PrimaryActionSection(onClick: () -> Unit = {}) {
    VidyaPrayagCard(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.clickable(onClick = onClick).padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AddCircle, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(28.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Schedule Next PTM", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Plan upcoming parent-teacher meets", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun ActiveEventSection(title: String, date: String, slot: String) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val infiniteTransition = rememberInfiniteTransition(label = "")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 0.4f,
                    animationSpec = infiniteRepeatable(animation = tween(1500), repeatMode = RepeatMode.Reverse),
                    label = ""
                )
                Box(modifier = Modifier.size(10.dp).alpha(alpha).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
                Text("Active Event", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = { }) {
                Icon(Icons.Default.Settings, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Manage", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
            }
        }

        VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Surface(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                        Text("IN PROGRESS", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
                    }
                    Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Medium)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    EventMetaItem(label = "TODAY'S DATE", value = date, icon = Icons.Default.CalendarToday, modifier = Modifier.weight(1f))
                    EventMetaItem(label = "ACTIVE SLOT", value = slot, icon = Icons.Default.Timer, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun EventMetaItem(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.outline, fontSize = 8.sp, letterSpacing = 1.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LivePulseSection(expected: Int, checkedIn: Int) {
    val turnout = (checkedIn.toFloat() / expected * 100).toInt()
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Live Crowd Pulse", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = RoundedCornerShape(4.dp)) {
                Text("REAL-TIME", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            VidyaPrayagCard(modifier = Modifier.weight(1f), backgroundColor = MaterialTheme.colorScheme.primary) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("EXPECTED PARENTS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.6f))
                    Text(expected.toString(), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = Color.White)
                    Text("Total Enrollment", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
                }
            }

            VidyaPrayagCard(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("CHECKED-IN NOW", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.outline)
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(checkedIn.toString(), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        Text("/$expected", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(bottom = 4.dp))
                    }
                    LinearProgressIndicator(
                        progress = { checkedIn.toFloat() / expected },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text("$turnout% Turnout", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
private fun LiveCommunicationCard(invites: Int, receipts: Int) {
    VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Live Communication", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Status of today's event broadcasts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.Chat, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                CommStatRow(label = "Invites Delivered", value = "$invites%", icon = Icons.Default.CheckCircle, iconColor = MaterialTheme.colorScheme.secondary)
                CommStatRow(label = "Read Receipts", value = "$receipts%", icon = Icons.Default.Visibility, iconColor = Color(0xFF3B82F6))
            }

            Button(
                onClick = { },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Send Last-Hour Reminder", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CommStatRow(label: String, value: String, icon: ImageVector, iconColor: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(iconColor.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(18.dp))
            }
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HistorySection(history: List<PTMHistoryItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("PTM History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        history.forEach { item ->
            VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.outline)
                        }
                        Column {
                            Text(item.date, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text(item.title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${item.turnout}% Turnout", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("${item.totalMet} Parents Met", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ClassProgressSection(progressList: List<ClassPTMProgress>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Live Progress by Class", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            IconButton(onClick = {}, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))) {
                Icon(Icons.Default.FilterList, null, modifier = Modifier.size(20.dp))
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            progressList.forEach { progress ->
                ClassProgressCard(progress)
            }
        }

        OutlinedButton(
            onClick = { },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Icon(Icons.Default.Groups, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.width(8.dp))
            Text("View All 42 Classes", color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ClassProgressCard(progress: ClassPTMProgress) {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val cardAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(animation = tween(1500), repeatMode = RepeatMode.Reverse),
        label = ""
    )
    val modifier = if (progress.className == "10A") Modifier.alpha(cardAlpha) else Modifier

    VidyaPrayagCard(modifier = Modifier.fillMaxWidth().then(modifier)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(if (progress.progress > 0.8f) MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(progress.className, fontWeight = FontWeight.Bold, color = if (progress.progress > 0.8f) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(progress.teacherName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("Class Teacher", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${progress.metCount}/${progress.totalCount}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = if (progress.progress > 0.8f) MaterialTheme.colorScheme.secondary else Color(0xFFF59E0B))
                    Text("MET", style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.outline)
                }
            }
            LinearProgressIndicator(
                progress = { progress.progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = if (progress.progress > 0.8f) MaterialTheme.colorScheme.secondary else Color(0xFFF59E0B),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun LiveSyncFooter() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
            Text("Syncing Live Data", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
        }
        Text("Last updated: Just now", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
    }
}
