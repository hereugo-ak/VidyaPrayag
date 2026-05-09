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
    val navigator = LocalAppNavigator.current

    Box(modifier = Modifier.fillMaxSize()) {
        // Orb Glows
        OrbGlow(modifier = Modifier.align(Alignment.TopEnd).offset(x = 100.dp, y = 50.dp))
        OrbGlow(modifier = Modifier.align(Alignment.BottomStart).offset(x = (-100).dp, y = (-100).dp))

        BaseScreen(
            bottomBar = {
                EduTrustBottomBar(
                    items = listOf(
                        BottomNavItem("Info", Icons.Default.Info, isSelected = true),
                        BottomNavItem("Brand", Icons.Default.PhotoLibrary),
                        BottomNavItem("Fees", Icons.Default.Payments),
                        BottomNavItem("Study", Icons.Default.School),
                        BottomNavItem("Launch", Icons.Default.CheckCircle)
                    )
                )
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
                    WelcomeHeroCard(
                        progress = progress,
                        onStartOnboarding = { navigator.navigateTo(Destination.BasicOnboarding) }
                    )
                }

                item {
                    SetupStepsHeader(stepCount = steps.size)
                }

                items(steps) { step ->
                    OnboardingStepItem(step = step)
                }

                item {
                    SupportSection()
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
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

@Composable
private fun WelcomeHeroCard(progress: Float, onStartOnboarding: () -> Unit) {
    EduTrustCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colorScheme.primaryContainer,
        elevation = 8
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                "Welcome, Admin",
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
                    "Pending",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.secondaryContainer,
                trackColor = Color.White.copy(alpha = 0.1f),
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onStartOnboarding,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                shape = CircleShape,
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Text("Start Onboarding", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
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
private fun OnboardingStepItem(step: OnboardingStep) {
    EduTrustCard(
        modifier = Modifier.fillMaxWidth()
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
                        imageVector = Icons.Default.RocketLaunch,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(32.dp)
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

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            )
        }
    }
}

@Composable
private fun SupportSection() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        EduTrustCard(
            modifier = Modifier.fillMaxWidth()
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
                        "Available 24/7 for institutions",
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
                    modifier = Modifier.clickable { }
                )
            }
        }

        OutlinedButton(
            onClick = {},
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
