package com.sangwolnongsan.nongdori.shared.data

import kotlinx.serialization.Serializable

/**
 * 정비/수리 종류. (농식이의 MaintenanceType 이식.)
 */
@Serializable
enum class MaintenanceType(val displayName: String) {
    REGULAR_CHECK("정기점검"),
    REPAIR("수리"),
    CONSUMABLE_REPLACE("소모품 교체"),
    INSPECTION("검사"),
    OTHER("기타"),
}

/**
 * 수리에 사용된 부품 라인 아이템.
 *
 * @param partId 재고(Part) 연결 FK — PHASE 2(재고관리)에서 재고 차감에 사용.
 *               지금은 자유 입력(name)만 받고 partId 는 null 로 둔다.
 */
@Serializable
data class PartUsage(
    val partId: String? = null,
    val name: String = "",
    val qty: Double = 1.0,
    val unitPrice: Int? = null,
    val lineTotal: Int? = null,
)

/**
 * 수리 결과 기록 — WorkOrder 안에 임베드된다.
 *
 * 완료된 WorkOrder 의 repair 가 곧 "수리 이력"의 상세 내역(내역·금액).
 *
 * @param isInProgress 수리 진행 중 여부 (false + status DONE → 이력 확정)
 */
@Serializable
data class RepairRecord(
    val type: MaintenanceType = MaintenanceType.REPAIR,
    val diagnosis: String = "",
    val workDescription: String = "",
    val partsUsed: List<PartUsage> = emptyList(),
    val laborHours: Double? = null,
    val laborCost: Int? = null,
    val partsCost: Int? = null,
    val totalCost: Int? = null,
    val operatingHoursAtService: Double? = null,
    val photoUrls: List<String> = emptyList(),
    val performedByUid: String = "",
    val performedByName: String = "",
    val isInProgress: Boolean = true,
) {
    /** 부품비 합계(라인 합 우선, 없으면 partsCost). */
    val computedPartsCost: Int
        get() = partsUsed.sumOf { it.lineTotal ?: ((it.unitPrice ?: 0) * it.qty).toInt() }
            .takeIf { it > 0 } ?: (partsCost ?: 0)

    /** 총액 = 공임 + 부품비 (명시 totalCost 우선). */
    val computedTotal: Int
        get() = totalCost ?: ((laborCost ?: 0) + computedPartsCost)
}
