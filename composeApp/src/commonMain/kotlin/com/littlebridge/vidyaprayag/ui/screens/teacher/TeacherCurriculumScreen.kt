package com.littlebridge.vidyaprayag.ui.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.CurriculumLesson
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.CurriculumUnit
import com.littlebridge.vidyaprayag.feature.teacher.presentation.TeacherCurriculumViewModel
import com.littlebridge.vidyaprayag.navigation.LocalAppNavigator
import com.littlebridge.vidyaprayag.ui.components.BaseScreen
import com.littlebridge.vidyaprayag.ui.components.VidyaPrayagCard
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TeacherCurriculumScreen() {
    val viewModel: TeacherCurriculumViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val navigator = LocalAppNavigator.current

    BaseScreen(
        onBackClick = { navigator.goBack() },
        immersiveTopBar = true
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(scrollModifier)
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Curriculum",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (state.units.isEmpty()) {
                item {
                    VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "No curriculum units available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(state.units) { unit ->
                    CurriculumUnitCard(unit = unit)
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun CurriculumUnitCard(unit: CurriculumUnit) {
    VidyaPrayagCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Unit ${unit.unitNumber}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.weight(1f))
                if (unit.isCompleted) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                unit.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                unit.subject,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (unit.lessons.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                unit.lessons.forEach { lesson ->
                    LessonRow(lesson = lesson)
                }
            }
        }
    }
}

@Composable
private fun LessonRow(lesson: CurriculumLesson) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (lesson.isCompleted) Icons.Default.CheckCircle
            else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (lesson.isCompleted) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            lesson.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (lesson.isCompleted) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
