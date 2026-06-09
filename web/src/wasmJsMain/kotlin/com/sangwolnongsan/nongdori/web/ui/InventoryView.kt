package com.sangwolnongsan.nongdori.web.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sangwolnongsan.nongdori.shared.data.Part
import com.sangwolnongsan.nongdori.shared.ui.theme.PriorityUrgent
import com.sangwolnongsan.nongdori.shared.ui.theme.StatusDone
import com.sangwolnongsan.nongdori.shared.ui.theme.TextSecondary
import com.sangwolnongsan.nongdori.web.data.WebRepositories
import kotlin.math.roundToInt

/**
 * 재고관리 — 부품 목록(재고 부족 강조), 추가/수정, 재고 +/- 조정, 삭제.
 */
@Composable
fun InventoryView(dealerCode: String) {
    var parts by remember { mutableStateOf<List<Part>>(emptyList()) }
    var adding by remember { mutableStateOf(false) }
    LaunchedEffect(dealerCode) { WebRepositories.observeParts(dealerCode) { parts = it } }

    val sorted = parts.sortedWith(compareByDescending<Part> { it.needsReorder }.thenBy { it.name })

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("재고 (${parts.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Box(Modifier.weight(1f))
            val low = parts.count { it.needsReorder }
            if (low > 0) Text("재주문 필요 $low", color = PriorityUrgent, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { adding = !adding }) { Text(if (adding) "닫기" else "+ 부품 추가") }
        }

        if (adding) {
            PartForm(dealerCode) { adding = false }
        }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (sorted.isEmpty() && !adding) {
                Text("등록된 부품이 없습니다. ‘+ 부품 추가’ 로 등록하세요.", color = TextSecondary)
            }
            sorted.forEach { part -> PartRow(dealerCode, part) }
        }
    }
}

@Composable
private fun PartRow(dealerCode: String, part: Part) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            // 상태 막대: 재고 부족=빨강, 정상=초록
            Box(Modifier.width(10.dp).height(36.dp).clip(RoundedCornerShape(5.dp))
                .background(if (part.needsReorder) PriorityUrgent else StatusDone))
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(part.name, fontWeight = FontWeight.SemiBold)
                val sub = buildString {
                    if (part.category.isNotBlank()) append(part.category)
                    if (part.partNumber?.isNotBlank() == true) { if (isNotEmpty()) append(" · "); append(part.partNumber) }
                    if (part.location?.isNotBlank() == true) { if (isNotEmpty()) append(" · "); append(part.location) }
                }
                if (sub.isNotBlank()) Text(sub, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                part.unitPrice?.let { Text("단가 ${it}원", color = TextSecondary, style = MaterialTheme.typography.bodySmall) }
            }
            // 재고 +/- 조정
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { WebRepositories.savePart(dealerCode, part.copy(stockQty = (part.stockQty - 1).coerceAtLeast(0.0))) }) { Text("-") }
                Text("  ${fmtQty(part.stockQty)}  ", fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = { WebRepositories.savePart(dealerCode, part.copy(stockQty = part.stockQty + 1)) }) { Text("+") }
            }
            TextButton(onClick = { WebRepositories.deletePart(dealerCode, part.id) }) { Text("삭제", color = PriorityUrgent) }
        }
    }
}

@Composable
private fun PartForm(dealerCode: String, onDone: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var partNumber by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("0") }
    var price by remember { mutableStateOf("") }
    var reorder by remember { mutableStateOf("0") }
    var location by remember { mutableStateOf("") }

    Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("부품명") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(category, { category = it }, label = { Text("분류") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(partNumber, { partNumber = it }, label = { Text("부품번호") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(stock, { stock = it.filter { c -> c.isDigit() } }, label = { Text("재고수량") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(reorder, { reorder = it.filter { c -> c.isDigit() } }, label = { Text("재주문 기준") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(price, { price = it.filter { c -> c.isDigit() } }, label = { Text("단가(원)") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(location, { location = it }, label = { Text("보관 위치") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Button(
                onClick = {
                    WebRepositories.savePart(
                        dealerCode,
                        Part(
                            id = WebRepositories.newId(),
                            name = name,
                            category = category,
                            partNumber = partNumber.ifBlank { null },
                            stockQty = stock.toDoubleOrNull() ?: 0.0,
                            unitPrice = price.toIntOrNull(),
                            reorderLevel = reorder.toDoubleOrNull() ?: 0.0,
                            location = location.ifBlank { null },
                        ),
                    ) { onDone() }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("저장") }
        }
    }
}

private fun fmtQty(q: Double): String = if (q == q.toLong().toDouble()) q.toLong().toString() else ((q * 10).roundToInt() / 10.0).toString()
