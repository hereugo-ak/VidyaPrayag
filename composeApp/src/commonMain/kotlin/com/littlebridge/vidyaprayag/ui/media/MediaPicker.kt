/*
 * File: MediaPicker.kt  (commonMain — expect)
 * Module: ui.media
 *
 * Cross-platform image/video picker. The school-side branding, gallery and
 * profile screens use this to let the user pick a REAL file from their device,
 * which is then uploaded to Supabase Storage via MediaApi — replacing the old
 * "paste a URL" placeholders.
 *
 * Returns the picked bytes + filename + mime as a shared PickedMedia model.
 * Android has a full implementation (Photo Picker / GetContent). The other
 * targets compile cleanly; their pickers are wired where a platform API
 * exists and otherwise no-op so the multiplatform build stays green.
 */
package com.littlebridge.vidyaprayag.ui.media

import androidx.compose.runtime.Composable
import com.littlebridge.vidyaprayag.feature.admin.domain.model.PickedMedia

/** What kind of media the picker should accept. */
enum class MediaPickType { IMAGE, VIDEO }

/**
 * Remembers a launcher you can invoke to open the system picker.
 *
 * @param onPicked called with the picked file's bytes/name/mime, or null if
 *                 the user cancelled or the file couldn't be read.
 * @return a lambda — call it with a [MediaPickType] to launch the picker.
 */
@Composable
expect fun rememberMediaPicker(
    onPicked: (PickedMedia?) -> Unit
): (MediaPickType) -> Unit
