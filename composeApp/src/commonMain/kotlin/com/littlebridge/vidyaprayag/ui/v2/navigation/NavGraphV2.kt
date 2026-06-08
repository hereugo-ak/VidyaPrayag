package com.littlebridge.vidyaprayag.ui.v2.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import com.littlebridge.vidyaprayag.feature.auth.domain.repository.AuthRepository
import com.littlebridge.vidyaprayag.ui.v2.screens.auth.AdminAuthScreenV2
import com.littlebridge.vidyaprayag.ui.v2.screens.auth.CommonLandingScreenV2
import com.littlebridge.vidyaprayag.ui.v2.screens.auth.ParentAuthScreenV2
import com.littlebridge.vidyaprayag.ui.v2.screens.auth.ParentLinkChildScreenV2
import com.littlebridge.vidyaprayag.ui.v2.screens.auth.SchoolOnboardingScreenV2
import com.littlebridge.vidyaprayag.ui.v2.screens.auth.TeacherFirstLoginScreenV2
import com.littlebridge.vidyaprayag.ui.v2.screens.discovery.DiscoveryScreenV2
import com.littlebridge.vidyaprayag.ui.v2.screens.parent.ParentPortalV2
import com.littlebridge.vidyaprayag.ui.v2.screens.school.SchoolPortalV2
import com.littlebridge.vidyaprayag.ui.v2.screens.teacher.TeacherPortalV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VMotion
import com.littlebridge.vidyaprayag.ui.v2.theme.VPortalTone
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import org.koin.compose.koinInject

/**
 * NavGraphV2 — the single source of nav truth for the `ui/v2` entry experience (PHASE 7).
 *
 * This is the Compose translation of the target entry flow. Navigation is **state-driven** (no flat
 * NavHost): the persisted session decides auth-vs-portal, and small enum state machines own the
 * unauth funnel and the post-login gate. Every transition is explicit and every post-auth jump pops
 * its predecessor so back-press can never return to splash, landing, or an auth screen (LAW 4).
 *
 *   Splash (in App.kt)
 *     ├─ valid session → [AuthedFlow] → role gate → correct portal
 *     └─ no session    → [UnauthFlow] → CommonLanding → Parent/Admin auth
 *
 * Role is the persisted JWT role; [EntryRole] normalizes it (handles ADMIN / SCHOOL_ADMIN / TEACHER
 * / PARENT) so no decision site hardcodes a raw string.
 */
