package com.littlebridge.vidyaprayag.ui.v2.screens.school

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.feature.admin.presentation.SchoolProfileState
import com.littlebridge.vidyaprayag.feature.admin.presentation.SchoolProfileViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VBackHeader
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonVariant
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VInput
import com.littlebridge.vidyaprayag.ui.v2.screens.VSectionHeader
import com.littlebridge.vidyaprayag.ui.v2.screens.VStateHost
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * RA-47: EditSchoolProfileScreenV2 — the admin edits the live `schools` row
 * (the institutional record) via [SchoolProfileViewModel]
 * (`GET /api/v1/school/profile`, `PUT /api/v1/school/profile`). The server
 * resolves school_id from the JWT and enforces school-admin (never trusts the
 * body), so an admin can only ever edit their own school.
 *
 * Three states via [VStateHost] (LAW 3): loading while the row loads,
 * error+retry on a fatal load failure, and the editable form once loaded.
 * Save errors / confirmations are surfaced inline (dangerInk / successInk)
 * rather than failing silently. Portal overlay — back returns to Settings.
 */
@Composable
fun EditSchoolProfileScreenV2(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SchoolProfileViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()

    Column(modifier.fillMaxSize()) {
        VBackHeader(title = "Institutional Profile", onBack = onBack)
        EditSchoolProfileContent(
            state = state,
            onName = viewModel::onName,
            onBoard = viewModel::onBoard,
            onMedium = viewModel::onMedium,
            onSchoolGender = viewModel::onSchoolGender,
            onContactPhone = viewModel::onContactPhone,
            onContactEmail = viewModel::onContactEmail,
            onPrincipalName = viewModel::onPrincipalName,
            onPrincipalPhone = viewModel::onPrincipalPhone,
            onPrincipalEmail = viewModel::onPrincipalEmail,
            onFullAddress = viewModel::onFullAddress,
            onCity = viewModel::onCity,
            onDistrict = viewModel::onDistrict,
            onState = viewModel::onState,
            onPincode = viewModel::onPincode,
            onSave = viewModel::save,
            onRetry = viewModel::load,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun EditSchoolProfileContent(
    state: SchoolProfileState,
    onName: (String) -> Unit,
    onBoard: (String) -> Unit,
    onMedium: (String) -> Unit,
    onSchoolGender: (String) -> Unit,
    onContactPhone: (String) -> Unit,
    onContactEmail: (String) -> Unit,
    onPrincipalName: (String) -> Unit,
    onPrincipalPhone: (String) -> Unit,
    onPrincipalEmail: (String) -> Unit,
    onFullAddress: (String) -> Unit,
    onCity: (String) -> Unit,
    onDistrict: (String) -> Unit,
    onState: (String) -> Unit,
    onPincode: (String) -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VStateHost(
            loading = state.isLoading,
            error = state.loadError,
            // The form always has fields to show once loaded; never "empty".
            isEmpty = false,
            onRetry = onRetry,
        ) {
            Text(
                "Update your school's public record. These details appear on your " +
                    "discovery listing and on documents shared with parents.",
                style = VTheme.type.caption.colored(c.ink3),
            )

            // ── Identity ─────────────────────────────────────────────────────
            VSectionHeader(title = "IDENTITY")
            VCard {
                VInput(state.name, onName, label = "School name", placeholder = "e.g. Little Bridge Public School", modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                VInput(state.board, onBoard, label = "Board", placeholder = "e.g. CBSE / ICSE / State", modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                VInput(state.medium, onMedium, label = "Medium of instruction", placeholder = "e.g. English", modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                VInput(state.schoolGender, onSchoolGender, label = "School type", placeholder = "co_ed / boys / girls", modifier = Modifier.fillMaxWidth())
            }

            // ── Contact ──────────────────────────────────────────────────────
            VSectionHeader(title = "CONTACT")
            VCard {
                VInput(state.contactPhone, onContactPhone, label = "Contact phone", placeholder = "10-digit number", keyboardType = KeyboardType.Phone, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                VInput(state.contactEmail, onContactEmail, label = "Contact email", placeholder = "office@school.edu", keyboardType = KeyboardType.Email, modifier = Modifier.fillMaxWidth())
            }

            // ── Principal ────────────────────────────────────────────────────
            VSectionHeader(title = "PRINCIPAL")
            VCard {
                VInput(state.principalName, onPrincipalName, label = "Principal name", placeholder = "Full name", modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                VInput(state.principalPhone, onPrincipalPhone, label = "Principal phone", placeholder = "10-digit number", keyboardType = KeyboardType.Phone, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                VInput(state.principalEmail, onPrincipalEmail, label = "Principal email", placeholder = "principal@school.edu", keyboardType = KeyboardType.Email, modifier = Modifier.fillMaxWidth())
            }

            // ── Address ──────────────────────────────────────────────────────
            VSectionHeader(title = "ADDRESS")
            VCard {
                VInput(state.fullAddress, onFullAddress, label = "Full address", placeholder = "Street, area, landmark", singleLine = false, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                VInput(state.city, onCity, label = "City", placeholder = "City", modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                VInput(state.district, onDistrict, label = "District", placeholder = "District", modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                VInput(state.state, onState, label = "State", placeholder = "State", modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                VInput(state.pincode, onPincode, label = "PIN code", placeholder = "6-digit PIN", keyboardType = KeyboardType.Number, modifier = Modifier.fillMaxWidth())
            }

            // ── Inline save feedback (LAW 3 — never silent) ──────────────────
            if (state.errorMessage != null) {
                Spacer(Modifier.height(2.dp))
                Text(state.errorMessage!!, style = VTheme.type.body.colored(c.dangerInk))
            }
            if (state.infoMessage != null) {
                Spacer(Modifier.height(2.dp))
                Text(state.infoMessage!!, style = VTheme.type.body.colored(c.successInk))
            }

            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth()) {
                VButton(
                    text = "Save changes",
                    onClick = onSave,
                    full = true,
                    variant = VButtonVariant.Primary,
                    tone = VButtonTone.Teal,
                    enabled = !state.isSaving,
                    loading = state.isSaving,
                )
            }
        }
    }
}
