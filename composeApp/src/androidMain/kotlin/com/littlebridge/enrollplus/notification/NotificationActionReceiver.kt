package com.littlebridge.enrollplus.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotifActionReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(NotificationManagerHelper.EXTRA_NOTIFICATION_ID, -1)
        val actionType = intent.getStringExtra("action_type")

        Log.d(TAG, "onReceive: actionType=$actionType notificationId=$notificationId")

        if (notificationId != -1) {
            NotificationManagerCompat.from(context).cancel(notificationId)
        }

        // Future: route APPROVE/REJECT/LINK_VIEW to the appropriate API + deep link.
        // For now the action just dismisses the notification.
    }
}
