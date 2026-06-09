package com.sangwolnongsan.nongdori.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.sangwolnongsan.nongdori.BuildConfig
import com.sangwolnongsan.nongdori.shared.data.DealershipMember
import com.sangwolnongsan.nongdori.shared.data.Role
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * 대리점 멤버십 — Firestore Rules 게이트의 클라이언트 측.
 * (농작이 FarmMembership 을 대리점 도메인으로 이식, tier/soft-delete 제거한 간소화 버전.)
 *
 *   dealerships/{code}                    ← ownerUid 기록
 *   dealerships/{code}/members/{uid}      ← 멤버 doc (role)
 *   dealerships/{code}/invitations/{email}← 이메일 초대
 */
object DealershipMembership {

    private const val TAG = "DealershipMembership"

    private fun db() = FirebaseFirestore.getInstance()

    sealed class CreateResult {
        object Success : CreateResult()
        object NotAuthed : CreateResult()
        object InvalidCode : CreateResult()
        data class AlreadyExists(val code: String) : CreateResult()
        data class Error(val message: String) : CreateResult()
    }

    /** 랜덤 6자리 대리점 코드. */
    fun generateDealerCode(): String = (100000..999999).random().toString()

    fun normalizeEmail(email: String): String = email.trim().lowercase()

    /** 본인이 멤버(역할 무관)인 모든 대리점 코드 — 재설치 후 자동 진입용. */
    suspend fun findMyMemberDealerships(): List<String> {
        val user = FirebaseAuth.getInstance().currentUser ?: return emptyList()
        return try {
            db().collectionGroup("members")
                .whereEqualTo("uid", user.uid)
                .get().await()
                .documents.mapNotNull { it.reference.parent.parent?.id }
        } catch (e: Exception) {
            Log.w(TAG, "findMyMemberDealerships failed", e); emptyList()
        }
    }