@Composable
fun NavGraphV2(
    role: String?,
    isAuthenticated: Boolean,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val entryRole = EntryRole.from(role)
    val tone = entryRole.tone(isAuthenticated)

    VTheme(tone = tone) {
        if (isAuthenticated) {
            AuthedFlow(role = entryRole, onLogout = onLogout, modifier = modifier)
        } else {
            UnauthFlow(modifier = modifier)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Role model — one place that turns the persisted string into a typed role.
// ─────────────────────────────────────────────────────────────────────────────

/** Typed app role, parsed once from the persisted JWT role string (LAW: no scattered role literals). */
enum class EntryRole {
    Parent, SchoolAdmin, SuperAdmin, Teacher, Unknown;

    fun tone(isAuthenticated: Boolean): VPortalTone = when {
        !isAuthenticated -> VPortalTone.Light
        this == SchoolAdmin || this == SuperAdmin || this == Teacher -> VPortalTone.Warm
        else -> VPortalTone.Light
    }

    companion object {
        fun from(raw: String?): EntryRole = when (raw?.trim()?.uppercase()) {
            "PARENT" -> Parent
            "ADMIN", "SCHOOL_ADMIN", "SCHOOLADMIN" -> SchoolAdmin
            // Audit §3.5: super_admin was previously unmapped → Unknown → Parent
            // portal. It is an operator/admin role, so it lands on the admin surface.
            "SUPER_ADMIN", "SUPERADMIN" -> SuperAdmin
            "TEACHER" -> Teacher
            else -> Unknown
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Unauthenticated funnel:  CommonLanding → Parent/Admin auth (+ discovery/link/onboard branches)
// ─────────────────────────────────────────────────────────────────────────────

private enum class UnauthRoute { Landing, ParentAuth, AdminAuth, Discovery, ParentLinkChild, SchoolOnboarding }

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun UnauthFlow(modifier: Modifier = Modifier) {
    var route by remember { mutableStateOf(UnauthRoute.Landing) }

    // System back: collapse the funnel toward the landing screen (never exit from a leaf).
    BackHandler(enabled = route != UnauthRoute.Landing) {
        route = when (route) {
            UnauthRoute.ParentAuth -> UnauthRoute.Landing
            UnauthRoute.AdminAuth -> UnauthRoute.Landing
            UnauthRoute.Discovery -> UnauthRoute.ParentAuth
            UnauthRoute.ParentLinkChild -> UnauthRoute.Discovery
            UnauthRoute.SchoolOnboarding -> UnauthRoute.AdminAuth
            UnauthRoute.Landing -> UnauthRoute.Landing
        }
    }

    AnimatedContent(
        targetState = route,
        // Funnel screens advance "deeper" → subtle forward horizontal momentum + fade.
        transitionSpec = { VMotion.forwardSlide() },
        label = "unauth-flow",
        modifier = modifier,
    ) { current ->
        when (current) {
            UnauthRoute.Landing -> CommonLandingScreenV2(
                onParent = { route = UnauthRoute.ParentAuth },
                onAdmin = { route = UnauthRoute.AdminAuth },
            )
            UnauthRoute.ParentAuth -> ParentAuthScreenV2(
                // On success the persisted session flips isAuthenticated=true and NavGraphV2
                // recomposes into AuthedFlow, which runs the child-link gate (PHASE 6).
                onAuthSuccess = {},
                onBack = { route = UnauthRoute.Landing },
            )
            UnauthRoute.AdminAuth -> AdminAuthScreenV2(
                // On success the session flips and AuthedFlow runs the onboard / first-login gate.
                onAuthSuccess = {},
                onBack = { route = UnauthRoute.Landing },
            )
            // Browse-first marketplace, reachable from the parent path for new families.
            UnauthRoute.Discovery -> DiscoveryScreenV2(
                onOpenSchool = { _ -> route = UnauthRoute.ParentLinkChild },
            )
            UnauthRoute.ParentLinkChild -> ParentLinkChildScreenV2(
                onDone = { route = UnauthRoute.ParentAuth },
                onBack = { route = UnauthRoute.Discovery },
            )
            UnauthRoute.SchoolOnboarding -> SchoolOnboardingScreenV2(
                onComplete = { route = UnauthRoute.AdminAuth },
                onBack = { route = UnauthRoute.AdminAuth },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Authenticated gate:  role → (child-link | onboarding | first-login) → portal
// ─────────────────────────────────────────────────────────────────────────────

private enum class AuthedRoute { Resolving, ParentLinkChild, SchoolOnboarding, TeacherFirstLogin, Portal }

/**
 * AuthedFlow — runs the one-time post-login gate before handing control to the role portal.
 *
 * Decision inputs come from the **live session** read via [AuthRepository.getSession]:
 *  • PARENT      — `profileCompleted == false` → child-link flow (Discovery + LinkChild), else portal.
 *  • SCHOOL_ADMIN— `profileCompleted == false` → school-onboarding wizard, else portal.
 *  • TEACHER     — `profileCompleted == false` → first-login change-password gate, else portal.
 *
 * Returning users (valid JWT after an app restart) have no in-memory session cache, so the gate
 * resolves immediately to the portal — which is correct: they've already onboarded/linked. Every
 * gate completion advances to [AuthedRoute.Portal] with no way back into the gate (LAW 4). The
 * change-password gate (RA-54) is now backed by a real `POST /auth/change-password` +
 * `must_change_password` flag: a provisioned teacher logs in with profileCompleted=false, the
 * gate calls the endpoint, the server flips profile_completed=true, and the gate resolves
 * permanently across cold starts.
 */
@Composable
private fun AuthedFlow(
    role: EntryRole,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val authRepository = koinInject<AuthRepository>()

    var route by remember(role) { mutableStateOf(AuthedRoute.Resolving) }

    // Resolve the gate exactly once per authenticated session.
    LaunchedEffect(role) {
        val profileCompleted = runCatching { authRepository.getSession()?.profileCompleted }
            .getOrNull() ?: true // null session (returning user / restart) → treat as completed
        route = when (role) {
            EntryRole.Parent -> if (profileCompleted) AuthedRoute.Portal else AuthedRoute.ParentLinkChild
            // super_admin shares the school-admin operator surface (see RolePortal below)
            // and therefore shares its first-login gate too.
            EntryRole.SchoolAdmin,
            EntryRole.SuperAdmin -> if (profileCompleted) AuthedRoute.Portal else AuthedRoute.SchoolOnboarding
            EntryRole.Teacher -> if (profileCompleted) AuthedRoute.Portal else AuthedRoute.TeacherFirstLogin
            EntryRole.Unknown -> AuthedRoute.Portal
        }
    }

    // Inside a portal, back-press is owned by the portal's own tab logic; the gate screens never
    // allow a back path to auth/splash (the session is already established).
    AnimatedContent(
        targetState = route,
        // Gate steps (link-child / onboarding / first-login) read as modal sheets →
        // vertical rise + fade. The brief Resolving frame uses a quiet cross-fade so
        // the common (already-completed) path never shows a directional slide.
        transitionSpec = {
            if (initialState == AuthedRoute.Resolving || targetState == AuthedRoute.Resolving) {
                VMotion.quietFade()
            } else {
                VMotion.modalRise()
            }
        },
        label = "authed-flow",
        modifier = modifier,
    ) { current ->
        when (current) {
            // Brief resolving frame — themed background only, no spinner flash for the common
            // (already-completed) case which resolves on the first composition.
            AuthedRoute.Resolving -> androidx.compose.foundation.layout.Box(
                Modifier.then(modifier),
            ) {}

            AuthedRoute.ParentLinkChild -> ParentLinkChildScreenV2(
                onDone = { route = AuthedRoute.Portal },
                onBack = { route = AuthedRoute.Portal },
            )
            AuthedRoute.SchoolOnboarding -> SchoolOnboardingScreenV2(
                onComplete = { route = AuthedRoute.Portal },
                onBack = { route = AuthedRoute.Portal },
            )
            AuthedRoute.TeacherFirstLogin -> TeacherFirstLoginScreenV2(
                onDone = { route = AuthedRoute.Portal },
            )
            AuthedRoute.Portal -> RolePortal(role = role, onLogout = onLogout)
        }
    }
}

/** Maps the typed role to its self-contained, tabbed portal (the role's "dashboard"). */
@Composable
private fun RolePortal(role: EntryRole, onLogout: () -> Unit, modifier: Modifier = Modifier) {
    when (role) {
        // super_admin currently shares the school-admin operator surface (no
        // dedicated super-admin portal exists yet) — but it is no longer
        // silently dropped into the parent UI (audit §3.5).
        EntryRole.SchoolAdmin, EntryRole.SuperAdmin -> SchoolPortalV2(onLogout = onLogout, modifier = modifier)
        EntryRole.Teacher -> TeacherPortalV2(onLogout = onLogout, modifier = modifier)
        EntryRole.Parent -> ParentPortalV2(onLogout = onLogout, modifier = modifier)
        // Authenticated but role unknown → safest default is the parent surface.
        EntryRole.Unknown -> ParentPortalV2(onLogout = onLogout, modifier = modifier)
    }
}
