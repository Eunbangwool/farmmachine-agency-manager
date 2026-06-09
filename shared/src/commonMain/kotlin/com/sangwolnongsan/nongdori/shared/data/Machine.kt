package com.sangwolnongsan.nongdori.shared.data

import kotlinx.serialization.Serializable

/**
 * 기계 종류 — 한국 농가에서 흔히 운용하는 농기계 위주.
 * (farm-machine-manager / 농식이 의 MachineType 을 그대로 이식.)
 */
@Serializable
enum class MachineType(val displayName: String) {
    TRACTOR("트랙터"),
    COMBINE("콤바인"),
    RICE_TRANSPLANTER("이앙기"),
    CULTIVATOR("관리기"),
    ROTAVATOR("로터베이터"),
    PLOW("쟁기"),
    SEEDER("파종기"),
    HARVESTER("수확기"),
    SPRAYER("농약살포기"),
    DRONE("드론"),
    BALER("베일러"),
    LAWN_MOWER("예초기"),
    LOADER("로더"),
    FORKLIFT("지게차"),
    VEHICLE("차량"),
    OTHER("기타"),
}

/**
 * 기계 상태.
 * NORMAL: 정상 / INSPECTION_NEEDED: 점검 필요 / UNDER_REPAIR: 수리 중
 */
@Serializable
enum class MachineStatus(val displayName: String) {
    NORMAL("정상"),
    INSPECTION_NEEDED("점검필요"),
    UNDER_REPAIR("수리중"),
}

/**
 * 고객이 보유한 기계.
 *
 * 대리점은 고객(Customer)별로 기계를 등록·관리하고, 출장(WorkOrder)을 특정 기계에 연결한다.
 * 날짜/시간은 플랫폼 독립을 위해 epoch millis(Long) 로 저장.
 *
 * @param customerId 소유 고객 ID
 * @param name 모델명 (예: "DK7320")
 * @param operatingHours 누적 가동시간(차량은 주행거리 km)
 * @param lastServiceAtMillis 마지막 수리/정비 시각 (이력 denormalize 캐시)
 */
@Serializable
data class Machine(
    val id: String = "",
    val customerId: String = "",
    val name: String = "",
    val manufacturer: String = "",
    val type: MachineType = MachineType.OTHER,
    val customTypeName: String? = null,
    val horsepower: Int? = null,
    val serialNumber: String? = null,
    val registrationNumber: String? = null,
    val year: Int? = null,
    val operatingHours: Double = 0.0,
    val status: MachineStatus = MachineStatus.NORMAL,
    val statusNote: String? = null,
    val purchaseDateMillis: Long? = null,
    val lastServiceAtMillis: Long? = null,
    val photoUrl: String? = null,
    val notes: String? = null,
) {
    /** 화면 표시용 종류 라벨. customTypeName 우선. */
    val typeDisplay: String get() = customTypeName ?: type.displayName

    /** 차량은 가동시간 대신 주행거리(km). */
    val isDistanceBased: Boolean get() = type == MachineType.VEHICLE

    val usageUnitShort: String get() = if (isDistanceBased) "km" else "h"
    val usageUnitLong: String get() = if (isDistanceBased) "km" else "시간"
}
