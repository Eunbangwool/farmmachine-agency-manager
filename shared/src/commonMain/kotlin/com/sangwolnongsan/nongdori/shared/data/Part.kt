package com.sangwolnongsan.nongdori.shared.data

import kotlinx.serialization.Serializable

/**
 * 재고 부품 — PHASE 2 (재고관리) 용 stub.
 *
 * 현재 단계(출장/수리)에서는 사용하지 않는다. WorkOrder 의 RepairRecord.partsUsed 에
 * PartUsage(partId=...) 로 연결될 예정이며, 재고관리 단계에서 stockQty 차감 로직을 붙인다.
 * dealerships/{dealerCode}/parts/{partId} (규칙 미정의 = 현재 접근 차단).
 */
@Serializable
data class Part(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val partNumber: String? = null,
    val stockQty: Double = 0.0,
    val unitPrice: Int? = null,
    val reorderLevel: Double = 0.0,
    val location: String? = null,
) {
    /** 재주문 필요 여부 (재고관리 단계에서 알림에 사용). */
    val needsReorder: Boolean get() = stockQty <= reorderLevel
}
