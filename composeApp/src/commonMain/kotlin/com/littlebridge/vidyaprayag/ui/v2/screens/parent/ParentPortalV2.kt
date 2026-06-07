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
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.feature.parent.presentation.TrackProgressViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatar
import com.littlebridge.vidyaprayag.ui.v2.components.VBottomNav
import com.littlebridge.vidyaprayag.ui.v2.components.VDivider
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VNavItem
import com.littlebridge.vidyaprayag.ui.v2.components.VScreenScaffold
import com.littlebridge.vidyaprayag.ui.v2.components.VStatusDot
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.screens.discovery.AcademicCalendarScreenV2
import com.littlebridge.vidyaprayag.ui.v2.screens.notifications.NotificationsScreenV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/** Full-screen overlays a portal can push above its tab content (back returns to the tabs). */
private enum class ParentOverlay { None, Notifications, Calendar }

/**
 * ParentPortalV2 — the 4-tab parent shell, a faithful copy of `Parent.tsx → ParentApp`.
 *
 * Owns the header (child identity from the real [TrackProgressViewModel]) and the bottom nav
 * (Home · Academics · Fees · Activity). Each leaf is now wired to its own real ViewModel via
 * `koinViewModel()` — no MockV2 in any production path. Notifications & Calendar are pushed as
 * full-screen overlays.
 */
@Composable
fun ParentPortalV2(
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier,
    headerViewModel: TrackProgressViewModel = koinViewModel(),
) {
    var tab by remember { mutableStateOf("home") }
    var overlay by remember { mutableStateOf(ParentOverlay.None) }
    val progress by headerViewModel.state.collectAsStateV2()

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
            ParentHeader(
                childName = progress.childName,
                childSubline = progress.journeyDescription.ifBlank { "Level ${progress.currentLevel}" },
                onOpenNotifications = { overlay = ParentOverlay.Notifications },
                onExit = onLogout,
            )
        },
        bottomBar = {
            VBottomNav(items = items, selected = tab, onSelect = { tab = it })
        },
    ) { _ ->
        Box(Modifier.fillMaxSize()) {
            when (tab) {
                "home" -> ParentHomeScreenV2()
                "academics" -> ParentAcademicsScreenV2()
                "fees" -> ParentFeesScreenV2()
                "activity" -> ParentActivityScreenV2()
            }
        }
    }
}

/**
 * ParentHeader — child identity chip (from real track-progress data), a notification bell with an
 * unread dot, and the parent's avatar (exit). Faithful to `Parent.tsx → ChildSwitcher`, minus the
 * sibling picker (multi-child linking lands with the parent-link-child backend).
 */
@Composable
private fun ParentHeader(
    childName: String,
    childSubline: String,
    onOpenNotifications: () -> Unit,
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
                    VStatusDot(color = c.dangerInk, size = 6.dp, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
                }
                val exitInteraction = remember { MutableInteractionSource() }
                Box(Modifier.clickable(interactionSource = exitInteraction, indication = null) { onExit() }) {
                    VAvatar(name = "Parent", size = 32.dp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        VDivider()
    }
}
