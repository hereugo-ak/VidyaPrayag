package com.littlebridge.vidyaprayag.ui.v2.screens.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.littlebridge.vidyaprayag.domain.util.UiState
import com.littlebridge.vidyaprayag.feature.content.domain.model.LandingData
import com.littlebridge.vidyaprayag.feature.content.domain.model.LandingItem
import com.littlebridge.vidyaprayag.feature.content.presentation.LandingViewModel
import com.littlebridge.vidyaprayag.feature.schools.domain.model.School
import com.littlebridge.vidyaprayag.presentation.MainViewModel
import com.littlebridge.vidyaprayag.ui.v2.components.VBadge
import com.littlebridge.vidyaprayag.ui.v2.components.VBadgeTone
import com.littlebridge.vidyaprayag.ui.v2.components.VBrandLogo
import com.littlebridge.vidyaprayag.ui.v2.components.VCard
import com.littlebridge.vidyaprayag.ui.v2.components.VIcons
import com.littlebridge.vidyaprayag.ui.v2.screens.collectAsStateV2
import com.littlebridge.vidyaprayag.ui.v2.theme.VMotion
import com.littlebridge.vidyaprayag.ui.v2.theme.VPortalTone
import com.littlebridge.vidyaprayag.ui.v2.theme.VTheme
import com.littlebridge.vidyaprayag.ui.v2.theme.colored
import com.littlebridge.vidyaprayag.ui.v2.theme.pressScale
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import kotlinx.coroutines.launch
import vidyaprayag.composeapp.generated.resources.Res
import vidyaprayag.composeapp.generated.resources.landing_school_1
import vidyaprayag.composeapp.generated.resources.landing_school_2
import vidyaprayag.composeapp.generated.resources.landing_school_3

/**
 * CommonLandingScreenV2 — the app's first interactive surface for users with no session.
 *
 * Rebuilt in the current design language (V* primitives, VColors tokens, VMotion). It carries the
 * full marketing content of the legacy `CommonLandingScreen` — hero, **Featured Institutions**,
 * social-proof trust strip, the two role-entry cards, the **Next-Gen Intelligence** showcase and
 * the **Portal access** rail — but every pixel is rendered with the new theme. The legacy file is
 * referenced for **content only**; none of its Material-3 chrome survives.
 *
 * Anatomy:
 *  • teal hero — [VBrandLogo] + halo, wordmark, value tagline, frosted trust pill.
 *  • lifted lavender sheet — trust strip, featured-school photo rail, the two role-entry cards
 *    (Parent → OTP auth, School / Administration → credential auth), the intelligence showcase,
 *    the portal-access rail and a privacy footnote.
 *
 * The two role-entry cards remain the only auth CTAs (teachers sign in through the Admin path —
 * their credentials are minted inside a school's admin account).
 *
 * Content is CMS-driven ([LandingViewModel] → GET /api/v1/content/landing); the hardcoded strings
 * are graceful fallbacks so a public screen is never blocked on a network state. Featured schools
 * come from [MainViewModel.schools]; their photos use the server `imageUrl`, falling back to bundled
 * professional campus photography when the CMS hasn't supplied an image yet.
 */
