package com.sangwolnongsan.nongdori.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.sangwolnongsan.nongdori.shared.data.CatalogPart
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * 기종별 부품 카탈로그 Firestore 레포지토리 — Android 측.
 * dealerships/{code}/partCatalog 스코프. 웹과 동일 컬렉션·필드.
 */
class CatalogPartRepository(private val dealerCode: String) {

    private val col = FirebaseFirestore.getInstance()
        .collection("dealerships").document(dealerCode).collection("partCatalog")

    fun observeAll(): Flow<List<CatalogPart>> = callbackFlow {
        val reg = col.addSnapshotListener { snap, err ->
            if (err != null) { Log.e(TAG, "observeAll err", err); trySend(emptyList()); return@addSnapshotListener }
            val list = snap?.documents?.map { FirestoreMappers.catalogPartFromDoc(it) } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    suspend fun save(entry: CatalogPart): Boolean = try {
        col.document(entry.id).set(FirestoreMappers.catalogPartToMap(entry)).await()
        true
    } catch (e: Exception) {
        Log.e(TAG, "save failed", e); false
    }

    suspend fun delete(entryId: String): Boolean = try {
        col.document(entryId).delete().await()
        true
    } catch (e: Exception) {
        Log.e(TAG, "delete failed", e); false
    }

    companion object { private const val TAG = "CatalogPartRepository" }
}
