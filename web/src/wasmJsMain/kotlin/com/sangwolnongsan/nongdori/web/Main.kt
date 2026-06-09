package com.sangwolnongsan.nongdori.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document

/**
 * 농돌이 웹(디스패처) 진입점.
 * index.html 의 #compose-target div 에 Compose UI 마운트. Android MainActivity 와 동일 역할.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val target = document.getElementById("compose-target")
        ?: error("Element with id 'compose-target' not found in index.html")
    ComposeViewport(target) {
        NongdoriWebApp()
    }
}
