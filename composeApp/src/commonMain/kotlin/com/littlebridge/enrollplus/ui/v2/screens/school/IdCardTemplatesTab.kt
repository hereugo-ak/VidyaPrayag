package com.littlebridge.enrollplus.ui.v2.screens.school

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.littlebridge.enrollplus.feature.idcard.domain.model.IdCardTemplateDto
import com.littlebridge.enrollplus.feature.idcard.presentation.IdCardState
import com.littlebridge.enrollplus.feature.idcard.presentation.IdCardViewModel
import com.littlebridge.enrollplus.ui.v2.components.ShimmerBox
import com.littlebridge.enrollplus.ui.v2.components.VBadge
import com.littlebridge.enrollplus.ui.v2.components.VBadgeTone
import com.littlebridge.enrollplus.ui.v2.components.VButton
import com.littlebridge.enrollplus.ui.v2.components.VButtonSize
import com.littlebridge.enrollplus.ui.v2.components.VButtonVariant
import com.littlebridge.enrollplus.ui.v2.components.VCard
import com.littlebridge.enrollplus.ui.v2.components.VEmptyState
import com.littlebridge.enrollplus.ui.v2.components.VProgressRing
import com.littlebridge.enrollplus.ui.v2.components.VTag
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored

// ════════════════════════════════════════════════════════════════════════════
// STATS BANNER — Progress ring + breakdown + achievement badge
// ════════════════════════════════════════════════════════════════════════════

@Composable
internal fun IdCardStatsBanner(
    totalCards: Int,
    studentCards: Int,
    teacherCards: Int,
    staffCards: Int,
    onBadgeClick: () -> Unit = {},
) {
    val c = VTheme.colors
    val milestone = remember(totalCards) {
        when {
            totalCards >= 500 -> Triple("ID Card Master", Icons.Filled.Star, VBadgeTone.Accent)
            totalCards >= 100 -> Triple("Century Club", Icons.Filled.Star, VBadgeTone.Success)
            totalCards >= 50 -> Triple("Half Century", Icons.Filled.Check, VBadgeTone.Warning)
            totalCards >= 1 -> Triple("First Steps", Icons.Filled.Check, VBadgeTone.Arctic)
            else -> Triple("Getting Started", Icons.Filled.Add, VBadgeTone.Neutral)
        }
    }

    VCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                VProgressRing(
                    value = if (totalCards > 0) 100f else 0f,
                    size = 56.dp,
                    strokeWidth = 6.dp,
                    tone = VBadgeTone.Accent,
                    label = totalCards.toString(),
                )
                Text(
                    text = "Total Cards",
                    style = VTheme.type.caption.colored(c.ink3),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                StatRow(label = "Students", count = studentCards, color = c.accent)
                StatRow(label = "Teachers", count = teacherCards, color = c.tealDeep)
                StatRow(label = "Staff", count = staffCards, color = c.warmOrange)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(c.accent.copy(alpha = 0.12f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onBadgeClick,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        milestone.second,
                        contentDescription = milestone.first,
                        tint = c.accentDeep,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Text(
                    text = milestone.first,
                    style = VTheme.type.caption.colored(c.ink2).copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(72.dp),
                )
            }
        }
    }
}

@Composable
private fun StatRow(label: String, count: Int, color: Color) {
    val c = VTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(
            text = "$count $label",
            style = VTheme.type.caption.colored(c.ink2),
        )
    }
}

// ════════════════════════════════════════════════════════════════════════════
// TEMPLATES TAB — Visual template builder
// ════════════════════════════════════════════════════════════════════════════

private val AVAILABLE_FIELDS = listOf("name", "role", "class", "school", "photo", "qrOnFront", "emergencyContact", "bloodGroup")
private val FIELD_LABELS = mapOf(
    "name" to "Name",
    "role" to "Role",
    "class" to "Class",
    "school" to "School",
    "photo" to "Photo",
    "qrOnFront" to "QR on Front",
    "emergencyContact" to "Emergency",
    "bloodGroup" to "Blood Group",
)

private val PRESET_COLORS = listOf(
    0xFF6C5CE0.toInt() to "Lavender",
    0xFF006A60.toInt() to "Teal",
    0xFF1A2422.toInt() to "Ink",
    0xFFB3261E.toInt() to "Red",
    0xFF1F7A4D.toInt() to "Green",
    0xFFB3651A.toInt() to "Amber",
)