@Composable
fun CommonLandingScreenV2(
    onParent: () -> Unit,
    onAdmin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LandingViewModel = koinViewModel(),
    mainViewModel: MainViewModel = koinViewModel(),
) = VTheme(tone = VPortalTone.Light) {
    val c = VTheme.colors
    val d = VTheme.dimens

    val landingState by viewModel.landingState.collectAsStateV2()
    val cms: LandingData? = (landingState as? UiState.Success)?.data

    val schoolsState by mainViewModel.schools.collectAsStateV2()
    val schools: List<School> = (schoolsState as? UiState.Success)?.data ?: emptyList()

    // Reveal ladder (mirrors the Welcome/Splash motion vocabulary).
    val logoScale = remember { Animatable(0.84f) }
    val logoAlpha = remember { Animatable(0f) }
    val wordAlpha = remember { Animatable(0f) }
    val wordY = remember { Animatable(12f) }
    val sheetY = remember { Animatable(64f) }
    val sheetAlpha = remember { Animatable(0f) }
    val proofAlpha = remember { Animatable(0f) }
    val cardsAlpha = remember { Animatable(0f) }
    val cardsY = remember { Animatable(16f) }

    LaunchedEffect(Unit) {
        launch { logoScale.animateTo(1f, VMotion.springSoft) }
        launch { logoAlpha.animateTo(1f, tween(380)) }
        launch { wordAlpha.animateTo(1f, tween(420, delayMillis = 220)) }
        launch { wordY.animateTo(0f, tween(420, delayMillis = 220)) }
        launch { sheetY.animateTo(0f, VMotion.springSheet) }
        launch { sheetAlpha.animateTo(1f, tween(320, delayMillis = 140)) }
        launch { proofAlpha.animateTo(1f, tween(400, delayMillis = 320)) }
        launch { cardsAlpha.animateTo(1f, tween(450, delayMillis = 420)) }
        launch { cardsY.animateTo(0f, tween(450, delayMillis = 420)) }
    }

    val halo = rememberInfiniteTransition(label = "land-halo")
    val haloAlpha by halo.animateFloat(
        initialValue = 0f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2400
                0f at 0; 0.6f at 1200; 0f at 2400
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "land-haloAlpha",
    )

    Column(
        modifier
            .fillMaxSize()
            .background(c.background)
            .verticalScroll(rememberScrollState())
            .widthIn(max = d.maxContentWidth),
    ) {
        // ── Teal hero ─────────────────────────────────────────────────────────────
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 380.dp)
                .background(c.teal)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                            center = Offset(size.width * 0.5f, size.height * 0.32f),
                            radius = size.maxDimension * 0.55f,
                        ),
                        radius = size.maxDimension * 0.55f,
                        center = Offset(size.width * 0.5f, size.height * 0.32f),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier.statusBarsPadding().padding(top = 64.dp, bottom = 72.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                VBrandLogo(
                    size = 132.dp,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = logoScale.value
                            scaleY = logoScale.value
                            alpha = logoAlpha.value
                        }
                        .drawBehind {
                            drawRoundRect(
                                color = Color.White.copy(alpha = (haloAlpha / 0.6f).coerceIn(0f, 1f) * 0.10f),
                                topLeft = Offset(-12.dp.toPx(), -12.dp.toPx()),
                                size = Size(size.width + 24.dp.toPx(), size.height + 24.dp.toPx()),
                                cornerRadius = CornerRadius(40.dp.toPx(), 40.dp.toPx()),
                                style = Stroke(width = 12.dp.toPx()),
                            )
                        },
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    "VidyaSetu",
                    style = VTheme.type.h1.colored(Color.White)
                        .copy(fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.02).em),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer {
                        alpha = wordAlpha.value
                        translationY = wordY.value * density
                    },
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    cms?.let { listOf(it.topTagline, it.subTagline).filter(String::isNotBlank).joinToString(" ") }
                        ?.takeIf { it.isNotBlank() }
                        ?: "Education with trust. Progress with purpose.",
                    style = VTheme.type.body.colored(Color.White.copy(alpha = 0.92f)).copy(fontSize = 14.sp),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .padding(horizontal = d.lg)
                        .graphicsLayer { alpha = wordAlpha.value },
                )
                Spacer(Modifier.height(18.dp))
                // Premium trust pill — frosted glass on the teal hero, reads as bank-grade reassurance.
                Row(
                    modifier = Modifier
                        .graphicsLayer { alpha = wordAlpha.value }
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.16f))
                        .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(VIcons.ShieldCheck, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Text(
                        "Bank-grade encryption",
                        style = VTheme.type.caption.colored(Color.White).copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                    )
                }
            }
        }

        // ── Lifted sheet ──────────────────────────────────────────────────────────
        Column(
            Modifier
                .fillMaxWidth()
                .offset(y = (-32).dp)
                .graphicsLayer {
                    translationY = sheetY.value * density
                    alpha = sheetAlpha.value
                }
                .drawBehind {
                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.06f),
                        topLeft = Offset(0f, -10.dp.toPx()),
                        size = Size(size.width, 32.dp.toPx()),
                        cornerRadius = CornerRadius(32.dp.toPx(), 32.dp.toPx()),
                    )
                }
                .background(c.background, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .navigationBarsPadding()
                .padding(top = 32.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // trust strip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.graphicsLayer { alpha = proofAlpha.value },
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                    listOf(c.success, c.warning, Color(0xFFC8DEFF)).forEach { ac ->
                        Box(
                            Modifier.size(24.dp).clip(CircleShape).background(ac)
                                .border(2.dp, c.background, CircleShape),
                        )
                    }
                }
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = c.ink)) { append("240+ schools") }
                        withStyle(SpanStyle(color = c.ink2)) { append(" · 38k parents trust us") }
                    },
                    style = VTheme.type.caption.colored(c.ink2),
                )
            }

            // ── Featured institutions (real-photo rail) ──
            if (schools.isNotEmpty()) {
                Spacer(Modifier.height(28.dp))
                FeaturedInstitutionsSection(
                    schools = schools,
                    onSchoolClick = onParent,
                    modifier = Modifier.graphicsLayer { alpha = proofAlpha.value },
                )
            }

            Spacer(Modifier.height(28.dp))
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                Text(
                    "How would you like to continue?",
                    style = VTheme.type.h2.colored(c.navy),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Pick your role to sign in to the right experience.",
                    style = VTheme.type.body.colored(c.ink2).copy(fontSize = 14.sp),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(24.dp))

                // ── the two role-entry cards ──
                Column(
                    Modifier.fillMaxWidth().graphicsLayer {
                        alpha = cardsAlpha.value
                        translationY = cardsY.value * density
                    },
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    RoleCard(
                        icon = VIcons.Users,
                        accent = c.teal,
                        accentInk = c.tealDeep,
                        title = "I'm a Parent",
                        body = cms?.parentInfo?.subTagline?.takeIf { it.isNotBlank() }
                            ?: "Track attendance, fees, homework and your child's progress — secured by a quick OTP sign-in.",
                        onClick = onParent,
                    )
                    RoleCard(
                        icon = VIcons.School,
                        accent = c.warmOrange,
                        accentInk = c.warmOrange,
                        title = "School / Administration",
                        body = cms?.schoolInfo?.subTagline?.takeIf { it.isNotBlank() }
                            ?: "For school admins and teachers. Manage classes, staff, records and announcements.",
                        onClick = onAdmin,
                    )
                }
            }

            // ── Next-Gen Intelligence showcase ──
            val offerings = cms?.listOfOfferings.orEmpty()
            if (offerings.isNotEmpty()) {
                Spacer(Modifier.height(32.dp))
                ShowcaseSection(
                    title = "Next-Gen Intelligence",
                    description = "Proprietary systems powering the ecosystem.",
                    items = offerings,
                    modifier = Modifier.graphicsLayer { alpha = cardsAlpha.value },
                )
            }

            // ── Portal access rail ──
            val portals = cms?.listOfPortals.orEmpty()
            if (portals.isNotEmpty()) {
                Spacer(Modifier.height(32.dp))
                PortalAccessSection(
                    portals = portals,
                    onLoginClick = onAdmin,
                    modifier = Modifier.graphicsLayer { alpha = cardsAlpha.value },
                )
            }

            Spacer(Modifier.height(24.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = c.ink3)) { append("By continuing you agree to our ") }
                    withStyle(SpanStyle(color = c.tealDeep, fontWeight = FontWeight.SemiBold)) { append("Terms") }
                    withStyle(SpanStyle(color = c.ink3)) { append(" & ") }
                    withStyle(SpanStyle(color = c.tealDeep, fontWeight = FontWeight.SemiBold)) { append("Privacy Policy") }
                },
                style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 11.sp),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            )
        }
    }
}

