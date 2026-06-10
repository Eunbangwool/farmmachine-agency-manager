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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sangwolnongsan.nongdori.data.DealershipMembership
import com.sangwolnongsan.nongdori.shared.ui.theme.NongdoriTheme
import com.sangwolnongsan.nongdori.shared.ui.theme.TextSecondary
import com.sangwolnongsan.nongdori.ui.screens.DealerCodeScreen
import com.sangwolnongsan.nongdori.ui.screens.LoginScreen
import kotlinx.coroutines.launch

/**
 * 농돌이 — 농기계 대리점 현장 엔지니어용 Android 앱.
 *
 * 진입 흐름: Firebase 설정 확인 → Google 로그인 → 대리점 생성/가입 → 홈.
 * 홈(출장 목록/상세/수리 입력)은 다음 단계에서 구축.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppContainer.init(applicationContext)
        // 정기 자동 업데이트 체크 (WorkManager) — prefs 기준 schedule.
        try {
            com.sangwolnongsan.nongdori.update.UpdateCheckScheduler.syncFromPreferences(applicationContext)
        } catch (e: Exception) {
            android.util.Log.w("UpdateCheck", "schedule failed: ${e.message}")
        }
        setContent {
            NongdoriTheme {
                Scaffold { padding ->
                    Column(Modifier.fillMaxSize().padding(padding)) {
                        AppRoot()
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRoot() {
    if (!AppContainer.isFirebaseReady) {
        ConfigNeeded()
        return
    }

    val user by AppContainer.userManager.currentUser.collectAsState()
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var dealerCode by remember { mutableStateOf(AppContainer.dealerCodeManager.dealerCode) }

    // 로그인되면 운영자 부트스트랩 + 내 대리점 자동 탐색.
    LaunchedEffect(user?.uid) {
        if (user != null && dealerCode.isNullOrBlank()) {
            runCatching { DealershipMembership.bootstrapOwnerIfAdmin() }
            val mine = runCatching { DealershipMembership.findMyMemberDealerships() }.getOrDefault(emptyList())
            mine.firstOrNull()?.let {
                AppContainer.dealerCodeManager.dealerCode = it
                dealerCode = it
            }
        }
    }

    when {
        user == null -> LoginScreen(
            loading = loading,
            error = error,
            onSignIn = {
                loading = true; error = null
                scope.launch {
                    val result = AppContainer.userManager.signInWithGoogle(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                    loading = false
                    if (result.isFailure) error = "로그인 실패: ${result.exceptionOrNull()?.message}"
                }
            },
        )

        dealerCode.isNullOrBlank() -> DealerCodeScreen(
            loading = loading,
            error = error,
            onCreate = { name ->
                loading = true; error = null
                scope.launch {
                    val code = DealershipMembership.generateDealerCode()
                    when (val r = DealershipMembership.createNewDealership(code, name)) {
                        is DealershipMembership.CreateResult.Success -> {
                            AppContainer.dealerCodeManager.dealerCode = code
                            dealerCode = code
                        }
                        is DealershipMembership.CreateResult.Error -> error = "생성 실패: ${r.message}"
                        else -> error = "대리점 생성에 실패했습니다."
                    }
                    loading = false
                }
            },
            onJoin = { code ->
                loading = true; error = null
                scope.launch {
                    // 초대 수락 시도 → 멤버 확인. (초대받지 않았다면 관리자에게 초대 요청 필요.)
                    DealershipMembership.acceptInvitation(code)
                    if (DealershipMembership.isMember(code)) {
                        AppContainer.dealerCodeManager.dealerCode = code
                        dealerCode = code
                    } else {
                        error = "가입 실패: 초대가 필요하거나 코드가 올바르지 않습니다."
                    }
                    loading = false
                }
            },
            onSignOut = { scope.launch { AppContainer.userManager.signOut(); dealerCode = null } },
        )

        else -> com.sangwolnongsan.nongdori.ui.screens.EngineerHome(
            dealerCode = dealerCode!!,
            uid = user!!.uid,
            engineerName = user!!.displayName ?: user!!.email ?: "",
            onSignOut = { scope.launch { AppContainer.userManager.signOut(); dealerCode = null } },
        )
    }
}

@Composable
private fun ConfigNeeded() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("농돌이", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Firebase 설정이 필요합니다.\napp/google-services.json 을 배치한 뒤 다시 실행하세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

