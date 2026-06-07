package com.littlebridge.vidyaprayag.ui.v2.screens.auth

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatar
import com.littlebridge.vidyaprayag.ui.v2.components.VBadge
import com.littlebridge.vidyaprayag.ui.v2.components.VBadgeTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonSize
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonTone
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VInput
import com.littlebridge.vidyaprayag.ui.v2.components.VLabel
import com.littlebridge.vidyaprayag.ui.v2.components.VTag
import com.littlebridge.vidyaprayag.ui.v2.data.MockV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

/**
 * ParentLinkChildScreenV2 — pixel-faithful Compose copy of `Auth.tsx → ParentLinkChild`.
 *
 * The 3-step "link your child" wizard the Figma prototype renders:
 *   1. **Tell us about you** — full name + preferred language tags.
 *   2. **Find your child's school** — search field + a "Match" school result card.
 *   3. **Link your child** — roll/admission field + the resolved-child preview card
 *      (driven by [MockV2.childForParent], exactly as the React mock does).
 *
 * The bottom CTA advances steps and finally calls [onDone] to open the dashboard.
 */
@Composable
fun ParentLinkChildScreenV2(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val c = VTheme.colors
    val d = VTheme.dimens
    val total = 3

    var step by remember { mutableIntStateOf(1) }
    var fullName by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("English") }
    var schoolQuery by remember { mutableStateOf("") }
    var rollNo by remember { mutableStateOf("") }

    val child = MockV2.childForParent

    Column(
        modifier
            .fillMaxSize()
            .background(c.background)
            // §11 cross-platform safe areas (Android + iOS, common code).
            .statusBarsPadding()
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(40.dp))
        // §5: React `Label` component = labelStrong (uppercase 11/700/0.10em).
        VLabel("Step $step of $total")
        Spacer(Modifier.height(d.sm))
        StepBars(current = step, total = total)

        // §13.2 — Crossfade step content for a smooth swap (no slide inside a verticalScroll,
        // which would break the parent's height measurement). 240ms tween matches the React
        // step indicator timing.
        Crossfade(targetState = step, animationSpec = tween(240), label = "linkStep") { current ->
        Column {
        when (current) {
            1 -> {
                Spacer(Modifier.height(d.lg))
                Text("Tell us about you", style = VTheme.type.h1.colored(c.ink))
                Text(
                    "So your child's school knows who to send updates to.",
                    style = VTheme.type.body.colored(c.ink2),
                )
                Spacer(Modifier.height(d.lg))
                VInput(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = "Your full name",
                    placeholder = "e.g. Sneha Sharma",
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(d.md))
                VLabel("Preferred language")
                Spacer(Modifier.height(d.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(d.sm)) {
                    VTag(text = "English", active = language == "English", onClick = { language = "English" })
                    VTag(text = "हिन्दी", active = language == "हिन्दी", onClick = { language = "हिन्दी" })
                }
            }

            2 -> {
                Spacer(Modifier.height(d.lg))
                Text("Find your child's school", style = VTheme.type.h1.colored(c.ink))
                Text(
                    "Type the school name. We'll match it against schools using VidyaSetu.",
                    style = VTheme.type.body.colored(c.ink2),
                )
                Spacer(Modifier.height(d.lg))
                VInput(
                    value = schoolQuery,
                    onValueChange = { schoolQuery = it },
                    placeholder = "Search by school name",
                    leadingIcon = VIcons.Search,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(d.sm))
                VCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // §5: React match-icon circle = solid var(--arctic)=teal, dark glyph (Auth.tsx L294).
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(c.teal),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(VIcons.GraduationCap, contentDescription = null, tint = c.ink, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(d.md))
                        Column(Modifier.weight(1f)) {
                            Text(MockV2.school.name, style = VTheme.type.bodyStrong.colored(c.ink))
                            Text("Lucknow • CBSE • 0.6 km", style = VTheme.type.caption.colored(c.ink2))
                        }
                        VBadge(text = "Match", tone = VBadgeTone.Arctic)
                    }
                }
            }

            else -> {
                Spacer(Modifier.height(d.lg))
                Text("Link your child", style = VTheme.type.h1.colored(c.ink))
                Text(
                    "Enter the roll or admission number assigned by the school.",
                    style = VTheme.type.body.colored(c.ink2),
                )
                Spacer(Modifier.height(d.lg))
                VInput(
                    value = rollNo,
                    onValueChange = { rollNo = it },
                    label = "Roll / admission number",
                    placeholder = "e.g. 02",
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(d.md))
                VCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        VAvatar(name = child.name, size = 48.dp)
                        Spacer(Modifier.width(d.md))
                        Column(Modifier.weight(1f)) {
                            Text(child.name, style = VTheme.type.bodyStrong.colored(c.ink))
                            Text(
                                "Class ${MockV2.classDisplay(child.klass)} • Roll ${child.roll}",
                                style = VTheme.type.caption.colored(c.ink2),
                            )
                        }
                        // §5: React resolved-child check = #155e3a (Auth.tsx L319).
                        Icon(VIcons.Check, contentDescription = null, tint = Color(0xFF155E3A), modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(d.md))
                // §5: React "+ Add another child" = plain 13/600 #0a3a76 link (Auth.tsx L322).
                Text(
                    "+ Add another child",
                    style = VTheme.type.body.colored(Color(0xFF0A3A76)).copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = d.xs),
                )
            }
        }
        }
        }

        Spacer(Modifier.height(d.xl))
        // §5: React has a SINGLE CTA (Continue / Finish) with a trailing ArrowRight; no Back button.
        VButton(
            text = if (step < total) "Continue" else "Finish & open dashboard",
            onClick = { if (step < total) step++ else onDone() },
            full = true,
            size = VButtonSize.Lg,
            tone = VButtonTone.Teal,
            soft = false,
            trailing = { Icon(VIcons.ArrowRight, contentDescription = null, modifier = Modifier.size(16.dp)) },
        )
        Spacer(Modifier.height(d.xl))
    }
}

@Composable
private fun StepBars(current: Int, total: Int) {
    val c = VTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(d6())) {
        repeat(total) { i ->
            // §13.2 — animate the bar fill instead of swapping colors instantly.
            val active = i + 1 <= current
            val targetColor by animateColorAsState(
                targetValue = if (active) c.teal else Color(0x14080808),
                animationSpec = tween(durationMillis = 250),
                label = "linkStepBar$i",
            )
            Box(
                Modifier
                    .weight(1f)
                    // React: h-1 (4dp) bar — filled var(--arctic)=teal, empty rgba(8,8,8,0.08).
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(targetColor),
            )
        }
    }
}

private fun d6() = 6.dp
