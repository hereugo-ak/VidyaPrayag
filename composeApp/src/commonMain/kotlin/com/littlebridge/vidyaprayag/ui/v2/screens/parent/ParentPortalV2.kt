package com.littlebridge.vidyaprayag.ui.v2.screens.parent

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.feature.parent.presentation.NotificationsViewModel
import com.littlebridge.vidyaprayag.feature.parent.presentation.TrackProgressViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatar
import com.littlebridge.vidyaprayag.ui.v2.components.VBottomNav
import com.littlebridge.vidyaprayag.ui.v2.components.VDivider
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VNavItem
import com.littlebridge.vidyaprayag.ui.v2.components.VScreenScaffold
import com.littlebridge.vidyaprayag.ui.v2.components.VStatusDot
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.screens.auth.ParentLinkChildScreenV2
import com.littlebridge.vidyaprayag.ui.v2.screens.discovery.AcademicCalendarScreenV2
import com.littlebridge.vidyaprayag.ui.v2.screens.discovery.DiscoveryScreenV2
import com.littlebridge.vidyaprayag.ui.v2.screens.notifications.NotificationsScreenV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/** Full-screen overlays a portal can push above its tab content (back returns to the tabs). */
private enum class ParentOverlay { None, Notifications, Calendar, Scholarships, Profile, Leave, Messages, LinkChild, Discovery }

/**
 * ParentPortalV2 — the 4-tab parent shell, a faithful copy of `Parent.tsx → ParentApp`.
 *
 * Owns the header (child identity from the real [TrackProgressViewModel]) and the bottom nav
 * (Home · Academics · Fees · Activity). Each leaf is now wired to its own real ViewModel via
 * `koinViewModel()` — no MockV2 in any production path. Notifications & Calendar are pushed as
 * full-screen overlays.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ParentPortalV2(
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier,
    headerViewModel: TrackProgressViewModel = koinViewModel(),
    // RA-S06: drives the header bell's unread dot from the real notifications feed.
    notificationsViewModel: NotificationsViewModel = koinViewModel(),
) {
    var tab by remember { mutableStateOf("home") }
    var overlay by remember { mutableStateOf(ParentOverlay.None) }
    val progress by headerViewModel.state.collectAsStateV2()
    val notifications by notificationsViewModel.state.collectAsStateV2()

    // §11 cross-platform — predictive back / edge-swipe dismisses the full-screen overlay back to
    // the tabs, not the portal.
    BackHandler(enabled = overlay != ParentOverlay.None) {
        overlay = ParentOverlay.None
    }

    when (overlay) {
        ParentOverlay.Notifications -> {
            NotificationsScreenV2(onBack = { overlay = ParentOverlay.None }, modifier = modifier)
            return
        }
        ParentOverlay.Calendar -> {
            AcademicCalendarScreenV2(onBack = { overlay = ParentOverlay.None }, modifier = modifier)
            return
        }
        ParentOverlay.Scholarships -> {
            ScholarshipsScreenV2(onBack = { overlay = ParentOverlay.None }, modifier = modifier)
            return
        }
        ParentOverlay.Profile -> {
            // §7 finding K — the avatar opens the real profile screen; logout now lives
            // on an explicit button inside it (no more "tap your photo = logout").
            // RA-S04 (directive): linking a child is opt-in from the profile, never forced
            // after signup/login — the "Linked children" row opens the link flow as an overlay.
            ParentProfileScreenV2(
                onBack = { overlay = ParentOverlay.None },
                onLogout = onLogout,
                onLinkChild = { overlay = ParentOverlay.LinkChild },
                onDiscoverSchools = { overlay = ParentOverlay.Discovery },
                modifier = modifier,
            )
            return
        }
        ParentOverlay.LinkChild -> {
            // RA-S04 (directive): parent-initiated child linking — reached only from the
            // Profile screen, never pushed automatically after auth. Done/back both return
            // to the tabs (the link request is a PENDING admin approval server-side).
            ParentLinkChildScreenV2(
                onDone = { overlay = ParentOverlay.None },
                onBack = { overlay = ParentOverlay.Profile },
                modifier = modifier,
            )
            return
        }
        ParentOverlay.Leave -> {
            // RA-44: the parent leg of the leave workflow.
            ParentLeaveScreenV2(onBack = { overlay = ParentOverlay.None }, modifier = modifier)
            return
        }
        ParentOverlay.Messages -> {
            // RA-51: parent ↔ teacher/admin messaging.
            ParentMessagesScreenV2(onBack = { overlay = ParentOverlay.None }, modifier = modifier)
            return
        }
        ParentOverlay.Discovery -> {
            // Marketplace browsing for AUTHENTICATED parents — the same DiscoveryScreenV2 that
            // serves the unauth flow, hosted as a portal overlay. Reached from the Profile's
            // "Discover schools" row and Home's "View all" featured-schools link. The header
            // "Exit" + system back both pop the overlay back to the tabs.
            DiscoveryScreenV2(
                onExit = { overlay = ParentOverlay.None },
                modifier = modifier,
            )
            return
        }
        ParentOverlay.None -> Unit
    }

    val items = listOf(
        VNavItem("home", "Home", VIcons.Home),
        VNavItem("academics", "Academics", VIcons.School),
        VNavItem("fees", "Fees", VIcons.Wallet),
        VNavItem("activity", "Activity", VIcons.Bell),
    )

    VScreenScaffold(
        modifier = modifier,
        topBar = {
            // The rebuilt Home dashboard renders its OWN greeting hero as the top bar
            // (a faithful port of the website reference), so the shared portal header is
            // suppressed on Home to avoid a duplicate bar. Every other tab keeps it.
            if (tab != "home") {
                ParentHeader(
                    childName = progress.childName,
                    childSubline = progress.journeyDescription.ifBlank { "Level ${progress.currentLevel}" },
                    // RA-S06: real account name (RA-S03 pref) + real unread count.
                    accountName = progress.accountName,
                    unreadCount = notifications.unreadCount,
                    onOpenNotifications = { overlay = ParentOverlay.Notifications },
                    onOpenMessages = { overlay = ParentOverlay.Messages },
                    onExit = { overlay = ParentOverlay.Profile },
                )
            }
        },
        bottomBar = {
            VBottomNav(items = items, selected = tab, onSelect = { tab = it })
        },
    ) { _ ->
        Box(Modifier.fillMaxSize()) {
            when (tab) {
                "home" -> ParentHomeScreenV2(
                    onDiscoverSchools = { overlay = ParentOverlay.Discovery },
                    onOpenNotifications = { overlay = ParentOverlay.Notifications },
                    onOpenFees = { tab = "fees" },
                )
                "academics" -> ParentAcademicsScreenV2(onOpenLeave = { overlay = ParentOverlay.Leave })
                "fees" -> ParentFeesScreenV2()
                "activity" -> ParentActivityScreenV2()
            }
        }
    }
}

/**
 * ParentHeader — child identity chip (from real track-progress data), a notification bell with an
 * unread dot, and the parent's avatar (opens the Profile screen, where logout lives — §7 finding K).
 * Faithful to `Parent.tsx → ChildSwitcher`, minus the sibling picker (multi-child linking lands with
 * the parent-link-child backend).
 */
