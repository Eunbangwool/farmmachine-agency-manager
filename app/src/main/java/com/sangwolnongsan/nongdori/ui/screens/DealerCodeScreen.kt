package com.sangwolnongsan.nongdori.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sangwolnongsan.nongdori.shared.ui.theme.TextSecondary

/**
 * 대리점 진입 — 새 대리점 생성 또는 기존 대리점 코드로 가입.
 * 엔지니어는 보통 초대를 통해, 대표/관리자는 새 대리점을 생성한다.
 */
@Composable
fun DealerCodeScreen(
    loading: Boolean,
    error: String?,
    onCreate: (name: String) -> Unit,
    onJoin: (code: String) -> Unit,
    onSignOut: () -> Unit,
) {
    var mode by remember { mutableStateOf(Mode.CHOOSE) }
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("대리점 설정", style = MaterialTheme.typography.headlineMedium)
        Text(
            "새 대리점을 만들거나, 받은 대리점 코드로 가입하세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
        )

        when (mode) {
            Mode.CHOOSE -> {
                Button(onClick = { mode = Mode.CREATE }, modifier = Modifier.fillMaxWidth()) {
                    Text("새 대리점 만들기")
                }
                OutlinedButton(
                    onClick = { mode = Mode.JOIN },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                ) {
                    Text("대리점 코드로 가입")
                }
            }
            Mode.CREATE -> {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("대리점 이름") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = { onCreate(name) },
                    enabled = !loading && name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                ) { Text("생성") }
                TextButton(onClick = { mode = Mode.CHOOSE }) { Text("뒤로") }
            }
            Mode.JOIN -> {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.filter { c -> c.isDigit() } },
                    label = { Text("대리점 코드") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = { onJoin(code) },
                    enabled = !loading && code.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                ) { Text("가입") }
                TextButton(onClick = { mode = Mode.CHOOSE }) { Text("뒤로") }
            }
        }

        if (loading) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
        }
        if (error != null) {
            Text(
                error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
        TextButton(onClick = onSignOut, modifier = Modifier.padding(top = 24.dp)) {
            Text("로그아웃", color = TextSecondary)
        }
    }
}

private enum class Mode { CHOOSE, CREATE, JOIN }
