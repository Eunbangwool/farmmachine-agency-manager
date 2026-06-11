package com.sangwolnongsan.nongdori.web.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sangwolnongsan.nongdori.shared.data.Customer
import com.sangwolnongsan.nongdori.shared.data.DealershipMember
import com.sangwolnongsan.nongdori.shared.data.Priority
import com.sangwolnongsan.nongdori.shared.data.RepairStatus
import com.sangwolnongsan.nongdori.shared.data.WorkOrder
import com.sangwolnongsan.nongdori.shared.data.openCountByEngineer
import com.sangwolnongsan.nongdori.shared.ui.theme.TextSecondary
import com.sangwolnongsan.nongdori.shared.util.generateOrderNo
import com.sangwolnongsan.nongdori.shared.util.nowMs
import com.sangwolnongsan.nongdori.web.data.WebRepositories
import com.sangwolnongsan.nongdori.web.firebase.jsCurrentUserUid

/**
 * 출장 생성 폼 — 고객 정보 + 기계 + 증상 + 우선순위 + 엔지니어 배정.
 * 저장 시 고객 doc + WorkOrder doc 을 만든다(고객 정보는 WO 에 스냅샷).
 * 엔지니어 칩에 미완료 건수를 보여주고 가장 한가한 사람에게 '추천' 표시.
 */
@Composable
fun WorkOrderForm(dealerCode: String, workOrders: List<WorkOrder>, onCreated: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var machine by remember { mutableStateOf("") }
    var symptom by remember { mutableStateOf("") }
    var urgent by remember { mutableStateOf(false) }
    var engineerUid by remember { mutableStateOf<String?>(null) }
    var engineerName by remember { mutableStateOf("") }
    var members by remember { mutableStateOf<List<DealershipMember>>(emptyList()) }
    var customers by remember { mutableStateOf<List<Customer>>(emptyList()) }
    var selectedCustomerId by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(dealerCode) {
        WebRepositories.observeMembers(dealerCode) { members = it }
        WebRepositories.observeCustomers(dealerCode) { customers = it }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp).widthIn(max = 560.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("새 출장", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Text("고객", color = TextSecondary, style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            name,
            { name = it; selectedCustomerId = null }, // 이름 직접 수정 시 기존 고객 선택 해제
            label = { Text("고객 이름") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        // 기존 고객 매칭 제안 (이름 입력 중, 미선택 상태일 때).
        if (selectedCustomerId == null && name.isNotBlank()) {
            val matches = customers.filter { it.name.contains(name, true) || it.phone.contains(name) }.take(5)
            matches.forEach { c ->
                Text(
                    "기존 고객 선택: ${c.name}${if (c.phone.isNotBlank()) " · ${c.phone}" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth().clickable {
                        selectedCustomerId = c.id; name = c.name; phone = c.phone; address = c.address
                    }.padding(vertical = 4.dp),
                )
            }
        }
        OutlinedTextField(phone, { phone = it }, label = { Text("연락처") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(address, { address = it }, label = { Text("주소") }, singleLine = true, modifier = Modifier.fillMaxWidth())

        Text("기계 / 증상", color = TextSecondary, style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(machine, { machine = it }, label = { Text("기계 (모델/종류)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(symptom, { symptom = it }, label = { Text("고장 증상") }, modifier = Modifier.fillMaxWidth())

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("우선순위:")
            FilterChip(selected = !urgent, onClick = { urgent = false }, label = { Text(Priority.NORMAL.displayName) })
            FilterChip(selected = urgent, onClick = { urgent = true }, label = { Text(Priority.URGENT.displayName) })
        }

        if (members.isNotEmpty()) {
            Text("담당 엔지니어 (선택)", color = TextSecondary, style = MaterialTheme.typography.labelLarge)
            // 미완료(열린) 출장 수가 가장 적은 멤버 = 추천 (동률이면 먼저 등록된 멤버).
            val openCounts = openCountByEngineer(workOrders)
            val recommendedUid = if (members.size > 1) members.minByOrNull { openCounts[it.uid] ?: 0 }?.uid else null
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                members.forEach { m ->
                    val count = openCounts[m.uid] ?: 0
                    val label = buildString {
                        append(m.displayName.ifBlank { m.email })
                        append(" · ").append(count).append("건")
                        if (m.uid == recommendedUid) append(" ★추천")
                    }
                    FilterChip(
                        selected = engineerUid == m.uid,
                        onClick = {
                            if (engineerUid == m.uid) { engineerUid = null; engineerName = "" }
                            else { engineerUid = m.uid; engineerName = m.displayName.ifBlank { m.email } }
                        },
                        label = { Text(label) },
                        modifier = Modifier.selectable(selected = engineerUid == m.uid, onClick = {}),
                    )
                }
            }
        }

        Button(
            onClick = {
                saving = true
                val now = nowMs()
                // 기존 고객 선택 시 그 id 재사용(중복 방지), 아니면 새 고객 생성.
                val customerId = selectedCustomerId ?: WebRepositories.newId()
                if (selectedCustomerId == null) {
                    WebRepositories.saveCustomer(
                        dealerCode,
                        Customer(id = customerId, name = name, phone = phone, address = address, createdAtMillis = now, updatedAtMillis = now),
                    )
                }
                val woId = WebRepositories.newId()
                val assigned = engineerUid != null
                WebRepositories.saveWorkOrder(
                    dealerCode,
                    WorkOrder(
                        id = woId,
                        orderNo = generateOrderNo(now),
                        customerId = customerId,
                        customerName = name,
                        customerPhone = phone,
                        customerAddress = address,
                        machineName = machine,
                        symptom = symptom,
                        priority = if (urgent) Priority.URGENT else Priority.NORMAL,
                        status = if (assigned) RepairStatus.SCHEDULED else RepairStatus.RECEIVED,
                        assignedEngineerUid = engineerUid,
                        assignedEngineerName = engineerName,
                        requestedAtMillis = now,
                        scheduledAtMillis = if (assigned) now else null,
                        createdByUid = jsCurrentUserUid(),
                        createdAtMillis = now,
                        updatedAtMillis = now,
                    ),
                ) { saving = false; if (it) onCreated() }
            },
            enabled = !saving && name.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (saving) "저장 중…" else "출장 생성") }
    }
}
