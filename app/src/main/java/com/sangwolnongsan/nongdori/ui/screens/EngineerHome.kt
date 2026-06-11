package com.sangwolnongsan.nongdori.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.sangwolnongsan.nongdori.data.DealershipMembership
import com.sangwolnongsan.nongdori.data.FirestoreMappers
import com.sangwolnongsan.nongdori.data.WorkOrderRepository
import com.sangwolnongsan.nongdori.shared.data.Customer
import com.sangwolnongsan.nongdori.shared.data.DealershipMember
import com.sangwolnongsan.nongdori.shared.data.PartUsage
import com.sangwolnongsan.nongdori.shared.data.Priority
import com.sangwolnongsan.nongdori.shared.data.RepairRecord
import com.sangwolnongsan.nongdori.shared.data.RepairStatus
import com.sangwolnongsan.nongdori.shared.data.WorkOrder
import com.sangwolnongsan.nongdori.shared.data.advanced
import com.sangwolnongsan.nongdori.shared.data.openCountByEngineer
import com.sangwolnongsan.nongdori.shared.data.withStatus
import com.sangwolnongsan.nongdori.shared.ui.repairStatusColor
import com.sangwolnongsan.nongdori.shared.ui.theme.PriorityUrgent
import com.sangwolnongsan.nongdori.shared.ui.theme.TextSecondary
import com.sangwolnongsan.nongdori.shared.util.formatDate
import com.sangwolnongsan.nongdori.shared.util.generateOrderNo
import com.sangwolnongsan.nongdori.update.AppUpdateChecker
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * 홈 — OWNER/ADMIN(매니저)은 전체 출장 + '출장 생성'(디스패처 역할), 엔지니어는
 * 나에게 배정된 출장만. 상세에서 고객정보/상태 진행/수리 입력.
 */
