package com.littlebridge.vidyaprayag.ui.v2.screens.school

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

/**
 * SchoolSettingsScreenV2 — a pixel-faithful copy of `Admin.tsx → SettingsScreen`.
 *
 * A stack of icon + title + subtitle setting rows, ending with a log-out button.
 */
@Composable
fun SchoolSettingsScreenV2(
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors

    // Exactly the 8 rows in Admin.tsx → SettingsScreen (no standalone log-out button exists there).
    // The Account row carries the logout affordance so we keep parity without adding a non-React control.
    val rows = listOf(
        SettingRow(VIcons.GraduationCap, "School profile", "Logo, address, contact info"),
        SettingRow(VIcons.Calendar, "Academic year", "Currently 2025-26 • 173 days left"),
        SettingRow(VIcons.BookOpen, "Classes & subjects", "6 classes • 42 subjects"),
        SettingRow(VIcons.Users, "Teacher management", "8 teachers • download credentials"),
        SettingRow(VIcons.Wallet, "Fee structure", "Edit heads & amounts for next cycle"),
        SettingRow(VIcons.Bell, "Notifications", "Channels & quiet hours"),
        SettingRow(VIcons.Download, "Data export", "CSV / PDF / UDISE (Coming Soon)"),
        SettingRow(VIcons.Settings, "Account", "Admin email & password", onClick = onLogout),
    )

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Settings", style = VTheme.type.h1.colored(c.ink))
        rows.forEach { row ->
            VCard(onClick = row.onClick) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(c.ink.copy(alpha = 0.06f)), contentAlignment = Alignment.Center) {
                        Icon(row.icon, contentDescription = null, tint = c.ink, modifier = Modifier.size(18.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(row.title, style = VTheme.type.bodyStrong.colored(c.ink))
                        // React sub: 11px, text-dark-2.
                        Text(row.sub, style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 11.sp))
                    }
                    // React: ChevronRight size 16, opacity-50 on the default foreground.
                    Icon(VIcons.ChevronRight, contentDescription = null, tint = c.ink.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

private data class SettingRow(
    val icon: ImageVector,
    val title: String,
    val sub: String,
    val onClick: (() -> Unit)? = null,
)