/** Bundled professional campus photography, used as a graceful fallback for schools lacking a CMS image. */
private val fallbackSchoolPhotos: List<DrawableResource> = listOf(
    Res.drawable.landing_school_1,
    Res.drawable.landing_school_2,
    Res.drawable.landing_school_3,
)

/**
 * Featured-institutions rail — a horizontally scrolling row of real-photo school cards (the legacy
 * "Featured Institutions" section, re-skinned). Each card shows a campus photo (server `imageUrl`,
 * falling back to bundled professional photography), the name, the city and an SRI score pill.
 */
@Composable
private fun FeaturedInstitutionsSection(
    schools: List<School>,
    onSchoolClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    Column(modifier.fillMaxWidth()) {
        SectionHeader(
            eyebrow = "Trusted campuses",
            title = "Featured Institutions",
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.width(10.dp))
            schools.forEachIndexed { index, school ->
                FeaturedSchoolCard(
                    school = school,
                    fallback = fallbackSchoolPhotos[index % fallbackSchoolPhotos.size],
                    onClick = onSchoolClick,
                )
            }
            Spacer(Modifier.width(10.dp))
        }
    }
}

@Composable
private fun FeaturedSchoolCard(
    school: School,
    fallback: DrawableResource,
    onClick: () -> Unit,
) {
    val c = VTheme.colors
    val interaction = remember { MutableInteractionSource() }
    VCard(
        modifier = Modifier
            .width(248.dp)
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        padding = 0.dp,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
        ) {
            if (school.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = school.imageUrl,
                    contentDescription = school.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                androidx.compose.foundation.Image(
                    painter = painterResource(fallback),
                    contentDescription = school.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            // Legibility scrim.
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.38f))),
                ),
            )
            if (school.isVerified) {
                Row(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.90f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(VIcons.ShieldCheck, contentDescription = null, tint = c.tealDeep, modifier = Modifier.size(11.dp))
                    Text("Verified", style = VTheme.type.label.colored(c.ink))
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(school.name, style = VTheme.type.h3.colored(c.ink), maxLines = 1)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(VIcons.MapPin, contentDescription = null, tint = c.ink3, modifier = Modifier.size(13.dp))
                Text(
                    school.location.ifBlank { school.board },
                    style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 12.sp),
                    maxLines = 1,
                )
            }
            if (school.sriScore > 0.0) {
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFFC8DEFF).copy(alpha = 0.30f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(VIcons.Star, contentDescription = null, tint = Color(0xFF0A3A76), modifier = Modifier.size(12.dp))
                    Text(formatScore(school.sriScore), style = VTheme.type.dataSm.colored(Color(0xFF0A3A76)))
                    Text("SRI", style = VTheme.type.label.colored(c.ink3))
                }
            }
        }
    }
}

