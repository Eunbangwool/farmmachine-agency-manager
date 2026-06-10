package com.sangwolnongsan.nongdori.update

import android.content.Context
import android.content.SharedPreferences

/**
 * 자동 업데이트 체크 설정 + 메타. (농작이 이식)
 *  - isAutoEnabled: 기본 ON
 *  - interval: DAILY / WEEKLY (기본 WEEKLY)
 *  - lastNotifiedVersionCode: 같은 버전 반복 알림 방지
 */
class UpdateCheckPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    enum class Interval(val displayName: String, val hours: Long) {
        DAILY("매일", 24),
        WEEKLY("매주", 24 * 7),
    }

    var isAutoEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    var interval: Interval
        get() = runCatching { Interval.valueOf(prefs.getString(KEY_INTERVAL, null) ?: Interval.WEEKLY.name) }
            .getOrDefault(Interval.WEEKLY)
        set(value) = prefs.edit().putString(KEY_INTERVAL, value.name).apply()

    var lastCheckedAt: Long
        get() = prefs.getLong(KEY_LAST_CHECKED, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_CHECKED, value).apply()

    var lastNotifiedVersionCode: Int
        get() = prefs.getInt(KEY_LAST_NOTIFIED, 0)
        set(value) = prefs.edit().putInt(KEY_LAST_NOTIFIED, value).apply()

    companion object {
        private const val PREFS = "update_check"
        private const val KEY_ENABLED = "auto_enabled"
        private const val KEY_INTERVAL = "interval"
        private const val KEY_LAST_CHECKED = "last_checked_at"
        private const val KEY_LAST_NOTIFIED = "last_notified_version_code"
    }
}
