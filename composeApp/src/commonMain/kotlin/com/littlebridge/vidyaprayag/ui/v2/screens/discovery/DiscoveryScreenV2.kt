package com.littlebridge.vidyaprayag.ui.v2.screens.discovery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.littlebridge.vidyaprayag.ui.v2.components.VBackHeader
import com.littlebridge.vidyaprayag.ui.v2.components.VBadge
import com.littlebridge.vidyaprayag.ui.v2.components.VBadgeTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonSize
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonVariant
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VComingSoon
import com.littlebridge.vidyaprayag.ui.v2.components.VDivider
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.components.VInput
import com.littlebridge.vidyaprayag.ui.v2.components.VLabel
import com.littlebridge.vidyaprayag.ui.v2.data.MockV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored

/** Internal view state for the discovery flow (mirrors React `DiscoveryApp` view union). */
private enum class DiscoveryView { List, Profile }

// React SRI pills use a fixed navy-blue ink (#0a3a76) on an arctic-blue (#C8DEFF) tint —
// independent of the warm/night remap. §8 / charts.tsx SRI styling.
private val SriInk = Color(0xFF0A3A76)
private val SriBg = Color(0xFFC8DEFF)

/**
 * DiscoveryScreenV2 — a pixel-faithful copy of `Discovery.tsx → DiscoveryApp`.
 *
 * The marketplace: a white header ("Find your child's school" + search + filter tags), the school
 * cards (cover photo + distance pill + SRI + board/type badges + fee range + Compare/Enquire), and
 * an in-place school profile (hero photo + sections). Driven by [MockV2.discoverySchools].
 */
@Composable
fun DiscoveryScreenV2(
    modifier: Modifier = Modifier,
    onOpenSchool: (String) -> Unit = {},
) {
    var view by remember { mutableStateOf(DiscoveryView.List) }
    var active by remember { mutableStateOf(MockV2.discoverySchools.first()) }
    var compare by remember { mutableStateOf(setOf<String>()) }
    var query by remember { mutableStateOf("") }

    when (view) {
        DiscoveryView.List -> DiscoveryList(
            modifier = modifier,
            query = query,
            onQuery = { query = it },
            compare = compare,
            onToggleCompare = { id ->
                compare = if (id in compare) compare - id else if (compare.size < 3) compare + id else compare
            },
            onOpen = { s -> active = s; view = DiscoveryView.Profile },
            onExit = { onOpenSchool(active.id) },
        )
        DiscoveryView.Profile -> SchoolProfile(
            modifier = modifier,
            school = active,
            onBack = { view = DiscoveryView.List },
            onEnquire = { onOpenSchool(active.id) },
        )
    }
}

@Composable
private fun DiscoveryList(
    modifier: Modifier,
    query: String,
    onQuery: (String) -> Unit,
    compare: Set<String>,
    onToggleCompare: (String) -> Unit,
    onOpen: (MockV2.DiscoverySchool) -> Unit,
    onExit: () -> Unit,
) {
    val c = VTheme.colors
    Column(modifier.fillMaxSize().background(c.background)) {
        // header
        Column(
            Modifier
                .fillMaxWidth()
                .background(c.card)
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    VLabel("Discover")
                    Text("Find your child's school", style = VTheme.type.h2.colored(c.ink), modifier = Modifier.padding(top = 4.dp))
                }
                val exitInteraction = remember { MutableInteractionSource() }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(c.cream)
                        .clickable(interactionSource = exitInteraction, indication = null) { onExit() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    // React label is "Exit" (leaves the discovery flow).
                    Text("Exit", style = VTheme.type.caption.colored(c.ink2).copy(fontWeight = FontWeight.SemiBold))
                }
            }
            Spacer(Modifier.height(12.dp))
            VInput(
                value = query,
                onValueChange = onQuery,
                placeholder = "Find schools near you or by name",
                leadingIcon = VIcons.Search,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("📍 Within 3 km", "🎓 CBSE", "💰 Fee range", "🏫 Type", "⭐ SRI").forEachIndexed { i, f ->
                    com.littlebridge.vidyaprayag.ui.v2.components.VTag(text = f, active = i == 0)
                }
            }
        }

        // list
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MockV2.discoverySchools.forEach { s ->
                SchoolCard(s, inCompare = s.id in compare, onToggleCompare = { onToggleCompare(s.id) }, onOpen = { onOpen(s) })
            }
        }

        if (compare.isNotEmpty()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(c.navy)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "${compare.size} school${if (compare.size > 1) "s" else ""} selected",
                    style = VTheme.type.caption.colored(Color.White),
                    modifier = Modifier.weight(1f),
                )
                VButton(text = "Compare now", onClick = {}, size = VButtonSize.Sm, tone = VButtonTone.Sky, soft = false)
            }
        }
    }
}

