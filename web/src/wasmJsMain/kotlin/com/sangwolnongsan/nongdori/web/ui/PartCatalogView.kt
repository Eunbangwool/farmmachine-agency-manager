package com.sangwolnongsan.nongdori.web.ui

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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.sangwolnongsan.nongdori.shared.data.CatalogPart
import com.sangwolnongsan.nongdori.shared.data.MachineType
import com.sangwolnongsan.nongdori.shared.ui.theme.PriorityUrgent
import com.sangwolnongsan.nongdori.shared.ui.theme.TextSecondary
import com.sangwolnongsan.nongdori.web.data.WebRepositories

/**
 * 기종별 부품 카탈로그 — 재고와 별개의 참고용 정비 자료.
 * 기종(MachineType)별로 묶어 표시. 항목: 모델/부품명/부품번호/단가/비고.
 */
@Composable
fun PartCatalogView(dealerCode: String) {
    var entries by remember { mutableStateOf<List<CatalogPart>>(emptyList()) }
    var adding by remember { mutableStateOf(false) }
    LaunchedEffect(dealerCode) { WebRepositories.observePartCatalog(dealerCode) { entries = it } }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("기종별 부품정보 (${entries.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Box(Modifier.weight(1f))
            TextButton(onClick = { adding = !adding }) { Text(if (adding) "닫기" else "+ 부품 추가") }
        }

        if (adding) {
            CatalogPartForm(dealerCode) { adding = false }
        }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (entries.isEmpty() && !adding) {
                Text("등록된 부품정보가 없습니다. ‘+ 부품 추가’ 로 등록하세요.", color = TextSecondary)
            }
            // 기종별 그룹 (기종 displayName 순) → 각 그룹 안에서 모델/부품명 순
            entries.groupBy { it.typeDisplay }
                .toList()
                .sortedBy { it.first }
                .forEach { (typeLabel, list) ->
                    Text(typeLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp))
                    list.sortedWith(compareBy({ it.modelName }, { it.partName }))
                        .forEach { entry -> CatalogPartRow(dealerCode, entry) }
                }
        }
    }
}

@Composable
private fun CatalogPartRow(dealerCode: String, entry: CatalogPart) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                val title = buildString {
                    if (entry.modelName.isNotBlank()) { append(entry.modelName); append(" · ") }
                    append(entry.partName.ifBlank { "(부품명 없음)" })
                }
                Text(title, fontWeight = FontWeight.SemiBold)
                val sub = buildString {
                    if (entry.partNumber.isNotBlank()) append("부품번호 ${entry.partNumber}")
                    entry.unitPrice?.let { if (isNotEmpty()) append(" · "); append("단가 ${it}원") }
                }
                if (sub.isNotBlank()) Text(sub, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                if (entry.note.isNotBlank()) Text(entry.note, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = { WebRepositories.deleteCatalogPart(dealerCode, entry.id) }) { Text("삭제", color = PriorityUrgent) }
        }
    }
}

@Composable
private fun CatalogPartForm(dealerCode: String, onDone: () -> Unit) {
    var type by remember { mutableStateOf(MachineType.TRACTOR) }
    var typeMenu by remember { mutableStateOf(false) }
    var customType by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var partName by remember { mutableStateOf("") }
    var partNumber by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // 기종 선택
            Box {
                OutlinedButton(onClick = { typeMenu = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("기종: ${type.displayName}")
                }
                DropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                    MachineType.entries.forEach { mt ->
                        DropdownMenuItem(text = { Text(mt.displayName) }, onClick = { type = mt; typeMenu = false })
                    }
                }
            }
            if (type == MachineType.OTHER) {
                OutlinedTextField(customType, { customType = it }, label = { Text("기종명 직접 입력") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(model, { model = it }, label = { Text("모델 (선택)") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(partName, { partName = it }, label = { Text("부품명") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(partNumber, { partNumber = it }, label = { Text("부품번호") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(price, { price = it.filter { c -> c.isDigit() } }, label = { Text("단가(원)") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(note, { note = it }, label = { Text("비고") }, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = {
                    WebRepositories.saveCatalogPart(
                        dealerCode,
                        CatalogPart(
                            id = WebRepositories.newId(),
                            machineType = type,
                            customTypeName = if (type == MachineType.OTHER) customType.ifBlank { null } else null,
                            modelName = model,
                            partName = partName,
                            partNumber = partNumber,
                            note = note,
                            unitPrice = price.toIntOrNull(),
                        ),
                    ) { onDone() }
                },
                enabled = partName.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("저장") }
        }
    }
}
