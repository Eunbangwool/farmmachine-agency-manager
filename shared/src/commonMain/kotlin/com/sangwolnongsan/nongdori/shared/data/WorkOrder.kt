package com.sangwolnongsan.nongdori.shared.data

import kotlinx.serialization.Serializable

/**
 * 출장 서비스콜(작업 지시) — 대리점 디스패치의 원자 단위.
 *
 * 고객 + 기계 + 배정 엔지니어 + 일정 + 상태 + 수리 결과(RepairRecord) 를 하나로 묶는다.
 * FSM 표준(ServiceTitan/Jobber 등)의 "job/work order" 개념.
 *
 * 고객/기계 정보는 *스냅샷*으로 복사 저장 → 현장 엔지니어 폰에서 추가 Firestore read 없이
 * 즉시 표시. 상태 전환 시 해당 타임스탬프를 stamp 한다.
 *
 * 수리 이력 = status==DONE 인 WorkOrder 들을 customerId/machineId 로 조회.
 */
@Serializable
data class WorkOrder(
    val id: String = "",
    /** 사람이 읽는 출장 번호 (예: "2026-0612-03"). */
    val orderNo: String = "",

    // ── 고객 (스냅샷) ──
    val customerId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val customerAddress: String = "",
    val lat: Double? = null,
    val lng: Double? = null,

    // ── 기계 (스냅샷, 선택) ──
    val machineId: String? = null,
    val machineName: String = "",
    val machineType: MachineType? = null,

    // ── 요청 내용 ──
    val symptom: String = "",
    val priority: Priority = Priority.NORMAL,
    val status: RepairStatus = RepairStatus.RECEIVED,

    // ── 배정 ──
    val assignedEngineerUid: String? = null,
    val assignedEngineerName: String = "",

    // ── 상태별 타임스탬프 (epoch millis) ──
    val requestedAtMillis: Long = 0L,
    val scheduledAtMillis: Long? = null,
    val dispatchedAtMillis: Long? = null,
    val arrivedAtMillis: Long? = null,
    val startedAtMillis: Long? = null,
    val completedAtMillis: Long? = null,

    // ── 수리 결과 (임베드) ──
    val repair: RepairRecord = RepairRecord(),

    val customerSignatureUrl: String? = null,
    val createdByUid: String = "",
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
) {
    val isOpen: Boolean get() = status.isOpen
    val isAssigned: Boolean get() = !assignedEngineerUid.isNullOrBlank()

    /** 목록 표시용 기계 라벨 (없으면 빈 문자열). */
    val machineLabel: String
        get() = when {
            machineName.isNotBlank() -> machineName
            machineType != null -> machineType.displayName
            else -> ""
        }
}
