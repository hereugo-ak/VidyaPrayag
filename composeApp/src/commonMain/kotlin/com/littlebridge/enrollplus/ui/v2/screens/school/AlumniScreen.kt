package com.littlebridge.enrollplus.ui.v2.screens.school

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.littlebridge.enrollplus.feature.alumni.domain.model.*
import com.littlebridge.enrollplus.feature.alumni.presentation.AlumniScreenState
import com.littlebridge.enrollplus.feature.alumni.presentation.AlumniViewModel
import com.littlebridge.enrollplus.ui.v2.components.VBackHeader
import com.littlebridge.enrollplus.ui.v2.components.VButton
import com.littlebridge.enrollplus.ui.v2.components.VButtonSize
import com.littlebridge.enrollplus.ui.v2.components.VButtonVariant
import com.littlebridge.enrollplus.ui.v2.components.VCard
import com.littlebridge.enrollplus.ui.v2.components.VIcons
import com.littlebridge.enrollplus.ui.v2.components.VInput
import com.littlebridge.enrollplus.ui.v2.components.VTopTabs
import com.littlebridge.enrollplus.ui.v2.screens.VSectionHeader
import com.littlebridge.enrollplus.ui.v2.screens.VStateHost
import com.littlebridge.enrollplus.ui.v2.screens.collectAsStateV2
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AlumniScreen(
    onBack: () -> Unit = {},
    onOpenAlumni: (String) -> Unit = {},
    onOpenCampaign: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: AlumniViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    var subTab by remember { mutableStateOf("Directory") }

    LaunchedEffect(Unit) {
        viewModel.loadAlumni()
        viewModel.loadCampaigns()
        viewModel.loadAnalytics()
        viewModel.loadPendingVerifications()
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        VBackHeader(title = "Alumni Management", onBack = onBack)

        VTopTabs(
            tabs = listOf("Directory", "Pending", "Campaigns", "Donations", "Mentorship", "Analytics"),
            selected = subTab,
            onSelect = {
                subTab = it
                when (it) {
                    "Directory" -> viewModel.loadAlumni()
                    "Pending" -> viewModel.loadPendingVerifications()
                    "Campaigns" -> viewModel.loadCampaigns()
                    "Donations" -> viewModel.loadDonations()
                    "Mentorship" -> {
                        viewModel.loadMentorships()
                        viewModel.loadMentorshipRequests()
                    }
                    "Analytics" -> viewModel.loadAnalytics()
                }
            },
        )

        when (subTab) {
            "Directory" -> AlumniDirectoryTab(
                state = state,
                onOpenAlumni = onOpenAlumni,
                onRetry = { viewModel.loadAlumni() },
                onAddAlumni = { viewModel.createAlumni(it) },
                onBulkImport = { viewModel.bulkImport(it) },
            )
            "Pending" -> AlumniPendingTab(
                state = state,
                onApprove = { id -> viewModel.verifyAlumni(id, "approve") },
                onDecline = { id -> viewModel.verifyAlumni(id, "decline") },
                onRetry = { viewModel.loadPendingVerifications() },
            )
            "Campaigns" -> AlumniCampaignsTab(
                state = state,
                onOpenCampaign = onOpenCampaign,
                onRetry = { viewModel.loadCampaigns() },
            )
            "Donations" -> AlumniDonationsTab(
                state = state,
                onRetry = { viewModel.loadDonations() },
            )
            "Mentorship" -> AlumniMentorshipTab(
                state = state,
                onRetry = {
                    viewModel.loadMentorships()
                    viewModel.loadMentorshipRequests()
                },
            )
            "Analytics" -> AlumniAnalyticsTab(
                state = state,
                onRetry = { viewModel.loadAnalytics() },
            )
        }
    }
}

