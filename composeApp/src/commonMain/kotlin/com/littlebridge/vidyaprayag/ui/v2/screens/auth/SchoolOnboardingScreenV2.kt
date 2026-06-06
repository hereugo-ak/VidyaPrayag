package com.littlebridge.vidyaprayag.ui.v2.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.feature.admin.presentation.AcademicInfoOBViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.BrandingInfoOBViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.InstitutionalBasicOBViewModel
import com.littlebridge.vidyaprayag.feature.admin.presentation.LaunchInfoOBViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VBadge
import com.littlebridge.vidyaprayag.ui.v2.components.VBadgeTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonSize
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonVariant
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VInput
import com.littlebridge.vidyaprayag.ui.v2.components.VProgressBar
import com.littlebridge.vidyaprayag.ui.v2.components.VTag
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * SchoolOnboardingScreenV2 — the 6-step (here 4-API-backed) school setup wizard, translated from
 * `Auth.tsx → SchoolOnboarding`.
 *
 * Each step binds **1:1** to its existing `shared/` ViewModel and advances only on a real backend
 * `submit { … }` success — never on a fake/optimistic local step:
 *  1. Basics    → [InstitutionalBasicOBViewModel]  (`ob_step_type = BASIC`)
 *  2. Branding  → [BrandingInfoOBViewModel]        (`ob_step_type = BRANDING`)
 *  3. Academic  → [AcademicInfoOBViewModel]         (`ob_step_type = ACADEMIC`)
 *  4. Launch    → [LaunchInfoOBViewModel]           (final submission)
 *
 * On the final step's success [onComplete] is invoked so the host can route into the Admin portal.
 */
@Composable
fun SchoolOnboardingScreenV2(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    var step by remember { mutableIntStateOf(0) }
    val total = 4

    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = VTheme.dimens.lg)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(VTheme.dimens.xl))

        // ── Header + step indicator ─────────────────────────────────────────────
        Text("School setup", style = VTheme.type.h2.colored(VTheme.colors.ink))
        Text(
            "Step ${step + 1} of $total",
            style = VTheme.type.caption.colored(VTheme.colors.ink2),
            modifier = Modifier.padding(top = VTheme.dimens.xs),
        )
        Spacer(Modifier.height(VTheme.dimens.md))
        StepDots(current = step, total = total)
        Spacer(Modifier.height(VTheme.dimens.lg))

        when (step) {
            0 -> BasicsStep(onNext = { step = 1 })
            1 -> BrandingStep(onNext = { step = 2 })
            2 -> AcademicStep(onNext = { step = 3 })
            else -> LaunchStep(onComplete = onComplete)
        }

        Spacer(Modifier.height(VTheme.dimens.md))
        VButton(
            text = if (step == 0) "Cancel" else "Back",
            onClick = { if (step == 0) onBack() else step-- },
            full = true,
            variant = VButtonVariant.Ghost,
            tone = VButtonTone.Navy,
        )
        Spacer(Modifier.height(VTheme.dimens.xl))
    }
}

@Composable
private fun StepDots(current: Int, total: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(VTheme.dimens.xs)) {
        repeat(total) { i ->
            val active = i <= current
            Box(
                Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(if (active) VTheme.colors.tealDeep else VTheme.colors.border2),
            )
        }
    }
}