@Composable
private fun SchoolCard(
    s: MockV2.DiscoverySchool,
    inCompare: Boolean,
    onToggleCompare: () -> Unit,
    onOpen: () -> Unit,
) {
    val c = VTheme.colors
    VCard {
        Box(
            Modifier
                .fillMaxWidth()
                .height(144.dp)
                .clip(RoundedCornerShape(12.dp)),
        ) {
            AsyncImage(
                model = s.photo,
                contentDescription = s.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f))),
                ),
            )
            Row(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.85f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(VIcons.MapPin, contentDescription = null, tint = c.tealDeep, modifier = Modifier.size(11.dp))
                Text(s.distance, style = VTheme.type.label.colored(c.ink).copy(letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(s.name, style = VTheme.type.h3.colored(c.ink))
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    VBadge(text = s.board, tone = VBadgeTone.Arctic)
                    VBadge(text = s.type, tone = VBadgeTone.Neutral)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                // React SRI pill: bg rgba(200,222,255,0.30) (arctic #C8DEFF@30%), ink #0a3a76.
                Row(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFFC8DEFF).copy(alpha = 0.30f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(VIcons.Star, contentDescription = null, tint = SriInk, modifier = Modifier.size(12.dp))
                    Text(s.sri.toString(), style = VTheme.type.dataSm.colored(SriInk))
                }
                Text("SRI score", style = VTheme.type.label.colored(c.ink3), modifier = Modifier.padding(top = 4.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(s.medium, style = VTheme.type.caption.colored(c.ink2))
            Text("Board result ${s.result}", style = VTheme.type.caption.colored(c.ink2))
        }
        Text("${s.feeRange} / year", style = VTheme.type.dataSm.colored(c.ink), modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VButton(
                text = if (inCompare) "In compare" else "Compare",
                onClick = onToggleCompare,
                variant = VButtonVariant.Secondary,
                size = VButtonSize.Sm,
                tone = VButtonTone.Navy,
                full = true,
                modifier = Modifier.weight(1f),
                leading = if (inCompare) {
                    { Icon(VIcons.Check, contentDescription = null, tint = c.ink, modifier = Modifier.size(14.dp)) }
                } else null,
            )
            VButton(
                text = "Enquire",
                onClick = onOpen,
                size = VButtonSize.Sm,
                tone = VButtonTone.Sky,
                soft = false,
                full = true,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SchoolProfile(
    modifier: Modifier,
    school: MockV2.DiscoverySchool,
    onBack: () -> Unit,
    onEnquire: () -> Unit,
) {
    val c = VTheme.colors
    Column(modifier.fillMaxSize().background(c.background)) {
        VBackHeader(title = "School profile", onBack = onBack, action = {
            Icon(VIcons.Share, contentDescription = "Share", tint = c.ink2, modifier = Modifier.size(18.dp))
        })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Box(Modifier.fillMaxWidth().height(224.dp)) {
                AsyncImage(
                    model = school.photo,
                    contentDescription = school.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)))))
            }
            Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text(school.name, style = VTheme.type.h2.colored(c.ink))
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        VBadge(text = school.board, tone = VBadgeTone.Arctic)
                        VBadge(text = school.type, tone = VBadgeTone.Neutral)
                        // React: dedicated SRI pill (Star + "SRI n") on #C8DEFF@30%, ink #0a3a76 — not a VBadge.
                        Row(
                            Modifier.clip(RoundedCornerShape(999.dp)).background(SriBg.copy(alpha = 0.30f)).padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(VIcons.Star, contentDescription = null, tint = SriInk, modifier = Modifier.size(11.dp))
                            Text("SRI ${school.sri}", style = VTheme.type.label.colored(SriInk).copy(letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified, fontWeight = FontWeight.SemiBold))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(school.distance, style = VTheme.type.caption.colored(c.ink2))
                        Text(school.medium, style = VTheme.type.caption.colored(c.ink2))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VButton(text = "♡", onClick = {}, variant = VButtonVariant.Secondary, size = VButtonSize.Sm, tone = VButtonTone.Navy)
                    VButton(text = "Compare", onClick = {}, variant = VButtonVariant.Secondary, size = VButtonSize.Sm, tone = VButtonTone.Navy, full = true, modifier = Modifier.weight(1f))
                    VButton(text = "Enquire now", onClick = onEnquire, size = VButtonSize.Sm, tone = VButtonTone.Sky, soft = false, full = true, modifier = Modifier.weight(1f))
                }

                ProfileSection("About") {
                    Text(
                        "Founded 1987 • English-medium co-educational institution focused on holistic CBSE-pattern education with strong emphasis on STEM and the arts.",
                        style = VTheme.type.caption.colored(c.ink2),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Smart Classrooms", "Library", "Sports Ground", "Computer Lab", "Canteen", "Transport").forEach {
                            VBadge(text = it, tone = VBadgeTone.Neutral)
                        }
                    }
                }
                ProfileSection("Academics") {
                    ProfileRow("Board", school.board)
                    ProfileRow("Classes offered", "Nursery – 12")
                    ProfileRow("Medium", school.medium)
                    ProfileRow("Board result (last yr)", school.result)
                    ProfileRow("Teacher–student ratio", "Coming Soon")
                }
                ProfileSection("Fee structure") {
                    ProfileRow("Tuition (annual)", school.feeRange)
                    ProfileRow("Admission", "₹ 18,000 (one-time)")
                    ProfileRow("Transport", "₹ 22,000 / yr")
                }
                ProfileSection("SRI breakdown") {
                    VComingSoon(
                        title = "School Reputation Index",
                        description = "Our 11-signal score lets you compare schools on academics, safety, facilities and parent sentiment.",
                        preview = { SriPreview(score = school.sri.toFloat()) },
                    )
                }
                ProfileSection("Parent reviews") {
                    VCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(4) { Icon(VIcons.Star, contentDescription = null, tint = Color(0xFFFFD4A3), modifier = Modifier.size(14.dp)) }
                            Icon(VIcons.Star, contentDescription = null, tint = c.border2, modifier = Modifier.size(14.dp))
                            Text("4.2 / 5 — 84 reviews", style = VTheme.type.dataSm.colored(c.ink2), modifier = Modifier.padding(start = 8.dp))
                        }
                        Text(
                            "\"Teachers actually respond. Attendance updates land within minutes.\" — Verified parent",
                            style = VTheme.type.caption.colored(c.ink2),
                            modifier = Modifier.padding(top = 12.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        VBadge(text = "Verified VidyaSetu parents only", tone = VBadgeTone.Success)
                    }
                }
                ProfileSection("Location") {
                    VCard {
                        Box(
                            Modifier.fillMaxWidth().height(128.dp).clip(RoundedCornerShape(10.dp)).background(c.cream),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(VIcons.MapPin, contentDescription = null, tint = c.ink3, modifier = Modifier.size(28.dp))
                        }
                        Text("12 Civil Lines, Lucknow, UP 226001", style = VTheme.type.caption.colored(c.ink), modifier = Modifier.padding(top = 8.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ProfileSection(title: String, content: @Composable () -> Unit) {
    Column {
        VLabel(title)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun ProfileRow(k: String, v: String) {
    val c = VTheme.colors
    Column {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(k, style = VTheme.type.caption.colored(c.ink2))
            Text(v, style = VTheme.type.caption.colored(c.ink).copy(fontWeight = FontWeight.SemiBold))
        }
        VDivider()
    }
}