@Composable
private fun ParentHeader(
    childName: String,
    childSubline: String,
    accountName: String,
    unreadCount: Int,
    onOpenNotifications: () -> Unit,
    onOpenMessages: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors

    Column(
        modifier
            .fillMaxWidth()
            .background(c.card)
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // child chip
            Row(
                Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(c.cream)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                VAvatar(name = childName.ifBlank { "?" }, size = 32.dp)
                Column {
                    Text(
                        childName.ifBlank { "Your child" },
                        style = VTheme.type.bodyStrong.colored(c.ink).copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                    )
                    Text(
                        childSubline,
                        style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 10.sp),
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // RA-51: parent messaging entry point.
                val chatInteraction = remember { MutableInteractionSource() }
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(c.cream)
                        .clickable(interactionSource = chatInteraction, indication = null) { onOpenMessages() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(VIcons.Chat, contentDescription = "Messages", tint = c.ink, modifier = Modifier.size(16.dp))
                }
                val bellInteraction = remember { MutableInteractionSource() }
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(c.cream)
                        .clickable(interactionSource = bellInteraction, indication = null) { onOpenNotifications() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(VIcons.Bell, contentDescription = "Notifications", tint = c.ink, modifier = Modifier.size(16.dp))
                    // RA-S06: only show the red unread dot when there is actually
                    // something unread — no more permanent cry-wolf indicator.
                    if (unreadCount > 0) {
                        VStatusDot(color = c.dangerInk, size = 6.dp, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
                    }
                }
                val exitInteraction = remember { MutableInteractionSource() }
                Box(Modifier.clickable(interactionSource = exitInteraction, indication = null) { onExit() }) {
                    // RA-S06: greet the real signed-in parent (RA-S03 persisted name);
                    // fall back to "Parent" only until the name resolves.
                    VAvatar(name = accountName.ifBlank { "Parent" }, size = 32.dp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        VDivider()
    }
}