// ── Step 1: Institutional basics ───────────────────────────────────────────────
@Composable
private fun BasicsStep(
    onNext: () -> Unit,
    vm: InstitutionalBasicOBViewModel = koinViewModel(),
) {
    val s by vm.state.collectAsStateV2()
    val submitting by vm.isSubmitting.collectAsStateV2()
    val error by vm.errorMessage.collectAsStateV2()

    Text("Institutional basics", style = VTheme.type.h3.colored(VTheme.colors.ink))
    Spacer(Modifier.height(VTheme.dimens.md))
    VInput(s.schoolName, vm::updateSchoolName, label = "School name", placeholder = "St. Augustine Academy", modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(VTheme.dimens.md))
    VInput(s.boardAffiliation, vm::updateBoard, label = "Board affiliation", placeholder = "CBSE / ICSE / State", modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(VTheme.dimens.md))
    VInput(s.officialEmail, vm::updateEmail, label = "Official email", placeholder = "office@school.edu", keyboardType = KeyboardType.Email, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(VTheme.dimens.md))
    VInput(s.contactNumber, vm::updateContact, label = "Contact number", placeholder = "98xxxxxxxx", keyboardType = KeyboardType.Phone, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(VTheme.dimens.md))
    VInput(s.address, vm::updateAddress, label = "Address", placeholder = "Street, city, pincode", singleLine = false, modifier = Modifier.fillMaxWidth())

    ErrorText(error)
    Spacer(Modifier.height(VTheme.dimens.lg))
    VButton("Continue", onClick = { vm.submit(onNext) }, full = true, size = VButtonSize.Lg, tone = VButtonTone.Teal, soft = false, loading = submitting)
}

// ── Step 2: Branding ───────────────────────────────────────────────────────────
@Composable
private fun BrandingStep(
    onNext: () -> Unit,
    vm: BrandingInfoOBViewModel = koinViewModel(),
) {
    val s by vm.state.collectAsStateV2()
    val submitting by vm.isSubmitting.collectAsStateV2()
    val error by vm.errorMessage.collectAsStateV2()

    Text("Branding & identity", style = VTheme.type.h3.colored(VTheme.colors.ink))
    Spacer(Modifier.height(VTheme.dimens.md))
    VInput(s.pedagogicalMission, vm::updatePedagogicalMission, label = "Pedagogical mission", placeholder = "How you teach", singleLine = false, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(VTheme.dimens.md))
    VInput(s.visionStatement, vm::updateVisionStatement, label = "Vision statement", placeholder = "Where you're headed", singleLine = false, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(VTheme.dimens.md))
    VInput(s.virtualTourUrl, vm::updateVirtualTour, label = "Virtual tour link (optional)", placeholder = "https://…", modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(VTheme.dimens.md))
    VInput(s.brandColorHex, vm::updateBrandColor, label = "Brand color (hex)", placeholder = "#2563EB", modifier = Modifier.fillMaxWidth())

    ErrorText(error)
    Spacer(Modifier.height(VTheme.dimens.lg))
    VButton("Continue", onClick = { vm.submit(onNext) }, full = true, size = VButtonSize.Lg, tone = VButtonTone.Teal, soft = false, loading = submitting)
}

// ── Step 3: Academic structure ─────────────────────────────────────────────────
@Composable
private fun AcademicStep(
    onNext: () -> Unit,
    vm: AcademicInfoOBViewModel = koinViewModel(),
) {
    val s by vm.state.collectAsStateV2()
    val submitting by vm.isSubmitting.collectAsStateV2()
    val error by vm.errorMessage.collectAsStateV2()

    Text("Academic structure", style = VTheme.type.h3.colored(VTheme.colors.ink))
    Text("Pick a class to configure its subjects", style = VTheme.type.caption.colored(VTheme.colors.ink2), modifier = Modifier.padding(top = VTheme.dimens.xs))
    Spacer(Modifier.height(VTheme.dimens.md))

    // Class selector chips
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(VTheme.dimens.sm),
    ) {
        s.availableClasses.forEach { cls ->
            VTag(text = cls, active = cls == s.selectedClass, onClick = { vm.selectClass(cls) })
        }
    }
    Spacer(Modifier.height(VTheme.dimens.md))

    Text("Subjects in ${s.selectedClass}", style = VTheme.type.label.colored(VTheme.colors.ink3))
    Spacer(Modifier.height(VTheme.dimens.sm))
    s.subjects.forEach { subject ->
        VCard(modifier = Modifier.fillMaxWidth().padding(bottom = VTheme.dimens.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(subject.name, style = VTheme.type.body.colored(VTheme.colors.ink), modifier = Modifier.weight(1f))
                if (subject.teacherName != null) {
                    VBadge(text = subject.teacherName!!, tone = VBadgeTone.Arctic)
                } else {
                    VBadge(text = "Unassigned", tone = VBadgeTone.Neutral)
                }
            }
        }
    }

    Spacer(Modifier.height(VTheme.dimens.sm))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Curriculum precision", style = VTheme.type.caption.colored(VTheme.colors.ink2), modifier = Modifier.weight(1f))
        Text("${s.curriculumPrecision}%", style = VTheme.type.data.colored(VTheme.colors.tealDeep))
    }
    Spacer(Modifier.height(VTheme.dimens.xs))
    VProgressBar(value = s.curriculumPrecision.toFloat(), modifier = Modifier.fillMaxWidth())

    ErrorText(error)
    Spacer(Modifier.height(VTheme.dimens.lg))
    VButton("Continue", onClick = { vm.submit(onNext) }, full = true, size = VButtonSize.Lg, tone = VButtonTone.Teal, soft = false, loading = submitting)
}

