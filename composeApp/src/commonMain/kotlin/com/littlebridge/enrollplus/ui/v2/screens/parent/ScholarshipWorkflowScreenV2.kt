package com.littlebridge.enrollplus.ui.v2.screens.parent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import com.littlebridge.enrollplus.feature.scholarship.domain.model.ApplyScholarshipRequest
import com.littlebridge.enrollplus.feature.scholarship.domain.model.ScholarshipScheme
import com.littlebridge.enrollplus.feature.scholarship.presentation.ScholarshipViewModel
import com.littlebridge.enrollplus.ui.v2.components.VBackHeader
import com.littlebridge.enrollplus.ui.v2.components.VBadge
import com.littlebridge.enrollplus.ui.v2.components.VBadgeTone
import com.littlebridge.enrollplus.ui.v2.components.VButton
import com.littlebridge.enrollplus.ui.v2.components.VButtonSize
import com.littlebridge.enrollplus.ui.v2.components.VButtonVariant
import com.littlebridge.enrollplus.ui.v2.components.VCard
import com.littlebridge.enrollplus.ui.v2.components.VIcons
import com.littlebridge.enrollplus.ui.v2.components.VInput
import com.littlebridge.enrollplus.ui.v2.components.VProgressBar
import com.littlebridge.enrollplus.ui.v2.screens.VSectionHeader
import com.littlebridge.enrollplus.ui.v2.screens.VStateHost
import com.littlebridge.enrollplus.ui.v2.screens.collectAsStateV2
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ScholarshipWorkflowScreenV2(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ScholarshipViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    var showApplyForm by remember { mutableStateOf(false) }
    var selectedScheme by remember { mutableStateOf<ScholarshipScheme?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadParentScholarships()
    }

    LaunchedEffect(state.infoMessage) {
        if (state.infoMessage != null) {
            showApplyForm = false
            selectedScheme = null
            viewModel.clearMessages()
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .navigationBarsPadding()
    ) {
        VBackHeader(title = "Scholarships", onBack = onBack)

        VStateHost(
            loading = state.isLoading,
            error = state.error,
            isEmpty = state.parentScholarships.isEmpty() && state.parentApplications.isEmpty(),
            emptyTitle = "No scholarships yet",
            emptyBody = "Scholarship opportunities will appear here as your school publishes them.",
            emptyIcon = VIcons.Sparkles,
            onRetry = { viewModel.loadParentScholarships() },
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Gamification card
                GamificationCard(
                    profileStrength = state.gamification.profileStrength,
                    streakDays = state.gamification.streakDays,
                    currentLevel = state.gamification.currentLevel,
                    totalApplications = state.gamification.totalApplications,
                    approvedCount = state.gamification.approvedCount,
                    totalAwarded = state.gamification.totalAwarded,
                )

                // Available scholarships
                if (state.parentScholarships.isNotEmpty()) {
                    VSectionHeader(title = "AVAILABLE SCHOLARSHIPS (${state.parentScholarships.size})")
                    state.parentScholarships.forEach { s ->
                        GamifiedScholarshipCard(
                            scheme = s,
                            onApply = {
                                selectedScheme = s
                                showApplyForm = true
                            },
                        )
                    }
                }

                // My applications
                if (state.parentApplications.isNotEmpty()) {
                    VSectionHeader(title = "MY APPLICATIONS (${state.parentApplications.size})")
                    state.parentApplications.forEach { app ->
                        ApplicationStatusCard(application = app)
                    }
                }
            }
        }
    }

    // Apply form overlay
    AnimatedVisibility(
        visible = showApplyForm && selectedScheme != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        selectedScheme?.let { scheme ->
            ApplyScholarshipForm(
                scheme = scheme,
                onApply = { childId, documents, text ->
                    viewModel.applyScholarship(
                        ApplyScholarshipRequest(
                            scholarshipId = scheme.id,
                            childId = childId,
                            documents = documents,
                            applicationText = text,
                        )
                    )
                },
                onDismiss = {
                    showApplyForm = false
                    selectedScheme = null
                },
            )
        }
    }
}