@Composable
private fun AlumniDirectoryTab(
    state: AlumniScreenState,
    onOpenAlumni: (String) -> Unit,
    onRetry: () -> Unit,
    onAddAlumni: (CreateAlumniRequest) -> Unit,
    onBulkImport: (List<CreateAlumniRequest>) -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VButton(
                text = "Add Alumni",
                onClick = { showAddDialog = true },
                variant = VButtonVariant.Primary,
                size = VButtonSize.Sm,
                leading = { Icon(VIcons.Users, contentDescription = null, modifier = Modifier.size(14.dp)) },
            )
            VButton(
                text = "Bulk Import",
                onClick = { showImportDialog = true },
                variant = VButtonVariant.Secondary,
                size = VButtonSize.Sm,
                leading = { Icon(VIcons.Upload, contentDescription = null, modifier = Modifier.size(14.dp)) },
            )
        }

        Spacer(Modifier.height(12.dp))

        state.infoMessage?.let {
            VCard(modifier = Modifier.fillMaxWidth()) {
                Text(it, style = VTheme.type.body, color = VTheme.colors.accent)
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    VStateHost(
        loading = state.isLoading,
        error = state.error,
        isEmpty = state.alumni.isEmpty(),
        emptyTitle = "No alumni yet",
        emptyBody = "Add alumni manually or use bulk import",
        onRetry = onRetry,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.alumni.forEach { alumni ->
                AlumniRowCard(alumni = alumni, onClick = { onOpenAlumni(alumni.id) })
            }
        }
    }

    if (showAddDialog) {
        AddAlumniDialog(
            loading = state.isLoading,
            onDismiss = { showAddDialog = false },
            onSubmit = { request ->
                onAddAlumni(request)
                showAddDialog = false
            },
        )
    }

    if (showImportDialog) {
        BulkImportDialog(
            loading = state.isLoading,
            onDismiss = { showImportDialog = false },
            onSubmit = { rows ->
                onBulkImport(rows)
                showImportDialog = false
            },
        )
    }
}

@Composable
private fun AddAlumniDialog(
    loading: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (CreateAlumniRequest) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var graduationYear by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var profession by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }

    val canSubmit = name.isNotBlank() && graduationYear.toIntOrNull() != null

    Dialog(onDismissRequest = onDismiss) {
        VCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Add Alumni", style = VTheme.type.h3, color = VTheme.colors.ink)

                VInput(
                    value = name,
                    onValueChange = { name = it },
                    label = "Full name *",
                    placeholder = "e.g. Priya Sharma",
                )
                VInput(
                    value = graduationYear,
                    onValueChange = { graduationYear = it.filter { ch -> ch.isDigit() }.take(4) },
                    label = "Graduation year *",
                    placeholder = "2024",
                    keyboardType = KeyboardType.Number,
                )
                VInput(
                    value = studentId,
                    onValueChange = { studentId = it },
                    label = "Student ID (optional)",
                    placeholder = "ADM-2020-001",
                )
                VInput(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email",
                    placeholder = "priya@example.com",
                    keyboardType = KeyboardType.Email,
                )
                VInput(
                    value = phone,
                    onValueChange = { phone = it },
                    label = "Phone",
                    placeholder = "+91 98765 43210",
                    keyboardType = KeyboardType.Phone,
                )
                VInput(
                    value = profession,
                    onValueChange = { profession = it },
                    label = "Profession",
                    placeholder = "Software Engineer",
                )
                VInput(
                    value = company,
                    onValueChange = { company = it },
                    label = "Company",
                    placeholder = "Google",
                )
                VInput(
                    value = city,
                    onValueChange = { city = it },
                    label = "City",
                    placeholder = "Bengaluru",
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        variant = VButtonVariant.Ghost,
                        size = VButtonSize.Sm,
                        modifier = Modifier.weight(1f),
                    )
                    VButton(
                        text = "Add",
                        onClick = {
                            onSubmit(
                                CreateAlumniRequest(
                                    studentId = studentId.ifBlank { null },
                                    name = name.trim(),
                                    graduationYear = graduationYear.toInt(),
                                    email = email.ifBlank { null },
                                    phone = phone.ifBlank { null },
                                    currentProfession = profession.ifBlank { null },
                                    company = company.ifBlank { null },
                                    city = city.ifBlank { null },
                                ),
                            )
                        },
                        variant = VButtonVariant.Primary,
                        size = VButtonSize.Sm,
                        modifier = Modifier.weight(1f),
                        enabled = canSubmit,
                        loading = loading,
                    )
                }
            }
        }
    }
}

