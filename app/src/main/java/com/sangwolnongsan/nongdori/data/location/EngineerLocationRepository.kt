package com.sangwolnongsan.nongdori.data.location

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * 엔지니어 라이브 위치 저장소 — dealerships/{code}/liveLocations/{uid}.
 * 출동 중 위치를 업로드. 멤버(디스패처)는 firestore.rules 에서 읽기 허용.
 */
object EngineerLocationRepository {

    private const val TAG = "EngineerLocation"

    private fun db() = FirebaseFirestore.getInstance()

    suspend fun upload(dealerCode: String, lat: Double, lng: Double, accuracy: Double): Boolean {
        val user = FirebaseAuth.getInstance().currentUser ?: return false
        if (dealerCode.isBlank()) return false
        return try {
            db().collection("dealerships").document(dealerCode)
                .collection("liveLocations").document(user.uid)
                .set(
                    mapOf(
                        "uid" to user.uid,
                        "displayName" to (user.displayName ?: ""),
                        "email" to (user.email ?: ""),
                        "lat" to lat,
                        "lng" to lng,
                        "accuracy" to accuracy,
                        "updatedAt" to System.currentTimeMillis(),
                    )
                ).await()
            true
        } catch (e: Exception) {
            Log.w(TAG, "upload failed", e); false
        }
    }

    suspend fun clear(dealerCode: String): Boolean {
        val user = FirebaseAuth.getInstance().currentUser ?: return false
        if (dealerCode.isBlank()) return false
        return try {
            db().collection("dealerships").document(dealerCode)
                .collection("liveLocations").document(user.uid).delete().await()
            true
        } catch (e: Exception) {
            Log.w(TAG, "clear failed", e); false
        }
    }
}
