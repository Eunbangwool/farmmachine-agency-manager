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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.sangwolnongsan.nongdori.shared.data.LiveLocation
import com.sangwolnongsan.nongdori.shared.data.WorkOrder
import com.sangwolnongsan.nongdori.shared.ui.theme.TextSecondary
import com.sangwolnongsan.nongdori.shared.util.formatDateTime
import com.sangwolnongsan.nongdori.shared.util.haversineKm
import com.sangwolnongsan.nongdori.shared.util.straightLineEtaMinutes
import com.sangwolnongsan.nongdori.web.data.WebRepositories
import com.sangwolnongsan.nongdori.web.firebase.jsOpenVworldMap
import kotlin.math.roundToInt

/**
 * 기사 위치 — 출동 중 엔지니어의 마지막 위치 + 담당 고객까지 직선 ETA.
 * 'VWorld 지도' 버튼으로 해당 좌표를 VWorld 지도 페이지(새 탭)에서 확인.
 */
@Composable
fun EngineerLiveScreen(dealerCode: String, workOrders: List<WorkOrder>) {
    var locations by remember { mutableStateOf<List<LiveLocation>>(emptyList()) }
    LaunchedEffect(dealerCode) {
        WebRepositories.observeLiveLocations(dealerCode) { locations = it }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("기사 위치 (${locations.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (locations.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("출동 중인 엔지니어가 없습니다.", color = TextSecondary)
            }
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                locations.forEach { loc ->
                    val wo = workOrders.firstOrNull {
                        it.assignedEngineerUid == loc.uid && it.status.isOpen && it.lat != null && it.lng != null
                    }
                    EngineerLocationCard(loc, wo)
                }
            }
        }
    }
}

@Composable
private fun EngineerLocationCard(loc: LiveLocation, assigned: WorkOrder?) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(loc.displayName.ifBlank { loc.email.ifBlank { "엔지니어" } }, fontWeight = FontWeight.SemiBold)
                Box(Modifier.weight(1f))
                Text(formatDateTime(loc.updatedAt), color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            if (assigned != null && assigned.lat != null && assigned.lng != null) {
                val km = haversineKm(loc.lat, loc.lng, assigned.lat!!, assigned.lng!!)
                val eta = straightLineEtaMinutes(km)
                Text(
                    "→ ${assigned.customerName}: 약 ${(km * 10).roundToInt() / 10.0}km · ETA ${eta}분 (직선)",
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                Text("현재 위치: ${fmt(loc.lat)}, ${fmt(loc.lng)}", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(
                onClick = { jsOpenVworldMap(loc.lat, loc.lng, loc.displayName) },
                modifier = Modifier.padding(top = 8.dp),
            ) { Text("VWorld 지도에서 보기") }
        }
    }
}

private fun fmt(v: Double): String {
    val r = (v * 100000).roundToInt() / 100000.0
    return r.toString()
}
