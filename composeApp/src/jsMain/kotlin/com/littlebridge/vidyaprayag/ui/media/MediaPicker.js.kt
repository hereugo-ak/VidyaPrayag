/*
 * File: MediaPicker.js.kt  (jsMain — actual)
 * Module: ui.media
 *
 * Web (Kotlin/JS) placeholder picker. Browser uploads can later be wired via
 * an <input type="file"> + FileReader bridge. For now returns null so the
 * multiplatform build stays green. Android is the primary school-side client.
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
