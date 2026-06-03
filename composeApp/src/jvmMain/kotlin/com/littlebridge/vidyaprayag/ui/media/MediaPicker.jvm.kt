/*
 * File: MediaPicker.jvm.kt  (jvmMain — actual)
 * Module: ui.media
 *
 * Desktop picker using Swing's JFileChooser, read on a background thread.
 * Fully functional for the desktop preview; Android remains the primary
 * school-side client.
 */
package com.littlebridge.vidyaprayag.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.littlebridge.vidyaprayag.feature.admin.domain.model.PickedMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
actual fun rememberMediaPicker(
    onPicked: (PickedMedia?) -> Unit
): (MediaPickType) -> Unit {
    val scope = rememberCoroutineScope()
    return remember {
        { type: MediaPickType ->
            scope.launch {
                val picked = withContext(Dispatchers.IO) {
                    runCatching {
                        val chooser = JFileChooser()
                        chooser.fileFilter = when (type) {
                            MediaPickType.IMAGE -> FileNameExtensionFilter(
                                "Images", "jpg", "jpeg", "png", "webp", "gif"
                            )
                            MediaPickType.VIDEO -> FileNameExtensionFilter(
                                "Videos", "mp4", "mov", "webm"
                            )
                        }
                        val result = chooser.showOpenDialog(null)
                        if (result != JFileChooser.APPROVE_OPTION) return@runCatching null
                        val file: File = chooser.selectedFile ?: return@runCatching null
                        PickedMedia(
                            bytes = file.readBytes(),
                            fileName = file.name,
                            mimeType = mimeFor(file.extension.lowercase())
                        )
                    }.getOrNull()
                }
                onPicked(picked)
            }
            Unit
        }
    }
}

private fun mimeFor(ext: String): String = when (ext) {
    "png" -> "image/png"
    "webp" -> "image/webp"
    "gif" -> "image/gif"
    "mp4" -> "video/mp4"
    "mov" -> "video/quicktime"
    "webm" -> "video/webm"
    else -> "image/jpeg"
}
