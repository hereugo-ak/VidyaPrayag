/*
 * File: MediaPicker.ios.kt  (iosMain — actual)
 * Module: ui.media
 *
 * iOS placeholder picker. The Android app is the primary school-side client;
 * iOS uploads can be wired to UIImagePickerController / PHPickerViewController
 * later. For now this returns null (cancelled) so the multiplatform build
 * stays green and the UI shows its "pick to upload" affordance without crash.
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
