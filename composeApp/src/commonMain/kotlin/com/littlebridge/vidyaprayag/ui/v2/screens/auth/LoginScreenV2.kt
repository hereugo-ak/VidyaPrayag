package com.littlebridge.vidyaprayag.ui.v2.screens.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.feature.auth.presentation.AuthStep
import com.littlebridge.vidyaprayag.feature.auth.presentation.AuthViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonSize
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonTone
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VInput
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/**
 * LoginScreenV2 — a pixel-faithful copy of `Auth.tsx → Login`.
 *
 * Layout (top→bottom):
 *  • a teal **brand panel** — frosted-glass logo cube with the white bridge mark, "Welcome to
 *    VidyaSetu 👋", and the tagline.
 *  • a rounded **form sheet** that overlaps the hero by 8dp — a 3-pill portal selector
 *    (Parent / Admin / Teacher) on the `--portal-tab-bg` track, the role-appropriate inputs, the
 *    primary CTA ("Send OTP" / "Verify & Continue" / "Sign In"), and the "Not a member? Register Now"
 *    footer.
 *
 * Wiring: re-binds 1:1 to the real [AuthViewModel] (`shared/`). The portal pill drives
 * [AuthViewModel.onRoleChanged]; Parent uses the phone→OTP flow, Admin/Teacher use email/credential
 * → password. The VM's [AuthStep] decides which fields are visible.
 */
