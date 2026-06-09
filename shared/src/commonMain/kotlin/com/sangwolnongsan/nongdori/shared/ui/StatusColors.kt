package com.sangwolnongsan.nongdori.shared.ui

import androidx.compose.ui.graphics.Color
import com.sangwolnongsan.nongdori.shared.data.RepairStatus
import com.sangwolnongsan.nongdori.shared.ui.theme.StatusCanceled
import com.sangwolnongsan.nongdori.shared.ui.theme.StatusDispatched
import com.sangwolnongsan.nongdori.shared.ui.theme.StatusDone
import com.sangwolnongsan.nongdori.shared.ui.theme.StatusInProgress
import com.sangwolnongsan.nongdori.shared.ui.theme.StatusReceived
import com.sangwolnongsan.nongdori.shared.ui.theme.StatusScheduled

/** RepairStatus → accent 색. 배지/배차 보드 칼럼 헤더에 사용. */
fun repairStatusColor(status: RepairStatus): Color = when (status) {
    RepairStatus.RECEIVED -> StatusReceived
    RepairStatus.SCHEDULED -> StatusScheduled
    RepairStatus.DISPATCHED -> StatusDispatched
    RepairStatus.IN_PROGRESS -> StatusInProgress
    RepairStatus.DONE -> StatusDone
    RepairStatus.CANCELED -> StatusCanceled
}
