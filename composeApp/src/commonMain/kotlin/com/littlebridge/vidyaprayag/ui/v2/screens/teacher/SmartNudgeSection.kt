package com.littlebridge.vidyaprayag.ui.v2.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.littlebridge.vidyaprayag.ui.v2.components.EnrollCard
import com.littlebridge.vidyaprayag.ui.v2.components.SectionHeader
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.theme.Enroll
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import com.littlebridge.vidyaprayag.ui.v2.theme.pressScale

// ─────────────────────────────────────────────────────────────────────────────
// P2-T5 — Smart Nudge Cards
//
// The "NEEDS ATTENTION" section on the teacher Home tab: a tight stack of
// actionable reminders the VM derives from local cache (no extra API call).
// Each nudge is a tinted EnrollCard with a coloured icon, a plain-English message,
// and a single action chip — tap the chip to jump straight to the fix (P7 wiring).
//
// Tints honour the portal palette (IMPORTANT NOTE): "pending" work uses the amber
// accent family (Enroll.colors.accent*), "info" uses the violet primary family
// (Enroll.colors.primary*). No new colours, all via the Enroll.* bridge → VTheme.
// ─────────────────────────────────────────────────────────────────────────────

/** Visual weight of a nudge — picks the card tint + icon colour family. */
private enum class NudgeTone { Pending, Info }

/**
 * A single actionable reminder on the teacher Home tab. The sealed hierarchy keeps
 * each nudge's payload typed so the UI (and the P7 deep-link router) can address the
 * exact class / parent / homework it points at.
 */
sealed class TeacherNudge {
    /** Marks for an exam haven't been entered yet. */
    data class MarksNotEntered(
        val className: String,
        val subjectName: String,
        val examName: String,
        val classId: String? = null,
        val examId: String? = null,
    ) : TeacherNudge()

    /** A period's attendance was never taken. */
    data class AttendanceNotTaken(
        val className: String,
        val periodTime: String,
        val periodId: String? = null,
    ) : TeacherNudge()

    /** A parent message is waiting for a reply. */
    data class ParentUnread(
        val parentName: String,
        val studentName: String,
        val threadId: String? = null,
        val studentId: String? = null,
    ) : TeacherNudge()

    /** Submitted homework is still ungraded. */
    data class HomeworkUngraded(
        val className: String,
        val count: Int,
        val classId: String? = null,
    ) : TeacherNudge()
}

/** The human-readable line shown in the centre of the card. */
private fun TeacherNudge.message(): String = when (this) {
    is TeacherNudge.MarksNotEntered ->
        "$examName marks for $className $subjectName aren't entered yet"
    is TeacherNudge.AttendanceNotTaken ->
        "Attendance for $className ($periodTime) wasn't taken"
    is TeacherNudge.ParentUnread ->
        "$parentName ($studentName's parent) is waiting for a reply"
    is TeacherNudge.HomeworkUngraded ->
        "$count homework submission${if (count == 1) "" else "s"} in $className need grading"
}

/** The action-chip label on the right edge. */
private fun TeacherNudge.actionLabel(): String = when (this) {
    is TeacherNudge.MarksNotEntered -> "Add Now"
    is TeacherNudge.AttendanceNotTaken -> "Take Now"
    is TeacherNudge.ParentUnread -> "Reply"
    is TeacherNudge.HomeworkUngraded -> "Grade"
}

private fun TeacherNudge.icon(): ImageVector = when (this) {
    is TeacherNudge.MarksNotEntered -> VIcons.GraduationCap
    is TeacherNudge.AttendanceNotTaken -> VIcons.Check
    is TeacherNudge.ParentUnread -> VIcons.Chat
    is TeacherNudge.HomeworkUngraded -> VIcons.ClipboardList
}

