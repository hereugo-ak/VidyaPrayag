package com.littlebridge.enrollplus.ui.v2.screens.school

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import com.littlebridge.enrollplus.feature.alumni.domain.model.Alumni
import com.littlebridge.enrollplus.feature.alumni.domain.repository.AlumniRepository
import com.littlebridge.enrollplus.ui.v2.components.VBackHeader
import com.littlebridge.enrollplus.ui.v2.components.VCard
import com.littlebridge.enrollplus.ui.v2.components.VIcons
import com.littlebridge.enrollplus.ui.v2.components.VTopTabs
import com.littlebridge.enrollplus.ui.v2.screens.VSectionHeader
import com.littlebridge.enrollplus.ui.v2.screens.VStateHost
import com.littlebridge.enrollplus.ui.v2.screens.collectAsStateV2
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import org.koin.compose.koinInject

@Composable
fun AlumniDetailScreen(
    alumniId: String,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    repository: AlumniRepository = koinInject(),
    prefs: PreferenceRepository = koinInject(),
) {
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var alumni by remember { mutableStateOf<Alumni?>(null) }
    var subTab by remember { mutableStateOf("Profile") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(alumniId) {
        scope.launch {
            val token = prefs.getUserToken().first()
            if (token.isNullOrBlank()) { error = "Not signed in"; isLoading = false; return@launch }
            when (val result = repository.getAlumni(token, alumniId)) {
                is NetworkResult.Success -> { alumni = result.data.data; isLoading = false }
                is NetworkResult.Error -> { error = result.message; isLoading = false }
                is NetworkResult.ConnectionError -> { error = "Connection error"; isLoading = false }
            }
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        VBackHeader(title = "Alumni Detail", onBack = onBack)

        val a = alumni
        VStateHost(
            loading = isLoading,
            error = error,
            isEmpty = a == null,
            emptyTitle = "Alumni not found",
            onRetry = { isLoading = true; error = null },
        ) {
            val data = a!!
            VTopTabs(
                tabs = listOf("Profile", "Career", "Donations"),
                selected = subTab,
                onSelect = { subTab = it },
            )

            when (subTab) {
                "Profile" -> {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        VCard(modifier = Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(data.name, style = VTheme.type.h2, color = VTheme.colors.ink)
                                Text("Batch ${data.graduationYear}", style = VTheme.type.body, color = VTheme.colors.ink3)
                                if (data.isFeatured) Text("★ Featured", style = VTheme.type.caption, color = VTheme.colors.accent)
                                if (data.verificationStatus != "approved") {
                                    Text("Status: ${data.verificationStatus}", style = VTheme.type.caption, color = VTheme.colors.danger)
                                }
                            }
                        }

                        VSectionHeader("Contact")
                        data.email?.let { DetailRow("Email", it) }
                        data.phone?.let { DetailRow("Phone", it) }
                        data.city?.let { DetailRow("City", it) }
                        data.linkedinUrl?.let { DetailRow("LinkedIn", it) }

                        VSectionHeader("Professional")
                        data.currentProfession?.let { DetailRow("Profession", it) }
                        data.company?.let { DetailRow("Company", it) }
                        data.skills?.let { DetailRow("Skills", it) }
                        data.achievements?.let { DetailRow("Achievements", it) }

                        if (data.isMentor) {
                            VSectionHeader("Mentorship")
                            DetailRow("Mentor", "Yes")
                            data.mentorExpertise?.let { DetailRow("Expertise", it) }
                        }

                        VSectionHeader("Privacy")
                        DetailRow("Visibility", data.visibilityLevel)
                        DetailRow("Show Phone", if (data.showPhone) "Yes" else "No")
                        DetailRow("Show Email", if (data.showEmail) "Yes" else "No")
                        DetailRow("Profile Completeness", "${data.profileCompleteness}%")
                    }
                }
                "Career" -> {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (data.careerHistory.isEmpty()) {
                            VStateHost(loading = false, error = null, isEmpty = true, emptyTitle = "No career history") {}
                        } else {
                            data.careerHistory.forEach { career ->
                                VCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(career.jobTitle, style = VTheme.type.body, fontWeight = FontWeight.SemiBold, color = VTheme.colors.ink)
                                        Text(career.company, style = VTheme.type.body, color = VTheme.colors.ink3)
                                        career.industry?.let { Text(it, style = VTheme.type.caption, color = VTheme.colors.ink3) }
                                        val dateRange = buildString {
                                            career.startDate?.let { append(it) }
                                            append(" — ")
                                            if (career.isCurrent) append("Present") else career.endDate?.let { append(it) }
                                        }
                                        Text(dateRange, style = VTheme.type.caption, color = VTheme.colors.ink3)
                                    }
                                }
                            }
                        }
                    }
                }
                "Donations" -> {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        var donations by remember { mutableStateOf<List<com.littlebridge.enrollplus.feature.alumni.domain.model.AlumniDonation>?>(null) }
                        var donationsLoading by remember { mutableStateOf(true) }
                        LaunchedEffect(alumniId) {
                            scope.launch {
                                val token = prefs.getUserToken().first()
                                if (token.isNullOrBlank()) { donationsLoading = false; return@launch }
                                when (val result = repository.getAlumniDonations(token, alumniId)) {
                                    is NetworkResult.Success -> { donations = result.data.data ?: emptyList(); donationsLoading = false }
                                    is NetworkResult.Error -> { donationsLoading = false }
                                    is NetworkResult.ConnectionError -> { donationsLoading = false }
                                }
                            }
                        }
                        VStateHost(
                            loading = donationsLoading,
                            error = null,
                            isEmpty = donations.isNullOrEmpty(),
                            emptyTitle = "No donations recorded",
                        ) {
                            donations!!.forEach { donation ->
                                VCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("₹${donation.amount.toInt()}", style = VTheme.type.body, fontWeight = FontWeight.SemiBold, color = VTheme.colors.ink)
                                        Text("Date: ${donation.donationDate}", style = VTheme.type.caption, color = VTheme.colors.ink3)
                                        donation.campaignTitle?.let { Text("Campaign: $it", style = VTheme.type.caption, color = VTheme.colors.ink3) }
                                        donation.paymentMode?.let { Text("Mode: $it", style = VTheme.type.caption, color = VTheme.colors.ink3) }
                                        if (donation.is80gEligible) {
                                            Text("80G Eligible • Receipt: ${donation.receiptNumber ?: "Pending"}", style = VTheme.type.caption, color = VTheme.colors.accent)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    VCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(label, style = VTheme.type.caption, color = VTheme.colors.ink3)
            Text(value, style = VTheme.type.body, color = VTheme.colors.ink)
        }
    }
}