@Composable
private fun GamificationCard(
    profileStrength: Int,
    streakDays: Int,
    currentLevel: Int,
    totalApplications: Int,
    approvedCount: Int,
    totalAwarded: Double,
) {
    val c = VTheme.colors
    VCard {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Profile Strength", style = VTheme.type.label.colored(c.ink3))
                Spacer(Modifier.height(4.dp))
                Text(
                    "$profileStrength%",
                    style = VTheme.type.dataLg.colored(c.ink),
                )
            }
            // Level badge with gradient feel
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(c.accent.copy(alpha = 0.15f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    "LVL $currentLevel",
                    style = VTheme.type.bodyStrong,
                    color = c.accent,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        VProgressBar(value = profileStrength.toFloat().coerceIn(0f, 100f))
        Spacer(Modifier.height(12.dp))

        // Stats row
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(label = "Applications", value = totalApplications.toString(), modifier = Modifier.weight(1f))
            StatTile(label = "Approved", value = approvedCount.toString(), modifier = Modifier.weight(1f))
            if (totalAwarded > 0) {
                StatTile(label = "Awarded", value = "₹${totalAwarded.toLong()}", modifier = Modifier.weight(1f))
            } else {
                StatTile(label = "Day Streak", value = streakDays.toString(), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    val c = VTheme.colors
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(c.cream)
            .padding(12.dp),
    ) {
        Text(label, style = VTheme.type.label.colored(c.ink3))
        Spacer(Modifier.height(4.dp))
        Text(value, style = VTheme.type.dataLg.colored(c.ink))
    }
}

@Composable
private fun GamifiedScholarshipCard(
    scheme: ScholarshipScheme,
    onApply: () -> Unit,
) {
    val c = VTheme.colors
    val categoryTone = when (scheme.category.lowercase()) {
        "full funding" -> VBadgeTone.Success
        "merit based" -> VBadgeTone.Accent
        "need based" -> VBadgeTone.Warning
        "international" -> VBadgeTone.Warning
        else -> VBadgeTone.Neutral
    }
    val typeTone = when (scheme.scholarshipType) {
        "full_waiver" -> VBadgeTone.Success
        "partial_waiver" -> VBadgeTone.Warning
        else -> VBadgeTone.Accent
    }

    VCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VBadge(text = scheme.category, tone = categoryTone)
            VBadge(text = scheme.scholarshipType.replace("_", " "), tone = typeTone)
            if (scheme.isCritical) {
                VBadge(text = "HOT", tone = VBadgeTone.Danger)
            }
            if (scheme.isRenewable) {
                VBadge(text = "Renewable", tone = VBadgeTone.Neutral)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(scheme.title, style = VTheme.type.h3.colored(c.ink))
        if (scheme.description.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(scheme.description, style = VTheme.type.body.colored(c.ink2))
        }
        Spacer(Modifier.height(10.dp))

        if (scheme.eligibilityCriteria.isNotBlank()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(VIcons.Sparkles, contentDescription = null, tint = c.ink3, modifier = Modifier.size(14.dp))
                Text("Eligibility: ", style = VTheme.type.label.colored(c.ink3))
                Text(scheme.eligibilityCriteria, style = VTheme.type.caption.colored(c.ink2))
            }
            Spacer(Modifier.height(8.dp))
        }

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("Award", style = VTheme.type.label.colored(c.ink3))
                Spacer(Modifier.height(2.dp))
                Text(scheme.amount, style = VTheme.type.dataLg.colored(c.ink))
            }
            val endDate = scheme.endDate
            if (endDate != null && endDate.isNotBlank()) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("Apply by", style = VTheme.type.label.colored(c.ink3))
                    Spacer(Modifier.height(2.dp))
                    Text(endDate, style = VTheme.type.dataSm.colored(c.ink2))
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        VButton(
            text = "Apply Now",
            onClick = onApply,
            variant = VButtonVariant.Primary,
            size = VButtonSize.Md,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ApplicationStatusCard(
    application: com.littlebridge.enrollplus.feature.scholarship.domain.model.ScholarshipApplication,
) {
    val c = VTheme.colors
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

        if (application.remarks?.isNotBlank() == true) {
            Spacer(Modifier.height(8.dp))
            Text("Remarks: ${application.remarks}", style = VTheme.type.caption.colored(c.ink3))
        }

        if (application.status == "DISBURSED" && application.disbursementAmount != null) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Disbursed: ₹${application.disbursementAmount}", style = VTheme.type.caption.colored(c.success))
                Text("Ref: ${application.disbursementReference ?: "—"}", style = VTheme.type.caption.colored(c.ink3))
            }
        }
    }
}

@Composable
private fun ApplyScholarshipForm(
    scheme: ScholarshipScheme,
    onApply: (String, List<String>, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var childId by remember { mutableStateOf("") }
    var documentUrl by remember { mutableStateOf("") }
    var documents by remember { mutableStateOf(listOf<String>()) }
    var applicationText by remember { mutableStateOf("") }
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
                .padding(16.dp)
                .fillMaxWidth()
                .clickable(enabled = false) {},
        ) {
            Text("Apply for Scholarship", style = VTheme.type.h3.colored(c.ink))
            Spacer(Modifier.height(4.dp))
            Text(scheme.title, style = VTheme.type.body.colored(c.ink2))
            Spacer(Modifier.height(16.dp))

            VInput(value = childId, onValueChange = { childId = it }, label = "Child ID *", modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))

            Text("Documents (URLs)", style = VTheme.type.label.colored(c.ink3))
            Spacer(Modifier.height(4.dp))
            documents.forEach { doc ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(doc.takeLast(30), style = VTheme.type.caption.colored(c.ink2), modifier = Modifier.weight(1f))
                    Text("✕", style = VTheme.type.body, color = c.danger, modifier = Modifier.clickable {
                        documents = documents.filter { it != doc }
                    })
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                    VInput(value = documentUrl, onValueChange = { documentUrl = it }, label = "Document URL", modifier = Modifier.fillMaxWidth())
                }
                Box(Modifier.align(Alignment.Bottom)) {
                    VButton(
                        text = "Add",
                        onClick = {
                            if (documentUrl.isNotBlank()) {
                                documents = documents + documentUrl
                                documentUrl = ""
                            }
                        },
                        variant = VButtonVariant.Secondary,
                        size = VButtonSize.Sm,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            VInput(
                value = applicationText,
                onValueChange = { applicationText = it },
                label = "Application Text (optional)",
                modifier = Modifier.fillMaxWidth(),
            )

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
                        text = "Submit",
                        onClick = {
                            if (childId.isNotBlank()) {
                                onApply(childId, documents, applicationText.ifBlank { null })
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
