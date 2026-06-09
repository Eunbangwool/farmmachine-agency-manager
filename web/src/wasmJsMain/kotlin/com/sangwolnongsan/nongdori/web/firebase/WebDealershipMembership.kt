package com.sangwolnongsan.nongdori.web.firebase

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.coroutines.resume

/**
 * 대리점 멤버십 — 웹(디스패처) 측. Android DealershipMembership 의 대응.
 * window.nongdori Firestore 헬퍼를 suspend 로 감싼다.
 */
object WebDealershipMembership {

    private val json = Json { ignoreUnknownKeys = true }

    // ── JS 콜백 → suspend 래퍼 ──
    private suspend fun getDoc(path: String): String =
        suspendCancellableCoroutine { cont -> jsGetDoc(path) { cont.resume(it) } }

    private suspend fun setDoc(path: String, jsonData: String): Boolean =
        suspendCancellableCoroutine { cont -> jsSetDoc(path, jsonData) { cont.resume(it == "ok") } }

    private suspend fun findMyDealershipsRaw(): String =
        suspendCancellableCoroutine { cont -> jsFindMyDealerships { cont.resume(it) } }

    /** 내가 멤버인 대리점 코드 목록. */
    suspend fun findMyMemberDealerships(): List<String> = try {
        val raw = findMyDealershipsRaw()
        if (raw.isBlank()) emptyList()
        else (json.parseToJsonElement(raw) as? JsonArray)?.mapNotNull {
            (it as? JsonPrimitive)?.content
        } ?: emptyList()
    } catch (_: Throwable) {
        emptyList()
    }

    fun generateDealerCode(): String = (100000..999999).random().toString()

    /** 새 대리점 생성 — dealership doc + owner 멤버 doc. */
    suspend fun createNewDealership(code: String, name: String): Boolean {
        val uid = jsCurrentUserUid()
        if (uid.isBlank() || code.isBlank()) return false
        // 이미 존재하면 중단
        if (getDoc("dealerships/$code").isNotBlank()) return false
        val dealerOk = setDoc(
            "dealerships/$code",
            json.encodeToString(JsonObject.serializer(), buildJsonObject {
                put("ownerUid", JsonPrimitive(uid))
                put("name", JsonPrimitive(name))
                put("createdAt", JsonPrimitive(nowMs()))
            })
        )
        if (!dealerOk) return false
        return setDoc("dealerships/$code/members/$uid", memberJson(uid, "owner"))
    }

    /** 초대 수락 / 가입 — 본인 멤버 doc 생성(Rules 가 초대 doc 검증). */
    suspend fun acceptInvitation(code: String): Boolean {
        val uid = jsCurrentUserUid()
        if (uid.isBlank() || code.isBlank()) return false
        return setDoc("dealerships/$code/members/$uid", memberJson(uid, "member"))
    }

    suspend fun isMember(code: String): Boolean {
        val uid = jsCurrentUserUid()
        if (uid.isBlank() || code.isBlank()) return false
        return getDoc("dealerships/$code/members/$uid").isNotBlank()
    }

    private fun memberJson(uid: String, role: String): String =
        json.encodeToString(JsonObject.serializer(), buildJsonObject {
            put("uid", JsonPrimitive(uid))
            put("role", JsonPrimitive(role))
            put("displayName", JsonPrimitive(jsCurrentUserDisplayName()))
            put("email", JsonPrimitive(jsCurrentUserEmail()))
            put("addedAt", JsonPrimitive(nowMs()))
        })

    private fun nowMs(): Long = com.sangwolnongsan.nongdori.shared.util.nowMs()
}
