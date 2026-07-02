package com.littlebridge.enrollplus.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.littlebridge.enrollplus.ui.v2.components.VButton
import com.littlebridge.enrollplus.ui.v2.components.VButtonSize
import com.littlebridge.enrollplus.ui.v2.components.VButtonTone
import com.littlebridge.enrollplus.ui.v2.components.VInput
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import com.littlebridge.enrollplus.ui.v2.theme.colored

@Composable
actual fun rememberBarcodeScanner(
    onScanned: (String) -> Unit,
): BarcodeScanner = remember { DesktopBarcodeScanner(onScanned) }

@Composable
actual fun BarcodeScannerView(
    onScanned: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier,
) {
    val c = VTheme.colors
    var manualBarcode by remember { mutableStateOf("") }

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
            Text("Enter Barcode", style = VTheme.type.h2.colored(c.ink))
            TextButton(onClick = onClose) { Text("Close") }
        }

        VInput(
            value = manualBarcode,
            onValueChange = { manualBarcode = it },
            label = "Barcode",
            modifier = Modifier.fillMaxWidth(),
        )

        VButton(
            text = "Submit",
            onClick = {
                if (manualBarcode.isNotBlank()) {
                    onScanned(manualBarcode)
                    manualBarcode = ""
                }
            },
            full = true,
            tone = VButtonTone.Lavender,
            size = VButtonSize.Md,
        )
    }
}

private class DesktopBarcodeScanner(
    private val onScanned: (String) -> Unit,
) : BarcodeScanner {
    override val hasCamera: Boolean = false
    override fun start() {}
    override fun stop() {}
}
