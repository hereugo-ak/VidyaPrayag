package com.littlebridge.enrollplus.ui.v2.screens.teacher

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.littlebridge.enrollplus.feature.health.domain.model.HealthAlertDto
import com.littlebridge.enrollplus.feature.health.presentation.TeacherHealthAlertsViewModel
import com.littlebridge.enrollplus.ui.v2.components.VBackHeader
import com.littlebridge.enrollplus.ui.v2.components.VCard
import com.littlebridge.enrollplus.ui.v2.components.VIcons
import com.littlebridge.enrollplus.ui.v2.screens.VSectionHeader
import com.littlebridge.enrollplus.ui.v2.screens.VStateHost
import com.littlebridge.enrollplus.ui.v2.screens.collectAsStateV2
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TeacherHealthAlertsScreenV2(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: TeacherHealthAlertsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()

    Column(
        modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        VBackHeader(title = "Health Alerts", onBack = onBack)

        VStateHost(
            loading = state.isLoading,
            error = state.error,
            isEmpty = state.alerts.isEmpty(),
            onRetry = viewModel::load,
            emptyTitle = "No health alerts",
            emptyBody = "Students in your classes have no recorded allergies or chronic conditions.",
            emptyIcon = VIcons.Heart,
            modifier = Modifier.fillMaxSize(),
        ) {
            TeacherHealthAlertsContent(alerts = state.alerts)
        }
    }
}

@Composable
private fun TeacherHealthAlertsContent(alerts: List<HealthAlertDto>) {
    val c = VTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Students with allergies or chronic conditions in your classes",
            style = VTheme.type.caption.colored(c.ink2),
        )

        alerts.forEach { alert ->
            VCard {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(c.danger.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(VIcons.AlertTriangle, contentDescription = null, tint = c.dangerInk, modifier = Modifier.size(20.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(alert.studentName, style = VTheme.type.bodyStrong.colored(c.ink))
                        Text("${alert.className} - ${alert.section}", style = VTheme.type.caption.colored(c.ink3))
                        val allergies = parseJsonArray(alert.allergies)
                        val conditions = parseJsonArray(alert.chronicConditions)
                        if (allergies.isNotEmpty()) {
                            Text("Allergies: ${allergies.joinToString(", ")}", style = VTheme.type.caption.colored(c.dangerInk))
                        }
                        if (conditions.isNotEmpty()) {
                            Text("Conditions: ${conditions.joinToString(", ")}", style = VTheme.type.caption.colored(c.ink2))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

private fun parseJsonArray(json: String): List<String> {
    if (json.isBlank() || json == "[]") return emptyList()
    return runCatching {
        json
            .removeSurrounding("[", "]")
            .split(",")
            .map { it.trim().removeSurrounding("\"") }
            .filter { it.isNotBlank() }
    }.getOrDefault(emptyList())
}
