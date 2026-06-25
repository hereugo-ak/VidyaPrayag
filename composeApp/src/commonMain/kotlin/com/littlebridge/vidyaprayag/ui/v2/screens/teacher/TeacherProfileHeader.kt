package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatar
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.theme.Enroll
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

/**
 * TeacherProfileHeader — the Profile tab's signature gradient hero (Loop task P6-T1).
 *
 * Per Design Spec PART 2 §SIGNATURE/§Gradients, a full-bleed brand gradient is
 * sanctioned ONLY on the Home and Profile headers — this is the Profile moment,
 * the formal "identity banner" for the signed-in teacher. It mirrors the visual
 * language of [TeacherHomeHeader] (same gradient, same white ring avatar) so the
 * two hero surfaces read as one family, while standing alone as an additive screen
 * (the operational tabs keep their calm lavender [TeacherHeader]).
 *
 * Every visual decision resolves through the `Enroll.*` bridge → existing VTheme
 * (no new colour; the gradient is the portal violet `accentDeep → accent`, keeping
 * parent↔teacher parity per the IMPORTANT NOTE):
 *   • Surface     → `Enroll.colors.headerGradient` (GradientStart → GradientEnd),
 *                   clipped to a soft bottom radius, full-width, 200dp tall, with
 *                   `statusBarsPadding` sitting ON the gradient.
 *   • Avatar      → 72dp circle ([VAvatarSize.Large]) centred, white 3dp ring
 *                   (`VAvatar(ring = true)`); tap → [onPickAvatar] (host wires the
 *                   platform image picker — a commonMain stub here, no Android API).
 *   • Name        → `HeadingLarge`, solid white, centred below the avatar.
 *   • Designation + school → `BodyMedium`, white 80% alpha, centred ("· " joined
 *                   only when both are present, so it never shows a dangling dot).
 *   • Edit pencil → `Edit3` in a glassy 36dp circle, top-right; tap → [onEdit]
 *                   (host navigates to the EditProfileScreen stub).
 *
 * Data-driven only: name / photo / designation / school come from the caller's
 * session + VM. Nothing about the teacher is fabricated. Avatar, pencil are ≥36dp
 * tap targets (a11y), each with a `contentDescription`.
 */
@Composable
fun TeacherProfileHeader(
    teacherName: String,
    photoUrl: String?,
    designation: String,
    schoolName: String,
    onPickAvatar: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayName = teacherName.ifBlank { "Teacher" }
    // Join designation + school with a calm middot, but only when both exist so we
    // never render a leading/trailing "·" — real EdTech copy, never a placeholder.
    val subtitle = listOf(designation.trim(), schoolName.trim())
        .filter { it.isNotEmpty() }
        .joinToString("  ·  ")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(Enroll.shape.sheet) // soft bottom-rounded banner (top corners flush to status bar)
            .background(Enroll.colors.headerGradient)
            .heightIn(min = 200.dp)
            .statusBarsPadding(),
    ) {
        // ── Top-right edit pencil → EditProfileScreen (host stub) ───────────────
        ProfileEditButton(
            onClick = onEdit,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(Enroll.space.lg),
        )

        // ── Centre column: avatar → name → designation · school ─────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = Enroll.space.xl, vertical = Enroll.space.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val avatarInteraction = remember { MutableInteractionSource() }
            Box(
                Modifier
                    .clip(CircleShape)
                    .clickable(interactionSource = avatarInteraction, indication = null) { onPickAvatar() }
                    .semantics { contentDescription = "Change profile photo" },
            ) {
                VAvatar(name = displayName, src = photoUrl, size = 72.dp, ring = true)
            }

            Spacer(Modifier.height(Enroll.space.md))

            Text(
                text = displayName,
                style = Enroll.type.headingLarge.colored(Color.White),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )

            if (subtitle.isNotEmpty()) {
                Spacer(Modifier.height(Enroll.space.xs))
                Text(
                    text = subtitle,
                    style = Enroll.type.bodyMedium.colored(Color.White.copy(alpha = 0.80f)),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Glassy circular pencil on the gradient → opens the EditProfileScreen (host stub). */
@Composable
private fun ProfileEditButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ix = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            // A whisper of white on the gradient → reads as a glass chip, not a flat hole.
            .background(Color.White.copy(alpha = 0.18f))
            .clickable(interactionSource = ix, indication = null) { onClick() }
            .semantics { contentDescription = "Edit profile" },
    ) {
        Icon(
            VIcons.Edit3,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
    }
}
