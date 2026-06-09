package com.sangwolnongsan.nongdori.shared.data

/**
 * 상태를 다음 단계로 전진시키고, 해당 단계의 타임스탬프를 stamp 한다.
 * (접수→배정→출동→진행중→완료). DONE/CANCELED 는 변화 없음.
 */
fun WorkOrder.advanced(nowMillis: Long): WorkOrder {
    val next = status.next()
    if (next == status) return this
    return withStatus(next, nowMillis)
}

/** 특정 상태로 전환 + 타임스탬프 stamp. */
fun WorkOrder.withStatus(target: RepairStatus, nowMillis: Long): WorkOrder = copy(
    status = target,
    updatedAtMillis = nowMillis,
    scheduledAtMillis = if (target == RepairStatus.SCHEDULED && scheduledAtMillis == null) nowMillis else scheduledAtMillis,
    dispatchedAtMillis = if (target == RepairStatus.DISPATCHED && dispatchedAtMillis == null) nowMillis else dispatchedAtMillis,
    startedAtMillis = if (target == RepairStatus.IN_PROGRESS && startedAtMillis == null) nowMillis else startedAtMillis,
    arrivedAtMillis = if (target == RepairStatus.IN_PROGRESS && arrivedAtMillis == null) nowMillis else arrivedAtMillis,
    completedAtMillis = if (target == RepairStatus.DONE) nowMillis else completedAtMillis,
)
