package com.sangwolnongsan.nongdori.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sangwolnongsan.nongdori.BuildConfig
import com.sangwolnongsan.nongdori.update.AppUpdateChecker

/**
 * 앱 업데이트 다이얼로그 — 진입 시 새 버전 확인 후 결과 표시.
 * @param manual 사용자가 직접 "업데이트 확인" 누른 경우 true → GitHub 강제 새로고침 + 최신/에러도 표시.
 *               자동(앱 시작)인 경우 false → 새 버전 있을 때만 띄움(호출 측에서 제어).
 * @param preloaded 호출 측이 이미 체크한 결과 — 있으면 재확인(중복 네트워크 호출) 생략.
 */
@Composable
fun UpdateDialog(manual: Boolean, preloaded: AppUpdateChecker.UpdateInfo? = null, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var state by remember { mutableStateOf<AppUpdateChecker.CheckResult?>(null) }

    LaunchedEffect(manual) {
        state = if (!manual && preloaded != null) AppUpdateChecker.CheckResult.UpdateAvailable(preloaded)
        else AppUpdateChecker.checkForUpdate(forceGitHubRefresh = manual)
    }

    val s = state
    val info = (s as? AppUpdateChecker.CheckResult.UpdateAvailable)?.info

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("앱 업데이트") },
        text = {
            when {
                s == null -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("새 버전 확인 중…")
                }
                info != null -> {
                    val sizeMb = info.sizeBytes / (1024f * 1024f)
                    Text(
                        "새 버전 v${info.latestVersionCode}" +
                            (info.latestVersionName?.let { " ($it)" } ?: "") +
                            (if (info.sizeBytes > 0) " · %.1f MB".format(sizeMb) else "") +
                            "\n\n‘지금 업데이트’를 누르면 다운로드 후 설치 화면이 자동으로 뜹니다. " +
                            "첫 설치 시 ‘출처를 알 수 없는 앱’ 허용이 필요합니다."
                    )
                }
                s is AppUpdateChecker.CheckResult.Error ->
                    Text("업데이트 확인 실패\n${s.message}")
                else ->
                    Text("이미 최신 버전입니다 (v${BuildConfig.VERSION_CODE}).")
            }
        },
        confirmButton = {
            if (info != null) {
                TextButton(onClick = { AppUpdateChecker.downloadAndInstall(context, info); onDismiss() }) {
                    Text("지금 업데이트")
                }
            } else {
                TextButton(onClick = onDismiss) { Text("확인") }
            }
        },
        dismissButton = {
            if (info != null) TextButton(onClick = onDismiss) { Text("나중에") }
        },
    )
}