// ── Step 4: Launch / compliance ────────────────────────────────────────────────
@Composable
private fun LaunchStep(
    onComplete: () -> Unit,
    vm: LaunchInfoOBViewModel = koinViewModel(),
) {
    val s by vm.state.collectAsStateV2()
    val submitting by vm.isSubmitting.collectAsStateV2()
    val error by vm.errorMessage.collectAsStateV2()

    Text("Launch checklist", style = VTheme.type.h3.colored(VTheme.colors.ink))
    Spacer(Modifier.height(VTheme.dimens.md))

    VCard(modifier = Modifier.fillMaxWidth()) {
        Text(s.schoolName, style = VTheme.type.h4.colored(VTheme.colors.ink))
        Text(s.licenseType, style = VTheme.type.caption.colored(VTheme.colors.ink2), modifier = Modifier.padding(top = VTheme.dimens.xs))
        Text(s.location, style = VTheme.type.caption.colored(VTheme.colors.ink3), modifier = Modifier.padding(top = VTheme.dimens.xs))
    }

    Spacer(Modifier.height(VTheme.dimens.md))
    Text("Compliance documents", style = VTheme.type.label.colored(VTheme.colors.ink3))
    Spacer(Modifier.height(VTheme.dimens.sm))
    s.documents.forEach { doc ->
        VCard(modifier = Modifier.fillMaxWidth().padding(bottom = VTheme.dimens.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(doc.name, style = VTheme.type.body.colored(VTheme.colors.ink))
                    if (doc.metadata != null) Text(doc.metadata!!, style = VTheme.type.caption.colored(VTheme.colors.ink3))
                }
                VBadge(
                    text = doc.status,
                    tone = if (doc.status.equals("Uploaded", true)) VBadgeTone.Success else VBadgeTone.Warning,
                )
            }
        }
    }

    Spacer(Modifier.height(VTheme.dimens.md))
    Text("App modules", style = VTheme.type.label.colored(VTheme.colors.ink3))
    Spacer(Modifier.height(VTheme.dimens.sm))
    s.modules.forEach { mod ->
        VCard(modifier = Modifier.fillMaxWidth().padding(bottom = VTheme.dimens.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(mod.name, style = VTheme.type.body.colored(VTheme.colors.ink))
                    Text(mod.description, style = VTheme.type.caption.colored(VTheme.colors.ink3))
                }
                VTag(
                    text = if (mod.isEnabled) "On" else "Off",
                    active = mod.isEnabled,
                    onClick = { vm.toggleModule(mod.id) },
                )
            }
        }
    }

    ErrorText(error)
    Spacer(Modifier.height(VTheme.dimens.lg))
    VButton("Finish & launch", onClick = { vm.submit(onComplete) }, full = true, size = VButtonSize.Lg, tone = VButtonTone.Teal, soft = false, loading = submitting)
}

@Composable
private fun ErrorText(error: String?) {
    if (error != null) {
        Spacer(Modifier.height(VTheme.dimens.sm))
        Text(error, style = VTheme.type.caption.colored(VTheme.colors.dangerInk))
    }
}

