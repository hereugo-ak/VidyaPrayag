package com.littlebridge.vidyaprayag.ui.v2.screens.school

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
 * SchoolPortalV2 — the 5-tab admin shell, translated from Admin.tsx.
 *
 * Bottom nav: Home · People · Records · Comms · Settings. Each leaf is the corresponding
 * `School*ScreenV2`, which `koinViewModel()`s its own VM. The portal is `tone = Warm` (set by the
 * host `VTheme`). Many downstream admin routes are local-only stubs per the master doc §5.3; those
 * surfaces render `VComingSoon` inside their screens rather than fabricating data.
 */
@Composable
fun SchoolPortalV2(
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var tab by remember { mutableStateOf("home") }

    val items = listOf(
        VNavItem("home", "Home", VIcons.Home),
        VNavItem("people", "People", VIcons.Users),
        VNavItem("records", "Records", VIcons.Bookmark),
        VNavItem("comms", "Comms", VIcons.Megaphone),
        VNavItem("settings", "Settings", VIcons.Settings),
    )

    VScreenScaffold(
        modifier = modifier,
        bottomBar = {
            VBottomNav(items = items, selected = tab, onSelect = { tab = it })
        },
    ) { _ ->
        Box(Modifier.fillMaxSize()) {
            when (tab) {
                "home" -> SchoolHomeScreenV2()
                "people" -> SchoolPeopleScreenV2()
                "records" -> SchoolRecordsScreenV2()
                "comms" -> SchoolCommsScreenV2()
                "settings" -> SchoolSettingsScreenV2(onLogout = onLogout)
            }
        }
    }
}
