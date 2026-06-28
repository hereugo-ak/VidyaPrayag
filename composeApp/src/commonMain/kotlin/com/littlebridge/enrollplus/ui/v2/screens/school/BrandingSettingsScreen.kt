package com.littlebridge.enrollplus.ui.v2.screens.school

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.enrollplus.feature.branding.domain.model.SchoolBranding
import com.littlebridge.enrollplus.feature.branding.domain.model.UpdateBrandingRequest
import com.littlebridge.enrollplus.feature.branding.presentation.BrandingThemeManager
import com.littlebridge.enrollplus.feature.branding.presentation.BrandingViewModel
import com.littlebridge.enrollplus.ui.v2.components.*
import com.littlebridge.enrollplus.ui.v2.screens.VStateHost
import com.littlebridge.enrollplus.ui.v2.screens.collectAsStateV2
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private val PRESET_COLORS = listOf(
    "#2563EB",
    "#7C3AED",
    "#059669",
    "#DC2626",
    "#EA580C",
    "#D97706",
    "#0891B2",
    "#4F46E5",
)

private val SUBDOMAIN_REGEX = Regex("^[a-z0-9][a-z0-9-]{2,30}[a-z0-9]$")

private fun parseHexColor(hex: String): Color {
    return try {
        val value = hex.removePrefix("#")
        if (value.length == 3) {
            val r = value[0].toString().repeat(2).toInt(16)
            val g = value[1].toString().repeat(2).toInt(16)
            val b = value[2].toString().repeat(2).toInt(16)
            Color(r, g, b)
        } else {
            val rgb = value.toLong(16)
            Color((rgb shr 16 and 0xFF).toInt(), (rgb shr 8 and 0xFF).toInt(), (rgb and 0xFF).toInt())
        }
    } catch (_: Exception) {
        Color(0xFF2563EB)
    }
}

@Composable
fun BrandingSettingsScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: BrandingViewModel = koinViewModel(),
    brandingThemeManager: BrandingThemeManager = koinInject(),
) {
    val state by viewModel.state.collectAsStateV2()

    LaunchedEffect(Unit) {
        viewModel.loadBranding()
    }

    // When branding is updated or reset, refresh the global theme manager
    // so the app's accent colors update live. Skips initial load (handled by
    // NavGraphV2's LaunchedEffect(isAuthenticated)).
    var hasInitialLoad = remember { mutableStateOf(false) }
    LaunchedEffect(state.branding) {
        if (state.branding != null) {
            if (hasInitialLoad.value) {
                brandingThemeManager.loadBranding()
            } else {
                hasInitialLoad.value = true
            }
        }
    }

    LaunchedEffect(state.infoMessage) {
        if (state.infoMessage != null) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearInfoMessage()
        }
    }

    LaunchedEffect(state.error) {
        if (state.error != null) {
            kotlinx.coroutines.delay(5000)
            viewModel.clearError()
        }
    }

    BrandingSettingsContent(
        state = state,
        onBack = onBack,
        onUpdateBranding = { req -> viewModel.updateBranding(req) },
        onResetBranding = { viewModel.resetBranding() },
        onCheckSubdomain = { sub -> viewModel.checkSubdomain(sub) },
        onAssignSubdomain = { sub -> viewModel.updateSubdomain(sub) },
        onRemoveSubdomain = { viewModel.removeSubdomain() },
        onSubdomainInputChanged = { viewModel.clearSubdomainCheck() },
        onRetry = { viewModel.loadBranding() },
        modifier = modifier,
    )
}

private fun isSubdomainValid(input: String): Boolean {
    return input.length >= 4 && input.length <= 32 && SUBDOMAIN_REGEX.matches(input)
}

