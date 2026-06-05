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

/**
 * ParentPortalV2 — the 4-tab parent shell, translated from Parent.tsx.
 *
 * Bottom nav: Home · Academics · Fees · Activity. Each leaf is the corresponding `*ScreenV2`, which
 * `koinViewModel()`s its own VM. The portal is `tone = Light` (lavender; set by the host `VTheme`).
 *
 * Reports / Messaging / PTM screens exist as separate parent VMs and are reached via deep-links in
 * Phase 3E navigation; the four tabs here mirror the design's primary surface.
 */
@Composable
fun ParentPortalV2(
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var tab by remember { mutableStateOf("home") }

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
                "home" -> ParentHomeScreenV2()
                "academics" -> ParentAcademicsScreenV2()
                "fees" -> ParentFeesScreenV2()
                "activity" -> ParentActivityScreenV2()
            }
        }
    }
}
