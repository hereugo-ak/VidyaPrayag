package com.littlebridge.vidyaprayag.ui.v2.screens.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.littlebridge.vidyaprayag.ui.v2.components.VButton
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonSize
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonTone
import com.littlebridge.vidyaprayag.ui.v2.components.VButtonVariant
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
 * CommonLandingScreenV2 — the app's flagship, no-session first surface.
 *
 * A complete marketing rebuild. The screen now reads as a category-defining product page: a
 * cinematic teal hero with a live campus photograph, a sharp value proposition, a credibility
 * strip, an "intelligence platform" showcase grounded in REAL shipped capability (SRI scores, the
 * PEWS early-warning radar, attendance, two-way messaging, admissions CRM, AI insights), a
 * featured-institutions rail, the two role-entry CTAs (Parent OTP / School onboarding), a portal
 * access rail, a closing call-to-action and the footer.
 *
 * Design language: every pixel is the V* system — [VColors] tokens, [VTheme.type] scale, [VMotion]
 * springs, [VCard]/[VButton]/[VBadge]/[VBrandLogo] primitives. Nothing hardcodes a hex except the
 * intentional gradient/scrim stops that don't exist as tokens.
 *
 * Content is CMS-driven ([LandingViewModel] → GET /api/v1/content/landing) with honest, shipped
 * fallbacks so the public screen is never blocked on a network state. Featured schools come from
 * [MainViewModel.schools]; photos use the server `imageUrl`, falling back to bundled professional
 * campus photography. The auth contract is unchanged: Parent CTA → [onParent] (OTP), School CTA →
 * [onAdmin] (credential); teachers sign in through the school's admin-minted credentials.
 */
