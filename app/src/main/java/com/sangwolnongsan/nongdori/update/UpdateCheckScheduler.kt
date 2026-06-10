package com.sangwolnongsan.nongdori.update

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * 자동 업데이트 체크 WorkManager 스케줄링. (농작이 이식)
 * 앱 시작 시 syncFromPreferences 호출 → prefs(on/off, 주기) 기준 schedule / cancel.
 */
object UpdateCheckScheduler {

    private const val TAG = "UpdateCheckScheduler"
    private const val UNIQUE_NAME = "nongdori_update_check"

    fun syncFromPreferences(context: Context) {
        val prefs = UpdateCheckPreferences(context)
        if (!prefs.isAutoEnabled) { cancel(context); return }
        schedule(context, prefs.interval)
    }

    fun schedule(context: Context, interval: UpdateCheckPreferences.Interval) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(interval.hours, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_NAME, ExistingPeriodicWorkPolicy.UPDATE, request,
        )
        Log.i(TAG, "자동 업데이트 체크 schedule: ${interval.displayName} (${interval.hours}h)")
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
    }
}
