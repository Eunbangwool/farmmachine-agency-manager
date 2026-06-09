package com.sangwolnongsan.nongdori.web.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sangwolnongsan.nongdori.shared.data.RepairStatus
import com.sangwolnongsan.nongdori.shared.data.WorkOrder
import com.sangwolnongsan.nongdori.shared.data.advanced
import com.sangwolnongsan.nongdori.shared.ui.repairStatusColor
import com.sangwolnongsan.nongdori.shared.ui.theme.BorderColor
import com.sangwolnongsan.nongdori.shared.ui.theme.SurfaceSecondary
import com.sangwolnongsan.nongdori.shared.ui.theme.TextSecondary
import com.sangwolnongsan.nongdori.shared.util.nowMs
import com.sangwolnongsan.nongdori.web.data.WebRepositories

/**
 * 디스패처 홈 — 배차 보드 / 출장 생성 탭.
 */
@Composable
fun DispatcherHome(dealerCode: String, onSignOut: () -> Unit) {
    var tab by remember { mutableStateOf(0) }
    var workOrders by remember { mutableStateOf<List<WorkOrder>>(emptyList()) }

    LaunchedEffect(dealerCode) {
        WebRepositories.observeWorkOrders(dealerCode) { workOrders = it }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text("농돌이  ·  $dealerCode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(16.dp))
            Text("디스패처", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onSignOut) { Text("로그아웃", color = TextSecondary) }
        }
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("배차 보드") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("출장 생성") })
            Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("수리 이력") })
        }
        when (tab) {
            0 -> DispatchBoard(dealerCode, workOrders)
            1 -> WorkOrderForm(dealerCode, onCreated = { tab = 0 })
            else -> RepairHistoryView(workOrders)
        }
    }
}

@Composable
private fun DispatchBoard(dealerCode: String, workOrders: List<WorkOrder>) {
    val columns = listOf(
        RepairStatus.RECEIVED, RepairStatus.SCHEDULED, RepairStatus.DISPATCHED,
        RepairStatus.IN_PROGRESS, RepairStatus.DONE,
    )
    if (workOrders.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("아직 출장이 없습니다. ‘출장 생성’ 탭에서 추가하세요.", color = TextSecondary)
        }
        return
    }
    Row(
        Modifier.fillMaxSize().horizontalScroll(rememberScrollState()).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        columns.forEach { status ->
            val items = workOrders.filter { it.status == status }
            Column(Modifier.width(260.dp)) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Box(Modifier.width(10.dp).height(10.dp).clip(RoundedCornerShape(5.dp)).background(repairStatusColor(status)))
                    Spacer(Modifier.width(6.dp))
                    Text("${status.displayName} (${items.size})", fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items.forEach { wo ->
                        WorkOrderCard(wo, onAdvance = {
                            WebRepositories.saveWorkOrder(dealerCode, wo.advanced(nowMs()))
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkOrderCard(wo: WorkOrder, onAdvance: () -> Unit) {
    Card(Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(8.dp))) {
        Column(Modifier.padding(12.dp)) {
            Text(wo.customerName.ifBlank { "(고객 미상)" }, fontWeight = FontWeight.SemiBold)
            if (wo.customerPhone.isNotBlank()) Text(wo.customerPhone, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            if (wo.machineLabel.isNotBlank()) Text("기계: ${wo.machineLabel}", style = MaterialTheme.typography.bodySmall)
            if (wo.symptom.isNotBlank()) Text(wo.symptom, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            if (wo.assignedEngineerName.isNotBlank()) Text("담당: ${wo.assignedEngineerName}", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            if (wo.status.isOpen) {
                OutlinedButton(onClick = onAdvance, modifier = Modifier.padding(top = 8.dp)) {
                    Text("→ ${wo.status.next().displayName}")
                }
            }
        }
    }
}
