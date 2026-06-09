package com.sangwolnongsan.nongdori.shared.data

import kotlinx.serialization.Serializable

/**
 * 출장/수리 진행 상태 — FSM 표준 워크플로.
 *
 * RECEIVED(접수) → SCHEDULED(배정) → DISPATCHED(출동) → IN_PROGRESS(진행중) → DONE(완료)
 * CANCELED(취소) 는 어느 단계에서든 빠질 수 있는 종료 상태.
 *
 * 완료(DONE) 된 WorkOrder 가 곧 "수리 이력" 레코드가 된다(별도 컬렉션 없음).
 */
@Serializable
enum class RepairStatus(val displayName: String) {
    RECEIVED("접수"),
    SCHEDULED("배정"),
    DISPATCHED("출동"),
    IN_PROGRESS("진행중"),
    DONE("완료"),
    CANCELED("취소");

    /** 다음 단계로 전진. DONE/CANCELED 는 종료 상태라 그대로. */
    fun next(): RepairStatus = when (this) {
        RECEIVED -> SCHEDULED
        SCHEDULED -> DISPATCHED
        DISPATCHED -> IN_PROGRESS
        IN_PROGRESS -> DONE
        DONE -> DONE
        CANCELED -> CANCELED
    }

    /** 아직 열려있는(처리 중) 출장인지 — 배차 보드 / 진행 목록 필터용. */
    val isOpen: Boolean get() = this != DONE && this != CANCELED
}

/** 출장 우선순위 — 배차 보드에서 긴급 건 강조. */
@Serializable
enum class Priority(val displayName: String) {
    NORMAL("일반"),
    URGENT("긴급"),
}
