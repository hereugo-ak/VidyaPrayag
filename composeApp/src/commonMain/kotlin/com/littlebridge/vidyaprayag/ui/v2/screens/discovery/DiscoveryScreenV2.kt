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
import com.littlebridge.vidyaprayag.feature.schools.presentation.DiscoveredSchool
import com.littlebridge.vidyaprayag.feature.schools.presentation.SchoolDiscoveryState
import com.littlebridge.vidyaprayag.feature.schools.presentation.SchoolDiscoveryViewModel
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
import com.littlebridge.vidyaprayag.ui.v2.screens.VStateHost
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import org.koin.compose.viewmodel.koinViewModel

/** Internal view state for the discovery flow (mirrors React `DiscoveryApp` view union). */
private enum class DiscoveryView { List, Profile }

// React SRI pills use a fixed navy-blue ink (#0a3a76) on an arctic-blue (#C8DEFF) tint —
// independent of the warm/night remap. §8 / charts.tsx SRI styling.
private val SriInk = Color(0xFF0A3A76)
private val SriBg = Color(0xFFC8DEFF)

/**
 * DiscoveryScreenV2 — the marketplace, wired to the real
 * [SchoolDiscoveryViewModel] → `GET /api/v1/parent/schools/discover` endpoint
 * (BACKEND_GAPS.md §3 + Phase C batch 4). The previous MockV2 driver is gone;
 * the screen now renders genuine schools sourced from the server.
 *
 * Fields the backend does not currently send (board, type, fee range, medium, board result)
 * are surfaced as `Coming Soon` placeholders rather than fabricated — LAW 6 / no guessed
 * data. Per the React design, the search field + tag chips remain visible because they are
 * the page's primary navigation affordance, but server-side filtering is not yet wired
 * (Phase D will plumb `lat/lng` + chip filters into [SchoolDiscoveryViewModel.load]).
 */
@Composable
fun DiscoveryScreenV2(
    modifier: Modifier = Modifier,
    onOpenSchool: (String) -> Unit = {},
    viewModel: SchoolDiscoveryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    var view by remember { mutableStateOf(DiscoveryView.List) }
    var active by remember { mutableStateOf<DiscoveredSchool?>(null) }
    var compare by remember { mutableStateOf(setOf<String>()) }

    // If the profile view was requested but `active` is null (defensive), fall back to List.
    val effectiveView = if (view == DiscoveryView.Profile && active == null) DiscoveryView.List else view
    when (effectiveView) {
        DiscoveryView.List -> DiscoveryList(
            modifier = modifier,
            state = state,
            onQuery = viewModel::setQuery,
            onRetry = viewModel::load,
            compare = compare,
            onToggleCompare = { id ->
                compare = if (id in compare) compare - id else if (compare.size < 3) compare + id else compare
            },
            onOpen = { s -> active = s; view = DiscoveryView.Profile },
            // Header "Exit" sends the user out of the marketplace; no specific school selected,
            // so we pass an empty id to the host (NavGraphV2 only routes off non-empty ids).
            onExit = { onOpenSchool(active?.id.orEmpty()) },
        )
        DiscoveryView.Profile -> {
            // Smart-cast guard: `active` is a `var` in this scope, so we capture into a local.
            val s = active
            if (s != null) {
                SchoolProfile(
                    modifier = modifier,
                    school = s,
                    onBack = { view = DiscoveryView.List },
                    onEnquire = { onOpenSchool(s.id) },
                )
            }
        }
    }
}