@Composable
private fun BulkImportDialog(
    loading: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (List<CreateAlumniRequest>) -> Unit,
) {
    var csvText by remember { mutableStateOf("") }

    val rows = parseAlumniCsv(csvText)
    val canSubmit = rows.isNotEmpty()

    Dialog(onDismissRequest = onDismiss) {
        VCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Bulk Import Alumni", style = VTheme.type.h3, color = VTheme.colors.ink)
                Text(
                    "Paste CSV data. Each line: name,graduationYear,email,phone,profession,company,city",
                    style = VTheme.type.caption,
                    color = VTheme.colors.ink3,
                )

                OutlinedTextField(
                    value = csvText,
                    onValueChange = { csvText = it },
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    placeholder = { Text("Priya Sharma,2024,priya@example.com,9876543210,Engineer,Google,Bengaluru\nRahul Verma,2023,...") },
                    textStyle = VTheme.type.body,
                )

                if (rows.isNotEmpty()) {
                    Text("${rows.size} row(s) ready to import", style = VTheme.type.caption, color = VTheme.colors.accent)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        variant = VButtonVariant.Ghost,
                        size = VButtonSize.Sm,
                        modifier = Modifier.weight(1f),
                    )
                    VButton(
                        text = "Import ${if (rows.isNotEmpty()) "(${rows.size})" else ""}",
                        onClick = { onSubmit(rows) },
                        variant = VButtonVariant.Primary,
                        size = VButtonSize.Sm,
                        modifier = Modifier.weight(1f),
                        enabled = canSubmit,
                        loading = loading,
                    )
                }
            }
        }
    }
}

private fun parseAlumniCsv(text: String): List<CreateAlumniRequest> {
    return text.lineSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() && !it.startsWith("#") }
        .mapNotNull { line ->
            val parts = line.split(",").map { it.trim() }
            if (parts.size < 2) return@mapNotNull null
            val name = parts[0]
            val year = parts[1].toIntOrNull() ?: return@mapNotNull null
            if (name.isBlank()) return@mapNotNull null
            CreateAlumniRequest(
                name = name,
                graduationYear = year,
                email = parts.getOrNull(2)?.ifBlank { null },
                phone = parts.getOrNull(3)?.ifBlank { null },
                currentProfession = parts.getOrNull(4)?.ifBlank { null },
                company = parts.getOrNull(5)?.ifBlank { null },
                city = parts.getOrNull(6)?.ifBlank { null },
            )
        }
        .toList()
}

@Composable
private fun AlumniPendingTab(
    state: AlumniScreenState,
    onApprove: (String) -> Unit,
    onDecline: (String) -> Unit,
    onRetry: () -> Unit,
) {
    VStateHost(
        loading = state.isLoading,
        error = state.error,
        isEmpty = state.pendingVerifications.isEmpty(),
        emptyTitle = "No pending verifications",
        emptyBody = "All alumni registrations have been reviewed",
        onRetry = onRetry,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.pendingVerifications.forEach { alumni ->
                VCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(alumni.name, style = VTheme.type.body, fontWeight = FontWeight.SemiBold, color = VTheme.colors.ink)
                        Text("Batch ${alumni.graduationYear}", style = VTheme.type.caption, color = VTheme.colors.ink3)
                        alumni.email?.let { Text(it, style = VTheme.type.caption, color = VTheme.colors.ink3) }
                        alumni.phone?.let { Text(it, style = VTheme.type.caption, color = VTheme.colors.ink3) }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                            VChipButton(text = "Approve", onClick = { onApprove(alumni.id) })
                            VChipButton(text = "Decline", onClick = { onDecline(alumni.id) }, isDestructive = true)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlumniCampaignsTab(
    state: AlumniScreenState,
    onOpenCampaign: (String) -> Unit,
    onRetry: () -> Unit,
) {
    VStateHost(
        loading = state.isLoading,
        error = state.error,
        isEmpty = state.campaigns.isEmpty(),
        emptyTitle = "No campaigns yet",
        emptyBody = "Create a donation campaign to engage alumni",
        onRetry = onRetry,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.campaigns.forEach { campaign ->
                VCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onOpenCampaign(campaign.id) },
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(campaign.title, style = VTheme.type.body, fontWeight = FontWeight.SemiBold, color = VTheme.colors.ink)
                        campaign.description?.let {
                            Text(it, style = VTheme.type.caption, color = VTheme.colors.ink3, maxLines = 2)
                        }
                        val progress = if (campaign.targetAmount > 0) {
                            (campaign.amountRaised / campaign.targetAmount * 100).toInt()
                        } else 0
                        Text(
                            "₹${campaign.amountRaised.toInt()} / ₹${campaign.targetAmount.toInt()} ($progress%) • ${campaign.donorCount} donors",
                            style = VTheme.type.caption,
                            color = VTheme.colors.ink3,
                        )
                        Text("Status: ${campaign.status}", style = VTheme.type.caption, color = VTheme.colors.ink3)
                    }
                }
            }
        }
    }
}

