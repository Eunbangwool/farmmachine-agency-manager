package com.sangwolnongsan.nongdori.data

import android.content.Context
import com.google.firebase.FirebaseApp

/**
 * Firebase 초기화 여부 — google-services.json 이 배치되어 google-services 플러그인이
 * FirebaseApp 을 자동 초기화했을 때만 true. 미설정 시 앱은 로컬 placeholder 로 동작.
 */
object FirebaseAvailability {
    @Volatile
    var isAvailable: Boolean = false
        private set

    fun check(context: Context) {
        isAvailable = try {
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (_: Throwable) {
            false
        }
    }
}
