package com.sangwolnongsan.nongdori

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sangwolnongsan.nongdori.shared.ui.theme.NongdoriTheme
import com.sangwolnongsan.nongdori.shared.ui.theme.TextSecondary

/**
 * 농돌이 — 농기계 대리점 현장 엔지니어용 Android 앱.
 *
 * 현 단계: 골격(scaffold). 이후 인증 → 출장 목록/상세 → 수리 입력 순으로 구축.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NongdoriTheme {
                Scaffold { padding ->
                    HomePlaceholder(Modifier.padding(padding))
                }
            }
        }
    }
}

@Composable
private fun HomePlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("농돌이", style = MaterialTheme.typography.headlineLarge)
        Text(
            "농기계 대리점 출장·수리 관리",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
