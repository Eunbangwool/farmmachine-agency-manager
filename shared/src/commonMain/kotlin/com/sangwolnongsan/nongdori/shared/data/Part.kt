package com.sangwolnongsan.nongdori.shared.data

import kotlinx.serialization.Serializable

/**
 * 재고 부품 — dealerships/{dealerCode}/parts/{partId}.
 *
 * 웹 디스패처의 '재고' 탭에서 등록/수정/재고 조정한다. RepairRecord.partsUsed 의
 * PartUsage(partId=...) 로 수리에 연결할 수 있다(자동 차감은 후속 개선).
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
