package com.sangwolnongsan.nongdori.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.sangwolnongsan.nongdori.MainActivity
import com.sangwolnongsan.nongdori.R

/**
 * 자동 업데이트 체크 결과 알림. (농작이 이식)
 * 새 버전 발견 시 알림 → 탭하면 MainActivity 진입 → 홈에서 업데이트 카드 노출.
 */
object UpdateNotificationHelper {

    private const val CHANNEL_UPDATE = "update_available"
    const val NOTIF_ID_UPDATE = 7771
    const val EXTRA_OPEN_UPDATE = "open_update"

    fun showUpdateAvailable(context: Context, info: AppUpdateChecker.UpdateInfo) {
        ensureChannel(context)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_OPEN_UPDATE, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pi = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = "농돌이 새 버전 v${info.latestVersionCode}"
        val body = buildString {
            append("탭하면 업데이트 화면이 열립니다")
            if (!info.latestVersionName.isNullOrBlank()) append(" (${info.latestVersionName})")
        }
        val notif = NotificationCompat.Builder(context, CHANNEL_UPDATE)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(NOTIF_ID_UPDATE, notif)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_UPDATE) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_UPDATE, "앱 업데이트 알림", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "새 버전이 올라오면 알려드립니다"
                }
            )
        }
    }
}