@Composable
private fun BrandingSettingsContent(
    state: com.littlebridge.enrollplus.feature.branding.presentation.BrandingState,
    onBack: () -> Unit,
    onUpdateBranding: (UpdateBrandingRequest) -> Unit,
    onResetBranding: () -> Unit,
    onCheckSubdomain: (String) -> Unit,
    onAssignSubdomain: (String) -> Unit,
    onRemoveSubdomain: () -> Unit,
    onSubdomainInputChanged: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    val branding = state.branding

    var primaryColor by remember(branding?.primaryColor) {
        mutableStateOf(branding?.primaryColor ?: "#2563EB")
    }
    var secondaryColor by remember(branding?.secondaryColor) {
        mutableStateOf(branding?.secondaryColor ?: "#1E40AF")
    }
    var accentColor by remember(branding?.accentColor) {
        mutableStateOf(branding?.accentColor ?: "#3B82F6")
    }
    var subdomainInput by remember(branding?.customSubdomain) {
        mutableStateOf(branding?.customSubdomain ?: "")
    }
    var showResetConfirm by remember { mutableStateOf(false) }

    VConfirmDialog(
        visible = showResetConfirm,
        title = "Reset branding?",
        message = "All colors will be reset to defaults. Your uploaded assets will be kept.",
        confirmLabel = "Reset",
        onConfirm = {
            showResetConfirm = false
            onResetBranding()
        },
        onDismiss = { showResetConfirm = false },
        icon = VIcons.AlertTriangle,
    )

    Column(
        modifier
            .fillMaxSize()
            .background(c.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ── Header ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.ink.copy(alpha = 0.06f))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(VIcons.ArrowLeft, contentDescription = "Back", tint = c.ink, modifier = Modifier.size(20.dp))
            }
            Text("Branding Kit", style = VTheme.type.h2.colored(c.ink))
        }

        VStateHost(
            loading = state.isLoading,
            error = state.error,
            isEmpty = branding == null && !state.isLoading,
            onRetry = onRetry,
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // ── Info / Error banner ────────────────────────────────────
                state.infoMessage?.let { msg ->
                    VCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(VIcons.Check, contentDescription = null, tint = c.successInk, modifier = Modifier.size(18.dp))
                            Text(msg, style = VTheme.type.body.colored(c.ink))
                        }
                    }
                }
                state.error?.let { err ->
                    VCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(VIcons.AlertTriangle, contentDescription = null, tint = c.dangerInk, modifier = Modifier.size(18.dp))
                            Text(err, style = VTheme.type.body.colored(c.dangerInk))
                        }
                    }
                }

                // ── Branding status badge ──────────────────────────────────
                branding?.let { b ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            b.schoolName,
                            style = VTheme.type.h3.colored(c.ink),
                        )
                        VBadge(
                            text = if (b.isCustomized) "Customized" else "Default",
                            tone = if (b.isCustomized) VBadgeTone.Success else VBadgeTone.Neutral,
                        )
                    }
                }

                // ── Live Preview Card ──────────────────────────────────────
                BrandingPreviewCard(
                    branding = branding,
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    accentColor = accentColor,
                )

                // ── Color Pickers ──────────────────────────────────────────
                Text("Brand Colors", style = VTheme.type.h3.colored(c.ink))
                ColorPickerSection(
                    label = "Primary Color",
                    currentColor = primaryColor,
                    onColorSelected = { primaryColor = it },
                )
                ColorPickerSection(
                    label = "Secondary Color",
                    currentColor = secondaryColor,
                    onColorSelected = { secondaryColor = it },
                )
                ColorPickerSection(
                    label = "Accent Color",
                    currentColor = accentColor,
                    onColorSelected = { accentColor = it },
                )

                // ── Save Colors Button ─────────────────────────────────────
                VButton(
                    text = "Save Colors",
                    onClick = {
                        onUpdateBranding(
                            UpdateBrandingRequest(
                                primaryColor = primaryColor,
                                secondaryColor = secondaryColor,
                                accentColor = accentColor,
                                isCustomized = true,
                            )
                        )
                    },
                    full = true,
                    loading = state.isLoading,
                )

                // ── Subdomain Section ──────────────────────────────────────
                Spacer(Modifier.height(8.dp))
                Text("Custom Subdomain", style = VTheme.type.h3.colored(c.ink))
                Text(
                    "Set a custom web address for your school's portal, e.g. dpsrkpuram.vidyaprayag.com",
                    style = VTheme.type.caption.colored(c.ink3),
                )

                branding?.customSubdomain?.let { existing ->
                    VCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text("Current subdomain", style = VTheme.type.caption.colored(c.ink3))
                                Text(
                                    "$existing.vidyaprayag.com",
                                    style = VTheme.type.bodyStrong.colored(c.ink),
                                )
                            }
                            VButton(
                                text = "Remove",
                                onClick = { onRemoveSubdomain() },
                                variant = VButtonVariant.Ghost,
                                tone = VButtonTone.Rose,
                                size = VButtonSize.Sm,
                            )
                        }
                    }
                }

                if (branding?.customSubdomain == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        VInput(
                            value = subdomainInput,
                            onValueChange = {
                                subdomainInput = it.lowercase().filter { ch -> ch.isLetterOrDigit() || ch == '-' }
                                onSubdomainInputChanged()
                            },
                            modifier = Modifier.weight(1f),
                            label = "Subdomain",
                            placeholder = "e.g. dpsrkpuram",
                            hint = "4-32 chars, lowercase letters, numbers & hyphens",
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        VButton(
                            text = "Check",
                            onClick = { onCheckSubdomain(subdomainInput) },
                            variant = VButtonVariant.Ghost,
                            size = VButtonSize.Sm,
                            enabled = isSubdomainValid(subdomainInput),
                            loading = state.isLoading,
                        )
                        VButton(
                            text = "Assign",
                            onClick = { onAssignSubdomain(subdomainInput) },
                            variant = VButtonVariant.Primary,
                            size = VButtonSize.Sm,
                            enabled = isSubdomainValid(subdomainInput),
                            loading = state.isLoading,
                        )
                    }

                    state.subdomainAvailable?.let { available ->
                        VCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    if (available) VIcons.Check else VIcons.Close,
                                    contentDescription = null,
                                    tint = if (available) c.successInk else c.dangerInk,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    if (available) "Subdomain is available!" else "Subdomain is already taken.",
                                    style = VTheme.type.body.colored(if (available) c.successInk else c.dangerInk),
                                )
                            }
                        }
                    }
                }

                // ── Reset to defaults ──────────────────────────────────────
                Spacer(Modifier.height(16.dp))
                VButton(
                    text = "Reset to Defaults",
                    onClick = { showResetConfirm = true },
                    variant = VButtonVariant.Ghost,
                    tone = VButtonTone.Rose,
                    full = true,
                )
            }
        }
    }
}

