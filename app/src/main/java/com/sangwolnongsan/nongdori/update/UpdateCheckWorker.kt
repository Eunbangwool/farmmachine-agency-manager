package com.sangwolnongsan.nongdori.update

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * 정기 백그라운드 업데이트 체크 → 새 버전 발견 시 알림. (농작이 이식)
 * 사용자가 알림 탭 → MainActivity 진입 → 홈의 업데이트 확인 흐름으로 다운로드/설치.
 */
class UpdateCheckWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val prefs = UpdateCheckPreferences(ctx)
        if (!prefs.isAutoEnabled) return Result.success()
        return try {
            when (val r = AppUpdateChecker.checkForUpdate()) {
                is AppUpdateChecker.CheckResult.UpdateAvailable -> {
                    prefs.lastCheckedAt = System.currentTimeMillis()
                    val newVer = r.info.latestVersionCode
                    if (newVer != prefs.lastNotifiedVersionCode) {
                        UpdateNotificationHelper.showUpdateAvailable(ctx, r.info)
                        prefs.lastNotifiedVersionCode = newVer
                    }
                    Result.success()
                }
                AppUpdateChecker.CheckResult.UpToDate -> {
                    prefs.lastCheckedAt = System.currentTimeMillis()
                    Result.success()
                }
                is AppUpdateChecker.CheckResult.Error -> {
                    Log.w(TAG, "체크 실패: ${r.message} — retry")
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Worker 예외", e); Result.retry()
        }
    }

    companion object { private const val TAG = "UpdateCheckWorker" }
}
