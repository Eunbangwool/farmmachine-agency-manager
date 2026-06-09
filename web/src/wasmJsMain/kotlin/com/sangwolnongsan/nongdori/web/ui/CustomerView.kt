package com.sangwolnongsan.nongdori.web.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.sangwolnongsan.nongdori.shared.data.RepairStatus
import com.sangwolnongsan.nongdori.shared.data.WorkOrder
import com.sangwolnongsan.nongdori.shared.ui.repairStatusColor
import com.sangwolnongsan.nongdori.shared.ui.theme.PriorityUrgent
import com.sangwolnongsan.nongdori.shared.ui.theme.TextSecondary
import com.sangwolnongsan.nongdori.shared.util.formatDate
import com.sangwolnongsan.nongdori.shared.util.nowMs
import com.sangwolnongsan.nongdori.web.data.WebRepositories

/**
 * 고객 관리 — 목록(검색) / 상세(고객 정보 + 그 고객의 수리이력) / 추가·수정.
 */
@Composable
fun CustomerView(dealerCode: String, workOrders: List<WorkOrder>) {
    var customers by remember { mutableStateOf<List<Customer>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<Customer?>(null) } // 편집/추가 대상

    LaunchedEffect(dealerCode) { WebRepositories.observeCustomers(dealerCode) { customers = it } }

    val editTarget = editing
    if (editTarget != null) {
        CustomerForm(dealerCode, editTarget) { editing = null }
        return
    }

    val selected = customers.firstOrNull { it.id == selectedId }
    if (selected != null) {
        CustomerDetail(
            customer = selected,
            workOrders = workOrders.filter { it.customerId == selected.id },
            onBack = { selectedId = null },
            onEdit = { editing = selected },
            onDelete = { WebRepositories.deleteCustomer(dealerCode, selected.id); selectedId = null },
        )
        return
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("고객 (${customers.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Box(Modifier.weight(1f))
            TextButton(onClick = { editing = Customer(id = WebRepositories.newId()) }) { Text("+ 고객 추가") }
        }
        OutlinedTextField(query, { query = it }, label = { Text("이름/연락처 검색") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
        val filtered = customers.filter {
            query.isBlank() || it.name.contains(query, true) || it.phone.contains(query)
        }.sortedBy { it.name }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (filtered.isEmpty()) Text("고객이 없습니다.", color = TextSecondary)
            filtered.forEach { c ->
                Card(Modifier.fillMaxWidth().clickable { selectedId = c.id }) {
                    Column(Modifier.padding(14.dp)) {
                        Text(c.name.ifBlank { "(이름 없음)" }, fontWeight = FontWeight.SemiBold)
                        val sub = listOfNotNull(c.phone.ifBlank { null }, c.address.ifBlank { null }).joinToString(" · ")
                        if (sub.isNotBlank()) Text(sub, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerDetail(
    customer: Customer,
    workOrders: List<WorkOrder>,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← 목록") }
            Box(Modifier.weight(1f))
            TextButton(onClick = onEdit) { Text("수정") }
            TextButton(onClick = onDelete) { Text("삭제", color = PriorityUrgent) }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(customer.name.ifBlank { "(이름 없음)" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (customer.businessName?.isNotBlank() == true) Text(customer.businessName!!, color = TextSecondary)
                if (customer.phone.isNotBlank()) Text("연락처: ${customer.phone}")
                if (customer.address.isNotBlank()) Text("주소: ${customer.address} ${customer.addressDetail}")
                if (customer.memo.isNotBlank()) Text("메모: ${customer.memo}", color = TextSecondary)
            }
        }
        Text("수리 이력 (${workOrders.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        val sorted = workOrders.sortedByDescending { it.requestedAtMillis }
        if (sorted.isEmpty()) Text("이력이 없습니다.", color = TextSecondary)
        sorted.forEach { wo ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        Text(wo.status.displayName, color = repairStatusColor(wo.status), fontWeight = FontWeight.SemiBold)
                        Box(Modifier.weight(1f))
                        val date = wo.completedAtMillis ?: wo.requestedAtMillis
                        Text(formatDate(date), color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                    if (wo.machineLabel.isNotBlank()) Text("기계: ${wo.machineLabel}", style = MaterialTheme.typography.bodySmall)
                    if (wo.symptom.isNotBlank()) Text(wo.symptom, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    if (wo.status == RepairStatus.DONE && wo.repair.computedTotal > 0)
                        Text("금액: ${wo.repair.computedTotal}원", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun CustomerForm(dealerCode: String, base: Customer, onDone: () -> Unit) {
    var name by remember { mutableStateOf(base.name) }
    var phone by remember { mutableStateOf(base.phone) }
    var address by remember { mutableStateOf(base.address) }
    var addressDetail by remember { mutableStateOf(base.addressDetail) }
    var businessName by remember { mutableStateOf(base.businessName ?: "") }
    var memo by remember { mutableStateOf(base.memo) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (base.name.isBlank()) "고객 추가" else "고객 수정", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        OutlinedTextField(name, { name = it }, label = { Text("이름") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(phone, { phone = it }, label = { Text("연락처") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(businessName, { businessName = it }, label = { Text("상호(선택)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(address, { address = it }, label = { Text("주소") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(addressDetail, { addressDetail = it }, label = { Text("상세 주소") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(memo, { memo = it }, label = { Text("메모") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onDone) { Text("취소") }
            Button(
                onClick = {
                    val now = nowMs()
                    WebRepositories.saveCustomer(
                        dealerCode,
                        base.copy(
                            name = name, phone = phone, address = address, addressDetail = addressDetail,
                            businessName = businessName.ifBlank { null }, memo = memo,
                            createdAtMillis = if (base.createdAtMillis == 0L) now else base.createdAtMillis,
                            updatedAtMillis = now,
                        ),
                    ) { onDone() }
                },
                enabled = name.isNotBlank(),
            ) { Text("저장") }
        }
    }
}
