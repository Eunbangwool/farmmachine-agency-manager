package com.sangwolnongsan.nongdori.web

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sangwolnongsan.nongdori.shared.ui.theme.NongdoriTheme
import com.sangwolnongsan.nongdori.shared.ui.theme.TextSecondary

/**
 * 농돌이 웹 앱 루트 (디스패처용).
 * 현 단계: 골격. 이후 인증 → 배차 보드 / 출장 생성 / 고객 / 이력 순으로 구축.
 */
@Composable
fun NongdoriWebApp() {
    NongdoriTheme {
        Scaffold { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("농돌이", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "농기계 대리점 디스패처 — 출장 배정 · 배차 보드 · 수리 이력",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
