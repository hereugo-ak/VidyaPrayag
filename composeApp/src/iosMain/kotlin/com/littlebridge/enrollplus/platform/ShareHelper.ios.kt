package com.littlebridge.enrollplus.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIPasteboard
import platform.Foundation.NSString
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

@Composable
actual fun rememberShareHelper(): ShareHelper {
    return remember { IosShareHelper() }
}

private class IosShareHelper : ShareHelper {
    override fun shareText(text: String, subject: String?) {
        val activityItems: List<Any> = if (subject != null) {
            listOf("$subject\n\n$text")
        } else {
            listOf(text as NSString)
        }
        val activityVC = UIActivityViewController(activityItems = activityItems, applicationActivities = null)

        val window = UIApplication.sharedApplication
            .connectedScenes
            .filterIsInstance<UIWindowScene>()
            .firstOrNull()
            ?.windows
            ?.firstOrNull { it.isKeyWindow() }

        val rootVC = window?.rootViewController
        rootVC?.presentViewController(activityVC, animated = true, completion = null)
    }
}