@Composable
fun CommonLandingScreenV2(
    onParent: () -> Unit,
    onAdmin: () -> Unit,
    modifier: Modifier = Modifier,
    onLegal: (LegalDoc) -> Unit = {},
    viewModel: LandingViewModel = koinViewModel(),
    mainViewModel: MainViewModel = koinViewModel(),
) = VTheme(tone = VPortalTone.Light) {
    val c = VTheme.colors
    val d = VTheme.dimens

    val landingState by viewModel.landingState.collectAsStateV2()
    val cms: LandingData? = (landingState as? UiState.Success)?.data

    val schoolsState by mainViewModel.schools.collectAsStateV2()
    val schools: List<School> = (schoolsState as? UiState.Success)?.data ?: emptyList()

    // ── Reveal ladder (Welcome/Splash motion vocabulary) ───────────────────────
    val logoScale = remember { Animatable(0.82f) }
    val logoAlpha = remember { Animatable(0f) }
    val wordAlpha = remember { Animatable(0f) }
    val wordY = remember { Animatable(14f) }
    val heroPhotoAlpha = remember { Animatable(0f) }
    val heroPhotoY = remember { Animatable(20f) }
    val sheetY = remember { Animatable(72f) }
    val sheetAlpha = remember { Animatable(0f) }
    val proofAlpha = remember { Animatable(0f) }
    val cardsAlpha = remember { Animatable(0f) }
    val cardsY = remember { Animatable(18f) }

    LaunchedEffect(Unit) {
        launch { logoScale.animateTo(1f, VMotion.springSoft) }
        launch { logoAlpha.animateTo(1f, tween(360)) }
        launch { wordAlpha.animateTo(1f, tween(420, delayMillis = 200)) }
        launch { wordY.animateTo(0f, tween(420, delayMillis = 200)) }
        launch { heroPhotoAlpha.animateTo(1f, tween(520, delayMillis = 360)) }
        launch { heroPhotoY.animateTo(0f, VMotion.springSheet) }
        launch { sheetY.animateTo(0f, VMotion.springSheet) }
        launch { sheetAlpha.animateTo(1f, tween(340, delayMillis = 160)) }
        launch { proofAlpha.animateTo(1f, tween(420, delayMillis = 340)) }
        launch { cardsAlpha.animateTo(1f, tween(460, delayMillis = 440)) }
        launch { cardsY.animateTo(0f, tween(460, delayMillis = 440)) }
    }

    // Slow drifting halo behind the bridge mark — premium, never distracting.
    val halo = androidx.compose.animation.core.rememberInfiniteTransition(label = "land-halo")
    val haloPulse by halo.animateFloat(
        initialValue = 0.06f, targetValue = 0.18f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Reverse),
        label = "land-haloPulse",
    )

    Column(
        modifier
            .fillMaxSize()
            .background(c.background)
            .verticalScroll(rememberScrollState())
            .widthIn(max = d.maxContentWidth),
    ) {
        // ── Cinematic teal hero ────────────────────────────────────────────────
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 460.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(c.tealDeep, c.teal, c.teal.copy(alpha = 0.94f)),
                    ),
                )
                .drawBehind {
                    // Soft radial bloom centred on the mark.
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.20f), Color.Transparent),
                            center = Offset(size.width * 0.5f, size.height * 0.28f),
                            radius = size.maxDimension * 0.52f,
                        ),
                        radius = size.maxDimension * 0.52f,
                        center = Offset(size.width * 0.5f, size.height * 0.28f),
                    )
                    // Subtle corner glow for depth.
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.10f), Color.Transparent),
                            center = Offset(size.width * 0.92f, size.height * 0.06f),
                            radius = size.maxDimension * 0.30f,
                        ),
                        radius = size.maxDimension * 0.30f,
                        center = Offset(size.width * 0.92f, size.height * 0.06f),
                    )
                },
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                Modifier.statusBarsPadding().padding(top = 52.dp, bottom = 84.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Eyebrow micro-pill — frosted, sets the "platform" framing immediately.
                Row(
                    Modifier
                        .graphicsLayer { alpha = wordAlpha.value }
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.14f))
                        .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(VIcons.Sparkles, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                    Text(
                        "THE INTELLIGENCE LAYER FOR EDUCATION",
                        style = VTheme.type.label.colored(Color.White)
                            .copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.10.em, fontSize = 10.sp),
                    )
                }
                Spacer(Modifier.height(22.dp))
                VBrandLogo(
                    size = 116.dp,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = logoScale.value
                            scaleY = logoScale.value
                            alpha = logoAlpha.value
                        }
                        .drawBehind {
                            drawRoundRect(
                                color = Color.White.copy(alpha = haloPulse * logoAlpha.value),
                                topLeft = Offset(-14.dp.toPx(), -14.dp.toPx()),
                                size = Size(size.width + 28.dp.toPx(), size.height + 28.dp.toPx()),
                                cornerRadius = CornerRadius(40.dp.toPx(), 40.dp.toPx()),
                                style = Stroke(width = 14.dp.toPx()),
                            )
                        },
                )
                Spacer(Modifier.height(22.dp))
                Text(
                    "VidyaSetu",
                    style = VTheme.type.h1.colored(Color.White)
                        .copy(fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.025).em),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer {
                        alpha = wordAlpha.value
                        translationY = wordY.value * density
                    },
                )
                Spacer(Modifier.height(12.dp))
                // Headline value statement — two lines, CMS-overridable, no fabricated claims.
                Text(
                    cms?.topTagline?.takeIf { it.isNotBlank() }
                        ?: "Where every school runs on insight,",
                    style = VTheme.type.h2.colored(Color.White)
                        .copy(fontSize = 19.sp, fontWeight = FontWeight.SemiBold, lineHeight = 26.sp),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .widthIn(max = 330.dp)
                        .padding(horizontal = d.lg)
                        .graphicsLayer { alpha = wordAlpha.value },
                )
                Text(
                    cms?.subTagline?.takeIf { it.isNotBlank() }
                        ?: "and every parent moves with confidence.",
                    style = VTheme.type.h2.colored(Color.White.copy(alpha = 0.90f))
                        .copy(fontSize = 19.sp, fontWeight = FontWeight.SemiBold, lineHeight = 26.sp),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .widthIn(max = 330.dp)
                        .padding(horizontal = d.lg)
                        .graphicsLayer { alpha = wordAlpha.value },
                )
                Spacer(Modifier.height(24.dp))
                // Live campus photograph — authentic photography on the very first screen,
                // framed glass-style with a gentle scrim for a premium, editorial finish.
                Box(
                    Modifier
                        .padding(horizontal = 26.dp)
                        .fillMaxWidth()
                        .heightIn(min = 168.dp, max = 208.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.24f), RoundedCornerShape(26.dp))
                        .graphicsLayer {
                            alpha = heroPhotoAlpha.value
                            translationY = heroPhotoY.value * density
                        },
                ) {
                    Image(
                        painter = painterResource(fallbackSchoolPhotos.first()),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, c.tealDeep.copy(alpha = 0.55f)),
                            ),
                        ),
                    )
                    // Floating "live data" credibility chip on the photo.
                    Row(
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White.copy(alpha = 0.92f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(VIcons.ShieldCheck, contentDescription = null, tint = c.tealDeep, modifier = Modifier.size(13.dp))
                        Text(
                            "Verified institutions, live data",
                            style = VTheme.type.label.colored(c.ink).copy(fontWeight = FontWeight.SemiBold),
                        )
                    }
                }
            }
        }

        // ── Lifted content sheet ───────────────────────────────────────────────
        Column(
            Modifier
                .fillMaxWidth()
                .offset(y = (-36).dp)
                .graphicsLayer {
                    translationY = sheetY.value * density
                    alpha = sheetAlpha.value
                }
                .drawBehind {
                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.06f),
                        topLeft = Offset(0f, -10.dp.toPx()),
                        size = Size(size.width, 36.dp.toPx()),
                        cornerRadius = CornerRadius(36.dp.toPx(), 36.dp.toPx()),
                    )
                }
                .background(c.background, RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp))
                .navigationBarsPadding()
                .padding(top = 28.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Grip handle — the sheet reads as a deliberate, pulled-up surface.
            Box(
                Modifier
                    .width(44.dp).height(5.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(c.ink3.copy(alpha = 0.22f)),
            )
            Spacer(Modifier.height(22.dp))

            // ── Headline impact stats (credibility, not fabricated metrics) ──
            ImpactStatsRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .graphicsLayer { alpha = proofAlpha.value },
            )

            Spacer(Modifier.height(28.dp))

            // ── Value proposition: why VidyaSetu ──
            ValuePropSection(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = cardsAlpha.value
                        translationY = cardsY.value * density
                    },
            )

            // ── Role-entry CTAs (Parent OTP / School onboarding) ──
            Spacer(Modifier.height(36.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .graphicsLayer {
                        alpha = cardsAlpha.value
                        translationY = cardsY.value * density
                    },
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                SectionHeader(
                    eyebrow = "Choose your door",
                    title = "Two journeys, one platform",
                )
                EntryPointCard(
                    accent = c.teal,
                    accentInk = c.tealDeep,
                    glyph = VIcons.Users,
                    label = cms?.parentInfo?.topTagline?.takeIf { it.isNotBlank() } ?: "FOR PARENTS",
                    title = cms?.parentInfo?.subTagline?.takeIf { it.isNotBlank() }
                        ?: "Find the right school. Then never miss a beat.",
                    description = "Discover verified institutions, then track attendance, results and teacher messages — all in one secure timeline.",
                    features = cms?.parentInfo?.listOfFeatures?.takeIf { it.isNotEmpty() }
                        ?: listOf(
                            "Verified institution profiles & SRI scores",
                            "Live attendance & academics for your child",
                            "Two-way messaging with teachers",
                        ),
                    buttonText = "Continue as a Parent",
                    buttonTone = VButtonTone.Teal,
                    onButtonClick = onParent,
                )
                EntryPointCard(
                    accent = c.warmOrange,
                    accentInk = c.warmOrange,
                    glyph = VIcons.School,
                    label = cms?.schoolInfo?.topTagline?.takeIf { it.isNotBlank() } ?: "FOR SCHOOLS",
                    title = cms?.schoolInfo?.subTagline?.takeIf { it.isNotBlank() }
                        ?: "Run the whole institution on intelligence.",
                    description = "From admissions to early-warning analytics, give administrators and teachers the operating system modern schools deserve.",
                    features = cms?.schoolInfo?.listOfFeatures?.takeIf { it.isNotEmpty() }
                        ?: listOf(
                            "Early-warning radar (PEWS) for at-risk students",
                            "Admissions CRM & staff management",
                            "Class broadcasts & accountable messaging",
                        ),
                    buttonText = "Onboard Your School",
                    buttonTone = VButtonTone.Peach,
                    onButtonClick = onAdmin,
                )
            }

            // ── The intelligence platform showcase ──
            Spacer(Modifier.height(40.dp))
            IntelligenceShowcase(
                cmsOfferings = cms?.listOfOfferings.orEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = cardsAlpha.value },
            )

            // ── Featured institutions (real-photo rail) ──
            if (schools.isNotEmpty()) {
                Spacer(Modifier.height(40.dp))
                FeaturedInstitutionsSection(
                    schools = schools,
                    onSchoolClick = onParent,
                    modifier = Modifier.graphicsLayer { alpha = cardsAlpha.value },
                )
            }

            // ── Portal access rail ──
            val portals = cms?.listOfPortals.orEmpty()
            if (portals.isNotEmpty()) {
                Spacer(Modifier.height(40.dp))
                PortalAccessSection(
                    portals = portals,
                    onLoginClick = onAdmin,
                    modifier = Modifier.graphicsLayer { alpha = cardsAlpha.value },
                )
            }

            // ── Closing call-to-action band ──
            Spacer(Modifier.height(40.dp))
            ClosingCtaBand(
                onParent = onParent,
                onAdmin = onAdmin,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .graphicsLayer { alpha = cardsAlpha.value },
            )

            // ── Footer ──
            Spacer(Modifier.height(36.dp))
            LandingFooter(
                onLegal = onLegal,
                modifier = Modifier.graphicsLayer { alpha = cardsAlpha.value },
            )

            Spacer(Modifier.height(22.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "By continuing you agree to our ",
                    style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 11.sp),
                    textAlign = TextAlign.Center,
                )
                Text(
                    "Terms",
                    style = VTheme.type.caption.colored(c.tealDeep).copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onLegal(LegalDoc.Terms) },
                )
                Text(" & ", style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 11.sp))
                Text(
                    "Privacy Policy",
                    style = VTheme.type.caption.colored(c.tealDeep).copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onLegal(LegalDoc.Privacy) },
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Impact stats — three credibility tiles. Phrased as platform capability, not
// invented numbers, so the screen stays honest on a public surface.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ImpactStatsRow(modifier: Modifier = Modifier) {
    val c = VTheme.colors
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ImpactStat("3", "roles, one\nunified system", c.tealDeep, Modifier.weight(1f))
        ImpactStat("24/7", "live academic\nvisibility", c.warmOrange, Modifier.weight(1f))
        ImpactStat("0", "fabricated data —\nSupabase-backed", c.navy, Modifier.weight(1f))
    }
}

