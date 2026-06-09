package com.sangwolnongsan.nongdori.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat

/**
 * 출동 위치 공유 Foreground Service 용 알림 채널/빌더.
 */
object NotificationHelper {
    const val CHANNEL_ID_TRACKING = "engineer_location_tracking"
    const val NOTIF_ID_TRACKING = 4201

    fun ensureChannel(context: Context) {
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return
        if (mgr.getNotificationChannel(CHANNEL_ID_TRACKING) == null) {
            mgr.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID_TRACKING,
                    "출동 위치 공유",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply { description = "출동 중 대리점에 현재 위치를 공유합니다." }
            )
        }
    }

    fun buildTrackingNotification(context: Context, text: String): Notification {
        ensureChannel(context)
        return NotificationCompat.Builder(context, CHANNEL_ID_TRACKING)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("출동 중 — 위치 공유")
            .setContentText(text)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }
}
