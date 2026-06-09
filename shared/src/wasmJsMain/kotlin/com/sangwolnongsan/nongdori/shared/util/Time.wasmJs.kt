@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.sangwolnongsan.nongdori.shared.util

// Kotlin/Wasm 에는 kotlin.js.Date 가 없음 → JS interop 으로 Date.now() 직접 호출.
@JsFun("() => Date.now()")
private external fun jsDateNow(): Double

actual fun nowMs(): Long = jsDateNow().toLong()
