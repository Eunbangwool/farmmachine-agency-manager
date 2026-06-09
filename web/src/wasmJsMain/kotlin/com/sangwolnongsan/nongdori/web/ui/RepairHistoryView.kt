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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sangwolnongsan.nongdori.shared.data.RepairStatus
import com.sangwolnongsan.nongdori.shared.data.WorkOrder
import com.sangwolnongsan.nongdori.shared.ui.theme.TextSecondary
import com.sangwolnongsan.nongdori.shared.util.formatDate
import com.sangwolnongsan.nongdori.shared.util.formatDateTime
import com.sangwolnongsan.nongdori.web.firebase.jsDownloadTextFile

/**
 * 수리 이력 — 완료(DONE)된 WorkOrder 목록. 고객/기계/금액 표시 + CSV 내보내기.
 * (수리 이력은 별도 컬렉션이 아니라 status==DONE 인 WorkOrder 다.)
 */
@Composable
fun RepairHistoryView(workOrders: List<WorkOrder>) {
    val done = workOrders.filter { it.status == RepairStatus.DONE }
        .sortedByDescending { it.completedAtMillis ?: 0L }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("수리 이력 (${done.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Box(Modifier.weight(1f))
            if (done.isNotEmpty()) {
                Button(onClick = { jsDownloadTextFile("repair_history.csv", toCsv(done), "text/csv;charset=utf-8") }) {
                    Text("CSV 내보내기")
                }
            }
        }
        if (done.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("완료된 수리가 없습니다.", color = TextSecondary)
            }
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                done.forEach { wo -> HistoryRow(wo) }
            }
        }
    }
}

@Composable
private fun HistoryRow(wo: WorkOrder) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text(wo.customerName.ifBlank { "(고객 미상)" }, fontWeight = FontWeight.SemiBold)
                Box(Modifier.weight(1f))
                Text(formatDate(wo.completedAtMillis ?: 0L), color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            if (wo.machineLabel.isNotBlank()) Text("기계: ${wo.machineLabel}", style = MaterialTheme.typography.bodySmall)
            if (wo.repair.workDescription.isNotBlank()) Text(wo.repair.workDescription, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            val total = wo.repair.computedTotal
            if (total > 0) Text("금액: ${total}원", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun toCsv(list: List<WorkOrder>): String {
    val sb = StringBuilder()
    sb.append("완료일,고객,연락처,기계,증상,진단,작업내용,부품,공임,총액,담당\n")
    list.forEach { wo ->
        val r = wo.repair
        val parts = r.partsUsed.joinToString(" / ") { it.name }
        val row = listOf(
            formatDateTime(wo.completedAtMillis ?: 0L),
            wo.customerName, wo.customerPhone, wo.machineLabel, wo.symptom,
            r.diagnosis, r.workDescription, parts,
            (r.laborCost ?: 0).toString(), r.computedTotal.toString(), r.performedByName,
        ).joinToString(",") { csvCell(it) }
        sb.append(row).append("\n")
    }
    return sb.toString()
}

/** CSV 셀 이스케이프. */
private fun csvCell(v: String): String =
    if (v.contains(",") || v.contains("\"") || v.contains("\n"))
        "\"" + v.replace("\"", "\"\"") + "\""
    else v
