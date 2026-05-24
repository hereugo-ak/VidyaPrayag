package com.littlebridge.vidyaprayag.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.littlebridge.vidyaprayag.feature.admin.domain.model.OnboardingStep
import com.littlebridge.vidyaprayag.feature.admin.presentation.DashboardOnboardingStatus
import com.littlebridge.vidyaprayag.feature.admin.presentation.SchoolDashboardViewModel
import com.littlebridge.vidyaprayag.navigation.Destination
import com.littlebridge.vidyaprayag.navigation.LocalAppNavigator
import com.littlebridge.vidyaprayag.ui.components.*
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SchoolDashboardScreen() {
    val viewModel: SchoolDashboardViewModel = koinViewModel()
    val steps by viewModel.steps.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val onboardingStatus by viewModel.onboardingStatus.collectAsState()
    val adminName by viewModel.adminName.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val navigator = LocalAppNavigator.current

    // Re-fetch when the user lands on the dashboard (e.g. after finishing
    // the onboarding flow, the previous "/user/details" is stale).
    LaunchedEffect(Unit) { viewModel.refresh() }

    val onContinueOnboarding: () -> Unit = {
        val target = viewModel.firstPendingStep()?.serverKey
            ?.let { it.toOnboardingDestination() }
            ?: Destination.InstitutionalBasicOB
        navigator.navigateTo(target)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Orb Glows
        OrbGlow(modifier = Modifier.align(Alignment.TopEnd).offset(x = 100.dp, y = 50.dp))
        OrbGlow(modifier = Modifier.align(Alignment.BottomStart).offset(x = (-100).dp, y = (-100).dp))

        BaseScreen(
            bottomBar = {
                SchoolDashboardBottomBar(selectedTab = SchoolTab.HOME)
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
                    if (onboardingStatus == DashboardOnboardingStatus.COMPLETED) {
                        CampusLiveHeroCard(adminName = adminName)
                    } else {
                        OnboardingHeroCard(
                            adminName = adminName,
                            progress = progress,
                            statusLabel = onboardingStatus.shortLabel(),
                            isPrimaryActionLoading = isLoading,
                            primaryActionText = if (progress > 0f && progress < 1f) "Continue Onboarding" else "Start Onboarding",
                            onPrimaryAction = onContinueOnboarding
                        )
                    }
                }

                item {
                    SetupStepsHeader(stepCount = steps.size)
                }

                items(steps) { step ->
                    OnboardingStepItem(
                        step = step,
                        clickable = onboardingStatus != DashboardOnboardingStatus.COMPLETED && step.isEnabled,
                        onClick = {
                            navigator.navigateTo(step.serverKey.toOnboardingDestination())
                        }
                    )
                }

                item {
                    SupportSection(
                        onChatClick = { navigator.navigateTo(Destination.Messages) },
                        onWatchVideoClick = {
                            navigator.navigateTo(Destination.InstitutionalProfile)
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

private fun String.toOnboardingDestination(): Destination = when (uppercase()) {
    OnboardingStep.SERVER_KEY_BASIC -> Destination.InstitutionalBasicOB
    OnboardingStep.SERVER_KEY_BRANDING -> Destination.BrandingInfoOB
    OnboardingStep.SERVER_KEY_ACADEMIC -> Destination.AcademicInfoOB
    OnboardingStep.SERVER_KEY_REVIEW -> Destination.LaunchInfoOB
    else -> Destination.InstitutionalBasicOB
}

private fun DashboardOnboardingStatus.shortLabel(): String = when (this) {
    DashboardOnboardingStatus.NOT_STARTED -> "Pending"
    DashboardOnboardingStatus.IN_PROGRESS -> "In Progress"
    DashboardOnboardingStatus.COMPLETED -> "Live"
    DashboardOnboardingStatus.UNKNOWN -> "Pending"
}

@Composable
private fun OrbGlow(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(300.dp)
            .blur(100.dp)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                        Color.Transparent
                    )
                )
            )
    )
}

/**
 * Hero card for the IN_PROGRESS / NOT_STARTED states: shows real progress and
 * a Start/Continue Onboarding CTA that jumps to the first non-completed step.
 */
@Composable
private fun OnboardingHeroCard(
    adminName: String,
    progress: Float,
    statusLabel: String,
    isPrimaryActionLoading: Boolean,
    primaryActionText: String,
    onPrimaryAction: () -> Unit
) {
    VidyaPrayagCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colorScheme.primaryContainer,
        elevation = 8
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                "Welcome, $adminName",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Your institutional setup is ready to begin. Let\'s build your digital campus.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${(progress * 100).toInt()}% Onboarding Complete",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.secondaryContainer,
                trackColor = Color.White.copy(alpha = 0.1f),
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onPrimaryAction,
                enabled = !isPrimaryActionLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                shape = CircleShape,
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Text(primaryActionText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

/**
 * Hero card for the COMPLETED state: confirms the campus is live and points
 * the admin at the next thing they'd actually want to do.
 */
@Composable
private fun CampusLiveHeroCard(adminName: String) {
    VidyaPrayagCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colorScheme.primaryContainer,
        elevation = 8
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Welcome back, $adminName",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "Your campus is live on Vidya Prayag. All four onboarding steps are complete — manage admissions, announcements and academics from the menu below.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.75f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "100% Onboarding Complete",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Live",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { 1f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.secondaryContainer,
                trackColor = Color.White.copy(alpha = 0.1f),
            )
        }
    }
}

@Composable
private fun SetupStepsHeader(stepCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Setup Steps",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f),
            shape = CircleShape
        ) {
            Text(
                "$stepCount STEPS",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun OnboardingStepItem(
    step: OnboardingStep,
    clickable: Boolean,
    onClick: () -> Unit
) {
    VidyaPrayagCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (clickable) Modifier.clickable { onClick() } else Modifier)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                if (step.iconUrl != null) {
                    AsyncImage(
                        model = step.iconUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(
                        imageVector = step.serverKey.toMaterialIcon(),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    step.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    step.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Trailing status indicator
            StepStatusIndicator(step.status)
        }
    }
}

@Composable
private fun StepStatusIndicator(status: String) {
    when (status.uppercase()) {
        OnboardingStep.STATUS_COMPLETED -> {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        OnboardingStep.STATUS_LOCKED -> {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        else -> {
            // PENDING / unknown — hollow circle
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            )
        }
    }
}

private fun String.toMaterialIcon() = when (uppercase()) {
    OnboardingStep.SERVER_KEY_BASIC -> Icons.Default.School
    OnboardingStep.SERVER_KEY_BRANDING -> Icons.Default.Palette
    OnboardingStep.SERVER_KEY_ACADEMIC -> Icons.Default.HistoryEdu
    OnboardingStep.SERVER_KEY_REVIEW -> Icons.Default.RocketLaunch
    else -> Icons.Default.RocketLaunch
}

@Composable
private fun SupportSection(
    onChatClick: () -> Unit,
    onWatchVideoClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Whole card is tappable so "anywhere in the support row" → chat,
        // not just the small "CHAT" pill. Matches industrial-app convention.
        VidyaPrayagCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onChatClick() }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.SupportAgent,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Need help? Chat with an expert",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Open the messages inbox to reach support",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                Text(
                    "CHAT",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    modifier = Modifier.clickable { onChatClick() }
                )
            }
        }

        OutlinedButton(
            onClick = onWatchVideoClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Watch Onboarding Video",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
        }
    }
}
