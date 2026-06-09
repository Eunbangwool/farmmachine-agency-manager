package com.sangwolnongsan.nongdori.web

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sangwolnongsan.nongdori.shared.ui.theme.NongdoriTheme
import com.sangwolnongsan.nongdori.shared.ui.theme.TextSecondary
import com.sangwolnongsan.nongdori.web.firebase.AuthManager
import com.sangwolnongsan.nongdori.web.firebase.WebDealershipMembership
import kotlinx.coroutines.launch

/**
 * 농돌이 웹 앱 루트 (디스패처용).
 * 흐름: Google 로그인 → 대리점 생성/가입 → 홈(배차 보드는 다음 단계).
 */
@Composable
fun NongdoriWebApp() {
    NongdoriTheme {
        Scaffold { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                WebRoot()
            }
        }
    }
}

@Composable
private fun WebRoot() {
    LaunchedEffect(Unit) { AuthManager.init() }

    val user by AuthManager.user.collectAsState()
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var dealerCode by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(user?.uid) {
        if (user != null && dealerCode.isNullOrBlank()) {
            dealerCode = WebDealershipMembership.findMyMemberDealerships().firstOrNull()
        }
    }

    when {
        user == null -> CenteredColumn {
            Text("농돌이", style = MaterialTheme.typography.headlineLarge)
            Text(
                "농기계 대리점 디스패처 — 출장 배정 · 배차 보드 · 수리 이력",
                style = MaterialTheme.typography.bodyMedium, color = TextSecondary,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
            )
            Button(onClick = { AuthManager.signInGoogle() }) { Text("Google 계정으로 로그인") }
        }

        dealerCode.isNullOrBlank() -> DealerCodeArea(
            loading = loading,
            error = error,
            onCreate = { name ->
                loading = true; error = null
                scope.launch {
                    val code = WebDealershipMembership.generateDealerCode()
                    if (WebDealershipMembership.createNewDealership(code, name)) dealerCode = code
                    else error = "대리점 생성에 실패했습니다."
                    loading = false
                }
            },
            onJoin = { code ->
                loading = true; error = null
                scope.launch {
                    WebDealershipMembership.acceptInvitation(code)
                    if (WebDealershipMembership.isMember(code)) dealerCode = code
                    else error = "가입 실패: 초대가 필요하거나 코드가 올바르지 않습니다."
                    loading = false
                }
            },
            onSignOut = { AuthManager.signOut(); dealerCode = null },
        )

        else -> com.sangwolnongsan.nongdori.web.ui.DispatcherHome(
            dealerCode = dealerCode!!,
            onSignOut = { AuthManager.signOut(); dealerCode = null },
        )
    }
}

@Composable
private fun DealerCodeArea(
    loading: Boolean,
    error: String?,
    onCreate: (String) -> Unit,
    onJoin: (String) -> Unit,
    onSignOut: () -> Unit,
) {
    var mode by remember { mutableStateOf(0) } // 0=choose 1=create 2=join
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }

    CenteredColumn {
        Text("대리점 설정", style = MaterialTheme.typography.headlineMedium)
        Text(
            "새 대리점을 만들거나, 받은 대리점 코드로 가입하세요.",
            style = MaterialTheme.typography.bodyMedium, color = TextSecondary,
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )
        when (mode) {
            0 -> {
                Button(onClick = { mode = 1 }, modifier = Modifier.width(280.dp)) { Text("새 대리점 만들기") }
                OutlinedButton(onClick = { mode = 2 }, modifier = Modifier.width(280.dp).padding(top = 12.dp)) {
                    Text("대리점 코드로 가입")
                }
            }
            1 -> {
                OutlinedTextField(name, { name = it }, label = { Text("대리점 이름") }, singleLine = true, modifier = Modifier.width(280.dp))
                Button(onClick = { onCreate(name) }, enabled = !loading && name.isNotBlank(), modifier = Modifier.width(280.dp).padding(top = 12.dp)) { Text("생성") }
                TextButton(onClick = { mode = 0 }) { Text("뒤로") }
            }
            else -> {
                OutlinedTextField(
                    code, { code = it.filter { c -> c.isDigit() } }, label = { Text("대리점 코드") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.width(280.dp),
                )
                Button(onClick = { onJoin(code) }, enabled = !loading && code.isNotBlank(), modifier = Modifier.width(280.dp).padding(top = 12.dp)) { Text("가입") }
                TextButton(onClick = { mode = 0 }) { Text("뒤로") }
            }
        }
        if (loading) CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
        if (error != null) Text(error, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 16.dp))
        TextButton(onClick = onSignOut, modifier = Modifier.padding(top = 24.dp)) { Text("로그아웃", color = TextSecondary) }
    }
}

@Composable
private fun CenteredColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) { content() }
}
