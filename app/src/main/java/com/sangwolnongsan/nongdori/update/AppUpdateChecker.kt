package com.sangwolnongsan.nongdori.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.sangwolnongsan.nongdori.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * GitHub Releases API로 새 버전 확인 + APK 다운로드 + 설치. (농작이 AppUpdateChecker 이식)
 *
 * 1. Firestore app_meta(nongdori) doc 우선 조회 (GitHub rate limit 회피)
 * 2. stale 가능성 → 강제 새로고침 시 GitHub Releases API fallback
 * 3. release body 의 "versionCode=N" 파싱 → BuildConfig.VERSION_CODE 비교
 * 4. 새 버전이면 DownloadManager 로 APK 받고 설치 화면 자동 표시
 *
 * 채널 분리: 디버그 앱(IS_DEBUG_APP) → tag=latest-debug (Nongdori-debug.apk),
 *           운영 앱 → releases/latest (Nongdori.apk). build.yml 의 태그와 일치.
 */
object AppUpdateChecker {

    private const val TAG = "AppUpdateChecker"
    private const val REPO_OWNER = "Eunbangwool"
    private const val REPO_NAME = "farmmachine-agency-manager"

    private val API_URL: String = if (BuildConfig.IS_DEBUG_APP) {
        "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/tags/latest-debug"
    } else {
        "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"
    }

    data class UpdateInfo(
        val latestVersionCode: Int,
        val latestVersionName: String?,
        val downloadUrl: String,
        val sizeBytes: Long,
        val buildTime: String?,
    )

    sealed class CheckResult {
        data class UpdateAvailable(val info: UpdateInfo) : CheckResult()
        object UpToDate : CheckResult()
        data class Error(val message: String) : CheckResult()
    }

    /**
     * 업데이트 확인. forceGitHubRefresh=true 면 Firestore meta 가 최신으로 보여도
     * GitHub 한 번 호출 (사용자 "다시 확인" = 강제 새로고침, 부트스트랩 deadlock 회피).
     */
    suspend fun checkForUpdate(forceGitHubRefresh: Boolean = false): CheckResult =
        withContext(Dispatchers.IO) {
            val metaInfo: UpdateInfo? = try {
                UpdateMetaRepository.fetch()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore meta 조회 예외", e); null
            }
            if (metaInfo != null && metaInfo.latestVersionCode > BuildConfig.VERSION_CODE) {
                Log.d(TAG, "[Firestore] 새 버전: v${metaInfo.latestVersionCode}")
                return@withContext CheckResult.UpdateAvailable(metaInfo)
            }
            if (forceGitHubRefresh || metaInfo == null) {
                val gh = checkFromGitHub()
                if (gh is CheckResult.UpdateAvailable) {
                    val ghVersion = gh.info.latestVersionCode
                    val metaVersion = metaInfo?.latestVersionCode ?: 0
                    if (ghVersion > metaVersion) {
                        try { UpdateMetaRepository.write(gh.info) }
                        catch (e: Exception) { Log.w(TAG, "meta 갱신 실패", e) }
                    }
                    return@withContext if (ghVersion > BuildConfig.VERSION_CODE) gh else CheckResult.UpToDate
                }
                if (gh is CheckResult.Error) return@withContext gh
            }
            Log.d(TAG, "[Firestore] 최신 버전")
            CheckResult.UpToDate
        }

