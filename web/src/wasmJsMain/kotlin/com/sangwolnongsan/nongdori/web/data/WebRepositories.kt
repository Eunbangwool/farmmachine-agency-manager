package com.sangwolnongsan.nongdori.web.data

import com.sangwolnongsan.nongdori.shared.data.Customer
import com.sangwolnongsan.nongdori.shared.data.DealershipMember
import com.sangwolnongsan.nongdori.shared.data.WorkOrder
import com.sangwolnongsan.nongdori.web.firebase.jsObserveCollection
import com.sangwolnongsan.nongdori.web.firebase.jsSetDoc
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 웹(디스패처) Firestore 레포지토리 — window.nongdori JSON 브리지 기반.
 * 모든 경로는 dealerships/{code}/... 로 스코프된다.
 * 컬렉션 구독은 onUpdate 콜백으로 최신 목록을 흘려보낸다(Compose 에서 state 로 수집).
 */
object WebRepositories {

    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** 새 문서 ID (시각 기반 + 랜덤 접미). */
    fun newId(): String = "${com.sangwolnongsan.nongdori.shared.util.nowMs()}-${(1000..9999).random()}"

    // ── WorkOrder ──
    fun observeWorkOrders(code: String, onUpdate: (List<WorkOrder>) -> Unit) {
        jsObserveCollection("dealerships/$code/workOrders") { arr ->
            onUpdate(parseList(arr) { json.decodeFromString<List<WorkOrder>>(it) })
        }
    }

    fun saveWorkOrder(code: String, wo: WorkOrder, onResult: (Boolean) -> Unit = {}) {
        jsSetDoc("dealerships/$code/workOrders/${wo.id}", json.encodeToString(wo)) { onResult(it == "ok") }
    }

    // ── Customer ──
    fun observeCustomers(code: String, onUpdate: (List<Customer>) -> Unit) {
        jsObserveCollection("dealerships/$code/customers") { arr ->
            onUpdate(parseList(arr) { json.decodeFromString<List<Customer>>(it) })
        }
    }

    fun saveCustomer(code: String, customer: Customer, onResult: (Boolean) -> Unit = {}) {
        jsSetDoc("dealerships/$code/customers/${customer.id}", json.encodeToString(customer)) { onResult(it == "ok") }
    }

    // ── Members (엔지니어 배정 선택용) ──
    fun observeMembers(code: String, onUpdate: (List<DealershipMember>) -> Unit) {
        jsObserveCollection("dealerships/$code/members") { arr ->
            onUpdate(parseList(arr) { json.decodeFromString<List<DealershipMember>>(it) })
        }
    }

    private fun <T> parseList(arr: String, decode: (String) -> List<T>): List<T> =
        if (arr.isBlank()) emptyList() else try { decode(arr) } catch (_: Throwable) { emptyList() }
}
