package com.sangwolnongsan.nongdori.shared.data

import kotlinx.serialization.Serializable

/**
 * 대리점 고객 (농가/업체).
 *
 * 출장(WorkOrder) 생성 시 고객을 선택하면 이름/연락처/주소가 WorkOrder 에 스냅샷으로
 * 복사되어, 현장 엔지니어가 추가 조회 없이 즉시 고객 정보를 본다.
 *
 * @param lat,lng 주소 좌표 (지도/내비 연동 — 없으면 null)
 */
@Serializable
data class Customer(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val addressDetail: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
    val businessName: String? = null,
    val memo: String = "",
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
)
