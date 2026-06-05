package com.littlebridge.vidyaprayag.ui.v2.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.littlebridge.vidyaprayag.ui.v2.screens.auth.LoginScreenV2
import com.littlebridge.vidyaprayag.ui.v2.screens.auth.WelcomeScreenV2
import com.littlebridge.vidyaprayag.ui.v2.screens.discovery.DiscoveryScreenV2
import com.littlebridge.vidyaprayag.ui.v2.screens.parent.ParentPortalV2
import com.littlebridge.vidyaprayag.ui.v2.screens.school.SchoolPortalV2
import com.littlebridge.vidyaprayag.ui.v2.screens.teacher.TeacherPortalV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VPortalTone
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme

/**
 * The portal-aware root for the `ui/v2` UI — the Compose translation of `App.tsx`'s screen graph.
 *
 * Replaces the old `navigation/NavGraph.kt` (35-destination `NavHost`) with a small role-driven
 * switch: the auth state decides which **portal** is shown, and each portal owns its own internal
 * tab navigation (`*PortalV2`). The correct [VPortalTone] is applied per role:
 *
 * | role     | start surface              | tone   |
 * |----------|----------------------------|--------|
 * | (none)   | Welcome → Login → Discovery| Light  |
 * | PARENT   | `ParentPortalV2`           | Light  |
 * | TEACHER  | `TeacherPortalV2`          | Warm   |
 * | ADMIN    | `SchoolPortalV2`           | Warm   |
 *
 * Keeping navigation role-scoped (rather than one flat 35-route graph) matches the design, where
 * each portal is a self-contained tabbed app. Cross-portal deep-links (Notifications, school detail,
 * onboarding wizard) are layered on in a later pass once the screens exist; the unauth flow already
 * reaches Discovery so the marketplace is browsable before sign-in.
 */
@Composable
fun NavGraphV2(
    role: String?,
    isAuthenticated: Boolean,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tone = toneFor(role, isAuthenticated)

    VTheme(tone = tone) {
        if (isAuthenticated) {
            when (role?.uppercase()) {
                "ADMIN" -> SchoolPortalV2(onLogout = onLogout, modifier = modifier)
                "TEACHER" -> TeacherPortalV2(onLogout = onLogout, modifier = modifier)
                "PARENT" -> ParentPortalV2(onLogout = onLogout, modifier = modifier)
                // Authenticated but role unknown → fall back to the parent surface.
                else -> ParentPortalV2(onLogout = onLogout, modifier = modifier)
            }
        } else {
            UnauthFlow(modifier = modifier)
        }
    }
}

/** Internal screens shown before a session exists: Welcome → Login → Discovery (browse-first). */
private enum class AuthRoute { Welcome, Login, Discovery }

@Composable
private fun UnauthFlow(modifier: Modifier = Modifier) {
    var route by remember { mutableStateOf(AuthRoute.Welcome) }

    AnimatedContent(
        targetState = route,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "auth-flow",
        modifier = modifier,
    ) { current ->
        when (current) {
            AuthRoute.Welcome -> WelcomeScreenV2(
                onGetStarted = { route = AuthRoute.Discovery },
                onHaveAccount = { route = AuthRoute.Login },
            )
            AuthRoute.Login -> LoginScreenV2(
                // On success the host MainViewModel's authState flips isAuthenticated=true and
                // NavGraphV2 recomposes into the role portal; nothing else to do here.
                onAuthSuccess = {},
            )
            AuthRoute.Discovery -> DiscoveryScreenV2(
                onOpenSchool = {},
            )
        }
    }
}

/** Parent & Discovery are LIGHT (lavender); Teacher & Admin are WARM. Night handled by host later. */
private fun toneFor(role: String?, isAuthenticated: Boolean): VPortalTone = when {
    !isAuthenticated -> VPortalTone.Light
    role.equals("ADMIN", ignoreCase = true) -> VPortalTone.Warm
    role.equals("TEACHER", ignoreCase = true) -> VPortalTone.Warm
    else -> VPortalTone.Light
}
