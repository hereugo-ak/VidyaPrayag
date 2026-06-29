package com.littlebridge.enrollplus.ui.v2.screens.school

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.littlebridge.enrollplus.feature.idcard.domain.model.IdCardDto
import com.littlebridge.enrollplus.feature.idcard.presentation.IdCardState
import com.littlebridge.enrollplus.feature.idcard.presentation.IdCardViewModel
import com.littlebridge.enrollplus.ui.v2.components.ShimmerBox
import com.littlebridge.enrollplus.ui.v2.components.VBadge
import com.littlebridge.enrollplus.ui.v2.components.VBadgeTone
import com.littlebridge.enrollplus.ui.v2.components.VButton
import com.littlebridge.enrollplus.ui.v2.components.VButtonSize
import com.littlebridge.enrollplus.ui.v2.components.VButtonVariant
import com.littlebridge.enrollplus.ui.v2.components.VCard
import com.littlebridge.enrollplus.ui.v2.components.VConfirmDialog
import com.littlebridge.enrollplus.ui.v2.components.VEmptyState
import com.littlebridge.enrollplus.ui.v2.components.VTag
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored
import com.littlebridge.enrollplus.util.AppConfig

@Composable
internal fun CardsTab(
    state: IdCardState,
    viewModel: IdCardViewModel,
) {
    val c = VTheme.colors
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf<String?>(null) }
    var cardToDelete by remember { mutableStateOf<IdCardDto?>(null) }
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    val filteredCards = state.cards.filter { card ->
        (filterType == null || card.personType == filterType) &&
        (searchQuery.isBlank() || card.personName.contains(searchQuery, ignoreCase = true))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search by name...") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            singleLine = true,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp),
        ) {
            listOf(null to "All", "student" to "Students", "teacher" to "Teachers", "staff" to "Staff").forEach { (type, label) ->
                VTag(
                    text = label,
                    active = filterType == type,
                    onClick = { filterType = type },
                    accentActive = true,
                )
            }
        }

        Text(
            text = "${filteredCards.size} of ${state.cards.size} cards",
            style = VTheme.type.caption.colored(c.ink3),
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (state.cards.isEmpty() && state.isLoading) {
            repeat(4) {
                ShimmerBox(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    height = 120.dp,
                    shape = RoundedCornerShape(12.dp),
                )
            }
        } else if (filteredCards.isEmpty()) {
            VEmptyState(
                title = if (searchQuery.isNotBlank()) "No cards match \"$searchQuery\"" else "No cards generated yet",
                body = if (searchQuery.isNotBlank()) "Try a different search term" else "Go to the Generate tab to create ID cards.",
                icon = Icons.Filled.School,
                modifier = Modifier.padding(top = 48.dp),
            )
        } else {
            filteredCards.chunked(2).forEach { rowCards ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowCards.forEach { card ->
                        CardGridItem(
                            card = card,
                            onDownloadPdf = { viewModel.loadPdfUrl(card.id) },
                            onDelete = { cardToDelete = card },
                            onVerify = { uriHandler.openUri(card.qrCodeData) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowCards.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }

    cardToDelete?.let { card ->
        VConfirmDialog(
            visible = true,
            title = "Delete ID Card?",
            message = "Are you sure you want to delete the ID card for ${card.personName}? This action cannot be undone.",
            confirmLabel = "Delete",
            onConfirm = {
                viewModel.deleteCard(card.id)
                cardToDelete = null
            },
            onDismiss = { cardToDelete = null },
            icon = Icons.Filled.Close,
        )
    }
}

@Composable
private fun CardGridItem(
    card: IdCardDto,
    onDownloadPdf: () -> Unit,
    onDelete: () -> Unit,
    onVerify: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VTheme.colors
    val status = remember(card.validTill) { cardStatus(card.validTill) }

    VCard(
        modifier = modifier.padding(vertical = 4.dp),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(54f / 86f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(c.cream),
                contentAlignment = Alignment.Center,
            ) {
                card.digitalCardUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = card.personName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } ?: run {
                    // Fallback: card-like layout with real QR image from server
                    val base = AppConfig.schoolBaseUrl.trimEnd('/')
                    val qrImgUrl = "$base/api/v1/id-card/${card.id}/qr.png"
                    Column(
                        modifier = Modifier.fillMaxSize().padding(6.dp),
                    ) {
                        // Mini header band
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(20.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(c.accent),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Text(
                                text = "ID CARD",
                                style = VTheme.type.caption.colored(Color.White).copy(fontSize = 7.sp),
                                modifier = Modifier.padding(horizontal = 4.dp),
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        // Body: photo + info
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Box(
                                    Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(c.accent.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Filled.Person, contentDescription = null, tint = c.accent, modifier = Modifier.size(20.dp))
                                }
                                // Real QR code from server endpoint
                                AsyncImage(
                                    model = qrImgUrl,
                                    contentDescription = "QR Code",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color.White),
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = card.personName,
                                    style = VTheme.type.caption.colored(c.ink).copy(fontWeight = FontWeight.Bold),
                                    maxLines = 2,
                                )
                                Text(
                                    text = card.personType.replaceFirstChar { it.uppercase() },
                                    style = VTheme.type.caption.colored(c.accent).copy(fontSize = 8.sp),
                                )
                                Text(
                                    text = "#${card.personId.takeLast(8)}",
                                    style = VTheme.type.caption.colored(c.ink3).copy(fontSize = 7.sp),
                                    maxLines = 1,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        // Mini footer
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(c.accent),
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                ) {
                    VBadge(
                        text = status.label,
                        tone = status.tone,
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD32F2F))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDelete,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = card.personName,
                    style = VTheme.type.bodyStrong.colored(c.ink),
                    maxLines = 1,
                )
                Text(
                    text = card.personType.replaceFirstChar { it.uppercase() },
                    style = VTheme.type.caption.colored(c.ink2),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    card.pdfUrl?.let {
                        VButton(
                            text = "PDF",
                            onClick = onDownloadPdf,
                            variant = VButtonVariant.Secondary,
                            size = VButtonSize.Sm,
                        )
                    }
                    VButton(
                        text = "Verify",
                        onClick = { onVerify() },
                        variant = VButtonVariant.Secondary,
                        size = VButtonSize.Sm,
                    )
                }
            }
        }
    }
}

private data class CardStatus(val label: String, val tone: VBadgeTone)

private fun cardStatus(validTill: String?): CardStatus {
    if (validTill == null) return CardStatus("No Expiry", VBadgeTone.Neutral)
    return try {
        val today = com.littlebridge.enrollplus.util.todayIso()
        val cmp = validTill.compareTo(today)
        when {
            cmp < 0 -> CardStatus("Expired", VBadgeTone.Danger)
            cmp == 0 -> CardStatus("Expiring", VBadgeTone.Warning)
            else -> {
                val parts = validTill.split("-")
                val tParts = today.split("-")
                if (parts.size == 3 && tParts.size == 3) {
                    val expiryApprox = parts[0].toInt() * 365 + parts[1].toInt() * 30 + parts[2].toInt()
                    val todayApprox = tParts[0].toInt() * 365 + tParts[1].toInt() * 30 + tParts[2].toInt()
                    if (expiryApprox - todayApprox < 30) CardStatus("Expiring", VBadgeTone.Warning)
                    else CardStatus("Valid", VBadgeTone.Success)
                } else {
                    CardStatus("Valid", VBadgeTone.Success)
                }
            }
        }
    } catch (e: Exception) {
        CardStatus("Valid", VBadgeTone.Success)
    }
}

