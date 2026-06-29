package com.littlebridge.enrollplus.ui.v2.screens.parent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.littlebridge.enrollplus.feature.idcard.domain.model.IdCardDto
import com.littlebridge.enrollplus.feature.idcard.presentation.IdCardViewModel
import com.littlebridge.enrollplus.ui.v2.components.QrCodeImage
import com.littlebridge.enrollplus.ui.v2.components.VBackHeader
import com.littlebridge.enrollplus.ui.v2.components.VButton
import com.littlebridge.enrollplus.ui.v2.components.VButtonSize
import com.littlebridge.enrollplus.ui.v2.components.VButtonVariant
import com.littlebridge.enrollplus.ui.v2.components.VCard
import com.littlebridge.enrollplus.ui.v2.screens.collectAsStateV2
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DigitalIdCardScreen(
    childId: String? = null,
    isTeacher: Boolean = false,
    isStaff: Boolean = false,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: IdCardViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    var showFront by remember { mutableStateOf(true) }

    LaunchedEffect(childId, isTeacher, isStaff) {
        when {
            isTeacher -> viewModel.loadTeacherIdCard()
            isStaff -> viewModel.loadStaffIdCard()
            childId != null -> viewModel.loadChildIdCard(childId)
        }
    }

    VTheme {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            VBackHeader(title = "Digital ID Card", onBack = onBack)

            Spacer(modifier = Modifier.height(24.dp))

            state.error?.let { err ->
                VCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Text(text = err, color = VTheme.colors.dangerInk, style = VTheme.type.body)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            val card = state.currentCard
            if (card != null) {
                // OPT-02: Display server-rendered card image via Coil AsyncImage
                // Falls back to hand-drawn DigitalCard composable if URL is null
                card.digitalCardUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = "Digital ID Card",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .fillMaxWidth()
                            .aspectRatio(54f / 86f),
                    )
                } ?: DigitalCard(card = card, showFront = showFront)

                Spacer(modifier = Modifier.height(16.dp))

                // Only show flip button when using fallback (no server image)
                if (card.digitalCardUrl == null) {
                    VButton(
                        text = if (showFront) "Show Back" else "Show Front",
                        onClick = { showFront = !showFront },
                        variant = VButtonVariant.Secondary,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text(
                    text = "Scan the QR code on the back to verify profile",
                    style = VTheme.type.caption,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Valid till: ${card.validTill ?: "N/A"}",
                    style = VTheme.type.bodyStrong,
                )
            } else if (state.isLoading) {
                Text(
                    text = "Loading ID card...",
                    style = VTheme.type.body,
                )
            } else if (state.error == null) {
                Text(
                    text = "No ID card found. Ask admin to generate.",
                    style = VTheme.type.body,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DigitalCard(card: IdCardDto, showFront: Boolean) {
    val primaryColor = VTheme.colors.accent

    Box(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .size(width = 300.dp, height = 480.dp)
            .drawBehind {
                drawRoundRect(
                    color = primaryColor,
                    cornerRadius = CornerRadius(16f, 16f),
                    style = Stroke(width = 2f),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(20.dp),
        ) {
            if (showFront) {
                Text(
                    text = card.personName,
                    style = VTheme.type.h3,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = card.personType.replaceFirstChar { it.uppercase() },
                    style = VTheme.type.body,
                    color = primaryColor,
                )
            } else {
                Text(
                    text = "QR Code",
                    style = VTheme.type.bodyStrong,
                )
                Spacer(modifier = Modifier.height(12.dp))
                QrCodeImage(
                    data = card.qrCodeData,
                    size = 160.dp,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Scan to verify profile",
                    style = VTheme.type.caption,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Valid till: ${card.validTill ?: "N/A"}",
                    style = VTheme.type.caption,
                )
            }
        }
    }
}
