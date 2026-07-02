package com.littlebridge.enrollplus.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.littlebridge.enrollplus.ui.v2.components.VInput
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored

@Composable
actual fun rememberBarcodeScanner(
    onScanned: (String) -> Unit,
): BarcodeScanner {
    val scanner = remember { AndroidBarcodeScanner(onScanned) }
    DisposableEffect(scanner) {
        onDispose { scanner.stop() }
    }
    return scanner
}

@Composable
actual fun BarcodeScannerView(
    onScanned: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier,
) {
    val c = VTheme.colors
    var manualBarcode by remember { mutableStateOf("") }

    // Fallback: manual text input (camera integration requires ML Kit + CameraX
    // dependencies which are not yet wired into the build.gradle.kts).
    // The live camera preview will be enabled when those deps are added.
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Scan Barcode", style = VTheme.type.h2.colored(c.ink))
            TextButton(onClick = onClose) { Text("Close") }
        }

        Text(
            "Enter the barcode manually below. Camera scanning will be available in a future update.",
            style = VTheme.type.caption.colored(c.ink2),
        )

        VInput(
            value = manualBarcode,
            onValueChange = { manualBarcode = it },
            label = "Barcode",
            modifier = Modifier.fillMaxWidth(),
        )

        com.littlebridge.enrollplus.ui.v2.components.VButton(
            text = "Submit",
            onClick = {
                if (manualBarcode.isNotBlank()) {
                    onScanned(manualBarcode)
                    manualBarcode = ""
                }
            },
            full = true,
            tone = com.littlebridge.enrollplus.ui.v2.components.VButtonTone.Lavender,
            size = com.littlebridge.enrollplus.ui.v2.components.VButtonSize.Md,
        )
    }
}

private class AndroidBarcodeScanner(
    private val onScanned: (String) -> Unit,
) : BarcodeScanner {
    override val hasCamera: Boolean = true

    override fun start() {
        // Camera preview lifecycle managed by BarcodeScannerView composable
    }

    override fun stop() {
        // Release camera resources when composable leaves composition
    }
}
