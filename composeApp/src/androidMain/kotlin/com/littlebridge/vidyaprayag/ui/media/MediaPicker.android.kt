/*
 * File: MediaPicker.android.kt  (androidMain — actual)
 * Module: ui.media
 *
 * Real Android implementation using ActivityResultContracts.GetContent.
 * Reads the chosen content:// uri into a ByteArray off the main thread, then
 * hands it back as a shared PickedMedia. No external picker library needed —
 * GetContent works on all supported API levels and surfaces the modern Photo
 * Picker automatically on Android 13+.
 */
package com.littlebridge.vidyaprayag.ui.media

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.littlebridge.vidyaprayag.feature.admin.domain.model.PickedMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
actual fun rememberMediaPicker(
    onPicked: (PickedMedia?) -> Unit
): (MediaPickType) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) {
            onPicked(null)
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val picked = withContext(Dispatchers.IO) {
                runCatching {
                    val resolver = context.contentResolver
                    val mime = resolver.getType(uri) ?: "application/octet-stream"
                    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: return@runCatching null
                    val name = queryDisplayName(context, uri) ?: defaultName(mime)
                    PickedMedia(bytes = bytes, fileName = name, mimeType = mime)
                }.getOrNull()
            }
            onPicked(picked)
        }
    }

    return remember(launcher) {
        { type: MediaPickType ->
            val filter = when (type) {
                MediaPickType.IMAGE -> "image/*"
                MediaPickType.VIDEO -> "video/*"
            }
            launcher.launch(filter)
        }
    }
}

private fun queryDisplayName(
    context: android.content.Context,
    uri: android.net.Uri
): String? = runCatching {
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
    }
}.getOrNull()

private fun defaultName(mime: String): String {
    val ext = when {
        mime.contains("png") -> "png"
        mime.contains("webp") -> "webp"
        mime.contains("gif") -> "gif"
        mime.contains("mp4") -> "mp4"
        mime.contains("quicktime") -> "mov"
        mime.contains("jpeg") || mime.contains("jpg") -> "jpg"
        else -> "bin"
    }
    return "upload_${kotlin.random.Random.nextInt(100000, 999999)}.$ext"
}