@Composable
fun LoginScreenV2(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    onRegister: () -> Unit = {},
    viewModel: AuthViewModel = koinViewModel(),
) {
    val c = VTheme.colors
    val state by viewModel.state.collectAsStateV2()

    LaunchedEffect(state.isAuthSuccessful) {
        if (state.isAuthSuccessful) onAuthSuccess()
    }

    // The selected portal pill. Mirror it into the VM's role on change.
    var portal by remember { mutableStateOf(if (state.role == "ADMIN") "admin" else "parent") }
    // Password reveal toggle (the React Eye affordance).
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier
            .fillMaxSize()
            .background(c.background)
            .verticalScroll(rememberScrollState()),
    ) {
        // ── Brand panel (teal hero) ──────────────────────────────────────────
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 320.dp)
                .background(c.teal),
            contentAlignment = Alignment.Center,
        ) {
            // faint decorative clouds
            CloudGlyph(Modifier.align(Alignment.TopStart).padding(start = 28.dp, top = 36.dp), alpha = 0.30f)
            CloudGlyph(Modifier.align(Alignment.BottomEnd).padding(end = 28.dp, bottom = 64.dp), alpha = 0.25f)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 48.dp),
            ) {
                LogoCube()
                Spacer(Modifier.height(24.dp))
                Text(
                    "Welcome to VidyaSetu. 👋",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.01).em,
                        fontFamily = VTheme.type.uiFamily,
                    ),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Bridging gaps for a glorious future",
                    style = TextStyle(
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 14.sp,
                        fontFamily = VTheme.type.uiFamily,
                    ),
                    textAlign = TextAlign.Center,
                )
            }
        }

        // ── Form sheet (overlaps hero, rounded top) ───────────────────────────
        Column(
            Modifier
                .fillMaxWidth()
                .offset(y = (-16).dp)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(c.background)
                .padding(horizontal = 20.dp)
                .padding(top = 32.dp, bottom = 32.dp),
        ) {
            // Portal selector — three pills on a tinted track
            PortalSelector(
                selected = portal,
                onSelect = { p ->
                    portal = p
                    viewModel.onRoleChanged(
                        when (p) {
                            "admin" -> "ADMIN"
                            "teacher" -> "TEACHER"
                            else -> "PARENT"
                        },
                    )
                },
            )

            Spacer(Modifier.height(24.dp))

            // Role-appropriate fields, driven by VM step.
            when (state.step) {
                AuthStep.Identifier -> {
                    if (portal == "parent") {
                        VInput(
                            value = state.identifier,
                            onValueChange = viewModel::onIdentifierChanged,
                            label = "Mobile number",
                            placeholder = "+91 98XXX XXXXX",
                            leadingIcon = VIcons.Phone,
                            keyboardType = KeyboardType.Phone,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        VInput(
                            value = state.identifier,
                            onValueChange = viewModel::onIdentifierChanged,
                            label = if (portal == "admin") "Email or School ID" else "Teacher credential",
                            placeholder = if (portal == "admin") "office@svm.edu.in" else "SVM001.T07",
                            leadingIcon = if (portal == "admin") VIcons.Mail else VIcons.User,
                            keyboardType = KeyboardType.Email,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                AuthStep.LoginPassword -> {
                    Column {
                        VInput(
                            value = state.password,
                            onValueChange = viewModel::onPasswordChanged,
                            label = "Password",
                            placeholder = "••••••••",
                            leadingIcon = VIcons.Lock,
                            isPassword = true,
                            passwordVisible = passwordVisible,
                            modifier = Modifier.fillMaxWidth(),
                            trailing = {
                                Icon(
                                    VIcons.Eye,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = c.ink3,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                        ) { passwordVisible = !passwordVisible },
                                )
                            },
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Forgot Password?",
                            style = VTheme.type.caption.colored(c.tealDeep).copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.align(Alignment.End),
                        )
                    }
                }
                AuthStep.SignupDetails -> {
                    VInput(
                        value = state.name,
                        onValueChange = viewModel::onNameChanged,
                        label = "Your name",
                        placeholder = "Full name",
                        leadingIcon = VIcons.User,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(16.dp))
                    VInput(
                        value = state.password,
                        onValueChange = viewModel::onPasswordChanged,
                        label = "Create password",
                        placeholder = "••••••••",
                        leadingIcon = VIcons.Lock,
                        isPassword = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                AuthStep.Otp -> {
                    VInput(
                        value = state.otp,
                        onValueChange = viewModel::onOtpChanged,
                        label = "OTP",
                        placeholder = "6-digit code",
                        leadingIcon = VIcons.ShieldCheck,
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (state.error != null) {
                Spacer(Modifier.height(12.dp))
                Text(state.error!!, style = VTheme.type.caption.colored(c.dangerInk))
            }

            Spacer(Modifier.height(24.dp))

            val ctaLabel = when (state.step) {
                AuthStep.Identifier -> if (portal == "parent") "Send OTP" else "Sign In"
                AuthStep.Otp -> "Verify & Continue"
                else -> "Sign In"
            }
            VButton(
                text = ctaLabel,
                onClick = {
                    if (state.step == AuthStep.Identifier) viewModel.onContinue() else viewModel.onSubmit()
                },
                full = true,
                size = VButtonSize.Lg,
                tone = VButtonTone.Teal,
                loading = state.isLoading,
                trailing = { Icon(VIcons.ArrowRight, contentDescription = null, modifier = Modifier.size(16.dp)) },
            )

            if (state.step != AuthStep.Identifier) {
                Spacer(Modifier.height(8.dp))
                val backInteraction = remember { MutableInteractionSource() }
                Text(
                    "‹ Back",
                    style = VTheme.type.caption.colored(c.ink3).copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(999.dp))
                        .clickable(interactionSource = backInteraction, indication = null) { viewModel.goBack() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }

            // Footer
            Spacer(Modifier.height(24.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Not a member?", style = VTheme.type.body.colored(c.ink2))
                Spacer(Modifier.size(6.dp))
                val regInteraction = remember { MutableInteractionSource() }
                Text(
                    "Register Now",
                    style = VTheme.type.body.colored(c.warmOrange).copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier
                        .clickable(interactionSource = regInteraction, indication = null) { onRegister() },
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Brand panel pieces
// ─────────────────────────────────────────────────────────────────────────────

/** The frosted-glass logo cube with the white bridge mark — identical to Splash. */
@Composable
private fun LogoCube(modifier: Modifier = Modifier) {
    val navy = VTheme.colors.navy
    Box(
        modifier
            .size(160.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(100.dp)) {
            val s = size.width / 56f
            fun x(v: Float) = v * s
            fun y(v: Float) = v * s

            // arc M12 32 Q28 12 44 32
            val arc = Path().apply {
                moveTo(x(12f), y(32f))
                quadraticTo(x(28f), y(12f), x(44f), y(32f))
            }
            drawPath(arc, color = Color.White, style = Stroke(width = 3f * s, cap = StrokeCap.Round))

            // deck M10 40 H46
            drawLine(Color.White, Offset(x(10f), y(40f)), Offset(x(46f), y(40f)), strokeWidth = 3.5f * s, cap = StrokeCap.Round)

            // cables
            val cable = Color.White.copy(alpha = 0.75f)
            drawLine(cable, Offset(x(18f), y(32f)), Offset(x(18f), y(40f)), strokeWidth = 1.6f * s, cap = StrokeCap.Round)
            drawLine(cable, Offset(x(28f), y(22f)), Offset(x(28f), y(40f)), strokeWidth = 1.6f * s, cap = StrokeCap.Round)
            drawLine(cable, Offset(x(38f), y(32f)), Offset(x(38f), y(40f)), strokeWidth = 1.6f * s, cap = StrokeCap.Round)

            // pillar caps
            drawCircle(Color.White, radius = 2.6f * s, center = Offset(x(12f), y(32f)))
            drawCircle(Color.White, radius = 2.6f * s, center = Offset(x(44f), y(32f)))
            drawCircle(navy, radius = 2.2f * s, center = Offset(x(28f), y(22f)))
        }
    }
}

/** A small wispy cloud outline used as faint hero decoration. */
@Composable
private fun CloudGlyph(modifier: Modifier = Modifier, alpha: Float = 0.3f) {
    Canvas(modifier.size(width = 56.dp, height = 38.dp)) {
        val sx = size.width / 48f
        val sy = size.height / 32f
        fun p(px: Float, py: Float) = Offset(px * sx, py * sy)
        // M6 24 Q12 14 22 18 Q28 8 38 14 Q46 14 44 24 Z
        val cloud = Path().apply {
            moveTo(p(6f, 24f).x, p(6f, 24f).y)
            quadraticTo(p(12f, 14f).x, p(12f, 14f).y, p(22f, 18f).x, p(22f, 18f).y)
            quadraticTo(p(28f, 8f).x, p(28f, 8f).y, p(38f, 14f).x, p(38f, 14f).y)
            quadraticTo(p(46f, 14f).x, p(46f, 14f).y, p(44f, 24f).x, p(44f, 24f).y)
            close()
        }
        drawPath(cloud, color = Color.White.copy(alpha = alpha), style = Stroke(width = 1.5f))
    }
}

/** The 3-pill portal selector on a tinted track (`--portal-tab-bg`). */
@Composable
private fun PortalSelector(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    // portal-tab-bg = #f6f1ff in light; in night fall back to the tinted surface.
    val track = if (c.isNight) c.cream else Color(0xFFF6F1FF)
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(track)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf("parent" to "Parent", "admin" to "Admin", "teacher" to "Teacher").forEach { (id, label) ->
            val active = id == selected
            val interaction = remember { MutableInteractionSource() }
            var pill = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
            pill = if (active) {
                pill.shadow(2.dp, RoundedCornerShape(8.dp)).background(c.card)
            } else {
                pill.background(Color.Transparent)
            }
            Box(
                pill
                    .clickable(interactionSource = interaction, indication = null) { onSelect(id) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = VTheme.type.caption.colored(if (active) c.tealDeep else c.ink2)
                        .copy(fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold),
                )
            }
        }
    }
}