@Composable
private fun AlumniDonationsTab(
    state: AlumniScreenState,
    onRetry: () -> Unit,
) {
    VStateHost(
        loading = state.isLoading,
        error = state.error,
        isEmpty = state.donations.isEmpty(),
        emptyTitle = "No donations recorded",
        emptyBody = "Log donations from the alumni detail screen",
        onRetry = onRetry,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.donations.forEach { donation ->
                VCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(donation.alumniName, style = VTheme.type.body, fontWeight = FontWeight.SemiBold, color = VTheme.colors.ink)
                        Text("₹${donation.amount.toInt()}", style = VTheme.type.body, color = VTheme.colors.ink)
                        donation.campaignTitle?.let { Text("Campaign: $it", style = VTheme.type.caption, color = VTheme.colors.ink3) }
                        Text("Date: ${donation.donationDate}", style = VTheme.type.caption, color = VTheme.colors.ink3)
                        if (donation.is80gEligible) {
                            Text("80G eligible • Receipt: ${donation.receiptNumber ?: "Pending"}", style = VTheme.type.caption, color = VTheme.colors.ink3)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlumniAnalyticsTab(
    state: AlumniScreenState,
    onRetry: () -> Unit,
) {
    val analytics = state.analytics
    VStateHost(
        loading = state.isLoading,
        error = state.error,
        isEmpty = analytics == null,
        emptyTitle = "No analytics data",
        onRetry = onRetry,
    ) {
        val a = analytics!!
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            VSectionHeader("Overview")
            AnalyticsStatCard(label = "Total Alumni", value = a.totalAlumni.toString())
            AnalyticsStatCard(label = "Active (90 days)", value = a.activeAlumni.toString())
            AnalyticsStatCard(label = "Pending Verifications", value = a.pendingVerifications.toString())
            AnalyticsStatCard(label = "Engagement Rate", value = "${(a.engagementRate * 100).toInt() / 100.0}%")
            AnalyticsStatCard(label = "Total Donations", value = "₹${a.totalDonations.toInt()}")
            AnalyticsStatCard(label = "Active Campaigns", value = a.activeCampaigns.toString())
            AnalyticsStatCard(label = "Active Mentorships", value = a.activeMentorships.toString())

            if (a.byGraduationYear.isNotEmpty()) {
                VSectionHeader("By Graduation Year")
                a.byGraduationYear.forEach { (year, count) ->
                    AnalyticsStatCard(label = year, value = count.toString())
                }
            }

            if (a.byProfession.isNotEmpty()) {
                VSectionHeader("By Profession")
                a.byProfession.forEach { (profession, count) ->
                    AnalyticsStatCard(label = profession, value = count.toString())
                }
            }

            if (a.byCity.isNotEmpty()) {
                VSectionHeader("By City")
                a.byCity.forEach { (city, count) ->
                    AnalyticsStatCard(label = city, value = count.toString())
                }
            }
        }
    }
}

@Composable
private fun AlumniMentorshipTab(
    state: AlumniScreenState,
    onRetry: () -> Unit,
) {
    val c = VTheme.colors
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Active mentorships
        VSectionHeader("Active Mentorships")
        VStateHost(
            loading = state.isLoading,
            error = state.error,
            isEmpty = state.mentorships.isEmpty(),
            emptyTitle = "No active mentorships",
            emptyBody = "Mentorships will appear here once alumni start mentoring students",
            onRetry = onRetry,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.mentorships.forEach { m ->
                    VCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(m.alumniName, style = VTheme.type.body, fontWeight = FontWeight.SemiBold, color = c.ink)
                            Text("Mentoring: ${m.studentName}", style = VTheme.type.caption, color = c.ink3)
                            Text("Status: ${m.status}", style = VTheme.type.caption, color = c.ink3)
                            Text("Started: ${m.startDate}", style = VTheme.type.caption, color = c.ink3)
                            if (m.sessionCount > 0) {
                                Text("Sessions: ${m.sessionCount}", style = VTheme.type.caption, color = c.ink3)
                            }
                            m.notes?.let { Text("Notes: $it", style = VTheme.type.caption, color = c.ink3) }
                        }
                    }
                }
            }
        }

        // Pending requests
        VSectionHeader("Mentorship Requests")
        VStateHost(
            loading = state.isLoading,
            error = state.error,
            isEmpty = state.mentorshipRequests.isEmpty(),
            emptyTitle = "No mentorship requests",
            emptyBody = "Student requests for alumni mentorship will appear here",
            onRetry = onRetry,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.mentorshipRequests.forEach { r ->
                    VCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(r.alumniName, style = VTheme.type.body, fontWeight = FontWeight.SemiBold, color = c.ink)
                            Text("From: ${r.studentName}", style = VTheme.type.caption, color = c.ink3)
                            Text("Requested by: ${r.requestedByName}", style = VTheme.type.caption, color = c.ink3)
                            r.expertiseArea?.let { Text("Expertise: $it", style = VTheme.type.caption, color = c.ink3) }
                            r.message?.let { Text("Message: $it", style = VTheme.type.caption, color = c.ink3) }
                            Text("Status: ${r.status}", style = VTheme.type.caption, color = c.ink3)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlumniRowCard(alumni: Alumni, onClick: () -> Unit) {
    VCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(alumni.name, style = VTheme.type.body, fontWeight = FontWeight.SemiBold, color = VTheme.colors.ink)
                if (alumni.isFeatured) {
                    Text("★", style = VTheme.type.caption, color = VTheme.colors.accent)
                }
            }
            Text("Batch ${alumni.graduationYear}", style = VTheme.type.caption, color = VTheme.colors.ink3)
            alumni.currentProfession?.let {
                Text("$it${alumni.company?.let { c -> " @ $c" }}", style = VTheme.type.caption, color = VTheme.colors.ink3)
            }
            alumni.city?.let { Text(it, style = VTheme.type.caption, color = VTheme.colors.ink3) }
            if (alumni.isMentor) {
                Text("Mentor${alumni.mentorExpertise?.let { e -> " — $e" }}", style = VTheme.type.caption, color = VTheme.colors.accent)
            }
        }
    }
}

@Composable
private fun AnalyticsStatCard(label: String, value: String) {
    VCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = VTheme.type.body, color = VTheme.colors.ink3)
            Text(value, style = VTheme.type.body, fontWeight = FontWeight.SemiBold, color = VTheme.colors.ink)
        }
    }
}

@Composable
private fun VChipButton(text: String, onClick: () -> Unit, isDestructive: Boolean = false) {
    val c = VTheme.colors
    VCard(
        onClick = onClick,
        padding = 8.dp,
    ) {
        Text(
            text,
            style = VTheme.type.caption,
            fontWeight = FontWeight.Medium,
            color = if (isDestructive) c.danger else c.accent,
        )
    }
}
