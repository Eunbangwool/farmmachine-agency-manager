package com.sangwolnongsan.nongdori.update

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.sangwolnongsan.nongdori.BuildConfig
import kotlinx.coroutines.tasks.await

/**
 * 농돌이 release 메타데이터 — Firestore 단일 doc 으로 read/write (GitHub rate limit 우회).
 *
 * 경로: app_meta/nongdori_release (운영), app_meta/nongdori_debug (디버그).
 * 농작이(app_meta/latest_release|latest_debug)와 같은 컬렉션이지만 doc id 가 달라 충돌 없음.
 */
object UpdateMetaRepository {

    private const val TAG = "UpdateMeta"
    private const val COLLECTION = "app_meta"

    val docId: String = if (BuildConfig.IS_DEBUG_APP) "nongdori_debug" else "nongdori_release"

    private fun db() = FirebaseFirestore.getInstance()

    suspend fun fetch(): AppUpdateChecker.UpdateInfo? {
        return try {
            val snap = db().collection(COLLECTION).document(docId).get().await()
            if (!snap.exists()) return null
            val versionCode = snap.getLong("versionCode")?.toInt() ?: return null
            val downloadUrl = snap.getString("downloadUrl") ?: return null
            AppUpdateChecker.UpdateInfo(
                latestVersionCode = versionCode,
                latestVersionName = snap.getString("versionName"),
                downloadUrl = downloadUrl,
                sizeBytes = snap.getLong("sizeBytes") ?: 0L,
                buildTime = snap.getString("buildTime"),
            )
        } catch (e: Exception) {
            Log.w(TAG, "fetch 실패 — GitHub fallback 으로 진행", e); null
        }
    }

    suspend fun write(info: AppUpdateChecker.UpdateInfo): Boolean {
        return try {
            db().collection(COLLECTION).document(docId).set(
                mapOf(
                    "versionCode" to info.latestVersionCode,
                    "versionName" to (info.latestVersionName ?: ""),
                    "downloadUrl" to info.downloadUrl,
                    "sizeBytes" to info.sizeBytes,
                    "buildTime" to (info.buildTime ?: ""),
                    "updatedAt" to System.currentTimeMillis(),
                )
            ).await()
            Log.i(TAG, "meta 갱신 OK ($docId) — v${info.latestVersionCode}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "write 실패 — Firestore rules / 권한 확인", e); false
        }
    }
}
