package com.littlebridge.vidyaprayag.ui.screens.parent

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.feature.parent.presentation.FeeAnnouncement
import com.littlebridge.vidyaprayag.feature.parent.presentation.FeeViewModel
import com.littlebridge.vidyaprayag.ui.components.*
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FeeScreen() {
    val viewModel: FeeViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()

    BaseScreen(
        bottomBar = {
            ParentDashboardBottomBar(selectedTab = ParentTab.FEES)
        }
    ) { paddingValues, scrollModi ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(scrollModi)
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                FeeManagementHeader()
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        TotalCollectedCard(state.totalCollected, state.collectionProgress)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        OutstandingFeesCard(state.outstandingFees, state.overdueCount)
                    }
                }
            }

            item {
                AnnouncementHeader()
            }

            items(state.announcements) { announcement ->
                FeeAnnouncementCard(announcement)
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun FeeManagementHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column {
            Text(
                "FINANCIAL OVERVIEW",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Text(
                "Fee Management",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Button(
            onClick = {},
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("Send Reminders", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TotalCollectedCard(amount: String, progress: Float) {
    VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AccountBalanceWallet, null, tint = MaterialTheme.colorScheme.secondary)
                }
                Text("+12.5%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
            }
            Column {
                Text("Total Collected", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                Text(amount, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text("${(progress * 100).toInt()}% of semester goal reached", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun OutstandingFeesCard(amount: String, count: Int) {
    VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PendingActions, null, tint = MaterialTheme.colorScheme.error)
                }
                Surface(color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                    Text("CRITICAL", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                }
            }
            Column {
                Text("Outstanding Fees", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                Text(amount, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.offset(x = 8.dp)) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .offset(x = (it * (-8)).dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.5.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(listOf("JD", "MK", "AL")[it], fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text("$count Students with overdue payments", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun AnnouncementHeader() {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(
            "COMMUNICATIONS HUB",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
        Text(
            "Recent Announcements",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun FeeAnnouncementCard(announcement: FeeAnnouncement) {
    VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        when (announcement.type) {
                            "Campaign" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                            "Emergency" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (announcement.type) {
                        "Campaign" -> Icons.Default.Campaign
                        "Emergency" -> Icons.Default.Error
                        else -> Icons.Default.Payments
                    },
                    contentDescription = null,
                    tint = when (announcement.type) {
                        "Campaign" -> MaterialTheme.colorScheme.secondary
                        "Emergency" -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outline
                    }
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(announcement.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text(announcement.time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
                Text(announcement.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    StatItem(Icons.Default.Visibility, "Open Rate", announcement.openRate)
                    StatItem(Icons.Default.ChatBubble, "Replies", announcement.engagement)
                }
            }
        }
    }
}

@Composable
private fun StatItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
        Column {
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.outline)
            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
    }
}