@Composable
internal fun TemplatesTab(
    state: IdCardState,
    viewModel: IdCardViewModel,
    scrollState: ScrollState = rememberScrollState(),
) {
    val c = VTheme.colors
    var templateName by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("student") }
    var selectedFields by remember { mutableStateOf(setOf("name", "role", "class", "school")) }
    var accentColorArgb by remember { mutableStateOf(0xFF6C5CE0.toInt()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
    ) {
        state.error?.let { err ->
            VCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                Text(err, color = c.dangerInk, style = VTheme.type.body)
            }
        }
        state.infoMessage?.let { msg ->
            VCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                Text(msg, color = c.successInk, style = VTheme.type.body)
            }
        }

        if (state.templates.isEmpty() && state.isLoading) {
            repeat(2) {
                ShimmerBox(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    height = 64.dp,
                )
            }
        } else if (state.templates.isEmpty()) {
            VEmptyState(
                title = "No templates yet",
                body = "Create your first ID card template with the visual builder below.",
                icon = Icons.Filled.School,
                modifier = Modifier.padding(top = 24.dp),
            )
        } else {
            state.templates.forEach { template ->
                TemplateCard(
                    template = template,
                    onDeactivate = { viewModel.deactivateTemplate(template.id) },
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "CREATE NEW TEMPLATE",
            style = VTheme.type.label.colored(c.ink3),
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = templateName,
                onValueChange = { templateName = it },
                label = { Text("Template Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(12.dp))

            Text("Card Type", style = VTheme.type.bodyStrong.colored(c.ink))
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("student" to "Student", "teacher" to "Teacher", "staff" to "Staff").forEach { (role, label) ->
                    VTag(
                        text = label,
                        active = selectedRole == role,
                        onClick = { selectedRole = role },
                        accentActive = true,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text("Fields to Display", style = VTheme.type.bodyStrong.colored(c.ink))
            Spacer(modifier = Modifier.height(6.dp))
            AVAILABLE_FIELDS.chunked(3).forEach { rowFields ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowFields.forEach { field ->
                        VTag(
                            text = FIELD_LABELS[field] ?: field,
                            active = field in selectedFields,
                            onClick = {
                                selectedFields = if (field in selectedFields) selectedFields - field else selectedFields + field
                            },
                            accentActive = true,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text("Accent Color", style = VTheme.type.bodyStrong.colored(c.ink))
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PRESET_COLORS.forEach { (argb, name) ->
                    ColorSwatch(
                        argb = argb,
                        label = name,
                        selected = accentColorArgb == argb,
                        onClick = { accentColorArgb = argb },
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            Text("Live Preview", style = VTheme.type.bodyStrong.colored(c.ink))
            Spacer(modifier = Modifier.height(8.dp))
            LiveCardPreview(
                templateName = templateName.ifBlank { "Preview" },
                roleType = selectedRole,
                fields = selectedFields,
                accentArgb = accentColorArgb,
            )
            Spacer(modifier = Modifier.height(16.dp))

            VButton(
                text = if (state.isLoading) "Creating..." else "Create Template",
                onClick = {
                    if (templateName.isNotBlank()) {
                        viewModel.clearMessages()
                        val config = buildConfigJson(selectedFields, accentColorArgb)
                        viewModel.createTemplate(templateName, selectedRole, config, config)
                        templateName = ""
                    }
                },
                variant = VButtonVariant.Primary,
                enabled = !state.isLoading && templateName.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ColorSwatch(
    argb: Int,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val c = VTheme.colors
    val color = Color(argb)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        ),
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color)
                .then(
                    if (selected) Modifier.border(3.dp, c.ink, CircleShape)
                    else Modifier.border(1.dp, c.hairline, CircleShape)
                ),
        )
        Text(
            text = label,
            style = VTheme.type.caption.colored(if (selected) c.ink else c.ink3),
            fontSize = 9.sp,
        )
    }
}

@Composable
private fun LiveCardPreview(
    templateName: String,
    roleType: String,
    fields: Set<String>,
    accentArgb: Int,
) {
    val accent = Color(accentArgb)
    val c = VTheme.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .aspectRatio(54f / 86f)
            .clip(RoundedCornerShape(16.dp))
            .background(c.card)
            .border(2.dp, accent, RoundedCornerShape(16.dp)),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Header band
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(accent),
                contentAlignment = Alignment.CenterStart,
            ) {
                if ("school" in fields) {
                    Text(
                        text = "Vidya Prayag School",
                        style = VTheme.type.caption.colored(Color.White).copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                } else {
                    Text(
                        text = "ID CARD",
                        style = VTheme.type.caption.colored(Color.White),
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            }

            // Body
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Left: photo + QR
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if ("photo" in fields) {
                        Box(
                            Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(accent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = accent, modifier = Modifier.size(32.dp))
                        }
                    }
                    if ("qrOnFront" in fields) {
                        QrGridPreview(
                            size = 52.dp,
                            color = c.ink,
                        )
                    }
                }

                // Right: text fields
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if ("name" in fields) {
                        Text(
                            text = templateName,
                            style = VTheme.type.bodyStrong.colored(c.ink),
                            maxLines = 2,
                        )
                    }
                    if ("role" in fields) {
                        Text(
                            text = roleType.replaceFirstChar { it.uppercase() },
                            style = VTheme.type.caption.colored(accent).copy(fontWeight = FontWeight.Bold),
                        )
                    }
                    if ("class" in fields) {
                        Text("Class 10-A", style = VTheme.type.caption.colored(c.ink2))
                    }
                    if ("bloodGroup" in fields) {
                        Text("Blood: B+", style = VTheme.type.caption.colored(c.ink2))
                    }
                    if ("emergencyContact" in fields) {
                        Text("Emergency: +91 98765 43210", style = VTheme.type.caption.colored(c.ink2), maxLines = 2)
                    }
                }
            }

            // Footer band
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(accent),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = "Scan QR to verify",
                    style = VTheme.type.caption.colored(Color.White).copy(fontSize = 8.sp),
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun QrGridPreview(
    size: androidx.compose.ui.unit.Dp,
    color: Color,
) {
    val cellSize = size / 7
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White)
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(4.dp)),
    ) {
        val pattern = listOf(
            listOf(1,1,1,0,1,0,1),
            listOf(1,0,1,1,0,1,1),
            listOf(1,1,1,0,1,1,0),
            listOf(0,1,0,1,0,0,1),
            listOf(1,0,1,1,1,1,0),
            listOf(1,1,0,0,1,0,1),
            listOf(0,1,1,1,0,1,1),
        )
        pattern.forEachIndexed { row, cols ->
            cols.forEachIndexed { col, cell ->
                if (cell == 1) {
                    Box(
                        Modifier
                            .offset(x = cellSize * col, y = cellSize * row)
                            .size(cellSize)
                            .background(color),
                    )
                }
            }
        }
        // Finder squares (3 corner markers with white center)
        val corners = listOf(0 to 0, 4 to 0, 0 to 4)
        corners.forEach { (col, row) ->
            Box(
                Modifier
                    .offset(x = cellSize * col, y = cellSize * row)
                    .size(cellSize * 3)
                    .background(color),
            )
            Box(
                Modifier
                    .offset(x = cellSize * (col + 1), y = cellSize * (row + 1))
                    .size(cellSize)
                    .background(Color.White),
            )
        }
    }
}

private fun buildConfigJson(fields: Set<String>, accentArgb: Int): String {
    val fieldsArray = fields.joinToString(",") { "\"$it\"" }
    val hex = String.format("#%06X", accentArgb and 0xFFFFFF)
    val showQr = "qrOnFront" in fields
    return """{"fields":[$fieldsArray],"backgroundColor":"#FFFFFF","textColor":"#1A2422","accentColor":"$hex","showLogo":true,"showQrOnFront":$showQr}"""
}

@Composable
private fun TemplateCard(
    template: IdCardTemplateDto,
    onDeactivate: () -> Unit,
) {
    val c = VTheme.colors
    VCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(template.name, style = VTheme.type.bodyStrong.colored(c.ink))
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    VBadge(
                        text = template.roleType.replaceFirstChar { it.uppercase() },
                        tone = VBadgeTone.Accent,
                    )
                    if (template.isActive) {
                        VBadge(text = "Active", tone = VBadgeTone.Success, leadingIcon = Icons.Filled.Check)
                    } else {
                        VBadge(text = "Inactive", tone = VBadgeTone.Neutral)
                    }
                }
            }
            if (template.isActive) {
                VButton(
                    text = "Deactivate",
                    onClick = onDeactivate,
                    variant = VButtonVariant.Secondary,
                    size = VButtonSize.Sm,
                )
            }
        }
    }
}
