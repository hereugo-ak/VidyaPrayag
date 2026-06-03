/*
 * File: MediaPicker.wasmJs.kt  (wasmJsMain — actual)
 * Module: ui.media
 *
 * Web (Kotlin/Wasm) placeholder picker. Returns null so the multiplatform
 * build stays green. Android is the primary school-side client.
 */
package com.littlebridge.vidyaprayag.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.littlebridge.vidyaprayag.feature.admin.domain.model.PickedMedia

@Composable
actual fun rememberMediaPicker(
    onPicked: (PickedMedia?) -> Unit
): (MediaPickType) -> Unit = remember {
    { _: MediaPickType -> onPicked(null) }
}
