package com.sangwolnongsan.nongdori.shared.data

import kotlinx.serialization.Serializable

/**
 * 대리점 멤버 역할.
 * - OWNER: 대표 — 모든 권한 + 대리점 삭제/소유권 이전
 * - ADMIN: 사무실 디스패처 — 출장 생성/배정, 멤버 관리, 고객 관리
 * - MEMBER: 현장 엔지니어 — 배정된 출장 처리, 수리 입력
 */
@Serializable
enum class Role(val displayName: String) {
    OWNER("대표"),
    ADMIN("관리자"),
    MEMBER("엔지니어");

    val canManageMembers: Boolean get() = this == OWNER || this == ADMIN
    val canDispatch: Boolean get() = this == OWNER || this == ADMIN
}

/** 엔지니어 부가 프로필 (연락처/전문 분야). */
@Serializable
data class EngineerProfile(
    val phone: String = "",
    val specialties: List<String> = emptyList(),
)

/**
 * 대리점 멤버 — dealerships/{dealerCode}/members/{uid}.
 */
@Serializable
data class DealershipMember(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: Role = Role.MEMBER,
    val addedAtMillis: Long = 0L,
    val engineerProfile: EngineerProfile? = null,
) {
    val isEngineer: Boolean get() = role == Role.MEMBER
}
