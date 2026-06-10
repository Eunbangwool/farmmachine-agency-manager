package com.sangwolnongsan.nongdori.ui.screens

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sangwolnongsan.nongdori.data.CatalogPartRepository
import com.sangwolnongsan.nongdori.shared.data.CatalogPart
import com.sangwolnongsan.nongdori.shared.data.MachineType
import com.sangwolnongsan.nongdori.shared.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 기종별 부품정보 (참고용 카탈로그). 기종별로 묶어 표시. 멤버는 추가 가능,
 * 삭제는 매니저만(규칙과 동일하게 UI 도 제한).
 */
@Composable
fun PartCatalogScreen(
    dealerCode: String,
    isManager: Boolean,
    onBack: () -> Unit,
) {
    val repo = remember(dealerCode) { CatalogPartRepository(dealerCode) }
    val entries by remember(dealerCode) { repo.observeAll() }.collectAsState(initial = emptyList())
    var adding by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← 뒤로") }
            Text("기종별 부품정보 (${entries.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Box(Modifier.weight(1f))
            TextButton(onClick = { adding = !adding }) { Text(if (adding) "닫기" else "+ 추가") }
        }

        if (adding) {
            CatalogForm(onSave = { entry ->
                scope.launch { repo.save(entry); adding = false }
            })
        }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (entries.isEmpty() && !adding) {
                Text("등록된 부품정보가 없습니다. ‘+ 추가’ 로 등록하세요.", color = TextSecondary)
            }
            entries.groupBy { it.typeDisplay }
                .toList()
                .sortedBy { it.first }
                .forEach { (typeLabel, list) ->
                    Text(typeLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp))
                    list.sortedWith(compareBy({ it.modelName }, { it.partName })).forEach { e ->
                        CatalogRow(e, showDelete = isManager, onDelete = { scope.launch { repo.delete(e.id) } })
                    }
                }
        }
    }
}

@Composable
private fun CatalogRow(e: CatalogPart, showDelete: Boolean, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                val title = buildString {
                    if (e.modelName.isNotBlank()) { append(e.modelName); append(" · ") }
                    append(e.partName.ifBlank { "(부품명 없음)" })
                }
                Text(title, fontWeight = FontWeight.SemiBold)
                val sub = buildString {
                    if (e.partNumber.isNotBlank()) append("부품번호 ${e.partNumber}")
                    e.unitPrice?.let { if (isNotEmpty()) append(" · "); append("단가 ${it}원") }
                }
                if (sub.isNotBlank()) Text(sub, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                if (e.note.isNotBlank()) Text(e.note, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            if (showDelete) TextButton(onClick = onDelete) { Text("삭제", color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun CatalogForm(onSave: (CatalogPart) -> Unit) {
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
                    onSave(
                        CatalogPart(
                            id = UUID.randomUUID().toString(),
                            machineType = type,
                            customTypeName = if (type == MachineType.OTHER) customType.ifBlank { null } else null,
                            modelName = model,
                            partName = partName,
                            partNumber = partNumber,
                            note = note,
                            unitPrice = price.toIntOrNull(),
                        )
                    )
                },
                enabled = partName.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("저장") }
        }
    }
}
