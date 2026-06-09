@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.sangwolnongsan.nongdori.web.firebase

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 웹 사용자 (Android UserManager 의 웹 버전). */
data class WebUser(
    val uid: String,
    val email: String,
    val displayName: String,
)

@JsFun("(cb) => setTimeout(cb, 50)")
private external fun jsScheduleRetry(cb: () -> Unit)

object AuthManager {
    private val _user = MutableStateFlow<WebUser?>(null)
    val user: StateFlow<WebUser?> = _user.asStateFlow()

    private var subscribed = false

    /** 앱 시작 시 한 번 호출. Firebase 준비 안 됐으면 짧게 대기 후 재시도. */
    fun init() {
        if (subscribed) return
        if (!isFirebaseReady()) {
            jsScheduleRetry { init() }
            return
        }
        subscribed = true
        refreshFromCurrentUser()
        jsSubscribeAuthState { stateString ->
            if (stateString.startsWith("signed_in:")) refreshFromCurrentUser()
            else _user.value = null
        }
    }

    private fun refreshFromCurrentUser() {
        val uid = jsCurrentUserUid()
        _user.value = if (uid.isBlank()) null
        else WebUser(uid, jsCurrentUserEmail(), jsCurrentUserDisplayName())
    }

    fun signInGoogle() = jsSignInGoogle { }

    fun signOut() {
        jsSignOut()
        _user.value = null
    }
}
