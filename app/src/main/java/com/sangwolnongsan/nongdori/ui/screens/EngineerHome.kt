package com.sangwolnongsan.nongdori.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sangwolnongsan.nongdori.data.WorkOrderRepository
import com.sangwolnongsan.nongdori.shared.data.PartUsage
import com.sangwolnongsan.nongdori.shared.data.RepairRecord
import com.sangwolnongsan.nongdori.shared.data.RepairStatus
import com.sangwolnongsan.nongdori.shared.data.WorkOrder
import com.sangwolnongsan.nongdori.shared.data.advanced
import com.sangwolnongsan.nongdori.shared.data.withStatus
import com.sangwolnongsan.nongdori.shared.ui.repairStatusColor
import com.sangwolnongsan.nongdori.shared.ui.theme.BorderColor
import com.sangwolnongsan.nongdori.shared.ui.theme.TextSecondary
import kotlinx.coroutines.launch

/**
 * 엔지니어 홈 — 나에게 배정된 출장 목록 → 상세(고객정보/상태 진행/수리 입력).
 */
@Composable
fun EngineerHome(
    dealerCode: String,
    uid: String,
    engineerName: String,
    onSignOut: () -> Unit,
) {
    val repo = remember(dealerCode) { WorkOrderRepository(dealerCode) }
    val flow = remember(dealerCode, uid) { repo.observeAssignedTo(uid) }
    val workOrders by flow.collectAsState(initial = emptyList())
    var selectedId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val selected = workOrders.firstOrNull { it.id == selectedId }
    if (selected != null) {
        WorkOrderDetail(
            wo = selected,
            engineerName = engineerName,
            onBack = { selectedId = null },
            onSave = { updated -> scope.launch { repo.save(updated) } },
        )
        return
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("오늘 출장", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onSignOut) { Text("로그아웃", color = TextSecondary) }
        }
        if (workOrders.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("배정된 출장이 없습니다.", color = TextSecondary)
            }
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                workOrders.forEach { wo -> WorkOrderListItem(wo) { selectedId = wo.id } }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun WorkOrderListItem(wo: WorkOrder, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(10.dp).height(10.dp).clip(RoundedCornerShape(5.dp)).background(repairStatusColor(wo.status)))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(wo.customerName.ifBlank { "(고객 미상)" }, fontWeight = FontWeight.SemiBold)
                if (wo.symptom.isNotBlank()) Text(wo.symptom, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Text(wo.status.displayName, style = MaterialTheme.typography.labelMedium, color = repairStatusColor(wo.status))
        }
    }
}

@Composable
private fun WorkOrderDetail(
    wo: WorkOrder,
    engineerName: String,
    onBack: () -> Unit,
    onSave: (WorkOrder) -> Unit,
) {
    val context = LocalContext.current
    var diagnosis by remember(wo.id) { mutableStateOf(wo.repair.diagnosis) }
    var work by remember(wo.id) { mutableStateOf(wo.repair.workDescription) }
    var parts by remember(wo.id) { mutableStateOf(wo.repair.partsUsed.firstOrNull()?.name ?: "") }
    var laborCost by remember(wo.id) { mutableStateOf(wo.repair.laborCost?.toString() ?: "") }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBack) { Text("← 목록") }

        // 고객 카드
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(wo.customerName.ifBlank { "(고객 미상)" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (wo.customerPhone.isNotBlank()) {
                    TextButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${wo.customerPhone}")))
                    }) { Text("전화: ${wo.customerPhone}") }
                }
                if (wo.customerAddress.isNotBlank()) {
                    TextButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(wo.customerAddress)}")))
                    }) { Text("길찾기: ${wo.customerAddress}") }
                }
                if (wo.machineLabel.isNotBlank()) Text("기계: ${wo.machineLabel}", color = TextSecondary)
            }
        }

        // 증상 + 상태
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("상태: ${wo.status.displayName}", color = repairStatusColor(wo.status), fontWeight = FontWeight.SemiBold)
                if (wo.symptom.isNotBlank()) Text("증상: ${wo.symptom}")
                if (wo.status.isOpen && wo.status != RepairStatus.IN_PROGRESS) {
                    OutlinedButton(onClick = { onSave(wo.advanced(now())) }) {
                        Text("→ ${wo.status.next().displayName} 로 진행")
                    }
                }
            }
        }

        // 수리 입력 (진행중/완료 단계)
        Text("수리 입력", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(diagnosis, { diagnosis = it }, label = { Text("진단") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(work, { work = it }, label = { Text("작업 내용") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(parts, { parts = it }, label = { Text("사용 부품") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(laborCost, { laborCost = it.filter { c -> c.isDigit() } }, label = { Text("공임(원)") }, modifier = Modifier.fillMaxWidth())

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { onSave(wo.copy(repair = buildRepair(wo, diagnosis, work, parts, laborCost, engineerName, inProgress = true), updatedAtMillis = now())) },
                modifier = Modifier.weight(1f),
            ) { Text("임시 저장") }
            Button(
                onClick = {
                    val done = wo.withStatus(RepairStatus.DONE, now())
                        .copy(repair = buildRepair(wo, diagnosis, work, parts, laborCost, engineerName, inProgress = false))
                    onSave(done)
                    onBack()
                },
                modifier = Modifier.weight(1f),
            ) { Text("수리 완료") }
        }
    }
}

private fun buildRepair(
    wo: WorkOrder,
    diagnosis: String,
    work: String,
    parts: String,
    laborCost: String,
    engineerName: String,
    inProgress: Boolean,
): RepairRecord = wo.repair.copy(
    diagnosis = diagnosis,
    workDescription = work,
    partsUsed = if (parts.isBlank()) emptyList() else listOf(PartUsage(name = parts)),
    laborCost = laborCost.toIntOrNull(),
    performedByName = engineerName,
    isInProgress = inProgress,
)

private fun now(): Long = System.currentTimeMillis()
