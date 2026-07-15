package com.littlebridge.vidyaprayag.ui.screens.teacher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.TeacherProfileResponse
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherProfileViewModel
import com.littlebridge.vidyaprayag.ui.components.BaseScreen
import com.littlebridge.vidyaprayag.ui.components.TeacherBottomBar
import com.littlebridge.vidyaprayag.ui.components.TeacherTab
import com.littlebridge.vidyaprayag.ui.components.VidyaPrayagCard
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TeacherProfileScreen() {
    val viewModel: TeacherProfileViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()

    BaseScreen(
        bottomBar = { TeacherBottomBar(selectedTab = TeacherTab.PROFILE) }
    ) { paddingValues, scrollModifier ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@BaseScreen
        }

        if (state.error != null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        state.error!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { viewModel.refresh() }) { Text("Retry") }
                }
            }
            return@BaseScreen
        }

        val profile = state.profile
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(scrollModifier)
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Profile",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (profile != null) {
                item { ProfileHeaderCard(profile = profile) }
                item { ProfileDetailsCard(profile = profile) }
                item { ProfileStatsCard(profile = profile) }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun ProfileHeaderCard(profile: TeacherProfileResponse) {
    VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                profile.teacherName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                profile.teacherSubject,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun ProfileDetailsCard(profile: TeacherProfileResponse) {
    VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            ProfileRow(label = "Email", value = profile.email)
            ProfileRow(label = "Phone", value = profile.phone)
            ProfileRow(label = "Joining Date", value = profile.joiningDate)
            ProfileRow(label = "Salary", value = profile.salary)
        }
    }
}

@Composable
private fun ProfileStatsCard(profile: TeacherProfileResponse) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatItem(
            modifier = Modifier.weight(1f),
            label = "Classes",
            value = profile.classesTaught.toString()
        )
        StatItem(
            modifier = Modifier.weight(1f),
            label = "Students",
            value = profile.totalStudents.toString()
        )
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatItem(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    VidyaPrayagCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
