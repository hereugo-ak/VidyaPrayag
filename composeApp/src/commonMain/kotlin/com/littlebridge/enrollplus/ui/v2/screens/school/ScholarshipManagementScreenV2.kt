package com.littlebridge.enrollplus.ui.v2.screens.school

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import com.littlebridge.enrollplus.feature.scholarship.domain.model.ScholarshipScheme
import com.littlebridge.enrollplus.feature.scholarship.domain.model.UpdateSchemeRequest
import com.littlebridge.enrollplus.feature.scholarship.presentation.ScholarshipScreenState
import com.littlebridge.enrollplus.feature.scholarship.presentation.ScholarshipViewModel
import com.littlebridge.enrollplus.ui.v2.components.VBackHeader
import com.littlebridge.enrollplus.ui.v2.components.VBadge
import com.littlebridge.enrollplus.ui.v2.components.VBadgeTone
import com.littlebridge.enrollplus.ui.v2.components.VButton
import com.littlebridge.enrollplus.ui.v2.components.VButtonSize
import com.littlebridge.enrollplus.ui.v2.components.VButtonVariant
import com.littlebridge.enrollplus.ui.v2.components.VCard
import com.littlebridge.enrollplus.ui.v2.components.VDatePicker
import com.littlebridge.enrollplus.ui.v2.components.VIcons
import com.littlebridge.enrollplus.ui.v2.components.VInput
import com.littlebridge.enrollplus.ui.v2.screens.VSectionHeader
import com.littlebridge.enrollplus.ui.v2.screens.VStateHost
import com.littlebridge.enrollplus.ui.v2.screens.collectAsStateV2
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ScholarshipManagementScreenV2(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ScholarshipViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    var showSchemeForm by remember { mutableStateOf(false) }
    var showApplications by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0=schemes, 1=applications, 2=renewals
    var editingScheme by remember { mutableStateOf<ScholarshipScheme?>(null) }
    var deleteScheme by remember { mutableStateOf<ScholarshipScheme?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadSchemes()
        viewModel.loadApplications()
        viewModel.loadRenewals()
    }

    LaunchedEffect(state.infoMessage) {
        if (state.infoMessage != null) {
            showSchemeForm = false
            editingScheme = null
            viewModel.clearMessages()
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        VBackHeader(title = "Scholarship Management", onBack = onBack)

        // Tab selector
        ScholarshipTabRow(selectedTab = selectedTab, onTabSelected = { selectedTab = it })

        // Create button is always visible on Schemes tab (even when empty)
        if (selectedTab == 0 && !state.isLoading && state.error == null) {
            VButton(
                text = "+ Create New Scheme",
                onClick = { showSchemeForm = true },
                variant = VButtonVariant.Primary,
                size = VButtonSize.Md,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        VStateHost(
            loading = state.isLoading,
            error = state.error,
            isEmpty = when (selectedTab) {
                0 -> state.schemes.isEmpty()
                1 -> state.applications.isEmpty()
                else -> state.renewals.isEmpty()
            },
            emptyTitle = when (selectedTab) {
                0 -> "No scholarship schemes yet"
                1 -> "No applications to review"
                else -> "No renewal requests"
            },
            emptyBody = when (selectedTab) {
                0 -> "Tap \"Create New Scheme\" above to add one."
                1 -> "Applications will appear here when parents apply."
                else -> "Renewal requests will appear here."
            },
            onRetry = { viewModel.loadSchemes() },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (selectedTab) {
                    0 -> {
                        items(state.schemes) { scheme ->
                            ScholarshipSchemeCard(
                                scheme = scheme,
                                onEdit = { editingScheme = scheme },
                                onDelete = { deleteScheme = scheme },
                            )
                        }
                    }
                    1 -> {
                        item {
                            VSectionHeader(title = "APPLICATIONS (${state.applications.size})")
                        }
                        items(state.applications) { app ->
                            ScholarshipApplicationReviewCard(
                                application = app,
                                onApprove = { remarks, amount ->
                                    viewModel.approveApplication(
                                        app.id,
                                        com.littlebridge.enrollplus.feature.scholarship.domain.model.ApproveApplicationRequest(
                                            remarks = remarks,
                                            disbursementAmount = amount,
                                        )
                                    )
                                },
                                onReject = { remarks ->
                                    viewModel.rejectApplication(
                                        app.id,
                                        com.littlebridge.enrollplus.feature.scholarship.domain.model.RejectApplicationRequest(remarks)
                                    )
                                },
                                onDisburse = { amount, reference ->
                                    viewModel.disburse(
                                        app.id,
                                        com.littlebridge.enrollplus.feature.scholarship.domain.model.DisburseRequest(amount, reference)
                                    )
                                },
                            )
                        }
                    }
                    2 -> {
                        item {
                            VSectionHeader(title = "RENEWALS (${state.renewals.size})")
                        }
                        items(state.renewals) { renewal ->
                            ScholarshipRenewalCard(
                                renewal = renewal,
                                onApprove = { remarks ->
                                    viewModel.approveRenewal(
                                        renewal.id,
                                        com.littlebridge.enrollplus.feature.scholarship.domain.model.ApproveRenewalRequest(remarks)
                                    )
                                },
                                onReject = { remarks ->
                                    viewModel.rejectRenewal(
                                        renewal.id,
                                        com.littlebridge.enrollplus.feature.scholarship.domain.model.RejectApplicationRequest(remarks)
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    // Create scheme form overlay
    AnimatedVisibility(
        visible = showSchemeForm,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        ScholarshipSchemeForm(
            onCreate = { request ->
                viewModel.createScheme(request)
            },
            onDismiss = { showSchemeForm = false },
        )
    }

    // Edit scheme form overlay
    AnimatedVisibility(
        visible = editingScheme != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        editingScheme?.let { scheme ->
            ScholarshipSchemeForm(
                existingScheme = scheme,
                onUpdate = { request ->
                    viewModel.updateScheme(scheme.id, request)
                },
                onDismiss = { editingScheme = null },
            )
        }
    }

    // Delete confirmation dialog
    if (deleteScheme != null) {
        DeleteConfirmationDialog(
            title = "Delete Scholarship",
            message = "Are you sure you want to deactivate \"${deleteScheme?.title}\"? This will remove it from the parent view but existing applications will be preserved.",
            onConfirm = {
                deleteScheme?.let { viewModel.deleteScheme(it.id) }
                deleteScheme = null
            },
            onDismiss = { deleteScheme = null },
        )
    }
}

@Composable
private fun ScholarshipTabRow(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val c = VTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf("Schemes", "Applications", "Renewals").forEachIndexed { index, label ->
            val isSelected = selectedTab == index
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) c.accent else c.cream)
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = VTheme.type.label,
                    color = if (isSelected) Color.White else c.ink3,
                )
            }
        }
    }
}

@Composable
private fun ScholarshipSchemeCard(
    scheme: ScholarshipScheme,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val c = VTheme.colors
    val typeTone = when (scheme.scholarshipType) {
        "full_waiver" -> VBadgeTone.Success
        "partial_waiver" -> VBadgeTone.Warning
        else -> VBadgeTone.Accent
    }
    VCard {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VBadge(text = scheme.scholarshipType.replace("_", " "), tone = typeTone)
            if (scheme.isRenewable) {
                VBadge(text = "Renewable", tone = VBadgeTone.Neutral)
            }
            if (!scheme.isActive) {
                VBadge(text = "Inactive", tone = VBadgeTone.Danger)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(scheme.title, style = VTheme.type.h3.colored(c.ink))
        if (scheme.description.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(scheme.description, style = VTheme.type.body.colored(c.ink2))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Award", style = VTheme.type.label.colored(c.ink3))
                Text(scheme.amount, style = VTheme.type.dataLg.colored(c.ink))
            }
            if (scheme.eligibilityCriteria.isNotBlank()) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("Eligibility", style = VTheme.type.label.colored(c.ink3))
                    Text(scheme.eligibilityCriteria, style = VTheme.type.caption.colored(c.ink2))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) {
                VButton(
                    text = "Edit",
                    onClick = onEdit,
                    variant = VButtonVariant.Secondary,
                    size = VButtonSize.Sm,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Box(Modifier.weight(1f)) {
                VButton(
                    text = "Deactivate",
                    onClick = onDelete,
                    variant = VButtonVariant.Destructive,
                    size = VButtonSize.Sm,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ScholarshipApplicationReviewCard(
    application: com.littlebridge.enrollplus.feature.scholarship.domain.model.ScholarshipApplication,
    onApprove: (String, Double?) -> Unit,
    onReject: (String) -> Unit,
    onDisburse: (Double, String) -> Unit,
) {
    val c = VTheme.colors
    var showActions by remember { mutableStateOf(false) }
    var remarks by remember { mutableStateOf("") }
    var disbursementAmount by remember { mutableStateOf("") }
    var disbursementReference by remember { mutableStateOf("") }

    val statusTone = when (application.status) {
        "PENDING" -> VBadgeTone.Warning
        "APPROVED" -> VBadgeTone.Success
        "REJECTED" -> VBadgeTone.Danger
        "DISBURSED" -> VBadgeTone.Accent
        else -> VBadgeTone.Neutral
    }

    VCard {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    application.scholarshipTitle ?: application.institution,
                    style = VTheme.type.bodyStrong.colored(c.ink),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    application.studentName ?: "Student",
                    style = VTheme.type.caption.colored(c.ink3),
                )
            }
            VBadge(text = application.status, tone = statusTone)
        }

        val appText = application.parentApplicationText
        if (appText?.isNotBlank() == true) {
            Spacer(Modifier.height(8.dp))
            Text(appText, style = VTheme.type.caption.colored(c.ink2))
        }

        if (application.documentUrls.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("${application.documentUrls.size} document(s) attached", style = VTheme.type.label.colored(c.ink3))
        }

        if (application.status == "PENDING") {
            Spacer(Modifier.height(12.dp))
            if (!showActions) {
                VButton(
                    text = "Review",
                    onClick = { showActions = true },
                    variant = VButtonVariant.Secondary,
                    size = VButtonSize.Sm,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                VInput(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = "Remarks",
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                VInput(
                    value = disbursementAmount,
                    onValueChange = { disbursementAmount = it.filter { it.isDigit() || it == '.' } },
                    label = "Disbursement Amount (optional)",
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) {
                        VButton(
                            text = "Approve",
                            onClick = {
                                val amount = disbursementAmount.toDoubleOrNull()
                                onApprove(remarks, amount)
                                showActions = false
                            },
                            variant = VButtonVariant.Primary,
                            size = VButtonSize.Sm,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        VButton(
                            text = "Reject",
                            onClick = {
                                onReject(remarks)
                                showActions = false
                            },
                            variant = VButtonVariant.Secondary,
                            size = VButtonSize.Sm,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        if (application.status == "APPROVED") {
            Spacer(Modifier.height(12.dp))
            VInput(
                value = disbursementReference,
                onValueChange = { disbursementReference = it },
                label = "Disbursement Reference",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            VButton(
                text = "Record Disbursement",
                onClick = {
                    val amount = disbursementAmount.toDoubleOrNull() ?: application.disbursementAmount ?: 0.0
                    if (disbursementReference.isNotBlank()) {
                        onDisburse(amount, disbursementReference)
                    }
                },
                variant = VButtonVariant.Primary,
                size = VButtonSize.Sm,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (application.status == "DISBURSED" && application.disbursementAmount != null) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Disbursed: ${application.disbursementAmount}", style = VTheme.type.caption.colored(c.ink3))
                Text("Ref: ${application.disbursementReference ?: "—"}", style = VTheme.type.caption.colored(c.ink3))
            }
        }
    }
}

@Composable
private fun ScholarshipRenewalCard(
    renewal: com.littlebridge.enrollplus.feature.scholarship.domain.model.ScholarshipRenewal,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
) {
    val c = VTheme.colors
    var remarks by remember { mutableStateOf("") }
    val statusTone = when (renewal.status) {
        "pending" -> VBadgeTone.Warning
        "approved" -> VBadgeTone.Success
        "rejected" -> VBadgeTone.Danger
        else -> VBadgeTone.Neutral
    }

    VCard {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    renewal.scholarshipTitle ?: "Scholarship Renewal",
                    style = VTheme.type.bodyStrong.colored(c.ink),
                )
                Spacer(Modifier.height(2.dp))
                Text("Renewal for academic year", style = VTheme.type.caption.colored(c.ink3))
            }
            VBadge(text = renewal.status, tone = statusTone)
        }

        if (renewal.status == "pending") {
            Spacer(Modifier.height(12.dp))
            VInput(
                value = remarks,
                onValueChange = { remarks = it },
                label = "Remarks",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                    VButton(
                        text = "Approve",
                        onClick = { onApprove(remarks) },
                        variant = VButtonVariant.Primary,
                        size = VButtonSize.Sm,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Box(Modifier.weight(1f)) {
                    VButton(
                        text = "Reject",
                        onClick = { onReject(remarks) },
                        variant = VButtonVariant.Secondary,
                        size = VButtonSize.Sm,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun ScholarshipSchemeForm(
    existingScheme: ScholarshipScheme? = null,
    onCreate: (com.littlebridge.enrollplus.feature.scholarship.domain.model.CreateSchemeRequest) -> Unit = {},
    onUpdate: (UpdateSchemeRequest) -> Unit = {},
    onDismiss: () -> Unit,
) {
    val isEdit = existingScheme != null
    var title by remember { mutableStateOf(existingScheme?.title ?: "") }
    var description by remember { mutableStateOf(existingScheme?.description ?: "") }
    var amount by remember { mutableStateOf(existingScheme?.amount ?: "") }
    var numericAmount by remember { mutableStateOf(existingScheme?.numericAmount?.toString() ?: "") }
    var scholarshipType by remember { mutableStateOf(existingScheme?.scholarshipType ?: "fixed") }
    var waiverPercentage by remember { mutableStateOf(existingScheme?.waiverPercentage?.toString() ?: "") }
    var eligibilityCriteria by remember { mutableStateOf(existingScheme?.eligibilityCriteria ?: "") }
    var category by remember { mutableStateOf(existingScheme?.category ?: "Merit Based") }
    var startDate by remember { mutableStateOf(existingScheme?.startDate ?: "") }
    var endDate by remember { mutableStateOf(existingScheme?.endDate ?: "") }
    var isRenewable by remember { mutableStateOf(existingScheme?.isRenewable ?: false) }
    var renewalPeriodMonths by remember { mutableStateOf(existingScheme?.renewalPeriodMonths?.toString() ?: "12") }
    var typeDropdownOpen by remember { mutableStateOf(false) }
    val c = VTheme.colors
    val scrollState = rememberScrollState()

    val typeOptions = listOf("fixed" to "Fixed Amount", "full_waiver" to "Full Waiver", "partial_waiver" to "Partial Waiver")
    val categoryOptions = listOf("Merit Based", "Need Based", "Sports", "Cultural", "Special")

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        VCard(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .clickable(enabled = false) {},
            padding = 16.dp,
        ) {
            Text(
                if (isEdit) "Edit Scholarship Scheme" else "Create Scholarship Scheme",
                style = VTheme.type.h3.colored(c.ink),
            )
            Spacer(Modifier.height(16.dp))

            Column(Modifier.verticalScroll(scrollState)) {
                VInput(value = title, onValueChange = { title = it }, label = "Title *", modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                VInput(value = description, onValueChange = { description = it }, label = "Description", modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                VInput(value = amount, onValueChange = { amount = it }, label = "Display Amount (e.g. ₹5,000)", modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                VInput(value = numericAmount, onValueChange = { numericAmount = it.filter { it.isDigit() || it == '.' } }, label = "Numeric Amount (for fixed type)", modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))

                // Scholarship type dropdown
                Text("Type", style = VTheme.type.label.colored(c.ink3))
                Spacer(Modifier.height(4.dp))
                Box {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(c.cream)
                            .clickable { typeDropdownOpen = !typeDropdownOpen }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                typeOptions.firstOrNull { it.first == scholarshipType }?.second ?: scholarshipType,
                                style = VTheme.type.body.colored(c.ink),
                            )
                            Icon(
                                imageVector = if (typeDropdownOpen) VIcons.ChevronUp else VIcons.ChevronDown,
                                contentDescription = "Toggle",
                                tint = c.ink3,
                            )
                        }
                    }
                    if (typeDropdownOpen) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .padding(vertical = 4.dp),
                        ) {
                            typeOptions.forEach { (value, label) ->
                                val isSelected = scholarshipType == value
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            scholarshipType = value
                                            typeDropdownOpen = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(label, style = VTheme.type.body.colored(if (isSelected) c.accent else c.ink))
                                    if (isSelected) {
                                        Icon(imageVector = VIcons.Check, contentDescription = null, tint = c.accent)
                                    }
                                }
                            }
                        }
                    }
                }

                if (scholarshipType == "partial_waiver") {
                    Spacer(Modifier.height(8.dp))
                    VInput(value = waiverPercentage, onValueChange = { waiverPercentage = it.filter { it.isDigit() || it == '.' } }, label = "Waiver Percentage (0-100)", modifier = Modifier.fillMaxWidth())
                }

                Spacer(Modifier.height(8.dp))
                VInput(value = eligibilityCriteria, onValueChange = { eligibilityCriteria = it }, label = "Eligibility Criteria", modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))

                // Category dropdown
                Text("Category", style = VTheme.type.label.colored(c.ink3))
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categoryOptions.forEach { cat ->
                        val isSelected = category == cat
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) c.accent else c.cream)
                                .clickable { category = cat }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text(cat, style = VTheme.type.label, color = if (isSelected) Color.White else c.ink3)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                VDatePicker(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = "Start Date",
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                VDatePicker(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = "End Date (Application Deadline)",
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))

                // Renewable toggle
                Row(
                    Modifier.fillMaxWidth().clickable { isRenewable = !isRenewable },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(if (isRenewable) c.accent else c.cream),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isRenewable) {
                            Text("✓", style = VTheme.type.label, color = Color.White)
                        }
                    }
                    Text("Renewable", style = VTheme.type.body.colored(c.ink))
                }

                if (isRenewable) {
                    Spacer(Modifier.height(8.dp))
                    VInput(value = renewalPeriodMonths, onValueChange = { renewalPeriodMonths = it.filter { ch -> ch.isDigit() } }, label = "Renewal Period (months)", modifier = Modifier.fillMaxWidth())
                }

                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) {
                        VButton(
                            text = "Cancel",
                            onClick = onDismiss,
                            variant = VButtonVariant.Secondary,
                            size = VButtonSize.Md,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        VButton(
                            text = if (isEdit) "Update" else "Create",
                            onClick = {
                                if (title.isNotBlank()) {
                                    if (isEdit) {
                                        onUpdate(
                                            UpdateSchemeRequest(
                                                title = title,
                                                description = description,
                                                amount = amount,
                                                numericAmount = numericAmount.toDoubleOrNull(),
                                                scholarshipType = scholarshipType,
                                                waiverPercentage = waiverPercentage.toFloatOrNull(),
                                                eligibilityCriteria = eligibilityCriteria,
                                                category = category,
                                                startDate = startDate.ifBlank { null },
                                                endDate = endDate.ifBlank { null },
                                                isRenewable = isRenewable,
                                                renewalPeriodMonths = renewalPeriodMonths.toIntOrNull(),
                                            )
                                        )
                                    } else {
                                        onCreate(
                                            com.littlebridge.enrollplus.feature.scholarship.domain.model.CreateSchemeRequest(
                                                title = title,
                                                description = description,
                                                amount = amount,
                                                numericAmount = numericAmount.toDoubleOrNull(),
                                                scholarshipType = scholarshipType,
                                                waiverPercentage = waiverPercentage.toFloatOrNull(),
                                                eligibilityCriteria = eligibilityCriteria,
                                                category = category,
                                                startDate = startDate.ifBlank { null },
                                                endDate = endDate.ifBlank { null },
                                                isRenewable = isRenewable,
                                                renewalPeriodMonths = renewalPeriodMonths.toIntOrNull(),
                                            )
                                        )
                                    }
                                }
                            },
                            variant = VButtonVariant.Primary,
                            size = VButtonSize.Md,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = VTheme.colors
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        VCard(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .clickable(enabled = false) {},
            padding = 20.dp,
        ) {
            Text(title, style = VTheme.type.h3.colored(c.ink))
            Spacer(Modifier.height(8.dp))
            Text(message, style = VTheme.type.body.colored(c.ink2))
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                    VButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        variant = VButtonVariant.Secondary,
                        size = VButtonSize.Md,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Box(Modifier.weight(1f)) {
                    VButton(
                        text = "Deactivate",
                        onClick = onConfirm,
                        variant = VButtonVariant.Destructive,
                        size = VButtonSize.Md,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