/** Unattended roll-call / overdue marks read as "pending"; messages read as "info". */
private fun TeacherNudge.tone(): NudgeTone = when (this) {
    is TeacherNudge.MarksNotEntered -> NudgeTone.Pending
    is TeacherNudge.AttendanceNotTaken -> NudgeTone.Pending
    is TeacherNudge.HomeworkUngraded -> NudgeTone.Pending
    is TeacherNudge.ParentUnread -> NudgeTone.Info
}

/**
 * SmartNudgeSection — renders the "NEEDS ATTENTION" stack, or nothing when there's
 * nothing to nudge about (a clean Home is a feature, not an empty card).
 *
 * @param nudges    VM-derived reminders (already prioritised / capped upstream)
 * @param onAction  fire the nudge's primary action (P7 deep-link router)
 * @param onDismiss optional swipe-away / dismiss hook; when null the dismiss affordance hides
 */
@Composable
fun SmartNudgeSection(
    nudges: List<TeacherNudge>,
    onAction: (TeacherNudge) -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: ((TeacherNudge) -> Unit)? = null,
) {
    if (nudges.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = "NEEDS ATTENTION")
        Spacer(Modifier.height(Enroll.space.md))
        nudges.forEachIndexed { index, nudge ->
            if (index > 0) Spacer(Modifier.height(Enroll.space.sm))
            NudgeCard(
                nudge = nudge,
                onAction = { onAction(nudge) },
                onDismiss = onDismiss?.let { { it(nudge) } },
            )
        }
    }
}

@Composable
private fun NudgeCard(
    nudge: TeacherNudge,
    onAction: () -> Unit,
    onDismiss: (() -> Unit)?,
) {
    val tone = nudge.tone()
    val cardTint: Color = when (tone) {
        NudgeTone.Pending -> Enroll.colors.accentSoft
        NudgeTone.Info -> Enroll.colors.primarySoft
    }
    val iconCircle: Color = when (tone) {
        NudgeTone.Pending -> Enroll.colors.accent
        NudgeTone.Info -> Enroll.colors.primaryMid
    }

    EnrollCard(
        modifier = Modifier.fillMaxWidth(),
        tint = cardTint,
        border = false,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left: colour-coded icon disc
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconCircle),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = nudge.icon(),
                    contentDescription = null,
                    tint = Enroll.colors.onPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }

            Spacer(Modifier.size(Enroll.space.md))

            // Center: the nudge message
            Text(
                text = nudge.message(),
                style = Enroll.type.bodyMedium.colored(Enroll.colors.textPrimary),
                maxLines = 3,
                modifier = Modifier.weight(1f),
            )

            Spacer(Modifier.size(Enroll.space.md))

            // Right: action chip (+ optional dismiss)
            NudgeActionChip(label = nudge.actionLabel(), tone = tone, onClick = onAction)
            if (onDismiss != null) {
                Spacer(Modifier.size(Enroll.space.xs))
                NudgeDismiss(onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun NudgeActionChip(label: String, tone: NudgeTone, onClick: () -> Unit) {
    val ix = remember { MutableInteractionSource() }
    val fg = when (tone) {
        NudgeTone.Pending -> Enroll.colors.accent
        NudgeTone.Info -> Enroll.colors.primaryMid
    }
    Box(
        modifier = Modifier
            .pressScale(ix)
            .clip(Enroll.shape.pill)
            .background(Enroll.colors.surfaceCard)
            .clickable(interactionSource = ix, indication = null, onClick = onClick)
            .padding(horizontal = Enroll.space.md, vertical = Enroll.space.sm),
    ) {
        Text(
            text = label,
            style = Enroll.type.labelBold.colored(fg),
        )
    }
}

@Composable
private fun NudgeDismiss(onClick: () -> Unit) {
    val ix = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .pressScale(ix)
            .size(28.dp)
            .clip(CircleShape)
            .clickable(interactionSource = ix, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = VIcons.Close,
            contentDescription = "Dismiss",
            tint = Enroll.colors.textTertiary,
            modifier = Modifier.size(16.dp),
        )
    }
}