@Composable
private fun BrandingPreviewCard(
    branding: SchoolBranding?,
    primaryColor: String,
    secondaryColor: String,
    accentColor: String,
) {
    val c = VTheme.colors
    val primary = parseHexColor(primaryColor)
    val secondary = parseHexColor(secondaryColor)
    val accent = parseHexColor(accentColor)

    VCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Live Preview", style = VTheme.type.bodyStrong.colored(c.ink))

            // Mock login header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    branding?.schoolName ?: "Your School",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Mock button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Primary Button", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(secondary.copy(alpha = 0.15f))
                        .border(1.dp, secondary, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Secondary", color = secondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }

            // Color swatches
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ColorSwatch("Primary", primary)
                ColorSwatch("Secondary", secondary)
                ColorSwatch("Accent", accent)
            }
        }
    }
}

@Composable
private fun ColorSwatch(label: String, color: Color) {
    Column(
        modifier = Modifier.fillMaxWidth(0.33f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color)
                .border(2.dp, Color.White, CircleShape),
        )
        Text(label, style = VTheme.type.caption.colored(VTheme.colors.ink3), fontSize = 10.sp)
    }
}

@Composable
private fun ColorPickerSection(
    label: String,
    currentColor: String,
    onColorSelected: (String) -> Unit,
) {
    val c = VTheme.colors
    var hexInput by remember(currentColor) { mutableStateOf(currentColor) }
    val parsedColor = parseHexColor(hexInput)

    VCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, style = VTheme.type.bodyStrong.colored(c.ink))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(parsedColor)
                        .border(2.dp, c.border2, CircleShape),
                )
            }

            // Preset swatches
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PRESET_COLORS.forEach { hex ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(parseHexColor(hex))
                            .border(
                                width = if (hex.equals(hexInput, ignoreCase = true)) 3.dp else 0.dp,
                                color = c.ink,
                                shape = CircleShape,
                            )
                            .clickable {
                                hexInput = hex
                                onColorSelected(hex)
                            },
                    )
                }
            }

            // Hex input
            VInput(
                value = hexInput,
                onValueChange = { value ->
                    val withoutHash = value.filter { it.isLetterOrDigit() }
                    hexInput = "#$withoutHash"
                    if (hexInput.length == 7 && hexInput.matches(Regex("^#[0-9A-Fa-f]{6}$"))) {
                        onColorSelected(hexInput)
                    }
                },
                label = "Hex color",
                placeholder = "#2563EB",
                singleLine = true,
            )
        }
    }
}
