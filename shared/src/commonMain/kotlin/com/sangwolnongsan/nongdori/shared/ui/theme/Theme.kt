package com.sangwolnongsan.nongdori.shared.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 농돌이 테마 — :app(Android) 와 :web 공용. dynamic color 미사용
 * (기기마다 색이 달라지면 상태 색 의미가 흔들리므로).
 *
 * Color.kt 의 토큰 getter 들이 MaterialTheme.colorScheme.background 의 luminance 로
 * light/dark 를 자동 분기하므로, 여기서는 colorScheme 만 올바르게 set 하면 된다.
 */
@Composable
fun NongdoriTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFFAAA8A2),
            background = Color(0xFF121110),
            surface = Color(0xFF1B1A19),
        )
    } else {
        lightColorScheme(
            primary = Primary,
            background = Color(0xFFFAF9F4),
            surface = Color(0xFFFAF9F4),
        )
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
