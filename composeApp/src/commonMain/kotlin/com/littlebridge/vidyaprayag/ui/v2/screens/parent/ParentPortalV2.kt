package com.littlebridge.vidyaprayag.ui.v2.screens.parent

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.littlebridge.vidyaprayag.ui.v2.components.VBottomNav
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VNavItem
import com.littlebridge.vidyaprayag.ui.v2.components.VScreenScaffold
import com.littlebridge.vidyaprayag.ui.v2.screens.discovery.AcademicCalendarScreenV2
import com.littlebridge.vidyaprayag.ui.v2.screens.notifications.NotificationsScreenV2

/** Full-screen overlays a portal can push above its tab content (back returns to the tabs). */
private enum class ParentOverlay { None, Notifications, Calendar }

/**
 * ParentPortalV2 — the 4-tab parent shell, translated from Parent.tsx.
 *
 * Bottom nav: Home · Academics · Fees · Activity. Each leaf is the corresponding `*ScreenV2`, which
 * `koinViewModel()`s its own VM. The portal is `tone = Light` (lavender; set by the host `VTheme`).
 *
 * Cross-portal destinations from the React `App.tsx` graph (Notifications, AcademicCalendar) are
 * pushed as full-screen overlays over the tabs, matching the design where the bell / calendar entries
 * open those screens with a back affordance.
 */
@Composable
fun ParentPortalV2(
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var tab by remember { mutableStateOf("home") }
    var overlay by remember { mutableStateOf(ParentOverlay.None) }

    // Overlays take over the whole portal; back returns to the tab shell.
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
        bottomBar = {
            VBottomNav(items = items, selected = tab, onSelect = { tab = it })
        },
    ) { _ ->
        Box(Modifier.fillMaxSize()) {
            when (tab) {
                "home" -> ParentHomeScreenV2(
                    onOpenNotifications = { overlay = ParentOverlay.Notifications },
                    onOpenCalendar = { overlay = ParentOverlay.Calendar },
                )
                "academics" -> ParentAcademicsScreenV2()
                "fees" -> ParentFeesScreenV2()
                "activity" -> ParentActivityScreenV2()
            }
        }
    }
}