    /** 새 대리점 생성 — dealership doc + owner 멤버 doc. */
    suspend fun createNewDealership(code: String, name: String): CreateResult {
        val user = FirebaseAuth.getInstance().currentUser ?: return CreateResult.NotAuthed
        if (code.isBlank()) return CreateResult.InvalidCode
        return try {
            val ref = db().collection("dealerships").document(code)
            if (ref.get().await().exists()) return CreateResult.AlreadyExists(code)
            ref.set(
                mapOf(
                    "ownerUid" to user.uid,
                    "name" to name,
                    "createdAt" to System.currentTimeMillis(),
                )
            ).await()
            ref.collection("members").document(user.uid).set(memberData(user, Role.OWNER)).await()
            Log.d(TAG, "새 대리점 생성: $code")
            CreateResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "createNewDealership failed", e)
            CreateResult.Error(e.message ?: "알 수 없는 오류")
        }
    }

    /** 운영자(ADMIN_EMAIL) 가 OWNER_DEALER_CODE 로 owner 멤버 자기 부트스트랩. */
    suspend fun bootstrapOwnerIfAdmin(): Boolean {
        val user = FirebaseAuth.getInstance().currentUser ?: return false
        val code = BuildConfig.OWNER_DEALER_CODE
        if (code.isBlank() || user.email != BuildConfig.ADMIN_EMAIL) return false
        if (isMember(code)) return true
        return try {
            val ref = db().collection("dealerships").document(code)
            if (!ref.get().await().exists()) {
                ref.set(
                    mapOf(
                        "ownerUid" to user.uid,
                        "name" to "운영자 대리점",
                        "createdAt" to System.currentTimeMillis(),
                    )
                ).await()
            }
            ref.collection("members").document(user.uid).set(memberData(user, Role.OWNER)).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "bootstrapOwnerIfAdmin failed", e); false
        }
    }

    suspend fun isMember(code: String): Boolean {
        val user = FirebaseAuth.getInstance().currentUser ?: return false
        if (code.isBlank()) return false
        return try {
            db().collection("dealerships").document(code)
                .collection("members").document(user.uid).get().await().exists()
        } catch (e: Exception) {
            Log.e(TAG, "isMember failed", e); false
        }
    }

    suspend fun myRole(code: String): Role? {
        val user = FirebaseAuth.getInstance().currentUser ?: return null
        if (code.isBlank()) return null
        return try {
            val doc = db().collection("dealerships").document(code)
                .collection("members").document(user.uid).get().await()
            if (!doc.exists()) null else parseRole(doc.getString("role"))
        } catch (e: Exception) {
            Log.e(TAG, "myRole failed", e); null
        }
    }

    /** 멤버 실시간 구독 (OWNER → ADMIN → MEMBER 정렬). */
    fun observeMembers(code: String): Flow<List<DealershipMember>> = callbackFlow {
        val reg = db().collection("dealerships").document(code)
            .collection("members")
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                val list = snap?.documents?.map { doc ->
                    DealershipMember(
                        uid = doc.getString("uid") ?: doc.id,
                        email = doc.getString("email") ?: "",
                        displayName = doc.getString("displayName") ?: "",
                        role = parseRole(doc.getString("role")),
                        addedAtMillis = doc.getLong("addedAt") ?: 0L,
                    )
                }?.sortedBy { it.role.ordinal } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    /** 이메일 초대 doc 생성 (OWNER/ADMIN). */
    suspend fun inviteByEmail(code: String, email: String): Boolean {
        val user = FirebaseAuth.getInstance().currentUser ?: return false
        val normalized = normalizeEmail(email)
        if (normalized.isBlank() || !email.contains("@")) return false
        return try {
            db().collection("dealerships").document(code)
                .collection("invitations").document(normalized)
                .set(
                    mapOf(
                        "email" to normalized,
                        "invitedBy" to user.uid,
                        "invitedByName" to (user.displayName ?: ""),
                        "invitedAt" to System.currentTimeMillis(),
                    )
                ).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "inviteByEmail failed", e); false
        }
    }

    /** 초대 수락 — 본인 멤버 doc 생성(Rules 가 초대 doc 존재 검증) 후 초대 doc 정리. */
    suspend fun acceptInvitation(code: String): Boolean {
        val user = FirebaseAuth.getInstance().currentUser ?: return false
        val email = normalizeEmail(user.email ?: return false)
        return try {
            db().collection("dealerships").document(code)
                .collection("members").document(user.uid)
                .set(memberData(user, Role.MEMBER)).await()
            try {
                db().collection("dealerships").document(code)
                    .collection("invitations").document(email).delete().await()
            } catch (_: Exception) {}
            true
        } catch (e: Exception) {
            Log.e(TAG, "acceptInvitation failed", e); false
        }
    }

    suspend fun setRole(code: String, targetUid: String, newRole: Role): Boolean {
        val user = FirebaseAuth.getInstance().currentUser ?: return false
        if (targetUid == user.uid || newRole == Role.OWNER) return false
        return try {
            db().collection("dealerships").document(code)
                .collection("members").document(targetUid)
                .update("role", newRole.name.lowercase()).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "setRole failed", e); false
        }
    }

    suspend fun removeMember(code: String, targetUid: String): Boolean {
        val user = FirebaseAuth.getInstance().currentUser ?: return false
        if (targetUid == user.uid) return false
        return try {
            db().collection("dealerships").document(code)
                .collection("members").document(targetUid).delete().await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "removeMember failed", e); false
        }
    }

    private fun parseRole(raw: String?): Role = when (raw?.lowercase()) {
        "owner" -> Role.OWNER
        "admin" -> Role.ADMIN
        else -> Role.MEMBER
    }

    private fun memberData(user: FirebaseUser, role: Role): Map<String, Any?> = mapOf(
        "uid" to user.uid,
        "role" to role.name.lowercase(),
        "addedAt" to System.currentTimeMillis(),
        "displayName" to (user.displayName ?: ""),
        "email" to (user.email ?: ""),
    )
}
