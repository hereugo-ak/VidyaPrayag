package com.littlebridge.vidyaprayag.ui.v2.screens.school

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.feature.admin.domain.model.TeacherProfileDto
import com.littlebridge.vidyaprayag.feature.admin.presentation.TeacherProfileUiState
import com.littlebridge.vidyaprayag.feature.admin.presentation.TeacherProfileViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatar
import com.littlebridge.vidyaprayag.ui.v2.components.VBackHeader
import com.littlebridge.vidyaprayag.ui.v2.components.VBadge
import com.littlebridge.vidyaprayag.ui.v2.components.VBadgeTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonVariant
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VConfirmDialog
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.screens.VSectionHeader
import com.littlebridge.vidyaprayag.ui.v2.screens.VStateHost
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * RA-45: TeacherProfileScreenV2 — a single teacher's detail (assignments,
 * class/subject coverage) for the admin. [teacherId] is passed by the caller
 * and loaded via [TeacherProfileViewModel.load] in a LaunchedEffect. Three
 * states via [VStateHost] (LAW 3). Portal overlay — back returns to People.
 */
@Composable
fun TeacherProfileScreenV2(
    teacherId: String,
    onBack: () -> Unit = {},
    // RA-S17: called after a successful soft-delete so the host can pop back to
    // People and refresh the roster.
    onRemoved: () -> Unit = onBack,
    modifier: Modifier = Modifier,
    viewModel: TeacherProfileViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    LaunchedEffect(teacherId) { viewModel.load(teacherId) }
    // RA-S17: when the VM confirms removal, leave the profile.
    LaunchedEffect(state.removed) { if (state.removed) onRemoved() }

    Column(modifier.fillMaxSize()) {
        VBackHeader(title = "Teacher", onBack = onBack)
        TeacherProfileContent(
            state = state,
            onRetry = viewModel::retry,
            onRemove = { viewModel.remove(teacherId) },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun TeacherProfileContent(
    state: TeacherProfileUiState,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    var confirmRemove by remember { mutableStateOf(false) }

    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VStateHost(
            loading = state.isLoading,
            error = state.error,
            isEmpty = state.profile == null && !state.isLoading && state.error == null,
            emptyTitle = "No profile",
            emptyBody = "This teacher's record could not be found.",
            emptyIcon = VIcons.User,
            onRetry = onRetry,
        ) {
            val p = state.profile ?: return@VStateHost
            TeacherProfileBody(p)

            // RA-S17: destructive action lives INSIDE the profile (not as a
            // direct list-row button), behind a confirm dialog.
            Spacer(Modifier.height(12.dp))
            state.removeError?.let { err ->
                Text(err, style = VTheme.type.caption.colored(c.danger))
                Spacer(Modifier.height(8.dp))
            }
            VButton(
                text = "Remove from school",
                onClick = { confirmRemove = true },
                variant = VButtonVariant.Destructive,
                full = true,
                enabled = !state.isRemoving,
                loading = state.isRemoving,
                leading = { Icon(VIcons.Close, contentDescription = null, modifier = Modifier.size(16.dp)) },
            )
        }
    }

    VConfirmDialog(
        visible = confirmRemove,
        title = "Remove teacher",
        message = "Remove ${state.profile?.name ?: "this teacher"} from your school? " +
            "They will lose access immediately. This can be reversed by re-adding them.",
        confirmLabel = "Remove",
        icon = VIcons.AlertTriangle,
        onConfirm = { confirmRemove = false; onRemove() },
        onDismiss = { confirmRemove = false },
    )
}

@Composable
private fun TeacherProfileBody(p: TeacherProfileDto) {
    val c = VTheme.colors

    VCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            VAvatar(name = p.name, size = 56.dp)
            Column(Modifier.weight(1f)) {
                Text(p.name, style = VTheme.type.h3.colored(c.ink))
                val contact = listOfNotNull(p.email?.takeIf { it.isNotBlank() }, p.phone?.takeIf { it.isNotBlank() }).joinToString(" · ")
                if (contact.isNotBlank()) {
                    Text(contact, style = VTheme.type.caption.colored(c.ink2))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatPillT("Classes", p.classCount, Modifier.weight(1f))
            StatPillT("Subjects", p.subjectCount, Modifier.weight(1f))
        }
    }

    VSectionHeader(title = "ASSIGNMENTS")
    if (p.assignments.isEmpty()) {
        VCard { Text("No class or subject assignments yet.", style = VTheme.type.body.colored(c.ink2)) }
    } else {
        p.assignments.forEach { a ->
            VCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(a.subject, style = VTheme.type.bodyStrong.colored(c.ink))
                        Text("${a.className} · Sec ${a.section}", style = VTheme.type.caption.colored(c.ink2))
                    }
                    VBadge(text = a.section, tone = VBadgeTone.Neutral)
                }
            }
        }
    }
}

@Composable
private fun StatPillT(label: String, value: Int, modifier: Modifier = Modifier) {
    val c = VTheme.colors
    VCard(modifier = modifier) {
        Column {
            Text(value.toString(), style = VTheme.type.dataSm.colored(c.ink))
            Text(label, style = VTheme.type.label.colored(c.ink3))
        }
    }
}