@Composable
private fun ImpactStat(value: String, label: String, accent: Color, modifier: Modifier = Modifier) {
    val c = VTheme.colors
    VCard(modifier = modifier, padding = 16.dp) {
        Text(value, style = VTheme.type.dataLg.colored(accent).copy(fontSize = 26.sp, fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 11.sp, lineHeight = 15.sp),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Value proposition — three pillars (Trust / Intelligence / Connection), each a
// tinted glyph tile + headline + supporting line. The product's whole pitch in
// one glance.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ValuePropSection(modifier: Modifier = Modifier) {
    val c = VTheme.colors
    Column(modifier.padding(horizontal = 24.dp)) {
        SectionHeader(eyebrow = "Why VidyaSetu", title = "Built on trust. Powered by intelligence.")
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ValuePropCard(
                glyph = VIcons.ShieldCheck,
                accent = c.tealDeep,
                title = "Verified, not vibes",
                body = "Every institution is verified and every number is Supabase-backed — what you see is the real record, never a mock.",
            )
            ValuePropCard(
                glyph = VIcons.TrendingUp,
                accent = c.warmOrange,
                title = "Intelligence that acts early",
                body = "SRI school ratings and the PEWS early-warning radar surface what matters before it becomes a problem.",
            )
            ValuePropCard(
                glyph = VIcons.Chat,
                accent = c.navy,
                title = "Everyone on the same page",
                body = "Accountable two-way messaging and class broadcasts keep parents, teachers and admins genuinely connected.",
            )
        }
    }
}

@Composable
private fun ValuePropCard(glyph: ImageVector, accent: Color, title: String, body: String) {
    val c = VTheme.colors
    VCard(Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(13.dp)).background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(glyph, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = VTheme.type.h3.colored(c.ink))
                Spacer(Modifier.height(4.dp))
                Text(
                    body,
                    style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 12.sp, lineHeight = 17.sp),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Intelligence showcase — the platform's capability grid. Prefers CMS offerings;
// otherwise renders the REAL shipped feature set as live / coming-soon cards.
// ─────────────────────────────────────────────────────────────────────────────

private data class ShowcaseFeature(
    val glyph: ImageVector,
    val heading: String,
    val description: String,
    val isLive: Boolean,
)

@Composable
private fun IntelligenceShowcase(
    cmsOfferings: List<LandingItem>,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    // Honest, shipped capability set (FEATURES_CURRENT_PHASE.md). Coming-soon items are flagged.
    val fallback = listOf(
        ShowcaseFeature(VIcons.Star, "SRI School Ratings", "A composite institutional rating that turns reputation into a number you can compare.", true),
        ShowcaseFeature(VIcons.AlertTriangle, "PEWS Early-Warning Radar", "Surfaces the real at-risk student cohort so schools can intervene early.", true),
        ShowcaseFeature(VIcons.Calendar, "Live Attendance", "Month-grid calendars with present / late / absent coding, synced from the classroom.", true),
        ShowcaseFeature(VIcons.Chat, "Accountable Messaging", "Two-way conversations and class broadcasts — immutable, with delivery notifications.", true),
        ShowcaseFeature(VIcons.ClipboardList, "Admissions CRM", "A pipeline that runs the entire admissions journey for the school office.", true),
        ShowcaseFeature(VIcons.Sparkles, "AI Report Card", "Narrative academic insight generated from real results.", false),
    )

    Column(modifier.padding(horizontal = 24.dp)) {
        SectionHeader(eyebrow = "The platform", title = "One system. Every workflow.")
        Spacer(Modifier.height(4.dp))
        Text(
            "Proprietary intelligence powering the entire ecosystem.",
            style = VTheme.type.body.colored(c.ink2).copy(fontSize = 13.sp),
        )
        Spacer(Modifier.height(16.dp))
        if (cmsOfferings.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                cmsOfferings.forEach { item ->
                    ShowcaseCard(
                        glyph = VIcons.Sparkles,
                        heading = item.heading,
                        description = item.description,
                        isLive = item.isLive,
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                fallback.forEach { f ->
                    ShowcaseCard(f.glyph, f.heading, f.description, f.isLive)
                }
            }
        }
    }
}

@Composable
private fun ShowcaseCard(glyph: ImageVector, heading: String, description: String, isLive: Boolean) {
    val c = VTheme.colors
    VCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(13.dp)).background(c.teal.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(glyph, contentDescription = null, tint = c.tealDeep, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(heading, style = VTheme.type.h3.colored(c.ink), maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                    VBadge(
                        text = if (isLive) "Live" else "Coming soon",
                        tone = if (isLive) VBadgeTone.Success else VBadgeTone.Neutral,
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    description,
                    style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 12.sp, lineHeight = 16.sp),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Featured institutions — real-photo rail.
// ─────────────────────────────────────────────────────────────────────────────

/** Bundled professional campus photography — graceful fallback for schools lacking a CMS image. */
private val fallbackSchoolPhotos: List<DrawableResource> = listOf(
    Res.drawable.landing_school_1,
    Res.drawable.landing_school_2,
    Res.drawable.landing_school_3,
)

@Composable
private fun FeaturedInstitutionsSection(
    schools: List<School>,
    onSchoolClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            .width(252.dp)
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        padding = 0.dp,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(142.dp)
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
                Image(
                    painter = painterResource(fallback),
                    contentDescription = school.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.40f))),
                ),
            )
            if (school.isVerified) {
                Row(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.92f))
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
                        .background(c.teal.copy(alpha = 0.14f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(VIcons.Star, contentDescription = null, tint = c.tealDeep, modifier = Modifier.size(12.dp))
                    Text(formatScore(school.sriScore), style = VTheme.type.dataSm.colored(c.tealDeep))
                    Text("SRI", style = VTheme.type.label.colored(c.ink3))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Portal access rail.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PortalAccessSection(
    portals: List<LandingItem>,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    Column(modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(0.78f)) {
                SectionHeader(eyebrow = "Already with us", title = "Access your portal")
                Spacer(Modifier.height(2.dp))
                Text(
                    "Sign in to your existing VidyaSetu workspace.",
                    style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 12.sp),
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            portals.forEach { portal ->
                val interaction = remember { MutableInteractionSource() }
                val portalIcon = if (portal.heading.contains("Parent", ignoreCase = true)) VIcons.Users else VIcons.GraduationCap
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
                            Icon(portalIcon, contentDescription = null, tint = c.warmOrange, modifier = Modifier.size(20.dp))
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

// ─────────────────────────────────────────────────────────────────────────────
// Closing CTA band — a deep teal call-to-action that re-offers both doors.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ClosingCtaBand(
    onParent: () -> Unit,
    onAdmin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(listOf(VTheme.colors.tealDeep, VTheme.colors.teal)),
            )
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.14f), Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.15f),
                        radius = size.maxDimension * 0.5f,
                    ),
                    radius = size.maxDimension * 0.5f,
                    center = Offset(size.width * 0.85f, size.height * 0.15f),
                )
            }
            .padding(28.dp),
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                "Ready to move with confidence?",
                style = VTheme.type.h2.colored(Color.White).copy(fontWeight = FontWeight.ExtraBold),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Join the schools and families already running on VidyaSetu.",
                style = VTheme.type.body.colored(Color.White.copy(alpha = 0.92f)).copy(fontSize = 14.sp, lineHeight = 20.sp),
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VButton(
                    text = "I'm a Parent",
                    onClick = onParent,
                    variant = VButtonVariant.Primary,
                    tone = VButtonTone.Navy,
                    size = VButtonSize.Md,
                    soft = false,
                    modifier = Modifier.weight(1f),
                )
                VButton(
                    text = "I'm a School",
                    onClick = onAdmin,
                    variant = VButtonVariant.Secondary,
                    size = VButtonSize.Md,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Footer.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LandingFooter(
    onLegal: (LegalDoc) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .background(c.cream.copy(alpha = 0.5f))
            .padding(horizontal = 24.dp, vertical = 28.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(VIcons.School, contentDescription = null, tint = c.tealDeep, modifier = Modifier.size(26.dp))
            Text("VidyaSetu", style = VTheme.type.h3.colored(c.navy).copy(fontWeight = FontWeight.Bold))
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "The intelligence layer for modern education — uniting parents, teachers and administrators on one trusted, data-backed platform.",
            style = VTheme.type.caption.colored(c.ink2).copy(fontSize = 12.sp, lineHeight = 18.sp),
        )
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(40.dp)) {
            FooterLinkGroup(
                title = "PORTALS",
                links = listOf<Pair<String, (() -> Unit)?>>(
                    "Parent Portal" to null,
                    "Admin Dashboard" to null,
                    "Teacher Console" to null,
                ),
            )
            FooterLinkGroup(
                title = "SUPPORT",
                links = listOf<Pair<String, (() -> Unit)?>>(
                    "Privacy Policy" to { onLegal(LegalDoc.Privacy) },
                    "Terms of Service" to { onLegal(LegalDoc.Terms) },
                    "Help Desk" to { onLegal(LegalDoc.Help) },
                ),
            )
        }
    }
}

@Composable
private fun FooterLinkGroup(title: String, links: List<Pair<String, (() -> Unit)?>>) {
    val c = VTheme.colors
    Column {
        Text(
            title,
            style = VTheme.type.label.colored(c.tealDeep).copy(fontWeight = FontWeight.Bold, letterSpacing = 0.10.em),
        )
        Spacer(Modifier.height(12.dp))
        links.forEach { (link, onClick) ->
            val interaction = remember { MutableInteractionSource() }
            val base = if (onClick != null) {
                Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
            } else {
                Modifier
            }
            Text(
                link,
                style = VTheme.type.caption.colored(if (onClick != null) c.ink else c.ink2).copy(fontSize = 12.sp),
                modifier = base,
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared building blocks.
// ─────────────────────────────────────────────────────────────────────────────

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

@Composable
private fun EntryPointCard(
    accent: Color,
    accentInk: Color,
    glyph: ImageVector,
    label: String,
    title: String,
    description: String,
    features: List<String>,
    buttonText: String,
    buttonTone: VButtonTone,
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    VCard(modifier = modifier.fillMaxWidth(), padding = 0.dp) {
        Column(Modifier.fillMaxWidth()) {
            // Role-tinted gradient header strip with the role glyph.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .background(
                        Brush.horizontalGradient(listOf(accent.copy(alpha = 0.95f), accent.copy(alpha = 0.40f))),
                    ),
            )
            Column(Modifier.fillMaxWidth().padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(accent.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(glyph, contentDescription = null, tint = accentInk, modifier = Modifier.size(20.dp))
                    }
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(accent.copy(alpha = 0.12f))
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                    ) {
                        Text(
                            label.uppercase(),
                            style = VTheme.type.label.colored(accentInk).copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.06.em),
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(title, style = VTheme.type.h2.colored(c.navy))
                Spacer(Modifier.height(10.dp))
                Text(
                    description,
                    style = VTheme.type.body.colored(c.ink2).copy(fontSize = 14.sp, lineHeight = 20.sp),
                )
                Spacer(Modifier.height(18.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    features.forEach { feature ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                Modifier.size(20.dp).clip(RoundedCornerShape(999.dp)).background(accent.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(VIcons.Check, contentDescription = null, tint = accentInk, modifier = Modifier.size(13.dp))
                            }
                            Text(feature, style = VTheme.type.bodyStrong.colored(c.ink), modifier = Modifier.weight(1f))
                        }
                    }
                }
                Spacer(Modifier.height(22.dp))
                VButton(
                    text = buttonText,
                    onClick = onButtonClick,
                    variant = VButtonVariant.Primary,
                    tone = buttonTone,
                    size = VButtonSize.Lg,
                    soft = false,
                    full = true,
                    trailing = {
                        Icon(VIcons.ArrowRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    },
                )
            }
        }
    }
}

/** Formats an SRI score to one decimal without a platform number formatter. */
private fun formatScore(value: Double): String {
    val rounded = (value * 10).toLong()
    return "${rounded / 10}.${rounded % 10}"
}
