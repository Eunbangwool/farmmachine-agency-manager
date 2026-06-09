@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.sangwolnongsan.nongdori.web.firebase

/**
 * Firebase JS SDK interop 베이스 레이어.
 * index.html 의 window.nongdori 글로벌에 노출된 Firebase API 를 @JsFun 으로 호출.
 * 데이터 교환은 string/Boolean 위주 (복잡한 건 JSON 문자열).
 */

/** Firebase 초기화 완료 여부. */
@JsFun("() => !!(window.nongdori && window.nongdori.ready)")
external fun isFirebaseReady(): Boolean

// ===== Auth =====

@JsFun(
    """(cb) => {
    try {
        const p = window.nongdori.signInGoogle();
        if (p && p.then) {
            p.then(r => { cb(r && r.user ? r.user.uid : ''); })
             .catch(err => { console.warn('signIn failed', err); cb(''); });
        } else { cb(''); }
    } catch (err) { console.warn('signIn exception', err); cb(''); }
}"""
)
external fun jsSignInGoogle(cb: (String) -> Unit)

@JsFun("() => window.nongdori.signOut()")
external fun jsSignOut()

@JsFun("() => { var u = window.nongdori.currentUser(); return u ? u.uid : ''; }")
external fun jsCurrentUserUid(): String

@JsFun("() => { var u = window.nongdori.currentUser(); return u && u.email ? u.email : ''; }")
external fun jsCurrentUserEmail(): String

@JsFun("() => { var u = window.nongdori.currentUser(); return u && u.displayName ? u.displayName : ''; }")
external fun jsCurrentUserDisplayName(): String

@JsFun(
    """(cb) => {
    window.nongdori.onAuthStateChanged(user => {
        cb(user ? ('signed_in:' + user.uid) : 'signed_out');
    });
}"""
)
external fun jsSubscribeAuthState(cb: (String) -> Unit)