/**
 * Showcase section — the legacy "Next-Gen Intelligence" moat strip, re-skinned. Renders the CMS
 * `list_of_offerings` as tinted feature cards, each with a live / coming-soon badge.
 */
@Composable
private fun ShowcaseSection(
    title: String,
    description: String,
    items: List<LandingItem>,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    Column(modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        SectionHeader(eyebrow = "The VidyaSetu moat", title = title)
        Spacer(Modifier.height(4.dp))
        Text(description, style = VTheme.type.body.colored(c.ink2).copy(fontSize = 13.sp))
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items.forEach { item -> OfferingCard(item) }
        }
    }
}

@Composable
private fun OfferingCard(item: LandingItem) {
    val c = VTheme.colors
    VCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(13.dp)).background(c.teal.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(VIcons.Sparkles, contentDescription = null, tint = c.tealDeep, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.heading, style = VTheme.type.h3.colored(c.ink), maxLines = 1)
                    VBadge(
                        text = if (item.isLive) "Live" else "Coming soon",
                        tone = if (item.isLive) VBadgeTone.Success else VBadgeTone.Neutral,
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    item.description,
                    style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 12.sp, lineHeight = 16.sp),
                )
            }
        }
    }
}

/**
 * Portal-access rail — the legacy "Portal access" section, re-skinned. Lists the CMS
 * `list_of_portals` as compact entry rows that route to the credential (Admin) sign-in.
 */
@Composable
private fun PortalAccessSection(
    portals: List<LandingItem>,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    Column(modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        SectionHeader(eyebrow = "For institutions", title = "Portal access")
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            portals.forEach { portal ->
                val interaction = remember { MutableInteractionSource() }
                VCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressScale(interaction)
                        .clickable(interactionSource = interaction, indication = null, onClick = onLoginClick),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(
                            Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
                                .background(c.warmOrange.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(VIcons.GraduationCap, contentDescription = null, tint = c.warmOrange, modifier = Modifier.size(20.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(portal.heading, style = VTheme.type.h3.colored(c.ink), maxLines = 1)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                portal.description,
                                style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 12.sp),
                                maxLines = 2,
                            )
                        }
                        Icon(VIcons.ArrowRight, contentDescription = null, tint = c.ink3, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

/** Shared section header — a tinted eyebrow over a heading, used across the marketing sections. */
@Composable
private fun SectionHeader(eyebrow: String, title: String, modifier: Modifier = Modifier) {
    val c = VTheme.colors
    Column(modifier) {
        Text(
            eyebrow.uppercase(),
            style = VTheme.type.label.colored(c.tealDeep).copy(fontWeight = FontWeight.Bold, letterSpacing = 0.08.em),
        )
        Spacer(Modifier.height(4.dp))
        Text(title, style = VTheme.type.h2.colored(c.navy))
    }
}

/** A tappable role-entry card: tinted icon chip + title + supporting copy + chevron, with press-give. */
@Composable
private fun RoleCard(
    icon: ImageVector,
    accent: Color,
    accentInk: Color,
    title: String,
    body: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    val interaction = remember { MutableInteractionSource() }
    VCard(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        padding = 0.dp,
    ) {
        Column(Modifier.fillMaxWidth()) {
            // Premium accent bar — a thin role-tinted gradient strip seating the card in its colour.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Brush.horizontalGradient(listOf(accent.copy(alpha = 0.95f), accent.copy(alpha = 0.45f))),
                    ),
            )
            Row(
                Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = accentInk, modifier = Modifier.size(26.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(title, style = VTheme.type.h3.colored(c.ink))
                    Spacer(Modifier.height(3.dp))
                    Text(body, style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 12.sp, lineHeight = 16.sp))
                }
                Icon(VIcons.ArrowRight, contentDescription = null, tint = c.ink3, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** Formats an SRI score to one decimal without pulling in a platform number formatter. */
private fun formatScore(value: Double): String {
    val rounded = (value * 10).toLong()
    return "${rounded / 10}.${rounded % 10}"
}
