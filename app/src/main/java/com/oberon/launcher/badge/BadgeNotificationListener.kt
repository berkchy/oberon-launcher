package com.oberon.launcher.badge

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow

object BadgeStore {
    val badges = MutableStateFlow<Map<String, Int>>(emptyMap())
}

class BadgeNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        refresh()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        stopSelf()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refresh()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refresh()
    }

    private fun refresh() {
        val map = HashMap<String, Int>()
        for (sbn in activeNotifications) {
            val notif = sbn.notification
            if (notif.flags and Notification.FLAG_GROUP_SUMMARY != 0) continue
            val pkg = sbn.packageName
            map[pkg] = (map[pkg] ?: 0) + 1
        }
        BadgeStore.badges.value = map
    }
}