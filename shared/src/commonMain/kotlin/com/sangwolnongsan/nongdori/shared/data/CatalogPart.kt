package com.sangwolnongsan.nongdori.shared.data

import kotlinx.serialization.Serializable

/**
 * 기종별 부품 카탈로그 항목 — dealerships/{dealerCode}/partCatalog/{id}.
 *
 * 재고(Part)와 별개의 **참고용 정비 자료**: 특정 기종(+모델)에 어떤 부품이
 * 들어가는지 / 부품번호가 무엇인지 빠르게 조회. 재고 수량과는 무관.
 */
@Serializable
data class CatalogPart(
    val id: String = "",
    val machineType: MachineType = MachineType.OTHER,
    /** machineType=OTHER 일 때 자유 입력 기종명. */
    val customTypeName: String? = null,
    /** 선택: 특정 모델명 (예: "DK7320"). 비우면 기종 공통. */
    val modelName: String = "",
    val partName: String = "",
    val partNumber: String = "",
    val note: String = "",
    val unitPrice: Int? = null,
) {
    /** 화면 표시용 기종 라벨. customTypeName 우선. */
    val typeDisplay: String
        get() = customTypeName?.takeIf { it.isNotBlank() } ?: machineType.displayName
}
