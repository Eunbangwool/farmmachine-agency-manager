package com.sangwolnongsan.nongdori.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.sangwolnongsan.nongdori.shared.data.WorkOrder
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * 출장(WorkOrder) Firestore 레포지토리 — Android(엔지니어) 측.
 * dealerships/{code}/workOrders 스코프.
 */
class WorkOrderRepository(private val dealerCode: String) {

    private val col = FirebaseFirestore.getInstance()
        .collection("dealerships").document(dealerCode).collection("workOrders")

    /** 나에게 배정된 출장 실시간 구독 (최신 요청 우선). */
    fun observeAssignedTo(uid: String): Flow<List<WorkOrder>> = callbackFlow {
        val reg = col.whereEqualTo("assignedEngineerUid", uid)
            .addSnapshotListener { snap, err ->
                if (err != null) { Log.e(TAG, "observeAssignedTo err", err); trySend(emptyList()); return@addSnapshotListener }
                val list = snap?.documents?.map { FirestoreMappers.workOrderFromDoc(it) }
                    ?.sortedByDescending { it.requestedAtMillis } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    /** 전체 출장 실시간 구독 (이력/관리용). */
    fun observeAll(): Flow<List<WorkOrder>> = callbackFlow {
        val reg = col.addSnapshotListener { snap, err ->
            if (err != null) { Log.e(TAG, "observeAll err", err); trySend(emptyList()); return@addSnapshotListener }
            val list = snap?.documents?.map { FirestoreMappers.workOrderFromDoc(it) }
                ?.sortedByDescending { it.requestedAtMillis } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    suspend fun save(wo: WorkOrder): Boolean = try {
        col.document(wo.id).set(FirestoreMappers.workOrderToMap(wo)).await()
        true
    } catch (e: Exception) {
        Log.e(TAG, "save failed", e); false
    }

    companion object { private const val TAG = "WorkOrderRepository" }
}