@Composable
fun EngineerHome(
    dealerCode: String,
    uid: String,
    engineerName: String,
    onSignOut: () -> Unit,
) {
    val repo = remember(dealerCode) { WorkOrderRepository(dealerCode) }
    // 역할 확인 — OWNER/ADMIN 이면 디스패처(전체 출장 + 생성) 모드.
    var isManager by remember(dealerCode) { mutableStateOf(false) }
    LaunchedEffect(dealerCode, uid) {
        isManager = runCatching { DealershipMembership.myRole(dealerCode)?.canDispatch == true }.getOrDefault(false)
    }
    val flow = remember(dealerCode, uid, isManager) {
        if (isManager) repo.observeAll() else repo.observeAssignedTo(uid)
    }
    val workOrders by flow.collectAsState(initial = emptyList())
    var selectedId by remember { mutableStateOf<String?>(null) }
    var creating by remember { mutableStateOf(false) }
    var showCatalog by remember { mutableStateOf(false) }
    // 앱 업데이트: null=닫힘, true=수동확인, false=자동(새 버전 발견 시 자동 표시)
    var updateMode by remember { mutableStateOf<Boolean?>(null) }
    val scope = rememberCoroutineScope()

    var preloadedUpdate by remember { mutableStateOf<AppUpdateChecker.UpdateInfo?>(null) }
    // 앱 시작 시 자동 업데이트 체크 — 새 버전이 있을 때만 다이얼로그 표시.
    LaunchedEffect(Unit) {
        val r = runCatching { AppUpdateChecker.checkForUpdate() }.getOrNull()
        if (r is AppUpdateChecker.CheckResult.UpdateAvailable && updateMode == null) {
            preloadedUpdate = r.info
            updateMode = false
        }
    }
    updateMode?.let { manual ->
        // 자동 표시일 땐 방금 체크한 결과 재사용 — 중복 네트워크 호출 방지.
        UpdateDialog(manual = manual, preloaded = if (manual) null else preloadedUpdate, onDismiss = { updateMode = null })
    }

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
    if (creating) {
        CreateWorkOrderScreen(dealerCode, uid, engineerName, workOrders, onDone = { creating = false })
        return
    }
    if (showCatalog) {
        PartCatalogScreen(dealerCode, isManager = isManager, onBack = { showCatalog = false })
        return
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(if (isManager) "출장 관리" else "오늘 출장", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            if (isManager) {
                TextButton(onClick = { creating = true }) { Text("+ 출장 생성") }
            }
            TextButton(onClick = { showCatalog = true }) { Text("부품정보") }
            TextButton(onClick = { updateMode = true }) { Text("업데이트") }
            TextButton(onClick = onSignOut) { Text("로그아웃", color = TextSecondary) }
        }
        if (workOrders.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (isManager) "출장이 없습니다. ‘+ 출장 생성’ 으로 추가하세요." else "배정된 출장이 없습니다.",
                    color = TextSecondary,
                )
            }
        } else {
            // 진행 중(긴급 우선 → 최신 요청 순) / 완료 를 섹션으로 분리.
            val open = workOrders.filter { it.status.isOpen }.sortedWith(
                compareByDescending<WorkOrder> { it.priority == Priority.URGENT }
                    .thenByDescending { it.requestedAtMillis }
            )
            val closed = workOrders.filter { !it.status.isOpen }
                .sortedByDescending { it.completedAtMillis ?: it.updatedAtMillis }
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (open.isNotEmpty()) {
                    Text("진행 중 (${open.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextSecondary)
                    open.forEach { wo -> WorkOrderListItem(wo) { selectedId = wo.id } }
                }
                if (closed.isNotEmpty()) {
                    Text("완료·취소 (${closed.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.padding(top = 6.dp))
                    closed.forEach { wo -> WorkOrderListItem(wo) { selectedId = wo.id } }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

/**
 * 출장 생성 (매니저 전용) — 고객/기계/증상 + 담당 엔지니어 배정.
 * 고객 doc + 출장 doc 을 함께 생성. 배정 기본값은 본인(혼자 운영 대응).
 */
@Composable
private fun CreateWorkOrderScreen(
    dealerCode: String,
    myUid: String,
    myName: String,
    workOrders: List<WorkOrder>,
    onDone: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var machine by remember { mutableStateOf("") }
    var symptom by remember { mutableStateOf("") }
    var urgent by remember { mutableStateOf(false) }
    var members by remember { mutableStateOf<List<DealershipMember>>(emptyList()) }
    var assignUid by remember { mutableStateOf(myUid) }
    var assignName by remember { mutableStateOf(myName) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    // 기존 고객 매칭 — 같은 고객을 매번 새로 만들지 않도록 (웹 폼과 동일 동작).
    var customers by remember { mutableStateOf<List<Customer>>(emptyList()) }
    var selectedCustomerId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(dealerCode) {
        customers = runCatching {
            FirebaseFirestore.getInstance().collection("dealerships").document(dealerCode)
                .collection("customers").get().await()
                .documents.map { FirestoreMappers.customerFromDoc(it) }
        }.getOrDefault(emptyList())
    }
    LaunchedEffect(dealerCode) {
        DealershipMembership.observeMembers(dealerCode).collect { members = it }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onDone) { Text("← 취소") }
        Text("새 출장", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Text("고객", color = TextSecondary, style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(name, { name = it; selectedCustomerId = null }, label = { Text("고객 이름") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        if (selectedCustomerId == null && name.isNotBlank()) {
            customers.filter { it.name.contains(name, true) || it.phone.contains(name) }.take(5).forEach { c ->
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

        Text("담당 엔지니어", color = TextSecondary, style = MaterialTheme.typography.labelLarge)
        // 미완료(열린) 출장 수가 가장 적은 멤버 = 추천 (동률이면 먼저 등록된 멤버).
        val openCounts = openCountByEngineer(workOrders)
        val recommendedUid = if (members.size > 1) members.minByOrNull { openCounts[it.uid] ?: 0 }?.uid else null
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            members.forEach { m ->
                val label = buildString {
                    append(m.displayName.ifBlank { m.email }.ifBlank { m.uid })
                    if (m.uid == myUid) append(" (나)")
                    append(" · ").append(openCounts[m.uid] ?: 0).append("건")
                    if (m.uid == recommendedUid) append(" ★추천")
                }
                FilterChip(
                    selected = assignUid == m.uid,
                    onClick = { assignUid = m.uid; assignName = m.displayName.ifBlank { m.email } },
                    label = { Text(label) },
                )
            }
        }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

        Button(
            onClick = {
                saving = true; error = null
                scope.launch {
                    val ok = runCatching {
                        val now = now()
                        // 기존 고객을 선택했으면 재사용 (중복 고객 doc 생성 방지).
                        val customerId = selectedCustomerId ?: UUID.randomUUID().toString()
                        if (selectedCustomerId == null) {
                            FirebaseFirestore.getInstance()
                                .collection("dealerships").document(dealerCode)
                                .collection("customers").document(customerId).set(
                                    mapOf(
                                        "id" to customerId, "name" to name, "phone" to phone,
                                        "address" to address, "createdAtMillis" to now, "updatedAtMillis" to now,
                                    )
                                ).await()
                        }
                        val wo = WorkOrder(
                            id = UUID.randomUUID().toString(),
                            orderNo = generateOrderNo(now),
                            customerId = customerId,
                            customerName = name, customerPhone = phone, customerAddress = address,
                            machineName = machine, symptom = symptom,
                            priority = if (urgent) Priority.URGENT else Priority.NORMAL,
                            status = RepairStatus.SCHEDULED,
                            assignedEngineerUid = assignUid, assignedEngineerName = assignName,
                            requestedAtMillis = now, scheduledAtMillis = now,
                            createdByUid = myUid, createdAtMillis = now, updatedAtMillis = now,
                        )
                        WorkOrderRepository(dealerCode).save(wo)
                    }
                    saving = false
                    if (ok.isSuccess) onDone() else error = "생성 실패: ${ok.exceptionOrNull()?.message}"
                }
            },
            enabled = !saving && name.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (saving) "저장 중…" else "출장 생성") }
    }
}

@Composable
private fun WorkOrderListItem(wo: WorkOrder, onClick: () -> Unit) {
    val urgent = wo.priority == Priority.URGENT && wo.status.isOpen
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(10.dp).height(10.dp).clip(RoundedCornerShape(5.dp)).background(repairStatusColor(wo.status)))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (urgent) {
                        Text("긴급", color = PriorityUrgent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(wo.customerName.ifBlank { "(고객 미상)" }, fontWeight = FontWeight.SemiBold)
                }
                if (wo.symptom.isNotBlank()) Text(wo.symptom, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                val sub = buildString {
                    val d = formatDate(if (wo.status.isOpen) wo.requestedAtMillis else (wo.completedAtMillis ?: wo.requestedAtMillis))
                    if (d.isNotBlank()) append(d)
                    if (wo.customerAddress.isNotBlank()) { if (isNotEmpty()) append(" · "); append(wo.customerAddress) }
                }
                if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
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
    // 위치 권한 부여 후 출동(DISPATCHED) 전환 + 위치 공유 서비스 시작.
    val locationPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) com.sangwolnongsan.nongdori.service.LocationTrackingService.start(context)
        onSave(wo.advanced(now()))
    }
    var diagnosis by remember(wo.id) { mutableStateOf(wo.repair.diagnosis) }
    var work by remember(wo.id) { mutableStateOf(wo.repair.workDescription) }
    var parts by remember(wo.id) { mutableStateOf(wo.repair.partsUsed.firstOrNull()?.name ?: "") }
    var laborCost by remember(wo.id) { mutableStateOf(wo.repair.laborCost?.toString() ?: "") }
    var partsCost by remember(wo.id) { mutableStateOf(wo.repair.partsCost?.toString() ?: "") }

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
                    OutlinedButton(onClick = {
                        if (wo.status.next() == RepairStatus.DISPATCHED) {
                            // 출동 전환: 위치 권한 요청 → 위치 공유 서비스 시작.
                            locationPermLauncher.launch(
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                            )
                        } else {
                            onSave(wo.advanced(now()))
                        }
                    }) {
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(laborCost, { laborCost = it.filter { c -> c.isDigit() } }, label = { Text("공임(원)") }, modifier = Modifier.weight(1f))
            OutlinedTextField(partsCost, { partsCost = it.filter { c -> c.isDigit() } }, label = { Text("부품비(원)") }, modifier = Modifier.weight(1f))
        }
        val totalPreview = (laborCost.toIntOrNull() ?: 0) + (partsCost.toIntOrNull() ?: 0)
        if (totalPreview > 0) Text("합계: ${totalPreview}원", fontWeight = FontWeight.SemiBold)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { onSave(wo.copy(repair = buildRepair(wo, diagnosis, work, parts, laborCost, partsCost, engineerName, inProgress = true), updatedAtMillis = now())) },
                modifier = Modifier.weight(1f),
            ) { Text("임시 저장") }
            Button(
                onClick = {
                    com.sangwolnongsan.nongdori.service.LocationTrackingService.stop(context)
                    val done = wo.withStatus(RepairStatus.DONE, now())
                        .copy(repair = buildRepair(wo, diagnosis, work, parts, laborCost, partsCost, engineerName, inProgress = false))
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
    partsCost: String,
    engineerName: String,
    inProgress: Boolean,
): RepairRecord = wo.repair.copy(
    diagnosis = diagnosis,
    workDescription = work,
    partsUsed = if (parts.isBlank()) emptyList() else listOf(PartUsage(name = parts)),
    laborCost = laborCost.toIntOrNull(),
    partsCost = partsCost.toIntOrNull(),
    performedByName = engineerName,
    isInProgress = inProgress,
)

private fun now(): Long = System.currentTimeMillis()
