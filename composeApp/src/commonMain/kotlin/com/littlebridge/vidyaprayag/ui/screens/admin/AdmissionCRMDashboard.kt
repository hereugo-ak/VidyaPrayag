package com.littlebridge.vidyaprayag.ui.screens.admin

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.feature.admin.domain.model.Enquiry
import com.littlebridge.vidyaprayag.feature.admin.presentation.AdmissionCRMState
import com.littlebridge.vidyaprayag.feature.admin.presentation.AdmissionCRMViewModel
import com.littlebridge.vidyaprayag.navigation.Destination
import com.littlebridge.vidyaprayag.navigation.LocalAppNavigator
import com.littlebridge.vidyaprayag.ui.components.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdmissionCRMDashboard() {
    val viewModel: AdmissionCRMViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val navigator = LocalAppNavigator.current

    // Re-sync on screen entry (e.g. coming back from another tab) - mirrors
    // the SchoolDashboard pattern.
    LaunchedEffect(Unit) { viewModel.refresh() }

    BaseScreen(
        bottomBar = {
            SchoolDashboardBottomBar(selectedTab = SchoolTab.ENQUIRIES)
        }
    ) { paddingValues, scrollModifier ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(scrollModifier)
                .padding(paddingValues),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                CRMHeaderSection(
                    isLoading = isLoading,
                    onRefresh = { viewModel.refresh() }
                )
            }

            errorMessage?.let { msg ->
                item {
                    ErrorBanner(
                        message = msg,
                        onDismiss = { viewModel.clearError() }
                    )
                }
            }

            item {
                QuickStatsGrid(state)
            }

            item {
                SectionTitle(title = "Recent Enquiries", action = "View All")
            }

            if (state.recentEnquiries.isEmpty() && !isLoading) {
                item { EmptyEnquiriesCard() }
            } else {
                items(state.recentEnquiries, key = { it.id ?: it.studentName + it.date }) { enquiry ->
                    EnquiryCard(
                        enquiry = enquiry,
                        onStatusChange = { newStatus ->
                            enquiry.id?.let { id ->
                                viewModel.updateEnquiryStatus(id, newStatus)
                            }
                        }
                    )
                }
            }

            item {
                ConversionInsightCard(
                    label = state.efficiencyLabel,
                    onGenerateReport = {
                        // Funnel insight → deep analytics dashboard for the
                        // full breakdown (this is the closest available report
                        // view; a dedicated CRM report screen can replace it
                        // when one is added).
                        navigator.navigateTo(Destination.AnalyticsDashboard)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun CRMHeaderSection(isLoading: Boolean, onRefresh: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Admission CRM",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        }
        Text(
            "Track, manage, and convert institutional enquiries efficiently.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuickStatsGrid(state: AdmissionCRMState) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard(
                label = "Total",
                value = state.totalEnquiries.toString(),
                icon = Icons.Default.Groups,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "New",
                value = state.newEnquiries.toString(),
                icon = Icons.Default.NewReleases,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard(
                label = "Follow-ups",
                value = state.followUps.toString(),
                icon = Icons.AutoMirrored.Filled.PhoneCallback,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Converted",
                value = state.conversions.toString(),
                icon = Icons.Default.AutoAwesome,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    VidyaPrayagCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(color.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                }
            }
            Column {
                Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, action: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        TextButton(onClick = { }) {
            Text(action, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun EnquiryCard(
    enquiry: Enquiry,
    onStatusChange: (String) -> Unit
) {
    VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    enquiry.studentName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    enquiry.studentName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Parent: ${enquiry.parentName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(enquiry.className, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Text("•", color = MaterialTheme.colorScheme.outline)
                    Text(enquiry.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }

            // Tap the status badge to open a dropdown that lets the admin move
            // the enquiry through the funnel. The VM handles optimistic patch +
            // server PATCH, so we just need the new status code.
            EnquiryStatusMenu(
                currentStatus = enquiry.status,
                enabled = !enquiry.id.isNullOrBlank(),
                onStatusSelected = onStatusChange
            )
        }
    }
}

/**
 * Tappable status badge that opens a [DropdownMenu] with the four canonical
 * enquiry stages. Disabled when [enabled] is false (e.g. missing id).
 */
@Composable
private fun EnquiryStatusMenu(
    currentStatus: String,
    enabled: Boolean,
    onStatusSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val (label, container) = statusVisuals(currentStatus)

    Box {
        Surface(
            color = container,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.clickable(enabled = enabled) { expanded = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    fontSize = 8.sp
                )
                if (enabled) {
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "Change status",
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            val options = listOf(
                Enquiry.STATUS_NEW to "New",
                Enquiry.STATUS_FOLLOWUP to "Follow-up",
                Enquiry.STATUS_CONVERTED to "Converted",
                Enquiry.STATUS_REJECTED to "Rejected"
            )
            options.forEach { (code, displayName) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            displayName,
                            fontWeight = if (code == currentStatus.lowercase()) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            }
                        )
                    },
                    onClick = {
                        expanded = false
                        if (code != currentStatus.lowercase()) {
                            onStatusSelected(code)
                        }
                    }
                )
            }
        }
    }
}

/** Resolves the label + container colour for a status code. */
@Composable
private fun statusVisuals(status: String): Pair<String, Color> {
    return when (status.lowercase()) {
        Enquiry.STATUS_NEW -> "NEW" to MaterialTheme.colorScheme.secondaryContainer
        Enquiry.STATUS_FOLLOWUP -> "FOLLOW-UP" to MaterialTheme.colorScheme.surfaceVariant
        Enquiry.STATUS_CONVERTED -> "CONVERTED" to MaterialTheme.colorScheme.primaryContainer
        Enquiry.STATUS_REJECTED -> "REJECTED" to MaterialTheme.colorScheme.errorContainer
        else -> status.uppercase() to MaterialTheme.colorScheme.surfaceVariant
    }
}

@Composable
private fun EmptyEnquiriesCard() {
    VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Inbox,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "No enquiries yet",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "New admission enquiries from your school will appear here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            TextButton(onClick = onDismiss) {
                Text("DISMISS", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ConversionInsightCard(label: String, onGenerateReport: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.secondaryContainer)
                Text("Efficiency Insight", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Text(
                "Your lead conversion rate is $label this month. Highly personalized follow-ups could improve this by 15%.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
            Button(
                onClick = onGenerateReport,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("GENERATE REPORT", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
