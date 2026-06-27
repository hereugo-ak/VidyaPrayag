package com.littlebridge.enrollplus.ui.v2.screens.parent

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.littlebridge.enrollplus.feature.health.domain.model.HealthIncidentDto
import com.littlebridge.enrollplus.feature.health.domain.model.HealthProfileDto
import com.littlebridge.enrollplus.feature.health.domain.model.ImmunizationDto
import com.littlebridge.enrollplus.feature.health.domain.model.ParentHealthResponse
import com.littlebridge.enrollplus.feature.health.presentation.ParentHealthViewModel
import com.littlebridge.enrollplus.ui.v2.components.VBackHeader
import com.littlebridge.enrollplus.ui.v2.components.VBadge
import com.littlebridge.enrollplus.ui.v2.components.VBadgeTone
import com.littlebridge.enrollplus.ui.v2.components.VCard
import com.littlebridge.enrollplus.ui.v2.components.VIcons
import com.littlebridge.enrollplus.ui.v2.screens.VSectionHeader
import com.littlebridge.enrollplus.ui.v2.screens.VStateHost
import com.littlebridge.enrollplus.ui.v2.screens.collectAsStateV2
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ParentHealthScreenV2(
    childId: String,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ParentHealthViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    LaunchedEffect(childId) { viewModel.load(childId) }

    Column(
        modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        VBackHeader(title = "Health Records", onBack = onBack)

        VStateHost(
            loading = state.isLoading,
            error = state.error,
            isEmpty = false,
            onRetry = { viewModel.load(childId) },
            modifier = Modifier.fillMaxSize(),
        ) {
            ParentHealthContent(data = state.data)
        }
    }
}

@Composable
private fun ParentHealthContent(data: ParentHealthResponse?) {
    val c = VTheme.colors
    val profile = data?.profile
    val immunizations = data?.immunizations.orEmpty()
    val incidents = data?.incidents.orEmpty()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            data?.childName?.takeIf { it.isNotBlank() } ?: "Your child",
            style = VTheme.type.h2.colored(c.ink),
        )

        if (profile == null) {
            VCard {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(VIcons.Heart, contentDescription = null, tint = c.ink3, modifier = Modifier.size(32.dp))
                    Text("No health profile linked yet", style = VTheme.type.bodyStrong.colored(c.ink))
                    Text(
                        "Once the school adds health records for your child, they will appear here.",
                        style = VTheme.type.caption.colored(c.ink2),
                    )
                }
            }
        } else {
            ProfileCard(profile)
        }

        if (immunizations.isNotEmpty()) {
            VSectionHeader("Immunizations")
            immunizations.forEach { imm -> ImmunizationCard(imm) }
        }

        if (incidents.isNotEmpty()) {
            VSectionHeader("Health Incidents")
            incidents.forEach { inc -> IncidentCard(inc) }
        }

        if (profile == null && immunizations.isEmpty() && incidents.isEmpty()) {
            Spacer(Modifier.height(32.dp))
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ProfileCard(profile: HealthProfileDto) {
    val c = VTheme.colors
    VCard {
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            VSectionHeader("Health Profile")

            InfoRow("Blood Group", profile.bloodGroup)
            InfoRow("Height", profile.heightCm?.let { "${it} cm" })
            InfoRow("Weight", profile.weightKg?.let { "${it} kg" })

            val allergies = parseJsonArray(profile.allergies)
            val conditions = parseJsonArray(profile.chronicConditions)
            val meds = parseJsonArray(profile.medications)

            if (allergies.isNotEmpty()) {
                InfoRow("Allergies", allergies.joinToString(", "))
            }
            if (conditions.isNotEmpty()) {
                InfoRow("Chronic Conditions", conditions.joinToString(", "))
            }
            if (meds.isNotEmpty()) {
                InfoRow("Medications", meds.joinToString(", "))
            }

            if (!profile.emergencyContactName.isNullOrBlank() || !profile.emergencyContactPhone.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                VSectionHeader("Emergency Contact")
                InfoRow("Name", profile.emergencyContactName)
                InfoRow("Phone", profile.emergencyContactPhone)
            }

            if (!profile.doctorName.isNullOrBlank() || !profile.doctorPhone.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                VSectionHeader("Doctor")
                InfoRow("Name", profile.doctorName)
                InfoRow("Phone", profile.doctorPhone)
            }
        }
    }
}

@Composable
private fun ImmunizationCard(imm: ImmunizationDto) {
    val c = VTheme.colors
    VCard {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(imm.vaccineName, style = VTheme.type.bodyStrong.colored(c.ink))
                Text("Dose ${imm.doseNumber} · ${imm.dateAdministered}", style = VTheme.type.caption.colored(c.ink2))
                if (!imm.administeredBy.isNullOrBlank()) {
                    Text("By ${imm.administeredBy}", style = VTheme.type.caption.colored(c.ink3))
                }
                if (!imm.nextDueDate.isNullOrBlank()) {
                    Text("Next due: ${imm.nextDueDate}", style = VTheme.type.caption.colored(c.tealDeep))
                }
            }
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(c.teal.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(VIcons.Heart, contentDescription = null, tint = c.tealDeep, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun IncidentCard(inc: HealthIncidentDto) {
    val c = VTheme.colors
    VCard {
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(inc.date, style = VTheme.type.bodyStrong.colored(c.ink))
                val (badgeText, badgeTone) = when (inc.severity) {
                    "major" -> "MAJOR" to VBadgeTone.Danger
                    "moderate" -> "MODERATE" to VBadgeTone.Warning
                    else -> "MINOR" to VBadgeTone.Success
                }
                VBadge(text = badgeText, tone = badgeTone)
            }
            Text(inc.description, style = VTheme.type.body.colored(c.ink))
            if (!inc.treatment.isNullOrBlank()) {
                Text("Treatment: ${inc.treatment}", style = VTheme.type.caption.colored(c.ink2))
            }
            if (!inc.medicationGiven.isNullOrBlank()) {
                Text("Medication: ${inc.medicationGiven}", style = VTheme.type.caption.colored(c.ink2))
            }
            if (!inc.time.isNullOrBlank()) {
                Text("Time: ${inc.time}", style = VTheme.type.caption.colored(c.ink3))
            }
            if (inc.parentNotified) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(VIcons.Check, contentDescription = null, tint = c.tealDeep, modifier = Modifier.size(14.dp))
                    Text("Parent notified", style = VTheme.type.caption.colored(c.tealDeep))
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String?) {
    val c = VTheme.colors
    if (value.isNullOrBlank()) return
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = VTheme.type.caption.colored(c.ink3))
        Text(value, style = VTheme.type.body.colored(c.ink), fontWeight = FontWeight.Medium)
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