@Composable
private fun DiscoveryList(
    modifier: Modifier,
    state: SchoolDiscoveryState,
    onQuery: (String) -> Unit,
    onRetry: () -> Unit,
    compare: Set<String>,
    onToggleCompare: (String) -> Unit,
    onOpen: (DiscoveredSchool) -> Unit,
    onExit: () -> Unit,
) {
    val c = VTheme.colors

    // Client-side query filter (substring against name + location). The endpoint already
    // supports `city=` for proper server-side filtering — Phase D will switch this over
    // once the design specifies per-key behaviour.
    val filtered = remember(state.schools, state.query) {
        val q = state.query.trim()
        if (q.isBlank()) state.schools
        else state.schools.filter {
            it.name.contains(q, ignoreCase = true) || it.location.contains(q, ignoreCase = true)
        }
    }

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
                value = state.query,
                onValueChange = onQuery,
                placeholder = "Find schools near you or by name",
                leadingIcon = VIcons.Search,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            // Tag chips are unfiltered today — they will become real filter pills once the
            // server-side filter contract for board / fee-range / SRI is finalised (Phase D).
            // Keeping the visual affordance preserves the React fidelity per LAW 5.
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("📍 Within 3 km", "🎓 CBSE", "💰 Fee range", "🏫 Type", "⭐ SRI").forEachIndexed { i, f ->
                    com.littlebridge.vidyaprayag.ui.v2.components.VTag(text = f, active = i == 0)
                }
            }
        }

        // body — Loading · Error · Empty · Content (LAW 3)
        VStateHost(
            loading = state.isLoading,
            error = state.errorMessage,
            isEmpty = !state.isLoading && state.errorMessage == null && filtered.isEmpty(),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            emptyTitle = if (state.query.isBlank()) "No schools yet" else "No matches",
            emptyBody = if (state.query.isBlank()) {
                "Schools registered on VidyaPrayag will appear here."
            } else {
                "Try another name or city."
            },
            emptyIcon = VIcons.Search,
            onRetry = onRetry,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                filtered.forEach { s ->
                    SchoolCard(
                        s,
                        inCompare = s.id in compare,
                        onToggleCompare = { onToggleCompare(s.id) },
                        onOpen = { onOpen(s) },
                    )
                }
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
    s: DiscoveredSchool,
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
                model = s.image,
                contentDescription = s.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f))),
                ),
            )
            // Distance pill — only shown when the server populated `distance_km`
            // (i.e. when lat/lng were available). LAW 6: don't fabricate "1.8 km" otherwise.
            s.distanceLabel?.let { dist ->
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
                    Text(dist, style = VTheme.type.label.colored(c.ink).copy(letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(s.name, style = VTheme.type.h3.colored(c.ink))
                Spacer(Modifier.height(6.dp))
                // Board/type badges were MockV2-only fields. The backend will surface them
                // in a follow-up — until then we show the city only, which we DO have.
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    VBadge(text = s.location, tone = VBadgeTone.Neutral)
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
                    Text(formatRating(s.rating), style = VTheme.type.dataSm.colored(SriInk))
                }
                Text("SRI score", style = VTheme.type.label.colored(c.ink3), modifier = Modifier.padding(top = 4.dp))
            }
        }
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
    school: DiscoveredSchool,
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
                    model = school.image,
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
                        VBadge(text = school.location, tone = VBadgeTone.Neutral)
                        // Dedicated SRI pill (real `rating` from the endpoint).
                        Row(
                            Modifier.clip(RoundedCornerShape(999.dp)).background(SriBg.copy(alpha = 0.30f)).padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(VIcons.Star, contentDescription = null, tint = SriInk, modifier = Modifier.size(11.dp))
                            Text("SRI ${formatRating(school.rating)}", style = VTheme.type.label.colored(SriInk).copy(letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified, fontWeight = FontWeight.SemiBold))
                        }
                    }
                    school.distanceLabel?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, style = VTheme.type.caption.colored(c.ink2))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VButton(text = "♡", onClick = {}, variant = VButtonVariant.Secondary, size = VButtonSize.Sm, tone = VButtonTone.Navy)
                    VButton(text = "Compare", onClick = {}, variant = VButtonVariant.Secondary, size = VButtonSize.Sm, tone = VButtonTone.Navy, full = true, modifier = Modifier.weight(1f))
                    VButton(text = "Enquire now", onClick = onEnquire, size = VButtonSize.Sm, tone = VButtonTone.Sky, soft = false, full = true, modifier = Modifier.weight(1f))
                }

                // About / academics / fees / reviews / location need richer endpoints
                // (school profile API, fees API, reviews API). Until they ship, surface
                // the real "we don't have this yet" message rather than fabricating data.
                ProfileSection("About") {
                    VComingSoon(
                        title = "School profile",
                        description = "Rich school descriptions and tags will appear here once schools complete their public profile in the admin portal.",
                    )
                }
                ProfileSection("Academics") {
                    VComingSoon(
                        title = "Academics",
                        description = "Board, classes offered, medium, ratios — these will populate from each school's institutional profile.",
                    )
                }
                ProfileSection("Fee structure") {
                    VComingSoon(
                        title = "Fee structure",
                        description = "Tuition and one-time fees will appear once the school admin publishes its fee plan.",
                    )
                }
                ProfileSection("SRI breakdown") {
                    VComingSoon(
                        title = "School Reputation Index",
                        description = "Our 11-signal score lets you compare schools on academics, safety, facilities and parent sentiment.",
                        preview = { SriPreview(score = school.rating.toFloat()) },
                    )
                }
                ProfileSection("Parent reviews") {
                    VComingSoon(
                        title = "Parent reviews",
                        description = "Verified-parent reviews launch alongside the family link-child flow.",
                    )
                }
                ProfileSection("Location") {
                    VComingSoon(
                        title = "On the map",
                        description = "Map embedding ships with the upcoming Maps integration. City: ${school.location}.",
                    )
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

@Suppress("unused")
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

/** "8.4" / "9.0" — server sends a Double rating; format to one decimal. */
private fun formatRating(r: Double): String {
    if (r <= 0.0) return "—"
    val rounded = (r * 10).toLong()
    return "${rounded / 10}.${rounded % 10}"
}
