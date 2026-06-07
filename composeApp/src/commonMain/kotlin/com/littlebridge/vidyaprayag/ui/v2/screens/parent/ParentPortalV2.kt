package com.littlebridge.vidyaprayag.ui.v2.screens.parent

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.ui.v2.components.VAvatar
import com.littlebridge.vidyaprayag.ui.v2.components.VBottomNav
import com.littlebridge.vidyaprayag.ui.v2.components.VDivider
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VNavItem
import com.littlebridge.vidyaprayag.ui.v2.components.VScreenScaffold
import com.littlebridge.vidyaprayag.ui.v2.components.VStatusDot
import com.littlebridge.vidyaprayag.ui.v2.data.MockV2
import com.littlebridge.vidyaprayag.ui.v2.screens.discovery.AcademicCalendarScreenV2
import com.littlebridge.vidyaprayag.ui.v2.screens.notifications.NotificationsScreenV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

/** Full-screen overlays a portal can push above its tab content (back returns to the tabs). */
private enum class ParentOverlay { None, Notifications, Calendar }

/**
 * ParentPortalV2 — the 4-tab parent shell, a faithful copy of `Parent.tsx → ParentApp`.
 *
 * It owns the [ChildSwitcher] header (so the selected child is shared across all tabs) and the
 * bottom nav (Home · Academics · Fees · Activity, with a "2" badge on Activity). Each leaf renders
 * from [MockV2] so the screens look identical to the Figma prototype. Notifications & Calendar are
 * pushed as full-screen overlays, matching the design's bell / calendar entries.
 */
@Composable
fun ParentPortalV2(
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var tab by remember { mutableStateOf("home") }
    var overlay by remember { mutableStateOf(ParentOverlay.None) }
    var activeChild by remember { mutableStateOf(0) }
    val child = MockV2.siblings[activeChild]

    // §11 cross-platform — Android predictive back / iOS edge-swipe should
    // dismiss the full-screen Notifications/Calendar overlay back to the tabs,
    // not exit the portal entirely. Mirrors the React `onBack` wiring that
    // each overlay screen already passes to its sheet.
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
        VNavItem("activity", "Activity", VIcons.Bell, badge = 2),
    )

    VScreenScaffold(
        modifier = modifier,
        topBar = {
            ChildSwitcher(
                activeIndex = activeChild,
                child = child,
                onSelectChild = { activeChild = it },
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
                "home" -> ParentHomeScreenV2(child = child)
                "academics" -> ParentAcademicsScreenV2()
                "fees" -> ParentFeesScreenV2()
                "activity" -> ParentActivityScreenV2()
            }
        }
    }
}

/**
 * ChildSwitcher — the parent header: a tappable child chip (avatar + name + class) that expands a
 * sibling picker, a notification bell with an unread dot, and the parent's own avatar (exit).
 * Faithful to `Parent.tsx → ChildSwitcher`.
 */
@Composable
private fun ChildSwitcher(
    activeIndex: Int,
    child: MockV2.Student,
    onSelectChild: (Int) -> Unit,
    onOpenNotifications: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    var open by remember { mutableStateOf(false) }

    Column(
        modifier
            .fillMaxWidth()
            .background(c.card)
            // §11.1 — push the opaque switcher card below the status bar on
            // Android + iOS so the avatar/name aren't clipped by the notch.
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
            val chipInteraction = remember { MutableInteractionSource() }
            Row(
                Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(c.cream)
                    .clickable(interactionSource = chipInteraction, indication = null) { open = !open }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                VAvatar(name = child.name, size = 32.dp)
                Column {
                    // §4.1: child chip name = 13/700 (not bodyStrong 14)
                    Text(
                        child.name,
                        style = VTheme.type.bodyStrong.colored(c.ink).copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                    )
                    // §4.1: subline = 10px plain text-light-2 (not label 11/upper)
                    Text(
                        "Class ${MockV2.classDisplay(child.klass)} • ${MockV2.school.shortName}",
                        style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 10.sp),
                    )
                }
                Icon(VIcons.ChevronDown, contentDescription = null, tint = c.ink3, modifier = Modifier.size(14.dp))
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
                    VAvatar(name = MockV2.parentName, size = 32.dp)
                }
            }
        }

        AnimatedVisibility(visible = open) {
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MockV2.siblings.forEachIndexed { i, s ->
                    val rowInteraction = remember { MutableInteractionSource() }
                    val active = i == activeIndex
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            // §4.1: active row = light blue rgba(200,222,255,0.30) (not teal)
                            .background(if (active) Color(0xFFC8DEFF).copy(alpha = 0.30f) else c.cream)
                            .clickable(interactionSource = rowInteraction, indication = null) {
                                onSelectChild(i); open = false
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        VAvatar(name = s.name, size = 32.dp)
                        Column(Modifier.weight(1f)) {
                            // §4.1: sibling row name = 13/600 (React Parent.tsx L65 fontWeight:600)
                            Text(
                                s.name,
                                style = VTheme.type.bodyStrong.colored(c.ink).copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                            )
                            Text(
                                "Class ${MockV2.classDisplay(s.klass)}",
                                style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 10.sp),
                            )
                        }
                        if (active) {
                            // §4.1: check tint = deep blue #0a3a76
                            Icon(VIcons.Check, contentDescription = null, tint = Color(0xFF0A3A76), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        VDivider()
    }
}
