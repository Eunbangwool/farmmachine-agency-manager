package com.sangwolnongsan.nongdori.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.sangwolnongsan.nongdori.AppContainer
import com.sangwolnongsan.nongdori.data.FirebaseAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Firebase Auth + Credential Manager (Google 로그인) 통합 관리.
 * (농작이 UserManager 에서 entitlement/billing 을 제거한 간소화 버전.)
 *
 * - 앱 시작 시 currentUser 확인 → 자동 로그인 복원
 * - signInWithGoogle(): Credential Manager → Google ID Token → Firebase Auth
 * - signOut(): Firebase + Credential Manager + 대리점 코드 정리
 */
class UserManager(private val context: Context) {

    // Firebase 미설정(google-services.json 없이 빌드)이어도 앱이 튕기지 않도록 가드.
    // FirebaseApp 미초기화 상태에서 getInstance() 를 호출하면 IllegalStateException → 시작 크래시.
    private val auth: FirebaseAuth? =
        if (FirebaseAvailability.isAvailable) FirebaseAuth.getInstance() else null
    private val credentialManager: CredentialManager = CredentialManager.create(context)

    private val _currentUser = MutableStateFlow(auth?.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    init {
        auth?.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
            Log.d(TAG, "Auth 상태 변경: ${firebaseAuth.currentUser?.email}")
        }
    }

    /**
     * Google 로그인. 1) 빠른 one-tap → 2) 실패 시 정식 sign-in flow.
     */
    suspend fun signInWithGoogle(webClientId: String): Result<FirebaseUser> {
        if (auth == null) {
            return Result.failure(IllegalStateException("Firebase 가 설정되지 않았습니다 (google-services.json 필요)"))
        }
        val quickRequest = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(false)
                    .build()
            )
            .build()
        return try {
            processCredentialResponse(credentialManager.getCredential(context, quickRequest))
        } catch (_: NoCredentialException) {
            try {
                val signInRequest = GetCredentialRequest.Builder()
                    .addCredentialOption(GetSignInWithGoogleOption.Builder(webClientId).build())
                    .build()
                processCredentialResponse(credentialManager.getCredential(context, signInRequest))
            } catch (e: Exception) {
                Log.e(TAG, "Sign-in flow 실패", e)
                Result.failure(e)
            }
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Credential Manager 오류", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "로그인 실패", e)
            Result.failure(e)
        }
    }

    private suspend fun processCredentialResponse(
        response: androidx.credentials.GetCredentialResponse
    ): Result<FirebaseUser> {
        val credential = response.credential
        if (credential !is androidx.credentials.CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return Result.failure(Exception("올바르지 않은 credential 타입"))
        }
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        val a = auth ?: return Result.failure(IllegalStateException("Firebase 미설정"))
        val authResult = a.signInWithCredential(firebaseCredential).await()
        return authResult.user?.let {
            Log.d(TAG, "Firebase 로그인 성공: ${it.email}")
            Result.success(it)
        } ?: Result.failure(Exception("Firebase 사용자 정보 없음"))
    }

    /** 로그아웃 — Firebase + Credential Manager + 대리점 코드 정리. */
    suspend fun signOut() {
        auth?.signOut()
        try {
            AppContainer.dealerCodeManager.clearCode()
        } catch (_: Exception) {}
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.w(TAG, "Credential state 정리 실패 (무시): ${e.message}")
        }
    }

    companion object {
        private const val TAG = "UserManager"
    }
}
