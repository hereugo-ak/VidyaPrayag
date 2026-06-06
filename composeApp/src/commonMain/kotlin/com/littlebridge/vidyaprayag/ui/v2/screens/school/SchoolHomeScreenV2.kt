package com.littlebridge.vidyaprayag.ui.v2.screens.school

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.feature.admin.domain.model.OnboardingStep
import com.littlebridge.vidyaprayag.feature.admin.presentation.AnalyticsCardData
import com.littlebridge.vidyaprayag.feature.admin.presentation.AnalyticsDashboardViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.SchoolDashboardViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VBadge
import com.littlebridge.vidyaprayag.ui.v2.components.VBadgeTone
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VProgressBar
import com.littlebridge.vidyaprayag.ui.v2.components.VSparkline
import com.littlebridge.vidyaprayag.ui.v2.components.VStatusDot
import com.littlebridge.vidyaprayag.ui.v2.screens.VPortalHeader
import com.littlebridge.vidyaprayag.ui.v2.screens.VSectionHeader
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * SchoolHomeScreenV2 — admin "Home" tab, translated from Admin.tsx → Home.
 *
 * Onboarding-progress card + step list (from [SchoolDashboardViewModel]) plus the analytics glance
 * (cards + performance sparkline) from [AnalyticsDashboardViewModel]. Both VMs already exist and are
 * registered in Koin; many downstream admin routes are local-only stubs per the master doc §5.3 —
 * those tabs render `VComingSoon` rather than fabricated data.
 */
@Composable
fun SchoolHomeScreenV2(
    modifier: Modifier = Modifier,
    dashboardVm: SchoolDashboardViewModel = koinViewModel(),
    analyticsVm: AnalyticsDashboardViewModel = koinViewModel(),
    onOpenNotifications: () -> Unit = {},
    onOpenCalendar: () -> Unit = {},
) {
    val c = VTheme.colors
    val d = VTheme.dimens
    val steps by dashboardVm.steps.collectAsStateV2()
    val progress by dashboardVm.progress.collectAsStateV2()
    val adminName by dashboardVm.adminName.collectAsStateV2()
    val analytics by analyticsVm.state.collectAsStateV2()

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = d.md, vertical = d.md),
        verticalArrangement = Arrangement.spacedBy(d.md),
    ) {
        VPortalHeader(
            name = adminName,
            subtitle = "School admin",
            trailing = {
                Row(horizontalArrangement = Arrangement.spacedBy(d.sm)) {
                    HeaderIconButton(VIcons.Calendar, "Academic calendar", onClick = onOpenCalendar)
                    HeaderIconButton(VIcons.Bell, "Notifications", onClick = onOpenNotifications)
                }
            },
        )

        // Onboarding progress
        VCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("SETUP PROGRESS", style = VTheme.type.label.colored(c.ink3))
                Text("${(progress.coerceIn(0f, 1f) * 100).toInt()}%", style = VTheme.type.dataSm.colored(c.tealDeep))
            }
            Spacer(Modifier.height(d.xs))
            VProgressBar(value = progress.coerceIn(0f, 1f) * 100f, tone = VBadgeTone.Success)
        }

        VSectionHeader("ONBOARDING STEPS")
        steps.forEach { StepRow(it) }

        // Analytics glance
        if (analytics.cards.isNotEmpty()) {
            VSectionHeader("ANALYTICS")
            analytics.cards.chunked(2).forEach { rowCards ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(d.sm)) {
                    rowCards.forEach { AnalyticsCard(it, Modifier.weight(1f)) }
                    if (rowCards.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        if (analytics.performanceTrend.isNotEmpty()) {
            VCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("PERFORMANCE", style = VTheme.type.label.colored(c.ink3))
                    Text(analytics.currentGrowth, style = VTheme.type.dataSm.colored(c.successInk))
                }
                Spacer(Modifier.height(d.sm))
                VSparkline(values = analytics.performanceTrend, modifier = Modifier.fillMaxWidth().height(64.dp))
            }
        }

        dashboardVm.errorMessage.collectAsStateV2().value?.let {
            Text(it, style = VTheme.type.caption.colored(c.dangerInk))
        }
        Spacer(Modifier.height(d.xl))
    }
}

@Composable
private fun HeaderIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val c = VTheme.colors
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(c.cream)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = c.ink, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun StepRow(step: OnboardingStep) {
    val c = VTheme.colors
    val tone = when (step.status.uppercase()) {
        OnboardingStep.STATUS_COMPLETED -> VBadgeTone.Success
        OnboardingStep.STATUS_LOCKED -> VBadgeTone.Neutral
        else -> VBadgeTone.Warning
    }
    val dotColor = when (step.status.uppercase()) {
        OnboardingStep.STATUS_COMPLETED -> c.successInk
        OnboardingStep.STATUS_LOCKED -> c.placeholder
        else -> c.warningInk
    }
    VCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VTheme.dimens.md)) {
            VStatusDot(color = dotColor)
            Column(Modifier.weight(1f)) {
                Text(step.title, style = VTheme.type.h4.colored(c.ink))
                Text(step.description, style = VTheme.type.caption.colored(c.ink3))
            }
            VBadge(text = step.status.uppercase(), tone = tone)
        }
    }
}

@Composable
private fun AnalyticsCard(card: AnalyticsCardData, modifier: Modifier = Modifier) {
    val c = VTheme.colors
    VCard(modifier = modifier) {
        Text(card.value, style = VTheme.type.dataLg.colored(c.ink))
        Text(card.title, style = VTheme.type.label.colored(c.ink3))
        if (!card.subValue.isBlank()) {
            Text(card.subValue, style = VTheme.type.caption.colored(c.ink2))
        }
    }
}
