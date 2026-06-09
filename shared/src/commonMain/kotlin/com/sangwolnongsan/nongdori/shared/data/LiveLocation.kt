package com.sangwolnongsan.nongdori.shared.data

import kotlinx.serialization.Serializable

/** 엔지니어 라이브 위치 — dealerships/{code}/liveLocations/{uid}. */
@Serializable
data class LiveLocation(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val accuracy: Double = 0.0,
    val updatedAt: Long = 0L,
)