    /** GitHub Releases REST API 직접 호출 — Firestore fallback. */
    suspend fun checkFromGitHub(): CheckResult = withContext(Dispatchers.IO) {
        try {
            val conn = (URL(API_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "Nongdori-App")
                connectTimeout = 10_000
                readTimeout = 10_000
            }
            val code = conn.responseCode
            if (code != 200) {
                val errBody = try {
                    conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                } catch (_: Exception) { "" }
                conn.disconnect()
                val hint = when (code) {
                    403 -> if (errBody.contains("rate limit", ignoreCase = true))
                        "GitHub API 요청 한도 초과 (시간당 60회). 잠시 후 다시 시도해주세요."
                    else "GitHub 접근 거부 (403)"
                    404 -> "Release 가 없거나 비공개"
                    in 500..599 -> "GitHub 서버 일시 장애 ($code) - 잠시 후 재시도"
                    else -> "GitHub 응답 코드 $code"
                }
                return@withContext CheckResult.Error(hint)
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val json = JSONObject(body)
            val releaseBody = json.optString("body", "")
            val assets = json.optJSONArray("assets")
            if (assets == null || assets.length() == 0) {
                return@withContext CheckResult.Error("Release에 APK 파일이 없습니다")
            }
            val firstApk = (0 until assets.length())
                .map { assets.getJSONObject(it) }
                .firstOrNull { it.optString("name", "").endsWith(".apk") }
                ?: return@withContext CheckResult.Error("APK 파일을 찾을 수 없습니다")

            val versionCode = Regex("""versionCode\s*=\s*(\d+)""")
                .find(releaseBody)?.groupValues?.get(1)?.toIntOrNull()
                ?: return@withContext CheckResult.Error("Release body에 versionCode 정보가 없습니다")

            val versionName = Regex("""versionName\s*=\s*(\S+)""")
                .find(releaseBody)?.groupValues?.get(1)
            val buildTime = Regex("""(?:Build|buildTime)[:\s=]+([\d\-:\s]+)""")
                .find(releaseBody)?.groupValues?.get(1)?.trim()

            val info = UpdateInfo(
                latestVersionCode = versionCode,
                latestVersionName = versionName,
                downloadUrl = firstApk.getString("browser_download_url"),
                sizeBytes = firstApk.optLong("size", 0),
                buildTime = buildTime,
            )
            if (versionCode > BuildConfig.VERSION_CODE) {
                Log.d(TAG, "새 버전: v$versionCode (현재 v${BuildConfig.VERSION_CODE})")
                CheckResult.UpdateAvailable(info)
            } else {
                CheckResult.UpToDate
            }
        } catch (e: java.net.UnknownHostException) {
            CheckResult.Error("인터넷 연결 확인 필요 (api.github.com 접근 불가)")
        } catch (e: java.net.SocketTimeoutException) {
            CheckResult.Error("GitHub 응답 지연 - 네트워크 상태 확인 후 재시도")
        } catch (e: javax.net.ssl.SSLException) {
            CheckResult.Error("SSL 인증서 오류 - 폰 날짜·시간 확인")
        } catch (e: Exception) {
            Log.e(TAG, "checkForUpdate 실패", e)
            CheckResult.Error(e.message ?: e.javaClass.simpleName ?: "알 수 없는 오류")
        }
    }

    /** DownloadManager 로 APK 다운로드 + 완료 시 자동 설치 화면. 실패 시 브라우저 fallback. */
    fun downloadAndInstall(context: Context, info: UpdateInfo) {
        val apkFile = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "Nongdori-v${info.latestVersionCode}.apk"
        )
        if (apkFile.exists()) apkFile.delete()

        val downloadId = try {
            val request = DownloadManager.Request(Uri.parse(info.downloadUrl)).apply {
                setTitle("농돌이 업데이트")
                setDescription("v${info.latestVersionCode} 다운로드 중")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationUri(Uri.fromFile(apkFile))
                setMimeType("application/vnd.android.package-archive")
                setAllowedOverRoaming(true)
                setAllowedNetworkTypes(
                    DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE
                )
            }
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val id = dm.enqueue(request)
            android.widget.Toast.makeText(context, "📥 다운로드 시작… 알림창에서 진행 상황 확인", android.widget.Toast.LENGTH_SHORT).show()
            id
        } catch (e: Exception) {
            Log.e(TAG, "DownloadManager 실패 - 브라우저 fallback", e)
            openInBrowser(context, info.downloadUrl)
            return
        }

        val appCtx = context.applicationContext
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: -1L
                if (id != downloadId) return
                try { appCtx.unregisterReceiver(this) } catch (_: Exception) {}

                val dm = appCtx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val q = DownloadManager.Query().setFilterById(id)
                var status = -1; var reason = -1; var totalBytes = 0L
                dm.query(q).use { c ->
                    if (c != null && c.moveToFirst()) {
                        status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        reason = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                        totalBytes = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    }
                }
                val sizeOk = apkFile.exists() && apkFile.length() > 0 && (totalBytes <= 0 || apkFile.length() >= totalBytes)
                val magicOk = sizeOk && hasApkMagic(apkFile)
                if (status == DownloadManager.STATUS_SUCCESSFUL && magicOk) {
                    android.widget.Toast.makeText(appCtx, "✅ 다운로드 완료 — 설치 화면을 띄웁니다", android.widget.Toast.LENGTH_SHORT).show()
                    triggerInstall(appCtx, apkFile)
                    return
                }
                if (status == DownloadManager.STATUS_SUCCESSFUL && !magicOk) {
                    Log.e(TAG, "다운로드 SUCCESSFUL 이지만 파일 손상")
                    runCatching { apkFile.delete() }
                    openInBrowser(appCtx, info.downloadUrl)
                    return
                }
                Log.e(TAG, "다운로드 실패: reason=$reason status=$status")
                android.widget.Toast.makeText(appCtx, "다운로드 실패 — 브라우저로 다시 시도합니다.", android.widget.Toast.LENGTH_LONG).show()
                openInBrowser(appCtx, info.downloadUrl)
            }
        }
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.applicationContext.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.applicationContext.registerReceiver(receiver, filter)
        }
    }

    private fun openInBrowser(context: Context, url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        } catch (e: Exception) {
            Log.e(TAG, "브라우저 fallback 실패", e)
        }
    }

    private fun triggerInstall(context: Context, apkFile: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, apkFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "설치 화면 띄우기 실패", e)
            android.widget.Toast.makeText(
                context,
                "설치 화면 열기 실패: ${e.message}\n설정 → 앱 → 농돌이 → '알 수 없는 앱 설치' 허용 확인",
                android.widget.Toast.LENGTH_LONG,
            ).show()
        }
    }

    private fun hasApkMagic(apkFile: File): Boolean = try {
        apkFile.inputStream().use { ins ->
            val head = ByteArray(4)
            val read = ins.read(head)
            read == 4 && head[0] == 0x50.toByte() && head[1] == 0x4B.toByte() &&
                head[2] == 0x03.toByte() && head[3] == 0x04.toByte()
        }
    } catch (e: Exception) { false }
}
