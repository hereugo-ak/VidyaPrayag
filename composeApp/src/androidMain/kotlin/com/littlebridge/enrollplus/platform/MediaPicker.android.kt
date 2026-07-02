package com.littlebridge.enrollplus.platform

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberMediaPicker(
    onPicked: (bytes: ByteArray, mimeType: String, fileName: String) -> Unit,
    onUnsupported: (String) -> Unit,
): MediaPicker {
    val context = LocalContext.current

    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
            val name = queryFileName(context, uri) ?: "picked_image"
            val bytes = readBytes(context, uri)
            if (bytes != null) {
                onPicked(bytes, mime, name)
            } else {
                onUnsupported("Could not read the selected image.")
            }
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val mime = context.contentResolver.getType(uri) ?: "application/pdf"
            val name = queryFileName(context, uri) ?: "picked_document.pdf"
            val bytes = readBytes(context, uri)
            if (bytes != null) {
                onPicked(bytes, mime, name)
            } else {
                onUnsupported("Could not read the selected file.")
            }
        }
    }

    return remember(imageLauncher, pdfLauncher) {
        object : MediaPicker {
            override fun launchImage() {
                imageLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }

            override fun launchPdf() {
                pdfLauncher.launch("application/pdf")
            }
        }
    }
}

private fun readBytes(context: Context, uri: Uri): ByteArray? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    } catch (e: Exception) {
        null
    }
}

private fun queryFileName(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
        }
    } catch (e: Exception) {
        null
    }
}
